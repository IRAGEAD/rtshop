# 모드팩 성능 (3.70.0 ~, 사용자 요청 11-27 16:26 "모드팩의 성능을 최적화하는 것을 최우선 작업", 16:30 "최적화를 최대한 진행하되, 퀄리티 감소를 최소화")

## 1. 재는 법 (CI 의 perf job, 테스트 가지에서만)

- 모드팩 전체(`pack_files.json`)로 **실제 지형의 새 세계**(perfworld, 씨앗 20261127)를 만든다.
- 서버: `test/PerfTest` (-Drtshop.perftest=true) — 시작 지점에 RT Shop NPC 24 명 → 가만히 30초(idle) → 멀리 14×14 청크 생성(worldgen) → 몹 150 마리(mobs).
  결과 `rtshop-perf-server.txt` (단계별 MSPT 평균 · p95 · 최대, 청크 생성 속도, 메모리).
- 클라이언트: `client/ClientPerfTest` (-Drtshop.clienttest.only=perf) — 같은 세계를 싱글플레이로 열어
  마을(NPC 24 명) 앞에서 고개를 돌리며 30초 → 초당 12칸으로 새 땅을 날며 60초 → 몹 60 마리 곁에서 30초.
  결과 `rtshop-perf-client-<변형>.txt` (FPS, 내장 서버 MSPT, 엔티티 · 청크 수). 변형: base(팩 그대로) · lean · opt.
- CPU 몫: JFR(Java Flight Recorder) 표본을 `.github/scripts/perf_report.py` 가 모드별로 모은다 (스레드 무리 server · render · worldgen · dh · other, '포함'/'자기' 몫, 바쁜 메서드).
- DH 빈틈: `client/ClientDhTest` (-Drtshop.clienttest.only=dh) — 전용 서버(시야 6)에 렌더 거리 12 로 접속해 높이 올라 찍고(170), RT Shop 의 맞춤을 켜고 다시 찍는다(171).
- 결과는 `ci-perf` 가지 (사진 160 · 161 · 170 · 171 포함). CI 는 그래픽 카드가 없는 소프트웨어 그리기(llvmpipe)라 FPS 의 절댓값은 의미가 없고, **같은 실행 안의 변형끼리** 비교한다.
- 주의: Distant Horizons 3.3.3 + Chunky 는 모드팩의 dhcompat(overrides, CI 에서는 받을 수 없음) 없이 켜면 첫 청크에서 튕긴다 ("Chunky is not loaded" — DH 의 `ChunkyAccessor`). 측정에서는 Chunky 를 뺀다.

## 2. 찾은 것 (2차 실행 45968280083)

| 무엇 | 숫자 | 뜻 |
|---|---|---|
| ArcaneVortex | 내장 서버 틱의 약 10~12% (자기 7~8%: `NativeAgent.MixinHooks` · `AttributeGuard`) | 물건은 RT Shop 규칙으로 전부 막혀 있어 무게만 남음 |
| ArcaneVortex · Farsight 빼기 | 마을 MSPT 46.7 → 34.0 (-27%), 날기 42.9 → 36.9, 몹 49.8 → 47.4 | 같은 실행 · 같은 세계 |
| Diorama | 배경 스레드 CPU 의 75~80% (`TileBaker.trace/blocked`, `VoxelReader`) — 코어 하나를 늘 씀 | 지도 품질 FANCY(기본) / FAST |
| Distant Horizons | DH 스레드의 60% 이상이 `ChunkPosQueue.popClosest/addItem` (청크 갱신 대기열) | 배경 스레드 |
| Spelunkery | 지형 생성 스레드의 18~23% | 강화 재료 모드라 유지 |
| Curios · Epic Fight · Wither Reincarnated · Mowzie · TACZ · Artifacts · Critters | 내장 서버 틱의 각 2~4% | 콘텐츠 모드 |
| RT Shop | 내장 서버 틱의 0.4~0.7% | — |

## 3. DH 의 가까운 청크 빈틈 (사용자 16:33)

- 원인: Farsight 가 바닐라의 '유효 렌더 거리'를 내 렌더 거리로 늘린다 (서버 시야 6 · 렌더 거리 12 → 유효 12). 서버는 6 청크까지만 보내므로 처음 가는 곳은 6~12 사이가 비고,
  DH 는 12 바깥부터 LOD 를 그려 그 고리가 빈칸이 된다.
- 고침: `client/DhViewGuard` — RT Shop 서버가 지도 표시 패킷에 서버 시야 거리를 실어 보내고("view"), DH 가 있으면 서버에 들어가 있는 동안 렌더 거리를 그 값으로 낮춘다 (나가면 되돌림). client.json 의 dhMatchServer 로 끔.
- 권장: Farsight 를 빼고 DH 만 쓴다 (같은 비용에 훨씬 멀리, 빈틈의 원인 제거).
