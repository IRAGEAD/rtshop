/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.FDProjectile
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Math
 */
package com.finderfeed.fdbosses.content.entities.chesed_sword_buff;

import com.finderfeed.fdbosses.client.particles.rush_particle.RushParticleOptions;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.FDProjectile;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;

public class FlyingSwordEntity
extends FDProjectile {
    public static int DELAY_UNTIL_LAUNCH = 15;
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.m_135353_(FlyingSwordEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135033_);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.m_135353_(FlyingSwordEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    private UUID target = null;
    public double xao;
    public double yao;
    public double zao;

    public FlyingSwordEntity(EntityType<? extends FDProjectile> type, Level level) {
        super(type, level);
    }

    public static FlyingSwordEntity summonAtTarget(LivingEntity owner, LivingEntity target, ItemStack weapon) {
        Level level = owner.m_9236_();
        FlyingSwordEntity flyingSwordEntity = new FlyingSwordEntity(BossEntities.FLYING_SWORD.get(), level);
        flyingSwordEntity.setItem(weapon);
        flyingSwordEntity.target = target.m_20148_();
        Vec3 targetPos = target.m_20182_();
        Vec3 between = targetPos.m_82546_(owner.m_20182_()).m_82542_(1.0, 0.0, 1.0).m_82541_();
        float rndDistMod = Mth.m_14036_((float)(target.m_20205_() * 3.0f), (float)2.0f, (float)10.0f) + level.f_46441_.m_188501_() * 2.0f;
        float rndOffsNum = level.f_46441_.m_188499_() ? 1.0f : -1.0f;
        Vec3 randomOffset = between.m_82542_((double)rndDistMod, (double)rndDistMod, (double)rndDistMod).m_82524_(rndOffsNum * (float)java.lang.Math.PI / 2.0f + -rndOffsNum * (float)java.lang.Math.PI / 8.0f).m_82520_(0.0, 1.5 + (double)(level.f_46441_.m_188501_() * 2.0f), 0.0);
        Vec3 resultPos = targetPos.m_82549_(randomOffset);
        flyingSwordEntity.m_146884_(resultPos);
        flyingSwordEntity.m_5602_((Entity)owner);
        flyingSwordEntity.setTargetIdForClient(target);
        if (level.m_7967_((Entity)flyingSwordEntity)) {
            ((ServerLevel)level).m_8767_((ParticleOptions)BallParticleOptions.builder().size(0.15f).color(0.1f, 0.9f, 1.0f).scalingOptions(2, 0, 5).friction(0.8f).build(), resultPos.f_82479_, resultPos.f_82480_, resultPos.f_82481_, 60, 0.0, 0.0, 0.0, (double)0.1f);
        }
        return flyingSwordEntity;
    }

    public void m_8119_() {
        if (this.f_19797_ == 1) {
            this.xao = this.m_20185_();
            this.yao = this.m_20186_();
            this.zao = this.m_20189_();
        }
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.f_19797_ >= 400) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
            this.handleTarget();
            if (this.f_19797_ == DELAY_UNTIL_LAUNCH - 2) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.GHOST_WEAPON_FLY_OUT.get(), SoundSource.NEUTRAL, 1.0f, 1.0f + this.f_19796_.m_188501_() * 0.25f);
            }
        } else {
            Entity entity;
            Vec3 center = this.getActualPos();
            if (this.f_19797_ == DELAY_UNTIL_LAUNCH && (entity = this.m_9236_().m_6815_(this.getTargetIdForClient())) instanceof LivingEntity) {
                LivingEntity target = (LivingEntity)entity;
                Vec3 targetPos = this.getTargetPos(target, 0.0f);
                Vec3 b = center.m_82546_(targetPos).m_82541_();
                int count = 7;
                for (int i = 0; i < count; ++i) {
                    Vec3 rndOffset = new Vec3((double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f)).m_82541_();
                    float bscale = (float)i / (float)(count - 1) * 2.0f;
                    float scale = this.f_19796_.m_188501_() * 0.25f;
                    float defaultScale = 0.1f;
                    Vec3 pos = center.m_82549_(rndOffset.m_82542_((double)scale, (double)scale, (double)scale)).m_82549_(rndOffset.m_82542_((double)defaultScale, (double)defaultScale, (double)defaultScale)).m_82549_(b.m_82542_((double)bscale, (double)bscale, (double)bscale));
                    this.m_9236_().m_7106_((ParticleOptions)new RushParticleOptions(b, new FDColor(0.8f, 1.0f, 1.0f, 1.0f), this.f_19796_.m_188501_() * 0.5f + 0.25f, 0.05f + this.f_19796_.m_188501_() * 0.025f, 2 + this.f_19796_.m_188503_(2)), pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
                }
            }
            Vec3 centerO = new Vec3(this.xao, this.yao, this.zao);
            Vec3 b = center.m_82546_(centerO);
            double dist = Math.min((double)centerO.m_82554_(center), (double)10.0);
            float size = 0.125f;
            Vec3 sp = b.m_82541_().m_82542_(0.25, 0.25, 0.25);
            float i = 0.0f;
            while ((double)i < dist) {
                double p = (double)i / dist;
                Vec3 ball = new Vec3(FDMathUtil.lerp((double)centerO.f_82479_, (double)center.f_82479_, (double)p), FDMathUtil.lerp((double)centerO.f_82480_, (double)center.f_82480_, (double)p), FDMathUtil.lerp((double)centerO.f_82481_, (double)center.f_82481_, (double)p)).m_82520_((double)this.f_19796_.m_188501_() * 0.25 - 0.125, (double)this.f_19796_.m_188501_() * 0.25 - 0.125, (double)this.f_19796_.m_188501_() * 0.25 - 0.125);
                this.m_9236_().m_7106_((ParticleOptions)BallParticleOptions.builder().size(size).color(0.1f, 0.9f, 1.0f).scalingOptions(2, 0, 10).friction(0.9f).build(), ball.f_82479_ + (double)this.f_19796_.m_188501_() * 0.25 - 0.125, ball.f_82480_ + (double)this.f_19796_.m_188501_() * 0.25 - 0.125, ball.f_82481_ + (double)this.f_19796_.m_188501_() * 0.25 - 0.125, sp.f_82479_, sp.f_82480_, sp.f_82481_);
                if (this.f_19797_ % 2 == 0) {
                    this.m_9236_().m_7106_((ParticleOptions)LightningParticleOptions.builder().randomRoll(true).lifetime(2).maxLightningSegments(4).quadSize(0.3f).color(20, 100, 255).build(), ball.f_82479_ + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, ball.f_82480_ + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, ball.f_82481_ + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, sp.f_82479_, sp.f_82480_, sp.f_82481_);
                }
                i += size * 2.0f;
            }
            this.xao = center.f_82479_;
            this.yao = center.f_82480_;
            this.zao = center.f_82481_;
        }
    }

    private Vec3 getActualPos() {
        Vec3 center = this.m_20182_().m_82520_(0.0, (double)(this.m_20206_() / 2.0f), 0.0);
        float time = this.f_19797_;
        float p = FDMathUtil.clamp((float)(time / (float)DELAY_UNTIL_LAUNCH), (float)0.0f, (float)1.0f);
        float offset = -Mth.m_14031_((float)(p * (float)java.lang.Math.PI)) * 2.0f;
        int targetId = this.getTargetIdForClient();
        Entity entity = this.m_9236_().m_6815_(targetId);
        if (entity instanceof LivingEntity) {
            LivingEntity target = (LivingEntity)entity;
            Vec3 targetPos = this.getTargetPos(target, 0.0f);
            Vec3 between = targetPos.m_82546_(center).m_82541_();
            return center.m_82549_(between.m_82542_((double)offset, (double)offset, (double)offset));
        }
        return center;
    }

    public boolean m_142038_() {
        return true;
    }

    public int m_19876_() {
        return 0x11AAFF;
    }

    private void handleTarget() {
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            LivingEntity livingTarget;
            ServerLevel serverLevel = (ServerLevel)level;
            Entity entity = serverLevel.m_8791_(this.target);
            if (entity instanceof LivingEntity && (livingTarget = (LivingEntity)entity).m_6084_() && (entity = this.m_19749_()) instanceof LivingEntity) {
                LivingEntity owner = (LivingEntity)entity;
                this.setTargetIdForClient(livingTarget);
                if ((float)this.f_19797_ > (float)DELAY_UNTIL_LAUNCH * 0.75f) {
                    Vec3 thisPos = this.m_20182_();
                    Vec3 targetPos = this.getTargetPos(livingTarget, 0.0f);
                    Vec3 between = targetPos.m_82546_(thisPos);
                    float speed = 1.0f;
                    Vec3 deltaMovement = between.m_82541_().m_82542_((double)speed, (double)speed, (double)speed);
                    this.m_20256_(deltaMovement);
                    if (this.m_20182_().m_82554_(targetPos) <= 0.5) {
                        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(2.0f).scalingOptions(0, 0, 2).color(150, 230, 255).build(), (Vec3)targetPos, (double)30.0);
                        this.m_9236_().m_6263_(null, targetPos.f_82479_, targetPos.f_82480_, targetPos.f_82481_, BossSounds.FAST_LIGHTNING_STRIKE.get(), SoundSource.HOSTILE, 2.0f, 1.0f + this.f_19796_.m_188501_() * 0.5f);
                        this.m_142687_(Entity.RemovalReason.DISCARDED);
                    }
                }
            } else {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    public Vec3 getTargetPos(LivingEntity target, float pticks) {
        return target.m_20318_(pticks).m_82520_(0.0, (double)(target.m_20206_() / 2.0f), 0.0);
    }

    public void setTargetIdForClient(LivingEntity target) {
        this.f_19804_.m_135381_(TARGET_ID, (Object)target.m_19879_());
    }

    public int getTargetIdForClient() {
        return (Integer)this.f_19804_.m_135370_(TARGET_ID);
    }

    protected void m_5790_(EntityHitResult hitResult) {
        super.m_5790_(hitResult);
    }

    protected void m_8060_(BlockHitResult hitResult) {
    }

    public ItemStack getItem() {
        return (ItemStack)this.f_19804_.m_135370_(ITEM);
    }

    public void setItem(ItemStack itemStack) {
        this.f_19804_.m_135381_(ITEM, (Object)itemStack);
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(ITEM, (Object)Items.f_42420_.m_7968_());
        this.f_19804_.m_135372_(TARGET_ID, (Object)0);
    }

    protected void m_20101_() {
    }

    public void m_7378_(CompoundTag tag) {
    }

    public void m_7380_(CompoundTag tag) {
    }
}

