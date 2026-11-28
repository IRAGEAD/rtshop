/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.registries.ForgeRegistries$Keys
 *  net.minecraftforge.registries.MissingMappingsEvent
 *  net.minecraftforge.registries.MissingMappingsEvent$Mapping
 */
package com.pla.spiral_abyss.forge.event;

import com.pla.spiral_abyss.init.ItemInit;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid="spiral_abyss")
public final class LegacyPortalMappings {
    @SubscribeEvent
    public static void missingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, "spiral_abyss")) {
            if (!mapping.getKey().m_135815_().equals("abyss_portal_anchor")) continue;
            mapping.remap((Object)Blocks.f_50602_);
        }
        for (MissingMappingsEvent.Mapping mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, "spiral_abyss")) {
            switch (mapping.getKey().m_135815_()) {
                case "abyss_key": {
                    mapping.remap((Object)((Item)ItemInit.ABYSS_GATE_ACTIVATE_BLOCK_ITEM.get()));
                    break;
                }
                case "abyss_draft": {
                    mapping.ignore();
                }
            }
        }
        for (MissingMappingsEvent.Mapping mapping : event.getMappings(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, "spiral_abyss")) {
            if (!mapping.getKey().m_135815_().equals("abyss_portal")) continue;
            mapping.ignore();
        }
    }
}

