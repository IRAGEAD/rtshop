/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 */
package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.init.ItemInit;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class CreativeTabInit {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create((String)"spiral_abyss", (ResourceKey)Registries.f_279569_);
    public static final RegistrySupplier<CreativeModeTab> SPIRAL_ABYSS = TABS.register("spiral_abyss", () -> AbyssPlatform.creativeTab().m_257941_((Component)Component.m_237115_((String)"itemGroup.spiral_abyss")).m_257737_(() -> {
        ItemStack icon = new ItemStack((ItemLike)ItemInit.ABYSS_GATE_ACTIVATE_BLOCK_ITEM.get());
        icon.m_41784_().m_128405_("CustomModelData", 1);
        return icon;
    }).m_257501_((parameters, output) -> {
        output.m_246326_((ItemLike)ItemInit.ABYSS_GATE_VERTICAL_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_GATE_HORIZONTAL_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_GATE_CURVE_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_GATE_ACTIVATE_BLOCK_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_BASE_BLOCK_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_PILLAR_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_RUNE_1_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_RUNE_2_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_RUNE_3_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_RUNE_4_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_BORDER_MIDDLE_ITEM.get());
        output.m_246326_((ItemLike)ItemInit.ABYSS_BORDER_OUTSIDE_ITEM.get());
    }).m_257652_());

    private CreativeTabInit() {
    }
}

