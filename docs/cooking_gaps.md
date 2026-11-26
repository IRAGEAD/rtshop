# 요리 이익 구멍 — 조사와 조치 (C-6)

2026-11-26 · 3.56.0. 요청: "매입가 기준(가장 비싼 조리법)은 바꾸지 않고 막을 방법을 찾을 것."
근거: 모드팩 전체 점검의 `audit.txt` (3.56.0 부터 음식마다 조리법별 재료비와 재료가 함께 적힙니다).

## 무엇이 구멍이었나

점검 목록의 56종을 조리법별로 뜯어 보니 원인이 둘이었습니다.

### 1. 47종 — Kaleidoscope Cookery 의 "자유 조리법" (3.56.0 에서 막음)

그 모드의 냄비·육수 냄비에는 같은 음식에 조리법이 두 가지 있습니다.

| | 정식 조리법 (`pot` · `stockpot`) | 자유 조리법 (`flex_pot` · `flex_stockpot`) |
|---|---|---|
| 재료 | 정해진 수 (예: 소고기 3 + 고추 3) | 종류만 맞으면 수는 자유 (소고기 1 + 고추 1 도 됨) |
| 결과 | 음식 1개 | 음식 1개 + **품질 꼬리표** (최상급 · 우수 · 보통 · 미흡) |
| 품질 | 없음 | 재료 비율이 숨은 정답에 가까울수록, 냄비를 많이 채울수록 높음 |

매입가는 정식 조리법(가장 비싼 조리법) 기준이라, 재료 한 벌로 만든 자유 조리 음식을 같은 값에 팔 수 있었습니다 (예: fondant_pie 재료 316 G → 매입 1,387 G).

**조치**: 품질 꼬리표가 붙은 요리는 **자유 조리법의 재료 한 벌 값 + 손 조리 이익(10%)** 으로 삽니다.

- 정식 조리법으로 만든 요리의 매입가는 그대로입니다 (기준 변경 없음).
- 품질이 **최상급(Superb)** 인 요리는 정식 조리법과 같은 값입니다. 최상급은 냄비에 재료를 7개 이상 넣고 비율도 맞아야 나와서, 싸게 만들 수 없습니다.
- 우수 · 보통 · 미흡은 모두 재료 한 벌 값 기준입니다. 재료 한 벌로 만들었을 때 손해가 나지 않습니다 ("재료를 사서 만들어 팔아도 적자 없음" 유지). 미흡이라고 더 깎지는 않습니다 — 숨은 비율을 몰라서 미흡이 나오는 것이라, 깎으면 적자가 됩니다.
- 요리사의 판매 보너스(+28%)는 두 값 모두에 그대로 붙습니다.
- 상점의 판매 목록에서는 정식 요리와 자유 조리 요리가 값이 다른 줄로 나뉘어 보입니다.
- 끄려면 `config/rtshop/pricing.json` 의 `flexDishPricing` 을 false.

### 2. 9종 — 다른 모드가 더한 조리법 (정해 주실 것)

같은 음식을 만드는 조리법이 모드마다 달라서 생기는 차이입니다. 기준을 바꾸지 않고 막으려면 **한쪽 조리법을 막는 것**(`/rtshop recipes`)이 방법입니다. 막는 쪽에 따라 결과가 다릅니다: 싼 조리법을 막으면 값은 그대로이고, 비싼 조리법을 막으면 그 음식의 매입가가 싼 쪽 기준으로 내려갑니다 (어느 쪽이든 "남은 조리법 가운데 가장 비싼 것" 이라는 기준은 같음).

| 음식 | 매입가 | 싼 조리법 (재료비) | 비싼 조리법 (재료비) | 권하는 조치 |
|---|---:|---|---|---|
| minecraft:bread | 132 | Create 반죽을 굽기 3종 (26.7) | 조합대 밀 3개 (120), Farmer's Delight 밀 반죽 (132) | **막기**: `create:smelting/bread`, `create:smoking/bread`, `create:campfire_cooking/bread` — 화로 자동화로 찍어 낼 수 있어 가장 위험 |
| minecraft:pumpkin_pie | 156 | 바닐라 조합 (12) | Farmer's Delight 파이 반죽 (142) | **막기**: `farmersdelight:pumpkin_pie_from_pie_crust` → 매입가가 약 13 G 로 내려감 |
| minecraft:cake | 150 | Create 조합 (19) | 바닐라 (136), Farmer's Delight (134.5) | **막기**: `create:crafting/curiosities/cake` |
| kaleidoscope_cookery:sashimi | 147 | 대구 · 연어 (40) | 열대어 (133) | **막기**: `kaleidoscope_cookery:chopping_board/sashimi_from_tropical_fish` → 매입가 약 44 G |
| minecraft:beetroot_soup | 265 | Farmer's Delight 냄비 (121) | 바닐라 조합 (241) | 막기: `minecraft:beetroot_soup` (조합대 조리법. 냄비 조리법이 남음) → 매입가 약 133 G |
| minecraft:rabbit_stew | 252 | Farmer's Delight 냄비 (129) | 바닐라 조합 2종 (229) | 막기: `minecraft:rabbit_stew_from_brown_mushroom`, `minecraft:rabbit_stew_from_red_mushroom` → 매입가 약 142 G |
| kaleidoscope_cookery:shengjian_mantou | 228 | 9개 한꺼번에 (개당 73.7) | 1개만 (207) | 그대로 둠 권장 (한 번에 많이 만들수록 싸지는 조리법. 용암 양동이 값이 판마다 한 번 듦) |
| kaleidoscope_cookery:cooked_cut_small_meats | 59 | 익힌 닭고기 (26.7) | 그 밖 (53~59) | 그대로 둠 권장 (차액 30 G 남짓) |
| kaleidoscope_cookery:raw_cut_small_meats | 29 | 닭고기 (13.3) | 토끼고기 (26.7) | 그대로 둠 권장 (차액 15 G 남짓) |

- 위 네 가지(굵은 글씨)는 차이가 5배 이상이라 막기를 권합니다. 말씀 주시면 기본 조합법 규칙으로 넣어 드리겠습니다. 직접 하실 때는 `/rtshop recipes` 의 검색 칸에 음식 이름을 치면 조리법이 모두 나옵니다 (3.56.0 에서 검색·분류를 넣었습니다).
- 조합법을 막거나 푼 뒤에는 `/rtshop reload` 를 하면 값이 다시 계산됩니다. 점검 결과는 `config/rtshop/export/audit.txt` 의 "같은 음식인데 조리법에 따라 재료비 차이가 큰 경우" 에 조리법별로 나옵니다.
