# 이어받기 안내 (2026-11-26, 3.57.0 올린 직후)

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

- **3.57.0 을 올렸고(릴리스 latest = RT Shop 3.57.0) jar 와 요약을 사용자에게 보냈습니다.** 같은 날 3.55.0 · 3.56.0 도 올렸습니다.
- 3.57.0: B-15 NPC 외형 시안(그림 네 장, 보여 주기만) · B-20 이야기 장소로 들어가는 법(안내서 세 쪽 + `docs/story_places.md`) + 퀘스트 표시가 흔적의 자리를 가리킴 + 퀘스트를 포기하면 흔적 자리를 잊음. 자세한 것은 worklist 의 "3.57.0".
- 3.56.0: 편집 창 검색·분류(C-5), NPC 웨이포인트를 세계 안에서 숨김(C-8), 요리 이익 구멍 가운데 자유 조리법 47종(C-6).
- 가지 이름: v5-next ~ v5-next7 은 이미 쓰였습니다. 다음 작업 가지는 **v5-next8** 부터 (`git ls-remote --heads origin` 으로 먼저 확인).

## 5. 사용자에게 보낸 3.57.0 요약 (참고)

1. NPC 외형 시안 네 장: 방식 비교(지금 · A 주민 몸+전용 옷 · B 사람 몸+스킨 · C 사람 몸+입체 장식)와 인물 27명. "가능하다"고 답했고 의견은 묻지 않았다.
2. 이야기 장소: 연습용 기억의 문은 "흔적"(다가가 우클릭), 기억의 문(보스전)은 퀘스트 수첩·퀘스트 이야기의 단추. 안내서에 세 쪽을 더했다.
3. 퀘스트 표시의 「➤」 가 흔적의 자리도 가리킨다. 퀘스트를 포기했다 다시 받으면 흔적이 곁에 새로 나타난다.
4. 직접 볼 것: `docs/review_checklist.md` 의 3.57 항목.

## 6. 남은 일 (11-26 낮, 3.57.0 뒤)

바로 할 수 있는 것은 없습니다. 아래는 모두 사용자의 답·자료를 기다립니다.

1. **C-6 나머지 9종**: `docs/cooking_gaps.md` 의 표. "권장대로" 라고 하면 그 조리법 ID 들을 기본 조합법 규칙(`RecipeRules` 의 기본 disabled)에 넣는다 — 깔린 서버의 recipes.json 은 운영자 것이므로 "없으면 덧붙이기" 방식으로 한 번만. "권장 4종만" 이면 빵·호박 파이·케이크·회만.
2. **A-22 음악**: 틀은 있음. 곡 파일을 받으면 넣는다.
3. **A-9 NPC 배치**: 사용자가 자리를 정하면 반영.
4. **A-12 마을 이식**: 계획 문서(`docs/town_transplant.md`)만 있음. `/rtshop town export|import` 는 요청하면 만든다.
5. **A-14 히든 아이템 3개**: 보류.
6. **B-15 NPC 외형을 실제로 만들기**: 시안만 보여 줬다. 사용자가 "이 방식으로 만들어 줘" 라고 하면 시작한다 — 주민 엔티티는 그대로 두고(거래·저장 호환), RT Shop NPC 표식이 붙은 주민만 클라이언트에서 사람 모델(`HumanoidModel`)로 그리는 렌더러 + 인물별 스킨(64×64) + (시안 C 라면) 인물별 입체 부품 층. 시안의 치수·색은 `tools/mock/npc_mock.py` 의 인물 함수에 그대로 있다. 인물이 27명이라 한 번에 다 하지 말고 이야기 인물부터 나눠서.

사용자에게 물어 둔 것 (답이 오면 목록에 넣는다):
- 예전에 계획으로 말한 **주식 시스템**(문서로 사고파는 주식)과 **히든 직업 2개**(위조자 · 조사관)는 요청 목록에 없어 만들지 않았다. 지금도 원하는지 물어 둠.
- C-8 은 세계 안 표시만 숨겼다. **미니맵의 NPC 아이콘**도 없앨지 물어 둠 (하려면 Diorama 의 미니맵이 핀을 그리는 곳을 inspect 로 먼저 봐야 함).

하면 좋은 것: 강화 창의 "+13%" 가 흐릿한지 게임에서 확인(필요하면 `enhance.json` 의 숫자 글꼴 크기 12 → 12.5). 영어판에 남은 한국어(운영자 명령의 답·편집 화면 항목 이름).

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
