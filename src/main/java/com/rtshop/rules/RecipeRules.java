package com.rtshop.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 서버 조합법 규칙 (config/rtshop/recipes.json).
 *  - disabled: 막을 조합법. 조합법 ID, "item:아이템ID"(그 아이템을 만드는 모든 조합법), "mod:모드ID"(그 모드의 모든 조합법).
 *  - custom: 직접 만든 조합법 (바닐라 데이터팩 형식). 기존 조합법과 ID 가 같으면 그것을 바꾼다(조절).
 * 데이터팩을 읽을 때(/reload 포함)마다 다시 적용하고, 막힌 조합법은 클라이언트(JEI 등)에서도 사라진다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class RecipeRules {
    private RecipeRules() {
    }

    public static final class Rules {
        public final Set<String> disabled = new LinkedHashSet<>();
        /** id -> 조합법 JSON. */
        public final Map<String, JsonObject> custom = new LinkedHashMap<>();

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            JsonArray help = new JsonArray();
            help.add("서버 조합법 규칙. 게임 안에서 /rtshop recipes (OP) 로도 고칠 수 있습니다.");
            help.add("disabled: 막을 조합법. 조합법 ID(예: minecraft:tnt), item:아이템ID(그 아이템을 만드는 조합법 전부), mod:모드ID(그 모드의 조합법 전부)");
            help.add("custom: 직접 만든 조합법. {id, recipe: 데이터팩 형식}. 기존 조합법과 ID 가 같으면 그 조합법을 바꿉니다");
            help.add("고친 뒤 /rtshop reload 또는 /reload");
            o.add("_help", help);
            JsonArray d = new JsonArray();
            disabled.forEach(d::add);
            o.add("disabled", d);
            JsonArray c = new JsonArray();
            for (Map.Entry<String, JsonObject> e : custom.entrySet()) {
                JsonObject x = new JsonObject();
                x.addProperty("id", e.getKey());
                x.add("recipe", e.getValue());
                c.add(x);
            }
            o.add("custom", c);
            return o;
        }

        public static Rules fromJson(JsonObject o) {
            Rules r = new Rules();
            if (o.has("disabled")) {
                for (JsonElement e : o.getAsJsonArray("disabled")) {
                    String s = e.getAsString().trim();
                    if (!s.isEmpty()) r.disabled.add(s);
                }
            }
            if (o.has("custom")) {
                for (JsonElement e : o.getAsJsonArray("custom")) {
                    JsonObject x = e.getAsJsonObject();
                    r.custom.put(x.get("id").getAsString().trim(), x.getAsJsonObject("recipe"));
                }
            }
            return r;
        }
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("recipes.json");
    }

    private static Rules rules;

    public static synchronized Rules get() {
        if (rules == null) rules = load();
        return rules;
    }

    private static Rules load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                return Rules.fromJson(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
            }
            Rules r = new Rules();
            save(r);
            return r;
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] recipes.json 을 읽지 못했습니다: {}", e.getMessage());
            return new Rules();
        }
    }

    public static synchronized void save(Rules r) throws Exception {
        Files.createDirectories(path().getParent());
        Files.writeString(path(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(r.toJson()), StandardCharsets.UTF_8);
        rules = r;
    }

    public static synchronized void reload() {
        rules = load();
    }

    // ------------------------------------------------------------------ 적용

    /** 데이터팩이 읽은 원래 조합법 (규칙을 바꿔도 여기서 다시 계산). */
    private static WeakReference<RecipeManager> lastManager = new WeakReference<>(null);
    private static Map<ResourceLocation, Recipe<?>> original = new LinkedHashMap<>();
    public static int lastDisabled, lastCustom;
    public static final List<String> lastErrors = new ArrayList<>();

    public static ResourceLocation outputId(Recipe<?> r, MinecraftServer server) {
        try {
            ItemStack out = r.getResultItem(server.registryAccess());
            return out.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(out.getItem());
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean disabled(Rules rules, ResourceLocation id, ResourceLocation output) {
        if (rules.disabled.contains(id.toString())) return true;
        if (rules.disabled.contains("mod:" + id.getNamespace())) return true;
        return output != null && rules.disabled.contains("item:" + output);
    }

    /** 원래 조합법(데이터팩)에서 규칙을 적용한 결과로 바꾼다. */
    public static synchronized void apply(MinecraftServer server, Rules rules) {
        RecipeManager rm = server.getRecipeManager();
        if (lastManager.get() != rm) {
            original = new LinkedHashMap<>();
            for (Recipe<?> r : rm.getRecipes()) original.put(r.getId(), r);
            lastManager = new WeakReference<>(rm);
        }
        List<Recipe<?>> out = new ArrayList<>();
        int off = 0;
        for (Recipe<?> r : original.values()) {
            if (rules.custom.containsKey(r.getId().toString())) continue;
            if (disabled(rules, r.getId(), outputId(r, server))) {
                off++;
                continue;
            }
            out.add(r);
        }
        lastErrors.clear();
        int made = 0;
        for (Map.Entry<String, JsonObject> e : rules.custom.entrySet()) {
            try {
                ResourceLocation id = new ResourceLocation(e.getKey());
                Recipe<?> r = RecipeManager.fromJson(id, e.getValue(), ICondition.IContext.EMPTY);
                if (rules.disabled.contains(id.toString())) {
                    off++;
                    continue;
                }
                out.add(r);
                made++;
            } catch (Exception ex) {
                lastErrors.add(e.getKey() + ": " + ex.getMessage());
                RtShop.LOGGER.warn("[rtshop] 직접 만든 조합법 {} 을 읽지 못했습니다: {}", e.getKey(), ex.getMessage());
            }
        }
        rm.replaceRecipes(out);
        lastDisabled = off;
        lastCustom = made;
        RtShop.LOGGER.info("[rtshop] 조합법 규칙 적용: 막음 {}, 직접 만든 것 {}", off, made);
    }

    /** 규칙을 바꾼 뒤: 적용하고 접속한 사람들에게 조합법을 다시 보낸다. */
    public static void applyAndSync(MinecraftServer server) {
        apply(server, get());
        server.getPlayerList().reloadResources();
    }

    public static Map<ResourceLocation, Recipe<?>> originalRecipes() {
        return original;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getRecipeManager() != null) {
            reload();
            apply(server, get());
        }
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        reload();
        apply(event.getServer(), get());
    }

    /** 편집기에 보낼 조합법 목록 (원래 조합법 + 직접 만든 것). */
    public static JsonObject listJson(MinecraftServer server) {
        apply(server, get());
        Rules r = get();
        JsonArray arr = new JsonArray();
        Set<String> seen = new LinkedHashSet<>();
        for (Recipe<?> rec : original.values()) {
            JsonObject o = new JsonObject();
            String id = rec.getId().toString();
            seen.add(id);
            o.addProperty("id", id);
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(rec.getType());
            o.addProperty("type", type == null ? "?" : type.getPath());
            ResourceLocation out = outputId(rec, server);
            o.addProperty("out", out == null ? "" : out.toString());
            int count = 1;
            try {
                count = rec.getResultItem(server.registryAccess()).getCount();
            } catch (Throwable ignored) {
            }
            o.addProperty("count", count);
            arr.add(o);
        }
        for (Map.Entry<String, JsonObject> e : r.custom.entrySet()) {
            if (seen.contains(e.getKey())) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", e.getKey());
            o.addProperty("type", "custom");
            JsonObject res = e.getValue().has("result") && e.getValue().get("result").isJsonObject() ? e.getValue().getAsJsonObject("result") : null;
            o.addProperty("out", res != null && res.has("item") ? res.get("item").getAsString() : "");
            o.addProperty("count", res != null && res.has("count") ? res.get("count").getAsInt() : 1);
            arr.add(o);
        }
        JsonObject o = new JsonObject();
        o.add("recipes", arr);
        o.add("rules", r.toJson());
        JsonArray err = new JsonArray();
        lastErrors.forEach(err::add);
        o.add("errors", err);
        return o;
    }
}
