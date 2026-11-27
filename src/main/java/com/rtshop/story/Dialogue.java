package com.rtshop.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * NPC 대화 (config/rtshop/dialogue.json).
 * <ul>
 * <li>모든 NPC 의 [대화]: 인사 한마디 + 일상 이야기(chatter, 무작위) + 이야기 주제(topics: 세계관·배경, 진행에 따라 열림).</li>
 * <li>퀘스트의 TALK 목표에 lines 가 있으면: 그 NPC 에게 말을 걸 때 대화 상자로 장면이 먼저 나오고, 다 보면 목표가 채워진다.</li>
 * </ul>
 * 문장은 "이름: 대사" 형식이면 말하는 사람이 바뀐다. 문자열 대신 {"ko": "...", "en": "..."} 로 두 언어를 적을 수 있다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Dialogue {
    private Dialogue() {
    }

    /** 기본 파일의 판 (이보다 낮고 직접 고치지 않은 파일은 새 기본값으로 바꾼다). */
    public static final int DEFAULT_VERSION = 4;
    private static JsonObject config;
    /** 직접 고친 대화 파일이라 새 판을 받지 못했는가 (운영자에게 알린다). */
    public static volatile boolean stale;
    private static final Random RNG = new Random();

    static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("dialogue.json");
    }

    static JsonObject bundled() {
        try (InputStream in = Dialogue.class.getResourceAsStream("/rtshop_defaults/dialogue.json")) {
            if (in == null) return new JsonObject();
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static synchronized JsonObject get() {
        if (config == null) {
            Path f = file();
            JsonObject def = bundled();
            try {
                JsonObject cur = Files.exists(f) ? JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject() : null;
                int v = cur != null && cur.has("version") ? cur.get("version").getAsInt() : -1;
                boolean edited = cur != null && cur.has("edited") && cur.get("edited").getAsBoolean();
                int bundledV = def.has("version") ? def.get("version").getAsInt() : DEFAULT_VERSION;
                if (cur != null && !edited && v < bundledV) { // 고치지 않은 예전 기본 대화: 이전 파일을 남기고 새 판으로
                    Path b = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("backup").resolve("dialogue_v" + v + "_" + System.currentTimeMillis() + ".json");
                    Files.createDirectories(b.getParent());
                    Files.copy(f, b, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    RtShop.LOGGER.info("[rtshop] 고치지 않은 예전 기본 대화(판 {})를 새 판({})으로 바꿨습니다 (이전 파일: {}).", v, bundledV, b);
                }
                stale = cur != null && edited && v < bundledV;
                if (cur == null || (!edited && v < bundledV)) {
                    Files.createDirectories(f.getParent());
                    Files.writeString(f, new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(def), StandardCharsets.UTF_8);
                    cur = def;
                }
                if (com.rtshop.util.ConfigNames.file(f)) cur = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                config = cur;
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] dialogue.json 을 읽지 못해 기본 대화를 씁니다: {}", e.toString());
                config = def;
            }
        }
        return config;
    }

    public static synchronized void reload() {
        config = null;
    }

    /** 직접 고친 대화 파일도 새 기본 대화로 바꾼다 (이전 파일은 backup 에). 바꿨으면 true. */
    public static synchronized boolean forceDefault() throws java.io.IOException {
        Path f = file();
        JsonObject def = bundled();
        if (def.size() == 0) return false;
        if (Files.exists(f)) {
            Path b = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("backup").resolve("dialogue_" + System.currentTimeMillis() + ".json");
            Files.createDirectories(b.getParent());
            Files.copy(f, b, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Files.createDirectories(f.getParent());
        Files.writeString(f, new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(def), StandardCharsets.UTF_8);
        config = null;
        stale = false;
        return true;
    }

    // ------------------------------------------------------------------ 조건

    /** need: {flags:[], notFlags:[], quests:[] (완료한 퀘스트), notQuests:[] (아직 끝내지 않은 퀘스트), items:[] (지니고 있는 물건)} */
    static boolean ok(ServerPlayer p, JsonObject o) {
        if (o == null || !o.has("need")) return true;
        JsonObject n = o.getAsJsonObject("need");
        if (n.has("flags")) for (JsonElement e : n.getAsJsonArray("flags")) if (!QuestManager.hasFlag(p, e.getAsString())) return false;
        if (n.has("notFlags")) for (JsonElement e : n.getAsJsonArray("notFlags")) if (QuestManager.hasFlag(p, e.getAsString())) return false;
        if (n.has("quests")) for (JsonElement e : n.getAsJsonArray("quests")) if (!QuestManager.isCompleted(p, e.getAsString())) return false;
        if (n.has("notQuests")) for (JsonElement e : n.getAsJsonArray("notQuests")) if (QuestManager.isCompleted(p, e.getAsString())) return false;
        if (n.has("items")) for (JsonElement e : n.getAsJsonArray("items")) if (!carries(p, e.getAsString())) return false;
        // 3.75.0 (사용자 W-25): 다른 NPC 에 대해 묻는 것은 그 NPC 와 한 번 이야기한 뒤에, 무엇에 대해 묻는 것은 그 이야기를 들은 뒤에
        if (n.has("met")) for (JsonElement e : n.getAsJsonArray("met")) if (!met(p, e.getAsString())) return false;
        if (n.has("heard")) for (JsonElement e : n.getAsJsonArray("heard")) if (!heard(p, e.getAsString())) return false;
        return true;
    }

    public static String metFlag(String npc) {
        return "met_" + npc;
    }

    public static String heardFlag(String term) {
        return "heard_" + term;
    }

    /**
     * 이 NPC 와 한 번이라도 이야기했는가: 대화 상자를 연 적이 있거나(3.75.0 부터 적어 둔다), 일상 이야기를 나눴거나,
     * 그 NPC 의 퀘스트를 끝냈다 (그 전부터 하던 사람도 다시 말을 걸지 않아도 되게).
     */
    public static boolean met(ServerPlayer p, String npc) {
        if (npc == null || npc.isEmpty()) return true;
        if (npc.contains("|")) {
            for (String one : npc.split("\\|")) if (!one.isBlank() && met(p, one.trim())) return true;   // 같은 사람의 여러 이름 (상인 · 이야기 NPC)
            return false;
        }
        if (QuestManager.hasFlag(p, metFlag(npc)) || chats(p, npc) > 0) return true;
        for (QuestDef q : QuestManager.loaded().quests.values()) {
            if ((npc.equals(q.giver) || npc.equals(q.turnInNpc())) && QuestManager.isCompleted(p, q.id)) return true;
        }
        return false;
    }

    /** 그 이야기(terms 의 낱말)를 들었는가: 들은 표시가 있거나, 그 낱말이 나오는 퀘스트(terms.quests)를 끝냈다. */
    public static boolean heard(ServerPlayer p, String term) {
        if (QuestManager.hasFlag(p, heardFlag(term))) return true;
        JsonObject t = terms().has(term) && terms().get(term).isJsonObject() ? terms().getAsJsonObject(term) : null;
        if (t == null) return true;   // 정의가 없는 낱말로 막지는 않는다
        if (t.has("quests")) for (JsonElement e : t.getAsJsonArray("quests")) if (QuestManager.isCompleted(p, e.getAsString())) return true;
        return false;
    }

    /** dialogue.json 의 "terms": 낱말 id → {words: [글에 나오는 말], quests: [그 말이 나오는 퀘스트]} */
    static JsonObject terms() {
        JsonObject all = get();
        return all.has("terms") && all.get("terms").isJsonObject() ? all.getAsJsonObject("terms") : new JsonObject();
    }

    /** 플레이어가 본 글에서 낱말을 찾아 「들었다」 표시를 남긴다 (원문 · 영어 둘 다 본다). 새로 들은 수. */
    public static int hear(ServerPlayer p, JsonElement shown) {
        if (shown == null || shown.isJsonNull()) return 0;
        StringBuilder all = new StringBuilder();
        gather(shown, all);
        String text = all.toString();
        if (text.isEmpty()) return 0;
        int n = 0;
        for (java.util.Map.Entry<String, JsonElement> e : terms().entrySet()) {
            if (!e.getValue().isJsonObject() || QuestManager.hasFlag(p, heardFlag(e.getKey()))) continue;
            JsonObject t = e.getValue().getAsJsonObject();
            if (!t.has("words")) continue;
            for (JsonElement w : t.getAsJsonArray("words")) {
                if (!w.getAsString().isEmpty() && text.contains(w.getAsString())) {
                    QuestManager.setFlag(p, heardFlag(e.getKey()), true);
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    public static int hear(ServerPlayer p, String shown) {
        return shown == null ? 0 : hear(p, new com.google.gson.JsonPrimitive(shown));
    }

    private static void gather(JsonElement e, StringBuilder out) {
        if (e == null || e.isJsonNull()) return;
        if (e.isJsonPrimitive()) {
            out.append(e.getAsString()).append('\n');
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) gather(x, out);
        } else if (e.isJsonObject()) {
            for (java.util.Map.Entry<String, JsonElement> x : e.getAsJsonObject().entrySet()) {
                if (x.getKey().equals("need") || x.getKey().equals("id") || x.getKey().equals("once") || x.getKey().equals("chance")) continue;
                gather(x.getValue(), out);
            }
        }
    }

    /** 이 물건을 지니고 있는가 (가방·양손·갑옷). */
    public static boolean carries(ServerPlayer p, String itemId) {
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        if (rl == null) return false;
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return false;
        return p.getInventory().contains(new net.minecraft.world.item.ItemStack(item));
    }

    public static boolean okForTest(ServerPlayer p, JsonObject o) {
        return ok(p, o);
    }

    static String text(ServerPlayer p, JsonElement e) {
        return Loc.pick(p, e);
    }

    private static boolean hangul(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= 0xAC00 && s.charAt(i) <= 0xD7A3) return true;
        return false;
    }

    private static void collect(ServerPlayer p, String where, JsonElement e, List<String> all, List<String> korean) {
        if (e == null || e.isJsonNull()) return;
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) collect(p, where, x, all, korean);
            return;
        }
        if (e.isJsonObject() && e.getAsJsonObject().has("text")) e = e.getAsJsonObject().get("text");
        String s = StoryText.t(p, text(p, e));
        all.add(s);
        if (hangul(s)) korean.add(where + ": " + (s.length() > 40 ? s.substring(0, 40) : s));
    }

    /**
     * 점검용: 모든 NPC 의 인사 · 일상 이야기 · 흘리는 말 · 이야기 주제(조건과 상관없이 전부)를 이 플레이어의 언어로 풀어 본다.
     * 돌려주는 것: {푼 글의 수, 한글이 남은 글의 목록}. 영어로 하는 사람에게는 목록이 비어야 한다.
     */
    public static Object[] languageAuditForTest(ServerPlayer p) {
        List<String> all = new ArrayList<>(), korean = new ArrayList<>();
        JsonObject npcs = get().has("npcs") ? get().getAsJsonObject("npcs") : new JsonObject();
        for (Map.Entry<String, JsonElement> en : npcs.entrySet()) {
            JsonObject n = en.getValue().getAsJsonObject();
            for (String part : new String[]{"greet", "chatter", "hints"}) if (n.has(part)) collect(p, en.getKey() + " " + part, n.get(part), all, korean);
            if (n.has("topics")) {
                for (JsonElement t : n.getAsJsonArray("topics")) {
                    JsonObject x = t.getAsJsonObject();
                    collect(p, en.getKey() + " topic " + x.get("id").getAsString(), x.get("title"), all, korean);
                    collect(p, en.getKey() + " topic " + x.get("id").getAsString(), x.get("lines"), all, korean);
                }
            }
        }
        return new Object[]{all.size(), korean};
    }

    static JsonArray texts(ServerPlayer p, JsonArray in) {
        JsonArray out = new JsonArray();
        if (in != null) for (JsonElement e : in) out.add(text(p, e));
        return out;
    }

    /** 아직 보지 않은 대화 장면: 진행 중인 퀘스트의 TALK 목표 중 이 NPC 에게 걸린 것 (lines 가 있는 것). */
    public static JsonArray pendingScene(ServerPlayer p, String npcKey) {
        for (QuestDef q : QuestManager.activeQuests(p)) {
            int[] pg = QuestManager.progressOf(p, q.id);
            for (int i = 0; i < q.objectives.size(); i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type == QuestDef.Type.TALK && ob.target.equals(npcKey) && ob.lines != null && ob.lines.size() > 0
                        && pg != null && i < pg.length && pg[i] < ob.count) {
                    return texts(p, ob.lines);
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 열기

    /** 시험용: 0 이상이면 '가끔' 인사의 주사위를 이 값으로 고정한다. */
    public static volatile double forcedRoll = -1;

    /** 처음 한 번만 하는 인사를 이미 했는지 적어 두는 표시. 글이 바뀌면 다시 한 번 한다. */
    public static String greetFlag(String npc, String text) {
        return "greeted_" + npc + "_" + Integer.toHexString(text.hashCode());
    }

    /**
     * 인사 고르기: 조건이 맞는 것 중 마지막(진행이 더 된 것)을 쓴다.
     *   "once": true  → 이 사람에게 처음 한 번만 하는 말. 아직 안 했으면 다른 인사보다 먼저 한다.
     *   "chance": 0.2 → 가끔만 하는 말. 걸렸을 때에만 보통 인사 대신 한다.
     * shown 이 false 면(장면 중이라 인사가 보이지 않으면) 처음 하는 말을 아껴 둔다.
     */
    public static String greet(ServerPlayer p, String npcKey, JsonObject n, boolean shown) {
        String greet = "";
        if (!n.has("greet")) return greet;
        String first = null, firstFlag = null, sometimes = null;
        JsonArray arr = n.getAsJsonArray("greet");
        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);
            if (e.isJsonObject() && e.getAsJsonObject().has("text")) {
                JsonObject g = e.getAsJsonObject();
                if (!ok(p, g)) continue;
                if (g.has("once") && g.get("once").getAsBoolean()) {
                    String flag = greetFlag(npcKey, text(p, g.get("text")));
                    if (!QuestManager.hasFlag(p, flag)) { first = text(p, g.get("text")); firstFlag = flag; }
                    continue;
                }
                if (g.has("chance")) {
                    double roll = forcedRoll >= 0 ? forcedRoll : RNG.nextDouble();
                    if (roll < g.get("chance").getAsDouble()) sometimes = text(p, g.get("text"));
                    continue;
                }
                greet = text(p, g.get("text"));
            } else {
                if (greet.isEmpty()) greet = text(p, e);
            }
        }
        if (first != null) {
            if (shown) QuestManager.setFlag(p, firstFlag, true);
            return first;
        }
        return sometimes != null ? sometimes : greet;
    }

    public static void open(ServerPlayer p, String npcKey, int entityId) {
        JsonObject out = build(p, npcKey, entityId);
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.DIALOGUE, StoryText.json(p, out).toString()));
    }

    /** 점검용: buildForTest 로 연 대화도 「이야기했다」 로 친다. */
    public static boolean testMet;

    public static JsonObject buildForTest(ServerPlayer p, String npcKey) {
        return build(p, npcKey, -1);
    }

    /**
     * 3.77.0: 이야기 속에서 그 사람이 자리를 비운 때 (이 사람에게만) — dialogue.json 의 NPC 마다 "absent": [{"text", "need"}].
     * 맞으면 그 글(빈자리의 모습)을 돌려준다, 아니면 null. 그동안 그 NPC 는 이 사람의 화면에서 보이지 않고(NpcLooks), 말을 걸면 그 글만 보인다.
     */
    public static String absentText(ServerPlayer p, String npcKey) {
        JsonObject a = absentEntry(p, npcKey);
        return a == null ? null : text(p, a.get("text"));
    }

    /** 그 사람의 화면에서 이 NPC 를 그리지 않는가 ("stay": true 는 곁에 있되 말이 없는 때 — 보이고, 그 한 마디만). */
    public static boolean hiddenFor(ServerPlayer p, String npcKey) {
        JsonObject a = absentEntry(p, npcKey);
        return a != null && !(a.has("stay") && a.get("stay").getAsBoolean());
    }

    private static JsonObject absentEntry(ServerPlayer p, String npcKey) {
        if (p == null || npcKey == null) return null;
        try {
            JsonObject all = get();
            if (!all.has("npcs") || !all.getAsJsonObject("npcs").has(npcKey)) return null;
            JsonObject n = all.getAsJsonObject("npcs").getAsJsonObject(npcKey);
            if (!n.has("absent") || !n.get("absent").isJsonArray()) return null;
            for (JsonElement e : n.getAsJsonArray("absent")) {
                if (e.isJsonObject() && e.getAsJsonObject().has("text") && ok(p, e.getAsJsonObject())) return e.getAsJsonObject();
            }
        } catch (Exception ex) {
            RtShop.LOGGER.debug("[rtshop] absent", ex);
        }
        return null;
    }

    static JsonObject build(ServerPlayer p, String npcKey, int entityId) {
        JsonObject all = get();
        JsonObject npcs = all.has("npcs") ? all.getAsJsonObject("npcs") : new JsonObject();
        JsonObject n = npcs.has(npcKey) ? npcs.getAsJsonObject(npcKey) : new JsonObject();
        JsonObject out = new JsonObject();
        out.addProperty("npc", npcKey);
        out.addProperty("entity", entityId);
        out.addProperty("name", QuestManager.npcName(npcKey));
        String away = absentText(p, npcKey);
        if (away != null) {   // 자리를 비웠다: 빈자리의 모습만 (장면 · 이야기 · 퀘스트 없음, 만난 것으로도 치지 않는다)
            out.addProperty("mode", "talk");
            out.addProperty("greet", away);
            out.add("topics", new JsonArray());
            out.addProperty("absent", true);
            return out;
        }
        JsonArray scene = pendingScene(p, npcKey);
        if (scene != null) {
            out.addProperty("mode", "scene");
            out.add("lines", scene);
        } else {
            out.addProperty("mode", "talk");
        }
        String greet = greet(p, npcKey, n, scene == null);
        if (entityId != -1 || testMet) QuestManager.setFlag(p, metFlag(npcKey), true);   // 3.75.0: 이 NPC 와 이야기했다
        if (entityId != -1) HiddenJobs.talkedTo(p, npcKey);   // 3.76.0: 비 오는 날의 기록관 (조사관의 숨은 길)
        hear(p, greet);
        // 3.64: 서버가 제 언어로 풀어 보내던 글(서버의 언어는 플레이어의 언어와 다를 수 있다)을 그 사람의 이야기 언어로 고른다
        if (greet.isEmpty()) greet = StoryText.pick(p, "무슨 일이세요?", "What can I do for you?");
        out.addProperty("greet", greet);
        JsonArray topics = new JsonArray();
        // 일상 이야기: 무작위 한 마디
        if (n.has("chatter")) {
            List<String> pool = new ArrayList<>();
            for (JsonElement e : n.getAsJsonArray("chatter")) {
                if (e.isJsonObject() && e.getAsJsonObject().has("text")) {
                    if (ok(p, e.getAsJsonObject())) pool.add(text(p, e.getAsJsonObject().get("text")));
                } else pool.add(text(p, e));
            }
            if (!pool.isEmpty()) {
                JsonObject t = new JsonObject();
                t.addProperty("id", "_chatter");
                t.addProperty("title", StoryText.pick(p, "요즘 어떠세요?", "How are things?"));
                JsonArray l = new JsonArray();
                l.add(pool.get(RNG.nextInt(pool.size())));
                t.add("lines", l);
                // 누를 때마다 다른 말을 하도록 전체 목록도 보낸다
                JsonArray every = new JsonArray();
                pool.forEach(every::add);
                t.add("pool", every);
                topics.add(t);
            }
        }
        // 오래 이야기하면 흘리는 말 (히든 단서 쪽으로 살짝)
        if (n.has("hints")) {
            JsonArray hs = new JsonArray();
            for (JsonElement e : n.getAsJsonArray("hints")) {
                if (e.isJsonObject() && e.getAsJsonObject().has("text")) {
                    if (ok(p, e.getAsJsonObject())) hs.add(text(p, e.getAsJsonObject().get("text")));
                } else hs.add(text(p, e));
            }
            if (hs.size() > 0) {
                out.add("hints", hs);
                out.addProperty("hintAfter", HINT_AFTER);
            }
        }
        if (n.has("topics")) {
            for (JsonElement e : n.getAsJsonArray("topics")) {
                JsonObject x = e.getAsJsonObject();
                if (!ok(p, x)) continue;
                JsonObject t = new JsonObject();
                String id = x.get("id").getAsString();
                t.addProperty("id", id);
                t.addProperty("title", text(p, x.get("title")));
                t.add("lines", texts(p, x.getAsJsonArray("lines")));
                t.addProperty("new", !QuestManager.hasFlag(p, readFlag(npcKey, id)));
                topics.add(t);
            }
        }
        out.add("topics", topics);
        out.addProperty("chats", chats(p, npcKey));
        String main = mainAction(npcKey);
        if (main != null) out.addProperty("main", main);
        out.addProperty("quests", QuestManager.viewsCount(p, npcKey));
        // 퀘스트를 주는 NPC 는 받을 퀘스트가 없어도 [퀘스트] 를 보인다 (진행 중·지난 퀘스트·다음 안내)
        boolean service = Hunts.NPC_BOARD.equals(npcKey) || com.rtshop.rank.Ranks.NPC_RANKS.equals(npcKey) || com.rtshop.rank.Ranks.NPC_TITLES.equals(npcKey);
        if (service) out.addProperty("service", true);   // 맡은 일이 따로 있는 NPC: 줄 퀘스트가 없으면 [퀘스트] 를 보이지 않는다
        if ((npcKey.startsWith("quest_") && !service) || QuestManager.hasQuests(npcKey)) out.addProperty("questNpc", true);
        if (com.rtshop.petition.Petitions.CLERK.equals(npcKey)) out.addProperty("petition", true);   // 시청 서기: 민원 창구 (3.61)
        if (com.rtshop.stock.Stocks.DESK.equals(npcKey) && com.rtshop.stock.StockConfig.get().enabled) out.addProperty("stocks", true);   // 은행원: 증권 창구 (3.72)
        if (com.rtshop.story.Hunts.isBoardKey(npcKey)) out.addProperty("bounty", true);
        if (com.rtshop.rank.Ranks.get().enabled) {
            if (com.rtshop.rank.Ranks.NPC_RANKS.equals(npcKey)) out.addProperty("ranks", true);
            if (com.rtshop.rank.Ranks.NPC_TITLES.equals(npcKey)) out.addProperty("titles", true);
        }
        return out;
    }

    /** 이 횟수만큼 일상 이야기를 나누면 가끔 흘리는 말이 섞인다. */
    public static final int HINT_AFTER = 12;

    /** NPC 의 본래 기능 (대화 상자의 첫 선택지): 번역 키, 없으면 null (이야기 전용 NPC). */
    public static String mainAction(String npcKey) {
        if (npcKey == null) return null;
        if (npcKey.startsWith("shop_")) return "rtshop.talk.main.shop";
        return switch (npcKey) {
            case "bank" -> "rtshop.talk.main.bank";
            case "job" -> "rtshop.talk.main.job";
            case "attendance" -> "rtshop.talk.main.attendance";
            case "gacha" -> "rtshop.talk.main.gacha";
            default -> null;
        };
    }

    public static int chats(ServerPlayer p, String npc) {
        return p.getPersistentData().getCompound("rtshop_chats").getInt(npc);
    }

    /** 일상 이야기를 한 번 더 나눴다. */
    public static void chatted(ServerPlayer p, String npc) {
        net.minecraft.nbt.CompoundTag root = p.getPersistentData();
        net.minecraft.nbt.CompoundTag c = root.getCompound("rtshop_chats");
        c.putInt(npc, Math.min(100000, c.getInt(npc) + 1));
        root.put("rtshop_chats", c);
    }

    /** 이 사람은 지금 NPC 의 본래 기능으로 바로 간다 (대화 상자에서 고른 것). */
    private static final java.util.Set<java.util.UUID> BYPASS = new java.util.HashSet<>();

    /** 대화 상자에서 [상점] 등을 골랐다: 그 NPC 를 우클릭한 것처럼 본래 창을 연다. */
    public static boolean main(ServerPlayer p, int entityId) {
        Entity e = p.level().getEntity(entityId);
        String key = e == null ? null : QuestManager.npcKey(e);
        if (key == null || !near(p, e) || mainAction(key) == null) return false;
        BYPASS.add(p.getUUID());
        try {
            PlayerInteractEvent.EntityInteract ev = new PlayerInteractEvent.EntityInteract(p, InteractionHand.MAIN_HAND, e);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(ev);
            return ev.isCanceled();
        } finally {
            BYPASS.remove(p.getUUID());
        }
    }

    public static String readFlag(String npc, String topic) {
        return "talk_" + npc + "_" + topic;
    }

    /** 대화 장면을 다 봤다: TALK 목표를 채우고 퀘스트 창을 연다. */
    public static void sceneDone(ServerPlayer p, int entityId) {
        Entity e = p.level().getEntity(entityId);
        String key = e == null ? null : QuestManager.npcKey(e);
        if (key == null || !near(p, e)) return;
        hear(p, pendingScene(p, key));   // 3.75.0: 장면에서 들은 이야기
        QuestManager.setFlag(p, metFlag(key), true);
        QuestManager.onTalk(p, key, true);
        QuestManager.openDialog(p, key, entityId);
    }

    public static void read(ServerPlayer p, String npc, String topic) {
        if (topic.startsWith("_")) return;
        QuestManager.setFlag(p, readFlag(npc, topic), true);
        try {
            JsonObject all = get();
            JsonObject n = all.has("npcs") && all.getAsJsonObject("npcs").has(npc) ? all.getAsJsonObject("npcs").getAsJsonObject(npc) : null;
            if (n != null && n.has("topics")) {
                for (JsonElement e : n.getAsJsonArray("topics")) {
                    JsonObject x = e.getAsJsonObject();
                    if (topic.equals(x.get("id").getAsString())) hear(p, x.get("lines"));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean openFor(ServerPlayer p, int entityId) {
        Entity e = p.level().getEntity(entityId);
        String key = e == null ? null : QuestManager.npcKey(e);
        if (key == null || !near(p, e)) return false;
        open(p, key, entityId);
        return true;
    }

    static boolean near(ServerPlayer p, Entity e) {
        double r = EconomyConfig.get().npcRange + 2;
        return e.isAlive() && e.distanceToSqr(p) <= r * r;
    }

    /** 점검용·설정: false 면 장면이 있을 때만 대화 상자를 먼저 연다 (예전 방식). */
    public static boolean dialogueFirst = true;

    /**
     * NPC 를 우클릭하면 상점·은행 등의 창보다 먼저 대화 상자를 연다 (말을 걸어야 하는 장면이 있으면 그 장면부터).
     * 대화 상자에서 [상점] 등을 고르면 본래 창으로 간다. 웅크리고 우클릭하면 대화 없이 본래 창을 바로 연다.
     */
    /** 이 NPC 를 누르면 대화 상자가 먼저 열리는가 (그러면 다른 창은 열지 않는다). */
    public static boolean claims(ServerPlayer p, String key) {
        if (p == null || key == null || BYPASS.contains(p.getUUID())) return false;
        if (absentText(p, key) != null) return true;   // 3.77: 자리를 비운 사람은 웅크려 눌러도 퀘스트 창을 열지 않는다
        return pendingScene(p, key) != null || (dialogueFirst && !p.isShiftKeyDown());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p)) return;
        if (BYPASS.contains(p.getUUID())) return;
        String key = QuestManager.npcKey(event.getTarget());
        if (key == null) return;
        if (!claims(p, key)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND) open(p, key, event.getTarget().getId());
    }

    /** 점검용: 이 NPC 의 대화 정보. */
    public static JsonObject npcForTest(String key) {
        JsonObject n = get().has("npcs") ? get().getAsJsonObject("npcs") : new JsonObject();
        return n.has(key) ? n.getAsJsonObject(key) : null;
    }

    public static int npcCountForTest() {
        JsonObject n = get().has("npcs") ? get().getAsJsonObject("npcs") : new JsonObject();
        int c = 0;
        for (Map.Entry<String, JsonElement> e : n.entrySet()) c++;
        return c;
    }
}
