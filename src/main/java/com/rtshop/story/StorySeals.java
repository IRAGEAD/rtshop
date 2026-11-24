package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.party.Party;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 결계: 이야기를 따라오지 않은 사람은 아직 그 보스와 싸울 수 없다.
 *
 * <ul>
 * <li>보스를 부르는 블록(제단, 묘비, 봉인의 문…)을 누르면 막히고 이유가 나온다.</li>
 * <li>결계가 걸린 보스 근처에 가면 밀려나고, 서로 피해를 줄 수 없다.</li>
 * <li>결계는 그 표식(flag)을 주는 퀘스트가 실제로 있을 때만 켜진다. 이야기 퀘스트를 지운 서버에서는 저절로 꺼진다.</li>
 * <li>표식을 가진 파티원이 가까이 있으면 함께 들어갈 수 있다 (partyCarry).</li>
 * </ul>
 * config/rtshop/seals.json
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class StorySeals {
    private StorySeals() {
    }

    public static final class Seal {
        public String id = "";
        /** 이 표식이 있어야 결계를 지나간다. */
        public String flag = "";
        /** 막혔을 때 보여 줄 말. */
        public String message = "";
        public final List<String> bosses = new ArrayList<>();
        public final List<String> blocks = new ArrayList<>();
        /** 이 구조물(ID 또는 #태그) 안으로는 표식이 없으면 들어갈 수 없다 (구조물 전체 범위). */
        public final List<String> structures = new ArrayList<>();
        public int radius = 20;
        /** 파일에 structures 칸이 있었는가 (없으면 새 기본값을 넣는다) */
        transient boolean hadStructures = true;
        /** true 면 표식을 주는 퀘스트가 없어도 늘 켜져 있다. */
        public boolean always;

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("flag", flag);
            o.addProperty("message", message);
            JsonArray b = new JsonArray();
            bosses.forEach(b::add);
            o.add("bosses", b);
            JsonArray k = new JsonArray();
            blocks.forEach(k::add);
            o.add("blocks", k);
            if (!structures.isEmpty()) {
                JsonArray st = new JsonArray();
                structures.forEach(st::add);
                o.add("structures", st);
            }
            o.addProperty("radius", radius);
            if (always) o.addProperty("always", true);
            return o;
        }

        static Seal parse(String id, JsonObject o) {
            Seal s = new Seal();
            s.id = id;
            if (o.has("flag")) s.flag = o.get("flag").getAsString();
            if (o.has("message")) s.message = o.get("message").getAsString();
            if (o.has("bosses")) for (JsonElement e : o.getAsJsonArray("bosses")) s.bosses.add(e.getAsString());
            if (o.has("blocks")) for (JsonElement e : o.getAsJsonArray("blocks")) s.blocks.add(e.getAsString());
            if (o.has("structures")) for (JsonElement e : o.getAsJsonArray("structures")) s.structures.add(e.getAsString());
            s.hadStructures = o.has("structures");
            if (o.has("radius")) s.radius = Math.max(4, o.get("radius").getAsInt());
            if (o.has("always")) s.always = o.get("always").getAsBoolean();
            return s;
        }
    }

    public boolean enabled = true;
    /** 이야기 퀘스트 보상으로만 얻는 물건: 조합법을 막는다 (봉인을 가리키는 대격변의 눈들). 그 물건을 주는 퀘스트가 있을 때만. */
    public final List<String> rewardOnly = new ArrayList<>(List.of(
            "cataclysm:monstrous_eye", "cataclysm:desert_eye", "cataclysm:mech_eye", "cataclysm:abyss_eye",
            "cataclysm:void_eye", "cataclysm:cursed_eye", "cataclysm:flame_eye", "cataclysm:storm_eye"));
    public boolean partyCarry = true;
    public int carryRadius = 48;
    public final Map<String, Seal> seals = new LinkedHashMap<>();
    /** 몹 ID / 블록 ID -> 결계 */
    private final Map<String, Seal> byBoss = new HashMap<>();
    private final Map<String, Seal> byBlock = new HashMap<>();

    private static StorySeals current;
    /** 점검용: true 면 퀘스트가 없어도 모든 결계를 켠다. */
    public static boolean forceActiveForTest;
    /** 점검용: 접속자 목록에 없는 가짜 플레이어도 파티원으로 찾는다. */
    public static final Map<UUID, ServerPlayer> TEST_PLAYERS = new HashMap<>();

    public static synchronized StorySeals get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void reload() {
        current = load();
    }

    private static Seal seal(String flag, String message, int radius, String[] bosses, String... blocks) {
        Seal s = new Seal();
        s.flag = flag;
        s.message = message;
        s.radius = radius;
        s.bosses.addAll(List.of(bosses));
        s.blocks.addAll(List.of(blocks));
        return s;
    }

    private static String[] b(String... ids) {
        return ids;
    }

    public static Map<String, Seal> defaults() {
        Map<String, Seal> m = new LinkedHashMap<>();
        // 2장: 모우지의 몹
        Seal s_wroughtnaut = seal("seal_wroughtnaut", "녹슨 문 앞에 보이지 않는 벽이 있다. 수도회의 기록관이 먼저 할 이야기가 있다고 했다.", 20,
                b("mowziesmobs:ferrous_wroughtnaut"));
        s_wroughtnaut.structures.add("mowziesmobs:wrought_chamber");
        m.put("wroughtnaut", s_wroughtnaut);
        Seal s_frostmaw = seal("seal_frostmaw", "눈보라가 길을 막는다. 거인이 품은 것이 무엇인지 아직 모른다.", 28, b("mowziesmobs:frostmaw"));
        s_frostmaw.structures.add("mowziesmobs:frostmaw_spawn");
        m.put("frostmaw", s_frostmaw);
        Seal s_umvuthi = seal("seal_umvuthi", "태양 가면의 부족이 이방인을 들이지 않는다. 탐험대장의 소개장이 필요하다.", 24, b("mowziesmobs:umvuthi"));
        s_umvuthi.structures.add("mowziesmobs:umvuthana_grove");
        m.put("umvuthi", s_umvuthi);
        Seal s_sculptor = seal("seal_sculptor", "산꼭대기의 바람이 등을 떠민다. 아직 오를 때가 아니다.", 24, b("mowziesmobs:sculptor"));
        s_sculptor.structures.add("mowziesmobs:monastery");
        m.put("sculptor", s_sculptor);
        // 3장: 아쿠아미라이
        Seal s_cornelia = seal("seal_cornelia", "얼음 위의 유령선이 안개 속으로 물러난다. 선장의 이름을 아직 모른다.", 28, b("aquamirae:captain_cornelia"));
        s_cornelia.structures.add("aquamirae:ship");
        m.put("cornelia", s_cornelia);
        m.put("maze_mother", seal("seal_maze_mother", "물 밑의 미로가 닫혀 있다.", 24, b("aquamirae:maze_mother")));
        // 4장: 대격변 — 일곱 봉인
        Seal s_netherite_monstrosity = seal("seal_monstrosity", "대장간의 불이 꺼져 있다. 봉인의 순서를 지켜야 한다.", 28,
                b("cataclysm:netherite_monstrosity"));
        s_netherite_monstrosity.structures.add("cataclysm:soul_black_smith");
        m.put("netherite_monstrosity", s_netherite_monstrosity);
        Seal s_ender_guardian = seal("seal_ender_guardian", "공허의 제단이 응답하지 않는다. 봉인의 순서를 지켜야 한다.", 28,
                b("cataclysm:ender_guardian"), "cataclysm:altar_of_void");
        s_ender_guardian.structures.add("cataclysm:ruined_citadel");
        m.put("ender_guardian", s_ender_guardian);
        Seal s_harbinger = seal("seal_harbinger", "기계의 심장이 잠들어 있다. 봉인의 순서를 지켜야 한다.", 28, b("cataclysm:the_harbinger"));
        s_harbinger.structures.add("cataclysm:ancient_factory");
        m.put("harbinger", s_harbinger);
        Seal s_leviathan = seal("seal_leviathan", "심연의 제단이 차갑다. 봉인의 순서를 지켜야 한다.", 32,
                b("cataclysm:the_leviathan"), "cataclysm:altar_of_abyss");
        s_leviathan.structures.add("cataclysm:sunken_city");
        m.put("leviathan", s_leviathan);
        Seal s_ancient_remnant = seal("seal_remnant", "봉인의 문이 꿈쩍도 하지 않는다. 봉인의 순서를 지켜야 한다.", 28,
                b("cataclysm:ancient_remnant"), "cataclysm:door_of_seal", "cataclysm:door_of_seal_part");
        s_ancient_remnant.structures.add("cataclysm:cursed_pyramid");
        m.put("ancient_remnant", s_ancient_remnant);
        Seal s_maledictus = seal("seal_maledictus", "저주받은 묘비가 침묵한다. 봉인의 순서를 지켜야 한다.", 28,
                b("cataclysm:maledictus"), "cataclysm:cursed_tombstone");
        s_maledictus.structures.add("cataclysm:frosted_prison");
        m.put("maledictus", s_maledictus);
        Seal s_ignis = seal("seal_ignis", "불의 제단이 당신을 밀어낸다. 여섯 눈이 감기기 전에는 이 문이 열리지 않는다.", 32,
                b("cataclysm:ignis"), "cataclysm:altar_of_fire");
        s_ignis.structures.add("cataclysm:burning_arena");
        m.put("ignis", s_ignis);
        // 숨은 보스
        Seal s_scylla = seal("seal_scylla", "폭풍이 당신을 알아보지 못한다. 지워진 이름을 먼저 찾아야 한다.", 32, b("cataclysm:scylla"));
        s_scylla.structures.add("cataclysm:acropolis");
        m.put("scylla", s_scylla);
        Seal ill = seal("seal_illusioner", "저택이 보랏빛 안개에 싸여 있다. 거울이 당신의 얼굴을 비추지 않는다. 연대기의 빈 쪽을 먼저 찾아야 한다.", 24, b("illusioner:illusioner"));
        ill.structures.add("minecraft:mansion");
        m.put("illusioner", ill);
        return m;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("seals.json");
    }

    private static StorySeals load() {
        StorySeals c = new StorySeals();
        Path f = file();
        boolean write = false;
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
                if (o.has("partyCarry")) c.partyCarry = o.get("partyCarry").getAsBoolean();
                if (o.has("rewardOnly")) {
                    c.rewardOnly.clear();
                    for (JsonElement e : o.getAsJsonArray("rewardOnly")) c.rewardOnly.add(e.getAsString());
                } else {
                    write = true;
                }
                if (o.has("carryRadius")) c.carryRadius = Math.max(8, o.get("carryRadius").getAsInt());
                if (o.has("seals")) {
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("seals").entrySet()) {
                        if (e.getValue().isJsonObject()) c.seals.put(e.getKey(), Seal.parse(e.getKey(), e.getValue().getAsJsonObject()));
                    }
                }
            } else {
                write = true;
            }
            for (Map.Entry<String, Seal> e : defaults().entrySet()) {
                if (!c.seals.containsKey(e.getKey())) {
                    e.getValue().id = e.getKey();
                    c.seals.put(e.getKey(), e.getValue());
                    write = true;
                } else if (!c.seals.get(e.getKey()).hadStructures && !e.getValue().structures.isEmpty()) {
                    // 예전 파일: 구조물 결계(예: 환술사의 저택)를 새로 넣는다
                    Seal old = c.seals.get(e.getKey());
                    old.structures.addAll(e.getValue().structures);
                    old.hadStructures = true;
                    write = true;
                }
            }
            if (write) {
                Files.createDirectories(f.getParent());
                Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] seals.json 을 읽지 못했습니다: {}", ex.toString());
            if (c.seals.isEmpty()) {
                defaults().forEach((k, v) -> {
                    v.id = k;
                    c.seals.put(k, v);
                });
            }
        }
        for (Seal s : c.seals.values()) {
            for (String id : s.bosses) c.byBoss.put(id, s);
            for (String id : s.blocks) c.byBlock.put(id, s);
        }
        return c;
    }

    JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "결계: flag 표식이 없는 사람은 bosses 의 보스에게 다가가거나 blocks 의 블록(보스 소환)을 쓸 수 없다. "
                + "그 표식을 주는 퀘스트가 있을 때만 켜진다 (always: true 면 늘). rewardOnly: 퀘스트 보상으로만 얻게 조합법을 막을 물건 (그 물건을 주는 퀘스트가 있을 때만). partyCarry: 표식을 가진 파티원이 carryRadius 안에 있으면 함께 지나간다. 저장한 뒤 /rtshop reload");
        o.addProperty("enabled", enabled);
        JsonArray ro = new JsonArray();
        rewardOnly.forEach(ro::add);
        o.add("rewardOnly", ro);
        o.addProperty("partyCarry", partyCarry);
        o.addProperty("carryRadius", carryRadius);
        JsonObject s = new JsonObject();
        for (Map.Entry<String, Seal> e : seals.entrySet()) s.add(e.getKey(), e.getValue().toJson());
        o.add("seals", s);
        return o;
    }

    // ------------------------------------------------------------------ 판단

    /** 이 표식을 주는 퀘스트가 있는가 (없으면 결계는 꺼져 있다). */
    public static boolean grantedByAnyQuest(String flag) {
        for (QuestDef q : QuestManager.loaded().quests.values()) {
            if (q.rewardFlags.contains(flag)) return true;
            for (QuestDef.Choice ch : q.choices) {
                if (ch.flags.contains(flag)) return true;
            }
        }
        return false;
    }

    /** 이 물건의 조합법을 막는가: rewardOnly 에 있고, 그 물건을 보상으로 주는 퀘스트가 있을 때 (스킬라의 폭풍의 눈처럼 아직 퀘스트가 없으면 막지 않는다). */
    public static boolean recipeLocked(String itemId) {
        StorySeals c = get();
        if (!c.enabled || !c.rewardOnly.contains(itemId)) return false;
        if (ScyllaClues.grantsItem(itemId)) return true; // 스킬라 단서를 다 모으면 주는 폭풍의 눈
        for (QuestDef q : QuestManager.loaded().quests.values()) {
            for (QuestDef.ItemReward r : q.rewardItems) if (r.item.equals(itemId)) return true;
        }
        return false;
    }

    public static boolean active(Seal s) {
        return get().enabled && !s.flag.isEmpty() && (s.always || forceActiveForTest || grantedByAnyQuest(s.flag) || ScyllaClues.grantsFlag(s.flag));
    }

    /** 이 사람이 결계를 지나갈 수 있는가. */
    public static boolean passes(ServerPlayer p, Seal s) {
        if (!active(s) || p.isCreative() || p.isSpectator() || p instanceof FakePlayer && !forceActiveForTest) return true;
        if (com.rtshop.raid.Raid.inArena(p)) return true; // 주간 레이드는 레이드 조건으로 따로 막는다
        if (QuestManager.hasFlag(p, s.flag)) return true;
        StorySeals c = get();
        if (c.partyCarry) {
            Party.Group g = Party.of(p.server, p.getUUID());
            if (g != null) {
                List<ServerPlayer> members = new ArrayList<>(Party.online(p.server, g));
                for (UUID u : g.members) if (TEST_PLAYERS.containsKey(u)) members.add(TEST_PLAYERS.get(u));
                for (ServerPlayer m : members) {
                    if (m != p && m.level() == p.level() && m.distanceTo(p) <= c.carryRadius && QuestManager.hasFlag(m, s.flag)) return true;
                }
            }
        }
        return false;
    }

    /** 결계가 걸린(=이야기) 보스가 결계 반경 안에 살아 있는가 (사망 때 Gold 손실을 막는다). */
    public static boolean nearStoryBoss(ServerPlayer p) {
        StorySeals c = get();
        if (!c.enabled || c.byBoss.isEmpty()) return false;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(48),
                e -> e.isAlive() && c.byBoss.containsKey(BossIntros.id(e)))) {
            Seal s = c.byBoss.get(BossIntros.id(e));
            if (e.distanceTo(p) <= Math.max(32, s.radius + 16)) return true;
        }
        return false;
    }

    public static Seal ofBoss(LivingEntity e) {
        return get().byBoss.get(BossIntros.id(e));
    }

    public static Seal ofBlock(String blockId) {
        return get().byBlock.get(blockId);
    }

    private static final Map<UUID, Long> LAST_MSG = new HashMap<>();

    private static void tell(ServerPlayer p, Seal s) {
        long now = p.server.getTickCount();
        Long last = LAST_MSG.get(p.getUUID());
        if (last != null && now - last < 60) return;
        LAST_MSG.put(p.getUUID(), now);
        com.rtshop.notice.Notice.send(p, Component.literal("✦ " + StoryText.t(p, s.message)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    /** 결계 밖으로 민다. */
    public static void pushAway(ServerPlayer p, Vec3 from, Seal s) {
        Vec3 d = p.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1e-4) d = new Vec3(1, 0, 0);
        d = d.normalize().scale(1.4);
        p.setDeltaMovement(d.x, 0.45, d.z);
        p.hurtMarked = true;
        if (p.connection != null) p.connection.send(new ClientboundSetEntityMotionPacket(p));
        tell(p, s);
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        String id = String.valueOf(ForgeRegistries.BLOCKS.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()));
        Seal s = ofBlock(id);
        if (s == null || passes(p, s)) return;
        event.setCanceled(true);
        tell(p, s);
    }

    /** 결계가 걸린 보스와 아직 그럴 자격이 없는 사람은 서로 다치지 않는다. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        MinecraftServer server = victim.getServer();
        if (server == null) return;
        if (victim instanceof ServerPlayer p && event.getSource().getEntity() instanceof LivingEntity boss) {
            Seal s = ofBoss(boss);
            if (s != null && !passes(p, s)) {
                event.setCanceled(true);
                pushAway(p, boss.position(), s);
            }
            return;
        }
        Seal s = ofBoss(victim);
        if (s == null || com.rtshop.raid.Raid.inArena(victim)) return;
        ServerPlayer attacker = com.rtshop.pvp.Pvp.attackerOf(event.getSource(), server);
        if (attacker != null && !passes(attacker, s)) {
            event.setCanceled(true);
            tell(attacker, s);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 10 != 3) return;
        StorySeals c = get();
        if (!c.enabled) return;
        if (server.getTickCount() % 20 == 3) structureTick(server, c);
        if (c.byBoss.isEmpty()) return;
        Set<String> live = new HashSet<>();
        for (Seal s : c.seals.values()) if (!s.bosses.isEmpty() && active(s)) live.addAll(s.bosses);
        if (live.isEmpty()) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isCreative() || p.isSpectator()) continue;
            ServerLevel level = p.serverLevel();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(40),
                    e -> e.isAlive() && live.contains(BossIntros.id(e)))) {
                Seal s = c.byBoss.get(BossIntros.id(e));
                if (s != null && e.distanceTo(p) < s.radius && !passes(p, s)) {
                    pushAway(p, e.position(), s);
                    break;
                }
            }
        }
    }

    /** 표식 없이 결계 구조물 안에 들어온 사람을 밖으로 민다. */
    private static void structureTick(MinecraftServer server, StorySeals c) {
        for (Seal s : c.seals.values()) {
            if (s.structures.isEmpty() || !active(s)) continue;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isCreative() || p.isSpectator() || passes(p, s)) continue;
                net.minecraft.world.level.levelgen.structure.BoundingBox box = structureBox(p, s);
                if (box == null) continue;
                // 지금 퀘스트가 이 구조물로 보냈다면 들어갈 수 있다 (도착 목표·안의 몹 처치 목표)
                boolean led = false;
                for (String st : s.structures) led |= QuestManager.questLeadsTo(p, st);
                if (led) continue;
                net.minecraft.core.BlockPos ctr = box.getCenter();
                pushAway(p, new Vec3(ctr.getX() + 0.5, p.getY(), ctr.getZ() + 0.5), s);
            }
        }
    }

    /** 이 사람이 서 있는 결계 구조물의 범위 (없으면 null). */
    public static net.minecraft.world.level.levelgen.structure.BoundingBox structureBox(ServerPlayer p, Seal s) {
        ServerLevel level = p.serverLevel();
        var reg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        net.minecraft.core.BlockPos pos = p.blockPosition();
        for (String id : s.structures) {
            try {
                if (id.startsWith("#")) {
                    var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE, new net.minecraft.resources.ResourceLocation(id.substring(1)));
                    var start = level.structureManager().getStructureWithPieceAt(pos, tag);
                    if (start.isValid()) return start.getBoundingBox();
                    continue;
                }
                var st = reg.get(new net.minecraft.resources.ResourceLocation(id));
                if (st == null) continue;
                var start = level.structureManager().getStructureAt(pos, st);
                if (start.isValid() && start.getBoundingBox().inflatedBy(2).isInside(pos)) return start.getBoundingBox();
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
