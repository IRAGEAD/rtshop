/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Registry
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.NewRegistryEvent
 *  net.minecraftforge.registries.RegistryBuilder
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSin;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.MOD)
public class BossRegistries {
    public static final ResourceKey<Registry<PlayerSin>> PLAYER_SIN = BossRegistries.key("player_sin");
    public static Supplier<IForgeRegistry<PlayerSin>> PLAYER_SINS;

    @SubscribeEvent
    public static void newRegistryEvent(NewRegistryEvent event) {
        PLAYER_SINS = event.create(new RegistryBuilder().setName(PLAYER_SIN.m_135782_()));
    }

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.m_135788_((ResourceLocation)ResourceLocation.m_214293_((String)"fdbosses", (String)name));
    }
}

