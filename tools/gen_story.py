# 메인 스토리 「골디아 연대기」 (3판: 서장 + 4장 + 종장 + 숨은 이야기) 퀘스트를 만든다: python3 tools/gen_story.py
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
      requiresFlags=None, turnIn=None, sceneAccept=None, sceneDone=None, choices=None, mod=None, blockedByFlags=None, companion=None):
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

# ================================================================ 종장 「연대기의 마지막 장」 — 불꽃 기사단장 아르덴과 에르윈 (Cataclysm: 이그니스)
# 발단: 남은 불씨 / 전개: 불타는 망령들이 떨군 기억 / 위기: 끌려간 기록관 / 절정: 이그니스 / 결말: 셋째 줄과 마지막 장
C5 = "종장 연대기의 마지막 장"
N5 = 8
p = q("story_5_01", ch(C5, 1, N5), "남은 불씨", CAPT,
      "에르윈이 불꽃 경기장으로 갔다. 지옥 깊은 곳, 최초의 불꽃이 잠든 곳이다. 놈은 여섯 눈으로 불꽃의 왕좌를 열 생각이다. 남은 건 너희 조사대의 불씨와 얼음 결정, 그리고 놈이 미처 못 가져간 이 마지막 눈뿐이다. 원정대를 모은다.",
      "탐험대장 로란과 출정을 준비하라.",
      "이사벨, 오스카, 브람, 하루, 모리스… 다들 왔다. 이번엔 아무도 남기고 오지 않는다. 그게 내 녹슨 맹세다.",
      [talk(CAPT, "탐험대장 로란", [
          "탐험대장 로란: 1장에서 너희는 세 끝을 걸었다. 2장에서 내 대원들을 데려왔다. 3장에서 바다를 녹였다.",
          "탐험대장 로란: 4장에서 봉인을 풀었다. 그게 놈의 계획이었다 해도, 너희가 걸어온 길은 거짓이 아니다.",
          "탐험대장 로란: 가자. 폭풍 조사대. 아니, 원정대.",
      ])], gold=8000, items=[item("cataclysm:flame_eye", 1, K)], requires=[CH4_END], flags=["seal_ignis"],
      sceneAccept=title("종장", "연대기의 마지막 장", "불을 지키고, 봉인을 지키고, 그리고…", "FF7A20", "ember", "minecraft:entity.lightning_bolt.thunder"))
p = q("story_5_02", ch(C5, 2, N5), "불타는 망령들", CAPT,
      "경기장으로 가는 길에 불타는 망령들이 서성인다. 놈들은 에르윈의 불꽃에 묶인 옛 기사들이다. 쓰러뜨리면 회색 재가 떨어지는데… 그 재를 쥐면 누군가의 기억이 들린다고 한다. 에르윈의 기억이다.",
      "불타는 망령 6명과 불타는 광전사 2명을 물리쳐라.",
      "기억의 조각 셋이다. 손에 쥐면 누군가 우는 소리가 들린다. 기록관이… 지금 여기 없다. 이사벨은 어디 갔지?",
      [kill("cataclysm:ignited_revenant", 6, "불타는 망령", K), kill("cataclysm:ignited_berserker", 2, "불타는 광전사", K)],
      gold=9000, items=[item("rtshop:memory_shard", 3)], requires=[p])
p = q("story_5_03", ch(C5, 3, N5), "천 년 전의 제자", FISHER,
      "이보게, 이사벨이 없어졌다네! 기록고에 이 쪽지만 남았어. '기억의 조각을 맞춰 볼게요.' …그 애가 조각을 맞추려면 조각이 있어야지. 자네가 가진 조각을 내게 주게. 내가 이사벨 대신 읽겠네. 어부도 글은 읽을 줄 안다네.",
      "기억의 조각 3개를 어부 오스카에게 건네게.",
      "(오스카가 조각을 맞춘다.) 에르윈은 여덟째 왕의 제자였다네. 스승이 봉인을 거부하다 이름을 잃고 허공 끝으로 쫓겨났을 때, 맹세했대. 흩어진 불을 다시 모아 스승의 이름을 돌려놓겠다고. 그런데 마지막 조각에… 이름을 다시 적으려면 '연대기를 쓰는 손'이 필요하다고 적혀 있네. 기록관의 손 말일세.",
      [deliver("rtshop:memory_shard", 3, "기억의 조각")], gold=8000, requires=[p],
      sceneDone=vision("젊은 에르윈", "스승님, 기다리십시오.\n흩어진 불을 모두 모아, 반드시 이름을 돌려드리겠습니다.", "B0B0C0", "minecraft:block.amethyst_block.chime"))
p = q("story_5_04", ch(C5, 4, N5), "끌려간 기록관", CAPT,
      "에르윈이 이사벨을 데려갔다. 지워진 이름을 연대기에 다시 쓰게 하려는 거다. 엘레나에게 시키려다 실패한 걸 딸에게 시키려는 거다. 불꽃 경기장으로 간다. 왕좌 앞 결계는 최초의 불꽃이다. 얼음 결정으로만 꺼진다. 2장에서 프로스트모에게서 얻은 그 결정이다.",
      "불꽃 경기장을 찾아 들어가, 얼음 결정을 탐험대장 로란에게 건네라.",
      "결정이 녹으며 불길이 가라앉았다. 왕좌 앞에 이사벨이 묶여 있다. 펜을 쥔 손을 끝까지 펴지 않았다. 그리고 그 앞에서… 에르윈이 최초의 불꽃에 손을 넣었다. 불꽃 속에서 갑옷 입은 기사가 일어선다. 꿈속의 그 기사다.",
      [struct("cataclysm:burning_arena", "불꽃 경기장", K), deliver("rtshop:frost_heart", 1, "얼음 결정")], gold=9000, requires=[p],
      sceneAccept=title("종장 · 불꽃의 왕좌", "끌려간 기록관", "최초의 불꽃 앞에서", "FF5A10", "ember", "minecraft:entity.blaze.ambient"))
p = q("story_5_05", ch(C5, 5, N5), "화염의 군주", CAPT,
      "이그니스다. 불꽃 기사단장 아르덴. 천 년 전 일곱 왕의 불을 지키던 기사다. 지금은 에르윈의 불꽃에 묶여 있다. 놈은 불꽃의 검으로 땅을 가르고, 방패로 막고, 마지막엔 푸른 지옥불을 두른다. 원정대 전원으로 간다. 기억의 문을 열어라. 쓰러진 그에게 불씨를 건네라.",
      "기억의 문을 열고 이그니스를 쓰러뜨려라.",
      "쓰러진 기사의 눈에서 불이 꺼지고, 사람의 눈빛이 돌아왔다. \"이번에는 너희가 지켜라.\" 불꽃이 꺼지며 회색 망토의 노인이 무릎을 꿇었다. 천 년 묵은 재를 손에 쥔 채. 이제 결정해야 한다.",
      [kill("cataclysm:ignis", 1, "화염의 군주 이그니스", K)], gold=20000, items=[item("rtshop:grey_ash")], requires=[p], flags=["ignis_raid_story"],
      sceneAccept=title("종장 · 최초의 불꽃", "화염의 군주", "불꽃 기사단장 아르덴", "FF3A10", "ember", "minecraft:entity.wither.spawn"),
      choices=[{"text": "에르윈을 용서한다. 그의 이름을 연대기에 남긴다.", "flags": ["fin_forgave"]},
               {"text": "에르윈의 이름을 지운다. 그가 지운 이름들처럼.", "flags": ["fin_erased"]}],
      sceneDone={"kind": "vision", "speaker": "불꽃 기사단장 아르덴", "text": "또 왔구나, 불씨를 쥔 자들아.\n…이번에는 너희가 지켜라.", "color": "FFB060",
                 "sound": "minecraft:block.respawn_anchor.deplete", "shake": 4, "seconds": 10})
p = q("story_5_06", ch(C5, 6, N5), "지워진 셋째 줄", ARCH,
      "…구해 줘서 고마워요. 에르윈은 끝까지 저한테 펜을 쥐여 주려고 했어요. 그런데 이상하죠. 무섭지 않았어요. 어머니가 끝까지 읽었으니까, 저는 끝까지 안 쓰기로 했거든요. 그리고 에르윈의 망토 안쪽에서 이걸 찾았어요. 어머니의 서약서 원본이에요. 에르윈이 지웠던 셋째 줄이 있어요.",
      "기록관 이사벨과 셋째 줄을 읽으세요.",
      "\"불을 지키고, 봉인을 지킨다. 그리고 때가 되면 놓아준다.\" …놓아준다. 에르윈은 이 줄이 싫었던 거예요. 스승을 놓아줄 수 없었으니까. 그런데요, 망토 안쪽에 한 줄이 더 있었어요. '여덟째 왕의 불은 폭풍이 되어 바다 위 신전에 잠든다.' …이건 나중에 생각할래요.",
      [talk(ARCH, "기록관 이사벨", [
          "기록관 이사벨: 첫째, 불을 지킨다.",
          "기록관 이사벨: 둘째, 봉인을 지킨다.",
          "기록관 이사벨: 셋째… 때가 되면 놓아준다.",
      ])], gold=9000, requires=[p], flags=["third_line_found"])
p = q("story_5_07", ch(C5, 7, N5), "폭풍이 그친 부두", FISHER,
      "폭풍이 그쳤다네. 자네가 처음 내린 그 부두 말일세. 다들 나와 있네. 한 사람씩 인사해 주게. 원정대는 그런 거라네.",
      "어부 오스카와 탐험대장 로란에게 인사하세요.",
      "(부두에 노을이 진다. 오스카의 그물, 로란의 지도, 이사벨의 펜.) 다들 자네 이름을 불렀다네. 바다 사람은 서로의 이름을 기억해 주는 게 일이지.",
      [talk(FISHER, "어부 오스카", [
          "어부 오스카: 그물이 무겁다네. 바다가 정직해졌거든.",
          "어부 오스카: 엘릭도 어디선가 웃고 있겠지.",
      ]), talk(CAPT, "탐험대장 로란", [
          "탐험대장 로란: 왔냐. 폭풍이 그쳤다.",
          "탐험대장 로란: 조사대는 이제 원정대다. 다음 원정에도 너를 부르마.",
          "탐험대장 로란: …아무도 남기지 않았다. 고맙다.",
      ])], gold=10000, requires=[p])
p = q("story_5_08", ch(C5, 8, N5), "연대기의 마지막 장", ARCH,
      "모두 끝났어요. 이제 연대기의 마지막 장을 함께 써요. 어머니가 끝까지 읽은 연대기를, 제가 끝까지 쓸게요.",
      "기록관 이사벨과 마지막 장을 쓰세요.",
      "\"불을 지키고, 봉인을 지키고, 그리고 놓아주었다.\" …마지막 장이에요. 조사대원 한 사람 한 사람의 이름으로 적었어요. 당신의 이름도요. 최초의 불꽃은 매주 다시 그의 모습으로 타오를 거예요. 그 불을 잠재우는 게 이제 당신들의 일이에요.",
      [talk(ARCH, "기록관 이사벨")], gold=20000, items=[item("rtshop:chronicle_last_page")], requires=[p], flags=["story_done"],
      sceneDone=title("에필로그", "연대기의 마지막 장", "폭풍이 그친 부두에서", "E8C878", "ember", "minecraft:music_disc.otherside"))

# ================================================================ 숨은 이야기: 환술사 (The Illusioner)
CH = "숨은 이야기 · 거울 속의 연대기"
I = "illusioner"
# 숨은 이야기의 조건: 1장 기록고에서 서가를 구했다면 그때 보랏빛 교정 자국(clue_purple_ink)을 본다.
# 견습 기록관 미아를 구했다면, 4장 나선의 심연 3층을 지난 뒤 미아가 대신 알려 준다.
q("hidden_purple_ink", CH + " (0/2)", "미아의 발견", ARCH,
  "조사대원님, 견습 기록관 미아가 할 말이 있대요. 그날 불 속에서 구해 준 그 아이요. 심연에서 여덟 왕의 회의를 봤다는 얘기를 듣고 떠오른 게 있대요.",
  "견습 기록관 미아의 이야기를 들으세요. (기록관 이사벨에게)",
  "미아가 그날 밤 연기 속에서 봤대요. 타지 않은 페이지 몇 장에 보랏빛 잉크로 고친 자국이 있었대요. 누군가 연대기를 고쳐 쓴 거예요. …불에 탄 서가 대신 미아의 기억이 남았네요.",
  [talk(ARCH, "기록관 이사벨", [
      "견습 기록관 미아: 그날 불길 속에서… 바닥에 떨어진 페이지를 봤어요.",
      "견습 기록관 미아: 글자 위에 보랏빛 잉크로 덧쓴 자국이 있었어요. 거울에 비친 것처럼 뒤집힌 글씨였어요.",
      "견습 기록관 미아: 무서워서 말 못 했어요. 죄송해요.",
  ])], gold=3000, requires=[CH4_COUNCIL], requiresFlags=["ch1_saved_apprentice"], flags=["clue_purple_ink"], mod=I)
p = q("hidden_illusioner_0", CH + " (단서)", "보랏빛 잉크의 행방", ARCH,
      "항해일지의 숨은 쪽에도 보랏빛 잉크가 묻어 있었어요. 1장에서 마녀의 잉크 단지에서 본 그 색이요. 그 잉크는 소환사들이 거울 속 사람들에게서 받아 쓴대요. 그런데 소환사들은 숨어 살아서… 마을을 습격할 때만 모습을 드러내요. 습격을 막다가 소환사를 만나면, 그 잉크를 찾아 주세요.",
      "마을 습격(레이드)에 나타나는 소환사를 3명 물리치세요.",
      "보랏빛 잉크병이에요. 병 바닥에 거울 문양이 있어요. …불타는 기록고에서 살아남은 기록에도 같은 잉크가 있었을 거예요. 이제 숲속 저택의 안개가 당신을 알아볼지도 몰라요.",
      [kill("minecraft:evoker", 3, "습격의 소환사")], gold=5000, requires=[CH3_LOGBOOK], requiresFlags=["clue_hidden_page"], blockedByFlags=["clue_purple_ink"],
      flags=["clue_purple_ink"], mod=I)
p = q("hidden_illusioner_1", CH + " (1/2)", "빈 쪽의 잉크", ARCH,
      "보랏빛 잉크 말이에요. 항해일지의 숨은 페이지에도 같은 잉크가 묻어 있었어요. 거울 글씨로요. 환술사들이 쓰는 잉크예요. 어두운 숲의 저택에 사는 일리저들이 그런 잉크를 쓴대요. 그 저택은 보랏빛 안개에 싸여 아무도 들어가지 못했대요. 빈 쪽을 찾은 당신이라면… 안개가 길을 열어 줄지도 몰라요.",
      "숲속 저택을 찾아가 보세요.",
      "저택의 안개가 걷혔어요. 지하에서 이상한 음악 소리가 들렸다고요? 이제 거울도 당신을 비출 거예요.",
      [struct("minecraft:mansion", "숲속 저택")], gold=6000, requires=["story_5_08"], requiresFlags=["clue_hidden_page", "clue_purple_ink"], flags=["seal_illusioner"], mod=I)
p = q("hidden_illusioner_2", CH + " (2/2)", "거짓 연대기의 필경사", ARCH,
      "저택 아래의 환술사를 쓰러뜨리세요. 그가 연대기를 고쳐 썼다면, 진짜 페이지를 가지고 있을 거예요.",
      "기억의 문을 열고 환술사를 쓰러뜨리세요.",
      "진짜 연대기의 한 장이에요! \"회색 망토의 학자 에르윈은 불꽃의 조각을 훔쳤고, 우리는 그의 이름을 지웠다. 그리고 그는 우리의 기록을 지웠다.\" …이걸 장로님이 보시면 안 돼요.",
      [kill("illusioner:illusioner", 1, "환술사", I)], gold=9000, items=[item("rtshop:chronicle_page", 2)], requires=[p], flags=["clue_true_page"], mod=I,
      sceneDone=vision("환술사 벨로크", "이 이야기는… 내가 고쳐 쓴 것이다.\n진짜 마지막 장은 아직 아무도 쓰지 않았다.", "B48CFF", "minecraft:entity.illusioner.mirror_move"))

# 플레이어의 대답: 대화 상자에서 선택지로 직접 눌러 말한다 (하나뿐이어도). "▷ " 로 시작하는 줄, " ‖ " 로 여러 대답.
# (퀘스트 ID, 말 거는 NPC) -> [(이 줄 다음에 넣을 위치, 대답), ...]  위치 -1 = 맨 끝
REPLIES = {
    ("story_5_01", CAPT): [(-1, "가요, 대장님.")],
    ("story_5_06", ARCH): [(-1, "때가 되면, 놓아준다…")],
    ("story_5_07", FISHER): [(-1, "엘릭도 분명 웃고 있을 거예요.")],
    ("story_5_07", CAPT): [(-1, "언제든 불러 주세요, 대장님.")],
    ("hidden_purple_ink", ARCH): [(-1, "괜찮아요. 말해 줘서 고마워요.")],
}
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

# 4판으로 다시 쓴 장: 「장 이름 (i/n)」 번호를 실제 순서대로 다시 매긴다 (퀘스트를 끼워 넣거나 빼도 ID 는 그대로 두려고)
for _prefix in ("story_0_", "story_1_", "story_2_", "story_3_", "story_4_"):
    _qs = [x for x in Q if x["id"].startswith(_prefix)]
    for _i, _x in enumerate(_qs):
        _x["chain"] = re.sub(r"\(\d+/\d+\)$", "(%d/%d)" % (_i + 1, len(_qs)), _x["chain"])

data = {
    "_help": "메인 스토리 「골디아 연대기」 3판 (서장·1~4장·종장·숨은 이야기). 이 파일은 모드 업데이트 때 새 기본값으로 바뀔 수 있으니, 직접 고쳤다면 이 파일을 계속 고쳐 쓰세요 (고친 파일은 덮어쓰지 않습니다). "
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
