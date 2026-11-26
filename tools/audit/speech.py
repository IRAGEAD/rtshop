# 말투 점검: 인물마다 문장 끝(종결 어미)을 갈래로 나눠, 그 인물의 주된 말투와 다른 문장을 뽑아 보여 준다.
#   python3 tools/audit/speech.py            → 인물별 갈래 수와 어긋난 문장
#   python3 tools/audit/speech.py <인물>     → 그 인물의 문장 전부 (갈래와 함께)
# 갈래는 어림이다 (어미가 겹치는 말이 많다): 뽑힌 문장은 사람이 읽고 판단한다.
import json, os, re, sys, collections
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'resources', 'rtshop_defaults')
def load(p): return json.load(open(os.path.join(R, p), encoding='utf-8'))
NAME2KEY = {}
def canon(k): return k.replace('quest_', '').replace('shop_', 's_') if k else k
lines = collections.defaultdict(list)     # 인물 → [(어디, 문장)]
def add(who, where, text):
    if not isinstance(text, str) or not text.strip(): return
    text = re.sub(r'\([^)]*\)', ' ', text)                     # (지문)은 뺀다
    text = re.sub(r'["“”][^"“”]*["“”]', ' ', text)             # 인용한 글은 뺀다
    text = re.sub(r"'[^']*'", ' ', text)
    for s in re.split(r'(?<=[.!?…])\s+', text):
        s = s.strip()
        if len(s) >= 2: lines[who].append((where, s))
def ko(e):
    if isinstance(e, dict): return e.get('ko') or e.get('text')
    return e
def spoken(default, where, e):
    e = ko(e)
    if isinstance(e, dict): e = ko(e)
    if not isinstance(e, str): return
    if e.startswith('▷'): return
    m = re.match(r'^([^:：]{2,14}): (.*)$', e)
    if m: add(NAME2KEY.get(m.group(1).strip(), m.group(1).strip()), where, m.group(2))
    elif default: add(default, where, e)
# 이름 → 열쇠
st = load('quests/story.json'); dj = load('dialogue.json')
for f in ('quests/story.json', 'quests/npcs.json', 'quests/tutorial.json', 'quests/job_quests.json'):
    try:
        for n in load(f).get('npcs', []): NAME2KEY[n['name']] = canon('quest_' + n['id'])
    except Exception: pass
NAME2KEY.update({'안내인 모리스': 'guide', '어부 오스카': 's_fisher', '시청 서기 하루': 'attendance', '직업 안내인 알버트': 'job', '대장장이 브람': 'bram', '지도 제작자 펠릭스': 'felix'})
for k, n in dj['npcs'].items():
    who = canon(k)
    for part in ('greet', 'chatter', 'hints'):
        for e in n.get(part, []): spoken(who, 'talk ' + part, e)
    for t in n.get('topics', []):
        for e in t.get('lines', []): spoken(who, 'talk ' + t['id'], e)
for f in ('quests/story.json', 'quests/tutorial.json', 'quests/job_quests.json'):
    d = load(f)
    for q in d.get('quests', []):
        giver = canon(q.get('giver', '')) or None
        if giver in ('attendance',): pass
        for part in ('intro', 'done'):
            add(giver, q['id'] + ' ' + part, re.sub(r'^\([^)]*\)\s*', '', q.get(part, '')))
        for o in q.get('objectives', []):
            for e in o.get('lines', []) or []:
                spoken(canon(o['target']) if o['type'] == 'TALK' else None, q['id'] + ' ' + o['type'].lower(), e)
        c = q.get('companion')
        if c:
            for k2, v in c.get('lines', {}).items():
                for e in (v.values() if isinstance(v, dict) else v): spoken(canon('quest_' + c['npc']) if not c['npc'].startswith('order_') else c['npc'], q['id'] + ' walk', e)
        for sc in ('sceneDone', 'sceneAccept'):
            x = q.get(sc)
            if isinstance(x, dict) and x.get('kind') == 'line': add(NAME2KEY.get(x.get('speaker'), x.get('speaker')), q['id'] + ' ' + sc, x.get('text', ''))
# 갈래
PAT = [
 ('합쇼', r'(습니다|습니까|십시오|ㅂ니다|[가-힣]니다|니까|십시다|옵니다)[.!?…~]*$'),
 ('하오', r'(시오|[가-힣]오|[가-힣]소|구려|리다|ㅂ시다|[읍합봅갑]시다|더이다|외다|외까)[.!?…~]*$'),
 ('해요', r'(요|죠|까요|걸요|데요|네요|군요|세요|예요|에요|래요|대요)[.!?…~]*$'),
 ('하게', r'(일세|하게|[가-힣]게|[가-힣]네|는가|던가|ㄴ가|[가-힣]나|더군|[가-힣]세|이지|라네|다네|ㄹ세|걸세|텐가)[.!?…~]*$'),
 ('두루', r'(지|군|걸|고|데|며|서|든|나|가|까|구먼|구만|거든|더라|려나|으니|니까|는데|텐데|건만|다만|면서|구나|로군|이야|거야)[.!?…~]*$'),   # 여러 말투가 함께 쓰는 끝 (반말 · 하게 · 하오 어디에나 나온다)
 ('해라', r'(다|냐|라|자|마|어|아|야|해|네|니|래|게|음|임|함|여|워|와|봐|줘|둬|써|쳐|져|겨|려|켜|펴|러|렴|구|거)[.!?…~]*$'),
]
# 주된 말투마다 "어긋난 것으로 볼 갈래"
CLASH = {'해요': {'해라', '하게', '하오', '두루'}, '합쇼': {'해라', '하게', '하오', '두루', '해요'}, '하게': {'해요', '합쇼', '하오', '해라'},
         '하오': {'해요', '합쇼', '하게', '해라'}, '해라': {'해요', '합쇼', '하오', '하게'}, '두루': set(), '?': set()}
ORD = [p[0] for p in PAT]
def kind(s):
    s = s.rstrip(' )"\'')
    for k, p in PAT:
        if re.search(p, s): return k
    return '?'
only = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] != '-' else None
for who in sorted(lines, key=lambda w: -len(lines[w])):
    c = collections.Counter(kind(s) for _, s in lines[who])
    tot = sum(c.values())
    if tot < 4 and not only: continue
    main = ([k for k, _ in c.most_common() if k not in ('?', '두루')] or ['?'])[0]
    if only:
        if who != only: continue
        for w, s in lines[who]: print(kind(s), '|', w, '|', s)
        continue
    print('=====', who, tot, dict(c), '주된:', main)
    for w, s in lines[who]:
        k = kind(s)
        if k in CLASH.get(main, ()) and (len(sys.argv) < 3 or w.startswith(sys.argv[2])): print('   ', k, '|', w, '|', s[:110])
