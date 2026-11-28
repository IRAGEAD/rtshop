/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter;

public final class FloorClock {
    private int remainingTicks;
    private int durationTicks;

    public void start(int ticks) {
        this.durationTicks = this.remainingTicks = Math.max(0, ticks);
    }

    public int remainingTicks() {
        return this.remainingTicks;
    }

    public int elapsedTicks() {
        return this.durationTicks - this.remainingTicks;
    }

    public int remainingSeconds() {
        return (this.remainingTicks + 19) / 20;
    }

    public boolean tick(boolean fighting) {
        if (fighting && this.remainingTicks > 0) {
            --this.remainingTicks;
        }
        return this.remainingTicks == 0;
    }

    public void clear() {
        this.remainingTicks = 0;
        this.durationTicks = 0;
    }
}

