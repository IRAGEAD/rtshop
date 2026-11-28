/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.FDScrollableWidget
 *  com.finderfeed.fdlib.systems.simple_screen.FDWidget
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$Scissor
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 */
package com.finderfeed.fdbosses.client.boss_screen.widget;

import com.finderfeed.fdlib.systems.simple_screen.FDScrollableWidget;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class BossAbilitiesButtonContainer
extends FDScrollableWidget {
    private float maxScroll = 0.0f;

    public BossAbilitiesButtonContainer(Screen screen, float x, float y, float width, float height) {
        super(screen, x, y, width, height);
    }

    public void renderWidget(GuiGraphics guiGraphics, float v, float v1, float v2) {
    }

    public void addChild(String id, FDWidget widget) {
        super.addChild(id, widget);
        this.maxScroll = Math.max(this.maxScroll, widget.getY() - (this.getParent().getY() + 27.0f) + widget.getHeight() + 5.0f);
    }

    public void useScissor() {
        FDRenderUtil.Scissor.pushScissors((float)this.getX(), (float)this.getY(), (float)this.getWidth(), (float)this.getHeight());
    }

    public void endScissor() {
        FDRenderUtil.Scissor.popScissors();
    }

    public float getMaxScroll() {
        return Math.max(this.maxScroll - 74.0f, 0.0f);
    }

    public void onScroll(float v) {
        this.moveChildren(0.0f, -v);
    }

    public float scrollAmount() {
        return 5.0f;
    }

    public boolean onMouseClick(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseRelease(float v, float v1, int i) {
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

