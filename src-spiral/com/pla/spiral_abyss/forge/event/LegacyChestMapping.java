/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.registries.MissingMappingsEvent
 *  net.minecraftforge.registries.MissingMappingsEvent$Mapping
 */
package com.pla.spiral_abyss.forge.event;

import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid="spiral_abyss")
public final class LegacyChestMapping {
    @SubscribeEvent
    public static void missing(MissingMappingsEvent event) {
        event.getMappings(Registries.f_256939_, "spiral_abyss").stream().filter(mapping -> mapping.getKey().m_135815_().equals("reward_chest") || mapping.getKey().m_135815_().equals("scroll_entity")).forEach(MissingMappingsEvent.Mapping::ignore);
    }
}

