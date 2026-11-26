package com.rtshop.npc;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import com.rtshop.quest.QuestManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NPC 외형 (3.61, 시안 C): RT Shop NPC 를 주민 모습 대신 인물마다 다른 사람 모습으로 그린다.
 * <ul>
 * <li>엔티티는 그대로 주민이다 (상호작용·자리·저장은 그대로). 모습만 화면 쪽(client/NpcLookClient)에서 바꿔 그린다.</li>
 * <li>NPC 의 열쇠(엔티티 태그)는 화면 쪽에 전해지지 않으므로, 서버가 1초마다 가까운 NPC 의 "엔티티 번호 → 모습 이름"을 그 사람에게 알려 준다.</li>
 * <li>모습 파일: assets/rtshop/npc_looks/&lt;이름&gt;.bin (tools/gen_npc_looks.py 가 tools/mock/npc_mock.py 의 인물에서 뽑는다).</li>
 * <li>어느 NPC 가 어느 모습인지는 config/rtshop/npc_looks.json 의 looks ("NPC 열쇠": "모습 이름", "" = 주민 그대로). 열쇠 끝의 * 는 앞머리 맞춤.
 *     고친 뒤 /rtshop npclooks reload. enabled 를 false 로 하면 모두 주민 모습.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class NpcLooks {
    private NpcLooks() {
    }

    /** 만들어 둔 모습 전부 (모습 파일의 이름). */
    public static final List<String> ALL = List.of("erwin", "isabel", "roran", "felix", "bram", "oscar", "haru", "morris", "lucky",
            "m_farmer", "m_food", "m_fish", "m_gear", "m_general", "m_land", "m_bank", "m_ticket", "m_raid",
            "leon", "bron", "garen", "lia", "elis", "garret", "theo", "adel", "raid_guide");
    public static final String RAID_GUIDE_KEY = "raid_guide";
    private static final double RANGE = 72.0;
    private static final int VERSION = 1;

    public boolean enabled = true;
    public final Map<String, String> looks = new LinkedHashMap<>();

    private static NpcLooks current;
    /** 사람마다 이미 알려 준 "엔티티 번호 → 모습". */
    private static final Map<UUID, Map<Integer, String>> SENT = new HashMap<>();

    public static NpcLooks defaults() {
        NpcLooks n = new NpcLooks();
        String[][] d = {
                {"quest_order_elder", "erwin"}, {"quest_order_archivist", "isabel"}, {"quest_order_captain", "roran"}, {"quest_felix", "felix"},
                {"quest_bram", "bram"}, {"quest_oscar", "oscar"}, {"quest_guide", "morris"}, {"quest_bounty", "garret"}, {"quest_ranks", "theo"},
                {"quest_titles", "adel"}, {"quest_sword_master", "leon"}, {"quest_war_chief", "bron"}, {"quest_shield_keeper", "garen"},
                {"quest_hunter", "lia"}, {"quest_archmage", "elis"},
                {"attendance", "haru"}, {"gacha", "lucky"}, {"bank", "m_bank"}, {"job", "m_gear"}, {RAID_GUIDE_KEY, "raid_guide"},
                {"shop_farmer", "m_farmer"}, {"shop_food", "m_food"}, {"shop_restaurant", "m_food"}, {"shop_fisher", "oscar"},
                {"shop_blacksmith*", "bram"}, {"shop_general*", "m_general"}, {"shop_land", "m_land"}, {"shop_castle_plots", "m_land"},
                {"shop_raid", "m_raid"}, {"shop_gacha_shop*", "m_ticket"}, {"shop_attendance_shop", "haru"}};
        for (String[] e : d) n.looks.put(e[0], e[1]);
        return n;
    }

    public static synchronized NpcLooks get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void reload() {
        current = load();
        SENT.clear();
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("npc_looks.json");
    }

    private static NpcLooks load() {
        Path f = file();
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                NpcLooks n = new NpcLooks();
                if (o.has("enabled")) n.enabled = o.get("enabled").getAsBoolean();
                if (o.has("looks")) {
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("looks").entrySet()) n.looks.put(e.getKey(), e.getValue().getAsString());
                }
                return n;
            }
            NpcLooks d = defaults();
            if (Boolean.getBoolean("rtshop.selftest") || Boolean.getBoolean("rtshop.clienttest")) return d;
            Files.createDirectories(f.getParent());
            JsonObject o = new JsonObject();
            o.addProperty("_help", "NPC 외형: RT Shop NPC 를 주민 대신 인물별 사람 모습으로 그린다. looks 는 \"NPC 열쇠\": \"모습 이름\" (\"\" = 주민 그대로, 열쇠 끝의 * = 앞머리 맞춤). "
                    + "열쇠: quest_<NPC ID> · shop_<상점 ID> · bank · job · attendance · gacha · raid_guide. 모습: " + String.join(", ", ALL)
                    + ". 고친 뒤 /rtshop npclooks reload. enabled 를 false 로 하면 모두 예전의 주민 모습. 플레이어는 저마다 client.json 의 npcLooks 로 끌 수 있다.");
            o.addProperty("version", VERSION);
            o.addProperty("enabled", d.enabled);
            JsonObject l = new JsonObject();
            d.looks.forEach(l::addProperty);
            o.add("looks", l);
            Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            return d;
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] npc_looks.json 을 읽지 못했습니다: {}", ex.toString());
            return defaults();
        }
    }

    /** 이 NPC 열쇠의 모습 이름. 없거나 꺼져 있으면 "". 딱 맞는 열쇠가 먼저, 그다음 * 로 끝나는 것 가운데 가장 긴 것. */
    public static String lookOf(String key) {
        NpcLooks n = get();
        if (key == null || !n.enabled) return "";
        String exact = n.looks.get(key);
        if (exact != null) return ALL.contains(exact) ? exact : "";
        String best = "";
        int len = -1;
        for (Map.Entry<String, String> e : n.looks.entrySet()) {
            String k = e.getKey();
            if (!k.endsWith("*")) continue;
            String head = k.substring(0, k.length() - 1);
            if (key.startsWith(head) && head.length() > len) {
                len = head.length();
                best = e.getValue();
            }
        }
        return ALL.contains(best) ? best : "";
    }

    /** 엔티티의 NPC 열쇠 (레이드 안내인 포함). NPC 가 아니면 null. */
    public static String keyOf(Entity e) {
        if (e.getTags().isEmpty()) return null;
        String k = QuestManager.npcKey(e);
        if (k == null && e.getTags().contains(com.rtshop.raid.Raid.GUIDE_TAG)) return RAID_GUIDE_KEY;
        return k;
    }

    /** 모습 파일이 jar 안에 있는가 (점검용). 네모 수를 돌려준다, 없으면 -1. */
    public static int quadsOf(String look) {
        try (java.io.InputStream in = NpcLooks.class.getResourceAsStream("/assets/rtshop/npc_looks/" + look + ".bin")) {
            if (in == null) return -1;
            byte[] h = in.readNBytes(9);
            if (h.length < 9 || h[0] != 'R' || h[1] != 'T' || h[2] != 'N' || h[3] != 'L') return -1;
            return ((h[5] & 0xFF) << 24) | ((h[6] & 0xFF) << 16) | ((h[7] & 0xFF) << 8) | (h[8] & 0xFF);
        } catch (Exception ex) {
            return -1;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server == null || server.getTickCount() % 20 != 7) return;
        try {
            if (!get().enabled) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p instanceof FakePlayer || p.connection == null) continue;
                Map<Integer, String> sent = SENT.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
                if (sent.size() > 4000) sent.clear();
                JsonObject add = null;
                for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(RANGE), x -> !x.getTags().isEmpty())) {
                    String look = lookOf(keyOf(e));
                    if (look.isEmpty() || look.equals(sent.get(e.getId()))) continue;
                    sent.put(e.getId(), look);
                    if (add == null) add = new JsonObject();
                    add.addProperty(Integer.toString(e.getId()), look);
                }
                if (add != null) {
                    JsonObject o = new JsonObject();
                    o.add("looks", add);
                    ServerPlayer to = p;
                    ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> to), new StoryPacket(StoryPacket.NPC_LOOKS, o.toString()));
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] npc looks", t);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SENT.remove(event.getEntity().getUUID());
        // 다시 들어온 화면은 앞서 받은 것을 잊었다: 처음부터 다시 알린다 (reset 표시와 함께)
        if (event.getEntity() instanceof ServerPlayer p && !(p instanceof FakePlayer) && p.connection != null) {
            JsonObject o = new JsonObject();
            o.addProperty("reset", true);
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.NPC_LOOKS, o.toString()));
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("npclooks").requires(s -> s.hasPermission(2))
                .then(Commands.literal("reload").executes(ctx -> {
                    reload();
                    for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("reset", true);
                        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.NPC_LOOKS, o.toString()));
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(com.rtshop.story.StoryText.pick(ctx.getSource(),
                            "NPC 외형 설정을 다시 읽었습니다 (config/rtshop/npc_looks.json).", "Reloaded the NPC looks (config/rtshop/npc_looks.json).")), false);
                    return 1;
                }))));
    }
}
