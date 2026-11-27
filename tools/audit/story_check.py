#!/usr/bin/env python3
"""스토리 전체의 짜임 점검 (3.77, W-21): python3 tools/audit/story_check.py
- 퀘스트 연결: requires 가 있는 퀘스트인지, 돌고 도는 고리가 없는지, 처음부터 닿을 수 있는지
- 표식: requiresFlags · blockedByFlags 의 표식을 어디선가 주는지 (퀘스트 보상 · 선택지 · 자바 코드)
- 장 번호 「(i/n)」 가 순서대로인지
- 말하는 사람 이름("이름: ")이 아는 인물인지, 대답 줄(▷)이 대화(TALK) 안에만 있는지
- 글: 두 칸 띄어쓰기, 따옴표 · 괄호의 짝, 남은 조사 표시 '을(를)' 같은 자리 표시, 같은 낱말 두 번
- 진행 글의 숫자와 목표의 개수가 맞는지 (가져오기 · 잡기)
나온 것은 '문제일 수 있는 곳'이다. 사람이 보고 고친다."""
import json, os, re, glob, sys
from collections import defaultdict, deque

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
R = os.path.join(ROOT, 'src', 'main', 'resources', 'rtshop_defaults')
story = json.load(open(os.path.join(R, 'quests', 'story.json'), encoding='utf-8'))
others = {}
for f in ('tutorial.json', 'job_quests.json', 'npcs.json'):
    p = os.path.join(R, 'quests', f)
    if os.path.exists(p):
        others[f] = json.load(open(p, encoding='utf-8'))
Q = {q['id']: q for q in story['quests']}
ALLQ = dict(Q)
for f, d in others.items():
    for q in d.get('quests', []):
        ALLQ.setdefault(q['id'], q)
issues = defaultdict(list)

# ---- 표식을 주는 곳
granted = set()
for q in ALLQ.values():
    r = q.get('reward') or {}
    granted.update(r.get('flags', []))
    for c in q.get('choices', []) or []:
        granted.update(c.get('flags', []))
code_flags = set()
for jf in glob.glob(os.path.join(ROOT, 'src', 'main', 'java', '**', '*.java'), recursive=True):
    src = open(jf, encoding='utf-8').read()
    for m in re.finditer(r'setFlag\([^,]+,\s*"([a-z0-9_]+)"\s*,\s*true', src):
        code_flags.add(m.group(1))
    for m in re.finditer(r'public static final String [A-Z_]+ = "([a-z0-9_]+)"', src):
        code_flags.add(m.group(1))
    for m in re.finditer(r'"((?:story|ch\d|clue|seal|scylla|hj|unlock|order|illusioner)[a-z0-9_]*)"', src):
        code_flags.add(m.group(1))
known_flags = granted | code_flags
for q in ALLQ.values():
    for k in ('requiresFlags', 'blockedByFlags'):
        for fl in q.get(k, []) or []:
            if fl.startswith('met_') or fl.startswith('heard_'):
                continue
            if fl not in known_flags:
                issues['표식을 주는 곳이 없음'].append('%s %s: %s' % (q['id'], k, fl))
    for o in q.get('objectives', []):
        if o.get('type') == 'FLAG' and o.get('target') not in known_flags and not str(o.get('target')).startswith('abyss_floor_'):   # 심연의 층 표식은 코드가 층마다 만든다
            issues['표식 목표의 표식을 주는 곳이 없음'].append('%s: %s' % (q['id'], o.get('target')))

# ---- 연결
for q in Q.values():
    for r in q.get('requires', []) or []:
        if r not in ALLQ:
            issues['없는 퀘스트를 앞에 둠'].append('%s → %s' % (q['id'], r))
color = {}
def dfs(u, stack):
    color[u] = 1
    for v in Q.get(u, {}).get('requires', []) or []:
        if v not in Q:
            continue
        if color.get(v) == 1:
            issues['돌고 도는 연결'].append(' → '.join(stack + [v]))
        elif color.get(v) is None:
            dfs(v, stack + [v])
    color[u] = 2
sys.setrecursionlimit(10000)
for u in Q:
    if color.get(u) is None:
        dfs(u, [u])
# 처음부터 닿는가 (요구 퀘스트가 모두 닿는 퀘스트이면 닿는다; 표식은 따지지 않음)
reach = set(x for x in ALLQ if not (ALLQ[x].get('requires')))
changed = True
while changed:
    changed = False
    for x, q in ALLQ.items():
        if x in reach:
            continue
        if all(r in reach for r in q.get('requires', []) or []):
            reach.add(x)
            changed = True
for x in Q:
    if x not in reach:
        issues['닿을 수 없는 퀘스트'].append(x)

# ---- 장 번호
chains = defaultdict(list)
for q in story['quests']:
    m = re.match(r'^(.*) \((\d+)/(\d+)\)$', q.get('chain', ''))
    if m:
        chains[m.group(1)].append((int(m.group(2)), int(m.group(3)), q['id']))
for c, xs in chains.items():
    nums = [a for a, b, _ in xs]
    tot = set(b for a, b, _ in xs)
    distinct = sorted(set(nums))   # 같은 번호가 둘이면 갈래 (예: 거울 속의 연대기의 첫걸음 둘)
    if distinct != list(range(1, len(distinct) + 1)) or tot != {len(distinct)} or nums != sorted(nums):
        issues['장 번호가 어긋남'].append('%s: %s' % (c, [(a, b) for a, b, _ in xs][:6]))

# ---- 말하는 사람
names = set()
for d in [story] + list(others.values()):
    for n in d.get('npcs', []) or []:
        if isinstance(n, dict) and n.get('name'):
            names.add(n['name'])
SPK = re.compile(r'^([가-힣A-Za-z ]{2,24}): ')
speakers = defaultdict(int)
def texts(q):
    for k in ('intro', 'progress', 'done'):
        if q.get(k):
            yield k, q[k], False
    for o in q.get('objectives', []):
        if o.get('text'):
            yield 'objective', o['text'], False
        for l in o.get('lines', []) or []:
            yield 'line', l, o.get('type') in ('TALK', 'INSPECT')   # 살펴보기의 글도 대화 상자로 보인다 (대답 줄 가능)
    for c in q.get('choices', []) or []:
        yield 'choice', c.get('text', ''), False
for q in Q.values():
    for kind, t, in_talk in texts(q):
        if not isinstance(t, str):
            continue
        m = SPK.match(t)
        if m and kind == 'line':
            speakers[m.group(1)] += 1
        if t.startswith('▷') and not in_talk:
            issues['대답 줄(▷)이 대화 밖에 있음'].append('%s %s: %s' % (q['id'], kind, t[:40]))
        if '  ' in t:
            issues['두 칸 띄어쓰기'].append('%s %s: %s' % (q['id'], kind, t[:60]))
        if re.search(r'을\(를\)|이\(가\)|은\(는\)|와\(과\)|으로\(로\)', t):
            issues['조사 자리 표시가 남음'].append('%s %s: %s' % (q['id'], kind, t[:60]))
        for a, b in (('(', ')'), ('「', '」'), ('“', '”')):
            if t.count(a) != t.count(b):
                issues['괄호 · 따옴표 짝'].append('%s %s: %s' % (q['id'], kind, t[:80]))
        if t.count('"') % 2:
            issues['큰따옴표 짝'].append('%s %s: %s' % (q['id'], kind, t[:80]))
        m2 = re.search(r'(?<![가-힣])([가-힣]{2,})\s+\1(?![가-힣])', t)
        if m2 and m2.group(1) not in ('아직', '천천히', '하나', '조금', '빨리', '어서', '얼른', '네', '자', '자꾸', '오래', '멀리', '자꾸만', '그래', '정말', '좀', '아니', '아니오', '천천', '스승의'):   # '스승의 스승의 스승' 은 일부러
            issues['같은 낱말 두 번'].append('%s %s: …%s…' % (q['id'], kind, t[max(0, m2.start() - 10):m2.end() + 10]))
# 이야기에서 줄여 부르는 이름 (명단의 긴 이름과 같은 사람) · 사람이 아닌 글 (쪽지의 머리)
SHORT = {'장로 에르윈', '시청 서기 하루', '안내인 모리스', '식당 주인 로렌초', '코넬리아 선장', '견습 기록관 미아',
         '행운 상인 럭키', '선장 카스', '은행원 그레고리'}
unknown = {s: n for s, n in speakers.items() if s not in names and s not in SHORT and len(s.split()) <= 3}
issues['처음 보는 말하는 사람 (확인용)'] = ['%s ×%d' % (s, n) for s, n in sorted(unknown.items(), key=lambda x: -x[1])]

# ---- 진행 글의 숫자와 목표 개수
NUM = {'한': 1, '두': 2, '세': 3, '네': 4, '다섯': 5, '여섯': 6, '일곱': 7, '여덟': 8, '아홉': 9, '열': 10}
for q in Q.values():
    prog = q.get('progress', '')
    for o in q.get('objectives', []):
        if o.get('type') not in ('DELIVER', 'KILL') or not o.get('text') or str(o.get('mod', '')).startswith('!'):   # 모드가 없을 때의 대신 목표는 빼고
            continue
        n = o.get('count', 1)
        t = o['text']
        m = re.search(re.escape(t) + r'\s*(?:을|를)?\s*(\d+)\s*(?:개|마리|명|장|번)', prog)
        if m and int(m.group(1)) != n:
            issues['진행 글의 개수와 목표가 다름'].append('%s: "%s %s" 인데 목표는 %d' % (q['id'], t, m.group(1), n))

total = 0
for k, v in issues.items():
    if not v:
        continue
    print('=====', k, len(v))
    for x in v[:60]:
        print('  ', x)
    total += len(v)
print('합계', total)
