"""NPC 외형 (시안 C: 사람 몸 + 인물별 입체 장식) 을 게임 파일로 뽑는다 (3.61).
python3 tools/gen_npc_looks.py   →  src/main/resources/assets/rtshop/npc_looks/<모습>.bin (네모들)
                                    + src/main/resources/assets/rtshop/textures/npc_looks/<모습>.png (빛깔)

모습의 원본은 tools/mock/npc_mock.py 의 인물 함수(반 픽셀 칸으로 쌓은 상자들)다. 여기서는 그 칸들의 겉면만 모아,
이어진 면을 (빛깔과 상관없이) 큰 네모로 합치고, 빛깔은 그림 한 장에 구워 넣는다. 화면 쪽(client/NpcLookClient)은 그 그림을 입힌 네모들을 그린다.
(3.61 은 빛깔마다 네모를 따로 적어 인물당 2천 개가 넘었고, NPC 가 모인 곳에서 프레임이 크게 떨어졌다. 3.63 부터 인물당 2~3백 개.)

파일 형식 2판 (바이트): 'RTNL' · 판(1) = 2 · 네모 수(4, 큰 끝) · 그림 너비(2) · 그림 높이(2) · 네모마다 11바이트
  [부위 0=몸 1=머리 2=팔(+x) 3=팔(-x) 4=다리(+x) 5=다리(-x)][방향 0:+x 1:-x 2:+y 3:-y 4:+z 5:-z][면의 자리][a0][b0][a1][b1][u(2)][v(2)]   (자리는 반 픽셀, 부호 있는 1바이트. u·v 는 (a0, b0) 칸의 그림 속 자리)
  (3.71: 팔다리 부위 2~5 를 더했다. 3.70 까지의 화면 쪽은 1 이 아닌 부위를 모두 몸으로 그리므로 그대로 읽힌다.)
  ±x 면: a = y, b = z / ±y 면: a = x, b = z / ±z 면: a = x, b = y.  앞은 +z, 발바닥은 y = 0, 머리의 축은 y = 48(목).
  그림에서 칸 (a, b) 의 빛깔은 (u + a - a0, v + b - b0) 에 있다. 네모마다 둘레에 한 칸씩 같은 빛깔을 덧대어 이웃 네모의 빛깔이 번지지 않게 한다.
머리(얼굴·머리카락·수염·모자·안경)는 따로 묶여 플레이어를 바라보며 돈다. 그 밖은 몸.
"""
import importlib.util, os, struct, sys
here = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('npc_mock', os.path.join(here, 'mock', 'npc_mock.py'))
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
U = m.U
OUT = os.path.join(here, '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'npc_looks')
TEX = os.path.join(here, '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'textures', 'npc_looks')

LOOKS = ['erwin', 'isabel', 'roran', 'felix', 'bram', 'oscar', 'haru', 'morris', 'lucky',
         'm_farmer', 'm_food', 'm_fish', 'm_gear', 'm_general', 'm_land', 'm_bank', 'm_ticket', 'm_raid',
         'leon', 'bron', 'garen', 'lia', 'elis', 'garret', 'theo', 'adel', 'raid_guide',
         'albert', 'lorenzo', 'eli', 'erwin_bare']
HEAD_FN = ['face', 'hair', 'beard', 'glasses', 'hat_brim', 'hood', 'furhat', 'toque', 'wizhat', 'beret', 'bandana', 'circlet', 'cap', 'quill']
BODY_FN = ['pack', 'pauldron', 'scarf', 'hood_down', 'mantle', 'cloak', 'staff', 'held']

# 부위 적기: 머리 함수 안에서 놓인 칸은 머리, 몸 함수 안은 몸, 그 밖의 낱 상자는 자리로 정한다 (목 위 · 머리 둘레 안이면 머리)
_orig_box = m.Fig.box
def _box(s, x0, y0, z0, x1, y1, z1, col, n=5):
    _orig_box(s, x0, y0, z0, x1, y1, z1, col, n)
    forced = getattr(s, '_force', [])
    if s.head is not None: part = 1 if s.head else 0
    elif forced: part = forced[-1]
    else: part = 1 if (y0 >= 23.9 and abs((x0 + x1) / 2) <= 5.2 and abs((z0 + z1) / 2) <= 7.6) else 0
    if not hasattr(s, 'p'): s.p = {}
    for x in range(round(x0 * U), round(x1 * U)):
        for y in range(round(y0 * U), round(y1 * U)):
            for z in range(round(z0 * U), round(z1 * U)):
                if (x, y, z) in s.v: s.p[(x, y, z)] = part
m.Fig.box = _box

# 3.71 팔다리: 상자를 놓은 함수(옷 · 소품)를 칸마다 적어 두고, 다 쌓은 뒤 팔 · 다리 칸을 가른다 (limbs)
_box_part = m.Fig.box
def _box_src(s, x0, y0, z0, x1, y1, z1, col, n=5):
    _box_part(s, x0, y0, z0, x1, y1, z1, col, n)
    src = getattr(s, '_src', [])
    if not hasattr(s, 'src'): s.src = {}
    tag = src[-1] if src else 'direct'
    for x in range(round(x0 * U), round(x1 * U)):
        for y in range(round(y0 * U), round(y1 * U)):
            for z in range(round(z0 * U), round(z1 * U)):
                if (x, y, z) in s.v: s.src[(x, y, z)] = tag
m.Fig.box = _box_src
SRC_FN = ['human', 'coat', 'held', 'skirt', 'apron', 'cloak', 'mantle', 'pack', 'pauldron', 'staff', 'scarf', 'strap', 'plate', 'belt', 'pouch', 'satchel', 'hood_down', 'buttons']
# 몸에 붙어 움직이지 않는 것 (팔다리 자리에 있어도 몸)
STATIC = {'skirt', 'apron', 'cloak', 'mantle', 'pack', 'pauldron', 'staff', 'scarf', 'strap', 'plate', 'belt', 'pouch', 'satchel', 'hood_down', 'buttons'}
def _tag(name):
    fn = getattr(m, name)
    def w(g, *a, **k):
        if not hasattr(g, '_src'): g._src = []
        g._src.append(name)
        try: return fn(g, *a, **k)
        finally: g._src.pop()
    setattr(m, name, w)
for _n in SRC_FN: _tag(_n)

def limbs(g):
    """몸(0) 칸 가운데 팔 · 다리를 가른다: 팔 = 어깨 아래 몸통 바깥(|x| >= 4, 12 <= y < 25) 또는 손에 든 것, 다리 = 허리 아래(y < 12).
    치마 · 긴 겉옷 · 앞치마 · 망토가 허리 아래를 덮거나 다리 기둥 밖으로 나온 칸이 있으면 다리는 가르지 않는다 (걸을 때 치마가 찢어지지 않게).
    돌려준다: 다리를 갈랐는가."""
    p, src = g.p, getattr(g, 'src', {})
    skirted = False
    for (x, y, z), part in p.items():
        if part != 0 or y >= 12 * U: continue
        t = src.get((x, y, z), 'direct')
        cx, cz = (x + .5) / U, (z + .5) / U
        # 허리 아래를 덮는 옷이거나, 따로 그린 칸이 다리 기둥 밖으로 나왔으면 (허리띠 · 주머니 · 가방 같은 몸의 소품은 빼고)
        if t in ('coat', 'skirt', 'apron', 'cloak') or (t == 'direct' and (abs(cx) > 4.6 or abs(cz) > 2.8)):
            skirted = True
            break
    for key, part in list(p.items()):
        if part != 0: continue
        t = src.get(key, 'direct')
        if t in STATIC: continue
        x, y, z = key
        cx, cy = (x + .5) / U, (y + .5) / U
        if t == 'held':
            p[key] = 2 if cx >= 0 else 3
        elif 12 <= cy < 25 and abs(cx) >= 4:
            p[key] = 2 if cx > 0 else 3
        elif cy < 12 and not skirted and t in ('human', 'direct'):
            p[key] = 4 if cx >= 0 else 5
    return not skirted

def _wrap(name, part):
    fn = getattr(m, name)
    def w(g, *a, **k):
        if not hasattr(g, '_force'): g._force = []
        g._force.append(part)
        try: return fn(g, *a, **k)
        finally: g._force.pop()
    setattr(m, name, w)
for _n in HEAD_FN: _wrap(_n, 1)
for _n in BODY_FN: _wrap(_n, 0)

DIRS = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
def rects(g):
    """겉면을 큰 네모로: [(부위, 방향, 면의 자리, a0, b0, a1, b1, {(a, b): 빛깔})]"""
    v, p = g.v, g.p
    out = []
    for d, (dx, dy, dz) in enumerate(DIRS):
        faces = {}   # (부위, 면의 자리) -> {(a, b): 빛깔}
        for (x, y, z), c in v.items():
            nb = (x + dx, y + dy, z + dz)
            part = p.get((x, y, z), 0)
            if nb in v and p.get(nb, 0) == part: continue        # 같은 부위의 칸이 막고 있다 (다른 부위는 머리가 돌면 드러나므로 남긴다)
            if d < 2: plane, a, b = x + (1 if dx > 0 else 0), y, z
            elif d < 4: plane, a, b = y + (1 if dy > 0 else 0), x, z
            else: plane, a, b = z + (1 if dz > 0 else 0), x, y
            faces.setdefault((part, plane), {})[(a, b)] = c
        for (part, plane), cells in faces.items():
            done = set()
            for (a, b) in sorted(cells):
                if (a, b) in done: continue
                a1 = a
                while (a1 + 1, b) in cells and (a1 + 1, b) not in done: a1 += 1
                b1 = b
                while all((aa, b1 + 1) in cells and (aa, b1 + 1) not in done for aa in range(a, a1 + 1)): b1 += 1
                col = {}
                for aa in range(a, a1 + 1):
                    for bb in range(b, b1 + 1): done.add((aa, bb)); col[(aa, bb)] = cells[(aa, bb)]
                out.append((part, d, plane, a, b, a1 + 1, b1 + 1, col))
    return out

def pack(rs):
    """네모들을 그림 한 장에 선반 쌓듯 놓는다 (둘레 한 칸씩 덧댐). (너비, 높이, [(u, v)…]) — u·v 는 덧댄 칸 안쪽의 첫 칸."""
    order = sorted(range(len(rs)), key=lambda i: -(rs[i][6] - rs[i][4]))
    for W, H in ((128, 128), (256, 128), (256, 256), (512, 256), (512, 512)):
        pos = [None] * len(rs); x = y = shelf = 0; ok = True
        for i in order:
            w, h = rs[i][5] - rs[i][3] + 2, rs[i][6] - rs[i][4] + 2
            if x + w > W: x = 0; y += shelf; shelf = 0
            if y + h > H: ok = False; break
            pos[i] = (x + 1, y + 1); x += w; shelf = max(shelf, h)
        if ok: return W, H, pos
    raise SystemExit('그림에 다 들어가지 않습니다')

def main():
    from PIL import Image
    os.makedirs(OUT, exist_ok=True); os.makedirs(TEX, exist_ok=True)
    total = 0
    for name in LOOKS:
        g = getattr(m, name)()
        legs = limbs(g)
        rs = rects(g)
        W, H, pos = pack(rs)
        im = Image.new('RGBA', (W, H), (0, 0, 0, 0)); px = im.load()
        for (part, d, plane, a0, b0, a1, b1, col), (u, v) in zip(rs, pos):
            for aa in range(a0 - 1, a1 + 1):
                for bb in range(b0 - 1, b1 + 1):
                    c = col[(min(max(aa, a0), a1 - 1), min(max(bb, b0), b1 - 1))]   # 둘레는 가장 가까운 칸의 빛깔
                    px[u + aa - a0, v + bb - b0] = (c[0], c[1], c[2], 255)
        im.save(os.path.join(TEX, name + '.png'), optimize=True)
        with open(os.path.join(OUT, name + '.bin'), 'wb') as f:
            f.write(b'RTNL' + bytes([2]) + struct.pack('>iHH', len(rs), W, H))
            for (part, d, plane, a0, b0, a1, b1, col), (u, v) in zip(rs, pos):
                f.write(struct.pack('>BBbbbbbHH', part, d, plane, a0, b0, a1, b1, u, v))
        total += len(rs)
        print('%-11s %4d quads  (head %d, arms %d, legs %d%s)  texture %dx%d' % (name, len(rs), sum(1 for r in rs if r[0] == 1), sum(1 for r in rs if r[0] in (2, 3)),
              sum(1 for r in rs if r[0] in (4, 5)), '' if legs else ' — skirt: legs stay', W, H))
    print(len(LOOKS), 'looks,', total, 'quads ->', os.path.normpath(OUT))

if __name__ == '__main__': main()
