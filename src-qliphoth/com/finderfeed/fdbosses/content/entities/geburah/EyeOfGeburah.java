/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.projectile.EyeOfEnder
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EyeOfGeburah
extends EyeOfEnder {
    public EyeOfGeburah(EntityType<? extends EyeOfEnder> type, Level p_36958_) {
        super(type, p_36958_);
    }

    public EyeOfGeburah(Level level, double x, double y, double z) {
        this(BossEntities.EYE_OF_CHESED.get(), level);
        this.m_6034_(x, y, z);
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            Vec3 vec3 = this.m_20184_();
            double d0 = this.m_20185_();
            double d1 = this.m_20186_();
            double d2 = this.m_20189_();
            Object options = (double)this.m_9236_().f_46441_.m_188501_() > 0.25 ? BallParticleOptions.builder().size(0.15f).brightness(3).scalingOptions(2, 0, 20).color(0.2f, 0.5f, 1.0f).build() : ParticleTypes.f_123810_;
            this.m_9236_().m_7106_((ParticleOptions)options, d0 - vec3.f_82479_ * 0.5 + this.f_19796_.m_188500_() * 0.6 - 0.3, d1 - vec3.f_82480_ * 0.5, d2 - vec3.f_82481_ * 0.5 + this.f_19796_.m_188500_() * 0.6 - 0.3, vec3.f_82479_ * 0.25, vec3.f_82480_ * 0.25, vec3.f_82481_ * 0.25);
        }
    }
}

