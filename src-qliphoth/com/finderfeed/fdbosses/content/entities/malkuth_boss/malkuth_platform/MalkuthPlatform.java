/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_platform;

import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MalkuthPlatform
extends FDEntity {
    public static final EntityDataAccessor<Boolean> CAN_BE_COLLIDED_WITH = SynchedEntityData.m_135353_(MalkuthPlatform.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);

    public MalkuthPlatform(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
        this.m_20242_(true);
        this.f_19794_ = true;
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.f_19797_ > 20) {
                this.f_19804_.m_135381_(CAN_BE_COLLIDED_WITH, (Object)true);
            }
        } else if (this.f_19797_ > 40 && this.f_19797_ % 10 == 0) {
            Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.FIRE);
            FDColor fireColorStart = new FDColor(colFire.x, colFire.y - this.f_19796_.m_188501_() * 0.1f - 0.3f, colFire.z, 0.5f);
            FDColor fireColor = new FDColor(colFire.x, colFire.y + this.f_19796_.m_188501_() * 0.1f, colFire.z, 1.0f);
            StripeParticleOptions stripeParticleOptions = StripeParticleOptions.createHorizontalCircling(fireColorStart, fireColor, new Vec3((double)0.01f, 1.0, 0.0), this.f_19796_.m_188501_() * (float)Math.PI * 2.0f, 0.2f, 20, 50, 6.0f, 4.0f, 2.0f, 0.5f, false, false);
            this.m_9236_().m_6493_((ParticleOptions)stripeParticleOptions, true, this.m_20185_(), this.m_20186_() - 6.0, this.m_20189_(), 0.0, 0.0, 0.0);
        }
    }

    public void m_142036_() {
        super.m_142036_();
        for (int i = 0; i < 150; ++i) {
            Vector3f color = MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.FIRE, this.f_19796_);
            BallParticleOptions ballParticleOptions = BallParticleOptions.builder().color(color.x, color.y, color.z).scalingOptions(0, 0, 20).size(0.25f + this.f_19796_.m_188501_() * 0.1f).friction(0.8f).brightness(2).build();
            float p = FDEasings.easeOut((float)this.f_19796_.m_188501_());
            float h = p * 6.0f;
            float v = p * 4.0f;
            Vec3 offs = new Vec3((double)v, 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f);
            Vec3 ppos = this.m_20182_().m_82520_(offs.f_82479_, (double)(-6.0f + h), offs.f_82481_);
            this.m_9236_().m_7106_((ParticleOptions)ballParticleOptions, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, (double)this.f_19796_.m_188501_() - 0.5, (double)this.f_19796_.m_188501_() - 0.5, (double)this.f_19796_.m_188501_() - 0.5);
        }
    }

    public void m_6074_() {
        super.m_6074_();
    }

    public void m_5997_(double p_20286_, double p_20287_, double p_20288_) {
    }

    public void m_7334_(Entity p_20293_) {
    }

    public boolean m_5829_() {
        return (Boolean)this.f_19804_.m_135370_(CAN_BE_COLLIDED_WITH);
    }

    public boolean m_6087_() {
        return true;
    }

    public boolean m_6094_() {
        return false;
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(CAN_BE_COLLIDED_WITH, (Object)false);
    }

    public void m_7378_(CompoundTag tag) {
        this.f_19804_.m_135381_(CAN_BE_COLLIDED_WITH, (Object)tag.m_128471_("can_collide"));
    }

    public void m_7380_(CompoundTag tag) {
        tag.m_128379_("can_collide", ((Boolean)this.f_19804_.m_135370_(CAN_BE_COLLIDED_WITH)).booleanValue());
    }
}

