/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.stats.Stats
 *  net.minecraft.tags.ItemTags
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.block.ChiseledBookShelfBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.gameevent.GameEvent
 */
package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.Compat;
import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;

public final class ShelfActions {
    private static final Map<UUID, Integer> SHOWN = new HashMap<UUID, Integer>();
    private static final Map<UUID, Integer> READING = new HashMap<UUID, Integer>();

    private ShelfActions() {
    }

    public static void handle(ServerPlayer player, ServerLevel level, Sessions.Session session, StationActionPayload action) {
        BlockEntity blockEntity;
        BlockPos pos = session.pos();
        int slot = action.index();
        if (slot < 0 || slot >= 6 || !((blockEntity = level.m_7702_(pos)) instanceof ChiseledBookShelfBlockEntity)) {
            return;
        }
        ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)blockEntity;
        switch (action.action()) {
            case CLICK: {
                ShelfActions.take(player, level, session, shelf, slot, action.shift());
                break;
            }
            case PLACE: {
                ShelfActions.put(player, level, session, shelf, slot, action.button());
                break;
            }
            case CRAFT: {
                boolean start;
                boolean bl = start = action.button() == 1 && !shelf.m_8020_(slot).m_41619_();
                if (start) {
                    READING.put(player.m_20148_(), slot);
                } else if (READING.remove(player.m_20148_()) == null) {
                    return;
                }
                ShelfActions.cue(player, session, start ? MenuFxPayload.Cue.READ : MenuFxPayload.Cue.READ_END, slot, shelf.m_8020_(slot));
                break;
            }
        }
    }

    private static void take(ServerPlayer player, ServerLevel level, Sessions.Session session, ChiseledBookShelfBlockEntity shelf, int slot, boolean toInventory) {
        boolean fits;
        ItemStack there = shelf.m_8020_(slot);
        ItemStack carried = player.f_36095_.m_142621_();
        boolean bl = fits = carried.m_41619_() || ItemStack.m_150942_((ItemStack)carried, (ItemStack)there) && carried.m_41613_() < carried.m_41741_();
        if (there.m_41619_() || !toInventory && !fits) {
            ShelfActions.show(player, session);
            return;
        }
        ItemStack book = shelf.m_7407_(slot, 1);
        level.m_5594_(null, session.pos(), ShelfActions.sound(book, false), SoundSource.BLOCKS, 1.0f, 1.0f);
        if (Integer.valueOf(slot).equals(READING.get(player.m_20148_()))) {
            READING.remove(player.m_20148_());
        }
        ShelfActions.cue(player, session, MenuFxPayload.Cue.PULL, slot, book);
        if (toInventory) {
            player.m_150109_().m_150079_(book);
        } else if (carried.m_41619_()) {
            player.f_36095_.m_142503_(book);
        } else {
            carried.m_41769_(1);
        }
        player.f_36095_.m_38946_();
        ShelfActions.show(player, session);
    }

    private static void put(ServerPlayer player, ServerLevel level, Sessions.Session session, ChiseledBookShelfBlockEntity shelf, int slot, int from) {
        ItemStack source;
        ItemStack itemStack = from < 0 ? player.f_36095_.m_142621_() : (source = from < 36 ? player.m_150109_().m_8020_(from) : ItemStack.f_41583_);
        if (source.m_41619_() || !source.m_204117_(ItemTags.f_244646_) || !shelf.m_8020_(slot).m_41619_()) {
            ShelfActions.show(player, session);
            return;
        }
        player.m_36246_(Stats.f_12982_.m_12902_((Object)source.m_41720_()));
        ItemStack book = source.m_41620_(1);
        if (from < 0 && source.m_41619_()) {
            player.f_36095_.m_142503_(ItemStack.f_41583_);
        }
        shelf.m_6836_(slot, book);
        level.m_5594_(null, session.pos(), ShelfActions.sound(book, true), SoundSource.BLOCKS, 1.0f, 1.0f);
        ShelfActions.cue(player, session, MenuFxPayload.Cue.PUT, slot, book);
        player.f_36095_.m_38946_();
        ShelfActions.show(player, session);
    }

    private static SoundEvent sound(ItemStack book, boolean in) {
        boolean enchanted = book.m_150930_(Items.f_42690_);
        return in ? (enchanted ? SoundEvents.f_256863_ : SoundEvents.f_256961_) : (enchanted ? SoundEvents.f_257000_ : SoundEvents.f_256717_);
    }

    private static void cue(ServerPlayer player, Sessions.Session session, MenuFxPayload.Cue cue, int slot, ItemStack book) {
        Network.sendToTrackingChunk(player.m_284548_(), new ChunkPos(session.pos()), new MenuFxPayload(session.pos(), StationKind.CHISELED_BOOKSHELF, player.m_19879_(), cue, slot, List.of(), book.m_41777_()));
    }

    public static void show(ServerPlayer player, Sessions.Session session) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(6);
        BlockEntity blockEntity = player.m_284548_().m_7702_(session.pos());
        if (blockEntity instanceof ChiseledBookShelfBlockEntity) {
            ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)blockEntity;
            for (int i = 0; i < 6; ++i) {
                items.add(shelf.m_8020_(i).m_41777_());
            }
        }
        SHOWN.put(player.m_20148_(), ShelfActions.fingerprint(items));
        Network.sendToPlayer(player, new MenuDisplayPayload(session.pos(), StationKind.CHISELED_BOOKSHELF, session.side().m_122416_(), items));
    }

    public static void tick(ServerPlayer player, Sessions.Session session) {
        BlockEntity blockEntity = player.m_284548_().m_7702_(session.pos());
        if (!(blockEntity instanceof ChiseledBookShelfBlockEntity)) {
            return;
        }
        ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)blockEntity;
        int fingerprint = 1;
        for (int i = 0; i < 6; ++i) {
            ItemStack stack = shelf.m_8020_(i);
            fingerprint = fingerprint * 31 + (stack.m_41619_() ? 0 : Compat.hashItem(stack));
        }
        Integer last = SHOWN.get(player.m_20148_());
        if (last == null || last != fingerprint) {
            ShelfActions.show(player, session);
        }
    }

    private static int fingerprint(List<ItemStack> items) {
        int fingerprint = 1;
        for (ItemStack stack : items) {
            fingerprint = fingerprint * 31 + (stack.m_41619_() ? 0 : Compat.hashItem(stack));
        }
        return fingerprint;
    }

    public static void close(ServerPlayer player, Sessions.Session session) {
        BlockEntity blockEntity;
        SHOWN.remove(player.m_20148_());
        Integer slot = READING.remove(player.m_20148_());
        if (slot != null && (blockEntity = player.m_284548_().m_7702_(session.pos())) instanceof ChiseledBookShelfBlockEntity) {
            ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)blockEntity;
            ShelfActions.cue(player, session, MenuFxPayload.Cue.READ_END, slot, shelf.m_8020_(slot.intValue()));
        }
    }

    public static void vanillaUse(ServerPlayer player, BlockPos pos, int slot) {
        BlockEntity blockEntity;
        ServerLevel level = player.m_284548_();
        BlockState state = level.m_8055_(pos);
        if (slot < 0 || slot >= 6 || !(state.m_60734_() instanceof ChiseledBookShelfBlock) || !((blockEntity = level.m_7702_(pos)) instanceof ChiseledBookShelfBlockEntity)) {
            return;
        }
        ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)blockEntity;
        if (((Boolean)state.m_61143_((Property)ChiseledBookShelfBlock.f_260698_.get(slot))).booleanValue()) {
            ItemStack book = shelf.m_7407_(slot, 1);
            level.m_5594_(null, pos, ShelfActions.sound(book, false), SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.m_150109_().m_36054_(book)) {
                player.m_36176_(book, false);
            }
            level.m_142346_((Entity)player, GameEvent.f_157792_, pos);
        } else if (player.m_21205_().m_204117_(ItemTags.f_244646_)) {
            ItemStack hand = player.m_21205_();
            player.m_36246_(Stats.f_12982_.m_12902_((Object)hand.m_41720_()));
            ItemStack book = player.m_150110_().f_35937_ ? hand.m_255036_(1) : hand.m_41620_(1);
            shelf.m_6836_(slot, book);
            level.m_5594_(null, pos, ShelfActions.sound(book, true), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}

