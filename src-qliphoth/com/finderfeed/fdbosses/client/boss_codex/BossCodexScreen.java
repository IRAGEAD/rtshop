/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDScreenParticle
 *  com.finderfeed.fdlib.systems.screen.screen_particles.ScreenParticleEngine
 *  com.finderfeed.fdlib.systems.simple_screen.SimpleFDScreen
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Renderable
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.ForgeHooksClient
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector2f
 */
package com.finderfeed.fdbosses.client.boss_codex;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.boss_codex.FlashyColoredQuadParticle;
import com.finderfeed.fdbosses.client.boss_codex.LineBetweenStars;
import com.finderfeed.fdbosses.client.boss_codex.StarButton;
import com.finderfeed.fdbosses.init.BossCoreShaders;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.systems.screen.screen_particles.FDScreenParticle;
import com.finderfeed.fdlib.systems.screen.screen_particles.ScreenParticleEngine;
import com.finderfeed.fdlib.systems.simple_screen.SimpleFDScreen;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;

public class BossCodexScreen
extends SimpleFDScreen {
    public static RenderTarget RENDER_TARGET;
    public static final ResourceLocation TREE;
    public static final ResourceLocation NAMES;
    public float scaleProgressO = 1.0f;
    public float fromScaleProgress = 1.0f;
    public float scaleProgress = 1.0f;
    public float targetScaleProgress = 1.0f;
    public int scaleTime = 0;
    public int currentScaleTime = 0;
    public float offsetXTarget = 0.0f;
    public float offsetYTarget = 0.0f;
    public float offsetXPrev = 0.0f;
    public float offsetYPrev = 0.0f;
    public float offsetX = 0.0f;
    public float offsetY = 0.0f;
    public float offsetXO = 0.0f;
    public float offsetYO = 0.0f;
    public int offsetTime = 0;
    public int currentOffsetTime = 0;
    private boolean blockingOffset;
    public Runnable offsetEndAction;
    private List<LineBetweenStars> lines = new ArrayList<LineBetweenStars>();
    public ScreenParticleEngine screenParticleEngine;
    public StarButton starMalkuth;
    public StarButton starYesod;
    public StarButton starHod;
    public StarButton starNetzach;
    public StarButton starTiphereth;
    public StarButton starGeburah;
    public StarButton starChesed;
    public StarButton starBinah;
    public StarButton starHokma;
    public StarButton starKether;
    private int time = 0;
    private boolean playOpenAnimation;
    private LinkedList<FDScreenParticle<?>> particles = new LinkedList();

    public BossCodexScreen(boolean playOpenAnimation) {
        this.playOpenAnimation = playOpenAnimation;
    }

    protected void m_7856_() {
        super.m_7856_();
        Window window = Minecraft.m_91087_().m_91268_();
        float aspectRatio = (float)window.m_85442_() / (float)window.m_85441_();
        this.f_96543_ = 400;
        this.f_96544_ = (int)(aspectRatio * (float)this.f_96543_);
        this.fromScaleProgress = this.scaleProgress = 0.635f;
        this.scaleProgressO = this.scaleProgress;
        this.targetScaleProgress = this.scaleProgress;
        this.offsetYPrev = this.offsetY = -250.0f;
        this.offsetYO = this.offsetY;
        this.offsetYTarget = this.offsetY;
        this.offsetTime = 0;
        this.currentOffsetTime = 0;
        this.offsetXO = this.offsetX = 0.0f;
        this.offsetXPrev = this.offsetX;
        this.offsetXTarget = this.offsetX;
        if (this.playOpenAnimation) {
            this.scaleTo(0.235f, 42);
            this.moveTo(0.0f, 40.0f, 42);
        } else {
            this.fromScaleProgress = this.scaleProgress = 0.235f;
            this.scaleProgressO = this.scaleProgress;
            this.targetScaleProgress = this.scaleProgress;
            this.offsetYPrev = this.offsetY = 40.0f;
            this.offsetYO = this.offsetY;
            this.offsetYTarget = this.offsetY;
        }
        if (RENDER_TARGET == null) {
            RENDER_TARGET = new TextureTarget(window.m_85441_(), window.m_85442_(), true, Minecraft.f_91002_);
        } else {
            RENDER_TARGET.m_83941_(window.m_85441_(), window.m_85442_(), Minecraft.f_91002_);
        }
        this.screenParticleEngine = new ScreenParticleEngine();
        this.lines.clear();
        Random random = new Random();
        float sideOffset = 100.0f;
        float wOffset = -12.0f;
        float hOffset = -12.0f;
        int lineTravelTime = 6;
        this.starMalkuth = new StarButton(BossEntities.MALKUTH.get(), (Screen)this, wOffset, 200.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), 0.0f, 0, 1.3f);
        this.starYesod = new StarButton(null, (Screen)this, wOffset, 100.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), -30.0f, lineTravelTime, 1.35f);
        this.starHod = new StarButton(null, (Screen)this, -sideOffset + wOffset, 30.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), 23.0f, lineTravelTime * 2, -1.4f);
        this.starNetzach = new StarButton(null, (Screen)this, sideOffset + wOffset, 30.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), -12.0f, lineTravelTime * 2, 1.4f);
        this.starTiphereth = new StarButton(null, (Screen)this, wOffset, -29.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), 40.0f, lineTravelTime * 3, 1.45f);
        this.starGeburah = new StarButton(BossEntities.GEBURAH.get(), (Screen)this, -sideOffset + wOffset, -95.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), -10.0f, lineTravelTime * 4, 1.5f);
        this.starChesed = new StarButton(BossEntities.CHESED.get(), (Screen)this, sideOffset + wOffset, -95.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), 23.0f, lineTravelTime * 4, -1.5f);
        this.starBinah = new StarButton(null, (Screen)this, -sideOffset + wOffset, -220.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), -1.0f, lineTravelTime * 5, -1.55f);
        this.starHokma = new StarButton(null, (Screen)this, sideOffset + wOffset, -220.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), 23.0f, lineTravelTime * 5, 1.55f);
        this.starKether = new StarButton(null, (Screen)this, wOffset, -280.0f + hOffset, 24.0f, 24.0f, random.nextInt(6), -23.0f, lineTravelTime * 6, 1.6f);
        if (!this.playOpenAnimation) {
            this.starMalkuth.setActivated();
            this.starYesod.setActivated();
            this.starHod.setActivated();
            this.starNetzach.setActivated();
            this.starTiphereth.setActivated();
            this.starGeburah.setActivated();
            this.starChesed.setActivated();
            this.starBinah.setActivated();
            this.starHokma.setActivated();
            this.starKether.setActivated();
        }
        int flashTime = 40;
        LineBetweenStars lineBetweenStars1 = new LineBetweenStars(this.starMalkuth, this.starYesod, flashTime, 0, 0, lineTravelTime);
        LineBetweenStars lineBetweenStars14 = new LineBetweenStars(this.starMalkuth, this.starHod, flashTime, 31, 0, lineTravelTime * 2);
        LineBetweenStars lineBetweenStars15 = new LineBetweenStars(this.starMalkuth, this.starNetzach, flashTime, 17, 0, lineTravelTime * 2);
        LineBetweenStars lineBetweenStars2 = new LineBetweenStars(this.starYesod, this.starHod, flashTime, 20, lineTravelTime, lineTravelTime);
        LineBetweenStars lineBetweenStars3 = new LineBetweenStars(this.starYesod, this.starNetzach, flashTime, 10, lineTravelTime, lineTravelTime);
        LineBetweenStars lineBetweenStars12 = new LineBetweenStars(this.starNetzach, this.starChesed, flashTime, 29, lineTravelTime * 2, lineTravelTime * 2);
        LineBetweenStars lineBetweenStars13 = new LineBetweenStars(this.starHod, this.starGeburah, flashTime, 7, lineTravelTime * 2, lineTravelTime * 2);
        LineBetweenStars lineBetweenStars4 = new LineBetweenStars(this.starHod, this.starTiphereth, flashTime, 4, lineTravelTime * 2, lineTravelTime);
        LineBetweenStars lineBetweenStars5 = new LineBetweenStars(this.starNetzach, this.starTiphereth, flashTime, 30, lineTravelTime * 2, lineTravelTime);
        LineBetweenStars lineBetweenStars6 = new LineBetweenStars(this.starTiphereth, this.starChesed, flashTime, 15, lineTravelTime * 3, lineTravelTime);
        LineBetweenStars lineBetweenStars7 = new LineBetweenStars(this.starTiphereth, this.starGeburah, flashTime, 5, lineTravelTime * 3, lineTravelTime);
        LineBetweenStars lineBetweenStars8 = new LineBetweenStars(this.starGeburah, this.starBinah, flashTime, 19, lineTravelTime * 4, lineTravelTime);
        LineBetweenStars lineBetweenStars9 = new LineBetweenStars(this.starChesed, this.starHokma, flashTime, 25, lineTravelTime * 4, lineTravelTime);
        LineBetweenStars lineBetweenStars10 = new LineBetweenStars(this.starBinah, this.starKether, flashTime, 34, lineTravelTime * 5, lineTravelTime);
        LineBetweenStars lineBetweenStars11 = new LineBetweenStars(this.starHokma, this.starKether, flashTime, 23, lineTravelTime * 5, lineTravelTime);
        this.lines.add(lineBetweenStars14);
        this.lines.add(lineBetweenStars15);
        this.lines.add(lineBetweenStars12);
        this.lines.add(lineBetweenStars13);
        this.lines.add(lineBetweenStars1);
        this.lines.add(lineBetweenStars2);
        this.lines.add(lineBetweenStars3);
        this.lines.add(lineBetweenStars4);
        this.lines.add(lineBetweenStars5);
        this.lines.add(lineBetweenStars6);
        this.lines.add(lineBetweenStars7);
        this.lines.add(lineBetweenStars8);
        this.lines.add(lineBetweenStars9);
        this.lines.add(lineBetweenStars10);
        this.lines.add(lineBetweenStars11);
        if (!this.playOpenAnimation) {
            for (LineBetweenStars line : this.lines) {
                line.setActivated();
            }
        }
        this.m_142416_((GuiEventListener)this.starMalkuth);
        this.m_142416_((GuiEventListener)this.starYesod);
        this.m_142416_((GuiEventListener)this.starHod);
        this.m_142416_((GuiEventListener)this.starNetzach);
        this.m_142416_((GuiEventListener)this.starTiphereth);
        this.m_142416_((GuiEventListener)this.starGeburah);
        this.m_142416_((GuiEventListener)this.starChesed);
        this.m_142416_((GuiEventListener)this.starBinah);
        this.m_142416_((GuiEventListener)this.starHokma);
        this.m_142416_((GuiEventListener)this.starKether);
    }

    public void m_86600_() {
        float p;
        super.m_86600_();
        this.scaleProgressO = this.scaleProgress;
        if (this.scaleTime != 0) {
            this.currentScaleTime = Mth.m_14045_((int)(this.currentScaleTime + 1), (int)0, (int)this.scaleTime);
            p = FDEasings.easeOut((float)((float)this.currentScaleTime / (float)this.scaleTime));
            this.scaleProgress = FDMathUtil.lerp((float)this.fromScaleProgress, (float)this.targetScaleProgress, (float)p);
        } else {
            this.scaleProgress = this.targetScaleProgress;
        }
        this.offsetXO = this.offsetX;
        this.offsetYO = this.offsetY;
        if (this.offsetTime != 0) {
            this.currentOffsetTime = Mth.m_14045_((int)(this.currentOffsetTime + 1), (int)0, (int)this.offsetTime);
            p = FDEasings.easeOut((float)((float)this.currentOffsetTime / (float)this.offsetTime));
            if (this.currentOffsetTime >= this.offsetTime) {
                this.blockingOffset = false;
                if (this.offsetEndAction != null) {
                    this.offsetEndAction.run();
                    this.offsetEndAction = null;
                }
            }
            this.offsetX = FDMathUtil.lerp((float)this.offsetXPrev, (float)this.offsetXTarget, (float)p);
            this.offsetY = FDMathUtil.lerp((float)this.offsetYPrev, (float)this.offsetYTarget, (float)p);
        } else {
            if (this.offsetEndAction != null) {
                this.offsetEndAction.run();
                this.offsetEndAction = null;
            }
            this.blockingOffset = false;
            this.offsetX = this.offsetXTarget;
            this.offsetY = this.offsetYTarget;
        }
        this.particles.removeIf(FDScreenParticle::isRemoved);
        if (this.time % 4 == 0) {
            Vector2f mousePos = this.getMousePos(0.0f);
            RandomSource random = Minecraft.m_91087_().f_91073_.f_46441_;
            float speed = random.m_188501_() * 0.5f + 0.5f;
            Vec3 rnd = new Vec3((double)speed, 0.0, 0.0).m_82535_(random.m_188501_() * (float)Math.PI * 2.0f);
            FlashyColoredQuadParticle flashyColoredQuadParticle = (FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)((FlashyColoredQuadParticle)new FlashyColoredQuadParticle().setPos(mousePos.x, mousePos.y, true)).setColor(1.0f, 1.0f, 0.25f + random.m_188501_() * 0.25f, 1.0f)).setQuadSize(0.5f + random.m_188501_() * 0.25f).setFlashFrequency(0.75f).setSpeed(rnd.f_82479_, rnd.f_82480_)).setFriction(0.95f)).setLifetime(40);
            this.particles.add(flashyColoredQuadParticle);
            this.screenParticleEngine.addParticle((FDScreenParticle)flashyColoredQuadParticle);
        }
        ++this.time;
        this.screenParticleEngine.tick();
        for (LineBetweenStars line : this.lines) {
            line.tick();
        }
    }

    public void m_88315_(GuiGraphics graphics, int mx, int my, float pticks) {
        Matrix4f previousProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0f, (float)((double)this.f_96543_), (float)((double)this.f_96544_), 0.0f, 1000.0f, ForgeHooksClient.getGuiFarPlane());
        RenderSystem.setProjectionMatrix((Matrix4f)matrix4f, (VertexSorting)VertexSorting.f_276633_);
        Window window = Minecraft.m_91087_().m_91268_();
        FDRenderUtil.fill((PoseStack)graphics.m_280168_(), (float)0.0f, (float)0.0f, (float)window.m_85441_(), (float)window.m_85442_(), (float)0.0f, (float)0.0f, (float)0.0f, (float)0.5f);
        this.m_280273_(graphics);
        this.renderTree(graphics, mx, my, pticks);
        RenderSystem.setProjectionMatrix((Matrix4f)previousProjection, (VertexSorting)previousSorting);
    }

    public void renderFramebufferedTree(GuiGraphics graphics, int mx, int my, float pticks) {
        pticks = FDRenderUtil.tryGetPartialTickIgnorePause();
        PoseStack matrices = graphics.m_280168_();
        matrices.m_85836_();
        RenderSystem.disableCull();
        matrices.m_85841_(1.0f, -1.0f, 1.0f);
        matrices.m_252880_(0.0f, (float)(-this.f_96544_), 0.0f);
        RenderSystem.setShader(() -> BossCoreShaders.CODEX_UI);
        float ntime = 0.005f * ((float)this.time + pticks);
        BossCoreShaders.CODEX_UI.m_173356_("time").m_5985_(ntime);
        BossCoreShaders.CODEX_UI.m_173356_("offsetX").m_5985_(-this.getOffsetX(pticks));
        BossCoreShaders.CODEX_UI.m_173356_("offsetY").m_5985_(this.getOffsetY(pticks));
        BossCoreShaders.CODEX_UI.m_173356_("scale").m_5985_(this.getRealScale(pticks));
        BossCoreShaders.CODEX_UI.m_173356_("screenSize").m_7971_((float)this.f_96543_, (float)this.f_96544_);
        int count = 64;
        float[] positions = new float[count * 2];
        Arrays.fill(positions, 1000000.0f);
        float[] radiuses = new float[count];
        int index = 0;
        for (Renderable renderable : this.f_169369_) {
            StarButton starButton;
            if (!(renderable instanceof StarButton) || !(starButton = (StarButton)renderable).isActivated()) continue;
            float starTime = (float)starButton.getTick() + FDRenderUtil.tryGetPartialTickIgnorePause();
            float t = starTime + (float)starButton.getCurrentFrame() * 0.37f;
            float flicker = 1.0f - 0.15f * (float)Math.pow(Math.max(0.0, Math.sin(t * 9.0f)), 8.0);
            float radius = 60.0f * flicker;
            if (starTime > (float)starButton.getActivationTime()) {
                float p = 1.0f - Mth.m_14036_((float)((starTime - (float)starButton.getActivationTime()) / 10.0f), (float)0.0f, (float)1.0f);
                radius = Math.max(radius, 100.0f * FDEasings.easeIn((float)p));
            }
            positions[index * 2] = starButton.getX();
            positions[index * 2 + 1] = starButton.getY();
            radiuses[index] = radius;
            ++index;
        }
        Vector2f mousePos = this.getMousePos(pticks);
        positions[index * 2] = mousePos.x;
        positions[index * 2 + 1] = mousePos.y;
        radiuses[index] = 15.0f;
        ++index;
        for (int i = this.particles.size() - 1; i >= 0 && index < count; ++index, --i) {
            FDScreenParticle<?> particle = this.particles.get(i);
            float p = 1.0f - ((float)particle.getAge() + FDRenderUtil.tryGetPartialTickIgnorePause()) / (float)particle.getLifetime();
            p = FDEasings.easeOutBack((float)p);
            positions[index * 2] = (float)particle.getX(FDRenderUtil.tryGetPartialTickIgnorePause());
            positions[index * 2 + 1] = (float)particle.getY(FDRenderUtil.tryGetPartialTickIgnorePause());
            radiuses[index] = p * 3.0f;
        }
        BossCoreShaders.CODEX_UI.m_173356_("radiuses").m_5941_(radiuses);
        BossCoreShaders.CODEX_UI.m_173356_("positions").m_5941_(positions);
        RenderSystem.setShaderTexture((int)0, (int)RENDER_TARGET.m_83975_());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder vertex = Tesselator.m_85913_().m_85915_();
        vertex.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85819_);
        vertex.m_252986_(matrices.m_85850_().m_252922_(), 0.0f, 0.0f, 0.0f).m_7421_(0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(matrices.m_85850_().m_252922_(), 0.0f, (float)this.f_96544_, 0.0f).m_7421_(0.0f, 1.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(matrices.m_85850_().m_252922_(), (float)this.f_96543_, (float)this.f_96544_, 0.0f).m_7421_(1.0f, 1.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(matrices.m_85850_().m_252922_(), (float)this.f_96543_, 0.0f, 0.0f).m_7421_(1.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)vertex.m_231175_());
        RenderSystem.disableBlend();
        matrices.m_85849_();
    }

    public void renderTree(GuiGraphics graphics, int mx, int my, float pticks) {
        PoseStack matrices = graphics.m_280168_();
        matrices.m_85836_();
        float rs = this.getRealScale(FDRenderUtil.tryGetPartialTickIgnorePause());
        float offsetX = this.getOffsetX(FDRenderUtil.tryGetPartialTickIgnorePause());
        float offsetY = this.getOffsetY(FDRenderUtil.tryGetPartialTickIgnorePause());
        matrices.m_252880_((float)this.f_96543_ / 2.0f, (float)this.f_96544_ / 2.0f, 0.0f);
        matrices.m_85841_(rs, rs, rs);
        matrices.m_252880_(offsetX, offsetY, 0.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderTarget main = Minecraft.m_91087_().m_91385_();
        float treeWidth = 484.0f;
        float treeHeight = 683.0f;
        RENDER_TARGET.m_83954_(Minecraft.f_91002_);
        RENDER_TARGET.m_83947_(true);
        FDRenderUtil.bindTexture((ResourceLocation)TREE);
        FDRenderUtil.blitWithBlendCentered((PoseStack)matrices, (float)-4.5f, (float)-0.5f, (float)treeWidth, (float)treeHeight, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        main.m_83947_(true);
        matrices.m_85849_();
        this.renderFramebufferedTree(graphics, mx, my, pticks);
        matrices.m_85836_();
        matrices.m_252880_((float)this.f_96543_ / 2.0f, (float)this.f_96544_ / 2.0f, 0.0f);
        matrices.m_85841_(rs, rs, rs);
        matrices.m_252880_(offsetX, offsetY, 0.0f);
        FDRenderUtil.bindTexture((ResourceLocation)NAMES);
        if (this.starMalkuth.isActivated()) {
            this.renderName(matrices, 0.0f, 6.0f, 230.0f, 0.0f, true);
        }
        if (this.starYesod.isActivated()) {
            this.renderName(matrices, 1.8032743f, 14.0f, 70.0f, 1.0f, false);
        }
        if (this.starHod.isActivated()) {
            this.renderName(matrices, 0.4944867f, -122.0f, 10.0f, 2.0f, false);
        }
        if (this.starTiphereth.isActivated()) {
            this.renderName(matrices, 2.3794422f, 0.0f, -65.0f, 3.0f, false);
        }
        if (this.starChesed.isActivated()) {
            this.renderName(matrices, 2.745752f, 155.0f, -120.0f, 4.0f, true);
        }
        if (this.starGeburah.isActivated()) {
            this.renderName(matrices, 1.1749557f, -146.0f, -120.0f, 5.0f, true);
        }
        if (this.starBinah.isActivated()) {
            this.renderName(matrices, 0.8607964f, -136.0f, -240.0f, 6.0f, false);
        }
        if (this.starKether.isActivated()) {
            this.renderName(matrices, 3.0599113f, 13.0f, -310.0f, 7.0f, false);
        }
        if (this.starHokma.isActivated()) {
            this.renderName(matrices, 1.1749557f, 155.0f, -240.0f, 8.0f, false);
        }
        if (this.starNetzach.isActivated()) {
            this.renderName(matrices, 3.68823f, 155.0f, 10.0f, 9.0f, false);
        }
        for (LineBetweenStars line : this.lines) {
            line.render(graphics);
        }
        Vector2f mousePos = this.getMousePos(FDRenderUtil.tryGetPartialTickIgnorePause());
        this.screenParticleEngine.render(graphics, FDRenderUtil.tryGetPartialTickIgnorePause());
        matrices.m_85836_();
        matrices.m_252880_(mousePos.x, mousePos.y, 0.0f);
        matrices.m_252781_(Axis.f_252403_.m_252977_(-2.0f * ((float)this.time + FDRenderUtil.tryGetPartialTickIgnorePause())));
        FDRenderUtil.bindTexture((ResourceLocation)StarButton.STAR);
        FDRenderUtil.blitWithBlendCentered((PoseStack)matrices, (float)0.0f, (float)0.0f, (float)16.0f, (float)16.0f, (float)0.0f, (float)(this.time / 2 % 10), (float)1.0f, (float)1.0f, (float)1.0f, (float)11.0f, (float)0.0f, (float)1.0f);
        matrices.m_85849_();
        for (Renderable renderable : this.f_169369_) {
            renderable.m_88315_(graphics, (int)mousePos.x, (int)mousePos.y, pticks);
        }
        matrices.m_85849_();
        RenderSystem.disableBlend();
    }

    private void renderName(PoseStack matrices, float offset, float x, float y, float texPosY, boolean opened) {
        float t = ((float)this.time + FDRenderUtil.tryGetPartialTickIgnorePause()) * 0.05f;
        float ampl = 1.5f;
        float xOffs = (float)Math.sin(t + offset) * ampl;
        float yOffs = (float)Math.sin(t * 1.25f + 1.3304645f + offset) * ampl;
        FDRenderUtil.blitWithBlendRgb((PoseStack)matrices, (float)(x + xOffs - 33.0f), (float)(y + yOffs - 5.0f), (float)66.0f, (float)10.0f, (float)0.0f, (float)texPosY, (float)1.0f, (float)1.0f, (float)1.0f, (float)10.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)(opened ? 1.0f : 0.15f), (float)(opened ? 1.0f : 0.15f));
    }

    public float getOffsetX(float partialTicks) {
        return FDMathUtil.lerp((float)this.offsetXO, (float)this.offsetX, (float)partialTicks);
    }

    public float getOffsetY(float partialTicks) {
        return FDMathUtil.lerp((float)this.offsetYO, (float)this.offsetY, (float)partialTicks);
    }

    public void moveTo(float offsetX, float offsetY, int time) {
        this.moveTo(offsetX, offsetY, time, false, null);
    }

    public void moveTo(float offsetX, float offsetY, int time, boolean blocking, Runnable offsetEndAction) {
        this.offsetXPrev = this.offsetX;
        this.offsetYPrev = this.offsetY;
        this.offsetXTarget = offsetX;
        this.offsetYTarget = offsetY;
        this.currentOffsetTime = 0;
        this.offsetTime = time;
        this.blockingOffset = blocking;
        if (this.offsetEndAction != null) {
            this.offsetEndAction.run();
        }
        this.offsetEndAction = offsetEndAction;
    }

    public boolean m_5953_(double mx, double my) {
        Vector2f realPos = this.getMousePos(0.0f);
        return super.m_5953_((double)realPos.x, (double)realPos.y);
    }

    public boolean m_6050_(double mx, double my, double yOffs) {
        this.scaleTo((float)((double)this.getTargetScaleProgress() + yOffs * (double)0.035f), 2);
        Vector2f realPos = this.getMousePos(0.0f);
        return super.m_6050_((double)realPos.x, (double)realPos.y, yOffs);
    }

    public void scaleTo(float target, int time) {
        target = Mth.m_14036_((float)target, (float)0.0f, (float)1.0f);
        this.currentScaleTime = 0;
        this.fromScaleProgress = this.scaleProgress;
        this.scaleTime = time;
        this.targetScaleProgress = target;
    }

    public float getTargetScaleProgress() {
        return this.targetScaleProgress;
    }

    public float getRealScale(float pticks) {
        return FDMathUtil.lerp((float)0.1f, (float)5.0f, (float)FDEasings.easeIn((float)FDMathUtil.lerp((float)this.scaleProgressO, (float)this.scaleProgress, (float)pticks)));
    }

    public boolean m_6375_(double mx, double my, int button) {
        Vector2f realPos = this.getMousePos(0.0f);
        return super.m_6375_((double)realPos.x, (double)realPos.y, button);
    }

    public boolean m_7979_(double mx, double my, int button, double xOffs, double yOffs) {
        if (button == 0 && !this.blockingOffset) {
            float realScale = this.getRealScale(0.0f);
            this.moveTo((float)((double)this.offsetXTarget + xOffs / (double)realScale), (float)((double)this.offsetYTarget + yOffs / (double)realScale), 0);
        }
        Vector2f realPos = this.getMousePos(0.0f);
        return super.m_7979_((double)realPos.x, (double)realPos.y, button, xOffs, yOffs);
    }

    private Vector2f getMousePos(float pticks) {
        float px = (float)Minecraft.m_91087_().f_91067_.m_91589_() / (float)Minecraft.m_91087_().m_91268_().m_85441_();
        float py = (float)Minecraft.m_91087_().f_91067_.m_91594_() / (float)Minecraft.m_91087_().m_91268_().m_85442_();
        float windowX = px * (float)this.f_96543_;
        float windowY = py * (float)this.f_96544_;
        float rs = this.getRealScale(pticks);
        windowX -= (float)this.f_96543_ / 2.0f + this.getOffsetX(pticks) * rs;
        windowY -= (float)this.f_96544_ / 2.0f + this.getOffsetY(pticks) * rs;
        return new Vector2f(windowX /= rs, windowY /= rs);
    }

    public boolean m_6348_(double mx, double my, int button) {
        Vector2f realPos = this.getMousePos(0.0f);
        return super.m_6348_((double)realPos.x, (double)realPos.y, button);
    }

    public void m_94757_(double mx, double my) {
        Vector2f realPos = this.getMousePos(0.0f);
        super.m_94757_((double)realPos.x, (double)realPos.y);
    }

    public float getScreenWidth() {
        return 0.0f;
    }

    public float getScreenHeight() {
        return 0.0f;
    }

    static {
        TREE = FDBosses.location("textures/gui/tree_of_life.png");
        NAMES = FDBosses.location("textures/gui/names.png");
    }
}

