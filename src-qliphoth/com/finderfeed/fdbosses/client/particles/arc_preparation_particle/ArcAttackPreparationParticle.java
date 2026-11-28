/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
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
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.client.particles.arc_preparation_particle;

import com.finderfeed.fdbosses.client.particles.arc_preparation_particle.ArcAttackPreparationParticleOptions;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ArcAttackPreparationParticle
extends Particle {
    private ArcAttackPreparationParticleOptions options;
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(){

        public void m_6294_(Tesselator tesselator) {
            tesselator.m_85914_();
            RenderSystem.setShader(GameRenderer::m_172829_);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
        }

        @Nullable
        public void m_6505_(BufferBuilder tesselator, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::m_172811_);
            RenderSystem.blendFunc((int)770, (int)771);
            RenderSystem.disableCull();
            tesselator.m_166779_(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.f_85815_);
        }
    };

    public ArcAttackPreparationParticle(ArcAttackPreparationParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.options = options;
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.f_107225_ = options.getAttackChargeTime() + options.getFadeOut();
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float pticks) {
        Vec3 thisPos = FDMathUtil.interpolateVectors((Vec3)new Vec3(this.f_107209_, this.f_107210_, this.f_107211_), (Vec3)new Vec3(this.f_107212_, this.f_107213_, this.f_107214_), (float)pticks);
        Vec3 camPos = camera.m_90583_();
        Vec3 pos = thisPos.m_82546_(camPos);
        Vec3 hdir = this.options.getHorizontalDirection().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_((double)this.options.getLength(), (double)this.options.getLength(), (double)this.options.getLength());
        float distanceModifier = FDEasings.easeOut((float)FDMathUtil.clamp((float)(((float)this.f_107224_ + pticks) / (float)this.options.getFadeIn()), (float)0.0f, (float)1.0f));
        float attackPreparationModifier = FDMathUtil.clamp((float)(((float)this.f_107224_ + pticks) / (float)this.options.getAttackChargeTime()), (float)0.0f, (float)1.0f);
        Vec3 toPos2 = hdir.m_82524_(this.options.getHalfAttackAngle());
        Vec3 toPos3 = hdir.m_82524_(-this.options.getHalfAttackAngle());
        Vec3 toPos2p = hdir.m_82524_(this.options.getHalfAttackAngle());
        Vec3 toPos3p = hdir.m_82524_(-this.options.getHalfAttackAngle());
        Vec3 pos2 = pos.m_82549_(toPos2.m_82542_((double)distanceModifier, (double)distanceModifier, (double)distanceModifier));
        Vec3 pos3 = pos.m_82549_(toPos3.m_82542_((double)distanceModifier, (double)distanceModifier, (double)distanceModifier));
        Vec3 pos2p = pos.m_82549_(toPos2p.m_82542_((double)attackPreparationModifier, (double)attackPreparationModifier, (double)attackPreparationModifier));
        Vec3 pos3p = pos.m_82549_(toPos3p.m_82542_((double)attackPreparationModifier, (double)attackPreparationModifier, (double)attackPreparationModifier));
        Vec3 center = pos.m_82549_(hdir.m_82542_((double)distanceModifier, (double)distanceModifier, (double)distanceModifier));
        Vec3 centerp = pos.m_82549_(hdir.m_82542_((double)attackPreparationModifier, (double)attackPreparationModifier, (double)attackPreparationModifier));
        FDColor color = this.options.getColor();
        float fadeOutP = 1.0f;
        if (this.f_107224_ - this.options.getAttackChargeTime() >= 0) {
            fadeOutP = 1.0f - FDMathUtil.clamp((float)(((float)this.f_107224_ + pticks - (float)this.options.getAttackChargeTime()) / (float)this.options.getFadeOut()), (float)0.0f, (float)1.0f);
        }
        float colmod = 0.75f;
        vertex.m_5483_((double)((float)pos.f_82479_), (double)((float)pos.f_82480_), (double)((float)pos.f_82481_)).m_85950_(color.r * colmod, color.g * colmod, color.b * colmod, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)pos2.f_82479_), (double)((float)pos2.f_82480_), (double)((float)pos2.f_82481_)).m_85950_(color.r * colmod, color.g * colmod, color.b * colmod, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)center.f_82479_), (double)((float)center.f_82480_), (double)((float)center.f_82481_)).m_85950_(color.r * colmod, color.g * colmod, color.b * colmod, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)pos.f_82479_), (double)((float)pos.f_82480_), (double)((float)pos.f_82481_)).m_85950_(color.r * colmod, color.g * colmod, color.b * colmod, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)pos3.f_82479_), (double)((float)pos3.f_82480_), (double)((float)pos3.f_82481_)).m_85950_(color.r * colmod, color.g * colmod, color.b * colmod, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)center.f_82479_), (double)((float)center.f_82480_), (double)((float)center.f_82481_)).m_85950_(color.r * colmod, color.g * colmod, color.b * colmod, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)pos.f_82479_), (double)((float)pos.f_82480_ + 0.005f), (double)((float)pos.f_82481_)).m_85950_(color.r, color.g, color.b, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)pos2p.f_82479_), (double)((float)pos.f_82480_ + 0.005f), (double)((float)pos2p.f_82481_)).m_85950_(color.r, color.g, color.b, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)centerp.f_82479_), (double)((float)pos.f_82480_ + 0.005f), (double)((float)centerp.f_82481_)).m_85950_(color.r, color.g, color.b, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)pos.f_82479_), (double)((float)pos.f_82480_ + 0.005f), (double)((float)pos.f_82481_)).m_85950_(color.r, color.g, color.b, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)pos3p.f_82479_), (double)((float)pos.f_82480_ + 0.005f), (double)((float)pos3p.f_82481_)).m_85950_(color.r, color.g, color.b, color.a * fadeOutP).m_5752_();
        vertex.m_5483_((double)((float)centerp.f_82479_), (double)((float)pos.f_82480_ + 0.005f), (double)((float)centerp.f_82481_)).m_85950_(color.r, color.g, color.b, color.a * fadeOutP).m_5752_();
    }

    protected int m_6355_(float p_107249_) {
        return 0xF000F0;
    }

    public ParticleRenderType m_7556_() {
        return RENDER_TYPE;
    }

    public boolean shouldCull() {
        return false;
    }

    public static class Factory
    implements ParticleProvider<ArcAttackPreparationParticleOptions> {
        @Nullable
        public Particle createParticle(ArcAttackPreparationParticleOptions options, ClientLevel level, double px, double py, double pz, double xd, double yd, double zd) {
            return new ArcAttackPreparationParticle(options, level, px, py, pz, xd, yd, zd);
        }
    }
}

