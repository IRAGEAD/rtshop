package com.rtshop.story;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rtshop.RtShop;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import com.rtshop.rules.ItemPolicy;
import com.rtshop.rules.LootRules;
import com.rtshop.rules.MobRules;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 도감 (config/rtshop/codex.json, 3.54): 아이템 「조사대 도감」을 들고 우클릭하면 펼쳐진다.
 * <ul>
 * <li><b>몹·보스</b>: 처음에는 잠겨 있다(???). 마주치면 모습과 이름이 적히고, 잡으면 능력치·전리품·설명까지 다 적힌다.</li>
 * <li><b>장비</b>: 손에 넣으면 모습과 능력·설명이 적힌다 (무기·방어구·도구·장신구, 어느 모드 것이든).</li>
 * <li>낚시 도감이 따로 있는 Starcatcher 의 것은 넣지 않는다 (excludeMods).</li>
 * </ul>
 * 기록은 세계에 저장된다 (죽어도 남는다). 처치 인정은 퀘스트·현상금과 같다 (QuestManager.onKill).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Codex {
    private Codex() {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static class Config {
        public String _help = "도감. excludeMods: 도감에 넣지 않을 모드 ID. meetRadius: 이 거리 안에서 눈에 보이면 '마주친' 것으로 적는다. "
                + "notes: 몹·아이템 ID → 도감에 적을 설명 (잡은 뒤·얻은 뒤에 보인다). 이야기 보스의 설명은 story.json 의 등장 연출 글에서 가져온다. "
                + "extraMobs / hideMobs: 목록에 더 넣거나 뺄 몹 ID. giveOnFirstJoin: 처음 들어온 사람에게 도감을 한 권 준다.";
        public boolean enabled = true;
        public List<String> excludeMods = new ArrayList<>(List.of("starcatcher"));
        public int meetRadius = 16;
        public boolean notify = true;
        public boolean giveOnFirstJoin = true;
        public List<String> extraMobs = new ArrayList<>();
        public List<String> hideMobs = new ArrayList<>();
        public Map<String, String> notes = new LinkedHashMap<>();
    }

    private static Config config;

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("codex.json");
    }

    public static synchronized Config get() {
        if (config == null) {
            Config c = null;
            Path f = file();
            try {
                if (Files.exists(f)) c = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Config.class);
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] codex.json 을 읽지 못해 기본값을 씁니다: {}", e.toString());
            }
            boolean write = c == null;
            if (c == null) c = new Config();
            if (c.excludeMods == null) c.excludeMods = new ArrayList<>();
            if (c.extraMobs == null) c.extraMobs = new ArrayList<>();
            if (c.hideMobs == null) c.hideMobs = new ArrayList<>();
            if (c.notes == null) c.notes = new LinkedHashMap<>();
            c.meetRadius = Math.max(4, Math.min(48, c.meetRadius));
            if (write) {
                try {
                    Files.createDirectories(f.getParent());
                    Files.writeString(f, GSON.toJson(c), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    RtShop.LOGGER.warn("[rtshop] codex.json 을 쓰지 못했습니다: {}", e.toString());
                }
            }
            config = c;
        }
        return config;
    }

    public static synchronized void reload() {
        config = null;
        mobs = null;
        gear = null;
        DROPS.clear();
    }

    // ================================================================== 목록

    private static volatile Map<String, Boolean> mobs;   // 몹 ID → 보스인가
    private static volatile List<String> gear;

    private static String id(EntityType<?> t) {
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(t);
        return rl == null ? "" : rl.toString();
    }

    private static boolean bossType(EntityType<?> t, String id) {
        return t.is(Tags.EntityTypes.BOSSES) || MobRules.get().extraBosses.contains(id)
                || com.rtshop.party.PartyConfig.get().bosses.contains(id) || com.rtshop.party.PartyConfig.get().arenaBosses.contains(id)
                || StoryConfig.get().intros.containsKey(id);
    }

    /** 도감에 오르는 몹: 살아 움직이는 것 가운데 몬스터·동물·물짐승, 그리고 보스. (갑옷 거치대·플레이어·탄환 같은 것은 빠진다.) */
    @SuppressWarnings("unchecked")
    public static Map<String, Boolean> mobs() {
        Map<String, Boolean> m = mobs;
        if (m != null) return m;
        Config c = get();
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (EntityType<?> t : ForgeRegistries.ENTITY_TYPES) {
            String id = id(t);
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(t);
            if (rl == null || c.excludeMods.contains(rl.getNamespace()) || c.hideMobs.contains(id)) continue;
            if (t == EntityType.PLAYER || t == EntityType.ARMOR_STAND) continue;
            boolean living;
            try {
                living = DefaultAttributes.hasSupplier(t);
            } catch (Throwable e) {
                living = false;
            }
            if (!living) continue;
            boolean boss = bossType(t, id);
            if (t.getCategory() == MobCategory.MISC && !boss && !c.extraMobs.contains(id) && t != EntityType.VILLAGER
                    && t != EntityType.IRON_GOLEM && t != EntityType.SNOW_GOLEM) continue;
            if (rl.getNamespace().equals(RtShop.MODID)) continue;
            out.put(id, boss);
        }
        mobs = out;
        return out;
    }

    /** 도감에 오르는 장비: 무기·방어구·도구·장신구. */
    public static List<String> gear() {
        List<String> g = gear;
        if (g != null) return g;
        Config c = get();
        List<String> out = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null || item == Items.AIR || c.excludeMods.contains(rl.getNamespace())) continue;
            if (ItemPolicy.gear(item)) out.add(rl.toString());
        }
        gear = out;
        return out;
    }

    // ================================================================== 기록

    public static class Rec {
        public final Set<String> met = new LinkedHashSet<>(), gear = new LinkedHashSet<>();
        public final Map<String, Integer> kills = new LinkedHashMap<>();
        public final Map<String, Long> first = new LinkedHashMap<>();
        public boolean given, guideGiven;
    }

    public static class Data extends SavedData {
        final Map<UUID, Rec> recs = new HashMap<>();

        Rec of(UUID id) {
            return recs.computeIfAbsent(id, k -> new Rec());
        }

        static Data load(CompoundTag t) {
            Data d = new Data();
            ListTag ps = t.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < ps.size(); i++) {
                CompoundTag c = ps.getCompound(i);
                Rec r = new Rec();
                for (Tag e : c.getList("met", Tag.TAG_STRING)) r.met.add(e.getAsString());
                for (Tag e : c.getList("gear", Tag.TAG_STRING)) r.gear.add(e.getAsString());
                CompoundTag k = c.getCompound("kills"), f = c.getCompound("first");
                for (String key : k.getAllKeys()) r.kills.put(key, k.getInt(key));
                for (String key : f.getAllKeys()) r.first.put(key, f.getLong(key));
                r.given = c.getBoolean("given");
                r.guideGiven = c.getBoolean("guide");
                d.recs.put(c.getUUID("id"), r);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            ListTag ps = new ListTag();
            for (Map.Entry<UUID, Rec> e : recs.entrySet()) {
                Rec r = e.getValue();
                CompoundTag c = new CompoundTag();
                c.putUUID("id", e.getKey());
                ListTag met = new ListTag(), g = new ListTag();
                for (String s : r.met) met.add(StringTag.valueOf(s));
                for (String s : r.gear) g.add(StringTag.valueOf(s));
                c.put("met", met);
                c.put("gear", g);
                CompoundTag k = new CompoundTag(), f = new CompoundTag();
                r.kills.forEach(k::putInt);
                r.first.forEach(f::putLong);
                c.put("kills", k);
                c.put("first", f);
                c.putBoolean("given", r.given);
                c.putBoolean("guide", r.guideGiven);
                ps.add(c);
            }
            t.put("players", ps);
            return t;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rtshop_codex");
    }

    public static Rec rec(ServerPlayer p) {
        return data(p.server).of(p.getUUID());
    }

    /** 시험용: 가짜 플레이어도 적는다. */
    public static volatile boolean testFakes;

    private static boolean real(ServerPlayer p) {
        return p != null && p.server != null && get().enabled && (testFakes || !(p instanceof FakePlayer));
    }

    /** 0 = 모름, 1 = 마주침, 2 = 잡음. */
    public static int state(Rec r, String mob) {
        return r.kills.getOrDefault(mob, 0) > 0 ? 2 : r.met.contains(mob) ? 1 : 0;
    }

    /** 마주쳤다. 새로 적었으면 true. */
    public static boolean meet(ServerPlayer p, LivingEntity e) {
        if (!real(p)) return false;
        String id = id(e.getType());
        if (!mobs().containsKey(id)) return false;
        Rec r = rec(p);
        if (!r.met.add(id)) return false;
        data(p.server).setDirty();
        if (get().notify && r.kills.getOrDefault(id, 0) == 0) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.codex.met", e.getType().getDescription()).withStyle(ChatFormatting.GRAY), true);
        }
        return true;
    }

    /** 처치로 인정된 사람마다 한 번 (QuestManager.onKill). */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        if (!real(p)) return;
        String id = id(victim.getType());
        if (!mobs().containsKey(id)) return;
        Rec r = rec(p);
        r.met.add(id);
        int n = r.kills.merge(id, 1, Integer::sum);
        if (n == 1) {
            r.first.put(id, System.currentTimeMillis());
            if (get().notify) {
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.codex.killed", victim.getType().getDescription()).withStyle(ChatFormatting.GREEN), true);
            }
        }
        data(p.server).setDirty();
    }

    /** 지니고 있는 장비를 적는다. 새로 적은 수. */
    public static int scanGear(ServerPlayer p) {
        if (!real(p)) return 0;
        Config c = get();
        Rec r = rec(p);
        int added = 0;
        List<ItemStack> all = new ArrayList<>(p.getInventory().items);
        all.addAll(p.getInventory().armor);
        all.addAll(p.getInventory().offhand);
        for (ItemStack s : all) {
            if (s.isEmpty()) continue;
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (rl == null || c.excludeMods.contains(rl.getNamespace()) || r.gear.contains(rl.toString()) || !ItemPolicy.gear(s.getItem())) continue;
            r.gear.add(rl.toString());
            added++;
            if (c.notify) {
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.codex.gear", s.getItem().getDescription()).withStyle(ChatFormatting.GREEN), true);
            }
        }
        if (added > 0) data(p.server).setDirty();
        return added;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        int t = p.tickCount + (p.getId() & 31);
        if (t % 40 != 0 || !real(p) || p.isSpectator()) return;
        try {
            Rec r = rec(p);
            Map<String, Boolean> list = mobs();
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(get().meetRadius), LivingEntity::isAlive)) {
                String id = id(m.getType());
                if (r.met.contains(id) || !list.containsKey(id)) continue;
                if (p.hasLineOfSight(m)) meet(p, m);
            }
            if (t % 120 == 0) scanGear(p);
        } catch (Throwable e) {
            RtShop.LOGGER.debug("[rtshop] codex tick", e);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !real(p) || !get().giveOnFirstJoin) return;
        Rec r = rec(p);
        if (r.given) return;
        r.given = true;
        data(p.server).setDirty();
        ItemStack book = new ItemStack(com.rtshop.item.ModItems.CODEX.get());
        if (!p.getInventory().contains(book) && !p.getInventory().add(book)) p.drop(book, false);
    }

    // ================================================================== 화면

    /** 도감 전체: 몹 [id, 보스, 상태, 처치 수], 장비 [id, 얻음]. */
    public static JsonObject view(ServerPlayer p) {
        Rec r = rec(p);
        JsonObject out = new JsonObject();
        JsonArray ms = new JsonArray();
        for (Map.Entry<String, Boolean> e : mobs().entrySet()) {
            JsonArray a = new JsonArray();
            a.add(e.getKey());
            a.add(e.getValue() ? 1 : 0);
            a.add(state(r, e.getKey()));
            a.add(r.kills.getOrDefault(e.getKey(), 0));
            ms.add(a);
        }
        out.add("mobs", ms);
        JsonArray gs = new JsonArray();
        for (String g : gear()) {
            JsonArray a = new JsonArray();
            a.add(g);
            a.add(r.gear.contains(g) ? 1 : 0);
            gs.add(a);
        }
        out.add("gear", gs);
        return out;
    }

    private static final Map<String, JsonArray> DROPS = new HashMap<>();

    /** 이 몹이 떨어뜨리는 것 (전리품표를 여러 번 열어 본 결과: [아이템, 가장 적게, 가장 많이, 나올 확률 %]). */
    static JsonArray drops(MinecraftServer server, EntityType<?> type, String id) {
        JsonArray cached = DROPS.get(id);
        if (cached != null) return cached;
        JsonArray out = new JsonArray();
        try {
            int n = 60;
            Map<String, LootRules.Stat> got = LootRules.sample(server.overworld(), type.getDefaultLootTable(), n, false);
            List<Map.Entry<String, LootRules.Stat>> list = new ArrayList<>(got.entrySet());
            list.sort((x, y) -> Integer.compare(y.getValue().hits, x.getValue().hits));
            for (Map.Entry<String, LootRules.Stat> e : list) {
                if (out.size() >= 18) break;
                JsonArray a = new JsonArray();
                a.add(e.getKey());
                a.add(e.getValue().min);
                a.add(e.getValue().max);
                a.add(Math.max(1, Math.round(e.getValue().hits * 100F / n)));
                out.add(a);
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] codex drops {}", id, t);
        }
        DROPS.put(id, out);
        return out;
    }

    /** 몹 하나의 자세한 내용 (잡은 뒤에만 능력치·전리품·설명이 들어간다). */
    public static JsonObject detail(ServerPlayer p, String id) {
        JsonObject out = new JsonObject();
        out.addProperty("id", id);
        Rec r = rec(p);
        int st = state(r, id);
        out.addProperty("state", st);
        ResourceLocation rl = ResourceLocation.tryParse(id);
        EntityType<?> type = rl == null || !ForgeRegistries.ENTITY_TYPES.containsKey(rl) ? null : ForgeRegistries.ENTITY_TYPES.getValue(rl);
        if (type == null || st < 2) return out;
        out.addProperty("kills", r.kills.getOrDefault(id, 0));
        out.addProperty("first", r.first.getOrDefault(id, 0L));
        try {
            @SuppressWarnings("unchecked")
            AttributeSupplier sup = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
            out.addProperty("hp", sup.getBaseValue(Attributes.MAX_HEALTH));
            if (sup.hasAttribute(Attributes.ATTACK_DAMAGE)) out.addProperty("atk", sup.getBaseValue(Attributes.ATTACK_DAMAGE));
            if (sup.hasAttribute(Attributes.ARMOR)) out.addProperty("armor", sup.getBaseValue(Attributes.ARMOR));
            if (sup.hasAttribute(Attributes.MOVEMENT_SPEED)) out.addProperty("speed", sup.getBaseValue(Attributes.MOVEMENT_SPEED));
        } catch (Throwable ignored) {
        }
        out.add("drops", drops(p.server, type, id));
        // 설명: 설정의 notes, 없으면 이야기 보스의 등장 글
        String note = get().notes.getOrDefault(id, "");
        StoryConfig.Intro in = StoryConfig.get().intros.get(id);
        if (in != null) {
            out.addProperty("epithet", in.epithet);
            if (note.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                if (!in.quote.isEmpty()) sb.append("“").append(in.quote).append("”");
                for (String l : in.lore) sb.append(sb.length() == 0 ? "" : "\n").append(l);
                note = sb.toString();
            }
        }
        out.addProperty("note", note);
        return out;
    }

    public static void open(ServerPlayer p) {
        if (!get().enabled) return;
        scanGear(p);
        if (p.connection == null || p instanceof FakePlayer) return;
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.CODEX, view(p).toString()));
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("codex")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    // 도감을 지니고 있어야 펼친다 (운영자는 없어도 된다)
                    if (!com.rtshop.util.Perm.op(p) && !p.getInventory().contains(new ItemStack(com.rtshop.item.ModItems.CODEX.get()))) {
                        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.codex.need_item").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    open(p);
                    return 1;
                })
                .then(Commands.literal("mob").then(Commands.argument("id", StringArgumentType.greedyString()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    JsonObject d = detail(p, StringArgumentType.getString(ctx, "id").trim());
                    ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.CODEX_MOB, StoryText.json(p, d).toString()));
                    return 1;
                })))));
    }

    // ---- 시험용
    public static void resetForTest(ServerPlayer p) {
        data(p.server).recs.remove(p.getUUID());
    }
}
