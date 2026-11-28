/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDScreenParticle
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle
 *  com.finderfeed.fdlib.systems.screen.screen_particles.ScreenParticleEngine
 *  com.finderfeed.fdlib.systems.simple_screen.FDWidget
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$ParticleRenderTypesS
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector2f
 */
package com.finderfeed.fdbosses.client.boss_codex;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.BossRenderUtil;
import com.finderfeed.fdbosses.client.boss_codex.BossCodexScreen;
import com.finderfeed.fdbosses.client.boss_codex.FlashyColoredQuadParticle;
import com.finderfeed.fdbosses.client.boss_codex.FlashyTexturedScreenParticle;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.packets.RequestDossierScreenPacket;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.screen.screen_particles.FDScreenParticle;
import com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle;
import com.finderfeed.fdlib.systems.screen.screen_particles.ScreenParticleEngine;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class StarButton
extends FDWidget {
    public static final ResourceLocation STAR = FDBosses.location("textures/gui/star/star.png");
    public static final ResourceLocation STAR_NOT_RELEASED = FDBosses.location("textures/gui/star/star_not_released.png");
    public static final ResourceLocation SINGLE_STAR = FDBosses.location("gui/star/singlestar");
    private int currentFrame;
    private int tick = 0;
    private float startingAngle = 0.0f;
    private int startingFrame;
    private int activationTime;
    private Random random;
    public float activationPitch;
    public EntityType<?> entityType;

    public StarButton(EntityType<?> entityType, Screen screen, float x, float y, float width, float height, int startFrame, float startingAngle, int activationTime, float activationPitch) {
        super(screen, x, y, width, height);
        this.activationPitch = activationPitch;
        this.entityType = entityType;
        this.startingFrame = this.currentFrame = Mth.m_14045_((int)startFrame, (int)0, (int)10);
        this.startingAngle = startingAngle;
        this.activationTime = activationTime;
        this.random = new Random();
    }

    public void setActivated() {
        this.tick = this.activationTime + 1;
    }

    public void tick() {
        super.tick();
        if (this.tick >= this.activationTime) {
            BossCodexScreen bossCodexScreen = (BossCodexScreen)this.widgetOwner;
            ScreenParticleEngine particleEngine = bossCodexScreen.screenParticleEngine;
            Vector2f pos = new Vector2f(this.getX() + this.getWidth() / 2.0f, this.getY() + this.getHeight() / 2.0f);
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            if (this.entityType == null) {
                r = 1.0f;
                g = 0.0f;
                b = 0.0f;
            }
            if (this.tick == this.activationTime) {
                if (this.activationPitch > 0.0f) {
                    FDClientHelpers.getSoundManager().m_120367_((SoundInstance)SimpleSoundInstance.m_119752_((SoundEvent)BossSounds.QLIPHOTHIC_CODEX_STAR.get(), (float)this.activationPitch));
                }
                FDTexturedSParticle particle = (FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)FlashyTexturedScreenParticle.create(FDRenderUtil.ParticleRenderTypesS.TEXTURES_DEFAULT, SINGLE_STAR).setFlashFrequency(1.0f).setFlashOffset(0.7853982f).setRollFriction(0.8f).setQuadScaleOptions(ComplexEasingFunction.builder().addArea(1.0f, FDEasings::easeOut).build()).setColor(r, g, b, 1.0f)).setMaxQuadSize(100.0f).setPos((double)pos.x, (double)pos.y, true)).setLifetime(20)).setRollSpeed((10.0f + this.random.nextFloat() * 10.0f) * (float)(this.startingAngle > 0.0f ? -1 : 1))).setRoll(this.random.nextFloat() * 45.0f, true);
                FDTexturedSParticle particle2 = (FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)FDTexturedSParticle.create((Function)FDRenderUtil.ParticleRenderTypesS.TEXTURES_BLUR_ADDITIVE, (ResourceLocation)BallParticle.LOCATION).setQuadScaleOptions(ComplexEasingFunction.builder().addArea(1.0f, FDEasings::quadroHill).build()).setColor(r, g, 0.0f, 1.0f)).setMaxQuadSize(100.0f).setPos((double)pos.x, (double)pos.y, true)).setLifetime(2);
                particleEngine.addParticle((FDScreenParticle)particle);
                particleEngine.addParticle((FDScreenParticle)particle2);
                for (int i = 0; i < 40; ++i) {
                    float speed = this.random.nextFloat() * 16.0f + 2.0f;
                    Vec3 rnd = new Vec3((double)speed, 0.0, 0.0).m_82535_(this.random.nextFloat() * (float)Math.PI * 2.0f);
                    FlashyColoredQuadParticle flashyColoredQuadParticle = (FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)new FlashyColoredQuadParticle().setPos(pos.x, pos.y, true)).setColor(r, g, 0.25f + this.random.nextFloat() * 0.25f, 1.0f)).setQuadSize(0.75f).setFlashOffset(this.random.nextFloat() * (float)Math.PI).setFlashFrequency(0.75f).setSpeed(rnd.f_82479_, rnd.f_82480_)).setFriction(0.8f)).setLifetime(40);
                    particleEngine.addParticle((FDScreenParticle)flashyColoredQuadParticle);
                }
            }
            if (this.tick % 2 == 0) {
                float speed = this.random.nextFloat() * 0.5f + 0.5f;
                Vec3 rnd = new Vec3((double)speed, 0.0, 0.0).m_82535_(this.random.nextFloat() * (float)Math.PI * 2.0f);
                FlashyColoredQuadParticle flashyColoredQuadParticle = (FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)new FlashyColoredQuadParticle().setPos(pos.x, pos.y, true)).setColor(r, g, 0.25f + this.random.nextFloat() * 0.25f, 1.0f)).setQuadSize(0.5f).setFlashFrequency(0.75f).setSpeed(rnd.f_82479_, rnd.f_82480_)).setFriction(0.95f)).setLifetime(40);
                particleEngine.addParticle((FDScreenParticle)flashyColoredQuadParticle);
            }
        }
        ++this.tick;
        if (this.tick % 2 == 0) {
            this.currentFrame = (this.currentFrame + 1) % 11;
        }
    }

    public boolean isActivated() {
        return this.tick > this.activationTime;
    }

    public int getTick() {
        return this.tick;
    }

    public int getActivationTime() {
        return this.activationTime;
    }

    public int getCurrentFrame() {
        return this.currentFrame;
    }

    public int getStartingFrame() {
        return this.startingFrame;
    }

    public void renderWidget(GuiGraphics guiGraphics, float v, float v1, float v2) {
        RenderSystem.blendFunc((int)770, (int)1);
        if (this.entityType != null) {
            FDRenderUtil.bindTexture((ResourceLocation)STAR);
        } else {
            FDRenderUtil.bindTexture((ResourceLocation)STAR_NOT_RELEASED);
        }
        PoseStack matrices = guiGraphics.m_280168_();
        matrices.m_85836_();
        matrices.m_252880_(this.getX() + this.getWidth() / 2.0f, this.getY() + this.getHeight() / 2.0f, 0.0f);
        if (this.tick > this.activationTime) {
            float t = (float)this.tick + FDRenderUtil.tryGetPartialTickIgnorePause();
            if (this.startingAngle > 0.0f) {
                matrices.m_252781_(Axis.f_252403_.m_252977_(t + this.startingAngle));
            } else {
                matrices.m_252781_(Axis.f_252403_.m_252977_(-t + this.startingAngle));
            }
            FDRenderUtil.blitWithBlendRgb((PoseStack)guiGraphics.m_280168_(), (float)-32.0f, (float)-32.0f, (float)64.0f, (float)64.0f, (float)0.0f, (float)this.currentFrame, (float)1.0f, (float)1.0f, (float)1.0f, (float)11.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        } else {
            FDRenderUtil.blitWithBlendRgb((PoseStack)guiGraphics.m_280168_(), (float)-32.0f, (float)-32.0f, (float)64.0f, (float)64.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)11.0f, (float)0.0f, (float)1.0f, (float)0.1f, (float)0.1f, (float)0.1f);
        }
        matrices.m_85849_();
        RenderSystem.defaultBlendFunc();
        if (this.entityType == null && this.isHovered()) {
            BossRenderUtil.renderBossScreenTooltip(guiGraphics, (Component)Component.m_237115_((String)"fdbosses.word.not_released"), v, v1, 120.0f, 14324834, 1.0f, -1000.0f, -1000.0f, 1000.0f, 1000.0f);
        }
    }

    public boolean onMouseClick(float v, float v1, int i) {
        Screen screen = this.widgetOwner;
        if (screen instanceof BossCodexScreen) {
            BossCodexScreen bossCodexScreen = (BossCodexScreen)screen;
            if (i == 0 && this.entityType != null) {
                bossCodexScreen.moveTo(-(this.getX() + this.getWidth() / 2.0f), -(this.getY() + this.getHeight() / 2.0f), 20, true, () -> FDPacketHandler.INSTANCE.sendToServer((Object)new RequestDossierScreenPacket(this.entityType)));
                bossCodexScreen.scaleTo(0.8f, 20);
            }
        }
        return true;
    }

    public boolean onMouseRelease(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseScroll(float v, float v1, float v2) {
        return false;
    }

    public boolean onCharTyped(char c, int i) {
        return false;
    }

    public boolean onKeyPress(int i, int i1, int i2) {
        return false;
    }

    public boolean onKeyRelease(int i, int i1, int i2) {
        return false;
    }
}

