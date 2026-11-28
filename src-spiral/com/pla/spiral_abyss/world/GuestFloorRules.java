/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.FloorProgress;

public final class GuestFloorRules {
    private GuestFloorRules() {
    }

    public static int firstUnclaimed(FloorProgress progress) {
        for (int floor = 1; floor <= 6; ++floor) {
            if (progress.claimed(floor)) continue;
            return floor;
        }
        return 6;
    }

    public static int portalDestination(FloorProgress progress, int hostFloor) {
        if (progress.pendingFloor() != 0) {
            return progress.pendingFloor();
        }
        return progress.unlocked(hostFloor) ? hostFloor : GuestFloorRules.firstUnclaimed(progress);
    }

    public static int next(FloorProgress progress, int currentFloor) {
        return GuestFloorRules.next(progress, currentFloor, false);
    }

    public static int next(FloorProgress progress, int currentFloor, boolean acknowledgedCurrentFloor) {
        if (!progress.claimed(currentFloor)) {
            return 0;
        }
        int next = currentFloor + 1;
        return progress.canSelectFloor(next, acknowledgedCurrentFloor) ? next : 0;
    }

    public static boolean joinHost(boolean portalEntry, boolean personalRun, int floor, int hostFloor, boolean hostRunning, boolean pendingReward) {
        return (portalEntry || !personalRun) && floor == hostFloor && !hostRunning && !pendingReward;
    }
}

