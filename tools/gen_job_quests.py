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

def D(t, n, text=None, mod=None):
    d = {"type": "DELIVER", "target": t, "count": n}
    if text: d["text"] = text
    if mod: d["mod"] = mod
    return d
def K(t, n, text=None):
    d = {"type": "KILL", "target": t, "count": n}
    if text: d["text"] = text
    return d
def T(t): return {"type": "TALK", "target": t}
def R(item, n=1, mod=None):
    r = {"item": item, "count": n}
    if mod: r["mod"] = mod
    return r

jobs = [
 ("swordsman", "검사", "quest_sword_master", "검술 교관 레온", 100,
  "검을 주로 다루는 검사에 관심이 있나요? 마을의 검술 교관 레온을 찾아가 보세요. 엄하지만 실력은 확실한 분이에요.",
  [("검을 쥐는 법", "검사는 자기 검을 스스로 마련할 줄 알아야 한다. 철검 한 자루를 만들어 가져와라. 날이 잘 섰는지 내가 직접 보겠다.",
    "철검 한 자루를 가져와라.", "좋은 검이군. 이제 이걸 쓸 줄 아는지 보자.", [D("minecraft:iron_sword", 1)], 100),
   ("기초 수련", "검은 휘두른 만큼 손에 붙는다. 좀비 12마리를 상대로 기본기를 익혀라.",
    "좀비 12마리를 처치하라.", "자세가 조금 잡혔군.", [K("minecraft:zombie", 12)], 150),
   ("밤의 시험", "밤에는 적이 사방에서 온다. 스켈레톤 8마리와 거미 6마리를 베어라. 화살을 피하며 파고드는 게 요령이다.",
    "스켈레톤 8마리와 거미 6마리를 처치하라.", "살아 돌아왔군. 검이 손에 붙기 시작했을 거다.",
    [K("minecraft:skeleton", 8), K("minecraft:spider", 6)], 200),
   ("검 손질", "검사는 검을 아낄 줄 알아야 한다. 철 주괴 8개와 숫돌 하나를 가져와라. 네 검을 함께 손질하자.",
    "철 주괴 8개와 숫돌 하나를 가져와라.", "날이 다시 섰다. 이제 강한 적을 상대해 보자.",
    [D("minecraft:iron_ingot", 8), D("minecraft:grindstone", 1)], 250),
   ("빠르게, 정확하게", "크리퍼는 망설이면 터진다. 크리퍼 5마리와 거미 8마리를 처치해라. 빠른 판단이 검사의 무기다.",
    "크리퍼 5마리와 거미 8마리를 처치하라.", "제법 빨라졌군.", [K("minecraft:creeper", 5), K("minecraft:spider", 8)], 300),
   ("검사의 증표", "마지막이다. 스켈레톤 10마리를 베고, 그 뼈 16개를 증거로 가져와라. 그러면 너를 검사로 인정하마.",
    "스켈레톤 10마리를 처치하고 뼈 16개를 가져와라.", "훌륭하다. 이제부터 너는 검사다. 직업 안내인에게 가서 검사를 고르도록.",
    [K("minecraft:skeleton", 10), D("minecraft:bone", 16)], 500)]),
 ("warrior", "전사", "quest_war_chief", "전사 대장 브론", 110,
  "힘으로 밀어붙이는 전사가 되고 싶은가요? 그렇다면 전사 대장 브론을 만나 보세요. 목소리가 커서 금방 찾을 거예요.",
  [("전사의 도끼", "전사에게는 묵직한 도끼가 어울리지! 철도끼 하나를 만들어 와라.",
    "철도끼 하나를 가져와라.", "오, 제법 묵직하군! 이제 휘둘러 볼 차례다.", [D("minecraft:iron_axe", 1)], 100),
   ("힘 자랑", "도끼는 힘으로 휘두르는 거다! 좀비 15마리를 쓰러뜨려 와라.", "좀비 15마리를 처치하라.",
    "땀 좀 흘렸겠군! 좋아.", [K("minecraft:zombie", 15)], 150),
   ("정면 돌파", "전사는 피하지 않는다. 크리퍼 5마리와 스켈레톤 5마리를 쓰러뜨려 와라. 크리퍼는 터지기 전에 끝내는 게 요령이다!",
    "크리퍼 5마리와 스켈레톤 5마리를 처치하라.", "몸 성히 왔군! 역시 내 눈이 틀리지 않았어.",
    [K("minecraft:creeper", 5), K("minecraft:skeleton", 5)], 200),
   ("갑옷 준비", "맞아도 버티려면 갑옷이 있어야지. 가죽 12장과 철 주괴 6개를 모아 와라.",
    "가죽 12장과 철 주괴 6개를 가져와라.", "좋은 재료군! 갑옷을 만들 준비가 됐다.", [D("minecraft:leather", 12), D("minecraft:iron_ingot", 6)], 250),
   ("난전", "전장은 늘 어지럽지. 거미 8마리와 좀비 10마리를 한데 상대해 봐라!",
    "거미 8마리와 좀비 10마리를 처치하라.", "하하! 이제 진짜 전사 같구나!", [K("minecraft:spider", 8), K("minecraft:zombie", 10)], 300),
   ("전사의 갑주", "마지막이다! 철 흉갑과 철 투구를 만들어 와라. 그걸 걸치면 너는 어엿한 전사다!",
    "철 흉갑 1개와 철 투구 1개를 가져와라.", "좋아, 이제 너는 전사다! 직업 안내인에게 가서 전사를 골라라!",
    [D("minecraft:iron_chestplate", 1), D("minecraft:iron_helmet", 1)], 500)]),
 ("tank", "탱커", "quest_shield_keeper", "방패 수호자 가렌", 120,
  "동료를 지키는 탱커의 길을 걷고 싶은가요? 방패 수호자 가렌을 찾아가 보세요. 말수는 적지만 믿음직한 분이에요.",
  [("방패를 들다", "수호자의 시작은 방패다. 방패 하나를 만들어 와라.", "방패 하나를 가져와라.",
    "튼튼하군.", [D("minecraft:shield", 1)], 100),
   ("무거운 갑옷", "탱커는 가벼운 차림으로는 버틸 수 없다. 철 흉갑을 만들어 가져와라.",
    "철 흉갑 하나를 가져와라.", "좋다. 무게는 곧 너의 힘이 된다.", [D("minecraft:iron_chestplate", 1)], 150),
   ("버티기", "방패를 들고 좀비 12마리를 상대해라. 막고, 밀어내고, 쓰러뜨려라.", "좀비 12마리를 처치하라.",
    "물러서지 않았군.", [K("minecraft:zombie", 12)], 200),
   ("완전 무장", "다리와 발도 지켜야 한다. 철 레깅스와 철 부츠를 만들어 와라.", "철 레깅스 1개와 철 부츠 1개를 가져와라.",
    "이제 온몸이 강철이군.", [D("minecraft:iron_leggings", 1), D("minecraft:iron_boots", 1)], 250),
   ("화살비 속에서", "스켈레톤의 화살을 막아 내며 10마리를 쓰러뜨려라.", "스켈레톤 10마리를 처치하라.",
    "화살이 두렵지 않게 됐을 거다.", [K("minecraft:skeleton", 10)], 300),
   ("수호자의 맹세", "마지막 시험이다. 크리퍼 4마리를 쓰러뜨리고, 철 블록 2개를 가져와라. 그 철로 네 방패를 보강해 주마.",
    "크리퍼 4마리를 처치하고 철 블록 2개를 가져와라.", "끝까지 버텼군. 이제 너는 탱커다. 직업 안내인에게 가서 탱커를 고르도록 해라.",
    [K("minecraft:creeper", 4), D("minecraft:iron_block", 2)], 500)]),
 ("archer", "궁수", "quest_hunter", "사냥꾼 리아", 130,
  "멀리서 적을 꿰뚫는 궁수에 관심이 있다면, 사냥꾼 리아를 찾아가 보세요. 숲 냄새가 나는 아가씨예요.",
  [("활과 화살", "궁수에게 활은 몸의 일부야. 활 하나와 화살 32개를 준비해서 보여 줘.", "활 하나와 화살 32개를 가져와.",
    "좋아, 시위가 팽팽하네.", [D("minecraft:bow", 1), D("minecraft:arrow", 32)], 100),
   ("화살 재료", "좋은 궁수는 화살도 스스로 만들지. 깃털 16개와 부싯돌 8개를 모아 와.",
    "깃털 16개와 부싯돌 8개를 가져와라.", "좋은 재료야.", [D("minecraft:feather", 16), D("minecraft:flint", 8)], 150),
   ("첫 사냥", "이제 실전이야. 스켈레톤 8마리를 맞혀 봐. 저쪽도 활을 쏘니까 조심하고.",
    "스켈레톤 8마리를 처치하라.", "제법인데?", [K("minecraft:skeleton", 8)], 200),
   ("새 시위", "시위가 닳았을 거야. 거미 8마리를 잡고 실 8개를 가져와. 새 시위를 메어 줄게.",
    "거미 8마리를 처치하고 실 8개를 가져와라.", "이걸로 시위가 더 팽팽해질 거야.", [K("minecraft:spider", 8), D("minecraft:string", 8)], 250),
   ("거리를 두고", "궁수는 다가오기 전에 끝내는 거야. 크리퍼 6마리를 처치해 봐. 절대 가까이 오게 하지 마.",
    "크리퍼 6마리를 처치하라.", "깔끔해. 감이 좋아.", [K("minecraft:creeper", 6)], 300),
   ("명사수", "마지막이야. 석궁 하나를 만들고, 스켈레톤 10마리를 더 처치해 와. 그러면 너도 궁수야.",
    "석궁 1개를 가져오고 스켈레톤 10마리를 처치하라.", "이제 너도 궁수야! 직업 안내인에게 가서 궁수를 골라. 화살은 선물이야.",
    [D("minecraft:crossbow", 1), K("minecraft:skeleton", 10)], 500, [{"item": "minecraft:arrow", "count": 64}])]),
 ("mage", "마법사", "quest_archmage", "대마법사 엘리스", 140,
  "마법의 길에 관심이 있나요? 대마법사 엘리스를 찾아가 보세요. 책과 별을 좋아하는 분이에요.",
  [("마력의 매개", "마법은 매개가 있어야 다룰 수 있단다. 청금석 16개와 책 3권을 가져오렴.", "청금석 16개와 책 3권을 가져와라.",
    "이 청금석에서 마력이 느껴지는구나.", [D("minecraft:lapis_lazuli", 16), D("minecraft:book", 3)], 100),
   ("빛나는 결정", "마력은 땅속 깊은 곳에 고여 있지. 자수정 조각 12개와 발광석 가루 12개를 모아 오렴.",
    "자수정 조각 12개와 발광석 가루 12개를 가져와라.", "아름답구나.", [D("minecraft:amethyst_shard", 12), D("minecraft:glowstone_dust", 12)], 150),
   ("마력의 흐름", "레드스톤은 마력을 흘려보내는 길이란다. 레드스톤 가루 32개를 가져오렴.", "레드스톤 가루 32개를 가져와라.",
    "좋아, 마력이 잘 흐르겠구나.", [D("minecraft:redstone", 32)], 200),
   ("폭발의 원리", "불꽃과 폭발도 마법의 한 갈래란다. 크리퍼 4마리를 처치하고 화약 8개를 가져오렴.",
    "크리퍼 4마리를 처치하고 화약 8개를 가져와라.", "위험한 재료지만 쓸모가 많단다.", [K("minecraft:creeper", 4), D("minecraft:gunpowder", 8)], 250),
   ("차원의 조각", "엔더맨은 차원 사이를 오가는 마력 덩어리란다. 엔더맨 3마리를 쓰러뜨리고 엔더 진주 4개를 가져오렴. 눈을 마주치지 않게 조심하고.",
    "엔더맨 3마리를 처치하고 엔더 진주 4개를 가져와라.", "대단하구나.", [K("minecraft:enderman", 3), D("minecraft:ender_pearl", 4)], 300),
   ("마법사의 서재", "마지막으로 마법 부여대를 만들어 오렴. 그대만의 서재를 여는 첫걸음이 될 거란다.",
    "마법 부여대 1개를 가져와라.", "훌륭하구나. 이제 그대는 마법사란다. 직업 안내인에게 가서 마법사를 고르렴.",
    [D("minecraft:enchanting_table", 1)], 500)]),
 ("farmer", "농부", "shop_farmer", "농작물 상인", 150,
  "땅을 일구는 농부가 되고 싶다면 농작물 상인을 찾아가 보세요. 씨앗도 그분에게서 사야 하니 인사해 두면 좋을 거예요.",
  [("첫 밭", "씨앗은 저한테서 사서 심어야 자라요. 밀을 길러서 32개를 가져와 보세요!", "밀 32개를 가져와라.",
    "잘 여물었네요! 농사에 소질이 있어요.", [D("minecraft:wheat", 32)], 100),
   ("뿌리 작물", "이번엔 땅속에서 자라는 작물이에요. 당근 24개와 감자 24개를 길러 오세요.", "당근 24개와 감자 24개를 가져와라.",
    "좋아요, 밭이 제법 넓어졌겠는걸요?", [D("minecraft:carrot", 24), D("minecraft:potato", 24)], 150),
   ("붉은 뿌리", "비트도 길러 보세요. 수프 재료로 인기가 많거든요. 비트 24개를 가져오세요.", "비트 24개를 가져와라.",
    "색이 참 곱네요!", [D("minecraft:beetroot", 24)], 200),
   ("덩굴 작물", "호박과 수박은 덩굴 옆에 열려요. 호박 8개와 수박 조각 16개를 가져오세요.", "호박 8개와 수박 조각 16개를 가져와라.",
    "탐스럽게 열렸네요!", [D("minecraft:pumpkin", 8), D("minecraft:melon_slice", 16)], 250),
   ("농장 살림", "농부는 작물만 키우는 게 아니에요. 사탕수수 32개와 달걀 8개를 가져오세요.", "사탕수수 32개와 달걀 8개를 가져와라.",
    "농장이 제법 살림살이가 됐네요!", [D("minecraft:sugar_cane", 32), D("minecraft:egg", 8)], 300),
   ("수확제", "마지막이에요! 건초 더미 4개를 만들어 오세요. 마을 수확제에 쓸 거예요. 그러면 어엿한 농부예요.",
    "건초 더미 4개를 가져와라.", "풍성하네요! 이제 어엿한 농부예요. 직업 안내인에게 가서 농부를 고르세요.",
    [D("minecraft:hay_block", 4)], 500)]),
 ("fisher", "어부", "shop_fisher", "수산물 상인", 160,
  "물가에서 시간을 보내는 걸 좋아한다면 어부는 어떤가요? 수산물 상인을 찾아가 보세요.",
  [("첫 낚시", "낚시는 기다림이오. 아무 물고기나 10마리 낚아 오시오.", "물고기 10마리를 가져와라.",
    "오, 손맛을 봤구려.", [D("#minecraft:fishes", 10, "물고기")], 100),
   ("물때를 읽다", "물고기는 시간과 장소에 따라 달라지오. 이번엔 20마리를 낚아 오시오.", "물고기 20마리를 가져와라.",
    "꾸준하구려. 그게 어부의 덕목이오.", [D("#minecraft:fishes", 20, "물고기")], 150),
   ("낚시 도구", "어부는 도구를 아껴야 하오. 낚싯대 하나와 실 8개를 가져오시오. 여벌 낚싯줄을 준비해 둡시다.",
    "낚싯대 1개와 실 8개를 가져와라.", "이제 줄이 끊어져도 걱정 없겠소.", [D("minecraft:fishing_rod", 1), D("minecraft:string", 8)], 200),
   ("만선", "배가 가득 찰 만큼 낚아 오시오. 물고기 30마리!", "물고기 30마리를 가져와라.",
    "만선이구려!", [D("#minecraft:fishes", 30, "물고기")], 250),
   ("바다의 풀", "바다에는 물고기만 있는 게 아니오. 말린 켈프 16개를 가져오시오. 오래 바다에 나갈 때 든든하오.",
    "말린 켈프 16개를 가져와라.", "짭짤하니 좋구려.", [D("minecraft:dried_kelp", 16)], 300),
   ("월척", "마지막이오. 물고기 40마리를 낚아 오시오. 그러면 어엿한 어부요.",
    "물고기 40마리를 가져와라.", "대단하구려! 이제 어엿한 어부요. 직업 안내인에게 가서 어부를 고르시오.",
    [D("#minecraft:fishes", 40, "물고기")], 500)]),
 ("miner", "광부", "shop_blacksmith", "장비 상인", 170,
  "땅속 깊이 파고드는 광부가 되고 싶다면 장비 상인을 찾아가 보세요. 광물은 그분이 사 주거든요.",
  [("곡괭이질", "광부의 시작은 석탄과 철이지. 석탄 32개와 철 원석 16개를 캐 오게.", "석탄 32개와 철 원석 16개를 가져와라.",
    "좋아, 손에 물집 좀 잡혔겠군.", [D("minecraft:coal", 32), D("minecraft:raw_iron", 16)], 100),
   ("구리와 레드스톤", "구리 원석 16개와 레드스톤 가루 24개를 캐 오게.", "구리 원석 16개와 레드스톤 가루 24개를 가져와라.",
    "다양하게 캐는구먼.", [D("minecraft:raw_copper", 16), D("minecraft:redstone", 24)], 150),
   ("깊은 곳으로", "더 깊이 내려가 보게. 금 원석 8개를 가져오게. 용암 조심하고.", "금 원석 8개를 가져와라.",
    "제법 깊이 들어갔군.", [D("minecraft:raw_gold", 8)], 200),
   ("광부의 도구", "좋은 광부는 곡괭이를 여럿 챙기지. 철 곡괭이 하나와 청금석 16개를 가져오게.", "철 곡괭이 1개와 청금석 16개를 가져와라.",
    "준비성이 좋구먼.", [D("minecraft:iron_pickaxe", 1), D("minecraft:lapis_lazuli", 16)], 250),
   ("단단한 돌", "흑요석은 다이아몬드 곡괭이로만 캘 수 있지. 다이아몬드 1개와 흑요석 4개를 가져오게.",
    "다이아몬드 1개와 흑요석 4개를 가져와라.", "이제 웬만한 돌은 무섭지 않겠군.", [D("minecraft:diamond", 1), D("minecraft:obsidian", 4)], 300),
   ("빛나는 돌", "마지막일세. 다이아몬드 2개와 자수정 조각 8개를 가져오게.",
    "다이아몬드 2개와 자수정 조각 8개를 가져와라.", "다이아몬드라! 이제 자네는 광부일세. 직업 안내인에게 가서 광부를 고르게.",
    [D("minecraft:diamond", 2), D("minecraft:amethyst_shard", 8)], 500)]),
 ("cook", "요리사", "shop_food", "식품 상인", 180,
  "음식으로 사람을 행복하게 하고 싶다면 요리사는 어떤가요? 식품 상인을 찾아가 보세요. 재료는 거기서 다 살 수 있어요.",
  [("빵 굽기", "요리의 기본은 빵이죠. 빵 8개를 만들어 오세요. 밀은 저한테서 사도 돼요.", "빵 8개를 가져와라.",
    "고소한 냄새가 나네요.", [D("minecraft:bread", 8)], 100),
   ("고기 굽기", "고기는 잘 익혀야 해요. 스테이크 8개와 구운 돼지고기 8개를 만들어 오세요.", "스테이크 8개와 구운 돼지고기 8개를 가져와라.",
    "육즙이 살아 있네요.", [D("minecraft:cooked_beef", 8), D("minecraft:cooked_porkchop", 8)], 150),
   ("따뜻한 한 그릇", "이번엔 그릇 요리예요. 버섯 스튜 4그릇과 비트 수프 2그릇을 만들어 오세요.", "버섯 스튜 4개와 비트 수프 2개를 가져와라.",
    "든든하겠어요.", [D("minecraft:mushroom_stew", 4), D("minecraft:beetroot_soup", 2)], 200),
   ("달콤한 간식", "손님들이 간식을 찾아요. 쿠키 16개와 꿀병 2개를 준비해 오세요.", "쿠키 16개와 꿀병 2개를 가져와라.",
    "아이들이 좋아하겠어요.", [D("minecraft:cookie", 16), D("minecraft:honey_bottle", 2)], 250),
   ("가을 식탁", "호박 파이 4개와 구운 감자 16개를 만들어 오세요. 가을 잔치 준비예요.", "호박 파이 4개와 구운 감자 16개를 가져와라.",
    "향긋하네요.", [D("minecraft:pumpkin_pie", 4), D("minecraft:baked_potato", 16)], 300),
   ("잔칫상", "마지막은 잔칫상이에요. 케이크 2개와 황금 당근 4개를 만들어 오세요. 손님들이 기다리고 있어요.",
    "케이크 2개와 황금 당근 4개를 가져와라.", "훌륭한 솜씨예요! 이제 어엿한 요리사예요. 직업 안내인에게 가서 요리사를 고르세요.",
    [D("minecraft:cake", 2), D("minecraft:golden_carrot", 4)], 500)]),
]


# ---------------------------------------------------------------------------
# 모드 사용법 안내 (3.6.2)
#  - 단계 번호는 퀘스트 번호(2~7)
#  - tips: "[모드ID] 내용" 줄로 이야기 끝에 붙는다. 그 모드가 있을 때만 보이고, {key:...} 는 플레이어가 정한 키로 바뀐다.
#  - objs: 그 단계의 목표를 통째로 바꾼다 (mod 조건이 붙은 목표는 그 모드가 있을 때만 / !모드 는 없을 때만 쓰인다).
#  - add: 목표를 더한다.  rewards: 보상 아이템을 더한다.  text: (intro, progress) 를 바꾼다.
# ---------------------------------------------------------------------------
EF = "epicfight"
MODS = {
 ("swordsman", 2): dict(
    text=("검사는 자기 검을 스스로 마련할 줄 알아야 한다. 검 한 자루를 만들어 가져와라. 날이 잘 섰는지 내가 직접 보겠다.",
          "목표에 적힌 검을 만들어 가져와라."),
    objs=[D("epicfight:iron_tachi", 1, mod=EF), D("minecraft:iron_sword", 1, mod="!" + EF)],
    tips=[(EF, "철 태도는 작업대에서 철검 1개와 철 주괴 2개로 만든다. 장검·대검·창처럼 무기마다 쓰는 기술이 다르니 이것저것 쥐어 봐라.")]),
 ("swordsman", 3): dict(tips=[(EF, "{key:key.epicfight.switch_mode} 키로 전투 모드로 바꾸면, 공격을 이어서 누를 때 연속 베기가 나간다. 블록을 캘 때는 같은 키로 채광 모드로 돌아가라.")]),
 ("swordsman", 4): dict(tips=[(EF, "화살이 날아오면 {key:key.epicfight.dodge} 키로 몸을 굴려 피해라. 구르기에도 기력이 드니 아껴 써라.")]),
 ("swordsman", 5): dict(tips=[(EF, "{key:key.epicfight.skill_gui} 키를 누르면 기술 편집 창이 열린다. 스킬북을 손에 들고 쓰면 새 기술을 배우고, 이 창에서 끼워 쓸 수 있다.")]),
 ("swordsman", 6): dict(tips=[(EF, "무기마다 고유 기술이 있다. 적을 몇 번 베어 기술 게이지를 채운 뒤 {key:key.epicfight.weapon_innate_skill} 키로 써 봐라.")]),
 ("swordsman", 7): dict(tips=[(EF, "{key:key.epicfight.lock_on} 키로 눈앞의 적을 붙잡아 두면, 여럿과 싸울 때도 한 놈에게 집중할 수 있다.")]),

 ("warrior", 2): dict(
    text=("전사에게는 묵직한 도끼가 어울리지! 도끼 하나를 만들어 와라.", "목표에 적힌 도끼를 만들어 가져와라."),
    objs=[D("rogues:iron_double_axe", 1, mod="rogues"), D("minecraft:iron_axe", 1, mod="!rogues")],
    tips=[("rogues", "철 양날 도끼는 철 주괴 4개와 막대기 3개로 만든다. 전사에게 딱 맞는 묵직한 무기지!")]),
 ("warrior", 3): dict(tips=[(EF, "{key:key.epicfight.switch_mode} 키로 전투 모드로 바꾸고 공격을 이어서 눌러 봐라. 무거운 무기일수록 한 방이 묵직하다!")]),
 ("warrior", 4): dict(tips=[(EF, "{key:key.epicfight.guard} 키를 누르고 있으면 무기로 공격을 막는다. 막는 데도 기력이 드니 무작정 버티지는 마라!")]),
 ("warrior", 6): dict(tips=[(EF, "둘러싸이면 {key:key.epicfight.dodge} 키로 빠져나온 뒤 다시 몰아쳐라!")]),

 ("tank", 2): dict(
    text=("수호자의 시작은 방패다. 방패 하나를 만들어 와라.", "목표에 적힌 방패를 만들어 가져와라."),
    objs=[D("paladins:iron_kite_shield", 1, mod="paladins"), D("minecraft:shield", 1, mod="!paladins")],
    tips=[("paladins", "철 카이트 방패는 철 주괴 6개와 가죽 1개로 만든다. 성기사들이 쓰는 묵직한 방패다.")]),
 ("tank", 4): dict(tips=[(EF, "전투 모드에서는 {key:key.epicfight.guard} 키로 방패를 들어 막는다. 막을 때마다 기력이 줄어드니 틈을 보아 반격해라.")]),

 ("archer", 2): dict(
    text=("궁수에게 활은 몸의 일부야. 활 하나와 화살 32개를 준비해서 보여 줘.", "목표에 적힌 활과 화살 32개를 가져와."),
    objs=[D("archers:composite_longbow", 1, mod="archers"), D("minecraft:bow", 1, mod="!archers"), D("minecraft:arrow", 32)],
    tips=[("archers", "복합 장궁은 막대기 2개, 뼈 1개, 실 3개로 만들어. 시위를 끝까지 당기는 데 시간이 걸리지만 그만큼 멀리, 세게 날아가.")]),
 ("archer", 7): dict(rewards=[R("archers:archer_armor_head", 1, mod="archers")],
    tips=[("archers", "궁수 후드도 하나 줄게. 가죽 3개와 사슬 2개로 나머지 궁수 옷도 맞춰 입어 봐.")]),

 ("mage", 2): dict(tips=[("irons_spellbooks", "두루마리는 손에 들고 쓰면 마나 없이 주문이 한 번 나가고 사라진단다. 아껴 두었다가 주문서에 새기면 몇 번이고 쓸 수 있지.")]),
 ("mage", 4): dict(rewards=[R("irons_spellbooks:copper_spell_book", 1, mod="irons_spellbooks")],
    tips=[("irons_spellbooks", "끝내면 가장 쉬운 주문서를 주마. 장신구의 주문서 칸에 끼우고 {key:key.irons_spellbooks.spell_wheel} 키로 주문을 고른 뒤 {key:key.irons_spellbooks.spellbook_cast} 키로 시전하렴.")]),
 ("mage", 5): dict(tips=[("irons_spellbooks", "각인 테이블은 책 1권, 나무 반 블록 3개, 나무 울타리 2개로 만든단다. 주문서와 두루마리를 올려 두루마리의 주문을 주문서에 새기렴.")]),
 ("mage", 6): dict(tips=[("wizards", "석탄 1개와 막대기 1개로 초심자 완드를 만들 수 있단다. 주문 부여대에서 주문서를 만들고 주문을 채운 뒤, 완드나 지팡이를 들고 쓰렴.")]),
 ("mage", 7): dict(rewards=[R("wizards:wand_novice", 1, mod="wizards")]),

 ("farmer", 6): dict(tips=[("kaleidoscope_cookery", "밭 옆에 허수아비를 세우면 주변 농지를 지켜 줘요. 밀짚모자, 짚 묶음, 호박, 막대기로 만들 수 있어요.")]),

 ("fisher", 2): dict(tips=[("starcatcher", "보통 낚싯대로는 흔한 물고기만 낚이오. 스타캐쳐의 낚싯대로 찌가 물리면 원판이 뜨는데, 돌아가는 바늘이 초록 칸(스위트스팟)에 겹칠 때 {key:key.starcatcher.minigame_hit} 키를 눌러 줄을 감으시오.")]),
 ("fisher", 3): dict(rewards=[R("starcatcher:starcatcher_guide", 1, mod="starcatcher")],
    tips=[("starcatcher", "끝내면 낚시 안내서를 주겠소. 손에 들고 열거나 {key:key.starcatcher.open_guide} 키로 열면, 물고기마다 잡히는 곳·시간·날씨가 적혀 있소.")]),
 ("fisher", 4): dict(
    text=("어부는 도구를 아껴야 하오. 목표에 적힌 낚시 도구와 실 8개를 가져오시오.", "목표에 적힌 낚시 도구와 실 8개를 가져와라."),
    objs=[D("starcatcher:hook", 1, mod="starcatcher"), D("starcatcher:bobber", 1, mod="starcatcher"),
          D("minecraft:fishing_rod", 1, mod="!starcatcher"), D("minecraft:string", 8)],
    rewards=[R("starcatcher:starcatcher_rod", 1, mod="starcatcher")],
    tips=[("starcatcher", "철 낚싯바늘은 철 주괴 3개와 철 조각 1개, 낚시찌는 나무판자 4개·막대기 2개·양털 1개로 만드오. 가져오면 스타캐쳐의 낚싯대로 만들어 주겠소.")]),
 ("fisher", 7): dict(add=[D("#starcatcher:rare_fishes", 1, "희귀 물고기", mod="starcatcher")],
    tips=[("starcatcher", "마지막으로 희귀한 물고기도 한 마리 보여 주시오. 낚싯바늘·낚시찌·미끼를 바꿔 끼우면 까다로운 물고기도 훨씬 수월하오.")]),

 ("miner", 2): dict(tips=[(EF, "전투 모드에서는 곡괭이질이 불편하다네. {key:key.epicfight.switch_mode} 키로 채광 모드로 바꾸고 캐게.")]),

 ("cook", 3): dict(rewards=[R("kaleidoscope_cookery:iron_kitchen_knife", 1, mod="kaleidoscope_cookery")],
    tips=[("kaleidoscope_cookery", "조약돌 8개와 모닥불로 난로를, 철 주괴 7개로 프라이팬을 만들어 난로 위에 올리세요. 난로는 부싯돌과 부시로 불을 붙여요."),
          ("kaleidoscope_cookery", "팬에 기름을 두르고 재료를 넣은 뒤 뒤집개(철 주괴·철 조각·막대기)로 몇 번 볶아 그릇에 담으면 요리가 돼요. 기름은 중식도로 돼지를 잡으면 얻을 수 있으니, 끝내면 중식도를 하나 드릴게요.")]),
 ("cook", 4): dict(add=[D("farmersdelight:beef_stew", 2, mod="farmersdelight")],
    tips=[("farmersdelight", "조리용 냄비를 불 위(난로나 모닥불)에 올리고 재료를 넣으면 요리가 끓어요. 소고기 스튜는 익히지 않은 소고기, 당근, 감자로 만들어요."),
          ("handson_delight", "냄비는 따로 창이 뜨지 않고 그 자리에서 열려요. 재료를 직접 넣고 저어서 끓이세요.")]),
 ("cook", 5): dict(rewards=[R("farmersdelight:iron_knife", 1, mod="farmersdelight")],
    tips=[("farmersdelight", "도마(막대기 2개·나무판자 4개)에 재료를 올리고 식칼로 손질하면 썬 재료가 나와요. 끝내면 철 식칼을 하나 드릴게요."),
          ("handson_delight", "도마 위에 그어진 선을 따라 칼을 든 채 직접 썰어야 해요.")]),
 ("cook", 6): dict(add=[D("kaleidoscope_cookery:pork_bone_soup", 2, mod="kaleidoscope_cookery")],
    tips=[("kaleidoscope_cookery", "냄비는 벽돌 2개와 철 주괴 5개로, 뚜껑은 벽돌 1개와 철 주괴 3개로 만들어요. 불 위에 올리고 물을 부은 뒤 재료를 넣고 뚜껑을 덮으면 끓기 시작해요. 뼈만 넣어도 사골국이 돼요.")]),
}

# 3.64: 말투 맞춤. 진행 글은 "…가져와라 / …처치하라"로 적어 두고, 스승의 말투에 맞게 끝말을 바꾼다 (레온 · 브론 · 가렌은 그대로).
#   한나 · 마르타 해요체 / 오스카 하오체 / 브람 하게체 / 리아 반말 / 엘리스 "~하렴"
VOICE = {"farmer": ("가져오세요.", "처치하세요."), "cook": ("가져오세요.", "처치하세요."), "fisher": ("가져오시오.", "처치하시오."),
         "miner": ("가져오게.", "처치하게."), "archer": ("가져와.", "처치해 와."), "mage": ("가져오렴.", "처치하렴.")}
def voice(jid, prog):
    v = VOICE.get(jid)
    if not v: return prog
    return prog.replace("가져와라.", v[0]).replace("처치하라.", v[1])
def eul(name):
    """받침이 있으면 '을', 없으면 '를'."""
    return "을" if (ord(name[-1]) - 0xAC00) % 28 else "를"

quests = []
for jid, jname, mentor, mname, order, intro1, steps in jobs:
    chain = jname + " 선행 퀘스트"
    total = len(steps) + 1
    first = q(f"job_{jid}_1", f"{chain} (1/{total})", order, f"{jname}의 길", "job",
              intro1, f"{mname}에게 말을 걸어 보세요.", f"{mname}{eul(mname)} 만났군요. 이제 그분의 시험을 받아 보세요.",
              [T(mentor)], 100, turnIn="job")
    quests.append(first)
    prev = first["id"]
    for i, step in enumerate(steps):
        title, intro, prog, done, objs, gold = step[:6]
        items = step[6] if len(step) > 6 else None
        m = MODS.get((jid, i + 2), {})
        if "text" in m:
            intro, prog = m["text"]
        prog = voice(jid, prog)
        if "objs" in m:
            objs = m["objs"]
        objs = list(objs) + m.get("add", [])
        if m.get("rewards"):
            items = (items or []) + m["rewards"]
        for mod, tip in m.get("tips", []):
            intro += "\n[" + mod + "] " + tip
            prog += "\n[" + mod + "] " + tip
        last = i == len(steps) - 1
        qq = q(f"job_{jid}_{i+2}", f"{chain} ({i+2}/{total})", order + i + 1, title, mentor, intro, prog, done, objs, gold,
               requires=[prev], flags=[f"unlock_job_{jid}"] if last else None, items=items)
        quests.append(qq)
        prev = qq["id"]

doc = {"_help": "직업별 선행 퀘스트 (직업마다 7단계). 1단계는 직업 안내인(job)에게서 받고, 2~7단계는 각 직업의 스승에게서 받습니다. 이야기 속 \"[모드ID] ...\" 줄은 그 모드가 있을 때만 보이는 도움말이고, {key:키 이름} 은 플레이어가 정한 키로 바뀝니다. 목표·보상의 mod 는 그 모드가 있을 때만(!모드ID 는 없을 때만) 쓰입니다. "
                "마지막 단계의 보상 표식(unlock_job_<직업>)을 얻어야 그 직업을 고를 수 있습니다. 게임 안 퀘스트 편집기(/rtshop quest edit)로 고칠 수 있고, "
                "이 파일은 한 번 설치된 뒤에는 모드 업데이트로 덮어쓰지 않습니다. 전투 직업 스승은 /rtshop npc quest_sword_master 처럼 소환합니다.",
       "npcs": npcs, "quests": quests}
json.dump(doc, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(len(quests), "quests")
