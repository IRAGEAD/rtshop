package com.rtshop.quest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * NPC 자리 배치 (3.58): 게임 안에서 세운 NPC 의 자리를 config/rtshop/npc_layout.json 에 저절로 적어 두고,
 * 그 파일을 가진 다른 서버(다른 세계)에서는 같은 자리에 NPC 를 저절로 세운다.
 * <ul>
 * <li>적기: NPC 를 소환하거나, 사람 가까이(48칸)에 있는 NPC 를 5초마다 살펴 자리가 달라졌으면 파일을 고쳐 쓴다. NPC 를 없애면 파일에서도 빠진다.</li>
 * <li>적용: 서버가 켜질 때 파일의 id 가 이 세계가 마지막으로 적용한 id 와 다르면 적용한다.
 *     없는 NPC 는 세우고, 자리가 달라진 NPC 는 옛 것을 치우고 새 자리에 세운다. 파일에 없는 NPC 는 건드리지 않는다.</li>
 * <li>세우는 때: 그 자리의 청크가 실제로 돌고 있을 때 (누군가 가까이 왔을 때). 바닥이 없거나 막혀 있으면 세우지 않고 기다린다 (지형이 다른 세계).</li>
 * </ul>
 * 월드 데이터 rtshop_npc_layout. 파일의 "auto": false 로 끈다. 명령: /rtshop npc layout [save|apply]
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class NpcLayout extends SavedData {
    private static final String NAME = "rtshop_npc_layout";

    public static final class Spot {
        public String key = "", dim = "minecraft:overworld";
        public double x, y, z;
        public float yaw;
        /** 세우려 했으나 바닥이 없거나 막혀 있었다 (상태 보기에 쓴다). */
        public transient boolean blocked;

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("key", key);
            o.addProperty("dim", dim);
            o.addProperty("x", Math.round(x * 100) / 100.0);
            o.addProperty("y", Math.round(y * 100) / 100.0);
            o.addProperty("z", Math.round(z * 100) / 100.0);
            o.addProperty("yaw", Math.round(yaw * 10) / 10.0);
            return o;
        }

        static Spot fromJson(JsonObject o) {
            Spot s = new Spot();
            s.key = o.get("key").getAsString().trim();
            if (o.has("dim")) s.dim = o.get("dim").getAsString().trim();
            s.x = o.get("x").getAsDouble();
            s.y = o.get("y").getAsDouble();
            s.z = o.get("z").getAsDouble();
            if (o.has("yaw")) s.yaw = o.get("yaw").getAsFloat();
            return s;
        }

        CompoundTag toTag() {
            CompoundTag c = new CompoundTag();
            c.putString("key", key);
            c.putString("dim", dim);
            c.putDouble("x", x);
            c.putDouble("y", y);
            c.putDouble("z", z);
            c.putFloat("yaw", yaw);
            return c;
        }

        static Spot fromTag(CompoundTag c) {
            Spot s = new Spot();
            s.key = c.getString("key");
            s.dim = c.getString("dim");
            s.x = c.getDouble("x");
            s.y = c.getDouble("y");
            s.z = c.getDouble("z");
            s.yaw = c.getFloat("yaw");
            return s;
        }

        boolean sameAs(Spot o) {
            return key.equals(o.key) && dim.equals(o.dim) && Math.abs(x - o.x) < 0.02 && Math.abs(y - o.y) < 0.02 && Math.abs(z - o.z) < 0.02 && Math.abs(yaw - o.yaw) < 0.5F;
        }
    }

    /** 이 세계에 서 있는 NPC (개체 → 자리). */
    private final Map<UUID, Spot> known = new LinkedHashMap<>();
    /** 파일에서 왔고 아직 세우지 못한 자리. */
    private final List<Spot> pending = new ArrayList<>();
    /** 새 자리로 옮겨졌으니 눈에 띄는 대로 치울 옛 NPC. */
    private final Set<UUID> retired = new HashSet<>();
    private String appliedId = "";
    private boolean changed;

    /** 점검용: 시험 환경에서도 파일을 쓰고 적용한다. */
    public static boolean forceForTest;

    private static boolean live() {
        return forceForTest || !(Boolean.getBoolean("rtshop.selftest") || Boolean.getBoolean("rtshop.clienttest"));
    }

    public static NpcLayout get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NpcLayout::load, NpcLayout::new, NAME);
    }

    public static NpcLayout load(CompoundTag tag) {
        NpcLayout d = new NpcLayout();
        ListTag k = tag.getList("known", Tag.TAG_COMPOUND);
        for (int i = 0; i < k.size(); i++) {
            CompoundTag c = k.getCompound(i);
            if (c.hasUUID("id")) d.known.put(c.getUUID("id"), Spot.fromTag(c));
        }
        ListTag p = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < p.size(); i++) d.pending.add(Spot.fromTag(p.getCompound(i)));
        ListTag r = tag.getList("retired", Tag.TAG_COMPOUND);
        for (int i = 0; i < r.size(); i++) if (r.getCompound(i).hasUUID("id")) d.retired.add(r.getCompound(i).getUUID("id"));
        d.appliedId = tag.getString("applied");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag k = new ListTag();
        for (Map.Entry<UUID, Spot> e : known.entrySet()) {
            CompoundTag c = e.getValue().toTag();
            c.putUUID("id", e.getKey());
            k.add(c);
        }
        tag.put("known", k);
        ListTag p = new ListTag();
        for (Spot s : pending) p.add(s.toTag());
        tag.put("pending", p);
        ListTag r = new ListTag();
        for (UUID id : retired) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            r.add(c);
        }
        tag.put("retired", r);
        tag.putString("applied", appliedId);
        return tag;
    }

    // ------------------------------------------------------------------ 열쇠

    /** 배치에 적는 NPC 의 열쇠 (상점·은행·퀘스트·직업·출석·뽑기·레이드 안내인). 그 밖(플레이어 상점 직원 등)은 null. */
    public static String keyOf(Entity e) {
        if (!(e instanceof Villager)) return null;
        String k = QuestManager.npcKey(e);
        if (k == null && e.getTags().contains(com.rtshop.raid.Raid.GUIDE_TAG)) k = "raid_guide";
        return k;
    }

    /** 열쇠 → /rtshop npc 에 넣는 종류 이름. */
    public static String typeOf(String key) {
        return key.startsWith("shop_") ? key.substring(5) : key;
    }

    // ------------------------------------------------------------------ 파일

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("npc_layout.json");
    }

    public static final class FileData {
        public boolean auto = true;
        public String id = "";
        public final List<Spot> npcs = new ArrayList<>();
    }

    /** 파일을 읽는다. 없으면 jar 에 든 기본 배치(있을 때)를 꺼내 쓴다. 둘 다 없으면 null. */
    public static FileData read() {
        try {
            Path f = file();
            if (!Files.exists(f)) {
                try (InputStream in = NpcLayout.class.getResourceAsStream("/rtshop_defaults/npc_layout.json")) {
                    if (in == null) return null;
                    Files.createDirectories(f.getParent());
                    Files.write(f, in.readAllBytes());
                }
            }
            JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            FileData d = new FileData();
            if (o.has("auto")) d.auto = o.get("auto").getAsBoolean();
            if (o.has("id")) d.id = o.get("id").getAsString();
            if (o.has("npcs")) {
                for (JsonElement e : o.getAsJsonArray("npcs")) {
                    try {
                        Spot s = Spot.fromJson(e.getAsJsonObject());
                        if (!s.key.isEmpty()) d.npcs.add(s);
                    } catch (Exception bad) {
                        RtShop.LOGGER.warn("[rtshop] npc_layout.json 의 한 줄을 읽지 못했습니다: {}", e);
                    }
                }
            }
            return d;
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] npc_layout.json 을 읽지 못했습니다: {}", e.toString());
            return null;
        }
    }

    /** 이 세계의 배치(서 있는 NPC + 아직 세우지 못한 자리)를 파일로 적는다. 적은 수(실패하면 -1). */
    public static int export(MinecraftServer server) {
        NpcLayout d = get(server);
        try {
            FileData old = read();
            JsonObject o = new JsonObject();
            o.addProperty("_help", "NPC 자리 배치. 게임 안에서 NPC 를 세우거나 옮기면 저절로 고쳐 쓰입니다. 이 파일을 다른 서버의 config/rtshop 에 넣으면 그 서버가 켜질 때 같은 자리에 NPC 가 섭니다 "
                    + "(그 자리의 청크에 누가 가까이 갔을 때, 바닥이 있고 비어 있으면). auto: false 면 저절로 적용하지 않습니다. 상태 보기·다시 적기·다시 적용: /rtshop npc layout [save|apply]");
            o.addProperty("auto", old == null || old.auto);
            String id = Long.toString(System.currentTimeMillis(), 36) + "-" + Integer.toString(d.known.size() + d.pending.size(), 36);
            o.addProperty("id", id);
            JsonArray a = new JsonArray();
            List<Spot> all = new ArrayList<>(d.known.values());
            all.addAll(d.pending);
            all.sort((p, q) -> p.key.equals(q.key) ? Double.compare(p.x, q.x) : p.key.compareTo(q.key));
            for (Spot s : all) a.add(s.toJson());
            o.add("npcs", a);
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            d.appliedId = id;
            d.changed = false;
            d.setDirty();
            return a.size();
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] npc_layout.json 을 쓰지 못했습니다: {}", e.toString());
            return -1;
        }
    }

    // ------------------------------------------------------------------ 적기

    /** NPC 가 여기 서 있다 (소환했을 때, 사람 가까이에서 5초마다). 치우기로 한 옛 NPC 면 치운다. */
    public static void note(Entity e) {
        if (e == null || e.level().isClientSide() || e.getServer() == null || !e.isAlive()) return;
        String key = keyOf(e);
        if (key == null || e.level().dimension() == com.rtshop.story.StoryArena.DIM) return;
        NpcLayout d = get(e.getServer());
        if (d.retired.remove(e.getUUID())) {
            d.known.remove(e.getUUID());
            d.setDirty();
            e.discard();
            return;
        }
        Spot s = new Spot();
        s.key = key;
        s.dim = e.level().dimension().location().toString();
        s.x = e.getX();
        s.y = e.getY();
        s.z = e.getZ();
        s.yaw = e.getYRot();
        Spot old = d.known.get(e.getUUID());
        if (old != null && old.sameAs(s)) return;
        d.known.put(e.getUUID(), s);
        d.changed = true;
        d.setDirty();
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || !(e instanceof Villager) || e.getServer() == null) return;
        if (e.getRemovalReason() == null || !e.getRemovalReason().shouldDestroy()) return;   // 청크가 내려간 것은 없어진 게 아니다
        try {
            NpcLayout d = get(e.getServer());
            if (d.known.remove(e.getUUID()) != null) {
                d.changed = true;
                d.setDirty();
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 적용

    /** 파일의 배치를 이 세계에 맞춘다. force = 이미 적용한 파일이어도 다시. 새로 세울 자리의 수(적용하지 않았으면 -1). */
    public static int apply(MinecraftServer server, boolean force) {
        FileData f = read();
        if (f == null || (!force && (!f.auto || f.npcs.isEmpty()))) return -1;
        NpcLayout d = get(server);
        if (!force && f.id.equals(d.appliedId)) return -1;
        d.pending.clear();
        Map<UUID, Spot> free = new LinkedHashMap<>(d.known);
        for (Spot want : f.npcs) {
            UUID best = null;
            double bd = Double.MAX_VALUE;
            for (Map.Entry<UUID, Spot> e : free.entrySet()) {
                Spot has = e.getValue();
                if (!has.key.equals(want.key)) continue;
                double dist = has.dim.equals(want.dim) ? (has.x - want.x) * (has.x - want.x) + (has.y - want.y) * (has.y - want.y) + (has.z - want.z) * (has.z - want.z) : 1.0e12;
                if (dist < bd) {
                    bd = dist;
                    best = e.getKey();
                }
            }
            if (best == null) {
                d.pending.add(want);
                continue;
            }
            Spot has = free.remove(best);
            if (bd <= 0.25 && Math.abs(has.yaw - want.yaw) < 1.0F) continue;   // 그 자리에 이미 서 있다
            d.retired.add(best);                                              // 자리가 달라졌다: 옛 것은 치우고 새 자리에 세운다
            d.known.remove(best);
            d.pending.add(want);
        }
        d.appliedId = f.id;
        d.changed = false;
        d.setDirty();
        NpcPlaces places = NpcPlaces.get(server);
        for (Spot s : d.pending) places.put(s.key, s.dim, (int) Math.floor(s.x), (int) Math.floor(s.y), (int) Math.floor(s.z));
        RtShop.LOGGER.info("[rtshop] NPC 배치(npc_layout.json)를 적용합니다: 새로 세울 자리 {}곳, 옮길 NPC {}명. 그 자리에 누가 가까이 가면 섭니다.", d.pending.size(), d.retired.size());
        return d.pending.size();
    }

    private static boolean fits(ServerLevel level, Spot s) {
        AABB body = new AABB(s.x - 0.3, s.y + 0.02, s.z - 0.3, s.x + 0.3, s.y + 1.9, s.z + 0.3);
        AABB feet = new AABB(s.x - 0.3, s.y - 0.6, s.z - 0.3, s.x + 0.3, s.y - 0.02, s.z + 0.3);
        return level.noCollision(body) && !level.noCollision(feet);
    }

    /** 세울 수 있게 된 자리에 NPC 를 세우고, 치울 옛 NPC 를 치운다. 세운 수. */
    public static int settle(MinecraftServer server) {
        NpcLayout d = get(server);
        int made = 0;
        if (!d.retired.isEmpty()) {
            for (UUID id : new ArrayList<>(d.retired)) {
                for (ServerLevel level : server.getAllLevels()) {
                    Entity e = level.getEntity(id);
                    if (e == null) continue;
                    d.retired.remove(id);
                    e.discard();
                    d.setDirty();
                    break;
                }
            }
        }
        for (Spot s : new ArrayList<>(d.pending)) {
            ResourceLocation dimId = ResourceLocation.tryParse(s.dim);
            ServerLevel level = dimId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
            if (level == null) continue;
            BlockPos at = BlockPos.containing(s.x, s.y, s.z);
            if (forceForTest ? !level.isLoaded(at) : !level.isPositionEntityTicking(at)) continue;
            // 이미 그 자리에 같은 NPC 가 서 있으면 (세계를 통째로 옮겨 온 서버) 그대로 쓴다
            boolean there = false;
            for (Entity e : level.getEntities((Entity) null, new AABB(at).inflate(2.0), x -> s.key.equals(keyOf(x)))) {
                boolean old = d.retired.contains(e.getUUID());
                note(e);   // 치우기로 한 옛 NPC 면 여기서 치워진다
                if (!old) there = true;
            }
            if (there) {
                d.pending.remove(s);
                d.setDirty();
                continue;
            }
            if (!fits(level, s)) {
                s.blocked = true;
                continue;
            }
            Villager v = com.rtshop.shop.ShopServer.spawnNpc(level, new Vec3(s.x, s.y, s.z), s.yaw, typeOf(s.key));
            d.pending.remove(s);
            d.setDirty();
            if (v == null) {
                RtShop.LOGGER.warn("[rtshop] NPC 배치: '{}' 는 이 서버에 없는 종류라 세우지 못했습니다.", s.key);
                continue;
            }
            v.setYHeadRot(s.yaw);
            v.setYBodyRot(s.yaw);
            note(v);
            made++;
        }
        return made;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!live()) return;
        try {
            apply(event.getServer(), false);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] NPC 배치를 적용하지 못했습니다: {}", t.toString());
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        int t = server.getTickCount();
        if (t % 20 != 13) return;
        try {
            NpcLayout d = get(server);
            if (t % 100 == 53) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(48), x -> keyOf(x) != null)) note(e);
                }
                if (d.changed && live()) {
                    FileData f = read();
                    // 다른 서버에서 가져온 배치를 아직 적용하지 않은 채로 덮어쓰지 않는다
                    if (f == null || f.npcs.isEmpty() || f.id.equals(d.appliedId) || !f.auto) export(server);
                    else if (apply(server, false) < 0) export(server);
                }
            }
            if (live() && (!d.pending.isEmpty() || !d.retired.isEmpty())) settle(server);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 명령

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("npc").requires(s -> s.hasPermission(2))
                .then(Commands.literal("layout")
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            NpcLayout d = get(server);
                            FileData f = read();
                            int blocked = 0;
                            for (Spot s : d.pending) if (s.blocked) blocked++;
                            int fileN = f == null ? 0 : f.npcs.size(), known = d.known.size(), waiting = d.pending.size(), stuck = blocked;
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.npclayout.status", known, fileN, waiting, stuck).withStyle(ChatFormatting.GOLD), false);
                            for (Spot s : d.pending) {
                                ctx.getSource().sendSuccess(() -> Component.translatable(s.blocked ? "rtshop.npclayout.blocked" : "rtshop.npclayout.waiting",
                                        s.key, (int) Math.floor(s.x), (int) Math.floor(s.y), (int) Math.floor(s.z)).withStyle(ChatFormatting.GRAY), false);
                            }
                            return 1;
                        })
                        .then(Commands.literal("save").executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            for (ServerLevel level : server.getAllLevels()) {
                                for (Entity e : level.getAllEntities()) if (keyOf(e) != null) note(e);
                            }
                            int n = export(server);
                            ctx.getSource().sendSuccess(() -> Component.translatable(n < 0 ? "rtshop.npclayout.save_failed" : "rtshop.npclayout.saved", n).withStyle(n < 0 ? ChatFormatting.RED : ChatFormatting.GREEN), true);
                            return n < 0 ? 0 : 1;
                        }))
                        .then(Commands.literal("apply").executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            int n = apply(server, true);
                            int made = n < 0 ? 0 : settle(server);
                            ctx.getSource().sendSuccess(() -> Component.translatable(n < 0 ? "rtshop.npclayout.no_file" : "rtshop.npclayout.applied", n, made).withStyle(n < 0 ? ChatFormatting.RED : ChatFormatting.GREEN), true);
                            return n < 0 ? 0 : 1;
                        }))
                        // 3.74.0 (사용자 W-18 "NPC 배치 초기화"): 모든 RT Shop NPC 를 치우고 배치 파일을 비운다 (지금 불러온 곳은 바로, 나머지는 그 청크가 돌 때). 되돌릴 수 없어 confirm 을 붙여야 한다
                        .then(Commands.literal("reset").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.npclayout.reset_ask").withStyle(ChatFormatting.GOLD), false);
                            return 1;
                        }).then(Commands.literal("confirm").executes(ctx -> {
                            int[] n = reset(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.npclayout.reset_done", n[0], n[1]).withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))))
                // 3.74.0 (사용자 W-18 "선택 npc 삭제 명령어"): 바라보는(없으면 가장 가까운 4칸 안) RT Shop NPC 하나를 치운다. 배치 파일에서도 빠진다
                .then(Commands.literal("remove").executes(ctx -> {
                    net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Entity e = picked(p);
                    if (e == null) {
                        ctx.getSource().sendFailure(Component.translatable("rtshop.npclayout.remove_none"));
                        return 0;
                    }
                    String name = e.getCustomName() == null ? keyOf(e) : e.getCustomName().getString();
                    e.discard();
                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.npclayout.removed", name, (int) Math.floor(e.getX()), (int) Math.floor(e.getY()), (int) Math.floor(e.getZ()))
                            .withStyle(ChatFormatting.GREEN), true);
                    return 1;
                }))));
    }

    /** 바라보는 RT Shop NPC (8칸 안, 시선에서 가장 가까운 것). 없으면 4칸 안에서 가장 가까운 것. 플레이어 상점의 직원은 뺀다. */
    public static Entity picked(net.minecraft.server.level.ServerPlayer p) {
        net.minecraft.world.phys.Vec3 eye = p.getEyePosition(), look = p.getViewVector(1F);
        Entity best = null, near = null;
        double bestDot = 0.96, nearD = 16;
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(8), x -> keyOf(x) != null && !x.getTags().contains(com.rtshop.pshop.Employees.TAG))) {
            net.minecraft.world.phys.Vec3 to = e.position().add(0, e.getBbHeight() * 0.6, 0).subtract(eye);
            double len = to.length();
            if (len < 0.01) continue;
            double dot = to.scale(1 / len).dot(look);
            if (len <= 8 && dot > bestDot) {
                bestDot = dot;
                best = e;
            }
            double d2 = e.distanceToSqr(p);
            if (d2 < nearD) {
                nearD = d2;
                near = e;
            }
        }
        return best != null ? best : near;
    }

    /** 모든 RT Shop NPC 를 치운다: {지금 치운 수, 나중에(그 청크가 돌 때) 치울 수}. 배치 파일은 비운다. */
    public static int[] reset(MinecraftServer server) {
        NpcLayout d = get(server);
        int now = 0;
        java.util.Set<java.util.UUID> gone = new java.util.HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> list = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (keyOf(e) != null && !e.getTags().contains(com.rtshop.pshop.Employees.TAG)) list.add(e);
            }
            for (Entity e : list) {
                gone.add(e.getUUID());
                e.discard();
                now++;
            }
        }
        int later = 0;
        for (java.util.UUID id : new ArrayList<>(d.known.keySet())) {
            if (gone.contains(id)) continue;
            d.retired.add(id);   // 아직 불러오지 않은 곳의 NPC: 다음에 보이면 치운다 (note)
            later++;
        }
        d.known.clear();
        d.pending.clear();
        d.changed = true;
        d.setDirty();
        export(server);
        return new int[]{now, later};
    }

    // ------------------------------------------------------------------ 점검용

    public static void resetForTest(MinecraftServer server) {
        NpcLayout d = get(server);
        d.known.clear();
        d.pending.clear();
        d.retired.clear();
        d.appliedId = "";
        d.changed = false;
        d.setDirty();
    }

    public static int knownForTest(MinecraftServer server, String key) {
        int n = 0;
        for (Spot s : get(server).known.values()) if (s.key.equals(key)) n++;
        return n;
    }

    public static int pendingForTest(MinecraftServer server) {
        return get(server).pending.size();
    }
}
