/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.ChestPayout;
import com.pla.spiral_abyss.encounter.FloorRewards;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class FloorProgress {
    private int floor = 1;
    private final int[][] stars = new int[7][3];
    private final int[] tiers = new int[7];
    private final ChestPayout[][] payouts = new ChestPayout[7][3];
    private final Runnable dirty;

    public FloorProgress(Runnable dirty) {
        this.dirty = Objects.requireNonNull(dirty);
        for (int number = 1; number <= 6; ++number) {
            for (int index = 0; index < 3; ++index) {
                this.payouts[number][index] = new ChestPayout(dirty);
            }
        }
    }

    public int floor() {
        return this.floor;
    }

    public boolean completed() {
        return this.claimed();
    }

    public boolean claimed() {
        return this.claimed(this.floor);
    }

    public boolean claimed(int number) {
        return FloorProgress.validFloor(number) && this.tiers[number] != 0;
    }

    public boolean unlocked(int number) {
        if (!FloorProgress.validFloor(number)) {
            return false;
        }
        if (this.claimed(number)) {
            return true;
        }
        for (int previous = 1; previous < number; ++previous) {
            if (this.claimed(previous)) continue;
            return false;
        }
        return true;
    }

    public int claimedMask() {
        int mask = 0;
        for (int number = 1; number <= 6; ++number) {
            if (!this.claimed(number)) continue;
            mask |= 1 << number;
        }
        return mask;
    }

    public int masteredMask() {
        int mask = 0;
        for (int number = 1; number <= 6; ++number) {
            if (this.totalStars(number) != 9) continue;
            mask |= 1 << number;
        }
        return mask;
    }

    public int[] waveStars(int number) {
        FloorProgress.requireFloor(number);
        return (int[])this.stars[number].clone();
    }

    public int totalStars(int number) {
        return Arrays.stream(this.waveStars(number)).sum();
    }

    public int[] projectedStars(int number, int[] attemptStars) {
        FloorProgress.requireStars(attemptStars);
        int[] projected = this.waveStars(number);
        for (int index = 0; index < 3; ++index) {
            projected[index] = Math.max(projected[index], attemptStars[index]);
        }
        return projected;
    }

    public boolean tierClaimed(int number, int tier) {
        FloorProgress.requireFloor(number);
        FloorProgress.requireTier(tier);
        return (this.tiers[number] & 1 << tier - 1) != 0;
    }

    public boolean commitClaim(int number, int tier, int[] attemptStars) {
        FloorProgress.requireTier(tier);
        int[] projected = this.projectedStars(number, attemptStars);
        if (this.tierClaimed(number, tier) || Arrays.stream(projected).sum() < tier * 3) {
            return false;
        }
        this.stars[number] = projected;
        int n = number;
        this.tiers[n] = this.tiers[n] | 1 << tier - 1;
        this.dirty.run();
        return true;
    }

    public void recordStars(int number, int[] attemptStars) {
        int[] projected = this.projectedStars(number, attemptStars);
        if (!this.claimed(number)) {
            throw new IllegalStateException("Claim a reward before recording stars");
        }
        if (!Arrays.equals(this.stars[number], projected)) {
            this.stars[number] = projected;
            this.dirty.run();
        }
    }

    public ChestPayout payout(int tier) {
        return this.payout(this.floor, tier);
    }

    public ChestPayout payout(int number, int tier) {
        FloorProgress.requireFloor(number);
        FloorProgress.requireTier(tier);
        return this.payouts[number][tier - 1];
    }

    public boolean hasPayouts() {
        return this.hasPayouts(this.floor);
    }

    public boolean hasPayouts(int number) {
        FloorProgress.requireFloor(number);
        return Arrays.stream(this.payouts[number]).anyMatch(ChestPayout::active);
    }

    public int pendingFloor() {
        for (int number = 1; number <= 6; ++number) {
            for (ChestPayout payout : this.payouts[number]) {
                if (!payout.active()) continue;
                return number;
            }
        }
        return 0;
    }

    public int requiredPayoutFloor() {
        for (int number = 1; number <= 6; ++number) {
            if (!this.hasPayouts(number) || this.claimed(number)) continue;
            return this.pendingFloor();
        }
        return 0;
    }

    public boolean canEnterFloor(int number) {
        int required = this.requiredPayoutFloor();
        return (required == 0 || number == required) && this.canSelectFloor(number, true);
    }

    public FloorRewards.Bundle pending() {
        for (ChestPayout payout : this.payouts[this.floor]) {
            if (payout.pending() == null) continue;
            return payout.pending();
        }
        return null;
    }

    public int chestTicks() {
        return Arrays.stream(this.payouts[this.floor]).mapToInt(ChestPayout::chestTicks).max().orElse(-1);
    }

    public boolean canSelectFloor(int number) {
        return this.canSelectFloor(number, false);
    }

    public boolean canSelectFloor(int number, boolean acknowledgedCurrentFloor) {
        return this.unlocked(number) && (number == this.floor || this.hasPayouts(number) || !this.hasPayouts() || acknowledgedCurrentFloor && this.claimed(this.floor));
    }

    public void placeSection(int number) {
        FloorProgress.requireFloor(number);
        this.floor = number;
        this.dirty.run();
    }

    public void clearPayouts(int number) {
        FloorProgress.requireFloor(number);
        for (ChestPayout payout : this.payouts[number]) {
            if (!payout.active()) continue;
            payout.complete();
        }
    }

    public void selectFloor(int number) {
        this.selectFloor(number, false);
    }

    public void selectFloor(int number, boolean acknowledgedCurrentFloor) {
        if (!this.canSelectFloor(number, acknowledgedCurrentFloor)) {
            throw new IllegalStateException("Floor is locked or another chest is pending");
        }
        if (number != this.floor) {
            this.floor = number;
            this.dirty.run();
        }
    }

    public void advance() {
        if (!this.claimed() || this.hasPayouts() || !this.unlocked(this.floor + 1)) {
            throw new IllegalStateException("Claim the current floor first");
        }
        ++this.floor;
        this.dirty.run();
    }

    public void skipThrough(int number) {
        FloorProgress.requireFloor(number);
        for (int current = 1; current <= number; ++current) {
            Arrays.fill(this.stars[current], 3);
            this.tiers[current] = 7;
            for (ChestPayout payout : this.payouts[current]) {
                if (!payout.active()) continue;
                payout.complete();
            }
        }
        this.floor = Math.max(this.floor, Math.min(number + 1, 6));
        this.dirty.run();
    }

    public void reset() {
        this.floor = 1;
        for (int[] waveStars : this.stars) {
            Arrays.fill(waveStars, 0);
        }
        Arrays.fill(this.tiers, 0);
        for (int number = 1; number <= 6; ++number) {
            for (ChestPayout payout : this.payouts[number]) {
                payout.complete();
            }
        }
        this.dirty.run();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.m_128405_("Floor", this.floor);
        tag.m_128405_("StarProgressVersion", 1);
        ListTag floors = new ListTag();
        for (int number = 1; number <= 6; ++number) {
            CompoundTag entry = new CompoundTag();
            entry.m_128405_("Floor", number);
            entry.m_128385_("Stars", this.stars[number]);
            entry.m_128405_("Tiers", this.tiers[number]);
            floors.add((Object)entry);
        }
        tag.m_128365_("StarFloors", (Tag)floors);
        ListTag chests = new ListTag();
        for (int number = 1; number <= 6; ++number) {
            for (int tier = 1; tier <= 3; ++tier) {
                if (!this.payout(number, tier).active()) continue;
                CompoundTag entry = this.payout(number, tier).save();
                entry.m_128405_("Floor", number);
                entry.m_128405_("Tier", tier);
                chests.add((Object)entry);
            }
        }
        tag.m_128365_("Payouts", (Tag)chests);
        return tag;
    }

    public static FloorProgress load(CompoundTag tag, Runnable dirty) {
        FloorProgress data = new FloorProgress(dirty);
        data.floor = Math.max(1, Math.min(6, tag.m_128451_("Floor")));
        if (tag.m_128441_("StarProgressVersion")) {
            CompoundTag entry;
            for (Tag value : tag.m_128437_("StarFloors", 10)) {
                entry = (CompoundTag)value;
                int number = entry.m_128451_("Floor");
                if (!FloorProgress.validFloor(number)) continue;
                int[] savedStars = entry.m_128465_("Stars");
                for (int index = 0; index < Math.min(3, savedStars.length); ++index) {
                    data.stars[number][index] = Math.max(0, Math.min(3, savedStars[index]));
                }
                data.tiers[number] = entry.m_128451_("Tiers") & 7;
            }
            for (Tag value : tag.m_128437_("Payouts", 10)) {
                entry = (CompoundTag)value;
                int tier = entry.m_128451_("Tier");
                int number = entry.m_128441_("Floor") ? entry.m_128451_("Floor") : data.floor;
                if (!FloorProgress.validFloor(number) || tier < 1 || tier > 3) continue;
                data.payouts[number][tier - 1] = ChestPayout.load(entry, dirty);
            }
        } else {
            int claimed = tag.m_128451_("Claimed");
            for (int number = 1; number <= 6; ++number) {
                if ((claimed & 1 << number) == 0) continue;
                Arrays.fill(data.stars[number], 1);
                data.tiers[number] = 1;
            }
            if (tag.m_128441_("Pending") && tag.m_128441_("ChestTicks") && tag.m_128451_("ChestTicks") >= 0) {
                data.payouts[data.floor][0] = ChestPayout.load(tag, dirty);
            }
        }
        return data;
    }

    private static boolean validFloor(int number) {
        return number >= 1 && number <= 6;
    }

    private static void requireFloor(int number) {
        if (!FloorProgress.validFloor(number)) {
            throw new IllegalArgumentException("Invalid floor: " + number);
        }
    }

    private static void requireTier(int tier) {
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("Invalid chest tier: " + tier);
        }
    }

    private static void requireStars(int[] values) {
        if (values == null || values.length != 3 || Arrays.stream(values).anyMatch(value -> value < 0 || value > 3)) {
            throw new IllegalArgumentException("Expected three wave star counts between zero and three");
        }
    }
}

