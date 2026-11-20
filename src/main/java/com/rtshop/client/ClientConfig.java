package com.rtshop.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/rtshop/client.json : Gold HUD 위치/크기.
 * 값은 화면 크기에 대한 비율(0~1)입니다. 기본값은 요청 스크린샷(1920×1004)의 노란 박스
 * (x 1587~1916, y 443~569)를 그대로 옮긴 값이라 해상도/GUI 배율이 달라도 같은 자리, 같은 크기입니다.
 */
public final class ClientConfig {
    public boolean hudEnabled = true;
    // 2.1.1: 박스를 원래 크기의 약 75%로 줄임 (오른쪽 끝과 위쪽 위치는 노란 박스와 동일)
    private static final int CONFIG_VERSION = 2;
    public double hudX = 1669.0 / 1920.0;
    public double hudY = 443.0 / 1004.0;
    public double hudWidth = 247.0 / 1920.0;
    public double hudHeight = 94.0 / 1004.0;

    private static ClientConfig instance;

    public static synchronized ClientConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static ClientConfig load() {
        ClientConfig c = new ClientConfig();
        Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("client.json");
        try {
            JsonObject old = null;
            if (Files.exists(p)) {
                old = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                // 이전 버전이 만든 파일이면 새 기본 크기로 다시 만든다
                if (!old.has("configVersion") || old.get("configVersion").getAsInt() < CONFIG_VERSION) {
                    if (old.has("hudEnabled")) c.hudEnabled = old.get("hudEnabled").getAsBoolean();
                    old = null;
                }
            }
            if (old != null) {
                JsonObject o = old;
                if (o.has("hudEnabled")) c.hudEnabled = o.get("hudEnabled").getAsBoolean();
                if (o.has("hudX")) c.hudX = o.get("hudX").getAsDouble();
                if (o.has("hudY")) c.hudY = o.get("hudY").getAsDouble();
                if (o.has("hudWidth")) c.hudWidth = o.get("hudWidth").getAsDouble();
                if (o.has("hudHeight")) c.hudHeight = o.get("hudHeight").getAsDouble();
            } else {
                JsonObject o = new JsonObject();
                o.addProperty("_설명", "Gold HUD 위치와 크기. 화면 크기에 대한 비율(0~1). hudX/hudY = 박스 왼쪽 위 모서리. 수정 후 게임 재시작.");
                o.addProperty("configVersion", CONFIG_VERSION);
                o.addProperty("hudEnabled", c.hudEnabled);
                o.addProperty("hudX", c.hudX);
                o.addProperty("hudY", c.hudY);
                o.addProperty("hudWidth", c.hudWidth);
                o.addProperty("hudHeight", c.hudHeight);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] client.json 을 읽지 못해 기본값을 사용합니다.", ex);
        }
        return c;
    }
}
