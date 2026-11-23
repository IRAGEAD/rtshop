package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public final class Scene {
    public static Phase phase;
    public static ClientStation station;
    public static double time;
    public static double frameDt;

    private Scene() {
    }

    public static double clock() {
        return 0;
    }

    public static boolean active() {
        return false;
    }

    public static double introProgress() {
        return 0;
    }

    public static void begin(BlockPos pos, StationKind kind) {
    }

    public static void leave() {
    }

    public enum Phase {
        NONE, INTRO, ACTIVE, OUTRO
    }
}
