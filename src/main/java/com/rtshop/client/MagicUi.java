package com.rtshop.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * 룬 제단(마법 뽑기) 화면의 글자: 글꼴·그림자·크기는 화면 설정(ClientConfig.magic*)을 따른다.
 * 기본은 또렷한 고딕에 그림자·테두리 없이. 제단의 창, 장면 옆 패널, 장면 속 글자, 뽑기 연출이 모두 이것으로 그린다.
 */
public final class MagicUi {
    private MagicUi() {
    }

    public static final String[] FACES = {"sans", "serif", "batang", "pixel"};
    public static final double[] SCALES = {0.8, 0.9, 1.0, 1.1, 1.2};

    private static ResourceLocation rl(String path) {
        return new ResourceLocation("rtshop", path);
    }

    private static String face() {
        String f = ClientConfig.get().magicFont;
        for (String k : FACES) if (k.equals(f)) return k;
        return "sans";
    }

    /** 본문 글꼴. */
    public static ResourceLocation body() {
        return switch (face()) {
            case "serif" -> rl("ui_serif");
            case "batang" -> RaidText.SANS;
            case "pixel" -> Style.DEFAULT_FONT;
            default -> rl("ui_sans");
        };
    }

    /** 머리말 글꼴 (조금 큰 글자). */
    public static ResourceLocation head() {
        return switch (face()) {
            case "serif" -> rl("ui_serif_l");
            case "batang" -> RaidText.SERIF;
            case "pixel" -> Style.DEFAULT_FONT;
            default -> rl("ui_sans_l");
        };
    }

    /** 제목 글꼴 (큰 글자). */
    public static ResourceLocation title() {
        return switch (face()) {
            case "serif" -> rl("magic_title_serif");
            case "batang" -> RaidText.TITLE;
            case "pixel" -> Style.DEFAULT_FONT;
            default -> rl("magic_title");
        };
    }

    public static boolean shadow() {
        return ClientConfig.get().magicShadow;
    }

    public static float scale() {
        return (float) Math.max(0.7, Math.min(1.3, ClientConfig.get().magicScale));
    }

    public static Component c(String s, ResourceLocation f) {
        return Component.literal(s).withStyle(st -> st.withFont(f));
    }

    public static int w(Font font, String s, ResourceLocation f) {
        return font.width(c(s, f));
    }

    /** 글자를 그린다. 그림자는 화면 설정에서 켠 경우에만 (RT Shop 창의 '그림자 없음' 규칙을 잠시 멈추고 그린다). */
    public static void draw(GuiGraphics g, Font font, String s, ResourceLocation f, int x, int y, int color) {
        draw(g, font, c(s, f).getVisualOrderText(), x, y, color);
    }

    public static void draw(GuiGraphics g, Font font, FormattedCharSequence s, int x, int y, int color) {
        if (!shadow()) {
            g.drawString(font, s, x, y, color, false);
            return;
        }
        boolean was = UiText.pause();
        try {
            g.drawString(font, s, x, y, color, true);
        } finally {
            UiText.resume(was);
        }
    }

    public static String clip(Font font, String s, ResourceLocation f, int width) {
        return RaidText.clip(font, s, f, Math.max(8, width));
    }

    public static List<FormattedCharSequence> split(Font font, String s, ResourceLocation f, int width) {
        return font.split(c(s, f), width);
    }
}
