/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ExperienceOrb
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.AbstractArrow
 *  net.minecraft.world.entity.projectile.AbstractArrow$Pickup
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.event.AbyssRewardCarriers;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class AbyssMobDrops {
    public static final String NO_PICKUP = "spiral_abyss_no_pickup";

    private AbyssMobDrops() {
    }

    public static boolean suppress(Entity entity) {
        return !entity.m_9236_().f_46443_ && !(entity instanceof Player) && (AbyssPlatform.data(entity).m_128403_("spiral_abyss_environment_owner") || AbyssPlatform.data(entity).m_128403_("spiral_abyss_run") || entity instanceof Mob && (entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) || AbyssPlatform.data(entity).m_128403_("spiral_abyss_run")));
    }

    public static boolean suppressAuxiliary(Entity entity) {
        return !entity.m_9236_().f_46443_ && entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) && !(entity instanceof LivingEntity) && !(entity instanceof ItemEntity) && MobCatalog.suppressesSpawn(AbyssRegistries.ENTITY_TYPES.getKey(entity.m_6095_()));
    }

    public static boolean suppressFallingBlock(Level level, BlockPos pos, BlockState state) {
        ServerLevel serverLevel;
        block7: {
            block6: {
                if (!(level instanceof ServerLevel)) break block6;
                serverLevel = (ServerLevel)level;
                if (level.m_46472_().equals(AbyssDimension.LEVEL)) break block7;
            }
            return false;
        }
        Map<ResourceLocation, Set<ResourceLocation>> targets = MobCatalog.suppressedFallingBlocks();
        if (targets.isEmpty()) {
            return false;
        }
        ResourceLocation block = AbyssRegistries.BLOCKS.getKey(state.m_60734_());
        if (targets.values().stream().noneMatch(blocks -> blocks.contains(block))) {
            return false;
        }
        for (Entity entity : serverLevel.m_8583_()) {
            ResourceLocation owner;
            Mob mob;
            if (!(entity instanceof Mob) || (mob = (Mob)entity).m_6084_() || !mob.m_20183_().equals((Object)pos) || !AbyssPlatform.data((Entity)mob).m_128403_("spiral_abyss_run") || !targets.getOrDefault(owner = AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_()), Set.of()).contains(block)) continue;
            return true;
        }
        return false;
    }

    public static void auxiliarySpawn(AbyssEvents.EntityJoinLevelEvent event) {
        if (AbyssMobDrops.suppressAuxiliary(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void auxiliaryInteract(AbyssEvents.PlayerInteractEvent.EntityInteract event) {
        if (AbyssMobDrops.suppressAuxiliary(event.getTarget()) || AbyssRewardCarriers.suppress(event.getTarget())) {
            event.getTarget().m_146870_();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void auxiliaryInteractAt(AbyssEvents.PlayerInteractEvent.EntityInteractSpecific event) {
        if (AbyssMobDrops.suppressAuxiliary(event.getTarget()) || AbyssRewardCarriers.suppress(event.getTarget())) {
            event.getTarget().m_146870_();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void drops(AbyssEvents.LivingDropsEvent event) {
        if (AbyssMobDrops.suppress((Entity)event.getEntity())) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    public static void experience(AbyssEvents.LivingExperienceDropEvent event) {
        if (AbyssMobDrops.suppress((Entity)event.getEntity())) {
            event.setDroppedExperience(0);
        }
    }

    public static void thrownItem(AbyssEvents.EntityJoinLevelEvent event) {
        ItemEntity item;
        Entity thrower;
        AbstractArrow arrow;
        Entity entity;
        LivingEntity scriptedSource = MobTickContext.scriptedRewardSource();
        if (scriptedSource != null && event.getLevel() == scriptedSource.m_9236_() && (event.getEntity() instanceof ItemEntity || event.getEntity() instanceof ExperienceOrb)) {
            event.setCanceled(true);
            return;
        }
        if (event.getLevel() instanceof ServerLevel && (entity = event.getEntity()) instanceof AbstractArrow && (AbyssPlatform.data((Entity)(arrow = (AbstractArrow)entity)).m_128471_(NO_PICKUP) || arrow.m_19749_() != null && AbyssMobDrops.suppress(arrow.m_19749_()))) {
            arrow.f_36705_ = AbstractArrow.Pickup.DISALLOWED;
            AbyssPlatform.data((Entity)arrow).m_128379_(NO_PICKUP, true);
        }
        if (event.getLevel() instanceof ServerLevel && (entity = event.getEntity()) instanceof ItemEntity && (thrower = (item = (ItemEntity)entity).m_19749_()) != null && AbyssMobDrops.suppress(thrower)) {
            event.setCanceled(true);
        }
    }
}

