package com.rtshop.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HUD 마다 게임 언어와 따로 고르는 언어. "" 이면 게임 언어(Component.translatable)를 그대로 쓰고,
 * ko_kr / en_us 면 이 모드의 번역 파일에서 직접 읽어 쓴다.
 */
public final class HudLang {
    private HudLang() {
    }

    /** 고를 수 있는 언어 ("" = 게임 언어를 따름). */
    public static final String[] LANGS = {"", "ko_kr", "en_us"};
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    public static String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase(Locale.ROOT);
        for (String l : LANGS) if (l.equals(s)) return s;
        return "";
    }

    /** 언어 이름 (그 언어로): 게임 언어 / 한국어 / English */
    public static String label(String lang) {
        return switch (normalize(lang)) {
            case "ko_kr" -> "한국어";
            case "en_us" -> "English";
            default -> Component.translatable("rtshop.hud.lang_game").getString();
        };
    }

    private static synchronized Map<String, String> table(String lang) {
        return CACHE.computeIfAbsent(lang, l -> {
            Map<String, String> m = new HashMap<>();
            try {
                var res = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(RtShop.MODID, "lang/" + l + ".json"));
                if (res.isPresent()) {
                    try (var in = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                        JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> e : o.entrySet()) m.put(e.getKey(), e.getValue().getAsString());
                    }
                }
            } catch (Exception ex) {
                RtShop.LOGGER.warn("[rtshop] HUD 언어 {} 를 읽지 못했습니다: {}", l, ex.toString());
            }
            return m;
        });
    }

    /** 리소스를 다시 읽을 때 (F3+T 등) */
    public static synchronized void clear() {
        CACHE.clear();
    }

    /** key 를 고른 언어로. 그 언어 번역이 없으면 게임 언어로. */
    public static String tr(String lang, String key, Object... args) {
        lang = normalize(lang);
        if (lang.isEmpty()) return Component.translatable(key, args).getString();
        String fmt = table(lang).get(key);
        if (fmt == null) return Component.translatable(key, args).getString();
        Object[] a = new Object[args.length];
        for (int i = 0; i < args.length; i++) a[i] = args[i] instanceof Component c ? c.getString() : args[i];
        try {
            return String.format(Locale.ROOT, fmt, a);
        } catch (Exception e) {
            return fmt;
        }
    }

    /** 지금 고른 이야기 언어 코드 (서버에 알리는 것과 같은 기준: 비어 있으면 게임 언어). */
    public static String storyCode() {
        String l = normalize(ClientConfig.get().storyLang);
        if (l.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            l = mc.options == null || mc.options.languageCode == null ? "ko_kr" : mc.options.languageCode.toLowerCase(Locale.ROOT);
        }
        return l;
    }

    /** 이야기 언어가 한국어가 아닌가. */
    public static boolean storyEn() {
        return !storyCode().startsWith("ko");
    }

    /** 서버에 내 스토리 언어를 알린다 (접속할 때와 바꿀 때). */
    public static void sendStoryLang() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        String l = normalize(ClientConfig.get().storyLang);
        // "게임 언어를 따름" 이면 지금 게임 언어를 그대로 보낸다
        if (l.isEmpty()) l = mc.options.languageCode == null ? "ko_kr" : mc.options.languageCode.toLowerCase(Locale.ROOT);
        mc.player.connection.sendCommand("rtshop storylang " + l.replaceAll("[^a-z_]", ""));
    }
}
