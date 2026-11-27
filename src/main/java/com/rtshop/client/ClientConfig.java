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
    public static final double QDEF_W = 0.3;
    public static final double QDEF_X = 1 - QDEF_W - 0.004;
    public static final double QDEF_Y = 0.56;
    public static final double QDEF_SCALE = 0.9;
    public static final int QDEF_MAX = 3;
    public boolean questHudEnabled = true;
    public double questHudX = QDEF_X;
    public double questHudY = QDEF_Y;
    public double questHudWidth = QDEF_W;
    public double questHudScale = QDEF_SCALE;
    /** 한 번에 보일 퀘스트 수 (1~5). */
    public int questHudMax = QDEF_MAX;
    /** HUD 글꼴 ("" = 게임 기본 글꼴, 예: minecraft:uniform). */
    public String hudFont = "";
    public String questHudFont = "";
    /** Gold 표시의 글자 크기 배율 (상자 크기는 그대로, 0.5~1.5). 퀘스트 HUD 의 글자 크기는 questHudScale. */
    public double hudTextScale = 1.0;

    // 강화 정보 패널 (강화 창 옆 / 시네마틱 모루 장면 위). 위치는 화면 비율, -1 = 자동(강화 창 오른쪽 / 화면 오른쪽 가운데).
    public boolean enhPanelEnabled = true;
    public double enhPanelX = -1;
    public double enhPanelY = -1;
    /** 크기 배율 (0.6~1.5). */
    public double enhPanelScale = 1.0;
    /** 가방 창 왼쪽 위에 다른 모드가 붙이는 기능 단추 묶음을 숨긴다. */
    public boolean hideSidebarButtons = true;
    /** 가방 창 왼쪽 위의 Spiral Abyss 일지 단추를 숨긴다 (3.78, 사용자 11-28). 일지는 그 모드의 키로 여전히 열린다. */
    public boolean hideAbyssInventoryButton = true;
    /** 보스 등장 때 카메라가 보스 곁에서 움직이는 3D 연출 (3.61). 끄면 화면 위의 2D 연출만 나온다. */
    public boolean bossCamera = true;
    /** RT Shop NPC 를 주민 대신 인물별 사람 모습으로 그린다 (3.61). 끄면 예전의 주민 모습. */
    public boolean npcLooks = true;
    /** 보스 음악이 겹치지 않게 한다 (3.63): 다른 모드의 보스 음악은 한 번에 하나만 흐르고, 그동안 배경 음악은 쉰다. 끄면 다른 모드의 음악에 손대지 않는다. */
    public boolean oneBossMusic = true;
    /** RT Shop NPC 의 이름표를 이 거리(칸) 안에서만 그린다 (3.64, 프레임). 0 이면 예전처럼 멀리서도(64칸) 그린다. */
    public int npcNameDistance = 16;
    /** 룬 제단 가운데의 빛나는 구슬의 색 (3.64): ice(달빛 은백, 기본 — 3.64.1 에서 사용자가 고름) · blue(별빛 푸른색) · gold(황금빛) · violet(자수정 보라) · jade(비취 청록) · rose(장미빛). */
    public String altarOrbColor = DEFAULT_ORB;
    public static final String DEFAULT_ORB = "ice";
    /** client.json 에 구슬 색과 함께 적는 판 번호. 이것이 없는 파일의 "blue" 는 3.64.0 이 기본값으로 적은 것이다. */
    private static final int ORB_COLOR_VERSION = 2;

    /**
     * client.json 의 구슬 색을 읽는다 (3.64.1). 3.64.0 은 기본값 blue 를 파일에 적어 두었으므로, 판 번호(altarOrbColorVersion)가 없는 파일의 blue 는
     * 사람이 고른 것이 아니라 옛 기본값이다 → 새 기본값(ice)으로 옮긴다. 판 번호가 있으면(이 판부터 저장한 파일) 적힌 그대로 쓴다.
     */
    public static String orbColorFrom(JsonObject o) {
        if (o == null || !o.has("altarOrbColor")) return DEFAULT_ORB;
        String c = o.get("altarOrbColor").getAsString().trim().toLowerCase(java.util.Locale.ROOT);
        if ("blue".equals(c) && !o.has("altarOrbColorVersion")) return DEFAULT_ORB;
        return c;
    }

    // PvP 상태 표시 (켜짐/꺼짐 + 지금 있는 곳). 위치·크기는 화면 비율.
    public static final double PDEF_W = 247.0 / 1920.0;
    public static final double PDEF_H = 94.0 / 1004.0;
    public static final double PDEF_X = DEF_X;
    public static final double PDEF_Y = DEF_Y - PDEF_H - 0.012;
    public boolean pvpHudEnabled = true;
    /** 파티 표시 (화면 왼쪽). */
    public boolean partyHudEnabled = true;
    /** 무기 설명에 무기 분류(검·창·총…)를 적는다. */
    public boolean weaponTypeTooltip = true;
    /** Fantasy Mounts 의 퀘스트 대화창이 저절로 뜨는 것 (기본 끔) */
    public boolean fantasyQuestPopups = false;
    /** 대화 상자 글자가 써질 때 나는 작은 소리. */
    public boolean dialogueSound = true;
    /** 미니맵(Diorama)의 몹 아이콘에 RT Shop NPC 를 보일 것인가. /rtshop hud 에서 켜고 끈다. */
    public boolean minimapNpcIcons = true;
    /**
     * 3.70.0: Distant Horizons 가 있으면, 서버에 들어갔을 때 렌더 거리를 서버 시야 거리에 맞춘다 (나가면 되돌림).
     * DH 는 렌더 거리 바깥부터 먼 풍경(LOD)을 그리므로, 렌더 거리가 서버 시야 거리보다 크면 그 사이의 가까운 청크가 비어 보인다.
     */
    public boolean dhMatchServer = true;
    /**
     * 3.70.0: dhcompat 이 꺼 둔 ImmediatelyFast 의 빠른 버퍼 업로드를 dhcompat 이 빠진 뒤 한 번 되돌린다 (FastBufferFix).
     * fastBufferRestored: 이미 한 번 했음 (그 뒤에 플레이어가 끈 것은 그대로 둔다).
     */
    public boolean restoreFastBuffer = true;
    public boolean fastBufferRestored = false;
    /** RT Shop 창 글꼴: sans(고딕) / serif(명조) / pixel(게임 기본) */
    public String uiFont = "sans";
    /** RT Shop 창 글자 크기: 0 작게, 1 보통, 2 크게 */
    public int uiFontSize = 1;
    // 룬 제단(마법 뽑기) 화면: 글꼴 sans(고딕)·serif(명조)·batang(바탕, 예전 모습)·pixel(게임 기본), 창 크기 배율, 글자 그림자,
    // 뽑기 연출(0 보통, 1 짧게, 2 끔), 장면 옆 정보 패널 자리(false 오른쪽, true 왼쪽)
    public String magicFont = "sans";
    /** 강화 정보 창을 전용 글꼴(송명 + Playfair Display)로 그릴까. false 면 게임 글꼴. */
    public boolean enhanceLuxFont = true;
    public double magicScale = 1.0;
    public boolean magicShadow = false;
    public int magicFx = 0;
    public boolean magicPanelLeft = false;

    public void resetMagic() {
        magicFont = "sans";
        magicScale = 1.0;
        magicShadow = false;
        magicFx = 0;
        magicPanelLeft = false;
    }

    public double pvpHudX = PDEF_X;
    public double pvpHudY = PDEF_Y;
    public double pvpHudWidth = PDEF_W;
    public double pvpHudHeight = PDEF_H;
    public String pvpHudFont = "";
    public double pvpHudTextScale = 1.0;
    /** HUD·스토리 언어: "" = 게임 언어를 따름, ko_kr, en_us */
    public String questHudLang = "";
    public String pvpHudLang = "";
    public String storyLang = "";

    public void resetPvpHud() {
        pvpHudX = PDEF_X;
        pvpHudY = PDEF_Y;
        pvpHudWidth = PDEF_W;
        pvpHudHeight = PDEF_H;
        pvpHudFont = "";
        pvpHudTextScale = 1.0;
    }

    public void clampPvp() {
        pvpHudTextScale = Math.max(0.5, Math.min(1.5, pvpHudTextScale));
        pvpHudWidth = Math.max(0.05, Math.min(0.6, pvpHudWidth));
        pvpHudHeight = Math.max(0.03, Math.min(0.4, pvpHudHeight));
        pvpHudX = Math.max(0, Math.min(1 - pvpHudWidth, pvpHudX));
        pvpHudY = Math.max(0, Math.min(1 - pvpHudHeight, pvpHudY));
    }

    public void resetEnhPanel() {
        enhPanelEnabled = true;
        enhPanelX = -1;
        enhPanelY = -1;
        enhPanelScale = 1.0;
    }

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
        hudFont = "";
        hudTextScale = 1.0;
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
        questHudFont = "";
    }

    public void clampQuest() {
        questHudScale = Math.max(0.5, Math.min(2.5, questHudScale));
        questHudWidth = Math.max(0.08, Math.min(0.6, questHudWidth));
        questHudMax = Math.max(1, Math.min(5, questHudMax));
        questHudX = Math.max(0, Math.min(1 - questHudWidth, questHudX));
        questHudY = Math.max(0, Math.min(0.98, questHudY));
    }

    public void clamp() {
        hudTextScale = Math.max(0.5, Math.min(1.5, hudTextScale));
        hudWidth = Math.max(0.05, Math.min(0.6, hudWidth));
        hudHeight = Math.max(0.03, Math.min(0.4, hudHeight));
        hudX = Math.max(0, Math.min(1 - hudWidth, hudX));
        hudY = Math.max(0, Math.min(1 - hudHeight, hudY));
    }

    public synchronized void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("_설명", "Gold 표시 박스·퀘스트 HUD·PvP 표시(pvpHud*)의 위치와 크기(화면 크기에 대한 비율 0~1). 게임 안에서 /rtshop hud 로 끌어서 옮길 수 있습니다. questHudScale 은 글자 배율, questHudMax 는 한 번에 보일 퀘스트 수, hudFont/questHudFont 는 글꼴(빈 값 = 기본), hudTextScale 은 Gold 글자 크기입니다. altarOrbColor 는 룬 제단 가운데 구슬의 색(ice 달빛 은백 · blue 별빛 푸른색 · gold 황금빛 · violet 자수정 보라 · jade 비취 청록 · rose 장미빛), 고친 뒤 게임을 다시 켜면 적용됩니다.");
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
            o.addProperty("hudFont", hudFont);
            o.addProperty("hudTextScale", hudTextScale);
            o.addProperty("questHudFont", questHudFont);
            o.addProperty("pvpHudEnabled", pvpHudEnabled);
            o.addProperty("partyHudEnabled", partyHudEnabled);
            o.addProperty("weaponTypeTooltip", weaponTypeTooltip);
            o.addProperty("fantasyQuestPopups", fantasyQuestPopups);
            o.addProperty("dialogueSound", dialogueSound);
            o.addProperty("minimapNpcIcons", minimapNpcIcons);
            o.addProperty("dhMatchServer", dhMatchServer);
            o.addProperty("restoreFastBuffer", restoreFastBuffer);
            o.addProperty("fastBufferRestored", fastBufferRestored);
            o.addProperty("uiFont", uiFont);
            o.addProperty("uiFontSize", uiFontSize);
            o.addProperty("pvpHudX", pvpHudX);
            o.addProperty("pvpHudY", pvpHudY);
            o.addProperty("pvpHudWidth", pvpHudWidth);
            o.addProperty("pvpHudHeight", pvpHudHeight);
            o.addProperty("pvpHudFont", pvpHudFont);
            o.addProperty("pvpHudTextScale", pvpHudTextScale);
            o.addProperty("questHudLang", questHudLang);
            o.addProperty("pvpHudLang", pvpHudLang);
            o.addProperty("storyLang", storyLang);
            o.addProperty("enhPanelEnabled", enhPanelEnabled);
            o.addProperty("enhPanelX", enhPanelX);
            o.addProperty("enhPanelY", enhPanelY);
            o.addProperty("enhPanelScale", enhPanelScale);
            o.addProperty("hideSidebarButtons", hideSidebarButtons);
            o.addProperty("hideAbyssInventoryButton", hideAbyssInventoryButton);
            o.addProperty("bossCamera", bossCamera);
            o.addProperty("npcLooks", npcLooks);
            o.addProperty("oneBossMusic", oneBossMusic);
            o.addProperty("npcNameDistance", npcNameDistance);
            o.addProperty("altarOrbColor", altarOrbColor);
            o.addProperty("altarOrbColorVersion", ORB_COLOR_VERSION);
            o.addProperty("magicFont", magicFont);
            o.addProperty("enhanceLuxFont", enhanceLuxFont);
            o.addProperty("magicScale", magicScale);
            o.addProperty("magicShadow", magicShadow);
            o.addProperty("magicFx", magicFx);
            o.addProperty("magicPanelLeft", magicPanelLeft);
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
                if (o.has("hudFont")) c.hudFont = o.get("hudFont").getAsString();
                if (o.has("hudTextScale")) c.hudTextScale = o.get("hudTextScale").getAsDouble();
                if (o.has("questHudFont")) c.questHudFont = o.get("questHudFont").getAsString();
                if (o.has("pvpHudEnabled")) c.pvpHudEnabled = o.get("pvpHudEnabled").getAsBoolean();
                if (o.has("partyHudEnabled")) c.partyHudEnabled = o.get("partyHudEnabled").getAsBoolean();
                if (o.has("weaponTypeTooltip")) c.weaponTypeTooltip = o.get("weaponTypeTooltip").getAsBoolean();
                if (o.has("fantasyQuestPopups")) c.fantasyQuestPopups = o.get("fantasyQuestPopups").getAsBoolean();
                if (o.has("dialogueSound")) c.dialogueSound = o.get("dialogueSound").getAsBoolean();
                if (o.has("minimapNpcIcons")) c.minimapNpcIcons = o.get("minimapNpcIcons").getAsBoolean();
                if (o.has("dhMatchServer")) c.dhMatchServer = o.get("dhMatchServer").getAsBoolean();
                if (o.has("restoreFastBuffer")) c.restoreFastBuffer = o.get("restoreFastBuffer").getAsBoolean();
                if (o.has("fastBufferRestored")) c.fastBufferRestored = o.get("fastBufferRestored").getAsBoolean();
                if (o.has("uiFont")) c.uiFont = o.get("uiFont").getAsString();
                if (o.has("uiFontSize")) c.uiFontSize = Math.max(0, Math.min(2, o.get("uiFontSize").getAsInt()));
                if (o.has("pvpHudX")) c.pvpHudX = o.get("pvpHudX").getAsDouble();
                if (o.has("pvpHudY")) c.pvpHudY = o.get("pvpHudY").getAsDouble();
                if (o.has("pvpHudWidth")) c.pvpHudWidth = o.get("pvpHudWidth").getAsDouble();
                if (o.has("pvpHudHeight")) c.pvpHudHeight = o.get("pvpHudHeight").getAsDouble();
                if (o.has("pvpHudFont")) c.pvpHudFont = o.get("pvpHudFont").getAsString();
                if (o.has("pvpHudTextScale")) c.pvpHudTextScale = o.get("pvpHudTextScale").getAsDouble();
                if (o.has("questHudLang")) c.questHudLang = HudLang.normalize(o.get("questHudLang").getAsString());
                if (o.has("pvpHudLang")) c.pvpHudLang = HudLang.normalize(o.get("pvpHudLang").getAsString());
                if (o.has("storyLang")) c.storyLang = HudLang.normalize(o.get("storyLang").getAsString());
                c.clampPvp();
                if (o.has("enhPanelEnabled")) c.enhPanelEnabled = o.get("enhPanelEnabled").getAsBoolean();
                if (o.has("enhPanelX")) c.enhPanelX = o.get("enhPanelX").getAsDouble();
                if (o.has("enhPanelY")) c.enhPanelY = o.get("enhPanelY").getAsDouble();
                if (o.has("hideSidebarButtons")) c.hideSidebarButtons = o.get("hideSidebarButtons").getAsBoolean();
                if (o.has("hideAbyssInventoryButton")) c.hideAbyssInventoryButton = o.get("hideAbyssInventoryButton").getAsBoolean();
                if (o.has("bossCamera")) c.bossCamera = o.get("bossCamera").getAsBoolean();
                if (o.has("npcLooks")) c.npcLooks = o.get("npcLooks").getAsBoolean();
                if (o.has("oneBossMusic")) c.oneBossMusic = o.get("oneBossMusic").getAsBoolean();
                c.altarOrbColor = orbColorFrom(o);
                boolean orbMoved = o.has("altarOrbColor") && !o.has("altarOrbColorVersion");   // 옛 파일: 판 번호를 적어 두어 다음부터는 적힌 그대로 읽게 한다
                if (o.has("npcNameDistance")) c.npcNameDistance = Math.max(0, Math.min(64, o.get("npcNameDistance").getAsInt()));
                if (o.has("enhPanelScale")) c.enhPanelScale = Math.max(0.6, Math.min(1.5, o.get("enhPanelScale").getAsDouble()));
                if (o.has("magicFont")) c.magicFont = o.get("magicFont").getAsString();
                if (o.has("enhanceLuxFont")) c.enhanceLuxFont = o.get("enhanceLuxFont").getAsBoolean();
                if (o.has("magicScale")) c.magicScale = Math.max(0.7, Math.min(1.3, o.get("magicScale").getAsDouble()));
                if (o.has("magicShadow")) c.magicShadow = o.get("magicShadow").getAsBoolean();
                if (o.has("magicFx")) c.magicFx = Math.max(0, Math.min(2, o.get("magicFx").getAsInt()));
                if (o.has("magicPanelLeft")) c.magicPanelLeft = o.get("magicPanelLeft").getAsBoolean();
                c.clamp();
                c.clampQuest();
                if (!o.has("questHudEnabled") || orbMoved) {
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
