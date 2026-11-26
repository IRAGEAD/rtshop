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

# 티켓: 룬 부적 (세로로 긴 카드). 무작위 = 보랏빛 바탕에 흰 별빛과 다섯 분야의 점, 선택 = 짙은 남보라 바탕에 금테와 금빛 룬.
def card(edge, body, light, shade, motif, colors, corners=None):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(1, 15):
        for x in range(3, 14):
            if (x in (3, 13)) and (y in (1, 14)):
                continue  # 둥근 모서리
            rim = x in (3, 13) or y in (1, 14)
            px[x, y] = (*edge, 255) if rim else (*body, 255)
    for x in range(4, 13):
        px[x, 2] = (*light, 255)
        px[x, 13] = (*shade, 255)
    for y in range(3, 13):
        px[12, y] = (*shade, 255)
    for iy, row in enumerate(motif):
        for ix, ch in enumerate(row):
            if ch != '.':
                px[4 + ix, 2 + iy] = (*colors[ch], 255)
    if corners:
        for (x, y) in ((4, 2), (12, 2), (4, 13), (12, 13)):
            px[x, y] = (*corners, 255)
    return im

t1 = card((44, 20, 70), (150, 96, 214), (190, 144, 240), (108, 60, 168), [
    '.........',
    '.........',
    '....W....',
    '....W....',
    '...WWW...',
    '.WWWYWWW.',
    '...WWW...',
    '....W....',
    '....W....',
    '.........',
    '.r.g.b.y.',
    '.........',
], {'W': (250, 244, 255), 'Y': (255, 232, 150), 'r': (255, 150, 150), 'g': (160, 240, 170), 'b': (150, 210, 255), 'y': (255, 226, 130)})
t2 = card((22, 12, 44), (74, 46, 150), (104, 72, 190), (50, 30, 110), [
    '.........',
    '.........',
    '..G.G.G..',
    '..G.G.G..',
    '...GGG...',
    '....G....',
    '....H....',
    '....G....',
    '....G....',
    '.........',
    '...GHG...',
    '.........',
], {'G': (255, 206, 80), 'H': (255, 240, 180)}, corners=(255, 206, 80))
save(t1, 'item/magic_ticket.png')
save(t2, 'item/magic_ticket_select.png')

# 인챈트 재설정 주문서: 금빛 봉이 달린 두루마리, 푸른 종이에 빛나는 룬 글씨와 보랏빛 봉인
def scroll():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    E, ROD, RODL, KNOB = (20, 30, 66), (70, 104, 176), (120, 160, 226), (255, 206, 80)
    PAPER, PAPERL, PAPERS = (196, 222, 250), (226, 240, 255), (150, 186, 232)
    for y0 in (1, 12):
        for x in range(3, 13):
            px[x, y0] = (*E, 255)
            px[x, y0 + 1] = (*RODL, 255)
            px[x, y0 + 2] = (*ROD, 255)
        for y in (y0 + 1, y0 + 2):
            px[2, y] = (*KNOB, 255)
            px[13, y] = (*KNOB, 255)
        px[2, y0] = px[13, y0] = (*E, 255)
    for x in range(2, 14):
        px[x, 15] = (*E, 255)
    for y in range(4, 12):
        px[3, y] = (*E, 255)
        px[12, y] = (*E, 255)
        for x in range(4, 12):
            px[x, y] = (*PAPER, 255)
        px[4, y] = (*PAPERL, 255)
        px[11, y] = (*PAPERS, 255)
    C, V, VL = (60, 150, 235), (132, 84, 232), (214, 190, 255)
    for (x, y) in ((5, 5), (6, 5), (8, 5), (10, 5), (5, 7), (7, 7), (8, 7), (10, 7), (6, 9), (5, 9)):
        px[x, y] = (*C, 255)
    for (x, y) in ((8, 9), (9, 9), (10, 9), (8, 10), (10, 10), (9, 10)):
        px[x, y] = (*V, 255)
    px[9, 9] = (*VL, 255)
    return im

save(scroll(), 'item/enchant_scroll.png')

# 받침 블록 「별빛 천구」: 남빛 돌 받침에 은빛 테, 가는 기둥의 푸른 빛틈. 옆면 그림은 모델이 줄 단위로 나눠 쓴다
# (0~1 줄 = 받침 접시, 4~8 줄 = 기둥, 12~13 줄 = 둘째 단, 14~15 줄 = 바닥 단).
NAVY, NAVY2, NAVY3, SILVER, CYAN, CYAN2 = (40, 44, 78), (58, 64, 108), (74, 82, 134), (196, 206, 230), (120, 220, 255), (70, 150, 200)
def navy(seed, base=NAVY):
    import random
    rnd = random.Random(seed)
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            v = rnd.randint(-5, 5)
            px[x, y] = (base[0] + v, base[1] + v, base[2] + v * 2, 255)
    return im

side = navy(1)
px = side.load()
for x in range(16):
    px[x, 0] = (*SILVER, 255)                      # 받침 접시의 은테
    px[x, 1] = (*NAVY2, 255)
    px[x, 12] = (*NAVY3, 255)                      # 둘째 단
    px[x, 13] = (*NAVY2, 255)
    px[x, 14] = (*SILVER, 255)                     # 바닥 단의 은테
for y in range(4, 9):                              # 기둥: 가운데 푸른 빛틈
    for x in (6, 9):
        px[x, y] = (*NAVY2, 255)
for y in (5, 6, 7):
    px[7, y] = (*CYAN, 255)
    px[8, y] = (*CYAN2, 255)
save(side, 'block/arcane_pedestal_side.png')

top = navy(2, NAVY2)
d = ImageDraw.Draw(top)
d.rectangle((2, 2, 13, 13), outline=(*SILVER, 255))      # 바닥 단의 은테
d.rectangle((4, 4, 11, 11), outline=(*NAVY3, 255))       # 둘째 단
d.rectangle((5, 5, 10, 10), outline=(*SILVER, 255))      # 받침 접시의 은테
d.rectangle((7, 7, 8, 8), fill=(*CYAN, 255))             # 접시 가운데의 빛
save(top, 'block/arcane_pedestal_top.png')
save(navy(3), 'block/arcane_pedestal_bottom.png')

# 고리(혼천의의 띠)는 색만 입힌 입체로 그린다: 흰 그림 한 장
save(Image.new('RGBA', (4, 4), (255, 255, 255, 255)), 'entity/white.png')

# 구슬 (푸른 별빛): 가장자리가 어두워지지 않고, 가장자리로 갈수록 하늘빛 빛이 감돈다 (3.51: 검은 테두리 없앰)
def orb_tex(N, core, mid, rim_light):
    im = Image.new('RGBA', (N, N)); px = im.load(); h = (N - 1) / 2
    for y in range(N):
        for x in range(N):
            dx, dy = (x - h) / h, (y - h) / h
            r = math.hypot(dx, dy)
            if r > 1:
                continue
            t = min(1.0, r / 0.6)
            base = [core[i] + (mid[i] - core[i]) * t for i in range(3)]
            rim = max(0.0, (r - 0.5) / 0.5) ** 1.6
            c = [base[i] + (rim_light[i] - base[i]) * rim * 0.85 for i in range(3)]
            hl = max(0, 1 - math.hypot(dx + 0.34, dy + 0.36) * 1.9) ** 1.3
            c = [min(255, c[i] + (255 - c[i]) * hl * 0.9) for i in range(3)]
            a = 255 if r < 0.94 else int(255 * (1 - (r - 0.94) / 0.06))
            px[x, y] = (int(c[0]), int(c[1]), int(c[2]), a)
    return im
save(orb_tex(64, (74, 66, 214), (104, 110, 244), (170, 236, 255)), 'entity/arcane_orb.png')

# 빛무리 (구슬 둘레): 하늘빛
G = 64
glow = Image.new('RGBA', (G, G))
px = glow.load()
for y in range(G):
    for x in range(G):
        r = math.hypot((x - 31.5) / 31.5, (y - 31.5) / 31.5)
        if r < 1:
            px[x, y] = (150, 222, 255, int(190 * (1 - r) ** 2.1))
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
_save(orb_tex(N, (96, 44, 190), (132, 70, 226), (176, 226, 255)), 'gui/magic/orb.png')
gl = Image.new('RGBA', (N, N)); px = gl.load()
for y in range(N):
    for x in range(N):
        r = math.hypot((x - 63.5) / 63.5, (y - 63.5) / 63.5)
        if r < 1: px[x, y] = (255, 255, 255, int(220 * (1 - r) ** 2.2))
_save(gl, 'gui/magic/glow.png')

# 룬석 (마름모): 꺼짐 / 켜짐. 5개 룬을 가로로 (각 64x80)
def stone_sheet(lit, sel=False):
    S = 2
    im = Image.new('RGBA', (64 * 5 * S, 80 * S)); d = ImageDraw.Draw(im)
    for i, g in enumerate(glyphs):
        ox = i * 64 * S; cx, cy = ox + 32 * S, 40 * S
        body = (58, 34, 96, 255) if lit else (30, 24, 44, 255)
        edge = (246, 208, 128, 255) if lit else (124, 98, 170, 255)
        if sel:  # 고른 룬석: 금빛 테가 푸른빛으로 바뀐다
            body = (52, 44, 118, 255); edge = (120, 208, 255, 255)
        pts = [(cx, cy - 37 * S), (cx + 29 * S, cy), (cx, cy + 37 * S), (cx - 29 * S, cy)]
        d.polygon(pts, fill=body, outline=edge)
        d.line(pts + [pts[0]], fill=edge, width=3 * S)
        # 깎인 면
        d.polygon([(cx, cy - 37 * S), (cx + 29 * S, cy), (cx, cy - 6 * S)], fill=tuple(min(255, v + (26 if lit else 12)) for v in body[:3]) + (255,))
        d.polygon([(cx, cy + 37 * S), (cx - 29 * S, cy), (cx, cy + 6 * S)], fill=tuple(max(0, v - 10) for v in body[:3]) + (255,))
        d.line(pts + [pts[0]], fill=edge, width=3 * S)
        col = (255, 244, 214, 255) if lit else (168, 150, 204, 255)
        if sel: col = (226, 246, 255, 255)
        for (x1, y1, x2, y2) in g:
            d.line((cx + (x1 - 8) * 2.2 * S, cy - 26 * S + (y1 - 2) * 1.5 * S, cx + (x2 - 8) * 2.2 * S, cy - 26 * S + (y2 - 2) * 1.5 * S), fill=col, width=2 * S)
    return im.resize((320, 80), Image.LANCZOS)
_save(stone_sheet(False), 'gui/magic/stones.png')
_save(stone_sheet(True), 'gui/magic/stones_on.png')
_save(stone_sheet(True, True), 'gui/magic/stones_sel.png')
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


# ---------------------------------------------------------------- 3.64: 룬 제단 가운데의 빛나는 구슬 (색을 고를 수 있게 색마다 한 벌)
# 색 이름 → (깊은 색, 몸통 색, 가장자리 빛, 빛무리 색). 이름은 client/ArcaneOrbRenderer.COLORS 와 같아야 한다.
ORB_COLORS = {
    'blue':   ((36, 78, 226), (78, 136, 255), (176, 232, 255), (120, 200, 255)),   # 별빛 푸른색 (기본)
    'ice':    ((136, 180, 232), (196, 222, 250), (255, 255, 255), (214, 236, 255)), # 달빛 은백
    'gold':   ((226, 140, 24), (250, 194, 76), (255, 244, 190), (255, 212, 116)),   # 황금빛
    'violet': ((112, 44, 206), (158, 94, 245), (232, 190, 255), (196, 140, 255)),   # 자수정 보라
    'jade':   ((12, 142, 134), (56, 202, 178), (190, 255, 236), (110, 240, 210)),   # 비취 청록
    'rose':   ((206, 44, 104), (245, 102, 150), (255, 204, 220), (255, 140, 180)),  # 장미빛
}

def orb_lit(N, deep, body, rim_light):
    """속에서 빛이 나는 구슬: 가운데는 환하고, 그 둘레가 가장 짙고, 가장자리에는 다시 빛이 감돈다. 왼쪽 위에 반짝임. 어두운(검은) 빛깔은 쓰지 않는다."""
    im = Image.new('RGBA', (N, N)); px = im.load(); h = (N - 1) / 2
    for y in range(N):
        for x in range(N):
            dx, dy = (x - h) / h, (y - h) / h
            r = math.hypot(dx, dy)
            if r > 1:
                continue
            core = max(0.0, 1 - r / 0.50) ** 1.4                 # 가운데의 환한 빛
            c = [body[i] + (deep[i] - body[i]) * min(1.0, max(0.0, (r - 0.15) / 0.45)) for i in range(3)]
            c = [c[i] + (255 - c[i]) * core * 0.80 for i in range(3)]
            rim = max(0.0, (r - 0.58) / 0.42) ** 1.5             # 가장자리에 감도는 빛
            c = [c[i] + (rim_light[i] - c[i]) * rim * 0.90 for i in range(3)]
            hl = max(0, 1 - math.hypot(dx + 0.36, dy + 0.38) * 2.6) ** 1.2
            c = [min(255, c[i] + (255 - c[i]) * hl * 0.95) for i in range(3)]
            a = 255 if r < 0.94 else int(255 * (1 - (r - 0.94) / 0.06))
            px[x, y] = (int(c[0]), int(c[1]), int(c[2]), a)
    return im

def orb_halo(N, color, peak, power):
    """빛무리: 빛깔은 어디서나 같고 투명도만 바깥으로 옅어진다 (어두운 빛깔이 섞이면 셰이더·어두운 곳에서 검은 테로 보인다)."""
    im = Image.new('RGBA', (N, N)); px = im.load(); h = (N - 1) / 2
    for y in range(N):
        for x in range(N):
            r = math.hypot((x - h) / h, (y - h) / h)
            px[x, y] = (*color, int(peak * (1 - r) ** power) if r < 1 else 0)
    return im

def orb_rim_add(N, color, power=1.3):
    """구슬 둘레의 빛 (3.64.2): 더하기 섞기(RenderType.eyes)용 그림 — 빛깔에 세기를 미리 곱해 둔다(더하기 섞기는 투명도를 보지 않는다).
    가운데는 구슬이 덮고, 바깥 가장자리로 갈수록 0 이 된다. 예전의 넓은 빛무리(_halo)와 알파 섞기 빛(_glow)은 Photon 같은 셰이더에서
    다른 무늬를 덮거나 탁하게 보여서 없앴다."""
    im = Image.new('RGBA', (N, N)); px = im.load(); h = (N - 1) / 2
    for y in range(N):
        for x in range(N):
            r = math.hypot((x - h) / h, (y - h) / h)
            k = max(0.0, 1 - r) ** power if r < 1 else 0.0
            px[x, y] = (int(color[0] * k), int(color[1] * k), int(color[2] * k), int(255 * k))
    return im

def orb_core_add(lit):
    """구슬 몸통의 더하기 섞기 판 (3.64.2): 셰이더를 켰을 때만 몸통 위에 한 번 더 그린다 (RenderType.eyes = 셰이더의 발광 단계).
    셰이더는 빛을 받지 않게 그린 몸통도 그늘에서 어둡게 칠하므로, 구슬이 스스로 빛나 보이게 한다. 빛깔에 투명도를 미리 곱해 둔다."""
    im = Image.new('RGBA', lit.size); src = lit.load(); px = im.load()
    for y in range(lit.size[1]):
        for x in range(lit.size[0]):
            r, g, b, a = src[x, y]
            k = a / 255.0
            px[x, y] = (int(r * k), int(g * k), int(b * k), a)
    return im

for _name, (_deep, _body, _rim, _glow) in ORB_COLORS.items():
    _lit = orb_lit(64, _deep, _body, _rim)
    save(_lit, 'entity/orb/%s.png' % _name)
    save(orb_rim_add(64, _glow), 'entity/orb/%s_rim.png' % _name)            # 구슬 바로 둘레의 빛 (더하기 섞기)
    save(orb_core_add(_lit), 'entity/orb/%s_core.png' % _name)               # 셰이더를 켰을 때 몸통에 더하는 빛
