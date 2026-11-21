"""Category gacha tickets: the tier is the ticket colour (bronze/silver/gold), the category is the emblem.
Reads the plain ticket shapes from tools/ticket_base_<tier>.png and writes
gacha_ticket_<tier>.png (weapon), gacha_ticket_pet_<tier>.png, gacha_ticket_accessory_<tier>.png."""
import os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'textures', 'item')
EMBLEMS = {
    'weapon': (["..X..", "..X..", "..X..", ".XXX.", "..X.."], (205, 45, 45, 255), (120, 20, 20, 255)),
    'pet': ([".X.X.", "X...X", ".....", ".XXX.", ".XXX."], (40, 150, 60, 255), (20, 80, 30, 255)),
    'accessory': (["..X..", ".X.X.", "X.X.X", ".X.X.", "..X.."], (120, 70, 225, 255), (60, 30, 130, 255)),
}
for tier in ('normal', 'mid', 'high'):
    base = Image.open(os.path.join(HERE, 'ticket_base_' + tier + '.png')).convert('RGBA')
    bg = base.getpixel((3, 5))
    for cat, (rows, col, _) in EMBLEMS.items():
        im = base.copy()
        for y in range(5, 10):
            for x in range(3, 9):
                if im.getpixel((x, y))[3] and x != 10:
                    im.putpixel((x, y), bg)
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch == 'X':
                    im.putpixel((4 + dx, 5 + dy), col)
        name = 'gacha_ticket_' + tier if cat == 'weapon' else 'gacha_ticket_%s_%s' % (cat, tier)
        im.save(os.path.join(OUT, name + '.png'))
        print('wrote', name)
