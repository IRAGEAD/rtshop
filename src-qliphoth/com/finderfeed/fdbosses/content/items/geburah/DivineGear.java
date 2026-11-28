/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.FDTargetFinder
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.items.geburah;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray.GeburahRayOptions;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.FDTargetFinder;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DivineGear
extends FDEntity
implements AutoSerializable {
    @SerializableField
    private int cooldown = -1;
    private int attackTime = 0;
    @SerializableField
    private int age = 0;
    private UUID owner;

    public static void summon(Player player, Vec3 pos) {
        DivineGear divineGear = new DivineGear(BossEntities.DIVINE_GEAR.get(), player.m_9236_());
        divineGear.owner = player.m_20148_();
        divineGear.m_146884_(pos);
        player.m_9236_().m_7967_((Entity)divineGear);
    }

    public DivineGear(EntityType<?> type, Level level) {
        super(type, level);
        this.getAnimationSystem().startAnimation("SUMMON", AnimationTicker.builder(BossAnims.DIVINE_GEAR_SUMMON).setToNullTransitionTime(0).build());
        this.getAnimationSystem().startAnimation("IDLE", AnimationTicker.builder(BossAnims.DIVINE_GEAR_IDLE).setToNullTransitionTime(0).build());
    }

    public void m_8119_() {
        super.m_8119_();
        Vec3 corePos = this.m_20182_().m_82520_(0.0, 1.65, 0.0);
        if (!this.m_9236_().f_46443_) {
            Player owner = this.getOwner();
            if (owner == null) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
                return;
            }
            if (this.cooldown == -1) {
                this.cooldown = 20;
            }
            int lifetime = BossConfigs.BOSS_CONFIG.get().itemConfig.divineGearLifetime;
            if (this.age > ((Animation)BossAnims.DIVINE_GEAR_SUMMON.get()).getAnimTime() && this.age < lifetime) {
                if (this.cooldown > 0) {
                    --this.cooldown;
                } else {
                    LivingEntity target = this.getTarget();
                    if (target != null) {
                        if (this.attackTime == 0) {
                            this.sendChargeParticles(10);
                        } else if (this.attackTime > 10) {
                            Vec3 targetLocation = target.m_20191_().m_82399_();
                            Vec3 between = corePos.m_82546_(targetLocation);
                            GeburahRayOptions options = GeburahRayOptions.builder().end(targetLocation).color(1.0f, 0.8f, 0.2f).time(0, 2, 7).width(0.25f).build();
                            FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)corePos, (double)120.0);
                            BossUtil.divineGearRayParticles((ServerLevel)this.m_9236_(), targetLocation, 120.0, between);
                            PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().amplitude(1.0f).stayTime(0).outTime(10).frequency(10.0f).build(), (Vec3)targetLocation, (double)40.0);
                            this.m_9236_().m_6263_(null, corePos.f_82479_, corePos.f_82480_, corePos.f_82481_, BossSounds.GEBURAH_CORE_RAY_STRIKE.get(), SoundSource.HOSTILE, 4.0f, 1.0f);
                            target.f_19802_ = 0;
                            target.m_6469_(this.m_9236_().m_269111_().m_269075_(owner), (float)BossConfigs.BOSS_CONFIG.get().itemConfig.divineGearDamage);
                            this.attackTime = -1;
                            this.setOnCooldown();
                        }
                        ++this.attackTime;
                    } else {
                        this.attackTime = 0;
                    }
                }
            } else if (this.age == lifetime) {
                this.sendChargeParticles(10);
            }
            int animTime = ((Animation)BossAnims.DIVINE_GEAR_DEATH.get()).getAnimTime() - 10;
            if (this.age > lifetime && this.age < lifetime + animTime) {
                this.getAnimationSystem().startAnimation("SUMMON", AnimationTicker.builder(BossAnims.DIVINE_GEAR_DEATH).setLoopMode(Animation.LoopMode.HOLD_ON_LAST_FRAME).setToNullTransitionTime(0).build());
            } else if (this.age >= lifetime + animTime) {
                BallParticleOptions ballParticle = BallParticleOptions.builder().color(1.0f, 0.7f, 0.2f).scalingOptions(0, 0, 20).brightness(2).size(1.0f).friction(0.7f).build();
                ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ballParticle, corePos.f_82479_, corePos.f_82480_, corePos.f_82481_, 30, (double)0.05f, (double)0.05f, (double)0.05f, 0.5);
                ballParticle.size = 0.5f;
                ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ballParticle, corePos.f_82479_, corePos.f_82480_, corePos.f_82481_, 30, (double)0.05f, (double)0.05f, (double)0.05f, 0.5);
                ballParticle.size = 0.25f;
                ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ballParticle, corePos.f_82479_, corePos.f_82480_, corePos.f_82481_, 30, (double)0.05f, (double)0.05f, (double)0.05f, 0.5);
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            }
            ++this.age;
        } else if (this.f_19797_ % 2 == 0) {
            float size = Mth.m_14036_((float)((float)this.f_19797_ / (float)((Animation)BossAnims.DIVINE_GEAR_SUMMON.get()).getAnimTime() * 2.0f), (float)0.0f, (float)1.0f);
            BallParticleOptions ballParticle = BallParticleOptions.builder().color(1.0f, 0.8f, 0.3f).scalingOptions(0, 0, 10).brightness(1).size(size).build();
            this.m_9236_().m_6493_((ParticleOptions)ballParticle, true, corePos.f_82479_, corePos.f_82480_, corePos.f_82481_, 0.0, 0.0, 0.0);
        }
    }

    public Player getOwner() {
        Level level;
        if (this.owner != null && (level = this.m_9236_()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            return serverLevel.m_46003_(this.owner);
        }
        return null;
    }

    public void setOnCooldown() {
        this.cooldown = BossConfigs.BOSS_CONFIG.get().itemConfig.divineGearAttackCooldown;
    }

    private LivingEntity getTarget() {
        ArrayList<Mob> entities = FDTargetFinder.getEntitiesInSphere(Mob.class, (Level)this.m_9236_(), (Vec3)this.getCorePos(), (float)BossConfigs.BOSS_CONFIG.get().itemConfig.divineGearAttackRadius, living -> true);
        if ((entities = new ArrayList<Mob>(entities.stream().filter(living -> {
            if (living.m_21224_()) {
                return false;
            }
            ClipContext clipContext = new ClipContext(this.getCorePos(), living.m_20191_().m_82399_(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
            BlockHitResult res = this.m_9236_().m_45547_(clipContext);
            return res.m_6662_() == HitResult.Type.MISS;
        }).toList())).isEmpty()) {
            return null;
        }
        Player owner = this.getOwner();
        if (entities.contains(owner.m_21214_())) {
            return owner.m_21214_();
        }
        entities.sort(Comparator.comparingDouble(living -> living.m_20191_().m_82399_().m_82554_(this.getCorePos())));
        return (LivingEntity)entities.get(0);
    }

    private Vec3 getCorePos() {
        Vec3 corePos = this.m_20182_().m_82520_(0.0, 1.65, 0.0);
        return corePos;
    }

    private void sendChargeParticles(int prepareTime) {
        for (int i = 0; i < 3; ++i) {
            StripeParticleOptions stripeParticleOptions = StripeParticleOptions.createHorizontalCircling(new FDColor(1.0f, 0.3f, 0.2f, 1.0f), new FDColor(1.0f, 0.6f, 0.2f, 1.0f), new Vec3(0.0, 1.0, 0.0), (float)i / 3.0f * (float)Math.PI * 2.0f, 0.1f, prepareTime, 40, 0.0f, 3.0f, 0.5f, 0.75f, true, true);
            FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)stripeParticleOptions, (Vec3)this.getCorePos(), (double)120.0);
        }
    }

    protected void m_8097_() {
    }

    protected void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        if (tag.m_128441_("playerOwner")) {
            this.owner = tag.m_128342_("playerOwner");
        }
    }

    protected void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
        if (this.owner != null) {
            tag.m_128362_("playerOwner", this.owner);
        }
    }
}

