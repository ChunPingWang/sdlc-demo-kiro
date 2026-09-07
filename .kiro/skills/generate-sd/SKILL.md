---
name: generate-sd
description: 根據 FSD 文件與技術架構相關文件，產出系統設計文件（SD），包含 C4 L3 元件圖、技術層循序圖、API 規格、資料表設計，最終產出開發 Task List 供架構師 HITL 確認後再進行 code gen。Use when generating a System Design Document from FSD and architecture documents.
metadata:
  author: kiro-sdlc
  version: "1.0"
  stage: SD
---

## 概述

本 Skill 以 FSD 文件為主要輸入，結合技術架構決策，產出完整的系統設計文件（SD）。  
**兩個 HITL 確認關卡** 確保架構師在大量程式碼產出前驗證設計正確性，避免方向錯誤導致的 token 浪費與重工。

```
輸入文件
  ├─ FSD-{CODE}-*.md（功能需求、C4 L1/L2、業務循序圖、Gherkin）
  ├─ 技術架構決策紀錄（ADR）
  └─ 企業技術標準規範

       │
       ▼
  Phase 1：產出 SD 文件本體
  （架構設計 + C4 L3 + 技術循序圖 + API + 資料表）
       │
       ▼ ⏸ HITL-1：SD 文件確認（架構師審查設計）
       │
       ▼
  Phase 2：產出開發 Task List
  （明確列出所有待產出類別、方法、決策）
       │
       ▼ ⏸ HITL-2：Task List 確認（架構師確認無誤）
       │
       ▼
  ✅ 進入 /springboot-codegen
```

---

## 輸入來源（Input）

| 輸入類型 | 路徑 / 說明 |
|---------|------------|
| FSD 文件 | `sdlc/fsd/output/FSD-{PROJECT_CODE}-*.md` |
| Gherkin Feature | `sdlc/fsd/output/features/*.feature` |
| ADR | 使用者提供或 `sdlc/inputs/adr-*.md` |
| 技術標準規範 | 使用者提供或 `sdlc/inputs/tech-standards.md` |

---

## Phase 1：產出 SD 文件本體

### Step 1-1：讀取並分析 FSD

依序讀取 FSD：
1. 第 4 章：系統概述（背景、目標、使用者族群）
2. 第 5 章：C4 L1 System Context + C4 L2 Container 圖（識別所有 Container 與外部系統）
3. 第 6 章：業務流程循序圖（識別跨 Container 的技術互動點）
4. 第 7 章：功能需求 FR 清單（識別業務規則、驗證條件、例外情境）
5. 第 8 章：非功能需求（效能、安全、可用性目標）
6. 第 9 章：整合需求（外部系統整合方式）
7. 第 13 章：Gherkin Feature（識別測試邊界，影響 Service 方法設計）

### Step 1-2：確認技術選型

若 ADR 已提供，直接採用；若未提供，依企業標準規範建議並在文件中標注 `⚠️ 待確認`：

| 面向 | 來源 |
|------|------|
| 後端框架 | ADR / 企業標準 |
| 資料庫 | ADR / 非功能需求（8.1 效能） |
| 快取 | ADR / 非功能需求（8.1 效能） |
| 訊息佇列 | ADR / FSD 整合需求（11.1） |
| 認證方式 | ADR / FSD 安全需求（8.2） |
| 部署平台 | ADR / FSD 可用性需求（8.3） |

### Step 1-3：填寫 SD 文件各章節

嚴格依照 `references/SD-template.md` 結構，**所有 `{PLACEHOLDER}` 必須替換**，不足資訊標注 `⚠️ 待確認`：

| 章節 | 來源 | 關鍵產出 |
|------|------|---------|
| 第 3 章：架構概觀 | ADR + FSD C4 L2 | 架構風格、ADR 決策表 |
| 第 4 章：C4 L3 元件圖 | FSD C4 L2 + FR 業務規則 | 每個 Container 的內部元件結構 |
| 第 5 章：技術層循序圖 | FSD 業務循序圖 → 對應技術實作 | 同步呼叫鏈、Event-Driven 非同步流程 |
| 第 6 章：模組設計 | C4 L3 + FR 對應關係 | 模組清單、依賴關係 |
| 第 7 章：資料設計 | FSD 資料實體 + 非功能需求 | ER 圖說明、欄位定義、索引、快取策略 |
| 第 8 章：API 設計 | FSD FR + 循序圖 | 完整 Endpoint 清單 + Request/Response schema |
| 第 9 章：安全設計 | FSD 安全需求 + ADR | JWT 設定、RBAC 權限矩陣 |
| 第 10 章：部署架構 | FSD 可用性需求 + ADR | 環境清單、容器化、CI/CD |
| 第 11-13 章：可觀測性/效能/錯誤處理 | FSD 非功能需求 | 監控指標、快取策略、Circuit Breaker |

### Step 1-4：輸出 SD 文件

存至：`sdlc/sd/output/SD-{PROJECT_CODE}-v{VERSION}.md`  
架構圖來源檔（PlantUML）存至：`sdlc/sd/output/assets/`

---

### ⏸ HITL-1：SD 文件確認

產出 SD 文件後**立即停止**，呈現以下確認清單：

```
══════════════════════════════════════════════════════
  ⏸  HITL-1：SD 文件完成 — 請架構師審查設計
══════════════════════════════════════════════════════

📄 SD 文件：sdlc/sd/output/SD-{PROJECT_CODE}-v{VERSION}.md

🏗️  架構決策摘要（請確認以下選型）：
  □ 架構模式：{Monolith / Microservices / ...}
  □ 後端框架：{Java/Spring Boot 3.x / ...}
  □ 資料庫：  {PostgreSQL xx / MySQL xx / ...}
  □ 快取：    {Redis xx / 無}
  □ 訊息佇列：{Kafka / RabbitMQ / 無}
  □ 認證：    {JWT / OAuth2 / ...}
  □ 部署：    {Docker + K8s / AWS ECS / ...}

📐 C4 L3 元件清單（請確認元件劃分合理）：
  □ {Container 1}：{Component A}, {Component B}, ...
  □ {Container 2}：{Component C}, {Component D}, ...

🔗 API 端點數量：{N} 個（見 SD 第 8 章）

💾 資料表數量：{N} 張（見 SD 第 7 章）

⚠️  待確認問題（需架構師決定）：
  1. {QUESTION_1}
  2. {QUESTION_2}

✅  確認無誤後，請回覆「確認，產出 Task List」
❌  若有修改，請說明修改項目，Kiro 將更新 SD 文件後重新確認
```

---

## Phase 2：產出開發 Task List

> 目標：將 SD 文件轉譯為明確的開發工作清單，讓架構師在 code gen 前確認 Kiro 的理解完全正確，  
> 避免因設計模糊導致產出錯誤程式碼，節省修正成本。

### Step 2-1：解析 SD 文件產出類別清單

從 SD 文件各章節提取所有需要產出的類別：

**提取規則：**
- C4 L3 每個 Component → 一個 Java 類別
- API 清單每個 Resource → Controller + Service 介面 + ServiceImpl + Mapper
- 資料表每張 → Entity + Repository
- Request/Response schema → DTO
- 錯誤碼表 → Exception 類別 + ErrorCode 枚舉
- 安全設計 → Security Config + JWT 相關類別

### Step 2-2：產出 Task List 文件

存至：`sdlc/sd/output/TASK-LIST-{PROJECT_CODE}-v{VERSION}.md`

---

**Task List 文件格式：**

```markdown
# 開發 Task List
**專案：** {PROJECT_NAME}
**對應 SD：** SD-{PROJECT_CODE}-v{VERSION}.md
**產出日期：** {DATE}
**總計：** {N} 個任務，預估 {N} 個類別

---

## Phase A：測試程式（Red）— code gen Phase 1

### A1. Cucumber Step Definitions

| # | 檔案路徑 | 對應 Feature | Step 數量 | FR 對應 |
|---|---------|------------|---------|--------|
| A1-01 | `src/test/.../bdd/steps/{Module}Steps.java` | `{feature}.feature` | {N} | {FR_IDS} |

### A2. Controller Integration Tests

| # | 檔案路徑 | 測試方法 | 涵蓋情境 |
|---|---------|---------|--------|
| A2-01 | `src/test/.../controller/{Resource}ControllerTest.java` | `should_return_201_when_create_success()` | 正常建立 |
| A2-02 | `src/test/.../controller/{Resource}ControllerTest.java` | `should_return_400_when_field_blank()` | 欄位驗證失敗 |
| A2-03 | `src/test/.../controller/{Resource}ControllerTest.java` | `should_return_401_when_no_token()` | 未認證 |
| A2-04 | `src/test/.../controller/{Resource}ControllerTest.java` | `should_return_404_when_not_found()` | 資源不存在 |

### A3. Service Unit Tests

| # | 檔案路徑 | 測試方法 | 業務規則來源 |
|---|---------|---------|-----------|
| A3-01 | `src/test/.../service/{Business}ServiceTest.java` | `create_should_throw_when_duplicate_{field}()` | FR-{X}-{N} 唯一性規則 |
| A3-02 | `src/test/.../service/{Business}ServiceTest.java` | `findById_should_throw_NotFoundException_when_not_exist()` | 例外處理 |

### A4. Repository Tests

| # | 檔案路徑 | 測試方法 | 對應查詢 |
|---|---------|---------|--------|
| A4-01 | `src/test/.../repository/{Resource}RepositoryTest.java` | `findBy{Field}_should_return_empty_when_not_exist()` | SD 7.2 索引查詢 |

---

## Phase B：實作程式碼（Green）— code gen Phase 2

### B1. 資料層

| # | 類別名稱 | 檔案路徑 | 關鍵欄位 / 方法 | 來源章節 |
|---|---------|---------|--------------|--------|
| B1-01 | `BaseEntity` | `domain/common/BaseEntity.java` | id(UUID), createdAt, updatedAt, deletedAt | SD 7.2 |
| B1-02 | `{Entity}` | `domain/{Entity}.java` | {欄位清單} \| status enum: {VALUES} \| 關聯: {RELATIONS} | SD 7.2 |
| B1-03 | `{Entity}Repository` | `repository/{Entity}Repository.java` | findBy{Field}, searchByKeyword(Page), findByIdWithChildren | SD 7.2 索引 |

### B2. DTO 層

| # | 類別名稱 | 類型 | 關鍵欄位 / 驗證規則 | 來源章節 |
|---|---------|------|-----------------|--------|
| B2-01 | `{Resource}CreateRequest` | Record | {field1}(@NotBlank), {field2}(@Positive) | SD 8.3 Request |
| B2-02 | `{Resource}Response` | Record | id, {fields}, createdAt | SD 8.3 Response |
| B2-03 | `ApiResponse<T>` | Record | code, message, data, timestamp | SD 8.3 統一格式 |
| B2-04 | `PageResponse<T>` | Record | content, page, size, totalElements | SD 8.2 分頁 |

### B3. 業務邏輯層

| # | 類別名稱 | 檔案路徑 | 公開方法簽章 | 關鍵業務規則 |
|---|---------|---------|-----------|-----------|
| B3-01 | `{Business}Service`（介面） | `service/{Business}Service.java` | create / findById / findAll / update / delete | — |
| B3-02 | `{Business}ServiceImpl` | `service/impl/{Business}ServiceImpl.java` | 同上 | 唯一性驗證({field})；軟刪除；Cache-Aside(TTL={N}s) |
| B3-03 | `{Resource}Mapper` | `mapper/{Resource}Mapper.java` | toEntity / toResponse / updateEntityFromRequest | MapStruct |

### B4. API 層

| # | 類別名稱 | 路徑前綴 | Endpoint 清單 | 權限 |
|---|---------|---------|-------------|------|
| B4-01 | `{Resource}Controller` | `/api/v1/{resources}` | GET / GET{id} / POST / PUT{id} / DELETE{id} | {ROLE_LIST} |

### B5. 例外處理

| # | 類別名稱 | HTTP 狀態碼 | 觸發條件 |
|---|---------|-----------|--------|
| B5-01 | `{Resource}NotFoundException` | 404 | findById / update / delete 找不到 |
| B5-02 | `{Resource}AlreadyExistsException` | 409 | create 時 {uniqueField} 重複 |
| B5-03 | `BusinessValidationException` | 422 | {業務規則驗證失敗條件} |
| B5-04 | `GlobalExceptionHandler` | — | 統一攔截所有例外 |
| B5-05 | `ErrorCode`（枚舉） | — | {N} 個錯誤碼 |

### B6. 設定類別

| # | 類別名稱 | 功能 | 關鍵設定 |
|---|---------|------|--------|
| B6-01 | `SecurityConfig` | Spring Security + JWT | 白名單路徑: {PATHS}；角色: {ROLES} |
| B6-02 | `JpaConfig` | @EnableJpaAuditing | — |
| B6-03 | `CacheConfig` | Redis Cache | TTL={N}s；Key 前綴={prefix} |
| B6-04 | `OpenApiConfig` | Springdoc | Bearer Auth；Server URL |

---

## Phase C：重構提示（Refactor）— code gen Phase 3

> Kiro 將在 Green 通過後自動分析並產出 `REFACTOR-NOTES.md`，不需要在此列任務。

---

## 統計摘要

| 類別 | 測試檔案數 | 實作檔案數 | 總計 |
|------|---------|---------|------|
| Cucumber Steps | {N} | — | {N} |
| Controller Tests | {N} | {N} | {N} |
| Service Tests | {N} | {N} | {N} |
| Repository Tests | {N} | {N} | {N} |
| Entity / Repository | — | {N} | {N} |
| DTO | — | {N} | {N} |
| Service（介面+實作） | — | {N} | {N} |
| Mapper | — | {N} | {N} |
| Exception | — | {N} | {N} |
| Config | — | {N} | {N} |
| **合計** | **{N}** | **{N}** | **{N}** |
```

---

### ⏸ HITL-2：Task List 確認

產出 Task List 後**立即停止**，呈現以下確認清單：

```
══════════════════════════════════════════════════════════════
  ⏸  HITL-2：Task List 完成 — 請架構師確認後再進行 Code Gen
══════════════════════════════════════════════════════════════

📋 Task List：sdlc/sd/output/TASK-LIST-{PROJECT_CODE}-v{VERSION}.md

📊 統計：
  • 測試檔案：{N} 個（{N} 個測試方法）
  • 實作檔案：{N} 個
  • 合計：{N} 個類別

🔍 請重點確認以下項目：

  【資料模型】
  □ Entity 欄位與型別是否正確？
  □ 關聯（OneToMany / ManyToOne）方向是否正確？
  □ 索引設計是否符合查詢需求？

  【API 設計】
  □ Endpoint 路徑與 HTTP Method 是否正確？
  □ Request / Response 欄位是否完整？
  □ 權限設定（RBAC）是否符合需求？

  【業務規則】
  □ Service 的業務規則（唯一性、狀態機、權限）是否完整？
  □ 例外情境是否涵蓋所有 FR 的例外處理？

  【技術決策】
  □ 快取 TTL 設定是否合理？
  □ 交易邊界（@Transactional）範圍是否正確？
  □ 非同步流程（Event）設計是否符合預期？

⚠️  待確認問題：
  1. {PENDING_QUESTION_1}
  2. {PENDING_QUESTION_2}

✅  全部確認無誤後，請執行：/springboot-codegen
❌  若有修改，請說明修改項目，Kiro 將更新 Task List（不需重新產 SD）
```

---

## 輸出清單

| 產出物 | 路徑 | 說明 |
|-------|------|------|
| SD 文件 | `sdlc/sd/output/SD-{PROJECT_CODE}-v{VERSION}.md` | Phase 1 產出 |
| PlantUML 來源 | `sdlc/sd/output/assets/*.puml` | C4 L3 + 技術循序圖 |
| Task List | `sdlc/sd/output/TASK-LIST-{PROJECT_CODE}-v{VERSION}.md` | Phase 2 產出，HITL-2 確認用 |

---

## 參考資源

- `references/SD-template.md` — SD 文件章節結構範本
- `references/SD-word-style-guide.md` — Word 套版轉換規範
- FSD 輸入：`sdlc/fsd/output/FSD-{PROJECT_CODE}-*.md`
- Gherkin 輸入：`sdlc/fsd/output/features/*.feature`
