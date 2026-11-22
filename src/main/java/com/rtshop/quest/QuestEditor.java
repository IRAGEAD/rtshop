package com.rtshop.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.net.QuestEditorPacket;
import com.rtshop.net.ResultPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 게임 안 퀘스트 편집기의 서버 쪽 (OP 전용).
 * 퀘스트·NPC 를 원래 들어 있던 파일에 그대로 고쳐 쓰고, 새로 만든 것은 my_quests.json 에 넣는다.
 * 파일 안의 다른 내용(_help 설명, 다른 퀘스트의 순서)은 건드리지 않는다.
 */
public final class QuestEditor {
    private QuestEditor() {
    }

    public static final int PERMISSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String CUSTOM_HELP = "게임 안 퀘스트 편집기(/rtshop quest edit)에서 새로 만든 퀘스트와 NPC 가 저장되는 파일입니다. 메모장으로 고쳐도 됩니다. 모드 업데이트로 바뀌지 않습니다.";

    /** 결과: ok = 저장/삭제가 되었는가, message = 사용자에게 보일 설명. */
    public record Result(boolean ok, String message) {
    }

    public static boolean allowed(ServerPlayer p) {
        return p.hasPermissions(PERMISSION);
    }

    // ------------------------------------------------------------------ 편집기 열기

    public static void open(ServerPlayer p) {
        if (!allowed(p)) {
            p.sendSystemMessage(Component.translatable("rtshop.qedit.no_permission"));
            return;
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), snapshot(p.server));
    }

    /** 편집 화면에 보낼 현재 상태: 파일에 적힌 그대로의 퀘스트·NPC (지금 쓰이지 않는 것도 포함). */
    public static QuestEditorPacket snapshot(MinecraftServer server) {
        QuestManager.Loaded l = QuestManager.loaded();
        List<QuestEditorPacket.Entry> quests = new ArrayList<>();
        List<QuestEditorPacket.Entry> npcs = new ArrayList<>();
        try (Stream<Path> files = Files.list(QuestManager.dir())) {
            for (Path f : files.filter(x -> x.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String name = f.getFileName().toString();
                JsonObject root;
                try {
                    root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                } catch (Exception e) {
                    quests.add(new QuestEditorPacket.Entry("", name, "", "파일을 읽을 수 없습니다: " + e.getMessage()));
                    continue;
                }
                for (JsonElement e : array(root, "quests")) {
                    String id = idOf(e);
                    String status = "";
                    if (!e.isJsonObject()) {
                        status = "퀘스트 형식이 아닙니다";
                    } else {
                        try {
                            QuestDef.parse(e.getAsJsonObject());
                            if (l.dropped.containsKey(id)) {
                                status = l.dropped.get(id);
                            } else if (!l.quests.containsKey(id)) {
                                status = "다른 파일에 같은 ID 가 있어서 쓰이지 않습니다";
                            } else if (!name.equals(l.questFile.get(id))) {
                                status = "다른 파일(" + l.questFile.get(id) + ")에 같은 ID 가 있어서 쓰이지 않습니다";
                            }
                        } catch (Exception ex) {
                            status = ex.getMessage();
                        }
                    }
                    quests.add(new QuestEditorPacket.Entry(id, name, GSON.toJson(e), status));
                }
                for (JsonElement e : array(root, "npcs")) {
                    npcs.add(new QuestEditorPacket.Entry(idOf(e), name, GSON.toJson(e), ""));
                }
            }
        } catch (IOException e) {
            RtShop.LOGGER.error("[rtshop] 퀘스트 폴더를 읽지 못했습니다", e);
        }
        List<String[]> keys = new ArrayList<>();
        keys.add(new String[]{"bank", EconomyConfig.get().bankNpcName});
        keys.add(new String[]{"job", com.rtshop.job.JobConfig.get().npcName});
        keys.add(new String[]{"attendance", com.rtshop.attendance.AttendanceConfig.get().npcName});
        keys.add(new String[]{"gacha", com.rtshop.gacha.GachaConfig.get().npcName});
        for (ShopData d : ShopServer.shops().values()) {
            keys.add(new String[]{"shop_" + d.id, d.npcName});
        }
        for (QuestManager.NpcDef n : l.npcs.values()) {
            keys.add(new String[]{"quest_" + n.id, n.name});
        }
        return new QuestEditorPacket(quests, npcs, keys, new ArrayList<>(l.problems));
    }

    private static Iterable<JsonElement> array(JsonObject root, String key) {
        if (root.has(key) && root.get(key).isJsonArray()) {
            return root.getAsJsonArray(key);
        }
        return new JsonArray();
    }

    private static String idOf(JsonElement e) {
        if (e.isJsonObject() && e.getAsJsonObject().has("id") && e.getAsJsonObject().get("id").isJsonPrimitive()) {
            return e.getAsJsonObject().get("id").getAsString();
        }
        return "";
    }

    // ------------------------------------------------------------------ 파일 쓰기

    private static JsonObject readRoot(Path f, String help) throws IOException {
        if (Files.exists(f)) {
            try {
                return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                throw new IOException(f.getFileName() + " 파일이 깨져 있어서 고쳐 쓸 수 없습니다. 메모장으로 열어 고치거나 지워 주세요. (" + e.getMessage() + ")");
            }
        }
        JsonObject o = new JsonObject();
        o.addProperty("_help", help);
        return o;
    }

    private static void writeRoot(Path f, JsonObject root) throws IOException {
        Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** root[key] 배열에서 id 가 같은 항목을 바꾸거나(없으면 끝에 붙임), value 가 null 이면 지운다. 바뀌었으면 true. */
    private static boolean replace(JsonObject root, String key, String id, JsonObject value) {
        JsonArray arr = root.has(key) && root.get(key).isJsonArray() ? root.getAsJsonArray(key) : new JsonArray();
        root.add(key, arr);
        for (int i = 0; i < arr.size(); i++) {
            if (id.equals(idOf(arr.get(i)))) {
                if (value == null) {
                    arr.remove(i);
                } else {
                    arr.set(i, value);
                }
                return true;
            }
        }
        if (value != null) {
            arr.add(value);
            return true;
        }
        return false;
    }

    /** 이 ID 의 퀘스트(또는 NPC)가 적힌 파일. 없으면 null. 쓰이지 않는 것까지 파일을 직접 뒤진다. */
    private static String findFile(String key, String id) throws IOException {
        try (Stream<Path> files = Files.list(QuestManager.dir())) {
            for (Path f : files.filter(x -> x.getFileName().toString().endsWith(".json")).sorted().toList()) {
                try {
                    JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (JsonElement e : array(root, key)) {
                        if (id.equals(idOf(e))) {
                            return f.getFileName().toString();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 퀘스트

    /**
     * 퀘스트 저장. origId 가 비어 있으면 새 퀘스트, 아니면 그 퀘스트를 고친다 (ID 는 바꿀 수 없다).
     * 저장은 되었지만 조건 문제로 쓰이지 않으면 ok=true 와 함께 그 이유를 알린다.
     */
    public static synchronized Result saveQuest(String origId, String json) {
        QuestDef q;
        try {
            q = QuestDef.parse(JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception e) {
            return new Result(false, "저장하지 못했습니다: " + e.getMessage());
        }
        boolean isNew = origId == null || origId.isEmpty();
        // 편집 화면에 없는 값(연출)은 원래 퀘스트에서 그대로 가져간다
        QuestDef old = isNew ? null : QuestManager.loaded().quests.get(origId);
        if (old != null) {
            if (q.sceneAccept == null) q.sceneAccept = old.sceneAccept;
            if (q.sceneDone == null) q.sceneDone = old.sceneDone;
        }
        try {
            String file = findFile("quests", q.id);
            if (isNew && file != null) {
                return new Result(false, "ID '" + q.id + "' 는 이미 있습니다 (" + file + "). 다른 ID 를 써 주세요.");
            }
            if (!isNew && !origId.equals(q.id)) {
                return new Result(false, "이미 있는 퀘스트의 ID 는 바꿀 수 없습니다 (플레이어 진행 기록이 ID 에 묶여 있습니다).");
            }
            if (file == null) {
                file = QuestManager.CUSTOM_FILE;
            }
            Path f = QuestManager.dir().resolve(file);
            JsonObject root = readRoot(f, CUSTOM_HELP);
            replace(root, "quests", q.id, q.toJson());
            writeRoot(f, root);
            QuestManager.Loaded l = QuestManager.reload();
            if (!l.quests.containsKey(q.id)) {
                String why = l.dropped.getOrDefault(q.id, "다른 파일에 같은 ID 가 있습니다");
                return new Result(true, "저장했지만 지금은 쓰이지 않습니다: " + why);
            }
            return new Result(true, "'" + q.title + "' 을(를) 저장했습니다 (" + file + "). 바로 적용됩니다.");
        } catch (IOException e) {
            return new Result(false, "저장하지 못했습니다: " + e.getMessage());
        }
    }

    /** 퀘스트 삭제. 다른 퀘스트가 선행 조건으로 쓰고 있으면 거절한다. */
    public static synchronized Result deleteQuest(String id) {
        List<String> users = new ArrayList<>();
        for (QuestDef o : QuestManager.loaded().quests.values()) {
            if (o.requires.contains(id)) {
                users.add(o.id);
            }
        }
        if (!users.isEmpty()) {
            return new Result(false, "다음 퀘스트가 이 퀘스트를 선행 조건으로 쓰고 있어서 지울 수 없습니다: " + String.join(", ", users));
        }
        try {
            String file = findFile("quests", id);
            if (file == null) {
                return new Result(false, "퀘스트 '" + id + "' 를 찾지 못했습니다.");
            }
            Path f = QuestManager.dir().resolve(file);
            JsonObject root = readRoot(f, CUSTOM_HELP);
            replace(root, "quests", id, null);
            writeRoot(f, root);
            QuestManager.reload();
            return new Result(true, "퀘스트 '" + id + "' 를 지웠습니다. 이미 받은 플레이어의 진행 기록은 무시됩니다.");
        } catch (IOException e) {
            return new Result(false, "지우지 못했습니다: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ NPC

    public static synchronized Result saveNpc(MinecraftServer server, String origId, String json) {
        JsonObject o;
        String id;
        String name;
        try {
            o = JsonParser.parseString(json).getAsJsonObject();
            id = o.get("id").getAsString().trim();
            name = o.has("name") ? o.get("name").getAsString().trim() : "";
        } catch (Exception e) {
            return new Result(false, "저장하지 못했습니다: NPC 정보가 올바르지 않습니다.");
        }
        if (!id.matches("[a-z0-9_.-]+")) {
            return new Result(false, "NPC ID 는 영문 소문자·숫자·_ . - 만 쓸 수 있습니다: '" + id + "'");
        }
        if (name.isEmpty()) {
            return new Result(false, "NPC 이름을 적어 주세요.");
        }
        JsonObject clean = new JsonObject();
        clean.addProperty("id", id);
        clean.addProperty("name", name);
        clean.addProperty("profession", o.has("profession") ? o.get("profession").getAsString() : "librarian");
        clean.addProperty("greeting", o.has("greeting") ? o.get("greeting").getAsString() : "");
        boolean isNew = origId == null || origId.isEmpty();
        try {
            String file = findFile("npcs", id);
            if (isNew && file != null) {
                return new Result(false, "NPC ID '" + id + "' 는 이미 있습니다. 다른 ID 를 써 주세요.");
            }
            if (!isNew && !origId.equals(id)) {
                return new Result(false, "이미 있는 NPC 의 ID 는 바꿀 수 없습니다 (소환된 NPC 와 퀘스트가 ID 로 연결되어 있습니다).");
            }
            if (file == null) {
                file = QuestManager.CUSTOM_FILE;
            }
            Path f = QuestManager.dir().resolve(file);
            JsonObject root = readRoot(f, CUSTOM_HELP);
            replace(root, "npcs", id, clean);
            writeRoot(f, root);
            QuestManager.reload();
            int renamed = server == null ? 0 : renameSpawned(server, id, name);
            String extra = renamed > 0 ? " 소환되어 있는 NPC " + renamed + "명의 이름도 바꿨습니다." : "";
            return new Result(true, "NPC '" + name + "' 을(를) 저장했습니다." + extra);
        } catch (IOException e) {
            return new Result(false, "저장하지 못했습니다: " + e.getMessage());
        }
    }

    /** 불러와져 있는 구역의 해당 NPC 이름을 새 이름으로 바꾼다. */
    private static int renameSpawned(MinecraftServer server, String id, String name) {
        int n = 0;
        String tag = QuestManager.QUEST_TAG_PREFIX + id;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e.getTags().contains(tag)) {
                    e.setCustomName(Component.literal(name));
                    n++;
                }
            }
        }
        return n;
    }

    public static synchronized Result deleteNpc(String id) {
        String key = "quest_" + id;
        List<String> users = new ArrayList<>();
        for (QuestDef q : QuestManager.loaded().quests.values()) {
            boolean uses = q.giver.equals(key) || q.turnInNpc().equals(key);
            for (QuestDef.Objective ob : q.objectives) {
                if (ob.type == QuestDef.Type.TALK && ob.target.equals(key)) {
                    uses = true;
                }
            }
            if (uses) {
                users.add(q.id);
            }
        }
        if (!users.isEmpty()) {
            return new Result(false, "다음 퀘스트가 이 NPC 를 쓰고 있어서 지울 수 없습니다: " + String.join(", ", users));
        }
        try {
            String file = findFile("npcs", id);
            if (file == null) {
                return new Result(false, "NPC '" + id + "' 를 찾지 못했습니다.");
            }
            Path f = QuestManager.dir().resolve(file);
            JsonObject root = readRoot(f, CUSTOM_HELP);
            replace(root, "npcs", id, null);
            writeRoot(f, root);
            QuestManager.reload();
            return new Result(true, "NPC '" + id + "' 를 지웠습니다. 이미 소환된 NPC 는 크리에이티브에서 직접 없애 주세요.");
        } catch (IOException e) {
            return new Result(false, "지우지 못했습니다: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ 화면에서 온 요청

    public static final int SAVE_QUEST = 0;
    public static final int DELETE_QUEST = 1;
    public static final int SAVE_NPC = 2;
    public static final int DELETE_NPC = 3;

    public static void handle(ServerPlayer p, int action, String origId, String json) {
        if (!allowed(p)) {
            p.sendSystemMessage(Component.translatable("rtshop.qedit.no_permission"));
            return;
        }
        Result r = switch (action) {
            case SAVE_QUEST -> saveQuest(origId, json);
            case DELETE_QUEST -> deleteQuest(origId);
            case SAVE_NPC -> saveNpc(p.server, origId, json);
            case DELETE_NPC -> deleteNpc(origId);
            default -> new Result(false, "?");
        };
        RtShop.LOGGER.info("[rtshop] 퀘스트 편집 ({}): {}", p.getGameProfile().getName(), r.message());
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), snapshot(p.server));
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ResultPacket(Component.literal(r.message()), r.ok()));
    }
}
