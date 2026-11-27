package com.rtshop.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rtshop.RtShop;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import com.rtshop.shop.ShopServer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 동행 NPC 관리: 동행이 붙은 퀘스트(companion)를 진행 중인 플레이어가 그 장소에 닿으면 동행을 곁에 불러내고,
 * 퀘스트를 마치거나 포기하거나 멀리 떠나면 돌려보낸다. 상황마다 한마디씩 한다 (알림 상자).
 * <p>여럿이 함께할 때 (3.48): 가까이에 같은 NPC 의 동행이 이미 서 있으면 새로 불러내지 않고 그 동행의 일행이 된다 (파티가 아니어도).
 * 말은 듣는 사람마다 그 사람의 퀘스트에 붙은 것을, 사람마다 한 번씩 한다. 동행이 잡은 몬스터는 일행 모두의 처치로 센다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Companions {
    private Companions() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RtShop.MODID);
    public static final RegistryObject<EntityType<CompanionEntity>> TYPE = ENTITIES.register("companion",
            () -> EntityType.Builder.<CompanionEntity>of(CompanionEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .noSave().build("rtshop:companion"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(TYPE.get(), CompanionEntity.attributes().build()));
    }

    /** 플레이어 -> (퀘스트 -> 동행). */
    private static final Map<UUID, Map<String, CompanionEntity>> LIVE = new HashMap<>();
    private static final Map<UUID, long[]> IDLE = new HashMap<>(); // {마지막으로 움직인 시각, 서 있는 자리, (안 씀), 마지막 혼잣말}
    public static final long SAY_GAP_MS = 7000;

    public static CompanionEntity of(ServerPlayer p, String questId) {
        Map<String, CompanionEntity> m = LIVE.get(p.getUUID());
        CompanionEntity c = m == null ? null : m.get(questId);
        return c != null && c.isAlive() && !c.isRemoved() ? c : null;
    }

    public static int count(ServerPlayer p) {
        Map<String, CompanionEntity> m = LIVE.get(p.getUUID());
        if (m == null) return 0;
        m.values().removeIf(c -> !c.isAlive() || c.isRemoved());
        return m.size();
    }

    private static String str(JsonObject o, String k, String d) {
        return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d;
    }

    /** 동행을 플레이어 곁(뒤쪽 옆)에 불러낸다. */
    public static CompanionEntity spawn(ServerPlayer p, QuestDef q) {
        JsonObject def = q.companion;
        String npcId = str(def, "npc", "");
        QuestManager.NpcDef n = QuestManager.loaded().npcs.get(npcId);
        if (n == null) return null;
        ServerLevel level = p.serverLevel();
        CompanionEntity c = TYPE.get().create(level);
        if (c == null) return null;
        Vec3 back = Vec3.directionFromRotation(0, p.getYRot()).scale(-1.6);
        BlockPos at = BlockPos.containing(p.getX() + back.x + 0.8, p.getY(), p.getZ() + back.z + 0.8);
        if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() || !level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
            at = p.blockPosition();
        }
        c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), 0);
        c.ownerId = p.getUUID();
        if (p instanceof net.minecraftforge.common.util.FakePlayer) c.testOwner = p;
        c.questId = q.id;
        c.npcId = npcId;
        c.setLook(com.rtshop.npc.NpcLooks.lookOf("quest_" + npcId));   // 3.75 (W-2): 나타나는 순간부터 그 인물의 모습
        c.style = str(def, "style", "melee");
        c.setVillagerData(new VillagerData(VillagerType.PLAINS, ShopServer.profession(n.profession), 5));
        c.setCustomName(Component.literal(StoryText.t(p, n.name)));
        c.setCustomNameVisible(true);
        level.addFreshEntity(c);
        level.sendParticles(ParticleTypes.POOF, c.getX(), c.getY() + 1, c.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
        LIVE.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(q.id, c);
        say(c, "arrive");
        return c;
    }

    /** 같은 NPC 의 동행이 이 사람 가까이(같은 세계, 40칸 안)에 이미 서 있으면 그 동행. */
    public static CompanionEntity nearby(ServerPlayer p, String npcId) {
        for (Map<String, CompanionEntity> m : LIVE.values()) {
            for (CompanionEntity c : m.values()) {
                if (c.isAlive() && !c.isRemoved() && c.npcId.equals(npcId) && c.level() == p.level() && c.distanceToSqr(p) <= 40 * 40
                        && !p.getUUID().equals(c.ownerId)) return c;
            }
        }
        return null;
    }

    /** 이미 서 있는 동행의 일행이 된다 (새로 불러내지 않는다). 처음 만나는 인사는 이 사람에게만 한다. */
    public static void join(ServerPlayer p, QuestDef q, CompanionEntity c) {
        c.guests.put(p.getUUID(), q.id);
        if (p instanceof net.minecraftforge.common.util.FakePlayer) c.testGuests.put(p.getUUID(), p);
        LIVE.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(q.id, c);
        sayTo(c, p, q.id, "arrive");
    }

    /** 더는 함께 걷지 않는 일행(나갔거나, 멀어졌거나, 퀘스트를 마친 사람)을 뺀다. */
    static void pruneGuests(CompanionEntity c) {
        for (java.util.Iterator<Map.Entry<UUID, String>> it = c.guests.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, String> e = it.next();
            ServerPlayer g = c.guest(e.getKey());
            if (c.walksWith(g, e.getValue())) continue;
            it.remove();
            c.testGuests.remove(e.getKey());
            Map<String, CompanionEntity> m = LIVE.get(e.getKey());
            if (m != null) m.remove(e.getValue(), c);
            if (g != null && !QuestManager.isActive(g, e.getValue())) forget(g, e.getValue());
        }
    }

    /** 앞장선 사람이 떠났다: 남은 일행 가운데 첫 사람이 이어받는다. 이어받을 사람이 없으면 false. */
    static boolean handOver(CompanionEntity c, ServerPlayer old) {
        for (Map.Entry<UUID, String> e : new java.util.ArrayList<>(c.guests.entrySet())) {
            ServerPlayer g = c.guest(e.getKey());
            if (!c.walksWith(g, e.getValue())) continue;
            if (c.ownerId != null) {
                Map<String, CompanionEntity> m = LIVE.get(c.ownerId);
                if (m != null) m.remove(c.questId, c);
            }
            if (old != null && !QuestManager.isActive(old, c.questId)) forget(old, c.questId);
            c.guests.remove(e.getKey());
            c.ownerId = g.getUUID();
            c.testOwner = g instanceof net.minecraftforge.common.util.FakePlayer ? g : null;
            c.testGuests.remove(e.getKey());
            c.questId = e.getValue();
            QuestDef q = QuestManager.loaded().quests.get(c.questId);
            if (q != null && q.companion != null) c.style = str(q.companion, "style", c.style);
            return true;
        }
        return false;
    }

    /** 동행이 떠난다. 퀘스트가 끝났거나 포기됐으면 그 퀘스트에서 한 말의 기록도 지운다. */
    static void leave(CompanionEntity c, ServerPlayer owner) {
        if (owner != null && !QuestManager.isActive(owner, c.questId)) forget(owner, c.questId);
        if (c.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.POOF, c.getX(), c.getY() + 1, c.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
        if (c.ownerId != null) {
            Map<String, CompanionEntity> m = LIVE.get(c.ownerId);
            if (m != null) m.remove(c.questId, c);
        }
        for (Map.Entry<UUID, String> e : c.guests.entrySet()) {
            Map<String, CompanionEntity> m = LIVE.get(e.getKey());
            if (m != null) m.remove(e.getValue(), c);
        }
        c.guests.clear();
        c.discard();
    }

    // ------------------------------------------------------------------ 동행의 말 (3.41: 같은 말을 되풀이하지 않고, 자리에 맞는 말만)
    // 규칙: 도착 인사는 퀘스트마다 한 번. 흔적 한마디는 그 흔적에 붙은 말을 한 번. 쉴 때의 혼잣말은 줄마다 한 번, 싸움이 끝나고 한참 서 있을 때만.
    //       싸울 때의 외침은 드물게(한 번 외치면 90초 쉼), 같은 줄을 잇달아 하지 않는다. 일이 끝나면(목표를 다 채우면) 작별 한마디를 한 번.
    public static final long FIGHT_GAP_MS = 90_000, IDLE_FIRST_MS = 45_000, IDLE_GAP_MS = 150_000, LOW_GAP_MS = 60_000, CALM_MS = 20_000;
    private static final String SAID = "rtshop_comp_said";

    private static net.minecraft.nbt.CompoundTag saidStore(ServerPlayer p, boolean create) {
        net.minecraft.nbt.CompoundTag pers = p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if (create && !p.getPersistentData().contains(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG)) {
            p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, pers);
        }
        if (create && !pers.contains(SAID)) pers.put(SAID, new net.minecraft.nbt.CompoundTag());
        return pers.getCompound(SAID);
    }

    /** 이 퀘스트에서 이 말을 이미 했는가. */
    public static boolean said(ServerPlayer p, String questId, String key) {
        return saidStore(p, false).getCompound(questId).getBoolean(key);
    }

    private static void mark(ServerPlayer p, String questId, String key) {
        net.minecraft.nbt.CompoundTag st = saidStore(p, true);
        net.minecraft.nbt.CompoundTag qd = st.getCompound(questId);
        qd.putBoolean(key, true);
        st.put(questId, qd);
    }

    /** 퀘스트가 끝났거나 포기됐다: 그 퀘스트에서 한 말의 기록을 지운다 (다시 받으면 처음부터). */
    static void forget(ServerPlayer p, String questId) {
        saidStore(p, false).remove(questId);
    }

    private static JsonObject lines(String questId) {
        QuestDef q = QuestManager.loaded().quests.get(questId);
        return q == null || q.companion == null || !q.companion.has("lines") ? null : q.companion.getAsJsonObject("lines");
    }

    private static void speak(CompanionEntity c, ServerPlayer o, com.google.gson.JsonElement line) {
        c.lastSaid = System.currentTimeMillis();
        String text = StoryText.t(o, Loc.pick(o, line));
        com.rtshop.notice.Notice.send(o, Component.literal(c.getName().getString()).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(": " + text).withStyle(ChatFormatting.WHITE)));
    }

    /** 줄 묶음에서 이 사람에게 아직 안 한 첫 줄을 한다. once 가 아니면 다 한 뒤 처음부터 돌되, 방금 한 줄은 잇달아 하지 않는다. */
    private static boolean sayNext(CompanionEntity c, ServerPlayer o, String questId, String when, boolean once) {
        JsonObject ls = lines(questId);
        if (ls == null || !ls.has(when) || !ls.get(when).isJsonArray()) return false;
        JsonArray pool = ls.getAsJsonArray(when);
        if (pool.size() == 0) return false;
        for (int i = 0; i < pool.size(); i++) {
            String key = when + i;
            if (!said(o, questId, key)) {
                mark(o, questId, key);
                speak(c, o, pool.get(i));
                return true;
            }
        }
        if (once) return false;
        String ik = when + "/" + o.getUUID();
        int next = (c.lastIndex.getOrDefault(ik, -1) + 1) % pool.size();
        c.lastIndex.put(ik, next);
        speak(c, o, pool.get(next));
        return true;
    }

    /**
     * 동행이 한 사람에게 하는 한마디 (알림 상자에 "이름: 대사"), 그 사람의 퀘스트에 붙은 말로.
     * when: arrive(한 번) · idle(줄마다 한 번) · done(한 번) · talk(말을 걸었을 때) · fight · low. 시간 간격은 say() 가 본다.
     */
    public static boolean sayTo(CompanionEntity c, ServerPlayer o, String questId, String when) {
        long now = System.currentTimeMillis();
        switch (when) {
            case "arrive", "done" -> {
                if (said(o, questId, when)) return false;
                JsonObject ls = lines(questId);
                if (ls == null || !ls.has(when) || !ls.get(when).isJsonArray() || ls.getAsJsonArray(when).size() == 0) return false;
                mark(o, questId, when);
                speak(c, o, ls.getAsJsonArray(when).get(0));
                return true;
            }
            case "fight", "low" -> {
                return sayNext(c, o, questId, when, false);
            }
            case "idle" -> {
                return sayNext(c, o, questId, "idle", true);
            }
            case "talk" -> {
                if (now - c.lastSaid < 1500) return false;
                return sayNext(c, o, questId, "talk", false) || sayNext(c, o, questId, "idle", false);
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * 동행의 한마디를 함께 걷는 모두에게 (각자의 퀘스트에 붙은 말로). 어떤 말이든 7초 안에는 잇달아 하지 않고,
     * 싸울 때의 외침은 90초, 위태로울 때의 말은 60초에 한 번이다.
     */
    public static boolean say(CompanionEntity c, String when) {
        java.util.LinkedHashMap<ServerPlayer, String> group = c.group();
        if (group.isEmpty()) return false;
        long now = System.currentTimeMillis();
        if (now - c.lastSaid < SAY_GAP_MS && !"talk".equals(when)) return false;
        if ("fight".equals(when) && now - c.lastFight < FIGHT_GAP_MS) return false;
        if ("low".equals(when) && now - c.lastLow < LOW_GAP_MS) return false;
        boolean any = false;
        for (Map.Entry<ServerPlayer, String> e : group.entrySet()) any |= sayTo(c, e.getKey(), e.getValue(), when);
        if (any && "fight".equals(when)) c.lastFight = now;
        if (any && "low".equals(when)) c.lastLow = now;
        return any;
    }

    /** 살펴볼 흔적이 나타났다: 그 흔적이 동행의 퀘스트 것이면, 그 흔적에 붙은 한마디를 한다 (spot: {흔적 ID: 대사}). */
    public static void onSpotNear(ServerPlayer p, String questId, String spotId) {
        CompanionEntity c = of(p, questId);
        if (c == null) return;
        JsonObject ls = lines(questId);
        if (ls == null || !ls.has("spot") || !ls.get("spot").isJsonObject()) return;
        JsonObject by = ls.getAsJsonObject("spot");
        String sid = spotId.contains("@") ? spotId.substring(0, spotId.indexOf('@')) : spotId; // 흔적 ID 는 "이름@장소" 꼴일 수 있다
        String key = "spot:" + sid;
        com.google.gson.JsonElement line = by.has(sid) ? by.get(sid) : by.get(spotId);
        if (line == null || said(p, questId, key)) return;
        mark(p, questId, key);
        speak(c, p, line);
    }

    private static boolean calm(ServerPlayer p, CompanionEntity c, long now) {
        return c.getTarget() == null && p.tickCount - p.getLastHurtByMobTimestamp() > 400 && p.tickCount - p.getLastHurtMobTimestamp() > 400
                && now - c.lastFight > CALM_MS && now - c.lastSaid > CALM_MS;
    }

    /** 매초: 불러낼 동행이 있는지, 일이 끝났는지, 주인이 위태로운지·오래 서 있는지. */
    static void update(ServerPlayer p) {
        for (QuestDef q : QuestManager.activeQuests(p)) {
            if (q.companion == null || of(p, q.id) != null) continue;
            if (!QuestManager.atStructure(p, str(q.companion, "at", ""))) continue;
            if (com.rtshop.story.VoidGuard.guarded(p.serverLevel())) continue; // 기억의 문 안에는 따라 들어가지 않는다
            // 같은 사람이 이미 가까이에 서 있으면(다른 플레이어의 동행) 새로 부르지 않고 그 일행이 된다
            CompanionEntity there = nearby(p, str(q.companion, "npc", ""));
            if (there != null) join(p, q, there);
            else spawn(p, q);
        }
        Map<String, CompanionEntity> m = LIVE.get(p.getUUID());
        if (m == null || m.isEmpty()) return;
        long now = System.currentTimeMillis();
        long[] st = IDLE.computeIfAbsent(p.getUUID(), k -> new long[]{now, 0, 0, 0});
        long here = p.blockPosition().asLong();
        if (st[1] != here) {
            st[1] = here;
            st[0] = now;
        }
        for (Map.Entry<String, CompanionEntity> en : new java.util.ArrayList<>(m.entrySet())) {
            CompanionEntity c = en.getValue();
            String quest = en.getKey(); // 이 사람의 퀘스트 (일행이면 동행의 퀘스트와 다를 수 있다)
            if (!c.isAlive()) continue;
            if (p.getHealth() < p.getMaxHealth() * 0.35F) {
                say(c, "low");
            } else if (QuestManager.objectivesDone(p, quest)) {
                if (now - c.lastSaid >= SAY_GAP_MS) sayTo(c, p, quest, "done");
            } else if (now - st[0] > IDLE_FIRST_MS && now - st[3] > IDLE_GAP_MS && calm(p, c, now)) {
                if (now - c.lastSaid >= SAY_GAP_MS && sayTo(c, p, quest, "idle")) st[3] = now;
                st[0] = now;
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer sp && sp.tickCount % 20 == 13) update(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        Map<String, CompanionEntity> m = LIVE.remove(id);
        if (m != null) m.values().forEach(c -> {
            if (!c.isAlive()) return;
            if (id.equals(c.ownerId)) {
                if (!handOver(c, null)) leave(c, null); // 남은 일행이 있으면 이어받는다
            } else {
                c.guests.remove(id);
            }
        });
        IDLE.remove(event.getEntity().getUUID());
    }
}
