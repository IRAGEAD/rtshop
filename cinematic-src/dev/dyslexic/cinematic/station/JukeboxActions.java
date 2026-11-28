/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.stats.Stats
 *  net.minecraft.tags.ItemTags
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.JukeboxBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.JukeboxBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.gameevent.GameEvent
 *  net.minecraft.world.level.gameevent.GameEvent$Context
 */
package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;

public final class JukeboxActions {
    private JukeboxActions() {
    }

    public static void handle(ServerPlayer player, ServerLevel level, Sessions.Session session, StationActionPayload action) {
        BlockEntity blockEntity;
        BlockPos pos = session.pos();
        if (action.action() != StationActionPayload.Action.CLICK || !((blockEntity = level.m_7702_(pos)) instanceof JukeboxBlockEntity)) {
            return;
        }
        JukeboxBlockEntity jukebox = (JukeboxBlockEntity)blockEntity;
        BlockState state = level.m_8055_(pos);
        if (((Boolean)state.m_61143_((Property)JukeboxBlock.f_54254_)).booleanValue()) {
            ItemStack record = jukebox.m_272108_();
            if (!record.m_41619_()) {
                player.m_150109_().m_150079_(record);
            }
            level.m_220407_(GameEvent.f_157792_, pos, GameEvent.Context.m_223719_((Entity)player, (BlockState)state));
        } else {
            ItemStack source;
            ItemStack itemStack = action.index() == 0 ? player.f_36095_.m_142621_() : (source = action.button() >= 0 && action.button() < 36 ? player.m_150109_().m_8020_(action.button()) : ItemStack.f_41583_);
            if (JukeboxActions.isRecord(source)) {
                JukeboxActions.insert(level, pos, state, jukebox, source, player);
                if (action.index() == 0 && source.m_41619_()) {
                    player.f_36095_.m_142503_(ItemStack.f_41583_);
                }
            }
        }
        player.f_36095_.m_38946_();
        JukeboxActions.show(player, session);
    }

    public static void show(ServerPlayer player, Sessions.Session session) {
        ItemStack itemStack;
        ServerLevel level = player.m_284548_();
        BlockEntity blockEntity = level.m_7702_(session.pos());
        if (blockEntity instanceof JukeboxBlockEntity) {
            JukeboxBlockEntity jukebox = (JukeboxBlockEntity)blockEntity;
            itemStack = jukebox.m_272036_().m_41777_();
        } else {
            itemStack = ItemStack.f_41583_;
        }
        ItemStack record = itemStack;
        Network.sendToTrackingChunk(level, new ChunkPos(session.pos()), new MenuDisplayPayload(session.pos(), StationKind.JUKEBOX, session.side().m_122416_(), List.of(record)));
    }

    public static void vanillaUse(ServerPlayer player, BlockPos pos) {
        BlockEntity blockEntity;
        ServerLevel level = player.m_284548_();
        BlockState state = level.m_8055_(pos);
        if (!state.m_60713_(Blocks.f_50131_)) {
            return;
        }
        if (((Boolean)state.m_61143_((Property)JukeboxBlock.f_54254_)).booleanValue()) {
            BlockEntity blockEntity2 = level.m_7702_(pos);
            if (blockEntity2 instanceof JukeboxBlockEntity) {
                JukeboxBlockEntity jukebox = (JukeboxBlockEntity)blockEntity2;
                jukebox.m_272252_();
            }
        } else if (JukeboxActions.isRecord(player.m_21205_()) && (blockEntity = level.m_7702_(pos)) instanceof JukeboxBlockEntity) {
            JukeboxBlockEntity jukebox = (JukeboxBlockEntity)blockEntity;
            JukeboxActions.insert(level, pos, state, jukebox, player.m_21205_(), player);
        }
    }

    public static boolean isRecord(ItemStack stack) {
        return stack.m_204117_(ItemTags.f_13158_);
    }

    private static void insert(ServerLevel level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox, ItemStack source, ServerPlayer player) {
        jukebox.m_272287_(source.m_255036_(1));
        level.m_220407_(GameEvent.f_157792_, pos, GameEvent.Context.m_223719_((Entity)player, (BlockState)state));
        if (!player.m_150110_().f_35937_) {
            source.m_41774_(1);
        }
        player.m_36220_(Stats.f_12965_);
    }
}

