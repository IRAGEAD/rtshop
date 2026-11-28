/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.InventoryMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.api.AddonStations;
import dev.dyslexic.cinematic.campfire.CampfireActions;
import dev.dyslexic.cinematic.enchant.EnchantActions;
import dev.dyslexic.cinematic.furnace.FurnaceActions;
import dev.dyslexic.cinematic.net.AtStationPayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.SessionClosePayload;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.JukeboxActions;
import dev.dyslexic.cinematic.station.MenuActions;
import dev.dyslexic.cinematic.station.RecipeChoices;
import dev.dyslexic.cinematic.station.ShelfActions;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableActions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class Sessions {
    private static final Map<UUID, Session> SESSIONS = new HashMap<UUID, Session>();
    private static final double REACH_SQR = 36.0;

    private Sessions() {
    }

    @Nullable
    public static Session get(ServerPlayer player) {
        return SESSIONS.get(player.m_20148_());
    }

    public static void handle(ServerPlayer player, StationActionPayload action) {
        if (action.action() == StationActionPayload.Action.BEGIN) {
            Sessions.begin(player, action.pos(), action.kind(), Direction.m_122407_((int)action.side()));
            return;
        }
        if (action.action() == StationActionPayload.Action.WINDOW) {
            Sessions.openWindow(player, action.pos(), action.kind(), action.index());
            return;
        }
        Session session = Sessions.get(player);
        if (session == null || !session.pos().equals((Object)action.pos()) || session.kind() != action.kind()) {
            if (action.action() != StationActionPayload.Action.LEAVE) {
                Network.sendToPlayer(player, new SessionClosePayload());
            }
            return;
        }
        if (action.action() == StationActionPayload.Action.LEAVE) {
            Sessions.end(player, false);
            return;
        }
        if (!Sessions.valid(player, session, true)) {
            Sessions.end(player, true);
            return;
        }
        ServerLevel level = player.m_284548_();
        if (action.action() == StationActionPayload.Action.CHOOSE) {
            RecipeChoices.choose(player, level, session, action.index());
            return;
        }
        if (action.action() == StationActionPayload.Action.QUICK) {
            Sessions.quickMove(player, level, session, action.index());
            return;
        }
        switch (session.kind()) {
            case TABLE: {
                TableActions.handle(player, level, session, action);
                break;
            }
            case FURNACE: 
            case SMOKER: 
            case BLAST_FURNACE: {
                FurnaceActions.handle(player, level, session, action);
                break;
            }
            case ENCHANTING: {
                break;
            }
            case ANVIL: 
            case GRINDSTONE: 
            case SMITHING: 
            case STONECUTTER: 
            case LOOM: 
            case CARTOGRAPHY: 
            case BREWING: 
            case LECTERN: 
            case BEACON: {
                MenuActions.handle(player, session, action);
                break;
            }
            case CAMPFIRE: {
                CampfireActions.handle(player, level, session, action);
                break;
            }
            case JUKEBOX: {
                JukeboxActions.handle(player, level, session, action);
                break;
            }
            case CHISELED_BOOKSHELF: {
                ShelfActions.handle(player, level, session, action);
                break;
            }
        }
    }

    private static void quickMove(ServerPlayer player, ServerLevel level, Session session, int slot) {
        if (slot < 0 || slot >= 36 || player.m_150109_().m_8020_(slot).m_41619_()) {
            return;
        }
        if (session.kind().usesMenu()) {
            MenuActions.quickMove(player, session, slot);
        } else if (session.kind().isFurnace()) {
            FurnaceActions.quickMove(player, level, session, slot);
        } else if (session.kind() == StationKind.CAMPFIRE) {
            CampfireActions.quickMove(player, level, session, slot);
        }
    }

    private static void begin(ServerPlayer player, BlockPos pos, StationKind kind, Direction side) {
        Session session;
        AddonStations.Definition addon;
        BlockState state = player.m_9236_().m_8055_(pos);
        if (!kind.matches(state)) {
            Network.sendToPlayer(player, new SessionClosePayload());
            return;
        }
        AddonStations.Definition definition = addon = kind == StationKind.ADDON ? AddonStations.forBlock(state) : null;
        if (kind == StationKind.ADDON && addon == null) {
            Network.sendToPlayer(player, new SessionClosePayload());
            return;
        }
        List<Direction> sides = kind.sides(state);
        if (sides.isEmpty()) {
            Network.sendToPlayer(player, new SessionClosePayload());
            return;
        }
        if (!sides.contains(side)) {
            side = sides.get(0);
        }
        if (!Sessions.valid(player, session = new Session((ResourceKey<Level>)player.m_9236_().m_46472_(), pos.m_7949_(), side, kind, addon == null ? null : addon.id()), false) || !kind.usable((BlockGetter)player.m_9236_(), pos, state)) {
            Network.sendToPlayer(player, new SessionClosePayload());
            return;
        }
        if (Sessions.get(player) != null) {
            Sessions.end(player, false);
        }
        if (kind.usesMenu() && !MenuActions.open(player, session)) {
            Network.sendToPlayer(player, new SessionClosePayload());
            return;
        }
        SESSIONS.put(player.m_20148_(), session);
        Network.sendToTrackingEntityAndSelf((Entity)player, new AtStationPayload(player.m_19879_(), Optional.of(session.pos()), kind));
        if (kind.isFurnace()) {
            FurnaceActions.sendState(player, session);
        }
        if (kind == StationKind.CAMPFIRE) {
            CampfireActions.sendProgress(player, session);
        }
        if (kind == StationKind.JUKEBOX) {
            JukeboxActions.show(player, session);
        }
        if (kind == StationKind.CHISELED_BOOKSHELF) {
            ShelfActions.show(player, session);
        }
    }

    private static void openWindow(ServerPlayer player, BlockPos pos, StationKind kind, int slot) {
        if (Sessions.get(player) != null) {
            Sessions.end(player, false);
        }
        BlockState state = player.m_9236_().m_8055_(pos);
        if (player.m_5833_() || !kind.matches(state) || player.m_20238_(pos.m_252807_()) > 36.0) {
            return;
        }
        if (kind == StationKind.JUKEBOX) {
            JukeboxActions.vanillaUse(player, pos);
            return;
        }
        if (kind == StationKind.CHISELED_BOOKSHELF) {
            ShelfActions.vanillaUse(player, pos, slot);
            return;
        }
        MenuProvider provider = MenuActions.provider(player, pos, kind);
        if (provider != null) {
            player.m_5893_(provider);
        }
    }

    public static void end(ServerPlayer player, boolean notifyClient) {
        InventoryMenu menu;
        ItemStack carried;
        Session session = SESSIONS.remove(player.m_20148_());
        if (session == null) {
            return;
        }
        if (session.kind().usesMenu()) {
            MenuActions.close(player, session);
        }
        if (session.kind() == StationKind.ENCHANTING) {
            EnchantActions.close(player, session);
        }
        if (session.kind() == StationKind.CHISELED_BOOKSHELF) {
            ShelfActions.close(player, session);
        }
        if (!(carried = (menu = player.f_36095_).m_142621_()).m_41619_()) {
            menu.m_142503_(ItemStack.f_41583_);
            player.m_150109_().m_150079_(carried);
        }
        menu.m_38946_();
        if (notifyClient) {
            Network.sendToPlayer(player, new SessionClosePayload());
        }
        Network.sendToTrackingEntityAndSelf((Entity)player, new AtStationPayload(player.m_19879_(), Optional.empty(), session.kind()));
    }

    public static void tick(ServerPlayer player) {
        Session session = Sessions.get(player);
        if (session == null) {
            return;
        }
        if ((session.kind().isFurnace() || player.f_19797_ % 5 == 0) && !Sessions.valid(player, session, true)) {
            Sessions.end(player, true);
            return;
        }
        if (session.kind().isFurnace() && player.f_19797_ % 4 == 0) {
            FurnaceActions.sendState(player, session);
        }
        if (session.kind() == StationKind.CAMPFIRE && player.f_19797_ % 4 == 0) {
            CampfireActions.sendProgress(player, session);
        }
        if (session.kind() == StationKind.CHISELED_BOOKSHELF && player.f_19797_ % 4 == 0) {
            ShelfActions.tick(player, session);
        }
        if (session.kind() == StationKind.ENCHANTING) {
            EnchantActions.watch(player, session);
        }
        if (session.kind() == StationKind.ENCHANTING && player.f_19797_ % 2 == 0) {
            EnchantActions.tick(player, session);
        } else if (session.kind().usesMenu() && player.f_19797_ % 2 == 0) {
            MenuActions.tick(player, session);
        }
    }

    public static void endAllAt(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.m_6907_()) {
            Session session = Sessions.get(player);
            if (session == null || !session.pos().equals((Object)pos)) continue;
            Sessions.end(player, true);
        }
    }

    public static void clear() {
        SESSIONS.clear();
    }

    public static void sendAtStation(ServerPlayer target, ServerPlayer watcher) {
        Session session = Sessions.get(target);
        if (session != null) {
            Network.sendToPlayer(watcher, new AtStationPayload(target.m_19879_(), Optional.of(session.pos()), session.kind()));
        }
    }

    private static boolean valid(ServerPlayer player, Session session, boolean opened) {
        AddonStations.Definition addon;
        if (!player.m_9236_().m_46749_(session.pos())) {
            return false;
        }
        AddonStations.Definition definition = addon = session.kind() == StationKind.ADDON ? AddonStations.forBlock(player.m_9236_().m_8055_(session.pos())) : null;
        boolean menu = session.kind().usesMenu() && opened ? MenuActions.isOpen(session, player.f_36096_) && (session.kind() != StationKind.ADDON || player.f_36096_.m_6875_((Player)player)) : player.f_36096_ == player.f_36095_;
        return player.m_6084_() && !player.m_5833_() && player.m_9236_().m_46472_() == session.dimension() && menu && session.kind().matches(player.m_9236_().m_8055_(session.pos())) && (session.kind() != StationKind.ADDON || addon != null && addon.id().equals((Object)session.addonId())) && player.m_20238_(session.pos().m_252807_()) <= 36.0;
    }

    public record Session(ResourceKey<Level> dimension, BlockPos pos, Direction side, StationKind kind, @Nullable ResourceLocation addonId) {
        public Session(ResourceKey<Level> dimension, BlockPos pos, Direction side, StationKind kind) {
            this(dimension, pos, side, kind, null);
        }
    }
}

