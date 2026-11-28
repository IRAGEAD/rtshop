/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.init.FDCoreShaders
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDShaderRenderer
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.util.Mth
 *  net.minecraft.world.effect.MobEffect
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
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.client.overlay;

import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningParticle;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdlib.init.FDCoreShaders;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDShaderRenderer;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE, modid="fdbosses", value={Dist.CLIENT})
public class ElectrifiedOverlay
implements IGuiOverlay {
    private static int electrifiedEffectTick = 0;
    private static int electrifiedEffectTickO = 0;
    private static int electrifiedEffectTickMax = 20;

    @SubscribeEvent
    public static void detectChesedEffect(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (event.side != LogicalSide.CLIENT) {
            return;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            electrifiedEffectTick = 0;
            electrifiedEffectTickO = 0;
            return;
        }
        electrifiedEffectTickMax = 10;
        electrifiedEffectTickO = electrifiedEffectTick;
        electrifiedEffectTick = player.m_21023_((MobEffect)BossEffects.CHESED_ENERGIZED.get()) ? Mth.m_14045_((int)(electrifiedEffectTick + 1), (int)0, (int)electrifiedEffectTickMax) : Mth.m_14045_((int)(electrifiedEffectTick - 1), (int)0, (int)electrifiedEffectTickMax);
    }

    public static float getElectrifiedEffectPercent(float partialTick) {
        return FDMathUtil.lerp((float)electrifiedEffectTickO, (float)electrifiedEffectTick, (float)partialTick) / (float)electrifiedEffectTickMax;
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (electrifiedEffectTick == 0 && electrifiedEffectTickO == 0) {
            return;
        }
        Window window = Minecraft.m_91087_().m_91268_();
        float width = window.m_85445_();
        float height = window.m_85446_();
        PoseStack matrices = graphics.m_280168_();
        matrices.m_85836_();
        float alphamod = ElectrifiedOverlay.getElectrifiedEffectPercent(partialTick);
        alphamod = FDEasings.easeOut((float)alphamod);
        this.renderElectrifiedThing(graphics, 0.0f, 0.0f, 30.0f, alphamod);
        this.renderElectrifiedThing(graphics, width, 0.0f, -30.0f, alphamod);
        this.renderElectrifiedThing(graphics, width, height, 205.0f, alphamod);
        this.renderElectrifiedThing(graphics, 0.0f, height, -205.0f, alphamod);
        matrices.m_85849_();
    }

    private void renderElectrifiedThing(GuiGraphics graphics, float x, float y, float angle, float alphamod) {
        Window window = Minecraft.m_91087_().m_91268_();
        float width = window.m_85445_();
        float height = window.m_85446_();
        float time = (float)(System.currentTimeMillis() % 356556456L) / 100000.0f;
        GameRenderer gameRenderer = Minecraft.m_91087_().f_91063_;
        PoseStack matrices = graphics.m_280168_();
        matrices.m_85836_();
        matrices.m_252880_(x, y, 0.0f);
        float noiseAlpha = 0.7f * alphamod;
        matrices.m_252781_(Axis.f_252393_.m_252977_(angle));
        matrices.m_85836_();
        matrices.m_252880_(0.0f, 20.0f, 0.0f);
        FDShaderRenderer.start((GuiGraphics)graphics, (ShaderInstance)FDCoreShaders.NOISE).position(0.0f, 0.0f, 0.0f).setUpColor(0.5f, 1.0f, 1.0f, noiseAlpha).setDownColor(0.5f, 1.0f, 1.0f, 0.0f).setResolution(width, 40.0f).setCentered(true).setShaderUniform("size", new float[]{width, 40.0f}).setShaderUniform("xyOffset", new float[]{0.0f, -time * 3.0f}).setShaderUniform("sections", new float[]{50.0f}).setShaderUniform("octaves", new float[]{4.0f}).setShaderUniform("time", new float[]{time * 5.0f}).end();
        matrices.m_85849_();
        matrices.m_85836_();
        matrices.m_252880_(0.0f, 40.0f, 0.0f);
        FDShaderRenderer.start((GuiGraphics)graphics, (ShaderInstance)GameRenderer.m_172811_()).position(0.0f, 0.0f, 0.0f).setUpColor(0.5f, 1.0f, 1.0f, noiseAlpha).setDownColor(0.5f, 1.0f, 1.0f, 0.0f).setResolution(width, 80.0f).setCentered(true).end();
        matrices.m_85849_();
        float lightningAlpha = alphamod;
        matrices.m_85836_();
        matrices.m_252880_(0.0f, 30.0f, 0.0f);
        Matrix4f t = matrices.m_85850_().m_252922_();
        List<Vec3> positions = List.of(new Vec3(-150.0, 0.0, 0.0), new Vec3(150.0, 0.0, 0.0));
        ArcLightningParticle.fullLightningImmediateDraw(System.currentTimeMillis() / 50L, 42354355, 10, t, positions, 5.0f, 10.0f, 0.3f, 1.0f, 1.0f, alphamod);
        ArcLightningParticle.fullLightningImmediateDraw(System.currentTimeMillis() / 50L, 42354355, 10, t, positions, 1.5f, 10.0f, 1.0f, 1.0f, 1.0f, alphamod);
        ArcLightningParticle.fullLightningImmediateDraw(System.currentTimeMillis() / 50L, 54354355, 10, t, positions, 1.5f, 20.0f, 1.0f, 1.0f, 1.0f, alphamod);
        matrices.m_85849_();
        matrices.m_85849_();
    }
}

