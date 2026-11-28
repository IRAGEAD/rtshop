/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EntityType
 *  net.minecraftforge.event.entity.EntityAttributeCreationEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.pla.spiral_abyss.forge.event;

import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.init.EntityInit;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="spiral_abyss", bus=Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put((EntityType)EntityInit.KEY_ENTITY.get(), KeyEntity.createAttributes().m_22265_());
        event.put((EntityType)EntityInit.COPPER_CHEST.get(), KeyEntity.createAttributes().m_22265_());
        event.put((EntityType)EntityInit.IRON_CHEST.get(), KeyEntity.createAttributes().m_22265_());
        event.put((EntityType)EntityInit.GOLDEN_CHEST.get(), KeyEntity.createAttributes().m_22265_());
        event.put((EntityType)EntityInit.EXPERIENCE_CHEST.get(), KeyEntity.createAttributes().m_22265_());
    }
}

