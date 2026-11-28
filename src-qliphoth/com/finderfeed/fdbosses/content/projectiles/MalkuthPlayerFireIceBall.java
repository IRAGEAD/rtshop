/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDProjectile
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.Direction
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.projectile.AbstractHurtingProjectile
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.projectiles;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_crush.MalkuthCrushAttack;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDProjectile;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MalkuthPlayerFireIceBall
extends FDProjectile
implements AutoSerializable {
    public static final EntityDataAccessor<MalkuthAttackType> ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthPlayerFireIceBall.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    private ItemStack itemStack;

    public static MalkuthPlayerFireIceBall summon(LivingEntity owner, Vec3 pos, Vec3 speed, MalkuthAttackType attackType, ItemStack tool) {
        MalkuthPlayerFireIceBall malkuthPlayerFireIceBall = new MalkuthPlayerFireIceBall(BossEntities.MALKUTH_PLAYER_FIREBALL.get(), owner.m_9236_());
        malkuthPlayerFireIceBall.m_5602_((Entity)owner);
        malkuthPlayerFireIceBall.m_146884_(pos);
        malkuthPlayerFireIceBall.m_20256_(speed);
        malkuthPlayerFireIceBall.setAttackType(attackType);
        malkuthPlayerFireIceBall.itemStack = tool;
        owner.m_9236_().m_6263_(null, pos.f_82479_, pos.f_82480_, pos.f_82481_, BossSounds.MALKUTH_FIREBALL_LAUNCH.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        owner.m_9236_().m_7967_((Entity)malkuthPlayerFireIceBall);
        return malkuthPlayerFireIceBall;
    }

    public MalkuthPlayerFireIceBall(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.f_19797_ > 1000 || this.itemStack == null || this.itemStack.m_41619_()) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
        } else {
            this.particles();
        }
    }

    protected void m_5790_(EntityHitResult result) {
        LivingEntity livingEntity;
        Entity entity;
        super.m_5790_(result);
        if (!this.m_9236_().f_46443_ && (entity = result.m_82443_()) instanceof LivingEntity && (livingEntity = (LivingEntity)entity) != this.m_19749_()) {
            Vec3 location = result.m_82450_();
            Direction direction = null;
            if (Math.abs(location.f_82480_ - livingEntity.m_20186_()) < 3.0 && livingEntity.m_20096_()) {
                location = new Vec3(location.f_82479_, livingEntity.m_20186_(), location.f_82481_);
                direction = Direction.UP;
            }
            this.explode(location, direction);
        }
    }

    protected void m_8060_(BlockHitResult result) {
        super.m_8060_(result);
        if (!this.m_9236_().f_46443_) {
            Direction direction = result.m_82434_();
            this.explode(result.m_82450_(), direction);
        }
    }

    public void explode(Vec3 pos, Direction crushDirectionOrNoCrush) {
        if (this.getAttackType().isIce()) {
            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_ICE_FIREBALL_EXPLOSION.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
        } else {
            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_FIREBALL_EXPLOSION.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
        }
        BossUtil.malkuthPlayerFireballExplosionParticles((ServerLevel)this.m_9236_(), pos, this.getAttackType());
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(50.0f).amplitude(1.5f).inTime(0).stayTime(0).outTime(10).build(), (Vec3)pos, (double)40.0);
        this.m_142687_(Entity.RemovalReason.DISCARDED);
        if (crushDirectionOrNoCrush != null) {
            MalkuthCrushAttack.summon(this.m_9236_(), pos, 0.0f, crushDirectionOrNoCrush, this.getAttackType());
            this.m_9236_().m_6263_(null, pos.f_82479_, pos.f_82480_, pos.f_82481_, BossSounds.ROCK_IMPACT.get(), SoundSource.PLAYERS, 3.0f, 0.8f);
            this.m_9236_().m_6263_(null, pos.f_82479_, pos.f_82480_, pos.f_82481_, BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.PLAYERS, 3.0f, 0.8f);
        }
        DamageSource damageSource = this.m_9236_().m_269111_().m_269264_();
        Entity entity = this.m_19749_();
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            damageSource = this.m_9236_().m_269111_().m_269333_(livingEntity);
        }
        float dmgModifier = BossConfigs.BOSS_CONFIG.get().itemConfig.playerMalkuthFireballToolDamagePercent / 100.0f;
        for (LivingEntity target : BossTargetFinder.getEntitiesInSphere(LivingEntity.class, this.m_9236_(), pos, 3.0f)) {
            double damage = 5.0;
            if (target == this.m_19749_()) continue;
            Entity entity2 = this.m_19749_();
            if (entity2 instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity2;
                damage = BossUtil.getToolDamage(livingEntity, (Entity)target, this.itemStack) * (double)dmgModifier;
            }
            if (this.getAttackType().isFire()) {
                target.m_7311_(200);
            } else {
                target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 200, 2));
            }
            target.m_6469_(damageSource, (float)damage);
        }
    }

    private void particles() {
        Vec3 between = new Vec3(this.m_20185_() - this.f_19854_, this.m_20186_() - this.f_19855_, this.m_20189_() - this.f_19856_);
        double length = between.m_82553_();
        Vec3 pos = this.m_20182_().m_82520_(0.0, (double)(this.m_20206_() / 2.0f), 0.0);
        float maxrad = 0.5f;
        float g = -0.001f;
        while ((double)g < length * (double)0.8f) {
            for (int i = 0; i < 10; ++i) {
                Vector3f color = MalkuthEntity.getAndRandomizeColor(this.getAttackType(), this.f_19796_);
                Object particleOptions = this.f_19796_.m_188501_() > 0.1f ? BallParticleOptions.builder().color(color.x, color.y, color.z).scalingOptions(0, 0, 4 + this.f_19796_.m_188503_(4)).brightness(1).build() : (this.getAttackType().isFire() ? ParticleTypes.f_123744_ : new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 5 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 0.0f, 2.0f, true));
                float rndOut = maxrad / 2.0f + this.f_19796_.m_188501_() * maxrad / 2.0f;
                Vec3 rndOffs = new Vec3((double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f)).m_82541_().m_82542_((double)rndOut, (double)rndOut, (double)rndOut);
                Vec3 distOffset = between.m_82541_().m_82542_((double)g, (double)g, (double)g);
                Vec3 ppos = pos.m_82549_(rndOffs).m_82546_(distOffset);
                float vspeed = length == 0.0 ? -this.f_19796_.m_188501_() * 0.1f : 0.0f;
                this.m_9236_().m_6493_((ParticleOptions)particleOptions, true, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, 0.0, (double)vspeed, 0.0);
            }
            g += maxrad;
        }
    }

    public boolean m_6087_() {
        return true;
    }

    public MalkuthAttackType getAttackType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE));
    }

    public void setAttackType(MalkuthAttackType type) {
        this.f_19804_.m_135381_(ATTACK_TYPE, (Object)type);
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(ATTACK_TYPE, (Object)MalkuthAttackType.ICE);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        tag.m_128365_("item", (Tag)this.itemStack.m_41739_(new CompoundTag()));
        tag.m_128359_("mtype", ((MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE))).name());
        this.autoSave(tag);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        if (tag.m_128441_("mtype")) {
            this.f_19804_.m_135381_(ATTACK_TYPE, (Object)MalkuthAttackType.valueOf(tag.m_128461_("mtype")));
        }
        if (tag.m_128441_("item")) {
            this.itemStack = ItemStack.m_41712_((CompoundTag)tag.m_128469_("item"));
        }
        this.autoLoad(tag);
    }
}

