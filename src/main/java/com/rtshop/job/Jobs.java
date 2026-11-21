package com.rtshop.job;

import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.net.JobOpenPacket;
import com.rtshop.net.ResultPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 직업: 전투 직업 1개 + 생활 직업 1개. 고르는 즉시 패시브가 적용된다.
 * 처음 고를 때는 무료, 바꿀 때는 Gold 를 내고 바로 바뀌며, 바꾼 뒤에는 정해진 시간 동안 그 칸을 다시 바꿀 수 없다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Jobs {
    private Jobs() {
    }

    public static final String NPC_TAG = "rtshop_job";
    /** 점검용: 시계를 앞으로 돌린다 (쿨타임 확인). */
    public static long testClockOffset = 0;

    public static long now() {
        return System.currentTimeMillis() + testClockOffset;
    }

    private static final Map<UUID, Integer> SESSIONS = new HashMap<>();

    // ------------------------------------------------------------------ 조회

    public static JobData.Entry entry(ServerPlayer p) {
        return JobData.get(p.server).of(p.getUUID());
    }

    public static boolean has(ServerPlayer p, String jobId) {
        JobData.Entry e = entry(p);
        return jobId.equals(e.combat) || jobId.equals(e.life);
    }

    /** 지금 가진 두 직업(전투·생활)의 이 패시브 수치 합. */
    public static double stat(ServerPlayer p, String key) {
        if (p == null) {
            return 0;
        }
        JobConfig cfg = JobConfig.get();
        JobData.Entry e = entry(p);
        double v = 0;
        JobConfig.JobDef c = cfg.job(e.combat);
        if (c != null) {
            v += c.param(key, 0);
        }
        JobConfig.JobDef l = cfg.job(e.life);
        if (l != null) {
            v += l.param(key, 0);
        }
        return v;
    }

    /** 전투 직업의 무기 상성: {피해 증감, 공격 속도 증감}. 전투 직업이 없으면 0. */
    public static double[] weapon(ServerPlayer p, String type) {
        JobConfig cfg = JobConfig.get();
        JobConfig.JobDef c = cfg.job(entry(p).combat);
        if (c == null) {
            return new double[]{0, 0};
        }
        return new double[]{c.weaponDamage(type), c.weaponSpeed(type)};
    }

    /** 이 직업의 패시브 수치. 그 직업이 아니면 0. */
    public static double param(ServerPlayer p, String jobId, String key) {
        if (p == null || !has(p, jobId)) {
            return 0;
        }
        JobConfig.JobDef d = JobConfig.get().job(jobId);
        return d == null ? 0 : d.param(key, 0);
    }

    /** 이 표식을 보상으로 주는 퀘스트. 없으면 null. */
    public static QuestDef questGiving(String flag) {
        if (flag == null || flag.isEmpty()) {
            return null;
        }
        for (QuestDef q : QuestManager.loaded().quests.values()) {
            if (q.rewardFlags.contains(flag)) {
                return q;
            }
            for (QuestDef.Choice c : q.choices) {
                if (c.flags.contains(flag)) {
                    return q;
                }
            }
        }
        return null;
    }

    /** 퀘스트 줄기 이름 ("검사 선행 퀘스트 (4/4)" -> "검사 선행 퀘스트"), 없으면 제목. */
    public static String chainName(QuestDef q) {
        if (q.chain == null || q.chain.isEmpty()) {
            return q.title;
        }
        return q.chain.replaceAll("\\s*\\(\\d+/\\d+\\)\\s*$", "");
    }

    public static boolean unlocked(ServerPlayer p, JobConfig.JobDef d) {
        if (d.unlockFlag == null || d.unlockFlag.isEmpty() || QuestManager.hasFlag(p, d.unlockFlag)) {
            return true;
        }
        return questGiving(d.unlockFlag) == null && !d.hidden; // 아직 선행 퀘스트가 없는 직업은 바로 고를 수 있다
    }

    /** 이 칸을 지금 바꿀 수 있을 때까지 남은 시간(ms). 0 이면 바로 가능. */
    public static long cooldownLeft(ServerPlayer p, String slot) {
        JobData.Entry e = entry(p);
        long at = e.changedAt(slot);
        if (at <= 0 || e.get(slot).isEmpty()) {
            return 0;
        }
        return Math.max(0, at + JobConfig.get().cooldownMillis(slot) - now());
    }

    public static String duration(long ms) {
        long min = (ms + 59_999) / 60_000;
        long h = min / 60;
        long m = min % 60;
        if (h >= 24) {
            return (h / 24) + "일 " + (h % 24) + "시간";
        }
        return h > 0 ? h + "시간 " + m + "분" : m + "분";
    }

    // ------------------------------------------------------------------ 고르기 / 바꾸기

    public record Result(boolean ok, Component message) {
    }

    public static Result choose(ServerPlayer p, String jobId) {
        JobConfig cfg = JobConfig.get();
        JobConfig.JobDef d = cfg.job(jobId);
        if (d == null) {
            return new Result(false, Component.translatable("rtshop.job.unknown", jobId));
        }
        if (!unlocked(p, d)) {
            QuestDef q = questGiving(d.unlockFlag);
            return new Result(false, q == null ? Component.translatable("rtshop.job.locked", d.name)
                    : Component.translatable("rtshop.job.locked_quest", d.name, chainName(q)));
        }
        JobData data = JobData.get(p.server);
        JobData.Entry e = data.of(p.getUUID());
        String current = e.get(d.slot);
        if (jobId.equals(current)) {
            return new Result(false, Component.translatable("rtshop.job.already", d.name));
        }
        if (current.isEmpty()) {
            e.set(d.slot, jobId, 0);
            data.dirty();
            applyAttributes(p);
            RtShop.LOGGER.info("[rtshop] {} 이(가) 직업 '{}' 을(를) 골랐습니다", p.getGameProfile().getName(), d.name);
            com.rtshop.land.StarterPlots.check(p);
            return new Result(true, Component.translatable("rtshop.job.chosen", d.name));
        }
        long left = cooldownLeft(p, d.slot);
        if (left > 0) {
            return new Result(false, Component.translatable("rtshop.job.cooldown", duration(left)));
        }
        long cost = cfg.cost(d.slot);
        if (Economy.wallet(p) < cost) {
            return new Result(false, Component.translatable("rtshop.job.no_money", String.format("%,d", cost)));
        }
        Economy.addWallet(p, -cost);
        JobConfig.JobDef old = cfg.job(current);
        e.set(d.slot, jobId, now());
        data.dirty();
        applyAttributes(p);
        com.rtshop.land.StarterPlots.check(p);
        RtShop.LOGGER.info("[rtshop] {} 이(가) 직업을 '{}' -> '{}' 로 바꿨습니다 ({} G)", p.getGameProfile().getName(),
                old == null ? current : old.name, d.name, cost);
        return new Result(true, Component.translatable("rtshop.job.changed", old == null ? current : old.name, d.name,
                String.format("%,d", cost), duration(cfg.cooldownMillis(d.slot))));
    }

    /** 관리자용: 비용·쿨타임·해금과 상관없이 바로 정한다. 빈 문자열이면 그 칸을 비운다. */
    public static void force(ServerPlayer p, String slot, String jobId) {
        JobData data = JobData.get(p.server);
        data.of(p.getUUID()).set(slot, jobId, 0);
        data.dirty();
        applyAttributes(p);
        com.rtshop.land.StarterPlots.check(p);
    }

    public static void resetCooldowns(ServerPlayer p) {
        JobData data = JobData.get(p.server);
        JobData.Entry e = data.of(p.getUUID());
        e.combatChangedAt = 0;
        e.lifeChangedAt = 0;
        data.dirty();
    }

    // ------------------------------------------------------------------ 화면

    public static JobOpenPacket view(ServerPlayer p, boolean atNpc) {
        JobConfig cfg = JobConfig.get();
        JobData.Entry e = entry(p);
        List<JobOpenPacket.JobView> list = new ArrayList<>();
        for (JobConfig.JobDef d : cfg.jobs.values()) {
            boolean open = unlocked(p, d);
            if (d.hidden && !open) {
                continue;
            }
            int state = d.id.equals(e.get(d.slot)) ? JobOpenPacket.CURRENT : (open ? JobOpenPacket.AVAILABLE : JobOpenPacket.LOCKED);
            String lock = "";
            if (!open) {
                QuestDef q = questGiving(d.unlockFlag);
                lock = q == null ? "" : chainName(q);
            }
            list.add(new JobOpenPacket.JobView(d.id, d.slot, d.name, d.icon, cfg.describe(d, com.rtshop.shop.PricingConfig.get().handCookedMargin), state, lock));
        }
        return new JobOpenPacket(atNpc, e.combat, e.life, cooldownLeft(p, JobConfig.COMBAT), cooldownLeft(p, JobConfig.LIFE),
                cfg.changeCostCombat, cfg.changeCostLife, cfg.cooldownHoursCombat, cfg.cooldownHoursLife, list);
    }

    public static void open(ServerPlayer p, int npcEntityId) {
        if (npcEntityId >= 0) {
            SESSIONS.put(p.getUUID(), npcEntityId);
        } else {
            SESSIONS.remove(p.getUUID());
        }
        Economy.sync(p);
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), view(p, npcEntityId >= 0));
    }

    private static boolean nearNpc(ServerPlayer p) {
        Integer id = SESSIONS.get(p.getUUID());
        if (id == null) {
            return false;
        }
        Entity e = p.level().getEntity(id);
        double r = EconomyConfig.get().npcRange;
        return e != null && e.isAlive() && e.getTags().contains(NPC_TAG) && e.distanceToSqr(p) <= r * r;
    }

    /** 화면에서 '이 직업 선택'을 눌렀을 때. */
    public static void handleChoose(ServerPlayer p, String jobId) {
        Result r;
        if (!nearNpc(p)) {
            r = new Result(false, Component.translatable("rtshop.job.need_npc", JobConfig.get().npcName));
        } else {
            r = choose(p, jobId);
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), view(p, nearNpc(p)));
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ResultPacket(r.message(), r.ok()));
    }

    public static Villager spawnNpc(ServerLevel level, Vec3 pos, float yaw) {
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) {
            return null;
        }
        v.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.addTag(NPC_TAG);
        v.setCustomName(Component.literal(JobConfig.get().npcName));
        v.setCustomNameVisible(true);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.ARMORER, 5));
        level.addFreshEntity(v);
        return v;
    }

    // ------------------------------------------------------------------ 능력치 패시브 (속성 수정치)

    private static final UUID SWORD_SPEED = UUID.fromString("6f1d2a40-5b1e-4c7e-9d3a-1a2b3c4d5e01");
    private static final UUID WARRIOR_HP = UUID.fromString("6f1d2a40-5b1e-4c7e-9d3a-1a2b3c4d5e02");
    private static final UUID TANK_KB = UUID.fromString("6f1d2a40-5b1e-4c7e-9d3a-1a2b3c4d5e03");
    private static final UUID MAGE_MANA = UUID.fromString("6f1d2a40-5b1e-4c7e-9d3a-1a2b3c4d5e04");
    private static final UUID MAGE_POWER = UUID.fromString("6f1d2a40-5b1e-4c7e-9d3a-1a2b3c4d5e05");
    private static final UUID MOVE_SPEED = UUID.fromString("6f1d2a40-5b1e-4c7e-9d3a-1a2b3c4d5e06");
    private static final Set<String> SPELL_SCHOOLS = Set.of("arcane", "fire", "frost", "healing", "lightning", "soul");

    private static void modifier(ServerPlayer p, Attribute attr, UUID id, String name, double amount, AttributeModifier.Operation op) {
        if (attr == null) {
            return;
        }
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) {
            return;
        }
        AttributeModifier cur = inst.getModifier(id);
        if (amount == 0) {
            if (cur != null) {
                inst.removeModifier(id);
            }
            return;
        }
        if (cur != null && cur.getAmount() == amount && cur.getOperation() == op) {
            return;
        }
        if (cur != null) {
            inst.removeModifier(id);
        }
        inst.addTransientModifier(new AttributeModifier(id, "rtshop job: " + name, amount, op));
    }

    private static Attribute attribute(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : ForgeRegistries.ATTRIBUTES.getValue(rl);
    }

    /** 직업에 따른 능력치 수정치를 맞춘다 (없는 직업의 수정치는 지운다). 0.5초마다, 그리고 직업을 바꿀 때 부른다. */
    public static void applyAttributes(ServerPlayer p) {
        JobConfig cfg = JobConfig.get();
        String type = cfg.weaponType(p.getMainHandItem());
        double speed = cfg.isMelee(type) ? weapon(p, type)[1] : 0;
        modifier(p, Attributes.ATTACK_SPEED, SWORD_SPEED, "weapon speed", speed, AttributeModifier.Operation.MULTIPLY_TOTAL);
        double hp = stat(p, "maxHealth");
        modifier(p, Attributes.MAX_HEALTH, WARRIOR_HP, "max health", hp, AttributeModifier.Operation.ADDITION);
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
        modifier(p, Attributes.MOVEMENT_SPEED, MOVE_SPEED, "move speed", stat(p, "moveSpeed"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p, Attributes.KNOCKBACK_RESISTANCE, TANK_KB, "knockback", stat(p, "knockbackResist"), AttributeModifier.Operation.ADDITION);
        modifier(p, attribute("irons_spellbooks:max_mana"), MAGE_MANA, "mage", stat(p, "maxMana"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        double power = stat(p, "spellPower");
        modifier(p, attribute("irons_spellbooks:spell_power"), MAGE_POWER, "mage", power, AttributeModifier.Operation.MULTIPLY_TOTAL);
        for (String school : SPELL_SCHOOLS) {
            modifier(p, attribute("spell_power:" + school), MAGE_POWER, "mage", power, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        // 채굴 속도는 클라이언트도 알아야 블록이 빨리 깨진다
        double mine = stat(p, "mineSpeed");
        Double sent = SENT_MINE.get(p.getUUID());
        if (sent == null || sent != mine) {
            SENT_MINE.put(p.getUUID(), mine);
            if (p.connection != null) {
                ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new com.rtshop.net.JobSyncPacket(mine));
            }
        }
    }

    private static final Map<UUID, Double> SENT_MINE = new HashMap<>();

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !(event.player instanceof ServerPlayer sp)) {
            return;
        }
        if (sp.tickCount % 10 == 0) {
            applyAttributes(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            applyAttributes(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            applyAttributes(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
        SENT_MINE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !event.getTarget().getTags().contains(NPC_TAG)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer sp) {
            open(sp, event.getTarget().getId());
        }
    }

    /** 다른 곳(ShopServer.isNpc 등)에서 쓰는 NPC 판별. */
    public static boolean isNpc(Entity e) {
        return e.getTags().contains(NPC_TAG);
    }

    /** ShopServer 가 쓰는 종류 이름: "job". */
    public static String npcKey() {
        return "job";
    }
}
