package com.rtshop.raid;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * config/rtshop/raid.json : 주간 레이드 (기본 보스: L_Ender's Cataclysm 의 이그니스).
 * 게임 안에서 /rtshop raid edit (OP) 로 수치·문구·보상을 고친다. 보스는 boss 값을 다른 Cataclysm 보스 ID 로 바꾸면 된다.
 */
public final class RaidConfig {
    public static final String G_WEEKLY = "weekly";
    public static final String G_DROP = "drop";

    public boolean enabled = true;
    /** 레이드 보스 (엔티티 ID). */
    public String boss = "cataclysm:ignis";
    /** 입장 조건: 이 몬스터를 requireKills 번 이상 처치 (레이드 보스는 세지 않음). 빈 값 = 조건 없음. */
    public String requireKill = "cataclysm:ignis";
    public int requireKills = 1;
    /** 레이드 관련 NPC(안내인·레이드 상점)는 이름 있는 보스를 처음 쓰러뜨린 뒤에야 말을 받아 준다. false 면 처음부터 열려 있다. */
    public boolean npcsAfterFirstBoss = true;
    /** 일반 보스 대비 체력·공격력 배율. */
    public double healthMultiplier = 2.0;
    public double damageMultiplier = 1.5;
    public int resetDay = 1;
    public int resetHour = 6;
    /** 성공 뒤 마을로 돌아가기까지 (초). */
    public int returnSeconds = 30;
    /** 실패 뒤 확인하지 않은 사람을 돌려보내기까지 (초). */
    public int failReturnSeconds = 60;
    /** 드랍 굴림 횟수: 이번 주 주간 보상을 아직 받지 않은 사람 / 이미 받은 사람. */
    public int rollsFresh = 3;
    public int rollsRepeat = 1;
    /** 굴림 한 번에 각 등급이 나올 확률(%) (합이 100 보다 작으면 나머지는 꽝). 0 일반 … 4 전설, 5 [???]. */
    public double[] chanceFresh = {40, 30, 18, 8, 3.95, 0.05};
    public double[] chanceRepeat = {30, 15, 6, 2, 0.5, 0.01};
    /** 주간 보상으로 함께 주는 Gold. */
    public long weeklyGold = 10000;
    // 레이드 구역 (바닥 가운데)
    public String arenaDim = "minecraft:overworld";
    public int arenaX = 20000, arenaY = 200, arenaZ = 20000;
    public int arenaRadius = 26, arenaHeight = 30;
    /** 구역을 모드가 지었는가 (false 면 처음 들어갈 때 짓는다). */
    public boolean arenaBuilt = false;
    // 돌아갈 곳 (정하지 않았으면 월드 스폰)
    public boolean returnSet = false;
    public String returnDim = "minecraft:overworld";
    public double returnX, returnY, returnZ;
    public final Map<String, String> texts = new LinkedHashMap<>();
    public final List<Reward> rewards = new ArrayList<>();

    public static Map<String, String> defaultTexts() {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("npcName", "레이드 안내인");
        t.put("bossName", "화염의 군주 이그니스");
        t.put("dialogue1", "…뜨거운 바람이 느껴지나? 이 문 너머가 화염의 군주, 이그니스의 처소다.");
        t.put("dialogue2", "제단에 불꽃을 바쳐 군주가 깨어나면 문은 닫힌다. 안에 있는 자들만으로 끝을 봐야 하지.");
        t.put("dialogue3", "참가 티켓을 내고 들어가겠나? …부디 살아서 돌아오게.");
        t.put("uiTitle", "화염의 군주 · 이그니스");
        t.put("uiSub", "주간 레이드");
        t.put("enterButton", "레이드 입장");
        t.put("confirmTitle", "정말 입장하시겠습니까?");
        t.put("confirmBody", "참가 티켓 1장을 사용합니다. 이그니스가 소환된 뒤에는 다른 플레이어가 들어올 수 없습니다.");
        t.put("confirmYes", "입장한다");
        t.put("confirmNo", "돌아간다");
        t.put("needKill", "입장 조건: 이그니스를 %s번 이상 처치해야 합니다 (현재 %s번).");
        t.put("npcLocked", "…아직 자네에게 보여 줄 것이 없네. 이름 있는 것을 하나 쓰러뜨리고 오게. 이야기는 그다음일세.");
        t.put("npcOpened", "이제 레이드 안내인과 레이드 상점이 말을 받아 줍니다.");
        t.put("needTicket", "레이드 참가 티켓이 필요합니다. 레이드 상인에게서 살 수 있습니다.");
        t.put("locked", "이그니스가 이미 깨어났습니다. 레이드가 끝난 뒤에 다시 오세요.");
        t.put("entered", "레이드 구역에 들어왔습니다. 중앙 제단에 소환의 눈을 사용하면 이그니스가 깨어납니다.");
        t.put("summoned", "%s 님이 제단에 불꽃을 바쳤습니다. 이그니스가 깨어납니다!");
        t.put("alreadySummoned", "이그니스는 이미 깨어났습니다.");
        t.put("successTitle", "레이드 성공");
        t.put("successSub", "화염의 군주가 쓰러졌습니다");
        t.put("failTitle", "레이드 실패");
        t.put("failSub", "불꽃이 모두를 삼켰습니다");
        t.put("returnNotice", "%s초 뒤 마을로 돌아갑니다. 구역 안의 모두가 동의하면 바로 이동합니다.");
        t.put("voteTitle", "바로 마을로 이동할까요?");
        t.put("voteAgree", "지금 이동");
        t.put("voteStay", "조금 더 머물기");
        t.put("voteCount", "동의 %s / %s명");
        t.put("failConfirm", "마을로 이동");
        t.put("failBody", "확인을 누르면 마을로 이동합니다.");
        t.put("despawn", "레이드 구역에 아무도 남지 않아 이그니스가 사라졌습니다. (처치로 인정되지 않습니다)");
        t.put("notByPlayer", "이그니스가 플레이어의 공격이 아닌 이유로 쓰러져 보상이 없습니다.");
        t.put("downed", "쓰러졌습니다. 남은 동료가 싸우는 동안 지켜봅니다.");
        t.put("escaped", "레이드 구역에서 탈출했습니다.");
        t.put("weeklyGot", "이번 주 주간 보상을 받았습니다!");
        t.put("weeklyAlready", "이번 주 주간 보상은 이미 받았습니다. (드랍 확률이 낮아집니다)");
        t.put("dropGot", "보스 드랍: %s");
        t.put("dropNone", "이번에는 드랍 보상이 나오지 않았습니다.");
        t.put("mysteryName", "태초의 불씨");
        t.put("mysteryGot", "%s 님이 [%s] 등급 보상 %s 을(를) 획득했습니다!");
        t.put("rewardWeekly", "주간 보상 (매주 월요일 오전 6시 초기화)");
        t.put("rewardDrops", "보스 드랍");
        return t;
    }

    /** 편집기에 보일 문구 이름. */
    public static String label(String key) {
        return switch (key) {
            case "npcName" -> "안내인 이름";
            case "bossName" -> "보스 이름";
            case "dialogue1" -> "대화 1";
            case "dialogue2" -> "대화 2";
            case "dialogue3" -> "대화 3";
            case "uiTitle" -> "입장 화면 제목";
            case "uiSub" -> "입장 화면 부제";
            case "enterButton" -> "입장 버튼";
            case "confirmTitle" -> "확인 창 제목";
            case "confirmBody" -> "확인 창 설명";
            case "confirmYes" -> "확인 창: 예";
            case "confirmNo" -> "확인 창: 아니오";
            case "needKill" -> "조건 미달 (처치 수)";
            case "npcLocked" -> "첫 보스 처치 전 (NPC 잠김)";
            case "npcOpened" -> "첫 보스 처치 알림";
            case "needTicket" -> "티켓 없음";
            case "locked" -> "입장 불가 (진행 중)";
            case "entered" -> "입장 안내";
            case "summoned" -> "보스 소환 알림";
            case "alreadySummoned" -> "이미 소환됨";
            case "successTitle" -> "성공 문구";
            case "successSub" -> "성공 문구 (작은 글씨)";
            case "failTitle" -> "실패 문구";
            case "failSub" -> "실패 문구 (작은 글씨)";
            case "returnNotice" -> "귀환 안내 (%s = 초)";
            case "voteTitle" -> "이동 투표 제목";
            case "voteAgree" -> "투표: 지금 이동";
            case "voteStay" -> "투표: 머물기";
            case "voteCount" -> "투표 현황";
            case "failConfirm" -> "실패 창: 이동 버튼";
            case "failBody" -> "실패 창 설명";
            case "despawn" -> "보스 사라짐 안내";
            case "notByPlayer" -> "보상 없음 안내";
            case "downed" -> "쓰러짐 안내";
            case "escaped" -> "탈출 안내";
            case "weeklyGot" -> "주간 보상 받음";
            case "weeklyAlready" -> "주간 보상 이미 받음";
            case "dropGot" -> "드랍 획득 (%s = 아이템)";
            case "dropNone" -> "드랍 없음";
            case "mysteryName" -> "[???] 등급의 이름";
            case "mysteryGot" -> "[???] 획득 알림";
            case "rewardWeekly" -> "보상 목록: 주간 보상 제목";
            case "rewardDrops" -> "보상 목록: 드랍 제목";
            case "title" -> "창 제목";
            case "reset" -> "초기화 안내";
            case "floor" -> "층 이름";
            case "got" -> "보상 받음 알림";
            case "gotNothing" -> "확률 보상 없음 알림";
            case "done" -> "받음 표시";
            default -> key;
        };
    }

    /** 편집기에 보일 문구 이름 (영어). */
    public static String labelEn(String key) {
        return switch (key) {
            case "npcName" -> "Guide's name";
            case "bossName" -> "Boss name";
            case "dialogue1" -> "Dialogue 1";
            case "dialogue2" -> "Dialogue 2";
            case "dialogue3" -> "Dialogue 3";
            case "uiTitle" -> "Entry screen title";
            case "uiSub" -> "Entry screen subtitle";
            case "enterButton" -> "Enter button";
            case "confirmTitle" -> "Confirm window title";
            case "confirmBody" -> "Confirm window text";
            case "confirmYes" -> "Confirm window: yes";
            case "confirmNo" -> "Confirm window: no";
            case "needKill" -> "Requirement not met (kills)";
            case "npcLocked" -> "Before the first boss kill (NPC locked)";
            case "npcOpened" -> "First boss kill notice";
            case "needTicket" -> "No ticket";
            case "locked" -> "Cannot enter (in progress)";
            case "entered" -> "Entry notice";
            case "summoned" -> "Boss summoned notice";
            case "alreadySummoned" -> "Already summoned";
            case "successTitle" -> "Success text";
            case "successSub" -> "Success text (small)";
            case "failTitle" -> "Failure text";
            case "failSub" -> "Failure text (small)";
            case "returnNotice" -> "Return notice (%s = seconds)";
            case "voteTitle" -> "Leave vote title";
            case "voteAgree" -> "Vote: leave now";
            case "voteStay" -> "Vote: stay";
            case "voteCount" -> "Vote count";
            case "failConfirm" -> "Failure window: leave button";
            case "failBody" -> "Failure window text";
            case "despawn" -> "Boss vanished notice";
            case "notByPlayer" -> "No reward notice";
            case "downed" -> "Downed notice";
            case "escaped" -> "Escaped notice";
            case "weeklyGot" -> "Weekly reward received";
            case "weeklyAlready" -> "Weekly reward already received";
            case "dropGot" -> "Drop received (%s = item)";
            case "dropNone" -> "No drop";
            case "mysteryName" -> "Name of the [???] grade";
            case "mysteryGot" -> "[???] obtained notice";
            case "rewardWeekly" -> "Reward list: weekly reward heading";
            case "rewardDrops" -> "Reward list: drops heading";
            case "title" -> "Window title";
            case "reset" -> "Reset notice";
            case "floor" -> "Floor name";
            case "got" -> "Reward received notice";
            case "gotNothing" -> "No chance reward notice";
            case "done" -> "Received mark";
            default -> key;
        };
    }

    public static String label(String key, boolean en) {
        return en ? labelEn(key) : label(key);
    }

    public String text(String key) {
        String v = texts.get(key);
        if (v == null || v.isEmpty()) {
            v = defaultTexts().get(key);
        }
        return v == null ? key : v;
    }

    public String text(String key, Object... args) {
        try {
            return String.format(text(key), args);
        } catch (Exception e) {
            return text(key);
        }
    }

    /** 그 플레이어의 이야기 언어로 (기본 문구는 영어 표에 있고, 운영자가 고친 문구는 그대로 나간다). */
    public String textFor(net.minecraft.server.level.ServerPlayer p, String key, Object... args) {
        return com.rtshop.story.StoryText.f(p, text(key), args);
    }

    public List<Reward> group(String g) {
        List<Reward> out = new ArrayList<>();
        for (Reward r : rewards) {
            if (r.group.equals(g)) out.add(r);
        }
        return out;
    }

    private void defaults() {
        texts.putAll(defaultTexts());
        rewards.add(new Reward(G_WEEKLY, 3, "minecraft:diamond", 8));
        rewards.add(new Reward(G_WEEKLY, 3, "rtshop:gacha_ticket_high", 1));
        rewards.add(new Reward(G_DROP, 0, "minecraft:iron_ingot", 16));
        rewards.add(new Reward(G_DROP, 0, "minecraft:gold_ingot", 8));
        rewards.add(new Reward(G_DROP, 1, "minecraft:gold_ingot", 24));
        rewards.add(new Reward(G_DROP, 1, "minecraft:emerald", 12));
        rewards.add(new Reward(G_DROP, 2, "minecraft:diamond", 4));
        rewards.add(new Reward(G_DROP, 2, "rtshop:gacha_ticket_mid", 2));
        rewards.add(new Reward(G_DROP, 3, "minecraft:netherite_scrap", 2));
        rewards.add(new Reward(G_DROP, 3, "rtshop:gacha_ticket_high", 1));
        rewards.add(new Reward(G_DROP, 4, "minecraft:netherite_ingot", 2));
        rewards.add(new Reward(G_DROP, 4, "cataclysm:ignitium_ingot", 2));
        rewards.add(new Reward(G_DROP, 5, "minecraft:nether_star", 1));
    }

    // ------------------------------------------------------------------ 읽기 / 쓰기

    private static RaidConfig instance;

    public static synchronized RaidConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized RaidConfig reload() {
        instance = load();
        return instance;
    }

    public static synchronized void setForTest(RaidConfig c) {
        instance = c;
    }

    public static RaidConfig fresh() {
        RaidConfig c = new RaidConfig();
        c.defaults();
        return c;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("raid.json");
    }

    private static JsonArray arr(double[] v) {
        JsonArray a = new JsonArray();
        for (double d : v) a.add(d);
        return a;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("주간 레이드: 레이드 상인(/rtshop npc raid)에게서 참가 티켓을 사고, 레이드 안내인(/rtshop npc raid_guide)에게 말을 걸어 들어갑니다.");
        help.add("게임 안에서 /rtshop raid edit 로 수치·문구·보상을 고칠 수 있습니다. 위치: /rtshop raid arena here (구역을 지금 서 있는 곳에 다시 지음) · /rtshop raid return here (돌아갈 곳)");
        help.add("boss: 레이드 보스 엔티티 ID (예: cataclysm:ignis, cataclysm:ender_guardian, cataclysm:netherite_monstrosity, cataclysm:the_harbinger, cataclysm:the_leviathan, cataclysm:ancient_remnant)");
        help.add("requireKill / requireKills: 입장 조건 (이 몬스터를 몇 번 처치) · healthMultiplier / damageMultiplier: 일반 보스 대비 체력·공격력 배율");
        help.add("npcsAfterFirstBoss: true 면 레이드 안내인과 레이드 상점 NPC 는 보스(party.json 의 보스 목록)를 처음 쓰러뜨린 사람에게만 말을 받아 준다. 레이드 자체는 그 레이드의 보스(requireKill)를 쓰러뜨려야 열린다.");
        help.add("chanceFresh / chanceRepeat: 드랍 굴림 한 번에 각 등급이 나올 확률(%) [일반, 고급, 희귀, 영웅, 전설, ???]. Fresh = 이번 주 주간 보상을 아직 안 받은 사람, Repeat = 이미 받은 사람");
        help.add("rewards: group 이 weekly 면 주간 보상(매주 처음 성공할 때 모두 지급), drop 이면 보스 드랍(등급 굴림으로 그 등급의 아이템 중 하나)");
        o.add("_설명", help);
        o.addProperty("enabled", enabled);
        o.addProperty("boss", boss);
        o.addProperty("requireKill", requireKill);
        o.addProperty("requireKills", requireKills);
        o.addProperty("npcsAfterFirstBoss", npcsAfterFirstBoss);
        o.addProperty("healthMultiplier", healthMultiplier);
        o.addProperty("damageMultiplier", damageMultiplier);
        o.addProperty("resetDay", resetDay);
        o.addProperty("resetHour", resetHour);
        o.addProperty("returnSeconds", returnSeconds);
        o.addProperty("failReturnSeconds", failReturnSeconds);
        o.addProperty("rollsFresh", rollsFresh);
        o.addProperty("rollsRepeat", rollsRepeat);
        o.add("chanceFresh", arr(chanceFresh));
        o.add("chanceRepeat", arr(chanceRepeat));
        o.addProperty("weeklyGold", weeklyGold);
        JsonObject a = new JsonObject();
        a.addProperty("dim", arenaDim);
        a.addProperty("x", arenaX);
        a.addProperty("y", arenaY);
        a.addProperty("z", arenaZ);
        a.addProperty("radius", arenaRadius);
        a.addProperty("height", arenaHeight);
        a.addProperty("built", arenaBuilt);
        o.add("arena", a);
        JsonObject r = new JsonObject();
        r.addProperty("set", returnSet);
        r.addProperty("dim", returnDim);
        r.addProperty("x", returnX);
        r.addProperty("y", returnY);
        r.addProperty("z", returnZ);
        o.add("return", r);
        JsonObject t = new JsonObject();
        for (Map.Entry<String, String> e : texts.entrySet()) t.addProperty(e.getKey(), e.getValue());
        o.add("texts", t);
        JsonArray rw = new JsonArray();
        for (Reward x : rewards) rw.add(x.toJson());
        o.add("rewards", rw);
        return o;
    }

    public synchronized void save() {
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson()), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] raid.json 을 저장하지 못했습니다.", ex);
        }
    }

    private static double[] chances(JsonObject o, String key, double[] def) {
        double[] out = def.clone();
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (int i = 0; i < out.length && i < a.size(); i++) {
                out[i] = Math.max(0, Math.min(100, a.get(i).getAsDouble()));
            }
        }
        return out;
    }

    public static RaidConfig parse(JsonObject o) {
        RaidConfig c = new RaidConfig();
        c.texts.putAll(defaultTexts());
        if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
        if (o.has("boss")) c.boss = o.get("boss").getAsString();
        if (o.has("requireKill")) c.requireKill = o.get("requireKill").getAsString();
        if (o.has("requireKills")) c.requireKills = Math.max(0, o.get("requireKills").getAsInt());
        if (o.has("npcsAfterFirstBoss")) c.npcsAfterFirstBoss = o.get("npcsAfterFirstBoss").getAsBoolean();
        if (o.has("healthMultiplier")) c.healthMultiplier = Math.max(0.1, Math.min(1000, o.get("healthMultiplier").getAsDouble()));
        if (o.has("damageMultiplier")) c.damageMultiplier = Math.max(0.1, Math.min(1000, o.get("damageMultiplier").getAsDouble()));
        if (o.has("resetDay")) c.resetDay = Math.max(1, Math.min(7, o.get("resetDay").getAsInt()));
        if (o.has("resetHour")) c.resetHour = Math.max(0, Math.min(23, o.get("resetHour").getAsInt()));
        if (o.has("returnSeconds")) c.returnSeconds = Math.max(3, Math.min(600, o.get("returnSeconds").getAsInt()));
        if (o.has("failReturnSeconds")) c.failReturnSeconds = Math.max(5, Math.min(600, o.get("failReturnSeconds").getAsInt()));
        if (o.has("rollsFresh")) c.rollsFresh = Math.max(0, Math.min(20, o.get("rollsFresh").getAsInt()));
        if (o.has("rollsRepeat")) c.rollsRepeat = Math.max(0, Math.min(20, o.get("rollsRepeat").getAsInt()));
        c.chanceFresh = chances(o, "chanceFresh", c.chanceFresh);
        c.chanceRepeat = chances(o, "chanceRepeat", c.chanceRepeat);
        if (o.has("weeklyGold")) c.weeklyGold = Math.max(0, o.get("weeklyGold").getAsLong());
        if (o.has("arena") && o.get("arena").isJsonObject()) {
            JsonObject a = o.getAsJsonObject("arena");
            if (a.has("dim")) c.arenaDim = a.get("dim").getAsString();
            if (a.has("x")) c.arenaX = a.get("x").getAsInt();
            if (a.has("y")) c.arenaY = a.get("y").getAsInt();
            if (a.has("z")) c.arenaZ = a.get("z").getAsInt();
            if (a.has("radius")) c.arenaRadius = Math.max(12, Math.min(60, a.get("radius").getAsInt()));
            if (a.has("height")) c.arenaHeight = Math.max(12, Math.min(60, a.get("height").getAsInt()));
            if (a.has("built")) c.arenaBuilt = a.get("built").getAsBoolean();
        }
        if (o.has("return") && o.get("return").isJsonObject()) {
            JsonObject r = o.getAsJsonObject("return");
            if (r.has("set")) c.returnSet = r.get("set").getAsBoolean();
            if (r.has("dim")) c.returnDim = r.get("dim").getAsString();
            if (r.has("x")) c.returnX = r.get("x").getAsDouble();
            if (r.has("y")) c.returnY = r.get("y").getAsDouble();
            if (r.has("z")) c.returnZ = r.get("z").getAsDouble();
        }
        if (o.has("texts") && o.get("texts").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("texts").entrySet()) {
                if (!e.getValue().isJsonPrimitive()) continue;
                String v = e.getValue().getAsString();
                // 3.22.2: 예전 기본 문구 "(지금 %s번)" 은 새 기본 문구 "(현재 %s번)" 으로 바꾼다 (직접 고친 문구는 그대로)
                if (e.getKey().equals("needKill") && v.equals("입장 조건: 이그니스를 %s번 이상 처치해야 합니다 (지금 %s번).")) v = c.texts.get("needKill");
                c.texts.put(e.getKey(), v);
            }
        }
        if (o.has("rewards") && o.get("rewards").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("rewards")) {
                if (e.isJsonObject()) c.rewards.add(Reward.parse(e.getAsJsonObject()));
            }
        }
        return c;
    }

    private static RaidConfig load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                return parse(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] raid.json 을 읽지 못해 기본값을 사용합니다.", ex);
            return fresh();
        }
        RaidConfig c = fresh();
        c.save();
        return c;
    }
}
