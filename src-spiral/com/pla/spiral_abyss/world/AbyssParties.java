/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.boss.enderdragon.EndCrystal
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.entity.projectile.ThrownTrident
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.border.WorldBorder
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.world;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.FloorProgress;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import com.pla.spiral_abyss.encounter.challenge.ChallengeEligibility;
import com.pla.spiral_abyss.encounter.challenge.ChallengePlan;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.entity.ArenaInteractable;
import com.pla.spiral_abyss.entity.ExperienceChestEntity;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.entity.LootChestEntity;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.world.AbyssCycle;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssPlayers;
import com.pla.spiral_abyss.world.ArenaSession;
import com.pla.spiral_abyss.world.GuestFloorRules;
import com.pla.spiral_abyss.world.PortalParties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AbyssParties {
    public static final int ENDLESS_FLOOR = 7;
    public static final int DIMENSION_SIZE = 2304;
    private static final String RETURN = "spiral_abyss_return";
    private static final String VISIT = "spiral_abyss_visit_host";
    private static final String VISIT_FLOOR = "spiral_abyss_visit_floor";
    private static final String RETURN_REQUIRED = "spiral_abyss_return_required";
    private static final Map<MinecraftServer, AbyssParties> SERVERS = new IdentityHashMap<MinecraftServer, AbyssParties>();
    private final MinecraftServer server;
    private final AbyssPlayers data;
    private final Map<UUID, ArenaSession> sessions = new LinkedHashMap<UUID, ArenaSession>();
    private final Map<UUID, UUID> membership = new HashMap<UUID, UUID>();
    private final Map<ServerPlayer, AbyssNetwork.EncounterStatus> hudStates = new IdentityHashMap<ServerPlayer, AbyssNetwork.EncounterStatus>();
    private final PortalParties portals;

    private AbyssParties(MinecraftServer server) {
        this.server = server;
        this.data = AbyssPlayers.get(server);
        this.portals = new PortalParties(this, server);
    }

    public PortalParties portals() {
        return this.portals;
    }

    public AbyssCycle cycle() {
        if (!this.data.cycle().initialized()) {
            this.data.cycle().start(System.currentTimeMillis(), ThreadLocalRandom.current().nextLong(), MobCatalog.snapshot());
            this.data.m_77762_();
        }
        return this.data.cycle();
    }

    public long resetRemainingMillis() {
        return this.cycle().remainingMillis(System.currentTimeMillis(), (Integer)AbyssConfig.RESET_DAYS.get());
    }

    public void prepareCycle() {
        AbyssCycle cycle = this.cycle();
        MobCatalog.Pools fallback = MobCatalog.snapshot();
        cycle.repair(MobCatalog::selectableMob, fallback);
        for (FloorDefinition floor : FloorCatalog.snapshot().floors().values()) {
            try {
                this.challenges(floor.number());
            }
            catch (IllegalArgumentException ex) {
                LogUtils.getLogger().warn("Abyss floor {} challenges unavailable: {}", (Object)floor.number(), (Object)ex.getMessage());
            }
            for (int wave = 0; wave < floor.waves().size(); ++wave) {
                try {
                    List<FloorDefinition.Enemy> lineup = cycle.lineup(floor.number(), wave + 1, floor.waves().get(wave), MobCatalog::selectableMob, fallback);
                    if (!lineup.stream().anyMatch(enemy -> !MobCatalog.selectableMob(enemy.id()))) continue;
                    throw new IllegalArgumentException("A saved or explicit mob is unavailable or blacklisted; check mob_blacklist and its category pool");
                }
                catch (IllegalArgumentException ex) {
                    LogUtils.getLogger().warn("Abyss cycle floor {} wave {} unavailable: {}", new Object[]{floor.number(), wave + 1, ex.getMessage()});
                }
            }
        }
        this.data.m_77762_();
    }

    public List<FloorDefinition.Enemy> cycleLineup(int floor, int wave, FloorDefinition.Wave definition) {
        List<FloorDefinition.Enemy> result = this.cycle().lineup(floor, wave, definition, MobCatalog::selectableMob, MobCatalog.snapshot());
        this.data.m_77762_();
        if (result.stream().anyMatch(enemy -> !MobCatalog.selectableMob(enemy.id()))) {
            throw new IllegalArgumentException("A saved or explicit cycle mob is unavailable or blacklisted; check mob_blacklist and its category pool");
        }
        return result;
    }

    public List<Challenge> challenges(int floor) {
        ServerLevel level = this.server.m_129880_(AbyssDimension.LEVEL);
        if (level == null) {
            throw new IllegalArgumentException("Missing Abyss level");
        }
        FloorDefinition definition = FloorCatalog.snapshot().floors().get(floor);
        if (definition == null) {
            throw new IllegalArgumentException("Missing challenge floor " + floor);
        }
        ArrayList<List<FloorDefinition.Enemy>> rosters = new ArrayList<List<FloorDefinition.Enemy>>();
        for (int wave = 0; wave < definition.waves().size(); ++wave) {
            rosters.add(this.cycleLineup(floor, wave + 1, definition.waves().get(wave)));
        }
        ChallengePlan.Context context = new ChallengePlan.Context(rosters.stream().map(roster -> roster.stream().mapToInt(FloorDefinition.Enemy::count).sum()).toList(), new ChallengeEligibility(level, rosters));
        List<Challenge> result = this.cycle().challenges(definition, context, ChallengeCatalog.snapshot());
        this.data.m_77762_();
        return result;
    }

    public void resetGlobal() {
        this.resetGlobal(System.currentTimeMillis());
    }

    private void resetGlobal(long cycleStart) {
        this.server.m_6846_().m_11314_().forEach(player -> this.profile(player.m_20148_()));
        for (ArenaSession session : List.copyOf(this.sessions.values())) {
            this.retire(session);
        }
        this.portals.invalidateAll();
        this.data.resetAllProgress();
        this.cycle().start(cycleStart, ThreadLocalRandom.current().nextLong(), MobCatalog.snapshot());
        this.prepareCycle();
        this.data.m_77762_();
        for (ServerPlayer player2 : this.server.m_6846_().m_11314_()) {
            AbyssNetwork.closeFloors(player2);
            AbyssNetwork.encounterStatus(player2, AbyssNetwork.EncounterStatus.HIDDEN);
            this.returnAfterCycleReset(player2);
            player2.m_213846_((Component)Component.m_237115_((String)"message.spiral_abyss.global_reset"));
        }
        this.hudStates.clear();
    }

    private boolean returnAfterCycleReset(ServerPlayer player) {
        AbyssPlayers.Profile profile = this.profile(player.m_20148_());
        if (!profile.cycleReturnRequired) {
            return true;
        }
        if (player.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) && !this.leave(player, true)) {
            return false;
        }
        CompoundTag mark = AbyssParties.persisted(player);
        mark.m_128473_(VISIT);
        mark.m_128473_(VISIT_FLOOR);
        mark.m_128473_(RETURN);
        AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)mark);
        if (ArenaSession.hasDefeatMarker(player)) {
            ArenaSession.restoreSurvival(player);
        }
        profile.cycleReturnRequired = false;
        this.data.m_77762_();
        return true;
    }

    public ArenaSession hostedBy(UUID host) {
        return this.sessions.get(host);
    }

    public AbyssPlayers.Profile profile(UUID player) {
        return this.data.profile(player);
    }

    public void bindPortal(UUID host, UUID portal) {
        this.data.profile((UUID)host).portal = portal;
        this.data.m_77762_();
    }

    public boolean checkUnlocked(ServerPlayer recipient, ServerPlayer player, int floor) {
        if (floor == 7 || this.data.profile((UUID)player.m_20148_()).progress.unlocked(floor)) {
            return true;
        }
        recipient.m_213846_((Component)Component.m_237110_((String)"message.spiral_abyss.floor_locked_player", (Object[])new Object[]{player.m_5446_(), floor}));
        return false;
    }

    public static AbyssParties get(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, AbyssParties::new);
    }

    public static boolean queueBossFloorRepair(ServerLevel level, BlockPos pos) {
        AbyssParties parties = SERVERS.get(level.m_7654_());
        if (parties == null) {
            return false;
        }
        boolean owned = false;
        for (ArenaSession session : parties.sessions.values()) {
            if (session.level() != level || !session.controller().bossEnvironments().queueFloorRepair(pos)) continue;
            owned = true;
        }
        return owned;
    }

    public static void disconnect(ServerPlayer player) {
        AbyssParties parties = SERVERS.get(player.f_8924_);
        if (parties != null) {
            parties.loggedOut(player);
        }
    }

    public static void stop(MinecraftServer server) {
        AbyssParties parties = SERVERS.remove(server);
        if (parties != null) {
            new ArrayList<ArenaSession>(parties.sessions.values()).forEach(parties::retire);
        }
    }

    public Collection<ArenaSession> sessions() {
        return List.copyOf(this.sessions.values());
    }

    public ArenaSession sessionFor(ServerPlayer player) {
        ArenaSession session = this.sessions.get(this.membership.get(player.m_20148_()));
        return session != null && session.contains(player) ? session : null;
    }

    public UUID hostOf(Entity entity) {
        Projectile projectile;
        if (AbyssPlatform.data(entity).m_128403_("spiral_abyss_host")) {
            return AbyssPlatform.data(entity).m_128342_("spiral_abyss_host");
        }
        if (entity instanceof Projectile && (projectile = (Projectile)entity).m_19749_() != null) {
            return this.hostOf(projectile.m_19749_());
        }
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            ArenaSession session = this.sessionFor(player);
            return session == null ? null : session.host();
        }
        ArenaSession session = this.nearest(entity.m_20182_());
        return session == null ? null : session.host();
    }

    public static void configure(ServerLevel level) {
        level.m_46734_(0.0f);
        level.m_46707_(0.0f);
        AbyssParties.maintainBorder(level);
        level.m_7726_().m_8354_(6);
        level.m_7726_().m_184026_(4);
        for (long packed : level.m_8902_().toLongArray()) {
            ChunkPos chunk = new ChunkPos(packed);
            level.m_8602_(chunk.f_45578_, chunk.f_45579_, false);
        }
    }

    private static boolean maintainBorder(ServerLevel level) {
        WorldBorder border = level.m_6857_();
        if (border.m_6347_() == 0.5 && border.m_6345_() == 0.5 && border.m_61959_() == 2304.0 && border.m_61961_() == 2304.0) {
            return false;
        }
        border.m_61949_(0.5, 0.5);
        border.m_61917_(2304.0);
        return true;
    }

    private ArenaSession acquire(UUID host) {
        ArenaSession existing = this.sessions.get(host);
        if (existing != null) {
            return existing;
        }
        HashSet occupied = new HashSet();
        this.sessions.values().forEach(s -> occupied.add(s.profile().slot));
        int slot = 0;
        while (occupied.contains(slot)) {
            ++slot;
        }
        if (slot >= 16) {
            return null;
        }
        ServerLevel level = this.server.m_129880_(AbyssDimension.LEVEL);
        if (level == null) {
            return null;
        }
        AbyssPlayers.Profile profile = this.data.profile(host);
        profile.slot = slot;
        ArenaSession session = new ArenaSession(this, level, profile);
        this.sessions.put(host, session);
        return session;
    }

    public boolean enterOwn(ServerPlayer player) {
        int floor;
        if (this.profile((UUID)player.m_20148_()).endlessMode) {
            return this.enterFloor(player, 7);
        }
        FloorProgress progress = this.data.profile((UUID)player.m_20148_()).progress;
        int n = floor = progress.requiredPayoutFloor() != 0 ? progress.requiredPayoutFloor() : progress.floor();
        if (!progress.unlocked(floor)) {
            floor = 1;
        }
        return this.enterFloor(player, floor);
    }

    public boolean enterPortalGuest(ServerPlayer player, UUID host, Vec3 returnPosition) {
        ArenaSession session = this.sessions.get(host);
        if (session == null) {
            return false;
        }
        if (host.equals(player.m_20148_())) {
            if (!this.enter(player, host)) {
                return false;
            }
            this.follow(player.m_20148_(), null);
        } else if (session.endless()) {
            if (!this.enter(player, host)) {
                return false;
            }
            this.follow(player.m_20148_(), host);
            this.profile((UUID)player.m_20148_()).personalRun = false;
            this.data.m_77762_();
        } else {
            int floor = GuestFloorRules.portalDestination(this.profile((UUID)player.m_20148_()).progress, session.controller().currentFloor());
            if (!this.travelFollowing(player, host, floor, true)) {
                return false;
            }
        }
        this.rememberPortalReturn(player, returnPosition);
        return true;
    }

    public void rememberPortalReturn(ServerPlayer player, Vec3 returnPosition) {
        CompoundTag persisted = AbyssParties.persisted(player);
        CompoundTag origin = persisted.m_128469_(RETURN);
        origin.m_128347_("X", returnPosition.f_82479_);
        origin.m_128347_("Y", returnPosition.f_82480_);
        origin.m_128347_("Z", returnPosition.f_82481_);
        persisted.m_128365_(RETURN, (Tag)origin);
        AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)persisted);
        player.m_20091_();
    }

    public ArenaSession section(UUID portal, int floor) {
        if (portal == null) {
            return null;
        }
        return this.sessions.values().stream().filter(s -> s.section() && portal.equals(s.profile().portal) && s.controller().currentFloor() == floor).findFirst().orElse(null);
    }

    void changeSectionOwner(ArenaSession session, UUID owner) {
        session.profile().sectionOwner = owner;
        this.data.m_77762_();
    }

    public boolean enterSection(ServerPlayer player, UUID portal, int floor) {
        if (portal == null || floor < 1 || floor > 7) {
            return false;
        }
        if (floor != 7 && !FloorCatalog.snapshot().floors().containsKey(floor)) {
            return AbyssParties.tell(player, "floor_unavailable", new Object[0]);
        }
        if (!this.checkUnlocked(player, player, floor) || !AbyssParties.canTravel(player)) {
            return false;
        }
        ArenaSession current = this.sessionFor(player);
        if (current != null && !current.canNavigate(player)) {
            return AbyssParties.tell(player, "claim_first", new Object[0]);
        }
        FloorProgress personal = this.profile((UUID)player.m_20148_()).progress;
        if (floor != 7 && !(current != null ? personal.canSelectFloor(floor, true) : personal.canEnterFloor(floor))) {
            return AbyssParties.tell(player, "claim_first", new Object[0]);
        }
        ArenaSession destination = this.section(portal, floor);
        if (destination == current && current != null) {
            return true;
        }
        if (destination == null) {
            if (this.sessions.size() >= 16) {
                return AbyssParties.tell(player, "arenas_full", new Object[0]);
            }
            UUID id = UUID.nameUUIDFromBytes(("spiral_abyss:section:" + String.valueOf(portal) + ":" + floor).getBytes(StandardCharsets.UTF_8));
            AbyssPlayers.Profile sectionProfile = this.profile(id);
            sectionProfile.portal = portal;
            sectionProfile.sectionFloor = floor;
            sectionProfile.sectionOwner = player.m_20148_();
            sectionProfile.setEndlessMode(floor == 7);
            if (floor != 7) {
                sectionProfile.progress.placeSection(floor);
                sectionProfile.progress.clearPayouts(floor);
            }
            this.data.m_77762_();
            destination = this.acquire(id);
            if (destination == null) {
                return AbyssParties.tell(player, "arenas_full", new Object[0]);
            }
        }
        if (!this.enter(player, destination.host())) {
            return false;
        }
        this.follow(player.m_20148_(), null);
        this.profile((UUID)player.m_20148_()).portal = portal;
        if (floor != 7) {
            personal.selectFloor(floor, true);
        }
        this.data.m_77762_();
        if (current != null && current != destination && current.players().isEmpty()) {
            this.retire(current);
        }
        return true;
    }

    public boolean enterFloor(ServerPlayer player, int number) {
        ArenaSession current = this.sessionFor(player);
        if (current != null && current.section()) {
            return this.enterSection(player, current.profile().portal, number);
        }
        if (current != null && current.isLeader(player) && !current.host().equals(player.m_20148_())) {
            return this.enterLedFloor(player, current, number);
        }
        if (this.profile((UUID)player.m_20148_()).followingHost != null) {
            return this.followFloor(player, number);
        }
        return this.enterOwnFloor(player, number);
    }

    private boolean enterLedFloor(ServerPlayer player, ArenaSession session, int number) {
        if (session.controller().active()) {
            return AbyssParties.tell(player, "already_running", new Object[0]);
        }
        if (!session.canNavigate(player)) {
            return AbyssParties.tell(player, "claim_first", new Object[0]);
        }
        if (!this.enterOwnFloor(player, number)) {
            return false;
        }
        this.follow(player.m_20148_(), null);
        return true;
    }

    private boolean enterOwnFloor(ServerPlayer player, int number) {
        boolean acknowledged;
        if (number != 7 && !FloorCatalog.snapshot().floors().containsKey(number)) {
            return AbyssParties.tell(player, "floor_unavailable", new Object[0]);
        }
        if (!this.checkUnlocked(player, player, number)) {
            return false;
        }
        ArenaSession current = this.sessionFor(player);
        FloorProgress progress = this.data.profile((UUID)player.m_20148_()).progress;
        if (current != null && !current.canNavigate(player)) {
            return AbyssParties.tell(player, "claim_first", new Object[0]);
        }
        ArenaSession own = this.sessions.get(player.m_20148_());
        if (own != null && !own.isLeader(player)) {
            return AbyssParties.tell(player, "host_only", new Object[0]);
        }
        if (current != null && current.controller().active() || own != null && own.controller().active()) {
            return AbyssParties.tell(player, "already_running", new Object[0]);
        }
        boolean bl = acknowledged = current == null || current.canNavigate(player);
        if (number != 7 && !(current != null ? progress.canSelectFloor(number, acknowledged) : progress.canEnterFloor(number))) {
            return AbyssParties.tell(player, "claim_first", new Object[0]);
        }
        if (own != null && own.controller().currentFloor() != number && !this.separateGuests(player, own)) {
            return false;
        }
        if (!this.enter(player, player.m_20148_())) {
            return false;
        }
        ArenaSession session = this.sessionFor(player);
        if (number != 7) {
            progress.selectFloor(number, acknowledged);
        }
        CompoundTag mark = AbyssParties.persisted(player);
        mark.m_128405_(VISIT_FLOOR, number);
        AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)mark);
        session.controller().close();
        session.clearParticipants();
        this.profile(player.m_20148_()).setEndlessMode(number == 7);
        session.controller().close();
        if (own != null) {
            session.restoreStructure();
        }
        session.refreshDisplay();
        return true;
    }

    private boolean enter(ServerPlayer player, UUID host) {
        if (!this.returnAfterCycleReset(player)) {
            return false;
        }
        if (!AbyssParties.canTravel(player)) {
            return AbyssParties.tell(player, "travel_unavailable", new Object[0]);
        }
        ArenaSession source = this.sessionFor(player);
        if (source != null && !source.host().equals(host) && source.controller().active() && !source.observer(player)) {
            return AbyssParties.tell(player, "already_running", new Object[0]);
        }
        ArenaSession session = this.acquire(host);
        if (session == null) {
            return AbyssParties.tell(player, "arenas_full", new Object[0]);
        }
        if (!host.equals(player.m_20148_()) && !this.checkUnlocked(player, player, session.controller().currentFloor())) {
            return false;
        }
        if (!session.canAdmit(player.m_20148_())) {
            return AbyssParties.tell(player, "party_unavailable", new Object[0]);
        }
        CompoundTag persisted = AbyssParties.persisted(player);
        if (!player.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            CompoundTag origin = new CompoundTag();
            origin.m_128359_("Dimension", player.m_9236_().m_46472_().m_135782_().toString());
            origin.m_128347_("X", player.m_20185_());
            origin.m_128347_("Y", player.m_20186_());
            origin.m_128347_("Z", player.m_20189_());
            origin.m_128350_("Yaw", player.m_146908_());
            origin.m_128350_("Pitch", player.m_146909_());
            persisted.m_128365_(RETURN, (Tag)origin);
        }
        Vec3 destination = session.bounds().entrance();
        ArenaSession previous = this.sessionFor(player);
        if (player.m_9236_() == session.level()) {
            player.m_8999_(session.level(), destination.f_82479_, destination.f_82480_, destination.f_82481_, 180.0f, 0.0f);
        } else if (AbyssPlatform.transfer(player, session.level(), destination, 180.0f, 0.0f) == null) {
            if (session.admitted().isEmpty()) {
                this.retire(session);
            }
            return false;
        }
        player.m_6915_();
        if (previous != null && previous != session) {
            previous.remove(player.m_20148_());
        }
        this.membership.put(player.m_20148_(), host);
        persisted.m_128362_(VISIT, host);
        persisted.m_128405_(VISIT_FLOOR, session.controller().currentFloor());
        AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)persisted);
        session.admit(player);
        if (!session.section() && !session.endless() && host.equals(player.m_20148_()) && !session.controller().active() && session.profile().progress.claimed()) {
            session.controller().close();
            session.refreshDisplay();
        }
        player.f_8906_.m_9829_((Packet)new ClientboundInitializeBorderPacket(session.level().m_6857_()));
        player.f_19789_ = 0.0f;
        player.m_20256_(Vec3.f_82478_);
        player.m_213846_((Component)Component.m_237110_((String)"message.spiral_abyss.menu_hint", (Object[])new Object[]{Component.m_237117_((String)"key.spiral_abyss.menu")}));
        return true;
    }

    public boolean leave(ServerPlayer player) {
        return this.leave(player, false);
    }

    private boolean leave(ServerPlayer player, boolean forced) {
        if (forced) {
            player.m_8127_();
            player.m_20153_();
            if (player.m_5803_()) {
                player.m_6145_(true, true);
            }
        } else if (!AbyssParties.canTravel(player)) {
            return AbyssParties.tell(player, "travel_unavailable", new Object[0]);
        }
        if (!player.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            return AbyssParties.tell(player, "wrong_dimension", new Object[0]);
        }
        CompoundTag persisted = AbyssParties.persisted(player);
        CompoundTag saved = persisted.m_128469_(RETURN);
        ResourceLocation id = ResourceLocation.m_135820_((String)saved.m_128461_("Dimension"));
        ServerLevel destination = this.server.m_129783_();
        Vec3 position = new Vec3(saved.m_128459_("X"), saved.m_128459_("Y"), saved.m_128459_("Z"));
        Vec3 safe = id != null && id.equals((Object)destination.m_46472_().m_135782_()) ? AbyssParties.safeReturnPosition(destination, player, position) : null;
        position = safe != null ? safe : Vec3.m_82539_((Vec3i)destination.m_5452_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, destination.m_220360_()));
        ArenaSession session = this.sessionFor(player);
        if (forced) {
            player.m_8999_(destination, position.f_82479_, position.f_82480_, position.f_82481_, saved.m_128457_("Yaw"), saved.m_128457_("Pitch"));
            if (player.m_9236_() != destination) {
                return false;
            }
        } else if (AbyssPlatform.transfer(player, destination, position, saved.m_128457_("Yaw"), saved.m_128457_("Pitch")) == null) {
            return false;
        }
        player.m_6915_();
        if (session != null) {
            session.remove(player.m_20148_());
        }
        if (session != null && session.players().isEmpty()) {
            this.retire(session);
        }
        this.membership.remove(player.m_20148_());
        persisted.m_128473_(VISIT);
        persisted.m_128473_(VISIT_FLOOR);
        persisted.m_128473_(RETURN);
        persisted.m_128473_(RETURN_REQUIRED);
        this.follow(player.m_20148_(), null);
        AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)persisted);
        player.f_19789_ = 0.0f;
        if (ArenaSession.hasDefeatMarker(player)) {
            ArenaSession.restoreSurvival(player);
        }
        player.m_20091_();
        return true;
    }

    public boolean startKey(ServerPlayer player, KeyEntity key) {
        ArenaSession session = this.sessionFor(player);
        if (!this.validInteraction(player, key, session) || !session.controller().start(player, key.m_20182_())) {
            return false;
        }
        if (session.host().equals(player.m_20148_()) && this.followedHost(player) != null) {
            this.profile((UUID)player.m_20148_()).personalRun = true;
            this.data.m_77762_();
        }
        return true;
    }

    public UUID followedHost(ServerPlayer player) {
        ArenaSession session = this.sessionFor(player);
        if (session != null && session.isLeader(player) && !session.host().equals(player.m_20148_())) {
            return null;
        }
        if (session != null && !session.host().equals(player.m_20148_())) {
            return session.host();
        }
        return this.profile((UUID)player.m_20148_()).followingHost;
    }

    private void follow(UUID player, UUID host) {
        this.profile((UUID)player).followingHost = host;
        if (host == null) {
            this.profile((UUID)player).personalRun = false;
        }
        this.data.m_77762_();
    }

    public int guestDestination(ServerPlayer player) {
        UUID host = this.followedHost(player);
        ArenaSession current = this.sessionFor(player);
        if (host == null || current == null || current.controller().active() || current.endless()) {
            return 0;
        }
        return GuestFloorRules.next(this.profile((UUID)player.m_20148_()).progress, current.controller().currentFloor(), current.canNavigate(player));
    }

    public boolean followFloor(ServerPlayer player, int number) {
        ArenaSession section = this.sessionFor(player);
        if (section != null && section.section()) {
            return this.enterSection(player, section.profile().portal, number);
        }
        UUID host = this.followedHost(player);
        if (host == null) {
            return AbyssParties.tell(player, "selection_expired", new Object[0]);
        }
        ArenaSession current = this.sessionFor(player);
        if (current == null || current.controller().active()) {
            return AbyssParties.tell(player, "already_running", new Object[0]);
        }
        if (!current.canNavigate(player) || number != 7 && !this.profile((UUID)player.m_20148_()).progress.canSelectFloor(number, true)) {
            return AbyssParties.tell(player, "claim_first", new Object[0]);
        }
        return this.travelFollowing(player, host, number, false);
    }

    private boolean travelFollowing(ServerPlayer player, UUID host, int number, boolean portalEntry) {
        boolean entered;
        if (player.m_20148_().equals(host)) {
            return false;
        }
        FloorProgress progress = this.profile((UUID)player.m_20148_()).progress;
        ArenaSession current = this.sessionFor(player);
        if (number != 7 && !(current != null ? progress.canSelectFloor(number, !portalEntry && current.canNavigate(player)) : progress.canEnterFloor(number))) {
            return AbyssParties.tell(player, "claim_first", new Object[0]);
        }
        ArenaSession destination = this.hostedBy(host);
        boolean join = destination != null && (destination.endless() && number == 7 && (portalEntry || !this.profile((UUID)player.m_20148_()).personalRun) && !destination.controller().active() || !destination.endless() && GuestFloorRules.joinHost(portalEntry, this.profile((UUID)player.m_20148_()).personalRun, number, destination.controller().currentFloor(), destination.controller().active(), progress.pendingFloor() != 0 || destination.looting()));
        boolean bl = entered = join ? this.enter(player, host) : this.enterOwnFloor(player, number);
        if (entered) {
            this.follow(player.m_20148_(), host);
            this.profile((UUID)player.m_20148_()).personalRun = !join;
            this.data.m_77762_();
            if (!join) {
                player.m_213846_((Component)Component.m_237110_((String)"message.spiral_abyss.personal_session", (Object[])new Object[]{number}));
            }
        }
        return entered;
    }

    private boolean separateGuests(ServerPlayer requester, ArenaSession session) {
        if (!AbyssParties.canTravel(requester)) {
            return AbyssParties.tell(requester, "travel_unavailable", new Object[0]);
        }
        int oldFloor = session.controller().currentFloor();
        List<ServerPlayer> guests = session.players().stream().filter(p -> !session.isLeader((ServerPlayer)p)).toList();
        long required = guests.stream().filter(p -> this.hostedBy(p.m_20148_()) == null).count();
        if ((long)this.sessions.size() + required > 16L) {
            return AbyssParties.tell(requester, "arenas_full", new Object[0]);
        }
        for (ServerPlayer guest : guests) {
            ArenaSession own = this.hostedBy(guest.m_20148_());
            if (AbyssParties.canTravel(guest) && (oldFloor == 7 || this.profile((UUID)guest.m_20148_()).progress.canSelectFloor(oldFloor)) && (own == null || own.players().isEmpty() && !own.controller().active())) continue;
            return AbyssParties.tell(requester, "guest_transfer_blocked", guest.m_5446_());
        }
        for (ServerPlayer guest : guests) {
            FloorProgress personal = this.profile((UUID)guest.m_20148_()).progress;
            if (oldFloor != 7) {
                personal.selectFloor(oldFloor);
            }
            this.profile(guest.m_20148_()).setEndlessMode(oldFloor == 7);
            if (!this.enter(guest, guest.m_20148_())) {
                throw new IllegalStateException("Prevalidated guest transfer failed");
            }
            this.follow(guest.m_20148_(), session.host());
            this.profile((UUID)guest.m_20148_()).personalRun = false;
            ArenaSession own = this.sessionFor(guest);
            own.controller().close();
            own.refreshDisplay();
            guest.m_213846_((Component)Component.m_237115_((String)"message.spiral_abyss.host_moved"));
        }
        return true;
    }

    public boolean openChest(ServerPlayer player, LootChestEntity chest) {
        ArenaSession session = this.sessionFor(player);
        if (!this.validInteraction(player, chest, session)) {
            return false;
        }
        if (chest.phase() == 4) {
            return false;
        }
        return session.openReward(player, chest.tier());
    }

    public boolean openExperienceChest(ServerPlayer player, ExperienceChestEntity chest) {
        ArenaSession session = this.sessionFor(player);
        return this.validInteraction(player, chest, session) && session.endless() && chest.phase() == 0 && session.controller().openEndlessCheckpoint(player);
    }

    private boolean validInteraction(ServerPlayer player, ArenaInteractable entity, ArenaSession session) {
        return session != null && player.m_6084_() && !player.m_5833_() && !entity.m_213877_() && session.host().equals(entity.arenaHost()) && player.m_20280_((Entity)entity) <= 64.0;
    }

    public boolean nextFloor(ServerPlayer player) {
        ArenaSession section = this.sessionFor(player);
        if (section != null && section.section()) {
            int next = section.controller().currentFloor() + 1;
            return next <= 6 && section.canAdvance(player) && this.enterSection(player, section.profile().portal, next);
        }
        if (this.followedHost(player) != null) {
            return this.followFloor(player, this.guestDestination(player));
        }
        ArenaSession session = this.sessionFor(player);
        return session == null ? AbyssParties.tell(player, "wrong_dimension", new Object[0]) : session.controller().nextFloor(player);
    }

    public boolean skipFloors(ServerPlayer player, int number) {
        if (number < 1 || number > 6) {
            return false;
        }
        ArenaSession owned = this.hostedBy(player.m_20148_());
        if (owned != null && owned.looting() && !owned.players().isEmpty() && (this.sessionFor(player) == null || owned.players().size() > 1)) {
            return AbyssParties.tell(player, "skip_floor_party_looting", new Object[0]);
        }
        if (player.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) && !this.leave(player)) {
            return false;
        }
        this.portals.invalidate(player);
        AbyssNetwork.closeFloors(player);
        FloorProgress progress = this.profile((UUID)player.m_20148_()).progress;
        progress.skipThrough(number);
        player.m_213846_((Component)Component.m_237110_((String)"message.spiral_abyss.skip_floor", (Object[])new Object[]{number, progress.floor()}));
        return true;
    }

    public boolean reset(ServerPlayer player) {
        ArenaSession current = this.sessionFor(player);
        if (current != null && current.section() && !this.leave(player)) {
            return false;
        }
        ArenaSession session = this.sessions.get(player.m_20148_());
        if (session != null && !session.isLeader(player)) {
            return AbyssParties.tell(player, "host_only", new Object[0]);
        }
        AbyssPlayers.Profile profile = this.data.profile(player.m_20148_());
        profile.progress.reset();
        this.follow(player.m_20148_(), null);
        profile.requestArenaReset();
        if (session != null) {
            session.resetArena();
        }
        player.m_213846_((Component)Component.m_237115_((String)"message.spiral_abyss.reset"));
        return true;
    }

    public void loggedIn(ServerPlayer player) {
        if (player.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            CompoundTag mark = AbyssParties.persisted(player);
            mark.m_128379_(RETURN_REQUIRED, true);
            AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)mark);
        }
    }

    public void loggedOut(ServerPlayer player) {
        ArenaSession session;
        if (player.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            CompoundTag mark = AbyssParties.persisted(player);
            mark.m_128379_(RETURN_REQUIRED, true);
            AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)mark);
        }
        if ((session = this.sessionFor(player)) != null) {
            session.remove(player.m_20148_());
            if (session.players().isEmpty()) {
                this.retire(session);
            }
        }
        this.membership.remove(player.m_20148_());
        this.hudStates.remove(player);
        this.follow(player.m_20148_(), null);
    }

    private void returnAfterDisconnect(ServerPlayer player) {
        if (!AbyssParties.persisted(player).m_128471_(RETURN_REQUIRED)) {
            return;
        }
        ArenaSession.restoreSurvival(player);
        if (player.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) && !this.leave(player, true)) {
            return;
        }
        CompoundTag mark = AbyssParties.persisted(player);
        mark.m_128473_(RETURN_REQUIRED);
        mark.m_128473_(VISIT);
        mark.m_128473_(VISIT_FLOOR);
        mark.m_128473_(RETURN);
        AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)mark);
        this.follow(player.m_20148_(), null);
    }

    public void tick() {
        ArenaSession session;
        long now = System.currentTimeMillis();
        int days = (Integer)AbyssConfig.RESET_DAYS.get();
        if (this.cycle().due(now, days)) {
            this.resetGlobal(this.cycle().nextStart(now, days));
        }
        for (Object player : this.server.m_6846_().m_11314_()) {
            this.returnAfterDisconnect((ServerPlayer)player);
            this.returnAfterCycleReset((ServerPlayer)player);
        }
        this.portals.tick();
        ServerLevel level = this.server.m_129880_(AbyssDimension.LEVEL);
        if (level == null) {
            return;
        }
        if (AbyssParties.maintainBorder(level)) {
            level.m_6907_().forEach(p -> p.f_8906_.m_9829_((Packet)new ClientboundInitializeBorderPacket(level.m_6857_())));
        }
        for (ServerPlayer serverPlayer : List.copyOf(level.m_6907_())) {
            if (this.profile((UUID)serverPlayer.m_20148_()).cycleReturnRequired || this.sessionFor(serverPlayer) != null) continue;
            ArenaSession.restoreSurvival(serverPlayer);
            this.leave(serverPlayer, true);
        }
        for (ArenaSession arenaSession : List.copyOf(this.sessions.values())) {
            for (UUID id : arenaSession.admitted()) {
                if (!arenaSession.players().stream().noneMatch(p -> p.m_20148_().equals(id) && !p.m_9232_())) continue;
                arenaSession.remove(id);
                this.membership.remove(id);
            }
            if (arenaSession.players().isEmpty()) {
                this.retire(arenaSession);
                continue;
            }
            arenaSession.tick();
        }
        List online = this.server.m_6846_().m_11314_();
        this.hudStates.keySet().retainAll(online);
        for (ServerPlayer player : online) {
            session = this.sessionFor(player);
            AbyssNetwork.EncounterStatus status = session == null ? AbyssNetwork.EncounterStatus.HIDDEN : session.controller().hudStatus(player);
            if (status.equals(this.hudStates.put(player, status))) continue;
            AbyssNetwork.encounterStatus(player, status);
        }
        for (ServerPlayer player : this.server.m_6846_().m_11314_()) {
            if (!ArenaSession.hasDefeatMarker(player) || (session = this.sessionFor(player)) != null && session.waitingForParty(player)) continue;
            ArenaSession.restoreSurvival(player);
        }
        ArrayList<Object> arrayList = new ArrayList<Object>();
        for (Entity entity : level.m_8583_()) {
            ArenaSession session3;
            ArenaSession owner;
            if (AbyssPlatform.data(entity).m_128403_("spiral_abyss_environment_owner")) {
                owner = this.sessions.get(this.hostOf(entity));
                if (owner != null && owner.controller().bossEnvironments().ownsPlacement(entity)) continue;
                arrayList.add(entity);
                continue;
            }
            if (entity instanceof Projectile) {
                Mob owner2;
                Entity entity2;
                ThrownTrident trident;
                Projectile projectile = (Projectile)entity;
                if (projectile instanceof ThrownTrident && (trident = (ThrownTrident)projectile).m_36797_() && AbyssPlatform.data((Entity)trident).m_128471_("spiral_abyss_no_pickup") && (entity2 = trident.m_19749_()) instanceof Mob && trident.m_20238_((owner2 = (Mob)entity2).m_146892_()) < 2.25) {
                    arrayList.add(trident);
                    continue;
                }
                ArenaSession owner3 = this.sessions.get(this.hostOf((Entity)projectile));
                if (owner3 == null || Math.abs(entity.m_20185_() - owner3.bounds().center().f_82479_) > 128.0 || Math.abs(entity.m_20189_() - owner3.bounds().center().f_82481_) > 128.0 || entity.m_20186_() < 64.0 || entity.m_20186_() > 240.0) {
                    arrayList.add(entity);
                    continue;
                }
                AbyssPlatform.data(entity).m_128362_("spiral_abyss_host", owner3.host());
                continue;
            }
            if (entity instanceof EndCrystal && AbyssPlatform.data(entity).m_128403_("spiral_abyss_crystal_owner")) {
                owner = this.sessions.get(this.hostOf(entity));
                if (owner != null && owner.controller().bossEnvironments().ownsCrystal(entity)) continue;
                arrayList.add(entity);
                continue;
            }
            if (!(entity instanceof Mob)) continue;
            Mob mob = (Mob)entity;
            if (entity instanceof ArenaInteractable) continue;
            ArenaSession arenaSession = session3 = AbyssPlatform.data(entity).m_128403_("spiral_abyss_host") ? this.sessions.get(AbyssPlatform.data(entity).m_128342_("spiral_abyss_host")) : this.nearest(entity.m_20182_());
            if (session3 == null) {
                arrayList.add(entity);
                continue;
            }
            AbyssPlatform.data(entity).m_128362_("spiral_abyss_host", session3.host());
            session3.rescueMob((LivingEntity)mob);
            if (this.server.m_129921_() % 20 != 0) continue;
            session3.keepMobActive((LivingEntity)mob);
        }
        arrayList.forEach(Entity::m_146870_);
        if (this.server.m_129921_() % 20 == 0) {
            for (ServerLevel world : this.server.m_129785_()) {
                arrayList.clear();
                for (Entity entity : world.m_8583_()) {
                    if (!AbyssPlatform.data(entity).m_128403_("spiral_abyss_run") || !this.sessions.values().stream().noneMatch(s -> s.controller().owns(entity) || s.controller().bossEnvironments().ownsPlacement(entity))) continue;
                    arrayList.add(entity);
                }
                arrayList.forEach(Entity::m_146870_);
            }
        }
    }

    private ArenaSession nearest(Vec3 point) {
        return this.sessions.values().stream().filter(s -> Math.abs(s.bounds().center().f_82479_ - point.f_82479_) <= 128.0 && Math.abs(s.bounds().center().f_82481_ - point.f_82481_) <= 128.0).findFirst().orElse(null);
    }

    private void retire(ArenaSession session) {
        session.close();
        this.sessions.remove(session.host());
        session.profile().slot = -1;
        this.membership.values().removeIf(session.host()::equals);
    }

    private static CompoundTag persisted(ServerPlayer p) {
        return AbyssPlatform.data((Entity)p).m_128469_("PlayerPersisted");
    }

    private static boolean canTravel(ServerPlayer p) {
        return !p.m_20159_() && !p.m_20160_() && !p.m_5803_() && p.m_6072_();
    }

    private static boolean isSafe(ServerLevel level, ServerPlayer player, Vec3 position) {
        if (!(Double.isFinite(position.f_82479_) && Double.isFinite(position.f_82480_) && Double.isFinite(position.f_82481_))) {
            return false;
        }
        BlockPos feet = BlockPos.m_274446_((Position)position);
        if (!level.m_6857_().m_61937_(feet) || feet.m_123342_() <= level.m_141937_() || feet.m_123342_() + 2 >= level.m_151558_()) {
            return false;
        }
        AABB body = player.m_20191_().m_82383_(position.m_82546_(player.m_20182_()));
        AABB collisionArea = body.m_82400_(1.0);
        for (int x = Mth.m_14107_((double)collisionArea.f_82288_) >> 4; x <= Mth.m_14107_((double)collisionArea.f_82291_) >> 4; ++x) {
            for (int z = Mth.m_14107_((double)collisionArea.f_82290_) >> 4; z <= Mth.m_14107_((double)collisionArea.f_82293_) >> 4; ++z) {
                level.m_6325_(x, z);
            }
        }
        return !level.m_45756_((Entity)player, body.m_82386_(0.0, -0.05, 0.0)) && level.m_6425_(feet).m_76178_() && !level.m_46855_(body) && level.m_45756_((Entity)player, body);
    }

    private static Vec3 safeReturnPosition(ServerLevel level, ServerPlayer player, Vec3 saved) {
        if (AbyssParties.isSafe(level, player, saved)) {
            return saved;
        }
        if (!(Double.isFinite(saved.f_82479_) && Double.isFinite(saved.f_82480_) && Double.isFinite(saved.f_82481_))) {
            return null;
        }
        BlockPos origin = BlockPos.m_274446_((Position)saved);
        for (int radius = 1; radius <= 4; ++radius) {
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos column = origin.m_7918_(dx, 0, dz);
                    if (!level.m_6857_().m_61937_(column)) continue;
                    level.m_6325_(column.m_123341_() >> 4, column.m_123343_() >> 4);
                    for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                        Vec3 candidate;
                        VoxelShape shape;
                        BlockPos floor = column.m_7918_(0, dy - 1, 0);
                        if (floor.m_123342_() < level.m_141937_() || floor.m_123342_() >= level.m_151558_() || (shape = level.m_8055_(floor).m_60812_((BlockGetter)level, floor)).m_83281_() || !AbyssParties.isSafe(level, player, candidate = new Vec3((double)column.m_123341_() + 0.5, (double)floor.m_123342_() + shape.m_83297_(Direction.Axis.Y), (double)column.m_123343_() + 0.5))) continue;
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean tell(ServerPlayer player, String key, Object ... args) {
        player.m_5661_((Component)Component.m_237110_((String)("message.spiral_abyss." + key), (Object[])args), true);
        return false;
    }
}

