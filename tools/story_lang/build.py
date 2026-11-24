# 스토리 영어판 만들기: python3 tools/story_lang/build.py
# source.json(원문 목록, gen 스크립트로 다시 뽑을 수 있음) + en/<시작번호>.json(번역, 같은 순서) + 아래 EXTRA
# -> src/main/resources/rtshop_defaults/story_lang/en_us.json ("원문": "번역")
import json, glob, os, re, sys
here = os.path.dirname(os.path.abspath(__file__))
root = os.path.dirname(os.path.dirname(here))
src = json.load(open(os.path.join(here, 'source.json'), encoding='utf-8'))
out = {"_help": "스토리 대사 영어판. 키 = 한국어 원문, 값 = 번역. 원문을 고치면 그 문장은 번역이 없어 한국어로 나간다. config/rtshop/story_lang/en_us.json 에 같은 형식으로 넣으면 덮어쓴다."}
n = 0
for f in sorted(glob.glob(os.path.join(here, 'en', '*.json')), key=lambda p: int(os.path.basename(p)[:-5])):
    start = int(os.path.basename(f)[:-5])
    tr = json.load(open(f, encoding='utf-8'))
    for i, t in enumerate(tr):
        out[src[start + i]] = t
        n += 1
ROLES = {"농작물 상인": "Crop Merchant", "잡화 상인": "General Merchant", "장비 상인": "Equipment Merchant", "레이드 상인": "Raid Merchant",
         "토지 상인": "Land Merchant", "출석 상점 주인": "Attendance Shopkeeper", "수산물 상인": "Fish Merchant", "티켓 판매원": "Ticket Seller",
         "식품 상인": "Food Merchant", "직업 안내인": "Job Guide", "식당 주인": "Restaurant Owner", "은행원": "Banker", "레이드 안내인": "Raid Guide"}
NAMES = {"한나": "Hanna", "오스카": "Oscar", "마르타": "Martha", "브람": "Bram", "피터": "Peter", "에드먼드": "Edmund", "세라": "Sera",
         "루시": "Lucy", "엘리": "Ellie", "그레고리": "Gregory", "알버트": "Albert", "발터": "Walter", "로렌초": "Lorenzo", "하루": "Haru"}
EXTRA = dict(ROLES)
for r, re_ in ROLES.items():
    for k, v in NAMES.items():
        EXTRA[r + " " + k] = re_ + " " + v
EXTRA.update({"행운 상인 럭키": "Lucky the Luck Merchant", "시청 서기 하루": "Haru the Town Clerk", "1장": "Chapter 1", "1장 중반": "mid Chapter 1",
              "2장": "Chapter 2", "3장": "Chapter 3", "4장": "Chapter 4"})
# 플레이어의 대답 (대화 상자의 선택지, "▷ " 로 시작)
REPLIES = {
    "셋째 줄은 왜 지워졌나요?": "Why was the third line erased?",
    "알겠어요. 장로님께 가져갈게요.": "All right. I'll take it to the Elder.",
    "회색 망토… 기억해 두겠습니다.": "A grey cloak… I'll remember that.",
    "뭐라고 쓰여 있죠?": "What does it say?",
    "세 끝… 모두 찾아보겠습니다.": "Three ends… I'll find every one of them.",
    "제가 정할게요.": "I'll decide.",
    "(마지막 줄을 소리 내어 읽는다)": "(You read the last line aloud.)",
    "이번엔 아무도 잃지 않을 거예요.": "This time, we won't lose anyone.",
    "약속할게요. 반드시 돌아올게요.": "I promise. I'll come back, no matter what.",
    "엘릭을 꼭 찾아 드릴게요.": "I'll find Elric for you.",
    "그 망토, 반드시 찾아내겠습니다.": "I will find that cloak, whatever it takes.",
    "…제가 정하겠습니다.": "…I'll make the choice.",
    "맡겨 주세요. 봉인은 저희가 살피겠습니다.": "Leave it to us. We'll look after the seals.",
    "대답은… 곧 들려 드리죠.": "My answer… you'll hear it soon.",
    "가요, 대장님.": "Let's go, Captain.",
    "때가 되면, 놓아준다…": "When the time comes, let go…",
    "엘릭도 분명 웃고 있을 거예요.": "I'm sure Elric is smiling too.",
    "언제든 불러 주세요, 대장님.": "Call me anytime, Captain.",
    "괜찮아요. 말해 줘서 고마워요.": "It's all right. Thank you for telling me.",
}
for k, v in REPLIES.items():
    EXTRA["▷ " + k] = "▷ " + v
# 4판(대개편)으로 다시 쓴 장: 퀘스트 ID 별 번역 (ch01_en.py) 을 story.json 의 원문과 짝짓는다. 원문이 같아도 새 번역이 이긴다.
import importlib.util
story = json.load(open(os.path.join(root, 'src/main/resources/rtshop_defaults/quests/story.json'), encoding='utf-8'))
byid = {x["id"]: x for x in story["quests"]}
for mod in ("ch01_en",):
    spec = importlib.util.spec_from_file_location(mod, os.path.join(here, mod + ".py"))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    for qid, tr in m.EN.items():
        qd = byid[qid]
        for ko, en in zip((qd["title"], qd["intro"], qd["progress"], qd["done"]), tr):
            out[ko] = en
    missing = [i for i in byid if i.startswith(("story_0_", "story_1_")) and i not in m.EN]
    if missing: print("번역 없는 퀘스트:", missing); sys.exit(1)
EXTRA.update({"지도 제작자 펠릭스": "Felix the Mapmaker", "대장장이 브람": "Bram the Smith", "서장 폭풍 뒤의 부두": "Prologue: The Dock After the Storm",
              "1장 잿불의 서약": "Chapter 1: The Ember Oath"})
for k, v in EXTRA.items():
    out.setdefault(k, v)
for qd in story["quests"]:
    mm = re.match(r"^(.*) (\(\d+/\d+\))$", qd.get("chain", ""))
    if mm and qd["chain"] not in out and mm.group(1) in out:
        out[qd["chain"]] = out[mm.group(1)] + " " + mm.group(2)
if n != len(src):
    print("번역 수가 원문과 다릅니다:", n, len(src)); sys.exit(1)
dst = os.path.join(root, 'src/main/resources/rtshop_defaults/story_lang/en_us.json')
os.makedirs(os.path.dirname(dst), exist_ok=True)
json.dump(out, open(dst, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print("en_us:", len(out) - 1, "entries ->", dst)
