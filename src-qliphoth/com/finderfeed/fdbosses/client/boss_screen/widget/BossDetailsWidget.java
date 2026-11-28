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
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceLocation
 */
package com.finderfeed.fdbosses.client.boss_screen.widget;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;

public class BossDetailsWidget
extends FDWidget {
    private Component bossName = Component.m_237119_();
    private int textColor = 0xFFFFFF;

    public BossDetailsWidget(Screen screen, float x, float y, float width, float height) {
        super(screen, x, y, width, height);
    }

    public void renderWidget(GuiGraphics guiGraphics, float v, float v1, float v2) {
        PoseStack matrices = guiGraphics.m_280168_();
        FDRenderUtil.bindTexture((ResourceLocation)FDBosses.location("textures/gui/boss_detail.png"));
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)this.getX(), (float)this.getY(), (float)this.getWidth(), (float)this.getHeight(), (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        Font font = Minecraft.m_91087_().f_91062_;
        float bossNameScale = 2.0f;
        float bossNameWidth = (float)font.m_92852_((FormattedText)this.bossName) * bossNameScale;
        Objects.requireNonNull(font);
        float bossNameHeight = 9.0f * bossNameScale;
        float bossNameOffsetX = this.getWidth() / 2.0f - bossNameWidth / 2.0f;
        float bossNameOffsetY = 35.0f;
        float gradientOffset = 2.0f;
        float gradientWidthAddition = 20.0f;
        float r = (float)((this.textColor & 0xFF0000) >> 16) / 255.0f;
        float g = (float)((this.textColor & 0xFF00) >> 8) / 255.0f;
        float b = (float)(this.textColor & 0xFF) / 255.0f;
        FDRenderUtil.fill((PoseStack)matrices, (float)(this.getX() + bossNameOffsetX - gradientOffset + bossNameWidth / 2.0f), (float)(this.getY() + bossNameOffsetY - gradientOffset), (float)(bossNameWidth + gradientOffset * 2.0f + gradientWidthAddition - bossNameWidth / 2.0f), (float)(bossNameHeight + gradientOffset * 2.0f), (float)r, (float)g, (float)b, (float)0.3f, (float)r, (float)g, (float)b, (float)0.0f, (float)r, (float)g, (float)b, (float)0.0f, (float)r, (float)g, (float)b, (float)0.3f);
        FDRenderUtil.fill((PoseStack)matrices, (float)(this.getX() + bossNameOffsetX - gradientOffset * 2.0f - gradientWidthAddition), (float)(this.getY() + bossNameOffsetY - gradientOffset), (float)(bossNameWidth / 2.0f + gradientWidthAddition + gradientOffset), (float)(bossNameHeight + gradientOffset * 2.0f), (float)r, (float)g, (float)b, (float)0.0f, (float)r, (float)g, (float)b, (float)0.3f, (float)r, (float)g, (float)b, (float)0.3f, (float)r, (float)g, (float)b, (float)0.0f);
        FDRenderUtil.renderScaledText((GuiGraphics)guiGraphics, (Component)this.bossName, (float)(this.getX() + bossNameOffsetX), (float)(this.getY() + bossNameOffsetY), (float)bossNameScale, (boolean)true, (int)this.textColor);
    }

    public BossDetailsWidget setBossInfo(Component bossName, int textColor) {
        this.bossName = bossName;
        this.textColor = textColor;
        return this;
    }

    public boolean onMouseClick(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseRelease(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseScroll(float v, float v1, float v3) {
        return false;
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

