/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.Hashing
 *  com.mojang.logging.LogUtils
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 */
package com.pla.spiral_abyss.world;

import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.BossWavePolicy;
import com.pla.spiral_abyss.encounter.WaveComposition;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import com.pla.spiral_abyss.encounter.challenge.ChallengePlan;
import com.pla.spiral_abyss.encounter.data.CombatRole;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public final class AbyssCycle {
    public static final long DAY_MILLIS = 86400000L;
    private long startedAt;
    private long seed;
    private MobCatalog.Pools pools;
    private ResourceLocation floorFiveBoss;
    private ResourceLocation floorSixBoss;
    private ResourceLocation floorSixFinalBoss;
    private ResourceLocation floorSixFinalSecondBoss;
    private final Map<String, List<FloorDefinition.Enemy>> lineups = new TreeMap<String, List<FloorDefinition.Enemy>>();
    private final Map<String, ResourceLocation> selectedTypes = new TreeMap<String, ResourceLocation>();
    private final Map<String, String> activeLineups = new TreeMap<String, String>();
    private final Map<Integer, List<Challenge>> challenges = new TreeMap<Integer, List<Challenge>>();

    public boolean initialized() {
        return this.pools != null;
    }

    public long bannerPoseSeed() {
        return this.startedAt;
    }

    public MobCatalog.Pools pools() {
        return this.pools;
    }

    public long deadline(int days) {
        return days <= 0 ? -1L : this.startedAt + (long)days * 86400000L;
    }

    public long remainingMillis(long now, int days) {
        return days <= 0 ? -1L : Math.max(0L, this.deadline(days) - now);
    }

    public boolean due(long now, int days) {
        return this.initialized() && days > 0 && now >= this.deadline(days);
    }

    public long nextStart(long now, int days) {
        long interval = (long)days * 86400000L;
        return this.startedAt + Math.max(1L, (now - this.startedAt) / interval) * interval;
    }

    public void start(long now, long newSeed, MobCatalog.Pools snapshot) {
        this.startedAt = now;
        this.seed = newSeed;
        this.pools = snapshot;
        this.lineups.clear();
        this.selectedTypes.clear();
        this.activeLineups.clear();
        this.challenges.clear();
        this.floorFiveBoss = null;
        this.floorSixBoss = null;
        this.floorSixFinalBoss = null;
        this.floorSixFinalSecondBoss = null;
    }

    public List<Challenge> challenges(FloorDefinition floor, ChallengePlan.Context context, ChallengeCatalog.Snapshot catalog) {
        List<Challenge> selected = ChallengePlan.select(this.seed ^ 0x4348414C4C454E47L ^ (long)floor.number(), catalog, floor.challenge(), this.challenges.getOrDefault(floor.number(), List.of()), context);
        this.challenges.put(floor.number(), selected);
        return selected;
    }

    public List<FloorDefinition.Enemy> lineup(int floor, int waveNumber, FloorDefinition.Wave wave, Predicate<ResourceLocation> available, MobCatalog.Pools fallback) {
        BossWavePolicy.validate(floor, waveNumber, wave);
        this.selectBossPlan(available, fallback);
        String key = AbyssCycle.lineupKey(floor, waveNumber, wave);
        String previousKey = this.activeLineups.put(floor + ":" + waveNumber, key);
        if (floor == 6 && waveNumber == 3 && previousKey != null && !this.lineups.containsKey(key)) {
            List<FloorDefinition.Enemy> prior = this.lineups.get(previousKey);
            ResourceLocation selected = this.selectedTypes.get(previousKey);
            FloorDefinition.WaveType definition = wave.waveTypes().stream().filter(type -> type.id().equals((Object)selected)).findFirst().orElse(null);
            if (prior != null && definition != null && definition.groups().size() == prior.size()) {
                boolean compatible = true;
                for (int index = 0; index < prior.size(); ++index) {
                    FloorDefinition.WaveGroup group = definition.groups().get(index);
                    FloorDefinition.Enemy enemy = prior.get(index);
                    compatible &= group.category() == enemy.category() && group.count() == enemy.count() && group.role() == enemy.role();
                }
                if (compatible) {
                    this.lineups.put(key, BossWavePolicy.sequential(prior));
                    this.selectedTypes.put(key, selected);
                }
            }
        }
        HashSet<ResourceLocation> used = new HashSet<ResourceLocation>();
        for (int other = 1; other <= 3; ++other) {
            String otherKey;
            if (other == waveNumber || (otherKey = this.activeLineups.get(floor + ":" + other)) == null || !this.selectedTypes.containsKey(otherKey)) continue;
            used.add(this.selectedTypes.get(otherKey));
        }
        this.migrateLegacyTemplate(floor, waveNumber, wave, key);
        List<FloorDefinition.Enemy> existing = this.lineups.get(key);
        if (existing != null && !key.equals(previousKey) && used.contains(this.selectedTypes.get(key))) {
            LogUtils.getLogger().warn("Reactivated floor {} wave {} template {} conflicts with another wave; selecting an unused configured candidate", new Object[]{floor, waveNumber, this.selectedTypes.get(key)});
            this.lineups.remove(key);
            this.selectedTypes.remove(key);
            existing = null;
        }
        if (existing == null) {
            MobCatalog.Pools usable = this.usablePools(available, fallback);
            WaveComposition.Selection selected = WaveComposition.select(wave, usable, MobCatalog.equipment(), RandomSource.m_216335_((long)this.waveSeed(floor, waveNumber)), used);
            LogUtils.getLogger().debug("Abyss floor {} wave {} selected {} (cycle-stable)", new Object[]{floor, waveNumber, selected.type()});
            this.lineups.put(key, selected.enemies());
            if (selected.type() != null) {
                this.selectedTypes.put(key, selected.type());
            }
        }
        this.repair(available, fallback);
        this.lineups.put(key, BossWavePolicy.sequential(this.lineups.get(key)));
        return this.lineups.get(key);
    }

    private ResourceLocation savedBoss(String slot, int index, Predicate<ResourceLocation> available) {
        String key = this.activeLineups.get(slot);
        if (key == null) {
            return null;
        }
        List bosses = this.lineups.getOrDefault(key, List.of()).stream().filter(enemy -> enemy.category() == MobCategory.BOSS).flatMap(enemy -> Collections.nCopies(enemy.count(), enemy.id()).stream()).toList();
        return index < bosses.size() && available.test((ResourceLocation)bosses.get(index)) ? (ResourceLocation)bosses.get(index) : null;
    }

    private void selectBossPlan(Predicate<ResourceLocation> available, MobCatalog.Pools fallback) {
        HashSet<ResourceLocation> occupied;
        long[] seeds;
        ResourceLocation[] selected;
        ArrayList<ResourceLocation> choices;
        block9: {
            block10: {
                boolean dualFinal;
                choices = new ArrayList<ResourceLocation>(new LinkedHashSet(this.usablePools(available, fallback).entries().getOrDefault((Object)MobCategory.BOSS, List.of())));
                if (choices.size() < 4) {
                    for (ResourceLocation id : fallback.entries().getOrDefault((Object)MobCategory.BOSS, List.of())) {
                        if (!available.test(id) || choices.contains(id)) continue;
                        choices.add(id);
                    }
                }
                selected = new ResourceLocation[]{this.floorFiveBoss, this.floorSixBoss, this.floorSixFinalBoss, this.floorSixFinalSecondBoss};
                String[] slots = new String[]{"5:3", "6:2", "6:3", "6:3"};
                seeds = new long[]{this.waveSeed(5, 3), this.waveSeed(6, 2), this.waveSeed(6, 3), this.waveSeed(6, 3) ^ 0x46494E414C424F53L};
                occupied = new HashSet<ResourceLocation>();
                for (int index = 0; index < selected.length; ++index) {
                    if (selected[index] == null) {
                        selected[index] = this.savedBoss(slots[index], index == 3 ? 1 : 0, available);
                    }
                    if (selected[index] == null || available.test(selected[index]) && occupied.add(selected[index])) continue;
                    selected[index] = null;
                }
                String finalKey = this.activeLineups.get("6:3");
                boolean bl = dualFinal = finalKey != null && this.lineups.getOrDefault(finalKey, List.of()).stream().filter(enemy -> enemy.category() == MobCategory.BOSS).mapToInt(FloorDefinition.Enemy::count).sum() > 1;
                if (dualFinal || selected[3] == null || !choices.contains(selected[3])) break block9;
                if (selected[0] == null || selected[1] == null) break block10;
                if (selected[2] != null) break block9;
            }
            if (choices.stream().allMatch(occupied::contains)) {
                occupied.remove(selected[3]);
                selected[3] = null;
            }
        }
        for (int index = 0; index < selected.length; ++index) {
            if (selected[index] != null) continue;
            selected[index] = AbyssCycle.chooseBoss(choices, occupied, seeds[index]);
            if (selected[index] == null) continue;
            occupied.add(selected[index]);
        }
        this.floorFiveBoss = selected[0];
        this.floorSixBoss = selected[1];
        this.floorSixFinalBoss = selected[2];
        this.floorSixFinalSecondBoss = selected[3];
    }

    private static ResourceLocation chooseBoss(List<ResourceLocation> choices, Set<ResourceLocation> excluded, long seed) {
        List<Object> allowed = choices.stream().filter(id -> !excluded.contains(id)).sorted().toList();
        if (allowed.isEmpty()) {
            allowed = choices.stream().sorted().toList();
        }
        return allowed.isEmpty() ? null : allowed.get(RandomSource.m_216335_((long)seed).m_188503_(allowed.size()));
    }

    public ResourceLocation selectedType(int floor, int waveNumber, FloorDefinition.Wave wave) {
        return this.selectedTypes.get(AbyssCycle.lineupKey(floor, waveNumber, wave));
    }

    private void migrateLegacyTemplate(int floor, int waveNumber, FloorDefinition.Wave wave, String key) {
        if (this.lineups.containsKey(key) || wave.waveTypes().isEmpty()) {
            return;
        }
        if (wave.waveTypes().stream().anyMatch(type -> type.tier() != 1 || type.sequence().stream().anyMatch(stage -> stage.groups().size() != 1 || stage.groups().get(0).role() != CombatRole.ANY))) {
            return;
        }
        String types = wave.waveTypes().stream().map(type -> "WaveType[id=" + String.valueOf(type.id()) + ", groups=" + type.groups().stream().map(group -> "WaveGroup[category=" + String.valueOf((Object)group.category()) + ", count=" + group.count() + "]").collect(Collectors.joining(", ", "[", "]")) + "]").collect(Collectors.joining(", ", "[", "]"));
        String legacy = floor + ":" + waveNumber + ":[]:0:0:0:0:0:types:" + String.valueOf(Hashing.sha256().hashString((CharSequence)types, StandardCharsets.UTF_8));
        List<FloorDefinition.Enemy> previous = this.lineups.get(legacy);
        ResourceLocation selected = this.selectedTypes.get(legacy);
        if (previous == null || selected == null) {
            return;
        }
        FloorDefinition.WaveType definition = wave.waveTypes().stream().filter(type -> type.id().equals((Object)selected)).findFirst().orElse(null);
        if (definition == null || definition.groups().size() != previous.size()) {
            return;
        }
        ArrayList<FloorDefinition.Enemy> migrated = new ArrayList<FloorDefinition.Enemy>();
        for (int index = 0; index < previous.size(); ++index) {
            FloorDefinition.Enemy enemy = previous.get(index);
            FloorDefinition.WaveGroup group = definition.groups().get(index);
            if (enemy.count() != group.count() || enemy.category() != group.category()) {
                return;
            }
            migrated.add(new FloorDefinition.Enemy(enemy.id(), enemy.count(), enemy.category(), index, enemy.role()));
        }
        this.lineups.put(key, List.copyOf(migrated));
        this.selectedTypes.put(key, selected);
    }

    private static String lineupKey(int floor, int waveNumber, FloorDefinition.Wave wave) {
        String bosses = wave.bosses().stream().map(enemy -> "Enemy[id=" + String.valueOf(enemy.id()) + ", count=" + enemy.count() + ", category=" + String.valueOf((Object)enemy.category()) + "]").collect(Collectors.joining(", ", "[", "]"));
        String key = floor + ":" + waveNumber + ":" + bosses;
        for (MobCategory category : MobCategory.values()) {
            key = key + ":" + String.valueOf(wave.categories().getOrDefault((Object)category, 0));
        }
        return wave.waveTypes().isEmpty() ? key : key + ":types:" + String.valueOf(Hashing.sha256().hashString((CharSequence)wave.waveTypes().toString(), StandardCharsets.UTF_8));
    }

    private MobCatalog.Pools usablePools(Predicate<ResourceLocation> available, MobCatalog.Pools fallback) {
        MobCatalog.Pools valid = this.pools.available(available);
        EnumMap<MobCategory, List<ResourceLocation>> entries = new EnumMap<MobCategory, List<ResourceLocation>>(MobCategory.class);
        for (MobCategory category : MobCategory.values()) {
            List<Object> choices = valid.entries().getOrDefault((Object)category, List.of());
            if (choices.isEmpty()) {
                choices = fallback.entries().getOrDefault((Object)category, List.of()).stream().filter(available).toList();
            }
            entries.put(category, choices);
        }
        return new MobCatalog.Pools(this.pools.mixMobs(), entries);
    }

    public boolean repair(Predicate<ResourceLocation> available, MobCatalog.Pools fallback) {
        MobCatalog.Pools usable = this.usablePools(available, fallback);
        ResourceLocation previousFive = this.floorFiveBoss;
        ResourceLocation previousSix = this.floorSixBoss;
        ResourceLocation previousFinal = this.floorSixFinalBoss;
        ResourceLocation previousSecond = this.floorSixFinalSecondBoss;
        this.selectBossPlan(available, fallback);
        boolean changed = !Objects.equals(previousFive, this.floorFiveBoss) || !Objects.equals(previousSix, this.floorSixBoss) || !Objects.equals(previousFinal, this.floorSixFinalBoss) || !Objects.equals(previousSecond, this.floorSixFinalSecondBoss);
        for (Map.Entry<String, List<FloorDefinition.Enemy>> entry : this.lineups.entrySet()) {
            ArrayList<FloorDefinition.Enemy> replacements = new ArrayList<FloorDefinition.Enemy>((Collection)entry.getValue());
            for (int index = 0; index < replacements.size(); ++index) {
                FloorDefinition.Enemy enemy = (FloorDefinition.Enemy)replacements.get(index);
                if (available.test(enemy.id()) || enemy.category() == null) continue;
                List<ResourceLocation> choices = usable.entries().getOrDefault((Object)enemy.category(), List.of());
                if (this.selectedTypes.containsKey(entry.getKey())) {
                    choices = WaveComposition.candidates(choices, enemy.role(), MobCatalog.equipment(), "saved " + String.valueOf(this.selectedTypes.get(entry.getKey())) + " category " + enemy.category().key());
                    HashSet<ResourceLocation> occupied = new HashSet<ResourceLocation>();
                    for (int other = 0; other < replacements.size(); ++other) {
                        FloorDefinition.Enemy group = (FloorDefinition.Enemy)replacements.get(other);
                        if (other == index || group.stage() != enemy.stage() || group.category() != enemy.category()) continue;
                        occupied.add(group.id());
                    }
                    List<ResourceLocation> distinct = choices.stream().filter(id -> !occupied.contains(id)).toList();
                    if (!distinct.isEmpty()) {
                        choices = distinct;
                    }
                }
                if (choices.isEmpty()) continue;
                RandomSource random = RandomSource.m_216335_((long)(this.seed ^ (long)entry.getKey().hashCode() ^ (long)enemy.id().hashCode() ^ (long)index));
                replacements.set(index, new FloorDefinition.Enemy(choices.get(random.m_188503_(choices.size())), enemy.count(), enemy.category(), enemy.stage(), enemy.role()));
                changed = true;
            }
            entry.setValue(List.copyOf(replacements));
        }
        for (String slot : List.of("5:3", "6:2", "6:3")) {
            String key = this.activeLineups.get(slot);
            if (key == null || !this.lineups.containsKey(key)) continue;
            List<FloorDefinition.Enemy> before = this.lineups.get(key);
            ArrayList<FloorDefinition.Enemy> after = new ArrayList<FloorDefinition.Enemy>();
            int bossIndex = 0;
            for (FloorDefinition.Enemy enemy : BossWavePolicy.sequential(before)) {
                ResourceLocation boss;
                ResourceLocation resourceLocation = slot.equals("5:3") ? this.floorFiveBoss : (slot.equals("6:2") ? this.floorSixBoss : (boss = bossIndex == 0 ? this.floorSixFinalBoss : this.floorSixFinalSecondBoss));
                if (enemy.category() == MobCategory.BOSS) {
                    ++bossIndex;
                    after.add(boss == null ? enemy : new FloorDefinition.Enemy(boss, enemy.count(), enemy.category(), enemy.stage(), enemy.role()));
                    continue;
                }
                after.add(enemy);
            }
            if (before.equals(after)) continue;
            this.lineups.put(key, List.copyOf(after));
            changed = true;
        }
        return changed;
    }

    public long waveSeed(int floor, int wave) {
        long mixed = this.seed ^ -7046029254386353131L * (long)floor ^ -4417276706812531889L * (long)wave;
        mixed = (mixed ^ mixed >>> 30) * -4658895280553007687L;
        mixed = (mixed ^ mixed >>> 27) * -7723592293110705685L;
        return mixed ^ mixed >>> 31;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (!this.initialized()) {
            return tag;
        }
        tag.m_128356_("StartedAt", this.startedAt);
        tag.m_128356_("Seed", this.seed);
        tag.m_128379_("MixMobs", this.pools.mixMobs());
        if (this.floorFiveBoss != null) {
            tag.m_128359_("FloorFiveBoss", this.floorFiveBoss.toString());
        }
        if (this.floorSixBoss != null) {
            tag.m_128359_("FloorSixBoss", this.floorSixBoss.toString());
        }
        if (this.floorSixFinalBoss != null) {
            tag.m_128359_("FloorSixFinalBoss", this.floorSixFinalBoss.toString());
        }
        if (this.floorSixFinalSecondBoss != null) {
            tag.m_128359_("FloorSixFinalSecondBoss", this.floorSixFinalSecondBoss.toString());
        }
        CompoundTag entries = new CompoundTag();
        this.pools.entries().forEach((category, ids) -> {
            ListTag list = new ListTag();
            ids.forEach(id -> list.add((Object)StringTag.m_129297_((String)id.toString())));
            entries.m_128365_(category.key(), (Tag)list);
        });
        tag.m_128365_("Pools", (Tag)entries);
        ListTag rolls = new ListTag();
        this.lineups.forEach((key, enemies) -> {
            CompoundTag roll = new CompoundTag();
            roll.m_128359_("Key", key);
            if (this.selectedTypes.containsKey(key)) {
                roll.m_128359_("SelectedType", this.selectedTypes.get(key).toString());
            }
            ListTag mobs = new ListTag();
            enemies.forEach(enemy -> {
                CompoundTag mob = new CompoundTag();
                mob.m_128359_("Id", enemy.id().toString());
                mob.m_128405_("Count", enemy.count());
                if (enemy.category() != null) {
                    mob.m_128359_("Category", enemy.category().name());
                }
                mob.m_128405_("Stage", enemy.stage());
                mob.m_128359_("Role", enemy.role().name());
                mobs.add((Object)mob);
            });
            roll.m_128365_("Mobs", (Tag)mobs);
            rolls.add((Object)roll);
        });
        tag.m_128365_("Lineups", (Tag)rolls);
        CompoundTag active = new CompoundTag();
        this.activeLineups.forEach((arg_0, arg_1) -> ((CompoundTag)active).m_128359_(arg_0, arg_1));
        tag.m_128365_("ActiveLineups", (Tag)active);
        ListTag challengeFloors = new ListTag();
        this.challenges.forEach((floor, selected) -> {
            CompoundTag entry = new CompoundTag();
            entry.m_128405_("Floor", floor.intValue());
            ListTag list = new ListTag();
            selected.forEach(challenge -> list.add((Object)challenge.save()));
            entry.m_128365_("Challenges", (Tag)list);
            challengeFloors.add((Object)entry);
        });
        tag.m_128365_("ChallengeFloors", (Tag)challengeFloors);
        tag.m_128405_("ChallengePolicy", 6);
        return tag;
    }

    public static AbyssCycle load(CompoundTag tag) {
        ResourceLocation id;
        AbyssCycle cycle = new AbyssCycle();
        if (!tag.m_128425_("Pools", 10)) {
            return cycle;
        }
        EnumMap<MobCategory, List<ResourceLocation>> entries = new EnumMap<MobCategory, List<ResourceLocation>>(MobCategory.class);
        CompoundTag saved = tag.m_128469_("Pools");
        for (MobCategory category : MobCategory.values()) {
            if (!saved.m_128441_(category.key())) continue;
            ArrayList<ResourceLocation> ids = new ArrayList<ResourceLocation>();
            for (Tag value : saved.m_128437_(category.key(), 8)) {
                id = ResourceLocation.m_135820_((String)value.m_7916_());
                if (id == null) continue;
                ids.add(id);
            }
            entries.put(category, ids);
        }
        cycle.start(tag.m_128454_("StartedAt"), tag.m_128454_("Seed"), new MobCatalog.Pools(tag.m_128471_("MixMobs"), entries));
        cycle.floorFiveBoss = tag.m_128425_("FloorFiveBoss", 8) ? ResourceLocation.m_135820_((String)tag.m_128461_("FloorFiveBoss")) : null;
        cycle.floorSixBoss = tag.m_128425_("FloorSixBoss", 8) ? ResourceLocation.m_135820_((String)tag.m_128461_("FloorSixBoss")) : null;
        cycle.floorSixFinalBoss = tag.m_128425_("FloorSixFinalBoss", 8) ? ResourceLocation.m_135820_((String)tag.m_128461_("FloorSixFinalBoss")) : null;
        cycle.floorSixFinalSecondBoss = tag.m_128425_("FloorSixFinalSecondBoss", 8) ? ResourceLocation.m_135820_((String)tag.m_128461_("FloorSixFinalSecondBoss")) : null;
        for (Tag value : tag.m_128437_("Lineups", 10)) {
            CompoundTag roll = (CompoundTag)value;
            ArrayList<FloorDefinition.Enemy> enemies = new ArrayList<FloorDefinition.Enemy>();
            for (Tag mobValue : roll.m_128437_("Mobs", 10)) {
                CompoundTag mob = (CompoundTag)mobValue;
                id = ResourceLocation.m_135820_((String)mob.m_128461_("Id"));
                if (id == null || mob.m_128451_("Count") <= 0) continue;
                MobCategory category = mob.m_128441_("Category") ? MobCategory.valueOf(mob.m_128461_("Category")) : null;
                CombatRole role = CombatRole.ANY;
                try {
                    if (mob.m_128441_("Role")) {
                        role = CombatRole.valueOf(mob.m_128461_("Role"));
                    }
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
                enemies.add(new FloorDefinition.Enemy(id, mob.m_128451_("Count"), category, Math.max(0, mob.m_128451_("Stage")), role));
            }
            cycle.lineups.put(roll.m_128461_("Key"), List.copyOf(enemies));
            ResourceLocation type = roll.m_128425_("SelectedType", 8) ? ResourceLocation.m_135820_((String)roll.m_128461_("SelectedType")) : null;
            if (type == null) continue;
            cycle.selectedTypes.put(roll.m_128461_("Key"), type);
        }
        CompoundTag active = tag.m_128469_("ActiveLineups");
        for (String key : active.m_128431_()) {
            cycle.activeLineups.put(key, active.m_128461_(key));
        }
        if (tag.m_128451_("ChallengePolicy") == 6) {
            for (Tag value : tag.m_128437_("ChallengeFloors", 10)) {
                CompoundTag entry = (CompoundTag)value;
                int floor = entry.m_128451_("Floor");
                if (floor < 1 || floor > 6) continue;
                ArrayList<Challenge> selected = new ArrayList<Challenge>();
                for (Tag challenge : entry.m_128437_("Challenges", 10)) {
                    selected.add(Challenge.load((CompoundTag)challenge));
                }
                if (selected.size() != 3) continue;
                cycle.challenges.put(floor, List.copyOf(selected));
            }
        }
        return cycle;
    }
}

