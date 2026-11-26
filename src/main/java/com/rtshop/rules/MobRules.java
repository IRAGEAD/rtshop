package com.rtshop.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.party.BossCredit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 몹 규칙 (config/rtshop/mob_rules.json).
 * <ul>
 * <li><b>자연 스폰 줄이기</b>: 바닐라 몹이 아닌 모드 몹은 자연 스폰의 일부만 남긴다 (기본 20% = 약 80% 감소). 이름에 따라 따로 정할 수 있다
 *     (기본: lycanth 10%). 보스, 이름에 hollow 가 들어가는 몹(기사), 구조물 안의 스폰, 스포너·소환·이야기 연출의 스폰은 건드리지 않는다.</li>
 * <li><b>드랍 정리</b>: 보스가 아닌 몹은 무기, 스킬북, 인챈트 북·인챈트 걸린 장비, 모드 아이템을 떨어뜨리지 않는다.
 *     퀘스트가 건네 달라고 하는 아이템과 keepItems 에 적은 것은 남긴다.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class MobRules {
    private MobRules() {
    }

    public static final class Config {
        public boolean spawnEnabled = true;
        /** 모드 몹의 자연 스폰 가운데 남기는 비율 (0~1). */
        public double modMobKeep = 0.2;
        /** 이름(ID 또는 영어 이름)에 이 글이 들어가면 그 비율을 쓴다. */
        public final Map<String, Double> keepByName = new LinkedHashMap<>();
        /** 이름에 이 글이 들어가는 몹은 줄이지 않는다. */
        public final List<String> spawnExempt = new ArrayList<>();
        /** 이 모드의 몹은 줄이지 않는다. */
        public final List<String> spawnExemptMods = new ArrayList<>();
        public boolean dropsEnabled = true;
        public boolean removeWeapons = true, removeSkillBooks = true, removeEnchanted = true, removeModItems = true;
        /** 남겨 두는 아이템(ID) 또는 모드("모드ID:*"). */
        public final List<String> keepItems = new ArrayList<>();
        /** 최대 체력이 이 값 이상이면 보스로 친다 (0 = 쓰지 않음). */
        public double bossHealth = 300;
        public final List<String> extraBosses = new ArrayList<>();

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("_설명", "몹 규칙. spawn: 모드 몹(바닐라가 아닌 몹)의 자연 스폰 가운데 modMobKeep 만큼만 남긴다 (0.2 = 약 80% 감소). keepByName: 이름(ID 나 영어 이름)에 그 글이 들어가는 몹의 비율. "
                    + "exempt: 이름에 그 글이 들어가면 줄이지 않는다 (hollow = 기사). exemptMods: 줄이지 않는 모드. 보스·구조물 안·스포너·소환은 언제나 그대로. "
                    + "drops: 보스가 아닌 몹의 드랍에서 무기·스킬북·인챈트·모드 아이템을 뺀다. keepItems: 남길 아이템 ID 또는 '모드ID:*'. 퀘스트가 건네 달라는 아이템은 자동으로 남는다. "
                    + "bossHealth: 최대 체력이 이 이상이면 보스로 본다. extraBosses: 보스로 볼 몹 ID. 고친 뒤 /rtshop reload.");
            JsonObject s = new JsonObject();
            s.addProperty("enabled", spawnEnabled);
            s.addProperty("modMobKeep", modMobKeep);
            JsonObject kb = new JsonObject();
            keepByName.forEach(kb::addProperty);
            s.add("keepByName", kb);
            s.add("exempt", arr(spawnExempt));
            s.add("exemptMods", arr(spawnExemptMods));
            o.add("spawn", s);
            JsonObject d = new JsonObject();
            d.addProperty("enabled", dropsEnabled);
            d.addProperty("removeWeapons", removeWeapons);
            d.addProperty("removeSkillBooks", removeSkillBooks);
            d.addProperty("removeEnchanted", removeEnchanted);
            d.addProperty("removeModItems", removeModItems);
            d.add("keepItems", arr(keepItems));
            o.add("drops", d);
            o.addProperty("bossHealth", bossHealth);
            o.add("extraBosses", arr(extraBosses));
            return o;
        }
    }

    private static JsonArray arr(List<String> l) {
        JsonArray a = new JsonArray();
        l.forEach(a::add);
        return a;
    }

    private static void list(JsonObject o, String k, List<String> into) {
        if (!o.has(k) || !o.get(k).isJsonArray()) return;
        into.clear();
        o.getAsJsonArray(k).forEach(e -> into.add(e.getAsString()));
    }

    public static Config fresh() {
        Config c = new Config();
        c.keepByName.put("lycanth", 0.1);
        c.spawnExempt.add("hollow");
        c.keepItems.add("rtshop:*");
        return c;
    }

    private static Config config;

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("mob_rules.json");
    }

    public static synchronized Config get() {
        if (config == null) config = load();
        return config;
    }

    public static synchronized void reload() {
        config = load();
        questItems = null;
    }

    public static synchronized void setForTest(Config c) {
        config = c;
        questItems = null;
    }

    private static Config load() {
        Config c = fresh();
        Path p = path();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("spawn")) {
                    JsonObject s = o.getAsJsonObject("spawn");
                    if (s.has("enabled")) c.spawnEnabled = s.get("enabled").getAsBoolean();
                    if (s.has("modMobKeep")) c.modMobKeep = Math.max(0, Math.min(1, s.get("modMobKeep").getAsDouble()));
                    if (s.has("keepByName")) {
                        c.keepByName.clear();
                        s.getAsJsonObject("keepByName").entrySet().forEach(e -> c.keepByName.put(e.getKey().toLowerCase(Locale.ROOT), Math.max(0, Math.min(1, e.getValue().getAsDouble()))));
                    }
                    list(s, "exempt", c.spawnExempt);
                    list(s, "exemptMods", c.spawnExemptMods);
                }
                if (o.has("drops")) {
                    JsonObject d = o.getAsJsonObject("drops");
                    if (d.has("enabled")) c.dropsEnabled = d.get("enabled").getAsBoolean();
                    if (d.has("removeWeapons")) c.removeWeapons = d.get("removeWeapons").getAsBoolean();
                    if (d.has("removeSkillBooks")) c.removeSkillBooks = d.get("removeSkillBooks").getAsBoolean();
                    if (d.has("removeEnchanted")) c.removeEnchanted = d.get("removeEnchanted").getAsBoolean();
                    if (d.has("removeModItems")) c.removeModItems = d.get("removeModItems").getAsBoolean();
                    list(d, "keepItems", c.keepItems);
                }
                if (o.has("bossHealth")) c.bossHealth = Math.max(0, o.get("bossHealth").getAsDouble());
                list(o, "extraBosses", c.extraBosses);
            } else {
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] mob_rules.json 을 읽지 못했습니다: {}", e.getMessage());
        }
        return c;
    }

    // ------------------------------------------------------------------ 보스인가

    public static boolean boss(Entity e) {
        if (!(e instanceof LivingEntity l)) return false;
        if (BossCredit.isBoss(l) || l.getType().is(Tags.EntityTypes.BOSSES)) return true;
        Config c = get();
        if (c.extraBosses.contains(BossCredit.id(l))) return true;
        return c.bossHealth > 0 && l.getMaxHealth() >= c.bossHealth;
    }

    // ------------------------------------------------------------------ 자연 스폰

    /** 이 종류의 몹이 자연 스폰될 때 남는 비율 (1 = 그대로). */
    public static double keepChance(EntityType<?> type) {
        Config c = get();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (!c.spawnEnabled || id == null || id.getNamespace().equals("minecraft") || id.getNamespace().equals(RtShop.MODID)) return 1;
        if (c.spawnExemptMods.contains(id.getNamespace())) return 1;
        String name = (id + " " + type.getDescription().getString()).toLowerCase(Locale.ROOT);
        for (String ex : c.spawnExempt) if (!ex.isBlank() && name.contains(ex.toLowerCase(Locale.ROOT))) return 1;
        if (type.is(Tags.EntityTypes.BOSSES) || com.rtshop.party.PartyConfig.get().bosses.contains(id.toString())
                || com.rtshop.party.PartyConfig.get().arenaBosses.contains(id.toString()) || c.extraBosses.contains(id.toString())) return 1;
        for (Map.Entry<String, Double> e : c.keepByName.entrySet()) if (name.contains(e.getKey())) return e.getValue();
        return c.modMobKeep;
    }

    /** 점검용: 굴림을 정한다 (음수 = 진짜 무작위). */
    public static double testRoll = -1;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        try {
            MobSpawnType t = event.getSpawnType();
            if (t != MobSpawnType.NATURAL && t != MobSpawnType.CHUNK_GENERATION && t != MobSpawnType.REINFORCEMENT) return;
            double keep = keepChance(event.getEntity().getType());
            if (keep >= 1) return;
            if (event.getLevel() instanceof ServerLevel sl && sl.structureManager().hasAnyStructureAt(event.getEntity().blockPosition())) return; // 구조물 안은 그대로
            double r = testRoll >= 0 ? testRoll : event.getEntity().getRandom().nextDouble();
            if (r >= keep) {
                event.setSpawnCancelled(true);
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 드랍

    private static Set<String> questItems;

    /** 퀘스트가 건네 달라고 하는 아이템 (몹이 떨어뜨려도 남겨 둔다). */
    private static synchronized Set<String> questItems() {
        if (questItems == null) {
            Set<String> out = new HashSet<>();
            try {
                for (com.rtshop.quest.QuestDef q : com.rtshop.quest.QuestManager.loaded().quests.values()) {
                    for (com.rtshop.quest.QuestDef.Objective ob : q.objectives) {
                        if (ob.type == com.rtshop.quest.QuestDef.Type.DELIVER && !ob.target.startsWith("#")) out.add(ob.target);
                    }
                }
            } catch (Throwable ignored) {
            }
            questItems = out;
        }
        return questItems;
    }

    private static boolean weapon(ItemStack s) {
        return s.getItem() instanceof SwordItem || s.getItem() instanceof net.minecraft.world.item.AxeItem || s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem || s.getItem() instanceof TridentItem
                || com.rtshop.compat.EfWeapons.capability(s) != null && !(s.getItem() instanceof net.minecraft.world.item.DiggerItem) && !s.isEdible()
                && s.getMaxStackSize() == 1 && !(s.getItem() instanceof net.minecraft.world.item.ArmorItem) && !(s.getItem() instanceof net.minecraft.world.item.ShieldItem);
    }

    /** 보스가 아닌 몹이 이 물건을 떨어뜨려도 되는가. */
    public static boolean dropAllowed(ItemStack s) {
        Config c = get();
        if (s.isEmpty() || !c.dropsEnabled) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        if (id == null) return true;
        if (c.keepItems.contains(id.toString()) || c.keepItems.contains(id.getNamespace() + ":*")) return true;
        if (ItemPolicy.lootSpared(s)) return true; // 자연 드랍으로 주기로 한 것 (잉크)
        if (c.removeSkillBooks && id.toString().equals(com.rtshop.magic.MagicSkills.BOOK)) return false;
        if (c.removeEnchanted && (s.getItem() instanceof EnchantedBookItem || s.isEnchanted())) return false;
        if (c.removeWeapons && weapon(s)) return false;
        // 모드 아이템: 퀘스트가 건네 달라는 재료만은 남긴다 (무기·인챈트 물건은 퀘스트에 쓰여도 몹이 떨어뜨리지 않는다)
        if (c.removeModItems && !id.getNamespace().equals("minecraft")) return questItems().contains(id.toString());
        return true;
    }

    /** 몹이 주워서 들고 있던 물건 (플레이어가 떨어뜨린 것): 죽을 때 반드시 떨어지는 것이므로 지우지 않는다. */
    private static final Map<Entity, List<ItemStack>> PICKED = new java.util.WeakHashMap<>();

    /** 주운 물건의 목록: 떨어질 확률이 100% 를 넘게 적힌 칸(몹이 주운 것의 표시)의 물건들. */
    public static List<ItemStack> pickedUp(net.minecraft.world.entity.Mob m) {
        List<ItemStack> out = new ArrayList<>();
        try {
            net.minecraft.nbt.CompoundTag tag = m.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            net.minecraft.nbt.ListTag hands = tag.getList("HandDropChances", net.minecraft.nbt.Tag.TAG_FLOAT), armor = tag.getList("ArmorDropChances", net.minecraft.nbt.Tag.TAG_FLOAT);
            net.minecraft.world.entity.EquipmentSlot[] hs = {net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.entity.EquipmentSlot.OFFHAND};
            net.minecraft.world.entity.EquipmentSlot[] as = {net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD};
            for (int i = 0; i < hs.length && i < hands.size(); i++) if (hands.getFloat(i) > 1F && !m.getItemBySlot(hs[i]).isEmpty()) out.add(m.getItemBySlot(hs[i]).copy());
            for (int i = 0; i < as.length && i < armor.size(); i++) if (armor.getFloat(i) > 1F && !m.getItemBySlot(as[i]).isEmpty()) out.add(m.getItemBySlot(as[i]).copy());
        } catch (Throwable ignored) {
        }
        return out;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob m && !m.level().isClientSide() && get().dropsEnabled) {
            List<ItemStack> held = pickedUp(m);
            if (!held.isEmpty()) {
                synchronized (PICKED) {
                    PICKED.put(m, held);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        try {
            LivingEntity e = event.getEntity();
            if (e.level().isClientSide() || e instanceof Player || com.rtshop.shop.NpcGuard.isNpc(e)) return;
            boolean isBoss = boss(e);
            try {
                ItemPolicy.mobDrops(e, isBoss, event.getSource().getEntity(), event.getDrops());
            } catch (Throwable ignored) {
            }
            // 3.65.0: 터무니없이 센 장비(Arcane Vortex 등)는 보스가 코드로 직접 떨어뜨려도 뺀다 (전리품표가 아닌 길)
            event.getDrops().removeIf(d -> ItemPolicy.extreme(d.getItem()));
            if (!get().dropsEnabled || isBoss) return;
            List<ItemStack> held;
            synchronized (PICKED) {
                held = PICKED.remove(e);
            }
            final List<ItemStack> mine = held == null ? List.of() : held;
            event.getDrops().removeIf(d -> !dropAllowed(d.getItem()) && mine.stream().noneMatch(h -> ItemStack.isSameItemSameTags(h, d.getItem())));
        } catch (Throwable ignored) {
        }
    }
}
