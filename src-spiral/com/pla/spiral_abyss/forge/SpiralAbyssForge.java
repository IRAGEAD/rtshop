/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  dev.architectury.platform.forge.EventBuses
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.fml.common.Mod
 *  net.minecraftforge.fml.config.IConfigSpec
 *  net.minecraftforge.fml.config.ModConfig$Type
 *  net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
 */
package com.pla.spiral_abyss.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.init.CommandInit;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(value="spiral_abyss")
public final class SpiralAbyssForge {
    public SpiralAbyssForge(FMLJavaModLoadingContext context) {
        EventBuses.registerModEventBus((String)"spiral_abyss", (IEventBus)context.getModEventBus());
        context.registerConfig(ModConfig.Type.COMMON, (IConfigSpec)AbyssConfig.SPEC, "spiral_abyss/spiral_abyss-common.toml");
        context.registerConfig(ModConfig.Type.CLIENT, (IConfigSpec)AbyssClientConfig.SPEC, "spiral_abyss/spiral_abyss-client.toml");
        SpiralAbyss.init();
        MinecraftForge.EVENT_BUS.addListener(e -> CommandInit.register((CommandDispatcher<CommandSourceStack>)e.getDispatcher()));
    }
}

