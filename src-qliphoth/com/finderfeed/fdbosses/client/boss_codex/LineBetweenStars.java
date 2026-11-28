/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.util.Mth
 */
package com.finderfeed.fdbosses.client.boss_codex;

import com.finderfeed.fdbosses.client.BossRenderUtil;
import com.finderfeed.fdbosses.client.boss_codex.StarButton;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class LineBetweenStars {
    private ComplexEasingFunction FUNCTION = ComplexEasingFunction.builder().addArea(0.5f, FDEasings::linear).addArea(0.5f, FDEasings::reversedLinear).build();
    private StarButton starButton1;
    private StarButton starButton2;
    private int flashTime;
    private int time;
    private int delay;
    private int travelTime;
    private int tick;

    public LineBetweenStars(StarButton starButton1, StarButton starButton2, int flashTime, int startTime, int delay, int travelTime) {
        this.starButton1 = starButton1;
        this.starButton2 = starButton2;
        this.flashTime = flashTime;
        this.time = startTime;
        this.tick = 0;
        this.travelTime = travelTime;
        this.delay = delay;
    }

    public void tick() {
        this.tick = Mth.m_14045_((int)(this.tick + 1), (int)0, (int)(this.travelTime + this.delay));
        this.time = (this.time + 1) % this.flashTime;
    }

    public void setActivated() {
        this.tick = this.travelTime + this.delay;
    }

    public void render(GuiGraphics graphics) {
        float t = (float)this.time + FDRenderUtil.tryGetPartialTickIgnorePause();
        float p = Mth.m_14036_((float)(t / (float)this.flashTime), (float)0.0f, (float)1.0f);
        float val = this.FUNCTION.apply(p);
        float t2 = (float)this.tick + FDRenderUtil.tryGetPartialTickIgnorePause();
        float p2 = Mth.m_14036_((float)((t2 - (float)this.delay) / (float)this.travelTime), (float)0.0f, (float)1.0f);
        float xt = FDMathUtil.lerp((float)(this.starButton1.getX() + this.starButton1.getWidth() / 2.0f), (float)(this.starButton2.getX() + this.starButton2.getWidth() / 2.0f), (float)p2);
        float yt = FDMathUtil.lerp((float)(this.starButton1.getY() + this.starButton1.getHeight() / 2.0f), (float)(this.starButton2.getY() + this.starButton2.getHeight() / 2.0f), (float)p2);
        float g1 = this.starButton1.entityType != null ? 1.0f : 0.0f;
        float g2 = this.starButton2.entityType != null ? 1.0f : 0.0f;
        float b1 = this.starButton1.entityType != null ? val : 0.0f;
        float b2 = this.starButton2.entityType != null ? val : 0.0f;
        BossRenderUtil.renderLine(graphics, this.starButton1.getX() + this.starButton1.getWidth() / 2.0f, this.starButton1.getY() + this.starButton1.getHeight() / 2.0f, xt, yt, 2.0f, 1.0f, g1, b1, 1.0f, 1.0f, g2, b2, 1.0f);
    }
}

