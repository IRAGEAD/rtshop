# 스토리(story.json)에서 영어 번역이 아직 없는 글을 모은다: python3 tools/story_lang/extract3.py [--show 시작 끝]
#   → tools/story_lang/source3.json (원문 목록, 퀘스트 순서. 순서 고정: 새 글은 뒤에 붙는다). 번역은 en3/p<시작번호>.py 에 같은 순서로 적는다.
#   --show a b : a 번째부터 b 번째 앞까지를 "번호 [퀘스트 · 자리] 원문" 으로 보여 준다 (번역할 때 앞뒤를 보려고).
import json, os, sys
here = os.path.dirname(os.path.abspath(__file__))
root = os.path.dirname(os.path.dirname(here))
R = os.path.join(root, 'src', 'main', 'resources', 'rtshop_defaults')
done = json.load(open(os.path.join(R, 'story_lang', 'en_us.json'), encoding='utf-8'))
story = json.load(open(os.path.join(R, 'quests', 'story.json'), encoding='utf-8'))
found, where = [], {}
def walk(x, qid, key):
    if isinstance(x, str):
        if any(0xAC00 <= ord(c) <= 0xD7A3 for c in x):
            found.append(x); where.setdefault(x, qid + ' · ' + str(key))
    elif isinstance(x, list):
        for i in x: walk(i, qid, key)
    elif isinstance(x, dict):
        for k, v in x.items():
            if k.startswith('_'): continue
            walk(v, qid, k)
walk(story.get('npcs', {}), 'npcs', None)
for q in story['quests']: walk(q, q['id'], None)
path = os.path.join(here, 'source3.json')
old = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else []
have = set(old)
# en3 의 번역이 이미 en_us.json 에 들어가 있으므로, 옛 목록에 있는 것은 done 에 있어도 그대로 둔다
add = [s for s in dict.fromkeys(found) if s not in have and s not in done]
allv = old + add
json.dump(allv, open(path, 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(len(old), '+', len(add), '=', len(allv), 'lines,', sum(len(s) for s in allv), 'chars')
if '--show' in sys.argv:
    a, b = int(sys.argv[sys.argv.index('--show') + 1]), int(sys.argv[sys.argv.index('--show') + 2])
    for i in range(a, min(b, len(allv))):
        print(i, '[' + where.get(allv[i], '?') + ']', json.dumps(allv[i], ensure_ascii=False))
