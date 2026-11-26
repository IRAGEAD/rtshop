# 이어받기 안내 (2026-11-26 오후, 3.58.0 올린 직후)

새 세션은 이 파일 하나만 읽으면 이어서 일할 수 있게 적었습니다. 더 자세한 것은 `docs/worklist.md`(요청 목록·진행), `docs/story_bible_pointer.md`(코드 지도), `docs/review_checklist.md`(사용자가 직접 볼 것) 에 있습니다.

## 1. 사용자와의 약속 (꼭 지킬 것)

- 사용자는 한국어로 말하고 **코딩을 하지 못합니다.** 빌드·시험·올리기는 전부 대신 합니다. 답은 한국어로.
- 일을 시작하기 전에 **예상 소요 시간**을 먼저 말합니다. 과정과 시험에 문제가 없으면 **허락을 묻지 않고** 다음 단계로 갑니다.
- 단계가 끝날 때마다 **jar 파일을 보내고**(SendUserFile) 한국어로 단계별 요약을 붙입니다.
- 디자인을 고를 일이 있으면 시안을 여러 개 그림으로 보여 주고 고르게 합니다 (B-15 NPC 외형만 예외: 보여 주기만 함).
- 게임 원곡 음악은 넣지 않습니다 (곡 파일은 사용자가 줍니다).
- 마을 이름은 **베스탈리아**. 보조 에이전트(subagent)는 쓰지 않습니다.
- 한도를 아끼려면: 긴 출력을 화면에 찍지 말고(`| tail`, `| cut -c1-200`), 스크린샷은 꼭 필요한 것만 잘라서 봅니다.

## 2. 저장소와 올리는 법

- 저장소: GitHub `IRAGEAD/rtshop` (Forge 1.20.1 모드 "RT Shop"). 작업 가지 **v5**, 배포 가지 **main**.
- 새 세션에서는: 저장소를 붙이고(쓰기 권한) `git clone` → `git checkout v5`. 큰 작업은 `v5-nextN` 가지를 따로 만들어 시험한 뒤 v5 에 합칩니다.
- 커밋 메시지 끝에는 그 세션이 알려 주는 귀속 줄(Co-Authored-By …, Claude-Session …)을 붙입니다.
- **시험(CI)**: `.github/workflows/build.yml` — build(기본 서버·클라이언트 자체 점검, 약 12분) · integration · inspect · fullpack(모드팩 전체에서 점검 + 스크린샷, 약 30~40분).
  - 돌리기: `gh workflow run build.yml --ref <가지>` (v5-next 가지는 push 만으로는 돌지 않음)
  - 보기: `gh run view <id> --json jobs -q '.jobs[]|"\(.name) \(.status) \(.conclusion)"'`, 실패 로그 `gh run view <id> --log-failed`
  - fullpack 은 **main 에서는 결과를 가지에 올리지 않습니다** (v5-next 가지에서 돌린 것만 올라감). fullpack 은 실패해도 실행 전체는 초록으로 보이니(continue-on-error) job 의 결론을 따로 봅니다.
  - fullpack 결과: 가지 `ci-fullpack-screenshots` (강제 푸시되므로 `git fetch origin +ci-fullpack-screenshots:refs/remotes/origin/ci-fullpack-screenshots`). 안에 `rtshop-selftest.txt`, `rtshop-clienttest.txt`, 스크린샷, `log-summary.txt`, `diag.txt`. 커밋 메시지의 run 번호로 어느 실행 것인지 확인.
  - 다른 모드를 들여다본 결과: 가지 `ci-inspect` (같은 방식으로 fetch).
- **올리기**: v5 에서 `git push origin v5 && git push origin HEAD:main`. main 의 build 가 통과하면 릴리스 `latest` 가 바뀝니다.
  - 자산 번호: `gh api repos/IRAGEAD/rtshop/releases/tags/latest -q '.name, .published_at, (.assets[]|"\(.id) \(.name)")'`
  - 받기: `gh api -H "Accept: application/octet-stream" repos/IRAGEAD/rtshop/releases/assets/<id> > rtshop-<판>.jar` → 사용자에게 보냄.
  - GitHub 의 artifact 내려받기는 막혀 있습니다. 릴리스 자산만 됩니다.
- **로컬에서는 컴파일이 안 됩니다** (마인크래프트 라이브러리 없음). 문법만 미리 봅니다:
  `javac -proc:none -Xmaxerrs 9000 -d /tmp/jc -sourcepath src/main/java:src/stubs/java $(find src/main/java -name '*.java') 2>&1 | grep "error:" | grep -v "does not exist\|cannot find symbol\|cannot access\|is not visible\|static import only\|does not override"`
  → 변수 이름 겹침·없는 기호 같은 것은 CI 의 build 에서만 잡힙니다. 한 메서드에 지역 변수를 더할 때 이름이 겹치지 않는지 직접 확인하세요 (SelfTest 의 메서드가 아주 깁니다).
- 워크플로의 `run:` 한 덩어리는 `${{ }}` 가 있으면 21000자를 넘기면 안 됩니다 (넘으면 단계를 나눕니다).

## 3. 글을 고칠 때 돌리는 생성기 (순서대로)

```
python3 tools/gen_lang.py src/main/resources/assets/rtshop/lang   # 화면 문구 (ko/en 을 함께 적음)
python3 tools/gen_story.py                                        # 스토리 (tools/story_ch0X.py 가 원본)
python3 tools/gen_dialogue.py                                     # NPC 대화 (고치면 맨 위 VERSION 을 올림. 지금 15)
python3 tools/gen_readables.py ; python3 tools/gen_guide.py       # 읽을거리 · 안내서
python3 tools/story_lang/extract2.py ; python3 tools/story_lang/extract3.py   # 번역 안 된 글 모으기
python3 tools/story_lang/extract4.py                                          # 자바 코드의 설정 기본 문구 (레이드·보스 소개·단서·직업 이름 등)
python3 tools/story_lang/build.py                                 # 영어 표 만들기
git checkout -q tools/story_lang/__pycache__                      # (추적되는 캐시라 되돌려 둠)
```

- 영어 번역의 짜임: `story_lang/en_us.json` = "한국어 원문 → 영어". 서버가 `StoryText.t / StoryText.json` 으로 바꿔 보냅니다.
  - `source.json`+`en/*.json`(옛 대사), `ch0X_en.py`(퀘스트 제목·소개·진행·완료), `source2.json`+`en2/p*.py`(대화·읽을거리·안내서·초보/직업 퀘스트), `source3.json`+`en3/p*.py`(스토리 본문 1,548줄).
  - **원문을 고치면** 그 글은 목록 끝에 새 번호로 붙습니다. source2 는 `en2/p<번호>.py` 를 새로 더해 번역을 채우고(수가 다르면 build 가 멈춤), source3 는 처음부터 다시 뽑습니다: en3 폴더를 잠깐 옮기고 → build → `source3.json` 삭제 → extract3 → en3 되돌림 → 바뀐 번호의 번역을 고침 → build.
  - `source4.json`+`en4/p*.py`(설정 기본 문구 207줄: 자바 코드에서 extract4 가 모음). 자바의 기본 문구를 고치거나 더하면 extract4 → 목록 끝에 붙은 글을 `en4/p<번호>.py` 로 번역 → build. 자체 점검 "every default config text … has an English version" 이 빠진 것을 잡습니다.
  - 받는 사람의 언어로 보내는 법: 표에 있는 글은 `StoryText.t(p, 글)` / 서식이 있으면 `StoryText.f(p, 글, 값…)`, 코드에 박힌 짧은 알림은 `StoryText.pick(p, "한국어", "English")`, 수치로 조립하는 글은 `StoryText.en(p)` 로 갈라 따로 조립. 클라이언트 쪽은 `StoryText.tl(HudLang.storyCode(), 글)`.
  - 용어: Surveyor(조사대원), Survey Corps, the Order (of the Embers), Memory Gate, First Flame, Undying Ember, the Chronicle, Ashen Band, Captain Roran, Archivist Isabel, Elder Erwin, Oscar the Fisher, Bram the Smith, Haru the Town Clerk, name tag(명패), Hors, Helvar, Sirok, Skor, Nerakyss, Malkuth, Geburah, Chesed, Belloc, Orel, Isle of Dawn, Underworld Arena, King of the Furnace, Ignited Revenant, tomb chamber, Island of the Scale. "[모드] …" 머리와 `{key:…}` 는 그대로 둡니다.

## 4. 지금 상태

- **3.58.0 을 올렸고(릴리스 latest = RT Shop 3.58.0) jar 와 요약을 사용자에게 보냈습니다.** 같은 날 3.55.0 ~ 3.57.0 도 올렸습니다.
- 3.58.0 (사용자의 13:16 요청 D-1~D-9, worklist 의 "3.58.0"):
  기억의 문을 걸어 들어가는 문으로 + 보스 구역의 대기·불씨·깨어남 연출 + 파티 동행 16칸 / 요리 권장안(조리법 9개 기본 금지) / Create 조합법·전리품·블록 드랍 금지 / NPC 자리 배치 파일(`npc_layout.json`) / 미니맵 NPC 아이콘(말을 건 NPC 만 + 켜고 끄기) / 주식·히든 직업 제안서 / 마을 이식 설명.
- 3.57.0: B-15 NPC 외형 시안(그림 네 장, 보여 주기만) · B-20 이야기 장소로 들어가는 법.
- 가지 이름: v5-next ~ v5-next8 은 이미 쓰였습니다. 다음 작업 가지는 **v5-next9** 부터 (`git ls-remote --heads origin` 으로 먼저 확인).

## 5. 사용자에게 보낸 3.58.0 요약 (참고)

1. 기억의 문: 단추 → 앞에 문이 열림 → 걸어 들어감(16칸 안의 파티원 동행, 늦은 파티원은 문으로 합류) → 보스는 잠들어 있고 가운데 불씨를 우클릭하면(또는 45초 뒤) 4초 연출 뒤 깨어남.
2. 요리: 권장안의 여섯 음식(조리법 9개)을 기본으로 막음. 호박 파이·회·비트 수프·토끼 스튜는 매입가가 내려간다고 표로 설명함 (`docs/cooking_gaps.md` 끝).
3. Create: 조합법·상자/몹 전리품·블록 드랍 금지. "Create 는 던전 모드 IDAS 가 건축물에 쓰는 것이라 건축물은 그대로"라고 설명함. **이 해석이 맞는지 확인을 부탁해 둠** (던전 상자의 Create 물건을 남기고 싶었던 것이면 `item_policy.json` 의 lootBan · blockDropBan 에서 create:* 를 빼면 됨).
4. NPC 배치: `config/rtshop/npc_layout.json` 이 저절로 적히고, 그 파일을 넣은 서버에서 저절로 적용. "서버끼리는 파일 하나를 복사해야 한다 / 원하면 jar 기본값으로 넣어 주겠다"고 함.
5. 미니맵: 말을 걸어 본 NPC 만 아이콘이 뜨고 `/rtshop hud` 에서 끔.
6. 제안과 설명: `docs/ideas_hidden_jobs_stock.md`(가·나·다 안과 정할 것 4가지), 마을 이식(같은 좌표로 옮기기는 지금 가능, 다른 좌표는 명령을 만들어야 함).

## 6. 남은 일 (11-26 오후, 3.58.0 뒤)

바로 할 수 있는 것 (사용자 답 없이):
1. 모드팩 전체 점검에서 무작위 지형에 따라 흔들리는 두 항목 손보기: 화면 쪽 "the border is a low outline along the ground"(아래 함정 참고)와 서버 쪽 "buying a farmland lot removes the crops and structures standing on it"(토지 뒷정리: 3.55 에서 자리를 먼저 비우게 고쳤는데도 3.58.0 의 main 실행에서 울타리·판자가 남았다고 실패). 시험 코드만 고치는 일.
2. 영어판에 남은 한국어(운영자 명령의 답·편집 화면 항목 이름), 강화 창 "+13%" 글자 확인 — "하면 좋은 것".

사용자의 답·자료를 기다리는 것:
3. **주식 시스템 · 히든 직업(위조자 · 조사관)**: `docs/ideas_hidden_jobs_stock.md` 의 가·나·다 가운데 고르고 "정해 주실 것" 네 가지에 답하면 단계별로 만든다 (① 증서·시세·배당 → ② 위조·감정 → ③ 숨은 퀘스트). 예전 약속: 직업·퀘스트·주식은 단계별로 하나씩 만들어 전달.
4. **B-15 NPC 외형 실제로 만들기**: 사용자가 "나중에 얘기"하기로 함. 시안은 `docs/design/npc_looks_*.png`, 치수·색은 `tools/mock/npc_mock.py`. 만들 때: 주민 엔티티는 그대로 두고 RT Shop NPC 만 클라이언트에서 사람 모델로 그리는 렌더러 + 인물별 스킨(+입체 부품). 이야기 인물부터 나눠서. (화면 쪽에서 RT Shop NPC 를 알아보는 법은 지금 "NoAI + 이름 붙은 주민"뿐 — `MapMarkClient.looksLikeNpc`. 외형을 만들 때는 서버가 NPC 의 열쇠를 화면에 알려 주는 길이 필요하다.)
5. **A-22 음악**: 사용자가 "나중에 얘기". 틀은 있음. 곡 파일을 받으면 넣는다 (게임 원곡은 넣지 않는다).
6. **NPC 배치 파일을 jar 기본값으로**: 사용자가 `npc_layout.json` 을 보내면 `src/main/resources/rtshop_defaults/npc_layout.json` 으로 넣는다 (`NpcLayout.read` 가 config 에 파일이 없을 때 꺼내 쓴다).
7. **A-12 마을 이식 명령** (`/rtshop town export|import`): 설명만 했다. 요청하면 만든다 (2~3일분).
8. **A-14 히든 아이템 3개**: 보류.
9. **Create 금지의 해석 확인** (위 5절 3번).
10. **Create 물건이 남아 있는 다른 길** (모드팩 전체 점검에서 봄, 사용자에게 알리고 뺄지 물어 둠): 장비 상점에 지팡이로 올라간 `create:wand_of_symmetry`(9,000 G), 뽑기 장신구 묶음의 Create 1종, 마법 뽑기 스킬북의 Create 인챈트 2종(capacity · potato_recovery). 빼라고 하면: `ItemPolicy.Config.defaults()` 와 판 올리기(4판)에 shopBan `create:*` 를 더하고, 뽑기 묶음을 만드는 곳(Gacha 의 장신구·스킬북 모으기)에서 lootBan 에 걸리는 것을 거른다.

3.58.0 의 시험 결과 (기록):
- build (가지 v5-next8, 실행 45756341891): 서버 773개 전부 통과 · 화면 295/300 (늘 실패하는 5개뿐).
- 모드팩 전체 (main, 실행 45757436070 — main 은 결과를 가지에 올리지 않아 job 로그로 봄): 화면 320개 전부 통과 (미니맵 믹스인 확인 줄 `[4]`), 서버 817/818 — 실패 1개는 "buying a farmland lot removes the crops and structures standing on it" (토지 뒷정리 시험). 같은 코드의 가지 실행에서는 서버 시험이 전부 통과했으므로 무작위 지형에 따라 흔들리는 항목으로 봄.
- 가지 실행의 모드팩 전체 화면 점검은 러너가 중간에 끊겨(뒤 단계가 모두 skipped, 로그 없음) 결과가 없다.

## 7. 알아 둘 함정

- TTF 글꼴의 기준선 = 어센트(px) − 3 + shift. 게임 글꼴과 맞추려면 기준선이 y+7 이 되게 shift 를 잡습니다 (`assets/rtshop/font/enhance.json`).
- 물건 칸의 쿨타임 표시(`getCooldowns().addCooldown`)를 건 물건은 우클릭이 서버에 닿지 않습니다. 남은 시간을 알려 줘야 하는 물건에는 쓰지 않습니다.
- 죽어도 남아야 하는 플레이어 기록은 `getPersistentData()` 의 `Player.PERSISTED_NBT_TAG` 아래에 둡니다.
- 설치된 상점 파일에는 "없는 품목 덧붙이기"(`ShopLoader.APPENDED`)와 "옛 기본값일 때만 값 고치기"(`REPRICED`)만 합니다. 운영자가 고친 값은 건드리지 않습니다.
- 퀘스트 파일(story.json 등)은 고치지 않은 설치본이면 새 기본값으로 자동 교체됩니다(`.bundled_hashes`). dialogue.json 은 `version` 을 올려야 교체됩니다.
- Diorama 에는 공개 API 가 없어 `dev.diorama.waypoint.WaypointStore` 를 리플렉션으로 씁니다(`client/MapMarkClient`). Diorama 판이 바뀌어 모양이 달라지면 조용히 꺼지고 로그에 한 줄 남습니다.
- `rm -rf *` 같은 넓은 삭제는 막혀 있습니다. 지울 폴더를 정확히 적습니다.
- CI 스크린샷은 가끔 앞 장과 똑같이 찍힙니다 (기계가 느려 화면이 새로 그려지기 전에 찍힘). 화면의 문제로 보기 전에 앞 장과 같은지 봅니다.
- 다른 모드의 클래스에 Mixin 을 걸 때는 `src/stubs/java` 에 컴파일용 껍데기를 두고 `@Pseudo` + `remap = false` + `require = 0` 으로 씁니다 (예: `DioramaWorldPinsMixin`). 그 모드의 바이트코드는 inspect job 에 `javap -p -c` 를 더해 `ci-inspect` 가지에서 봅니다 (inspect 의 마지막 단계 `run:` 은 21000자 한도에 거의 찼으니 새 단계를 그 앞에 따로 둡니다).
- 가격 계산은 조합법 파일을 직접 읽습니다(`RecipeScan`). 운영자가 막은 조리법은 음식 매입가의 기준에서만 빠집니다 (원가 계산에는 그대로 쓰임).
- 모드팩 전체의 세계는 씨앗이 매번 다릅니다. 세계에 블록을 놓는 시험은 먼저 그 자리를 비워야 합니다.
- 흔적(살펴보기 표식)은 사람마다 `rtshop_spots` 에 자리가 남고, 살펴보거나 퀘스트를 포기할 때만 지워집니다. 자리를 정하는 조건을 고칠 때는 "이미 놓인 흔적"이 있는 사람을 생각합니다.
- 안내서(guide.json)를 고치면 `tools/gen_guide.py` 의 VERSION 을 올려야 설치본이 바뀝니다 (지금 2). 쪽 제목은 목록 칸(116px)에 들어가야 하고, 영어 제목도 같습니다 (화면 점검 `fitsForTest`).
- build 의 화면 점검(clienttest)은 모드 없는 기본 환경에서 **늘 5개가 실패**합니다 (강화 정보 창의 재료 칸 1개 · 별빛 수정 구체/마법 뽑기 4개: 스킬북을 주는 모드가 없어서). 3.56.0 과 3.57.0 이 같습니다. 그 단계는 continue-on-error 라 job 은 초록입니다. 새로 깨진 것인지 보려면 앞 실행의 FAIL 줄과 비교합니다: `gh api repos/IRAGEAD/rtshop/actions/jobs/<build job id>/logs | grep "FAIL "`.
- 모드팩 전체(fullpack)의 화면 점검 가운데 "the border is a low outline along the ground" 는 **지형에 따라 흔들립니다**: 시험용 토지가 무작위 지형에 놓여서 경계선이 오르내린 높이가 실행마다 다릅니다 (6.7 · 8.7 은 통과, 3.57.0 의 실행 45750104161 에서는 13.7 로 기준 12 를 넘어 실패 — 토지·경계 코드는 건드리지 않은 판). 다음에 손볼 때: 그 시험이 토지를 놓기 전에 자리를 평평하게 고르거나(`ClientSelfTest2` 의 plot 앞), 기준을 "땅의 높낮이 + 여유"로 바꿉니다. 그 실행의 나머지는 서버 810개 전부 · 화면 317/318 통과.
- 기억의 문(3.58): `/rtshop arena <보스>` 는 이제 문을 열 뿐이다(`StoryArena.openGate`). 곧바로 들여보내는 것은 `enter`(설정 walkIn=false 일 때와 문 안에 들어섰을 때). 점검에서 가짜 플레이어를 들여보낼 때는 `gatesForTest()` 의 문 자리를 고쳐 놓고 한 번 문 밖에 세웠다가(armed) 문 자리에 세운다. 보스는 `wake` 뒤 `WAKE_TICKS`(80틱)가 지나야 나온다.
- 보스 구역 가운데의 불씨는 진짜 블록(조각된 흑암 + 모닥불)이다. 보스가 나올 때 치운다. 물이 찬 경기장에서는 물로 되돌린다.
- NPC 자리 배치(`NpcLayout`)는 시험 환경(-Drtshop.selftest / clienttest)에서는 파일을 쓰지도 적용하지도 않는다 (`forceForTest` 로만). 시험이 세운 NPC 가 다음 시험의 세계에 서지 않게 하려는 것이니 바꾸지 말 것.
- recipes.json 의 `_applied` 는 "기본 금지를 이미 한 번 덧붙였다"는 기록이다. 기본 금지를 더할 때는 새 이름(예: `cook_gaps@2`)으로 `RecipeRules.addDefaults` 에 묶음을 더한다. 편집 화면이 보낸 규칙에는 이 기록이 없어 `save` 가 옛 기록을 이어 준다.
- item_policy.json 은 3판이다. 기본 규칙을 바꿀 때는 `Config.VERSION` 을 올리고 `fromJson` 에 "v < N" 묶음을 더한다 (운영자가 넣은 줄은 그대로 두고 덧붙이기만).
- 미니맵의 몹 아이콘은 Diorama 의 `dev.diorama.render.Radar.collect` 가 그린다 (`ci-inspect` 가지의 diorama-radar.txt). 우리 믹스인은 그 안에서 `Radar.kind` 를 부르는 자리 하나만 가로챈다. 모드팩 전체 화면 점검의 "info: the map mod's minimap asked RT Shop …" 줄의 숫자가 0 보다 크면 믹스인이 붙은 것이다.
- 안내서(guide.json)는 3판. 고친 글의 영어는 `en2/p759.py` 처럼 새 번호 파일에 (원문이 바뀌면 source2 끝에 새 줄이 붙는다. extract2 는 en_us.json 에 이미 있는 글을 건너뛰므로, 같은 세션에서 글을 또 고쳤으면 source2.json 과 en_us.json 을 되돌린 뒤 다시 뽑는다).
