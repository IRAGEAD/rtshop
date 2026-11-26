# 설정 기본값의 한국어 글(자바 코드 안에 적힌 것) 가운데 영어 번역이 없는 것을 모은다: python3 tools/story_lang/extract4.py
#   레이드·주간 보상 안내, 보스 소개 글, 결계 안내, 스킬라 단서, 직업·무기 종류 이름, 세계 규칙 안내
#   → tools/story_lang/source4.json (원문 목록, 순서 고정: 새 글은 뒤에 붙는다). 번역은 en4/p<시작번호>.py 에 같은 순서로 적는다.
#   서버는 이 글들을 보낼 때 StoryText.t / StoryText.json 으로 바꾼다 (운영자가 고친 글은 번역이 없어 그대로 나간다).
import json, os, re, sys
here = os.path.dirname(os.path.abspath(__file__))
root = os.path.dirname(os.path.dirname(here))
J = os.path.join(root, 'src', 'main', 'java', 'com', 'rtshop')
done = json.load(open(os.path.join(root, 'src', 'main', 'resources', 'rtshop_defaults', 'story_lang', 'en_us.json'), encoding='utf-8'))
# (파일, 그 줄에 이 글자가 있어야 모은다 — None 이면 모든 줄)
SOURCES = [
    ('story/StoryConfig.java', None),
    ('story/BossIntros.java', None),
    ('story/StorySeals.java', ('seal(',)),
    ('story/ScyllaClues.java', ('clue(', 'hint(', '"speaker"', '"text"')),
    ('raid/RaidConfig.java', ('t.put(',)),
    ('raid/AbyssWeekly.java', ('t.put(',)),
    ('job/JobConfig.java', ('add(def(', 'return "', 'npcName')),
    ('job/WeaponTypes.java', ('l.add(type(',)),
    ('rules/WorldRules.java', ('public String text',)),
]
SKIP = ('LOGGER.', 'help.add', '_help', 'IllegalArgument', 'sendSuccess', 'sendFailure')
lit = re.compile(r'"((?:[^"\\]|\\.)*)"')
def unesc(x):
    return x.replace('\\"', '"').replace('\\n', '\n').replace('\\\\', '\\')
found = []
for name, need in SOURCES:
    for line in open(os.path.join(J, name), encoding='utf-8'):
        st = line.strip()
        if st.startswith(('//', '*', '/*')) or any(k in line for k in SKIP):
            continue
        if need and not any(k in line for k in need):
            continue
        for m in lit.finditer(line):
            v = unesc(m.group(1))
            if any(0xAC00 <= ord(c) <= 0xD7A3 for c in v):
                found.append(v)
path = os.path.join(here, 'source4.json')
old = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else []
have = set(old)
add = [s for s in dict.fromkeys(found) if s not in have and s not in done]
json.dump(old + add, open(path, 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(len(old), '+', len(add), '=', len(old) + len(add), 'lines,', sum(len(s) for s in old + add), 'chars')
gone = [s for s in old if s not in set(found)]
if gone:
    print('코드에서 사라진 원문 %d줄 (번역은 남겨 둠):' % len(gone), [g[:30] for g in gone[:5]])
if '--show' in sys.argv:
    a, b = int(sys.argv[sys.argv.index('--show') + 1]), int(sys.argv[sys.argv.index('--show') + 2])
    allv = old + add
    for i in range(a, min(b, len(allv))): print(i, json.dumps(allv[i], ensure_ascii=False))
