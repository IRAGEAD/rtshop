package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.84 (사용자 11-29 "배고픔 조정(허기를 기본 마크처럼 돌려놓고 배고픔이 차는 수치만 줄이기)"): 배고픔이 바닐라보다 빨리 닳게 하던 것 두 가지를 서버가 켜질 때 끈다.
 * <ul>
 *   <li>Kaleidoscope Cookery 의 「포만 보호막(Satiated Shield)」: 그 모드의 음식을 먹으면 붙는 효과로, 맞을 때 피해를 거의 다 막는 대신 배고픔이 닳는다
 *       (모드팩 설정: 피해 100% 를 64 까지 막고, 피해 1 마다 소모 2.0 = 배고픔 반 칸). 바닐라처럼 맞으면 체력이 닳고 배고픔은 그대로가 되게
 *       그 모드의 공통 설정 cookery.SatiatedShieldAbsorbEnabled 를 false 로 (파일에도 저장).</li>
 *   <li>Spell Engine(RPG 주문)의 주문 배고픔 소모: 서버 설정 spell_cost_exhaust_multiplier 를 0 으로 (그 모드가 켜질 때 읽은 값 · 메모리만, 켤 때마다 다시).</li>
 * </ul>
 * world_rules.json 의 vanillaHunger=false 면 건드리지 않는다. 두 모드 모두 이름으로만 찾는다 (없거나 모양이 바뀌었으면 아무것도 하지 않음).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class HungerVanilla {
    private HungerVanilla() {
    }

    public static final String COOKERY = "kaleidoscope_cookery";
    public static final List<String> SHIELD_PATH = List.of("cookery", "SatiatedShieldAbsorbEnabled");
    /** 점검용: 마지막 결과. */
    public static volatile String lastForTest = "";

    public static String apply() {
        if (!com.rtshop.rules.WorldRules.get().vanillaHunger) return lastForTest = "allowed";
        List<String> out = new ArrayList<>();
        out.add("shield " + shieldOff());
        out.add("spell " + SpellEngineTweaks.exhaustOff());
        lastForTest = String.join(", ", out);
        RtShop.LOGGER.info("[rtshop] 배고픔을 바닐라처럼: {}", lastForTest);
        return lastForTest;
    }

    /** Kaleidoscope Cookery 의 포만 보호막을 끈다. */
    public static String shieldOff() {
        try {
            if (!ModList.get().isLoaded(COOKERY)) return "absent";
            for (ModConfig cfg : ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON)) {
                if (!cfg.getModId().equals(COOKERY) || !(cfg.getSpec() instanceof ForgeConfigSpec spec)) continue;
                Object v = spec.getValues().get(SHIELD_PATH);
                if (!(v instanceof ForgeConfigSpec.BooleanValue b)) continue;
                if (!b.get()) return "already off";
                b.set(false);
                try {
                    cfg.save();
                } catch (Throwable ignored) {
                    // 파일에 쓰지 못해도 메모리의 값은 이미 꺼졌다 (켤 때마다 다시 끈다)
                }
                return "off";
            }
            return "no setting";
        } catch (Throwable t) {
            return "error " + t;
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            apply();
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 배고픔 설정을 바꾸지 못했습니다: {}", t.toString());
        }
    }
}
