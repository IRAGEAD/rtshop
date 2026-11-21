package com.rtshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD 글꼴. 게임과 리소스팩·모드가 가진 글꼴(assets/&lt;ns&gt;/font/*.json)을 찾아 고를 수 있게 하고,
 * 고른 글꼴로 글을 재고(폭), 자르고, 그린다. "" 는 게임 기본 글꼴.
 */
public final class HudFonts {
    private HudFonts() {
    }

    private static List<String> cache;

    /** 고를 수 있는 글꼴 ID 목록 (기본 글꼴 "" 이 맨 앞). 읽을 수 없는 룬 문자 글꼴과 조각 파일(include/)은 뺀다. */
    public static synchronized List<String> available() {
        if (cache != null) {
            return cache;
        }
        List<String> out = new ArrayList<>();
        out.add("");
        try {
            List<String> found = new ArrayList<>();
            Minecraft.getInstance().getResourceManager().listResources("font", rl -> rl.getPath().endsWith(".json")).keySet().forEach(rl -> {
                String path = rl.getPath().substring("font/".length(), rl.getPath().length() - ".json".length());
                String id = rl.getNamespace() + ":" + path;
                if (path.startsWith("include/") || id.equals("minecraft:alt") || id.equals("minecraft:illageralt")
                        || id.equals("minecraft:default") || found.contains(id)) {
                    return;
                }
                found.add(id);
            });
            found.sort(String::compareTo);
            out.addAll(found);
        } catch (Exception ignored) {
            // 리소스를 못 읽으면 기본 글꼴만
        }
        cache = out;
        return out;
    }

    /** 리소스팩이 바뀌면 다시 찾는다. */
    public static synchronized void refresh() {
        cache = null;
    }

    public static String label(String id) {
        if (id == null || id.isEmpty()) {
            return Component.translatable("rtshop.hud.font_default").getString();
        }
        if (id.equals("minecraft:uniform")) {
            return Component.translatable("rtshop.hud.font_uniform").getString();
        }
        return id;
    }

    /** 실제로 쓸 글꼴: 없어진 글꼴이면 기본 글꼴. */
    public static String effective(String id) {
        if (id == null || id.isEmpty() || !available().contains(id)) {
            return "";
        }
        return id;
    }

    public static MutableComponent text(String s, String font) {
        MutableComponent c = Component.literal(s);
        String f = effective(font);
        if (!f.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(f);
            if (rl != null) {
                c = c.withStyle(Style.EMPTY.withFont(rl));
            }
        }
        return c;
    }

    public static int width(Font font, String s, String fontId) {
        return font.width(text(s, fontId));
    }

    /** 폭에 맞게 잘라 줄임표를 붙인다. */
    public static String clip(Font font, String s, int maxWidth, String fontId) {
        if (maxWidth <= 0) {
            return "";
        }
        if (width(font, s, fontId) <= maxWidth) {
            return s;
        }
        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (width(font, s.substring(0, mid) + "…", fontId) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return s.substring(0, lo) + "…";
    }

    public static void draw(GuiGraphics g, Font font, String s, int x, int y, int color, boolean shadow, String fontId) {
        FormattedText t = text(s, fontId);
        g.drawString(font, Language.getInstance().getVisualOrder(t), x, y, color, shadow);
    }
}
