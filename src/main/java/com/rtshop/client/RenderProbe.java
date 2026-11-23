package com.rtshop.client;

import java.util.Map;
import java.util.TreeMap;

/** 점검용: 손에 든 아이템을 어느 그리기 경로가 그리는지 센다 (풀팩 테스트에서 무기 모양 문제 찾기). */
public final class RenderProbe {
    private RenderProbe() {
    }

    private static final Map<String, Integer> COUNTS = new TreeMap<>();
    public static volatile boolean on = false;

    public static synchronized void hit(String what) {
        if (!on) return;
        COUNTS.merge(what, 1, Integer::sum);
    }

    public static synchronized void reset() {
        COUNTS.clear();
        on = true;
    }

    public static synchronized String dump() {
        on = false;
        return COUNTS.toString();
    }
}
