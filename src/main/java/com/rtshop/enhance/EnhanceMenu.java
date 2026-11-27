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
 *  - 0번 칸: 강화할 장비 · 1번 칸: 보호권(선택; 한 번에 한 장, 한 가지) · 결과 칸: +1 단계 미리보기
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

    /** 칸마다 놓을 수 있는 것: 0 = 무엇이든(강화할 수 없으면 결과가 안 나올 뿐), 1 = 보호권만. */
    @Override
    protected net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition.create()
                .withSlot(0, 27, 47, s -> true)
                .withSlot(1, 76, 47, s -> s.getItem() instanceof GuardItem)
                .withResultSlot(2, 134, 47)
                .build();
    }

    @Override
    protected boolean canMoveIntoInputSlots(ItemStack s) {
        return Enhance.enhanceable(s) || s.getItem() instanceof GuardItem;
    }

    /** Shift 클릭: 보호권은 1번 칸, 장비는 0번 칸으로. */
    @Override
    public int getSlotToQuickMoveTo(ItemStack s) {
        return s.getItem() instanceof GuardItem ? 1 : 0;
    }

    /** 지금 올린 장비에 맞춘 다음 단계 확률 (전설 이상·히든 장비는 파괴 없음). */
    public EnhanceConfig.Level oddsLevel() {
        return Enhance.levelFor(Enhance.config(player), nextLevel(), equipment());
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
        // 모루에 올린 보호권 (올린 것이 없으면 옛 방식으로 미리 써 둔 보호)
        ItemStack ticket = guardStack();
        Enhance.Guard guard = Enhance.guardFor(sp, ticket);
        boolean useGuard = guard != Enhance.Guard.NONE;
        // 전설 이상·히든 장비는 파괴되지 않는다: 그 확률은 유지와 손상에 반씩. 실패 누적이 있으면 성공률이 오른다 (3.66.0)
        int failsBefore = Enhance.failStacks(in);
        l = Enhance.levelFor(cfg, l, in);
        Enhance.Outcome rolled = Enhance.roll(l, Enhance.testRoll != null ? Enhance.testRoll : sp.getRandom().nextDouble());
        Enhance.Outcome plain = rolled == Enhance.Outcome.DAMAGE && cur == 0 ? Enhance.Outcome.MAINTAIN : rolled;
        Enhance.Outcome o = useGuard ? Enhance.protect(rolled, guard) : rolled;
        if (o == Enhance.Outcome.DAMAGE && cur == 0) {
            o = Enhance.Outcome.MAINTAIN;
        }
        boolean guarded = o != plain;
        // 3.69.0: guardKeepUpTo(기본 +6) 까지 올리는 강화에서는 보호권이 장비를 지켰을 때만 한 장 쓰이고, 그 위는 시도할 때마다 한 장
        lastGuardSpentForTest = false;
        if (useGuard && Enhance.guardSpent(cfg, cur + 1, guarded)) {
            lastGuardSpentForTest = true;
            if (Enhance.guardOf(ticket) != Enhance.Guard.NONE) {
                if (!sp.getAbilities().instabuild) {
                    ticket.shrink(1);
                    inputSlots.setItem(1, ticket.isEmpty() ? ItemStack.EMPTY : ticket);
                }
            } else {
                Enhance.setActiveGuard(sp, 0);
            }
        }
        int after = Enhance.levelAfter(o, cur, cfg);
        ItemStack shown = target.copy();
        Enhance.clearPreview(shown);
        Enhance.clearPreview(target);
        int failsAfter = Enhance.failStacksAfter(cfg, failsBefore, o);
        if (after < 0) {
            target.setCount(0);
            Enhance.setLevel(shown, cur);
        } else {
            Enhance.setLevel(target, after);
            Enhance.setLevel(shown, after);
            Enhance.setFailStacks(target, failsAfter);
            Enhance.setFailStacks(shown, failsAfter);
        }
        inputSlots.setItem(0, ItemStack.EMPTY);
        int event = o == Enhance.Outcome.DESTROY ? 1029 : 1030;
        access.execute((level, pos) -> level.levelEvent(event, pos, 0));
        sp.getInventory().setChanged();
        broadcastChanges();
        Enhance.announce(sp, shown, o, guarded, cur, after < 0 ? cur : after);
    }

    /** 점검용: 마지막 강화에서 보호권이 쓰였는가. */
    public static volatile boolean lastGuardSpentForTest;

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
