package com.rtshop.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 별빛 수정 구체의 '창' (칸이 없는 빈 메뉴). Cinematic Interactions 가 구체를 작업대 장면으로 다루려면
 * 블록이 메뉴를 열어야 해서 둔다. 뽑기 자체는 MagicActionPacket 으로 한다.
 */
public class MagicMenu extends AbstractContainerMenu {
    private final BlockPos pos;

    public MagicMenu(int id, Inventory inv, BlockPos pos) {
        super(MagicRegistry.MENU.get(), id);
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pos == null) return true; // 클라이언트 쪽
        return player.level().getBlockState(pos).getBlock() instanceof ArcaneOrbBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MagicGacha.RANGE * MagicGacha.RANGE;
    }
}
