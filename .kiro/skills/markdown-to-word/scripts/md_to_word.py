#!/usr/bin/env python3
"""
SDLC Markdown → Word 套版轉換腳本
===================================
使用 Pandoc 將 FSD/SD Markdown 文件套用公司 Word 模板，產出 .docx。

功能：
  1. 自動將 Markdown 中的 plantuml 程式碼區塊替換為對應 PNG 圖片引用
  2. 呼叫 Pandoc 執行 Markdown → DOCX 轉換（套用 reference-doc 樣式）
  3. 驗證輸出檔案存在

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
import os
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


def check_plantuml() -> str | None:
    """尋找 plantuml.jar，回傳路徑或 None"""
    candidates = [
        Path("tools/plantuml.jar"),
        Path("plantuml.jar"),
        Path.home() / "tools/plantuml.jar",
    ]
    for p in candidates:
        if p.exists():
            log.info(f"PlantUML jar: {p}")
            return str(p)
    return None


# ──────────────────────────────────────────────
# PlantUML 處理
# ──────────────────────────────────────────────
PLANTUML_BLOCK_RE = re.compile(
    r"```plantuml\n(.*?)```",
    re.DOTALL
)

DIAGRAM_TITLE_RE = re.compile(r"title\s+(.+)")


def extract_diagram_title(puml_content: str) -> str:
    """從 PlantUML 內容中提取 title"""
    match = DIAGRAM_TITLE_RE.search(puml_content)
    return match.group(1).strip() if match else "架構圖"


def render_plantuml(puml_content: str, output_path: Path, plantuml_jar: str) -> bool:
    """將 PlantUML 內容渲染為 PNG"""
    with tempfile.NamedTemporaryFile(
        mode="w", suffix=".puml", encoding="utf-8", delete=False
    ) as f:
        f.write(puml_content)
        tmp_path = f.name

    try:
        result = subprocess.run(
            ["java", "-jar", plantuml_jar, "-png", "-charset", "UTF-8",
             "-o", str(output_path.parent), tmp_path],
            capture_output=True, text=True
        )
        if result.returncode != 0:
            log.warning(f"PlantUML 渲染失敗：{result.stderr}")
            return False

        # plantuml 輸出的 PNG 與 tmp 同名，移動至目標路徑
        generated = Path(tmp_path).with_suffix(".png")
        if generated.exists():
            shutil.move(str(generated), str(output_path))
            return True
        return False
    finally:
        Path(tmp_path).unlink(missing_ok=True)


def process_plantuml_blocks(
    markdown: str,
    assets_dir: Path,
    plantuml_jar: str | None
) -> str:
    """
    將 Markdown 中的 plantuml 區塊替換為 PNG 圖片引用。
    若 PlantUML 不可用，保留原始程式碼區塊並加上警告注釋。
    """
    diagram_count = [0]

    def replace_block(match: re.Match) -> str:
        puml_content = match.group(1)
        diagram_count[0] += 1
        idx = diagram_count[0]
        title = extract_diagram_title(puml_content)

        if plantuml_jar:
            png_name = f"diagram-{idx:02d}.png"
            png_path = assets_dir / png_name
            assets_dir.mkdir(parents=True, exist_ok=True)

            success = render_plantuml(puml_content, png_path, plantuml_jar)
            if success:
                rel_path = f"assets/{png_name}"
                log.info(f"[PlantUML] 圖 {idx} 渲染完成：{png_name}")
                return f"![{title}]({rel_path})\n\n> 圖：{title}"
            else:
                log.warning(f"[PlantUML] 圖 {idx} 渲染失敗，保留原始碼")

        # 無法渲染時，加上提示注釋
        return (
            f"<!-- ⚠️ PlantUML 圖表（需手動插入 PNG）：{title} -->\n\n"
            f"```plantuml\n{puml_content}```\n\n"
            f"> ⚠️ 請使用 PlantUML 將上方程式碼渲染為圖片後手動插入"
        )

    return PLANTUML_BLOCK_RE.sub(replace_block, markdown)


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
        description="SDLC Markdown → Word 套版轉換腳本"
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
        "--no-plantuml", action="store_true",
        help="跳過 PlantUML 渲染（保留程式碼區塊）"
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

    # ── PlantUML 處理 ──
    plantuml_jar = None if args.no_plantuml else check_plantuml()
    assets_dir = md_path.parent / "assets"

    if not args.no_plantuml:
        if not plantuml_jar:
            log.warning(
                "找不到 plantuml.jar，PlantUML 圖表將保留為程式碼區塊。\n"
                "下載：https://plantuml.com/download 並置於 tools/plantuml.jar"
            )
        markdown = process_plantuml_blocks(markdown, assets_dir, plantuml_jar)

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
    print("   3. 確認 PlantUML 圖表已正確插入（或手動補充）")


if __name__ == "__main__":
    main()
