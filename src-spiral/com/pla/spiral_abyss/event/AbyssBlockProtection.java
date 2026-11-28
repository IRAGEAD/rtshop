/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.item.PrimedTnt
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.BucketItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.ArenaBounds;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AbyssBlockProtection {
    private static final TagKey<EntityType<?>> NO_ARENA_GRIEFING = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "no_arena_griefing"));

    private AbyssBlockProtection() {
    }

    public static void preventDirectMobGriefing(AbyssEvents.EntityMobGriefingEvent event) {
        if (event.getEntity().m_9236_().m_46472_().equals(AbyssDimension.LEVEL) && event.getEntity().m_6095_().m_204039_(NO_ARENA_GRIEFING)) {
            event.setResult(AbyssEvents.Event.Result.DENY);
        }
    }

    public static void preventBreaking(AbyssEvents.BlockEvent.BreakEvent event) {
        if (!event.getPlayer().m_7500_() && (BlockInit.isAbyssArchitecture(event.getState().m_60734_()) || AbyssBlockProtection.managedTerrain(event.getLevel(), event.getPos()))) {
            event.setCanceled(true);
        }
    }

    private static boolean managedTerrain(LevelAccessor level, BlockPos pos) {
        ServerLevel server;
        return level instanceof ServerLevel && (server = (ServerLevel)level).m_46472_().equals(AbyssDimension.LEVEL) && AbyssParties.get(server.m_7654_()).sessions().stream().anyMatch(session -> session.controller().bossEnvironments().protects(pos));
    }

    public static void protectBossTerrain(AbyssEvents.LivingDestroyBlockEvent event) {
        if (BlockInit.isAbyssArchitecture(event.getState().m_60734_()) || AbyssBlockProtection.managedTerrain((LevelAccessor)event.getEntity().m_9236_(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static boolean restricted(Player player) {
        return !player.m_7500_() && player.m_9236_().m_46472_().equals(AbyssDimension.LEVEL);
    }

    private static boolean combatHazard(BlockState state) {
        return state.m_60713_(Blocks.f_50077_) || state.m_60713_(Blocks.f_50083_) || state.m_60713_(Blocks.f_50084_);
    }

    private static boolean canUseCombatHazard(Player player, BlockPos position) {
        if (player.m_9236_().f_46443_) {
            return true;
        }
        if (!(player instanceof ServerPlayer)) {
            return false;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        ArenaSession session = AbyssParties.get(serverPlayer.f_8924_).sessionFor(serverPlayer);
        if (session == null || !session.controller().active() || !session.isCombatant(serverPlayer)) {
            return false;
        }
        Vec3 center = session.bounds().center();
        return Math.abs((double)position.m_123341_() + 0.5 - center.f_82479_) <= 30.0 && Math.abs((double)position.m_123343_() + 0.5 - center.f_82481_) <= 30.0 && (double)position.m_123342_() >= 65.0 && (double)position.m_123342_() < 120.0;
    }

    public static void preventPlacement(AbyssEvents.BlockEvent.EntityPlaceEvent event) {
        Player player;
        Entity entity = event.getEntity();
        if (entity instanceof Player && AbyssBlockProtection.restricted(player = (Player)entity) && (!AbyssBlockProtection.combatHazard(event.getPlacedBlock()) || !AbyssBlockProtection.canUseCombatHazard(player, event.getPos()))) {
            event.setCanceled(true);
        }
    }

    public static void preventBlockItemUse(AbyssEvents.PlayerInteractEvent.RightClickBlock event) {
        boolean hazardItem;
        Item item = event.getItemStack().m_41720_();
        boolean bl = hazardItem = item == Items.f_41996_ || item == Items.f_42409_ || item == Items.f_42613_;
        if (AbyssBlockProtection.restricted(event.getEntity()) && hazardItem) {
            BlockPos target = event.getPos();
            if (!event.getLevel().m_8055_(target).m_60713_(Blocks.f_50077_) && event.getFace() != null) {
                target = target.m_121945_(event.getFace());
            }
            if (!AbyssBlockProtection.canUseCombatHazard(event.getEntity(), target)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
            }
            return;
        }
        if (AbyssBlockProtection.restricted(event.getEntity()) && (event.getItemStack().m_41720_() instanceof BlockItem || event.getItemStack().m_41720_() instanceof BucketItem)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    public static void preventBucketUse(AbyssEvents.PlayerInteractEvent.RightClickItem event) {
        if (AbyssBlockProtection.restricted(event.getEntity()) && event.getItemStack().m_41720_() instanceof BucketItem) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    public static void preventBucketFill(AbyssEvents.FillBucketEvent event) {
        if (AbyssBlockProtection.restricted(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void preserveArenaDuringExplosion(AbyssEvents.ExplosionEvent.Detonate event) {
        if (!event.getLevel().m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> !AbyssBlockProtection.combatHazard(event.getLevel().m_8055_(pos)));
    }

    public static void clearCombatHazards(ServerLevel level, ArenaBounds bounds) {
        Vec3 center = bounds.center();
        AABB area = new AABB(center.f_82479_ - 128.0, (double)level.m_141937_(), center.f_82481_ - 128.0, center.f_82479_ + 128.0, (double)level.m_151558_(), center.f_82481_ + 128.0);
        level.m_45976_(PrimedTnt.class, area).forEach(Entity::m_146870_);
        AABB structure = bounds.structureArea();
        BlockPos.m_121990_((BlockPos)BlockPos.m_274561_((double)structure.f_82288_, (double)structure.f_82289_, (double)structure.f_82290_), (BlockPos)BlockPos.m_274561_((double)(structure.f_82291_ - 1.0), (double)(structure.f_82292_ - 1.0), (double)(structure.f_82293_ - 1.0))).forEach(pos -> {
            if (level.m_46805_(pos) && AbyssBlockProtection.combatHazard(level.m_8055_(pos))) {
                level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 3);
            }
        });
    }
}

