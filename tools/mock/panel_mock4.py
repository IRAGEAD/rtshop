import sys, math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops
K = 4
W, H = 196, 262
def S(v): return int(round(v * K))
def F(sz, bold=False): return ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-%s.ttc' % ('Bold' if bold else 'Regular'), int(sz * K), index=1)
def FS(sz): return ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc', int(sz * K), index=1)
rng = np.random.default_rng(7)
def noise(w, h, scales=((48, .5), (16, .3), (4, .2)), seed=0):
    r = np.random.default_rng(seed); out = np.zeros((h, w), np.float32)
    for s, a in scales:
        small = r.random((h // s + 2, w // s + 2)).astype(np.float32)
        im = Image.fromarray((small * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(im, np.float32) / 255 * a
    return out
def stone(w, h, base, seed=0, contrast=38, speck=True):
    n = noise(w, h, seed=seed); n = (n - n.mean())
    arr = np.zeros((h, w, 3), np.float32)
    for i in range(3): arr[..., i] = base[i] + n * contrast * 2
    if speck:
        r = np.random.default_rng(seed + 1); sp = r.random((h, w)); arr[sp > 0.985] -= 22; arr[sp < 0.012] += 16
        g = r.normal(0, 3.2, (h, w, 1)); arr += g
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
def steel(w, h, base, seed=0, vertical=False):
    r = np.random.default_rng(seed)
    line = r.normal(0, 7, (h if not vertical else w,)).astype(np.float32)
    line = np.convolve(line, np.ones(3) / 3, 'same')
    arr = np.zeros((h, w, 3), np.float32)
    grad = np.linspace(14, -14, h if not vertical else w)
    for i in range(3):
        if vertical: arr[..., i] = base[i] + line[None, :] + grad[None, :]
        else: arr[..., i] = base[i] + line[:, None] + grad[:, None]
    arr += r.normal(0, 2, (h, w, 1))
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
def cracks(im, n=5, seed=3, col=(0, 0, 0), alpha=70):
    d = ImageDraw.Draw(im, 'RGBA'); r = np.random.default_rng(seed)
    for _ in range(n):
        x, y = r.random() * im.width, r.random() * im.height; a = r.random() * 6.28
        for _ in range(int(20 + r.random() * 40)):
            a += r.normal(0, .5); nx, ny = x + math.cos(a) * 5, y + math.sin(a) * 5
            d.line([(x, y), (nx, ny)], fill=col + (alpha,), width=1); d.line([(x + 1, y + 1), (nx + 1, ny + 1)], fill=(255, 255, 255, alpha // 4), width=1); x, y = nx, ny
class P:
    def __init__(s, im): s.im = im.convert('RGB'); s.d = ImageDraw.Draw(s.im, 'RGBA'); s.gl = Image.new('RGB', s.im.size, (0, 0, 0)); s.dg = ImageDraw.Draw(s.gl)
    def paste_tex(s, tex, box, r=0):
        x0, y0, x1, y1 = [S(v) for v in box]; t = tex.crop((0, 0, x1 - x0, y1 - y0)) if tex.width >= x1 - x0 and tex.height >= y1 - y0 else tex.resize((x1 - x0, y1 - y0))
        m = Image.new('L', (x1 - x0, y1 - y0), 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, x1 - x0 - 1, y1 - y0 - 1], radius=S(r), fill=255); s.im.paste(t, (x0, y0), m); s.d = ImageDraw.Draw(s.im, 'RGBA')
    def bevel(s, box, r=0, up=True, w=1.0, a=110):
        """돋을새김(up) / 오목새김 테두리"""
        x0, y0, x1, y1 = [S(v) for v in box]; ww = S(w); hi = (255, 255, 255, a); lo = (0, 0, 0, int(a * 1.5))
        c1, c2 = (hi, lo) if up else (lo, hi)
        for i in range(ww):
            s.d.line([(x0 + i, y1 - 1 - i), (x0 + i, y0 + i), (x1 - 1 - i, y0 + i)], fill=c1); s.d.line([(x0 + i, y1 - 1 - i), (x1 - 1 - i, y1 - 1 - i), (x1 - 1 - i, y0 + i)], fill=c2)
    def inset(s, box, shade=70, r=1.5):
        x0, y0, x1, y1 = [S(v) for v in box]; s.d.rounded_rectangle([x0, y0, x1, y1], radius=S(r), fill=(0, 0, 0, shade)); s.bevel(box, up=False, w=0.8, a=90)
    def text(s, x, y, t, f, col, anchor='la', engrave=False, glow=False, shadow=True):
        X, Y = S(x), S(y)
        if engrave:
            s.d.text((X + S(.35), Y + S(.35)), t, font=f, fill=(255, 255, 255, 90), anchor=anchor); s.d.text((X - S(.3), Y - S(.3)), t, font=f, fill=(0, 0, 0, 150), anchor=anchor)
        elif shadow: s.d.text((X + S(.4), Y + S(.5)), t, font=f, fill=(0, 0, 0, 170), anchor=anchor)
        s.d.text((X, Y), t, font=f, fill=col, anchor=anchor)
        if glow: s.dg.text((X, Y), t, font=f, fill=col, anchor=anchor)
    def rivet(s, x, y, r=1.7):
        X, Y, R = S(x), S(y), S(r)
        s.d.ellipse([X - R + 2, Y - R + 3, X + R + 2, Y + R + 3], fill=(0, 0, 0, 120)); s.d.ellipse([X - R, Y - R, X + R, Y + R], fill=(118, 120, 128, 255), outline=(40, 42, 48, 255))
        s.d.ellipse([X - R * .55, Y - R * .65, X + R * .1, Y - R * .05], fill=(214, 218, 226, 230))
    def el(s, cx, cy, r, fill, glow=False, outline=None, w=1):
        b = [S(cx - r), S(cy - r), S(cx + r), S(cy + r)]; s.d.ellipse(b, fill=fill, outline=outline, width=w)
        if glow: s.dg.ellipse(b, fill=fill[:3])
    def rect(s, box, fill, glow=False, r=0):
        b = [S(v) for v in box]; s.d.rounded_rectangle(b, radius=S(r), fill=fill)
        if glow: s.dg.rounded_rectangle(b, radius=S(r), fill=fill[:3])
    def sprite(s, rows, pal, x, y, px):
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch != '.': s.d.rectangle([S(x + i * px), S(y + j * px), S(x + (i + 1) * px) - 1, S(y + (j + 1) * px) - 1], fill=pal[ch])
    def done(s, a=.65, b=.45):
        im = ImageChops.add(s.im, s.gl.filter(ImageFilter.GaussianBlur(S(1.4))).point(lambda q: int(q * a)))
        return ImageChops.add(im, s.gl.filter(ImageFilter.GaussianBlur(S(5))).point(lambda q: int(q * b)))
SWORD = ["..............WW", ".............WSW", "............WSSW", "...........WSSW.", "..........WSSW..", ".........WSSW...", "........WSSW....", ".......WSSW.....", "..GG..WSSW......", "..GYGWSSW.......", "...GYGSW........", "....GYG.........", "...BGYGG........", "..BB..GG........", "GBB.............", "GG.............."]
SWP = {'W': (232, 238, 248, 255), 'S': (150, 160, 182, 255), 'G': (120, 86, 30, 255), 'Y': (226, 184, 80, 255), 'B': (104, 68, 40, 255)}
GEM = ["...WWWW...", "..WLLLLD..", ".WLLLLLDD.", "WLLLHLLDDD", "WLLHLLDDDD", ".LLLLDDDD.", "..LLDDDD..", "...LDDD...", "....DD...."]
def gempal(c): return {'W': tuple(min(255, int(v * 1.5 + 40)) for v in c) + (255,), 'L': c + (255,), 'H': (255, 255, 255, 255), 'D': tuple(int(v * .6) for v in c) + (255,)}
SALT = ["..........", "....WW....", "...WWLW...", "..WLWWLW..", ".WWWLWWWL.", "WLWWWWLWWW", "DDLDDLDDLD"]
SALTP = {'W': (236, 232, 236, 255), 'L': (200, 192, 204, 255), 'D': (150, 142, 158, 255)}
COIN = ["..YYYY..", ".YLLLLD.", "YLLYYLDD", "YLYLLDLD", "YLYLLDLD", "YLLYYLDD", ".DLLLLD.", "..DDDD.."]
COINP = {'Y': (250, 220, 120, 255), 'L': (226, 176, 64, 255), 'D': (150, 104, 30, 255)}
SHIELD = ["DDDDDDDD", "DLLLLLLD", "DLLLLLLD", "DLLLLLLD", "DLLLLLLD", ".DLLLLD.", "..DLLD..", "...DD..."]
ODDS = [('성공', 90, (112, 204, 120)), ('유지', 10, (168, 172, 184)), ('손상', 0, (232, 160, 64)), ('파괴', 0, (224, 86, 80))]
LV, MAXLV = 0, 11

def mats_row(p, y, slot_tex=None, txt=(214, 216, 222), bad=(240, 120, 104), lab=(150, 152, 160)):
    items = [('gem', '0/1', '보석'), ('salt', '0/2', '소금'), ('coin', '300', 'Gold')]
    for i, (k, cnt, n) in enumerate(items):
        bx = 13 + i * 34
        p.inset((bx, y, bx + 29, y + 29), shade=110)
        if k == 'gem': p.sprite(GEM, gempal((84, 160, 226)), bx + 4.5, y + 4, 2)
        elif k == 'salt': p.sprite(SALT, SALTP, bx + 4.5, y + 7, 2)
        else: p.sprite(COIN, COINP, bx + 6.5, y + 5, 2)
        p.text(bx + 27.5, y + 19.5, cnt, F(6.2, True), bad, 'ra'); p.text(bx + 14.5, y + 31.5, n, F(5.6), lab, 'ma', shadow=False)
    bx = 123; p.inset((bx, y, W - 13, y + 29), shade=110)
    p.sprite(SHIELD, {'D': (70, 74, 84, 255), 'L': (40, 42, 50, 255)}, bx + 5, y + 6.5, 2)
    p.text(bx + 26, y + 5, '보호권', F(6.4, True), txt); p.text(bx + 26, y + 16, '비어 있음', F(5.6), lab)
    p.text(bx + 30, y + 31.5, '보호권 칸', F(5.6), lab, 'ma', shadow=False)

def v_slab():    # 라 · 다듬은 석판
    p = P(stone(S(W), S(H), (86, 88, 94), seed=1)); cracks(p.im, 4, seed=5); p.d = ImageDraw.Draw(p.im, 'RGBA')
    p.bevel((0, 0, W, H), w=1.6, a=120); p.bevel((4, 4, W - 4, H - 4), up=False, w=0.6, a=70)
    for (x, y, sx, sy) in ((0, 0, 1, 1), (W, 0, -1, 1), (0, H, 1, -1), (W, H, -1, -1)):    # 강철 모서리쇠
        t = steel(S(22), S(7), (128, 132, 142), seed=x + y)
        bx0, bx1 = (x, x + 22) if sx > 0 else (x - 22, x); by0, by1 = (y, y + 7) if sy > 0 else (y - 7, y); p.paste_tex(t, (bx0, by0, bx1, by1)); p.bevel((bx0, by0, bx1, by1), w=.7)
        t2 = steel(S(7), S(22), (128, 132, 142), seed=x + y + 1, vertical=True)
        cx0, cx1 = (x, x + 7) if sx > 0 else (x - 7, x); cy0, cy1 = (y, y + 22) if sy > 0 else (y - 22, y); p.paste_tex(t2, (cx0, cy0, cx1, cy1)); p.bevel((cx0, cy0, cx1, cy1), w=.7)
        p.rivet(x + sx * 3.5, y + sy * 3.5); p.rivet(x + sx * 17, y + sy * 3.5, 1.2); p.rivet(x + sx * 3.5, y + sy * 17, 1.2)
    p.text(W / 2, 14, '강  화', FS(9.5), (58, 60, 66), 'mm', engrave=True)
    cx, cy = W / 2, 56
    p.el(cx, cy, 27, (0, 0, 0, 90)); p.el(cx, cy, 25, (40, 42, 48, 255), outline=(20, 20, 24, 255), w=S(.8)); p.d.arc([S(cx - 27), S(cy - 27), S(cx + 27), S(cy + 27)], 20, 200, fill=(255, 255, 255, 70), width=S(.8))
    p.sprite(SWORD, SWP, cx - 18, cy - 18, 2.25)
    p.text(cx, cy + 35, 'HF-BLADE', F(8.5, True), (236, 238, 242), 'mm')
    y = 106; x0 = 18; st = (W - 36) / (MAXLV - 1)      # 단계: 돌에 박은 강철 못
    p.inset((11, y - 8, W - 11, y + 8), shade=80)
    for i in range(MAXLV):
        x = x0 + i * st
        if i < LV: p.el(x, y, 3.3, (255, 170, 70, 255), glow=True)
        elif i == LV: p.el(x, y, 4.4, (255, 150, 50, 255), glow=True); p.el(x, y, 2.2, (255, 232, 170, 255), glow=True)
        else: p.rivet(x, y, 2.6)
    p.text(13, y + 11, '+0', F(7, True), (236, 238, 242)); p.text(W / 2, y + 11, '→', F(7), (150, 152, 160), 'ma'); p.text(W / 2 + 12, y + 10.3, '+1', F(8, True), (255, 180, 90), glow=True); p.text(W - 13, y + 11, '최대 +11', F(6), (150, 152, 160), 'ra')
    y = 134; p.inset((11, y - 2, W - 11, y + 24), shade=60)
    p.text(16, y + 1, '공격 보너스', F(6.6), (196, 198, 206)); p.text(W - 16, y + .5, '+0%  →  +3%', F(7.2, True), (150, 226, 156), 'ra')
    p.text(16, y + 12, '공격력', F(6.6), (196, 198, 206)); p.text(W - 16, y + 11.5, '14  →  14.4', F(7.2, True), (150, 226, 156), 'ra')
    y = 166; p.text(13, y + 2, '성공', F(6.8), (196, 198, 206)); p.text(36, y - 5.5, '90%', FS(14), (140, 232, 150), glow=True)
    for i, (n, pc, col) in enumerate(ODDS[1:]):
        xx = 92 + i * 32; p.el(xx + 2, y + 5.5, 2.2, col + (255,) if pc else (70, 72, 80, 255)); p.text(xx + 6.5, y + 1, n, F(6), (150, 152, 160)); p.text(xx + 6.5, y + 9, '%d%%' % pc, F(7, True), (236, 238, 242) if pc else (120, 122, 130))
    p.inset((12, y + 21, W - 12, y + 29), shade=150); xx = 13.2
    for n, pc, col in ODDS:
        if pc <= 0: continue
        ww = (W - 26.4) * pc / 100; p.rect((xx, y + 22.2, xx + ww, y + 27.8), col + (255,), glow=(n == '성공')); p.d.line([(S(xx), S(y + 22.6)), (S(xx + ww), S(y + 22.6))], fill=(255, 255, 255, 110), width=S(.5)); xx += ww
    mats_row(p, 203)
    return p.done()

def v_plate():   # 마 · 단조 강철판
    p = P(steel(S(W), S(H), (58, 60, 68), seed=3))
    hm = noise(S(W), S(H), scales=((10, .6), (5, .4)), seed=9); arr = np.asarray(p.im, np.float32) + ((hm - hm.mean()) * 46)[..., None]; p.im = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)); p.d = ImageDraw.Draw(p.im, 'RGBA')   # 망치 자국
    p.bevel((0, 0, W, H), w=1.8, a=110); p.bevel((5, 5, W - 5, H - 5), up=False, w=.6, a=80)
    for x in (9, W / 2, W - 9):
        for y in (9, H - 9): p.rivet(x, y)
    for y in (H * .35, H * .65): p.rivet(9, y, 1.3); p.rivet(W - 9, y, 1.3)
    st = stone(S(W), S(H), (44, 46, 52), seed=4, contrast=22)
    def stone_in(box): p.paste_tex(st, box, r=1.5); p.bevel(box, up=False, w=.9, a=120)
    p.text(W / 2, 16, '강  화', FS(9.5), (30, 32, 38), 'mm', engrave=True)
    stone_in((16, 26, W - 16, 96)); cx, cy = W / 2, 56
    p.el(cx, cy + 2, 21, (255, 120, 40, 26), glow=True); p.sprite(SWORD, SWP, cx - 18, cy - 18, 2.25)
    p.text(cx, cy + 32, 'HF-BLADE', F(8.5, True), (236, 238, 242), 'mm')
    y = 108; x0 = 22; stp = (W - 44) / (MAXLV - 1)     # 단계: 눈금자처럼 새긴 금
    p.d.line([(S(x0), S(y)), (S(W - 22), S(y))], fill=(0, 0, 0, 170), width=S(1.6)); p.d.line([(S(x0), S(y + 1)), (S(W - 22), S(y + 1))], fill=(255, 255, 255, 50), width=S(.5))
    for i in range(MAXLV):
        x = x0 + i * stp; p.d.line([(S(x), S(y - 4)), (S(x), S(y + 4))], fill=(0, 0, 0, 170), width=S(1)); p.d.line([(S(x + .6), S(y - 4)), (S(x + .6), S(y + 4))], fill=(255, 255, 255, 50), width=S(.4))
        if i < LV: p.rect((x - 1.4, y - 4, x + 1.4, y + 4), (255, 170, 70, 255), glow=True)
        elif i == LV: p.rect((x - 1.8, y - 5, x + 1.8, y + 5), (255, 200, 110, 255), glow=True, r=.5)
    p.text(W / 2, y + 8, '+0   →   +1', F(8, True), (255, 196, 120), 'ma', glow=True)
    y = 134; stone_in((12, y - 3, W - 12, y + 24))
    p.text(18, y + 1, '공격 보너스', F(6.6), (190, 192, 200)); p.text(W - 18, y + .5, '+0%  →  +3%', F(7.2, True), (255, 204, 130), 'ra')
    p.text(18, y + 12, '공격력', F(6.6), (190, 192, 200)); p.text(W - 18, y + 11.5, '14  →  14.4', F(7.2, True), (255, 204, 130), 'ra')
    y = 166; stone_in((12, y - 3, W - 12, y + 31))
    p.text(18, y + 3, '성공', F(6.8), (190, 192, 200)); p.text(40, y - 4.5, '90%', FS(14), (140, 232, 150), glow=True)
    for i, (n, pc, col) in enumerate(ODDS[1:]):
        xx = 96 + i * 30; p.text(xx, y, n, F(6), (150, 152, 160)); p.text(xx, y + 8, '%d%%' % pc, F(7, True), col + (255,) if pc else (110, 112, 120))
    p.rect((18, y + 22, W - 18, y + 27), (0, 0, 0, 190), r=1); xx = 18.6
    for n, pc, col in ODDS:
        if pc <= 0: continue
        ww = (W - 37.2) * pc / 100; p.rect((xx, y + 22.6, xx + ww, y + 26.4), col + (255,), glow=(n == '성공')); xx += ww
    mats_row(p, 205, lab=(170, 172, 180))
    return p.done(.7, .5)

def v_slate():   # 바 · 점판암과 쇠 띠
    p = P(stone(S(W), S(H), (40, 44, 54), seed=11, contrast=26)); cracks(p.im, 3, seed=2, alpha=90); p.d = ImageDraw.Draw(p.im, 'RGBA')
    p.bevel((0, 0, W, H), w=1.4, a=90)
    def band(y, h=9):
        p.paste_tex(steel(S(W), S(h), (120, 124, 134), seed=int(y)), (0, y, W, y + h)); p.bevel((0, y, W, y + h), w=.8, a=130); p.d = ImageDraw.Draw(p.im, 'RGBA')
        for x in (6, W - 6): p.rivet(x, y + h / 2, 1.6)
    band(0, 24); p.text(W / 2, 12, '강  화', FS(9.5), (36, 38, 44), 'mm', engrave=True); p.rivet(26, 12, 1.3); p.rivet(W - 26, 12, 1.3)
    band(H - 9)
    cx, cy = 44, 62      # 왼쪽: 장비, 오른쪽: 단계와 이름
    p.inset((16, 34, 72, 90), shade=120, r=2); p.sprite(SWORD, SWP, cx - 20, cy - 20, 2.5)
    p.text(80, 36, 'HF-BLADE', F(9, True), (236, 238, 242))
    p.text(80, 50, '+0', F(8, True), (190, 194, 204)); p.text(98, 50, '→', F(8), (130, 134, 144)); p.text(112, 46.5, '+1', FS(12), (255, 180, 90), glow=True)
    for i in range(MAXLV):   # 단계: 작은 쇠 마름모 11개
        x = 83 + i * 9.4; y = 74; pts = [(S(x), S(y - 4.2)), (S(x + 3.4), S(y)), (S(x), S(y + 4.2)), (S(x - 3.4), S(y))]
        if i < LV: p.d.polygon(pts, fill=(255, 170, 70, 255)); p.dg.polygon(pts, fill=(255, 170, 70))
        elif i == LV: p.d.polygon(pts, fill=(255, 210, 130, 255), outline=(255, 150, 50, 255)); p.dg.polygon(pts, fill=(255, 170, 70))
        else: p.d.polygon(pts, fill=(22, 24, 30, 255), outline=(110, 114, 126, 255))
    p.text(80, 82, '강화 단계  0 / 11', F(5.8), (140, 144, 156))
    y = 102; p.d.line([(S(12), S(y - 4)), (S(W - 12), S(y - 4))], fill=(0, 0, 0, 150), width=S(.8)); p.d.line([(S(12), S(y - 3.3)), (S(W - 12), S(y - 3.3))], fill=(255, 255, 255, 36), width=S(.4))
    p.text(14, y, '공격 보너스', F(6.6), (190, 194, 204)); p.text(W - 14, y - .5, '+0%  →  +3%', F(7.2, True), (150, 226, 156), 'ra')
    p.text(14, y + 11.5, '공격력', F(6.6), (190, 194, 204)); p.text(W - 14, y + 11, '14  →  14.4', F(7.2, True), (150, 226, 156), 'ra')
    y = 134      # 확률: 네 개의 돌 칸
    for i, (n, pc, col) in enumerate(ODDS):
        bx = 12 + i * 43.5; big = i == 0
        p.inset((bx, y, bx + 41, y + 44), shade=130 if big else 90, r=2)
        p.rect((bx + 1.2, y + 40.2, bx + 39.8, y + 42.8), col + ((255,) if pc else (70,)), glow=bool(pc and big))
        p.text(bx + 20.5, y + 16, '%d%%' % pc, FS(12) if big else F(10, True), (col if pc else (104, 108, 120)) + (255,), 'mm', glow=big); p.text(bx + 20.5, y + 31, n, F(6.4), (190, 194, 204), 'mm')
    y = 188; p.text(14, y, '재료와 보호권', F(6.4, True), (140, 144, 156))
    mats_row(p, 200, lab=(150, 154, 166))
    return p.done()

if __name__ == '__main__':
    T = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc', 30, index=1); t2 = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc', 18, index=1)
    ps = [('라 · 다듬은 석판', '회색 돌판 + 강철 모서리쇠. 글자는 돌에 새긴 듯,\n단계는 돌에 박은 쇠못 11개 (오른 만큼 달궈짐)', v_slab()),
          ('마 · 단조 강철판', '망치 자국이 남은 강철판에 어두운 돌 칸.\n단계는 눈금자처럼 새긴 금 11개', v_plate()),
          ('바 · 점판암과 쇠 띠', '짙은 점판암에 쇠 띠 두 줄. 장비는 왼쪽 칸,\n확률은 돌 칸 네 개로 크게', v_slate())]
    pw, ph = S(W), S(H); sc = 0.72; pw2, ph2 = int(pw * sc), int(ph * sc)
    out = Image.new('RGB', (pw2 * 3 + 160, ph2 + 150), (22, 23, 29)); d = ImageDraw.Draw(out)
    for i, (t, sub, im) in enumerate(ps):
        x = 40 + i * (pw2 + 40); out.paste(im.resize((pw2, ph2), Image.LANCZOS), (x, 30))
        d.text((x, ph2 + 44), t, font=T, fill=(232, 226, 214)); d.multiline_text((x, ph2 + 90), sub, font=t2, fill=(196, 200, 210), spacing=6)
    out.save(sys.argv[1])
