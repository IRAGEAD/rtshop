package com.rtshop.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.net.RulesDataPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;

/** OP 편집기(조합법·전리품)의 서버 쪽. */
public final class RulesEditor {
    private RulesEditor() {
    }

    public static final int RECIPE_OPEN = 0;
    public static final int RECIPE_SAVE = 1;
    public static final int LOOT_OPEN = 2;
    public static final int LOOT_SAMPLE = 3;
    public static final int LOOT_SAVE = 4;
    /** 아이템 ID 목록(json) 가운데 하나라도 나오는 전리품표를 찾는다 (key = 화면의 검색어, 그대로 돌려준다). */
    public static final int LOOT_FIND = 5;

    public static final int SAMPLES = 400;

    static void send(ServerPlayer p, int kind, JsonObject o) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new RulesDataPacket(kind, o.toString()));
    }

    public static void handle(ServerPlayer p, int action, String key, String json) {
        if (!com.rtshop.util.Perm.op(p)) {
            return;
        }
        try {
            switch (action) {
                case RECIPE_OPEN -> {
                    JsonObject o = RecipeRules.listJson(p.server);
                    o.add("sold", soldJson());
                    send(p, RulesDataPacket.RECIPES, o);
                }
                case RECIPE_SAVE -> {
                    RecipeRules.Rules r = RecipeRules.Rules.fromJson(JsonParser.parseString(json).getAsJsonObject());
                    RecipeRules.save(r);
                    RecipeRules.applyAndSync(p.server);
                    JsonObject o = RecipeRules.listJson(p.server);
                    o.addProperty("saved", true);
                    o.add("sold", soldJson());
                    send(p, RulesDataPacket.RECIPES, o);
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rules.recipes_saved", RecipeRules.lastDisabled, RecipeRules.lastCustom)
                            .withStyle(ChatFormatting.GREEN));
                    for (String e : RecipeRules.lastErrors) {
                        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rules.recipe_error", e).withStyle(ChatFormatting.RED));
                    }
                    RtShop.LOGGER.info("[rtshop] {} 이(가) 조합법 규칙을 고쳤습니다", p.getGameProfile().getName());
                }
                case LOOT_OPEN -> {
                    JsonObject o = new JsonObject();
                    JsonArray t = new JsonArray();
                    LootRules.editableTables(p.server).forEach(t::add);
                    o.add("tables", t);
                    o.add("rules", LootRules.get().toJson());
                    o.add("sold", soldJson());
                    TABLE_ITEMS.clear();   // 데이터팩이 바뀌었을 수 있으니 창을 열 때마다 찾기용 색인을 새로 만든다
                    send(p, RulesDataPacket.LOOT_LIST, o);
                }
                case LOOT_SAMPLE -> send(p, RulesDataPacket.LOOT_TABLE, sampleJson(p, key));
                case LOOT_SAVE -> {
                    LootRules.Rules r = LootRules.Rules.fromJson(LootRules.get().toJson());
                    LootRules.Table t = json.isBlank() ? new LootRules.Table()
                            : LootRules.Table.fromJson(JsonParser.parseString(json).getAsJsonObject());
                    if (t.empty()) r.tables.remove(key);
                    else r.tables.put(key, t);
                    LootRules.save(r);
                    JsonObject o = sampleJson(p, key);
                    o.addProperty("saved", true);
                    send(p, RulesDataPacket.LOOT_TABLE, o);
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rules.loot_saved", key).withStyle(ChatFormatting.GREEN));
                    RtShop.LOGGER.info("[rtshop] {} 이(가) 전리품 {} 규칙을 고쳤습니다", p.getGameProfile().getName(), key);
                }
                case LOOT_FIND -> {
                    java.util.Set<String> want = new java.util.HashSet<>();
                    for (com.google.gson.JsonElement e : JsonParser.parseString(json).getAsJsonArray()) want.add(e.getAsString());
                    JsonObject o = new JsonObject();
                    o.addProperty("query", key);
                    JsonArray t = new JsonArray();
                    findTables(p.server, want).forEach(t::add);
                    o.add("tables", t);
                    send(p, RulesDataPacket.LOOT_FOUND, o);
                }
                default -> {
                }
            }
        } catch (Exception e) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rules.failed", String.valueOf(e.getMessage())).withStyle(ChatFormatting.RED));
            RtShop.LOGGER.warn("[rtshop] 규칙 편집 실패", e);
        }
    }

    /** NPC 상점에서 살 수 있는 아이템 ID 들 (편집 화면의 "상점에서 파는 것" 분류용). */
    public static JsonArray soldJson() {
        java.util.Set<String> ids = new java.util.TreeSet<>();
        try {
            for (com.rtshop.shop.ShopData sd : com.rtshop.shop.ShopServer.shops().values()) {
                for (com.rtshop.shop.ShopData.Entry e : sd.entries) {
                    if (!e.buyable || e.stack.isEmpty()) continue;
                    ResourceLocation k = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(e.stack.getItem());
                    if (k != null) ids.add(k.toString());
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 상점 품목 목록을 만들지 못했습니다: {}", t.toString());
        }
        JsonArray a = new JsonArray();
        ids.forEach(a::add);
        return a;
    }

    /** 전리품표 → 그 표의 파일에 적힌 이름들(아이템 ID, 다른 표의 ID). 찾기용 색인이라 태그(#…)는 풀지 않는다. */
    private static final Map<String, java.util.Set<String>> TABLE_ITEMS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.regex.Pattern NAME_IN_TABLE = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([a-z0-9_.:/\\-]+)\"");

    static java.util.Set<String> tableItems(net.minecraft.server.MinecraftServer server, String table) {
        java.util.Set<String> cached = TABLE_ITEMS.get(table);
        if (cached != null) return cached;
        java.util.Set<String> out = new java.util.HashSet<>();
        try {
            ResourceLocation id = new ResourceLocation(table);
            ResourceLocation file = new ResourceLocation(id.getNamespace(), "loot_tables/" + id.getPath() + ".json");
            java.util.Optional<net.minecraft.server.packs.resources.Resource> res = server.getResourceManager().getResource(file);
            if (res.isPresent()) {
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader r = res.get().openAsReader()) {
                    char[] buf = new char[4096];
                    int n;
                    while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
                }
                java.util.regex.Matcher m = NAME_IN_TABLE.matcher(sb);
                while (m.find()) {
                    String name = m.group(1);
                    out.add(name.indexOf(':') < 0 ? "minecraft:" + name : name);
                }
            }
        } catch (Exception ignored) {
            // 읽지 못한 표는 빈 것으로 둔다
        }
        TABLE_ITEMS.put(table, out);
        return out;
    }

    /** 이 아이템들 가운데 하나라도 나오는 전리품표 (파일에 적힌 것 + RT Shop 규칙으로 넣은 것 + 그 표를 불러 쓰는 표 한 단계). */
    public static java.util.List<String> findTables(net.minecraft.server.MinecraftServer server, java.util.Set<String> want) {
        java.util.List<String> all = LootRules.editableTables(server);
        java.util.Set<String> hit = new java.util.LinkedHashSet<>();
        if (want.isEmpty()) return new java.util.ArrayList<>(hit);
        for (String table : all) {
            boolean ok = !java.util.Collections.disjoint(tableItems(server, table), want);
            if (!ok) {
                LootRules.Table rule = LootRules.get().tables.get(table);
                if (rule != null) {
                    JsonObject rj = rule.toJson();
                    if (rj.has("add")) {
                        for (com.google.gson.JsonElement e : rj.getAsJsonArray("add")) {
                            if (e.isJsonObject() && e.getAsJsonObject().has("item") && want.contains(e.getAsJsonObject().get("item").getAsString())) ok = true;
                        }
                    }
                }
            }
            if (ok) hit.add(table);
        }
        java.util.Set<String> direct = new java.util.HashSet<>(hit);
        for (String table : all) {
            if (!hit.contains(table) && !java.util.Collections.disjoint(tableItems(server, table), direct)) hit.add(table);
        }
        return new java.util.ArrayList<>(hit);
    }

    static JsonArray stats(Map<String, LootRules.Stat> m, int n) {
        JsonArray a = new JsonArray();
        for (Map.Entry<String, LootRules.Stat> e : m.entrySet()) {
            JsonObject o = new JsonObject();
            o.addProperty("item", e.getKey());
            o.addProperty("chance", e.getValue().hits / (double) n);
            o.addProperty("avg", e.getValue().total / (double) Math.max(1, e.getValue().hits));
            o.addProperty("min", e.getValue().min);
            o.addProperty("max", e.getValue().max);
            a.add(o);
        }
        return a;
    }

    public static JsonObject sampleJson(ServerPlayer p, String table) {
        ResourceLocation id = new ResourceLocation(table);
        JsonObject o = new JsonObject();
        o.addProperty("table", table);
        o.addProperty("samples", SAMPLES);
        o.add("raw", stats(LootRules.sample(p.server.overworld(), id, SAMPLES, true), SAMPLES));
        o.add("after", stats(LootRules.sample(p.server.overworld(), id, SAMPLES, false), SAMPLES));
        LootRules.Table t = LootRules.get().tables.get(table);
        o.add("rule", t == null ? new JsonObject() : t.toJson());
        JsonArray groups = new JsonArray();
        for (String k : LootRules.get().tables.keySet()) {
            if (k.endsWith("*") && table.startsWith(k.substring(0, k.length() - 1))) groups.add(k);
        }
        o.add("groups", groups);
        return o;
    }
}
