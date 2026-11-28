/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$ParticleRenderTypesS
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.gui.overlay.ForgeGui
 *  net.minecraftforge.client.gui.overlay.IGuiOverlay
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.LogicalSide
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.joml.Math
 *  org.joml.Matrix4f
 *  org.joml.Random
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.client.overlay;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthWeaknessHandler;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boss_spawner.MalkuthBossSpawner;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Random;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid="fdbosses", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class MalkuthWeaknessOverlay
implements IGuiOverlay {
    public static Random random = new Random();
    public static final int MAX_IN_TIME = 10;
    private static int iceTicker = 0;
    private static int fireTicker = 0;
    private static int iceTickerO = 0;
    private static int fireTickerO = 0;
    private static int alpha = 0;
    private static int alphaO = 0;

    @SubscribeEvent
    public static void tickClient(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (event.side != LogicalSide.CLIENT) {
            return;
        }
        Player player = FDClientHelpers.getClientPlayer();
        if (player == null) {
            iceTicker = 0;
            fireTicker = 0;
            return;
        }
        boolean spawnerNearby = MalkuthWeaknessOverlay.hasMalkuthSpawnerNearby();
        alphaO = alpha;
        alpha = spawnerNearby ? Mth.m_14045_((int)(alpha + 1), (int)0, (int)10) : Mth.m_14045_((int)(alpha - 1), (int)0, (int)10);
        MalkuthAttackType weakTo = MalkuthWeaknessHandler.getWeakTo(player);
        iceTickerO = iceTicker;
        fireTickerO = fireTicker;
        if (weakTo.isIce()) {
            iceTicker = FDMathUtil.clamp((int)(iceTicker + 1), (int)0, (int)10);
            fireTicker = FDMathUtil.clamp((int)(fireTicker - 1), (int)0, (int)10);
        } else {
            iceTicker = FDMathUtil.clamp((int)(iceTicker - 1), (int)0, (int)10);
            fireTicker = FDMathUtil.clamp((int)(fireTicker + 1), (int)0, (int)10);
        }
        if (spawnerNearby) {
            Window window = Minecraft.m_91087_().m_91268_();
            float w = window.m_85445_();
            float h = window.m_85446_();
            for (int i = 0; i < 5; ++i) {
                Vector3f color = MalkuthEntity.getAndRandomizeColor(weakTo, player.m_9236_().f_46441_);
                float x = random.nextFloat() * w;
                float y = h;
                ((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)FDTexturedSParticle.create((Function)FDRenderUtil.ParticleRenderTypesS.TEXTURES_BLUR_ADDITIVE, (ResourceLocation)BallParticle.LOCATION).setPos((double)x, (double)y, true)).setMaxQuadSize(3.5f).setSpeed(0.0, -0.4)).setFriction(1.0)).setColor(color.x, color.y, color.z, 0.8f)).setLifetime(30)).setDefaultScaleOut().sendToOverlay();
            }
        }
    }

    private static boolean hasMalkuthSpawnerNearby() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return false;
        }
        return !BossTargetFinder.getEntitiesInCylinder(MalkuthBossSpawner.class, player.m_9236_(), player.m_20182_().m_82520_(0.0, -40.0, 0.0), 80.0f, 130.0f).isEmpty();
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float pticks, int screenWidth, int screenHeight) {
        if (FDClientHelpers.getClientLevel() == null || Minecraft.m_91087_().f_91066_.f_92062_) {
            return;
        }
        Window window = Minecraft.m_91087_().m_91268_();
        float w = window.m_85445_();
        float h = window.m_85446_();
        Vector3f ice = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.ICE);
        Vector3f fire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.FIRE);
        float basicAlpha = FDMathUtil.lerp((float)alphaO, (float)alpha, (float)pticks) / 10.0f;
        if (basicAlpha == 0.0f) {
            return;
        }
        float iceAlpha = FDMathUtil.lerp((float)iceTickerO, (float)iceTicker, (float)pticks) / 10.0f;
        float fireAlpha = FDMathUtil.lerp((float)fireTickerO, (float)fireTicker, (float)pticks) / 10.0f;
        float size = 5.0f;
        PoseStack matrices = graphics.m_280168_();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::m_172811_);
        RenderSystem.blendFunc((int)770, (int)1);
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        for (int i = 0; i < 2; ++i) {
            matrices.m_85836_();
            matrices.m_252880_(w / 2.0f, h - size / 2.0f, 0.0f);
            QuadRenderer.start((VertexConsumer)builder).pose(matrices).direction(new Vec3(0.0, 0.0, -1.0)).color1(ice.x, ice.y, ice.z, iceAlpha * basicAlpha).color2(ice.x, ice.y, ice.z, iceAlpha * basicAlpha).color3(ice.x, ice.y, ice.z, 0.0f).color4(ice.x, ice.y, ice.z, 0.0f).sizeX(w / 2.0f).sizeY(size).render();
            QuadRenderer.start((VertexConsumer)builder).pose(matrices).direction(new Vec3(0.0, 0.0, -1.0)).color1(fire.x, fire.y, fire.z, fireAlpha * basicAlpha).color2(fire.x, fire.y, fire.z, fireAlpha * basicAlpha).color3(fire.x, fire.y, fire.z, 0.0f).color4(fire.x, fire.y, fire.z, 0.0f).sizeX(w / 2.0f).sizeY(size).render();
            matrices.m_85849_();
        }
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)builder.m_231175_());
        matrices.m_85836_();
        matrices.m_252880_(w / 2.0f - 0.5f, h / 2.0f - 0.5f, 0.0f);
        Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.FIRE);
        Vector3f fire1 = new Vector3f(colFire.x, colFire.y - 0.25f, colFire.z);
        Vector3f fire2 = new Vector3f(colFire.x, colFire.y, colFire.z);
        Vector3f colIce = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.ICE);
        Vector3f ice1 = new Vector3f(colIce.x - 0.1f, colIce.y - 0.6f, colIce.z);
        Vector3f ice2 = new Vector3f(colIce.x, colIce.y, colIce.z);
        float innerRadius = 1.5f;
        float radius = 7.5f;
        float time = (float)FDClientHelpers.getClientLevel().m_46467_() + pticks;
        matrices.m_252781_(Axis.f_252403_.m_252977_(time * 10.0f));
        float sin = Math.sin((float)(time / 3.0f)) / 2.0f + 0.5f;
        RenderSystem.blendFunc((int)770, (int)771);
        MalkuthWeaknessOverlay.drawCircle(new Vector3f(), new Vector3f(), matrices, radius, innerRadius, basicAlpha, 1);
        RenderSystem.blendFunc((int)770, (int)1);
        MalkuthWeaknessOverlay.drawCircle(ice1, ice2, matrices, radius, innerRadius, iceAlpha * basicAlpha, 3);
        MalkuthWeaknessOverlay.drawCircle(ice1, ice2, matrices, radius, innerRadius, iceAlpha * basicAlpha * (sin *= 0.5f), 2);
        RenderSystem.blendFunc((int)770, (int)1);
        MalkuthWeaknessOverlay.drawCircle(fire1, fire2, matrices, radius, innerRadius, fireAlpha * basicAlpha, 3);
        MalkuthWeaknessOverlay.drawCircle(fire1, fire2, matrices, radius, innerRadius, fireAlpha * basicAlpha * sin, 2);
        matrices.m_85849_();
        RenderSystem.defaultBlendFunc();
    }

    public static void drawCircle(Vector3f c1, Vector3f c2, PoseStack matrices, float radius, float innerRadius, float a, int renderAmount) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::m_172811_);
        Tesselator tesselator = Tesselator.m_85913_();
        BufferBuilder vertex = tesselator.m_85915_();
        vertex.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        Vec3 v = new Vec3((double)radius, 0.0, 0.0);
        int count = 48;
        float angle = (float)java.lang.Math.PI * 2 / (float)count;
        Matrix4f m = matrices.m_85850_().m_252922_();
        for (int i = 0; i < count; ++i) {
            float p = (float)i / (float)count;
            float sinp = Math.sin((float)((float)java.lang.Math.PI * 2 * p * 3.0f)) / 2.0f + 0.5f;
            float p2 = ((float)i + 1.0f) / (float)count;
            float sinp2 = Math.sin((float)((float)java.lang.Math.PI * 2 * p2 * 3.0f)) / 2.0f + 0.5f;
            Vector3f color1 = FDMathUtil.interpolateVectors((Vector3f)c1, (Vector3f)c2, (float)sinp);
            Vector3f color2 = FDMathUtil.interpolateVectors((Vector3f)c1, (Vector3f)c2, (float)sinp2);
            float currentAngle = angle * (float)i;
            float nextAngle = angle * (float)(i + 1);
            Vec3 offset = v.m_82535_(currentAngle);
            Vec3 noffset = offset.m_82541_().m_82542_((double)innerRadius, (double)innerRadius, (double)innerRadius);
            Vec3 offset2 = v.m_82535_(nextAngle);
            Vec3 noffset2 = offset2.m_82541_().m_82542_((double)innerRadius, (double)innerRadius, (double)innerRadius);
            for (int g = 0; g < renderAmount; ++g) {
                vertex.m_252986_(m, (float)(offset.f_82479_ - noffset.f_82479_), (float)(offset.f_82480_ - noffset.f_82480_), 0.0f).m_85950_(color1.x, color1.y, color1.z, 0.0f).m_5752_();
                vertex.m_252986_(m, (float)offset.f_82479_, (float)offset.f_82480_, 0.0f).m_85950_(color1.x, color1.y, color1.z, a).m_5752_();
                vertex.m_252986_(m, (float)offset2.f_82479_, (float)offset2.f_82480_, 0.0f).m_85950_(color2.x, color2.y, color2.z, a).m_5752_();
                vertex.m_252986_(m, (float)(offset2.f_82479_ - noffset2.f_82479_), (float)(offset2.f_82480_ - noffset2.f_82480_), 0.0f).m_85950_(color2.x, color2.y, color2.z, 0.0f).m_5752_();
                vertex.m_252986_(m, (float)offset.f_82479_, (float)offset.f_82480_, 0.0f).m_85950_(color1.x, color1.y, color1.z, a).m_5752_();
                vertex.m_252986_(m, (float)(offset.f_82479_ + noffset.f_82479_), (float)(offset.f_82480_ + noffset.f_82480_), 0.0f).m_85950_(color1.x, color1.y, color1.z, 0.0f).m_5752_();
                vertex.m_252986_(m, (float)(offset2.f_82479_ + noffset2.f_82479_), (float)(offset2.f_82480_ + noffset2.f_82480_), 0.0f).m_85950_(color2.x, color2.y, color2.z, 0.0f).m_5752_();
                vertex.m_252986_(m, (float)offset2.f_82479_, (float)offset2.f_82480_, 0.0f).m_85950_(color2.x, color2.y, color2.z, a).m_5752_();
            }
        }
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)vertex.m_231175_());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }
}

