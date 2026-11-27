# W-4 상점 토지 시안 그림 (3.78, 고르기 전의 안): python3 tools/mock/w4_shop_land.py <출력 폴더>
#   1) 지도 위 후보 자리 A~D  2) 자리별 배치 모습  3) 사고 쓰는 흐름 (화면 모양)
# 바탕은 마을 지도(castle_map.png, 1픽셀 = 1블록, 왼쪽 위 620x640 칸)와 castle_plots.json 의 택지.
import json, os, sys, zlib, base64
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
OUT = sys.argv[1] if len(sys.argv) > 1 else '/tmp/w4'
os.makedirs(OUT, exist_ok=True)
FONT = '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
FONTB = '/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc'


def F(size, bold=False):
    return ImageFont.truetype(FONTB if bold else FONT, size, index=1)


plots = json.load(open(os.path.join(ROOT, 'src/main/resources/rtshop_defaults/castle_plots.json'), encoding='utf-8'))
M = plots['map']
base = Image.open(os.path.join(ROOT, 'src/main/resources/assets/rtshop/textures/gui/castle_map.png')).convert('RGB').crop((0, 0, M['w'], M['h']))
K = 1.5
big = base.resize((int(M['w'] * K), int(M['h'] * K)), Image.NEAREST)
SPAWN = (plots['spawn'][0] - M['x0'], plots['spawn'][2] - M['z0']) if isinstance(plots.get('spawn'), list) else (578, 316)
LAND_PER_CELL = 40000 / 256   # 일반 토지 문서 16x16 = 40,000 G


def P(x, z):
    """지도 칸(블록) → 그림 픽셀"""
    return (x * K, z * K)


def price(w, h, mul):
    return int(round(w * h * LAND_PER_CELL * mul / 500.0)) * 500


# 후보 자리 (지도 칸 좌표: 왼쪽 위 x, z, 너비, 높이) — 지도에서 길과 빈터를 보고 고른 자리
ZONES = [
    ('A', '분수 광장 노점', (165, 278, 46, 46), (222, 160, 40),
     ['마을 한가운데 분수를 빙 둘러 작은 노점 8칸 (5x5)', '사람이 가장 자주 지나는 곳 · 지금의 길과 장식은 그대로', '좁다: 노점은 작고 수가 적다']),
    ('B', '동문 앞 시장 거리', (378, 178, 62, 52), (230, 90, 60),
     ['동문을 나서 부두로 가는 길가 빈터에 8x8 상점 토지 10칸', '부두(첫 도착)에서 마을로 들어오는 사람이 모두 지나간다', '넓어서 나중에 칸을 더 낼 수 있다 · 성벽 밖이라 마을 안은 그대로']),
    ('C', '부두 어시장', (536, 286, 40, 58), (60, 140, 230),
     ['처음 도착하는 부두(스폰) 곁 바닷가를 따라 6x6 노점 7칸', '새로 온 사람이 가장 먼저 보는 자리 · 물고기 · 낚시 가게에 어울림', '마을 가운데에서 멀다 · 평평한 땅이 적어 다져야 한다']),
    ('D', '남쪽 택지를 상점 토지로', (190, 370, 106, 32), (160, 80, 200),
     ['이미 있는 남쪽 택지 401~407 을 상점 토지로 돌린다 (새로 짓지 않음)', '지금 바로 쓸 수 있다 · 남문 곁이라 드나들기 좋다', '살림집 택지가 7칸 줄어든다 · 칸 크기가 제각각']),
]


def label(d, xy, text, fill, font, stroke=(20, 16, 10)):
    d.text(xy, text, font=font, fill=fill, stroke_width=3, stroke_fill=stroke)


def mask_cells(p):
    raw = zlib.decompress(base64.b64decode(p['mask']))
    out = []
    for z in range(p['h']):
        for x in range(p['w']):
            i = z * p['w'] + x
            if i // 8 < len(raw) and raw[i // 8] >> (i % 8) & 1:
                out.append((p['x0'] - M['x0'] + x, p['z0'] - M['z0'] + z))
    return out


# ---------------------------------------------------------------- 1) 지도 위 후보 자리
def candidates():
    im = big.copy().convert('RGBA')
    ov = Image.new('RGBA', im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    for p in plots['plots']:
        for (cx, cz) in mask_cells(p):
            x0, y0 = P(cx, cz)
            d.rectangle([x0, y0, x0 + K, y0 + K], fill=(255, 220, 60, 90))
    for key, name, (x, z, w, h), col, _ in ZONES:
        x0, y0 = P(x, z); x1, y1 = P(x + w, z + h)
        d.rectangle([x0, y0, x1, y1], fill=col + (70,), outline=col + (255,), width=4)
    im = Image.alpha_composite(im, ov)
    d = ImageDraw.Draw(im)
    for key, name, (x, z, w, h), col, _ in ZONES:
        x0, y0 = P(x, z)
        r = 22
        cx, cy = x0 - 8, y0 - 8
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col + (255,), outline=(255, 255, 255, 255), width=3)
        d.text((cx, cy), key, font=F(26, True), fill=(255, 255, 255), anchor='mm')
    sx, sy = P(*SPAWN)
    d.ellipse([sx - 9, sy - 9, sx + 9, sy + 9], outline=(255, 60, 200), width=4)
    label(d, (sx - 86, sy + 12), '부두 (처음 도착)', (255, 200, 240), F(18, True))
    # 범례
    W = im.width + 640
    out = Image.new('RGB', (W, im.height), (34, 26, 18))
    out.paste(im.convert('RGB'), (0, 0))
    d = ImageDraw.Draw(out)
    tx = im.width + 24
    d.text((tx, 22), 'W-4 상점 토지 — 자리 후보', font=F(30, True), fill=(255, 226, 150))
    d.text((tx, 64), '색칠한 네모가 상점 토지를 낼 자리입니다. 하나 또는 여럿을 고르면 됩니다.', font=F(16), fill=(220, 210, 190))
    y = 104
    for key, name, (x, z, w, h), col, notes in ZONES:
        d.ellipse([tx, y, tx + 34, y + 34], fill=col, outline=(255, 255, 255), width=2)
        d.text((tx + 17, y + 17), key, font=F(20, True), fill=(255, 255, 255), anchor='mm')
        d.text((tx + 46, y + 2), name, font=F(22, True), fill=(255, 240, 210))
        yy = y + 40
        for n in notes:
            d.text((tx + 46, yy), '· ' + n, font=F(16), fill=(214, 204, 186))
            yy += 26
        y = yy + 18
    d.text((tx, y + 6), '바탕: 마을 지도 (1칸 = 1블록, 1.5배) · 노란 칸은 지금의 살림집 택지', font=F(13), fill=(170, 160, 140))
    out.save(os.path.join(OUT, 'w4_1_map_candidates.png'))


# ---------------------------------------------------------------- 2) 자리별 배치 모습
def layouts():
    Z = 5   # 확대
    panels = []
    # A: 분수 둘레 노점 8칸 (5x5)
    def panel(zone, cells, extra=None):
        key, name, (x, z, w, h), col, notes = zone
        pad = 14
        crop = base.crop((x - pad, z - pad, x + w + pad, z + h + pad)).resize(((w + 2 * pad) * Z, (h + 2 * pad) * Z), Image.NEAREST).convert('RGBA')
        ov = Image.new('RGBA', crop.size, (0, 0, 0, 0)); d = ImageDraw.Draw(ov)
        for i, (cx, cz, cw, ch) in enumerate(cells):
            px, pz = (cx - x + pad) * Z, (cz - z + pad) * Z
            d.rectangle([px, pz, px + cw * Z, pz + ch * Z], fill=col + (110,), outline=(255, 255, 255, 255), width=2)
        crop = Image.alpha_composite(crop, ov); d = ImageDraw.Draw(crop)
        for i, (cx, cz, cw, ch) in enumerate(cells):
            px, pz = (cx - x + pad) * Z, (cz - z + pad) * Z
            d.text((px + cw * Z / 2, pz + ch * Z / 2), str(i + 1), font=F(16, True), fill=(255, 255, 255), anchor='mm', stroke_width=2, stroke_fill=(0, 0, 0))
        if extra: extra(d, x, z, pad)
        return crop
    a = ZONES[0]
    fx, fz = 188, 301   # 분수
    ring = []
    for (dx, dz) in ((-17, -17), (-3, -21), (11, -17), (15, -3), (11, 11), (-3, 15), (-17, 11), (-21, -3)):
        ring.append((fx + dx, fz + dz, 5, 5))
    panels.append((a, panel(a, ring), '노점 8칸 · 칸마다 5x5 (상점 블록 1개)', [price(5, 5, 1.5), price(5, 5, 2)]))
    b = ZONES[1]
    # 길(동문에서 북동쪽으로 휘는 길)을 사이에 두고 양쪽에 (길 위에는 놓지 않음)
    grid = [(381, 181, 8, 8), (391, 181, 8, 8), (401, 181, 8, 8), (424, 181, 8, 8), (433, 186, 8, 8),
            (381, 206, 8, 8), (391, 206, 8, 8), (401, 206, 8, 8), (411, 206, 8, 8), (431, 204, 8, 8)]
    panels.append((b, panel(b, grid), '상점 토지 10칸 · 칸마다 8x8 (상점 블록 2개까지) · 길가에 늘어섬', [price(8, 8, 1.5), price(8, 8, 2)]))
    c = ZONES[2]
    strip = [(544, 289 + i * 8, 6, 6) for i in range(7)]
    panels.append((c, panel(c, strip), '바닷가 노점 7칸 · 칸마다 6x6 (상점 블록 1개)', [price(6, 6, 1.5), price(6, 6, 2)]))
    dz = ZONES[3]
    south = [p for p in plots['plots'] if 401 <= p['no'] <= 407]
    lots = [(p['x0'] - M['x0'], p['z0'] - M['z0'], p['w'], p['h']) for p in south]
    panels.append((dz, panel(dz, lots), '지금의 택지 7칸 (401~407, 크기 제각각) · 칸마다 상점 블록 3개까지',
                   [50000 * 1.5, 50000 * 2]))
    W = 1500
    out = Image.new('RGB', (W, 2 * 560 + 90), (34, 26, 18))
    d = ImageDraw.Draw(out)
    d.text((24, 18), 'W-4 상점 토지 — 자리별 배치 모습 (안)', font=F(30, True), fill=(255, 226, 150))
    d.text((24, 58), '흰 테 = 상점 토지 한 칸. 값은 토지 문서 값(1칸 약 156 G)의 1.5배 / 2배 (고르기 전).', font=F(16), fill=(220, 210, 190))
    for i, (zone, img, desc, prices) in enumerate(panels):
        col, row = i % 2, i // 2
        ox, oy = 24 + col * 740, 96 + row * 560
        img.thumbnail((700, 430))
        out.paste(img.convert('RGB'), (ox, oy + 44))
        key, name = zone[0], zone[1]
        d.ellipse([ox, oy, ox + 34, oy + 34], fill=zone[3], outline=(255, 255, 255), width=2)
        d.text((ox + 17, oy + 17), key, font=F(20, True), fill=(255, 255, 255), anchor='mm')
        d.text((ox + 46, oy + 2), name, font=F(22, True), fill=(255, 240, 210))
        d.text((ox, oy + 44 + img.height + 8), desc, font=F(15), fill=(220, 210, 190))
        pr = '값 (한 칸): 1.5배 %s G  ·  2배 %s G' % (format(int(prices[0]), ','), format(int(prices[1]), ','))
        d.text((ox, oy + 44 + img.height + 32), pr, font=F(15, True), fill=(255, 214, 120))
    out.save(os.path.join(OUT, 'w4_2_layouts.png'))


# ---------------------------------------------------------------- 3) 사고 쓰는 흐름
def flow():
    W, H = 1500, 640
    out = Image.new('RGB', (W, H), (34, 26, 18))
    d = ImageDraw.Draw(out)
    d.text((24, 18), 'W-4 상점 토지 — 사고 쓰는 흐름 (안)', font=F(30, True), fill=(255, 226, 150))
    steps = [
        ('① 토지 상인', ['[상점 토지] 탭이 새로 생긴다', '지도에 상점 토지 칸이 보이고', '빈 칸 · 주인 · 값이 나온다'], (120, 90, 40)),
        ('② 칸 사기', ['칸을 눌러 [사기]', '값: 토지 문서 값의 1.5배 또는 2배', '한 사람 한 칸 (설정으로 늘림)'], (150, 110, 40)),
        ('③ 상점 차리기', ['개인 상점 블록은 이제', '상점 토지 안에만 놓인다', '(개인 토지 · 마을 땅에는 안 됨)'], (60, 120, 70)),
        ('④ 장사', ['물건 올리기 · 직원 고용은 지금처럼', '칸 안은 꾸밀 수 있다 (울타리 · 차양)', '필요 없으면 되팔기: 산 값의 절반'], (50, 100, 150)),
    ]
    bx, by, bw, bh, gap = 24, 90, 330, 200, 36
    for i, (title, lines, col) in enumerate(steps):
        x = bx + i * (bw + gap)
        d.rounded_rectangle([x, by, x + bw, by + bh], radius=14, fill=(58, 44, 30), outline=col, width=4)
        d.rounded_rectangle([x, by, x + bw, by + 52], radius=14, fill=col)
        d.text((x + 18, by + 10), title, font=F(24, True), fill=(255, 250, 235))
        y = by + 72
        for l in lines:
            d.text((x + 18, y), l, font=F(17), fill=(232, 222, 204))
            y += 34
        if i < len(steps) - 1:
            ax = x + bw + 4
            d.polygon([(ax, by + bh / 2 - 14), (ax + gap - 8, by + bh / 2), (ax, by + bh / 2 + 14)], fill=(255, 214, 120))
    # 토지 상인 화면 모양 (간단히)
    sx, sy = 24, 316
    d.rounded_rectangle([sx, sy, sx + 720, sy + 300], radius=10, fill=(236, 222, 190), outline=(120, 90, 40), width=4)
    d.text((sx + 20, sy + 12), '토지 상인 — 상점 토지', font=F(22, True), fill=(60, 40, 20))
    tabs = ['토지 문서', '마을 택지', '상점 토지']
    for i, t in enumerate(tabs):
        tx = sx + 20 + i * 130
        d.rounded_rectangle([tx, sy + 50, tx + 120, sy + 80], radius=6, fill=(196, 160, 100) if i == 2 else (220, 204, 170), outline=(120, 90, 40), width=2)
        d.text((tx + 60, sy + 65), t, font=F(15, True), fill=(50, 32, 14), anchor='mm')
    rows = [('B-1', '8x8', '비어 있음', price(8, 8, 1.5)), ('B-2', '8x8', '주인: 루시의 빵집', None), ('B-3', '8x8', '비어 있음', price(8, 8, 1.5)),
            ('B-4', '8x8', '주인: 낚시꾼 오스카', None)]
    for i, (no, size, who, pr) in enumerate(rows):
        ry = sy + 96 + i * 46
        d.rectangle([sx + 20, ry, sx + 470, ry + 40], fill=(246, 236, 212), outline=(170, 140, 90), width=1)
        d.text((sx + 32, ry + 8), no, font=F(17, True), fill=(60, 40, 20))
        d.text((sx + 96, ry + 8), size, font=F(16), fill=(90, 70, 40))
        d.text((sx + 160, ry + 8), who, font=F(16), fill=(30, 110, 60) if pr else (120, 90, 60))
        if pr:
            d.text((sx + 340, ry + 8), format(pr, ',') + ' G', font=F(16, True), fill=(150, 90, 10))
    d.rounded_rectangle([sx + 490, sy + 96, sx + 700, sy + 280], radius=8, fill=(214, 196, 160), outline=(120, 90, 40), width=2)
    d.text((sx + 595, sy + 120), '작은 지도', font=F(15, True), fill=(80, 60, 30), anchor='mm')
    for r in range(2):
        for c in range(4):
            cx, cy = sx + 512 + c * 44, sy + 150 + r * 56
            filled = (r * 4 + c) in (1, 3, 6)
            d.rectangle([cx, cy, cx + 36, cy + 36], fill=(150, 120, 80) if filled else (120, 190, 110), outline=(60, 40, 20), width=2)
    d.text((sx + 595, sy + 262), '초록 = 빈 칸', font=F(13), fill=(60, 90, 40), anchor='mm')
    # 정할 것
    qx = 780
    d.text((qx, 326), '정해 주실 것', font=F(24, True), fill=(255, 226, 150))
    qs = ['1. 자리: A · B · C · D 중 (여럿도 됨)',
          '2. 값: 토지 문서 값의 1.5배 또는 2배',
          '3. 한 사람이 가질 수 있는 상점 토지 수 (기본 1)',
          '4. 개인 토지에 이미 놓인 상점 블록: 그대로 둘지 / 옮기게 할지',
          '5. 주마다 내는 자릿세가 있을지 (없음 · 약간)']
    y = 366
    for q in qs:
        d.text((qx, y), q, font=F(17), fill=(232, 222, 204))
        y += 40
    out.save(os.path.join(OUT, 'w4_3_flow.png'))


candidates()
layouts()
flow()
print('saved to', OUT)
