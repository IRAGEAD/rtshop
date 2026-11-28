/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.world.item.ItemStack
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.FloorRewards;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class ChestPayout {
    private FloorRewards.Bundle pending;
    private int chestTicks = -1;
    private int releasedItems;
    private boolean container;
    private final Set<UUID> recipients = new HashSet<UUID>();
    private final Runnable dirty;

    public boolean container() {
        return this.container;
    }

    public boolean hasRecipients() {
        return !this.recipients.isEmpty();
    }

    public boolean allows(UUID player) {
        return this.recipients.contains(player);
    }

    public void addRecipients(Collection<UUID> players) {
        this.recipients.addAll(players);
        this.dirty.run();
    }

    public void useContainer(Collection<UUID> players) {
        this.container = true;
        this.addRecipients(players);
    }

    public boolean empty() {
        return this.pending == null || this.pending.items().stream().skip(this.releasedItems).allMatch(ItemStack::m_41619_);
    }

    public boolean hasRemainingRare() {
        return this.pending != null && this.pending.rareItems().stream().anyMatch(index -> index >= this.releasedItems && !this.pending.items().get((int)index).m_41619_());
    }

    public ItemStack item(int index) {
        return this.pending == null || index < this.releasedItems || index >= this.pending.items().size() ? ItemStack.f_41583_ : this.pending.items().get(index).m_41777_();
    }

    public void remaining(int index, ItemStack stack) {
        if (this.pending == null || index < this.releasedItems || index >= this.pending.items().size()) {
            return;
        }
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(this.pending.items());
        items.set(index, stack.m_41777_());
        this.pending = new FloorRewards.Bundle(items, this.pending.experience(), this.pending.rareItems());
        this.dirty.run();
    }

    public int takeExperience() {
        if (this.pending == null || this.pending.experience() == 0) {
            return 0;
        }
        int experience = this.pending.experience();
        this.pending = new FloorRewards.Bundle(this.pending.items(), 0, this.pending.rareItems());
        this.dirty.run();
        return experience;
    }

    public ChestPayout(Runnable dirty) {
        this.dirty = Objects.requireNonNull(dirty);
    }

    public FloorRewards.Bundle pending() {
        return this.pending;
    }

    public int chestTicks() {
        return this.chestTicks;
    }

    public int releasedItems() {
        return this.releasedItems;
    }

    public boolean active() {
        return this.pending != null;
    }

    public boolean begin(FloorRewards.Bundle bundle) {
        Objects.requireNonNull(bundle);
        if (this.active()) {
            return false;
        }
        this.container = false;
        this.recipients.clear();
        this.pending = bundle;
        this.chestTicks = 0;
        this.releasedItems = 0;
        this.dirty.run();
        return true;
    }

    public void tickOpening() {
        if (this.active()) {
            if (this.chestTicks < Integer.MAX_VALUE) {
                ++this.chestTicks;
            }
            this.dirty.run();
        }
    }

    public ItemStack takeNextReward() {
        if (this.pending == null || this.releasedItems >= this.pending.items().size()) {
            return null;
        }
        ItemStack stack = this.pending.items().get(this.releasedItems++).m_41777_();
        this.dirty.run();
        return stack;
    }

    public void complete() {
        this.container = false;
        this.recipients.clear();
        this.pending = null;
        this.chestTicks = -1;
        this.releasedItems = 0;
        this.dirty.run();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (this.pending != null) {
            tag.m_128365_("Pending", (Tag)this.pending.save());
            tag.m_128379_("Container", this.container);
            ListTag people = new ListTag();
            this.recipients.forEach(id -> people.add((Object)StringTag.m_129297_((String)id.toString())));
            tag.m_128365_("Recipients", (Tag)people);
            tag.m_128405_("ChestTicks", this.chestTicks);
            tag.m_128405_("ReleasedItems", this.releasedItems);
        }
        return tag;
    }

    public static ChestPayout load(CompoundTag tag, Runnable dirty) {
        ChestPayout payout = new ChestPayout(dirty);
        if (tag.m_128441_("Pending")) {
            payout.container = tag.m_128471_("Container");
            for (Tag value : tag.m_128437_("Recipients", 8)) {
                try {
                    payout.recipients.add(UUID.fromString(value.m_7916_()));
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
            payout.pending = FloorRewards.Bundle.load(tag.m_128469_("Pending"));
            payout.chestTicks = Math.max(0, tag.m_128451_("ChestTicks"));
            payout.releasedItems = Math.max(0, Math.min(tag.m_128451_("ReleasedItems"), payout.pending.items().size()));
        }
        return payout;
    }
}

