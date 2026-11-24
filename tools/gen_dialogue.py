# NPC 대화 기본값: python3 tools/gen_dialogue.py → src/main/resources/rtshop_defaults/dialogue.json
# 말투는 NPC 마다 하나로 고정한다 (아래 STYLE 참고). 줄 앞에 "이름: " 을 붙이면 말하는 사람이 바뀐다.
import json, os

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'rtshop_defaults', 'dialogue.json')
VERSION = 5

# 말투
#  에르윈(장로)   하게체  "~하게, ~네, ~일세"
#  이사벨(기록관) 해요체  "~해요, ~예요"
#  로란(탐험대장) 해라체  "~다, ~라, ~냐"
#  모리스(안내인) 해요체, 친절
#  한나(농부)     해요체, 밝음
#  오스카(어부)   하게체 섞인 뱃사람 말투 "~다네, ~지"
#  마르타(식품)   해요체, 다정
#  브람(대장장이) 해라체, 짧고 무뚝뚝
#  피터(잡화)     해요체, 수다스러운 장사꾼 "손님"
#  에드먼드(토지) 합쇼체 "~습니다"
#  세라(레이드)   해요체, 차분
#  루시(티켓)     해요체, 발랄 "~요!"
#  엘리(출석 상점) 해요체, 상냥
#  그레고리(은행) 합쇼체, 격식
#  알버트(직업)   해요체, 차분한 스승
#  하루(시청 서기) 합쇼체, 꼼꼼
#  럭키(뽑기)     반말 "~라구, ~지!"
#  로렌초(식당)   해요체, 요리사의 감탄 "벨리시모!"

def need(flags=None, quests=None, notFlags=None, items=None):
    n = {}
    if items: n["items"] = items  # 이 물건을 지니고 있을 때만 (가방·손·갑옷)
    if flags: n["flags"] = flags
    if quests: n["quests"] = quests
    if notFlags: n["notFlags"] = notFlags
    return {"need": n}

def g(text, **kw):
    o = {"text": text}
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
        g("…돌아왔군. 이 늙은이의 자리는 비었네. 자네가 보는 건 남은 그림자일 뿐일세.", flags=["erwin_betrayal"]),
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
            "천 년 전, 일곱 세력이 세계의 힘을 '눈'에 담아 봉인했지. 그 봉인을 지켜보는 것이 우리 일이네.",
            "…지금은 나와 기록관, 탐험대장 셋뿐이지만 말일세.",
        ]),
        topic("chronicle", "잿빛 연대기에 대해", [
            "연대기 말인가. 수도회가 대대로 적어 온 기록일세.",
            "찢긴 장이 있다고? …오래된 책이니 그럴 수도 있겠지.",
            "기록관이 너무 깊이 파고들지 않았으면 좋겠군. 진실은 때로 사람을 태우네.",
        ], flags=["order_apprentice"]),
        topic("ember", "꺼지지 않는 불씨에 대해", [
            "그 불씨를 꿈에서 받았다고?",
            "…흥미롭군. 아주 흥미로워. 그 불씨를 잘 간직하게. 절대 남에게 주지 말고.",
            "특히… 아니, 아무것도 아닐세.",
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
        g("견습님! 새로 알아낸 게 있으면 꼭 저한테 먼저 말해 주세요.", flags=["order_apprentice"]),
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
        topic("seven", "일곱 세력과 눈", [
            "일곱 세력은 각자 세계의 힘 하나씩을 '눈'에 담았어요.",
            "대장간의 불, 사막의 모래, 기계의 끝, 심연의 바다, 공허의 왕좌, 서리의 저주… 그리고 최초의 불꽃.",
            "그 눈들이 봉인의 열쇠예요. 눈을 모으면 봉인을 열 수도, 더 단단히 잠글 수도 있죠.",
        ], flags=["order_apprentice"]),
        topic("eighth", "여덟 번째 봉인", [
            "통비님이 '봉인은 여덟'이라고 했다면서요.",
            "연대기에는 일곱뿐이에요. 그런데 목차를 보면… 여덟 번째 장이 있었던 흔적이 있어요.",
            "누군가 그 장을 통째로 지웠어요. 그리고 그 사람은 지우는 데 익숙했던 것 같아요.",
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
        g("수호자. …그 호칭이 아직 어색하군. 그래도 네가 해냈다.", flags=["story_done"]),
    ],
    "chatter": [
        "지도는 발로 그리는 거다. 책상에서 그린 지도는 사람을 죽인다.",
        "원정대에 필요한 건 세 가지다. 식량, 칼, 그리고 돌아올 이유.",
        "북쪽 바다엔 가 봤냐? 숨 쉴 때마다 폐가 어는 곳이다.",
        "부상? 이건 사막에서 모래벌레한테 물린 거다. 멋지지?",
        g("장로가… 아니, 그 녀석 이야기는 하지 마라. 아직 화가 안 풀렸다.", flags=["erwin_betrayal"]),
    ],
    "topics": [
        topic("expedition", "원정대에 대해", [
            "수도회 원정대는 봉인이 흔들릴 때만 움직인다.",
            "마지막 원정은 이십 년 전이었다. 그때 돌아온 건 나 하나였다.",
            "그러니 너는 꼭 살아 돌아와라. 명령이다.",
        ], flags=["story_ch1_done"]),
        topic("guardians", "깨어난 수호자들", [
            "녹슨 기사, 얼음 거인, 산꼭대기 석공, 태양의 사제.",
            "놈들은 원래 봉인을 지키는 쪽이었다. 그런데 불이 깨어나자 미쳐 버렸지.",
            "아니면… 처음부터 누군가를 기다리고 있었거나.",
        ], flags=["story_ch1_done"]),
        topic("sea", "얼어붙은 바다", [
            "얼음 위에서 싸워라. 물에 빠지면 갑옷이 널 끌어내린다.",
            "유령선 선장 코르넬리아. 천 년 동안 같은 항해를 반복하는 여자다.",
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
        topic("city", "골디아에 대해", [
            "골디아는 폭풍을 피해 온 사람들이 세운 도시예요.",
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
merchant("shop_fisher", ["물고기 팔러 왔나? 무게 좋은 놈이면 값을 더 쳐 주지."], [
    "바다는 정직하다네. 기다린 만큼 준다네.",
    "황금빛 물고기를 낚으면 꼭 나한테 가져오게. 그건 행운이라네.",
    "그물 깁는 것도 일이지. 손가락이 성할 날이 없어.",
    g("북쪽 바다에서 물고기가 사라지고 있다네. 사십 년 바다 생활에 이런 건 처음이지.", flags=["order_apprentice"]),
    g("바다가 얼어붙었다고? …역시 그랬군. 물고기들이 먼저 알고 도망친 거라네.", flags=["story_ch2_done"]),
], [
    topic("fish_gone", "사라지는 물고기", [
        "처음엔 북쪽 대구가 줄더니, 다음엔 연어, 이젠 오징어까지 안 보인다네.",
        "물고기는 바다가 아플 때 제일 먼저 떠나지. 바다가 아프다는 거라네.",
    ], flags=["order_apprentice"]),
    topic("ghost_ship", "유령선 이야기", [
        "어부들 사이에 도는 이야기가 있지. 안개 낀 밤엔 얼음 위로 배가 지나간다네.",
        "돛은 찢어졌는데 바람을 거슬러 간다지. 선장은 아직도 선원들 이름을 부른다고.",
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
merchant("shop_blacksmith", ["뭘 찾냐. 칼이냐, 갑옷이냐."], [
    "좋은 칼은 손에 붙는다. 나쁜 칼은 손을 문다.",
    "강화는 운이다. 근데 운도 준비한 놈한테 온다.",
    "불 앞에 삼십 년. 덕분에 눈썹이 없다.",
    g("희귀 장비 말이냐? 원정대에 들어간 놈한테만 판다. 규칙이다.", notFlags=["story_ch1_done"]),
], [
    topic("forge", "대장간 이야기", [
        "쇠는 거짓말을 안 한다. 두드린 만큼 단단해진다.",
        "강화하다 깨진 칼? 다시 녹이면 된다. 사람도 마찬가지다.",
    ]),
    topic("old_knight", "녹슨 기사의 문장", [
        "그 투구 문장, 나도 봤다. 불꽃 기사단 문장이다.",
        "우리 할아버지의 할아버지가 그 기사단 칼을 벼렸다고 했지. 전설인 줄 알았다.",
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
    "땅은 사라지지 않습니다. 가장 안전한 재산이지요.",
    "성 안의 땅은 보호받습니다. 대신 값이 조금 나갑니다.",
    "이웃을 잘 고르는 것도 땅을 사는 일입니다.",
], [
    topic("castle", "성 영역에 대해", [
        "성 영역은 골디아 시청이 관리하는 보호 구역입니다.",
        "사신 땅 안에서만 짓고 부술 수 있습니다. 그 밖은 모두의 것이니까요.",
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
        "언젠가 골디아 모든 사람에게 제 요리를 먹이는 게 꿈이에요.",
        "원정대에도 도시락을 싸 드리고 싶어요. 배고픈 영웅은 슬프잖아요!",
    ]),
])
N["bank"] = {"greet": ["골디아 은행 그레고리입니다. 무엇을 도와드릴까요?"], "chatter": [
    "예금은 안전합니다. 쓰러지셔도 은행의 돈은 사라지지 않습니다.",
    "지갑의 돈은 위험에 노출됩니다. 큰돈은 맡겨 두시기를 권합니다.",
    "숫자는 정직합니다. 저는 그 점이 좋습니다.",
], "topics": [
    topic("history", "은행의 역사", [
        "골디아 은행은 폭풍 이후 첫 번째로 세워진 건물입니다.",
        "사람들이 서로를 믿게 하려면, 먼저 돈을 믿게 해야 했지요.",
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
    "골디아 인구가 늘고 있습니다. 서류도 늘고 있습니다.",
], "topics": [
    topic("records", "시청 기록", [
        "시청에는 폭풍 이후 모든 이주민의 이름이 있습니다.",
        "단 한 사람, 수도회 장로의 이주 기록만 없습니다. 처음부터 여기 계셨다고 하더군요.",
    ], flags=["clue_grey_cloak"]),
]}
N["gacha"] = {"greet": ["행운 상인 럭키라구! 오늘은 뭐가 나올까~?"], "chatter": [
    "전설은 기다리는 자에게 온다지! 아마도!",
    "YAMATO? 그거 본 사람은 손에 꼽는다구. 근데 너라면 혹시?",
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
         topic("feigned", "그때의 대답", ["에르윈 앞에서 제안을 받아들이는 척했다면서요. 덕분에 원정대가 모일 시간을 벌었어요.",
                                       "…그래도 다음엔 그런 연기는 하지 마세요. 심장이 멎는 줄 알았어요."], flags=["ch4_feigned"]),
         topic("refused", "그때의 대답", ["\"불을 지키고, 봉인을 지킨다.\" 에르윈 앞에서 그렇게 말했다면서요.",
                                       "어머니가 들었으면 좋아했을 거예요."], flags=["ch4_refused"]),
         topic("forgave", "연대기의 마지막 장", ["에르윈의 이름을 연대기에 남겼어요. 당신의 뜻대로요.",
                                             "천 년 동안 스승을 기다린 사람. 나쁜 사람이었지만… 혼자이긴 했겠죠."], flags=["fin_forgave"]),
         topic("erased", "연대기의 마지막 장", ["에르윈의 이름은 적지 않았어요. 그가 지운 이름들 자리에, 어머니의 이름을 적었어요.",
                                            "엘레나. 기록관 엘레나."], flags=["fin_erased"]),
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
     greet=[g("자네군. 그물을 다시 던졌다네. 바다가… 다시 정직해졌어.", flags=["story_ch3_done"])],
     topics=[
         topic("released", "엘릭", ["엘릭을 보냈다네. 등불을 여니 웃음소리가 바다 쪽으로 멀어지더군.",
                                   "열다섯 해 동안 켠 등불을 이제 껐다네. 이상하게 마음이 따뜻하군."], flags=["ch3_released"]),
         topic("kept", "엘릭", ["등불은 아직 내 곁에 있다네. 밤이면 그 안에서 웃음소리가 들리지.",
                               "언젠가는 보내 줘야겠지. …내가 그물을 못 던지는 날이 오면, 그때 같이 가겠네."], flags=["ch3_kept_lantern"]),
         topic("elric", "동생 엘릭", ["엘릭은 나보다 그물을 잘 던졌다네. 웃음소리가 컸고.",
                                    "코넬리아 선장의 배에 오를 때 그랬지. '형, 돌아오면 큰 놈 잡아 줄게.'"], quests=["story_3_01"]),
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
            b("나도 한때는 원정대에 들어가고 싶었다. 로란이 사람을 고를 때 말이다.", "I once wanted to join the expedition too. Back when Loran was choosing people."),
            b("떨어졌다. 그 덕에 살아 있지. …돌아오지 못한 열두 명 중에 내 동기가 둘 있었다.", "I wasn't picked. That's why I'm alive. …Two of my classmates were among the twelve who never came back."),
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
            b("오스카 영감님 동생 얘기는 들었다. 그 집 등불, 마을에서 제일 오래 켜져 있었지.", "I heard about old Oscar's brother. That lantern of his burned longer than any in town."),
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
            b("로란이 명패를 받을 때마다 나도 이름을 외운다.", "Every time Loran takes back a name plate, I memorize the name too."),
            b("열두 개. …다 모이면 성벽에 새길 거다.", "Twelve. …When they're all back, I'll carve them into the wall."),
        ], flags=["story_ch2_done"]),
    ],
}

N["quest_hunter"] = {
    "greet": [
        b("쉿, 바람 소리를 들어 봐. …응, 이제 말해도 돼. 무슨 일이야?", "Shh, listen to the wind. …Okay, you can talk now. What's up?"),
        gb("견습이 됐구나! 수도회 사람들은 숲을 잘 안 오던데, 너는 자주 와.", "So you're an apprentice now! Order people rarely come to the woods, but you do.", flags=["order_apprentice"]),
        gb("폭풍이 그쳤어. 새들이 돌아왔더라. 네 덕이지?", "The storms stopped. The birds came back. That was you, wasn't it?", flags=["story_done"]),
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
        gb("이그니스의 불꽃 속에 회색 연기가 섞여 있었다고? …그건 불의 것이 아니란다.", "Grey smoke mixed into Ignis's flames? …That doesn't belong to fire, dear.", flags=["story_ch4_done"]),
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
    "연대기에는 일곱 왕이 나와요. 그런데 왕관 그림은 여덟 개예요. 한 개는 번개 모양이고요.",
])
more("quest_order_captain", hints=[
    "남쪽 따뜻한 바다엔 가라앉은 유적이 많다. 거기서 붓질을 하던 놈이 이상한 악보를 주웠다더군. 제목만 남은.",
    "폭풍이 칠 때 높은 곳에 올라가 본 적 있나? 바람이 종이를 날려 온다. 대개는 쓰레기지만… 대개는.",
    "원정 중에 소환사를 셋 잡은 적이 있다. 셋 다 보랏빛 잉크병을 품고 있었지. 습격 때만 나오는 놈들이다.",
])
more("shop_fisher", hints=[
    "따뜻한 바다에서 낚시를 하다 보면 소금에 절은 종이가 걸려 올라올 때가 있지. 대개는 버리지만, 한 번은 '여덟 번째 배'라고 적혀 있었네.",
    "뇌우 치는 밤엔 드라운드들이 이상한 비늘을 떨군다는 얘기가 있어. 아직도 찌릿하다던가.",
])
more("attendance", hints=[
    "시청 기록에 이상한 항목이 하나 있습니다. '폭풍의 여제께 바치는 세금, 미납.' 날짜는 천 년 전입니다.",
    "숲속 저택 주변의 안개 민원이 들어온 적이 있습니다. 담당자는 '거울이 사람을 비추지 않는다'고 적고 조사를 그만뒀습니다.",
])
more("gacha", hints=[
    "있잖아, 진짜 행운은 뽑기에만 있는 게 아니라구. 폭풍 치는 날 하늘에서 떨어지는 종이 같은 거? 아무것도 아니야~",
    "보랏빛 잉크로 쓴 복권은 절대 사지 마! 당첨 번호가 거울처럼 뒤집혀 있다구!",
])
more("quest_order_elder", hints=[
    "…여덟. 아니, 일곱이라고 했나? 나이를 먹으니 숫자가 헷갈리는군.",
])
for key in ("shop_general", "shop_blacksmith", "bank"):
    if key in N:
        more(key, hints=["바다 위 신전 얘기 들어 봤나? 폭풍이 그치지 않는 곳에 있다던데. 신전 기둥이 여덟 개라나."])


# ================================================================ 황혼의 불꽃 (Duskfire, efn:meen_spear): 지니고 있으면 몇몇 사람의 말이 달라진다
# 설정: 최초의 불꽃은 처음부터 이름을 태우던 불이 아니었다. 모든 것을 집어 삼키기 이전, 기억을 유지하던 때가 있었고, 그 불빛을 '황혼의 불꽃'이라 불렀다.
#   일곱 왕이 자기 왕국이 영원히 기억되길 바라 불에 기록(족보·연대기)을 태워 넣기 시작했고, 불은 이름의 맛을 들였다. 그것이 잿빛 해의 시작이다.
#   황혼의 불꽃은 불이 모든 것을 집어 삼키기 전날, 당시 왕국 최고의 제련 기술을 가진 대장장이가 소문으로만 들려오던 의문의 금속 안에 불 한 줌을 '식혀' 가둔 창이다.
#   자신을 받아들인 사용자의 이름을 불타게 두지 않는다.
DUSK = ["efn:meen_spear"]
more("quest_order_elder",
     greet=[g("…그 창. (장로가 한 걸음 물러선다. 장갑 낀 손을 등 뒤로 감춘다.) 황혼의 불꽃이군. 그걸 어디서 얻었나.", items=DUSK)],
     topics=[topic("duskfire", "황혼의 불꽃", [
         "가까이 대지 말게. …아니, 위험해서가 아닐세. 내 쪽이 부끄러워서 그러네.",
         "그 불은 모든 것을 집어 삼키기 이전, 기억을 유지하던 때의 불일세. 최초의 불꽃은 처음부터 이름을 태우지 않았네. 그저 기억했지. 황혼의 빛으로. 사람들은 그 곁에 앉아 죽은 이의 이름을 불렀고, 불은 들었네. 그게 전부였어.",
         "▷ 그런데 왜 넘쳤습니까?",
         "먹였기 때문일세. 일곱 왕이. 자기 왕국이 영원히 기억되길 바라서, 족보와 연대기를 불에 태워 넣었지. '이것도 기억해라, 이것도.' 불은 처음으로 이름의 맛을 보았네. 그 뒤로는 주지 않아도 먹기 시작했고.",
         "연대기에는 '불이 넘쳤다'고만 적혀 있지. 누가 넘치게 했는지는 없네. …이 얘기는 책에서 읽은 걸세. 아주 오래된 책에서.",
         "그 창은 불이 맛을 들이기 전날에 떼어 낸 걸세. 그래서 자신을 받아들인 사용자의 이름을 불타게 두지 않네. 아무 손에나 그러는 게 아닐세. 그 불을 두려워하지 않고 받아들인 손에만. (장갑 낀 손을 내려다본다.) 훔친 불과는 다르지. 나는 받아들인 적이 없네. 움켜쥐었을 뿐.",
     ], items=DUSK)])
more("quest_order_archivist",
     greet=[g("조사대원님, 그 창이요. …잠깐만요. 창날에 제 잉크병이 비쳤는데, 잉크가 빛났어요. 방금요.", items=DUSK)],
     topics=[topic("duskfire", "황혼의 불꽃", [
         "기록관의 잉크에는 불씨의 재를 섞어요. 그래서 불이 못 먹어요. 그런데 왜 재를 섞으면 안 타는지는 아무도 몰랐어요. 그냥 그렇게 배웠어요.",
         "그 창 곁에 잉크병을 두면 재가 가라앉지 않고 떠요. 같은 불이에요. 잉크의 재는 이름을 태우는 불의 재가 아니었어요. 집어 삼키기 전의 불, 그 황혼의 불꽃의 재예요.",
         "첫 기록관들은 그 불 곁에서 썼대요. 연대기 첫 장 여백에 한 줄 있어요. '황혼에 쓰고, 황혼의 불꽃에 말린다.' 저는 그게 일과표인 줄 알았어요.",
         "▷ 그럼 지금 쓰는 재는 어디서 나오죠?",
         "…천 년 전에 긁어모은 걸 아껴 쓰는 거예요. 수도회 창고에 단지가 일곱 개 있었는데, 지금은 반 단지 남았어요. 그게 다 떨어지면 저는 이름을 적어도 지킬 수가 없어요. 그 창이… 유일하게 남은 그 불이에요.",
     ], items=DUSK)])
more("quest_order_captain",
     greet=[g("그 창 좀 치워라. …아니, 됐다. 들고 있어라. 그 옆에 서니 놈들 얼굴이 또렷하다. 이상한 물건이군.", items=DUSK)],
     topics=[topic("duskfire", "황혼의 불꽃", [
         "나는 불이니 이름이니 하는 건 모른다. 아는 사람을 알 뿐이다.",
         "그런데 이건 안다. 스무 해 동안 놈들 얼굴이 흐려졌다. 토마스 코가 어떻게 생겼는지, 리아가 어느 쪽 눈을 찡긋했는지. 흐려지는 게 제일 무서웠다.",
         "그 창의 황혼빛 아래 서니 돌아온다. 전부. 토마스는 코가 삐뚤었다. 리아는 왼쪽이다. …이게 좋은 건지 모르겠다. 잊어야 사는 것도 있으니까.",
         "기록관에게 보여 줘라. 그 아이라면 이걸 어디에 써야 하는지 알 거다.",
     ], items=DUSK)])
more("shop_blacksmith",
     greet=[g("…그거 이리 줘 보게. 아니, 만지진 않겠네. 보기만. 세상에. 이게 진짜 있었군.", items=DUSK)],
     topics=[topic("duskfire", "황혼의 불꽃", [
         "이건 벼린 게 아니야. 식힌 거지. 날을 보게, 망치 자국이 하나도 없어. 그리고 이건 쇠가 아니야. 구리도, 금도, 네더라이트도 아니고. 대장장이들 사이에 소문으로만 돌던 금속일세. 불을 담아도 녹지 않고, 식어도 차가워지지 않는다는. 나는 평생 그게 술자리 허풍인 줄 알았네.",
         "대장장이라면 다 아는 옛말이 있네. 그 시절 왕국에서 제련 솜씨가 으뜸이던 대장장이가 있었다고. 남들이 녹이지도 못하던 걸 그 사람만 다뤘다고. 그 대장장이가 잿빛 해 전날, 그 의문의 금속 안에 불 한 줌을 가뒀다고. 그러고는 이름을 안 새겼다고. 우리 집안은 그 사람 화로에서 불씨를 얻어다 쓴 곁가지일세.",
         "▷ 왜 이름을 안 새겼죠?",
         "새기면 불이 그 이름부터 삼킬까 봐서라네. 아직 아무것도 삼키지 않던 불인데, 처음 삼키는 게 제 이름이면 안 되잖나. 그래서 만든 사람이 누군지 아무도 몰라. 왕국 최고였다는 것만 알지. 그 사람도 화로의 왕 밑에 있었고, 왕은 대장장이들을 대장간에 둔 채 문을 잠갔다더군.",
         "그 사람이 가둔 불만 밖으로 나왔네. 창은 사람을 가린다더군. 불을 부리려는 손에서는 그냥 무거운 창이고, 불을 받아들인 손에서만 빛난다고. 자네 손에서는 빛나는군. 잠긴 대장간 얘기는 언젠가 그곳을 직접 보게 되면 하세.",
     ], items=DUSK)])
more("shop_fisher",
     greet=[g("그 창… 바람이 이렇게 부는데 불빛이 안 흔들리는군. 내 등불은 밤마다 세 번은 꺼지는데.", items=DUSK)],
     topics=[topic("duskfire", "황혼의 불꽃", [
         "어부들한테는 옛말이 있소. '황혼의 불꽃은 배를 부른다.' 해 질 녘에 부두에 켜는 불 말이오. 돌아올 사람 보라고.",
         "할아버지 말로는 그게 원래 진짜 불 얘기였다더군. 아주 옛날 부두마다 꺼지지 않는 불이 하나씩 있었는데, 그 불 곁에서 이름을 부르면 바다에 나간 사람이 제 이름을 듣는다고.",
         "그 창이 그 불이라면… 한 번만 빌려주시오. 얼음 위에서 이름을 하나 불러 보게. 아니, 아니오. 됐소. 내 등불로 하겠소. 남의 불로 부르면 그 애가 못 알아들을 거요.",
     ], items=DUSK)])
more("gacha",
     greet=[g("앗, 그거! 그게 나왔구나! …저기, 그거 어디서 났는지 묻지 말아 줄래? 나도 몰라서 그래!", items=DUSK)],
     topics=[topic("duskfire", "황혼의 불꽃", [
         "솔직히 말할게. 그건 내가 들여온 물건이 아니라구. 어느 날 상품 더미 맨 밑에 있었어. 누가 맡기고 갔는데, 얼굴이 기억이 안 나.",
         "얼굴만이 아니야. 목소리도, 키도. 영수증은 있다구. 여기. 근데 글씨가 뒤집혀 있어. 거울에 비춰야 읽혀.",
         "▷ 뭐라고 적혀 있는데?",
         "'불이 다시 굶주리기 전에, 이 불을 받아들일 손에 닿게 하라. 값은 치렀음.' …누가 받아들일지는 내가 정하는 게 아니라 운이 정하는 거라구! 그래서 뽑기에 넣었지. 난 장사꾼이지 심판이 아니거든.",
     ], items=DUSK)])

data = {"version": VERSION,
        "_help": "NPC 대화. npcs 의 키는 NPC 키(shop_상점ID, quest_NPCID, bank, job, attendance, gacha). greet: 인사 (조건이 맞는 것 중 마지막), "
                 "chatter: 일상 이야기 (무작위 한 마디), topics: 이야기 주제 (need: flags·notFlags·quests 조건). 문장은 \"이름: 대사\" 로 말하는 사람을 바꿀 수 있고, "
                 "{\"ko\": \"...\", \"en\": \"...\"} 로 두 언어를 적을 수 있습니다. 직접 고쳤다면 \"edited\": true 로 두세요 (모드 업데이트 때 덮어쓰지 않습니다).",
        "edited": False,
        "npcs": N}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump(data, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(N), "npcs")
