/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.TextureSheetParticle
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.client.particles;

import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.Vec3;

public abstract class GravityOptionsParticle
extends TextureSheetParticle {
    private GravityParticleOptions options;
    private float particleRollO = 0.0f;
    private float particleRoll = 0.0f;
    private float particleQuadSize;

    public GravityOptionsParticle(GravityParticleOptions gravityParticleOptions, ClientLevel leve, double x, double y, double z, double xd, double yd, double zd) {
        super(leve, x, y, z, xd, yd, zd);
        this.options = gravityParticleOptions;
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.particleRoll = this.f_107231_ = leve.f_46441_.m_188501_() * (float)Math.PI * 2.0f;
        this.particleRollO = this.f_107231_;
        this.f_107226_ = gravityParticleOptions.getGravity();
        this.f_107663_ = this.options.getQuadSize();
        this.particleQuadSize = gravityParticleOptions.getQuadSize();
        this.f_107225_ = this.options.getLifetime();
        this.f_107219_ = true;
    }

    public void m_5989_() {
        super.m_5989_();
        Vec3 v = new Vec3(this.f_107209_ - this.f_107212_, this.f_107210_ - this.f_107213_, this.f_107211_ - this.f_107214_);
        float length = (float)v.m_82553_();
        this.particleRollO = this.particleRoll;
        this.particleRoll += length * this.options.getRotationModifier();
    }

    public void m_5744_(VertexConsumer consumer, Camera camera, float pticks) {
        this.f_107231_ = this.particleRoll;
        this.f_107204_ = this.particleRollO;
        if (this.options.isFadeOut()) {
            this.f_107663_ = this.particleQuadSize * (1.0f - ((float)this.f_107224_ + pticks) / (float)this.f_107225_);
        }
        super.m_5744_(consumer, camera, pticks);
    }
}

