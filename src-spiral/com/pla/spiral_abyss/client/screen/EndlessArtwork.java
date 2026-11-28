/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.Util
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class EndlessArtwork {
    private static final ResourceLocation[] PAGES = (ResourceLocation[])IntStream.range(0, 5).mapToObj(i -> new ResourceLocation("spiral_abyss", "textures/gui/floors/endless_" + i + ".png")).toArray(ResourceLocation[]::new);

    EndlessArtwork() {
    }

    static void render(GuiGraphics graphics, int x, int y, int width, int height) {
        long time = Util.m_137550_() % 10000L;
        int remainder = (int)(time % 100L);
        int frame = (int)(time / 100L) * 3 + (remainder < 30 ? 0 : (remainder < 70 ? 1 : 2));
        int tile = frame % 60;
        RenderSystem.enableBlend();
        graphics.m_280411_(PAGES[frame / 60], x, y, width, height, (float)(tile % 10 * 128), (float)(tile / 10 * 228), 128, 228, 1280, 1368);
        RenderSystem.disableBlend();
    }
}

