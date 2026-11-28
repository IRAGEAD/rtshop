/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 */
package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.CombatRole;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public final class WaveComposition {
    private WaveComposition() {
    }

    public static List<FloorDefinition.Enemy> resolve(FloorDefinition.Wave wave, MobCatalog.Pools config, RandomSource random) {
        return WaveComposition.select(wave, config, random).enemies();
    }

    public static Selection select(FloorDefinition.Wave wave, MobCatalog.Pools config, RandomSource random) {
        return WaveComposition.select(wave, config, MobCatalog.equipment(), random, Set.of());
    }

    public static Selection select(FloorDefinition.Wave wave, MobCatalog.Pools config, MobCatalog.Equipment equipment, RandomSource random, Set<ResourceLocation> usedTypes) {
        if (!wave.waveTypes().isEmpty()) {
            List<FloorDefinition.WaveType> usable = wave.waveTypes().stream().filter(type -> WaveComposition.usable(type, config)).toList();
            if (usable.isEmpty()) {
                throw new IllegalArgumentException("No usable wave type: empty category pool in " + String.valueOf(wave.waveTypes().stream().map(FloorDefinition.WaveType::id).toList()));
            }
            List<FloorDefinition.WaveType> candidates = usable.stream().filter(type -> !usedTypes.contains(type.id())).toList();
            if (candidates.isEmpty()) {
                LogUtils.getLogger().warn("All usable configured wave types already used this floor: {}; reusing a configured difficulty-compatible candidate", usedTypes);
                candidates = usable;
            }
            FloorDefinition.WaveType selected = candidates.get(random.m_188503_(candidates.size()));
            LogUtils.getLogger().debug("Wave candidates {}, excluded {}, selected {}", new Object[]{wave.waveTypes().stream().map(FloorDefinition.WaveType::id).toList(), usedTypes, selected.id()});
            ArrayList<FloorDefinition.Enemy> groups = new ArrayList<FloorDefinition.Enemy>();
            for (int stage = 0; stage < selected.sequence().size(); ++stage) {
                EnumMap<MobCategory, Set> chosen = new EnumMap<MobCategory, Set>(MobCategory.class);
                for (FloorDefinition.WaveGroup group : selected.sequence().get(stage).groups()) {
                    Set used = chosen.computeIfAbsent(group.category(), unused -> new HashSet());
                    List<ResourceLocation> pool = WaveComposition.candidates(config.entries().getOrDefault((Object)group.category(), List.of()), group.role(), equipment, String.valueOf(selected.id()) + " stage " + (stage + 1) + " category " + group.category().key());
                    List<ResourceLocation> fresh = pool.stream().filter(id -> !used.contains(id)).toList();
                    if (!fresh.isEmpty()) {
                        pool = fresh;
                    }
                    ResourceLocation id2 = pool.get(random.m_188503_(pool.size()));
                    used.add(id2);
                    groups.add(new FloorDefinition.Enemy(id2, group.count(), group.category(), stage, group.role()));
                }
            }
            return new Selection(selected.id(), groups);
        }
        ArrayList<FloorDefinition.Enemy> result = new ArrayList<FloorDefinition.Enemy>(wave.bosses());
        for (MobCategory category : MobCategory.values()) {
            int count = wave.categories().getOrDefault((Object)category, 0);
            if (count == 0) continue;
            List pool = config.entries().getOrDefault((Object)category, List.of());
            if (pool.isEmpty()) {
                throw new IllegalArgumentException("No usable mob IDs configured for " + category.key());
            }
            if (!config.mixMobs()) {
                result.add(new FloorDefinition.Enemy((ResourceLocation)pool.get(random.m_188503_(pool.size())), count, category));
                continue;
            }
            LinkedHashMap<ResourceLocation, Integer> counts = new LinkedHashMap<ResourceLocation, Integer>();
            for (int i = 0; i < count; ++i) {
                counts.merge((ResourceLocation)pool.get(random.m_188503_(pool.size())), 1, Integer::sum);
            }
            counts.forEach((id, amount) -> result.add(new FloorDefinition.Enemy((ResourceLocation)id, (int)amount, category)));
        }
        return new Selection(null, result);
    }

    public static List<ResourceLocation> candidates(List<ResourceLocation> pool, CombatRole role, MobCatalog.Equipment equipment, String context) {
        List<ResourceLocation> distinct = pool.stream().distinct().toList();
        if (role == CombatRole.ANY) {
            return distinct;
        }
        List<ResourceLocation> matching = distinct.stream().filter(id -> equipment.role((ResourceLocation)id) == role).toList();
        if (!matching.isEmpty() || distinct.isEmpty()) {
            return matching;
        }
        LogUtils.getLogger().warn("No {} candidates found for {}; falling back to unfiltered category pool", (Object)role.key(), (Object)context);
        return distinct;
    }

    private static boolean usable(FloorDefinition.WaveType type, MobCatalog.Pools config) {
        return type.groups().stream().allMatch(group -> !config.entries().getOrDefault((Object)group.category(), List.of()).isEmpty());
    }

    public record Selection(ResourceLocation type, List<FloorDefinition.Enemy> enemies) {
        public Selection {
            enemies = List.copyOf(enemies);
        }
    }
}

