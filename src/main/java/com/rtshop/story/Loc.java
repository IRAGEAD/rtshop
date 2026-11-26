package com.rtshop.story;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * 이야기 글의 언어: 문자열이면 그대로, {"ko": "...", "en": "..."} 이면 그 플레이어가 고른 이야기 언어로.
 * 플레이어의 이야기 언어는 클라이언트가 접속할 때·바꿀 때 알려 준다 (게임 언어와 따로 고를 수 있다). 언어는 StoryText 가 맡는다.
 */
public final class Loc {
    private Loc() {
    }

    /**
     * 옛 명령(/rtshop talk lang)으로 언어를 정한다. 3.64: 이야기 언어는 StoryText 하나로 합쳤다
     * (전에는 여기에 따로 적어 두었는데 화면 쪽은 StoryText 에만 알려서, 두 언어를 나란히 적은 글 — 직업 사범 다섯 명의 대사 전부와
     * 몇몇 이야기 주제 — 은 영어로 하는 사람에게도 늘 한국어로 나갔다).
     */
    public static void set(ServerPlayer p, String lang) {
        StoryText.setLang(p, lang != null && lang.trim().toLowerCase(java.util.Locale.ROOT).startsWith("en") ? "en_us" : "ko_kr");
    }

    /** 이 플레이어의 이야기 언어를 두 글자로: "ko" 또는 "en" (StoryText 가 아는 언어를 그대로 따른다). */
    public static String lang(ServerPlayer p) {
        return StoryText.en(p) ? "en" : "ko";
    }

    public static String pick(ServerPlayer p, JsonElement e) {
        if (e == null || e.isJsonNull()) return "";
        if (e.isJsonPrimitive()) return e.getAsString();
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            String l = lang(p);
            if (o.has(l)) return o.get(l).getAsString();
            if (o.has("ko")) return o.get("ko").getAsString();
            for (Map.Entry<String, JsonElement> x : o.entrySet()) return x.getValue().getAsString();
        }
        return e.toString();
    }
}
