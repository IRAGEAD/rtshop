/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 */
package com.finderfeed.fdbosses.client.boss_screen.widget;

import com.finderfeed.fdbosses.client.boss_screen.screen_definitions.BossInfo;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class FDSkillButton
extends FDButton {
    private BossInfo bossInfo;

    public FDSkillButton(Screen screen, float x, float y, float width, float height, BossInfo bossInfo) {
        super(screen, x, y, width, height);
        this.bossInfo = bossInfo;
    }

    public void renderWidget(GuiGraphics graphics, float mx, float my, float pticks) {
        super.renderWidget(graphics, mx, my, pticks);
        Optional item = this.bossInfo.getInfoIcon().right();
        Optional rl = this.bossInfo.getInfoIcon().left();
        if (rl.isPresent()) {
            FDRenderUtil.bindTexture((ResourceLocation)((ResourceLocation)rl.get()));
            FDRenderUtil.blitWithBlend((PoseStack)graphics.m_280168_(), (float)(this.getX() + this.getWidth() / 2.0f - 8.0f), (float)(this.getY() + this.getHeight() / 2.0f - 8.0f), (float)16.0f, (float)16.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        } else if (item.isPresent()) {
            ItemStack stack = (ItemStack)item.get();
            PoseStack poseStack = graphics.m_280168_();
            poseStack.m_85836_();
            poseStack.m_252880_(0.0f, 0.0f, -100.0f);
            FDRenderUtil.renderScaledItemStack((GuiGraphics)graphics, (float)(this.getX() + this.getWidth() / 2.0f - 8.0f), (float)(this.getY() + this.getHeight() / 2.0f - 8.0f), (float)1.0f, (ItemStack)stack);
            poseStack.m_85849_();
        }
    }

    public BossInfo getBossInfo() {
        return this.bossInfo;
    }

    public void setBossInfo(BossInfo bossInfo) {
        this.bossInfo = bossInfo;
    }
}

