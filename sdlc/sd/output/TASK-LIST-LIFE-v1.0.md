# 開發 Task List

**專案：** 壽險新保件保費試算系統  
**對應 SD：** SD-LIFE-v1.0.md  
**產出日期：** 2026-09-08  
**Package Root：** `com.example.lifepremium`  
**總計：** 42 個任務（測試 15 + 實作 27）

> 本 Task List 由 `generate-sd` Phase 2 產出，經架構師 HITL-2 確認後，  
> 供 `springboot-codegen` 依 Phase A（Red）→ Phase B（Green）順序驅動 code gen。

---

## Phase A：測試程式（Red）— code gen Phase 1

### A1. Cucumber Step Definitions

| # | 檔案路徑 | 對應 Feature | Step 數量 | FR 對應 |
|---|---------|------------|---------|--------|
| A1-01 | `bdd/steps/PremiumCalculationSteps.java` | `premium-calculation.feature` | 10 scenario | FR-CALC-001, FR-CALC-002 |
| A1-02 | `bdd/CucumberSpringConfiguration.java` | — | Spring 整合設定 | — |
| A1-03 | `bdd/CucumberTestRunner.java` | — | JUnit Platform Suite | — |

### A2. Controller Integration Tests

| # | 檔案路徑 | 測試方法 | 涵蓋情境 |
|---|---------|---------|--------|
| A2-01 | `controller/PremiumCalculationControllerTest.java` | `calculate_success_returns200` | 正常試算 200 |
| A2-02 | 同上 | `calculate_ageOutOfRange_returns422` | 年齡超限 422 |
| A2-03 | 同上 | `calculate_amountOutOfRange_returns422` | 保額超限 422 |
| A2-04 | 同上 | `calculate_invalidPaymentPeriod_returns422` | 繳費年期非法 422 |
| A2-05 | 同上 | `calculate_rateNotFound_returns404` | 查無費率 404 |
| A2-06 | 同上 | `calculate_blankProductCode_returns400` | 格式驗證失敗 400 |

### A3. Service Unit Tests

| # | 檔案路徑 | 測試方法 | 業務規則來源 |
|---|---------|---------|-----------|
| A3-01 | `service/PremiumCalculationServiceTest.java` | `calculate_validRequest_returnsCorrectPremium` | FR-CALC-001 主流程 + BR-004/005 |
| A3-02 | 同上 | `calculate_ageBelowZero_throwsAgeOutOfRange` | BR-001 邊界（下限）|
| A3-03 | 同上 | `calculate_ageAbove70_throwsAgeOutOfRange` | BR-001 邊界（上限）|
| A3-04 | 同上 | `calculate_amountOutOfRange_throwsException` | BR-002 |
| A3-05 | 同上 | `calculate_invalidPeriod_throwsException` | BR-003 |
| A3-06 | 同上 | `calculate_guestAgentId_doesNotSaveRecord` | FR-CALC-001 替代流程（訪客不保存）|

### A4. Repository Tests

| # | 檔案路徑 | 測試方法 | 對應查詢 |
|---|---------|---------|--------|
| A4-01 | `repository/RateEntryRepositoryTest.java` | `findEffectiveRate_returnsLatestActiveRate` | SD 7.2 費率查詢（最新生效版本）|
| A4-02 | 同上 | `findEffectiveRate_noMatch_returnsEmpty` | 查無費率 |

### A5. 測試輔助

| # | 檔案路徑 | 說明 |
|---|---------|------|
| A5-01 | `fixture/PremiumFixture.java` | 測試資料工廠（Request/Entity/費率）|
| A5-02 | `architecture/ArchitectureTest.java` | ArchUnit 架構規範測試（對照 java-coding-standards）|
| A5-03 | `resources/application-test.yml` | 測試環境設定（H2 + 停用 Cache）|

---

## Phase B：實作程式碼（Green）— code gen Phase 2

### B1. 資料層（Entity + Repository）

| # | 類別 | 檔案路徑 | 關鍵欄位 / 方法 | 來源 |
|---|------|---------|--------------|------|
| B1-01 | `BaseEntity` | `domain/common/BaseEntity.java` | id(UUID), createdAt, updatedAt | SD 7.2 |
| B1-02 | `Product` | `domain/Product.java` | productCode, productName, status(ACTIVE/INACTIVE) | SD 7.2 |
| B1-03 | `RateTableVersion` | `domain/RateTableVersion.java` | productCode, versionNumber, effectiveDate, status(PENDING/ACTIVE/SUPERSEDED) | SD 7.2 |
| B1-04 | `RateEntry` | `domain/RateEntry.java` | age, gender, paymentPeriod, rate；關聯: ManyToOne RateTableVersion | SD 7.2 |
| B1-05 | `CalculationRecord` | `domain/CalculationRecord.java` | agentId(nullable), 試算參數, annualPremium, monthlyPremium, status | SD 7.2 |
| B1-06 | `ProductRepository` | `repository/ProductRepository.java` | findByProductCode, existsByProductCode | SD 7.2 |
| B1-07 | `RateTableVersionRepository` | `repository/RateTableVersionRepository.java` | existsByProductCodeAndEffectiveDate, findTopBy...OrderBy..., countByProductCode | SD 7.2 索引 |
| B1-08 | `RateEntryRepository` | `repository/RateEntryRepository.java` | findEffectiveRate(JPQL) | SD 7.2 索引 |
| B1-09 | `CalculationRecordRepository` | `repository/CalculationRecordRepository.java` | findByAgentIdAndDateRange, findAllByDateRange | SD 7.2 索引 |

### B2. DTO 層

| # | 類別 | 類型 | 關鍵欄位 / 驗證規則 | 來源 |
|---|------|------|-----------------|------|
| B2-01 | `PremiumCalculateRequest` | Record | productCode(@NotBlank), age(@Min0/@Max120), gender(@Pattern M\|F), insuredAmount(@Positive), paymentPeriod | SD 8.3 |
| B2-02 | `RateTableUploadRequest` | Record | file(MultipartFile), productCode, effectiveDate | SD 8.3 |
| B2-03 | `PremiumCalculateResponse` | Record | calculationId, rateUsed, annualPremium, monthlyPremium | SD 8.3 |
| B2-04 | `RateTableVersionResponse` | Record | versionId, versionNumber, effectiveDate, status, entryCount | SD 8.3 |
| B2-05 | `CalculationRecordResponse` | Record | id, 試算參數, 保費結果, status, createdAt | SD 8.3 |
| B2-06 | `ApiResponse<T>` | Record | code, message, data, timestamp | SD 8.1 統一格式 |
| B2-07 | `PageResponse<T>` | Record | content, page, size, totalElements, totalPages, last | SD 8.2 分頁 |

### B3. 業務邏輯層（Service + Impl）

| # | 類別 | 檔案路徑 | 公開方法 | 關鍵業務規則 |
|---|------|---------|---------|-----------|
| B3-01 | `PremiumCalculationService` | `service/PremiumCalculationService.java` | calculate | 介面 |
| B3-02 | `PremiumCalculationServiceImpl` | `service/impl/PremiumCalculationServiceImpl.java` | calculate | BR-001~003 驗證；BR-004/005 計算；訪客不保存；@Cacheable 費率 |
| B3-03 | `RateTableService` | `service/RateTableService.java` | upload, listVersions | 介面 |
| B3-04 | `RateTableServiceImpl` | `service/impl/RateTableServiceImpl.java` | upload, listVersions | CSV 解析、批次匯入(100/批)、版本衝突檢查、快取清除 |
| B3-05 | `CalculationRecordService` | `service/CalculationRecordService.java` | query | 介面 |
| B3-06 | `CalculationRecordServiceImpl` | `service/impl/CalculationRecordServiceImpl.java` | query | Agent 只查自己 / Admin 查全部 |
| B3-07 | `RateQueryService` | `service/RateQueryService.java` + impl | findEffectiveRate | Cache-Aside 費率查詢封裝 |

### B4. API 層（Controller）

| # | 類別 | 路徑前綴 | Endpoint | 權限 |
|---|------|---------|---------|------|
| B4-01 | `PremiumCalculationController` | `/api/v1/premium` | POST /calculate | 選用（訪客可）|
| B4-02 | `RateTableController` | `/api/v1/rate-tables` | POST（上傳）, GET（版本清單）| ADMIN |
| B4-03 | `CalculationRecordController` | `/api/v1/calculation-records` | GET（查詢）| AGENT / ADMIN |

### B5. 例外處理

| # | 類別 | HTTP | 觸發條件 |
|---|------|------|--------|
| B5-01 | `BusinessException` | — | 抽象基底 |
| B5-02 | `ErrorCode` | — | 錯誤碼枚舉（9 項）|
| B5-03 | `AgeOutOfRangeException` | 422 | BR-001 年齡超限 |
| B5-04 | `AmountOutOfRangeException` | 422 | BR-002 保額超限 |
| B5-05 | `InvalidPaymentPeriodException` | 422 | BR-003 繳費年期非法 |
| B5-06 | `RateNotFoundException` | 404 | 查無費率 |
| B5-07 | `RateVersionConflictException` | 409 | 同商品同生效日衝突 |
| B5-08 | `InvalidCsvFormatException` | 400 | CSV 格式錯誤 |
| B5-09 | `ProductNotFoundException` | 404 | 查無商品 |
| B5-10 | `GlobalExceptionHandler` | — | @RestControllerAdvice 統一攔截 |

### B6. 設定類別

| # | 類別 | 功能 | 關鍵設定 |
|---|------|------|--------|
| B6-01 | `LifePremiumApplication` | Spring Boot 啟動入口 | @SpringBootApplication |
| B6-02 | `JpaConfig` | JPA 審計 | @EnableJpaAuditing |
| B6-03 | `CacheConfig` | Redis 快取 | rates 快取 TTL 1hr |
| B6-04 | `SecurityConfig` | Spring Security | 白名單: /premium/calculate；角色: AGENT/ADMIN |
| B6-05 | `OpenApiConfig` | Springdoc | Bearer Auth |

### B7. 資料庫遷移

| # | 檔案 | 說明 |
|---|------|------|
| B7-01 | `db/migration/V1__create_initial_schema.sql` | products/rate_table_versions/rate_entries/calculation_records + 索引 |

---

## 統計摘要

| 類別 | 測試檔案 | 實作檔案 | 小計 |
|------|---------|---------|------|
| Cucumber / BDD | 3 | — | 3 |
| Controller 測試 | 1（6 方法）| — | 1 |
| Service 測試 | 1（6 方法）| — | 1 |
| Repository 測試 | 1（2 方法）| — | 1 |
| 測試輔助（Fixture/ArchUnit/設定）| 3 | — | 3 |
| Entity | — | 5 | 5 |
| Repository | — | 4 | 4 |
| DTO | — | 7 | 7 |
| Service（介面+實作）| — | 7 | 7 |
| Controller | — | 3 | 3 |
| Exception | — | 10 | 10 |
| Config / 啟動 | — | 5 | 5 |
| DB Migration | — | 1 | 1 |
| **合計** | **9** | **42** | **51** |

---

## HITL-2 確認紀錄

| 確認項 | 狀態 |
|--------|------|
| 資料模型（Entity 欄位、關聯、索引）| ✅ 已確認 |
| API 設計（路徑、Method、權限）| ✅ 已確認 |
| 業務規則（BR-001~005 對應 Service）| ✅ 已確認 |
| 例外情境涵蓋所有 FR 例外處理 | ✅ 已確認 |
| 快取策略（rates TTL 1hr）| ✅ 已確認 |

*本 Task List 由 Kiro SDLC `generate-sd` skill 產生，對照 SD-LIFE-v1.0.md。*
