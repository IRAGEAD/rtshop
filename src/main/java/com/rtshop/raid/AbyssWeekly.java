package com.rtshop.raid;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.net.RaidPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spiral Abyss 주간 보상 (config/rtshop/abyss.json).
 *  - 주간 목표: 그 주에 각 층(1~6층)을 클리어하기. 클리어하면 그 층의 보상을 (항목마다 정한 확률로) 받는다. 층마다 주 1회.
 *  - 매주 월요일 오전 6시에 초기화. 층 선택 화면 왼쪽 아래에 보상 목록이 뜨고, 받은 보상에는 ✔ 가 붙는다.
 *  - Spiral Abyss 의 진행 상태를 주기적으로 읽어 "층 클리어" 순간을 알아낸다 (그 모드를 고치지 않는다).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class AbyssWeekly {
    private AbyssWeekly() {
    }

    public static final int FLOORS = 6;
    public static final int ACT_VIEW = 6;
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI"};

    // ------------------------------------------------------------------ 설정

    public static final class Config {
        public boolean enabled = true;
        public int resetDay = 1;
        public int resetHour = 6;
        public long[] gold = {500, 1000, 2000, 3500, 5000, 8000};
        /** 주간 보상을 무한 모드에서 준다 (endlessStep 웨이브마다 한 단계: 10·20·…·60). false 면 예전처럼 1~6층 클리어 때. */
        public boolean endless = true;
        public int endlessStep = 10;
        /** 층 모드(1~6층)는 이야기 퀘스트가 그 층을 부를 때만 들어갈 수 있다. 무한 모드는 언제든. */
        public boolean storyOnlyFloors = true;
        public final Map<String, String> texts = new LinkedHashMap<>();
        public final List<Reward> rewards = new ArrayList<>();

        public String text(String k) {
            String v = texts.get(k);
            if (v == null || v.isEmpty()) v = defaultTexts().get(k);
            return v == null ? k : v;
        }

        public List<Reward> floor(int f) {
            List<Reward> out = new ArrayList<>();
            for (Reward r : rewards) if (r.group.equals("floor" + f)) out.add(r);
            return out;
        }

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            JsonArray help = new JsonArray();
            help.add("Spiral Abyss 주간 보상: endless 가 true 면 무한 모드에서 endlessStep 웨이브(10·20·…·60)를 넘길 때마다 한 단계씩 받습니다 (단계마다 주 1회, 매주 resetDay 요일 resetHour 시에 초기화. 1 = 월요일). false 면 1~6층 클리어 때.");
            help.add("storyOnlyFloors: true 면 층 모드(1~6층)는 이야기 퀘스트가 그 층을 부를 때만 들어갈 수 있습니다 (무한 모드는 언제든).");
            help.add("rewards: group 이 floor1~floor6 인 항목이 1~6단계의 보상. chance 는 받을 확률(%). gold: 단계마다 함께 주는 Gold.");
            help.add("게임 안에서 /rtshop abyss edit (OP) 로 고칠 수 있습니다: 아이템을 손에 들고 [손에 든 아이템 추가], 개수·확률 변경, 삭제.");
            o.add("_설명", help);
            o.addProperty("enabled", enabled);
            o.addProperty("resetDay", resetDay);
            o.addProperty("resetHour", resetHour);
            o.addProperty("endless", endless);
            o.addProperty("endlessStep", endlessStep);
            o.addProperty("storyOnlyFloors", storyOnlyFloors);
            JsonArray g = new JsonArray();
            for (long v : gold) g.add(v);
            o.add("gold", g);
            JsonObject t = new JsonObject();
            texts.forEach(t::addProperty);
            o.add("texts", t);
            o.add("rewards", RewardEditor.rewardsJson(rewards));
            return o;
        }
    }

    public static Map<String, String> defaultTexts() {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("title", "주간 보상");
        t.put("reset", "초기화까지 %s일 %s시간");
        t.put("floor", "%s층 클리어");
        t.put("got", "[주간 보상] %s층 클리어 보상을 받았습니다: %s");
        t.put("gotNothing", "[주간 보상] %s층 클리어! (이번에는 확률 보상이 나오지 않았습니다)");
        t.put("gotEndless", "[주간 보상] 무한 모드 %s웨이브 보상을 받았습니다: %s");
        t.put("gotNothingEndless", "[주간 보상] 무한 모드 %s웨이브 돌파! (이번에는 확률 보상이 나오지 않았습니다)");
        t.put("storyOnly", "이 층은 이야기가 부를 때만 열립니다. 무한 모드는 언제든 들어갈 수 있습니다.");
        t.put("done", "받음");
        return t;
    }

    private static Config config;

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("abyss.json");
    }

    public static synchronized Config config() {
        if (config == null) config = load();
        return config;
    }

    public static synchronized Config reload() {
        config = load();
        return config;
    }

    public static synchronized void setForTest(Config c) {
        config = c;
    }

    public static Config fresh() {
        Config c = new Config();
        c.texts.putAll(defaultTexts());
        String[][] d = {
                {"minecraft:iron_ingot", "8", "100"}, {"minecraft:gold_ingot", "8", "100"}, {"minecraft:diamond", "2", "100"},
                {"minecraft:diamond", "4", "100"}, {"minecraft:emerald", "16", "100"}, {"minecraft:netherite_scrap", "1", "100"}};
        for (int f = 1; f <= FLOORS; f++) {
            Reward r = new Reward("floor" + f, 0, d[f - 1][0], Integer.parseInt(d[f - 1][1]));
            r.chance = Double.parseDouble(d[f - 1][2]);
            c.rewards.add(r);
        }
        Reward t3 = new Reward("floor3", 0, "rtshop:gacha_ticket_normal", 1);
        t3.chance = 50;
        c.rewards.add(t3);
        Reward t5 = new Reward("floor5", 0, "rtshop:gacha_ticket_mid", 1);
        t5.chance = 50;
        c.rewards.add(t5);
        Reward t6 = new Reward("floor6", 0, "rtshop:gacha_ticket_high", 1);
        t6.chance = 25;
        c.rewards.add(t6);
        return c;
    }

    public static Config parse(JsonObject o) {
        Config c = new Config();
        c.texts.putAll(defaultTexts());
        if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
        if (o.has("resetDay")) c.resetDay = Math.max(1, Math.min(7, o.get("resetDay").getAsInt()));
        if (o.has("resetHour")) c.resetHour = Math.max(0, Math.min(23, o.get("resetHour").getAsInt()));
        if (o.has("endless")) c.endless = o.get("endless").getAsBoolean();
        if (o.has("endlessStep")) c.endlessStep = Math.max(1, o.get("endlessStep").getAsInt());
        if (o.has("storyOnlyFloors")) c.storyOnlyFloors = o.get("storyOnlyFloors").getAsBoolean();
        if (o.has("gold") && o.get("gold").isJsonArray()) {
            JsonArray g = o.getAsJsonArray("gold");
            for (int i = 0; i < FLOORS && i < g.size(); i++) c.gold[i] = Math.max(0, g.get(i).getAsLong());
        }
        if (o.has("texts") && o.get("texts").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("texts").entrySet()) {
                if (e.getValue().isJsonPrimitive()) c.texts.put(e.getKey(), e.getValue().getAsString());
            }
        }
        if (o.has("rewards") && o.get("rewards").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("rewards")) {
                if (e.isJsonObject()) c.rewards.add(Reward.parse(e.getAsJsonObject()));
            }
        }
        return c;
    }

    public static void save(Config c) {
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] abyss.json 을 저장하지 못했습니다.", ex);
        }
    }

    private static Config load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                return parse(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] abyss.json 을 읽지 못해 기본값을 사용합니다.", ex);
            return fresh();
        }
        Config c = fresh();
        save(c);
        return c;
    }

    // ------------------------------------------------------------------ 기록 (월드 저장)

    public static final class Data extends SavedData {
        /** 플레이어 → {주 번호, 받은 층 비트}. */
        public final Map<UUID, long[]> players = new LinkedHashMap<>();

        public static Data get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rtshop_abyss_weekly");
        }

        public static Data load(CompoundTag tag) {
            Data d = new Data();
            ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                if (c.hasUUID("id")) d.players.put(c.getUUID("id"), new long[]{c.getLong("week"), c.getLong("mask")});
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Map.Entry<UUID, long[]> e : players.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", e.getKey());
                c.putLong("week", e.getValue()[0]);
                c.putLong("mask", e.getValue()[1]);
                list.add(c);
            }
            tag.put("players", list);
            return tag;
        }
    }

    /** 이번 주에 받은 층 비트 (비트 f-1 = f층). */
    public static long claimedMask(ServerPlayer p) {
        Config c = config();
        long[] v = Data.get(p.server).players.get(p.getUUID());
        return v != null && v[0] == Weekly.id(Weekly.now(), c.resetDay, c.resetHour) ? v[1] : 0;
    }

    public static void resetPlayer(ServerPlayer p) {
        Data d = Data.get(p.server);
        d.players.remove(p.getUUID());
        d.setDirty();
    }

    /**
     * 플레이어가 f층을 클리어했다: 이번 주에 그 층 보상을 아직 받지 않았으면 준다.
     * @return 받았으면 true
     */
    public static boolean cleared(ServerPlayer p, int f) {
        Config c = config();
        if (f >= 1 && f <= FLOORS) com.rtshop.quest.QuestManager.setFlag(p, "abyss_floor_" + f, true); // 이야기 퀘스트용 (영구)
        if (c.endless) return false; // 주간 보상은 무한 모드에서 준다
        return reward(p, f, false);
    }

    /** 무한 모드에서 wave 웨이브를 넘겼다: endlessStep 의 배수 단계(1~6)마다 이번 주 보상을 한 번 준다. */
    public static boolean endlessCleared(ServerPlayer p, int wave) {
        Config c = config();
        if (!c.endless || wave < c.endlessStep) return false;
        int tier = Math.min(FLOORS, wave / c.endlessStep);
        boolean any = false;
        for (int t = 1; t <= tier; t++) any |= reward(p, t, true); // 건너뛴 단계가 있으면 함께 준다
        return any;
    }

    /** 이야기가 부르지 않은 층인가 (층 모드 잠금). floor 7 = 무한 모드는 잠그지 않는다. 운영자의 크리에이티브는 통과. */
    public static boolean floorLocked(ServerPlayer p, int floor) {
        Config c = config();
        if (!c.storyOnlyFloors || floor < 1 || floor > FLOORS) return false;
        if (p.isCreative() && com.rtshop.util.Perm.op(p)) return false;
        return com.rtshop.quest.QuestManager.storyAbyssFloor(p) < floor;
    }

    /** 층 입장을 막아야 하면 알리고 true. (AbyssFloorLockMixin 이 부른다) */
    public static boolean refuseFloor(ServerPlayer p, int floor) {
        try {
            if (com.rtshop.rules.WorldRules.refusePortal(p)) return true;
            if (!floorLocked(p, floor)) return false;
            com.rtshop.notice.Notice.send(p, Component.literal(com.rtshop.story.StoryText.t(p, config().text("storyOnly"))).withStyle(ChatFormatting.YELLOW));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean reward(ServerPlayer p, int f, boolean endless) {
        Config c = config();
        if (!c.enabled || f < 1 || f > FLOORS) return false;
        long week = Weekly.id(Weekly.now(), c.resetDay, c.resetHour);
        Data d = Data.get(p.server);
        long[] v = d.players.get(p.getUUID());
        if (v == null || v[0] != week) {
            v = new long[]{week, 0};
            d.players.put(p.getUUID(), v);
        }
        long bit = 1L << (f - 1);
        if ((v[1] & bit) != 0) return false;
        v[1] |= bit;
        d.setDirty();
        List<String> names = new ArrayList<>();
        if (c.gold[f - 1] > 0) {
            Economy.addWallet(p, c.gold[f - 1]);
            com.rtshop.rank.Ranks.earned(p, c.gold[f - 1]);
            names.add(String.format("%,d G", c.gold[f - 1]));
        }
        for (Reward r : c.floor(f)) {
            ItemStack s = r.stack();
            if (s.isEmpty()) continue;
            double roll = (Raid.testRandom != null ? Raid.testRandom.getAsDouble() : p.getRandom().nextDouble()) * 100.0;
            if (roll >= r.chance) continue;
            names.add(s.getHoverName().getString() + (s.getCount() > 1 ? " ×" + s.getCount() : ""));
            Reward.give(p, s);
        }
        String msg;
        try {
            String what = endless ? Integer.toString(f * c.endlessStep) : ROMAN[f - 1];
            msg = names.isEmpty() ? String.format(com.rtshop.story.StoryText.t(p, c.text(endless ? "gotNothingEndless" : "gotNothing")), what)
                    : String.format(com.rtshop.story.StoryText.t(p, c.text(endless ? "gotEndless" : "got")), what, String.join(", ", names));
        } catch (Exception e) {
            msg = c.text("got");
        }
        com.rtshop.notice.Notice.send(p, Component.literal(msg).withStyle(ChatFormatting.GOLD));
        sendView(p);
        return true;
    }

    public static JsonObject view(ServerPlayer p) {
        Config c = config();
        JsonObject o = new JsonObject();
        o.addProperty("enabled", c.enabled);
        o.addProperty("endless", c.endless);
        o.addProperty("step", c.endlessStep);
        o.addProperty("mask", claimedMask(p));
        o.addProperty("resetIn", Weekly.untilReset(Weekly.now(), c.resetDay, c.resetHour));
        JsonObject t = new JsonObject();
        for (String k : defaultTexts().keySet()) t.addProperty(k, com.rtshop.story.StoryText.t(p, c.text(k)));
        o.add("texts", t);
        JsonArray g = new JsonArray();
        for (long v : c.gold) g.add(v);
        o.add("gold", g);
        JsonArray rw = new JsonArray();
        for (Reward r : c.rewards) {
            if (!r.stack().isEmpty()) rw.add(r.toJson());
        }
        o.add("rewards", rw);
        return o;
    }

    public static void sendView(ServerPlayer p) {
        if (p instanceof FakePlayer || p.connection == null) return;
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new RaidPacket(RaidPacket.ABYSS, view(p).toString()));
    }

    // ------------------------------------------------------------------ Spiral Abyss 진행 읽기

    private static boolean probed, present;
    private static Method mParties, mSessions, mController, mState, mFloor, mPlayers, mWave;
    private static final Map<Object, String> LAST = new IdentityHashMap<>();

    private static boolean probe() {
        if (probed) return present;
        probed = true;
        if (!ModList.get().isLoaded("spiral_abyss")) return false;
        try {
            Class<?> parties = Class.forName("com.pla.spiral_abyss.world.AbyssParties");
            Class<?> session = Class.forName("com.pla.spiral_abyss.world.ArenaSession");
            Class<?> controller = Class.forName("com.pla.spiral_abyss.encounter.FloorController");
            mParties = parties.getMethod("get", MinecraftServer.class);
            mSessions = parties.getMethod("sessions");
            mController = session.getMethod("controller");
            mPlayers = session.getMethod("players");
            mState = controller.getMethod("state");
            mFloor = controller.getMethod("currentFloor");
            try {
                mWave = controller.getMethod("currentWave");
            } catch (Throwable ignored) {
                mWave = null;
            }
            present = true;
            RtShop.LOGGER.info("[rtshop] Spiral Abyss 를 찾았습니다: 층 클리어 시 주간 보상을 줍니다.");
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] Spiral Abyss 의 진행 상태를 읽을 수 없어 주간 보상이 자동으로 지급되지 않습니다 (모드 버전이 다를 수 있음): {}", t.toString());
        }
        return present;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null || event.getServer().getTickCount() % 10 != 5) return;
        if (!config().enabled || !probe()) return;
        try {
            Object parties = mParties.invoke(null, event.getServer());
            Collection<?> sessions = (Collection<?>) mSessions.invoke(parties);
            LAST.keySet().removeIf(k -> !sessions.contains(k));
            for (Object s : sessions) {
                Object ctl = mController.invoke(s);
                String state = String.valueOf(mState.invoke(ctl));
                String before = LAST.put(s, state);
                if ("COMPLETED".equals(state) && before != null && !"COMPLETED".equals(before)) {
                    int floor = (Integer) mFloor.invoke(ctl);
                    int wave = floor == 7 && mWave != null ? (Integer) mWave.invoke(ctl) : 0;
                    for (Object pl : (List<?>) mPlayers.invoke(s)) {
                        if (!(pl instanceof ServerPlayer sp)) continue;
                        if (floor == 7) endlessCleared(sp, wave); // 무한 모드: 10웨이브마다 COMPLETED(검문소)가 된다
                        else cleared(sp, floor);
                    }
                }
            }
        } catch (Throwable t) {
            present = false;
            RtShop.LOGGER.warn("[rtshop] Spiral Abyss 진행 상태를 읽다가 오류가 나서 주간 보상 자동 지급을 멈춥니다: {}", t.toString());
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        LAST.clear();
    }

    public static boolean detected() {
        return probe();
    }

    // ------------------------------------------------------------------ 편집기 / 명령어

    static {
        RewardEditor.TARGETS.put("abyss", new RewardEditor.Target() {
            @Override
            public JsonObject model(ServerPlayer p) {
                Config c = config();
                JsonObject o = new JsonObject();
                o.addProperty("id", "abyss");
                boolean en = com.rtshop.story.StoryText.en(p);
                o.addProperty("title", en ? "Spiral Abyss Weekly Reward Settings" : "Spiral Abyss 주간 보상 설정");
                JsonArray f = new JsonArray();
                for (int i = 0; i < FLOORS; i++) {
                    f.add(RewardEditor.field("gold" + i, (en ? (c.endless ? "Endless wave " + ((i + 1) * c.endlessStep) : "Floor " + ROMAN[i] + " clear")
                            : (c.endless ? "무한 " + ((i + 1) * c.endlessStep) + "웨이브" : ROMAN[i] + "층 클리어")) + " Gold", true, Long.toString(c.gold[i])));
                }
                f.add(RewardEditor.field("resetDay", en ? "Weekly reset day (1 = Monday … 7 = Sunday)" : "주간 초기화 요일 (1 = 월요일 … 7 = 일요일)", true, Integer.toString(c.resetDay)));
                f.add(RewardEditor.field("resetHour", en ? "Weekly reset hour (0-23)" : "주간 초기화 시각 (0~23시)", true, Integer.toString(c.resetHour)));
                for (String k : defaultTexts().keySet()) f.add(RewardEditor.field("text." + k, RaidConfig.label(k, en), false, c.text(k)));
                o.add("fields", f);
                JsonArray g = new JsonArray();
                for (int i = 1; i <= FLOORS; i++) g.add(RewardEditor.group("floor" + i, en ? (c.endless ? "Endless wave " + (i * c.endlessStep) : "Floor " + ROMAN[i - 1])
                        : (c.endless ? "무한 " + (i * c.endlessStep) + "웨이브" : ROMAN[i - 1] + "층"), false, true));
                o.add("groups", g);
                o.add("rewards", RewardEditor.rewardsJson(c.rewards));
                return o;
            }

            @Override
            public void apply(ServerPlayer p, JsonObject edited) {
                Config c = config();
                Map<String, String> f = RewardEditor.fields(edited);
                JsonObject j = c.toJson();
                JsonArray g = new JsonArray();
                for (int i = 0; i < FLOORS; i++) g.add((long) RewardEditor.dbl(f, "gold" + i, c.gold[i]));
                j.add("gold", g);
                j.addProperty("resetDay", (int) RewardEditor.dbl(f, "resetDay", c.resetDay));
                j.addProperty("resetHour", (int) RewardEditor.dbl(f, "resetHour", c.resetHour));
                JsonObject t = j.getAsJsonObject("texts");
                for (Map.Entry<String, String> e : f.entrySet()) {
                    if (e.getKey().startsWith("text.")) t.addProperty(e.getKey().substring(5), e.getValue());
                }
                if (edited.has("rewards")) j.add("rewards", RewardEditor.rewardsJson(RewardEditor.rewards(edited)));
                save(parse(j));
                reload();
            }

            @Override
            public void add(ServerPlayer p, Reward r) {
                Config c = config();
                r.chance = 100;
                c.rewards.add(r);
                save(c);
            }
        });
    }

    /** /rtshop abyss edit | reset <플레이어> | clear <플레이어> <층> */
    public static LiteralArgumentBuilder<CommandSourceStack> adminCommand() {
        return Commands.literal("abyss").requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(),
                            "Spiral Abyss 주간 보상: " + (config().enabled ? "켜짐" : "꺼짐") + " · 모드 연결 " + (detected() ? "됨" : "안 됨"),
                            "Spiral Abyss weekly rewards: " + (config().enabled ? "on" : "off") + " · mod " + (detected() ? "found" : "not found"))
                            + "  (/rtshop abyss edit)"), false);
                    return 1;
                })
                .then(Commands.literal("edit").executes(ctx -> {
                    Raid.openEditor(ctx.getSource().getPlayerOrException(), "abyss");
                    return 1;
                }))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                    resetPlayer(t);
                    sendView(t);
                    ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(),
                            t.getGameProfile().getName() + " 의 이번 주 주간 보상 기록을 지웠습니다.", "Cleared this week's weekly reward record for " + t.getGameProfile().getName() + ".")), true);
                    return 1;
                })))
                .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1, FLOORS)).executes(ctx -> {
                            ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                            boolean ok = cleared(t, IntegerArgumentType.getInteger(ctx, "floor"));
                            ctx.getSource().sendSuccess(() -> Component.literal(ok
                                    ? com.rtshop.story.StoryText.pick(ctx.getSource(), "그 층의 주간 보상을 지급했습니다.", "Gave that floor's weekly reward.")
                                    : com.rtshop.story.StoryText.pick(ctx.getSource(), "이번 주에 이미 받은 층입니다.", "That floor was already claimed this week.")), true);
                            return ok ? 1 : 0;
                        }))));
    }
}
