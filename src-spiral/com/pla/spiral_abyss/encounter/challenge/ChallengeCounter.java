/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter.challenge;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ChallengeCounter {
    private final int target;
    private int progress;
    private boolean failed;
    private final Set<UUID> distinct = new HashSet<UUID>();

    public ChallengeCounter(int target) {
        if (target <= 0) {
            throw new IllegalArgumentException("Objective target must be positive");
        }
        this.target = target;
    }

    public int progress() {
        return this.progress;
    }

    public int target() {
        return this.target;
    }

    public boolean failed() {
        return this.failed;
    }

    public boolean complete() {
        return !this.failed && this.progress >= this.target;
    }

    public void set(int amount) {
        this.progress = Math.max(0, Math.min(this.target, amount));
    }

    public void increment() {
        if (this.progress < this.target) {
            ++this.progress;
        }
    }

    public void incrementDistinct(UUID entity) {
        if (this.distinct.add(entity)) {
            this.increment();
        }
    }

    public void fail() {
        this.failed = true;
    }
}

