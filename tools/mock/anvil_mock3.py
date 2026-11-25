import math, sys, time
from PIL import Image, ImageDraw, ImageFont
from vox import *

STEEL = mat((70, 76, 90)); STEEL_L = mat((120, 128, 146), 5); STEEL_D = mat((40, 44, 56), 5); STEEL_P = mat((150, 158, 176), 5)
GOLD = mat((214, 172, 70), 10); GOLD_L = mat((246, 214, 120), 8); GOLD_D = mat((150, 112, 40), 8)
NAVY = mat((30, 40, 80), 5); NAVY_L = mat((50, 66, 120), 5); NAVY_D = mat((18, 24, 50), 4); SILVER = mat((182, 190, 206), 7)
BLUE = glow((110, 190, 255)); BLUE_H = glow((214, 240, 255)); STAR = glow((255, 222, 130)); STAR_H = glow((255, 246, 200))
BLACK = mat((34, 30, 34), 5); BLACK_L = mat((58, 52, 56), 6); SCALE = mat((70, 30, 30), 7); SCALE_L = mat((110, 44, 38), 7); BONE = mat((222, 210, 180), 7)
EMBER = glow((255, 120, 36)); EMBER_H = glow((255, 214, 120)); LAVA = glow((255, 80, 20))
MARBLE = mat((226, 224, 220), 6); MARBLE_D = mat((184, 182, 184), 6); RED = glow((255, 70, 80)); CRIMSON = mat((150, 30, 40), 7); CRIMSON_D = mat((104, 20, 30), 6)
WOOD = mat((120, 84, 48), 8)

def anvil(g, M, ML, MD, y0=6.0, x0=3.0, x1=13.0):
    c = 8.0
    g.box(x0 + .5, y0 - 1.5, 3, x1 - .5, y0, 13, MD); g.box(x0 + 1.5, y0, 4, x1 - 1.5, y0 + 1, 12, M)
    for i, w in enumerate((2.0, 1.75, 1.5, 1.5, 1.5, 1.75, 2.0, 2.5, 3.0)):   # curved waist
        yy = y0 + 1 + i * 0.5
        g.box(c - w, yy, c - w - 1.25, c + w, yy + 0.5, c + w + 1.25, M)
    yt = y0 + 5.5
    g.box(x0, yt, 4, x1, 16, 12.5, M)
    g.box(x0 + .5, yt + 1.5, 12.5, x1 - .5, 16, 15.5, M); g.box(x0 + 1, yt + 2.5, 15.5, x1 - 1, 16, 16, M)    # heel
    for i in range(8):   # tapered horn
        t = i / 8.0
        g.box(x0 + .5 + t * 3.6, yt + .5 + t * 2.6, 4 - (i + 1) * .5, x1 - .5 - t * 3.6, 16, 4 - i * .5, M)
    g.surf(lambda x, y, z: y == 16 * U - 1, ML)

def design_g():   # 별의 단조대
    g = Grid()
    g.cham(0.5, 0, 0.5, 15.5, 1.5, 15.5, 2.5, NAVY_D); g.cham(0.5, 1.0, 0.5, 15.5, 1.5, 15.5, 2.5, SILVER); g.cham(1, 1.0, 1, 15, 1.5, 15, 2.5, NAVY)
    g.cham(1.5, 1.5, 1.5, 14.5, 3.25, 14.5, 2.25, NAVY); g.cham(1.5, 2.75, 1.5, 14.5, 3.25, 14.5, 2.25, GOLD); g.cham(2, 2.75, 2, 14, 3.25, 14, 2.25, NAVY_L)
    g.surf(lambda x, y, z: 7 <= y < 10 and (x + z) % 8 in (0, 1) and (y - 7 + (x + z) // 8) % 2 == 0, GOLD_L)   # 금 점무늬
    g.cham(3, 3.25, 3, 13, 4.25, 13, 1.75, NAVY_L); g.cham(3, 4, 3, 13, 4.25, 13, 1.75, SILVER); g.cham(3.5, 4, 3.5, 12.5, 4.25, 12.5, 1.75, NAVY_L)
    g.cyl(8, 8, 2.25, 4.25, 10.5, NAVY); g.cyl(8, 8, 2.75, 4.25, 5, GOLD); g.cyl(8, 8, 2.6, 7, 7.5, GOLD); g.cyl(8, 8, 3.0, 9.75, 10.5, GOLD); g.cyl(8, 8, 3.5, 10.5, 11, SILVER)
    g.surf(lambda x, y, z: 21 <= y < 27 and (math.degrees(math.atan2(z - 31.5, x - 31.5)) % 45) < 9, BLUE)             # 기둥의 세로 빛줄
    for (cx, cz) in ((12.2, 12.2), (3.8, 12.2), (12.2, 3.8), (3.8, 3.8)):                                             # 모서리 수정
        g.cham(cx - 1, 3.25, cz - 1, cx + 1, 4.75, cz + 1, 0.5, GOLD_D); g.octa(cx, 6.6, cz, 0.85, BLUE, sy=2.2); g.octa(cx, 6.6, cz, 0.4, BLUE_H, sy=2.2)
    g.box(3, 11, 0.75, 13, 16, 15.25, NAVY); g.box(3.5, 11.5, 0.25, 12.5, 16, 0.75, NAVY); g.box(3.5, 11.5, 15.25, 12.5, 16, 15.75, NAVY)
    g.surf(lambda x, y, z: 44 <= y < 46, SILVER); g.surf(lambda x, y, z: 59 <= y < 61, GOLD)
    g.surf(lambda x, y, z: 48 <= y < 58 and ((z % 12) in (5, 6) and abs(y - 52.5) < 4 - abs((z % 12) - 5.5) * 0 and (abs(y - 52.5) + abs((z % 12) - 5.5) * 2) < 4), GOLD_L)
    g.surf(lambda x, y, z: x == 51 and abs(y - 52.5) + abs(z - 31.5) < 5, BLUE); g.surf(lambda x, y, z: x == 51 and abs(y - 52.5) + abs(z - 31.5) < 2, BLUE_H)
    g.surf(lambda x, y, z: z == 60 and abs(y - 52.5) + abs(x - 31.5) < 4, BLUE)
    top = 16 * U - 1
    g.surf(lambda x, y, z: y == top, NAVY_L)
    g.surf(lambda x, y, z: y == top and (x in (13, 14, 49, 50) or z in (4, 5, 58, 59)), SILVER)
    def star(x, y, z):
        if y != top: return False
        dx, dz = abs(x - 31.5), abs(z - 31.5)
        return (dx * 3.2 + dz < 26) or (dz * 3.2 + dx < 22) or (abs(dx - dz) < 1.6 and dx < 9) or dx + dz < 6
    g.surf(star, GOLD_L); g.surf(lambda x, y, z: y == top and abs(x - 31.5) + abs(z - 31.5) < 5, STAR)
    g.surf(lambda x, y, z: y == top and 25 < math.hypot(x - 31.5, (z - 31.5)) < 27 and 16 < x < 47, SILVER)
    # 떠 있는 것: 금 고리 두 겹 + 별 수정
    g.torus(8, 20.5, 8, 5.6, 0.32, STAR, gap=lambda a: a % 45 < 5); g.torus(8, 23.5, 8, 3.9, 0.28, mat((200, 208, 224), 5), gap=lambda a: (a + 20) % 90 < 12)
    g.octa(8, 23, 8, 1.3, BLUE, sy=2.0); g.octa(8, 23, 8, 0.7, BLUE_H, sy=2.0)
    for a in range(0, 360, 45):
        g.ball(8 + 5.6 * math.cos(math.radians(a + 22)), 20.5, 8 + 5.6 * math.sin(math.radians(a + 22)), 0.5, STAR_H)
    return g

def design_h():   # 용불의 모루
    g = Grid()
    g.cham(0.5, 0, 0.5, 15.5, 2, 15.5, 1.5, BLACK); g.cham(1.25, 2, 1.25, 14.75, 3, 14.75, 1.5, BLACK_L)
    g.surf(lambda x, y, z: y < 12 and hsh(x // 3, y // 2, z // 3, 4) % 6 == 0 and hsh(x // 9, 0, z // 9, 2) % 2 == 0, LAVA)       # 용암 금
    g.surf(lambda x, y, z: y < 8 and hsh((x + y) // 2, 0, (z - y) // 2, 9) % 11 == 0, EMBER_H)
    anvil(g, mat((48, 46, 52), 5), mat((92, 90, 98), 5), mat((30, 28, 32), 4), y0=4.5)
    g.surf(lambda x, y, z: 22 <= y < 60 and hsh((x * 2 + y) // 5, y // 9, (z * 2 - y) // 5, 6) % 7 == 0 and hsh(x // 6, y // 6, z // 6, 1) % 3 == 0, EMBER)   # 달아오른 금
    g.cyl(8, 8, 3.1, 7.75, 8.75, SCALE_L)
    g.surf(lambda x, y, z: 31 <= y < 35 and (x + z) % 4 < 2, SCALE)
    for (sx, sz) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):     # 네 귀의 용 발톱
        bx, bz = 8 + sx * 5.6, 8 + sz * 5.6
        g.cham(bx - 1.4, 2, bz - 1.4, bx + 1.4, 4.5, bz + 1.4, 0.5, SCALE); g.cham(bx - 1.1, 4.5, bz - 1.1, bx + 1.1, 6.5, bz + 1.1, 0.4, SCALE_L)
        g.surf(lambda x, y, z, bx=bx, bz=bz: abs(x - bx * U) < 6 and abs(z - bz * U) < 6 and 8 <= y < 26 and (y % 4 == 0), SCALE_L)
        for k, (ox, oz) in enumerate(((-sx * 1.2, 0), (0, -sz * 1.2), (-sx * 0.9, -sz * 0.9))):
            for i in range(5):
                t = i / 5.0
                g.box(bx + ox * (1 + t * 0.9) - .4 + t * .15, 6.5 + i * .5 - (0.4 if k == 2 else 0), bz + oz * (1 + t * 0.9) - .4 + t * .15,
                      bx + ox * (1 + t * 0.9) + .4 - t * .15, 7 + i * .5 - (0.4 if k == 2 else 0), bz + oz * (1 + t * 0.9) + .4 - t * .15, BONE if i >= 2 else SCALE_L)
    # 뿔 쪽의 용 뿔 한 쌍, 윗면의 달궈진 자리
    for sx in (-1, 1):
        for i in range(6):
            g.box(8 + sx * (3.6 + i * .28) - .45 + i * .05, 16 + i * .55, 4.6 - i * .22, 8 + sx * (3.6 + i * .28) + .45 - i * .05, 16.55 + i * .55, 5.5 - i * .22, BONE)
    top = 16 * U - 1
    g.surf(lambda x, y, z: y == top and math.hypot((x - 31.5) / 1.25, z - 33) < 9, EMBER); g.surf(lambda x, y, z: y == top and math.hypot((x - 31.5) / 1.25, z - 33) < 5, EMBER_H)
    # 떠 있는 것: 불씨 고리와 세 개의 불꽃
    g.torus(8, 20.5, 8, 5.2, 0.3, EMBER, gap=lambda a: a % 30 < 12)
    for a in (20, 140, 260):
        px, pz = 8 + 5.2 * math.cos(math.radians(a)), 8 + 5.2 * math.sin(math.radians(a))
        g.octa(px, 21.2, pz, 0.9, EMBER, sy=2.4); g.octa(px, 20.9, pz, 0.5, EMBER_H, sy=2.0)
    g.mirror_z()
    return g

def design_i():   # 왕실 대장간
    g = Grid()
    g.cham(0.5, 0, 0.5, 15.5, 1.5, 15.5, 1.25, MARBLE_D); g.cham(0.5, 1.5, 0.5, 15.5, 2, 15.5, 1.25, GOLD); g.cham(1.25, 2, 1.25, 14.75, 3.5, 14.75, 1.25, MARBLE)
    g.cham(1.25, 3.5, 1.25, 14.75, 4, 14.75, 1.25, GOLD); g.cham(2, 3.5, 2, 14, 4, 14, 1.25, CRIMSON)
    g.surf(lambda x, y, z: 8 <= y < 14 and (hsh(x // 5, y // 3, z // 5, 3) % 5 == 0), MARBLE_D)
    for (a, b) in ((31.5, None), (None, 31.5)):       # 받침 옆면의 붉은 보석
        g.surf(lambda x, y, z, a=a, b=b: 8 <= y < 14 and abs(y - 10.5) + abs((z if a is None else z) - 31.5 if a is not None else (x - 31.5)) < 3.5 and ((x == 58 and a is not None) or (z == 58 and a is None)), RED)
    for (cx, cz) in ((2.4, 2.4), (13.6, 2.4), (2.4, 13.6), (13.6, 13.6)):      # 금 기둥과 불 그릇
        g.cyl(cx, cz, 0.9, 4, 4.75, GOLD_D); g.cyl(cx, cz, 0.55, 4.75, 8.5, GOLD); g.cyl(cx, cz, 0.85, 8.5, 9, GOLD_L); g.cyl(cx, cz, 1.1, 9, 9.75, GOLD); g.ball(cx, 10.3, cz, 0.7, EMBER_H)
        g.surf(lambda x, y, z, cx=cx, cz=cz: abs(x - cx * U) < 4 and abs(z - cz * U) < 4 and 19 <= y < 34 and y % 6 == 0, GOLD_D)
    anvil(g, mat((96, 102, 116), 5), STEEL_P, mat((62, 66, 80), 5), y0=5.5)
    g.surf(lambda x, y, z: 57 <= y < 60 and y != 63, GOLD); g.surf(lambda x, y, z: 46 <= y < 48, GOLD_D)
    g.surf(lambda x, y, z: 49 <= y < 56 and 16 <= z < 50 and (abs(y - 52.5) + abs((z % 8) - 3.5) * 1.4) < 3.2, GOLD_L)     # 금 덩굴 무늬
    g.surf(lambda x, y, z: x == 51 and abs(y - 52.5) + abs(z - 33.5) < 3.2, RED)
    g.cyl(8, 8, 3.0, 8.25, 9, GOLD); g.box(3.25, 4, 3, 12.75, 4.5, 13, CRIMSON_D)
    top = 16 * U - 1
    g.surf(lambda x, y, z: y == top and 18 <= z < 50 and (x in (15, 16, 47, 48) or z in (18, 19, 48, 49)) and 15 <= x <= 48, GOLD)
    def crown(x, y, z):
        if y != top: return False
        dx, dz = x - 31.5, z - 33.5
        return (abs(dx) < 6 and 2 <= dz < 5) or (abs(dx) in (5.5, 4.5, 0.5, 1.5) and -4 <= dz < 2 and abs(dz + 1) < 3 + (1 if abs(dx) < 2 else 0)) or (abs(abs(dx) - 3) < 1 and -2 <= dz < 2)
    g.surf(crown, GOLD_L)
    # 떠 있는 것: 금빛 망치와 빛 고리
    g.torus(8, 19.5, 8, 5.0, 0.28, STAR, gap=lambda a: a % 60 < 6)
    g.box(7.5, 21.2, 4.5, 8.5, 22.2, 11.5, WOOD); g.box(6.6, 20.4, 9.6, 9.4, 23, 12.4, GOLD); g.box(6.6, 20.9, 9.2, 9.4, 22.5, 9.6, GOLD_L); g.box(6.6, 20.9, 12.4, 9.4, 22.5, 12.8, GOLD_L)
    g.surf(lambda x, y, z: 82 <= y < 92 and 38 <= z < 50 and x == 37 and abs(y - 86.5) + abs(z - 43.5) < 4, RED)
    g.mirror_z()
    return g

if __name__ == '__main__':
    F = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc', 30, index=1)
    f2 = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc', 18, index=1)
    panels = [('G · 별의 단조대', '남빛 팔각 단 + 금·은 세공, 네 귀의 푸른 수정\n움직임: 금 고리 두 겹이 엇갈려 돌고, 별 수정이 떠서\n숨 쉬듯 빛남 · 푸른 불티가 위로 오름', design_g),
              ('H · 용불의 모루', '검은 강철 모루를 움켜쥔 네 개의 용 발톱, 용암 금\n움직임: 불씨 고리와 불꽃 셋이 돌고, 몸의 금이\n맥박처럼 밝아짐 · 불똥이 튐', design_h),
              ('I · 왕실 대장간', '흰 대리석 단 + 금 기둥의 불 그릇, 금 덩굴 무늬 모루\n움직임: 금빛 망치가 떠 있다가 가끔 내려쳐 불똥이\n튀고, 빛 고리가 천천히 돎', design_i)]
    W = 560; out = Image.new('RGB', (W * 3 + 40, 760), (22, 23, 29)); d = ImageDraw.Draw(out)
    for i, (t, sub, fn) in enumerate(panels):
        t0 = time.time(); im = render(fn(), S=3.5, W=W, H=590, cy_off=150); print(t, round(time.time() - t0, 1), flush=True)
        out.paste(im, (10 + i * (W + 10), 10))
        d.text((10 + i * (W + 10) + 16, 610), t, font=F, fill=(240, 214, 140))
        d.multiline_text((10 + i * (W + 10) + 16, 656), sub, font=f2, fill=(200, 204, 214), spacing=5)
    out.save(sys.argv[1])
