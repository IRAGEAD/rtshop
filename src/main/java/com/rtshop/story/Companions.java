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
    private static final Map<UUID, long[]> IDLE = new HashMap<>(); // {마지막으로 움직인 시각, x, z 를 합친 값, 마지막 low 대사}
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

    /** 동행이 떠난다. 퀘스트를 마쳐서 떠나는 길이면 작별 한마디. */
    static void leave(CompanionEntity c, ServerPlayer owner) {
        if (owner != null && QuestManager.isCompleted(owner, c.questId)) {
            c.lastSaid = 0;
            say(c, "done");
        }
        if (c.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.POOF, c.getX(), c.getY() + 1, c.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
        if (c.ownerId != null) {
            Map<String, CompanionEntity> m = LIVE.get(c.ownerId);
            if (m != null) m.remove(c.questId);
        }
        c.discard();
    }

    /** 동행의 한마디 (알림 상자에 "이름: 대사"). 같은 동행은 7초에 한 번만 말한다. */
    public static boolean say(CompanionEntity c, String when) {
        ServerPlayer o = c.owner();
        if (o == null) return false;
        QuestDef q = QuestManager.loaded().quests.get(c.questId);
        if (q == null || q.companion == null || !q.companion.has("lines")) return false;
        JsonObject lines = q.companion.getAsJsonObject("lines");
        if (!lines.has(when) || !lines.get(when).isJsonArray()) return false;
        JsonArray pool = lines.getAsJsonArray(when);
        long now = System.currentTimeMillis();
        if (pool.size() == 0 || now - c.lastSaid < SAY_GAP_MS) return false;
        c.lastSaid = now;
        String line = StoryText.t(o, Loc.pick(o, pool.get(c.getRandom().nextInt(pool.size()))));
        com.rtshop.notice.Notice.send(o, Component.literal(c.getName().getString()).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(": " + line).withStyle(ChatFormatting.WHITE)));
        return true;
    }

    /** 살펴볼 흔적이 나타났다: 곁의 동행이 한마디. */
    static void onSpotNear(ServerPlayer p) {
        Map<String, CompanionEntity> m = LIVE.get(p.getUUID());
        if (m == null) return;
        for (CompanionEntity c : m.values()) if (c.isAlive() && say(c, "spot")) return;
    }

    /** 매초: 불러낼 동행이 있는지, 주인이 위태로운지·오래 서 있는지. */
    static void update(ServerPlayer p) {
        for (QuestDef q : QuestManager.activeQuests(p)) {
            if (q.companion == null || of(p, q.id) != null) continue;
            if (!QuestManager.atStructure(p, str(q.companion, "at", ""))) continue;
            if (com.rtshop.story.VoidGuard.guarded(p.serverLevel())) continue; // 기억의 문 안에는 따라 들어가지 않는다
            spawn(p, q);
        }
        Map<String, CompanionEntity> m = LIVE.get(p.getUUID());
        if (m == null || m.isEmpty()) return;
        long now = System.currentTimeMillis();
        long[] st = IDLE.computeIfAbsent(p.getUUID(), k -> new long[]{now, 0, 0});
        long here = p.blockPosition().asLong();
        if (st[1] != here) {
            st[1] = here;
            st[0] = now;
        }
        for (CompanionEntity c : m.values()) {
            if (!c.isAlive()) continue;
            if (p.getHealth() < p.getMaxHealth() * 0.35F && now - st[2] > 40_000) {
                st[2] = now;
                c.lastSaid = 0;
                say(c, "low");
            } else if (now - st[0] > 30_000) {
                st[0] = now;
                say(c, "idle");
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
        Map<String, CompanionEntity> m = LIVE.remove(event.getEntity().getUUID());
        if (m != null) m.values().forEach(c -> {
            if (c.isAlive()) c.discard();
        });
        IDLE.remove(event.getEntity().getUUID());
    }
}
