package com.rtshop.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** 서버에 로드된 데이터팩의 레시피 파일을 읽어 원가 계산기에 넘긴다. */
public final class RecipeScan {
    private RecipeScan() {
    }

    public static final class Scan {
        public final List<RecipeSolver.Recipe> recipes = new ArrayList<>();
        public RecipeSolver.Result result;
        public RecipeSolver.Env env;
        public int files;
        public int parsed;
        /** 해석하지 못한 레시피의 종류별 개수 (점검 보고서용). */
        public final Map<String, Integer> unparsedTypes = new java.util.TreeMap<>();

        /** 레시피 ID 로 찾기 (가격 근거 표시용). */
        public RecipeSolver.Recipe byId(String id) {
            if (id == null) {
                return null;
            }
            for (RecipeSolver.Recipe r : recipes) {
                if (id.equals(r.id)) {
                    return r;
                }
            }
            return null;
        }
    }

    /** 1단계: 레시피 파일을 모아 해석한다. */
    public static Scan collect(MinecraftServer server, PricingConfig pc) {
        RecipeSolver.Config cfg = pc.solverConfig();
        Scan scan = new Scan();
        List<RecipeSolver.Recipe> recipes = scan.recipes;
        try {
            ResourceManager rm = server.getResourceManager();
            Map<ResourceLocation, Resource> files = rm.listResources("recipes", rl -> rl.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> e : files.entrySet()) {
                scan.files++;
                try (Reader reader = e.getValue().openAsReader()) {
                    JsonElement el = JsonParser.parseReader(reader);
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject json = pick(el.getAsJsonObject(), 0);
                    if (json == null) {
                        continue;
                    }
                    String path = e.getKey().getPath();
                    String id = e.getKey().getNamespace() + ":" + path.substring("recipes/".length(), path.length() - 5);
                    List<RecipeSolver.Recipe> rs = RecipeSolver.parse(id, json, cfg);
                    if (!rs.isEmpty()) {
                        scan.parsed++;
                        recipes.addAll(rs);
                    } else {
                        String type = json.has("type") && json.get("type").isJsonPrimitive() ? json.get("type").getAsString() : "?";
                        scan.unparsedTypes.merge(type, 1, Integer::sum);
                    }
                } catch (Throwable ignored) {
                    // 읽을 수 없는 파일은 건너뜀
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop] 레시피 파일을 읽는 중 오류", t);
        }
        return scan;
    }

    /** 2단계: 기준 가격으로 원가와 판매 가치를 계산한다. */
    public static void solve(Scan scan, Map<String, Double> base, Map<String, Double> hints, PricingConfig pc,
                             ToDoubleFunction<String> estimate) {
        solve(scan, base, hints, java.util.Set.of(), pc, estimate);
    }

    public static void solve(Scan scan, Map<String, Double> base, Map<String, Double> hints, java.util.Set<String> anchors, PricingConfig pc,
                             ToDoubleFunction<String> estimate) {
        RecipeSolver.Config cfg = pc.solverConfig();
        List<RecipeSolver.Recipe> recipes = scan.recipes;
        final Map<String, List<String>> tagCache = new HashMap<>();
        final Map<String, String> remCache = new HashMap<>();
        RecipeSolver.Env env = new RecipeSolver.Env() {
            @Override
            public Collection<String> tagItems(String tagId) {
                return tagCache.computeIfAbsent(tagId, k -> {
                    List<String> out = new ArrayList<>();
                    try {
                        ResourceLocation rl = ResourceLocation.tryParse(k);
                        if (rl != null && ForgeRegistries.ITEMS.tags() != null) {
                            ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(rl)).stream().forEach(item -> {
                                ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
                                if (key != null) {
                                    out.add(key.toString());
                                }
                            });
                        }
                    } catch (Throwable ignored) {
                    }
                    return out;
                });
            }

            @Override
            public String remainder(String itemId) {
                if (remCache.containsKey(itemId)) {
                    return remCache.get(itemId);
                }
                String out = null;
                try {
                    Item item = item(itemId);
                    if (item != null) {
                        ItemStack st = new ItemStack(item);
                        if (st.hasCraftingRemainingItem()) {
                            ItemStack rem = st.getCraftingRemainingItem();
                            if (!rem.isEmpty()) {
                                ResourceLocation key = ForgeRegistries.ITEMS.getKey(rem.getItem());
                                out = key == null ? null : key.toString();
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
                remCache.put(itemId, out);
                return out;
            }

            @Override
            public boolean exists(String itemId) {
                return item(itemId) != null;
            }

            @Override
            public double estimate(String itemId) {
                return estimate.applyAsDouble(itemId);
            }

        };
        scan.env = env;
        scan.result = RecipeSolver.solve(base, hints, recipes, env, cfg, anchors);
    }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        return item == null || item == Items.AIR ? null : item;
    }

    /** 실제로 게임에 적용되는 레시피만 고른다 (설치되지 않은 모드의 레시피, 조건이 맞지 않는 레시피 제외). */
    private static JsonObject pick(JsonObject json, int depth) {
        if (depth > 3 || !json.has("type") || !json.get("type").isJsonPrimitive()) {
            return null;
        }
        String type = json.get("type").getAsString();
        if (type.equals("forge:conditional")) {
            if (!json.has("recipes") || !json.get("recipes").isJsonArray()) {
                return null;
            }
            for (JsonElement el : json.getAsJsonArray("recipes")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject o = el.getAsJsonObject();
                if (!o.has("recipe") || !o.get("recipe").isJsonObject()) {
                    continue;
                }
                if (o.has("conditions") && o.get("conditions").isJsonArray() && !conditionsPass(o.getAsJsonArray("conditions"))) {
                    continue;
                }
                return pick(o.getAsJsonObject("recipe"), depth + 1);
            }
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(type.contains(":") ? type : "minecraft:" + type);
        if (rl == null || !ForgeRegistries.RECIPE_SERIALIZERS.containsKey(rl)) {
            return null;
        }
        if (json.has("conditions") && json.get("conditions").isJsonArray() && !conditionsPass(json.getAsJsonArray("conditions"))) {
            return null;
        }
        return json;
    }

    private static boolean conditionsPass(JsonArray conditions) {
        try {
            return CraftingHelper.processConditions(conditions, ICondition.IContext.EMPTY);
        } catch (Throwable t) {
            return true;
        }
    }
}
