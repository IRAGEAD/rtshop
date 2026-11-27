"""성능 후보 모드 받기 (3.70.0): CurseForge 의 모드 주소 이름(slug)으로 Forge 1.20.1 용 최신 파일을 받는다.

python3 extra_mods.py <받을 폴더> <slug>[:client|:server|:both] ...
받은 파일 이름을 한 줄에 하나씩 찍는다 ("<slug> <파일 이름> <쪽>"). 못 받은 것은 "<slug> FAILED ...".
"""
import json
import os
import sys
import urllib.request

dest = sys.argv[1]
os.makedirs(dest, exist_ok=True)
for arg in sys.argv[2:]:
    slug, _, side = arg.partition(':')
    side = side or 'both'
    try:
        req = urllib.request.Request('https://api.cfwidget.com/minecraft/mc-mods/' + slug, headers={'User-Agent': 'rtshop-ci'})
        d = json.load(urllib.request.urlopen(req, timeout=60))
        files = [f for f in d.get('files', []) if '1.20.1' in f.get('versions', []) and any(v.lower() == 'forge' for v in f.get('versions', []))]
        files.sort(key=lambda f: f.get('uploaded_at', ''), reverse=True)
        if not files:
            print(slug, 'FAILED no forge 1.20.1 file')
            continue
        f = files[0]
        url = 'https://www.curseforge.com/api/v1/mods/%s/files/%s/download' % (d['id'], f['id'])
        data = urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'rtshop-ci'}), timeout=180).read()
        name = f.get('name') or (slug + '.jar')
        if not name.endswith('.jar'):
            name += '.jar'
        open(os.path.join(dest, name), 'wb').write(data)
        print(slug, name, side, d['id'], f['id'])
    except Exception as e:
        print(slug, 'FAILED', e)
