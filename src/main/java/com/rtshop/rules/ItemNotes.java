package com.rtshop.rules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rtshop.RtShop;
import com.rtshop.enhance.EnhanceConfig;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 아이템의 쓰임새 한 줄 설명 (config/rtshop/item_notes.json, 3.53).
 * 아이템에 마우스를 올리고 Shift 를 누르면, 그 물건이 이 서버에서 어디에 쓰이는지가 짧게 나온다:
 * 어느 상인이 팔고 어느 상인이 사 주는지, 강화 재료인지, 뽑기에서 나오는지, 보스 전리품인지, 퀘스트에 내는 물건인지.
 * 이 줄들은 상점·강화·뽑기·퀘스트 설정에서 저절로 만들어진다. 따로 적고 싶은 설명은 notes 에 "아이템 ID": "글" 로 넣는다 (맨 위에 보인다).
 * <p>클라이언트로 보내는 모양: {"아이템 ID": [["글 또는 번역 키", 인자…], …]}. 첫 칸이 "=" 로 시작하면 그대로 보이는 글.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class ItemNotes {
    private ItemNotes() {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static class Config {
        public String _help = "아이템 쓰임새 설명. enabled=false 면 끈다. auto: 상점·강화·뽑기·퀘스트에서 저절로 만드는 줄. notes: \"아이템 ID\": \"직접 적는 짧은 설명\" (\\n 으로 줄바꿈).";
        public boolean enabled = true;
        public boolean auto = true;
        public Map<String, String> notes = new LinkedHashMap<>();
    }

    private static Config config;
    private static volatile String payload = "{}";
    private static volatile Map<String, List<String[]>> built = Map.of();

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("item_notes.json");
    }

    public static synchronized Config get() {
        if (config == null) {
            Config c = null;
            Path f = file();
            try {
                if (Files.exists(f)) c = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Config.class);
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] item_notes.json 을 읽지 못해 기본값을 씁니다: {}", e.toString());
            }
            if (c == null) {
                c = new Config();
                try {
                    Files.createDirectories(f.getParent());
                    Files.writeString(f, GSON.toJson(c), StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                }
            }
            if (c.notes == null) c.notes = new LinkedHashMap<>();
            config = c;
        }
        return config;
    }

    private static void add(Map<String, List<String[]>> out, String item, String... line) {
        if (item == null || item.isEmpty()) return;
        List<String[]> l = out.computeIfAbsent(item, k -> new ArrayList<>());
        if (l.size() < 8) l.add(line);
    }

    private static String id(Item item) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl == null ? "" : rl.toString();
    }

    /** 상점을 읽은 뒤 (ShopServer.reload): 줄을 다시 만들고 접속한 사람에게 보낸다. */
    public static synchronized void rebuild(MinecraftServer server) {
        config = null;
        Config c = get();
        Map<String, List<String[]>> out = new LinkedHashMap<>();
        if (c.enabled) {
            for (Map.Entry<String, String> e : c.notes.entrySet()) {
                for (String line : e.getValue().split("\n")) add(out, e.getKey(), "=" + line);
            }
            if (c.auto) {
                try {
                    auto(out);
                } catch (Throwable t) {
                    RtShop.LOGGER.warn("[rtshop] 아이템 쓰임새 설명을 만들지 못했습니다: {}", t.toString());
                }
            }
        }
        built = out;
        JsonObject o = new JsonObject();
        for (Map.Entry<String, List<String[]>> e : out.entrySet()) {
            JsonArray lines = new JsonArray();
            for (String[] line : e.getValue()) {
                JsonArray a = new JsonArray();
                for (String s : line) a.add(s);
                lines.add(a);
            }
            o.add(e.getKey(), lines);
        }
        payload = o.toString();
        if (server != null) for (ServerPlayer p : server.getPlayerList().getPlayers()) send(p);
    }

    private static void auto(Map<String, List<String[]>> out) {
        // 상점: 파는 곳·사 주는 곳 (상점 여러 곳이면 값이 가장 좋은 곳)
        Map<String, String[]> buy = new LinkedHashMap<>(), sell = new LinkedHashMap<>();
        for (ShopData d : ShopServer.shops().values()) {
            String who = d.npcName == null || d.npcName.isEmpty() ? d.title : d.npcName;
            for (ShopData.Entry e : d.entries) {
                String item = id(e.stack.getItem());
                if (item.isEmpty() || e.stack.hasTag()) continue;
                if (e.buyable && e.price > 0) {
                    String[] old = buy.get(item);
                    if (old == null || Long.parseLong(old[2]) > e.price) buy.put(item, new String[]{"rtshop.note.sold_by", who, String.valueOf(e.price)});
                }
                if (e.sellable && e.sellUnit >= 1) {
                    String[] old = sell.get(item);
                    long v = Math.round(e.sellUnit);
                    if (old == null || Long.parseLong(old[2]) < v) sell.put(item, new String[]{"rtshop.note.bought_by", who, String.valueOf(v)});
                }
            }
        }
        buy.forEach((k, v) -> add(out, k, v));
        sell.forEach((k, v) -> add(out, k, v));
        // 강화 재료: 어느 단계에 드는가
        EnhanceConfig ec = EnhanceConfig.get();
        Map<String, int[]> mats = new LinkedHashMap<>();
        for (EnhanceConfig.Level l : ec.levels) {
            if (l == null || l.level <= 0) continue;
            for (EnhanceConfig.Material m : l.materials) {
                for (Item it : ec.itemsOf(m)) {
                    int[] r = mats.computeIfAbsent(id(it), k -> new int[]{l.level, l.level});
                    r[0] = Math.min(r[0], l.level);
                    r[1] = Math.max(r[1], l.level);
                }
            }
        }
        mats.forEach((k, r) -> add(out, k, r[0] == r[1] ? new String[]{"rtshop.note.enhance_one", String.valueOf(r[0])}
                : new String[]{"rtshop.note.enhance", String.valueOf(r[0]), String.valueOf(r[1])}));
        // 뽑기
        for (String cat : new String[]{"weapon", "accessory"}) {
            for (com.rtshop.gacha.GachaConfig.Entry e : com.rtshop.gacha.GachaConfig.get().pool(cat)) {
                if (e.item != null && !e.item.isEmpty()) add(out, e.item, "rtshop.note.gacha", "@grade:" + e.grade);
            }
        }
        // 퀘스트에 내는 물건
        Map<String, Integer> deliver = new LinkedHashMap<>();
        for (QuestDef q : QuestManager.loaded().quests.values()) {
            for (QuestDef.Objective ob : q.objectives) {
                if (ob.type == QuestDef.Type.DELIVER && ob.target != null && !ob.target.startsWith("#")) deliver.merge(ob.target, 1, Integer::sum);
            }
        }
        deliver.forEach((k, n) -> add(out, k, "rtshop.note.quest", String.valueOf(n)));
        // 보스 전리품·상점에서만 구하는 장비
        for (Item item : ForgeRegistries.ITEMS) {
            String iid = id(item);
            if (iid.isEmpty()) continue;
            if (ItemPolicy.bossItem(item)) add(out, iid, "rtshop.note.boss");
            else if (ItemPolicy.shopOnly(item)) add(out, iid, "rtshop.note.no_recipe");
        }
    }

    public static void send(ServerPlayer p) {
        if (p == null || p.connection == null || p instanceof FakePlayer) return;
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.ITEM_NOTES, payload));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) send(p);
    }

    /** 이 아이템의 줄들 (시험·점검용). */
    public static List<String[]> of(String itemId) {
        return built.getOrDefault(itemId, List.of());
    }

    public static int countForTest() {
        return built.size();
    }
}
