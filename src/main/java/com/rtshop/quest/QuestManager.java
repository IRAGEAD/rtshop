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
    private static final String[] DEFAULT_FILES = {"npcs.json", "tutorial.json", "job_quests.json", "story.json"};
    /**
     * 예전 버전의 기본 파일 내용(SHA-256). 설치된 파일이 이것과 '완전히 같으면' 아무도 고치지 않은 것이므로 새 기본 파일로 바꾼다.
     * 한 글자라도 고쳤으면 그대로 둔다.
     */
    private static final Map<String, Set<String>> OLD_DEFAULTS = Map.of(
            "job_quests.json", Set.of(
                    "9a51ce61d48d98107461017071f42f94276d31641b0a88349b1b88387dc8b540", // 3.6.0
                    "1e9b133e53648071971c057461ddfa6cf659694ae328164b0b68ed2f22655c52"), // 3.6.1
            // 스토리 1판 (3.20 ~ 3.27). 고치지 않았다면 2판으로 바꾼다 (진행은 migrateStory 가 옮긴다)
            "story.json", Set.of(
                    "0dd45697fc0f561ba7c05695829b407681e44109f918e91a5cb592dd74f61cbd",
                    "28a39e558504c9821ceda9d4f0ef29492bb8312a1c4dba3163befb4afc8ebd5a",
                    "783b2a39cddfd5931f88581e133775b74bb67b30480f3b4b8f0f5a70612dd2f0",
                    "f3f736fe44395acdef7d7b11fbc4153c7010385699f506da743489afb9ff2093"));

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
        HUD_SENT.clear();
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
                    if (!QuestDef.modOk(q.mod)) {
                        continue; // 그 모드가 없으면 퀘스트째 뺀다 (예: 환술사 숨은 이야기)
                    }
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
            if (tag.equals(com.rtshop.attendance.Attendance.NPC_TAG)) {
                return "attendance";
            }
            if (tag.equals(com.rtshop.gacha.Gacha.NPC_TAG)) {
                return "gacha";
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
        if (key.equals("attendance")) {
            return com.rtshop.attendance.AttendanceConfig.get().npcName;
        }
        if (key.equals("gacha")) {
            return com.rtshop.gacha.GachaConfig.get().npcName;
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

    /** 진행 중인 퀘스트들. */
    public static List<QuestDef> activeQuests(ServerPlayer p) {
        List<QuestDef> out = new ArrayList<>();
        for (String id : prog(p).active.keySet()) {
            QuestDef q = loaded.quests.get(id);
            if (q != null) out.add(q);
        }
        return out;
    }

    public static int[] progressOf(ServerPlayer p, String questId) {
        return prog(p).active.get(questId);
    }

    /** 지금 열려 있는 퀘스트 창의 NPC (엔티티 번호, 없으면 -1). */
    public static int sessionEntity(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        return s == null || s.npcKey() == null ? -1 : s.entityId();
    }

    public static int viewsCount(ServerPlayer p, String npcKey) {
        return viewsFor(p, npcKey).size();
    }

    public static boolean isCompleted(ServerPlayer p, String questId) {
        return prog(p).completed.contains(questId);
    }

    public static boolean isActive(ServerPlayer p, String questId) {
        return prog(p).active.containsKey(questId);
    }

    /** 진행 중인 퀘스트가 이 구조물(ID 또는 #태그)로 가라고 하는가 (STRUCTURE 목표). 결계 구조물에 들어갈 수 있게 한다. */
    public static boolean questLeadsTo(ServerPlayer p, String structure) {
        for (String id : prog(p).active.keySet()) {
            QuestDef q = loaded.quests.get(id);
            if (q == null) continue;
            for (QuestDef.Objective ob : q.objectives) {
                if (ob.type == QuestDef.Type.STRUCTURE && ob.target.equals(structure)) return true;
            }
        }
        return false;
    }

    public static void setFlag(ServerPlayer p, String flag, boolean on) {
        QuestData.Progress pr = prog(p);
        if (on ? pr.flags.add(flag) : pr.flags.remove(flag)) {
            QuestData.get(p.server).dirty();
        }
    }

    /**
     * 스토리 2판으로 옮기기: 예전 스토리에서 끝낸 장은 새 스토리에서도 끝낸 것으로 친다 (장 중간이면 그 장의 처음부터).
     * 한 번만 한다 (story_v3 표식).
     */
    public static boolean migrateStory(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        if (pr.flags.contains("story_v3")) return false;
        // 장을 끝낸 표식이 있으면 3판의 그 장 퀘스트를 모두 끝낸 것으로 하고, 그 퀘스트들이 주는 표식(결계 등)도 준다
        String[][] rules = {
                {"order_apprentice", "story_0_"}, {"story_ch1_done", "story_0_", "story_1_"}, {"story_ch2_done", "story_2_"},
                {"story_ch3_done", "story_3_"}, {"story_ch4_done", "story_4_"}, {"story_done", "story_4_", "story_5_"}};
        int n = 0;
        for (String[] r : rules) {
            if (!pr.flags.contains(r[0])) continue;
            for (QuestDef q : loaded.quests.values()) {
                for (int i = 1; i < r.length; i++) {
                    if (q.id.startsWith(r[i]) && !pr.completed.contains(q.id)) {
                        pr.completed.add(q.id);
                        pr.active.remove(q.id);
                        pr.flags.addAll(q.rewardFlags);
                        n++;
                    }
                }
            }
        }
        // 예전 스토리 퀘스트(새 정의에 없는 것)와, 장 안에서 진행 중이던 예전 퀘스트는 진행 중 목록에서 뺀다 (장의 처음부터 다시)
        pr.active.keySet().removeIf(id -> id.startsWith("story_"));
        if (pr.flags.contains("story_done")) pr.flags.add("story_ch4_done");
        pr.flags.add("story_v3");
        QuestData.get(p.server).dirty();
        return n > 0;
    }

    /**
     * 스토리 4판(대개편)으로 옮기기: 서장·1장이 새로 쓰였다. 1장을 끝낸 사람은 새 서장·1장도 끝낸 것으로,
     * 1장 도중이던 사람은 1장의 처음부터 (서장을 끝냈으면 서장은 끝낸 것으로). 한 번만 한다 (story_v4 표식).
     */
    public static boolean migrateStory4(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        if (pr.flags.contains("story_v4")) return false;
        boolean ch0 = pr.flags.contains("order_apprentice"), ch1 = pr.flags.contains("story_ch1_done");
        int n = 0;
        if (!ch1) { // 1장 도중: 예전 번호로 끝낸 1장 퀘스트와 그 표식을 걷어 낸다
            for (String id : new ArrayList<>(pr.completed)) {
                if (id.startsWith("story_1_") && pr.completed.remove(id)) n++;
            }
            pr.active.keySet().removeIf(id -> id.startsWith("story_1_") || id.startsWith("story_0_"));
            for (String f : List.of("ch1_three_ends", "clue_grey_coin", "clue_gloved_hand", "ch1_saved_records", "ch1_saved_apprentice",
                    "ch1_told_all", "ch1_folded")) pr.flags.remove(f);
        }
        for (QuestDef q : loaded.quests.values()) {
            boolean mark = (ch0 && q.id.startsWith("story_0_")) || (ch1 && q.id.startsWith("story_1_"));
            if (mark && !pr.completed.contains(q.id)) {
                pr.completed.add(q.id);
                pr.active.remove(q.id);
                pr.flags.addAll(q.rewardFlags);
                n++;
            }
        }
        pr.flags.add("story_v4");
        QuestData.get(p.server).dirty();
        return n > 0;
    }

    /** 2장이 새로 쓰였다 (3.34): 2장 도중이던 사람은 2장의 처음부터, 끝낸 사람은 끝낸 것으로. 한 번만 (story_v5 표식). */
    public static boolean migrateStory5(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        if (pr.flags.contains("story_v5")) return false;
        boolean ch2 = pr.flags.contains("story_ch2_done");
        int n = 0;
        if (!ch2) {
            for (String id : new ArrayList<>(pr.completed)) {
                if (id.startsWith("story_2_") && pr.completed.remove(id)) n++;
            }
            pr.active.keySet().removeIf(id -> id.startsWith("story_2_"));
            for (String f : List.of("seal_wroughtnaut", "seal_frostmaw", "seal_sculptor", "seal_umvuthi", "order_member", "clue_eighth_seal", "frost_stolen",
                    "frost_fought", "ch2_told_truth", "ch2_kept_secret")) pr.flags.remove(f);
        } else {
            for (QuestDef q : loaded.quests.values()) {
                if (q.id.startsWith("story_2_") && !pr.completed.contains(q.id)) {
                    pr.completed.add(q.id);
                    pr.active.remove(q.id);
                    pr.flags.addAll(q.rewardFlags);
                    n++;
                }
            }
        }
        pr.flags.add("story_v5");
        QuestData.get(p.server).dirty();
        return n > 0;
    }

    /** 3장이 새로 쓰였다 (3.37): 3장 도중이던 사람은 3장의 처음부터, 끝낸 사람은 끝낸 것으로. 한 번만 (story_v6 표식). */
    public static boolean migrateStory6(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        if (pr.flags.contains("story_v6")) return false;
        boolean ch3 = pr.flags.contains("story_ch3_done");
        int n = 0;
        if (!ch3) {
            for (String id : new ArrayList<>(pr.completed)) {
                if (id.startsWith("story_3_") && pr.completed.remove(id)) n++;
            }
            pr.active.keySet().removeIf(id -> id.startsWith("story_3_"));
            for (String f : List.of("clue_grey_cloak", "seal_maze_mother", "ch3_found_elric", "seal_cornelia", "clue_hidden_page", "ch3_released",
                    "ch3_kept_lantern", "clue_two_cloaks", "clue_mirror_maker")) pr.flags.remove(f);
        } else {
            for (QuestDef q : loaded.quests.values()) {
                if (q.id.startsWith("story_3_") && !pr.completed.contains(q.id)) {
                    pr.completed.add(q.id);
                    pr.active.remove(q.id);
                    for (String f : q.rewardFlags) {
                        if (!f.startsWith("ch3_")) pr.flags.add(f); // 선택 표식은 본인이 고른 것만 남긴다
                    }
                    n++;
                }
            }
            if (!pr.flags.contains("ch3_released") && !pr.flags.contains("ch3_kept_lantern")) pr.flags.add("ch3_kept_lantern");
            pr.flags.add("ch3_found_elric");
        }
        pr.flags.add("story_v6");
        QuestData.get(p.server).dirty();
        return n > 0;
    }

    /** 4장이 새로 쓰였다 (3.38): 4장 도중이던 사람은 4장의 처음부터, 끝낸 사람은 끝낸 것으로. 한 번만 (story_v7 표식). */
    public static boolean migrateStory7(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        if (pr.flags.contains("story_v7")) return false;
        boolean ch4 = pr.flags.contains("story_ch4_done");
        int n = 0;
        if (!ch4) {
            for (String id : new ArrayList<>(pr.completed)) {
                if (id.startsWith("story_4_") && pr.completed.remove(id)) n++;
            }
            pr.active.keySet().removeIf(id -> id.startsWith("story_4_"));
            for (String f : List.of("seal_monstrosity", "seal_remnant", "seal_harbinger", "seal_leviathan", "seal_ender_guardian", "seal_maledictus",
                    "clue_eighth_king", "clue_altar_note", "erwin_betrayal", "ch4_feigned", "ch4_refused", "ch4_mal_afraid", "ch4_mal_brave")) pr.flags.remove(f);
        } else {
            for (QuestDef q : loaded.quests.values()) {
                if (q.id.startsWith("story_4_") && !pr.completed.contains(q.id)) {
                    pr.completed.add(q.id);
                    pr.active.remove(q.id);
                    pr.flags.addAll(q.rewardFlags);
                    n++;
                }
            }
            if (!pr.flags.contains("ch4_feigned") && !pr.flags.contains("ch4_refused")) pr.flags.add("ch4_refused");
            if (!pr.flags.contains("ch4_mal_afraid") && !pr.flags.contains("ch4_mal_brave")) pr.flags.add("ch4_mal_afraid");
        }
        pr.flags.add("story_v7");
        QuestData.get(p.server).dirty();
        return n > 0;
    }

    /** 종장이 새로 쓰였다 (3.39): 종장 도중이던 사람은 종장의 처음부터, 끝낸 사람은 끝낸 것으로. 한 번만 (story_v8 표식). */
    public static boolean migrateStory8(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        if (pr.flags.contains("story_v8")) return false;
        boolean fin = pr.flags.contains("story_done");
        int n = 0;
        if (!fin) {
            for (String id : new ArrayList<>(pr.completed)) {
                if (id.startsWith("story_5_") && pr.completed.remove(id)) n++;
            }
            pr.active.keySet().removeIf(id -> id.startsWith("story_5_"));
            for (String f : List.of("seal_ignis", "ignis_raid_story", "third_line_found", "fin_forgave", "fin_erased", "clue_gate_design", "clue_orel_chose")) pr.flags.remove(f);
        } else {
            for (QuestDef q : loaded.quests.values()) {
                if (q.id.startsWith("story_5_") && !pr.completed.contains(q.id)) {
                    pr.completed.add(q.id);
                    pr.active.remove(q.id);
                    pr.flags.addAll(q.rewardFlags);
                    n++;
                }
            }
            if (!pr.flags.contains("fin_forgave") && !pr.flags.contains("fin_erased")) pr.flags.add("fin_forgave");
            // 숨은 이야기(거울 속의 연대기)는 단계가 늘었다: 끝낸 사람도 새 단계(원본 읽기, 여덟 글자, 이름)는 직접 한다
        }
        // 예전 판에서 환술사를 이미 잠재운 사람(진짜 쪽을 가짐)은 새 판의 대면 단계까지 끝낸 것으로 한다
        if (pr.flags.contains("clue_true_page")) {
            for (String id : List.of("hidden_illusioner_2", "hidden_illusioner_3")) {
                if (loaded.quests.containsKey(id) && pr.completed.add(id)) pr.active.remove(id);
            }
        }
        pr.flags.add("story_v8");
        QuestData.get(p.server).dirty();
        return n > 0;
    }

    @SubscribeEvent
    public static void onLoginMigrate(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && loaded.quests.containsKey("story_1_01")) {
            migrateStory(p);
            migrateStory4(p);
            migrateStory5(p);
            migrateStory6(p);
            migrateStory7(p);
            migrateStory8(p);
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

    /** 진행 중인 퀘스트의 목표를 다 채웠는가 (보고만 남았는가). */
    public static boolean objectivesDone(ServerPlayer p, String questId) {
        QuestDef q = loaded.quests.get(questId);
        int[] pg = prog(p).active.get(questId);
        return q != null && pg != null && allDone(p, q, pg);
    }

    // ------------------------------------------------------------------ 화면용 정보

    private static QuestView view(ServerPlayer p, QuestDef q, int state, int[] progress) {
        List<QuestView.ObjView> objs = new ArrayList<>();
        for (int i = 0; i < q.objectives.size(); i++) {
            QuestDef.Objective ob = q.objectives.get(i);
            int stored = progress != null && i < progress.length ? progress[i] : 0;
            int cur = state == QuestView.AVAILABLE ? 0 : state == QuestView.DONE ? ob.count : current(p, ob, stored);
            String text = ob.text;
            String target = ob.target;
            if (text.isEmpty() && ob.type == QuestDef.Type.TALK) {
                text = npcName(ob.target);
            } else if (text.isEmpty() && ob.type == QuestDef.Type.VISIT) {
                text = ob.x + ", " + ob.y + ", " + ob.z;
            }
            objs.add(new QuestView.ObjView(ob.type.ordinal(), target, com.rtshop.story.StoryText.t(p, text), cur, ob.count));
        }
        List<QuestView.RewardItem> items = new ArrayList<>();
        for (QuestDef.ItemReward ir : q.rewardItems) {
            items.add(new QuestView.RewardItem(ir.item, ir.count));
        }
        List<String> choices = new ArrayList<>();
        for (QuestDef.Choice c : q.choices) {
            choices.add(com.rtshop.story.StoryText.t(p, c.text));
        }
        String text = switch (state) {
            case QuestView.AVAILABLE -> q.intro;
            case QuestView.READY, QuestView.DONE -> q.done.isEmpty() ? q.progress : q.done;
            default -> q.progress.isEmpty() ? q.intro : q.progress;
        };
        String who = state == QuestView.AVAILABLE ? q.giver : q.turnInNpc();
        // 기억의 문: 아직 못 잡은 이야기 보스가 목표면 [보스전 입장]
        String arena = "";
        if (state == QuestView.ACTIVE) {
            for (int i = 0; i < q.objectives.size(); i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type == QuestDef.Type.KILL && objs.get(i).cur() < ob.count && com.rtshop.story.StoryArena.supports(ob.target)) {
                    arena = ob.target;
                    break;
                }
            }
        }
        return new QuestView(q.id, com.rtshop.story.StoryText.t(p, q.title), com.rtshop.story.StoryText.t(p, q.chain), state,
                com.rtshop.story.StoryText.t(p, text), objs, q.rewardGold, items, choices,
                who == null || who.isEmpty() ? "" : com.rtshop.story.StoryText.t(p, npcName(who)), arena);
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

    /** 클라이언트에서 번역할 글: \u0001키\u0002인자\u0002인자... (전용 서버에는 모드 번역이 없다). */
    public static String tr(String key, String... args) {
        StringBuilder b = new StringBuilder("\u0001").append(key);
        for (String a : args) b.append('\u0002').append(a);
        return b.toString();
    }

    /** 이 NPC 가 주거나 받는 퀘스트가 하나라도 정의돼 있는가. */
    public static boolean hasQuests(String npcKey) {
        for (QuestDef q : loaded.quests.values()) {
            if (q.giver.equals(npcKey) || q.turnInNpc().equals(npcKey)) {
                return true;
            }
        }
        return false;
    }

    /** 지금 받을 퀘스트가 없을 때: 이 NPC 의 다음 퀘스트를 열려면 무엇을 먼저 해야 하는지. 없으면 "". */
    public static String lockedHint(ServerPlayer p, String npcKey) {
        QuestData.Progress pr = prog(p);
        for (QuestDef q : loaded.quests.values()) {
            if (!q.giver.equals(npcKey) || pr.completed.contains(q.id) || pr.active.containsKey(q.id)) {
                continue;
            }
            for (String r : q.requires) {
                if (pr.completed.contains(r)) {
                    continue;
                }
                QuestDef need = loaded.quests.get(r);
                if (need == null) {
                    continue;
                }
                // 사슬의 맨 앞(아직 끝내지 않은 가장 이른 퀘스트)까지 거슬러 올라간다
                for (int guard = 0; guard < 32; guard++) {
                    QuestDef prev = null;
                    for (String r2 : need.requires) {
                        QuestDef d = loaded.quests.get(r2);
                        if (d != null && !pr.completed.contains(r2)) {
                            prev = d;
                            break;
                        }
                    }
                    if (prev == null) {
                        break;
                    }
                    need = prev;
                }
                String who = need.giver.isEmpty() ? "" : npcName(need.giver);
                return tr(pr.active.containsKey(need.id) ? "rtshop.quest.locked_active" : "rtshop.quest.locked_hint",
                        com.rtshop.story.StoryText.t(p, q.title), com.rtshop.story.StoryText.t(p, need.title), com.rtshop.story.StoryText.t(p, who));
            }
            if (!q.requiresFlags.isEmpty()) {
                return tr("rtshop.quest.locked_flag", com.rtshop.story.StoryText.t(p, q.title));
            }
        }
        return "";
    }

    /** 수첩: 진행 중 → 지금 받을 수 있는 것 → 끝낸 것. 내용은 어디서나 볼 수 있고, 받기·완료·포기는 그 NPC 앞에서만. */
    public static List<QuestView> journalViews(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        List<QuestView> out = new ArrayList<>();
        for (QuestDef q : loaded.quests.values()) {
            int[] pg = pr.active.get(q.id);
            if (pg != null) {
                out.add(view(p, q, allDone(p, q, pg) ? QuestView.READY : QuestView.ACTIVE, pg));
            }
        }
        for (QuestDef q : loaded.quests.values()) {
            if (!pr.active.containsKey(q.id) && !q.giver.isEmpty() && isAvailable(p, q)) {
                out.add(view(p, q, QuestView.AVAILABLE, null));
            }
        }
        List<QuestView> done = new ArrayList<>();
        for (QuestDef q : loaded.quests.values()) {
            if (pr.completed.contains(q.id) && !pr.active.containsKey(q.id)) {
                done.add(view(p, q, QuestView.DONE, null));
            }
        }
        java.util.Collections.reverse(done); // 최근에 가까운 것부터
        out.addAll(done);
        return out;
    }

    private static void send(ServerPlayer p, String npcKey, String title, String greeting, List<QuestView> views, boolean journal) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new QuestOpenPacket(npcKey == null ? "" : npcKey, com.rtshop.story.StoryText.t(p, title), com.rtshop.story.StoryText.t(p, greeting), journal, views));
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
        List<QuestView> views = viewsFor(p, npcKey);
        if (views.isEmpty()) {
            String hint = lockedHint(p, npcKey);
            if (!hint.isEmpty()) {
                greeting = greeting.isEmpty() ? hint : greeting + "\n\n" + hint;
            }
        }
        send(p, npcKey, npcName(npcKey), greeting, views, false);
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
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.too_far"));
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
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.too_far"));
                    return;
                }
                accept(p, questId, s.npcKey());
            }
            case QuestActionPacket.TURN_IN -> {
                if (!sessionOk(p, s)) {
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.too_far"));
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
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.unknown"));
            return false;
        }
        if (!isAvailable(p, q)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.not_available"));
            return false;
        }
        QuestData.Progress pr = prog(p);
        if (pr.active.size() >= MAX_ACTIVE) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.too_many", MAX_ACTIVE));
            return false;
        }
        // 직업 선행 퀘스트는 전투·생활 직업마다 한 직업씩만 진행한다
        String jobId = jobOf(q.id);
        if (jobId != null) {
            // 이미 그 칸(전투/생활)에 직업이 있으면 다른 직업의 선행 퀘스트는 할 수 없다
            String has = hasOtherJob(p, jobId);
            if (has != null) {
                com.rtshop.job.JobConfig.JobDef hd = com.rtshop.job.JobConfig.get().jobs.get(has);
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.job_has", hd == null ? has : hd.name));
                return false;
            }
            String busy = busyJob(pr, jobId);
            if (busy != null) {
                com.rtshop.job.JobConfig.JobDef bd = com.rtshop.job.JobConfig.get().jobs.get(busy);
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.job_busy", bd == null ? busy : bd.name));
                return false;
            }
            pr.flags.remove(JOB_PAUSED + jobId);
        }
        pr.active.put(q.id, new int[q.objectives.size()]);
        QuestData.get(p.server).dirty();
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.accepted", com.rtshop.story.StoryText.t(p, q.title)));
        com.rtshop.story.StagedStructures.scan(p); // 이 퀘스트가 가리키는 이야기 건축물이 새 지형에 생기기 시작한다
        scene(p, q.sceneAccept);
        // 이미 조건을 채운 상태(방문 위치에 서 있음 등)를 바로 반영
        checkVisits(p);
        return true;
    }

    public static boolean abandon(ServerPlayer p, String questId) {
        QuestData.Progress pr = prog(p);
        if (pr.active.remove(questId) == null) {
            return false;
        }
        QuestDef q = loaded.quests.get(questId);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.abandoned", q == null ? questId : com.rtshop.story.StoryText.t(p, q.title)));
        String jobId = jobOf(questId);
        if (jobId != null) {
            // 이 직업의 선행 퀘스트를 그만둔다: 다른 직업의 선행 퀘스트를 받을 수 있다 (다시 받으면 이어서 진행)
            pr.flags.add(JOB_PAUSED + jobId);
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.job_paused"));
        }
        QuestData.get(p.server).dirty();
        return true;
    }

    // ------------------------------------------------------------------ 직업 선행 퀘스트: 전투·생활 직업마다 하나씩

    private static final String JOB_PAUSED = "_job_paused_";
    private static final java.util.regex.Pattern JOB_QUEST = java.util.regex.Pattern.compile("^job_([a-z0-9_]+?)_(\\d+)$");

    /** 직업 선행 퀘스트면 그 직업 ID, 아니면 null. */
    public static String jobOf(String questId) {
        java.util.regex.Matcher m = JOB_QUEST.matcher(questId == null ? "" : questId);
        if (!m.matches()) return null;
        String id = m.group(1);
        return com.rtshop.job.JobConfig.get().jobs.containsKey(id) ? id : null;
    }

    /** 이 직업의 선행 퀘스트를 진행 중인가 (받아 둔 단계가 있거나, 몇 단계를 깼고 그만두지 않았으며 아직 직업을 열지 않음). */
    public static boolean jobInProgress(QuestData.Progress pr, String jobId) {
        com.rtshop.job.JobConfig.JobDef d = com.rtshop.job.JobConfig.get().jobs.get(jobId);
        if (d == null || (!d.unlockFlag.isEmpty() && pr.flags.contains(d.unlockFlag))) return false;
        for (String a : pr.active.keySet()) if (jobId.equals(jobOf(a))) return true;
        if (pr.flags.contains(JOB_PAUSED + jobId)) return false;
        for (String c : pr.completed) if (jobId.equals(jobOf(c))) return true;
        return false;
    }

    /** jobId 의 선행 퀘스트를 받으려 할 때, 같은 칸(전투/생활)에서 이미 진행 중인 다른 직업 (없으면 null). */
    /** 그 직업과 같은 칸에 이미 다른 직업이 있으면 그 직업 ID. */
    public static String hasOtherJob(ServerPlayer p, String jobId) {
        com.rtshop.job.JobConfig.JobDef me = com.rtshop.job.JobConfig.get().jobs.get(jobId);
        if (me == null) return null;
        String cur = com.rtshop.job.Jobs.entry(p).get(me.slot);
        return cur == null || cur.isEmpty() || cur.equals(jobId) ? null : cur;
    }

    public static String busyJob(QuestData.Progress pr, String jobId) {
        com.rtshop.job.JobConfig cfg = com.rtshop.job.JobConfig.get();
        com.rtshop.job.JobConfig.JobDef me = cfg.jobs.get(jobId);
        if (me == null) return null;
        for (com.rtshop.job.JobConfig.JobDef d : cfg.jobs.values()) {
            if (d.id.equals(jobId) || !d.slot.equals(me.slot)) continue;
            if (jobInProgress(pr, d.id)) return d.id;
        }
        return null;
    }

    /** 퀘스트 완료. npcKey 가 null 이면 NPC 확인을 건너뛴다 (명령어/점검용). */
    public static boolean turnIn(ServerPlayer p, String questId, String npcKey, int choice) {
        QuestDef q = loaded.quests.get(questId);
        QuestData.Progress pr = prog(p);
        int[] pg = pr.active.get(questId);
        if (q == null || pg == null) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.unknown"));
            return false;
        }
        if (npcKey != null && !q.turnInNpc().equals(npcKey)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.wrong_npc", npcName(q.turnInNpc())));
            return false;
        }
        if (!allDone(p, q, pg)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.not_done"));
            return false;
        }
        if (!q.choices.isEmpty() && (choice < 0 || choice >= q.choices.size())) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.need_choice"));
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
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.completed", com.rtshop.story.StoryText.t(p, q.title)));
        scene(p, q.sceneDone);
        nextHint(p, q, npcKey);
        if (!q.rewardFlags.isEmpty()) {
            com.rtshop.story.StoryGates.sync(p);
            com.rtshop.story.ScyllaClues.checkComplete(p); // 퀘스트가 스킬라 단서를 줄 수도 있다
        }
        return true;
    }

    /** 완료한 퀘스트 다음에 이어지는 퀘스트를 누가 주는지 알려 준다 (준 NPC 의 말로). */
    static void nextHint(ServerPlayer p, QuestDef done, String fromNpc) {
        String speaker = fromNpc == null ? null : com.rtshop.story.StoryText.t(p, npcName(fromNpc));
        java.util.Set<String> told = new java.util.HashSet<>();
        for (QuestDef n : loaded.quests.values()) {
            if (!n.requires.contains(done.id) || !isAvailable(p, n) || !told.add(n.giver)) continue;
            Component msg = n.giver.equals(fromNpc)
                    ? Component.translatable("rtshop.quest.next_same", com.rtshop.story.StoryText.t(p, n.title))
                    : Component.translatable("rtshop.quest.next_npc", com.rtshop.story.StoryText.t(p, npcName(n.giver)), com.rtshop.story.StoryText.t(p, n.title));
            com.rtshop.notice.Notice.send(p, speaker == null ? msg.copy().withStyle(net.minecraft.ChatFormatting.GOLD)
                    : Component.literal("[" + speaker + "] ").withStyle(net.minecraft.ChatFormatting.YELLOW).append(msg.copy().withStyle(net.minecraft.ChatFormatting.GOLD)));
        }
    }

    /** 이야기 연출을 보낸다. "party": true 면 가까운 파티원에게도. */
    public static void scene(ServerPlayer p, com.google.gson.JsonObject o) {
        if (o == null) return;
        if (o.has("onSleep") && o.get("onSleep").getAsBoolean()) {
            // 꿈 장면: 다음에 침대에서 잠들 때 보여 준다
            p.getPersistentData().putString(SLEEP_SCENE, o.toString());
            return;
        }
        com.google.gson.JsonObject c = o.deepCopy();
        if (!c.has("closeScreen")) c.addProperty("closeScreen", true);
        com.rtshop.story.BossIntros.send(p, c);
        if (o.has("party") && o.get("party").getAsBoolean()) {
            com.rtshop.party.Party.Group g = com.rtshop.party.Party.of(p.server, p.getUUID());
            for (ServerPlayer m : com.rtshop.party.Party.online(p.server, g)) {
                if (m != p && m.level() == p.level() && m.distanceTo(p) < 64) com.rtshop.story.BossIntros.send(m, c);
            }
        }
    }

    private static void giveRewards(ServerPlayer p, QuestDef q) {
        if (q.rewardGold > 0) {
            Economy.addWallet(p, q.rewardGold);
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.reward_gold", String.format("%,d", q.rewardGold)));
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
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.reward_item",
                    new ItemStack(item).getHoverName(), ir.count));
        }
    }

    // ------------------------------------------------------------------ 진행 추적

    private static void notifyProgress(ServerPlayer p, QuestDef q, int[] pg) {
        if (allDone(p, q, pg)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.objectives_done", com.rtshop.story.StoryText.t(p, q.title),
                    npcName(q.turnInNpc())));
        } else {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.progress", com.rtshop.story.StoryText.t(p, q.title)), true);
        }
    }

    /** 대화 목표 진행. */
    public static void onTalk(ServerPlayer p, String npcKey) {
        onTalk(p, npcKey, false);
    }

    /** sceneDone: 대화 장면(lines)이 있는 TALK 목표도 채운다 (장면을 다 본 뒤). */
    public static void onTalk(ServerPlayer p, String npcKey, boolean sceneDone) {
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
                if (ob.type == QuestDef.Type.TALK && ob.target.equals(npcKey) && pg[i] < ob.count
                        && (sceneDone || ob.lines == null || ob.lines.size() == 0)) {
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
        com.rtshop.story.Hunts.onKill(p, victim); // 토벌 의뢰·몹 도감 (처치 인정은 퀘스트와 같다)
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

    /** 구조물·발전과제·차원 목표를 지금 이뤘는가. */
    public static boolean reached(ServerPlayer p, QuestDef.Objective ob, String dim) {
        try {
            switch (ob.type) {
                case DIMENSION:
                    return ob.target.equals(dim);
                case FLAG:
                    return hasFlag(p, ob.target);
                case ADVANCEMENT: {
                    net.minecraft.advancements.Advancement adv = p.server.getAdvancements().getAdvancement(new ResourceLocation(ob.target));
                    return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
                }
                case STRUCTURE: {
                    net.minecraft.server.level.ServerLevel level = p.serverLevel();
                    net.minecraft.core.BlockPos pos = p.blockPosition();
                    net.minecraft.world.level.StructureManager sm = level.structureManager();
                    net.minecraft.core.Registry<net.minecraft.world.level.levelgen.structure.Structure> reg =
                            level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                    if (ob.target.startsWith("#")) {
                        net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> tag =
                                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE, new ResourceLocation(ob.target.substring(1)));
                        if (sm.getStructureWithPieceAt(pos, tag).isValid()) return true;
                        for (net.minecraft.world.level.levelgen.structure.Structure s : sm.getAllStructuresAt(pos).keySet()) {
                            if (reg.wrapAsHolder(s).is(tag) && near(sm, pos, s)) return true;
                        }
                        return false;
                    }
                    if (ob.target.endsWith(":*")) {
                        String ns = ob.target.substring(0, ob.target.length() - 2);
                        for (net.minecraft.world.level.levelgen.structure.Structure s : sm.getAllStructuresAt(pos).keySet()) {
                            ResourceLocation key = reg.getKey(s);
                            if (key != null && key.getNamespace().equals(ns) && (sm.getStructureWithPieceAt(pos, s).isValid() || near(sm, pos, s))) return true;
                        }
                        return false;
                    }
                    net.minecraft.world.level.levelgen.structure.Structure s = reg.get(new ResourceLocation(ob.target));
                    return s != null && (sm.getStructureWithPieceAt(pos, s).isValid() || near(sm, pos, s));
                }
                default:
                    return false;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    /** 지금 이 구조물(ID, #태그, "모드:*") 안이나 곁에 있는가. */
    public static boolean atStructure(ServerPlayer p, String target) {
        if (target == null || target.isBlank()) return true;
        if (target.startsWith("dim:")) return p.level().dimension().location().toString().equals(target.substring(4).trim()); // 그 차원에 들어서면
        QuestDef.Objective ob = new QuestDef.Objective();
        ob.type = QuestDef.Type.STRUCTURE;
        ob.target = target.trim();
        return reached(p, ob, p.level().dimension().location().toString());
    }

    /** 아직 살펴보지 않은 조사 목표들: {퀘스트, 목표}. */
    public static List<Object[]> pendingInspects(ServerPlayer p) {
        List<Object[]> out = new ArrayList<>();
        QuestData.Progress pr = prog(p);
        for (Map.Entry<String, int[]> e : pr.active.entrySet()) {
            QuestDef q = loaded.quests.get(e.getKey());
            if (q == null) continue;
            int[] pg = e.getValue();
            for (int i = 0; i < q.objectives.size() && i < pg.length; i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type == QuestDef.Type.INSPECT && pg[i] < ob.count) out.add(new Object[]{q, ob});
            }
        }
        return out;
    }

    /** 흔적을 살펴봤다: 그 조사 목표를 채운다. */
    public static boolean completeInspect(ServerPlayer p, String spotId) {
        QuestData.Progress pr = prog(p);
        boolean any = false;
        for (Map.Entry<String, int[]> e : new ArrayList<>(pr.active.entrySet())) {
            QuestDef q = loaded.quests.get(e.getKey());
            if (q == null) continue;
            int[] pg = e.getValue();
            boolean touched = false;
            for (int i = 0; i < q.objectives.size() && i < pg.length; i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type == QuestDef.Type.INSPECT && ob.target.equals(spotId) && pg[i] < ob.count) {
                    pg[i] = ob.count;
                    touched = true;
                }
            }
            if (touched) {
                any = true;
                notifyProgress(p, q, pg);
            }
        }
        if (any) QuestData.get(p.server).dirty();
        return any;
    }

    /** 구조물 곁 (작은 구조물은 조각 안에 딱 서기 어렵다: 전체 범위를 8칸 넓혀 본다). */
    private static boolean near(net.minecraft.world.level.StructureManager sm, net.minecraft.core.BlockPos pos,
                                net.minecraft.world.level.levelgen.structure.Structure s) {
        net.minecraft.world.level.levelgen.structure.StructureStart st = sm.getStructureAt(pos, s);
        return st.isValid() && st.getBoundingBox().inflatedBy(8).isInside(pos);
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
                if (pg[i] < ob.count && ob.type != QuestDef.Type.VISIT && reached(p, ob, dim)) {
                    pg[i] = ob.count;
                    touched = true;
                    continue;
                }
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
        // 대화 상자가 먼저 열리는 NPC: 퀘스트 창은 대화의 [퀘스트] 로만 연다 (같이 열리면 대화 상자를 덮어 버렸다)
        if (event.getEntity() instanceof ServerPlayer claimer && com.rtshop.story.Dialogue.claims(claimer, key)) {
            return;
        }
        boolean questNpc = isQuestNpc(target);
        // 퀘스트 전용 NPC: 바로 퀘스트 창. 상인·은행원·직업 안내인 등: 원래 화면을 열고, 받을 퀘스트가 있으면 그 화면에 [퀘스트] 단추가 뜬다.
        if (questNpc) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (questNpc) {
            openDialog(sp, key, target.getId());
        } else {
            onTalk(sp, key);
            int n = viewsFor(sp, key).size();
            com.rtshop.net.ShopNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new com.rtshop.net.NpcQuestPacket(target.getId(), n, hasQuests(key)));
        }
    }

    public static final String SLEEP_SCENE = "rtshop_sleep_scene";

    /** 침대에서 잠이 들면(잠든 지 1.5초) 미뤄 둔 꿈 장면을 보여 준다. 잠자는 화면은 닫지 않는다 (닫으면 깨어난다). */
    @SubscribeEvent
    public static void onSleepTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        if (!p.isSleeping() || p.getSleepTimer() < 30 || !p.getPersistentData().contains(SLEEP_SCENE)) return;
        try {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(p.getPersistentData().getString(SLEEP_SCENE)).getAsJsonObject();
            o.remove("onSleep");
            o.addProperty("closeScreen", false);
            p.getPersistentData().remove(SLEEP_SCENE);
            com.rtshop.story.BossIntros.send(p, o);
        } catch (Exception e) {
            p.getPersistentData().remove(SLEEP_SCENE);
        }
    }

    @SubscribeEvent
    public static void onKillEvent(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        net.minecraft.server.MinecraftServer server = victim.getServer();
        if (server == null || victim instanceof ServerPlayer) {
            if (event.getSource().getEntity() instanceof ServerPlayer sp && victim != sp) onKill(sp, victim);
            return;
        }
        if (com.rtshop.party.BossCredit.isBoss(victim)) {
            // 보스: 최대 체력의 일정 비율(기본 10%) 이상을 직접 입힌 사람만 처치로 인정
            for (ServerPlayer p : com.rtshop.party.BossCredit.qualified(victim, server)) {
                onKill(p, victim);
                com.rtshop.story.ScyllaClues.roll(p, "boss", "", 1, null); // 이야기 보스 한 마리가 시도 한 번
            }
            return;
        }
        ServerPlayer killer = com.rtshop.pvp.Pvp.attackerOf(event.getSource(), server);
        // 동행이 잡은 것은 함께 다니는 플레이어의 몫으로 센다
        if (killer == null && event.getSource().getEntity() instanceof com.rtshop.story.CompanionEntity c) killer = c.owner();
        if (killer == null) return;
        onKill(killer, victim);
        // 일반 몹: 가까이 있는 파티원도 처치 목표를 함께 채운다
        int r = com.rtshop.party.PartyConfig.get().shareRadius;
        com.rtshop.party.Party.Group g = r > 0 ? com.rtshop.party.Party.of(server, killer.getUUID()) : null;
        if (g != null) {
            for (ServerPlayer m : com.rtshop.party.Party.online(server, g)) {
                if (m != killer && m.level() == victim.level() && m.distanceToSqr(victim) <= (double) r * r) onKill(m, victim);
            }
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
        if (event.player instanceof ServerPlayer sp && sp.tickCount % 10 == 5) {
            syncHud(sp);
        }
    }

    // ------------------------------------------------------------------ 퀘스트 HUD

    /** 플레이어마다 마지막으로 보낸 HUD 내용의 요약 (바뀌었을 때만 다시 보낸다). */
    private static final Map<UUID, String> HUD_SENT = new java.util.concurrent.ConcurrentHashMap<>();

    /** 진행 중인 퀘스트를 받은 순서대로 (HUD 용). */
    public static List<com.rtshop.net.QuestHudPacket.Entry> hudEntries(ServerPlayer p) {
        QuestData.Progress pr = prog(p);
        List<com.rtshop.net.QuestHudPacket.Entry> out = new ArrayList<>();
        for (Map.Entry<String, int[]> e : pr.active.entrySet()) {
            QuestDef q = loaded.quests.get(e.getKey());
            if (q == null) {
                continue;
            }
            boolean ready = allDone(p, q, e.getValue());
            QuestView v = view(p, q, ready ? QuestView.READY : QuestView.ACTIVE, e.getValue());
            out.add(new com.rtshop.net.QuestHudPacket.Entry(q.id, v.title(), ready, com.rtshop.story.StoryText.t(p, npcName(q.turnInNpc())), v.objectives()));
        }
        // 받을 수 있는 퀘스트가 있으면 다음에 찾아갈 NPC 를 알려 준다 (메인 스토리 먼저, 숨은 이야기는 알리지 않는다)
        QuestDef next = null;
        int more = 0;
        for (QuestDef q : loaded.quests.values()) {
            if (q.giver.isEmpty() || q.id.startsWith("hidden") || !QuestDef.modOk(q.mod) || !isAvailable(p, q)) continue;
            if (next == null || nextRank(q) < nextRank(next)) {
                if (next != null) more++;
                next = q;
            } else {
                more++;
            }
        }
        if (next != null) {
            out.add(new com.rtshop.net.QuestHudPacket.Entry(NEXT_ID + more, com.rtshop.story.StoryText.t(p, next.title), false,
                    com.rtshop.story.StoryText.t(p, npcName(next.giver)), List.of()));
        }
        return out;
    }

    /** HUD 의 "다음 퀘스트" 줄 ID (뒤에 그 밖의 받을 수 있는 퀘스트 수). */
    public static final String NEXT_ID = "_next:";

    private static long nextRank(QuestDef q) {
        return (q.id.startsWith("story_") ? 0L : 1L << 40) + q.order;
    }

    public static void syncHud(ServerPlayer p) {
        List<com.rtshop.net.QuestHudPacket.Entry> list = hudEntries(p);
        StringBuilder sig = new StringBuilder();
        for (com.rtshop.net.QuestHudPacket.Entry e : list) {
            sig.append(e.id()).append(e.ready() ? '!' : ':').append(e.title()).append('|').append(e.turnIn());
            for (QuestView.ObjView o : e.objectives()) {
                sig.append(',').append(o.cur()).append('/').append(o.need()).append(o.text());
            }
            sig.append(';');
        }
        String now = sig.toString();
        if (now.equals(HUD_SENT.get(p.getUUID()))) {
            return;
        }
        HUD_SENT.put(p.getUUID(), now);
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new com.rtshop.net.QuestHudPacket(list));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
        HUD_SENT.remove(event.getEntity().getUUID());
    }

    public static void closeSession(ServerPlayer p) {
        SESSIONS.remove(p.getUUID());
    }
}
