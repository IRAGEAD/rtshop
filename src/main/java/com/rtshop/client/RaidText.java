package com.rtshop.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * 레이드 화면 전용 글꼴 「고딕 필사본」: 제목·머리말은 굵은 명조(나눔명조 ExtraBold), 본문은 굵은 바탕체(고운바탕 Bold),
 * 영문 장식은 고딕 필사체(UnifrakturMaguntia). 모두 SIL OFL 글꼴. 제목은 금빛 각인(그라데이션·테두리·입체·핏빛 불빛)으로 그린다.
 * 글꼴에 없는 글자(기호, 드문 한글)는 마인크래프트 기본 글꼴로 자연스럽게 넘어간다.
 */
public final class RaidText {
    private RaidText() {
    }

    public static final ResourceLocation TITLE = new ResourceLocation("rtshop", "raid_title");
    public static final ResourceLocation SERIF = new ResourceLocation("rtshop", "raid_myeongjo");
    public static final ResourceLocation SANS = new ResourceLocation("rtshop", "raid_batang");
    public static final ResourceLocation GOTHIC = new ResourceLocation("rtshop", "raid_gothic");

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

    /**
     * 금빛 각인 글자: 핏빛 불빛 → 아래쪽 두께 → 어두운 테두리 → 위에서 아래로 밝은 금·짙은 금 띠(그라데이션) → 윗변 반짝임.
     * 글자 높이 h 는 글꼴 크기(제목 13, 머리말 9.4)에 맞춘다.
     */
    public static void engraved(GuiGraphics g, Font font, String s, ResourceLocation f, int x, int y, int h) {
        Component t = c(s, f);
        int w = font.width(t);
        // 두께 (아래로 두 칸)
        g.drawString(font, t, x + 1, y + 2, 0xFF2A0A04, false);
        g.drawString(font, t, x, y + 2, 0xFF3A1206, false);
        // 테두리
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) g.drawString(font, t, x + dx, y + dy, 0xE0140404, false);
            }
        }
        // 금 띠: 위(밝음) · 가운데(짙은 줄) · 아래(구리빛)
        int top = y - 2, bot = y + h + 2;
        int a = y + Math.round(h * 0.45F), b = y + Math.round(h * 0.58F);
        int[][] bands = {{top, a, 0xFFFFF0C8}, {a, b, 0xFFB07428}, {b, bot, 0xFFE0A850}};
        for (int[] band : bands) {
            if (band[1] <= band[0]) continue;
            g.enableScissor(x - 2, band[0], x + w + 2, band[1]);
            g.drawString(font, t, x, y, band[2], false);
            g.disableScissor();
        }
        // 아래쪽 끝은 조금 더 어둡게 (금속의 그늘)
        g.enableScissor(x - 2, y + Math.round(h * 0.85F), x + w + 2, bot);
        g.drawString(font, t, x, y, 0xFFB8782C, false);
        g.disableScissor();
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
