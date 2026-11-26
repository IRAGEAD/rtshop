package com.rtshop.mixin;

import dev.diorama.render.Radar;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 지도 모드(Diorama)의 미니맵·지도에 뜨는 몹 아이콘(레이더)에서 RT Shop NPC 를 고른다 (3.58):
 * 한 번 이상 말을 걸어 본 NPC 만 보이고, /rtshop hud 의 「미니맵 NPC 아이콘」 으로 모두 끌 수 있다.
 * 레이더는 몹을 모을 때(Radar.collect) 몹마다 종류(Radar.kind)를 묻고, 종류가 없으면(null) 그리지 않는다.
 * 그 물음에서 감출 NPC 만 "없음"으로 답한다. 다른 몹과 플레이어는 건드리지 않는다.
 */
@Pseudo
@Mixin(targets = "dev.diorama.render.Radar", remap = false)
public abstract class DioramaRadarMixin {
    @Redirect(method = "collect", remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false,
                    target = "Ldev/diorama/render/Radar;kind(Lnet/minecraft/world/entity/Entity;)Ldev/diorama/render/Radar$Kind;"))
    private static Radar.Kind rtshop$hideUnmetNpcs(Entity entity) {
        try {
            if (com.rtshop.client.MapMarkClient.hideOnRadar(entity)) return null;
        } catch (Throwable ignored) {
            // Diorama 의 모양이 바뀌었으면 원래대로 둔다
        }
        return Radar.kind(entity);
    }
}
