package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.70.0 (사용자 요청: "distant horizon을 사용했을 때 먼 거리는 보이지만 가까운 청크들이 보이지 않는 문제").
 * Distant Horizons 는 내 렌더 거리 바깥부터 먼 풍경(LOD)을 그린다. 서버가 보내 주는 청크(서버 시야 거리)가 그보다 좁으면
 * 그 사이의 고리 모양 구역에는 진짜 청크도 LOD 도 없어 비어 보인다. 바닐라는 이미 서버 시야 거리까지만 그리므로(유효 렌더 거리),
 * 서버에 들어가 있는 동안 렌더 거리 설정 값을 유효 렌더 거리로 낮춰 두 값을 같게 한다 → DH 가 그 바로 바깥부터 LOD 를 채운다.
 * 나가면 원래 값으로 되돌린다. 싱글플레이는 내장 서버가 내 렌더 거리를 그대로 쓰므로 해당 없음. client.json 의 dhMatchServer 로 끈다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class DhViewGuard {
    private DhViewGuard() {
    }

    /** 원래 렌더 거리 (낮춰 두었을 때만, 아니면 -1). */
    private static int saved = -1;
    /**
     * RT Shop 서버가 알려 준 서버 시야 거리 (지도 표시 패킷에 실려 옴, 모르면 -1).
     * Farsight 처럼 서버보다 멀리 그리게 하는 모드는 바닐라의 '유효 렌더 거리'를 내 렌더 거리로 늘려 버려서, 그 값만으로는 빈 고리를 막지 못한다.
     */
    public static volatile int serverView = -1;
    private static boolean told;
    /** 점검용: 마지막으로 맞춘 렌더 거리. */
    public static volatile int clampedForTest = -1;

    public static boolean active() {
        return ModList.get().isLoaded("distanthorizons") && ClientConfig.get().dhMatchServer;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.hasSingleplayerServer()) return;
        if (!active()) {
            restore(mc);
            return;
        }
        int raw = mc.options.renderDistance().get(), eff = serverView >= 2 ? Math.min(serverView, mc.options.getEffectiveRenderDistance()) : mc.options.getEffectiveRenderDistance();
        if (eff >= 2 && eff < raw) {
            if (saved < 0) saved = raw;
            mc.options.renderDistance().set(eff);
            clampedForTest = eff;
            if (!told) {
                told = true;
                mc.gui.getChat().addMessage(Component.translatable("rtshop.dh.matched", eff, saved, eff).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            RtShop.LOGGER.info("[rtshop] Distant Horizons: render distance {} -> {} (the server sends {} chunks)", raw, eff, eff);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restore(Minecraft.getInstance());
        told = false;
        serverView = -1;
    }

    private static void restore(Minecraft mc) {
        if (saved < 0) return;
        mc.options.renderDistance().set(saved);
        mc.options.save();
        saved = -1;
    }
}
