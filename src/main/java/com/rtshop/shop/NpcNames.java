package com.rtshop.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 상인 NPC 의 개인 이름. 머리 위 이름은 "{직함} {이름}" (예: 농작물 상인 한나).
 * config/rtshop/npc_names.json 에서 이름과 형식을 바꿀 수 있고, 이미 서 있는 NPC 도 곧바로 새 이름으로 바뀐다.
 * 키: 상점 ID(farmer, fisher …), bank, job, attendance, raid_guide.
 */
public final class NpcNames {
    private NpcNames() {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final class Config {
        public String format = "{role} {name}";
        public final Map<String, String> names = new LinkedHashMap<>();
    }

    private static Config config;

    public static Map<String, String> defaults() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("farmer", "한나");
        m.put("fisher", "오스카");
        m.put("food", "마르타");
        m.put("blacksmith", "브람");
        m.put("general", "피터");
        m.put("land", "에드먼드");
        m.put("raid", "세라");
        m.put("gacha_shop", "루시");
        m.put("attendance_shop", "엘리");
        m.put("bank", "그레고리");
        m.put("job", "알버트");
        m.put("attendance", "클라라");
        m.put("raid_guide", "발터");
        return m;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("npc_names.json");
    }

    public static synchronized Config get() {
        if (config == null) {
            Config c = new Config();
            c.names.putAll(defaults());
            try {
                Path f = file();
                if (Files.exists(f)) {
                    JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (o.has("format")) c.format = o.get("format").getAsString();
                    if (o.has("names")) {
                        for (Map.Entry<String, com.google.gson.JsonElement> e : o.getAsJsonObject("names").entrySet()) {
                            c.names.put(e.getKey(), e.getValue().getAsString());
                        }
                    }
                } else {
                    save(c);
                }
            } catch (Exception ex) {
                RtShop.LOGGER.warn("[rtshop] npc_names.json 을 읽지 못했습니다: {}", ex.toString());
            }
            config = c;
        }
        return config;
    }

    public static synchronized void reload() {
        config = null;
    }

    private static void save(Config c) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("_help", "상인 NPC 의 이름. format 의 {role} 은 직함(상점 설정의 npcName), {name} 은 아래 이름입니다. 이름을 비우면 직함만 보입니다. 저장한 뒤 /rtshop reload");
            o.addProperty("format", c.format);
            JsonObject n = new JsonObject();
            for (Map.Entry<String, String> e : c.names.entrySet()) n.addProperty(e.getKey(), e.getValue());
            o.add("names", n);
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] npc_names.json 을 쓰지 못했습니다: {}", ex.toString());
        }
    }

    /** 직함과 키로 머리 위 이름을 만든다. */
    public static String display(String key, String role) {
        Config c = get();
        String name = key == null ? "" : c.names.getOrDefault(key, "");
        if (name == null || name.isBlank() || role == null || role.contains(name)) return role;
        return c.format.replace("{role}", role).replace("{name}", name).trim();
    }

    /** 이 NPC 의 키와 직함 (상인이 아니면 null). */
    public static String[] keyAndRole(Entity e) {
        for (String t : e.getTags()) {
            if (t.startsWith(ShopServer.SHOP_TAG_PREFIX)) {
                String id = t.substring(ShopServer.SHOP_TAG_PREFIX.length());
                ShopData d = ShopServer.shops().get(id);
                return d == null ? null : new String[]{id, d.npcName};
            }
            if (t.equals(ShopServer.BANK_TAG)) return new String[]{"bank", com.rtshop.economy.EconomyConfig.get().bankNpcName};
            if (t.equals(com.rtshop.job.Jobs.NPC_TAG)) return new String[]{"job", com.rtshop.job.JobConfig.get().npcName};
            if (t.equals(com.rtshop.attendance.Attendance.NPC_TAG)) return new String[]{"attendance", com.rtshop.attendance.AttendanceConfig.get().npcName};
            if (t.equals(com.rtshop.raid.Raid.GUIDE_TAG)) return new String[]{"raid_guide", com.rtshop.raid.RaidConfig.get().text("npcName")};
        }
        return null;
    }

    /** 서 있는 NPC 의 이름을 설정대로 맞춘다 (바뀌었을 때만). */
    public static void refresh(Entity e) {
        String[] kr = keyAndRole(e);
        if (kr == null || kr[1] == null) return;
        String want = display(kr[0], kr[1]);
        Component cur = e.getCustomName();
        if (cur == null || !cur.getString().equals(want)) {
            e.setCustomName(Component.literal(want));
            e.setCustomNameVisible(true);
        }
    }
}
