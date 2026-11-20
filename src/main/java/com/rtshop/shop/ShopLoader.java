package com.rtshop.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.farm.FarmConfig;
import com.rtshop.farm.SeedRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * config/rtshop/shops/*.json 을 읽어 상점들을 만든다. (파일 이름 = 상점 ID)
 *
 * 가격은 전 상점 공통이다: 같은 아이템은 어느 상점에서나 같은 값이므로 상점 사이의 차익 거래가 없다.
 *  1) "items": 가격을 직접 적은 품목
 *  2) "auto":  태그·모드·종류로 아이템을 모으고 공식으로 가격을 매기는 규칙
 *  3) 레시피 원가 계산: 만들 수 있는 음식은 "재료를 사서 만드는 비용 + 수고비"로 매입한다.
 *
 * items 항목: item, price, category, nbt, seed(심기 전용으로 판매), buy/sell(false 면 그 상점에서 안 팔거나 안 삼),
 *            sellPrice(매입가를 직접 지정), looseSell, fish, dynamic(레시피가 있으면 원가로 가격 결정)
 * auto 항목 : match{mod(s), tag(s), notTag(s), class(es), recipeType(s)}, category, formula(fixed|gear|food|auto),
 *            price, multiplier, rarityScale, sellPrice, buy, sell, looseSell, fish, seed, dynamic
 */
public final class ShopLoader {
    private ShopLoader() {
    }

    public static final String[] DEFAULTS = {"blacksmith", "farmer", "fisher", "food", "general", "land"};

    private static int versionCounter = 0;

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("shops");
    }

    /** 전 상점 공통 가격 정보. */
    public static final class Price {
        public ItemStack stack;
        public int price;
        public double sellUnit = -1;
        public boolean dynamicFood;
        public boolean fixedSell;
        public boolean buyableSomewhere;
        public String basis = "";
        public double cost = -1;
    }

    private static final class Ref {
        String key;
        String category;
        boolean buy;
        boolean sell;
        Boolean loose;
        boolean fish;
    }

    private static final class Raw {
        String id;
        JsonObject json;
        final List<ShopData.Category> categories = new ArrayList<>();
        final List<Ref> refs = new ArrayList<>();
        final Set<String> keys = new HashSet<>();
        final Set<String> excludeIds = new HashSet<>();
        final Set<String> excludeNamespaces = new HashSet<>();
    }

    public static final class Loaded {
        public final Map<String, ShopData> shops = new LinkedHashMap<>();
        public final Map<String, Price> prices = new LinkedHashMap<>();
        public final List<String> notes = new ArrayList<>();
        public RecipeSolver.Result solved;
    }

    public static synchronized Loaded loadAll(MinecraftServer server) {
        Loaded out = new Loaded();
        List<Raw> raws = new ArrayList<>();
        try {
            Path dir = dir();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                for (String name : DEFAULTS) {
                    try (InputStream in = ShopLoader.class.getResourceAsStream("/rtshop_defaults/" + name + ".json")) {
                        if (in != null) {
                            Files.write(dir.resolve(name + ".json"), in.readAllBytes());
                        }
                    }
                }
                RtShop.LOGGER.info("[rtshop] 기본 상점 설정을 생성했습니다: {}", dir);
            }
            List<Path> files = new ArrayList<>();
            try (Stream<Path> st = Files.list(dir)) {
                st.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(files::add);
            }
            files.sort(Comparator.comparing(p -> p.getFileName().toString()));
            for (Path f : files) {
                String name = f.getFileName().toString();
                String id = name.substring(0, name.length() - 5).toLowerCase(Locale.ROOT);
                if (!id.matches("[a-z0-9_]+")) {
                    RtShop.LOGGER.warn("[rtshop] 상점 파일 이름은 영문 소문자/숫자/_ 만 가능합니다. 건너뜀: {}", name);
                    continue;
                }
                try {
                    Raw r = new Raw();
                    r.id = id;
                    r.json = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                    raws.add(r);
                } catch (Exception ex) {
                    RtShop.LOGGER.error("[rtshop] {} 을(를) 읽지 못했습니다(JSON 문법 오류일 수 있음). 이 상점은 건너뜁니다.", name, ex);
                    out.notes.add("상점 파일 오류: " + name + " - " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] 상점 폴더를 읽는 중 오류", ex);
        }

        versionCounter++;
        PricingConfig pc = PricingConfig.get();
        Map<String, Price> prices = out.prices;

        // 레시피를 먼저 모은다: "이 조리법으로 만들어지는 아이템" 규칙과 원가 계산에 쓴다
        RecipeScan.Scan scan = null;
        Map<String, Set<String>> outputTypes = new HashMap<>();
        if (pc.recipePricing && server != null) {
            try {
                scan = RecipeScan.collect(server, pc);
                for (RecipeSolver.Recipe rc : scan.recipes) {
                    for (RecipeSolver.Out o : rc.outputs) {
                        outputTypes.computeIfAbsent(o.item, k -> new HashSet<>()).add(rc.type);
                    }
                }
            } catch (Throwable t) {
                RtShop.LOGGER.error("[rtshop] 레시피 수집 중 오류", t);
            }
        }

        // 1단계: 직접 적은 품목 (모든 상점)
        for (Raw r : raws) {
            parseHeader(r);
            parseExplicit(r, prices, out.notes);
        }
        // 2단계: 자동 규칙
        List<Item> allItems = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        allItems.sort(Comparator.comparing(i -> String.valueOf(ForgeRegistries.ITEMS.getKey(i))));
        for (Raw r : raws) {
            parseAuto(r, prices, allItems, outputTypes);
        }
        for (Raw r : raws) {
            for (Ref ref : r.refs) {
                if (ref.buy) {
                    prices.get(ref.key).buyableSomewhere = true;
                }
            }
        }

        // 3단계: 레시피 원가 계산
        if (scan != null) {
            try {
                applyRecipePricing(scan, pc, out);
            } catch (Throwable t) {
                RtShop.LOGGER.error("[rtshop] 레시피 원가 계산 중 오류 (상점 파일의 가격만 사용합니다)", t);
                out.notes.add("레시피 원가 계산 실패: " + t);
            }
        }
        for (Price p : prices.values()) {
            if (p.sellUnit < 0) {
                p.sellUnit = p.price * pc.sellRatio;
            }
        }

        FarmConfig fc = FarmConfig.get();
        for (Raw r : raws) {
            List<ShopData.Entry> entries = new ArrayList<>();
            for (Ref ref : r.refs) {
                Price p = prices.get(ref.key);
                boolean loose = ref.loose != null ? ref.loose : defaultLoose(p.stack);
                entries.add(new ShopData.Entry(p.stack, p.price, p.sellUnit, ref.category, loose, ref.buy, ref.sell, ref.fish));
            }
            JsonObject o = r.json;
            out.shops.put(r.id, new ShopData(r.id, versionCounter, str(o, "title", r.id), str(o, "npcName", r.id),
                    str(o, "profession", ""), r.categories, entries,
                    fc.starMultipliers.clone(), pc.fishBaseMultiplier, pc.fishWeightBonus, pc.fishGoldenMultiplier,
                    pc.fishNoInfoMultiplier));
            RtShop.LOGGER.info("[rtshop] 상점 '{}' 로드: 품목 {}개", r.id, entries.size());
        }
        return out;
    }

    // ------------------------------------------------------------------ 레시피 원가

    private static void applyRecipePricing(RecipeScan.Scan scan, PricingConfig pc, Loaded out) {
        Map<String, Price> prices = out.prices;
        // 기준 가격: 상점에서 실제로 살 수 있는 고정 가격 품목만
        Map<String, Double> base = new HashMap<>();
        Map<String, Double> hints = new HashMap<>();
        for (Price p : prices.values()) {
            String id = itemId(p.stack);
            if (p.buyableSomewhere && !p.dynamicFood) {
                base.merge(id, (double) p.price, Math::min);
            } else if (!p.stack.hasTag()) {
                // 상점에서 살 수 없는 재료의 값: 매입가를 직접 정한 품목은 그 매입가(직접 길러서 쓰는 재료의 기회비용)
                hints.put(id, p.fixedSell && !p.buyableSomewhere ? Math.max(0.01, p.sellUnit) : (double) p.price);
            }
        }
        RecipeScan.solve(scan, base, hints, pc, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != Items.AIR) {
                int f = Pricing.food(new ItemStack(item));
                if (f > 0) {
                    return f;
                }
            }
            return pc.defaultIngredientValue;
        });
        RecipeSolver.Result res = scan.result;
        out.solved = res;
        int adjusted = 0;
        for (Map.Entry<String, Price> e : prices.entrySet()) {
            Price p = e.getValue();
            if (p.stack.hasTag() || p.fixedSell) {
                continue; // NBT 가 붙은 품목(상점 씨앗 등)과 매입가를 직접 적은 품목은 그대로 둔다
            }
            String id = itemId(p.stack);
            Double c = res.cost.get(id);
            Double v = res.value.get(id);
            if (c != null) {
                p.cost = c;
            }
            boolean food = p.stack.isEdible() || res.processed.contains(id);
            if (p.dynamicFood) {
                if (v != null) {
                    p.sellUnit = v;
                    p.price = (int) Math.max(1, Math.ceil(v * pc.foodBuyMultiplier - 1.0e-9));
                    p.basis = String.valueOf(res.valueRecipe.get(id));
                    adjusted++;
                }
            } else if (food && v != null) {
                // 만들 수 있는 음식은 상점 파일의 가격 대신 "재료 원가 + 수고비"로 매입한다.
                // 더 낮으면 재료를 사서 만들어 팔 때 적자가 나고, 더 높으면 만들어 되파는 것만으로 돈이 불어난다.
                p.sellUnit = v;
                p.price = (int) Math.max(1, Math.ceil(v * pc.foodBuyMultiplier - 1.0e-9));
                p.basis = String.valueOf(res.valueRecipe.get(id));
                adjusted++;
            } else if (c != null && c < p.price * pc.sellRatio) {
                // 음식이 아닌 조합품은 원가보다 비싸게 매입하지 않는다 (조합해서 되파는 무한 돈벌이 방지)
                p.sellUnit = c;
                p.basis = String.valueOf(res.costRecipe.get(id));
            }
        }
        out.notes.add(String.format("레시피 파일 %d개 중 %d개 해석, 계산에 쓰지 못한 레시피 %d개, 가격을 추정한 재료 %d종, 원가 기준으로 가격을 정한 음식 %d종",
                scan.files, scan.parsed, res.recipesUnresolved, res.estimated.size(), adjusted));
        RtShop.LOGGER.info("[rtshop] {}", out.notes.get(out.notes.size() - 1));
    }

    // ------------------------------------------------------------------ 도우미

    private static Item findItem(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            return null;
        }
        return item;
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsBoolean() : def;
    }

    private static List<String> strings(JsonObject o, String single, String plural) {
        List<String> out = new ArrayList<>();
        if (o.has(single) && o.get(single).isJsonPrimitive()) {
            out.add(o.get(single).getAsString());
        }
        if (o.has(plural) && o.get(plural).isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray(plural)) {
                out.add(e.getAsString());
            }
        }
        return out;
    }

    public static String itemId(ItemStack s) {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));
    }

    public static String key(ItemStack s) {
        return itemId(s) + (s.hasTag() ? s.getTag().toString() : "");
    }

    /** 판매할 때 NBT 를 무시해도 되는 품목인가 (작물·물고기·음식처럼 별 등급이나 무게가 붙는 것). */
    private static boolean defaultLoose(ItemStack s) {
        if (s.hasTag() || s.isDamageableItem()) {
            return false;
        }
        try {
            return s.isEdible() || s.is(Tags.Items.CROPS) || s.is(ItemTags.FISHES);
        } catch (Throwable t) {
            return s.isEdible();
        }
    }

    private static void parseHeader(Raw r) {
        JsonObject root = r.json;
        if (root.has("categories")) {
            for (JsonElement el : root.getAsJsonArray("categories")) {
                JsonObject o = el.getAsJsonObject();
                String id = str(o, "id", "");
                if (id.isEmpty()) {
                    continue;
                }
                Item icon = findItem(str(o, "icon", "minecraft:chest"));
                r.categories.add(new ShopData.Category(id, str(o, "name", id),
                        new ItemStack(icon == null ? Items.CHEST : icon)));
            }
        }
        if (root.has("exclude")) {
            for (JsonElement el : root.getAsJsonArray("exclude")) {
                String s = el.getAsString();
                if (s.endsWith(":*")) {
                    r.excludeNamespaces.add(s.substring(0, s.length() - 2));
                } else {
                    r.excludeIds.add(s);
                }
            }
        }
    }

    private static ItemStack prepare(ItemStack stack, boolean seedFlag) {
        if (seedFlag || (FarmConfig.get().requireShopSeeds && SeedRules.isPureSeed(stack))) {
            return SeedRules.license(stack);
        }
        return stack;
    }

    private static void parseExplicit(Raw r, Map<String, Price> prices, List<String> notes) {
        if (!r.json.has("items")) {
            return;
        }
        JsonArray arr = r.json.getAsJsonArray("items");
        for (JsonElement el : arr) {
            try {
                JsonObject o = el.getAsJsonObject();
                String id = str(o, "item", "");
                Item item = findItem(id);
                if (item == null) {
                    RtShop.LOGGER.warn("[rtshop] ({}) 알 수 없는 아이템이라 건너뜁니다: {}", r.id, id);
                    continue;
                }
                ItemStack stack = new ItemStack(item);
                if (o.has("nbt") && !o.get("nbt").isJsonNull()) {
                    CompoundTag tag = TagParser.parseTag(o.get("nbt").getAsString());
                    stack.setTag(tag);
                }
                stack = prepare(stack, bool(o, "seed", false));
                String key = key(stack);
                if (!r.keys.add(key)) {
                    continue; // 같은 상점에 두 번 적힌 품목
                }
                int price = Math.max(1, o.has("price") ? o.get("price").getAsInt() : 1);
                Price p = prices.get(key);
                if (p == null) {
                    p = new Price();
                    p.stack = stack;
                    p.price = price;
                    if (o.has("sellPrice") && !o.get("sellPrice").isJsonNull()) {
                        p.sellUnit = Math.max(0, o.get("sellPrice").getAsDouble());
                        p.fixedSell = true;
                    }
                    p.dynamicFood = bool(o, "dynamic", false) && !stack.hasTag() && !p.fixedSell;
                    prices.put(key, p);
                } else if (o.has("price") && p.price != price) {
                    notes.add(String.format("%s: 상점 '%s'의 가격 %d 대신 먼저 정해진 가격 %d 을(를) 씁니다 (가격은 전 상점 공통)", id, r.id, price, p.price));
                }
                Ref ref = new Ref();
                ref.key = key;
                ref.category = str(o, "category", "");
                ref.buy = bool(o, "buy", true);
                ref.sell = bool(o, "sell", true);
                ref.loose = o.has("looseSell") ? o.get("looseSell").getAsBoolean() : null;
                ref.fish = bool(o, "fish", false);
                r.refs.add(ref);
            } catch (Exception ex) {
                RtShop.LOGGER.warn("[rtshop] ({}) 잘못된 아이템 항목을 건너뜁니다: {} ({})", r.id, el, ex.toString());
            }
        }
    }

    private static void parseAuto(Raw r, Map<String, Price> prices, List<Item> allItems, Map<String, Set<String>> outputTypes) {
        if (!r.json.has("auto")) {
            return;
        }
        for (JsonElement el : r.json.getAsJsonArray("auto")) {
            try {
                JsonObject rule = el.getAsJsonObject();
                JsonObject match = rule.has("match") ? rule.getAsJsonObject("match") : new JsonObject();
                List<String> mods = strings(match, "mod", "mods");
                List<String> classes = strings(match, "class", "classes");
                List<TagKey<Item>> tags = new ArrayList<>();
                for (String t : strings(match, "tag", "tags")) {
                    ResourceLocation rl = ResourceLocation.tryParse(t);
                    if (rl != null) {
                        tags.add(ItemTags.create(rl));
                    }
                }
                List<TagKey<Item>> notTags = new ArrayList<>();
                for (String t : strings(match, "notTag", "notTags")) {
                    ResourceLocation rl = ResourceLocation.tryParse(t);
                    if (rl != null) {
                        notTags.add(ItemTags.create(rl));
                    }
                }
                List<String> recipeTypes = strings(match, "recipeType", "recipeTypes");
                if (mods.isEmpty() && classes.isEmpty() && tags.isEmpty() && recipeTypes.isEmpty()) {
                    continue; // 조건 없는 규칙은 모든 아이템을 쓸어 담으므로 무시
                }
                boolean forceDynamic = bool(rule, "dynamic", false);
                String category = str(rule, "category", "");
                String formula = str(rule, "formula", "fixed");
                int fixed = rule.has("price") ? rule.get("price").getAsInt() : -1;
                double mult = rule.has("multiplier") ? rule.get("multiplier").getAsDouble() : 1.0;
                boolean rarityScale = bool(rule, "rarityScale", false);
                Boolean loose = rule.has("looseSell") ? rule.get("looseSell").getAsBoolean() : null;
                boolean buy = bool(rule, "buy", true);
                boolean sell = bool(rule, "sell", true);
                boolean fish = bool(rule, "fish", false);
                boolean seed = bool(rule, "seed", false);
                double ruleSell = rule.has("sellPrice") && !rule.get("sellPrice").isJsonNull() ? rule.get("sellPrice").getAsDouble() : -1;
                int count = 0;

                for (Item item : allItems) {
                    if (item == Items.AIR) {
                        continue;
                    }
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                    if (rl == null || rl.getNamespace().equals(RtShop.MODID)
                            || r.excludeNamespaces.contains(rl.getNamespace()) || r.excludeIds.contains(rl.toString())) {
                        continue;
                    }
                    if (!mods.isEmpty() && !mods.contains(rl.getNamespace())) {
                        continue;
                    }
                    if (!recipeTypes.isEmpty()) {
                        Set<String> made = outputTypes.get(rl.toString());
                        boolean any = false;
                        if (made != null) {
                            for (String t : recipeTypes) {
                                if (made.contains(t)) {
                                    any = true;
                                    break;
                                }
                            }
                        }
                        if (!any) {
                            continue;
                        }
                    }
                    ItemStack plain;
                    int price;
                    boolean dynamic;
                    try {
                        plain = new ItemStack(item);
                        if (plain.isEmpty() || !matches(plain, tags, notTags, classes)) {
                            continue;
                        }
                        price = computePrice(plain, formula, fixed, rarityScale);
                        dynamic = forceDynamic || ((formula.equals("food") || formula.equals("auto")) && plain.isEdible());
                    } catch (Throwable t) {
                        continue; // 다른 모드 아이템이 예외를 던져도 상점 로딩이 멈추지 않게 함
                    }
                    if (price <= 0) {
                        continue;
                    }
                    ItemStack stack = prepare(plain, seed);
                    String key = key(stack);
                    if (!r.keys.add(key)) {
                        continue;
                    }
                    Price p = prices.get(key);
                    if (p == null) {
                        p = new Price();
                        p.stack = stack;
                        p.price = Pricing.nice(price * mult);
                        if (ruleSell >= 0) {
                            p.sellUnit = ruleSell;
                            p.fixedSell = true;
                        }
                        p.dynamicFood = dynamic && !stack.hasTag() && !p.fixedSell;
                        prices.put(key, p);
                    }
                    Ref ref = new Ref();
                    ref.key = key;
                    ref.category = category;
                    ref.buy = buy;
                    ref.sell = sell;
                    ref.loose = loose;
                    ref.fish = fish;
                    r.refs.add(ref);
                    count++;
                }
                RtShop.LOGGER.info("[rtshop] ({}) 자동 규칙 {} -> {}개", r.id, match, count);
            } catch (Exception ex) {
                RtShop.LOGGER.warn("[rtshop] ({}) 잘못된 auto 규칙을 건너뜁니다: {} ({})", r.id, el, ex.toString());
            }
        }
    }

    private static boolean matches(ItemStack stack, List<TagKey<Item>> tags, List<TagKey<Item>> notTags, List<String> classes) {
        if (!tags.isEmpty()) {
            boolean any = false;
            for (TagKey<Item> t : tags) {
                if (stack.is(t)) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        for (TagKey<Item> t : notTags) {
            if (stack.is(t)) {
                return false;
            }
        }
        if (!classes.isEmpty()) {
            boolean any = false;
            for (String c : classes) {
                if (Pricing.isClass(stack, c)) {
                    any = true;
                    break;
                }
            }
            return any;
        }
        return true;
    }

    private static int computePrice(ItemStack stack, String formula, int fixed, boolean rarityScale) {
        int p;
        switch (formula) {
            case "gear":
                p = Pricing.gear(stack);
                break;
            case "food":
                p = Pricing.food(stack);
                if (p <= 0) {
                    p = fixed;
                }
                return p;
            case "auto":
                p = Pricing.gear(stack);
                if (p <= 0 && stack.getItem().isEdible()) {
                    p = Math.max(Pricing.food(stack), fixed);
                }
                if (p <= 0 && fixed > 0) {
                    p = Pricing.nice(fixed * Pricing.rarityFactor(stack));
                }
                return p;
            default:
                p = fixed;
                break;
        }
        if (p <= 0) {
            p = fixed;
        }
        if (p > 0 && rarityScale && "fixed".equals(formula)) {
            p = Pricing.nice(p * Pricing.rarityFactor(stack));
        }
        return p;
    }

    /** 현재 적용된 가격표를 config/rtshop/export/ 에 내보낸다. */
    public static int export(Loaded loaded) throws Exception {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("export");
        Files.createDirectories(dir);
        int total = 0;
        for (ShopData d : loaded.shops.values()) {
            StringBuilder sb = new StringBuilder();
            sb.append("# 상점: ").append(d.title).append(" (").append(d.id).append(")\n");
            sb.append("# 아이템 | 구매가 | 기본 매입가 | 구분 | 원가 | 가격 근거(레시피)\n");
            for (ShopData.Entry e : d.entries) {
                Price p = loaded.prices.get(key(e.stack));
                sb.append(String.format(Locale.ROOT, "%s%s | %s | %s | %s%s | %s | %s%n",
                        itemId(e.stack), e.stack.hasTag() ? " " + e.stack.getTag() : "",
                        e.buyable ? Integer.toString(e.price) : "-",
                        e.sellable ? String.format(Locale.ROOT, "%.2f", e.sellUnit) : "-",
                        e.category, e.looseSell ? " (품질 반영)" : "",
                        p != null && p.cost >= 0 ? String.format(Locale.ROOT, "%.2f", p.cost) : "-",
                        p != null ? p.basis : ""));
                total++;
            }
            Files.writeString(dir.resolve(d.id + ".txt"), sb.toString(), StandardCharsets.UTF_8);
        }
        // 점검 보고서: 음식의 매입가가 재료 원가보다 낮은 경우가 있는지
        StringBuilder audit = new StringBuilder("# RT Shop 가격 점검\n");
        for (String n : loaded.notes) {
            audit.append("- ").append(n).append('\n');
        }
        int deficits = 0;
        for (Price p : loaded.prices.values()) {
            if (p.cost >= 0 && !p.stack.hasTag() && (p.stack.isEdible() || p.dynamicFood) && p.sellUnit + 1.0e-6 < p.cost) {
                audit.append(String.format(Locale.ROOT, "적자 가능: %s 매입가 %.2f < 원가 %.2f (%s)%n", itemId(p.stack), p.sellUnit, p.cost, p.basis));
                deficits++;
            }
        }
        audit.append("적자 가능 음식: ").append(deficits).append("종\n");
        if (loaded.solved != null && !loaded.solved.estimated.isEmpty()) {
            audit.append("\n# 가격표에 없어 추정가를 쓴 재료 (상점 파일에 가격을 적으면 더 정확해집니다)\n");
            loaded.solved.estimated.forEach((k, v) -> audit.append(String.format(Locale.ROOT, "%s = %.0f%n", k, v)));
        }
        Files.writeString(dir.resolve("audit.txt"), audit.toString(), StandardCharsets.UTF_8);
        return total;
    }
}
