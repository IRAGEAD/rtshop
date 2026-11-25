# 읽을 수 있는 이야기 물건의 글을 만든다: python3 tools/gen_readables.py
#   서약서·연대기 페이지·일지 같은 것을 들고 우클릭하면 펼쳐 읽는다 (story/Readables.java, 화면은 client/ReadScreen.java).
#   글은 새로 지어내지 않는다: 스토리(quests/story.json)에서 그 물건을 읽는 장면의 문장을 그대로 옮긴다. Q("…") 로 감싼 문장이
#   스토리에 없으면 이 스크립트가 멈춘다 (스토리를 고쳤으면 여기도 같이 고친다).
#   쪽마다 need 를 달아, 이야기에서 그 대목을 읽은 뒤에만 보이게 한다.
# 결과: src/main/resources/rtshop_defaults/readables.json
import json, os, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'rtshop_defaults')
STORY_RAW = open(os.path.join(ROOT, 'quests', 'story.json'), encoding='utf-8').read()
STORY = json.load(open(os.path.join(ROOT, 'quests', 'story.json'), encoding='utf-8'))
BY = {q['id']: q for q in STORY['quests']}
FLAT = json.dumps(STORY, ensure_ascii=False).replace('\\"', '"').replace('\\n', '\n')

VERSION = 1


def Q(text):
    """스토리에 실제로 있는 문장만 쓴다."""
    if text not in FLAT:
        sys.exit("스토리에 없는 문장입니다 (tools/gen_readables.py): " + text)
    return text


def done(*ids):
    for i in ids: assert i in BY, i
    return {"quests": list(ids)}


def offered(qid):
    """그 퀘스트를 받을 수 있게 된 뒤 (앞 퀘스트를 끝낸 뒤). 그 퀘스트의 첫머리 글에서 읽은 대목에 쓴다."""
    return {"quests": list(BY[qid].get('requires') or [])}


def page(text, head="", by="", **need):
    o = {"head": head, "text": text, "by": by}
    if need: o["need"] = need
    return o


ITEMS = {}

# ---------------------------------------------------------------- 견습 서약서 (서장에서 받는다. 셋째 줄은 종장에서 드러난다)
Q("서약은 세 줄이에요. 첫째, 불을 지킨다. 둘째, 봉인을 지킨다. 셋째… 셋째 줄은 지워져 있네요.")
Q("이건 우리 어머니가 베껴 쓴 거예요.")
Q("하나. 불을 지킨다.\n둘. 봉인을 지킨다.\n셋. 때가 되면 놓아준다.")
ITEMS["rtshop:oath_scroll"] = {
    "title": "잿불 수도회 견습 서약", "sub": "베껴 쓴 이: 기록관 엘레나",
    "pages": [
        page("하나. 불을 지킨다.\n둘. 봉인을 지킨다.\n셋. ――――――――\n\n(셋째 줄은 지워져 있다.)", notQuests=["story_5_10"]),
        page("하나. 불을 지킨다.\n둘. 봉인을 지킨다.\n셋. 때가 되면 놓아준다.\n\n(지워져 있던 셋째 줄. 서약의 원본에는 이렇게 적혀 있었다.)", quests=["story_5_10"]),
    ]}

# ---------------------------------------------------------------- 잿빛 연대기의 페이지: 지금까지 기록관이 연대기에 적어 넣은 이름들
ITEMS["rtshop:chronicle_page"] = {
    "title": "베스탈리아 연대기", "sub": "되찾은 페이지 곁에 기록관이 적어 넣은 이름들",
    "chronicle": True,
    "empty": "아직 적힌 이름이 없다.\n\n이야기를 따라가면 기록관 이사벨이 한 줄씩 적어 넣는다."}

# ---------------------------------------------------------------- 기록관 엘레나의 일지 (1장)
ITEMS["rtshop:mother_journal"] = {
    "title": "기록관 엘레나의 일지", "sub": "블레이즈 불빛 아래에서만 드러나는 글",
    "pages": [
        page("\n\n".join([Q("이사벨에게. 이 글을 읽는다면 나는 돌아가지 못한 것이다. 남은 페이지는 세 끝에 숨겼다."),
                          Q("불타는 요새의 검은 별 곁에, 소리 없는 도시의 문 앞에, 허공 끝의 용 아래에."),
                          Q("연대기는 낡아서 떨어진 것이 아니다. 누군가 고쳐 쓰려고 잘랐다. 나는 고쳐지기 전의 한 장을 보았다.")]),
             head="백지에 떠오른 글"),
        page(Q("요새는 성이 아니라 둑이다. 명령판 셋을 읽으면 검은 별이 무엇인지 안다."), head="첫째 끝 · 불타는 요새", **offered("story_1_16")),
        page("「세 머리의 서약」\n\n" + Q("방벽이 뚫리면 세 대장의 머리를 영혼 모래 위에 세운다."), head="요새에서 베낀 글", **offered("story_1_20")),
        page(Q("땅 밑 깊은 어둠, 소리가 사라지는 도시. 문 앞에 페이지를 묻었다."), head="뒷장 · 둘째 끝", **offered("story_1_31")),
        page("\n\n".join([Q("벨. 연대기의 페이지를 사는 사람은 회색 망토를 입는다. 나는 그를 안다. 모닥불에서 늘 불 먼 쪽에 앉던 사람."),
                          Q("연대기를 자른 것은 불이 아니라 사람의 손이다. 그 손은 장갑을…")]) + "\n\n(마지막 줄은 접혀 있다.)",
             head="마지막 장", quests=["story_1_40"], flags=["ch1_folded"]),
        page("\n\n".join([Q("벨. 연대기의 페이지를 사는 사람은 회색 망토를 입는다. 나는 그를 안다. 모닥불에서 늘 불 먼 쪽에 앉던 사람."),
                          "연대기를 자른 것은 불이 아니라 사람의 손이다. 그 손은 장갑을 끼고 있다."]),
             head="마지막 장", quests=["story_1_40"], notFlags=["ch1_folded"]),
    ]}
Q("…장갑을 끼고 있다.")

# ---------------------------------------------------------------- 스무 해 전의 원정대 지도 (1장 끝 ~ 2장)
Q("지도 위 붉은 표시 열한 개는 대원들이 쓰러진 자리다. 하나만 지워져 있지. 카엘의 자리다.")
Q("토마스 · 리아 · 브렌 · 마르코 · 이네스\n다닐로 · 요나 · 페이 · 하윤 · 오토\n카엘, 부대장. 친구를 살리려고 사라진 사람\n그리고 로란, 대장. 돌아와서 인원을 센 사람")
ITEMS["rtshop:old_expedition_map"] = {
    "title": "스무 해 전의 원정대 지도", "sub": "구석의 서명: 로란, 카엘",
    "pages": [
        page("붉은 표시가 열한 개 찍혀 있다.\n하나만 지워져 있다.\n\n구석에 서명이 둘. 반듯한 글씨와, 장난스럽게 기운 글씨.", head="낡은 지도"),
        page("붉은 표시 열한 개는 대원들이 쓰러진 자리다.\n지워진 하나는 카엘의 자리다.", head="표시의 뜻", quests=["story_2_01"]),
        page("토마스 · 리아 · 브렌 · 마르코 · 이네스\n다닐로 · 요나 · 페이 · 하윤 · 오토\n\n카엘, 부대장. 친구를 살리려고 사라진 사람\n\n그리고 로란, 대장. 돌아와서 인원을 센 사람",
             head="열두 개의 이름", by="기록관 이사벨", quests=["story_2_25"]),
    ]}

# ---------------------------------------------------------------- 얼어붙은 항해일지 (3장)
ITEMS["rtshop:frozen_logbook"] = {
    "title": "얼어붙은 항해일지", "sub": "선장 코넬리아",
    "pages": [
        page("얼음이 박혀 펼쳐지지 않는다.\n\n급히 녹이면 잉크가 번진다. 기록관 이사벨이 천천히 녹이고 있다.", notQuests=["story_3_33"]),
        page("\n\n".join([Q("회색 망토의 학자가 왔다. '여덟 번째 눈이 있는 곳'이라며 항로를 내밀었다. 잊힌 것을 되돌리는 힘이 거기 있다고. 아침별을 아는 사람이었다. 서른둘의 이름을 댔다. 내가 한 번도 남에게 말한 적 없는 이름들을."),
                          Q("값은 배 한 척 값. 그리고 부탁 하나. '거기 닿으면 들리는 것을 받아 적어 오시오.' 무엇이 들리느냐 물으니 웃기만 했다. 장갑을 끼고 있었다. 한여름인데.")]),
             head="항로를 산 날", quests=["story_3_33"]),
        page("\n\n".join([Q("항로 끝에서 폭풍을 만났다. 폭풍이 배를 감쌌다. 부수지 않았다. 들여다봤다. 한 사람씩. 그때 누가 닻을 내렸다. 종소리가 났다. 선원들이 쓰러지기 시작했다. 얼어서가 아니다. 종소리에."),
                          Q("바다 밑에서 누가 물었다. 무엇을 청하느냐고. 나는 청했다. 아침별 때는 청할 데가 없었다.")]),
             head="항로 끝", quests=["story_3_33"]),
        page("글이 아니라 약도다. 선장실 바닥 널 밑.\n\n(풀로 붙여 숨겨 둔 쪽이다.)", head="붙여 놓은 쪽", **offered("story_3_36")),
        page("\n\n".join([Q("항로 끝에서 폭풍이 배를 들여다볼 것이오. 해치지 않소."),
                          Q("그때 폭풍 속에서 누가 이름을 부를 것이오. 사람 이름이오. 한 번만 부를지도 모르오. 그것을 받아 적어 오시오. 한 글자도 틀리지 말고."),
                          Q("그 이름 하나의 값으로 항로 값을 전부 돌려드리겠소.") + "\n(서명은 글자가 아니라 그림이다. 장갑을 낀 손.)"]),
             head="지갑에 접혀 있던 쪽지", quests=["story_3_36"]),
        page(Q("이름은 못 들었다. 폭풍은 아무 이름도 부르지 않았다. 부르려다 만 것 같았다. 부를 이름을 모르는 사람처럼."),
             head="쪽지 뒷면 · 선장의 글씨", quests=["story_3_36"]),
    ]}

# ---------------------------------------------------------------- 연대기의 마지막 장 (에필로그)
ITEMS["rtshop:chronicle_last_page"] = {
    "title": "베스탈리아 연대기 · 마지막 장", "sub": "폭풍이 그친 부두에서",
    "pages": [
        page(Q("불을 지키고, 봉인을 지키고, 그리고 때가 되어 놓아주었다.") + "\n\n조사대원 한 사람 한 사람의 이름으로 적었다.\n열일곱 척에 타고 온 전부.\n\n재의 잉크로 적었다. 불이 먹지 못한다.",
             head="마지막 장", by="기록관 이사벨"),
    ],
    "chronicle": True}
Q("조사대원 한 사람 한 사람의 이름으로 적었어요. 열일곱 척에 타고 온 전부요.")

# 조건에 쓴 깃발이 스토리에 실제로 있는지
for it in ITEMS.values():
    for p in it.get("pages", []):
        for f in (p.get("need", {}).get("flags", []) + p.get("need", {}).get("notFlags", [])):
            assert '"' + f + '"' in STORY_RAW, f
        for q in (p.get("need", {}).get("quests", []) + p.get("need", {}).get("notQuests", [])):
            assert q in BY, q

out = {"version": VERSION,
       "_help": "읽을 수 있는 이야기 물건. items 의 키 = 아이템 ID. pages 의 쪽마다 head(작은 제목)·text(글, \\n 으로 줄바꿈)·by(끝에 적는 이름)·need(조건: flags, notFlags, quests, notQuests, items). "
                "chronicle: true 면 끝낸 퀘스트에서 기록관이 적은 이름들이 뒤에 붙는다. 직접 고쳤으면 \"edited\": true 를 넣어 새 판으로 바뀌지 않게 한다. /rtshop reload 로 다시 읽는다.",
       "items": ITEMS}
path = os.path.join(ROOT, 'readables.json')
json.dump(out, open(path, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(ITEMS), "readables ->", os.path.normpath(path))
