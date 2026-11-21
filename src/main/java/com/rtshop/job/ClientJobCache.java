package com.rtshop.job;

/** 클라이언트가 알아야 하는 직업 정보 (서버가 JobSyncPacket 으로 보내 줌). 클라이언트 전용 클래스를 쓰지 않는다. */
public final class ClientJobCache {
    private ClientJobCache() {
    }

    /** 광부 채굴 속도 보너스 (0.08 = +8%). */
    public static volatile double mineSpeed = 0;
}
