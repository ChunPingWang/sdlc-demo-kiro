---
inclusion: always
---

# Java / Spring Boot 開發標準

> 本文件由 Kiro 在每次 session 自動載入，所有 Java/Spring Boot 程式碼產出（包含 `springboot-codegen` Skill 執行時）**必須完全遵守以下規範**，不得例外。

---

## 1. Package 命名規範（最高優先）

### 1.1 根 Package 結構

```
{groupId}.{artifactId}
  └─ com.{company}.{project}
```

| 部分 | 規則 | 範例 |
|------|------|------|
| `company` | 公司/組織英文縮寫，全小寫 | `example`, `acme`, `mybank` |
| `project` | 專案代碼，全小寫，僅含字母與數字 | `lifepremium`, `ecommerce`, `hrportal` |

> **package 名稱來源：** SD 文件第 3 章技術標準節 `§ Package Root` 欄位定義。  
> 若 SD 文件尚未建立，先與 Tech Lead 確認後寫入 SD，再開始 code gen。  
> **禁止** 在未讀取 SD 文件的情況下自行猜測 package 名稱。

### 1.2 分層子 Package

| 層次 | 子 Package | 範例完整路徑 |
|------|-----------|-------------|
| 啟動入口 | `{root}` | `com.example.lifepremium.LifePremiumApplication` |
| 領域模型 | `{root}.domain` | `com.example.lifepremium.domain.RateEntry` |
| 共用基底 | `{root}.domain.common` | `com.example.lifepremium.domain.common.BaseEntity` |
| 資料存取 | `{root}.repository` | `com.example.lifepremium.repository.RateEntryRepository` |
| 業務邏輯介面 | `{root}.service` | `com.example.lifepremium.service.PremiumCalculationService` |
| 業務邏輯實作 | `{root}.service.impl` | `com.example.lifepremium.service.impl.PremiumCalculationServiceImpl` |
| REST Controller | `{root}.controller` | `com.example.lifepremium.controller.PremiumCalculationController` |
| 請求 DTO | `{root}.dto.request` | `com.example.lifepremium.dto.request.PremiumCalculateRequest` |
| 回應 DTO | `{root}.dto.response` | `com.example.lifepremium.dto.response.PremiumCalculateResponse` |
| 例外 | `{root}.exception` | `com.example.lifepremium.exception.AgeOutOfRangeException` |
| 物件映射 | `{root}.mapper` | `com.example.lifepremium.mapper.PremiumMapper` |
| 設定 | `{root}.config` | `com.example.lifepremium.config.SecurityConfig` |
| 安全 | `{root}.security` | `com.example.lifepremium.security.JwtTokenProvider` |
| 領域事件（選用）| `{root}.event` | `com.example.lifepremium.event.CalculationCompletedEvent` |

### 1.3 禁止事項

- ❌ 禁止跨層直接依賴（Controller 不得直接呼叫 Repository）
- ❌ 禁止在 `domain` package 內 import Spring 框架（`@Component` 除外）
- ❌ 禁止在 `dto` package 內 import JPA Entity 型態
- ❌ 禁止使用 `util`, `helper`, `common`（根層）作為無意義的 package 名稱
- ❌ 禁止 package 名稱含大寫字母、連字號或底線

---

## 2. 類別命名規範

| 類型 | 規則 | 範例 |
|------|------|------|
| Entity | PascalCase，業務單數名詞 | `RateEntry`, `CalculationRecord` |
| Repository | `{Entity}Repository` | `RateEntryRepository` |
| Service 介面 | `{BusinessConcept}Service` | `PremiumCalculationService` |
| Service 實作 | `{BusinessConcept}ServiceImpl` | `PremiumCalculationServiceImpl` |
| Controller | `{Resource}Controller` | `PremiumCalculationController` |
| 建立 Request DTO | `{Resource}CreateRequest` | `RateTableCreateRequest` |
| 更新 Request DTO | `{Resource}UpdateRequest` | `RateTableUpdateRequest` |
| 上傳 Request DTO | `{Resource}UploadRequest` | `RateTableUploadRequest` |
| Response DTO | `{Resource}Response` | `PremiumCalculateResponse` |
| 統一回應包裝 | `ApiResponse<T>` | — |
| 分頁回應 | `PageResponse<T>` | — |
| Exception（業務）| `{BusinessReason}Exception` | `AgeOutOfRangeException` |
| Exception（基底）| `BusinessException` | — |
| 錯誤碼枚舉 | `ErrorCode` | — |
| 例外處理器 | `GlobalExceptionHandler` | — |
| Mapper | `{Resource}Mapper` | `PremiumMapper` |
| Config | `{Function}Config` | `SecurityConfig`, `JpaConfig` |
| Test 類別 | `{TargetClass}Test` | `PremiumCalculationServiceTest` |
| BDD Step Def | `{Feature}Steps` | `PremiumCalculationSteps` |
| Fixture | `{Resource}Fixture` | `PremiumCalculationFixture` |

---

## 3. 方法命名規範

| 操作類型 | 前綴 | 範例 |
|---------|------|------|
| 查詢單筆（存在或拋例外）| `get` | `getPremiumCalculation(UUID id)` |
| 查詢單筆（Optional）| `find` | `findByProductCode(String code)` |
| 查詢清單/分頁 | `list` / `search` | `listCalculationRecords(...)` |
| 建立 | `create` | `createRateTable(...)` |
| 更新 | `update` | `updateRateEntry(...)` |
| 刪除 | `delete` | `deleteRateTable(UUID id)` |
| 上傳 | `upload` | `uploadRateTable(...)` |
| 試算/計算 | `calculate` | `calculatePremium(...)` |
| 驗證（拋例外）| `validate` | `validateAge(int age)` |
| 存在性檢查 | `exists` | `existsByProductCode(String code)` |

---

## 4. 分層職責與禁止事項

### Controller 層

- ✅ 職責：HTTP 進出處理、`@Valid` 輸入驗證、回應格式化
- ✅ 從 `@AuthenticationPrincipal` 取得使用者資訊，傳入 Service
- ❌ 禁止：業務規則邏輯、直接存取 Repository、直接操作 Entity

### Service 層

- ✅ 職責：業務規則驗證、交易管理（`@Transactional`）、Entity ↔ DTO 轉換
- ✅ 讀取操作加 `@Transactional(readOnly = true)`
- ❌ 禁止：直接回傳 Entity（必須轉為 DTO）、HTTP 相關操作

### Repository 層

- ✅ 職責：資料存取、JPQL 查詢
- ✅ 複雜查詢使用 `@Query` + JPQL；禁止 Native SQL（有充分理由除外）
- ❌ 禁止：業務邏輯、DTO 轉換

### Domain (Entity) 層

- ✅ 職責：業務狀態封裝、JPA Mapping
- ✅ 使用靜態工廠方法（`create(...)`）代替 `new` + setter
- ✅ 狀態變更封裝為業務方法（`activate()`, `softDelete()` 等）
- ❌ 禁止：直接引用 DTO、Spring Bean、Repository

---

## 5. 注解使用規範

### Entity 強制注解

```java
@Entity
@Table(name = "{snake_case_plural}")    // 資料表名稱：snake_case 複數
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE {table} SET deleted_at = NOW() WHERE id = ?")  // 軟刪除
@SQLRestriction("deleted_at IS NULL")   // 預設過濾已刪除
```

### Service 強制注解

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)    // 類別層級預設唯讀
// 寫入方法個別加：
@Transactional                     // 覆蓋為讀寫
```

### Controller 強制注解

```java
@RestController
@RequestMapping("/api/v{N}/{resources}")   // 複數資源名稱
@RequiredArgsConstructor
@Tag(name = "...", description = "...")    // Swagger 文件
```

---

## 6. 測試規範

- 測試 package 結構必須與 main 完全對應
- 所有測試類別使用 `@ActiveProfiles("test")`
- Controller 測試：`@SpringBootTest` + `@AutoConfigureMockMvc`
- Service 測試：`@ExtendWith(MockitoExtension.class)`（不啟動 Spring Context）
- Repository 測試：`@DataJpaTest`（僅載入 JPA 相關 Bean）
- 測試資料統一由 `{Resource}Fixture` 工廠提供，禁止在測試方法內 hard-code 測試資料
- 每個測試方法使用 `@DisplayName` 描述測試意圖（中文或英文均可）
- 相關測試使用 `@Nested` class 分群

---

## 7. 違規處理

Kiro 在產出程式碼時，若發現以下情況，**必須停止並告知使用者**，而非自行決定：

1. SD 文件中找不到 `Package Root` 定義
2. 現有程式碼的 package 結構違反本規範
3. 使用者要求的命名與本規範衝突

告知格式：
```
⚠️  命名規範衝突
  問題：{描述衝突內容}
  規範要求：{規範說明}
  建議：{解決方案}
  請確認後繼續。
```
