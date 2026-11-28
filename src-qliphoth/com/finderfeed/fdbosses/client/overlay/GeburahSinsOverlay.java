/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.entity.player.Player
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.gui.overlay.ForgeGui
 *  net.minecraftforge.client.gui.overlay.IGuiOverlay
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.finderfeed.fdbosses.client.overlay;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE, value={Dist.CLIENT})
public class GeburahSinsOverlay
implements IGuiOverlay {
    public static final ResourceLocation SINS = FDBosses.location("textures/gui/geburah_sin.png");
    private static int ticker = 0;
    private static int oldSinCount = 0;

    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.m_91087_().f_91074_ == null || Minecraft.m_91087_().f_91066_.f_92062_ || !Minecraft.m_91087_().f_91074_.m_21023_((MobEffect)BossEffects.SINNER.get())) {
            return;
        }
        float pticks = partialTick;
        Window window = Minecraft.m_91087_().m_91268_();
        float width = window.m_85445_();
        float height = window.m_85446_();
        PlayerSins playerSins = PlayerSins.getPlayerSins((Player)Minecraft.m_91087_().f_91074_);
        int maxSins = BossConfigs.BOSS_CONFIG.get().geburahConfig.maxPlayerSins;
        int sinnedTimes = playerSins.getSinnedTimes();
        float h = height / 2.0f;
        float sinHeight = 25.0f;
        float xOffset = 2.5f;
        if (ticker < 20) {
            float p = Mth.m_14036_((float)((float)ticker - pticks), (float)0.0f, (float)2.1474836E9f) / 20.0f;
            xOffset -= FDEasings.easeInOut((float)(1.0f - p)) * 32.0f;
        }
        float startOffset = maxSins % 2 == 0 ? sinHeight * (float)(maxSins / 2) : sinHeight / 2.0f + sinHeight * (float)(maxSins / 2);
        for (int i = 0; i < maxSins; ++i) {
            float texOffset = i < sinnedTimes ? 0.5f : 0.0f;
            FDRenderUtil.bindTexture((ResourceLocation)SINS);
            FDRenderUtil.blitWithBlend((PoseStack)guiGraphics.m_280168_(), (float)xOffset, (float)(h - startOffset + (float)i * sinHeight), (float)28.0f, (float)33.0f, (float)0.0f, (float)texOffset, (float)1.0f, (float)0.5f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        }
    }

    @SubscribeEvent
    public static void tickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (Minecraft.m_91087_().f_91074_ == null) {
            ticker = 0;
            oldSinCount = 0;
            return;
        }
        PlayerSins playerSins = PlayerSins.getPlayerSins((Player)Minecraft.m_91087_().f_91074_);
        int sinnedTimes = playerSins.getSinnedTimes();
        if (sinnedTimes != oldSinCount) {
            oldSinCount = sinnedTimes;
            ticker = 100;
        }
        ticker = Mth.m_14045_((int)(ticker - 1), (int)0, (int)Integer.MAX_VALUE);
    }
}

