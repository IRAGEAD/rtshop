/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.encounter.FloorProgress;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import com.pla.spiral_abyss.world.ArenaSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class PortalParties {
    private static final int MENU_TIMEOUT_TICKS = 2400;
    private final AbyssParties parties;
    private final MinecraftServer server;
    private final Map<UUID, Selection> selections = new HashMap<UUID, Selection>();
    private final Map<UUID, ArenaMenu> arenaMenus = new HashMap<UUID, ArenaMenu>();

    public PortalParties(AbyssParties parties, MinecraftServer server) {
        this.parties = parties;
        this.server = server;
    }

    public void invalidateAll() {
        this.selections.clear();
        this.arenaMenus.clear();
    }

    public void invalidate(ServerPlayer player) {
        this.selections.remove(player.m_20148_());
        this.arenaMenus.remove(player.m_20148_());
    }

    public ArenaSession boundSession(AbyssPortalBlockEntity portal) {
        ArenaSession bound;
        ArenaSession arenaSession = bound = portal.host() == null ? null : this.parties.hostedBy(portal.host());
        if (bound != null && portal.portalId().equals(bound.profile().portal) && !bound.players().isEmpty()) {
            return bound;
        }
        return this.parties.sessions().stream().filter(s -> portal.portalId().equals(s.profile().portal) && !s.players().isEmpty()).findFirst().orElse(null);
    }

    public void touch(ServerPlayer player, AbyssPortalFrame frame) {
        if (!player.m_6084_() || player.m_5833_() || player.m_9232_() || !frame.active(player.m_9236_())) {
            return;
        }
        BlockEntity blockEntity = player.m_9236_().m_7702_(frame.anchor());
        if (!(blockEntity instanceof AbyssPortalBlockEntity)) {
            return;
        }
        AbyssPortalBlockEntity portal = (AbyssPortalBlockEntity)blockEntity;
        this.tick();
        UUID token = UUID.randomUUID();
        this.selections.put(player.m_20148_(), new Selection(token, portal.portalId(), player.m_20148_(), (ResourceKey<Level>)player.m_9236_().m_46472_(), frame.anchor(), frame.returnPosition(player), this.server.m_129921_() + 2400));
        AbyssNetwork.openFloors(player, token, this.parties.profile((UUID)player.m_20148_()).progress.masteredMask(), this.floorChoices(player), 254, false, frame.anchor());
    }

    public boolean openArenaMenu(ServerPlayer player) {
        if (!player.m_6084_() || player.m_9232_()) {
            return false;
        }
        ArenaSession session = this.parties.sessionFor(player);
        if (session == null) {
            return false;
        }
        UUID token = UUID.randomUUID();
        UUID following = this.parties.followedHost(player);
        int available = 0;
        if (session.canNavigate(player)) {
            available = this.floorChoices(player);
        }
        this.arenaMenus.put(player.m_20148_(), new ArenaMenu(token, session.host(), session.controller().currentFloor(), following, this.parties.profile((UUID)player.m_20148_()).personalRun, available, this.server.m_129921_() + 2400));
        AbyssNetwork.openFloors(player, token, this.parties.profile((UUID)player.m_20148_()).progress.masteredMask(), available, 254, true);
        return true;
    }

    public List<AbyssNetwork.SectionView> sectionViews(ServerPlayer player) {
        UUID portal;
        ArenaSession current = this.parties.sessionFor(player);
        Selection selection = this.selections.get(player.m_20148_());
        UUID uUID = current != null ? current.profile().portal : (portal = selection == null ? null : selection.portal);
        if (portal == null) {
            return List.of();
        }
        ArrayList<AbyssNetwork.SectionView> result = new ArrayList<AbyssNetwork.SectionView>();
        for (int floor = 1; floor <= 7; ++floor) {
            ArenaSession session = this.parties.section(portal, floor);
            if (session == null) continue;
            List<AbyssNetwork.SectionPlayer> members = session.players().stream().limit(80L).map(p -> new AbyssNetwork.SectionPlayer(p.m_20148_(), p.m_36316_().getName())).toList();
            result.add(new AbyssNetwork.SectionView(floor, session.controller().active() ? 1 : (session.looting() ? 2 : 0), members));
        }
        return List.copyOf(result);
    }

    private int floorChoices(ServerPlayer player) {
        int available = 0;
        FloorProgress progress = this.parties.profile((UUID)player.m_20148_()).progress;
        ArenaSession session = this.parties.sessionFor(player);
        boolean acknowledged = session != null && session.canNavigate(player);
        for (int floor : FloorCatalog.snapshot().floors().keySet()) {
            if (!(session == null ? progress.canEnterFloor(floor) : progress.canSelectFloor(floor, acknowledged))) continue;
            available |= 1 << floor;
        }
        return available | 0x80;
    }

    public void keepOpen(ServerPlayer player, UUID token) {
        Selection selection = this.selections.get(player.m_20148_());
        if (selection != null && selection.token.equals(token) && this.valid(selection, player)) {
            this.selections.put(player.m_20148_(), new Selection(selection.token, selection.portal, selection.player, selection.dimension, selection.base, selection.returnPosition, this.server.m_129921_() + 2400));
            AbyssNetwork.sectionStatus(player, token, this.sectionViews(player), this.floorChoices(player));
            return;
        }
        ArenaMenu menu = this.arenaMenus.get(player.m_20148_());
        if (menu != null && menu.token.equals(token) && this.valid(menu, player)) {
            AbyssNetwork.sectionStatus(player, token, this.sectionViews(player), this.parties.sessionFor(player).canNavigate(player) ? this.floorChoices(player) : 0);
            this.arenaMenus.put(player.m_20148_(), new ArenaMenu(menu.token, menu.arenaHost, menu.floor, menu.followingHost, menu.personalRun, menu.selectable, this.server.m_129921_() + 2400));
        }
    }

    private boolean valid(ArenaMenu menu, ServerPlayer player) {
        ArenaSession session = this.parties.sessionFor(player);
        return menu.expires >= this.server.m_129921_() && player.m_6084_() && !player.m_9232_() && session != null && session.host().equals(menu.arenaHost) && session.controller().currentFloor() == menu.floor && (session.section() || Objects.equals(this.parties.followedHost(player), menu.followingHost) && this.parties.profile((UUID)player.m_20148_()).personalRun == menu.personalRun);
    }

    public void choose(ServerPlayer player, UUID token, int floor) {
        ArenaMenu menu = this.arenaMenus.get(player.m_20148_());
        if (menu != null && menu.token.equals(token)) {
            this.arenaMenus.remove(player.m_20148_());
            ArenaSession session = this.parties.sessionFor(player);
            if (!this.valid(menu, player)) {
                if (floor != 0) {
                    PortalParties.tell(player, "selection_expired");
                }
                return;
            }
            if (floor == -1) {
                this.parties.leave(player);
                return;
            }
            if (floor <= 0 || floor > 7 || (this.floorChoices(player) & 1 << floor) == 0 || !session.canNavigate(player)) {
                return;
            }
            if (session.section()) {
                this.parties.enterSection(player, session.profile().portal, floor);
            } else if (session.isLeader(player)) {
                this.parties.enterFloor(player, floor);
            } else if (menu.followingHost != null) {
                this.parties.followFloor(player, floor);
            }
            return;
        }
        Selection selection = this.selections.get(player.m_20148_());
        if (selection == null || !selection.token.equals(token)) {
            if (floor != 0) {
                PortalParties.tell(player, "selection_expired");
            }
            return;
        }
        this.selections.remove(player.m_20148_());
        player.m_20091_();
        if (floor <= 0 || floor > 7) {
            return;
        }
        if (!this.valid(selection, player)) {
            PortalParties.tell(player, "selection_expired");
            return;
        }
        AbyssPortalBlockEntity portal = (AbyssPortalBlockEntity)player.m_9236_().m_7702_(selection.base);
        if (!this.parties.enterSection(player, selection.portal, floor)) {
            return;
        }
        portal.bindHost(this.parties.sessionFor(player).host());
        this.parties.rememberPortalReturn(player, selection.returnPosition);
    }

    private boolean valid(Selection selection, ServerPlayer player) {
        AbyssPortalBlockEntity portal;
        if (selection.expires < this.server.m_129921_() || !player.m_6084_() || player.m_5833_() || player.m_9232_() || !player.m_9236_().m_46472_().equals(selection.dimension) || player.m_20238_(Vec3.m_82512_((Vec3i)selection.base)) > 100.0 || !player.m_9236_().m_46805_(selection.base)) {
            return false;
        }
        BlockEntity blockEntity = player.m_9236_().m_7702_(selection.base);
        return blockEntity instanceof AbyssPortalBlockEntity && (portal = (AbyssPortalBlockEntity)blockEntity).portalId().equals(selection.portal) && portal.frame().active(player.m_9236_());
    }

    public void tick() {
        this.selections.values().removeIf(selection -> {
            ServerPlayer player = this.server.m_6846_().m_11259_(selection.player);
            return player == null || player.m_9232_() || !this.valid((Selection)selection, player);
        });
        this.arenaMenus.entrySet().removeIf(entry -> ((ArenaMenu)entry.getValue()).expires < this.server.m_129921_() || this.server.m_6846_().m_11259_((UUID)entry.getKey()) == null);
    }

    private static void tell(ServerPlayer player, String key) {
        player.m_5661_((Component)Component.m_237115_((String)("message.spiral_abyss." + key)), true);
    }

    private record Selection(UUID token, UUID portal, UUID player, ResourceKey<Level> dimension, BlockPos base, Vec3 returnPosition, int expires) {
    }

    private record ArenaMenu(UUID token, UUID arenaHost, int floor, UUID followingHost, boolean personalRun, int selectable, int expires) {
    }
}

