/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.RandomSource
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.BossWavePolicy;
import com.pla.spiral_abyss.encounter.WaveComposition;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.RandomSource;

public final class EndlessWaves {
    public static final int FLOOR = 7;
    private static final Set<String> NINTH = Set.of("boss_minions", "boss_vanguard", "solo_boss");
    private static final Set<String> TENTH = Set.of("dual_boss", "final_wave_boss", "final_escalation");

    private EndlessWaves() {
    }

    public static int position(int wave) {
        return (Math.max(1, wave) - 1) % 10 + 1;
    }

    public static int block(int wave) {
        return (Math.max(1, wave) - 1) / 10;
    }

    public static double bossHealth(int wave) {
        return 1000.0 + 500.0 * (double)EndlessWaves.block(wave);
    }

    public static double mobScale(int wave) {
        return Math.pow(1.2, Math.min(100, EndlessWaves.block(wave)));
    }

    public static boolean eligible(FloorDefinition.WaveType type, int wave) {
        int position = EndlessWaves.position(wave);
        String name = type.id().m_135815_();
        if (position == 9) {
            return NINTH.contains(name);
        }
        if (position == 10) {
            return TENTH.contains(name);
        }
        if (NINTH.contains(name) || TENTH.contains(name)) {
            return false;
        }
        boolean boss = type.groups().stream().anyMatch(group -> group.category() == MobCategory.BOSS);
        boolean mini = type.groups().stream().anyMatch(group -> group.category() == MobCategory.MINI_BOSS);
        boolean champion = type.groups().stream().anyMatch(group -> group.category() == MobCategory.CHAMPION_MOB);
        if (boss) {
            return position == 8;
        }
        if (mini) {
            return position >= 7;
        }
        if (champion) {
            return position >= 4;
        }
        return true;
    }

    public static int weight(FloorDefinition.WaveType type, int wave) {
        int target = EndlessWaves.position(wave) <= 3 ? 1 + EndlessWaves.position(wave) / 2 : (EndlessWaves.position(wave) <= 6 ? 3 : 5);
        return Math.max(1, 8 - Math.abs(type.tier() - target) * 3);
    }

    public static WaveComposition.Selection select(int wave, long seed, Collection<FloorDefinition.WaveType> templates, MobCatalog.Pools pools, MobCatalog.Equipment equipment) {
        List<FloorDefinition.WaveType> choices = templates.stream().filter(type -> EndlessWaves.eligible(type, wave)).filter(type -> type.groups().stream().allMatch(group -> !pools.entries().getOrDefault((Object)group.category(), List.of()).isEmpty())).sorted(Comparator.comparing(type -> type.id().toString())).toList();
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("No available endless archetype for wave " + wave);
        }
        RandomSource random = RandomSource.m_216335_((long)seed);
        int pick = random.m_188503_(choices.stream().mapToInt(type -> EndlessWaves.weight(type, wave)).sum());
        FloorDefinition.WaveType selected = choices.get(choices.size() - 1);
        for (FloorDefinition.WaveType type2 : choices) {
            if ((pick -= EndlessWaves.weight(type2, wave)) >= 0) continue;
            selected = type2;
            break;
        }
        WaveComposition.Selection composition = WaveComposition.select(new FloorDefinition.Wave(Map.of(), List.of(), List.of(selected)), pools, equipment, random, Set.of());
        return new WaveComposition.Selection(composition.type(), BossWavePolicy.sequential(composition.enemies()));
    }

    public static int experience(int wave, RandomSource random) {
        long milestone = Math.max(1, wave / 10);
        long base = 100L * milestone * milestone;
        return (int)Math.min(1000000L, base + (long)random.m_188503_((int)Math.min(100000L, 50L * milestone) + 1));
    }
}

