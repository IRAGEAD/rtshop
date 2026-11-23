# 중세풍 창 질감 (픽셀). 나무판·가죽·양피지·무쇠·놋쇠를 직접 그린다.
# python3 tools/gui_textures.py mock <출력.png>  : 시안 그림
# python3 tools/gui_textures.py atlas <폴더>      : 게임에 들어갈 텍스처
import sys
import math
import random
from PIL import Image, ImageDraw, ImageFont

R = random.Random(5)


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, f):
    return tuple(clamp(x * f) for x in c[:3]) + ((c[3],) if len(c) > 3 else (255,))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def noise2(w, h, scale, seed, wrap=False):
    """부드러운 값 잡음 (0..1). wrap=True 면 이어 붙여도 이음매가 없다 (w, h 가 scale 의 배수일 때)."""
    rr = random.Random(seed)
    gw, gh = w // scale + 2, h // scale + 2
    g = [[rr.random() for _ in range(gw)] for _ in range(gh)]
    if wrap:
        cw, ch = w // scale, h // scale
        for yy in range(gh):
            for xx in range(gw):
                g[yy][xx] = g[yy % ch][xx % cw]
    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            fx, fy = x / scale, y / scale
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            tx = tx * tx * (3 - 2 * tx)
            ty = ty * ty * (3 - 2 * ty)
            a = g[y0][x0] * (1 - tx) + g[y0][x0 + 1] * tx
            b = g[y0 + 1][x0] * (1 - tx) + g[y0 + 1][x0 + 1] * tx
            out[y][x] = a * (1 - ty) + b * ty
    return out


# ---------------------------------------------------------------- 재료

def planks(w, h, base=(122, 80, 44), vertical=False, board=11, seed=1):
    """결과 옹이가 있는 나무판 (가로로 이어진 판자)"""
    img = Image.new('RGBA', (w, h))
    px = img.load()
    rr = random.Random(seed)
    L, S = (h, w) if not vertical else (w, h)
    n = noise2(w, h, 3, seed)
    rows = []
    y = 0
    while y < L:
        bh = board + rr.randint(-1, 1)
        rows.append((y, bh, rr.uniform(0.86, 1.1), rr.randint(0, 1000), rr.randint(-40, S)))
        y += bh
    for (y0, bh, tone, off, knot) in rows:
        seam = rr.randint(10, S - 10) if S > 30 else -99
        for yy in range(y0, min(L, y0 + bh)):
            for xx in range(S):
                x, y = (xx, yy) if not vertical else (yy, xx)
                gr = math.sin((xx + off) * 0.09 + math.sin(xx * 0.031 + off) * 2.2 + (yy - y0) * 1.3) * 0.5 + 0.5
                f = tone * (0.84 + 0.2 * gr) * (0.92 + 0.16 * n[y][x])
                # 옹이
                dk = math.hypot((xx - knot) * 0.6, (yy - y0 - bh / 2) * 1.4)
                if dk < 3.5:
                    f *= 0.62 + 0.1 * dk
                elif dk < 6:
                    f *= 0.92
                c = shade(base, f)
                if yy == y0:
                    c = shade(base, 0.42)          # 판자 사이 틈
                elif yy == y0 + 1:
                    c = shade(base, tone * 1.18)   # 윗면 빛
                elif yy == y0 + bh - 1:
                    c = shade(base, tone * 0.72)
                if abs(xx - seam) < 1 and yy != y0:
                    c = shade(base, 0.48)          # 이음매
                px[x, y] = c
    return img


def leather(w, h, base=(110, 62, 34), seed=2, tooled=True, wrap=False):
    img = Image.new('RGBA', (w, h))
    px = img.load()
    n = noise2(w, h, 2, seed, wrap)
    n2 = noise2(w, h, 8 if wrap else 7, seed + 9, wrap)
    rr = random.Random(seed)
    for y in range(h):
        for x in range(w):
            f = 0.82 + 0.16 * n[y][x] + 0.14 * (n2[y][x] - 0.5)
            if rr.random() < 0.05:
                f *= 0.86   # 가죽 모공
            px[x, y] = shade(base, f)
    if wrap:
        return img
    # 가장자리가 닳아 어둡다
    for y in range(h):
        for x in range(w):
            d = min(x, y, w - 1 - x, h - 1 - y)
            if d < 4:
                c = px[x, y]
                px[x, y] = shade(c, 0.7 + 0.075 * d)
    return img


def parchment(w, h, base=(240, 224, 186), seed=3, burnt=True, wrap=False):
    img = Image.new('RGBA', (w, h))
    px = img.load()
    n = noise2(w, h, 4, seed, wrap)
    n2 = noise2(w, h, 16 if wrap else 13, seed + 4, wrap)
    rr = random.Random(seed)
    stains = [(rr.uniform(0, w), rr.uniform(0, h), rr.uniform(6, 16)) for _ in range(max(1, w * h // 6000))]
    for y in range(h):
        for x in range(w):
            f = 0.9 + 0.08 * n[y][x] + 0.1 * (n2[y][x] - 0.5)
            for sx, sy, sr in stains:
                d = math.hypot(x - sx, y - sy)
                if wrap:
                    d = math.hypot(min(abs(x - sx), w - abs(x - sx)), min(abs(y - sy), h - abs(y - sy)))
                if abs(d - sr) < 0.7:
                    f *= 0.955   # 얼룩 테
                elif d < sr:
                    f *= 0.975
            if rr.random() < 0.015:
                f *= 0.92
            c = shade(base, f)
            # 가장자리로 갈수록 누렇게 바랜다
            d = min(x, y, w - 1 - x, h - 1 - y) if not wrap else 99
            if d < 10:
                c = mix(c, (176, 132, 72), (1 - d / 10) ** 2 * 0.65)
            px[x, y] = c
    if burnt:
        # 들쭉날쭉 그을린 가장자리
        for x in range(w):
            for edge in (0, 1):
                depth = int(1 + 2 * noise2(w, 1, 5, seed + edge)[0][x] * 1.4)
                for k in range(depth):
                    y = k if edge == 0 else h - 1 - k
                    px[x, y] = (0, 0, 0, 0) if k < depth - 2 else (86, 52, 22, 255)
                y = depth if edge == 0 else h - 1 - depth
                if 0 <= y < h:
                    px[x, y] = (120, 78, 36, 255)
        for y in range(h):
            for edge in (0, 1):
                depth = int(1 + 2 * noise2(1, h, 5, seed + 7 + edge)[y][0] * 1.4)
                for k in range(depth):
                    x = k if edge == 0 else w - 1 - k
                    if px[x, y][3] != 0:
                        px[x, y] = (0, 0, 0, 0) if k < depth - 2 else (86, 52, 22, 255)
                x = depth if edge == 0 else w - 1 - depth
                if 0 <= x < w and px[x, y][3]:
                    px[x, y] = (120, 78, 36, 255)
    return img


def metal(w, h, base, seed=4):
    img = Image.new('RGBA', (w, h))
    px = img.load()
    n = noise2(w, h, 2, seed)
    for y in range(h):
        for x in range(w):
            f = 0.78 + 0.3 * (1 - y / max(1, h - 1)) + 0.1 * n[y][x]
            px[x, y] = shade(base, f)
    for x in range(w):
        px[x, 0] = shade(base, 1.35)
        px[x, h - 1] = shade(base, 0.45)
    for y in range(h):
        px[0, y] = shade(base, 1.15)
        px[w - 1, y] = shade(base, 0.5)
    return img


IRON = (120, 120, 126)
BRASS = (206, 160, 70)


def rivet(img, x, y, base=IRON, r=1):
    p = img.load()
    for dy in range(-r - 1, r + 2):
        for dx in range(-r - 1, r + 2):
            if 0 <= x + dx < img.width and 0 <= y + dy < img.height:
                d = math.hypot(dx, dy)
                if d <= r + 0.5:
                    f = 1.5 if (dx < 0 and dy < 0) else 1.0 if d < r else 0.6
                    p[x + dx, y + dy] = shade(base, f)
                elif d <= r + 1.2:
                    p[x + dx, y + dy] = shade(p[x + dx, y + dy], 0.6)


def stitch(img, x0, y0, x1, y1, col=(232, 206, 150), step=3):
    p = img.load()
    if y0 == y1:
        for x in range(x0, x1, step):
            for k in range(2):
                if x + k < x1:
                    p[x + k, y0] = col + (255,)
                    if y0 + 1 < img.height:
                        p[x + k, y0 + 1] = shade(p[x + k, y0 + 1], 0.7)
    else:
        for y in range(y0, y1, step):
            for k in range(2):
                if y + k < y1:
                    p[x0, y + k] = col + (255,)
                    if x0 + 1 < img.width:
                        p[x0 + 1, y + k] = shade(p[x0 + 1, y + k], 0.7)


def outline(img, x0, y0, x1, y1, col):
    d = ImageDraw.Draw(img)
    d.rectangle([x0, y0, x1, y1], outline=col)


def bevel(img, x0, y0, x1, y1, hi=1.3, lo=0.55):
    p = img.load()
    for x in range(x0, x1 + 1):
        p[x, y0] = shade(p[x, y0], hi)
        p[x, y1] = shade(p[x, y1], lo)
    for y in range(y0, y1 + 1):
        p[x0, y] = shade(p[x0, y], hi)
        p[x1, y] = shade(p[x1, y], lo)


def shadow(img, x0, y0, x1, y1, a=110, spread=3):
    over = Image.new('RGBA', img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(over)
    for k in range(spread, 0, -1):
        d.rectangle([x0 - k + 1, y0 - k + 2, x1 + k - 1, y1 + k + 1], fill=(20, 10, 4, a // (k + 1)))
    img.alpha_composite(over)


def corner_bracket(img, x, y, size, base, flipx=False, flipy=False):
    """L 자 무쇠/놋쇠 모서리 덧쇠 + 리벳"""
    t = 5
    br = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    a = metal(size, t, base)
    b = metal(t, size, base)
    br.alpha_composite(a, (0, 0))
    br.alpha_composite(b, (0, 0))
    # 끝을 뾰족하게 (백합 문양 흉내)
    p = br.load()
    for k in range(3):
        for j in range(k):
            p[size - 1 - j, k] = (0, 0, 0, 0)
            p[size - 1 - j, t - 1 - k] = (0, 0, 0, 0) if k else p[size - 1 - j, t - 1 - k]
            p[k, size - 1 - j] = (0, 0, 0, 0)
    rivet(br, 2, 2, base)
    rivet(br, size - 6, 2, base)
    rivet(br, 2, size - 6, base)
    if flipx:
        br = br.transpose(Image.FLIP_LEFT_RIGHT)
    if flipy:
        br = br.transpose(Image.FLIP_TOP_BOTTOM)
    img.alpha_composite(br, (x, y))


# ---------------------------------------------------------------- 창 그리기 (GUI 픽셀 단위)

WOOD = (128, 84, 46)
DARK_WOOD = (92, 58, 30)
LEATHER = (118, 64, 34)
LEATHER_C = (100, 53, 28)   # C 가죽을 조금 어둡게
LEATHER_DARK = (84, 44, 24)
PARCH = (241, 226, 189)


def frame_oak_leather(w, h, seed=10, corner=None):
    """A: 두꺼운 참나무 틀 + 모서리를 감싼 가죽 + 놋쇠 리벳"""
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    shadow(img, 2, 2, w - 3, h - 3, 140, 4)
    wood = planks(w - 4, h - 4, WOOD, seed=seed)
    img.alpha_composite(wood, (2, 2))
    outline(img, 2, 2, w - 3, h - 3, (40, 22, 10, 255))
    bevel(img, 3, 3, w - 4, h - 4)
    # 안쪽 홈 (판이 파인 자리)
    outline(img, 9, 9, w - 10, h - 10, (52, 30, 14, 255))
    outline(img, 10, 10, w - 11, h - 11, (168, 118, 70, 255))
    # 모서리 가죽 감싸기
    for cx, cy, fx, fy in ((2, 2, 0, 0), (w - 24, 2, 1, 0), (2, h - 24, 0, 1), (w - 24, h - 24, 1, 1)):
        lp = leather(22, 22, corner or LEATHER, seed=cx + cy)
        lp2 = Image.new('RGBA', (22, 22), (0, 0, 0, 0))
        mask = Image.new('L', (22, 22), 0)
        ImageDraw.Draw(mask).polygon([(0, 0), (21, 0), (21, 6), (6, 21), (0, 21)], fill=255)
        lp2.paste(lp, (0, 0), mask)
        d = ImageDraw.Draw(lp2)
        d.line([(21, 6), (6, 21)], fill=(50, 24, 10, 255))
        stitch(lp2, 3, 3, 17, 3)
        stitch(lp2, 3, 3, 3, 17)
        rivet(lp2, 7, 7, BRASS, 1)
        if fx:
            lp2 = lp2.transpose(Image.FLIP_LEFT_RIGHT)
        if fy:
            lp2 = lp2.transpose(Image.FLIP_TOP_BOTTOM)
        img.alpha_composite(lp2, (cx, cy))
    # 위아래 가장자리 놋쇠 징
    for x in range(34, w - 34, 26):
        rivet(img, x, 5, BRASS, 1)
        rivet(img, x, h - 6, BRASS, 1)
    return img


def add_leather_corners(img, w, h, col):
    for cx, cy, fx, fy in ((2, 2, 0, 0), (w - 24, 2, 1, 0), (2, h - 24, 0, 1), (w - 24, h - 24, 1, 1)):
        lp = leather(22, 22, col, seed=cx + cy)
        lp2 = Image.new('RGBA', (22, 22), (0, 0, 0, 0))
        mask = Image.new('L', (22, 22), 0)
        ImageDraw.Draw(mask).polygon([(0, 0), (21, 0), (21, 6), (6, 21), (0, 21)], fill=255)
        lp2.paste(lp, (0, 0), mask)
        d = ImageDraw.Draw(lp2)
        d.line([(21, 6), (6, 21)], fill=(40, 18, 8, 255))
        stitch(lp2, 3, 3, 17, 3)
        stitch(lp2, 3, 3, 3, 17)
        rivet(lp2, 7, 7, BRASS, 1)
        if fx:
            lp2 = lp2.transpose(Image.FLIP_LEFT_RIGHT)
        if fy:
            lp2 = lp2.transpose(Image.FLIP_TOP_BOTTOM)
        img.alpha_composite(lp2, (cx, cy))


def frame_iron_oak(w, h, seed=20):
    """B: 짙은 참나무 판 + 무쇠 띠·모서리 덧쇠·징"""
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    shadow(img, 2, 2, w - 3, h - 3, 150, 4)
    img.alpha_composite(planks(w - 4, h - 4, DARK_WOOD, vertical=True, board=13, seed=seed), (2, 2))
    outline(img, 2, 2, w - 3, h - 3, (26, 14, 6, 255))
    bevel(img, 3, 3, w - 4, h - 4)
    for y in (12, h - 18):
        band = metal(w - 8, 6, IRON, seed=y)
        img.alpha_composite(band, (4, y))
        for x in range(14, w - 14, 30):
            rivet(img, x, y + 3, IRON, 1)
    corner_bracket(img, 3, 3, 26, IRON)
    corner_bracket(img, w - 29, 3, 26, IRON, flipx=True)
    corner_bracket(img, 3, h - 29, 26, IRON, flipy=True)
    corner_bracket(img, w - 29, h - 29, 26, IRON, flipx=True, flipy=True)
    return img


def frame_leather_book(w, h, seed=30, base=None, leather_corners=False):
    """C: 무늬를 눌러 새긴 두꺼운 가죽 표지 + 놋쇠 모서리 + 바느질"""
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    shadow(img, 2, 2, w - 3, h - 3, 150, 4)
    lt = leather(w - 4, h - 4, base or LEATHER, seed=seed)
    img.alpha_composite(lt, (2, 2))
    outline(img, 2, 2, w - 3, h - 3, (40, 18, 8, 255))
    bevel(img, 3, 3, w - 4, h - 4, 1.25, 0.6)
    # 눌러 새긴 테두리 무늬
    p = img.load()
    for k, f in ((8, 0.7), (9, 1.18), (12, 0.75)):
        for x in range(k, w - k):
            p[x, k] = shade(p[x, k], f)
            p[x, h - 1 - k] = shade(p[x, h - 1 - k], f)
        for y in range(k, h - k):
            p[k, y] = shade(p[k, y], f)
            p[w - 1 - k, y] = shade(p[w - 1 - k, y], f)
    for x in range(16, w - 16, 8):  # 마름모 사슬 무늬
        for d in range(3):
            for yy in (10 - d, 10 + d):
                pass
    stitch(img, 6, 5, w - 6, 5)
    stitch(img, 6, h - 6, w - 6, h - 6)
    stitch(img, 5, 6, 5, h - 6)
    stitch(img, w - 6, 6, w - 6, h - 6)
    if leather_corners:
        add_leather_corners(img, w, h, shade(base or LEATHER, 0.62))
        return img
    for cx, cy, fx, fy in ((2, 2, 0, 0), (w - 20, 2, 1, 0), (2, h - 20, 0, 1), (w - 20, h - 20, 1, 1)):
        c = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
        m = metal(18, 18, BRASS, seed=cx)
        mask = Image.new('L', (18, 18), 0)
        ImageDraw.Draw(mask).polygon([(0, 0), (17, 0), (0, 17)], fill=255)
        c.paste(m, (0, 0), mask)
        ImageDraw.Draw(c).line([(17, 0), (0, 17)], fill=(110, 76, 20, 255))
        rivet(c, 4, 4, BRASS, 1)
        if fx:
            c = c.transpose(Image.FLIP_LEFT_RIGHT)
        if fy:
            c = c.transpose(Image.FLIP_TOP_BOTTOM)
        img.alpha_composite(c, (cx, cy))
    return img


def panel(img, x, y, w, h, seed, style):
    """양피지 판: 나무 홈에 끼운 듯 / 못으로 박은 듯 / 책장처럼"""
    shadow(img, x, y, x + w - 1, y + h - 1, 120, 3)
    pa = parchment(w, h, PARCH, seed=seed, burnt=(style != 'book'))
    img.alpha_composite(pa, (x, y))
    if style == 'nail':
        for nx, ny in ((x + 4, y + 4), (x + w - 5, y + 4), (x + 4, y + h - 5), (x + w - 5, y + h - 5)):
            rivet(img, nx, ny, IRON, 1)
    elif style == 'inset':
        outline(img, x - 1, y - 1, x + w, y + h, (48, 26, 10, 255))
    elif style == 'book':
        p = img.load()
        for yy in range(y, y + h):  # 가운데 쪽으로 그늘
            for xx in range(x + w - 8, x + w):
                p[xx, yy] = shade(p[xx, yy], 0.8 + 0.025 * (x + w - 1 - xx))


def leather_tag(w, h, text_w, seed=40):
    """가죽 이름표 (바느질 테두리, 아래 양쪽이 갈라진 리본 끝)"""
    img = Image.new('RGBA', (w, h + 6), (0, 0, 0, 0))
    lt = leather(w, h, LEATHER_DARK, seed=seed)
    mask = Image.new('L', (w, h), 0)
    ImageDraw.Draw(mask).polygon([(4, 0), (w - 5, 0), (w - 1, h // 2), (w - 5, h - 1), (4, h - 1), (0, h // 2)], fill=255)
    tag = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    tag.paste(lt, (0, 0), mask)
    d = ImageDraw.Draw(tag)
    d.polygon([(4, 0), (w - 5, 0), (w - 1, h // 2), (w - 5, h - 1), (4, h - 1), (0, h // 2)], outline=(36, 16, 6, 255))
    stitch(tag, 6, 2, w - 6, 2)
    stitch(tag, 6, h - 3, w - 6, h - 3)
    img.alpha_composite(tag, (0, 0))
    return img


def wood_button(w, h, seed=50, hover=False, metal_base=IRON):
    img = Image.new('RGBA', (w, h + 2), (0, 0, 0, 0))
    shadow(img, 0, 0, w - 1, h - 1, 150, 2)
    b = planks(w, h, (150, 100, 56) if hover else (132, 86, 48), board=h, seed=seed)
    img.alpha_composite(b, (0, 0))
    outline(img, 0, 0, w - 1, h - 1, (30, 16, 6, 255))
    bevel(img, 1, 1, w - 2, h - 2, 1.35, 0.55)
    band = metal(4, h - 2, metal_base)
    img.alpha_composite(band, (2, 1))
    img.alpha_composite(band, (w - 6, 1))
    rivet(img, 4, h // 2, metal_base, 1)
    rivet(img, w - 4, h // 2, metal_base, 1)
    if hover:
        glow = Image.new('RGBA', img.size, (0, 0, 0, 0))
        ImageDraw.Draw(glow).rectangle([0, 0, w - 1, h - 1], outline=(255, 214, 120, 200))
        img.alpha_composite(glow)
    return img


def wax_seal(r=13, seed=60):
    img = Image.new('RGBA', (r * 2 + 4, r * 2 + 4), (0, 0, 0, 0))
    p = img.load()
    rr = random.Random(seed)
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = x - r - 2, y - r - 2
            d = math.hypot(dx, dy)
            edge = r + 0.55 * math.sin(math.atan2(dy, dx) * 8 + 1)
            if d < edge:
                f = 1.15 - 0.45 * (d / r) + (0.25 if dx < -2 and dy < -2 and d > r * 0.5 else 0)
                if abs(d - r * 0.66) < 0.8:
                    f *= 0.7
                p[x, y] = shade((168, 28, 24), f)
    return img


# ---------------------------------------------------------------- 시안

FONT = '/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc'
FONT_R = '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'


def text(d, xy, s, size, fill, bold=False, shadow_col=None):
    f = ImageFont.truetype(FONT if bold else FONT_R, size, index=1 if False else 0)
    try:
        f = ImageFont.truetype(FONT if bold else FONT_R, size, index=1)  # KR
    except Exception:
        pass
    if shadow_col:
        d.text((xy[0] + 2, xy[1] + 2), s, font=f, fill=shadow_col)
    d.text(xy, s, font=f, fill=fill)
    return f


def mock_quest(frame_fn, panel_style, metal_base, title_style, label):
    W, H = 340, 200
    win = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    win.alpha_composite(frame_fn(W, H))
    panel(win, 16, 26, 98, 158, 101, panel_style)
    panel(win, 122, 26, 202, 158, 102, panel_style)
    # 선택된 목록 줄 (붉은 리본 책갈피 + 밝은 띠)
    sel = Image.new('RGBA', (88, 18), (255, 246, 214, 190))
    win.alpha_composite(sel, (21, 33))
    rib = Image.new('RGBA', (5, 22), (0, 0, 0, 0))
    ImageDraw.Draw(rib).polygon([(0, 0), (4, 0), (4, 21), (2, 18), (0, 21)], fill=(150, 30, 22, 255))
    win.alpha_composite(rib, (17, 31))
    # 내용 구분선 (갈색 잉크 덩굴 장식)
    d = ImageDraw.Draw(win)
    d.line([(132, 58), (312, 58)], fill=(150, 104, 52, 255))
    d.polygon([(222, 55), (225, 58), (222, 61), (219, 58)], fill=(150, 104, 52, 255))
    # 버튼
    btn = wood_button(70, 16, hover=True, metal_base=metal_base)
    win.alpha_composite(btn, (244, 160))
    # 제목 이름표
    if title_style == 'tag':
        tag = leather_tag(110, 22, 0)
        win.alpha_composite(tag, ((W - 110) // 2, -3))
    elif title_style == 'sign':
        sign = Image.new('RGBA', (110, 20), (0, 0, 0, 0))
        sign.alpha_composite(planks(110, 20, (160, 110, 60), board=20, seed=77))
        outline(sign, 0, 0, 109, 19, (30, 16, 6, 255))
        bevel(sign, 1, 1, 108, 18)
        dd = ImageDraw.Draw(win)
        for cx in ((W - 110) // 2 + 14, (W - 110) // 2 + 96):
            for k in range(0, 8, 2):
                dd.rectangle([cx, -6 + k, cx + 1, -5 + k], fill=(90, 90, 96, 255))
        win.alpha_composite(sign, ((W - 110) // 2, 0))
    else:
        tag = leather_tag(110, 22, 0, seed=44)
        win.alpha_composite(tag, ((W - 110) // 2, -3))
    seal = wax_seal(7)
    win.alpha_composite(seal, (238, 158))

    S = 3
    big = win.resize((W * S, H * S), Image.NEAREST)
    d = ImageDraw.Draw(big)
    ink, soft, red = (58, 36, 16), (130, 96, 56), (138, 34, 20)
    cream = (250, 230, 180)
    title = "기록관 이사벨"
    f = ImageFont.truetype(FONT, 27, index=1)
    bb = d.textbbox((0, 0), title, font=f)
    tw = bb[2] - bb[0]
    ty = (8 * S) - (bb[1] + bb[3]) / 2 + (2 if title_style == 'sign' else 0) * S
    d.text(((W * S - tw) / 2 + 2, ty + 2), title, font=f, fill=(20, 8, 2))
    d.text(((W * S - tw) / 2, ty), title, font=f, fill=cream if title_style != 'sign' else (44, 22, 6))
    text(d, (27 * S, 34 * S), "모래 속의 두 번째 장", 21, ink, True)
    text(d, (27 * S, 42 * S + 2), "새 퀘스트 · 1장 6/8", 16, red)
    text(d, (27 * S, 56 * S), "견습 서약", 21, soft)
    text(d, (27 * S, 64 * S), "완료", 16, (90, 120, 60))
    text(d, (132 * S, 31 * S), "모래 속의 두 번째 장", 30, (70, 30, 10), True)
    text(d, (132 * S, 45 * S - 2), "1장 잿불의 서약 (6/8)", 18, soft)
    body = ["어부 오스카가 투덜대더라고요. 북쪽 바다에서 물고기가", "사라진다고요. …그건 그렇고, 두 번째 페이지의 단서를", "찾았어요. 사막의 피라미드 아래에 연대기를 숨겼다는", "기록이 있어요."]
    for i, line in enumerate(body):
        text(d, (132 * S, 63 * S + i * 25), line, 19, ink)
    text(d, (132 * S, 101 * S), "◆ 목표", 20, red, True)
    text(d, (138 * S, 109 * S), "◇ 찾아가기: 사막 피라미드", 19, ink)
    text(d, (132 * S, 121 * S), "◆ 보상", 20, red, True)
    text(d, (138 * S, 129 * S), "1,000 G   ·   잿빛 연대기의 페이지 ×1", 19, ink)
    bf = ImageFont.truetype(FONT, 24, index=1)
    bw = d.textlength("수락", font=bf)
    d.text((244 * S + (70 * S - bw) / 2 + 2, 160 * S + 7 + 2), "수락", font=bf, fill=(20, 8, 2))
    d.text((244 * S + (70 * S - bw) / 2, 160 * S + 7), "수락", font=bf, fill=(255, 236, 190))
    return big


def mock(out):
    bg = Image.new('RGBA', (2150, 1500), (26, 23, 18, 255))
    d = ImageDraw.Draw(bg)
    text(d, (30, 20), "중세 질감 시안 (실제 게임에 들어갈 픽셀 질감으로 그림)", 36, (243, 231, 198), True)
    text(d, (30, 70), "나무결·옹이·판자 틈, 가죽 모공·바느질, 그을린 양피지 가장자리, 무쇠·놋쇠 리벳을 한 칸 한 칸 그렸어요. 게임 화면 배율 3 기준 크기입니다.", 20, (190, 178, 146))
    opts = [
        (frame_oak_leather, 'inset', BRASS, 'tag', "A. 참나무 틀 + 가죽 모서리 + 놋쇠", "두꺼운 참나무 판자 틀, 모서리를 가죽으로 감싸 바느질하고 놋쇠 리벳을 박음. 가죽 이름표, 홈에 끼운 양피지."),
        (frame_iron_oak, 'nail', IRON, 'sign', "B. 짙은 참나무 + 무쇠 띠", "세로 판자를 무쇠 띠와 L 자 덧쇠로 묶은 성문 같은 틀. 사슬에 매단 나무 간판, 못으로 박은 양피지."),
        (frame_leather_book, 'book', BRASS, 'tag2', "C. 가죽 장정 + 놋쇠 모서리", "눌러 새긴 무늬와 바느질이 있는 두꺼운 가죽 표지, 놋쇠 모서리. 책장처럼 가운데로 그늘진 양피지."),
    ]
    pos = [(30, 130), (1090, 130), (30, 820)]
    for (fn, ps, mb, ts, name, desc), (x, y) in zip(opts, pos):
        text(d, (x, y), name, 28, (255, 215, 106), True)
        text(d, (x, y + 40), desc, 18, (207, 196, 165))
        scene = Image.new('RGBA', (1040, 640), (0, 0, 0, 255))
        sd = ImageDraw.Draw(scene)
        for yy in range(640):
            c = mix((70, 84, 100), (36, 46, 30), yy / 640)
            sd.line([(0, yy), (1040, yy)], fill=c)
        scene.alpha_composite(Image.new('RGBA', (1040, 640), (0, 0, 0, 100)))
        q = mock_quest(fn, ps, mb, ts, name)
        scene.alpha_composite(q, ((1040 - q.width) // 2, 10))
        bg.alpha_composite(scene.crop((0, 0, 1040, 620)), (x, y + 70))
    # 오른쪽 아래: 재료 견본 확대
    x, y = 1090, 820
    text(d, (x, y), "재료 확대 (배율 6)", 28, (255, 215, 106), True)
    sw = [planks(48, 32, WOOD), planks(48, 32, DARK_WOOD, vertical=True, board=13, seed=20), leather(48, 32, LEATHER), parchment(48, 32, PARCH, seed=8),
          metal(48, 10, IRON), metal(48, 10, BRASS), wood_button(48, 16, hover=False), wax_seal(9)]
    names = ["참나무", "짙은 참나무", "가죽", "양피지", "무쇠", "놋쇠", "나무 버튼", "밀랍"]
    for i, (im, nm) in enumerate(zip(sw, names)):
        cx = x + (i % 4) * 255
        cy = y + 60 + (i // 4) * 270
        bg.alpha_composite(im.resize((im.width * 5, im.height * 5), Image.NEAREST), (cx, cy))
        text(d, (cx, cy + im.height * 5 + 8), nm, 20, (220, 208, 176))
    bg.convert('RGB').save(out)


def mock2(out):
    bg = Image.new('RGBA', (2150, 820), (26, 23, 18, 255))
    d = ImageDraw.Draw(bg)
    opts = [
        (lambda w, h: frame_oak_leather(w, h, corner=LEATHER_C), 'inset', BRASS, 'tag', "① A 참나무 틀 + 모서리를 C 가죽(조금 더 어둡게)으로", "틀은 A의 참나무, 모서리 가죽만 C의 가죽 색을 조금 어둡게."),
        (lambda w, h: frame_leather_book(w, h, base=LEATHER_C, leather_corners=True), 'book', BRASS, 'tag2', "② C 가죽 장정(조금 더 어둡게) + A의 가죽 모서리", "틀은 C의 가죽(조금 어둡게), 놋쇠 모서리 대신 A처럼 바느질한 가죽 모서리."),
    ]
    for i, (fn, ps, mb, ts, name, desc) in enumerate(opts):
        x, y = 30 + i * 1060, 20
        text(d, (x, y), name, 26, (255, 215, 106), True)
        text(d, (x, y + 38), desc + " 밀랍은 둥글게 다듬음.", 18, (207, 196, 165))
        scene = Image.new('RGBA', (1040, 640), (0, 0, 0, 255))
        sd = ImageDraw.Draw(scene)
        for yy in range(640):
            sd.line([(0, yy), (1040, yy)], fill=mix((70, 84, 100), (36, 46, 30), yy / 640))
        scene.alpha_composite(Image.new('RGBA', (1040, 640), (0, 0, 0, 100)))
        q = mock_quest(fn, ps, mb, ts, name)
        scene.alpha_composite(q, ((1040 - q.width) // 2, 10))
        bg.alpha_composite(scene.crop((0, 0, 1040, 620)), (x, y + 80))
    bg.convert('RGB').save(out)


def atlas(out):
    import os
    os.makedirs(out, exist_ok=True)
    leather(128, 128, LEATHER_C, seed=71, wrap=True).save(os.path.join(out, 'leather.png'))
    parchment(128, 128, PARCH, seed=72, burnt=False, wrap=True).save(os.path.join(out, 'parchment.png'))
    # 가죽 모서리 4개 (2x2): 왼위, 오른위 / 왼아래, 오른아래
    cor = Image.new('RGBA', (44, 44), (0, 0, 0, 0))
    col = shade(LEATHER_C, 0.62)
    for i, (fx, fy) in enumerate(((0, 0), (1, 0), (0, 1), (1, 1))):
        lp = leather(22, 22, col, seed=80 + i)
        lp2 = Image.new('RGBA', (22, 22), (0, 0, 0, 0))
        mask = Image.new('L', (22, 22), 0)
        ImageDraw.Draw(mask).polygon([(0, 0), (21, 0), (21, 6), (6, 21), (0, 21)], fill=255)
        lp2.paste(lp, (0, 0), mask)
        d = ImageDraw.Draw(lp2)
        d.line([(21, 6), (6, 21)], fill=(30, 12, 4, 255))
        d.line([(20, 7), (7, 20)], fill=shade(col, 1.25))
        stitch(lp2, 3, 3, 17, 3)
        stitch(lp2, 3, 3, 3, 17)
        rivet(lp2, 7, 7, BRASS, 1)
        if fx:
            lp2 = lp2.transpose(Image.FLIP_LEFT_RIGHT)
        if fy:
            lp2 = lp2.transpose(Image.FLIP_TOP_BOTTOM)
        cor.alpha_composite(lp2, (fx * 22, fy * 22))
    cor.save(os.path.join(out, 'leather_corners.png'))
    # 나무 버튼 3단 (보통 / 마우스 / 꺼짐), 각 64x16
    btn = Image.new('RGBA', (64, 48), (0, 0, 0, 0))
    for i, (base, mb) in enumerate((((132, 86, 48), IRON), ((156, 106, 60), BRASS), ((88, 64, 44), (90, 90, 94)))):
        b = planks(64, 16, base, board=16, seed=91 + i)
        outline(b, 0, 0, 63, 15, (30, 16, 6, 255))
        bevel(b, 1, 1, 62, 14, 1.35, 0.55)
        band = metal(4, 14, mb)
        b.alpha_composite(band, (2, 1))
        b.alpha_composite(band, (58, 1))
        rivet(b, 4, 8, mb, 1)
        rivet(b, 59, 8, mb, 1)
        if i == 1:
            ImageDraw.Draw(b).rectangle([0, 0, 63, 15], outline=(255, 214, 120, 255))
        btn.alpha_composite(b, (0, i * 16))
    btn.save(os.path.join(out, 'button.png'))
    seal = wax_seal(7)
    seal.save(os.path.join(out, 'seal.png'))
    print('atlas ok')


if __name__ == '__main__':
    if sys.argv[1] == 'atlas':
        atlas(sys.argv[2])
    if sys.argv[1] == 'mock':
        mock(sys.argv[2])
    elif sys.argv[1] == 'mock2':
        mock2(sys.argv[2])
