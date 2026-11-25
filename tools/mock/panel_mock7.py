import sys, math
import numpy as np
from lux import *
from panel_mock4 import stone, steel
STEEL = [(0, (252, 253, 255)), (.3, (208, 214, 224)), (.52, (138, 146, 160)), (.7, (216, 222, 232)), (1, (116, 124, 138))]
BRASS = [(0, (255, 238, 184)), (.3, (228, 186, 98)), (.52, (160, 116, 44)), (.7, (224, 180, 94)), (1, (128, 88, 28))]
INK = (26, 30, 40, 255); INK2 = (84, 92, 108, 255); OKC = (20, 122, 66, 255); BAD = (190, 44, 44, 255); EMB = (255, 140, 50)
LV, MAXLV = 0, 11
ODDS = [('성공', 90, (30, 140, 76)), ('유지', 10, (96, 104, 120)), ('손상', 0, (206, 124, 26)), ('파괴', 0, (190, 46, 46))]
RUNES = [[(0, 0, 0, 4), (0, 0, 2, 1), (2, 1, 0, 2)], [(0, 0, 2, 0), (1, 0, 1, 4)], [(0, 0, 2, 2), (2, 2, 0, 4)], [(0, 4, 0, 0), (0, 0, 2, 4), (2, 4, 2, 0)], [(0, 0, 0, 4), (0, 2, 2, 0), (0, 2, 2, 4)],
         [(1, 0, 1, 4), (0, 1, 2, 3), (2, 1, 0, 3)], [(0, 0, 2, 0), (2, 0, 0, 4), (0, 4, 2, 4)], [(0, 0, 0, 4), (2, 0, 2, 4), (0, 2, 2, 2)], [(1, 0, 0, 2), (1, 0, 2, 2), (1, 0, 1, 4)], [(0, 0, 2, 2), (0, 4, 2, 2), (0, 0, 0, 4)], [(0, 1, 1, 0), (1, 0, 2, 1), (1, 0, 1, 4), (0, 4, 2, 4)]]
class Forge(Lux):
    def __init__(s, W, H):
        s.W, s.H = W, H
        s.im = stone(S(W), S(H), (96, 104, 120), seed=21, contrast=14, speck=True); s.d = ImageDraw.Draw(s.im, 'RGBA'); s.gl = Image.new('RGB', s.im.size, (0, 0, 0)); s.dg = ImageDraw.Draw(s.gl)
    def steelf(s, fn, **kw): m = s.mask(); fn(ImageDraw.Draw(m)); s.gold(m, stops=STEEL, **kw)
    def brass(s, fn, **kw): m = s.mask(); fn(ImageDraw.Draw(m)); s.gold(m, stops=BRASS, **kw)
    def oct(s, d, box, c, **kw):
        x0, y0, x1, y1 = box; d.polygon([(S(x0 + c), S(y0)), (S(x1 - c), S(y0)), (S(x1), S(y0 + c)), (S(x1), S(y1 - c)), (S(x1 - c), S(y1)), (S(x0 + c), S(y1)), (S(x0), S(y1 - c)), (S(x0), S(y0 + c))], **kw)
    def frame(s):
        W, H = s.W, s.H
        s.steelf(lambda d: (s.oct(d, (1.5, 1.5, W - 1.5, H - 1.5), 7, outline=255, width=S(3.2))))
        s.brass(lambda d: s.oct(d, (5.6, 5.6, W - 5.6, H - 5.6), 5.6, outline=255, width=max(1, S(.6))), shadow=False)
        for (x, y) in ((9, 3.1), (W - 9, 3.1), (9, H - 3.1), (W - 9, H - 3.1), (3.1, 9), (W - 3.1, 9), (3.1, H - 9), (W - 3.1, H - 9), (W / 2, 3.1), (W / 2, H - 3.1)):
            s.brass(lambda d, x=x, y=y: d.ellipse([S(x - 1.1), S(y - 1.1), S(x + 1.1), S(y + 1.1)], fill=255), shadow=False)
    def plate(s, box, c=2.5):
        """밝게 닦은 강철 판 (글자를 올리는 자리) + 놋쇠 가는 테"""
        x0, y0, x1, y1 = box; m = s.mask(); s.oct(ImageDraw.Draw(m), box, c, fill=255); s.shadow(m, off=(.4, .8), blur=1.0, a=120)
        t = steel(S(x1 - x0) + 2, S(y1 - y0) + 2, (214, 219, 228), seed=int(x0 * 7 + y0)); s.im.paste(t, (S(x0), S(y0)), m.crop((S(x0), S(y0), S(x0) + t.width, S(y0) + t.height))); s.d = ImageDraw.Draw(s.im, 'RGBA')
        s.d.line([(S(x0 + c), S(y0) + 1), (S(x1 - c), S(y0) + 1)], fill=(255, 255, 255, 190), width=1)
        s.brass(lambda d: s.oct(d, box, c, outline=255, width=max(1, S(.55))), shadow=False)
    def runeband(s, x0, x1, y, h=13, lv=LV):
        m = s.mask(); s.oct(ImageDraw.Draw(m), (x0, y, x1, y + h), 2.5, fill=255)
        s.im.paste(stone(S(x1 - x0) + 2, S(h) + 2, (50, 54, 64), seed=4, contrast=10), (S(x0), S(y)), m.crop((S(x0), S(y), S(x1) + 2, S(y + h) + 2))); s.d = ImageDraw.Draw(s.im, 'RGBA')
        s.brass(lambda d: s.oct(d, (x0, y, x1, y + h), 2.5, outline=255, width=max(1, S(.55))), shadow=False)
        st = (x1 - x0 - 16) / (MAXLV - 1)
        for i in range(MAXLV):
            cx = x0 + 8 + i * st; u = 1.9; ox, oy = cx - u, y + h / 2 - 2 * u
            for (a, b, c, e) in RUNES[i]:
                p0, p1 = (S(ox + a * u), S(oy + b * u)), (S(ox + c * u), S(oy + e * u))
                if i < lv: s.d.line([p0, p1], fill=(255, 176, 90, 255), width=S(.9)); s.dg.line([p0, p1], fill=EMB, width=S(.9))
                elif i == lv: s.d.line([p0, p1], fill=(255, 226, 160, 255), width=S(1.1)); s.dg.line([p0, p1], fill=(255, 170, 70), width=S(1.6))
                else: s.d.line([(p0[0] + 2, p0[1] + 2), (p1[0] + 2, p1[1] + 2)], fill=(255, 255, 255, 36), width=S(.8)); s.d.line([p0, p1], fill=(20, 22, 28, 255), width=S(.8))
    def done(s):
        im = ImageChops.add(s.im, s.gl.filter(ImageFilter.GaussianBlur(S(1.4))).point(lambda q: int(q * .7)))
        return ImageChops.add(im, s.gl.filter(ImageFilter.GaussianBlur(S(5))).point(lambda q: int(q * .5)))
    def medal(s, cx, cy, R=27):
        s.steelf(lambda d: s.oct(d, (cx - R, cy - R, cx + R, cy + R), R * .58, fill=255))
        s.brass(lambda d: s.oct(d, (cx - R + 2.6, cy - R + 2.6, cx + R - 2.6, cy + R - 2.6), (R - 2.6) * .58, outline=255, width=S(.9)), shadow=False)
        m = s.mask(); s.oct(ImageDraw.Draw(m), (cx - R + 4, cy - R + 4, cx + R - 4, cy + R - 4), (R - 4) * .58, fill=255)
        s.im.paste(stone(S(2 * R), S(2 * R), (44, 48, 58), seed=8, contrast=10), (S(cx - R + 4), S(cy - R + 4)), m.crop((S(cx - R + 4), S(cy - R + 4), S(cx - R + 4) + S(2 * R), S(cy - R + 4) + S(2 * R)))); s.d = ImageDraw.Draw(s.im, 'RGBA')
        s.dg.ellipse([S(cx - 13), S(cy - 11), S(cx + 13), S(cy + 15)], fill=(120, 52, 14))
        s.sprite(SWORD, SWP, cx - 18, cy - 18, 2.25, shadow=False)
    def wtext(s, x, y, t, f, col=(240, 242, 248, 255), anchor='la'):
        s.d.text((S(x) + S(.35), S(y) + S(.55)), t, font=f, fill=(0, 0, 0, 180), anchor=anchor); s.d.text((S(x), S(y)), t, font=f, fill=col, anchor=anchor)
    def btext(s, x, y, t, f, anchor='la', glow=True):
        m = s.mask(); ImageDraw.Draw(m).text((S(x), S(y)), t, font=f, fill=255, anchor=anchor); s.shadow(m, off=(.3, .6), blur=.5, a=200); s.gold(m, stops=BRASS, shadow=False, bevel=False)
def mats(s, x, y, gap=31, size=27):
    for i, (k, cnt, n) in enumerate((('gem', '0/1', '보석'), ('salt', '0/2', '소금'), ('coin', '300', 'Gold'))):
        bx = x + i * gap; s.plate((bx, y, bx + size, y + size))
        if k == 'gem': s.sprite(GEM, gempal((60, 140, 220)), bx + 3.5, y + 3, 2)
        elif k == 'salt': s.sprite(SALT, {'W': (252, 250, 252, 255), 'L': (200, 194, 208, 255), 'D': (150, 142, 162, 255)}, bx + 3.5, y + 6.5, 2)
        else: s.sprite(COIN, COINP, bx + 5.5, y + 4.5, 2)
        s.text(bx + size - 1.8, y + size - 9.3, cnt, F(6.2, True), BAD, 'ra'); s.wtext(bx + size / 2, y + size + 1.5, n, F(5.6), (214, 220, 232, 255), 'ma')
def guard(s, bx, y, w, h=27):
    s.plate((bx, y, bx + w, y + h))
    s.brass(lambda d: d.polygon([(S(bx + 6), S(y + 5)), (S(bx + 19), S(y + 5)), (S(bx + 19), S(y + 14.5)), (S(bx + 12.5), S(y + 23)), (S(bx + 6), S(y + 14.5))], outline=255, width=S(1.1)), shadow=False)
    s.text(bx + 24, y + 4.5, '보호권', F(6.6, True), INK); s.text(bx + 24, y + 15.5, '비어 있음', F(5.8), INK2); s.wtext(bx + w / 2, y + h + 1.5, '보호권 칸', F(5.6), (214, 220, 232, 255), 'ma')
def stats(s, x0, x1, y):
    s.plate((x0, y, x1, y + 27)); s.text(x0 + 6, y + 3.5, '공격 보너스', F(6.8), INK2); s.text(x1 - 6, y + 3, '+0%  →  +3%', F(7.4, True), OKC, 'ra')
    s.text(x0 + 6, y + 15, '공격력', F(6.8), INK2); s.text(x1 - 6, y + 14.5, '14  →  14.4', F(7.4, True), OKC, 'ra')
def odds(s, x0, y, wbig=58, wsm=28.5, h=40):
    s.plate((x0, y, x0 + wbig, y + h)); s.text(x0 + wbig / 2, y + 16, '90%', FS(15), ODDS[0][2] + (255,), 'mm'); s.text(x0 + wbig / 2, y + 32, '성 공', F(6.2), INK2, 'mm')
    for i, (n, pc, col) in enumerate(ODDS[1:]):
        bx = x0 + wbig + 4 + i * (wsm + 3); s.plate((bx, y, bx + wsm, y + h)); s.d.ellipse([S(bx + wsm / 2 - 2.2), S(y + 5.5), S(bx + wsm / 2 + 2.2), S(y + 9.9)], fill=(col + (255,)) if pc else (176, 182, 194, 255))
        s.text(bx + wsm / 2, y + 20.5, '%d%%' % pc, F(8.2, True), (col + (255,)) if pc else (150, 156, 170, 255), 'mm'); s.text(bx + wsm / 2, y + 31.5, n, F(6), INK2, 'mm')

def q_wide():      # 파 · 가로 두 단 + 룬 띠
    W, H = 300, 204; s = Forge(W, H); s.frame()
    s.btext(64, 20, '강  화', FS(10.5), 'mm'); s.medal(64, 62); s.wtext(64, 94, 'HF-BLADE', F(8.6, True), anchor='ma')
    s.wtext(40, 110, '+0', F(8.5, True), (200, 206, 220, 255), 'ma'); s.brass(lambda d: d.polygon([(S(58), S(113)), (S(66), S(116.5)), (S(58), S(120))], fill=255), shadow=False); s.btext(82, 108, '+1', FS(13), 'ma')
    x0, x1 = 130, W - 14
    stats(s, x0, x1, 14); odds(s, x0, 47, wbig=58, wsm=29.3, h=40); mats(s, x0, 95, gap=31, size=27); guard(s, x0 + 96, 95, x1 - x0 - 96)
    s.runeband(14, W - 14, 146, h=15); s.wtext(W / 2, 166, '룬 열한 자가 강화 단계 — 오른 만큼 불이 들어온다  ·  지금 0 / 11', F(5.8), (214, 220, 232, 255), 'ma')
    s.brass(lambda d: d.line([(S(121), S(16)), (S(121), S(136))], fill=255, width=max(1, S(.5))), shadow=False)
    return s.done(), W, H
def q_tall():      # 하 · 세로 + 룬 띠
    W, H = 204, 280; s = Forge(W, H); s.frame()
    s.btext(W / 2, 19, '강  화', FS(10.5), 'mm'); s.medal(W / 2, 62); s.wtext(W / 2, 94, 'HF-BLADE', F(8.6, True), anchor='ma')
    s.runeband(14, W - 14, 111, h=15)
    cx = W / 2; s.wtext(cx - 17, 131, '+0', F(8.5, True), (200, 206, 220, 255), 'ma'); s.brass(lambda d: d.polygon([(S(cx - 4), S(134)), (S(cx + 4), S(137.5)), (S(cx - 4), S(141))], fill=255), shadow=False); s.btext(cx + 18, 129, '+1', FS(13), 'ma')
    stats(s, 14, W - 14, 152); odds(s, 14, 185, wbig=58, wsm=37, h=40); mats(s, 14, 232, gap=31, size=27); guard(s, 110, 232, W - 124)
    return s.done(), W, H
def q_anvil():     # 거 · 모루 머리
    W, H = 232, 262; s = Forge(W, H); s.frame()
    # 창 머리를 모루 윗면 모양의 강철로
    def head(d): d.polygon([(S(20), S(16)), (S(W - 44), S(16)), (S(W - 16), S(26)), (S(W - 44), S(36)), (S(W - 60), S(36)), (S(W - 66), S(42)), (S(54), S(42)), (S(48), S(36)), (S(20), S(36))], fill=255)
    s.steelf(head); s.brass(lambda d: d.line([(S(24), S(32)), (S(W - 48), S(32))], fill=255, width=max(1, S(.6))), shadow=False)
    s.text(W / 2 - 12, 25.5, '강  화', FS(10), INK, 'mm')
    s.medal(48, 84, R=26); s.wtext(84, 60, 'HF-BLADE', F(9, True))
    s.wtext(84, 76, '+0', F(8.5, True), (200, 206, 220, 255)); s.brass(lambda d: d.polygon([(S(102), S(78.5)), (S(110), S(82)), (S(102), S(85.5))], fill=255), shadow=False); s.btext(116, 73.5, '+1', FS(13))
    s.wtext(84, 96, '강화 단계  0 / 11', F(5.8), (190, 198, 214, 255))
    s.runeband(14, W - 14, 118, h=15)
    stats(s, 14, W - 14, 140); odds(s, 14, 173, wbig=64, wsm=43.5, h=38); mats(s, 14, 217, gap=31, size=26); guard(s, 112, 217, W - 126, h=26)
    return s.done(), W, H
if __name__ == '__main__':
    T = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc', 30, index=1); t2 = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc', 18, index=1)
    ps = [('파 · 가로 두 단', '모루와 같은 재료: 점판암 바탕 + 닦은 강철 판 + 놋쇠 상감.\n단계는 받침대와 같은 룬 띠 (오른 만큼 불이 들어옴)', q_wide()),
          ('하 · 세로 한 장', '같은 재료의 세로 구성. 장비 아래에 룬 띠,\n그 아래 능력·확률·재료·보호권', q_tall()),
          ('거 · 모루 머리', '창 머리가 모루 윗면 모양의 강철판.\n장비는 왼쪽, 이름과 단계는 오른쪽', q_anvil())]
    sc = 0.62; sizes = [(int(S(w) * sc), int(S(h) * sc)) for _, _, (im, w, h) in ps]; hmax = max(h for _, h in sizes)
    out = Image.new('RGB', (sum(w for w, _ in sizes) + 40 * 4, hmax + 150), (22, 23, 29)); d = ImageDraw.Draw(out); x = 40
    for (t, sub, (im, w, h)), (pw, ph) in zip(ps, sizes):
        out.paste(im.resize((pw, ph), Image.LANCZOS), (x, 30 + (hmax - ph) // 2))
        d.text((x, hmax + 44), t, font=T, fill=(240, 220, 160)); d.multiline_text((x, hmax + 90), sub, font=t2, fill=(196, 200, 210), spacing=6); x += pw + 40
    out.save(sys.argv[1])
