/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.FDWidget
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.resources.ResourceLocation
 */
package com.finderfeed.fdbosses.client.boss_screen.widget;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.boss_screen.widget.BossAbilitiesButtonContainer;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public class BossAbilitesWidget
extends FDWidget {
    private int stringColor;
    public BossAbilitiesButtonContainer bossAbilitiesButtonContainer;
    private int tickCount = 0;

    public BossAbilitesWidget(Screen screen, float x, float y, int stringColor) {
        super(screen, x, y, 231.0f, 120.0f);
        this.bossAbilitiesButtonContainer = new BossAbilitiesButtonContainer(screen, 14.0f, 26.0f, 195.0f, 74.0f);
        this.addChild("infoContainer", (FDWidget)this.bossAbilitiesButtonContainer);
        this.stringColor = stringColor;
    }

    public void renderWidget(GuiGraphics guiGraphics, float mx, float my, float pticks) {
        PoseStack matrices = guiGraphics.m_280168_();
        Font font = Minecraft.m_91087_().f_91062_;
        float bossAbilitiesXs = this.getX();
        float bossAbilitiesYs = this.getY();
        FDRenderUtil.bindTexture((ResourceLocation)FDBosses.location("textures/gui/boss_abilities.png"));
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)bossAbilitiesXs, (float)bossAbilitiesYs, (float)this.getWidth(), (float)this.getHeight(), (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        float p = this.bossAbilitiesButtonContainer.getCurrentScroll() / this.bossAbilitiesButtonContainer.getMaxScroll();
        if (Float.isNaN(p)) {
            p = 0.0f;
        }
        FDRenderUtil.bindTexture((ResourceLocation)FDBosses.location("textures/gui/scroller.png"));
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(this.getX() + 211.0f), (float)(this.getY() + 28.0f + p * 62.0f), (float)5.0f, (float)8.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        if (this.bossAbilitiesButtonContainer.getChildren().size() > 10 && this.bossAbilitiesButtonContainer.getCurrentScroll() == 0.0f) {
            float time = (float)this.tickCount + pticks;
            float offsetY = (float)Math.sin(time / 2.0f);
            this.renderArrow(matrices, 14.0f, 95.0f + offsetY);
        }
    }

    private void renderArrow(PoseStack matrices, float offsetX, float offsetY) {
        FDRenderUtil.fill((PoseStack)matrices, (float)(this.getX() + offsetX), (float)(this.getY() + offsetY), (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        FDRenderUtil.fill((PoseStack)matrices, (float)(this.getX() + offsetX + 1.0f), (float)(this.getY() + offsetY + 1.0f), (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        FDRenderUtil.fill((PoseStack)matrices, (float)(this.getX() + offsetX + 2.0f), (float)(this.getY() + offsetY), (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        FDRenderUtil.fill((PoseStack)matrices, (float)(this.getX() + offsetX + 3.0f), (float)(this.getY() + offsetY - 1.0f), (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        FDRenderUtil.fill((PoseStack)matrices, (float)(this.getX() + offsetX - 1.0f), (float)(this.getY() + offsetY - 1.0f), (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    public void tick() {
        super.tick();
        ++this.tickCount;
    }

    public boolean onMouseClick(float v, float v1, int i) {
        return true;
    }

    public boolean onMouseRelease(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseScroll(float v, float v1, float v3) {
        return true;
    }

    public boolean onCharTyped(char c, int i) {
        return false;
    }

    public boolean onKeyPress(int i, int i1, int i2) {
        return false;
    }

    public boolean onKeyRelease(int i, int i1, int i2) {
        return false;
    }
}

