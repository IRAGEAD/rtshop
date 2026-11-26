package com.rtshop.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 지도 표시를 지도 모드(Diorama, MIT)의 웨이포인트로 넣는다 (3.54). Diorama 가 없으면 아무 일도 하지 않는다.
 * <p>Diorama 에는 다른 모드가 쓰라고 내놓은 API 가 없어, 웨이포인트 저장소({@code dev.diorama.waypoint.WaypointStore})를
 * 이름으로 찾아 쓴다 (리플렉션). 판이 바뀌어 모양이 달라지면 조용히 그만두고 로그에 한 번만 적는다.</p>
 * <p>플레이어가 지도에서 직접 지운 표시를 다시 넣지 않으려고, 넣은 것을 config/rtshop/map_marks_client.json 에 세계별로 적어 둔다.
 * /rtshop mapmark resend 는 그 기록을 무시하고 다시 넣는다.</p>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class MapMarkClient {
    private MapMarkClient() {
    }

    private static final Deque<JsonObject> PENDING = new ArrayDeque<>();
    private static int wait, tries;
    private static boolean broken;
    /** 점검용: 지금까지 지도에 넣은 수 · 고친 수 · 뺀 수, 마지막 오류. */
    public static int addedForTest, movedForTest, removedForTest;
    public static String errorForTest = "";

    public static boolean available() {
        return !broken && ModList.get().isLoaded("diorama");
    }

    /** 서버가 알려 준 RT Shop 웨이포인트 폴더. 세계 안에서는 이 폴더의 표시를 그리지 않는다 (M 지도에서만 보인다). */
    private static volatile String townFolder = "베스탈리아";
    /** 서버 설정(map_marks.json 의 showInWorld)이 켜져 있으면 세계 안에도 그대로 둔다. */
    private static volatile boolean showInWorld;
    /** 점검용: 세계 안 표시에서 뺀 횟수 (그릴 때마다 는다). */
    public static volatile int hiddenInWorldForTest;

    /** 말을 걸어 본 NPC (서버가 알려 준 지도 표시 "npc:<UUID>"). 미니맵의 몹 아이콘은 이 NPC 들만 보인다. */
    private static final java.util.Set<java.util.UUID> KNOWN_NPCS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** 서버가 "말을 걸어 본 NPC 만" 을 켜 두었는가 (지도 표시가 꺼진 서버에서는 가리지 않는다). */
    private static volatile boolean radarKnownOnly;
    /** 점검용: 미니맵 아이콘에서 감춘 횟수. */
    public static volatile int hiddenOnRadarForTest;

    /** RT Shop NPC 로 보이는가 (화면 쪽에서는 꼬리표를 알 수 없다): 움직이지 않게 해 둔, 이름 붙은 주민. */
    public static boolean looksLikeNpc(net.minecraft.world.entity.Entity e) {
        return e instanceof net.minecraft.world.entity.npc.Villager v && v.isNoAi() && v.hasCustomName();
    }

    /** 미니맵·지도의 몹 아이콘에서 이 개체를 감출 것인가 (DioramaRadarMixin 이 부른다). */
    public static boolean hideOnRadar(net.minecraft.world.entity.Entity e) {
        if (!looksLikeNpc(e)) return false;
        boolean hide = !ClientConfig.get().minimapNpcIcons || (radarKnownOnly && !KNOWN_NPCS.contains(e.getUUID()));
        if (hide) hiddenOnRadarForTest++;
        return hide;
    }

    public static void learnForTest(String json) {
        learn(JsonParser.parseString(json).getAsJsonObject());
    }

    public static int knownNpcsForTest() {
        return KNOWN_NPCS.size();
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        KNOWN_NPCS.clear();
        radarKnownOnly = false;
    }

    private static void learn(JsonObject o) {
        radarKnownOnly = o.has("radar") && o.get("radar").getAsBoolean();
        if (o.has("marks")) {
            for (JsonElement e : o.getAsJsonArray("marks")) {
                String id = e.getAsJsonObject().get("id").getAsString();
                if (id.startsWith("npc:")) KNOWN_NPCS.add(java.util.UUID.fromString(id.substring(4)));
            }
        }
        if (o.has("gone")) {
            for (JsonElement e : o.getAsJsonArray("gone")) {
                String id = e.getAsString();
                if (id.startsWith("npc:")) KNOWN_NPCS.remove(java.util.UUID.fromString(id.substring(4)));
            }
        }
    }

    /** Diorama 의 세계 안 웨이포인트 표시가 이 폴더의 것을 그리려 할 때: 감출 것이면 true (DioramaWorldPinsMixin 이 부른다). */
    public static boolean hideInWorld(String folder) {
        if (showInWorld || folder == null) return false;
        String mine = townFolder;
        if (mine.isEmpty() || !mine.equalsIgnoreCase(folder.trim())) return false;
        hiddenInWorldForTest++;
        return true;
    }

    public static void receive(JsonObject o) {
        try {
            if (o.has("folder") && !o.get("folder").getAsString().isBlank()) townFolder = o.get("folder").getAsString().trim();
            showInWorld = o.has("world") && o.get("world").getAsBoolean();
        } catch (Exception ignored) {
        }
        try {
            learn(o);
        } catch (Exception ignored) {
        }
        if (!available()) return;
        synchronized (PENDING) {
            PENDING.add(o);
            if (PENDING.size() > 64) PENDING.removeFirst();
        }
        tries = 0;
        wait = 0;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            synchronized (PENDING) {
                PENDING.clear();   // 세계를 나갔다: 다음에 들어올 때 서버가 다시 보낸다
            }
            return;
        }
        if (wait-- > 0) return;
        try {
            Object store = store();
            if (store == null) {          // 지도 모드가 아직 이 세계를 열지 않았다: 조금 뒤에 다시
                wait = 20;
                if (++tries > 60) {
                    synchronized (PENDING) {
                        PENDING.clear();
                    }
                }
                return;
            }
            JsonObject o;
            while (true) {
                synchronized (PENDING) {
                    o = PENDING.pollFirst();
                }
                if (o == null) break;
                apply(store, o);
            }
        } catch (Throwable t) {
            broken = true;
            errorForTest = t.toString();
            synchronized (PENDING) {
                PENDING.clear();
            }
            RtShop.LOGGER.warn("[rtshop] 지도 모드(Diorama)에 표시를 넣지 못했습니다. 이 판의 Diorama 와는 맞지 않는 것 같습니다: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ Diorama (리플렉션)

    private static Class<?> storeClass, pointClass;

    private static Object store() throws Exception {
        if (storeClass == null) {
            storeClass = Class.forName("dev.diorama.waypoint.WaypointStore");
            pointClass = Class.forName("dev.diorama.waypoint.Waypoint");
        }
        return storeClass.getMethod("get").invoke(null);
    }

    private static String worldKey() {
        try {
            Object k = Class.forName("dev.diorama.WorldKey").getMethod("current").invoke(null);
            if (k != null) return k.toString();
        } catch (Throwable ignored) {
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.getCurrentServer() != null ? "server:" + mc.getCurrentServer().ip : "local";
    }

    private static Object field(Object wp, String name) throws Exception {
        Field f = pointClass.getField(name);
        return f.get(wp);
    }

    private static void set(Object wp, String name, Object value) throws Exception {
        pointClass.getField(name).set(wp, value);
    }

    /** 이 폴더에서 이름과 자리가 같은 웨이포인트를 찾는다. */
    private static Object find(Object store, String folder, String name, String dim, int x, int y, int z) throws Exception {
        for (Object wp : (List<?>) storeClass.getMethod("all").invoke(store)) {
            Object f = field(wp, "folder");
            if (f == null || !folder.equalsIgnoreCase(f.toString().trim())) continue;
            if (!name.equals(field(wp, "name")) || !dim.equals(field(wp, "dimension"))) continue;
            if ((int) field(wp, "x") == x && (int) field(wp, "y") == y && (int) field(wp, "z") == z) return wp;
        }
        return null;
    }

    /** 점검용: 이 폴더에 이 이름의 웨이포인트가 몇 개 있는가 (-1 = 지도 모드가 없거나 아직 열리지 않음). */
    public static int countForTest(String folder, String name) {
        try {
            if (!ModList.get().isLoaded("diorama")) return -1;
            Object store = store();
            if (store == null) return -1;
            int n = 0;
            for (Object wp : (List<?>) storeClass.getMethod("all").invoke(store)) {
                Object f = field(wp, "folder");
                if (f != null && folder.equalsIgnoreCase(f.toString().trim()) && name.equals(field(wp, "name"))) n++;
            }
            return n;
        } catch (Throwable t) {
            errorForTest = t.toString();
            return -1;
        }
    }

    private static void apply(Object store, JsonObject o) throws Exception {
        String folder = o.has("folder") ? o.get("folder").getAsString().trim() : "RT";
        boolean notify = o.has("notify") && o.get("notify").getAsBoolean(), force = o.has("force") && o.get("force").getAsBoolean();
        JsonObject seenAll = loadSeen();
        String wk = worldKey();
        JsonObject seen = seenAll.has(wk) && seenAll.get(wk).isJsonObject() ? seenAll.getAsJsonObject(wk) : new JsonObject();
        boolean dirty = false, storeDirty = false;
        if (o.has("gone")) {
            for (JsonElement e : o.getAsJsonArray("gone")) {
                String id = e.getAsString();
                if (!seen.has(id)) continue;
                String[] s = seen.get(id).getAsString().split("\\|");
                seen.remove(id);
                dirty = true;
                if (s.length != 5) continue;
                Object wp = find(store, folder, s[0], s[1], Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4]));
                if (wp != null) {
                    storeClass.getMethod("remove", pointClass).invoke(store, wp);
                    removedForTest++;
                }
            }
        }
        if (o.has("marks")) {
            for (JsonElement e : o.getAsJsonArray("marks")) {
                JsonObject m = e.getAsJsonObject();
                String id = m.get("id").getAsString(), name = m.get("name").getAsString().replace('|', ' '), dim = m.get("dim").getAsString();
                int x = m.get("x").getAsInt(), y = m.get("y").getAsInt(), z = m.get("z").getAsInt(), color = m.get("color").getAsInt();
                String sig = name + "|" + dim + "|" + x + "|" + y + "|" + z;
                String prev = seen.has(id) ? seen.get(id).getAsString() : null;
                if (sig.equals(prev) && !force) continue;                    // 이미 넣었다 (플레이어가 지웠더라도 다시 넣지 않는다)
                Object old = null;
                if (prev != null) {
                    String[] s = prev.split("\\|");
                    if (s.length == 5) old = find(store, folder, s[0], s[1], Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4]));
                }
                if (old != null && !sig.equals(prev)) {                      // 자리나 이름이 바뀌었다: 있던 것을 고친다
                    set(old, "name", name);
                    set(old, "dimension", dim);
                    set(old, "x", x);
                    set(old, "y", y);
                    set(old, "z", z);
                    try {
                        set(old, "initials", pointClass.getMethod("initialsFor", String.class).invoke(null, name));
                    } catch (NoSuchMethodException ignored) {
                    }
                    storeDirty = true;
                    movedForTest++;
                } else if (find(store, folder, name, dim, x, y, z) == null) {
                    storeClass.getMethod("addFolder", String.class).invoke(store, folder);   // 이미 있으면 그대로 둔다
                    Object wp = pointClass.getConstructor(String.class, int.class, int.class, int.class, String.class, int.class)
                            .newInstance(name, x, y, z, dim, color);
                    set(wp, "folder", folder);
                    storeClass.getMethod("add", pointClass).invoke(store, wp);
                    addedForTest++;
                    if (notify) NoticeHud.push(Component.translatable("rtshop.mapmark.added", name));
                }
                seen.addProperty(id, sig);
                dirty = true;
            }
        }
        if (storeDirty) storeClass.getMethod("changed").invoke(store);
        if (dirty) {
            seenAll.add(wk, seen);
            saveSeen(seenAll);
        }
    }

    // ------------------------------------------------------------------ 넣은 것의 기록

    private static Path seenPath() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("map_marks_client.json");
    }

    private static JsonObject loadSeen() {
        try {
            if (Files.exists(seenPath())) return JsonParser.parseString(Files.readString(seenPath(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ignored) {
        }
        return new JsonObject();
    }

    private static void saveSeen(JsonObject o) {
        try {
            Files.createDirectories(seenPath().getParent());
            Files.writeString(seenPath(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
