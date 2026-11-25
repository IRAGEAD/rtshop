#!/usr/bin/env python3
"""강화 정보 창의 숫자·영문 글꼴(enhance_num.ttf)을 만든다.

Playfair Display (SIL OFL 1.1) 의 굵기 700 을 뽑아 라틴 글자만 남기고, 숫자는 높이가 고른 '라이닝' 모양(zero.lf …)으로 바꾼다:
기본 모양(올드스타일)은 3·4·5·7·9 가 줄 아래로 내려가 칸 안의 숫자가 들쭉날쭉해 보이기 때문이다.
OFL 의 예약 이름을 피하려고 글꼴 이름은 "RT Enhance Numerals" 로 바꾼다.

쓰기: python3 tools/gen_enhance_font.py <PlayfairDisplay[wght].ttf> <enhance_num.ttf>
"""
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset

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
f.save(out)
print(out, len(f.getGlyphOrder()), "glyphs")
