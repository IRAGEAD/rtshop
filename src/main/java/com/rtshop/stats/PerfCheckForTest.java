package com.rtshop.stats;

/** 점검용: PerfCheck 의 표본 하나를 직접 넣는다. */
public final class PerfCheckForTest {
    private PerfCheckForTest() {
    }

    public static void sample(PerfCheck.Run r, StackTraceElement[] st) {
        PerfCheck.sample(r, st);
    }
}
