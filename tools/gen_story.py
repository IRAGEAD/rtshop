# 메인 스토리 「골디아 연대기」 퀘스트를 만든다: python3 tools/gen_story.py
# 결과: src/main/resources/rtshop_defaults/quests/story.json
import json, os

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'rtshop_defaults', 'quests', 'story.json')

ELDER, ARCH, CAPT = 'quest_order_elder', 'quest_order_archivist', 'quest_order_captain'
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
      requiresFlags=None, turnIn=None, sceneAccept=None, sceneDone=None, choices=None, mod=None):
    order[0] += 10
    o = {"id": id, "chain": chain, "order": order[0], "title": t, "giver": giver}
    if mod: o["mod"] = mod
    if turnIn: o["turnIn"] = turnIn
    if requires: o["requires"] = requires
    if requiresFlags: o["requiresFlags"] = requiresFlags
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
    Q.append(o)
    return id

# ================================================================ 1장 「잿불의 서약」
C1 = "1장 잿불의 서약"
p = q("story_1_1", C1 + " (1/8)", "폭풍 뒤의 부두", ELDER,
      "폭풍을 뚫고 배가 들어왔다더니, 자네였군. 이곳은 골디아, 잿불 수도회의 마지막 거점일세. 이 땅에서 살아가려면 먼저 삶의 기반부터 다져야 하네. 안내인 모리스에게서 '초보 모험가의 길'을 다 배우고 오게.",
      "모리스의 가르침(초보 모험가의 길)을 모두 마치고 오게.",
      "잘했네. 이제 자네도 이 땅의 사람일세. …손은 왜 보냐고? 이 장갑은 오래된 습관이야. 신경 쓰지 말게.",
      [flag("tutorial_done", "초보 모험가의 길 마치기")], gold=500,
      sceneAccept=title("제 1 장", "잿불의 서약", "폭풍이 지나간 부두, 골디아", "FF7A20", "ember", "minecraft:entity.lightning_bolt.thunder"))
p = q("story_1_2", C1 + " (2/8)", "견습 서약", ELDER,
      "수도회의 견습이 되려면 서약을 해야 하네. 기록관 이사벨에게 가서 서약서를 받아 오게. 그리고 내 앞에서 외우는 거야. \"불을 지키고, 봉인을 지킨다.\"",
      "기록관 이사벨을 만나고 오게.",
      "\"불을 지키고, 봉인을 지킨다.\" …좋네. 오늘부터 자네는 잿불 수도회의 견습일세. 오늘 밤은 푹 자 두게. 첫날 밤엔 다들 이상한 꿈을 꾸거든.",
      [talk(ARCH, "기록관 이사벨", [
          "기록관 이사벨: 아, 장로님이 보내셨군요! 서약서는 여기 있어요.",
          "기록관 이사벨: 서약은 세 줄이에요. 첫째, 불을 지킨다. 둘째, 봉인을 지킨다. 셋째… 셋째 줄은 지워져 있네요. 이상하다.",
          "기록관 이사벨: 신경 쓰지 마세요. 오래된 종이니까요. 장로님께 가져가서 외우면 돼요.",
      ])], gold=800, items=[item("rtshop:oath_scroll"), item("rtshop:undying_ember")], requires=[p], flags=["order_apprentice"],
      sceneDone=vision("꿈속의 불타는 기사", "또 왔구나, 견습이여.\n이 불씨를 쥐어라. 언젠가 네가 지켜야 할 것이다."))
p = q("story_1_3", C1 + " (3/8)", "찢긴 연대기", ARCH,
      "수도회의 「잿빛 연대기」예요. 일곱 세력이 세계의 힘을 '눈'에 담아 봉인한 이야기가 적혀 있죠. 그런데… 보세요. 이 부분은 낡아서 떨어진 게 아니라, 누군가 칼로 잘라 낸 자국이에요. 잃어버린 페이지는 옛 유적에 흩어졌다는 소문이 있어요. 무너진 차원문 근처부터 찾아봐 주세요.",
      "무너진 차원문을 찾아가 보세요.",
      "탄 자국이 남은 종이… 연대기의 첫 페이지예요! \"일곱은 불을 가두었고, 불은 한 사람을 가두었다.\" 한 사람이라니, 누구일까요.",
      [struct("#minecraft:ruined_portal", "무너진 차원문")], gold=900, items=[item("rtshop:chronicle_page")], requires=[p])
p = q("story_1_4", C1 + " (4/8)", "원정의 밑천", CAPT,
      "견습이 됐다고? 축하한다. 그런데 맨손으로 원정을 갈 셈이냐? 원정에는 돈과 기술이 든다. 직업 스승 알버트에게 가서 네 길을 정하고 와라. 직업이 있어야 원정대에 보급품을 댈 수 있다.",
      "직업 스승 알버트와 이야기하고 오너라.",
      "좋아. 원정대는 서로의 기술로 굴러간다. 대장장이는 칼을 벼리고, 요리사는 수프를 끓이고, 어부는 바다를 읽지.",
      [talk("job", "직업 스승")], gold=800, requires=[p])
p = q("story_1_5", C1 + " (5/8)", "벼린 칼날", CAPT,
      "네 칼을 보자. …이걸로는 사막의 바람도 못 벤다. 대장장이에게 장비를 강화받아 보고, 철 장비로 몸을 감싸라. 그리고 원정 식량도 챙기고.",
      "철 갑옷 한 벌과 원정 식량을 가져와라.",
      "이제야 원정대 꼴이 나는군. 식량은 원정대 창고에 넣어 두마.",
      [deliver("minecraft:iron_chestplate", 1, "철 흉갑"), deliver("minecraft:cooked_beef", 16, "익힌 소고기 (원정 식량)")],
      gold=1500, requires=[p])
p = q("story_1_6", C1 + " (6/8)", "모래 속의 두 번째 장", ARCH,
      "어부 오스카가 투덜대더라고요. 북쪽 바다에서 물고기가 사라진다고요. …그건 그렇고, 두 번째 페이지의 단서를 찾았어요. 사막의 피라미드 아래에 연대기를 숨겼다는 기록이 있어요.",
      "사막 피라미드를 찾아가 보세요.",
      "\"사막의 왕은 모래 속의 눈을 받았고, 서리의 왕은 얼음 속의 눈을 받았다.\" 일곱 눈 중 둘이에요. 나머지는… 여기서 잘려 있어요.",
      [struct("minecraft:desert_pyramid", "사막 피라미드")], gold=1000, items=[item("rtshop:chronicle_page")], requires=[p])
p = q("story_1_7", C1 + " (7/8)", "불타는 하늘", ELDER,
      "요즘 밤마다 남쪽 하늘이 붉게 물드는 걸 봤나? 네더의 불이 들끓고 있다는 징조일세. 차원문을 열고 네더에 가서 하늘을 보고 오게. 자네 눈으로 직접.",
      "네더에 들어가 하늘을 보고 오게.",
      "…그랬군. 하늘이 불타고 있었단 말이지. 서두르지 않으면 안 되겠어. (장로가 장갑 낀 손을 꽉 쥔다.)",
      [dim("minecraft:the_nether", "네더")], gold=1200, requires=[p],
      sceneDone=vision("꿈속의 불타는 기사", "불이… 나를 넘어서려 한다.\n견습이여, 아직은 오지 마라.", "FF5020", "minecraft:block.fire.ambient"))
p = q("story_1_8", C1 + " (8/8)", "깨어나는 수호자들", ELDER,
      "수도회의 옛 기록에 이런 구절이 있네. \"불이 깨어나면 땅의 수호자들이 먼저 미친다.\" 지하의 녹슨 기사, 얼음 거인, 산꼭대기의 석공, 태양의 사제… 그들이 깨어나고 있어. 탐험대장 로란에게 보고하게. 이제부터가 진짜 원정일세.",
      "탐험대장 로란에게 보고하게.",
      "수호자들이라… 좋다, 원정대를 꾸리지. 이제부터 장비 상인도 원정대에 쓸 좋은 장비(희귀 등급)를 내놓을 거다.",
      [talk(CAPT, "탐험대장 로란", [
          "탐험대장 로란: 장로가 보냈냐. 얼굴 보니 알겠다. 수호자들 이야기지?",
          "탐험대장 로란: 이십 년 전에도 같은 일이 있었다. 그때 원정대는 나만 돌아왔다.",
          "탐험대장 로란: 이번엔 다르게 한다. 네가 있으니까. 준비되면 다시 말 걸어라.",
      ])], gold=1300, requires=[p], flags=["story_ch1_done"],
      sceneDone=line("탐험대장 로란", "1장 「잿불의 서약」 끝. 수호자들이 깨어난다."))

# ================================================================ 2장 「깨어난 수호자들」 (Mowzie's Mobs)
C2 = "2장 깨어난 수호자들"
M = "mowziesmobs"
p = q("story_2_1", C2 + " (1/10)", "녹슨 맹세", CAPT,
      "광부들이 지하에서 쇠사슬 끄는 소리를 들었다더군. 그로틀이라는 광물 도둑들이 그쪽으로 몰려든다는 소문도 있고. 녹슨 문을 지키는 기사가 있다는 옛이야기… 사실이었던 모양이다. 먼저 그 방을 찾아라.",
      "지하의 녹슨 기사의 방을 찾아라.",
      "찾았다고? 문 앞의 기사는 아직 잠들어 있겠지. 이제 결계는 너를 막지 않는다. 각오가 되면 그 문을 두드려라.",
      [struct("mowziesmobs:wrought_chamber", "녹슨 기사의 방", M)], gold=1500, requires=[p], flags=["seal_wroughtnaut"],
      sceneAccept=title("제 2 장", "깨어난 수호자들", "불의 기운에 땅이 흔들린다", "C8A060", "stone", "minecraft:block.anvil.land"))
p = q("story_2_2", C2 + " (2/10)", "천 년의 파수꾼", CAPT,
      "녹슨 기사를 쓰러뜨려라. 놈의 갑옷은 단단하지만 등 뒤는 비어 있다. 혼자 어렵다면 파티를 꾸려 가라.",
      "강철의 기사를 쓰러뜨려라 (최대 체력의 10% 이상 피해).",
      "투구에 새겨진 문장… 불꽃 기사단? 수도회보다도 오래된 기사단이다. 이 녀석은 대체 누구를 기다린 거지?",
      [kill("mowziesmobs:ferrous_wroughtnaut", 1, "강철의 기사", M)], gold=3000, items=[item("rtshop:chronicle_page")], requires=[p])
p = q("story_2_3", C2 + " (3/10)", "나선의 심연", ARCH,
      "기사의 투구 안쪽에 지도가 새겨져 있었어요. 수도회가 견습을 시험하던 옛 시련장, '나선의 심연'이에요. 1층과 2층을 통과하는 것이 수도회의 정식 시련이죠.",
      "나선의 심연 1층과 2층을 깨고 오세요.",
      "시련을 통과했군요! 이제 당신은 정식 수도회원이에요. 심연의 주간 보상도 받을 수 있답니다.",
      [flag("abyss_floor_1", "나선의 심연 1층"), flag("abyss_floor_2", "나선의 심연 2층")], gold=2000, requires=[p], flags=["order_member"])
p = q("story_2_4", C2 + " (4/10)", "얼음의 심장", ELDER,
      "불에 맞서려면 불을 잠재울 차가움이 필요하네. 북쪽 설원에 잠든 얼음 거인이 그런 결정을 품고 있다고 들었네. 길목엔 식인 꽃 폴리아스가 숨어 있으니 조심하게. 먼저 거인의 거처를 찾게.",
      "설원의 얼음 거인이 잠든 곳을 찾게.",
      "찾았군. 거인은 결정을 쉽게 내주지 않을 걸세. 싸울 각오를 하든지, 몰래 가져오든지.",
      [struct("mowziesmobs:frostmaw_spawn", "얼음 거인의 거처", M), kill("mowziesmobs:foliaath", 3, "폴리아스", M)], gold=2000, requires=[p], flags=["seal_frostmaw"])
p = q("story_2_5", C2 + " (5/10)", "얼음을 품은 거인", ELDER,
      "프로스트모를 쓰러뜨리고 얼음 결정을 가져오게. 그 결정은 언젠가… 아주 중요한 순간에 쓰일 걸세.",
      "프로스트모를 쓰러뜨리게.",
      "결정이 손 안에서 떨리는군. 불을 느끼는 게야. 잘 간직하게. 절대 팔아서는 안 되네.",
      [kill("mowziesmobs:frostmaw", 1, "프로스트모", M)], gold=3500, items=[item("rtshop:frost_heart")], requires=[p],
      choices=[{"text": "거인이 잠든 사이 결정을 살짝 가져왔다고 말한다.", "flags": ["frost_stolen"]},
               {"text": "정정당당히 싸워 얻었다고 말한다.", "flags": ["frost_fought"]}])
p = q("story_2_6", C2 + " (6/10)", "산꼭대기의 석공", ARCH,
      "연대기에 '봉인을 돌에 새긴 석공'이 나와요. 아직 살아 있다면, 높은 산꼭대기의 수도원에 있을 거예요. 그분은 시험을 통과한 사람에게만 이야기를 들려준대요.",
      "산꼭대기 수도원을 찾아가세요.",
      "석공님을 만날 준비가 됐군요. 그분의 시험은 힘이 아니라 균형을 본대요.",
      [struct("mowziesmobs:monastery", "산꼭대기 수도원", M)], gold=2000, requires=[p], flags=["seal_sculptor"])
p = q("story_2_7", C2 + " (7/10)", "여덟 번째 자리", ARCH,
      "조각가 통비의 시험을 통과하세요. 그분이 쓰러지면… 아니, 시험을 마치면 무언가 말해 줄 거예요.",
      "조각가 통비의 시험을 통과하세요.",
      "\"봉인은 일곱이 아니라 여덟이다.\" …통비님이 그렇게 말했다고요? 연대기에는 일곱뿐인데. 장로님께는 아직 말하지 말아 주세요. 제가 먼저 확인해 볼게요.",
      [kill("mowziesmobs:sculptor", 1, "조각가 통비", M)], gold=3500, items=[item("rtshop:chronicle_page")], requires=[p], flags=["clue_eighth_seal"])
p = q("story_2_8", C2 + " (8/10)", "두 번째 태양", CAPT,
      "남쪽 사바나의 태양 부족이 미쳐 날뛴다. 가면을 쓴 움부타나 전사들이 마을을 덮쳤다고. 놈들의 숲을 찾아 전사들을 물리쳐라.",
      "태양 부족의 숲을 찾고 가면 전사를 물리쳐라.",
      "놈들이 '두 번째 태양'을 외쳤다고? 하늘에 태양이 둘일 리가… 아니, 너도 네더에서 붉은 하늘을 봤지.",
      [struct("mowziesmobs:umvuthana_grove", "태양 부족의 숲", M), kill("mowziesmobs:umvuthana", 6, "가면 전사", M)], gold=2500, requires=[p], flags=["seal_umvuthi"])
p = q("story_2_9", C2 + " (9/10)", "타오르는 태양", CAPT,
      "부족의 제사장 움부티가 모든 것의 원흉이다. 놈을 쓰러뜨려 부족을 풀어 줘라.",
      "태양조 움부티를 쓰러뜨려라.",
      "부족이 정신을 차렸다. 그들이 그러더군. '두 번째 태양은 남쪽이 아니라 아래에서 떠올랐다'고. 아래… 네더 말이다.",
      [kill("mowziesmobs:umvuthi", 1, "태양조 움부티", M)], gold=3500, requires=[p])
p = q("story_2_10", C2 + " (10/10)", "북쪽에서 온 소식", ELDER,
      "모두 해냈군, 견습… 아니, 이제 수도회원이지. 그런데 나쁜 소식일세. 태양의 힘이 북쪽으로 흘러가 바다가 얼어붙었다는군. 얼음 위에 유령선이 떠 있다는 소문도 있네. 나선의 심연 3층과 4층도 열렸다는 보고가 있으니, 거기서 실력을 쌓고 오게.",
      "나선의 심연 3층과 4층을 깨고 오게.",
      "좋네. 이제 장비 상인도 원정대에 최고급 장비(서사 등급)를 내놓을 걸세. 북쪽 바다로 가기 전에 단단히 준비하게.",
      [flag("abyss_floor_3", "나선의 심연 3층"), flag("abyss_floor_4", "나선의 심연 4층")], gold=2500, requires=[p], flags=["story_ch2_done"],
      sceneDone=line("수도회 장로 에르윈", "2장 「깨어난 수호자들」 끝. 바다가 얼어붙었다."))

# ================================================================ 3장 「얼어붙은 바다의 항해일지」 (Aquamirae)
C3 = "3장 얼어붙은 바다의 항해일지"
A = "aquamirae"
p = q("story_3_1", C3 + " (1/8)", "얼음 미로의 외곽", CAPT,
      "북쪽 바다가 통째로 얼었다. 얼음 사이로 미로가 생겼고, 해적들이 그 위에 전초기지를 세웠다. 원정대의 첫 발판이 될 곳이야. 해적 전초기지를 찾아라. 내한 수프는 꼭 챙기고.",
      "얼음 미로의 해적 전초기지를 찾아라.",
      "전초기지의 깃발… 코르넬리아의 해적단이다. 천 년 전에 바다와 함께 얼어붙었다는 그 선장 말이다.",
      [struct("aquamirae:outpost", "해적 전초기지", A)], gold=3000, requires=[p],
      sceneAccept=title("제 3 장", "얼어붙은 바다의 항해일지", "얼음 위에 유령선이 떠 있다", "9FD8FF", "frost", "minecraft:entity.player.hurt_freeze"))
p = q("story_3_2", C3 + " (2/8)", "고통받는 영혼들", CAPT,
      "미로를 떠도는 영혼들과 약탈자 해적이 길을 막고 있다. 놈들을 물리치고 은신처를 찾아라. 해적들이 숨긴 물건 중에 항해 기록이 있을지 모른다.",
      "해적 은신처를 찾고, 미로의 적을 물리쳐라.",
      "은신처의 벽에 낙서가 있더군. '회색 망토가 항로를 팔았다.' 회색 망토…? 수도회의 옛 학자들이 입던 옷이다.",
      [struct("aquamirae:shelter", "해적 은신처", A), kill("aquamirae:tortured_soul", 5, "고통받는 영혼", A)], gold=3500, requires=[p], flags=["clue_grey_cloak"])
p = q("story_3_3", C3 + " (3/8)", "원정대의 수프", ARCH,
      "원정대가 추위에 지쳐 가요. 따뜻한 수프와 넉넉한 식량이 필요해요. 요리사에게 부탁하거나 직접 만들어 주세요. 원정대가 오래 버틸 수 있도록요.",
      "원정대에 따뜻한 음식을 보내 주세요.",
      "고마워요. 원정대가 한숨 돌렸어요. 그리고… 북쪽 어부들이 그러는데, 물고기가 사라진 건 바다가 얼기 시작해서였대요. 처음부터 징조였던 거죠.",
      [deliver("minecraft:mushroom_stew", 8, "버섯 스튜 (내한 수프)"), deliver("minecraft:cooked_cod", 16, "익힌 대구")], gold=3000, requires=[p], flags=["seal_maze_mother"])
p = q("story_3_4", C3 + " (4/8)", "배들의 무덤", CAPT,
      "얼음 아래 심해에 배들의 무덤이 있다. 깊이 잠수하진 마라. 얼음 구멍 근처에서 짧게 내려갔다 올라와라. 미로의 어머니가 그 아래를 지킨다는 이야기가 있다.",
      "얼음 아래의 미로의 어머니를 쓰러뜨려라.",
      "놈의 뱃속에서 얼어붙은 열쇠가 나왔다. 코르넬리아의 선장실 상자 열쇠일 거다.",
      [kill("aquamirae:maze_mother", 1, "미로의 어머니", A)], gold=4000, requires=[p])
p = q("story_3_5", C3 + " (5/8)", "유령선", CAPT,
      "얼음 위에 갇힌 유령선을 찾아라. 선장실의 약탈자에게서 소라 나팔을 빼앗아야 선장을 부를 수 있다.",
      "얼음 위의 유령선을 찾아가라.",
      "소라 나팔이다. 이걸 불면 선장이 깨어날 거다. 이제 결계도 너를 막지 않는다.",
      [struct("aquamirae:ship", "유령선", A)], gold=3500, requires=[p], flags=["seal_cornelia"])
p = q("story_3_6", C3 + " (6/8)", "얼어붙은 바다의 선장", CAPT,
      "소라 나팔을 불고, 코르넬리아 선장의 유령을 쓰러뜨려라. 얼음 위에서 싸워라. 물에 빠지면 끝이다.",
      "코르넬리아 선장을 쓰러뜨려라.",
      "선장이… 선원들의 이름을 부르며 사라졌다. 남은 건 얼어붙은 항해일지뿐이다.",
      [kill("aquamirae:captain_cornelia", 1, "코르넬리아 선장", A)], gold=5000, requires=[p])
p = q("story_3_7", C3 + " (7/8)", "항해일지", ARCH,
      "항해일지를 녹여 읽어 봤어요. 코르넬리아는 '심연의 눈'과 '여덟 번째 눈'을 훔치러 갔대요. 항로를 판 건 회색 망토의 학자… 그리고 마지막 장에 일곱 봉인 구조물의 위치가 적혀 있어요. 그런데 한 장은 일부러 숨겨 놓은 것 같아요. 장로님께는 일곱 봉인 이야기만 전해 주세요.",
      "장로 에르윈에게 일곱 봉인의 위치를 전해 주세요.",
      "일곱 봉인… 드디어 찾았군. (장로의 목소리가 떨린다.) 잘했네, 아주 잘했어. 이제 봉인을 지키러 가세. 나선의 심연 5층과 6층도 열렸다는군.",
      [talk(ELDER, "장로 에르윈")], gold=4000, items=[item("rtshop:chronicle_page")], requires=[p], turnIn=ELDER, flags=["clue_hidden_page", "scylla_clue_seal_shard"])
p = q("story_3_8", C3 + " (8/8)", "심연의 끝", ELDER,
      "봉인으로 가기 전에, 나선의 심연을 끝까지 내려가 보게. 5층과 6층이야. 그곳을 지나면 일곱 봉인의 수호자들과도 겨룰 수 있을 걸세.",
      "나선의 심연 5층과 6층을 깨고 오게.",
      "자네는 이제 수도회 최고의 전사일세. 첫 번째 봉인은 네더의 영혼 대장간이네. 눈을 주겠네. 눈이 가리키는 곳으로 가게.",
      [flag("abyss_floor_5", "나선의 심연 5층"), flag("abyss_floor_6", "나선의 심연 6층")], gold=4000, requires=[p], flags=["story_ch3_done"],
      sceneDone=line("수도회 장로 에르윈", "3장 「얼어붙은 바다의 항해일지」 끝. 일곱 봉인이 기다린다."))

# ================================================================ 4장 「일곱 봉인」 (Cataclysm)
C4 = "4장 일곱 봉인"
K = "cataclysm"
EPI = [
    # (번호, 제목, 구조물, 잡몹, 잡몹수, 보스, 보스이름, 봉인표식, 이 편 시작 때 주는 눈, 환영 대사, 회수 대사)
    ("1", "영혼 대장간", "cataclysm:soul_black_smith", "minecraft:wither_skeleton", 8, "cataclysm:netherite_monstrosity", "네더라이트 몬스트로시티", "seal_monstrosity", "cataclysm:monstrous_eye",
     "대장장이들은 내 불을 훔쳤다.\n그 불이 그들을 삼켰지. 견습이여, 불은 누구의 것도 아니다.",
     "거인의 심장에서 눈이 떨어졌다. 눈을 쥐는 순간, 또 그 기사의 환영이 보였다."),
    ("2", "모래 속의 왕", "cataclysm:cursed_pyramid", "cataclysm:koboleton", 10, "cataclysm:ancient_remnant", "고대 잔해", "seal_remnant", "cataclysm:desert_eye",
     "사막의 왕은 내 벗이었다.\n그의 짐승은 아직도 주인을 기다리는구나.",
     "짐승이 쓰러지며 고개를 숙였다. 마치 누군가에게 인사하듯이."),
    ("3", "끝을 본 기계", "cataclysm:ancient_factory", "cataclysm:the_watcher", 8, "cataclysm:the_harbinger", "하빈저", "seal_harbinger", "cataclysm:mech_eye",
     "기계는 끝을 보았다.\n그 끝에 서 있는 것은… 너다, 견습이여.",
     "하빈저의 마지막 예언이 울렸다. \"마지막 문을 여는 것은 견습이다.\""),
    ("4", "심연의 수호자", "cataclysm:sunken_city", "cataclysm:deepling", 10, "cataclysm:the_leviathan", "리바이어던", "seal_leviathan", "cataclysm:abyss_eye",
     "바다 밑 도시는 눈을 빼앗기고 울었다.\n빼앗은 자는 회색 망토를 입고 있었다.",
     "리바이어던이 눈을 감았다. 천 년 만에 처음으로."),
    ("5", "빈 왕좌의 문지기", "cataclysm:ruined_citadel", "cataclysm:endermaptera", 8, "cataclysm:ender_guardian", "엔더 가디언", "seal_ender_guardian", "cataclysm:void_eye",
     "여덟 개의 왕좌. 하나는 지워졌다.\n지운 것은 우리 일곱이다. 그 죄도 내가 짊어지마.",
     "왕좌실의 여덟 번째 자리에 긁힌 이름이 있었다. 읽을 수는 없었다."),
    ("6", "봉인의 문", "cataclysm:frosted_prison", "cataclysm:draugr", 10, "cataclysm:maledictus", "말레딕투스", "seal_maledictus", "cataclysm:cursed_eye",
     "서리의 왕은 끝나지 않는 삶을 약속받았다.\n약속한 자는… 지금 네 곁에 있다.",
     ""),
]
prev = p
eyes = []
gold4 = [15000, 15000, 15000, 15000, 15000, 15000]
for i, (n, t, st, mob, mc, boss, bname, seal, eye, vis, after) in enumerate(EPI):
    ch = C4 + " (" + n + "편)"
    first = i == 0
    a = q("story_4_%sa" % n, ch, "%s — 찾아가기" % t, ELDER,
          ("첫 번째 봉인일세. 이 눈을 던지면 봉인이 있는 곳을 가리킬 걸세. " if first else "다음 봉인일세. 이 눈을 따라가게. ") + "봉인 주변의 잡것들부터 정리하게.",
          "눈이 가리키는 곳을 찾아가고, 그곳의 몬스터를 물리치게.",
          "도착했군. 결계는 이제 자네를 막지 않네. …봉인을 '지키는' 것일세. 잊지 말게.",
          [struct(st, t, K), kill(mob, mc, "", K if mob.startswith("cataclysm") else "")], gold=gold4[i] // 3, items=[item(eye, 1, K)], requires=[prev], flags=[seal],
          sceneAccept=title("제 4 장 · %s편" % n, t, "일곱 봉인", "FF9A40" if n != "6" else "A0C8FF", ["iron", "sand", "mech", "abyss", "void", "curse"][i]))
    b = q("story_4_%sb" % n, ch, "%s — %s" % (t, bname), ELDER,
          "봉인의 수호자 %s(이)가 깨어났네. 쓰러뜨리게. 혼자 어렵다면 동료를 모으게." % bname,
          "%s(을)를 쓰러뜨리게 (최대 체력의 10%% 이상 피해)." % bname,
          after if after else "…",
          [kill(boss, 1, bname, K)], gold=gold4[i] - gold4[i] // 3, items=[item("rtshop:chronicle_page")], requires=[a],
          sceneDone=vision("불타는 기사의 환영", vis))
    prev = b

# 4-6 배신 (말레딕투스 처치 직후)
p = q("story_4_6c", C4 + " (6편)", "장갑 아래의 낙인", ARCH,
      "말레딕투스가 쓰러지면서 마지막으로 한 말을 들었어요? \"회색 망토의 학자여, 약속을 지켜라.\" …회색 망토, 항로를 판 학자, 찢긴 연대기. 전부 한 사람을 가리켜요. 장로님께 가 보세요. 지금 바로요.",
      "장로 에르윈에게 가세요.",
      "(장로가 천천히 장갑을 벗는다. 손등에 불꽃 모양의 낙인이 타오른다.) \"고맙네, 견습. 여섯 눈이면 충분하네. 천 년을 기다렸지.\" …장로는 여섯 눈을 빼앗아 불꽃 속으로 사라졌다.",
      [talk(ELDER, "장로 에르윈")], gold=5000, requires=[prev], turnIn=ELDER, flags=["erwin_betrayal"],
      sceneDone={"kind": "vision", "speaker": "장로 에르윈", "text": "불을 지키고, 봉인을 지킨다? 하하…\n봉인을 푼 것은 자네일세, 견습. 고맙네.", "color": "FF3A20",
                 "sound": "minecraft:entity.wither.spawn", "shake": 3, "seconds": 7})
p = q("story_4_6d", C4 + " (6편)", "남은 불씨", ARCH,
      "장로님이… 아니, 에르윈이 불꽃의 왕좌로 갔어요. 남은 건 당신의 불씨와 얼음 결정, 그리고 이 마지막 눈뿐이에요. 탐험대장님이 원정대를 모으고 있어요. 불씨와 결정을 꼭 챙겨서 탐험대장님께 가 주세요.",
      "꺼지지 않는 불씨와 얼음 결정을 지니고 탐험대장 로란에게 가세요.",
      "이걸 가지고 있었다니. 좋다. 불꽃 경기장으로 간다. 이번엔 우리가 지킬 차례다.",
      [talk(CAPT, "탐험대장 로란")], gold=3000, items=[item("cataclysm:flame_eye", 1, K)], requires=[p], flags=["seal_ignis"])

# 4-7 결말
p = q("story_4_7a", C4 + " (7편)", "불꽃의 왕좌", CAPT,
      "불꽃 경기장이다. 에르윈이 최초의 불꽃에 손을 대려 하고 있다. 불꽃 잔당을 뚫고 경기장으로 가라. 제단에 얼음 결정을 바쳐야 왕좌가 열린다.",
      "불꽃 경기장에 들어가 얼음 결정을 바쳐라.",
      "결정이 녹으며 불길이 가라앉았다. 그런데 에르윈이… 불꽃에 삼켜졌다. 그리고 그 불꽃 속에서 무언가 깨어난다.",
      [struct("cataclysm:burning_arena", "불꽃 경기장", K), deliver("rtshop:frost_heart", 1, "얼음 결정")], gold=5000, requires=[p],
      sceneAccept=title("제 4 장 · 7편", "불꽃의 왕좌", "최초의 불꽃", "FF5A10", "ember", "minecraft:entity.blaze.ambient"))
p = q("story_4_7b", C4 + " (7편)", "화염의 군주", CAPT,
      "이그니스다. 화염의 군주… 쓰러뜨려라. 그리고 쓰러진 그에게 네 불씨를 건네라. 이유는 모르겠지만, 그래야 할 것 같다.",
      "이그니스를 쓰러뜨려라.",
      "쓰러진 기사의 눈에서 불이 꺼지고, 사람의 눈빛이 돌아왔다.",
      [kill("cataclysm:ignis", 1, "이그니스", K)], gold=10000, requires=[p],
      sceneDone={"kind": "vision", "speaker": "불꽃 기사단장 아르덴", "text": "또 왔구나, 견습이여.\n…이번에는 네가 지켜라.", "color": "FFB060",
                 "sound": "minecraft:block.respawn_anchor.deplete", "seconds": 8})
p = q("story_4_7c", C4 + " (에필로그)", "꺼지지 않는 불씨", ARCH,
      "모두 끝났어요. 강철의 기사가 기다린 주군이 누구였는지도 이제 알겠어요. …장로의 자리가 비었어요. 그 자리에서 서약을 다시 외워 주시겠어요? 이번엔 견습이 아니라 수호자로서요.",
      "부두로 돌아가 폭풍을 바라본 뒤, 기록관에게 돌아오세요.",
      "\"불을 지키고, 봉인을 지킨다.\" …최초의 불꽃은 매주 다시 그의 모습으로 타오를 거예요. 그 불을 잠재우는 게 이제 당신의 일이에요. 그리고 이 연대기의 마지막 장은… 여전히 비어 있네요.",
      [talk(CAPT, "탐험대장 로란에게 작별 인사")], gold=8000, items=[item("rtshop:chronicle_page")], requires=[p], flags=["story_done", "ignis_raid_story"],
      sceneDone=title("에필로그", "꺼지지 않는 불씨", "연대기의 마지막 장은 아직 비어 있다", "E8C878", "ember", "minecraft:music_disc.otherside"))

# ================================================================ 숨은 이야기: 환술사 (The Illusioner)
CH = "숨은 이야기 · 거울 속의 연대기"
I = "illusioner"
p = q("hidden_illusioner_1", CH + " (1/2)", "빈 쪽의 잉크", ARCH,
      "연대기 페이지들을 맞춰 보다가 이상한 걸 발견했어요. 잘린 자리마다 보랏빛 잉크가 묻어 있어요. 환술사들이 쓰는 잉크예요. 숲속 저택의 일리저들이 그런 잉크를 쓴다던데… 저택 아래에 뭔가 있을지도 몰라요.",
      "숲속 저택을 찾아가 보세요.",
      "저택 지하에서 이상한 음악 소리가 들렸다고요? 소리가 나는 상자를 건드리면 무언가 깨어날 거예요. 이제 거울도 당신을 비출 거예요.",
      [struct("minecraft:mansion", "숲속 저택")], gold=3000, requires=["story_3_7"], requiresFlags=["clue_hidden_page"], flags=["seal_illusioner"], mod=I)
p = q("hidden_illusioner_2", CH + " (2/2)", "거짓 연대기의 필경사", ARCH,
      "저택 아래의 환술사를 쓰러뜨리세요. 그가 연대기를 고쳐 썼다면, 진짜 페이지를 가지고 있을 거예요.",
      "저택 아래의 환술사를 쓰러뜨리세요.",
      "진짜 연대기의 한 장이에요! \"회색 망토의 학자 에르윈은 불꽃의 조각을 훔쳤고, 우리는 그의 이름을 지웠다. 그리고 그는 우리의 기록을 지웠다.\" …이걸 장로님이 보시면 안 돼요.",
      [kill("illusioner:illusioner", 1, "환술사", I)], gold=6000, items=[item("rtshop:chronicle_page", 2)], requires=[p], flags=["clue_true_page"], mod=I,
      sceneDone=vision("환술사 벨로크", "이 이야기는… 내가 쓴 것이다.\n진짜 마지막 장은 폭풍 속에 있다.", "B48CFF", "minecraft:entity.illusioner.mirror_move"))

data = {
    "_help": "메인 스토리 「골디아 연대기」. 이 파일은 모드 업데이트 때 새 기본값으로 바뀔 수 있으니, 고친 퀘스트는 별도 파일에 같은 ID 로 적지 말고 이 파일을 직접 고쳐 주세요 (고친 파일은 덮어쓰지 않습니다). "
             "목표 종류: STRUCTURE(구조물 안), ADVANCEMENT(발전과제), DIMENSION(차원), FLAG(표식). sceneAccept/sceneDone: 받을 때/끝낼 때 연출. "
             "seal_* 표식은 결계(config/rtshop/seals.json)를 연다. NPC 는 /rtshop npc quest_order_elder 처럼 소환합니다.",
    "npcs": NPCS,
    "quests": Q,
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=1)
print(len(Q), "quests,", sum(x["reward"].get("gold", 0) for x in Q), "G total")
