package com.rtshop;

import com.mojang.logging.LogUtils;
import com.rtshop.farm.FarmEvents;
import com.rtshop.farm.FarmLootModifier;
import com.rtshop.item.ModItems;
import com.rtshop.net.ShopNet;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * RT Shop - Gold 경제, 상인 NPC, 은행, 농사 규칙, 토지 시스템.
 * 믹스인/코어모드/다른 모드 API 를 쓰지 않고 자체 네임스페이스(rtshop)만 사용합니다.
 */
@Mod(RtShop.MODID)
public class RtShop {
    public static final String MODID = "rtshop";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RtShop() {
        com.rtshop.compat.LogNoise.install(); // 지형 생성 경고 폭주 걸러 내기
        com.rtshop.util.ConfigNames.sweep(); // 저장된 설정 파일 안의 옛 마을 이름을 새 이름으로
        com.rtshop.compat.BetterCombatEpicFight.patch(); // Epic Fight 무기가 납작한 아이템으로 보이던 문제
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        com.rtshop.enhance.ModEnhance.register(bus);
        ModItems.ITEMS.register(bus);
        com.rtshop.magic.MagicRegistry.register(bus);
        com.rtshop.story.Companions.register(bus);
        com.rtshop.rules.LootRules.init();
        FarmLootModifier.SERIALIZERS.register(bus);
        bus.addListener(this::onCreativeTab);
        bus.addListener(this::onCommonSetup);
        ShopNet.register();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FarmEvents::registerCompat);
        event.enqueueWork(() -> {
            if (net.minecraftforge.fml.ModList.get().isLoaded("cinematic")) com.rtshop.magic.CinematicCompat.register();
            if (net.minecraftforge.fml.ModList.get().isLoaded("spell_engine") && net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) com.rtshop.compat.SpellMana.hook();
            com.rtshop.job.MagicLock.hook();
        });
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.FIRST_LAND_DEED);
            event.accept(ModItems.LAND_DEED);
            event.accept(ModItems.LARGE_LAND_DEED);
            event.accept(ModItems.RESTORE_TICKET);
            event.accept(ModItems.TICKET_NORMAL);
            event.accept(ModItems.TICKET_MID);
            event.accept(ModItems.TICKET_HIGH);
            event.accept(ModItems.PET_TICKET_NORMAL);
            event.accept(ModItems.PET_TICKET_MID);
            event.accept(ModItems.PET_TICKET_HIGH);
            event.accept(ModItems.ACC_TICKET_NORMAL);
            event.accept(ModItems.ACC_TICKET_MID);
            event.accept(ModItems.ACC_TICKET_HIGH);
            event.accept(ModItems.SHARD);
            event.accept(ModItems.ENHANCE_ANVIL);
            event.accept(ModItems.PLAYER_SHOP);
            event.accept(ModItems.GUARD_DAMAGE);
            event.accept(ModItems.GUARD_DESTROY);
            event.accept(ModItems.GUARD_FULL);
            event.accept(ModItems.RAID_TICKET);
            event.accept(ModItems.SURVEY_COMPASS);
            event.accept(ModItems.WEAPON_MANUAL);
            event.accept(com.rtshop.magic.MagicRegistry.ORB_ITEM);
            event.accept(com.rtshop.magic.MagicRegistry.TICKET);
            event.accept(com.rtshop.magic.MagicRegistry.TICKET_SELECT);
            event.accept(com.rtshop.magic.MagicRegistry.SCROLL);
        }
    }
}
