package dev.dyslexic.cinematic.client;

import net.minecraft.world.phys.Vec3;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public final class CameraRig {
    private CameraRig() {
    }

    public record Pose(Vec3 pos, float yaw, float pitch, float roll) {
    }
}
