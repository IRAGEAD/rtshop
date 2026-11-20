package com.rtshop.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rtshop.RtShop;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** 화면 공통 그리기 도구와 중세풍 색상표(갈색 + 크림색). */
public final class Ui {
    private Ui() {
    }

    public static final ResourceLocation COIN = new ResourceLocation(RtShop.MODID, "textures/gui/gold_coin.png");

    public static final int C_SHADOW = 0xFF140B05;
    public static final int C_WOOD = 0xFF5C3A1E;
    public static final int C_WOOD_HI = 0xFF8A5A30;
    public static final int C_WOOD_DARK = 0xFF3E2512;
    public static final int C_GOLD = 0xFFE8B84A;
    public static final int C_GOLD_DARK = 0xFFA87A22;
    public static final int C_PARCH = 0xFFEADBB5;
    public static final int C_PARCH_DARK = 0xFFD9C592;
    public static final int C_PARCH_SHADE = 0xFFBFA472;
    public static final int C_PARCH_HI = 0xFFF6EBCB;
    public static final int C_INK = 0xFF3A2814;
    public static final int C_INK_SOFT = 0xFF7A6240;
    public static final int C_CREAM = 0xFFF5E6C0;
    public static final int C_RED = 0xFFA02C1E;
    public static final int C_GREEN = 0xFF2F6B24;

    public static void bevel(GuiGraphics g, int x, int y, int w, int h, int fill, int hi, int lo) {
        g.fill(x, y, x + w, y + h, lo);
        g.fill(x, y, x + w - 1, y + h - 1, hi);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** 나무 틀(그림자, 나뭇결, 모서리 금속 장식 포함). */
    public static void woodFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, C_SHADOW);
        g.fill(x, y, x + w, y + h, C_WOOD);
        for (int yy = y + 3; yy < y + h - 2; yy += 5) {
            g.fill(x + 1, yy, x + w - 1, yy + 1, 0x22000000);
        }
        g.fill(x, y, x + w, y + 1, C_WOOD_HI);
        g.fill(x, y, x + 1, y + h, C_WOOD_HI);
        int[][] corners = {{x + 2, y + 2}, {x + w - 5, y + 2}, {x + 2, y + h - 5}, {x + w - 5, y + h - 5}};
        for (int[] c : corners) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, C_GOLD_DARK);
            g.fill(c[0], c[1], c[0] + 2, c[1] + 2, C_GOLD);
        }
    }

    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean hover, boolean enabled) {
        int fill = !enabled ? C_WOOD_DARK : (hover ? C_WOOD_HI : C_WOOD);
        bevel(g, x, y, w, h, fill, C_WOOD_HI, C_SHADOW);
        String s = clip(font, label, w - 6);
        g.drawString(font, s, x + (w - font.width(s)) / 2, y + (h - 8) / 2, enabled ? C_CREAM : C_INK_SOFT, false);
    }

    /** G 금화 아이콘을 size×size 크기로 그립니다. */
    public static void coin(GuiGraphics g, int x, int y, int size) {
        RenderSystem.enableBlend();
        g.blit(COIN, x, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
    }

    public static void scaledText(GuiGraphics g, Font font, String text, float x, float y, float scale, int color, boolean shadow) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0.0F);
        ps.scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, shadow);
        ps.popPose();
    }

    public static String clip(Font font, String s, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(s) <= maxWidth) {
            return s;
        }
        String ell = "…";
        return font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width(ell))) + ell;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static String num(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    public static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 1.0e-9) {
            return num((long) Math.rint(v));
        }
        return String.format(Locale.ROOT, "%,.1f", v);
    }

    public static String compact(double v) {
        if (v >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", v / 1_000_000.0);
        }
        if (v >= 10_000) {
            return (long) (v / 1_000) + "k";
        }
        if (Math.abs(v - Math.rint(v)) < 1.0e-9) {
            return Long.toString((long) Math.rint(v));
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
