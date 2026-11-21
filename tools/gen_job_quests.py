import json, sys
out = sys.argv[1]

npcs = [
 {"id":"sword_master","name":"검술 교관 레온","profession":"weaponsmith","greeting":"검은 정직한 무기다. 휘두른 만큼만 돌려주지. 배울 각오가 됐나?"},
 {"id":"war_chief","name":"전사 대장 브론","profession":"armorer","greeting":"하하! 덩치 좋은 녀석이 왔군. 도끼 한 자루면 세상 무서울 게 없지!"},
 {"id":"shield_keeper","name":"방패 수호자 가렌","profession":"mason","greeting":"동료 앞에 서는 자가 가장 늦게 쓰러져야 한다. 그게 수호자의 일이다."},
 {"id":"hunter","name":"사냥꾼 리아","profession":"fletcher","greeting":"쉿, 바람 소리를 들어 봐. 활은 눈보다 귀로 쏘는 거야."},
 {"id":"archmage","name":"대마법사 엘리스","profession":"cleric","greeting":"마력은 책 속에만 있지 않단다. 세상 곳곳에 흩어져 있지."},
]

def q(id, chain, order, title, giver, intro, progress, done, objectives, gold, requires=None, flags=None, items=None, turnIn=None):
    o = {"id": id, "chain": chain, "order": order, "title": title, "giver": giver}
    if turnIn: o["turnIn"] = turnIn
    if requires: o["requires"] = requires
    o.update({"intro": intro, "progress": progress, "done": done, "objectives": objectives})
    r = {"gold": gold}
    if items: r["items"] = items
    if flags: r["flags"] = flags
    o["reward"] = r
    return o

def D(t, n, text=None):
    d = {"type": "DELIVER", "target": t, "count": n}
    if text: d["text"] = text
    return d
def K(t, n, text=None):
    d = {"type": "KILL", "target": t, "count": n}
    if text: d["text"] = text
    return d
def T(t): return {"type": "TALK", "target": t}

jobs = [
 # id, 이름, 스승 키, 스승 이름, 순서, 1단계 소개, [2,3,4단계]
 ("swordsman", "검사", "quest_sword_master", "검술 교관 레온", 100,
  "검을 주로 다루는 검사에 관심이 있나? 마을의 검술 교관 레온을 찾아가 보게. 엄하지만 실력은 확실한 사람이야.",
  [("검을 쥐는 법", "검사는 자기 검을 스스로 마련할 줄 알아야 한다. 철검 한 자루를 만들어 가져와라. 날이 잘 섰는지 내가 직접 보겠다.",
    "철검 한 자루를 가져와라.", "좋은 검이군. 이제 이걸 쓸 줄 아는지 보자.", [D("minecraft:iron_sword", 1)], 200),
   ("첫 실전", "연습은 끝났다. 밤에 나타나는 좀비와 스켈레톤을 상대해 봐라. 검사는 물러서지 않는다.",
    "좀비 8마리와 스켈레톤 4마리를 처치하라.", "살아 돌아왔군. 검이 손에 붙기 시작했을 거다.",
    [K("minecraft:zombie", 8), K("minecraft:skeleton", 4)], 300),
   ("검사의 증표", "마지막이다. 거미는 빠르고 교활하지. 거미 6마리를 베고, 그 거미줄을 증거로 가져와라. 그러면 너를 검사로 인정하마.",
    "거미 6마리를 처치하고 실 6개를 가져와라.", "훌륭하다. 이제부터 너는 검사다. 직업 안내인에게 가서 검사를 고르도록.",
    [K("minecraft:spider", 6), D("minecraft:string", 6)], 500)]),
 ("warrior", "전사", "quest_war_chief", "전사 대장 브론", 110,
  "힘으로 밀어붙이는 전사가 되고 싶다고? 그렇다면 전사 대장 브론을 만나 보게. 목소리가 커서 금방 찾을 걸세.",
  [("전사의 도끼", "전사에게는 묵직한 도끼가 어울리지! 철도끼 하나를 만들어 와라. 나무 패는 데 쓰던 거라도 상관없다, 하하!",
    "철도끼 하나를 가져와라.", "오, 제법 묵직하군! 이제 휘둘러 볼 차례다.", [D("minecraft:iron_axe", 1)], 200),
   ("정면 돌파", "전사는 피하지 않는다. 좀비 10마리와 크리퍼 3마리를 쓰러뜨려 와라. 크리퍼는 터지기 전에 끝내는 게 요령이다!",
    "좀비 10마리와 크리퍼 3마리를 처치하라.", "몸 성히 왔군! 역시 내 눈이 틀리지 않았어.",
    [K("minecraft:zombie", 10), K("minecraft:creeper", 3)], 300),
   ("전사의 갑주", "진짜 전사는 맞아도 버틴다. 가죽 8장을 모아 와라. 네 갑옷을 손질해 주마. 그걸로 너는 어엿한 전사다!",
    "가죽 8장을 가져와라.", "좋아, 이제 너는 전사다! 직업 안내인에게 가서 전사를 고르게.",
    [D("minecraft:leather", 8)], 500)]),
 ("tank", "탱커", "quest_shield_keeper", "방패 수호자 가렌", 120,
  "동료를 지키는 탱커의 길을 걷고 싶은가? 방패 수호자 가렌을 찾아가게. 말수는 적지만 믿음직한 사람일세.",
  [("방패를 들다", "수호자의 시작은 방패다. 방패 하나를 만들어 와라.", "방패 하나를 가져와라.",
    "튼튼하군. 이제 그 방패 뒤에 설 몸을 단련하자.", [D("minecraft:shield", 1)], 200),
   ("무거운 갑옷", "탱커는 가벼운 차림으로는 버틸 수 없다. 철 흉갑을 만들어 가져와라. 무겁겠지만 익숙해질 거다.",
    "철 흉갑 하나를 가져와라.", "좋다. 무게는 곧 너의 힘이 된다.", [D("minecraft:iron_chestplate", 1)], 300),
   ("화살비 속에서", "마지막 시험이다. 스켈레톤의 화살을 막아 내며 8마리를 쓰러뜨려라. 물러서지 않는 자만이 수호자다.",
    "스켈레톤 8마리를 처치하라.", "끝까지 버텼군. 이제 너는 탱커다. 직업 안내인에게 가서 탱커를 고르도록 해라.",
    [K("minecraft:skeleton", 8)], 500)]),
 ("archer", "궁수", "quest_hunter", "사냥꾼 리아", 130,
  "멀리서 적을 꿰뚫는 궁수에 관심이 있다면, 사냥꾼 리아를 찾아가 보게. 숲 냄새가 나는 아가씨라네.",
  [("활과 화살", "궁수에게 활은 몸의 일부야. 활 하나와 화살 32개를 준비해서 보여 줘.", "활 하나와 화살 32개를 가져와.",
    "좋아, 시위가 팽팽하네. 화살은 다시 돌려줄게, 써야 하니까.", [D("minecraft:bow", 1), D("minecraft:arrow", 32)], 200),
   ("거리를 두고", "궁수는 다가오기 전에 끝내는 거야. 크리퍼 4마리와 스켈레톤 6마리를 처치해 봐. 크리퍼는 절대 가까이 오게 하지 마.",
    "크리퍼 4마리와 스켈레톤 6마리를 처치하라.", "깔끔한데? 감이 좋아.",
    [K("minecraft:creeper", 4), K("minecraft:skeleton", 6)], 300),
   ("화살 만들기", "좋은 궁수는 화살도 스스로 만들지. 깃털 16개와 부싯돌 8개를 모아 와. 그걸로 네 몫의 화살을 만들어 줄게.",
    "깃털 16개와 부싯돌 8개를 가져와라.", "이제 너도 궁수야! 직업 안내인에게 가서 궁수를 골라. 화살은 선물이야.",
    [D("minecraft:feather", 16), D("minecraft:flint", 8)], 500, [{"item": "minecraft:arrow", "count": 64}])]),
 ("mage", "마법사", "quest_archmage", "대마법사 엘리스", 140,
  "마법의 길에 관심이 있는가? 대마법사 엘리스를 찾아가 보게. 책과 별을 좋아하는 분이라네.",
  [("마력의 매개", "마법은 매개가 있어야 다룰 수 있단다. 청금석 16개와 책 3권을 가져오렴.", "청금석 16개와 책 3권을 가져와라.",
    "좋아요. 이 청금석에서 마력이 느껴지는구나.", [D("minecraft:lapis_lazuli", 16), D("minecraft:book", 3)], 200),
   ("빛나는 결정", "마력은 땅속 깊은 곳에 고여 있지. 자수정 조각 8개와 발광석 가루 8개를 모아 오렴.",
    "자수정 조각 8개와 발광석 가루 8개를 가져와라.", "아름답구나. 이제 마지막 시험만 남았어.",
    [D("minecraft:amethyst_shard", 8), D("minecraft:glowstone_dust", 8)], 300),
   ("차원의 조각", "엔더맨은 차원 사이를 오가는 마력 덩어리야. 엔더맨 2마리를 쓰러뜨리고 엔더 진주 2개를 가져오렴. 눈을 마주치지 않게 조심하고.",
    "엔더맨 2마리를 처치하고 엔더 진주 2개를 가져와라.", "훌륭해요. 이제 그대는 마법사란다. 직업 안내인에게 가서 마법사를 고르렴.",
    [K("minecraft:enderman", 2), D("minecraft:ender_pearl", 2)], 500)]),
 ("farmer", "농부", "shop_farmer", "농작물 상인", 150,
  "땅을 일구는 농부가 되고 싶다면 농작물 상인을 찾아가 보게. 씨앗도 그 사람에게서 사야 하니 인사해 두면 좋을 걸세.",
  [("첫 밭", "씨앗은 내게서 사서 심어야 자란다네. 밀을 길러서 16개를 가져와 보게.", "밀 16개를 가져와라.",
    "잘 여물었군. 농사에 소질이 있어.", [D("minecraft:wheat", 16)], 200),
   ("뿌리 작물", "이번엔 땅속에서 자라는 작물이야. 당근 16개와 감자 16개를 길러 오게.", "당근 16개와 감자 16개를 가져와라.",
    "좋아, 밭이 제법 넓어졌겠는걸.", [D("minecraft:carrot", 16), D("minecraft:potato", 16)], 300),
   ("수확제", "마지막으로 호박 4개와 사탕수수 16개를 가져오게. 마을 수확제에 쓸 거라네. 그러면 자네도 어엿한 농부야.",
    "호박 4개와 사탕수수 16개를 가져와라.", "풍성하군! 이제 자네는 농부일세. 직업 안내인에게 가서 농부를 고르게.",
    [D("minecraft:pumpkin", 4), D("minecraft:sugar_cane", 16)], 500)]),
 ("fisher", "어부", "shop_fisher", "수산물 상인", 160,
  "물가에서 시간을 보내는 걸 좋아한다면 어부는 어떤가? 수산물 상인을 찾아가 보게.",
  [("첫 낚시", "낚시는 기다림이야. 아무 물고기나 5마리 낚아 오게.", "물고기 5마리를 가져와라.",
    "오, 손맛을 봤구먼.", [D("#minecraft:fishes", 5, "물고기")], 200),
   ("물때를 읽다", "물고기는 시간과 장소에 따라 달라지지. 이번엔 물고기 12마리를 낚아 오게.", "물고기 12마리를 가져와라.",
    "꾸준하구먼. 그게 어부의 덕목이지.", [D("#minecraft:fishes", 12, "물고기")], 300),
   ("월척", "마지막이야. 물고기 20마리를 낚아 오게. 그리고 낚싯대를 하나 더 만들어 와. 어부는 여벌 낚싯대가 있어야 하거든.",
    "물고기 20마리와 낚싯대 하나를 가져와라.", "대단하구먼! 이제 자네는 어부야. 직업 안내인에게 가서 어부를 고르게.",
    [D("#minecraft:fishes", 20, "물고기"), D("minecraft:fishing_rod", 1)], 500)]),
 ("miner", "광부", "shop_blacksmith", "장비 상인", 170,
  "땅속 깊이 파고드는 광부가 되고 싶다면 장비 상인을 찾아가 보게. 광물은 그 사람이 사 주거든.",
  [("곡괭이질", "광부의 시작은 석탄과 철이지. 석탄 16개와 철 원석 8개를 캐 오게.", "석탄 16개와 철 원석 8개를 가져와라.",
    "좋아, 손에 물집 좀 잡혔겠군.", [D("minecraft:coal", 16), D("minecraft:raw_iron", 8)], 200),
   ("깊은 곳으로", "더 깊이 내려가 보게. 금 원석 4개와 레드스톤 가루 16개를 가져오게. 용암 조심하고.",
    "금 원석 4개와 레드스톤 가루 16개를 가져와라.", "제법 깊이 들어갔군.", [D("minecraft:raw_gold", 4), D("minecraft:redstone", 16)], 300),
   ("빛나는 돌", "광부라면 다이아몬드 하나쯤은 캐 봐야지. 다이아몬드 1개와 청금석 8개를 가져오게.",
    "다이아몬드 1개와 청금석 8개를 가져와라.", "다이아몬드라! 이제 자네는 광부일세. 직업 안내인에게 가서 광부를 고르게.",
    [D("minecraft:diamond", 1), D("minecraft:lapis_lazuli", 8)], 500)]),
 ("cook", "요리사", "shop_food", "식품 상인", 180,
  "음식으로 사람을 행복하게 하고 싶다면 요리사가 어떤가? 식품 상인을 찾아가 보게. 재료는 거기서 다 살 수 있다네.",
  [("빵 굽기", "요리의 기본은 빵이지. 빵 4개를 만들어 오게. 밀은 내게서 사도 되네.", "빵 4개를 가져와라.",
    "고소한 냄새가 나는군.", [D("minecraft:bread", 4)], 200),
   ("따뜻한 한 그릇", "이번엔 그릇 요리야. 버섯 스튜 2그릇과 구운 소고기 4개를 만들어 오게.", "버섯 스튜 2개와 스테이크 4개를 가져와라.",
    "든든하겠구먼.", [D("minecraft:mushroom_stew", 2), D("minecraft:cooked_beef", 4)], 300),
   ("잔칫상", "마지막은 잔칫상이야. 케이크 1개와 호박 파이 2개를 만들어 오게. 손님들이 기다리고 있다네.",
    "케이크 1개와 호박 파이 2개를 가져와라.", "훌륭한 솜씨야! 이제 자네는 요리사일세. 직업 안내인에게 가서 요리사를 고르게.",
    [D("minecraft:cake", 1), D("minecraft:pumpkin_pie", 2)], 500)]),
]

quests = []
for jid, jname, mentor, mname, order, intro1, steps in jobs:
    chain = jname + " 선행 퀘스트"
    first = q(f"job_{jid}_1", f"{chain} (1/4)", order, f"{jname}의 길", "job",
              intro1, f"{mname} 에게 말을 걸어라.", f"{mname} 을(를) 만났군. 이제 그 사람의 시험을 받아 보게.",
              [T(mentor)], 100, turnIn="job")
    quests.append(first)
    prev = first["id"]
    for i, step in enumerate(steps):
        title, intro, prog, done, objs, gold = step[:6]
        items = step[6] if len(step) > 6 else None
        last = i == len(steps) - 1
        qq = q(f"job_{jid}_{i+2}", f"{chain} ({i+2}/4)", order + i + 1, title, mentor, intro, prog, done, objs, gold,
               requires=[prev], flags=[f"unlock_job_{jid}"] if last else None, items=items)
        quests.append(qq)
        prev = qq["id"]

doc = {"_help": "직업별 선행 퀘스트 (직업마다 4단계). 1단계는 직업 안내인(job)에게서 받고, 2~4단계는 각 직업의 스승에게서 받습니다. "
                "마지막 단계의 보상 표식(unlock_job_<직업>)을 얻어야 그 직업을 고를 수 있습니다. 게임 안 퀘스트 편집기(/rtshop quest edit)로 고칠 수 있고, "
                "이 파일은 한 번 설치된 뒤에는 모드 업데이트로 덮어쓰지 않습니다. 전투 직업 스승은 /rtshop npc quest_sword_master 처럼 소환합니다.",
       "npcs": npcs, "quests": quests}
json.dump(doc, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(len(quests), "quests")
