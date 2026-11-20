package com.rtshop.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * config/rtshop/shops/*.json 을 읽어 상점들을 만듭니다. (파일 이름 = 상점 ID)
 * - "items": 가격을 직접 적은 아이템
 * - "auto":  태그/모드/종류로 아이템을 자동 수집하고 공식으로 가격을 매기는 규칙
 * 한 아이템은 전체 상점을 통틀어 한 곳에서만 취급합니다(상점 간 가격 차이를 이용한 무한 돈벌이 방지).
 */
public final class ShopLoader {
    private ShopLoader() {
    }

    public static final String[] DEFAULTS = {"blacksmith", "farmer", "fisher", "general"};

    private static int versionCounter = 0;

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("shops");
    }

    private static final class Raw {
        String id;
        JsonObject json;
        final List<ShopData.Category> categories = new ArrayList<>();
        final List<ShopData.Entry> entries = new ArrayList<>();
        final Set<String> excludeIds = new HashSet<>();
        final Set<String> excludeNamespaces = new HashSet<>();
    }

    public static synchronized Map<String, ShopData> loadAll() {
        Map<String, ShopData> out = new LinkedHashMap<>();
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
                String id = name.substring(0, name.length() - 5).toLowerCase(java.util.Locale.ROOT);
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
                }
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] 상점 폴더를 읽는 중 오류", ex);
        }

        versionCounter++;
        Set<String> claimed = new HashSet<>();

        // 1단계: 직접 적은 아이템 (모든 상점)
        for (Raw r : raws) {
            parseHeader(r);
            parseExplicit(r, claimed);
        }
        // 2단계: 자동 규칙
        List<Item> allItems = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        allItems.sort(Comparator.comparing(i -> String.valueOf(ForgeRegistries.ITEMS.getKey(i))));
        for (Raw r : raws) {
            parseAuto(r, claimed, allItems);
        }

        for (Raw r : raws) {
            JsonObject o = r.json;
            double ratio = o.has("sellRatio") ? o.get("sellRatio").getAsDouble() : 0.5D;
            ratio = Math.max(0.0D, Math.min(1.0D, ratio));
            out.put(r.id, new ShopData(r.id, versionCounter, str(o, "title", r.id), str(o, "npcName", r.id),
                    str(o, "profession", ""), ratio, r.categories, r.entries));
            RtShop.LOGGER.info("[rtshop] 상점 '{}' 로드: 아이템 {}개", r.id, r.entries.size());
        }
        return out;
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

    private static String key(ItemStack s) {
        return ForgeRegistries.ITEMS.getKey(s.getItem()) + (s.hasTag() ? s.getTag().toString() : "");
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

    private static void parseExplicit(Raw r, Set<String> claimed) {
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
                if (!claimed.add(key(stack))) {
                    RtShop.LOGGER.warn("[rtshop] ({}) {} 은(는) 이미 다른 상점에서 취급 중이라 건너뜁니다.", r.id, id);
                    continue;
                }
                int price = Math.max(1, o.has("price") ? o.get("price").getAsInt() : 1);
                r.entries.add(new ShopData.Entry(stack, price, str(o, "category", ""), bool(o, "looseSell", false)));
            } catch (Exception ex) {
                RtShop.LOGGER.warn("[rtshop] ({}) 잘못된 아이템 항목을 건너뜁니다: {} ({})", r.id, el, ex.toString());
            }
        }
    }

    private static void parseAuto(Raw r, Set<String> claimed, List<Item> allItems) {
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
                if (mods.isEmpty() && classes.isEmpty() && tags.isEmpty()) {
                    continue; // 조건 없는 규칙은 모든 아이템을 쓸어 담으므로 무시
                }
                String category = str(rule, "category", "");
                String formula = str(rule, "formula", "fixed");
                int fixed = rule.has("price") ? rule.get("price").getAsInt() : -1;
                double mult = rule.has("multiplier") ? rule.get("multiplier").getAsDouble() : 1.0;
                boolean rarityScale = bool(rule, "rarityScale", false);
                boolean loose = bool(rule, "looseSell", false);
                int count = 0;

                for (Item item : allItems) {
                    if (item == Items.AIR) {
                        continue;
                    }
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                    if (rl == null || r.excludeNamespaces.contains(rl.getNamespace()) || r.excludeIds.contains(rl.toString())) {
                        continue;
                    }
                    if (!mods.isEmpty() && !mods.contains(rl.getNamespace())) {
                        continue;
                    }
                    ItemStack stack;
                    int price;
                    try {
                        stack = new ItemStack(item);
                        if (stack.isEmpty()) {
                            continue;
                        }
                        if (!tags.isEmpty()) {
                            boolean any = false;
                            for (TagKey<Item> t : tags) {
                                if (stack.is(t)) {
                                    any = true;
                                    break;
                                }
                            }
                            if (!any) {
                                continue;
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
                            if (!any) {
                                continue;
                            }
                        }
                        price = computePrice(stack, formula, fixed, rarityScale);
                    } catch (Throwable t) {
                        continue; // 다른 모드 아이템이 예외를 던져도 상점 로딩이 멈추지 않게 함
                    }
                    if (price <= 0) {
                        continue;
                    }
                    price = Pricing.nice(price * mult);
                    if (!claimed.add(key(stack))) {
                        continue;
                    }
                    r.entries.add(new ShopData.Entry(stack, price, category, loose));
                    count++;
                }
                RtShop.LOGGER.info("[rtshop] ({}) 자동 규칙 {} -> {}개", r.id, match, count);
            } catch (Exception ex) {
                RtShop.LOGGER.warn("[rtshop] ({}) 잘못된 auto 규칙을 건너뜁니다: {} ({})", r.id, el, ex.toString());
            }
        }
    }

    private static int computePrice(ItemStack stack, String formula, int fixed, boolean rarityScale) {
        int p;
        switch (formula) {
            case "gear":
                p = Pricing.gear(stack);
                break;
            case "food":
                p = Pricing.food(stack);
                break;
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

    /** 현재 적용된 가격표를 config/rtshop/export/상점ID.json 으로 내보냅니다. */
    public static int export(Map<String, ShopData> shops) throws Exception {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("export");
        Files.createDirectories(dir);
        int total = 0;
        for (ShopData d : shops.values()) {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < d.entries.size(); i++) {
                ShopData.Entry e = d.entries.get(i);
                sb.append("  { \"item\": \"").append(ForgeRegistries.ITEMS.getKey(e.stack.getItem()))
                        .append("\", \"price\": ").append(e.price)
                        .append(", \"category\": \"").append(e.category).append("\"");
                if (e.looseSell) {
                    sb.append(", \"looseSell\": true");
                }
                sb.append(" }").append(i + 1 < d.entries.size() ? "," : "").append("\n");
                total++;
            }
            sb.append("]\n");
            Files.writeString(dir.resolve(d.id + ".json"), sb.toString(), StandardCharsets.UTF_8);
        }
        return total;
    }
}
