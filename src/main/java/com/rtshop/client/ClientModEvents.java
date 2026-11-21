package com.rtshop.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rtshop.RtShop;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** 모드 버스 이벤트(클라이언트 전용): Gold HUD 와 단축키 등록. */
@Mod.EventBusSubscriber(modid = RtShop.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    // 다른 모드의 단축키와 겹치지 않도록 기본값은 "지정 안 됨". 조작 설정에서 원하는 키를 넣으면 된다.
    public static final KeyMapping HUD_KEY = new KeyMapping("key.rtshop.hud", InputConstants.UNKNOWN.getValue(), "key.categories.rtshop");
    public static final KeyMapping LAND_KEY = new KeyMapping("key.rtshop.land", InputConstants.UNKNOWN.getValue(), "key.categories.rtshop");

    public static final KeyMapping JOB_KEY = new KeyMapping("key.rtshop.job", InputConstants.UNKNOWN.getValue(), "key.categories.rtshop");
    public static final KeyMapping QUEST_KEY = new KeyMapping("key.rtshop.quest", InputConstants.UNKNOWN.getValue(), "key.categories.rtshop");

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("gold", (gui, graphics, partialTick, screenWidth, screenHeight) ->
                GoldHud.render(graphics, screenWidth, screenHeight));
        event.registerAboveAll("quests", (gui, graphics, partialTick, screenWidth, screenHeight) ->
                QuestHud.render(graphics, screenWidth, screenHeight));
    }

    /** 별 등급과 '심기 전용' 표시를 모든 아이템 아이콘 위에 그릴 수 있게 등록한다 (표시할 것이 없으면 아무것도 그리지 않음). */
    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : ForgeRegistries.ITEMS) {
            event.register(item, ItemMarks::render);
        }
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(HUD_KEY);
        event.register(LAND_KEY);
        event.register(QUEST_KEY);
        event.register(JOB_KEY);
    }
}
