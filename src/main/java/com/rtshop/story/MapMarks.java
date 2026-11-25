package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rtshop.RtShop;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 지도 표시 (3.54, config/rtshop/map_marks.json): 말을 걸어 본 NPC 와 가 본 장소의 이름이 지도 모드(Diorama)에 남는다.
 * <ul>
 * <li>NPC: 상인·은행원·퀘스트 NPC 등 이 모드의 NPC 에게 말을 걸면 그 이름과 자리가 적힌다.</li>
 * <li>장소: 운영자가 /rtshop mapmark add 로 정해 둔 자리(이름 · 반경)에 들어서면 적힌다.</li>
 * </ul>
 * 서버는 사람마다 "알게 된 것"만 기억하고(죽어도 남는다) 클라이언트에 보낸다. 지도에 실제로 그리는 일은 클라이언트의
 * {@code MapMarkClient} 가 Diorama 의 웨이포인트 저장소에 넣어서 한다 (Diorama 가 없으면 아무 일도 하지 않는다).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class MapMarks {
    private MapMarks() {
    }

    public static final class Place {
        public String name = "", dim = "minecraft:overworld";
        public int x, y, z, radius = 12;
    }

    public static final class Config {
        public boolean enabled = true;
        /** 지도 모드의 웨이포인트 목록에서 이 표시들이 모이는 폴더 이름. */
        public String folder = "베스탈리아";
        /** NPC 에게 말을 걸면 그 이름을 적는다. */
        public boolean npcs = true;
        public String npcColor = "#E8C56A", placeColor = "#8FC7F0";
        public final List<Place> places = new ArrayList<>();

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("_help", "지도 표시. 말을 걸어 본 NPC 와 가 본 장소의 이름이 지도 모드(Diorama)의 웨이포인트로 남습니다. folder = 웨이포인트 폴더 이름, npcs = NPC 표시 켜기, "
                    + "npcColor / placeColor = 표시 색(#RRGGBB). places = 장소 목록 (게임 안에서 /rtshop mapmark add <반경> <이름> 으로 서 있는 자리를 더하고, remove <이름> 으로 지웁니다).");
            o.addProperty("enabled", enabled);
            o.addProperty("folder", folder);
            o.addProperty("npcs", npcs);
            o.addProperty("npcColor", npcColor);
            o.addProperty("placeColor", placeColor);
            JsonArray a = new JsonArray();
            for (Place p : places) {
                JsonObject e = new JsonObject();
                e.addProperty("name", p.name);
                e.addProperty("dim", p.dim);
                e.addProperty("x", p.x);
                e.addProperty("y", p.y);
                e.addProperty("z", p.z);
                e.addProperty("radius", p.radius);
                a.add(e);
            }
            o.add("places", a);
            return o;
        }

        static Config fromJson(JsonObject o) {
            Config c = new Config();
            if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
            if (o.has("folder") && !o.get("folder").getAsString().isBlank()) c.folder = o.get("folder").getAsString().trim();
            if (o.has("npcs")) c.npcs = o.get("npcs").getAsBoolean();
            if (o.has("npcColor")) c.npcColor = o.get("npcColor").getAsString();
            if (o.has("placeColor")) c.placeColor = o.get("placeColor").getAsString();
            if (o.has("places") && o.get("places").isJsonArray()) {
                for (var e : o.getAsJsonArray("places")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject j = e.getAsJsonObject();
                    Place p = new Place();
                    if (j.has("name")) p.name = j.get("name").getAsString().trim();
                    if (j.has("dim")) p.dim = j.get("dim").getAsString();
                    if (j.has("x")) p.x = j.get("x").getAsInt();
                    if (j.has("y")) p.y = j.get("y").getAsInt();
                    if (j.has("z")) p.z = j.get("z").getAsInt();
                    if (j.has("radius")) p.radius = Math.max(1, Math.min(256, j.get("radius").getAsInt()));
                    if (!p.name.isEmpty()) c.places.add(p);
                }
            }
            return c;
        }
    }

    private static Config config;

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("map_marks.json");
    }

    public static synchronized Config get() {
        if (config == null) config = load();
        return config;
    }

    public static synchronized void reload() {
        config = load();
    }

    public static synchronized void setForTest(Config c) {
        config = c;
    }

    public static Config fresh() {
        return new Config();
    }

    private static Config load() {
        try {
            if (Files.exists(path())) return Config.fromJson(JsonParser.parseString(Files.readString(path(), StandardCharsets.UTF_8)).getAsJsonObject());
            Config c = new Config();
            save(c);
            return c;
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] map_marks.json 을 읽지 못했습니다: {}", e.getMessage());
            return new Config();
        }
    }

    private static void save(Config c) {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] map_marks.json 을 쓰지 못했습니다: {}", e.getMessage());
        }
    }

    static int color(String hex, int fallback) {
        try {
            String h = hex.trim();
            if (h.startsWith("#")) h = h.substring(1);
            return 0xFF000000 | (Integer.parseInt(h, 16) & 0xFFFFFF);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------ 사람마다 알게 된 것

    private static final String KEY = "rtshop_map_marks";

    private static CompoundTag kept(ServerPlayer p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void keep(ServerPlayer p, CompoundTag marks) {
        CompoundTag keep = kept(p);
        keep.put(KEY, marks);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, keep);
    }

    /** 이 사람이 알게 된 표시의 수 (점검용). */
    public static int known(ServerPlayer p) {
        return kept(p).getCompound(KEY).size();
    }

    public static boolean knows(ServerPlayer p, String id) {
        return kept(p).getCompound(KEY).contains(id);
    }

    public static void forgetAll(ServerPlayer p) {
        keep(p, new CompoundTag());
    }

    private static JsonObject json(String id, CompoundTag t) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("name", t.getString("name"));
        o.addProperty("dim", t.getString("dim"));
        o.addProperty("x", t.getInt("x"));
        o.addProperty("y", t.getInt("y"));
        o.addProperty("z", t.getInt("z"));
        o.addProperty("color", t.getInt("c"));
        return o;
    }

    /** 점검용: 마지막으로 보낸 내용. */
    public static String lastSentForTest = "";

    private static void send(ServerPlayer p, JsonArray marks, JsonArray gone, boolean notify, boolean force) {
        JsonObject o = new JsonObject();
        o.addProperty("folder", get().folder);
        o.add("marks", marks);
        o.add("gone", gone);
        o.addProperty("notify", notify);
        o.addProperty("force", force);
        lastSentForTest = o.toString();
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.MAP_MARKS, o.toString()));
        } catch (Throwable ignored) {
            // 점검용 가짜 플레이어에게는 보낼 길이 없다
        }
    }

    /**
     * 이 사람의 지도에 표시를 하나 남긴다. 처음 알게 되었거나 자리·이름이 달라졌으면 true.
     * 같은 표시(id)는 다시 보내지 않는다: NPC 가 옮겨졌을 때만 새 자리로 고쳐 보낸다.
     */
    public static boolean mark(ServerPlayer p, String id, String name, String dim, int x, int y, int z, int color) {
        if (!get().enabled || name == null || name.isBlank()) return false;
        CompoundTag marks = kept(p).getCompound(KEY);
        CompoundTag old = marks.contains(id) ? marks.getCompound(id) : null;
        if (old != null && old.getString("name").equals(name) && old.getString("dim").equals(dim)
                && Math.abs(old.getInt("x") - x) <= 3 && Math.abs(old.getInt("y") - y) <= 3 && Math.abs(old.getInt("z") - z) <= 3) {
            return false;   // 서너 칸 움직인 것은 같은 자리로 본다 (NPC 가 조금씩 밀리는 것 때문에 자꾸 고치지 않게)
        }
        CompoundTag t = new CompoundTag();
        t.putString("name", name);
        t.putString("dim", dim);
        t.putInt("x", x);
        t.putInt("y", y);
        t.putInt("z", z);
        t.putInt("c", color);
        marks.put(id, t);
        keep(p, marks);
        JsonArray a = new JsonArray();
        a.add(json(id, t));
        send(p, a, new JsonArray(), old == null, false);
        return true;
    }

    /** 알게 된 표시를 모두 다시 보낸다 (들어올 때). 운영자가 지운 장소는 이 사람의 기록에서도 지우고 지도에서 빼게 한다. force: 플레이어가 지도에서 지운 것도 다시 넣는다. */
    public static void sendAll(ServerPlayer p, boolean force) {
        Config c = get();
        if (!c.enabled) return;
        CompoundTag marks = kept(p).getCompound(KEY);
        JsonArray a = new JsonArray(), gone = new JsonArray();
        boolean changed = false;
        for (String id : new ArrayList<>(marks.getAllKeys())) {
            if (id.startsWith("place:")) {
                Place pl = place(c, id.substring(6));
                if (pl == null) {
                    marks.remove(id);
                    gone.add(id);
                    changed = true;
                    continue;
                }
                CompoundTag t = marks.getCompound(id);
                if (t.getInt("x") != pl.x || t.getInt("y") != pl.y || t.getInt("z") != pl.z || !t.getString("dim").equals(pl.dim)) {
                    t.putInt("x", pl.x);
                    t.putInt("y", pl.y);
                    t.putInt("z", pl.z);
                    t.putString("dim", pl.dim);
                    changed = true;
                }
            }
            a.add(json(id, marks.getCompound(id)));
        }
        if (changed) keep(p, marks);
        if (a.size() > 0 || gone.size() > 0) send(p, a, gone, false, force);
    }

    private static Place place(Config c, String name) {
        for (Place p : c.places) if (p.name.equals(name)) return p;
        return null;
    }

    // ------------------------------------------------------------------ 알게 되는 때

    /** NPC 에게 말을 걸었다. */
    public static boolean talked(ServerPlayer p, Entity npc) {
        Config c = get();
        if (!c.enabled || !c.npcs || npc == null || !com.rtshop.shop.NpcGuard.isNpc(npc) || npc.getCustomName() == null) return false;
        if (npc.getTags().contains(com.rtshop.pshop.Employees.TAG)) return false;   // 플레이어 상점의 직원은 적지 않는다
        if (npc.level().dimension() == StoryArena.DIM) return false;
        return mark(p, "npc:" + npc.getUUID(), npc.getCustomName().getString(), npc.level().dimension().location().toString(),
                npc.blockPosition().getX(), npc.blockPosition().getY(), npc.blockPosition().getZ(), color(c.npcColor, 0xFFE8C56A));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p)) return;
        try {
            talked(p, event.getTarget());
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] 지도 표시(NPC) 실패: {}", t.toString());
        }
    }

    /** 이 사람이 선 자리에 정해 둔 장소가 있으면 적는다. 적은 수를 돌려준다. */
    public static int visit(ServerPlayer p) {
        Config c = get();
        if (!c.enabled || c.places.isEmpty()) return 0;
        String dim = p.level().dimension().location().toString();
        int n = 0;
        for (Place pl : c.places) {
            if (!pl.dim.equals(dim)) continue;
            double dx = p.getX() - (pl.x + 0.5), dz = p.getZ() - (pl.z + 0.5), dy = p.getY() - pl.y;
            if (dx * dx + dz * dz > (double) pl.radius * pl.radius || Math.abs(dy) > pl.radius + 8) continue;
            if (mark(p, "place:" + pl.name, pl.name, pl.dim, pl.x, pl.y, pl.z, color(c.placeColor, 0xFF8FC7F0))) n++;
        }
        return n;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        if (p instanceof net.minecraftforge.common.util.FakePlayer) return;
        try {
            visit(p);
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] 지도 표시(장소) 실패: {}", t.toString());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sendAll(p, false);
    }

    // ------------------------------------------------------------------ 명령어

    /** 운영자: 서 있는 자리를 장소로 더한다 (같은 이름이 있으면 자리를 옮긴다). */
    public static Place addPlace(ServerPlayer p, String name, int radius) {
        Config c = get();
        Place pl = place(c, name);
        if (pl == null) {
            pl = new Place();
            pl.name = name;
            c.places.add(pl);
        }
        pl.dim = p.level().dimension().location().toString();
        pl.x = p.blockPosition().getX();
        pl.y = p.blockPosition().getY();
        pl.z = p.blockPosition().getZ();
        pl.radius = Math.max(1, Math.min(256, radius));
        save(c);
        return pl;
    }

    public static boolean removePlace(String name) {
        Config c = get();
        Place pl = place(c, name);
        if (pl == null) return false;
        c.places.remove(pl);
        save(c);
        return true;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("mapmark")
                .then(Commands.literal("add").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name").trim();
                                    if (name.isEmpty()) return 0;
                                    Place pl = addPlace(p, name, IntegerArgumentType.getInteger(ctx, "radius"));
                                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.mapmark.place_added", pl.name, pl.x, pl.y, pl.z, pl.radius)
                                            .withStyle(ChatFormatting.GREEN), true);
                                    return 1;
                                }))))
                .then(Commands.literal("remove").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> {
                                    for (Place pl : get().places) b.suggest(pl.name);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name").trim();
                                    boolean ok = removePlace(name);
                                    if (ok) {
                                        for (ServerPlayer sp : ctx.getSource().getServer().getPlayerList().getPlayers()) sendAll(sp, false);
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "rtshop.mapmark.place_removed" : "rtshop.mapmark.place_unknown", name), ok);
                                    return ok ? 1 : 0;
                                })))
                .then(Commands.literal("list").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    Config c = get();
                    if (c.places.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.mapmark.place_none"), false);
                        return 0;
                    }
                    for (Place pl : c.places) {
                        ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.mapmark.place_line", pl.name, pl.dim, pl.x, pl.y, pl.z, pl.radius), false);
                    }
                    return c.places.size();
                }))
                .then(Commands.literal("resend").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    sendAll(p, true);
                    int n = known(p);
                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.mapmark.resent", n), false);
                    return 1;
                }))
                .then(Commands.literal("forget").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    JsonArray gone = new JsonArray();
                    for (String id : kept(p).getCompound(KEY).getAllKeys()) gone.add(id);
                    forgetAll(p);
                    send(p, new JsonArray(), gone, false, false);
                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.mapmark.forgot"), false);
                    return 1;
                }))));
    }
}
