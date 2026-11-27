package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Bosses'Rise(block_factorys_bosses) 의 「Dodge Roll」(구르기 · 대시, 기본 Z 키, 핫바 오른쪽의 초록 화살표 두 개와 남은 횟수)을 끈다.
 * 3.78 (사용자 11-28 08:31 "없애는 것이 낫겠어"): 이 서버의 전투는 Epic Fight 의 회피기로 하고, 두 회피가 겹치지 않게.
 * <ul>
 *   <li>서버: 켜질 때 그 모드의 서버 설정 「Can use roll」(ServerConfiguration.CAN_USE_ROLL)을 false 로 (세계의 serverconfig 에 저장).
 *       서버가 구르기 요청(DodgeRollMessage)을 받을 때 이 값을 본다. world_rules.json 의 bossesRiseRoll=true 면 건드리지 않는다.</li>
 *   <li>화면 쪽(client/BossesRiseRollClient): 그 표시(ClientConfiguration.ROLL_GUI)를 끄고, Z 키 묶음을 한 번만 풀어 준다.</li>
 * </ul>
 * 그 모드의 클래스는 이름으로 찾는다 (없거나 모양이 바뀌었으면 아무것도 하지 않음).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class BossesRiseRollOff {
    private BossesRiseRollOff() {
    }

    public static final String MOD = "block_factorys_bosses";
    static final String SERVER_CFG = "net.unusual.block_factorys_bosses.configuration.ServerConfiguration";
    /** 점검용: 마지막 결과 ("off" = 껐음, "already" = 이미 꺼져 있음, "allowed" = 설정이 허용, "absent" = 모드 없음, 그 밖 = 오류). */
    public static volatile String lastForTest = "";

    public static boolean loaded() {
        try {
            return ModList.get().isLoaded(MOD);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 그 모드의 「Can use roll」 값 (없으면 null). */
    @SuppressWarnings("unchecked")
    public static ForgeConfigSpec.ConfigValue<Boolean> canUseRoll() {
        try {
            Object v = Class.forName(SERVER_CFG).getField("CAN_USE_ROLL").get(null);
            return v instanceof ForgeConfigSpec.ConfigValue<?> c ? (ForgeConfigSpec.ConfigValue<Boolean>) c : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 서버 설정의 구르기를 끈다 (world_rules 가 허용하면 그대로). 결과 글. */
    public static String apply() {
        if (!loaded()) return lastForTest = "absent";
        if (com.rtshop.rules.WorldRules.get().bossesRiseRoll) return lastForTest = "allowed";
        try {
            ForgeConfigSpec.ConfigValue<Boolean> c = canUseRoll();
            if (c == null) return lastForTest = "no field";
            if (!Boolean.TRUE.equals(c.get())) return lastForTest = "already";
            c.set(false);
            try {
                c.save();
            } catch (Throwable ignored) {
                // 저장은 그 설정이 파일과 묶여 있을 때만: 메모리의 값은 이미 꺼졌다
            }
            RtShop.LOGGER.info("[rtshop] Bosses'Rise 의 구르기(Dodge Roll, Z 키)를 껐습니다. 켜려면 world_rules.json 의 bossesRiseRoll 을 true 로.");
            return lastForTest = "off";
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] Bosses'Rise 의 구르기를 끄지 못했습니다: {}", t.toString());
            return lastForTest = "error " + t;
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        apply();
    }
}
