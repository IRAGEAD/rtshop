#!/usr/bin/env python3
"""강화 정보 창의 그림 (3.49 디자인 「하 · 세로 한 장」 + 테두리 놋쇠 장식 살짝): textures/gui/enhance/*.png
모루 블록과 같은 재료 — 점판암 바탕, 팔각 강철 테, 놋쇠 상감. 글자·숫자·아이템은 게임이 그 위에 그린다 (client/EnhancePanel).
그림은 화면 1픽셀에 2픽셀(2배)로 만든다. 자리(화면 픽셀)는 아래 LAYOUT 과 EnhancePanel 의 상수가 같아야 한다."""
import os, sys, math
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, os.path.join(HERE, 'mock'))
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import panel_mock7 as M
from panel_mock7 import Forge, S, RUNES, MAXLV, BRASS, STEEL
from panel_mock4 import stone, steel
OUT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'textures', 'gui', 'enhance')
W, H = 204, 280
MEDAL = (W / 2, 58, 27)          # 장비 메달의 가운데와 반지름
RUNE = (14, 108, W - 14, 15)     # 룬 띠: x0, y, x1, 높이
def down(im, k=2): return im.resize((im.width * k // M.K, im.height * k // M.K), Image.LANCZOS)

def background():
    s = Forge(W, H); s.frame()
    # 테두리의 놋쇠 장식 (살짝): 네 귀에 덩굴 한 가닥과 잎 하나, 위아래 변 가운데에 마름모
    def orn(d):
        for (x, y, sx, sy) in ((9.5, 9.5, 1, 1), (W - 9.5, 9.5, -1, 1), (9.5, H - 9.5, 1, -1), (W - 9.5, H - 9.5, -1, -1)):
            a0 = {(1, 1): 225, (-1, 1): 315, (1, -1): 135, (-1, -1): 45}[(sx, sy)]
            s.scroll(d, x + sx * 5, y + sy * 5, 4.6, a0, 1.0, 1.3, .3, 1 if sx * sy > 0 else -1)
            s.leaf(d, x + sx * 10.5, y + sy * 1.2, 0 if sx > 0 else 180, 6.5, 1.3); s.leaf(d, x + sx * 1.2, y + sy * 10.5, 90 if sy > 0 else 270, 6.5, 1.3)
        for y in (9.2, H - 9.2):
            d.polygon([(S(W / 2), S(y - 2.2)), (S(W / 2 + 5), S(y)), (S(W / 2), S(y + 2.2)), (S(W / 2 - 5), S(y))], fill=255)
            for sx in (-1, 1): d.line([(S(W / 2 + sx * 7), S(y)), (S(W / 2 + sx * 22), S(y))], fill=255, width=max(1, S(.55)))
    s.brass(orn)
    cx, cy, R = MEDAL; s.medal(cx, cy, R)
    # 메달 속의 칼 그림은 지운다(게임이 실제 장비를 그린다): 메달을 다시 깔고 불빛만 남긴다
    m = s.mask(); s.oct(ImageDraw.Draw(m), (cx - R + 4, cy - R + 4, cx + R - 4, cy + R - 4), (R - 4) * .58, fill=255)
    s.im.paste(stone(S(2 * R), S(2 * R), (44, 48, 58), seed=8, contrast=10), (S(cx - R + 4), S(cy - R + 4)), m.crop((S(cx - R + 4), S(cy - R + 4), S(cx - R + 4) + S(2 * R), S(cy - R + 4) + S(2 * R)))); s.d = ImageDraw.Draw(s.im, 'RGBA')
    s.gl = Image.new('RGB', s.im.size, (0, 0, 0)); s.dg = ImageDraw.Draw(s.gl); s.dg.ellipse([S(cx - 13), S(cy - 12), S(cx + 13), S(cy + 14)], fill=(110, 46, 12))
    x0, y, x1, h = RUNE; s.runeband(x0, x1, y, h=h, lv=-1)
    return down(s.done())

def runes():
    """룬 열한 자 x 두 모습(켜짐 · 다음 단계): 칸 하나 16x15 (화면 픽셀). 바탕은 투명.
    3.78 (사용자 "강화 정보 창의 룬이 채워질 때 글자 테두리를 벗어나는 문제"): 예전에는 바탕 띠에 새긴 홈(선 굵기 S(.8) = 3)보다 굵은 선(4 · 5)에
    바깥 빛무리까지 둘러, 켜진 룬이 홈 밖으로 번진 굵은 덩어리로 보였다 (줄일 때 LANCZOS 가 테두리에 붉은 · 노란 띠도 만들었다).
    이제 홈과 같은 자리 · 같은 굵기(3)의 선으로만 채우고(다음 단계는 더 밝은 색), 빛무리 없이, BOX 로 줄인다 → 빛이 홈 안에만 든다.
    (게임에서 더 굵어 보인 까닭은 그릴 때 섞기(blend)가 꺼져 반투명 빛무리가 불투명하게 찍힌 것: EnhancePanel 이 룬을 그리기 전에 섞기를 켠다.)"""
    cw, ch = 16, 15; K = M.K
    im = Image.new('RGBA', (cw * MAXLV * K, ch * 2 * K), (0, 0, 0, 0))
    for row, (core, width) in enumerate((((255, 172, 78), 3), ((255, 238, 184), 3))):
        layer = Image.new('L', im.size, 0); d = ImageDraw.Draw(layer)
        for i in range(MAXLV):
            u = 1.9; ox, oy = i * cw + cw / 2 - u, row * ch + ch / 2 - 2 * u
            for (a, b, c, e) in RUNES[i]: d.line([(S(ox + a * u), S(oy + b * u)), (S(ox + c * u), S(oy + e * u))], fill=255, width=width)
        c = Image.new('RGBA', im.size, core + (255,)); c.putalpha(layer); im = Image.alpha_composite(im, c)
    return im.resize((im.width * 2 // K, im.height * 2 // K), Image.BOX)

def plate():
    """연한 금빛 판 (가운데가 늘어나는 9칸 그림): 48x48 화면 픽셀, 모서리 6."""
    s = Forge(48, 48); s.im = Image.new('RGB', s.im.size, (0, 0, 0)); s.d = ImageDraw.Draw(s.im, 'RGBA')
    m = s.mask(); s.oct(ImageDraw.Draw(m), (1, 1, 47, 47), 2.5, fill=255)
    t = steel(S(48), S(48), (247, 237, 206), seed=5); s.im.paste(t, (0, 0), m); s.d = ImageDraw.Draw(s.im, 'RGBA')
    s.d.line([(S(3.5), S(1) + 1), (S(44.5), S(1) + 1)], fill=(255, 248, 220, 200), width=1); s.d.line([(S(3.5), S(47) - 2), (S(44.5), S(47) - 2)], fill=(128, 98, 44, 160), width=1)
    s.brass(lambda d: s.oct(d, (1, 1, 47, 47), 2.5, outline=255, width=max(1, S(.6))), shadow=False)
    out = s.im.convert('RGBA'); a = Image.new('L', s.im.size, 0); s.oct(ImageDraw.Draw(a), (.6, .6, 47.4, 47.4), 2.5, fill=255); out.putalpha(a)
    return down(out)

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    background().save(os.path.join(OUT, 'panel.png')); runes().save(os.path.join(OUT, 'runes.png')); plate().save(os.path.join(OUT, 'plate.png'))
    for n in ('panel', 'runes', 'plate'): print(n, Image.open(os.path.join(OUT, n + '.png')).size)
