package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.84 (사용자 11-29 "smallship 모드로 인한 보트의 속도 저하 해결"): Small Ships 는 서버 설정 「vanillaBoatSlowdownEnable」(기본 켜짐)으로
 * 바닐라 보트의 속도 · 회전을 vanillaBoatSpeedFactor 배로 줄인다 (그 모드의 BoatSpeedMixin · BoatRotationMixin). 서버가 켜질 때 그 설정을 끈다:
 * 값은 세계의 serverconfig 에 저장되고, 들어오는 플레이어에게 그 모드가 설정을 보낼 때(SyncedServerConfig) 꺼진 값이 간다.
 * world_rules.json 의 vanillaBoatSpeed=false 면 건드리지 않는다. 그 모드의 클래스는 이름으로 찾는다 (없거나 모양이 바뀌었으면 아무것도 하지 않음).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class SmallShipsBoats {
    private SmallShipsBoats() {
    }

    public static final String MOD = "smallships";
    static final String SERVER_CFG = "com.talhanation.smallships.config.SmallShipsConfig$Server";
    /** 점검용: 마지막 결과 ("off" · "already" · "allowed" · "absent" · 그 밖 = 오류). */
    public static volatile String lastForTest = "";

    @SuppressWarnings("unchecked")
    public static ForgeConfigSpec.ConfigValue<Boolean> slowdown() {
        try {
            Object v = Class.forName(SERVER_CFG).getField("vanillaBoatSlowdownEnable").get(null);
            return v instanceof ForgeConfigSpec.ConfigValue<?> c ? (ForgeConfigSpec.ConfigValue<Boolean>) c : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String apply() {
        try {
            if (!ModList.get().isLoaded(MOD)) return lastForTest = "absent";
        } catch (Throwable t) {
            return lastForTest = "absent";
        }
        if (!com.rtshop.rules.WorldRules.get().vanillaBoatSpeed) return lastForTest = "allowed";
        try {
            ForgeConfigSpec.ConfigValue<Boolean> c = slowdown();
            if (c == null) return lastForTest = "no field";
            if (!Boolean.TRUE.equals(c.get())) return lastForTest = "already";
            c.set(false);
            try {
                c.save();
            } catch (Throwable ignored) {
                // 파일과 묶여 있지 않아도 메모리의 값은 이미 꺼졌다 (켤 때마다 다시 끈다)
            }
            RtShop.LOGGER.info("[rtshop] Small Ships 가 바닐라 보트를 느리게 하던 설정을 껐습니다. 켜려면 world_rules.json 의 vanillaBoatSpeed 를 false 로.");
            return lastForTest = "off";
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] Small Ships 의 보트 속도 설정을 바꾸지 못했습니다: {}", t.toString());
            return lastForTest = "error " + t;
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        apply();
    }
}
