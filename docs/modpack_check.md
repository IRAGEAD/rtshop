# 모드팩 중간 점검 보고서 (A-16)

2026-11-26 · RT Shop 3.54.0 ~ 3.55.0 · 모드팩 "RT1 - 2" 전체(매니페스트 157개 파일, 서버에서 읽힌 모드 200개)를 CI 에서 켜고 본 결과입니다.
근거: `ci-fullpack-screenshots` 가지의 `log-summary.txt`(클라이언트 로그 11,172줄을 묶은 것), `mixin-errors.txt`, `villager-trade-mixins.txt`, `diag.txt`, `audit.txt`, 스크린샷 231장.

## 한 줄 결론

**게임이 튕기거나 켜지지 않는 문제는 없습니다.** 로그의 오류 512줄은 거의 전부 "다른 모드끼리의 자잘한 어긋남"이고, RT Shop 의 기능을 막는 것은 없었습니다.
직접 정해 주셔야 할 것이 세 가지 있습니다 (맨 아래 「정해 주실 것」).

## 1. RT Shop 자체 점검

| 점검 | 결과 |
|---|---|
| 서버 자체 점검 (모드팩 전체) | 798개 모두 통과 (run 45721437420) |
| 클라이언트 화면 점검 (모드팩 전체) | 313개 모두 통과 |
| 튕김(crash report) | 없음 |
| 서버 쪽 동작 오류(`server-action-errors.txt`) | 없음 |

- 한 번(run 45727942957)은 서버 점검 799개 가운데 2개가 실패했습니다. 원인은 **시험 쪽**이었습니다: 토지 뒷정리 시험이 좌표 (3300, 100, 3300) 에 나무를 세우는데, 모드팩 전체에서는 세계의 씨앗이 매번 달라 그 자리에 산이 솟아 있으면 잎을 놓을 빈칸이 없었습니다. 게임 기능의 문제는 아니고, 시험이 자리를 먼저 비우도록 고쳤습니다 (3.55.0).
- CI 스크린샷 가운데 앞 장과 똑같이 찍힌 것이 둘 있습니다 (`22_qedit_goals`, `111c_boss_intro_leviathan`). CI 기계가 느려 화면이 새로 그려지기 전에 찍힌 것이고, 실제 화면의 문제는 아닙니다.

## 2. 로그의 오류 512줄 — 무엇이었나

| 줄 수 | 낸 곳 | 내용 | 영향 |
|---:|---|---|---|
| 258 | (어느 모드의) 크리에이티브 탭 | 탭에 빈 아이템이 들어 있음 | 없음. 크리에이티브 탭에 빈칸이 생길 뿐 |
| 140 | efn (EpicFight Nightfall) | 무기 모션 데이터 읽기 실패 ("No constructor information") | **아래 2-1** |
| 34 | epicfightx (Epic Fight Extra) | 같은 종류 | 아래 2-1 |
| 2 | wom (Weapons of Miracles) | 같은 종류 | 아래 2-1 |
| 17 | idas (Integrated Dungeons and Structures) | 팩에 없는 모드(iceandfire · ars_nouveau · byg · biomesoplenty)의 아이템·바이옴을 찾음 | 그 구조물 상자에서 "없는 모드의 물건"만 안 나옴. 나머지 전리품은 정상 |
| 12 | arcanevortex (알파판) | 블록 10종의 전리품 표와 조합법 1개가 없는 아이템을 가리킴 | 아래 2-2 |
| 11 | 여러 모드 | Mixin 설정에 minVersion 이 없다는 알림 | 없음 |
| 6 | adorablehamsterpets | 공지 "읽지 않음" 표시 실패 | 없음 |
| 4 | mowziesmobs | 모델의 particle 텍스처 (알려진 메시지) | 없음 |
| 4 | epic_fight_avalon | 빈 데이터 파일 1개(`item_skins/merlin_gg.json`), 애니메이션 `melee_N` 해석 실패 | 그 스킨 하나만 적용 안 됨 |
| 1 | Fabric API (다른 모드 안에 든 것) | 주민 거래 Mixin 주입 실패 | 아래 2-3 |
| 1 | accessories | ContainerMixin 이 적용되지 않음 | 아래 2-4 |
| 1 | spiral_abyss | mowziesmobs 보스 방 설정이 맞지 않음 ("flat_room cannot contain decorations…") | 그 보스의 방 꾸밈이 기본 방으로 나옴 |
| 1 | voicecmd (Iron's Voicebound Spells) | 음성 작업 거부 | CI 에 마이크가 없어서. 실제 PC 에서는 무관 |
| 그 밖 약 20 | — | 소리 장치 없음 · 로그인 인증 401 · Epic Fight 웹 서버 접속 실패 · 조합법 등록이 오래 걸린다는 알림 등 | **CI 기계라서** 나는 것. 실제 PC 와 무관 |

### 2-1. 무기 모션 읽기 실패 (efn · epicfightx · wom, 176줄) — 직접 확인 권장

- 읽지 못한 것은 대부분 `visual_effect/…` 로 끝나는 **이펙트용 모션**과 몇몇 무기의 일부 동작입니다. 해당 무기: 야마토(`yamato` — dodge · orbit · killerbee · rapidslash · void_slash), HF 무라사마 · HF 블레이드(스킬 이펙트 전부), exsiliumgladius(콤보 전부), nf_meen(차지), ng_greatsword(2개), thornwheel, scythe, 세키로 fushigiri, summoned_sword.
- 무기를 드는 것·기본 공격은 화면 점검에서 정상이었습니다(스크린샷 `119c_efn_yamato` 등). 다만 **위 스킬을 썼을 때 이펙트가 비거나 동작이 멈춘 듯 보일 수 있습니다.**
- 야마토와 HF 무라사마는 무기 뽑기의 최고 등급이므로, 게임에서 한 번씩 스킬을 써 보시길 권합니다 (`docs/review_checklist.md` 에 적어 둠). 문제가 보이면 efn 과 Epic Fight 본체의 버전 짝이 맞는지부터 봐야 합니다 (efn 3.4.2 / epicfightx 1.2.1).

### 2-2. arcanevortex (알파 0.7.10)

- 블록 10종(`apocalypse_slab · stairs · wall`, `neon_frame_fence(_gate)`, `quantum_lattice_fence(_gate)`, `reinforced_ash_planks_fence(_gate)` 등)은 **캐도 아이템이 떨어지지 않습니다** (전리품 표가 없는 아이템을 가리킴).
- 조합법 `little_wooden_sword` 1개가 깨져 있습니다.
- 셰이더 경고가 약 200줄 있습니다 (이펙트 일부가 다르게 보일 수 있음).
- 이 모드의 아이템은 이미 NPC 상점에서 팔지 않게 해 두었으므로(B-32) 경제에는 영향이 없습니다.

### 2-3. 주민 거래 Mixin 주입 실패 (1건)

- 낸 곳: Fabric API 의 `fabric-object-builder` 조각 (`TradeOffersTypeAwareBuyForOneEmeraldFactoryMixin`). 이 조각은 `accessories-neoforge`, `txnilib`, `DistantHorizons` 안에 겹쳐 들어 있습니다 (`villager-trade-mixins.txt`).
- 영향: Fabric 방식으로 만든 "주민 종류(사막·눈 등)에 따라 달라지는 에메랄드 거래" 한 가지가 원래 방식 그대로 남습니다. **RT Shop 상점, 바닐라 주민 거래, 스킬라 단서의 주민 소문에는 영향이 없습니다.**
- 조치: 필요 없음.

### 2-4. accessories 의 ContainerMixin

- "인터페이스 Mixin 에 public 이 아닌 메서드가 있다"며 Mixin 하나가 통째로 빠집니다. 가방 안에서 특정 아이템을 찾는 기능을 장신구 칸까지 넓히는 부분입니다.
- 장신구를 차는 것·장신구 뽑기는 화면 점검에서 정상이었습니다. 다만 **다른 모드가 장신구 칸의 물건을 가방 안의 물건처럼 찾는 기능**이 있다면 그 부분은 동작하지 않을 수 있습니다 (어떤 기능이 해당하는지는 로그만으로는 알 수 없음).
- 조치: 급하지 않음. accessories 를 새 판으로 올릴 일이 있을 때 다시 확인.

## 3. 경고 4,876줄

| 줄 수 | 내용 | 영향 |
|---:|---|---|
| 3,621 | 각 모드의 설정 파일이 처음 만들어지며 기본값을 채움 ("Incorrect key … corrected") | 없음. 처음 켤 때만 |
| 약 240 | handson_delight 의 텍스처·모델 없음 (cookingforblockheads · youkaisfeasts 연동용 모델) | 그 두 모드가 팩에 없어서. 연동용 블록만 해당 |
| 약 120 | ydtwo (Yazz's Dungeons) `wraithlight_lantern` 의 모델 없음 | 그 등불 블록 한 종이 보라·검정 무늬로 보일 수 있음 |
| 약 10 | betternether 의 모델 없음 (`basalt_slab`, 상자 몇 종) | 그 블록 몇 종의 모양 |
| 약 200 | arcanevortex 셰이더 | 2-2 |
| 54 | crittersandcompanions · artifacts 의 등록 대기 알림 | 없음 |
| 34 | irons_spellbooks 의 마법서 조합법(구리·금·다이아몬드·네더라이트·용가죽 등)을 찾지 못함 | 어느 모드가 그 조합법을 바꾸거나 뺐는지는 로그로 알 수 없음. **마법서를 조합대에서 만들 수 있는지 게임에서 확인 필요** |
| 22 | efn 의 모델 11종 없음 | 그 아이템의 손에 든 모양 |
| 5~6 | "Can't keep up" (서버가 느림) | CI 기계에서 세계를 처음 만들 때. 실제 서버에서는 따로 봐야 함 |
| 1 | Embeddium "tainted" | 다른 모드가 Embeddium 내부를 고쳤다는 안내. 지원 범위 알림일 뿐 |

## 4. 가격 점검 (`audit.txt`)

- 재료를 모두 상점에서 사서 만들어 팔 때 **손해인 음식: 0종** (요청하신 기준 충족).
- 가격표에 없어 추정가를 쓴 재료 323종, 계산에 쓰지 못한 레시피 종류 23가지(create 의 filling · mixing, irons_spellbooks 의 가마솥 등). 값이 이상해 보이는 물건이 있으면 상점 파일에 가격을 직접 적으면 됩니다.
- **조리법에 따라 이익이 지나치게 커지는 음식이 56종** 있습니다 → 「정해 주실 것 1」.

## 5. 화면에서 본 것 (A-19 UI 검토와 함께)

스크린샷 163장(연출 장면 제외)을 12장씩 붙여 훑어봤습니다. RT Shop 화면에서 글자가 칸을 넘치거나 창이 잘리는 곳은 찾지 못했습니다. 눈에 띈 것:

1. **다른 모드의 음악 재생 HUD** ("재생 중인 곡이 없습니다 · Spotify | YouTube…")가 화면 왼쪽 위에 늘 떠서 RT Shop 의 알림 줄, 대화 창의 NPC 이름과 겹칩니다 → 「정해 주실 것 2」.
2. **지도 표시(A-10)로 생긴 Diorama 웨이포인트**가 세계 안에도 "직업 안내인 알버트 · 4 m" 처럼 떠서, NPC 바로 앞에서도 머리 위에 겹치고 장 제목·보스 등장 연출 위로도 보입니다 → 「정해 주실 것 3」.
3. 무기 뽑기 확률표에 `item.lrtactical.melee` 라는 이름이 5줄 보입니다. Blade of Shadow 근접 무기 5종인데, CI 에는 그 총기팩(모드팩 overrides 안에 있어 CI 가 받지 못함)이 없어서 이름을 못 찾은 것입니다. **실제 모드팩에서는 제 이름이 나와야 합니다** (확인 목록에 적어 둠).
4. 영어 화면에서 NPC 이름("행운 상인 럭키" 등)이 한국어로 남던 것은 3.55.0 에서 고쳤습니다.
5. 알림이 여러 개 한꺼번에 뜨면 창 맨 위의 탭 줄을 잠깐 가립니다 (몇 초 뒤 사라짐). 지금은 그대로 두었습니다.

## 정해 주실 것

> **답을 받아 3.56.0 에서 처리했습니다 (11-26):** ① 요리 이익 구멍 → 매입가 기준은 그대로 두고, 자유 조리법으로 만든 요리(품질이 붙은 요리)만 넣은 재료 값 기준으로 삽니다. 남은 9종은 `docs/cooking_gaps.md`. ② 음악 재생 HUD → 직접 자리·크기를 조정하시기로 함. ③ NPC 웨이포인트 → 세계 안에서는 뜨지 않고 M 지도에서만 보이게 고침 (`map_marks.json` 의 `showInWorld` 로 되돌릴 수 있음). 아래는 처음 보고한 내용입니다.

1. **요리 이익 구멍 (가장 중요)**: 음식의 매입가는 "가장 비싼 조리법" 기준이라, 같은 음식을 싼 조리법으로 만들면 차액이 그대로 남습니다. 요리사가 아니어도 됩니다.

   | 음식 | 매입가 | 싼 조리법 재료비 | 한 개당 남는 돈 |
   |---|---:|---:|---:|
   | kaleidoscope_cookery:fondant_pie | 1,387 G | 316 G | 약 1,071 G |
   | kaleidoscope_cookery:oil_splashed_fish | 881 G | 161 G | 약 720 G |
   | kaleidoscope_cookery:fried_caterpillar | 991 G | 301 G | 약 690 G |
   | kaleidoscope_cookery:four_joy_meatball_soup | 776 G | 177 G | 약 598 G |
   | kaleidoscope_cookery:stargazy_pie | 1,005 G | 434 G | 약 571 G |
   | minecraft:cake | 150 G | 19 G | 약 131 G |

   56종 가운데 51종이 Kaleidoscope Cookery 의 냄비·육수 냄비 요리입니다 (재료 칸에 "아무 고기" 같은 묶음을 받는 조리법이라 가장 싼 재료로 채울 수 있음).
   - 방법 ㉮ 매입가를 "가장 싼 조리법" 기준으로 바꾼다 → 구멍은 사라지지만, 비싼 조리법으로 만들면 손해가 납니다 (요청하신 "적자 없음" 기준과 부딪힘).
   - 방법 ㉯ 매입가를 "싼 조리법 재료비 × 정해진 이익률(비요리사 +10%, 요리사 +40%)"로 하고, 비싼 조리법은 손해가 날 수 있다고 안내서에 적는다.
   - 방법 ㉰ 그대로 둔다 (요리사 직업이 돈을 잘 버는 직업이 됨).
2. **음악 재생 HUD 의 자리**: 그 모드의 설정에서 HUD 를 끄거나 자리를 옮기는 쪽을 권합니다. 원하시면 RT Shop 의 알림 줄을 조금 아래로 내리는 방법도 있습니다.
3. **NPC 웨이포인트의 세계 안 표시**: 지도에만 남기고 세계 안의 이름표는 끄는 쪽을 권합니다 (Diorama 설정의 웨이포인트 표시). RT Shop 쪽에서 강제로 끌 수는 없습니다 (Diorama 에 웨이포인트별 설정이 없음).

## 다음에 점검할 때

- 돌리기: `gh workflow run build.yml --ref <v5-next 가지>` → `ci-fullpack-screenshots` 가지의 `log-summary.txt`.
- 3.55.0 부터 요약에 "모드 이름으로 뭉친 오류 목록"(같은 모드의 같은 오류 수백 줄이 한 줄)과 "오류 바로 다음 줄(예외 이름)"이 함께 나옵니다. 주민 거래 Mixin 은 jar 안에 겹쳐 든 jar 까지 찾습니다 (`.github/scripts/mixin_scan.py`).
