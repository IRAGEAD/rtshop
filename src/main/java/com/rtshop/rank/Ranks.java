package com.rtshop.rank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import com.rtshop.quest.QuestManager;
import com.rtshop.raid.Weekly;
import com.rtshop.rules.MobRules;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 기록과 칭호 (config/rtshop/ranks.json, 3.53).
 * <ul>
 * <li><b>기록</b>: 몬스터 처치·보스 처치·총 수익·총 소비·보유 Gold·퀘스트 완료·현상금 완료·레이드 완료. 직업과 상관없이 누구나 쌓는 것만 센다.
 *     (수익·소비는 NPC·시스템과의 거래만: 플레이어끼리 주고받는 돈은 세지 않는다.)</li>
 * <li><b>주간 순위</b>: 종목마다 이번 주 기록으로 순위를 매기고, 주가 바뀔 때 1~3위에게 보상을 준다 (기록원 NPC 에게서 받는다).
 *     1위는 한 주 동안 「이번 주의 …」 칭호를 내걸 수 있다.</li>
 * <li><b>칭호</b>: 종목마다 평생 기록이 쌓이면 단계별 칭호가 열린다. 같은 종목의 칭호는 효과가 같고 수치만 다르다.
 *     효과는 2개까지(종목이 서로 달라야 한다) 켜고, 내걸어 보이는 칭호는 1개 (채팅·이름표·탭 목록).</li>
 * </ul>
 * NPC: quest_ranks (순위 기록원), quest_titles (칭호 관리인). 대화 상자에 [순위 보기]·[칭호 고르기] 가 생긴다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Ranks {
    private Ranks() {
    }

    public static final String KILLS = "kills", BOSSES = "bosses", EARNED = "earned", SPENT = "spent", GOLD = "gold",
            QUESTS = "quests", BOUNTIES = "bounties", RAIDS = "raids";
    public static final String[] CATS = {KILLS, BOSSES, EARNED, SPENT, GOLD, QUESTS, BOUNTIES, RAIDS};
    public static final String NPC_RANKS = "quest_ranks", NPC_TITLES = "quest_titles";
    /** 이번 주 1위에게 주는 칭호의 ID 머리말 (week:<종목>). */
    public static final String WEEK = "week:";
    private static final UUID HEALTH_UUID = UUID.fromString("7b1f3a52-91c4-4d0e-a6b7-3c5e8f2d9a41");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // ================================================================== 설정

    public static class TitleDef {
        public String id = "", category = "", name = "", effect = "";
        public long need;
        public double value;

        TitleDef() {
        }

        TitleDef(String id, String category, long need, String name, String effect, double value) {
            this.id = id;
            this.category = category;
            this.need = need;
            this.name = name;
            this.effect = effect;
            this.value = value;
        }
    }

    public static class Config {
        public String _help = "기록·주간 순위·칭호. categories: 종목을 끄려면 false. rewards: 주간 1·2·3위 보상 Gold (종목마다 같다, categoryRewards 로 종목별로 따로). "
                + "resetDay 1=월 … 7=일, resetHour 0~23 (서버 컴퓨터 시간). titles: need = 그 종목의 평생 기록, effect = mob_damage·boss_damage(%) · sell_bonus·buy_discount(%) · "
                + "death_keep(죽을 때 잃는 Gold 감소 %) · quest_gold·bounty_gold(%) · max_health(체력). 직접 고쳤으면 \"edited\": true.";
        public int version = VERSION;
        public boolean edited = false;
        public boolean enabled = true;
        public int resetDay = 1, resetHour = 5;
        public int topShown = 10;
        public int maxEffectTitles = 2;
        /** 스포너에서 나온 몹도 처치로 셀까. */
        public boolean countSpawnerMobs = false;
        public boolean weekTitles = true;
        public Map<String, Boolean> categories = new LinkedHashMap<>();
        public long[] rewards = {20000, 10000, 5000};
        public Map<String, long[]> categoryRewards = new LinkedHashMap<>();
        public List<TitleDef> titles = new ArrayList<>();
    }

    public static final int VERSION = 1;
    private static Config config;

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("ranks.json");
    }

    static List<TitleDef> defaultTitles() {
        List<TitleDef> t = new ArrayList<>();
        // 몬스터 처치: 일반 몬스터에게 주는 피해
        t.add(new TitleDef("kills_1", KILLS, 10, "풋내기 사냥꾼", "mob_damage", 0.5));
        t.add(new TitleDef("kills_2", KILLS, 100, "숙련된 사냥꾼", "mob_damage", 1));
        t.add(new TitleDef("kills_3", KILLS, 1000, "노련한 추적자", "mob_damage", 2));
        t.add(new TitleDef("kills_4", KILLS, 5000, "마물 사냥꾼", "mob_damage", 3));
        t.add(new TitleDef("kills_5", KILLS, 20000, "마물의 천적", "mob_damage", 4));
        // 보스 처치: 보스에게 주는 피해
        t.add(new TitleDef("bosses_1", BOSSES, 1, "첫 토벌", "boss_damage", 1));
        t.add(new TitleDef("bosses_2", BOSSES, 10, "수호자를 재운 자", "boss_damage", 2));
        t.add(new TitleDef("bosses_3", BOSSES, 50, "거수 토벌자", "boss_damage", 3));
        t.add(new TitleDef("bosses_4", BOSSES, 200, "왕들을 재운 손", "boss_damage", 5));
        // 총 수익: NPC 상점에 팔 때 받는 값
        t.add(new TitleDef("earned_1", EARNED, 10_000, "잔돈 모으는 이", "sell_bonus", 0.5));
        t.add(new TitleDef("earned_2", EARNED, 100_000, "수완 좋은 장사꾼", "sell_bonus", 1));
        t.add(new TitleDef("earned_3", EARNED, 1_000_000, "거상", "sell_bonus", 1.5));
        t.add(new TitleDef("earned_4", EARNED, 10_000_000, "황금 손", "sell_bonus", 2));
        // 총 소비: NPC 상점에서 살 때 값
        t.add(new TitleDef("spent_1", SPENT, 10_000, "단골손님", "buy_discount", 0.5));
        t.add(new TitleDef("spent_2", SPENT, 100_000, "큰손", "buy_discount", 1));
        t.add(new TitleDef("spent_3", SPENT, 1_000_000, "귀빈", "buy_discount", 1.5));
        t.add(new TitleDef("spent_4", SPENT, 10_000_000, "상단의 후원자", "buy_discount", 2));
        // 보유 Gold (가장 많이 지녔던 때): 죽을 때 잃는 Gold 가 줄어든다
        t.add(new TitleDef("gold_1", GOLD, 50_000, "알뜰한 저축가", "death_keep", 5));
        t.add(new TitleDef("gold_2", GOLD, 500_000, "금고지기", "death_keep", 10));
        t.add(new TitleDef("gold_3", GOLD, 5_000_000, "대부호", "death_keep", 20));
        // 퀘스트 완료: 퀘스트 보상 Gold
        t.add(new TitleDef("quests_1", QUESTS, 10, "부지런한 심부름꾼", "quest_gold", 1));
        t.add(new TitleDef("quests_2", QUESTS, 50, "믿음직한 조사대원", "quest_gold", 2));
        t.add(new TitleDef("quests_3", QUESTS, 150, "베스탈리아의 해결사", "quest_gold", 3));
        t.add(new TitleDef("quests_4", QUESTS, 300, "연대기의 증인", "quest_gold", 5));
        // 현상금 완료: 현상금 Gold
        t.add(new TitleDef("bounties_1", BOUNTIES, 5, "견습 현상금 사냥꾼", "bounty_gold", 2));
        t.add(new TitleDef("bounties_2", BOUNTIES, 25, "현상금 사냥꾼", "bounty_gold", 4));
        t.add(new TitleDef("bounties_3", BOUNTIES, 100, "이름난 추적자", "bounty_gold", 6));
        // 레이드 완료: 최대 체력
        t.add(new TitleDef("raids_1", RAIDS, 1, "첫 원정", "max_health", 1));
        t.add(new TitleDef("raids_2", RAIDS, 10, "노련한 원정대원", "max_health", 2));
        t.add(new TitleDef("raids_3", RAIDS, 50, "원정대의 기둥", "max_health", 4));
        return t;
    }

    public static synchronized Config get() {
        if (config == null) {
            Config c = null;
            Path f = file();
            try {
                if (Files.exists(f)) c = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Config.class);
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] ranks.json 을 읽지 못해 기본값을 씁니다: {}", e.toString());
            }
            boolean write = c == null;
            if (c == null) c = new Config();
            if (c.categories == null) c.categories = new LinkedHashMap<>();
            for (String cat : CATS) if (!c.categories.containsKey(cat)) { c.categories.put(cat, true); write = true; }
            if (c.rewards == null) c.rewards = new long[]{20000, 10000, 5000};
            if (c.categoryRewards == null) c.categoryRewards = new LinkedHashMap<>();
            if (c.titles == null || c.titles.isEmpty() || (!c.edited && c.version < VERSION)) {
                c.titles = defaultTitles();
                c.version = VERSION;
                write = true;
            }
            c.titles.removeIf(t -> t == null || t.id == null || t.id.isEmpty() || t.category == null);
            c.resetDay = Math.max(1, Math.min(7, c.resetDay));
            c.resetHour = Math.max(0, Math.min(23, c.resetHour));
            c.maxEffectTitles = Math.max(0, Math.min(8, c.maxEffectTitles));
            c.topShown = Math.max(3, Math.min(50, c.topShown));
            if (write) {
                try {
                    Files.createDirectories(f.getParent());
                    Files.writeString(f, GSON.toJson(c), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    RtShop.LOGGER.warn("[rtshop] ranks.json 을 쓰지 못했습니다: {}", e.toString());
                }
            }
            config = c;
        }
        return config;
    }

    public static synchronized void reload() {
        config = null;
    }

    public static boolean on(String cat) {
        Config c = get();
        return c.enabled && Boolean.TRUE.equals(c.categories.get(cat));
    }

    public static TitleDef title(String id) {
        for (TitleDef t : get().titles) if (t.id.equals(id)) return t;
        return null;
    }

    static long[] rewards(String cat) {
        Config c = get();
        long[] r = c.categoryRewards.get(cat);
        return r != null ? r : c.rewards;
    }

    // ================================================================== 저장

    public static class Rec {
        public String name = "";
        public final Map<String, Long> life = new LinkedHashMap<>(), week = new LinkedHashMap<>();
        /** 마지막으로 본 보유 Gold (지갑 + 은행). 접속해 있지 않을 때의 순위에 쓴다. */
        public long goldSeen;
        public final List<String> effects = new ArrayList<>();
        public String show = "";
        public long pendingGold;
        public final List<String> pendingNotes = new ArrayList<>();
        /** 지난주에 1위를 한 종목들 (이번 주 동안 「이번 주의 …」 칭호를 쓸 수 있다). */
        public final List<String> weekWon = new ArrayList<>();

        long life(String cat) {
            return life.getOrDefault(cat, 0L);
        }

        long week(String cat) {
            return GOLD.equals(cat) ? goldSeen : week.getOrDefault(cat, 0L);
        }
    }

    public record Row(UUID id, String name, long value) {
    }

    public static class Data extends SavedData {
        public final Map<UUID, Rec> recs = new LinkedHashMap<>();
        long weekId = -1;
        /** 지난주 결과: 종목 → 순위대로. */
        public final Map<String, List<Row>> last = new LinkedHashMap<>();

        Rec of(UUID id) {
            return recs.computeIfAbsent(id, k -> new Rec());
        }

        static Data load(CompoundTag t) {
            Data d = new Data();
            d.weekId = t.contains("week") ? t.getLong("week") : -1;
            ListTag ps = t.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < ps.size(); i++) {
                CompoundTag c = ps.getCompound(i);
                Rec r = new Rec();
                r.name = c.getString("name");
                CompoundTag l = c.getCompound("life"), w = c.getCompound("weekly");
                for (String k : l.getAllKeys()) r.life.put(k, l.getLong(k));
                for (String k : w.getAllKeys()) r.week.put(k, w.getLong(k));
                r.goldSeen = c.getLong("gold");
                for (Tag e : c.getList("effects", Tag.TAG_STRING)) r.effects.add(e.getAsString());
                r.show = c.getString("show");
                r.pendingGold = c.getLong("pending");
                for (Tag e : c.getList("notes", Tag.TAG_STRING)) r.pendingNotes.add(e.getAsString());
                for (Tag e : c.getList("won", Tag.TAG_STRING)) r.weekWon.add(e.getAsString());
                d.recs.put(c.getUUID("id"), r);
            }
            CompoundTag last = t.getCompound("last");
            for (String cat : last.getAllKeys()) {
                List<Row> rows = new ArrayList<>();
                ListTag lt = last.getList(cat, Tag.TAG_COMPOUND);
                for (int i = 0; i < lt.size(); i++) {
                    CompoundTag c = lt.getCompound(i);
                    rows.add(new Row(c.getUUID("id"), c.getString("name"), c.getLong("v")));
                }
                d.last.put(cat, rows);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            t.putLong("week", weekId);
            ListTag ps = new ListTag();
            for (Map.Entry<UUID, Rec> e : recs.entrySet()) {
                Rec r = e.getValue();
                CompoundTag c = new CompoundTag();
                c.putUUID("id", e.getKey());
                c.putString("name", r.name);
                CompoundTag l = new CompoundTag(), w = new CompoundTag();
                r.life.forEach(l::putLong);
                r.week.forEach(w::putLong);
                c.put("life", l);
                c.put("weekly", w);
                c.putLong("gold", r.goldSeen);
                ListTag ef = new ListTag(), notes = new ListTag(), won = new ListTag();
                for (String s : r.effects) ef.add(StringTag.valueOf(s));
                for (String s : r.pendingNotes) notes.add(StringTag.valueOf(s));
                for (String s : r.weekWon) won.add(StringTag.valueOf(s));
                c.put("effects", ef);
                c.putString("show", r.show);
                c.putLong("pending", r.pendingGold);
                c.put("notes", notes);
                c.put("won", won);
                ps.add(c);
            }
            t.put("players", ps);
            CompoundTag lastTag = new CompoundTag();
            for (Map.Entry<String, List<Row>> e : last.entrySet()) {
                ListTag lt = new ListTag();
                for (Row r : e.getValue()) {
                    CompoundTag c = new CompoundTag();
                    c.putUUID("id", r.id);
                    c.putString("name", r.name);
                    c.putLong("v", r.value);
                    lt.add(c);
                }
                lastTag.put(e.getKey(), lt);
            }
            t.put("last", lastTag);
            return t;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rtshop_ranks");
    }

    public static Rec rec(ServerPlayer p) {
        Rec r = data(p.server).of(p.getUUID());
        r.name = p.getGameProfile().getName();
        return r;
    }

    /** 시험용: 가짜 플레이어도 센다 (평소에는 다른 모드의 기계가 쓰는 가짜 플레이어를 세지 않는다). */
    public static volatile boolean testFakes;

    private static boolean real(ServerPlayer p) {
        return p != null && p.server != null && (testFakes || !(p instanceof FakePlayer));
    }

    // ================================================================== 기록하기

    /** 종목의 기록을 n 만큼 올린다 (평생 + 이번 주). 새 칭호가 열리면 알린다. */
    public static void add(ServerPlayer p, String cat, long n) {
        if (n <= 0 || !real(p) || !on(cat)) return;
        Rec r = rec(p);
        long before = r.life(cat);
        r.life.put(cat, before + n);
        r.week.merge(cat, n, Long::sum);
        data(p.server).setDirty();
        unlocked(p, cat, before, before + n);
    }

    /** 보유 Gold 를 다시 본다 (평생 기록은 가장 많이 지녔던 때). */
    public static void touchGold(ServerPlayer p) {
        if (!real(p) || !get().enabled) return;
        Rec r = rec(p);
        long v = Economy.wallet(p) + Economy.bank(p);
        if (v == r.goldSeen) return;
        r.goldSeen = v;
        long before = r.life(GOLD);
        if (v > before && on(GOLD)) {
            r.life.put(GOLD, v);
            unlocked(p, GOLD, before, v);
        }
        data(p.server).setDirty();
    }

    private static void unlocked(ServerPlayer p, String cat, long before, long after) {
        for (TitleDef t : get().titles) {
            if (!t.category.equals(cat) || before >= t.need || after < t.need) continue;
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.title.unlocked", nameOf(t.id)).withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    /** NPC·시스템에게서 번 Gold. (플레이어 사이의 거래·은행 입출금·환불은 세지 않는다.) */
    public static void earned(ServerPlayer p, long gold) {
        add(p, EARNED, gold);
    }

    /** NPC·시스템에 쓴 Gold. */
    public static void spent(ServerPlayer p, long gold) {
        add(p, SPENT, gold);
    }

    /** 처치로 인정된 사람마다 한 번 (QuestManager.onKill: 퀘스트·현상금과 같은 기준). */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        if (!real(p) || !(victim instanceof Enemy)) return;
        if (MobRules.boss(victim)) {
            add(p, BOSSES, 1);
            return;
        }
        if (!get().countSpawnerMobs && victim instanceof Mob m && m.getSpawnType() == MobSpawnType.SPAWNER) return;
        add(p, KILLS, 1);
    }

    // ================================================================== 칭호

    public static boolean has(Rec r, String titleId) {
        if (titleId == null || titleId.isEmpty()) return false;
        if (titleId.startsWith(WEEK)) return get().weekTitles && r.weekWon.contains(titleId.substring(WEEK.length()));
        TitleDef t = title(titleId);
        return t != null && on(t.category) && r.life(t.category) >= t.need;
    }

    /** 칭호의 이름 (기본 칭호는 언어 파일에서, 직접 만든 칭호는 설정의 name). */
    public static MutableComponent nameOf(String titleId) {
        if (titleId.startsWith(WEEK)) return Component.translatable("rtshop.title.week." + titleId.substring(WEEK.length()));
        TitleDef t = title(titleId);
        return Component.translatableWithFallback("rtshop.title.name." + titleId, t == null ? titleId : t.name);
    }

    /** 켜 둔 칭호들의 효과 합 (같은 효과는 가장 큰 것 하나만). */
    public static double effect(ServerPlayer p, String effect) {
        if (!real(p) || !get().enabled) return 0;
        Rec r = data(p.server).recs.get(p.getUUID());
        if (r == null || r.effects.isEmpty()) return 0;
        double best = 0;
        for (String id : r.effects) {
            TitleDef t = title(id);
            if (t != null && t.effect.equals(effect) && has(r, id)) best = Math.max(best, t.value);
        }
        return best;
    }

    /** 효과 칭호를 켜거나 끈다. 안 되면 까닭(번역 키), 되면 null. */
    public static String setEffect(ServerPlayer p, String id, boolean on) {
        Rec r = rec(p);
        if (!on) {
            if (!r.effects.remove(id)) return "rtshop.title.err.not_on";
        } else {
            TitleDef t = title(id);
            if (t == null || !has(r, id)) return "rtshop.title.err.locked";
            if (r.effects.contains(id)) return "rtshop.title.err.already";
            // 같은 종목의 다른 단계를 켜 두었으면 바꿔 끼운다
            r.effects.removeIf(e -> {
                TitleDef o = title(e);
                return o == null || o.category.equals(t.category);
            });
            if (r.effects.size() >= get().maxEffectTitles) return "rtshop.title.err.full";
            r.effects.add(id);
        }
        data(p.server).setDirty();
        applyHealth(p);
        return null;
    }

    /** 내걸어 보일 칭호를 고른다 (빈 문자열 = 내걸지 않음). */
    public static String setShow(ServerPlayer p, String id) {
        Rec r = rec(p);
        if (id == null || id.isEmpty() || id.equals("none")) {
            r.show = "";
        } else {
            if (!has(r, id)) return "rtshop.title.err.locked";
            r.show = id;
        }
        data(p.server).setDirty();
        refreshName(p);
        return null;
    }

    static void refreshName(ServerPlayer p) {
        try {
            p.refreshDisplayName();
            p.refreshTabListName();
        } catch (Throwable ignored) {
        }
    }

    /** 최대 체력 효과를 몸에 맞춘다. */
    public static void applyHealth(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) return;
        double v = effect(p, "max_health");
        AttributeModifier old = a.getModifier(HEALTH_UUID);
        if (old != null && old.getAmount() == v) return;
        if (old != null) a.removeModifier(HEALTH_UUID);
        if (v > 0) a.addPermanentModifier(new AttributeModifier(HEALTH_UUID, "rtshop title", v, AttributeModifier.Operation.ADDITION));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static Component shownTitle(ServerPlayer p) {
        if (!real(p) || !get().enabled) return null;
        Rec r = data(p.server).recs.get(p.getUUID());
        if (r == null || r.show.isEmpty() || !has(r, r.show)) return null;
        boolean week = r.show.startsWith(WEEK);
        return Component.literal("[").append(nameOf(r.show)).append("] ").withStyle(week ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
    }

    @SubscribeEvent
    public static void onName(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Component t = shownTitle(p);
        if (t != null) event.setDisplayname(Component.empty().append(t).append(event.getDisplayname()));
    }

    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Component t = shownTitle(p);
        if (t == null) return;
        Component base = event.getDisplayName() != null ? event.getDisplayName() : p.getName();
        event.setDisplayName(Component.empty().append(t).append(base));
    }

    /** 칭호 효과: 몬스터·보스에게 주는 피해. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        Entity src = event.getSource().getEntity();
        if (!(src instanceof ServerPlayer p) || !(event.getEntity() instanceof Enemy)) return;
        double pct = effect(p, MobRules.boss(event.getEntity()) ? "boss_damage" : "mob_damage");
        if (pct > 0) event.setAmount((float) (event.getAmount() * (1.0 + pct / 100.0)));
    }

    /** value 에 칭호 효과(%)를 더하거나 뺀 값. */
    public static long plus(ServerPlayer p, String effect, long value) {
        double pct = effect(p, effect);
        return pct <= 0 || value <= 0 ? value : Math.round(value * (1.0 + pct / 100.0));
    }

    public static long minus(ServerPlayer p, String effect, long value) {
        double pct = effect(p, effect);
        return pct <= 0 || value <= 0 ? value : Math.max(0, Math.round(value * (1.0 - Math.min(100, pct) / 100.0)));
    }

    // ================================================================== 주간 순위

    static long weekNow() {
        Config c = get();
        return Weekly.id(Weekly.now(), c.resetDay, c.resetHour);
    }

    /** 이번 주 순위 (기록이 0 인 사람은 빠진다). */
    public static List<Row> ranking(MinecraftServer server, String cat) {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<UUID, Rec> e : data(server).recs.entrySet()) {
            long v = e.getValue().week(cat);
            if (v > 0) rows.add(new Row(e.getKey(), e.getValue().name, v));
        }
        rows.sort(Comparator.comparingLong(Row::value).reversed().thenComparing(Row::name));
        return rows;
    }

    /** 주가 바뀌었으면 지난주를 마감한다. */
    static void checkWeek(MinecraftServer server) {
        if (!get().enabled) return;
        Data d = data(server);
        long now = weekNow();
        if (d.weekId == now) return;
        boolean first = d.weekId < 0;
        d.weekId = now;
        d.setDirty();
        if (!first) settle(server);
    }

    /** 지난주 마감: 순위를 적고, 1~3위에게 보상을 맡겨 두고, 이번 주 기록을 비운다. */
    public static void settle(MinecraftServer server) {
        Config c = get();
        Data d = data(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) touchGold(p);
        d.last.clear();
        for (Rec r : d.recs.values()) r.weekWon.clear();
        List<String> lines = new ArrayList<>();
        for (String cat : CATS) {
            if (!on(cat)) continue;
            List<Row> rows = ranking(server, cat);
            d.last.put(cat, new ArrayList<>(rows.subList(0, Math.min(rows.size(), c.topShown))));
            long[] rw = rewards(cat);
            for (int i = 0; i < rows.size() && i < rw.length; i++) {
                Rec r = d.of(rows.get(i).id);
                if (rw[i] > 0) {
                    r.pendingGold += rw[i];
                    r.pendingNotes.add(cat + ":" + (i + 1) + ":" + rw[i]);
                    while (r.pendingNotes.size() > 40) r.pendingNotes.remove(0);
                }
                if (i == 0) r.weekWon.add(cat);
            }
            if (!rows.isEmpty()) lines.add(cat + ":" + rows.get(0).name);
        }
        for (Rec r : d.recs.values()) {
            r.week.clear();
            if (r.show.startsWith(WEEK) && !has(r, r.show)) r.show = "";
        }
        d.setDirty();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            refreshName(p);
            if (lines.isEmpty()) continue;
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rank.week_over").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (String l : lines) {
                String[] s = l.split(":", 2);
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rank.week_first", Component.translatable("rtshop.rank.cat." + s[0]), s[1]).withStyle(ChatFormatting.YELLOW));
            }
            remind(p);
        }
    }

    static void remind(ServerPlayer p) {
        Rec r = data(p.server).recs.get(p.getUUID());
        if (r != null && r.pendingGold > 0) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rank.pending", String.format("%,d", r.pendingGold)).withStyle(ChatFormatting.GREEN));
        }
    }

    /** 맡겨 둔 주간 보상을 받는다. 받은 Gold. */
    public static long claim(ServerPlayer p) {
        Rec r = rec(p);
        long g = r.pendingGold;
        if (g <= 0) return 0;
        r.pendingGold = 0;
        r.pendingNotes.clear();
        data(p.server).setDirty();
        Economy.addWallet(p, g);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rank.claimed", String.format("%,d", g)).withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.0F);
        return g;
    }

    // ================================================================== 사건

    private static int tick;

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++tick % 200 != 0) return;
        MinecraftServer server = event.getServer();
        try {
            if (tick % 1200 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) touchGold(p);
            checkWeek(server);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 주간 순위 점검 실패: {}", t.toString());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !real(p) || !get().enabled) return;
        rec(p);
        touchGold(p);
        applyHealth(p);
        refreshName(p);
        remind(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) touchGold(p);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer p && real(p)) applyHealth(p);
    }

    // ================================================================== 화면

    /** 시험용: NPC 곁이 아니어도 화면을 열고 고를 수 있다. */
    public static volatile boolean testAnywhere;

    private static boolean near(ServerPlayer p, String npcKey) {
        if (testAnywhere || com.rtshop.util.Perm.op(p)) return true;
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(8))) {
            if (npcKey.equals(QuestManager.npcKey(e))) return true;
        }
        return false;
    }

    public static JsonObject ranksView(ServerPlayer p) {
        Config c = get();
        Data d = data(p.server);
        touchGold(p);
        Rec me = rec(p);
        JsonObject out = new JsonObject();
        out.addProperty("resetIn", Weekly.untilReset(Weekly.now(), c.resetDay, c.resetHour) / 1000L);
        out.addProperty("pending", me.pendingGold);
        JsonArray cats = new JsonArray();
        for (String cat : CATS) {
            if (!on(cat)) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", cat);
            List<Row> rows = ranking(p.server, cat);
            JsonArray top = new JsonArray();
            int myRank = 0;
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                if (r.id.equals(p.getUUID())) myRank = i + 1;
                if (i < c.topShown) {
                    JsonObject j = new JsonObject();
                    j.addProperty("name", r.name);
                    j.addProperty("value", r.value);
                    j.addProperty("me", r.id.equals(p.getUUID()));
                    top.add(j);
                }
            }
            o.add("top", top);
            o.addProperty("myRank", myRank);
            o.addProperty("myValue", me.week(cat));
            o.addProperty("players", rows.size());
            JsonArray last = new JsonArray();
            for (Row r : d.last.getOrDefault(cat, List.of())) {
                JsonObject j = new JsonObject();
                j.addProperty("name", r.name);
                j.addProperty("value", r.value);
                j.addProperty("me", r.id.equals(p.getUUID()));
                last.add(j);
            }
            o.add("last", last);
            JsonArray rw = new JsonArray();
            for (long g : rewards(cat)) rw.add(g);
            o.add("rewards", rw);
            cats.add(o);
        }
        out.add("cats", cats);
        return out;
    }

    public static JsonObject titlesView(ServerPlayer p) {
        Config c = get();
        touchGold(p);
        Rec me = rec(p);
        me.effects.removeIf(id -> !has(me, id));
        JsonObject out = new JsonObject();
        out.addProperty("max", c.maxEffectTitles);
        JsonArray ef = new JsonArray();
        me.effects.forEach(ef::add);
        out.add("effects", ef);
        out.addProperty("show", has(me, me.show) ? me.show : "");
        JsonArray won = new JsonArray();
        if (c.weekTitles) me.weekWon.forEach(won::add);
        out.add("week", won);
        JsonArray cats = new JsonArray();
        for (String cat : CATS) {
            if (!on(cat)) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", cat);
            o.addProperty("value", me.life(cat));
            JsonArray ts = new JsonArray();
            List<TitleDef> defs = new ArrayList<>();
            for (TitleDef t : c.titles) if (t.category.equals(cat)) defs.add(t);
            defs.sort(Comparator.comparingLong(t -> t.need));
            for (TitleDef t : defs) {
                JsonObject j = new JsonObject();
                j.addProperty("id", t.id);
                j.addProperty("name", t.name);
                j.addProperty("need", t.need);
                j.addProperty("effect", t.effect);
                j.addProperty("value", t.value);
                j.addProperty("open", me.life(cat) >= t.need);
                ts.add(j);
            }
            o.add("titles", ts);
            cats.add(o);
        }
        out.add("cats", cats);
        return out;
    }

    public static void openRanks(ServerPlayer p) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.RANKS, ranksView(p).toString()));
    }

    public static void openTitles(ServerPlayer p) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.TITLES, titlesView(p).toString()));
    }

    private static int fail(ServerPlayer p, String key) {
        com.rtshop.notice.Notice.send(p, Component.translatable(key).withStyle(ChatFormatting.RED));
        return 0;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop")
                .then(Commands.literal("rank")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (!get().enabled) return fail(p, "rtshop.rank.off");
                            if (!near(p, NPC_RANKS)) return fail(p, "rtshop.rank.need_npc");
                            openRanks(p);
                            return 1;
                        })
                        .then(Commands.literal("claim").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (!near(p, NPC_RANKS)) return fail(p, "rtshop.rank.need_npc");
                            if (claim(p) <= 0) fail(p, "rtshop.rank.nothing");
                            openRanks(p);
                            return 1;
                        }))
                        .then(Commands.literal("settle").requires(s -> s.hasPermission(2)).executes(ctx -> {
                            settle(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.rank.settled"), true);
                            return 1;
                        })))
                .then(Commands.literal("title")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (!get().enabled) return fail(p, "rtshop.rank.off");
                            if (!near(p, NPC_TITLES)) return fail(p, "rtshop.title.need_npc");
                            openTitles(p);
                            return 1;
                        })
                        .then(Commands.literal("effect").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (!near(p, NPC_TITLES)) return fail(p, "rtshop.title.need_npc");
                            String id = StringArgumentType.getString(ctx, "id");
                            boolean on = !rec(p).effects.contains(id);
                            String err = setEffect(p, id, on);
                            if (err != null) fail(p, err);
                            openTitles(p);
                            return err == null ? 1 : 0;
                        })))
                        .then(Commands.literal("show").then(Commands.argument("id", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (!near(p, NPC_TITLES)) return fail(p, "rtshop.title.need_npc");
                            String id = StringArgumentType.getString(ctx, "id").trim();
                            String err = setShow(p, id.equals(rec(p).show) ? "" : id);
                            if (err != null) fail(p, err);
                            openTitles(p);
                            return err == null ? 1 : 0;
                        })))));
    }

    // ================================================================== 시험용

    public static void resetForTest(ServerPlayer p) {
        Data d = data(p.server);
        d.recs.remove(p.getUUID());
        d.setDirty();
        applyHealth(p);
        refreshName(p);
    }

    public static void setWeekForTest(MinecraftServer server, long id) {
        data(server).weekId = id;
    }
}
