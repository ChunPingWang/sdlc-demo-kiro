# SDLC 企業文件工作流程

本目錄包含完整的企業級 SDLC 文件工作流程，涵蓋需求輸入、功能規格（FSD）、系統設計（SD）兩大階段，並整合 Kiro Skills 自動化產出。

---

## 目錄結構

```
sdlc/
├── inputs/                          # 原始需求輸入
│   └── *.md                         # 需求訪談紀錄、PRD、User Story
│
├── fsd/                             # Phase 1：功能規格文件
│   ├── templates/
│   │   ├── FSD-template.md          # FSD Markdown 模板
│   │   └── FSD-word-style-guide.md  # Word 套版樣式指南
│   └── output/
│       ├── FSD-{CODE}-v{N}.md       # 產出的 FSD 文件
│       └── features/
│           └── *.feature            # Gherkin BDD 測試案例
│
└── sd/                              # Phase 2：系統設計文件
    ├── templates/
    │   ├── SD-template.md           # SD Markdown 模板
    │   └── SD-word-style-guide.md   # Word 套版樣式指南
    └── output/
        ├── SD-{CODE}-v{N}.md        # 產出的 SD 文件
        └── assets/                  # 架構圖 PNG（由 Mermaid 渲染）與外部繪圖來源檔
```

---

## 整體流程

```
需求文件 / 原始碼
        │
        ▼  /generate-fsd
┌───────────────────────────────────┐
│  Phase 1A：FSD 主體               │
│  ├─ C4 L1 System Context 圖       │
│  ├─ C4 L2 Container 圖            │
│  ├─ 業務流程循序圖                │
│  ├─ 功能需求（FR 編號）           │
│  ├─ 非功能需求                    │
│  └─ UI / 資料 / 整合需求          │
└──────────────┬────────────────────┘
               │ ⏸ HITL：確認 FSD 主體與架構圖正確性
               ▼
┌───────────────────────────────────┐
│  Phase 1B：Gherkin 測試案例       │
│  ├─ .feature 檔（依 FR 產出）     │
│  ├─ Scenario tags（@smoke 等）    │
│  └─ Scenario Outline（邊界值）    │
└──────────────┬────────────────────┘
               │ ⏸ HITL：確認 Gherkin 情境正確性
               ▼
        FSD 階段完成 ✅
               │
               ▼  /generate-sd
┌───────────────────────────────────┐
│  Phase 2：SD 文件                 │
│  ├─ C4 L3 Component 圖            │
│  ├─ 技術層循序圖（同步 + 非同步） │
│  ├─ API 清單與詳細規格            │
│  ├─ 資料表設計（欄位 / 索引）     │
│  ├─ 安全設計（RBAC / 加密）       │
│  ├─ 部署架構（Docker / K8s）      │
│  └─ 效能 / 可觀測性設計           │
└──────────────┬────────────────────┘
               │ ⏸ HITL：確認 SD 技術設計
               ▼
        SD 階段完成 ✅
               │
               ▼  /springboot-codegen
┌───────────────────────────────────┐
│  Phase 3：TDD/BDD Code Gen        │
│  Red:    測試程式（先行）         │
│          ⏸ HITL 確認測試案例      │
│  Green:  實作程式碼（自動執行）   │
│  Refactor: 重構建議報告          │
└───────────────────────────────────┘
```

---

## Kiro Skills

| Skill | 指令 | 用途 |
|-------|------|------|
| `generate-fsd` | `/generate-fsd` | 從需求文件或原始碼產出 FSD + Gherkin |
| `generate-sd` | `/generate-sd` | 從 FSD 產出 SD（含 C4 L3 / API / 資料表）|
| `springboot-codegen` | `/springboot-codegen` | 從 SD+FSD 以 TDD/BDD 產出 Spring Boot 程式碼 |

---

## Phase 1：FSD 使用說明

### 輸入

```
/generate-fsd #sdlc/inputs/LIFE-PREMIUM-requirements.md
```

或直接描述需求：

```
/generate-fsd 我需要一個壽險保費試算系統，支援業務員與訪客試算...
```

### 輸出

| 檔案 | 說明 |
|------|------|
| `sdlc/fsd/output/FSD-{CODE}-v{N}.md` | FSD 主體（含 C4 L1/L2 + 循序圖）|
| `sdlc/fsd/output/features/*.feature` | Gherkin 測試案例 |

### Word 轉換

```bash
pandoc sdlc/fsd/output/FSD-LIFE-v1.0.md \
  --reference-doc=sdlc/fsd/templates/FSD-template.docx \
  --toc --toc-depth=3 \
  --output=sdlc/fsd/output/FSD-LIFE-v1.0.docx
```

---

## Phase 2：SD 使用說明

### 輸入

```
/generate-sd #sdlc/fsd/output/FSD-LIFE-v1.0.md
```

### 輸出

| 檔案 | 說明 |
|------|------|
| `sdlc/sd/output/SD-{CODE}-v{N}.md` | SD 主體（含 C4 L3 + 技術循序圖 + API + 資料表）|

### Word 轉換

```bash
pandoc sdlc/sd/output/SD-LIFE-v1.0.md \
  --reference-doc=sdlc/sd/templates/SD-template.docx \
  --toc --toc-depth=3 \
  --output=sdlc/sd/output/SD-LIFE-v1.0.docx
```

---

## Phase 3：Code Gen 使用說明

### 輸入

```
/springboot-codegen \
  #sdlc/sd/output/SD-LIFE-v1.0.md \
  #sdlc/fsd/output/FSD-LIFE-v1.0.md \
  #sdlc/fsd/output/features/
```

### HITL 確認點

| 關卡 | 時機 | 確認內容 |
|------|------|---------|
| Phase 1 HITL | 測試程式產出後 | 業務規則是否正確反映、邊界值是否完整 |
| Phase 2 自動 | 實作程式碼產出後 | 自動執行 `./mvnw test` 確認 Green |

---

## 文件版本命名規則

```
FSD-{PROJECT_CODE}-v{MAJOR}.{MINOR}.md
SD-{PROJECT_CODE}-v{MAJOR}.{MINOR}.md

範例：
  FSD-LIFE-v1.0.md    （壽險保費試算 初版）
  SD-LIFE-v1.2.md     （第二次設計修訂）
```

## FSD ↔ SD 版本對應

| FSD 版本 | SD 版本 | 說明 |
|---------|---------|------|
| FSD-LIFE-v1.0 | SD-LIFE-v1.0 | 初版對應 |
| FSD-LIFE-v1.x | SD-LIFE-v1.x | 功能修訂同步 |
| FSD-LIFE-v2.0 | SD-LIFE-v2.0 | 重大改版同步升版 |

---

*本工作流程由 Kiro SDLC Skills 管理。如需新增或修改 Skill，請編輯 `.kiro/skills/` 目錄下的對應 `SKILL.md`。*
