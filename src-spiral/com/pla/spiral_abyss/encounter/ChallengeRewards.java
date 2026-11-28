/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.FloorProgress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class ChallengeRewards {
    private ChallengeRewards() {
    }

    public static List<UUID> eligible(Set<UUID> captured, Collection<UUID> present, Function<UUID, FloorProgress> progress, int floor, int tier, int[] attemptStars) {
        if (tier < 1 || tier > 3) {
            return List.of();
        }
        return present.stream().distinct().filter(captured::contains).filter(id -> {
            FloorProgress personal = (FloorProgress)progress.apply((UUID)id);
            return !personal.tierClaimed(floor, tier) && Arrays.stream(personal.projectedStars(floor, attemptStars)).sum() >= tier * 3;
        }).toList();
    }

    public static double scale(int eligiblePlayers) {
        return (double)Math.max(0, eligiblePlayers) / 3.0;
    }

    public static int rollCount(int tier, int eligiblePlayers) {
        if (tier < 1 || tier > 3 || eligiblePlayers <= 0) {
            return 0;
        }
        return Math.multiplyExact(tier, eligiblePlayers);
    }
}

