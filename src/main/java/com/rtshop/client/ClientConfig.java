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
 * config/rtshop/client.json : Gold 표시 박스의 위치와 크기.
 * 값은 화면 크기에 대한 비율(0~1)이라 해상도나 GUI 배율이 달라도 같은 자리에 나온다.
 * 게임 안에서 /rtshop hud (또는 조작 설정의 단축키)로 끌어서 옮길 수 있다.
 */
public final class ClientConfig {
    private static final int CONFIG_VERSION = 2;
    public static final double DEF_X = 1669.0 / 1920.0;
    public static final double DEF_Y = 443.0 / 1004.0;
    public static final double DEF_W = 247.0 / 1920.0;
    public static final double DEF_H = 94.0 / 1004.0;

    public boolean hudEnabled = true;
    public double hudX = DEF_X;
    public double hudY = DEF_Y;
    public double hudWidth = DEF_W;
    public double hudHeight = DEF_H;

    // 퀘스트 HUD (진행 중인 퀘스트와 목표). 위치는 화면 비율, 크기는 글자 배율과 너비 비율.
    public static final double QDEF_W = 0.2;
    public static final double QDEF_X = 1 - QDEF_W - 0.004;
    public static final double QDEF_Y = 0.56;
    public static final double QDEF_SCALE = 1.0;
    public static final int QDEF_MAX = 3;
    public boolean questHudEnabled = true;
    public double questHudX = QDEF_X;
    public double questHudY = QDEF_Y;
    public double questHudWidth = QDEF_W;
    public double questHudScale = QDEF_SCALE;
    /** 한 번에 보일 퀘스트 수 (1~5). */
    public int questHudMax = QDEF_MAX;

    private static ClientConfig instance;

    public static synchronized ClientConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("client.json");
    }

    public void resetHud() {
        hudX = DEF_X;
        hudY = DEF_Y;
        hudWidth = DEF_W;
        hudHeight = DEF_H;
    }

    public void resetQuestHud() {
        questHudX = QDEF_X;
        questHudY = QDEF_Y;
        questHudWidth = QDEF_W;
        questHudScale = QDEF_SCALE;
        questHudMax = QDEF_MAX;
    }

    public void clampQuest() {
        questHudScale = Math.max(0.5, Math.min(2.5, questHudScale));
        questHudWidth = Math.max(0.08, Math.min(0.6, questHudWidth));
        questHudMax = Math.max(1, Math.min(5, questHudMax));
        questHudX = Math.max(0, Math.min(1 - questHudWidth, questHudX));
        questHudY = Math.max(0, Math.min(0.98, questHudY));
    }

    public void clamp() {
        hudWidth = Math.max(0.05, Math.min(0.6, hudWidth));
        hudHeight = Math.max(0.03, Math.min(0.4, hudHeight));
        hudX = Math.max(0, Math.min(1 - hudWidth, hudX));
        hudY = Math.max(0, Math.min(1 - hudHeight, hudY));
    }

    public synchronized void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("_설명", "Gold 표시 박스와 퀘스트 HUD 의 위치와 크기(화면 크기에 대한 비율 0~1). 게임 안에서 /rtshop hud 로 끌어서 옮길 수 있습니다. questHudScale 은 글자 배율, questHudMax 는 한 번에 보일 퀘스트 수입니다.");
            o.addProperty("configVersion", CONFIG_VERSION);
            o.addProperty("hudEnabled", hudEnabled);
            o.addProperty("hudX", hudX);
            o.addProperty("hudY", hudY);
            o.addProperty("hudWidth", hudWidth);
            o.addProperty("hudHeight", hudHeight);
            o.addProperty("questHudEnabled", questHudEnabled);
            o.addProperty("questHudX", questHudX);
            o.addProperty("questHudY", questHudY);
            o.addProperty("questHudWidth", questHudWidth);
            o.addProperty("questHudScale", questHudScale);
            o.addProperty("questHudMax", questHudMax);
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] client.json 을 저장하지 못했습니다.", ex);
        }
    }

    private static ClientConfig load() {
        ClientConfig c = new ClientConfig();
        Path p = path();
        try {
            JsonObject o = null;
            if (Files.exists(p)) {
                o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                // 예전 버전이 만든 파일이면 새 기본 크기로 다시 만든다
                if (!o.has("configVersion") || o.get("configVersion").getAsInt() < CONFIG_VERSION) {
                    if (o.has("hudEnabled")) c.hudEnabled = o.get("hudEnabled").getAsBoolean();
                    o = null;
                }
            }
            if (o != null) {
                if (o.has("hudEnabled")) c.hudEnabled = o.get("hudEnabled").getAsBoolean();
                if (o.has("hudX")) c.hudX = o.get("hudX").getAsDouble();
                if (o.has("hudY")) c.hudY = o.get("hudY").getAsDouble();
                if (o.has("hudWidth")) c.hudWidth = o.get("hudWidth").getAsDouble();
                if (o.has("hudHeight")) c.hudHeight = o.get("hudHeight").getAsDouble();
                c.clamp();
                if (o.has("questHudEnabled")) c.questHudEnabled = o.get("questHudEnabled").getAsBoolean();
                if (o.has("questHudX")) c.questHudX = o.get("questHudX").getAsDouble();
                if (o.has("questHudY")) c.questHudY = o.get("questHudY").getAsDouble();
                if (o.has("questHudWidth")) c.questHudWidth = o.get("questHudWidth").getAsDouble();
                if (o.has("questHudScale")) c.questHudScale = o.get("questHudScale").getAsDouble();
                if (o.has("questHudMax")) c.questHudMax = o.get("questHudMax").getAsInt();
                c.clampQuest();
                if (!o.has("questHudEnabled")) {
                    c.save();
                }
            } else {
                c.save();
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] client.json 을 읽지 못해 기본값을 사용합니다.", ex);
        }
        return c;
    }
}
