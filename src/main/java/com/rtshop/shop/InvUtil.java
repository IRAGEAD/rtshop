package com.rtshop.shop;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 인벤토리 시뮬레이션 유틸. 서버/클라이언트 양쪽에서 같은 규칙으로 "들어갈 자리가 있는가"를 판단합니다.
 * 플레이어의 메인 인벤토리(핫바 + 가방 36칸)만 대상으로 하며, 갑옷/보조손 슬롯은 건드리지 않습니다.
 */
public final class InvUtil {
    private InvUtil() {
    }

    public static final int MAX_STACK_CAP = 64;

    public static List<ItemStack> copyMain(Inventory inv) {
        List<ItemStack> out = new ArrayList<>(inv.items.size());
        for (ItemStack s : inv.items) {
            out.add(s.copy());
        }
        return out;
    }

    public static List<ItemStack> deepCopy(List<ItemStack> list) {
        List<ItemStack> out = new ArrayList<>(list.size());
        for (ItemStack s : list) {
            out.add(s.copy());
        }
        return out;
    }

    public static long count(List<ItemStack> list, Predicate<ItemStack> match) {
        long total = 0;
        for (ItemStack s : list) {
            if (!s.isEmpty() && match.test(s)) {
                total += s.getCount();
            }
        }
        return total;
    }

    /** list 에서 amount 개를 제거합니다. 모자라면 false (이미 일부 제거됐을 수 있으므로 시뮬레이션 복사본에만 사용). */
    public static boolean remove(List<ItemStack> list, Predicate<ItemStack> match, long amount) {
        long need = amount;
        for (int i = 0; i < list.size() && need > 0; i++) {
            ItemStack s = list.get(i);
            if (s.isEmpty() || !match.test(s)) {
                continue;
            }
            int take = (int) Math.min((long) s.getCount(), need);
            s.shrink(take);
            need -= take;
            if (s.isEmpty()) {
                list.set(i, ItemStack.EMPTY);
            }
        }
        return need <= 0;
    }

    /** list 에 proto 아이템 amount 개를 넣을 수 있으면 넣고 true, 자리가 모자라면 false. */
    public static boolean insert(List<ItemStack> list, ItemStack proto, long amount) {
        if (amount <= 0) {
            return true;
        }
        if (proto.isEmpty()) {
            return false;
        }
        int max = Math.max(1, Math.min(proto.getMaxStackSize(), MAX_STACK_CAP));
        long left = amount;

        // 1) 기존 스택에 채우기
        for (ItemStack s : list) {
            if (left <= 0) {
                break;
            }
            if (!s.isEmpty() && s.getCount() < max && ItemStack.isSameItemSameTags(s, proto)) {
                int add = (int) Math.min((long) (max - s.getCount()), left);
                s.grow(add);
                left -= add;
            }
        }
        // 2) 빈 칸 사용
        for (int i = 0; i < list.size() && left > 0; i++) {
            if (list.get(i).isEmpty()) {
                int put = (int) Math.min((long) max, left);
                ItemStack n = proto.copy();
                n.setCount(put);
                list.set(i, n);
                left -= put;
            }
        }
        return left <= 0;
    }

    /** list 에 proto 아이템을 몇 개까지 더 넣을 수 있는지 계산합니다(목록은 바꾸지 않음). */
    public static long capacity(List<ItemStack> list, ItemStack proto) {
        if (proto.isEmpty()) {
            return 0;
        }
        int max = Math.max(1, Math.min(proto.getMaxStackSize(), MAX_STACK_CAP));
        long cap = 0;
        for (ItemStack s : list) {
            if (s.isEmpty()) {
                cap += max;
            } else if (s.getCount() < max && ItemStack.isSameItemSameTags(s, proto)) {
                cap += max - s.getCount();
            }
        }
        return cap;
    }

    /** 시뮬레이션 결과를 실제 인벤토리에 반영합니다. */
    public static void apply(Inventory inv, List<ItemStack> sim) {
        int n = Math.min(inv.items.size(), sim.size());
        for (int i = 0; i < n; i++) {
            inv.items.set(i, sim.get(i));
        }
        inv.setChanged();
    }
}
