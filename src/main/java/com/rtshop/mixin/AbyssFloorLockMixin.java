package com.rtshop.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Spiral Abyss 의 층 모드(1~6층)는 이야기가 그 층을 부를 때만 들어갈 수 있다 (무한 모드 = 7층은 언제든).
 * 층에 들어가는 세 길(혼자·구역·따라가기)을 모두 막는다. 판단은 AbyssWeekly.floorLocked.
 */
@Pseudo
@Mixin(targets = "com.pla.spiral_abyss.world.AbyssParties", remap = false)
public abstract class AbyssFloorLockMixin {
    @Inject(method = "enterFloor", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$lockFloor(ServerPlayer player, int number, CallbackInfoReturnable<Boolean> cir) {
        if (com.rtshop.raid.AbyssWeekly.refuseFloor(player, number)) cir.setReturnValue(false);
    }

    @Inject(method = "followFloor", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$lockFollow(ServerPlayer player, int number, CallbackInfoReturnable<Boolean> cir) {
        if (com.rtshop.raid.AbyssWeekly.refuseFloor(player, number)) cir.setReturnValue(false);
    }

    @Inject(method = "enterSection", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$lockSection(ServerPlayer player, UUID portal, int floor, CallbackInfoReturnable<Boolean> cir) {
        if (com.rtshop.raid.AbyssWeekly.refuseFloor(player, floor)) cir.setReturnValue(false);
    }
}
