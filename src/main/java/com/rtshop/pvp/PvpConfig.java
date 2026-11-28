package com.rtshop.pvp;

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

/**
 * config/rtshop/pvp.json : PvP 켜기/끄기 시스템.
 *  - 마을(성 영역)과 '안전 구역'에서는 누구도 PvP 를 할 수 없다.
 *  - 그 밖에서는 'PvP 가능 구역' 안에서만 (또는 outside 가 true 면 어디서나) PvP 를 켤 수 있다.
 *  - 때리는 쪽과 맞는 쪽이 둘 다 PvP 를 켜고 있어야 피해가 들어간다.
 * 게임 안에서 /rtshop pvp ... 로 바꾸면 이 파일에 저장된다.
 */
public final class PvpConfig {
    public static final class Zone {
        public String name = "";
        public String dim = "minecraft:overworld";
        public int x0, z0, x1, z1;
        /** true = 안전 구역 (PvP 불가). false = PvP 가능 구역. */
        public boolean safe;

        public boolean has(String dimension, int x, int z) {
            return dim.equals(dimension) && x >= x0 && x <= x1 && z >= z0 && z <= z1;
        }

        public long area() {
            return (long) (x1 - x0 + 1) * (z1 - z0 + 1);
        }
    }

    /** false 면 이 시스템을 끈다 (바닐라 규칙 그대로). */
    public boolean enabled = true;
    /** 마을·구역 밖에서도 PvP 를 켤 수 있는가. 3.84 (사용자 11-29 "pvp는 마을 영역 밖에서는 서로 pvp를 킨 경우에는 가능하도록"): 기본 true. */
    public boolean outside = true;
    /** 파일의 판. 2 (3.84): outside 를 켠다. */
    public int version = 2;
    /** 성 영역(아모노스 맵의 성 안)을 안전 구역으로 고정한다. */
    public boolean castleSafe = true;
    /** server.properties 의 pvp=false 여도 이 시스템이 PvP 를 관리하도록 서버 PvP 를 켠다. */
    public boolean manageServerPvp = true;
    /** PvP 로 때리거나 맞은 뒤 이 시간(초) 동안은 PvP 를 끌 수 없다. */
    public int combatSeconds = 10;
    /** 켜고 끄기 사이의 최소 간격(초). */
    public int switchSeconds = 2;
    /** 구역에 들어가고 나올 때 안내 문구를 보여 준다. */
    public boolean announceArea = true;
    public final List<Zone> zones = new ArrayList<>();

    private static PvpConfig instance;

    public static synchronized PvpConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized PvpConfig reload() {
        instance = load();
        return instance;
    }

    /** 점검용: 파일을 건드리지 않고 설정을 바꿔 끼운다. */
    public static synchronized void setForTest(PvpConfig c) {
        instance = c;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("pvp.json");
    }

    public Zone zone(String name) {
        for (Zone z : zones) {
            if (z.name.equalsIgnoreCase(name)) {
                return z;
            }
        }
        return null;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("PvP 켜기/끄기: 플레이어는 /pvp (또는 조작 설정의 'PvP 켜기/끄기' 키)로 자기 PvP 를 켜고 끕니다. 둘 다 켜고 있어야 서로 피해가 들어갑니다.");
        help.add("마을(성 영역)과 안전 구역(safe: true)에서는 아무도 PvP 를 할 수 없습니다. 그 밖에서는 PvP 가능 구역 안에서만 켤 수 있습니다.");
        help.add("outside: true(기본, 3.84) 면 마을 밖 어디서나 PvP 를 켤 수 있습니다 — 둘 다 켜야 서로 피해가 들어갑니다. false 면 PvP 가능 구역 안에서만. 게임 안에서는 /rtshop pvp outside on|off");
        help.add("zones: PvP 가능 구역(safe: false)·안전 구역(safe: true). 게임 안에서 /rtshop pvp pos1, pos2, zone add <이름> [safe] 로 만듭니다.");
        help.add("combatSeconds: PvP 로 때리거나 맞은 뒤 이 시간 동안은 PvP 를 끌 수 없습니다 · switchSeconds: 켜고 끄기 사이 최소 간격");
        o.add("_설명", help);
        o.addProperty("version", version);
        o.addProperty("enabled", enabled);
        o.addProperty("outside", outside);
        o.addProperty("castleSafe", castleSafe);
        o.addProperty("manageServerPvp", manageServerPvp);
        o.addProperty("combatSeconds", combatSeconds);
        o.addProperty("switchSeconds", switchSeconds);
        o.addProperty("announceArea", announceArea);
        JsonArray zs = new JsonArray();
        for (Zone z : zones) {
            JsonObject j = new JsonObject();
            j.addProperty("name", z.name);
            j.addProperty("dim", z.dim);
            j.addProperty("x0", z.x0);
            j.addProperty("z0", z.z0);
            j.addProperty("x1", z.x1);
            j.addProperty("z1", z.z1);
            j.addProperty("safe", z.safe);
            zs.add(j);
        }
        o.add("zones", zs);
        return o;
    }

    public synchronized void save() {
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson()), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] pvp.json 을 저장하지 못했습니다.", ex);
        }
    }

    public static PvpConfig parse(JsonObject o) {
        PvpConfig c = new PvpConfig();
        if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
        if (o.has("outside")) c.outside = o.get("outside").getAsBoolean();
        if (o.has("castleSafe")) c.castleSafe = o.get("castleSafe").getAsBoolean();
        if (o.has("manageServerPvp")) c.manageServerPvp = o.get("manageServerPvp").getAsBoolean();
        if (o.has("combatSeconds")) c.combatSeconds = Math.max(0, Math.min(600, o.get("combatSeconds").getAsInt()));
        if (o.has("switchSeconds")) c.switchSeconds = Math.max(0, Math.min(60, o.get("switchSeconds").getAsInt()));
        if (o.has("announceArea")) c.announceArea = o.get("announceArea").getAsBoolean();
        if (o.has("zones") && o.get("zones").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("zones")) {
                if (!e.isJsonObject()) continue;
                JsonObject j = e.getAsJsonObject();
                Zone z = new Zone();
                z.name = j.has("name") ? j.get("name").getAsString() : "";
                if (j.has("dim")) z.dim = j.get("dim").getAsString();
                int ax = j.has("x0") ? j.get("x0").getAsInt() : 0, bx = j.has("x1") ? j.get("x1").getAsInt() : 0;
                int az = j.has("z0") ? j.get("z0").getAsInt() : 0, bz = j.has("z1") ? j.get("z1").getAsInt() : 0;
                z.x0 = Math.min(ax, bx);
                z.x1 = Math.max(ax, bx);
                z.z0 = Math.min(az, bz);
                z.z1 = Math.max(az, bz);
                z.safe = j.has("safe") && j.get("safe").getAsBoolean();
                if (!z.name.isBlank() && c.zone(z.name) == null) {
                    c.zones.add(z);
                }
            }
        }
        return c;
    }

    private static PvpConfig load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                PvpConfig c = parse(o);
                int v = o.has("version") ? o.get("version").getAsInt() : 1;
                if (v < 2) {   // 3.84: 마을 밖에서는 서로 켜면 PvP (예전 기본값 false 를 true 로; 다른 값은 그대로)
                    c.outside = true;
                    c.version = 2;
                    c.save();
                    RtShop.LOGGER.info("[rtshop] pvp.json 을 새 판으로 고쳤습니다: 마을 밖에서는 서로 PvP 를 켜면 싸울 수 있습니다 (outside).");
                }
                return c;
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] pvp.json 을 읽지 못해 기본값을 사용합니다.", ex);
            return new PvpConfig();
        }
        PvpConfig c = new PvpConfig();
        c.save();
        return c;
    }
}
