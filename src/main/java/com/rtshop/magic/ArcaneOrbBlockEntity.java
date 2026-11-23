package com.rtshop.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 별빛 수정 구체의 블록 엔티티 (저장할 것은 없고, 구슬·고리를 그리기 위해 있다). */
public class ArcaneOrbBlockEntity extends BlockEntity {
    public ArcaneOrbBlockEntity(BlockPos pos, BlockState state) {
        super(MagicRegistry.ORB_BE.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.5, 1.2, 0.5);
    }
}
