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
 */
package com.finderfeed.fdbosses.client.boss_screen.widget;

import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public class FDButtonWithTexture
extends FDButton {
    private ResourceLocation location;

    public FDButtonWithTexture(Screen screen, float x, float y, float width, float height, ResourceLocation location) {
        super(screen, x, y, width, height);
        this.location = location;
    }

    public void renderWidget(GuiGraphics graphics, float mx, float my, float pticks) {
        super.renderWidget(graphics, mx, my, pticks);
        FDRenderUtil.bindTexture((ResourceLocation)this.location);
        FDRenderUtil.blitWithBlend((PoseStack)graphics.m_280168_(), (float)(this.getX() + this.getWidth() / 2.0f - 8.0f), (float)(this.getY() + this.getHeight() / 2.0f - 8.0f), (float)16.0f, (float)16.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
    }
}

