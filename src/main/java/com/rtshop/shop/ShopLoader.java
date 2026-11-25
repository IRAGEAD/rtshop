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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
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
 *  3) 레시피 원가 계산: 만들 수 있는 물건은 "재료를 사서 만드는 비용(+조리 수고비)"으로 매입한다.
 *
 * 가격 원칙
 *  - 원재료(만들 수 없는 것)와 원가 이하로 파는 기본 재료: 상점가의 50% 에 매입.
 *  - 만들 수 있고 상점가가 원가보다 비싼 것(완제품): 재료 원가만큼 매입하고, 상점가는 그 2배 이상으로 맞춘다.
 *    -> 재료를 상점에서 사서 만들어 팔면 본전 이상이고, 만들어서 되파는 것만으로 돈이 불어나지도 않는다.
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

    /** 기본 상점 파일의 세대. 가격 체계가 바뀌면 올린다 (이전 세대의 파일은 백업 폴더로 옮기고 새 기본값을 설치). */
    public static final int DEFAULTS_VERSION = 3;

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
        public boolean sellableSomewhere;
        /**
         * 광물 기준가: 상점 파일에 값을 직접 적은 광물(주괴·원석·보석 등). 모드의 광석 가공(분쇄·세척으로 양 불리기, 자갈 세척 등)으로
         * 원가를 낮춰 잡지 않고 상점 값을 그대로 기준으로 쓴다 (그렇지 않으면 철 주괴 매입가가 0 에 가깝게 떨어진다).
         */
        public boolean anchor;
        /** 장비 공식으로 가격이 정해진 품목(무기·방어구·도구). 만들 수 있으면 항상 재료 원가 기준으로 매입한다. */
        public boolean gear;
        public String basis = "";
        public double cost = -1;
        /** 매입가를 계산한 조리법의 재료(가장 싼 것 기준)와 1회 생산량. */
        public List<ShopData.Part> parts = List.of();
        public float partsOut = 1.0F;
        /** 만들 수 있는데 상점가가 원가보다 비싼 품목(완제품)인가. */
        public boolean finished;
        /** 어느 상점이 '재료'(요리·음료가 아닌 구분)로 파는 품목: 사서 쓰는 원재료이므로 매입가 하한을 걸지 않는다. */
        public boolean rawStock;
        /** 매입가가 "재료를 상점에서 사서 만드는 값" 기준으로 정해졌다 (parts 가 이미 채워져 있음). */
        public boolean purchaseBasis;
        /** 재료를 모두 상점에서 사서 만들 때 가장 비싼 조리법의 1개당 필요 매입가 (없으면 -1). */
        public double purchaseNeed = -1;
        /** 상점 재료를 화로에 바로 넣어 만드는 값 (없으면 -1). 요리사 판매 보너스를 줄지 판단하는 데 쓴다. */
        public double autoMin = -1;
        /** 손으로 만드는 요리 (재료를 사서 만들 수 있고, 화로에 바로 구워 만드는 길이 없음): 이익 하한과 요리사 보너스 대상. */
        public boolean handMade;
        /** 요리사 판매 보너스 대상: 손으로 만드는 요리 중 화로에 바로 넣어 만드는 길이 아예 없는 것. */
        public boolean cookable;
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
        public RecipeScan.Scan scan;
        public long loadMillis;
        public long pricingMillis;
    }

    /**
     * 기본 상점 파일 설치. 처음 실행이거나, 이전 버전(가격 체계가 다른)의 파일만 있을 때 실행된다.
     * 이전 파일은 지우지 않고 config/rtshop/backup/ 아래로 옮긴다.
     */
    private static void installDefaults(Path dir, List<String> notes) throws IOException {
        Path root = FMLPaths.CONFIGDIR.get().resolve("rtshop");
        Path marker = root.resolve("defaults_version.txt");
        boolean had = Files.exists(dir);
        int installed = 0;
        if (Files.exists(marker)) {
            try {
                installed = Integer.parseInt(Files.readString(marker, StandardCharsets.UTF_8).trim());
            } catch (Exception ignored) {
            }
        }
        if (had && installed >= DEFAULTS_VERSION) {
            installAdded(dir, root, false);
            return;
        }
        if (had) {
            List<Path> old = new ArrayList<>();
            try (Stream<Path> st = Files.list(dir)) {
                st.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(old::add);
            }
            if (!old.isEmpty()) {
                Path backup = root.resolve("backup").resolve("shops_before_v" + DEFAULTS_VERSION + "_" + System.currentTimeMillis());
                Files.createDirectories(backup);
                for (Path f : old) {
                    Files.move(f, backup.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
                String msg = "이전 버전의 상점 파일 " + old.size() + "개를 " + backup + " 로 옮기고 새 기본 상점을 설치했습니다.";
                notes.add(msg);
                RtShop.LOGGER.warn("[rtshop] {}", msg);
            }
        }
        Files.createDirectories(dir);
        for (String name : DEFAULTS) {
            try (InputStream in = ShopLoader.class.getResourceAsStream("/rtshop_defaults/" + name + ".json")) {
                if (in != null) {
                    Files.write(dir.resolve(name + ".json"), in.readAllBytes());
                }
            }
        }
        Files.writeString(marker, Integer.toString(DEFAULTS_VERSION), StandardCharsets.UTF_8);
        RtShop.LOGGER.info("[rtshop] 기본 상점 설정을 생성했습니다: {}", dir);
        installAdded(dir, root, true); // 기본 상점을 새로 깔았으니 나중에 생긴 상점도 함께
    }

    /** 나중 버전에서 새로 생긴 기본 상점 (기존 상점 파일은 건드리지 않고, 한 번만 설치한다). */
    public static final String[] ADDED = {"attendance_shop", "gacha_shop", "raid"};
    /** 기본 내용이 크게 바뀐 상점: "이름@판" 이 기록에 없으면 기존 파일을 backup 으로 옮기고 새 기본 파일로 바꾼다 (한 번만). */
    public static final String[] UPGRADED = {"gacha_shop@2"};
    /** 기존 상점 파일에 품목을 덧붙임: "상점+조각파일@판" (조각의 categories/items 중 없는 것만 추가, 한 번만). */
    public static final String[] APPENDED = {"blacksmith+blacksmith_enhance@1", "blacksmith+blacksmith_enhance@2", "general+general_pshop@1",
            "gacha_shop+gacha_shop_magic@1", "general+general_return@1"};

    private static void installAdded(Path dir, Path root, boolean force) throws IOException {
        Path list = root.resolve("added_shops.txt");
        List<String> done = Files.exists(list) ? new ArrayList<>(Files.readAllLines(list, StandardCharsets.UTF_8)) : new ArrayList<>();
        boolean changed = false;
        for (String name : ADDED) {
            if (done.contains(name) && !force) {
                continue;
            }
            Path f = dir.resolve(name + ".json");
            if (!Files.exists(f)) {
                try (InputStream in = ShopLoader.class.getResourceAsStream("/rtshop_defaults/" + name + ".json")) {
                    if (in != null) {
                        Files.write(f, in.readAllBytes());
                        RtShop.LOGGER.info("[rtshop] 새 기본 상점 {} 을(를) 설치했습니다.", name);
                    }
                }
            }
            if (!done.contains(name)) {
                done.add(name);
            }
            changed = true;
        }
        for (String mark : UPGRADED) {
            if (done.contains(mark)) {
                continue;
            }
            String name = mark.substring(0, mark.indexOf('@'));
            Path f = dir.resolve(name + ".json");
            if (Files.exists(f) && !force) {
                Path backup = root.resolve("backup");
                Files.createDirectories(backup);
                Files.move(f, backup.resolve(name + "_before_" + mark.substring(mark.indexOf('@') + 1) + "_" + System.currentTimeMillis() + ".json"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            try (InputStream in = ShopLoader.class.getResourceAsStream("/rtshop_defaults/" + name + ".json")) {
                if (in != null) {
                    Files.write(f, in.readAllBytes());
                    RtShop.LOGGER.info("[rtshop] 기본 상점 {} 을(를) 새 판으로 바꿨습니다 (이전 파일은 backup 폴더).", name);
                }
            }
            done.add(mark);
            changed = true;
        }
        for (String mark : APPENDED) {
            if (done.contains(mark) && !force) {
                continue; // 기본 상점을 새로 깔았을 때(force)는 다시 덧붙인다 (이미 있는 품목은 건너뜀)
            }
            String target = mark.substring(0, mark.indexOf('+'));
            String frag = mark.substring(mark.indexOf('+') + 1, mark.indexOf('@'));
            try {
                appendItems(dir.resolve(target + ".json"), frag);
            } catch (Exception ex) {
                RtShop.LOGGER.error("[rtshop] 상점 {} 에 새 품목을 덧붙이지 못했습니다: {}", target, ex.toString());
            }
            if (!done.contains(mark)) {
                done.add(mark);
            }
            changed = true;
        }
        if (changed) {
            Files.write(list, done, StandardCharsets.UTF_8);
        }
    }

    /** 상점 파일에 조각 파일(rtshop_defaults/조각.json)의 카테고리·품목 중 없는 것만 덧붙인다. */
    public static int appendItems(Path file, String frag) throws IOException {
        if (!Files.exists(file)) {
            return 0;
        }
        com.google.gson.JsonObject add;
        try (InputStream in = ShopLoader.class.getResourceAsStream("/rtshop_defaults/" + frag + ".json")) {
            if (in == null) {
                return 0;
            }
            add = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        com.google.gson.JsonObject shop = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        int n = 0;
        for (String key : new String[]{"categories", "items"}) {
            if (!add.has(key)) {
                continue;
            }
            String idKey = key.equals("categories") ? "id" : "item";
            com.google.gson.JsonArray arr = shop.has(key) ? shop.getAsJsonArray(key) : new com.google.gson.JsonArray();
            Set<String> have = new HashSet<>();
            for (com.google.gson.JsonElement e : arr) {
                if (e.isJsonObject() && e.getAsJsonObject().has(idKey)) {
                    have.add(e.getAsJsonObject().get(idKey).getAsString());
                }
            }
            for (com.google.gson.JsonElement e : add.getAsJsonArray(key)) {
                String id = e.getAsJsonObject().get(idKey).getAsString();
                if (have.add(id)) {
                    arr.add(e.deepCopy());
                    n++;
                }
            }
            shop.add(key, arr);
        }
        if (n > 0) {
            Files.writeString(file, new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(shop),
                    StandardCharsets.UTF_8);
            RtShop.LOGGER.info("[rtshop] 상점 {} 에 새 품목 {}개를 덧붙였습니다.", file.getFileName(), n);
        }
        return n;
    }

    public static synchronized Loaded loadAll(MinecraftServer server) {
        long startedAt = System.nanoTime();
        Loaded out = new Loaded();
        List<Raw> raws = new ArrayList<>();
        com.rtshop.rules.ItemPolicy.beginShops();
        try {
            Path dir = dir();
            installDefaults(dir, out.notes);
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
        try {
            addMissingSeeds(raws, prices, allItems, out.notes);
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop] 빠진 씨앗을 상점에 올리는 중 오류", t);
        }
        try {
            addMissingStaffs(raws, prices, allItems, out.notes);
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop] 빠진 지팡이를 상점에 올리는 중 오류", t);
        }
        if (scan != null && pc.autoListFoodIngredients) {
            try {
                addMissingFoodIngredients(raws, prices, scan, pc, out.notes);
            } catch (Throwable t) {
                RtShop.LOGGER.error("[rtshop] 빠진 요리 재료를 식품 상인에 올리는 중 오류", t);
            }
        }
        for (Raw r : raws) {
            for (Ref ref : r.refs) {
                if (ref.buy) {
                    prices.get(ref.key).buyableSomewhere = true;
                    if (!"dish".equals(ref.category) && !"drink".equals(ref.category)) {
                        prices.get(ref.key).rawStock = true;
                    }
                }
                if (ref.sell) {
                    prices.get(ref.key).sellableSomewhere = true;
                }
            }
        }

        // 3단계: 레시피 원가 계산
        long pricingAt = System.nanoTime();
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
        // 아이템 규칙: 묶음별 상점가 배수 (예: Weapons of Miracles x5). 매입가는 그대로 두고 사는 값만 바꾼다.
        for (Price p : prices.values()) {
            double factor = com.rtshop.rules.ItemPolicy.priceFactor(p.stack);
            if (factor != 1) {
                p.price = (int) Math.max(Math.max(1, Math.ceil(p.sellUnit)), Pricing.nice(p.price * factor));
                com.rtshop.rules.ItemPolicy.noteRaised(p.stack);
            }
        }

        FarmConfig fc = FarmConfig.get();
        for (Raw r : raws) {
            List<ShopData.Entry> entries = new ArrayList<>();
            for (Ref ref : r.refs) {
                Price p = prices.get(ref.key);
                boolean loose = ref.loose != null ? ref.loose : defaultLoose(p.stack);
                entries.add(new ShopData.Entry(p.stack, p.price, p.sellUnit, ref.category, loose, ref.buy, ref.sell, ref.fish,
                        p.parts, p.partsOut));
            }
            JsonObject o = r.json;
            out.shops.put(r.id, new ShopData(r.id, versionCounter, str(o, "title", r.id), str(o, "npcName", r.id),
                    str(o, "profession", ""), r.categories, entries,
                    fc.starMultipliers.clone(), pc.fishBaseMultiplier, pc.fishWeightBonus, pc.fishGoldenMultiplier,
                    pc.fishNoInfoMultiplier));
            RtShop.LOGGER.info("[rtshop] 상점 '{}' 로드: 품목 {}개", r.id, entries.size());
        }
        try {
            Restaurant.install(out, server, versionCounter);
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop] 식당 상점을 준비하지 못했습니다", t);
        }
        long now = System.nanoTime();
        out.loadMillis = (now - startedAt) / 1_000_000L;
        out.pricingMillis = (now - pricingAt) / 1_000_000L;
        RtShop.LOGGER.info("[rtshop] 가격표 준비 {} ms (레시피 {}개, 원가 계산 {} ms)", out.loadMillis,
                scan == null ? 0 : scan.recipes.size(), out.pricingMillis);
        return out;
    }

    /**
     * 씨앗은 상점에서만 구할 수 있으므로, 어느 상점도 '심을 수 있는 형태'로 팔지 않는 씨앗이 있으면
     * 그 작물은 아예 키울 수 없게 된다. 가격표에 없는 모드 작물이 그렇게 되지 않도록,
     * 빠진 씨앗과 '작물 겸 씨앗'의 심기 전용 품목을 씨앗을 파는 상점에 자동으로 올린다.
     */
    private static void addMissingSeeds(List<Raw> raws, Map<String, Price> prices, List<Item> allItems, List<String> notes) {
        FarmConfig fc = FarmConfig.get();
        if (!fc.requireShopSeeds || !fc.autoListMissingSeeds) {
            return;
        }
        Raw target = null;
        String category = "";
        int best = 0;
        Set<String> sold = new HashSet<>();
        for (Raw r : raws) {
            Map<String, Integer> cats = new HashMap<>();
            int n = 0;
            for (Ref ref : r.refs) {
                Price p = prices.get(ref.key);
                if (p != null && ref.buy && SeedRules.isLicensed(p.stack)) {
                    n++;
                    sold.add(itemId(p.stack));
                    cats.merge(ref.category, 1, Integer::sum);
                }
            }
            if (n > best) {
                best = n;
                target = r;
                category = cats.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
            }
        }
        if (target == null) {
            return; // 씨앗을 파는 상점이 하나도 없다: 관리자가 일부러 없앤 것으로 본다
        }
        List<String> added = new ArrayList<>();
        for (Item item : allItems) {
            ItemStack plain = new ItemStack(item);
            String id = itemId(plain);
            if (sold.contains(id) || !SeedRules.isPlantable(plain) || com.rtshop.rules.ItemPolicy.shopBanned(plain)) {
                continue;
            }
            int colon = id.indexOf(':');
            if (target.excludeIds.contains(id) || (colon > 0 && target.excludeNamespaces.contains(id.substring(0, colon)))) {
                continue;
            }
            ItemStack stack = SeedRules.license(plain);
            String key = key(stack);
            if (!target.keys.add(key)) {
                continue;
            }
            Price p = prices.get(key);
            if (p == null) {
                p = new Price();
                p.stack = stack;
                int price = fc.autoSeedPrice;
                if (SeedRules.isDual(plain)) {
                    Price crop = prices.get(id);
                    int cropPrice = crop != null ? crop.price : fc.autoSeedPrice;
                    price = (int) Math.ceil(cropPrice * fc.autoDualSeedFactor - 1e-9);
                }
                p.price = Math.max(1, price);
                prices.put(key, p);
            }
            Ref ref = new Ref();
            ref.key = key;
            ref.category = category;
            ref.buy = true;
            ref.sell = true;
            target.refs.add(ref);
            added.add(id + "=" + p.price);
        }
        if (!added.isEmpty()) {
            RtShop.LOGGER.info("[rtshop] 가격표에 없던 씨앗 {}종을 상점 '{}' 에 자동으로 올렸습니다: {}", added.size(), target.id, added);
            notes.add("가격표에 없던 씨앗 " + added.size() + "종을 상점 '" + target.id + "' 에 자동으로 올렸습니다 (farming.json 의 autoListMissingSeeds): "
                    + String.join(", ", added));
        }
    }

    /**
     * 지팡이는 만들 수 없고 상점에서 사야 하므로(아이템 규칙), 어느 상점도 팔지 않는 지팡이를 대장장이에게 올린다.
     * 값은 장비 공식(공격력·내구도)의 5배와 희귀도에 따른 기본값 중 큰 쪽.
     */
    private static void addMissingStaffs(List<Raw> raws, Map<String, Price> prices, List<Item> allItems, List<String> notes) {
        com.rtshop.rules.ItemPolicy.Config ic = com.rtshop.rules.ItemPolicy.get();
        if (!ic.enabled || !ic.recipeBanStaffs || !ic.listStaffs) {
            return;
        }
        Raw target = null;
        Set<String> sold = new HashSet<>();
        for (Raw r : raws) {
            if (r.id.equals(ic.staffShop)) {
                target = r;
            }
            for (Ref ref : r.refs) {
                Price p = prices.get(ref.key);
                if (p != null && ref.buy) {
                    sold.add(itemId(p.stack));
                }
            }
        }
        if (target == null) {
            return;
        }
        List<String> added = new ArrayList<>();
        for (Item item : allItems) {
            if (item == Items.AIR || !com.rtshop.rules.ItemPolicy.staff(item)) {
                continue;
            }
            ItemStack plain = new ItemStack(item);
            String id = itemId(plain);
            int colon = id.indexOf(':');
            if (sold.contains(id) || id.startsWith(RtShop.MODID + ":") || com.rtshop.rules.ItemPolicy.shopBanned(plain)) {
                continue;
            }
            if (target.excludeIds.contains(id) || (colon > 0 && target.excludeNamespaces.contains(id.substring(0, colon)))) {
                continue;
            }
            String key = key(plain);
            if (!target.keys.add(key)) {
                continue;
            }
            Price p = prices.get(key);
            if (p == null) {
                p = new Price();
                p.stack = plain;
                double rare = switch (plain.getRarity()) {
                    case UNCOMMON -> 1.5;
                    case RARE -> 2.5;
                    case EPIC -> 4.0;
                    default -> 1.0;
                };
                int g = Pricing.gear(plain);
                p.price = Math.max(1, Pricing.nice(Math.max(g * 5.0, ic.staffPrice * rare)));
                p.gear = g > 0;
                prices.put(key, p);
            }
            Ref ref = new Ref();
            ref.key = key;
            ref.category = ic.staffCategory;
            ref.buy = true;
            ref.sell = true;
            target.refs.add(ref);
            added.add(id + "=" + p.price);
            com.rtshop.rules.ItemPolicy.noteStaff(id + "=" + p.price);
        }
        if (!added.isEmpty()) {
            RtShop.LOGGER.info("[rtshop] 어느 상점도 팔지 않던 지팡이 {}종을 상점 '{}' 에 올렸습니다: {}", added.size(), target.id, added);
            notes.add("어느 상점도 팔지 않던 지팡이 " + added.size() + "종을 상점 '" + target.id + "' 에 올렸습니다 (item_policy.json 의 listStaffs): "
                    + String.join(", ", added));
        }
    }

    // ------------------------------------------------------------------ 레시피 원가

    private static void applyRecipePricing(RecipeScan.Scan scan, PricingConfig pc, Loaded out) {
        Map<String, Price> prices = out.prices;
        // 기준 가격: 상점에서 실제로 살 수 있는 고정 가격 품목만
        Map<String, Double> base = new HashMap<>();
        Map<String, Double> hints = new HashMap<>();
        Set<String> craftable = new HashSet<>();
        for (RecipeSolver.Recipe r : scan.recipes) {
            for (RecipeSolver.Out o : r.outputs) {
                craftable.add(o.item);
            }
        }
        for (Price p : prices.values()) {
            String id = itemId(p.stack);
            if (p.dynamicFood && craftable.contains(id)) {
                continue; // 레시피 원가로 값이 정해질 품목: 상점 파일의 임시 가격을 기준표에 넣지 않는다
            }
            if (p.buyableSomewhere) {
                base.merge(id, (double) p.price, Math::min);
            } else if (isPlain(p.stack)) {
                // 상점에서 살 수 없는 재료의 값: 매입가를 직접 정한 품목은 그 매입가(직접 길러서 쓰는 재료의 기회비용)
                hints.put(id, p.fixedSell && !p.buyableSomewhere ? Math.max(0.01, p.sellUnit) : (double) p.price);
            }
        }
        Set<String> anchors = new HashSet<>();
        for (Price p : prices.values()) {
            if (p.anchor && p.buyableSomewhere && base.containsKey(itemId(p.stack))) {
                anchors.add(itemId(p.stack));
            }
        }
        RecipeScan.solve(scan, base, hints, anchors, pc, id -> {
            // 가격표에 없는 말단 재료의 추정가. 공짜로 얻을 수 있는 재료일 수도 있으므로 낮게 잡는다
            // (높게 잡으면 그 재료가 들어간 음식값이 부풀어 돈벌이 수단이 된다).
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != Items.AIR) {
                int f = Pricing.food(new ItemStack(item));
                if (f > 0) {
                    return Math.max(pc.defaultIngredientValue, f * pc.edibleEstimateFactor);
                }
            }
            return pc.defaultIngredientValue;
        });
        RecipeSolver.Result res = scan.result;
        out.solved = res;
        out.scan = scan;
        double ratio = pc.sellRatio > 0 ? pc.sellRatio : 0.5;
        int adjusted = 0;
        List<String> zeroCost = new ArrayList<>();
        for (Map.Entry<String, Price> e : prices.entrySet()) {
            Price p = e.getValue();
            if (!isPlain(p.stack) || p.fixedSell) {
                continue; // NBT 가 붙은 품목(상점 씨앗 등)과 매입가를 직접 적은 품목은 그대로 둔다
            }
            String id = itemId(p.stack);
            if (anchors.contains(id)) {
                p.cost = p.price;
                continue; // 광물 기준가: 상점가의 sellRatio 에 매입 (아래에서 채움)
            }
            Double c = res.cost.get(id);
            Double v = res.value.get(id);
            if (c != null) {
                p.cost = c;
            }
            if (c == null || v == null) {
                continue; // 만들 수 없는 원재료
            }
            if (c < 0.005 || v < 0.005) {
                // 원가가 0 으로 계산됨 = 레시피 해석이 잘못된 것 (공짜로 만들 수 있는 물건은 없다): 레시피 원가를 쓰지 않는다
                zeroCost.add(id + " (" + res.costRecipe.get(id) + ")");
                p.cost = -1;
                continue;
            }
            boolean food = p.stack.isEdible() || res.processed.contains(id);
            if (p.dynamicFood) {
                // 음식·조리 도구: 상점 파일의 가격 대신 "재료 원가 + 수고비"로 매입한다.
                p.sellUnit = v;
                p.price = buyPrice(v, pc.foodBuyMultiplier);
                p.basis = String.valueOf(res.valueRecipe.get(id));
                p.finished = true;
                adjusted++;
            } else if (p.sellableSomewhere && (p.gear || p.price >= 1.5 * c - 1.0e-9)) {
                // 완제품: 장비이거나, 상점가가 만드는 원가보다 훨씬(1.5배 이상) 비싼 품목.
                // 재료 원가만큼 매입하고, 되팔 때 50% 규칙이 지켜지도록 상점가를 원가의 2배 이상으로 맞춘다.
                p.finished = true;
                if (food) {
                    p.sellUnit = v;
                    p.price = Math.max(p.price, buyPrice(v, pc.foodBuyMultiplier));
                    p.basis = String.valueOf(res.valueRecipe.get(id));
                    adjusted++;
                } else if (res.exact.contains(id)) {
                    // 가격표에 있는 재료만으로 만드는 물건: 재료값 그대로 매입 (재료를 사서 만들어 팔면 본전)
                    p.sellUnit = c;
                    p.price = Math.max(p.price, buyPrice(c, 1.0 / ratio));
                    p.basis = String.valueOf(res.costRecipe.get(id));
                } else {
                    // 가격을 추정한 재료가 섞인 물건: 추정이 틀려도 손해가 없도록 보수적으로 (상점가의 50% 와 원가 중 낮은 쪽)
                    p.sellUnit = Math.min(p.price * ratio, c);
                    p.basis = String.valueOf(res.costRecipe.get(id));
                }
            }
            // 그 밖 = 원가와 비슷하거나 더 싸게 파는 기본 재료(설탕, 기름, 숯 등): 상점가의 50% 에 매입 (아래에서 채움)
        }
        if (!zeroCost.isEmpty()) {
            out.notes.add("원가가 0 으로 계산되어 레시피 원가를 쓰지 않은 품목 " + zeroCost.size() + "종: "
                    + String.join(", ", zeroCost.subList(0, Math.min(20, zeroCost.size()))));
        }
        if (pc.purchaseFloor) {
            applyPurchaseFloor(scan, pc, out);
        }
        // 가격 근거(어떤 재료 기준으로 값을 매겼는지)를 상점 화면에 보여 주기 위해 정리한다
        for (Price p : prices.values()) {
            if (!p.finished || p.basis.isEmpty() || p.purchaseBasis) {
                continue;
            }
            try {
                RecipeSolver.Recipe r = scan.byId(p.basis);
                if (r == null) {
                    continue;
                }
                String id = itemId(p.stack);
                boolean food = p.dynamicFood || p.stack.isEdible() || res.processed.contains(id);
                List<ShopData.Part> parts = new ArrayList<>();
                for (RecipeSolver.Pick pick : RecipeSolver.picks(r, res, scan.env, food)) {
                    Item item = findItem(pick.item);
                    if (item != null) {
                        parts.add(new ShopData.Part(item, (float) pick.count));
                    }
                }
                double n = RecipeSolver.outputCount(r, id, scan.env);
                if (!parts.isEmpty() && n > 0) {
                    p.parts = parts;
                    p.partsOut = (float) n;
                }
            } catch (Throwable ignored) {
                // 표시용 정보이므로 실패해도 가격에는 영향이 없다
            }
        }
        out.notes.add(String.format("레시피 파일 %d개 중 %d개 해석, 계산에 쓰지 못한 레시피 %d개, 가격을 추정한 재료 %d종, 원가 기준으로 가격을 정한 음식 %d종",
                scan.files, scan.parsed, res.recipesUnresolved, res.estimated.size(), adjusted));
        RtShop.LOGGER.info("[rtshop] {}", out.notes.get(out.notes.size() - 1));
    }


    // ------------------------------------------------------------------ "재료를 사서 만들어 팔 때" 손해 없음

    private static final Set<String> COOKING_MODS = Set.of("farmersdelight", "kaleidoscope_cookery", "kaleidoscope_tavern",
            "farm_and_charm", "handson_delight", "brewinandchewin", "cookingforblockheads");
    private static final String[] FOOD_TAG_PREFIXES = {"crops", "vegetables", "fruits", "grain", "mushrooms", "eggs", "milk",
            "salad_ingredients", "raw_", "cooking_oil", "flour", "dough", "rice", "berries", "nuts", "spices", "herbs"};

    private static Collection<String> slotItems(RecipeSolver.Ing ing, Map<String, List<String>> tagCache) {
        List<String> out = new ArrayList<>();
        for (String alt : ing.alts) {
            if (alt.startsWith("#")) {
                out.addAll(tagCache.computeIfAbsent(alt.substring(1), k -> {
                    List<String> l = new ArrayList<>();
                    try {
                        ResourceLocation rl = ResourceLocation.tryParse(k);
                        if (rl != null && ForgeRegistries.ITEMS.tags() != null) {
                            ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(rl)).stream().forEach(item -> {
                                ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
                                if (key != null) {
                                    l.add(key.toString());
                                }
                            });
                        }
                    } catch (Throwable ignored) {
                    }
                    return l;
                }));
            } else {
                out.add(alt);
            }
        }
        return out;
    }

    /** 식품 상인에 올려도 되는 '식재료'인가 (먹을 수 있거나, 요리 모드의 재료이거나, 농산물·식재료 태그가 있는 것). */
    private static boolean foodish(String id) {
        Item item = findItem(id);
        if (item == null) {
            return false;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || rl.getPath().startsWith("wild_")) {
            return false; // 야생 작물 덤불은 채집용
        }
        ItemStack st = new ItemStack(item);
        if (st.getMaxStackSize() <= 1 || st.getRarity() != net.minecraft.world.item.Rarity.COMMON) {
            return false; // 하나뿐인 특별한 물건(유물·장비 등)은 팔지 않는다
        }
        if (st.isEdible() || COOKING_MODS.contains(rl.getNamespace())) {
            return !st.isDamageableItem();
        }
        try {
            return st.getTags().anyMatch(t -> {
                if (!t.location().getNamespace().equals("forge")) {
                    return false;
                }
                String path = t.location().getPath();
                for (String pre : FOOD_TAG_PREFIXES) {
                    if (path.startsWith(pre)) {
                        return true;
                    }
                }
                return false;
            });
        } catch (Throwable t) {
            return false;
        }
    }

    /** 요리 조리법인가: 결과물이 먹을 수 있는 것이거나, 수고비·마진이 붙는 조리 기구 레시피. */
    private static boolean foodRecipe(RecipeSolver.Recipe r, PricingConfig pc) {
        if (pc.margins.containsKey(r.type) || pc.labor.containsKey(r.type)) {
            return true;
        }
        for (RecipeSolver.Out o : r.outputs) {
            Item item = findItem(o.item);
            if (item != null && new ItemStack(item).isEdible()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 요리 재료인데 어느 상점에서도 살 수 없는 식재료를 식품 상인에 자동으로 올린다.
     * 그래야 "재료를 모두 상점에서 사서 요리"할 수 있고, 그 값이 요리 매입가 하한에 반영된다.
     */
    private static void addMissingFoodIngredients(List<Raw> raws, Map<String, Price> prices, RecipeScan.Scan scan, PricingConfig pc,
                                                  List<String> notes) {
        Raw food = null;
        for (Raw r : raws) {
            if (r.id.equals("food")) {
                food = r;
            }
        }
        if (food == null) {
            return;
        }
        Map<String, List<String>> tagCache = new HashMap<>();
        Set<String> obtainable = new HashSet<>();
        for (Raw r : raws) {
            for (Ref ref : r.refs) {
                Price p = prices.get(ref.key);
                if (ref.buy && p != null && isPlain(p.stack)) {
                    obtainable.add(itemId(p.stack));
                }
            }
        }
        List<RecipeSolver.Recipe> foodRecipes = new ArrayList<>();
        for (RecipeSolver.Recipe r : scan.recipes) {
            if (foodRecipe(r, pc)) {
                foodRecipes.add(r);
            }
        }
        Set<String> craftable = new HashSet<>();
        for (RecipeSolver.Recipe r : scan.recipes) {
            for (RecipeSolver.Out o : r.outputs) {
                craftable.add(o.item);
            }
        }
        Map<String, Integer> added = new LinkedHashMap<>();
        for (int round = 0; round < 6; round++) {
            // 살 수 있는 것만으로 만들 수 있는 것까지 넓힌다
            boolean grew = true;
            int guard = 0;
            while (grew && guard++ < 40) {
                grew = false;
                for (RecipeSolver.Recipe r : scan.recipes) {
                    boolean newOut = false;
                    for (RecipeSolver.Out o : r.outputs) {
                        if (!obtainable.contains(o.item)) {
                            newOut = true;
                            break;
                        }
                    }
                    if (!newOut) {
                        continue;
                    }
                    boolean ok = true;
                    for (RecipeSolver.Ing ing : r.ingredients) {
                        boolean any = false;
                        for (String it : slotItems(ing, tagCache)) {
                            if (obtainable.contains(it)) {
                                any = true;
                                break;
                            }
                        }
                        if (!any) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        for (RecipeSolver.Out o : r.outputs) {
                            grew |= obtainable.add(o.item);
                        }
                    }
                }
            }
            // 요리 조리법의 재료 칸 중 마련할 방법이 없는 칸: 그 칸에 들어갈 식재료 하나를 올린다 (만들 수 없는 것 우선)
            Map<String, Integer> want = new LinkedHashMap<>();
            for (RecipeSolver.Recipe r : foodRecipes) {
                for (RecipeSolver.Ing ing : r.ingredients) {
                    Collection<String> items = slotItems(ing, tagCache);
                    boolean any = false;
                    for (String it : items) {
                        if (obtainable.contains(it)) {
                            any = true;
                            break;
                        }
                    }
                    if (any) {
                        continue;
                    }
                    String pick = null;
                    for (String it : items) {
                        if (!craftable.contains(it) && foodish(it)) {
                            pick = it;
                            break;
                        }
                    }
                    if (pick == null) {
                        for (String it : items) {
                            if (foodish(it)) {
                                pick = it;
                                break;
                            }
                        }
                    }
                    if (pick != null) {
                        want.merge(pick, 1, Integer::sum);
                    }
                }
            }
            if (want.isEmpty()) {
                break;
            }
            for (String id : want.keySet()) {
                int colon = id.indexOf(':');
                if (food.excludeIds.contains(id) || (colon > 0 && food.excludeNamespaces.contains(id.substring(0, colon)))) {
                    obtainable.add(id); // 관리자가 뺀 품목: 다시 고르지 않게만 표시
                    continue;
                }
                Item item = findItem(id);
                ItemStack stack = new ItemStack(item);
                if (com.rtshop.rules.ItemPolicy.shopBanned(stack)) {
                    obtainable.add(id); // 아이템 규칙이 막은 품목: 다시 고르지 않게만 표시
                    continue;
                }
                String key = key(stack);
                Price p = prices.get(key);
                if (p == null) {
                    p = new Price();
                    p.stack = stack;
                    int f = Pricing.food(stack);
                    p.price = f > 0 ? Math.max(pc.autoIngredientPrice / 2, Pricing.nice(f * 2.0)) : pc.autoIngredientPrice;
                    prices.put(key, p);
                }
                boolean had = false;
                for (Ref ref : food.refs) {
                    if (ref.key.equals(key)) {
                        ref.buy = true;
                        had = true;
                    }
                }
                if (!had && food.keys.add(key)) {
                    Ref ref = new Ref();
                    ref.key = key;
                    ref.category = "ingredient";
                    ref.buy = true;
                    ref.sell = true;
                    ref.loose = true;
                    food.refs.add(ref);
                }
                obtainable.add(id);
                added.put(id, p.price);
            }
        }
        if (!added.isEmpty()) {
            List<String> list = new ArrayList<>();
            added.forEach((k, v) -> list.add(k + "=" + v));
            RtShop.LOGGER.info("[rtshop] 상점에서 살 수 없던 요리 재료 {}종을 식품 상인에 올렸습니다: {}", added.size(), list);
            notes.add("상점에서 살 수 없던 요리 재료 " + added.size() + "종을 식품 상인에 올렸습니다 (pricing.json 의 autoListFoodIngredients): "
                    + String.join(", ", list));
        }
    }

    /**
     * 서로 되돌릴 수 있는 보관용 조리법(작물 상자 <-> 작물 9개, 블록 <-> 주괴 등)을 찾는다.
     * 이런 조리법은 "요리"가 아니므로 매입가 하한 계산에서 뺀다.
     */
    private static Set<RecipeSolver.Recipe> storageRecipes(RecipeScan.Scan scan) {
        Map<String, List<RecipeSolver.Recipe>> singleByIn = new HashMap<>();
        Map<RecipeSolver.Recipe, String> inOf = new java.util.IdentityHashMap<>();
        Map<String, List<String>> tagCache = new HashMap<>();
        for (RecipeSolver.Recipe r : scan.recipes) {
            if (r.ingredients.isEmpty() || r.outputs.size() != 1) {
                continue;
            }
            Set<String> ins = new HashSet<>();
            for (RecipeSolver.Ing ing : r.ingredients) {
                Collection<String> items = slotItems(ing, tagCache);
                if (items.size() != 1) {
                    ins.add("?multi");
                }
                ins.addAll(items);
            }
            if (ins.size() == 1) {
                String in = ins.iterator().next();
                inOf.put(r, in);
                singleByIn.computeIfAbsent(in, k -> new ArrayList<>()).add(r);
            }
        }
        Set<RecipeSolver.Recipe> out = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Map.Entry<RecipeSolver.Recipe, String> e : inOf.entrySet()) {
            String in = e.getValue();
            String outItem = e.getKey().outputs.get(0).item;
            for (RecipeSolver.Recipe back : singleByIn.getOrDefault(outItem, List.of())) {
                if (back.outputs.get(0).item.equals(in)) {
                    out.add(e.getKey());
                    out.add(back);
                }
            }
        }
        return out;
    }

    /** 매입가 하한을 걸 품목인가: 상점에서 파는(되사는) 음식·조리 가공품 중, 원재료로 파는 것이 아닌 것. */
    private static boolean floorTarget(Price p, RecipeSolver.Result res) {
        if (p == null || !isPlain(p.stack) || !p.sellableSomewhere || p.rawStock) {
            return false;
        }
        String id = itemId(p.stack);
        return p.dynamicFood || p.stack.isEdible() || (res != null && res.processed.contains(id));
    }

    /**
     * 화로·훈연기·조합대처럼 수고비가 없는(=자동화할 수 있는) 조리법인가.
     * 이런 조리법으로 싸게 만들 수 있는 음식은 그 값을 넘는 하한을 주지 않는다 (자동화로 돈을 찍어 내지 못하게).
     */
    private static final Set<String> HOPPER_TYPES = Set.of("minecraft:smelting", "minecraft:smoking", "minecraft:blasting");

    private static boolean automatable(RecipeSolver.Recipe r, PricingConfig pc) {
        // 이 모드팩에는 자동 조합기가 없으므로, 사람 손 없이 돌릴 수 있는 것은 깔때기로 넣는 화로·훈연기·용광로뿐이다
        return HOPPER_TYPES.contains(r.type) && !pc.labor.containsKey(r.type);
    }

    private record Need(double value, RecipeSolver.Recipe by, List<RecipeSolver.Pick> picks, double cheapest, List<RecipeSolver.Recipe> skipped,
                        double autoMin, double fullMax, List<AutoRoute> autoRoutes, List<RecipeCost> costs) {
    }

    /** 조리법 하나의 재료비와 쓰인 재료. */
    private record RecipeCost(RecipeSolver.Recipe recipe, double cost, Set<String> items) {
    }

    /** 상점에서 산 재료를 그대로 화로에 넣어 만드는 길. */
    private record AutoRoute(RecipeSolver.Recipe recipe, List<RecipeSolver.Pick> picks, double cost) {
    }

    /**
     * 한 음식의 매입가 하한: 재료를 모두 살 수 있는 조리법들의 순수 재료비 중 가장 큰 값.
     * 단 자동화할 수 있는 조리법으로 더 싸게 만들 수 있으면, 그보다 비싼 조리법은 보호하지 않는다 (skipped).
     */
    private static Need need(String id, Map<String, List<RecipeSolver.Recipe>> makers, Map<String, Double> pcost, RecipeSolver.Env env,
                             RecipeSolver.Config pure, PricingConfig pc, Map<String, Double> supply) {
        List<RecipeSolver.Recipe> rs = makers.getOrDefault(id, List.of());
        double autoMin = Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        double[] costs = new double[rs.size()];
        double fullMax = -1;
        List<AutoRoute> routes = new ArrayList<>();
        List<RecipeCost> all = new ArrayList<>();
        for (int i = 0; i < rs.size(); i++) {
            List<RecipeSolver.Pick> used = new ArrayList<>();
            costs[i] = RecipeSolver.purchaseRequirement(rs.get(i), id, pcost, env, pure, used);
            if (costs[i] > 0) {
                Set<String> items = new HashSet<>();
                for (RecipeSolver.Pick u : used) {
                    items.add(u.item);
                }
                all.add(new RecipeCost(rs.get(i), costs[i], items));
                min = Math.min(min, costs[i]);
                // 재료를 상점에서 산 그대로 화로에 넣어 만들 수 있는 경우만 '자동화 가능'으로 본다
                boolean direct = !used.isEmpty();
                for (RecipeSolver.Pick u : used) {
                    direct &= supply.containsKey(u.item);
                }
                if (direct && automatable(rs.get(i), pc)) {
                    autoMin = Math.min(autoMin, costs[i]);
                    routes.add(new AutoRoute(rs.get(i), used, costs[i]));
                }
                fullMax = Math.max(fullMax, costs[i]);
            }
        }
        double best = -1;
        RecipeSolver.Recipe by = null;
        List<RecipeSolver.Recipe> skipped = new ArrayList<>();
        for (int i = 0; i < rs.size(); i++) {
            if (costs[i] <= 0) {
                continue;
            }
            if (costs[i] > autoMin + 1.0e-6) {
                skipped.add(rs.get(i));
                continue;
            }
            if (costs[i] > best) {
                best = costs[i];
                by = rs.get(i);
            }
        }
        List<RecipeSolver.Pick> picks = new ArrayList<>();
        if (by != null) {
            RecipeSolver.purchaseRequirement(by, id, pcost, env, pure, picks);
        }
        return new Need(best, by, picks, min == Double.MAX_VALUE ? -1 : min, skipped, autoMin == Double.MAX_VALUE ? -1 : autoMin,
                fullMax, routes, all);
    }

    /** 다 쓰고 돌려받는 그릇(빈 양동이·병 등)을 되팔 때 받는 값. */
    private static Map<String, Double> refunds(Map<String, Price> prices, PricingConfig pc) {
        Map<String, Double> m = new HashMap<>();
        for (Price p : prices.values()) {
            if (p.sellableSomewhere && isPlain(p.stack)) {
                double v = p.sellUnit >= 0 ? p.sellUnit : p.price * pc.sellRatio;
                m.merge(itemId(p.stack), v, Math::max);
            }
        }
        return m;
    }

    /** 지금 가격표에서 살 수 있는 일반 아이템의 가장 싼 판매가. */
    private static Map<String, Double> supply(Map<String, Price> prices) {
        Map<String, Double> m = new HashMap<>();
        for (Price p : prices.values()) {
            if (p.buyableSomewhere && isPlain(p.stack) && p.price > 0) {
                m.merge(itemId(p.stack), (double) p.price, Math::min);
            }
        }
        return m;
    }

    private static Map<String, List<RecipeSolver.Recipe>> byOutput(RecipeScan.Scan scan, Set<RecipeSolver.Recipe> skip) {
        Map<String, List<RecipeSolver.Recipe>> m = new HashMap<>();
        for (RecipeSolver.Recipe r : scan.recipes) {
            if (skip.contains(r)) {
                continue;
            }
            for (RecipeSolver.Out o : r.outputs) {
                List<RecipeSolver.Recipe> l = m.computeIfAbsent(o.item, k -> new ArrayList<>());
                if (!l.contains(r)) {
                    l.add(r);
                }
            }
        }
        return m;
    }

    /**
     * 요리 매입가 하한: 레시피의 재료를 모두 상점에서 사서 만들어 팔아도 손해가 없게 한다.
     * 음식마다 "재료를 상점에서 살 수 있는" 모든 조리법 중 가장 비싼 것의 재료비 이상으로 매입하고,
     * 되팔기 규칙(50%)을 지키도록 상점 판매가도 그 2배 이상으로 맞춘다.
     * 요리가 다른 요리의 재료이면 앞 요리의 상점가가 오르면서 뒤 요리도 오르므로 바뀌지 않을 때까지 몇 번 반복한다.
     */
    private static void applyPurchaseFloor(RecipeScan.Scan scan, PricingConfig pc, Loaded out) {
        // 하한은 '순수 재료비'로만 잡는다 (수고비·마진 제외). 수고비가 붙은 조리법과 화로처럼 자동화되는 조리법이 같은 음식을 만들 때,
        // 수고비까지 하한에 넣으면 화로로 만들어 팔기만 해도 돈이 불어나기 때문이다. 수고비는 원래 계산(가장 싼 방법 + 수고비)에 이미 들어 있다.
        RecipeSolver.Config cfg = new RecipeSolver.Config();
        cfg.flatExtra.putAll(pc.flatExtra);
        Set<RecipeSolver.Recipe> storage = storageRecipes(scan);
        List<RecipeSolver.Recipe> usable = new ArrayList<>();
        for (RecipeSolver.Recipe r : scan.recipes) {
            if (!storage.contains(r)) {
                usable.add(r);
            }
        }
        Map<String, List<RecipeSolver.Recipe>> makers = byOutput(scan, storage);
        double ratio = pc.sellRatio > 0 ? pc.sellRatio : 0.5;
        int raised = 0;
        Set<String> raisedIds = new HashSet<>();
        Set<String> raisedInputs = new java.util.TreeSet<>();
        for (int round = 0; round < 8; round++) {
            RecipeSolver.setRefunds(refunds(out.prices, pc));
            Map<String, Double> sup = supply(out.prices);
            Map<String, Double> pcost = RecipeSolver.purchaseCosts(sup, usable, scan.env, cfg);
            boolean changed = false;
            for (Price p : out.prices.values()) {
                if (!floorTarget(p, out.solved)) {
                    continue;
                }
                String id = itemId(p.stack);
                Need n = need(id, makers, pcost, scan.env, cfg, pc, sup);
                // 화로에 바로 넣는 싼 길 때문에 다른 조리법이 보호받지 못하는 경우: 그 화로 재료가 '만들어 쓰는 중간 재료'(반죽 등)이면
                // 중간 재료의 상점가를 올려 화로 길도 같은 값이 되게 한다. 고기 같은 원재료 값은 건드리지 않는다.
                if (n.fullMax() > 0) {
                    for (AutoRoute route : n.autoRoutes()) {
                        if (route.picks().size() != 1) {
                            continue;
                        }
                        RecipeSolver.Pick in = route.picks().get(0);
                        // 목표 값은 이 중간 재료를 쓰지 않는 조리법들로만 정한다 (쓰는 조리법까지 넣으면 올릴수록 목표도 올라 끝없이 오른다)
                        double others = -1;
                        for (RecipeCost rc : n.costs()) {
                            if (!rc.items().contains(in.item)) {
                                others = Math.max(others, rc.cost());
                            }
                        }
                        double target = others * (1.0 + pc.handCookedMargin);
                        if (others <= 0 || route.cost() + 1.0e-6 >= target) {
                            continue;
                        }
                        if (!makers.containsKey(in.item) || in.count <= 0) {
                            continue; // 원재료
                        }
                        Price ip = out.prices.get(in.item);
                        if (ip == null || ip.fixedSell || !ip.buyableSomewhere || ip.rawStock) {
                            continue; // 상점이 원재료(농산물·고기 등)로 파는 것은 값을 바꾸지 않는다
                        }
                        double outN = RecipeSolver.outputCount(route.recipe(), id, scan.env);
                        int newPrice = (int) Math.ceil(target * outN / in.count - 1.0e-9);
                        if (newPrice > ip.price) {
                            if (ip.sellUnit < 0) {
                                ip.sellUnit = ip.price * ratio; // 되파는 값은 그대로 (올린 판매가로 되팔아 이익을 보지 못하게)
                            }
                            ip.price = newPrice;
                            changed = true;
                            raisedInputs.add(in.item + "=" + newPrice);
                        }
                    }
                }
                p.purchaseNeed = n.value();
                p.autoMin = n.autoMin();
                if (n.value() <= 0) {
                    continue;
                }
                // 손으로 만드는 요리는 재료비보다 조금(handCookedMargin) 더. 단 상점 재료를 화로에 바로 넣어 그보다 싸게 만들 수 있으면
                // (화로 자동화로 돈을 찍어 낼 수 있으므로) 본전.
                double handFloor = n.value() * (1.0 + pc.handCookedMargin);
                p.handMade = n.autoMin() <= 0 || n.autoMin() + 1.0e-6 >= handFloor;
                p.cookable = n.autoMin() <= 0;
                double floorV = p.handMade ? handFloor : n.value();
                double now = p.sellUnit >= 0 ? p.sellUnit : p.price * ratio;
                if (now + 1.0e-6 >= floorV) {
                    continue;
                }
                p.sellUnit = floorV;
                p.price = Math.max(p.price, buyPrice(floorV, Math.max(pc.foodBuyMultiplier, 1.0 / ratio)));
                p.finished = true;
                p.basis = n.by().id;
                p.purchaseBasis = true;
                List<ShopData.Part> parts = new ArrayList<>();
                for (RecipeSolver.Pick pick : n.picks()) {
                    Item item = findItem(pick.item);
                    if (item != null) {
                        parts.add(new ShopData.Part(item, (float) pick.count));
                    }
                }
                double cnt = RecipeSolver.outputCount(n.by(), id, scan.env);
                if (!parts.isEmpty() && cnt > 0) {
                    p.parts = parts;
                    p.partsOut = (float) cnt;
                }
                changed = true;
                if (raisedIds.add(id)) {
                    raised++;
                }
            }
            if (!changed) {
                break;
            }
        }
        out.notes.add("재료를 모두 상점에서 사서 만들어도 손해가 없도록 매입가를 올린 음식: " + raised + "종 (pricing.json 의 purchaseFloor)");
        if (!raisedInputs.isEmpty()) {
            out.notes.add("화로에 바로 넣어 싸게 만드는 길을 막으려고 판매가를 올린 중간 재료: " + String.join(", ", raisedInputs));
        }
        RtShop.LOGGER.info("[rtshop] {}", out.notes.get(out.notes.size() - 1));
    }

    /** 같은 음식의 조리법끼리 재료비가 30% 이상 차이 나는 경우 (싼 조리법으로 만들면 이익이 크게 남는 음식). */
    public static List<String> purchaseSpread(Loaded loaded) {
        List<String> out = new ArrayList<>();
        if (loaded.scan == null || loaded.scan.env == null) {
            return out;
        }
        PricingConfig pc = PricingConfig.get();
        RecipeSolver.Config cfg = new RecipeSolver.Config();
        cfg.flatExtra.putAll(pc.flatExtra);
        Set<RecipeSolver.Recipe> storage = storageRecipes(loaded.scan);
        List<RecipeSolver.Recipe> usable = new ArrayList<>();
        for (RecipeSolver.Recipe r : loaded.scan.recipes) {
            if (!storage.contains(r)) {
                usable.add(r);
            }
        }
        Map<String, List<RecipeSolver.Recipe>> makers = byOutput(loaded.scan, storage);
        RecipeSolver.setRefunds(refunds(loaded.prices, pc));
        Map<String, Double> sup = supply(loaded.prices);
        Map<String, Double> pcost = RecipeSolver.purchaseCosts(sup, usable, loaded.scan.env, cfg);
        for (Price p : loaded.prices.values()) {
            if (!floorTarget(p, loaded.solved)) {
                continue;
            }
            String id = itemId(p.stack);
            Need n = need(id, makers, pcost, loaded.scan.env, cfg, pc, sup);
            if (n.value() > 0 && n.cheapest() > 0 && n.value() > n.cheapest() * 1.3) {
                out.add(String.format(Locale.ROOT, "%s | 매입가 %.1f | 싼 조리법 재료비 %.1f | 비싼 조리법 재료비 %.1f (%s)",
                        id, p.sellUnit, n.cheapest(), n.value(), n.by() == null ? "" : n.by().type));
            }
            for (RecipeSolver.Recipe r : n.skipped()) {
                out.add(String.format(Locale.ROOT, "%s | 보호 안 함: %s [%s] (상점 재료를 화로에 바로 넣어 더 싸게 만들 수 있음)", id, r.id, r.type));
            }
        }
        return out;
    }

    /** 결과 점검: 재료를 상점에서 사서 어떤 조리법으로 만들어도 매입가가 재료비 이상인가. 손해인 경우의 설명 목록. */
    public static List<String> purchaseDeficits(Loaded loaded) {
        List<String> out = new ArrayList<>();
        if (loaded.scan == null || loaded.scan.env == null) {
            return out;
        }
        PricingConfig pc = PricingConfig.get();
        RecipeSolver.Config cfg = new RecipeSolver.Config();
        cfg.flatExtra.putAll(pc.flatExtra);
        Set<RecipeSolver.Recipe> storage = storageRecipes(loaded.scan);
        List<RecipeSolver.Recipe> usable = new ArrayList<>();
        for (RecipeSolver.Recipe r : loaded.scan.recipes) {
            if (!storage.contains(r)) {
                usable.add(r);
            }
        }
        Map<String, List<RecipeSolver.Recipe>> makers = byOutput(loaded.scan, storage);
        RecipeSolver.setRefunds(refunds(loaded.prices, pc));
        Map<String, Double> sup = supply(loaded.prices);
        Map<String, Double> pcost = RecipeSolver.purchaseCosts(sup, usable, loaded.scan.env, cfg);
        for (Price p : loaded.prices.values()) {
            if (!floorTarget(p, loaded.solved)) {
                continue;
            }
            String id = itemId(p.stack);
            Need n = need(id, makers, pcost, loaded.scan.env, cfg, pc, sup);
            if (n.autoMin() > 0 && p.sellUnit > n.autoMin() + 1.0e-6) {
                out.add(String.format(Locale.ROOT, "%s 화로 자동화로 이익: 매입가 %.2f > 상점 재료를 구워 만드는 값 %.2f", id, p.sellUnit, n.autoMin()));
            }
            for (RecipeSolver.Recipe r : makers.getOrDefault(id, List.of())) {
                if (n.skipped().contains(r)) {
                    continue; // 더 싼 자동화 조리법이 있는 음식의 비싼 조리법 (보고서에 따로 적음)
                }
                double cost = RecipeSolver.purchaseRequirement(r, id, pcost, loaded.scan.env, cfg, null);
                if (cost > 0 && p.sellUnit + 1.0e-6 < cost) {
                    out.add(String.format(Locale.ROOT, "%s 매입가 %.2f < 재료 구입비 %.2f (%s)", id, p.sellUnit, cost, r.id));
                }
            }
        }
        return out;
    }

    /**
     * 요리사 판매 보너스를 받는 '손으로 조리한 요리'인가: 수고비가 붙는 조리법으로 값이 정해졌고,
     * 상점 재료를 화로에 바로 넣어 만드는 길이 없는 것 (있으면 화로 자동화 + 보너스로 돈을 찍어 낼 수 있음).
     */
    public static boolean handCooked(Loaded loaded, ItemStack stack) {
        Price p = loaded.prices.get(key(stack));
        if (p == null || loaded.solved == null || p.autoMin > 0) {
            return false;
        }
        if (p.cookable) {
            return true;
        }
        String id = itemId(stack);
        return loaded.solved.processed.contains(id) || p.dynamicFood;
    }

    /** 매입가의 multiplier 배 이상이 되는 가장 작은 정수 판매가. */
    private static int buyPrice(double sell, double multiplier) {
        return (int) Math.max(1, Math.min(2_000_000_000L, (long) Math.ceil(sell * multiplier - 1.0e-9)));
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
        return itemId(s) + (isPlain(s) ? "" : String.valueOf(s.getTag()));
    }

    /**
     * 특별한 NBT 가 없는 '일반' 아이템인가.
     * 도구·무기·방어구는 새것도 {Damage:0} 태그를 갖고 있으므로, 그 태그만 있는 경우는 일반 아이템으로 본다
     * (그러지 않으면 장비가 "NBT 품목"으로 취급되어 재료 원가 계산에서 빠진다).
     */
    public static boolean isPlain(ItemStack s) {
        if (!s.hasTag()) {
            return true;
        }
        CompoundTag t = s.getTag();
        return t != null && t.size() == 1 && t.contains("Damage") && s.getDamageValue() == 0;
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
                if (com.rtshop.rules.ItemPolicy.shopBanned(stack)) {
                    com.rtshop.rules.ItemPolicy.noteSkipped(r.id, stack); // 아이템 규칙: NPC 상점에 오르지 않는 물건
                    notes.add(String.format("%s: 아이템 규칙(item_policy.json)으로 NPC 상점에 올릴 수 없어 상점 '%s'에서 뺐습니다", id, r.id));
                    continue;
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
                    p.dynamicFood = bool(o, "dynamic", false) && isPlain(stack) && !p.fixedSell;
                    p.anchor = isPlain(stack) && !p.dynamicFood && (str(o, "category", "").equals("mineral") || oreProduct(stack));
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

    private static final String[] ORE_TAGS = {"forge:ingots", "forge:raw_materials", "forge:gems", "forge:nuggets", "forge:dusts"};

    /** 광석에서 나오는 재료(주괴·원석·보석·조각·가루)인가. */
    static boolean oreProduct(ItemStack s) {
        for (String t : ORE_TAGS) {
            ResourceLocation rl = ResourceLocation.tryParse(t);
            if (rl != null && s.is(ItemTags.create(rl))) {
                return true;
            }
        }
        return false;
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
                        if (com.rtshop.rules.ItemPolicy.shopBanned(plain)) {
                            com.rtshop.rules.ItemPolicy.noteSkipped(r.id, plain); // 아이템 규칙: NPC 상점에 오르지 않는 물건
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
                        p.dynamicFood = dynamic && isPlain(stack) && !p.fixedSell;
                        p.gear = formula.equals("gear") || (formula.equals("auto") && Pricing.gear(plain) > 0);
                        prices.put(key, p);
                    }
                    Ref ref = new Ref();
                    ref.key = key;
                    ref.category = category;
                    ref.buy = buy && !PricingConfig.get().notForSaleMods.contains(rl.getNamespace());
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
                        itemId(e.stack), isPlain(e.stack) ? "" : " " + e.stack.getTag(),
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
            if (p.finished && p.cost >= 0 && isPlain(p.stack) && (p.stack.isEdible() || p.dynamicFood) && p.sellUnit + 1.0e-6 < p.cost) {
                audit.append(String.format(Locale.ROOT, "적자 가능: %s 매입가 %.2f < 원가 %.2f (%s)%n", itemId(p.stack), p.sellUnit, p.cost, p.basis));
                deficits++;
            }
        }
        audit.append("적자 가능 음식: ").append(deficits).append("종\n");
        List<String> buyLoss = purchaseDeficits(loaded);
        audit.append("\n# 재료를 모두 상점에서 사서 만들어 팔 때 손해인 조리법\n");
        for (String line : buyLoss) {
            audit.append(line).append('\n');
        }
        audit.append("재료를 사서 만들면 손해인 조리법: ").append(buyLoss.size()).append("개\n");
        List<String> spread = purchaseSpread(loaded);
        audit.append("\n# 같은 음식인데 조리법에 따라 재료비 차이가 큰 경우 (음식 | 매입가 | 가장 싼 조리법 재료비 | 조리법)\n");
        audit.append("# 매입가는 가장 비싼 조리법 기준이라, 싼 조리법으로 만들면 차액만큼 남습니다.\n");
        for (String line : spread) {
            audit.append(line).append('\n');
        }
        audit.append("재료비 차이가 큰 음식: ").append(spread.size()).append("종\n");
        // 요리 재료로 쓰이는데 상점에서 사는 것보다 직접 만드는 편이 싼 품목: 사서 쓰면 차액만큼 손해를 본다
        audit.append("\n# 요리 재료 중 직접 만들면 더 싼 품목 (아이템 | 상점가 | 만드는 원가 | 조리법)\n");
        audit.append("# 음식값은 가장 싼 방법 기준으로 매겨지므로, 아래 품목을 상점에서 사서 재료로 쓰면 차액만큼 손해입니다.\n");
        Set<String> usedInFood = new HashSet<>();
        if (loaded.scan != null && loaded.scan.env != null) {
            Set<String> foods = new HashSet<>();
            for (Price p : loaded.prices.values()) {
                if (isPlain(p.stack) && (p.dynamicFood || p.stack.isEdible())) {
                    foods.add(itemId(p.stack));
                }
            }
            for (RecipeSolver.Recipe r : loaded.scan.recipes) {
                boolean makesFood = false;
                for (RecipeSolver.Out o : r.outputs) {
                    makesFood |= foods.contains(o.item);
                }
                if (!makesFood) {
                    continue;
                }
                for (RecipeSolver.Ing ing : r.ingredients) {
                    for (String alt : ing.alts) {
                        if (alt.startsWith("#")) {
                            usedInFood.addAll(loaded.scan.env.tagItems(alt.substring(1)));
                        } else {
                            usedInFood.add(alt);
                        }
                    }
                }
            }
        }
        int traps = 0;
        for (Price p : loaded.prices.values()) {
            if (p.buyableSomewhere && !p.dynamicFood && isPlain(p.stack) && p.cost >= 0 && p.price > p.cost + 1.0e-6
                    && usedInFood.contains(itemId(p.stack))) {
                audit.append(String.format(Locale.ROOT, "%s | %d | %.2f | %s%n", itemId(p.stack), p.price, p.cost,
                        loaded.solved == null ? "" : String.valueOf(loaded.solved.costRecipe.get(itemId(p.stack)))));
                traps++;
            }
        }
        audit.append("해당 품목: ").append(traps).append("종\n");
        if (loaded.solved != null && !loaded.solved.estimated.isEmpty()) {
            audit.append("\n# 가격표에 없어 추정가를 쓴 재료 (상점 파일에 가격을 적으면 더 정확해집니다)\n");
            loaded.solved.estimated.forEach((k, v) -> audit.append(String.format(Locale.ROOT, "%s = %.0f%n", k, v)));
        }
        if (loaded.scan != null && !loaded.scan.unparsedTypes.isEmpty()) {
            audit.append("\n# 해석하지 못한 레시피 종류 (종류 = 개수). 이 조리법으로 만드는 물건은 원가 계산에 반영되지 않습니다.\n");
            loaded.scan.unparsedTypes.forEach((k, v) -> audit.append(k).append(" = ").append(v).append('\n'));
        }
        Files.writeString(dir.resolve("audit.txt"), audit.toString(), StandardCharsets.UTF_8);
        return total;
    }
}
