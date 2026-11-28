/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.projectile.EyeOfEnder
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class EyeOfMalkuthEntity
extends EyeOfEnder {
    public EyeOfMalkuthEntity(EntityType<? extends EyeOfEnder> type, Level p_36958_) {
        super(type, p_36958_);
    }

    public EyeOfMalkuthEntity(Level level, double x, double y, double z) {
        this(BossEntities.EYE_OF_MALKUTH.get(), level);
        this.m_6034_(x, y, z);
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            Vec3 vec3 = this.m_20184_();
            double d0 = this.m_20185_();
            double d1 = this.m_20186_();
            double d2 = this.m_20189_();
            Vector3f color = (double)this.m_9236_().f_46441_.m_188501_() > 0.5 ? MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.FIRE, this.f_19796_) : MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.ICE, this.f_19796_);
            BallParticleOptions options = BallParticleOptions.builder().size(0.2f).brightness(2).scalingOptions(2, 0, 20).color(color.x, color.y, color.z).build();
            this.m_9236_().m_7106_((ParticleOptions)options, d0 - vec3.f_82479_ * 0.5 + this.f_19796_.m_188500_() * 0.6 - 0.3, d1 - vec3.f_82480_ * 0.5, d2 - vec3.f_82481_ * 0.5 + this.f_19796_.m_188500_() * 0.6 - 0.3, vec3.f_82479_ * 0.25, vec3.f_82480_ * 0.25, vec3.f_82481_ * 0.25);
        }
    }
}

