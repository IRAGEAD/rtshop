/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.TransitionAnimation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer$Mode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.IHasHead
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackChain
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackInstance
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.PathfinderMob
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
 *  net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
 *  net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
 *  net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_warrior;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.base.BossMonsterMob;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBuddy;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthDamageSource;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake.MalkuthEarthquake;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.TransitionAnimation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.IHasHead;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackChain;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackInstance;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class MalkuthWarriorEntity
extends BossMonsterMob
implements IHasHead<MalkuthWarriorEntity>,
MalkuthBossBuddy {
    public static float DISTANCE_TO_ATTACK = 1.75f;
    public static final EntityDataAccessor<MalkuthAttackType> WARRIOR_TYPE = SynchedEntityData.m_135353_(MalkuthWarriorEntity.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    public static final String SIMPLE_HIT = "simple_axe_hit";
    public static final String EARTH_SLAM_ATTACK = "earth_slam_attack";
    public static final String MAIN_LAYER = "main";
    private static FDModel CLIENT_MODEL;
    private boolean wasNormalAttackCancelled = false;
    private int slamCooldown = 0;
    public AttackChain attackChain;

    public MalkuthWarriorEntity(EntityType<? extends BossMonsterMob> type, Level level, MalkuthAttackType initialType) {
        super(type, level);
        this.f_21364_ = 20;
        this.f_19804_.m_135381_(WARRIOR_TYPE, (Object)initialType);
        if (level.f_46443_ && CLIENT_MODEL == null) {
            CLIENT_MODEL = new FDModel(BossModels.MALKUTH_WARRIOR.get());
        }
        this.f_21365_ = new HeadControllerContainer((Mob)this).addHeadController(CLIENT_MODEL, "head", true);
        this.attackChain = new AttackChain(this.m_9236_().f_46441_).registerAttack(SIMPLE_HIT, this::simpleAxeAttack).registerAttack(EARTH_SLAM_ATTACK, this::earthSlamAttack).addAlwaysTryCastAttack(this::canCastSlamAttack, EARTH_SLAM_ATTACK).addAttack(0, SIMPLE_HIT);
        this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_IDLE).build());
    }

    public boolean earthSlamAttack(AttackInstance instance) {
        LivingEntity target = this.m_5448_();
        int stage = instance.stage;
        int tick = instance.tick;
        if (stage == 0) {
            if (target == null) {
                return true;
            }
            this.m_21573_().m_26573_();
            this.m_7618_(EntityAnchorArgument.Anchor.EYES, target.m_20182_());
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_SLAM_EARTH).important().nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_IDLE).build()).build());
            instance.nextStage();
            this.getHeadControllerContainer().setControllersMode(HeadControllerContainer.Mode.ANIMATION);
        } else if (stage == 1) {
            if (tick == 14) {
                if (target != null) {
                    this.m_7618_(EntityAnchorArgument.Anchor.EYES, target.m_20182_());
                }
            } else if (tick == 17) {
                double dist = 13.0;
                Vec3 forward = this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(dist, dist, dist);
                SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(this.m_20097_(), this.m_20182_().m_82520_(0.0, 0.5, 0.0), forward.m_82541_()).maxAngle((float)Math.PI).maxSpeed(0.5f).collectRadius(2).maxParticleLifetime(30).count(40).maxVerticalSpeedEdges(0.15f).maxVerticalSpeedCenter(0.4f));
                FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)packet);
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_() + (double)(this.m_20206_() / 2.0f), this.m_20189_(), BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
                float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.malkuthWarriorEarthSlamDamage);
                MalkuthEarthquake malkuthEarthquake = MalkuthEarthquake.summon(this.m_9236_(), (MalkuthAttackType)((Object)this.f_19804_.m_135370_(WARRIOR_TYPE)), this.m_20182_(), forward, 20, 0.5235988f, damage);
            } else if (tick >= 25) {
                this.wasNormalAttackCancelled = false;
                this.slamCooldown = 20;
                this.getHeadControllerContainer().setControllersMode(HeadControllerContainer.Mode.LOOK);
                return true;
            }
        }
        return false;
    }

    public boolean canCastSlamAttack() {
        LivingEntity target = this.m_5448_();
        if (target != null) {
            float distance = target.m_20270_((Entity)this);
            if (distance <= DISTANCE_TO_ATTACK || distance >= 10.0f) {
                return false;
            }
            double yDiff = this.m_20186_() - target.m_20186_();
            if (yDiff > (double)-0.1f && yDiff < 1.5) {
                return this.wasNormalAttackCancelled && this.slamCooldown <= 0;
            }
        }
        return false;
    }

    public boolean simpleAxeAttack(AttackInstance attackInstance) {
        LivingEntity target = this.m_5448_();
        int stage = attackInstance.stage;
        int tick = attackInstance.tick;
        if (stage == 0) {
            this.wasNormalAttackCancelled = false;
            if (target != null) {
                Vec3 targetPos = target.m_20182_();
                double dist = this.m_20182_().m_82554_(targetPos);
                if (dist > (double)DISTANCE_TO_ATTACK) {
                    if ((double)this.m_9236_().f_46441_.m_188501_() < 0.015) {
                        this.wasNormalAttackCancelled = true;
                        return true;
                    }
                    if (this.m_9236_().m_46467_() % 5L == 0L) {
                        this.m_21573_().m_5624_((Entity)target, 1.0);
                    }
                } else {
                    this.m_21573_().m_26573_();
                    this.getHeadControllerContainer().setControllersMode(HeadControllerContainer.Mode.ANIMATION);
                    this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_ATTACK).important().nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_IDLE).build()).build());
                    attackInstance.nextStage();
                }
                this.m_21563_().m_148051_((Entity)target);
            }
        } else if (stage == 1) {
            if (tick == 0 && target != null) {
                this.m_7618_(EntityAnchorArgument.Anchor.EYES, target.m_20182_());
            }
            if (tick == 5) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_() + (double)(this.m_20206_() / 2.0f), this.m_20189_(), BossSounds.MALKUTH_SLASH.get(), SoundSource.HOSTILE, 1.0f, 1.0f + this.f_19796_.m_188501_() * 0.5f);
            } else if (tick == 10) {
                float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.malkuthWarriorDamage);
                MalkuthAttackType type = (MalkuthAttackType)((Object)this.f_19804_.m_135370_(WARRIOR_TYPE));
                for (LivingEntity t : this.getTargetsForAxeAttack()) {
                    t.m_6469_((DamageSource)new MalkuthDamageSource(this.m_9236_().m_269111_().m_269264_(), type, 50), damage);
                }
            } else if (tick >= 15) {
                this.getHeadControllerContainer().setControllersMode(HeadControllerContainer.Mode.LOOK);
                return true;
            }
        }
        return false;
    }

    private List<LivingEntity> getTargetsForAxeAttack() {
        Vec3 forward = this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_();
        float cylinderOffset = 1.25f;
        Vec3 cylinderStart = this.m_20182_().m_82549_(forward.m_82542_((double)cylinderOffset, 0.0, (double)cylinderOffset)).m_82520_(0.0, -1.01, 0.0);
        List<LivingEntity> list = BossTargetFinder.getEntitiesInCylinder(LivingEntity.class, this.m_9236_(), cylinderStart, 2.5f, 0.75f, entity -> !(entity instanceof MalkuthBossBuddy));
        Vec2 direction = new Vec2((float)forward.f_82479_, (float)forward.f_82481_);
        List<LivingEntity> list2 = BossTargetFinder.getEntitiesInArc(LivingEntity.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -1.01, 0.0), direction, 1.5707964f, 2.5f, 1.0f, entity -> !(entity instanceof MalkuthBossBuddy));
        list.addAll(list2);
        return list;
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(WARRIOR_TYPE, (Object)MalkuthAttackType.FIRE);
    }

    protected void m_8099_() {
        super.m_8099_();
        this.f_21345_.m_25352_(6, (Goal)new WaterAvoidingRandomStrollGoal((PathfinderMob)this, 1.0){

            public void m_8037_() {
                super.m_8037_();
                Vec3 lookAt = this.f_25725_.m_20182_().m_82549_(this.f_25725_.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(100.0, 100.0, 100.0));
                this.f_25725_.m_21563_().m_24964_(lookAt);
            }

            public boolean m_8036_() {
                return super.m_8036_() && MalkuthWarriorEntity.super.m_5448_() == null;
            }
        });
        this.f_21345_.m_25352_(7, (Goal)new LookAtPlayerGoal((Mob)this, Player.class, 6.0f){

            public boolean m_8036_() {
                return super.m_8036_() && MalkuthWarriorEntity.super.m_5448_() == null;
            }
        });
        this.f_21345_.m_25352_(8, (Goal)new RandomLookAroundGoal((Mob)this){

            public boolean m_8036_() {
                return super.m_8036_() && MalkuthWarriorEntity.super.m_5448_() == null;
            }
        });
        this.f_21345_.m_25352_(2, (Goal)new NearestAttackableTargetGoal((Mob)this, Player.class, true));
    }

    @Override
    public void m_8119_() {
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            if (!this.m_21224_()) {
                this.getHeadControllerContainer().clientTick();
                AnimationTicker ticker = this.getAnimationSystem().getTicker(MAIN_LAYER);
                if (ticker == null) {
                    this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_IDLE).build());
                } else {
                    Animation anim = ticker.getAnimation();
                    if (anim instanceof TransitionAnimation) {
                        TransitionAnimation tr = (TransitionAnimation)anim;
                        anim = tr.getTransitionTo();
                    }
                    if (anim == BossAnims.MALKUTH_WARRIOR_IDLE.get()) {
                        if (this.f_267362_.m_267731_() > 0.05f) {
                            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_WALK).setToNullTransitionTime(5).setSpeed(2.0f).build());
                        }
                    } else if (anim == BossAnims.MALKUTH_WARRIOR_WALK.get() && this.f_267362_.m_267731_() < 0.05f) {
                        this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_IDLE).build());
                    }
                }
            }
        } else {
            if (this.f_19797_ == 1) {
                Vec3 forward = this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(100.0, 100.0, 100.0);
                this.m_21563_().m_24964_(this.m_20182_().m_82549_(forward).m_82520_(0.0, (double)this.m_20192_(), 0.0));
            }
            if (!this.m_21224_()) {
                this.slamCooldown = FDMathUtil.clamp((int)(this.slamCooldown - 1), (int)0, (int)Integer.MAX_VALUE);
                this.attackChain.tick();
                this.m_146922_(this.f_20883_);
            }
        }
    }

    public void m_6667_(DamageSource p_21014_) {
        super.m_6667_(p_21014_);
        if (!this.m_9236_().f_46443_) {
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_WARRIOR_DIE).build());
        }
    }

    protected SoundEvent m_7975_(DamageSource p_33034_) {
        return BossSounds.MALKUTH_WARRIOR_HIT.get();
    }

    protected SoundEvent m_5592_() {
        return BossSounds.MALKUTH_WARRIOR_HIT.get();
    }

    @Override
    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        tag.m_128359_("mtype", ((MalkuthAttackType)((Object)this.f_19804_.m_135370_(WARRIOR_TYPE))).name());
    }

    @Override
    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        if (tag.m_128441_("mtype")) {
            this.f_19804_.m_135381_(WARRIOR_TYPE, (Object)MalkuthAttackType.valueOf(tag.m_128461_("mtype")));
        }
    }

    public HeadControllerContainer<MalkuthWarriorEntity> getHeadControllerContainer() {
        return (HeadControllerContainer)this.f_21365_;
    }

    public boolean m_6573_(Player p_21418_) {
        return false;
    }
}

