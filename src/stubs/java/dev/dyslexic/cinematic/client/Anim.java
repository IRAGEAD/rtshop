package dev.dyslexic.cinematic.client;

import net.minecraft.world.phys.Vec3;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public final class Anim {
    private Anim() {
    }

    public static double clamp01(double t) {
        return t;
    }

    public static double window(double t, double a, double b) {
        return t;
    }

    public static double smoother(double t) {
        return t;
    }

    public static double inOutCubic(double t) {
        return t;
    }

    public static double outCubic(double t) {
        return t;
    }

    public static double outBack(double t, double overshoot) {
        return t;
    }

    public static double hump(double t) {
        return t;
    }

    public static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a;
    }

    public static double approach(double current, double target, double rate, double dt) {
        return target;
    }
}
