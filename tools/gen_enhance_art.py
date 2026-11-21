"""Enhancement art: enhance anvil (top/body) and three guard tickets (silver ticket base + shield emblem)."""
import os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'textures')
random.seed(11)

def noise(base, spread):
    return tuple(max(0, min(255, c + random.randint(-spread, spread))) for c in base) + (255,)

# body: dark blue steel with subtle noise, lighter top edge
body = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = noise((52, 58, 72), 7)
        if y == 0 or x == 0:
            c = noise((78, 86, 104), 6)
        if y == 15 or x == 15:
            c = noise((30, 34, 44), 5)
        body.putpixel((x, y), c)
body.save(os.path.join(RES, 'block', 'enhance_anvil_body.png'))

# top: steel face with gold rim and a glowing purple rune
top = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        top.putpixel((x, y), noise((70, 76, 92), 6))
for i in range(16):
    for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
        top.putpixel((x, y), noise((200, 160, 60), 12))
rune = ["....XX....",
        "...X..X...",
        "..X.XX.X..",
        ".X.X..X.X.",
        "X.X.XX.X.X",
        "X.X.XX.X.X",
        ".X.X..X.X.",
        "..X.XX.X..",
        "...X..X...",
        "....XX...."]
for dy, row in enumerate(rune):
    for dx, ch in enumerate(row):
        if ch == 'X':
            top.putpixel((3 + dx, 3 + dy), (190, 110, 255, 255) if (dx + dy) % 2 else (150, 80, 230, 255))
top.save(os.path.join(RES, 'block', 'enhance_anvil_top.png'))

# guard tickets
base = Image.open(os.path.join(HERE, 'ticket_base_mid.png')).convert('RGBA')
bg = base.getpixel((3, 5))
SHIELD = [".XXXX.",
          "XXXXXX",
          "XXOOXX",
          ".XXXX.",
          "..XX.."]
STYLES = {
    'enhance_guard_damage': ((60, 130, 230, 255), (230, 240, 255, 255)),
    'enhance_guard_destroy': ((215, 50, 50, 255), (255, 220, 120, 255)),
    'enhance_guard_full': ((235, 185, 40, 255), (170, 70, 230, 255)),
}
for name, (col, mark) in STYLES.items():
    im = base.copy()
    for y in range(5, 10):
        for x in range(3, 9):
            if im.getpixel((x, y))[3] and x != 10:
                im.putpixel((x, y), bg)
    for dy, row in enumerate(SHIELD):
        for dx, ch in enumerate(row):
            if ch == 'X':
                im.putpixel((3 + dx, 5 + dy), col)
            elif ch == 'O':
                im.putpixel((3 + dx, 5 + dy), mark)
    im.save(os.path.join(RES, 'item', name + '.png'))
    print('wrote', name)
