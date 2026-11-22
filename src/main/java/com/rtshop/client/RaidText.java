package com.rtshop.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * 레이드 화면 전용 글꼴: 제목·머리말은 명조(Noto Serif CJK KR Bold), 본문은 고딕(Noto Sans CJK KR Medium).
 * 글꼴에 없는 글자(기호, 드문 한글)는 마인크래프트 기본 글꼴로 자연스럽게 넘어간다.
 */
public final class RaidText {
    private RaidText() {
    }

    public static final ResourceLocation TITLE = new ResourceLocation("rtshop", "raid_title");
    public static final ResourceLocation SERIF = new ResourceLocation("rtshop", "raid_serif");
    public static final ResourceLocation SANS = new ResourceLocation("rtshop", "raid_sans");

    public static Component c(String s, ResourceLocation f) {
        return Component.literal(s).withStyle(st -> st.withFont(f));
    }

    public static int w(Font font, String s, ResourceLocation f) {
        return font.width(c(s, f));
    }

    public static void draw(GuiGraphics g, Font font, String s, ResourceLocation f, int x, int y, int color) {
        g.drawString(font, c(s, f), x, y, color, false);
    }

    public static void center(GuiGraphics g, Font font, String s, ResourceLocation f, int cx, int y, int color) {
        draw(g, font, s, f, cx - w(font, s, f) / 2, y, color);
    }

    public static void right(GuiGraphics g, Font font, String s, ResourceLocation f, int rx, int y, int color) {
        draw(g, font, s, f, rx - w(font, s, f), y, color);
    }

    /** 어두운 테두리를 두른 글자 (밝은 배경 위에서도 또렷하다). */
    public static void outlined(GuiGraphics g, Font font, String s, ResourceLocation f, int x, int y, int color, int outline) {
        Component t = c(s, f);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) g.drawString(font, t, x + dx, y + dy, outline, false);
            }
        }
        g.drawString(font, t, x, y, color, false);
    }

    /** 폭에 맞춰 자르고 말줄임표를 붙인다. */
    public static String clip(Font font, String s, ResourceLocation f, int width) {
        if (w(font, s, f) <= width) return s;
        String e = "…";
        int ew = w(font, e, f);
        int n = s.length();
        while (n > 0 && w(font, s.substring(0, n), f) + ew > width) n--;
        return s.substring(0, n) + e;
    }

    public static List<FormattedCharSequence> split(Font font, String s, ResourceLocation f, int width) {
        return font.split(c(s, f), width);
    }
}
