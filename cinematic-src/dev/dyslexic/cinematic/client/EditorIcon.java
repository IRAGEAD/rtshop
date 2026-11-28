/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.util.Mth
 */
package dev.dyslexic.cinematic.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class EditorIcon {
    private static final String[] GEAR = new String[]{".....####.....", ".....####.....", "..##.####.##..", "..##########..", "...########...", "##############", "#####....#####", "#####....#####", "##############", "...########...", "..##########..", "..##.####.##..", ".....####.....", ".....####....."};

    private EditorIcon() {
    }

    static void draw(GuiGraphics g, float alpha) {
        int color = Mth.m_14045_((int)Math.round(alpha * 230.0f), (int)0, (int)255) << 24 | 0x67D3E7;
        for (int y = 0; y < GEAR.length; ++y) {
            for (int x = 0; x < GEAR[y].length(); ++x) {
                if (GEAR[y].charAt(x) != '#') continue;
                g.m_280509_(8 + x, 7 + y, 9 + x, 8 + y, color);
            }
        }
    }
}

