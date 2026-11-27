package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.item.ClueItem;
import com.rtshop.item.StoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 살펴보기 (3.78, 사용자 W-13 "타르코프식 아이템 설명을 스토리 및 히든 아이템에도"):
 * 이야기 물건(스킬라 단서 포함)과 히든 아이템(위조 도구 꾸러미 · 감정 돋보기 · 설정한 히든 장비)은 처음 손에 넣으면 무엇인지 모른다.
 * 이름은 「처음 보는 물건」, 설명은 감춰지고 아이콘 귀퉁이에 「?」. 들고 우클릭하면 몇 초 동안 살펴보고, 그 뒤로 그 사람에게는 이름과 설명이 보인다.
 * 아는 것은 사람마다 (플레이어 데이터 rtshop_examined), 같은 종류는 한 번만 (단서는 단서마다). 3.78 전부터 지니던 것은 처음 들어올 때 이미 아는 것으로 친다.
 * 설정: config/rtshop/examine.json.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Examine {
    private Examine() {
    }

    public static final String DATA = "rtshop_examined";

    // ------------------------------------------------------------------ 설정

    public static final class Config {
        public boolean enabled = true;
        /** 살펴보는 데 드는 시간(초). */
        public double seconds = 2.0;
        /** 이야기 물건(StoryItem)과 스킬라 단서를 살펴봐야 하는가. */
        public boolean storyItems = true;
        /** 히든 아이템 (아이템 ID). 앞으로 더할 히든 아이템도 여기에. */
        public final List<String> hiddenItems = new ArrayList<>(List.of("rtshop:forger_kit", "rtshop:appraisal_loupe"));
        /** 강화 설정의 「파괴되지 않는 히든 장비」(safeItems)도 히든 아이템으로 친다. */
        public boolean enhanceSafeItems = true;
    }

    private static Config config;

    public static synchronized Config get() {
        if (config == null) config = load();
        return config;
    }

    public static synchronized void reload() {
        config = load();
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("examine.json");
    }

    private static Config load() {
        Config c = new Config();
        Path f = file();
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
                if (o.has("seconds")) c.seconds = Math.max(0.25, Math.min(30, o.get("seconds").getAsDouble()));
                if (o.has("storyItems")) c.storyItems = o.get("storyItems").getAsBoolean();
                if (o.has("enhanceSafeItems")) c.enhanceSafeItems = o.get("enhanceSafeItems").getAsBoolean();
                if (o.has("hiddenItems")) {
                    c.hiddenItems.clear();
                    for (JsonElement e : o.getAsJsonArray("hiddenItems")) c.hiddenItems.add(e.getAsString());
                }
            } else {
                Files.createDirectories(f.getParent());
                Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson(c)), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] examine.json 을 읽지 못했습니다: {}", ex.toString());
        }
        return c;
    }

    static JsonObject toJson(Config c) {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "살펴보기 (타르코프식). 이야기 물건 · 히든 아이템은 처음엔 「처음 보는 물건」으로 보이고, 들고 우클릭해 seconds 초 동안 살펴봐야 이름과 설명이 보인다 "
                + "(사람마다, 종류마다 한 번). storyItems: 이야기 물건과 스킬라 단서. hiddenItems: 히든 아이템 ID 목록. enhanceSafeItems: 강화 설정의 히든 장비(safeItems)도. "
                + "enabled=false 면 끈다. 고친 뒤 /rtshop reload.");
        o.addProperty("enabled", c.enabled);
        o.addProperty("seconds", c.seconds);
        o.addProperty("storyItems", c.storyItems);
        JsonArray a = new JsonArray();
        c.hiddenItems.forEach(a::add);
        o.add("hiddenItems", a);
        o.addProperty("enhanceSafeItems", c.enhanceSafeItems);
        return o;
    }

    // ------------------------------------------------------------------ 무엇을 살펴봐야 하나

    /** 살펴볼 물건이면 그 열쇠 (이야기 물건 · 히든 아이템 = 아이템 ID, 스킬라 단서 = "clue:" + 단서 ID), 아니면 null. 서버의 설정으로. */
    public static String key(ItemStack s) {
        Config c = get();
        if (!c.enabled || s == null || s.isEmpty()) return null;
        java.util.Collection<String> safe = c.enhanceSafeItems ? com.rtshop.enhance.EnhanceConfig.get().safeItems : List.of();
        return keyOf(s, c.storyItems, c.hiddenItems, safe);
    }

    static String keyOf(ItemStack s, boolean story, java.util.Collection<String> hidden, java.util.Collection<String> safe) {
        if (s == null || s.isEmpty()) return null;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(s.getItem());
        String id = rl == null ? "" : rl.toString();
        if (story && s.getItem() instanceof ClueItem) {
            // 단서 ID 는 물건에 적혀 있다 (설정 파일을 읽지 않는다: 화면 쪽에서도 같은 열쇠)
            String cid = s.hasTag() ? s.getTag().getString(ScyllaClues.TAG) : "";
            return cid.isEmpty() ? id : "clue:" + cid;
        }
        if (story && s.getItem() instanceof StoryItem) return id;
        if (hidden.contains(id) || safe.contains(id)) return id;
        return null;
    }

    // ------------------------------------------------------------------ 아는 것 (서버)

    private static CompoundTag data(Player p) {
        CompoundTag pers = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return pers.getCompound(DATA);
    }

    private static void keep(Player p, CompoundTag d) {
        CompoundTag pers = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        pers.put(DATA, d);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, pers);
    }

    public static Set<String> known(Player p) {
        Set<String> out = new HashSet<>();
        ListTag l = data(p).getList("k", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }

    public static boolean knows(Player p, String key) {
        return key == null || known(p).contains(key);
    }

    /** 살펴본 것으로 적는다 (이미 알면 false). */
    public static boolean learn(ServerPlayer p, String key) {
        if (key == null) return false;
        CompoundTag d = data(p);
        ListTag l = d.getList("k", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) if (l.getString(i).equals(key)) return false;
        l.add(StringTag.valueOf(key));
        d.put("k", l);
        keep(p, d);
        sync(p);
        return true;
    }

    /** 3.78 전부터 지니던 물건은 처음 들어올 때 이미 아는 것으로 (가방 · 갑옷 · 왼손 · 엔더 상자). */
    public static void grandfather(ServerPlayer p) {
        CompoundTag d = data(p);
        if (d.getInt("v") >= 1) return;
        ListTag l = d.getList("k", Tag.TAG_STRING);
        Set<String> have = new HashSet<>();
        for (int i = 0; i < l.size(); i++) have.add(l.getString(i));
        List<ItemStack> all = new ArrayList<>();
        all.addAll(p.getInventory().items);
        all.addAll(p.getInventory().armor);
        all.addAll(p.getInventory().offhand);
        for (int i = 0; i < p.getEnderChestInventory().getContainerSize(); i++) all.add(p.getEnderChestInventory().getItem(i));
        for (ItemStack s : all) {
            String k = key(s);
            if (k != null && have.add(k)) l.add(StringTag.valueOf(k));
        }
        d.put("k", l);
        d.putInt("v", 1);
        keep(p, d);
    }

    /** 시험용: 아는 것을 비우고 다시 처음부터. */
    public static void resetForTest(ServerPlayer p, boolean grandfathered) {
        CompoundTag d = new CompoundTag();
        if (grandfathered) d.putInt("v", 1);
        keep(p, d);
        JOBS.remove(p.getUUID());
        sync(p);
    }

    /** 화면 쪽에 알린다: {"on":…, "story":…, "items":[히든 아이템 ID…], "known":[열쇠…], "sec":…}. */
    public static void sync(ServerPlayer p) {
        if (p instanceof net.minecraftforge.common.util.FakePlayer || p.connection == null) return;
        Config c = get();
        JsonObject o = new JsonObject();
        o.addProperty("on", c.enabled);
        o.addProperty("story", c.storyItems);
        o.addProperty("sec", c.seconds);
        JsonArray items = new JsonArray();
        c.hiddenItems.forEach(items::add);
        if (c.enhanceSafeItems) com.rtshop.enhance.EnhanceConfig.get().safeItems.forEach(items::add);
        o.add("items", items);
        JsonArray k = new JsonArray();
        known(p).forEach(k::add);
        o.add("known", k);
        com.rtshop.net.ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new com.rtshop.net.StoryPacket(com.rtshop.net.StoryPacket.EXAMINE, o.toString()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            try {
                grandfather(sp);
                sync(sp);
            } catch (Throwable t) {
                RtShop.LOGGER.warn("[rtshop] 살펴보기 정보를 보내지 못했습니다: {}", t.toString());
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // 죽어도 아는 것은 그대로 (PERSISTED_NBT_TAG 는 Forge 가 옮겨 준다) — 다시 알린다
        if (event.getEntity() instanceof ServerPlayer sp) {
            try {
                sync(sp);
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ 살펴보기 (몇 초 동안)

    private static final class Job {
        final String key;
        final InteractionHand hand;
        final int total;
        int left;

        Job(String key, InteractionHand hand, int total) {
            this.key = key;
            this.hand = hand;
            this.total = total;
            this.left = total;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    /** 모르는 물건을 들고 우클릭: 그 물건의 쓰임 대신 살펴보기를 시작한다. 막았으면 true. */
    public static boolean tryStart(ServerPlayer p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        String k = key(s);
        if (k == null || knows(p, k)) return false;
        Job j = JOBS.get(p.getUUID());
        if (j != null && j.key.equals(k)) return true;   // 이미 살펴보는 중
        int ticks = Math.max(5, (int) Math.round(get().seconds * 20));
        JOBS.put(p.getUUID(), new Job(k, hand, ticks));
        p.displayClientMessage(Component.translatable("rtshop.examine.progress", bar(0, ticks)).withStyle(ChatFormatting.YELLOW), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.2F);
        return true;
    }

    /** 한 틱: 들고 있는 물건이 바뀌면 멈추고, 다 되면 적는다. 끝났으면 그 열쇠, 아니면 null. */
    static String step(ServerPlayer p, Job j) {
        ItemStack s = p.getItemInHand(j.hand);
        if (!j.key.equals(key(s))) {
            JOBS.remove(p.getUUID());
            p.displayClientMessage(Component.translatable("rtshop.examine.stopped").withStyle(ChatFormatting.GRAY), true);
            return null;
        }
        j.left--;
        if (j.left > 0) {
            if (j.left % 5 == 0) {
                p.displayClientMessage(Component.translatable("rtshop.examine.progress", bar(j.total - j.left, j.total)).withStyle(ChatFormatting.YELLOW), true);
            }
            return null;
        }
        JOBS.remove(p.getUUID());
        learn(p, j.key);
        p.displayClientMessage(Component.translatable("rtshop.examine.done", s.getHoverName().copy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GREEN), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 0.8F, 1.1F);
        return j.key;
    }

    private static String bar(int done, int total) {
        int n = 10, f = total <= 0 ? n : Math.min(n, done * n / total);
        return "▮".repeat(f) + "▯".repeat(n - f);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || JOBS.isEmpty() || event.getServer() == null) return;
        for (UUID id : new ArrayList<>(JOBS.keySet())) {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
            Job j = JOBS.get(id);
            if (p == null || j == null) {
                JOBS.remove(id);
                continue;
            }
            try {
                step(p, j);
            } catch (Throwable t) {
                JOBS.remove(id);
            }
        }
    }

    /** 시험용: 지금 설정을 파일 모양으로. */
    public static JsonObject configJsonForTest() {
        return toJson(get());
    }

    /** 시험용: 살펴보는 중이면 n 틱을 돌린다. 끝났으면 그 열쇠. */
    public static String tickForTest(ServerPlayer p, int n) {
        String got = null;
        for (int i = 0; i < n; i++) {
            Job j = JOBS.get(p.getUUID());
            if (j == null) break;
            String k = step(p, j);
            if (k != null) got = k;
        }
        return got;
    }

    public static boolean examiningForTest(ServerPlayer p) {
        return JOBS.containsKey(p.getUUID());
    }

    /** 들고 우클릭: 모르는 물건이면 그 물건의 쓰임(읽기 · 단서 적기 · 도구) 대신 살펴보기. 화면 쪽도 같은 물건이면 막는다 (서버가 살펴보기를 시작). */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        try {
            if (event.getEntity() instanceof ServerPlayer sp) {
                if (tryStart(sp, event.getHand())) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.CONSUME);
                }
            } else if (event.getLevel().isClientSide && clientUnknown(event.getItemStack())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 화면 쪽 (클라이언트) — 서버가 보낸 것

    private static volatile boolean clientOn, clientStory = true;
    private static volatile double clientSeconds = 2.0;
    private static volatile Set<String> clientItems = Set.of(), clientKnown = Set.of();
    private static volatile boolean clientSynced;

    /** 서버가 보낸 것을 받는다 (StoryClient). */
    public static void clientReceive(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        clientOn = o.has("on") && o.get("on").getAsBoolean();
        clientStory = !o.has("story") || o.get("story").getAsBoolean();
        clientSeconds = o.has("sec") ? o.get("sec").getAsDouble() : 2.0;
        Set<String> items = new HashSet<>(), known = new HashSet<>();
        if (o.has("items")) for (JsonElement e : o.getAsJsonArray("items")) items.add(e.getAsString());
        if (o.has("known")) for (JsonElement e : o.getAsJsonArray("known")) known.add(e.getAsString());
        clientItems = items;
        clientKnown = known;
        clientSynced = true;
    }

    /** 접속을 끊으면 잊는다 (다른 서버에서는 감추지 않는다). */
    public static void clientClear() {
        clientSynced = false;
        clientKnown = Set.of();
        clientItems = Set.of();
    }

    public static double clientSeconds() {
        return clientSeconds;
    }

    /** 화면 쪽: 이 사람이 아직 살펴보지 않은 물건인가. 서버에서 알려 오기 전이나 다른 서버에서는 false. */
    public static boolean clientUnknown(ItemStack s) {
        if (!clientSynced || !clientOn || s == null || s.isEmpty()) return false;
        String k = keyOf(s, clientStory, clientItems, List.of());
        return k != null && !clientKnown.contains(k);
    }

    /** 이름을 감출 때인가: 화면을 그리는 쪽(클라이언트 스레드)에서, 아직 살펴보지 않은 물건. 서버 스레드(같은 컴퓨터의 싱글 서버 포함)는 언제나 진짜 이름. */
    public static boolean hideName(ItemStack s) {
        try {
            return FMLEnvironment.dist.isClient() && EffectiveSide.get().isClient() && clientUnknown(s);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 감춘 이름. */
    public static Component unknownName() {
        return Component.translatable("rtshop.examine.unknown_name").withStyle(ChatFormatting.GRAY);
    }
}
