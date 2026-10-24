#!/usr/bin/env python3
"""Builds the PDF guides from the same markdown articles the app shows.

Each guide in content/<lang>/guides.json becomes a PDF with a cover, contents, one
chapter per article, a combined sources list and the disclaimer.
Requires: pip install reportlab   (fonts: DejaVu Sans, for the ₹ sign and accents)
"""
import html, json, os, re, sys

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle,
                                ListFlowable, ListItem)

sys.path.insert(0, os.path.dirname(__file__))
from content_qa import front

FONT_DIRS = ["/usr/share/fonts/truetype/dejavu", "/usr/share/fonts/dejavu", "/Library/Fonts", "C:/Windows/Fonts"]
ACCENT = colors.HexColor("#357A61")
INK = colors.HexColor("#2A2420")
SUB = colors.HexColor("#71665E")

def register_fonts():
    for d in FONT_DIRS:
        reg, bold = os.path.join(d, "DejaVuSans.ttf"), os.path.join(d, "DejaVuSans-Bold.ttf")
        if os.path.exists(reg) and os.path.exists(bold):
            pdfmetrics.registerFont(TTFont("Body", reg))
            pdfmetrics.registerFont(TTFont("Body-Bold", bold))
            from reportlab.pdfbase.pdfmetrics import registerFontFamily
            registerFontFamily("Body", normal="Body", bold="Body-Bold", italic="Body", boldItalic="Body-Bold")
            return "Body", "Body-Bold"
    return "Helvetica", "Helvetica-Bold"

REG, BOLD = register_fonts()
S = {
    "cover_title": ParagraphStyle("ct", fontName=BOLD, fontSize=30, leading=36, textColor=INK, alignment=TA_CENTER),
    "cover_sub": ParagraphStyle("cs", fontName=REG, fontSize=13, leading=18, textColor=SUB, alignment=TA_CENTER),
    "h1": ParagraphStyle("h1", fontName=BOLD, fontSize=20, leading=25, textColor=INK, spaceAfter=8),
    "h2": ParagraphStyle("h2", fontName=BOLD, fontSize=14, leading=18, textColor=ACCENT, spaceBefore=10, spaceAfter=4),
    "h3": ParagraphStyle("h3", fontName=BOLD, fontSize=12, leading=16, textColor=INK, spaceBefore=8, spaceAfter=3),
    "p": ParagraphStyle("p", fontName=REG, fontSize=10.5, leading=15, textColor=INK, spaceAfter=6),
    "small": ParagraphStyle("s", fontName=REG, fontSize=8.5, leading=12, textColor=SUB, spaceAfter=3),
    "quote": ParagraphStyle("q", fontName=REG, fontSize=10.5, leading=15, textColor=INK, backColor=colors.HexColor("#F1EBE4"),
                            borderPadding=6, spaceBefore=4, spaceAfter=8),
    "toc": ParagraphStyle("toc", fontName=REG, fontSize=11.5, leading=19, textColor=INK),
}

def inline(t):
    t = html.escape(t, quote=False)
    t = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", t)
    t = re.sub(r"(?<![\w*])\*(.+?)\*(?!\w)", r"<i>\1</i>", t)
    t = re.sub(r"\[([^\]]+)\]\(([^)\s]+)\)", r'<link href="\2" color="#357A61">\1</link>', t)
    return t

def md_flow(body, skip_sources=True):
    flow, para, bullets, kind = [], [], [], "p"
    def flush():
        nonlocal para, bullets, kind
        if para:
            flow.append(Paragraph(inline(" ".join(para)), S["quote" if kind == "q" else "p"]))
        if bullets:
            flow.append(ListFlowable([ListItem(Paragraph(inline(b), S["p"]), leftIndent=12) for b in bullets],
                                     bulletType="bullet", start="•", leftIndent=12))
        para, bullets, kind = [], [], "p"
    for raw in body.splitlines():
        line = raw.strip()
        if skip_sources and line.startswith("## Sources"):
            break
        if not line or line == "---":
            flush(); continue
        if line.startswith("#"):
            flush()
            level = len(line) - len(line.lstrip("#"))
            flow.append(Paragraph(inline(line.lstrip("#").strip()), S["h2" if level <= 2 else "h3"]))
        elif re.match(r"^([-*]|\d+[.)]) ", line):
            if para: flush()
            bullets.append(re.sub(r"^([-*]|\d+[.)]) ", "", line))
        elif line.startswith(">"):
            if kind != "q": flush()
            kind = "q"; para.append(line.lstrip("> "))
        else:
            if bullets: flush()
            para.append(line)
    flush()
    return flow

def sources_of(body):
    if "## Sources" not in body:
        return []
    return [l.strip()[2:] for l in body.split("## Sources", 1)[1].splitlines() if l.strip().startswith("- ")]

def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont(REG, 8)
    canvas.setFillColor(SUB)
    canvas.drawString(18 * mm, 10 * mm, doc.guide_title + " · Education, not medical advice")
    canvas.drawRightString(A4[0] - 18 * mm, 10 * mm, str(doc.page))
    canvas.restoreState()

def build_guide(lang_dir, g, out_path, version, reviewed):
    articles = []
    for aid in g.get("articles", []):
        path = os.path.join(lang_dir, "articles", aid + ".md")
        meta, body = front(open(path, encoding="utf-8").read())
        articles.append((meta, body))
    extra = []
    for rel in g.get("documents", []):
        meta, body = front(open(os.path.join(lang_dir, rel), encoding="utf-8").read())
        extra.append((meta or {"title": g["title"]}, body))
    chapters = extra + articles
    doc = SimpleDocTemplate(out_path, pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
                            topMargin=20 * mm, bottomMargin=18 * mm, title=g["title"], author="Fit90")
    doc.guide_title = g["title"]
    story = [Spacer(1, 70 * mm), Paragraph(g["title"], S["cover_title"]), Spacer(1, 6 * mm),
             Paragraph(g.get("subtitle", ""), S["cover_sub"]), Spacer(1, 80 * mm),
             Paragraph(f"Fit90 · content version {version} · last reviewed {reviewed}", S["cover_sub"]),
             Paragraph("Free guide. Education, not medical advice.", S["cover_sub"]), PageBreak()]
    story.append(Paragraph("Contents", S["h1"]))
    for i, (meta, _) in enumerate(chapters, 1):
        story.append(Paragraph(f"{i}. {inline(meta.get('title', ''))}", S["toc"]))
    story.append(PageBreak())
    all_sources = []
    for i, (meta, body) in enumerate(chapters, 1):
        story.append(Paragraph(f"{i}. {inline(meta.get('title', ''))}", S["h1"]))
        story += md_flow(body)
        for s in sources_of(body):
            if s not in all_sources:
                all_sources.append(s)
        story.append(PageBreak())
    if g.get("checklist_days"):
        story.append(Paragraph("Your 30-day checklist", S["h1"]))
        rows = [["Day", "Today's focus", "Done"]] + [[str(d["day"]), Paragraph(inline(d["text"]), S["p"]), "☐"]
                                                     for d in g["checklist_days"]]
        t = Table(rows, colWidths=[14 * mm, 130 * mm, 16 * mm], repeatRows=1)
        t.setStyle(TableStyle([("FONT", (0, 0), (-1, -1), REG, 10), ("FONT", (0, 0), (-1, 0), BOLD, 10),
                               ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#E8E1D9")),
                               ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#DCEDE4")),
                               ("VALIGN", (0, 0), (-1, -1), "MIDDLE")]))
        story += [t, PageBreak()]
    story.append(Paragraph("Sources", S["h1"]))
    for s in all_sources:
        story.append(Paragraph(inline(s), S["small"]))
    story.append(Spacer(1, 8 * mm))
    story.append(Paragraph("Disclaimer", S["h2"]))
    story.append(Paragraph(inline(g.get("disclaimer", "")), S["p"]))
    doc.build(story, onFirstPage=lambda c, d: None, onLaterPages=footer)

def main(src, dst):
    os.makedirs(dst, exist_ok=True)
    meta = json.load(open(os.path.join(src, "content.json"), encoding="utf-8"))
    lang_dir = os.path.join(src, "en")
    guides_path = os.path.join(lang_dir, "guides.json")
    if not os.path.exists(guides_path):
        print("  no guides.json, skipping")
        return
    guides = json.load(open(guides_path, encoding="utf-8"))
    for g in guides["guides"]:
        g.setdefault("disclaimer", guides.get("disclaimer", ""))
        out = os.path.join(dst, g["id"] + ".pdf")
        build_guide(lang_dir, g, out, meta.get("version", ""), meta.get("last_reviewed", ""))
        print(f"  {g['id']}.pdf ({os.path.getsize(out) // 1024} KB)")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
