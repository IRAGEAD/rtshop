/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.event.AddReloadListenerEvent
 *  net.minecraftforge.event.OnDatapackSyncEvent
 *  net.minecraftforge.event.TickEvent$LevelTickEvent
 *  net.minecraftforge.event.TickEvent$ServerTickEvent
 *  net.minecraftforge.event.entity.EntityJoinLevelEvent
 *  net.minecraftforge.event.entity.EntityLeaveLevelEvent
 *  net.minecraftforge.event.entity.EntityMobGriefingEvent
 *  net.minecraftforge.event.entity.ProjectileImpactEvent
 *  net.minecraftforge.event.entity.living.LivingAttackEvent
 *  net.minecraftforge.event.entity.living.LivingChangeTargetEvent
 *  net.minecraftforge.event.entity.living.LivingDamageEvent
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.event.entity.living.LivingDestroyBlockEvent
 *  net.minecraftforge.event.entity.living.LivingDropsEvent
 *  net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent
 *  net.minecraftforge.event.entity.living.LivingEvent$LivingTickEvent
 *  net.minecraftforge.event.entity.living.LivingExperienceDropEvent
 *  net.minecraftforge.event.entity.living.LivingHealEvent
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.event.entity.living.LivingUseTotemEvent
 *  net.minecraftforge.event.entity.living.MobSpawnEvent$FinalizeSpawn
 *  net.minecraftforge.event.entity.living.MobSpawnEvent$SpawnPlacementCheck
 *  net.minecraftforge.event.entity.living.ShieldBlockEvent
 *  net.minecraftforge.event.entity.player.ArrowLooseEvent
 *  net.minecraftforge.event.entity.player.FillBucketEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$ItemPickupEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteract
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteractSpecific
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickItem
 *  net.minecraftforge.event.level.BlockEvent$BreakEvent
 *  net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent
 *  net.minecraftforge.event.level.ChunkEvent$Load
 *  net.minecraftforge.event.level.ExplosionEvent$Detonate
 *  net.minecraftforge.event.level.ExplosionEvent$Start
 *  net.minecraftforge.event.level.LevelEvent$Unload
 *  net.minecraftforge.event.server.ServerStartedEvent
 *  net.minecraftforge.event.server.ServerStoppedEvent
 *  net.minecraftforge.event.server.ServerStoppingEvent
 *  net.minecraftforge.eventbus.api.Event$Result
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 */
package com.pla.spiral_abyss.forge.event;

import com.pla.spiral_abyss.encounter.AbyssEncounters;
import com.pla.spiral_abyss.encounter.AbyssRejection;
import com.pla.spiral_abyss.encounter.WaveAllies;
import com.pla.spiral_abyss.encounter.challenge.ChallengeEvents;
import com.pla.spiral_abyss.event.AbyssArchitectureRepair;
import com.pla.spiral_abyss.event.AbyssBlockProtection;
import com.pla.spiral_abyss.event.AbyssMobDrops;
import com.pla.spiral_abyss.event.AbyssNaturalSpawns;
import com.pla.spiral_abyss.event.AbyssPlayerLifecycle;
import com.pla.spiral_abyss.event.AbyssRewardCarriers;
import com.pla.spiral_abyss.event.AbyssVoidMigration;
import com.pla.spiral_abyss.event.BossDeathBlocks;
import com.pla.spiral_abyss.event.RareRewardBroadcast;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.BossProgress;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="spiral_abyss")
public final class ForgeGameplayEvents {
    @SubscribeEvent
    public static void AbyssEncounters_reload(AddReloadListenerEvent event) {
        AbyssEvents.AddReloadListenerEvent shared = new AbyssEvents.AddReloadListenerEvent(arg_0 -> ((AddReloadListenerEvent)event).addListener(arg_0));
        AbyssEncounters.reload(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_started(ServerStartedEvent event) {
        AbyssEvents.ServerStartedEvent shared = new AbyssEvents.ServerStartedEvent(event.getServer());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ServerStartedEvent)event).isCanceled(), arg_0 -> ((ServerStartedEvent)event).setCanceled(arg_0));
        }
        AbyssEncounters.started(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_datapacksSynced(OnDatapackSyncEvent event) {
        AbyssEvents.OnDatapackSyncEvent shared = new AbyssEvents.OnDatapackSyncEvent(event.getPlayer(), event.getPlayerList());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((OnDatapackSyncEvent)event).isCanceled(), arg_0 -> ((OnDatapackSyncEvent)event).setCanceled(arg_0));
        }
        AbyssEncounters.datapacksSynced(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_tick(TickEvent.ServerTickEvent event) {
        AbyssEvents.TickEvent.ServerTickEvent shared = new AbyssEvents.TickEvent.ServerTickEvent(event.getServer(), AbyssEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((TickEvent.ServerTickEvent)event).isCanceled(), arg_0 -> ((TickEvent.ServerTickEvent)event).setCanceled(arg_0));
        }
        AbyssEncounters.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_stop(ServerStoppingEvent event) {
        AbyssEvents.ServerStoppingEvent shared = new AbyssEvents.ServerStoppingEvent(event.getServer());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ServerStoppingEvent)event).isCanceled(), arg_0 -> ((ServerStoppingEvent)event).setCanceled(arg_0));
        }
        AbyssEncounters.stop(shared);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void AbyssEncounters_preventPlayerDeath(LivingDeathEvent event) {
        AbyssEvents.LivingDeathEvent shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingDeathEvent)event).isCanceled(), arg_0 -> ((LivingDeathEvent)event).setCanceled(arg_0));
        }
        AbyssEncounters.preventPlayerDeath(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_isolateDamage(LivingAttackEvent event) {
        AbyssEvents.LivingAttackEvent shared = new AbyssEvents.LivingAttackEvent(event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingAttackEvent)event).isCanceled(), arg_0 -> ((LivingAttackEvent)event).setCanceled(arg_0));
        }
        shared.bindAmount(() -> ((LivingAttackEvent)event).getAmount());
        AbyssEncounters.isolateDamage(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOW)
    public static void AbyssEncounters_scaleDamage(LivingHurtEvent event) {
        AbyssEvents.LivingHurtEvent shared = new AbyssEvents.LivingHurtEvent(event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingHurtEvent)event).isCanceled(), arg_0 -> ((LivingHurtEvent)event).setCanceled(arg_0));
        }
        shared.bindAmount(() -> ((LivingHurtEvent)event).getAmount());
        AbyssEncounters.scaleDamage(shared);
        if (shared.amount != event.getAmount()) {
            event.setAmount(shared.amount);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void AbyssEncounters_scaleEffectiveHealth(LivingDamageEvent event) {
        AbyssEvents.LivingDamageEvent shared = new AbyssEvents.LivingDamageEvent(event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingDamageEvent)event).isCanceled(), arg_0 -> ((LivingDamageEvent)event).setCanceled(arg_0));
        }
        shared.bindAmount(() -> ((LivingDamageEvent)event).getAmount());
        AbyssEncounters.scaleEffectiveHealth(shared);
        if (shared.amount != event.getAmount()) {
            event.setAmount(shared.amount);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void AbyssEncounters_scaleEffectiveHealing(LivingHealEvent event) {
        AbyssEvents.LivingHealEvent shared = new AbyssEvents.LivingHealEvent(event.getEntity(), event.getAmount());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingHealEvent)event).isCanceled(), arg_0 -> ((LivingHealEvent)event).setCanceled(arg_0));
        }
        shared.bindAmount(() -> ((LivingHealEvent)event).getAmount());
        AbyssEncounters.scaleEffectiveHealing(shared);
        if (shared.amount != event.getAmount()) {
            event.setAmount(shared.amount);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void AbyssRejection_scaleDamage(LivingDamageEvent event) {
        AbyssEvents.LivingDamageEvent shared = new AbyssEvents.LivingDamageEvent(event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingDamageEvent)event).isCanceled(), arg_0 -> ((LivingDamageEvent)event).setCanceled(arg_0));
        }
        shared.bindAmount(() -> ((LivingDamageEvent)event).getAmount());
        AbyssRejection.scaleDamage(shared);
        if (shared.amount != event.getAmount()) {
            event.setAmount(shared.amount);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void WaveAllies_join(EntityJoinLevelEvent event) {
        AbyssEvents.EntityJoinLevelEvent shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((EntityJoinLevelEvent)event).isCanceled(), arg_0 -> ((EntityJoinLevelEvent)event).setCanceled(arg_0));
        }
        WaveAllies.join(shared);
    }

    @SubscribeEvent
    public static void WaveAllies_leave(EntityLeaveLevelEvent event) {
        AbyssEvents.EntityLeaveLevelEvent shared = new AbyssEvents.EntityLeaveLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((EntityLeaveLevelEvent)event).isCanceled(), arg_0 -> ((EntityLeaveLevelEvent)event).setCanceled(arg_0));
        }
        WaveAllies.leave(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void WaveAllies_target(LivingChangeTargetEvent event) {
        AbyssEvents.LivingChangeTargetEvent shared = new AbyssEvents.LivingChangeTargetEvent(event.getEntity(), event.getNewTarget());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingChangeTargetEvent)event).isCanceled(), arg_0 -> ((LivingChangeTargetEvent)event).setCanceled(arg_0));
        }
        shared.bindNewTarget(() -> ((LivingChangeTargetEvent)event).getNewTarget());
        WaveAllies.target(shared);
        if (shared.newTarget != event.getNewTarget()) {
            event.setNewTarget(shared.newTarget);
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void WaveAllies_attack(LivingAttackEvent event) {
        AbyssEvents.LivingAttackEvent shared = new AbyssEvents.LivingAttackEvent(event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingAttackEvent)event).isCanceled(), arg_0 -> ((LivingAttackEvent)event).setCanceled(arg_0));
        }
        shared.bindAmount(() -> ((LivingAttackEvent)event).getAmount());
        WaveAllies.attack(shared);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void WaveAllies_hurt(LivingHurtEvent event) {
        AbyssEvents.LivingHurtEvent shared = new AbyssEvents.LivingHurtEvent(event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingHurtEvent)event).isCanceled(), arg_0 -> ((LivingHurtEvent)event).setCanceled(arg_0));
        }
        shared.bindAmount(() -> ((LivingHurtEvent)event).getAmount());
        WaveAllies.hurt(shared);
        if (shared.amount != event.getAmount()) {
            event.setAmount(shared.amount);
        }
    }

    @SubscribeEvent
    public static void WaveAllies_started(ServerStartedEvent event) {
        AbyssEvents.ServerStartedEvent shared = new AbyssEvents.ServerStartedEvent(event.getServer());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ServerStartedEvent)event).isCanceled(), arg_0 -> ((ServerStartedEvent)event).setCanceled(arg_0));
        }
        WaveAllies.started(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void AbyssArchitectureRepair_creativeBreak(BlockEvent.BreakEvent event) {
        AbyssEvents.BlockEvent.BreakEvent shared = new AbyssEvents.BlockEvent.BreakEvent(event.getLevel(), event.getPos(), event.getState(), event.getPlayer());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((BlockEvent.BreakEvent)event).isCanceled(), arg_0 -> ((BlockEvent.BreakEvent)event).setCanceled(arg_0));
        }
        AbyssArchitectureRepair.creativeBreak(shared);
    }

    @SubscribeEvent
    public static void AbyssArchitectureRepair_tick(TickEvent.LevelTickEvent event) {
        AbyssEvents.TickEvent.LevelTickEvent shared = new AbyssEvents.TickEvent.LevelTickEvent(event.level, AbyssEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((TickEvent.LevelTickEvent)event).isCanceled(), arg_0 -> ((TickEvent.LevelTickEvent)event).setCanceled(arg_0));
        }
        AbyssArchitectureRepair.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssArchitectureRepair_unload(LevelEvent.Unload event) {
        AbyssEvents.LevelEvent.Unload shared = new AbyssEvents.LevelEvent.Unload(event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LevelEvent.Unload)event).isCanceled(), arg_0 -> ((LevelEvent.Unload)event).setCanceled(arg_0));
        }
        AbyssArchitectureRepair.unload(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventDirectMobGriefing(EntityMobGriefingEvent event) {
        AbyssEvents.EntityMobGriefingEvent shared = new AbyssEvents.EntityMobGriefingEvent(event.getEntity());
        shared.setResult(AbyssEvents.Event.Result.valueOf(event.getResult().name()));
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((EntityMobGriefingEvent)event).isCanceled(), arg_0 -> ((EntityMobGriefingEvent)event).setCanceled(arg_0));
        }
        AbyssBlockProtection.preventDirectMobGriefing(shared);
        event.setResult(Event.Result.valueOf((String)shared.getResult().name()));
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBreaking(BlockEvent.BreakEvent event) {
        AbyssEvents.BlockEvent.BreakEvent shared = new AbyssEvents.BlockEvent.BreakEvent(event.getLevel(), event.getPos(), event.getState(), event.getPlayer());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((BlockEvent.BreakEvent)event).isCanceled(), arg_0 -> ((BlockEvent.BreakEvent)event).setCanceled(arg_0));
        }
        AbyssBlockProtection.preventBreaking(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_protectBossTerrain(LivingDestroyBlockEvent event) {
        AbyssEvents.LivingDestroyBlockEvent shared = new AbyssEvents.LivingDestroyBlockEvent(event.getEntity(), event.getPos(), event.getState());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingDestroyBlockEvent)event).isCanceled(), arg_0 -> ((LivingDestroyBlockEvent)event).setCanceled(arg_0));
        }
        AbyssBlockProtection.protectBossTerrain(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventPlacement(BlockEvent.EntityPlaceEvent event) {
        AbyssEvents.BlockEvent.EntityPlaceEvent shared = new AbyssEvents.BlockEvent.EntityPlaceEvent(event.getLevel(), event.getPos(), event.getPlacedBlock(), event.getEntity());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((BlockEvent.EntityPlaceEvent)event).isCanceled(), arg_0 -> ((BlockEvent.EntityPlaceEvent)event).setCanceled(arg_0));
        }
        AbyssBlockProtection.preventPlacement(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBlockItemUse(PlayerInteractEvent.RightClickBlock event) {
        AbyssEvents.PlayerInteractEvent.RightClickBlock shared = new AbyssEvents.PlayerInteractEvent.RightClickBlock(event.getEntity(), event.getItemStack(), event.getPos(), event.getFace(), null);
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerInteractEvent.RightClickBlock)event).isCanceled(), arg_0 -> ((PlayerInteractEvent.RightClickBlock)event).setCanceled(arg_0));
        }
        AbyssBlockProtection.preventBlockItemUse(shared);
        if (shared.isCanceled()) {
            event.setCancellationResult(shared.getCancellationResult());
        }
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBucketUse(PlayerInteractEvent.RightClickItem event) {
        AbyssEvents.PlayerInteractEvent.RightClickItem shared = new AbyssEvents.PlayerInteractEvent.RightClickItem(event.getEntity(), event.getItemStack(), event.getPos(), event.getFace(), null);
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerInteractEvent.RightClickItem)event).isCanceled(), arg_0 -> ((PlayerInteractEvent.RightClickItem)event).setCanceled(arg_0));
        }
        AbyssBlockProtection.preventBucketUse(shared);
        if (shared.isCanceled()) {
            event.setCancellationResult(shared.getCancellationResult());
        }
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBucketFill(FillBucketEvent event) {
        AbyssEvents.FillBucketEvent shared = new AbyssEvents.FillBucketEvent(event.getEntity());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((FillBucketEvent)event).isCanceled(), arg_0 -> ((FillBucketEvent)event).setCanceled(arg_0));
        }
        AbyssBlockProtection.preventBucketFill(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preserveArenaDuringExplosion(ExplosionEvent.Detonate event) {
        AbyssEvents.ExplosionEvent.Detonate shared = new AbyssEvents.ExplosionEvent.Detonate(event.getLevel(), event.getAffectedBlocks());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ExplosionEvent.Detonate)event).isCanceled(), arg_0 -> ((ExplosionEvent.Detonate)event).setCanceled(arg_0));
        }
        AbyssBlockProtection.preserveArenaDuringExplosion(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void AbyssMobDrops_auxiliarySpawn(EntityJoinLevelEvent event) {
        AbyssEvents.EntityJoinLevelEvent shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((EntityJoinLevelEvent)event).isCanceled(), arg_0 -> ((EntityJoinLevelEvent)event).setCanceled(arg_0));
        }
        AbyssMobDrops.auxiliarySpawn(shared);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void AbyssMobDrops_auxiliaryInteract(PlayerInteractEvent.EntityInteract event) {
        AbyssEvents.PlayerInteractEvent.EntityInteract shared = new AbyssEvents.PlayerInteractEvent.EntityInteract(event.getEntity(), event.getItemStack(), event.getPos(), event.getFace(), event.getTarget());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerInteractEvent.EntityInteract)event).isCanceled(), arg_0 -> ((PlayerInteractEvent.EntityInteract)event).setCanceled(arg_0));
        }
        AbyssMobDrops.auxiliaryInteract(shared);
        if (shared.isCanceled()) {
            event.setCancellationResult(shared.getCancellationResult());
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void AbyssMobDrops_auxiliaryInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        AbyssEvents.PlayerInteractEvent.EntityInteractSpecific shared = new AbyssEvents.PlayerInteractEvent.EntityInteractSpecific(event.getEntity(), event.getItemStack(), event.getPos(), event.getFace(), event.getTarget());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerInteractEvent.EntityInteractSpecific)event).isCanceled(), arg_0 -> ((PlayerInteractEvent.EntityInteractSpecific)event).setCanceled(arg_0));
        }
        AbyssMobDrops.auxiliaryInteractAt(shared);
        if (shared.isCanceled()) {
            event.setCancellationResult(shared.getCancellationResult());
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void AbyssMobDrops_drops(LivingDropsEvent event) {
        AbyssEvents.LivingDropsEvent shared = new AbyssEvents.LivingDropsEvent(event.getEntity(), event.getDrops());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingDropsEvent)event).isCanceled(), arg_0 -> ((LivingDropsEvent)event).setCanceled(arg_0));
        }
        AbyssMobDrops.drops(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void AbyssMobDrops_experience(LivingExperienceDropEvent event) {
        AbyssEvents.LivingExperienceDropEvent shared = new AbyssEvents.LivingExperienceDropEvent(event.getEntity(), event.getDroppedExperience());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingExperienceDropEvent)event).isCanceled(), arg_0 -> ((LivingExperienceDropEvent)event).setCanceled(arg_0));
        }
        shared.bindDroppedExperience(() -> ((LivingExperienceDropEvent)event).getDroppedExperience());
        AbyssMobDrops.experience(shared);
        if (shared.droppedExperience != event.getDroppedExperience()) {
            event.setDroppedExperience(shared.droppedExperience);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void AbyssMobDrops_thrownItem(EntityJoinLevelEvent event) {
        AbyssEvents.EntityJoinLevelEvent shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((EntityJoinLevelEvent)event).isCanceled(), arg_0 -> ((EntityJoinLevelEvent)event).setCanceled(arg_0));
        }
        AbyssMobDrops.thrownItem(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void AbyssNaturalSpawns_placement(MobSpawnEvent.SpawnPlacementCheck event) {
        AbyssEvents.MobSpawnEvent.SpawnPlacementCheck shared = new AbyssEvents.MobSpawnEvent.SpawnPlacementCheck(event.getLevel(), event.getSpawnType());
        shared.setResult(AbyssEvents.Event.Result.valueOf(event.getResult().name()));
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((MobSpawnEvent.SpawnPlacementCheck)event).isCanceled(), arg_0 -> ((MobSpawnEvent.SpawnPlacementCheck)event).setCanceled(arg_0));
        }
        AbyssNaturalSpawns.placement(shared);
        event.setResult(Event.Result.valueOf((String)shared.getResult().name()));
    }

    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void AbyssNaturalSpawns_finalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        AbyssEvents.MobSpawnEvent.FinalizeSpawn shared = new AbyssEvents.MobSpawnEvent.FinalizeSpawn(event.getLevel(), event.getSpawnType());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((MobSpawnEvent.FinalizeSpawn)event).isCanceled(), arg_0 -> ((MobSpawnEvent.FinalizeSpawn)event).setCanceled(arg_0));
        }
        AbyssNaturalSpawns.finalizeSpawn(shared);
        if (shared.isSpawnCancelled()) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void AbyssPlayerLifecycle_login(PlayerEvent.PlayerLoggedInEvent event) {
        AbyssEvents.PlayerEvent.PlayerLoggedInEvent shared = new AbyssEvents.PlayerEvent.PlayerLoggedInEvent(event.getEntity());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerEvent.PlayerLoggedInEvent)event).isCanceled(), arg_0 -> ((PlayerEvent.PlayerLoggedInEvent)event).setCanceled(arg_0));
        }
        AbyssPlayerLifecycle.login(shared);
    }

    @SubscribeEvent
    public static void AbyssPlayerLifecycle_logout(PlayerEvent.PlayerLoggedOutEvent event) {
        AbyssEvents.PlayerEvent.PlayerLoggedOutEvent shared = new AbyssEvents.PlayerEvent.PlayerLoggedOutEvent(event.getEntity());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerEvent.PlayerLoggedOutEvent)event).isCanceled(), arg_0 -> ((PlayerEvent.PlayerLoggedOutEvent)event).setCanceled(arg_0));
        }
        AbyssPlayerLifecycle.logout(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void AbyssRewardCarriers_join(EntityJoinLevelEvent event) {
        AbyssEvents.EntityJoinLevelEvent shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((EntityJoinLevelEvent)event).isCanceled(), arg_0 -> ((EntityJoinLevelEvent)event).setCanceled(arg_0));
        }
        AbyssRewardCarriers.join(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void AbyssRewardCarriers_deathExplosion(ExplosionEvent.Start event) {
        AbyssEvents.ExplosionEvent.Start shared = new AbyssEvents.ExplosionEvent.Start(event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ExplosionEvent.Start)event).isCanceled(), arg_0 -> ((ExplosionEvent.Start)event).setCanceled(arg_0));
        }
        AbyssRewardCarriers.deathExplosion(shared);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void AbyssRewardCarriers_tick(LivingEvent.LivingTickEvent event) {
        AbyssEvents.LivingEvent.LivingTickEvent shared = new AbyssEvents.LivingEvent.LivingTickEvent(event.getEntity());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingEvent.LivingTickEvent)event).isCanceled(), arg_0 -> ((LivingEvent.LivingTickEvent)event).setCanceled(arg_0));
        }
        AbyssRewardCarriers.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssVoidMigration_load(ChunkEvent.Load event) {
        AbyssEvents.ChunkEvent.Load shared = new AbyssEvents.ChunkEvent.Load(event.getLevel(), event.getChunk());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ChunkEvent.Load)event).isCanceled(), arg_0 -> ((ChunkEvent.Load)event).setCanceled(arg_0));
        }
        AbyssVoidMigration.load(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void BossDeathBlocks_joined(EntityJoinLevelEvent event) {
        AbyssEvents.EntityJoinLevelEvent shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((EntityJoinLevelEvent)event).isCanceled(), arg_0 -> ((EntityJoinLevelEvent)event).setCanceled(arg_0));
        }
        BossDeathBlocks.joined(shared);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST, receiveCanceled=true)
    public static void BossDeathBlocks_died(LivingDeathEvent event) {
        AbyssEvents.LivingDeathEvent shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingDeathEvent)event).isCanceled(), arg_0 -> ((LivingDeathEvent)event).setCanceled(arg_0));
        }
        BossDeathBlocks.died(shared);
    }

    @SubscribeEvent
    public static void BossDeathBlocks_tick(TickEvent.LevelTickEvent event) {
        AbyssEvents.TickEvent.LevelTickEvent shared = new AbyssEvents.TickEvent.LevelTickEvent(event.level, AbyssEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((TickEvent.LevelTickEvent)event).isCanceled(), arg_0 -> ((TickEvent.LevelTickEvent)event).setCanceled(arg_0));
        }
        BossDeathBlocks.tick(shared);
    }

    @SubscribeEvent
    public static void BossDeathBlocks_unload(LevelEvent.Unload event) {
        AbyssEvents.LevelEvent.Unload shared = new AbyssEvents.LevelEvent.Unload(event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LevelEvent.Unload)event).isCanceled(), arg_0 -> ((LevelEvent.Unload)event).setCanceled(arg_0));
        }
        BossDeathBlocks.unload(shared);
    }

    @SubscribeEvent
    public static void RareRewardBroadcast_pickedUp(PlayerEvent.ItemPickupEvent event) {
        AbyssEvents.PlayerEvent.ItemPickupEvent shared = new AbyssEvents.PlayerEvent.ItemPickupEvent(event.getEntity(), event.getOriginalEntity(), event.getStack());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerEvent.ItemPickupEvent)event).isCanceled(), arg_0 -> ((PlayerEvent.ItemPickupEvent)event).setCanceled(arg_0));
        }
        RareRewardBroadcast.pickedUp(shared);
    }

    @SubscribeEvent
    public static void BossProgress_login(PlayerEvent.PlayerLoggedInEvent event) {
        AbyssEvents.PlayerEvent.PlayerLoggedInEvent shared = new AbyssEvents.PlayerEvent.PlayerLoggedInEvent(event.getEntity());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerEvent.PlayerLoggedInEvent)event).isCanceled(), arg_0 -> ((PlayerEvent.PlayerLoggedInEvent)event).setCanceled(arg_0));
        }
        BossProgress.login(shared);
    }

    @SubscribeEvent
    public static void BossProgress_stopped(ServerStoppedEvent event) {
        AbyssEvents.ServerStoppedEvent shared = new AbyssEvents.ServerStoppedEvent(event.getServer());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ServerStoppedEvent)event).isCanceled(), arg_0 -> ((ServerStoppedEvent)event).setCanceled(arg_0));
        }
        BossProgress.stopped(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_damage(LivingDamageEvent event) {
        AbyssEvents.LivingDamageEvent shared = new AbyssEvents.LivingDamageEvent(event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingDamageEvent)event).isCanceled(), arg_0 -> ((LivingDamageEvent)event).setCanceled(arg_0));
        }
        shared.bindAmount(() -> ((LivingDamageEvent)event).getAmount());
        ChallengeEvents.damage(shared);
        if (shared.amount != event.getAmount()) {
            event.setAmount(shared.amount);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_death(LivingDeathEvent event) {
        AbyssEvents.LivingDeathEvent shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingDeathEvent)event).isCanceled(), arg_0 -> ((LivingDeathEvent)event).setCanceled(arg_0));
        }
        ChallengeEvents.death(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_projectile(EntityJoinLevelEvent event) {
        AbyssEvents.EntityJoinLevelEvent shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((EntityJoinLevelEvent)event).isCanceled(), arg_0 -> ((EntityJoinLevelEvent)event).setCanceled(arg_0));
        }
        ChallengeEvents.projectile(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_impact(ProjectileImpactEvent event) {
        AbyssEvents.ProjectileImpactEvent shared = new AbyssEvents.ProjectileImpactEvent(event.getProjectile(), event.getRayTraceResult());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ProjectileImpactEvent)event).isCanceled(), arg_0 -> ((ProjectileImpactEvent)event).setCanceled(arg_0));
        }
        shared.bindImpact(() -> AbyssEvents.ProjectileImpactEvent.ImpactResult.valueOf(event.getImpactResult().name()));
        ChallengeEvents.impact(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_totem(LivingUseTotemEvent event) {
        AbyssEvents.LivingUseTotemEvent shared = new AbyssEvents.LivingUseTotemEvent(event.getEntity(), event.getTotem());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingUseTotemEvent)event).isCanceled(), arg_0 -> ((LivingUseTotemEvent)event).setCanceled(arg_0));
        }
        ChallengeEvents.totem(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_shield(ShieldBlockEvent event) {
        AbyssEvents.ShieldBlockEvent shared = new AbyssEvents.ShieldBlockEvent(event.getEntity(), event.getDamageSource(), event.getBlockedDamage());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ShieldBlockEvent)event).isCanceled(), arg_0 -> ((ShieldBlockEvent)event).setCanceled(arg_0));
        }
        shared.bindBlockedDamage(() -> ((ShieldBlockEvent)event).getBlockedDamage());
        ChallengeEvents.shield(shared);
        if (shared.blockedDamage != event.getBlockedDamage()) {
            event.setBlockedDamage(shared.blockedDamage);
        }
    }

    @SubscribeEvent
    public static void ChallengeEvents_equipment(LivingEquipmentChangeEvent event) {
        AbyssEvents.LivingEquipmentChangeEvent shared = new AbyssEvents.LivingEquipmentChangeEvent(event.getEntity(), event.getSlot(), event.getTo());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((LivingEquipmentChangeEvent)event).isCanceled(), arg_0 -> ((LivingEquipmentChangeEvent)event).setCanceled(arg_0));
        }
        ChallengeEvents.equipment(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_useItem(PlayerInteractEvent.RightClickItem event) {
        AbyssEvents.PlayerInteractEvent.RightClickItem shared = new AbyssEvents.PlayerInteractEvent.RightClickItem(event.getEntity(), event.getItemStack(), event.getPos(), event.getFace(), null);
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerInteractEvent.RightClickItem)event).isCanceled(), arg_0 -> ((PlayerInteractEvent.RightClickItem)event).setCanceled(arg_0));
        }
        ChallengeEvents.useItem(shared);
        if (shared.isCanceled()) {
            event.setCancellationResult(shared.getCancellationResult());
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_looseArrow(ArrowLooseEvent event) {
        AbyssEvents.ArrowLooseEvent shared = new AbyssEvents.ArrowLooseEvent(event.getEntity(), event.getBow());
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((ArrowLooseEvent)event).isCanceled(), arg_0 -> ((ArrowLooseEvent)event).setCanceled(arg_0));
        }
        ChallengeEvents.looseArrow(shared);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void ChallengeEvents_useBlock(PlayerInteractEvent.RightClickBlock event) {
        AbyssEvents.PlayerInteractEvent.RightClickBlock shared = new AbyssEvents.PlayerInteractEvent.RightClickBlock(event.getEntity(), event.getItemStack(), event.getPos(), event.getFace(), null);
        if (event.isCancelable()) {
            shared.bindCancellation(() -> ((PlayerInteractEvent.RightClickBlock)event).isCanceled(), arg_0 -> ((PlayerInteractEvent.RightClickBlock)event).setCanceled(arg_0));
        }
        ChallengeEvents.useBlock(shared);
        if (shared.isCanceled()) {
            event.setCancellationResult(shared.getCancellationResult());
        }
    }
}

