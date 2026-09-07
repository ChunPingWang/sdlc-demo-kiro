---
name: generate-fsd
description: 根據需求文件或原始碼產生功能規格文件（FSD）。當使用者提供需求訪談紀錄、User Story、PRD 或現有原始碼，並要求輸出 FSD Markdown 文件或 Word 套版時使用。Use when generating a Functional Specification Document from requirements or source code.
metadata:
  author: kiro-sdlc
  version: "1.0"
  stage: FSD
---

## 概述

本 Skill 指導 Kiro 將輸入的需求文件或原始碼，轉化為符合企業標準的功能規格文件（FSD）。  
輸出格式支援 **Markdown**（主要）與 **Word 套版**（依 `references/FSD-word-style-guide.md` 規範）。

---

## 輸入來源（Input）

使用者應提供以下一種或多種輸入：

| 輸入類型 | 說明 | 範例指令 |
|---------|------|---------|
| 需求文件 | PRD、User Story、訪談紀錄、需求規格 | `/generate-fsd #需求文件.md` |
| 原始碼 | 現有程式碼（逆向推導功能規格） | `/generate-fsd #src/` |
| 自由描述 | 以文字直接描述功能需求 | `/generate-fsd 我需要一個電商訂單管理系統` |

---

## 執行步驟

### Step 1：分析輸入

1. 讀取使用者提供的所有輸入文件或描述
2. 識別以下關鍵資訊：
   - **專案名稱** 與 **系統邊界**
   - **使用者角色**（Actor）
   - **核心功能模組**（依業務領域分群）
   - **業務規則**與**驗證條件**
   - **整合的外部系統**
3. 若輸入為原始碼，分析以下面向：
   - Route / Controller 定義 → 對應功能項目
   - Service 層邏輯 → 對應業務規則
   - Schema / Model → 對應資料需求
   - 現有 Test Case → 對應驗收標準

### Step 2：規劃文件結構

依分析結果決定模組劃分，每個模組對應 FSD 第 5 章的一個小節。  
功能編號規則：`FR-{MODULE_CODE}-{3位序號}`，例如 `FR-ORDER-001`。

### Step 3：產生 FSD Markdown

嚴格依照 `references/FSD-template.md` 的章節結構填寫，規則如下：

- **所有 `{PLACEHOLDER}` 必須替換為實際內容**，不得保留未填的佔位符
- 若資訊不足，在該欄位標注 `⚠️ 待確認：{說明需要釐清的問題}`
- 每個功能項目（FR）必須包含：優先等級、功能描述、主要流程、驗收標準
- 優先等級判斷原則：
  - 高：核心業務流程、無此功能系統無法運作
  - 中：重要輔助功能、影響使用者體驗
  - 低：Nice-to-have、可延後實作

### Step 4：輸出檔案

1. 將產生的 FSD 存至：
   ```
   sdlc/fsd/output/FSD-{PROJECT_CODE}-v{VERSION}.md
   ```
2. 在回應中告知使用者：
   - 已識別的模組清單
   - 功能項目總數
   - 標注為「待確認」的問題列表
   - Word 轉換指令（見下方）

### Step 5：Word 套版轉換（選用）

若使用者要求輸出 Word 檔，提供以下 Pandoc 指令：

```bash
pandoc sdlc/fsd/output/FSD-{PROJECT_CODE}-v{VERSION}.md \
  --reference-doc=sdlc/fsd/templates/FSD-template.docx \
  --toc --toc-depth=3 \
  --output=sdlc/fsd/output/FSD-{PROJECT_CODE}-v{VERSION}.docx
```

Word 樣式規範請參考 `references/FSD-word-style-guide.md`。

---

## 品質檢查清單

產出 FSD 前，確認以下項目：

- [ ] 文件標頭（編號、專案名稱、版本、日期）已填寫
- [ ] 所有功能項目均有唯一的 FR 編號
- [ ] 每個 FR 均有明確的驗收標準
- [ ] 非功能需求（效能、安全、可用性）章節已填寫
- [ ] 無殘留的 `{PLACEHOLDER}` 佔位符（「待確認」除外）
- [ ] 審查與核准表格已列出相關人員欄位

---

## 參考資源

- 模板：`references/FSD-template.md`
- Word 套版規範：`references/FSD-word-style-guide.md`
- 輸出目錄：`sdlc/fsd/output/`
