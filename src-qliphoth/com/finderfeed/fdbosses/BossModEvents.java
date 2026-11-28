/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.particle.FDParticleProcessors
 *  com.finderfeed.fdlib.systems.particle.ParticleProcessorType
 *  net.minecraft.advancements.CriteriaTriggers
 *  net.minecraft.advancements.CriterionTrigger
 *  net.minecraft.world.entity.SpawnPlacements$Type
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraftforge.event.entity.SpawnPlacementRegisterEvent
 *  net.minecraftforge.event.entity.SpawnPlacementRegisterEvent$Operation
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdbosses.client.particles.particle_processors.ChesedRayCircleParticleProcessor;
import com.finderfeed.fdbosses.init.BossCriteriaTriggers;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.systems.particle.FDParticleProcessors;
import com.finderfeed.fdlib.systems.particle.ParticleProcessorType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD, modid="fdbosses")
public class BossModEvents {
    public static ParticleProcessorType<ChesedRayCircleParticleProcessor> CHESED_CIRCLE_PARTICLE_PROCESSOR_PARTICLE_PROCESSOR_TYPE;

    @SubscribeEvent
    public static void setupEvent(FMLCommonSetupEvent event) {
        CHESED_CIRCLE_PARTICLE_PROCESSOR_PARTICLE_PROCESSOR_TYPE = FDParticleProcessors.register((ParticleProcessorType)new ChesedRayCircleParticleProcessor.Type());
        event.enqueueWork(() -> CriteriaTriggers.m_10595_((CriterionTrigger)BossCriteriaTriggers.BOSS_KILLED));
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(BossEntities.ICE_MALKUTH_WARRIOR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::m_219013_, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(BossEntities.FIRE_MALKUTH_WARRIOR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::m_219013_, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}

