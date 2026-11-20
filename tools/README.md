# 개발용 도구

모드(jar)에는 들어가지 않는 스크립트입니다. 기본 상점 파일·언어 파일·아이템 그림은 여기서 만들어 `src/main/resources` 에 넣습니다.

| 파일 | 하는 일 | 실행 |
|------|---------|------|
| `gen_shops.py` | 가격표 한 곳에서 6개 기본 상점 JSON 을 만든다 | `python3 tools/gen_shops.py src/main/resources/rtshop_defaults` |
| `gen_lang.py` | 한국어·영어 언어 파일을 갱신한다 | `python3 tools/gen_lang.py src/main/resources/assets/rtshop/lang` |
| `gen_art.py` | 수표·토지 문서 아이템 그림을 만든다 (Pillow 필요) | `python3 tools/gen_art.py src/main/resources/assets/rtshop/textures/item /tmp/preview.png` |

기본 가격을 바꾸려면 `gen_shops.py` 의 가격표를 고친 뒤 다시 실행하고, 이미 설치된 서버에도 새 기본값이 깔리게 하려면
`ShopLoader.DEFAULTS_VERSION` 을 올립니다 (예전 파일은 `config/rtshop/backup/` 으로 옮겨집니다).
가격 규칙이 지켜지는지는 빌드의 서버 자체 점검(`SelfTest`)이 확인합니다: 음식 적자 0종, 사서 되팔기·만들어 팔기로 이익이 나지 않을 것.
