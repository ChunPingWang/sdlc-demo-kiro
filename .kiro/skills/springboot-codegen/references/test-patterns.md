# Spring Boot 測試範本

> 本文件提供各類測試的標準範本，供 `springboot-codegen` Skill 在 Phase 1 產出測試程式時參照。  
> 測試框架：JUnit 5 + Mockito + AssertJ + Cucumber 7 + Testcontainers。

---

## §1 Cucumber BDD 測試

### CucumberTestRunner（執行入口）

```java
// src/test/java/com/{company}/{project}/bdd/CucumberTestRunner.java
package com.{company}.{project}.bdd;

import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.plugin", value =
    "pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json")
@ConfigurationParameter(key = "cucumber.glue", value = "com.{company}.{project}.bdd.steps")
@ConfigurationParameter(key = "cucumber.filter.tags", value = "not @wip")
public class CucumberTestRunner {}
```

### CucumberSpringConfiguration（Spring 整合）

```java
// src/test/java/com/{company}/{project}/bdd/CucumberSpringConfiguration.java
package com.{company}.{project}.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {}
```

### Step Definition 範本

```java
// src/test/java/com/{company}/{project}/bdd/steps/{Module}Steps.java
package com.{company}.{project}.bdd.steps;

// FR: FR-{MODULE}-001, FR-{MODULE}-002
// Feature: sdlc/fsd/output/features/{module_code}-{feature}.feature

import com.{company}.{project}.dto.request.{Resource}CreateRequest;
import com.{company}.{project}.dto.response.{Resource}Response;
import com.{company}.{project}.dto.response.ApiResponse;
import io.cucumber.java.Before;
import io.cucumber.java.zh_tw.*;           // 繁體中文 Step 關鍵字
import io.cucumber.java.en.*;              // 英文 Step 關鍵字（擇一）
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@ScenarioScope
public class {Module}Steps {

    @Autowired
    private TestRestTemplate restTemplate;

    // Scenario 內共享狀態
    private ResponseEntity<?> lastResponse;
    private {Resource}CreateRequest createRequest;

    @Before
    public void setUp() {
        // 每個 Scenario 前的初始化
    }

    // ── Given ──

    @Given("使用者已登入系統")
    public void 使用者已登入系統() {
        // TODO: 設定認證 Token
        // restTemplate = restTemplate.withBasicAuth(...) 或設定 JWT Header
        throw new io.cucumber.java.PendingException();
    }

    @Given("{string} 已存在於系統中")
    public void 資源已存在於系統中(String identifier) {
        // TODO: 準備測試資料（呼叫 API 或直接寫入 DB）
        throw new io.cucumber.java.PendingException();
    }

    @Given("以下{資源}資料：")
    public void 以下資源資料(io.cucumber.datatable.DataTable dataTable) {
        // TODO: 從 DataTable 建立 Request
        // createRequest = new {Resource}CreateRequest(...)
        throw new io.cucumber.java.PendingException();
    }

    // ── When ──

    @When("使用者送出建立{資源}的請求")
    public void 使用者送出建立資源的請求() {
        // TODO: 呼叫 API
        // lastResponse = restTemplate.postForEntity("/api/v1/{resources}", createRequest, {Resource}Response.class);
        throw new io.cucumber.java.PendingException();
    }

    @When("使用者查詢 ID 為 {string} 的{資源}")
    public void 使用者查詢ID為的資源(String id) {
        // TODO:
        // lastResponse = restTemplate.getForEntity("/api/v1/{resources}/" + id, {Resource}Response.class);
        throw new io.cucumber.java.PendingException();
    }

    // ── Then ──

    @Then("系統應回應狀態碼 {int}")
    public void 系統應回應狀態碼(int statusCode) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(statusCode);
    }

    @Then("回應中應包含{資源}的 ID")
    public void 回應中應包含資源的ID() {
        // TODO: 解析回應並驗證 ID 欄位
        throw new io.cucumber.java.PendingException();
    }

    @Then("系統應顯示錯誤訊息 {string}")
    public void 系統應顯示錯誤訊息(String expectedMessage) {
        // TODO: 解析 ApiResponse.message 並比對
        throw new io.cucumber.java.PendingException();
    }
}
```

---

## §2 Controller Integration Test（@SpringBootTest + MockMvc）

```java
// src/test/java/com/{company}/{project}/controller/{Resource}ControllerTest.java
package com.{company}.{project}.controller;

// FR: FR-{MODULE}-001 ~ FR-{MODULE}-006

import com.fasterxml.jackson.databind.ObjectMapper;
import com.{company}.{project}.dto.request.{Resource}CreateRequest;
import com.{company}.{project}.dto.response.{Resource}Response;
import com.{company}.{project}.service.{Business}Service;
import com.{company}.{project}.exception.{Resource}NotFoundException;
import com.{company}.{project}.fixture.{Resource}Fixture;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("{Resource} Controller Tests")
class {Resource}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private {Business}Service {business}Service;

    private {Resource}Response sample{Resource};

    @BeforeEach
    void setUp() {
        sample{Resource} = {Resource}Fixture.aDefault{Resource}();
    }

    // ── POST /api/v1/{resources} ──

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /{resources} - 正常建立")
    // FR-{MODULE}-001: 正常流程
    void create_success() throws Exception {
        {Resource}CreateRequest request = {Resource}Fixture.aCreateRequest();
        given({business}Service.create(any(), any())).willReturn(sample{Resource});

        mockMvc.perform(post("/api/v1/{resources}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /{resources} - 驗證失敗（必填欄位空白）")
    // FR-{MODULE}-001: 例外情境 - 輸入驗證失敗
    void create_validationFailed_blankField() throws Exception {
        {Resource}CreateRequest invalidRequest = {Resource}Fixture.anInvalidCreateRequest();

        mockMvc.perform(post("/api/v1/{resources}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.data.{field}").isArray());
    }

    @Test
    @DisplayName("POST /{resources} - 未認證")
    void create_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/{resources}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString({Resource}Fixture.aCreateRequest())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")        // 只有 ADMIN 可建立
    @DisplayName("POST /{resources} - 未授權（USER 角色）")
    void create_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/{resources}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString({Resource}Fixture.aCreateRequest())))
            .andExpect(status().isForbidden());
    }

    // ── GET /api/v1/{resources}/{id} ──

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /{resources}/{id} - 查詢成功")
    // FR-{MODULE}-002: 查詢單筆
    void findById_success() throws Exception {
        UUID id = sample{Resource}.id();
        given({business}Service.findById(id)).willReturn(sample{Resource});

        mockMvc.perform(get("/api/v1/{resources}/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /{resources}/{id} - 資源不存在")
    // FR-{MODULE}-002: 例外情境 - 不存在
    void findById_notFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        given({business}Service.findById(nonExistentId))
            .willThrow(new {Resource}NotFoundException(nonExistentId));

        mockMvc.perform(get("/api/v1/{resources}/{id}", nonExistentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("{RESOURCE}_NOT_FOUND"));
    }

    // ── DELETE /api/v1/{resources}/{id} ──

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /{resources}/{id} - 刪除成功")
    // FR-{MODULE}-006
    void delete_success() throws Exception {
        UUID id = sample{Resource}.id();
        willDoNothing().given({business}Service).delete(eq(id), any());

        mockMvc.perform(delete("/api/v1/{resources}/{id}", id))
            .andExpect(status().isNoContent());
    }
}
```

---

## §3 Service Unit Test（JUnit5 + Mockito）

```java
// src/test/java/com/{company}/{project}/service/{Business}ServiceTest.java
package com.{company}.{project}.service;

// FR: FR-{MODULE}-001 ~ FR-{MODULE}-006

import com.{company}.{project}.domain.{Entity};
import com.{company}.{project}.dto.request.{Resource}CreateRequest;
import com.{company}.{project}.dto.response.{Resource}Response;
import com.{company}.{project}.exception.{Resource}AlreadyExistsException;
import com.{company}.{project}.exception.{Resource}NotFoundException;
import com.{company}.{project}.fixture.{Resource}Fixture;
import com.{company}.{project}.mapper.{Resource}Mapper;
import com.{company}.{project}.repository.{Entity}Repository;
import com.{company}.{project}.service.impl.{Business}ServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("{Business}Service Unit Tests")
class {Business}ServiceTest {

    @Mock
    private {Entity}Repository {entity}Repository;

    @Mock
    private {Resource}Mapper {resource}Mapper;

    @InjectMocks
    private {Business}ServiceImpl {business}Service;

    private {Entity} sample{Entity};
    private {Resource}Response sample{Resource}Response;

    @BeforeEach
    void setUp() {
        sample{Entity} = {Resource}Fixture.a{Entity}();
        sample{Resource}Response = {Resource}Fixture.aDefault{Resource}();
    }

    // ── create ──

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("정상 건립 성공")
        // FR-{MODULE}-001: 主要流程
        void create_success() {
            {Resource}CreateRequest request = {Resource}Fixture.aCreateRequest();
            given({entity}Repository.existsBy{UniqueField}(any())).willReturn(false);
            given({resource}Mapper.toEntity(request)).willReturn(sample{Entity});
            given({entity}Repository.save(sample{Entity})).willReturn(sample{Entity});
            given({resource}Mapper.toResponse(sample{Entity})).willReturn(sample{Resource}Response);

            {Resource}Response result = {business}Service.create(request, UUID.randomUUID());

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(sample{Resource}Response.id());
            then({entity}Repository).should().save(sample{Entity});
        }

        @Test
        @DisplayName("重複資料時拋出 AlreadyExistsException")
        // FR-{MODULE}-001: 例外情境 - 重複
        void create_alreadyExists_throwsException() {
            {Resource}CreateRequest request = {Resource}Fixture.aCreateRequest();
            given({entity}Repository.existsBy{UniqueField}(any())).willReturn(true);

            assertThatThrownBy(() -> {business}Service.create(request, UUID.randomUUID()))
                .isInstanceOf({Resource}AlreadyExistsException.class);

            then({entity}Repository).should(never()).save(any());
        }
    }

    // ── findById ──

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("正常查詢成功")
        // FR-{MODULE}-002: 主要流程
        void findById_success() {
            UUID id = sample{Entity}.getId();
            given({entity}Repository.findById(id)).willReturn(Optional.of(sample{Entity}));
            given({resource}Mapper.toResponse(sample{Entity})).willReturn(sample{Resource}Response);

            {Resource}Response result = {business}Service.findById(id);

            assertThat(result).isEqualTo(sample{Resource}Response);
        }

        @Test
        @DisplayName("不存在時拋出 NotFoundException")
        // FR-{MODULE}-002: 例外情境 - 不存在
        void findById_notFound_throwsException() {
            UUID nonExistentId = UUID.randomUUID();
            given({entity}Repository.findById(nonExistentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> {business}Service.findById(nonExistentId))
                .isInstanceOf({Resource}NotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());
        }
    }

    // ── delete (softDelete) ──

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("軟刪除成功")
        // FR-{MODULE}-006
        void delete_success() {
            UUID id = sample{Entity}.getId();
            given({entity}Repository.findById(id)).willReturn(Optional.of(sample{Entity}));

            {business}Service.delete(id, UUID.randomUUID());

            assertThat(sample{Entity}.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("不存在時拋出 NotFoundException")
        void delete_notFound_throwsException() {
            UUID nonExistentId = UUID.randomUUID();
            given({entity}Repository.findById(nonExistentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> {business}Service.delete(nonExistentId, UUID.randomUUID()))
                .isInstanceOf({Resource}NotFoundException.class);
        }
    }

    // ── 業務規則邊界值測試 ──

    @ParameterizedTest
    @ValueSource(strings = {"{boundary_value_1}", "{boundary_value_2}"})
    @DisplayName("邊界值：{業務規則說明}")
    // FR-{MODULE}-001: 業務規則 - 邊界值
    void create_boundaryValue_{fieldName}(String {fieldName}) {
        // TODO: 依實際業務規則填寫邊界值測試
        throw new org.junit.jupiter.api.extension.TestAbortedException("待實作");
    }
}
```

---

## §4 Repository Test（@DataJpaTest）

```java
// src/test/java/com/{company}/{project}/repository/{Resource}RepositoryTest.java
package com.{company}.{project}.repository;

import com.{company}.{project}.domain.{Entity};
import com.{company}.{project}.fixture.{Resource}Fixture;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("{Resource} Repository Tests")
class {Resource}RepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private {Entity}Repository {entity}Repository;

    private {Entity} persisted{Entity};

    @BeforeEach
    void setUp() {
        persisted{Entity} = entityManager.persistAndFlush({Resource}Fixture.a{Entity}());
    }

    @Test
    @DisplayName("findById - 正常查詢")
    void findById_success() {
        Optional<{Entity}> result = {entity}Repository.findById(persisted{Entity}.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(persisted{Entity}.getId());
    }

    @Test
    @DisplayName("findBy{UniqueField} - 正常查詢")
    void findByUniqueField_success() {
        Optional<{Entity}> result =
            {entity}Repository.findBy{UniqueField}(persisted{Entity}.get{UniqueField}());

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findBy{UniqueField} - 不存在")
    void findByUniqueField_notFound() {
        Optional<{Entity}> result =
            {entity}Repository.findBy{UniqueField}("{nonExistentValue}");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsBy{UniqueField} - 存在時回傳 true")
    void existsByUniqueField_true() {
        boolean exists =
            {entity}Repository.existsBy{UniqueField}(persisted{Entity}.get{UniqueField}());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("searchByKeywordAndStatus - 關鍵字搜尋")
    void searchByKeyword_found() {
        Page<{Entity}> result = {entity}Repository.searchByKeywordAndStatus(
            "{keyword}",
            {Entity}.{Entity}Status.ACTIVE,
            PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("軟刪除後 findById 應回傳 empty")
    void softDelete_shouldBeFilteredOut() {
        persisted{Entity}.softDelete();
        entityManager.flush();

        Optional<{Entity}> result = {entity}Repository.findById(persisted{Entity}.getId());

        assertThat(result).isEmpty();    // @SQLRestriction("deleted_at IS NULL") 生效
    }

    @Test
    @DisplayName("審計欄位 createdAt/updatedAt 應自動填入")
    void auditFields_shouldBePopulated() {
        assertThat(persisted{Entity}.getCreatedAt()).isNotNull();
        assertThat(persisted{Entity}.getUpdatedAt()).isNotNull();
    }
}
```

---

## §5 測試資料工廠（Fixture）

```java
// src/test/java/com/{company}/{project}/fixture/{Resource}Fixture.java
package com.{company}.{project}.fixture;

import com.{company}.{project}.domain.{Entity};
import com.{company}.{project}.dto.request.{Resource}CreateRequest;
import com.{company}.{project}.dto.response.{Resource}Response;

import java.time.Instant;
import java.util.UUID;

/**
 * 測試資料工廠 — 提供各測試類別統一的 fixture，
 * 避免測試程式碼中散落 hard-coded 測試資料。
 */
public class {Resource}Fixture {

    private static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEFAULT_{FIELD} = "{default_value}";

    // ── Entity ──

    public static {Entity} a{Entity}() {
        return {Entity}.create(DEFAULT_{FIELD}, {default_field2_value});
    }

    public static {Entity} a{Entity}With({FieldType} {field}) {
        return {Entity}.create(DEFAULT_{FIELD}, {field});
    }

    // ── Request DTO ──

    public static {Resource}CreateRequest aCreateRequest() {
        return new {Resource}CreateRequest(DEFAULT_{FIELD}, {default_field2_value});
    }

    public static {Resource}CreateRequest anInvalidCreateRequest() {
        return new {Resource}CreateRequest(
            "",         // blank — 觸發 @NotBlank 驗證失敗
            null        // null — 觸發 @NotNull 驗證失敗
        );
    }

    // ── Response DTO ──

    public static {Resource}Response aDefault{Resource}() {
        return new {Resource}Response(
            DEFAULT_ID,
            DEFAULT_{FIELD},
            {default_field2_value},
            "ACTIVE",
            Instant.now(),
            Instant.now()
        );
    }
}
```

---

## §6 application-test.yml（測試環境設定）

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop       # 測試環境由 Hibernate 管理 schema
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.H2Dialect

  flyway:
    enabled: false                # 測試環境關閉 Flyway，改用 ddl-auto

  data:
    redis:
      host: localhost
      port: 6379

  cache:
    type: none                    # 測試環境停用 Cache，避免狀態汙染

app:
  jwt:
    secret: test-secret-key-for-testing-only-not-for-production
    access-token-expiry: 900
    refresh-token-expiry: 604800

logging:
  level:
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```
