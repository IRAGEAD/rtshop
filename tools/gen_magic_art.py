# 마법 뽑기 그림 만들기: python3 tools/gen_magic_art.py
# 티켓·주문서 아이콘, 별빛 수정 구체(받침 블록·구슬·룬 고리), 연출용 룬석 그림
import math, os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/rtshop/textures')

def save(img, rel):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)

def recolor(src, mapping):
    im = Image.open(os.path.join(ROOT, src)).convert('RGBA')
    px = im.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255
            # 어두운 테두리·밝은 몸통·붉은 무늬를 각각 다른 색으로
            if r > 150 and g < 90:
                c = mapping['mark']
            elif lum < 0.3:
                c = mapping['edge']
            elif lum < 0.62:
                c = mapping['shade']
            else:
                c = mapping['body']
            px[x, y] = (*c, a)
    return im

# 티켓 (무작위: 보라 + 흰 별, 선택: 짙은 남보라 + 금 룬)
t1 = recolor('item/gacha_ticket_high.png', {'edge': (40, 18, 60), 'shade': (112, 62, 170), 'body': (160, 104, 220), 'mark': (250, 240, 255)})
t2 = recolor('item/gacha_ticket_high.png', {'edge': (24, 14, 48), 'shade': (70, 40, 130), 'body': (104, 66, 186), 'mark': (255, 206, 80)})
save(t1, 'item/magic_ticket.png')
save(t2, 'item/magic_ticket_select.png')
s1 = recolor('item/oath_scroll.png', {'edge': (20, 34, 70), 'shade': (90, 140, 210), 'body': (190, 220, 250), 'mark': (120, 80, 230)})
save(s1, 'item/enchant_scroll.png')

# 받침 블록: 흑요석 + 보라 빛줄
def obsidian(seed, veins=True, top=False):
    import random
    rnd = random.Random(seed)
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            v = rnd.randint(0, 22)
            px[x, y] = (16 + v // 2, 10 + v // 3, 26 + v, 255)
    if veins:
        for x in range(16):
            y = int(8 + 3 * math.sin(x * 0.7 + seed))
            px[x, y] = (150, 90, 230, 255)
            if 0 <= y + 1 < 16:
                px[x, y + 1] = (90, 50, 160, 255)
    if top:
        d = ImageDraw.Draw(im)
        d.ellipse((3, 3, 12, 12), outline=(170, 110, 250, 255))
        d.point((8, 8), fill=(230, 200, 255, 255))
    # 테두리
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            r, g, b, a = px[x, y]
            px[x, y] = (max(0, r - 8), max(0, g - 6), max(0, b - 12), 255)
    return im

save(obsidian(1), 'block/arcane_pedestal_side.png')
save(obsidian(2, veins=False, top=True), 'block/arcane_pedestal_top.png')
save(obsidian(3, veins=False), 'block/arcane_pedestal_bottom.png')

# 구슬 (가운데가 밝은 자수정)
N = 32
orb = Image.new('RGBA', (N, N))
px = orb.load()
for y in range(N):
    for x in range(N):
        dx, dy = (x - 15.5) / 15.5, (y - 15.5) / 15.5
        r = math.hypot(dx, dy)
        if r > 1:
            continue
        hl = max(0, 1 - math.hypot(dx + 0.35, dy + 0.35) * 1.6)
        k = 1 - r * 0.55
        c = (int(120 * k + 135 * hl), int(60 * k + 170 * hl), int(210 * k + 45 * hl))
        a = 255 if r < 0.92 else int(255 * (1 - (r - 0.92) / 0.08))
        px[x, y] = (min(255, c[0]), min(255, c[1]), min(255, c[2]), a)
save(orb, 'entity/arcane_orb.png')

# 빛무리 (구슬 둘레)
G = 32
glow = Image.new('RGBA', (G, G))
px = glow.load()
for y in range(G):
    for x in range(G):
        r = math.hypot((x - 15.5) / 15.5, (y - 15.5) / 15.5)
        if r < 1:
            px[x, y] = (200, 150, 255, int(150 * (1 - r) ** 2))
save(glow, 'entity/arcane_glow.png')

# 룬 고리: 바깥(굵은 고리 + 룬 눈금), 안쪽(가는 고리 + 점)
def ring(size, inner, outer, ticks, color, dot=False):
    im = Image.new('RGBA', (size, size))
    d = ImageDraw.Draw(im)
    c = size / 2
    d.ellipse((c - outer, c - outer, c + outer, c + outer), outline=color, width=2)
    d.ellipse((c - inner, c - inner, c + inner, c + inner), outline=color, width=1)
    for i in range(ticks):
        a = i * 2 * math.pi / ticks
        mid = (inner + outer) / 2
        x, y = c + mid * math.cos(a), c + mid * math.sin(a)
        if dot:
            d.ellipse((x - 1.2, y - 1.2, x + 1.2, y + 1.2), fill=color)
        else:
            # 작은 룬 모양 (세로 획 + 비스듬한 획)
            tx, ty = -math.sin(a), math.cos(a)
            nx, ny = math.cos(a), math.sin(a)
            d.line((x - nx * 2, y - ny * 2, x + nx * 2, y + ny * 2), fill=color, width=1)
            d.line((x, y, x + tx * 2 + nx, y + ty * 2 + ny), fill=color, width=1)
    return im

save(ring(64, 25, 30, 12, (190, 140, 255, 235)), 'entity/arcane_ring_outer.png')
save(ring(64, 16, 19, 8, (255, 214, 120, 220), dot=True), 'entity/arcane_ring_inner.png')

# 연출용 룬석 5개 (16x16 씩 가로로)
runes = Image.new('RGBA', (80, 16))
d = ImageDraw.Draw(runes)
glyphs = [
    [(8, 2, 8, 14), (8, 5, 12, 2), (8, 9, 12, 6)],          # ᚠ
    [(5, 2, 5, 14), (5, 2, 11, 6), (11, 6, 11, 14)],         # ᚢ
    [(8, 2, 8, 14), (8, 5, 12, 8), (12, 8, 8, 11)],          # ᚦ
    [(4, 14, 8, 2), (8, 2, 12, 14), (6, 9, 10, 9)],          # ᚨ
    [(8, 2, 8, 14), (4, 6, 12, 10), (12, 6, 4, 10)],         # ᛟ 비슷
]
for i, g in enumerate(glyphs):
    ox = i * 16
    d.polygon([(ox + 8, 0), (ox + 15, 8), (ox + 8, 15), (ox + 1, 8)], fill=(46, 30, 70, 255), outline=(120, 90, 170, 255))
    for (x1, y1, x2, y2) in g:
        d.line((ox + x1, y1, ox + x2, y2), fill=(235, 225, 255, 255), width=1)
save(runes, 'gui/magic_runes.png')
print('ok')

# ---------------------------------------------------------------- 룬 제단 창 (GUI) 그림
import random as _r
def _save(img, rel): save(img, rel)
# 흑요석 판 (타일)
rnd = _r.Random(11)
slab = Image.new('RGBA', (64, 64))
px = slab.load()
for y in range(64):
    for x in range(64):
        v = rnd.randint(0, 10)
        px[x, y] = (14 + v, 11 + v // 2, 22 + v + v // 2, 255)
d = ImageDraw.Draw(slab)
for _ in range(5):
    x, y = rnd.randint(0, 63), rnd.randint(0, 63)
    for k in range(rnd.randint(6, 14)):
        d.point((x % 64, y % 64), fill=(34, 24, 52, 255))
        x += rnd.choice([-1, 0, 1]); y += 1
_save(slab, 'gui/magic/slab.png')

def big_ring(size, r_out, r_in, n, color, runes=True, seed=1):
    S = size * 2  # 두 배로 그려 줄인다 (매끈하게)
    im = Image.new('RGBA', (S, S)); d = ImageDraw.Draw(im); c = S / 2
    d.ellipse((c - r_out * 2, c - r_out * 2, c + r_out * 2, c + r_out * 2), outline=color, width=5)
    d.ellipse((c - r_in * 2, c - r_in * 2, c + r_in * 2, c + r_in * 2), outline=color, width=3)
    rr = _r.Random(seed)
    for i in range(n):
        a = i * 2 * math.pi / n
        mid = (r_out + r_in)
        x, y = c + mid * math.cos(a), c + mid * math.sin(a)
        nx, ny = math.cos(a), math.sin(a); tx, ty = -ny, nx
        h = (r_out - r_in) * 0.62
        if runes:
            # 룬: 세로 획 + 무작위 가지 한두 개
            d.line((x - nx * h, y - ny * h, x + nx * h, y + ny * h), fill=color, width=4)
            for _ in range(rr.randint(1, 2)):
                o = rr.uniform(-0.6, 0.6) * h; s = rr.choice([-1, 1])
                d.line((x + nx * o, y + ny * o, x + nx * (o + h * 0.5) + tx * s * h * 0.7, y + ny * (o + h * 0.5) + ty * s * h * 0.7), fill=color, width=4)
        else:
            d.ellipse((x - 4, y - 4, x + 4, y + 4), fill=color)
    return im.resize((size, size), Image.LANCZOS)

_save(big_ring(256, 124, 104, 20, (255, 255, 255, 255), seed=3), 'gui/magic/ring_outer.png')
_save(big_ring(256, 124, 112, 28, (255, 255, 255, 255), runes=False), 'gui/magic/ring_inner.png')
tick = Image.new('RGBA', (512, 512)); d = ImageDraw.Draw(tick)
for i in range(48):
    a = i * math.pi / 24; r0, r1 = 236, (252 if i % 4 == 0 else 244)
    d.line((256 + math.cos(a) * r0, 256 + math.sin(a) * r0, 256 + math.cos(a) * r1, 256 + math.sin(a) * r1), fill=(255, 255, 255, 230), width=3)
_save(tick.resize((256, 256), Image.LANCZOS), 'gui/magic/ring_ticks.png')

# 구슬과 빛무리 (크게)
N = 128
o = Image.new('RGBA', (N, N)); px = o.load()
for y in range(N):
    for x in range(N):
        dx, dy = (x - 63.5) / 63.5, (y - 63.5) / 63.5; r = math.hypot(dx, dy)
        if r > 1: continue
        hl = max(0, 1 - math.hypot(dx + 0.35, dy + 0.38) * 1.7); k = 1 - r * 0.6
        rim = max(0, (r - 0.8) / 0.2) * 0.5
        c = (int(110 * k + 140 * hl + 60 * rim), int(52 * k + 180 * hl + 30 * rim), int(205 * k + 50 * hl + 50 * rim))
        a = 255 if r < 0.96 else int(255 * (1 - (r - 0.96) / 0.04))
        px[x, y] = (min(255, c[0]), min(255, c[1]), min(255, c[2]), a)
_save(o, 'gui/magic/orb.png')
gl = Image.new('RGBA', (N, N)); px = gl.load()
for y in range(N):
    for x in range(N):
        r = math.hypot((x - 63.5) / 63.5, (y - 63.5) / 63.5)
        if r < 1: px[x, y] = (255, 255, 255, int(220 * (1 - r) ** 2.2))
_save(gl, 'gui/magic/glow.png')

# 룬석 (마름모): 꺼짐 / 켜짐. 5개 룬을 가로로 (각 64x80)
def stone_sheet(lit):
    S = 2
    im = Image.new('RGBA', (64 * 5 * S, 80 * S)); d = ImageDraw.Draw(im)
    for i, g in enumerate(glyphs):
        ox = i * 64 * S; cx, cy = ox + 32 * S, 40 * S
        body = (58, 34, 96, 255) if lit else (30, 24, 44, 255)
        edge = (246, 208, 128, 255) if lit else (124, 98, 170, 255)
        pts = [(cx, cy - 37 * S), (cx + 29 * S, cy), (cx, cy + 37 * S), (cx - 29 * S, cy)]
        d.polygon(pts, fill=body, outline=edge)
        d.line(pts + [pts[0]], fill=edge, width=3 * S)
        # 깎인 면
        d.polygon([(cx, cy - 37 * S), (cx + 29 * S, cy), (cx, cy - 6 * S)], fill=tuple(min(255, v + (26 if lit else 12)) for v in body[:3]) + (255,))
        d.polygon([(cx, cy + 37 * S), (cx - 29 * S, cy), (cx, cy + 6 * S)], fill=tuple(max(0, v - 10) for v in body[:3]) + (255,))
        d.line(pts + [pts[0]], fill=edge, width=3 * S)
        col = (255, 244, 214, 255) if lit else (168, 150, 204, 255)
        for (x1, y1, x2, y2) in g:
            d.line((cx + (x1 - 8) * 2.2 * S, cy - 26 * S + (y1 - 2) * 1.5 * S, cx + (x2 - 8) * 2.2 * S, cy - 26 * S + (y2 - 2) * 1.5 * S), fill=col, width=2 * S)
    return im.resize((320, 80), Image.LANCZOS)
_save(stone_sheet(False), 'gui/magic/stones.png')
_save(stone_sheet(True), 'gui/magic/stones_on.png')
# 오각 별 (소환진 안쪽 선)
st = Image.new('RGBA', (512, 512)); d = ImageDraw.Draw(st)
P = [(256 + 206 * math.cos(math.radians(-90 + 72 * i)), 256 + 206 * math.sin(math.radians(-90 + 72 * i))) for i in range(5)]
for i in range(5):
    d.line((P[i], P[(i + 2) % 5]), fill=(255, 255, 255, 150), width=3)
    d.line((P[i], P[(i + 1) % 5]), fill=(255, 255, 255, 70), width=2)
d.ellipse((256 - 78, 256 - 78, 256 + 78, 256 + 78), outline=(255, 255, 255, 120), width=2)
_save(st.resize((256, 256), Image.LANCZOS), 'gui/magic/star.png')
print('altar ok')

# ---------------------------------------------------------------- 장소 조사 흔적의 그림 (8종, 각 32x32)
def spot_sheet():
    S = 4
    im = Image.new('RGBA', (32 * 8 * S, 32 * S)); d = ImageDraw.Draw(im)
    W = (255, 255, 255, 255)
    def L(i, pts, w=2): d.line([(i * 32 * S + x * S, y * S) for x, y in pts], fill=W, width=w * S)
    def E(i, box, w=2): d.ellipse([i * 32 * S + box[0] * S, box[1] * S, i * 32 * S + box[2] * S, box[3] * S], outline=W, width=w * S)
    # 0 circle 마법진
    E(0, (4, 4, 28, 28)); E(0, (10, 10, 22, 22), 1); L(0, [(16, 4), (26, 24), (6, 24), (16, 4)], 1)
    # 1 mural 벽화
    L(1, [(5, 6), (27, 6), (27, 26), (5, 26), (5, 6)]); L(1, [(8, 22), (13, 14), (17, 19), (21, 11), (24, 22)], 1); E(1, (19, 8, 23, 12), 1)
    # 2 scrawl 낙서
    L(2, [(6, 9), (12, 7), (17, 10), (25, 8)], 1); L(2, [(6, 15), (11, 17), (18, 14), (26, 16)], 1); L(2, [(6, 22), (13, 21), (19, 24)], 1); L(2, [(22, 20), (26, 25)]); L(2, [(26, 20), (22, 25)])
    # 3 board 명령판
    L(3, [(6, 5), (26, 5), (26, 20), (6, 20), (6, 5)]); L(3, [(16, 20), (16, 28)]); L(3, [(10, 10), (22, 10)], 1); L(3, [(10, 15), (19, 15)], 1)
    # 4 camp 야영지
    L(4, [(4, 26), (16, 6), (28, 26), (4, 26)]); L(4, [(16, 26), (16, 16)], 1); L(4, [(12, 26), (16, 18), (20, 26)], 1)
    # 5 relic 유품
    L(5, [(16, 4), (26, 12), (22, 27), (10, 27), (6, 12), (16, 4)]); L(5, [(6, 12), (26, 12)], 1); L(5, [(16, 4), (16, 27)], 1)
    # 6 bones 유해
    L(6, [(8, 8), (24, 24)]); L(6, [(24, 8), (8, 24)]); 
    for (x, y) in [(7, 7), (25, 25), (25, 7), (7, 25)]: E(6, (x - 3, y - 3, x + 3, y + 3), 1)
    # 7 book 기록
    L(7, [(4, 8), (16, 11), (28, 8), (28, 24), (16, 27), (4, 24), (4, 8)]); L(7, [(16, 11), (16, 27)], 1); L(7, [(7, 13), (13, 15)], 1); L(7, [(19, 15), (25, 13)], 1); L(7, [(7, 18), (13, 20)], 1)
    return im.resize((256, 32), Image.LANCZOS)
save(spot_sheet(), 'gui/spots.png')
print('spots ok')
