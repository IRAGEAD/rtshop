/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 */
package com.pla.spiral_abyss.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

final class JournalWorldBackdrop {
    private JournalWorldBackdrop() {
    }

    static void render(GuiGraphics graphics, int width, int height, int panelX, int panelY, int panelWidth, int panelHeight) {
        int color = Minecraft.m_91087_().f_91073_ == null ? -15721942 : -2146428629;
        graphics.m_286007_(() -> {
            graphics.m_280509_(0, 0, width, panelY, color);
            graphics.m_280509_(0, panelY, panelX, panelY + panelHeight, color);
            graphics.m_280509_(panelX + panelWidth, panelY, width, panelY + panelHeight, color);
            graphics.m_280509_(0, panelY + panelHeight, width, height, color);
        });
    }
}

