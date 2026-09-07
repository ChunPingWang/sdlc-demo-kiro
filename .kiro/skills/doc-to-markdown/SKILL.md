---
name: doc-to-markdown
description: 將輸入文件（PDF、Word .docx、Excel .xlsx、PowerPoint .pptx）轉換為 Markdown，完全在本地端執行，不傳送文件內容至雲端，降低 token 消耗。整合 Docling（複雜 PDF/掃描件）與 Microsoft MarkItDown（Office 文件）。Use when converting input documents to Markdown before feeding into FSD or SD generation workflow.
metadata:
  author: kiro-sdlc
  version: "1.0"
  stage: Pre-processing
  tools: Docling, MarkItDown, Pandoc
---

## 概述

本 Skill 為 SDLC 文件工作流程的**前置轉換步驟**，將各種格式的輸入文件轉為 Markdown，再交由 `generate-fsd` 或 `generate-sd` Skill 處理。

**核心原則：**
- 所有轉換在**本地端**執行，文件不上傳任何雲端服務
- 轉換後的 Markdown 存入 `sdlc/inputs/`，作為後續 Skill 的輸入來源
- 減少直接將原始 Office/PDF 二進位檔案餵入 LLM 所消耗的 token

**工具選型邏輯：**

| 輸入格式 | 優先工具 | 備用工具 | 理由 |
|---------|---------|---------|------|
| PDF（結構化，有文字層） | Docling | MarkItDown | Docling 表格/標題結構萃取最佳 |
| PDF（掃描件，無文字層）| Docling + OCR | — | Docling 內建 OCR pipeline |
| Word (.docx) | MarkItDown | Pandoc | 速度快，結構保留良好 |
| Excel (.xlsx) | MarkItDown | Docling | 表格轉 Markdown table |
| PowerPoint (.pptx) | MarkItDown | Docling | 投影片文字萃取 |
| 混合多格式批次 | `scripts/convert.py` | — | 自動判斷類型，批次處理 |

---

## 執行步驟

### Step 1：確認工具已安裝

執行以下指令確認環境就緒（詳見 `references/setup-guide.md`）：

```bash
python -m docling --version
markitdown --version
pandoc --version
```

若未安裝，依 `references/setup-guide.md` 完成安裝後再繼續。

### Step 2：判斷輸入類型並選擇工具

**情境 A：單一 PDF（有文字層）**

```bash
python scripts/convert.py --input sdlc/inputs/raw/{filename}.pdf --output sdlc/inputs/
```

內部使用 Docling，產出 `sdlc/inputs/{filename}.md`。

**情境 B：單一 PDF（掃描件）**

```bash
python scripts/convert.py --input sdlc/inputs/raw/{filename}.pdf --output sdlc/inputs/ --ocr
```

啟用 Docling OCR pipeline（EasyOCR）。

**情境 C：Word / Excel / PowerPoint**

```bash
python scripts/convert.py --input sdlc/inputs/raw/{filename}.docx --output sdlc/inputs/
```

內部使用 MarkItDown。

**情境 D：批次轉換整個資料夾**

```bash
python scripts/convert.py --input sdlc/inputs/raw/ --output sdlc/inputs/ --batch
```

自動偵測每個檔案類型，選用適合工具逐一轉換。

### Step 3：轉換後品質確認

轉換完成後，檢查以下項目：

- [ ] **表格**：確認 Markdown 表格欄位對齊，無合併儲存格遺失
- [ ] **標題層次**：`#` / `##` / `###` 結構是否反映原文件章節
- [ ] **圖片**：確認圖片說明文字已保留（圖片本身轉為 `![圖 N](path)` 或文字說明）
- [ ] **頁首頁尾**：確認頁碼、浮水印等雜訊已清除
- [ ] **特殊字元**：確認中文字元無亂碼

### Step 4：存放至 sdlc/inputs/

轉換後的 Markdown 依以下規則命名與存放：

```
sdlc/inputs/
├── raw/                              # 原始輸入文件（不修改）
│   ├── {filename}.pdf
│   ├── {filename}.docx
│   └── {filename}.xlsx
└── {filename}-converted.md          # 轉換後的 Markdown
```

### Step 5：通知後續 Skill

轉換完成後，告知使用者可用以下指令繼續：

```
已完成文件轉換，輸出至 sdlc/inputs/{filename}-converted.md

下一步：
  /generate-fsd #sdlc/inputs/{filename}-converted.md
  或
  /generate-sd  #sdlc/inputs/{filename}-converted.md #sdlc/fsd/output/FSD-*.md
```

---

## 輸出規格

| 項目 | 規格 |
|------|------|
| 輸出格式 | GitHub Flavored Markdown（GFM） |
| 檔案編碼 | UTF-8 |
| 圖片處理 | 萃取至 `sdlc/inputs/assets/{filename}/`，MD 中以相對路徑引用 |
| 表格格式 | Markdown pipe table |
| 行尾 | LF（Unix） |

---

## 參考資源

- `references/setup-guide.md` — Docling + MarkItDown 安裝與設定
- `scripts/convert.py` — 自動轉換腳本
