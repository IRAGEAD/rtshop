package com.rtshop.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 읽을 수 있는 이야기 물건 (config/rtshop/readables.json): 서약서·연대기 페이지·일지 같은 것을 손에 들고 우클릭하면 펼쳐 읽는다.
 * <pre>
 * "items": { "rtshop:oath_scroll": { "title": "...", "sub": "...", "pages": [ {"head": "...", "text": "...", "by": "...", "need": {...}} ],
 *                                    "chronicle": true (끝낸 퀘스트에서 기록관이 적은 이름들을 뒤에 붙인다), "empty": "아직 읽을 것이 없을 때의 글" } }
 * </pre>
 * 쪽마다 need (대화와 같은 조건: flags·notFlags·quests·notQuests·items) 를 둘 수 있어, 이야기가 나아간 만큼만 읽힌다.
 */
public final class Readables {
    private Readables() {
    }

    private static JsonObject config;

    static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("readables.json");
    }

    static JsonObject bundled() {
        try (InputStream in = Readables.class.getResourceAsStream("/rtshop_defaults/readables.json")) {
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
                int bundledV = def.has("version") ? def.get("version").getAsInt() : 1;
                if (cur == null || (!edited && v < bundledV)) {   // 고치지 않은 예전 기본 파일은 새 판으로 바꾼다 ("edited": true 면 그대로 둔다)
                    Files.createDirectories(f.getParent());
                    Files.writeString(f, new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(def), StandardCharsets.UTF_8);
                    cur = def;
                }
                config = cur;
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] readables.json 을 읽지 못해 기본 글을 씁니다: {}", e.toString());
                config = def;
            }
        }
        return config;
    }

    public static synchronized void reload() {
        config = null;
    }

    private static JsonObject def(String itemId) {
        JsonObject all = get();
        if (!all.has("items") || !all.get("items").isJsonObject()) return null;
        JsonObject items = all.getAsJsonObject("items");
        return itemId != null && items.has(itemId) && items.get(itemId).isJsonObject() ? items.getAsJsonObject(itemId) : null;
    }

    public static boolean has(String itemId) {
        return def(itemId) != null;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private static JsonObject page(String head, String text, String by) {
        JsonObject o = new JsonObject();
        o.addProperty("head", head);
        o.addProperty("text", text);
        o.addProperty("by", by);
        return o;
    }

    /** 끝낸 퀘스트에서 기록관이 연대기에 적은 이름들 (퀘스트 순서대로). */
    public static List<JsonObject> chronicle(ServerPlayer p) {
        List<QuestDef> qs = new ArrayList<>(QuestManager.loaded().quests.values());
        qs.sort(Comparator.comparingInt(q -> q.order));
        List<JsonObject> out = new ArrayList<>();
        for (QuestDef q : qs) {
            JsonObject s = q.sceneDone;
            if (s == null || !"names".equals(str(s, "kind")) || !QuestManager.isCompleted(p, q.id)) continue;
            String title = str(s, "title");
            int cut = title.indexOf(" · ");
            out.add(page(cut >= 0 ? title.substring(cut + 3) : title, str(s, "text"), str(s, "speaker")));
        }
        return out;
    }

    /** 이 사람이 지금 읽을 수 있는 내용. 읽는 물건이 아니면 null. */
    public static JsonObject build(ServerPlayer p, String itemId) {
        JsonObject d = def(itemId);
        if (d == null) return null;
        JsonObject out = new JsonObject();
        out.addProperty("item", itemId);
        out.addProperty("title", str(d, "title"));
        out.addProperty("sub", str(d, "sub"));
        JsonArray pages = new JsonArray();
        if (d.has("pages") && d.get("pages").isJsonArray()) {
            for (JsonElement e : d.getAsJsonArray("pages")) {
                if (!e.isJsonObject()) continue;
                JsonObject pg = e.getAsJsonObject();
                if (!Dialogue.ok(p, pg)) continue;
                pages.add(page(str(pg, "head"), str(pg, "text"), str(pg, "by")));
            }
        }
        if (d.has("chronicle") && d.get("chronicle").getAsBoolean()) for (JsonObject c : chronicle(p)) pages.add(c);
        if (pages.size() == 0) pages.add(page("", str(d, "empty"), ""));
        out.add("pages", pages);
        return out;
    }

    /** 손에 든 물건을 펼쳐 읽는다. 읽는 물건이 아니면 false. */
    public static boolean open(ServerPlayer p, ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        JsonObject out = id == null ? null : build(p, id.toString());
        if (out == null) return false;
        if (p.connection != null && !(p instanceof net.minecraftforge.common.util.FakePlayer)) {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.READ, StoryText.json(p, out).toString()));
        }
        return true;
    }
}
