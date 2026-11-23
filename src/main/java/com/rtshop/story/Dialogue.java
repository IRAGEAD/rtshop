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
    public static final int DEFAULT_VERSION = 1;
    private static JsonObject config;
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
                if (cur == null || (!edited && v < DEFAULT_VERSION)) {
                    Files.createDirectories(f.getParent());
                    Files.writeString(f, new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(def), StandardCharsets.UTF_8);
                    cur = def;
                }
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

    // ------------------------------------------------------------------ 조건

    /** need: {flags:[], notFlags:[], quests:[] (완료한 퀘스트)} */
    static boolean ok(ServerPlayer p, JsonObject o) {
        if (o == null || !o.has("need")) return true;
        JsonObject n = o.getAsJsonObject("need");
        if (n.has("flags")) for (JsonElement e : n.getAsJsonArray("flags")) if (!QuestManager.hasFlag(p, e.getAsString())) return false;
        if (n.has("notFlags")) for (JsonElement e : n.getAsJsonArray("notFlags")) if (QuestManager.hasFlag(p, e.getAsString())) return false;
        if (n.has("quests")) for (JsonElement e : n.getAsJsonArray("quests")) if (!QuestManager.isCompleted(p, e.getAsString())) return false;
        return true;
    }

    static String text(ServerPlayer p, JsonElement e) {
        return Loc.pick(p, e);
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

    public static void open(ServerPlayer p, String npcKey, int entityId) {
        JsonObject all = get();
        JsonObject npcs = all.has("npcs") ? all.getAsJsonObject("npcs") : new JsonObject();
        JsonObject n = npcs.has(npcKey) ? npcs.getAsJsonObject(npcKey) : new JsonObject();
        JsonObject out = new JsonObject();
        out.addProperty("npc", npcKey);
        out.addProperty("entity", entityId);
        out.addProperty("name", QuestManager.npcName(npcKey));
        JsonArray scene = pendingScene(p, npcKey);
        if (scene != null) {
            out.addProperty("mode", "scene");
            out.add("lines", scene);
        } else {
            out.addProperty("mode", "talk");
        }
        // 인사: 조건이 맞는 것 중 마지막(진행이 더 된 것)을 쓴다
        String greet = "";
        if (n.has("greet")) {
            for (JsonElement e : n.getAsJsonArray("greet")) {
                if (e.isJsonObject() && e.getAsJsonObject().has("text")) {
                    if (ok(p, e.getAsJsonObject())) greet = text(p, e.getAsJsonObject().get("text"));
                } else {
                    if (greet.isEmpty()) greet = text(p, e);
                }
            }
        }
        if (greet.isEmpty()) greet = Component.translatable("rtshop.talk.default_greet").getString();
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
                t.addProperty("title", Component.translatable("rtshop.talk.chatter").getString());
                JsonArray l = new JsonArray();
                l.add(pool.get(RNG.nextInt(pool.size())));
                t.add("lines", l);
                topics.add(t);
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
        out.addProperty("quests", QuestManager.viewsCount(p, npcKey));
        if (com.rtshop.story.Hunts.get().boardNpcs.contains(npcKey)) out.addProperty("bounty", true);
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.DIALOGUE, out.toString()));
    }

    public static String readFlag(String npc, String topic) {
        return "talk_" + npc + "_" + topic;
    }

    /** 대화 장면을 다 봤다: TALK 목표를 채우고 퀘스트 창을 연다. */
    public static void sceneDone(ServerPlayer p, int entityId) {
        Entity e = p.level().getEntity(entityId);
        String key = e == null ? null : QuestManager.npcKey(e);
        if (key == null || !near(p, e)) return;
        QuestManager.onTalk(p, key, true);
        QuestManager.openDialog(p, key, entityId);
    }

    public static void read(ServerPlayer p, String npc, String topic) {
        if (topic.startsWith("_")) return;
        QuestManager.setFlag(p, readFlag(npc, topic), true);
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

    /** 말을 걸어야 하는 장면이 있으면, 상점·퀘스트 창보다 먼저 대화 상자를 연다. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p)) return;
        String key = QuestManager.npcKey(event.getTarget());
        if (key == null || pendingScene(p, key) == null) return;
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
