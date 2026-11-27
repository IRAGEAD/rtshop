package com.rtshop.economy;

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
import java.util.List;

/** config/rtshop/economy.json */
public final class EconomyConfig {
    /** 설정 파일의 세대. 2 = RT Shop 3.0 (전체 물가를 10배로 세분화). 4 = 3.69.0 (사망 시 소지금 손실 5~50% → 5~30%). */
    private static final int CONFIG_VERSION = 4;

    public long startingGold = 1000;
    public double deathLossMinPercent = 5.0;
    /** 3.69.0: 50 → 30 (사용자 요청 "J는 손실 폭을 5-30%로"). 옛 기본값 50 그대로인 파일만 한 번 고친다. */
    public double deathLossMaxPercent = 30.0;
    public String bankNpcName = "은행원";
    /** 플레이어 상점 직원의 하루 고용비 (출석 초기화 시각에 예금 → 지갑 순으로 나간다). */
    public long employeeWage = 1000;
    public double npcRange = 10.0;
    /** 사망 시 가지고 있던 수표의 금액도 같은 비율로 줄어드는지 여부. */
    public boolean checkDeathLoss = true;
    public long minCheckAmount = 1;
    /**
     * 플레이어가 직접 잡았을 때만 전리품을 떨어뜨리는 몹. 자동 농장(용암·낙하·질식으로 잡는 방식)으로
     * 철·금·물고기가 무한히 쌓여 경제가 무너지는 것을 막는다. 비우면 바닐라와 같아진다.
     */
    public final List<String> playerKillOnlyDrops = new ArrayList<>();
    /**
     * 주민·떠돌이 상인과의 거래.
     *  filtered: 상인 NPC 가 매입하는 물건(빵, 철 갑옷 등)을 파는 거래만 없앤다 - 에메랄드로 물건을 받아 되파는 무한 돈벌이 방지 (기본값)
     *  allow   : 바닐라 그대로 · block: 주민 거래 전체 차단
     */
    public String villagerTrading = "filtered";

    private static EconomyConfig instance;

    public static synchronized EconomyConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized EconomyConfig reload() {
        instance = load();
        return instance;
    }

    private void defaults() {
        playerKillOnlyDrops.add("minecraft:iron_golem");
        playerKillOnlyDrops.add("minecraft:zombified_piglin");
        playerKillOnlyDrops.add("minecraft:guardian");
    }

    private static EconomyConfig load() {
        EconomyConfig c = new EconomyConfig();
        c.defaults();
        Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("economy.json");
        boolean rewrite = true;
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                int version = o.has("configVersion") ? o.get("configVersion").getAsInt() : 1;
                if (o.has("startingGold")) c.startingGold = Math.max(0, o.get("startingGold").getAsLong());
                if (version < 2) {
                    // 3.0 에서 전체 물가가 10배 단위로 바뀌었으므로 이전 설정의 금액도 같이 환산한다
                    c.startingGold = Math.min(1_000_000_000L, c.startingGold * 10);
                }
                if (o.has("deathLossMinPercent")) c.deathLossMinPercent = o.get("deathLossMinPercent").getAsDouble();
                if (o.has("deathLossMaxPercent")) c.deathLossMaxPercent = o.get("deathLossMaxPercent").getAsDouble();
                if (version < 4 && c.deathLossMaxPercent == 50.0) c.deathLossMaxPercent = 30.0;   // 3.69.0: 옛 기본값만
                if (o.has("bankNpcName")) c.bankNpcName = o.get("bankNpcName").getAsString();
                if (o.has("npcRange")) c.npcRange = o.get("npcRange").getAsDouble();
                if (o.has("checkDeathLoss")) c.checkDeathLoss = o.get("checkDeathLoss").getAsBoolean();
                if (o.has("minCheckAmount")) c.minCheckAmount = Math.max(1, o.get("minCheckAmount").getAsLong());
                if (o.has("playerKillOnlyDrops") && o.get("playerKillOnlyDrops").isJsonArray()) {
                    c.playerKillOnlyDrops.clear();
                    for (JsonElement e : o.getAsJsonArray("playerKillOnlyDrops")) {
                        c.playerKillOnlyDrops.add(e.getAsString());
                    }
                }
                if (o.has("employeeWage")) c.employeeWage = Math.max(0, o.get("employeeWage").getAsLong());
                if (o.has("villagerTrading")) c.villagerTrading = o.get("villagerTrading").getAsString();
                rewrite = version < CONFIG_VERSION;
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] economy.json 을 읽지 못해 기본값을 사용합니다.", ex);
            rewrite = false;
        }
        c.deathLossMinPercent = Math.max(0, Math.min(100, c.deathLossMinPercent));
        c.deathLossMaxPercent = Math.max(c.deathLossMinPercent, Math.min(100, c.deathLossMaxPercent));
        if (!c.villagerTrading.equals("allow") && !c.villagerTrading.equals("block")) {
            c.villagerTrading = "filtered";
        }
        if (rewrite) {
            c.save(p);
        }
        return c;
    }

    /** 새 항목이 보이도록 파일을 다시 쓴다 (기존에 적어 둔 값은 유지). */
    private void save(Path p) {
        try {
            JsonObject o = new JsonObject();
            JsonArray help = new JsonArray();
            help.add("startingGold: 첫 접속 시 지급 Gold / deathLoss*: 사망 시 소지금 손실 범위(%) / npcRange: NPC와 거래 가능한 거리(블록)");
            help.add("checkDeathLoss: 사망 시 들고 있던 수표 금액도 같은 비율로 감소 / minCheckAmount: 수표 최소 금액");
            help.add("playerKillOnlyDrops: 플레이어가 직접 잡아야만 전리품을 주는 몹 (자동 농장으로 철·금이 무한히 쌓이는 것 방지). 바닐라처럼 하려면 [] 로 비우세요");
            help.add("employeeWage: 플레이어 상점 직원의 하루 고용비 (출석 초기화 시각에 예금 → 지갑 순으로 지출, 모자라면 그날 휴무). 0 이면 무료");
            help.add("villagerTrading: filtered = 상인 NPC 가 매입하는 물건을 파는 주민 거래만 제거 (기본) / allow = 바닐라 그대로 / block = 주민 거래 전체 차단");
            o.add("_설명", help);
            o.addProperty("configVersion", CONFIG_VERSION);
            o.addProperty("startingGold", startingGold);
            o.addProperty("deathLossMinPercent", deathLossMinPercent);
            o.addProperty("deathLossMaxPercent", deathLossMaxPercent);
            o.addProperty("bankNpcName", bankNpcName);
            o.addProperty("npcRange", npcRange);
            o.addProperty("checkDeathLoss", checkDeathLoss);
            o.addProperty("minCheckAmount", minCheckAmount);
            JsonArray drops = new JsonArray();
            playerKillOnlyDrops.forEach(drops::add);
            o.add("playerKillOnlyDrops", drops);
            o.addProperty("employeeWage", employeeWage);
            o.addProperty("villagerTrading", villagerTrading);
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] economy.json 을 저장하지 못했습니다.", ex);
        }
    }
}
