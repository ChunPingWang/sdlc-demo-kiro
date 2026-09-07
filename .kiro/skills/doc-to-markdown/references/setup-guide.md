# Docling + MarkItDown 安裝與使用指南

---

## §1 環境需求

| 項目 | 最低需求 |
|------|---------|
| Python | 3.10 以上 |
| pip | 23.0 以上 |
| 磁碟空間 | Docling 模型約 1–2 GB |
| RAM | 建議 8 GB 以上（Docling OCR） |
| OS | Windows 10+、macOS 12+、Ubuntu 20.04+ |

建議使用虛擬環境：

```bash
python -m venv .venv
# Windows
.venv\Scripts\activate
# macOS / Linux
source .venv/bin/activate
```

---

## §2 安裝 Docling

[Docling](https://github.com/docling-project/docling) 由 IBM 開源，支援 PDF（含掃描件）、Word、Excel、PowerPoint、HTML 轉 Markdown。

```bash
pip install docling
```

### 驗證安裝

```bash
python -m docling --version
# 或
docling --version
```

### 首次使用下載模型

Docling 首次執行時會自動下載 AI 模型（約 1 GB），需要網路連線。之後離線可用。

```bash
# 測試轉換（確認模型下載正常）
docling --output-format md --output ./test-output sample.pdf
```

### OCR 支援（掃描 PDF）

```bash
# 安裝 OCR 支援
pip install "docling[ocr]"

# 使用 OCR 轉換
docling --ocr --output-format md --output ./output scan.pdf
```

---

## §3 安裝 MarkItDown

[MarkItDown](https://github.com/microsoft/markitdown) 由 Microsoft 開源，專為 Office 文件快速轉 Markdown 設計。

```bash
pip install markitdown
```

### 驗證安裝

```bash
markitdown --version
```

### 選用依賴（強化 Excel/PPT 支援）

```bash
pip install "markitdown[all]"
```

---

## §4 一鍵安裝腳本

在專案根目錄建立 `requirements-convert.txt`：

```
docling>=2.0.0
docling[ocr]>=2.0.0
markitdown[all]>=0.1.0
```

然後執行：

```bash
pip install -r requirements-convert.txt
```

---

## §5 工具使用範例

### Docling CLI 常用指令

```bash
# PDF → Markdown（標準）
docling --output-format md --output sdlc/inputs/ document.pdf

# PDF → Markdown（掃描件，啟用 OCR）
docling --ocr --output-format md --output sdlc/inputs/ scanned.pdf

# PDF → JSON（含完整結構資訊）
docling --output-format json --output sdlc/inputs/ document.pdf

# 批次轉換資料夾中所有 PDF
docling --output-format md --output sdlc/inputs/ sdlc/inputs/raw/*.pdf

# 萃取圖片至子資料夾
docling --output-format md --image-export-mode referenced \
        --output sdlc/inputs/ document.pdf
```

### Docling Python API

```python
from docling.document_converter import DocumentConverter

converter = DocumentConverter()
result = converter.convert("sdlc/inputs/raw/requirements.pdf")

# 輸出 Markdown
markdown = result.document.export_to_markdown()
with open("sdlc/inputs/requirements-converted.md", "w", encoding="utf-8") as f:
    f.write(markdown)
```

### MarkItDown CLI 常用指令

```bash
# Word → Markdown
markitdown document.docx > sdlc/inputs/document-converted.md

# Excel → Markdown
markitdown spreadsheet.xlsx > sdlc/inputs/spreadsheet-converted.md

# PowerPoint → Markdown
markitdown presentation.pptx > sdlc/inputs/presentation-converted.md
```

### MarkItDown Python API

```python
from markitdown import MarkItDown

md = MarkItDown()

# Word
result = md.convert("sdlc/inputs/raw/requirements.docx")
with open("sdlc/inputs/requirements-converted.md", "w", encoding="utf-8") as f:
    f.write(result.text_content)

# Excel
result = md.convert("sdlc/inputs/raw/data.xlsx")
with open("sdlc/inputs/data-converted.md", "w", encoding="utf-8") as f:
    f.write(result.text_content)
```

---

## §6 工具選型決策樹

```
輸入檔案
    │
    ├─ .pdf
    │   ├─ 有文字層（可選取文字）？
    │   │   └─ YES → Docling（標準模式）
    │   └─ 無文字層（掃描件）？
    │       └─ YES → Docling（OCR 模式）
    │
    ├─ .docx（Word）
    │   └─ MarkItDown（快速）
    │      備用：pandoc --to gfm（若需精確樣式對應）
    │
    ├─ .xlsx（Excel）
    │   └─ MarkItDown
    │      備用：Docling（複雜表格）
    │
    └─ .pptx（PowerPoint）
        └─ MarkItDown
```

---

## §7 已知限制與處理方式

| 限制 | 工具 | 處理方式 |
|------|------|---------|
| 複雜合併儲存格表格可能失真 | 兩者皆有 | 轉換後手動修正 Markdown 表格 |
| 掃描 PDF 中文 OCR 準確率 | Docling | 安裝中文語言包：`pip install easyocr` |
| Excel 多工作表 | MarkItDown | 預設只轉第一張；多張需指定 sheet |
| 嵌入圖片（非向量）| 兩者皆有 | 圖片另存為 PNG，MD 中以路徑引用 |
| 密碼保護的 Office 文件 | 兩者皆有 | 需先解除密碼保護 |
