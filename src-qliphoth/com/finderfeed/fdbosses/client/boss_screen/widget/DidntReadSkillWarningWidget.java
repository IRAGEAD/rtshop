/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.FDWidget
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 */
package com.finderfeed.fdbosses.client.boss_screen.widget;

import com.finderfeed.fdbosses.client.BossRenderUtil;
import com.finderfeed.fdbosses.client.boss_screen.BaseBossScreen;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DidntReadSkillWarningWidget
extends FDWidget {
    public DidntReadSkillWarningWidget(BaseBossScreen screen, float x, float y, float width, float height) {
        super((Screen)screen, x, y, width, height);
    }

    public void renderWidget(GuiGraphics guiGraphics, float v, float v1, float v2) {
        BossRenderUtil.renderBossScreenTooltip(guiGraphics.m_280168_(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        FDRenderUtil.renderFullyCenteredText((GuiGraphics)guiGraphics, (float)(this.getX() + this.getWidth() / 2.0f), (float)(this.getY() + 30.0f), (int)Math.round(this.getWidth() - 20.0f), (float)1.0f, (boolean)true, (int)((BaseBossScreen)this.widgetOwner).getBaseStringColor(), (Component)Component.m_237115_((String)"fdbosses.word.didnt_read_skills"));
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

