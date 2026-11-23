package com.rtshop.quest;

import java.util.List;

/** 서버가 클라이언트 화면에 보내는 퀘스트 한 건의 표시용 정보. */
public record QuestView(String id, String title, String chain, int state, String text,
                        List<ObjView> objectives, long rewardGold, List<RewardItem> rewardItems,
                        List<String> choices, String npc) {
    /** 받을 수 있음 */
    public static final int AVAILABLE = 0;
    /** 진행 중 */
    public static final int ACTIVE = 1;
    /** 목표를 모두 채워서 완료를 받을 수 있음 */
    public static final int READY = 2;
    /** 이미 끝낸 퀘스트 (수첩에서 다시 읽기) */
    public static final int DONE = 3;

    public record ObjView(int type, String target, String text, int cur, int need) {
    }

    public record RewardItem(String item, int count) {
    }
}
