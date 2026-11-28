/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffect
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.gui.GuiGraphics
 */
package com.finderfeed.fdbosses.content.entities.geburah.sins;

import com.finderfeed.fdbosses.content.entities.geburah.sins.ScreenFlashEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffect;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public class ScreenFlashEffect
extends ScreenEffect<ScreenFlashEffectData> {
    public ComplexEasingFunction complexEasingFunction;

    public ScreenFlashEffect(ScreenFlashEffectData data, int inTime, int stayTime, int outTime) {
        super((ScreenEffectData)data, inTime, stayTime, outTime);
        this.complexEasingFunction = ComplexEasingFunction.builder().addArea((float)inTime, FDEasings::linear).addArea((float)stayTime, FDEasings::one).addArea((float)outTime, FDEasings::reversedLinear).build();
    }

    public void render(GuiGraphics guiGraphics, float pticks, int currentTick, float screenWidth, float screenHeight) {
        float p = this.complexEasingFunction.apply((float)currentTick + pticks);
        ScreenFlashEffectData data = (ScreenFlashEffectData)this.getData();
        FDColor color = data.color;
        float percent = data.flashSizePercent;
        float width = screenWidth / 2.0f * percent * p;
        PoseStack matrices = guiGraphics.m_280168_();
        float otherSideStart = screenWidth - width;
        FDRenderUtil.fill((PoseStack)matrices, (float)0.0f, (float)0.0f, (float)width, (float)screenHeight, (float)color.r, (float)color.g, (float)color.b, (float)color.a, (float)color.r, (float)color.g, (float)color.b, (float)0.0f, (float)color.r, (float)color.g, (float)color.b, (float)0.0f, (float)color.r, (float)color.g, (float)color.b, (float)color.a);
        FDRenderUtil.fill((PoseStack)matrices, (float)otherSideStart, (float)0.0f, (float)width, (float)screenHeight, (float)color.r, (float)color.g, (float)color.b, (float)0.0f, (float)color.r, (float)color.g, (float)color.b, (float)color.a, (float)color.r, (float)color.g, (float)color.b, (float)color.a, (float)color.r, (float)color.g, (float)color.b, (float)0.0f);
    }
}

