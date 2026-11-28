/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.shapes.FD2DShape
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.renderers.ShapeOnCurveRenderer
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.client.particles.stripe_particle;

import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdlib.systems.shapes.FD2DShape;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.renderers.ShapeOnCurveRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class StripeParticle
extends Particle {
    private StripeParticleOptions stripeParticleOptions;
    private List<Vector3f> points;
    private FD2DShape shape;
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(){

        public void m_6294_(Tesselator tesselator) {
            tesselator.m_85914_();
            RenderSystem.setShader(GameRenderer::m_172829_);
            RenderSystem.defaultBlendFunc();
        }

        @Nullable
        public void m_6505_(BufferBuilder tesselator, TextureManager textureManager) {
            if (Minecraft.m_91085_()) {
                Minecraft.m_91087_().f_91060_.m_109830_().m_83947_(false);
            } else {
                Minecraft.m_91087_().m_91385_().m_83947_(false);
            }
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::m_172753_);
            RenderSystem.blendFunc((int)770, (int)1);
            RenderSystem.enableCull();
            tesselator.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        }
    };

    public StripeParticle(StripeParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.stripeParticleOptions = options;
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.f_107225_ = options.getLifetime();
        this.shape = FD2DShape.createSimpleCircleNVertexShape((float)options.getScale(), (int)options.getShapeVertices());
        this.points = this.stripeParticleOptions.getOffsets().stream().map(v -> new Vector3f((float)v.f_82479_ + 0.001f, (float)v.f_82480_ + 0.001f, (float)v.f_82481_ + 0.001f)).toList();
    }

    public boolean shouldCull() {
        return false;
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float pticks) {
        float lifetimeP = ((float)this.f_107224_ + pticks) / (float)this.f_107225_;
        float startP = FDMathUtil.lerp((float)(-this.stripeParticleOptions.getStripePercentLength()), (float)1.0f, (float)lifetimeP);
        float endP = startP + this.stripeParticleOptions.getStripePercentLength();
        Vec3 pos = FDMathUtil.interpolateVectors((Vec3)new Vec3(this.f_107209_, this.f_107210_, this.f_107211_), (Vec3)new Vec3(this.f_107212_, this.f_107213_, this.f_107214_), (float)pticks).m_82546_(camera.m_90583_());
        PoseStack matrix = new PoseStack();
        matrix.m_85836_();
        matrix.m_85837_(pos.f_82479_, pos.f_82480_, pos.f_82481_);
        ShapeOnCurveRenderer.start((VertexConsumer)vertex).scalingFunction(v -> {
            float distToEnd = endP - v.floatValue();
            float distToStart = v.floatValue() - startP;
            if (distToStart < 0.0f) {
                distToStart = 0.0f;
            }
            float p = distToEnd > distToStart ? distToStart / (this.stripeParticleOptions.getStripePercentLength() / 2.0f) : distToEnd / (this.stripeParticleOptions.getStripePercentLength() / 2.0f);
            float startp = FDMathUtil.clamp((float)(v.floatValue() / this.stripeParticleOptions.getStartInPercent()), (float)0.0f, (float)1.0f);
            float endp = FDMathUtil.clamp((float)((1.0f - v.floatValue()) / this.stripeParticleOptions.getEndOutPercent()), (float)0.0f, (float)1.0f);
            return Float.valueOf(FDMathUtil.clamp((float)FDEasings.easeOut((float)p), (float)0.0f, (float)1.0f) * startp * endp);
        }).renderCount(2).startPercent(startP).endPercent(endP).pose(matrix).shape(this.shape).startColor(this.stripeParticleOptions.getStartColor()).endColor(this.stripeParticleOptions.getEndColor()).curvePositions(this.points).lod(FDMathUtil.clamp((int)this.stripeParticleOptions.getLOD(), (int)0, (int)200)).render();
        matrix.m_85849_();
    }

    protected int m_6355_(float p_107249_) {
        return 0xF000F0;
    }

    public ParticleRenderType m_7556_() {
        return RENDER_TYPE;
    }

    public static class Factory
    implements ParticleProvider<StripeParticleOptions> {
        @Nullable
        public Particle createParticle(StripeParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new StripeParticle(options, level, x, y, z, xd, yd, zd);
        }
    }
}

