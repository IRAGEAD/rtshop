/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDShaderRenderer
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.util.Mth
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.gui.overlay.ForgeGui
 *  net.minecraftforge.client.gui.overlay.IGuiOverlay
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.finderfeed.fdbosses.content.items.chesed;

import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossCoreShaders;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDShaderRenderer;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="fdbosses", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class PhaseSphereOverlay
implements IGuiOverlay {
    private static Boolean smartCull;
    public static final int MAX_PROGRESS = 20;
    private static int progress;
    private static int progressO;

    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.m_91087_().f_91073_ == null || progress == 0) {
            return;
        }
        ShaderInstance shader = BossCoreShaders.CHESED_ITEM_OVERLAY;
        Window window = Minecraft.m_91087_().m_91268_();
        float width = window.m_85445_();
        float height = window.m_85446_();
        float pticks = partialTick;
        float gametime = Minecraft.m_91087_().f_91073_.m_46467_() % 2000000L;
        float time = gametime + pticks;
        float progress = FDMathUtil.lerp((float)progressO, (float)PhaseSphereOverlay.progress, (float)pticks) / 20.0f;
        if (BossConfigs.BOSS_CONFIG_CLIENT.get().phaseSphereVisualEffect) {
            FDShaderRenderer.start((GuiGraphics)guiGraphics, (ShaderInstance)shader).setShaderUniform("progress", new float[]{FDEasings.easeOut((float)progress)}).setShaderUniform("size", new float[]{width, height}).setShaderUniform("time", new float[]{time / 200.0f}).setResolution(width, height).end();
        }
        RenderSystem.defaultBlendFunc();
    }

    @SubscribeEvent
    public static void tickClient(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (progress == 0) {
            if (smartCull != null) {
                Minecraft.m_91087_().f_90980_ = smartCull;
                smartCull = null;
            }
        } else {
            if (smartCull == null) {
                smartCull = Minecraft.m_91087_().f_90980_;
            }
            Minecraft.m_91087_().f_90980_ = false;
        }
        if (player == null) {
            progress = 0;
            return;
        }
        progressO = progress;
        progress = player.m_21211_().m_150930_(BossItems.PHASE_SPHERE.get()) ? Mth.m_14045_((int)(progress + 1), (int)0, (int)20) : Mth.m_14045_((int)(progress - 1), (int)0, (int)20);
    }
}

