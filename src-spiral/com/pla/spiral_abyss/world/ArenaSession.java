/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.server.level.TicketType
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.SimpleMenuProvider
 *  net.minecraft.world.entity.AreaEffectCloud
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.ExperienceOrb
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.projectile.EvokerFangs
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.ChallengeRewards;
import com.pla.spiral_abyss.encounter.ChestPayout;
import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.encounter.FloorController;
import com.pla.spiral_abyss.encounter.FloorProgress;
import com.pla.spiral_abyss.encounter.FloorRewards;
import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.entity.ArenaInteractable;
import com.pla.spiral_abyss.entity.ExperienceChestEntity;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.entity.LootChestEntity;
import com.pla.spiral_abyss.event.AbyssBlockProtection;
import com.pla.spiral_abyss.init.EntityInit;
import com.pla.spiral_abyss.menu.RewardChestMenu;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.AbyssPlayers;
import com.pla.spiral_abyss.world.ArenaBounds;
import com.pla.spiral_abyss.world.ArenaFlight;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ArenaSession {
    public static final String DEFEATED_TAG = "spiral_abyss_defeated";
    private static final TicketType<UUID> MOB_TICKET = TicketType.m_9465_((String)"spiral_abyss_mob", UUID::compareTo, (int)40);
    private final AbyssParties parties;
    private final ServerLevel level;
    private final AbyssPlayers.Profile profile;
    private final FloorController controller;
    private final Set<UUID> participants = new LinkedHashSet<UUID>();
    private final Set<UUID> rewardParticipants = new LinkedHashSet<UUID>();
    private int[] attemptStars = new int[3];
    private FloorDefinition.Rewards offeredRewards;
    private int displayedTiers;
    private final Set<UUID> defeated = new HashSet<UUID>();
    private final Map<UUID, Vec3> lastSafe = new HashMap<UUID, Vec3>();
    private boolean displayReady;
    private long nextDisplayRefresh = Long.MAX_VALUE;
    private final Set<UUID> admitted = new LinkedHashSet<UUID>();
    private UUID leader;
    private final Set<UUID> observers = new HashSet<UUID>();

    public ArenaSession(AbyssParties parties, ServerLevel level, AbyssPlayers.Profile profile) {
        this.parties = parties;
        this.level = level;
        this.profile = profile;
        this.leader = profile.sectionOwner == null ? profile.owner : profile.sectionOwner;
        this.controller = new FloorController(this, FloorCatalog::snapshot);
        this.applyRewardDeliveryConfig();
        AbyssDimension.initializeArena(level, profile);
    }

    public boolean section() {
        return this.profile.sectionFloor != 0;
    }

    public UUID originalOwner() {
        return this.profile.sectionOwner == null ? this.host() : this.profile.sectionOwner;
    }

    public AbyssPlayers.Profile endlessProgress() {
        return this.section() ? this.parties.profile(this.originalOwner()) : this.profile;
    }

    public boolean observer(ServerPlayer player) {
        return this.observers.contains(player.m_20148_());
    }

    public boolean endless() {
        return this.profile.endlessMode;
    }

    public long endlessSeed(int wave) {
        return this.parties.cycle().bannerPoseSeed() ^ this.originalOwner().getMostSignificantBits() ^ this.originalOwner().getLeastSignificantBits() ^ -7046029254386353131L * (long)wave;
    }

    public ServerLevel level() {
        return this.level;
    }

    public AbyssPlayers.Profile profile() {
        return this.profile;
    }

    public List<Challenge> challenges(int floor) {
        return this.parties.challenges(floor);
    }

    public ResourceLocation selectedWaveType(int floor, int wave, FloorDefinition.Wave definition) {
        return this.parties.cycle().selectedType(floor, wave, definition);
    }

    public List<FloorDefinition.Enemy> cycleLineup(int floor, int wave, FloorDefinition.Wave definition) {
        return this.parties.cycleLineup(floor, wave, definition);
    }

    public UUID host() {
        return this.profile.owner;
    }

    public boolean isLeader(ServerPlayer player) {
        return player.m_20148_().equals(this.leader);
    }

    public ArenaBounds bounds() {
        return this.profile.bounds();
    }

    public FloorController controller() {
        return this.controller;
    }

    public boolean selectFloor(ServerPlayer player, int floor) {
        return this.parties.enterFloor(player, floor);
    }

    public boolean canAdvance(ServerPlayer player) {
        return this.canNavigate(player) && this.parties.profile((UUID)player.m_20148_()).progress.claimed(this.controller.currentFloor());
    }

    public boolean canNavigate(ServerPlayer player) {
        if (this.observer(player)) {
            return this.contains(player);
        }
        if (this.endless()) {
            return this.contains(player) && !this.controller.active();
        }
        boolean openedOwnPayout = (this.host().equals(player.m_20148_()) || this.section()) && this.profile.progress.hasPayouts();
        return this.contains(player) && !this.controller.active() && (!this.looting() || (this.rewardParticipants.contains(player.m_20148_()) || openedOwnPayout) && this.parties.profile((UUID)player.m_20148_()).progress.claimed(this.controller.currentFloor()));
    }

    public Set<UUID> participants() {
        return Set.copyOf(this.participants);
    }

    public Set<UUID> rewardParticipants() {
        return Set.copyOf(this.rewardParticipants);
    }

    public boolean partyCanEnterFloor(ServerPlayer requester, int floor) {
        for (ServerPlayer player : this.players()) {
            if (this.parties.checkUnlocked(requester, player, floor)) continue;
            return false;
        }
        return true;
    }

    public Set<UUID> admitted() {
        return Set.copyOf(this.admitted);
    }

    public boolean contains(ServerPlayer player) {
        return this.admitted.contains(player.m_20148_()) && player.m_9236_() == this.level;
    }

    public List<ServerPlayer> players() {
        return this.level.m_6907_().stream().filter(this::contains).filter(p -> !p.m_9232_()).toList();
    }

    public boolean canAdmit(UUID player) {
        if (this.section() || this.endless()) {
            return true;
        }
        if (this.looting()) {
            return this.admitted.contains(player) || this.admitted.isEmpty() && player.equals(this.host());
        }
        return !this.controller.active() || this.admitted.contains(player) && this.participants.contains(player);
    }

    public boolean looting() {
        int tier;
        if (this.endless()) {
            return this.controller.waitingForExperience();
        }
        for (tier = 1; tier <= 3; ++tier) {
            ChestPayout payout = this.profile.progress.payout(tier);
            if (!payout.active() || payout.container() || ((Boolean)AbyssConfig.NO_DROP_REWARD_CHESTS.get()).booleanValue() || !this.hasPayoutRecipient(payout)) continue;
            return true;
        }
        for (tier = 1; tier <= this.displayedTiers; ++tier) {
            if (this.eligibleForTier(tier).isEmpty()) continue;
            return true;
        }
        return false;
    }

    private void applyRewardDeliveryConfig() {
        if (this.endless()) {
            return;
        }
        for (int tier = 1; tier <= 3; ++tier) {
            ChestPayout payout = this.profile.progress.payout(tier);
            if (!payout.active()) continue;
            if (!this.section() && !payout.hasRecipients()) {
                payout.addRecipients(List.of(this.host()));
            }
            if (!((Boolean)AbyssConfig.NO_DROP_REWARD_CHESTS.get()).booleanValue() || payout.container()) continue;
            payout.useContainer(List.of());
        }
    }

    private boolean hasPayoutRecipient(ChestPayout payout) {
        return this.players().stream().anyMatch(player -> player.m_6084_() && !player.m_5833_() && payout.allows(player.m_20148_()));
    }

    private List<ServerPlayer> eligibleForTier(int tier) {
        if (this.offeredRewards == null) {
            return List.of();
        }
        List<ServerPlayer> present = this.players().stream().filter(p -> p.m_6084_() && !p.m_5833_()).toList();
        List<UUID> eligible = ChallengeRewards.eligible(this.rewardParticipants, present.stream().map(Entity::m_20148_).toList(), id -> this.parties.profile((UUID)id).progress, this.controller.currentFloor(), tier, this.attemptStars);
        return present.stream().filter(p -> eligible.contains(p.m_20148_())).toList();
    }

    public void completeChallenge(int[] stars, FloorDefinition.Rewards rewards) {
        this.attemptStars = (int[])stars.clone();
        this.offeredRewards = rewards;
        this.displayedTiers = 0;
        this.players().forEach(player -> player.m_213846_((Component)Component.m_237110_((String)"message.spiral_abyss.challenge_result", (Object[])new Object[]{this.controller.currentFloor(), Arrays.stream(stars).sum()}).m_130946_(" ").m_7220_((Component)Component.m_237110_((String)"message.spiral_abyss.menu_hint", (Object[])new Object[]{Component.m_237117_((String)"key.spiral_abyss.menu")}))));
        for (ServerPlayer player2 : this.players()) {
            if (!this.rewardParticipants.contains(player2.m_20148_())) continue;
            FloorProgress progress = this.parties.profile((UUID)player2.m_20148_()).progress;
            this.displayedTiers = Math.max(this.displayedTiers, Arrays.stream(progress.projectedStars(this.controller.currentFloor(), stars)).sum() / 3);
        }
    }

    public boolean openReward(ServerPlayer opener, int tier) {
        if (this.endless() || tier < 1 || tier > 3 || opener.m_5833_()) {
            return false;
        }
        this.applyRewardDeliveryConfig();
        ChestPayout saved = this.profile.progress.payout(tier);
        if (saved.active() && saved.container()) {
            return this.showRewardContainer(opener, saved, tier);
        }
        if (tier < 1 || tier > this.displayedTiers || this.controller.active() || !this.rewardParticipants.contains(opener.m_20148_())) {
            return false;
        }
        ChestPayout payout = this.profile.progress.payout(tier);
        if (payout.chestTicks() >= 0) {
            return false;
        }
        List<ServerPlayer> eligible = this.eligibleForTier(tier);
        if (eligible.isEmpty()) {
            FloorProgress personal = this.parties.profile((UUID)opener.m_20148_()).progress;
            if (personal.tierClaimed(this.controller.currentFloor(), tier)) {
                personal.recordStars(this.controller.currentFloor(), this.attemptStars);
            }
            this.refreshDisplay();
            return false;
        }
        FloorRewards.Bundle bundle = FloorRewards.roll(this.level, this.chestPosition(tier), this.offeredRewards, tier, eligible.size());
        for (ServerPlayer player : eligible) {
            this.parties.profile((UUID)player.m_20148_()).progress.commitClaim(this.controller.currentFloor(), tier, this.attemptStars);
        }
        FloorProgress personal = this.parties.profile((UUID)opener.m_20148_()).progress;
        if (personal.tierClaimed(this.controller.currentFloor(), tier)) {
            personal.recordStars(this.controller.currentFloor(), this.attemptStars);
        }
        payout.begin(bundle);
        payout.addRecipients(eligible.stream().map(Entity::m_20148_).toList());
        if (((Boolean)AbyssConfig.NO_DROP_REWARD_CHESTS.get()).booleanValue()) {
            payout.useContainer(List.of());
            this.finishContainerPayout(payout, tier);
        }
        this.refreshDisplay();
        if (payout.container()) {
            this.showRewardContainer(opener, payout, tier);
        }
        return true;
    }

    private boolean showRewardContainer(ServerPlayer player, ChestPayout payout, int tier) {
        if (!this.canTakeReward(player, payout, tier)) {
            return false;
        }
        this.finishContainerPayout(payout, tier);
        if (!payout.active()) {
            return true;
        }
        player.m_5893_((MenuProvider)new SimpleMenuProvider((id, inventory, ignored) -> new RewardChestMenu(id, inventory, this, payout, tier), (Component)Component.m_237115_((String)"screen.spiral_abyss.reward_chest")));
        return true;
    }

    public boolean canTakeReward(ServerPlayer player, ChestPayout payout, int tier) {
        return !this.endless() && tier >= 1 && tier <= 3 && this.contains(player) && player.m_6084_() && !player.m_5833_() && !this.controller.active() && payout != null && payout == this.profile.progress.payout(tier) && payout.active() && payout.container() && payout.allows(player.m_20148_()) && player.m_20182_().m_82557_(this.chestPosition(tier)) <= 64.0;
    }

    public void finishContainerPayout(ChestPayout payout, int tier) {
        if (!payout.active() || !payout.container() || this.controller.active() || !this.hasPayoutRecipient(payout)) {
            return;
        }
        FloorRewards.releaseExperience(this.level, this.chestPosition(tier), payout.takeExperience());
        if (!payout.empty() || payout.chestTicks() < 110) {
            return;
        }
        payout.complete();
        this.refreshDisplay();
    }

    private Vec3 chestPosition(int tier) {
        return this.bounds().center().m_82520_(tier == 2 ? -4.0 : (tier == 1 ? 4.0 : 0.0), 0.0, tier == 3 ? 0.0 : 2.0);
    }

    public void admit(ServerPlayer player) {
        boolean resuming;
        boolean busy = this.controller.active() || this.looting();
        boolean bl = resuming = !this.controller.active() && !this.endless() && this.players().stream().noneMatch(p -> this.rewardParticipants.contains(p.m_20148_()));
        if (resuming) {
            boolean ownsSaved = false;
            for (int tier = 1; tier <= 3; ++tier) {
                ownsSaved |= this.profile.progress.payout(tier).allows(player.m_20148_());
            }
            resuming = ownsSaved;
        }
        if (this.endless() && !this.controller.active() && player.m_20148_().equals(this.originalOwner())) {
            resuming = true;
        }
        this.admitted.add(player.m_20148_());
        if (this.leader == null || this.players().stream().noneMatch(this::isLeader)) {
            this.leader = player.m_20148_();
        }
        if (this.section() && !this.endless() && busy && !resuming) {
            this.observers.add(player.m_20148_());
            player.m_143403_(GameType.SPECTATOR);
            player.m_213846_((Component)Component.m_237115_((String)"message.spiral_abyss.section_spectating"));
        } else {
            ArenaSession.restoreSurvival(player);
            if (this.endless() && this.controller.active()) {
                this.participants.add(player.m_20148_());
            }
        }
    }

    public void remove(UUID player) {
        ServerPlayer departing = this.level.m_7654_().m_6846_().m_11259_(player);
        if (departing != null) {
            QliphothEncounters.clearPlayer(departing);
        }
        this.admitted.remove(player);
        this.lastSafe.remove(player);
        this.rewardParticipants.remove(player);
        if (this.observers.remove(player) && departing != null) {
            ArenaSession.restoreSurvival(departing);
        }
        List<ServerPlayer> remaining = this.players();
        if (player.equals(this.leader) && !remaining.isEmpty()) {
            List<ServerPlayer> candidates = remaining.stream().filter(p -> !p.m_5833_()).toList();
            if (candidates.isEmpty()) {
                candidates = remaining;
            }
            ServerPlayer next = candidates.get(this.level.f_46441_.m_188503_(candidates.size()));
            this.leader = next.m_20148_();
            remaining.forEach(member -> member.m_213846_((Component)Component.m_237110_((String)"message.spiral_abyss.host_changed", (Object[])new Object[]{next.m_5446_()})));
        }
        if (this.controller.active() && (this.combatants().isEmpty() || this.endless() && player.equals(this.originalOwner()))) {
            this.controller.defeated();
        }
        if (this.section() && this.endless() && player.equals(this.originalOwner()) && !remaining.isEmpty()) {
            this.controller.close();
            this.parties.changeSectionOwner(this, this.leader);
            this.releaseObservers();
            this.controller.close();
            this.refreshDisplay();
        }
    }

    public boolean captureParty() {
        List<ServerPlayer> living = this.players().stream().filter(p -> p.m_6084_() && !p.m_5833_()).toList();
        if (living.stream().noneMatch(this::isLeader) || this.endless() && living.stream().noneMatch(p -> p.m_20148_().equals(this.originalOwner()))) {
            return false;
        }
        this.participants.clear();
        this.rewardParticipants.clear();
        this.defeated.clear();
        this.offeredRewards = null;
        this.attemptStars = new int[3];
        this.displayedTiers = 0;
        living.forEach(p -> {
            this.participants.add(p.m_20148_());
            this.rewardParticipants.add(p.m_20148_());
        });
        return true;
    }

    public void clearParticipants() {
        this.participants.clear();
        this.rewardParticipants.clear();
        this.offeredRewards = null;
        this.attemptStars = new int[3];
        this.displayedTiers = 0;
    }

    public boolean isCombatant(ServerPlayer player) {
        return this.contains(player) && this.participants.contains(player.m_20148_()) && !this.defeated.contains(player.m_20148_()) && player.m_6084_() && !player.m_5833_();
    }

    public boolean isCombatantUsingTotem(ServerPlayer player) {
        return this.contains(player) && this.participants.contains(player.m_20148_()) && !this.defeated.contains(player.m_20148_()) && !player.m_213877_() && !player.m_9232_() && !player.m_5833_();
    }

    public List<ServerPlayer> combatants() {
        return this.players().stream().filter(this::isCombatant).toList();
    }

    public boolean waitingForParty(ServerPlayer player) {
        return this.observer(player) || this.controller.active() && this.defeated.contains(player.m_20148_());
    }

    public void defeat(ServerPlayer player) {
        ArenaSession.heal(player);
        if (this.controller.active() && this.participants.contains(player.m_20148_())) {
            this.defeated.add(player.m_20148_());
            this.markDefeated(player);
            player.m_143403_(GameType.SPECTATOR);
            this.returnToEntrance(player);
            if (this.combatants().isEmpty()) {
                this.controller.defeated();
            } else {
                player.m_213846_((Component)Component.m_237115_((String)"message.spiral_abyss.spectating"));
            }
        } else {
            this.recover(player, true);
        }
    }

    private void markDefeated(ServerPlayer player) {
        CompoundTag persisted = AbyssPlatform.data((Entity)player).m_128469_("PlayerPersisted");
        CompoundTag mark = new CompoundTag();
        mark.m_128362_("Host", this.host());
        mark.m_128362_("Run", this.controller.runId());
        persisted.m_128365_(DEFEATED_TAG, (Tag)mark);
        AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)persisted);
    }

    public static boolean hasDefeatMarker(ServerPlayer player) {
        return AbyssPlatform.data((Entity)player).m_128469_("PlayerPersisted").m_128441_(DEFEATED_TAG);
    }

    public static void restoreSurvival(ServerPlayer player) {
        CompoundTag persisted = AbyssPlatform.data((Entity)player).m_128469_("PlayerPersisted");
        persisted.m_128473_(DEFEATED_TAG);
        AbyssPlatform.data((Entity)player).m_128365_("PlayerPersisted", (Tag)persisted);
        player.m_9213_((Entity)player);
        player.m_143403_(GameType.SURVIVAL);
        ArenaSession.heal(player);
    }

    private static void heal(ServerPlayer player) {
        player.m_8127_();
        player.m_21219_();
        player.m_20095_();
        player.m_146917_(0);
        player.m_20301_(player.m_6062_());
        player.m_7911_(0.0f);
        player.m_21153_(player.m_21233_());
        player.f_20919_ = 0;
        player.f_19789_ = 0.0f;
        player.m_36324_().m_38705_(20);
        player.m_36324_().m_38717_(5.0f);
        player.f_19802_ = 60;
    }

    private void recover(ServerPlayer player, boolean teleport) {
        ArenaSession.restoreSurvival(player);
        if (teleport) {
            this.returnToEntrance(player);
        }
    }

    private void returnToEntrance(ServerPlayer player) {
        Vec3 entrance = this.bounds().entrance();
        player.m_9213_((Entity)player);
        player.m_8999_(this.level, entrance.f_82479_, entrance.f_82480_, entrance.f_82481_, 180.0f, 0.0f);
        player.m_20256_(Vec3.f_82478_);
        player.f_19789_ = 0.0f;
        this.lastSafe.put(player.m_20148_(), entrance);
    }

    public void finishAttempt() {
        this.controller.bossEnvironments().clear();
        AbyssBlockProtection.clearCombatHazards(this.level, this.bounds());
        AABB area = new AABB(this.bounds().center().f_82479_ - 128.0, 0.0, this.bounds().center().f_82481_ - 128.0, this.bounds().center().f_82479_ + 128.0, (double)this.level.m_151558_(), this.bounds().center().f_82481_ + 128.0);
        this.level.m_6249_((Entity)null, area, entity -> !(entity instanceof ArenaInteractable) && (entity instanceof Mob || entity instanceof Projectile || entity instanceof AreaEffectCloud || entity instanceof EvokerFangs)).forEach(Entity::m_146870_);
        for (ServerPlayer player : this.players()) {
            if (this.observer(player) || !this.defeated.contains(player.m_20148_()) && !ArenaSession.hasDefeatMarker(player)) continue;
            this.recover(player, true);
        }
        this.defeated.clear();
    }

    public boolean captureEndlessWave() {
        this.finishAttempt();
        return this.captureParty();
    }

    public void reviveForNextWave() {
        for (ServerPlayer player : this.players()) {
            if (!this.participants.contains(player.m_20148_()) || !this.defeated.remove(player.m_20148_())) continue;
            this.recover(player, true);
        }
    }

    public void releaseObservers() {
        for (ServerPlayer player : this.players()) {
            if (!this.observers.remove(player.m_20148_())) continue;
            this.recover(player, true);
        }
    }

    public void tick() {
        if (!this.displayReady && this.furnitureLoaded()) {
            AbyssBlockProtection.clearCombatHazards(this.level, this.bounds());
            this.clearDroppedRewards();
            this.displayReady = true;
            this.refreshDisplay();
        }
        for (ServerPlayer player2 : this.players()) {
            if (this.waitingForParty(player2) && !player2.m_5833_()) {
                player2.m_143403_(GameType.SPECTATOR);
            }
            this.containPlayer(player2);
        }
        if (!this.displayReady) {
            return;
        }
        this.applyRewardDeliveryConfig();
        this.controller.tick();
        if (!this.controller.active() && !this.looting()) {
            this.releaseObservers();
            if (this.players().stream().noneMatch(p -> this.isLeader((ServerPlayer)p) && !p.m_5833_())) {
                this.players().stream().filter(p -> !p.m_5833_()).findFirst().ifPresent(p -> {
                    this.leader = p.m_20148_();
                });
            }
        }
        for (int tier = 1; !this.endless() && tier <= 3; ++tier) {
            ChestPayout payout = this.profile.progress.payout(tier);
            if (payout.chestTicks() < 0 || this.controller.active() || !this.hasPayoutRecipient(payout)) continue;
            payout.tickOpening();
            FloorRewards.Bundle bundle = payout.pending();
            if (payout.container()) {
                this.finishContainerPayout(payout, tier);
                this.refreshDisplay();
                continue;
            }
            if (bundle != null && payout.chestTicks() >= 10) {
                int dispensingTicks = payout.chestTicks() - 10;
                if (dispensingTicks % 6 == 0) {
                    boolean rare = bundle.rare(payout.releasedItems());
                    ItemStack stack = payout.takeNextReward();
                    if (stack != null) {
                        FloorRewards.releaseItem(this.level, this.chestPosition(tier), stack, rare, this.controller.currentFloor());
                    }
                }
                if (payout.releasedItems() >= bundle.items().size() && (long)dispensingTicks >= (long)bundle.items().size() * 6L) {
                    int experience = bundle.experience();
                    payout.complete();
                    FloorRewards.releaseExperience(this.level, this.chestPosition(tier), experience);
                    if (!this.profile.progress.hasPayouts()) {
                        this.players().forEach(player -> player.m_213846_((Component)Component.m_237110_((String)"message.spiral_abyss.menu_hint", (Object[])new Object[]{Component.m_237117_((String)"key.spiral_abyss.menu")})));
                    }
                }
            }
            this.refreshDisplay();
        }
        if (this.level.m_46467_() >= this.nextDisplayRefresh || this.level.m_46467_() % 20L == 0L) {
            this.refreshDisplay();
        }
    }

    public void containPlayer(ServerPlayer player) {
        if (this.bounds().outsidePlayer((Entity)player)) {
            player.m_8127_();
            Vec3 safe = this.lastSafe.getOrDefault(player.m_20148_(), this.bounds().entrance());
            if (safe.f_82480_ < this.controller.bossEnvironments().waterSurface() || !this.level.m_45756_((Entity)player, player.m_20191_().m_82383_(safe.m_82546_(player.m_20182_())))) {
                safe = this.surfaceDestination((Entity)player, this.bounds().entrance());
            }
            player.m_8999_(this.level, safe.f_82479_, safe.f_82480_, safe.f_82481_, player.m_146908_(), player.m_146909_());
            player.m_20256_(Vec3.f_82478_);
            player.f_19789_ = 0.0f;
        } else if (player.m_20096_()) {
            this.lastSafe.put(player.m_20148_(), player.m_20182_());
        }
    }

    public void rescueMob(LivingEntity mob) {
        if (DragonArena.defeated(mob)) {
            return;
        }
        if (mob instanceof Mob) {
            Mob ai = (Mob)mob;
            ArenaFlight.steer(ai, this.bounds());
        }
        if (!this.bounds().rescueMob((Entity)mob, MobCatalog.isGhostMob(EntityType.m_20613_((EntityType)mob.m_6095_())))) {
            return;
        }
        Vec3 center = this.surfaceDestination((Entity)mob, this.bounds().center());
        mob.m_6021_(center.f_82479_, center.f_82480_ + (mob instanceof EnderDragon ? 20.0 : 0.1), center.f_82481_);
        if (mob instanceof Mob) {
            Mob ai = (Mob)mob;
            ArenaFlight.stop(ai);
        }
    }

    private boolean furnitureLoaded() {
        BlockPos center = BlockPos.m_274446_((Position)this.bounds().center());
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                if (this.level.m_143319_(ChunkPos.m_45589_((int)((center.m_123341_() >> 4) + x), (int)((center.m_123343_() >> 4) + z)))) continue;
                return false;
            }
        }
        return true;
    }

    private Vec3 surfaceDestination(Entity entity, Vec3 destination) {
        double water = this.controller.bossEnvironments().waterSurface();
        if (water > 0.0) {
            Vec3 candidate = new Vec3(destination.f_82479_, water, destination.f_82481_);
            if (this.level.m_45756_(entity, entity.m_20191_().m_82383_(candidate.m_82546_(entity.m_20182_())))) {
                return candidate;
            }
        }
        int y = 64;
        while ((double)y < 120.0) {
            BlockPos pos = BlockPos.m_274561_((double)destination.f_82479_, (double)y, (double)destination.f_82481_);
            VoxelShape shape = this.level.m_8055_(pos).m_60812_((BlockGetter)this.level, pos);
            if (!shape.m_83281_()) {
                double feet = (double)y + shape.m_83297_(Direction.Axis.Y);
                Vec3 candidate = new Vec3(destination.f_82479_, feet, destination.f_82481_);
                AABB box = entity.m_20191_().m_82383_(candidate.m_82546_(entity.m_20182_()));
                if (feet >= 65.0 && box.f_82292_ <= 120.0 && this.level.m_45756_(entity, box)) {
                    return candidate;
                }
            }
            ++y;
        }
        return destination;
    }

    public void keepMobActive(LivingEntity mob) {
        this.level.m_7726_().m_8387_(MOB_TICKET, mob.m_146902_(), 2, (Object)mob.m_20148_());
    }

    public void refreshDisplay() {
        if (!this.displayReady) {
            return;
        }
        this.nextDisplayRefresh = Long.MAX_VALUE;
        if (this.endless()) {
            this.refreshEndlessDisplay();
            return;
        }
        List furniture = this.level.m_45976_(ArenaInteractable.class, this.bounds().structureArea());
        boolean hide = this.controller.active();
        int chestMask = (1 << this.displayedTiers) - 1;
        for (int tier = 1; tier <= 3; ++tier) {
            if (this.profile.progress.payout(tier).chestTicks() < 0) continue;
            chestMask |= 1 << tier - 1;
        }
        boolean showChests = this.controller.showsChest() && chestMask != 0;
        boolean showKey = !showChests && (!this.section() || this.controller.state() != FloorController.State.COMPLETED);
        KeyEntity key = null;
        HashMap<Integer, ArenaInteractable> chests = new HashMap<Integer, ArenaInteractable>();
        for (ArenaInteractable entity : furniture) {
            ArenaInteractable candidate;
            if (hide || !this.host().equals(entity.arenaHost())) {
                entity.m_146870_();
                continue;
            }
            if (entity instanceof KeyEntity) {
                candidate = (KeyEntity)entity;
                if (showKey && key == null) {
                    key = candidate;
                    continue;
                }
            }
            if (entity instanceof LootChestEntity) {
                candidate = (LootChestEntity)entity;
                if (showChests && (chestMask & 1 << ((LootChestEntity)candidate).tier() - 1) != 0 && !chests.containsKey(((LootChestEntity)candidate).tier())) {
                    chests.put(((LootChestEntity)candidate).tier(), candidate);
                    continue;
                }
            }
            entity.m_146870_();
        }
        if (hide) {
            return;
        }
        if (showKey && key == null && (key = (KeyEntity)((EntityType)EntityInit.KEY_ENTITY.get()).m_20615_((Level)this.level)) != null) {
            key.setArenaHost(this.host());
            key.m_7678_(this.bounds().center().f_82479_, this.bounds().center().f_82480_, this.bounds().center().f_82481_, 0.0f, 0.0f);
            this.level.m_7967_((Entity)key);
        }
        if (!showChests) {
            return;
        }
        for (int tier = 1; tier <= 3; ++tier) {
            long start;
            int phase;
            boolean containerGlow;
            if ((chestMask & 1 << tier - 1) == 0) continue;
            LootChestEntity chest = (LootChestEntity)((Object)chests.get(tier));
            Vec3 position = this.chestPosition(tier);
            if (chest == null) {
                chest = (LootChestEntity)(tier == 1 ? (EntityType)EntityInit.COPPER_CHEST.get() : (tier == 2 ? (EntityType)EntityInit.IRON_CHEST.get() : (EntityType)EntityInit.GOLDEN_CHEST.get())).m_20615_((Level)this.level);
                if (chest == null) continue;
                chest.setArenaHost(this.host());
                chest.setPhase(4, this.level.m_46467_());
                chest.m_7678_(position.f_82479_, position.f_82480_, position.f_82481_, 0.0f, 0.0f);
                this.level.m_7967_((Entity)chest);
            } else if (chest.m_20182_().m_82557_(position) > 1.0E-4) {
                chest.m_6021_(position.f_82479_, position.f_82480_, position.f_82481_);
            }
            ChestPayout payout = this.profile.progress.payout(tier);
            int ticks = payout.chestTicks();
            boolean bl = containerGlow = payout.container() && (payout.hasRemainingRare() || ticks < 110);
            int n = ticks >= 0 ? (ticks < 10 ? 1 : (payout.container() && !containerGlow ? 3 : 2)) : (phase = !this.eligibleForTier(tier).isEmpty() ? 0 : 3);
            long l = ticks < 0 ? 0L : (start = this.level.m_46467_() - (long)ticks + (long)(phase == 2 ? 10 : 0));
            if (ticks >= 0) {
                chest.setRarePayout(payout.container() ? payout.hasRemainingRare() : payout.pending().hasRareItems());
            }
            if (ticks < 0 && chest.phase() == 4 && this.level.m_46467_() - chest.phaseStart() < 11L) {
                this.nextDisplayRefresh = Math.min(this.nextDisplayRefresh, chest.phaseStart() + 11L);
                continue;
            }
            if (ticks < 0 && chest.phase() == phase) continue;
            chest.setPhase(phase, ticks < 0 ? this.level.m_46467_() : start);
        }
    }

    private void refreshEndlessDisplay() {
        ExperienceChestEntity experienceChest;
        boolean chest = this.controller.showsEndlessChest();
        ArenaInteractable kept = null;
        for (ArenaInteractable entity : this.level.m_45976_(ArenaInteractable.class, this.bounds().structureArea())) {
            boolean wanted;
            boolean bl = (!this.controller.active() || chest) && this.host().equals(entity.arenaHost()) && (chest ? entity instanceof ExperienceChestEntity : entity instanceof KeyEntity) ? true : (wanted = false);
            if (wanted && kept == null) {
                kept = entity;
                continue;
            }
            entity.m_146870_();
        }
        if (this.controller.active() && !chest) {
            return;
        }
        if (kept == null) {
            ArenaInteractable arenaInteractable = kept = chest ? (ArenaInteractable)((EntityType)EntityInit.EXPERIENCE_CHEST.get()).m_20615_((Level)this.level) : (ArenaInteractable)((EntityType)EntityInit.KEY_ENTITY.get()).m_20615_((Level)this.level);
            if (kept != null) {
                kept.setArenaHost(this.host());
                if (kept instanceof ExperienceChestEntity) {
                    experienceChest = (ExperienceChestEntity)kept;
                    experienceChest.setPhase(4, this.level.m_46467_());
                }
                kept.m_7678_(this.bounds().center().f_82479_, this.bounds().center().f_82480_, this.bounds().center().f_82481_, 0.0f, 0.0f);
                this.level.m_7967_((Entity)kept);
            }
        }
        if (kept instanceof ExperienceChestEntity) {
            long start;
            int phase;
            experienceChest = (ExperienceChestEntity)kept;
            int ticks = this.endlessProgress().endlessPayout.chestTicks();
            int n = ticks >= 0 ? (ticks < 10 ? 1 : 2) : (phase = this.controller.waitingForExperience() ? 0 : 3);
            if (ticks < 0 && experienceChest.phase() == 4 && this.level.m_46467_() - experienceChest.phaseStart() < 11L) {
                this.nextDisplayRefresh = experienceChest.phaseStart() + 11L;
                return;
            }
            long l = ticks >= 0 ? this.level.m_46467_() - (long)ticks + (long)(phase == 2 ? 10 : 0) : (start = this.level.m_46467_());
            if (ticks >= 0 || experienceChest.phase() != phase) {
                experienceChest.setPhase(phase, start);
            }
        }
    }

    public void restoreStructure() {
        this.controller.bossEnvironments().clear();
        AbyssBlockProtection.clearCombatHazards(this.level, this.bounds());
        this.clearDroppedRewards();
        AbyssDimension.restoreArena(this.level, this.profile);
        this.lastSafe.clear();
        this.players().forEach(this::returnToEntrance);
    }

    public void resetArena() {
        this.controller.close();
        this.clearParticipants();
        this.restoreStructure();
        this.level.m_6249_((Entity)null, this.bounds().structureArea(), entity -> entity instanceof ArenaInteractable || entity instanceof ItemEntity || entity instanceof ExperienceOrb).forEach(Entity::m_146870_);
        this.refreshDisplay();
    }

    public void close() {
        this.releaseObservers();
        this.players().forEach(ServerPlayer::m_6915_);
        if (this.section() && !this.endless()) {
            this.profile.progress.clearPayouts(this.controller.currentFloor());
        }
        this.controller.close();
        this.clearParticipants();
        this.clearDroppedRewards();
        this.level.m_45976_(ArenaInteractable.class, this.bounds().structureArea()).forEach(Entity::m_146870_);
        AbyssDimension.releaseArena(this.level, this.profile);
    }

    private void clearDroppedRewards() {
        this.level.m_6249_((Entity)null, this.rewardArea(), entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb).forEach(Entity::m_146870_);
    }

    private AABB rewardArea() {
        Vec3 center = this.bounds().center();
        return new AABB(center.f_82479_ - 128.0, (double)this.level.m_141937_(), center.f_82481_ - 128.0, center.f_82479_ + 128.0, (double)this.level.m_151558_(), center.f_82481_ + 128.0);
    }
}

