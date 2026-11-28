/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.FDTagHelper
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.content.entities.IEffectImmune;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthWeaknessHandler;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonProjectile;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.FDTagHelper;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class MalkuthCannonEntity
extends FDLivingEntity
implements AutoSerializable,
IEffectImmune {
    private static FDModel CLIENT_MODEL;
    public static final EntityDataAccessor<MalkuthAttackType> MALKUTH_ATTACK_TYPE;
    public static final EntityDataAccessor<Boolean> PLAYER_CONTROLLED;
    public static final EntityDataAccessor<Boolean> BROKEN;
    public static final EntityDataAccessor<Boolean> BROKEN_REQUIRES_MATERIALS;
    private List<Vec3> shootTargets = new ArrayList<Vec3>();
    @SerializableField
    private MalkuthAttackType malkuthCannonType = MalkuthAttackType.ICE;
    @SerializableField
    private int shootTickCount = 0;
    @SerializableField
    private float damage;

    public MalkuthCannonEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.m_20242_(true);
        this.getAnimationSystem().startAnimation("SUMMON", AnimationTicker.builder(BossAnims.MALKUTH_CANNON_SUMMON).setSpeed(0.75f).build());
        if (level.f_46443_) {
            CLIENT_MODEL = new FDModel(BossModels.MALKUTH_CANNON.get());
        }
    }

    public static MalkuthCannonEntity summon(Level level, Vec3 pos, Vec3 lookAt, MalkuthAttackType malkuthAttackType) {
        MalkuthCannonEntity malkuthCannonEntity = new MalkuthCannonEntity(BossEntities.MALKUTH_CANNON.get(), level);
        malkuthCannonEntity.m_146884_(pos);
        malkuthCannonEntity.setCannonType(malkuthAttackType);
        malkuthCannonEntity.m_7618_(EntityAnchorArgument.Anchor.FEET, lookAt);
        level.m_7967_((Entity)malkuthCannonEntity);
        return malkuthCannonEntity;
    }

    public void shoot(List<Vec3> targets, float damage) {
        if (this.m_9236_().f_46443_) {
            return;
        }
        if (this.shootTargets.isEmpty()) {
            this.shootTargets = new ArrayList<Vec3>(targets);
            this.damage = damage;
            this.shootTickCount = 20;
        }
    }

    public InteractionResult m_7111_(Player player, Vec3 vec, InteractionHand hand) {
        if (!this.m_9236_().f_46443_ && hand == InteractionHand.MAIN_HAND && this.isPlayerControlled() && MalkuthWeaknessHandler.isWeakTo(player, this.getCannonType())) {
            if (this.isBroken()) {
                return InteractionResult.SUCCESS;
            }
            MalkuthAttackType attackType = this.getCannonType();
            if (attackType.isFire()) {
                Vec3 fireOffset = new Vec3(6.5, 14.0, 55.0);
                this.shoot(List.of(fireOffset.m_82549_(this.m_20182_())), 1.0f);
            } else {
                Vec3 iceOffset = new Vec3(-6.5, 14.0, 55.0);
                this.shoot(List.of(iceOffset.m_82549_(this.m_20182_())), 1.0f);
            }
            player.m_6674_(hand);
        }
        return super.m_7111_(player, vec, hand);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (!this.shootTargets.isEmpty()) {
                if (this.shootTickCount == 20) {
                    this.getAnimationSystem().startAnimation("SHOOT", AnimationTicker.builder(BossAnims.MALKUTH_CANNON_SHOOT).build());
                } else if (this.shootTickCount == 16) {
                    Vec3 frwd = this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_();
                    Vec3 summonPos = this.m_20182_().m_82520_(frwd.f_82479_ * 1.2, 1.75, frwd.f_82481_ * 1.2);
                    BossUtil.malkuthCannonShoot((ServerLevel)this.m_9236_(), this.malkuthCannonType, summonPos, frwd.m_82520_(0.0, (double)0.6f, 0.0), 100.0);
                    for (Vec3 target : this.shootTargets) {
                        Vec3 speed = BossUtil.calculateMortarProjectileVelocity(summonPos, target, -0.08f, 35 + this.f_19796_.m_188503_(10));
                        MalkuthCannonProjectile.summon(this.m_9236_(), summonPos, speed, 1000, this.malkuthCannonType, this.damage);
                    }
                    ((ServerLevel)this.m_9236_()).m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_CANNON_SHOOT.get(), SoundSource.HOSTILE, 4.0f, 0.75f);
                    PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(50.0f).amplitude(1.5f).inTime(0).stayTime(0).outTime(10).build(), (Vec3)this.m_20182_().m_82549_(this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82520_(0.0, 1.0, 0.0)), (double)60.0);
                } else if (this.shootTickCount == 5) {
                    if (this.isPlayerControlled()) {
                        this.setBroken(true);
                    }
                } else if (this.shootTickCount <= 0) {
                    this.shootTargets.clear();
                }
                --this.shootTickCount;
            }
        } else {
            this.brokenRequiresMaterialsParticles();
            this.brokenParticles();
        }
    }

    public void cancelShot() {
        this.getAnimationSystem().stopAnimation("SHOOT");
        this.shootTargets.clear();
        this.shootTickCount = 0;
        this.damage = 0.0f;
    }

    private void brokenRequiresMaterialsParticles() {
        if (!this.requiresRepair()) {
            return;
        }
        Vec3 pos = this.m_20182_();
        float t = (float)Math.sin((float)this.f_19797_ / 30.0f);
        float h = t * 0.125f;
        Vec3 basePPos = pos.m_82520_(0.0, (double)(4.0f + h), 0.0);
        for (int i = 0; i < 2; ++i) {
            Vector3f col = MalkuthEntity.getAndRandomizeColor(this.getCannonType(), this.m_9236_().f_46441_);
            BallParticleOptions ballParticleOptions = BallParticleOptions.builder().color(col.x, col.y, col.z).scalingOptions(0, 0, 20).size(0.025f).brightness(2).build();
            Vec3 fwd = this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_();
            this.m_9236_().m_7106_((ParticleOptions)ballParticleOptions, basePPos.f_82479_ + (double)(this.f_19796_.m_188501_() * 0.25f) - 0.125 - fwd.f_82479_ * (double)0.05f, basePPos.f_82480_ + (double)(this.f_19796_.m_188501_() * 0.25f) - 0.125, basePPos.f_82481_ + (double)(this.f_19796_.m_188501_() * 0.25f) - 0.125 - fwd.f_82481_ * (double)0.05f, 0.0, -0.05, 0.0);
        }
    }

    private void brokenParticles() {
        if (this.m_9236_().m_46467_() % 10L == 0L && this.isBroken()) {
            Matrix4f t = this.getModelPartTransformation((Entity)this, "cannon2", CLIENT_MODEL);
            Vector3f position = t.transformPosition(0.0f, 0.0f, -1.25f, new Vector3f()).add((float)this.m_20185_(), (float)this.m_20186_(), (float)this.m_20189_());
            float r = this.f_19796_.m_188501_() * 0.2f + 0.2f;
            BigSmokeParticleOptions options = BigSmokeParticleOptions.builder().color(r, r, r).size(1.0f).lifetime(0, 0, 60).build();
            this.m_9236_().m_7106_((ParticleOptions)options, (double)position.x, (double)position.y, (double)position.z, 0.0, (double)0.05f, 0.0);
            this.m_9236_().m_7106_((ParticleOptions)options, (double)position.x, (double)position.y, (double)position.z, 0.0, (double)0.05f, 0.0);
        }
    }

    public void setBrokenRequiresMaterials(boolean broken) {
        if (broken) {
            if (!this.isBroken()) {
                this.getAnimationSystem().startAnimation("BROKE", AnimationTicker.builder(BossAnims.MALKUTH_CANNON_BREAK).build());
            }
            this.f_19804_.m_135381_(BROKEN, (Object)false);
            this.f_19804_.m_135381_(BROKEN_REQUIRES_MATERIALS, (Object)true);
        } else if (this.requiresRepair()) {
            if (this.isBroken()) {
                this.getAnimationSystem().startAnimation("BROKE", AnimationTicker.builder(BossAnims.MALKUTH_CANNON_REPAIR).build());
            }
            this.f_19804_.m_135381_(BROKEN_REQUIRES_MATERIALS, (Object)false);
        }
    }

    public void repairWithMaterial() {
        this.setBrokenRequiresMaterials(false);
    }

    public void setBroken(boolean broken) {
        if (!this.requiresRepair()) {
            if (broken) {
                if (!this.isBroken()) {
                    this.getAnimationSystem().startAnimation("BROKE", AnimationTicker.builder(BossAnims.MALKUTH_CANNON_BREAK).build());
                }
            } else if (this.isBroken()) {
                this.getAnimationSystem().startAnimation("BROKE", AnimationTicker.builder(BossAnims.MALKUTH_CANNON_REPAIR).build());
            }
            this.f_19804_.m_135381_(BROKEN, (Object)broken);
        }
    }

    public boolean requiresRepair() {
        return (Boolean)this.f_19804_.m_135370_(BROKEN_REQUIRES_MATERIALS);
    }

    public boolean isBroken() {
        return (Boolean)this.f_19804_.m_135370_(BROKEN) != false || (Boolean)this.f_19804_.m_135370_(BROKEN_REQUIRES_MATERIALS) != false;
    }

    public boolean isPlayerControlled() {
        return (Boolean)this.f_19804_.m_135370_(PLAYER_CONTROLLED);
    }

    public void setPlayerControlled(boolean controlled) {
        this.f_19804_.m_135381_(PLAYER_CONTROLLED, (Object)controlled);
    }

    private void setCannonType(MalkuthAttackType malkuthAttackType) {
        this.f_19804_.m_135381_(MALKUTH_ATTACK_TYPE, (Object)malkuthAttackType);
        this.malkuthCannonType = malkuthAttackType;
    }

    public MalkuthAttackType getCannonType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(MALKUTH_ATTACK_TYPE));
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(MALKUTH_ATTACK_TYPE, (Object)MalkuthAttackType.FIRE);
        this.f_19804_.m_135372_(BROKEN_REQUIRES_MATERIALS, (Object)false);
        this.f_19804_.m_135372_(PLAYER_CONTROLLED, (Object)false);
        this.f_19804_.m_135372_(BROKEN, (Object)false);
    }

    public boolean m_6469_(DamageSource src, float damage) {
        if (src.m_276093_(DamageTypes.f_268724_) || src.m_276093_(DamageTypes.f_286979_)) {
            return super.m_6469_(src, damage);
        }
        return false;
    }

    public boolean m_20068_() {
        return true;
    }

    public void m_7334_(Entity p_21294_) {
    }

    public void m_5997_(double p_20286_, double p_20287_, double p_20288_) {
    }

    protected void m_6138_() {
    }

    public void m_20256_(Vec3 p_20257_) {
    }

    public void m_20334_(double p_20335_, double p_20336_, double p_20337_) {
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        this.shootTargets = FDTagHelper.loadVec3List((CompoundTag)tag, (String)"targets");
        this.f_19804_.m_135381_(BROKEN, (Object)tag.m_128471_("broken"));
        this.f_19804_.m_135381_(BROKEN_REQUIRES_MATERIALS, (Object)tag.m_128471_("broken_materials"));
        this.f_19804_.m_135381_(PLAYER_CONTROLLED, (Object)tag.m_128471_("playerControlled"));
        this.setCannonType(this.malkuthCannonType);
        if (this.isBroken()) {
            this.getAnimationSystem().startAnimation("BROKE", AnimationTicker.builder(BossAnims.MALKUTH_CANNON_BREAK).build());
        }
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        FDTagHelper.saveVec3List((CompoundTag)tag, (String)"targets", this.shootTargets);
        this.autoSave(tag);
        this.setCannonType(this.malkuthCannonType);
        tag.m_128379_("broken", ((Boolean)this.f_19804_.m_135370_(BROKEN)).booleanValue());
        tag.m_128379_("broken_materials", ((Boolean)this.f_19804_.m_135370_(BROKEN_REQUIRES_MATERIALS)).booleanValue());
        tag.m_128379_("playerControlled", ((Boolean)this.f_19804_.m_135370_(PLAYER_CONTROLLED)).booleanValue());
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }

    static {
        MALKUTH_ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthCannonEntity.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
        PLAYER_CONTROLLED = SynchedEntityData.m_135353_(MalkuthCannonEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
        BROKEN = SynchedEntityData.m_135353_(MalkuthCannonEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
        BROKEN_REQUIRES_MATERIALS = SynchedEntityData.m_135353_(MalkuthCannonEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    }
}

