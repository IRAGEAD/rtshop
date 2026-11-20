"""RT Shop 기본 상점 파일 생성기.
가격표를 한곳에서 관리하고, 6개의 상점 JSON 과 로컬 검증용 base.json 을 만든다.
(이 스크립트는 개발용이며 모드에 포함되지 않는다.)

가격 원칙
 1. 원재료(만들 수 없는 것): 가격표 값에 팔고, 그 50% 에 매입한다.
 2. 만들 수 있는 것(음식·장비·가공품): 재료를 가격표대로 샀을 때의 원가만큼 매입한다 -> 재료를 사서 만들어 팔면 본전 이상.
 3. 요리 재료로 쓰이는 가공품(설탕, 기름, 유리병 등)은 원가 이하로 판다 -> 사서 써도 손해가 없다.
 4. 손이 가지 않고 무한히 쌓이는 것(사탕수수, 대나무, 켈프, 몹 드롭)은 값을 아주 낮게 잡는다.
"""
import json, sys, os

OUT = sys.argv[1]          # .../rtshop_defaults
# (선택) 가격표 요약 JSON 을 쓸 경로: 레시피 원가를 게임 밖에서 계산해 볼 때 쓰는 입력 자료
HARNESS = sys.argv[2] if len(sys.argv) > 2 else None

HELP = [
    "파일 이름이 상점 ID 입니다 (예: farmer.json -> /rtshop npc farmer). 가격 단위는 Gold(G).",
    "가격은 전 상점 공통입니다. 같은 아이템이 여러 상점에 있으면 먼저 읽힌 값 하나만 쓰입니다 (상점 사이의 차익 거래 방지).",
    "items: 가격을 직접 정한 품목. item, price(상점 판매가), category 외에 아래 옵션을 쓸 수 있습니다.",
    "  sellPrice: 매입가를 직접 지정 (없으면 price 의 50%) · buy/sell: false 면 이 상점에서 팔지 않음/사지 않음",
    "  seed: true 면 '심을 수 있는 상점 씨앗'으로 판매 · fish: true 면 물고기(무게 보정) · dynamic: true 면 레시피 원가로 가격 결정",
    "auto: 태그·모드·종류로 아이템을 모으는 규칙. match{mod(s), tag(s), notTag(s), class(es), recipeType(s)}",
    "  class: weapon|ranged|armor|shield|tool|hoe|fishing_rod|food|gear · formula: fixed|gear|food|auto · multiplier: 공식 가격에 곱하는 값",
    "만들 수 있는 물건은 여기 적힌 값과 상관없이 '재료 원가(+조리 수고비)'에 매입하고, 판매가는 그 2배 이상이 됩니다 (config/rtshop/pricing.json).",
    "수정 후 /rtshop reload · 실제 적용된 가격과 점검 결과는 /rtshop export (config/rtshop/export)"
]

# ----------------------------------------------------------------------------- 가격표
P = {}        # item id -> dict(price, sellPrice?)  (전 상점 공통)


def price(item, p, sell=None):
    assert item not in P, item
    P[item] = {'price': p}
    if sell is not None:
        P[item]['sellPrice'] = sell


def E(item, cat, **kw):
    """상점 항목. 가격은 공통 가격표에서 가져온다."""
    d = {'item': item, 'price': P[item]['price'], 'category': cat}
    if 'sellPrice' in P[item]:
        d['sellPrice'] = P[item]['sellPrice']
    d.update(kw)
    return d


def S(item, p, cat='seeds'):
    """상점 씨앗 (심기 전용)."""
    return {'item': item, 'price': p, 'category': cat, 'seed': True}


# --- 작물: 한 포기에서 1개 나오는 작물 40G(씨앗 20G), 3~4개 나오는 작물 20G(심는 것 50G) -> 포기당 약 20G 이익
for it in ['minecraft:wheat', 'minecraft:beetroot', 'farmersdelight:cabbage', 'farmersdelight:tomato',
           'kaleidoscope_cookery:tomato', 'kaleidoscope_cookery:red_chili', 'kaleidoscope_cookery:green_chili',
           'kaleidoscope_cookery:lettuce', 'farm_and_charm:barley', 'farm_and_charm:oat', 'farm_and_charm:corn',
           'farm_and_charm:strawberry', 'farm_and_charm:tomato', 'farm_and_charm:lettuce', 'farm_and_charm:onion']:
    price(it, 40)
for it in ['minecraft:carrot', 'minecraft:potato', 'farmersdelight:onion', 'minecraft:cocoa_beans', 'kaleidoscope_cookery:wild_rice']:
    price(it, 20)
price('minecraft:nether_wart', 30)
# 쌀: 칼레이도스코프 벼 한 포기 = 이삭 3개 = 쌀 9톨. 파머스 딜라이트 쌀과 같은 태그라서 같은 값으로 맞춘다
price('kaleidoscope_cookery:rice', 4)
price('kaleidoscope_cookery:rice_panicle', 12)
price('farmersdelight:rice', 4)
price('farmersdelight:rice_panicle', 8)
price('farmersdelight:straw', 4)
price('farmersdelight:cabbage_leaf', 20)      # 양배추 1개를 썰면 2장
price('minecraft:brown_mushroom', 8)
price('minecraft:red_mushroom', 8)
price('minecraft:apple', 50)
price('minecraft:chorus_fruit', 6)
# 한 번 심으면 계속 열리는 작물 (씨앗값이 들지 않으므로 싸다)
price('minecraft:sweet_berries', 6)
price('minecraft:glow_berries', 8)
price('kaleidoscope_cookery:fresh_tea_leaves', 10)
price('kaleidoscope_tavern:grape', 20)
price('kaleidoscope_tavern:green_grape', 24)
price('kaleidoscope_tavern:ice_grape', 60)
price('kaleidoscope_tavern:gold_grape', 120)
# 자동 농장으로 무한 생산이 가능한 작물: 값이 아주 낮다
price('minecraft:melon_slice', 1)
price('minecraft:melon', 9)
price('minecraft:pumpkin', 4)
price('minecraft:sugar_cane', 2)
price('minecraft:cactus', 2)
# 대나무·켈프는 자동 농장에서 시간당 수만 개가 나오므로 매입하지 않는다 (재료로 사서 쓸 수만 있음)
price('minecraft:bamboo', 1)
price('minecraft:kelp', 1)
CROPS = ['minecraft:wheat', 'minecraft:beetroot', 'minecraft:carrot', 'minecraft:potato', 'minecraft:nether_wart',
         'minecraft:cocoa_beans', 'minecraft:brown_mushroom', 'minecraft:red_mushroom', 'minecraft:apple',
         'minecraft:sweet_berries', 'minecraft:glow_berries', 'minecraft:chorus_fruit',
         'minecraft:melon_slice', 'minecraft:melon', 'minecraft:pumpkin', 'minecraft:sugar_cane',
         'minecraft:cactus',
         'farmersdelight:cabbage', 'farmersdelight:cabbage_leaf', 'farmersdelight:tomato', 'farmersdelight:onion', 'farmersdelight:rice',
         'kaleidoscope_cookery:tomato', 'kaleidoscope_cookery:red_chili', 'kaleidoscope_cookery:green_chili',
         'kaleidoscope_cookery:lettuce', 'kaleidoscope_cookery:rice', 'kaleidoscope_cookery:wild_rice',
         'kaleidoscope_cookery:fresh_tea_leaves',
         'farm_and_charm:barley', 'farm_and_charm:oat', 'farm_and_charm:corn', 'farm_and_charm:strawberry',
         'farm_and_charm:tomato', 'farm_and_charm:lettuce', 'farm_and_charm:onion',
         'kaleidoscope_tavern:grape', 'kaleidoscope_tavern:green_grape', 'kaleidoscope_tavern:ice_grape',
         'kaleidoscope_tavern:gold_grape']
CROPS_SELL_ONLY = ['farmersdelight:rice_panicle', 'kaleidoscope_cookery:rice_panicle']
CROPS_BUY_ONLY = ['minecraft:bamboo', 'minecraft:kelp']

# --- 축산물·육류·수산물
price('minecraft:egg', 6)
# 양동이는 철 주괴 3개 값(450G)에 매입한다. 내용물이 든 양동이는 "양동이 값 + 내용물 값"이라
# 쓰고 난 빈 양동이를 되팔면 내용물 값만 치르는 셈이 된다 (우유 2G, 물 1G, 용암 150G)
price('minecraft:bucket', 900, 450)
price('minecraft:milk_bucket', 452, 451)
price('minecraft:water_bucket', 451, 450)
price('minecraft:lava_bucket', 600, 525)
price('minecraft:honey_bottle', 16)
price('minecraft:honeycomb', 6)
price('minecraft:leather', 1)                 # 썩은 살점을 말리면 가죽이 되므로 싸다
price('minecraft:rabbit_hide', 20)
price('minecraft:rabbit_foot', 200)
price('minecraft:feather', 4)
price('minecraft:white_wool', 3)
price('minecraft:beef', 80)
price('minecraft:porkchop', 50)
price('farmersdelight:ham', 105)              # 칼로 잡은 돼지에서 나온다. 썰면 돼지고기 2개 + 뼈
price('minecraft:mutton', 80)
price('minecraft:rabbit', 80)
price('minecraft:chicken', 40)
price('minecraft:cod', 120)
price('minecraft:salmon', 120)
price('minecraft:pufferfish', 220)
price('minecraft:tropical_fish', 400)
FISH_TAGS = [('starcatcher:legendary_fishes', 1300), ('starcatcher:epic_fishes', 750), ('starcatcher:rare_fishes', 400),
             ('starcatcher:uncommon_fishes', 220), ('starcatcher:common_fishes', 120)]

# --- 기본 재료 (요리사가 사서 쓰는 것: 원가 이하로 판다)
price('minecraft:sugar', 2)
price('minecraft:bowl', 1)
price('minecraft:glass_bottle', 1)
price('minecraft:dried_kelp', 1, 0.1)     # 켈프 자동 농장 + 화로로 무한히 나온다
price('minecraft:ice', 2)
price('minecraft:snowball', 1)
price('minecraft:bone', 5)
price('minecraft:bone_meal', 1)
price('kaleidoscope_cookery:oil', 1)
price('minecraft:paper', 2)
price('minecraft:stick', 1)
price('minecraft:lily_pad', 4)

# --- 광물. 주괴와 원석은 같은 값 (화로에 굽는 것만으로 돈이 불어나지 않게)
price('minecraft:coal', 50)
price('minecraft:charcoal', 20)
price('minecraft:raw_copper', 30)
price('minecraft:copper_ingot', 30)
price('minecraft:raw_iron', 150)
price('minecraft:iron_ingot', 150)
price('minecraft:raw_gold', 300)
price('minecraft:gold_ingot', 300)
price('minecraft:redstone', 40)
price('minecraft:lapis_lazuli', 60)
price('minecraft:quartz', 20)
price('minecraft:amethyst_shard', 6)      # 자수정 정동에서 계속 자란다
price('minecraft:emerald', 60, 3)         # 주민 거래로 무한히 얻을 수 있어 매입가는 거의 없다
price('minecraft:diamond', 1000)
price('minecraft:netherite_scrap', 3000)
price('minecraft:netherite_ingot', 13200)
price('minecraft:netherite_upgrade_smithing_template', 7000)   # 다이아몬드 7개로 복제할 수 있다
price('minecraft:flint', 10, 1)           # 자갈을 손질하면 공짜로 계속 나온다
price('minecraft:gunpowder', 20, 2)
price('minecraft:obsidian', 20)
price('minecraft:glowstone_dust', 20)
# 원석 블록(섬세한 손길): 맷돌에 갈면 나오는 개수만큼의 값
ORES = {'coal_ore': 150, 'deepslate_coal_ore': 150, 'copper_ore': 180, 'deepslate_copper_ore': 180, 'iron_ore': 450,
        'deepslate_iron_ore': 450, 'gold_ore': 900, 'deepslate_gold_ore': 900, 'nether_gold_ore': 300, 'redstone_ore': 280,
        'deepslate_redstone_ore': 280, 'lapis_ore': 420, 'deepslate_lapis_ore': 420, 'diamond_ore': 2000,
        'deepslate_diamond_ore': 2000, 'nether_quartz_ore': 60, 'ancient_debris': 3000}
for k, v in ORES.items():
    price('minecraft:' + k, v)
price('minecraft:emerald_ore', 800, 400)
price('minecraft:deepslate_emerald_ore', 800, 400)

# --- 잡화
LOGS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry']
for w in LOGS:
    price(f'minecraft:{w}_log', 20)
GENERAL = {'torch': 30, 'lantern': 300, 'glowstone': 80, 'book': 30, 'compass': 1300, 'clock': 2500, 'spyglass': 150,
           'name_tag': 1000, 'lead': 20, 'shears': 600,
           'sea_pickle': 4, 'ink_sac': 4, 'glow_ink_sac': 10, 'scute': 60, 'sponge': 400,
           'heart_of_the_sea': 10000, 'spectral_arrow': 50, 'firework_rocket': 5}
for k, v in GENERAL.items():
    price('minecraft:' + k, v)
# 나무 가공품과 건축 자재: 대나무 자동 농장이나 맨손 채집으로 무한히 만들 수 있어서 팔기만 하고 매입하지 않는다
BUY_ONLY = {'ladder': 5, 'chest': 40, 'crafting_table': 20, 'furnace': 10, 'bookshelf': 150, 'white_bed': 80, 'composter': 20,
            'oak_boat': 30, 'glass': 2, 'bricks': 10, 'stone_bricks': 2, 'cobblestone': 1, 'stone': 2, 'sand': 1, 'gravel': 1,
            'dirt': 1, 'clay_ball': 1, 'seagrass': 1}
for k, v in BUY_ONLY.items():
    price('minecraft:' + k, v)
price('farmersdelight:tree_bark', 1)
price('minecraft:potion', 1)
price('minecraft:netherrack', 1)
price('minecraft:carved_pumpkin', 4)
# 물고기 양동이 = 양동이 + 물 + 물고기 (국물 재료로 쓰일 때의 값)
price('minecraft:cod_bucket', 571)
price('minecraft:salmon_bucket', 571)
price('minecraft:pufferfish_bucket', 671)
price('minecraft:tropical_fish_bucket', 851)
price('minecraft:saddle', 1500, 150)          # 습격·던전에서 쌓이는 물건
price('minecraft:nautilus_shell', 1000, 100)
# 몹 농장으로 쉽게 쌓이는 것들은 값을 낮게 잡는다
MOB = {'string': 1, 'slime_ball': 4, 'blaze_rod': 100, 'spider_eye': 2, 'rotten_flesh': 1, 'arrow': 5,
       'prismarine_shard': 4, 'prismarine_crystals': 8, 'feather_dummy': 0}
del MOB['feather_dummy']
for k, p in MOB.items():
    price('minecraft:' + k, p)
price('minecraft:ender_pearl', 20, 1)         # 엔더맨 농장에서 시간당 수천 개
price('minecraft:experience_bottle', 300, 20)

# --- 스타캐처
price('starcatcher:pearl', 600)
price('starcatcher:worm', 20)

COOK_MODS = ['farmersdelight', 'kaleidoscope_cookery', 'kaleidoscope_tavern', 'farm_and_charm', 'handson_delight']
COOK_TYPES = ['kaleidoscope_cookery:pot', 'kaleidoscope_cookery:flex_pot', 'kaleidoscope_cookery:stockpot',
              'kaleidoscope_cookery:flex_stockpot', 'kaleidoscope_cookery:steamer', 'kaleidoscope_cookery:teapot',
              'kaleidoscope_cookery:rice_bowl', 'farmersdelight:cooking',
              'farm_and_charm:pot_cooking', 'farm_and_charm:roaster', 'farm_and_charm:stove', 'farm_and_charm:crafting_bowl']
DRINK_TYPES = ['kaleidoscope_tavern:barrel', 'kaleidoscope_tavern:shaker']
MARGINS = {'kaleidoscope_cookery:pot': 0.05, 'kaleidoscope_cookery:flex_pot': 0.05, 'kaleidoscope_cookery:stockpot': 0.05,
           'kaleidoscope_cookery:flex_stockpot': 0.05, 'kaleidoscope_cookery:steamer': 0.05, 'kaleidoscope_cookery:teapot': 0.05,
           'kaleidoscope_tavern:barrel': 0.15, 'kaleidoscope_tavern:shaker': 0.05}
LABOR = {'kaleidoscope_cookery:pot': 80, 'kaleidoscope_cookery:flex_pot': 80, 'kaleidoscope_cookery:stockpot': 60,
         'kaleidoscope_cookery:flex_stockpot': 60, 'kaleidoscope_cookery:steamer': 10, 'kaleidoscope_cookery:teapot': 20,
         'kaleidoscope_tavern:barrel': 50, 'kaleidoscope_tavern:shaker': 40}

# ----------------------------------------------------------------------------- 농작물 상점
farmer_items = [E(i, 'crops', looseSell=True) for i in CROPS]
farmer_items += [E(i, 'crops', looseSell=True, buy=False) for i in CROPS_SELL_ONLY]
farmer_items += [E(i, 'crops', sell=False) for i in CROPS_BUY_ONLY]
farmer_items += [
    S('minecraft:wheat_seeds', 20), S('minecraft:beetroot_seeds', 20), S('minecraft:carrot', 50), S('minecraft:potato', 50),
    S('minecraft:nether_wart', 60), S('minecraft:cocoa_beans', 40), S('minecraft:sweet_berries', 300),
    S('minecraft:glow_berries', 300), S('minecraft:melon_seeds', 600), S('minecraft:pumpkin_seeds', 600),
    S('minecraft:torchflower_seeds', 250), S('minecraft:pitcher_pod', 250),
    S('farmersdelight:cabbage_seeds', 20), S('farmersdelight:tomato_seeds', 20), S('farmersdelight:onion', 50),
    S('farmersdelight:rice', 4),
    S('kaleidoscope_cookery:tomato_seed', 20), S('kaleidoscope_cookery:chili_seed', 20), S('kaleidoscope_cookery:lettuce_seed', 20),
    S('kaleidoscope_cookery:wild_rice', 30), S('kaleidoscope_cookery:rice', 30), S('kaleidoscope_cookery:tea_seed', 400),
    S('farm_and_charm:barley_seeds', 20), S('farm_and_charm:oat_seeds', 20), S('farm_and_charm:kernels', 20),
    S('farm_and_charm:strawberry_seeds', 20), S('farm_and_charm:tomato_seeds', 20), S('farm_and_charm:lettuce_seeds', 40),
    S('farm_and_charm:onion', 40),
]
for w in ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'cherry']:
    farmer_items.append({'item': f'minecraft:{w}_sapling', 'price': 20, 'category': 'seeds'})
farmer_items += [
    {'item': 'kaleidoscope_tavern:grapevine', 'price': 600, 'category': 'seeds'},
    E('minecraft:shears', 'tools'), E('minecraft:water_bucket', 'tools'),
    E('minecraft:lead', 'tools'), E('minecraft:composter', 'tools', sell=False),
    {'item': 'kaleidoscope_cookery:sickle', 'price': 1500, 'category': 'tools', 'dynamic': True},
    {'item': 'better_farming_plus:sprinkler', 'price': 1200, 'category': 'tools', 'dynamic': True},
    E('minecraft:bone_meal', 'supplies'), E('minecraft:dirt', 'supplies', sell=False),
    E('farmersdelight:straw', 'supplies'),
    {'item': 'farmersdelight:rope', 'price': 10, 'category': 'supplies'},
    {'item': 'farmersdelight:canvas', 'price': 40, 'category': 'supplies'},
    {'item': 'farmersdelight:organic_compost', 'price': 60, 'category': 'supplies'},
    {'item': 'farmersdelight:rich_soil', 'price': 120, 'category': 'supplies'},
    {'item': 'farm_and_charm:fertilizer', 'price': 4, 'category': 'supplies'},
    E('minecraft:egg', 'animal'), E('minecraft:milk_bucket', 'animal'), E('minecraft:honey_bottle', 'animal'),
    E('minecraft:honeycomb', 'animal'), E('minecraft:leather', 'animal'), E('minecraft:rabbit_hide', 'animal'),
    E('minecraft:rabbit_foot', 'animal'), E('minecraft:feather', 'animal'), E('minecraft:white_wool', 'animal'),
    E('minecraft:beef', 'animal'), E('minecraft:porkchop', 'animal'), E('minecraft:mutton', 'animal'),
    E('minecraft:rabbit', 'animal'), E('minecraft:chicken', 'animal'), E('farmersdelight:ham', 'animal', buy=False),
    E('minecraft:bucket', 'animal', buy=False),
]
farmer = {
    '_설명': HELP + [
        "씨앗은 이 상점에서 산 것만 심을 수 있고, 작물을 수확해도 씨앗은 나오지 않습니다.",
        "당근·감자처럼 작물이 곧 씨앗인 것은 seed: true 인 '심기 전용' 품목을 따로 팝니다 (수확물은 심을 수 없음).",
        "직접 수확한 작물은 별 등급(1~3별)에 따라 가격표의 90% / 110% / 140% 에 매입합니다. 별이 없는 작물은 50%.",
        "사탕수수·선인장·수박·호박처럼 자동 농장으로 무한히 쌓이는 작물은 값을 아주 낮게 잡았고, 대나무·켈프는 매입하지 않습니다.",
    ],
    'title': '농작물 상점', 'npcName': '농작물 상인', 'profession': 'farmer',
    'categories': [
        {'id': 'crops', 'name': '농작물', 'icon': 'minecraft:wheat'},
        {'id': 'seeds', 'name': '씨앗·묘목', 'icon': 'minecraft:wheat_seeds'},
        {'id': 'tools', 'name': '농기구', 'icon': 'minecraft:iron_hoe'},
        {'id': 'supplies', 'name': '농사 용품', 'icon': 'minecraft:bone_meal'},
        {'id': 'animal', 'name': '축산물', 'icon': 'minecraft:egg'},
    ],
    'items': farmer_items,
    'auto': [
        {'match': {'class': 'hoe'}, 'category': 'tools', 'formula': 'gear', 'multiplier': 5},
        {'match': {'tags': ['forge:seeds']}, 'category': 'seeds', 'price': 20},
        {'match': {'tags': ['forge:mushrooms']}, 'category': 'crops', 'price': 8, 'looseSell': True},
        {'match': {'tags': ['forge:crops', 'forge:vegetables', 'forge:grain']}, 'category': 'crops', 'price': 40, 'looseSell': True},
        {'match': {'tags': ['forge:fruits', 'forge:berries']}, 'category': 'crops', 'price': 40, 'looseSell': True},
    ],
    'exclude': ['minecraft:hay_block', 'tacz:*', 'kubejs:*'],
}

# ----------------------------------------------------------------------------- 식품 상점
food_items = [E(i, 'ingredient', looseSell=True) for i in CROPS]
food_items += [E(i, 'ingredient', looseSell=True, buy=False) for i in CROPS_SELL_ONLY]
food_items += [E(i, 'ingredient', sell=False) for i in CROPS_BUY_ONLY]
food_items += [
    E('minecraft:beef', 'meat'), E('minecraft:porkchop', 'meat'), E('minecraft:mutton', 'meat'), E('minecraft:rabbit', 'meat'),
    E('minecraft:chicken', 'meat'), E('farmersdelight:ham', 'meat', buy=False), E('minecraft:cod', 'meat', fish=True), E('minecraft:salmon', 'meat', fish=True),
    E('minecraft:pufferfish', 'meat', fish=True), E('minecraft:tropical_fish', 'meat', fish=True),
    E('minecraft:egg', 'base'), E('minecraft:milk_bucket', 'base'), E('minecraft:water_bucket', 'base'),
    E('minecraft:honey_bottle', 'base'), E('minecraft:sugar', 'base'), E('kaleidoscope_cookery:oil', 'base'),
    E('minecraft:dried_kelp', 'base'), E('minecraft:bowl', 'base', sell=False), E('minecraft:glass_bottle', 'base', sell=False),
    E('minecraft:stick', 'base', sell=False), E('minecraft:paper', 'base'), E('minecraft:lily_pad', 'base'),
    E('minecraft:ice', 'base', sell=False), E('minecraft:snowball', 'base', sell=False), E('minecraft:bone', 'base'),
    E('minecraft:bone_meal', 'base'), E('minecraft:coal', 'base'), E('minecraft:charcoal', 'base'),
    E('minecraft:bucket', 'base', buy=False),
    {'item': 'minecraft:cake', 'price': 600, 'category': 'dish', 'dynamic': True},
]
TOOLS = ['kaleidoscope_cookery:pot', 'kaleidoscope_cookery:stockpot', 'kaleidoscope_cookery:stockpot_lid',
         'kaleidoscope_cookery:steamer', 'kaleidoscope_cookery:teapot', 'kaleidoscope_cookery:millstone',
         'kaleidoscope_cookery:chopping_board', 'kaleidoscope_cookery:kitchen_shovel', 'kaleidoscope_cookery:iron_kitchen_knife',
         'kaleidoscope_cookery:gold_kitchen_knife', 'kaleidoscope_cookery:diamond_kitchen_knife', 'kaleidoscope_cookery:stove',
         'kaleidoscope_cookery:oil_pot', 'kaleidoscope_cookery:bamboo_tray', 'kaleidoscope_cookery:enamel_basin',
         'kaleidoscope_cookery:shawarma_spit', 'kaleidoscope_cookery:kitchenware_racks',
         'kaleidoscope_tavern:barrel', 'kaleidoscope_tavern:pressing_tub', 'kaleidoscope_tavern:shaker', 'kaleidoscope_tavern:tap',
         'kaleidoscope_tavern:empty_bottle', 'kaleidoscope_tavern:empty_glassware', 'kaleidoscope_tavern:trellis',
         'farmersdelight:cooking_pot', 'farmersdelight:skillet', 'farmersdelight:stove', 'farmersdelight:cutting_board',
         'farmersdelight:flint_knife', 'farmersdelight:iron_knife', 'farmersdelight:golden_knife', 'farmersdelight:diamond_knife',
         'farm_and_charm:cooking_pot', 'farm_and_charm:roaster', 'farm_and_charm:stove', 'farm_and_charm:mincer',
         'farm_and_charm:crafting_bowl']
food_items += [{'item': i, 'price': 1000, 'category': 'tool', 'dynamic': True} for i in TOOLS]
food = {
    '_설명': HELP + [
        "요리사를 위한 상점입니다. 식재료를 사서 조리한 음식을 여기에 팔 수 있습니다.",
        "조리한 음식의 매입가 = 재료를 이 가격표대로 샀을 때의 원가 × (1 + 마진) + 수고비. 그래서 재료를 사서 만들어 팔아도 적자가 나지 않습니다.",
        "여러 재료 중 하나를 고를 수 있는 조리법(예: '아무 채소')은 가장 싼 재료 기준으로 값을 매깁니다.",
        "우유·물 양동이는 '양동이 값 + 내용물 값'입니다. 다 쓴 빈 양동이를 되팔면 양동이 값(450G)을 그대로 돌려받습니다.",
        "화로 연료비는 음식값에 들어가지 않습니다. 모닥불이나 스토브처럼 연료가 들지 않는 조리 도구를 쓰면 손해가 없습니다.",
    ],
    'title': '식품 상점', 'npcName': '식품 상인', 'profession': 'butcher',
    'categories': [
        {'id': 'ingredient', 'name': '농산물', 'icon': 'minecraft:carrot'},
        {'id': 'meat', 'name': '육류·수산물', 'icon': 'minecraft:beef'},
        {'id': 'base', 'name': '기본 재료', 'icon': 'minecraft:sugar'},
        {'id': 'dish', 'name': '요리', 'icon': 'minecraft:rabbit_stew'},
        {'id': 'drink', 'name': '음료·주류', 'icon': 'minecraft:honey_bottle'},
        {'id': 'tool', 'name': '조리 도구', 'icon': 'minecraft:cauldron'},
    ],
    'items': food_items,
    'auto': [
        {'match': {'recipeTypes': DRINK_TYPES}, 'category': 'drink', 'formula': 'fixed', 'price': 300, 'dynamic': True, 'looseSell': True},
        {'match': {'recipeTypes': COOK_TYPES, 'class': 'food'}, 'category': 'dish', 'formula': 'fixed', 'price': 200, 'dynamic': True, 'looseSell': True},
        {'match': {'mods': COOK_MODS, 'tags': ['kaleidoscope_cookery:meals', 'kaleidoscope_cookery:feasts', 'farmersdelight:meals', 'farmersdelight:feasts']},
         'category': 'dish', 'formula': 'fixed', 'price': 300, 'dynamic': True, 'looseSell': True},
        {'match': {'mods': COOK_MODS, 'class': 'food'}, 'category': 'dish', 'formula': 'food', 'multiplier': 6},
        {'match': {'mod': 'minecraft', 'class': 'food'}, 'category': 'dish', 'formula': 'food', 'multiplier': 6},
    ],
    'exclude': ['minecraft:enchanted_golden_apple', 'minecraft:rotten_flesh', 'minecraft:spider_eye', 'minecraft:poisonous_potato',
                'minecraft:popped_chorus_fruit', 'minecraft:suspicious_stew',
                'farmersdelight:rotten_tomato', 'farm_and_charm:rotten_tomato'],
}

# ----------------------------------------------------------------------------- 수산물 상점
fisher_items = [
    E('minecraft:cod', 'fish', fish=True), E('minecraft:salmon', 'fish', fish=True),
    E('minecraft:pufferfish', 'fish', fish=True), E('minecraft:tropical_fish', 'fish', fish=True),
    E('starcatcher:worm', 'bait'), E('minecraft:string', 'bait'),
    E('minecraft:oak_boat', 'gear', sell=False), E('minecraft:water_bucket', 'gear'), E('minecraft:bucket', 'gear', buy=False),
    E('starcatcher:pearl', 'treasure'), E('minecraft:nautilus_shell', 'treasure'), E('minecraft:heart_of_the_sea', 'treasure'),
    E('minecraft:scute', 'treasure'), E('minecraft:sponge', 'treasure'),
    E('minecraft:lily_pad', 'sea'), E('minecraft:seagrass', 'sea', sell=False), E('minecraft:sea_pickle', 'sea'),
    E('minecraft:ink_sac', 'sea'), E('minecraft:glow_ink_sac', 'sea'), E('minecraft:prismarine_shard', 'sea'),
    E('minecraft:prismarine_crystals', 'sea'), E('minecraft:kelp', 'sea', sell=False), E('minecraft:dried_kelp', 'sea'),
]
fisher = {
    '_설명': HELP + [
        "스타캐처로 낚은 물고기는 희귀도에 따라 기준 가격이 정해지고, 무게가 무거울수록 비싸게 매입합니다.",
        "기본 설정: 평균 무게 = 가격표 가격, 가장 가벼움 = 75%, 가장 무거움 = 125%, 황금 물고기 = 3배 (config/rtshop/pricing.json).",
        "무게 정보가 없는 물고기(상점에서 산 것, 몹이 떨어뜨린 것)는 가격표의 50% 에 매입합니다.",
    ],
    'title': '수산물 상점', 'npcName': '수산물 상인', 'profession': 'fisherman',
    'categories': [
        {'id': 'fish', 'name': '물고기', 'icon': 'minecraft:cod'},
        {'id': 'gear', 'name': '낚시 도구', 'icon': 'minecraft:fishing_rod'},
        {'id': 'bait', 'name': '미끼·찌·바늘', 'icon': 'minecraft:string'},
        {'id': 'treasure', 'name': '보물·잡동사니', 'icon': 'minecraft:nautilus_shell'},
        {'id': 'sea', 'name': '해양 자원', 'icon': 'minecraft:prismarine_shard'},
    ],
    'items': fisher_items,
    'auto': [{'match': {'tags': [t]}, 'category': 'fish', 'price': p, 'fish': True, 'looseSell': True, 'buy': False} for t, p in FISH_TAGS] + [
        {'match': {'mods': ['starcatcher'], 'tags': ['starcatcher:rods']}, 'category': 'gear', 'formula': 'gear', 'price': 2000, 'multiplier': 5},
        {'match': {'class': 'fishing_rod'}, 'category': 'gear', 'formula': 'gear', 'price': 650, 'multiplier': 5},
        {'match': {'mods': ['starcatcher'], 'tags': ['starcatcher:tackle_boxes']}, 'category': 'gear', 'price': 1200},
        {'match': {'mods': ['starcatcher'], 'tags': ['starcatcher:gadgets']}, 'category': 'gear', 'price': 2500},
        {'match': {'mods': ['starcatcher'], 'tags': ['starcatcher:hats']}, 'category': 'gear', 'price': 400},
        {'match': {'mods': ['starcatcher'], 'tags': ['starcatcher:baits']}, 'category': 'bait', 'price': 60},
        {'match': {'mods': ['starcatcher'], 'tags': ['starcatcher:hooks']}, 'category': 'bait', 'price': 400},
        {'match': {'mods': ['starcatcher'], 'tags': ['starcatcher:bobbers']}, 'category': 'bait', 'price': 300},
        {'match': {'mods': ['starcatcher'], 'tags': ['starcatcher:templates']}, 'category': 'treasure', 'price': 2000, 'buy': False},
        {'match': {'mods': ['starcatcher']}, 'category': 'treasure', 'formula': 'auto', 'price': 160, 'multiplier': 10, 'buy': False},
    ],
    'exclude': ['starcatcher:starcatcher_guide', 'starcatcher:settings', 'starcatcher:missingno', 'starcatcher:unknown_fish',
                'starcatcher:starcaught_bucket', 'starcatcher:starcaught_lava_bucket', 'starcatcher:starcaught_fish'],
}

# ----------------------------------------------------------------------------- 장비 상점
smith_items = [E('minecraft:arrow', 'ammo'), E('minecraft:spectral_arrow', 'ammo'), E('minecraft:firework_rocket', 'ammo')]
for i in ['coal', 'charcoal', 'raw_copper', 'copper_ingot', 'raw_iron', 'iron_ingot', 'raw_gold', 'gold_ingot', 'redstone',
          'lapis_lazuli', 'quartz', 'amethyst_shard', 'diamond', 'netherite_scrap', 'netherite_ingot',
          'netherite_upgrade_smithing_template', 'flint', 'gunpowder', 'obsidian', 'glowstone_dust']:
    smith_items.append(E('minecraft:' + i, 'mineral'))
smith_items.append(E('minecraft:emerald', 'mineral', buy=False))
for k in list(ORES) + ['emerald_ore', 'deepslate_emerald_ore']:
    smith_items.append(E('minecraft:' + k, 'mineral', buy=False))
smith = {
    '_설명': HELP + [
        "무기·방어구·도구의 가격은 공격력, 방어력, 내구도, 희귀도로 자동 계산됩니다 (formula: gear).",
        "가격표에 있는 재료만으로 만드는 장비는 재료값만큼 매입합니다 (철 주괴 2개로 만든 검 = 철 주괴 2개 값).",
        "광부는 광물 탭에서 캔 광물을 팔 수 있습니다. 원석 블록(섬세한 손길)은 맷돌에 갈았을 때 나오는 광물 개수만큼의 값입니다.",
    ],
    'title': '장비 상점', 'npcName': '장비 상인', 'profession': 'weaponsmith',
    'categories': [
        {'id': 'weapon', 'name': '근접 무기', 'icon': 'minecraft:iron_sword'},
        {'id': 'ranged', 'name': '원거리 무기', 'icon': 'minecraft:bow'},
        {'id': 'armor', 'name': '방어구', 'icon': 'minecraft:iron_chestplate'},
        {'id': 'shield', 'name': '방패', 'icon': 'minecraft:shield'},
        {'id': 'tool', 'name': '도구', 'icon': 'minecraft:iron_pickaxe'},
        {'id': 'mineral', 'name': '광물', 'icon': 'minecraft:iron_ingot'},
        {'id': 'ammo', 'name': '소모품', 'icon': 'minecraft:arrow'},
    ],
    'items': smith_items,
    'auto': [
        {'match': {'mods': ['miningmaster', 'jewelry'], 'tags': ['forge:gems']}, 'category': 'mineral', 'price': 1500},
        {'match': {'class': 'armor'}, 'category': 'armor', 'formula': 'gear', 'multiplier': 5},
        {'match': {'class': 'shield'}, 'category': 'shield', 'formula': 'gear', 'multiplier': 5},
        {'match': {'class': 'ranged'}, 'category': 'ranged', 'formula': 'gear', 'multiplier': 5},
        {'match': {'class': 'weapon'}, 'category': 'weapon', 'formula': 'gear', 'multiplier': 5},
        {'match': {'class': 'tool'}, 'category': 'tool', 'formula': 'gear', 'multiplier': 5},
    ],
    'exclude': ['tacz:*', 'artifacts:*', 'relics:*', 'kubejs:*', 'lrtactical:*'],
}

# ----------------------------------------------------------------------------- 잡화점
general_items = [E(f'minecraft:{w}_log', 'building') for w in LOGS]
for i in ['torch', 'lantern', 'glowstone']:
    general_items.append(E('minecraft:' + i, 'building'))
for i in ['ladder', 'chest', 'crafting_table', 'furnace', 'bookshelf', 'white_bed', 'glass', 'bricks', 'stone_bricks',
          'cobblestone', 'stone', 'sand', 'gravel', 'dirt', 'clay_ball']:
    general_items.append(E('minecraft:' + i, 'building', sell=False))
for i in ['bone', 'string', 'gunpowder', 'slime_ball', 'spider_eye', 'leather', 'feather', 'paper', 'book',
          'ender_pearl', 'blaze_rod', 'flint', 'coal']:
    general_items.append(E('minecraft:' + i, 'material'))
for i in ['glass_bottle', 'stick', 'bowl', 'ice', 'snowball']:
    general_items.append(E('minecraft:' + i, 'material', sell=False))
# 값만 정해 두는 품목 (팔지도 사지도 않음): 가격표에 없으면 추정가가 붙어서 이것이 들어간 음식값이 부풀 수 있다
for i in ['minecraft:cod_bucket', 'minecraft:salmon_bucket', 'minecraft:pufferfish_bucket', 'minecraft:tropical_fish_bucket']:
    general_items.append({'item': i, 'price': P[i]['price'], 'category': 'material', 'buy': False, 'sell': False})
for i in ['farmersdelight:tree_bark', 'minecraft:potion', 'minecraft:netherrack', 'minecraft:carved_pumpkin']:
    general_items.append({'item': i, 'price': P[i]['price'], 'category': 'material', 'buy': False, 'sell': False})
general_items.append(E('minecraft:rotten_flesh', 'material', buy=False))
for i in ['compass', 'clock', 'spyglass', 'name_tag', 'saddle', 'lead', 'shears', 'lava_bucket', 'experience_bottle']:
    general_items.append(E('minecraft:' + i, 'misc'))
for i in ['composter', 'oak_boat']:
    general_items.append(E('minecraft:' + i, 'misc', sell=False))
general_items.append(E('minecraft:bucket', 'misc', buy=False))
general = {
    '_설명': HELP + [
        "건축 자재와 잡다한 재료를 다루는 상점입니다. 몹 농장으로 쉽게 쌓이는 재료는 값을 낮게 잡았습니다.",
        "흙·돌·모래·판자·상자 같은 건축 자재는 팔기만 하고 매입하지 않습니다 (맨손이나 자동 농장으로 무한히 얻을 수 있기 때문).",
        "빈 양동이는 팔지 않습니다 (철 주괴 3개로 만들거나, 물·우유 양동이를 사서 쓰세요). 빈 양동이는 450G 에 매입합니다.",
    ],
    'title': '잡화점', 'npcName': '잡화 상인', 'profession': 'cartographer',
    'categories': [
        {'id': 'building', 'name': '건축', 'icon': 'minecraft:bricks'},
        {'id': 'material', 'name': '재료', 'icon': 'minecraft:string'},
        {'id': 'misc', 'name': '기타', 'icon': 'minecraft:compass'},
    ],
    'items': general_items,
    'auto': [
        {'match': {'mod': 'minecraft', 'tags': ['minecraft:planks']}, 'category': 'building', 'price': 2, 'sell': False},
        {'match': {'mod': 'minecraft', 'tags': ['minecraft:wool']}, 'category': 'material', 'price': 3},
        {'match': {'mod': 'minecraft', 'tags': ['forge:dyes']}, 'category': 'material', 'price': 4},
    ],
    'exclude': [],
}

# ----------------------------------------------------------------------------- 토지 상점
land = {
    '_설명': HELP + [
        "토지 문서를 파는 상점입니다. 문서를 들고 원하는 곳에서 우클릭하면 그 자리가 내 토지가 됩니다.",
        "첫 토지 문서는 한 사람당 한 번만 살 수 있는 할인 상품입니다. 문서는 되팔 수 없습니다 (토지를 반환하면 문서로 돌려받음).",
        "가격 기준: 생활 직업 수입이 시간당 약 20,000G 일 때 일반 토지 = 약 2시간, 대형 토지(넓이 4배) = 약 7시간 분량.",
    ],
    'title': '토지 상점', 'npcName': '토지 상인', 'profession': 'librarian',
    'categories': [{'id': 'deed', 'name': '토지 문서', 'icon': 'rtshop:land_deed'}],
    'items': [
        {'item': 'rtshop:first_land_deed', 'price': 15000, 'category': 'deed', 'sell': False},
        {'item': 'rtshop:land_deed', 'price': 40000, 'category': 'deed', 'sell': False},
        {'item': 'rtshop:large_land_deed', 'price': 140000, 'category': 'deed', 'sell': False},
    ],
}

SHOPS = {'farmer': farmer, 'food': food, 'fisher': fisher, 'blacksmith': smith, 'general': general, 'land': land}
os.makedirs(OUT, exist_ok=True)
for name, data in SHOPS.items():
    with open(os.path.join(OUT, name + '.json'), 'w', encoding='utf-8') as fh:
        json.dump(data, fh, ensure_ascii=False, indent=1)

# ----------------------------------------------------------------------------- 검증용 base.json
# ShopLoader 가 만드는 것과 같은 기준표: 어느 상점에서든 살 수 있는(동적 가격이 아닌) 품목의 최저가
buyable = {}
hints = {}
seen = {}
for name in sorted(SHOPS):               # ShopLoader 는 파일 이름 순으로 읽는다
    for it in SHOPS[name].get('items', []):
        iid = it['item']
        key = iid + ('#seed' if it.get('seed') else '')
        if key not in seen:
            seen[key] = dict(it)
        rec = seen[key]
        if it.get('buy', True):
            rec['_buy'] = True
for key, it in seen.items():
    iid = it['item']
    if it.get('dynamic') or iid.startswith('rtshop:'):
        continue
    if it.get('_buy'):
        buyable[iid] = min(buyable.get(iid, 1e18), it['price'])
    elif not it.get('seed'):
        hints[iid] = it['sellPrice'] if 'sellPrice' in it else it['price']
for k in list(hints):
    if k in buyable:
        del hints[k]

if HARNESS:
    harness = json.load(open(HARNESS, encoding='utf-8')) if os.path.exists(HARNESS) else {}
    harness['base'] = buyable
    harness['hints'] = hints
    harness['estimate'] = 2
    # 자동 규칙으로 가격이 붙는 태그 (파일 이름 순, 규칙 순서대로 먼저 붙은 값이 우선)
    harness['tagBase'] = {'forge:seeds': 20, 'forge:mushrooms': 8, 'forge:crops': 40, 'forge:vegetables': 40, 'forge:grain': 40,
                          'forge:fruits': 40, 'forge:berries': 40, 'minecraft:planks': 2, 'minecraft:wool': 3, 'forge:dyes': 4}
    harness['tagHints'] = dict(FISH_TAGS)
    pricing = harness.setdefault('pricing', {})
    pricing['flatExtra'] = {}
    pricing['margins'] = MARGINS
    pricing['labor'] = LABOR
    harness.setdefault('remainders', {})['minecraft:milk_bucket'] = 'minecraft:bucket'
    with open(HARNESS, 'w', encoding='utf-8') as fh:
        json.dump(harness, fh, ensure_ascii=False, indent=1)
print('shops:', {k: (len(v.get('items', [])), len(v.get('auto', []))) for k, v in SHOPS.items()})
print('base:', len(buyable), 'hints:', len(hints))
