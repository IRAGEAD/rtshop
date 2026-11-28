/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.hud.bossbars.FDBossBar
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$ParticleRenderTypesS
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdlib.systems.hud.bossbars.FDBossBar;
import com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class GeburahBossBar
extends FDBossBar {
    public static final int HIT_EVENT = 0;
    public static final int MAX_HIT_TIME = 20;
    public static final ResourceLocation GEBURAH_BOSSBAR = FDBosses.location("textures/boss_bars/geburah_bossbar.png");
    private static final Random r = new Random();
    private int time = 0;
    private int hitTime = 0;
    private int hitTimeO = 0;
    private int lastSinnedTimes = -1;

    public GeburahBossBar(UUID uuid, int entityId) {
        super(uuid, entityId);
    }

    public void render(GuiGraphics guiGraphics, float v) {
        PoseStack matrices = guiGraphics.m_280168_();
        matrices.m_85836_();
        if (this.hitTime != 0) {
            float str = 1.0f;
            float t = FDMathUtil.lerp((float)this.hitTimeO, (float)this.hitTime, (float)v) / 20.0f;
            t *= t;
            long rndOffset = 2343L;
            Random shakeRnd = new Random((long)(this.time + 1) * rndOffset);
            float tx = shakeRnd.nextFloat() * 2.5f - 1.25f;
            float ty = shakeRnd.nextFloat() * 2.5f - 1.25f;
            matrices.m_252880_(tx * (str *= t), ty * str, 0.0f);
        }
        int hp = (int)Math.ceil(this.getPercentage() * 10.0f);
        FDRenderUtil.bindTexture((ResourceLocation)GEBURAH_BOSSBAR);
        float scale = 1.25f;
        float width = 287.0f / scale;
        float height = 70.0f / scale;
        FDRenderUtil.blitWithBlend((PoseStack)guiGraphics.m_280168_(), (float)(-width / 2.0f), (float)0.0f, (float)width, (float)height, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        FDRenderUtil.bindTexture((ResourceLocation)FDBosses.location("textures/gui/geburah_sin_scales.png"));
        float scalesWidth = 18.0f;
        float scalesOffset = 24.0f;
        int hp1 = Math.min(hp, 5);
        for (int i = 0; i < 5; ++i) {
            float xOffs = 9.0f / scale + -width / 2.0f + (float)i * scalesOffset / scale;
            float yOffs = 30.0f / scale;
            if (this.hitTime == 19 && this.lastSinnedTimes != -1 && i == this.lastSinnedTimes) {
                Matrix4f p = guiGraphics.m_280168_().m_85850_().m_252922_();
                Vector3f pos = p.transformPosition(new Vector3f());
                this.particleExplosion(pos.x + xOffs + scalesWidth / scale / 2.0f, pos.y + yOffs + 15.0f / scale / 2.0f - scale * 2.0f);
                this.lastSinnedTimes = -1;
            }
            if (i >= hp1) continue;
            FDRenderUtil.blitWithBlend((PoseStack)guiGraphics.m_280168_(), (float)xOffs, (float)yOffs, (float)(scalesWidth / scale), (float)(15.0f / scale), (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        }
        RenderSystem.disableCull();
        int hp2 = Math.min(hp - 5, 5);
        for (int i = 0; i < 5; ++i) {
            float xOffs = -8.0f / scale + width / 2.0f - (float)i * scalesOffset / scale;
            float yOffs = 30.0f / scale;
            if (this.hitTime == 19 && this.lastSinnedTimes != -1 && i + 5 == this.lastSinnedTimes) {
                Matrix4f p = guiGraphics.m_280168_().m_85850_().m_252922_();
                Vector3f pos = p.transformPosition(new Vector3f());
                this.particleExplosion(pos.x + xOffs - scalesWidth / scale / 2.0f, pos.y + yOffs + 15.0f / scale / 2.0f - scale * 2.0f);
                this.lastSinnedTimes = -1;
            }
            if (i >= hp2) continue;
            FDRenderUtil.blitWithBlend((PoseStack)guiGraphics.m_280168_(), (float)xOffs, (float)yOffs, (float)(-scalesWidth / scale), (float)(15.0f / scale), (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        }
        RenderSystem.enableCull();
        matrices.m_85849_();
    }

    public void tick(float v) {
        ++this.time;
        this.hitTimeO = this.hitTime;
        this.hitTime = Mth.m_14045_((int)(this.hitTime - 1), (int)0, (int)20);
    }

    public float height() {
        return 70.0f;
    }

    public void hanldeBarEvent(int eventId, int timesSinned) {
        if (eventId == 0) {
            this.hitTime = 20;
            this.hitTimeO = 20;
            this.lastSinnedTimes = timesSinned;
        }
    }

    private void particleExplosion(float x, float y) {
        for (int i = 0; i < 30; ++i) {
            float blue;
            float green;
            float red;
            Vec3 initialSpeed = new Vec3((double)(1.0f + r.nextFloat() * 1.0f), 0.0, 0.0).m_82535_(r.nextFloat() * (float)Math.PI * 2.0f);
            if (r.nextBoolean()) {
                float col;
                red = col = r.nextFloat() * 0.1f + 0.2f;
                green = col;
                blue = col;
            } else {
                red = 0.9f + r.nextFloat() * 0.1f;
                green = 0.6f + r.nextFloat() * 0.2f;
                blue = 0.1f;
            }
            FDTexturedSParticle fDTexturedSParticle = (FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)FDTexturedSParticle.create((Function)FDRenderUtil.ParticleRenderTypesS.TEXTURES_BLUR_ADDITIVE, (ResourceLocation)BallParticle.LOCATION).setColor(red, green, blue, 1.0f)).sendToOverlay()).setMaxQuadSize(5.0f).setDefaultScaleOut().setLifetime(30)).setPos((double)(x + r.nextFloat() * 6.0f - 3.0f), (double)(y + r.nextFloat() * 6.0f - 3.0f), true)).setSpeed(initialSpeed.f_82479_, initialSpeed.f_82480_)).setAcceleration(0.0, 0.25)).sendToScreen();
        }
    }
}

