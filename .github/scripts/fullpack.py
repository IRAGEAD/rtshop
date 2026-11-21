#!/usr/bin/env python3
"""Download the mod files of a CurseForge pack manifest (project id / file id pairs) for a CI compatibility test."""
import json, os, re, sys, time, urllib.parse, urllib.request, zipfile

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
    # 모드팩 overrides/mods 에 직접 넣은 모드: Modrinth 에서 같은 파일(sha1)을 찾아 받는다
    overrides = set()
    ov_path = sys.argv[5] if len(sys.argv) > 5 else ''
    if ov_path and os.path.exists(ov_path):
        ua = {'User-Agent': 'rtshop-ci (github.com/IRAGEAD/rtshop)'}
        for name, sha in json.load(open(ov_path)).items():
            dst_name = name.replace(' ', '_')
            path = os.path.join(cache, dst_name)
            if not os.path.exists(path):
                try:
                    v = json.load(urllib.request.urlopen(urllib.request.Request(
                        f'https://api.modrinth.com/v2/version_file/{sha}?algorithm=sha1', headers=ua), timeout=60))
                    f = next((x for x in v['files'] if x['hashes'].get('sha1') == sha), v['files'][0])
                    req = urllib.request.Request(f['url'], headers=ua)
                    with urllib.request.urlopen(req, timeout=300) as r:
                        data = r.read()
                    with open(path, 'wb') as out:
                        out.write(data)
                except Exception as ex:
                    print('OVERRIDE MISSING', name, ex)
                    continue
            index['override/' + dst_name] = dst_name
            overrides.add(dst_name)
        json.dump(index, open(index_path, 'w'))
    # 같은 모드가 매니페스트와 overrides 에 모두 있으면 overrides 쪽(모드팩에 실제로 들어간 파일)만 쓴다
    def mod_ids(path):
        try:
            with zipfile.ZipFile(path) as z:
                toml = z.read('META-INF/mods.toml').decode('utf-8', 'replace')
            return set(re.findall(r'modId\s*=\s*"([^"]+)"', toml))
        except Exception:
            return set()
    ov_ids = set()
    for n in overrides:
        ov_ids |= mod_ids(os.path.join(cache, n))
    dropped = set()
    for key, name in index.items():
        if name in overrides or not name.lower().endswith('.jar'):
            continue
        ids = mod_ids(os.path.join(cache, name))
        if ids and ids & ov_ids:
            dropped.add(name)
    print('replaced by overrides:', ', '.join(sorted(dropped)))
    client = sorted({n for n in index.values() if n.lower().endswith('.jar') and n not in dropped
                     and os.path.exists(os.path.join(cache, n))})
    open(os.path.join(cache, 'client_mods.txt'), 'w').write('\n'.join(client) + '\n')
    placed, skipped, other = [], [], []
    for key, name in sorted(index.items(), key=lambda kv: kv[1].lower()):
        src = os.path.join(cache, name)
        if not os.path.exists(src):
            continue
        if not name.lower().endswith('.jar'):
            other.append(name)
            continue
        if name in dropped:
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
