#!/usr/bin/env python3
"""강화 정보 창의 숫자·영문 글꼴(enhance_num.ttf)을 만든다.

Playfair Display (SIL OFL 1.1) 의 굵기 700 을 뽑아 라틴 글자만 남기고, 숫자는 높이가 고른 '라이닝' 모양(zero.lf …)으로 바꾼다:
기본 모양(올드스타일)은 3·4·5·7·9 가 줄 아래로 내려가 칸 안의 숫자가 들쭉날쭉해 보이기 때문이다.
OFL 의 예약 이름을 피하려고 글꼴 이름은 "RT Enhance Numerals" 로 바꾼다.

쓰기: python3 tools/gen_enhance_font.py <PlayfairDisplay[wght].ttf> <enhance_num.ttf>
      python3 tools/gen_enhance_font.py --fix-signs <enhance_num.ttf>   (이미 만든 파일의 + − = 만 굵게)
"""
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset


def thicken_signs(font, thick=84):
    """+ − = 의 가는 획을 굵게 한다.

    Playfair 의 더하기표는 가로 획이 30/1000 em 이라 12px 로 그리면 0.4px 가 되어 사라진다
    (강화 정보 창의 "+13%" 가 "|13%" 로 보였다). 하이픈의 굵기(83)에 맞춘다. 글자의 폭과 자리는 그대로.
    """
    from fontTools.ttLib.tables._g_l_y_f import GlyphCoordinates
    from fontTools.ttLib.tables import ttProgram
    glyf = font["glyf"]
    h = thick // 2

    def put(name, contours):
        g = glyf[name]
        pts, ends = [], []
        for c in contours:
            pts += c
            ends.append(len(pts) - 1)
        g.coordinates = GlyphCoordinates(pts)
        g.endPtsOfContours = ends
        g.flags = bytearray([1] * len(pts))
        g.numberOfContours = len(ends)
        g.program = ttProgram.Program()
        g.program.fromBytecode(b"")
        g.recalcBounds(glyf)

    def bar(x0, x1, cy):
        return [(x1, cy + h), (x1, cy - h), (x0, cy - h), (x0, cy + h)]

    if "plus" in glyf.glyphs and glyf["plus"].numberOfContours == 1:
        g = glyf["plus"]
        cx, cy = (g.xMin + g.xMax) // 2, (g.yMin + g.yMax) // 2
        x0, x1, y0, y1 = g.xMin, g.xMax, g.yMin, g.yMax
        put("plus", [[(cx + h, y1), (cx + h, cy + h), (x1, cy + h), (x1, cy - h), (cx + h, cy - h), (cx + h, y0),
                      (cx - h, y0), (cx - h, cy - h), (x0, cy - h), (x0, cy + h), (cx - h, cy + h), (cx - h, y1)]])
    if "minus" in glyf.glyphs and glyf["minus"].numberOfContours == 1:
        g = glyf["minus"]
        put("minus", [bar(g.xMin, g.xMax, (g.yMin + g.yMax) // 2)])
    if "equal" in glyf.glyphs and glyf["equal"].numberOfContours == 2:
        g = glyf["equal"]
        cy, gap = (g.yMin + g.yMax) // 2, 30
        t = thick - 14
        put("equal", [[(g.xMax, cy + gap + t), (g.xMax, cy + gap), (g.xMin, cy + gap), (g.xMin, cy + gap + t)],
                      [(g.xMax, cy - gap), (g.xMax, cy - gap - t), (g.xMin, cy - gap - t), (g.xMin, cy - gap)]])


if len(sys.argv) == 3 and sys.argv[1] == "--fix-signs":
    # 이미 만든 enhance_num.ttf 를 그 자리에서 고친다 (원본 Playfair 파일이 없을 때)
    f = TTFont(sys.argv[2])
    thicken_signs(f)
    f.save(sys.argv[2])
    print(sys.argv[2], "signs thickened")
    sys.exit(0)

src, out = sys.argv[1], sys.argv[2]
f = TTFont(src)
f = instancer.instantiateVariableFont(f, {"wght": 700})
digits = ["zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"]
order = set(f.getGlyphOrder())
for table in f["cmap"].tables:
    for code, name in list(table.cmap.items()):
        if name in digits and name + ".lf" in order:
            table.cmap[code] = name + ".lf"
opt = subset.Options()
opt.layout_features = ["kern", "liga"]
opt.name_IDs = ["*"]
opt.notdef_outline = True
opt.glyph_names = True
sub = subset.Subsetter(opt)
sub.populate(unicodes=list(range(0x20, 0x7F)) + list(range(0xA0, 0x100)) + [0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2026, 0x2212, 0x00D7])
sub.subset(f)
for rec in f["name"].names:
    if rec.nameID in (1, 4, 16):
        rec.string = "RT Enhance Numerals" + (" Bold" if rec.nameID == 4 else "")
    elif rec.nameID == 6:
        rec.string = "RTEnhanceNumerals-Bold"
    elif rec.nameID == 3:
        rec.string = "RT Enhance Numerals Bold; derived from Playfair Display (OFL 1.1)"
thicken_signs(f)
f.save(out)
print(out, len(f.getGlyphOrder()), "glyphs")
