package com.rtshop.pshop;

import com.rtshop.enhance.ModEnhance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 플레이어 상점 진열대의 저장 내용: 주인, 상점 이름, 품목(견본·개당 가격·재고). 재고 아이템은 진열대 안에 보관된다. */
public class PlayerShopBlockEntity extends BlockEntity {
    public static final int MAX_LISTINGS = 27;
    public static final int MAX_STOCK = 9999;

    public static final class Listing {
        public ItemStack item = ItemStack.EMPTY; // 개수 1 견본
        public long price;
        public int stock;
        public long sold;
    }

    public UUID owner = new UUID(0, 0);
    public String ownerName = "";
    public String name = "";
    public final List<Listing> listings = new ArrayList<>();
    public long totalSales;

    public PlayerShopBlockEntity(BlockPos pos, BlockState state) {
        super(ModEnhance.PSHOP_BE.get(), pos, state);
    }

    public boolean hasOwner() {
        return owner.getMostSignificantBits() != 0 || owner.getLeastSignificantBits() != 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("owner", owner);
        tag.putString("ownerName", ownerName);
        tag.putString("name", name);
        tag.putLong("totalSales", totalSales);
        ListTag list = new ListTag();
        for (Listing l : listings) {
            CompoundTag t = new CompoundTag();
            t.put("item", l.item.save(new CompoundTag()));
            t.putLong("price", l.price);
            t.putInt("stock", l.stock);
            t.putLong("sold", l.sold);
            list.add(t);
        }
        tag.put("listings", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("owner")) owner = tag.getUUID("owner");
        ownerName = tag.getString("ownerName");
        name = tag.getString("name");
        totalSales = tag.getLong("totalSales");
        listings.clear();
        ListTag list = tag.getList("listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Listing l = new Listing();
            l.item = ItemStack.of(t.getCompound("item"));
            l.price = t.getLong("price");
            l.stock = t.getInt("stock");
            l.sold = t.getLong("sold");
            if (!l.item.isEmpty()) listings.add(l);
        }
    }

    /** 부서질 때 돌려줄 재고 아이템들. */
    public List<ItemStack> stockItems() {
        List<ItemStack> out = new ArrayList<>();
        for (Listing l : listings) {
            int left = l.stock;
            while (left > 0) {
                ItemStack s = l.item.copy();
                int n = Math.min(left, s.getMaxStackSize());
                s.setCount(n);
                out.add(s);
                left -= n;
            }
        }
        return out;
    }
}
