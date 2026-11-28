/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.init.FDCoreShaders
 *  com.finderfeed.fdlib.systems.hud.bossbars.FDBossBarInterpolated
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$ParticleRenderTypesS
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$Scissor
 *  com.finderfeed.fdlib.util.rendering.FDShaderRenderer
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss;

import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningParticle;
import com.finderfeed.fdlib.init.FDCoreShaders;
import com.finderfeed.fdlib.systems.hud.bossbars.FDBossBarInterpolated;
import com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.finderfeed.fdlib.util.rendering.FDShaderRenderer;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ChesedBossBar
extends FDBossBarInterpolated {
    public static final int HIT_EVENT = 0;
    public static final int MAX_HIT_TIME = 20;
    private static final Random r = new Random();
    public static final ResourceLocation TEXTURE = ResourceLocation.m_214293_((String)"fdbosses", (String)"textures/boss_bars/chesed_bossbar.png");
    private int previousHitStrength = 0;
    private int time = 0;
    private int hitTime = 0;
    private int hitTimeO = 0;

    public ChesedBossBar(UUID uuid, int entityId) {
        super(uuid, entityId, 10);
    }

    public void renderInterpolatedBossBar(GuiGraphics graphics, float partialTicks, float interpolatedPercentage) {
        PoseStack matrices = graphics.m_280168_();
        matrices.m_85836_();
        if (this.hitTime != 0) {
            float str = 1.0f + Math.min((float)this.previousHitStrength / 5.0f, 1.5f);
            float t = FDMathUtil.lerp((float)this.hitTimeO, (float)this.hitTime, (float)partialTicks) / 20.0f;
            t *= t;
            long rndOffset = 2343L;
            Random shakeRnd = new Random((long)(this.time + 1) * rndOffset);
            float tx = shakeRnd.nextFloat() * 2.5f - 1.25f;
            float ty = shakeRnd.nextFloat() * 2.5f - 1.25f;
            matrices.m_252880_(tx * (str *= t), ty * str, 0.0f);
        }
        FDRenderUtil.bindTexture((ResourceLocation)TEXTURE);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)-127.0f, (float)0.0f, (float)254.0f, (float)54.0f, (float)0.0f, (float)0.0f, (float)254.0f, (float)54.0f, (float)254.0f, (float)59.0f, (float)0.0f, (float)1.0f);
        float hpPosX = -96.0f;
        float hpPosY = 26.0f;
        float hpW = 192.0f;
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)hpPosX, (float)hpPosY, (float)(hpW * interpolatedPercentage), (float)5.0f, (float)0.0f, (float)54.0f, (float)(hpW * interpolatedPercentage), (float)5.0f, (float)254.0f, (float)59.0f, (float)0.0f, (float)1.0f);
        float shaderTime = ((float)this.time + partialTicks) / 4000.0f;
        FDRenderUtil.Scissor.pushScissors((PoseStack)matrices, (float)hpPosX, (float)hpPosY, (float)(hpW * interpolatedPercentage), (float)5.0f);
        FDShaderRenderer.start((GuiGraphics)graphics, (ShaderInstance)FDCoreShaders.NOISE).position(hpPosX, hpPosY, 0.0f).setResolution(hpW, 5.0f).setUVSpan(0.5f, 1.0f).setUpColor(0.1f, 0.8f, 0.8f, 0.0f).setDownColor(0.5f, 1.0f, 1.0f, 0.8f).setShaderUniform("size", new float[]{hpW, 5.0f}).setShaderUniform("xyOffset", new float[]{0.0f, shaderTime}).setShaderUniform("sections", new float[]{100.0f}).setShaderUniform("octaves", new float[]{4.0f}).setShaderUniform("time", new float[]{shaderTime}).end();
        FDRenderUtil.Scissor.popScissors();
        this.renderLightning(matrices, hpPosX, hpPosY, hpW, interpolatedPercentage, partialTicks);
        matrices.m_85849_();
    }

    private void renderLightning(PoseStack matrices, float hpPosX, float hpPosY, float hpW, float interpolatedPercentage, float partialTicks) {
        if (this.hitTime != 0) {
            float t = FDMathUtil.lerp((float)this.hitTimeO, (float)this.hitTime, (float)partialTicks) / 20.0f;
            t *= t;
            List<Vec3> path = List.of(new Vec3((double)(hpPosX - 2.0f), (double)(hpPosY + 2.5f), 0.0), new Vec3((double)(hpPosX + (hpW *= interpolatedPercentage) + 2.0f), (double)(hpPosY + 2.5f), 0.0));
            List<Vec3> path2 = List.of(new Vec3((double)(hpPosX - 2.0f), (double)(hpPosY + 2.5f), 0.0), new Vec3((double)(hpPosX + hpW + 2.0f), (double)(hpPosY + 2.5f), 0.0));
            float lw = 1.75f;
            float lspread = 4.0f;
            int lightningBreaks = Math.max(2, Math.round(interpolatedPercentage * 20.0f));
            ArcLightningParticle.fullLightningImmediateDraw(this.time, 32423543, lightningBreaks, matrices.m_85850_().m_252922_(), path, lw, lspread, 0.2f, 1.0f, 1.0f, t);
            ArcLightningParticle.fullLightningImmediateDraw(this.time, 32423543, lightningBreaks, matrices.m_85850_().m_252922_(), path, lw / 2.0f, lspread, 0.7f, 1.0f, 1.0f, t);
            ArcLightningParticle.fullLightningImmediateDraw(this.time, 5353543, lightningBreaks, matrices.m_85850_().m_252922_(), path2, lw, lspread, 0.2f, 1.0f, 1.0f, t);
            ArcLightningParticle.fullLightningImmediateDraw(this.time, 5353543, lightningBreaks, matrices.m_85850_().m_252922_(), path2, lw / 2.0f, lspread, 0.7f, 1.0f, 1.0f, t);
        }
    }

    public void tick(float topOffset) {
        super.tick(topOffset);
        ++this.time;
        this.hitTimeO = this.hitTime;
        this.hitTime = Mth.m_14045_((int)(this.hitTime - 1), (int)0, (int)20);
        Window window = Minecraft.m_91087_().m_91268_();
        float w = window.m_85445_();
        float h = window.m_85446_();
        if (this.time % 4 == 0) {
            float randomOffset = r.nextFloat() * 200.0f - 100.0f;
            float randomOffset2 = r.nextFloat() * 200.0f - 100.0f;
            float x = w / 2.0f + randomOffset;
            float x2 = w / 2.0f + randomOffset2;
            float y = 28.0f;
            ((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)FDTexturedSParticle.create((Function)FDRenderUtil.ParticleRenderTypesS.TEXTURES_BLUR_ADDITIVE, (ResourceLocation)BallParticle.LOCATION).setPos((double)x, (double)y, true)).setMaxQuadSize(3.5f).setSpeed(0.0, -0.1)).setFriction((double)0.99f)).setColor(0.1f + r.nextFloat() * 0.1f - 0.05f, 0.8f + r.nextFloat() * 0.1f - 0.05f, 0.8f + r.nextFloat() * 0.1f - 0.05f, 0.8f)).setLifetime(30)).setDefaultScaleInOut().sendToOverlay();
            ((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)FDTexturedSParticle.create((Function)FDRenderUtil.ParticleRenderTypesS.TEXTURES_BLUR_ADDITIVE, (ResourceLocation)BallParticle.LOCATION).setPos((double)x2, (double)(y + 7.0f), true)).setMaxQuadSize(3.5f).setSpeed(0.0, 0.1)).setFriction((double)0.99f)).setColor(0.1f + r.nextFloat() * 0.1f - 0.05f, 0.8f + r.nextFloat() * 0.1f - 0.05f, 0.8f + r.nextFloat() * 0.1f - 0.05f, 0.8f)).setLifetime(30)).setDefaultScaleInOut().sendToOverlay();
        }
    }

    public float height() {
        return 50.0f;
    }

    public void hanldeBarEvent(int eventId, int data) {
        if (eventId == 0) {
            this.previousHitStrength = data;
            this.hitTime = 20;
            this.hitTimeO = 20;
        }
    }
}

