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


# ---- 다른 모드와의 연동 설계용: 구조(클래스·메서드 서명, 등록 이름)만 뽑는다. 코드나 그림은 싣지 않는다.
import subprocess, tempfile

def dump_structure(key, pat, sig_pat, call_pat, lang_pat, file_pat):
    jars = [n for n in os.listdir(cache) if n.endswith('.jar') and re.search(pat, n.lower().replace('-', ' ').replace('_', ' '))]
    print(key, jars)
    with open(os.path.join(out, key + '-files.txt'), 'w') as ff, open(os.path.join(out, key + '-sigs.txt'), 'w') as fs, \
            open(os.path.join(out, key + '-calls.txt'), 'w') as fc, open(os.path.join(out, key + '-data.txt'), 'w') as fd:
        for n in jars:
            z = zipfile.ZipFile(os.path.join(cache, n))
            names = z.namelist()
            ff.write('== jar ' + n + ' (' + str(len(names)) + ' entries)\n')
            for e in names:
                if e.endswith('/'):
                    continue
                if e.endswith('.class') or re.search(file_pat, e):
                    ff.write(e + '\n')
            tmp = tempfile.mkdtemp()
            classes = [e for e in names if e.endswith('.class')]
            z.extractall(tmp, classes)
            def javap(sel, extra):
                res = ''
                for i in range(0, len(sel), 40):
                    part = [os.path.join(tmp, e) for e in sel[i:i + 40]]
                    r = subprocess.run(['javap', '-p', '-constants'] + extra + part, capture_output=True, text=True)
                    res += r.stdout
                return res
            sig = [e for e in classes if re.search(sig_pat, e)]
            fs.write('== jar ' + n + ' (' + str(len(sig)) + ' of ' + str(len(classes)) + ' classes)\n')
            fs.write(javap(sig, []))
            if call_pat:
                calls = [e for e in classes if re.search(call_pat, e)]
                txt = javap(calls, ['-c'])
                keep = []
                for line in txt.split('\n'):
                    s = line.strip()
                    if not s:
                        continue
                    if not re.match(r'^\d+: ', s):
                        if s.startswith('Code:') or s.startswith('Exception table') or re.match(r'^(from|\d+\s+\d+\s+\d+)', s):
                            continue
                        keep.append(line)          # 클래스·메서드 머리줄
                    elif re.search(r'invoke|ldc|getstatic|putstatic|getfield|putfield|instanceof|checkcast|new ', s):
                        keep.append('      ' + re.sub(r'^\d+: ', '', s))
                fc.write('== jar ' + n + '\n' + '\n'.join(keep) + '\n')
            for e in names:
                low = e.lower()
                if low.endswith('mods.toml') or low.endswith('.mixins.json') or low.endswith('mixins.json') or low.endswith('pack.mcmeta'):
                    fd.write('---- ' + e + '\n' + z.read(e).decode('utf-8', 'replace')[:4000] + '\n')
                elif 'lang/en_us.json' in low:
                    try:
                        lang = json.loads(z.read(e).decode('utf-8', 'replace'))
                        fd.write('---- ' + e + ' (keys matching)\n')
                        for k, v in lang.items():
                            if re.search(lang_pat, k):
                                fd.write(k + ' = ' + str(v)[:160].replace('\n', ' ') + '\n')
                    except Exception as ex:
                        fd.write('err ' + str(ex) + '\n')

dump_structure('spiral', r'spiral', r'.', os.environ.get('SPIRAL_CALLS', ''), r'.', r'\.(json|toml|png|nbt|mcmeta|mcfunction)$')
dump_structure('cataclysm', r'cataclysm',
               r'(Ignis|Altar_?[Oo]f_?Fire|Altar|Boss_?[Mm]onster|BossMonster|CMConfig|ModEntities|ModItems|ModBlocks|ModSounds|ModTag|Flame_?Eye|ModParticle|Cm_?Boss|Animation_?Monster)',
               os.environ.get('CATACLYSM_CALLS', ''), r'^(entity\.|item\.cataclysm\.[a-z_]*(eye|ashes|ignit)|block\.cataclysm\.[a-z_]*altar|death\.)',
               r'(textures/item/[^/]*(eye|ashes)[^/]*\.png|models/item/[^/]*(eye|ashes)[^/]*\.json|loot_tables/entities/.*\.json|advancements/.*\.json)$')
