"""Scan .mca region files: which chunks exist, and per-column heights (WORLD_SURFACE, OCEAN_FLOOR).
usage: python3 -I mca_scan.py <region_dir> <out.npz>
"""
import os, struct, sys, zlib, gzip
import numpy as np

def read_tag_payload(b, p, t, want=None, path=()):
    # returns (value or None, new_p); only builds values for compounds/longarrays we want
    if t == 1: return b[p], p + 1
    if t == 2: return None, p + 2
    if t == 3: return struct.unpack_from('>i', b, p)[0], p + 4
    if t == 4: return None, p + 8
    if t == 5: return None, p + 4
    if t == 6: return None, p + 8
    if t == 7:
        n = struct.unpack_from('>i', b, p)[0]; return None, p + 4 + n
    if t == 8:
        n = struct.unpack_from('>H', b, p)[0]; return b[p + 2:p + 2 + n].decode('utf-8', 'replace'), p + 2 + n
    if t == 9:
        et = b[p]; n = struct.unpack_from('>i', b, p + 1)[0]; p += 5
        for _ in range(n):
            _, p = read_tag_payload(b, p, et)
        return None, p
    if t == 10:
        out = {}
        while True:
            tt = b[p]; p += 1
            if tt == 0: break
            n = struct.unpack_from('>H', b, p)[0]; name = b[p + 2:p + 2 + n].decode('utf-8', 'replace'); p += 2 + n
            v, p = read_tag_payload(b, p, tt)
            if v is not None: out[name] = v
        return out, p
    if t == 11:
        n = struct.unpack_from('>i', b, p)[0]; return None, p + 4 + 4 * n
    if t == 12:
        n = struct.unpack_from('>i', b, p)[0]
        arr = np.frombuffer(b, dtype='>i8', count=n, offset=p + 4).astype(np.int64)
        return arr, p + 4 + 8 * n
    raise ValueError('bad tag %d' % t)

def unpack_heights(longs):
    # 9 bits, 7 per long, 256 entries
    out = np.zeros(256, np.int32)
    i = 0
    for L in longs:
        v = int(L) & 0xFFFFFFFFFFFFFFFF
        for k in range(7):
            if i >= 256: break
            out[i] = (v >> (9 * k)) & 0x1FF
            i += 1
    return out.reshape(16, 16)  # [z][x]

def main():
    rdir, outp = sys.argv[1], sys.argv[2]
    chunks = []
    heights = {}
    for fn in sorted(os.listdir(rdir)):
        if not fn.endswith('.mca'): continue
        _, rx, rz, _ = fn.split('.')
        rx, rz = int(rx), int(rz)
        data = open(os.path.join(rdir, fn), 'rb').read()
        if len(data) < 8192: continue
        for i in range(1024):
            off = int.from_bytes(data[i * 4:i * 4 + 3], 'big')
            if off == 0: continue
            p = off * 4096
            ln = struct.unpack_from('>i', data, p)[0]
            comp = data[p + 4]
            raw = data[p + 5:p + 4 + ln]
            cx, cz = rx * 32 + (i & 31), rz * 32 + (i >> 5)
            chunks.append((cx, cz))
            try:
                nb = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw) if comp == 1 else raw
                assert nb[0] == 10
                n = struct.unpack_from('>H', nb, 1)[0]
                root, _ = read_tag_payload(nb, 3 + n, 10)
                hm = root.get('Heightmaps', {})
                ws = hm.get('WORLD_SURFACE'); of = hm.get('OCEAN_FLOOR')
                if ws is not None and of is not None:
                    heights[(cx, cz)] = (unpack_heights(ws) - 64, unpack_heights(of) - 64)
            except Exception as e:
                pass
    ch = np.array(chunks)
    x0, z0 = ch[:, 0].min(), ch[:, 1].min()
    x1, z1 = ch[:, 0].max(), ch[:, 1].max()
    W, H = (x1 - x0 + 1) * 16, (z1 - z0 + 1) * 16
    ws = np.full((H, W), -999, np.int16); of = np.full((H, W), -999, np.int16)
    for (cx, cz), (a, b) in heights.items():
        ox, oz = (cx - x0) * 16, (cz - z0) * 16
        ws[oz:oz + 16, ox:ox + 16] = a; of[oz:oz + 16, ox:ox + 16] = b
    np.savez_compressed(outp, chunks=ch, origin=np.array([x0 * 16, z0 * 16]), ws=ws, of=of)
    print('chunks', len(ch), 'with heights', len(heights), 'chunk x', x0, x1, 'z', z0, z1, 'blocks x', x0 * 16, (x1 + 1) * 16 - 1, 'z', z0 * 16, (z1 + 1) * 16 - 1)

if __name__ == '__main__':
    main()
