# 모드팩 성능 (3.70.0 ~, 사용자 요청 11-27 16:26 "모드팩의 성능을 최적화하는 것을 최우선 작업", 16:30 "최적화를 최대한 진행하되, 퀄리티 감소를 최소화")

## 1. 재는 법 (CI 의 perf job, 테스트 가지에서만)

- 모드팩 전체(`pack_files.json` + overrides 의 Arcane Vortex · Iron's Spells)로 **실제 지형의 새 세계**(perfworld, 씨앗 20261127)를 만든다.
  모드팩 overrides 의 다른 파일(dhcompat, 설정 파일)은 CI 에서 받을 수 없어 **각 모드의 기본 설정**으로 잰다.
- 서버: `test/PerfTest` (-Drtshop.perftest=true) — 시작 지점에 RT Shop NPC 24 명 → 가만히 30초(idle) → 멀리 14×14 청크 생성(worldgen) → 몹 150 마리(mobs).
  결과 `rtshop-perf-server.txt` (단계별 MSPT 평균 · p95 · 최대, 청크 생성 속도, 메모리, Arcane Vortex 가 건 고리 목록).
- 클라이언트: `client/ClientPerfTest` (-Drtshop.clienttest.only=perf) — 같은 세계를 싱글플레이로 열어
  마을(NPC 24 명) 앞에서 고개를 돌리며 30초 → 초당 12칸으로 새 땅을 날며 60초 → 몹 60 마리 곁에서 30초 → 큰 지도 사진(162_perf_map_<변형>).
  결과 `rtshop-perf-client-<변형>.txt` (FPS: 그린 화면 수로 소수점까지, 내장 서버 MSPT, 엔티티 · 청크 수).
  변형은 **한 가지씩만** 바꾼다: base(팩 그대로, Chunky 포함) · noav(Arcane Vortex 뺌) · nofs(Farsight 뺌) · dfast(Diorama 지도 품질 FAST) · addons(AI Improvements + Clumps 더함).
- CPU 몫: JFR(Java Flight Recorder) 표본을 `.github/scripts/perf_report.py` 가 모드별로 모은다
  (스레드 무리 server · render · worldgen · dh · other, '포함'/'자기' 몫, 바쁜 메서드, 무거운 모드마다 **무엇이 부르나 · 어디서 시간을 쓰나**).
- DH 빈틈: `client/ClientDhTest` (-Drtshop.clienttest.only=dh) — 전용 서버(시야 6, 한낮으로 멈춤)에 렌더 거리 12 로 접속해 90 칸 올라 찍고(170), RT Shop 의 맞춤을 켜고 다시 찍는다(171).
- 결과는 `ci-perf` 가지 (사진 160 · 161 · 162 · 170 · 171, `chunky-check.txt`). CI 는 그래픽 카드가 없는 소프트웨어 그리기(llvmpipe)라 FPS 의 절댓값은 의미가 없고,
  **같은 실행 안의 변형끼리** 비교한다. 내장 서버 MSPT 는 CPU 로 재는 값이라 변형 사이 비교에 가장 믿을 만하다.

## 2. 찾은 것

### 2-1. 3차 실행 45974223631 (base · lean = Arcane Vortex + Farsight 뺌, 같은 세계)

| 장면 | base 내장 서버 MSPT | lean | 차이 |
|---|---|---|---|
| 마을 (NPC 24) | 72.1 | 51.8 | -28% |
| 날기 (새 땅) | 93.6 | 63.9 | -32% |
| 몹 60 | 80.6 | 62.7 | -22% |

- **Arcane Vortex**: 마을 장면 내장 서버 스레드의 **14.2%** (자기 11.8%: `NativeAgent.Mixin.MixinHooks.invoke/enter`, `Mixin.Protection.AttributeGuard.bind`).
  Arcane Vortex 는 자체 '네이티브 에이전트'(JVMTI, Windows 에서는 DLL)로 바닐라 메서드 수십 곳에 고리를 걸어 **보호 장비를 입은 플레이어**를 지킨다
  (ProtectDisarm · ProtectUse · MotionGuard · UseGuard · CooldownGuard · AttributeGuard: 인벤토리 변경 · 아이템 사용 · 움직임 · 속성 값을 매번 검사).
  RT Shop 이 터무니없는 장비를 막아 보호가 켜질 일은 거의 없지만 검사 비용은 모든 엔티티 · 모든 호출에 든다. 자기 클래스를 바꾸는 다른 모드를 감시하는 장치(코드 지문 · 원본 복원)도 있어
  **RT Shop 이 안을 고치는 것은 위험** → 남길지(날개 등 콘텐츠) 뺄지는 사용자가 고른다.
- **Diorama**: 배경 스레드 CPU 의 **70~77%** (`TileBaker.trace/blocked`, `VoxelReader`) — 지도 조각을 굽는 일. 설정 `quality`(FANCY/FAST) · `gpuBaking`.
  (3차의 lean 은 설정 파일을 못 찾아 FANCY 그대로였다 → 4차에서 따로 잰다.)
- **Canary**(Lithium 이식): 이 모드팩과 함께 켜지지 않음 (accessories 의 mixin 오류 "Interface mixin contains a non-public method") → 넣지 않는다.
- Distant Horizons 서버 쪽: DH 스레드의 60% 이상이 `ChunkPosQueue.popClosest/addItem` (청크 갱신 대기열, 배경 스레드).
- 지형 생성: 바닐라 기후 표(`Climate$RTree`)와 Spelunkery(18~23%)가 대부분 → **Chunky 로 미리 만들어 두면 플레이 중에는 들지 않는다.**
- 전용 서버(플레이어 없음): 마을 · 몹 장면 MSPT 1~2 ms (여유 많음), 시작 80초, 메모리 GC 뒤 1.3 GB.

### 2-2. 4차 실행 (변형 하나씩) — 아래에 숫자

(결과를 받으면 채움)

## 3. RT Shop 이 고친 것 (3.70.0)

1. **DH 의 가까운 청크 빈틈** (사용자 16:33): 원인 = 서버가 보내는 거리(서버 시야 거리)보다 내 렌더 거리가 넓으면, 그 사이 고리에는 진짜 청크도 DH 의 LOD 도 없다
   (DH 는 렌더 거리 바깥부터 LOD 를 그린다. Farsight 가 바닐라의 '유효 렌더 거리'를 늘려 부추긴다).
   → `client/DhViewGuard`: RT Shop 서버가 지도 표시 패킷에 서버 시야 거리("view")를 실어 보내고, DH 가 있으면 서버에 있는 동안 렌더 거리를 그 값으로 낮춘다 (나가면 되돌림).
   client.json 의 `dhMatchServer` 로 끈다. 싱글플레이는 내장 서버가 내 렌더 거리를 그대로 쓰므로 해당 없음.
2. **DH 3.3.3 + Chunky 크래시** (`mixin/DhChunkyMixin`, `DhChunkyBindMixin`, `compat/ChunkyReady`): DH 는 청크가 바뀔 때마다(`SharedApi.applyChunkUpdate`)
   `IChunkyAccessor.tryRunFirstTimeSetup` 을 부르고, 거기서 '연결했음' 표시를 먼저 한 뒤 Chunky 에 연결한다. 세계를 여는 동안(시작 지점 청크)에는 Chunky 가 아직 등록 전
   (Chunky 는 ServerStartingEvent 에서 등록)이라 `IllegalStateException: Chunky is not loaded` → "Exception during promotion of chunk to FULL status" 로 튕겼다.
   → Chunky 가 등록될 때까지 설정을 건너뛴다('연결했음' 표시도 하지 않아 다음 청크 때 다시 시도). 모드팩의 **dhcompat 이 막던 문제를 RT Shop 이 대신 막으므로 dhcompat 을 뺄 수 있다.**
3. **ImmediatelyFast 빠른 버퍼 업로드 되돌리기** (`client/FastBufferFix`): dhcompat 은 켤 때마다 `config/immediatelyfast.json` 의 `fast_buffer_upload` 를 false 로 썼다.
   dhcompat 이 없으면 RT Shop 이 한 번 true 로 되돌린다 (다음 실행부터, 다른 값은 그대로, 한 번 한 뒤에는 플레이어 선택을 존중). client.json 의 `restoreFastBuffer` 로 끈다.

## 4. 추천 (사용자에게 보낸 것)

(4차 결과를 받으면 채움)
