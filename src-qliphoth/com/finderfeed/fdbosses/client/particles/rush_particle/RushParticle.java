/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Vector4f
 */
package com.finderfeed.fdbosses.client.particles.rush_particle;

import com.finderfeed.fdbosses.client.particles.rush_particle.RushParticleOptions;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class RushParticle
extends Particle {
    private RushParticleOptions rushParticleOptions;
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(){

        public void m_6294_(Tesselator tesselator) {
            tesselator.m_85914_();
            RenderSystem.setShader(GameRenderer::m_172829_);
        }

        @Nullable
        public void m_6505_(BufferBuilder tesselator, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.setShader(GameRenderer::m_172811_);
            tesselator.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        }
    };

    public RushParticle(RushParticleOptions rushParticleOptions, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.rushParticleOptions = rushParticleOptions;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107225_ = rushParticleOptions.getLifetime();
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float pticks) {
        Vec3 pos = new Vec3(Mth.m_14139_((double)pticks, (double)this.f_107209_, (double)this.f_107212_), Mth.m_14139_((double)pticks, (double)this.f_107210_, (double)this.f_107213_), Mth.m_14139_((double)pticks, (double)this.f_107211_, (double)this.f_107214_)).m_82546_(camera.m_90583_());
        float p = ((float)this.f_107224_ + pticks) / (float)this.f_107225_;
        Matrix4f mat = new Matrix4f();
        mat.translate((float)pos.f_82479_, (float)pos.f_82480_, (float)pos.f_82481_);
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)this.rushParticleOptions.getRushDirection());
        Vec3 n = FDMathUtil.getNormalVectorFromLineToPoint((Vec3)pos, (Vec3)this.rushParticleOptions.getRushDirection().m_82549_(pos), (Vec3)Vec3.f_82478_);
        Matrix4f mt2 = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mt2, (Vec3)this.rushParticleOptions.getRushDirection());
        Vector4f up = new Vector4f(0.0f, 0.0f, 1.0f, 1.0f);
        mt2.transform(up);
        Vector4f left = new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
        mt2.transform(left);
        Vec3 vup = new Vec3((double)(up.x / up.w), (double)(up.y / up.w), (double)(up.z / up.w));
        Vec3 vleft = new Vec3((double)left.x, (double)left.y, (double)left.z);
        float angle = (float)FDMathUtil.angleBetweenVectors((Vec3)n, (Vec3)vup);
        if (vleft.m_82526_(n) > 0.0) {
            mat.rotateY(angle);
        } else {
            mat.rotateY(-angle);
        }
        float w = this.rushParticleOptions.getWidth() / 2.0f;
        float l = this.rushParticleOptions.getLength();
        FDColor color = this.rushParticleOptions.getColor();
        float addl = FDEasings.easeIn((float)p) * l;
        if (addl >= l) {
            return;
        }
        vertex.m_252986_(mat, -w / 2.0f, addl, 0.0f).m_85950_(color.r, color.g, color.b, color.a).m_5752_();
        vertex.m_252986_(mat, w / 2.0f, addl, 0.0f).m_85950_(color.r, color.g, color.b, color.a).m_5752_();
        vertex.m_252986_(mat, w / 2.0f, l, 0.0f).m_85950_(color.r, color.g, color.b, color.a).m_5752_();
        vertex.m_252986_(mat, -w / 2.0f, l, 0.0f).m_85950_(color.r, color.g, color.b, color.a).m_5752_();
    }

    public ParticleRenderType m_7556_() {
        return RENDER_TYPE;
    }

    protected int m_6355_(float p_107249_) {
        return 0xF000F0;
    }

    public static class Factory
    implements ParticleProvider<RushParticleOptions> {
        @Nullable
        public Particle createParticle(RushParticleOptions rushParticleOptions, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new RushParticle(rushParticleOptions, level, x, y, z, xd, yd, zd);
        }
    }
}

