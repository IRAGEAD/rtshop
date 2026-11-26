package com.rtshop.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.rtshop.RtShop;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스토리 대사 언어 (게임 언어와 따로). 스토리 원문은 한국어이고, 다른 언어는 "원문 → 번역" 표로 바꿔 보낸다.
 * 표: 모드 안의 rtshop_defaults/story_lang/&lt;언어&gt;.json + config/rtshop/story_lang/&lt;언어&gt;.json (덮어쓰기·추가).
 * 번역이 없는 문장(게임 안 편집기로 고친 문장 등)은 원문 그대로 나간다.
 */
public final class StoryText {
    private StoryText() {
    }

    public static final String TAG = "rtshop_storylang";
    private static final Map<String, Map<String, String>> TABLES = new ConcurrentHashMap<>();

    /** 플레이어가 고른 스토리 언어 (클라이언트가 접속할 때 알려 준다). 모르면 ko_kr. */
    public static String langOf(ServerPlayer p) {
        if (p == null) return "ko_kr";
        String s = p.getPersistentData().getString(TAG);
        return s.isEmpty() ? "ko_kr" : s;
    }

    public static void setLang(ServerPlayer p, String code) {
        code = code == null ? "" : code.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z_]", "");
        if (code.isEmpty()) p.getPersistentData().remove(TAG);
        else p.getPersistentData().putString(TAG, code);
    }

    /** 이 언어로 바꿀 표 (한국어면 null: 원문 그대로). 한국어가 아닌 언어는 영어 표를 쓴다. */
    static Map<String, String> table(String lang) {
        if (lang == null || lang.startsWith("ko")) return null;
        String file = "en_us";
        return TABLES.computeIfAbsent(file, StoryText::load);
    }

    private static Map<String, String> load(String file) {
        Map<String, String> m = new HashMap<>();
        try (InputStream in = StoryText.class.getResourceAsStream("/rtshop_defaults/story_lang/" + file + ".json")) {
            if (in != null) read(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject(), m);
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] 스토리 번역 {} 을 읽지 못했습니다: {}", file, e.toString());
        }
        Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("story_lang").resolve(file + ".json");
        if (Files.exists(p)) {
            try {
                read(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject(), m);
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] {} 을 읽지 못했습니다: {}", p, e.toString());
            }
        }
        return m;
    }

    private static void read(JsonObject o, Map<String, String> m) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (e.getKey().startsWith("_") || !e.getValue().isJsonPrimitive()) continue;
            m.put(e.getKey(), e.getValue().getAsString());
        }
    }

    /** /rtshop reload 때 다시 읽는다. */
    public static void reload() {
        TABLES.clear();
    }

    public static int sizeForTest(String lang) {
        Map<String, String> t = table(lang);
        return t == null ? 0 : t.size();
    }

    /** 한 문장을 그 플레이어의 스토리 언어로. */
    public static String t(ServerPlayer p, String s) {
        if (s == null || s.isEmpty()) return s;
        Map<String, String> t = table(langOf(p));
        if (t == null) return s;
        String r = t.get(s);
        return r == null ? s : r;
    }

    /** 이 플레이어가 한국어가 아닌 언어(영어 표)로 보는가. */
    public static boolean en(ServerPlayer p) {
        return p != null && !langOf(p).startsWith("ko");
    }

    /** 코드에 박힌 알림: 한국어 원문과 영어를 함께 적어 두고 그 플레이어의 언어로 고른다. */
    public static String pick(ServerPlayer p, String ko, String en) {
        return en(p) ? en : ko;
    }

    /** 서식(%s)이 든 설정 문구: 먼저 번역하고 그다음에 값을 끼운다. */
    public static String f(ServerPlayer p, String template, Object... args) {
        String v = t(p, template);
        if (args == null || args.length == 0) return v;
        try {
            return String.format(v, args);
        } catch (Exception e) {
            return v;
        }
    }

    /** 언어 코드로 바로 (클라이언트 화면처럼 플레이어 객체가 없는 곳). */
    public static String tl(String lang, String s) {
        if (s == null || s.isEmpty()) return s;
        Map<String, String> t = table(lang == null || lang.isEmpty() ? "ko_kr" : lang.toLowerCase(Locale.ROOT));
        if (t == null) return s;
        String r = t.get(s);
        return r == null ? s : r;
    }

    /** 영어 표에 이 원문의 번역이 있는가 (자체 점검용). */
    public static boolean hasEnglish(String s) {
        Map<String, String> t = table("en_us");
        return s == null || s.isEmpty() || (t != null && t.containsKey(s));
    }

    /** JSON 안의 모든 문자열 값을 그 플레이어의 스토리 언어로 (키·식별자 같은 번역 없는 값은 그대로). */
    public static JsonElement json(ServerPlayer p, JsonElement e) {
        Map<String, String> t = table(langOf(p));
        if (t == null || e == null) return e;
        return walk(t, e);
    }

    private static JsonElement walk(Map<String, String> t, JsonElement e) {
        if (e.isJsonObject()) {
            JsonObject o = new JsonObject();
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) o.add(en.getKey(), walk(t, en.getValue()));
            return o;
        }
        if (e.isJsonArray()) {
            JsonArray a = new JsonArray();
            for (JsonElement x : e.getAsJsonArray()) a.add(walk(t, x));
            return a;
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            String r = t.get(e.getAsString());
            return r == null ? e : new JsonPrimitive(r);
        }
        return e;
    }
}
