package com.rtshop.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rtshop.RtShop;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bosses'Rise 의 구르기 표시(핫바 오른쪽의 초록 화살표 두 개 + 남은 횟수)를 끄고, 그 키(기본 Z)를 한 번만 풀어 준다 (3.78, compat/BossesRiseRollOff 의 화면 쪽).
 * client.json 의 hideBossesRiseRoll=false 면 건드리지 않는다. 키는 처음 한 번만 풀므로, 그 뒤에 다시 묶으면 그대로 둔다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class BossesRiseRollClient {
    private BossesRiseRollClient() {
    }

    static final String CLIENT_CFG = "net.unusual.block_factorys_bosses.configuration.ClientConfiguration";
    static final String KEYS = "net.unusual.block_factorys_bosses.init.BossesRiseKeyMappings";
    private static boolean done;
    /** 점검용: 표시를 껐는가 · 키를 풀었는가. */
    public static volatile String lastForTest = "";

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        done = true;
        try {
            lastForTest = apply(mc);
        } catch (Throwable t) {
            lastForTest = "error " + t;
        }
    }

    @SuppressWarnings("unchecked")
    static String apply(Minecraft mc) {
        if (!com.rtshop.compat.BossesRiseRollOff.loaded()) return "absent";
        ClientConfig cc = ClientConfig.get();
        if (!cc.hideBossesRiseRoll) return "kept";
        StringBuilder out = new StringBuilder();
        try {
            Object v = Class.forName(CLIENT_CFG).getField("ROLL_GUI").get(null);
            if (v instanceof ForgeConfigSpec.ConfigValue<?> c) {
                ForgeConfigSpec.ConfigValue<Boolean> b = (ForgeConfigSpec.ConfigValue<Boolean>) c;
                if (Boolean.TRUE.equals(b.get())) {
                    b.set(false);
                    try {
                        b.save();
                    } catch (Throwable ignored) {
                    }
                }
                out.append("gui ").append(b.get());
            }
        } catch (Throwable t) {
            out.append("gui ? ").append(t.getClass().getSimpleName());
        }
        if (!cc.bossesRiseKeyCleared) {
            try {
                Object k = Class.forName(KEYS).getField("DODGE_ROLL").get(null);
                if (k instanceof KeyMapping km && !km.isUnbound()) {
                    km.setKey(InputConstants.UNKNOWN);
                    KeyMapping.resetMapping();
                    mc.options.save();
                    RtShop.LOGGER.info("[rtshop] Bosses'Rise 의 구르기 키를 풀었습니다 (Z 키가 비었습니다).");
                }
                cc.bossesRiseKeyCleared = true;
                cc.save();
                out.append(" · key unbound");
            } catch (Throwable t) {
                out.append(" · key ? ").append(t.getClass().getSimpleName());
            }
        } else {
            out.append(" · key left as set");
        }
        return out.toString();
    }

    /** 점검용: 지금 그 모드의 표시 설정과 키 (없으면 null). */
    public static String stateForTest() {
        try {
            Object v = Class.forName(CLIENT_CFG).getField("ROLL_GUI").get(null);
            Object k = Class.forName(KEYS).getField("DODGE_ROLL").get(null);
            boolean gui = v instanceof ForgeConfigSpec.ConfigValue<?> c && Boolean.TRUE.equals(c.get());
            boolean bound = k instanceof KeyMapping km && !km.isUnbound();
            return "gui=" + gui + " bound=" + bound;
        } catch (Throwable t) {
            return null;
        }
    }
}
