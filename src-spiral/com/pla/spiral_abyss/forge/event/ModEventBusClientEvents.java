/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.DimensionSpecialEffects
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.EntityRenderersEvent$RegisterLayerDefinitions
 *  net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent
 *  net.minecraftforge.client.event.RegisterGuiOverlaysEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.pla.spiral_abyss.forge.event;

import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.RareRewardOverlay;
import com.pla.spiral_abyss.client.renderer.AbyssSkyEffects;
import com.pla.spiral_abyss.client.renderer.CopperChestModel;
import com.pla.spiral_abyss.client.renderer.ExperienceChestModel;
import com.pla.spiral_abyss.client.renderer.GoldenChestModel;
import com.pla.spiral_abyss.client.renderer.IronChestModel;
import com.pla.spiral_abyss.client.renderer.KeyEntityModel;
import com.pla.spiral_abyss.client.renderer.ModModelLayers;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="spiral_abyss", bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class ModEventBusClientEvents {
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("encounter", (gui, graphics, partialTick, width, height) -> AbyssEncounterOverlay.HUD.render(gui, graphics, partialTick, width, height));
        event.registerAboveAll("rare_reward", (gui, graphics, partialTick, width, height) -> RareRewardOverlay.HUD.render(gui, graphics, partialTick, width, height));
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(AbyssSkyEffects.ID, (DimensionSpecialEffects)new AbyssSkyEffects());
    }

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.KEY_ENTITY_LAYER, KeyEntityModel::createBodyLayer);
        event.registerLayerDefinition(ExperienceChestModel.LAYER_LOCATION, ExperienceChestModel::createBodyLayer);
        event.registerLayerDefinition(CopperChestModel.LAYER_LOCATION, CopperChestModel::createBodyLayer);
        event.registerLayerDefinition(IronChestModel.LAYER_LOCATION, IronChestModel::createBodyLayer);
        event.registerLayerDefinition(GoldenChestModel.LAYER_LOCATION, GoldenChestModel::createBodyLayer);
    }
}

