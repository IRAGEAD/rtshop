/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector2d
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_crush;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBuddy;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3f;

public class MalkuthCrushAttack
extends FDEntity
implements AutoSerializable {
    public static final EntityDataAccessor<Direction> DIRECTION = SynchedEntityData.m_135353_(MalkuthCrushAttack.class, (EntityDataSerializer)EntityDataSerializers.f_135040_);
    public static final EntityDataAccessor<Byte> VISUAL_APPEARANCE = SynchedEntityData.m_135353_(MalkuthCrushAttack.class, (EntityDataSerializer)EntityDataSerializers.f_135027_);
    @SerializableField
    private float damage;

    public static MalkuthCrushAttack summon(Level level, Vec3 pos, float damage) {
        return MalkuthCrushAttack.summon(level, pos, damage, Direction.UP, null);
    }

    public static MalkuthCrushAttack summon(Level level, Vec3 pos, float damage, Direction direction, MalkuthAttackType visualAppearance) {
        MalkuthCrushAttack malkuthCrushAttack = new MalkuthCrushAttack(BossEntities.MALKUTH_CRUSH.get(), level);
        malkuthCrushAttack.m_146884_(pos);
        malkuthCrushAttack.damage = damage;
        malkuthCrushAttack.f_19804_.m_135381_(DIRECTION, (Object)direction);
        if (visualAppearance != null) {
            if (visualAppearance.isFire()) {
                malkuthCrushAttack.f_19804_.m_135381_(VISUAL_APPEARANCE, (Object)1);
            } else {
                malkuthCrushAttack.f_19804_.m_135381_(VISUAL_APPEARANCE, (Object)2);
            }
        }
        level.m_7967_((Entity)malkuthCrushAttack);
        return malkuthCrushAttack;
    }

    public MalkuthCrushAttack(EntityType<?> type, Level level) {
        super(type, level);
        this.getAnimationSystem().startAnimation("ATTACK", AnimationTicker.builder(BossAnims.MALKUTH_CRUSH_ATTACK_CRUSH).setSpeed(1.25f).build());
    }

    public void m_8119_() {
        if (this.f_19803_) {
            if (this.m_9236_().f_46443_) {
                this.doParticles();
            } else {
                this.doDamage();
            }
        }
        super.m_8119_();
        if (!this.m_9236_().f_46443_ && this.f_19797_ > ((Animation)BossAnims.MALKUTH_CRUSH_ATTACK_CRUSH.get()).getAnimTime() - 10) {
            this.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    public boolean isBothFireAndIce() {
        return (Byte)this.f_19804_.m_135370_(VISUAL_APPEARANCE) == 0;
    }

    public boolean isFire() {
        return (Byte)this.f_19804_.m_135370_(VISUAL_APPEARANCE) == 1;
    }

    public boolean isIce() {
        return (Byte)this.f_19804_.m_135370_(VISUAL_APPEARANCE) == 2;
    }

    private void doDamage() {
        if (this.damage == 0.0f) {
            return;
        }
        float radius = 4.0f;
        Predicate<LivingEntity> entityPredicate = v -> !(v instanceof MalkuthBossBuddy);
        List<LivingEntity> targets = BossTargetFinder.getEntitiesInCylinder(LivingEntity.class, this.m_9236_(), this.m_20182_(), 2.0f, radius, entityPredicate);
        for (LivingEntity target : targets) {
            target.m_6469_(BossDamageSources.MALKUTH_EARTHSHATTER_SOURCE, this.damage);
            Vec3 speed = target.m_20182_().m_82546_(this.m_20182_()).m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(2.0, 0.0, 2.0).m_82520_(0.0, 1.5, 0.0);
            if (target instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)target;
                FDLibCalls.setServerPlayerSpeed((ServerPlayer)player, (Vec3)speed);
                continue;
            }
            target.m_20256_(speed);
        }
    }

    private void doParticles() {
        int i;
        int count = 30;
        Vec3i nrm = ((Direction)this.m_20088_().m_135370_(DIRECTION)).m_122436_();
        Vec3 v = new Vec3((double)nrm.m_123341_(), (double)nrm.m_123342_(), (double)nrm.m_123343_());
        Matrix4f mat = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)v);
        float angle = (float)Math.PI * 2 / (float)count;
        for (i = 0; i < count; ++i) {
            Vector2d randomAngle;
            float m;
            float dist;
            float currentAngle = angle * (float)i;
            for (dist = 0.0f; dist < 3.0f; dist += 0.5f) {
                float b;
                float g;
                float r;
                m = dist + this.f_19796_.m_188501_() * 0.25f;
                randomAngle = this.randomAngle(angle, currentAngle);
                if (this.isBothFireAndIce()) {
                    if (this.f_19796_.m_188501_() > 0.5f) {
                        r = this.f_19796_.m_188501_() * 0.2f;
                        g = 0.7f + this.f_19796_.m_188501_() * 0.1f;
                        b = 0.9f + this.f_19796_.m_188501_() * 0.1f;
                    } else {
                        r = 0.9f + this.f_19796_.m_188501_() * 0.1f;
                        g = 0.2f + this.f_19796_.m_188501_() * 0.2f;
                        b = this.f_19796_.m_188501_() * 0.2f;
                    }
                } else if (this.isFire()) {
                    r = 0.9f + this.f_19796_.m_188501_() * 0.1f;
                    g = 0.2f + this.f_19796_.m_188501_() * 0.2f;
                    b = this.f_19796_.m_188501_() * 0.2f;
                } else {
                    r = this.f_19796_.m_188501_() * 0.2f;
                    g = 0.7f + this.f_19796_.m_188501_() * 0.1f;
                    b = 0.9f + this.f_19796_.m_188501_() * 0.1f;
                }
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(0.2f + this.f_19796_.m_188501_() * 0.2f).color(r, g, b).scalingOptions(0, 0, 10 + this.f_19796_.m_188503_(4)).friction(0.6f).build();
                float hspeed = (0.1f + this.f_19796_.m_188501_() * 0.2f) * 2.0f;
                float vspeed = (0.05f + this.f_19796_.m_188501_() * 0.5f) * 2.0f;
                Vec3 speed = BossUtil.matTransformDirectionVec3(mat, new Vec3(randomAngle.x * (double)hspeed, (double)vspeed, randomAngle.y * (double)hspeed));
                Vec3 spOffset = BossUtil.matTransformDirectionVec3(mat, new Vec3(randomAngle.x * (double)m, 0.0, randomAngle.y * (double)m));
                this.m_9236_().m_6493_((ParticleOptions)ballParticleOptions, true, this.m_20185_() + spOffset.f_82479_, this.m_20186_() + spOffset.f_82480_, this.m_20189_() + spOffset.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
            for (dist = 1.0f; dist < 3.5f; dist += 1.0f) {
                m = dist + this.f_19796_.m_188501_() * 0.5f;
                randomAngle = this.randomAngle(angle, currentAngle);
                if (this.f_19796_.m_188501_() > 0.66f) {
                    if (this.isIce()) continue;
                    this.m_9236_().m_6493_((ParticleOptions)ParticleTypes.f_123756_, true, this.m_20185_() + randomAngle.x * (double)m, this.m_20186_(), this.m_20189_() + randomAngle.y * (double)m, 0.0, 0.0, 0.0);
                    continue;
                }
                GravityParticleOptions particleOptions = this.isBothFireAndIce() ? (this.f_19796_.m_188501_() > 0.5f ? new GravityParticleOptions(BossParticles.FLAME_WITH_STONE.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true) : new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true)) : (this.isFire() ? new GravityParticleOptions(BossParticles.FLAME_WITH_STONE.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true) : new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true));
                float hspeed = 0.1f + this.f_19796_.m_188501_() * 0.05f;
                float vspeed = 0.4f + this.f_19796_.m_188501_() * 0.25f;
                Vec3 speed = BossUtil.matTransformDirectionVec3(mat, new Vec3(randomAngle.x * (double)hspeed, (double)vspeed, randomAngle.y * (double)hspeed));
                Vec3 spOffset = BossUtil.matTransformDirectionVec3(mat, new Vec3(randomAngle.x * (double)m, 0.0, randomAngle.y * (double)m));
                this.m_9236_().m_6493_((ParticleOptions)particleOptions, true, this.m_20185_() + spOffset.f_82479_, this.m_20186_() + spOffset.f_82480_, this.m_20189_() + spOffset.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
            Vector2d randomAngle2 = this.randomAngle(angle, currentAngle);
            int cr = this.f_19796_.m_188503_(50);
            BigSmokeParticleOptions smokeParticleOptions = BigSmokeParticleOptions.builder().size(1.0f + this.f_19796_.m_188501_() * 0.5f).friction(0.75f).minSpeed(0.025f).color(50 + cr, 50 + cr, 50 + cr).lifetime(0, 5, 30 + this.f_19796_.m_188503_(5)).build();
            float rndOffs = this.f_19796_.m_188501_() * 0.5f;
            float rndSpeed = this.f_19796_.m_188501_() * 1.0f + 0.25f;
            Vec3 speed = BossUtil.matTransformDirectionVec3(mat, new Vec3(randomAngle2.x * (double)rndSpeed, (double)(this.f_19796_.m_188501_() * 0.025f + 0.025f), randomAngle2.y * (double)rndSpeed));
            Vec3 spOffset = BossUtil.matTransformDirectionVec3(mat, new Vec3(randomAngle2.x * (double)rndOffs, (double)(this.f_19796_.m_188501_() * 0.5f), randomAngle2.y * (double)rndOffs));
            this.m_9236_().m_6493_((ParticleOptions)smokeParticleOptions, true, this.m_20185_() + spOffset.f_82479_, this.m_20186_() + spOffset.f_82480_, this.m_20189_() + spOffset.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
        }
        for (i = 0; i < 15; ++i) {
            if (this.isBothFireAndIce()) {
                this.stripeParticles(mat, MalkuthAttackType.FIRE);
                this.stripeParticles(mat, MalkuthAttackType.ICE);
                continue;
            }
            if (this.isFire()) {
                this.stripeParticles(mat, MalkuthAttackType.FIRE);
                this.stripeParticles(mat, MalkuthAttackType.FIRE);
                continue;
            }
            this.stripeParticles(mat, MalkuthAttackType.ICE);
            this.stripeParticles(mat, MalkuthAttackType.ICE);
        }
    }

    private void stripeParticles(Matrix4f mat, MalkuthAttackType type) {
        float rndRadius = 1.0f + FDEasings.easeOut((float)this.f_19796_.m_188501_()) * 4.0f;
        Vec3 rnd = new Vec3((double)rndRadius, 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
        Vec3 dir = rnd.m_82541_();
        float startOffsetRand = 0.1f + this.f_19796_.m_188501_() * 0.5f;
        Vec3 startOffset = dir.m_82542_((double)startOffsetRand, (double)startOffsetRand, (double)startOffsetRand);
        Vec3 stripePos = this.m_20182_().m_82549_(BossUtil.matTransformDirectionVec3(mat, startOffset));
        Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(type);
        FDColor fireColorStart = new FDColor(colFire.x, colFire.y - this.f_19796_.m_188501_() * 0.1f - 0.3f, colFire.z, 0.5f);
        FDColor fireColor = new FDColor(colFire.x, colFire.y + this.f_19796_.m_188501_() * 0.1f, colFire.z, 1.0f);
        float firstPointOffset = 2.0f + this.f_19796_.m_188501_() * 1.0f;
        StripeParticleOptions stripeParticleOptions = new StripeParticleOptions(fireColorStart, fireColor, 5 + this.f_19796_.m_188503_(10), 50, 0.05f, 0.75f, BossUtil.matTransformDirectionVec3(mat, new Vec3((double)0.01f, 0.0, 0.0)), BossUtil.matTransformDirectionVec3(mat, dir.m_82542_((double)firstPointOffset, 0.0, (double)firstPointOffset).m_82520_(0.0, 0.5, 0.0)), BossUtil.matTransformDirectionVec3(mat, rnd.m_82520_(0.0, (double)(1.5f + this.f_19796_.m_188501_() * 2.0f), 0.0)));
        this.m_9236_().m_6493_((ParticleOptions)stripeParticleOptions, true, stripePos.f_82479_, stripePos.f_82480_, stripePos.f_82481_, 0.0, 0.0, 0.0);
    }

    private Vector2d randomAngle(float angle, float currentAngle) {
        float a = currentAngle + (this.f_19796_.m_188501_() * 2.0f - 1.0f) * angle / 2.0f;
        return new Vector2d(Math.sin(a), Math.cos(a));
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(DIRECTION, (Object)Direction.UP);
        this.f_19804_.m_135372_(VISUAL_APPEARANCE, (Object)0);
    }

    protected void m_7378_(CompoundTag tag) {
        this.autoSave(tag);
    }

    protected void m_7380_(CompoundTag tag) {
        this.autoLoad(tag);
    }

    public boolean m_6783_(double dist) {
        return dist <= 3600.0;
    }
}

