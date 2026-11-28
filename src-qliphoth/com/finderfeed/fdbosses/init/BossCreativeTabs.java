/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.init.BossItems;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;

public class BossCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create((ResourceKey)Registries.f_279569_, (String)"fdbosses");
    public static final Supplier<CreativeModeTab> MAIN = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder().m_257737_(() -> BossItems.EYE_OF_CHESED.get().m_7968_()).m_257941_((Component)Component.m_237115_((String)"fdbosses.word.fdbosses")).m_257652_());
}

