package com.rtshop.magic;

import com.rtshop.RtShop;
import com.rtshop.item.TooltipItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 마법 뽑기 블록·티켓 등록. */
public final class MagicRegistry {
    private MagicRegistry() {
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RtShop.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RtShop.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RtShop.MODID);

    public static final RegistryObject<Block> ORB = BLOCKS.register("arcane_orb",
            () -> new ArcaneOrbBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(5F, 1200F).noOcclusion()));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ArcaneOrbBlockEntity>> ORB_BE = BLOCK_ENTITIES.register("arcane_orb",
            () -> BlockEntityType.Builder.of(ArcaneOrbBlockEntity::new, ORB.get()).build(null));

    public static final RegistryObject<Item> ORB_ITEM = ITEMS.register("arcane_orb",
            () -> new BlockItem(ORB.get(), new Item.Properties().rarity(Rarity.EPIC)) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
                                            java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag flag) {
                    tip.add(net.minecraft.network.chat.Component.translatable("block.rtshop.arcane_orb.tip1").withStyle(net.minecraft.ChatFormatting.GRAY));
                    tip.add(net.minecraft.network.chat.Component.translatable("block.rtshop.arcane_orb.tip2").withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            });

    /** 무작위 마법 뽑기권 · 분야 선택 마법 뽑기권 · 인챈트 재설정 주문서. */
    public static final RegistryObject<Item> TICKET = ITEMS.register("magic_ticket",
            () -> new TooltipItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), false,
                    "item.rtshop.magic_ticket.tip", "item.rtshop.magic_ticket.tip2"));
    public static final RegistryObject<Item> TICKET_SELECT = ITEMS.register("magic_ticket_select",
            () -> new TooltipItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), true,
                    "item.rtshop.magic_ticket_select.tip", "item.rtshop.magic_ticket.tip2"));
    public static final RegistryObject<Item> SCROLL = ITEMS.register("enchant_scroll",
            () -> new TooltipItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), false,
                    "item.rtshop.enchant_scroll.tip", "item.rtshop.magic_ticket.tip2"));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
    }
}
