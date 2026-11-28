/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile.JudgementBallExplosionParticleOptions;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class JudgementBallExplosionParticle
extends Particle {
    private static FDModel model;
    private JudgementBallExplosionParticleOptions options;
    public static final ParticleRenderType RENDER_TYPE;

    public JudgementBallExplosionParticle(ClientLevel p_107239_, double x, double y, double z, double xd, double yd, double zd, JudgementBallExplosionParticleOptions options) {
        super(p_107239_, x, y, z, xd, yd, zd);
        if (model == null) {
            model = new FDModel(BossModels.JUDGEMENT_BALL.get());
        }
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.f_107225_ = Math.abs(options.lifetime);
        this.options = options;
    }

    public void m_5744_(VertexConsumer v, Camera camera, float pticks) {
        Vec3 ppos = FDMathUtil.interpolateVectors((Vec3)new Vec3(this.f_107209_, this.f_107210_, this.f_107211_), (Vec3)new Vec3(this.f_107212_, this.f_107213_, this.f_107214_), (float)pticks);
        Vec3 realPos = ppos.m_82546_(camera.m_90583_());
        PoseStack poseStack = new PoseStack();
        poseStack.m_85837_(realPos.f_82479_, realPos.f_82480_, realPos.f_82481_);
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)poseStack, (Vec3)this.options.movement);
        boolean inverted = this.options.lifetime < 0;
        float time = ((float)this.f_107224_ + pticks) / (float)this.f_107225_;
        float scale = (inverted ? 1.0f - time : FDEasings.easeOut((float)time)) * this.options.size;
        if (!inverted) {
            float rotation = (float)this.f_107224_ + pticks;
            poseStack.m_252781_(Axis.f_252403_.m_252977_(rotation * 20.0f));
        }
        poseStack.m_85841_(scale, scale, scale);
        float alpha = 1.0f - FDEasings.easeOut((float)time);
        model.render(poseStack, v, 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, alpha * 0.9f);
    }

    public ParticleRenderType m_7556_() {
        return RENDER_TYPE;
    }

    static {
        RENDER_TYPE = new ParticleRenderType(){

            public void m_6505_(BufferBuilder buffer, TextureManager p_107437_) {
                RenderSystem.depthMask((boolean)true);
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture((int)0, (ResourceLocation)FDBosses.location("textures/entities/geburah/judgement_ball_3.png"));
                RenderSystem.setShader(() -> GameRenderer.m_172835_());
                buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85820_);
            }

            public void m_6294_(Tesselator p_107438_) {
                p_107438_.m_85914_();
                RenderSystem.setShader(() -> GameRenderer.m_172829_());
            }

            public String toString() {
                return "GEBURAH_JUDGEMENT_BALL";
            }
        };
    }

    public static class Factory
    implements ParticleProvider<JudgementBallExplosionParticleOptions> {
        @Nullable
        public Particle createParticle(JudgementBallExplosionParticleOptions p_107421_, ClientLevel p_107422_, double x, double y, double z, double xd, double yd, double zd) {
            JudgementBallExplosionParticle particle = new JudgementBallExplosionParticle(p_107422_, x, y, z, xd, yd, zd, p_107421_);
            return particle;
        }
    }
}

