/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_opening_floor;

import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GeburahOpeningFloor
extends Entity {
    public GeburahOpeningFloor(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.f_19797_ > 500) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
            if (this.f_19797_ == 17) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.GEBURAH_FLOOR_OPENING.get(), SoundSource.AMBIENT, 3.0f, 1.0f);
            }
        } else if (this.f_19797_ == 17) {
            for (int i = 0; i < 200; ++i) {
                float c = 0.3f + this.f_19796_.m_188501_() * 0.2f;
                Vec3 v = new Vec3((double)(FDEasings.easeOut((float)this.f_19796_.m_188501_()) * 5.0f), 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
                BigSmokeParticleOptions bigSmokeParticle = BigSmokeParticleOptions.builder().size(1.0f + this.f_19796_.m_188501_() * 2.0f).lifetime(5 + this.f_19796_.m_188503_(5), 0, 40 + this.f_19796_.m_188503_(20)).color(c, c, c, c).minSpeed(0.05f).friction(0.7f).build();
                Vec3 pos = this.m_20182_().m_82549_(v).m_82520_(0.0, (double)0.1f, 0.0);
                this.m_9236_().m_6493_((ParticleOptions)bigSmokeParticle, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, (double)(this.f_19796_.m_188501_() * 4.0f - 2.0f), 0.0, (double)(this.f_19796_.m_188501_() * 4.0f - 2.0f));
            }
        }
    }

    protected void m_8097_() {
    }

    protected void m_7378_(CompoundTag p_20052_) {
    }

    protected void m_7380_(CompoundTag p_20139_) {
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }
}

