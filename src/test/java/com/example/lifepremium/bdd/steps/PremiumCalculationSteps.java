package com.example.lifepremium.bdd.steps;

// Feature: sdlc/fsd/output/features/premium-calculation.feature
// FR: FR-CALC-001, FR-CALC-002 | BR: BR-001 ~ BR-005

import com.example.lifepremium.domain.*;
import com.example.lifepremium.repository.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.cucumber.spring.ScenarioScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ScenarioScope
public class PremiumCalculationSteps {

    @Autowired private TestRestTemplate          restTemplate;
    @Autowired private ProductRepository         productRepository;
    @Autowired private RateTableVersionRepository versionRepository;
    @Autowired private RateEntryRepository       rateEntryRepository;
    @Autowired private CalculationRecordRepository recordRepository;

    private ResponseEntity<Map>     lastResponse;
    private Map<String, Object>     requestBody;
    private String                  agentId;
    private RateTableVersion        activeVersion;

    @Before
    public void setUp() {
        requestBody = new HashMap<>();
        agentId     = null;
    }

    // ── Given ──

    @Given("系統存在商品代碼 {string} 的生效費率表")
    public void 系統存在商品代碼的生效費率表(String productCode) {
        if (!productRepository.existsByProductCode(productCode)) {
            Product product = productRepository.save(
                    Product.create(productCode, productCode + " 終身壽險"));
            activeVersion = RateTableVersion.create(
                    product, 1, LocalDate.of(2026, 1, 1),
                    "s3://test/v1.csv", UUID.randomUUID());
            activeVersion.activate(0);
            versionRepository.save(activeVersion);
        } else {
            Product product = productRepository.findByProductCode(productCode).orElseThrow();
            activeVersion = versionRepository
                    .findTopByProductCodeAndStatusOrderByEffectiveDateDesc(
                            productCode, RateTableVersion.VersionStatus.ACTIVE)
                    .orElseThrow();
        }
    }

    @Given("費率表包含以下資料：")
    public void 費率表包含以下資料(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        for (Map<String, String> row : rows) {
            int    age    = Integer.parseInt(row.get("年齡"));
            String gender = row.get("性別");
            int    period = Integer.parseInt(row.get("繳費年期"));
            BigDecimal rate = new BigDecimal(row.get("費率"));
            rateEntryRepository.save(RateEntry.create(activeVersion, age, gender, period, rate));
        }
        activeVersion.activate(rows.size());
        versionRepository.save(activeVersion);
    }

    @Given("業務員 {string} 已登入系統")
    public void 業務員已登入系統(String agent) {
        this.agentId = UUID.randomUUID().toString();
    }

    @Given("使用者未登入")
    public void 使用者未登入() {
        this.agentId = null;
    }

    @Given("系統中不存在商品代碼 {string} 的費率資料")
    public void 系統中不存在商品代碼的費率資料(String productCode) {
        // 不建立該商品費率，確認查不到即可
        assertThat(rateEntryRepository.findEffectiveRate(
                productCode, 35, "M", 20, LocalDate.now())).isEmpty();
    }

    @Given("費率表中年齡 {int}、性別 {string}、繳費年期 {int} 的費率為 {bigdecimal}")
    public void 費率表中費率設定(int age, String gender, int period, BigDecimal rate) {
        rateEntryRepository.save(RateEntry.create(activeVersion, age, gender, period, rate));
    }

    // ── When ──

    @When("業務員送出保費試算請求：")
    public void 業務員送出保費試算請求(DataTable table) {
        Map<String, String> data = table.asMap();
        buildRequestAndCall(data);
    }

    @When("業務員送出保費試算請求，年齡為 {int}，性別 {string}，保額 {int} 萬，繳費年期 {int}，商品 {string}")
    public void 業務員送出保費試算請求參數(int age, String gender, int amount, int period, String code) {
        Map<String, Object> body = Map.of(
                "productCode", code, "age", age, "gender", gender,
                "insuredAmount", amount, "paymentPeriod", period);
        callApi(body);
    }

    @When("業務員送出保費試算請求，年齡 {int}，性別 {string}，保額 {int} 萬，繳費年期 {int}，商品 {string}")
    public void 業務員送出保費試算請求保額(int age, String gender, int amount, int period, String code) {
        業務員送出保費試算請求參數(age, gender, amount, period, code);
    }

    @When("使用者送出匿名保費試算請求：")
    public void 使用者送出匿名保費試算請求(DataTable table) {
        Map<String, String> data = table.asMap();
        buildRequestAndCall(data);
    }

    @When("業務員送出保費試算請求，商品代碼為 {string}，年齡 {int}，性別 {string}，保額 {int} 萬，繳費年期 {int}")
    public void 業務員送出保費試算請求商品代碼(String code, int age, String gender, int amount, int period) {
        業務員送出保費試算請求參數(age, gender, amount, period, code);
    }

    @When("業務員試算保額 {int} 萬元")
    public void 業務員試算保額(int amount) {
        // 使用上一個 Given 設定的費率明細條件重新試算
        requestBody.put("insuredAmount", amount);
        callApi(requestBody);
    }

    // ── Then ──

    @Then("系統應回傳 HTTP 狀態碼 {int}")
    public void 系統應回傳HTTP狀態碼(int statusCode) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(statusCode);
    }

    @Then("回應中 {string} 應為 {int}")
    public void 回應中整數應為(String field, int expected) {
        Object data = getResponseData();
        if (data instanceof Map<?,?> map) {
            assertThat(((Number) map.get(field)).intValue()).isEqualTo(expected);
        }
    }

    @Then("回應中 {string} 應為 {string}")
    public void 回應中字串應為(String field, String expected) {
        Object data = getResponseData();
        if (data instanceof Map<?,?> map) {
            assertThat(map.get(field)).isEqualTo(expected);
        }
    }

    @Then("回應中 {string} 應包含 {string}")
    public void 回應中應包含(String field, String substring) {
        String value = (String) lastResponse.getBody().get(field);
        assertThat(value).contains(substring);
    }

    @Then("業務員 {string} 的試算紀錄應被保存")
    public void 業務員的試算紀錄應被保存(String agent) {
        assertThat(recordRepository.count()).isGreaterThan(0);
    }

    @Then("系統試算紀錄表中不應新增任何紀錄")
    public void 系統試算紀錄表中不應新增任何紀錄() {
        assertThat(recordRepository.countByAgentIdIsNull()).isEqualTo(0);
    }

    @Then("年繳保費應為 {int}")
    public void 年繳保費應為(int expected) {
        回應中整數應為("annualPremium", expected);
    }

    @Then("月繳保費應為 {int}")
    public void 月繳保費應為(int expected) {
        回應中整數應為("monthlyPremium", expected);
    }

    // ── Private Helpers ──

    @SuppressWarnings("unchecked")
    private void buildRequestAndCall(Map<String, String> data) {
        Map<String, Object> body = new HashMap<>();
        body.put("productCode",    data.get("productCode"));
        body.put("age",            Integer.parseInt(data.get("age")));
        body.put("gender",         data.get("gender"));
        body.put("insuredAmount",  Integer.parseInt(data.get("insuredAmount")));
        body.put("paymentPeriod",  Integer.parseInt(data.get("paymentPeriod")));
        this.requestBody = body;
        callApi(body);
    }

    @SuppressWarnings("unchecked")
    private void callApi(Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (agentId != null) {
            headers.set("X-Agent-Id", agentId);
        }
        lastResponse = restTemplate.exchange(
                "/api/v1/premium/calculate",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);
    }

    @SuppressWarnings("unchecked")
    private Object getResponseData() {
        if (lastResponse.getBody() == null) return null;
        return lastResponse.getBody().get("data");
    }
}
