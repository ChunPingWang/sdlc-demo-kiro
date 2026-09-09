#!/usr/bin/env python3
"""
SDLC Markdown → Word 套版轉換腳本
===================================
使用 Pandoc 將 FSD/SD Markdown 文件套用公司 Word 模板，產出 .docx。

功能：
  1. 自動將 Markdown 中的 mermaid 程式碼區塊渲染為 PNG 並替換為圖片引用
     （使用 mermaid-cli / mmdc，本地端執行，不上雲端）
  2. 呼叫 Pandoc 執行 Markdown → DOCX 轉換（套用 reference-doc 樣式）
  3. 驗證輸出檔案存在

圖形標準：本工具鏈一律使用 Mermaid（不使用 PlantUML）。FSD/SD 產出的
C4 圖與循序圖皆為 ` ```mermaid ` 區塊，可直接在 GitHub / GitLab 預覽。

使用方式：
  python md_to_word.py --input sdlc/fsd/output/FSD-PROJ-v1.0.md \\
                       --template sdlc/fsd/templates/FSD-template.docx \\
                       --output sdlc/fsd/output/FSD-PROJ-v1.0.docx

  python md_to_word.py --input sdlc/sd/output/SD-PROJ-v1.0.md \\
                       --template sdlc/sd/templates/SD-template.docx \\
                       --output sdlc/sd/output/SD-PROJ-v1.0.docx
"""

import argparse
import logging
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)


# ──────────────────────────────────────────────
# 工具可用性檢查
# ──────────────────────────────────────────────
def check_pandoc() -> str | None:
    """回傳 pandoc 路徑，若未安裝回傳 None"""
    path = shutil.which("pandoc")
    if path:
        result = subprocess.run(["pandoc", "--version"], capture_output=True, text=True)
        version = result.stdout.splitlines()[0] if result.returncode == 0 else "unknown"
        log.info(f"Pandoc: {version}")
    return path


def check_mermaid() -> str | None:
    """尋找 mermaid-cli (mmdc) 可執行檔，回傳路徑或 None"""
    # 1) PATH 上的 mmdc（npm install -g @mermaid-js/mermaid-cli）
    path = shutil.which("mmdc")
    if path:
        log.info(f"mermaid-cli (mmdc): {path}")
        return path
    # 2) 專案本地 node_modules（npm install @mermaid-js/mermaid-cli）
    local = Path("node_modules/.bin/mmdc")
    if local.exists():
        log.info(f"mermaid-cli (mmdc): {local}")
        return str(local)
    # 3) 免安裝的 npx 後備方案
    if shutil.which("npx"):
        log.info("mermaid-cli: 將以 npx 執行（首次會下載，需要網路）")
        return "npx:@mermaid-js/mermaid-cli"
    return None


# ──────────────────────────────────────────────
# Mermaid 處理
# ──────────────────────────────────────────────
MERMAID_BLOCK_RE = re.compile(
    r"```mermaid\n(.*?)```",
    re.DOTALL
)

# Mermaid 常見圖種第一個關鍵字 → 中文圖名（用於圖說文字）
DIAGRAM_KIND_RE = re.compile(
    r"^\s*(C4Context|C4Container|C4Component|sequenceDiagram|erDiagram|"
    r"classDiagram|stateDiagram(?:-v2)?|flowchart|graph|gantt|journey)",
    re.MULTILINE,
)
# Mermaid 標題：C4 的 `title ...` 或 sequence 的 `title ...`
TITLE_RE = re.compile(r"^\s*title\s+(.+)$", re.MULTILINE)


def extract_diagram_title(mmd_content: str, idx: int) -> str:
    """從 Mermaid 內容擷取標題；若無 title 則以圖種 + 序號命名"""
    m = TITLE_RE.search(mmd_content)
    if m:
        return m.group(1).strip()
    k = DIAGRAM_KIND_RE.search(mmd_content)
    kind = k.group(1) if k else "Diagram"
    return f"{kind}-{idx:02d}"


def render_mermaid(mmd_content: str, output_path: Path, mmdc: str) -> bool:
    """將 Mermaid 內容渲染為 PNG（mermaid-cli / mmdc，本地端執行）"""
    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", suffix=".mmd", encoding="utf-8", delete=False
        ) as f:
            f.write(mmd_content)
            tmp_path = f.name

        if mmdc.startswith("npx:"):
            pkg = mmdc.split(":", 1)[1]
            base = ["npx", "-y", pkg]
        else:
            base = [mmdc]

        cmd = base + [
            "-i", tmp_path,
            "-o", str(output_path),
            "-b", "white",          # 白底，避免 Word 透明背景問題
            "-s", "2",              # 2x 縮放，提升 Word 內解析度
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            log.warning(f"Mermaid 渲染失敗：{result.stderr.strip()}")
            return False
        return output_path.exists()
    finally:
        if tmp_path:
            Path(tmp_path).unlink(missing_ok=True)


def process_mermaid_blocks(
    markdown: str,
    assets_dir: Path,
    mmdc: str | None
) -> str:
    """
    將 Markdown 中的 mermaid 區塊渲染為 PNG 並替換為圖片引用。
    若 mermaid-cli 不可用，保留原始程式碼區塊並加上警告注釋。
    """
    diagram_count = [0]

    def replace_block(match: re.Match) -> str:
        mmd_content = match.group(1)
        diagram_count[0] += 1
        idx = diagram_count[0]
        title = extract_diagram_title(mmd_content, idx)

        if mmdc:
            png_name = f"diagram-{idx:02d}.png"
            png_path = assets_dir / png_name
            assets_dir.mkdir(parents=True, exist_ok=True)

            if render_mermaid(mmd_content, png_path, mmdc):
                rel_path = f"assets/{png_name}"
                log.info(f"[Mermaid] 圖 {idx} 渲染完成：{png_name}")
                return f"![{title}]({rel_path})\n\n> 圖：{title}"
            log.warning(f"[Mermaid] 圖 {idx} 渲染失敗，保留原始碼")

        # 無法渲染時，保留來源並提示手動處理
        return (
            f"<!-- ⚠️ Mermaid 圖表（需手動插入 PNG）：{title} -->\n\n"
            f"```mermaid\n{mmd_content}```\n\n"
            f"> ⚠️ 請使用 mermaid-cli（mmdc）將上方程式碼渲染為圖片後手動插入"
        )

    return MERMAID_BLOCK_RE.sub(replace_block, markdown)


# ──────────────────────────────────────────────
# Pandoc 轉換
# ──────────────────────────────────────────────
def convert_to_docx(
    md_path: Path,
    template_path: Path,
    output_path: Path,
    title: str = "",
) -> bool:
    """呼叫 Pandoc 執行 Markdown → DOCX 轉換"""
    cmd = [
        "pandoc",
        str(md_path),
        "--from", "markdown+smart+pipe_tables+fenced_code_blocks",
        "--to", "docx",
        "--reference-doc", str(template_path),
        "--toc",
        "--toc-depth=3",
        "--wrap=none",
        "--output", str(output_path),
    ]

    if title:
        cmd += ["--metadata", f"title={title}"]

    log.info(f"[Pandoc] 執行轉換：{md_path.name} → {output_path.name}")
    log.debug(f"指令：{' '.join(cmd)}")

    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        log.error(f"[Pandoc] 轉換失敗：\n{result.stderr}")
        return False

    if result.stderr:
        log.warning(f"[Pandoc] 警告：{result.stderr}")

    return True


# ──────────────────────────────────────────────
# 主程式
# ──────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(
        description="SDLC Markdown → Word 套版轉換腳本（Mermaid 圖形）"
    )
    parser.add_argument(
        "--input", "-i", required=True,
        help="輸入 Markdown 檔案路徑"
    )
    parser.add_argument(
        "--template", "-t", required=True,
        help="Word 套版模板 (.docx) 路徑"
    )
    parser.add_argument(
        "--output", "-o", required=True,
        help="輸出 Word 文件 (.docx) 路徑"
    )
    parser.add_argument(
        "--title",
        default="",
        help="文件標題（選用，顯示於 Word 文件屬性）"
    )
    parser.add_argument(
        "--no-mermaid", action="store_true",
        help="跳過 Mermaid 渲染（保留程式碼區塊）"
    )

    args = parser.parse_args()

    md_path = Path(args.input)
    template_path = Path(args.template)
    output_path = Path(args.output)

    # ── 前置檢查 ──
    log.info("=== SDLC Markdown → Word 轉換 ===")

    if not check_pandoc():
        log.error("Pandoc 未安裝。請參考 references/pandoc-guide.md §1 安裝說明。")
        sys.exit(1)

    if not md_path.exists():
        log.error(f"輸入檔案不存在：{md_path}")
        sys.exit(1)

    if not template_path.exists():
        log.error(
            f"套版模板不存在：{template_path}\n"
            "請參考 references/pandoc-guide.md §3 建立初始套版。"
        )
        sys.exit(1)

    output_path.parent.mkdir(parents=True, exist_ok=True)

    # ── 讀取 Markdown ──
    markdown = md_path.read_text(encoding="utf-8")

    # ── Mermaid 處理 ──
    mmdc = None if args.no_mermaid else check_mermaid()
    assets_dir = md_path.parent / "assets"

    if not args.no_mermaid:
        if not mmdc:
            log.warning(
                "找不到 mermaid-cli（mmdc），Mermaid 圖表將保留為程式碼區塊。\n"
                "安裝：npm install -g @mermaid-js/mermaid-cli"
            )
        markdown = process_mermaid_blocks(markdown, assets_dir, mmdc)

    # ── 寫入暫存 Markdown（含已替換的圖片路徑）──
    with tempfile.NamedTemporaryFile(
        mode="w", suffix=".md", encoding="utf-8",
        dir=md_path.parent, delete=False
    ) as tmp:
        tmp.write(markdown)
        tmp_md_path = Path(tmp.name)

    # ── Pandoc 轉換 ──
    try:
        success = convert_to_docx(
            tmp_md_path, template_path, output_path,
            title=args.title or md_path.stem
        )
    finally:
        tmp_md_path.unlink(missing_ok=True)

    if not success:
        sys.exit(1)

    # ── 結果摘要 ──
    size_kb = output_path.stat().st_size // 1024
    log.info("=== 轉換完成 ===")
    log.info(f"輸出：{output_path}  ({size_kb} KB)")
    print("\n✅ Word 文件已產出。轉換後請確認：")
    print("   1. 在 Word 中更新目錄（右鍵 → 更新欄位 → 更新整個目錄）")
    print("   2. 確認頁首頁尾資訊正確（文件編號、機密等級）")
    print("   3. 確認 Mermaid 圖表已正確插入（或手動補充）")


if __name__ == "__main__":
    main()
