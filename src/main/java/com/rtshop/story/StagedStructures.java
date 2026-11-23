package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 단계적으로 풀리는 이야기 건축물: 서버에서 누군가 그 건축물로 가라는 퀘스트를 처음 받기 전까지는 새 지형에 생기지 않는다.
 * (가장 앞선 사람의 진행도를 따른다.) 이미 생긴 건축물은 그대로 두고, 들어가는 것은 결계(StorySeals)가 막는다.
 * config/rtshop/staged_structures.json · 월드 데이터 rtshop_staged
 * 주의: 막혀 있는 동안 처음 만들어진 지형에는 나중에도 생기지 않는다 (미리 생성한 월드라면 끄거나 /rtshop staged unlock all).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class StagedStructures {
    private StagedStructures() {
    }

    public boolean enabled = true;
    /** 건축물 ID -> 그 건축물을 풀어 주는 표식 (빈 값 = 그 건축물로 가는 퀘스트를 받으면) */
    public final Map<String, String> structures = new LinkedHashMap<>();

    private static StagedStructures current;
    /** 지금 생성이 막힌 건축물 (지형 생성 스레드에서 읽는다) */
    private static volatile Set<String> locked = Collections.emptySet();
    /** 점검용: 지형 생성이 건축물마다 물어본 횟수 / 막은 횟수 */
    public static final AtomicLong ASKED = new AtomicLong(), BLOCKED = new AtomicLong();

    public static StagedStructures defaults() {
        StagedStructures s = new StagedStructures();
        for (String id : List.of("mowziesmobs:wrought_chamber", "mowziesmobs:frostmaw_spawn", "mowziesmobs:monastery", "mowziesmobs:umvuthana_grove",
                "aquamirae:ship", "cataclysm:soul_black_smith", "cataclysm:cursed_pyramid", "cataclysm:ancient_factory", "cataclysm:sunken_city",
                "cataclysm:amethyst_nest", "cataclysm:ruined_citadel", "cataclysm:frosted_prison", "cataclysm:burning_arena", "minecraft:mansion")) {
            s.structures.put(id, "");
        }
        s.structures.put("cataclysm:acropolis", "seal_scylla");
        return s;
    }

    public static synchronized StagedStructures get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void reload() {
        current = load();
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("staged_structures.json");
    }

    private static StagedStructures load() {
        Path f = file();
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                StagedStructures s = new StagedStructures();
                if (o.has("enabled")) s.enabled = o.get("enabled").getAsBoolean();
                if (o.has("structures")) {
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("structures").entrySet()) s.structures.put(e.getKey(), e.getValue().getAsString());
                }
                return s;
            }
            StagedStructures d = defaults();
            Files.createDirectories(f.getParent());
            JsonObject o = new JsonObject();
            o.addProperty("_help", "이야기 건축물을 단계적으로 생성한다: 누군가 그 건축물로 가는 퀘스트를 받으면(또는 적힌 표식을 얻으면) 그때부터 새 지형에 생긴다. "
                    + "막혀 있는 동안 처음 만들어진 지형에는 나중에도 생기지 않는다. 월드를 미리 생성하려면 enabled 를 false 로 하거나 /rtshop staged unlock all.");
            o.addProperty("enabled", d.enabled);
            JsonObject st = new JsonObject();
            d.structures.forEach(st::addProperty);
            o.add("structures", st);
            Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            return d;
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] staged_structures.json 을 읽지 못했습니다: {}", ex.toString());
            return defaults();
        }
    }

    // ------------------------------------------------------------------ 월드 데이터: 풀린 건축물

    public static final class Data extends SavedData {
        final Set<String> unlocked = new LinkedHashSet<>();

        static Data load(CompoundTag t) {
            Data d = new Data();
            ListTag l = t.getList("unlocked", Tag.TAG_STRING);
            for (int i = 0; i < l.size(); i++) d.unlocked.add(l.getString(i));
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            ListTag l = new ListTag();
            for (String s : unlocked) l.add(StringTag.valueOf(s));
            t.put("unlocked", l);
            return t;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rtshop_staged");
    }

    /** 이야기 퀘스트가 있고 켜져 있을 때만 막는다. */
    static boolean active() {
        return get().enabled && QuestManager.loaded().quests.containsKey("story_1_01");
    }

    public static void recompute(MinecraftServer server) {
        if (server == null || !active()) {
            locked = Collections.emptySet();
            return;
        }
        Set<String> un = data(server).unlocked;
        Set<String> l = new LinkedHashSet<>();
        for (String id : get().structures.keySet()) if (!un.contains(id)) l.add(id);
        locked = Collections.unmodifiableSet(l);
    }

    /** 지형 생성이 묻는다 (아무 스레드). */
    public static boolean blocks(String structureId) {
        ASKED.incrementAndGet();
        if (locked.contains(structureId)) {
            BLOCKED.incrementAndGet();
            return true;
        }
        return false;
    }

    public static boolean isLocked(String structureId) {
        return locked.contains(structureId);
    }

    public static boolean unlock(MinecraftServer server, String id, ServerPlayer by) {
        Data d = data(server);
        if (!d.unlocked.add(id)) return false;
        d.setDirty();
        recompute(server);
        RtShop.LOGGER.info("[rtshop] 이야기 건축물 생성이 풀렸습니다: {}{}", id, by == null ? "" : " (" + by.getGameProfile().getName() + ")");
        if (by != null) by.displayClientMessage(Component.translatable("rtshop.staged.unlocked").withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    /** 이 사람의 진행도로 풀 수 있는 건축물을 푼다: 진행 중이거나 끝낸 퀘스트의 STRUCTURE 목표, 또는 표식. */
    public static void scan(ServerPlayer p) {
        if (!active()) return;
        StagedStructures c = get();
        for (Map.Entry<String, String> e : c.structures.entrySet()) {
            if (!isLocked(e.getKey())) continue;
            boolean open = !e.getValue().isEmpty() ? QuestManager.hasFlag(p, e.getValue()) : leadsOrLed(p, e.getKey());
            if (open) unlock(p.server, e.getKey(), p);
        }
    }

    private static boolean leadsOrLed(ServerPlayer p, String structure) {
        if (QuestManager.questLeadsTo(p, structure)) return true;
        for (QuestDef q : QuestManager.loaded().quests.values()) {
            if (!QuestManager.isCompleted(p, q.id)) continue;
            for (QuestDef.Objective ob : q.objectives) if (ob.type == QuestDef.Type.STRUCTURE && ob.target.equals(structure)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        recompute(event.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) scan(p);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 100 != 17) return;
        if (locked.isEmpty()) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) scan(p);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("staged").requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    recompute(server);
                    Set<String> un = data(server).unlocked;
                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.staged.title", active() ? "ON" : "OFF").withStyle(ChatFormatting.GOLD), false);
                    for (String id : get().structures.keySet()) {
                        boolean open = un.contains(id) || !active();
                        ctx.getSource().sendSuccess(() -> Component.literal((open ? " ✔ " : " ✖ ") + id).withStyle(open ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
                    }
                    return 1;
                })
                .then(Commands.literal("unlock").then(Commands.argument("structure", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                java.util.stream.Stream.concat(java.util.stream.Stream.of("all"), get().structures.keySet().stream()), b))
                        .executes(ctx -> {
                            String id = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "structure").trim();
                            MinecraftServer server = ctx.getSource().getServer();
                            List<String> ids = id.equals("all") ? new ArrayList<>(get().structures.keySet()) : List.of(id);
                            int n = 0;
                            for (String s : ids) if (unlock(server, s, null)) n++;
                            int fn = n;
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.staged.unlocked_n", fn).withStyle(ChatFormatting.GREEN), true);
                            return n;
                        })))
                .then(Commands.literal("lock").then(Commands.argument("structure", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(data(ctx.getSource().getServer()).unlocked, b))
                        .executes(ctx -> {
                            String id = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "structure").trim();
                            MinecraftServer server = ctx.getSource().getServer();
                            boolean r = data(server).unlocked.remove(id);
                            data(server).setDirty();
                            recompute(server);
                            ctx.getSource().sendSuccess(() -> Component.literal((r ? "✖ " : "- ") + id), true);
                            return r ? 1 : 0;
                        })))));
    }

    /** 점검용 */
    public static void resetForTest(MinecraftServer server) {
        data(server).unlocked.clear();
        recompute(server);
    }
}
