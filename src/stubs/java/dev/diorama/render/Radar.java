package dev.diorama.render;

import net.minecraft.world.entity.Entity;

/** 컴파일용 껍데기 (Diorama 0.11 의 미니맵 몹 표시). 실제 클래스는 그 모드의 것을 쓴다. */
public final class Radar {
    public enum Kind {
        ITEM, PASSIVE, NEUTRAL, HOSTILE, PLAYER
    }

    public static Kind kind(Entity entity) {
        return null;
    }
}
