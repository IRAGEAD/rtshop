package com.rtshop.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Chunky 가 이미 켜졌는지(ChunkyProvider 에 등록됐는지) 묻는다 (3.70.0).
 * Distant Horizons 3.3.3 은 세계를 열며 처음 불러오는 청크에서 Chunky 와 연결하려 하는데, Chunky 는 서버가 다 켜진 뒤
 * (ServerStartingEvent)에야 등록돼 "Chunky is not loaded" 로 게임이 튕겼다. 아직이면 DH 가 연결을 미루게 한다 (DhChunkyMixin).
 */
public final class ChunkyReady {
    private static volatile boolean looked;
    private static volatile boolean deferred;
    private static Field instance;
    private static Method get;

    private ChunkyReady() {
    }

    /** 한 번 켜진 것을 보았으면 그 뒤로는 묻지 않는다 (DH 는 청크가 바뀔 때마다 설정을 다시 부르고, 한 번 연결하면 스스로 건너뛴다). */
    private static volatile boolean seen;

    public static boolean now() {
        if (seen) return true;
        try {
            if (!looked) look();
            boolean r = false;
            if (instance != null) r = instance.get(null) != null;
            else if (get != null) {
                try {
                    r = get.invoke(null) != null;
                } catch (Throwable notYet) {
                    r = false;
                }
            }
            if (r) seen = true;
            return r;
        } catch (Throwable ignored) {
            // 모양이 바뀐 Chunky: 연결을 미룬다 (튕기는 것보다 낫다)
        }
        return false;
    }

    private static synchronized void look() {
        if (looked) return;
        Class<?> c = null;
        for (ClassLoader l : new ClassLoader[]{ChunkyReady.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
            if (l == null) continue;
            try {
                c = Class.forName("org.popcraft.chunky.ChunkyProvider", false, l);
                break;
            } catch (Throwable ignored) {
            }
        }
        if (c != null) {
            try {
                Field f = c.getDeclaredField("instance");
                if (f.trySetAccessible()) instance = f;
            } catch (Throwable ignored) {
            }
            try {
                get = c.getMethod("get");
            } catch (Throwable ignored) {
            }
        }
        looked = true;
    }

    /** DH 의 연결을 미룰 때 부른다: 처음 한 번만 로그에 남긴다 (CI 의 DH 시험이 이 줄로 막이가 붙었는지 본다). */
    public static void deferred(String where) {
        if (deferred) return;
        deferred = true;
        try {
            com.rtshop.RtShop.LOGGER.info("[RT Shop] Distant Horizons -> Chunky link deferred until Chunky is loaded ({})", where);
        } catch (Throwable ignored) {
        }
    }

    public static boolean wasDeferred() {
        return deferred;
    }

    /** 시험용: 지금 Chunky 를 찾는 방법 ("field" · "method" · "none"). */
    public static String probeForTest() {
        if (!looked) look();
        return instance != null ? "field" : get != null ? "method" : "none";
    }
}
