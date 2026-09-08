# sdlc-demo-kiro

以**壽險新保件保費試算（Life Premium）** 為 MVP，端對端驗證 Kiro SDLC 自動化工作流程：從需求文件到可執行的 Spring Boot 程式碼，全程由 Kiro Skills 驅動，關鍵節點由人工確認（HITL）。

---

## 快速導覽

```
sdlc-demo-kiro/
├── sdlc/                        # SDLC 文件（需求 → FSD → SD → ADR → Task List → 測試報告）
│   ├── inputs/                  # 原始需求
│   ├── fsd/                     # 功能規格文件 + Gherkin
│   ├── sd/                      # 系統設計文件 + Task List
│   ├── adr/                     # 架構決策紀錄（輸出；HITL-1 審核）
│   └── test/                    # 測試報告模板與輸出
│
├── src/                         # Spring Boot 實作（由 /springboot-codegen 產出）
│   ├── main/java/com/example/lifepremium/
│   └── test/java/com/example/lifepremium/
│                                #   含 architecture/ArchitectureTest.java（ArchUnit）
│
├── .kiro/
│   ├── skills/                  # Kiro Skills（7 個）
│   │   ├── doc-to-markdown/     # ① 前置：文件轉 Markdown（本地）
│   │   ├── generate-fsd/        # ② FSD + Gherkin 產出
│   │   ├── generate-sd/         # ③ SD + Task List 產出
│   │   ├── springboot-codegen/  # ④ TDD/BDD Code Gen
│   │   ├── test-report/         # ⑤ 彙整測試報告
│   │   ├── code-review/         # ⑥ ArchUnit + LLM 分層審查
│   │   └── markdown-to-word/    # ⑦ Word 套版輸出
│   └── steering/                # 開發標準（每次 session 自動載入）
│       └── java-coding-standards.md
│
├── pom.xml                      # Maven 建置設定（含 ArchUnit）
└── mvnw / mvnw.cmd              # Maven Wrapper
```

---

## MVP 專案：壽險保費試算

### 業務情境

壽險業務員或訪客輸入被保人基本資料（年齡、性別、保額、繳費年期），系統即時計算年繳與月繳保費，協助投保決策。

### 核心業務規則

| 規則 | 說明 |
|------|------|
| BR-001 | 被保人年齡 0～70 歲 |
| BR-002 | 保額 100～5,000 萬元 |
| BR-003 | 繳費年期：10 / 20 / 30 / 99 年 |
| BR-004 | 年繳保費 = ROUND(保額 ÷ 1000 × 費率) |
| BR-005 | 月繳保費 = ROUND(年繳 ÷ 12 × 1.03) |

### 計算範例

| 條件 | 值 |
|------|---|
| 商品 | LIFE-WL-01（終身壽險）|
| 年齡 / 性別 | 35 歲 / 男性 |
| 保額 | 1,000 萬元 |
| 繳費年期 | 20 年 |
| 費率 | 12.50（每千元）|
| **年繳保費** | **125,000 元** |
| **月繳保費** | **10,729 元** |

---

## SDLC 工作流程

### 整體流程圖

```
原始文件（PDF/Word/Excel）
        │  ▼  /doc-to-markdown          本地轉換，不上雲端
需求文件（inputs/）
        │
        ▼  /generate-fsd
   FSD 文件 + C4 L1/L2 + Gherkin .feature
        │  ⏸ HITL：確認 FSD 主體
        │  ⏸ HITL：確認 Gherkin 情境
        ▼  /generate-sd
      SD 文件（C4 L3 + API + 資料表）
      ADR 草稿（Proposed，sdlc/adr/output/）
        │  ⏸ HITL-1（雙向）：架構師輸入決策 + 審核核准 ADR（→ Accepted）
      Task List（TASK-LIST-*.md）
        │  ⏸ HITL-2：架構師確認任務清單
        ▼  /springboot-codegen         依 Task List 順序驅動
   測試程式（🔴 Red）
        │  ⏸ HITL：確認測試案例
   實作程式碼（🟢 Green）→ REFACTOR-NOTES.md
        │
        ▼  /test-report                彙整 surefire/cucumber/jacoco
   測試報告（sdlc/test/output/）
        │
        ▼  /code-review                ArchUnit 結構檢查 + LLM 語意審查
   Code Review 報告
        │
        ▼  /markdown-to-word           套用公司 Word 樣板
   FSD.docx / SD.docx
```

> **Steering 全程生效：** `.kiro/steering/java-coding-standards.md` 於每次 session 自動載入，  
> 強制 code gen 與 code review 遵守 package 命名、分層職責、注解規範。

### 產出文件清單

| 階段 | 文件 | 路徑 |
|------|------|------|
| 需求 | 業務需求描述 | `sdlc/inputs/LIFE-PREMIUM-requirements.md` |
| FSD | 功能規格文件 | `sdlc/fsd/output/FSD-LIFE-v1.0.md` |
| FSD | Gherkin 測試案例 | `sdlc/fsd/output/features/premium-calculation.feature` |
| SD | 系統設計文件 | `sdlc/sd/output/SD-LIFE-v1.0.md` |
| ADR | 架構決策紀錄（6 筆）| `sdlc/adr/output/ADR-0001~0006-*.md` |
| ADR | 決策日誌索引 | `sdlc/adr/README.md` |
| SD | 開發 Task List | `sdlc/sd/output/TASK-LIST-LIFE-v1.0.md` |
| 測試 | 測試報告模板 | `sdlc/test/templates/TEST-REPORT-template.md` |
| 標準 | Java 開發標準（Steering）| `.kiro/steering/java-coding-standards.md` |

### 程式碼產出清單

| 類型 | 檔案 |
|------|------|
| Entity | `Product`, `RateTableVersion`, `RateEntry`, `CalculationRecord` |
| Repository | `ProductRepository`, `RateEntryRepository`, `CalculationRecordRepository`, `RateTableVersionRepository` |
| Service | `PremiumCalculationService`, `RateTableService`, `CalculationRecordService` |
| Controller | `PremiumCalculationController`, `RateTableController`, `CalculationRecordController` |
| Exception | `AgeOutOfRangeException`, `AmountOutOfRangeException`, `InvalidPaymentPeriodException`, `RateNotFoundException` + `GlobalExceptionHandler` |
| Test | `PremiumCalculationServiceTest`, `PremiumCalculationControllerTest`, `RateEntryRepositoryTest` |
| BDD | `PremiumCalculationSteps` + Cucumber Runner |
| 架構測試 | `architecture/ArchitectureTest.java`（ArchUnit：分層依賴、命名慣例、循環依賴）|

---

## 快速開始

### 前置需求

- Java 17+
- Maven 3.9+
- Docker（本機開發用 PostgreSQL + Redis）

### 啟動本機服務

```bash
docker-compose up -d
```

### 執行測試

```bash
# 單元測試
./mvnw test -Dgroups="unit"

# 整合測試
./mvnw test -Dgroups="integration"

# BDD 測試（Cucumber）
./mvnw test -Dtest="CucumberTestRunner"

# 架構測試（ArchUnit）
./mvnw test -Dtest="ArchitectureTest"

# 全部測試 + 覆蓋率報告
./mvnw test

# 全部測試（Windows PowerShell）
.\mvnw.cmd test
```

> **Windows 家目錄含空格的 workaround：** 若家目錄路徑含空格（如 `C:\Users\Rex Wang`），
> Maven Wrapper 可能無法啟動。改用本機已安裝的 Maven 直接執行，或將 Maven 安裝於無空格路徑後以
> `mvn -B test` 執行。

### 啟動應用程式

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

API 文件：http://localhost:8080/swagger-ui.html

### 試算 API 範例

```bash
curl -X POST http://localhost:8080/api/v1/premium/calculate \
  -H "Content-Type: application/json" \
  -H "X-Agent-Id: 00000000-0000-0000-0000-000000000001" \
  -d '{
    "productCode":    "LIFE-WL-01",
    "age":            35,
    "gender":         "M",
    "insuredAmount":  1000,
    "paymentPeriod":  20
  }'
```

預期回應：

```json
{
  "code": "SUCCESS",
  "data": {
    "productCode":    "LIFE-WL-01",
    "insuredAmount":  1000,
    "paymentPeriod":  20,
    "rateUsed":       12.50,
    "annualPremium":  125000,
    "monthlyPremium": 10729
  },
  "timestamp": "2026-09-07T03:00:00Z"
}
```

---

## Kiro Skills 原理與說明

### 什麼是 Kiro Skill？

Kiro Skill 是遵循 [Agent Skills 開放標準](https://agentskills.io) 的**可攜式指令套件**，讓 Kiro AI 在執行任務時擁有結構化的工作流程知識。

**核心設計哲學：漸進式揭露（Progressive Disclosure）**

```
Session 啟動
    │
    ▼  載入輕量索引（name + description）
   ┌─────────────────────────────────┐
   │  skill: doc-to-markdown         │  ← 只讀 description，幾乎無 token 消耗
   │  skill: generate-fsd            │
   │  skill: generate-sd             │
   │  skill: springboot-codegen      │
   │  skill: test-report             │
   │  skill: code-review             │
   │  skill: markdown-to-word        │
   └─────────────────────────────────┘
    │
    │  使用者輸入匹配或輸入 /skill-name
    ▼  完整 SKILL.md 載入至 Context
   ┌─────────────────────────────────┐
   │  完整工作流程指引 (SKILL.md)      │  ← 按需載入，節省 Context 空間
   │  + references/ 參考文件         │  ← 指令指向才讀取
   └─────────────────────────────────┘
```

**Skill 的三種啟動方式：**

| 方式 | 範例 | 說明 |
|------|------|------|
| 自動匹配 | 輸入「幫我產 FSD」 | description 與使用者意圖語意匹配 |
| 斜線指令 | `/generate-fsd` | 明確呼叫，立即啟動 |
| 帶參數呼叫 | `/generate-fsd #需求文件.md` | 搭配 `#File` 傳入輸入文件 |

**Skill 的構成要素：**

```
.kiro/skills/{skill-name}/
├── SKILL.md              # 核心：YAML frontmatter（name, description）+ 工作流程指引
└── references/           # 延伸資料：範本、樣式指南、程式碼範本（按需載入）
    ├── {template}.md
    └── {pattern}.md
```

**Skill vs. 其他 Kiro 機制對比：**

| 機制 | 載入時機 | 用途 | 可攜性 |
|------|---------|------|--------|
| **Skill** | 按需（匹配或明確呼叫）| 可重用的工作流程 | ✅ 跨專案 |
| **Steering** | 固定 / 條件觸發 | 專案規範、團隊標準 | ❌ 專案綁定 |
| **Hook** | IDE 事件驅動 | 自動化觸發（存檔、提交）| ❌ 專案綁定 |

---

### SDLC 流程與 Skill 對應

本專案的完整 SDLC 流程由 **7 個 Skills** 協作完成，各自負責不同階段：

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SDLC 端對端工作流程                                │
│                                                                     │
│  原始文件（PDF/Word/Excel）                                           │
│       ▼  ① /doc-to-markdown        本地轉換，不上雲端                 │
│  sdlc/inputs/*.md                                                    │
│       ▼  ② /generate-fsd                                            │
│  FSD + C4 L1/L2 + Gherkin          ⏸ HITL：FSD 主體 / Gherkin       │
│       ▼  ③ /generate-sd                                             │
│  SD + C4 L3 + API + 資料表                                          │
│  ADR 草稿（Proposed）              ⏸ HITL-1（雙向）：輸入+審核 ADR    │
│  Task List（TASK-LIST-*.md）        ⏸ HITL-2：架構師確認任務         │
│       ▼  ④ /springboot-codegen     依 Task List 順序驅動            │
│  測試（🔴 Red）                     ⏸ HITL：測試案例                 │
│  實作（🟢 Green）+ REFACTOR-NOTES   自動執行                          │
│       ▼  ⑤ /test-report            彙整 surefire/cucumber/jacoco    │
│  測試報告（sdlc/test/output/）                                       │
│       ▼  ⑥ /code-review            ArchUnit 結構 + LLM 語意          │
│  Code Review 報告                                                    │
│       ▼  ⑦ /markdown-to-word       套用公司 Word 樣板               │
│  FSD.docx / SD.docx                                                  │
│                                                                     │
│  ▲ Steering: java-coding-standards.md 全程自動載入，強制遵守規範      │
└─────────────────────────────────────────────────────────────────────┘
```

---

### ① `doc-to-markdown` — 前置文件轉換

**觸發：** `/doc-to-markdown`  
**輸入：** `sdlc/inputs/raw/*.{pdf,docx,xlsx,pptx}`  
**輸出：** `sdlc/inputs/*.md`

企業文件往往以 PDF、Word、Excel 形式交付。此 Skill 在**本地端**將各種格式轉換為 Markdown，再交由後續 Skill 處理，降低 token 消耗、保護文件機密性。

| 輸入格式 | 工具 | 說明 |
|---------|------|------|
| PDF（有文字層） | Docling | 表格、標題結構萃取最佳 |
| PDF（掃描件）| Docling + OCR | 內建 EasyOCR pipeline |
| Word / Excel / PPT | MarkItDown | 速度快，Office 結構保留良好 |
| 批次多格式 | `scripts/convert.py` | 自動判斷類型 |

```
/doc-to-markdown #sdlc/inputs/raw/需求訪談紀錄.pdf
```

---

### ② `generate-fsd` — 功能規格文件產出

**觸發：** `/generate-fsd`  
**輸入：** 需求文件、User Story、PRD 或原始碼  
**輸出：** `sdlc/fsd/output/FSD-{CODE}-v{N}.md` + `.feature` 檔

依據 FSD 模板產出完整功能規格文件，並附帶 **C4 Model** 與 **Gherkin 測試案例**。採分階段 HITL 確認：

| Phase | 產出內容 | HITL |
|-------|---------|------|
| Phase 1 | FSD 主體 + C4 L1 System Context + C4 L2 Container + 業務循序圖 | ⏸ 確認架構與功能正確性 |
| Phase 2 | Gherkin `.feature` 檔（BDD 測試情境）| ⏸ 確認測試案例是否覆蓋所有驗收標準 |

**Gherkin 標籤策略：**

```gherkin
@smoke       # 每次部署後執行的冒煙測試
@regression  # 每日 CI 完整迴歸
@happy-path  # 正常流程
@boundary    # 邊界值
@error-handling  # 例外情境
@wip         # 開發中，暫不執行
```

```
/generate-fsd #sdlc/inputs/LIFE-PREMIUM-requirements.md
```

---

### ③ `generate-sd` — 系統設計文件產出

**觸發：** `/generate-sd`  
**輸入：** FSD 文件（+ 先前已 `Accepted` 的 ADR，若有）  
**輸出：** `sdlc/sd/output/SD-{CODE}-v{N}.md` + `ADR-NNNN-*.md` + `TASK-LIST-{CODE}-v{N}.md`

以 FSD 為輸入，產出完整系統設計文件，著重技術實作細節：

| 章節 | 內容 | 對應 FSD |
|------|------|---------|
| C4 L3 Component | 各 Container 內部元件、依賴關係 | 延伸 C4 L2 |
| 技術循序圖 | 服務間呼叫鏈、非同步事件流 | 業務循序圖的技術實作面 |
| API 規格 | 完整 Request/Response schema、錯誤碼 | 功能需求 FR |
| 資料表設計 | 欄位定義、索引、關聯、快取策略 | 資料需求 |
| 安全設計 | RBAC 角色矩陣、資料加密、防護措施 | 非功能需求 |

採兩階段 HITL：

| Phase | 產出 | HITL |
|-------|------|------|
| Phase 1 | SD 文件本體 + **ADR 草稿（Proposed）** | ⏸ HITL-1（雙向）：架構師**輸入決策** + **審核核准** ADR |
| Phase 2 | 開發 Task List（列出所有待產出類別、方法、決策）| ⏸ HITL-2：確認任務清單再進 code gen |

> **ADR 是輸出、HITL-1 是雙向：** 架構決策的主體是「人」，AI 不自行拍板。  
> `generate-sd` 先提出候選方案（含 tradeoff）起草 `Proposed` ADR，架構師在 HITL-1 **輸入決策**  
> 並**審核核准**後，狀態轉 `Accepted` 並輸出至 `sdlc/adr/output/`。SD §3.3 僅保留 ADR 索引，  
> 完整背景／替代方案／影響記錄於各 ADR 檔。詳見 [`sdlc/adr/README.md`](sdlc/adr/README.md)。

> **Task List 的意義：** 在大量程式碼產出前，先讓架構師確認 Kiro 的理解正確，  
> 避免方向錯誤造成的重工與 token 浪費。`springboot-codegen` 依此 Task List 的順序驅動。

```
/generate-sd #sdlc/fsd/output/FSD-LIFE-v1.0.md
```

---

### ④ `springboot-codegen` — TDD/BDD 程式碼產出

**觸發：** `/springboot-codegen`  
**輸入：** Task List（`TASK-LIST-*.md`）+ SD 文件（C4 L3 + API + 資料表）+ Gherkin `.feature` 檔  
**輸出：** `src/` 下完整 Spring Boot 專案程式碼

**依 Task List 的先後順序驅動**（HITL-2 確認過的清單），遵循 **Red → Green → Refactor** 方法論，分三個 Phase 執行。過程會**實際執行 Maven 編譯與測試**，直到全數通過：

**Phase 1：產出測試程式（🔴 Red）**

先寫測試，不寫實作。確保測試編譯通過但執行失敗。

| 測試類型 | 來源 | 路徑 |
|---------|------|------|
| Cucumber Step Definitions | Gherkin `.feature` | `bdd/steps/` |
| Controller Integration Test | SD API 規格 | `controller/*ControllerTest` |
| Service Unit Test | FSD 業務規則 + FR | `service/*ServiceTest` |
| Repository Test | SD 資料表設計 | `repository/*RepositoryTest` |

⏸ **HITL**：確認測試案例正確反映業務規則與邊界值

**Phase 2：產出實作程式碼（🟢 Green）【自動執行，無需確認】**

依賴方向由內而外逐層產出，每步驟後即時編譯驗證：

```
Entity → Repository → DTO → Exception → Service → Controller → Config
```

每個 Step 產出後立即執行對應測試，持續修正直到全數 Green。

**Phase 3：重構建議（Refactor）**

輸出 `REFACTOR-NOTES.md`，指出程式碼異味、可抽象的介面、N+1 查詢問題、測試覆蓋率缺口。**僅提建議，不自動修改。**

```
/springboot-codegen
```

---

### ⑤ `test-report` — 測試報告彙整

**觸發：** `/test-report`  
**輸入：** `target/surefire-reports/`、`target/cucumber-reports/`、`target/site/jacoco/`  
**輸出：** `sdlc/test/output/TEST-REPORT-{CODE}-v{N}.md`

執行 Maven/Gradle 測試後，讀取原始報告，套用 `sdlc/test/templates/TEST-REPORT-template.md` 彙整成統一的中文測試報告：

| 來源 | 內容 |
|------|------|
| surefire | 單元/整合測試通過率、失敗案例、執行時間 |
| cucumber | BDD 情境結果（依 `@smoke` / `@regression` 標籤分群）|
| jacoco | 行/分支覆蓋率，標示未覆蓋的關鍵路徑 |
| ArchUnit | 架構規則檢查結果（分層依賴、命名、循環依賴）|

報告涵蓋整體摘要、各測試層明細、覆蓋率統計與待改善項目，可直接附於交付文件。

```
/test-report
```

---

### ⑥ `code-review` — 架構與資安審查（ArchUnit + LLM 分層）

**觸發：** `/code-review`  
**輸入：** `src/` 程式碼 + `.kiro/steering/java-coding-standards.md`  
**輸出：** Code Review 報告（問題分級、修正建議）

採**兩層分工**，把可機械化的規則交給確定性測試，LLM 只審查真正需要判斷的語意問題，**大幅降低 token 消耗**：

| 層次 | 檢查者 | 檢查內容 |
|------|--------|---------|
| **Phase 1 結構檢查** | **ArchUnit（確定性測試）** | 分層依賴方向、package 結構、類別/方法命名慣例、注解規範、循環依賴 |
| **Phase 2 語意審查** | **LLM** | 業務邏輯正確性、資安意圖（授權、注入、機密外洩）、缺漏的輸入驗證、錯誤處理 |
| **Phase 3 報告** | LLM | 彙整問題分級（Blocker/Major/Minor），發現錯誤時告警並可回饋 code gen 修正 |

> **為何用 ArchUnit 降 token？** 結構規則（如「Controller 不得直接依賴 Repository」）若交給 LLM 審查，  
> token 消耗隨檔案數線性成長；改寫成 ArchUnit 測試後，這類規則由 JVM 確定性驗證，  
> LLM 只需聚焦無法機械化的語意判斷。規則範本見 `.kiro/skills/code-review/references/archunit-rules.md`。

對應的架構測試已內建於本專案：`src/test/java/com/example/lifepremium/architecture/ArchitectureTest.java`。

```
/code-review
```

---

### ⑦ `markdown-to-word` — Word 套版輸出

**觸發：** `/markdown-to-word`  
**輸入：** `sdlc/fsd/output/*.md` 或 `sdlc/sd/output/*.md`  
**輸出：** `sdlc/fsd/output/*.docx` 或 `sdlc/sd/output/*.docx`

使用 **Pandoc + reference-doc** 機制，套用公司 Word 樣板，產出可直接發送審查的 `.docx` 文件，全程本地端執行。

| 文件類型 | 套版 | 輸出命名 |
|---------|------|---------|
| FSD | `sdlc/fsd/templates/FSD-template.docx` | `FSD-{CODE}-v{N}.docx` |
| SD | `sdlc/sd/templates/SD-template.docx` | `SD-{CODE}-v{N}.docx` |

```
/markdown-to-word #sdlc/fsd/output/FSD-LIFE-v1.0.md
```

---

### HITL（Human-in-the-Loop）確認點總覽

全流程共有 **5 個 HITL 確認點**，確保關鍵決策有人工把關：

```
SDLC 流程                    HITL 確認點              確認重點
─────────────────────────────────────────────────────────────────
/generate-fsd Phase 1   →   ⏸ FSD 主體確認      架構邊界、功能完整性
                        →   ⏸ Gherkin 確認      測試情境覆蓋率、業務規則
/generate-sd  Phase 1   →   ⏸ SD + ADR 確認     雙向：架構師輸入決策 + 審核核准 ADR
              Phase 2   →   ⏸ Task List 確認    任務清單正確性（HITL-2）
/springboot-codegen     →   ⏸ 測試案例確認      Red 狀態、邊界值、測試資料
```

**HITL-1 是雙向關卡：** 不只是「審核」，還包含「輸入」——AI 先提出候選架構方案（含 tradeoff），  
架構師輸入實際決策，AI 依此起草 ADR，再由架構師核准（`Proposed → Accepted`）。ADR 是產出的 artifact。

實作程式碼（Green）為**全自動**，無需人工確認，由測試套件自動驗收。  
`test-report`、`code-review`、`markdown-to-word` 為產出後的自動化步驟；code-review 若偵測到 Blocker 會告警並可回饋修正。

詳細說明請參閱各 Skill 的 `SKILL.md`：

| # | Skill | 說明文件 |
|---|-------|---------|
| ① | `doc-to-markdown` | `.kiro/skills/doc-to-markdown/SKILL.md` |
| ② | `generate-fsd` | `.kiro/skills/generate-fsd/SKILL.md` |
| ③ | `generate-sd` | `.kiro/skills/generate-sd/SKILL.md` |
| ④ | `springboot-codegen` | `.kiro/skills/springboot-codegen/SKILL.md` |
| ⑤ | `test-report` | `.kiro/skills/test-report/SKILL.md` |
| ⑥ | `code-review` | `.kiro/skills/code-review/SKILL.md`（+ `references/archunit-rules.md`）|
| ⑦ | `markdown-to-word` | `.kiro/skills/markdown-to-word/SKILL.md` |
| — | Steering | `.kiro/steering/java-coding-standards.md`（每次 session 自動載入）|

---

## 技術棧

| 類別 | 選型 |
|------|------|
| 語言 | Java 17 |
| 框架 | Spring Boot 3.3 |
| ORM | Spring Data JPA + Hibernate 6 |
| 資料庫 | PostgreSQL 15 |
| 快取 | Redis 7（Cache-Aside，TTL 1hr）|
| 測試 | JUnit 5 + Mockito + Cucumber 7 |
| 架構測試 | ArchUnit 1.3（分層依賴、命名、循環依賴確定性檢查）|
| 覆蓋率 | JaCoCo |
| 文件 | Springdoc OpenAPI 2 |
| 建置 | Maven 3.9（含 Maven Wrapper）|

---

## SDLC 詳細說明

請參閱 [`sdlc/README.md`](sdlc/README.md)。
