import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops
from panel_mock4 import SWORD, SWP, GEM, gempal, SALT, SALTP, COIN, COINP
K = 4
def S(v): return int(round(v * K))
def F(sz, bold=False): return ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-%s.ttc' % ('Bold' if bold else 'Regular'), int(sz * K), index=1)
def FS(sz): return ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc', int(sz * K), index=1)
GOLD = [(0, (255, 246, 206)), (.3, (244, 208, 116)), (.52, (190, 138, 48)), (.7, (240, 200, 104)), (1, (158, 108, 34))]
def metal(w, h, stops):
    arr = np.zeros((h, w, 3), np.float32); ys = np.linspace(0, 1, h)
    for i in range(3): arr[..., i] = np.interp(ys, [s[0] for s in stops], [s[1][i] for s in stops])[:, None]
    return Image.fromarray(arr.astype(np.uint8))
class Lux:
    def __init__(s, W, H, base=(247, 241, 228), vein=(214, 200, 176), seed=2):
        s.W, s.H = W, H; w, h = S(W), S(H); r = np.random.default_rng(seed)
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        def nz(sc, sd):
            small = np.random.default_rng(sd).random((h // sc + 2, w // sc + 2)).astype(np.float32)
            return np.asarray(Image.fromarray((small * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), np.float32) / 255
        n = nz(90, seed) * .6 + nz(30, seed + 1) * .3 + nz(9, seed + 2) * .1
        veins = np.clip(1 - np.abs(n - .5) * 26, 0, 1) ** 2 * .55 + np.clip(1 - np.abs(nz(60, seed + 5) - .52) * 40, 0, 1) ** 2 * .3      # 대리석 결
        vg = 1 - .10 * (((xx - w / 2) / (w * .6)) ** 2 + ((yy - h / 2) / (h * .6)) ** 2)
        arr = np.zeros((h, w, 3), np.float32)
        for i in range(3): arr[..., i] = (base[i] + (n - .5) * 10) * vg * (1 - veins) + vein[i] * veins
        arr += r.normal(0, .8, (h, w, 1))
        s.im = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)); s.d = ImageDraw.Draw(s.im, 'RGBA')
    def mask(s): return Image.new('L', s.im.size, 0)
    def shadow(s, mask, off=(.5, .9), blur=1.2, a=110):
        sh = ImageChops.offset(mask, S(off[0]), S(off[1])).filter(ImageFilter.GaussianBlur(S(blur))).point(lambda q: int(q * a / 255))
        s.im = Image.composite(Image.new('RGB', s.im.size, (60, 40, 14)), s.im, sh); s.d = ImageDraw.Draw(s.im, 'RGBA')
    def gold(s, mask, stops=GOLD, shadow=True, bevel=True):
        bb = mask.getbbox()
        if not bb: return
        if shadow: s.shadow(mask)
        g = metal(bb[2] - bb[0], bb[3] - bb[1], stops); s.im.paste(g, (bb[0], bb[1]), mask.crop(bb))
        if bevel:      # 윗변은 밝게, 아랫변은 어둡게: 두께가 있어 보이게
            hi = ImageChops.subtract(mask, ImageChops.offset(mask, 0, max(1, S(.3)))); lo = ImageChops.subtract(mask, ImageChops.offset(mask, 0, -max(1, S(.3))))
            s.im = Image.composite(Image.new('RGB', s.im.size, (255, 252, 232)), s.im, hi.point(lambda q: int(q * .75))); s.im = Image.composite(Image.new('RGB', s.im.size, (104, 66, 16)), s.im, lo.point(lambda q: int(q * .6)))
        s.d = ImageDraw.Draw(s.im, 'RGBA')
    def gshape(s, fn, **kw):
        m = s.mask(); fn(ImageDraw.Draw(m)); s.gold(m, **kw)
    def scroll(s, d, cx, cy, r0, a0, turns, w0=1.6, w1=.35, cw=1):
        """끝으로 갈수록 가늘어지며 말려 들어가는 덩굴"""
        n = int(60 * abs(turns)) + 8
        for i in range(n):
            t = i / (n - 1); a = math.radians(a0) + cw * t * turns * 2 * math.pi; r = r0 * (1 - t) ** 1.25 + .3
            x, y = cx + math.cos(a) * r, cy + math.sin(a) * r; w = (w0 + (w1 - w0) * t) / 2
            d.ellipse([S(x - w), S(y - w), S(x + w), S(y + w)], fill=255)
        return cx + math.cos(math.radians(a0)) * r0, cy + math.sin(math.radians(a0)) * r0
    def leaf(s, d, x, y, ang, L=5, w=1.8):
        a = math.radians(ang); ux, uy = math.cos(a), math.sin(a); px, py = -uy, ux
        d.polygon([(S(x), S(y)), (S(x + ux * L * .45 + px * w), S(y + uy * L * .45 + py * w)), (S(x + ux * L), S(y + uy * L)), (S(x + ux * L * .45 - px * w * .6), S(y + uy * L * .45 - py * w * .6))], fill=255)
    def corner(s, d, x, y, sx, sy, k=1.0):
        fx = lambda v: x + sx * v * k; fy = lambda v: y + sy * v * k
        cwv = 1 if sx * sy > 0 else -1
        s.scroll(d, fx(8), fy(8), 6.5 * k, 225 if (sx, sy) == (1, 1) else (315 if (sx, sy) == (-1, 1) else (135 if (sx, sy) == (1, -1) else 45)), 1.15, 1.5 * k, .3, cwv)
        s.scroll(d, fx(19), fy(5.5), 4.2 * k, 180 if sx > 0 else 0, .9, 1.2 * k, .3, -cwv)
        s.scroll(d, fx(5.5), fy(19), 4.2 * k, 270 if sy > 0 else 90, .9, 1.2 * k, .3, cwv)
        base = {(1, 1): 0, (-1, 1): 180, (1, -1): 0, (-1, -1): 180}[(sx, sy)]
        s.leaf(d, fx(24), fy(3.2), 0 if sx > 0 else 180, 6 * k, 1.5 * k); s.leaf(d, fx(31), fy(3.2), 0 if sx > 0 else 180, 4 * k, 1.1 * k)
        s.leaf(d, fx(3.2), fy(24), 90 if sy > 0 else 270, 6 * k, 1.5 * k); s.leaf(d, fx(3.2), fy(31), 90 if sy > 0 else 270, 4 * k, 1.1 * k)
        s.leaf(d, fx(12), fy(12), {(1, 1): 45, (-1, 1): 135, (1, -1): -45, (-1, -1): 225}[(sx, sy)], 7 * k, 1.8 * k)
    def frame(s, m=2.5, r=6, corners=True):
        W, H = s.W, s.H
        def fn(d):
            d.rounded_rectangle([S(m), S(m), S(W - m), S(H - m)], radius=S(r), outline=255, width=S(2.0))
            d.rounded_rectangle([S(m + 3.4), S(m + 3.4), S(W - m - 3.4), S(H - m - 3.4)], radius=S(r - 2), outline=255, width=max(1, S(.5)))
            if corners:
                for (x, y, sx, sy) in ((m + 3.6, m + 3.6, 1, 1), (W - m - 3.6, m + 3.6, -1, 1), (m + 3.6, H - m - 3.6, 1, -1), (W - m - 3.6, H - m - 3.6, -1, -1)): s.corner(d, x, y, sx, sy)
        s.gshape(fn)
        for (x, y) in ((m + 1, m + 1), (W - m - 1, m + 1), (m + 1, H - m - 1), (W - m - 1, H - m - 1)): s.cab(x, y, 2.6, (200, 40, 56))
    def cab(s, x, y, r, col, lit=True, bezel=True):
        """둥근 보석(카보숑): 금 물림 + 광택"""
        if bezel: s.gshape(lambda d: d.ellipse([S(x - r - 1.1), S(y - r - 1.1), S(x + r + 1.1), S(y + r + 1.1)], fill=255))
        c = col if lit else tuple(int(v * .25 + 150) for v in col)
        R = S(r); size = R * 2 + 1; yy, xx = np.mgrid[0:size, 0:size].astype(np.float32); dx, dy = (xx - R) / R, (yy - R) / R; dist = np.hypot(dx, dy)
        shade = np.clip(1.15 - dist * .75 - (dy * .25), .35, 1.3); arr = np.zeros((size, size, 4), np.float32)
        for i in range(3): arr[..., i] = np.clip(c[i] * shade, 0, 255)
        spec = np.clip(1 - np.hypot(dx + .35, dy + .4) / .38, 0, 1) ** 1.5; arr[..., :3] += spec[..., None] * (255 if lit else 120)
        refl = np.clip(1 - np.hypot(dx - .25, dy - .45) / .5, 0, 1) * .35; arr[..., :3] += refl[..., None] * np.array(c) * (1 if lit else .3)
        arr[..., 3] = np.clip((1 - dist) * R, 0, 1) * 255
        g = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA'); s.im.paste(g, (S(x) - R, S(y) - R), g); s.d = ImageDraw.Draw(s.im, 'RGBA')
    def guilloche(s, cx, cy, R, r, n=36, col=(190, 150, 70, 90)):
        for k in range(n):
            a = 2 * math.pi * k / n; x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
            s.d.ellipse([S(x - R), S(y - R), S(x + R), S(y + R)], outline=col, width=1)
    def enamel(s, box, r=3, col=(255, 253, 246), a=255, inner=True):
        """오목하게 들어간 에나멜 칸 (안쪽 그림자) + 금 테"""
        b = [S(v) for v in box]; m = s.mask(); ImageDraw.Draw(m).rounded_rectangle(b, radius=S(r), fill=255)
        s.im.paste(Image.new('RGB', s.im.size, col), (0, 0), m.point(lambda q: int(q * a / 255)))
        if inner:
            edge = ImageChops.subtract(m, ImageChops.offset(m.filter(ImageFilter.GaussianBlur(S(1.2))), S(.5), S(1.1)))
            s.im = Image.composite(Image.new('RGB', s.im.size, (120, 96, 60)), s.im, edge.point(lambda q: int(min(255, q * .5))))
        s.d = ImageDraw.Draw(s.im, 'RGBA'); s.gshape(lambda d: d.rounded_rectangle(b, radius=S(r), outline=255, width=S(.8)), shadow=False)
    def gtext(s, x, y, t, f, anchor='la', stops=GOLD, shadow=True):
        m = s.mask(); ImageDraw.Draw(m).text((S(x), S(y)), t, font=f, fill=255, anchor=anchor)
        if shadow: s.shadow(m, off=(.3, .6), blur=.6, a=140)
        s.gold(m, stops=[(0, (214, 164, 60)), (.5, (150, 100, 24)), (1, (190, 140, 50))], shadow=False, bevel=False)
    def text(s, x, y, t, f, col, anchor='la'): s.d.text((S(x), S(y)), t, font=f, fill=col, anchor=anchor)
    def sprite(s, rows, pal, x, y, px, shadow=True):
        if shadow:
            for j, row in enumerate(rows):
                for i, ch in enumerate(row):
                    if ch != '.': s.d.rectangle([S(x + i * px + .7), S(y + j * px + 1.0), S(x + (i + 1) * px + .7) - 1, S(y + (j + 1) * px + 1.0) - 1], fill=(90, 66, 30, 60))
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch != '.': s.d.rectangle([S(x + i * px), S(y + j * px), S(x + (i + 1) * px) - 1, S(y + (j + 1) * px) - 1], fill=pal[ch])
    def divider(s, x0, x1, y):
        c = (x0 + x1) / 2
        def fn(d):
            d.line([(S(x0), S(y)), (S(c - 7), S(y))], fill=255, width=max(1, S(.5))); d.line([(S(c + 7), S(y)), (S(x1), S(y))], fill=255, width=max(1, S(.5)))
            s.scroll(d, c - 9, y, 2.2, 0, .8, .9, .25, 1); s.scroll(d, c + 9, y, 2.2, 180, .8, .9, .25, -1)
        s.gshape(fn, shadow=False); s.cab(c, y, 1.7, (200, 40, 56))
INK = (58, 40, 22, 255); INK2 = (128, 102, 70, 255); OKC = (30, 128, 70, 255); BAD = (190, 44, 44, 255)
