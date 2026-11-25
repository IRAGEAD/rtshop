package com.rtshop.job;

/** 클라이언트가 알아야 하는 직업 정보 (서버가 JobSyncPacket 으로 보내 줌). 클라이언트 전용 클래스를 쓰지 않는다. */
public final class ClientJobCache {
    private ClientJobCache() {
    }

    /** 광부 채굴 속도 보너스 (0.08 = +8%). */
    public static volatile double mineSpeed = 0;
    /** 지금 직업의 이름 (없으면 "") 과 패시브 설명 줄 ("+…" 버프, "-…" 디버프, " …" 덧붙임). */
    public static volatile String combat = "", life = "";
    /** 마법사가 아니라서 마법을 쓸 수 없다 (서버가 알려 준다). */
    public static volatile boolean spellLocked;
    public static volatile java.util.List<String> combatDesc = java.util.List.of(), lifeDesc = java.util.List.of();
}
