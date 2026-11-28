/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.flag.FeatureFlags
 *  net.minecraft.world.inventory.MenuType
 */
package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.menu.RewardChestMenu;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class MenuInit {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create((String)"spiral_abyss", (ResourceKey)Registries.f_256798_);
    public static final RegistrySupplier<MenuType<RewardChestMenu>> REWARD_CHEST = MENUS.register("reward_chest", () -> new MenuType(RewardChestMenu::new, FeatureFlags.f_244332_));

    private MenuInit() {
    }
}

