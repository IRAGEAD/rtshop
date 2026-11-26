# 이어받기 안내 (2026-11-26, 3.55.0 올린 직후)

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

- **3.55.0 을 올렸고(릴리스 latest = RT Shop 3.55.0) jar 와 요약을 사용자에게 보냈습니다.**
- 3.55.0 에 든 것:
  - 일반 귀환권 기본 가격 5,000 → 10,000 G (사용자 요청 C-4. 깔린 상점은 옛 기본값일 때만 오름).
  - 영어 2차(A-25): 설정 기본 문구 207줄 + 직업 특성 줄의 영어 조립 + 파티 알림 + NPC 이름. 자세한 것은 worklist 의 "3.55.0".
  - 모드팩 점검 보고서(A-16) `docs/modpack_check.md`, UI 검토(A-19) 결과도 그 문서 5절.
  - 시험 고침: landSweep 가 무작위 지형에서 실패하던 것.
- 3.54.0 에 든 것은 worklist 를 봅니다 (귀환권 조정 C-3, 지도 표시 A-10, 영어 1차, 스토리 검토 B-2, 3.53 묶음).

## 5. 사용자에게 보낸 3.55.0 요약 (참고)

1. 일반 귀환권 10,000 G.
2. 영어판 2차: 직업·레이드·주간 보상·보스 소개·결계·스킬라 단서·파티 알림.
3. 모드팩 점검 보고서와 "정해 주실 것" 셋(요리 이익 구멍 / 음악 HUD 자리 / NPC 웨이포인트의 세계 안 표시).
4. 직접 볼 것: `docs/review_checklist.md` 의 3.55 항목.

## 6. 남은 일

1. **사용자가 정한 뒤에 할 것** (`docs/modpack_check.md` 맨 아래): ① 요리 이익 구멍 — 방법 ㉮㉯㉰ 가운데 고르면 `PricingConfig`·가격 계산을 고침 (약 1~2시간) ② 음악 HUD 와 겹침 — 알림 줄을 내려 달라고 하면 고침 (약 20분) ③ 웨이포인트의 세계 안 표시 — Diorama 설정이라 안내만.
2. 사용자 답을 기다리는 것: A-22 음악(곡 파일), A-9 NPC 배치(사용자가 정함), A-14 히든 아이템(보류), B-15 NPC 외형(시안만 보여 줌), `/rtshop town export|import`(요청하면 만듦, `docs/town_transplant.md`).
3. 하면 좋은 것: 강화 창의 "+13%" 의 + 가 흐릿한지 게임에서 확인(필요하면 `enhance.json` 의 숫자 글꼴 크기 12 → 12.5). 영어판에서 아직 한국어인 것(운영자 명령의 답·편집 화면 항목 이름)은 운영자만 보는 글이라 남겨 둠.

## 7. 알아 둘 함정

- TTF 글꼴의 기준선 = 어센트(px) − 3 + shift. 게임 글꼴과 맞추려면 기준선이 y+7 이 되게 shift 를 잡습니다 (`assets/rtshop/font/enhance.json`).
- 물건 칸의 쿨타임 표시(`getCooldowns().addCooldown`)를 건 물건은 우클릭이 서버에 닿지 않습니다. 남은 시간을 알려 줘야 하는 물건에는 쓰지 않습니다.
- 죽어도 남아야 하는 플레이어 기록은 `getPersistentData()` 의 `Player.PERSISTED_NBT_TAG` 아래에 둡니다.
- 설치된 상점 파일에는 "없는 품목 덧붙이기"(`ShopLoader.APPENDED`)와 "옛 기본값일 때만 값 고치기"(`REPRICED`)만 합니다. 운영자가 고친 값은 건드리지 않습니다.
- 퀘스트 파일(story.json 등)은 고치지 않은 설치본이면 새 기본값으로 자동 교체됩니다(`.bundled_hashes`). dialogue.json 은 `version` 을 올려야 교체됩니다.
- Diorama 에는 공개 API 가 없어 `dev.diorama.waypoint.WaypointStore` 를 리플렉션으로 씁니다(`client/MapMarkClient`). Diorama 판이 바뀌어 모양이 달라지면 조용히 꺼지고 로그에 한 줄 남습니다.
- `rm -rf *` 같은 넓은 삭제는 막혀 있습니다. 지울 폴더를 정확히 적습니다.
- CI 스크린샷은 가끔 앞 장과 똑같이 찍힙니다 (기계가 느려 화면이 새로 그려지기 전에 찍힘). 화면의 문제로 보기 전에 앞 장과 같은지 봅니다.
- 모드팩 전체의 세계는 씨앗이 매번 다릅니다. 세계에 블록을 놓는 시험은 먼저 그 자리를 비워야 합니다.
