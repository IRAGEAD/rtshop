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
