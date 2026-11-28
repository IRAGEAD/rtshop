# W-4 상점 토지 최종 자리 그림 (3.79, 11:21 기존 가판대 자리): python3 tools/mock/w4_final_market.py <출력 폴더>
#   바탕: 마을 지도(castle_map.png, 1픽셀 = 1블록) · 장터 칸: rtshop_defaults/shop_lots.json
import json, os, sys
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
lots = json.load(open(os.path.join(ROOT, 'src/main/resources/rtshop_defaults/shop_lots.json'), encoding='utf-8'))['lots']
M = plots['map']
base = Image.open(os.path.join(ROOT, 'src/main/resources/assets/rtshop/textures/gui/castle_map.png')).convert('RGB').crop((0, 0, M['w'], M['h']))


def mx(x):
    return x - M['x0']


def mz(z):
    return z - M['z0']


# 1) 마을 전체에서 어디인지 (작게)
K1 = 1.2
whole = base.resize((int(M['w'] * K1), int(M['h'] * K1)), Image.NEAREST).convert('RGBA')
ov = Image.new('RGBA', whole.size, (0, 0, 0, 0))
d = ImageDraw.Draw(ov)
x0 = min(mx(t['x0']) for t in lots); z0 = min(mz(t['z0']) for t in lots)
x1 = max(mx(t['x0']) + t['w'] for t in lots); z1 = max(mz(t['z0']) + t['h'] for t in lots)
d.rectangle([x0 * K1 - 3, z0 * K1 - 3, x1 * K1 + 3, z1 * K1 + 3], outline=(255, 200, 40, 255), width=4, fill=(255, 200, 40, 90))
whole = Image.alpha_composite(whole, ov)

# 2) 광장 크게
ZX0, ZZ0, ZX1, ZZ1 = 266, 296, 336, 406
K2 = 7
zoom = base.crop((ZX0, ZZ0, ZX1, ZZ1)).resize(((ZX1 - ZX0) * K2, (ZZ1 - ZZ0) * K2), Image.NEAREST).convert('RGBA')
ov = Image.new('RGBA', zoom.size, (0, 0, 0, 0))
d = ImageDraw.Draw(ov)
for t in lots:
    a = ((mx(t['x0']) - ZX0) * K2, (mz(t['z0']) - ZZ0) * K2)
    b = ((mx(t['x0']) + t['w'] - ZX0) * K2 - 1, (mz(t['z0']) + t['h'] - ZZ0) * K2 - 1)
    # 가판대가 보이게 안은 옅게, 테두리는 굵게
    d.rectangle([a, b], fill=(255, 210, 60, 45), outline=(255, 220, 60, 255), width=4)
zoom = Image.alpha_composite(zoom, ov)
d = ImageDraw.Draw(zoom)
for t in lots:
    # 번호는 칸 바깥 위쪽(또는 왼쪽)에 — 가판대 그림을 가리지 않게
    cx = (mx(t['x0']) + t['w'] / 2 - ZX0) * K2
    top = (mz(t['z0']) - ZZ0) * K2
    n = t['name'].split()[-1]
    d.text((cx, top - 14), n, font=F(24, True), fill=(40, 26, 10), anchor='mm', stroke_width=4, stroke_fill=(255, 225, 90))
# 길 · 문 표시
def tag(x, z, text, col):
    px, pz = (x - ZX0) * K2, (z - ZZ0) * K2
    d.text((px, pz), text, font=F(20, True), fill=col, anchor='mm', stroke_width=4, stroke_fill=(20, 16, 10))
tag(308, 401, '남동문 (마을 입구)', (255, 255, 255))
tag(280, 300, '↑ 중앙 호수', (140, 200, 255))
tag(301, 356, '가운데 큰길은 그대로', (230, 230, 230))

# 3) 한 장으로 (글은 폭에 맞춰 줄을 나눔)
def wrap(text, font, width):
    lines, cur = [], ''
    for word in text.split(' '):
        t = (cur + ' ' + word).strip()
        if font.getlength(t) <= width or not cur:
            cur = t
        else:
            lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    return lines


def jround(v):
    import math
    return int(math.floor(v + 0.5))


def price(area):
    return jround(40000 / 256 * area * 2 / 10) * 10


W = whole.width + zoom.width + 60
head = ['광장에 이미 있는 가판대(줄무늬 · 체크무늬 차양) 10곳이 그대로 상점 토지 10칸이 됩니다. 노란 테두리 = 칸 (가판대의 차양 · 기둥 · 곁의 수레를 덮는 네모), 번호 = 가판대 1~10 (북 → 남).',
        '가판대 건물은 그대로 넘기고, 산 사람이 그 안에 진열대를 놓거나 고쳐 짓습니다. 가운데 큰길 · 길은 그대로입니다.']
ps = sorted((price(t['w'] * t['h']), t['name'].split()[-1]) for t in lots)
lo, hi = ps[0], ps[-1]
info = [f'값: 가판대 크기만큼 (토지 문서 값의 2배 = 블록당 312.5 G) — 가장 작은 {lo[1]}번 {lo[0]:,} G ~ 가장 큰 {hi[1]}번 {hi[0]:,} G · 한 사람이 여러 칸 가능',
        '자릿세: 주마다 산 값의 15% (예금 → 지갑, 산 주는 없음) + 그 칸 진열대에서 팔린 금액의 5% (팔 때 떼어 감)',
        '자릿세를 못 내면 밀리고(4주 치까지) 그 칸의 진열대가 문을 닫음 · 진열대는 상점 토지 안에만 새로 · 개인 토지에 이미 놓인 진열대는 그대로']
fh, fi = F(22), F(22)
hl = [l for t in head for l in wrap(t, fh, W - 40)]
il = [l for t in info for l in wrap('· ' + t, fi, W - 40)]
top = 76 + 32 * len(hl) + 16
H = top + max(whole.height, zoom.height) + 20 + 34 * len(il) + 20
im = Image.new('RGB', (W, H), (36, 26, 18))
im.paste(whole.convert('RGB'), (20, top))
im.paste(zoom.convert('RGB'), (whole.width + 40, top))
d = ImageDraw.Draw(im)
d.text((20, 18), 'W-4 상점 토지 — 남동문 광장의 기존 가판대 10곳 = 상점 토지 10칸', font=F(40, True), fill=(255, 216, 102))
y = 76
for i, line in enumerate(hl):
    d.text((20, y), line, font=fh, fill=(235, 225, 205) if i < len(hl) - 1 else (200, 190, 170))
    y += 32
y = top + max(whole.height, zoom.height) + 20
for line in il:
    d.text((20, y), line, font=fi, fill=(235, 225, 205))
    y += 34
im.save(os.path.join(OUT, 'w4_6_stall_lots.png'))
print('saved', os.path.join(OUT, 'w4_6_stall_lots.png'), im.size)
