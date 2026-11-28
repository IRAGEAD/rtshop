/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.init.FDScreenEffects
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.network.lib_packets.PlaySoundInEarsPacket
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.cutscenes.CameraPos
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData
 *  com.finderfeed.fdlib.systems.cutscenes.EasingType
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackAction
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackChain
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackInstance
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackOptions
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackOptions$ChainAttackOptions
 *  com.finderfeed.fdlib.systems.hud.bossbars.FDServerBossBar
 *  com.finderfeed.fdlib.systems.impact_frames.ImpactFrame
 *  com.finderfeed.fdlib.systems.impact_frames.ImpactFramesPacket
 *  com.finderfeed.fdlib.systems.music.data.FDMusicData
 *  com.finderfeed.fdlib.systems.music.data.FDMusicPartData
 *  com.finderfeed.fdlib.systems.music.music_areas.FDMusicArea
 *  com.finderfeed.fdlib.systems.music.music_areas.FDMusicAreasHandler
 *  com.finderfeed.fdlib.systems.music.music_areas.shapes.FDMusicAreaCylinder
 *  com.finderfeed.fdlib.systems.music.music_areas.shapes.FDMusicAreaShape
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData
 *  com.finderfeed.fdlib.systems.shake.DefaultShakePacket
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.FDTargetFinder
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.core.particles.SimpleParticleType
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectCategory
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Targeting
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$PlayerTickEvent
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$Clone
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.BossDespawner;
import com.finderfeed.fdbosses.content.entities.FDDespawnable;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerContextAssignable;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahBossBuddy;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahBossInitializer;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahBossSpawner;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahLaserAttackPreparator;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahRayController;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahSecondPhaseInitializer;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahSinnedPacket;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahStompingController;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahSyncRayVisualsPacket;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircle;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircleJudgementBird;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahChainTrapCastCircle;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahSinCrystalCastCircle;
import com.finderfeed.fdbosses.content.entities.geburah.chain_trap.ChainTrapSummonProjectile;
import com.finderfeed.fdbosses.content.entities.geburah.chain_trap.GeburahChainTrapEntity;
import com.finderfeed.fdbosses.content.entities.geburah.distortion_sphere.StartGeburahDistortionEffectPacket;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_bell.GeburahBell;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_earthquake.GeburahEarthquake;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_explosive_crystal.GeburahSinCrystal;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.GeburahWeaponAttack;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.GeburahWeaponAttackController;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.instances.GeburahAttackFireDefaultProjectiles;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.instances.GeburahLasersAttack;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.instances.GeburahRoundAndRoundLaserAttack;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile.JudgementBallProjectile;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_bird.JudgementBirdEntity;
import com.finderfeed.fdbosses.content.entities.geburah.justice_hammer.JusticeHammerAttack;
import com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block.GeburahRespiteBlock;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdbosses.content.entities.geburah.scales_controller.GeburahScalesController;
import com.finderfeed.fdbosses.content.entities.geburah.sins.PlayerSinsHandler;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.ActivePlayerSinInstance;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.util.CylinderPlayerPositionsCollector;
import com.finderfeed.fdbosses.content.util.HorizontalCircleRandomDirections;
import com.finderfeed.fdbosses.content.util.WorldBox;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossBars;
import com.finderfeed.fdbosses.init.BossBlocks;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossCriteriaTriggers;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.init.GeburahSins;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.init.FDScreenEffects;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.network.lib_packets.PlaySoundInEarsPacket;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.cutscenes.CameraPos;
import com.finderfeed.fdlib.systems.cutscenes.CutsceneData;
import com.finderfeed.fdlib.systems.cutscenes.EasingType;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackAction;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackChain;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackInstance;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackOptions;
import com.finderfeed.fdlib.systems.hud.bossbars.FDServerBossBar;
import com.finderfeed.fdlib.systems.impact_frames.ImpactFrame;
import com.finderfeed.fdlib.systems.impact_frames.ImpactFramesPacket;
import com.finderfeed.fdlib.systems.music.data.FDMusicData;
import com.finderfeed.fdlib.systems.music.data.FDMusicPartData;
import com.finderfeed.fdlib.systems.music.music_areas.FDMusicArea;
import com.finderfeed.fdlib.systems.music.music_areas.FDMusicAreasHandler;
import com.finderfeed.fdlib.systems.music.music_areas.shapes.FDMusicAreaCylinder;
import com.finderfeed.fdlib.systems.music.music_areas.shapes.FDMusicAreaShape;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData;
import com.finderfeed.fdlib.systems.shake.DefaultShakePacket;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.FDTargetFinder;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class GeburahEntity
extends FDLivingEntity
implements AutoSerializable,
GeburahBossBuddy,
BossSpawnerContextAssignable,
Targeting,
FDDespawnable {
    public final FDServerBossBar bossBar = new FDServerBossBar(BossBars.GEBURAG_BOSS_BAR, (Entity)this);
    public static final int SINNED_CLIENT_ANIM_DURATION = 25;
    public static final int MAX_GEBURAH_SINS = 10;
    public static final int SIN_PUNISHMENT_ATTACK_DURATION = 40;
    public static final int ATTACK_START_DELAY = 40;
    public static final int CANNONS_AMOUNT = 8;
    public static final float RAY_PREPARATION_PARTICLES_OFFSET = 0.05f;
    public static final float STOMP_PREPARATION_PARTICLES_OFFSET = 0.01f;
    public static final float LASERS_PREPARATION_OFFSET = 0.03f;
    public static final float RAY_DECAL_OFFSET = 0.03f;
    public static final String SIMPLE_NO_SIN_RUN_AROUND = "simple_no_sin_run_around";
    public static final String NO_SIN_SECOND_PHASE = "no_sin_second_phase";
    public static final String RUN_CLOCKWISE_HAMMERS_RAY_PROJECTILES = "run_clockwise_hammers_ray_projectiles";
    public static final String LIMITED_BUTTONS_LASERS_AND_EARTHQUAKES = "limited_buttons_lasers_and_earthquakes";
    public static final String NO_JUMP_RAYS_EARTHQUAKES_PROJECTILES = "no_jump_rays_earthquakes_projectiles";
    public static final String SIN_CRYSTALS_LASERS_AND_CANNONS = "sin_crystals_lasers_and_cannons";
    public static final String NO_KILL_ENTITIES_ATTACK = "no_kill_entities_attack";
    public static final String BELL_ATTACK = "bell_attack";
    public static final String EMPTY_SINS_AND_DELAY = "empty_sins_and_delay";
    public static final String GEBURAH_STOMPING_LAYER = "stomping";
    public static final String GEBURAH_CANNONS_LAYER = "cannons";
    public static final String MAIN_LAYER = "layer";
    public static final int ARENA_HEIGHT = 30;
    public static final int ARENA_RADIUS = 32;
    public static final float MAX_LASERS_RADIUS = 32.0f;
    public static final float BELL_ATTACK_HAMMER_OFFSET = 18.0f;
    private static FDModel CLIENT_MODEL;
    public static EntityDataAccessor<Boolean> PREPARING_FINAL_ATTACK;
    public static EntityDataAccessor<List<PlayerSin>> ACTIVE_SINS;
    public static EntityDataAccessor<Boolean> LASERS_ACTIVE;
    public static EntityDataAccessor<Boolean> OPERATING;
    public static EntityDataAccessor<Boolean> SECOND_PHASE;
    private UUID geburahSpawnerUUID;
    @SerializableField
    public int judgementBirdSpawnTicker = 0;
    @SerializableField
    private GeburahStompingController stompingController;
    protected GeburahWeaponRotationController rotatingWeaponsHandler;
    private GeburahRayController rayController;
    private GeburahWeaponAttackController attackController;
    @SerializableField
    private GeburahScalesController scalesController;
    public AttackChain mainAttackChain;
    private CylinderPlayerPositionsCollector playerPositionsCollector;
    public static final int MAX_SIN_APPEAR_TICK = 20;
    public int sinsAppearTick = 0;
    public int sinsAppearTickO = 0;
    public static final int MAX_LASER_VISUAL_DISAPPEAR_TIME = 5;
    public int laserVisualDisappearTicker = 0;
    public GeburahLaserAttackPreparator laserAttackPreparator;
    public int rayAttackPreparationTicker = -1;
    public int sinPunishmentAttackTicker = -1;
    public int finalAttackPrepareClientTick = 0;
    public int clientOperatingTicks = -1;
    public int clientSecondPhaseTicker = -1;
    @SerializableField
    private Vec3 spawnPosition;
    @SerializableField
    private GeburahBossInitializer bossInitializer;
    @SerializableField
    private GeburahSecondPhaseInitializer secondPhaseBossInitializer;
    @SerializableField
    private int sinnedTimes = 0;
    public int sinnedTicks = 0;
    private BossDespawner<GeburahEntity> bossDespawner;
    private boolean skipNext = false;
    @SerializableField
    public boolean canBeDamaged = false;
    @SerializableField
    private float sinCrystalsThrowTick = 0.0f;
    private boolean sideSwitch = true;
    private int stompingSwitcher = 0;
    @SerializableField
    private boolean rotatingLaserDirection = false;
    @SerializableField
    private boolean clockwise = true;
    private int stompAmount = 2;
    private boolean cannonRotationSwap = false;
    private int hammerDirectionSwap = 1;
    private HashMap<Player, Integer> stayingStillPlayers = new HashMap();

    public GeburahEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.playerPositionsCollector = new CylinderPlayerPositionsCollector(level, 32.0f, 30.0f, player -> true);
        this.bossDespawner = new BossDespawner<GeburahEntity>(this, new AABB(-32.0, -2.0, -32.0, 32.0, 30.0, 32.0), 20, JudgementBirdEntity.class, GeburahCastingCircle.class, GeburahBell.class, GeburahEntity.class, GeburahChainTrapEntity.class, GeburahEarthquake.class);
        this.bossInitializer = new GeburahBossInitializer(this);
        this.secondPhaseBossInitializer = new GeburahSecondPhaseInitializer(this);
        this.rotatingWeaponsHandler = new GeburahWeaponRotationController(this);
        this.rayController = new GeburahRayController(this);
        this.stompingController = new GeburahStompingController(this, 32);
        this.attackController = new GeburahWeaponAttackController(this);
        this.scalesController = new GeburahScalesController(this);
        AttackOptions.ChainAttackOptions simpleRunAroundNoSins = AttackOptions.chainOptionsBuilder().addAttack(SIMPLE_NO_SIN_RUN_AROUND).addAttack(EMPTY_SINS_AND_DELAY).build();
        AttackOptions.ChainAttackOptions noSinSecondPhase = AttackOptions.chainOptionsBuilder().addAttack(NO_SIN_SECOND_PHASE).addAttack(EMPTY_SINS_AND_DELAY).build();
        AttackOptions.ChainAttackOptions noJumpRaysEarthquakesProjectiles = AttackOptions.chainOptionsBuilder().addAttack(NO_JUMP_RAYS_EARTHQUAKES_PROJECTILES).addAttack(EMPTY_SINS_AND_DELAY).build();
        AttackOptions.ChainAttackOptions runClockwiseHammersRayProjectiles = AttackOptions.chainOptionsBuilder().addAttack(RUN_CLOCKWISE_HAMMERS_RAY_PROJECTILES).addAttack(EMPTY_SINS_AND_DELAY).build();
        AttackOptions.ChainAttackOptions limitedButtonsLasersAndEarthquakes = AttackOptions.chainOptionsBuilder().addAttack(LIMITED_BUTTONS_LASERS_AND_EARTHQUAKES).addAttack(EMPTY_SINS_AND_DELAY).build();
        AttackOptions.ChainAttackOptions sinCrystalsLasersAndCannons = AttackOptions.chainOptionsBuilder().addAttack(SIN_CRYSTALS_LASERS_AND_CANNONS).addAttack(EMPTY_SINS_AND_DELAY).build();
        AttackOptions.ChainAttackOptions noKillEntitiesAttack = AttackOptions.chainOptionsBuilder().addAttack(NO_KILL_ENTITIES_ATTACK).addAttack(EMPTY_SINS_AND_DELAY).build();
        AttackOptions.ChainAttackOptions bellAttack = AttackOptions.chainOptionsBuilder().addAttack(BELL_ATTACK).build();
        this.mainAttackChain = new AttackChain(level.f_46441_).registerAttack(EMPTY_SINS_AND_DELAY, this::emptySinsAndDelay).registerAttack(SIMPLE_NO_SIN_RUN_AROUND, this::simpleNoSinRunAroundAttack).registerAttack(NO_SIN_SECOND_PHASE, this::simpleNoSinAttackSecondPhase).registerAttack(RUN_CLOCKWISE_HAMMERS_RAY_PROJECTILES, this::runClockwiseProjectilesHammersRay).registerAttack(LIMITED_BUTTONS_LASERS_AND_EARTHQUAKES, this::limitedButtonsRotatingLasers).registerAttack(NO_JUMP_RAYS_EARTHQUAKES_PROJECTILES, this::noJumpEarthquakesProjectilesRays).registerAttack(SIN_CRYSTALS_LASERS_AND_CANNONS, this::sinCrystalsLasersAndCannons).registerAttack(NO_KILL_ENTITIES_ATTACK, this::noKillEntitiesAttack).registerAttack(BELL_ATTACK, this::bellAttack).attackListener(this::attackListener).addAttack(0, EMPTY_SINS_AND_DELAY).addAttack(1, (AttackOptions)noSinSecondPhase).addAttack(1, (AttackOptions)simpleRunAroundNoSins).addAttack(1, (AttackOptions)noJumpRaysEarthquakesProjectiles).addAttack(1, (AttackOptions)limitedButtonsLasersAndEarthquakes).addAttack(2, (AttackOptions)noKillEntitiesAttack).addAttack(3, (AttackOptions)runClockwiseHammersRayProjectiles).addAttack(3, (AttackOptions)sinCrystalsLasersAndCannons).addAttack(4, (AttackOptions)noKillEntitiesAttack).addAttack(5, (AttackOptions)bellAttack);
        this.laserAttackPreparator = new GeburahLaserAttackPreparator(this);
    }

    public static FDModel getClientModel() {
        if (CLIENT_MODEL == null) {
            CLIENT_MODEL = new FDModel(BossModels.GEBURAH.get());
        }
        return CLIENT_MODEL;
    }

    private FDMusicArea constructMusicArea() {
        return new FDMusicArea(this.m_9236_().m_46472_(), this.m_20182_().m_82520_(0.0, -2.0, 0.0), (FDMusicAreaShape)new FDMusicAreaCylinder(32.0f, 38.0f), this.constructMusicData());
    }

    private FDMusicData constructMusicData() {
        return new FDMusicData(MalkuthEntity.BOSS_MUSIC_UUID, new FDMusicPartData(BossSounds.GEBURAH_THEME.get(), 109.7f).setLooping(true)).fadeInTime(80).inactiveDeleteTime(600);
    }

    public void m_8119_() {
        this.playerPositionsCollector.tick(this.m_20182_().m_82520_(0.0, -0.1, 0.0));
        super.m_8119_();
        if (((Boolean)this.m_20088_().m_135370_(OPERATING)).booleanValue()) {
            ++this.clientOperatingTicks;
        }
        if (((Boolean)this.m_20088_().m_135370_(SECOND_PHASE)).booleanValue()) {
            ++this.clientSecondPhaseTicker;
        }
        this.rayAttackPreparationTicker = Mth.m_14045_((int)(this.rayAttackPreparationTicker - 1), (int)-1, (int)Integer.MAX_VALUE);
        if (this.m_9236_().f_46443_) {
            this.sinnedTicks = Mth.m_14045_((int)(this.sinnedTicks - 1), (int)0, (int)Integer.MAX_VALUE);
            this.tickFinalAttackPreparation();
            this.tickSinPunishmentEffect();
            this.particles();
            this.tickSinsAppearTick();
            this.laserAttackPreparator.tick();
            this.tickLaserVisualDisappearance();
        } else {
            this.tickTeleportHome();
            this.pushAwayCombatants();
            this.bossBar.setPercentage((float)this.sinnedTimes / 10.0f);
            if (!this.m_21224_()) {
                if (this.bossInitializer.isFinished() && (!this.sinnedHalfTimes() || this.secondPhaseBossInitializer.isFinished() && this.sinnedHalfTimes())) {
                    if (!FDMusicAreasHandler.hasMusicArea((UUID)this.m_20148_())) {
                        FDMusicAreasHandler.addArea((UUID)this.m_20148_(), (FDMusicArea)this.constructMusicArea());
                    }
                    this.bossDespawner.tick();
                    this.m_20088_().m_135381_(OPERATING, (Object)true);
                    this.mainAttackChain.tick();
                    this.throwSinCrystals();
                    this.tickTrapEntitiesSpawn();
                    this.tickJudgementBirdSpawn();
                    if (this.secondPhaseBossInitializer.isFinished()) {
                        this.m_20088_().m_135381_(SECOND_PHASE, (Object)true);
                    }
                } else if (!this.bossInitializer.isFinished()) {
                    this.bossInitializer.tick();
                } else if (!this.secondPhaseBossInitializer.isFinished() && this.sinnedHalfTimes()) {
                    this.secondPhaseBossInitializer.tick();
                }
                this.tickClockwiseSin();
                this.tickStayStillSin();
            }
            this.getRayController().tick();
            this.getStompingController().tick();
            this.getWeaponAttackController().tick();
            this.setSinners(this.m_21224_());
        }
        this.getScalesController().tick();
        this.getWeaponRotationController().tick();
    }

    public void triggerRaysShotPreparationEffect() {
        this.rayAttackPreparationTicker = 30;
    }

    private void pushAwayCombatants() {
        List<LivingEntity> entities = BossTargetFinder.getEntitiesInCylinder(LivingEntity.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -1.0, 0.0), 4.0f, 7.0f, living -> {
            if (living instanceof Player) {
                Player player = (Player)living;
                return !player.m_5833_();
            }
            return living != this;
        });
        Vec3 thisPos = this.m_20182_().m_82520_(0.0, -0.5, 0.0);
        for (LivingEntity entity : entities) {
            Vec3 pushVector = entity.m_20182_().m_82546_(thisPos).m_82541_().m_82490_(0.2);
            if (entity instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)entity;
                FDLibCalls.setServerPlayerSpeed((ServerPlayer)serverPlayer, (Vec3)pushVector);
                continue;
            }
            entity.m_20256_(pushVector);
            entity.f_19812_ = true;
        }
    }

    private void setSinners(boolean dying) {
        for (Player entity : this.playerPositionsCollector.getPlayers().stream().filter(BossUtil::isPlayerInSurvival).toList()) {
            if (!dying) {
                if (entity.m_21023_((MobEffect)BossEffects.SINNER.get())) continue;
                MobEffectInstance inst = new MobEffectInstance((MobEffect)BossEffects.SINNER.get(), -1, 0, true, false);
                entity.m_7292_(inst);
                continue;
            }
            entity.m_21195_((MobEffect)BossEffects.SINNER.get());
        }
    }

    private void tickFinalAttackPreparation() {
        if (this.isPreparingFinalAttack()) {
            ++this.finalAttackPrepareClientTick;
            this.prepareHammers(false);
            this.spawnPlatesFlamesParticles();
        } else {
            if (this.finalAttackPrepareClientTick != 0) {
                this.prepareHammers(true);
            }
            this.finalAttackPrepareClientTick = 0;
        }
    }

    private void prepareHammers(boolean end) {
        int hammerCount = 8;
        float angle = (float)Math.PI * 2 / (float)hammerCount;
        for (int i = 0; i < hammerCount; ++i) {
            BallParticleOptions options;
            Vec3 hammerOffset = new Vec3(18.0, 0.0, 0.0).m_82524_((float)i * angle);
            Vec3 pos = this.getCorePosition().m_82549_(hammerOffset);
            if (!end) {
                float ballParticleSize = Mth.m_14036_((float)((float)this.finalAttackPrepareClientTick / 200.0f), (float)0.01f, (float)1.0f) * 4.0f;
                options = BallParticleOptions.builder().size(ballParticleSize).brightness(2).color(0.1f, 0.8f, 1.0f).scalingOptions(0, 0, 5).build();
                this.m_9236_().m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
                if (this.finalAttackPrepareClientTick % 2 != 0) continue;
                int k = i % 2 == 0 ? -1 : 1;
                StripeParticleOptions stripeParticleOptions = StripeParticleOptions.createHorizontalCircling(new FDColor(0.1f, 0.4f, 1.0f, 1.0f), new FDColor(0.1f, 0.8f, 1.0f, 1.0f), new Vec3(0.0, 1.0, 0.0), (float)(this.finalAttackPrepareClientTick / 2) * 1.5707964f * (float)k, 0.3f, 10, 50, 0.0f, 5.0f, 0.5f, 0.75f, i % 2 == 0, true);
                this.m_9236_().m_6493_((ParticleOptions)stripeParticleOptions, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
                continue;
            }
            for (int l = 0; l < 40; ++l) {
                options = BallParticleOptions.builder().size(this.f_19796_.m_188501_() * 0.25f + 0.25f).brightness(2).friction(0.8f).color(0.1f, 0.8f, 1.0f).scalingOptions(0, 0, 100).build();
                Vec3 rnd = new Vec3((double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f)).m_82541_();
                this.m_9236_().m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, rnd.f_82479_, rnd.f_82480_, rnd.f_82481_);
            }
        }
    }

    private void spawnPlatesFlamesParticles() {
        if (this.f_19797_ % 2 != 0) {
            return;
        }
        Matrix4f scalesPlatesPos = this.getModelPartTransformation((Entity)this, "scales_plates", GeburahEntity.getClientModel());
        Vector3f scalesPlates = scalesPlatesPos.transformPosition(new Vector3f());
        Matrix4f newMat = new Matrix4f();
        float rotation = this.getScalesController().getClientDisplacementAngle(0.0f);
        newMat.rotateX((float)Math.toRadians(rotation));
        Vector3f dir1 = newMat.transformDirection(new Vector3f(0.0f, 0.0f, 6.5f));
        Vector3f dir2 = dir1.mul(-1.0f, new Vector3f());
        Vector3f southPos = scalesPlates.add((Vector3fc)dir1, new Vector3f());
        Vector3f northPos = scalesPlates.add((Vector3fc)dir2, new Vector3f());
        BallParticleOptions redFlame = BallParticleOptions.builder().brightness(2).color(1.0f, 0.3f, 0.1f, 1.0f).scalingOptions(0, 0, 20).size(3.0f).build();
        BallParticleOptions blueFlame = BallParticleOptions.builder().brightness(2).color(0.1f, 0.8f, 1.0f, 1.0f).scalingOptions(0, 0, 20).size(3.0f).build();
        this.m_9236_().m_6493_((ParticleOptions)redFlame, true, this.m_20185_() + (double)southPos.x + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, this.m_20186_() + (double)southPos.y - 6.5, this.m_20189_() + (double)southPos.z + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, 0.0, 0.25, 0.0);
        this.m_9236_().m_6493_((ParticleOptions)blueFlame, true, this.m_20185_() + (double)northPos.x + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, this.m_20186_() + (double)northPos.y - 6.5, this.m_20189_() + (double)northPos.z + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, 0.0, 0.25, 0.0);
    }

    private void tickSinPunishmentEffect() {
        this.sinPunishmentAttackTicker = Mth.m_14045_((int)(this.sinPunishmentAttackTicker - 1), (int)-1, (int)40);
    }

    public void tickTrapEntitiesSpawn() {
        if (this.f_19797_ % 20 != 0) {
            return;
        }
        int angles = this.sinnedHalfTimes() ? 8 : 4;
        float angle = (float)Math.PI * 2 / (float)angles;
        for (int i = 0; i < angles; ++i) {
            float currentAngle = angle * (float)i + 0.7853982f + (this.m_9236_().f_46441_.m_188501_() * 2.0f - 1.0f) * (angle / 4.0f);
            Vec3 direction = new Vec3(1.0, 0.0, 0.0).m_82524_(currentAngle);
            Vec3 checkDir = new Vec3(1.0, 0.0, 0.0).m_82524_(angle * (float)i + 0.7853982f);
            List entities = FDTargetFinder.getEntitiesInArc(Entity.class, (Level)this.m_9236_(), (Vec3)this.m_20182_().m_82520_(0.0, -5.0, 0.0), (Vec2)new Vec2((float)checkDir.f_82479_, (float)checkDir.f_82481_), (float)angle, (float)35.0f, (float)32.0f, entity -> entity instanceof GeburahChainTrapEntity || entity instanceof ChainTrapSummonProjectile || entity instanceof GeburahChainTrapCastCircle);
            if (!entities.isEmpty()) continue;
            double radiusToSpawnAt = (double)(16.0f + this.m_9236_().f_46441_.m_188501_() * 3.0f) - 1.5 + 4.0;
            Vec3 offsetPos = direction.m_82490_(radiusToSpawnAt);
            Vec3 targetPos = this.m_20182_().m_82549_(offsetPos);
            Vec3 castCirclePos = this.m_20182_().m_82549_(direction.m_82490_(3.0)).m_82520_(0.0, 5.0, 0.0);
            GeburahChainTrapCastCircle.summon(this.m_9236_(), castCirclePos, direction.m_82520_(0.0, 1.0, 0.0), targetPos);
        }
    }

    public void tickJudgementBirdSpawn() {
        if (this.judgementBirdSpawnTicker != 0) {
            List birds = FDTargetFinder.getEntitiesInCylinder(JudgementBirdEntity.class, (Level)this.m_9236_(), (Vec3)this.m_20182_().m_82520_(0.0, -1.0, 0.0), (float)30.0f, (float)32.0f);
            if (birds.size() < 4) {
                this.judgementBirdSpawnTicker = Mth.m_14045_((int)(this.judgementBirdSpawnTicker - 1), (int)0, (int)Integer.MAX_VALUE);
            }
            return;
        }
        this.judgementBirdSpawnTicker = 600;
        int angles = 4;
        float angle = (float)Math.PI * 2 / (float)angles;
        float halfRadius = 16.0f;
        float roostingBoxCenterHeight = 4.0f;
        double centerDirectionOffset = Math.sqrt(halfRadius * halfRadius + halfRadius * halfRadius);
        for (int i = 0; i < angles; ++i) {
            float currentAngle = angle * (float)i + 0.7853982f;
            Vec3 direction = new Vec3(1.0, 0.0, 0.0).m_82524_(currentAngle);
            Vec3 roostingBoxCenter = this.m_20182_().m_82549_(direction.m_82490_(centerDirectionOffset)).m_82520_(0.0, (double)roostingBoxCenterHeight, 0.0);
            AABB roostingBox = new AABB(roostingBoxCenter.f_82479_ - (double)halfRadius, roostingBoxCenter.f_82480_ - 0.5, roostingBoxCenter.f_82481_ - (double)halfRadius, roostingBoxCenter.f_82479_ + (double)halfRadius, roostingBoxCenter.f_82480_ + 2.0, roostingBoxCenter.f_82481_ + (double)halfRadius);
            AABB findEntityBox = new AABB(roostingBoxCenter.f_82479_ - (double)halfRadius, roostingBoxCenter.f_82480_ - (double)roostingBoxCenterHeight, roostingBoxCenter.f_82481_ - (double)halfRadius, roostingBoxCenter.f_82479_ + (double)halfRadius, roostingBoxCenter.f_82480_ + 5.0, roostingBoxCenter.f_82481_ + (double)halfRadius);
            List entities = this.m_9236_().m_6443_(Entity.class, findEntityBox, entity -> entity instanceof GeburahCastingCircleJudgementBird || entity instanceof JudgementBirdEntity);
            if (!entities.isEmpty()) continue;
            Vec3 castCirclePos = this.m_20182_().m_82549_(direction.m_82490_(3.0)).m_82520_(0.0, (double)(roostingBoxCenterHeight + 1.0f), 0.0);
            Vec3 flyTo = castCirclePos.m_82549_(direction.m_82490_(7.0));
            GeburahCastingCircleJudgementBird.summon(this.m_9236_(), castCirclePos, direction, flyTo, roostingBox);
        }
    }

    public void tickLaserVisualDisappearance() {
        this.laserVisualDisappearTicker = this.isLaserVisualActive() ? 5 : Mth.m_14045_((int)(this.laserVisualDisappearTicker - 1), (int)0, (int)5);
    }

    public float getLaserVisualAlpha(float pticks) {
        if (this.isLaserVisualActive() && this.laserVisualDisappearTicker == 5) {
            return 1.0f;
        }
        return Mth.m_14036_((float)((float)this.laserVisualDisappearTicker - pticks), (float)0.0f, (float)5.0f) / 5.0f;
    }

    public void m_7350_(EntityDataAccessor<?> accessor) {
        super.m_7350_(accessor);
        if (accessor == LASERS_ACTIVE && ((Boolean)this.m_20088_().m_135370_(LASERS_ACTIVE)).booleanValue()) {
            this.laserVisualDisappearTicker = 5;
        }
    }

    public CylinderPlayerPositionsCollector getPlayerPositionsCollector() {
        return this.playerPositionsCollector;
    }

    public Player pickRandomCombatant() {
        CylinderPlayerPositionsCollector positionsCollector = this.getPlayerPositionsCollector();
        ArrayList<Player> players = new ArrayList<Player>(positionsCollector.getPlayers().stream().filter(BossUtil::isPlayerInSurvival).toList());
        if (players.isEmpty()) {
            return null;
        }
        return (Player)players.get(this.f_19796_.m_188503_(players.size()));
    }

    private AttackAction attackListener(String s) {
        if (this.getPlayerPositionsCollector().getPlayers().stream().noneMatch(BossUtil::isPlayerInSurvival)) {
            this.setLaserVisualsState(false);
            return AttackAction.WAIT;
        }
        if (this.sinnedHalfTimes() && Objects.equals(s, SIMPLE_NO_SIN_RUN_AROUND) || !this.sinnedHalfTimes() && Objects.equals(s, NO_SIN_SECOND_PHASE)) {
            this.skipNext = true;
            return AttackAction.SKIP;
        }
        if (this.skipNext) {
            this.skipNext = false;
            return AttackAction.SKIP;
        }
        return AttackAction.PROCEED;
    }

    public void bellRang(GeburahBell bellEntity) {
        this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.GEBURAH_SIN_CHANGE.get(), SoundSource.HOSTILE, 5.0f, 1.25f + this.f_19796_.m_188501_() * 0.1f);
        GeburahScalesController scalesController = this.getScalesController();
        if (bellEntity.isRed()) {
            scalesController.setCurrentDisplacement(scalesController.getCurrentDisplacement() + 1, 10);
        } else {
            scalesController.setCurrentDisplacement(scalesController.getCurrentDisplacement() - 1, 10);
        }
        if (this.scalesController.getCurrentDisplacement() == 0) {
            for (Player player : this.playerPositionsCollector.getPlayers()) {
                FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)player), (Object)new PlaySoundInEarsPacket(BossSounds.ATTACK_DING.get(), 1.0f, 1.0f));
            }
        }
    }

    public void setSinnedTimes(int sinnedTimes) {
        if (!this.m_9236_().f_46443_ && sinnedTimes > this.sinnedTimes) {
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)new GeburahSinnedPacket(this));
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)new DefaultShakePacket(FDShakeData.builder().outTime(40).amplitude(1.0f).build()));
            if (sinnedTimes > 0) {
                this.bossBar.broadcastEvent(0, sinnedTimes - 1);
            }
            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.GEBURAH_SIN.get(), SoundSource.HOSTILE, 3.0f, 0.75f);
        }
        this.sinnedTimes = Mth.m_14045_((int)sinnedTimes, (int)0, (int)10);
        if (this.sinnedTimes == 10) {
            this.m_6074_();
        }
    }

    public int getSinnedTimes() {
        return this.sinnedTimes;
    }

    public boolean bellAttack(AttackInstance instance) {
        int stage = instance.stage;
        int tick = instance.tick;
        this.propagateSins(0, new PlayerSin[0]);
        GeburahScalesController scalesController = this.getScalesController();
        if (stage == 0) {
            int random = (3 + this.m_9236_().f_46441_.m_188503_(3)) * BossUtil.randomPlusMinus();
            if (this.sinnedHalfTimes()) {
                random = BossUtil.randomPlusMinus() * 5;
            }
            scalesController.setCurrentDisplacement(random, 40);
            this.summonBells();
            instance.nextStage();
        } else if (stage == 1) {
            this.setPreparingFinalAttack(true);
            if (tick > 300) {
                instance.nextStage();
            }
        } else if (stage == 2) {
            if (scalesController.getCurrentDisplacement() == 0) {
                this.removeBells();
                this.setPreparingFinalAttack(false);
                return true;
            }
            BossUtil.geburahTriggerSinPunishmentAttack((ServerLevel)this.m_9236_(), this.m_20182_(), 120.0, this);
            instance.nextStage();
        } else if (stage == 3) {
            if (tick == 37) {
                for (Player entity : this.playerPositionsCollector.getPlayers()) {
                    FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)entity), (Object)new ImpactFramesPacket(List.of(new ImpactFrame().setDuration(3), new ImpactFrame().setDuration(1).setInverted(true), new ImpactFrame().setDuration(1), new ImpactFrame().setDuration(1).setInverted(true), new ImpactFrame().setDuration(1), new ImpactFrame().setDuration(1).setInverted(true))));
                }
            } else if (tick == 32) {
                this.setPreparingFinalAttack(false);
            } else if (tick == 40) {
                BossUtil.geburahTriggerSinPunishmentAttackImpactEffect((ServerLevel)this.m_9236_(), this.m_20182_(), 120.0, 18);
                BossUtil.geburahSinPunishmentAttackServerEffect(this.m_9236_(), this.m_20182_(), 18.0f, BossBlocks.JUSTICESTONE_BRICKS.get().m_49966_());
                for (Player entity : this.playerPositionsCollector.getPlayers()) {
                    FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)entity), (Object)new DefaultShakePacket(FDShakeData.builder().amplitude(0.3f).outTime(80).build()));
                    if (BossUtil.isPlayerInSurvival(entity)) {
                        this.removePositiveEffects(entity);
                        float health = entity.m_21223_();
                        if (health > 1.0f) {
                            float percent = 1.0f - BossConfigs.BOSS_CONFIG.get().geburahConfig.ultimateMoveHealthPercentage / 100.0f;
                            entity.m_21153_(health * percent);
                        }
                        entity.m_6469_(this.m_9236_().m_269111_().m_269264_(), 0.1f);
                        PlayerSinsHandler.sin((ServerPlayer)entity, 0, 1.0f, 3);
                    }
                    FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)entity), (Object)new PlaySoundInEarsPacket(BossSounds.CHESED_FINAL_ATTACK_EXPLOSION_BIGGER.get(), 1.0f, 1.5f));
                }
                this.removeBells();
            } else if (tick >= 90) {
                this.setPreparingFinalAttack(false);
                this.removeBells();
                return true;
            }
        }
        return false;
    }

    private void removePositiveEffects(Player player) {
        ArrayList<MobEffect> effectsToRemove = new ArrayList<MobEffect>();
        for (MobEffectInstance effect : player.m_21220_()) {
            MobEffectCategory category = effect.m_19544_().m_19483_();
            if (category != MobEffectCategory.BENEFICIAL && category != MobEffectCategory.NEUTRAL || effect.m_19544_().equals(MobEffects.f_19611_)) continue;
            effectsToRemove.add(effect.m_19544_());
        }
        effectsToRemove.forEach(arg_0 -> ((Player)player).m_21195_(arg_0));
    }

    private void summonBells() {
        this.removeBells();
        int count = 18;
        float angle = (float)Math.PI * 2 / (float)count;
        float randomOffset = this.f_19796_.m_188501_() * (float)Math.PI * 2.0f;
        for (int i = 0; i < count; ++i) {
            Vec3 v = new Vec3(29.0, 0.0, 0.0).m_82524_(randomOffset + angle * (float)i);
            Vec3 bellPos = this.m_20182_().m_82549_(v).m_82520_(0.0, 1.0, 0.0);
            GeburahBell.summon(this, bellPos, this.m_20182_(), i % 2 == 0);
        }
    }

    private void removeBells() {
        for (GeburahBell bell : FDTargetFinder.getEntitiesInCylinder(GeburahBell.class, (Level)this.m_9236_(), (Vec3)this.m_20182_().m_82520_(0.0, -1.0, 0.0), (float)30.0f, (float)64.0f)) {
            bell.m_142467_(Entity.RemovalReason.DISCARDED);
        }
    }

    public boolean emptySinsAndDelay(AttackInstance attackInstance) {
        this.propagateSins(20, new PlayerSin[0]);
        if (attackInstance.tick == 0) {
            int currentDisplacement = this.getScalesController().getCurrentDisplacement();
            if (currentDisplacement >= 0) {
                this.getScalesController().setCurrentDisplacement(-5, 60);
            } else {
                this.getScalesController().setCurrentDisplacement(5, 60);
            }
        }
        if (attackInstance.tick == 30) {
            for (ServerPlayer serverPlayer : FDTargetFinder.getEntitiesInCylinder(ServerPlayer.class, (Level)this.m_9236_(), (Vec3)this.m_20182_().m_82520_(0.0, -0.1, 0.0), (float)40.0f, (float)32.0f)) {
                FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), (Object)new StartGeburahDistortionEffectPacket(this));
            }
            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.GEBURAH_SIN_CHANGE.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
        }
        return attackInstance.tick > 50;
    }

    public boolean noKillEntitiesAttack(AttackInstance inst) {
        if (!this.sinnedHalfTimes()) {
            this.propagateSins(0, GeburahSins.KILL_ENTITY_SIN.get());
        } else {
            this.propagateSins(0, GeburahSins.KILL_ENTITY_SIN.get(), GeburahSins.CRYSTAL_OF_SIN.get());
        }
        int localStagesCount = 3;
        int stage = inst.stage;
        int tick = inst.tick;
        if (stage == 0 && tick == 0) {
            this.canBeDamaged = true;
        }
        if (stage == 0 && tick < 40) {
            return false;
        }
        int localStage = stage % localStagesCount;
        GeburahWeaponAttackController attackController = this.getWeaponAttackController();
        int maxStages = 8 * localStagesCount;
        if (this.getPlayerPositionsCollector().getPlayers().stream().noneMatch(BossUtil::isPlayerInSurvival)) {
            return false;
        }
        if (stage < maxStages) {
            if (localStage == 0) {
                this.getStompingController().stompFullCircle(30, true, 1.0f, BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.earthquakeDamage));
                attackController.setCurrentAttack(new GeburahRoundAndRoundLaserAttack(this, this.sideSwitch), false);
                this.sideSwitch = !this.sideSwitch;
                inst.nextStage();
            } else if (localStage == 1) {
                if (!attackController.isAttacking()) {
                    inst.nextStage();
                }
            } else if (localStage == 2 && tick >= 30) {
                inst.nextStage();
            }
        }
        if (stage >= maxStages) {
            this.canBeDamaged = false;
            return true;
        }
        return false;
    }

    public void throwSinCrystals() {
        List activeSins = (List)this.m_20088_().m_135370_(ACTIVE_SINS);
        if (activeSins.contains(GeburahSins.CRYSTAL_OF_SIN.get())) {
            ArrayList<Player> players;
            if (this.sinCrystalsThrowTick % 250.0f == 0.0f && !(players = new ArrayList<Player>(this.playerPositionsCollector.getPlayers().stream().filter(BossUtil::isPlayerInSurvival).toList())).isEmpty()) {
                for (int i = 0; i < 4; ++i) {
                    Vec3 direction = new Vec3(1.0, 0.0, 0.0).m_82524_(0.7853982f + 1.5707964f * (float)i);
                    Vec3 pos = this.m_20182_().m_82520_(0.0, 4.0, 0.0).m_82549_(direction.m_82490_(5.0));
                    Player player = players.get(this.f_19796_.m_188503_(players.size()));
                    GeburahSinCrystalCastCircle.summon(this.m_9236_(), pos, direction.m_82520_(0.0, 1.0, 0.0), player);
                }
            }
            this.sinCrystalsThrowTick += 1.0f;
        } else {
            this.sinCrystalsThrowTick = 0.0f;
        }
    }

    public boolean sinCrystalsLasersAndCannons(AttackInstance inst) {
        this.propagateSins(0, GeburahSins.CRYSTAL_OF_SIN.get());
        int stage = inst.stage;
        int tick = inst.tick;
        if (stage == 0) {
            if (tick < 40) {
                return false;
            }
            inst.nextStage();
        } else {
            --stage;
        }
        int localStagesAmount = 6;
        int localStage = stage % localStagesAmount;
        int stagesAmount = 10 * localStagesAmount;
        if (stage < stagesAmount) {
            if (localStage < 3) {
                this.simpleCannonAttacks(inst.tick, 20, 10, 175);
                if (tick > 11) {
                    inst.nextStage();
                }
            } else {
                GeburahWeaponAttackController controller = this.getWeaponAttackController();
                if (localStage == 3) {
                    if (this.getPlayerPositionsCollector().getPlayers().stream().anyMatch(BossUtil::isPlayerInSurvival)) {
                        this.sideSwitch = !this.sideSwitch;
                        controller.setCurrentAttack(new GeburahRoundAndRoundLaserAttack(this, this.sideSwitch), true);
                        inst.nextStage();
                    }
                } else if (localStage == 4) {
                    if (controller.getCurrentAttack() == null) {
                        inst.nextStage();
                        if (this.sinnedHalfTimes()) {
                            this.getStompingController().stompFullCircle(10, true, 1.0f, BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.earthquakeDamage));
                        }
                    }
                } else if (tick > 2) {
                    inst.nextStage();
                }
            }
        }
        return stage >= stagesAmount && tick > 40;
    }

    public boolean noJumpEarthquakesProjectilesRays(AttackInstance inst) {
        if (!this.sinnedHalfTimes()) {
            this.propagateSins(30, GeburahSins.JUMPING_SIN.get());
        } else {
            this.propagateSins(30, GeburahSins.JUMPING_SIN.get(), GeburahSins.STAY_STILL_SIN.get());
        }
        int tick = inst.tick;
        if (tick < 40) {
            return false;
        }
        this.simpleCannonAttacks(tick -= 40, 10, 15);
        this.randomStompsAndRays(tick, 70, this.f_19796_.m_188503_(2) + 4);
        return tick > 400;
    }

    public void randomStompsAndRays(int currentTick, int frequency, int count) {
        if (currentTick % frequency == 0) {
            float rayShotRadius = 5.0f;
            GeburahStompingController stompingController = this.getStompingController();
            List<Vec3> playerPositions = this.playerPositionsCollector.getCurrentPlayerPositions();
            if (playerPositions.isEmpty()) {
                return;
            }
            ArrayList<GeburahStompingController.StompInstance> stompInstances = new ArrayList<GeburahStompingController.StompInstance>();
            float angle = (float)Math.PI / (float)count;
            Vec3 pos = playerPositions.get(this.f_19796_.m_188503_(playerPositions.size()));
            Vec3 direction = pos.m_82546_(this.m_20182_()).m_82542_(1.0, 0.0, 1.0);
            if (this.stompingSwitcher % 2 == 0) {
                direction = direction.m_82524_(angle);
            }
            double distToPlayer = direction.m_82553_();
            direction = direction.m_82541_();
            ++this.stompingSwitcher;
            ArrayList<Vec3> rayPositions = new ArrayList<Vec3>();
            float startRadius = 6.0f;
            float radiusForRays = 32.0f - startRadius - rayShotRadius * 2.0f;
            float startRadiusForRays = startRadius + rayShotRadius;
            for (int i = 0; i < count; ++i) {
                float rad;
                Vec3 stompDirection = direction.m_82524_((float)i * angle * 2.0f);
                stompInstances.add(new GeburahStompingController.StompInstance(new Vec2((float)stompDirection.f_82479_, (float)stompDirection.f_82481_), angle / 2.0f));
                float randomRadius = startRadiusForRays + this.f_19796_.m_188501_() * radiusForRays;
                if (i == 0 || i == count - 1) {
                    randomRadius = Math.min(Math.max(startRadiusForRays, (float)distToPlayer), 32.0f - rayShotRadius);
                }
                Vec3 rayPos = this.rayPosAtRadius(i, direction, randomRadius, angle, rayShotRadius);
                rayPositions.add(rayPos);
                if (!this.sinnedHalfTimes()) continue;
                if (randomRadius > startRadiusForRays + radiusForRays / 2.0f) {
                    rad = startRadius + (randomRadius - startRadiusForRays) / 2.0f;
                } else {
                    float distToEdge = 32.0f - randomRadius;
                    rad = randomRadius + distToEdge / 2.0f;
                }
                Vec3 rayPos2 = this.rayPosAtRadius(i, direction, rad, angle, rayShotRadius);
                rayPositions.add(rayPos2);
            }
            this.getRayController().shoot(frequency - 20, rayShotRadius, true, rayPositions);
            stompingController.stomp(frequency - 30, true, 1.0f, BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.earthquakeDamage), stompInstances);
        }
    }

    private Vec3 rayPosAtRadius(int i, Vec3 baseDirection, float randomRadius, float angle, float rayShotRadius) {
        float arcLength = (float)Math.PI * 2 * randomRadius * (angle / (float)Math.PI / 2.0f);
        float angleRandomCoefficient = arcLength / (rayShotRadius * 2.0f) / 2.0f;
        float additionAngle = Math.max(0.0f, angle / 2.0f * (angleRandomCoefficient - 1.0f));
        Vec3 rayDirection = baseDirection.m_82524_((float)i * angle * 2.0f + angle + additionAngle * (float)BossUtil.randomPlusMinus());
        Vec3 rayPos = this.m_20182_().m_82549_(rayDirection.m_82490_((double)randomRadius));
        return rayPos;
    }

    public boolean limitedButtonsRotatingLasers(AttackInstance attackInstance) {
        this.propagateSins(60, GeburahSins.PRESSED_TOO_MANY_BUTTONS_SIN.get());
        int tick = attackInstance.tick;
        if (tick < 40) {
            BossUtil.geburahChargeConstantLaserParticles((ServerLevel)this.m_9236_(), this.m_20182_(), 120.0, this);
            return false;
        }
        int attackDuration = 150;
        if ((tick -= 40) < attackDuration) {
            if (tick == 0) {
                this.rotatingLaserDirection = this.f_19796_.m_188499_();
            }
            int md = 1;
            if (this.sinnedHalfTimes()) {
                int n = md = this.rotatingLaserDirection ? -1 : 1;
                if (tick % 75 == 0) {
                    this.rotatingLaserDirection = !this.rotatingLaserDirection;
                    for (Player player : this.playerPositionsCollector.getPlayers()) {
                        FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)player), (Object)new PlaySoundInEarsPacket(BossSounds.ATTACK_DING.get(), 1.0f, 1.0f));
                    }
                }
            }
            this.constantRotatingLaser((float)md * 0.5f);
            if ((tick + 20) % 30 == 0) {
                this.getStompingController().stompFullCircle(10, true, 1.0f, BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.earthquakeDamage));
            }
        } else {
            this.stopLaser();
        }
        if (tick > attackDuration) {
            this.rotatingLaserDirection = !this.rotatingLaserDirection;
            this.stopLaser();
            return true;
        }
        return false;
    }

    private void constantRotatingLaser(float rotationSpeed) {
        GeburahWeaponRotationController rotationController = this.getWeaponRotationController();
        GeburahWeaponAttackController attackController = this.getWeaponAttackController();
        rotationController.startConstantRotation(rotationSpeed);
        if (!(attackController.getCurrentAttack() instanceof GeburahLasersAttack)) {
            attackController.setCurrentAttack(new GeburahLasersAttack(this), true);
        }
    }

    private void stopLaser() {
        GeburahWeaponRotationController rotationController = this.getWeaponRotationController();
        GeburahWeaponAttackController attackController = this.getWeaponAttackController();
        rotationController.stopRotation();
        if (attackController.getCurrentAttack() instanceof GeburahLasersAttack) {
            attackController.stopAttack();
        }
    }

    public boolean runClockwiseProjectilesHammersRay(AttackInstance attackInstance) {
        if (!this.sinnedHalfTimes()) {
            if (this.clockwise) {
                this.propagateSins(20, GeburahSins.MOVE_CLOCKWISE_SIN.get());
            } else {
                this.propagateSins(20, GeburahSins.MOVE_COUNTERCLOCKWISE_SIN.get());
            }
        } else if (this.clockwise) {
            this.propagateSins(40, GeburahSins.MOVE_CLOCKWISE_SIN.get(), GeburahSins.JUMPING_SIN.get());
        } else {
            this.propagateSins(40, GeburahSins.MOVE_COUNTERCLOCKWISE_SIN.get(), GeburahSins.JUMPING_SIN.get());
        }
        int tick = attackInstance.tick;
        if (tick < 40) {
            return false;
        }
        int attackDuration = 400;
        if ((tick -= 40) <= attackDuration) {
            this.attackPlayersWithRay(tick, 20, 10, 80);
            this.simpleCannonAttacks(tick, 5, 20, 80);
            this.smackWithHammersAround(tick + 1, 40);
        }
        if (tick > attackDuration + 40) {
            this.clockwise = !this.clockwise;
            return true;
        }
        return false;
    }

    public boolean simpleNoSinRunAroundAttack(AttackInstance attackInstance) {
        this.propagateSins(20, new PlayerSin[0]);
        int tick = attackInstance.tick;
        if (tick < 40) {
            return false;
        }
        this.attackPlayersWithRay(tick -= 40, 20, 10, 60);
        this.simpleCannonAttacks(tick, 5, 20);
        return tick > 300;
    }

    public boolean simpleNoSinAttackSecondPhase(AttackInstance attackInstance) {
        this.propagateSins(40, GeburahSins.STAY_STILL_SIN.get());
        int stage = attackInstance.stage;
        int tick = attackInstance.tick;
        if (stage == 0) {
            if (tick < 40) {
                return false;
            }
            attackInstance.nextStage();
        } else {
            int attackStagesCount = 4;
            int maxStages = 6 * attackStagesCount;
            int lstage = --stage % attackStagesCount;
            if (stage < maxStages) {
                if (lstage == 0) {
                    this.simpleCannonAttacks(tick, 0, 15, 120);
                    float earthquakeDamage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.earthquakeDamage);
                    if (tick % 20 == 0 && tick <= 20 * this.stompAmount) {
                        this.getStompingController().stompFullCircle(15, tick == 0, 1.0f, earthquakeDamage);
                    } else if (tick > this.stompAmount * 20 + 5) {
                        attackInstance.nextStage();
                    }
                } else if (lstage == 1) {
                    GeburahWeaponAttack currentAttack = this.getWeaponAttackController().getCurrentAttack();
                    if (currentAttack instanceof GeburahRoundAndRoundLaserAttack) {
                        GeburahRoundAndRoundLaserAttack attack = (GeburahRoundAndRoundLaserAttack)currentAttack;
                    } else if (!this.getPlayerPositionsCollector().getPlayers().stream().filter(BossUtil::isPlayerInSurvival).toList().isEmpty()) {
                        this.getWeaponAttackController().setCurrentAttack(new GeburahRoundAndRoundLaserAttack(this, this.cannonRotationSwap), true);
                        this.cannonRotationSwap = !this.cannonRotationSwap;
                        attackInstance.nextStage();
                    }
                } else if (lstage == 2) {
                    GeburahWeaponAttackController controller = this.getWeaponAttackController();
                    if (!controller.isAttacking()) {
                        attackInstance.nextStage();
                    }
                } else if (lstage == 3 && tick > 0) {
                    attackInstance.nextStage();
                    int st = this.stompAmount;
                    while (this.stompAmount == st) {
                        this.stompAmount = 1 + this.f_19796_.m_188503_(3);
                    }
                }
            } else {
                return tick > 20;
            }
        }
        return false;
    }

    private void simpleCannonAttacks(int currentTick, int timeBetweenShots, int frequency) {
        this.simpleCannonAttacks(currentTick, timeBetweenShots, frequency, 80, -1.0f);
    }

    private void simpleCannonAttacks(int currentTick, int timeBetweenShots, int frequency, int projectileFlyTime) {
        this.simpleCannonAttacks(currentTick, timeBetweenShots, frequency, projectileFlyTime, -1.0f);
    }

    private void simpleCannonAttacks(int currentTick, int timeBetweenShots, int frequency, int projectileFlyTime, float fixedAngle) {
        GeburahWeaponAttackController attackController = this.getWeaponAttackController();
        GeburahWeaponRotationController weaponRotationController = this.getWeaponRotationController();
        int startShootTime = frequency + timeBetweenShots;
        int localtick = currentTick % startShootTime;
        if (localtick == 0) {
            float rotation;
            if (fixedAngle == -1.0f) {
                rotation = (this.f_19796_.m_188501_() * 100.0f + 40.0f) * (float)(this.cannonRotationSwap ? -1 : 1);
                this.cannonRotationSwap = !this.cannonRotationSwap;
            } else {
                rotation = fixedAngle * (float)(this.cannonRotationSwap ? -1 : 1);
            }
            weaponRotationController.rotateWeaponsBy(rotation, frequency);
        } else if (localtick == frequency - ((Animation)BossAnims.GEBURAH_FIRE_CANNONS.get()).getAnimTime() + 5) {
            attackController.setCurrentAttack(new GeburahAttackFireDefaultProjectiles(this, 32.0f, projectileFlyTime, 1.0f), false);
        }
    }

    private void attackPlayersWithRay(int currentTick, int rayChargeDuration, int attackRedirectTick, int frequency) {
        List<Vec3> list;
        if (rayChargeDuration >= frequency) {
            throw new RuntimeException("Cannot have ray charge duration higher than frequency");
        }
        GeburahRayController rayController = this.getRayController();
        if (currentTick % frequency == 0) {
            list = this.playerPositionsCollector.getCurrentPlayerPositions();
            rayController.shoot(rayChargeDuration, 2.0f, false, list);
        }
        if (rayController.getCurrentShotCharge() == attackRedirectTick) {
            list = this.playerPositionsCollector.getCurrentPlayerPositions();
            rayController.shoot(rayChargeDuration, 2.0f, false, list.stream().map(v -> v.m_82520_(0.0, 0.5, 0.0)).toList());
        }
    }

    private void smackWithHammersAround(int currentTick, int frequency) {
        if (currentTick % frequency == 0) {
            float centerOffset = 18.0f;
            float offsetFromCenter = 6.0f;
            int localDirectionSwap = this.hammerDirectionSwap == 0 ? -1 : 1;
            for (Vec3 dir : new HorizontalCircleRandomDirections(this.f_19796_, 8, 0.0f)) {
                Vec3 pos = this.m_20182_().m_82549_(dir.m_82490_((double)centerOffset));
                Vec3 hammerOffset = dir.m_82490_((double)localDirectionSwap).m_82490_((double)offsetFromCenter);
                JusticeHammerAttack.summon(this.m_9236_(), pos.m_82549_(hammerOffset), hammerOffset.m_82548_());
                localDirectionSwap = -localDirectionSwap;
            }
            this.hammerDirectionSwap = (this.hammerDirectionSwap + 1) % 2;
        }
    }

    private void tickSinsAppearTick() {
        List list = (List)this.m_20088_().m_135370_(ACTIVE_SINS);
        this.sinsAppearTickO = this.sinsAppearTick;
        this.sinsAppearTick = list.isEmpty() ? Mth.m_14045_((int)(this.sinsAppearTick - 1), (int)0, (int)20) : Mth.m_14045_((int)(this.sinsAppearTick + 1), (int)0, (int)20);
    }

    public void tickStayStillSin() {
        this.stayingStillPlayers.entrySet().removeIf(pair -> ((Player)pair.getKey()).m_21224_() || ((Player)pair.getKey()).m_20270_((Entity)this) > 50.0f);
        for (Player player : this.playerPositionsCollector.getPlayers().stream().filter(BossUtil::isPlayerInSurvival).toList()) {
            PlayerSins playerSins = PlayerSins.getPlayerSins(player);
            if (!playerSins.hasSinActive(GeburahSins.STAY_STILL_SIN.get()) || playerSins.isGainingSinsOnCooldown()) continue;
            Pair<Vec3, Vec3> positions = this.playerPositionsCollector.getOldAndCurrentPlayerPosition(player);
            Vec3 oldPos = (Vec3)positions.first;
            Vec3 newPos = (Vec3)positions.second;
            if (oldPos.m_82554_(newPos) < 0.05) {
                int stayingStillTicks = this.stayingStillPlayers.computeIfAbsent(player, p -> 0);
                if (stayingStillTicks > 3) {
                    PlayerSinsHandler.sin((ServerPlayer)player, 20);
                    continue;
                }
                this.stayingStillPlayers.put(player, stayingStillTicks + 1);
                continue;
            }
            this.stayingStillPlayers.remove(player);
        }
    }

    public void tickClockwiseSin() {
        for (Player player : this.playerPositionsCollector.getPlayers().stream().filter(BossUtil::isPlayerInSurvival).toList()) {
            PlayerSins playerSins = PlayerSins.getPlayerSins(player);
            if (!playerSins.hasSinActive(GeburahSins.MOVE_CLOCKWISE_SIN.get()) && !playerSins.hasSinActive(GeburahSins.MOVE_COUNTERCLOCKWISE_SIN.get()) || playerSins.isGainingSinsOnCooldown()) continue;
            Pair<Vec3, Vec3> pair = this.playerPositionsCollector.getOldAndCurrentPlayerPosition(player);
            Vec3 oldPos = ((Vec3)pair.first).m_82542_(1.0, 0.0, 1.0);
            Vec3 newPos = ((Vec3)pair.second).m_82542_(1.0, 0.0, 1.0);
            Vec3 between = newPos.m_82546_(oldPos);
            if (between.m_82553_() < 0.2) continue;
            Vec3 betweenThisAndThat = newPos.m_82546_(this.m_20182_().m_82542_(1.0, 0.0, 1.0));
            Vec3 rotated = betweenThisAndThat.m_82524_(1.5707964f);
            double dot = between.m_82541_().m_82526_(rotated.m_82541_());
            if (playerSins.hasSinActive(GeburahSins.MOVE_CLOCKWISE_SIN.get())) {
                if (!(dot < -0.5)) continue;
                PlayerSinsHandler.sin((ServerPlayer)player, 40);
                continue;
            }
            if (!(dot > 0.5)) continue;
            PlayerSinsHandler.sin((ServerPlayer)player, 40);
        }
    }

    public List<Pair<Vec3, Vec3>> getCannonsPositionAndDirection() {
        return this.getCannonsPositionAndDirection(1.0f);
    }

    public List<Pair<Vec3, Vec3>> getCannonsPositionAndDirection(float pticks) {
        float offsetFromCenter = 6.5f;
        float verticalOffset = 1.5f;
        float currentRotation = this.getWeaponRotationController().getLerpedRotation(pticks);
        float angle = 45.0f;
        ArrayList<Pair<Vec3, Vec3>> pairs = new ArrayList<Pair<Vec3, Vec3>>();
        for (int i = 0; i < 8; ++i) {
            Vec3 dir = new Vec3(1.0, 0.0, 0.0).m_82524_((float)Math.toRadians(currentRotation + angle * (float)i));
            Vec3 pos = this.m_20182_().m_82520_(dir.f_82479_ * (double)offsetFromCenter, (double)verticalOffset, dir.f_82481_ * (double)offsetFromCenter);
            pairs.add((Pair<Vec3, Vec3>)new Pair((Object)pos, (Object)dir));
        }
        return pairs;
    }

    public void setLaserVisualsState(boolean state) {
        this.m_20088_().m_135381_(LASERS_ACTIVE, (Object)state);
    }

    public boolean isLaserVisualActive() {
        return (Boolean)this.m_20088_().m_135370_(LASERS_ACTIVE);
    }

    public Vec3 getCorePosition() {
        return this.m_20182_().m_82520_(0.0, 21.5, 0.0);
    }

    public GeburahRayController getRayController() {
        return this.rayController;
    }

    public GeburahStompingController getStompingController() {
        return this.stompingController;
    }

    public GeburahWeaponAttackController getWeaponAttackController() {
        return this.attackController;
    }

    public void propagateSins(int cooldown, PlayerSin ... sins) {
        ArrayList<PlayerSin> list = new ArrayList<PlayerSin>(Arrays.stream(sins).toList());
        this.m_20088_().m_135381_(ACTIVE_SINS, list);
        this.trySetAllPlayersSins(list, cooldown, true);
    }

    private void particles() {
        this.coreParticles();
        this.laserParticles();
    }

    public boolean m_6469_(DamageSource src, float p_21017_) {
        return (src.m_276093_(DamageTypes.f_286979_) || src.m_276093_(DamageTypes.f_268724_)) && super.m_6469_(src, p_21017_);
    }

    public boolean m_20068_() {
        return true;
    }

    public void m_5997_(double p_20286_, double p_20287_, double p_20288_) {
    }

    public void m_7334_(Entity p_21294_) {
    }

    protected void m_6138_() {
    }

    private boolean sinnedHalfTimes() {
        return this.sinnedTimes >= 5;
    }

    private void laserParticles() {
        if (!this.isLaserVisualActive()) {
            return;
        }
        float r = 0.3f;
        float g = 0.7f;
        float b = 1.0f;
        for (Pair<Vec3, Vec3> pair : this.getCannonsPositionAndDirection()) {
            Vec3 pos = (Vec3)pair.first;
            Vec3 dir = (Vec3)pair.second;
            Matrix4f mat = new Matrix4f();
            FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)dir);
            Vec3 ppos = pos.m_82549_(dir.m_82490_(0.25));
            for (Vec3 dir2 : new HorizontalCircleRandomDirections(this.f_19796_, 6, 1.0f)) {
                Vec3 baseSpeed = dir2.m_82490_((double)(0.25f - this.f_19796_.m_188501_() * 0.05f)).m_82520_(0.0, (double)-0.1f - (double)this.f_19796_.m_188501_() * 0.05, 0.0);
                Vec3 speed = BossUtil.matTransformDirectionVec3(mat, baseSpeed);
                Vec3 spawnOffset = BossUtil.matTransformDirectionVec3(mat, dir2.m_82490_((double)(0.1f + this.f_19796_.m_188501_() * 0.1f)));
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().brightness(2).color(r, g, b).size(0.2f + this.f_19796_.m_188501_() * 0.1f).friction(0.7f).scalingOptions(0, 0, 5).build();
                this.m_9236_().m_7106_((ParticleOptions)ballParticleOptions, ppos.f_82479_ + spawnOffset.f_82479_, ppos.f_82480_ + spawnOffset.f_82480_, ppos.f_82481_ + spawnOffset.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
        }
    }

    private void coreParticles() {
        if (((Boolean)this.m_20088_().m_135370_(OPERATING)).booleanValue()) {
            if (this.m_9236_().m_46467_() % 3L == 0L) {
                BallParticleOptions ballParticle = BallParticleOptions.builder().color(1.0f, 0.8f, 0.3f).scalingOptions(0, 0, 20).brightness(1).size(2.0f).build();
                Vec3 corePos = this.getCorePosition();
                this.m_9236_().m_6493_((ParticleOptions)ballParticle, true, corePos.f_82479_, corePos.f_82480_, corePos.f_82481_, 0.0, 0.0, 0.0);
            }
            if (this.clientOperatingTicks == 0 || this.clientSecondPhaseTicker == 0) {
                Vec3 corePos = this.getCorePosition();
                float minSize = 0.25f;
                float maxSize = 1.5f;
                for (int i = 0; i < 200; ++i) {
                    float size = minSize + this.f_19796_.m_188501_() * (maxSize - minSize);
                    float sizeP = (size - minSize) / (maxSize - minSize);
                    BallParticleOptions ballParticle = BallParticleOptions.builder().color(1.0f, 0.8f, 0.3f).scalingOptions(0, 0, 40).brightness(1).friction(0.8f).size(size).build();
                    Vec3 v = new Vec3(1.0, 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_()).m_82490_((double)((1.0f - sizeP) * 3.0f + 0.2f));
                    this.m_9236_().m_6493_((ParticleOptions)ballParticle, true, corePos.f_82479_, corePos.f_82480_, corePos.f_82481_, v.f_82479_, v.f_82480_, v.f_82481_);
                }
            }
        }
    }

    public void trySetAllPlayersSins(List<PlayerSin> sins, int sinCooldown, boolean sendUpdate) {
        AABB sinBox = this.constructSinBox();
        for (Player player : this.playerPositionsCollector.getPlayers().stream().filter(BossUtil::isPlayerInSurvival).toList()) {
            PlayerSins playerSins = PlayerSins.getPlayerSins(player);
            if (playerSins.hasExactlyThisSins(sins)) continue;
            List<ActivePlayerSinInstance> instances = sins.stream().map(sin -> new ActivePlayerSinInstance((PlayerSin)sin, new WorldBox((ResourceKey<Level>)this.m_9236_().m_46472_(), sinBox), 0)).toList();
            playerSins.setSinGainCooldown(sinCooldown);
            playerSins.setActiveSins(instances);
            if (!sendUpdate) continue;
            PlayerSins.setPlayerSins(player, playerSins);
        }
    }

    public void removeAllArenaTrash() {
        for (JudgementBirdEntity judgementBirdEntity : this.getArenaEntities(JudgementBirdEntity.class)) {
            judgementBirdEntity.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        for (GeburahCastingCircle geburahCastingCircle : this.getArenaEntities(GeburahCastingCircle.class)) {
            geburahCastingCircle.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        for (GeburahEarthquake geburahEarthquake : this.getArenaEntities(GeburahEarthquake.class)) {
            geburahEarthquake.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        for (GeburahSinCrystal geburahSinCrystal : this.getArenaEntities(GeburahSinCrystal.class)) {
            geburahSinCrystal.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        for (JudgementBallProjectile judgementBallProjectile : this.getArenaEntities(JudgementBallProjectile.class)) {
            judgementBallProjectile.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        for (GeburahChainTrapEntity geburahChainTrapEntity : this.getArenaEntities(GeburahChainTrapEntity.class)) {
            geburahChainTrapEntity.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    public void removeCrystalsFromPlayerInventories() {
        for (ServerPlayer entity : this.getArenaEntities(ServerPlayer.class)) {
            Inventory inventory = entity.m_150109_();
            for (int i = 0; i < inventory.m_6643_(); ++i) {
                ItemStack item = inventory.m_8020_(i);
                if (!item.m_41720_().equals(BossItems.GEBURAH_EXPLOSIVE_CRYSTAL.get())) continue;
                inventory.m_6836_(i, ItemStack.f_41583_);
            }
        }
    }

    protected void m_6153_() {
        if (!this.m_9236_().f_46443_) {
            FDMusicAreasHandler.removeArea((MinecraftServer)((ServerLevel)this.m_9236_()).m_7654_(), (UUID)this.m_20148_(), (int)100);
            if (this.f_20919_ == 0) {
                CutsceneData data = this.deathCutscene();
                for (ServerPlayer player : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -1.0, 0.0), 30.0f, 52.0f)) {
                    FDLibCalls.startCutsceneForPlayer((ServerPlayer)player, (CutsceneData)data);
                }
            }
            this.propagateSins(0, new PlayerSin[0]);
            if (this.f_20919_ < 5) {
                this.removeCrystalsFromPlayerInventories();
                this.removeAllArenaTrash();
            }
            if (this.getSpawner() != null) {
                this.getSpawner().setActive(true);
            }
            if (this.f_20919_ == 132) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.GEBURAH_DEATH.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
            }
        }
        ++this.f_20919_;
        if (this.f_20919_ >= 150) {
            if (!this.m_213877_() && !this.m_9236_().f_46443_) {
                for (Player player : this.getPlayerPositionsCollector().getPlayers()) {
                    BossCriteriaTriggers.BOSS_KILLED.trigger((ServerPlayer)player, (Entity)this);
                }
                this.m_142687_(Entity.RemovalReason.KILLED);
            }
        } else if (this.f_20919_ == 145 && this.m_9236_().f_46443_) {
            this.spawnDeathParticles();
        }
    }

    private void spawnDeathParticles() {
        for (int i = 0; i < 1500; ++i) {
            SimpleParticleType particleOptions;
            float dist = FDEasings.easeOut((float)this.f_19796_.m_188501_()) * 20.0f;
            Vec3 v = new Vec3((double)dist, 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f);
            float height = this.f_19796_.m_188501_() * 40.0f;
            Vec3 ppos = this.m_20182_().m_82549_(v).m_82520_(0.0, (double)height, 0.0);
            Vec3 speed = new Vec3((double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f)).m_82541_().m_82490_((double)0.035f);
            if (this.m_9236_().f_46441_.m_188499_()) {
                particleOptions = BallParticleOptions.builder().color(1.0f, 0.8f, 0.3f, 0.75f).scalingOptions(0, 0, 200).brightness(2 + this.f_19796_.m_188503_(2)).build();
                this.m_9236_().m_6493_((ParticleOptions)particleOptions, true, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
                continue;
            }
            particleOptions = ParticleTypes.f_123810_;
            BossClientPackets.summonParticle((ParticleOptions)particleOptions, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_, 200);
        }
    }

    private CutsceneData deathCutscene() {
        CutsceneData cutsceneData = CutsceneData.create().addScreenEffect(0, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 0, 40).timeEasing(EasingType.EASE_IN_OUT).time(120);
        CameraPos lastPos = null;
        Vec3 lastLook = null;
        Vec3 origin = this.m_20182_().m_82520_(0.0, 12.0, 0.0);
        int c = 24;
        float angle = (float)Math.PI * 2 / (float)c;
        for (Vec3 dir : new HorizontalCircleRandomDirections(this.f_19796_, c, 0.0f)) {
            CameraPos cameraPos;
            Vec3 offs = dir.m_82548_().m_82520_(0.0, 0.2, 0.0).m_82524_(angle);
            Vec3 camPos = origin.m_82549_(offs.m_82490_(25.0));
            lastLook = offs.m_82548_();
            lastPos = cameraPos = new CameraPos(camPos, offs.m_82548_());
            cutsceneData.addCameraPos(cameraPos);
        }
        CutsceneData cutsceneData1 = CutsceneData.create().addScreenEffect(14, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(1.0f, 1.0f, 1.0f, 1.0f), 0, 40, 60).addScreenEffect(125, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 20, 10, 20).time(150).addCameraPos(lastPos);
        cutsceneData.nextCutscene(cutsceneData1);
        return cutsceneData;
    }

    protected <T extends Entity> List<T> getArenaEntities(Class<T> clazz) {
        return BossTargetFinder.getEntitiesInCylinder(clazz, this.m_9236_(), this.m_20182_().m_82520_(0.0, -2.0, 0.0), 30.0f, 42.0f);
    }

    private AABB constructSinBox() {
        return new AABB(-32.0, -2.0, -32.0, 32.0, 30.0, 32.0).m_82383_(this.m_20182_());
    }

    public GeburahWeaponRotationController getWeaponRotationController() {
        return this.rotatingWeaponsHandler;
    }

    public void m_6457_(ServerPlayer player) {
        super.m_6457_(player);
        this.getWeaponRotationController().onStartSeeingGeburah(player);
        this.getScalesController().syncToPlayer(player);
        this.bossBar.addPlayer(player);
        FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), (Object)new GeburahSyncRayVisualsPacket(this, this.isLaserVisualActive()));
    }

    public void m_6452_(ServerPlayer player) {
        super.m_6452_(player);
        this.bossBar.removePlayer(player);
    }

    protected void m_8097_() {
        super.m_8097_();
        this.m_20088_().m_135372_(LASERS_ACTIVE, (Object)false);
        this.m_20088_().m_135372_(ACTIVE_SINS, new ArrayList());
        this.m_20088_().m_135372_(PREPARING_FINAL_ATTACK, (Object)false);
        this.m_20088_().m_135372_(OPERATING, (Object)false);
        this.m_20088_().m_135372_(SECOND_PHASE, (Object)false);
    }

    public boolean isPreparingFinalAttack() {
        return (Boolean)this.m_20088_().m_135370_(PREPARING_FINAL_ATTACK);
    }

    public void setPreparingFinalAttack(boolean preparing) {
        this.m_20088_().m_135381_(PREPARING_FINAL_ATTACK, (Object)preparing);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
        CompoundTag mainAttackChain = new CompoundTag();
        this.mainAttackChain.save(mainAttackChain);
        tag.m_128365_("mainAttackChain", (Tag)mainAttackChain);
        if (this.geburahSpawnerUUID != null) {
            tag.m_128362_("spawnerId", this.geburahSpawnerUUID);
        }
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        this.mainAttackChain.load(tag.m_128469_("mainAttackChain"));
        if (tag.m_128441_("spawnerId")) {
            this.geburahSpawnerUUID = tag.m_128342_("spawnerId");
        }
    }

    public void tickTeleportHome() {
        if (this.spawnPosition != null && this.m_20182_().m_82554_(this.spawnPosition) > 2.5) {
            this.m_6021_(this.spawnPosition.f_82479_, this.spawnPosition.f_82480_, this.spawnPosition.f_82481_);
        }
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_5825_() {
        return true;
    }

    public boolean m_7301_(MobEffectInstance p_21197_) {
        return false;
    }

    public void m_20256_(Vec3 p_20257_) {
    }

    protected boolean m_7341_(Entity p_20339_) {
        return false;
    }

    public boolean m_7998_(Entity p_21396_, boolean p_21397_) {
        return false;
    }

    public GeburahScalesController getScalesController() {
        return this.scalesController;
    }

    public void m_20334_(double p_20335_, double p_20336_, double p_20337_) {
    }

    @Override
    public void setSpawnedBy(BossSpawnerEntity bossSpawnerEntity) {
        this.geburahSpawnerUUID = bossSpawnerEntity.m_20148_();
    }

    @Override
    public void setSpawnPosition(Vec3 spawnPosition) {
        this.spawnPosition = spawnPosition;
    }

    @Override
    public BossSpawnerEntity getSpawner() {
        Entity entity;
        if (this.geburahSpawnerUUID != null && (entity = ((ServerLevel)this.m_9236_()).m_8791_(this.geburahSpawnerUUID)) instanceof BossSpawnerEntity) {
            BossSpawnerEntity bossSpawner = (BossSpawnerEntity)entity;
            return bossSpawner;
        }
        return null;
    }

    @Nullable
    public LivingEntity m_5448_() {
        List<Player> players = this.getPlayerPositionsCollector().getPlayers().stream().filter(BossUtil::isPlayerInSurvival).toList();
        if (!players.isEmpty()) {
            return (LivingEntity)players.get(0);
        }
        return null;
    }

    @Override
    public boolean onFDDespawn() {
        BossSpawnerEntity spawner = this.getSpawner();
        FDMusicAreasHandler.removeArea((MinecraftServer)((ServerLevel)this.m_9236_()).m_7654_(), (UUID)this.m_20148_(), (int)100);
        if (spawner != null) {
            spawner.setActive(true);
            return true;
        }
        return false;
    }

    static {
        PREPARING_FINAL_ATTACK = SynchedEntityData.m_135353_(GeburahEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
        ACTIVE_SINS = SynchedEntityData.m_135353_(GeburahEntity.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.SINS.get()));
        LASERS_ACTIVE = SynchedEntityData.m_135353_(GeburahEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
        OPERATING = SynchedEntityData.m_135353_(GeburahEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
        SECOND_PHASE = SynchedEntityData.m_135353_(GeburahEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    }

    @Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {
        @SubscribeEvent
        public static void deathEvent(LivingDeathEvent event) {
            ServerPlayer serverPlayer;
            CompoundTag respawnData;
            LivingEntity livingEntity = event.getEntity();
            if (livingEntity instanceof ServerPlayer && (respawnData = GeburahRespiteBlock.getRespawnData(serverPlayer = (ServerPlayer)livingEntity)) != null) {
                List spawners = FDTargetFinder.getEntitiesInCylinder(GeburahBossSpawner.class, (Level)serverPlayer.m_9236_(), (Vec3)serverPlayer.m_20182_().m_82520_(0.0, -10.0, 0.0), (float)40.0f, (float)52.0f);
                if (!spawners.isEmpty()) {
                    respawnData.m_128379_("diedToGeburah", true);
                } else {
                    respawnData.m_128379_("diedToGeburah", false);
                }
            }
        }

        @SubscribeEvent
        public static void onClone(PlayerEvent.Clone event) {
            if (!event.getOriginal().m_9236_().f_46443_) {
                Player original = event.getOriginal();
                Player clone = event.getEntity();
                CompoundTag data = GeburahRespiteBlock.getRespawnData((ServerPlayer)original);
                if (data != null) {
                    GeburahRespiteBlock.putRespawnData((ServerPlayer)clone, data);
                }
            }
        }

        @SubscribeEvent
        public static void handleSinnerEffect(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }
            Player player = event.player;
            if (!player.m_9236_().f_46443_ && player.m_21023_((MobEffect)BossEffects.SINNER.get())) {
                if (BossUtil.isPlayerInSurvival(player)) {
                    float cylinderHeight = 3.0f;
                    List<GeburahEntity> entities = BossTargetFinder.getEntitiesInCylinder(GeburahEntity.class, player.m_9236_(), player.m_20182_().m_82520_(0.0, (double)(-cylinderHeight), 0.0), cylinderHeight + 1.0f, 32.0f);
                    if (entities.isEmpty() && player.f_19797_ % 40 == 0) {
                        PlayerSinsHandler.sin((ServerPlayer)player, 0);
                    }
                } else {
                    player.m_21195_((MobEffect)BossEffects.SINNER.get());
                }
            }
        }

        @SubscribeEvent
        public static void killEvent(LivingDeathEvent event) {
            LivingEntity entity = event.getEntity();
            DamageSource source = event.getSource();
            Level level = entity.m_9236_();
            if (!(entity instanceof ServerPlayer)) {
                ServerPlayer serverPlayer;
                PlayerSins playerSins;
                Entity entity2 = source.m_7639_();
                if (entity2 instanceof ServerPlayer && (playerSins = PlayerSins.getPlayerSins((Player)(serverPlayer = (ServerPlayer)entity2))).hasSinActive(GeburahSins.KILL_ENTITY_SIN.get())) {
                    PlayerSinsHandler.sin(serverPlayer, 100);
                }
            } else {
                ServerPlayer player = (ServerPlayer)entity;
                Inventory inventory = player.m_150109_();
                for (int i = 0; i < inventory.m_6643_(); ++i) {
                    ItemStack item = inventory.m_8020_(i);
                    if (!item.m_150930_(BossItems.GEBURAH_EXPLOSIVE_CRYSTAL.get())) continue;
                    inventory.m_6836_(i, ItemStack.f_41583_);
                }
            }
        }
    }
}

