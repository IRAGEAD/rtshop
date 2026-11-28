/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Difficulty
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironments;
import com.pla.spiral_abyss.config.GearConfig;
import com.pla.spiral_abyss.encounter.AbyssRejection;
import com.pla.spiral_abyss.encounter.BossHealthBars;
import com.pla.spiral_abyss.encounter.ChestPayout;
import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.encounter.EncounterReinforcements;
import com.pla.spiral_abyss.encounter.EndlessWaves;
import com.pla.spiral_abyss.encounter.FloorClock;
import com.pla.spiral_abyss.encounter.FloorProgress;
import com.pla.spiral_abyss.encounter.FloorRewards;
import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.encounter.WaveChallenges;
import com.pla.spiral_abyss.encounter.WaveComposition;
import com.pla.spiral_abyss.encounter.WaveEquipment;
import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.encounter.WaveStages;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.challenge.ChallengeTracker;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.world.ArenaSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public final class FloorController {
    private final ServerLevel level;
    private final ArenaSession session;
    private double partyScale = 1.0;
    private final FloorProgress progress;
    private final Supplier<FloorCatalog.Snapshot> catalog;
    private final WaveSpawner spawner = new WaveSpawner();
    private final BossHealthBars bossHealthBars = new BossHealthBars();
    private final FloorClock clock = new FloorClock();
    private final WaveStages<LivingEntity> stages = new WaveStages();
    private final BossEnvironments bossEnvironments;
    private final EncounterReinforcements reinforcements;
    private static final int CLEAR_DELAY_TICKS = 20;
    private long emptySince = -1L;
    private WaveEquipment waveEquipment;
    private ResourceLocation selectedWaveType;
    private State state;
    private FloorDefinition floor;
    private MobCatalog.Equipment equipment;
    private MobCatalog.ScalingSnapshot mobScaling = MobCatalog.ScalingSnapshot.EMPTY;
    private GearConfig.Floor gear;
    private Map<ResourceLocation, List<FloorDefinition.RewardEntry>> mobRewards = Map.of();
    private final Set<ResourceLocation> encounteredMobs = new LinkedHashSet<ResourceLocation>();
    private UUID runId;
    private Vec3 center = new Vec3(0.5, 65.0, 0.5);
    private int waveIndex;
    private int delayTicks;
    private int waveEnemies;
    private boolean endlessLooted;
    private final int[] attemptStars = new int[3];
    private List<Challenge> challenges = List.of();
    private ChallengeTracker challengeTracker;
    private int revision;
    private AbyssNetwork.EncounterStatus readyHud;
    private int readyHudFloor;
    private List<Challenge> readyChallenges = List.of();

    public FloorController(ArenaSession session, Supplier<FloorCatalog.Snapshot> catalog) {
        this.session = session;
        this.level = session.level();
        this.progress = session.profile().progress;
        this.center = session.bounds().center();
        this.catalog = catalog;
        this.bossEnvironments = new BossEnvironments(this.level, session.bounds(), session.host());
        this.reinforcements = new EncounterReinforcements(session);
        State state = this.state = this.progress.pending() != null ? State.COMPLETED : State.READY;
        if (session.endless()) {
            this.state = State.READY;
        }
        this.revision = catalog.get().revision();
    }

    public State state() {
        return this.state;
    }

    public BossEnvironments bossEnvironments() {
        return this.bossEnvironments;
    }

    public int currentFloor() {
        return this.session.endless() ? 7 : this.progress.floor();
    }

    public int currentWave() {
        return this.session.endless() && !this.active() ? this.session.endlessProgress().endlessWave : this.waveIndex + 1;
    }

    public int remainingTicks() {
        return this.clock.remainingTicks();
    }

    public int remainingEnemies() {
        return this.stages.enemies().size();
    }

    public List<LivingEntity> enemies() {
        return this.stages.enemies();
    }

    public ChallengeTracker challengeTracker() {
        return this.challengeTracker;
    }

    public List<ChallengeTracker.Progress> challengeProgress() {
        return this.challengeTracker == null ? List.of() : this.challengeTracker.progress();
    }

    public boolean waitingForExperience() {
        return this.session.endless() && this.session.endlessProgress().endlessCheckpoint > 0 && !this.active();
    }

    public boolean showsEndlessChest() {
        return this.waitingForExperience() || this.session.endless() && this.endlessLooted && this.state == State.BETWEEN_WAVES;
    }

    public AbyssNetwork.EncounterStatus hudStatus(ServerPlayer viewer) {
        if (this.session.endless()) {
            return new AbyssNetwork.EncounterStatus(true, !this.active(), 7, this.currentWave(), this.stages.removed(), this.waveEnemies, this.active() ? this.clock.remainingSeconds() : 0, this.state == State.BETWEEN_WAVES ? WaveChallenges.countdownSeconds(this.delayTicks) : 0, List.of());
        }
        if (this.active()) {
            List<AbyssNetwork.ChallengeView> views = this.challengeProgress().stream().map(p -> new AbyssNetwork.ChallengeView(p.description(), p.progress(), p.target(), p.complete(), p.failed())).toList();
            return new AbyssNetwork.EncounterStatus(true, false, this.currentFloor(), this.currentWave(), this.stages.removed(), this.waveEnemies, this.clock.remainingSeconds(), this.state == State.BETWEEN_WAVES ? WaveChallenges.countdownSeconds(this.delayTicks) : 0, views, this.stages.enemies().stream().filter(mob -> {
                EnderDragon dragon;
                return this.session.isCombatant(viewer) && mob.m_6084_() && !mob.m_213877_() && (!(mob instanceof EnderDragon) || !DragonArena.defeated((LivingEntity)(dragon = (EnderDragon)mob))) && AbyssRejection.applies(viewer, mob);
            }).map(AbyssRejection::bossId).distinct().toList());
        }
        if (this.state != State.READY && this.state != State.FAILED || this.showsChest()) {
            return AbyssNetwork.EncounterStatus.HIDDEN;
        }
        if (this.readyHud == null || this.readyHudFloor != this.currentFloor()) {
            this.readyHudFloor = this.currentFloor();
            this.readyHud = this.prepareHudStatus();
        }
        return this.readyHudFor(viewer);
    }

    private AbyssNetwork.EncounterStatus readyHudFor(ServerPlayer viewer) {
        if (this.readyHud == AbyssNetwork.EncounterStatus.HIDDEN) {
            return this.readyHud;
        }
        ArrayList<AbyssNetwork.ChallengeView> views = null;
        for (int index = 0; index < this.readyChallenges.size(); ++index) {
            boolean blocked;
            Challenge challenge = this.readyChallenges.get(index);
            switch (challenge.type()) {
                case NO_ARMOR: {
                    boolean bl = challenge.forbiddenSlots().stream().anyMatch(slot -> !viewer.m_6844_(slot).m_41619_());
                    break;
                }
                case ARMOR_ENCHANTMENT: {
                    boolean bl;
                    if (!ChallengeTracker.wearsArmorEnchantments(viewer, challenge.ids())) {
                        bl = true;
                        break;
                    }
                    bl = false;
                    break;
                }
                default: {
                    boolean bl = blocked = false;
                }
            }
            if (!blocked) continue;
            if (views == null) {
                views = new ArrayList<AbyssNetwork.ChallengeView>(this.readyHud.challenges());
            }
            AbyssNetwork.ChallengeView original = (AbyssNetwork.ChallengeView)views.get(index);
            views.set(index, new AbyssNetwork.ChallengeView(original.description(), original.progress(), original.target(), false, true));
        }
        return views == null ? this.readyHud : new AbyssNetwork.EncounterStatus(true, true, this.readyHud.floor(), this.readyHud.wave(), 0, 0, 0, 0, views);
    }

    private AbyssNetwork.EncounterStatus prepareHudStatus() {
        this.readyChallenges = List.of();
        FloorDefinition definition = this.catalog.get().floors().get(this.currentFloor());
        if (definition == null || definition.waves().isEmpty()) {
            return AbyssNetwork.EncounterStatus.HIDDEN;
        }
        try {
            List<Challenge> selected = this.session.challenges(this.currentFloor());
            if (selected.size() != 3) {
                return AbyssNetwork.EncounterStatus.HIDDEN;
            }
            int population = this.session.cycleLineup(this.currentFloor(), 1, definition.waves().get(0)).stream().mapToInt(FloorDefinition.Enemy::count).sum();
            List<AbyssNetwork.ChallengeView> views = selected.stream().map(challenge -> new AbyssNetwork.ChallengeView(challenge.description(population), 0, challenge.type() == Challenge.Type.TIME || challenge.type() == Challenge.Type.NO_ARMOR || challenge.type() == Challenge.Type.ARMOR_ENCHANTMENT ? 0 : challenge.requiredCount(population), false, false)).toList();
            this.readyChallenges = List.copyOf(selected);
            return new AbyssNetwork.EncounterStatus(true, true, this.currentFloor(), 1, 0, 0, 0, 0, views);
        }
        catch (IllegalArgumentException ex) {
            LogUtils.getLogger().warn("Cannot preview Abyss floor {} preparation challenges: {}", (Object)this.currentFloor(), (Object)ex.getMessage());
            return AbyssNetwork.EncounterStatus.HIDDEN;
        }
    }

    public UUID runId() {
        return this.runId;
    }

    public boolean showsChest() {
        return this.session.endless() ? this.waitingForExperience() : this.state == State.COMPLETED || this.progress.pending() != null;
    }

    public boolean start(ServerPlayer player, Vec3 keyPosition) {
        this.refreshDefinitions();
        if (player.m_9236_() != this.level || !this.session.contains(player)) {
            return false;
        }
        if (!player.m_6084_() || player.m_5833_()) {
            return this.message(player, "spectator", new Object[0]);
        }
        if (this.active()) {
            return this.message(player, "already_running", new Object[0]);
        }
        if (this.session.section() && !this.session.endless() && this.state == State.COMPLETED) {
            return this.message(player, "already_completed", new Object[0]);
        }
        if (this.showsChest() && this.session.looting()) {
            return this.message(player, "already_completed", new Object[0]);
        }
        if (this.level.m_46791_() == Difficulty.PEACEFUL) {
            return this.message(player, "peaceful", new Object[0]);
        }
        if (this.session.endless()) {
            return this.startEndless(player);
        }
        if (!this.session.partyCanEnterFloor(player, this.progress.floor())) {
            return false;
        }
        this.floor = this.catalog.get().floors().get(this.progress.floor());
        if (this.floor == null) {
            return this.message(player, "missing_floor", this.progress.floor());
        }
        try {
            this.challenges = List.copyOf(this.session.challenges(this.currentFloor()));
            if (this.challenges.size() != 3) {
                throw new IllegalArgumentException("Expected three challenges");
            }
        }
        catch (IllegalArgumentException ex) {
            LogUtils.getLogger().warn("Floor {} has unavailable challenges", (Object)this.currentFloor(), (Object)ex);
            return this.message(player, "challenges_unavailable", new Object[0]);
        }
        if (!this.session.captureParty()) {
            return this.message(player, "host_required", new Object[0]);
        }
        this.equipment = MobCatalog.equipment();
        this.mobScaling = MobCatalog.scalingSnapshot();
        this.mobRewards = MobCatalog.rewards();
        this.encounteredMobs.clear();
        this.gear = GearConfig.load(this.currentFloor());
        this.partyScale = FloorRewards.partyScale(this.session.participants().size());
        this.runId = UUID.randomUUID();
        this.waveIndex = 0;
        this.delayTicks = 0;
        Arrays.fill(this.attemptStars, 0);
        return this.startWave(player);
    }

    public boolean nextFloor(ServerPlayer player) {
        this.refreshDefinitions();
        if (player.m_9236_() != this.level || !this.session.contains(player)) {
            return this.message(player, "wrong_dimension", new Object[0]);
        }
        if (player.m_5833_()) {
            return this.message(player, "spectator", new Object[0]);
        }
        if (!this.session.isLeader(player)) {
            return this.message(player, "host_only", new Object[0]);
        }
        if (this.active()) {
            return this.message(player, "already_running", new Object[0]);
        }
        if (!this.session.canAdvance(player)) {
            return this.message(player, "claim_first", new Object[0]);
        }
        if (this.progress.floor() >= 6) {
            return this.message(player, "last_floor", new Object[0]);
        }
        int next = this.progress.floor() + 1;
        if (!this.catalog.get().floors().containsKey(next)) {
            return this.message(player, "missing_floor", next);
        }
        return this.session.selectFloor(player, next);
    }

    public void tick() {
        this.refreshDefinitions();
        if (this.waitingForExperience() && this.session.endlessProgress().endlessPayout.active()) {
            this.tickEndlessPayout();
            return;
        }
        if (!this.active()) {
            return;
        }
        ServerPlayer target = this.session.combatants().stream().findFirst().orElse(null);
        if (target == null) {
            this.fail("abandoned");
            return;
        }
        if (this.level.m_46791_() == Difficulty.PEACEFUL) {
            this.fail("peaceful");
            return;
        }
        if (this.state == State.BETWEEN_WAVES) {
            if (--this.delayTicks <= 0) {
                this.session.reviveForNextWave();
                ++this.waveIndex;
                if (this.session.endless() && !this.session.captureEndlessWave()) {
                    this.fail("host_required");
                    return;
                }
                this.startWave(target);
            }
            return;
        }
        if (this.clock.tick(true)) {
            this.fail("time_up");
            return;
        }
        if (this.challengeTracker != null) {
            this.challengeTracker.update(this.clock.elapsedTicks());
        }
        this.reinforcements.discover(this.runId, mob -> {
            if (this.stages.add((LivingEntity)mob)) {
                this.emptySince = -1L;
            }
        });
        this.waveEnemies = this.stages.total();
        this.stages.removeInvalid(mob -> DragonArena.finished(mob, this.level));
        if (!this.stages.enemies().isEmpty()) {
            this.emptySince = -1L;
        } else if (this.emptySince < 0L) {
            this.emptySince = this.level.m_46467_();
        }
        boolean settled = this.emptySince >= 0L && this.level.m_46467_() - this.emptySince >= 20L;
        try {
            this.bossEnvironments.tick(mob -> !settled && this.reinforcements.retainsEnvironment((LivingEntity)mob));
        }
        catch (RuntimeException ex) {
            LogUtils.getLogger().error("Boss environment failed in floor {} wave {}", new Object[]{this.currentFloor(), this.currentWave(), ex});
            this.fail("spawn_failed");
            return;
        }
        this.bossHealthBars.tick(this.stages.enemies(), this.session.players());
        for (LivingEntity enemy : this.stages.enemies()) {
            ServerPlayer p;
            LivingEntity livingEntity;
            Mob mob2;
            if (DragonArena.defeated(enemy)) {
                this.session.keepMobActive(enemy);
                continue;
            }
            this.session.rescueMob(enemy);
            this.session.keepMobActive(enemy);
            if (!(enemy instanceof Mob) || (mob2 = (Mob)enemy).m_5448_() != null && mob2.m_5448_().m_6084_() && !mob2.m_5448_().m_5833_() && mob2.m_5448_().m_9236_() == this.level && (!((livingEntity = mob2.m_5448_()) instanceof ServerPlayer) || this.session.isCombatant(p = (ServerPlayer)livingEntity))) continue;
            mob2.m_6710_((LivingEntity)target);
        }
        if (this.stages.enemies().isEmpty()) {
            if (!settled) {
                return;
            }
            if (this.bossEnvironments.cleaning()) {
                return;
            }
            LogUtils.getLogger().debug("Floor {} wave {} type {} stage {}/{} cleared", new Object[]{this.currentFloor(), this.currentWave(), this.selectedWaveType, this.stages.index() + 1, this.stages.size()});
            if (!this.stages.complete()) {
                try {
                    this.spawnStage(target);
                }
                catch (RuntimeException ex) {
                    LogUtils.getLogger().error("Unable to spawn floor {} wave {} next stage", new Object[]{this.currentFloor(), this.currentWave(), ex});
                    this.fail("spawn_failed");
                }
                return;
            }
            LogUtils.getLogger().debug("Wave type {} complete; floor {} wave {} complete", new Object[]{this.selectedWaveType, this.currentFloor(), this.currentWave()});
            if (this.session.endless()) {
                this.completeEndlessWave();
                return;
            }
            if (this.waveIndex < this.attemptStars.length) {
                this.attemptStars[this.waveIndex] = this.challengeTracker.stars();
            }
            if (this.waveIndex + 1 == this.floor.waves().size()) {
                this.complete();
            } else {
                this.state = State.BETWEEN_WAVES;
                this.delayTicks = 100;
                this.session.reviveForNextWave();
            }
        }
    }

    private boolean startWave(ServerPlayer target) {
        try {
            if (this.session.endless()) {
                this.endlessLooted = false;
                int wave = this.waveIndex + 1;
                this.session.endlessProgress().saveEndlessWave(wave);
                WaveComposition.Selection selected = EndlessWaves.select(wave, this.session.endlessSeed(wave), FloorCatalog.waveTypes().values(), MobCatalog.snapshot(), this.equipment);
                this.stages.prepare(selected.enemies());
                this.selectedWaveType = selected.type();
                this.gear = GearConfig.loadEndless(wave);
                this.partyScale = FloorRewards.partyScale(this.session.participants().size());
                this.runId = UUID.randomUUID();
            } else {
                FloorDefinition.Wave wave = this.floor.waves().get(this.waveIndex);
                this.stages.prepare(this.session.cycleLineup(this.currentFloor(), this.currentWave(), wave));
                this.selectedWaveType = this.session.selectedWaveType(this.currentFloor(), this.currentWave(), wave);
            }
            this.waveEquipment = new WaveEquipment(this.equipment, this.gear, this.level.f_46441_);
            this.waveEnemies = this.stages.total();
            this.spawnStage(target);
            this.challengeTracker = this.session.endless() ? null : new ChallengeTracker(this.session, this.challenges, this.waveEnemies);
            this.clock.start(6000);
            this.state = State.RUNNING;
            this.session.refreshDisplay();
            return true;
        }
        catch (RuntimeException ex) {
            LogUtils.getLogger().error("Unable to spawn floor {} wave {}", new Object[]{this.currentFloor(), this.currentWave(), ex});
            this.fail("spawn_failed");
            return false;
        }
    }

    private void spawnStage(ServerPlayer target) {
        this.emptySince = -1L;
        this.stages.advance(composition -> {
            LogUtils.getLogger().debug("Floor {} wave {} type {} stage {}/{} starting: {}", new Object[]{this.currentFloor(), this.currentWave(), this.selectedWaveType, this.stages.index() + 1, this.stages.size(), composition});
            List<LivingEntity> created = this.spawner.spawn(this.level, this.center, target, (List<FloorDefinition.Enemy>)composition, this.runId, this.waveEquipment, this.currentFloor(), this.partyScale, this.mobScaling, this.session.endless() ? this.waveIndex + 1 : 0);
            created.forEach(mob -> this.encounteredMobs.add(AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_())));
            created.forEach(mob -> AbyssPlatform.data((Entity)mob).m_128362_("spiral_abyss_host", this.session.host()));
            this.reinforcements.begin(created);
            return created;
        });
        this.bossEnvironments.begin(this.stages.enemies());
    }

    private boolean startEndless(ServerPlayer player) {
        if (this.waitingForExperience()) {
            return this.message(player, "already_completed", new Object[0]);
        }
        if (!this.session.captureParty()) {
            return this.message(player, "host_required", new Object[0]);
        }
        this.equipment = MobCatalog.equipment();
        this.mobScaling = MobCatalog.scalingSnapshot();
        this.encounteredMobs.clear();
        this.challenges = List.of();
        this.waveIndex = this.session.endlessProgress().endlessWave - 1;
        this.delayTicks = 0;
        return this.startWave(player);
    }

    private void completeEndlessWave() {
        this.bossHealthBars.clear();
        this.session.finishAttempt();
        int wave = this.waveIndex + 1;
        this.session.endlessProgress().saveEndlessWave(wave);
        if (wave % 10 == 0 && wave > this.session.endlessProgress().endlessRewardedWave) {
            this.session.endlessProgress().saveEndlessCheckpoint(wave);
            this.state = State.COMPLETED;
            this.runId = null;
        } else {
            this.state = State.BETWEEN_WAVES;
            this.delayTicks = 100;
        }
        this.session.refreshDisplay();
    }

    public boolean openEndlessCheckpoint(ServerPlayer player) {
        if (!this.waitingForExperience() || !this.session.contains(player) || !player.m_6084_() || player.m_5833_() || this.session.endlessProgress().endlessPayout.active() || !this.session.originalOwner().equals(player.m_20148_()) && !this.session.participants().contains(player.m_20148_()) || this.session.players().stream().noneMatch(p -> p.m_20148_().equals(this.session.originalOwner()) && p.m_6084_() && !p.m_5833_())) {
            return false;
        }
        int wave = this.session.endlessProgress().endlessCheckpoint;
        int experience = wave > this.session.endlessProgress().endlessRewardedWave ? EndlessWaves.experience(wave, this.level.f_46441_) : 0;
        this.session.endlessProgress().markEndlessRewarded(wave);
        this.session.endlessProgress().endlessPayout.begin(new FloorRewards.Bundle(List.of(), experience));
        this.session.refreshDisplay();
        return true;
    }

    private void tickEndlessPayout() {
        if (this.session.players().stream().noneMatch(p -> p.m_20148_().equals(this.session.originalOwner()) && p.m_6084_() && !p.m_5833_())) {
            return;
        }
        ChestPayout payout = this.session.endlessProgress().endlessPayout;
        payout.tickOpening();
        if (payout.chestTicks() < 16) {
            this.session.refreshDisplay();
            return;
        }
        int experience = payout.pending().experience();
        payout.complete();
        int wave = this.session.endlessProgress().endlessCheckpoint;
        this.session.endlessProgress().saveEndlessCheckpoint(0);
        FloorRewards.releaseExperience(this.level, this.center, experience);
        this.waveIndex = wave - 1;
        this.equipment = MobCatalog.equipment();
        this.mobScaling = MobCatalog.scalingSnapshot();
        this.session.releaseObservers();
        if (!this.session.captureParty()) {
            this.state = State.READY;
            this.session.refreshDisplay();
            return;
        }
        this.endlessLooted = true;
        this.runId = UUID.randomUUID();
        this.state = State.BETWEEN_WAVES;
        this.delayTicks = 100;
        this.session.refreshDisplay();
    }

    private void complete() {
        this.session.completeChallenge((int[])this.attemptStars.clone(), FloorRewards.withMobRewards(this.floor.rewards(), this.encounteredMobs, this.mobRewards));
        this.session.finishAttempt();
        this.state = State.COMPLETED;
        this.clock.clear();
        this.runId = null;
        this.session.refreshDisplay();
    }

    private void fail(String reason) {
        this.cleanup();
        this.session.finishAttempt();
        this.state = State.FAILED;
        this.session.clearParticipants();
        this.session.refreshDisplay();
        this.announce(reason, new Object[0]);
    }

    private void refreshDefinitions() {
        int nextRevision = this.catalog.get().revision();
        if (this.revision == nextRevision) {
            return;
        }
        this.revision = nextRevision;
        this.readyHud = null;
        this.readyChallenges = List.of();
        if (this.active()) {
            this.fail("reloaded");
        }
    }

    public boolean owns(Entity entity) {
        return this.runId != null && this.stages.enemies().contains(entity) && AbyssPlatform.data(entity).m_128403_("spiral_abyss_run") && this.runId.equals(AbyssPlatform.data(entity).m_128342_("spiral_abyss_run"));
    }

    public void close() {
        this.cleanup();
        this.session.finishAttempt();
        this.state = !this.session.endless() && this.progress.pending() != null ? State.COMPLETED : State.READY;
        this.readyHud = null;
        this.readyChallenges = List.of();
    }

    public void defeated() {
        if (this.active()) {
            this.fail("party_defeated");
        }
    }

    private void cleanup() {
        this.endlessLooted = false;
        this.bossHealthBars.clear();
        this.bossEnvironments.clear();
        this.stages.clear(entity -> {
            QliphothEncounters.clear(entity);
            entity.m_146870_();
        });
        this.reinforcements.clear();
        this.emptySince = -1L;
        this.selectedWaveType = null;
        this.waveEquipment = null;
        this.mobScaling = MobCatalog.ScalingSnapshot.EMPTY;
        this.encounteredMobs.clear();
        this.runId = null;
        this.clock.clear();
        this.challengeTracker = null;
        this.waveEnemies = 0;
        this.delayTicks = 0;
    }

    public boolean active() {
        return this.state == State.RUNNING || this.state == State.BETWEEN_WAVES;
    }

    private void announce(String key, Object ... args) {
        this.session.players().forEach(player -> player.m_5661_((Component)Component.m_237110_((String)("message.spiral_abyss." + key), (Object[])args), false));
    }

    private boolean message(ServerPlayer player, String key, Object ... args) {
        player.m_5661_((Component)Component.m_237110_((String)("message.spiral_abyss." + key), (Object[])args), true);
        return false;
    }

    public static enum State {
        READY,
        RUNNING,
        BETWEEN_WAVES,
        FAILED,
        COMPLETED;

    }
}

