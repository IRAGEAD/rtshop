package com.rtshop.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 월드 규칙 (config/rtshop/world_rules.json): 이 서버에서 아예 생기지 않을 것들.
 *  - neverStructures: 새 지형에 절대 생기지 않는 건축물 (나선 비경의 무너진 문, Create 전용 건축물 …)
 *  - neverRecipes: 막는 조합법 ("item:아이템ID" = 그 아이템을 만드는 조합법 전부)
 *  - lootBlockedMods: 이 모드의 아이템은 상자·몹 전리품에서 빠진다
 *  - 나선 비경의 문: 운영자(크리에이티브)만 세울 수 있고, 정해 둔 한 곳(마을)의 문으로만 들어갈 수 있다
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class WorldRules {
    public static final String ABYSS_KEY = "spiral_abyss:abyss_gate_activate_block";
    public static final String ABYSS_DIM = "spiral_abyss:spiral_abyss";

    public final Set<String> neverStructures = new LinkedHashSet<>();
    public final Set<String> neverRecipes = new LinkedHashSet<>();
    public final Set<String> lootBlockedMods = new LinkedHashSet<>();
    public boolean abyssPortalOpOnly = true;
    public boolean abyssPortalSet;
    public String abyssPortalDim = "minecraft:overworld";
    public int abyssPortalX, abyssPortalY, abyssPortalZ, abyssPortalRadius = 24;
    /** 룬 제단(별빛 수정 구체): 운영자만 세우고, 정해 둔 하나만 쓸 수 있다. */
    public static final String ALTAR_KEY = "rtshop:arcane_orb";
    public boolean altarOpOnly = true;
    /**
     * 마법(Iron's Spells 의 주문, Spell Engine 의 마법 계열 주문)은 마법사 직업만 쓴다.
     * 3.84 (사용자 11-29 "마법사를 제외한 직업들도 마법을 사용할 수 있게 하되 … 많이 떨어지게"): 기본 false — 누구나 쓰되 마법사가 아니면 약하다 (jobs.json 의 nonMagePower · nonMageCastTime).
     */
    public boolean magicOnlyForMage = false;
    /** world_rules.json 의 판. 2 (3.84): magicOnlyForMage 를 끈다. */
    public int rulesVersion = 2;
    /** 옛 판을 읽어 고쳤다 (다시 쓴다). */
    transient boolean migrated384;
    /** spmovesets 의 생존 시작 꾸러미(안내서 + 열매 뽑기 열쇠)를 접속할 때 주는가 (3.84: 기본 false = 주지 않는다, compat/SpMovesetsKitOff). */
    public boolean spmovesetsStarterKit = false;
    /** 배고픔을 바닐라처럼 (3.84, compat/HungerVanilla): Kaleidoscope Cookery 의 포만 보호막(맞으면 피해 대신 배고픔이 닳음)과 Spell Engine 주문의 배고픔 소모를 끈다. */
    public boolean vanillaHunger = true;
    /** Small Ships 가 바닐라 보트를 느리게 만드는 것을 끈다 (3.84, compat/SmallShipsBoats). */
    public boolean vanillaBoatSpeed = true;
    /** RPG 주문(Spell Engine)의 물건(룬) 값을 끈다 (3.84, compat/SpellEngineTweaks): 모드팩에 Runes 모드가 없어 '공기 부족'으로 막히던 것. */
    public boolean spellItemCostOff = true;
    /** 처음 들어온 사람에게 무작위 색의 셜커 상자를 하나 준다 (3.84, rules/WelcomeGift). */
    public boolean welcomeShulker = true;
    /** 무기를 바꿔도 특수기의 스택·게이지가 사라지지 않는다 (compat/EfStacks). */
    public boolean keepSkillStacks = true;
    /** 스킬북으로 배운 기술(Epic Fight)은 죽어도 남는다: 서버가 켜질 때 게임 규칙 keepSkills 를 켠다. */
    public boolean keepSkillsOnDeath = true;
    /** 특수기가 있는 무기를 처음 얻으면 그 무기의 설명서를 준다. */
    public boolean weaponManuals = true;
    /** Bosses'Rise 의 구르기(Dodge Roll, Z 키)를 쓰게 둘지 (3.78: 기본 끔 — Epic Fight 의 회피기와 겹치지 않게). */
    public boolean bossesRiseRoll = false;
    /** 강화 모루도 마을에 하나뿐 (3.49): 운영자(크리에이티브)만 놓고 치울 수 있고, 정해 둔 모루만 열린다. */
    public static final String ANVIL_KEY = "rtshop:enhance_anvil";
    public boolean anvilOpOnly = true;
    public boolean anvilSet;
    public String anvilDim = "minecraft:overworld";
    public int anvilX, anvilY, anvilZ;
    public String textAnvilOnly = "강화 모루는 마을에 있는 하나뿐입니다. 그 모루를 이용하세요.";
    public String textAnvilBuild = "강화 모루는 새로 놓을 수 없습니다. 마을의 모루를 이용하세요.";
    public boolean altarSet;
    public String altarDim = "minecraft:overworld";
    public int altarX, altarY, altarZ;
    public String textAltarOnly = "룬 제단은 마을에 있는 하나뿐입니다. 그 제단을 이용하세요.";
    public String textAltarBuild = "룬 제단은 새로 세울 수 없습니다. 마을의 제단을 이용하세요.";
    public String textPortalOnly = "나선 비경으로 가는 문은 마을에 있는 하나뿐입니다.";
    public String textPortalBuild = "나선 비경의 문은 새로 세울 수 없습니다. 마을의 문을 이용하세요.";

    private static WorldRules current;
    private static volatile Set<String> never = Collections.emptySet();
    private static volatile Set<String> lootMods = Collections.emptySet();

    public static WorldRules defaults() {
        WorldRules w = new WorldRules();
        w.neverStructures.addAll(List.of("spiral_abyss:ruined_portal",
                "idas:tinkers_citadel", "idas:tinkers_workshop", "idas:train_ruins", "idas:nether_pump_camp", "idas:washing_camp"));
        w.neverRecipes.addAll(List.of("item:" + ABYSS_KEY, "item:spiral_abyss:abyss_gate_vertical", "item:spiral_abyss:abyss_gate_horizontal", "item:spiral_abyss:abyss_gate_curve"));
        w.lootBlockedMods.add("create");
        return w;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("world_rules.json");
    }

    public static synchronized WorldRules get() {
        if (current == null) {
            current = load();
            publish(current);
        }
        return current;
    }

    public static synchronized void reload() {
        current = null;
        get();
    }

    private static void publish(WorldRules w) {
        never = Set.copyOf(w.neverStructures);
        lootMods = Set.copyOf(w.lootBlockedMods);
    }

    private static void strings(JsonObject o, String k, Set<String> into) {
        if (!o.has(k)) return;
        into.clear();
        for (JsonElement e : o.getAsJsonArray(k)) {
            String s = e.getAsString().trim();
            if (!s.isEmpty()) into.add(s);
        }
    }

    public static WorldRules parse(JsonObject o) {
        WorldRules w = defaults();
        strings(o, "neverStructures", w.neverStructures);
        strings(o, "neverRecipes", w.neverRecipes);
        strings(o, "lootBlockedMods", w.lootBlockedMods);
        if (o.has("abyssPortalOpOnly")) w.abyssPortalOpOnly = o.get("abyssPortalOpOnly").getAsBoolean();
        if (o.has("weaponManuals")) w.weaponManuals = o.get("weaponManuals").getAsBoolean();
        if (o.has("bossesRiseRoll")) w.bossesRiseRoll = o.get("bossesRiseRoll").getAsBoolean();
        if (o.has("keepSkillsOnDeath")) w.keepSkillsOnDeath = o.get("keepSkillsOnDeath").getAsBoolean();
        if (o.has("magicOnlyForMage")) w.magicOnlyForMage = o.get("magicOnlyForMage").getAsBoolean();
        int rv = o.has("rulesVersion") ? o.get("rulesVersion").getAsInt() : 1;
        if (rv < 2) {   // 3.84: 마법사가 아니어도 주문을 쓴다 (약하게). 다음에 저장할 때 판 번호가 적힌다
            w.magicOnlyForMage = false;
            w.migrated384 = true;
        }
        if (o.has("keepSkillStacks")) w.keepSkillStacks = o.get("keepSkillStacks").getAsBoolean();
        if (o.has("spmovesetsStarterKit")) w.spmovesetsStarterKit = o.get("spmovesetsStarterKit").getAsBoolean();
        if (o.has("vanillaHunger")) w.vanillaHunger = o.get("vanillaHunger").getAsBoolean();
        if (o.has("vanillaBoatSpeed")) w.vanillaBoatSpeed = o.get("vanillaBoatSpeed").getAsBoolean();
        if (o.has("spellItemCostOff")) w.spellItemCostOff = o.get("spellItemCostOff").getAsBoolean();
        if (o.has("welcomeShulker")) w.welcomeShulker = o.get("welcomeShulker").getAsBoolean();
        if (o.has("abyssPortal") && o.get("abyssPortal").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("abyssPortal");
            w.abyssPortalSet = p.has("set") && p.get("set").getAsBoolean();
            if (p.has("dim")) w.abyssPortalDim = p.get("dim").getAsString();
            if (p.has("x")) w.abyssPortalX = p.get("x").getAsInt();
            if (p.has("y")) w.abyssPortalY = p.get("y").getAsInt();
            if (p.has("z")) w.abyssPortalZ = p.get("z").getAsInt();
            if (p.has("radius")) w.abyssPortalRadius = Math.max(4, p.get("radius").getAsInt());
        }
        if (o.has("runeAltarOpOnly")) w.altarOpOnly = o.get("runeAltarOpOnly").getAsBoolean();
        if (o.has("runeAltar") && o.get("runeAltar").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("runeAltar");
            w.altarSet = p.has("set") && p.get("set").getAsBoolean();
            if (p.has("dim")) w.altarDim = p.get("dim").getAsString();
            if (p.has("x")) w.altarX = p.get("x").getAsInt();
            if (p.has("y")) w.altarY = p.get("y").getAsInt();
            if (p.has("z")) w.altarZ = p.get("z").getAsInt();
        }
        if (o.has("enhanceAnvilOpOnly")) w.anvilOpOnly = o.get("enhanceAnvilOpOnly").getAsBoolean();
        if (o.has("enhanceAnvil") && o.get("enhanceAnvil").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("enhanceAnvil");
            w.anvilSet = p.has("set") && p.get("set").getAsBoolean();
            if (p.has("dim")) w.anvilDim = p.get("dim").getAsString();
            if (p.has("x")) w.anvilX = p.get("x").getAsInt();
            if (p.has("y")) w.anvilY = p.get("y").getAsInt();
            if (p.has("z")) w.anvilZ = p.get("z").getAsInt();
        }
        if (o.has("textAnvilOnly")) w.textAnvilOnly = o.get("textAnvilOnly").getAsString();
        if (o.has("textAnvilBuild")) w.textAnvilBuild = o.get("textAnvilBuild").getAsString();
        if (o.has("textAltarOnly")) w.textAltarOnly = o.get("textAltarOnly").getAsString();
        if (o.has("textAltarBuild")) w.textAltarBuild = o.get("textAltarBuild").getAsString();
        if (o.has("textPortalOnly")) w.textPortalOnly = o.get("textPortalOnly").getAsString();
        if (o.has("textPortalBuild")) w.textPortalBuild = o.get("textPortalBuild").getAsString();
        return w;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("월드 규칙. neverStructures: 새 지형에 절대 생기지 않는 건축물 ID. neverRecipes: 막는 조합법 (item:아이템ID). lootBlockedMods: 전리품에서 빼는 모드 ID.");
        help.add("magicOnlyForMage (3.84 부터 기본 false): false 면 누구나 주문을 쓰되 마법사가 아니면 위력 · 시전 속도가 떨어진다 (jobs.json 의 nonMagePower · nonMageCastTime, 지팡이 없이는 noStaffPower). true 면 마법사 직업만 마법을 쓴다 (Iron's Spells 의 모든 주문, Spell Engine 의 마법 계열 주문; 근접·활 계열의 무기 기술은 누구나). 크리에이티브는 언제나 쓸 수 있다.");
        help.add("keepSkillStacks: true 면 무기를 바꿔도 특수기의 스택(충전 횟수)과 게이지가 사라지지 않는다: 그 무기를 다시 들면 돌아오고, 처음 드는 무기에는 방금 것의 스택이 넘어간다.");
        help.add("keepSkillsOnDeath: true 면 스킬북으로 배운 기술이 죽어도 사라지지 않는다 (서버가 켜질 때 Epic Fight 의 게임 규칙 keepSkills 를 켠다).");
        help.add("weaponManuals: true 면 특수기가 있는 무기(Epic Fight)를 처음 얻었을 때 그 무기의 설명서를 한 권 준다 (같은 종류·같은 특수기는 한 권).");
        help.add("bossesRiseRoll: false 면 Bosses'Rise 모드의 구르기(Dodge Roll, 기본 Z 키 · 핫바 오른쪽의 초록 화살표)를 서버에서 끈다 (서버가 켜질 때 그 모드의 Can use roll 을 끔). true 면 그 모드 설정 그대로.");
        help.add("spmovesetsStarterKit (3.84): false 면 spmovesets 가 접속할 때 주던 생존 시작 꾸러미(안내서 · 열매 뽑기 열쇠)를 주지 않는다. 이미 받은 것은 그대로.");
        help.add("vanillaHunger (3.84): true 면 배고픔이 바닐라처럼 닳는다 — Kaleidoscope Cookery 의 포만 보호막(맞으면 피해 대신 배고픔이 닳던 효과)과 RPG(Spell Engine) 주문의 배고픔 소모를 서버가 켜질 때 끈다.");
        help.add("vanillaBoatSpeed (3.84): true 면 Small Ships 가 바닐라 보트를 느리게 만드는 설정(vanilla boat slowdown)을 서버가 켜질 때 끈다.");
        help.add("welcomeShulker (3.84): true 면 처음 들어온 사람(이미 놀던 사람은 이 판에서 처음 들어올 때)에게 무작위 색의 셜커 상자를 하나씩 준다 (한 사람에 한 번).");
        help.add("spellItemCostOff (3.84): true 면 RPG 주문(Spell Engine)이 룬 같은 물건을 값으로 치르지 않는다 — 모드팩에 Runes 모드가 없어 '공기 부족'으로 주문이 막히던 것. 주문 값은 Iron's 마나(spell_mana.json).");
        help.add("abyssPortal: 나선 비경으로 들어갈 수 있는 단 하나의 문. 게임 안에서 문 앞에 서서 /rtshop abyss portal here (OP). abyssPortalOpOnly: 문은 운영자(크리에이티브)만 세울 수 있음.");
        help.add("enhanceAnvil: 쓸 수 있는 단 하나의 강화 모루. 운영자가 처음 놓은 것이 자동으로 정해지고, 바꾸려면 새 모루 곁에서 /rtshop anvil here (풀기: /rtshop anvil clear). enhanceAnvilOpOnly: 운영자(크리에이티브)만 놓고 치울 수 있음.");
        help.add("runeAltar: 쓸 수 있는 단 하나의 룬 제단(별빛 수정 구체). 운영자가 처음 놓은 것이 자동으로 정해지고, 바꾸려면 새 제단 곁에서 /rtshop altar here. runeAltarOpOnly: 운영자(크리에이티브)만 놓을 수 있음.");
        help.add("이미 만들어진 지형에 있는 건축물은 사라지지 않습니다. 고친 뒤 /rtshop reload.");
        o.add("_help", help);
        JsonArray a = new JsonArray();
        neverStructures.forEach(a::add);
        o.add("neverStructures", a);
        JsonArray b = new JsonArray();
        neverRecipes.forEach(b::add);
        o.add("neverRecipes", b);
        JsonArray c = new JsonArray();
        lootBlockedMods.forEach(c::add);
        o.add("lootBlockedMods", c);
        o.addProperty("abyssPortalOpOnly", abyssPortalOpOnly);
        o.addProperty("weaponManuals", weaponManuals);
        o.addProperty("bossesRiseRoll", bossesRiseRoll);
        o.addProperty("keepSkillsOnDeath", keepSkillsOnDeath);
        o.addProperty("rulesVersion", rulesVersion);
        o.addProperty("magicOnlyForMage", magicOnlyForMage);
        o.addProperty("keepSkillStacks", keepSkillStacks);
        o.addProperty("spmovesetsStarterKit", spmovesetsStarterKit);
        o.addProperty("vanillaHunger", vanillaHunger);
        o.addProperty("vanillaBoatSpeed", vanillaBoatSpeed);
        o.addProperty("spellItemCostOff", spellItemCostOff);
        o.addProperty("welcomeShulker", welcomeShulker);
        JsonObject p = new JsonObject();
        p.addProperty("set", abyssPortalSet);
        p.addProperty("dim", abyssPortalDim);
        p.addProperty("x", abyssPortalX);
        p.addProperty("y", abyssPortalY);
        p.addProperty("z", abyssPortalZ);
        p.addProperty("radius", abyssPortalRadius);
        o.add("abyssPortal", p);
        o.addProperty("runeAltarOpOnly", altarOpOnly);
        JsonObject al = new JsonObject();
        al.addProperty("set", altarSet);
        al.addProperty("dim", altarDim);
        al.addProperty("x", altarX);
        al.addProperty("y", altarY);
        al.addProperty("z", altarZ);
        o.add("runeAltar", al);
        o.addProperty("enhanceAnvilOpOnly", anvilOpOnly);
        JsonObject an = new JsonObject();
        an.addProperty("set", anvilSet);
        an.addProperty("dim", anvilDim);
        an.addProperty("x", anvilX);
        an.addProperty("y", anvilY);
        an.addProperty("z", anvilZ);
        o.add("enhanceAnvil", an);
        o.addProperty("textAnvilOnly", textAnvilOnly);
        o.addProperty("textAnvilBuild", textAnvilBuild);
        o.addProperty("textAltarOnly", textAltarOnly);
        o.addProperty("textAltarBuild", textAltarBuild);
        o.addProperty("textPortalOnly", textPortalOnly);
        o.addProperty("textPortalBuild", textPortalBuild);
        return o;
    }

    private static WorldRules load() {
        try {
            if (Files.exists(file())) {
                WorldRules w = parse(JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject());
                if (w.migrated384) {
                    w.migrated384 = false;
                    Files.writeString(file(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(w.toJson()), StandardCharsets.UTF_8);
                    RtShop.LOGGER.info("[rtshop] world_rules.json 을 새 판으로 고쳤습니다: 마법사가 아니어도 주문을 쓰되 약하게 (magicOnlyForMage=false).");
                }
                return w;
            }
            WorldRules w = defaults();
            save(w);
            return w;
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] world_rules.json 을 읽지 못했습니다: {}", e.toString());
            return defaults();
        }
    }

    public static synchronized void save(WorldRules w) {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(w.toJson()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] world_rules.json 을 쓰지 못했습니다: {}", e.toString());
        }
        current = w;
        publish(w);
    }

    /** 시험용: 파일을 건드리지 않고 규칙을 바꾼다. */
    public static synchronized void setForTest(WorldRules w) {
        current = w;
        publish(w);
    }

    // ------------------------------------------------------------------ 묻는 곳

    /** 지형 생성이 묻는다 (아무 스레드). */
    public static boolean neverStructure(String id) {
        if (current == null) get();
        return never.contains(id);
    }

    public static boolean recipeBlocked(ResourceLocation id, ResourceLocation output) {
        WorldRules w = get();
        if (w.neverRecipes.isEmpty()) return false;
        return w.neverRecipes.contains(id.toString()) || (output != null && w.neverRecipes.contains("item:" + output));
    }

    /** 전리품에서 빠져야 하는 아이템인가. */
    public static boolean lootBlocked(ItemStack s) {
        if (current == null) get();
        Set<String> m = lootMods;
        if (m.isEmpty() || s.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(s.getItem());
        return rl != null && m.contains(rl.getNamespace());
    }

    /** 이 사람이 지금 선 자리에서 나선 비경으로 들어가도 되는가 (정해 둔 문 곁인가). 문을 아직 정하지 않았으면 어디서든 된다. */
    public static boolean portalAllowed(ServerPlayer p) {
        WorldRules w = get();
        if (!w.abyssPortalSet) return true;
        String dim = p.level().dimension().location().toString();
        if (dim.equals(ABYSS_DIM)) return true; // 이미 안에 있다 (층 이동)
        if (p.isCreative() && com.rtshop.util.Perm.op(p)) return true;
        if (!dim.equals(w.abyssPortalDim)) return false;
        double dx = p.getX() - (w.abyssPortalX + 0.5), dy = p.getY() - w.abyssPortalY, dz = p.getZ() - (w.abyssPortalZ + 0.5);
        return dx * dx + dz * dz <= (double) w.abyssPortalRadius * w.abyssPortalRadius && Math.abs(dy) <= w.abyssPortalRadius;
    }

    /** 막아야 하면 알리고 true. */
    public static boolean refusePortal(ServerPlayer p) {
        try {
            if (portalAllowed(p)) return false;
            com.rtshop.notice.Notice.send(p, Component.literal(com.rtshop.story.StoryText.t(p, get().textPortalOnly)).withStyle(ChatFormatting.YELLOW));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 이 자리의 룬 제단을 써도 되는가 (제단을 정해 두었으면 그것만). */
    public static boolean altarAllowed(Level level, BlockPos pos) {
        WorldRules w = get();
        if (!w.altarSet) return true;
        return level.dimension().location().toString().equals(w.altarDim) && pos.getX() == w.altarX && pos.getY() == w.altarY && pos.getZ() == w.altarZ;
    }

    /** 막아야 하면 알리고 true. */
    public static boolean refuseAltar(ServerPlayer p, BlockPos pos) {
        try {
            if (altarAllowed(p.level(), pos)) return false;
            com.rtshop.notice.Notice.send(p, Component.literal(com.rtshop.story.StoryText.t(p, get().textAltarOnly)).withStyle(ChatFormatting.YELLOW), true);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 이 자리의 강화 모루를 써도 되는가 (모루를 정해 두었으면 그것만). */
    public static boolean anvilAllowed(Level level, BlockPos pos) {
        WorldRules w = get();
        if (!w.anvilSet) return true;
        return level.dimension().location().toString().equals(w.anvilDim) && pos.getX() == w.anvilX && pos.getY() == w.anvilY && pos.getZ() == w.anvilZ;
    }

    /** 막아야 하면 알리고 true. */
    public static boolean refuseAnvil(ServerPlayer p, BlockPos pos) {
        try {
            if (anvilAllowed(p.level(), pos)) return false;
            com.rtshop.notice.Notice.send(p, Component.literal(com.rtshop.story.StoryText.t(p, get().textAnvilOnly)).withStyle(ChatFormatting.YELLOW), true);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setAnvil(Level level, BlockPos pos) {
        WorldRules w = get();
        w.anvilSet = true;
        w.anvilDim = level.dimension().location().toString();
        w.anvilX = pos.getX();
        w.anvilY = pos.getY();
        w.anvilZ = pos.getZ();
        save(w);
    }

    public static void clearAnvil() {
        WorldRules w = get();
        w.anvilSet = false;
        save(w);
    }

    public static void setAltar(Level level, BlockPos pos) {
        WorldRules w = get();
        w.altarSet = true;
        w.altarDim = level.dimension().location().toString();
        w.altarX = pos.getX();
        w.altarY = pos.getY();
        w.altarZ = pos.getZ();
        save(w);
    }

    public static void setPortal(Level level, BlockPos pos) {
        WorldRules w = get();
        w.abyssPortalSet = true;
        w.abyssPortalDim = level.dimension().location().toString();
        w.abyssPortalX = pos.getX();
        w.abyssPortalY = pos.getY();
        w.abyssPortalZ = pos.getZ();
        save(w);
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        try {
            ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
            if (rl != null && rl.toString().equals(ALTAR_KEY)) {
                WorldRules wa = get();
                boolean opA = event.getEntity() instanceof ServerPlayer pa && pa.isCreative() && com.rtshop.util.Perm.op(pa);
                if (wa.altarOpOnly && !opA) {
                    event.setCanceled(true);
                    if (event.getEntity() instanceof ServerPlayer pa) com.rtshop.notice.Notice.send(pa, Component.literal(com.rtshop.story.StoryText.t(pa, wa.textAltarBuild)).withStyle(ChatFormatting.YELLOW), true);
                } else if (opA && !wa.altarSet && event.getLevel() instanceof Level la) {
                    setAltar(la, event.getPos());
                    ((ServerPlayer) event.getEntity()).sendSystemMessage(Component.literal(com.rtshop.story.StoryText.pick((ServerPlayer) event.getEntity(), "이 제단을 마을의 단 하나뿐인 룬 제단으로 정했습니다. (바꾸려면 새 제단 곁에서 /rtshop altar here)", "This altar is now the town's one and only Rune Altar. (To change it, stand by the new altar and use /rtshop altar here)")).withStyle(ChatFormatting.GREEN));
                }
                return;
            }
            if (rl != null && rl.toString().equals(ANVIL_KEY)) {
                WorldRules wa = get();
                boolean opA = event.getEntity() instanceof ServerPlayer pa && pa.isCreative() && com.rtshop.util.Perm.op(pa);
                if (wa.anvilOpOnly && !opA) {
                    event.setCanceled(true);
                    if (event.getEntity() instanceof ServerPlayer pa) com.rtshop.notice.Notice.send(pa, Component.literal(com.rtshop.story.StoryText.t(pa, wa.textAnvilBuild)).withStyle(ChatFormatting.YELLOW), true);
                } else if (opA && !wa.anvilSet && event.getLevel() instanceof Level la) {
                    setAnvil(la, event.getPos());
                    ((ServerPlayer) event.getEntity()).sendSystemMessage(Component.literal(com.rtshop.story.StoryText.pick((ServerPlayer) event.getEntity(), "이 모루를 마을의 단 하나뿐인 강화 모루로 정했습니다. (바꾸려면 새 모루 곁에서 /rtshop anvil here)", "This anvil is now the town's one and only Enhancement Anvil. (To change it, stand by the new anvil and use /rtshop anvil here)")).withStyle(ChatFormatting.GREEN));
                }
                return;
            }
            if (rl == null || !rl.toString().equals(ABYSS_KEY)) return;
            WorldRules w = get();
            if (!(event.getEntity() instanceof ServerPlayer p)) {
                if (w.abyssPortalOpOnly) event.setCanceled(true);
                return;
            }
            boolean op = p.isCreative() && com.rtshop.util.Perm.op(p);
            if (w.abyssPortalOpOnly && !op) {
                event.setCanceled(true);
                com.rtshop.notice.Notice.send(p, Component.literal(com.rtshop.story.StoryText.t(p, w.textPortalBuild)).withStyle(ChatFormatting.YELLOW));
                return;
            }
            if (op && !w.abyssPortalSet && event.getLevel() instanceof Level l) {
                setPortal(l, event.getPos());
                p.sendSystemMessage(Component.literal(com.rtshop.story.StoryText.pick(p, "이 문을 나선 비경으로 가는 단 하나의 문으로 정했습니다. (바꾸려면 새 문 앞에서 /rtshop abyss portal here)", "This portal is now the only way into the Spiral Abyss. (To change it, stand at the new portal and use /rtshop abyss portal here)")).withStyle(ChatFormatting.GREEN));
            }
        } catch (Throwable ignored) {
        }
    }

    /** 룬 제단은 운영자(크리에이티브)만 치울 수 있다. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        try {
            ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
            if (rl != null && rl.toString().equals(ANVIL_KEY) && get().anvilOpOnly) { // 강화 모루도 운영자(크리에이티브)만 치운다
                if (event.getPlayer() instanceof ServerPlayer p && p.isCreative() && com.rtshop.util.Perm.op(p)) {
                    WorldRules w = get();
                    if (w.anvilSet && anvilAllowed(p.level(), event.getPos())) {
                        w.anvilSet = false;
                        save(w);
                    }
                    return;
                }
                event.setCanceled(true);
                return;
            }
            if (rl == null || !rl.toString().equals(ALTAR_KEY) || !get().altarOpOnly) return;
            if (event.getPlayer() instanceof ServerPlayer p && p.isCreative() && com.rtshop.util.Perm.op(p)) {
                WorldRules w = get();
                if (w.altarSet && altarAllowed(p.level(), event.getPos())) { // 정해 둔 제단을 치웠다: 지정을 푼다 (다음에 놓는 것이 새 제단이 된다)
                    w.altarSet = false;
                    save(w);
                }
                return;
            }
            event.setCanceled(true);
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            if (!event.getDimension().location().toString().equals(ABYSS_DIM)) return;
            if (refusePortal(p)) event.setCanceled(true);
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onAnvilCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("anvil").requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    WorldRules w = get();
                    ctx.getSource().sendSuccess(() -> Component.literal(w.anvilSet
                            ? "강화 모루: " + w.anvilDim + " " + w.anvilX + " " + w.anvilY + " " + w.anvilZ + " (이 모루만 쓸 수 있음)"
                            : "강화 모루가 아직 정해지지 않았습니다. 모루 곁에서 /rtshop anvil here (정하기 전에는 어느 모루든 열립니다)"), false);
                    return 1;
                })
                .then(Commands.literal("here").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    BlockPos found = null;
                    for (BlockPos b : BlockPos.betweenClosed(p.blockPosition().offset(-5, -3, -5), p.blockPosition().offset(5, 3, 5))) {
                        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(p.level().getBlockState(b).getBlock());
                        if (rl != null && rl.toString().equals(ANVIL_KEY) && (found == null || b.distSqr(p.blockPosition()) < found.distSqr(p.blockPosition()))) found = b.immutable();
                    }
                    if (found == null) {
                        ctx.getSource().sendFailure(Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(), "5칸 안에 강화 모루가 없습니다. 모루 곁에서 다시 입력하세요.", "No Enhancement Anvil within 5 blocks. Stand next to the anvil and try again.")));
                        return 0;
                    }
                    setAnvil(p.level(), found);
                    ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(), "이 모루를 단 하나뿐인 강화 모루로 정했습니다. 다른 모루는 쓸 수 없습니다.", "This anvil is now the one and only Enhancement Anvil. No other anvil can be used.")).withStyle(ChatFormatting.GREEN), true);
                    return 1;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    WorldRules w = get();
                    w.anvilSet = false;
                    save(w);
                    ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(), "모루 지정을 풀었습니다. 지금은 어느 강화 모루든 쓸 수 있습니다.", "The anvil is no longer fixed. Any Enhancement Anvil can be used now.")), true);
                    return 1;
                }))));
    }

    @SubscribeEvent
    public static void onAltarCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("altar").requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    WorldRules w = get();
                    ctx.getSource().sendSuccess(() -> Component.literal(w.altarSet
                            ? "룬 제단: " + w.altarDim + " " + w.altarX + " " + w.altarY + " " + w.altarZ + " (이 제단만 쓸 수 있음)"
                            : "룬 제단이 아직 정해지지 않았습니다. 제단 곁에서 /rtshop altar here"), false);
                    return 1;
                })
                .then(Commands.literal("here").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    BlockPos found = null;
                    for (BlockPos b : BlockPos.betweenClosed(p.blockPosition().offset(-5, -3, -5), p.blockPosition().offset(5, 3, 5))) {
                        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(p.level().getBlockState(b).getBlock());
                        if (rl != null && rl.toString().equals(ALTAR_KEY) && (found == null || b.distSqr(p.blockPosition()) < found.distSqr(p.blockPosition()))) found = b.immutable();
                    }
                    if (found == null) {
                        ctx.getSource().sendFailure(Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(), "5칸 안에 별빛 수정 구체가 없습니다. 제단 곁에서 다시 입력하세요.", "No Starlight Crystal Orb within 5 blocks. Stand next to the altar and try again.")));
                        return 0;
                    }
                    setAltar(p.level(), found);
                    ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(), "이 제단을 단 하나뿐인 룬 제단으로 정했습니다. 다른 제단은 쓸 수 없습니다.", "This altar is now the one and only Rune Altar. No other altar can be used.")).withStyle(ChatFormatting.GREEN), true);
                    return 1;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    WorldRules w = get();
                    w.altarSet = false;
                    save(w);
                    ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(), "제단 지정을 풀었습니다. 지금은 어느 제단이든 쓸 수 있습니다.", "The altar is no longer fixed. Any altar can be used now.")), true);
                    return 1;
                }))));
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("abyss").requires(s -> s.hasPermission(2))
                .then(Commands.literal("portal")
                        .executes(ctx -> {
                            WorldRules w = get();
                            ctx.getSource().sendSuccess(() -> Component.literal(w.abyssPortalSet
                                    ? "나선 비경의 문: " + w.abyssPortalDim + " " + w.abyssPortalX + " " + w.abyssPortalY + " " + w.abyssPortalZ + " (반경 " + w.abyssPortalRadius + "칸 안에서만 입장)"
                                    : "나선 비경의 문이 아직 정해지지 않았습니다. 문 앞에서 /rtshop abyss portal here"), false);
                            return 1;
                        })
                        .then(Commands.literal("here").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            setPortal(p.level(), p.blockPosition());
                            ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(), "여기를 나선 비경으로 가는 단 하나의 문으로 정했습니다. 다른 곳의 문으로는 들어갈 수 없습니다.", "This spot is now the only portal into the Spiral Abyss. Portals anywhere else will not let anyone in.")).withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))
                        .then(Commands.literal("clear").executes(ctx -> {
                            WorldRules w = get();
                            w.abyssPortalSet = false;
                            save(w);
                            ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(), "문 지정을 풀었습니다. 지금은 어느 문으로든 들어갈 수 있습니다.", "The portal is no longer fixed. Any portal leads in now.")), true);
                            return 1;
                        })))));
    }
}
