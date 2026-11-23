package dev.dyslexic.cinematic.client;

import net.minecraft.world.phys.Vec3;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public final class Picking {
    private Picking() {
    }

    public static Vec3 camera() {
        return Vec3.ZERO;
    }

    public static double[] mouseGui() {
        return new double[2];
    }

    public static double[] toGui(Vec3 world) {
        return null;
    }
}
