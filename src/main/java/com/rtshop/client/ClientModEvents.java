package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 모드 버스 이벤트(클라이언트 전용): Gold HUD 등록. */
@Mod.EventBusSubscriber(modid = RtShop.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("gold", (gui, graphics, partialTick, screenWidth, screenHeight) ->
                GoldHud.render(graphics, screenWidth, screenHeight));
    }
}
