#!/usr/bin/env python3
"""Download the mod files of a CurseForge pack manifest (project id / file id pairs) for a CI compatibility test."""
import json, os, sys, time, urllib.parse, urllib.request

def main():
    listing, cache, mods = sys.argv[1], sys.argv[2], sys.argv[3]
    skip = [s.lower() for s in sys.argv[4].split(',') if s] if len(sys.argv) > 4 else []
    os.makedirs(cache, exist_ok=True)
    os.makedirs(mods, exist_ok=True)
    index_path = os.path.join(cache, 'index.json')
    index = json.load(open(index_path)) if os.path.exists(index_path) else {}
    files = json.load(open(listing))
    ok, failed = 0, []
    for pid, fid in files:
        key = f'{pid}/{fid}'
        name = index.get(key)
        if not name or not os.path.exists(os.path.join(cache, name)):
            url = f'https://www.curseforge.com/api/v1/mods/{pid}/files/{fid}/download'
            try:
                req = urllib.request.Request(url, headers={'User-Agent': 'rtshop-ci (github.com/IRAGEAD/rtshop)'})
                with urllib.request.urlopen(req, timeout=120) as r:
                    name = urllib.parse.unquote(os.path.basename(urllib.parse.urlparse(r.geturl()).path))
                    data = r.read()
                if not name or len(data) < 200:
                    raise RuntimeError('empty response')
                with open(os.path.join(cache, name), 'wb') as f:
                    f.write(data)
                index[key] = name
                time.sleep(0.3)
            except Exception as ex:
                failed.append(f'{key}: {ex}')
                continue
        ok += 1
    json.dump(index, open(index_path, 'w'))
    placed, skipped, other = [], [], []
    for key, name in sorted(index.items(), key=lambda kv: kv[1].lower()):
        src = os.path.join(cache, name)
        if not os.path.exists(src):
            continue
        if not name.lower().endswith('.jar'):
            other.append(name)
            continue
        if any(s in name.lower() for s in skip):
            skipped.append(name)
            continue
        dst = os.path.join(mods, name)
        if not os.path.exists(dst):
            with open(src, 'rb') as a, open(dst, 'wb') as b:
                b.write(a.read())
        placed.append(name)
    print(f'downloaded {ok}/{len(files)}; jars placed {len(placed)}; skipped {len(skipped)}; non-jar {len(other)}')
    print('non-jar:', ', '.join(other))
    print('skipped:', ', '.join(skipped))
    print('jars:', ', '.join(placed))
    for f in failed[:20]:
        print('FAILED', f)

main()
