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

    static final int VERSION = 2;
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

    /** 수량·보상 모두 난이도로 자동 */
    private static Target a(String e) {
        return new Target(e, 0, 0, 0);
    }

    // ------------------------------------------------------------------ 난이도
    private static final java.util.Map<String, Double> SCORE = new java.util.concurrent.ConcurrentHashMap<>();
    /** 기본 능력치만으로는 덜 보이는 위험 (폭발, 원거리, 물약 등) */
    private static final java.util.Map<String, Double> EXTRA = java.util.Map.ofEntries(
            java.util.Map.entry("minecraft:creeper", 1.8), java.util.Map.entry("minecraft:witch", 1.8),
            java.util.Map.entry("minecraft:skeleton", 1.25), java.util.Map.entry("minecraft:stray", 1.35),
            java.util.Map.entry("minecraft:pillager", 1.25), java.util.Map.entry("minecraft:blaze", 1.4),
            java.util.Map.entry("minecraft:ghast", 1.4), java.util.Map.entry("minecraft:phantom", 1.3),
            java.util.Map.entry("minecraft:evoker", 2.5), java.util.Map.entry("minecraft:slime", 0.8),
            java.util.Map.entry("minecraft:wither_skeleton", 1.3), java.util.Map.entry("minecraft:cave_spider", 1.3));

    /** 몹의 난이도 점수 (좀비 ≈ 1.6). 체력·공격력·방어력으로 잰다. */
    @SuppressWarnings("unchecked")
    public static double difficulty(String entity) {
        return SCORE.computeIfAbsent(entity, e -> {
            double hp = 20, atk = 3, armor = 0;
            try {
                ResourceLocation rl = ResourceLocation.tryParse(e);
                EntityType<?> t = rl == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(rl);
                if (t != null && net.minecraft.world.entity.ai.attributes.DefaultAttributes.hasSupplier(t)) {
                    var sup = net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) t);
                    hp = sup.getBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                    if (sup.hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))
                        atk = sup.getBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                    if (sup.hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR))
                        armor = sup.getBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
                }
            } catch (Exception ignored) {
            }
            double sc = Math.pow(Math.max(1, hp) / 20.0, 0.9) * (1 + Math.max(0, atk) / 6.0) * (1 + Math.max(0, armor) / 20.0);
            return Math.max(0.3, sc * EXTRA.getOrDefault(e, 1.0));
        });
    }

    /** 난이도 별 1~5 */
    public static int stars(String entity) {
        double d = difficulty(entity);
        return d < 1.5 ? 1 : d < 3 ? 2 : d < 6 ? 3 : d < 15 ? 4 : 5;
    }

    /** 마리당 Gold (목록에 적은 값이 있으면 그 값, 0 이면 난이도로) */
    public static long goldEach(Target t) {
        if (t.gold > 0) return t.gold;
        long g = Math.round(15 * Math.pow(difficulty(t.entity), 1.15));
        return Math.max(5, Math.round(g / 5.0) * 5);
    }

    /** 수량 범위 [최소, 최대] (목록에 적은 값이 있으면 그 값, 0 이면 난이도로: 쉬운 몹일수록 많이) */
    public static int[] range(Target t) {
        if (t.min > 0 && t.max > 0) return new int[]{t.min, Math.max(t.min, t.max)};
        int max = (int) Math.max(1, Math.min(30, Math.round(24 / difficulty(t.entity))));
        int min = (int) Math.max(1, Math.round(max * 0.5));
        if (t.min > 0) min = t.min;
        if (t.max > 0) max = t.max;
        return new int[]{min, Math.max(min, max)};
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
        // 수량(최소·최대)과 마리당 Gold 를 0 으로 두면 몹의 체력·공격력·방어력으로 난이도를 재서 자동으로 정한다
        h.pools.add(pool("", a("minecraft:zombie"), a("minecraft:skeleton"), a("minecraft:spider"), a("minecraft:creeper"),
                a("minecraft:drowned"), a("minecraft:husk"), a("minecraft:stray"), a("minecraft:witch"), a("minecraft:enderman"),
                a("minecraft:pillager"), a("minecraft:vindicator"), a("minecraft:slime")));
        h.pools.add(pool("story_ch1_done", a("mowziesmobs:foliaath"), a("mowziesmobs:lantern"), a("mowziesmobs:naga"),
                a("mowziesmobs:umvuthana"), a("mowziesmobs:umvuthana_raptor"), a("mowziesmobs:grottol"),
                a("minecraft:wither_skeleton"), a("minecraft:blaze")));
        h.pools.add(pool("story_ch2_done", a("aquamirae:tortured_soul"), a("aquamirae:anglerfish"), a("aquamirae:eel"),
                a("aquamirae:maw"), a("aquamirae:spinefish"), a("aquamirae:abyssal_scyphoid")));
        h.pools.add(pool("story_ch3_done", a("cataclysm:koboleton"), a("cataclysm:the_watcher"), a("cataclysm:deepling"),
                a("cataclysm:deepling_brute"), a("cataclysm:draugr"), a("cataclysm:elite_draugr"), a("cataclysm:endermaptera"),
                a("cataclysm:ignited_revenant"), a("cataclysm:the_prowler")));
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
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                Hunts h = fromJson(o);
                if (!o.has("version") || o.get("version").getAsInt() < VERSION) h.save();
                return h;
            }
            Hunts d = defaults();
            Files.createDirectories(f.getParent());
            Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(d.toJson()), StandardCharsets.UTF_8);
            return d;
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] hunts.json 을 읽지 못했습니다: {}", ex.toString());
            return defaults();
        }
    }

    void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson()), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] hunts.json 을 저장하지 못했습니다: {}", ex.toString());
        }
    }

    JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "토벌 의뢰판(/bounty)과 몹 도감(/bestiary). pools: flag 표식을 가진 사람에게 그 몹들이 의뢰로 나온다 (\"\" = 누구나). "
                + "targets: [몹 ID, 최소, 최대, 마리당 Gold] (0 이면 몹의 체력·공격력·방어력으로 잰 난이도로 자동. 몹 ID 만 적어도 된다). "
                + "게임 안에서는 OP 가 /bounty pool add|remove|list 로 고칠 수 있다. weekly 의뢰는 weeklyMultiplier 배로 많고 비싸다. jobBonus: 전투 직업이 있으면 보상 +비율. "
                + "books: 장별 도감, 들어 있는 몹(설치된 모드만)을 모두 잡으면 gold 를 한 번 준다.");
        o.addProperty("version", VERSION);
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
        // 예전 파일(version 없음)의 목록은 손으로 매긴 보상이라 새 기본 목록(난이도 자동)으로 바꾼다
        if (!o.has("version") || o.get("version").getAsInt() < VERSION) {
            h.pools.addAll(defaults().pools);
        } else if (o.has("pools")) {
            for (JsonElement e : o.getAsJsonArray("pools")) {
                JsonObject x = e.getAsJsonObject();
                Pool p = new Pool();
                p.flag = x.has("flag") ? x.get("flag").getAsString() : "";
                for (JsonElement te : x.getAsJsonArray("targets")) {
                    if (te.isJsonPrimitive()) {
                        p.targets.add(new Target(te.getAsString(), 0, 0, 0));
                        continue;
                    }
                    JsonArray a = te.getAsJsonArray();
                    int mn = a.size() > 1 ? a.get(1).getAsInt() : 0, mx = a.size() > 2 ? a.get(2).getAsInt() : 0;
                    p.targets.add(new Target(a.get(0).getAsString(), mn, mn > 0 ? Math.max(mn, mx) : mx, a.size() > 3 ? a.get(3).getAsLong() : 0));
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
            int[] r = range(t);
            int need = (int) Math.round((r[0] + RNG.nextInt(r[1] - r[0] + 1)) * (weekly ? mult : 1));
            CompoundTag b = new CompoundTag();
            b.putString("entity", t.entity);
            b.putInt("need", Math.max(1, need));
            b.putInt("have", 0);
            b.putLong("gold", Math.round(goldEach(t) * Math.max(1, need) * (weekly ? 1.25 : 1.0)));
            b.putInt("stars", stars(t.entity));
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
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.hunt.new_entry", victim.getType().getDescription(), b.name).withStyle(ChatFormatting.GREEN), true);
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
                com.rtshop.notice.Notice.send(p, Component.translatable(b.getBoolean("weekly") ? "rtshop.hunt.weekly_done" : "rtshop.hunt.daily_done",
                        victim.getType().getDescription(), need, String.format("%,d", gold)).withStyle(ChatFormatting.GOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
            } else {
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.hunt.progress", victim.getType().getDescription(), have, need).withStyle(ChatFormatting.YELLOW), true);
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
            x.addProperty("stars", b.contains("stars") ? b.getInt("stars") : stars(b.getString("entity")));
            x.addProperty("accepted", accepted(b));
            x.addProperty("done", b.getBoolean("done"));
            a.add(x);
        }
        o.add("list", a);
        return o;
    }

    public static void openBoard(ServerPlayer p, int entity) {
        if (!nearBoard(p, entity)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.hunt.board_far").withStyle(ChatFormatting.RED), true);
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
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.hunt.book_done", b.name, String.format("%,d", b.gold)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
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

    // ------------------------------------------------------------------ 목록 고치기 (OP)

    private static String desc(Target t) {
        int[] r = range(t);
        return t.entity + " (" + name(t.entity) + ")  " + "★".repeat(stars(t.entity)) + "  " + r[0] + "~" + r[1]
                + (t.min > 0 ? "" : " auto") + " × " + goldEach(t) + " G" + (t.gold > 0 ? "" : " auto");
    }

    static void poolList(net.minecraft.commands.CommandSourceStack src) {
        for (Pool pl : get().pools) {
            String head = pl.flag.isEmpty() ? "[누구나 / everyone]" : "[" + pl.flag + "]";
            src.sendSuccess(() -> Component.literal(head).withStyle(ChatFormatting.GOLD), false);
            for (Target t : pl.targets) {
                boolean ok = installed(t.entity);
                src.sendSuccess(() -> Component.literal(" - " + desc(t)).withStyle(ok ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY), false);
            }
        }
    }

    /** 누구나 받는 목록(flag "")에 넣는다. 이미 있으면 수량·보상만 바꾼다. */
    public static synchronized int poolAdd(net.minecraft.commands.CommandSourceStack src, String mob, int min, int max, long gold) {
        if (!installed(mob)) {
            if (src != null) src.sendFailure(Component.translatable("rtshop.hunt.pool_unknown", mob));
            return 0;
        }
        Hunts h = get();
        Target found = null;
        for (Pool pl : h.pools) for (Target t : pl.targets) if (t.entity.equals(mob)) found = t;
        if (found == null) {
            Pool open = null;
            for (Pool pl : h.pools) if (pl.flag.isEmpty()) open = pl;
            if (open == null) h.pools.add(0, open = new Pool());
            open.targets.add(found = new Target(mob, 0, 0, 0));
        }
        found.min = min;
        found.max = min > 0 ? Math.max(min, max) : max;
        found.gold = gold;
        if (!testMode) h.save();
        Target f = found;
        if (src != null) src.sendSuccess(() -> Component.translatable("rtshop.hunt.pool_added", desc(f)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    public static synchronized int poolRemove(net.minecraft.commands.CommandSourceStack src, String mob) {
        Hunts h = get();
        boolean any = false;
        for (Pool pl : h.pools) any |= pl.targets.removeIf(t -> t.entity.equals(mob));
        if (any && !testMode) h.save();
        if (src != null) {
            if (any) src.sendSuccess(() -> Component.translatable("rtshop.hunt.pool_removed", mob).withStyle(ChatFormatting.YELLOW), true);
            else src.sendFailure(Component.translatable("rtshop.hunt.pool_missing", mob));
        }
        return any ? 1 : 0;
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
                    if (r == 2) com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.hunt.board_full").withStyle(ChatFormatting.RED), true);
                    if (r == 0) p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 1F);
                    openBoard(p, ent);
                    return r == 0 ? 1 : 0;
                })))));
        // OP: 의뢰로 나오는 몹 목록 고치기 (수량·보상을 비우면 난이도로 자동)
        event.getDispatcher().register(Commands.literal("bounty").then(Commands.literal("pool").requires(src -> src.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> {
                    poolList(ctx.getSource());
                    return 1;
                }))
                .then(Commands.literal("add").then(Commands.argument("mob", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                        .suggests(net.minecraft.commands.synchronization.SuggestionProviders.SUMMONABLE_ENTITIES)
                        .executes(ctx -> poolAdd(ctx.getSource(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "mob").toString(), 0, 0, 0))
                        .then(Commands.argument("min", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 999))
                                .then(Commands.argument("max", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 999))
                                        .executes(ctx -> poolAdd(ctx.getSource(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "mob").toString(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "min"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "max"), 0))
                                        .then(Commands.argument("gold", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                                .executes(ctx -> poolAdd(ctx.getSource(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "mob").toString(),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "min"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "max"),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "gold"))))))))
                .then(Commands.literal("remove").then(Commands.argument("mob", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                get().pools.stream().flatMap(pl -> pl.targets.stream()).map(t -> t.entity), b))
                        .executes(ctx -> poolRemove(ctx.getSource(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "mob").toString()))))
                .then(Commands.literal("reroll").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    data(p).putLong("day", -1);
                    data(p).putLong("week", -1);
                    data(p).put("list", new ListTag());
                    bounties(p);
                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.hunt.pool_rerolled").withStyle(ChatFormatting.GREEN), false);
                    return 1;
                }))));
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
