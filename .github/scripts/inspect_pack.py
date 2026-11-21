#!/usr/bin/env python3
"""Dump what RT Shop needs to know about some mods in the pack (item lists, ore generation, Cinematic Interactions classes)."""
import json, os, re, sys, zipfile
cache, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
want = {'cinematic': r'cinematic interaction', 'miningmaster': r'miningmaster', 'spelunkery': r'spelunkery'}
for key, pat in want.items():
    jars = [n for n in os.listdir(cache) if n.endswith('.jar') and re.search(pat, n.lower().replace('-', ' ').replace('_', ' '))]
    with open(os.path.join(out, key + '.txt'), 'w') as f:
        for n in jars:
            z = zipfile.ZipFile(os.path.join(cache, n))
            names = z.namelist()
            f.write('== jar ' + n + ' (' + str(len(names)) + ' entries)\n')
            for e in names:
                if e.endswith('.class'):
                    if key == 'cinematic' and '$' not in e:
                        f.write('class ' + e + '\n')
                    continue
                f.write('file ' + e + '\n')
            for e in names:
                low = e.lower()
                keep = False
                if key == 'cinematic':
                    keep = (low.endswith('.json') and ('lang/en_us' in low or '/scene' in low or 'station' in low or 'addon' in low)) or low.endswith('.toml') or low.endswith('.md')
                else:
                    keep = (low.endswith('.json') and ('worldgen/placed_feature' in low or 'worldgen/configured_feature' in low
                            or 'lang/en_us' in low or 'lang/ko_kr' in low or '/recipes/' in low or 'tags/items' in low or 'loot_tables/blocks' in low)) or low.endswith('.toml')
                if keep:
                    try:
                        f.write('---- ' + e + '\n' + z.read(e).decode('utf-8', 'replace')[:6000] + '\n')
                    except Exception as ex:
                        f.write('err ' + str(ex) + '\n')
            if key == 'cinematic':
                # 공개 API 로 보이는 클래스의 문자열 상수 (메서드 이름 등)
                for e in names:
                    if e.endswith('.class') and re.search(r'(api|addon|scene|station|registry)', e.lower()) and '$' not in e:
                        b = z.read(e)
                        strs = sorted(set(s.decode('latin1') for s in re.findall(rb'[A-Za-z_][A-Za-z0-9_/$.;()<>]{3,}', b)))
                        f.write('==== strings ' + e + '\n' + ' '.join(strs)[:5000] + '\n')
    print(key, jars)
