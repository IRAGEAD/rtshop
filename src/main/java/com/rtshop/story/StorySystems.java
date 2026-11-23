package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 이야기 시스템 설정 (config/rtshop/story_systems.json): 용 소환식, 룬 제단 잠금. */
public final class StorySystems {
    private StorySystems() {
    }

    /** 허공의 용은 처음부터 잠들어 있고, 출구 받침대의 의식으로 불러낸다. */
    public static boolean dragonRite = true;
    /** 이 표식이 있는 사람만 룬 제단(별빛 수정 구체)을 쓸 수 있다. "" = 누구나. */
    public static String altarLockFlag = "";
    private static boolean loaded;

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("story_systems.json");
    }

    public static synchronized void load() {
        loaded = true;
        try {
            Path p = path();
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("dragonRite")) dragonRite = o.get("dragonRite").getAsBoolean();
                if (o.has("altarLockFlag")) altarLockFlag = o.get("altarLockFlag").getAsString().trim();
            } else {
                JsonObject o = new JsonObject();
                o.addProperty("_help", "이야기 시스템 설정. dragonRite: true 면 허공(엔드)의 용이 처음부터 잠들어 있고, 출구 받침대 네 모서리에 수정을 놓는 의식으로 불러낸다 "
                        + "(파티마다 다시 부를 수 있다). altarLockFlag: 이 표식을 가진 사람만 룬 제단(별빛 수정 구체)을 쓸 수 있다. 비우면 누구나. 고친 뒤 /rtshop reload.");
                o.addProperty("dragonRite", dragonRite);
                o.addProperty("altarLockFlag", altarLockFlag);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] story_systems.json 을 읽지 못했습니다: {}", e.getMessage());
        }
    }

    public static void ensure() {
        if (!loaded) load();
    }
}
