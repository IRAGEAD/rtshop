/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
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
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.client.particles.malkuth_slash;

import com.finderfeed.fdbosses.client.particles.malkuth_slash.MalkuthHorizontalSlashOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_slash.MalkuthSlashRenderer;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MalkuthHorizontalSlashParticle
extends Particle {
    private MalkuthHorizontalSlashOptions options;
    public static final ParticleRenderType ICE_RENDER_TYPE = new ParticleRenderType(){

        public void m_6294_(Tesselator tesselator) {
            tesselator.m_85914_();
        }

        @Nullable
        public void m_6505_(BufferBuilder tesselator, TextureManager textureManager) {
            RenderSystem.depthMask((boolean)true);
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)MalkuthSlashRenderer.ICE_LOCATION);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::m_172829_);
            tesselator.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85813_);
        }
    };
    public static final ParticleRenderType FIRE_RENDER_TYPE = new ParticleRenderType(){

        public void m_6294_(Tesselator tesselator) {
            tesselator.m_85914_();
        }

        @Nullable
        public void m_6505_(BufferBuilder tesselator, TextureManager textureManager) {
            RenderSystem.depthMask((boolean)true);
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)MalkuthSlashRenderer.FIRE_LOCATION);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::m_172829_);
            tesselator.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85813_);
        }
    };

    public MalkuthHorizontalSlashParticle(MalkuthHorizontalSlashOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.options = options;
        this.f_107225_ = options.getLifetime();
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float pticks) {
        float p = Mth.m_14036_((float)(((float)this.f_107224_ + pticks) / (float)(this.f_107225_ - 1)), (float)0.0f, (float)1.0f);
        Vec3 thisPos = new Vec3(FDMathUtil.lerp((double)this.f_107209_, (double)this.f_107212_, (double)pticks), FDMathUtil.lerp((double)this.f_107210_, (double)this.f_107213_, (double)pticks), FDMathUtil.lerp((double)this.f_107211_, (double)this.f_107214_, (double)pticks));
        Vec3 camPos = camera.m_90583_();
        Vec3 actualPos = thisPos.m_82546_(camPos);
        QuadRenderer.start((VertexConsumer)vertex).translate((float)actualPos.f_82479_, (float)actualPos.f_82480_, (float)actualPos.f_82481_).sizeY(this.options.getSlashWidth() * 0.3461538f).sizeX(this.options.getSlashWidth()).direction(this.options.getSlashDirection().m_82548_()).verticalRendering().renderBack().setTexColor().rotationDegrees(this.options.getRotation()).color(1.0f, 1.0f, 1.0f, 0.8f - p * 0.8f).render();
    }

    public ParticleRenderType m_7556_() {
        switch (this.options.getAttackType()) {
            case ICE: {
                return ICE_RENDER_TYPE;
            }
            case FIRE: {
                return FIRE_RENDER_TYPE;
            }
        }
        return FIRE_RENDER_TYPE;
    }

    protected int m_6355_(float p_107249_) {
        return 0xF000F0;
    }

    public static class Factory
    implements ParticleProvider<MalkuthHorizontalSlashOptions> {
        @Nullable
        public Particle createParticle(MalkuthHorizontalSlashOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new MalkuthHorizontalSlashParticle(options, level, x, y, z, xd, yd, zd);
        }
    }
}

