/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.NativeImage
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class BannerArtwork
implements AutoCloseable {
    private static final List<Pixel> ART = BannerArtwork.build();
    private ResourceLocation texture;

    BannerArtwork() {
    }

    void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (this.texture == null) {
            NativeImage image = new NativeImage(800, 400, true);
            for (Pixel pixel : ART) {
                int argb = pixel.color;
                int abgr = argb & 0xFF00FF00 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
                image.m_84997_(pixel.x, pixel.y, pixel.width, pixel.height, abgr);
            }
            this.texture = Minecraft.m_91087_().m_91097_().m_118490_("abyss_banner", new DynamicTexture(image));
        }
        graphics.m_280509_(x + width, y + 6, x + width + 4, y + height + 6, 1711740456);
        graphics.m_280509_(x + 4, y + height, x + width, y + height + 6, 1711740456);
        graphics.m_280411_(this.texture, x, y, width, height, 0.0f, 0.0f, 800, 400, 800, 400);
    }

    @Override
    public void close() {
        if (this.texture != null) {
            Minecraft.m_91087_().m_91097_().m_118513_(this.texture);
        }
        this.texture = null;
    }

    /*
     * WARNING - void declaration
     */
    private static List<Pixel> build() {
        void var1_4;
        int x;
        void var1_2;
        ArrayList<Pixel> art = new ArrayList<Pixel>();
        BannerArtwork.add(art, 0, 0, 800, 400, -1708557);
        BannerArtwork.add(art, 442, 0, 358, 400, -13617563);
        int n = 20;
        while (var1_2 < 400) {
            for (x = 18; x < 442; x += 48) {
                BannerArtwork.diamond(art, x, (int)var1_2, 19, -2234385);
                BannerArtwork.diamond(art, x, (int)var1_2, 15, -1708557);
                BannerArtwork.add(art, x - 1, (int)(var1_2 - 7), 2, 14, -2102800);
            }
            var1_2 += 48;
        }
        BannerArtwork.ring(art, 546, 195, 228, 1, -12037248);
        BannerArtwork.ring(art, 546, 195, 244, 1, -12695177);
        boolean bl = false;
        while (var1_4 < 15) {
            x = 465 + var1_4 * 83 % 309;
            int y = 25 + var1_4 * 59 % 350;
            BannerArtwork.star(art, x, y, var1_4 % 3 == false ? 5 : 2, -7823424);
            ++var1_4;
        }
        BannerArtwork.disk(art, 534, 200, 184, -3877406);
        BannerArtwork.ring(art, 534, 200, 184, 7, -920072);
        BannerArtwork.ring(art, 534, 200, 171, 3, -7230006);
        BannerArtwork.ring(art, 534, 200, 164, 2, -1642251);
        BannerArtwork.ring(art, 534, 200, 136, 2, -4928804);
        BannerArtwork.diamond(art, 534, 200, 115, -4994341);
        BannerArtwork.diamond(art, 534, 200, 110, -3877406);
        BannerArtwork.diamond(art, 534, 200, 76, -4599841);
        BannerArtwork.diamond(art, 534, 200, 69, -3877406);
        BannerArtwork.star(art, 534, 200, 40, -5125412);
        for (int[] point : new int[][]{{534, 25}, {534, 375}, {359, 200}, {709, 200}}) {
            BannerArtwork.diamond(art, point[0], point[1], 10, -920072);
            BannerArtwork.diamond(art, point[0], point[1], 5, -8873029);
        }
        for (int inset : new int[]{4, 9}) {
            int color = inset == 4 ? -6638901 : -4666403;
            BannerArtwork.add(art, 18, inset, 764, 1, color);
            BannerArtwork.add(art, 18, 399 - inset, 764, 1, color);
            BannerArtwork.add(art, inset, 18, 1, 364, color);
            BannerArtwork.add(art, 799 - inset, 18, 1, 364, color);
        }
        for (int x2 : new int[]{13, 786}) {
            for (int y : new int[]{13, 386}) {
                BannerArtwork.star(art, x2, y, 5, -6967610);
            }
        }
        return List.copyOf(art);
    }

    private static void add(List<Pixel> art, int x, int y, int width, int height, int color) {
        int right = Math.min(800, x + width);
        int bottom = Math.min(400, y + height);
        x = Math.max(0, x);
        y = Math.max(0, y);
        if (right > x && bottom > y) {
            art.add(new Pixel(x, y, right - x, bottom - y, color));
        }
    }

    private static void disk(List<Pixel> art, int cx, int cy, int radius, int color) {
        for (int y = -radius; y < radius; y += 4) {
            int half = (int)Math.sqrt(Math.max(0, radius * radius - (y + 2) * (y + 2))) / 4 * 4;
            BannerArtwork.add(art, cx - half, cy + y, half * 2, 4, color);
        }
    }

    private static void ring(List<Pixel> art, int cx, int cy, int radius, int thickness, int color) {
        for (int y = -radius; y < radius; y += 4) {
            int half = (int)Math.sqrt(Math.max(0, radius * radius - (y + 2) * (y + 2))) / 4 * 4;
            int inner = (int)Math.sqrt(Math.max(0, (radius - thickness) * (radius - thickness) - (y + 2) * (y + 2))) / 4 * 4;
            int line = Math.max(2, half - inner);
            BannerArtwork.add(art, cx - half, cy + y, line, 4, color);
            BannerArtwork.add(art, cx + half - line, cy + y, line, 4, color);
        }
    }

    private static void diamond(List<Pixel> art, int cx, int cy, int radius, int color) {
        for (int y = -radius; y < radius; y += 3) {
            int half = radius - Math.abs(y);
            BannerArtwork.add(art, cx - half, cy + y, half * 2, 3, color);
        }
    }

    private static void star(List<Pixel> art, int cx, int cy, int radius, int color) {
        BannerArtwork.add(art, cx - 1, cy - radius, 2, 2 * radius + 1, color);
        BannerArtwork.add(art, cx - radius, cy - 1, 2 * radius + 1, 2, color);
        BannerArtwork.diamond(art, cx, cy, Math.max(2, radius / 2), color);
    }

    private record Pixel(int x, int y, int width, int height, int color) {
    }
}

