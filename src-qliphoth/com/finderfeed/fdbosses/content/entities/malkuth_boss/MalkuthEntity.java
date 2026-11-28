/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.init.FDRenderTypes
 *  com.finderfeed.fdlib.init.FDScreenEffects
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.network.lib_packets.PlaySoundInEarsPacket
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDMob
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer$Mode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.IHasHead
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.ModelSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.attachments.BaseModelAttachmentData
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.attachments.ModelAttachmentData
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.attachments.instances.fdmodel.FDModelAttachmentData
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModelInfo
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
 *  com.finderfeed.fdlib.systems.music.data.FDMusicData
 *  com.finderfeed.fdlib.systems.music.data.FDMusicPartData
 *  com.finderfeed.fdlib.systems.music.music_areas.FDMusicArea
 *  com.finderfeed.fdlib.systems.music.music_areas.FDMusicAreasHandler
 *  com.finderfeed.fdlib.systems.music.music_areas.shapes.FDMusicAreaShape
 *  com.finderfeed.fdlib.systems.render_types.FDRenderType
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData
 *  com.finderfeed.fdlib.systems.shake.DefaultShakePacket
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$PlayerTickEvent
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.event.entity.living.LivingUseTotemEvent
 *  net.minecraftforge.event.entity.living.MobEffectEvent$Expired
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.network.NetworkDirection
 *  net.minecraftforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 *  org.joml.AxisAngle4d
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.particles.arc_preparation_particle.ArcAttackPreparationParticleOptions;
import com.finderfeed.fdbosses.client.particles.square_preparation_particle.RectanglePreparationParticleOptions;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.BossDespawner;
import com.finderfeed.fdbosses.content.entities.FDDespawnable;
import com.finderfeed.fdbosses.content.entities.IEffectImmune;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerContextAssignable;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.BossMusicAreaCylinder;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBuddy;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossInitializer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthDamageSource;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthSecondPhaseInitializer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthWeaknessHandler;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boss_spawner.MalkuthBossSpawner;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boulder.MalkuthBoulderEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonProjectile;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_chain.MalkuthChainEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_crush.MalkuthCrushAttack;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake.MalkuthEarthquake;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_fireball.MalkuthFireball;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_floor.MalkuthFloorEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_giant_sword.MalkuthGiantSwordSlash;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_platform.MalkuthPlatform;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal.MalkuthRepairCrystal;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal.MalkuthRepairEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_slash.MalkuthSlashProjectile;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.packets.MalkuthChargeSwordPacket;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossBars;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossCriteriaTriggers;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.init.FDRenderTypes;
import com.finderfeed.fdlib.init.FDScreenEffects;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.network.lib_packets.PlaySoundInEarsPacket;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDMob;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.IHasHead;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.ModelSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.attachments.BaseModelAttachmentData;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.attachments.ModelAttachmentData;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.attachments.instances.fdmodel.FDModelAttachmentData;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.bedrock.models.FDModelInfo;
import com.finderfeed.fdlib.systems.cutscenes.CameraPos;
import com.finderfeed.fdlib.systems.cutscenes.CutsceneData;
import com.finderfeed.fdlib.systems.cutscenes.EasingType;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackAction;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackChain;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackInstance;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackOptions;
import com.finderfeed.fdlib.systems.hud.bossbars.FDServerBossBar;
import com.finderfeed.fdlib.systems.impact_frames.ImpactFrame;
import com.finderfeed.fdlib.systems.music.data.FDMusicData;
import com.finderfeed.fdlib.systems.music.data.FDMusicPartData;
import com.finderfeed.fdlib.systems.music.music_areas.FDMusicArea;
import com.finderfeed.fdlib.systems.music.music_areas.FDMusicAreasHandler;
import com.finderfeed.fdlib.systems.music.music_areas.shapes.FDMusicAreaShape;
import com.finderfeed.fdlib.systems.render_types.FDRenderType;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData;
import com.finderfeed.fdlib.systems.shake.DefaultShakePacket;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class MalkuthEntity
extends FDMob
implements IHasHead<MalkuthEntity>,
MalkuthBossBuddy,
AutoSerializable,
BossSpawnerContextAssignable,
IEffectImmune,
FDDespawnable {
    public static final UUID BOSS_MUSIC_UUID = UUID.fromString("5c6cd8c0-7e3e-44a3-9c2e-2459a61377f3");
    public static float CHANCE_TO_CHOOSE_WEAK_TYPE = 0.75f;
    public static final Vec3 FIRE_PLAYER_CANNON_OFFSET = new Vec3(-6.5, -1.0, -25.0);
    public static final Vec3 ICE_PLAYER_CANNON_OFFSET = new Vec3(6.5, -1.0, -25.0);
    public UUID bossSpawnerUUID;
    public static final Vec3 WALL_OFFSET = new Vec3(0.0, 13.0, 27.85);
    public static final String MAIN_LAYER = "MAIN";
    public static final float ENRAGE_HEIGHT = 11.0f;
    public static final float ENRAGE_RADIUS = 29.0f;
    public static final String SLASH_ATTACK = "slash";
    public static final String JUMP_CRUSH = "jump_crush";
    public static final String PULL_AND_PUNCH = "pull_and_punch";
    public static final String JUMP_ON_WALL_COMMAND_CANNONS = "jump_on_wall_command_cannons";
    public static final String CAROUSEL_SLASHES = "carousel_slashes";
    public static final String JUMP_BACK_ON_SPAWN = "jump_back_on_spawn";
    public static final String JUMP_BACK_ON_SPAWN_WITH_CRUSH = "jump_back_on_spawn_with_crush";
    public static final String GIANT_SWORDS_ULTIMATE = "giant_swords_attack";
    public static final String SUMMON_AND_THROW_SIDE_ROCKS = "summon_and_throw_side_rocks";
    public static final String ATTACH_SWORDS = "attach_swords";
    public static final String DEATTACH_SWORDS = "deattach_swords";
    public static final String DELAY_20 = "nothing_20_ticks";
    public static final String DELAY_10 = "nothing_10_ticks";
    public static final String DELAY_5 = "nothing_5_ticks";
    public static final String SUMMON_EARTHQUAKE = "summon_earthquake";
    public static final String SUMMON_EARTHQUAKE_LOWER = "summon_earthquake_lower";
    public static final String PLATFORMS_N_FIREBALLS = "platforms_n_fireballs";
    private static FDModel SERVER_MODEL;
    private static FDModel CLIENT_MODEL;
    public static final UUID FIRE_SWORD_UUID;
    public static final UUID FIRE_SWORD_EMISSIVE_UUID;
    public static final UUID ICE_SWORD_UUID;
    public static final UUID ICE_SWORD_EMISSIVE_UUID;
    public static final ResourceLocation MALKUTH_SWORD_SOLID;
    public static final ResourceLocation MALKUTH_ICE_SWORD;
    public static final ResourceLocation MALKUTH_FIRE_SWORD;
    private FDServerBossBar bossbar = new FDServerBossBar(BossBars.MALKUTH_BOSS_BAR, (Entity)this);
    private HeadControllerContainer<MalkuthEntity> headControllerContainer;
    private AttackChain attackChain;
    @SerializableField
    public int pullAndPunchCooldown = 0;
    @SerializableField
    protected Vec3 spawnPosition;
    @SerializableField
    private MalkuthBossInitializer malkuthBossInitializer;
    @SerializableField
    private MalkuthSecondPhaseInitializer malkuthSecondPhaseInitializer;
    @SerializableField
    protected boolean lookAtTarget = true;
    @SerializableField
    private int hits = 10;
    @SerializableField
    private boolean allowedToBeDamaged = false;
    private int maxHits = 10;
    private boolean dropLoot = true;
    private Vec3 oldTargetPos;
    private BossDespawner<MalkuthEntity> bossDespawner;
    private static final Vec3[] PLATFORM_SPAWN_OFFSETS;
    private static final int[][] PLATFORM_ATTACK_PATTERN;
    private Vec3[] FLY_TO_OFFSETS = new Vec3[]{new Vec3(0.0, 7.0, 15.0), new Vec3(-15.0, 7.0, 12.0), new Vec3(15.0, 7.0, 12.0)};
    @SerializableField
    private int currentlyFlyingTo = -1;
    @SerializableField
    private ProjectileMovementPath startSummonPlatformsPath;
    private MalkuthAttackType earthquakeToSummon = MalkuthAttackType.FIRE;
    private MalkuthAttackType sideRocksCurrentType = MalkuthAttackType.FIRE;
    private ProjectileMovementPath jumpBackOnSpawnPath;
    private MalkuthAttackType jumpBackOnSpawnCrushType = MalkuthAttackType.FIRE;
    private MalkuthAttackType slashAttackType = MalkuthAttackType.FIRE;
    private ProjectileMovementPath jumpCrushAttackMovementPath;
    private List<Player> pullAndPunchTargets = new ArrayList<Player>();
    private ProjectileMovementPath jumpOnWallPath = null;
    private MalkuthAttackType currentStartCarouselSlash = MalkuthAttackType.FIRE;
    @SerializableField
    private MalkuthAttackType giantSwordUltimateStartAttackType = MalkuthAttackType.FIRE;

    public MalkuthEntity(EntityType<? extends FDMob> type, Level level) {
        super(type, level);
        if (level.f_46443_) {
            if (CLIENT_MODEL == null) {
                CLIENT_MODEL = new FDModel(BossModels.MALKUTH.get());
            }
        } else if (SERVER_MODEL == null) {
            SERVER_MODEL = new FDModel(BossModels.MALKUTH.get());
        }
        this.maxHits = BossConfigs.BOSS_CONFIG.get().malkuthConfig.malkuthMaxHits;
        this.hits = BossConfigs.BOSS_CONFIG.get().malkuthConfig.malkuthMaxHits;
        this.malkuthBossInitializer = new MalkuthBossInitializer(this);
        this.malkuthSecondPhaseInitializer = new MalkuthSecondPhaseInitializer(this);
        this.headControllerContainer = new HeadControllerContainer((Mob)this).addHeadController(CLIENT_MODEL, "head", true);
        this.f_21365_ = this.headControllerContainer;
        AttackOptions.ChainAttackOptions cannons = AttackOptions.chainOptionsBuilder().addAttack(JUMP_ON_WALL_COMMAND_CANNONS).addAttack(DELAY_5).addAttack(JUMP_BACK_ON_SPAWN).build();
        AttackOptions.ChainAttackOptions cannonsNoJumpBack = AttackOptions.chainOptionsBuilder().addAttack(JUMP_ON_WALL_COMMAND_CANNONS).addAttack(DELAY_5).build();
        AttackOptions.ChainAttackOptions jumpCrushEarthquake = AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(JUMP_BACK_ON_SPAWN).build();
        AttackOptions.ChainAttackOptions jumpCrushEarthquakeNoJumpBack = AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).build();
        AttackOptions.ChainAttackOptions jumpCrushEarthquakeJumpCrushEarthquake = AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(JUMP_CRUSH).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(JUMP_CRUSH).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(JUMP_BACK_ON_SPAWN).build();
        AttackOptions.ChainAttackOptions jumpCrushEarthquakeJumpCrushEarthquakeNoJumpBack = AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(JUMP_CRUSH).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(JUMP_CRUSH).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).build();
        AttackOptions.ChainAttackOptions jumpCrushSlashNEarthquake = AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(JUMP_CRUSH).addAttack(DELAY_5).addAttack(JUMP_BACK_ON_SPAWN).build();
        AttackOptions.ChainAttackOptions jumpCrushSlashNEarthquakeNoJumpBack = AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(JUMP_CRUSH).addAttack(DELAY_5).build();
        AttackOptions.ChainAttackOptions jumpCrushMoreSlashNEarthquake = AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(JUMP_BACK_ON_SPAWN).build();
        AttackOptions.ChainAttackOptions jumpCrushMoreSlashNEarthquakeNoJumpBack = AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).addAttack(DELAY_5).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(SLASH_ATTACK).addAttack(DELAY_5).addAttack(SUMMON_EARTHQUAKE).build();
        AttackOptions randomJumpCrush = AttackOptions.builder().addAttack((AttackOptions)jumpCrushEarthquake).addAttack((AttackOptions)jumpCrushEarthquakeJumpCrushEarthquake).addAttack((AttackOptions)jumpCrushSlashNEarthquake).addAttack((AttackOptions)jumpCrushMoreSlashNEarthquake).build();
        AttackOptions randomJumpCrushNoJumpBack = AttackOptions.builder().addAttack((AttackOptions)jumpCrushEarthquakeNoJumpBack).addAttack((AttackOptions)jumpCrushEarthquakeJumpCrushEarthquakeNoJumpBack).addAttack((AttackOptions)jumpCrushSlashNEarthquakeNoJumpBack).addAttack((AttackOptions)jumpCrushMoreSlashNEarthquakeNoJumpBack).build();
        AttackOptions.ChainAttackOptions slashOptions = AttackOptions.chainOptionsBuilder().addAttack(SLASH_ATTACK).addAttack(SLASH_ATTACK).addAttack(SLASH_ATTACK).addAttack(SLASH_ATTACK).addAttack(SLASH_ATTACK).build();
        AttackOptions.ChainAttackOptions carouselEarthquakes = AttackOptions.chainOptionsBuilder().addAttack(CAROUSEL_SLASHES).addAttack(SUMMON_EARTHQUAKE_LOWER).addAttack(CAROUSEL_SLASHES).addAttack(SUMMON_EARTHQUAKE_LOWER).addAttack(CAROUSEL_SLASHES).build();
        AttackOptions.ChainAttackOptions boulders = AttackOptions.chainOptionsBuilder().addAttack(DELAY_10).addAttack(DEATTACH_SWORDS).addAttack(SUMMON_AND_THROW_SIDE_ROCKS).addAttack(SUMMON_AND_THROW_SIDE_ROCKS).addAttack(SUMMON_AND_THROW_SIDE_ROCKS).addAttack(ATTACH_SWORDS).addAttack(DELAY_20).build();
        AttackOptions.ChainAttackOptions bouldersNCarousel = AttackOptions.chainOptionsBuilder().addAttack(SUMMON_AND_THROW_SIDE_ROCKS).addAttack(CAROUSEL_SLASHES).addAttack(SUMMON_AND_THROW_SIDE_ROCKS).addAttack(CAROUSEL_SLASHES).build();
        int id = 0;
        this.attackChain = new AttackChain(this.m_217043_()).registerAttack(DELAY_5, v -> this.doNothingNTicks(v, 5)).registerAttack(DELAY_10, v -> this.doNothingNTicks(v, 10)).registerAttack(DELAY_20, v -> this.doNothingNTicks(v, 20)).registerAttack(DEATTACH_SWORDS, v -> this.attachSwordsAttack(v, false)).registerAttack(ATTACH_SWORDS, v -> this.attachSwordsAttack(v, true)).registerAttack(SLASH_ATTACK, this::aerialSlashAttack).registerAttack(PULL_AND_PUNCH, this::pullAndPunch).registerAttack(JUMP_CRUSH, this::jumpCrushAttack).registerAttack(CAROUSEL_SLASHES, this::carouselSlashesAttack).registerAttack(JUMP_BACK_ON_SPAWN, v -> this.jumpBackOnSpawn(v, this.isBelowHalfHP())).registerAttack(JUMP_ON_WALL_COMMAND_CANNONS, this::jumpAndCommandCannons).registerAttack(GIANT_SWORDS_ULTIMATE, this::giantSwordUltimate).registerAttack(SUMMON_AND_THROW_SIDE_ROCKS, this::summonAndThrowSideRocks).registerAttack(SUMMON_EARTHQUAKE, v -> this.summonEarthquake(v, 0)).registerAttack(SUMMON_EARTHQUAKE_LOWER, v -> this.summonEarthquake(v, -1)).registerAttack(PLATFORMS_N_FIREBALLS, this::fireballsNPlatforms).addAlwaysTryCastAttack(this::checkCanPunch, PULL_AND_PUNCH).addAttack(id++, (AttackOptions)slashOptions).addAttack(id++, randomJumpCrushNoJumpBack).addAttack(id++, (AttackOptions)cannonsNoJumpBack).addAttack(id++, randomJumpCrush).addAttack(id++, (AttackOptions)carouselEarthquakes).addAttack(id++, randomJumpCrushNoJumpBack).addAttack(id++, (AttackOptions)cannons).addAttack(id++, (AttackOptions)slashOptions).addAttack(id++, GIANT_SWORDS_ULTIMATE).addAttack(id++, randomJumpCrushNoJumpBack).addAttack(id++, (AttackOptions)cannons).addAttack(id++, (AttackOptions)slashOptions).addAttack(id++, randomJumpCrush).addAttack(id++, (AttackOptions)boulders).addAttack(id++, (AttackOptions)AttackOptions.chainOptionsBuilder().addAttack(JUMP_CRUSH).addAttack(DELAY_10).addAttack(PLATFORMS_N_FIREBALLS).build()).attackListener(this::attackListener);
        this.bossDespawner = new BossDespawner<MalkuthEntity>(this, new AABB(-100.0, -50.0, -100.0, 100.0, 50.0, 100.0), 50, MalkuthRepairCrystal.class, MalkuthCannonProjectile.class, MalkuthBoulderEntity.class, MalkuthGiantSwordSlash.class, MalkuthCrushAttack.class, MalkuthRepairEntity.class, MalkuthPlatform.class, MalkuthFloorEntity.class);
    }

    private boolean doNothingNTicks(AttackInstance instance, int tick) {
        return instance.tick >= tick;
    }

    public static FDModel getClientModel() {
        if (CLIENT_MODEL == null) {
            CLIENT_MODEL = new FDModel(BossModels.MALKUTH.get());
        }
        return CLIENT_MODEL;
    }

    public void m_8119_() {
        if (!this.m_9236_().f_46443_ && this.f_19803_) {
            this.attachSwords();
        }
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            this.headControllerContainer.clientTick();
        } else {
            this.pullAndPunchCooldown = Mth.m_14045_((int)(this.pullAndPunchCooldown - 1), (int)0, (int)Integer.MAX_VALUE);
            this.preventEnteringLavaField();
            this.bossbar.setPercentage((float)this.hits / (float)this.getMaxHits());
            AnimationSystem animationSystem = this.getAnimationSystem();
            if (this.malkuthBossInitializer.isFinished() && !this.m_21224_()) {
                if (animationSystem.getTicker(MAIN_LAYER) == null) {
                    animationSystem.startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build());
                }
                if (!FDMusicAreasHandler.hasMusicArea((UUID)this.m_20148_())) {
                    FDMusicAreasHandler.addArea((UUID)this.m_20148_(), (FDMusicArea)this.constructMusicArea());
                }
            }
            if (!this.m_21224_()) {
                if (this.malkuthBossInitializer.isFinished() && (!this.isBelowHalfHP() || this.malkuthSecondPhaseInitializer.isFinished())) {
                    this.attackChain.tick();
                    if (this.m_5448_() != null) {
                        LivingEntity target = this.m_5448_();
                        this.checkTarget(target);
                        if (this.lookAtTarget && this.m_5448_() != null) {
                            this.m_21563_().m_148051_((Entity)this.m_5448_());
                        }
                    } else {
                        this.changeTarget();
                        if (this.m_5448_() == null) {
                            this.m_21563_().m_24964_(this.m_20182_().m_82549_(this.m_20156_().m_82542_(100.0, 0.0, 100.0)));
                        }
                    }
                    this.bossDespawner.tick();
                } else if (!this.malkuthBossInitializer.isFinished()) {
                    this.malkuthBossInitializer.tick();
                } else if (!this.malkuthSecondPhaseInitializer.isFinished()) {
                    this.malkuthSecondPhaseInitializer.tick();
                }
            }
            if (this.m_5448_() != null) {
                this.oldTargetPos = this.m_5448_().m_20182_();
            }
            this.m_146922_(this.f_20883_);
        }
    }

    private FDMusicArea constructMusicArea() {
        return new FDMusicArea(this.m_9236_().m_46472_(), this.spawnPosition.m_82520_(0.0, -2.0, 0.0), (FDMusicAreaShape)new BossMusicAreaCylinder(29.0f, 19.0f), this.constructMusicData());
    }

    private FDMusicData constructMusicData() {
        return new FDMusicData(BOSS_MUSIC_UUID, new FDMusicPartData(BossSounds.MALKUTH_THEME_INTRO.get(), 14.75f)).addMusicPart(new FDMusicPartData(BossSounds.MALKUTH_THEME_MAIN.get(), 103.375f).setLooping(true)).fadeInTime(80).inactiveDeleteTime(600);
    }

    private void preventEnteringLavaField() {
        for (LivingEntity entity : BossTargetFinder.getEntitiesInArc(LivingEntity.class, this.m_9236_(), this.spawnPosition.m_82520_(0.0, -1.5, -0.7), new Vec2(0.0f, 1.0f), (float)Math.PI, 13.0f, 29.0f)) {
            if (entity instanceof MalkuthBossBuddy) continue;
            Vec3 speed = new Vec3(0.0, (double)0.025f, -1.0);
            if (entity instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)entity;
                FDLibCalls.setServerPlayerSpeed((ServerPlayer)serverPlayer, (Vec3)speed);
            } else {
                entity.m_20256_(speed);
            }
            Vector3f col = MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.FIRE, this.f_19796_);
            BallParticleOptions ballParticleOptions = BallParticleOptions.builder().color(col.x, col.y, col.z).scalingOptions(0, 0, 20).brightness(2).friction(0.7f).build();
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ballParticleOptions, entity.m_20185_(), entity.m_20186_() + (double)(entity.m_20206_() / 2.0f), entity.m_20189_(), 25, (double)(entity.m_20205_() / 2.0f), (double)(entity.m_20206_() / 2.0f), (double)(entity.m_20205_() / 2.0f), 0.5);
        }
    }

    public void hurtBoss(int amount) {
        if (this.allowedToBeDamaged) {
            this.hits = FDMathUtil.clamp((int)(this.hits - amount), (int)0, (int)this.maxHits);
            if (this.hits == 0) {
                this.m_6074_();
            }
        }
    }

    public void m_6667_(DamageSource src) {
        this.dropLoot = false;
        for (MalkuthRepairCrystal repairCrystal : BossTargetFinder.getEntitiesInCylinder(MalkuthRepairCrystal.class, this.m_9236_(), this.spawnPosition.m_82520_(0.0, -3.0, 0.0), 30.0f, 40.0f)) {
            repairCrystal.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_FLOAT).setToNullTransitionTime(0).build());
        this.getAnimationSystem().stopAnimation(MAIN_LAYER);
        Vec3 teleportPos = this.spawnPosition.m_82549_(WALL_OFFSET);
        this.m_6021_(teleportPos.f_82479_, teleportPos.f_82480_, teleportPos.f_82481_);
        this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.ANIMATION);
        this.m_7618_(EntityAnchorArgument.Anchor.EYES, this.m_20182_().m_82520_(0.0, 0.0, -100.0));
        super.m_6667_(src);
        CutsceneData data = this.deathCutscene();
        for (ServerPlayer player : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, this.m_9236_(), this.spawnPosition.m_82492_(0.0, 2.0, 0.0), 40.0f, 40.0f)) {
            FDLibCalls.startCutsceneForPlayer((ServerPlayer)player, (CutsceneData)data);
        }
    }

    private CutsceneData deathCutscene() {
        Vec3 base = this.spawnPosition;
        CameraPos last = new CameraPos(base.m_82520_(0.0, 16.119, 23.01), new Vec3(0.0, -0.27, 0.963));
        CutsceneData data1 = CutsceneData.create().addScreenEffect(0, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 6, 30).timeEasing(EasingType.EASE_OUT).time(80).addCameraPos(new CameraPos(base.m_82520_(0.0, 16.119, 19.863), new Vec3(0.0, -0.182, 0.983))).addCameraPos(last);
        CutsceneData data2 = CutsceneData.create().addScreenEffect(219, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 30, 30).addCameraPos(last).time(220);
        data1.nextCutscene(data2);
        return data1;
    }

    protected void m_6153_() {
        ++this.f_20919_;
        int animStartTime = 5;
        int animEndtime = animStartTime + ((Animation)BossAnims.MALKUTH_DEATH.get()).getAnimTime();
        if (!this.m_9236_().f_46443_) {
            FDMusicAreasHandler.removeArea((MinecraftServer)((ServerLevel)this.m_9236_()).m_7654_(), (UUID)this.m_20148_(), (int)40);
            if (this.f_20919_ == 1) {
                for (Player combatant : this.getCombatants(true)) {
                    if (combatant.m_5833_()) continue;
                    BossCriteriaTriggers.BOSS_KILLED.trigger((ServerPlayer)combatant, (Entity)this);
                }
            } else if (this.f_20919_ == animStartTime) {
                this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_DEATH).build());
            } else if (this.f_20919_ == animEndtime - 38) {
                DefaultShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (Vec3)this.spawnPosition, (double)40.0, (FDShakeData)FDShakeData.builder().amplitude(0.3f).outTime(30).build());
                for (ServerPlayer player : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, this.m_9236_(), this.spawnPosition.m_82492_(0.0, 2.0, 0.0), 40.0f, 40.0f)) {
                    FDPacketHandler.INSTANCE.sendTo((Object)new PlaySoundInEarsPacket(BossSounds.MALKUTH_HIT.get(), 0.75f, 1.0f), player.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
                    FDPacketHandler.INSTANCE.sendTo((Object)new PlaySoundInEarsPacket(BossSounds.MALKUTH_VOLCANO_ERRUPTION.get(), 1.75f, 1.0f), player.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
                }
            } else if (this.f_20919_ == animEndtime - 37) {
                BossUtil.malkuthSwordsInsertParticles((ServerLevel)this.m_9236_(), this.spawnPosition, 100.0, this.m_19879_());
            } else if (this.f_20919_ == animEndtime + 15) {
                this.dropLoot = true;
                Vector3f col1 = MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.FIRE, this.f_19796_);
                Vector3f col2 = MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.ICE, this.f_19796_);
                BallParticleOptions options = BallParticleOptions.builder().color(col1.x, col1.y, col1.z).scalingOptions(0, 0, 25).size(0.25f).brightness(2).friction(0.8f).build();
                BallParticleOptions options2 = BallParticleOptions.builder().color(col2.x, col2.y, col2.z).scalingOptions(0, 0, 25).size(0.25f).brightness(2).friction(0.8f).build();
                for (ServerPlayer player : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, this.m_9236_(), this.spawnPosition.m_82492_(0.0, 2.0, 0.0), 40.0f, 40.0f)) {
                    player.f_8906_.m_9829_((Packet)new ClientboundLevelParticlesPacket((ParticleOptions)options, true, this.m_20185_(), this.m_20186_(), this.m_20189_(), 1.0f, 1.0f, 1.0f, 0.75f, 400));
                    player.f_8906_.m_9829_((Packet)new ClientboundLevelParticlesPacket((ParticleOptions)options2, true, this.m_20185_(), this.m_20186_(), this.m_20189_(), 1.0f, 1.0f, 1.0f, 0.75f, 400));
                }
                BossSpawnerEntity spawnerEntity = this.getSpawner();
                if (spawnerEntity != null) {
                    spawnerEntity.setActive(true);
                }
                this.m_6021_(this.spawnPosition.f_82479_, this.spawnPosition.f_82480_, this.spawnPosition.f_82481_);
                this.m_6668_(this.m_9236_().m_269111_().m_269264_());
                this.m_142467_(Entity.RemovalReason.KILLED);
            }
        }
    }

    protected boolean m_6125_() {
        return this.dropLoot;
    }

    public int getCurrentHits() {
        return this.hits;
    }

    public int getMaxHits() {
        return this.maxHits;
    }

    public void removeThingsForSecondPhase() {
        for (MalkuthCannonEntity malkuthCannonEntity : this.getMalkuthCannons()) {
            malkuthCannonEntity.cancelShot();
        }
        for (MalkuthCannonEntity malkuthCannonEntity : this.getPlayerCannons(false)) {
            malkuthCannonEntity.cancelShot();
        }
        for (MalkuthCannonProjectile malkuthCannonProjectile : BossTargetFinder.getEntitiesInCylinder(MalkuthCannonProjectile.class, this.m_9236_(), this.spawnPosition.m_82520_(0.0, -2.0, 0.0), 30.0f, 40.0f)) {
            malkuthCannonProjectile.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        this.getAnimationSystem().stopAnimation(MAIN_LAYER);
        this.attackChain.reset();
    }

    private AttackAction attackListener(String attack) {
        if (this.m_5448_() == null) {
            return AttackAction.WAIT;
        }
        return AttackAction.PROCEED;
    }

    private boolean fireballsNPlatforms(AttackInstance inst) {
        int stage = inst.stage;
        int tick = inst.tick;
        if (stage == 0) {
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_JUMP_AND_CRUSH).setSpeed(1.25f).build());
            Vec3 between = this.spawnPosition.m_82546_(this.m_20182_());
            Vec3 midpos = this.m_20182_().m_82549_(between.m_82542_(0.4, 0.0, 0.4)).m_82520_(0.0, 15.0, 0.0);
            if (this.startSummonPlatformsPath == null) {
                this.startSummonPlatformsPath = new ProjectileMovementPath(15, false).addPos(this.m_20182_()).addPos(midpos).addPos(this.spawnPosition);
            }
            if (tick > 5) {
                this.startSummonPlatformsPath.tick((Entity)this);
                if (tick == 6) {
                    this.doJumpStartParticles(0.0f);
                }
            }
            if (tick == 25) {
                this.startSummonPlatformsPath = null;
                this.m_20256_(Vec3.f_82478_);
                this.m_6021_(this.spawnPosition.f_82479_, this.spawnPosition.f_82480_, this.spawnPosition.f_82481_);
                this.m_7618_(EntityAnchorArgument.Anchor.FEET, this.m_20182_().m_82520_(0.0, 0.0, -100.0));
                inst.nextStage();
            }
        } else if (stage == 1) {
            if (tick == 1) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
                for (int i = 0; i < 20; ++i) {
                    this.malkuthEarthStrikeStripe(MalkuthAttackType.getRandom(this.f_19796_));
                }
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).amplitude(2.5f).inTime(0).stayTime(0).outTime(5).build(), (Vec3)this.m_20182_(), (double)60.0);
                MalkuthFloorEntity malkuthFloorEntity = new MalkuthFloorEntity(BossEntities.MALKUTH_FLOOR.get(), this.m_9236_());
                malkuthFloorEntity.m_146884_(this.spawnPosition.m_82520_(0.0, -1.0, 0.0));
                this.m_9236_().m_7967_((Entity)malkuthFloorEntity);
                List<Player> combatants = this.getCombatants(true);
                for (Player player : combatants) {
                    if (!(Math.abs(player.m_20186_() - this.spawnPosition.f_82480_) <= 3.0)) continue;
                    FDLibCalls.setServerPlayerSpeed((ServerPlayer)((ServerPlayer)player), (Vec3)new Vec3(0.0, 1.6, 0.0));
                }
                for (Vec3 offset : PLATFORM_SPAWN_OFFSETS) {
                    Vec3 pos = this.spawnPosition.m_82549_(offset);
                    MalkuthPlatform malkuthPlatform = new MalkuthPlatform(BossEntities.MALKUTH_PLATFORM.get(), this.m_9236_());
                    malkuthPlatform.m_146884_(pos);
                    this.m_9236_().m_7967_((Entity)malkuthPlatform);
                }
                inst.nextStage();
            }
        } else if (stage == 2) {
            if (tick == 0) {
                this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_JUMP_TO_FLOAT).nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_FLOAT).build()).build());
            }
            if (tick > 4) {
                BossUtil.malkuthFloatParticles((ServerLevel)this.m_9236_(), this);
                this.currentlyFlyingTo = 0;
                this.f_19794_ = true;
                this.m_20242_(true);
                inst.nextStage();
            }
        } else if (stage == 3) {
            BossUtil.malkuthFloatParticles((ServerLevel)this.m_9236_(), this);
            if (this.m_5448_() != null) {
                this.m_7618_(EntityAnchorArgument.Anchor.EYES, this.m_5448_().m_20182_());
            }
            this.f_19794_ = true;
            this.m_20242_(true);
            int fireballTickStart = 30;
            int fireballTickEnd = fireballTickStart + PLATFORM_SPAWN_OFFSETS.length;
            int fireballLaunchTick = fireballTickEnd + 20;
            int cycleTime = fireballLaunchTick + 5;
            int currentCycle = tick / cycleTime;
            int localTick = tick % cycleTime;
            if (currentCycle > 6) {
                inst.nextStage();
                return false;
            }
            if (this.currentlyFlyingTo == -1) {
                this.currentlyFlyingTo = this.f_19796_.m_188503_(this.FLY_TO_OFFSETS.length);
            }
            if (localTick == 0) {
                int old = this.currentlyFlyingTo;
                while (this.currentlyFlyingTo == old) {
                    this.currentlyFlyingTo = this.f_19796_.m_188503_(this.FLY_TO_OFFSETS.length);
                }
                if (currentCycle == 0) {
                    this.currentlyFlyingTo = 0;
                }
            } else if (localTick < fireballTickStart) {
                Vec3 target = this.spawnPosition.m_82549_(this.FLY_TO_OFFSETS[this.currentlyFlyingTo]);
                this.moveToPos(target);
                int ticksToEnd = fireballTickStart - localTick;
                if (ticksToEnd == 7) {
                    this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_SUMMON_AND_FIRE_FIREBALLS).important().nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_FLOAT).build()).build());
                }
            } else if (localTick < fireballTickEnd) {
                MalkuthAttackType type;
                int id;
                int[] pattern = PLATFORM_ATTACK_PATTERN[currentCycle % PLATFORM_ATTACK_PATTERN.length];
                if (localTick == fireballTickStart) {
                    for (int i = 0; i < PLATFORM_SPAWN_OFFSETS.length; ++i) {
                        id = pattern[i];
                        type = id == 0 ? MalkuthAttackType.FIRE : MalkuthAttackType.ICE;
                        Vec3 offset = PLATFORM_SPAWN_OFFSETS[i];
                        Vec3 centerpos = this.spawnPosition.m_82549_(offset);
                        Vector3f color = MalkuthEntity.getMalkuthAttackPreparationParticleColor(type);
                        RectanglePreparationParticleOptions options = new RectanglePreparationParticleOptions(new Vec3(0.0, 0.0, -1.0), 5.0f, 1.5f, 30, 5, 10, color.x, color.y, color.z, 0.2f);
                        int dir = offset.m_82526_(new Vec3(1.0, 0.0, 0.0)) < 0.0 ? 1 : -1;
                        Vec3 direction = new Vec3((double)dir, 0.0, 0.0);
                        RectanglePreparationParticleOptions options2 = new RectanglePreparationParticleOptions(direction, 5.0f, 1.5f, 30, 5, 10, color.x, color.y, color.z, 0.2f);
                        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)centerpos.m_82520_(0.0, (double)1.06f, 2.5), (double)60.0);
                        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options2, (Vec3)centerpos.m_82520_(0.0, (double)1.061f, 0.0).m_82549_(direction.m_82542_(-2.5, 0.0, 0.0)), (double)60.0);
                    }
                }
                this.m_20256_(Vec3.f_82478_);
                int currentFireball = localTick - fireballTickStart;
                id = pattern[currentFireball];
                type = id == 0 ? MalkuthAttackType.FIRE : MalkuthAttackType.ICE;
                Vec3 forward = this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_();
                Vec3 left = forward.m_82524_(1.5707964f);
                Vec3 platformPos = this.spawnPosition.m_82549_(PLATFORM_SPAWN_OFFSETS[currentFireball]);
                float p = (float)currentFireball / (float)(PLATFORM_SPAWN_OFFSETS.length - 1);
                float angle = (float)Math.PI * p;
                Vector3f spawnOffset = new Quaternionf(new AxisAngle4d((double)angle, forward.f_82479_, forward.f_82480_, forward.f_82481_)).transform((float)left.f_82479_, (float)left.f_82480_, (float)left.f_82481_, new Vector3f()).mul(5.0f);
                Vec3 spawnPos = this.m_20182_().m_82520_((double)(spawnOffset.x * 0.3f), (double)(spawnOffset.y * 0.3f + 2.0f), (double)(spawnOffset.z * 0.3f));
                Vec3 gotoPos = spawnPos.m_82520_((double)spawnOffset.x, (double)spawnOffset.y, (double)spawnOffset.z);
                float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.fireballsDamage);
                MalkuthFireball malkuthFireball = MalkuthFireball.summon(type, this.m_9236_(), spawnPos, gotoPos, platformPos.m_82520_(0.0, 1.5, 0.0), damage);
            } else if (localTick == fireballLaunchTick) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_FIREBALL_LAUNCH.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
                for (MalkuthFireball fireball : this.m_9236_().m_45976_(MalkuthFireball.class, new AABB(-20.0, -20.0, -20.0, 20.0, 20.0, 20.0).m_82383_(this.m_20182_()))) {
                    fireball.setMoveToTarget(12);
                }
            }
        } else if (stage == 4) {
            if (tick == 0) {
                this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_IDLE).important().build());
            }
            if (this.moveToPos(this.spawnPosition)) {
                this.f_19794_ = false;
                this.m_20242_(false);
                this.m_20256_(Vec3.f_82478_);
                this.m_6021_(this.spawnPosition.f_82479_, this.spawnPosition.f_82480_, this.spawnPosition.f_82481_);
                inst.nextStage();
            } else {
                BossUtil.malkuthFloatParticles((ServerLevel)this.m_9236_(), this);
            }
        } else if (stage == 5) {
            if (tick == 5) {
                for (Object platform : this.m_9236_().m_45976_(MalkuthPlatform.class, new AABB(-29.0, -29.0, -29.0, 29.0, 29.0, 29.0).m_82383_(this.spawnPosition))) {
                    ((MalkuthPlatform)((Object)platform)).m_6074_();
                }
                for (Object platform : this.m_9236_().m_45976_(MalkuthFloorEntity.class, new AABB(-29.0, -29.0, -29.0, 29.0, 29.0, 29.0).m_82383_(this.spawnPosition))) {
                    ((MalkuthFloorEntity)((Object)platform)).m_6074_();
                }
            } else if (tick >= 20) {
                return true;
            }
        }
        return false;
    }

    private void malkuthEarthStrikeStripe(MalkuthAttackType type) {
        float rndRadius = 4.0f + FDEasings.easeOut((float)this.f_19796_.m_188501_()) * 3.0f;
        Vec3 rnd = new Vec3((double)rndRadius, 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
        Vec3 dir = rnd.m_82541_();
        float startOffsetRand = 0.25f;
        Vec3 startOffset = dir.m_82542_((double)startOffsetRand, (double)startOffsetRand, (double)startOffsetRand);
        Vec3 stripePos = this.m_20182_().m_82549_(this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_()).m_82549_(startOffset);
        Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(type);
        FDColor fireColorStart = new FDColor(colFire.x, colFire.y - this.f_19796_.m_188501_() * 0.1f - 0.3f, colFire.z, 0.5f);
        FDColor fireColor = new FDColor(colFire.x, colFire.y + this.f_19796_.m_188501_() * 0.1f, colFire.z, 1.0f);
        float firstPointOffset = 1.0f + this.f_19796_.m_188501_() * 1.0f;
        float secondPointOffset = 3.0f + this.f_19796_.m_188501_() * 1.0f;
        StripeParticleOptions stripeParticleOptions = StripeParticleOptions.builder().startColor(fireColorStart).endColor(fireColor).lifetime(10).lod(50).scale(0.1f).stripePercentLength(0.5f).endOutPercent(0.2f).startInPercent(0.2f).offsets(new Vec3((double)0.01f, 0.0, 0.0), dir.m_82542_((double)firstPointOffset, 0.0, (double)firstPointOffset).m_82520_(0.0, 1.5, 0.0), dir.m_82542_((double)secondPointOffset, 0.0, (double)secondPointOffset).m_82520_(0.0, 3.5, 0.0), rnd.m_82520_(0.0, (double)(4.0f + this.f_19796_.m_188501_() * 2.0f), 0.0)).build();
        ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)stripeParticleOptions, stripePos.f_82479_, stripePos.f_82480_, stripePos.f_82481_, 0, 0.0, 0.0, 0.0, 0.0);
    }

    private boolean moveToPos(Vec3 target) {
        Vec3 thisPos = this.m_20182_();
        Vec3 between = target.m_82546_(thisPos);
        double len = between.m_82553_();
        if (len > 0.025) {
            float p = (float)FDMathUtil.clamp((double)(len / 15.0), (double)0.0, (double)1.0);
            float speed = (float)FDMathUtil.lerp((double)0.025, (double)2.0, (double)p);
            this.m_20256_(between.m_82541_().m_82542_((double)speed, (double)speed, (double)speed));
            return false;
        }
        this.m_20256_(Vec3.f_82478_);
        return true;
    }

    private MalkuthAttackType chooseAttackTypeForTarget(LivingEntity target, float chanceToChooseWeakToType) {
        if (target instanceof Player) {
            Player player = (Player)target;
            MalkuthAttackType weakTo = MalkuthWeaknessHandler.getWeakTo(player);
            if (this.f_19796_.m_188501_() < chanceToChooseWeakToType) {
                return weakTo;
            }
            return MalkuthAttackType.getOpposite(weakTo);
        }
        return MalkuthAttackType.getRandom(this.f_19796_);
    }

    private boolean summonEarthquake(AttackInstance inst, int spawnYOffset) {
        int stage = inst.stage;
        int tick = inst.tick;
        if (stage == 0) {
            this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.ANIMATION);
            this.lookAtTarget = true;
            LivingEntity target = this.m_5448_();
            this.m_7618_(EntityAnchorArgument.Anchor.EYES, target.m_20182_());
            this.earthquakeToSummon = this.chooseAttackTypeForTarget(this.m_5448_(), CHANCE_TO_CHOOSE_WEAK_TYPE);
            if (this.earthquakeToSummon.isFire()) {
                this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_SINGLE_EARTHQUAKE_FIRE).important().nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
            } else {
                this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_SINGLE_EARTHQUAKE_ICE).important().nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
            }
            inst.nextStage();
        } else if (stage == 1) {
            if (tick == 8) {
                Vec3 targetPos = this.m_5448_() != null ? this.m_5448_().m_20182_() : this.m_20182_().m_82549_(this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(10.0, 10.0, 10.0));
                Vec3 direction = targetPos.m_82546_(this.m_20182_()).m_82542_(1.5, 0.0, 1.5);
                SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(this.m_20097_(), this.m_20182_().m_82520_(0.0, 0.5, 0.0), direction.m_82541_()).maxAngle((float)Math.PI).maxSpeed(0.5f).collectRadius(2).maxParticleLifetime(30).count(40).maxVerticalSpeedEdges(0.15f).maxVerticalSpeedCenter(0.4f));
                FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)packet);
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.HOSTILE, 3.0f, 1.0f + this.f_19796_.m_188501_() * 0.2f);
                double dist = direction.m_82553_();
                int time = (int)Math.ceil(dist * (double)0.9f);
                float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.impalingDoomDamage);
                float arcAngle = 0.47123894f;
                MalkuthEarthquake malkuthEarthquake = MalkuthEarthquake.summon(this.m_9236_(), this.earthquakeToSummon, this.m_20182_().m_82520_(0.0, (double)spawnYOffset, 0.0), direction, time, arcAngle, damage);
                for (LivingEntity e : BossTargetFinder.getEntitiesInCylinder(LivingEntity.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -0.1, 0.0), 4.0f, 1.0f, entity -> !(entity instanceof MalkuthBossBuddy))) {
                    e.m_6469_((DamageSource)new MalkuthDamageSource(BossDamageSources.MALKUTH_IMPALING_DOOM_SOURCE, this.earthquakeToSummon, 51), damage * 2.0f);
                }
            } else if (tick == 9) {
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).amplitude(2.5f).inTime(0).stayTime(0).outTime(5).build(), (Vec3)this.m_20182_(), (double)60.0);
            } else if (tick >= 20) {
                this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.LOOK);
                return true;
            }
        }
        return false;
    }

    private boolean attachSwordsAttack(AttackInstance instance, boolean attach) {
        if (attach) {
            this.attachSwords();
            this.causeSwordChargeParticles(MalkuthAttackType.FIRE);
            this.causeSwordChargeParticles(MalkuthAttackType.ICE);
        } else {
            this.deattachSwords();
            this.causeSwordChargeParticles(MalkuthAttackType.FIRE);
            this.causeSwordChargeParticles(MalkuthAttackType.ICE);
        }
        return true;
    }

    private boolean summonAndThrowSideRocks(AttackInstance inst) {
        int stage = inst.stage;
        int lstage = stage % 3;
        int tick = inst.tick;
        if (lstage == 0) {
            this.deattachSwords();
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_SUMMON_THROW_SIDE_STONES).important().setSpeed(0.8f).nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
            inst.nextStage();
        } else if (lstage == 1) {
            if (tick == 10) {
                int count = 8;
                float distForOne = 3.4f;
                Vec3 startPos = this.m_20182_().m_82520_(0.0, 0.1, -1.0);
                int h = this.f_19796_.m_188503_(2);
                float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.sideRocksDamage);
                MalkuthAttackType currentType = this.sideRocksCurrentType;
                for (int i = 0; i < count; ++i) {
                    int k = h % 2 == 0 ? 1 : -1;
                    Vec3 clipdir = new Vec3((double)(k * 30), 0.0, 0.0);
                    Vec3 cliplastpos = startPos.m_82549_(clipdir);
                    ClipContext clipContext = new ClipContext(startPos, cliplastpos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
                    BlockHitResult ctx = this.m_9236_().m_45547_(clipContext);
                    Vec3 location = ctx.m_82450_();
                    double x = location.f_82479_ - (double)k;
                    double r = startPos.f_82479_ - x;
                    double y = this.m_20186_() - 1.0;
                    double z = startPos.f_82481_;
                    Vec3 summonPos = new Vec3(x, y, z);
                    Vec3 moveToPos = new Vec3(x + r * 2.0, y, z);
                    ProjectileMovementPath movementPath = new ProjectileMovementPath(summonPos, 10, false).addPos(moveToPos);
                    MalkuthBoulderEntity malkuthBoulderEntity = MalkuthBoulderEntity.summon(this.m_9236_(), summonPos, 10, 3.0f, movementPath, currentType, damage);
                    Vector3f col = MalkuthEntity.getMalkuthAttackPreparationParticleColor(currentType);
                    RectanglePreparationParticleOptions rectanglePreparationParticleOptions = new RectanglePreparationParticleOptions(new Vec3((double)(-k), 0.0, 0.0), 56.0f, distForOne / 2.0f, 30, 10, 10, col.x, col.y, col.z, 0.25f);
                    Vec3 ppos = new Vec3(this.m_20185_() + (double)(k * 28), y + (double)0.01f, z);
                    FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)rectanglePreparationParticleOptions, (Vec3)ppos, (double)60.0);
                    currentType = currentType.isFire() ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
                    startPos = startPos.m_82520_(0.0, 0.0, (double)(-distForOne));
                    ++h;
                }
                this.sideRocksCurrentType = this.sideRocksCurrentType.isFire() ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
            } else if (tick > 40) {
                List l = this.m_9236_().m_45976_(MalkuthBoulderEntity.class, new AABB(-30.0, -30.0, -30.0, 30.0, 30.0, 30.0).m_82383_(this.m_20182_()));
                for (MalkuthBoulderEntity b : l) {
                    b.setShouldMoveToTarget(true);
                }
                inst.nextStage();
            }
        } else if (lstage == 2) {
            return true;
        }
        return false;
    }

    private boolean jumpBackOnSpawn(AttackInstance attackInstance, boolean crush) {
        int stage = attackInstance.stage;
        int tick = attackInstance.tick;
        ProjectileMovementPath path = this.jumpBackOnSpawnPath;
        if (this.jumpBackOnSpawnPath == null) {
            Vec3 thisPos = this.m_20182_();
            Vec3 target = this.spawnPosition;
            Vec3 between = target.m_82546_(thisPos);
            Vec3 pos1 = thisPos.m_82549_(between.m_82542_(0.25, 0.25, 0.25).m_82520_(0.0, 5.0, 0.0));
            Vec3 pos2 = thisPos.m_82549_(between.m_82542_(0.5, 0.5, 0.5).m_82520_(0.0, 8.0, 0.0));
            Vec3 pos3 = thisPos.m_82549_(between.m_82542_(0.75, 0.75, 0.75).m_82520_(0.0, 5.0, 0.0));
            this.jumpBackOnSpawnPath = path = new ProjectileMovementPath(20, false).addPos(thisPos).addPos(pos1).addPos(pos2).addPos(pos3).addPos(target);
            attackInstance.stage = 0;
            attackInstance.nextStage();
        }
        int earthquakesCount = 5;
        float angle = (float)Math.PI / (float)earthquakesCount;
        float radius = 28.0f;
        if (stage == 1) {
            Vec3 lastpos = (Vec3)this.jumpBackOnSpawnPath.getPositions().get(this.jumpBackOnSpawnPath.getPositions().size() - 1);
            if (crush && tick == 10) {
                this.jumpEndEarthquakePrepareParticles(lastpos.m_82520_(0.0, -0.99, 0.0), earthquakesCount, angle, radius);
            }
            if (tick == 0) {
                this.jumpBackOnSpawnCrushType = MalkuthAttackType.getRandom(this.f_19796_);
                if (!crush) {
                    this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_JUMP_AND_LAND).nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
                } else {
                    this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_JUMP_AND_CRUSH).nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
                }
            } else if (tick > 5) {
                this.f_19794_ = true;
                this.m_20242_(true);
                this.m_21563_().m_24964_(lastpos);
                this.lookAtTarget = false;
                if (path.isFinished()) {
                    this.m_20256_(Vec3.f_82478_);
                    this.m_6021_(lastpos.f_82479_, lastpos.f_82480_, lastpos.f_82481_);
                    this.f_19794_ = false;
                    this.m_20242_(false);
                    this.lookAtTarget = true;
                    attackInstance.nextStage();
                } else {
                    path.tick((Entity)this);
                }
            }
        } else if (stage == 2) {
            int waitTime;
            if (tick == 3 && crush) {
                this.jumpEarthquake(this.m_20182_().m_82520_(0.0, -0.99, 0.0), earthquakesCount, angle, radius);
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.ROCK_IMPACT.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
            }
            this.f_19794_ = false;
            this.m_20242_(false);
            this.lookAtTarget = true;
            int n = waitTime = crush ? 30 : 10;
            if (tick == 0 && this.m_5448_() != null) {
                this.m_7618_(EntityAnchorArgument.Anchor.EYES, this.m_5448_().m_20182_());
            }
            if (tick >= waitTime) {
                this.jumpBackOnSpawnPath = null;
                return true;
            }
        }
        return false;
    }

    private void jumpEndEarthquakePrepareParticles(Vec3 lastpos, int earthquakesCount, float angle, float radius) {
        MalkuthAttackType localType = this.jumpBackOnSpawnCrushType;
        for (int i = 0; i < earthquakesCount; ++i) {
            float currentAngle = (float)i * angle + angle / 2.0f;
            Vector3f col = MalkuthEntity.getMalkuthAttackPreparationParticleColor(localType);
            Vec3 v = new Vec3(1.0, 0.0, 0.0).m_82524_(currentAngle);
            ArcAttackPreparationParticleOptions options = new ArcAttackPreparationParticleOptions(v, radius, angle / 2.0f, 20, 5, 10, col.x, col.y, col.z, 0.25f);
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)options, lastpos.f_82479_, lastpos.f_82480_, lastpos.f_82481_, 1, 0.0, 0.0, 0.0, 0.0);
            localType = localType.isFire() ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
        }
    }

    private void jumpEarthquake(Vec3 lastpos, int earthquakesCount, float angle, float radius) {
        MalkuthAttackType localType = this.jumpBackOnSpawnCrushType;
        float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.jumpBackOnSpawnCrushDamage);
        for (int i = 0; i < earthquakesCount; ++i) {
            float currentAngle = (float)i * angle + angle / 2.0f;
            Vec3 v = new Vec3((double)radius, 0.0, 0.0).m_82524_(currentAngle);
            MalkuthEarthquake earthquake = MalkuthEarthquake.summon(this.m_9236_(), localType, lastpos, v, 40, angle, damage);
            localType = localType.isFire() ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
        }
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).amplitude(5.0f).inTime(0).stayTime(0).outTime(5).build(), (Vec3)lastpos, (double)120.0);
    }

    private Animation getSlashAttackAnimation(MalkuthAttackType malkuthAttackType) {
        if (malkuthAttackType.isFire()) {
            return (Animation)BossAnims.MALKUTH_SLASH_FIRE.get();
        }
        return (Animation)BossAnims.MALKUTH_SLASH_ICE.get();
    }

    private boolean aerialSlashAttack(AttackInstance inst) {
        int stage = inst.stage;
        int tick = inst.tick;
        int localStage = stage % 3;
        if (localStage == 0) {
            this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.LOOK);
            this.slashAttackType = this.chooseAttackTypeForTarget(this.m_5448_(), CHANCE_TO_CHOOSE_WEAK_TYPE);
            Animation animation = this.getSlashAttackAnimation(this.slashAttackType);
            AnimationTicker ticker = AnimationTicker.builder((Animation)animation).nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).important().build();
            this.getAnimationSystem().startAnimation(MAIN_LAYER, ticker);
            inst.nextStage();
        } else if (localStage == 1) {
            if (tick == 24) {
                Vec3 targetPos;
                Vec3 spawnPos = this.m_20182_().m_82520_(0.0, 1.5, 0.0);
                float speedMod = 1.0f;
                if (this.m_5448_() != null) {
                    LivingEntity t = this.m_5448_();
                    if (t.m_21023_(MobEffects.f_19596_)) {
                        speedMod = t.m_21124_(MobEffects.f_19596_).m_19564_() + 1;
                    }
                    targetPos = t.m_20182_().m_82520_(0.0, (double)(t.m_20206_() / 2.0f), 0.0);
                } else {
                    targetPos = this.m_20182_().m_82549_(this.m_20156_().m_82542_(100.0, 0.0, 100.0));
                }
                float spawnForwardOffset = -speedMod;
                spawnPos = spawnPos.m_82549_(this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82542_((double)spawnForwardOffset, (double)spawnForwardOffset, (double)spawnForwardOffset));
                Vec3 speedv = targetPos.m_82546_(spawnPos);
                double speed = Math.min(Math.max(2.0, speedv.m_82553_() * (double)0.15f) * (double)speedMod, 5.0);
                speedv = speedv.m_82541_().m_82542_(speed, speed, speed);
                float rotation = this.slashAttackType.isFire() ? 25.0f : -25.0f;
                float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.slashAttackDamage);
                MalkuthSlashProjectile malkuthSlashProjectile = MalkuthSlashProjectile.summon(this.m_9236_(), spawnPos, speedv, this.slashAttackType, damage, 2.2f, rotation, 0);
                for (LivingEntity e : BossTargetFinder.getEntitiesInCylinder(LivingEntity.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -0.1, 0.0), 4.0f, 1.0f, entity -> !(entity instanceof MalkuthBossBuddy))) {
                    e.m_6469_((DamageSource)new MalkuthDamageSource(BossDamageSources.MALKUTH_SLASHES_SOURCE, this.slashAttackType, 33), damage * 2.0f);
                }
            } else if (tick == 20) {
                this.playSlashSound();
            } else if (tick >= 28) {
                inst.nextStage();
            } else if (tick == 8) {
                this.causeSwordChargeParticles(this.slashAttackType);
                for (int i = 0; i < 6; ++i) {
                    this.doSwordChargeStripe(this.slashAttackType, 3.0f, 0.5f, 0.25f, false);
                }
            }
        } else if (localStage == 2) {
            return this.m_5448_() != null;
        }
        return false;
    }

    private boolean jumpCrushAttack(AttackInstance inst) {
        int stage = inst.stage;
        int tick = inst.tick;
        if (stage == 0) {
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_CRUSH_ATTACK_FULL).setLoopMode(Animation.LoopMode.ONCE).nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
            if (tick == 5) {
                this.jumpCrushAttackMovementPath = this.createJumpCrushAttackMovementPath(15);
                this.m_7618_(EntityAnchorArgument.Anchor.FEET, (Vec3)this.jumpCrushAttackMovementPath.getPositions().get(this.jumpCrushAttackMovementPath.getPositions().size() - 1));
                inst.nextStage();
            }
        } else if (stage == 1) {
            if (tick == 1) {
                this.doJumpStartParticles(-2.0f);
            }
            this.m_20242_(true);
            this.f_19794_ = true;
            this.jumpCrushAttackMovementPath.tick((Entity)this);
            this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.ANIMATION);
            if (this.jumpCrushAttackMovementPath.isFinished()) {
                Vec3 lastPos = (Vec3)this.jumpCrushAttackMovementPath.getPositions().get(this.jumpCrushAttackMovementPath.getPositions().size() - 1);
                Vec3 between = lastPos.m_82546_(this.m_20182_());
                double dist = between.m_82553_();
                if (dist < 0.5) {
                    inst.nextStage();
                } else {
                    this.m_20256_(between.m_82542_(0.5, 0.5, 0.5));
                }
            } else if (tick < 20 && this.m_5448_() != null) {
                this.jumpCrushAttackMovementPath.getPositions().set(this.jumpCrushAttackMovementPath.getPositions().size() - 1, this.m_5448_().m_20182_());
            }
        } else if (stage == 2) {
            this.m_20242_(false);
            this.f_19794_ = false;
            if (tick == 5) {
                Vec3 basePos = this.m_20182_().m_82549_(this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_());
                Vec3 actualPos = this.findGroundPosForCrush(basePos);
                if (this.isBelowHalfHP()) {
                    this.summonRepairCrystal(actualPos);
                }
                float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.earthshatterDamage);
                MalkuthCrushAttack malkuthCrushAttack = MalkuthCrushAttack.summon(this.m_9236_(), actualPos, damage);
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).amplitude(5.0f).inTime(0).stayTime(0).outTime(5).build(), (Vec3)malkuthCrushAttack.m_20182_(), (double)120.0);
                this.m_9236_().m_6263_(null, malkuthCrushAttack.m_20185_(), malkuthCrushAttack.m_20186_(), malkuthCrushAttack.m_20189_(), BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
                this.m_9236_().m_6263_(null, malkuthCrushAttack.m_20185_(), malkuthCrushAttack.m_20186_(), malkuthCrushAttack.m_20189_(), BossSounds.ROCK_IMPACT.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
            } else if (tick >= 10) {
                return true;
            }
        }
        return false;
    }

    private void summonRepairCrystal(Vec3 pos) {
        Vec3 posBelow = pos.m_82520_(0.0, -20.0, 0.0);
        BlockHitResult result = this.m_9236_().m_45547_(new ClipContext(pos, posBelow, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        pos = result.m_82450_();
        if (!BossTargetFinder.isPointInCylinder(pos, this.spawnPosition.m_82520_(0.0, -2.0, 0.0), 21.0f, 29.0f)) {
            return;
        }
        List<MalkuthRepairCrystal> crystals = BossTargetFinder.getEntitiesInCylinder(MalkuthRepairCrystal.class, this.m_9236_(), this.spawnPosition.m_82520_(0.0, -2.0, 0.0), 20.0f, 30.0f);
        List<MalkuthCannonEntity> playerCannons = this.getPlayerCannons(true);
        int spawnedCrystals = crystals.size();
        int cannonsAmount = playerCannons.size();
        if (crystals.size() == playerCannons.size()) {
            return;
        }
        int fireCrystals = (int)crystals.stream().filter(c -> c.getCrystalType().isFire()).count();
        int iceCrystals = spawnedCrystals - fireCrystals;
        int fireCannons = (int)playerCannons.stream().filter(cannon -> cannon.getCannonType().isFire()).count();
        int iceCannons = cannonsAmount - fireCannons;
        if (fireCrystals >= fireCannons && iceCrystals >= iceCannons) {
            return;
        }
        MalkuthAttackType finalType = fireCrystals >= fireCannons ? MalkuthAttackType.ICE : (iceCrystals >= iceCannons ? MalkuthAttackType.FIRE : MalkuthAttackType.getRandom(this.f_19796_));
        MalkuthRepairCrystal malkuthRepairCrystal = new MalkuthRepairCrystal(BossEntities.MALKUTH_REPAIR_CRYSTAL.get(), this.m_9236_());
        malkuthRepairCrystal.m_20088_().m_135381_(MalkuthRepairCrystal.CRYSTAL_TYPE, (Object)finalType);
        malkuthRepairCrystal.m_146884_(pos);
        this.m_9236_().m_7967_((Entity)malkuthRepairCrystal);
    }

    private Vec3 findGroundPosForCrush(Vec3 crushBasePos) {
        Vec3 end = crushBasePos.m_82520_(0.0, -5.0, 0.0);
        ClipContext clipContext = new ClipContext(crushBasePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
        BlockHitResult res = this.m_9236_().m_45547_(clipContext);
        if (res.m_6662_() == HitResult.Type.BLOCK) {
            return res.m_82450_();
        }
        return crushBasePos;
    }

    private ProjectileMovementPath createJumpCrushAttackMovementPath(int flyTime) {
        Vec3 target = this.m_5448_() != null ? this.m_5448_().m_20182_() : this.m_20182_().m_82549_(this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(10.0, 0.0, 10.0));
        Vec3 begin = this.m_20182_();
        Vec3 between = target.m_82546_(begin);
        Vec3 pos1 = this.m_20182_().m_82549_(between.m_82542_(0.25, 0.25, 0.25)).m_82520_(0.0, 9.0, 0.0);
        Vec3 pos2 = this.m_20182_().m_82549_(between.m_82542_((double)0.33f, (double)0.33f, (double)0.33f)).m_82520_(0.0, 13.0, 0.0);
        Vec3 pos3 = this.m_20182_().m_82549_(between.m_82542_(0.5, 0.5, 0.5)).m_82520_(0.0, 9.0, 0.0);
        ProjectileMovementPath path = new ProjectileMovementPath(begin, flyTime, false).addPos(pos1).addPos(pos2).addPos(pos3).addPos(target);
        return path;
    }

    private boolean checkCanPunch() {
        if (this.pullAndPunchCooldown > 0) {
            return false;
        }
        AABB box = new AABB(-29.0, 5.0, -29.0, 29.0, 29.0, 29.0).m_82383_(this.m_20182_());
        List players = this.m_9236_().m_45976_(Player.class, box);
        return !players.isEmpty();
    }

    private List<Player> getPullAndPunchTargets() {
        AABB box = new AABB(-29.0, 4.0, -29.0, 29.0, 29.0, 29.0).m_82383_(this.m_20182_());
        return new ArrayList<Player>(this.m_9236_().m_6443_(Player.class, box, player -> !player.m_7500_() && !player.m_5833_()));
    }

    private boolean pullAndPunch(AttackInstance inst) {
        int stage = inst.stage;
        int tick = inst.tick;
        if (stage == 0) {
            if (this.m_5448_() != null) {
                this.m_7618_(EntityAnchorArgument.Anchor.EYES, this.m_5448_().m_20182_());
            }
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_PULL_AND_PUNCH).nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
            this.pullAndPunchTargets = this.getPullAndPunchTargets();
            inst.nextStage();
        } else if (stage == 1) {
            this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.LOOK);
            if (tick == 6) {
                this.causeSwordChargeParticles(MalkuthAttackType.ICE);
                this.deattachIceSword();
            } else if (tick == 10) {
                if (this.m_5448_() == null) {
                    this.causeSwordChargeParticles(MalkuthAttackType.ICE);
                    this.attachIceSword();
                    this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build());
                    return true;
                }
                if (this.pullAndPunchTargets.isEmpty()) {
                    this.pullAndPunchTargets = this.getPullAndPunchTargets();
                }
                for (Player target : this.pullAndPunchTargets) {
                    Vec3 pullToPos = this.m_20182_().m_82520_(0.0, 2.5, 0.0).m_82549_(this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(2.0, 2.0, 2.0));
                    MalkuthChainEntity malkuthChainEntity = MalkuthChainEntity.summon(this.m_9236_(), this, MalkuthAttackType.ICE, pullToPos, (LivingEntity)target, 10, 10);
                }
                inst.nextStage();
            }
        } else if (stage == 2) {
            if (tick > 0 && tick < 11) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_CHAIN_PULL.get(), SoundSource.HOSTILE, 2.5f, 0.85f);
            } else if (tick < 21) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_CHAIN_PULL.get(), SoundSource.HOSTILE, 2.5f, 0.9f);
            }
            if (tick == 10) {
                this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.ANIMATION);
            } else if (tick == 21) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_PUNCH.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            } else if (tick == 25) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.CHESED_CRYSTAL_HIT.get(), SoundSource.HOSTILE, 2.0f, 0.5f);
                Vector3f pos = this.getModelPartPosition((Entity)this, MalkuthEntity.getMalkuthSwordPlaceBone(MalkuthAttackType.ICE), SERVER_MODEL);
                Vector3f col = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.ICE);
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(5.0f).scalingOptions(0, 0, 2).color(col.x, col.y, col.z).brightness(2).build(), (Vec3)this.m_20182_().m_82520_((double)pos.x, (double)pos.y, (double)pos.z), (double)60.0);
                float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.pullAndPunchDamage);
                for (MalkuthChainEntity chain : this.m_9236_().m_45976_(MalkuthChainEntity.class, this.m_20191_().m_82377_(20.0, 20.0, 20.0))) {
                    ArrayList passengers = new ArrayList(chain.m_20197_());
                    chain.m_20153_();
                    chain.m_142467_(Entity.RemovalReason.DISCARDED);
                    Vec3 forward = this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_();
                    for (Entity e : passengers) {
                        if (!(e instanceof LivingEntity)) continue;
                        LivingEntity livingEntity = (LivingEntity)e;
                        livingEntity.m_6469_(BossDamageSources.MALKUTH_CHAINPUNCH_SOURCE, damage);
                        Vec3 speed = forward.m_82542_(6.0, 6.0, 6.0).m_82520_(0.0, -1.0, 0.0);
                        if (livingEntity instanceof ServerPlayer) {
                            ServerPlayer player = (ServerPlayer)livingEntity;
                            FDLibCalls.setServerPlayerSpeed((ServerPlayer)player, (Vec3)speed);
                            player.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 33, 0, true, false));
                            player.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 5, 0, true, false));
                            player.m_7292_(new MobEffectInstance(MobEffects.f_19604_, 100, 0, true, false));
                            FDPacketHandler.INSTANCE.sendTo((Object)new DefaultShakePacket(FDShakeData.builder().stayTime(0).inTime(0).outTime(35).amplitude(0.2f).build()), player.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
                            continue;
                        }
                        livingEntity.m_20256_(speed);
                    }
                }
            } else if (tick == 35) {
                this.causeSwordChargeParticles(MalkuthAttackType.ICE);
                this.attachIceSword();
                this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.LOOK);
            } else if (tick >= 40) {
                this.pullAndPunchCooldown = BossConfigs.BOSS_CONFIG.get().malkuthConfig.chainpunchCooldown;
                return true;
            }
        }
        return false;
    }

    private boolean jumpAndCommandCannons(AttackInstance attackInstance) {
        this.allowedToBeDamaged = true;
        this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.ANIMATION);
        this.lookAtTarget = false;
        this.m_21563_().m_24964_(this.m_20182_().m_82520_(0.0, 0.0, -100.0));
        int stage = attackInstance.stage;
        int tick = attackInstance.tick;
        if (stage == 0) {
            for (MalkuthCannonEntity cannon : BossTargetFinder.getEntitiesInCylinder(MalkuthCannonEntity.class, this.m_9236_(), this.spawnPosition.m_82520_(0.0, -5.0, 0.0), 20.0f, 35.0f)) {
                cannon.setBroken(false);
            }
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_JUMP_AND_LAND).important().nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
            this.jumpOnWallPath = this.makeJumpOnWallPath(23, false);
            attackInstance.nextStage();
        } else if (stage == 1) {
            if (this.jumpOnWallPath == null) {
                this.jumpOnWallPath = this.makeJumpOnWallPath(23, false);
            }
            if (tick >= 6) {
                if (tick == 6) {
                    this.doJumpStartParticles(0.0f);
                }
                this.jumpOnWallPath.tick((Entity)this);
                this.f_19794_ = true;
                this.m_20242_(true);
            }
            if (this.jumpOnWallPath.isFinished()) {
                this.m_7618_(EntityAnchorArgument.Anchor.EYES, this.m_20182_().m_82520_(0.0, 0.0, -100.0));
                Vec3 last = (Vec3)this.jumpOnWallPath.getPositions().get(this.jumpOnWallPath.getPositions().size() - 1);
                this.m_20242_(false);
                this.f_19794_ = false;
                this.m_6021_(last.f_82479_, last.f_82480_, last.f_82481_);
                attackInstance.nextStage();
                this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_SWORD_FORWARD).setToNullTransitionTime(0).build());
            }
        } else if (stage == 2) {
            int start = 6;
            int t = tick - start;
            if (t >= 160) {
                attackInstance.nextStage();
            }
            if (t % 40 == 0) {
                this.shootCannons();
            }
        } else if (stage == 3) {
            if (tick > 60) {
                this.allowedToBeDamaged = false;
                this.lookAtTarget = true;
                this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.LOOK);
                return true;
            }
            return false;
        }
        return false;
    }

    private void shootCannons() {
        this.shootCannons(5);
    }

    private void shootCannons(int amountPerPoint) {
        List<MalkuthCannonEntity> cannons = this.getMalkuthCannons();
        if (cannons.isEmpty()) {
            return;
        }
        List<Player> combatants = this.getCombatants(false);
        ArrayList<Vec3> positions = new ArrayList<Vec3>();
        for (Player combatant : combatants) {
            for (int i = 0; i < 4; ++i) {
                Vec3 p = combatant.m_20182_().m_82520_((double)(this.f_19796_.m_188501_() * 8.0f - 4.0f), 0.0, (double)(this.f_19796_.m_188501_() * 8.0f - 4.0f));
                positions.add(p);
            }
            positions.add(combatant.m_20182_());
        }
        Vec3 start = this.spawnPosition.m_82520_(0.0, -1.0, 0.0);
        Vec3 v = new Vec3(0.0, 0.0, -16.5);
        float rndRadius = 14.5f;
        for (int i = 0; i < 3; ++i) {
            Vec3 p1 = start.m_82549_(v.m_82524_((float)i * (float)Math.PI / 8.0f));
            Vec3 p2 = start.m_82549_(v.m_82524_((float)(-i) * (float)Math.PI / 8.0f));
            for (int k = 0; k < amountPerPoint; ++k) {
                Vec3 rndPos = p1.m_82520_((double)(this.f_19796_.m_188501_() * 2.0f * rndRadius - rndRadius), 0.0, (double)(this.f_19796_.m_188501_() * 2.0f * rndRadius - rndRadius));
                Vec3 rndPos2 = p2.m_82520_((double)(this.f_19796_.m_188501_() * 2.0f * rndRadius - rndRadius), 0.0, (double)(this.f_19796_.m_188501_() * 2.0f * rndRadius - rndRadius));
                positions.add(rndPos);
                if (i == 0) continue;
                positions.add(rndPos2);
            }
        }
        int baseAmountPerCannon = positions.size() / cannons.size();
        int spreadAmount = baseAmountPerCannon * cannons.size();
        HashMap<MalkuthCannonEntity, List> cannonTargets = new HashMap<MalkuthCannonEntity, List>();
        int currentCannon = 0;
        while (!positions.isEmpty()) {
            Vec3 pos = (Vec3)positions.remove(0);
            MalkuthCannonEntity cannon = cannons.get(currentCannon);
            cannonTargets.computeIfAbsent(cannon, c -> new ArrayList()).add(pos);
            currentCannon = (currentCannon + 1) % cannons.size();
            if (--spreadAmount > 0) continue;
            currentCannon = this.f_19796_.m_188503_(cannons.size());
        }
        float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.cannonDamage);
        for (Map.Entry entry : cannonTargets.entrySet()) {
            MalkuthCannonEntity cannon = (MalkuthCannonEntity)entry.getKey();
            cannon.shoot((List)entry.getValue(), damage);
        }
    }

    private ProjectileMovementPath makeJumpOnWallPath(int time, boolean reversed) {
        Vec3 start = this.m_20182_();
        Vec3 offset = WALL_OFFSET;
        Vec3 end = this.spawnPosition.m_82549_(offset);
        double yDiff = end.f_82480_ - start.f_82480_;
        Vec3 hoffset = end.m_82546_(start).m_82542_(1.0, 0.0, 1.0);
        Vec3 v1 = start.m_82549_(hoffset.m_82542_(0.25, 0.25, 0.25).m_82520_(0.0, yDiff + 5.0, 0.0));
        Vec3 v2 = start.m_82549_(hoffset.m_82542_(0.5, 0.5, 0.5).m_82520_(0.0, yDiff + 10.0, 0.0));
        Vec3 v3 = start.m_82549_(hoffset.m_82542_(0.75, 0.75, 0.75).m_82520_(0.0, yDiff + 5.0, 0.0));
        ProjectileMovementPath path = new ProjectileMovementPath(time, false);
        if (!reversed) {
            path.addPos(start);
            path.addPos(v1);
            path.addPos(v2);
            path.addPos(v3);
            path.addPos(end);
        } else {
            path.addPos(end);
            path.addPos(v3);
            path.addPos(v2);
            path.addPos(v1);
            path.addPos(start);
        }
        return path;
    }

    protected void doJumpStartParticles(float verticalOffset) {
        SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(this.m_20097_(), this.m_20182_().m_82520_(0.0, (double)verticalOffset, 0.0), new Vec3(1.0, 0.0, 0.0)).maxAngle((float)Math.PI * 2).maxSpeed(0.3f).collectRadius(2).maxParticleLifetime(30).count(20).maxVerticalSpeedEdges(0.15f).maxVerticalSpeedCenter(0.15f));
        FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)packet);
    }

    private void hideRepairCrystals(boolean hide) {
        for (MalkuthRepairCrystal crystal : BossTargetFinder.getEntitiesInCylinder(MalkuthRepairCrystal.class, this.m_9236_(), this.spawnPosition.m_82520_(0.0, -2.0, 0.0), 10.0f, 40.0f)) {
            crystal.setHidden(hide);
        }
    }

    private boolean carouselSlashesAttack(AttackInstance attackInstance) {
        int stage = attackInstance.stage;
        int tick = attackInstance.tick;
        int preparationTime = 20;
        float radius = 30.0f;
        Vec3 startVec = new Vec3(1.0, 0.0, 0.0);
        int slashesAmount = 10;
        this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.ANIMATION);
        if (tick == 0) {
            this.hideRepairCrystals(true);
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_CAROUSEL_SLASH_1).important().nextAnimation(AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build()).build());
            float angle = (float)Math.PI / (float)slashesAmount;
            MalkuthAttackType localCarouselSlash = this.currentStartCarouselSlash;
            for (int i = 0; i < slashesAmount; ++i) {
                Vec3 direction = startVec.m_82524_((float)i * angle + angle / 2.0f);
                Vector3f col = MalkuthEntity.getMalkuthAttackPreparationParticleColor(localCarouselSlash);
                ArcAttackPreparationParticleOptions options = new ArcAttackPreparationParticleOptions(direction, radius, angle / 2.0f, preparationTime, 10, 10, col.x, col.y, col.z, 0.25f);
                ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)options, this.m_20185_(), this.m_20186_() - 0.99, this.m_20189_(), 1, 0.0, 0.0, 0.0, 0.0);
                localCarouselSlash = localCarouselSlash.isFire() ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
            }
        } else if (tick == preparationTime) {
            float angle = (float)Math.PI / (float)slashesAmount;
            float maxSlashSize = (float)Math.sqrt(2.0f * radius * radius * (1.0f - (float)Math.cos(angle))) / 2.0f;
            float slashSpeed = 2.5f;
            int reachDestinationTime = Math.round(radius / slashSpeed);
            float verticalSpeed = -1.0f / (float)reachDestinationTime;
            MalkuthAttackType localCarouselSlash = this.currentStartCarouselSlash;
            float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.arcSlashesDamage);
            this.playSlashSound();
            for (int i = 0; i < slashesAmount; ++i) {
                Vec3 direction = startVec.m_82524_((float)i * angle + angle / 2.0f);
                Vec3 speed = direction.m_82542_((double)slashSpeed, (double)slashSpeed, (double)slashSpeed);
                speed = speed.m_82520_(0.0, (double)(verticalSpeed * 0.15f), 0.0);
                MalkuthSlashProjectile.summon(this.m_9236_(), this.m_20182_().m_82520_(0.0, 0.25, 0.0), speed, localCarouselSlash, damage, maxSlashSize + 2.0f, 0.0f, reachDestinationTime);
                localCarouselSlash = localCarouselSlash.isFire() ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
            }
            for (LivingEntity entity : BossTargetFinder.getEntitiesInCylinder(LivingEntity.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -2.0, 0.0), 5.0f, 2.0f)) {
                MalkuthAttackType malkuthAttackType;
                if (entity instanceof MalkuthBossBuddy) continue;
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    malkuthAttackType = MalkuthWeaknessHandler.getWeakTo(player);
                } else {
                    malkuthAttackType = MalkuthAttackType.getRandom(this.f_19796_);
                }
                entity.m_6469_((DamageSource)new MalkuthDamageSource(BossDamageSources.MALKUTH_SLASHES_SOURCE, malkuthAttackType, 33), damage);
            }
            this.currentStartCarouselSlash = this.currentStartCarouselSlash.isFire() ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
        } else if (tick >= preparationTime + 20) {
            this.hideRepairCrystals(false);
            return true;
        }
        return false;
    }

    private void playSlashSound() {
        this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_SLASH.get(), SoundSource.HOSTILE, 2.0f, 1.0f + this.f_19796_.m_188501_() * 0.5f);
    }

    public static Vector3f getAndRandomizeColor(MalkuthAttackType malkuthAttackType, RandomSource random) {
        Vector3f color = MalkuthEntity.getMalkuthAttackPreparationParticleColor(malkuthAttackType);
        color.x = FDMathUtil.clamp((float)(color.x + random.m_188501_() * 0.2f), (float)0.0f, (float)1.0f);
        color.y = FDMathUtil.clamp((float)(color.y + random.m_188501_() * 0.2f), (float)0.0f, (float)1.0f);
        color.z = FDMathUtil.clamp((float)(color.z + random.m_188501_() * 0.2f), (float)0.0f, (float)1.0f);
        return color;
    }

    public static Vector3f getMalkuthAttackPreparationParticleColor(MalkuthAttackType attackType) {
        float b;
        float g;
        float r;
        if (attackType.isFire()) {
            r = 1.0f;
            g = 0.4f;
            b = 0.1f;
        } else {
            r = 0.1f;
            g = 0.8f;
            b = 1.0f;
        }
        return new Vector3f(r, g, b);
    }

    public boolean giantSwordUltimate(AttackInstance inst) {
        this.allowedToBeDamaged = false;
        int stage = inst.stage;
        int tick = inst.tick;
        if (stage == 0) {
            AABB firstBox = new AABB(0.0, -2.0, -29.0, 29.0, 10.0, 0.0).m_82383_(this.spawnPosition);
            AABB secondBox = new AABB(-29.0, -2.0, -29.0, 0.0, 10.0, 0.0).m_82383_(this.spawnPosition);
            List players = this.m_9236_().m_6443_(Player.class, firstBox, player -> true);
            if (players.isEmpty()) {
                players = this.m_9236_().m_6443_(Player.class, secondBox, player -> true);
                if (!players.isEmpty()) {
                    Player firstPlayer = (Player)players.get(0);
                    MalkuthAttackType weakTo = MalkuthWeaknessHandler.getWeakTo(firstPlayer);
                    this.giantSwordUltimateStartAttackType = MalkuthAttackType.getOpposite(weakTo);
                }
            } else {
                MalkuthAttackType weakTo;
                Player firstPlayer = (Player)players.get(0);
                this.giantSwordUltimateStartAttackType = weakTo = MalkuthWeaknessHandler.getWeakTo(firstPlayer);
            }
            this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_GIANT_SWORD_ATTACK).setLoopMode(Animation.LoopMode.HOLD_ON_LAST_FRAME).build());
            this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.ANIMATION);
            this.lookAtTarget = false;
            this.m_21563_().m_24964_(new Vec3(0.0, 0.0, -100.0).m_82549_(this.m_20182_()));
            inst.nextStage();
        } else if (stage == 1) {
            int swordSpawnTick = 12;
            int particlesStart = swordSpawnTick - 3;
            int particlesEnd = swordSpawnTick + 40;
            if (tick >= particlesStart && tick < particlesEnd && tick % 3 == 0) {
                BossUtil.malkuthSwordChargeParticles((ServerLevel)this.m_9236_(), MalkuthAttackType.FIRE, this, 60.0);
                BossUtil.malkuthSwordChargeParticles((ServerLevel)this.m_9236_(), MalkuthAttackType.ICE, this, 60.0);
                float sizeModifier = (float)(tick - particlesStart) / (float)(particlesEnd - particlesStart);
                this.doSwordChargeStripe(MalkuthAttackType.FIRE, 1.75f, sizeModifier);
                this.doSwordChargeStripe(MalkuthAttackType.ICE, 1.75f, sizeModifier);
            }
            if (this.isBelowHalfHP() && tick < swordSpawnTick + MalkuthGiantSwordSlash.TIME_TO_RISE && tick % 25 == 0) {
                this.shootCannons(3);
            }
            if (tick == swordSpawnTick - 5) {
                this.m_7618_(EntityAnchorArgument.Anchor.EYES, new Vec3(0.0, 0.0, -100.0).m_82549_(this.m_20182_()));
                Vector3f red = MalkuthEntity.getMalkuthAttackPreparationParticleColor(this.giantSwordUltimateStartAttackType);
                Vector3f blue = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.getOpposite(this.giantSwordUltimateStartAttackType));
                RectanglePreparationParticleOptions rectanglePreparationParticleOptions1 = new RectanglePreparationParticleOptions(new Vec3(0.0, 0.0, -1.0), 30.0f, 14.0f, 60, 20, 10, red.x, red.y, red.z, 0.25f);
                RectanglePreparationParticleOptions rectanglePreparationParticleOptions2 = new RectanglePreparationParticleOptions(new Vec3(0.0, 0.0, -1.0), 30.0f, 14.0f, 60, 20, 10, blue.x, blue.y, blue.z, 0.25f);
                Vec3 pos1 = new Vec3(14.0, -0.99, 0.5).m_82549_(this.m_20182_());
                Vec3 pos2 = new Vec3(-14.0, -0.99, 0.5).m_82549_(this.m_20182_());
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)rectanglePreparationParticleOptions1, (Vec3)pos1, (double)100.0);
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)rectanglePreparationParticleOptions2, (Vec3)pos2, (double)100.0);
            } else if (tick == swordSpawnTick) {
                Vec3 offs1 = new Vec3(10.0, -1.0, 11.0);
                Vec3 offs2 = new Vec3(-10.0, -1.0, 11.0);
                Vec3 pos1 = this.m_20182_().m_82549_(offs1);
                Vec3 pos2 = this.m_20182_().m_82549_(offs2);
                MalkuthGiantSwordSlash slash1 = MalkuthGiantSwordSlash.summon(this.m_9236_(), pos1, new Vec3(0.0, 0.0, -1.0), this.giantSwordUltimateStartAttackType, 0.0f);
                slash1.setDoDamage(false);
                MalkuthGiantSwordSlash slash2 = MalkuthGiantSwordSlash.summon(this.m_9236_(), pos2, new Vec3(0.0, 0.0, -1.0), MalkuthAttackType.getOpposite(this.giantSwordUltimateStartAttackType), 0.0f);
                slash2.setDoDamage(false);
            } else if (tick == MalkuthGiantSwordSlash.TIME_TO_HIT + MalkuthGiantSwordSlash.TIME_TO_RISE + swordSpawnTick - 2) {
                ImpactFrame base = new ImpactFrame(0.5f, 0.1f, 1, false);
                FDLibCalls.sendImpactFrames((ServerLevel)((ServerLevel)this.m_9236_()), (Vec3)this.m_20182_(), (float)30.0f, (ImpactFrame[])new ImpactFrame[]{base, new ImpactFrame(base).setDuration(1).setInverted(true), new ImpactFrame(base).setDuration(1), new ImpactFrame(base).setDuration(1).setInverted(true), new ImpactFrame(base).setDuration(1), new ImpactFrame(base).setDuration(1).setInverted(true), new ImpactFrame(base).setDuration(1), new ImpactFrame(base).setDuration(1).setInverted(true)});
            } else if (tick == MalkuthGiantSwordSlash.TIME_TO_HIT + MalkuthGiantSwordSlash.TIME_TO_RISE + swordSpawnTick) {
                Player player2;
                float damage;
                for (ServerPlayer entity2 : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, this.m_9236_(), this.spawnPosition.m_82520_(0.0, -5.0, 0.0), 50.0f, 50.0f)) {
                    FDPacketHandler.INSTANCE.sendTo((Object)new PlaySoundInEarsPacket(BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), 1.0f, 1.0f), entity2.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
                    FDPacketHandler.INSTANCE.sendTo((Object)new PlaySoundInEarsPacket(BossSounds.MALKUTH_SWORD_ULTIMATE_IMPACT.get(), 1.0f, 1.0f), entity2.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
                }
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().amplitude(4.0f).outTime(60).frequency(100.0f).build(), (Vec3)this.spawnPosition, (double)50.0);
                AABB firstBox = new AABB(0.0, -2.0, -29.0, 29.0, 10.0, 0.0).m_82383_(this.spawnPosition);
                AABB secondBox = new AABB(-29.0, -2.0, -29.0, 0.0, 10.0, 0.0).m_82383_(this.spawnPosition);
                DamageSource damageSource = BossDamageSources.MALKUTH_TSARS_WRATH_SOURCE;
                for (LivingEntity entity3 : this.m_9236_().m_6443_(LivingEntity.class, firstBox, entity -> !(entity instanceof MalkuthBossBuddy))) {
                    entity3.f_19802_ = 0;
                    damage = 9000000.0f;
                    if (entity3 instanceof Player && !MalkuthWeaknessHandler.isWeakTo(player2 = (Player)entity3, this.giantSwordUltimateStartAttackType)) {
                        damage = 0.1f;
                    }
                    entity3.m_6469_((DamageSource)new MalkuthDamageSource(damageSource, this.giantSwordUltimateStartAttackType, 101), damage);
                }
                for (LivingEntity entity3 : this.m_9236_().m_6443_(LivingEntity.class, secondBox, entity -> !(entity instanceof MalkuthBossBuddy))) {
                    entity3.f_19802_ = 0;
                    damage = 9000000.0f;
                    if (entity3 instanceof Player && !MalkuthWeaknessHandler.isWeakTo(player2 = (Player)entity3, MalkuthAttackType.getOpposite(this.giantSwordUltimateStartAttackType))) {
                        damage = 0.1f;
                    }
                    entity3.m_6469_((DamageSource)new MalkuthDamageSource(damageSource, MalkuthAttackType.getOpposite(this.giantSwordUltimateStartAttackType), 101), damage);
                }
                inst.nextStage();
            }
        } else if (stage == 2) {
            if (tick == 0) {
                this.getAnimationSystem().startAnimation(MAIN_LAYER, AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build());
                this.headControllerContainer.setControllersMode(HeadControllerContainer.Mode.LOOK);
                this.lookAtTarget = true;
            }
            if (tick > 80) {
                return true;
            }
        }
        return false;
    }

    private void doSwordChargeStripe(MalkuthAttackType sword, float radius, float sizeModifier) {
        this.doSwordChargeStripe(sword, radius, sizeModifier, 1.0f + this.f_19796_.m_188501_(), true);
    }

    private void doSwordChargeStripe(MalkuthAttackType sword, float radius, float sizeModifier, float circleAmount, boolean in) {
        Matrix4f swordTransform = this.getModelPartTransformation((Entity)this, MalkuthEntity.getMalkuthSwordPlaceBone(sword), SERVER_MODEL);
        Vector3f fireSwordDirection = swordTransform.transformDirection(0.0f, 1.0f, 0.0f, new Vector3f());
        Vector3f fireSwordPosition = swordTransform.transformPosition(0.0f, 0.0f, 0.0f, new Vector3f());
        float rndHeightFire = 0.5f + 2.0f * this.f_19796_.m_188501_();
        Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(sword);
        FDColor fireColorStart = new FDColor(colFire.x, colFire.y - this.f_19796_.m_188501_() * 0.1f - 0.3f, colFire.z, 0.5f);
        FDColor fireColor = new FDColor(colFire.x, colFire.y + this.f_19796_.m_188501_() * 0.1f, colFire.z, 1.0f);
        StripeParticleOptions fireOptions = StripeParticleOptions.createHorizontalCircling(fireColorStart, fireColor, new Vec3((double)fireSwordDirection.x, (double)fireSwordDirection.y, (double)fireSwordDirection.z), (float)Math.PI * 2 * this.f_19796_.m_188501_(), 0.015f + 0.06f * FDEasings.easeOut((float)sizeModifier), 10, 50, this.f_19796_.m_188501_() * 4.0f - 2.0f, radius, circleAmount, 0.5f, true, in);
        Vector3f fireStripeLocation = fireSwordPosition.add((Vector3fc)fireSwordDirection.mul(rndHeightFire, new Vector3f()));
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)fireOptions, (Vec3)new Vec3((double)fireStripeLocation.x + this.m_20185_(), (double)fireStripeLocation.y + this.m_20186_(), (double)fireStripeLocation.z + this.m_20189_()), (double)60.0);
    }

    protected boolean m_7341_(Entity p_20339_) {
        return false;
    }

    public boolean m_7998_(Entity p_21396_, boolean p_21397_) {
        return false;
    }

    public boolean m_20329_(Entity p_20330_) {
        return false;
    }

    private boolean isBelowHalfHP() {
        return this.hits <= this.maxHits / 2;
    }

    public boolean m_6469_(DamageSource src, float damage) {
        Entity entity = src.m_7639_();
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            Vec3 position = this.m_20182_();
            double dist = serverPlayer.m_20182_().m_82554_(position);
            if (dist < 5.0) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_HIT.get(), SoundSource.HOSTILE, 1.0f, 0.5f);
                FDPacketHandler.INSTANCE.sendTo((Object)new DefaultShakePacket(FDShakeData.builder().outTime(5).amplitude(0.15f).build()), serverPlayer.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
                serverPlayer.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 60, 2));
            }
        }
        if (!src.m_276093_(DamageTypes.f_286979_) && !src.m_276093_(DamageTypes.f_268724_)) {
            return false;
        }
        return super.m_6469_(src, damage);
    }

    private void causeSwordChargeParticles(MalkuthAttackType attackType) {
        FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)new MalkuthChargeSwordPacket(this, attackType));
    }

    @Nullable
    public LivingEntity m_5448_() {
        return super.m_5448_();
    }

    public void m_6710_(@Nullable LivingEntity living) {
        super.m_6710_(living);
    }

    private void changeTarget() {
        List<Player> combatants = this.getCombatants(false);
        if (combatants.isEmpty()) {
            this.oldTargetPos = null;
            this.m_6710_(null);
        } else {
            LivingEntity oldTarget = this.m_5448_();
            this.m_6710_((LivingEntity)combatants.get(this.f_19796_.m_188503_(combatants.size())));
            LivingEntity newTarget = this.m_5448_();
            if (oldTarget != newTarget) {
                this.oldTargetPos = newTarget.m_20182_();
            }
        }
    }

    private Vec3 vectorBetweenNewAndOldTargetPos() {
        LivingEntity target = this.m_5448_();
        if (target == null) {
            return null;
        }
        Vec3 oldPos = this.oldTargetPos == null ? target.m_20182_() : this.oldTargetPos;
        return target.m_20182_().m_82546_(oldPos);
    }

    private void checkTarget(LivingEntity target) {
        Player player;
        if (target.m_21224_()) {
            this.changeTarget();
            return;
        }
        if (target.m_20182_().m_82554_(this.m_20182_()) > 29.0) {
            this.changeTarget();
            return;
        }
        if (target instanceof Player && ((player = (Player)target).m_7500_() || player.m_5833_())) {
            this.changeTarget();
            return;
        }
    }

    public void m_6034_(double x, double y, double z) {
        super.m_6034_(x, y, z);
        if (this.spawnPosition == null && x != 0.0 && y != 0.0 && z != 0.0) {
            this.spawnPosition = new Vec3(x, y, z);
        }
    }

    public List<Player> getCombatants(boolean includeCreativeAndSpectator) {
        return this.m_9236_().m_6443_(Player.class, this.createEnrageRadiusAABB(this.spawnPosition), player -> includeCreativeAndSpectator || !player.m_7500_() && !player.m_5833_());
    }

    private AABB createEnrageRadiusAABB(Vec3 offset) {
        return new AABB(-29.0, -29.0, -29.0, 29.0, 29.0, 29.0).m_82383_(offset);
    }

    protected List<MalkuthCannonEntity> getMalkuthCannons() {
        return BossTargetFinder.getEntitiesInCylinder(MalkuthCannonEntity.class, this.m_9236_(), this.spawnPosition, 30.0f, 35.0f, cannon -> !cannon.isPlayerControlled());
    }

    protected List<MalkuthCannonEntity> getPlayerCannons(boolean onlyCannonsThatNeedRepair) {
        return BossTargetFinder.getEntitiesInCylinder(MalkuthCannonEntity.class, this.m_9236_(), this.spawnPosition.m_82520_(0.0, -2.0, 0.0), 30.0f, 30.0f, cannon -> cannon.isPlayerControlled() && (!onlyCannonsThatNeedRepair || cannon.requiresRepair()));
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.attackChain.save(tag);
        if (this.jumpCrushAttackMovementPath != null) {
            this.jumpCrushAttackMovementPath.autoSave("jumpCrushAttackMovementPath", tag);
        }
        if (this.jumpOnWallPath != null) {
            this.jumpOnWallPath.autoSave("jumpOnWallPath", tag);
        }
        if (this.bossSpawnerUUID != null) {
            tag.m_128362_("boss_spawner", this.bossSpawnerUUID);
        }
        this.autoSave(tag);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        if (tag.m_128441_("boss_spawner")) {
            this.bossSpawnerUUID = tag.m_128342_("boss_spawner");
        }
        this.attackChain.load(tag);
        if (tag.m_128441_("jumpCrushAttackMovementPath")) {
            this.jumpCrushAttackMovementPath = new ProjectileMovementPath();
            this.jumpCrushAttackMovementPath.autoLoad("jumpCrushAttackMovementPath", tag);
        }
        if (tag.m_128441_("jumpOnWallPath")) {
            this.jumpOnWallPath = new ProjectileMovementPath();
            this.jumpOnWallPath.autoLoad("jumpOnWallPath", tag);
        }
    }

    private void attachSwords() {
        this.attachFireSword();
        this.attachIceSword();
    }

    private void attachIceSword() {
        ModelSystem modelSystem = this.getModelSystem();
        BaseModelAttachmentData iceSword = new BaseModelAttachmentData();
        modelSystem.attachToLayer(0, "ice_sword_place", ICE_SWORD_UUID, (ModelAttachmentData)FDModelAttachmentData.create((BaseModelAttachmentData)iceSword, (FDModelInfo)BossModels.MALKUTH_SWORD.get()).texture(MALKUTH_SWORD_SOLID).renderType((FDRenderType)FDRenderTypes.ENTITY_CUTOUT_NO_CULL.get()));
        modelSystem.attachToLayer(0, "ice_sword_place", ICE_SWORD_EMISSIVE_UUID, (ModelAttachmentData)FDModelAttachmentData.create((BaseModelAttachmentData)iceSword, (FDModelInfo)BossModels.MALKUTH_SWORD.get()).texture(MALKUTH_ICE_SWORD).renderType((FDRenderType)FDRenderTypes.EYES.get()));
    }

    private void attachFireSword() {
        ModelSystem modelSystem = this.getModelSystem();
        BaseModelAttachmentData fireSword = new BaseModelAttachmentData();
        modelSystem.attachToLayer(0, "fire_sword_place", FIRE_SWORD_UUID, (ModelAttachmentData)FDModelAttachmentData.create((BaseModelAttachmentData)fireSword, (FDModelInfo)BossModels.MALKUTH_SWORD.get()).texture(MALKUTH_SWORD_SOLID).renderType((FDRenderType)FDRenderTypes.ENTITY_CUTOUT_NO_CULL.get()));
        modelSystem.attachToLayer(0, "fire_sword_place", FIRE_SWORD_EMISSIVE_UUID, (ModelAttachmentData)FDModelAttachmentData.create((BaseModelAttachmentData)fireSword, (FDModelInfo)BossModels.MALKUTH_SWORD.get()).texture(MALKUTH_FIRE_SWORD).renderType((FDRenderType)FDRenderTypes.EYES.get()));
    }

    protected void m_21229_() {
    }

    private void deattachFireSword() {
        ModelSystem modelSystem = this.getModelSystem();
        modelSystem.removeAttachment(FIRE_SWORD_UUID);
        modelSystem.removeAttachment(FIRE_SWORD_EMISSIVE_UUID);
    }

    private void deattachIceSword() {
        ModelSystem modelSystem = this.getModelSystem();
        modelSystem.removeAttachment(ICE_SWORD_UUID);
        modelSystem.removeAttachment(ICE_SWORD_EMISSIVE_UUID);
    }

    private void deattachSwords() {
        this.deattachIceSword();
        this.deattachFireSword();
    }

    public int m_21529_() {
        return 10;
    }

    public int m_8132_() {
        return super.m_8132_();
    }

    public int m_8085_() {
        return 80;
    }

    public HeadControllerContainer<MalkuthEntity> getHeadControllerContainer() {
        return this.headControllerContainer;
    }

    public void m_5997_(double p_20286_, double p_20287_, double p_20288_) {
    }

    public void m_7334_(Entity p_21294_) {
    }

    protected void m_6138_() {
    }

    public boolean m_21532_() {
        return true;
    }

    @Override
    public void setSpawnedBy(BossSpawnerEntity bossSpawnerEntity) {
        this.bossSpawnerUUID = bossSpawnerEntity.m_20148_();
    }

    @Override
    public void setSpawnPosition(Vec3 spawnPosition) {
        this.spawnPosition = spawnPosition;
    }

    @Override
    public BossSpawnerEntity getSpawner() {
        Level level;
        if (this.bossSpawnerUUID != null && (level = this.m_9236_()) instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            return (BossSpawnerEntity)level2.m_8791_(this.bossSpawnerUUID);
        }
        return null;
    }

    public boolean m_5825_() {
        return true;
    }

    public boolean m_6051_() {
        return false;
    }

    public void m_6457_(ServerPlayer player) {
        super.m_6457_(player);
        this.bossbar.addPlayer(player);
    }

    public void m_6452_(ServerPlayer player) {
        super.m_6452_(player);
        this.bossbar.removePlayer(player);
    }

    public static String getMalkuthSwordPlaceBone(MalkuthAttackType type) {
        if (type.isFire()) {
            return "fire_sword_place";
        }
        return "ice_sword_place";
    }

    @Override
    public boolean onFDDespawn() {
        BossSpawnerEntity spawner = this.getSpawner();
        if (spawner != null) {
            for (MalkuthCannonEntity cannon : this.getPlayerCannons(false)) {
                cannon.setBrokenRequiresMaterials(false);
                cannon.setBroken(false);
            }
            Level level = this.m_9236_();
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                FDMusicAreasHandler.removeArea((MinecraftServer)serverLevel.m_7654_(), (UUID)this.m_20148_(), (int)40);
            }
            spawner.setActive(true);
            return true;
        }
        return false;
    }

    static {
        FIRE_SWORD_UUID = UUID.fromString("7a6d1a24-599a-4717-baa3-42d9e3293896");
        FIRE_SWORD_EMISSIVE_UUID = UUID.fromString("cd95b81b-4a3f-4ef0-9b46-f0b3503ed7fb");
        ICE_SWORD_UUID = UUID.fromString("a46c06e3-f6af-4295-a296-20fba19ac613");
        ICE_SWORD_EMISSIVE_UUID = UUID.fromString("930a0a3b-5a47-4ebd-b614-7e42e5d0cd92");
        MALKUTH_SWORD_SOLID = FDBosses.location("textures/item/malkuth_sword_solid.png");
        MALKUTH_ICE_SWORD = FDBosses.location("textures/item/malkuth_sword_ice_emissive.png");
        MALKUTH_FIRE_SWORD = FDBosses.location("textures/item/malkuth_sword_fire_emissive.png");
        PLATFORM_SPAWN_OFFSETS = new Vec3[]{new Vec3(16.0, 5.0, -15.0), new Vec3(16.0, 5.0, -7.0), new Vec3(8.0, 5.0, -17.0), new Vec3(8.0, 5.0, -9.0), new Vec3(0.0, 5.0, -19.0), new Vec3(0.0, 5.0, -11.0), new Vec3(-8.0, 5.0, -17.0), new Vec3(-8.0, 5.0, -9.0), new Vec3(-16.0, 5.0, -15.0), new Vec3(-16.0, 5.0, -7.0)};
        PLATFORM_ATTACK_PATTERN = new int[][]{{0, 1, 1, 0, 0, 1, 1, 0, 0, 1}, {1, 1, 0, 0, 1, 1, 0, 0, 1, 1}, {0, 0, 0, 1, 1, 1, 0, 1, 0, 0}, {1, 0, 0, 1, 1, 0, 0, 1, 1, 0}, {0, 0, 1, 1, 0, 0, 1, 1, 0, 0}, {1, 1, 1, 0, 0, 0, 1, 0, 1, 1}};
    }

    @Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {
        public static final int MARK_OF_A_COWARD_DURATION = 60;

        @SubscribeEvent
        public static void ignoreTotems(LivingUseTotemEvent event) {
            LivingEntity livingEntity = event.getEntity();
            if (!(livingEntity.m_9236_().f_46443_ || event.getSource() != BossDamageSources.MALKUTH_COWARDICE_SOURCE && event.getSource() != BossDamageSources.GEBURAH_SINNED_TOO_MUCH_SOURCE)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onCowardice(MobEffectEvent.Expired event) {
            MobEffectInstance instance = event.getEffectInstance();
            MobEffect effect = instance.m_19544_();
            LivingEntity entity = event.getEntity();
            if (!entity.m_9236_().f_46443_ && effect.equals(BossEffects.MARK_OF_A_COWARD.get())) {
                entity.m_6469_(BossDamageSources.MALKUTH_COWARDICE_SOURCE, 9000000.0f);
            }
        }

        @SubscribeEvent
        public static void livingTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Player entity = event.player;
            Level level = entity.m_9236_();
            if (!level.f_46443_ && entity.m_21023_((MobEffect)BossEffects.MARK_OF_A_KNIGHT.get())) {
                Vec3 cylinderStart = entity.m_20182_().m_82520_(0.0, -11.0, 0.0);
                List<MalkuthBossSpawner> spawners = BossTargetFinder.getEntitiesInArc(MalkuthBossSpawner.class, level, cylinderStart, new Vec2(0.0f, 1.0f), (float)Math.PI, 13.0f, 29.0f);
                if (spawners.isEmpty()) {
                    entity.m_21195_((MobEffect)BossEffects.MARK_OF_A_KNIGHT.get());
                    if (!entity.m_21023_((MobEffect)BossEffects.MARK_OF_A_COWARD.get())) {
                        entity.m_7292_(new MobEffectInstance((MobEffect)BossEffects.MARK_OF_A_COWARD.get(), 60, 0, true, false));
                    }
                } else {
                    entity.m_21195_((MobEffect)BossEffects.MARK_OF_A_COWARD.get());
                }
            }
        }

        @SubscribeEvent
        public static void livingHurtEvent(LivingHurtEvent event) {
            LivingEntity living = event.getEntity();
            Level level = living.m_9236_();
            if (!level.f_46443_ && living instanceof Player) {
                Player player = (Player)living;
                DamageSource damageSource = event.getSource();
                if (damageSource instanceof MalkuthDamageSource) {
                    MalkuthDamageSource damageSource2 = (MalkuthDamageSource)damageSource;
                    int malkuthDamageAmount = damageSource2.getMalkuthAttackAmount();
                    MalkuthAttackType malkuthAttackType = damageSource2.getMalkuthAttackType();
                    if (!MalkuthWeaknessHandler.isWeakTo(player, malkuthAttackType)) {
                        float damage = event.getAmount();
                        float reduction = 1.0f - BossConfigs.BOSS_CONFIG.get().malkuthConfig.nonWeakToDamageReduction / 100.0f;
                        event.setAmount(damage * reduction);
                    }
                    MalkuthWeaknessHandler.damageWeakness(malkuthAttackType, player, malkuthDamageAmount);
                }
            }
        }
    }
}

