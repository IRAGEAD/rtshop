# 도감·가이드북 아이템 그림 (16px): python3 tools/gen_books.py
# 무기 설명서와 같은 책 모양에, 표지 색과 문양만 다르다.
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'textures', 'item')


def book(name, cover, dark, edge, mark, marks):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(2, 14):
        for x in range(3, 13):
            px[x, y] = cover
    for x in range(3, 13):
        px[x, 2] = dark
        px[x, 13] = dark
    for y in range(2, 14):
        px[3, y] = edge
        px[12, y] = dark
    for y in range(3, 14):
        px[13, y] = (230, 220, 190, 255)      # 책배
    for x in range(4, 14):
        px[x, 14] = (200, 188, 156, 255)
    for (x, y) in marks:
        px[x, y] = mark
    im.save(os.path.join(OUT, name + '.png'))


GOLD = (214, 170, 70, 255)
# 도감: 짙은 풀빛 표지, 금빛 발자국
book('codex', (46, 84, 52, 255), (22, 44, 28, 255), (30, 58, 36, 255), GOLD,
     [(6, 5), (9, 5), (5, 7), (10, 7), (7, 8), (8, 8), (6, 9), (7, 9), (8, 9), (9, 9), (6, 10), (7, 10), (8, 10), (9, 10), (7, 11), (8, 11)])
# 가이드북: 짙은 쪽빛 표지, 금빛 나침반 별
book('guidebook', (44, 66, 110, 255), (22, 34, 62, 255), (30, 46, 80, 255), GOLD,
     [(7, 4), (8, 4), (7, 5), (8, 5), (7, 6), (8, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8),
      (7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (8, 11)])
print('codex.png, guidebook.png')
