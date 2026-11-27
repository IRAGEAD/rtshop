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
    if name in ('quests/tutorial.json', 'quests/job_quests.json'):
        # 'quests' 는 조건 목록의 열쇠라 위에서 건너뛴다: 퀘스트 파일의 본문은 여기서 따로 모은다
        for qd in data.get('quests', []): walk(qd)
# 3.69.0: 상점 창의 제목 · 상인 이름 · 분류 이름 (상점 파일의 품목 목록은 빼고). 다른 곳의 같은 낱말과 뜻이 갈리지 않게 "상점:" 을 붙여 따로 번역한다
#   (서버 ShopServer.shopText 가 "상점:" + 글 → 그냥 글 순으로 찾는다). 식당의 제목 · 주인 이름은 코드(Restaurant)의 기본값.
SHOPS = ('attendance_shop', 'blacksmith', 'blacksmith_enhance', 'farmer', 'fisher', 'food', 'gacha_shop', 'gacha_shop_magic', 'general', 'land', 'raid')
for name in SHOPS:
    data = json.load(open(os.path.join(R, name + '.json'), encoding='utf-8'))
    for k in ('title', 'npcName'):
        if k in data: walk('상점:' + data[k])
    for c in data.get('categories', []) or []:
        if c.get('name'): walk('상점:' + c['name'])
for s in ('식당', '식당 주인'):
    walk('상점:' + s)
path = os.path.join(here, 'source2.json')
old = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else []
have = set(old)
add = [s for s in dict.fromkeys(found) if s not in have and s not in done]
json.dump(old + add, open(path, 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(len(old), '+', len(add), '=', len(old) + len(add), 'lines,', sum(len(s) for s in old + add), 'chars')
import sys
if '--show' in sys.argv:
    a, b = int(sys.argv[sys.argv.index('--show') + 1]), int(sys.argv[sys.argv.index('--show') + 2])
    allv = old + add
    for i in range(a, min(b, len(allv))): print(i, json.dumps(allv[i], ensure_ascii=False))
