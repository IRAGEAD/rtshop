/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.ContainerData
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.inventory.SimpleContainerData
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 */
package com.pla.spiral_abyss.menu;

import com.pla.spiral_abyss.encounter.ChestPayout;
import com.pla.spiral_abyss.event.RareRewardBroadcast;
import com.pla.spiral_abyss.init.MenuInit;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RewardChestMenu
extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 54;
    private final SimpleContainer view = new SimpleContainer(54);
    private final ContainerData data = new SimpleContainerData(56);
    private final ArenaSession session;
    private final ChestPayout payout;
    private final int tier;

    public RewardChestMenu(int id, Inventory inventory) {
        this(id, inventory, null, null, 0);
    }

    public RewardChestMenu(int id, Inventory inventory, ArenaSession session, ChestPayout payout, int tier) {
        super((MenuType)MenuInit.REWARD_CHEST.get(), id);
        this.session = session;
        this.payout = payout;
        this.tier = tier;
        for (int i = 0; i < 54; ++i) {
            this.m_38897_(new Slot((Container)this.view, i, 8 + i % 9 * 18, 28 + i / 9 * 18){

                public boolean m_5857_(ItemStack stack) {
                    return false;
                }

                public boolean m_8010_(Player player) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.m_38897_(new Slot((Container)inventory, col + row * 9 + 9, 8 + col * 18, 158 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.m_38897_(new Slot((Container)inventory, col, 8 + col * 18, 216));
        }
        this.m_38884_(this.data);
        this.refresh();
    }

    public int page() {
        return this.data.m_6413_(54);
    }

    public int pages() {
        return Math.max(1, this.data.m_6413_(55));
    }

    public boolean rare(int slot) {
        return slot >= 0 && slot < 54 && this.data.m_6413_(slot) != 0;
    }

    private void refresh() {
        if (this.payout == null) {
            return;
        }
        int count = this.payout.pending() == null ? 0 : this.payout.pending().items().size();
        this.data.m_8050_(55, Math.max(1, (count + 54 - 1) / 54));
        this.data.m_8050_(54, Math.min(this.page(), this.pages() - 1));
        for (int i = 0; i < 54; ++i) {
            int index = this.page() * 54 + i;
            this.view.m_6836_(i, this.payout.item(index));
            this.data.m_8050_(i, this.payout.pending() != null && this.payout.pending().rare(index) && !this.view.m_8020_(i).m_41619_() ? 1 : 0);
        }
    }

    public void m_38946_() {
        this.refresh();
        super.m_38946_();
    }

    public boolean m_6875_(Player player) {
        ServerPlayer serverPlayer;
        return this.session != null && player instanceof ServerPlayer && this.session.canTakeReward(serverPlayer = (ServerPlayer)player, this.payout, this.tier);
    }

    public boolean m_6366_(Player player, int button) {
        if (!this.m_6875_(player) || button < 0 || button > 1) {
            return false;
        }
        this.data.m_8050_(54, Math.max(0, Math.min(this.pages() - 1, this.page() + (button == 0 ? -1 : 1))));
        this.m_38946_();
        return true;
    }

    public void m_150399_(int slot, int button, ClickType type, Player player) {
        if (!this.m_6875_(player)) {
            return;
        }
        if (slot >= 0 && slot < 54 && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE)) {
            if (button < 0 || button > 1) {
                return;
            }
            int index = this.page() * 54 + slot;
            ItemStack saved = this.payout.item(index);
            if (saved.m_41619_()) {
                return;
            }
            ItemStack transfer = saved.m_41777_();
            if (type == ClickType.PICKUP && button == 1) {
                transfer.m_41764_(1);
            }
            int requested = transfer.m_41613_();
            ItemStack acquired = transfer.m_41777_();
            boolean rare = this.payout.pending().rare(index);
            player.m_150109_().m_36054_(transfer);
            int taken = requested - transfer.m_41613_();
            if (taken > 0) {
                saved.m_41774_(taken);
                this.payout.remaining(index, saved);
                if (rare) {
                    RareRewardBroadcast.acquired((ServerPlayer)player, acquired, this.session.controller().currentFloor());
                }
                this.session.finishContainerPayout(this.payout, this.tier);
            }
            this.m_38946_();
        }
    }

    public ItemStack m_7648_(Player player, int slot) {
        return ItemStack.f_41583_;
    }
}

