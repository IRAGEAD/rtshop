# NPC 대화 기본값: python3 tools/gen_dialogue.py → src/main/resources/rtshop_defaults/dialogue.json
# 말투는 NPC 마다 하나로 고정한다 (아래 STYLE 참고). 줄 앞에 "이름: " 을 붙이면 말하는 사람이 바뀐다.
import json, os

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'rtshop_defaults', 'dialogue.json')
VERSION = 18

# 말투
#  에르윈(장로)   하게체  "~하게, ~네, ~일세"
#  이사벨(기록관) 해요체  "~해요, ~예요"
#  로란(탐험대장) 해라체  "~다, ~라, ~냐"
#  모리스(안내인) 해요체, 친절
#  한나(농부)     해요체, 밝음
#  오스카(어부)   하오체, 뱃사람 "~소, ~오, ~시오, ~구려" (3.64: 이야기 본문과 같은 말투로 표를 고침)
#  마르타(식품)   해요체, 다정
#  브람(대장장이) 하게체, 짧고 무뚝뚝 "~네, ~게, ~일세" (3.64: 이야기 본문과 같은 말투로 표를 고침)
#  피터(잡화)     해요체, 수다스러운 장사꾼 "손님"
#  에드먼드(토지) 합쇼체 "~습니다"
#  세라(레이드)   해요체, 차분
#  루시(티켓)     해요체, 발랄 "~요!"
#  엘리(출석 상점) 해요체, 상냥
#  그레고리(은행) 합쇼체, 격식
#  알버트(직업)   해요체, 차분한 스승 (직업 선행 퀘스트 1단계의 안내도 같은 말투: tools/gen_job_quests.py)
#  직업 사범: 레온 · 브론 · 가렌 해라체 / 리아 반말 "~야, ~해" / 엘리스 "~란다, ~하렴"
#  말투 점검: python3 tools/audit/speech.py (인물마다 문장 끝을 갈래로 나눠 어긋난 문장을 뽑는다)
#  하루(시청 서기) 합쇼체, 꼼꼼
#  럭키(뽑기)     반말 "~라구, ~지!"
#  로렌초(식당)   해요체, 요리사의 감탄 "벨리시모!"
#  가렛(현상금 게시판지기) 하오체, 과묵한 옛 사냥꾼 "~하오, ~시오"
#  테오(순위 기록원) 해요체, 숫자 앞에서 들뜨는 젊은 기록원
#  아델(칭호 관리인) 합쇼체, 조용하고 단정 "~습니다"

def need(flags=None, quests=None, notFlags=None, items=None):
    n = {}
    if items: n["items"] = items  # 이 물건을 지니고 있을 때만 (가방·손·갑옷)
    if flags: n["flags"] = flags
    if quests: n["quests"] = quests
    if notFlags: n["notFlags"] = notFlags
    return {"need": n}

def g(text, once=False, chance=None, **kw):
    """인사 한 줄. once: 이 사람에게 처음 한 번만 한다. chance: 가끔만 (0~1)."""
    o = {"text": text}
    if once: o["once"] = True
    if chance is not None: o["chance"] = chance
    if kw: o.update(need(**kw))
    return o

def topic(id, title, lines, **kw):
    o = {"id": id, "title": title, "lines": lines}
    if kw: o.update(need(**kw))
    return o

N = {}

N["quest_order_elder"] = {
    "greet": [
        "불을 지키고, 봉인을 지킨다. …무슨 일인가, 젊은이.",
        g("불을 지키고, 봉인을 지킨다. …견습이여, 오늘은 무엇을 배우러 왔나.", flags=["order_apprentice"]),
        g("(장로의 자리는 비어 있다. 책상 위에 장갑 한 켤레가 놓여 있다. 안쪽이 그을렸다.)", flags=["erwin_betrayal"]),
        g("…자네군. 발소리로 아네. 문은 열려 있네. 잠그지 말라더군, 로란이.", flags=["story_done"]),
    ],
    "chatter": [
        "이 장갑 말인가? 오래된 화상 자국이 있어서 말일세. 신경 쓰지 말게.",
        "요즘 밤마다 남쪽 하늘이 붉어지는군. 좋은 징조는 아닐세.",
        "수도회도 예전 같지 않네. 한때는 견습이 백 명이 넘었지.",
        "늙으면 잠이 줄지. …나는 잠을 잊은 지 오래일세.",
        g("일곱 봉인이라… 그 이름을 다시 듣게 될 줄은 몰랐네.", flags=["clue_hidden_page"]),
    ],
    "topics": [
        topic("order", "잿불 수도회에 대해", [
            "잿불 수도회는 '불을 지키는 자들'이 세운 곳일세.",
            "천 년 전, 일곱 왕이 불을 일곱 눈에 나누어 봉인했지. 그 봉인을 지켜보는 것이 우리 일이네.",
            "…지금은 나와 기록관, 탐험대장 셋뿐이지만 말일세.",
        ]),
        topic("chronicle", "잿빛 연대기에 대해", [
            "연대기 말인가. 수도회가 대대로 적어 온 기록일세.",
            "찢긴 장이 있다고? …오래된 책이니 그럴 수도 있겠지.",
            "기록관이 너무 깊이 파고들지 않았으면 좋겠군. 진실은 때로 사람을 태우네.",
        ], flags=["order_apprentice"]),
        topic("ember", "꺼지지 않는 불씨에 대해", [
            "그 불씨는 잘 있나. 주머니 안쪽이 따뜻하면 잘 있는 걸세.",
            "불씨는 쥔 사람의 이름을 한 번 붙들어 주네. 한 번뿐일세. 그러니 아끼게.",
            "남에게 주지는 말게. 특히… 아니, 아무것도 아닐세.",
        ], flags=["order_apprentice"]),
        topic("age", "장로님의 나이", [
            "내 나이 말인가? 하하, 세다가 잊어버렸네.",
            "수도회의 첫 견습이 들어오던 날을 기억하지. 그게 언제였는지는… 묻지 말게.",
        ], flags=["story_ch1_done"]),
    ],
}

N["quest_order_archivist"] = {
    "greet": [
        "어서 오세요. 기록관 이사벨이에요. 책장 사이로 조심해서 오세요.",
        g("조사대원님! 새로 알아낸 게 있으면 꼭 저한테 먼저 말해 주세요.", flags=["order_apprentice"]),
        g("…아직도 믿기지 않아요. 그분이 우리 곁에 그렇게 오래 있었다니.", flags=["erwin_betrayal"]),
    ],
    "chatter": [
        "잉크가 또 떨어졌어요. 오징어 먹물로 잉크를 만들 수 있다는 거 아세요?",
        "연대기는 거짓말을 하지 않아요. 다만 누군가 찢어 냈을 뿐이죠.",
        "책을 읽다 보면 시간이 사라져요. 아, 벌써 밤이네요.",
        "낡은 종이 냄새가 좋아요. 이상한가요?",
        g("페이지를 모을수록 무서워져요. 퍼즐이 완성되는 게 아니라, 그림이 점점 어두워지거든요.", flags=["clue_eighth_seal"]),
    ],
    "topics": [
        topic("seven", "일곱 왕과 눈", [
            "연대기에는 일곱 왕이 불을 일곱 눈에 나누어 담았다고 적혀 있어요.",
            "화로, 물길, 톱니, 물결, 눈꽃, 닫힌 입, 그리고 불꽃. 왕마다 문장이 하나씩이에요.",
            "눈이 어디 있는지는 연대기에 없어요. 잊은 게 아니라 일부러 안 적은 것 같아요.",
        ], flags=["order_apprentice"]),
        topic("eighth", "여덟째 자리", [
            "조각가 통비가 '봉인은 여덟'이라고 했다면서요.",
            "연대기에는 일곱뿐이에요. 그런데 왕의 문장을 그린 쪽을 보면 자리가 여덟 칸이에요. 한 칸이 비었어요.",
            "긁어낸 자국이 없어요. 처음부터 비워 둔 것처럼 매끈해요. 그게 더 이상해요.",
        ], flags=["clue_eighth_seal"]),
        topic("grey", "회색 망토의 학자", [
            "회색 망토는 수도회 초기 학자들이 입던 옷이에요.",
            "지금은 아무도 입지 않아요. 마지막으로 입은 사람의 기록이… 없어요. 이름까지 지워져 있어요.",
        ], flags=["clue_grey_cloak"]),
        topic("storm", "폭풍에 대해", [
            "옛 노래에 '폭풍을 다스리던 여인'이 나와요. 연대기에는 한 줄도 없고요.",
            "노래와 기록이 다를 때는, 보통 기록 쪽이 고쳐진 거예요.",
        ], flags=["story_ch3_done"]),
    ],
}

N["quest_order_captain"] = {
    "greet": [
        "탐험대장 로란이다. 볼일이 있으면 짧게 말해라.",
        g("왔냐, 견습. 신발 끈은 단단히 맸겠지?", flags=["order_apprentice"]),
        g("왔냐. …이제 견습이라고는 못 부르겠군. 그래도 인원 확인은 한다. 하나.", flags=["story_done"]),
    ],
    "chatter": [
        "지도는 발로 그리는 거다. 책상에서 그린 지도는 사람을 죽인다.",
        "원정대에 필요한 건 세 가지다. 식량, 칼, 그리고 돌아올 이유.",
        "북쪽 바다엔 가 봤냐? 숨 쉴 때마다 폐가 어는 곳이다.",
        "다리? 스무 해 묵은 거다. 비 오기 전날은 내가 구름보다 먼저 안다.",
        g("장로가… 아니, 그 녀석 이야기는 하지 마라. 아직 화가 안 풀렸다.", flags=["erwin_betrayal"]),
    ],
    "topics": [
        topic("expedition", "원정대에 대해", [
            "수도회 원정대는 봉인이 흔들릴 때만 움직인다.",
            "마지막 원정은 이십 년 전이었다. 그때 돌아온 건 나 하나였다.",
            "그러니 너는 꼭 살아 돌아와라. 명령이다.",
        ], flags=["story_ch1_done"]),
        topic("guardians", "깨어난 수호자들", [
            "녹슨 기사, 얼음 거인, 산꼭대기 석공, 태양의 사제. 스무 해 전 내 원정이 지나간 자리들이다.",
            "그때는 괴물인 줄만 알았다. 벽에 뭐가 적혀 있는지 읽어 볼 생각도 안 했다.",
            "이번에는 읽고 간다. 싸우기 전에 상대가 어떻게 사는 놈인지 아는 건 대장 일이다.",
        ], flags=["story_ch1_done"]),
        topic("sea", "얼어붙은 바다", [
            "얼음 위에서 싸워라. 물에 빠지면 갑옷이 널 끌어내린다.",
            "유령선 선장 코넬리아. 열다섯 해 전 삼백 명을 태운 채 얼음에 멈춘 배다.",
            "선장을 욕하는 사람은 이 마을에 없다. 그 배를 구하러 나간 어선들도 못 돌아왔으니.",
        ], flags=["story_ch2_done"]),
    ],
}

N["quest_guide"] = {
    "greet": ["반가워요, 모험가님! 모험 안내인 모리스예요. 처음이시면 저한테 뭐든 물어보세요."],
    "chatter": [
        "은행에 돈을 맡겨 두면 쓰러져도 잃지 않아요. 꼭 기억하세요!",
        "땅을 사면 그 안에서는 아무도 함부로 건드리지 못해요.",
        "상인들 이름을 외워 두면 장사가 훨씬 즐거워져요. 한나, 브람, 피터…",
        "밤에는 몬스터가 많아요. 횃불은 넉넉히!",
    ],
    "topics": [
        topic("city", "베스탈리아에 대해", [
            "베스탈리아는 폭풍을 피해 온 사람들이 세운 도시예요.",
            "부두 근처에 시청이 있고, 상인들은 광장 주변에 모여 있어요.",
            "성 영역은 보호 구역이라 아무것도 부술 수 없어요. 대신 토지를 사면 마음껏 지을 수 있죠!",
        ]),
        topic("order", "수도회 사람들", [
            "부두 끝 건물에 잿불 수도회 사람들이 있어요. 장로님, 기록관님, 탐험대장님.",
            "초보 모험가의 길을 마치면 장로님이 찾으실 거예요.",
        ]),
    ],
}

def merchant(key, greet, chatter, topics):
    N[key] = {"greet": greet, "chatter": chatter, "topics": topics}

merchant("shop_farmer", ["어서 오세요! 오늘 아침에 딴 채소예요. 아직 이슬이 맺혀 있죠?"], [
    "씨앗은 꼭 상점에서 산 걸 심으세요. 들판에서 주운 씨앗은 잘 안 자라요.",
    "비 오는 날이 제일 좋아요. 밭에 물 줄 필요가 없거든요!",
    "농지를 사면 같이 농사지어요. 이웃이 생기면 좋잖아요.",
    g("요즘 땅이 따뜻해요. 겨울인데도요. 이상하죠?", flags=["order_apprentice"]),
], [
    topic("seasons", "농사 이야기", [
        "밀은 금방 자라지만 값이 싸고, 호박은 오래 걸리지만 비싸게 팔려요.",
        "농부 직업을 얻으면 초보 농지를 하나 무료로 받을 수 있어요. 꼭 해 보세요!",
    ]),
    topic("warm_soil", "따뜻해진 땅", [
        "밭을 갈다 보면 흙이 미지근해요. 마치 아래에서 무언가 타고 있는 것처럼요.",
        "할머니가 그러셨어요. 땅이 따뜻해지면 오래된 불이 깨어나는 거라고.",
    ], flags=["order_apprentice"]),
])
merchant("shop_fisher", ["물고기 팔러 왔소? 무게 좋은 놈이면 값을 더 쳐 주오."], [
    "바다는 정직하오. 기다린 만큼 주지.",
    "황금빛 물고기를 낚으면 꼭 나한테 가져오시오. 그건 행운이오.",
    "그물 깁는 것도 일이오. 손가락이 성할 날이 없소.",
    g("얕은 물에서 고기가 사라지고 있소. 사십 년 바다 생활에 이런 건 처음이오.", flags=["order_apprentice"]),
    g("얼음이 올겨울엔 하루에 한 뼘씩 내려오오. 열다섯 해 동안 가만있던 것이.", flags=["story_ch2_done"], notFlags=["story_ch3_done"]),
], [
    topic("fish_gone", "사라지는 물고기", [
        "처음엔 대구가 줄더니, 다음엔 연어, 이젠 오징어까지 안 보이오.",
        "물고기는 바다가 아플 때 제일 먼저 떠나오. 바다가 아프다는 거요.",
    ], flags=["order_apprentice"]),
    topic("ghost_ship", "유령선 이야기", [
        "어부들 사이에 도는 이야기가 있소. 안개 낀 밤엔 얼음 속에서 배 한 척에 불이 켜진다고.",
        "열다섯 해가 지났는데 돛이 안 삭았다지. 선장이 아직도 선원들 이름을 부른다고.",
    ], flags=["story_ch2_done"]),
])
merchant("shop_food", ["어서 와요. 배고프죠? 얼굴에 다 써 있어요."], [
    "끼니는 거르지 마요. 모험도 배가 불러야 하죠.",
    "식당 로렌초 씨가 또 새 요리를 만들었대요. 냄새가 여기까지 나요.",
    "따뜻한 수프 한 그릇이면 웬만한 추위는 견뎌요.",
], [
    topic("recipe", "요리 이야기", [
        "같은 재료라도 굽느냐 끓이느냐에 따라 맛이 완전히 달라져요.",
        "원정 갈 때는 익힌 고기를 넉넉히 챙겨요. 날고기는 배탈 나요.",
    ]),
])
merchant("shop_blacksmith", ["뭘 찾나. 칼인가, 갑옷인가."], [
    "좋은 칼은 손에 붙네. 나쁜 칼은 손을 물지.",
    "강화는 운일세. 그런데 운도 준비한 사람한테 오더군.",
    "불 앞에 삼십 년일세. 덕분에 눈썹이 없네.",
    g("희귀 장비 말인가? 원정대에 든 사람한테만 파네. 규칙일세.", notFlags=["story_ch1_done"]),
], [
    topic("forge", "대장간 이야기", [
        "쇠는 거짓말을 안 하네. 두드린 만큼 단단해지지.",
        "강화하다 깨진 칼? 다시 녹이면 되네. 사람도 마찬가질세.",
    ]),
    topic("old_knight", "녹슨 기사의 문장", [
        "그 투구 문장, 나도 봤네. 불꽃 기사단 문장일세.",
        "우리 할아버지의 할아버지가 그 기사단 칼을 벼렸다고 했지. 옛날얘긴 줄 알았네.",
    ], quests=["story_2_04"]),
])
merchant("shop_general", ["어서 오세요, 손님! 없는 것 빼고 다 있습니다… 아니 있어요!"], [
    "손님, 이 횃불 좀 보세요. 오래 타요. 진짜로요.",
    "요즘 장사가 잘돼요. 다들 원정 준비를 하나 봐요.",
    "제가 말이 많다고요? 손님, 장사꾼이 말이 없으면 굶어요!",
    "개인 상점도 열 수 있다는 거 아세요? 손님 물건을 다른 분들께 팔 수 있어요.",
], [
    topic("rumors", "요즘 소문", [
        "부두에서 들었는데요, 밤마다 남쪽 하늘이 붉대요.",
        "또 누가 그러는데, 산꼭대기 수도원에서 돌 깎는 소리가 들린대요. 아무도 안 사는데요!",
    ]),
])
merchant("shop_land", ["토지 상인 에드먼드입니다. 어느 땅을 보러 오셨습니까?"], [
    "땅은 사라지지 않습니다. 가장 안전한 재산입니다.",
    "성 안의 땅은 보호받습니다. 대신 값이 조금 나갑니다.",
    "이웃을 잘 고르는 것도 땅을 사는 일입니다.",
], [
    topic("castle", "성 영역에 대해", [
        "성 영역은 베스탈리아 시청이 관리하는 보호 구역입니다.",
        "사신 땅 안에서만 짓고 부술 수 있습니다. 그 밖은 모두의 것이기 때문입니다.",
    ]),
])
merchant("shop_raid", ["레이드 상인 세라예요. 주간 레이드 보상을 바꿔 드려요."], [
    "레이드는 혼자보다 여럿이 쉬워요. 파티를 꾸려 보세요.",
    "이번 주 보스는 만만치 않아요. 준비를 단단히 하세요.",
], [
    topic("lord", "레이드의 불꽃", [
        "주간 레이드의 불꽃 군주는… 최초의 불꽃이 남긴 그림자래요.",
        "매주 같은 모습으로 다시 타오른대요. 누군가 계속 잠재워야 하죠.",
    ], flags=["story_done"]),
])
merchant("shop_gacha_shop", ["어서 오세요! 티켓 판매원 루시예요! 오늘의 행운을 사 가세요!"], [
    "조각을 모으면 티켓으로 바꿀 수 있어요! 버리지 마세요!",
    "어제 어떤 분이 전설을 뽑았어요! 다음은 손님 차례일지도요!",
    "티켓은 상하지 않아요. 모아 두셔도 돼요!",
], [])
merchant("shop_attendance_shop", ["출석 상점 주인 엘리예요. 출석 도장으로 물건을 바꿔 가세요."], [
    "매일 오시는 분들 얼굴은 다 기억해요. 오늘도 와 주셨네요.",
    "도장이 쌓이면 좋은 물건이 기다려요. 천천히 모으세요.",
], [])
merchant("shop_restaurant", ["어서 오세요! 식당 주인 로렌초예요. 오늘의 요리, 벨리시모!"], [
    "요리는 사랑이에요! 그리고 버터. 사랑과 버터!",
    "재료를 가져오시면 제가 비싸게 사 드려요. 좋은 재료는 좋은 요리가 되니까요!",
    "마르타 씨네 빵은 최고예요. 제 수프랑 먹으면… 아, 천국!",
], [
    topic("dream", "로렌초의 꿈", [
        "언젠가 베스탈리아 모든 사람에게 제 요리를 먹이는 게 꿈이에요.",
        "원정대에도 도시락을 싸 드리고 싶어요. 배고픈 영웅은 슬프잖아요!",
    ]),
])
N["bank"] = {"greet": ["베스탈리아 은행 그레고리입니다. 무엇을 도와드리겠습니까?"], "chatter": [
    "예금은 안전합니다. 쓰러지셔도 은행의 돈은 사라지지 않습니다.",
    "지갑의 돈은 위험에 노출됩니다. 큰돈은 맡겨 두시기를 권합니다.",
    "숫자는 정직합니다. 저는 그 점이 좋습니다.",
], "topics": [
    topic("history", "은행의 역사", [
        "베스탈리아 은행은 폭풍 이후 첫 번째로 세워진 건물입니다.",
        "사람들이 서로를 믿게 하려면, 먼저 돈을 믿게 해야 했습니다.",
    ]),
]}
# ---- 3.53: 맡은 일이 따로 있는 세 사람 (현상금 게시판·주간 순위·칭호)
N["quest_bounty"] = {"greet": ["게시판은 저쪽이오. 떼어 간 종이의 일은 끝내고 오시오."], "chatter": [
    "나도 예전엔 종이를 떼어 가던 쪽이었소. 무릎이 먼저 그만두더군.",
    "큰 놈을 노리기 전에 작은 놈부터 세어 보시오. 숫자가 손에 익어야 하오.",
    "하루치 종이와 한 주치 종이는 색이 다르오. 한 주치는 급하게 떼지 마시오.",
], "topics": [
    topic("board", "현상금 게시판", [
        "마을 밖에서 늘어난 것들을 줄여 달라는 종이요. 하루에 몇 장, 한 주에 몇 장까지 맡을 수 있소.",
        "맡은 놈을 정해진 수만큼 잡으면 값은 그 자리에서 나가오. 여기까지 돌아올 것 없소.",
        "싸우는 길을 걷는 이에게는 값을 조금 더 쳐 주오. 목숨 값이니 당연한 일이오.",
    ]),
]}
N["quest_ranks"] = {"greet": ["이번 주 기록 보러 오셨어요? 장부는 거짓말을 안 해요. 한 주가 끝나면 윗자리 셋한테 상이 나가요!"], "chatter": [
    "숫자가 올라가는 걸 보고 있으면 시간 가는 줄 몰라요. 하루 씨는 그게 병이래요.",
    "맨 윗줄은 매주 바뀌어요. 그래서 재밌는 거예요.",
    "저는 사람들끼리 주고받은 돈은 안 적어요. 그건 둘이서 얼마든지 부풀릴 수 있으니까요.",
], "topics": [
    topic("how", "주간 순위", [
        "종목마다 이번 주에 쌓은 것만 세요. 잡은 몬스터, 재운 수호자, 번 돈, 쓴 돈, 지닌 돈, 끝낸 퀘스트, 끝낸 현상금, 마친 원정이요.",
        "어느 길을 걷든 똑같이 쌓을 수 있는 것만 골랐어요. 낚시꾼만 유리하면 안 되잖아요.",
        "한 주가 끝나면 종목마다 첫째부터 셋째까지 상이 나가요. 상은 저한테 와서 받아 가세요. 맡아 두고 있을게요.",
        "첫째는 다음 한 주 동안 이름 앞에 '이번 주의' 칭호를 걸 수 있어요. 거는 건 아델 씨한테 가서요.",
    ]),
]}
N["quest_titles"] = {"greet": ["이름 앞에 무엇을 걸지는 스스로 정하시는 겁니다. 다만 하신 일만큼만 걸 수 있습니다."], "chatter": [
    "불은 이름을 먹는다고들 합니다. 그래도 한 일까지 먹지는 못합니다. 저는 그것을 적어 둡니다.",
    "칭호는 빌려 드리는 것이 아닙니다. 이미 하신 일에 붙은 이름표를 찾아 드릴 뿐입니다.",
    "높은 칭호를 얻으셨다고 낮은 칭호가 부끄러워지지는 않습니다. 처음 열 마리가 가장 어려운 법입니다.",
], "topics": [
    topic("how", "칭호", [
        "종목마다 쌓으신 만큼 칭호가 열립니다. 같은 종목의 칭호는 힘이 같고, 높을수록 조금 더 셉니다.",
        "힘을 빌려 쓰는 칭호는 두 개까지 고르실 수 있습니다. 서로 다른 종목이어야 합니다.",
        "남에게 보이는 칭호는 하나입니다. 말씀하실 때 이름 앞에 걸립니다. 힘을 쓰는 칭호와 달라도 됩니다.",
    ]),
]}
N["job"] = {"greet": ["직업 안내인 알버트예요. 어떤 길을 걸을지 정하셨나요?"], "chatter": [
    "직업에는 버프와 디버프가 함께 있어요. 장점만 있는 길은 없지요.",
    "검사는 검에, 궁수는 활에 강해요. 무기 설명에 분류가 적혀 있으니 확인해 보세요.",
    "생활 직업도 중요해요. 원정대는 칼만으로 굴러가지 않아요.",
], "topics": [
    topic("choose", "직업 고르는 법", [
        "먼저 선행 퀘스트를 마쳐야 그 직업을 고를 수 있어요.",
        "이미 직업이 있으면 다른 직업의 선행 퀘스트는 할 수 없어요. 한 길에 집중하세요.",
        "바꾸고 싶어지면 다시 오세요. 대가는 있지만요.",
    ]),
]}
N["attendance"] = {"greet": ["시청 서기 하루입니다. 오늘 출석 도장을 찍어 드리겠습니다."], "chatter": [
    "기록은 빠짐없이 남깁니다. 그것이 서기의 일입니다.",
    "출석 상점은 엘리 씨가 맡고 있습니다. 도장은 그쪽에서 쓰십시오.",
    "베스탈리아 인구가 늘고 있습니다. 서류도 늘고 있습니다.",
], "topics": [
    topic("records", "시청 기록", [
        "시청에는 폭풍 이후 모든 이주민의 이름이 있습니다.",
        "단 한 사람, 수도회 장로님의 기록만 출발지 칸이 비어 있습니다. '먼 지부에서 부임'이라고만 돼 있습니다.",
    ], flags=["clue_grey_cloak"]),
]}
N["gacha"] = {"greet": ["행운 상인 럭키라구! 오늘은 뭐가 나올까~?"], "chatter": [
    "전설은 기다리는 자에게 온다지! 아마도!",
    "제일 깊은 데 숨은 그거? 본 사람은 손에 꼽는다구. 이름도 안 알려 줄 거야. 근데 너라면 혹시?",
    "꽝도 분해하면 조각이 된다구. 버릴 건 하나도 없지!",
], "topics": []}


# ---------------------------------------------------------------- 2판 스토리: 선택에 대한 반응, 장마다 새 이야기
def more(npc, greet=None, chatter=None, topics=None, hints=None):
    n = N[npc]
    if hints: n.setdefault("hints", []).extend(hints)
    if greet: n["greet"].extend(greet)
    if chatter: n["chatter"].extend(chatter)
    if topics: n["topics"].extend(topics)

more("quest_order_archivist",
     greet=[g("조사대원님. …어머니의 일지는 제 책상 맨 위 서랍에 있어요. 매일 한 줄씩 읽어요.", flags=["story_ch1_done"])],
     topics=[
         topic("ch1_records", "불타는 기록고", ["서가의 기록이 남았어요. 당신이 불 속으로 뛰어든 덕분에요.",
                                             "…미아는 팔에 화상을 입었어요. 그래도 웃더라고요. 기록이 남아서 다행이라고.",
                                             "그런데 이상해요. 건진 페이지 몇 장에 보랏빛 잉크로 덧쓴 자국이 있어요. 거울에 비친 것처럼 뒤집힌 글씨로요.",
                                             "어머니 글씨는 아니에요. 누군가 연대기를 고쳐 쓴 거예요."], flags=["ch1_saved_records"]),
         topic("ch1_kas_spared", "놓아준 선장", ["카스는 깃발의 노래가 적힌 한 장을 품에 넣고 갔어요. 읽을 줄 모른대요. 그래도 가져갔어요.",
                                               "문턱에서 돌아보더니 그랬어요. '빚은 진 쪽이 기억한다.' …무슨 뜻인지는 아직 모르겠어요.",
                                               "그 뒤로 동쪽 망루의 깃발이 하나 줄었대요. 로란 대장님이 세어 보셨어요. 두 번요."], flags=["ch1_kas_spared"]),
         topic("ch1_kas_held", "넘겨진 선장", ["카스는 수도회 지하에 있어요. 장로님이 직접 맡으셨어요.",
                                             "밥을 가져다주러 갔더니 노래를 부르고 있었어요. 그 노래요. 가사를 받아 적어 뒀어요.",
                                             "장로님이 그 방 앞에 오래 서 계시는 걸 봤어요. 문은 안 여시고요. 그냥… 듣고 계셨어요."], flags=["ch1_kas_held"]),
         topic("ch1_told_all", "어머니의 마지막 줄", ["마지막 줄까지 읽어 줘서 고마워요. '그 손은 장갑을 끼고 있다.'",
                                                   "…알아요. 이 마을에서 장갑을 벗지 않는 사람이 누군지.",
                                                   "아직은 아무 말도 안 할 거예요. 증거가 필요해요. 어머니라면 그랬을 거예요."], flags=["ch1_told_all"], notFlags=["erwin_betrayal"]),
         topic("ch1_folded", "어머니의 마지막 줄", ["접어 둔 마지막 줄, 아직 안 폈어요. 책상 맨 위 서랍에 있어요.",
                                                 "가끔 손이 가요. 그런데 아직은요. 어머니도 끝까지 읽는 데 열다섯 해가 걸렸으니까요."], flags=["ch1_folded"], notFlags=["erwin_betrayal"]),
         topic("ch1_folded_after", "접어 둔 줄", ["접어 둔 줄을 폈어요. '…장갑을 끼고 있다.'",
                                               "당신이 그날 접어 준 게 고마워요. 그때 읽었으면 장로님 앞에서 아무것도 못 했을 거예요."], flags=["ch1_folded", "erwin_betrayal"]),
         topic("ch1_apprentice", "불타는 기록고", ["미아를 끌어내 준 거, 고마워요. 그 아이는 이제 당신을 따라다니겠대요.",
                                                "서가의 기록은 재가 됐어요. 그런데 이상하죠. 아깝지가 않아요.",
                                                "어머니도 사람을 먼저 구했을 거예요. 기록은… 다시 쓰면 되니까요."], flags=["ch1_saved_apprentice"]),
         topic("told_all", "어머니의 마지막 줄", ["마지막 줄을 끝까지 읽어 줘서 고마워요. 듣는 순간 다리가 풀렸지만요.",
                                             "장갑… 그 말을 들은 뒤로 장로님 손만 보게 돼요. 그래도 모른 척할게요. 아직은요."], flags=["ch1_told_all"], notFlags=["erwin_betrayal"]),
         topic("folded", "접어 둔 줄", ["마지막 줄, 아직 접어 두고 있어요. 매일 밤 펴 볼까 말까 해요.",
                                      "…당신이 접어 줘서 다행이에요. 제가 펼 수 있을 때 펼게요."], flags=["ch1_folded"], notFlags=["erwin_betrayal"]),
         topic("folded_open", "접어 둔 줄", ["접어 둔 줄을 오늘 폈어요. \"그 손은 장갑을 끼고 있다.\"",
                                           "당신이 접어 둔 덕분에, 제가 직접 펼 수 있었어요. 그게 중요했어요."], flags=["ch1_folded", "erwin_betrayal"]),
         topic("gloved_hand", "장갑 낀 손", ["어머니가 '장갑'이라고 쓴 건… 아니에요. 생각하고 싶지 않아요.",
                                         "장로님은 우리를 거둬 주신 분이에요. 어머니가 사라진 뒤에도요."], flags=["clue_gloved_hand"], notFlags=["erwin_betrayal"]),
         topic("after_betrayal", "에르윈", ["어머니는 끝까지 읽었고, 그래서 돌아오지 못했대요.",
                                        "…괜찮아요. 저는 끝까지 읽을 거예요. 그리고 돌아올 거예요. 당신들이 있으니까."], flags=["erwin_betrayal"]),
         topic("feigned", "그때의 대답", ["불씨를 내미는 척하셨죠. 그 틈에 제가 등불을 들고 물러섰어요. 불은 뺏겼지만 등잔은 남았어요.",
                                       "…그래도 다음엔 그런 연기는 하지 마세요. 심장이 멎는 줄 알았어요."], flags=["ch4_feigned"]),
         topic("refused", "그때의 대답", ["\"불을 지키고, 봉인을 지킨다.\" 장로님 앞에서 장로님이 가르친 서약으로 거절하셨죠.",
                                       "어머니가 들었으면 좋아했을 거예요."], flags=["ch4_refused"]),
         topic("forgave", "천 년 묵은 재로 쓴 한 줄", ["에르윈. 적었어요. 정해 주신 대로요. '스승이 지어 준 이름. 천 년을 거역했고, 마지막에 놓았다.'",
                                             "어머니 이름 바로 아래 줄이에요. 일부러 그랬어요. 어머니가 그 사람을 끝까지 읽었으니까요.",
                                             "용서한 건지는 모르겠어요. 적은 거예요. 적는 거랑 용서하는 건 달라요. 그래도 적었어요."], flags=["fin_forgave"]),
         topic("erased", "천 년 묵은 재로 쓴 한 줄", ["그 한 줄에는 엘레나부터 적었어요. 카엘과 원정대 열 명, 코넬리아의 삼백서른둘. 재가 모자랄까 봐 작게 썼어요.",
                                            "장로님 이름은 없어요. 그분도 그게 맞다고 했어요. '내 이름은 스승님이 지어 주신 것이니 스승님만 아시면 되네' 하고요.",
                                            "…없다고 잊는 건 아니에요. 저는 기억해요. 글씨 가르쳐 준 손을요. 적지 않았을 뿐이에요."], flags=["fin_erased"]),
     ])
more("quest_order_captain",
     greet=[g("왔냐. 명단은 안 태웠다. 기록관이 적었지. 이제 너희도 거기 적힌다. 돌아온 쪽 칸에.", flags=["ch2_roster_burned"])],
     topics=[
         topic("truth", "카엘에게 한 말", ["카엘에게 진실을 말했다. 버린 게 아니라 살린 거라고.",
                                         "놈이 웃더군. '알아. 처음부터 알았어. 그래도 원망할 사람이 필요했어.'",
                                         "…스무 해 동안 서로 미안해하고 있었던 거다."], flags=["ch2_told_truth"]),
         topic("secret", "카엘에게 한 말", ["끝까지 말하지 않았다. 카엘은 나를 원망하면서 갔다.",
                                         "그래도 괜찮다. 원망할 사람이라도 있어야 스무 해를 버티지.",
                                         "…가끔은, 말할 걸 그랬나 싶다."], flags=["ch2_kept_secret"]),
         topic("expedition", "원정대", ["조사대는 내가 본 원정대 중에 제일 시끄럽다.", "좋은 뜻이다."], flags=["story_ch2_done"]),
     ])
more("shop_fisher",
     greet=[g("왔소. 그물을 다시 던졌소. 손이 기억하더이다.", flags=["story_ch3_done"])],
     topics=[
         topic("elric", "아들 엘릭", ["엘릭은 나보다 그물을 잘 던졌소. 웃음소리가 컸고. 제 어미를 닮아서.",
                                    "코넬리아 선장의 배에 오르던 날 그랬지. '영감, 돌아오면 큰 놈 잡아 줄게.' 열두 살 때부터 나를 영감이라고 불렀소.",
                                    "부두에서 손을 오래 흔들었소. 그 애는 한 번 돌아보고 말았지. 뱃사람은 원래 그렇소."], quests=["story_3_01"]),
         topic("captain", "코넬리아 선장", ["선장은 선원 삼백 명 이름을 전부 외웠소. 이름만이 아니라 누가 추위를 타는지, 누가 노래를 못하는지도.",
                                         "원망하려고 열다섯 해를 애썼소. 안 되더이다. 그 사람이 내 아들한테 목도리를 하나 더 줬다는 걸 읽고 나서는 더."], quests=["story_3_30"]),
         topic("nerakyss", "배를 품는 것", ["어부들은 그놈을 미워하지 않았소. 무서워했지. 폭풍을 무서워하듯이.",
                                         "놈이 품던 배가 수백 척 올라왔소. 주인 찾아 주는 데 한 해는 걸리겠지. 뱃머리 이름이 남은 것부터 하나씩 하고 있소.",
                                         "저녁별은 못 건졌소. 놈이 안고 내려갔으니. 괜찮소. 데리러 간 배가 같이 간 거니까."], quests=["story_3_20"]),
         topic("released", "등불", ["등불을 껐소. 심지에서 연기가 올라가는데 그게 바다 쪽으로 가더이다. 바람은 뭍으로 부는데.",
                                   "열다섯 해 켠 등을 끄니 손이 허전해서, 부두에 등을 하나 달았소. 그건 아무나 보라고 켜는 거요."], flags=["ch3_released"]),
         topic("kept", "등불", ["등불은 아직 내 곁에 있소. 밤이면 그 안에서 웃음소리가 나오. '영감' 하고.",
                               "바다가 다 녹는 날 같이 끄기로 했지. 당신이랑. 잊지 마시오."], flags=["ch3_kept_lantern"]),
     ])
more("attendance",
     topics=[topic("grey_coin", "회색 금화", ["회색 금화의 현상금은 열다섯 해 전 봄에도 붙었습니다.",
                                              "그해 봄, 기록관 엘레나 씨의 실종 신고를 받은 것도 저였습니다.",
                                              "신고서를 가져간 분은… 장로님이었습니다."], flags=["clue_grey_coin"])])
more("quest_order_elder",
     chatter=[g("일곱 봉인… 천 년이 이렇게 짧을 줄이야.", flags=["story_ch3_done"], notFlags=["erwin_betrayal"])],
     topics=[topic("gloves", "장갑", ["이 장갑은 벗을 수가 없네. 벗으면… 다들 놀랄 걸세.",
                                     "언젠가 보여 주지. 그때가 오면."], flags=["clue_gloved_hand"], notFlags=["erwin_betrayal"])])


# ---------------------------------------------------------------- 직업 스승 다섯 (일상 이야기 + 스토리 이야기)
#  레온(검술 교관)  해라체, 엄하고 짧게 "~다, ~지"
#  브론(전사 대장)  호탕한 해라체 "하하! ~라고!"
#  가렌(방패 수호자) 말수 적은 해라체, 한두 마디
#  리아(사냥꾼)     반말 해체 "~야, ~지?"
#  엘리스(대마법사)  다정한 노스승 "~단다, ~구나"
def b(ko, en): return {"ko": ko, "en": en}
def gb(ko, en, **kw):
    o = {"text": b(ko, en)}
    if kw: o.update(need(**kw))
    return o
def tb(id, ko, en, lines, **kw):
    o = {"id": id, "title": b(ko, en), "lines": lines}
    if kw: o.update(need(**kw))
    return o

N["quest_sword_master"] = {
    "greet": [
        b("검은 정직한 무기다. 휘두른 만큼만 돌려주지. …무슨 일이냐.", "A sword is an honest weapon. It gives back only what you swing. …What is it?"),
        gb("수도회 견습이 됐다고? 칼끝이 흔들리면 서약도 흔들린다. 잊지 마라.", "An apprentice of the Order now? If your blade wavers, so does your oath. Remember that.", flags=["order_apprentice"]),
        gb("장로 일은 들었다. …검을 내려놓지 마라. 지금은 더더욱.", "I heard about the Elder. …Don't put down your sword. Least of all now.", flags=["erwin_betrayal"]),
    ],
    "chatter": [
        b("아침마다 천 번 휘두른다. 그래야 하루가 시작된 것 같지.", "A thousand swings every morning. Only then does the day feel started."),
        b("숫돌은 매일 쓴다. 날이 무뎌지는 건 하루면 충분하다.", "I use the whetstone every day. One day is all it takes for an edge to dull."),
        b("검술은 발에서 시작한다. 팔은 마지막이다.", "Swordplay starts at the feet. The arms come last."),
        b("제자 중에 제일 빠른 놈은 제일 먼저 다쳤다. 빠른 것보다 정확한 게 낫다.", "My fastest student was the first to get hurt. Precise beats fast."),
        gb("서리 숨결이 부는 산에 다녀왔다고? 칼날에 얼음이 끼면 바로 털어라.", "Back from the mountains with the frost breath? Shake the ice off your blade right away.", flags=["story_ch2_done"]),
    ],
    "topics": [
        tb("sword_way", "검사의 길", "The way of the sword", [
            b("검사는 한 걸음 안에서 싸운다. 그 한 걸음을 지배하는 자가 이긴다.", "A swordsman fights within a single step. Whoever rules that step wins."),
            b("다른 무기를 들면 손이 어색해질 거다. 그게 검사의 대가다.", "Pick up another weapon and your hands will feel wrong. That is the price of the sword."),
        ]),
        tb("leon_past", "레온의 옛날", "Leon's past", [
            b("나도 한때는 원정대에 들어가고 싶었다. 로란이 사람을 고를 때 말이다.", "I once wanted to join the expedition too. Back when Roran was choosing people."),
            b("떨어졌다. 그 덕에 살아 있지. …돌아오지 못한 열한 명 중에 내 동기가 둘 있었다.", "I wasn't picked. That's why I'm alive. …Two of my classmates were among the eleven who never came back."),
        ], flags=["story_ch1_done"]),
        tb("leon_guardians", "깨어나는 수호자들", "The waking guardians", [
            b("녹슨 기사와 싸웠다고? 그놈의 검은 무겁지만 느리다. 첫 휘두름을 피하면 길이 보인다.", "You fought the rusted knight? Its blade is heavy but slow. Dodge the first swing and you'll see the way in."),
            b("수호자들이 왜 깨어났는지는 모른다. 다만 검을 든 놈은 다 이유가 있어서 든 거다.", "I don't know why the guardians woke. But anyone who takes up a blade has a reason."),
        ], flags=["story_ch2_done"]),
        tb("leon_erwin", "장로에 대해", "About the Elder", [
            b("장로는 한 번도 검을 들지 않았다. 늘 장갑 낀 손을 등 뒤로 감췄지.", "The Elder never once held a sword. He always kept those gloved hands behind his back."),
            b("…검을 들지 않는 자가 더 무서울 때가 있다. 이번에 알았다.", "…Sometimes the one who never draws a blade is the one to fear. I learned that this time."),
        ], flags=["erwin_betrayal"]),
    ],
}

N["quest_war_chief"] = {
    "greet": [
        b("하하! 덩치 좋은 녀석이 왔군! 오늘은 뭘 부수러 왔나?", "Ha ha! A sturdy one! What are we smashing today?"),
        gb("견습이라고? 하하! 수도회 옷이 꽉 끼겠군! 근육 때문에!", "An apprentice? Ha! That Order robe must be tight. All that muscle!", flags=["order_apprentice"]),
        gb("다 끝났다면서! 하하하! 오늘은 내가 산다! …술은 마르타한테 가서 받아 와라!", "I hear it's all over! Ha ha ha! Drinks are on me today! …Go get them from Martha!", flags=["story_done"]),
    ],
    "chatter": [
        b("도끼 한 자루면 세상 웬만한 문제는 풀린다! 안 풀리면 두 자루다!", "One axe solves most of the world's problems! If not, use two!"),
        b("밥은 많이 먹어라! 굶은 전사는 첫 번째 바람에 날아간다!", "Eat plenty! A starving warrior is blown away by the first gust!"),
        b("어제 대장장이 브람이랑 팔씨름했다. …비겼다. 다음엔 이긴다!", "Arm-wrestled Bram the smith yesterday. …A draw. Next time I win!"),
        b("전사는 맞으면서 배운다! 그러니까 많이 맞아라! 하하!", "Warriors learn by getting hit! So get hit a lot! Ha ha!"),
        gb("거대한 쇠 기사가 쓰러졌다고? 그 녀석 투구 하나 가져오지 그랬냐! 내 방에 걸게!", "The giant iron knight fell? Should've brought me its helmet! I'd hang it on my wall!", flags=["story_ch2_done"]),
    ],
    "topics": [
        tb("warrior_way", "전사의 길", "The way of the warrior", [
            b("전사는 앞으로 간다! 뒤를 볼 시간에 한 걸음 더 간다!", "A warrior moves forward! Instead of looking back, take one more step!"),
            b("빨리 달리는 다리, 무거운 걸 드는 팔. 그거면 된다! 섬세한 건 레온한테 배워라!", "Fast legs, strong arms. That's all you need! Learn the delicate stuff from Leon!"),
        ]),
        tb("bron_sea", "바다 이야기", "Sea stories", [
            b("얼음 바다에 다녀왔다고? 나는 배 멀미가 심해서 못 간다! …비밀이다!", "You went to the frozen sea? I get terribly seasick, so I can't! …That's a secret!"),
            b("오스카 영감님 아들 얘기는 들었다. 그 집 등불, 마을에서 제일 오래 켜져 있었지.", "I heard about old Oscar's son. That lantern of his burned longer than any in town."),
        ], flags=["story_ch3_done"]),
        tb("bron_fight", "같이 싸우기", "Fighting together", [
            b("혼자 싸우는 놈은 강해 보여도 오래 못 간다! 파티를 짜라! 최대 열두 명이다!", "Lone fighters look strong but don't last! Form a party! Up to twelve people!"),
            b("불꽃 경기장에 간다면 날 불러라! …아, 나는 마을을 지켜야 하는구나. 하하! 대신 이 함성 가져가라! 우오오오!", "If you're going to the burning arena, call me! …Ah, I have to guard the town. Ha! Take this war cry instead! Raaargh!"),
        ], flags=["story_ch4_done"]),
    ],
}

N["quest_shield_keeper"] = {
    "greet": [
        b("…왔나.", "…You came."),
        gb("견습. 방패는 들었나.", "Apprentice. Did you bring a shield?", flags=["order_apprentice"]),
        gb("…다 돌아왔군. 좋다.", "…Everyone made it back. Good.", flags=["story_done"]),
    ],
    "chatter": [
        b("방패에 흠집이 늘수록 뒤에 선 사람이 산다.", "The more scratches on the shield, the more people behind it live."),
        b("…말은 적게. 방패는 높게.", "…Few words. Shield high."),
        b("넘어지지 않는 법보다 일어나는 법을 먼저 배워라.", "Learn to stand back up before you learn not to fall."),
        b("밤에는 성벽을 돈다. 잠이 안 와서가 아니다. …아마도.", "At night I walk the walls. Not because I can't sleep. …Probably."),
        gb("남쪽 하늘이 붉다. 방패 끈을 다시 묶어 둬라.", "The southern sky is red. Retie your shield straps.", flags=["story_ch3_done"]),
    ],
    "topics": [
        tb("tank_way", "탱커의 길", "The way of the tank", [
            b("동료 앞에 서는 자가 가장 늦게 쓰러져야 한다.", "The one who stands before their comrades must be the last to fall."),
            b("막을 때 방패를 밀어라. 받기만 하면 무너진다.", "Push the shield when you block. Only take hits, and you'll crumble."),
        ]),
        tb("garen_scar", "가렌의 흉터", "Garen's scar", [
            b("…이 흉터? 스무 해 전이다.", "…This scar? Twenty years ago."),
            b("원정대가 떠나던 날 부두에서 배웅했다. 짐을 들다 다쳤지. 그래서 못 갔다.", "I saw the expedition off at the docks. Hurt myself carrying their packs. So I couldn't go."),
            b("…갔으면 막을 수 있었을까. 매일 생각한다.", "…Could I have shielded them if I'd gone? I think about it every day."),
        ], flags=["story_ch1_done"]),
        tb("garen_tags", "명패", "The name plates", [
            b("로란이 명패를 받을 때마다 나도 이름을 외운다.", "Every time Roran takes back a name plate, I memorize the name too."),
            b("열 개. …다 모이면 성벽에 새길 거다.", "Ten. …When they're all back, I'll carve them into the wall."),
        ], flags=["story_ch2_done"]),
    ],
}

N["quest_hunter"] = {
    "greet": [
        b("쉿, 바람 소리를 들어 봐. …응, 이제 말해도 돼. 무슨 일이야?", "Shh, listen to the wind. …Okay, you can talk now. What's up?"),
        gb("견습이 됐구나! 수도회 사람들은 숲을 잘 안 오던데, 너는 자주 와.", "So you're an apprentice now! Order people rarely come to the woods, but you do.", flags=["order_apprentice"]),
        gb("하늘 색이 돌아왔어. 새들이 먼저 알더라. 네 덕이지?", "The sky's colour came back. The birds knew first. That was you, wasn't it?", flags=["story_done"]),
    ],
    "chatter": [
        b("활은 눈보다 귀로 쏘는 거야. 바람이 어디로 부는지 먼저 들어.", "You shoot a bow with your ears more than your eyes. Listen to where the wind blows first."),
        b("화살촉은 직접 갈아. 남이 간 건 어딘가 어긋나 있거든.", "Sharpen your own arrowheads. Someone else's are always off somewhere."),
        b("여우는 똑똑해. 발자국을 일부러 두 번 밟고 지나가.", "Foxes are clever. They step in their own tracks on purpose."),
        b("오늘 사슴을 봤는데 안 쐈어. 새끼가 있었거든.", "I saw a deer today but didn't shoot. It had a fawn."),
        gb("남쪽 사막 피라미드 근처엔 바람이 이상하게 불어. 화살이 자꾸 왼쪽으로 휘어.", "The wind blows strangely near the desert pyramid. My arrows keep curving left.", flags=["order_apprentice"]),
    ],
    "topics": [
        tb("archer_way", "궁수의 길", "The way of the archer", [
            b("멀수록 세게 박혀. 거리를 두는 게 궁수의 무기야.", "The farther away, the harder it hits. Distance is an archer's weapon."),
            b("대신 가까이 오면 끝이야. 그러니까 발을 멈추지 마.", "But if they get close, it's over. So never stop moving."),
        ]),
        tb("ria_forest", "숲의 소문", "Forest rumors", [
            b("밤에 숲 깊은 곳에서 노래가 들릴 때가 있어. 따라가면 안 돼. 진짜야.", "Sometimes you hear singing deep in the woods at night. Don't follow it. I mean it."),
            b("햇빛 가면을 쓴 무리가 남쪽 정글에 있대. 태양을 섬긴다나.", "They say a tribe in sun masks lives in the southern jungle. Sun worshippers, apparently."),
        ], flags=["story_ch1_done"]),
        tb("ria_kael", "카엘", "Kael", [
            b("카엘이라는 사람, 원정대 지도에 이름이 있었다며?", "That Kael—his name was on the expedition map, right?"),
            b("…가면을 스무 해 동안 쓰고 있었다니. 숲에서 혼자 오래 지내 봐서 알아. 사람은 혼자 있으면 무언가를 꼭 붙잡아.", "…Wearing a mask for twenty years. I've lived alone in the woods a long time, so I get it. Alone, people cling to something."),
        ], flags=["story_ch2_done"]),
    ],
}

N["quest_archmage"] = {
    "greet": [
        b("어서 오렴. 마력은 책 속에만 있지 않단다. 오늘은 무엇이 궁금하니?", "Welcome, dear. Magic isn't only in books. What are you curious about today?"),
        gb("수도회 견습이 되었구나. 불을 지키는 일… 마법사에게도 낯설지 않은 일이란다.", "So you've become an Order apprentice. Guarding the fire… it's not unfamiliar to a mage either.", flags=["order_apprentice"]),
        gb("…에르윈 일은 마음이 아프구나. 별은 다 보고 있었을 텐데.", "…My heart aches about Erwin. The stars must have seen it all.", flags=["erwin_betrayal"]),
    ],
    "chatter": [
        b("밤하늘의 별은 오래된 책이란다. 읽는 법만 알면 말이지.", "The night sky is an old book. If you know how to read it."),
        b("마나가 바닥나면 차 한 잔 하렴. 진짜로 도움이 된단다.", "When your mana runs dry, have a cup of tea. It truly helps."),
        b("주문은 외우는 게 아니라 이해하는 거란다. 이해한 주문은 잊히지 않아.", "Spells aren't memorized, they're understood. An understood spell is never forgotten."),
        b("오늘 밤엔 남쪽 별자리가 흐리구나. …폭풍이 오려나.", "The southern constellations are faint tonight. …A storm coming, perhaps."),
        gb("이그니스의 불꽃 속에 회색 연기가 섞여 있었다고? …그건 불의 것이 아니란다.", "Grey smoke mixed into Ignis's flames? …That doesn't belong to fire, dear.", flags=["story_done"]),
    ],
    "topics": [
        tb("mage_way", "마법사의 길", "The way of the mage", [
            b("마법사는 몸이 약한 대신 멀리, 넓게 본단다.", "A mage is frail in body, but sees far and wide."),
            b("앞에 설 동료를 믿으렴. 마법사는 혼자서는 별 하나만큼만 빛난단다.", "Trust the companions in front of you. Alone, a mage shines only as bright as one star."),
        ]),
        tb("elise_seals", "봉인과 마력", "Seals and magic", [
            b("봉인은 마력으로 짠 매듭이란다. 천 년을 버틴 매듭이면… 아주 정성스럽게 묶였겠지.", "A seal is a knot woven of magic. One that has held a thousand years… must have been tied with great care."),
            b("매듭은 묶은 사람만 쉽게 풀 수 있단다. 그 점을 잊지 마렴.", "Only the one who tied a knot can untie it easily. Don't forget that."),
        ], flags=["clue_eighth_seal"]),
        tb("elise_ink", "보랏빛 잉크", "Purple ink", [
            b("보랏빛 잉크? 환술사들이 즐겨 쓰는 것이란다. 거울에 비춰야 읽히는 글을 쓰지.", "Purple ink? Illusioners are fond of it. They write words that can only be read in a mirror."),
            b("…기록관 아이에게 거울을 가져가 보라고 전해 주렴.", "…Tell the archivist girl to try holding a mirror to it."),
        ], flags=["story_ch1_done"]),
        tb("elise_stars", "별의 이야기", "A story of the stars", [
            b("천 년 전에도 별은 같은 자리에 있었단다. 사람들만 자리를 바꿨지.", "A thousand years ago, the stars were in the same places. Only people moved."),
            b("이제 네 이름도 연대기에 남았구나. 별처럼 오래 남기를 바란단다.", "Now your name is in the chronicle too. May it last as long as the stars."),
        ], flags=["story_done"]),
    ],
}


# ---------------------------------------------------------------- 오래 이야기하면 흘리는 말 (히든 단서 쪽으로 아주 살짝)
# 일상 이야기를 12번 넘게 나누면 다섯 번에 한 번 꼴로 섞인다. 결정적인 답은 주지 않는다.
more("quest_order_archivist", hints=[
    "…이건 그냥 혼잣말이에요. 잘린 연대기 자리마다 아주 옅은 보랏빛이 묻어 있어요. 우리 잉크는 검은색인데요.",
    "어머니 일지에 이런 낙서가 있었어요. '거울은 거짓말을 한다. 거울 속 필경사는 더하다.' 무슨 뜻일까요.",
    "옛 뱃노래에 이런 절이 있어요. '우리에겐 배가 있었다. 마중 나간 누이는 돌아오지 않았다.' 연대기에는 그 누이가 한 줄도 없어요.",
])
more("quest_order_captain", hints=[
    "남쪽 따뜻한 바다엔 가라앉은 유적이 많다. 거기서 붓질을 하던 놈이 이상한 악보를 주웠다더군. 제목만 남은.",
    "폭풍이 칠 때 높은 곳에 올라가 본 적 있나? 바람이 종이를 날려 온다. 대개는 쓰레기지만… 대개는.",
    "원정 중에 소환사를 셋 잡은 적이 있다. 셋 다 보랏빛 잉크병을 품고 있었지. 습격 때만 나오는 놈들이다.",
])
more("shop_fisher", hints=[
    "따뜻한 바다에서 낚시를 하다 보면 소금에 절은 종이가 걸려 올라올 때가 있소. 대개는 버리오. 한 번은 인장이 찍혀 있었소. '마중하는 이'라고.",
    "뇌우 치는 밤엔 드라운드들이 이상한 비늘을 떨군다는 얘기가 있소. 아직도 찌릿하다던가.",
])
more("attendance", hints=[
    "시청 옛 장부에 이상한 항목이 하나 있습니다. '새벽의 섬 함대 입항 예정. 마중 준비.' 날짜가 없습니다. 취소했다는 기록도 없습니다.",
    "숲속 저택 주변의 안개 민원이 들어온 적이 있습니다. 담당자는 '거울이 사람을 비추지 않는다'고 적고 조사를 그만뒀습니다.",
])
more("gacha", hints=[
    "있잖아, 진짜 행운은 뽑기에만 있는 게 아니라구. 폭풍 치는 날 하늘에서 떨어지는 종이 같은 거? 아무것도 아니야~",
    "보랏빛 잉크로 쓴 복권은 절대 사지 마! 당첨 번호가 거울처럼 뒤집혀 있다구!",
])
more("quest_order_elder", hints=[
    "…여덟. 아니, 일곱이라고 했나? 나이를 먹으니 숫자가 헷갈리는군.",
])
for key, hint in (("shop_general", "손님, 바다 위 신전 얘기 들어 보셨어요? 폭풍이 그치지 않는 데 있대요. 누가 거기서 천 년째 배를 기다린다나요."),
                  ("shop_blacksmith", "바다 위에 신전이 있다더군. 폭풍이 안 그치는 데. 거기 쇠는 천 년이 돼도 녹이 안 슨다던데, 한번 보고 싶네."),
                  ("bank", "바다 위 신전에 관한 소문이 있습니다. 폭풍이 그치지 않는 곳이라 합니다. 저희 금고에 그곳 주화가 한 닢 맡겨져 있습니다. 찾아가신 분은 없습니다.")):
    if key in N:
        more(key, hints=[hint])


# ================================================================ 황혼의 불꽃 (Duskfire, efn:meen_spear): 지니고 있으면 몇몇 사람의 말이 달라진다
# 설정: 최초의 불꽃은 처음부터 이름을 태우던 불이 아니었다. 모든 것을 집어 삼키기 이전, 아직 기억을 지키던 때가 있었고, 그 불빛을 '황혼의 불꽃'이라 불렀다.
#   일곱 왕이 자기 왕국이 영원히 기억되길 바라 불에 기록(족보·연대기)을 태워 넣기 시작했고, 불은 이름의 맛을 들였다. 그것이 잿빛 해의 시작이다.
#   황혼의 불꽃은 불이 모든 것을 집어 삼키기 전날, 왕국에서 제련으로는 따를 자가 없던 대장장이가 소문으로만 전해지던 이름 없는 금속 안에 불 한 줌을 '식혀' 가둔 창이다.
#   그 대장장이는 창에 '자기 이름'을 새기지 않았다 (불이 처음 삼키는 것이 제 이름이면 안 되므로). 자신을 받아들인 이의 이름을 불타게 두지 않는다.
# 말하는 법 (3.52): 사람들은 그 창의 이름을 입에 올리지 않는다. "그 창", "그 불빛", "저물녘의 빛" 처럼 가리켜 말하되, 무엇을 가리키는지는 알 수 있게 한다.
#   놀라는 첫 인사는 그 사람에게 처음 한 번만(once), 그 뒤에는 가끔만(chance) 한마디 한다. 묻는 대화(주제)는 그대로 둔다.
DUSK = ["efn:meen_spear"]
DUSK_TOPIC = "그 창에 대하여"
more("quest_order_elder",
     greet=[g("불을 지키고… (장로의 눈이 창날에 머문다.) …오늘은 그 빛이 조용하군. 다행일세.", chance=0.2, items=DUSK),
            g("…그 창. (장로가 한 걸음 물러선다. 장갑 낀 손을 등 뒤로 감춘다.) 저물녘의 그 빛이군. 그걸 어디서 얻었나.", once=True, items=DUSK)],
     topics=[topic("duskfire", DUSK_TOPIC, [
         "가까이 대지 말게. …아니, 위험해서가 아닐세. 내 쪽이 부끄러워서 그러네.",
         "그 불은 모든 것을 집어 삼키기 이전, 아직 기억을 지키던 때의 불일세. 최초의 불꽃은 처음부터 이름을 태우지 않았네. 그저 기억했지. 해 질 녘의 빛으로. 사람들은 그 곁에 앉아 죽은 이의 이름을 불렀고, 불은 들었네. 그게 전부였어.",
         "▷ 그런데 왜 넘쳤습니까?",
         "먹였기 때문일세. 일곱 왕이. 자기 왕국이 영원히 기억되길 바라서, 족보와 연대기를 불에 태워 넣었지. '이것도 기억해라, 이것도.' 불은 처음으로 이름의 맛을 보았네. 그 뒤로는 주지 않아도 먹기 시작했고.",
         "연대기에는 '불이 넘쳤다'고만 적혀 있지. 누가 넘치게 했는지는 없네. …이 얘기는 책에서 읽은 걸세. 아주 오래된 책에서.",
         "자네가 든 그것은 불이 맛을 들이기 전날에 떼어 낸 걸세. 그래서 자신을 받아들인 이의 이름을 불타게 두지 않네. 아무 손에나 그러는 게 아닐세. 그 불을 두려워하지 않고 받아들인 손에만. (장갑 낀 손을 내려다본다.) 훔친 불과는 다르지. 나는 받아들인 적이 없네. 움켜쥐었을 뿐.",
     ], items=DUSK)])
more("quest_order_archivist",
     greet=[g("(이사벨이 잉크병 뚜껑을 슬쩍 닫는다.) 아, 조사대원님. 그 창이 가까이 오면 잉크가 들떠서요. 괜찮아요, 싫은 건 아니에요.", chance=0.2, items=DUSK),
            g("조사대원님, 그 창이요. …잠깐만요. 창날에 제 잉크병이 비쳤는데, 잉크가 빛났어요. 방금요.", once=True, items=DUSK)],
     topics=[topic("duskfire", DUSK_TOPIC, [
         "기록관의 잉크에는 불씨의 재를 섞어요. 그래서 불이 못 먹어요. 그런데 왜 재를 섞으면 안 타는지는 아무도 몰랐어요. 그냥 그렇게 배웠어요.",
         "그 창 곁에 잉크병을 두면 재가 가라앉지 않고 떠요. 같은 불이에요. 잉크의 재는 이름을 태우는 불의 재가 아니었어요. 집어 삼키기 전의 불, 그 창에 든 저물녘 빛의 재예요.",
         "첫 기록관들은 그 불 곁에서 썼대요. 연대기 첫 장 여백에 한 줄 있어요. '해 질 녘에 쓰고, 해 질 녘의 불에 말린다.' 저는 그게 일과표인 줄 알았어요.",
         "▷ 그럼 지금 쓰는 재는 어디서 나오죠?",
         "…천 년 전에 긁어모은 걸 아껴 쓰는 거예요. 수도회 창고에 단지가 일곱 개 있었는데, 지금은 반 단지 남았어요. 그게 다 떨어지면 저는 이름을 적어도 지킬 수가 없어요. 그 창이… 유일하게 남은 그 불이에요.",
     ], items=DUSK)])
more("quest_order_captain",
     greet=[g("…또 그걸 들고 왔군. (로란이 잠깐 눈을 감았다 뜬다.) 됐다. 용건이나 말해라.", chance=0.2, items=DUSK),
            g("그 창 좀 치워라. …아니, 됐다. 들고 있어라. 그 옆에 서니 놈들 얼굴이 또렷하다. 이상한 물건이군.", once=True, items=DUSK)],
     topics=[topic("duskfire", DUSK_TOPIC, [
         "나는 불이니 이름이니 하는 건 모른다. 아는 사람을 알 뿐이다.",
         "그런데 이건 안다. 스무 해 동안 놈들 얼굴이 흐려졌다. 토마스 코가 어떻게 생겼는지, 리아가 어느 쪽 눈을 찡긋했는지. 흐려지는 게 제일 무서웠다.",
         "그 창의 저물녘 빛 아래 서니 돌아온다. 전부. 토마스는 코가 삐뚤었다. 리아는 왼쪽이다. …이게 좋은 건지 모르겠다. 잊어야 사는 것도 있으니까.",
         "기록관에게 보여 줘라. 그 아이라면 이걸 어디에 써야 하는지 알 거다.",
     ], items=DUSK)])
more("shop_blacksmith",
     greet=[g("어서 오게. …(브람이 창날 쪽으로 손등을 내밀었다가 거둔다.) 두드려 보고 싶은 걸 참는 중일세. 그건 두드리는 물건이 아니니까.", chance=0.2, items=DUSK),
            g("…그거 이리 줘 보게. 아니, 만지진 않겠네. 보기만. 세상에. 이게 진짜 있었군.", once=True, items=DUSK)],
     topics=[topic("duskfire", DUSK_TOPIC, [
         "이건 벼린 게 아니야. 식힌 거지. 날을 보게, 망치 자국이 하나도 없어. 그리고 이건 쇠가 아니야. 구리도, 금도, 네더라이트도 아니고. 대장장이들 사이에 소문으로만 전해지던 금속일세. 이름조차 없는. 불을 담아도 녹지 않고, 식어도 차가워지지 않는다는. 나는 평생 그게 술자리 허풍인 줄 알았네.",
         "대장장이라면 다 아는 옛말이 있네. 그 시절 왕국에 제련으로는 따를 자가 없던 대장장이가 있었다고. 남들이 녹이지도 못하던 걸 그 사람만 다뤘다고. 그 대장장이가 잿빛 해 전날, 그 이름 없는 금속 안에 불 한 줌을 가뒀다고. 그러고는 창 어디에도 자기 이름을 안 새겼다고. 우리 집안은 그 사람 화로에서 불씨를 얻어다 쓴 곁가지일세.",
         "▷ 왜 자기 이름을 안 새겼죠?",
         "새기면 불이 그 이름부터 삼킬까 봐서라네. 아직 아무것도 삼키지 않던 불인데, 처음 삼키는 게 만든 이의 이름이면 안 되잖나. 그래서 그 대장장이의 이름만은 아무도 몰라. 따를 자가 없었다는 것만 전해지지. 그 사람도 화로의 왕 밑에 있었고, 왕은 대장장이들을 대장간에 둔 채 문을 잠갔다더군.",
         "그 사람이 가둔 불만 밖으로 나왔네. 창은 사람을 가린다더군. 불을 부리려는 손에서는 그냥 무거운 창이고, 불을 받아들인 손에서만 빛난다고. 자네 손에서는 빛나는군. 잠긴 대장간 얘기는 언젠가 그곳을 직접 보게 되면 하세.",
     ], items=DUSK)])
more("shop_fisher",
     greet=[g("오셨소. (오스카가 제 등불 심지를 한 번 더 돋운다.) …그 옆에 두니 내 불이 초라해 보여서 그러오.", chance=0.2, items=DUSK),
            g("그 창… 바람이 이렇게 부는데 불빛이 안 흔들리는군. 내 등불은 밤마다 세 번은 꺼지는데.", once=True, items=DUSK)],
     topics=[topic("duskfire", DUSK_TOPIC, [
         "어부들한테는 옛말이 있소. '저물녘 불은 배를 부른다.' 해 질 녘에 부두에 켜는 불 말이오. 돌아올 사람 보라고.",
         "할아버지 말로는 그게 원래 진짜 불 얘기였다더군. 아주 옛날 부두마다 꺼지지 않는 불이 하나씩 있었는데, 그 불 곁에서 이름을 부르면 바다에 나간 사람이 제 이름을 듣는다고.",
         "그 창에 든 것이 그 불이라면… 한 번만 빌려주시오. 얼음 위에서 이름을 하나 불러 보게. 아니, 아니오. 됐소. 내 등불로 하겠소. 남의 불로 부르면 그 애가 못 알아들을 거요.",
     ], items=DUSK)])
more("gacha",
     greet=[g("어서 와~ (럭키가 창 쪽을 흘끔 보고는 영수증 뭉치를 서랍에 밀어 넣는다.) 아냐, 아무것도! 오늘은 뭘 뽑을래?", chance=0.2, items=DUSK),
            g("앗, 그거! 그게 나왔구나! …저기, 그거 어디서 났는지 묻지 말아 줄래? 나도 몰라서 그래!", once=True, items=DUSK)],
     topics=[topic("duskfire", DUSK_TOPIC, [
         "솔직히 말할게. 그건 내가 들여온 물건이 아니라구. 어느 날 상품 더미 맨 밑에 있었어. 누가 맡기고 갔는데, 얼굴이 기억이 안 나.",
         "얼굴만이 아니야. 목소리도, 키도. 영수증은 있다구. 여기. 근데 글씨가 뒤집혀 있어. 거울에 비춰야 읽혀.",
         "▷ 뭐라고 적혀 있는데?",
         "'불이 다시 굶주리기 전에, 이 불을 받아들일 손에 닿게 하라. 값은 치렀음.' …누가 받아들일지는 내가 정하는 게 아니라 운이 정하는 거라구! 그래서 뽑기에 넣었지. 난 장사꾼이지 심판이 아니거든.",
     ], items=DUSK)])

# 4장: 하빈저의 인쇄지(조사대원이 한 선택을 기계가 셈해 둠), 무리의 노래를 들은 장로
P4 = dict(quests=["story_4_14"])
more("quest_order_archivist", topics=[
    topic("tape", "기계의 인쇄지", ["하빈저가 뱉은 종이요. 천 년 전에 셈한 건데 우리 얘기가 있어요. 날짜까지요.",
                                  "\"불씨를 쥔 자들, 열일곱 번째 배로 도착.\" 맞아요. 하루 씨 장부에 열일곱 척이라고 적혀 있잖아요.",
                                  "그런데 줄마다 끝에 숫자가 있어요. '오차'래요. 어떤 줄은 오차 없음이고, 어떤 줄은 오차가 커요. 조사대원님이 뭔가를 고른 줄이요."], **P4),
    topic("tape_kas_a", "인쇄지: 잿빛 선장", ["\"잿빛 선장, 넘겨짐.\" 기계는 그렇게 셈했어요. 그런데 놓아주셨죠.",
                                           "그 줄 끝에 이렇게 찍혀 있어요. '오차 있음. 다시 셈할 수 없음.' 기계가 틀린 거예요. 조사대원님 때문에요."], flags=["ch1_kas_spared"], **P4),
    topic("tape_kas_b", "인쇄지: 잿빛 선장", ["\"잿빛 선장, 넘겨짐. 오차 없음.\" 이 줄은 맞혔어요.",
                                           "그 다음 줄이 이상해요. '넘겨진 자, 문 안에서 노래함. 듣는 자 있음.' 기계는 그것까지 셈했어요. 누가 듣는지는 안 찍혀 있고요."], flags=["ch1_kas_held"], **P4),
    topic("tape_kael_a", "인쇄지: 태양의 땅", ["\"남은 대장, 죽은 벗에게 진실을 말하지 않음.\" 기계는 그렇게 봤어요. 그런데 말했잖아요.",
                                            "'오차 있음.' 저는 이 두 글자가 이렇게 좋을 줄 몰랐어요."], flags=["ch2_told_truth"], **P4),
    topic("tape_kael_b", "인쇄지: 태양의 땅", ["\"남은 대장, 죽은 벗에게 진실을 말하지 않음. 오차 없음.\"",
                                            "기계가 맞혔다고 그게 틀린 선택은 아니에요. 로란 대장님은 그걸 지고 걷고 있어요. 그것도 걷는 거예요."], flags=["ch2_kept_secret"], **P4),
    topic("tape_lantern_a", "인쇄지: 등불", ["\"어부, 등불을 끄지 못함.\" 그런데 껐죠. 조사대원님이 옆에 있어서요.",
                                           "'오차 있음.' 기계는 곁에 누가 서 있는 건 셈에 못 넣나 봐요."], flags=["ch3_released"], **P4),
    topic("tape_lantern_b", "인쇄지: 등불", ["\"어부, 등불을 끄지 못함. 오차 없음.\" 그런데 그 뒤에 한 줄 더 있어요.",
                                           "'바다가 다 녹는 날, 두 손이 끔.' 두 손이요. 약속하셨다면서요. 기계가 그건 맞히게 두세요."], flags=["ch3_kept_lantern"], **P4),
    topic("tape_last", "인쇄지: 마지막 줄", ["마지막 줄은 장갑 얘기예요. '그 손을 이끄는 것은 장갑이다. 오차 없음.'",
                                           "그런데 그 밑에 한 줄이 반쯤 찍히다 말았어요. '장갑을 벗기는 것은' 까지요. 종이가 거기서 끊겼어요. 기계가 쓰러져서요.",
                                           "…우리가 끊은 거예요. 답을 못 듣게. 아니면, 아직 안 정해져서 못 찍은 걸지도 몰라요."], **P4),
])
more("quest_order_elder", topics=[
    topic("song2", "무리의 노래", ["그 노래를 어디서 들었나. …둘째 절까지.",
                                 "\"마중 나간 누이는 돌아오지 않았다.\" 그래. 돌아오지 않았지. 마중 나갔으니까. 올 사람이 올 때까지는 못 돌아오는 걸세.",
                                 "나는 바다를 잘 안 보네. 눈이 시려서라고 했지. 거짓말일세. 폭풍이 보일까 봐 안 보는 걸세. 폭풍이 나를 알아볼까 봐.",
                                 "더는 묻지 말게. 때가 되면 내가 먼저 말하겠네."], flags=["scylla_song2", "story_ch3_done"], notFlags=["erwin_betrayal"]),
])
more("shop_blacksmith", topics=[
    topic("hundredth", "백 번째 망치걸이", ["이름표 자리만 있고 이름이 없던 그 걸이 말이네. 제일 닳은 걸이였지. 백 명 가운데 망치를 제일 많이 들었다 놨다는 얘기야.",
                                         "문이 잠긴 게 잿빛 해 전날 밤이라네. 그 밤에 백 번째가 문틈으로 뭘 하나 내보냈다는 거지. 불 한 줌 가둔 것을. 받아 든 사람이 그 화로에서 불씨를 얻어다 쓰던 마을 대장장이, 우리 시조고.",
                                         "왜 자기는 안 나왔느냐고? 문틈이 사람 나올 만큼은 안 됐겠지. 아니면 아흔아홉을 두고 못 나왔거나. 나는 뒤쪽이라고 보네. 대장장이가 그래. 같이 쓰던 화로는 못 버려.",
                                         "주문서 뒷면에 서명 대신 찍힌 망치 자국 하나, 봤나. 그게 그 사람 서명이야. 이름을 안 새기는 사람이 남긴 유일한 표시지."], quests=["story_4_04"]),
])

# 종장·숨은 이야기: 앞 장의 선택이 돌아오는 자리
more("quest_order_captain", topics=[
    topic("kas_road", "길을 터 준 무리", ["지옥 가는 길목에 잿빛 무리가 진을 치고 있었다. 싸울 줄 알았다. 놈들이 길을 텄다. 양쪽으로 갈라서서.",
                                        "앞에 선 놈이 그러더라. '빚은 진 쪽이 기억한다.' 카스다. 네가 창고에서 놓아준 그 선장.",
                                        "기록관이 끌려가는 걸 봤다더군. 어느 쪽으로 갔는지도 일러 줬다. …적을 놓아주면 뒤에서 찌른다고 배웠는데. 스무 해 헛배웠다."], flags=["ch1_kas_spared"], quests=["story_5_04"]),
    topic("kas_cell", "빈 방의 새 주인", ["장로가 들어간 방 말이다. 카스가 있던 방. 벽에 그 노래가 아직 긁혀 있다.",
                                        "장로가 밤마다 그걸 읽는다더라. 소리 내서. 둘째 절에서 멈춘다고. 문지기가 그러더라.",
                                        "가둔 놈이 갇혔던 놈의 방에 제 발로 들어갔다. 그걸 뭐라고 불러야 할지 나는 모르겠다. 기록관한테 물어봐라."], flags=["ch1_kas_held", "story_done"]),
])
more("quest_order_archivist", topics=[
    topic("mal_afraid", "서리의 왕에게 한 대답", ["'무섭다, 그래서 이름을 적어 줄 사람 곁에서 싸운다'고 하셨죠. 묘실 문 앞에서요.",
                                             "그 말을 받아 적으면서 손이 떨렸어요. 이름을 적어 줄 사람이 저니까요. 그때는 그게 그렇게 큰일인 줄 몰랐어요.",
                                             "지옥에서 손바닥에 이름을 적을 때 그 말이 생각났어요. 무서웠어요. 그래도 적었어요. 곁에서 싸우는 사람이 있었으니까요."], flags=["ch4_mal_afraid"], quests=["story_5_08"]),
    topic("mal_brave", "서리의 왕에게 한 대답", ["'무섭지 않다, 끝이 있으니까 걷는 거다'라고 하셨죠. 묘실 문 앞에서요.",
                                            "왕이 그 말을 듣고 한참 조용했잖아요. 천 년 동안 끝이 없어서 무서웠던 사람한테, 끝이 있어서 걷는다고 하셨으니까요.",
                                            "장로님도 그 말을 문틀에서 들으셨어요. 끝나지 않는 삶을 판 사람이요. …그 말이 장로님한테 제일 아팠을 거예요."], flags=["ch4_mal_brave"], quests=["story_5_08"]),
    topic("tape_end", "인쇄지의 끊긴 줄", ["하빈저의 인쇄지 마지막 줄이요. '장갑을 벗기는 것은' 하고 끊겼던 거.",
                                         "저는 그 뒤에 조사대나 칼 같은 말이 올 줄 알았어요. 아니었어요. 장갑은 장로님이 벗었어요. 스스로요. 약속했으니까.",
                                         "기계가 못 찍은 게 아니라 찍을 수 없었던 거예요. 사람이 스스로 하는 일은 셈에 안 들어가나 봐요."], quests=["story_5_01"]),
    topic("name_told", "들려준 이름", ["이름을 들려주셨죠. 한 번만, 천천히. 장로님이 따라 하셨어요. 입 모양으로만요. 소리는 안 내고.",
                                      "그러고는 '이제 됐네' 하셨어요. 다시 쥐지 않으셨어요. 듣고도 놓을 수 있는 사람이 된 거예요. 천 년 걸려서요."], flags=["name_told"]),
    topic("name_kept", "들려주지 않은 이름", ["'놓을 수 있게 된 날 직접 읽으라'고 하셨죠. 장로님이 고개를 끄덕이셨어요. 고맙다고요.",
                                           "연대기는 기록고에 펴 놨어요. 그 쪽으로요. 장로님 방 문은 안 잠겨 있고요. 언제 올라오실지는 저도 몰라요. 급하지 않아요."], flags=["name_kept"]),
])
more("quest_order_elder", topics=[
    topic("after", "천 년", ["천 년이 길었느냐고. 길지 않았네. 매일 첫 줄이 같았으니. 하루를 천 년 산 걸세.",
                             "이제 일기 첫 줄을 바꿨네. '오늘은 이사벨이 등불 심지를 갈았다.' 그런 것들을 적네. 적을 게 많더군.",
                             "자네들한테 한 일은 갚을 길이 없네. 그래서 갚겠다는 말은 안 하겠네. 기억하겠네. 그건 내가 잘하는 일이니."], flags=["story_done"]),
    topic("velok", "벨로크 형", ["형은 왼손잡이였네. 그래서 글씨가 뒤집혀 보였지. 거울 글씨는 형이 숨기려고 쓴 게 아닐세. 원래 그렇게 썼네.",
                                "스승님이 그러셨지. '벨로크는 세상을 거울로 보는 아이다. 그러니 남들이 못 보는 쪽을 본다.' 형은 내가 못 견딜 쪽을 본 걸세."], flags=["illusioner_done"]),
])

# 버릇은 본인이 설명하지 않는다: 곁의 사람들이 흘린다 (3.42)
more("quest_order_archivist", topics=[
    topic("habit_roran", "로란 대장님", ["대장님은 누가 마을을 나설 때마다 사람을 세요. 나갈 때 한 번, 돌아와서 한 번. 처음엔 깐깐한 분인 줄 알았어요.",
                                        "신참 신발 끈도 세 번씩 보세요. 제 것도요. 저는 그걸 연대기 여백에 '잔소리'라고 적어 놨는데… 지워야 할 것 같아요. 까닭이 있더라고요."], flags=["order_apprentice"]),
    topic("habit_erwin", "장로님", ["장로님은 식당에 안 오세요. 뜨거운 걸 못 드신대요. 차도 다 식혀서 드세요.",
                                   "그리고 장갑요. 글씨를 가르쳐 주실 때도, 한여름에도 끼고 계셨어요. 어릴 땐 어른들은 다 그런 줄 알았어요."], flags=["order_apprentice"], notFlags=["erwin_betrayal"]),
])
more("quest_order_captain", topics=[
    topic("habit_isabel", "기록관", ["그 아이는 종이가 없으면 손등에 적는다. 걸으면서도 적는다. 제 어미도 그랬다. 엘레나는 팔뚝까지 적었지.",
                                    "그리고 뭘 찾으면 귀퉁이에 점을 찍는다. 본인은 모르는 버릇이다. 누구한테 배웠는지 나는 안다."], flags=["order_apprentice"]),
    topic("habit_oscar", "어부 오스카", ["그 양반은 해가 지면 말을 하다가도 일어난다. 얼음에 나갈 시간이라고. 열다섯 해째다.",
                                       "뜨거운 건 안 먹는다. 식당 주인이 속상해하지. 나는 까닭을 안 묻는다. 나도 치우지 않는 침상이 열한 개 있으니까."], flags=["order_apprentice"], notFlags=["story_ch3_done"]),
])
more("shop_blacksmith", topics=[
    topic("habit_erwin", "장로의 장갑", ["장로 양반 장갑을 한 번 수선한 적이 있네. 손가락 끝이 해져서. 그런데 안쪽이 그을려 있더군. 바깥은 멀쩡한데.",
                                       "물어보니 '젊을 때 불에 덴 자리가 볕을 싫어한다'더군. 그런가 보다 했지. 대장장이는 덴 자리 얘기는 안 캐묻네."], flags=["order_apprentice"], notFlags=["erwin_betrayal"]),
    topic("habit_felix", "지도쟁이 펠릭스", ["펠릭스는 걷다가 서서 지도를 고치네. 길이 열다섯 해 사이에 바뀌었다면서. 실은 제 기억이 맞는지 확인하는 걸세.",
                                          "그 친구는 지하실에서 열다섯 해를 살았어. 숟가락 쥐는 법도 잊었었지. 그러니 좀 느려도 봐주게."], flags=["ch1_three_ends"]),
])
more("shop_fisher", topics=[
    topic("habit_bram", "대장장이 브람", ["브람은 쇠만 보면 손등으로 두드려 보오. 남의 집 문고리도. 속이 찼는지 듣는 거라오.",
                                        "그 양반이 내 등불 놋쇠를 해마다 갈아 주오. 값을 안 받아서 생선을 두고 오지. 그러면 다음 날 생선 가시가 문 앞에 가지런히 놓여 있소. 잘 먹었다는 거요."], flags=["order_apprentice"]),
])
more("quest_guide", topics=[
    topic("habit_haru", "서기 하루 씨", ["하루 씨는 누가 한 말을 꼭 한 번 되받아 말해요. '그러니까 이러이러하다는 말씀입니까' 하고요. 받아 적는 사람 버릇이래요.",
                                       "그리고 무슨 일이든 '기록이 있습니다'로 시작해요. 없을 때는 '기록이 없습니다'로 시작하고요."], flags=["order_apprentice"]),
])

# 3.60: 고른 뒤에 아무 말도 돌아오지 않던 선택 둘의 뒷이야기 (1장 마녀의 잉크, 숨은 이야기 「마중」의 대답)
more("quest_order_archivist", topics=[
    tb("ch1_witch_paid", "마녀의 잉크 값", "The price of the witch's ink", [
        b("늪의 마녀에게 값을 치르고 받아 오셨죠. 그래서 꼬리표를 한 장 써서 보냈어요. '한 단지 값 치름. 조사대.' 어머니 꼬리표 옆에 붙여 달라고요.",
          "You paid the witch in the swamp for it. So I wrote out a tag and sent it to her. 'One jar, paid for. The Survey Corps.' I asked her to hang it beside Mother's."),
        b("열다섯 해 전에도 지금도, 그 선반 앞에서 우리는 손님이었던 거예요. 저는 그게 좋아요. 연대기에는 그런 것도 적거든요. 누가 값을 치렀는지.",
          "Fifteen years ago and now, we stood at that shelf as customers. I like that. The Chronicle keeps that kind of thing too: who paid what they owed."),
    ], flags=["ch1_witch_trade"]),
    tb("ch1_witch_owed", "마녀의 잉크 값", "The price of the witch's ink", [
        b("늪에서는 마녀가 먼저 병을 던졌다고 하셨죠. 다친 데가 없어서 다행이에요. 그런데 잉크는 받아 왔고, 값은 못 치렀잖아요.",
          "You said the witch threw the first bottle, out in the swamp. I'm glad you weren't hurt. But we came home with the ink, and we never paid for it."),
        b("그래서 장부에 적어 뒀어요. '마녀의 잉크 한 단지. 외상.' 어머니는 값을 치르고 가셨거든요. 같은 선반에 우리 이름만 빚으로 남는 건 싫어요.",
          "So I put it in the ledger. 'One jar of the witch's ink. Owed.' Mother paid before she left. I don't want ours to be the only name on that shelf left as a debt."),
        b("다음 보급 편에 먹물 주머니를 실어 보낼게요. 받을지는 모르겠어요. 그래도 빚은 진 쪽이 기억하는 거니까요.",
          "I'll send ink sacs out with the next supply run. I don't know if she'll take them. Still, a debt is for the one who owes it to remember."),
    ], flags=["ch1_witch_fight"]),
])
more("shop_fisher", topics=[
    tb("scylla_answer_told", "제독에게 한 대답", "The answer you gave the Admiral", [
        b("기록관한테 들었소. 그분께 사실대로 말했다면서. 그 사람은 허공에 있고, 제 발로 간 길이라고.",
          "The archivist told me. You gave her the truth, she says. That he is in the Void, and that he walked there on his own feet."),
        b("기다리는 사람한테 제일 무서운 말은 '안 온다'가 아니오. '너를 잊었다'요. 그게 아니라고 말해 준 거 아니오. 잊은 건 세상이지, 그 사람이 아니라고.",
          "The worst thing you can tell someone who waits isn't 'he's not coming.' It's 'he forgot you.' And you told her it wasn't so. That the world forgot, not him."),
        b("나도 얼음 앞에서 그 말을 기다려 본 사람이오. 그분이 웃었다니, 그걸로 됐소.",
          "I stood in front of the ice waiting to hear that myself. She smiled, they say. That will do."),
    ], flags=["scylla_told"]),
    tb("scylla_answer_lantern", "제독 앞에 둔 등불", "The lantern you left with the Admiral", [
        b("내 등불을 그분 앞에 두고 왔다면서. 아무 말도 안 하고.",
          "I hear you set my lantern down in front of her. And said nothing at all."),
        b("잘했소. 등불은 말보다 먼저 닿소. '여기 있다, 돌아오라'는 뜻도 되고, '이제 됐다, 가도 된다'는 뜻도 되지. 어느 쪽인지는 받는 사람이 정하는 거요.",
          "You did right. A lantern gets there before words do. It can mean 'here I am, come back.' It can mean 'that's enough now, you can go.' Which one it is, the one who receives it decides."),
        b("돌려받을 생각은 없소. 그건 원래 마중 나가는 사람 물건이오. 이제야 임자를 찾아간 거요.",
          "I don't want it back. A lantern like that belongs to whoever goes out to meet someone. It has only now found its owner."),
    ], flags=["scylla_lantern"]),
])

# 3.62: 조연의 말투와 지나온 일 (이사벨·에르윈 말고도 사람이 보이게)
more("attendance", topics=[
    tb("haru_first_day", "서기의 첫날", "The clerk's first day", [
        b("저는 열아홉에 서기가 됐습니다. 첫날 받아 적은 것이 제 아버지의 사망 신고였습니다. 신고하러 오신 분이 어머니였습니다.",
          "I became a clerk at nineteen. The first thing I took down, on my first day, was the notice of my own father's death. The one who came to file it was my mother."),
        b("어머니는 날짜를 하루 틀리게 말씀하셨습니다. 저는 들은 대로 적었습니다. 그 장부는 지금도 하루가 틀린 채로 있습니다. 고치려면 신고한 사람이 다시 와야 하는데, 어머니도 이제 안 계십니다.",
          "My mother gave the date wrong by one day. I wrote what I heard. That ledger is still wrong by one day. To correct it the person who filed has to come back, and my mother is gone now too."),
        b("그날부터 저는 들은 말을 한 번 되받아 말합니다. '그러니까 이러이러하다는 말씀입니까' 하고요. 귀찮아하시는 분이 많습니다. 그래도 합니다.",
          "Since that day I say back whatever I am told, once. 'So what you mean is this and this?' Many people find it tiresome. I do it anyway."),
    ], flags=["order_apprentice"]),
])
more("quest_bounty", topics=[
    tb("garret_eye", "안대", "The eyepatch", [
        b("이 눈 말이오. 짐승한테 잃은 게 아니오. 현상금 벽보를 믿다가 잃었소.",
          "This eye. I didn't lose it to a beast. I lost it trusting a bounty notice."),
        b("스물둘에 벽보 한 장을 뜯었소. '가축 도둑, 왼뺨에 흉터.' 사흘을 쫓아 잡았는데 흉터가 오른뺨에 있더이다. 벽보 쓴 서기가 마주 보고 적은 거요. 엉뚱한 사람이었소. 그 사람이 휘두른 낫이 이 눈이오.",
          "At twenty-two I tore down a notice. 'Cattle thief, scar on the left cheek.' I tracked him three days and caught him, and the scar was on the right. The clerk who wrote it had been facing the man. It was the wrong fellow. The sickle he swung is this eye."),
        b("그래서 내 게시판의 벽보는 내가 직접 보고 온 것만 붙소. 느리다고들 하오. 느려도 되오. 틀린 벽보는 사람을 둘 잡소. 쫓기는 쪽 하나, 쫓는 쪽 하나.",
          "So the only notices on my board are ones I went and saw for myself. They say I'm slow. Slow will do. A wrong notice takes two people. The one hunted, and the one hunting."),
    ]),
])
more("shop_restaurant", topics=[
    tb("lorenzo_word", "벨리시모", "Bellissimo", [
        b("'벨리시모'요? 우리 할머니 말이에요. 바다 건너에서 시집오셨는데, 이쪽 말을 끝까지 다 못 배우셨어요. 맛있을 때 하는 말은 그것 하나였죠.",
          "'Bellissimo'? That's my grandmother's word. She married in from across the sea and never did learn all of our language. She had exactly one thing she said when food was good."),
        b("그런데 그게 맛있다는 뜻이 아니래요. 나중에 뱃사람한테 들었어요. '참 곱다'는 뜻이라고. 할머니는 평생 음식을 보고 곱다고 하신 거예요.",
          "Only it doesn't mean tasty. A sailor told me, years later. It means 'how lovely.' All her life my grandmother looked at food and called it lovely."),
        b("그래서 저는 접시를 내기 전에 한 번 봐요. 맛은 손님이 정하지만, 고운 건 내가 정할 수 있으니까요. 자, 오늘 것도… 벨리시모!",
          "So I look at every plate once before it goes out. The guest decides whether it's tasty, but lovely is something I can decide. There, today's too… bellissimo!"),
    ]),
])

data = {"version": VERSION,
        "_help": "NPC 대화. npcs 의 키는 NPC 키(shop_상점ID, quest_NPCID, bank, job, attendance, gacha). greet: 인사 (조건이 맞는 것 중 마지막), "
                 "chatter: 일상 이야기 (무작위 한 마디), topics: 이야기 주제 (need: flags·notFlags·quests 조건). 문장은 \"이름: 대사\" 로 말하는 사람을 바꿀 수 있고, "
                 "{\"ko\": \"...\", \"en\": \"...\"} 로 두 언어를 적을 수 있습니다. 직접 고쳤다면 \"edited\": true 로 두세요 (모드 업데이트 때 덮어쓰지 않습니다).",
        "edited": False,
        "npcs": N}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump(data, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(N), "npcs")
