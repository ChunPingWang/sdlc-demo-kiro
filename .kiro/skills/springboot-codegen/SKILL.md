---
name: springboot-codegen
description: 以 TDD/BDD 方法為 Java/Spring Boot 專案產生程式碼。輸入來自 SD 文件（API 規格、資料表設計、C4 L3）與 FSD 的 Gherkin .feature 檔，依序產出測試程式（Red）→ 實作程式碼（Green）→ 重構提示（Refactor）。Use when generating Spring Boot code from SDLC documents using TDD/BDD approach.
metadata:
  author: kiro-sdlc
  version: "1.0"
  stage: Implementation
  methodology: TDD/BDD (Red → Green → Refactor)
---

## 概述

本 Skill 以 **TDD/BDD（Red → Green → Refactor）** 為核心方法論，從 SDLC 文件鏈產生 Java/Spring Boot 程式碼。

**輸入文件鏈：**

| 文件 | 路徑 | 用途 |
|------|------|------|
| Gherkin Feature | `sdlc/fsd/output/features/*.feature` | BDD 情境 → Cucumber Step Definitions |
| SD 文件 | `sdlc/sd/output/SD-{PROJECT_CODE}-*.md` | API / 資料表 / C4 L3 → 實作程式碼 |
| FSD 文件 | `sdlc/fsd/output/FSD-{PROJECT_CODE}-*.md` | FR 驗收標準 → 測試案例補充 |

> **⚠️ code gen 開始前必須先讀取 SD 文件第 3.2 節「技術標準宣告」，取得 `Package Root` 定義。**  
> 若 SD 文件不存在或 §3.2 為空，**必須停止並提示使用者補充後再繼續**，禁止自行推測 package 名稱。  
> 命名規範詳見 `.kiro/steering/java-coding-standards.md`（Kiro 自動載入，每次 session 強制生效）。

**輸出目錄：** `src/` 下標準 Spring Boot 專案結構（見 `references/project-structure.md`）

---

## 執行流程

### ── Phase 1：產出測試程式（Red）⏸ HITL

> 目標：在沒有任何實作的情況下，先讓所有測試「編譯成功但執行失敗（Red）」。

#### Step 1-1：讀取輸入文件

依序讀取以下文件，**不得跳過任何一項**：

1. `sdlc/fsd/output/features/*.feature` — 取得所有 Gherkin Scenario
2. `sdlc/sd/output/SD-*.md` 第 8 章 API 清單與規格 — 取得 Endpoint、Request/Response schema
3. `sdlc/sd/output/SD-*.md` 第 7 章資料設計 — 取得 Entity 欄位、索引、關聯
4. `sdlc/sd/output/SD-*.md` 第 4 章 C4 L3 元件圖 — 取得 Component 結構與依賴關係
5. `sdlc/fsd/output/FSD-*.md` 第 7 章功能需求 — 補充業務規則作為額外測試案例

#### Step 1-2：產出 Cucumber Step Definitions

對每個 `.feature` 檔，產出對應的 Step Definition 類別：

- 路徑：`src/test/java/{package}/bdd/steps/{Module}Steps.java`
- 遵循 `references/test-patterns.md` §1 Cucumber 範本
- 每個 Step 先拋出 `PendingException`（確保 Red 狀態）
- 在類別頂端以 `// FR: FR-{MODULE}-{N}` 標注對應需求編號

#### Step 1-3：產出 Controller Integration Tests

對 SD 文件第 8 章每個 Endpoint，產出 `@SpringBootTest` + `MockMvc` 測試：

- 路徑：`src/test/java/{package}/controller/{Resource}ControllerTest.java`
- 涵蓋：正常流程（2xx）、驗證失敗（400）、未認證（401）、未授權（403）、不存在（404）
- 遵循 `references/test-patterns.md` §2 Controller Integration Test 範本
- 此時 Controller 尚未實作，測試應編譯通過但執行失敗

#### Step 1-4：產出 Service Unit Tests

對每個 Service 元件（來自 C4 L3），產出 JUnit5 + Mockito 單元測試：

- 路徑：`src/test/java/{package}/service/{Business}ServiceTest.java`
- 涵蓋：每個 FR 的主要流程、替代流程、例外情境
- 業務規則驗證測試：來自 FSD 功能需求的驗收標準
- 遵循 `references/test-patterns.md` §3 Service Unit Test 範本

#### Step 1-5：產出 Repository Tests

對每個 Entity（來自 SD 資料設計），產出 `@DataJpaTest` 測試：

- 路徑：`src/test/java/{package}/repository/{Resource}RepositoryTest.java`
- 涵蓋：基本 CRUD、自訂查詢方法、索引欄位查詢、唯一性約束驗證
- 遵循 `references/test-patterns.md` §4 Repository Test 範本

#### Step 1-6：執行編譯驗證（確認 Red 狀態）

產出所有測試程式後，**立即執行以下指令**，驗證測試程式可編譯成功但執行失敗：

```bash
# Maven
./mvnw test-compile

# Gradle
./gradlew compileTestJava
```

**預期結果：** `BUILD SUCCESS`（僅編譯，尚未執行測試）

接著執行測試，**確認為 Red 狀態**（失敗是預期的）：

```bash
# Maven — 執行測試但允許失敗，取得失敗摘要
./mvnw test -Dmaven.test.failure.ignore=true 2>&1 | tail -30

# Gradle
./gradlew test --continue 2>&1 | tail -30
```

**Red 狀態判斷規則：**

| 狀況 | 代表意義 | 處置 |
|------|---------|------|
| 編譯成功 + 所有測試失敗 | ✅ 正確的 Red 狀態 | 繼續 HITL |
| 編譯失敗 | ❌ 測試程式有語法錯誤 | 修正後重新執行 Step 1-6 |
| 部分測試通過 | ⚠️ 可能有預設實作殘留 | 檢查 Production Code 是否乾淨 |

若編譯失敗，分析錯誤並修正，**直到編譯成功才進入 HITL**。

#### ⏸ HITL 確認點 — Phase 1

產出測試程式並確認 Red 狀態後，**停止並呈現以下確認清單，等待人工確認**：

```
═══════════════════════════════════════════════════════
  ⏸  Phase 1 完成 — 編譯 ✅  測試狀態：🔴 Red（預期）
═══════════════════════════════════════════════════════

🔴 編譯 & 測試結果：
  編譯：BUILD SUCCESS
  測試執行：{N} tests, {N} failed, 0 errors（Red — 符合預期）

📋 已產出測試檔案清單：
  □ Cucumber Step Definitions：
    - src/test/java/.../bdd/steps/{Module}Steps.java  ({N} steps)
  □ Controller Integration Tests：
    - src/test/java/.../controller/{Resource}ControllerTest.java  ({N} test cases)
  □ Service Unit Tests：
    - src/test/java/.../service/{Business}ServiceTest.java  ({N} test cases)
  □ Repository Tests：
    - src/test/java/.../repository/{Resource}RepositoryTest.java  ({N} test cases)

⚠️  請確認以下問題後再繼續：
  1. 業務規則是否正確反映於測試案例？
  2. 邊界值與例外情境是否完整？
  3. 測試資料（fixtures）是否符合業務情境？

✅  確認無誤後，請回覆「確認，繼續 Phase 2」
❌  如有修改需求，請說明，Kiro 將調整測試後重新執行 Step 1-6
```

---

### ── Phase 2：產出實作程式碼（Green）【自動執行，無需確認】

> 目標：依序產出 Production Code，讓 Phase 1 所有測試由 Red 轉為 Green。  
> **本階段全程自動執行，不停頓等待確認。每個 Step 產出後立即編譯驗證，持續修正直到所有測試通過。**

**產出順序（由內而外，依賴方向）：**

```
Entity → Repository → DTO → Exception → Service → Controller → Config
```

#### Step 2-1：產出 Entity 類別

- 路徑：`src/main/java/{package}/domain/{Entity}.java`
- 來源：SD 文件第 7.2 節資料模型
- 遵循 `references/code-patterns.md` §1 Entity 範本
- 規則：
  - 使用 `@Entity`, `@Table(name = "{table_name}")`
  - UUID 主鍵使用 `@GeneratedValue(strategy = GenerationType.UUID)`
  - 審計欄位使用 `@CreatedDate`, `@LastModifiedDate`（需啟用 `@EnableJpaAuditing`）
  - 軟刪除使用 `@SQLDelete` + `@Where(clause = "deleted_at IS NULL")`
  - 關聯使用 `FetchType.LAZY`（預設）

**產出後立即執行：**
```bash
./mvnw compile -pl . --also-make 2>&1 | grep -E "BUILD|ERROR"
# 或 Gradle：./gradlew compileJava 2>&1 | grep -E "BUILD|error"
```
若編譯失敗，修正 Entity 後再繼續。

#### Step 2-2：產出 Repository 介面

- 路徑：`src/main/java/{package}/repository/{Entity}Repository.java`
- 來源：SD 文件第 7.2 節索引與查詢需求
- 遵循 `references/code-patterns.md` §2 Repository 範本
- 規則：
  - 繼承 `JpaRepository<{Entity}, UUID>`
  - 自訂查詢優先使用 Spring Data 命名查詢；複雜查詢使用 `@Query` + JPQL
  - 分頁查詢返回 `Page<{Entity}>`

**產出後立即執行：**
```bash
./mvnw compile 2>&1 | grep -E "BUILD|ERROR"
```
同時執行 Repository Test（此時應仍為 Red，但確認無編譯錯誤）：
```bash
./mvnw test -Dtest="*RepositoryTest" -Dmaven.test.failure.ignore=true 2>&1 | tail -15
```

#### Step 2-3：產出 DTO 類別

- 路徑：
  - `src/main/java/{package}/dto/request/{Resource}CreateRequest.java`
  - `src/main/java/{package}/dto/request/{Resource}UpdateRequest.java`
  - `src/main/java/{package}/dto/response/{Resource}Response.java`
  - `src/main/java/{package}/dto/response/ApiResponse.java`
  - `src/main/java/{package}/dto/response/PageResponse.java`
- 來源：SD 文件第 8.3 節 API 詳細規格 Request/Response schema
- 遵循 `references/code-patterns.md` §3 DTO 範本
- 規則：
  - 使用 Java Record（Java 17+）
  - Request DTO 加上 Bean Validation 注解（`@NotNull`, `@Size`, `@Pattern` 等）
  - Response DTO 不對外暴露 Entity 直接引用

**產出後立即執行：**
```bash
./mvnw compile 2>&1 | grep -E "BUILD|ERROR"
```

#### Step 2-4：產出 Exception 類別

- 路徑：`src/main/java/{package}/exception/`
- 來源：SD 文件 8.3 節錯誤回應、FSD 例外處理
- 遵循 `references/code-patterns.md` §4 Exception 範本
- 必須產出：
  - `BusinessException.java`（抽象基底）
  - `{Resource}NotFoundException.java`
  - `{Resource}AlreadyExistsException.java`
  - `BusinessValidationException.java`
  - `ErrorCode.java`（枚舉）
  - `GlobalExceptionHandler.java`（`@RestControllerAdvice`）

**產出後立即執行：**
```bash
./mvnw compile 2>&1 | grep -E "BUILD|ERROR"
```

#### Step 2-5：產出 Mapper 與 Service 類別

- Mapper 路徑：`src/main/java/{package}/mapper/{Resource}Mapper.java`
- Service 介面：`src/main/java/{package}/service/{Business}Service.java`
- Service 實作：`src/main/java/{package}/service/impl/{Business}ServiceImpl.java`
- 來源：SD C4 L3 元件圖、FSD 功能需求業務規則
- 遵循 `references/code-patterns.md` §5/§6 Service 與 Mapper 範本
- 規則：
  - 先定義 `{Business}Service` **介面**，再實作 `{Business}ServiceImpl`
  - 寫入操作加 `@Transactional`；讀取操作加 `@Transactional(readOnly = true)`
  - 業務規則驗證在 Service 層集中處理，不在 Controller
  - 使用 MapStruct Mapper 轉換 Entity ↔ DTO

**產出後立即執行 Service Unit Tests：**
```bash
./mvnw test -Dtest="*ServiceTest" -Dmaven.test.failure.ignore=true 2>&1 | tail -20
```
Service Tests 此時應逐步由 Red 轉為 Green。若有失敗，分析錯誤並修正 Service 實作。

#### Step 2-6：產出 Controller 類別

- 路徑：`src/main/java/{package}/controller/{Resource}Controller.java`
- 來源：SD 文件第 8.2 節 API 清單
- 遵循 `references/code-patterns.md` §7 Controller 範本
- 規則：
  - 使用 `@RestController`, `@RequestMapping("/api/v1/{resources}")`
  - 輸入驗證用 `@Valid`；驗證失敗由 `GlobalExceptionHandler` 統一處理
  - Controller 不含業務邏輯，只做請求轉發與回應格式化
  - 認證資訊從 `@AuthenticationPrincipal` 取得

**產出後立即執行 Controller Integration Tests：**
```bash
./mvnw test -Dtest="*ControllerTest" -Dmaven.test.failure.ignore=true 2>&1 | tail -20
```
Controller Tests 應逐步由 Red 轉為 Green。若有失敗，分析錯誤並修正。

#### Step 2-7：產出 Config 類別

依需求產出以下 Config（以實際專案需求為準）：
- `SecurityConfig.java` — Spring Security + JWT Filter
- `JpaConfig.java` — `@EnableJpaAuditing`
- `OpenApiConfig.java` — Springdoc OpenAPI 文件設定
- `CacheConfig.java` — Redis Cache 設定（若有快取需求）

**產出後執行完整測試套件：**
```bash
# Maven（含 Cucumber BDD 測試）
./mvnw test 2>&1 | tail -30

# Gradle
./gradlew test 2>&1 | tail -30
```

**完整測試通過後，直接進入 Phase 3（無需確認）。**

若測試持續失敗，依以下順序診斷：
1. 讀取失敗的測試錯誤訊息
2. 對照 `references/code-patterns.md` 確認實作是否符合規範
3. 修正對應的 Production Code
4. 重新執行對應的測試類別（`-Dtest="FailingClassTest"`）
5. 重複直到 Green

#### Step 2-7 完成：自動進入 Phase 3

所有測試通過後，輸出測試結果摘要並直接進入 Phase 3：

```
🟢 Phase 2 完成 — 自動進入 Phase 3（Refactor）

  編譯：BUILD SUCCESS
  測試執行：{N} tests, 0 failed, 0 errors（Green ✅）

  ✅ Cucumber BDD：     {N}/{N} scenarios passed
  ✅ Controller Tests： {N}/{N} tests passed
  ✅ Service Tests：    {N}/{N} tests passed
  ✅ Repository Tests： {N}/{N} tests passed
```

---

### ── Phase 3：重構提示（Refactor）

> 目標：在所有測試 Green 的前提下，指出可改善點，**不自動修改**，由開發者決定。

產出 `REFACTOR-NOTES.md` 至專案根目錄，內容包含：

#### 3-1 程式碼異味偵測

- 重複邏輯（DRY 違反）
- 過長方法（> 20 行業務邏輯）
- 巢狀條件過深（> 3 層）
- Magic Number / Magic String

#### 3-2 可抽取介面 / 抽象

- 多個 Service 有相同 CRUD 骨架 → 建議 `BaseService<T, ID>` 泛型
- 重複的 Mapper 邏輯 → 建議 MapStruct `@Mapper`
- 重複的 Exception 處理 → 建議統一 Error Code 枚舉

#### 3-3 效能優化建議

- N+1 查詢問題（`@OneToMany` 沒有 `@BatchSize` 或 `JOIN FETCH`）
- 缺少快取的熱點查詢（對應 SD 文件快取策略）
- 可改為非同步的耗時操作（`@Async` + Event）

#### 3-4 測試覆蓋率缺口

- 指出哪些 FR 的邊界值未被測試覆蓋
- 指出缺少的例外情境測試

---

## 命名規範

> ⚠️ 本節為摘要參照。完整命名規範以 `.kiro/steering/java-coding-standards.md` 為準，  
> 每次 session 由 Kiro 自動載入強制執行。**兩者衝突時，以 Steering 為最終依據。**

### Package（最高優先）

**Package Root 必須從 SD 文件第 3.2 節「技術標準宣告」讀取，禁止自行猜測。**

```
{com.{company}.{projectCode}}              ← 從 SD §3.2 Package Root 欄位取得
├── domain / domain.common
├── repository
├── service / service.impl
├── controller
├── dto.request / dto.response
├── exception
├── mapper
├── config
└── security
```

### 類別與方法命名摘要

| 類型 | 規範 | 範例 |
|------|------|------|
| Entity | PascalCase，業務單數名詞 | `RateEntry`, `CalculationRecord` |
| Repository | `{Entity}Repository` | `RateEntryRepository` |
| Service 介面 | `{BusinessConcept}Service` | `PremiumCalculationService` |
| Service 實作 | `{BusinessConcept}ServiceImpl` | `PremiumCalculationServiceImpl` |
| Controller | `{Resource}Controller` | `PremiumCalculationController` |
| Request DTO | `{Resource}{Action}Request` | `PremiumCalculateRequest` |
| Response DTO | `{Resource}Response` | `PremiumCalculateResponse` |
| Exception | `{BusinessReason}Exception` | `AgeOutOfRangeException` |
| Test 類別 | `{TargetClass}Test` | `PremiumCalculationServiceTest` |
| Step Def | `{Feature}Steps` | `PremiumCalculationSteps` |

---

## 參考資源

- **強制規範：** `.kiro/steering/java-coding-standards.md`（Kiro 自動載入，最高優先）
- `references/project-structure.md` — 標準專案目錄結構
- `references/code-patterns.md` — Entity/Repository/Service/Controller/DTO/Exception 程式碼範本
- `references/test-patterns.md` — Cucumber/JUnit5/Mockito/@DataJpaTest 測試範本
- SD 輸入：`sdlc/sd/output/SD-{PROJECT_CODE}-*.md`（**必須先讀取 §3.2 取得 Package Root**）
- FSD 輸入：`sdlc/fsd/output/FSD-{PROJECT_CODE}-*.md`
- Gherkin 輸入：`sdlc/fsd/output/features/*.feature`
