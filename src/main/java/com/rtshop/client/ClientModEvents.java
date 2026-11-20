package com.rtshop.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rtshop.RtShop;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 모드 버스 이벤트(클라이언트 전용): Gold HUD 와 단축키 등록. */
@Mod.EventBusSubscriber(modid = RtShop.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    // 다른 모드의 단축키와 겹치지 않도록 기본값은 "지정 안 됨". 조작 설정에서 원하는 키를 넣으면 된다.
    public static final KeyMapping HUD_KEY = new KeyMapping("key.rtshop.hud", InputConstants.UNKNOWN.getValue(), "key.categories.rtshop");
    public static final KeyMapping LAND_KEY = new KeyMapping("key.rtshop.land", InputConstants.UNKNOWN.getValue(), "key.categories.rtshop");

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("gold", (gui, graphics, partialTick, screenWidth, screenHeight) ->
                GoldHud.render(graphics, screenWidth, screenHeight));
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(HUD_KEY);
        event.register(LAND_KEY);
    }
}
