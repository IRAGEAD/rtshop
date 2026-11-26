"""NPC 외형 (시안 C: 사람 몸 + 인물별 입체 장식) 을 게임 파일로 뽑는다 (3.61).
python3 tools/gen_npc_looks.py   →  src/main/resources/assets/rtshop/npc_looks/<모습>.bin

모습의 원본은 tools/mock/npc_mock.py 의 인물 함수(반 픽셀 칸으로 쌓은 상자들)다. 여기서는 그 칸들의 겉면만 모아,
같은 빛깔이 이어진 면을 큰 네모로 합쳐 적는다. 화면 쪽(client/NpcLookClient)은 이 네모들을 그대로 그린다.

파일 형식 (바이트): 'RTNL' · 판(1) · 네모 수(4, 큰 끝) · 네모마다 10바이트
  [부위 0=몸 1=머리][방향 0:+x 1:-x 2:+y 3:-y 4:+z 5:-z][면의 자리][a0][b0][a1][b1][R][G][B]   (자리는 반 픽셀, 부호 있는 1바이트)
  ±x 면: a = y, b = z / ±y 면: a = x, b = z / ±z 면: a = x, b = y.  앞은 +z, 발바닥은 y = 0, 머리의 축은 y = 48(목).
머리(얼굴·머리카락·수염·모자·안경)는 따로 묶여 플레이어를 바라보며 돈다. 그 밖은 몸.
"""
import importlib.util, os, struct, sys
here = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('npc_mock', os.path.join(here, 'mock', 'npc_mock.py'))
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
U = m.U
OUT = os.path.join(here, '..', 'src', 'main', 'resources', 'assets', 'rtshop', 'npc_looks')

LOOKS = ['erwin', 'isabel', 'roran', 'felix', 'bram', 'oscar', 'haru', 'morris', 'lucky',
         'm_farmer', 'm_food', 'm_fish', 'm_gear', 'm_general', 'm_land', 'm_bank', 'm_ticket', 'm_raid',
         'leon', 'bron', 'garen', 'lia', 'elis', 'garret', 'theo', 'adel', 'raid_guide',
         'albert', 'lorenzo', 'eli']
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
def quads(g):
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
                c = cells[(a, b)]
                a1 = a
                while (a1 + 1, b) in cells and (a1 + 1, b) not in done and cells[(a1 + 1, b)] == c: a1 += 1
                b1 = b
                while all((aa, b1 + 1) in cells and (aa, b1 + 1) not in done and cells[(aa, b1 + 1)] == c for aa in range(a, a1 + 1)): b1 += 1
                for aa in range(a, a1 + 1):
                    for bb in range(b, b1 + 1): done.add((aa, bb))
                out.append((part, d, plane, a, b, a1 + 1, b1 + 1, c[0], c[1], c[2]))
    return out

def main():
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for name in LOOKS:
        g = getattr(m, name)()
        q = quads(g)
        with open(os.path.join(OUT, name + '.bin'), 'wb') as f:
            f.write(b'RTNL' + bytes([1]) + struct.pack('>i', len(q)))
            for r in q:
                f.write(struct.pack('>BBbbbbbBBB', *r))
        total += len(q)
        print('%-11s %5d quads  (head %d)' % (name, len(q), sum(1 for r in q if r[0] == 1)))
    print(len(LOOKS), 'looks,', total, 'quads ->', os.path.normpath(OUT))

if __name__ == '__main__': main()
