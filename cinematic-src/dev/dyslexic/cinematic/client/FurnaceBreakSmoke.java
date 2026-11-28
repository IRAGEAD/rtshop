/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class FurnaceBreakSmoke {
    private static final String ROLE = System.getProperty("cinematic.furnaceBreakRole", "");
    private static final BlockPos FURNACE = new BlockPos(2, -60, 0);
    private static int ticks;
    private static boolean started;
    private static boolean broken;

    private FurnaceBreakSmoke() {
    }

    static void tick() {
        if (ROLE.isEmpty() || broken && ROLE.equals("breaker")) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null || mc.f_91072_ == null) {
            return;
        }
        if (++ticks > 2400) {
            Cinematic.LOGGER.error("[FurnaceBreakSmoke] FAIL: timeout role={} phase={}", (Object)ROLE, (Object)Scene.phase);
            mc.m_91395_();
            return;
        }
        if (ROLE.equals("worker")) {
            if (ticks % 100 == 0 && !started) {
                Cinematic.LOGGER.info("[FurnaceBreakSmoke] waiting at={} block={}", (Object)mc.f_91074_.m_20182_(), (Object)mc.f_91073_.m_8055_(FURNACE).m_60734_());
            }
            if (!started && StationKind.FURNACE.matches(mc.f_91073_.m_8055_(FURNACE)) && mc.f_91074_.m_20182_().f_82481_ > 2.0 && mc.f_91074_.m_20096_() && mc.f_91080_ == null) {
                Scene.begin(FURNACE, StationKind.FURNACE);
                started = true;
                Cinematic.LOGGER.info("[FurnaceBreakSmoke] worker used furnace phase={} grounded={} screen={} refused={}", new Object[]{Scene.phase, mc.f_91074_.m_20096_(), mc.f_91080_, Scene.refused});
            }
            if (started && ticks % 100 == 0 && Scene.phase != Scene.Phase.ACTIVE) {
                Cinematic.LOGGER.info("[FurnaceBreakSmoke] worker phase={} block={}", (Object)Scene.phase, (Object)mc.f_91073_.m_8055_(FURNACE).m_60734_());
            }
            if (started && Scene.phase == Scene.Phase.ACTIVE && StationKind.FURNACE.matches(mc.f_91073_.m_8055_(FURNACE)) && ticks % 100 == 0) {
                Cinematic.LOGGER.info("[FurnaceBreakSmoke] worker ready");
            }
            if (started && !StationKind.FURNACE.matches(mc.f_91073_.m_8055_(FURNACE)) && (Scene.phase == Scene.Phase.NONE || Scene.phase == Scene.Phase.OUTRO)) {
                Cinematic.LOGGER.info("[FurnaceBreakSmoke] PASS: worker scene closed after furnace was broken");
                broken = true;
                mc.m_91395_();
            }
        } else if (ROLE.equals("breaker") && started && !StationKind.FURNACE.matches(mc.f_91073_.m_8055_(FURNACE))) {
            Cinematic.LOGGER.info("[FurnaceBreakSmoke] breaker removed furnace");
            broken = true;
            mc.m_91395_();
        } else if (ROLE.equals("breaker") && !started && mc.f_91074_.m_20182_().f_82479_ > 1.5 && mc.f_91074_.m_20182_().f_82479_ < 3.5 && mc.f_91074_.m_20182_().f_82481_ > 2.0 && StationKind.FURNACE.matches(mc.f_91073_.m_8055_(FURNACE)) && mc.f_91072_.m_105269_(FURNACE, Direction.SOUTH)) {
            Cinematic.LOGGER.info("[FurnaceBreakSmoke] breaker attacked furnace");
            started = true;
        }
    }
}

