package com.rtshop.mixin;

import dev.diorama.waypoint.Waypoint;
import dev.diorama.waypoint.WaypointStore;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 지도 모드(Diorama)의 "세계 안 웨이포인트 표시"에서 RT Shop 이 만든 표시(폴더 「베스탈리아」: NPC · 가 본 장소)만 뺀다 (C-8).
 * 그 표시들은 M 지도에서 찾아보라고 남기는 것인데, 세계 안에도 "이름 · 거리" 이름표와 빛기둥으로 떠서
 * NPC 바로 앞에서 머리 위에 겹치고 장 제목·보스 등장 연출 위로도 보였다.
 * Diorama 에는 웨이포인트별 설정이 없어(전체를 끄는 설정뿐), 세계 안 표시를 그리는 곳(WaypointHud.render)이
 * "가까운가"를 묻는 자리에서 우리 폴더의 것만 "아니오"로 답한다. 지도 화면·웨이포인트 목록은 그대로다.
 * 사용자가 직접 만든 웨이포인트와 죽은 자리 표시는 건드리지 않는다. map_marks.json 의 showInWorld 로 되돌릴 수 있다.
 */
@Pseudo
@Mixin(targets = "dev.diorama.waypoint.WaypointHud", remap = false)
public abstract class DioramaWorldPinsMixin {
    @Redirect(method = "render", remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false,
                    target = "Ldev/diorama/waypoint/WaypointStore;isNear(Ldev/diorama/waypoint/Waypoint;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean rtshop$hideTownMarksInWorld(Waypoint waypoint, Entity viewer) {
        try {
            if (com.rtshop.client.MapMarkClient.hideInWorld(waypoint.folder)) return false;
        } catch (Throwable ignored) {
            // Diorama 의 모양이 바뀌었으면 원래대로 둔다
        }
        return WaypointStore.isNear(waypoint, viewer);
    }
}
