#!/usr/bin/env python3
"""모드팩 전체를 켠 로그(latest.log)를 요약한다 (A-16 모드팩 점검용).

쓰기: python3 .github/scripts/log_summary.py <latest.log> <out.txt>
- 오류(ERROR) · 경고(WARN) 를 낸 곳(로거)별로 세고, 같은 모양의 문장끼리 묶어 많이 나온 것부터 적는다.
- 자주 문제가 되는 것(믹스인 실패, 조합법·전리품 읽기 실패, 없는 텍스처·모델, 느린 틱)을 따로 센다.
"""
import collections
import re
import sys

src, out = sys.argv[1], sys.argv[2]
line_re = re.compile(r'^\[(\d\d:\d\d:\d\d)\] \[([^/\]]+)/(\w+)\](?: \[([^\]]+)\])?: (.*)$')
num = re.compile(r'\d+')
levels = collections.Counter()
by_logger = {'ERROR': collections.Counter(), 'WARN': collections.Counter()}
shapes = {'ERROR': collections.Counter(), 'WARN': collections.Counter()}
first = {}
# 자원 경로(efn:biped/... 같은 것)를 모드 이름만 남기고 뭉친 묶음: 같은 모드의 같은 오류 수백 줄이 한 줄이 된다
resloc = re.compile(r'\b([a-z][a-z0-9_.-]*):[a-z0-9_./#=,-]+')
coll = {'ERROR': collections.Counter(), 'WARN': collections.Counter()}
follow = {}
last_err = None
special = collections.OrderedDict((k, [0, []]) for k in [
    'mixin', 'recipe', 'loot table', 'advancement', 'tag', 'missing texture', 'missing model', 'sound', 'lang / translation',
    "can't keep up", 'exception', 'registry / missing', 'network / packet'])
keys = {
    'mixin': re.compile(r'mixin|InjectionError|Critical injection', re.I),
    'recipe': re.compile(r'recipe', re.I),
    'loot table': re.compile(r'loot.?table|loot_table', re.I),
    'advancement': re.compile(r'advancement', re.I),
    'tag': re.compile(r"\btags?\b.*(missing|not present|couldn't|unknown)|Couldn't load tag", re.I),
    'missing texture': re.compile(r'missing texture|Using missing texture|Unable to load .*texture|FileNotFoundException.*textures', re.I),
    'missing model': re.compile(r'Unable to load model|Missing model|Exception loading blockstate|missing variant', re.I),
    'sound': re.compile(r'sound', re.I),
    'lang / translation': re.compile(r'lang|translation', re.I),
    "can't keep up": re.compile(r"Can't keep up", re.I),
    'exception': re.compile(r'Exception|Caused by', re.I),
    'registry / missing': re.compile(r'Unknown registry|Missing mappings|not registered|Unknown (item|block|entity)', re.I),
    'network / packet': re.compile(r'packet|payload|channel', re.I),
}
times = []
total = 0
with open(src, encoding='utf-8', errors='replace') as f:
    for raw in f:
        total += 1
        m = line_re.match(raw.rstrip('\n'))
        if not m:
            # 오류 줄 바로 다음 줄(대개 예외 이름)을 한 줄만 붙여 둔다
            if last_err is not None and last_err not in follow and raw.strip():
                follow[last_err] = raw.strip()[:220]
            continue
        last_err = None
        t, thread, level, logger, msg = m.groups()
        times.append(t)
        levels[level] += 1
        if level not in by_logger:
            continue
        logger = logger or thread
        by_logger[level][logger] += 1
        shape = logger + ' | ' + num.sub('N', msg)[:150]
        shapes[level][shape] += 1
        first.setdefault(shape, msg[:300])
        cshape = logger + ' | ' + num.sub('N', resloc.sub(lambda mm: mm.group(1) + ':*', msg))[:170]
        coll[level][cshape] += 1
        if level == 'ERROR':
            last_err = cshape
        for k, rx in keys.items():
            if rx.search(msg) or (k == 'mixin' and 'mixin' in logger.lower()):
                special[k][0] += 1
                if len(special[k][1]) < 6 and msg[:200] not in special[k][1]:
                    special[k][1].append(msg[:200])
with open(out, 'w', encoding='utf-8') as o:
    o.write('lines: %d, levels: %s\n' % (total, dict(levels)))
    if times:
        o.write('from %s to %s\n' % (times[0], times[-1]))
    for level in ('ERROR', 'WARN'):
        o.write('\n== %s by logger (top 40)\n' % level)
        for k, v in by_logger[level].most_common(40):
            o.write('%6d  %s\n' % (v, k))
        o.write('\n== %s messages, same shape grouped (top %d)\n' % (level, 70 if level == 'ERROR' else 50))
        for k, v in shapes[level].most_common(70 if level == 'ERROR' else 50):
            o.write('%6d  %s\n' % (v, first[k] if v == 1 else k))
    o.write('\n== ERROR messages, resource paths collapsed to the mod name (all, up to 250)\n')
    for k, v in coll['ERROR'].most_common(250):
        o.write('%6d  %s\n' % (v, k))
        if k in follow:
            o.write('          -> %s\n' % follow[k])
    o.write('\n== WARN messages, resource paths collapsed to the mod name (top 120)\n')
    for k, v in coll['WARN'].most_common(120):
        o.write('%6d  %s\n' % (v, k))
    o.write('\n== by topic (ERROR + WARN)\n')
    for k, (n, ex) in special.items():
        o.write('%6d  %s\n' % (n, k))
        for e in ex[:4]:
            o.write('          e.g. %s\n' % e)
print('log summary ->', out)
