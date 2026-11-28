package com.rtshop.rules;

import com.rtshop.RtShop;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.82 (사용자 11-28 17:39 "마을의 지도 제작자가 원소 지도와 열쇠를 판매하는 것도 막아줘"):
 * 주민 · 떠돌이 상인의 거래 가운데 금지 아이템(item_policy.json 의 tradeBan, 기본 spmovesets 전부)이 들어간 것을 뺀다.
 * spmovesets 는 지도 제작자에게 원소 지도(값: 에메랄드 + 뽑기 열쇠)를 붙인다 — 파는 것 · 값 어느 쪽이든 걸리면 뺀다.
 * 거래 창이 열리기 전(상호작용)과 거래한 뒤(레벨이 올라 새 거래가 붙을 수 있음)에 본다. 뺀 거래는 주민의 기록에서도 사라진다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class TradeGuard {
    private TradeGuard() {
    }

    /** 점검용: 지금까지 뺀 거래 수. */
    public static volatile int removedForTest;

    public static boolean banned(MerchantOffer o) {
        return ItemPolicy.tradeBanned(o.getResult()) || ItemPolicy.tradeBanned(o.getBaseCostA()) || ItemPolicy.tradeBanned(o.getCostB());
    }

    /** 이 상인의 거래에서 금지 거래를 뺀다. 뺀 수. */
    public static int strip(AbstractVillager merchant) {
        if (merchant == null || merchant.level().isClientSide()) return 0;
        MerchantOffers offers = merchant.getOffers();
        int before = offers.size();
        offers.removeIf(TradeGuard::banned);
        int removed = before - offers.size();
        if (removed > 0) removedForTest += removed;
        return removed;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof AbstractVillager merchant)) return;
        try {
            strip(merchant);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 주민 거래 정리 실패", t);
        }
    }

    /** 거래 창이 열리는 순간 (상호작용 뒤에 모드가 거래를 붙여도): 이 사람과 거래 중인 상인을 찾아 뺀다. 거래 목록은 이 뒤에 화면으로 보내진다. */
    @SubscribeEvent
    public static void onOpen(net.minecraftforge.event.entity.player.PlayerContainerEvent.Open event) {
        if (!(event.getContainer() instanceof net.minecraft.world.inventory.MerchantMenu) || event.getEntity().level().isClientSide()) return;
        try {
            for (AbstractVillager m : event.getEntity().level().getEntitiesOfClass(AbstractVillager.class,
                    event.getEntity().getBoundingBox().inflate(16), v -> v.getTradingPlayer() == event.getEntity())) {
                strip(m);
            }
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 주민 거래 정리 실패", t);
        }
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        try {
            strip(event.getAbstractVillager());
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 주민 거래 정리 실패", t);
        }
    }
}
