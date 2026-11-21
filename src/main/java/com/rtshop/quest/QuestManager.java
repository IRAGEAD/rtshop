package com.rtshop.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.net.QuestActionPacket;
import com.rtshop.net.QuestOpenPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 퀘스트 엔진: 정의 읽기, 수락/진행/완료/보상, NPC 대화, 표식(flag).
 * 직업 시스템과 숨겨진 퀘스트는 표식(flag)으로 이 엔진에 연결한다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class QuestManager {
    private QuestManager() {
    }

    /** 엔티티 태그: rtshop_quest_<NPC ID> = 퀘스트 NPC */
    public static final String QUEST_TAG_PREFIX = "rtshop_quest_";
    /**
     * 모드에 들어 있는 기본 퀘스트 파일. 한 번 설치한 파일은 다시는 덮어쓰지 않는다 (지워도 다시 깔지 않는다).
     * 새 기본 퀘스트는 항상 새 파일 이름으로 추가한다.
     */
    private static final String[] DEFAULT_FILES = {"npcs.json", "tutorial.json", "job_quests.json"};
    /**
     * 예전 버전의 기본 파일 내용(SHA-256). 설치된 파일이 이것과 '완전히 같으면' 아무도 고치지 않은 것이므로 새 기본 파일로 바꾼다.
     * 한 글자라도 고쳤으면 그대로 둔다.
     */
    private static final Map<String, Set<String>> OLD_DEFAULTS = Map.of(
            "job_quests.json", Set.of(
                    "9a51ce61d48d98107461017071f42f94276d31641b0a88349b1b88387dc8b540", // 3.6.0
                    "1e9b133e53648071971c057461ddfa6cf659694ae328164b0b68ed2f22655c52")); // 3.6.1

    /** 편집 화면에서 새로 만든 퀘스트·NPC 가 들어가는 파일. */
    public static final String CUSTOM_FILE = "my_quests.json";
    private static final String INSTALLED_LIST = ".installed_defaults";
    /** 동시에 진행할 수 있는 퀘스트 수. */
    public static final int MAX_ACTIVE = 10;

    public static final class NpcDef {
        public String id;
        public String name;
        public String profession = "librarian";
        public String greeting = "";
    }

    public static final class Loaded {
        public final Map<String, QuestDef> quests = new LinkedHashMap<>();
        public final Map<String, NpcDef> npcs = new LinkedHashMap<>();
        public final List<String> problems = new ArrayList<>();
        /** 퀘스트 ID -> 정의된 파일 이름 */
        public final Map<String, String> questFile = new HashMap<>();
        /** NPC ID -> 정의된 파일 이름 */
        public final Map<String, String> npcFile = new HashMap<>();
        /** 문제 때문에 빠진 퀘스트 ID -> 이유 */
        public final Map<String, String> dropped = new LinkedHashMap<>();
    }

    private record Session(String npcKey, int entityId) {
    }

    private static volatile Loaded loaded = new Loaded();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static Loaded loaded() {
        return loaded;
    }

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("quests");
    }

    // ------------------------------------------------------------------ 불러오기

    public static synchronized Loaded reload() {
        SESSIONS.clear();
        Loaded l = new Loaded();
        try {
            installDefaults();
        } catch (IOException e) {
            RtShop.LOGGER.error("[rtshop] 기본 퀘스트 파일을 설치하지 못했습니다", e);
            l.problems.add("기본 퀘스트 파일 설치 실패: " + e);
        }
        List<QuestDef> all = new ArrayList<>();
        Map<QuestDef, String> fileOf = new HashMap<>();
        try (Stream<Path> files = Files.list(dir())) {
            List<Path> list = files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList();
            for (Path f : list) {
                readFile(f, l, all, fileOf);
            }
        } catch (IOException e) {
            RtShop.LOGGER.error("[rtshop] 퀘스트 폴더를 읽지 못했습니다", e);
            l.problems.add("퀘스트 폴더 읽기 실패: " + e);
        }
        all.sort(Comparator.comparingInt((QuestDef q) -> q.order).thenComparing(q -> q.id));
        for (QuestDef q : all) {
            if (l.quests.containsKey(q.id)) {
                l.problems.add("퀘스트 ID 가 겹칩니다: " + q.id + " (뒤의 것은 무시)");
                continue;
            }
            l.quests.put(q.id, q);
            l.questFile.put(q.id, fileOf.get(q));
        }
        validate(l);
        for (String p : l.problems) {
            RtShop.LOGGER.warn("[rtshop] 퀘스트: {}", p);
        }
        RtShop.LOGGER.info("[rtshop] 퀘스트 {}개, 퀘스트 NPC {}명을 불러왔습니다.", l.quests.size(), l.npcs.size());
        loaded = l;
        return l;
    }

    private static void readFile(Path f, Loaded l, List<QuestDef> out, Map<QuestDef, String> files) {
        String name = f.getFileName().toString();
        JsonObject root;
        try {
            root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            l.problems.add(name + ": 읽을 수 없습니다 (" + e.getMessage() + ")");
            return;
        }
        if (root.has("npcs") && root.get("npcs").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("npcs")) {
                try {
                    JsonObject o = e.getAsJsonObject();
                    NpcDef n = new NpcDef();
                    n.id = o.get("id").getAsString().trim();
                    if (!n.id.matches("[a-z0-9_.-]+")) {
                        throw new IllegalArgumentException("NPC id 형식이 틀렸습니다: " + n.id);
                    }
                    n.name = o.has("name") ? o.get("name").getAsString() : n.id;
                    if (o.has("profession")) {
                        n.profession = o.get("profession").getAsString();
                    }
                    if (o.has("greeting")) {
                        n.greeting = o.get("greeting").getAsString();
                    }
                    l.npcs.put(n.id, n);
                    l.npcFile.put(n.id, name);
                } catch (Exception ex) {
                    l.problems.add(name + ": NPC 정의가 잘못되었습니다 (" + ex.getMessage() + ")");
                }
            }
        }
        if (root.has("quests") && root.get("quests").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("quests")) {
                try {
                    QuestDef q = QuestDef.parse(e.getAsJsonObject());
                    applyModConditions(q);
                    out.add(q);
                    files.put(q, name);
                } catch (Exception ex) {
                    l.problems.add(name + ": 퀘스트 정의가 잘못되었습니다 (" + ex.getMessage() + ")");
                }
            }
        }
    }

    /** 모드 조건(mod)이 맞지 않는 목표와 보상 아이템을 뺀다 (예: Epic Fight 가 없으면 Epic Fight 무기 목표 대신 철검 목표만 남는다). */
    public static void applyModConditions(QuestDef q) {
        q.objectives.removeIf(ob -> !QuestDef.modOk(ob.mod));
        q.rewardItems.removeIf(ir -> !QuestDef.modOk(ir.mod));
    }

    /** 참조 오류(없는 선행 퀘스트, 순환)를 찾아 문제 목록에 넣고, 못 쓰는 퀘스트는 뺀다. */
    private static void validate(Loaded l) {
        Set<String> bad = new HashSet<>();
        for (QuestDef q : l.quests.values()) {
            for (String r : q.requires) {
                if (!l.quests.containsKey(r)) {
                    l.problems.add("퀘스트 '" + q.id + "' 의 선행 퀘스트 '" + r + "' 가 없습니다 (이 퀘스트는 쓰지 않습니다)");
                    l.dropped.put(q.id, "선행 퀘스트 '" + r + "' 가 없습니다");
                    bad.add(q.id);
                }
            }
            if (q.requires.contains(q.id)) {
                l.problems.add("퀘스트 '" + q.id + "' 가 자기 자신을 선행 조건으로 가집니다");
                l.dropped.put(q.id, "자기 자신을 선행 퀘스트로 넣었습니다");
                bad.add(q.id);
            }
            if (!q.choices.isEmpty() && q.choices.size() > 6) {
                l.problems.add("퀘스트 '" + q.id + "' 의 선택지가 너무 많습니다 (최대 6개)");
                l.dropped.put(q.id, "선택지는 6개까지입니다");
                bad.add(q.id);
            }
            for (QuestDef.ItemReward ir : q.rewardItems) {
                if (ForgeRegistries.ITEMS.getValue(new ResourceLocation(ir.item)) == null
                        || ForgeRegistries.ITEMS.getValue(new ResourceLocation(ir.item)) == net.minecraft.world.item.Items.AIR) {
                    l.problems.add("퀘스트 '" + q.id + "' 의 보상 아이템 '" + ir.item + "' 이 이 모드팩에 없습니다 (이 보상은 건너뜁니다)");
                }
            }
        }
        for (QuestDef q : l.quests.values()) {
            if (!bad.contains(q.id) && cyclic(l, q.id, new HashSet<>())) {
                l.problems.add("퀘스트 '" + q.id + "' 의 선행 조건이 순환합니다 (이 퀘스트는 쓰지 않습니다)");
                l.dropped.put(q.id, "선행 퀘스트가 돌고 돌아 자기 자신으로 이어집니다");
                bad.add(q.id);
            }
        }
        for (String id : bad) {
            l.quests.remove(id);
        }
    }

    private static boolean cyclic(Loaded l, String id, Set<String> path) {
        if (!path.add(id)) {
            return true;
        }
        QuestDef q = l.quests.get(id);
        if (q != null) {
            for (String r : q.requires) {
                if (cyclic(l, r, path)) {
                    return true;
                }
            }
        }
        path.remove(id);
        return false;
    }

    /**
     * 아직 설치한 적 없는 기본 파일만 깐다. 이미 있는 파일은 절대 건드리지 않는다
     * (편집 화면이나 메모장으로 고친 내용이 업데이트 때 사라지지 않게).
     */
    private static void installDefaults() throws IOException {
        Path d = dir();
        Files.createDirectories(d);
        Path list = d.resolve(INSTALLED_LIST);
        Set<String> installed = new java.util.LinkedHashSet<>();
        if (Files.exists(list)) {
            for (String line : Files.readAllLines(list, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    installed.add(line.trim());
                }
            }
        }
        boolean changed = false;
        for (String name : DEFAULT_FILES) {
            if (installed.contains(name)) {
                continue;
            }
            Path f = d.resolve(name);
            if (!Files.exists(f)) {
                try (InputStream in = QuestManager.class.getResourceAsStream("/rtshop_defaults/quests/" + name)) {
                    if (in == null) {
                        throw new IOException("기본 퀘스트 파일이 모드 안에 없습니다: " + name);
                    }
                    Files.write(f, in.readAllBytes());
                }
                RtShop.LOGGER.info("[rtshop] 기본 퀘스트 파일 {} 을 설치했습니다.", name);
            }
            installed.add(name);
            changed = true;
        }
        if (changed) {
            Files.write(list, installed, StandardCharsets.UTF_8);
        }
        for (Map.Entry<String, Set<String>> e : OLD_DEFAULTS.entrySet()) {
            Path f = d.resolve(e.getKey());
            if (!Files.exists(f) || !e.getValue().contains(sha256(Files.readAllBytes(f)))) {
                continue;
            }
            Path backup = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("backup")
                    .resolve("quests_" + System.currentTimeMillis()).resolve(e.getKey());
            Files.createDirectories(backup.getParent());
            Files.copy(f, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try (InputStream in = QuestManager.class.getResourceAsStream("/rtshop_defaults/quests/" + e.getKey())) {
                if (in != null) {
                    Files.write(f, in.readAllBytes());
                    RtShop.LOGGER.info("[rtshop] 고치지 않은 예전 기본 퀘스트 파일 {} 을 새 버전으로 바꿨습니다 (이전 파일: {}).", e.getKey(), backup);
                }
            }
        }
        Files.deleteIfExists(d.resolve(".defaults_version")); // 3.1.0 의 옛 표시 파일
    }

    public static String sha256(byte[] data) {
        try {
            byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : h) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ------------------------------------------------------------------ NPC

    /** 엔티티의 태그에서 NPC 키를 만든다: bank / shop_<id> / quest_<id>. NPC 가 아니면 null. */
    public static String npcKey(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.equals(ShopServer.BANK_TAG)) {
                return "bank";
            }
            if (tag.startsWith(ShopServer.SHOP_TAG_PREFIX)) {
                return "shop_" + tag.substring(ShopServer.SHOP_TAG_PREFIX.length());
            }
            if (tag.startsWith(QUEST_TAG_PREFIX)) {
                return "quest_" + tag.substring(QUEST_TAG_PREFIX.length());
            }
            if (tag.equals(com.rtshop.job.Jobs.NPC_TAG)) {
                return "job";
            }
        }
        return null;
    }

    public static boolean isQuestNpc(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(QUEST_TAG_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /** NPC 키에 대응하는 화면 표시 이름. */
    public static String npcName(String key) {
        if (key.equals("bank")) {
            return EconomyConfig.get().bankNpcName;
        }
        if (key.equals("job")) {
            return com.rtshop.job.JobConfig.get().npcName;
        }
        if (key.startsWith("shop_")) {
            ShopData d = ShopServer.shops().get(key.substring(5));
            return d == null ? key : d.npcName;
        }
        if (key.startsWith("quest_")) {
            NpcDef n = loaded.npcs.get(key.substring(6));
            return n == null ? key : n.name;
        }
        return key;
    }

    /** 퀘스트 NPC 를 소환한다. 알 수 없는 NPC 면 null. */
    public static Villager spawnNpc(ServerLevel level, Vec3 pos, float yaw, String npcId) {
        NpcDef n = loaded.npcs.get(npcId);
        if (n == null) {
            return null;
        }
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) {
            return null;
        }
        v.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.addTag(QUEST_TAG_PREFIX + n.id);
        v.setCustomName(Component.literal(n.name));
        v.setCustomNameVisible(true);
        VillagerProfession prof = ShopServer.profession(n.profession);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, prof, 5));
        level.addFreshEntity(v);
        return v;
    }

    // ------------------------------------------------------------------ 상태 조회

    private static QuestData.Progress prog(ServerPlayer p) {
        return QuestData.get(p.server).of(p.getUUID());
    }

    public static boolean hasFlag(ServerPlayer p, String flag) {
        return prog(p).flags.contains(flag);
    }

    public static boolean isCompleted(ServerPlayer p, String questId) {
        return prog(p).completed.contains(questId);
    }

    public static boolean isActive(ServerPlayer p, String questId) {
        return prog(p).active.containsKey(questId);
    }

    public static void setFlag(ServerPlayer p, String flag, boolean on) {
        QuestData.Progress pr = prog(p);
        if (on ? pr.flags.add(flag) : pr.flags.remove(flag)) {
            QuestData.get(p.server).dirty();
        }
    }

    /** 받을 수 있는 조건(선행 퀘스트·표식)을 만족하는가. 이미 완료했거나 진행 중이면 false. */
    public static boolean isAvailable(ServerPlayer p, QuestDef q) {
        QuestData.Progress pr = prog(p);
        if (pr.completed.contains(q.id) || pr.active.containsKey(q.id)) {
            return false;
        }
        for (String r : q.requires) {
            if (!pr.completed.contains(r)) {
                return false;
            }
        }
        for (String f : q.requiresFlags) {
            if (!pr.flags.contains(f)) {
                return false;
            }
        }
        for (String f : q.blockedByFlags) {
            if (pr.flags.contains(f)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ 목표 계산

    private static boolean itemMatches(ItemStack s, String target) {
        if (s.isEmpty()) {
            return false;
        }
        if (target.startsWith("#")) {
            return s.is(TagKey.create(Registries.ITEM, new ResourceLocation(target.substring(1))));
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(s.getItem());
        return key != null && key.toString().equals(target);
    }

    private static int countItems(ServerPlayer p, String target) {
        long n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (itemMatches(s, target)) {
                n += s.getCount();
            }
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (itemMatches(s, target)) {
                n += s.getCount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    private static void takeItems(ServerPlayer p, String target, int amount) {
        int left = amount;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) {
                return;
            }
            if (itemMatches(s, target)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (left <= 0) {
                return;
            }
            if (itemMatches(s, target)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
    }

    /** 목표 i 의 현재 수치 (납품·지불은 지금 가진 양, 나머지는 저장된 진행도). */
    private static int current(ServerPlayer p, QuestDef.Objective ob, int stored) {
        return switch (ob.type) {
            case DELIVER -> Math.min(ob.count, countItems(p, ob.target));
            case PAY -> (int) Math.min(ob.count, Economy.wallet(p));
            default -> Math.min(ob.count, stored);
        };
    }

    private static boolean allDone(ServerPlayer p, QuestDef q, int[] progress) {
        for (int i = 0; i < q.objectives.size(); i++) {
            QuestDef.Objective ob = q.objectives.get(i);
            int stored = progress != null && i < progress.length ? progress[i] : 0;
            if (current(p, ob, stored) < ob.count) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ 화면용 정보

    private static QuestView view(ServerPlayer p, QuestDef q, int state, int[] progress) {
        List<QuestView.ObjView> objs = new ArrayList<>();
        for (int i = 0; i < q.objectives.size(); i++) {
            QuestDef.Objective ob = q.objectives.get(i);
            int stored = progress != null && i < progress.length ? progress[i] : 0;
            int cur = state == QuestView.AVAILABLE ? 0 : current(p, ob, stored);
            String text = ob.text;
            String target = ob.target;
            if (text.isEmpty() && ob.type == QuestDef.Type.TALK) {
                text = npcName(ob.target);
            } else if (text.isEmpty() && ob.type == QuestDef.Type.VISIT) {
                text = ob.x + ", " + ob.y + ", " + ob.z;
            }
            objs.add(new QuestView.ObjView(ob.type.ordinal(), target, text, cur, ob.count));
        }
        List<QuestView.RewardItem> items = new ArrayList<>();
        for (QuestDef.ItemReward ir : q.rewardItems) {
            items.add(new QuestView.RewardItem(ir.item, ir.count));
        }
        List<String> choices = new ArrayList<>();
        for (QuestDef.Choice c : q.choices) {
            choices.add(c.text);
        }
        String text = switch (state) {
            case QuestView.AVAILABLE -> q.intro;
            case QuestView.READY -> q.done.isEmpty() ? q.progress : q.done;
            default -> q.progress.isEmpty() ? q.intro : q.progress;
        };
        return new QuestView(q.id, q.title, q.chain, state, text, objs, q.rewardGold, items, choices);
    }

    /** 한 NPC 앞에서 보일 퀘스트 목록. */
    public static List<QuestView> viewsFor(ServerPlayer p, String npcKey) {
        QuestData.Progress pr = prog(p);
        List<QuestView> out = new ArrayList<>();
        for (QuestDef q : loaded.quests.values()) {
            int[] pg = pr.active.get(q.id);
            if (pg != null) {
                if (!q.giver.equals(npcKey) && !q.turnInNpc().equals(npcKey)) {
                    continue;
                }
                boolean ready = allDone(p, q, pg) && q.turnInNpc().equals(npcKey);
                out.add(view(p, q, ready ? QuestView.READY : QuestView.ACTIVE, pg));
            } else if (q.giver.equals(npcKey) && isAvailable(p, q)) {
                out.add(view(p, q, QuestView.AVAILABLE, null));
            }
        }
        return out;
    }

    /** 수첩(진행 중인 모든 퀘스트). */
    public static List<QuestView> journalViews(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        List<QuestView> out = new ArrayList<>();
        for (QuestDef q : loaded.quests.values()) {
            int[] pg = pr.active.get(q.id);
            if (pg != null) {
                out.add(view(p, q, allDone(p, q, pg) ? QuestView.READY : QuestView.ACTIVE, pg));
            }
        }
        return out;
    }

    private static void send(ServerPlayer p, String npcKey, String title, String greeting, List<QuestView> views, boolean journal) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new QuestOpenPacket(npcKey == null ? "" : npcKey, title, greeting, journal, views));
    }

    private static void refresh(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null && s.npcKey() != null) {
            sendDialog(p, s.npcKey());
        } else {
            send(p, null, Component.translatable("rtshop.quest.journal").getString(), "", journalViews(p), true);
        }
    }

    private static void sendDialog(ServerPlayer p, String npcKey) {
        String greeting = "";
        if (npcKey.startsWith("quest_")) {
            NpcDef n = loaded.npcs.get(npcKey.substring(6));
            if (n != null) {
                greeting = n.greeting;
            }
        }
        send(p, npcKey, npcName(npcKey), greeting, viewsFor(p, npcKey), false);
    }

    // ------------------------------------------------------------------ 대화 / 수첩 열기

    /** NPC 와 대화: TALK 목표를 채우고 퀘스트 창을 연다. */
    public static void openDialog(ServerPlayer p, String npcKey, int entityId) {
        SESSIONS.put(p.getUUID(), new Session(npcKey, entityId));
        onTalk(p, npcKey);
        sendDialog(p, npcKey);
    }

    /** 채팅의 [퀘스트 보기] 를 눌렀을 때: 가까이 있는 NPC 의 퀘스트 창을 연다. */
    public static boolean openFromLink(ServerPlayer p, int entityId) {
        Entity e = p.level().getEntity(entityId);
        double r = EconomyConfig.get().npcRange + 2;
        String key = e == null ? null : npcKey(e);
        if (key == null || !e.isAlive() || e.distanceToSqr(p) > r * r) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.too_far"));
            return false;
        }
        openDialog(p, key, entityId);
        return true;
    }

    public static void openJournal(ServerPlayer p) {
        SESSIONS.put(p.getUUID(), new Session(null, -1));
        send(p, null, Component.translatable("rtshop.quest.journal").getString(), "", journalViews(p), true);
    }

    private static boolean sessionOk(ServerPlayer p, Session s) {
        if (s == null || s.npcKey() == null) {
            return false;
        }
        if (s.entityId() < 0) {
            return true; // 관리자 명령어로 연 경우
        }
        Entity e = p.level().getEntity(s.entityId());
        double r = EconomyConfig.get().npcRange;
        return e != null && e.isAlive() && e.distanceToSqr(p) <= r * r;
    }

    // ------------------------------------------------------------------ 행동

    /** 화면에서 온 요청 (수락 / 완료 / 포기). */
    public static void handleAction(ServerPlayer p, int action, String questId, int choice) {
        Session s = SESSIONS.get(p.getUUID());
        switch (action) {
            case QuestActionPacket.ACCEPT -> {
                if (!sessionOk(p, s)) {
                    p.sendSystemMessage(Component.translatable("rtshop.quest.too_far"));
                    return;
                }
                accept(p, questId, s.npcKey());
            }
            case QuestActionPacket.TURN_IN -> {
                if (!sessionOk(p, s)) {
                    p.sendSystemMessage(Component.translatable("rtshop.quest.too_far"));
                    return;
                }
                turnIn(p, questId, s.npcKey(), choice);
            }
            case QuestActionPacket.ABANDON -> abandon(p, questId);
            default -> {
            }
        }
        refresh(p);
    }

    /** 퀘스트 수락. 실패하면 이유를 알리고 false. */
    public static boolean accept(ServerPlayer p, String questId, String npcKey) {
        QuestDef q = loaded.quests.get(questId);
        if (q == null || (npcKey != null && !q.giver.equals(npcKey))) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.unknown"));
            return false;
        }
        if (!isAvailable(p, q)) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.not_available"));
            return false;
        }
        QuestData.Progress pr = prog(p);
        if (pr.active.size() >= MAX_ACTIVE) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.too_many", MAX_ACTIVE));
            return false;
        }
        pr.active.put(q.id, new int[q.objectives.size()]);
        QuestData.get(p.server).dirty();
        p.sendSystemMessage(Component.translatable("rtshop.quest.accepted", q.title));
        // 이미 조건을 채운 상태(방문 위치에 서 있음 등)를 바로 반영
        checkVisits(p);
        return true;
    }

    public static boolean abandon(ServerPlayer p, String questId) {
        QuestData.Progress pr = prog(p);
        if (pr.active.remove(questId) == null) {
            return false;
        }
        QuestData.get(p.server).dirty();
        QuestDef q = loaded.quests.get(questId);
        p.sendSystemMessage(Component.translatable("rtshop.quest.abandoned", q == null ? questId : q.title));
        return true;
    }

    /** 퀘스트 완료. npcKey 가 null 이면 NPC 확인을 건너뛴다 (명령어/점검용). */
    public static boolean turnIn(ServerPlayer p, String questId, String npcKey, int choice) {
        QuestDef q = loaded.quests.get(questId);
        QuestData.Progress pr = prog(p);
        int[] pg = pr.active.get(questId);
        if (q == null || pg == null) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.unknown"));
            return false;
        }
        if (npcKey != null && !q.turnInNpc().equals(npcKey)) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.wrong_npc", npcName(q.turnInNpc())));
            return false;
        }
        if (!allDone(p, q, pg)) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.not_done"));
            return false;
        }
        if (!q.choices.isEmpty() && (choice < 0 || choice >= q.choices.size())) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.need_choice"));
            return false;
        }
        // 납품·지불을 가져간다
        for (QuestDef.Objective ob : q.objectives) {
            if (ob.type == QuestDef.Type.DELIVER) {
                takeItems(p, ob.target, ob.count);
            } else if (ob.type == QuestDef.Type.PAY) {
                Economy.addWallet(p, -ob.count);
            }
        }
        pr.active.remove(q.id);
        pr.completed.add(q.id);
        pr.flags.addAll(q.rewardFlags);
        if (choice >= 0 && choice < q.choices.size()) {
            pr.flags.addAll(q.choices.get(choice).flags);
        }
        QuestData.get(p.server).dirty();
        giveRewards(p, q);
        p.sendSystemMessage(Component.translatable("rtshop.quest.completed", q.title));
        return true;
    }

    private static void giveRewards(ServerPlayer p, QuestDef q) {
        if (q.rewardGold > 0) {
            Economy.addWallet(p, q.rewardGold);
            p.sendSystemMessage(Component.translatable("rtshop.quest.reward_gold", String.format("%,d", q.rewardGold)));
        }
        for (QuestDef.ItemReward ir : q.rewardItems) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ir.item));
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                continue;
            }
            int left = ir.count;
            int max = Math.max(1, new ItemStack(item).getMaxStackSize());
            while (left > 0) {
                int n = Math.min(left, max);
                ItemStack s = new ItemStack(item, n);
                if (!p.getInventory().add(s)) {
                    p.drop(s, false);
                }
                left -= n;
            }
            p.sendSystemMessage(Component.translatable("rtshop.quest.reward_item",
                    new ItemStack(item).getHoverName(), ir.count));
        }
    }

    // ------------------------------------------------------------------ 진행 추적

    private static void notifyProgress(ServerPlayer p, QuestDef q, int[] pg) {
        if (allDone(p, q, pg)) {
            p.sendSystemMessage(Component.translatable("rtshop.quest.objectives_done", q.title,
                    npcName(q.turnInNpc())));
        } else {
            p.displayClientMessage(Component.translatable("rtshop.quest.progress", q.title), true);
        }
    }

    /** 대화 목표 진행. */
    public static void onTalk(ServerPlayer p, String npcKey) {
        QuestData.Progress pr = prog(p);
        boolean changed = false;
        for (Map.Entry<String, int[]> e : new ArrayList<>(pr.active.entrySet())) {
            QuestDef q = loaded.quests.get(e.getKey());
            if (q == null) {
                continue;
            }
            int[] pg = e.getValue();
            boolean touched = false;
            for (int i = 0; i < q.objectives.size() && i < pg.length; i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type == QuestDef.Type.TALK && ob.target.equals(npcKey) && pg[i] < ob.count) {
                    pg[i] = ob.count;
                    touched = true;
                }
            }
            if (touched) {
                changed = true;
                notifyProgress(p, q, pg);
            }
        }
        if (changed) {
            QuestData.get(p.server).dirty();
        }
    }

    /** 처치 목표 진행. */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        QuestData.Progress pr = prog(p);
        if (pr.active.isEmpty()) {
            return;
        }
        ResourceLocation vid = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        String id = vid == null ? "" : vid.toString();
        boolean monster = victim.getType().getCategory() == MobCategory.MONSTER;
        boolean changed = false;
        for (Map.Entry<String, int[]> e : new ArrayList<>(pr.active.entrySet())) {
            QuestDef q = loaded.quests.get(e.getKey());
            if (q == null) {
                continue;
            }
            int[] pg = e.getValue();
            boolean touched = false;
            for (int i = 0; i < q.objectives.size() && i < pg.length; i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type != QuestDef.Type.KILL || pg[i] >= ob.count) {
                    continue;
                }
                if (ob.target.equals(id) || (ob.target.equals("*") && monster)) {
                    pg[i]++;
                    touched = true;
                }
            }
            if (touched) {
                changed = true;
                notifyProgress(p, q, pg);
            }
        }
        if (changed) {
            QuestData.get(p.server).dirty();
        }
    }

    /** 방문 목표 진행 (지금 위치 기준). */
    public static void checkVisits(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        if (pr.active.isEmpty()) {
            return;
        }
        String dim = p.level().dimension().location().toString();
        boolean changed = false;
        for (Map.Entry<String, int[]> e : new ArrayList<>(pr.active.entrySet())) {
            QuestDef q = loaded.quests.get(e.getKey());
            if (q == null) {
                continue;
            }
            int[] pg = e.getValue();
            boolean touched = false;
            for (int i = 0; i < q.objectives.size() && i < pg.length; i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type != QuestDef.Type.VISIT || pg[i] >= ob.count || !ob.target.equals(dim)) {
                    continue;
                }
                double dx = p.getX() - (ob.x + 0.5);
                double dy = p.getY() - ob.y;
                double dz = p.getZ() - (ob.z + 0.5);
                if (dx * dx + dy * dy + dz * dz <= (double) ob.radius * ob.radius) {
                    pg[i] = ob.count;
                    touched = true;
                }
            }
            if (touched) {
                changed = true;
                notifyProgress(p, q, pg);
            }
        }
        if (changed) {
            QuestData.get(p.server).dirty();
        }
    }

    // ------------------------------------------------------------------ 관리

    /** 플레이어의 퀘스트 기록을 모두 지운다 (표식 포함). */
    public static void reset(ServerPlayer p) {
        QuestData.get(p.server).clear(p.getUUID());
    }

    /** 조건·목표와 상관없이 퀘스트를 완료 처리한다 (관리자용). 보상은 주지 않는다. */
    public static boolean forceComplete(ServerPlayer p, String questId) {
        QuestDef q = loaded.quests.get(questId);
        if (q == null) {
            return false;
        }
        QuestData.Progress pr = prog(p);
        pr.active.remove(q.id);
        pr.completed.add(q.id);
        pr.flags.addAll(q.rewardFlags);
        QuestData.get(p.server).dirty();
        return true;
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity target = event.getTarget();
        String key = npcKey(target);
        if (key == null) {
            return;
        }
        boolean questNpc = isQuestNpc(target);
        // 상인·은행원·직업 안내인: 웅크리고 말을 걸면 상점 대신 퀘스트 창, 그냥 말을 걸면 원래 화면 + (퀘스트가 있으면) 채팅 안내
        boolean crouchForQuests = !questNpc && event.getEntity().isShiftKeyDown() && event.getEntity() instanceof ServerPlayer p0
                && !viewsFor(p0, key).isEmpty();
        if (questNpc || crouchForQuests) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (questNpc || crouchForQuests) {
            openDialog(sp, key, target.getId());
        } else {
            onTalk(sp, key);
            if (!viewsFor(sp, key).isEmpty()) {
                sp.sendSystemMessage(Component.translatable("rtshop.quest.npc_hint", npcName(key),
                        Component.translatable("rtshop.quest.npc_hint_link").withStyle(st -> st.withColor(net.minecraft.ChatFormatting.GOLD)
                                .withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/rtshop quest open " + target.getId())))));
            }
        }
    }

    @SubscribeEvent
    public static void onKillEvent(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer sp && event.getEntity() != sp) {
            onKill(sp, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        if (event.player instanceof ServerPlayer sp && sp.tickCount % 20 == 0) {
            checkVisits(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    public static void closeSession(ServerPlayer p) {
        SESSIONS.remove(p.getUUID());
    }
}
