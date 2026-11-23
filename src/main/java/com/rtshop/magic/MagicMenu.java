package com.rtshop.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 별빛 수정 구체의 '창' (작업 칸이 없는 메뉴). Cinematic Interactions 가 구체를 작업대 장면으로 다루려면
 * 블록이 메뉴를 열어야 해서 둔다. 뽑기 자체는 MagicActionPacket 으로 한다.
 */
public class MagicMenu extends AbstractContainerMenu {
    private final BlockPos pos;

    public MagicMenu(int id, Inventory inv, BlockPos pos) {
        super(MagicRegistry.MENU.get(), id);
        this.pos = pos;
        // 가방 칸: 창에는 보이지 않지만, 열려 있는 동안 가방의 변화(티켓 사용·뽑은 책·바뀐 인챈트)가 클라이언트에 전해지게 한다
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new net.minecraft.world.inventory.Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new net.minecraft.world.inventory.Slot(inv, col, 8 + col * 18, 142));
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
