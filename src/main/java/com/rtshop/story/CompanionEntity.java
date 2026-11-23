package com.rtshop.story;

import com.rtshop.quest.QuestManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 동행 NPC: 동행 퀘스트를 가진 플레이어 곁에 따로 나타나 따라다니고, 함께 싸운다.
 * 마을에 서 있는 NPC 와는 별개의 존재이고, 세계에 저장되지 않는다 (퀘스트가 끝나거나 주인이 떠나면 사라진다).
 * 죽지 않는다: 쓰러질 만큼 맞으면 잠시 무릎을 꿇었다가 다시 일어난다.
 */
public class CompanionEntity extends PathfinderMob implements VillagerDataHolder {
    private static final EntityDataAccessor<VillagerData> DATA = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.VILLAGER_DATA);
    private static final EntityDataAccessor<Boolean> DOWNED = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int DOWN_TICKS = 120;

    UUID ownerId;
    String questId = "";
    String npcId = "";
    String style = "melee";
    private int downed;
    private int boltCooldown;
    long lastSaid;
    boolean saidFight;

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 5));
        entityData.define(DOWNED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() {
                return "melee".equals(style) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return "melee".equals(style) && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(3, new FollowOwner());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8F));
    }

    // ------------------------------------------------------------------ 주인 · 겉모습

    public ServerPlayer owner() {
        ServerPlayer o = ownerId == null || getServer() == null ? null : getServer().getPlayerList().getPlayer(ownerId);
        return o != null ? o : testOwner;
    }

    /** 점검용 가짜 플레이어는 접속 목록에 없어서 따로 붙잡아 둔다. */
    ServerPlayer testOwner;

    public boolean isDowned() {
        return entityData.get(DOWNED);
    }

    @Override
    public VillagerData getVillagerData() {
        return entityData.get(DATA);
    }

    @Override
    public void setVillagerData(VillagerData d) {
        entityData.set(DATA, d);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player p) {
        return false;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || downed > 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    // ------------------------------------------------------------------ 맞기 · 쓰러지기

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (downed > 0 || source.getEntity() instanceof Player || source.getEntity() instanceof CompanionEntity) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) { // 공허·/kill: 조용히 사라진다
            discard();
            return false;
        }
        if (getHealth() - amount <= 1F) {
            setHealth(1F);
            downed = DOWN_TICKS;
            entityData.set(DOWNED, true);
            setTarget(null);
            getNavigation().stop();
            playSound(SoundEvents.VILLAGER_HURT, 1F, 0.8F);
            Companions.say(this, "low");
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return downed <= 0 && super.canBeSeenAsEnemy();
    }

    // ------------------------------------------------------------------ 매 틱

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (downed > 0) {
            if (--downed == 0) {
                setHealth(getMaxHealth());
                entityData.set(DOWNED, false);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 8, 0.3, 0.4, 0.3, 0);
            }
            return;
        }
        if (tickCount % 20 == 3) {
            ServerPlayer o = owner();
            if (o == null || !o.isAlive() || o.level() != level() || !QuestManager.isActive(o, questId)) {
                Companions.leave(this, o);
                return;
            }
            double d = distanceToSqr(o);
            if (d > 28 * 28) {
                teleportTo(o.getX() + (random.nextDouble() - 0.5) * 3, o.getY(), o.getZ() + (random.nextDouble() - 0.5) * 3);
                getNavigation().stop();
            }
            if (tickCount % 40 == 3 && getHealth() < getMaxHealth()) heal(2F);
            pickTarget(o);
        }
        if ("bolt".equals(style)) boltTick();
    }

    /** 싸울 상대: 주인을 노리는 몬스터, 주인이 방금 때린 것, 주인을 방금 때린 것. */
    private void pickTarget(ServerPlayer o) {
        if ("none".equals(style)) return;
        LivingEntity cur = getTarget();
        if (cur != null && cur.isAlive() && cur.distanceToSqr(o) < 20 * 20) return;
        LivingEntity best = null;
        LivingEntity hit = o.getLastHurtMob();
        if (hit != null && hit.isAlive() && o.tickCount - o.getLastHurtMobTimestamp() < 100 && ok(hit)) best = hit;
        LivingEntity by = o.getLastHurtByMob();
        if (best == null && by != null && by.isAlive() && ok(by)) best = by;
        if (best == null) {
            List<Mob> near = level().getEntitiesOfClass(Mob.class, o.getBoundingBox().inflate(12, 5, 12),
                    m -> m.isAlive() && m instanceof Enemy && (m.getTarget() == o || m.getTarget() == this) && ok(m));
            double bd = Double.MAX_VALUE;
            for (Mob m : near) {
                double dd = m.distanceToSqr(this);
                if (dd < bd) {
                    bd = dd;
                    best = m;
                }
            }
        }
        setTarget(best);
        if (best != null && !saidFight) {
            saidFight = true;
            Companions.say(this, "fight");
        }
        if (best == null) saidFight = false;
    }

    private boolean ok(LivingEntity e) {
        return e != this && !(e instanceof Player) && !(e instanceof CompanionEntity) && !e.isInvulnerable();
    }

    /** bolt 방식: 다가가지 않고 떨어져서 빛줄기를 쏜다. */
    private void boltTick() {
        LivingEntity t = getTarget();
        if (boltCooldown > 0) boltCooldown--;
        if (t == null || !t.isAlive()) return;
        getLookControl().setLookAt(t, 30F, 30F);
        double d = distanceToSqr(t);
        if (d > 14 * 14 || !hasLineOfSight(t)) {
            getNavigation().moveTo(t, 1.0);
            return;
        }
        if (d < 5 * 5) getNavigation().stop();
        if (boltCooldown > 0) return;
        boltCooldown = 30;
        swing(InteractionHand.MAIN_HAND);
        t.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (level() instanceof ServerLevel sl) {
            Vec3 a = getEyePosition(), b = t.getBoundingBox().getCenter();
            for (int i = 1; i <= 8; i++) {
                Vec3 q = a.lerp(b, i / 8.0);
                sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.3F);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND && player.getUUID().equals(ownerId)) {
            lastSaid = 0;
            Companions.say(this, "idle");
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** 주인을 따라간다: 4칸 넘게 벌어지면 걷고, 2.5칸 안이면 멈춘다. 싸우는 중에는 쉬되 너무 멀어지면 돌아온다. */
    private final class FollowOwner extends Goal {
        private int recalc;

        FollowOwner() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            ServerPlayer o = owner();
            if (o == null || downed > 0) return false;
            double d = distanceToSqr(o);
            if (getTarget() != null && getTarget().isAlive()) return d > 16 * 16;
            return d > 4 * 4;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer o = owner();
            return o != null && downed <= 0 && distanceToSqr(o) > 2.5 * 2.5 && !(getTarget() != null && distanceToSqr(o) < 8 * 8);
        }

        @Override
        public void tick() {
            ServerPlayer o = owner();
            if (o == null) return;
            getLookControl().setLookAt(o, 10F, 30F);
            if (--recalc <= 0) {
                recalc = 10;
                getNavigation().moveTo(o, distanceToSqr(o) > 10 * 10 ? 1.35 : 1.05);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
