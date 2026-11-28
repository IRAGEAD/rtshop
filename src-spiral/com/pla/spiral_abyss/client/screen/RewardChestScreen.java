/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.Slot
 */
package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.menu.RewardChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public final class RewardChestScreen
extends AbstractContainerScreen<RewardChestMenu> {
    public RewardChestScreen(RewardChestMenu menu, Inventory inventory, Component title) {
        super((AbstractContainerMenu)menu, inventory, title);
        this.f_97727_ = 240;
        this.f_97731_ = 146;
    }

    protected void m_7856_() {
        super.m_7856_();
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"<"), b -> this.f_96541_.f_91072_.m_105208_(((RewardChestMenu)this.f_97732_).f_38840_, 0)).m_252987_(this.f_97735_ + 8, this.f_97736_ + 7, 20, 16).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)">"), b -> this.f_96541_.f_91072_.m_105208_(((RewardChestMenu)this.f_97732_).f_38840_, 1)).m_252987_(this.f_97735_ + 148, this.f_97736_ + 7, 20, 16).m_253136_());
    }

    protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.m_280509_(this.f_97735_, this.f_97736_, this.f_97735_ + this.f_97726_, this.f_97736_ + this.f_97727_, -14338739);
        graphics.m_280637_(this.f_97735_, this.f_97736_, this.f_97726_, this.f_97727_, -7628115);
        for (Slot slot : ((RewardChestMenu)this.f_97732_).f_38839_) {
            int x = this.f_97735_ + slot.f_40220_;
            int y = this.f_97736_ + slot.f_40221_;
            graphics.m_280509_(x - 1, y - 1, x + 17, y + 17, -15721685);
            graphics.m_280637_(x - 1, y - 1, 18, 18, -9337959);
        }
    }

    protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.m_280653_(this.f_96547_, (Component)Component.m_237110_((String)"screen.spiral_abyss.loot_page", (Object[])new Object[]{((RewardChestMenu)this.f_97732_).page() + 1, ((RewardChestMenu)this.f_97732_).pages()}), this.f_97726_ / 2, 11, 0xFFFFFF);
        graphics.m_280614_(this.f_96547_, this.f_169604_, this.f_97730_, this.f_97731_, 0xFFFFFF, false);
    }

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(graphics);
        super.m_88315_(graphics, mouseX, mouseY, partialTick);
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(0.0f, 0.0f, 300.0f);
        for (int i = 0; i < 54; ++i) {
            if (!((RewardChestMenu)this.f_97732_).rare(i) || !((RewardChestMenu)this.f_97732_).m_38853_(i).m_6657_()) continue;
            Slot slot = ((RewardChestMenu)this.f_97732_).m_38853_(i);
            graphics.m_280637_(this.f_97735_ + slot.f_40220_ - 1, this.f_97736_ + slot.f_40221_ - 1, 18, 18, -10179);
        }
        graphics.m_280168_().m_85849_();
        this.m_280072_(graphics, mouseX, mouseY);
    }
}

