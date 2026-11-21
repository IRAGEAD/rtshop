package com.rtshop.enhance;

import com.rtshop.RtShop;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 강화 모루 블록과 강화 창 등록. */
public final class ModEnhance {
    private ModEnhance() {
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RtShop.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RtShop.MODID);

    public static final RegistryObject<Block> ANVIL = BLOCKS.register("enhance_anvil",
            () -> new EnhanceAnvilBlock(BlockBehaviour.Properties.copy(Blocks.ANVIL)));

    public static final RegistryObject<MenuType<EnhanceMenu>> MENU = MENUS.register("enhance",
            () -> new MenuType<>(EnhanceMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        MENUS.register(bus);
    }
}
