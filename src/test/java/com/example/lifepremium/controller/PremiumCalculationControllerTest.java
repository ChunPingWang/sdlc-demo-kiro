package com.example.lifepremium.controller;

// FR: FR-CALC-001

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.lifepremium.dto.request.PremiumCalculateRequest;
import com.example.lifepremium.exception.*;
import com.example.lifepremium.fixture.PremiumFixture;
import com.example.lifepremium.service.PremiumCalculationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.example.lifepremium.fixture.PremiumFixture.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PremiumCalculationController.class)
@ActiveProfiles("test")
@DisplayName("PremiumCalculationController Tests")
class PremiumCalculationControllerTest {

    @Autowired  private MockMvc       mockMvc;
    @Autowired  private ObjectMapper  objectMapper;
    @MockBean   private PremiumCalculationService calculationService;

    @Test @DisplayName("POST /calculate - 正常試算回傳 200")
    // FR-CALC-001 主要流程
    void calculate_success_200() throws Exception {
        given(calculationService.calculate(any(), any()))
            .willReturn(PremiumFixture.aSuccessResponse());

        mockMvc.perform(post("/api/v1/premium/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Agent-Id", AGENT_ID.toString())
                .content(objectMapper.writeValueAsString(aValidRequest())))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.annualPremium").value(ANNUAL_MALE))
            .andExpect(jsonPath("$.data.monthlyPremium").value(MONTHLY_MALE));
    }

    @Test @DisplayName("POST /calculate - 訪客無 X-Agent-Id Header 仍回傳 200")
    // FR-CALC-001 替代流程（訪客試算）
    void calculate_guest_noHeader_200() throws Exception {
        given(calculationService.calculate(any(), isNull()))
            .willReturn(PremiumFixture.aSuccessResponse());

        mockMvc.perform(post("/api/v1/premium/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aValidRequest())))
            .andExpect(status().isOk());
    }

    @Test @DisplayName("POST /calculate - productCode 空白回傳 400")
    // 輸入格式驗證
    void calculate_blankProductCode_400() throws Exception {
        PremiumCalculateRequest bad = new PremiumCalculateRequest(
                "", DEFAULT_AGE, GENDER_MALE, DEFAULT_AMOUNT, DEFAULT_PERIOD);

        mockMvc.perform(post("/api/v1/premium/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.data.productCode").isArray());
    }

    @Test @DisplayName("POST /calculate - 年齡超出上限回傳 422 AGE_OUT_OF_RANGE")
    // FR-CALC-001 例外處理 - BR-001
    void calculate_ageOutOfRange_422() throws Exception {
        given(calculationService.calculate(any(), any()))
            .willThrow(new AgeOutOfRangeException(71));

        mockMvc.perform(post("/api/v1/premium/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Agent-Id", AGENT_ID.toString())
                .content(objectMapper.writeValueAsString(aRequestWithAge(71))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("AGE_OUT_OF_RANGE"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("被保人年齡須介於 0 至 70 歲")));
    }

    @Test @DisplayName("POST /calculate - 保額超出範圍回傳 422 AMOUNT_OUT_OF_RANGE")
    void calculate_amountOutOfRange_422() throws Exception {
        given(calculationService.calculate(any(), any()))
            .willThrow(new AmountOutOfRangeException(50));

        mockMvc.perform(post("/api/v1/premium/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aRequestWithAmount(50))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("AMOUNT_OUT_OF_RANGE"));
    }

    @Test @DisplayName("POST /calculate - 繳費年期非法回傳 422 INVALID_PAYMENT_PERIOD")
    void calculate_invalidPeriod_422() throws Exception {
        given(calculationService.calculate(any(), any()))
            .willThrow(new InvalidPaymentPeriodException(15));

        mockMvc.perform(post("/api/v1/premium/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aRequestWithPeriod(15))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_PERIOD"));
    }

    @Test @DisplayName("POST /calculate - 查無費率回傳 404 RATE_NOT_FOUND")
    void calculate_rateNotFound_404() throws Exception {
        given(calculationService.calculate(any(), any()))
            .willThrow(new RateNotFoundException("LIFE-XX-99", 35, "M", 20));

        mockMvc.perform(post("/api/v1/premium/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        aRequestWithProductCode("LIFE-XX-99"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RATE_NOT_FOUND"));
    }
}
