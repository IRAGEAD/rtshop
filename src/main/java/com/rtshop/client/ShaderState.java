package com.rtshop.client;

/**
 * 셰이더 팩이 켜져 있는가 (Oculus / Iris 의 공개 API 를 리플렉션으로, 3.64.2).
 * 셰이더는 화면의 빛을 스스로 계산해서, 빛을 받지 않게 그린 그림(FULL_BRIGHT)도 그늘에서는 어둡게 칠한다.
 * 스스로 빛나야 하는 것(룬 제단의 구슬)은 셰이더가 켜졌을 때만 더하기 섞기 층(RenderType.eyes = 셰이더의 발광 단계)을 하나 더 그린다.
 * 셰이더가 없으면 지금 모습 그대로 둔다. 1초에 한 번만 묻는다.
 */
public final class ShaderState {
    private ShaderState() {
    }

    private static long checkedAt = -1L;
    private static boolean on;
    private static boolean broken;
    /** 점검용: 셰이더가 켜졌다고 치기 (null 이면 실제 값). */
    public static volatile Boolean forceForTest;

    public static boolean on() {
        Boolean f = forceForTest;
        if (f != null) return f;
        if (broken) return false;
        long now = System.currentTimeMillis();
        if (checkedAt >= 0 && now - checkedAt < 1000L) return on;
        checkedAt = now;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            on = (Boolean) api.getMethod("isShaderPackInUse").invoke(inst);
        } catch (Throwable t) {
            broken = true;   // Oculus 가 없다
            on = false;
        }
        return on;
    }
}
