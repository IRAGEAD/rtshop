package com.rtshop.rules;

import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 터무니없이 센 장비(ItemPolicy.extreme, 3.65.0)가 서바이벌 플레이어 손에 들어가지 않게 하는 마지막 그물.
 * 전리품 · 몹 드롭 · 조합법은 ItemPolicy / LootRules / MobRules 가 막는다. 그 밖의 길(구조물 상자에 미리 든 것, 모드가 가방에 바로 넣는 것)도 있으므로
 * 땅에서 줍지 못하게 하고, 서바이벌 플레이어의 가방은 5초마다 살펴 치운다. 창작 · 관전 모드는 건드리지 않는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class ExtremeGuard {
    private ExtremeGuard() {
    }

    /** 점검용: 치운 수. */
    public static volatile int removedForTest;

    static boolean exempt(ServerPlayer p) {
        return p.isCreative() || p.isSpectator();
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || exempt(p)) {
            return;
        }
        ItemStack s = e.getItem().getItem();
        if (ItemPolicy.extreme(s)) {
            e.setCanceled(true);
            e.getItem().discard();
            tell(p, s, 1);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 100 != 37 || exempt(p)) {
            return;
        }
        sweep(p);
    }

    /** 가방(손 · 갑옷 칸 포함)의 터무니없는 장비를 치우고 그 수를 돌려준다. */
    public static int sweep(ServerPlayer p) {
        Inventory inv = p.getInventory();
        int n = 0;
        ItemStack first = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemPolicy.extreme(s)) {
                if (first.isEmpty()) {
                    first = s.copy();
                }
                inv.setItem(i, ItemStack.EMPTY);
                n++;
            }
        }
        if (n > 0) {
            tell(p, first, n);
        }
        return n;
    }

    private static void tell(ServerPlayer p, ItemStack s, int n) {
        removedForTest += n;
        RtShop.LOGGER.info("[rtshop] {} 의 터무니없이 센 장비 {}개를 치웠습니다 (처음 것: {})", p.getGameProfile().getName(), n, s.getItem());
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rules.extreme_removed", s.getHoverName()).withStyle(ChatFormatting.GRAY));
    }
}
