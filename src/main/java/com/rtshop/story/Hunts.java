package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.job.Jobs;
import com.rtshop.quest.QuestManager;
import com.rtshop.raid.Weekly;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 토벌 의뢰판과 몹 도감 (config/rtshop/hunts.json).
 * <ul>
 * <li>토벌 의뢰: 게시판(시청 서기 하루)에 매일 dailyOffers 개, 매주 weeklyOffers 개가 붙고, 그중 dailyCount·weeklyCount 개까지 골라 맡는다. 몹은 깨어 둔 장(표식)에 따라 늘어난다.
 * 다 잡으면 바로 Gold. 전투 직업이 있으면 jobBonus 만큼 더 준다. /bounty 로 본다.</li>
 * <li>몹 도감: 처음 잡은 몹을 기록하고, 장별 목록을 다 채우면 한 번 보상. /bestiary 로 본다.</li>
 * </ul>
 * 처치 인정은 퀘스트와 같다 (보스: 최대 체력 10% 이상 피해, 일반 몹: 가까운 파티원도).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Hunts {
    private Hunts() {
    }

    public static final class Target {
        public String entity;
        public int min, max;
        public long gold;

        Target(String e, int min, int max, long gold) {
            entity = e;
            this.min = min;
            this.max = max;
            this.gold = gold;
        }
    }

    public static final class Pool {
        public String flag = "";
        public final List<Target> targets = new ArrayList<>();
    }

    public static final class Book {
        public String id = "", name = "";
        public long gold;
        public final List<String> entities = new ArrayList<>();
    }

    public boolean enabled = true;
    public int dailyCount = 3, weeklyCount = 1;
    /** 게시판에 붙는 의뢰 수 (이 중에서 dailyCount·weeklyCount 개까지 골라 맡는다) */
    public int dailyOffers = 6, weeklyOffers = 3;
    /** 게시판을 맡는 NPC (말을 걸면 [현상금 게시판] 이 보인다) */
    public List<String> boardNpcs = new ArrayList<>(List.of("attendance"));
    public double weeklyMultiplier = 3.0;
    /** 하루가 바뀌는 시각 (한국 시간 기준 시) */
    public int resetHour = 6;
    /** 전투 직업이 있으면 보상 배율 +이만큼 */
    public double jobBonus = 0.2;
    public final List<Pool> pools = new ArrayList<>();
    public final List<Book> books = new ArrayList<>();

    private static Hunts current;
    private static final Random RNG = new Random();

    public static synchronized Hunts get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void reload() {
        current = load();
    }

    private static Pool pool(String flag, Target... t) {
        Pool p = new Pool();
        p.flag = flag;
        p.targets.addAll(List.of(t));
        return p;
    }

    private static Target t(String e, int min, int max, long gold) {
        return new Target(e, min, max, gold);
    }

    private static Book book(String id, String name, long gold, String... e) {
        Book b = new Book();
        b.id = id;
        b.name = name;
        b.gold = gold;
        b.entities.addAll(List.of(e));
        return b;
    }

    static Hunts defaults() {
        Hunts h = new Hunts();
        h.pools.add(pool("",
                t("minecraft:zombie", 12, 24, 20), t("minecraft:skeleton", 10, 20, 25), t("minecraft:spider", 10, 20, 22),
                t("minecraft:creeper", 6, 12, 40), t("minecraft:drowned", 8, 16, 30), t("minecraft:husk", 8, 16, 30),
                t("minecraft:stray", 8, 16, 30), t("minecraft:witch", 2, 5, 120), t("minecraft:enderman", 3, 8, 110),
                t("minecraft:pillager", 6, 12, 45), t("minecraft:slime", 8, 16, 25)));
        h.pools.add(pool("story_ch1_done",
                t("mowziesmobs:foliaath", 3, 6, 180), t("mowziesmobs:lantern", 3, 6, 120), t("mowziesmobs:naga", 2, 4, 260),
                t("mowziesmobs:umvuthana", 5, 10, 140), t("mowziesmobs:umvuthana_raptor", 1, 3, 300), t("mowziesmobs:grottol", 1, 2, 500),
                t("minecraft:wither_skeleton", 6, 12, 90), t("minecraft:blaze", 6, 12, 90)));
        h.pools.add(pool("story_ch2_done",
                t("aquamirae:tortured_soul", 4, 8, 220), t("aquamirae:anglerfish", 2, 4, 300), t("aquamirae:eel", 1, 2, 600),
                t("aquamirae:maw", 3, 6, 220), t("aquamirae:spinefish", 4, 8, 160), t("aquamirae:abyssal_scyphoid", 2, 4, 260)));
        h.pools.add(pool("story_ch3_done",
                t("cataclysm:koboleton", 6, 12, 200), t("cataclysm:the_watcher", 4, 8, 260), t("cataclysm:deepling", 6, 12, 220),
                t("cataclysm:deepling_brute", 2, 4, 450), t("cataclysm:draugr", 6, 12, 220), t("cataclysm:elite_draugr", 2, 4, 450),
                t("cataclysm:endermaptera", 4, 8, 260), t("cataclysm:ignited_revenant", 2, 4, 500), t("cataclysm:the_prowler", 1, 1, 2500)));
        h.books.add(book("vanilla", "골디아의 들짐승", 3000, "minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper",
                "minecraft:drowned", "minecraft:husk", "minecraft:stray", "minecraft:witch", "minecraft:enderman", "minecraft:pillager",
                "minecraft:slime", "minecraft:phantom", "minecraft:blaze", "minecraft:wither_skeleton", "minecraft:piglin_brute"));
        h.books.add(book("guardians", "깨어난 수호자들", 8000, "mowziesmobs:foliaath", "mowziesmobs:lantern", "mowziesmobs:naga",
                "mowziesmobs:umvuthana", "mowziesmobs:umvuthana_raptor", "mowziesmobs:grottol", "mowziesmobs:ferrous_wroughtnaut",
                "mowziesmobs:frostmaw", "mowziesmobs:sculptor", "mowziesmobs:umvuthi"));
        h.books.add(book("frozen_sea", "얼어붙은 바다", 12000, "aquamirae:tortured_soul", "aquamirae:anglerfish", "aquamirae:eel",
                "aquamirae:maw", "aquamirae:spinefish", "aquamirae:abyssal_scyphoid", "aquamirae:maze_mother", "aquamirae:captain_cornelia"));
        h.books.add(book("seven_seals", "일곱 봉인", 20000, "cataclysm:koboleton", "cataclysm:kobolediator", "cataclysm:the_watcher",
                "cataclysm:the_prowler", "cataclysm:deepling", "cataclysm:deepling_brute", "cataclysm:deepling_priest", "cataclysm:endermaptera",
                "cataclysm:draugr", "cataclysm:elite_draugr", "cataclysm:royal_draugr", "cataclysm:ignited_revenant",
                "cataclysm:netherite_monstrosity", "cataclysm:ancient_remnant", "cataclysm:the_harbinger", "cataclysm:the_leviathan",
                "cataclysm:ender_guardian", "cataclysm:maledictus", "cataclysm:ignis"));
        h.books.add(book("hidden", "지워진 이름들", 15000, "cataclysm:scylla", "illusioner:illusioner"));
        return h;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("hunts.json");
    }

    private static Hunts load() {
        Path f = file();
        try {
            if (Files.exists(f)) return fromJson(JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject());
            Hunts d = defaults();
            Files.createDirectories(f.getParent());
            Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(d.toJson()), StandardCharsets.UTF_8);
            return d;
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] hunts.json 을 읽지 못했습니다: {}", ex.toString());
            return defaults();
        }
    }

    JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "토벌 의뢰판(/bounty)과 몹 도감(/bestiary). pools: flag 표식을 가진 사람에게 그 몹들이 의뢰로 나온다 (\"\" = 누구나). "
                + "targets: [몹 ID, 최소, 최대, 마리당 Gold]. weekly 의뢰는 weeklyMultiplier 배로 많고 비싸다. jobBonus: 전투 직업이 있으면 보상 +비율. "
                + "books: 장별 도감, 들어 있는 몹(설치된 모드만)을 모두 잡으면 gold 를 한 번 준다.");
        o.addProperty("enabled", enabled);
        o.addProperty("dailyCount", dailyCount);
        o.addProperty("weeklyCount", weeklyCount);
        o.addProperty("dailyOffers", dailyOffers);
        o.addProperty("weeklyOffers", weeklyOffers);
        com.google.gson.JsonArray bn = new com.google.gson.JsonArray();
        boardNpcs.forEach(bn::add);
        o.add("boardNpcs", bn);
        o.addProperty("weeklyMultiplier", weeklyMultiplier);
        o.addProperty("resetHour", resetHour);
        o.addProperty("jobBonus", jobBonus);
        JsonArray ps = new JsonArray();
        for (Pool p : pools) {
            JsonObject x = new JsonObject();
            x.addProperty("flag", p.flag);
            JsonArray ts = new JsonArray();
            for (Target t : p.targets) {
                JsonArray a = new JsonArray();
                a.add(t.entity);
                a.add(t.min);
                a.add(t.max);
                a.add(t.gold);
                ts.add(a);
            }
            x.add("targets", ts);
            ps.add(x);
        }
        o.add("pools", ps);
        JsonArray bs = new JsonArray();
        for (Book b : books) {
            JsonObject x = new JsonObject();
            x.addProperty("id", b.id);
            x.addProperty("name", b.name);
            x.addProperty("gold", b.gold);
            JsonArray es = new JsonArray();
            b.entities.forEach(es::add);
            x.add("entities", es);
            bs.add(x);
        }
        o.add("books", bs);
        return o;
    }

    static Hunts fromJson(JsonObject o) {
        Hunts h = new Hunts();
        if (o.has("enabled")) h.enabled = o.get("enabled").getAsBoolean();
        if (o.has("dailyCount")) h.dailyCount = Math.max(0, o.get("dailyCount").getAsInt());
        if (o.has("weeklyCount")) h.weeklyCount = Math.max(0, o.get("weeklyCount").getAsInt());
        if (o.has("dailyOffers")) h.dailyOffers = Math.max(h.dailyCount, o.get("dailyOffers").getAsInt());
        if (o.has("weeklyOffers")) h.weeklyOffers = Math.max(h.weeklyCount, o.get("weeklyOffers").getAsInt());
        if (o.has("boardNpcs")) {
            h.boardNpcs.clear();
            for (com.google.gson.JsonElement e : o.getAsJsonArray("boardNpcs")) h.boardNpcs.add(e.getAsString());
        }
        if (o.has("weeklyMultiplier")) h.weeklyMultiplier = Math.max(1, o.get("weeklyMultiplier").getAsDouble());
        if (o.has("resetHour")) h.resetHour = Math.max(0, Math.min(23, o.get("resetHour").getAsInt()));
        if (o.has("jobBonus")) h.jobBonus = Math.max(0, o.get("jobBonus").getAsDouble());
        if (o.has("pools")) {
            for (JsonElement e : o.getAsJsonArray("pools")) {
                JsonObject x = e.getAsJsonObject();
                Pool p = new Pool();
                p.flag = x.has("flag") ? x.get("flag").getAsString() : "";
                for (JsonElement te : x.getAsJsonArray("targets")) {
                    JsonArray a = te.getAsJsonArray();
                    p.targets.add(new Target(a.get(0).getAsString(), a.get(1).getAsInt(), Math.max(a.get(1).getAsInt(), a.get(2).getAsInt()), a.get(3).getAsLong()));
                }
                h.pools.add(p);
            }
        }
        if (o.has("books")) {
            for (JsonElement e : o.getAsJsonArray("books")) {
                JsonObject x = e.getAsJsonObject();
                Book b = new Book();
                b.id = x.get("id").getAsString();
                b.name = x.has("name") ? x.get("name").getAsString() : b.id;
                b.gold = x.has("gold") ? x.get("gold").getAsLong() : 0;
                for (JsonElement en : x.getAsJsonArray("entities")) b.entities.add(en.getAsString());
                h.books.add(b);
            }
        }
        return h;
    }

    // ------------------------------------------------------------------ 상태

    static boolean installed(String entity) {
        ResourceLocation rl = ResourceLocation.tryParse(entity);
        if (rl == null) return false;
        if (!rl.getNamespace().equals("minecraft") && !ModList.get().isLoaded(rl.getNamespace())) return false;
        return ForgeRegistries.ENTITY_TYPES.containsKey(rl);
    }

    public static String name(String entity) {
        ResourceLocation rl = ResourceLocation.tryParse(entity);
        EntityType<?> t = rl == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(rl);
        return t == null ? entity : t.getDescription().getString();
    }

    static long dayId() {
        long ms = Weekly.now() + 9L * 3600_000L - get().resetHour * 3600_000L; // 한국 시간 기준
        return Math.floorDiv(ms, 86_400_000L);
    }

    static long weekId() {
        return Weekly.id(Weekly.now(), 1, get().resetHour);
    }

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains("rtshop_hunts")) root.put("rtshop_hunts", new CompoundTag());
        return root.getCompound("rtshop_hunts");
    }

    /** 오늘·이번 주 의뢰 (없거나 날이 바뀌었으면 새로 뽑는다). */
    public static ListTag bounties(ServerPlayer p) {
        CompoundTag d = data(p);
        Hunts h = get();
        long day = dayId(), week = weekId();
        ListTag list = d.getList("list", Tag.TAG_COMPOUND);
        boolean newDay = d.getLong("day") != day, newWeek = d.getLong("week") != week;
        if (newDay || newWeek) {
            ListTag keep = new ListTag();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag b = list.getCompound(i);
                if (b.getBoolean("weekly") ? !newWeek : !newDay) keep.add(b);
            }
            List<Target> avail = available(p);
            if (newDay) roll(keep, avail, Math.max(h.dailyCount, h.dailyOffers), false, 1.0);
            if (newWeek) roll(keep, avail, Math.max(h.weeklyCount, h.weeklyOffers), true, h.weeklyMultiplier);
            d.putLong("day", day);
            d.putLong("week", week);
            d.put("list", keep);
            list = keep;
        }
        return list;
    }

    public static List<Target> available(ServerPlayer p) {
        List<Target> out = new ArrayList<>();
        for (Pool pool : get().pools) {
            if (!pool.flag.isEmpty() && !QuestManager.hasFlag(p, pool.flag)) continue;
            for (Target t : pool.targets) if (installed(t.entity)) out.add(t);
        }
        return out;
    }

    private static void roll(ListTag into, List<Target> avail, int n, boolean weekly, double mult) {
        List<Target> bag = new ArrayList<>(avail);
        // 새로 열린 장의 몹이 더 자주 나오도록 뒤쪽(나중 장)부터 고르기 쉽게 한다
        for (int i = 0; i < n && !bag.isEmpty(); i++) {
            int pick = bag.size() > 3 && RNG.nextDouble() < 0.5 ? bag.size() - 1 - RNG.nextInt(Math.min(6, bag.size())) : RNG.nextInt(bag.size());
            Target t = bag.remove(pick);
            int need = (int) Math.round((t.min + RNG.nextInt(t.max - t.min + 1)) * (weekly ? mult : 1));
            CompoundTag b = new CompoundTag();
            b.putString("entity", t.entity);
            b.putInt("need", Math.max(1, need));
            b.putInt("have", 0);
            b.putLong("gold", Math.round(t.gold * Math.max(1, need) * (weekly ? 1.25 : 1.0)));
            b.putBoolean("weekly", weekly);
            b.putBoolean("done", false);
            b.putBoolean("accepted", false); // 게시판에서 골라야 시작된다
            into.add(b);
        }
    }

    public static boolean seen(ServerPlayer p, String entity) {
        return data(p).getCompound("seen").contains(entity);
    }

    // ------------------------------------------------------------------ 처치

    /** 처치로 인정된 사람마다 한 번 (QuestManager.onKill 에서). */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        Hunts h = get();
        if (!h.enabled || victim instanceof ServerPlayer) return;
        if (p instanceof net.minecraftforge.common.util.FakePlayer && !testMode) return; // 다른 자동 점검의 Gold 계산을 흔들지 않게
        String id = BossIntros.id(victim);
        CompoundTag d = data(p);
        // 도감
        CompoundTag seen = d.getCompound("seen");
        if (!seen.contains(id)) {
            seen.putLong(id, System.currentTimeMillis());
            d.put("seen", seen);
            for (Book b : h.books) {
                if (!b.entities.contains(id)) continue;
                p.displayClientMessage(Component.translatable("rtshop.hunt.new_entry", victim.getType().getDescription(), b.name).withStyle(ChatFormatting.GREEN), true);
                checkBook(p, b);
            }
        }
        // 의뢰
        ListTag list = bounties(p);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag b = list.getCompound(i);
            if (b.getBoolean("done") || !accepted(b) || !b.getString("entity").equals(id)) continue;
            int have = b.getInt("have") + 1;
            b.putInt("have", have);
            int need = b.getInt("need");
            if (have >= need) {
                b.putBoolean("done", true);
                long gold = reward(p, b.getLong("gold"));
                Economy.addWallet(p, gold);
                p.sendSystemMessage(Component.translatable(b.getBoolean("weekly") ? "rtshop.hunt.weekly_done" : "rtshop.hunt.daily_done",
                        victim.getType().getDescription(), need, String.format("%,d", gold)).withStyle(ChatFormatting.GOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
            } else {
                p.displayClientMessage(Component.translatable("rtshop.hunt.progress", victim.getType().getDescription(), have, need).withStyle(ChatFormatting.YELLOW), true);
            }
            break;
        }
    }

    /** 맡은 의뢰인가 (예전 저장에는 표시가 없다: 맡은 것으로 본다). */
    public static boolean accepted(CompoundTag b) {
        return !b.contains("accepted") || b.getBoolean("accepted");
    }

    private static int taken(ListTag list, boolean weekly) {
        int n = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag b = list.getCompound(i);
            if (b.getBoolean("weekly") == weekly && accepted(b)) n++;
        }
        return n;
    }

    /** 게시판 NPC 곁(8칸)인가. */
    public static boolean nearBoard(ServerPlayer p, int entity) {
        if (testMode && entity == -1) return true;
        net.minecraft.world.entity.Entity e = p.level().getEntity(entity);
        if (e == null || e.distanceTo(p) > 8) return false;
        String key = QuestManager.npcKey(e);
        return key != null && get().boardNpcs.contains(key);
    }

    /** 게시판에서 의뢰 하나를 맡는다. 0 = 맡음, 1 = 이미/끝남, 2 = 그 종류는 더 못 맡음, 3 = 게시판 곁이 아님. */
    public static int take(ServerPlayer p, int entity, int index) {
        if (!nearBoard(p, entity)) return 3;
        ListTag list = bounties(p);
        if (index < 0 || index >= list.size()) return 1;
        CompoundTag b = list.getCompound(index);
        if (accepted(b) || b.getBoolean("done")) return 1;
        boolean weekly = b.getBoolean("weekly");
        if (taken(list, weekly) >= (weekly ? get().weeklyCount : get().dailyCount)) return 2;
        b.putBoolean("accepted", true);
        return 0;
    }

    /** 게시판 화면에 보낼 내용. */
    public static com.google.gson.JsonObject boardJson(ServerPlayer p, int entity) {
        ListTag list = bounties(p);
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("entity", entity);
        o.addProperty("dailyMax", get().dailyCount);
        o.addProperty("weeklyMax", get().weeklyCount);
        o.addProperty("dailyTaken", taken(list, false));
        o.addProperty("weeklyTaken", taken(list, true));
        com.google.gson.JsonArray a = new com.google.gson.JsonArray();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag b = list.getCompound(i);
            com.google.gson.JsonObject x = new com.google.gson.JsonObject();
            x.addProperty("i", i);
            x.addProperty("entity", b.getString("entity"));
            x.addProperty("name", name(b.getString("entity")));
            x.addProperty("need", b.getInt("need"));
            x.addProperty("have", Math.min(b.getInt("have"), b.getInt("need")));
            x.addProperty("gold", reward(p, b.getLong("gold")));
            x.addProperty("weekly", b.getBoolean("weekly"));
            x.addProperty("accepted", accepted(b));
            x.addProperty("done", b.getBoolean("done"));
            a.add(x);
        }
        o.add("list", a);
        return o;
    }

    public static void openBoard(ServerPlayer p, int entity) {
        if (!nearBoard(p, entity)) {
            p.displayClientMessage(Component.translatable("rtshop.hunt.board_far").withStyle(ChatFormatting.RED), true);
            return;
        }
        com.rtshop.net.ShopNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                new com.rtshop.net.StoryPacket(com.rtshop.net.StoryPacket.BOUNTY, boardJson(p, entity).toString()));
    }

    public static long reward(ServerPlayer p, long base) {
        boolean fighter = !Jobs.entry(p).combat.isEmpty();
        return Math.round(base * (fighter ? 1 + get().jobBonus : 1));
    }

    static int[] progress(ServerPlayer p, Book b) {
        CompoundTag seen = data(p).getCompound("seen");
        int have = 0, total = 0;
        for (String e : b.entities) {
            if (!installed(e)) continue;
            total++;
            if (seen.contains(e)) have++;
        }
        return new int[]{have, total};
    }

    static void checkBook(ServerPlayer p, Book b) {
        int[] pr = progress(p, b);
        CompoundTag d = data(p);
        CompoundTag done = d.getCompound("books");
        if (pr[1] == 0 || pr[0] < pr[1] || done.contains(b.id)) return;
        done.putBoolean(b.id, true);
        d.put("books", done);
        Economy.addWallet(p, b.gold);
        p.sendSystemMessage(Component.translatable("rtshop.hunt.book_done", b.name, String.format("%,d", b.gold)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1F);
    }

    // ------------------------------------------------------------------ 보기

    public static void showBounties(ServerPlayer p) {
        ListTag list = bounties(p);
        p.sendSystemMessage(Component.translatable("rtshop.hunt.board_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (list.isEmpty()) p.sendSystemMessage(Component.translatable("rtshop.hunt.none").withStyle(ChatFormatting.GRAY));
        int shown = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag b = list.getCompound(i);
            if (!accepted(b)) continue;
            shown++;
            boolean done = b.getBoolean("done");
            MutableComponent line = Component.literal(done ? " ✔ " : " ✦ ").withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GOLD)
                    .append(Component.translatable(b.getBoolean("weekly") ? "rtshop.hunt.tag_weekly" : "rtshop.hunt.tag_daily").withStyle(ChatFormatting.DARK_AQUA))
                    .append(Component.literal(" " + name(b.getString("entity")) + " " + Math.min(b.getInt("have"), b.getInt("need")) + "/" + b.getInt("need"))
                            .withStyle(done ? ChatFormatting.GRAY : ChatFormatting.WHITE))
                    .append(Component.literal("  " + String.format("%,d", reward(p, b.getLong("gold"))) + " G").withStyle(ChatFormatting.YELLOW));
            p.sendSystemMessage(line);
        }
        if (shown == 0) p.sendSystemMessage(Component.translatable("rtshop.hunt.none_taken").withStyle(ChatFormatting.GRAY));
        boolean fighter = !Jobs.entry(p).combat.isEmpty();
        p.sendSystemMessage(Component.translatable(fighter ? "rtshop.hunt.job_bonus_on" : "rtshop.hunt.job_bonus_off", Math.round(get().jobBonus * 100))
                .withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.translatable("rtshop.hunt.open_book").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bestiary"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/bestiary")))));
    }

    public static void showBestiary(ServerPlayer p, String bookId) {
        Hunts h = get();
        if (bookId == null) {
            p.sendSystemMessage(Component.translatable("rtshop.hunt.book_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            CompoundTag done = data(p).getCompound("books");
            for (Book b : h.books) {
                int[] pr = progress(p, b);
                if (pr[1] == 0) continue;
                p.sendSystemMessage(Component.literal((done.contains(b.id) ? " ✔ " : " ✦ ") + b.name + " " + pr[0] + "/" + pr[1]
                                + (done.contains(b.id) ? "" : "  (" + String.format("%,d", b.gold) + " G)"))
                        .withStyle(Style.EMPTY.withColor(done.contains(b.id) ? ChatFormatting.GREEN : ChatFormatting.WHITE)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bestiary " + b.id))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("rtshop.hunt.click_book")))));
            }
            return;
        }
        for (Book b : h.books) {
            if (!b.id.equals(bookId)) continue;
            int[] pr = progress(p, b);
            p.sendSystemMessage(Component.literal("— " + b.name + " " + pr[0] + "/" + pr[1] + " —").withStyle(ChatFormatting.GOLD));
            CompoundTag seen = data(p).getCompound("seen");
            for (String e : b.entities) {
                if (!installed(e)) continue;
                boolean s = seen.contains(e);
                p.sendSystemMessage(Component.literal(s ? " ✔ " + name(e) : " ✧ ???").withStyle(s ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
            }
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bounty").executes(ctx -> {
            showBounties(ctx.getSource().getPlayerOrException());
            return 1;
        }).then(Commands.literal("board").then(Commands.argument("entity", com.mojang.brigadier.arguments.IntegerArgumentType.integer()).executes(ctx -> {
            openBoard(ctx.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "entity"));
            return 1;
        }))).then(Commands.literal("take").then(Commands.argument("entity", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                .then(Commands.argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int ent = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "entity");
                    int r = take(p, ent, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "index"));
                    if (r == 2) p.displayClientMessage(Component.translatable("rtshop.hunt.board_full").withStyle(ChatFormatting.RED), true);
                    if (r == 0) p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 1F);
                    openBoard(p, ent);
                    return r == 0 ? 1 : 0;
                })))));
        event.getDispatcher().register(Commands.literal("bestiary").executes(ctx -> {
            showBestiary(ctx.getSource().getPlayerOrException(), null);
            return 1;
        }).then(Commands.argument("book", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
            showBestiary(ctx.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "book"));
            return 1;
        })));
    }

    public static boolean testMode;

    /** 점검용: 의뢰를 정해 둔다. */
    public static void setForTest(ServerPlayer p, String entity, int need, long gold) {
        bounties(p);
        ListTag l = new ListTag();
        CompoundTag b = new CompoundTag();
        b.putString("entity", entity);
        b.putInt("need", need);
        b.putInt("have", 0);
        b.putLong("gold", gold);
        b.putBoolean("weekly", false);
        b.putBoolean("done", false);
        b.putBoolean("accepted", true);
        l.add(b);
        data(p).put("list", l);
    }

    public static void clearForTest(ServerPlayer p) {
        p.getPersistentData().remove("rtshop_hunts");
    }
}
