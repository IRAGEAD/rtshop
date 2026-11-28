/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.Rarity
 *  net.minecraft.world.level.block.Block
 */
package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.init.BlockInit;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create((String)"spiral_abyss", (ResourceKey)Registries.f_256913_);
    public static final RegistrySupplier<BlockItem> SPINNING_BLOCK_ITEM = ITEMS.register("spinning_block", () -> new BlockItem((Block)BlockInit.SPINNING_BLOCK.get(), new Item.Properties().m_41497_(Rarity.UNCOMMON)));
    public static final RegistrySupplier<BlockItem> ABYSS_BASE_BLOCK_ITEM = ItemInit.blockItem("abyss_base_block", BlockInit.ABYSS_BASE_BLOCK);
    public static final RegistrySupplier<BlockItem> ABYSS_PILLAR_ITEM = ItemInit.blockItem("abyss_pillar", BlockInit.ABYSS_PILLAR);
    public static final RegistrySupplier<BlockItem> ABYSS_RUNE_1_ITEM = ItemInit.blockItem("abyss_rune_1", BlockInit.ABYSS_RUNE_1);
    public static final RegistrySupplier<BlockItem> ABYSS_RUNE_2_ITEM = ItemInit.blockItem("abyss_rune_2", BlockInit.ABYSS_RUNE_2);
    public static final RegistrySupplier<BlockItem> ABYSS_RUNE_3_ITEM = ItemInit.blockItem("abyss_rune_3", BlockInit.ABYSS_RUNE_3);
    public static final RegistrySupplier<BlockItem> ABYSS_RUNE_4_ITEM = ItemInit.blockItem("abyss_rune_4", BlockInit.ABYSS_RUNE_4);
    public static final RegistrySupplier<BlockItem> ABYSS_BORDER_MIDDLE_ITEM = ItemInit.blockItem("abyss_border_middle", BlockInit.ABYSS_BORDER_MIDDLE);
    public static final RegistrySupplier<BlockItem> ABYSS_BORDER_OUTSIDE_ITEM = ItemInit.blockItem("abyss_border_outside", BlockInit.ABYSS_BORDER_OUTSIDE);
    public static final RegistrySupplier<BlockItem> ABYSS_GATE_VERTICAL_ITEM = ItemInit.blockItem("abyss_gate_vertical", BlockInit.ABYSS_GATE_VERTICAL);
    public static final RegistrySupplier<BlockItem> ABYSS_GATE_HORIZONTAL_ITEM = ItemInit.blockItem("abyss_gate_horizontal", BlockInit.ABYSS_GATE_HORIZONTAL);
    public static final RegistrySupplier<BlockItem> ABYSS_GATE_CURVE_ITEM = ItemInit.blockItem("abyss_gate_curve", BlockInit.ABYSS_GATE_CURVE);
    public static final RegistrySupplier<BlockItem> ABYSS_GATE_ACTIVATE_BLOCK_ITEM = ItemInit.blockItem("abyss_gate_activate_block", BlockInit.ABYSS_GATE_ACTIVATE_BLOCK);

    private static RegistrySupplier<BlockItem> blockItem(String name, RegistrySupplier<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem((Block)block.get(), new Item.Properties().m_41497_(Rarity.UNCOMMON)));
    }
}

