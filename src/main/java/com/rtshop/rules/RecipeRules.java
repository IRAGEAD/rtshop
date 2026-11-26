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
        /** 이미 한 번 덧붙인 기본 금지 묶음의 이름 (운영자가 그 뒤에 푼 것을 다시 막지 않으려고). */
        public final Set<String> applied = new LinkedHashSet<>();
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
            JsonArray ap = new JsonArray();
            applied.forEach(ap::add);
            o.add("_applied", ap);
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
            if (o.has("_applied")) {
                for (JsonElement e : o.getAsJsonArray("_applied")) r.applied.add(e.getAsString().trim());
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

    /** 3.58 (C-6): 같은 음식을 훨씬 싸게 만드는 조리법 — 모드의 조리법이라 그 모드가 없으면 아무 일도 없다. */
    public static final List<String> COOK_GAPS = List.of("create:smelting/bread", "create:smoking/bread", "create:campfire_cooking/bread",
            "farmersdelight:pumpkin_pie_from_pie_crust", "create:crafting/curiosities/cake", "kaleidoscope_cookery:chopping_board/sashimi_from_tropical_fish");
    /** 바닐라 조합법: Farmer's Delight 의 냄비 조리법이 남을 때만 막는다 (그 모드가 없으면 만들 길이 없어지므로). */
    public static final List<String> COOK_GAPS_FD = List.of("minecraft:beetroot_soup", "minecraft:rabbit_stew_from_brown_mushroom", "minecraft:rabbit_stew_from_red_mushroom");

    /** 기본 금지 묶음을 한 번만 덧붙인다 (깔린 서버의 recipes.json 은 운영자 것: 뒤에 푼 것은 다시 막지 않는다). 덧붙였으면 true. */
    public static boolean addDefaults(Rules r, boolean farmersDelight) {
        boolean changed = false;
        if (r.applied.add("cook_gaps@1")) {
            r.disabled.addAll(COOK_GAPS);
            changed = true;
        }
        if (farmersDelight && r.applied.add("cook_gaps_fd@1")) {
            r.disabled.addAll(COOK_GAPS_FD);
            changed = true;
        }
        return changed;
    }

    private static Rules load() {
        Path p = path();
        try {
            boolean fd = net.minecraftforge.fml.ModList.get().isLoaded("farmersdelight");
            if (Files.exists(p)) {
                Rules old = Rules.fromJson(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
                if (addDefaults(old, fd)) save(old);
                return old;
            }
            Rules r = new Rules();
            addDefaults(r, fd);
            save(r);
            return r;
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] recipes.json 을 읽지 못했습니다: {}", e.getMessage());
            return new Rules();
        }
    }

    public static synchronized void save(Rules r) throws Exception {
        if (r.applied.isEmpty() && rules != null && rules != r) r.applied.addAll(rules.applied);   // 편집 화면에서 온 규칙에는 이 기록이 없다
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
        if (WorldRules.recipeBlocked(id, output)) return true; // 월드 규칙: 이 서버에서는 만들 수 없는 것
        if (output != null && com.rtshop.story.StorySeals.recipeLocked(output.toString())) return true; // 이야기: 퀘스트 보상으로만
        return output != null && rules.disabled.contains("item:" + output);
    }

    /** 데이터팩이 읽은 원래 조합법 (아직 적어 두지 않았으면 지금 적는다). */
    public static synchronized java.util.Collection<Recipe<?>> originals(MinecraftServer server) {
        RecipeManager rm = server.getRecipeManager();
        if (lastManager.get() != rm) {
            original = new LinkedHashMap<>();
            for (Recipe<?> r : rm.getRecipes()) original.put(r.getId(), r);
            lastManager = new WeakReference<>(rm);
        }
        return new ArrayList<>(original.values());
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
        ItemPolicy.beginRecipes();
        for (Recipe<?> r : original.values()) {
            if (rules.custom.containsKey(r.getId().toString())) continue;
            ResourceLocation made = outputId(r, server);
            if (disabled(rules, r.getId(), made)) {
                off++;
                continue;
            }
            // 아이템 규칙: 총·지팡이·상점과 뽑기에 오른 장비·모드 장비는 만들 수 없다
            String why = ItemPolicy.recipeReason(r.getId(), made);
            if (why != null) {
                ItemPolicy.noteRecipe(why, r.getId(), made);
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
