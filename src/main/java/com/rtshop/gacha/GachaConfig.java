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
    /** yamato: 무기 뽑기에만 있는 맨 위 등급 (Nightfall 의 야마토 하나를 위한 등급). */
    public static final String[] GRADES = {"common", "uncommon", "rare", "epic", "legendary", "yamato"};
    public static final int YAMATO = 5;
    public static final String[] CATEGORIES = {"weapon", "pet", "accessory"};
    public static final String[] TIERS = {"normal", "mid", "high"};
    /** 자동 분류 때 등급별로 나눌 비율 (성능 순서로 아래부터). */
    private static final double[] AUTO_SPLIT = {0.35, 0.30, 0.22, 0.10, 0.03, 0};

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
    /** 이 등급 이상으로 뽑은 장비는 내구도가 닳지 않는다 (빈 값이면 끔). */
    public String unbreakableFrom = "epic";
    public final Map<String, Tier> tiers = new LinkedHashMap<>();
    public final Map<String, List<Entry>> pools = new LinkedHashMap<>();
    /** 분해하면 받는 조각 (등급별). */
    public final int[] dismantle = {1, 3, 10, 40, 150, 600};
    /** 조각으로 뽑기 티켓 한 장을 바꿀 때 드는 조각 [종류][단계]. 티켓값의 약 9% 를 돌려받는 셈. */
    public final int[][] ticketCost = {{20, 40, 100}, {15, 30, 75}, {15, 30, 75}};
    public boolean exchangeEnabled = true;
    /** 펫 뽑기의 일반·고급 등급 보상(펫 대신 조각). */
    public static final int[] PET_SHARDS = {2, 5};
    public static final int CONFIG_VERSION = 4;
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

    /** 등급 확률 (일반·고급·희귀·영웅·전설). 3.11 에서 낮춤. */
    static final double[][] NEW_ODDS = {{80, 18, 2, 0, 0, 0}, {58, 33, 8.4, 0.6, 0, 0}, {0, 69.96, 26.5, 3.2, 0.3, 0.04}};
    /** 예전 기본 확률 (이 값 그대로면 새 확률로 바꾼다. 직접 고친 확률은 그대로 둔다). */
    static final double[][] OLD_ODDS = {{72, 25, 3, 0, 0, 0}, {45, 40, 14, 1, 0, 0}, {0, 55, 39.5, 5, 0.5, 0}};
    /** 야마토 등급 확률 (고급 뽑기 0.04%). 4판으로 올릴 때 고급 뽑기에 넣는다. */
    public static final double YAMATO_HIGH = 0.04;

    private static Tier tier(String id, double... w) {
        Tier t = new Tier();
        t.id = id;
        System.arraycopy(w, 0, t.weights, 0, GRADES.length);
        return t;
    }

    public void defaults() {
        tiers.clear();
        // 영웅·전설은 낮게: 일반 뽑기는 희귀까지, 중급은 영웅 1%, 고급은 영웅 5% · 전설 0.5%
        tiers.put("normal", tier("normal", NEW_ODDS[0]));
        tiers.put("mid", tier("mid", NEW_ODDS[1]));
        tiers.put("high", tier("high", NEW_ODDS[2]));
        excludeMods.clear();
        excludeMods.add("cataclysm"); // L_Ender's Cataclysm: 보스 무기·장신구
        excludeMods.add("tacz"); // 총은 데이터(NBT)로 종류가 갈려서 따로 다루어야 함
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

    private static void grade(List<Object[]> scored, List<Entry> out) {
        grade(scored, out, AUTO_SPLIT);
    }

    /** 점수 순으로 줄 세워 split 비율대로 등급을 매긴다 (비율 0 인 등급은 건너뜀). */
    private static void grade(List<Object[]> scored, List<Entry> out, double[] split) {
        scored.sort(Comparator.comparingDouble((Object[] a) -> (double) a[1]).thenComparing(a -> ((Entry) a[0]).key()));
        int n = scored.size();
        int idx = 0;
        double acc = 0;
        int lastG = 0;
        for (int g = 0; g < GRADES.length; g++) {
            if (split[g] > 0) {
                lastG = g;
            }
        }
        for (int g = 0; g <= lastG; g++) {
            acc += split[g];
            int until = g == lastG ? n : (int) Math.round(acc * n);
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

    /** 일반~희귀 무기를 가져올 모드 (기본: Epic Fight 의 기본 무기). */
    public final List<String> weaponMods = new ArrayList<>(List.of("epicfight"));
    /** 정해 둔 무기 말고도 영웅·전설 무기를 가져올 모드 (기본: 없음). */
    public final List<String> topWeaponMods = new ArrayList<>();
    /** 등급을 정해 둔 무기 (아이템 ID → 등급). */
    public static final String[] YAMATO_ITEMS = {"efn:yamato_dmc_in_sheath", "efn:yamato_dmc4_in_sheath"};

    static final Map<String, String> FIXED_WEAPONS = new LinkedHashMap<>();

    static {
        // EpicFight Nightfall: HF-무라사마, HF-블레이드, 고대 스컬크 쌍검, 야마토 2종 = 전설
        for (String id : new String[]{"efn:hf_murasama", "efn:hf_blade", "efn:nf_dual_sword"}) {
            FIXED_WEAPONS.put(id, "legendary");
        }
        // 야마토: 전설에서 빼서 야마토 등급(무기 뽑기 전용)으로
        for (String id : YAMATO_ITEMS) {
            FIXED_WEAPONS.put(id, "yamato");
        }
        // 여제의 단검, 룬 단검, 초승달, 야수의 발톱, 선봉의 검, 쿠사비마루, 폐허의 대검 = 영웅
        for (String id : new String[]{"efn:nf_shortsword", "efn:nf_shortsword_2", "efn:crescent_moon", "efn:nf_claw",
                "efn:sword_of_pioneer", "efn:kusabimaru", "efn:ruinsgreatsword"}) {
            FIXED_WEAPONS.put(id, "epic");
        }
    }

    /**
     * Blade of Shadow (TaCZ 총기팩 katana): 모두 영웅 등급.
     * 검 모양 총(tacz:modern_kinetic_gun + GunId)과 LesRaisins 근접 무기(lrtactical:melee + MeleeWeaponId).
     */
    public static final String[] BOS_GUNS = {"aerondight", "dark", "dark_repulsor", "hoshimi", "kirito", "miko", "musou_isshin",
            "ryuichimonji_1", "shard", "tss", "ak47_ellen", "hk416d"};
    public static final String[] BOS_MELEE = {"judgment", "murasama_r", "ryuichimonji", "sfsg", "yehuang_r"};

    public static List<Entry> bladeOfShadow() {
        List<Entry> out = new ArrayList<>();
        for (String g : BOS_GUNS) {
            boolean rifle = g.equals("ak47_ellen") || g.equals("hk416d");
            Entry e = itemEntry("tacz:modern_kinetic_gun", "epic", 10);
            e.nbt = "{GunId:\"katana:" + g + "\",GunFireMode:\"" + (rifle ? "SEMI" : "AUTO") + "\",GunCurrentAmmoCount:" + (rifle ? 30 : 1)
                    + ",HasBulletInBarrel:1b}";
            out.add(e);
        }
        for (String m : BOS_MELEE) {
            Entry e = itemEntry("lrtactical:melee", "epic", 10);
            e.nbt = "{MeleeWeaponId:\"katana:" + m + "\"}";
            out.add(e);
        }
        return out;
    }

    /** 이미 있는 칸(같은 아이템·같은 NBT)은 빼고 Blade of Shadow 무기를 더한다. */
    static void addBladeOfShadow(List<Entry> out) {
        java.util.Set<String> have = new java.util.HashSet<>();
        for (Entry e : out) have.add(e.key());
        for (Entry e : bladeOfShadow()) {
            if (have.add(e.key())) out.add(e);
        }
    }

    /** LesRaisins 근접 무기(lrtactical:melee, 종류는 NBT): 이름에 이 말이 들어 있으면 그 등급. */
    static final String[][] LR_MELEE = {{"karambit", "epic"}, {"baseball", "rare"}, {"bat", "rare"}, {"dagger", "rare"}};

    private static double weaponScore(ItemStack s) {
        double dmg = 0;
        for (var m : s.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            dmg += m.getAmount();
        }
        double score = dmg > 0 ? dmg + 1 : Math.max(1, s.getMaxDamage() / 60.0);
        return score + rarityRank(s.getRarity()) * 1.5;
    }

    /** 이름이 번역되지 않았거나 비어 있는 아이템 = 모드 안에서만 쓰는 형태(칼집에서 뽑은 상태 등). */
    private static boolean internalForm(ItemStack s) {
        String n = s.getHoverName().getString().trim();
        return n.isEmpty() || n.equals(s.getDescriptionId()) || n.startsWith("item.");
    }

    /**
     * 무기: 정해 둔 무기(FIXED_WEAPONS)와 LesRaisins 근접 무기(LR_MELEE)는 그 등급 그대로,
     * 나머지는 weaponMods(기본 Epic Fight)의 기본 무기만 공격력·희귀도 순서로 일반~희귀에. topWeaponMods 의 무기는 영웅·전설에.
     */
    public List<Entry> autoWeapons() {
        List<Object[]> low = new ArrayList<>();
        List<Object[]> top = new ArrayList<>();
        List<Entry> out = new ArrayList<>();
        JobConfig jc = JobConfig.get();
        // 정한 모드가 하나도 없으면 (예: Epic Fight 없이) 모든 무기에서 고른다
        boolean anyBase = weaponMods.stream().anyMatch(m -> net.minecraftforge.fml.ModList.get().isLoaded(m));
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (item == Items.AIR || excluded(rl)) {
                continue;
            }
            try {
                String id = rl.toString();
                ItemStack s = new ItemStack(item);
                String fixed = FIXED_WEAPONS.get(id);
                if (fixed != null) {
                    out.add(itemEntry(id, fixed, fixed.equals("legendary") || fixed.equals("yamato") ? 8 : 10));
                    continue;
                }
                String type = WeaponTypes.classify(item, jc.weaponTypes, jc.weaponOverrides);
                if (type.equals(WeaponTypes.NONE) || type.equals("tool") || type.equals("gun")) {
                    continue;
                }
                if (internalForm(s)) {
                    continue;
                }
                if (topWeaponMods.contains(rl.getNamespace())) {
                    top.add(new Object[]{itemEntry(id, "epic", 10), weaponScore(s)});
                    continue;
                }
                if (anyBase && !weaponMods.contains(rl.getNamespace())) {
                    continue;
                }
                if (s.getRarity() == Rarity.EPIC) {
                    continue; // 보스·특수 보상 무기 (대부분 EPIC)
                }
                low.add(new Object[]{itemEntry(id, "common", 10), weaponScore(s)});
            } catch (Throwable ignored) {
                // 이상한 아이템은 건너뛴다
            }
        }
        // LesRaisins 근접 무기: 하나의 아이템에 NBT 로 종류가 갈린다 → 창작 탭에 나오는 종류마다 한 칸
        for (ItemStack s : variants("lrtactical:melee")) {
            try {
                if (s.getTag() != null && s.getTag().getString("MeleeWeaponId").startsWith("katana:")) {
                    continue; // Blade of Shadow 근접 무기는 아래에서 영웅으로
                }
                String n = (s.getHoverName().getString() + " " + s.getTag()).toLowerCase(Locale.ROOT);
                String g = null;
                for (String[] r : LR_MELEE) {
                    if (n.contains(r[0])) {
                        g = r[1];
                        break;
                    }
                }
                Entry e = itemEntry("lrtactical:melee", g == null ? "common" : g, 10);
                e.nbt = s.getTag() == null ? "" : s.getTag().toString();
                if (g != null) {
                    out.add(e);
                } else {
                    low.add(new Object[]{e, weaponScore(s)});
                }
            } catch (Throwable ignored) {
            }
        }
        grade(low, out, new double[]{0.45, 0.35, 0.20, 0, 0, 0});
        grade(top, out, new double[]{0, 0, 0, 0.75, 0.25, 0});
        addBladeOfShadow(out);
        return out;
    }

    public static List<ItemStack> variantsForDiag(String itemId) {
        return variants(itemId);
    }

    /** 창작 탭에 나오는 그 아이템의 모든 형태 (NBT 로 종류가 갈리는 아이템용). 못 구하면 빈 목록. */
    static List<ItemStack> variants(String itemId) {
        List<ItemStack> out = new ArrayList<>();
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
        if (item == null || item == Items.AIR) {
            return out;
        }
        try {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return out;
            }
            net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(server.getWorldData().enabledFeatures(), true, server.registryAccess());
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (net.minecraft.world.item.CreativeModeTab tab : net.minecraft.world.item.CreativeModeTabs.allTabs()) {
                for (ItemStack s : tab.getDisplayItems()) {
                    if (s.is(item) && s.hasTag() && seen.add(s.getTag().toString())) {
                        out.add(s.copyWithCount(1));
                    }
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] {} 의 종류를 창작 탭에서 읽지 못했습니다: {}", itemId, t.toString());
        }
        return out;
    }

    /** 아이템 ID 로 정해지는 0~1 사이 값 (자동 분류를 다시 해도 같은 결과가 나오도록). */
    private static double hash01(String id) {
        long h = 1125899906842597L;
        for (int i = 0; i < id.length(); i++) {
            h = 31 * h + id.charAt(i);
        }
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static Entry itemEntry(String id, String grade, int weight) {
        Entry e = new Entry();
        e.item = id;
        e.grade = grade;
        e.weight = weight;
        return e;
    }

    /** Artifacts 모드: 전투·생존에 크게 도움이 되는 것은 전설, 나머지는 영웅. */
    static final java.util.Set<String> ARTIFACTS_LEGENDARY = java.util.Set.of(
            "crystal_heart", "power_glove", "vampiric_glove", "fire_gauntlet", "chorus_totem", "cloud_in_a_bottle",
            "feral_claws", "running_shoes", "cross_necklace", "scarf_of_invisibility");
    /** Artifacts 모드 중 장난감·꾸미기에 가까운 것 (영웅 등급 안에서 더 자주). */
    static final java.util.Set<String> ARTIFACTS_LIGHT = java.util.Set.of(
            "villager_hat", "novelty_drinking_hat", "plastic_drinking_hat", "whoopee_cushion", "cowboy_hat", "anglers_hat",
            "superstitious_hat", "umbrella", "kitty_slippers", "bunny_hoppers", "snorkel", "flippers", "lucky_scarf", "onion_ring");

    /**
     * 장신구: Curios(또는 Accessories) 칸에 끼우는 아이템. 주문서 칸(Iron's Spells)은 뺀다.
     *  - Simple Hats 꾸미기 모자: 모자의 기본 등급(일반·고급·희귀·영웅)을 바탕으로 일반·고급·희귀에 섞어 넣는다.
     *  - Artifacts: 영웅·전설.
     *  - 그 밖의 장신구: 일반~희귀 (영웅·전설은 Artifacts 만).
     */
    public List<Entry> autoAccessories() {
        List<Object[]> scored = new ArrayList<>();
        List<Entry> out = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (item == Items.AIR || excluded(rl)) {
                continue;
            }
            try {
                String ns = rl.getNamespace();
                ItemStack s = new ItemStack(item);
                if (ns.equals("simplehats")) {
                    String p = rl.getPath();
                    if (!item.getClass().getSimpleName().startsWith("HatItem") || p.equals("special")) {
                        continue; // 모자만 (모자 주머니·조각·거치대는 빼고)
                    }
                    double r = hash01(rl.toString());
                    int base = rarityRank(s.getRarity()); // 0 일반 1 고급 2 희귀 3 영웅
                    String g = switch (base) {
                        case 0 -> r < 0.75 ? "common" : "uncommon";
                        case 1 -> r < 0.2 ? "common" : r < 0.8 ? "uncommon" : "rare";
                        case 2 -> r < 0.25 ? "uncommon" : "rare";
                        default -> "rare";
                    };
                    out.add(itemEntry(rl.toString(), g, 5));
                    continue;
                }
                boolean slot = false;
                boolean spellbook = false;
                for (TagKey<Item> tag : item.builtInRegistryHolder().tags().toList()) {
                    String tns = tag.location().getNamespace();
                    if (tns.equals("curios") || tns.equals("accessories") || tns.equals("trinkets")) {
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
                if (ns.equals("artifacts")) {
                    String p = rl.getPath();
                    if (ARTIFACTS_LEGENDARY.contains(p)) {
                        out.add(itemEntry(rl.toString(), "legendary", 10));
                    } else {
                        out.add(itemEntry(rl.toString(), "epic", ARTIFACTS_LIGHT.contains(p) ? 14 : 10));
                    }
                    continue;
                }
                if (s.getRarity() == Rarity.EPIC && ns.equals("cataclysm")) {
                    continue;
                }
                double score = rarityRank(s.getRarity()) * 10 + Math.min(9, s.getMaxDamage() / 100.0);
                scored.add(new Object[]{itemEntry(rl.toString(), "common", 10), score});
            } catch (Throwable ignored) {
            }
        }
        grade(scored, out, new double[]{0.40, 0.35, 0.25, 0, 0, 0});
        return out;
    }

    /** Fantasy Mounts: 하늘을 나는 탈것과 불사조(부활)·하늘 고래(6인승)는 전설, 나머지는 영웅. */
    static final java.util.Set<String> MOUNTS_LEGENDARY = java.util.Set.of(
            "phoenix", "frost_drake", "griffin", "pegasus", "sky_whale", "emerald_dragonfly");

    private static boolean isMount(Entity e) {
        for (Class<?> c = e.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getSimpleName().equals("FantasyBeast")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 펫: 길들일 수 있는 동물(TamableAnimal)과 Fantasy Mounts 의 탈것. 펫은 희귀 등급 이상에만 둔다.
     *  - 햄스터(Adorable Hamster Pets) · 작은 용(RPG Companions: Tiny Dragons): 영웅
     *  - Fantasy Mounts: 성능에 따라 영웅·전설 (MOUNTS_LEGENDARY)
     *  - 그 밖의 펫: 희귀
     *  - 일반·고급 등급은 펫 대신 조각 (PET_SHARDS)
     */
    public List<Entry> autoPets(MinecraftServer server) {
        List<Entry> out = new ArrayList<>();
        if (server == null) {
            return out;
        }
        Entry c1 = itemEntry(RtShop.MODID + ":gacha_shard", "common", 10);
        c1.count = PET_SHARDS[0];
        Entry c2 = itemEntry(RtShop.MODID + ":gacha_shard", "uncommon", 10);
        c2.count = PET_SHARDS[1];
        out.add(c1);
        out.add(c2);
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (excluded(rl) || type.getCategory() == MobCategory.MISC) {
                continue;
            }
            try {
                Entity e = type.create(server.overworld());
                if (e == null) {
                    continue;
                }
                boolean tame = e instanceof TamableAnimal;
                boolean mount = isMount(e);
                e.discard();
                if (!tame && !mount) {
                    continue;
                }
                if (!mount && type.getCategory() == MobCategory.MONSTER) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                EntityType<? extends LivingEntity> lt = (EntityType<? extends LivingEntity>) type;
                if (DefaultAttributes.hasSupplier(lt) && DefaultAttributes.getSupplier(lt).getBaseValue(Attributes.MAX_HEALTH) > 1000) {
                    continue; // 소환수·연출용 (체력 수억) 은 펫이 아니다
                }
                String ns = rl.getNamespace();
                String grade;
                if (mount) {
                    grade = MOUNTS_LEGENDARY.contains(rl.getPath()) ? "legendary" : "epic";
                } else if (ns.equals("adorablehamsterpets") || ns.equals("rpg_companions_tiny_dragons") || rl.getPath().contains("hamster")) {
                    grade = "epic";
                } else {
                    grade = "rare";
                }
                Entry en = new Entry();
                en.entity = rl.toString();
                en.grade = grade;
                en.weight = 10;
                out.add(en);
            } catch (Throwable ignored) {
            }
        }
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
        help.add("등급: common(일반) uncommon(고급) rare(희귀) epic(영웅) legendary(전설) yamato(야마토: 무기 뽑기 전용, 맨 위 등급).");
        help.add("tiers: 뽑기 단계별 등급 확률(비율). 아이템이 하나도 없는 등급은 빼고 나머지 비율로 뽑습니다.");
        help.add("pools: 뽑기 종류별 목록 {item, count, nbt} 또는 {entity} + grade, weight(같은 등급 안에서의 비율).");
        help.add("tenGuarantee: 10회 뽑기에서 하나는 반드시 이 등급 이상. shards.dismantle: 등급별 분해 조각 수, shards.tickets: 티켓 한 장과 바꾸는 데 드는 조각 수.");
        help.add("unbreakableFrom: 이 등급 이상으로 뽑은 장비는 내구도가 닳지 않습니다 (기본 epic = 영웅 이상, 빈 값이면 끔).");
        help.add("item 에 rtshop:gacha_shard 를 넣은 칸은 뽑기 표시 없는 조각 보상입니다 (펫 뽑기의 일반·고급 등급).");
        help.add("weaponMods: 자동 분류에서 일반~희귀 무기를 가져올 모드 (기본 epicfight). topWeaponMods: 정해 둔 무기 말고 영웅·전설 무기를 가져올 모드.");
        help.add("excludeMods/excludeItems: 자동 분류에서 뺄 모드·아이템 (보스 무기 등). 뽑기 티켓 가격은 config/rtshop/shops/gacha_shop.json.");
        o.add("_help", help);
        o.addProperty("configVersion", CONFIG_VERSION);
        o.addProperty("npcName", npcName);
        o.addProperty("shop", shop);
        o.addProperty("tenGuarantee", tenGuarantee);
        o.addProperty("unbreakableFrom", unbreakableFrom);
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
        for (int i = 0; i < GRADES.length; i++) {
            dm.addProperty(GRADES[i], dismantle[i]);
        }
        sh.add("dismantle", dm);
        JsonObject tk = new JsonObject();
        for (int c = 0; c < CATEGORIES.length; c++) {
            JsonObject tc = new JsonObject();
            for (int i = 0; i < TIERS.length; i++) {
                tc.addProperty(TIERS[i], ticketCost[c][i]);
            }
            tk.add(CATEGORIES[c], tc);
        }
        sh.add("tickets", tk);
        sh.addProperty("exchangeEnabled", exchangeEnabled);
        o.add("shards", sh);
        JsonArray em = new JsonArray();
        excludeMods.forEach(em::add);
        o.add("excludeMods", em);
        JsonArray ei = new JsonArray();
        excludeItems.forEach(ei::add);
        o.add("excludeItems", ei);
        JsonArray wm = new JsonArray();
        weaponMods.forEach(wm::add);
        o.add("weaponMods", wm);
        JsonArray tm = new JsonArray();
        topWeaponMods.forEach(tm::add);
        o.add("topWeaponMods", tm);
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
        c.version = o.has("configVersion") ? o.get("configVersion").getAsInt() : 1;
        if (o.has("npcName")) c.npcName = o.get("npcName").getAsString().replace("릭키", "럭키"); // 예전 오타
        if (o.has("shop")) c.shop = o.get("shop").getAsString().trim();
        if (o.has("tenGuarantee")) c.tenGuarantee = o.get("tenGuarantee").getAsString().trim();
        if (o.has("unbreakableFrom")) c.unbreakableFrom = o.get("unbreakableFrom").getAsString().trim();
        if (!c.unbreakableFrom.isEmpty() && gradeIndex(c.unbreakableFrom) < 0) {
            throw new IllegalArgumentException("내구도 보호 등급을 알 수 없습니다: " + c.unbreakableFrom);
        }
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
            }
            if (sh.has("tickets")) {
                JsonObject tk = sh.getAsJsonObject("tickets");
                for (int ci = 0; ci < CATEGORIES.length; ci++) {
                    if (tk.has(CATEGORIES[ci])) {
                        JsonObject t = tk.getAsJsonObject(CATEGORIES[ci]);
                        for (int i = 0; i < TIERS.length; i++) {
                            if (t.has(TIERS[i])) {
                                c.ticketCost[ci][i] = Math.max(1, t.get(TIERS[i]).getAsInt());
                            }
                        }
                    }
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
        if (o.has("weaponMods")) {
            c.weaponMods.clear();
            for (JsonElement e : o.getAsJsonArray("weaponMods")) {
                c.weaponMods.add(e.getAsString().trim());
            }
        }
        if (o.has("topWeaponMods")) {
            c.topWeaponMods.clear();
            for (JsonElement e : o.getAsJsonArray("topWeaponMods")) {
                c.topWeaponMods.add(e.getAsString().trim());
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
    /** 읽어 들인 파일의 설정 버전 (예전 버전이면 서버가 켜질 때 목록을 새 규칙으로 다시 만든다). */
    public int version = CONFIG_VERSION;

    private static GachaConfig load(MinecraftServer server) {
        Path p = path();
        GachaConfig old = null;
        try {
            if (Files.exists(p)) {
                old = fromJson(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
                if (old.version >= CONFIG_VERSION || server == null) {
                    return old;
                }
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
        if (old != null) {
            // 예전 버전: 이름·확률·10회 보장·분해 조각·제외 목록은 그대로 두고, 목록만 새 규칙으로 다시 만든다
            c.npcName = old.npcName;
            c.shop = old.shop;
            c.tenGuarantee = old.tenGuarantee;
            c.unbreakableFrom = old.unbreakableFrom;
            c.tiers.clear();
            c.tiers.putAll(old.tiers);
            for (int i = 0; i < TIERS.length; i++) {
                Tier t = c.tiers.get(TIERS[i]);
                if (t == null || java.util.Arrays.equals(t.weights, OLD_ODDS[i])) {
                    c.tiers.put(TIERS[i], tier(TIERS[i], NEW_ODDS[i])); // 기본 확률을 쓰던 서버만 새 (낮춘) 확률로
                }
            }
            System.arraycopy(old.dismantle, 0, c.dismantle, 0, c.dismantle.length);
            if (old.version >= 2) {
                for (int i = 0; i < c.ticketCost.length; i++) {
                    System.arraycopy(old.ticketCost[i], 0, c.ticketCost[i], 0, c.ticketCost[i].length);
                }
            }
            c.exchangeEnabled = old.exchangeEnabled;
            c.excludeItems.clear();
            c.excludeItems.addAll(old.excludeItems);
            for (String m : old.excludeMods) {
                if (!m.equals("simplehats") && !c.excludeMods.contains(m)) {
                    c.excludeMods.add(m);
                }
            }
            try {
                Path backup = p.getParent().resolve("backup");
                Files.createDirectories(backup);
                Files.copy(p, backup.resolve("gacha_v" + old.version + "_" + System.currentTimeMillis() + ".json"));
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] 예전 gacha.json 을 백업하지 못했습니다: {}", e.getMessage());
            }
        }
        if (old != null && old.version >= 3) {
            // 3판 → 4판: 목록은 그대로 두고 (직접 고친 것 보존) 야마토 등급과 Blade of Shadow 만 더한다
            for (String cat : CATEGORIES) c.pool(cat).addAll(old.pool(cat));
            upgradeTo4(c);
            try {
                c.save();
                RtShop.LOGGER.info("[rtshop] 뽑기 4판: 야마토 등급과 Blade of Shadow 무기를 더했습니다");
            } catch (Exception e) {
                RtShop.LOGGER.error("[rtshop] gacha.json 을 저장하지 못했습니다", e);
            }
            return c;
        }
        for (String cat : CATEGORIES) {
            if (old != null && old.version >= 2 && !cat.equals("weapon")) {
                c.pool(cat).addAll(old.pool(cat)); // 2판 → 3판: 바뀐 것은 무기 규칙뿐, 펫·장신구 목록은 그대로
            } else {
                c.pool(cat).addAll(c.auto(cat, server));
            }
        }
        try {
            c.save();
            RtShop.LOGGER.info("[rtshop] 뽑기 목록을 {}: 무기 {} · 펫 {} · 장신구 {}", old == null ? "자동으로 만들었습니다" : "새 규칙으로 다시 만들었습니다",
                    c.pool("weapon").size(), c.pool("pet").size(), c.pool("accessory").size());
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] gacha.json 을 만들지 못했습니다", e);
        }
        return c;
    }

    public static void upgradeTo4ForTest(GachaConfig c) {
        upgradeTo4(c);
    }

    /** 4판: 야마토는 전설 → 야마토 등급, 고급 뽑기에 야마토 확률, Blade of Shadow 무기는 영웅으로 추가. */
    static void upgradeTo4(GachaConfig c) {
        List<Entry> w = c.pool("weapon");
        boolean hasYamato = false;
        for (Entry e : w) {
            if (java.util.Arrays.asList(YAMATO_ITEMS).contains(e.item)) {
                e.grade = "yamato";
                hasYamato = true;
            }
        }
        if (!hasYamato) {
            for (String id : YAMATO_ITEMS) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null && ForgeRegistries.ITEMS.containsKey(rl)) w.add(itemEntry(id, "yamato", 8));
            }
        }
        addBladeOfShadow(w);
        Tier high = c.tiers.get("high");
        if (high != null && high.weights[YAMATO] <= 0) {
            high.weights[YAMATO] = YAMATO_HIGH;
            if (Math.abs(high.weights[1] - 70) < 1e-9) high.weights[1] = 69.96; // 기본 확률을 쓰던 서버는 합이 100 이 되게
        }
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
