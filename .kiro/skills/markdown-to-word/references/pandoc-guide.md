# Pandoc 安裝與 Word 套版使用指南

---

## §1 安裝 Pandoc

### Windows

```powershell
# 方式一：winget（推薦）
winget install JohnMacFarlane.Pandoc

# 方式二：Chocolatey
choco install pandoc

# 方式三：手動下載
# 至 https://github.com/jgm/pandoc/releases 下載 pandoc-*-windows-x86_64.msi
```

### macOS

```bash
brew install pandoc
```

### Linux (Ubuntu/Debian)

```bash
sudo apt-get install pandoc
```

### 驗證安裝

```bash
pandoc --version
# 建議版本：3.0 以上
```

---

## §2 mermaid-cli 安裝（架構圖渲染）

本工具鏈的圖形一律使用 **Mermaid**（不使用 PlantUML）。Pandoc 不直接渲染 Mermaid，  
需先以 **mermaid-cli（`mmdc`）** 將 ` ```mermaid ` 區塊轉為 PNG，再嵌入 Word。`md_to_word.py` 會自動完成此步。

### 前置需求：Node.js

```bash
node -v   # 需要 Node.js 18 以上
```

### 安裝 mermaid-cli

```bash
# 全域安裝（提供 mmdc 指令）
npm install -g @mermaid-js/mermaid-cli

# 或安裝於專案本地（scripts 會自動偵測 node_modules/.bin/mmdc）
npm install @mermaid-js/mermaid-cli

# 驗證
mmdc --version
```

> 若不安裝，`md_to_word.py` 會退而使用 `npx -y @mermaid-js/mermaid-cli`（首次執行需網路下載）。  
> 完全離線環境請先全域或本地安裝。

### 手動批次轉換 mermaid → PNG（選用）

```bash
# 單一檔案：mmdc 讀取 .mmd 產生 PNG（白底、2x 解析度）
mmdc -i diagram.mmd -o assets/diagram-01.png -b white -s 2
```

> 一般情況不需手動執行；`md_to_word.py` 會自動擷取 Markdown 中的 mermaid 區塊並渲染。

---

## §3 建立 Word 套版（reference-doc）

Pandoc 的 `--reference-doc` 機制：Pandoc 不複製模板內容，而是**借用模板中的樣式定義**套用至產出文件。

### 建立初始套版

```bash
# 讓 Pandoc 產生一個含有所有預設樣式的 reference.docx
pandoc -o sdlc/fsd/templates/FSD-template.docx \
       --print-default-data-file reference.docx > sdlc/fsd/templates/FSD-template.docx
```

### 在 Word 中修改套版樣式

1. 以 Word 開啟 `FSD-template.docx`
2. 開啟「**樣式窗格**」（常用 → 樣式 → 管理樣式）
3. 依 `sdlc/fsd/templates/FSD-word-style-guide.md` 修改以下樣式：
   - `Heading 1` / `Heading 2` / `Heading 3`
   - `Normal`（本文）
   - `Table` / `Table Header`（表格）
   - `Verbatim Char`（程式碼行內）
   - `Source Code`（程式碼區塊）
4. 設定頁首頁尾（見樣式指南第 3 節）
5. 儲存並關閉

> **注意：** 套版檔案中的「內容」不重要，只有「樣式定義」會被 Pandoc 使用。

### Pandoc 樣式對應表

| Markdown 元素 | Pandoc 套用的 Word 樣式 |
|-------------|----------------------|
| `# 標題` | Heading 1 |
| `## 標題` | Heading 2 |
| `### 標題` | Heading 3 |
| 一般段落 | Normal / Body Text |
| `` `行內程式碼` `` | Verbatim Char |
| ` ```程式碼區塊``` ` | Source Code |
| `**粗體**` | Strong / Bold |
| `*斜體*` | Emphasis / Italic |
| `\| 表格 \|` | Table / Table Paragraph |
| `- 清單` | List Paragraph |

---

## §4 Pandoc 轉換完整指令

### FSD 文件

```bash
pandoc "sdlc/fsd/output/FSD-{CODE}-v{VER}.md" \
  --from "markdown+smart+pipe_tables+fenced_code_blocks" \
  --to docx \
  --reference-doc="sdlc/fsd/templates/FSD-template.docx" \
  --toc \
  --toc-depth=3 \
  --wrap=none \
  --metadata title="功能規格文件 - {PROJECT_NAME}" \
  --output "sdlc/fsd/output/FSD-{CODE}-v{VER}.docx"
```

### SD 文件

```bash
pandoc "sdlc/sd/output/SD-{CODE}-v{VER}.md" \
  --from "markdown+smart+pipe_tables+fenced_code_blocks" \
  --to docx \
  --reference-doc="sdlc/sd/templates/SD-template.docx" \
  --toc \
  --toc-depth=3 \
  --wrap=none \
  --metadata title="系統設計文件 - {PROJECT_NAME}" \
  --output "sdlc/sd/output/SD-{CODE}-v{VER}.docx"
```

### 常用 Pandoc 參數說明

| 參數 | 說明 |
|------|------|
| `--from markdown+smart+pipe_tables` | 啟用智慧引號、pipe table 語法 |
| `--reference-doc` | 指定 Word 套版模板 |
| `--toc` | 自動產生目錄 |
| `--toc-depth=3` | 目錄顯示至第 3 層標題 |
| `--wrap=none` | 不自動換行（保持 Markdown 原始行結構）|
| `--metadata title=""` | 設定文件標題（顯示於 Word 屬性）|
| `--extract-media=assets/` | 將嵌入圖片解出至指定資料夾 |

---

## §5 常見問題排除

| 問題 | 原因 | 解決方式 |
|------|------|---------|
| 中文亂碼 | 編碼問題 | 確認 Markdown 為 UTF-8；加 `--metadata lang=zh-TW` |
| 表格樣式未套用 | 套版缺少 Table 樣式 | 在套版 Word 中手動新增 `Table` 樣式 |
| 程式碼區塊沒有底色 | 套版缺少 Source Code 樣式 | 新增 `Source Code` 段落樣式並設定背景色 |
| 目錄不更新 | Word 快取 | 開啟 docx 後全選（Ctrl+A）→ F9 更新欄位 |
| 圖片破圖 | 相對路徑錯誤 | 確認 Pandoc 執行目錄與 Markdown 圖片路徑一致 |
| Mermaid 顯示為程式碼 | 未預先轉 PNG | 確認已安裝 `mmdc`（`npm i -g @mermaid-js/mermaid-cli`），由 `md_to_word.py` 自動渲染 |
