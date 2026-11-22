package com.rtshop.util;

import com.rtshop.RtShop;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 서버 틱 기준의 작은 예약기: "N틱 뒤에 이것을 해라". 서버가 꺼지면 남은 일은 버린다. */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Later {
    private Later() {
    }

    private record Job(long due, Runnable run) {
    }

    private static final List<Job> JOBS = new ArrayList<>();
    private static long now;

    public static synchronized void run(int delayTicks, Runnable r) {
        JOBS.add(new Job(now + Math.max(0, delayTicks), r));
    }

    /** 시험용: 밀린 일을 지금 모두 한다. */
    public static void flushForTest() {
        List<Job> due;
        synchronized (Later.class) {
            due = new ArrayList<>(JOBS);
            JOBS.clear();
        }
        for (Job j : due) j.run.run();
    }

    public static synchronized int pendingForTest() {
        return JOBS.size();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = e.getServer();
        if (server == null) return;
        List<Job> due = null;
        synchronized (Later.class) {
            now++;
            for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
                Job j = it.next();
                if (j.due <= now) {
                    if (due == null) due = new ArrayList<>();
                    due.add(j);
                    it.remove();
                }
            }
        }
        if (due != null) {
            for (Job j : due) {
                try {
                    j.run.run();
                } catch (Exception ex) {
                    RtShop.LOGGER.error("[rtshop] 예약된 일을 하지 못했습니다", ex);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        synchronized (Later.class) {
            JOBS.clear();
        }
    }
}
