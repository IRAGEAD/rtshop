package com.rtshop.economy;

import com.rtshop.RtShop;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 경제를 무너뜨리는 대표적인 바닐라 꼼수 두 가지를 막는다 (config/rtshop/economy.json 에서 끌 수 있다).
 *  1) 철 골렘·좀비화 피글린·가디언 자동 농장: 플레이어가 직접 잡지 않으면 전리품이 나오지 않는다.
 *  2) 주민 거래: 에메랄드로 빵·철 갑옷 등을 받아 상인 NPC 에게 되파는 것을 막기 위해, NPC 가 매입하는 물건을 파는 거래를 없앤다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class EconomyGuards {
    private EconomyGuards() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || event.isRecentlyHit()) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        if (id != null && EconomyConfig.get().playerKillOnlyDrops.contains(id.toString())) {
            event.setCanceled(true);
        }
    }

    /** 이 물건을 받아 주는 상인 NPC 가 있는가. */
    public static boolean npcBuys(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (ShopData d : ShopServer.shops().values()) {
            if (d.findSellEntry(stack) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** 주민의 거래 목록에서 "상인 NPC 가 매입하는 물건을 파는 거래"를 지운다. 지운 개수를 돌려준다. */
    public static int filterOffers(AbstractVillager villager) {
        MerchantOffers offers = villager.getOffers();
        int before = offers.size();
        offers.removeIf(o -> !o.getResult().is(Items.EMERALD) && npcBuys(o.getResult()));
        return before - offers.size();
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractVillager villager) || event.getLevel().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player) || ShopServer.isNpc(villager)) {
            return;
        }
        String mode = EconomyConfig.get().villagerTrading;
        if (mode.equals("allow")) {
            return;
        }
        if (mode.equals("block")) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            player.displayClientMessage(Component.translatable("rtshop.msg.villager_blocked").withStyle(ChatFormatting.RED), true);
            return;
        }
        try {
            filterOffers(villager);
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] 주민 거래 목록을 정리하지 못했습니다", t);
        }
    }
}
