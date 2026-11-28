/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.BooleanOp
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.kinetic_field;

import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChesedKineticFieldEntity
extends FDEntity {
    public static final int KINETIC_FIELD_SQUARE_RADIUS = 5;

    public ChesedKineticFieldEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.getAnimationSystem().startAnimation("SPAWN", AnimationTicker.builder(BossAnims.CHESED_KINETIC_FIELD_SPAWN).build());
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.m_9236_().f_46443_ && this.f_19797_ == 4) {
            double radius = this.getSquareRadius();
            this.spawnSpearParticles(-radius, -radius);
            this.spawnSpearParticles(-radius, radius);
            this.spawnSpearParticles(radius, radius);
            this.spawnSpearParticles(radius, -radius);
        }
        this.getAnimationSystem().setVariable("variable.radius", (float)this.getSquareRadius() * 16.0f);
    }

    public void m_142687_(Entity.RemovalReason p_146834_) {
        super.m_142687_(p_146834_);
        if (this.m_9236_().f_46443_) {
            double radius = this.getSquareRadius();
            this.spawnSpearParticles(-radius, -radius);
            this.spawnSpearParticles(-radius, radius);
            this.spawnSpearParticles(radius, radius);
            this.spawnSpearParticles(radius, -radius);
        }
    }

    public boolean m_6087_() {
        return true;
    }

    private void spawnSpearParticles(double offsetX, double offsetZ) {
        Vec3 pos = this.m_20182_();
        Vec3 spear = pos.m_82520_(offsetX, 0.0, offsetZ);
        Vec3 between = spear.m_82546_(pos).m_82541_();
        int height = 5;
        for (float i = 0.0f; i < (float)height; i += 0.25f) {
            float p = i / ((float)height - 1.0f);
            Vec3 add = between.m_82542_((double)p, (double)p, (double)p);
            BallParticleOptions options = BallParticleOptions.builder().friction(0.8f).size(0.5f + this.f_19796_.m_188501_() * 0.2f).scalingOptions(2, 10, 10).color(100 + this.f_19796_.m_188503_(50), 255, 255).build();
            Vec3 v = spear.m_82520_(add.f_82479_, (double)i, add.f_82481_);
            Vec3 speed = new Vec3(0.25 * (double)this.f_19796_.m_188501_() + 0.25, 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
            this.m_9236_().m_7106_((ParticleOptions)options, v.f_82479_, v.f_82480_, v.f_82481_, speed.f_82479_, 0.0, speed.f_82481_);
        }
    }

    public VoxelShape getCollisionShape(Entity player) {
        double rad = this.getSquareRadius();
        Vec3 pos = this.m_20182_();
        AABB box = new AABB(pos.f_82479_ - rad, pos.f_82480_ - 0.1, pos.f_82481_ - rad, pos.f_82479_ + rad, pos.f_82480_ + 3.0, pos.f_82481_ + rad).m_82400_(0.025);
        if (box.m_82390_(player.m_20182_()) || box.m_82390_(player.m_20182_().m_82520_(0.0, (double)player.m_20206_(), 0.0))) {
            return Shapes.m_83113_((VoxelShape)Shapes.f_83036_, (VoxelShape)Shapes.m_83064_((AABB)box), (BooleanOp)BooleanOp.f_82685_);
        }
        return Shapes.m_83040_();
    }

    public double getSquareRadius() {
        return 5.0;
    }

    protected void m_8097_() {
    }

    protected void m_7378_(CompoundTag p_20052_) {
    }

    protected void m_7380_(CompoundTag p_20139_) {
    }
}

