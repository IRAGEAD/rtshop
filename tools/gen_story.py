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

# ================================================================ 3장 「얼어붙은 바다의 항해일지」 — 오스카와 코넬리아의 이야기 (Aquamirae)
# 발단: 얼음 아래에서 이름을 부르는 소리 / 전개: 얼음 미로의 전초기지·은신처·심해의 짐승들·미로의 어머니·배들의 무덤·유령선
# 위기: 코넬리아의 항해일지 (회색 망토가 판 항로) / 결말: 놓아주지 못하는 마음, 녹는 바다
C3 = "3장 얼어붙은 바다의 항해일지"
A = "aquamirae"
N3 = 19
p = q("story_3_01", ch(C3, 1, N3), "얼어붙은 그물", FISHER,
      "자네가 조사대원인가. 어부 오스카라네. 바다가 얼어붙은 뒤로 그물을 한 번도 못 걷었지. 아니, 그물 얘기를 하려던 게 아니라네. 열다섯 해 전, 내 동생 엘릭이 코넬리아라는 선장의 배에 올랐다네. 북쪽 바다로 나갔다가 돌아오지 않았지. 그런데 얼음이 덮인 뒤로 밤마다 얼음 아래에서 이름을 부르는 소리가 들린다네.",
      "어부 오스카의 이야기를 들어 주게.",
      "나는 매일 밤 얼음 위에 등불을 켜 둔다네. 엘릭이 길을 잃지 않게. …바보 같지? 그래도 자네들이 북쪽으로 간다니, 부탁 하나만 하세. 얼음 미로에서 선원들의 이름표를 보거든 가져다주게.",
      [talk(FISHER, "어부 오스카", [
          "어부 오스카: 바다는 정직하다네. 기다린 만큼 준다네. …그런데 이번엔 열다섯 해를 기다렸는데 아무것도 안 주는군.",
          "어부 오스카: 엘릭은 웃음소리가 컸다네. 얼음 아래에서 들리는 소리도… 웃음소리 같을 때가 있지.",
      ])], gold=2800, requires=[CH2_END],
      sceneAccept=title("제 3 장", "얼어붙은 바다의 항해일지", "어부 오스카, 열다섯 해의 등불", "9FD8FF", "frost", "minecraft:entity.player.hurt_freeze"))
p = q("story_3_02", ch(C3, 2, N3), "얼음 미로의 외곽", CAPT,
      "북쪽 바다가 통째로 얼었다. 얼음 사이로 미로가 생겼고, 해적들이 그 위에 전초기지를 세웠다. 원정대의 첫 발판이 될 곳이다. 해적 전초기지를 찾아라. 얼음 위에서는 걸음을 아껴라.",
      "얼음 미로의 해적 전초기지를 찾아라.",
      "전초기지의 깃발… 코넬리아의 해적단이다. 열다섯 해 전 바다와 함께 얼어붙었다는 그 선장 말이다. 기지는 비어 있었다. 식탁 위 접시까지 그대로였다고.",
      [struct("aquamirae:outpost", "해적 전초기지", A)], gold=3400, requires=[p])
p = q("story_3_03", ch(C3, 3, N3), "고통받는 영혼들", CAPT,
      "미로를 떠도는 영혼들이 길을 막고 있다. 놈들을 물리치고 해적 은신처를 찾아라. 놈들 목에 걸린 이름표가 있으면 오스카에게 가져다줘라.",
      "해적 은신처를 찾고, 고통받는 영혼을 5명 물리쳐라.",
      "은신처 벽에 낙서가 있더군. '회색 망토가 항로를 팔았다.' 회색 망토…? 수도회의 옛 학자들이 입던 옷이다. 그리고 회색 금화도 한 닢 굴러다녔다. 1장의 그 금화다.",
      [struct("aquamirae:shelter", "해적 은신처", A), kill("aquamirae:tortured_soul", 5, "고통받는 영혼", A)], gold=3700, items=[item("rtshop:crew_name_tag", 2)], requires=[p], flags=["clue_grey_cloak"])
p = q("story_3_04", ch(C3, 4, N3), "이름을 불러 주게", FISHER,
      "이름표를 찾았다고? 가져다주게. 내가 한 명씩 불러 주겠네. 엘릭이 아니어도 괜찮네. 누군가는 불러 줘야 하니까.",
      "선원의 이름표 2개를 어부 오스카에게 건네게.",
      "\"요나스. 베른.\" …엘릭은 아니군. 그래도 고맙네. 이 둘의 가족에게도 소식을 전해야겠어. 바다 사람은 서로의 이름을 기억해 주는 게 일이라네.",
      [deliver("rtshop:crew_name_tag", 2, "선원의 이름표")], gold=2800, requires=[p])
p = q("story_3_05", ch(C3, 5, N3), "가시고기 수프", ARCH,
      "원정대가 추위에 지쳐 가요. 얼음 미로의 가시고기는 독가시만 조심하면 기름이 많아서 몸이 데워진대요. 가시고기를 잡아 오고, 따뜻한 스튜도 같이 보내 주세요. 원정대가 미로 깊은 곳까지 버틸 수 있도록요.",
      "가시고기 6마리와 버섯 스튜 4그릇을 원정대에 보내 주세요.",
      "고마워요. 원정대가 한숨 돌렸어요. 그리고 북쪽 어부들이 그러는데, 물고기가 사라진 건 바다가 얼기 시작해서였대요. 처음부터 징조였던 거죠. 미로 한가운데 물 밑 길도 이제 열 수 있대요.",
      [deliver("aquamirae:spinefish", 6, "가시고기", A), deliver("minecraft:mushroom_stew", 4, "버섯 스튜")], gold=3400, requires=[p], flags=["seal_maze_mother"])
p = q("story_3_06", ch(C3, 6, N3), "얼음 위의 꽃", FISHER,
      "북쪽 바다 사람들은 돌아오지 못한 이를 위해 얼음 위에 꽃을 놓는다네. 눈 속에서 피는 등나무 꽃, 위스테리아 니베이스. 엘릭과 선원들을 위해 꽃을 놓아 주게. …나는 열다섯 해 동안 한 번도 놓지 못했다네. 놓으면 정말 끝인 것 같아서.",
      "얼음 미로의 눈꽃 등나무(위스테리아 니베이스) 4송이를 어부 오스카에게 가져다주게.",
      "(오스카가 꽃을 얼음 위에 하나씩 놓는다.) …놓고 나니 이상하게 숨이 쉬어지는군. 끝이 아니라 인사였어. 고맙네.",
      [deliver("aquamirae:wisteria_niveis", 4, "눈꽃 등나무", A)], gold=3000, requires=[p])
p = q("story_3_07", ch(C3, 7, N3), "심해의 등불", FISHER,
      "얼음 아래는 칠흑이라네. 그래도 빛을 내는 놈들이 있지. 아귀는 머리에 등불을 달고 먹이를 꾀고, 심연 해파리는 떠다니며 빛난다네. 아귀의 등불, 에스카를 가져오게. 그걸로 등불을 만들면 얼음 아래 영혼들도 빛을 따라올 걸세.",
      "아귀 2마리와 심연 해파리 3마리를 물리치고, 에스카 1개를 가져오게.",
      "(오스카가 에스카를 등불 틀에 넣는다. 푸른빛이 번진다.) 이제 얼음 아래가 보이는군. …저기, 무언가 크게 움직였네. 미로의 어머니라 불리는 놈일세.",
      [kill("aquamirae:anglerfish", 2, "아귀", A), kill("aquamirae:abyssal_scyphoid", 3, "심연 해파리", A), deliver("aquamirae:esca", 1, "에스카", A)],
      gold=3800, requires=[p])
p = q("story_3_08", ch(C3, 8, N3), "갈라지는 얼음", ARCH,
      "조사대원님! 오스카 씨가 새 등불을 들고 혼자 얼음 위로 나갔어요. 엘릭을 부르면서요. 그런데 얼음이 갈라졌대요. 고통받는 영혼들이 오스카 씨를 물 밑으로 끌어당기고 있어요! 미로의 어머니가 부르는 거예요!",
      "오스카를 끌어당기는 고통받는 영혼을 3명 물리쳐 오스카를 구하세요.",
      "(오스카가 젖은 채 떨며 등불을 꼭 쥐고 있다.) \"…밑에서 엘릭이 불렀다네. 분명히 들었어. 그런데 엘릭 목소리 뒤에서 거대한 무언가가 숨을 쉬더군.\" 미로의 어머니예요. 영혼들을 미끼로 쓰고 있어요.",
      [kill("aquamirae:tortured_soul", 3, "오스카를 끄는 영혼", A)], gold=3600, requires=[p])
p = q("story_3_09", ch(C3, 9, N3), "미로의 어머니", CAPT,
      "얼음 아래 미로를 지키는 놈이다. 미로의 어머니. 물속에서 싸우는 건 미친 짓이다. 기억의 문을 열어 놈을 끌어내라. 놈 뱃속에 미로가 삼킨 것들이 다 들어 있다.",
      "기억의 문을 열고 미로의 어머니를 쓰러뜨려라.",
      "놈의 뱃속에서 얼어붙은 열쇠가 나왔다. 코넬리아의 선장실 열쇠일 거다. 그리고… 이름표가 하나 더. '엘릭'.",
      [kill("aquamirae:maze_mother", 1, "미로의 어머니", A)], gold=4800, items=[item("rtshop:crew_name_tag"), item("aquamirae:frozen_key", 1, A)], requires=[p], flags=["ch3_found_elric"],
      sceneAccept=line("어부 오스카", "엘릭의 목소리를 미끼로 썼다지. 용서하지 말게."))
p = q("story_3_10", ch(C3, 10, N3), "배들의 무덤", CAPT,
      "열쇠가 가리키는 곳은 배들의 무덤이다. 얼음에 갇힌 난파선들 사이로 큰 입을 가진 놈들과 장어가 돈다. 놈들을 정리해라. 물 밑으로 내려갈 잠수 장비는 내가 마련해 뒀다. 세 개의 볼트로 조인 투구다.",
      "배들의 무덤의 큰입(모) 3마리와 장어 1마리를 물리쳐라.",
      "장어 뱃속에서 선원 하나의 단검이 나왔다. 손잡이에 회색 금화가 박혀 있었다. …코넬리아의 선원들은 회색 금화로 품삯을 받았다. 항로를 산 값이었겠지.",
      [kill("aquamirae:maw", 3, "큰입", A), kill("aquamirae:eel", 1, "장어", A)], gold=4400,
      items=[item("aquamirae:three_bolt_helmet", 1, A)], requires=[p])
p = q("story_3_11", ch(C3, 11, N3), "유령선", CAPT,
      "무덤 한가운데 얼음에 갇힌 배가 있다. 코넬리아의 배다. 선장실 문은 그 열쇠로 열릴 거다. 선장실에서 선장을 부를 방법을 찾아라.",
      "얼음 위의 유령선을 찾아가라.",
      "선장실 책상 위에 소라 나팔이 있었다. 이걸 불면 선장이 깨어날 거다. 결계도 이제 너를 막지 않는다. 오스카에게 먼저 알려라. …책상에 해도가 펼쳐져 있었다고? 바다 한가운데 동그라미 하나. '여덟 번째 눈'.",
      [struct("aquamirae:ship", "유령선", A)], gold=4000, requires=[p], flags=["seal_cornelia"])
p = q("story_3_12", ch(C3, 12, N3), "선장에게 묶인 영혼들", FISHER,
      "소라 나팔 얘기는 들었네. …잠깐만 기다려 주게. 선원들의 영혼은 선장에게 묶여 있다고 들었네. 선장을 쓰러뜨리면 엘릭도 같이 사라지는 건 아닌가? 열다섯 해를 기다렸는데, 내 손으로 동생을 지우게 되는 건 아닌가?",
      "어부 오스카와 이야기하게.",
      "(이사벨이 항해법전을 펼친다.) \"선장이 쓰러지면 묶인 자들은 풀려난다. 선장은 배와 함께 가라앉는다.\" …풀려난다라. 그 말을 믿겠네. 가게. 그리고 선장에게 전해 주게. 엘릭을 데려가 줘서, 그래도 고마웠다고.",
      [talk(FISHER, "어부 오스카", [
          "어부 오스카: 선장은 엘릭에게 키를 잡게 해 줬다네. 엘릭이 편지에 그렇게 자랑했지.",
          "어부 오스카: 그런 사람이 왜 항로를 믿었을까. 회색 망토는 무슨 말로 그 사람을 꾀었을까.",
          "어부 오스카: …원망은 그 망토에게 하겠네. 선장에게는 하지 않겠네.",
      ])], gold=3400, requires=[p])
p = q("story_3_13", ch(C3, 13, N3), "얼어붙은 바다의 선장", CAPT,
      "기억의 문을 열고 코넬리아 선장을 쓰러뜨려라. 선장은 열다섯 해 동안 선원들을 기다렸다. 칼끝에 독이 있다. 원망은 놈이 아니라 항로를 판 놈에게 돌려라.",
      "기억의 문을 열고 코넬리아 선장을 쓰러뜨려라.",
      "선장이… 선원들의 이름을 하나씩 부르며 사라졌다. 엘릭의 이름도 있었다. 남은 건 얼어붙은 항해일지뿐이다.",
      [kill("aquamirae:captain_cornelia", 1, "코넬리아 선장", A)], gold=7500, items=[item("rtshop:frozen_logbook")], requires=[p],
      sceneAccept=title("제 3 장 · 유령선", "얼어붙은 바다의 선장", "열다섯 해 동안 선원들을 기다린 사람", "9FD8FF", "frost", "minecraft:entity.elder_guardian.curse"),
      sceneDone={"kind": "vision", "speaker": "코넬리아 선장", "text": "나는 항로를 믿었다. 그 학자를 믿었다.\n…내 선원들을 찾아다오. 얼음 아래에서 아직 노래하고 있다.", "color": "9FD8FF",
                 "sound": "minecraft:entity.elder_guardian.curse", "shake": 2, "seconds": 8})
p = q("story_3_14", ch(C3, 14, N3), "항해일지", ARCH,
      "항해일지를 녹여 읽어 볼게요. 조금만 기다려 주세요.",
      "기록관 이사벨에게 항해일지를 맡기세요.",
      "\"회색 망토의 학자가 '여덟 번째 눈'이 있는 곳이라며 항로를 팔았다. 그 항로 끝에서 폭풍을 만났고, 바다가 얼었다.\" 그리고 일지 사이에 끼어 있던 이 돌… 폭풍의 문양이 새겨져 있어요. 마지막 장에는 일곱 봉인 구조물의 위치가 적혀 있어요. 한 장은 일부러 숨겨 놓은 것 같아요.",
      [talk(ARCH, "기록관 이사벨")], gold=4400, items=[item("rtshop:chronicle_page"), item("aquamirae:rune_of_the_storm", 1, A)], requires=[p],
      flags=["clue_hidden_page"])  # 스킬라의 인장 조각은 이제 선택지(카스)에 따라 다른 때에 얻는다: story_scylla.py
p = q("story_3_15", ch(C3, 15, N3), "얼음 아래의 노래", FISHER,
      "선장이 그랬다지. 선원들이 얼음 아래에서 아직 노래한다고. …고통받는 영혼들 말일세. 그 노래는 엘릭과 선원들이었어. 내 등불을 들고 가게. 영혼들을 쓰러뜨리면 등불이 그들을 거둬 줄 걸세. 놈들을 미워하진 말게.",
      "얼음 미로의 고통받는 영혼을 10명 물리쳐 등불에 거두게.",
      "(등불 안에서 수많은 작은 빛이 흔들린다.) …들리나? 노래가 그쳤다네. 대신 누가 웃는군. 큰 소리로. 엘릭일세.",
      [kill("aquamirae:tortured_soul", 10, "고통받는 영혼", A)], gold=4400, requires=[p])
p = q("story_3_16", ch(C3, 16, N3), "놓아주지 못하는 마음", FISHER,
      "등불에 영혼들이 모여 있다네. 엘릭도. (오스카가 등불을 들여다본다.) 이 등불을 내가 가지고 있으면 엘릭은 여기 있는 거라네. 열다섯 해 만에. …그런데 등불 안이 너무 추워 보이는군.",
      "어부 오스카와 이야기하게.",
      "…자네가 정한 대로 하겠네. 바다 사람은 바다로 보내는 법이지. 아니, 아직은 못 보내는 사람도 있는 법이고.",
      [talk(FISHER, "어부 오스카", [
          "어부 오스카: 엘릭, 듣고 있나. 형이다.",
          "어부 오스카: 열다섯 해 동안 등불을 켰다네. 네가 길을 잃지 않게.",
          "어부 오스카: …조사대원, 자네가 정해 주게. 나는 못 하겠네.",
      ])], gold=3200, requires=[p],
      choices=[{"text": "등불을 열어 영혼들을 바다로 보내 주자.", "flags": ["ch3_released"]},
               {"text": "등불을 오스카에게 남겨 두자. 아직은 이별할 때가 아니다.", "flags": ["ch3_kept_lantern"]}])
p = q("story_3_17", ch(C3, 17, N3), "녹는 바다, 심연 2층", ARCH,
      "바다가 녹기 시작했대요! 어부들이 내일 그물을 다시 던진대요. 그리고 나선의 심연 2층이 열렸다는 보고가 있어요. 2층은 바다의 기억을 보여 준대요.",
      "나선의 심연 2층을 깨고 오세요.",
      "심연에서 회색 망토를 입은 사람이 코넬리아에게 해도를 건네는 장면을 보셨다고요? 얼굴은 안 보였고… 손에 장갑을 끼고 있었다고요. 어머니의 마지막 장에도 장갑이 있었어요.",
      [flag("abyss_floor_2", "나선의 심연 2층")], gold=4800, requires=[p],
      sceneDone=vision("나선의 심연", "해도를 건네는 장갑 낀 손.\n\"여덟 번째 눈은 그 항로 끝에 있소, 선장.\"", "9FD8FF", "minecraft:ambient.underwater.loop.additions.rare"))
p = q("story_3_18", ch(C3, 18, N3), "다시 던지는 그물", FISHER,
      "바다가 녹았다네. 오늘 아침 열다섯 해 만에 그물을 던졌지. …같이 걷어 주겠나? 혼자 걷기엔 그물이 무거워서 말일세.",
      "어부 오스카와 함께 첫 그물을 걷게.",
      "(그물 가득 은빛 물고기가 퍼덕인다.) 바다는 정직하다네. 기다린 만큼 줬어. 열다섯 해치를 한 번에. 하하! …고맙네. 이제 장로님께 가 보게. 항해일지 이야기를 기다리고 계실 걸세.",
      [talk(FISHER, "어부 오스카")], gold=3600, items=[item("minecraft:cooked_cod", 16)], requires=[p])
p = q("story_3_19", ch(C3, 19, N3), "일곱 봉인의 지도", ELDER,
      "항해일지에 일곱 봉인의 위치가 있다고 들었네. 가져와 보게.",
      "장로 에르윈에게 일곱 봉인의 위치를 전하게.",
      "일곱 봉인… 드디어 찾았군. (장로의 목소리가 떨린다.) 잘했네, 아주 잘했어. 이제 봉인을 '점검'하러 가세. 내가 직접 안내하겠네. …숨겨진 한 장? 신경 쓰지 말게. 일곱이면 충분하네.",
      [talk(ELDER, "장로 에르윈")], gold=4800, requires=[p], flags=["story_ch3_done"],
      sceneDone=line("어부 오스카", "3장 「얼어붙은 바다의 항해일지」 끝. 바다가 녹았다."))

# ================================================================ 4장 「일곱 봉인」 — 에르윈의 이야기 (L_Ender's Cataclysm)
# 봉인마다: 발단·전개(찾아가기와 수호자의 부하들) → 위기(봉인마다 다른 사고) → 절정(수호자)
# 장의 절정: 서리의 왕 말레딕투스, 그리고 장갑 아래의 낙인
C4 = "4장 일곱 봉인"
K = "cataclysm"
N4 = 23
p = q("story_4_01", ch(C4, 1, N4), "봉인 점검", ELDER,
      "봉인이 흔들리고 있네. 일곱 봉인을 하나씩 '점검'해야 하네. 봉인마다 수호자가 깨어나 있을 걸세. 쓰러뜨리면 봉인의 눈이 드러나지. 눈은 내게 가져오게. 내가 안전하게 보관하겠네.",
      "장로 에르윈의 이야기를 듣게.",
      "고맙네. 천 년… 아니, 오래 기다렸네. 첫 봉인은 지옥의 영혼 대장간일세. (장로가 장갑을 고쳐 낀다. 장갑 안쪽이 희미하게 빛난다.)",
      [talk(ELDER, "장로 에르윈", [
          "수도회 장로 에르윈: 일곱 봉인은 천 년 전 일곱 왕이 불을 나눠 가둔 자리일세.",
          "수도회 장로 에르윈: 봉인마다 그 왕국의 비극이 남아 있지. 수호자들은 그 비극을 지키는 걸세.",
          "수도회 장로 에르윈: 조사대에게 부탁하는 건 미안하네. 하지만 나는… 이 손으로는 갈 수 없네.",
      ])], gold=5000, requires=["story_3_19"],
      sceneAccept=title("제 4 장", "일곱 봉인", "장로 에르윈, 천 년의 장갑", "FF9A40", "ember", "minecraft:block.respawn_anchor.charge"))

# (번호, 제목, 구조물, [(잡몹, 수, 이름)], 보스, 보스이름, 봉인표식, 눈, 함께 가는 사람, 동행 대사,
#  위기(제목, 주는 사람, 소개, 목표 설명, 끝, 목표들), 보스 끝 대사, 환영 대사, 테마, 보스 전 한마디)
SEALS = [
    ("02", "영혼 대장간", "cataclysm:soul_black_smith", [("cataclysm:ignited_revenant", 4, "불타는 망령")],
     "cataclysm:netherite_monstrosity", "네더라이트 몬스트로시티", "seal_monstrosity", "cataclysm:monstrous_eye",
     "대장장이 브람", "지옥의 대장장이들은 불을 훔쳐 거인을 벼렸다. 그 거인은 지금도 망치질을 멈추지 않는다.",
     ("식지 않는 화로", CAPT,
      "큰일이다. 대장장이 브람이 거인의 열기에 망치를 잃었다. 거인의 화로 곁에선 보통 쇠가 녹는다. 브람이 네더라이트 조각으로 원정대의 날을 다시 벼리겠단다. 조각을 가져와라.",
      "고대 잔해를 캐 네더라이트 조각 2개를 대장장이 브람에게 가져와라.",
      "(브람이 데인 손으로 망치를 쥔다.) \"쇠는 거짓말을 안 한다. 두드린 만큼 단단해지지.\" 날이 섰다. 이제 거인과 맞설 수 있다.",
      [deliver("minecraft:netherite_scrap", 2, "네더라이트 조각")]),
     "거인의 심장이 식으며 눈이 굴러 나왔다. 대장간 벽에 새겨진 이름: '회색 망토의 의뢰'.",
     "대장장이들은 내 불을 훔쳤다.\n그 불이 그들을 삼켰지. 불은 누구의 것도 아니다.", "iron",
     "대장장이 브람: 거인의 망치를 보면 숨지 말고 옆으로 굴러라."),
    ("05", "모래 속의 왕", "cataclysm:cursed_pyramid", [("cataclysm:koboleton", 10, "코볼레톤")],
     "cataclysm:ancient_remnant", "고대 잔해", "seal_remnant", "cataclysm:desert_eye",
     "기록관 이사벨", "1장에서 찾은 두 번째 장의 '모래 속의 눈'이 여기 있어요. 사막의 왕은 백성을 지키려고 눈을 받았대요.",
     ("봉인의 문 뒤", ARCH,
      "조사대원님… 저 여기 갇혔어요. 봉인의 문을 읽다가 모래가 쏟아져 문이 닫혔어요. 문 너머에서 투기장의 검투사, 코볼레디에이터가 다가와요. 문을 지키는 놈이에요. 그놈을 쓰러뜨리면 문이 열릴 거예요. 서둘러 주세요!",
      "저주받은 피라미드의 코볼레디에이터를 쓰러뜨려 기록관 이사벨을 구하세요.",
      "(이사벨이 모래를 털며 웃는다.) 문 안쪽 벽에 사막의 왕 이야기가 있었어요. 왕은 백성들이 모래에 묻히지 않게 자기 짐승을 수호자로 남겼대요. …그 짐승이 아직 기다리고 있어요.",
      [kill("cataclysm:kobolediator", 1, "코볼레디에이터", K)]),
     "짐승이 쓰러지며 고개를 숙였다. 마치 누군가에게 인사하듯이. 이사벨이 말했다. \"주인을 기다린 거예요. 강철의 기사처럼.\"",
     "사막의 왕은 내 벗이었다.\n그의 짐승은 아직도 주인을 기다리는구나.", "sand",
     "기록관 이사벨: 모래가 솟는 곳을 피하세요. 짐승은 땅 밑으로 숨어요."),
    ("09", "끝을 본 기계", "cataclysm:ancient_factory", [("cataclysm:the_watcher", 8, "감시자")],
     "cataclysm:the_harbinger", "하빈저", "seal_harbinger", "cataclysm:mech_eye",
     "탐험대장 로란", "기계의 나라는 미래를 계산하다가 스스로 멈췄다더군. 마지막 계산 결과가 '끝'이었다고.",
     ("사냥하는 기계", CAPT,
      "공장 깊은 곳에서 무언가가 우리를 쫓는다. 프라울러. 사냥하는 기계다. 놈이 원정대 뒤를 밟으며 하나씩 노린다. 내가 미끼가 되겠다. 놈이 나를 쫓는 동안 쓰러뜨려라. …이번엔 내가 남는 게 아니다. 너희가 올 걸 아니까.",
      "고대 공장의 프라울러를 쓰러뜨려라.",
      "(로란이 숨을 몰아쉬며 웃는다.) 스무 해 전 카엘이 이런 기분이었겠군. 뒤에서 누가 올 거라고 믿는 기분. …나쁘지 않다.",
      [kill("cataclysm:the_prowler", 1, "프라울러", K)]),
     "하빈저의 마지막 예언이 울렸다. \"마지막 문을 여는 것은 불씨를 쥔 자들이다. 그 손을 이끄는 것은 장갑이다.\"",
     "기계는 끝을 보았다.\n그 끝에 서 있는 것은… 너희다.", "mech",
     "탐험대장 로란: 놈의 미사일은 따라온다. 기둥 뒤로 돌아라."),
    ("12", "심연의 수호자", "cataclysm:sunken_city", [("cataclysm:deepling", 8, "딥링")],
     "cataclysm:the_leviathan", "리바이어던", "seal_leviathan", "cataclysm:abyss_eye",
     "어부 오스카", "바다 밑 도시의 사람들도 바다를 정직하다고 믿었겠지. 그런데 누가 그들의 눈을 빼앗았다네.",
     ("때 이른 각성", FISHER,
      "이보게, 도시 한가운데서 딥링 사제들이 노래를 하고 있네. 리바이어던을 때보다 먼저 깨우려는 걸세. 깨어나면 바다가 다시 얼어붙을지도 모르네. 사제들과 그들을 지키는 덩치들을 막아 주게. 바다는… 이제 막 녹았다네.",
      "가라앉은 도시의 딥링 사제 2명과 딥링 브루트 2명을 물리치게.",
      "노래가 그쳤군. 바다가 조용해졌어. …사제들 목에 회색 금화가 걸려 있었다고? 바다 밑까지 그 금화가 내려왔단 말인가.",
      [kill("cataclysm:deepling_priest", 2, "딥링 사제", K), kill("cataclysm:deepling_brute", 2, "딥링 브루트", K)]),
     "리바이어던이 눈을 감았다. 천 년 만에 처음으로. 오스카가 조용히 모자를 벗었다.",
     "바다 밑 도시는 눈을 빼앗기고 울었다.\n빼앗은 자는 회색 망토를 입고 있었다.", "abyss",
     "어부 오스카: 물 밖에선 놈이 힘을 못 쓴다네. 물가로 끌어내게."),
]

prev = p
for i, (n, t, st, mobs, boss, bname, seal, eye, who, whoLine, crisis, after, vis, theme, preLine) in enumerate(SEALS):
    num = int(n)
    a = q("story_4_%s" % n, ch(C4, num, N4), "%s — 찾아가기" % t, ELDER,
          ("첫 번째 봉인일세. " if i == 0 else "다음 봉인일세. ") + "이번에는 %s(이)가 함께 가겠다는군. 봉인 주변의 수호자 부하들부터 정리하게. 봉인의 눈이 드러나면 내게 보여 주게." % who,
          "%s을(를) 찾아가고, 그곳의 몬스터를 물리치게." % t,
          "도착했군. 결계는 이제 자네를 막지 않네. …봉인을 '점검'하는 걸세. 잊지 말게.",
          [struct(st, t, K)] + [kill(m, c, nm, K) for (m, c, nm) in mobs], gold=5200 + i * 300, items=[item(eye, 1, K)], requires=[prev], flags=[seal],
          sceneAccept=title("제 4 장 · %s" % t, t, "일곱 봉인", "FF9A40", theme),
          sceneDone=line(who, whoLine))
    ct, cg, ci, cp, cd, cgoals = crisis
    c = q("story_4_%02d" % (num + 1), ch(C4, num + 1, N4), "%s — %s" % (t, ct), cg, ci, cp, cd, cgoals, gold=5200 + i * 300, requires=[a])
    b = q("story_4_%02d" % (num + 2), ch(C4, num + 2, N4), "%s — %s" % (t, bname), ELDER,
          "봉인의 수호자 %s(이)가 깨어났네. 기억의 문을 열어 쓰러뜨리게. 혼자 어렵다면 동료를 모으게." % bname,
          "기억의 문을 열고 %s을(를) 쓰러뜨리게." % bname,
          after, [kill(boss, 1, bname, K)], gold=9000 + i * 500, items=[item("rtshop:chronicle_page")], requires=[c],
          sceneAccept=line(*preLine.split(": ", 1)),
          sceneDone=vision("불타는 기사의 환영", vis))
    prev = b
    if n == "05":
        # 나선의 심연 3층 (모래 다음, 기계 전에)
        prev = q("story_4_08", ch(C4, 8, N4), "여덟 왕의 회의", ARCH,
                 "나선의 심연 3층이 열렸어요. 3층은 천 년 전 '여덟 왕의 회의'를 보여 준대요. 연대기에는 일곱 왕뿐인데요. 꼭 보고 와 주세요.",
                 "나선의 심연 3층을 깨고 오세요.",
                 "여덟 번째 왕은 봉인을 거부했대요. '불을 나누면 언젠가 누군가 다시 모은다'고요. 그래서 이름이 지워지고 허공 끝으로 쫓겨났대요. 그리고 그 왕 곁에 회색 망토를 입은 젊은 제자가 서 있었다고요? …어머니의 메아리가 했던 말이랑 같아요. '다 모이면 누군가 그것을 쓴다.'",
                 [flag("abyss_floor_3", "나선의 심연 3층")], gold=6000, requires=[prev], flags=["clue_eighth_king"],
                 sceneDone=vision("나선의 심연", "\"불을 나누면, 언젠가 누군가 다시 모은다.\"\n여덟 번째 왕 곁에 회색 망토의 제자가 서 있다.", "B48CFF", "minecraft:block.end_portal.spawn"))

# 허공: 자수정 둥지(발단) → 폐허 성채(전개) → 엔더 골렘(위기) → 엔더 가디언(절정)
p = q("story_4_15", ch(C4, 15, N4), "자수정의 렌즈", ELDER,
      "다섯 번째 봉인은 허공의 땅, 무너진 성채에 있네. 그런데 성채는 허공의 안개에 숨어 보이지 않지. 자수정 둥지에 사는 거대한 게의 등딱지로 렌즈를 깎으면 안개 너머가 보인다네. 둥지를 찾아 게를 쓰러뜨리게.",
      "땅 밑의 자수정 둥지를 찾고, 자수정 게를 쓰러뜨리게.",
      "훌륭하네. 이 렌즈로 보면… 보이는군. 여덟 개의 왕좌가 있던 성채일세. (장로가 렌즈를 오래 들여다본다. 손이 떨린다.)",
      [struct("cataclysm:amethyst_nest", "자수정 둥지", K), kill("cataclysm:amethyst_crab", 1, "자수정 게", K)], gold=6200, requires=[prev])
a = q("story_4_16", ch(C4, 16, N4), "빈 왕좌의 문지기 — 찾아가기", ELDER,
      "허공의 땅으로 가게. 렌즈가 가리키는 폐허 성채로. 엔더맙테라 떼를 정리하고 성채로 들어가게.",
      "엔드의 폐허 성채를 찾고, 엔더맙테라를 8마리 물리치게.",
      "왕좌실의 여덟 번째 자리에 긁힌 이름이 있었다고? …읽지 말게. 읽을 수 없을 걸세.",
      [dim("minecraft:the_end", "엔드"), struct("cataclysm:ruined_citadel", "폐허 성채", K), kill("cataclysm:endermaptera", 8, "엔더맙테라", K)],
      gold=6400, items=[item("cataclysm:void_eye", 1, K)], requires=[p], flags=["seal_ender_guardian"],
      sceneAccept=title("제 4 장 · 빈 왕좌의 문지기", "빈 왕좌의 문지기", "일곱 봉인", "B48CFF", "void"))
c = q("story_4_17", ch(C4, 17, N4), "빈 왕좌의 문지기 — 공허의 골렘", ARCH,
      "조사대원님, 왕좌실로 가는 길을 엔더 골렘이 막고 있어요. 공허의 돌로 만든 거인이에요. 그런데 이상해요. 골렘이 지키는 건 문이 아니라… 여덟 번째 왕좌예요. 누가 거기 앉지 못하게요. 골렘을 쓰러뜨려야 제단에 닿아요.",
      "폐허 성채의 엔더 골렘을 쓰러뜨리세요.",
      "골렘이 무너진 자리에 회색 천 조각이 걸려 있었어요. 망토 자락이요. 천 년이 지났는데도 해지지 않았어요. …누군가 여기까지 와서, 여덟 번째 왕좌에 앉으려 했던 거예요.",
      [kill("cataclysm:ender_golem", 1, "엔더 골렘", K)], gold=6400, requires=[a])
b = q("story_4_18", ch(C4, 18, N4), "빈 왕좌의 문지기 — 엔더 가디언", ELDER,
      "빈 왕좌를 지키는 엔더 가디언을 기억의 문 너머에서 쓰러뜨리게. 회색 천 조각? …오래된 이야기일세. 서두르게.",
      "기억의 문을 열고 엔더 가디언을 쓰러뜨리게.",
      "가디언이 무너지며 여덟 번째 왕좌를 가리켰다. 그 자리에 앉은 사람은 없었다. 천 년 동안.",
      [kill("cataclysm:ender_guardian", 1, "엔더 가디언", K)], gold=11000, items=[item("rtshop:chronicle_page")], requires=[c],
      sceneAccept=line("기록관 이사벨", "회색 망토… 장로님이 대답을 피했어요."),
      sceneDone=vision("불타는 기사의 환영", "여덟 개의 왕좌. 하나는 지워졌다.\n지운 것은 우리 일곱이다. 그 죄도 내가 짊어지마."))

# 서리의 왕: 장의 절정
a = q("story_4_19", ch(C4, 19, N4), "봉인의 문 — 찾아가기", ELDER,
      "마지막 봉인일세. 서리의 왕이 잠든 얼어붙은 감옥. 드라우그들이 지키고 있네. 정예 드라우그는 방패를 들었으니 뒤를 노리게. …이것만 끝나면 되네. 이것만.",
      "얼어붙은 감옥을 찾아가고, 드라우그 8명과 정예 드라우그 2명을 물리치게.",
      "도착했군. 이제 마지막이네. (장로의 장갑에서 연기가 피어오른다.)",
      [struct("cataclysm:frosted_prison", "얼어붙은 감옥", K), kill("cataclysm:draugr", 8, "드라우그", K), kill("cataclysm:elite_draugr", 2, "정예 드라우그", K)],
      gold=7000, items=[item("cataclysm:cursed_eye", 1, K)], requires=[b], flags=["seal_maledictus"],
      sceneAccept=title("제 4 장 · 마지막 봉인", "봉인의 문", "서리의 왕이 잠든 감옥", "A0C8FF", "curse", "minecraft:entity.player.hurt_freeze"))
c = q("story_4_20", ch(C4, 20, N4), "봉인의 문 — 왕의 근위대", ARCH,
      "조사대원님, 감옥 안쪽에 왕의 근위대가 있어요. 왕관을 쓴 드라우그와 거대한 앞트강그르요. …그리고 할 말이 있어요. 하빈저의 예언, 회색 천 조각, 장로님의 장갑. 어머니가 마지막 장에 쓴 '장갑'. 저 이제 그만 모른 척할래요. 근위대를 쓰러뜨리고 왕을 만나면, 왕에게 직접 물어볼 거예요. 회색 망토가 누구냐고.",
      "얼어붙은 감옥의 왕관 쓴 드라우그와 앞트강그르를 쓰러뜨리세요.",
      "근위대가 쓰러졌어요. 왕의 묘비가 갈라지고 있어요. …장로님이 감옥 입구에 와 계세요. 들어오지는 않으시고요. 장갑에서 연기가 계속 나요.",
      [kill("cataclysm:royal_draugr", 1, "왕관 쓴 드라우그", K), kill("cataclysm:aptrgangr", 1, "앞트강그르", K)], gold=7000, requires=[a])
b = q("story_4_21", ch(C4, 21, N4), "봉인의 문 — 서리의 왕 말레딕투스", ELDER,
      "서리의 왕 말레딕투스를 기억의 문 너머에서 쓰러뜨리게. 왕은 허공을 날며 저주받은 창을 던지고, 쓰러진 자의 혼을 거둔다네. …그리고 쓰러지면서 무슨 말을 하든 귀 기울이지 말게. 절대로.",
      "기억의 문을 열고 말레딕투스를 쓰러뜨리게.",
      "\"회색 망토의 학자여… 약속을 지켜라. 끝나지 않는 삶을.\" 말레딕투스는 그렇게 말하고 부서졌다. 감옥 입구에서 장갑 낀 손이 문틀을 움켜쥐었다.",
      [kill("cataclysm:maledictus", 1, "서리의 왕 말레딕투스", K)], gold=15000, items=[item("rtshop:chronicle_page")], requires=[c],
      sceneAccept=title("제 4 장 · 서리의 왕", "말레딕투스", "끝나지 않는 삶을 약속받은 왕", "A0C8FF", "curse", "minecraft:entity.wither.ambient"),
      sceneDone={"kind": "vision", "speaker": "서리의 왕 말레딕투스", "text": "회색 망토의 학자여… 약속을 지켜라.\n끝나지 않는 삶을. 너와 같은 삶을.", "color": "A0C8FF",
                 "sound": "minecraft:entity.elder_guardian.curse", "shake": 3, "seconds": 9})
p = q("story_4_22", ch(C4, 22, N4), "장갑 아래의 낙인", ARCH,
      "말레딕투스의 마지막 말, 들으셨어요? 회색 망토의 학자. 회색 금화. 항로를 판 학자. 장갑을 낀 손. 어머니의 일지. 전부… 한 사람을 가리켜요. 장로님께 가 보세요. 지금 바로요. 저도 같이 갈게요.",
      "장로 에르윈에게 가세요.",
      "(장로가 천천히 장갑을 벗는다. 손등에 불꽃 모양의 낙인이 타오른다.) \"여섯 눈이면 충분하네. 천 년을 기다렸지. …자네들 조사대는 쓸모가 있었네.\" 장로는 여섯 눈을 빼앗아 불꽃 속으로 사라졌다.",
      [talk(ELDER, "장로 에르윈", [
          "수도회 장로 에르윈: 왔군. 이사벨도 왔구나. 그 아이는 제 어미를 닮아 영리하네.",
          "수도회 장로 에르윈: 엘레나도 끝까지 읽었지. 그래서 돌아오지 못했네. 허공 끝으로 가는 길을 알려 준 건 나였으니까.",
          "수도회 장로 에르윈: 마지막으로 제안하지. 불씨를 내게 넘기게. 그러면 조사대는 살려 주겠네.",
      ])], gold=8000, requires=[b], turnIn=ELDER, flags=["erwin_betrayal"],
      choices=[{"text": "제안을 받아들이는 척하며 시간을 번다.", "flags": ["ch4_feigned"]},
               {"text": "\"불을 지키고, 봉인을 지킨다.\" 거절한다.", "flags": ["ch4_refused"]}],
      sceneDone={"kind": "vision", "speaker": "장로 에르윈", "text": "불을 지키고, 봉인을 지킨다? 하하…\n봉인을 푼 것은 자네들일세. 고맙네.", "color": "FF3A20",
                 "sound": "minecraft:entity.wither.spawn", "shake": 4, "seconds": 9})
p = q("story_4_23", ch(C4, 23, N4), "꺼진 등불", ARCH,
      "…수도회 기록고의 등불이 꺼졌어요. 어머니의 등불이요. 장로님이… 에르윈이 사라지면서 불을 가져갔어요. 저는 괜찮아요. 정말이에요. 다만 조금만, 여기 같이 앉아 있어 주세요.",
      "기록관 이사벨 곁에 있어 주세요.",
      "어머니의 마지막 장을 다시 읽었어요. \"그 손은 장갑을 끼고 있다.\" …어머니는 알고 있었어요. 알면서도 이 마을을 떠나지 않았어요. 우리를 지키려고요. 이제 제 차례예요. 탐험대장님이 원정대를 모으고 계세요.",
      [talk(ARCH, "기록관 이사벨")], gold=6000, requires=[p], flags=["story_ch4_done"],
      sceneDone=line("기록관 이사벨", "4장 「일곱 봉인」 끝. 불을 지키겠다던 사람이 불을 가져갔다."))

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
      ])], gold=8000, items=[item("cataclysm:flame_eye", 1, K)], requires=["story_4_23"], flags=["seal_ignis"],
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
  ])], gold=3000, requires=["story_4_08"], requiresFlags=["ch1_saved_apprentice"], flags=["clue_purple_ink"], mod=I)
p = q("hidden_illusioner_0", CH + " (단서)", "보랏빛 잉크의 행방", ARCH,
      "항해일지의 숨은 쪽에도 보랏빛 잉크가 묻어 있었어요. 1장에서 마녀의 잉크 단지에서 본 그 색이요. 그 잉크는 소환사들이 거울 속 사람들에게서 받아 쓴대요. 그런데 소환사들은 숨어 살아서… 마을을 습격할 때만 모습을 드러내요. 습격을 막다가 소환사를 만나면, 그 잉크를 찾아 주세요.",
      "마을 습격(레이드)에 나타나는 소환사를 3명 물리치세요.",
      "보랏빛 잉크병이에요. 병 바닥에 거울 문양이 있어요. …불타는 기록고에서 살아남은 기록에도 같은 잉크가 있었을 거예요. 이제 숲속 저택의 안개가 당신을 알아볼지도 몰라요.",
      [kill("minecraft:evoker", 3, "습격의 소환사")], gold=5000, requires=["story_3_14"], requiresFlags=["clue_hidden_page"], blockedByFlags=["clue_purple_ink"],
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
    ("story_3_01", FISHER): [(-1, "엘릭을 꼭 찾아 드릴게요.")],
    ("story_3_12", FISHER): [(-1, "그 망토, 반드시 찾아내겠습니다.")],
    ("story_3_16", FISHER): [(-1, "…제가 정하겠습니다.")],
    ("story_4_01", ELDER): [(-1, "맡겨 주세요. 봉인은 저희가 살피겠습니다.")],
    ("story_4_22", ELDER): [(-1, "대답은… 곧 들려 드리죠.")],
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
for _prefix in ("story_0_", "story_1_", "story_2_"):
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
