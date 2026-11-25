import math, sys
from PIL import Image, ImageDraw, ImageFont
from vox import *
from anvil_mock3 import anvil
FORGED = mat((56, 58, 66), 5); FORGED_L = mat((118, 122, 134), 5); FORGED_D = mat((34, 36, 42), 4)
SLATE = mat((62, 66, 76), 5); SLATE_L = mat((86, 90, 102), 5); SLATE_D = mat((44, 48, 58), 4)
BRASS = mat((190, 150, 72), 8); BRASS_L = mat((232, 198, 112), 7); BRASS_D = mat((136, 100, 44), 7)
EMBER = glow((255, 130, 44)); EMBER_S = glow((214, 92, 30), 6); EMBER_H = glow((255, 214, 130))
def design(lit=True):
    g = Grid()
    # K 의 받침: 점판암 팔각 단 두 층 + 새긴 룬 띠
    g.cham(0.5, 0, 0.5, 15.5, 3, 15.5, 1.5, SLATE); g.cham(0.5, 0, 0.5, 15.5, 0.5, 15.5, 1.5, SLATE_D); g.cham(0.5, 2.5, 0.5, 15.5, 3, 15.5, 1.5, SLATE_L)
    g.cham(1.5, 3, 1.5, 14.5, 4.5, 14.5, 1.25, SLATE); g.cham(1.5, 4.25, 1.5, 14.5, 4.5, 14.5, 1.25, BRASS); g.cham(2, 4.25, 2, 14, 4.5, 14, 1.25, SLATE_L)      # 윗단 가장자리에 놋쇠 띠 한 줄
    rune = EMBER_S if lit else SLATE_D
    def band(x, y, z):
        if not (4 <= y < 8): return False
        side_x = (x + 1, y, z) not in g.v or (x - 1, y, z) not in g.v; side_z = (x, y, z + 1) not in g.v or (x, y, z - 1) not in g.v
        if not (side_x or side_z): return False
        c = (z if side_x else x) % 10
        return c in (2, 3, 4, 6) and (y in (4, 7) or c in (2, 6))
    g.paint(band, rune)
    for (x, z) in ((2.6, 2.6), (13.4, 2.6), (2.6, 13.4), (13.4, 13.4)): g.cham(x - .5, 4.5, z - .5, x + .5, 5.1, z + .5, .15, BRASS_L)   # 네 귀의 놋쇠 못
    # J 의 모루: 진짜 모루 실루엣의 단조 강철 + 조금 더한 화려함
    anvil(g, FORGED, FORGED_L, FORGED_D, y0=6.0)
    g.cyl(8, 8, 2.9, 8.5, 9, BRASS); g.cyl(8, 8, 3.3, 10.75, 11.25, BRASS_D)                                             # 허리의 놋쇠 띠 두 줄
    g.surf(lambda x, y, z: 58 <= y < 59 and 14 <= z < 52, BRASS_L); g.surf(lambda x, y, z: 47 <= y < 48 and 16 <= z < 50, BRASS)     # 옆면 상감 두 줄
    g.surf(lambda x, y, z: 50 <= y < 56 and 18 <= z < 48 and (abs(y - 52.5) + abs((z % 10) - 4.5) * .9) < 2.6 and (abs(y - 52.5) + abs((z % 10) - 4.5) * .9) > 1.2, BRASS)   # 마름모 무늬 상감
    g.surf(lambda x, y, z: x == 51 and abs(y - 52.5) + abs(z - 33.5) < 2.4, EMBER if lit else BRASS_D)                  # 옆면 가운데의 작은 불씨 보석
    top = 16 * U - 1
    g.surf(lambda x, y, z: y == top and 18 <= z < 50 and 14 <= x <= 49 and (x in (14, 49) or z in (18, 49)), BRASS)      # 윗면의 가는 놋쇠 테
    g.mirror_z(); return g
if __name__ == '__main__':
    F = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc', 28, index=1); f2 = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc', 18, index=1)
    W = 560; out = Image.new('RGB', (W * 2 + 30, 680), (22, 23, 29)); d = ImageDraw.Draw(out)
    for i, (t, sub, lit) in enumerate((('평소', '받침의 룬 띠와 옆면 보석은 꺼져 있음', False), ('사람이 다가오면', '룬 띠가 천천히 켜지고 옆면 보석이 은은히 빛남', True))):
        out.paste(render(design(lit), S=4.0, W=W, H=530, cy_off=150), (10 + i * (W + 10), 10))
        d.text((26 + i * (W + 10), 552), t, font=F, fill=(232, 226, 214)); d.text((26 + i * (W + 10), 596), sub, font=f2, fill=(196, 200, 210))
    out.save(sys.argv[1])
