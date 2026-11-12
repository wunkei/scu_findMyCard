# -*- coding: utf-8 -*-
import os
import re
from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_color):
    tcPr = cell._element.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_color}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._element.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def create_document():
    doc = Document()

    # 页面边距设置 (2.54cm 标样)
    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        section.different_first_page_header_footer = True

        # 页脚添加页码
        footer = section.footer
        f_p = footer.paragraphs[0]
        f_p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        f_run = f_p.add_run("四川大学「找卡宝」系统设计说明书")
        f_run.font.size = Pt(9)
        f_run.font.color.rgb = RGBColor(128, 128, 128)

    # 默认正文字体
    normal_style = doc.styles['Normal']
    normal_style.font.name = 'Microsoft YaHei'
    normal_style.font.size = Pt(10.5)
    normal_style.font.color.rgb = RGBColor(51, 51, 51)
    normal_style._element.rPr.rFonts.set(qn('w:eastAsia'), 'Microsoft YaHei')

    # 读取 Markdown
    md_path = r'D:\Code\MyApplication\docs\四川大学「找卡宝」系统设计说明书.md'
    with open(md_path, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')

    # 封面
    p_pre = doc.add_paragraph()
    p_pre.paragraph_format.space_before = Pt(36)

    p_num = doc.add_paragraph()
    r_num = p_num.add_run("文档编号：SCU-FMC-DSD-2026-V1.0")
    r_num.font.size = Pt(11)
    r_num.font.color.rgb = RGBColor(120, 120, 120)

    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(72)
    p_title.paragraph_format.space_after = Pt(18)
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_title = p_title.add_run("四川大学智能卡片失物招领系统\n（找卡宝）")
    r_title.font.size = Pt(26)
    r_title.font.bold = True
    r_title.font.color.rgb = RGBColor(24, 76, 120)

    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_after = Pt(120)
    p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_sub = p_sub.add_run("设 计 说 明 书")
    r_sub.font.size = Pt(20)
    r_sub.font.bold = True
    r_sub.font.color.rgb = RGBColor(70, 70, 70)

    p_meta = doc.add_paragraph()
    p_meta.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_meta.paragraph_format.line_spacing = 1.6
    r_meta = p_meta.add_run(
        "课题组：四川大学软件工程课题组\n"
        "版本号：Version 1.0.0\n"
        "日期：2026 年 11 月 11 日"
    )
    r_meta.font.size = Pt(12)
    r_meta.font.color.rgb = RGBColor(80, 80, 80)

    doc.add_page_break()

    # 正文解析状态
    in_code_block = False
    code_lines = []
    code_lang = ""
    in_table = False
    table_lines = []

    def flush_table(tbl_lines):
        if not tbl_lines:
            return
        rows_data = []
        for l in tbl_lines:
            # 过滤分割行 |---|---|
            if re.match(r'^\s*\|?\s*[-:]+\s*\|', l):
                continue
            cells = [c.strip() for c in l.strip().strip('|').split('|')]
            if cells and any(cells):
                rows_data.append(cells)
        if not rows_data:
            return

        num_cols = max(len(r) for r in rows_data)
        table = doc.add_table(rows=len(rows_data), cols=num_cols)
        table.alignment = WD_TABLE_ALIGNMENT.CENTER
        table.autofit = True

        for i, row in enumerate(rows_data):
            for j in range(num_cols):
                val = row[j] if j < len(row) else ""
                val_clean = val.replace('<br>', '\n').replace('&nbsp;', ' ')
                cell = table.cell(i, j)
                cell.text = val_clean
                cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
                set_cell_margins(cell, top=120, bottom=120, left=150, right=150)

                # 表头样式
                if i == 0:
                    set_cell_background(cell, "EBF3FA")
                    for p in cell.paragraphs:
                        p.paragraph_format.space_before = Pt(3)
                        p.paragraph_format.space_after = Pt(3)
                        for r in p.runs:
                            r.font.bold = True
                            r.font.size = Pt(9.5)
                            r.font.color.rgb = RGBColor(24, 76, 120)
                else:
                    if i % 2 == 1:
                        set_cell_background(cell, "FAFCFF")
                    else:
                        set_cell_background(cell, "FFFFFF")
                    for p in cell.paragraphs:
                        p.paragraph_format.space_before = Pt(2)
                        p.paragraph_format.space_after = Pt(2)
                        for r in p.runs:
                            r.font.size = Pt(9)
                            r.font.color.rgb = RGBColor(50, 50, 50)

        # 表格底边空行
        p_after = doc.add_paragraph()
        p_after.paragraph_format.space_after = Pt(6)

    def flush_code(lines_arr, lang):
        if not lines_arr:
            return
        p_code = doc.add_paragraph()
        p_code.paragraph_format.space_before = Pt(4)
        p_code.paragraph_format.space_after = Pt(6)
        p_code.paragraph_format.left_indent = Inches(0.2)
        p_code.paragraph_format.right_indent = Inches(0.2)

        code_text = "\n".join(lines_arr)
        r = p_code.add_run(code_text)
        r.font.name = 'Consolas'
        r.font.size = Pt(9)
        r.font.color.rgb = RGBColor(40, 40, 40)
        r._element.rPr.rFonts.set(qn('w:eastAsia'), 'Consolas')

    # 跳过原 Markdown 顶部的标题和元信息，直接从正文开始（已经制作了标准封面）
    skip_header = True

    for line in lines:
        if line.startswith("## 目录"):
            skip_header = False
            continue

        if skip_header:
            continue

        # 代码块起止
        if line.strip().startswith("```"):
            if in_code_block:
                in_code_block = False
                flush_code(code_lines, code_lang)
                code_lines = []
            else:
                if in_table:
                    in_table = False
                    flush_table(table_lines)
                    table_lines = []
                in_code_block = True
                code_lang = line.strip().replace("```", "")
                code_lines = []
            continue

        if in_code_block:
            code_lines.append(line)
            continue

        # 表格判断
        if line.strip().startswith("|") and line.strip().endswith("|"):
            in_table = True
            table_lines.append(line)
            continue
        else:
            if in_table:
                in_table = False
                flush_table(table_lines)
                table_lines = []

        # 标题处理
        if line.startswith("## 一、") or line.startswith("## 二、") or line.startswith("## 三、") or \
           line.startswith("## 四、") or line.startswith("## 五、") or line.startswith("## 六、") or \
           line.startswith("## 七、") or line.startswith("## 八、"):
            h_text = line.replace("##", "").strip()
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(18)
            p.paragraph_format.space_after = Pt(8)
            p.paragraph_format.keep_with_next = True
            r = p.add_run(h_text)
            r.font.size = Pt(16)
            r.font.bold = True
            r.font.color.rgb = RGBColor(24, 76, 120)
            continue

        if line.startswith("### "):
            h_text = line.replace("###", "").strip()
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(14)
            p.paragraph_format.space_after = Pt(6)
            p.paragraph_format.keep_with_next = True
            r = p.add_run(h_text)
            r.font.size = Pt(13)
            r.font.bold = True
            r.font.color.rgb = RGBColor(40, 80, 110)
            continue

        if line.startswith("#### "):
            h_text = line.replace("####", "").strip()
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(10)
            p.paragraph_format.space_after = Pt(4)
            p.paragraph_format.keep_with_next = True
            r = p.add_run(h_text)
            r.font.size = Pt(11)
            r.font.bold = True
            r.font.color.rgb = RGBColor(60, 60, 60)
            continue

        if line.strip() == "---":
            continue

        if not line.strip():
            continue

        # 普通段落处理
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.35

        # 列表缩进
        raw_text = line
        if raw_text.strip().startswith("- ") or raw_text.strip().startswith("* "):
            p.paragraph_format.left_indent = Inches(0.2)
            raw_text = raw_text.strip()[2:]
        elif re.match(r'^\d+\.\s', raw_text.strip()):
            p.paragraph_format.left_indent = Inches(0.2)

        # 粗体标记解析 **text**
        parts = re.split(r'(\*\*.*?\*\*)', raw_text)
        for part in parts:
            if part.startswith("**") and part.endswith("**") and len(part) >= 4:
                bold_text = part[2:-2]
                r = p.add_run(bold_text)
                r.font.bold = True
                r.font.color.rgb = RGBColor(30, 30, 30)
            else:
                # 行内代码块 `code`
                sub_parts = re.split(r'(`.*?`)', part)
                for sp in sub_parts:
                    if sp.startswith("`") and sp.endswith("`") and len(sp) >= 2:
                        code_txt = sp[1:-1]
                        r = p.add_run(code_txt)
                        r.font.name = 'Consolas'
                        r.font.size = Pt(9.5)
                        r.font.color.rgb = RGBColor(190, 45, 45)
                    else:
                        r = p.add_run(sp)
                        r.font.size = Pt(10.5)

    if in_table:
        flush_table(table_lines)
    if in_code_block:
        flush_code(code_lines, code_lang)

    output_path = r'D:\Code\MyApplication\docs\四川大学「找卡宝」系统设计说明书.docx'
    doc.save(output_path)
    print("Successfully converted to:", output_path)

if __name__ == '__main__':
    create_document()
