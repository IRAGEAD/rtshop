import math
from PIL import Image, ImageDraw, ImageFilter, ImageChops
U = 4  # voxels per pixel
def hsh(x, y, z, k=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (k * 2654435761)) & 0xFFFF
def mat(base, spread=6, cell=U):
    def f(x, y, z):
        r = hsh(x // cell, y // cell, z // cell) % (2 * spread + 1) - spread
        return tuple(max(0, min(255, c + r)) for c in base), False
    return f
def glow(base, spread=8):
    def f(x, y, z):
        r = hsh(x // U, y // U, z // U) % (2 * spread + 1) - spread
        return tuple(max(0, min(255, c + r)) for c in base), True
    return f
class Grid:
    def __init__(s): s.v = {}
    def _r(s, a, b): return range(int(round(a * U)), int(round(b * U)))
    def box(s, x0, y0, z0, x1, y1, z1, m):
        for x in s._r(x0, x1):
            for y in s._r(y0, y1):
                for z in s._r(z0, z1): s.v[(x, y, z)] = m(x, y, z)
    def cham(s, x0, y0, z0, x1, y1, z1, c, m):
        for x in s._r(x0, x1):
            for z in s._r(z0, z1):
                dx = min(x - x0 * U, x1 * U - 1 - x); dz = min(z - z0 * U, z1 * U - 1 - z)
                if dx + dz < c * U: continue
                for y in s._r(y0, y1): s.v[(x, y, z)] = m(x, y, z)
    def cyl(s, cx, cz, r, y0, y1, m):
        for x in s._r(cx - r, cx + r):
            for z in s._r(cz - r, cz + r):
                if math.hypot(x + .5 - cx * U, z + .5 - cz * U) <= r * U:
                    for y in s._r(y0, y1): s.v[(x, y, z)] = m(x, y, z)
    def ball(s, cx, cy, cz, r, m, sy=1.0):
        for x in s._r(cx - r, cx + r):
            for y in s._r(cy - r * sy, cy + r * sy):
                for z in s._r(cz - r, cz + r):
                    if math.sqrt((x + .5 - cx * U) ** 2 + ((y + .5 - cy * U) / sy) ** 2 + (z + .5 - cz * U) ** 2) <= r * U: s.v[(x, y, z)] = m(x, y, z)
    def octa(s, cx, cy, cz, r, m, sy=1.6):
        for x in s._r(cx - r, cx + r):
            for y in s._r(cy - r * sy, cy + r * sy):
                for z in s._r(cz - r, cz + r):
                    if abs(x + .5 - cx * U) + abs(y + .5 - cy * U) / sy + abs(z + .5 - cz * U) <= r * U: s.v[(x, y, z)] = m(x, y, z)
    def torus(s, cx, cy, cz, R, r, m, gap=None):
        for x in s._r(cx - R - r, cx + R + r):
            for z in s._r(cz - R - r, cz + R + r):
                dx, dz = x + .5 - cx * U, z + .5 - cz * U
                q = math.hypot(dx, dz) - R * U
                if gap and gap(math.degrees(math.atan2(dz, dx)) % 360): continue
                for y in s._r(cy - r, cy + r):
                    if q * q + (y + .5 - cy * U) ** 2 <= (r * U) ** 2: s.v[(x, y, z)] = m(x, y, z)
    def cut(s, x0, y0, z0, x1, y1, z1):
        for x in s._r(x0, x1):
            for y in s._r(y0, y1):
                for z in s._r(z0, z1): s.v.pop((x, y, z), None)
    def paint(s, pred, m):
        for k in list(s.v):
            if pred(*k): s.v[k] = m(*k)
    def surf(s, pred, m):
        """paint only voxels on the visible surfaces"""
        for k in list(s.v):
            x, y, z = k
            if ((x + 1, y, z) not in s.v or (x, y, z + 1) not in s.v or (x, y + 1, z) not in s.v) and pred(x, y, z): s.v[k] = m(x, y, z)
    def mirror_z(s, size=16):
        s.v = {(x, y, size * U - 1 - z): c for (x, y, z), c in s.v.items()}
def render(g, S=3.6, W=560, H=640, bg=(30, 32, 40), ground=True, cy_off=150):
    im = Image.new('RGB', (W, H), bg); gl = Image.new('RGB', (W, H), (0, 0, 0))
    d = ImageDraw.Draw(im); dg = ImageDraw.Draw(gl)
    cx, cy = W // 2, H - cy_off
    c30, s30 = math.cos(math.radians(30)), math.sin(math.radians(30)); N = 16 * U
    def P(x, y, z):
        x -= N / 2; z -= N / 2
        return (cx + (x - z) * c30 * S, cy + (x + z) * s30 * S - y * S)
    if ground:
        sh = Image.new('L', (W, H), 0); ds = ImageDraw.Draw(sh)
        ds.polygon([P(2, 0, 2), P(N - 2, 0, 2), P(N + 8, 0, N + 8), P(2, 0, N - 2)], fill=120)
        sh = sh.filter(ImageFilter.GaussianBlur(12)); im.paste((14, 15, 20), mask=sh)
    v = g.v
    for (x, y, z) in sorted(v, key=lambda k: (k[0] + k[2], k[1])):
        col, em = v[(x, y, z)]
        top = (x, y + 1, z) not in v; rx = (x + 1, y, z) not in v; rz = (x, y, z + 1) not in v
        if not (top or rx or rz): continue
        def face(pts, f):
            c = col if em else tuple(min(255, int(c0 * f)) for c0 in col)
            d.polygon(pts, fill=c)
            if em: dg.polygon(pts, fill=col)
        if top:
            edge = (x + 1, y, z) not in v or (x, y, z + 1) not in v or (x - 1, y, z) not in v or (x, y, z - 1) not in v
            ao = 0.86 if ((x, y + 1, z - 1) in v or (x - 1, y + 1, z) in v) else 1.0
            face([P(x, y + 1, z), P(x + 1, y + 1, z), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], (1.12 if edge else 1.0) * ao)
        if rx:
            ao = 0.8 if (x + 1, y - 1, z) in v else 1.0
            face([P(x + 1, y, z), P(x + 1, y, z + 1), P(x + 1, y + 1, z + 1), P(x + 1, y + 1, z)], 0.58 * ao * (1.1 if top else 1.0))
        if rz:
            ao = 0.8 if (x, y - 1, z + 1) in v else 1.0
            face([P(x, y, z + 1), P(x + 1, y, z + 1), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], 0.78 * ao * (1.1 if top else 1.0))
    im = ImageChops.add(im, gl.filter(ImageFilter.GaussianBlur(6)).point(lambda q: int(q * 0.7)))
    im = ImageChops.add(im, gl.filter(ImageFilter.GaussianBlur(22)).point(lambda q: int(q * 0.55)))
    return im
