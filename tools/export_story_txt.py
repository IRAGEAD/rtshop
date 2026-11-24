# 스토리 대사를 읽기 좋은 텍스트로 뽑는다: python3 tools/export_story_txt.py [출력 파일] [퀘스트 ID 접두어 ...]
import json, os, sys, re
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
data = json.load(open(os.path.join(root, 'src/main/resources/rtshop_defaults/quests/story.json'), encoding='utf-8'))
out_path = sys.argv[1] if len(sys.argv) > 1 else 'story.txt'
prefixes = tuple(sys.argv[2:]) or ('story_', 'hidden_')
npc = {"quest_" + n["id"]: n["name"] for n in data["npcs"]}
npc.update({"quest_guide": "안내인 모리스", "shop_fisher": "어부 오스카", "attendance": "시청 서기 하루", "job": "직업 안내인 알버트"})
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
    if kind == "title": w("  [%s · 장 제목] %s 「%s」 — %s" % (tag, s.get("chapter", ""), s.get("title", ""), s.get("subtitle", "")))
    elif kind == "names":
        w("  [%s · 이름 적기] %s (%s)" % (tag, s.get("title", ""), s.get("speaker", "")))
        for t in s.get("text", "").split("\n"): w("      " + t)
    else:
        w("  [%s · %s%s] %s:" % (tag, "환영" if kind == "vision" else "한마디", ", 잠들 때" if s.get("onSleep") else "", s.get("speaker", "")))
        for t in s.get("text", "").split("\n"): w("      " + t)
last = None
for q in data["quests"]:
    if not q["id"].startswith(prefixes): continue
    chap = re.sub(r"\s*\(\d+/\d+\)$", "", q.get("chain", ""))
    if chap != last:
        w(); w("=" * 72); w(chap); w("=" * 72); last = chap
    w(); w("■ %s  %s" % (re.search(r"\((\d+/\d+)\)$", q.get("chain", "") or "()").group(1) if "(" in q.get("chain", "") else "", q["title"]))
    w("  주는 사람: " + npc.get(q["giver"], q["giver"]))
    scene("받을 때", q.get("sceneAccept"))
    w("  [받을 때] " + q["intro"])
    w("  [진행 중] " + q["progress"])
    for o in q["objectives"]:
        mod = o.get("mod", "")
        note = ""
        if mod: note = "  (%s 모드가 %s)" % (MODS.get(mod.lstrip("!"), mod.lstrip("!")), "없을 때만" if mod.startswith("!") else "있을 때")
        label = o.get("text") or npc.get(o.get("target", ""), o.get("target", ""))
        cnt = " x%d" % o["count"] if o.get("count", 1) > 1 else ""
        w("  ◇ 목표 [%s] %s%s%s" % (TYPE.get(o["type"], o["type"]), label, cnt, note))
        for ln in o.get("lines", []):
            w("      " + (ln if isinstance(ln, str) else json.dumps(ln, ensure_ascii=False)))
    c = q.get("companion")
    if c:
        w("  ◇ 동행: %s" % npc.get(c["npc"], c["npc"]))
        WHEN = {"arrive": "도착", "fight": "전투", "low": "위급", "idle": "쉴 때", "spot": "흔적 발견", "done": "작별"}
        for k, v in c.get("lines", {}).items():
            for t in v: w("      (%s) %s" % (WHEN.get(k, k), t))
    w("  [끝낼 때] " + q["done"])
    for ch in q.get("choices", []):
        w("  ▶ 선택: " + ch["text"])
    scene("끝낸 뒤", q.get("sceneDone"))
open(out_path, 'w', encoding='utf-8').write("골디아 연대기 — 스토리 대사 모음\n(■ 퀘스트 / ◇ 목표·동행 / ▷ 플레이어의 대답 / ▶ 선택지)\n" + "\n".join(L) + "\n")
print(len(L), "lines ->", out_path)
