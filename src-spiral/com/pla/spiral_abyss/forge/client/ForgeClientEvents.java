/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.ClientPlayerNetworkEvent$LoggingOut
 *  net.minecraftforge.client.event.RenderGuiEvent$Pre
 *  net.minecraftforge.client.event.RenderHandEvent
 *  net.minecraftforge.client.event.RenderHighlightEvent$Block
 *  net.minecraftforge.client.event.ScreenEvent$Init$Post
 *  net.minecraftforge.client.event.ScreenEvent$KeyPressed$Pre
 *  net.minecraftforge.client.event.ScreenEvent$Render$Pre
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 */
package com.pla.spiral_abyss.forge.client;

import com.pla.spiral_abyss.client.handler.JournalClientHandler;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.RareRewardOverlay;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="spiral_abyss", value={Dist.CLIENT})
public final class ForgeClientEvents {
    @SubscribeEvent
    public static void JournalClientHandler_inventory(ScreenEvent.Init.Post event) {
        AbyssClientEvents.ScreenInit shared = new AbyssClientEvents.ScreenInit(event.getScreen(), widget -> event.addListener((GuiEventListener)widget));
        JournalClientHandler.inventory(shared);
    }

    @SubscribeEvent
    public static void JournalClientHandler_renderInventory(ScreenEvent.Render.Pre event) {
        AbyssClientEvents.ScreenEvent.Render.Pre shared = new AbyssClientEvents.ScreenEvent.Render.Pre(event.getScreen());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ScreenEvent.Render.Pre)event).isCanceled(), arg_0 -> ((ScreenEvent.Render.Pre)event).setCanceled(arg_0));
        }
        JournalClientHandler.renderInventory(shared);
    }

    @SubscribeEvent
    public static void JournalClientHandler_tick(TickEvent.ClientTickEvent event) {
        AbyssClientEvents.TickEvent.ClientTickEvent shared = new AbyssClientEvents.TickEvent.ClientTickEvent(AbyssClientEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((TickEvent.ClientTickEvent)event).isCanceled(), arg_0 -> ((TickEvent.ClientTickEvent)event).setCanceled(arg_0));
        }
        JournalClientHandler.tick(shared);
    }

    @SubscribeEvent
    public static void JournalClientHandler_key(ScreenEvent.KeyPressed.Pre event) {
        AbyssClientEvents.ScreenEvent.KeyPressed.Pre shared = new AbyssClientEvents.ScreenEvent.KeyPressed.Pre(event.getScreen(), event.getKeyCode(), event.getScanCode());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ScreenEvent.KeyPressed.Pre)event).isCanceled(), arg_0 -> ((ScreenEvent.KeyPressed.Pre)event).setCanceled(arg_0));
        }
        JournalClientHandler.key(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounterOverlay_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ClientPlayerNetworkEvent.LoggingOut)event).isCanceled(), arg_0 -> ((ClientPlayerNetworkEvent.LoggingOut)event).setCanceled(arg_0));
        }
        AbyssEncounterOverlay.logout(shared);
    }

    @SubscribeEvent
    public static void RareRewardOverlay_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ClientPlayerNetworkEvent.LoggingOut)event).isCanceled(), arg_0 -> ((ClientPlayerNetworkEvent.LoggingOut)event).setCanceled(arg_0));
        }
        RareRewardOverlay.logout(shared);
    }

    @SubscribeEvent
    public static void RareRewardOverlay_tick(TickEvent.ClientTickEvent event) {
        AbyssClientEvents.TickEvent.ClientTickEvent shared = new AbyssClientEvents.TickEvent.ClientTickEvent(AbyssClientEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((TickEvent.ClientTickEvent)event).isCanceled(), arg_0 -> ((TickEvent.ClientTickEvent)event).setCanceled(arg_0));
        }
        RareRewardOverlay.tick(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ClientPlayerNetworkEvent.LoggingOut)event).isCanceled(), arg_0 -> ((ClientPlayerNetworkEvent.LoggingOut)event).setCanceled(arg_0));
        }
        PortalEntryAnimation.logout(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_hud(RenderGuiEvent.Pre event) {
        AbyssClientEvents.RenderGuiEvent.Pre shared = new AbyssClientEvents.RenderGuiEvent.Pre();
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((RenderGuiEvent.Pre)event).isCanceled(), arg_0 -> ((RenderGuiEvent.Pre)event).setCanceled(arg_0));
        }
        PortalEntryAnimation.hud(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_hand(RenderHandEvent event) {
        AbyssClientEvents.RenderHandEvent shared = new AbyssClientEvents.RenderHandEvent();
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((RenderHandEvent)event).isCanceled(), arg_0 -> ((RenderHandEvent)event).setCanceled(arg_0));
        }
        PortalEntryAnimation.hand(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_outline(RenderHighlightEvent.Block event) {
        AbyssClientEvents.RenderHighlightEvent.Block shared = new AbyssClientEvents.RenderHighlightEvent.Block(event.getTarget());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((RenderHighlightEvent.Block)event).isCanceled(), arg_0 -> ((RenderHighlightEvent.Block)event).setCanceled(arg_0));
        }
        PortalEntryAnimation.outline(shared);
    }
}

