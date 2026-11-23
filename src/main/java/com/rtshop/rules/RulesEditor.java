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

    public static final int SAMPLES = 400;

    static void send(ServerPlayer p, int kind, JsonObject o) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new RulesDataPacket(kind, o.toString()));
    }

    public static void handle(ServerPlayer p, int action, String key, String json) {
        if (!p.hasPermissions(2)) {
            return;
        }
        try {
            switch (action) {
                case RECIPE_OPEN -> send(p, RulesDataPacket.RECIPES, RecipeRules.listJson(p.server));
                case RECIPE_SAVE -> {
                    RecipeRules.Rules r = RecipeRules.Rules.fromJson(JsonParser.parseString(json).getAsJsonObject());
                    RecipeRules.save(r);
                    RecipeRules.applyAndSync(p.server);
                    JsonObject o = RecipeRules.listJson(p.server);
                    o.addProperty("saved", true);
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
                default -> {
                }
            }
        } catch (Exception e) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rules.failed", String.valueOf(e.getMessage())).withStyle(ChatFormatting.RED));
            RtShop.LOGGER.warn("[rtshop] 규칙 편집 실패", e);
        }
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
