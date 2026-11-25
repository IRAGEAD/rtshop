package com.rtshop.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 룬 제단의 창. 0번 칸은 제단 위 (인챈트를 다시 붙일 무기·장비를 올려놓는 자리), 그 뒤는 가방 칸이다.
 * Cinematic Interactions 가 제단을 작업대 장면으로 다루려면 블록이 메뉴를 열어야 해서, 제단은 언제나 이 메뉴로 열린다.
 * 뽑기와 재설정 자체는 MagicActionPacket 으로 한다. 창을 닫으면 올려 둔 장비는 가방으로 돌아온다.
 */
public class MagicMenu extends AbstractContainerMenu {
    public static final int ALTAR = 0;
    private final BlockPos pos;
    private final Player player;
    private final SimpleContainer altar = new SimpleContainer(1);

    public MagicMenu(int id, Inventory inv, BlockPos pos) {
        super(MagicRegistry.MENU.get(), id);
        this.pos = pos;
        this.player = inv.player;
        addSlot(new Slot(altar, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack s) {
                // 클라이언트는 서버의 목록을 모르므로 느슨하게 보고, 서버가 정확히 가린다
                if (player.level().isClientSide) return !s.isEmpty() && s.getMaxStackSize() == 1;
                return MagicGacha.rerollable(MagicConfig.get(), s);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (player instanceof ServerPlayer sp) MagicGacha.refresh(sp); // 올린 장비에 맞는 확률을 다시 보낸다
            }
        });
        // 가방 칸: 열려 있는 동안 가방의 변화(티켓 사용·뽑은 책)가 클라이언트에 전해진다
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    public BlockPos pos() {
        return pos;
    }

    /** 제단 위에 올려 둔 장비 (없으면 빈 것). */
    public ItemStack altarItem() {
        return altar.getItem(0);
    }

    /** 가방 칸(0~35)에 해당하는 이 메뉴의 칸 번호. */
    public static int slotOfInventory(int inventoryIndex) {
        return inventoryIndex < 9 ? 28 + inventoryIndex : inventoryIndex - 9 + 1;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack in = slot.getItem();
        ItemStack copy = in.copy();
        if (index == ALTAR) {
            if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            Slot a = slots.get(ALTAR);
            if (a.hasItem() || !a.mayPlace(in)) return ItemStack.EMPTY;
            a.set(in.split(1));
        }
        if (in.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (in.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, in);
        return copy;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!p.level().isClientSide) clearContainer(p, altar); // 올려 둔 장비는 가방으로
    }

    @Override
    public boolean stillValid(Player p) {
        if (pos == null) return true; // 클라이언트 쪽
        return p.level().getBlockState(pos).getBlock() instanceof ArcaneOrbBlock
                && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MagicGacha.RANGE * MagicGacha.RANGE;
    }
}
