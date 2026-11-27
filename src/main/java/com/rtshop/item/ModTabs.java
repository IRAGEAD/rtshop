package com.rtshop.item;

import com.rtshop.RtShop;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * RT Shop 크리에이티브 탭 (3.74.0, 사용자 W-29 "크리에이티브에서 rtshop 아이템들을 꺼낼 수 있도록"): 이 모드의 아이템을 모두 한 탭에.
 * (예전처럼 일부는 바닐라 「도구 및 유틸리티」 탭에도 그대로 있다.)
 */
public final class ModTabs {
    private ModTabs() {
    }

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RtShop.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rtshop"))
            .icon(() -> new ItemStack(ModItems.GOLD_CHECK.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> ro : ModItems.ITEMS.getEntries()) out.accept(ro.get());
                for (RegistryObject<Item> ro : com.rtshop.magic.MagicRegistry.ITEMS.getEntries()) out.accept(ro.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    /** 점검용: 탭에 들어가는 아이템 수. */
    public static int countForTest() {
        return ModItems.ITEMS.getEntries().size() + com.rtshop.magic.MagicRegistry.ITEMS.getEntries().size();
    }
}
