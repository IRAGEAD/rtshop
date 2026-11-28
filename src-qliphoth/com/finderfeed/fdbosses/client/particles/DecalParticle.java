/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.client.particles.options.AlphaOptions
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.client.particles;

import com.finderfeed.fdbosses.client.particles.DecalParticleOptions;
import com.finderfeed.fdlib.util.client.particles.options.AlphaOptions;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public abstract class DecalParticle
extends Particle {
    private DecalParticleOptions decalParticleOptions;
    private ComplexEasingFunction easingFunction;

    public DecalParticle(DecalParticleOptions decalParticleOptions, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.decalParticleOptions = decalParticleOptions;
        AlphaOptions alphaOptions = decalParticleOptions.getAlphaOptions();
        this.f_107225_ = alphaOptions.fullTime();
        this.easingFunction = ComplexEasingFunction.builder().addArea((float)alphaOptions.inTime, FDEasings::linear).addArea((float)alphaOptions.stayTime, FDEasings::one).addArea((float)alphaOptions.outTime, FDEasings::reversedLinear).build();
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float pticks) {
        Vec3 camPos = camera.m_90583_();
        Vec3 thisPos = FDMathUtil.interpolateVectors((Vec3)new Vec3(this.f_107209_, this.f_107210_, this.f_107211_), (Vec3)new Vec3(this.f_107212_, this.f_107213_, this.f_107214_), (float)pticks);
        Vec3 translation = thisPos.m_82546_(camPos);
        Matrix4f mat = new Matrix4f();
        mat.translate((float)translation.f_82479_, (float)translation.f_82480_, (float)translation.f_82481_);
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)this.decalParticleOptions.getDirection());
        float roll = FDMathUtil.lerp((float)this.f_107204_, (float)this.f_107231_, (float)pticks);
        mat.rotateY((float)Math.toRadians(roll));
        float alpha = this.easingFunction.apply((float)this.f_107224_ + pticks);
        float size = this.decalParticleOptions.getSize();
        int light = this.m_6355_(pticks);
        vertex.m_252986_(mat, size, 0.0f, size).m_7421_(0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        vertex.m_252986_(mat, size, 0.0f, -size).m_7421_(0.0f, 1.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        vertex.m_252986_(mat, -size, 0.0f, -size).m_7421_(1.0f, 1.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        vertex.m_252986_(mat, -size, 0.0f, size).m_7421_(1.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
    }

    public DecalParticleOptions getDecalParticleOptions() {
        return this.decalParticleOptions;
    }
}

