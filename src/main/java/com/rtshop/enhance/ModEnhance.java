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

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RtShop.MODID);

    /** 플레이어 상점 진열대. */
    public static final RegistryObject<Block> PSHOP = BLOCKS.register("player_shop",
            () -> new com.rtshop.pshop.PlayerShopBlock(BlockBehaviour.Properties.copy(Blocks.BARREL).strength(2.5F, 1200F).noOcclusion()));

    /** 조합 시세판 (3.72.0): 우클릭하면 조합 증서의 시세판이 열린다. */
    public static final RegistryObject<Block> STOCK_BOARD = BLOCKS.register("stock_board",
            () -> new com.rtshop.stock.StockBoardBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_PLANKS).strength(2.0F, 1200F).noOcclusion()));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.rtshop.pshop.PlayerShopBlockEntity>> PSHOP_BE =
            BLOCK_ENTITIES.register("player_shop", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(com.rtshop.pshop.PlayerShopBlockEntity::new, PSHOP.get()).build(null));

    public static final RegistryObject<MenuType<EnhanceMenu>> MENU = MENUS.register("enhance",
            () -> new MenuType<>(EnhanceMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }
}
