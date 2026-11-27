package com.rtshop.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 레시피 원가 계산기 (마인크래프트 클래스에 의존하지 않는 순수 자바 코드).
 *
 * 레시피 JSON(재료·결과)을 읽어 "재료를 상점에서 사서 만들 때 드는 최소 비용"을 계산합니다.
 *  - cost  : 마진 없이 재료값만 더한 원가
 *  - value : 조리처럼 시간이 드는 가공 단계마다 마진(예: +25%)을 붙인 판매 가치
 * 음식은 value 로 매입하므로 재료를 사서 조리해 팔면 항상 본전 이상이 됩니다.
 */
public final class RecipeSolver {
    private RecipeSolver() {
    }

    /** 게임 정보(태그, 조합 후 남는 아이템 등)를 제공하는 인터페이스. */
    public interface Env {
        /** 태그에 속한 아이템 ID 목록 (없으면 빈 목록). */
        Collection<String> tagItems(String tagId);

        /** 조합 후 돌려받는 아이템(양동이 등). 없으면 null, 자기 자신이면 같은 ID. */
        String remainder(String itemId);

        boolean exists(String itemId);

        /** 가격표에 없는 말단 재료의 추정 가격. */
        double estimate(String itemId);
    }

    public static final class Config {
        /** 레시피 종류별 마진 (0.25 = +25%). */
        public final Map<String, Double> margins = new HashMap<>();
        public double defaultMargin = 0.0;
        /** 레시피 1회당 추가 비용(연료 등). 원가에 포함된다. */
        public final Map<String, Double> flatExtra = new HashMap<>();
        /** 레시피 1회당 수고비(G). 원가에는 넣지 않고 판매 가치에만 더한다. */
        public final Map<String, Double> labor = new HashMap<>();
        /** 레시피 JSON 에는 없지만 실제로 소비되는 재료 (예: 덮밥의 밥). */
        public final Map<String, List<String>> typeExtras = new HashMap<>();
        /** 액체 1회 사용분의 비용을 아이템으로 환산: 액체ID -> "아이템ID*개수". */
        public final Map<String, String> fluidCosts = new HashMap<>();
        public final Set<String> ignoreTypes = new HashSet<>();
        public final Set<String> ignoreNamespaces = new HashSet<>();

        public double margin(String type) {
            Double m = margins.get(type);
            return m != null ? m : defaultMargin;
        }
    }

    public static final class Ing {
        public final List<String> alts = new ArrayList<>(); // "modid:item" 또는 "#modid:tag"
        public double count = 1;
    }

    public static final class Out {
        public final String item;
        public final double count;

        public Out(String item, double count) {
            this.item = item;
            this.count = count;
        }
    }

    public static final class Recipe {
        public String id = "";
        public String type = "";
        public final List<Ing> ingredients = new ArrayList<>();
        public final List<Out> outputs = new ArrayList<>();
    }

    public static final class Result {
        public final Map<String, Double> cost = new HashMap<>();
        public final Map<String, Double> value = new HashMap<>();
        public final Map<String, String> costRecipe = new HashMap<>();
        public final Map<String, String> valueRecipe = new HashMap<>();
        /** value 를 정한 레시피에 마진이 있었는지(= 조리 가공품인지). */
        public final Set<String> processed = new HashSet<>();
        public final Map<String, Double> estimated = new LinkedHashMap<>();
        /** 추정가 없이, 가격표에 있는 재료만으로 원가가 계산된 아이템. */
        public final Set<String> exact = new HashSet<>();
        /** 계산에 쓴 가격표 (상점 판매가 + 살 수는 없지만 값이 정해진 재료). */
        public Map<String, Double> known = new HashMap<>();
        /** 각 아이템의 원가/가치를 정한 레시피에서 "만들어 쓰는" 것으로 계산된 재료 (고리 검사용). */
        public final Map<String, List<String>> costDeriv = new HashMap<>();
        public final Map<String, List<String>> valueDeriv = new HashMap<>();
        public int recipesUsed = 0;
        public int recipesUnresolved = 0;
        public final List<String> unresolved = new ArrayList<>();
    }

    // =====================================================================================
    // JSON -> Recipe
    // =====================================================================================

    private static final String[] SINGLE_KEYS = {"ingredient", "input", "base", "addition", "template", "catalyst"};
    private static final String[] LIST_KEYS = {"ingredients", "inputs", "gems"};
    private static final String[] CONTAINER_KEYS = {"container", "carrier"};
    private static final String[] FLUID_KEYS = {"fluid", "soup_base", "tea_fluid"};

    /** 레시피 JSON 을 해석합니다. 이해하지 못하는 형식이면 빈 목록. */
    public static List<Recipe> parse(String id, JsonObject json, Config cfg) {
        List<Recipe> out = new ArrayList<>();
        try {
            parseInto(id, json, cfg, out);
        } catch (RuntimeException ignored) {
            // 알 수 없는 형식은 건너뜀
        }
        return out;
    }

    private static void parseInto(String id, JsonObject json, Config cfg, List<Recipe> out) {
        String type = str(json, "type");
        if (type == null) {
            return;
        }
        if (!type.contains(":")) {
            type = "minecraft:" + type;
        }
        if (type.equals("forge:conditional") && json.has("recipes") && json.get("recipes").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("recipes")) {
                if (el.isJsonObject() && el.getAsJsonObject().has("recipe") && el.getAsJsonObject().get("recipe").isJsonObject()) {
                    parseInto(id, el.getAsJsonObject().getAsJsonObject("recipe"), cfg, out);
                }
            }
            return;
        }
        if (cfg.ignoreTypes.contains(type) || cfg.ignoreNamespaces.contains(type.substring(0, type.indexOf(':')))) {
            return;
        }
        Recipe r = new Recipe();
        r.id = id;
        r.type = type;

        // --- 결과
        JsonElement res = json.has("result") ? json.get("result") : (json.has("results") ? json.get("results")
                : (json.has("output") ? json.get("output") : null));
        if (res == null) {
            return;
        }
        double defaultCount = json.has("count") && json.get("count").isJsonPrimitive() ? num(json.get("count"), 1) : 1;
        if (res.isJsonArray()) {
            for (JsonElement el : res.getAsJsonArray()) {
                Out o = parseOut(el, 1);
                if (o != null) {
                    r.outputs.add(o);
                }
            }
        } else {
            Out o = parseOut(res, defaultCount);
            if (o != null) {
                r.outputs.add(o);
            }
        }
        if (r.outputs.isEmpty()) {
            return;
        }
        for (Out o : r.outputs) {
            if (o.count > 64) {
                return; // 한 번에 64개를 넘게 만드는 레시피는 장난·관리자용 (예: 금 29개 → 네더라이트 233개)
            }
        }

        // --- 재료
        if (json.has("pattern") && json.has("key") && json.get("pattern").isJsonArray() && json.get("key").isJsonObject()) {
            Map<Character, Integer> counts = new HashMap<>();
            for (JsonElement row : json.getAsJsonArray("pattern")) {
                for (char c : row.getAsString().toCharArray()) {
                    if (c != ' ') {
                        counts.merge(c, 1, Integer::sum);
                    }
                }
            }
            JsonObject key = json.getAsJsonObject("key");
            for (Map.Entry<Character, Integer> e : counts.entrySet()) {
                String k = String.valueOf(e.getKey());
                if (!key.has(k)) {
                    return; // 깨진 레시피
                }
                Ing ing = parseIng(key.get(k));
                if (ing == null) {
                    return;
                }
                ing.count *= e.getValue();
                r.ingredients.add(ing);
            }
        }
        for (String k : LIST_KEYS) {
            if (json.has(k) && json.get(k).isJsonArray()) {
                for (JsonElement el : json.getAsJsonArray(k)) {
                    Ing ing = parseIng(el);
                    if (ing == null) {
                        return;
                    }
                    r.ingredients.add(ing);
                }
            }
        }
        for (String k : SINGLE_KEYS) {
            if (json.has(k) && !json.get(k).isJsonNull()) {
                Ing ing = parseIng(json.get(k));
                if (ing == null) {
                    return;
                }
                if (k.equals("ingredient") && json.has("ingredient_count")) {
                    ing.count *= Math.max(1, num(json.get("ingredient_count"), 1));
                }
                r.ingredients.add(ing);
            }
        }
        for (String k : CONTAINER_KEYS) {
            if (json.has(k) && !json.get(k).isJsonNull()) {
                Ing ing = parseIng(json.get(k));
                if (ing != null) {
                    r.ingredients.add(ing);
                }
            }
        }
        for (String k : FLUID_KEYS) {
            if (json.has(k) && json.get(k).isJsonPrimitive()) {
                String spec = cfg.fluidCosts.get(json.get(k).getAsString());
                if (spec != null && !spec.isEmpty()) {
                    Ing ing = new Ing();
                    int star = spec.indexOf('*');
                    ing.alts.add(star < 0 ? spec : spec.substring(0, star));
                    ing.count = star < 0 ? 1 : Double.parseDouble(spec.substring(star + 1));
                    r.ingredients.add(ing);
                }
            }
        }
        List<String> extras = cfg.typeExtras.get(type);
        if (extras != null) {
            for (String ex : extras) {
                Ing ing = new Ing();
                ing.alts.add(ex);
                r.ingredients.add(ing);
            }
        }
        if (r.ingredients.isEmpty()) {
            return;
        }
        out.add(r);
    }

    private static Out parseOut(JsonElement el, double defaultCount) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (el.isJsonPrimitive()) {
            String s = el.getAsString();
            return s.contains(":") ? new Out(s, defaultCount) : new Out("minecraft:" + s, defaultCount);
        }
        if (!el.isJsonObject()) {
            return null;
        }
        JsonObject o = el.getAsJsonObject();
        JsonElement idEl = o.has("item") ? o.get("item") : (o.has("id") ? o.get("id") : null);
        if (idEl == null) {
            return null;
        }
        if (idEl.isJsonObject()) { // {"item": {"item": "..."}}
            return parseOut(idEl, o.has("count") ? num(o.get("count"), defaultCount) : defaultCount);
        }
        String id = idEl.getAsString();
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        double count = o.has("count") ? num(o.get("count"), defaultCount) : (o.has("amount") ? num(o.get("amount"), defaultCount) : defaultCount);
        if (o.has("chance")) {
            count *= Math.max(0, Math.min(1, num(o.get("chance"), 1)));
        }
        return count > 0 ? new Out(id, count) : null;
    }

    private static Ing parseIng(JsonElement el) {
        Ing ing = new Ing();
        if (!collectAlts(el, ing, 0)) {
            return null;
        }
        return ing.alts.isEmpty() ? null : ing;
    }

    /** 재료 JSON 에서 대체 가능한 아이템/태그를 모읍니다. */
    private static boolean collectAlts(JsonElement el, Ing ing, int depth) {
        if (el == null || el.isJsonNull() || depth > 4) {
            return false;
        }
        if (el.isJsonPrimitive()) {
            String s = el.getAsString();
            if (s.isEmpty()) {
                return false;
            }
            ing.alts.add(s.startsWith("#") || s.contains(":") ? s : "minecraft:" + s);
            return true;
        }
        if (el.isJsonArray()) {
            boolean any = false;
            for (JsonElement e : el.getAsJsonArray()) {
                any |= collectAlts(e, ing, depth + 1);
            }
            return any;
        }
        JsonObject o = el.getAsJsonObject();
        if (o.has("count") && o.get("count").isJsonPrimitive()) {
            ing.count *= Math.max(1, num(o.get("count"), 1));
        } else if (o.has("amount") && o.get("amount").isJsonPrimitive()) {
            ing.count *= Math.max(1, num(o.get("amount"), 1));
        }
        if (o.has("item")) {
            JsonElement it = o.get("item");
            if (it.isJsonObject() || it.isJsonArray()) {
                return collectAlts(it, ing, depth + 1);
            }
            String s = it.getAsString();
            ing.alts.add(s.contains(":") ? s : "minecraft:" + s);
            return true;
        }
        if (o.has("tag") && o.get("tag").isJsonPrimitive()) {
            String s = o.get("tag").getAsString();
            ing.alts.add("#" + (s.contains(":") ? s : "minecraft:" + s));
            return true;
        }
        for (String k : new String[]{"ingredient", "children", "items", "values", "base"}) {
            if (o.has(k)) {
                return collectAlts(o.get(k), ing, depth + 1);
            }
        }
        return false;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    private static double num(JsonElement e, double def) {
        try {
            return e.getAsDouble();
        } catch (RuntimeException ex) {
            return def;
        }
    }

    // =====================================================================================
    // 풀이
    // =====================================================================================

    private static final double INF = Double.MAX_VALUE;

    /**
     * @param base  상점 가격표(구매가). 레시피로 만들 수 없는 원재료의 기준 가격.
     * @param hints 상점에서 살 수는 없지만 값이 정해져 있는 재료(직접 캐거나 낚는 것)의 값. 가격표와 똑같이 취급한다.
     */
    public static Result solve(Map<String, Double> base, Map<String, Double> hints, List<Recipe> recipes, Env env, Config cfg) {
        return solve(base, hints, recipes, env, cfg, Set.of());
    }

    /** @param anchors 상점 값을 그대로 원가로 쓰는 품목 (레시피로 더 싸게 잡지 않음). */
    public static Result solve(Map<String, Double> base, Map<String, Double> hints, List<Recipe> recipes, Env env, Config cfg,
                               Set<String> anchors) {
        Map<String, Double> known = new HashMap<>();
        if (hints != null) {
            for (Map.Entry<String, Double> e : hints.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) {
                    known.put(e.getKey(), Math.max(0.01, e.getValue()));
                }
            }
        }
        known.putAll(base);
        return solve(known, recipes, env, cfg, anchors);
    }

    /**
     * @param base 값이 정해진 재료의 가격표.
     */
    public static Result solve(Map<String, Double> base, List<Recipe> recipes, Env env, Config cfg) {
        return solve(base, recipes, env, cfg, Set.of());
    }

    public static Result solve(Map<String, Double> base, List<Recipe> recipes, Env env, Config cfg, Set<String> anchors) {
        Result res = new Result();
        Set<String> pinned = new HashSet<>();
        for (String a : anchors) {
            Double v = base.get(a);
            if (v != null) {
                pinned.add(a);
                res.cost.put(a, v);
                res.value.put(a, v);
            }
        }
        res.known = base;
        Set<String> craftable = new HashSet<>();
        for (Recipe r : recipes) {
            for (Out o : r.outputs) {
                craftable.add(o.item);
            }
        }
        Map<String, Double> est = res.estimated;

        // 1단계: 가격표에 있는 재료만으로 계산한다. 여기서 정해진 값은 이후 단계에서 바뀌지 않는다
        // (가격을 모르는 재료의 추정가 때문에 멀쩡한 아이템이 싸게 계산되는 것을 막기 위함).
        relax(base, est, recipes, env, cfg, res, pinned);
        res.exact.addAll(res.cost.keySet());
        Set<String> frozen = new HashSet<>(res.cost.keySet());
        frozen.addAll(base.keySet());

        for (int round = 0; round < 6; round++) {
            if (round > 0) {
                res.cost.keySet().retainAll(frozen);
                res.value.keySet().retainAll(frozen);
                res.costRecipe.keySet().retainAll(frozen);
                res.valueRecipe.keySet().retainAll(frozen);
                res.processed.retainAll(frozen);
                res.costDeriv.keySet().retainAll(frozen);
                res.valueDeriv.keySet().retainAll(frozen);
                relax(base, est, recipes, env, cfg, res, frozen);
            }

            // 풀리지 않은 레시피의 말단 재료(가격표에도 없고 만들 수도 없는 것)에 추정가를 준다
            boolean added = false;
            res.unresolved.clear();
            for (Recipe r : recipes) {
                if (ingredientSum(r, base, est, res, env, false) < INF) {
                    continue;
                }
                res.unresolved.add(r.id);
                for (Ing ing : r.ingredients) {
                    if (bestAlt(ing, base, est, res, env, false, false) < INF) {
                        continue;
                    }
                    for (String alt : ing.alts) {
                        Collection<String> items = alt.startsWith("#") ? env.tagItems(alt.substring(1)) : List.of(alt);
                        for (String item : items) {
                            if (base.containsKey(item) || est.containsKey(item) || !env.exists(item)) {
                                continue;
                            }
                            if (!craftable.contains(item)) {
                                est.put(item, Math.max(0.01, env.estimate(item)));
                                added = true;
                            }
                        }
                    }
                }
            }
            if (!added) {
                break;
            }
        }
        res.recipesUnresolved = res.unresolved.size();
        res.recipesUsed = recipes.size() - res.recipesUnresolved;
        return res;
    }

    /** 레시피 하나를 계산하기 좋게 정리한 것. */
    private static final class Prepared {
        boolean[] returned;                       // 쓰고 나면 그대로 돌려받는 재료 (원가 0)
        final List<Out> outs = new ArrayList<>(); // 돌려받는 재료를 뺀 실제 결과물
        double outSum;
    }

    private static final class Choice {
        double total;
        /** 사지 않고 만들어 쓰는 것으로 계산된 재료 (고리 검사용). */
        final List<String> via = new ArrayList<>();
    }

    private static boolean holds(Ing ing, String item, Env env) {
        // 재료가 정확히 그 아이템 하나일 때만 "되돌아오는 재료"로 본다.
        // 태그까지 따지면 "달걀 태그 -> 달걀 프라이"처럼 결과물이 재료 태그에 들어 있는 조리법을 잘못 걸러 낸다.
        return ing.alts.size() == 1 && ing.alts.get(0).equals(item);
    }

    /**
     * 재료가 결과물로 그대로 되돌아오는 레시피(예: 자갈을 손질하면 자갈 + 가끔 부싯돌)는
     * 그 재료를 도구처럼 취급한다: 재료값은 0 이고, 되돌아온 것은 결과물로 치지 않는다.
     */
    private static Prepared prepare(Recipe r, Env env) {
        Prepared p = new Prepared();
        p.returned = new boolean[r.ingredients.size()];
        p.outs.addAll(r.outputs);
        for (int i = 0; i < r.ingredients.size(); i++) {
            Ing ing = r.ingredients.get(i);
            for (int k = 0; k < p.outs.size(); k++) {
                Out o = p.outs.get(k);
                if (o.count + 1.0e-9 >= ing.count && holds(ing, o.item, env)) {
                    p.returned[i] = true;
                    p.outs.remove(k);
                    break;
                }
            }
        }
        for (Out o : p.outs) {
            p.outSum += o.count;
        }
        return p;
    }

    private static boolean viaRecipe(String item, Map<String, Double> base, Map<String, Double> est, Result res, boolean forValue) {
        if (forValue && res.processed.contains(item) && res.value.containsKey(item)) {
            return true;
        }
        Double b = base.get(item);
        if (b == null) {
            b = est.get(item);
        }
        Double c = res.cost.get(item);
        return c != null && (b == null || c < b - 1.0e-12);
    }

    private static Choice choose(Recipe r, Prepared prep, Map<String, Double> base, Map<String, Double> est, Result res, Env env,
                                 boolean forValue) {
        Choice c = new Choice();
        boolean grid = r.type.startsWith("minecraft:crafting");
        for (int i = 0; i < r.ingredients.size(); i++) {
            if (prep.returned[i]) {
                continue;
            }
            Ing ing = r.ingredients.get(i);
            String bestItem = cheapest(ing, prep, base, est, res, env, forValue, grid);
            if (bestItem == null) {
                return null;
            }
            c.total += net(bestItem, base, est, res, env, forValue, grid) * ing.count;
            if (viaRecipe(bestItem, base, est, res, forValue)) {
                c.via.add(bestItem);
            }
        }
        return c;
    }

    /**
     * 재료 한 칸에 쓸 수 있는 것 중 가장 싼 아이템. 없으면 null.
     * 이 레시피의 결과물 자신은 후보에서 뺀다 (달걀 태그에 달걀 프라이가 들어 있는 경우처럼 자기 자신을 재료로 고르지 않게).
     * 값이 같으면 가격표에 있는(만들지 않아도 되는) 쪽을 고른다.
     */
    private static String cheapest(Ing ing, Prepared prep, Map<String, Double> base, Map<String, Double> est, Result res, Env env,
                                   boolean forValue, boolean grid) {
        double best = INF;
        String bestItem = null;
        boolean bestMade = false;
        for (String alt : ing.alts) {
            Collection<String> items = alt.startsWith("#") ? env.tagItems(alt.substring(1)) : List.of(alt);
            for (String item : items) {
                boolean own = false;
                for (Out o : prep.outs) {
                    if (o.item.equals(item)) {
                        own = true;
                        break;
                    }
                }
                if (own) {
                    continue;
                }
                double w = net(item, base, est, res, env, forValue, grid);
                if (w >= INF) {
                    continue;
                }
                boolean made = viaRecipe(item, base, est, res, forValue);
                if (w < best - 1.0e-9 || (bestItem != null && w <= best + 1.0e-9 && bestMade && !made)) {
                    best = w;
                    bestItem = item;
                    bestMade = made;
                }
            }
        }
        return bestItem;
    }

    /** start 의 재료들을 만드는 과정을 거슬러 올라가면 target 이 나오는가 (= target 으로 target 을 만드는 고리). */
    private static boolean derivesFrom(String target, List<String> start, Map<String, List<String>> a, Map<String, List<String>> b) {
        if (start.isEmpty()) {
            return false;
        }
        java.util.ArrayDeque<String> stack = new java.util.ArrayDeque<>(start);
        Set<String> seen = new HashSet<>();
        int guard = 0;
        while (!stack.isEmpty() && guard++ < 20000) {
            String it = stack.pop();
            if (it.equals(target)) {
                return true;
            }
            if (!seen.add(it)) {
                continue;
            }
            List<String> n = a.get(it);
            if (n != null) {
                stack.addAll(n);
            }
            if (b != null) {
                n = b.get(it);
                if (n != null) {
                    stack.addAll(n);
                }
            }
        }
        return !stack.isEmpty(); // 너무 깊으면 고리로 간주하고 건너뛴다
    }

    /**
     * 원가와 판매 가치를 더 싼 쪽으로 반복해서 갱신한다.
     * 레시피 중에는 돌릴수록 양이 늘어나는 고리가 있다 (예: 돌 1개를 갈면 조약돌 2개, 조약돌을 구우면 다시 돌).
     * 그런 고리를 따라가면 원가가 끝없이 0 으로 내려가므로, "자기 자신을 재료로 거쳐서 만드는" 갱신은 받아들이지 않는다.
     */
    private static void relax(Map<String, Double> base, Map<String, Double> est, List<Recipe> recipes, Env env, Config cfg, Result res,
                              Set<String> frozen) {
        Map<Recipe, Prepared> prepared = new java.util.IdentityHashMap<>();
        for (Recipe r : recipes) {
            prepared.put(r, prepare(r, env));
        }
        for (int pass = 0; pass < 64; pass++) {
            boolean changed = false;
            for (Recipe r : recipes) {
                Prepared prep = prepared.get(r);
                if (prep.outSum <= 0) {
                    continue;
                }
                double extra = cfg.flatExtra.getOrDefault(r.type, 0.0);
                double labor = cfg.labor.getOrDefault(r.type, 0.0);
                double m = cfg.margin(r.type);

                Choice c = choose(r, prep, base, est, res, env, false);
                if (c != null && c.total + extra <= 1.0e-9) {
                    c = null; // 아무것도 들지 않는 레시피(재료가 모두 그대로 돌아오는 등)로는 원가를 0 으로 만들지 않는다
                }
                if (c != null) {
                    double[] share = allocate(prep, c.total + extra, base, est);
                    for (int oi = 0; oi < prep.outs.size(); oi++) {
                        Out o = prep.outs.get(oi);
                        double per = share[oi];
                        if (Double.isNaN(per) || frozen.contains(o.item)) {
                            continue;
                        }
                        Double old = res.cost.get(o.item);
                        if ((old == null || per < old - 1.0e-9) && !derivesFrom(o.item, c.via, res.costDeriv, null)) {
                            res.cost.put(o.item, per);
                            res.costRecipe.put(o.item, r.id);
                            res.costDeriv.put(o.item, c.via);
                            changed = true;
                        }
                    }
                }
                Choice v = choose(r, prep, base, est, res, env, true);
                if (v != null && v.total + extra <= 1.0e-9) {
                    v = null;
                }
                if (v != null) {
                    double[] share = allocate(prep, (v.total + extra) * (1.0 + m) + labor, base, est);
                    for (int oi = 0; oi < prep.outs.size(); oi++) {
                        Out o = prep.outs.get(oi);
                        double per = share[oi];
                        if (Double.isNaN(per) || frozen.contains(o.item)) {
                            continue;
                        }
                        Double old = res.value.get(o.item);
                        if ((old == null || per < old - 1.0e-9) && !derivesFrom(o.item, v.via, res.costDeriv, res.valueDeriv)) {
                            res.value.put(o.item, per);
                            res.valueRecipe.put(o.item, r.id);
                            res.valueDeriv.put(o.item, v.via);
                            if (m > 0 || labor > 0) {
                                res.processed.add(o.item);
                            } else {
                                res.processed.remove(o.item);
                            }
                            changed = true;
                        }
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
    }

    /** 가격 계산에 실제로 쓰인 재료 한 가지. */
    public static final class Pick {
        public final String item;
        public final double count;

        public Pick(String item, double count) {
            this.item = item;
            this.count = count;
        }
    }

    /**
     * 이 레시피의 값을 매길 때 고른(가장 싼) 재료 목록. 같은 재료는 합친다. 풀 수 없는 레시피면 빈 목록.
     * 상점 화면에서 "이 음식의 매입가는 어떤 재료 기준인가"를 보여 주는 데 쓴다.
     */
    public static List<Pick> picks(Recipe r, Result res, Env env, boolean forValue) {
        Prepared prep = prepare(r, env);
        boolean grid = r.type.startsWith("minecraft:crafting");
        Map<String, Double> sum = new LinkedHashMap<>();
        for (int i = 0; i < r.ingredients.size(); i++) {
            if (prep.returned[i]) {
                continue;
            }
            Ing ing = r.ingredients.get(i);
            String bestItem = cheapest(ing, prep, res.known, res.estimated, res, env, forValue, grid);
            if (bestItem == null) {
                return new ArrayList<>();
            }
            sum.merge(bestItem, ing.count, Double::sum);
        }
        List<Pick> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : sum.entrySet()) {
            out.add(new Pick(e.getKey(), e.getValue()));
        }
        return out;
    }


    // =====================================================================================
    // "상점에서 재료를 사서 만들 때" 원가
    // =====================================================================================

    /** 이 레시피 결과물 중 해당 아이템의 개수 (되돌아오는 재료 제외). */
    private static double count(Prepared p, String item) {
        double n = 0;
        for (Out o : p.outs) {
            if (o.item.equals(item)) {
                n += o.count;
            }
        }
        return n;
    }

    /** 레시피 1회의 재료를 모두 pc(구입 원가) 기준으로 마련하는 비용. 한 칸이라도 마련할 수 없으면 INF. */
    /** 조합 후 돌려받는 그릇(빈 양동이 등)을 되팔아 돌려받는 값. purchaseCosts/purchaseRequirement 전에 정해 둔다. */
    private static Map<String, Double> refund = Map.of();

    public static synchronized void setRefunds(Map<String, Double> values) {
        refund = values == null ? Map.of() : values;
    }

    /** 재료 한 개를 사서 쓰는 순비용: 판매가 - 돌려받는 그릇의 되팔기 값. */
    private static double netPurchase(String item, double price, Env env) {
        String rem = env.remainder(item);
        if (rem == null || rem.equals(item)) {
            return price;
        }
        return Math.max(0, price - refund.getOrDefault(rem, 0.0));
    }

    private static double purchaseTotal(Recipe r, Prepared p, Map<String, Double> pc, Env env, Config cfg, List<Pick> picksOut) {
        double total = cfg.flatExtra.getOrDefault(r.type, 0.0);
        Map<String, Double> sum = picksOut == null ? null : new LinkedHashMap<>();
        for (int i = 0; i < r.ingredients.size(); i++) {
            if (p.returned[i]) {
                continue;
            }
            Ing ing = r.ingredients.get(i);
            double best = INF;
            String bestItem = null;
            for (String alt : ing.alts) {
                Collection<String> items = alt.startsWith("#") ? env.tagItems(alt.substring(1)) : List.of(alt);
                for (String item : items) {
                    if (count(p, item) > 0) {
                        continue; // 결과물 자신은 재료로 고르지 않는다
                    }
                    Double v = pc.get(item);
                    if (v != null) {
                        // 3.65.0: 돌려받는 그릇이 바로 이 조합법의 결과물이면(물 양동이 → 양동이) 그릇 값을 두 번 셈하지 않는다
                        String rem = env.remainder(item);
                        double net = rem != null && !rem.equals(item) && count(p, rem) > 0 ? v : netPurchase(item, v, env);
                        if (net < best) {
                            best = net;
                            bestItem = item;
                        }
                    }
                }
            }
            if (bestItem == null) {
                return INF;
            }
            total += best * ing.count;
            if (sum != null) {
                sum.merge(bestItem, ing.count, Double::sum);
            }
        }
        if (picksOut != null) {
            for (Map.Entry<String, Double> e : sum.entrySet()) {
                picksOut.add(new Pick(e.getKey(), e.getValue()));
            }
        }
        return total;
    }

    /**
     * 상점에서 살 수 있는 것(supply: 아이템 -> 상점 판매가)만으로 각 아이템을 마련하는 비용.
     * 상점에서 파는 아이템은 그 판매가 그대로(사서 쓴다고 본다), 팔지 않는 아이템은 파는 재료로 만드는 가장 싼 값.
     * 재료가 되돌아 늘어나는 고리(돌 -> 조약돌 2개 -> 돌)로 값이 끝없이 내려가지 않도록,
     * 한 번 값이 정해진 아이템은 다시 낮추지 않는다 (더 비싸게 잡히는 쪽이라 요리 적자에는 안전).
     */
    public static Map<String, Double> purchaseCosts(Map<String, Double> supply, List<Recipe> recipes, Env env, Config cfg) {
        Map<String, Double> pc = new HashMap<>(supply);
        Map<Recipe, Prepared> prepared = new java.util.IdentityHashMap<>();
        for (Recipe r : recipes) {
            prepared.put(r, prepare(r, env));
        }
        for (int layer = 0; layer < 64; layer++) {
            Map<String, Double> found = new HashMap<>();
            for (Recipe r : recipes) {
                Prepared p = prepared.get(r);
                if (p.outSum <= 0) {
                    continue;
                }
                boolean anyNew = false;
                for (Out o : p.outs) {
                    if (!pc.containsKey(o.item)) {
                        anyNew = true;
                        break;
                    }
                }
                if (!anyNew) {
                    continue;
                }
                double total = purchaseTotal(r, p, pc, env, cfg, null);
                if (total >= INF) {
                    continue;
                }
                for (Out o : p.outs) {
                    if (pc.containsKey(o.item)) {
                        continue;
                    }
                    // 결과물이 여러 가지여도 그 아이템 하나가 재료값을 다 떠안는 것으로 친다 (비싸게 잡는 쪽 = 적자에 안전)
                    double per = total / count(p, o.item);
                    found.merge(o.item, per, Math::min);
                }
            }
            if (found.isEmpty()) {
                break;
            }
            pc.putAll(found);
        }
        return pc;
    }

    /**
     * 이 레시피로 item 을 만들어 팔 때 최소한 받아야 하는 1개당 매입가:
     * (재료 구입비 + flatExtra) × (1 + margin) + labor 를 생산량으로 나눈 값. 재료를 살 수 없으면 -1.
     */
    public static double purchaseRequirement(Recipe r, String item, Map<String, Double> pc, Env env, Config cfg, List<Pick> picksOut) {
        Prepared p = prepare(r, env);
        double n = count(p, item);
        if (n <= 0) {
            return -1;
        }
        double total = purchaseTotal(r, p, pc, env, cfg, picksOut);
        if (total >= INF) {
            return -1;
        }
        return (total * (1.0 + cfg.margin(r.type)) + cfg.labor.getOrDefault(r.type, 0.0)) / n;
    }


    /** 이 레시피 1회로 나오는 해당 아이템의 개수 (되돌아오는 재료 제외). */
    public static double outputCount(Recipe r, String item, Env env) {
        double n = 0;
        for (Out o : prepare(r, env).outs) {
            if (o.item.equals(item)) {
                n += o.count;
            }
        }
        return n;
    }

    /**
     * 레시피 1회분의 비용을 결과물들에 나눈다 (1개당 값, NaN = 이 결과물에는 반영하지 않음).
     * 결과물이 여러 종류면: 값이 이미 정해진 것(뼛가루 같은 부산물)은 그 값만큼만 가져가고, 나머지를 값이 없는 결과물이 나눈다.
     * 예) 닭고기 50G -> 닭고기 조각 2개 + 뼛가루 1개(1G) 이면 조각 1개 = (50 - 1) / 2.
     * 결과물 값의 합은 항상 들어간 비용과 같으므로, 나누는 방식 때문에 가치가 새로 생기지는 않는다.
     */
    private static double[] allocate(Prepared prep, double total, Map<String, Double> base, Map<String, Double> est) {
        int n = prep.outs.size();
        double[] per = new double[n];
        if (n == 1) {
            per[0] = total / prep.outs.get(0).count;
            return per;
        }
        Double[] priced = new Double[n];
        double knownSum = 0;
        double unknownCount = 0;
        for (int i = 0; i < n; i++) {
            Out o = prep.outs.get(i);
            Double b = base.get(o.item);
            if (b == null) {
                b = est.get(o.item);
            }
            priced[i] = b;
            if (b != null) {
                knownSum += b * o.count;
            } else {
                unknownCount += o.count;
            }
        }
        if (unknownCount > 0) {
            double rest = Math.max(0, total - knownSum) / unknownCount;
            for (int i = 0; i < n; i++) {
                per[i] = priced[i] == null ? rest : Double.NaN;
            }
        } else if (knownSum > 0) {
            for (int i = 0; i < n; i++) {
                per[i] = total * priced[i] / knownSum;
            }
        } else {
            for (int i = 0; i < n; i++) {
                per[i] = total / prep.outSum;
            }
        }
        return per;
    }

    private static double ingredientSum(Recipe r, Map<String, Double> base, Map<String, Double> est, Result res, Env env, boolean forValue) {
        double total = 0;
        boolean grid = r.type.startsWith("minecraft:crafting");
        for (Ing ing : r.ingredients) {
            double best = bestAlt(ing, base, est, res, env, forValue, grid);
            if (best >= INF) {
                return INF;
            }
            total += best * ing.count;
        }
        return total;
    }

    private static double bestAlt(Ing ing, Map<String, Double> base, Map<String, Double> est, Result res, Env env, boolean forValue,
                                  boolean grid) {
        double best = INF;
        for (String alt : ing.alts) {
            if (alt.startsWith("#")) {
                for (String item : env.tagItems(alt.substring(1))) {
                    best = Math.min(best, net(item, base, est, res, env, forValue, grid));
                }
            } else {
                best = Math.min(best, net(alt, base, est, res, env, forValue, grid));
            }
        }
        return best;
    }

    /** 재료 한 개의 실질 비용: 조합 후 돌려받는 아이템(양동이 등)의 값은 뺀다. */
    private static double net(String item, Map<String, Double> base, Map<String, Double> est, Result res, Env env, boolean forValue,
                              boolean grid) {
        String rem = env.remainder(item);
        boolean self = rem != null && rem.equals(item);
        if (self && grid) {
            return 0; // 조합대에서는 소모되지 않는 도구(칼 등)
        }
        double w = worth(item, base, est, res, forValue);
        if (w >= INF) {
            return INF;
        }
        if (rem != null && !self) {
            double rw = worth(rem, base, est, res, false);
            if (rw < INF) {
                w = Math.max(0, w - rw);
            }
        }
        return w;
    }

    /**
     * 재료로 쓸 때의 가치.
     * 원가 계산: 상점에서 사는 값과 직접 만드는 원가 중 싼 쪽.
     * 판매 가치 계산: 조리 가공품(마진이 붙은 것)은 그 가치를 그대로 물려준다.
     */
    private static double worth(String item, Map<String, Double> base, Map<String, Double> est, Result res, boolean forValue) {
        Double b = base.get(item);
        if (b == null) {
            b = est.get(item);
        }
        Double c = res.cost.get(item);
        double cheapest = Math.min(b != null ? b : INF, c != null ? c : INF);
        if (forValue && res.processed.contains(item)) {
            Double v = res.value.get(item);
            if (v != null) {
                return v; // 가공 음식의 상점 구매가는 어차피 이 가치의 2배로 다시 정해진다
            }
        }
        return cheapest;
    }
}
