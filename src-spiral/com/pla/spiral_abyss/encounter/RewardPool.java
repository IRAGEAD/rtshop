/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public final class RewardPool {
    public static final double RARE_CHANCE = 0.05;
    public static final int RARE_COUNT = 1;
    private final List<FloorDefinition.RewardEntry> tables;
    private final List<FloorDefinition.RewardEntry> items;
    private final int tableWeight;

    public static double rareChance() {
        return (Double)AbyssConfig.RARE_REWARD_CHANCE.get();
    }

    public static FloorDefinition.RewardEntry rare(FloorDefinition.RewardEntry entry) {
        return new FloorDefinition.RewardEntry(entry.id(), true, 1, 1, entry.weight(), 0.05, true, entry.enchantments());
    }

    public RewardPool(List<FloorDefinition.RewardEntry> entries, Predicate<FloorDefinition.RewardEntry> available) {
        this.tables = entries.stream().filter(e -> !e.item()).filter(available).toList();
        this.items = entries.stream().filter(FloorDefinition.RewardEntry::item).filter(available).toList();
        this.tableWeight = this.tables.stream().mapToInt(FloorDefinition.RewardEntry::weight).sum();
    }

    public FloorDefinition.RewardEntry table(RandomSource random) {
        if (this.tableWeight == 0) {
            return null;
        }
        int choice = random.m_188503_(this.tableWeight);
        for (FloorDefinition.RewardEntry entry : this.tables) {
            if ((choice -= entry.weight()) >= 0) continue;
            return entry;
        }
        throw new IllegalStateException("Invalid reward weights");
    }

    public FloorDefinition.RewardEntry table(RandomSource random, Set<ResourceLocation> attempted) {
        int remainingWeight = this.tables.stream().filter(entry -> !attempted.contains(entry.id())).mapToInt(FloorDefinition.RewardEntry::weight).sum();
        if (remainingWeight == 0) {
            return null;
        }
        int choice = random.m_188503_(remainingWeight);
        for (FloorDefinition.RewardEntry entry2 : this.tables) {
            if (attempted.contains(entry2.id()) || (choice -= entry2.weight()) >= 0) continue;
            return entry2;
        }
        throw new IllegalStateException("Invalid remaining reward weights");
    }

    public FloorDefinition.RewardEntry bonus(FloorDefinition.RewardEntry requested, RandomSource random) {
        if (this.items.contains(requested)) {
            return requested;
        }
        return this.items.isEmpty() ? null : this.items.get(random.m_188503_(this.items.size()));
    }

    public FloorDefinition.RewardEntry singleBonus(List<FloorDefinition.RewardEntry> requested, RandomSource random, int eligiblePlayers) {
        double chance = RewardPool.rareChance();
        FloorDefinition.RewardEntry selected = null;
        int successes = 0;
        for (int player = 0; player < eligiblePlayers; ++player) {
            for (FloorDefinition.RewardEntry request : requested) {
                FloorDefinition.RewardEntry entry = this.bonus(request, random);
                if (entry == null || random.m_188500_() >= chance || random.m_188503_(++successes) != 0) continue;
                selected = entry;
            }
        }
        return selected;
    }
}

