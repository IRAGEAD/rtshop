/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter;

public final class WaveChallenges {
    public static final int WAVE_COUNT = 3;
    public static final int TIME_LIMIT_TICKS = 6000;
    public static final int TRANSITION_TICKS = 100;

    private WaveChallenges() {
    }

    public static boolean beforeDeadline(int elapsedTicks, int seconds) {
        return elapsedTicks >= 0 && seconds > 0 && (long)elapsedTicks < (long)seconds * 20L;
    }

    public static int countdownSeconds(int ticks) {
        return Math.max(0, (ticks + 19) / 20);
    }
}

