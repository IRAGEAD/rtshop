package com.rtshop.story;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이야기 글의 언어: 문자열이면 그대로, {"ko": "...", "en": "..."} 이면 그 플레이어가 고른 이야기 언어로.
 * 플레이어의 이야기 언어는 클라이언트가 접속할 때·바꿀 때 알려 준다 (게임 언어와 따로 고를 수 있다).
 */
public final class Loc {
    private Loc() {
    }

    private static final Map<UUID, String> LANG = new ConcurrentHashMap<>();

    public static void set(ServerPlayer p, String lang) {
        LANG.put(p.getUUID(), lang == null || lang.isBlank() ? "ko" : lang);
    }

    public static String lang(ServerPlayer p) {
        return p == null ? "ko" : LANG.getOrDefault(p.getUUID(), "ko");
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
