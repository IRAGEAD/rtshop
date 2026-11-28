/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.screens.MenuScreens
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory
 *  net.minecraftforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.minecraftforge.client.event.RegisterKeyMappingsEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.ModLoadingContext
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
 */
package com.pla.spiral_abyss.forge.client.handler;

import com.pla.spiral_abyss.client.handler.JournalClientHandler;
import com.pla.spiral_abyss.client.renderer.AbyssPortalRenderer;
import com.pla.spiral_abyss.client.renderer.ChestLootRenderer;
import com.pla.spiral_abyss.client.renderer.ExperienceChestRenderer;
import com.pla.spiral_abyss.client.renderer.KeyEntityRenderer;
import com.pla.spiral_abyss.client.renderer.LootChestRenderer;
import com.pla.spiral_abyss.client.screen.AbyssClientConfigScreen;
import com.pla.spiral_abyss.client.screen.RewardChestScreen;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.init.EntityInit;
import com.pla.spiral_abyss.init.MenuInit;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="spiral_abyss", bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public final class ClientModHandler {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.m_96206_((MenuType)((MenuType)MenuInit.REWARD_CHEST.get()), RewardChestScreen::new));
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory(AbyssClientConfigScreen::new));
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(JournalClientHandler.OPEN);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)EntityInit.KEY_ENTITY.get(), KeyEntityRenderer::new);
        event.registerBlockEntityRenderer((BlockEntityType)BlockEntityInit.ABYSS_PORTAL.get(), AbyssPortalRenderer::new);
        event.registerEntityRenderer((EntityType)EntityInit.COPPER_CHEST.get(), LootChestRenderer::copper);
        event.registerEntityRenderer((EntityType)EntityInit.IRON_CHEST.get(), LootChestRenderer::iron);
        event.registerEntityRenderer((EntityType)EntityInit.GOLDEN_CHEST.get(), LootChestRenderer::golden);
        event.registerEntityRenderer((EntityType)EntityInit.CHEST_LOOT.get(), ChestLootRenderer::new);
        event.registerEntityRenderer((EntityType)EntityInit.EXPERIENCE_CHEST.get(), ExperienceChestRenderer::new);
    }
}

