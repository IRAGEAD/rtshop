package com.rtshop.gacha;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.job.JobConfig;
import com.rtshop.job.WeaponTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * config/rtshop/gacha.json : 뽑기 설정.
 *  - 뽑기 종류(무기·펫·장신구)마다 아이템 목록(pool). 아이템마다 등급(일반~전설)과 같은 등급 안에서의 가중치(weight).
 *  - 뽑기 단계(일반·중급·고급)마다 등급별 확률(weights). 티켓 한 장 = 한 번.
 *  - 처음 만들 때 모드 아이템을 성능(공격력·희귀도·체력)으로 줄 세워 자동으로 채운다. 게임 안 편집기(OP)로 고친다.
 */
public final class GachaConfig {
    public static final String[] GRADES = {"common", "uncommon", "rare", "epic", "legendary"};
    public static final String[] CATEGORIES = {"weapon", "pet", "accessory"};
    public static final String[] TIERS = {"normal", "mid", "high"};
    /** 자동 분류 때 등급별로 나눌 비율 (성능 순서로 아래부터). */
    private static final double[] AUTO_SPLIT = {0.35, 0.30, 0.22, 0.10, 0.03};

    public static int gradeIndex(String g) {
        for (int i = 0; i < GRADES.length; i++) {
            if (GRADES[i].equals(g)) {
                return i;
            }
        }
        return -1;
    }

    /** 목록의 한 칸: 아이템(item, count, nbt) 또는 펫(entity). */
    public static final class Entry {
        public String item = "";
        public String entity = "";
        public int count = 1;
        /** 인챈트 등 아이템 데이터 (SNBT, 비워도 됨). */
        public String nbt = "";
        public String grade = "common";
        public int weight = 10;

        public String key() {
            return entity.isEmpty() ? item + (nbt.isEmpty() ? "" : "#" + Integer.toHexString(nbt.hashCode())) : "entity:" + entity;
        }

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            if (!entity.isEmpty()) {
                o.addProperty("entity", entity);
            } else {
                o.addProperty("item", item);
                o.addProperty("count", count);
                if (!nbt.isEmpty()) {
                    o.addProperty("nbt", nbt);
                }
            }
            o.addProperty("grade", grade);
            o.addProperty("weight", weight);
            return o;
        }

        static Entry fromJson(JsonObject o) {
            Entry e = new Entry();
            e.item = o.has("item") ? o.get("item").getAsString().trim() : "";
            e.entity = o.has("entity") ? o.get("entity").getAsString().trim() : "";
            e.count = Math.max(1, Math.min(64, o.has("count") ? o.get("count").getAsInt() : 1));
            e.nbt = o.has("nbt") ? o.get("nbt").getAsString() : "";
            e.grade = o.has("grade") ? o.get("grade").getAsString() : "common";
            e.weight = Math.max(0, o.has("weight") ? o.get("weight").getAsInt() : 10);
            if (gradeIndex(e.grade) < 0) {
                throw new IllegalArgumentException("등급을 알 수 없습니다: " + e.grade);
            }
            if (e.item.isEmpty() == e.entity.isEmpty()) {
                throw new IllegalArgumentException("목록의 한 칸에는 아이템(item)이나 펫(entity) 중 하나만 적어야 합니다");
            }
            return e;
        }
    }

    public static final class Tier {
        public String id;
        /** 등급별 확률 가중치 (합이 100 이 아니어도 비율로 쓴다). */
        public final double[] weights = new double[GRADES.length];
    }

    public String npcName = "행운 상인 럭키";
    /** 뽑기 창의 '티켓 사기' 버튼이 여는 상점. */
    public String shop = "gacha_shop";
    /** 10회 뽑기에서 하나는 반드시 이 등급 이상. "" 이면 보장 없음. */
    public String tenGuarantee = "rare";
    public final Map<String, Tier> tiers = new LinkedHashMap<>();
    public final Map<String, List<Entry>> pools = new LinkedHashMap<>();
    /** 분해하면 받는 조각 / 조각으로 교환할 때 드는 조각 (등급별). */
    public final int[] dismantle = {1, 3, 10, 40, 150};
    public final int[] exchange = {10, 30, 100, 400, 1500};
    public boolean exchangeEnabled = true;
    /** 자동 분류에서 뺄 모드·아이템 (보스 무기 등). */
    public final List<String> excludeMods = new ArrayList<>();
    public final List<String> excludeItems = new ArrayList<>();

    private static GachaConfig instance;

    public static synchronized GachaConfig get() {
        if (instance == null) {
            instance = load(null);
        }
        return instance;
    }

    public static synchronized void reload(MinecraftServer server) {
        instance = load(server);
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("gacha.json");
    }

    public List<Entry> pool(String category) {
        return pools.computeIfAbsent(category, k -> new ArrayList<>());
    }

    // ------------------------------------------------------------------ 기본값

    private static Tier tier(String id, double... w) {
        Tier t = new Tier();
        t.id = id;
        System.arraycopy(w, 0, t.weights, 0, GRADES.length);
        return t;
    }

    public void defaults() {
        tiers.clear();
        // 영웅·전설은 낮게: 일반 뽑기는 희귀까지, 중급은 영웅 1%, 고급은 영웅 5% · 전설 0.5%
        tiers.put("normal", tier("normal", 72, 25, 3, 0, 0));
        tiers.put("mid", tier("mid", 45, 40, 14, 1, 0));
        tiers.put("high", tier("high", 0, 55, 39.5, 5, 0.5));
        excludeMods.clear();
        excludeMods.add("cataclysm"); // L_Ender's Cataclysm: 보스 무기·장신구
        excludeMods.add("tacz"); // 총은 데이터(NBT)로 종류가 갈려서 따로 다루어야 함
        excludeMods.add("simplehats"); // 꾸미기 모자(성능 없음): 장신구 뽑기를 모자로 채우지 않게
        excludeItems.clear();
        for (String s : new String[]{"irons_spellbooks:dev_staff", "irons_spellbooks:necronomicon_spell_book",
                "irons_spellbooks:dragonskin_spell_book", "irons_spellbooks:legendary_spell_book", "minecraft:debug_stick"}) {
            excludeItems.add(s);
        }
    }

    // ------------------------------------------------------------------ 자동 분류

    private boolean excluded(ResourceLocation rl) {
        if (rl == null) {
            return true;
        }
        String path = rl.getPath();
        return excludeMods.contains(rl.getNamespace()) || excludeItems.contains(rl.toString())
                || path.contains("debug") || path.contains("creative") || path.startsWith("dev_") || path.contains("_test")
                || rl.getNamespace().equals(RtShop.MODID);
    }

    /** 점수 순으로 줄 세워 AUTO_SPLIT 비율대로 등급을 매긴다. */
    private static void grade(List<Object[]> scored, List<Entry> out) {
        scored.sort(Comparator.comparingDouble((Object[] a) -> (double) a[1]).thenComparing(a -> ((Entry) a[0]).key()));
        int n = scored.size();
        int idx = 0;
        double acc = 0;
        for (int g = 0; g < GRADES.length; g++) {
            acc += AUTO_SPLIT[g];
            int until = g == GRADES.length - 1 ? n : (int) Math.round(acc * n);
            for (; idx < until && idx < n; idx++) {
                Entry e = (Entry) scored.get(idx)[0];
                e.grade = GRADES[g];
                out.add(e);
            }
        }
    }

    private static int rarityRank(Rarity r) {
        return r == null ? 0 : r.ordinal();
    }

    /** 무기: 공격력(근접) 또는 내구도(활·석궁 등)와 아이템 희귀도로 점수를 매긴다. 보스 무기는 빼는 목록으로 거른다. */
    public List<Entry> autoWeapons() {
        List<Object[]> scored = new ArrayList<>();
        JobConfig jc = JobConfig.get();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (item == Items.AIR || excluded(rl)) {
                continue;
            }
            try {
                String type = WeaponTypes.classify(item, jc.weaponTypes, jc.weaponOverrides);
                if (type.equals(WeaponTypes.NONE) || type.equals("tool") || type.equals("gun")) {
                    continue;
                }
                ItemStack s = new ItemStack(item);
                if (s.getRarity() == Rarity.EPIC) {
                    continue; // 보스·특수 보상 무기 (대부분 EPIC)
                }
                double dmg = 0;
                for (var m : s.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
                    dmg += m.getAmount();
                }
                double score = dmg > 0 ? dmg + 1 : Math.max(1, s.getMaxDamage() / 60.0);
                score += rarityRank(s.getRarity()) * 1.5;
                Entry e = new Entry();
                e.item = rl.toString();
                scored.add(new Object[]{e, score});
            } catch (Throwable ignored) {
                // 이상한 아이템은 건너뛴다
            }
        }
        List<Entry> out = new ArrayList<>();
        grade(scored, out);
        return out;
    }

    /** 장신구: Curios(또는 Accessories) 칸에 끼우는 아이템. 주문서 칸(Iron's Spells)은 뺀다. */
    public List<Entry> autoAccessories() {
        List<Object[]> scored = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (item == Items.AIR || excluded(rl)) {
                continue;
            }
            try {
                boolean slot = false;
                boolean spellbook = false;
                for (TagKey<Item> tag : item.builtInRegistryHolder().tags().toList()) {
                    String ns = tag.location().getNamespace();
                    if (ns.equals("curios") || ns.equals("accessories") || ns.equals("trinkets")) {
                        if (tag.location().getPath().contains("spellbook")) {
                            spellbook = true;
                        } else if (!tag.location().getPath().equals("curio")) {
                            slot = true;
                        }
                    }
                }
                if (!slot || spellbook) {
                    continue;
                }
                ItemStack s = new ItemStack(item);
                if (s.getRarity() == Rarity.EPIC && excludeMods.contains("cataclysm") && rl.getNamespace().equals("cataclysm")) {
                    continue;
                }
                double score = rarityRank(s.getRarity()) * 10 + Math.min(9, s.getMaxDamage() / 100.0);
                Entry e = new Entry();
                e.item = rl.toString();
                scored.add(new Object[]{e, score});
            } catch (Throwable ignored) {
            }
        }
        List<Entry> out = new ArrayList<>();
        grade(scored, out);
        return out;
    }

    /** 펫: 길들일 수 있는 동물(TamableAnimal). 최대 체력으로 점수를 매긴다. */
    public List<Entry> autoPets(MinecraftServer server) {
        List<Object[]> scored = new ArrayList<>();
        if (server == null) {
            return new ArrayList<>();
        }
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (excluded(rl) || type.getCategory() == MobCategory.MONSTER || type.getCategory() == MobCategory.MISC) {
                continue;
            }
            try {
                Entity e = type.create(server.overworld());
                if (e == null) {
                    continue;
                }
                boolean tame = e instanceof TamableAnimal;
                e.discard();
                if (!tame) {
                    continue;
                }
                double hp = 10;
                @SuppressWarnings("unchecked")
                EntityType<? extends LivingEntity> lt = (EntityType<? extends LivingEntity>) type;
                if (DefaultAttributes.hasSupplier(lt)) {
                    hp = DefaultAttributes.getSupplier(lt).getBaseValue(Attributes.MAX_HEALTH);
                }
                Entry en = new Entry();
                en.entity = rl.toString();
                scored.add(new Object[]{en, hp + (rl.getNamespace().equals("minecraft") ? 0 : 5)});
            } catch (Throwable ignored) {
            }
        }
        List<Entry> out = new ArrayList<>();
        grade(scored, out);
        return out;
    }

    public List<Entry> auto(String category, MinecraftServer server) {
        return switch (category) {
            case "weapon" -> autoWeapons();
            case "pet" -> autoPets(server);
            default -> autoAccessories();
        };
    }

    // ------------------------------------------------------------------ 읽기 / 쓰기

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("뽑기 설정. 게임 안에서 /rtshop gacha edit (OP) 또는 뽑기 창의 '편집'으로 고칠 수 있습니다.");
        help.add("등급: common(일반) uncommon(고급) rare(희귀) epic(영웅) legendary(전설).");
        help.add("tiers: 뽑기 단계별 등급 확률(비율). 아이템이 하나도 없는 등급은 빼고 나머지 비율로 뽑습니다.");
        help.add("pools: 뽑기 종류별 목록 {item, count, nbt} 또는 {entity} + grade, weight(같은 등급 안에서의 비율).");
        help.add("tenGuarantee: 10회 뽑기에서 하나는 반드시 이 등급 이상. dismantle/exchange: 등급별 분해 조각 수 / 교환에 드는 조각 수.");
        help.add("excludeMods/excludeItems: 자동 분류에서 뺄 모드·아이템 (보스 무기 등). 뽑기 티켓 가격은 config/rtshop/shops/gacha_shop.json.");
        o.add("_help", help);
        o.addProperty("configVersion", 1);
        o.addProperty("npcName", npcName);
        o.addProperty("shop", shop);
        o.addProperty("tenGuarantee", tenGuarantee);
        JsonObject t = new JsonObject();
        for (Tier tr : tiers.values()) {
            JsonObject w = new JsonObject();
            for (int i = 0; i < GRADES.length; i++) {
                w.addProperty(GRADES[i], tr.weights[i]);
            }
            t.add(tr.id, w);
        }
        o.add("tiers", t);
        JsonObject sh = new JsonObject();
        JsonObject dm = new JsonObject();
        JsonObject ex = new JsonObject();
        for (int i = 0; i < GRADES.length; i++) {
            dm.addProperty(GRADES[i], dismantle[i]);
            ex.addProperty(GRADES[i], exchange[i]);
        }
        sh.add("dismantle", dm);
        sh.add("exchange", ex);
        sh.addProperty("exchangeEnabled", exchangeEnabled);
        o.add("shards", sh);
        JsonArray em = new JsonArray();
        excludeMods.forEach(em::add);
        o.add("excludeMods", em);
        JsonArray ei = new JsonArray();
        excludeItems.forEach(ei::add);
        o.add("excludeItems", ei);
        JsonObject p = new JsonObject();
        for (String c : CATEGORIES) {
            JsonArray a = new JsonArray();
            for (Entry e : pool(c)) {
                a.add(e.toJson());
            }
            p.add(c, a);
        }
        o.add("pools", p);
        return o;
    }

    public static GachaConfig fromJson(JsonObject o) {
        GachaConfig c = new GachaConfig();
        c.defaults();
        if (o.has("npcName")) c.npcName = o.get("npcName").getAsString();
        if (o.has("shop")) c.shop = o.get("shop").getAsString().trim();
        if (o.has("tenGuarantee")) c.tenGuarantee = o.get("tenGuarantee").getAsString().trim();
        if (c.npcName.isBlank()) {
            throw new IllegalArgumentException("NPC 이름이 비어 있습니다");
        }
        if (!c.tenGuarantee.isEmpty() && gradeIndex(c.tenGuarantee) < 0) {
            throw new IllegalArgumentException("10회 보장 등급을 알 수 없습니다: " + c.tenGuarantee);
        }
        if (o.has("tiers")) {
            JsonObject t = o.getAsJsonObject("tiers");
            for (String id : TIERS) {
                if (t.has(id)) {
                    JsonObject w = t.getAsJsonObject(id);
                    Tier tr = new Tier();
                    tr.id = id;
                    double sum = 0;
                    for (int i = 0; i < GRADES.length; i++) {
                        tr.weights[i] = w.has(GRADES[i]) ? w.get(GRADES[i]).getAsDouble() : 0;
                        if (tr.weights[i] < 0) {
                            throw new IllegalArgumentException("확률은 0 이상이어야 합니다 (" + id + ")");
                        }
                        sum += tr.weights[i];
                    }
                    if (sum <= 0) {
                        throw new IllegalArgumentException("'" + id + "' 뽑기의 등급 확률이 모두 0 입니다");
                    }
                    c.tiers.put(id, tr);
                }
            }
        }
        if (o.has("shards")) {
            JsonObject sh = o.getAsJsonObject("shards");
            for (int i = 0; i < GRADES.length; i++) {
                if (sh.has("dismantle") && sh.getAsJsonObject("dismantle").has(GRADES[i])) {
                    c.dismantle[i] = Math.max(0, sh.getAsJsonObject("dismantle").get(GRADES[i]).getAsInt());
                }
                if (sh.has("exchange") && sh.getAsJsonObject("exchange").has(GRADES[i])) {
                    c.exchange[i] = Math.max(1, sh.getAsJsonObject("exchange").get(GRADES[i]).getAsInt());
                }
            }
            if (sh.has("exchangeEnabled")) c.exchangeEnabled = sh.get("exchangeEnabled").getAsBoolean();
        }
        if (o.has("excludeMods")) {
            c.excludeMods.clear();
            for (JsonElement e : o.getAsJsonArray("excludeMods")) {
                c.excludeMods.add(e.getAsString().trim());
            }
        }
        if (o.has("excludeItems")) {
            c.excludeItems.clear();
            for (JsonElement e : o.getAsJsonArray("excludeItems")) {
                c.excludeItems.add(e.getAsString().trim());
            }
        }
        if (o.has("pools")) {
            JsonObject p = o.getAsJsonObject("pools");
            for (String cat : CATEGORIES) {
                List<Entry> l = c.pool(cat);
                if (p.has(cat)) {
                    for (JsonElement e : p.getAsJsonArray(cat)) {
                        l.add(Entry.fromJson(e.getAsJsonObject()));
                    }
                }
            }
        }
        return c;
    }

    public void save() throws java.io.IOException {
        Path p = path();
        Files.createDirectories(p.getParent());
        Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson()), StandardCharsets.UTF_8);
    }

    public String loadError;

    private static GachaConfig load(MinecraftServer server) {
        Path p = path();
        try {
            if (Files.exists(p)) {
                return fromJson(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] gacha.json 을 읽지 못해 뽑기를 잠시 쉽니다 (파일은 그대로 둡니다): {}", e.getMessage());
            GachaConfig c = new GachaConfig();
            c.defaults();
            c.loadError = e.getMessage();
            return c;
        }
        GachaConfig c = new GachaConfig();
        c.defaults();
        if (server == null) {
            return c; // 서버가 켜지기 전: 목록은 서버가 켜질 때 만든다
        }
        for (String cat : CATEGORIES) {
            c.pool(cat).addAll(c.auto(cat, server));
        }
        try {
            c.save();
            RtShop.LOGGER.info("[rtshop] 뽑기 목록을 자동으로 만들었습니다: 무기 {} · 펫 {} · 장신구 {}",
                    c.pool("weapon").size(), c.pool("pet").size(), c.pool("accessory").size());
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] gacha.json 을 만들지 못했습니다", e);
        }
        return c;
    }

    /** 이 이름의 아이템/엔티티가 실제로 있는지 (없는 칸은 뽑기에서 뺀다). */
    public static boolean valid(Entry e) {
        if (!e.entity.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(e.entity);
            return rl != null && ForgeRegistries.ENTITY_TYPES.containsKey(rl);
        }
        ResourceLocation rl = ResourceLocation.tryParse(e.item);
        return rl != null && ForgeRegistries.ITEMS.containsKey(rl) && ForgeRegistries.ITEMS.getValue(rl) != Items.AIR;
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
