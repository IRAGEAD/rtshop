# 이야기 물건의 16x16 픽셀 그림을 그린다: python3 tools/story_items.py
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'textures', 'item')
MODELS = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'models', 'item')

O = (40, 24, 12, 255)  # 외곽선


def img():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def save(name, im):
    im.save(os.path.join(OUT, name + '.png'))
    with open(os.path.join(MODELS, name + '.json'), 'w') as f:
        f.write('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "rtshop:item/%s"\n  }\n}\n' % name)


def journal():
    im = img(); d = ImageDraw.Draw(im)
    d.rectangle([3, 2, 12, 13], fill=(96, 52, 28, 255), outline=O)
    d.rectangle([4, 3, 4, 12], fill=(70, 36, 18, 255))
    d.rectangle([6, 4, 11, 4], fill=(214, 170, 90, 255))
    d.rectangle([6, 6, 10, 6], fill=(200, 160, 84, 255))
    d.rectangle([12, 2, 13, 13], fill=(236, 222, 190, 255))
    d.line([(9, 9), (9, 13)], fill=(170, 30, 30, 255))  # 책갈피
    return im


def coin():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([3, 3, 12, 12], fill=(150, 150, 156, 255), outline=(60, 60, 66, 255))
    d.ellipse([5, 5, 10, 10], outline=(110, 110, 118, 255))
    d.point([(6, 6), (7, 5)], fill=(220, 220, 228, 255))
    d.line([(7, 7), (8, 8)], fill=(80, 80, 88, 255))
    return im


def old_map():
    im = img(); d = ImageDraw.Draw(im)
    d.polygon([(2, 3), (13, 2), (14, 12), (3, 13)], fill=(222, 200, 150, 255), outline=O)
    d.line([(4, 10), (7, 7), (9, 9), (12, 5)], fill=(150, 40, 30, 255))
    for p in [(5, 5), (11, 10), (8, 4)]:
        d.point(p, fill=(60, 90, 50, 255))
    d.point((12, 5), fill=(200, 30, 20, 255)); d.point((12, 4), fill=(200, 30, 20, 255))
    return im


def tag():
    im = img(); d = ImageDraw.Draw(im)
    d.rectangle([4, 5, 11, 11], fill=(150, 150, 140, 255), outline=(50, 50, 46, 255))
    d.line([(7, 2), (6, 5)], fill=(90, 90, 84, 255)); d.line([(8, 2), (9, 5)], fill=(90, 90, 84, 255))
    d.line([(6, 7), (9, 7)], fill=(70, 70, 64, 255)); d.line([(6, 9), (8, 9)], fill=(70, 70, 64, 255))
    d.point((5, 6), fill=(200, 200, 190, 255))
    return im


def mask():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([2, 2, 13, 14], fill=(210, 150, 60, 255), outline=O)
    d.rectangle([4, 6, 6, 7], fill=(30, 20, 10, 255)); d.rectangle([9, 6, 11, 7], fill=(30, 20, 10, 255))
    d.line([(5, 11), (10, 11)], fill=(150, 60, 30, 255))
    d.line([(8, 2), (6, 8), (9, 14)], fill=(60, 30, 10, 255))  # 금
    for x in range(3, 13, 3):
        d.point((x, 3), fill=(250, 220, 120, 255))
    return im


def name_tag():
    im = img(); d = ImageDraw.Draw(im)
    d.rectangle([3, 4, 12, 11], fill=(170, 210, 230, 255), outline=(40, 70, 90, 255))
    d.line([(5, 6), (10, 6)], fill=(60, 90, 110, 255)); d.line([(5, 8), (9, 8)], fill=(60, 90, 110, 255))
    d.point((12, 3), fill=(220, 240, 255, 255)); d.point((3, 12), fill=(220, 240, 255, 255))
    return im


def frozen_log():
    im = journal()
    d = ImageDraw.Draw(im)
    for p in [(3, 2), (4, 2), (12, 13), (13, 12), (3, 13), (13, 2)]:
        d.point(p, fill=(200, 235, 255, 255))
    d.line([(3, 7), (5, 7)], fill=(180, 225, 250, 255))
    return im


def compass():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([2, 2, 13, 13], fill=(120, 80, 40, 255), outline=O)
    d.ellipse([4, 4, 11, 11], fill=(236, 222, 190, 255))
    d.polygon([(7, 4), (9, 8), (7, 8)], fill=(200, 30, 30, 255))
    d.polygon([(7, 8), (9, 8), (8, 11)], fill=(80, 80, 90, 255))
    d.point((8, 2), fill=(230, 190, 90, 255))
    return im


def ash():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([3, 8, 12, 13], fill=(110, 110, 112, 255), outline=(50, 50, 52, 255))
    for p in [(6, 6), (8, 4), (10, 6), (7, 3), (9, 2)]:
        d.point(p, fill=(150, 150, 155, 200))
    d.point((7, 10), fill=(220, 90, 30, 255))
    return im


def memory():
    im = img(); d = ImageDraw.Draw(im)
    d.polygon([(8, 1), (13, 7), (8, 14), (3, 7)], fill=(120, 110, 140, 255), outline=(40, 30, 60, 255))
    d.polygon([(8, 3), (11, 7), (8, 11), (5, 7)], fill=(190, 180, 220, 255))
    d.point((7, 5), fill=(255, 255, 255, 255))
    return im


def last_page():
    im = img(); d = ImageDraw.Draw(im)
    d.polygon([(3, 2), (12, 2), (13, 13), (4, 14)], fill=(248, 238, 210, 255), outline=(150, 110, 40, 255))
    d.line([(5, 5), (10, 5)], fill=(210, 160, 60, 255))
    d.line([(5, 7), (11, 7)], fill=(170, 140, 100, 255))
    d.line([(5, 9), (9, 9)], fill=(170, 140, 100, 255))
    d.point((10, 11), fill=(255, 120, 40, 255)); d.point((11, 11), fill=(255, 200, 80, 255))
    return im


def weapon_manual():
    im = img(); d = ImageDraw.Draw(im)
    # 가죽 표지의 얇은 책 + 표지에 칼 한 자루
    d.rectangle([3, 2, 12, 13], fill=(96, 58, 34, 255), outline=(44, 24, 12, 255))
    d.line([(3, 2), (3, 13)], fill=(60, 34, 18, 255))
    d.line([(13, 3), (13, 13)], fill=(230, 220, 190, 255))
    d.line([(4, 14), (13, 14)], fill=(200, 188, 156, 255))
    d.line([(10, 4), (6, 10)], fill=(214, 220, 230, 255))
    d.point((10, 4), fill=(255, 255, 255, 255))
    d.line([(5, 9), (7, 11)], fill=(214, 170, 70, 255))
    d.point((5, 11), fill=(150, 100, 40, 255))
    d.point((11, 12), fill=(214, 170, 70, 255))
    return im


if __name__ == '__main__':
    for n, f in [('mother_journal', journal), ('grey_coin', coin), ('old_expedition_map', old_map), ('expedition_tag', tag),
                 ('broken_sun_mask', mask), ('crew_name_tag', name_tag), ('frozen_logbook', frozen_log), ('survey_compass', compass),
                 ('grey_ash', ash), ('memory_shard', memory), ('chronicle_last_page', last_page), ('weapon_manual', weapon_manual)]:
        save(n, f())
    print('ok')
