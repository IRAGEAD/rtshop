package com.rtshop.client;

import com.google.gson.JsonObject;
import com.rtshop.RtShop;
import com.rtshop.magic.CinematicCompat;
import com.rtshop.net.MagicActionPacket;
import com.rtshop.net.MagicPacket;
import com.rtshop.net.ShopNet;
import dev.dyslexic.cinematic.api.client.AddonScenes;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Cinematic Interactions 연동 (클라이언트): 별빛 수정 구체의 3D 장면을 붙이고, 서버 답을 장면으로 넘긴다.
 * Cinematic 이 깔려 있을 때만 부른다 (MagicRegistry.cinematic).
 */
public final class CinematicOrb {
    private CinematicOrb() {
    }

    /** 장면에서 물러난 뒤에 보낼 동작 (티켓 상점·편집기는 장면이 끝난 다음에 연다). */
    static int afterLeave = -1;
    /** 장면에서 물러난 뒤에 이쪽(클라이언트)에서 할 일 (화면 설정 창). */
    static Runnable afterLeaveClient;
    private static java.lang.reflect.Method tap;

    public static void register() {
        try {
            AddonScenes.register(CinematicCompat.ID, (pos, state, player, side) -> new CinematicOrbStation(pos, side));
        } catch (Throwable t) {
            com.rtshop.magic.MagicRegistry.cinematic = false;
            RtShop.LOGGER.warn("[rtshop] Cinematic Interactions 장면 붙이기 실패: {}", t.toString());
        }
    }

    public static CinematicOrbStation station() {
        try {
            return Scene.active() && Scene.station instanceof CinematicOrbStation s ? s : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean sceneActive() {
        return station() != null;
    }

    /** 서버가 보낸 창 내용·결과를 장면으로. 장면이 없으면 false. */
    public static boolean route(int kind, JsonObject o) {
        CinematicOrbStation s = station();
        if (s == null) return false;
        if (kind == MagicPacket.VIEW) s.update(o, false);
        else if (kind == MagicPacket.RESULT) s.update(o, true);
        else return false;
        return true;
    }

    public static boolean result(Component msg, boolean ok) {
        CinematicOrbStation s = station();
        if (s == null) return false;
        s.notice(msg, ok);
        return true;
    }

    public static void tick() {
        if (afterLeave >= 0 && !Scene.active()) {
            ShopNet.CHANNEL.sendToServer(new MagicActionPacket(afterLeave, ""));
            afterLeave = -1;
        }
        if (afterLeaveClient != null && !Scene.active()) {
            Runnable r = afterLeaveClient;
            afterLeaveClient = null;
            r.run();
        }
    }

    /** 손을 한 번 까딱인다 (Cinematic 의 손 연출; 공개 API 가 아니라서 실패해도 조용히 넘어간다). */
    static void tap() {
        try {
            if (tap == null) {
                tap = Class.forName("dev.dyslexic.cinematic.client.Hands").getDeclaredMethod("tap");
                tap.setAccessible(true);
            }
            tap.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 점검용

    public static void beginForTest(BlockPos pos) {
        Scene.begin(pos, StationKind.ADDON);
    }

    public static void leaveForTest() {
        Scene.leave();
    }

    public static String phaseForTest() {
        return String.valueOf(Scene.phase);
    }
}
