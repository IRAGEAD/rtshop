"""Turn a map screenshot (MCA Selector view with the 512-block region grid) into an MCA Selector chunk selection.

usage: python3 -I map_select.py <screenshot.png> <old_selection.csv> <out.csv> <out.png>

The screenshot the user sent on 11-28 14:19 (whole round island around the town):
the region grid lines were found at these pixel columns / rows, and matched to world coordinates
by lining the water up with the earlier region scan (tools/transplant/mca_scan.py, 93% agreement).
Steps: chunk exists = more than half of its pixels are not black -> biggest connected piece ->
fill holes inside -> cut thin strips hanging off the edge (opening with a 5-chunk disk) -> csv + picture.
"""
import sys
from collections import deque
import numpy as np
from PIL import Image, ImageDraw, ImageFont

COLS = ([89, 180, 272, 364, 455, 547], [-25600, -25088, -24576, -24064, -23552, -23040])
ROWS = ([58, 149, 241, 333, 424, 516], [-6656, -6144, -5632, -5120, -4608, -4096])
OPEN_R = 5
FONT = '/usr/share/fonts/opentype/noto/NotoSansCJK-%s.ttc'


def font(size, bold=False):
    return ImageFont.truetype(FONT % ('Bold' if bold else 'Regular'), size, index=1)


def disk(r):
    y, x = np.mgrid[-r:r + 1, -r:r + 1]
    return (x * x + y * y) <= r * r + r * 0.5


def morph(m, k, grow):
    r = k.shape[0] // 2
    h, w = m.shape
    p = np.pad(m, r, constant_values=False)
    out = np.zeros_like(m) if grow else np.ones_like(m)
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            if k[dy + r, dx + r]:
                s = p[r + dy:r + dy + h, r + dx:r + dx + w]
                out = (out | s) if grow else (out & s)
    return out


def pieces(m, eight=True):
    lab = np.zeros(m.shape, int)
    sizes = [0]
    steps = [(a, b) for a in (-1, 0, 1) for b in (-1, 0, 1) if (a or b) and (eight or a == 0 or b == 0)]
    for j, i in zip(*np.nonzero(m)):
        if lab[j, i]:
            continue
        n = len(sizes)
        lab[j, i] = n
        q = deque([(j, i)])
        c = 0
        while q:
            y, x = q.popleft()
            c += 1
            for dy, dx in steps:
                yy, xx = y + dy, x + dx
                if 0 <= yy < m.shape[0] and 0 <= xx < m.shape[1] and m[yy, xx] and not lab[yy, xx]:
                    lab[yy, xx] = n
                    q.append((yy, xx))
        sizes.append(c)
    return lab, sizes


def main():
    shot, old_csv, out_csv, out_png = sys.argv[1:5]
    im = Image.open(shot).convert('RGB')
    a = np.asarray(im).astype(int)
    H, W, _ = a.shape
    kx, bx = np.polyfit(COLS[1], COLS[0], 1)
    kz, bz = np.polyfit(ROWS[1], ROWS[0], 1)
    px = lambda X: bx + kx * X
    pz = lambda Z: bz + kz * Z
    nonblack = a.sum(2) > 24
    yy, xx = np.mgrid[0:H, 0:W]
    nonblack &= ~((yy >= 517) & (xx <= 88) & (a[..., 0] == 51) & (a[..., 1] == 51) & (a[..., 2] == 51))  # grey box
    nonblack[:3, :] = False  # window edge
    cx0, cx1 = int(np.floor((0 - bx) / kx / 16)), int(np.ceil((W - bx) / kx / 16))
    cz0, cz1 = int(np.floor((0 - bz) / kz / 16)), int(np.ceil((H - bz) / kz / 16))
    nx, nz = cx1 - cx0 + 1, cz1 - cz0 + 1
    exist = np.zeros((nz, nx), bool)
    for j in range(nz):
        y0, y1 = pz((cz0 + j) * 16), pz((cz0 + j) * 16 + 16)
        ys = [y for y in range(int(y0), int(y1) + 2) if 0 <= y < H and y0 <= y + 0.5 < y1]
        for i in range(nx):
            x0, x1 = px((cx0 + i) * 16), px((cx0 + i) * 16 + 16)
            xs = [x for x in range(int(x0), int(x1) + 2) if 0 <= x < W and x0 <= x + 0.5 < x1]
            if ys and xs:
                exist[j, i] = nonblack[np.ix_(ys, xs)].mean() >= 0.5
    lab, sizes = pieces(exist)
    body = lab == int(np.argmax(sizes))
    outside, _ = pieces(~body, eight=False)
    edge_labels = set(outside[0, :]) | set(outside[-1, :]) | set(outside[:, 0]) | set(outside[:, -1])
    filled = body | ~np.isin(outside, list(edge_labels - {0}))
    k = disk(OPEN_R)
    opened = morph(morph(filled, k, False), k, True)
    ol, osz = pieces(opened, eight=False)
    opened = ol == int(np.argmax(osz))
    final = filled & morph(opened, disk(1), True)

    # csv: whole regions as "rx;rz", the rest one chunk per line (absolute chunk coords)
    sel = [(cx0 + i, cz0 + j) for j, i in zip(*np.nonzero(final))]
    by_region = {}
    for cx, cz in sel:
        by_region.setdefault((cx >> 5, cz >> 5), []).append((cx, cz))
    lines = []
    for (rx, rz) in sorted(by_region):
        cs = by_region[(rx, rz)]
        if len(cs) == 1024:
            lines.append('%d;%d' % (rx, rz))
        else:
            lines += ['%d;%d;%d;%d' % (rx, rz, cx, cz) for cx, cz in sorted(cs)]
    open(out_csv, 'w').write('\n'.join(lines) + '\n')

    real = final & exist
    xs_, zs_ = np.nonzero(final)[1] + cx0, np.nonzero(final)[0] + cz0
    stats = dict(selected=int(final.sum()), existing=int(real.sum()), holes=int((final & ~exist).sum()),
                 cut=int((exist & ~final).sum()), regions=len(by_region), full_regions=sum(len(v) == 1024 for v in by_region.values()),
                 bx=(int(xs_.min()) * 16, int(xs_.max()) * 16 + 15), bz=(int(zs_.min()) * 16, int(zs_.max()) * 16 + 15),
                 chunk_x=(int(xs_.min()), int(xs_.max())), chunk_z=(int(zs_.min()), int(zs_.max())),
                 region_files=sorted(by_region))

    # old selection outline
    old = set()
    for ln in open(old_csv):
        p = ln.strip().split(';')
        if len(p) == 4:
            old.add((int(p[2]), int(p[3])))

    S = 2
    TOP = 196
    big = im.resize((W * S, H * S), Image.NEAREST).convert('RGBA')
    ov = Image.new('RGBA', big.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)

    def rect(cx, cz):
        return px(cx * 16) * S, pz(cz * 16) * S, px(cx * 16 + 16) * S, pz(cz * 16 + 16) * S

    for j, i in zip(*np.nonzero(exist & ~final)):
        d.rectangle(rect(cx0 + i, cz0 + j), fill=(0, 0, 0, 165))

    def outline(cells, col, width):
        for cx, cz in cells:
            x0, y0, x1, y1 = rect(cx, cz)
            if (cx, cz - 1) not in cells: d.line([x0, y0, x1, y0], fill=col, width=width)
            if (cx, cz + 1) not in cells: d.line([x0, y1, x1, y1], fill=col, width=width)
            if (cx - 1, cz) not in cells: d.line([x0, y0, x0, y1], fill=col, width=width)
            if (cx + 1, cz) not in cells: d.line([x1, y0, x1, y1], fill=col, width=width)

    outline(set(sel), (255, 45, 45, 255), 3)
    outline(old, (255, 214, 64, 255), 2)
    pic = Image.alpha_composite(big, ov)
    canvas = Image.new('RGB', (pic.width, pic.height + TOP), (24, 22, 26))
    canvas.paste(pic.convert('RGB'), (0, TOP))
    d = ImageDraw.Draw(canvas)
    d.text((22, 14), '베스탈리아 이식 범위 — 원 모양 섬 전체', font=font(34, True), fill=(255, 214, 64))
    w_km = (stats['bx'][1] - stats['bx'][0] + 1) / 1000
    h_km = (stats['bz'][1] - stats['bz'][0] + 1) / 1000
    rows = [
        '빨간 선 안 = 옮길 범위: 실제로 있는 청크 %s개 (가로 %.1f km × 세로 %.1f km, 지역 파일 %d개)' % (format(stats['existing'], ','), w_km, h_km, stats['regions']),
        '노란 선 = 지난번 범위 (5,216개 · 마을과 북쪽 큰 산) · 어둡게 = 바깥에 흩어진 조각과 가는 띠 (뺌, %s개)' % format(stats['cut'], ','),
        '남쪽 끝 (z −4096 아래)과 원 안의 검은 칸 %s개는 원래 맵에 땅이 없어 새 맵의 땅이 그대로 남습니다' % format(stats['holes'], ','),
    ]
    for n, t in enumerate(rows):
        d.text((24, 66 + n * 40), t, font=font(21), fill=(232, 228, 220))
    # coordinates at the frame
    f = font(16)
    for X, gx in zip(COLS[1], COLS[0]):
        d.text((gx * S + 4, TOP + 6), 'x %s' % format(X, ','), font=f, fill=(200, 200, 200))
    for Z, gz in zip(ROWS[1], ROWS[0]):
        d.text((6, TOP + gz * S + 4), 'z %s' % format(Z, ','), font=f, fill=(200, 200, 200))
    canvas.save(out_png, optimize=True)
    for k_, v in stats.items():
        print(k_, v)


if __name__ == '__main__':
    main()
