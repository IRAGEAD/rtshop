package dev.diorama.waypoint;

import net.minecraft.world.entity.Entity;

/** 컴파일용 껍데기 (Diorama 0.11 의 웨이포인트 저장소). 실제 클래스는 그 모드의 것을 쓴다. */
public final class WaypointStore {
    public static boolean isNear(Waypoint waypoint, Entity viewer) {
        return true;
    }
}
