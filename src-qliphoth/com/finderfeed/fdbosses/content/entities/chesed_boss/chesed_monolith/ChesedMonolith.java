/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_monolith;

import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ChesedMonolith
extends FDLivingEntity
implements AutoSerializable,
ChesedBossBuddy {
    public static final EntityDataAccessor<Boolean> DEACTIVATED = SynchedEntityData.m_135353_(ChesedMonolith.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    @SerializableField
    private boolean deactivated = false;
    @SerializableField
    private boolean immuneToAttacks = false;

    public ChesedMonolith(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.m_20242_(true);
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            if (!this.isDeactivated()) {
                this.getAnimationSystem().startAnimation("IDLE", AnimationTicker.builder(BossAnims.CHESED_MONOLITH_IDLE).build());
                this.getAnimationSystem().stopAnimation("TURN_OFF");
            } else {
                this.getAnimationSystem().stopAnimation("IDLE");
                this.getAnimationSystem().startAnimation("TURN_OFF", AnimationTicker.builder(BossAnims.CHESED_MONOLITH_TURN_OFF).build());
            }
        } else {
            this.f_19804_.m_135381_(DEACTIVATED, (Object)this.deactivated);
        }
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    public void m_7350_(EntityDataAccessor<?> accessor) {
        super.m_7350_(accessor);
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(DEACTIVATED, (Object)false);
    }

    public boolean isDeactivated() {
        if (!this.m_9236_().f_46443_) {
            return this.deactivated;
        }
        return (Boolean)this.f_19804_.m_135370_(DEACTIVATED);
    }

    public boolean m_6469_(DamageSource src, float damage) {
        if ((this.isDeactivated() || this.isImmuneToAttacks()) && !src.m_276093_(DamageTypes.f_286979_) && !src.m_276093_(DamageTypes.f_268724_)) {
            return false;
        }
        return super.m_6469_(src, damage);
    }

    public void m_20334_(double p_20335_, double p_20336_, double p_20337_) {
        super.m_20334_(0.0, 0.0, 0.0);
    }

    public void m_20256_(Vec3 p_20257_) {
        super.m_20256_(Vec3.f_82478_);
    }

    public void m_6667_(DamageSource src) {
        if (src.m_276093_(DamageTypes.f_286979_) || src.m_276093_(DamageTypes.f_268724_)) {
            super.m_6667_(src);
        } else {
            Level level = this.m_9236_();
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                this.m_21153_(this.m_21233_());
                this.setDeactivated(true);
                BallParticleOptions options = BallParticleOptions.builder().size(0.5f).color(0.3f, 1.0f, 1.0f, 1.0f).physics(false).friction(0.7f).scalingOptions(0, 0, 10 + this.f_19796_.m_188503_(4)).build();
                serverLevel.m_8767_((ParticleOptions)options, this.m_20185_(), this.m_20186_() + 2.5, this.m_20189_(), 30, (double)0.1f, (double)0.1f, (double)0.1f, 0.25);
                LightningParticleOptions li = LightningParticleOptions.builder().color(50 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 200 + this.f_19796_.m_188503_(50)).quadSize(0.2f).lifetime(10).randomRoll(true).build();
                serverLevel.m_8767_((ParticleOptions)li, this.m_20185_(), this.m_20186_() + 2.5, this.m_20189_(), 20, (double)0.1f, (double)0.1f, (double)0.1f, (double)0.05f);
            }
        }
    }

    @Nullable
    protected SoundEvent m_7975_(DamageSource p_21239_) {
        return BossSounds.MONOLITH_HIT.get();
    }

    @Nullable
    protected SoundEvent m_5592_() {
        return BossSounds.MONOLITH_HIT.get();
    }

    public boolean isImmuneToAttacks() {
        return this.immuneToAttacks;
    }

    public void setImmuneToAttacks(boolean immuneToAttacks) {
        this.immuneToAttacks = immuneToAttacks;
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

    public void m_147240_(double p_147241_, double p_147242_, double p_147243_) {
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        boolean deactivated = tag.m_128471_("deactivated");
        this.setDeactivated(deactivated);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
        boolean deactivated = this.isDeactivated();
        tag.m_128379_("deactivated", deactivated);
    }
}

