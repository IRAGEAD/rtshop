/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.event.TickEvent$LevelTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$PlayerTickEvent
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerChangedDimensionEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$StartTracking
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  net.minecraftforge.event.level.BlockEvent$NeighborNotifyEvent
 *  net.minecraftforge.event.level.ChunkWatchEvent$Watch
 *  net.minecraftforge.event.server.ServerStoppedEvent
 *  net.minecraftforge.eventbus.api.Event$Result
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 */
package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.brewing.BrewingWatch;
import dev.dyslexic.cinematic.campfire.CampfireActions;
import dev.dyslexic.cinematic.furnace.FurnaceWatch;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableActions;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class StationEvents {
    private StationEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ServerPlayer serverPlayer;
        Player player = event.getEntity();
        BlockState state = event.getLevel().m_8055_(event.getPos());
        StationKind kind = StationKind.of(state);
        if (kind == null || !kind.startsOnUse() || event.getHand() != InteractionHand.MAIN_HAND || event.getUseBlock() == Event.Result.DENY || player.m_36341_() || player.m_5833_() || !kind.usable((BlockGetter)event.getLevel(), event.getPos(), state) || !kind.usedOn(state, event.getHitVec().m_82434_())) {
            return;
        }
        if (event.getLevel().m_5776_() ? ClientBridge.vanillaUse.test(event.getPos().m_7949_(), kind) : player instanceof ServerPlayer && !Network.accepts(serverPlayer = (ServerPlayer)player)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (event.getLevel().m_5776_()) {
            ClientBridge.useAt = event.getHitVec().m_82450_();
            ClientBridge.useStation.accept(event.getPos().m_7949_(), kind);
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)levelAccessor;
            if (!TableGrid.isTable(event.getState()) && TableStore.get(level).get(event.getPos()) != null) {
                TableActions.spill(level, event.getPos());
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTickPre(TickEvent.LevelTickEvent event) {
        Object object;
        if (event.phase != TickEvent.Phase.START || !((object = event.level) instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)object;
        for (ServerPlayer player : level.m_6907_()) {
            Sessions.Session session = Sessions.get(player);
            if (session == null || session.kind() != StationKind.CAMPFIRE || !session.dimension().equals((Object)level.m_46472_())) continue;
            CampfireActions.tick(level, session);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        ServerLevel level;
        Level level2;
        if (event.phase != TickEvent.Phase.END || !((level2 = event.level) instanceof ServerLevel) || (level = (ServerLevel)level2).m_46467_() % 40L != 0L) {
            return;
        }
        TableStore store = TableStore.get(level);
        for (BlockPos pos : store.positions()) {
            if (!level.m_46749_(pos) || TableGrid.isTable(level.m_8055_(pos))) continue;
            TableActions.spill(level, pos);
        }
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Watch event) {
        TableStore store = TableStore.get(event.getLevel());
        for (BlockPos pos : store.inChunk(event.getPos())) {
            Network.sendToPlayer(event.getPlayer(), TableActions.contents(pos, store.get(pos)));
        }
        FurnaceWatch.chunkSent(event.getPlayer(), event.getChunk());
        BrewingWatch.chunkSent(event.getPlayer(), event.getChunk());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player;
        if (event.phase == TickEvent.Phase.END && (player = event.player) instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            Sessions.tick(player2);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity entity = event.getTarget();
        if (entity instanceof ServerPlayer) {
            ServerPlayer target = (ServerPlayer)entity;
            entity = event.getEntity();
            if (entity instanceof ServerPlayer) {
                ServerPlayer watcher = (ServerPlayer)entity;
                Sessions.sendAtStation(target, watcher);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            Sessions.end(player2, false);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)livingEntity;
            Sessions.end(player, true);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            Sessions.end(player2, true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Sessions.clear();
    }
}

