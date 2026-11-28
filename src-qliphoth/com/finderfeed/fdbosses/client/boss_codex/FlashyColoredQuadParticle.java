/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDScreenParticle
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Axis
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.util.Mth
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.client.boss_codex;

import com.finderfeed.fdlib.systems.screen.screen_particles.FDScreenParticle;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class FlashyColoredQuadParticle
extends FDScreenParticle<FlashyColoredQuadParticle> {
    private float flashFrequency;
    private float flashOffset;
    private float quadSize;
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(){

        public void m_6505_(BufferBuilder bufferBuilder, TextureManager p_107437_) {
            bufferBuilder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc((int)770, (int)1);
            RenderSystem.setShader(GameRenderer::m_172811_);
        }

        public void m_6294_(Tesselator p_107438_) {
            p_107438_.m_85914_();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    };

    public void render(GuiGraphics graphics, BufferBuilder bufferBuilder, float partialTicks) {
        PoseStack matrices = graphics.m_280168_();
        float x = (float)this.getX(partialTicks);
        float y = (float)this.getY(partialTicks);
        float roll = this.getRoll(partialTicks);
        float halfQuadSize = this.quadSize;
        matrices.m_85836_();
        matrices.m_252880_(x, y, 0.0f);
        matrices.m_252781_(Axis.f_252393_.m_252977_(roll));
        Matrix4f m = matrices.m_85850_().m_252922_();
        float a = this.getA();
        bufferBuilder.m_252986_(m, -halfQuadSize, -halfQuadSize, 0.0f).m_85950_(this.getR(), this.getG(), this.getB(), a).m_5752_();
        bufferBuilder.m_252986_(m, -halfQuadSize, halfQuadSize, 0.0f).m_85950_(this.getR(), this.getG(), this.getB(), a).m_5752_();
        bufferBuilder.m_252986_(m, halfQuadSize, halfQuadSize, 0.0f).m_85950_(this.getR(), this.getG(), this.getB(), a).m_5752_();
        bufferBuilder.m_252986_(m, halfQuadSize, -halfQuadSize, 0.0f).m_85950_(this.getR(), this.getG(), this.getB(), a).m_5752_();
        matrices.m_85849_();
    }

    public ParticleRenderType getParticleRenderType() {
        return RENDER_TYPE;
    }

    public FlashyColoredQuadParticle setFlashFrequency(float flashFrequency) {
        this.flashFrequency = flashFrequency;
        return this;
    }

    public FlashyColoredQuadParticle setFlashOffset(float flashOffset) {
        this.flashOffset = flashOffset;
        return this;
    }

    public FlashyColoredQuadParticle setQuadSize(float quadSize) {
        this.quadSize = quadSize;
        return this;
    }

    public float getA() {
        float pticks = FDRenderUtil.tryGetPartialTickIgnorePause();
        float time = (float)this.getAge() + pticks;
        float p = Mth.m_14036_((float)(time / (float)this.getLifetime()), (float)0.0f, (float)1.0f);
        float flash = (float)(Math.sin(this.flashFrequency * (this.flashOffset + time)) * 0.5 + 0.5) * 0.5f + 0.5f;
        return FDMathUtil.lerp((float)0.0f, (float)super.getA(), (float)(1.0f - p)) * flash;
    }
}

