/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.EmitterProcessor
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterData
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.processors.BoundToEntityProcessor
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Math
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_giant_sword;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.chesed_boss.falling_block.ChesedFallingBlock;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBuddy;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthDamageSource;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.particle.particle_emitter.EmitterProcessor;
import com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterData;
import com.finderfeed.fdlib.systems.particle.particle_emitter.processors.BoundToEntityProcessor;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Vector3f;

public class MalkuthGiantSwordSlash
extends Entity
implements AutoSerializable {
    public static int TIME_TO_RISE = 60;
    public static int TIME_TO_HIT = 30;
    public static int DISSOLVE_TIME = 30;
    public static final EntityDataAccessor<MalkuthAttackType> MALKUTH_ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthGiantSwordSlash.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    @SerializableField
    private boolean doDamage = true;
    @SerializableField
    private float damage;
    @SerializableField
    private int serverTicks;

    public static MalkuthGiantSwordSlash summon(Level level, Vec3 pos, Vec3 direction, MalkuthAttackType attackType, float damage) {
        MalkuthGiantSwordSlash slash = new MalkuthGiantSwordSlash(BossEntities.MALKUTH_GIANT_SWORD.get(), level);
        direction = direction.m_82542_(1.0, 0.0, 1.0).m_82541_();
        slash.setAttackType(attackType);
        slash.m_146884_(pos);
        slash.m_7618_(EntityAnchorArgument.Anchor.FEET, pos.m_82549_(direction));
        slash.damage = damage;
        level.m_7967_((Entity)slash);
        return slash;
    }

    public MalkuthGiantSwordSlash(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        ++this.serverTicks;
        if (this.m_9236_().f_46443_) {
            if (this.f_19797_ == TIME_TO_HIT + TIME_TO_RISE) {
                this.impactParticles();
            }
            if (this.f_19797_ > 3 && this.f_19797_ < TIME_TO_RISE - 20) {
                this.riseParticles();
            }
        } else if (this.serverTicks == TIME_TO_HIT + TIME_TO_RISE) {
            this.impactBlocks();
            this.doDamage();
        } else if (this.serverTicks >= TIME_TO_HIT + TIME_TO_RISE + DISSOLVE_TIME) {
            this.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    private void doDamage() {
        if (this.doDamage) {
            Vec3 direction = new Vec3(0.0, 0.0, 1.0).m_82524_(-Math.toRadians((float)this.m_146908_()));
            float offset = 7.5f;
            Vec3 start = this.m_20182_().m_82549_(direction.m_82542_((double)offset, (double)offset, (double)offset)).m_82520_(0.0, -1.0, 0.0);
            List<LivingEntity> targets = BossTargetFinder.getEntitiesInHorizontalBox(LivingEntity.class, this.m_9236_(), start, new Vec2((float)direction.f_82479_, (float)direction.f_82481_), 32.0f, 20.0f, 10.0f, entity -> !(entity instanceof MalkuthBossBuddy));
            for (LivingEntity target : targets) {
                target.m_6469_((DamageSource)new MalkuthDamageSource(this.m_9236_().m_269111_().m_269264_(), this.getAttackType(), 100), this.damage);
            }
        }
    }

    public void setDoDamage(boolean doDamage) {
        this.doDamage = doDamage;
    }

    public boolean isDoDamage() {
        return this.doDamage;
    }

    public float getDamage() {
        return this.damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    private void riseParticles() {
        for (int i = 0; i < 10; ++i) {
            Vector3f color = MalkuthEntity.getMalkuthAttackPreparationParticleColor(this.getAttackType());
            color.x = FDMathUtil.clamp((float)(color.x + 0.2f), (float)0.0f, (float)1.0f);
            color.y = FDMathUtil.clamp((float)(color.y + 0.2f), (float)0.0f, (float)1.0f);
            color.z = FDMathUtil.clamp((float)(color.z + 0.2f), (float)0.0f, (float)1.0f);
            float rndRadius = FDEasings.easeOut((float)this.f_19796_.m_188501_()) * 4.0f;
            Vec3 rnd = new Vec3((double)rndRadius, 0.0, 0.0).m_82524_((float)java.lang.Math.PI * 2 * this.f_19796_.m_188501_());
            float speedf = 0.05f + this.f_19796_.m_188501_() * 0.8f;
            Vec3 speed = rnd.m_82541_().m_82542_((double)speedf, 0.0, (double)speedf).m_82520_(0.0, (double)(speedf * 1.5f), 0.0);
            Vec3 ppos = this.m_20182_().m_82549_(rnd);
            BallParticleOptions options = BallParticleOptions.builder().scalingOptions(5, 0, 10).color(color.x, color.y, color.z).size(0.15f + (1.0f - speedf / 0.85f) * 0.5f).friction(0.8f).brightness(2).build();
            this.m_9236_().m_6493_((ParticleOptions)options, true, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
        }
        float rndRadius = 7.0f + FDEasings.easeOut((float)this.f_19796_.m_188501_()) * 3.0f;
        Vec3 rnd = new Vec3((double)rndRadius, 0.0, 0.0).m_82524_((float)java.lang.Math.PI * 2 * this.f_19796_.m_188501_());
        Vec3 dir = rnd.m_82541_();
        float startOffsetRand = 3.0f + this.f_19796_.m_188501_() * 2.0f;
        Vec3 startOffset = dir.m_82542_((double)startOffsetRand, (double)startOffsetRand, (double)startOffsetRand);
        Vec3 stripePos = this.m_20182_().m_82549_(startOffset);
        Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(this.getAttackType());
        FDColor fireColorStart = new FDColor(colFire.x, colFire.y - this.f_19796_.m_188501_() * 0.1f - 0.3f, colFire.z, 0.5f);
        FDColor fireColor = new FDColor(colFire.x, colFire.y + this.f_19796_.m_188501_() * 0.1f, colFire.z, 1.0f);
        float firstPointOffset = 2.0f + this.f_19796_.m_188501_() * 2.0f;
        float secondPointOffset = 6.0f + this.f_19796_.m_188501_() * 2.0f;
        StripeParticleOptions stripeParticleOptions = StripeParticleOptions.builder().startColor(fireColorStart).endColor(fireColor).lifetime(20).lod(50).scale(0.24f).stripePercentLength(0.5f).endOutPercent(0.2f).startInPercent(0.2f).offsets(new Vec3((double)0.01f, 0.0, 0.0), dir.m_82542_((double)firstPointOffset, 0.0, (double)firstPointOffset).m_82520_(0.0, 3.0, 0.0), dir.m_82542_((double)secondPointOffset, 0.0, (double)secondPointOffset).m_82520_(0.0, 7.0, 0.0), rnd.m_82520_(0.0, 12.0, 0.0)).build();
        this.m_9236_().m_6493_((ParticleOptions)stripeParticleOptions, true, stripePos.f_82479_, stripePos.f_82480_, stripePos.f_82481_, 0.0, 0.0, 0.0);
    }

    private void impactBlocks() {
        float start = 10.0f;
        float end = 40.0f;
        Vec3 direction = new Vec3(0.0, 0.0, 1.0).m_82524_(-Math.toRadians((float)this.m_146908_()));
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(50.0f).amplitude(1.0f).inTime(0).stayTime(0).outTime(50).build(), (Vec3)this.m_20182_().m_82520_(direction.f_82479_ * (double)((end - start) / 2.0f), direction.f_82480_ * (double)((end - start) / 2.0f), direction.f_82481_ * (double)((end - start) / 2.0f)), (double)60.0);
        float step = 0.4f;
        for (float i = start; i <= end; i += step) {
            Vec3 offset = direction.m_82542_((double)i, (double)i, (double)i);
            Vec3 pos = this.m_20182_().m_82549_(offset);
            BlockState state = this.randomParticleBlock();
            float verticalSpeed = this.f_19796_.m_188501_() * 0.5f + 0.25f;
            float horizontalSpeed = 0.05f + this.f_19796_.m_188501_() * 0.8f;
            int m = this.f_19796_.m_188503_(2) == 1 ? -1 : 1;
            Vec3 rndVec = direction.m_82524_((1.5707964f - 0.7853982f * this.f_19796_.m_188501_()) * (float)m).m_82542_((double)horizontalSpeed, (double)horizontalSpeed, (double)horizontalSpeed);
            Vec3 speed = rndVec.m_82520_(0.0, (double)verticalSpeed, 0.0);
            ChesedFallingBlock block = ChesedFallingBlock.summon(this.m_9236_(), state, pos, speed, 0.0f);
            block.softerSound = true;
            float rnd = this.f_19796_.m_188501_() * 0.05f;
            FDLibCalls.addParticleEmitter((Level)this.m_9236_(), (double)120.0, (ParticleEmitterData)ParticleEmitterData.builder((ParticleOptions)BigSmokeParticleOptions.builder().color(0.35f - rnd, 0.35f - rnd, 0.35f - rnd).lifetime(0, 0, 25).size(1.5f).build()).lifetime(200).processor((EmitterProcessor)new BoundToEntityProcessor(block.m_19879_(), Vec3.f_82478_)).position(this.m_20182_()).build());
        }
    }

    private void impactParticles() {
        float start = 10.0f;
        float end = 40.0f;
        Vec3 direction = new Vec3(0.0, 0.0, 1.0).m_82524_(-Math.toRadians((float)this.m_146908_()));
        int bigsmokerows = 4;
        float step = 0.5f;
        for (float i = start; i <= end; i += step) {
            Vec3 p;
            float v;
            Vec3 speed;
            Vec3 rndVec;
            int m;
            float horizontalSpeed;
            float verticalSpeed;
            int g;
            Vec3 offset = direction.m_82542_((double)i, (double)i, (double)i);
            Vec3 pos = this.m_20182_().m_82549_(offset);
            for (g = 0; g <= 4; ++g) {
                verticalSpeed = this.f_19796_.m_188501_() * 0.5f + 0.05f;
                horizontalSpeed = 0.05f + this.f_19796_.m_188501_() * 0.8f;
                m = this.f_19796_.m_188503_(2) == 1 ? -1 : 1;
                rndVec = direction.m_82524_(1.5707964f * (float)m).m_82542_((double)horizontalSpeed, (double)horizontalSpeed, (double)horizontalSpeed);
                speed = rndVec.m_82520_(0.0, (double)verticalSpeed, 0.0);
                v = this.f_19796_.m_188501_() * step - step / 2.0f;
                p = pos.m_82549_(direction.m_82542_((double)v, (double)v, (double)v));
                Vector3f col = MalkuthEntity.getMalkuthAttackPreparationParticleColor(this.getAttackType());
                float size = 0.5f + this.f_19796_.m_188501_() * 2.0f;
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(size).color(col.x, col.y + 0.2f, col.z).friction(0.8f).brightness(2).scalingOptions(0, 0, 40 + this.f_19796_.m_188503_(20)).build();
                float speedm = 2.0f;
                this.m_9236_().m_6493_((ParticleOptions)ballParticleOptions, true, p.f_82479_, p.f_82480_, p.f_82481_, speed.f_82479_ * (double)speedm, speed.f_82480_ * (double)speedm, speed.f_82481_ * (double)speedm);
            }
            for (g = 0; g <= 3; ++g) {
                verticalSpeed = this.f_19796_.m_188501_() * 0.6f + 0.05f;
                horizontalSpeed = 0.05f + this.f_19796_.m_188501_() * 0.4f;
                m = this.f_19796_.m_188503_(2) == 1 ? -1 : 1;
                rndVec = direction.m_82524_(1.5707964f * (float)m).m_82542_((double)horizontalSpeed, (double)horizontalSpeed, (double)horizontalSpeed);
                speed = rndVec.m_82520_(0.0, (double)verticalSpeed, 0.0);
                v = this.f_19796_.m_188501_() * step - step / 2.0f;
                p = pos.m_82549_(direction.m_82542_((double)v, (double)v, (double)v));
                ParticleOptions particleOptions = this.randomParticle();
                float speedm = 2.0f;
                this.m_9236_().m_6493_(particleOptions, true, p.f_82479_, p.f_82480_, p.f_82481_, speed.f_82479_ * (double)speedm, speed.f_82480_ * (double)speedm, speed.f_82481_ * (double)speedm);
            }
            for (g = 0; g <= bigsmokerows; ++g) {
                Object options;
                float p2 = (float)g / ((float)bigsmokerows - 1.0f);
                Vec3 dir1 = direction.m_82524_(1.5707964f);
                Vec3 dir2 = direction.m_82524_(-1.5707964f);
                float v2 = this.f_19796_.m_188501_() * step - step / 2.0f;
                float v22 = this.f_19796_.m_188501_() * step - step / 2.0f;
                Vec3 ppos = pos.m_82549_(direction.m_82542_((double)v2, (double)v2, (double)v2));
                Vec3 ppos2 = pos.m_82549_(direction.m_82542_((double)v22, (double)v22, (double)v22));
                if (this.f_19796_.m_188501_() > 0.1f) {
                    float col = this.f_19796_.m_188501_() * 0.4f + 0.2f;
                    options = BigSmokeParticleOptions.builder().lifetime(0, 0, 60 + this.f_19796_.m_188503_(20)).friction(0.9f - p2 * 0.1f).size(3.0f - p2 * 1.5f).color(col, col, col).minSpeed(0.05f).build();
                } else {
                    Vector3f col = MalkuthEntity.getMalkuthAttackPreparationParticleColor(this.getAttackType());
                    options = BallParticleOptions.builder().size(3.0f - p2 * 1.5f).color(col.x, col.y + 0.2f, col.z).friction(0.9f - p2 * 0.1f).scalingOptions(0, 0, 40 + this.f_19796_.m_188503_(20)).build();
                }
                float speed2 = p2 * 2.0f + 0.15f + this.f_19796_.m_188501_() * 1.5f * (p2 + 0.5f);
                float rndy = this.f_19796_.m_188501_() * (1.0f - p2) * 0.25f;
                float rndy2 = this.f_19796_.m_188501_() * (1.0f - p2) * 0.25f;
                this.m_9236_().m_6493_((ParticleOptions)options, true, ppos.f_82479_, ppos.f_82480_ + (double)rndy, ppos.f_82481_, dir1.f_82479_ * (double)speed2, dir1.f_82480_ * (double)speed2, dir1.f_82481_ * (double)speed2);
                this.m_9236_().m_6493_((ParticleOptions)options, true, ppos2.f_82479_, ppos2.f_82480_ + (double)rndy2, ppos2.f_82481_, dir2.f_82479_ * (double)speed2, dir2.f_82480_ * (double)speed2, dir2.f_82481_ * (double)speed2);
            }
        }
    }

    private ParticleOptions randomParticle() {
        if (this.getAttackType().isFire()) {
            if (this.f_19796_.m_188501_() > 0.5f) {
                GravityParticleOptions particleOptions = new GravityParticleOptions(BossParticles.FLAME_WITH_STONE.get(), 40 + this.f_19796_.m_188503_(4), 0.75f + this.f_19796_.m_188501_() * 0.25f, 1.5999999f, 2.0f, true);
                return particleOptions;
            }
            return ParticleTypes.f_123756_;
        }
        GravityParticleOptions particleOptions = new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 40 + this.f_19796_.m_188503_(4), 0.75f + this.f_19796_.m_188501_() * 0.25f, 1.5999999f, 2.0f, true);
        return particleOptions;
    }

    private BlockState randomParticleBlock() {
        if (this.f_19796_.m_188501_() > 0.5f) {
            if (this.getAttackType() == MalkuthAttackType.ICE) {
                return Blocks.f_50568_.m_49966_();
            }
            return Blocks.f_50450_.m_49966_();
        }
        return Blocks.f_50730_.m_49966_();
    }

    public float getCurrentMoveUpTime(float pticks) {
        return FDMathUtil.clamp((float)((float)this.f_19797_ + pticks), (float)0.0f, (float)TIME_TO_RISE);
    }

    public float getCurrentHitTime(float pticks) {
        return FDMathUtil.clamp((float)((float)(this.f_19797_ - TIME_TO_RISE) + pticks), (float)0.0f, (float)TIME_TO_HIT);
    }

    public float getDissolveTime(float pticks) {
        return FDMathUtil.clamp((float)((float)(this.f_19797_ - TIME_TO_RISE - TIME_TO_HIT) + pticks), (float)0.0f, (float)DISSOLVE_TIME);
    }

    public MalkuthAttackType getAttackType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(MALKUTH_ATTACK_TYPE));
    }

    public void setAttackType(MalkuthAttackType type) {
        this.f_19804_.m_135381_(MALKUTH_ATTACK_TYPE, (Object)type);
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(MALKUTH_ATTACK_TYPE, (Object)MalkuthAttackType.FIRE);
    }

    protected void m_7378_(CompoundTag tag) {
        if (tag.m_128441_("mtype")) {
            this.setAttackType(MalkuthAttackType.valueOf(tag.m_128461_("mtype")));
        }
        this.autoLoad(tag);
    }

    protected void m_7380_(CompoundTag tag) {
        tag.m_128359_("mtype", this.getAttackType().name());
        this.autoSave(tag);
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }
}

