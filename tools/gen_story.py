# 메인 스토리 「베스탈리아 연대기」 (3판: 서장 + 4장 + 종장 + 숨은 이야기) 퀘스트를 만든다: python3 tools/gen_story.py
# 1장 바닐라(이사벨·위더·워든·엔더 드래곤), 2장 Mowzie's Mobs(로란), 3장 Aquamirae(오스카·코넬리아), 4장 L_Ender's Cataclysm(에르윈), 종장 이그니스. 플레이어는 폭풍 조사대의 수많은 대원 중 한 명.
# 보스전은 「기억의 문」(전용 공간)에서 치른다: config/rtshop/story_arenas.json 에 있는 보스를 잡는 목표에는 입장 단추가 생긴다.
# 결과: src/main/resources/rtshop_defaults/quests/story.json
import json, os, re

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'rtshop_defaults', 'quests', 'story.json')

ELDER, ARCH, CAPT = 'quest_order_elder', 'quest_order_archivist', 'quest_order_captain'
GUIDE, FISHER, CLERK = 'quest_guide', 'shop_fisher', 'attendance'
NPCS = [
    {"id": "order_elder", "name": "수도회 장로 에르윈", "profession": "cleric",
     "greeting": "불을 지키고, 봉인을 지킨다. …견습이여, 오늘은 무엇을 배우러 왔나."},
    {"id": "order_archivist", "name": "기록관 이사벨", "profession": "librarian",
     "greeting": "연대기는 거짓말을 하지 않아요. 다만… 누군가 찢어 냈을 뿐이죠."},
    {"id": "order_captain", "name": "탐험대장 로란", "profession": "cartographer",
     "greeting": "지도는 발로 그리는 거다. 신발 끈은 단단히 맸나?"},
]

def title(ch, t, sub, color, theme, sound="minecraft:ui.toast.challenge_complete"):
    return {"kind": "title", "chapter": ch, "title": t, "subtitle": sub, "color": color, "theme": theme, "sound": sound}

def vision(speaker, text, color="FF7A20", sound="minecraft:block.respawn_anchor.charge"):
    return {"kind": "vision", "speaker": speaker, "text": text, "color": color, "sound": sound}

def line(speaker, text, color="E8C878"):
    return {"kind": "line", "speaker": speaker, "text": text, "color": color}

def talk(npc, text="", lines=None): return {"type": "TALK", "target": npc, **({"text": text} if text else {}), **({"lines": lines} if lines else {})}
def kill(ent, n=1, text="", mod=""): return {"type": "KILL", "target": ent, "count": n, **({"text": text} if text else {}), **({"mod": mod} if mod else {})}
def deliver(item, n=1, text="", mod=""): return {"type": "DELIVER", "target": item, "count": n, **({"text": text} if text else {}), **({"mod": mod} if mod else {})}
def struct(sid, text, mod=""): return {"type": "STRUCTURE", "target": sid, "text": text, **({"mod": mod} if mod else {})}
def dim(d, text): return {"type": "DIMENSION", "target": d, "text": text}
def flag(f, text): return {"type": "FLAG", "target": f, "text": text}
def adv(a, text): return {"type": "ADVANCEMENT", "target": a, "text": text}
def pay(n): return {"type": "PAY", "count": n}
def item(i, n=1, mod=""): return {"item": i, "count": n, **({"mod": mod} if mod else {})}

Q = []
order = [1000]

def q(id, chain, t, giver, intro, progress, done, objs, gold=0, items=None, flags=None, requires=None,
      requiresFlags=None, turnIn=None, sceneAccept=None, sceneDone=None, choices=None, mod=None, blockedByFlags=None, companion=None, dream=None):
    order[0] += 10
    o = {"id": id, "chain": chain, "order": order[0], "title": t, "giver": giver}
    if mod: o["mod"] = mod
    if turnIn: o["turnIn"] = turnIn
    if requires: o["requires"] = requires
    if requiresFlags: o["requiresFlags"] = requiresFlags
    if blockedByFlags: o["blockedByFlags"] = blockedByFlags
    o["intro"], o["progress"], o["done"] = intro, progress, done
    o["objectives"] = objs
    r = {}
    if gold: r["gold"] = gold
    if items: r["items"] = items
    if flags: r["flags"] = flags
    o["reward"] = r
    if sceneAccept: o["sceneAccept"] = sceneAccept
    if sceneDone: o["sceneDone"] = sceneDone
    if choices: o["choices"] = choices
    if companion: o["companion"] = companion
    if dream: o["sceneDream"] = dream
    Q.append(o)
    return id


def ch(c, i, n):
    return "%s (%d/%d)" % (c, i, n)

# 서장 + 1장은 tools/story_ch01.py (4판: 스토리 대개편)
exec(open(os.path.join(os.path.dirname(__file__), 'story_ch01.py'), encoding='utf-8').read())

# 2장은 tools/story_ch02.py (4판: 스토리 대개편)
exec(open(os.path.join(os.path.dirname(__file__), 'story_ch02.py'), encoding='utf-8').read())

# 3장은 tools/story_ch03.py (4판: 스토리 대개편)
exec(open(os.path.join(os.path.dirname(__file__), 'story_ch03.py'), encoding='utf-8').read())

# 4장은 tools/story_ch04.py (4판: 스토리 대개편)
exec(open(os.path.join(os.path.dirname(__file__), 'story_ch04.py'), encoding='utf-8').read())

# 종장과 숨은 이야기 「거울 속의 연대기」는 tools/story_ch05.py (4판: 스토리 대개편)
exec(open(os.path.join(os.path.dirname(__file__), 'story_ch05.py'), encoding='utf-8').read())

# 플레이어의 대답: 대화 상자에서 선택지로 직접 눌러 말한다 (하나뿐이어도). "▷ " 로 시작하는 줄, " ‖ " 로 여러 대답.
# (퀘스트 ID, 말 거는 NPC) -> [(이 줄 다음에 넣을 위치, 대답), ...]  위치 -1 = 맨 끝
REPLIES = {}
_used = set()
for _q in Q:
    for _o in _q["objectives"]:
        if _o.get("type") != "TALK" or "lines" not in _o:
            continue
        _key = (_q["id"], _o["target"])
        _r = REPLIES.get(_key)
        if not _r:
            continue
        _used.add(_key)
        _lines = list(_o["lines"])
        for _at, _text in sorted(_r, key=lambda x: -(x[0] if x[0] >= 0 else 10 ** 6)):
            _pos = len(_lines) if _at < 0 else _at + 1
            _lines.insert(_pos, "▷ " + _text)
        _o["lines"] = _lines
assert _used == set(REPLIES), set(REPLIES) - _used

# 숨은 이야기 「마중 나간 사람」(스킬라)은 tools/story_scylla.py
exec(open(os.path.join(os.path.dirname(__file__), 'story_scylla.py'), encoding='utf-8').read())

# 숨은 이야기 「고쳐 쓰는 손」 · 「지키는 눈」 (히든 직업 위조자 · 조사관으로 가는 길, 3.76.0)은 tools/story_hidden_jobs.py
exec(open(os.path.join(os.path.dirname(__file__), 'story_hidden_jobs.py'), encoding='utf-8').read())

# 4판으로 다시 쓴 장: 「장 이름 (i/n)」 번호를 실제 순서대로 다시 매긴다 (퀘스트를 끼워 넣거나 빼도 ID 는 그대로 두려고)
for _prefix in ("story_0_", "story_1_", "story_2_", "story_3_", "story_4_", "story_5_"):
    _qs = [x for x in Q if x["id"].startswith(_prefix)]
    for _i, _x in enumerate(_qs):
        _x["chain"] = re.sub(r"\(\d+/\d+\)$", "(%d/%d)" % (_i + 1, len(_qs)), _x["chain"])

# 동행 NPC 의 말: 퀘스트마다 그 자리에 맞는 말로 (tools/story_walks.py)
QUESTS = Q
exec(open(os.path.join(os.path.dirname(__file__), 'story_walks.py'), encoding='utf-8').read())

data = {
    "_help": "메인 스토리 「베스탈리아 연대기」 3판 (서장·1~4장·종장·숨은 이야기). 이 파일은 모드 업데이트 때 새 기본값으로 바뀔 수 있으니, 직접 고쳤다면 이 파일을 계속 고쳐 쓰세요 (고친 파일은 덮어쓰지 않습니다). "
             "목표 종류: STRUCTURE(구조물 곁), ADVANCEMENT(발전과제), DIMENSION(차원), FLAG(표식). sceneAccept/sceneDone: 받을 때/끝낼 때 연출. "
             "seal_* 표식은 결계(config/rtshop/seals.json)를 연다. 보스를 잡는 목표에는 「기억의 문」 단추가 생긴다 (config/rtshop/story_arenas.json).",
    "version": 4,
    "npcs": NPCS,
    "quests": Q,
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=1)
print(len(Q), "quests,", sum(x["reward"].get("gold", 0) for x in Q), "G total")
