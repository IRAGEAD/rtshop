package com.rtshop.land;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** config/rtshop/land.json */
public final class LandConfig {
    /** 토지 문서를 쓸 때 발 아래로 포함되는 깊이 (16칸 토지 기준). 큰 토지는 2배. */
    public int depthBelowFeet = 4;
    /** 한 사람이 가질 수 있는 토지 구역 수 (0 = 제한 없음). */
    public int maxClaimsPerPlayer = 0;
    /** 폭발로부터 토지 안의 블록을 보호. */
    public boolean protectFromExplosions = true;
    /** 토지 밖의 피스톤이 토지 안의 블록을 밀거나 당기지 못하게 함. */
    public boolean protectFromPistons = true;
    public int pinMinLength = 4;
    public int pinMaxLength = 8;
    /** PIN 을 맞게 입력한 뒤 다시 묻지 않는 시간(분). */
    public int pinUnlockMinutes = 5;
    public int pinMaxAttempts = 5;
    public int pinLockoutSeconds = 60;
    /** 다른 사람의 토지 근처(블록)에는 토지를 만들 수 없음 (0 = 바로 옆에도 가능). */
    public int minDistanceToOthers = 0;
    /** 넓은 범위에 작용하는 블록: 남의 토지에서 이 거리(블록) 안에는 놓을 수 없다. */
    public final Map<String, Integer> rangedBlocks = new LinkedHashMap<>();

    public LandConfig() {
        rangedBlocks.put("better_farming_plus:farming_crate", 5);
    }

    private static LandConfig instance;

    public static synchronized LandConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized LandConfig reload() {
        instance = load();
        return instance;
    }

    private static LandConfig load() {
        LandConfig c = new LandConfig();
        Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("land.json");
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("depthBelowFeet")) c.depthBelowFeet = Math.max(0, Math.min(15, o.get("depthBelowFeet").getAsInt()));
                if (o.has("maxClaimsPerPlayer")) c.maxClaimsPerPlayer = Math.max(0, o.get("maxClaimsPerPlayer").getAsInt());
                if (o.has("protectFromExplosions")) c.protectFromExplosions = o.get("protectFromExplosions").getAsBoolean();
                if (o.has("protectFromPistons")) c.protectFromPistons = o.get("protectFromPistons").getAsBoolean();
                if (o.has("pinMinLength")) c.pinMinLength = Math.max(1, o.get("pinMinLength").getAsInt());
                if (o.has("pinMaxLength")) c.pinMaxLength = Math.max(c.pinMinLength, Math.min(16, o.get("pinMaxLength").getAsInt()));
                if (o.has("pinUnlockMinutes")) c.pinUnlockMinutes = Math.max(0, o.get("pinUnlockMinutes").getAsInt());
                if (o.has("pinMaxAttempts")) c.pinMaxAttempts = Math.max(1, o.get("pinMaxAttempts").getAsInt());
                if (o.has("pinLockoutSeconds")) c.pinLockoutSeconds = Math.max(0, o.get("pinLockoutSeconds").getAsInt());
                if (o.has("minDistanceToOthers")) c.minDistanceToOthers = Math.max(0, o.get("minDistanceToOthers").getAsInt());
                if (o.has("rangedBlocks") && o.get("rangedBlocks").isJsonObject()) {
                    c.rangedBlocks.clear();
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("rangedBlocks").entrySet()) {
                        c.rangedBlocks.put(e.getKey(), Math.max(0, Math.min(32, e.getValue().getAsInt())));
                    }
                }
            } else {
                JsonObject o = new JsonObject();
                JsonArray help = new JsonArray();
                help.add("depthBelowFeet: 문서를 쓸 때 발 아래로 포함되는 깊이(16칸 토지 기준, 큰 토지는 2배)");
                help.add("maxClaimsPerPlayer: 1인당 토지 구역 수 제한 (0 = 무제한) · minDistanceToOthers: 남의 토지와 떨어져야 하는 거리");
                help.add("pinUnlockMinutes: PIN 을 맞힌 뒤 다시 묻지 않는 시간 · pinMaxAttempts/pinLockoutSeconds: 틀린 횟수 제한과 잠금 시간");
                help.add("rangedBlocks: 넓은 범위에 작용하는 블록(자동 수확 상자 등). 남의 토지에서 이 거리 안에는 놓을 수 없음");
                o.add("_설명", help);
                o.addProperty("depthBelowFeet", c.depthBelowFeet);
                o.addProperty("maxClaimsPerPlayer", c.maxClaimsPerPlayer);
                o.addProperty("minDistanceToOthers", c.minDistanceToOthers);
                o.addProperty("protectFromExplosions", c.protectFromExplosions);
                o.addProperty("protectFromPistons", c.protectFromPistons);
                o.addProperty("pinMinLength", c.pinMinLength);
                o.addProperty("pinMaxLength", c.pinMaxLength);
                o.addProperty("pinUnlockMinutes", c.pinUnlockMinutes);
                o.addProperty("pinMaxAttempts", c.pinMaxAttempts);
                o.addProperty("pinLockoutSeconds", c.pinLockoutSeconds);
                JsonObject ranged = new JsonObject();
                c.rangedBlocks.forEach(ranged::addProperty);
                o.add("rangedBlocks", ranged);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] land.json 을 읽지 못해 기본값을 사용합니다.", ex);
        }
        return c;
    }
}
