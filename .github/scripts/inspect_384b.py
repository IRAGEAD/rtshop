#!/usr/bin/env python3
"""3.84 (11-29 요청 목록) 둘째 살펴보기: 모드 안의 설정 · 아이템 · 이벤트를 글로 적는다.

python3 inspect_384b.py <packcache> <out.txt>
"""
import os
import re
import subprocess
import sys
import tempfile
import zipfile

PACK, OUT = sys.argv[1], sys.argv[2]
jars = sorted(os.path.join(PACK, f) for f in os.listdir(PACK) if f.endswith('.jar'))
out = open(OUT, 'w', encoding='utf-8')


def w(*a):
    print(*a, file=out)
    out.flush()


def jar_of(*words):
    for j in jars:
        b = os.path.basename(j).lower()
        if all(x.lower() in b for x in words):
            return j
    return None


def mod_ids(j):
    try:
        z = zipfile.ZipFile(j)
        t = z.read('META-INF/mods.toml').decode('utf8', 'replace')
        return re.findall(r'modId\s*=\s*"([^"]+)"', t)
    except Exception:
        return []


def names(j):
    try:
        return zipfile.ZipFile(j).namelist()
    except Exception:
        return []


def read(j, n, limit=None):
    try:
        b = zipfile.ZipFile(j).read(n)
        s = b.decode('utf8', 'replace')
        return s if limit is None else s[:limit]
    except Exception as e:
        return f'<{e}>'


def javap(j, cls_names, grep=None, head=400):
    """cls_names: 'a/b/C.class' 꼴. javap -c -p -constants 의 결과에서 grep 에 맞는 줄만."""
    with tempfile.TemporaryDirectory() as d:
        z = zipfile.ZipFile(j)
        for c in cls_names:
            try:
                z.extract(c, d)
            except Exception as e:
                w(f'#### {c}: extract failed {e}')
                continue
            try:
                r = subprocess.run(['javap', '-c', '-p', '-constants', os.path.join(d, c)], capture_output=True, text=True, timeout=120)
                lines = r.stdout.splitlines()
            except Exception as e:
                w(f'#### {c}: javap failed {e}')
                continue
            w(f'#### {c} ({len(lines)} lines)')
            n = 0
            for ln in lines:
                if grep is None or re.search(grep, ln):
                    w('   ' + ln[:260])
                    n += 1
                    if n >= head:
                        w('   ...')
                        break


def classes_with(j, pattern, name_re=None, limit=40):
    """바이트코드에 pattern(바이트열 정규식)이 들어 있는 클래스."""
    hits = []
    z = zipfile.ZipFile(j)
    for n in z.namelist():
        if not n.endswith('.class'):
            continue
        if name_re and not re.search(name_re, n):
            continue
        try:
            if re.search(pattern, z.read(n)):
                hits.append(n)
        except Exception:
            pass
        if len(hits) >= limit:
            break
    return hits


def lang(j, loc='en_us', key_re=None, limit=200):
    res = []
    for n in names(j):
        if re.search(rf'assets/[^/]+/lang/{loc}\.json$', n):
            for ln in read(j, n).splitlines():
                if key_re is None or re.search(key_re, ln):
                    res.append(f'{n.split("/")[1]} :: {ln.strip()[:240]}')
                    if len(res) >= limit:
                        return res
    return res


def section(t):
    w('')
    w('######## ' + t)


# ---------------------------------------------------------------- A. Small Ships 설정 (바닐라 보트 느려짐)
section('A. smallships config')
j = jar_of('smallships')
if j:
    cfg = [n for n in names(j) if re.search(r'smallships/config/.*\.class$', n)]
    w('config classes:', cfg)
    javap(j, cfg, grep=r'ldc|Method .*(define|comment|push|pop|translation|Value|get)|static .*Value|getstatic|putstatic|public static', head=260)
    w('-- mods.toml modIds', mod_ids(j))
else:
    w('no smallships jar')

# ---------------------------------------------------------------- B. 카지노 (칩)
section('B. casino')
j = jar_of('casino') or jar_of('vegas')
if j:
    w('jar', os.path.basename(j), 'modIds', mod_ids(j))
    for ln in lang(j, 'en_us', r'"(item|block)\.', 400):
        w(ln)
    w('-- ko_kr')
    for ln in lang(j, 'ko_kr', r'"(item|block)\.', 400):
        w(ln)
    rec = [n for n in names(j) if re.search(r'^data/[^/]+/recipes?/.*\.json$', n)]
    w('-- recipes', len(rec))
    for n in rec[:400]:
        w(n, '::', re.sub(r'\s+', ' ', read(j, n, 400)))
    w('-- loot tables mentioning chip')
    for n in names(j):
        if n.startswith('data/') and n.endswith('.json') and 'loot_tables' in n:
            s = read(j, n, 4000)
            if 'chip' in s:
                w(n, '::', re.sub(r'\s+', ' ', s)[:300])
    w('-- classes with Chip in the name')
    for n in names(j):
        if n.endswith('.class') and re.search(r'(?i)chip|token|coin|currency|exchange|cashier', n):
            w(n)
    w('-- classes naming PlayerLoggedInEvent / addItem')
    for n in classes_with(j, rb'PlayerLoggedInEvent|m_36054_|m_36356_'):
        w(n)
else:
    w('no casino jar')

# ---------------------------------------------------------------- C. Spiral Abyss 단축키 · 가방 단추
section('C. spiral abyss key + inventory button')
j = jar_of('spiral')
if j:
    w('modIds', mod_ids(j))
    cs = [n for n in names(j) if re.search(r'(AbyssClientPlatformImpl|JournalClientHandler|AbyssKeyHandler|InventoryButton|ClientEvents|ClientForgeHandler|ClientHandler)[^/]*\.class$', n)]
    w('classes', cs)
    javap(j, cs, grep=r'bipush|sipush|iconst|ldc|invoke|getstatic|putstatic|new |Method|lambda', head=500)
    w('-- lang (key / screen / journal)')
    for ln in lang(j, 'en_us', r'(?i)key\.|journal|screen\.', 80):
        w(ln)
    for ln in lang(j, 'ko_kr', r'(?i)key\.|journal|screen\.', 80):
        w(ln)
else:
    w('no spiral jar')

# ---------------------------------------------------------------- D. '공기 부족' 찾기
section('D. lang lines with 부족 / 공기 (ko_kr) and not enough / missing (en_us)')
for j in jars:
    hits = lang(j, 'ko_kr', r'부족|공기', 25)
    if hits:
        w(f'== {os.path.basename(j)}')
        for h in hits:
            w('   ' + h)
for j in jars:
    b = os.path.basename(j).lower()
    if any(x in b for x in ('spell', 'wizard', 'paladin', 'rogue', 'archer', 'arsenal', 'jewel', 'relic', 'rune', 'iron', 'skill')):
        hits = lang(j, 'en_us', r'(?i)not enough|missing|insufficient|out of|requires|cannot cast|can\'t cast', 30)
        if hits:
            w(f'== en {os.path.basename(j)}')
            for h in hits:
                w('   ' + h)

# ---------------------------------------------------------------- E. Epic Fight 스킬북 조합법
section('E. epic fight skill books (recipes and items)')
for j in jars:
    ids = mod_ids(j)
    b = os.path.basename(j).lower()
    if not any(('epic' in x or x in ('efn', 'efn_enhance', 'wom', 'p1nero_ec', 'invincible')) for x in ids) and 'epic' not in b and 'nightfall' not in b and 'miracle' not in b:
        continue
    w(f'== {os.path.basename(j)} {ids}')
    for n in names(j):
        if re.search(r'^data/[^/]+/recipes?/.*\.json$', n):
            s = read(j, n, 3000)
            if re.search(r'(?i)skill', s + n):
                w('   recipe', n, '::', re.sub(r'\s+', ' ', s)[:400])
    for ln in lang(j, 'en_us', r'(?i)"item\.[^"]*(skill|book|scroll)', 40):
        w('   ' + ln)

# ---------------------------------------------------------------- F. Quark 음식 툴팁이 부르는 메서드
section('F. quark food tooltips')
j = jar_of('quark')
if j:
    cs = [n for n in names(j) if re.search(r'(?i)FoodTooltip[^/]*\.class$', n)]
    w('classes', cs)
    javap(j, cs, grep=r'getFoodProperties|m_41473_|m_38744_|m_38745_|Method|invoke', head=200)
else:
    w('no quark jar')

# ---------------------------------------------------------------- G. Kaleidoscope ImmersiveEating
section('G. kaleidoscope immersive eating')
j = jar_of('immersiveeating') or jar_of('immersive', 'eating')
if j:
    w('modIds', mod_ids(j))
    for n in names(j):
        if n.endswith('.json') and ('mixin' in n.lower()) and '/' not in n:
            w('--', n, re.sub(r'\s+', ' ', read(j, n, 3000)))
    cs = [n for n in names(j) if n.endswith('.class')]
    w('classes', len(cs), cs[:120])
    eat = classes_with(j, rb'FoodData|m_38707_|m_38712_|getFoodProperties|m_41473_|LivingEntityUseItemEvent')
    w('food classes', eat)
    javap(j, eat[:8], grep=r'getFoodProperties|m_41473_|m_38744_|m_38745_|m_38707_|m_38712_|FoodData|Method|invoke|ldc', head=150)
    for ln in lang(j, 'en_us', None, 80):
        w(ln)
else:
    w('no immersive eating jar')

# ---------------------------------------------------------------- H. 펫 모드 생물 · 스폰
section('H. pet mods: entities and spawn modifiers')
for word in ('hamster', 'critters', 'tiny_dragon', 'tinydragon', 'companions_tiny', 'fantasymounts', 'fantasy_mounts', 'fantasy-mounts'):
    j = jar_of(word)
    if not j:
        continue
    w(f'== {os.path.basename(j)} {mod_ids(j)}')
    for ln in lang(j, 'en_us', r'"entity\.', 60):
        w('   ' + ln)
    for n in names(j):
        if re.search(r'(biome_modifier|spawn|biome)/.*\.json$', n) or re.search(r'forge/biome_modifier', n):
            w('   spawn', n, '::', re.sub(r'\s+', ' ', read(j, n, 600)))

# ---------------------------------------------------------------- I. spmovesets 의 도착 단계
section('I. spmovesets SurvivalProgress')
j = jar_of('spmovesets')
if j:
    cs = [n for n in names(j) if re.search(r'survival/SurvivalProgress[^/]*\.class$', n) or re.search(r'survival/SurvivalItems\.class$', n)]
    javap(j, cs, grep=r'ldc|invoke|getstatic|Method|ARRIVAL|case|tableswitch', head=300)

# ---------------------------------------------------------------- J. Aquamirae 아이템
section('J. aquamirae items')
j = jar_of('aquamirae')
if j:
    w(os.path.basename(j), mod_ids(j))
    for ln in lang(j, 'en_us', r'"item\.aquamirae\.[a-z_]+"', 150):
        w('   ' + ln)

# ---------------------------------------------------------------- K. Nightfall Enhance
section('K. nightfall enhance')
for j in jars:
    b = os.path.basename(j).lower()
    if 'nightfall' in b:
        w(f'== {os.path.basename(j)} {mod_ids(j)}')
        for ln in lang(j, 'en_us', r'"(item|block|entity)\.', 120):
            w('   ' + ln)
        rec = [n for n in names(j) if re.search(r'^data/[^/]+/recipes?/.*\.json$', n)]
        w('   recipes', len(rec), rec[:60])

# ---------------------------------------------------------------- L. 오른쪽 클릭 수확 (뼛가루 문제)
section('L. right-click harvest handlers')
for word in ('better_farming', 'quark', 'farmersdelight', 'FarmersDelight', 'farm_and_charm', 'handson'):
    j = jar_of(word)
    if not j:
        continue
    hs = classes_with(j, rb'RightClickBlock', name_re=r'(?i)harvest|farm|crop|click|event', limit=20)
    w(f'== {os.path.basename(j)} {mod_ids(j)} handlers: {hs}')
    javap(j, hs[:6], grep=r'RuntimeVisibleAnnotations|SubscribeEvent|priority|EventPriority|BoneMeal|m_41720_|isEmpty|m_41619_|instanceof|invoke.*(destroy|m_46961_|m_7471_|m_46953_|setBlock|m_7731_|BonemealableBlock|BoneMealItem)|Method', head=200)

# ---------------------------------------------------------------- M. Ender Dragon Plus 다시 소환
section('M. ender dragon plus')
j = jar_of('enderdragon') or jar_of('dragonplus')
if j:
    w(os.path.basename(j), mod_ids(j))
    for n in names(j):
        if re.search(r'(?i)config', n) and n.endswith('.class'):
            w('  ', n)

out.close()
