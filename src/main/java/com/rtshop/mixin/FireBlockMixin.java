package com.rtshop.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3.82 (사용자 11-28 19:08 "마을 안에서만 번지지 않게 해줘"): 불 블록의 틱 처음에 FireGuard 를 묻고, 마을(성 영역) 안이면
 * 원래의 틱(번지기 · 옆 블록 태우기)을 건너뛴다. 이 모드는 mixin 이름표(refmap)를 만들지 않으므로 개발 환경의 이름(tick)과
 * 실제 게임의 이름(m_213897_)을 둘 다 적는다 — 있는 쪽 하나에만 붙는다.
 */
@Mixin(value = FireBlock.class, remap = false)
public abstract class FireBlockMixin {
    @Inject(method = {"tick", "m_213897_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$townFire(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        try {
            if (com.rtshop.rules.FireGuard.onFireTick(state, level, pos, random)) ci.cancel();
        } catch (Throwable ignored) {
            // 막지 못하면 원래대로 번진다 (게임은 멈추지 않게)
        }
    }
}
