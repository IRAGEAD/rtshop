/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.ChestPayout;
import com.pla.spiral_abyss.encounter.FloorProgress;
import com.pla.spiral_abyss.world.AbyssCycle;
import com.pla.spiral_abyss.world.ArenaBounds;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public final class AbyssPlayers
extends SavedData {
    public static final int SPACING = 512;
    public static final int ARENA_SLOTS = 16;
    private static final int ARENA_LAYOUT_VERSION = 1;
    private final Set<Integer> placedSlots = new HashSet<Integer>();
    private final Set<Integer> legacySlots = new HashSet<Integer>();
    private final Map<UUID, Profile> profiles = new HashMap<UUID, Profile>();
    private AbyssCycle cycle = new AbyssCycle();

    public AbyssCycle cycle() {
        return this.cycle;
    }

    public void resetAllProgress() {
        this.profiles.values().forEach(profile -> {
            profile.progress.reset();
            profile.followingHost = null;
            profile.personalRun = false;
            profile.endlessMode = false;
            profile.endlessWave = 1;
            profile.endlessCheckpoint = 0;
            profile.endlessRewardedWave = 0;
            profile.endlessPayout = new ChestPayout(() -> ((AbyssPlayers)this).m_77762_());
            profile.portal = null;
            profile.cycleReturnRequired = true;
            profile.requestArenaReset();
        });
        this.m_77762_();
    }

    public static AbyssPlayers get(MinecraftServer server) {
        return (AbyssPlayers)server.m_129783_().m_8895_().m_164861_(AbyssPlayers::load, AbyssPlayers::new, "spiral_abyss_players");
    }

    public Profile profile(UUID owner) {
        return this.profiles.computeIfAbsent(owner, id -> {
            this.m_77762_();
            return new Profile((UUID)id, -1, new FloorProgress(() -> ((AbyssPlayers)this).m_77762_()));
        });
    }

    public Collection<Profile> profiles() {
        return List.copyOf(this.profiles.values());
    }

    public static AbyssPlayers load(CompoundTag tag) {
        AbyssPlayers data = new AbyssPlayers();
        data.cycle = AbyssCycle.load(tag.m_128469_("Cycle"));
        Set<Integer> destination = tag.m_128451_("ArenaLayoutVersion") == 1 ? data.placedSlots : data.legacySlots;
        for (int slot : tag.m_128465_("PlacedSlots")) {
            if (slot < 0 || slot >= 16) continue;
            destination.add(slot);
        }
        for (int slot : tag.m_128465_("LegacySlots")) {
            if (slot < 0 || slot >= 16) continue;
            data.legacySlots.add(slot);
        }
        Object object = tag.m_128437_("Profiles", 10).iterator();
        while (object.hasNext()) {
            Tag value = (Tag)object.next();
            CompoundTag entry = (CompoundTag)value;
            UUID owner = entry.m_128342_("Owner");
            AbyssPlayers abyssPlayers = data;
            Objects.requireNonNull(abyssPlayers);
            Profile profile = abyssPlayers.new Profile(owner, -1, FloorProgress.load(entry.m_128469_("Progress"), () -> ((AbyssPlayers)data).m_77762_()));
            data.profiles.put(owner, profile);
            profile.sectionFloor = Math.max(0, Math.min(7, entry.m_128451_("SectionFloor")));
            if (entry.m_128403_("SectionOwner")) {
                profile.sectionOwner = entry.m_128342_("SectionOwner");
            }
            if (entry.m_128403_("Portal")) {
                profile.portal = entry.m_128342_("Portal");
            }
            if (entry.m_128403_("FollowingHost")) {
                profile.followingHost = entry.m_128342_("FollowingHost");
            }
            profile.personalRun = entry.m_128441_("PersonalRun") ? entry.m_128471_("PersonalRun") : profile.followingHost != null;
            profile.arenaResetRequested = entry.m_128471_("ResetArena");
            profile.cycleReturnRequired = entry.m_128471_("CycleReturnRequired");
            profile.endlessMode = entry.m_128471_("EndlessMode");
            profile.endlessWave = Math.max(1, entry.m_128451_("EndlessWave"));
            profile.endlessCheckpoint = Math.max(0, entry.m_128451_("EndlessCheckpoint"));
            profile.endlessRewardedWave = Math.max(0, entry.m_128451_("EndlessRewardedWave"));
            profile.endlessPayout = ChestPayout.load(entry.m_128469_("EndlessPayout"), () -> ((AbyssPlayers)data).m_77762_());
        }
        return data;
    }

    public CompoundTag m_7176_(CompoundTag tag) {
        tag.m_128365_("Cycle", (Tag)this.cycle.save());
        tag.m_128405_("ArenaLayoutVersion", 1);
        tag.m_128385_("PlacedSlots", this.placedSlots.stream().mapToInt(Integer::intValue).toArray());
        tag.m_128385_("LegacySlots", this.legacySlots.stream().mapToInt(Integer::intValue).toArray());
        ListTag list = new ListTag();
        this.profiles.values().forEach(profile -> {
            CompoundTag entry = new CompoundTag();
            entry.m_128362_("Owner", profile.owner);
            entry.m_128405_("SectionFloor", profile.sectionFloor);
            if (profile.sectionOwner != null) {
                entry.m_128362_("SectionOwner", profile.sectionOwner);
            }
            entry.m_128379_("ResetArena", profile.arenaResetRequested);
            entry.m_128379_("CycleReturnRequired", profile.cycleReturnRequired);
            entry.m_128379_("EndlessMode", profile.endlessMode);
            entry.m_128405_("EndlessWave", profile.endlessWave);
            entry.m_128405_("EndlessCheckpoint", profile.endlessCheckpoint);
            entry.m_128405_("EndlessRewardedWave", profile.endlessRewardedWave);
            entry.m_128365_("EndlessPayout", (Tag)profile.endlessPayout.save());
            if (profile.portal != null) {
                entry.m_128362_("Portal", profile.portal);
            }
            if (profile.followingHost != null) {
                entry.m_128362_("FollowingHost", profile.followingHost);
            }
            entry.m_128379_("PersonalRun", profile.personalRun);
            entry.m_128365_("Progress", (Tag)profile.progress.save());
            list.add((Object)entry);
        });
        tag.m_128365_("Profiles", (Tag)list);
        return tag;
    }

    public final class Profile {
        public final UUID owner;
        public int slot;
        public final FloorProgress progress;
        public UUID portal;
        public int sectionFloor;
        public UUID sectionOwner;
        public UUID followingHost;
        public boolean personalRun;
        public boolean cycleReturnRequired;
        public boolean endlessMode;
        public int endlessWave = 1;
        public int endlessCheckpoint;
        public int endlessRewardedWave;
        public ChestPayout endlessPayout = new ChestPayout(() -> ((AbyssPlayers)AbyssPlayers.this).m_77762_());
        private boolean arenaResetRequested;

        public void setEndlessMode(boolean enabled) {
            this.endlessMode = enabled;
            AbyssPlayers.this.m_77762_();
        }

        public void saveEndlessWave(int wave) {
            this.endlessWave = Math.max(1, wave);
            AbyssPlayers.this.m_77762_();
        }

        public void saveEndlessCheckpoint(int wave) {
            this.endlessCheckpoint = Math.max(0, wave);
            AbyssPlayers.this.m_77762_();
        }

        public void markEndlessRewarded(int wave) {
            this.endlessRewardedWave = Math.max(this.endlessRewardedWave, wave);
            AbyssPlayers.this.m_77762_();
        }

        private Profile(UUID owner, int slot, FloorProgress progress) {
            this.owner = owner;
            this.slot = slot;
            this.progress = progress;
        }

        public ArenaBounds bounds() {
            if (this.slot < 0 || this.slot >= 16) {
                throw new IllegalStateException("Profile has no active arena lease");
            }
            return new ArenaBounds(new Vec3(((double)(this.slot % 4) - 1.5) * 512.0 + 0.5, 65.0, ((double)(this.slot / 4) - 1.5) * 512.0 + 0.5));
        }

        public boolean placed() {
            return AbyssPlayers.this.placedSlots.contains(this.slot) && !this.arenaResetRequested;
        }

        public boolean needsArenaReset() {
            return this.arenaResetRequested;
        }

        public void requestArenaReset() {
            this.arenaResetRequested = true;
            AbyssPlayers.this.m_77762_();
        }

        public boolean needsLegacyCleanup() {
            return AbyssPlayers.this.legacySlots.contains(this.slot);
        }

        public void markPlaced() {
            AbyssPlayers.this.placedSlots.add(this.slot);
            AbyssPlayers.this.legacySlots.remove(this.slot);
            this.arenaResetRequested = false;
            AbyssPlayers.this.m_77762_();
        }
    }
}

