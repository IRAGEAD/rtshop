/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.encounter.data;

import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.data.CombatRole;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record FloorDefinition(int number, int timeTicks, int waveDelayTicks, Rewards rewards, List<Wave> waves, Map<Challenge.Difficulty, List<Challenge.Type>> challenge) {
    public FloorDefinition {
        waves = List.copyOf(waves);
        EnumMap<Challenge.Difficulty, List<Challenge.Type>> choices = new EnumMap<Challenge.Difficulty, List<Challenge.Type>>(Challenge.Difficulty.class);
        for (Challenge.Difficulty difficulty : Challenge.Difficulty.values()) {
            List<Challenge.Type> types = challenge.get((Object)difficulty);
            if (types == null || types.isEmpty()) {
                throw new IllegalArgumentException("Missing challenge." + difficulty.key());
            }
            choices.put(difficulty, List.copyOf(types));
        }
        challenge = Map.copyOf(choices);
    }

    public record Rewards(List<RewardEntry> entries, int rolls, int experience) {
        public Rewards {
            entries = List.copyOf(entries);
        }
    }

    public record RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount, int weight, double chance, boolean rare, Map<ResourceLocation, Integer> enchantments) {
        public RewardEntry {
            enchantments = Map.copyOf(enchantments);
        }

        public RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount, int weight, double chance, boolean rare) {
            this(id, item, minCount, maxCount, weight, chance, rare, Map.of());
        }

        public RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount, int weight, double chance) {
            this(id, item, minCount, maxCount, weight, chance, false);
        }
    }

    public record Enemy(ResourceLocation id, int count, MobCategory category, int stage, CombatRole role) {
        public Enemy(ResourceLocation id, int count, MobCategory category) {
            this(id, count, category, 0, CombatRole.ANY);
        }

        public Enemy(ResourceLocation id, int count) {
            this(id, count, null);
        }
    }

    public record WaveType(ResourceLocation id, List<WaveStage> sequence, int tier) {
        public WaveType {
            sequence = List.copyOf(sequence);
        }

        public WaveType(ResourceLocation id, List<WaveGroup> groups) {
            this(id, groups.stream().map(group -> new WaveStage(List.of(group))).toList(), 1);
        }

        public List<WaveGroup> groups() {
            return this.sequence.stream().flatMap(stage -> stage.groups().stream()).toList();
        }

        public int count() {
            return this.groups().stream().mapToInt(WaveGroup::count).sum();
        }
    }

    public record WaveStage(List<WaveGroup> groups) {
        public WaveStage {
            groups = List.copyOf(groups);
        }
    }

    public record WaveGroup(MobCategory category, int count, CombatRole role) {
        public WaveGroup(MobCategory category, int count) {
            this(category, count, CombatRole.ANY);
        }

        private static boolean bossCategory(MobCategory category) {
            return category == MobCategory.MINI_BOSS || category == MobCategory.BOSS;
        }
    }

    public record Wave(Map<MobCategory, Integer> categories, List<Enemy> bosses, List<WaveType> waveTypes) {
        public Wave {
            categories = Map.copyOf(categories);
            bosses = List.copyOf(bosses);
            waveTypes = List.copyOf(waveTypes);
        }

        public Wave(Map<MobCategory, Integer> categories, List<Enemy> bosses) {
            this(categories, bosses, List.of());
        }

        public int minimumEnemies() {
            return this.waveTypes.isEmpty() ? this.categories.values().stream().mapToInt(Integer::intValue).sum() + this.bosses.stream().mapToInt(Enemy::count).sum() : this.waveTypes.stream().mapToInt(WaveType::count).min().orElse(0);
        }

        public boolean bossOnly() {
            return this.waveTypes.isEmpty() ? this.categories.keySet().stream().allMatch(WaveGroup::bossCategory) : this.waveTypes.stream().allMatch(type -> type.groups().stream().allMatch(group -> WaveGroup.bossCategory(group.category())));
        }
    }
}

