/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDHelpers
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDTargetFinder
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.core.Direction
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$MoveFunction
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.items.malkuth;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.FDBossesServerScheduler;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_crush.MalkuthCrushAttack;
import com.finderfeed.fdbosses.content.items.malkuth.MalkuthFist;
import com.finderfeed.fdbosses.content.items.malkuth.PlayerForceAttackEntityPacket;
import com.finderfeed.fdbosses.content.util.Undismountable;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDHelpers;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDTargetFinder;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

public class MalkuthFistChain
extends FDEntity
implements Undismountable {
    public static final EntityDataAccessor<Boolean> IS_HOOK = SynchedEntityData.m_135353_(MalkuthFistChain.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    public static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.m_135353_(MalkuthFistChain.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    public static final EntityDataAccessor<Integer> HOOKED_TO = SynchedEntityData.m_135353_(MalkuthFistChain.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    private UUID ownerPlayer;
    private UUID hookFlyingTo;
    private Vec3 currentPlayerHookPos = null;
    private int pullingPlayerTickerTime = -1;
    private int pullingPlayerTicker = -1;
    private Vec3 vectorBetween;
    private Direction cachedDirection;
    public Vec3 cachedDeltaMovement;

    public static void summon(Player owner, boolean isHook) {
        MalkuthFistChain malkuthFistChain = new MalkuthFistChain(BossEntities.MALKUTH_FIST_CHAIN.get(), owner.m_9236_());
        malkuthFistChain.m_146884_(owner.m_146892_());
        malkuthFistChain.m_20088_().m_135381_(IS_HOOK, (Object)isHook);
        malkuthFistChain.ownerPlayer = owner.m_20148_();
        malkuthFistChain.m_20256_(owner.m_20154_().m_82490_(3.0));
        owner.m_9236_().m_7967_((Entity)malkuthFistChain);
    }

    public MalkuthFistChain(EntityType<?> type, Level level) {
        super(type, level);
        this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_FIST_FLYING).build());
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            LivingEntity hookedTo;
            if (this.ownerPlayer == null) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
                this.onStopChaining();
                return;
            }
            this.checkOwnerValidity();
            Player owner = this.getOwner();
            if (owner != null) {
                this.m_20088_().m_135381_(OWNER_ID, (Object)owner.m_19879_());
            }
            if ((hookedTo = this.getHookedTo()) != null) {
                this.m_20088_().m_135381_(HOOKED_TO, (Object)hookedTo.m_19879_());
            }
        }
        if (this.isHook()) {
            this.tickHook();
        } else {
            this.tickPullToBlocks();
        }
        LivingEntity hookedTo = this.getHookedTo();
        if (hookedTo == null) {
            this.m_146884_(this.m_20182_().m_82549_(this.m_20184_()));
        } else {
            Vec3 pos = this.getHookedToPos(hookedTo);
            this.m_6034_(pos.f_82479_, pos.f_82480_, pos.f_82481_);
        }
        this.tickPullingTime();
    }

    public Vec3 getHookedToPos(LivingEntity hookedTo) {
        return hookedTo.m_20191_().m_82399_();
    }

    public LivingEntity getHookedTo() {
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            Entity entity;
            ServerLevel serverLevel = (ServerLevel)level;
            if (this.hookFlyingTo != null && (entity = serverLevel.m_8791_(this.hookFlyingTo)) instanceof LivingEntity) {
                livingEntity = (LivingEntity)entity;
                return livingEntity;
            }
        } else {
            Entity entity = this.m_9236_().m_6815_(((Integer)this.m_20088_().m_135370_(HOOKED_TO)).intValue());
            if (entity instanceof LivingEntity) {
                livingEntity = (LivingEntity)entity;
                return livingEntity;
            }
        }
        return null;
    }

    public void m_20256_(Vec3 vec) {
        super.m_20256_(vec);
        if (!vec.equals((Object)Vec3.f_82478_)) {
            this.cachedDeltaMovement = vec;
        }
    }

    public void tickPullToBlocks() {
        if (!this.m_9236_().f_46443_) {
            Vec3 deltaMovement = this.m_20184_();
            if (!deltaMovement.equals((Object)Vec3.f_82478_)) {
                if (this.f_19797_ > 2) {
                    this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_CHAIN_PULL.get(), SoundSource.HOSTILE, 4.0f, 0.85f);
                }
                Vec3 start = this.m_20182_();
                Vec3 end = start.m_82549_(deltaMovement.m_82490_(2.0));
                ClipContext clipContext = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
                BlockHitResult res = this.m_9236_().m_45547_(clipContext);
                if (res.m_6662_() != HitResult.Type.MISS) {
                    Vec3 location = res.m_82450_();
                    this.m_6021_(location.f_82479_, location.f_82480_, location.f_82481_);
                    this.m_20256_(Vec3.f_82478_);
                    this.cachedDirection = res.m_82434_();
                    this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_FIST_GRAB).build());
                }
            } else {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_CHAIN_PULL.get(), SoundSource.HOSTILE, 4.0f, 0.9f);
                this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_FIST_GRAB).build());
                Player owner = this.getOwner();
                if (owner != null) {
                    if (owner.m_20202_() != this) {
                        if (!owner.m_7998_((Entity)this, true)) {
                            this.m_142467_(Entity.RemovalReason.DISCARDED);
                            this.onStopChaining();
                        }
                    } else if (this.pullingPlayerTickerTime != -1 && this.pullingPlayerTicker > this.pullingPlayerTickerTime + 1) {
                        owner.m_8127_();
                        int cooldownIKnowYouAreLookingHereToChangeThisField = BossConfigs.BOSS_CONFIG.get().itemConfig.iceFireGauntletCooldown;
                        owner.m_36335_().m_41524_(BossItems.MALKUTH_FIST.get(), cooldownIKnowYouAreLookingHereToChangeThisField);
                        this.m_142467_(Entity.RemovalReason.DISCARDED);
                        this.onStopChaining();
                        Direction direction = this.cachedDirection != null ? this.cachedDirection : Direction.UP;
                        BossUtil.malkuthPlayerFireballExplosionParticles((ServerLevel)this.m_9236_(), this.m_20182_(), MalkuthAttackType.FIRE);
                        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(50.0f).amplitude(1.5f).inTime(0).stayTime(0).outTime(10).build(), (Vec3)this.m_20182_(), (double)40.0);
                        MalkuthCrushAttack.summon(this.m_9236_(), this.m_20182_().m_82520_((double)((float)direction.m_122429_() * 0.1f), (double)((float)direction.m_122430_() * 0.1f), (double)((float)direction.m_122431_() * 0.1f)), 0.0f, direction, null);
                        this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_FIREBALL_EXPLOSION.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
                        this.m_9236_().m_6263_(null, this.m_20182_().f_82479_, this.m_20182_().f_82480_, this.m_20182_().f_82481_, BossSounds.ROCK_IMPACT.get(), SoundSource.PLAYERS, 3.0f, 0.8f);
                        this.m_9236_().m_6263_(null, this.m_20182_().f_82479_, this.m_20182_().f_82480_, this.m_20182_().f_82481_, BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.PLAYERS, 3.0f, 0.8f);
                        if (this.cachedDirection != null) {
                            Vec3 n = new Vec3((double)this.cachedDirection.m_122429_(), (double)this.cachedDirection.m_122430_(), (double)this.cachedDirection.m_122431_());
                            double offs = n.m_82526_(new Vec3(0.0, -1.0, 0.0));
                            Vec3 tppos = this.m_20182_().m_82549_(n.m_82490_(offs += 1.0));
                            owner.m_6021_(tppos.f_82479_, tppos.f_82480_, tppos.f_82481_);
                        }
                        float damage = BossConfigs.BOSS_CONFIG.get().itemConfig.iceFireGauntletCrushDamage;
                        for (LivingEntity entity : FDTargetFinder.getEntitiesInSphere(LivingEntity.class, (Level)this.m_9236_(), (Vec3)this.m_20182_(), (float)3.0f)) {
                            if (entity == owner) continue;
                            entity.m_7311_(100);
                            entity.m_6469_(this.m_9236_().m_269111_().m_269075_(owner), damage);
                            Vec3 between = entity.m_20182_().m_82546_(this.m_20182_());
                            Vec3 speed = between.m_82541_().m_82490_(0.5).m_82520_(0.0, 0.25, 0.0);
                            if (entity instanceof ServerPlayer) {
                                ServerPlayer serverPlayer = (ServerPlayer)entity;
                                FDLibCalls.setServerPlayerSpeed((ServerPlayer)serverPlayer, (Vec3)speed);
                                serverPlayer.f_19812_ = true;
                                continue;
                            }
                            entity.m_20256_(speed);
                        }
                    }
                }
            }
        }
    }

    public void tickHook() {
        if (!this.m_9236_().f_46443_) {
            if (this.hookFlyingTo == null) {
                Entity entity;
                if (this.f_19797_ > 2) {
                    this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_CHAIN_PULL.get(), SoundSource.HOSTILE, 4.0f, 0.85f);
                }
                Vec3 deltaMovement = this.m_20184_();
                Vec3 start = this.m_20182_().m_82549_(deltaMovement.m_82548_());
                Vec3 end = start.m_82549_(deltaMovement.m_82490_(2.0));
                List entities = FDHelpers.traceEntities((Level)this.m_9236_(), (Vec3)start, (Vec3)end, (double)0.15f, livingEntity -> livingEntity instanceof LivingEntity && livingEntity != this.getOwner());
                if (!entities.isEmpty() && (entity = (Entity)entities.get(0)) != null) {
                    this.hookFlyingTo = entity.m_20148_();
                    this.m_20088_().m_135381_(HOOKED_TO, (Object)entity.m_19879_());
                } else {
                    ClipContext clipContext = new ClipContext(this.m_20182_(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
                    BlockHitResult res = this.m_9236_().m_45547_(clipContext);
                    if (res.m_6662_() != HitResult.Type.MISS) {
                        this.m_142467_(Entity.RemovalReason.DISCARDED);
                        this.onStopChaining();
                    }
                }
            } else {
                this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_FIST_GRAB).build());
                LivingEntity entity = this.getEntityOnHook();
                Player owner = this.getOwner();
                if (owner != null && entity != null && !entity.m_21224_()) {
                    this.m_20256_(Vec3.f_82478_);
                    if (owner.m_20202_() == null) {
                        if (!owner.m_7998_((Entity)this, true)) {
                            this.m_142467_(Entity.RemovalReason.DISCARDED);
                            this.onStopChaining();
                        }
                    } else if (owner.m_20202_() == this) {
                        this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_CHAIN_PULL.get(), SoundSource.HOSTILE, 4.0f, 0.9f);
                        if (this.pullingPlayerTicker <= this.pullingPlayerTickerTime / 2) {
                            this.vectorBetween = entity.m_20182_().m_82546_(owner.m_20182_());
                        }
                        if (this.pullingPlayerTickerTime != -1 && this.pullingPlayerTicker > this.pullingPlayerTickerTime + 1) {
                            owner.m_8127_();
                            FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)owner), (Object)new PlayerForceAttackEntityPacket(entity));
                            BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(5.0f).scalingOptions(1, 0, 2).color(1.0f, 0.6f, 0.2f).brightness(2).build();
                            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ballParticleOptions, entity.m_20185_(), entity.m_20186_() + (double)(entity.m_20206_() / 2.0f), entity.m_20189_(), 1, 0.0, 0.0, 0.0, 0.0);
                            BallParticleOptions ballParticleOptions2 = BallParticleOptions.builder().size(0.5f).friction(0.9f).scalingOptions(0, 0, 10).color(1.0f, 0.6f, 0.2f).brightness(2).build();
                            BallParticleOptions ballParticleOptions3 = BallParticleOptions.builder().size(0.5f).friction(0.9f).scalingOptions(0, 0, 10).color(0.2f, 0.6f, 1.0f).brightness(2).build();
                            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ballParticleOptions2, entity.m_20185_(), entity.m_20186_() + (double)(entity.m_20206_() / 2.0f), entity.m_20189_(), 20, 0.1, 0.1, 0.1, 0.25);
                            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ballParticleOptions3, entity.m_20185_(), entity.m_20186_() + (double)(entity.m_20206_() / 2.0f), entity.m_20189_(), 20, 0.1, 0.1, 0.1, 0.25);
                            FDBossesServerScheduler.addDelayedAction(2, server -> {
                                if (this.vectorBetween != null) {
                                    Vec3 between = this.vectorBetween;
                                    Vec3 movement = between.m_82542_(-1.0, 0.0, -1.0).m_82541_().m_82520_(0.0, 1.0, 0.0).m_82490_(1.25);
                                    FDLibCalls.setServerPlayerSpeed((ServerPlayer)((ServerPlayer)owner), (Vec3)movement);
                                    owner.f_19812_ = true;
                                }
                            });
                            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
                            int cooldown = BossConfigs.BOSS_CONFIG.get().itemConfig.iceFireGauntletCooldown;
                            ItemStack item = owner.m_21211_();
                            if (owner.m_21211_().m_150930_(BossItems.MALKUTH_FIST.get())) {
                                MalkuthFist.setCanSkipCooldown(item, true);
                                MalkuthFist.setHookCooldown(item, cooldown);
                            }
                            owner.m_36335_().m_41524_(BossItems.MALKUTH_FIST.get(), cooldown);
                            this.m_142467_(Entity.RemovalReason.DISCARDED);
                            this.onStopChaining();
                        }
                    } else {
                        this.m_142467_(Entity.RemovalReason.DISCARDED);
                        this.onStopChaining();
                    }
                } else {
                    this.m_142467_(Entity.RemovalReason.DISCARDED);
                    this.onStopChaining();
                }
            }
        }
    }

    private LivingEntity getEntityOnHook() {
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            Entity entity;
            ServerLevel serverLevel = (ServerLevel)level;
            if (this.hookFlyingTo != null && (entity = serverLevel.m_8791_(this.hookFlyingTo)) instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                return livingEntity;
            }
            return null;
        }
        return null;
    }

    private void checkOwnerValidity() {
        double checkDistance;
        Player owner = this.getOwner();
        double d = checkDistance = this.getHookedTo() != null || this.m_20184_().equals((Object)Vec3.f_82478_) ? 40.0 : 30.0;
        if (owner == null || (double)this.m_20270_((Entity)owner) > checkDistance || !owner.m_21211_().m_150930_(BossItems.MALKUTH_FIST.get())) {
            this.m_142467_(Entity.RemovalReason.DISCARDED);
            this.onStopChaining();
        }
    }

    public boolean isHook() {
        return (Boolean)this.m_20088_().m_135370_(IS_HOOK);
    }

    public Player getOwner() {
        if (!this.m_9236_().f_46443_) {
            if (this.ownerPlayer != null) {
                return ((ServerLevel)this.m_9236_()).m_46003_(this.ownerPlayer);
            }
            return null;
        }
        Entity entity = this.m_9236_().m_6815_(((Integer)this.m_20088_().m_135370_(OWNER_ID)).intValue());
        if (entity instanceof Player) {
            Player player = (Player)entity;
            return player;
        }
        return null;
    }

    private void onStopChaining() {
        Player owner = this.getOwner();
        if (owner != null && owner.m_21211_().m_150930_(BossItems.MALKUTH_FIST.get())) {
            owner.m_5810_();
        }
    }

    protected void m_19956_(Entity entity, Entity.MoveFunction function) {
        Entity vehicle;
        if (this.currentPlayerHookPos == null) {
            this.currentPlayerHookPos = entity.m_20182_().m_82520_(0.0, (double)(entity.m_20206_() / 2.0f) * 0.5, 0.0);
            double distance = this.currentPlayerHookPos.m_82554_(this.m_20182_());
            this.pullingPlayerTickerTime = (int)Math.ceil(distance / 2.0);
            if (this.pullingPlayerTickerTime <= 0) {
                this.pullingPlayerTickerTime = 1;
            }
            this.pullingPlayerTicker = 0;
        }
        Vec3 thisPos = (vehicle = this.m_20202_()) != null ? this.m_20182_().m_82520_(0.0, (double)(-vehicle.m_20206_() / 2.0f), 0.0) : this.m_20182_();
        float p = Mth.m_14036_((float)((float)this.pullingPlayerTicker / (float)this.pullingPlayerTickerTime), (float)0.0f, (float)1.0f);
        Vec3 interpolated = FDMathUtil.interpolateVectors((Vec3)this.currentPlayerHookPos, (Vec3)thisPos, (float)FDEasings.easeIn((float)p));
        function.m_20372_(entity, interpolated.f_82479_, interpolated.f_82480_, interpolated.f_82481_);
    }

    private void tickPullingTime() {
        if (this.pullingPlayerTickerTime != -1) {
            ++this.pullingPlayerTicker;
        }
    }

    public boolean shouldRiderSit() {
        return false;
    }

    protected void m_8097_() {
        this.m_20088_().m_135372_(IS_HOOK, (Object)true);
        this.m_20088_().m_135372_(OWNER_ID, (Object)-1);
        this.m_20088_().m_135372_(HOOKED_TO, (Object)-1);
    }

    protected void m_7378_(CompoundTag tag) {
        if (tag.m_128441_("ownerPlayer")) {
            this.ownerPlayer = tag.m_128342_("ownerPlayer");
        }
        if (tag.m_128441_("hookFlyingTo")) {
            this.hookFlyingTo = tag.m_128342_("hookFlyingTo");
        }
        this.m_20088_().m_135381_(IS_HOOK, (Object)tag.m_128471_("isHook"));
    }

    protected void m_7380_(CompoundTag tag) {
        if (this.ownerPlayer != null) {
            tag.m_128362_("ownerPlayer", this.ownerPlayer);
        }
        if (this.hookFlyingTo != null) {
            tag.m_128362_("hookFlyingTo", this.hookFlyingTo);
        }
        tag.m_128379_("isHook", ((Boolean)this.m_20088_().m_135370_(IS_HOOK)).booleanValue());
    }

    @Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {
        @SubscribeEvent
        public static void hurtEvent(LivingHurtEvent event) {
            ServerPlayer serverPlayer;
            Entity entity;
            LivingEntity entity2 = event.getEntity();
            if (entity2 instanceof ServerPlayer && (entity = (serverPlayer = (ServerPlayer)entity2).m_20202_()) instanceof MalkuthFistChain) {
                MalkuthFistChain malkuthFistChain = (MalkuthFistChain)entity;
                Entity source = event.getSource().m_7639_();
                if (source != null) {
                    AABB sourceBB = source.m_20191_().m_82400_(2.0);
                    if (sourceBB.m_82381_(serverPlayer.m_20191_())) {
                        event.setCanceled(true);
                        serverPlayer.f_19802_ = 20;
                    }
                } else if (event.getSource().m_276093_(DamageTypes.f_268612_)) {
                    event.setCanceled(true);
                    serverPlayer.f_19802_ = 20;
                }
            }
        }
    }
}

