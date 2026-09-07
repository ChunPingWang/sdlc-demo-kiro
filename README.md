# sdlc-demo-kiro

以**壽險新保件保費試算（Life Premium）** 為 MVP，端對端驗證 Kiro SDLC 自動化工作流程：從需求文件到可執行的 Spring Boot 程式碼，全程由 Kiro Skills 驅動，關鍵節點由人工確認（HITL）。

---

## 快速導覽

```
sdlc-demo-kiro/
├── sdlc/                        # SDLC 文件（需求 → FSD → SD）
│   ├── inputs/                  # 原始需求
│   ├── fsd/                     # 功能規格文件 + Gherkin
│   └── sd/                      # 系統設計文件
│
├── src/                         # Spring Boot 實作（由 /springboot-codegen 產出）
│   ├── main/java/com/example/lifepremium/
│   └── test/java/com/example/lifepremium/
│
├── .kiro/skills/                # Kiro Skills
│   ├── generate-fsd/            # FSD + Gherkin 產出 Skill
│   ├── generate-sd/             # SD 文件產出 Skill
│   └── springboot-codegen/      # TDD/BDD Code Gen Skill
│
└── pom.xml                      # Maven 建置設定
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
需求文件（inputs/）
        │
        ▼  Kiro: /generate-fsd
   FSD 文件 + Gherkin .feature
        │  ⏸ HITL：確認 FSD 主體
        │  ⏸ HITL：確認 Gherkin 情境
        ▼  Kiro: /generate-sd
      SD 文件（C4 L3 + API + 資料表）
        │  ⏸ HITL：確認 SD 設計
        ▼  Kiro: /springboot-codegen
   測試程式（Red）
        │  ⏸ HITL：確認測試案例
        ▼  自動執行
   實作程式碼（Green）→ 重構建議
```

### 產出文件清單

| 階段 | 文件 | 路徑 |
|------|------|------|
| 需求 | 業務需求描述 | `sdlc/inputs/LIFE-PREMIUM-requirements.md` |
| FSD | 功能規格文件 | `sdlc/fsd/output/FSD-LIFE-v1.0.md` |
| FSD | Gherkin 測試案例 | `sdlc/fsd/output/features/premium-calculation.feature` |
| SD | 系統設計文件 | `sdlc/sd/output/SD-LIFE-v1.0.md` |

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

# 全部測試
./mvnw test
```

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
   │  skill: generate-fsd            │  ← 只讀 description，幾乎無 token 消耗
   │  skill: generate-sd             │
   │  skill: springboot-codegen      │
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

本專案的完整 SDLC 流程由 **5 個 Skills** 協作完成，各自負責不同階段：

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SDLC 端對端工作流程                                │
│                                                                     │
│  原始文件（PDF/Word/Excel）                                           │
│       │                                                             │
│       ▼  ① /doc-to-markdown                                        │
│  sdlc/inputs/*.md  ←─ 本地轉換，不上雲端                             │
│       │                                                             │
│       ▼  ② /generate-fsd                                           │
│  FSD 文件 + C4 L1/L2 + 循序圖                                        │
│       │  ⏸ HITL Phase 1：確認 FSD 主體                              │
│  Gherkin .feature 檔                                                │
│       │  ⏸ HITL Phase 2：確認測試情境                               │
│       │                                                             │
│       ▼  ③ /generate-sd                                            │
│  SD 文件 + C4 L3 + API 規格 + 資料表設計                              │
│       │  ⏸ HITL：確認設計內容                                        │
│       │                                                             │
│       ▼  ④ /springboot-codegen                                     │
│  測試程式（🔴 Red）                                                   │
│       │  ⏸ HITL：確認測試案例正確性                                   │
│  實作程式碼（🟢 Green）← 自動執行，無需確認                             │
│  REFACTOR-NOTES.md   ← Phase 3 重構建議                             │
│       │                                                             │
│       ▼  ⑤ /markdown-to-word                                       │
│  FSD.docx + SD.docx  ← 套用公司 Word 樣板                           │
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
**輸入：** FSD 文件 + 技術架構決策 (ADR)  
**輸出：** `sdlc/sd/output/SD-{CODE}-v{N}.md`

以 FSD 為輸入，產出完整系統設計文件，著重技術實作細節：

| 章節 | 內容 | 對應 FSD |
|------|------|---------|
| C4 L3 Component | 各 Container 內部元件、依賴關係 | 延伸 C4 L2 |
| 技術循序圖 | 服務間呼叫鏈、非同步事件流 | 業務循序圖的技術實作面 |
| API 規格 | 完整 Request/Response schema、錯誤碼 | 功能需求 FR |
| 資料表設計 | 欄位定義、索引、關聯、快取策略 | 資料需求 |
| 安全設計 | RBAC 角色矩陣、資料加密、防護措施 | 非功能需求 |

```
/generate-sd #sdlc/fsd/output/FSD-LIFE-v1.0.md
```

---

### ④ `springboot-codegen` — TDD/BDD 程式碼產出

**觸發：** `/springboot-codegen`  
**輸入：** SD 文件（C4 L3 + API + 資料表）+ Gherkin `.feature` 檔  
**輸出：** `src/` 下完整 Spring Boot 專案程式碼

遵循 **Red → Green → Refactor** 方法論，分三個 Phase 執行：

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

### ⑤ `markdown-to-word` — Word 套版輸出

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

全流程共有 **4 個 HITL 確認點**，確保關鍵決策有人工把關：

```
SDLC 流程                    HITL 確認點              確認重點
─────────────────────────────────────────────────────────────────
/generate-fsd Phase 1   →   ⏸ FSD 主體確認      架構邊界、功能完整性
                        →   ⏸ Gherkin 確認      測試情境覆蓋率、業務規則
/generate-sd            →   ⏸ SD 設計確認       API 規格、資料表、安全設計
/springboot-codegen     →   ⏸ 測試案例確認      Red 狀態、邊界值、測試資料
```

Phase 2 實作程式碼（Green）為**全自動**，無需人工確認，由測試套件自動驗收。

詳細說明請參閱各 Skill 的 `SKILL.md`：

| Skill | 說明文件 |
|-------|---------|
| `doc-to-markdown` | `.kiro/skills/doc-to-markdown/SKILL.md` |
| `generate-fsd` | `.kiro/skills/generate-fsd/SKILL.md` |
| `generate-sd` | `.kiro/skills/generate-sd/SKILL.md` |
| `springboot-codegen` | `.kiro/skills/springboot-codegen/SKILL.md` |
| `markdown-to-word` | `.kiro/skills/markdown-to-word/SKILL.md` |

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
| 文件 | Springdoc OpenAPI 2 |
| 建置 | Maven 3.9 |

---

## SDLC 詳細說明

請參閱 [`sdlc/README.md`](sdlc/README.md)。
