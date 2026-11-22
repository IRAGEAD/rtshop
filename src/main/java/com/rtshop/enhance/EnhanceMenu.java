package com.rtshop.enhance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 강화 모루 창. 바닐라 모루 창(AnvilMenu)을 이어받아서 Cinematic Interactions 의 모루 장면(망치질)이 그대로 동작한다.
 *  - 0번 칸: 강화할 장비 · 1번 칸: 방지권(선택) · 결과 칸: +1 단계 미리보기
 *  - 결과를 꺼내는 순간(망치질) 재료·Gold 를 쓰고 결과(성공·유지·손상·파괴)를 굴려서 꺼낸 장비에 적용한다.
 */
public class EnhanceMenu extends AnvilMenu {
    /** 서버: 지금 결과 칸에 놓인 미리보기의 표시값. */
    private String pendingId = "";

    public EnhanceMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public EnhanceMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(id, inv, access);
    }

    @Override
    public MenuType<?> getType() {
        return ModEnhance.MENU.get();
    }

    public ItemStack equipment() {
        return inputSlots.getItem(0);
    }

    public ItemStack guardStack() {
        return inputSlots.getItem(1);
    }

    /** 지금 올린 장비의 다음 단계 설정 (강화할 수 없으면 null). */
    public EnhanceConfig.Level nextLevel() {
        ItemStack in = equipment();
        if (in.isEmpty() || !Enhance.enhanceable(in)) return null;
        return Enhance.config(player).level(Enhance.level(in) + 1);
    }

    @Override
    public void createResult() {
        if (player.level().isClientSide) {
            return; // 결과 칸은 서버가 정해서 보내 준다
        }
        ItemStack in = equipment();
        if (in.isEmpty() || Enhance.isPreview(in) || !Enhance.enhanceable(in) || Enhance.level(in) >= EnhanceConfig.MAX_LEVEL
                || EnhanceConfig.get().level(Enhance.level(in) + 1) == null) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            pendingId = "";
        } else {
            ItemStack out = in.copy();
            out.setCount(1);
            Enhance.setLevel(out, Enhance.level(in) + 1);
            pendingId = UUID.randomUUID().toString();
            out.getOrCreateTag().putString(Enhance.TRY_TAG, pendingId);
            resultSlots.setItem(0, out);
        }
        broadcastChanges();
    }

    @Override
    protected boolean canMoveIntoInputSlots(ItemStack s) {
        return Enhance.enhanceable(s);
    }

    /** Shift 클릭: 방지권은 1번 칸, 장비는 0번 칸으로. */
    @Override
    public int getSlotToQuickMoveTo(ItemStack s) {
        return 0;
    }

    @Override
    public boolean setItemName(String name) {
        return true; // 이름 바꾸기는 쓰지 않음 (Cinematic 의 모루 장면이 보내는 이름 변경도 무시)
    }

    @Override
    protected boolean mayPickup(Player p, boolean hasStack) {
        if (!hasStack) return false;
        EnhanceConfig.Level l = nextLevel();
        return l != null && Enhance.affordable(p, Enhance.config(p), l);
    }

    @Override
    protected void onTake(Player p, ItemStack taken) {
        if (!(p instanceof ServerPlayer sp)) {
            return;
        }
        EnhanceConfig cfg = EnhanceConfig.get();
        ItemStack in = equipment();
        int cur = Enhance.level(in);
        EnhanceConfig.Level l = cfg.level(cur + 1);
        String id = pendingId;
        pendingId = "";
        ItemStack target = find(sp, taken, id);
        if (l == null || in.isEmpty() || target == null) {
            if (target != null) {
                Enhance.clearPreview(target);
                Enhance.setLevel(target, cur);
            }
            inputSlots.setItem(0, ItemStack.EMPTY);
            return;
        }
        Enhance.consume(sp, cfg, l);
        // 미리 사용한 보호권: 이번 강화에 쓰이고, 결과와 상관없이 사라진다
        int mask = Enhance.activeGuard(sp);
        Enhance.Guard guard = Enhance.fromMask(mask);
        boolean useGuard = mask != 0;
        if (useGuard) {
            Enhance.setActiveGuard(sp, 0);
        }
        Enhance.Outcome rolled = Enhance.roll(l, Enhance.testRoll != null ? Enhance.testRoll : sp.getRandom().nextDouble());
        Enhance.Outcome o = useGuard ? Enhance.protect(rolled, guard) : rolled;
        if (o == Enhance.Outcome.DAMAGE && cur == 0) {
            o = Enhance.Outcome.MAINTAIN;
        }
        boolean guarded = o != rolled;
        int after = Enhance.levelAfter(o, cur, cfg);
        ItemStack shown = target.copy();
        Enhance.clearPreview(shown);
        Enhance.clearPreview(target);
        if (after < 0) {
            target.setCount(0);
            Enhance.setLevel(shown, cur);
        } else {
            Enhance.setLevel(target, after);
            Enhance.setLevel(shown, after);
        }
        inputSlots.setItem(0, ItemStack.EMPTY);
        int event = o == Enhance.Outcome.DESTROY ? 1029 : 1030;
        access.execute((level, pos) -> level.levelEvent(event, pos, 0));
        sp.getInventory().setChanged();
        broadcastChanges();
        Enhance.announce(sp, shown, o, guarded, cur, after < 0 ? cur : after);
    }

    /** 꺼낸 미리보기 장비를 찾는다: 손에 든 것(일반 클릭) → 꺼낸 그 스택 → 가방(Shift 클릭). */
    private static ItemStack find(ServerPlayer sp, ItemStack taken, String id) {
        if (id.isEmpty()) return null;
        ItemStack carried = sp.containerMenu.getCarried();
        if (matches(carried, id)) return carried;
        if (matches(taken, id)) return taken;
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (matches(s, id)) return s;
        }
        return null;
    }

    private static boolean matches(ItemStack s, String id) {
        return !s.isEmpty() && s.getTag() != null && id.equals(s.getTag().getString(Enhance.TRY_TAG));
    }
}
