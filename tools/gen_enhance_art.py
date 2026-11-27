"""Enhancement art: four guard tickets (silver ticket base + shield emblem). The anvil block: tools/gen_enhance_anvil.py."""
import os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'textures')
random.seed(11)

def noise(base, spread):
    return tuple(max(0, min(255, c + random.randint(-spread, spread))) for c in base) + (255,)

# (강화 모루 블록의 질감과 모델은 3.49 부터 tools/gen_enhance_anvil.py 가 만든다)

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
    # 3.66.0: 하급 보호권 (+6 강화까지, 손상 · 파괴 모두 막음) — 청동 방패
    'enhance_guard_lesser': ((176, 118, 66, 255), (245, 226, 196, 255)),
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
