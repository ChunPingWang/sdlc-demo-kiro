#!/usr/bin/env python3
"""
SDLC Document Converter
=======================
本地端文件轉換腳本，自動判斷輸入格式，選用最適合的工具轉換為 Markdown。

工具選型：
  - PDF（有文字層）→ Docling
  - PDF（掃描件）  → Docling + OCR
  - .docx / .xlsx / .pptx → MarkItDown
  - 批次模式       → 自動偵測每個檔案類型

使用方式：
  python convert.py --input <file_or_dir> --output <output_dir> [--ocr] [--batch]

範例：
  python convert.py --input sdlc/inputs/raw/spec.pdf --output sdlc/inputs/
  python convert.py --input sdlc/inputs/raw/ --output sdlc/inputs/ --batch
  python convert.py --input sdlc/inputs/raw/scan.pdf --output sdlc/inputs/ --ocr
"""

import argparse
import logging
import shutil
import sys
from pathlib import Path

# ──────────────────────────────────────────────
# 設定日誌
# ──────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)

# ──────────────────────────────────────────────
# 格式分類
# ──────────────────────────────────────────────
PDF_EXTENSIONS = {".pdf"}
MARKITDOWN_EXTENSIONS = {".docx", ".xlsx", ".pptx", ".doc", ".xls", ".ppt"}
SUPPORTED_EXTENSIONS = PDF_EXTENSIONS | MARKITDOWN_EXTENSIONS


# ──────────────────────────────────────────────
# 工具可用性檢查
# ──────────────────────────────────────────────
def check_docling() -> bool:
    try:
        from docling.document_converter import DocumentConverter  # noqa: F401
        return True
    except ImportError:
        return False


def check_markitdown() -> bool:
    try:
        from markitdown import MarkItDown  # noqa: F401
        return True
    except ImportError:
        return False


# ──────────────────────────────────────────────
# 轉換函式
# ──────────────────────────────────────────────
def convert_with_docling(input_path: Path, output_dir: Path, ocr: bool = False) -> Path:
    """使用 Docling 轉換 PDF → Markdown"""
    if not check_docling():
        log.error("Docling 未安裝。請執行：pip install docling")
        sys.exit(1)

    from docling.document_converter import DocumentConverter
    from docling.datamodel.pipeline_options import PipelineOptions, PdfPipelineOptions

    log.info(f"[Docling] 轉換：{input_path.name}  OCR={'開啟' if ocr else '關閉'}")

    pipeline_options = PdfPipelineOptions()
    pipeline_options.do_ocr = ocr
    pipeline_options.do_table_structure = True

    converter = DocumentConverter()
    result = converter.convert(str(input_path))

    markdown = result.document.export_to_markdown()

    # 建立圖片輸出目錄
    assets_dir = output_dir / "assets" / input_path.stem
    assets_dir.mkdir(parents=True, exist_ok=True)

    output_path = output_dir / f"{input_path.stem}-converted.md"
    output_path.write_text(markdown, encoding="utf-8")

    log.info(f"[Docling] 完成 → {output_path}")
    return output_path


def convert_with_markitdown(input_path: Path, output_dir: Path) -> Path:
    """使用 MarkItDown 轉換 Office 文件 → Markdown"""
    if not check_markitdown():
        log.error("MarkItDown 未安裝。請執行：pip install markitdown[all]")
        sys.exit(1)

    from markitdown import MarkItDown

    log.info(f"[MarkItDown] 轉換：{input_path.name}")

    md = MarkItDown()
    result = md.convert(str(input_path))

    output_path = output_dir / f"{input_path.stem}-converted.md"
    output_path.write_text(result.text_content, encoding="utf-8")

    log.info(f"[MarkItDown] 完成 → {output_path}")
    return output_path


def convert_file(input_path: Path, output_dir: Path, ocr: bool = False) -> Path | None:
    """判斷格式並選用適合工具"""
    ext = input_path.suffix.lower()

    if ext not in SUPPORTED_EXTENSIONS:
        log.warning(f"不支援的格式：{ext}，跳過 {input_path.name}")
        return None

    output_dir.mkdir(parents=True, exist_ok=True)

    if ext in PDF_EXTENSIONS:
        return convert_with_docling(input_path, output_dir, ocr=ocr)
    elif ext in MARKITDOWN_EXTENSIONS:
        return convert_with_markitdown(input_path, output_dir)

    return None


def convert_batch(input_dir: Path, output_dir: Path, ocr: bool = False) -> list[Path]:
    """批次轉換資料夾中所有支援格式的文件"""
    files = [
        f for f in input_dir.iterdir()
        if f.is_file() and f.suffix.lower() in SUPPORTED_EXTENSIONS
    ]

    if not files:
        log.warning(f"在 {input_dir} 中找不到支援的文件格式")
        return []

    log.info(f"批次模式：找到 {len(files)} 個文件")
    results = []

    for f in sorted(files):
        output = convert_file(f, output_dir, ocr=ocr)
        if output:
            results.append(output)

    return results


# ──────────────────────────────────────────────
# 後處理：清理常見雜訊
# ──────────────────────────────────────────────
def clean_markdown(md_path: Path) -> None:
    """清理轉換後 Markdown 中常見的雜訊"""
    text = md_path.read_text(encoding="utf-8")
    lines = text.splitlines()
    cleaned = []

    for line in lines:
        # 移除頁碼模式（如 "- 1 -", "Page 1 of 10"）
        if line.strip().lower().startswith("page ") and "of" in line.lower():
            continue
        if line.strip() in {str(i) for i in range(1, 500)}:
            continue
        cleaned.append(line)

    # 合併連續空行（超過 2 行空行縮減為 1 行）
    result = []
    blank_count = 0
    for line in cleaned:
        if line.strip() == "":
            blank_count += 1
            if blank_count <= 1:
                result.append(line)
        else:
            blank_count = 0
            result.append(line)

    md_path.write_text("\n".join(result), encoding="utf-8")
    log.info(f"[清理] 完成後處理：{md_path.name}")


# ──────────────────────────────────────────────
# 主程式
# ──────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(
        description="SDLC Document Converter — 本地端文件轉 Markdown"
    )
    parser.add_argument(
        "--input", "-i", required=True,
        help="輸入檔案路徑或資料夾路徑（批次模式）"
    )
    parser.add_argument(
        "--output", "-o", required=True,
        help="輸出 Markdown 的目標資料夾"
    )
    parser.add_argument(
        "--ocr", action="store_true",
        help="啟用 OCR（用於掃描 PDF）"
    )
    parser.add_argument(
        "--batch", action="store_true",
        help="批次模式：轉換 --input 資料夾中所有支援格式的文件"
    )
    parser.add_argument(
        "--no-clean", action="store_true",
        help="跳過後處理清理步驟"
    )

    args = parser.parse_args()

    input_path = Path(args.input)
    output_dir = Path(args.output)

    # 環境檢查
    log.info("=== SDLC Document Converter ===")
    log.info(f"Docling    : {'✓ 可用' if check_docling() else '✗ 未安裝 (pip install docling)'}")
    log.info(f"MarkItDown : {'✓ 可用' if check_markitdown() else '✗ 未安裝 (pip install markitdown[all])'}")

    if not input_path.exists():
        log.error(f"輸入路徑不存在：{input_path}")
        sys.exit(1)

    # 執行轉換
    if args.batch or input_path.is_dir():
        outputs = convert_batch(input_path, output_dir, ocr=args.ocr)
    else:
        output = convert_file(input_path, output_dir, ocr=args.ocr)
        outputs = [output] if output else []

    # 後處理清理
    if not args.no_clean:
        for md_path in outputs:
            if md_path and md_path.exists():
                clean_markdown(md_path)

    # 輸出摘要
    log.info("=== 轉換完成 ===")
    if outputs:
        log.info(f"成功轉換 {len(outputs)} 個文件：")
        for p in outputs:
            if p:
                log.info(f"  → {p}")
        print("\n下一步：")
        print(f"  /generate-fsd #{outputs[0]}")
        print(f"  或")
        print(f"  /generate-sd  #{outputs[0]} #sdlc/fsd/output/FSD-*.md")
    else:
        log.warning("沒有成功轉換的文件")
        sys.exit(1)


if __name__ == "__main__":
    main()
