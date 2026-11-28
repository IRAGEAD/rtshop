/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal.MalkuthRepairEntity;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public class MalkuthRepairCrystal
extends FDEntity
implements AutoSerializable {
    public static final EntityDataAccessor<MalkuthAttackType> CRYSTAL_TYPE = SynchedEntityData.m_135353_(MalkuthRepairCrystal.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    public static final EntityDataAccessor<Boolean> HIDDEN = SynchedEntityData.m_135353_(MalkuthRepairCrystal.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);

    public MalkuthRepairCrystal(EntityType<?> type, Level level) {
        super(type, level);
        this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_REPAIR_CRYSTAL_SUMMON).setLoopMode(Animation.LoopMode.ONCE).build());
    }

    public void m_8119_() {
        super.m_8119_();
    }

    public void setHidden(boolean hidden) {
        this.f_19804_.m_135381_(HIDDEN, (Object)hidden);
        if (hidden) {
            this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_REPAIR_CRYSTAL_SUMMON).setLoopMode(Animation.LoopMode.HOLD_ON_LAST_FRAME).setToNullTransitionTime(0).reversed().build());
        } else {
            this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_REPAIR_CRYSTAL_SUMMON).setLoopMode(Animation.LoopMode.HOLD_ON_LAST_FRAME).setToNullTransitionTime(0).build());
        }
    }

    public boolean isHidden() {
        return (Boolean)this.f_19804_.m_135370_(HIDDEN);
    }

    public MalkuthAttackType getCrystalType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(CRYSTAL_TYPE));
    }

    public void setCrystalType(MalkuthAttackType crystalType) {
        this.f_19804_.m_135381_(CRYSTAL_TYPE, (Object)crystalType);
    }

    public void destroyAndSummonRepairMaterial() {
        if (this.isHidden()) {
            return;
        }
        MalkuthRepairEntity malkuthRepairEntity = MalkuthRepairEntity.summon(this.m_9236_(), this.m_20182_().m_82520_(0.0, 3.0, 0.0), (MalkuthAttackType)((Object)this.f_19804_.m_135370_(CRYSTAL_TYPE)));
        Vector3f color = MalkuthEntity.getMalkuthAttackPreparationParticleColor(this.getCrystalType());
        ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)BallParticleOptions.builder().color(color.x, color.y, color.z).brightness(2).size(0.5f).scalingOptions(0, 0, 20).friction(0.6f).build(), this.m_20185_(), this.m_20186_() + 1.5, this.m_20189_(), 100, (double)0.2f, (double)0.2f, (double)0.2f, 0.5);
        this.m_142687_(Entity.RemovalReason.DISCARDED);
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(CRYSTAL_TYPE, (Object)MalkuthAttackType.FIRE);
        this.f_19804_.m_135372_(HIDDEN, (Object)false);
    }

    public boolean m_6087_() {
        return true;
    }

    protected void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        tag.m_128359_("crystalType", this.getCrystalType().name());
        tag.m_128379_("hidden", ((Boolean)this.f_19804_.m_135370_(HIDDEN)).booleanValue());
        this.autoSave(tag);
    }

    protected void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        if (tag.m_128441_("crystalType")) {
            this.setCrystalType(MalkuthAttackType.valueOf(tag.m_128461_("crystalType")));
        }
        this.f_19804_.m_135381_(HIDDEN, (Object)tag.m_128471_("hidden"));
        this.setHidden((Boolean)this.f_19804_.m_135370_(HIDDEN));
        this.autoLoad(tag);
    }
}

