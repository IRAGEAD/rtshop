package com.rtshop.party;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** config/rtshop/party.json : 파티 인원, 보스 처치 인정 기준, 보스 목록. */
public final class PartyConfig {
    public int maxMembers = 12;
    /** 보스 최대 체력의 몇 % 이상을 직접 입혀야 처치로 인정하는가. */
    public double bossCreditPercent = 10.0;
    /** 일반 몹 처치 목표를 함께 채우는 파티원 거리 (블록). */
    public int shareRadius = 64;
    /** 파티원끼리 PvP 피해를 막는다. */
    public boolean noFriendlyFire = true;
    public final Set<String> bosses = new LinkedHashSet<>();

    public static final List<String> DEFAULT_BOSSES = List.of(
            "minecraft:wither", "minecraft:ender_dragon", "minecraft:elder_guardian", "minecraft:warden",
            "mowziesmobs:ferrous_wroughtnaut", "mowziesmobs:frostmaw", "mowziesmobs:umvuthi", "mowziesmobs:sculptor",
            "aquamirae:captain_cornelia", "aquamirae:maze_mother",
            "cataclysm:ignis", "cataclysm:netherite_monstrosity", "cataclysm:ender_guardian", "cataclysm:the_harbinger",
            "cataclysm:the_leviathan", "cataclysm:ancient_remnant", "cataclysm:maledictus", "cataclysm:scylla",
            "illusioner:illusioner");

    private static PartyConfig current;

    public static synchronized PartyConfig get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void reload() {
        current = null;
    }

    public static synchronized void setForTest(PartyConfig c) {
        current = c;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("party.json");
    }

    public static PartyConfig fresh() {
        PartyConfig c = new PartyConfig();
        c.bosses.addAll(DEFAULT_BOSSES);
        return c;
    }

    private static PartyConfig load() {
        PartyConfig c = fresh();
        Path f = file();
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("maxMembers")) c.maxMembers = Math.max(2, Math.min(64, o.get("maxMembers").getAsInt()));
                if (o.has("bossCreditPercent")) c.bossCreditPercent = Math.max(0, Math.min(100, o.get("bossCreditPercent").getAsDouble()));
                // 3.23.0 테스트판의 기본값 5% 는 새 기본값 10% 로 올린다
                if (c.bossCreditPercent == 5.0 && !o.has("version")) {
                    c.bossCreditPercent = 10.0;
                    o.addProperty("version", 2);
                    o.addProperty("bossCreditPercent", 10.0);
                    Files.writeString(f, new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
                }
                if (o.has("shareRadius")) c.shareRadius = Math.max(0, o.get("shareRadius").getAsInt());
                if (o.has("noFriendlyFire")) c.noFriendlyFire = o.get("noFriendlyFire").getAsBoolean();
                if (o.has("bosses")) {
                    c.bosses.clear();
                    for (JsonElement e : o.getAsJsonArray("bosses")) c.bosses.add(e.getAsString());
                }
            } else {
                Files.createDirectories(f.getParent());
                Files.writeString(f, new GsonBuilder().setPrettyPrinting().create().toJson(c.toJson()), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] party.json 을 읽지 못했습니다: {}", ex.toString());
        }
        return c;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "파티: maxMembers 최대 인원, bossCreditPercent 보스 처치로 인정받는 최소 피해(보스 최대 체력의 %), shareRadius 일반 몹 처치를 함께 채우는 파티원 거리, bosses 보스로 다루는 몹 ID. 저장한 뒤 /rtshop reload");
        o.addProperty("version", 2);
        o.addProperty("maxMembers", maxMembers);
        o.addProperty("bossCreditPercent", bossCreditPercent);
        o.addProperty("shareRadius", shareRadius);
        o.addProperty("noFriendlyFire", noFriendlyFire);
        JsonArray b = new JsonArray();
        for (String s : bosses) b.add(s);
        o.add("bosses", b);
        return o;
    }
}
