/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.fml.common.Mod
 *  net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
 *  org.slf4j.Logger
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossBars;
import com.finderfeed.fdbosses.init.BossBlocks;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossCreativeTabs;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossRecipeSerializers;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.init.BossStructures;
import com.finderfeed.fdbosses.init.BossTileEntities;
import com.finderfeed.fdbosses.init.GeburahSins;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(value="fdbosses")
public class FDBosses {
    public static final String MOD_ID = "fdbosses";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation location(String loc) {
        return ResourceLocation.m_214293_((String)MOD_ID, (String)loc);
    }

    public FDBosses() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BossItems.ITEMS.register(bus);
        BossModels.INFOS.register(bus);
        GeburahSins.SINS.register(bus);
        BossBlocks.BLOCKS.register(bus);
        BossSounds.SOUNDS.register(bus);
        BossBars.BOSS_BARS.register(bus);
        BossConfigs.CONFIGS.register(bus);
        BossAnims.ANIMATIONS.register(bus);
        BossEntities.ENTITIES.register(bus);
        BossEffects.MOB_EFFECTS.register(bus);
        BossParticles.PARTICLES.register(bus);
        BossStructures.STRUCTURES.register(bus);
        BossTileEntities.TILE_ENTITIES.register(bus);
        BossCreativeTabs.CREATIVE_TABS.register(bus);
        BossRecipeSerializers.RECIPE_SERIALIZERS.register(bus);
        BossEntityDataSerializers.ENTITY_DATA_SERIALIZERS.register(bus);
    }
}

