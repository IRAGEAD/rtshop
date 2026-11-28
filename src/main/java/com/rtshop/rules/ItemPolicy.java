package com.rtshop.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.shop.Pricing;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 아이템을 어디서 얻는가의 규칙 (config/rtshop/item_policy.json).
 *  - 상점: shopBan 의 아이템, 보스와 얽힌 아이템, 총 모양 무기는 NPC 상점에 오르지 않는다 (플레이어 상점은 제한 없음).
 *  - 값: priceFactor 의 묶음은 상점가를 곱한다.
 *  - 전리품: lootBan 의 아이템은 상자·몹·낚시 어디서도 나오지 않는다. 모드 장비는 상점에서만 (보스의 전리품과 보스 아이템은 그대로).
 *  - 조합법: recipeBan, 지팡이, 상점·뽑기에 오른 장비, 모드 장비는 만들 수 없다 (바닐라 장비와 보스 아이템은 그대로).
 * 적는 법: "모드:아이템", "모드:*", 또는 * 가 낀 무늬 ("irons_spellbooks:*_ink").
 */
public final class ItemPolicy {
    private ItemPolicy() {
    }

    public static final class Config {
        /** 설정 파일의 판. 2 (3.53): Better Nether 도 상점 금지, 전리품 금지의 "*fruit*:*" 자리에 "spmovesets:*". 3 (3.58): Create 의 조합법·전리품·블록 드랍 금지.
         *  4 (3.58.1): Create 는 상점(shopBan)과 뽑기(gachaBan)에도 오르지 않는다.
         *  5 (3.66.0): Better Nether 의 모든 아이템 — 조합법 · 전리품 · 몹/블록 드롭 금지 (상점은 2판부터 금지).
         *  6 (3.78.0): spmovesets 의 모든 아이템 — NPC 상점 · 뽑기 · 조합법 · 전리품 · 몹/블록 드롭 금지 (사용자 "spmovesets 의 모든 아이템들을 상점에서 제거하고
         *     드랍과 조합과 같은 얻는 방식도 모두 금지"). 전리품은 2판부터 막혀 있었다.
         *  7 (3.80.0): 상점의 무기 · 장비 값 올리기 (gearRaise*) — 파일에 새 칸을 적는다.
         *  8 (3.82.0): 주민 거래에서 spmovesets 를 뺀다 (tradeBan).
         *  9 (3.84.0, 사용자 11-29 "새로 추가한 모드 아이템(nightfall enhance, smallships) 드랍, 전리품, 조합 금지" ·
         *     "epic fight와 관련된 모든 스킬북들을 제작하지 못하도록" · "aquamirae 관련 장비를 상점에서 삭제"):
         *     Nightfall Enhance · Small Ships 의 조합법 · 전리품 · 몹/블록 드롭 금지, Epic Fight 스킬북(epicfight:skillbook) 조합 금지,
         *     Aquamirae 의 장비(무기 · 방어구 · 도구 · 장신구)는 NPC 상점에 오르지 않는다 (shopBanGearMods). */
        public static final int VERSION = 9;
        /** 읽어 온 파일이 예전 판이라 고쳐 썼는가 (다시 저장한다). */
        transient boolean migrated;
        public boolean enabled = true;
        public final Set<String> shopBan = new LinkedHashSet<>();
        public boolean shopBanBossItems = true;
        public boolean shopBanGuns = true;
        public final Set<String> gunWords = new LinkedHashSet<>();
        public final Map<String, Double> priceFactor = new LinkedHashMap<>();
        public final Set<String> bossMods = new LinkedHashSet<>();
        public final Set<String> bossItems = new LinkedHashSet<>();
        public final Set<String> notBossItems = new LinkedHashSet<>();
        public boolean sampleBossDrops = true;
        public final Set<String> lootBan = new LinkedHashSet<>();
        public final Set<String> lootAllow = new LinkedHashSet<>();
        /** 블록을 캤을 때도 나오지 않는 아이템 (던전에 장식으로 놓인 Create 블록을 캐서 얻지 못하게). */
        public final Set<String> blockDropBan = new LinkedHashSet<>();
        /** 뽑기에 오르지 않는 것: 무기·장신구·펫 묶음의 아이템(엔티티) ID 와 마법 뽑기의 인챈트 ID. */
        public final Set<String> gachaBan = new LinkedHashSet<>();
        public final Set<String> recipeBan = new LinkedHashSet<>();
        public final Set<String> recipeBanMods = new LinkedHashSet<>();
        /** 3.82: 주민 · 떠돌이 상인의 거래에 오르면 안 되는 아이템 (파는 것 · 값 모두, "모드:*" 는 NBT 에 그 모드가 적힌 것도). */
        public final Set<String> tradeBan = new LinkedHashSet<>();
        /** 3.84: 이 모드들의 장비(무기 · 방어구 · 방패 · 활 · 도구 · 장신구)는 NPC 상점에 오르지 않는다 (재료 · 음식은 그대로). */
        public final Set<String> shopBanGearMods = new LinkedHashSet<>();
        public boolean recipeBanStaffs = true;
        public final Set<String> staffWords = new LinkedHashSet<>();
        public boolean listStaffs = true;
        public String staffShop = "blacksmith";
        public String staffCategory = "weapon";
        public int staffPrice = 6000;
        public boolean recipeBanListedGear = true;
        public boolean modGearShopOnly = true;
        /** 상점 금지(shopBan) 모드의 장비는 "상점에서만" 규칙에서 빼서, 그 모드의 조합법과 전리품을 그대로 둔다. */
        public boolean shopBannedKeepOwnWays = true;
        public boolean vanillaGearToo = false;
        /**
         * 터무니없이 센 장비 (3.65.0): 공격력이 extremeAttack 이상이거나 갑옷 한 조각의 방어력이 extremeArmor 이상인 장비는
         * 전리품(상자 · 몹 · 보스) · 몹 드롭 · 조합법에서 뺀다 (keep · lootAllow 에 적어도). 0 = 끔.
         * 모드팩에서 Arcane Vortex 의 공격력 수백 ~ 무한 무기 · 방어력 21억 갑옷이 여기에 걸린다. 다른 장비는 공격력 17 · 방어력 13 이 가장 높다.
         */
        public double extremeAttack = 25;
        public double extremeArmor = 20;
        /**
         * 상점의 무기 · 장비 값 올리기 (3.80.0, 사용자 11-28 13:35 "전체적인 상점의 무기와 장비의 값을 올리는 것이 좋을 것 같아. 대신 돈이 불어나거나 그래선 안돼" → 다안):
         * 근접 무기 · 활 · 석궁 · 지팡이 · 방패 · 방어구 · 장신구의 사는 값에, 올리기 전의 값에 따라 곱한다 — gearRaiseMidFrom G 미만은 gearRaiseLow 배,
         * gearRaiseHighFrom G 미만은 gearRaiseMid 배, 그 이상은 gearRaiseHigh 배. 초당 피해(DPS)가 gearRaiseStrongDps 이상인 근접 무기는 값과 상관없이 gearRaiseHigh 배.
         * 상점이 사 주는 값(매입가)은 그대로라 돈이 불어나지 않는다. 곡괭이 · 삽 · 괭이 · 낚싯대 · 블록은 빼고, priceFactor 묶음은 둘 중 큰 배수만.
         */
        public boolean gearRaise = true;
        public double gearRaiseLow = 2, gearRaiseMid = 3, gearRaiseHigh = 5;
        public int gearRaiseMidFrom = 5000, gearRaiseHighFrom = 40000;
        public double gearRaiseStrongDps = 14;
        public final Set<String> keep = new LinkedHashSet<>();
        /** 몬스터를 잡으면 따로 굴리는 드랍 (Iron's Spells 의 잉크): 아이템 → 확률(0~1). */
        public final Map<String, Double> mobDrops = new LinkedHashMap<>();
        public double bossDropFactor = 10;

        /** 3.84: 조합 · 전리품 · 드롭을 막은 새 모드 (Nightfall Enhance, Small Ships). */
        public static final List<String> NEW_MOD_IDS_384 = List.of("efn_enhance", "smallships");
        public static final List<String> NEW_MODS_384 = List.of("efn_enhance:*", "smallships:*");
        /** 3.84: 만들 수 없는 Epic Fight 스킬북 (스킬은 NBT 에 적힌다 — 어느 스킬이든 이 아이템이다). */
        public static final List<String> SKILL_BOOKS_384 = List.of("epicfight:skillbook");

        public static Config defaults() {
            Config c = new Config();
            c.shopBan.addAll(List.of("arcanevortex:*", "betternether:*", "efn:*", "rtshop:enhance_anvil", "create:*", "spmovesets:*"));
            c.gachaBan.addAll(List.of("create:*", "spmovesets:*"));
            c.gunWords.addAll(List.of("gun", "pistol", "rifle", "revolver", "musket", "shotgun", "blaster", "bullet"));
            c.priceFactor.put("wom:*", 5.0);
            c.bossMods.addAll(List.of("cataclysm", "fdbosses", "block_factorys_bosses", "witherreincarnated", "enderdragonplus"));
            c.bossItems.addAll(List.of("minecraft:nether_star", "minecraft:dragon_egg", "minecraft:dragon_head"));
            c.lootBan.addAll(List.of("spmovesets:*", "artifacts:*", "relics_rpgs:*", "jewelry:*", "simplehats:*",
                    "adorablehamsterpets:*", "crittersandcompanions:*", "rpg_companions_tiny_dragons:*", "fantasymounts:*",
                    "efn:*", "tacz:*", "lrtactical:*", "create:*", "betternether:*"));
            c.lootBan.addAll(NEW_MODS_384);
            c.blockDropBan.addAll(List.of("create:*", "betternether:*", "spmovesets:*"));
            c.blockDropBan.addAll(NEW_MODS_384);
            c.shopBanGearMods.add("aquamirae");
            c.tradeBan.add("spmovesets:*");   // 3.82: 지도 제작자의 원소 지도(값: 에메랄드 + 뽑기 열쇠) 등
            c.lootAllow.addAll(List.of("irons_spellbooks:*_ink", "irons_spellbooks:ink_*"));
            c.recipeBan.addAll(List.of("tacz:*", "lrtactical:*", "rtshop:enhance_anvil", "create:*", "betternether:*", "spmovesets:*"));
            c.recipeBan.addAll(NEW_MODS_384);
            c.recipeBan.addAll(SKILL_BOOKS_384);
            c.recipeBanMods.addAll(List.of("tacz", "create", "betternether", "spmovesets"));
            c.recipeBanMods.addAll(NEW_MOD_IDS_384);
            c.staffWords.addAll(List.of("staff", "wand", "scepter", "sceptre"));
            c.mobDrops.put("irons_spellbooks:common_ink", 0.03);
            c.mobDrops.put("irons_spellbooks:uncommon_ink", 0.012);
            c.mobDrops.put("irons_spellbooks:rare_ink", 0.005);
            c.mobDrops.put("irons_spellbooks:epic_ink", 0.0015);
            c.mobDrops.put("irons_spellbooks:legendary_ink", 0.0003);
            return c;
        }

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            JsonArray help = new JsonArray();
            help.add("아이템을 어디서 얻는가의 규칙. 적는 법: \"모드:아이템\", \"모드:*\", * 가 낀 무늬(\"irons_spellbooks:*_ink\"). 고친 뒤 /rtshop reload.");
            help.add("shopBan: NPC 상점에 오르지 않는 아이템 (플레이어 상점은 제한 없음). shopBanBossItems: 보스와 얽힌 아이템도 팔지 않음. shopBanGuns + gunWords: 이름에 이 낱말이 든 무기(총)는 팔지 않음.");
            help.add("priceFactor: 이 묶음의 상점가에 곱하는 수 (예: \"wom:*\": 5).");
            help.add("보스 아이템 = bossMods 의 모든 아이템 + bossItems + 보스가 떨어뜨리는 그 모드의 아이템(sampleBossDrops) + 그것으로만 만드는 물건. notBossItems 는 뺀다.");
            help.add("lootBan: 상자·몹·낚시 전리품에서 빠지는 아이템. lootAllow: 그래도 나오는 것 (잉크). mobDrops: 몬스터를 잡으면 따로 굴리는 드랍(확률 0~1), 보스는 bossDropFactor 배.");
            help.add("blockDropBan: 블록을 캤을 때도 나오지 않는 아이템 (Create: 던전의 건축물에 놓인 블록을 캐서 얻지 못하게). 비우면 캐서 얻을 수 있다.");
            help.add("어떤 모드의 아이템을 아예 얻지 못하게 하려면 (Better Nether · spmovesets 처럼): shopBan · gachaBan · recipeBan 에 \"모드:*\", recipeBanMods 에 \"모드\", lootBan · blockDropBan 에 \"모드:*\". 이미 가진 것은 그대로 남는다.");
            help.add("gachaBan: 뽑기에 오르지 않는 것 — 무기·장신구·펫 묶음의 아이템 ID 와 마법 뽑기의 인챈트 ID (Create: 상점·뽑기 어디에서도 얻지 못하게). gacha.json 의 목록은 지우지 않고 건너뛰기만 한다.");
            help.add("recipeBan / recipeBanMods: 만들 수 없는 아이템 / 그 모드의 조합법 전부. recipeBanStaffs + staffWords: 지팡이는 만들 수 없고 상점에서 삼 (listStaffs: 팔지 않는 지팡이를 staffShop 에 올림).");
            help.add("extremeAttack / extremeArmor (3.65.0): 공격력이 이 값 이상이거나 갑옷 한 조각의 방어력이 이 값 이상인 터무니없는 장비는 전리품 · 몹과 보스의 드롭 · 조합법에서 뺀다 (keep · lootAllow 보다 먼저). 0 = 끔. 기본 25 / 20 (모드팩의 다른 장비는 17 / 13 이 가장 높다).");
            help.add("shopBanGearMods (3.84): 이 모드들의 장비(무기 · 방어구 · 방패 · 활 · 도구 · 장신구)는 NPC 상점에 오르지 않는다 — 재료 · 음식 같은 다른 물건은 그대로. 기본: aquamirae.");
            help.add("tradeBan (3.82): 주민 · 떠돌이 상인의 거래에서 뺄 아이템 — 파는 것이든 값이든 이 아이템이 들어간 거래는 거래 창이 열리기 전에 빠진다. \"모드:*\" 는 NBT 에 그 모드의 ID 나 번역 키가 적힌 물건(모드가 만든 지도 등)도. 기본: spmovesets 전부.");
            help.add("shopBannedKeepOwnWays: shopBan 에 적힌 모드의 장비는 '상점에서만' 규칙에서 빠진다 (조합법·전리품 그대로). 전리품까지 막으려면 lootBan 에도 적는다.");
            help.add("recipeBanListedGear: 상점·뽑기에 오른 무기·장비는 만들 수 없음. modGearShopOnly: 모드의 무기·방어구·도구·장신구는 상점(뽑기)에서만 — 조합법과 전리품에서 뺌. vanillaGearToo: 바닐라 장비에도 적용. keep: 예외로 둘 아이템.");
            help.add("gearRaise (3.80): 상점의 무기 · 활 · 지팡이 · 방패 · 방어구 · 장신구의 사는 값을 올린다 — 올리기 전 값이 gearRaiseMidFrom G 미만이면 gearRaiseLow 배, gearRaiseHighFrom G 미만이면 gearRaiseMid 배, 그 이상은 gearRaiseHigh 배. "
                    + "초당 피해(DPS)가 gearRaiseStrongDps 이상인 근접 무기는 값과 상관없이 gearRaiseHigh 배. 상점이 사 주는 값(매입가)은 그대로. 도구 · 낚싯대는 빼고, priceFactor 와 겹치면 큰 쪽. false 면 끔.");
            o.add("_help", help);
            o.addProperty("version", VERSION);
            o.addProperty("enabled", enabled);
            o.add("shopBan", arr(shopBan));
            o.add("tradeBan", arr(tradeBan));
            o.add("shopBanGearMods", arr(shopBanGearMods));
            o.addProperty("shopBanBossItems", shopBanBossItems);
            o.addProperty("shopBanGuns", shopBanGuns);
            o.add("gunWords", arr(gunWords));
            JsonObject pf = new JsonObject();
            priceFactor.forEach(pf::addProperty);
            o.add("priceFactor", pf);
            o.add("bossMods", arr(bossMods));
            o.add("bossItems", arr(bossItems));
            o.add("notBossItems", arr(notBossItems));
            o.addProperty("sampleBossDrops", sampleBossDrops);
            o.add("lootBan", arr(lootBan));
            o.add("lootAllow", arr(lootAllow));
            o.add("gachaBan", arr(gachaBan));
            o.add("blockDropBan", arr(blockDropBan));
            JsonObject md = new JsonObject();
            mobDrops.forEach(md::addProperty);
            o.add("mobDrops", md);
            o.addProperty("bossDropFactor", bossDropFactor);
            o.add("recipeBan", arr(recipeBan));
            o.add("recipeBanMods", arr(recipeBanMods));
            o.addProperty("recipeBanStaffs", recipeBanStaffs);
            o.add("staffWords", arr(staffWords));
            o.addProperty("listStaffs", listStaffs);
            o.addProperty("staffShop", staffShop);
            o.addProperty("staffCategory", staffCategory);
            o.addProperty("staffPrice", staffPrice);
            o.addProperty("recipeBanListedGear", recipeBanListedGear);
            o.addProperty("modGearShopOnly", modGearShopOnly);
            o.addProperty("shopBannedKeepOwnWays", shopBannedKeepOwnWays);
            o.addProperty("vanillaGearToo", vanillaGearToo);
            o.addProperty("extremeAttack", extremeAttack);
            o.addProperty("extremeArmor", extremeArmor);
            o.addProperty("gearRaise", gearRaise);
            o.addProperty("gearRaiseLow", gearRaiseLow);
            o.addProperty("gearRaiseMid", gearRaiseMid);
            o.addProperty("gearRaiseHigh", gearRaiseHigh);
            o.addProperty("gearRaiseMidFrom", gearRaiseMidFrom);
            o.addProperty("gearRaiseHighFrom", gearRaiseHighFrom);
            o.addProperty("gearRaiseStrongDps", gearRaiseStrongDps);
            o.add("keep", arr(keep));
            return o;
        }

        public static Config fromJson(JsonObject o) {
            Config d = defaults();
            Config c = new Config();
            c.enabled = bool(o, "enabled", true);
            strings(o, "shopBan", c.shopBan, d.shopBan);
            strings(o, "tradeBan", c.tradeBan, d.tradeBan);
            strings(o, "shopBanGearMods", c.shopBanGearMods, v0(o) < 9 ? Set.<String>of() : d.shopBanGearMods);
            c.shopBanBossItems = bool(o, "shopBanBossItems", true);
            c.shopBanGuns = bool(o, "shopBanGuns", true);
            strings(o, "gunWords", c.gunWords, d.gunWords);
            numbers(o, "priceFactor", c.priceFactor, d.priceFactor);
            strings(o, "bossMods", c.bossMods, d.bossMods);
            strings(o, "bossItems", c.bossItems, d.bossItems);
            strings(o, "notBossItems", c.notBossItems, d.notBossItems);
            c.sampleBossDrops = bool(o, "sampleBossDrops", true);
            strings(o, "lootBan", c.lootBan, d.lootBan);
            strings(o, "lootAllow", c.lootAllow, d.lootAllow);
            strings(o, "blockDropBan", c.blockDropBan, v0(o) < 3 ? Set.<String>of() : d.blockDropBan);
            numbers(o, "mobDrops", c.mobDrops, d.mobDrops);
            if (o.has("bossDropFactor")) c.bossDropFactor = Math.max(0, o.get("bossDropFactor").getAsDouble());
            strings(o, "recipeBan", c.recipeBan, d.recipeBan);
            strings(o, "recipeBanMods", c.recipeBanMods, d.recipeBanMods);
            c.recipeBanStaffs = bool(o, "recipeBanStaffs", true);
            strings(o, "staffWords", c.staffWords, d.staffWords);
            c.listStaffs = bool(o, "listStaffs", true);
            if (o.has("staffShop")) c.staffShop = o.get("staffShop").getAsString().trim();
            if (o.has("staffCategory")) c.staffCategory = o.get("staffCategory").getAsString().trim();
            if (o.has("staffPrice")) c.staffPrice = Math.max(1, o.get("staffPrice").getAsInt());
            c.recipeBanListedGear = bool(o, "recipeBanListedGear", true);
            c.modGearShopOnly = bool(o, "modGearShopOnly", true);
            c.shopBannedKeepOwnWays = bool(o, "shopBannedKeepOwnWays", true);
            c.vanillaGearToo = bool(o, "vanillaGearToo", false);
            c.extremeAttack = o.has("extremeAttack") && o.get("extremeAttack").isJsonPrimitive() ? o.get("extremeAttack").getAsDouble() : d.extremeAttack;
            c.extremeArmor = o.has("extremeArmor") && o.get("extremeArmor").isJsonPrimitive() ? o.get("extremeArmor").getAsDouble() : d.extremeArmor;
            strings(o, "keep", c.keep, d.keep);
            strings(o, "gachaBan", c.gachaBan, d.gachaBan);
            c.gearRaise = bool(o, "gearRaise", d.gearRaise);
            c.gearRaiseLow = num(o, "gearRaiseLow", d.gearRaiseLow);
            c.gearRaiseMid = num(o, "gearRaiseMid", d.gearRaiseMid);
            c.gearRaiseHigh = num(o, "gearRaiseHigh", d.gearRaiseHigh);
            c.gearRaiseMidFrom = (int) num(o, "gearRaiseMidFrom", d.gearRaiseMidFrom);
            c.gearRaiseHighFrom = (int) num(o, "gearRaiseHighFrom", d.gearRaiseHighFrom);
            c.gearRaiseStrongDps = num(o, "gearRaiseStrongDps", d.gearRaiseStrongDps);
            int v = v0(o);
            if (v < 9) {   // 3.84.0: Nightfall Enhance · Small Ships 는 만들거나 줍거나 캐서 얻지 못한다, Epic Fight 스킬북은 만들 수 없다, Aquamirae 장비는 상점에 없다
                c.lootBan.addAll(NEW_MODS_384);
                c.blockDropBan.addAll(NEW_MODS_384);
                c.recipeBan.addAll(NEW_MODS_384);
                c.recipeBan.addAll(SKILL_BOOKS_384);
                c.recipeBanMods.addAll(NEW_MOD_IDS_384);
                c.shopBanGearMods.add("aquamirae");
                c.migrated = true;
            }
            if (v < 8) {   // 3.82.0: spmovesets 가 주민(지도 제작자)에게 붙인 거래를 뺀다 (운영자가 적은 다른 줄은 그대로)
                c.tradeBan.add("spmovesets:*");
                c.migrated = true;
            }
            if (v < 7) c.migrated = true;   // 3.80.0: 무기 · 장비 값 올리기의 새 칸을 파일에 적는다 (켜짐)
            if (v < 6) {   // 3.78.0: spmovesets 의 아이템은 상점 · 뽑기에 오르지 않고, 만들거나 줍거나 캐서 얻지도 못한다 (운영자가 적은 다른 줄은 그대로)
                c.shopBan.add("spmovesets:*");
                c.gachaBan.add("spmovesets:*");
                c.recipeBanMods.add("spmovesets");
                c.recipeBan.add("spmovesets:*");
                c.lootBan.add("spmovesets:*");
                c.blockDropBan.add("spmovesets:*");
                c.migrated = true;
            }
            if (v < 5) {   // 3.66.0: Better Nether 의 아이템은 만들거나 줍거나 캐서 얻지 못한다 (조합으로 만드는 무기가 뽑기 전설 무기보다 셌다)
                c.recipeBanMods.add("betternether");
                c.recipeBan.add("betternether:*");
                c.lootBan.add("betternether:*");
                c.blockDropBan.add("betternether:*");
                c.migrated = true;
            }
            if (v < 4) {   // 3.58.1: Create 전용 아이템은 상점과 뽑기에도 오르지 않는다 (지팡이로 올라간 대칭의 지팡이, 장신구 묶음의 고글, 마법 뽑기의 인챈트 2종)
                c.shopBan.add("create:*");
                c.gachaBan.add("create:*");
                c.migrated = true;
            }
            if (v < 3) {   // 3.58: Create 는 던전 모드(IDAS)가 건축물에 쓰는 것만 남기고, 만들거나 줍거나 캐서 얻는 길을 막는다
                c.recipeBanMods.add("create");
                c.recipeBan.add("create:*");
                c.lootBan.add("create:*");
                c.blockDropBan.add("create:*");
                c.migrated = true;
            }
            if (v < 2) {   // 3.53: 요청대로 기본 규칙을 고친다 (직접 넣은 다른 줄은 그대로)
                c.shopBan.add("betternether:*");
                if (c.lootBan.remove("*fruit*:*") | !c.lootBan.contains("spmovesets:*")) c.lootBan.add("spmovesets:*");
                c.migrated = true;
            }
            return c;
        }

        private static int v0(JsonObject o) {
            return o.has("version") ? o.get("version").getAsInt() : 1;
        }

        private static JsonArray arr(Collection<String> c) {
            JsonArray a = new JsonArray();
            c.forEach(a::add);
            return a;
        }

        private static boolean bool(JsonObject o, String k, boolean def) {
            return o.has(k) ? o.get(k).getAsBoolean() : def;
        }

        /** 0 이상의 수 (없거나 숫자가 아니면 def). */
        private static double num(JsonObject o, String k, double def) {
            return o.has(k) && o.get(k).isJsonPrimitive() ? Math.max(0, o.get(k).getAsDouble()) : def;
        }

        private static void strings(JsonObject o, String k, Set<String> into, Set<String> def) {
            if (!o.has(k)) {
                into.addAll(def);
                return;
            }
            for (JsonElement e : o.getAsJsonArray(k)) {
                String s = e.getAsString().trim();
                if (!s.isEmpty()) into.add(s);
            }
        }

        private static void numbers(JsonObject o, String k, Map<String, Double> into, Map<String, Double> def) {
            if (!o.has(k)) {
                into.putAll(def);
                return;
            }
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(k).entrySet()) {
                into.put(e.getKey().trim(), Math.max(0, e.getValue().getAsDouble()));
            }
        }
    }

    /** 아이템 ID 무늬의 묶음. */
    public static final class Pat {
        private final Set<String> exact = new HashSet<>();
        private final Set<String> spaces = new HashSet<>();
        private final List<Pattern> wild = new ArrayList<>();

        public Pat(Collection<String> src) {
            for (String raw : src) {
                String s = raw.trim().toLowerCase(Locale.ROOT);
                if (s.isEmpty()) continue;
                if (s.indexOf('*') < 0) {
                    exact.add(s);
                } else if (s.endsWith(":*") && s.indexOf('*') == s.length() - 1) {
                    spaces.add(s.substring(0, s.length() - 2));
                } else {
                    StringBuilder b = new StringBuilder();
                    String[] parts = s.split("\\*", -1);
                    for (int i = 0; i < parts.length; i++) {
                        if (i > 0) b.append(".*");
                        if (!parts[i].isEmpty()) b.append(Pattern.quote(parts[i]));
                    }
                    wild.add(Pattern.compile(b.toString()));
                }
            }
        }

        public boolean hit(ResourceLocation id) {
            if (id == null) return false;
            if (spaces.contains(id.getNamespace())) return true;
            String s = id.toString();
            if (exact.contains(s)) return true;
            for (Pattern p : wild) if (p.matcher(s).matches()) return true;
            return false;
        }

        public boolean empty() {
            return exact.isEmpty() && spaces.isEmpty() && wild.isEmpty();
        }
    }

    /** 한 번 읽은 규칙과 거기서 셈한 것. 바꿀 때는 통째로 갈아 끼운다 (전리품은 다른 스레드에서도 묻는다). */
    private static final class State {
        final Config c;
        final Pat shopBan, bossItems, notBoss, lootBan, lootAllow, recipeBan, keep, blockDropBan, gachaBan, tradeBan;
        /** tradeBan 의 "모드:*" 에서 뽑은 "모드:" 와 ".모드." (번역 키) — NBT 글에 들어 있으면 그 모드의 물건으로 본다. */
        final List<String> tradeBanTags = new ArrayList<>();
        final List<Map.Entry<Pat, Double>> factors = new ArrayList<>();
        volatile Set<Item> boss = Set.of();
        volatile Set<Item> listed = Set.of();
        final Map<Item, Boolean> gear = new ConcurrentHashMap<>(), staff = new ConcurrentHashMap<>(), gun = new ConcurrentHashMap<>(),
                extreme = new ConcurrentHashMap<>();

        State(Config c) {
            this.c = c;
            shopBan = new Pat(c.shopBan);
            bossItems = new Pat(c.bossItems);
            notBoss = new Pat(c.notBossItems);
            lootBan = new Pat(c.lootBan);
            lootAllow = new Pat(c.lootAllow);
            blockDropBan = new Pat(c.blockDropBan);
            gachaBan = new Pat(c.gachaBan);
            tradeBan = new Pat(c.tradeBan);
            for (String t : c.tradeBan) {
                String v = t.trim().toLowerCase(Locale.ROOT);
                if (v.endsWith(":*") && v.indexOf('*') == v.length() - 1 && v.length() > 2) {
                    String ns = v.substring(0, v.length() - 2);
                    tradeBanTags.add(ns + ":");          // 아이템 · 구조물 ID (모드:이름)
                    tradeBanTags.add("." + ns + ".");    // 번역 키 (예: 예전 판의 아레나 지도 이름 filled_map.spmovesets.arena)
                }
            }
            recipeBan = new Pat(c.recipeBan);
            keep = new Pat(c.keep);
            for (Map.Entry<String, Double> e : c.priceFactor.entrySet()) factors.add(Map.entry(new Pat(List.of(e.getKey())), e.getValue()));
        }
    }

    private static volatile State state;

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("item_policy.json");
    }

    private static State st() {
        State s = state;
        if (s == null) {
            synchronized (ItemPolicy.class) {
                if (state == null) state = new State(load());
                s = state;
            }
        }
        return s;
    }

    public static Config get() {
        return st().c;
    }

    private static Config load() {
        try {
            if (Files.exists(path())) {
                Config c = Config.fromJson(JsonParser.parseString(Files.readString(path(), StandardCharsets.UTF_8)).getAsJsonObject());
                if (c.migrated) {
                    Files.writeString(path(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
                    RtShop.LOGGER.info("[rtshop] item_policy.json 을 새 판({})으로 고쳤습니다 (2: Better Nether 상점 금지 · 전리품 금지에 spmovesets, 3: Create 의 조합법·전리품·블록 드랍 금지, 4: Create 는 상점·뽑기에도 오르지 않음, 5: Better Nether 의 조합법·전리품·드롭 금지, 6: spmovesets 의 상점·뽑기·조합법·전리품·드롭 금지, 7: 무기 · 장비 값 올리기, 8: 주민 거래에서 spmovesets 뺌, 9: Nightfall Enhance · Small Ships 조합법·전리품·드롭 금지, 스킬북 조합 금지, Aquamirae 장비 상점에서 뺌).", Config.VERSION);
                }
                return c;
            }
            Config c = Config.defaults();
            Files.createDirectories(path().getParent());
            Files.writeString(path(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
            return c;
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] item_policy.json 을 읽지 못했습니다: {}", e.getMessage());
            return Config.defaults();
        }
    }

    /** 파일을 다시 읽는다. 셈한 것(보스 아이템, 상점에 오른 장비)은 prepare / afterShops 가 다시 채운다. */
    public static synchronized void reload() {
        State old = state;
        State s = new State(load());
        if (old != null) {
            s.boss = old.boss;
            s.listed = old.listed;
        }
        state = s;
    }

    /** 시험용: 파일을 건드리지 않고 규칙을 바꾼다 (셈한 것은 비운다). */
    public static synchronized void setForTest(Config c) {
        state = new State(c);
    }

    // ------------------------------------------------------------------ 아이템의 갈래

    static ResourceLocation id(Item i) {
        return ForgeRegistries.ITEMS.getKey(i);
    }

    private static boolean vanilla(ResourceLocation id) {
        return id.getNamespace().equals("minecraft");
    }

    private static boolean ours(ResourceLocation id) {
        return id.getNamespace().equals(RtShop.MODID);
    }

    /** 무기·방어구·방패·활·도구·낚싯대·장신구인가 (어느 모드 것이든). */
    public static boolean gear(Item item) {
        State s = st();
        Boolean hit = s.gear.get(item);
        if (hit != null) return hit;
        boolean g = false;
        try {
            ItemStack stack = new ItemStack(item);
            g = item instanceof ArmorItem || item instanceof DiggerItem || item instanceof FishingRodItem
                    || Pricing.isClass(stack, "gear") || trinket(item);
        } catch (Throwable ignored) {
        }
        s.gear.put(item, g);
        return g;
    }

    /** 장신구 칸에 끼는 물건인가 (Curios · Accessories · Trinkets 의 태그). */
    private static boolean trinket(Item item) {
        try {
            for (TagKey<Item> t : item.builtInRegistryHolder().tags().toList()) {
                String ns = t.location().getNamespace();
                if (ns.equals("curios") || ns.equals("accessories") || ns.equals("trinkets")) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean word(Item item, Set<String> words) {
        ResourceLocation id = id(item);
        if (id == null || words.isEmpty()) return false;
        String path = id.getPath().toLowerCase(Locale.ROOT);
        for (String w : words) {
            if (w.isBlank()) continue;
            for (String piece : path.split("[_/.\\-]")) if (piece.equals(w) || piece.equals(w + "s") || piece.endsWith(w)) return true;
        }
        return false;
    }

    /** 지팡이인가: 이름의 낱말(staff · wand …)이나 아이템의 갈래(…Staff…)로 본다. 블록·음식은 아니다. */
    public static boolean staff(Item item) {
        State s = st();
        Boolean hit = s.staff.get(item);
        if (hit != null) return hit;
        boolean g = false;
        try {
            // 갑옷은 지팡이가 아니다 (3.64.2: Iron's Spells 의 '떠돌이 마법사(Wandering Magician)' 갑옷이 갈래 이름의 "wand" 에 걸려
            // 지팡이로 잡혀서, 조합법이 막히고 지팡이 값으로 상점에 올라 있었다)
            if (!(item instanceof BlockItem) && !(item instanceof net.minecraft.world.item.ArmorItem) && !item.isEdible() && new ItemStack(item).getMaxStackSize() == 1) {
                g = word(item, s.c.staffWords);
                for (Class<?> k = item.getClass(); !g && k != null && k != Item.class; k = k.getSuperclass()) {
                    String n = k.getSimpleName().toLowerCase(Locale.ROOT);
                    g = n.contains("staff") || n.contains("wand") && !n.contains("wander");
                }
            }
        } catch (Throwable ignored) {
        }
        s.staff.put(item, g);
        return g;
    }

    /** 총 모양의 무기인가 (이름의 낱말로 본다). */
    public static boolean gun(Item item) {
        State s = st();
        Boolean hit = s.gun.get(item);
        if (hit != null) return hit;
        boolean g = false;
        try {
            g = !(item instanceof BlockItem) && !item.isEdible() && word(item, s.c.gunWords);
        } catch (Throwable ignored) {
        }
        s.gun.put(item, g);
        return g;
    }

    /** 보스와 얽힌 아이템인가 (prepare 가 셈해 둔다). */
    public static boolean bossItem(Item item) {
        return st().boss.contains(item);
    }

    /** 상점·뽑기에 올라 있어 조합법이 막힌 장비인가 (아이템 쓰임새 설명에 쓴다). */
    public static boolean shopOnly(Item item) {
        State s = st();
        return s.c.enabled && s.c.recipeBanListedGear && s.listed.contains(item);
    }

    // ------------------------------------------------------------------ 묻는 곳

    /** NPC 상점에 오르면 안 되는가. */
    public static boolean shopBanned(ItemStack stack) {
        State s = st();
        if (!s.c.enabled || stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation id = id(item);
        if (id == null) return false;
        if (s.shopBan.hit(id)) return true;
        if (ours(id)) return false;
        if (s.c.shopBanGearMods.contains(id.getNamespace()) && gear(item)) return true;   // 3.84: Aquamirae 의 장비
        if (s.c.shopBanBossItems && s.boss.contains(item)) return true;
        return s.c.shopBanGuns && gun(item);
    }

    /**
     * 상점의 무기 · 장비 값 올리기의 배수 (3.80.0, gearRaise*): 무기 · 장비가 아니거나 꺼져 있으면 1. price = 올리기 전의 사는 값(원가 계산 뒤).
     * 사는 값에만 곱한다 — 상점이 사 주는 값(매입가)은 부르는 쪽(ShopLoader)에서 그대로 둔다.
     */
    public static double gearRaise(ItemStack stack, double price) {
        State s = st();
        if (!s.c.enabled || !s.c.gearRaise || stack == null || stack.isEmpty()) return 1;
        Item item = stack.getItem();
        ResourceLocation id = id(item);
        if (id == null || ours(id) || !raisedGear(item)) return 1;
        return gearRaiseFor(meleeDps(stack), price, s.c);
    }

    /** 배수를 고른다: 센 근접 무기(DPS) → 큰 배수, 아니면 올리기 전의 값으로 세 단계. */
    static double gearRaiseFor(double meleeDps, double price, Config c) {
        if (c.gearRaiseStrongDps > 0 && meleeDps >= c.gearRaiseStrongDps) return Math.max(1, c.gearRaiseHigh);
        if (price < c.gearRaiseMidFrom) return Math.max(1, c.gearRaiseLow);
        if (price < c.gearRaiseHighFrom) return Math.max(1, c.gearRaiseMid);
        return Math.max(1, c.gearRaiseHigh);
    }

    /** 시험용: 지금 규칙으로 고른 배수. */
    public static double gearRaiseForTest(double meleeDps, double price) {
        return gearRaiseFor(meleeDps, price, st().c);
    }

    /**
     * 값을 올리는 무기 · 장비인가 (3.80.0): 근접 무기 · 활 · 석궁 · 방패 · 방어구(Pricing 의 gear 갈래) · 지팡이 · 장신구.
     * 곡괭이 · 삽 · 괭이(도구) · 낚싯대 · 블록(장신구 칸에 끼는 등불 같은 것)은 아니다.
     */
    public static boolean raisedGear(Item item) {
        try {
            if (item instanceof BlockItem || item instanceof net.minecraft.world.item.FishingRodItem) return false;
            ItemStack one = new ItemStack(item);
            if (one.getMaxStackSize() != 1) return false;
            return com.rtshop.shop.Pricing.isClass(one, "gear") || staff(item) || trinket(item);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 근접 무기의 초당 피해 — 점검표(PackDiag)와 같게 (1 + 공격력) × (4 + 공격 속도). 근접 무기가 아니면 0. */
    public static double meleeDps(ItemStack s) {
        try {
            Item i = s.getItem();
            if (i instanceof net.minecraft.world.item.ProjectileWeaponItem || gun(i) || staff(i) || !com.rtshop.shop.Pricing.isClass(s, "weapon")) return 0;
            double dmg = 0, spd = 0;
            for (net.minecraft.world.entity.ai.attributes.AttributeModifier m : s.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) dmg += m.getAmount();
            for (net.minecraft.world.entity.ai.attributes.AttributeModifier m : s.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)) spd += m.getAmount();
            return (1 + dmg) * Math.max(0.1, 4 + spd);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 상점가에 곱하는 수 (없으면 1). */
    public static double priceFactor(ItemStack stack) {
        State s = st();
        if (!s.c.enabled || s.factors.isEmpty()) return 1;
        ResourceLocation id = id(stack.getItem());
        for (Map.Entry<Pat, Double> e : s.factors) if (e.getKey().hit(id)) return e.getValue();
        return 1;
    }

    /** 전리품 금지에서 빼 주는 것 (잉크처럼 자연 드랍으로 주는 것). */
    public static boolean lootSpared(ItemStack stack) {
        State s = st();
        return s.c.enabled && !stack.isEmpty() && s.lootAllow.hit(id(stack.getItem()));
    }

    /**
     * 터무니없이 센 장비인가 (3.65.0): 공격력(더하기 수정치 합)이 extremeAttack 이상, 또는 어느 칸에서든 방어력이 extremeArmor 이상.
     * 무한 · 숫자가 아닌 값도 센 것으로 본다. 아이템별로 기억한다.
     */
    public static boolean extreme(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        State s = st();
        if (!s.c.enabled) return false;
        Item item = stack.getItem();
        Boolean hit = s.extreme.get(item);
        if (hit != null) return hit;
        boolean g = false;
        try {
            ItemStack one = new ItemStack(item);
            if (one.getMaxStackSize() == 1 && !(item instanceof BlockItem)) {
                double dmg = 0;
                for (net.minecraft.world.entity.ai.attributes.AttributeModifier m : one.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                        .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                    if (m.getOperation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION) dmg += m.getAmount();
                }
                double armor = 0;
                for (net.minecraft.world.entity.EquipmentSlot es : net.minecraft.world.entity.EquipmentSlot.values()) {
                    double sum = 0;
                    for (net.minecraft.world.entity.ai.attributes.AttributeModifier m : one.getAttributeModifiers(es).get(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)) {
                        if (m.getOperation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION) sum += m.getAmount();
                    }
                    armor = Math.max(armor, Double.isNaN(sum) ? Double.POSITIVE_INFINITY : sum);
                }
                if (Double.isNaN(dmg)) dmg = Double.POSITIVE_INFINITY;
                g = s.c.extremeAttack > 0 && dmg >= s.c.extremeAttack || s.c.extremeArmor > 0 && armor >= s.c.extremeArmor;
            }
        } catch (Throwable ignored) {
        }
        s.extreme.put(item, g);
        return g;
    }

    /** 전리품(상자·몹·낚시)에서 빠져야 하는가. bossTable = 보스의 전리품표. */
    public static boolean lootBanned(ItemStack stack, boolean bossTable) {
        State s = st();
        if (!s.c.enabled || stack.isEmpty()) return false;
        if (extreme(stack)) return true;   // 3.65.0: 터무니없이 센 장비는 보스 전리품 · lootAllow 에서도 뺀다
        Item item = stack.getItem();
        ResourceLocation id = id(item);
        if (id == null || s.lootAllow.hit(id)) return false;
        if (s.lootBan.hit(id)) return true;
        if (bossTable || !s.c.modGearShopOnly || ours(id) || s.keep.hit(id) || s.boss.contains(item)) return false;
        if (s.c.shopBannedKeepOwnWays && s.shopBan.hit(id)) return false;
        if (vanilla(id) && !s.c.vanillaGearToo) return false;
        return gear(item);
    }

    /** 뽑기에 오르면 안 되는 ID 인가 (gachaBan): 뽑기 묶음의 아이템·엔티티, 마법 뽑기의 인챈트. */
    public static boolean gachaBanned(ResourceLocation id) {
        State s = st();
        return s.c.enabled && id != null && s.gachaBan.hit(id);
    }

    /** 전리품 금지 목록(lootBan)에 직접 적힌 물건인가 (모드 장비 규칙 · 보스 예외 없이, lootAllow 는 뺌). 몹이 코드로 떨어뜨리는 드롭에 쓴다 (3.68.1). */
    public static boolean lootListed(ItemStack stack) {
        State s = st();
        if (!s.c.enabled || stack.isEmpty()) return false;
        ResourceLocation id = id(stack.getItem());
        return id != null && s.lootBan.hit(id) && !s.lootAllow.hit(id);
    }

    /** 주민 · 떠돌이 상인의 거래에 오르면 안 되는가 (tradeBan, 3.82): 아이템 ID, 또는 NBT 에 금지 모드가 적힌 물건. */
    public static boolean tradeBanned(ItemStack stack) {
        State s = st();
        if (!s.c.enabled || stack == null || stack.isEmpty()) return false;
        ResourceLocation id = id(stack.getItem());
        if (id != null && s.tradeBan.hit(id)) return true;
        if (!s.tradeBanTags.isEmpty() && stack.hasTag()) {
            String tag = stack.getTag().toString().toLowerCase(Locale.ROOT);
            for (String ns : s.tradeBanTags) if (tag.contains(ns)) return true;
        }
        return false;
    }

    /** 블록을 캤을 때 나오면 안 되는 아이템인가 (blockDropBan). */
    public static boolean blockDropBanned(ItemStack stack) {
        State s = st();
        if (!s.c.enabled || stack.isEmpty()) return false;
        ResourceLocation id = id(stack.getItem());
        return id != null && s.blockDropBan.hit(id);
    }

    /** 이 조합법을 막는 까닭 (막지 않으면 null). */
    public static String recipeReason(ResourceLocation recipe, ResourceLocation output) {
        State s = st();
        if (!s.c.enabled) return null;
        if (recipe != null && s.c.recipeBanMods.contains(recipe.getNamespace())) return "mod";
        if (output == null) return null;
        Item item0 = ForgeRegistries.ITEMS.getValue(output);
        if (item0 != null && item0 != Items.AIR && extreme(new ItemStack(item0))) return "extreme";   // 3.65.0: keep 에 적어도 못 만든다
        if (s.keep.hit(output)) return null;
        if (s.recipeBan.hit(output)) return "ban";
        Item item = item0;
        if (item == null || item == Items.AIR || s.boss.contains(item)) return null;
        if (s.c.recipeBanStaffs && staff(item)) return "staff";
        if (ours(output) || (vanilla(output) && !s.c.vanillaGearToo)) return null;
        if (s.c.recipeBanListedGear && s.listed.contains(item)) return "listed";
        // 상점에 올리지 않기로 한 모드(shopBan)의 장비는 "상점에서만" 규칙에서 빠진다: 그 모드의 방식대로(조합·전리품) 얻는다
        if (s.c.modGearShopOnly && gear(item) && !(s.c.shopBannedKeepOwnWays && s.shopBan.hit(output))) return "gear";
        return null;
    }

    public static boolean recipeBanned(ResourceLocation recipe, ResourceLocation output) {
        return recipeReason(recipe, output) != null;
    }

    // ------------------------------------------------------------------ 셈

    /** 보스 아이템을 셈한다 (상점을 읽기 전에). */
    public static void prepare(MinecraftServer server) {
        State s = st();
        Set<Item> out = new HashSet<>();
        try {
            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation id = id(item);
                if (id == null || item == Items.AIR || ours(id)) continue;
                if (s.c.bossMods.contains(id.getNamespace()) || s.bossItems.hit(id)) out.add(item);
            }
            if (s.c.sampleBossDrops && server != null) out.addAll(sampledBossDrops(server, out, s.c.bossMods));
            if (server != null) derive(server, out);
            out.removeIf(i -> s.notBoss.hit(id(i)));
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 보스 아이템을 셈하지 못했습니다: {}", t.toString());
        }
        s.boss = Set.copyOf(out);
    }

    /**
     * 보스들의 전리품표를 여러 번 열어, 그 보스의 모드가 만든 아이템 가운데 "보스에게서만 나오는 것"을 보스 아이템으로 친다.
     * 같은 모드의 다른 몹·상자에서도 나오는 것(흔한 재료·두루마리)과, 보스 아이템 없이도 만들 수 있는 것은 뺀다.
     * (통째로 보스 모드인 bossMods 는 이미 전부 보스 아이템이라 여기서 다루지 않는다.)
     */
    private static Set<Item> sampledBossDrops(MinecraftServer server, Set<Item> known, Set<String> bossMods) {
        // 전리품을 열어 보는 일은 주사위라서, 할 때마다 결과가 조금씩 달라질 수 있다. 서버를 켠 동안에는 한 번 센 것을 그대로 쓴다
        // (상점을 다시 읽을 때마다 보스 아이템이 들락날락하지 않게). 보스 모드 목록이 바뀌면 다시 센다.
        String key = System.identityHashCode(server) + ":" + bossMods;
        Set<Item> cached = sampledKey.equals(key) ? sampled : null;
        if (cached == null) {
            Set<Item> all = new HashSet<>(known);
            sampleBosses(server, all);
            all.removeAll(known);
            cached = Set.copyOf(all);
            sampled = cached;
            sampledKey = key;
        }
        return cached;
    }

    private static volatile Set<Item> sampled = Set.of();
    private static volatile String sampledKey = "";

    /** 시험용·/rtshop reload 용: 보스 전리품을 다음에 다시 센다. */
    public static void forgetSampled() {
        sampledKey = "";
    }

    private static void sampleBosses(MinecraftServer server, Set<Item> out) {
        State s = st();
        Set<String> ids = new LinkedHashSet<>();
        try {
            var tag = ForgeRegistries.ENTITY_TYPES.tags();
            if (tag != null) {
                for (EntityType<?> t : tag.getTag(Tags.EntityTypes.BOSSES)) {
                    ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(t);
                    if (rl != null) ids.add(rl.toString());
                }
            }
        } catch (Throwable ignored) {
        }
        ids.addAll(com.rtshop.party.PartyConfig.get().bosses);
        ids.addAll(com.rtshop.party.PartyConfig.get().arenaBosses);
        ids.addAll(MobRules.get().extraBosses);
        Map<String, Set<Item>> found = new HashMap<>();          // 보스의 모드 → 그 보스들이 떨어뜨린 그 모드의 아이템
        Set<ResourceLocation> bossTables = new HashSet<>();
        long until = System.currentTimeMillis() + 15000;
        for (String bid : ids) {
            if (System.currentTimeMillis() > until) break;
            try {
                ResourceLocation rl = ResourceLocation.tryParse(bid);
                EntityType<?> type = rl == null || !ForgeRegistries.ENTITY_TYPES.containsKey(rl) ? null : ForgeRegistries.ENTITY_TYPES.getValue(rl);
                if (type == null || rl.getNamespace().equals("minecraft") || s.c.bossMods.contains(rl.getNamespace())) continue;
                bossTables.add(type.getDefaultLootTable());
                Map<String, LootRules.Stat> got = LootRules.sample(server.overworld(), type.getDefaultLootTable(), 24, true);
                for (String key : got.keySet()) {
                    ResourceLocation iid = ResourceLocation.tryParse(key);
                    if (iid == null || !iid.getNamespace().equals(rl.getNamespace())) continue;
                    Item item = ForgeRegistries.ITEMS.getValue(iid);
                    if (item != null && item != Items.AIR) found.computeIfAbsent(rl.getNamespace(), k -> new HashSet<>()).add(item);
                }
            } catch (Throwable ignored) {
            }
        }
        if (found.isEmpty()) return;
        // 같은 모드의 다른 몹·상자에서도 나오면 보스만의 것이 아니다
        until = System.currentTimeMillis() + 15000;
        try {
            for (ResourceLocation table : server.getLootData().getKeys(net.minecraft.world.level.storage.loot.LootDataType.TABLE)) {
                if (System.currentTimeMillis() > until) break;
                Set<Item> cand = found.get(table.getNamespace());
                if (cand == null || cand.isEmpty() || bossTables.contains(table)) continue;
                String path = table.getPath();
                if (!path.startsWith("entities/") && !path.startsWith("chests/")) continue;
                Map<String, LootRules.Stat> got;
                try {
                    got = LootRules.sample(server.overworld(), table, 12, true);
                } catch (Throwable t) {
                    continue;
                }
                for (String key : got.keySet()) {
                    ResourceLocation iid = ResourceLocation.tryParse(key);
                    Item item = iid == null ? null : ForgeRegistries.ITEMS.getValue(iid);
                    if (item != null) cand.remove(item);
                }
            }
        } catch (Throwable ignored) {
        }
        Set<Item> all = new HashSet<>(out);
        for (Set<Item> c : found.values()) all.addAll(c);
        // 보스 아이템이 하나도 들지 않는 조합법으로 만들 수 있으면 보스만의 것이 아니다
        Set<Item> craftable = new HashSet<>();
        for (Recipe<?> r : RecipeRules.originals(server)) {
            try {
                ItemStack res = r.getResultItem(server.registryAccess());
                if (res.isEmpty() || out.contains(res.getItem()) || !all.contains(res.getItem()) || r.getIngredients().isEmpty()) continue;
                boolean needsBoss = false;
                for (Ingredient ing : r.getIngredients()) {
                    ItemStack[] opts = ing.getItems();
                    if (opts.length == 0) continue;
                    boolean only = true;
                    for (ItemStack o : opts) only &= all.contains(o.getItem());
                    needsBoss |= only;
                }
                if (!needsBoss) craftable.add(res.getItem());
            } catch (Throwable ignored) {
            }
        }
        for (Set<Item> c : found.values()) {
            c.removeAll(craftable);
            out.addAll(c);
        }
    }

    /**
     * 보스 아이템으로만 채워지는 재료 칸이 있는 조합법의 결과물도 보스 아이템이다. 다만 결과물이 그 보스 재료와 같은 모드의 것이거나
     * 보스 모드(bossMods)의 것일 때만 (남의 모드의 안내서·기술서·장식까지 번지지 않게). 바닐라 것은 뺀다.
     */
    private static void derive(MinecraftServer server, Set<Item> out) {
        State s = st();
        Collection<Recipe<?>> recipes = RecipeRules.originals(server);
        for (int pass = 0; pass < 4; pass++) {
            boolean grew = false;
            for (Recipe<?> r : recipes) {
                try {
                    ItemStack res = r.getResultItem(server.registryAccess());
                    if (res.isEmpty() || out.contains(res.getItem())) continue;
                    ResourceLocation rid = id(res.getItem());
                    if (rid == null || vanilla(rid) || ours(rid)) continue;
                    boolean bossMod = s.c.bossMods.contains(rid.getNamespace());
                    for (Ingredient ing : r.getIngredients()) {
                        ItemStack[] opts = ing.getItems();
                        if (opts.length == 0) continue;
                        boolean all = true, kin = bossMod;
                        for (ItemStack o : opts) {
                            all &= out.contains(o.getItem());
                            ResourceLocation oid = id(o.getItem());
                            kin |= oid != null && oid.getNamespace().equals(rid.getNamespace());
                        }
                        if (all && kin) {
                            out.add(res.getItem());
                            grew = true;
                            break;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (!grew) break;
        }
    }

    /** 상점을 읽은 뒤: 상점·뽑기에 오른 장비를 적고, 조합법 규칙을 다시 건다. */
    public static void afterShops(MinecraftServer server, ShopLoader.Loaded loaded) {
        State s = st();
        Set<Item> listed = new HashSet<>();
        try {
            for (ShopData shop : loaded.shops.values()) {
                for (ShopData.Entry e : shop.entries) {
                    if (e.buyable && !e.stack.isEmpty() && gear(e.stack.getItem())) listed.add(e.stack.getItem());
                }
            }
            for (String cat : new String[]{"weapon", "accessory"}) {
                for (com.rtshop.gacha.GachaConfig.Entry e : com.rtshop.gacha.GachaConfig.get().pool(cat)) {
                    ResourceLocation rl = ResourceLocation.tryParse(e.item);
                    Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
                    if (item != null && item != Items.AIR) listed.add(item);
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 상점·뽑기에 오른 장비를 모으지 못했습니다: {}", t.toString());
        }
        s.listed = Set.copyOf(listed);
        if (server == null) return;
        try {
            RecipeRules.apply(server, RecipeRules.get());
            if (server.getPlayerList() != null && !server.getPlayerList().getPlayers().isEmpty()) server.getPlayerList().reloadResources();
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 조합법 규칙을 다시 걸지 못했습니다: {}", t.toString());
        }
        try {
            report(server, loaded);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 아이템 규칙 보고서를 쓰지 못했습니다: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ 몬스터 드랍 (잉크)

    /** 점검용: 굴림을 정한다 (음수 = 진짜 무작위). */
    public static double testRoll = -1;

    /** 플레이어가 몬스터를 잡았을 때 따로 굴리는 드랍을 더한다. */
    public static void mobDrops(LivingEntity dead, boolean boss, Object killer, Collection<ItemEntity> drops) {
        State s = st();
        if (!s.c.enabled || s.c.mobDrops.isEmpty() || !(killer instanceof Player) || !(dead instanceof Enemy)) return;
        for (Map.Entry<String, Double> e : s.c.mobDrops.entrySet()) {
            ResourceLocation rl = ResourceLocation.tryParse(e.getKey());
            Item item = rl == null || !ForgeRegistries.ITEMS.containsKey(rl) ? null : ForgeRegistries.ITEMS.getValue(rl);
            if (item == null || item == Items.AIR) continue;
            double chance = e.getValue() * (boss ? s.c.bossDropFactor : 1);
            double roll = testRoll >= 0 ? testRoll : dead.getRandom().nextDouble();
            if (roll < chance) drops.add(new ItemEntity(dead.level(), dead.getX(), dead.getY() + 0.3, dead.getZ(), new ItemStack(item)));
        }
    }

    // ------------------------------------------------------------------ 보고서

    /** 상점을 읽다가 규칙 때문에 뺀 품목 (상점 → 아이템). */
    private static final Map<String, Set<String>> SKIPPED = new TreeMap<>();
    /** 조합법 규칙이 막은 것 (까닭 → 결과물). */
    private static final Map<String, Set<String>> RECIPES = new TreeMap<>();
    private static final List<String> STAFFS_LISTED = new ArrayList<>();
    private static final Map<String, Integer> RAISED = new TreeMap<>();

    public static synchronized void beginShops() {
        SKIPPED.clear();
        STAFFS_LISTED.clear();
        RAISED.clear();
    }

    public static synchronized void noteSkipped(String shop, ItemStack stack) {
        SKIPPED.computeIfAbsent(shop, k -> new TreeSet<>()).add(String.valueOf(id(stack.getItem())));
    }

    public static synchronized void noteStaff(String idAndPrice) {
        STAFFS_LISTED.add(idAndPrice);
    }

    public static synchronized void noteRaised(ItemStack stack) {
        ResourceLocation id = id(stack.getItem());
        if (id != null) RAISED.merge(id.getNamespace(), 1, Integer::sum);
    }

    public static synchronized void beginRecipes() {
        RECIPES.clear();
    }

    public static synchronized void noteRecipe(String reason, ResourceLocation recipe, ResourceLocation output) {
        RECIPES.computeIfAbsent(reason, k -> new TreeSet<>()).add(output == null ? "(" + recipe + ")" : output.toString());
    }

    public static synchronized Map<String, Set<String>> skippedForTest() {
        Map<String, Set<String>> m = new TreeMap<>();
        SKIPPED.forEach((k, v) -> m.put(k, new TreeSet<>(v)));
        return m;
    }

    public static synchronized int recipesBlockedForTest(String reason) {
        Set<String> s = RECIPES.get(reason);
        return s == null ? 0 : s.size();
    }

    public static int bossCountForTest() {
        return st().boss.size();
    }

    public static int listedCountForTest() {
        return st().listed.size();
    }

    private static String byMod(Collection<String> ids) {
        Map<String, Integer> m = new TreeMap<>();
        for (String s : ids) m.merge(s.contains(":") ? s.substring(0, s.indexOf(':')) : s, 1, Integer::sum);
        return m.toString();
    }

    /** config/rtshop/export/item_policy.txt: 규칙이 실제로 무엇을 막았는지. */
    private static synchronized void report(MinecraftServer server, ShopLoader.Loaded loaded) throws Exception {
        State s = st();
        List<String> out = new ArrayList<>();
        out.add("# 아이템 규칙이 한 일 (config/rtshop/item_policy.json)");
        Set<String> boss = new TreeSet<>();
        for (Item i : s.boss) boss.add(String.valueOf(id(i)));
        out.add("");
        out.add("## 보스 아이템 " + boss.size() + "종 " + byMod(boss) + " — 상점에 오르지 않고, 조합법·보스 전리품은 그대로");
        out.add(String.join(", ", boss));
        out.add("");
        out.add("## 상점에서 뺀 품목");
        for (Map.Entry<String, Set<String>> e : SKIPPED.entrySet()) {
            out.add("- " + e.getKey() + " " + e.getValue().size() + "종 " + byMod(e.getValue()) + ": " + String.join(", ", e.getValue()));
        }
        out.add("");
        out.add("## 상점가를 올린 품목 (모드 → 품목 수): " + RAISED);
        out.add("");
        out.add("## 상점에 새로 올린 지팡이 " + STAFFS_LISTED.size() + "종: " + String.join(", ", STAFFS_LISTED));
        out.add("");
        out.add("## 막은 조합법 (까닭 → 결과물)  mod = 그 모드의 조합법 전부 · ban = recipeBan · staff = 지팡이 · listed = 상점·뽑기에 오른 장비 · gear = 모드 장비");
        for (Map.Entry<String, Set<String>> e : RECIPES.entrySet()) {
            out.add("- " + e.getKey() + " " + e.getValue().size() + "종 " + byMod(e.getValue()) + ": " + String.join(", ", e.getValue()));
        }
        Set<String> sold = new HashSet<>();
        for (ShopData shop : loaded.shops.values()) {
            for (ShopData.Entry e : shop.entries) if (e.buyable) sold.add(String.valueOf(id(e.stack.getItem())));
        }
        Set<String> gacha = new HashSet<>();
        for (Item i : s.listed) gacha.add(String.valueOf(id(i)));
        Set<String> lootBanned = new TreeSet<>(), lost = new TreeSet<>(), staffs = new TreeSet<>(), guns = new TreeSet<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation id = id(item);
            if (id == null || item == Items.AIR || ours(id)) continue;
            if (s.lootBan.hit(id) && !s.lootAllow.hit(id)) lootBanned.add(id.toString());
            if (staff(item)) staffs.add(id + (sold.contains(id.toString()) ? "" : s.boss.contains(item) ? "(보스)" : "(상점에 없음)"));
            if (gun(item)) guns.add(id.toString());
            if (!vanilla(id) && gear(item) && !s.boss.contains(item) && !s.keep.hit(id) && !sold.contains(id.toString()) && !gacha.contains(id.toString())) {
                lost.add(id.toString());
            }
        }
        out.add("");
        out.add("## 전리품에서 빠지는 아이템 (lootBan) " + lootBanned.size() + "종 " + byMod(lootBanned));
        out.add("");
        out.add("## 지팡이 " + staffs.size() + "종: " + String.join(", ", staffs));
        out.add("");
        out.add("## 총 모양 무기 " + guns.size() + "종: " + String.join(", ", guns));
        out.add("");
        out.add("## 상점에도 뽑기에도 없는 모드 장비 " + lost.size() + "종 " + byMod(lost) + " — 조합법·전리품이 막혀 있으니, 쓰게 하려면 상점에 올리거나 keep 에 적으세요");
        out.add(String.join(", ", lost));
        Path file = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("export").resolve("item_policy.txt");
        Files.createDirectories(file.getParent());
        Files.write(file, out, StandardCharsets.UTF_8);
    }
}
