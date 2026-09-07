---
name: markdown-to-word
description: 將 FSD 或 SD 的 Markdown 輸出套用公司 Word 樣式模板，產生 .docx 格式文件。使用 Pandoc 本地端執行，完全離線，不傳送內容至雲端。Use when exporting FSD or SD Markdown documents to Word format with company branding.
metadata:
  author: kiro-sdlc
  version: "1.0"
  stage: Post-processing
  tools: Pandoc
---

## 概述

本 Skill 為 SDLC 文件工作流程的**最後輸出步驟**，將 `generate-fsd` 或 `generate-sd` 產出的 Markdown，透過 Pandoc 套用公司 Word 樣式模板，產出可直接發送審查的 `.docx` 文件。

**核心原則：**
- 使用 **Pandoc + reference-doc** 機制，樣式定義在 `*.docx` 模板中，不依賴雲端服務
- 支援 FSD 與 SD 兩種套版（樣式略有差異，見 FSD/SD Word 樣式指南）
- 產出檔案存入對應的 `output/` 資料夾

---

## 前置需求

確認 Pandoc 已安裝（詳見 `references/pandoc-guide.md`）：

```bash
pandoc --version
```

確認套版模板存在：

```
sdlc/fsd/templates/FSD-template.docx    ← FSD 套版
sdlc/sd/templates/SD-template.docx     ← SD 套版
```

> 若模板不存在，依 `references/pandoc-guide.md §3` 建立初始套版。

---

## 執行步驟

### Step 1：確認輸入 Markdown

| 文件類型 | 輸入來源 | 套版 |
|---------|---------|------|
| FSD | `sdlc/fsd/output/FSD-{CODE}-{VER}.md` | `sdlc/fsd/templates/FSD-template.docx` |
| SD | `sdlc/sd/output/SD-{CODE}-{VER}.md` | `sdlc/sd/templates/SD-template.docx` |

### Step 2：執行轉換腳本

**FSD 文件轉換：**

```bash
python scripts/md_to_word.py \
  --input  sdlc/fsd/output/FSD-{CODE}-{VER}.md \
  --template sdlc/fsd/templates/FSD-template.docx \
  --output sdlc/fsd/output/FSD-{CODE}-{VER}.docx
```

**SD 文件轉換：**

```bash
python scripts/md_to_word.py \
  --input  sdlc/sd/output/SD-{CODE}-{VER}.md \
  --template sdlc/sd/templates/SD-template.docx \
  --output sdlc/sd/output/SD-{CODE}-{VER}.docx
```

**直接使用 Pandoc 指令（不透過腳本）：**

```bash
pandoc "{input}.md" \
  --from markdown+smart+pipe_tables \
  --to docx \
  --reference-doc="{template}.docx" \
  --toc \
  --toc-depth=3 \
  --wrap=none \
  --output="{output}.docx"
```

### Step 3：轉換後品質確認

開啟產出的 `.docx`，確認以下項目：

- [ ] **樣式套用**：標題 1/2/3、本文、表格標頭、程式碼區塊樣式正確
- [ ] **目錄**：更新目錄（右鍵 → 更新欄位 → 更新整個目錄）
- [ ] **頁首頁尾**：文件編號、專案名稱、機密等級正確填寫
- [ ] **表格**：無表格斷頁、欄寬適中
- [ ] **PlantUML 圖表**：確認圖片以附件方式插入（PNG 格式，見下方說明）
- [ ] **Gherkin 程式碼區塊**：套用 `程式碼區塊` 樣式

### Step 4：PlantUML 圖表處理

Pandoc 無法直接渲染 PlantUML，需先將 `.puml` 轉為 PNG 再嵌入：

```bash
# 安裝 PlantUML（需要 Java）
# 批次轉換所有 .puml 至 PNG
java -jar plantuml.jar -png sdlc/*/output/assets/*.puml
```

轉換後 PNG 存於 `sdlc/*/output/assets/`，Markdown 中的 plantuml 區塊須先替換為：

```markdown
![{圖表說明}](assets/{filename}.png)

> 圖 N-M：{說明文字}
```

腳本 `scripts/md_to_word.py` 會自動處理此替換。

### Step 5：輸出命名與版本

```
FSD-{PROJECT_CODE}-v{MAJOR}.{MINOR}.docx
SD-{PROJECT_CODE}-v{MAJOR}.{MINOR}.docx

範例：
  sdlc/fsd/output/FSD-ECOM-v1.0.docx
  sdlc/sd/output/SD-ECOM-v1.0.docx
```

---

## 參考資源

- `references/pandoc-guide.md` — Pandoc 安裝、套版建立、進階設定
- `scripts/md_to_word.py` — 轉換腳本（含 PlantUML 自動替換）
- `sdlc/fsd/templates/FSD-word-style-guide.md` — FSD Word 樣式定義
- `sdlc/sd/templates/SD-word-style-guide.md` — SD Word 樣式定義
