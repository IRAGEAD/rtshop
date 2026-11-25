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
    // 중세풍 질감 (tools/gui_textures.py atlas 로 만든 픽셀 그림)
    public static final ResourceLocation LEATHER = new ResourceLocation(RtShop.MODID, "textures/gui/medieval/leather.png");
    public static final ResourceLocation PARCHMENT = new ResourceLocation(RtShop.MODID, "textures/gui/medieval/parchment.png");
    public static final ResourceLocation CORNERS = new ResourceLocation(RtShop.MODID, "textures/gui/medieval/leather_corners.png");
    public static final ResourceLocation BUTTON = new ResourceLocation(RtShop.MODID, "textures/gui/medieval/button.png");
    public static final ResourceLocation SEAL = new ResourceLocation(RtShop.MODID, "textures/gui/medieval/seal.png");
    private static final int STITCH = 0xD8E6CC94;

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
    public static final int C_CREAM = 0xFFE9DCBC;
    public static final int C_RED = 0xFFA02C1E;
    public static final int C_GREEN = 0xFF2F6B24;

    /** 질감을 이어 붙여 칠한다 (ts = 질감 한 장의 크기). */
    public static void tile(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int ts) {
        for (int yy = 0; yy < h; yy += ts) {
            for (int xx = 0; xx < w; xx += ts) {
                g.blit(tex, x + xx, y + yy, 0.0F, 0.0F, Math.min(ts, w - xx), Math.min(ts, h - yy), ts, ts);
            }
        }
    }

    private static boolean parchmentFill(int fill) {
        return fill == C_PARCH || fill == C_PARCH_DARK || fill == C_PARCH_HI;
    }

    /** 올록볼록한 판. 양피지 색이면 양피지 질감을 깐다. */
    public static void bevel(GuiGraphics g, int x, int y, int w, int h, int fill, int hi, int lo) {
        g.fill(x, y, x + w, y + h, lo);
        g.fill(x, y, x + w - 1, y + h - 1, hi);
        if (parchmentFill(fill) && w >= 10 && h >= 8) {
            float c = fill == C_PARCH_DARK ? 0.9F : fill == C_PARCH_HI ? 1.0F : 0.97F;
            RenderSystem.setShaderColor(c, c * (fill == C_PARCH_DARK ? 0.98F : 1F), c * (fill == C_PARCH_DARK ? 0.94F : 1F), 1F);
            tile(g, PARCHMENT, x + 1, y + 1, w - 2, h - 2, 128);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            if (fill == C_PARCH_HI) g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x22FFFFFF);
            // 가장자리가 누렇게 바랜 양피지
            if (w >= 40 && h >= 30) {
                int e = 0x24724A1C;
                g.fillGradient(x + 1, y + 1, x + w - 1, y + 5, e, 0);
                g.fillGradient(x + 1, y + h - 5, x + w - 1, y + h - 1, 0, e);
                for (int k = 0; k < 4; k++) {
                    int a = (0x24 * (4 - k) / 4) << 24 | 0x724A1C;
                    g.fill(x + 1 + k, y + 1, x + 2 + k, y + h - 1, a);
                    g.fill(x + w - 2 - k, y + 1, x + w - 1 - k, y + h - 1, a);
                }
            }
            return;
        }
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static void stitchH(GuiGraphics g, int x0, int x1, int y) {
        for (int x = x0; x < x1 - 1; x += 4) {
            g.fill(x, y, x + 2, y + 1, STITCH);
            g.fill(x, y + 1, x + 2, y + 2, 0x55000000);
        }
    }

    private static void stitchV(GuiGraphics g, int x, int y0, int y1) {
        for (int y = y0; y < y1 - 1; y += 4) {
            g.fill(x, y, x + 1, y + 2, STITCH);
            g.fill(x + 1, y, x + 2, y + 2, 0x55000000);
        }
    }

    /** 창 틀: 눌러 새긴 테두리 홈만 있는 짙은 가죽 장정 (바느질·모서리 장식 없이 깔끔하게). */
    public static void woodFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 3, y - 2, x + w + 3, y + h + 4, 0x66000000);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, C_SHADOW);
        tile(g, LEATHER, x, y, w, h, 128);
        // 닳은 가장자리와 입체감
        g.fill(x, y, x + w, y + 1, 0x50FFD9A0);
        g.fill(x, y, x + 1, y + h, 0x40FFD9A0);
        g.fill(x, y + h - 1, x + w, y + h, 0x80000000);
        g.fill(x + w - 1, y, x + w, y + h, 0x70000000);
        // 눌러 새긴 테두리 홈
        int k = 5;
        g.fill(x + k, y + k, x + w - k, y + k + 1, 0x70000000);
        g.fill(x + k, y + h - k - 1, x + w - k, y + h - k, 0x30FFD9A0);
        g.fill(x + k, y + k, x + k + 1, y + h - k, 0x70000000);
        g.fill(x + w - k - 1, y + k, x + w - k, y + h - k, 0x30FFD9A0);
    }

    /** 제목 띠: 더 짙은 가죽, 아래 금박 선 (바느질·징 없이). */
    public static void titleBand(GuiGraphics g, int x0, int y0, int x1, int y1) {
        RenderSystem.setShaderColor(0.62F, 0.58F, 0.55F, 1F);
        tile(g, LEATHER, x0, y0, x1 - x0, y1 - y0, 128);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.fill(x0, y0, x1, y0 + 1, 0x90000000);
        g.fill(x0, y1 - 1, x1, y1, 0x40FFD9A0);
    }

    /** 단추: 예전처럼 깔끔한 나무 판 (밝은 윗선·어두운 아랫선), 마우스를 올리면 밝아지고 금테가 둘린다. */
    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean hover, boolean enabled) {
        // 깔끔한 가죽 버튼 (64x48 텍스처, 3단 16px: 보통 / 마우스 / 꺼짐) 9조각 늘리기
        int v = !enabled ? 32 : (hover ? 16 : 0);
        int c = Math.min(4, Math.min(w / 2, h / 2));
        int midW = 64 - 2 * c, midH = 16 - 2 * c;
        for (int row = 0; row < 3; row++) {
            int ty, th, tv;
            if (row == 0) { ty = y; th = c; tv = v; }
            else if (row == 2) { ty = y + h - c; th = c; tv = v + 16 - c; }
            else { ty = y + c; th = h - 2 * c; tv = v + c; }
            if (th <= 0) continue;
            for (int yy = 0; yy < th; yy += midH) {
                int hh = row == 1 ? Math.min(midH, th - yy) : th;
                g.blit(BUTTON, x, ty + yy, 0, tv, c, hh, 64, 48);
                g.blit(BUTTON, x + w - c, ty + yy, 64 - c, tv, c, hh, 64, 48);
                for (int xx = c; xx < w - c; xx += midW) {
                    int ww = Math.min(midW, w - c - xx);
                    g.blit(BUTTON, x + xx, ty + yy, c, tv, ww, hh, 64, 48);
                }
                if (row != 1) break;
            }
        }
        String s = clip(font, label, w - 8);
        int tx = x + (w - font.width(s)) / 2, tyy = y + (h - 8) / 2;
        if (enabled) {
            // 그림자 없이, 눈부시지 않은 크림색
            g.drawString(font, s, tx, tyy, hover ? 0xFFF2E4C4 : 0xFFE2D3B2, false);
        } else {
            g.drawString(font, s, tx, tyy, 0xFFB8A888, false);
        }
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
