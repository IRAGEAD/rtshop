# 스토리 대사를 읽기 좋은 텍스트로 뽑는다: python3 tools/export_story_txt.py [출력 파일] [퀘스트 ID 접두어 ...]
import json, os, sys, re
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
data = json.load(open(os.path.join(root, 'src/main/resources/rtshop_defaults/quests/story.json'), encoding='utf-8'))
EN = '--en' in sys.argv   # 영어판: story_lang/en_us.json 의 번역으로 바꿔서 뽑는다 (번역이 없는 글은 한국어 그대로)
_args = [a for a in sys.argv[1:] if a != '--en']
out_path = _args[0] if _args else 'story.txt'
prefixes = tuple(_args[1:]) or ('story_', 'hidden_')
if EN:
    _tr = json.load(open(os.path.join(root, 'src/main/resources/rtshop_defaults/story_lang/en_us.json'), encoding='utf-8'))
    def _walk(x):
        if isinstance(x, str): return _tr.get(x, x)
        if isinstance(x, list): return [_walk(i) for i in x]
        if isinstance(x, dict): return {k: (v if k in ('id', 'giver', 'target', 'type', 'kind', 'mod', 'npc', 'chain_raw') else _walk(v)) for k, v in x.items()}
        return x
    for _n in data["npcs"]: _n["name"] = _tr.get(_n["name"], _n["name"])
    data["quests"] = [_walk(q) for q in data["quests"]]
LBL = {"주는 사람": "Given by", "받을 때": "On accept", "진행 중": "In progress", "끝낼 때": "On completion", "끝낸 뒤": "After completion", "목표": "Goal", "동행": "Companion",
       "선택": "Choice", "장 제목": "chapter title", "이름 적기": "names written", "환영": "vision", "한마디": "line", ", 잠들 때": ", in sleep",
       "대화": "talk", "처치": "defeat", "가져오기": "bring", "찾아가기": "go to", "차원": "dimension", "조건": "condition", "발전과제": "advancement", "살펴보기": "examine", "지불": "pay", "방문": "visit",
       "도착": "arrive", "전투": "fight", "위급": "in danger", "쉴 때": "idle", "흔적 발견": "trace found", "일을 마쳤을 때": "done",
       "없을 때만": "is absent", "있을 때": "is present"}
def T(x): return LBL.get(x, x) if EN else x
npc = {"quest_" + n["id"]: n["name"] for n in data["npcs"]}
npc.update({"quest_guide": "안내인 모리스", "shop_fisher": "어부 오스카", "attendance": "시청 서기 하루", "job": "직업 안내인 알버트"} if not EN else
           {"quest_guide": "Morris the Guide", "shop_fisher": "Oscar the Fisher", "attendance": "Haru the Town Clerk", "job": "Albert the Job Guide"})
npc.update({n["id"]: n["name"] for n in data["npcs"]})
TYPE = {"TALK": "대화", "KILL": "처치", "DELIVER": "가져오기", "STRUCTURE": "찾아가기", "DIMENSION": "차원", "FLAG": "조건", "ADVANCEMENT": "발전과제",
        "INSPECT": "살펴보기", "PAY": "지불", "VISIT": "방문"}
MODS = {"block_factorys_bosses": "Bosses'Rise", "betternether": "BetterNether", "betterdungeons": "Better Dungeons", "ydtwo": "Yazz's Dungeons",
        "enderdragonplus": "Ender Dragon Plus"}
L = []
def w(s=""): L.append(s)
def scene(tag, s):
    if not s: return
    kind = s.get("kind")
    if kind == "title": w(("  [%s · " + T("장 제목") + "] %s 「%s」 — %s") % (T(tag), s.get("chapter", ""), s.get("title", ""), s.get("subtitle", "")))
    elif kind == "names":
        w(("  [%s · " + T("이름 적기") + "] %s (%s)") % (T(tag), s.get("title", ""), s.get("speaker", "")))
        for t in s.get("text", "").split("\n"): w("      " + t)
    else:
        w("  [%s · %s%s] %s:" % (T(tag), T("환영" if kind == "vision" else "한마디"), T(", 잠들 때") if s.get("onSleep") else "", s.get("speaker", "")))
        for t in s.get("text", "").split("\n"): w("      " + t)
last = None
for q in data["quests"]:
    if not q["id"].startswith(prefixes): continue
    chap = re.sub(r"\s*\(\d+/\d+\)$", "", q.get("chain", ""))
    if chap != last:
        w(); w("=" * 72); w(chap); w("=" * 72); last = chap
    _m = re.search(r"\(([^()]*)\)$", q.get("chain", ""))
    w(); w("■ %s  %s" % (_m.group(1) if _m else "", q["title"]))
    w("  " + T("주는 사람") + ": " + npc.get(q["giver"], q["giver"]))
    scene("받을 때", q.get("sceneAccept"))
    w("  [" + T("받을 때") + "] " + q["intro"])
    w("  [" + T("진행 중") + "] " + q["progress"])
    for o in q["objectives"]:
        mod = o.get("mod", "")
        note = ""
        if mod: note = ("  (when the %s mod %s)" if EN else "  (%s 모드가 %s)") % (MODS.get(mod.lstrip("!"), mod.lstrip("!")), T("없을 때만" if mod.startswith("!") else "있을 때"))
        label = o.get("text") or npc.get(o.get("target", ""), o.get("target", ""))
        cnt = " x%d" % o["count"] if o.get("count", 1) > 1 else ""
        w(("  ◇ " + T("목표") + " [%s] %s%s%s") % (T(TYPE.get(o["type"], o["type"])), label, cnt, note))
        for ln in o.get("lines", []):
            w("      " + (ln if isinstance(ln, str) else json.dumps(ln, ensure_ascii=False)))
    c = q.get("companion")
    if c:
        w(("  ◇ " + T("동행") + ": %s") % npc.get(c["npc"], npc.get("quest_" + c["npc"], c["npc"])))
        WHEN = {"arrive": "도착", "fight": "전투", "low": "위급", "idle": "쉴 때", "spot": "흔적 발견", "done": "일을 마쳤을 때"}
        for k, v in c.get("lines", {}).items():
            for t in (v.values() if isinstance(v, dict) else v): w("      (%s) %s" % (T(WHEN.get(k, k)), t))
    w("  [" + T("끝낼 때") + "] " + q["done"])
    for ch in q.get("choices", []):
        w("  ▶ " + T("선택") + ": " + ch["text"])
    scene("끝낸 뒤", q.get("sceneDone"))
open(out_path, 'w', encoding='utf-8').write(("The Vestalia Chronicle — story script (English version)\n(■ quest / ◇ goal · companion / ▷ the player's reply / ▶ choice)\n" if EN else "베스탈리아 연대기 — 스토리 대사 모음\n(■ 퀘스트 / ◇ 목표·동행 / ▷ 플레이어의 대답 / ▶ 선택지)\n") + "\n".join(L) + "\n")
print(len(L), "lines ->", out_path)
