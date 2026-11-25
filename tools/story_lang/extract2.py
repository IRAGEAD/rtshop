# 영어 번역이 아직 없는 글을 모은다: python3 tools/story_lang/extract2.py
#   대화(dialogue.json) · 읽을 물건(readables.json) · 안내서(guide.json) · 직업 퀘스트·초보 퀘스트·NPC 인사
#   → tools/story_lang/source2.json (원문 목록, 순서 고정: 새 글은 뒤에 붙는다). 번역은 en2/<시작번호>.json 에 같은 순서로 적는다.
import json, os
here = os.path.dirname(os.path.abspath(__file__))
root = os.path.dirname(os.path.dirname(here))
R = os.path.join(root, 'src', 'main', 'resources', 'rtshop_defaults')
done = json.load(open(os.path.join(R, 'story_lang', 'en_us.json'), encoding='utf-8'))
SKIP_KEYS = {'id', 'item', 'target', 'mod', 'image', 'icons', 'profession', 'type', 'flags', 'notFlags', 'quests', 'notQuests', 'items', 'need', 'requires', 'giver',
             'sound', 'color', 'theme', 'kind', 'keys_', 'entity', 'structure', 'npc', 'style', 'at', 'chain_id'}
found = []
def walk(x, key=None):
    if isinstance(x, str):
        if any(0xAC00 <= ord(c) <= 0xD7A3 for c in x): found.append(x)
    elif isinstance(x, list):
        for i in x: walk(i, key)
    elif isinstance(x, dict):
        if 'ko' in x and 'en' in x: return   # 두 언어를 함께 적은 글
        for k, v in x.items():
            if k in SKIP_KEYS or k.startswith('_'): continue
            walk(v, k)
for name in ('dialogue.json', 'readables.json', 'guide.json', 'quests/npcs.json', 'quests/tutorial.json', 'quests/job_quests.json'):
    data = json.load(open(os.path.join(R, name), encoding='utf-8'))
    walk(data['items'] if name == 'readables.json' else data)   # 읽을 물건은 'items' 아래에 글이 있다
path = os.path.join(here, 'source2.json')
old = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else []
have = set(old)
add = [s for s in dict.fromkeys(found) if s not in have and s not in done]
json.dump(old + add, open(path, 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(len(old), '+', len(add), '=', len(old) + len(add), 'lines,', sum(len(s) for s in old + add), 'chars')
