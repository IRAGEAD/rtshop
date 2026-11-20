package com.rtshop.economy;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/rtshop/economy.json */
public final class EconomyConfig {
    public long startingGold = 100;
    public double deathLossMinPercent = 5.0;
    public double deathLossMaxPercent = 50.0;
    public String bankNpcName = "은행원";
    public double npcRange = 10.0;

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

    private static EconomyConfig load() {
        EconomyConfig c = new EconomyConfig();
        Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("economy.json");
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("startingGold")) c.startingGold = Math.max(0, o.get("startingGold").getAsLong());
                if (o.has("deathLossMinPercent")) c.deathLossMinPercent = o.get("deathLossMinPercent").getAsDouble();
                if (o.has("deathLossMaxPercent")) c.deathLossMaxPercent = o.get("deathLossMaxPercent").getAsDouble();
                if (o.has("bankNpcName")) c.bankNpcName = o.get("bankNpcName").getAsString();
                if (o.has("npcRange")) c.npcRange = o.get("npcRange").getAsDouble();
            } else {
                JsonObject o = new JsonObject();
                o.addProperty("_설명", "startingGold: 첫 접속 시 지급 Gold / deathLoss*: 사망 시 소지금 손실 범위(%) / npcRange: NPC와 거래 가능한 거리(블록)");
                o.addProperty("startingGold", c.startingGold);
                o.addProperty("deathLossMinPercent", c.deathLossMinPercent);
                o.addProperty("deathLossMaxPercent", c.deathLossMaxPercent);
                o.addProperty("bankNpcName", c.bankNpcName);
                o.addProperty("npcRange", c.npcRange);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] economy.json 을 읽지 못해 기본값을 사용합니다.", ex);
        }
        c.deathLossMinPercent = Math.max(0, Math.min(100, c.deathLossMinPercent));
        c.deathLossMaxPercent = Math.max(c.deathLossMinPercent, Math.min(100, c.deathLossMaxPercent));
        return c;
    }
}
