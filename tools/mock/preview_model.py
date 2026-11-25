"""블록 모델(JSON, UV 를 적지 않은 상자들)을 비스듬히 내려다본 그림으로 미리 본다: python3 tools/mock/preview_model.py <model.json> <out.png> [mirror]"""
import json, math, os, sys
from PIL import Image, ImageDraw
U = 4
def main(model_path, out, mirror=False):
    m = json.load(open(model_path)); root = os.path.join(os.path.dirname(model_path), '..', '..', 'textures')
    tex = {k: Image.open(os.path.join(root, v.split(':')[1] + '.png')).convert('RGBA') for k, v in m['textures'].items()}
    vox = {}
    def smp(name, u, v):
        im = tex[name.lstrip('#')]; return im.getpixel((min(im.width - 1, max(0, int(u * im.width / 16))), min(im.height - 1, max(0, int(v * im.height / 16)))))[:3]
    for e in m['elements']:
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']; f = e['faces']
        for x in range(round(x0 * U), round(x1 * U)):
            for y in range(round(y0 * U), round(y1 * U)):
                for z in range(round(z0 * U), round(z1 * U)):
                    px, py, pz = (x + .5) / U, (y + .5) / U, (z + .5) / U
                    side = next(iter(f.values()))['texture']
                    top = smp(f['up']['texture'] if 'up' in f else side, px, pz)
                    if mirror: east = smp(f.get('east', f.get('west', {'texture': side}))['texture'], 16 - pz, 16 - py); south = smp(f.get('north', {'texture': side})['texture'], 16 - px, 16 - py)
                    else: east = smp(f.get('east', {'texture': side})['texture'], 16 - pz, 16 - py); south = smp(f.get('south', {'texture': side})['texture'], px, 16 - py)
                    vox[(x, y, (16 * U - 1 - z) if mirror else z)] = (top, east, south)
    W, H, S = 620, 600, 4.4; im = Image.new('RGB', (W, H), (30, 32, 40)); d = ImageDraw.Draw(im); cx, cy = W // 2, H - 150; N = 16 * U
    c30, s30 = math.cos(math.radians(30)), math.sin(math.radians(30))
    def P(x, y, z): return (cx + (x - N / 2 - (z - N / 2)) * c30 * S, cy + (x - N / 2 + z - N / 2) * s30 * S - y * S)
    for (x, y, z) in sorted(vox, key=lambda k: (k[0] + k[2], k[1])):
        t, e, s = vox[(x, y, z)]
        if (x, y + 1, z) not in vox: d.polygon([P(x, y + 1, z), P(x + 1, y + 1, z), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], fill=t)
        if (x + 1, y, z) not in vox: d.polygon([P(x + 1, y, z), P(x + 1, y, z + 1), P(x + 1, y + 1, z + 1), P(x + 1, y + 1, z)], fill=tuple(int(v * .6) for v in e))
        if (x, y, z + 1) not in vox: d.polygon([P(x, y, z + 1), P(x + 1, y, z + 1), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], fill=tuple(int(v * .8) for v in s))
    im.save(out)
if __name__ == '__main__': main(sys.argv[1], sys.argv[2], len(sys.argv) > 3)
