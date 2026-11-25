#!/usr/bin/env python3
"""강화 모루 블록 (3.49 디자인: 단조 강철 모루 + 놋쇠 상감 / 점판암 팔각 받침 + 룬 띠).
모델(models/block/enhance_anvil.json)과 질감(textures/block/enh_*.png, 64x64 = 모델 1픽셀에 4텍셀)을 만든다.
확정 그림: docs/design/enhance_anvil_block.png. 치수는 tools/mock/anvil_final.py 와 같다.
면의 UV 는 적지 않는다(마인크래프트가 상자의 자리로 정한다) — 그래서 질감은 "블록 한 칸을 옆·위에서 본 그림"으로 그린다.
룬 띠·옆면 보석의 빛은 모델이 아니라 client/EnhanceAnvilGlow 가 그 위에 덧그린다 (enh_glow_side.png, enh_glow_anvil.png)."""
import json, os, random
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'rtshop')
T = 64; Q = T // 16      # 텍셀 / 모델 픽셀

def tile(base, spread, seed, cell=2, speck=0.0):
    r = random.Random(seed); im = Image.new('RGBA', (T, T)); px = im.load()
    for y in range(0, T, cell):
        for x in range(0, T, cell):
            d = r.randint(-spread, spread); c = tuple(max(0, min(255, v + d)) for v in base) + (255,)
            for yy in range(cell):
                for xx in range(cell): px[x + xx, y + yy] = c
    for _ in range(int(T * T * speck)):
        x, y = r.randrange(T), r.randrange(T); d = r.choice((-18, 14)); px[x, y] = tuple(max(0, min(255, v + d)) for v in px[x, y][:3]) + (255,)
    return im
def shade(c, f): return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)

SLATE, SLATE_L, SLATE_D = (62, 66, 76), (86, 90, 102), (44, 48, 58)
FORGED, FORGED_L, FORGED_D = (56, 58, 66), (118, 122, 134), (34, 36, 42)
BRASS, BRASS_L, BRASS_D = (190, 150, 72), (232, 198, 112), (136, 100, 44)
EMBER, EMBER_H = (255, 130, 44), (255, 214, 130)

# 룬 여섯 자 (5x5 텍셀). 받침 아랫단의 옆면 가운데 판(모델 x 2..14)에 2픽셀 간격으로 놓인다.
RUNES = ["#...#|#..#.|###..|#..#.|#...#", "#####|..#..|..#..|.#.#.|#...#", "#..#.|#.#..|##...|#.#..|#..##", ".###.|#...#|#.#.#|#...#|.###.", "#...#|##..#|#.#.#|#..##|#...#", "..#..|.###.|#.#.#|..#..|..#.."]
RUNE_Y0 = 0.75      # 룬의 아랫변 (모델 픽셀)
def rune_texels():
    out = []
    for i, g in enumerate(RUNES):
        x0 = int((2.5 + i * 2.0) * Q)
        for j, row in enumerate(g.split('|')):
            for k, ch in enumerate(row):
                if ch == '#': out.append((x0 + k, T - int(RUNE_Y0 * Q) - 5 + j))
    return out

def make_textures():
    out = {}
    out['enh_slate'] = tile(SLATE, 5, 1, speck=.01)
    out['enh_slate_top'] = tile(SLATE_L, 5, 2, speck=.01)
    out['enh_slate_dark'] = tile(SLATE_D, 4, 3)
    out['enh_forged'] = tile(FORGED, 5, 4, speck=.006)
    out['enh_forged_dark'] = tile(FORGED_D, 4, 5)
    out['enh_brass'] = tile(BRASS, 9, 6)
    out['enh_brass_light'] = tile(BRASS_L, 8, 7)
    # 받침 아랫단 옆면: 점판암 + 윗가장자리 밝은 줄 + 아랫가장자리 어두운 줄 + 새긴 룬 (꺼진 모습)
    side = tile(SLATE, 5, 8, speck=.01); px = side.load()
    for x in range(T):
        for y in range(T - 3 * Q, T - 3 * Q + 2): px[x, y] = shade(SLATE_L, 1 + (x % 5 - 2) * .01)
        for y in range(T - 2, T): px[x, y] = shade(SLATE_D, 1.0)
    for (x, y) in rune_texels():
        px[x, y] = shade(SLATE_D, .62)
        if px[x + 1, y + 1] != px[x, y] and (x + 1, y + 1) not in set(rune_texels()): px[x + 1, y + 1] = shade(SLATE_L, 1.05)
    out['enh_slate_runes'] = side
    glow = Image.new('RGBA', (T, T), (0, 0, 0, 0)); gp = glow.load()
    for (x, y) in rune_texels(): gp[x, y] = EMBER + (255,)
    for (x, y) in rune_texels():
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if 0 <= x + dx < T and 0 <= y + dy < T and gp[x + dx, y + dy][3] == 0: gp[x + dx, y + dy] = EMBER + (70,)
    out['enh_glow_side'] = glow
    # 모루 옆면(긴 쪽): 단조 강철 + 놋쇠 상감 두 줄 + 마름모 무늬 + 가운데 보석 자리 (꺼진 모습)
    a = tile(FORGED, 5, 9, speck=.006); px = a.load()
    def row(y_model, h, col):
        for x in range(T):
            for y in range(T - int(y_model * Q) - h, T - int(y_model * Q)): px[x, y] = shade(col, 1 + ((x * 7) % 5 - 2) * .015)
    row(11.5, 2, FORGED_D); row(14.75, 1, BRASS_L); row(12.0, 1, BRASS)
    cy = T - int(13.4 * Q)
    for k in range(-3, 4):
        if k == 0: continue
        cx = T // 2 + k * 7
        for dx in range(-3, 4):
            for dy in range(-3, 4):
                if 2 <= abs(dx) + abs(dy) <= 3 and 16 <= cx + dx < 48: px[cx + dx, cy + dy] = shade(BRASS, 1.0)
    for dx in range(-4, 5):
        for dy in range(-4, 5):
            d = abs(dx) + abs(dy)
            if d <= 4: px[T // 2 + dx, cy + dy] = shade(BRASS_L, 1.0) if d == 4 else shade(BRASS_D, .7 if d > 1 else .9)
    out['enh_anvil_side'] = a
    g2 = Image.new('RGBA', (T, T), (0, 0, 0, 0)); gp = g2.load()
    for dx in range(-5, 6):
        for dy in range(-5, 6):
            d = abs(dx) + abs(dy)
            if d <= 3: gp[T // 2 + dx, cy + dy] = (EMBER_H if d <= 1 else EMBER) + (255,)
            elif d <= 5: gp[T // 2 + dx, cy + dy] = EMBER + (90 if d == 4 else 40,)
    out['enh_glow_anvil'] = g2
    # 모루 윗면: 닳아 밝아진 강철 + 가는 놋쇠 테 (모델 x 3.5..12.5, z 4.5..12.25)
    t = tile(FORGED_L, 5, 10, speck=.008); px = t.load()
    x0, x1, z0, z1 = int(3.5 * Q), int(12.5 * Q) - 1, int(4.5 * Q), int(12.25 * Q) - 1
    for x in range(x0, x1 + 1): px[x, z0] = shade(BRASS, 1); px[x, z1] = shade(BRASS, .92)
    for z in range(z0, z1 + 1): px[x0, z] = shade(BRASS, 1); px[x1, z] = shade(BRASS, .92)
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)): px[x, z] = shade(BRASS_L, 1)
    out['enh_anvil_top'] = t
    for n, im in out.items(): im.save(os.path.join(ASSETS, 'textures', 'block', n + '.png'))
    return out

# ------------------------------------------------------------------ 모델
EL = []
def box(x0, y0, z0, x1, y1, z1, side, top=None, bottom=None, faces='nsewud', over=None):
    """over: {'east': '#tex', ...} 로 면마다 질감을 바꾼다"""
    f = {}
    names = {'n': 'north', 's': 'south', 'e': 'east', 'w': 'west', 'u': 'up', 'd': 'down'}
    for k in faces:
        nm = names[k]; tex = (top or side) if k == 'u' else ((bottom or side) if k == 'd' else side)
        if over and nm in over: tex = over[nm]
        f[nm] = {'texture': '#' + tex}
    EL.append({'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': f})
def octa(x0, y0, z0, x1, y1, z1, c, side, top=None, center_side=None):
    """모서리를 c 만큼 깎은 팔각 단: 가운데 + 네 변의 판 + 네 귀의 작은 상자 (겹치지 않게)"""
    cs = center_side or side
    box(x0 + c, y0, z0 + c, x1 - c, y1, z1 - c, side, top, faces='ud')
    box(x0 + c, y0, z0, x1 - c, y1, z0 + c, cs, top, faces='nud' + 'ew'); box(x0 + c, y0, z1 - c, x1 - c, y1, z1, cs, top, faces='sud' + 'ew')
    box(x0, y0, z0 + c, x0 + c, y1, z1 - c, cs, top, faces='wud' + 'ns'); box(x1 - c, y0, z0 + c, x1, y1, z1 - c, cs, top, faces='eud' + 'ns')
    h = c / 2
    for (ax, az) in ((x0 + h, z0 + h), (x1 - c, z0 + h), (x0 + h, z1 - c), (x1 - c, z1 - c)): box(ax, y0, az, ax + h, y1, az + h, side, top)

def build():
    S, ST, SD, F, FD, B, BL = 'slate', 'slate_top', 'slate_dark', 'forged', 'forged_dark', 'brass', 'brass_light'
    # 받침: 점판암 팔각 단 두 층. 아랫단 옆면 가운데 판에 룬 띠.
    octa(0.5, 0, 0.5, 15.5, 3, 15.5, 1.5, S, ST, center_side='slate_runes')
    octa(1.5, 3, 1.5, 14.5, 4.25, 14.5, 1.25, S, ST)
    # 윗단 가장자리의 놋쇠 띠 한 줄 (가운데는 밝은 점판암)
    octa(1.5, 4.25, 1.5, 14.5, 4.5, 14.5, 1.25, B, BL)
    box(2.0, 4.5, 2.0, 14.0, 4.55, 14.0, ST, ST, faces='nsewu')
    for (x, z) in ((2.6, 2.6), (13.4, 2.6), (2.6, 13.4), (13.4, 13.4)): box(x - .5, 4.55, z - .5, x + .5, 5.15, z + .5, BL)        # 네 귀의 놋쇠 못
    # 모루: 발 → 잘록한 허리 → 머리(뿔·굽)
    box(3.5, 4.55, 3, 12.5, 6, 13, FD); box(4.5, 6, 4, 11.5, 7, 12, F)
    for i, w in enumerate((2.0, 1.75, 1.5, 1.5, 1.5, 1.75, 2.0, 2.5, 3.0)): box(8 - w, 7 + i * .5, 8 - w - 1.25, 8 + w, 7.5 + i * .5, 8 + w + 1.25, F)
    box(5.1, 8.5, 3.85, 10.9, 9, 12.15, B, faces='nsew'); box(4.7, 10.75, 4.2, 11.3, 11.25, 12.3, B, FD, faces='nsew')                         # 허리의 놋쇠 띠 두 줄
    box(3, 11.5, 4, 13, 16, 12.5, F, 'anvil_top', FD, over={'east': 'anvil_side', 'west': 'anvil_side'})
    box(3.5, 13, 12.5, 12.5, 16, 15.5, F, 'anvil_top', FD); box(4, 14, 15.5, 12, 16, 16, F, 'anvil_top', FD)                    # 굽
    for i in range(8):                                                                                                         # 뿔
        t = i / 8.0; box(round(3.5 + t * 3.6, 3), round(12 + t * 2.6, 3), 4 - (i + 1) * .5, round(12.5 - t * 3.6, 3), 16, 4 - i * .5, F, 'anvil_top', FD)
    tex = {k: 'rtshop:block/enh_' + k for k in ('slate', 'slate_top', 'slate_dark', 'slate_runes', 'forged', 'forged_dark', 'brass', 'brass_light', 'anvil_side', 'anvil_top')}
    tex['particle'] = 'rtshop:block/enh_forged'
    model = {'parent': 'minecraft:block/block', 'textures': tex, 'elements': EL,
             'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
                         'fixed': {'rotation': [0, 90, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]}}}
    with open(os.path.join(ASSETS, 'models', 'block', 'enhance_anvil.json'), 'w', encoding='utf-8') as f:
        json.dump(model, f, separators=(',', ':')); f.write('\n')
    return len(EL)

if __name__ == '__main__':
    make_textures(); print('elements', build())
