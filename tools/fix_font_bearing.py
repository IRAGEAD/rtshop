# Minecraft 의 TTF 글꼴은 글자 왼쪽 여백(left side bearing)을 두 번 더하는 것처럼 그려서,
# 왼쪽 여백이 큰 글자(「 『 【 《 〈 · 등)가 오른쪽으로 밀려 다음 글자와 겹쳤다 (「조 → 초).
# 왼쪽 여백이 큰 글자는 모양을 왼쪽으로 옮기고 그만큼 폭을 줄인다. python3 tools/fix_font_bearing.py <ttf>...
import sys
from fontTools.ttLib import TTFont
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
LIMIT, KEEP = 120, 60
for path in sys.argv[1:]:
    f = TTFont(path)
    gs = f.getGlyphSet(); hm = f['hmtx']; glyf = f['glyf']
    n = 0
    for g in f.getGlyphOrder():
        adv, lsb = hm[g]
        bp = BoundsPen(gs); gs[g].draw(bp)
        if bp.bounds is None: continue
        xmin = bp.bounds[0]
        if xmin <= LIMIT: continue
        dx = KEEP - xmin
        pen = TTGlyphPen(gs)
        gs[g].draw(TransformPen(pen, (1, 0, 0, 1, dx, 0)))
        glyf[g] = pen.glyph()
        glyf[g].recalcBounds(glyf)
        hm[g] = (max(1, adv + dx), KEEP)
        n += 1
    f.save(path)
    print(path, "fixed", n)
