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
for k, v in EXTRA.items():
    out.setdefault(k, v)
if n != len(src):
    print("번역 수가 원문과 다릅니다:", n, len(src)); sys.exit(1)
dst = os.path.join(root, 'src/main/resources/rtshop_defaults/story_lang/en_us.json')
os.makedirs(os.path.dirname(dst), exist_ok=True)
json.dump(out, open(dst, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print("en_us:", len(out) - 1, "entries ->", dst)
