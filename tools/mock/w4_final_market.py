# W-4 상점 토지 최종 자리 그림 (3.79): python3 tools/mock/w4_final_market.py <출력 폴더>
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
    d.rectangle([a, b], fill=(255, 210, 60, 110), outline=(255, 230, 120, 255), width=3)
zoom = Image.alpha_composite(zoom, ov)
d = ImageDraw.Draw(zoom)
for t in lots:
    cx = (mx(t['x0']) + t['w'] / 2 - ZX0) * K2
    cz = (mz(t['z0']) + t['h'] / 2 - ZZ0) * K2
    n = t['name'].split()[-1]
    d.text((cx, cz), n, font=F(22, True), fill=(40, 26, 10), anchor='mm', stroke_width=3, stroke_fill=(255, 240, 190))
# 길 · 문 표시
def tag(x, z, text, col):
    px, pz = (x - ZX0) * K2, (z - ZZ0) * K2
    d.text((px, pz), text, font=F(20, True), fill=col, anchor='mm', stroke_width=4, stroke_fill=(20, 16, 10))
tag(308, 401, '남동문 (마을 입구)', (255, 255, 255))
tag(280, 300, '↑ 중앙 호수', (140, 200, 255))
tag(301, 352, '가운데 큰길은 비움', (230, 230, 230))

# 3) 한 장으로
W = whole.width + zoom.width + 60
H = max(whole.height, zoom.height) + 290
im = Image.new('RGB', (W, H), (36, 26, 18))
im.paste(whole.convert('RGB'), (20, 150))
im.paste(zoom.convert('RGB'), (whole.width + 40, 150))
d = ImageDraw.Draw(im)
d.text((20, 18), 'W-4 상점 토지 — 남동문 안 가판대 광장의 장터 (새 자리)', font=F(40, True), fill=(255, 216, 102))
d.text((20, 76), '남동쪽 입구 안, 가판대가 늘어선 넓은 광장의 가장자리에 5×5 칸 9개를 흩어 놓았습니다 (서쪽 1~6, 동쪽 7~9, 서로 4칸 이상 떨어짐).', font=F(22), fill=(235, 225, 205))
d.text((20, 108), '모든 칸은 포장된 길 위에만 있고, 가판대 · 건물 · 등불과 한 칸 이상 떨어져 있습니다. 가운데 큰길(호수 ~ 남동문)은 비워 두었습니다.', font=F(22), fill=(200, 190, 170))
y = 150 + max(whole.height, zoom.height) + 20
info = ['값: 한 칸 7,810 G (토지 문서 값의 2배 = 블록당 312.5 G × 25칸) · 한 사람이 여러 칸 가능 (제한 없음)',
        '자릿세: 주마다 산 값의 15% (약 1,170 G, 예금 → 지갑, 산 주는 없음) + 그 칸 진열대에서 팔린 금액의 5% (팔 때 떼어 감)',
        '자릿세를 못 내면 밀리고(4주 치까지) 그 칸의 진열대가 문을 닫음 · 진열대는 상점 토지 안에만 새로 · 개인 토지에 이미 놓인 진열대는 그대로']
for line in info:
    d.text((20, y), '· ' + line, font=F(22), fill=(235, 225, 205))
    y += 34
im.save(os.path.join(OUT, 'w4_5_se_gate_market.png'))
print('saved', os.path.join(OUT, 'w4_5_se_gate_market.png'), im.size)
