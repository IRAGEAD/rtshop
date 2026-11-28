/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.FDHelpers
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.init.FDScreenEffects
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.network.lib_packets.PlaySoundInEarsPacket
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDMob
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.cutscenes.CameraPos
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData$StopMode
 *  com.finderfeed.fdlib.systems.cutscenes.EasingType
 *  com.finderfeed.fdlib.systems.cutscenes.packets.StartCutscenePacket
 *  com.finderfeed.fdlib.systems.cutscenes.packets.StopCutscenePacket
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackAction
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackChain
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackInstance
 *  com.finderfeed.fdlib.systems.entity.action_chain.AttackOptions
 *  com.finderfeed.fdlib.systems.hud.bossbars.FDServerBossBar
 *  com.finderfeed.fdlib.systems.impact_frames.ImpactFrame
 *  com.finderfeed.fdlib.systems.particle.CircleParticleProcessor
 *  com.finderfeed.fdlib.systems.particle.CompositeParticleProcessor
 *  com.finderfeed.fdlib.systems.particle.ParticleProcessor
 *  com.finderfeed.fdlib.systems.particle.SetParticleSpeedProcessor
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.EmitterProcessor
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterData
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.processors.BoundToEntityProcessor
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectType
 *  com.finderfeed.fdlib.systems.screen.screen_effect.SendScreenEffectPacket
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData
 *  com.finderfeed.fdlib.systems.shake.DefaultShakePacket
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
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
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkDirection
 *  net.minecraftforge.network.PacketDistributor
 *  org.joml.Math
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fStack
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.joml.Vector4f
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningOptions;
import com.finderfeed.fdbosses.client.particles.chesed_attack_ray.ChesedRayOptions;
import com.finderfeed.fdbosses.client.particles.particle_processors.ChesedRayCircleParticleProcessor;
import com.finderfeed.fdbosses.client.particles.rush_particle.RushParticleOptions;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.client.particles.sonic_particle.SonicParticleOptions;
import com.finderfeed.fdbosses.content.entities.BossDespawner;
import com.finderfeed.fdbosses.content.entities.BossInitializer;
import com.finderfeed.fdbosses.content.entities.FDDespawnable;
import com.finderfeed.fdbosses.content.entities.IEffectImmune;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerContextAssignable;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossInitializer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedFireTrailEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_crystal.ChesedCrystalEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_monolith.ChesedMonolith;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_one_shot_vertical_ray.ChesedOneShotVerticalRayEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_vertical_ray.ChesedMovingVerticalRay;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterSettings;
import com.finderfeed.fdbosses.content.entities.chesed_boss.electric_sphere.ChesedElectricSphereEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.falling_block.ChesedFallingBlock;
import com.finderfeed.fdbosses.content.entities.chesed_boss.kinetic_field.ChesedKineticFieldEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.radial_earthquake.RadialEarthquakeEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ray_reflector.ChesedRayReflector;
import com.finderfeed.fdbosses.content.projectiles.ChesedBlockProjectile;
import com.finderfeed.fdbosses.content.util.DelayedSound;
import com.finderfeed.fdbosses.content.util.RepeatedSound;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossBars;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossCriteriaTriggers;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.packets.ChesedRayReflectPacket;
import com.finderfeed.fdbosses.packets.PosLevelEventPacket;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.FDHelpers;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.init.FDScreenEffects;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.network.lib_packets.PlaySoundInEarsPacket;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDMob;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.cutscenes.CameraPos;
import com.finderfeed.fdlib.systems.cutscenes.CutsceneData;
import com.finderfeed.fdlib.systems.cutscenes.EasingType;
import com.finderfeed.fdlib.systems.cutscenes.packets.StartCutscenePacket;
import com.finderfeed.fdlib.systems.cutscenes.packets.StopCutscenePacket;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackAction;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackChain;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackInstance;
import com.finderfeed.fdlib.systems.entity.action_chain.AttackOptions;
import com.finderfeed.fdlib.systems.hud.bossbars.FDServerBossBar;
import com.finderfeed.fdlib.systems.impact_frames.ImpactFrame;
import com.finderfeed.fdlib.systems.particle.CircleParticleProcessor;
import com.finderfeed.fdlib.systems.particle.CompositeParticleProcessor;
import com.finderfeed.fdlib.systems.particle.ParticleProcessor;
import com.finderfeed.fdlib.systems.particle.SetParticleSpeedProcessor;
import com.finderfeed.fdlib.systems.particle.particle_emitter.EmitterProcessor;
import com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterData;
import com.finderfeed.fdlib.systems.particle.particle_emitter.processors.BoundToEntityProcessor;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectType;
import com.finderfeed.fdlib.systems.screen.screen_effect.SendScreenEffectPacket;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData;
import com.finderfeed.fdlib.systems.shake.DefaultShakePacket;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public class ChesedEntity
extends FDMob
implements ChesedBossBuddy,
BossSpawnerContextAssignable,
IEffectImmune,
FDDespawnable {
    public static final String ROCKFALL_TICKER = "ROCKFALL";
    public static final String RAY_ATTACK_LAYER = "ATTACK";
    public static final String FINAL_ATTACK_LAYER = "BOOM";
    public static final String EARTHQUAKE_ATTACK_LAYER = "EARTHQUAKE";
    public static int ARENA_HEIGHT = 40;
    public static int ARENA_RADIUS = 39;
    private UUID bossSpawnerUUID;
    public static final String FINAL_ATTACK = "final";
    public static final String CRYSTALS_ATTACK = "crystals";
    public static final String RAY_ATTACK = "ray";
    public static final String BLOCKS_ATTACK = "blocks";
    public static final String ROLL_ATTACK = "roll";
    public static final String EARTHQUAKE_ATTACK = "equake";
    public static final String ROCKFALL_ATTACK = "rockfall";
    public static final String ELECTRIC_SPHERE_ATTACK = "esphere";
    public static final String RAY_EVASION_ATTACK = "ray_evasion";
    private static final Vec3[] MONOLITH_SPAWN_OFFSETS = new Vec3[]{new Vec3(10.0, 0.0, 10.0), new Vec3(-10.0, 0.0, 10.0), new Vec3(10.0, 0.0, -10.0), new Vec3(-10.0, 0.0, -10.0)};
    public static final EntityDataAccessor<Boolean> IS_ROLLING = SynchedEntityData.m_135353_(ChesedEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    public static final EntityDataAccessor<Boolean> IS_LAUNCHING_ORBS = SynchedEntityData.m_135353_(ChesedEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    public static final EntityDataAccessor<Boolean> IS_DRAINING_MONOLITHS = SynchedEntityData.m_135353_(ChesedEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    public static final EntityDataAccessor<Float> DRAIN_PERCENT = SynchedEntityData.m_135353_(ChesedEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    public FDServerBossBar bossBar = new FDServerBossBar(BossBars.CHESED_BOSS_BAR, (Entity)this);
    public AttackChain chain;
    protected static FDModel serverModel;
    protected static FDModel clientModel;
    private Vec3 oldRollPos;
    private boolean playIdle = true;
    private boolean lookingAtTarget = true;
    public float drainPercentOld;
    private LivingEntity target;
    private Vec3 previousTargetPos;
    private boolean doBlinding = true;
    private int remainingHits = 10;
    private boolean alreadySpawned = false;
    private BossInitializer<ChesedEntity> bossInitializer = new ChesedBossInitializer(this);
    private List<DelayedSound> delayedSounds = new ArrayList<DelayedSound>();
    private RepeatedSound repeatedSound = null;
    private boolean dropLoot = true;
    private int secondPhaseTicker = 0;
    private boolean secondPhaseAnimPlayed = false;
    private int skipAttackTimes = 0;
    private BossDespawner<ChesedEntity> bossDespawner;
    @SerializableField
    private Vec3 spawnPosition;
    public static final int FINAL_ATTACK_RAY_ROTATION_DURATION = 15;
    private List<ChesedBlockProjectile> blockAttackProjectiles = new ArrayList<ChesedBlockProjectile>();

    public ChesedEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        if (serverModel == null) {
            serverModel = new FDModel(BossModels.CHESED.get());
        }
        if (clientModel == null) {
            clientModel = new FDModel(BossModels.CHESED.get());
        }
        this.remainingHits = BossConfigs.BOSS_CONFIG.get().chesedConfig.chesedMaxHits;
        if (!level.f_46443_) {
            AttackOptions ray = AttackOptions.builder().setPreAttack(CRYSTALS_ATTACK).addAttack(RAY_ATTACK).build();
            AttackOptions rayOrBlocks = AttackOptions.builder().addAttack(3, ray).addAttack(2, BLOCKS_ATTACK).build();
            this.chain = new AttackChain(level.f_46441_).registerAttack("nothing", this::doNothing).registerAttack(FINAL_ATTACK, this::finalBOOMAttack).registerAttack(CRYSTALS_ATTACK, this::summonCrystals).registerAttack(RAY_ATTACK, this::rayAttack).registerAttack(BLOCKS_ATTACK, this::blockAttack).registerAttack(ROLL_ATTACK, this::roll).registerAttack(EARTHQUAKE_ATTACK, this::earthquakeAttack).registerAttack(ROCKFALL_ATTACK, this::rockfallAttack).registerAttack(ELECTRIC_SPHERE_ATTACK, this::electricSphereAttack).registerAttack(RAY_EVASION_ATTACK, this::rayEvasionAttack).attackListener(this::attackListener).addAttack(0, ray).addAttack(1, AttackOptions.builder().addAttack(ELECTRIC_SPHERE_ATTACK).setNextAttack(rayOrBlocks).build()).addAttack(1, AttackOptions.builder().addAttack(ROCKFALL_ATTACK).setNextAttack(rayOrBlocks).build()).addAttack(1, AttackOptions.builder().addAttack(EARTHQUAKE_ATTACK).setNextAttack(rayOrBlocks).build()).addAttack(4, AttackOptions.builder().addAttack(ROLL_ATTACK).build()).addAttack(5, RAY_EVASION_ATTACK).addAttack(6, FINAL_ATTACK);
            this.bossDespawner = new BossDespawner<ChesedEntity>(this, new AABB((double)(-ARENA_RADIUS), -5.0, (double)(-ARENA_RADIUS), (double)ARENA_RADIUS, (double)ARENA_HEIGHT, (double)ARENA_RADIUS), 30, ChesedCrystalEntity.class, ChesedMonolith.class, ChesedBlockProjectile.class, ChesedFallingBlock.class, ChesedKineticFieldEntity.class, ChesedElectricSphereEntity.class);
        }
    }

    public boolean m_6469_(DamageSource src, float damage) {
        Vec3 between;
        double distance;
        LivingEntity livingEntity;
        Entity entity;
        if (!this.m_9236_().f_46443_ && (entity = src.m_7639_()) instanceof LivingEntity && !((livingEntity = (LivingEntity)entity) instanceof ChesedEntity) && !this.isRolling() && (distance = (between = livingEntity.m_20182_().m_82546_(this.m_20182_())).m_82553_()) < 10.0) {
            between = between.m_82541_();
            Vec3 speed = new Vec3(between.f_82479_ * 10.0, between.f_82480_ + 0.5, between.f_82481_ * 10.0);
            if (livingEntity instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)livingEntity;
                FDLibCalls.setServerPlayerSpeed((ServerPlayer)serverPlayer, (Vec3)speed);
            } else {
                livingEntity.m_20334_(between.f_82479_ * 10.0, between.f_82480_ + 0.5, between.f_82481_ * 10.0);
                livingEntity.f_19812_ = true;
            }
            livingEntity.m_6469_(BossDamageSources.chesedAttack((Entity)this), 2.0f);
            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.FAST_LIGHTNING_STRIKE.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(25.0f).scalingOptions(0, 0, 2).color(150, 230, 255).build(), (Vec3)this.m_20182_().m_82549_(between.m_82542_(2.0, 2.0, 2.0)), (double)100.0);
        }
        if (!src.m_276093_(DamageTypes.f_286979_) && !src.m_276093_(DamageTypes.f_268724_)) {
            return false;
        }
        return super.m_6469_(src, damage);
    }

    public void m_8119_() {
        if (!this.isRolling() && !this.m_9236_().f_46443_) {
            this.m_146922_(this.f_20885_);
        }
        this.drainPercentOld = this.getMonolithDrainPercent();
        super.m_8119_();
        AnimationSystem system = this.getAnimationSystem();
        system.setVariable("variable.radius", 580.0f);
        system.setVariable("variable.appear_height", 400.0f);
        system.setVariable("variable.angle", 360.0f);
        if (!this.m_9236_().f_46443_) {
            this.tickTeleportHome();
            this.passiveEntityPullInArena();
            this.bossBar.setPercentage((float)this.getRemainingHits() / (float)this.getBossMaxHits());
            this.bossInitializer.tick();
            this.tickDelayedSounds();
            if (this.f_19797_ == 5 && !this.alreadySpawned) {
                this.alreadySpawned = true;
                this.summonOrReviveMonoliths();
            }
            if (this.playIdle) {
                system.startAnimation("IDLE", AnimationTicker.builder(BossAnims.CHESED_IDLE).build());
            } else {
                system.stopAnimation("IDLE");
            }
            if (this.m_9236_().m_46467_() % 5L == 0L) {
                this.blindCombatants();
            }
            if (this.bossInitializer.isFinished() && !this.m_21224_() && this.secondPhaseTicker <= 0) {
                this.chain.tick();
                this.electrifiedAir();
                if (this.repeatedSound != null && this.repeatedSound.tick(this.m_9236_())) {
                    if (this.repeatedSound.sound.soundEvent == BossSounds.ELECTRIC_SPHERES_STORM.get()) {
                        this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.ELECTRIC_SPHERES_STORM_END.get(), SoundSource.HOSTILE, this.repeatedSound.sound.volume, this.repeatedSound.sound.pitch);
                    }
                    this.repeatedSound = null;
                }
                if (this.m_5448_() != null) {
                    this.checkTarget(this.m_5448_());
                    if (this.m_5448_() != null) {
                        if (this.lookingAtTarget) {
                            this.lookAtTarget(this.m_5448_());
                        } else {
                            this.m_7618_(EntityAnchorArgument.Anchor.EYES, this.m_146892_().m_82549_(this.m_20154_()));
                        }
                        this.previousTargetPos = this.target.m_20182_();
                    }
                } else if (this.m_9236_().m_46467_() % 20L == 0L) {
                    this.changeTarget();
                }
                this.bossDespawner.tick();
            } else {
                this.setRolling(false);
                this.setDrainingMonoliths(false);
            }
            this.tickSecondPhaseStart();
        } else {
            this.clientBoomAttackRotatingRay();
            if (this.isDrainingFromMonoliths()) {
                this.monolithEnergyDrainParticles();
            }
            this.playIdleSound();
            if (this.isRolling()) {
                this.handleClientRolling();
            } else {
                AnimationTicker ticker = this.getAnimationSystem().getTicker("APPEAR");
                if (ticker != null) {
                    if (this.f_19797_ > 60) {
                        this.idleParticles();
                    }
                } else {
                    this.idleParticles();
                }
            }
        }
    }

    private void playIdleSound() {
        if (this.m_9236_().m_46467_() % 28L == 0L) {
            this.m_9236_().m_6263_(FDClientHelpers.getClientPlayer(), this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.CHESED_IDLE.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
        }
    }

    private void addMonolithHP() {
        List<ChesedMonolith> monoliths = this.getMonoliths();
        float monolithHPGain = BossConfigs.BOSS_CONFIG.get().chesedConfig.additionalSecondPhaseMonolithHP;
        for (ChesedMonolith m : monoliths) {
            AttributeModifier hpModifier = new AttributeModifier(FDBosses.location("monolith_hp").toString(), (double)monolithHPGain, AttributeModifier.Operation.ADDITION);
            AttributeInstance attr = m.m_21051_(Attributes.f_22276_);
            if (attr != null) {
                attr.m_22125_(hpModifier);
            }
            m.m_21153_(m.m_21233_());
            m.setDeactivated(false);
        }
    }

    private void tickSecondPhaseStart() {
        if (!this.bossInitializer.isFinished() || this.f_19797_ < 250) {
            return;
        }
        int tickerTime = 210;
        if (!this.secondPhaseAnimPlayed && this.isBelowHalfHP()) {
            this.secondPhaseTicker = tickerTime;
            this.secondPhaseAnimPlayed = true;
            this.lookingAtTarget = false;
            this.killCrystals();
            this.setRolling(false);
            this.f_19804_.m_135381_(IS_LAUNCHING_ORBS, (Object)false);
            this.chain.reset();
            this.addMonolithHP();
        }
        if (this.secondPhaseTicker <= 0) {
            return;
        }
        Vec3 hlook = this.m_20154_().m_82542_(1.0, 0.0, 1.0).m_82541_();
        int fullOffset = 20;
        int firstRayOffset = 39;
        int perRayOffset = 11;
        float distmult = 37.0f;
        float heighton = 23.0f;
        if (this.secondPhaseTicker == tickerTime) {
            this.stopAllNonIdleAnimations();
            this.getAnimationSystem().startAnimation("PREPARE_SECOND_PHASE", AnimationTicker.builder(BossAnims.CHESED_SECOND_PHASE_PREPARE).setToNullTransitionTime(0).startTime(2.0f).setSpeed(0.75f).build());
            this.sendPacketToCombatants(new SendScreenEffectPacket((ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), (ScreenEffectType)FDScreenEffects.SCREEN_COLOR.get(), 0, fullOffset, 20));
        } else if (this.secondPhaseTicker <= tickerTime - fullOffset && this.secondPhaseTicker >= tickerTime - fullOffset - 10) {
            int count = 3;
            float angle = (float)Math.PI * 2 / (float)count;
            Vec3 center = this.m_20182_();
            for (int i = 0; i < count; ++i) {
                float a = angle * (float)i;
                Vec3 v = new Vec3(3.0, 0.0, 0.0).m_82524_(a);
                Vec3 ppos = this.m_20182_().m_82549_(v);
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(0.5f).color(100 + this.f_19796_.m_188503_(50), 255, 255).particleProcessor((ParticleProcessor)new CompositeParticleProcessor(new ParticleProcessor[]{new CircleParticleProcessor(center, true, true, 1.0f), new SetParticleSpeedProcessor(new Vec3(0.0, 0.075, 0.0))})).scalingOptions(10, 10, 0).build(), (Vec3)ppos, (double)(ARENA_RADIUS * 2));
            }
            if (this.secondPhaseTicker == tickerTime - fullOffset) {
                Vec3 pos = this.m_20182_().m_82520_(0.0, 3.0, 0.0).m_82549_(hlook.m_82542_(4.0, 0.0, 4.0));
                this.sendPacketToCombatants(new DefaultShakePacket(FDShakeData.builder().stayTime(30).inTime(2).outTime(0).amplitude(0.2f).build()));
                this.getAnimationSystem().startAnimation("PREPARE_SECOND_PHASE", AnimationTicker.builder(BossAnims.CHESED_SECOND_PHASE_PREPARE).setToNullTransitionTime(0).build());
                CutsceneData cutsceneData = new CutsceneData().time(60).timeEasing(EasingType.EASE_IN_OUT).stopMode(CutsceneData.StopMode.UNSTOPPABLE).addCameraPos(new CameraPos(pos, hlook.m_82548_().m_82520_(0.0, -0.3, 0.0))).addCameraPos(new CameraPos(pos.m_82549_(hlook.m_82542_(20.0, 0.0, 20.0)), hlook.m_82548_().m_82520_(0.0, 0.1, 0.0)));
                this.sendPacketToCombatants(new StartCutscenePacket(cutsceneData));
            }
        } else if (this.secondPhaseTicker == tickerTime - 30 - fullOffset) {
            this.getAnimationSystem().startAnimation("SECOND_PHASE_START", AnimationTicker.builder(BossAnims.CHESED_SECOND_PHASE_STRIKE_WALLS).setToNullTransitionTime(0).build());
        } else if (this.secondPhaseTicker == tickerTime - firstRayOffset - fullOffset) {
            Vec3 v = hlook.m_82524_(2.3561945f).m_82542_((double)distmult, 0.0, (double)distmult).m_82520_(0.0, (double)heighton, 0.0);
            this.decorativeRay(v.m_82549_(this.m_20182_()));
            this.sendPacketToCombatants(new DefaultShakePacket(FDShakeData.builder().stayTime(perRayOffset - 2).inTime(2).outTime(0).amplitude(0.2f).build()));
        } else if (this.secondPhaseTicker == tickerTime - firstRayOffset - perRayOffset - fullOffset) {
            Vec3 v = hlook.m_82524_(3.926991f).m_82542_((double)distmult, 0.0, (double)distmult).m_82520_(0.0, (double)heighton, 0.0);
            this.decorativeRay(v.m_82549_(this.m_20182_()));
            this.sendPacketToCombatants(new DefaultShakePacket(FDShakeData.builder().stayTime(perRayOffset - 2).inTime(0).outTime(0).amplitude(0.5f).build()));
        } else if (this.secondPhaseTicker == tickerTime - firstRayOffset - perRayOffset * 2 - fullOffset) {
            Vec3 v = hlook.m_82524_(5.497787f).m_82542_((double)distmult, 0.0, (double)distmult).m_82520_(0.0, (double)heighton, 0.0);
            this.decorativeRay(v.m_82549_(this.m_20182_()));
            this.sendPacketToCombatants(new DefaultShakePacket(FDShakeData.builder().stayTime(perRayOffset - 2).inTime(0).outTime(0).amplitude(0.75f).build()));
        } else if (this.secondPhaseTicker == tickerTime - firstRayOffset - perRayOffset * 3 - fullOffset) {
            Vec3 v = hlook.m_82524_(0.7853982f).m_82542_((double)distmult, 0.0, (double)distmult).m_82520_(0.0, (double)heighton, 0.0);
            this.decorativeRay(v.m_82549_(this.m_20182_()));
            this.sendPacketToCombatants(new DefaultShakePacket(FDShakeData.builder().stayTime(40).inTime(0).outTime(0).amplitude(0.9f).build()));
            this.sendPacketToCombatants(new SendScreenEffectPacket((ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), (ScreenEffectType)FDScreenEffects.SCREEN_COLOR.get(), 5, 80, 20));
        } else if (this.secondPhaseTicker == 40) {
            this.lookingAtTarget = true;
            this.sendPacketToCombatants(new StopCutscenePacket());
            float rh = BossConfigs.BOSS_CONFIG.get().chesedConfig.healthAfterSecondPhaseTransition;
            for (Player player : this.getCombatants(false)) {
                float health = player.m_21223_();
                if (!(health > rh)) continue;
                player.m_21153_(rh);
            }
            this.skipAttackTimes = 2;
            this.chain.reset();
        }
        this.secondPhaseTicker = Mth.m_14045_((int)(this.secondPhaseTicker - 1), (int)0, (int)Integer.MAX_VALUE);
    }

    private void decorativeRay(Vec3 end) {
        Vec3 start = this.m_20182_().m_82520_(0.0, 1.5, 0.0);
        Vec3 look = start.m_82546_(end).m_82541_();
        ChesedRayOptions options = ChesedRayOptions.builder().time(2, 2, 5).lightningColor(90, 180, 255).color(100, 255, 255).end(end).width(0.8f).build();
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)start, (double)120.0);
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(25.0f).scalingOptions(1, 0, 2).color(150, 230, 255).build(), (Vec3)end, (double)200.0);
        BossUtil.chesedRaySmoke((ServerLevel)this.m_9236_(), end, look, 120.0);
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).amplitude(5.0f).inTime(0).stayTime(0).outTime(5).build(), (Vec3)end, (double)120.0);
        BossUtil.chesedRayExplosion((ServerLevel)this.m_9236_(), end, look, 100.0, 15, 0.75f);
        this.summonStonesAfterRayAttack(30, look.m_82542_(1.0, 0.0, 1.0).m_82541_(), end);
        ((ServerLevel)this.m_9236_()).m_6263_(null, end.f_82479_, end.f_82480_, end.f_82481_, BossSounds.CHESED_RAY.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
    }

    private void electrifiedAir() {
        for (LivingEntity entity : this.getAffectedEntities(false)) {
            double y = this.m_20186_();
            double ey = entity.m_20186_();
            if (!(ey - y > 3.0)) continue;
            if (entity.f_19797_ % 30 == 0) {
                float damagePercent = BossConfigs.BOSS_CONFIG.get().chesedConfig.electrifiedAirCurrentHealthDamagePercent;
                float instadeathHP = BossConfigs.BOSS_CONFIG.get().chesedConfig.electrifiedAirInstadeathHP;
                float health = entity.m_21223_();
                float damage = health <= instadeathHP ? 2.1474836E9f : health * (damagePercent / 100.0f);
                entity.m_6469_(BossDamageSources.chesedAttack((Entity)this), damage);
            }
            if (this.f_19797_ % 5 != 0) continue;
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)LightningParticleOptions.builder().color(50 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 200 + this.f_19796_.m_188503_(50)).quadSize(0.1f).lifetime(10).randomRoll(true).build(), entity.m_20185_(), entity.m_20186_() + (double)(entity.m_20206_() / 2.0f), entity.m_20189_(), 4, (double)(entity.m_20205_() / 2.0f), 0.5, (double)(entity.m_20205_() / 2.0f), 0.0);
        }
    }

    private void tickDelayedSounds() {
        if (!this.m_9236_().f_46443_) {
            ListIterator<DelayedSound> iterator = this.delayedSounds.listIterator();
            while (iterator.hasNext()) {
                DelayedSound s = iterator.next();
                if (!s.tick()) continue;
                this.m_9236_().m_6263_(null, s.pos.f_82479_, s.pos.f_82480_, s.pos.f_82481_, s.sound.soundEvent, s.sound.soundSource, s.sound.volume, s.sound.pitch);
                iterator.remove();
            }
        }
    }

    public AttackAction attackListener(String attackName) {
        if (this.m_5448_() == null) {
            return AttackAction.WAIT;
        }
        if (this.skipAttackTimes > 0) {
            --this.skipAttackTimes;
            return AttackAction.SKIP;
        }
        if (!this.isBelowHalfHP() && attackName.equals(RAY_EVASION_ATTACK)) {
            return AttackAction.SKIP;
        }
        this.doBlinding = true;
        return AttackAction.PROCEED;
    }

    public List<ChesedMonolith> getMonoliths() {
        AABB box = new AABB(-30.0, -10.0, -30.0, 30.0, 10.0, 30.0).m_82383_(this.m_20182_());
        List list = this.m_9236_().m_45976_(ChesedMonolith.class, box);
        return list;
    }

    public void setMonolithsImmunity(boolean immune) {
        List<ChesedMonolith> monoliths = this.getMonoliths();
        for (ChesedMonolith monolith : monoliths) {
            monolith.setImmuneToAttacks(immune);
        }
    }

    public void summonOrReviveMonoliths() {
        List<ChesedMonolith> list = this.getMonoliths();
        if (list.size() != 4) {
            for (ChesedMonolith monolith : list) {
                monolith.m_142687_(Entity.RemovalReason.DISCARDED);
            }
            this.summonMonoliths();
        } else {
            for (ChesedMonolith monolith : list) {
                monolith.setDeactivated(false);
            }
        }
    }

    private void removeMonoliths() {
        AABB box = new AABB(-30.0, -10.0, -30.0, 30.0, 10.0, 30.0).m_82383_(this.m_20182_());
        List list = this.m_9236_().m_45976_(ChesedMonolith.class, box);
        for (ChesedMonolith monolith : list) {
            monolith.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    private void summonMonoliths() {
        for (Vec3 pos : MONOLITH_SPAWN_OFFSETS) {
            ChesedMonolith monolith = new ChesedMonolith(BossEntities.CHESED_MONOLITH.get(), this.m_9236_());
            monolith.m_146884_(pos.m_82549_(this.m_20182_()));
            this.m_9236_().m_7967_((Entity)monolith);
        }
    }

    public int getAliveMonolithsCount() {
        List<ChesedMonolith> monoliths = this.getMonoliths();
        if (monoliths.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ChesedMonolith monolith : monoliths) {
            if (monolith.isDeactivated()) continue;
            ++count;
        }
        return count;
    }

    public List<LivingEntity> getAffectedEntities(boolean includeCreativeAndSpectator) {
        float radius = this.targetDetectionRadius();
        List combatants = this.m_9236_().m_6443_(LivingEntity.class, new AABB((double)(-radius), -2.0, (double)(-radius), (double)radius, (double)ARENA_HEIGHT, (double)radius).m_82383_(this.m_20182_()), entity -> {
            boolean result;
            if (entity instanceof ChesedBossBuddy) {
                return false;
            }
            boolean bl = result = entity.m_20182_().m_82542_(1.0, 0.0, 1.0).m_82554_(this.m_20182_().m_82542_(1.0, 0.0, 1.0)) <= (double)radius;
            if (entity instanceof Player) {
                Player player = (Player)entity;
                result = result && (includeCreativeAndSpectator || !player.m_7500_() && !player.m_5833_());
            }
            return result;
        });
        return combatants;
    }

    public List<Player> getCombatants(boolean includeCreativeAndSpectator) {
        return this.getCombatants(includeCreativeAndSpectator, 0.0f);
    }

    public List<Player> getCombatants(boolean includeCreativeAndSpectator, float additionalRadius) {
        float radius = this.targetDetectionRadius() + additionalRadius;
        List combatants = this.m_9236_().m_6443_(Player.class, new AABB((double)(-radius), -1.0, (double)(-radius), (double)radius, (double)ARENA_HEIGHT, (double)radius).m_82383_(this.m_20182_()), player -> player.m_20182_().m_82542_(1.0, 0.0, 1.0).m_82554_(this.m_20182_().m_82542_(1.0, 0.0, 1.0)) <= (double)radius && (includeCreativeAndSpectator || !player.m_7500_() && !player.m_5833_()) && player.m_6084_());
        return combatants;
    }

    private void checkTarget(LivingEntity target) {
        Player player;
        if (target.m_21224_()) {
            this.changeTarget();
            return;
        }
        if (target.m_20182_().m_82554_(this.m_20182_()) > (double)this.targetDetectionRadius()) {
            this.changeTarget();
            return;
        }
        if (target instanceof Player && ((player = (Player)target).m_7500_() || player.m_5833_())) {
            this.changeTarget();
            return;
        }
    }

    private void changeTarget() {
        List<Player> combatants = this.getCombatants(false);
        if (combatants.isEmpty()) {
            this.m_6710_(null);
        } else {
            this.m_6710_((LivingEntity)combatants.get(this.f_19796_.m_188503_(combatants.size())));
        }
    }

    public LivingEntity m_5448_() {
        return this.target;
    }

    public void m_6710_(LivingEntity target) {
        this.target = target;
        if (target != null) {
            this.previousTargetPos = target.m_20182_();
        }
    }

    private void blindCombatants() {
        List<Player> combatants = this.getCombatants(true);
        for (Player player : combatants) {
            Vec3 lookAngle = player.m_20154_();
            Vec3 b = player.m_20182_().m_82546_(this.m_20182_()).m_82541_();
            player.m_7292_(new MobEffectInstance(MobEffects.f_19611_, 400, 0, true, false));
            if (!player.m_7500_() && !player.m_5833_()) {
                if (this.doBlinding) {
                    if (lookAngle.m_82526_(b) > 0.05 && !player.m_21023_((MobEffect)BossEffects.CHESED_ENERGIZED.get())) {
                        player.m_7292_(new MobEffectInstance((MobEffect)BossEffects.CHESED_GAZE.get(), 200, 0, true, true));
                        continue;
                    }
                    player.m_21195_((MobEffect)BossEffects.CHESED_GAZE.get());
                    continue;
                }
                player.m_21195_((MobEffect)BossEffects.CHESED_GAZE.get());
                continue;
            }
            player.m_21195_((MobEffect)BossEffects.CHESED_GAZE.get());
        }
    }

    private void idleParticles() {
        if (this.f_20919_ > ((Animation)BossAnims.CHESED_DEATH.get()).getAnimTime() - 10) {
            return;
        }
        if (this.m_9236_().m_46467_() % 2L == 0L) {
            float rad = 2.0f;
            if (((Boolean)this.f_19804_.m_135370_(IS_LAUNCHING_ORBS)).booleanValue()) {
                rad = 5.0f;
            }
            Vector3f bonePos = this.getModelPartPosition((Entity)this, "core", clientModel);
            Vec3 basePos = new Vec3((double)bonePos.x, (double)bonePos.y, (double)bonePos.z).m_82520_(this.m_20185_(), this.m_20186_(), this.m_20189_());
            float baseAngle = -org.joml.Math.toRadians((float)this.f_20883_) + 0.7853982f;
            float randomRange = 4.712389f;
            for (int i = 0; i < 2; ++i) {
                Vector3f endOffset = new Vector3f(0.0f, 0.0f, rad).rotateY(baseAngle + randomRange * this.f_19796_.m_188501_()).add(0.0f, -bonePos.y, 0.0f);
                Vec3 end = basePos.m_82520_((double)endOffset.x, (double)endOffset.y, (double)endOffset.z);
                this.m_9236_().m_6493_((ParticleOptions)ArcLightningOptions.builder(BossParticles.ARC_LIGHTNING.get()).end(end.f_82479_, end.f_82480_, end.f_82481_).lifetime(2).color(1 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 165 + this.f_19796_.m_188503_(60)).lightningSpread(0.25f).width(0.1f).segments(6).circleOffset(this.f_19796_.m_188501_() * 2.0f - 2.0f).build(), true, basePos.f_82479_, basePos.f_82480_, basePos.f_82481_, 0.0, 0.0, 0.0);
            }
        }
        if (((Boolean)this.f_19804_.m_135370_(IS_LAUNCHING_ORBS)).booleanValue()) {
            for (int i = 0; i <= 30; ++i) {
                this.summonOrbLaunchingParticle();
            }
        }
    }

    private void summonOrbLaunchingParticle() {
        Vec3 direction = new Vec3(1.0, 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f);
        float rndHeight = this.f_19796_.m_188501_();
        float heightMd = FDEasings.easeOut((float)(1.0f - org.joml.Math.abs((float)(rndHeight * 2.0f - 1.0f)))) * 0.5f + 0.5f;
        Vec3 basePos = this.m_20182_().m_82520_(0.0, (double)(rndHeight *= 3.0f), 0.0);
        Vec3 rotateFromPos = this.m_20182_().m_82520_(direction.f_82479_ * (double)heightMd * 3.0, (double)rndHeight, direction.f_82481_ * (double)heightMd * 3.0);
        Object options = (double)this.f_19796_.m_188501_() < 0.9 ? BallParticleOptions.builder().particleProcessor((ParticleProcessor)new CircleParticleProcessor(basePos, true, true, 2.0f)).color(1 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 165 + this.f_19796_.m_188503_(60)).size(0.4f).scalingOptions(2, 20, 10).build() : LightningParticleOptions.builder().particleProcessor((ParticleProcessor)new CircleParticleProcessor(basePos, true, true, 2.0f)).color(50 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 165 + this.f_19796_.m_188503_(60)).quadSize(0.4f).lifetime(10).randomRoll(true).build();
        this.m_9236_().m_6493_((ParticleOptions)options, true, rotateFromPos.f_82479_, rotateFromPos.f_82480_, rotateFromPos.f_82481_, 0.0, 0.0, 0.0);
    }

    private void monolithEnergyDrainParticles() {
        for (Vec3 pos : MONOLITH_SPAWN_OFFSETS) {
            Vec3 ppos = this.m_20182_().m_82549_(pos.m_82520_(0.0, 2.5, 0.0));
            Vec3 center = this.m_20182_().m_82520_(0.0, 2.5, 0.0);
            BallParticleOptions options = BallParticleOptions.builder().particleProcessor((ParticleProcessor)new CircleParticleProcessor(center, false, true, 0.5f)).color(1 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 165 + this.f_19796_.m_188503_(60)).size(1.0f).scalingOptions(2, 15, 0).build();
            float p = this.getMonolithDrainPercent();
            this.m_9236_().m_7106_((ParticleOptions)options, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, 0.0, FDMathUtil.lerp((double)0.0, (double)-0.08, (double)p), 0.0);
        }
    }

    public boolean rayEvasionAttack(AttackInstance inst) {
        int stage = inst.stage;
        int tick = inst.tick;
        if (stage == 0) {
            if (tick == 0) {
                this.trapPlayers(true);
                this.doBlinding = false;
            } else if (tick > 20) {
                inst.nextStage();
            }
        } else if (stage == 1) {
            int rate = 40;
            if (tick % rate == 0) {
                for (ChesedKineticFieldEntity kineticFieldEntity : this.findCages()) {
                    this.summonRayPattern(kineticFieldEntity, kineticFieldEntity.getSquareRadius() - 0.5, tick / rate % 4);
                }
            } else if (tick > rate * 8) {
                inst.nextStage();
            }
        } else if (tick > 20) {
            this.trapPlayers(false);
            this.doBlinding = true;
            return true;
        }
        return false;
    }

    private List<ChesedKineticFieldEntity> findCages() {
        return this.m_9236_().m_45976_(ChesedKineticFieldEntity.class, new AABB((double)(-ARENA_RADIUS), -1.0, (double)(-ARENA_RADIUS), (double)ARENA_RADIUS, 3.0, (double)ARENA_RADIUS).m_82383_(this.m_20182_()));
    }

    private void summonRayPattern(ChesedKineticFieldEntity kineticFieldEntity, double maxRad, int pattern) {
        Vec3 pos = kineticFieldEntity.m_20182_();
        if (pattern == 0) {
            this.summonOneShotAtPos(pos.m_82520_(-maxRad, 0.0, -maxRad));
            this.summonOneShotAtPos(pos.m_82520_(maxRad, 0.0, -maxRad));
            this.summonOneShotAtPos(pos.m_82520_(maxRad, 0.0, maxRad));
            this.summonOneShotAtPos(pos.m_82520_(-maxRad, 0.0, maxRad));
            this.summonOneShotAtPos(pos.m_82520_(maxRad, 0.0, 0.0));
            this.summonOneShotAtPos(pos.m_82520_(0.0, 0.0, maxRad));
            this.summonOneShotAtPos(pos.m_82520_(-maxRad, 0.0, 0.0));
            this.summonOneShotAtPos(pos.m_82520_(0.0, 0.0, -maxRad));
            this.summonOneShotAtPos(pos);
        } else if (pattern == 1) {
            this.summonOneShotAtPos(pos.m_82520_(-maxRad / 2.0, 0.0, -maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(maxRad / 2.0, 0.0, -maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(maxRad / 2.0, 0.0, maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(-maxRad / 2.0, 0.0, maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(-maxRad, 0.0, -maxRad));
            this.summonOneShotAtPos(pos.m_82520_(maxRad, 0.0, -maxRad));
            this.summonOneShotAtPos(pos.m_82520_(maxRad, 0.0, maxRad));
            this.summonOneShotAtPos(pos.m_82520_(-maxRad, 0.0, maxRad));
            this.summonOneShotAtPos(pos);
        } else if (pattern == 2) {
            this.summonOneShotAtPos(pos.m_82520_(-maxRad / 2.0, 0.0, -maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(maxRad / 2.0, 0.0, -maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(maxRad / 2.0, 0.0, maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(-maxRad / 2.0, 0.0, maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(maxRad / 2.0, 0.0, 0.0));
            this.summonOneShotAtPos(pos.m_82520_(0.0, 0.0, maxRad / 2.0));
            this.summonOneShotAtPos(pos.m_82520_(-maxRad / 2.0, 0.0, 0.0));
            this.summonOneShotAtPos(pos.m_82520_(0.0, 0.0, -maxRad / 2.0));
        } else if (pattern == 3) {
            double l = Math.PI * 2 * maxRad;
            double angleB = 4.0 / l * Math.PI;
            for (double a = 0.0; a < Math.PI * 2 - angleB; a += angleB) {
                Vec3 b = new Vec3(maxRad, 0.0, 0.0).m_82524_((float)a);
                this.summonOneShotAtPos(pos.m_82549_(b));
            }
            this.summonOneShotAtPos(pos);
        }
    }

    private void summonOneShotAtPos(Vec3 pos) {
        float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.kineticFieldRayDamage);
        ChesedOneShotVerticalRayEntity entity = ChesedOneShotVerticalRayEntity.summon(this.m_9236_(), pos, damage, 40.0f, 30);
        entity.setDamageRadius(1.75f);
        entity.softerSound = true;
    }

    public boolean finalBOOMAttack(AttackInstance instance) {
        if (instance.stage == 0) {
            this.lookingAtTarget = false;
            int chargeEndTime = 100;
            int chargeStartTime = 20;
            int afterChargeIdle = 30;
            int chargeTime = chargeEndTime - chargeStartTime;
            if (instance.tick == 0) {
                this.setMonolithsImmunity(true);
                this.setMonolithDrainPercent(0.0f);
                this.getAnimationSystem().startAnimation(FINAL_ATTACK_LAYER, AnimationTicker.builder(BossAnims.CHESED_BOOM_ATTACK).setToNullTransitionTime(0).build());
            } else if (instance.tick == chargeStartTime - 5) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.CHESED_FINAL_ATTACK_CHARGE.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
            } else if (instance.tick == chargeStartTime) {
                this.setMonolithDrainPercent(0.0f);
                this.setDrainingMonoliths(true);
            } else if (instance.tick < chargeEndTime) {
                int currentChargeTime = instance.tick - chargeStartTime;
                this.setMonolithDrainPercent((float)currentChargeTime / (float)chargeTime);
            } else if (instance.tick < chargeEndTime + afterChargeIdle) {
                this.setDrainingMonoliths(false);
            } else {
                instance.nextStage();
            }
        } else if (instance.stage == 1) {
            this.lookingAtTarget = false;
            int rayStartTick = 15;
            int rayDuration = 15;
            int buildupSoundStart = 54;
            int impactFramesStart = 58;
            int damageAndEffectsStart = 60;
            if (instance.tick == 0) {
                this.setMonolithsImmunity(true);
                this.killCrystals();
                this.darkenCombatants(80, false);
            } else if (instance.tick == rayStartTick) {
                this.boomAttackRotatingRay(rayDuration);
                ((ServerLevel)this.m_9236_()).m_6263_(null, this.m_20182_().f_82479_, this.m_20182_().f_82480_, this.m_20182_().f_82481_, BossSounds.CHESED_FINAL_ATTACK_RAY.get(), SoundSource.HOSTILE, 6.0f, 0.8f);
            } else if (instance.tick > rayStartTick && instance.tick <= rayStartTick + rayDuration) {
                float p = 1.0f - (float)(instance.tick - rayStartTick) / (float)rayDuration;
                this.setMonolithDrainPercent(p);
            } else if (instance.tick == buildupSoundStart) {
                this.setMonolithDrainPercent(0.0f);
                this.playInEarsSound(BossSounds.CHESED_FINAL_ATTACK_EXPLOSION_PREPARE.get(), 1.0f, 1.0f);
            } else if (instance.tick == impactFramesStart) {
                this.setMonolithDrainPercent(0.0f);
                ImpactFrame base = new ImpactFrame(0.5f, 0.1f, 4, false);
                FDLibCalls.sendImpactFrames((ServerLevel)((ServerLevel)this.m_9236_()), (Vec3)this.m_20182_(), (float)60.0f, (ImpactFrame[])new ImpactFrame[]{base, new ImpactFrame(base).setDuration(1).setInverted(true), new ImpactFrame(base).setDuration(1), new ImpactFrame(base).setDuration(1).setInverted(true)});
            } else if (instance.tick == damageAndEffectsStart) {
                this.lookingAtTarget = false;
                this.setMonolithDrainPercent(0.0f);
                this.boomAttackAfterBlackout();
                this.playInEarsSound(BossSounds.CHESED_FINAL_ATTACK_EXPLOSION_BIGGER.get(), 1.0f, 1.0f);
                DefaultShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (Vec3)this.m_20182_(), (double)60.0, (FDShakeData)FDShakeData.builder().amplitude(0.25f).inTime(0).stayTime(30).outTime(100).build());
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(50.0f).amplitude(4.0f).inTime(0).stayTime(0).outTime(50).build(), (Vec3)this.m_20182_(), (double)60.0);
                int aliveMonoliths = this.getAliveMonolithsCount();
                if (aliveMonoliths != 0) {
                    float damagePercent = BossConfigs.BOSS_CONFIG.get().chesedConfig.finalAttackDamagePercentPerMonolith / 100.0f * (float)aliveMonoliths;
                    List<LivingEntity> affectedEntities = this.getAffectedEntities(false);
                    DamageSource source = BossDamageSources.chesedAttack((Entity)this);
                    for (LivingEntity livingEntity : affectedEntities) {
                        livingEntity.f_19802_ = 0;
                        float totalHealth = livingEntity.m_21233_() + livingEntity.m_6103_();
                        livingEntity.m_6469_(source, totalHealth * damagePercent);
                    }
                }
            } else if (instance.tick == damageAndEffectsStart + 1) {
                this.setMonolithDrainPercent(0.0f);
                this.darkenCombatants(0, true);
                this.summonOrReviveMonoliths();
            } else if (instance.tick >= damageAndEffectsStart + 2) {
                this.setMonolithDrainPercent(0.0f);
                instance.nextStage();
            }
        } else if (instance.stage == 2) {
            if (instance.tick > 100) {
                this.lookingAtTarget = true;
                this.summonOrReviveMonoliths();
                this.setMonolithsImmunity(false);
                return true;
            }
            return false;
        }
        return false;
    }

    private void darkenCombatants(int duration, boolean clear) {
        for (Player player : this.getCombatants(true)) {
            if (clear) {
                player.m_21195_((MobEffect)BossEffects.CHESED_DARKEN.get());
                continue;
            }
            player.m_7292_(new MobEffectInstance((MobEffect)BossEffects.CHESED_DARKEN.get(), duration, 0, true, false));
        }
    }

    private void clientBoomAttackRotatingRay() {
        float offset;
        AnimationSystem system = this.getAnimationSystem();
        AnimationTicker ticker = system.getTicker(FINAL_ATTACK_LAYER);
        if (ticker == null) {
            return;
        }
        float elapsedTime = ticker.getElapsedTime();
        if (elapsedTime < (offset = 155.0f) || elapsedTime > 157.0f) {
            return;
        }
        float radius = 35.0f;
        float angle = 0.2617994f;
        Vector3f v1 = new Vector3f(radius, 0.0f, 0.0f);
        Vector3f v2 = new Vector3f(radius, 0.0f, 0.0f).rotateY(angle);
        for (float i = 0.0f; i < (float)Math.PI * 2; i += angle) {
            float dist = v1.distance((Vector3fc)v2);
            for (float v = 0.0f; v < dist; v += 2.0f) {
                Vector3f b = v1.lerp((Vector3fc)v2, FDMathUtil.clamp((float)this.f_19796_.m_188501_(), (float)0.0f, (float)1.0f), new Vector3f());
                for (int k = 0; k < 3; ++k) {
                    float rndHeight = this.f_19796_.m_188501_() * 0.8f - 0.4f;
                    float hp = 1.0f - org.joml.Math.abs((float)rndHeight) / 0.4f;
                    Vector3f n = b.normalize(new Vector3f()).mul(0.025f * (float)k / 2.0f * hp);
                    Object options = (double)this.f_19796_.m_188501_() > 0.5 ? BallParticleOptions.builder().size(1.0f - this.f_19796_.m_188501_() * 0.1f - (1.0f - hp) * 0.2f).scalingOptions(5 + this.f_19796_.m_188503_(3), 0, 40).color(150 + this.f_19796_.m_188503_(40), 230, 255).build() : LightningParticleOptions.builder().color(50 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 200 + this.f_19796_.m_188503_(50)).quadSize(0.25f).lifetime(40).randomRoll(true).build();
                    this.m_9236_().m_6493_((ParticleOptions)options, true, this.m_20185_() + (double)b.x + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, this.m_20186_() + (double)1.4f + (double)b.y + (double)rndHeight, this.m_20189_() + (double)b.z + (double)this.f_19796_.m_188501_() * 0.5 - 0.25, (double)(-n.x + this.f_19796_.m_188501_() * 0.01f - 0.005f), (double)(-n.y + this.f_19796_.m_188501_() * 0.01f - 0.005f), (double)(-n.z + this.f_19796_.m_188501_() * 0.01f - 0.005f));
                }
            }
            v1 = v1.rotateY(angle);
            v2 = v2.rotateY(angle);
        }
    }

    private void boomAttackRotatingRay(int rotateDuration) {
        Vec3 look = this.m_20154_();
        Vec3 p = this.getCenter();
        Vec3 end = p.m_82549_(look.m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(38.0, 38.0, 38.0));
        ChesedRayOptions options = ChesedRayOptions.builder().processor(new ChesedRayCircleParticleProcessor(p, 1.15f, true)).time(5, rotateDuration - 10, 5).lightningColor(90, 180, 255).color(100, 255, 255).end(p).width(0.8f).build();
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)end, (double)120.0);
    }

    private void boomAttackAfterBlackout() {
        BossUtil.chesedBoomParticles((ServerLevel)this.m_9236_(), this.m_20182_().m_82520_(0.0, 1.0, 0.0), 38, 120.0);
        int amount = 60;
        int amountPerAmount = 3;
        float angle = (float)Math.PI * 2 / (float)amount;
        for (int i = 0; i < amount; ++i) {
            for (int c = 0; c < amountPerAmount; ++c) {
                float p = (float)c / (float)(amountPerAmount - 1);
                Vec3 v = new Vec3(36.0, 0.0, 0.0).m_82524_(angle * (float)i + (this.f_19796_.m_188501_() * 2.0f - 1.0f) * angle / 2.0f);
                Vec3 direction = v.m_82548_().m_82541_();
                Vec3 pos = this.m_20182_().m_82520_(v.f_82479_, (double)(1.0f + this.f_19796_.m_188501_() * 2.0f), v.f_82481_);
                BlockState state = (double)this.m_9236_().f_46441_.m_188501_() > 0.5 ? Blocks.f_220855_.m_49966_() : Blocks.f_152550_.m_49966_();
                float horizontalSpeed = 0.2f + this.f_19796_.m_188501_() * 0.05f + p * 0.4f;
                float verticalSpeed = this.f_19796_.m_188501_() * 0.4f + 0.05f + 0.3f * p;
                float randomRot = (this.f_19796_.m_188501_() * 2.0f - 1.0f) * (float)Math.PI / 4.0f;
                Vec3 speed = direction.m_82542_((double)horizontalSpeed, (double)horizontalSpeed, (double)horizontalSpeed).m_82524_(randomRot).m_82520_(0.0, (double)verticalSpeed, 0.0);
                ChesedFallingBlock block = ChesedFallingBlock.summon(this.m_9236_(), state, pos, 0.0f);
                block.m_20256_(speed);
                float rnd = this.f_19796_.m_188501_() * 0.05f;
                FDLibCalls.addParticleEmitter((Level)this.m_9236_(), (double)120.0, (ParticleEmitterData)ParticleEmitterData.builder((ParticleOptions)BigSmokeParticleOptions.builder().color(0.35f - rnd, 0.35f - rnd, 0.35f - rnd).lifetime(0, 0, 25).size(1.5f).build()).lifetime(200).processor((EmitterProcessor)new BoundToEntityProcessor(block.m_19879_(), Vec3.f_82478_)).position(this.m_20182_()).build());
            }
        }
    }

    public boolean doNothing(AttackInstance instance) {
        return instance.tick > 100;
    }

    private void killCrystals() {
        AABB box = new AABB(-40.0, -5.0, -40.0, 40.0, 5.0, 40.0).m_82383_(this.m_20182_());
        List list = this.m_9236_().m_45976_(ChesedCrystalEntity.class, box);
        for (ChesedCrystalEntity chesedCrystal : list) {
            chesedCrystal.m_6074_();
        }
        for (Player player : this.getCombatants(true)) {
            player.m_21195_((MobEffect)BossEffects.CHESED_ENERGIZED.get());
        }
    }

    public boolean summonCrystals(AttackInstance instance) {
        if (this.m_5448_() == null) {
            if (instance.tick % 10 == 0) {
                this.changeTarget();
            }
            return false;
        }
        this.killCrystals();
        for (int i = 0; i < 4; ++i) {
            ChesedCrystalEntity chesedCrystal = new ChesedCrystalEntity(BossEntities.CHESED_CRYSTAL.get(), this.m_9236_());
            Vector3f pos = new Vector3f((float)this.m_20182_().f_82479_, (float)this.m_20182_().f_82480_, (float)this.m_20182_().f_82481_);
            Vector3f between = new Vector3f(36.0f, 0.0f, 0.0f).rotateY(0.7853982f + 1.5707964f * (float)i);
            Vector3f summonPos = pos.add((Vector3fc)between, new Vector3f());
            Vector3f normalized = between.normalize(new Vector3f());
            Matrix4f mt = new Matrix4f();
            FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mt, (Vec3)new Vec3((double)(-normalized.x), 0.0, (double)(-normalized.z)));
            mt.rotateX(-1.0471976f);
            Vector3f direction = mt.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
            chesedCrystal.setCrystalFacingDirection(new Vec3((double)direction.x, (double)direction.y, (double)direction.z));
            chesedCrystal.m_6034_(summonPos.x, summonPos.y, summonPos.z);
            this.m_9236_().m_7967_((Entity)chesedCrystal);
        }
        return true;
    }

    public boolean rayAttack(AttackInstance instance) {
        int localStage;
        this.doBlinding = false;
        int tick = instance.tick;
        int stage = instance.stage;
        if (stage == 0 && tick == 0) {
            this.changeTarget();
        }
        if ((localStage = stage % 3) == 1) {
            float animationSpeed = 1.0f;
            int rayAttackTick = 35;
            int rayAttackReloadTime = 30;
            SoundEvent soundEvent = BossSounds.CHESED_RAY_CHARGE.get();
            if (this.isBelowHalfHP()) {
                soundEvent = BossSounds.CHESED_RAY_CHARGE_FAST.get();
                rayAttackTick = 25;
                animationSpeed = 1.5f;
                rayAttackReloadTime = 20;
            }
            if (tick == 0) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), soundEvent, SoundSource.HOSTILE, 5.0f, 1.0f);
                this.getAnimationSystem().startAnimation(RAY_ATTACK_LAYER, AnimationTicker.builder(BossAnims.CHESED_ATTACK).setToNullTransitionTime(0).setSpeed(animationSpeed).build());
            } else if (tick > 10 && tick < rayAttackTick) {
                int d;
                int n = d = this.isBelowHalfHP() ? 12 : 10;
                if (tick == rayAttackTick - d) {
                    this.playInEarsSound(BossSounds.ATTACK_DING.get(), 1.0f, 1.0f);
                }
                this.lookingAtTarget = tick < rayAttackTick - 5;
                Vector3f c = this.getModelPartPosition((Entity)this, "core", serverModel);
                Vec3 center = new Vec3((double)c.x, (double)c.y, (double)c.z).m_82549_(this.m_20182_());
                for (int i = 0; i < 10; ++i) {
                    Vec3 pos = center.m_82549_(new Vec3((double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f)).m_82541_().m_82542_(1.2, 1.2, 1.2));
                    Vec3 speed = center.m_82546_(pos).m_82541_().m_82542_(0.05, 0.05, 0.05);
                    BallParticleOptions options = BallParticleOptions.builder().friction(1.8f).size(0.3f).scalingOptions(0, 1, 4).color(100 + this.f_19796_.m_188503_(50), 255, 255).particleProcessor((ParticleProcessor)new SetParticleSpeedProcessor(speed)).build();
                    FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)pos, (double)60.0);
                }
            } else if (tick == rayAttackTick) {
                this.lookingAtTarget = false;
                Vec3 look = this.m_20154_();
                Vec3 p = this.getCenter().m_82549_(look.m_82548_());
                Vec3 end = p.m_82549_(look.m_82542_(60.0, 60.0, 60.0));
                ClipContext clipContext = new ClipContext(p, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
                BlockHitResult result = this.m_9236_().m_45547_(clipContext);
                end = result.m_82450_();
                ChesedRayOptions options = ChesedRayOptions.builder().time(0, 15, 5).lightningColor(90, 180, 255).color(100, 255, 255).end(end).width(0.8f).build();
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)p, (double)60.0);
                Vec3 reversedLook = look.m_82548_();
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(25.0f).scalingOptions(1, 0, 2).color(150, 230, 255).build(), (Vec3)p, (double)200.0);
                BossUtil.chesedRaySmoke((ServerLevel)this.m_9236_(), end, reversedLook, 120.0);
                ((ServerLevel)this.m_9236_()).m_6263_(null, end.f_82479_, end.f_82480_, end.f_82481_, BossSounds.CHESED_LIGHTNING_RAY.get(), SoundSource.HOSTILE, 5.0f, 0.8f);
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).amplitude(5.0f).inTime(0).stayTime(0).outTime(5).build(), (Vec3)end, (double)120.0);
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(50.0f).scalingOptions(2, 0, 3).color(100, 230, 255).build(), (Vec3)end, (double)120.0);
                this.summonStonesAfterRayAttack(30, reversedLook, end);
                BossUtil.chesedRayExplosion((ServerLevel)this.m_9236_(), end, reversedLook, 100.0, 15, 0.75f);
                this.damageOnRay(p, end);
            } else if (tick > rayAttackTick && tick > rayAttackTick + rayAttackReloadTime) {
                instance.nextStage();
                return false;
            }
        } else if (localStage == 0) {
            this.lookingAtTarget = true;
            if (this.m_5448_() != null && this.isLookingStraightAtEntity(this.m_5448_(), 0.01)) {
                instance.nextStage();
                this.lookingAtTarget = false;
            }
        } else if (localStage == 2) {
            float p;
            float ch;
            float rnd;
            if (instance.stage / 3 > 6 && (rnd = this.f_19796_.m_188501_()) > (ch = FDMathUtil.normalDistribution((float)(p = (float)stage / 18.0f), (float)0.0f, (float)0.43f))) {
                this.lookingAtTarget = true;
                this.killCrystals();
                this.doBlinding = true;
                return true;
            }
            instance.nextStage();
        }
        return false;
    }

    private void summonStonesAfterRayAttack(int count, Vec3 direction, Vec3 pos) {
        Vector3f v = new Vector3f(0.0f, 1.0f, 0.0f).cross((float)direction.f_82479_, (float)direction.f_82480_, (float)direction.f_82481_);
        for (int i = 0; i < count; ++i) {
            BlockState state = (double)this.f_19796_.m_188501_() > 0.5 ? Blocks.f_50730_.m_49966_() : Blocks.f_220855_.m_49966_();
            Vector3f add = v.rotateAxis((float)Math.PI * 2 * this.f_19796_.m_188501_(), (float)direction.f_82479_, (float)direction.f_82480_, (float)direction.f_82481_, new Vector3f());
            float rd = this.f_19796_.m_188501_() * 0.5f;
            ChesedFallingBlock block = ChesedFallingBlock.summon(this.m_9236_(), state, pos, direction.m_82520_((double)(add.x * rd * 2.0f), (double)(add.y * rd), (double)(add.z * rd * 2.0f)).m_82541_().m_82542_(0.5, 2.4 - (double)rd, 0.5), 0.0f);
            float rnd = this.f_19796_.m_188501_() * 0.05f;
            FDLibCalls.addParticleEmitter((Level)this.m_9236_(), (double)120.0, (ParticleEmitterData)ParticleEmitterData.builder((ParticleOptions)BigSmokeParticleOptions.builder().color(0.35f - rnd, 0.35f - rnd, 0.35f - rnd).lifetime(0, 0, 25).size(1.5f).build()).lifetime(200).processor((EmitterProcessor)new BoundToEntityProcessor(block.m_19879_(), Vec3.f_82478_)).position(this.m_20182_()).build());
        }
    }

    private void damageOnRay(Vec3 begin, Vec3 end) {
        List list = FDHelpers.traceEntities((Level)this.m_9236_(), (Vec3)begin, (Vec3)end, (double)1.0, entity -> !(entity instanceof ChesedBossBuddy));
        float damage = BossUtil.transformDamage(this.m_9236_(), BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.rayDamage));
        boolean chesedRayReflectorHit = false;
        ArrayList<LivingEntity> targets = new ArrayList<LivingEntity>();
        for (Entity entity2 : list) {
            if (entity2 instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity2;
                targets.add(living);
                continue;
            }
            if (!(entity2 instanceof ChesedRayReflector)) continue;
            ChesedRayReflector reflector = (ChesedRayReflector)entity2;
            chesedRayReflectorHit = true;
        }
        boolean targetHadEnergized = false;
        for (LivingEntity target : targets) {
            if (target.m_21023_((MobEffect)BossEffects.CHESED_ENERGIZED.get())) {
                if (target instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer)target;
                    FDPacketHandler.INSTANCE.sendTo((Object)new ChesedRayReflectPacket(), serverPlayer.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
                }
                MobEffectInstance instance = target.m_21124_((MobEffect)BossEffects.CHESED_ENERGIZED.get());
                int amplifier = instance.m_19564_();
                target.m_21195_((MobEffect)BossEffects.CHESED_ENERGIZED.get());
                if (amplifier > 0 && !chesedRayReflectorHit) {
                    target.m_7292_(new MobEffectInstance((MobEffect)BossEffects.CHESED_ENERGIZED.get(), 400, amplifier - 1, false, true));
                }
                targetHadEnergized = true;
                continue;
            }
            DamageSource source = BossDamageSources.chesedAttack((Entity)this);
            if ((double)this.m_9236_().f_46441_.m_188501_() < 0.05) {
                source = (double)this.m_9236_().f_46441_.m_188501_() > 0.5 ? BossDamageSources.chesedBaAttack((Entity)this) : BossDamageSources.chesedLorAttack((Entity)this);
            }
            target.m_6469_(source, damage);
        }
        if (chesedRayReflectorHit && targetHadEnergized) {
            this.decreaseHitCount(1);
        }
    }

    public boolean electricSphereAttack(AttackInstance instance) {
        this.lookingAtTarget = true;
        int tick = instance.tick;
        int stage = instance.stage;
        if (stage == 0) {
            if (tick == 0) {
                this.getAnimationSystem().startAnimation("CAST", AnimationTicker.builder(BossAnims.CHESED_CAST).setToNullTransitionTime(0).build());
            } else if (tick > 15) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.ELECTRIC_SPHERES_CAST.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
                this.repeatedSound = new RepeatedSound(BossSounds.ELECTRIC_SPHERES_STORM.get(), SoundSource.HOSTILE, this.m_20182_(), 5.0f, 1.0f, 60, 11);
                this.f_19804_.m_135381_(IS_LAUNCHING_ORBS, (Object)true);
                instance.nextStage();
            }
        } else if (stage == 1) {
            if (tick % 10 == 0) {
                this.summonLessSpheresAround(this.m_20182_(), tick);
            }
            if (tick > 200) {
                instance.nextStage();
            }
        } else if (stage == 2) {
            if (tick % 15 == 0) {
                int amountOnArc = 5;
                float rndOffs = this.f_19796_.m_188501_();
                for (int i = -amountOnArc; i <= amountOnArc; ++i) {
                    Vec3 v = new Vec3(1.0, 0.0, 0.0).m_82524_((float)i / (float)amountOnArc * (float)Math.PI / 8.0f + rndOffs * (float)Math.PI);
                    this.shootSphere(this.m_20182_(), v, 80);
                    this.shootSphere(this.m_20182_(), v.m_82548_(), 80);
                }
            }
            if (tick > 200) {
                instance.nextStage();
            }
        } else {
            if (tick < 200) {
                if (tick % 10 == 0) {
                    this.summonSpheresAround(25, (float)tick / 10.0f * (float)Math.PI / 9.0f);
                }
            } else if (tick < 350) {
                this.f_19804_.m_135381_(IS_LAUNCHING_ORBS, (Object)false);
            } else {
                return true;
            }
            return false;
        }
        return false;
    }

    private void summonLessSpheresAround(Vec3 pos, int tick) {
        float rndAngle = this.f_19796_.m_188501_() * (float)Math.PI / 8.0f - 0.19634955f;
        for (int i = 0; i < 4; ++i) {
            Vec3 v = new Vec3(1.0, 0.0, 0.0).m_82524_((float)i * (float)Math.PI / 2.0f + (float)tick / 2.0f * (float)Math.PI / 8.0f + rndAngle);
            this.shootSphere(pos, v);
        }
    }

    private void summonSpheresAround(int count, float angleOffset) {
        float angle = (float)Math.PI * 2 / (float)count;
        for (int i = 0; i < count; ++i) {
            Vec3 dir = new Vec3(1.0, 0.0, 0.0).m_82524_((float)i * angle + angleOffset);
            this.shootSphere(this.m_20182_(), dir, 150);
        }
    }

    private void shootSphere(Vec3 pos, Vec3 direction) {
        this.shootSphere(pos, direction, 100);
    }

    private void shootSphere(Vec3 pos, Vec3 direction, int time) {
        direction = direction.m_82541_();
        Vec3 sppos = pos.m_82520_(0.0, 1.0, 0.0).m_82549_(direction);
        Vec3 endPos = sppos.m_82549_(direction.m_82542_(37.0, 37.0, 37.0));
        ProjectileMovementPath path = new ProjectileMovementPath(time, false).addPos(sppos).addPos(endPos);
        float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.electricSphereDamage);
        ChesedElectricSphereEntity sphereEntity = ChesedElectricSphereEntity.summon(this.m_9236_(), damage, path);
    }

    public boolean rockfallAttack(AttackInstance instance) {
        this.lookingAtTarget = true;
        int stage = instance.stage;
        int tick = instance.tick;
        int height = 32;
        int rad = 36;
        if (stage == 0) {
            this.getAnimationSystem().startAnimation(ROCKFALL_TICKER, AnimationTicker.builder(BossAnims.CHESED_ROCKFALL_CAST).setToNullTransitionTime(0).build());
            if (tick > 10 && tick < 30) {
                int count = 3;
                float angle = (float)Math.PI * 2 / (float)count;
                Vec3 center = this.m_20182_();
                for (int i = 0; i < count; ++i) {
                    float a = angle * (float)i;
                    Vec3 v = new Vec3(2.0, 0.0, 0.0).m_82524_(a);
                    Vec3 ppos = this.m_20182_().m_82549_(v);
                    FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(0.5f).color(100 + this.f_19796_.m_188503_(50), 255, 255).particleProcessor((ParticleProcessor)new CompositeParticleProcessor(new ParticleProcessor[]{new CircleParticleProcessor(center, true, true, 2.0f), new SetParticleSpeedProcessor(new Vec3(0.0, 0.2, 0.0))})).scalingOptions(10, 10, 0).build(), (Vec3)ppos, (double)((float)height * 1.25f));
                }
            } else if (tick == 45) {
                Vec3 pos = this.m_20182_();
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)ChesedRayOptions.builder().width(1.0f).end(pos.m_82520_(0.0, (double)height, 0.0)).lightningColor(90, 180, 255).color(100, 255, 255).stay(8).in(2).out(10).build(), (Vec3)pos.m_82520_(0.0, 1.0, 0.0), (double)60.0);
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(10.0f).amplitude(10.0f).inTime(0).stayTime(0).outTime(10).build(), (Vec3)this.m_20182_().m_82520_(0.0, (double)height, 0.0), (double)((float)height * 1.25f));
                DefaultShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (Vec3)this.m_20182_(), (double)60.0, (FDShakeData)FDShakeData.builder().amplitude(0.15f).inTime(0).stayTime(50).outTime(150).build());
                BossUtil.chesedRayExplosion((ServerLevel)this.m_9236_(), this.m_20182_().m_82520_(0.0, (double)height, 0.0), new Vec3(0.0, -1.0, 0.0), 120.0, 10, 1.0f);
                ((ServerLevel)this.m_9236_()).m_6263_(null, this.m_20185_(), this.m_20186_() + (double)height, this.m_20189_(), BossSounds.CHESED_RAY.get(), SoundSource.HOSTILE, 100.0f, 1.0f);
                this.playInEarsSound(BossSounds.ROCKFALL.get(), 1.0f, 1.0f);
                this.playInEarsSound(BossSounds.RUMBLING.get(), 1.0f, 1.0f);
                int count = 10 + this.f_19796_.m_188503_(10);
                float angle = (float)Math.PI * 2 / (float)count;
                this.rockfallCastStones(count, height, angle);
                for (ServerPlayer player : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -2.0, 0.0), ARENA_HEIGHT + 2, ARENA_RADIUS + 10)) {
                    FDPacketHandler.INSTANCE.sendTo((Object)new PosLevelEventPacket(this.m_20182_().m_82520_(0.0, (double)(height + 2), 0.0), 23, this.m_19879_()), player.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
                }
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(75.0f).scalingOptions(1, 0, 2).color(150, 230, 255).build(), (Vec3)this.m_20182_().m_82520_(0.0, (double)height, 0.0), (double)200.0);
            } else if (tick >= 46) {
                instance.nextStage();
            }
        } else if (stage == 1) {
            int stonesAroundPlayersFrequency;
            float duration = 400.0f;
            if (tick % (this.isBelowHalfHP() ? 25 : 40) == 0) {
                this.summonDelayedVerticalRayOnFieldNearPlayers(5.0f, this.isBelowHalfHP() ? 3 : 2);
            }
            this.summonStonesAround(2, rad, this.m_20182_().m_82520_(0.0, (double)height, 0.0), true, false, FDEasings::easeOut);
            int n = stonesAroundPlayersFrequency = this.isBelowHalfHP() ? 6 : 5;
            if (tick % stonesAroundPlayersFrequency == 0) {
                AABB box = new AABB((double)(-rad), -2.0, (double)(-rad), (double)rad, (double)height, (double)rad).m_82383_(this.m_20182_());
                for (Player player : this.m_9236_().m_45955_(BossUtil.ALL, (LivingEntity)this, box)) {
                    Vec3 p = player.m_20182_();
                    if (p.m_82546_(this.m_20182_()).m_82542_(1.0, 0.0, 1.0).m_82553_() > (double)rad) continue;
                    this.summonStonesAround(1, 5, p.m_82542_(1.0, 0.0, 1.0).m_82520_(0.0, this.m_20182_().f_82480_ + (double)height, 0.0), true, false, FDEasings::easeOut);
                }
            }
            if ((float)tick >= duration) {
                instance.nextStage();
            }
        } else if (stage == 2) {
            return instance.tick > 80;
        }
        return false;
    }

    public void rockfallCastStones(int count, float height, float angle) {
        for (int i = 0; i < count; ++i) {
            BlockState state = this.f_19796_.m_188501_() > 0.5f ? Blocks.f_152550_.m_49966_() : Blocks.f_220855_.m_49966_();
            ChesedFallingBlock block = ChesedFallingBlock.summon(this.m_9236_(), state, this.m_20182_().m_82520_(0.0, (double)height, 0.0), BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.rockfallRockDamage));
            Vec3 v = new Vec3((double)this.f_19796_.m_188501_() * 0.025 + 0.2, 0.0, 0.0).m_82524_(angle * (float)i + (this.f_19796_.m_188501_() * 2.0f - 1.0f) * angle);
            block.m_20256_(v.m_82520_(0.0, (double)(-this.f_19796_.m_188501_()) * 0.5 - 0.25, 0.0));
            float rnd = this.f_19796_.m_188501_() * 0.05f;
            FDLibCalls.addParticleEmitter((Level)this.m_9236_(), (double)(height * 2.0f), (ParticleEmitterData)ParticleEmitterData.builder((ParticleOptions)BigSmokeParticleOptions.builder().color(0.35f - rnd, 0.35f - rnd, 0.35f - rnd).lifetime(0, 0, 50).size(2.0f).build()).lifetime(200).processor((EmitterProcessor)new BoundToEntityProcessor(block.m_19879_(), Vec3.f_82478_)).position(this.m_20182_()).build());
        }
    }

    private void summonStonesAround(int count, int rad, Vec3 center, boolean useEasing, boolean reverseEasing, Function<Float, Float> func) {
        float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.rockfallRockDamage);
        for (int i = 0; i < count; ++i) {
            float rnd = this.f_19796_.m_188501_();
            if (useEasing) {
                rnd = !reverseEasing ? func.apply(Float.valueOf(rnd)).floatValue() : 1.0f - func.apply(Float.valueOf(rnd)).floatValue();
            }
            Vec3 v = new Vec3((double)((float)rad * rnd), 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f);
            Vec3 p = center.m_82549_(v);
            BlockState state = (double)this.m_9236_().f_46441_.m_188501_() > 0.5 ? Blocks.f_220855_.m_49966_() : Blocks.f_50730_.m_49966_();
            ChesedFallingBlock fallingBlock = ChesedFallingBlock.summon(this.m_9236_(), state, p, damage);
            fallingBlock.softerSound = true;
        }
    }

    public boolean earthquakeAttack(AttackInstance instance) {
        this.lookingAtTarget = false;
        int t = instance.tick;
        int radius = 38;
        if (instance.stage <= 4) {
            if (t < 6) {
                if (t > 3) {
                    this.summonCirclingParticlesServerside(4, 0.55f, 2.0f, 0.5f, 6, 9);
                }
                if (t == 0) {
                    this.getAnimationSystem().startAnimation(EARTHQUAKE_ATTACK_LAYER, AnimationTicker.builder(BossAnims.CHESED_EARTHQUAKE_CAST).setToNullTransitionTime(0).build());
                }
            } else {
                if (t < 10) {
                    this.summonCirclingParticlesServerside(4, 0.55f, 2.0f, 0.5f, 6, 9);
                }
                if (t == 17) {
                    this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.CHESED_FLOOR_SMASH.get(), SoundSource.HOSTILE, 10.0f, 1.0f);
                } else if (t == 18) {
                    this.summonRadialEarthquake(radius, true);
                } else if (t == 21) {
                    PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(10.0f).stayTime(0).inTime(5).outTime(5).amplitude(3.0f).build(), (Vec3)this.m_20182_(), (double)50.0);
                    FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(25.0f).scalingOptions(0, 0, 2).color(150 - this.f_19796_.m_188503_(50), 230, 255).build(), (Vec3)this.m_20182_(), (double)60.0);
                    int count = this.isBelowHalfHP() ? 12 : 8;
                    this.summonVerticalRayAttacksRadial(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f, (float)Math.PI * (float)((double)this.f_19796_.m_188501_() > 0.5 ? 1 : -1), radius + 2, BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.earthquakeRayDamage), 90 - (this.isBelowHalfHP() ? 10 : 0), count);
                } else if (t > 50) {
                    instance.nextStage();
                }
            }
        } else if (instance.tick > 40) {
            return true;
        }
        return false;
    }

    private void trapPlayers(boolean trap) {
        if (trap) {
            List<Player> players = this.getCombatants(false);
            for (Player player : players) {
                ChesedKineticFieldEntity fieldEntity = new ChesedKineticFieldEntity(BossEntities.CHESED_KINETIC_FIELD.get(), this.m_9236_());
                fieldEntity.m_6034_(player.m_20185_(), this.m_20186_() - 0.01, player.m_20189_());
                this.m_9236_().m_7967_((Entity)fieldEntity);
            }
        } else {
            float r = 80.0f;
            List list = this.m_9236_().m_45976_(ChesedKineticFieldEntity.class, new AABB((double)(-r), (double)(-r), (double)(-r), (double)r, (double)r, (double)r).m_82383_(this.m_20182_()));
            for (ChesedKineticFieldEntity entity : list) {
                entity.m_6074_();
            }
        }
    }

    private void summonRadialEarthquake(int radius, boolean doSonicParticles) {
        SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(this.m_20097_(), this.m_20182_().m_82520_(0.0, 0.5, 0.0), new Vec3(1.0, 0.0, 0.0)).maxAngle((float)Math.PI * 2).maxSpeed(0.15f).collectRadius(2).maxParticleLifetime(30).count(100).maxVerticalSpeedEdges(0.2f).maxVerticalSpeedCenter(0.2f));
        FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)packet);
        RadialEarthquakeEntity radialEarthquakeEntity = RadialEarthquakeEntity.summon(this.m_9236_(), this.m_20097_(), 1, radius, 1.0f, BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.eartquakeDamage));
        if (doSonicParticles) {
            for (int i = 0; i < 6; ++i) {
                SonicParticleOptions options = SonicParticleOptions.builder().facing(0.0, 1.0, 0.0).color(0.3f, 1.0f, 1.0f).startSize(2.0f).endSize(radius).resizeSpeed(-i * 2).resizeAcceleration(0.75f + (float)i).lifetime(60).build();
                for (Player player : this.m_9236_().m_45955_(BossUtil.ALL, (LivingEntity)this, this.m_20191_().m_82400_(100.0))) {
                    ((ServerLevel)this.m_9236_()).m_8624_((ServerPlayer)player, (ParticleOptions)options, true, this.m_20182_().f_82479_, this.m_20182_().f_82480_ + 0.01, this.m_20182_().f_82481_, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private void summonVerticalRayAttacksRadial(float offsetAngle, float finalRotationAngle, float radius, float damage, int time, int count) {
        float angleBetween = (float)Math.PI * 2 / (float)count;
        for (int i = 0; i < count; ++i) {
            ProjectileMovementPath path = new ProjectileMovementPath(time, false);
            for (float x = 1.0f; x <= radius; x += 1.0f) {
                float p = x / radius;
                float angle = angleBetween * (float)i + offsetAngle + p * finalRotationAngle;
                Vec3 offs = new Vec3((double)x, 0.0, 0.0).m_82524_(angle);
                path.addPos(this.m_20182_().m_82549_(offs));
            }
            ChesedMovingVerticalRay.summon(this.m_9236_(), this.m_20182_(), path, damage, 40.0f);
        }
    }

    public boolean blockAttack(AttackInstance instance) {
        this.lookingAtTarget = true;
        float height = 8.0f;
        int blocksUpTime = 30;
        int blocksCycleTime = 30;
        int timeTillAttack = 40;
        int stage = instance.stage;
        int tick = instance.tick;
        int count = 10;
        if (stage == 0) {
            if (this.blockAttackProjectiles.isEmpty()) {
                if (!this.trySearchProjectiles()) {
                    this.getAnimationSystem().startAnimation("blockAttack", AnimationTicker.builder(BossAnims.CHESED_CAST).setToNullTransitionTime(0).setSpeed(1.2f).build());
                    this.initiateBlockProjectiles(blocksUpTime, blocksCycleTime, height, count);
                    this.changeTarget();
                    instance.nextStage();
                }
            } else {
                this.changeTarget();
                instance.nextStage();
            }
        } else if (stage == 1) {
            if (tick == 13) {
                if (this.blockAttackProjectiles.size() == count) {
                    BossUtil.posEvent((ServerLevel)this.m_9236_(), this.m_20182_().m_82520_(0.0, 0.05, 0.0), 1, 0, 60.0);
                    this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.CHESED_FLOOR_SMASH.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
                }
            } else if (tick >= timeTillAttack) {
                instance.nextStage();
            }
        } else if (stage == 2) {
            if (!this.blockAttackProjectiles.isEmpty()) {
                LivingEntity player;
                if (instance.tick % 8 == 0 && (player = this.m_5448_()) != null) {
                    this.throwBlock(player, height);
                }
            } else {
                instance.nextStage();
            }
        } else if (instance.tick > 20) {
            return true;
        }
        return false;
    }

    private void throwBlock(LivingEntity player, float height) {
        ChesedBlockProjectile next = this.blockAttackProjectiles.remove(this.blockAttackProjectiles.size() - 1);
        next.f_19794_ = false;
        next.movementPath = null;
        Vec3 tpos = this.targetGroundPosition(player);
        Vec3 b = tpos.m_82546_(next.m_20182_());
        Vec3 h = b.m_82542_(1.0, 0.0, 1.0);
        Vec3 targetPos = tpos.m_82549_(h.m_82541_().m_82548_().m_82542_(2.5, 0.0, 2.5));
        next.setRotationSpeed(10.0f);
        Vec3 flyTo = this.m_20182_().m_82520_(0.0, (double)height + 1.5, 0.0);
        ProjectileMovementPath path = new ProjectileMovementPath(8, false);
        path.addPos(next.m_20182_());
        path.addPos(flyTo);
        path.addPos(flyTo.m_82520_(0.0, 1.0, 0.0));
        path.setSpeedOnEnd(targetPos.m_82546_(flyTo).m_82542_(0.25, 0.25, 0.25));
        next.movementPath = path;
        this.delayedSounds.add(new DelayedSound(BossSounds.THROW_STUFF.get(), SoundSource.HOSTILE, flyTo, 10.0f, 2.0f, 7));
    }

    private Vec3 targetGroundPosition(LivingEntity target) {
        Vec3 last = this.previousTargetPos != null ? this.previousTargetPos : target.m_20182_();
        Vec3 current = target.m_20182_();
        Vec3 b = current.m_82546_(last);
        Vec3 toReturn = target.m_20182_().m_82549_(b.m_82542_(3.0, 0.0, 3.0));
        BlockPos pos = new BlockPos((int)org.joml.Math.floor((double)toReturn.f_82479_), (int)org.joml.Math.floor((double)toReturn.f_82480_), (int)org.joml.Math.floor((double)toReturn.f_82481_));
        for (int i = 0; i < 5; ++i) {
            if (!this.m_9236_().m_8055_(pos.m_7918_(0, -i, 0)).m_60795_()) {
                return toReturn;
            }
            toReturn = toReturn.m_82492_(0.0, 1.0, 0.0);
        }
        return toReturn;
    }

    private void initiateBlockProjectiles(int blocksUpTime, int blocksCycleTime, float height, int count) {
        for (int i = 0; i < count; ++i) {
            float angle = this.getInitProjectileRotation(i, count);
            ChesedBlockProjectile projectile = new ChesedBlockProjectile(BossEntities.BLOCK_PROJECTILE.get(), this.m_9236_());
            projectile.setBlockState((double)this.f_19796_.m_188501_() > 0.5 ? Blocks.f_220855_.m_49966_() : Blocks.f_152550_.m_49966_());
            projectile.setDropParticlesTime(blocksUpTime);
            ProjectileMovementPath path = this.createRotationPath(angle, -2.0f, height, 30, blocksUpTime, false);
            ProjectileMovementPath next = this.createRotationPath(angle, height, height, 30, blocksCycleTime, true);
            path.setNext(next);
            projectile.setDamage(BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.blockAttackDamage));
            projectile.f_19794_ = true;
            projectile.m_146884_((Vec3)path.getPositions().get(0));
            projectile.movementPath = path;
            this.blockAttackProjectiles.add(projectile);
            this.m_9236_().m_7967_((Entity)projectile);
        }
    }

    private boolean trySearchProjectiles() {
        List projectiles = this.m_9236_().m_6443_(ChesedBlockProjectile.class, this.m_20191_().m_82377_(10.0, 20.0, 10.0), projectile -> true);
        if (!projectiles.isEmpty()) {
            this.blockAttackProjectiles = projectiles;
            return true;
        }
        return false;
    }

    private ProjectileMovementPath createRotationPath(float angle, float yStart, float yEnd, int pathDetalization, int time, boolean cycle) {
        ProjectileMovementPath path = new ProjectileMovementPath(time, cycle);
        float a1 = (float)Math.PI * 2 / (float)pathDetalization;
        for (int i = 0; i <= pathDetalization; ++i) {
            float p = (float)i / (float)pathDetalization;
            float a = angle + (float)i * a1;
            Vec3 v = new Vec3(5.0, 0.0, 0.0).m_82524_(a);
            Vec3 pos = this.m_20182_().m_82520_(v.f_82479_, (double)FDMathUtil.lerp((float)yStart, (float)yEnd, (float)FDEasings.easeInOutBack((float)p)), v.f_82481_);
            path.addPos(pos);
        }
        return path;
    }

    private float getInitProjectileRotation(int id, int count) {
        return (float)Math.PI * 2 * ((float)id / (float)count);
    }

    public boolean roll(AttackInstance instance) {
        this.lookingAtTarget = false;
        this.doBlinding = false;
        int tick = instance.tick;
        int stage = instance.stage;
        if (tick == 0 && stage == 0) {
            this.oldRollPos = this.m_20182_();
        }
        Vector3f p = this.getModelPartPosition((Entity)this, "base", serverModel);
        Vec3 pos = this.m_20182_().m_82520_((double)p.x, (double)p.y, (double)p.z);
        AnimationSystem system = this.getAnimationSystem();
        if (stage == 0) {
            this.playIdle = false;
            if (system.getTickerAnimation("ROLL_UP") != BossAnims.CHESED_ROLL_UP.get()) {
                system.startAnimation("ROLL_UP", AnimationTicker.builder(BossAnims.CHESED_ROLL_UP).startTime((float)tick).build());
            }
            if (tick >= ((Animation)BossAnims.CHESED_ROLL_UP.get()).getAnimTime()) {
                instance.nextStage();
                return false;
            }
        } else if (stage == 1) {
            this.playIdle = false;
            PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).stayTime(0).inTime(2).outTime(5).amplitude(3.5f).build(), (Vec3)pos, (double)10.0);
            if (this.f_19797_ % 2 == 0) {
                this.m_9236_().m_6263_(null, pos.f_82479_, pos.f_82480_, pos.f_82481_, BossSounds.RUMBLING_SHORT.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
            }
            if (system.getTickerAnimation("ROLL_AROUND") != BossAnims.CHESED_ROLL.get()) {
                system.startAnimation("ROLL_AROUND", AnimationTicker.builder(BossAnims.CHESED_ROLL).startTime((float)tick).setToNullTransitionTime(0).build());
            }
            if (system.getTickerAnimation("ROLLING") != BossAnims.CHESED_ROLL_ROLL.get()) {
                system.startAnimation("ROLLING", AnimationTicker.builder(BossAnims.CHESED_ROLL_ROLL).setToNullTransitionTime(0).startTime((float)tick).build());
            }
            if (tick >= ((Animation)BossAnims.CHESED_ROLL.get()).getAnimTime() - 20) {
                instance.nextStage();
                return false;
            }
            if (this.isBelowHalfHP()) {
                if (this.oldRollPos == null) {
                    this.oldRollPos = pos;
                }
                Vec3 b = this.oldRollPos.m_82546_(pos);
                int amount = (int)org.joml.Math.ceil((double)b.m_82553_());
                for (int i = 0; i < amount; ++i) {
                    ChesedFireTrailEntity chesedFireTrailEntity = ChesedFireTrailEntity.summon(this.m_9236_(), pos.m_82520_(0.0, -0.5, 0.0).m_82549_(b.m_82541_().m_82542_((double)i, (double)i, (double)i)), BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.fireTrailDamage), 80);
                }
            }
            this.handleRoll(tick, pos);
        } else if (stage == 2) {
            this.doBlinding = true;
            this.setRolling(false);
            system.stopAnimation("ROLL_UP");
            system.startAnimation("ROLL_UP_END", AnimationTicker.builder(BossAnims.CHESED_ROLL_UP_JUST_END).setToNullTransitionTime(0).setLoopMode(Animation.LoopMode.HOLD_ON_LAST_FRAME).build());
            if (tick >= ((Animation)BossAnims.CHESED_ROLL_UP_JUST_END.get()).getAnimTime() + 20) {
                instance.nextStage();
                this.playIdle = true;
                return false;
            }
        } else {
            this.playIdle = true;
            this.setRolling(false);
            system.stopAnimation("ROLL_UP");
            system.stopAnimation("ROLL_UP_END");
            system.stopAnimation("ROLL_AROUND");
            system.stopAnimation("ROLLING");
            this.doBlinding = true;
            return true;
        }
        return false;
    }

    private void handleRoll(int attackTime, Vec3 pos) {
        if (attackTime == 0) {
            this.oldRollPos = this.m_20182_();
        }
        if (this.oldRollPos == null) {
            this.oldRollPos = pos;
        }
        this.summonRollEarthShatters(this.oldRollPos.m_82520_(0.0, -1.0, 0.0), pos.m_82520_(0.0, -1.0, 0.0));
        this.rollDamageEntities(this.oldRollPos, pos);
        this.oldRollPos = pos;
        int rollTime = ((Animation)BossAnims.CHESED_ROLL.get()).getAnimTime() + ((Animation)BossAnims.CHESED_ROLL_UP.get()).getAnimTime();
        if (attackTime >= rollTime) {
            this.setRolling(false);
        } else {
            this.setRolling(true);
        }
    }

    private void rollDamageEntities(Vec3 oldPos, Vec3 pos) {
        List entities = FDHelpers.traceEntities((Level)this.m_9236_(), (Vec3)oldPos, (Vec3)pos, (double)2.1, entity -> {
            if (!(entity instanceof LivingEntity)) return false;
            LivingEntity living = (LivingEntity)entity;
            if (entity instanceof ChesedBossBuddy) return false;
            return true;
        });
        float damage = BossUtil.transformDamage(this.m_9236_(), BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.rollAttackDamage));
        for (Entity entity2 : entities) {
            LivingEntity livingEntity = (LivingEntity)entity2;
            Vec3 pushDirection = FDMathUtil.getNormalVectorFromLineToPoint((Vec3)oldPos, (Vec3)pos, (Vec3)entity2.m_20182_()).m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(3.0, 0.0, 3.0).m_82520_(0.0, 0.5, 0.0);
            livingEntity.m_6469_(BossDamageSources.CHESED_ROLL_SOURCE, damage);
            if (livingEntity instanceof Player) {
                Player player = (Player)livingEntity;
                FDLibCalls.setServerPlayerSpeed((ServerPlayer)((ServerPlayer)player), (Vec3)pushDirection);
                continue;
            }
            livingEntity.m_20256_(pushDirection);
        }
    }

    private void summonRollEarthShatters(Vec3 oldPos, Vec3 pos) {
        Vec3 b = pos.m_82546_(oldPos);
        Vec3 nb = b.m_82541_();
        Vec3 r = nb.m_82524_(1.5707964f);
        Vec3 l = nb.m_82524_(-1.5707964f);
        Matrix4fStack mat = new Matrix4fStack(3);
        float angle = (float)org.joml.Math.atan2((double)nb.f_82479_, (double)nb.f_82481_);
        mat.rotateY(angle);
        mat.pushMatrix();
        mat.rotateX(org.joml.Math.toRadians((float)-65.0f));
        Vector4f direction = mat.transform(0.0f, 0.0f, 1.0f, 1.0f, new Vector4f());
        mat.popMatrix();
        mat.pushMatrix();
        mat.rotateY(org.joml.Math.toRadians((float)45.0f));
        mat.rotateX(org.joml.Math.toRadians((float)-45.0f));
        Vector4f directionLeft = mat.transform(0.0f, 0.0f, 1.0f, 1.0f, new Vector4f());
        mat.popMatrix();
        mat.pushMatrix();
        mat.rotateY(org.joml.Math.toRadians((float)-45.0f));
        mat.rotateX(org.joml.Math.toRadians((float)-45.0f));
        Vector4f directionRight = mat.transform(0.0f, 0.0f, 1.0f, 1.0f, new Vector4f());
        mat.popMatrix();
        EarthShatterSettings settingsMain = EarthShatterSettings.builder().direction(direction.x, direction.y, direction.z).upTime(4).upDistance(0.25f).downTime(4).stayTime(2).build();
        EarthShatterSettings settingsLeft = EarthShatterSettings.builder().direction(directionLeft.x, directionLeft.y, directionLeft.z).upTime(4).upDistance(0.5f).downTime(4).stayTime(2).build();
        EarthShatterSettings settingsRight = EarthShatterSettings.builder().direction(directionRight.x, directionRight.y, directionRight.z).upTime(4).upDistance(0.5f).downTime(4).stayTime(2).build();
        float i = 0.0f;
        while ((double)i < b.m_82553_()) {
            Vec3 v = pos.m_82549_(nb.m_82542_((double)i, (double)i, (double)i).m_82548_());
            Vec3 r1 = v.m_82549_(r);
            Vec3 l1 = v.m_82549_(l);
            BlockPos mainpos = new BlockPos((int)v.f_82479_, (int)v.f_82480_, (int)v.f_82481_);
            BlockPos rpos = new BlockPos((int)r1.f_82479_, (int)r1.f_82480_, (int)r1.f_82481_);
            BlockPos lpos = new BlockPos((int)l1.f_82479_, (int)l1.f_82480_, (int)l1.f_82481_);
            int tries = 0;
            while (EarthShatterEntity.summon(this.m_9236_(), mainpos.m_6625_(tries), settingsMain) == null && tries++ < 5) {
            }
            tries = 0;
            while (EarthShatterEntity.summon(this.m_9236_(), lpos.m_6625_(tries), settingsLeft) == null && tries++ < 5) {
            }
            tries = 0;
            while (EarthShatterEntity.summon(this.m_9236_(), rpos.m_6625_(tries), settingsRight) == null && tries++ < 5) {
            }
            i += 1.0f;
        }
    }

    private void handleClientRolling() {
        Vector3f p = this.getModelPartPosition((Entity)this, "base", clientModel);
        Vec3 pos = this.m_20182_().m_82520_((double)p.x, (double)(p.y - 1.0f), (double)p.z);
        if (this.oldRollPos == null) {
            this.oldRollPos = pos;
        }
        Vec3 b = pos.m_82546_(this.oldRollPos);
        Vec3 nb = b.m_82541_();
        Vec3 r = nb.m_82524_(1.5707964f);
        Vec3 l = nb.m_82524_(-1.5707964f);
        Vec3 ppos = pos.m_82520_((double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), 0.0, (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f));
        float md = this.f_19796_.m_188501_() + 1.0f;
        this.m_9236_().m_6493_((ParticleOptions)ArcLightningOptions.builder(BossParticles.ARC_LIGHTNING.get()).end(ppos.m_82520_(-nb.f_82479_ * (double)md, (double)1.1f, -nb.f_82481_ * (double)md)).lifetime(4).color(1 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 165 + this.f_19796_.m_188503_(60)).lightningSpread(0.25f).width(0.2f).segments(5).circleOffset(-3.0f).build(), true, ppos.f_82479_, ppos.f_82480_ + 0.5 + (double)0.1f, ppos.f_82481_, 0.0, 0.0, 0.0);
        float i = 0.0f;
        while ((double)i < b.m_82553_()) {
            int g;
            Vec3 v = pos.m_82549_(nb.m_82542_((double)i, (double)i, (double)i).m_82548_());
            Vec3 r1 = v.m_82549_(r);
            Vec3 l1 = v.m_82549_(l);
            BlockPos mainpos = new BlockPos((int)v.f_82479_, (int)v.f_82480_, (int)v.f_82481_);
            BlockPos rpos = new BlockPos((int)r1.f_82479_, (int)r1.f_82480_, (int)r1.f_82481_);
            BlockPos lpos = new BlockPos((int)l1.f_82479_, (int)l1.f_82480_, (int)l1.f_82481_);
            BlockState mainState = this.m_9236_().m_8055_(mainpos);
            BlockState leftState = this.m_9236_().m_8055_(lpos);
            BlockState rightState = this.m_9236_().m_8055_(rpos);
            float randomRadius = 1.0f;
            if (!mainState.m_60795_()) {
                for (g = 0; g < 5; ++g) {
                    this.m_9236_().m_6493_((ParticleOptions)new BlockParticleOption(ParticleTypes.f_123794_, mainState), true, v.f_82479_ + (double)(this.m_9236_().f_46441_.m_188501_() * randomRadius) - (double)(randomRadius / 2.0f), v.f_82480_ + 0.75, v.f_82481_ + (double)(this.m_9236_().f_46441_.m_188501_() * randomRadius) - (double)(randomRadius / 2.0f), 0.0, 0.0, 0.0);
                }
            }
            if (!leftState.m_60795_()) {
                for (g = 0; g < 5; ++g) {
                    this.m_9236_().m_6493_((ParticleOptions)new BlockParticleOption(ParticleTypes.f_123794_, leftState), true, l1.f_82479_ + (double)(this.m_9236_().f_46441_.m_188501_() * randomRadius) - (double)(randomRadius / 2.0f), l1.f_82480_ + 0.75, l1.f_82481_ + (double)(this.m_9236_().f_46441_.m_188501_() * randomRadius) - (double)(randomRadius / 2.0f), 0.0, 0.0, 0.0);
                }
            }
            if (!rightState.m_60795_()) {
                for (g = 0; g < 5; ++g) {
                    this.m_9236_().m_6493_((ParticleOptions)new BlockParticleOption(ParticleTypes.f_123794_, rightState), true, r1.f_82479_ + (double)(this.m_9236_().f_46441_.m_188501_() * randomRadius) - (double)(randomRadius / 2.0f), r1.f_82480_ + 0.75, r1.f_82481_ + (double)(this.m_9236_().f_46441_.m_188501_() * randomRadius) - (double)(randomRadius / 2.0f), 0.0, 0.0, 0.0);
                }
            }
            i += 1.0f;
        }
        this.oldRollPos = pos;
    }

    private void lookAtTarget(LivingEntity target) {
        Vec3 pos = this.getLookAtPos(target);
        this.m_21563_().m_24964_(pos);
    }

    private Vec3 getLookAtPos(LivingEntity target) {
        double dist = target.m_20270_((Entity)this);
        double distMod = Mth.m_14008_((double)(dist / 5.0), (double)0.0, (double)1.0);
        double baseLookAtDist = this.isBelowHalfHP() ? 8.0 : 10.0;
        double lookAtPosMod = baseLookAtDist * (double)FDEasings.easeIn((float)((float)(distMod * distMod)));
        Vec3 v = target.m_20182_().m_82546_(this.previousTargetPos);
        Vec3 lookAtMod = v.m_82542_(lookAtPosMod, lookAtPosMod, lookAtPosMod);
        Vec3 pos = target.m_20182_().m_82520_(0.0, (double)(target.m_20206_() / 2.0f), 0.0).m_82549_(lookAtMod);
        return pos;
    }

    private boolean isLookingStraightAtEntity(LivingEntity entity, double accuracy) {
        Vec3 center;
        Vec3 lookAt;
        Vec3 b;
        Vec3 look = this.m_20154_();
        double v = look.m_82526_(b = (lookAt = this.getLookAtPos(entity)).m_82546_(center = this.m_146892_()).m_82541_());
        return v >= 1.0 - accuracy;
    }

    private void summonDelayedVerticalRayOnFieldNearPlayers(float radius, int count) {
        float angle = (float)Math.PI * 2 / (float)count;
        for (Player player : this.getCombatants(false)) {
            float r = this.f_19796_.m_188501_();
            Vec3 pos = new Vec3((double)(radius - r), 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
            for (int i = 0; i < count; ++i) {
                Vec3 resultingPos = pos.m_82524_(angle * (float)i).m_82549_(player.m_20182_());
                ChesedOneShotVerticalRayEntity entity = ChesedOneShotVerticalRayEntity.summon(this.m_9236_(), resultingPos, BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().chesedConfig.rockfallRayDamage), 40.0f, 30);
                entity.setDamageRadius(2.0f);
            }
        }
    }

    private void summonCirclingParticlesServerside(int count, float verticalSpeed, float offset, float size, int in, int stay) {
        float angle = (float)Math.PI * 2 / (float)count;
        Vec3 center = this.m_20182_();
        for (int i = 0; i < count; ++i) {
            float a = angle * (float)i;
            Vec3 v = new Vec3((double)offset, 0.0, 0.0).m_82524_(a);
            Vec3 ppos = this.m_20182_().m_82549_(v);
            FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(size).color(100 + this.f_19796_.m_188503_(50), 255, 255).particleProcessor((ParticleProcessor)new CompositeParticleProcessor(new ParticleProcessor[]{new CircleParticleProcessor(center, true, true, 2.0f), new SetParticleSpeedProcessor(new Vec3(0.0, (double)verticalSpeed, 0.0))})).scalingOptions(in, stay, 0).build(), (Vec3)ppos, (double)80.0);
        }
    }

    public void m_6667_(DamageSource p_21014_) {
        this.dropLoot = false;
        super.m_6667_(p_21014_);
        if (!this.m_9236_().f_46443_) {
            this.trySearchProjectiles();
            for (ChesedBlockProjectile projectile : this.blockAttackProjectiles) {
                projectile.m_142687_(Entity.RemovalReason.DISCARDED);
            }
            this.removeMonoliths();
            this.killCrystals();
            this.lookingAtTarget = false;
            this.stopAllNonIdleAnimations();
            this.getAnimationSystem().startAnimation("DEATH", AnimationTicker.builder(BossAnims.CHESED_DEATH).build());
            Vec3 forward = this.m_20156_();
            Vec3 pos = this.m_20182_().m_82549_(forward.m_82542_(4.0, 0.0, 4.0)).m_82520_(0.0, 1.75, 0.0);
            CutsceneData cutsceneData = new CutsceneData().stopMode(CutsceneData.StopMode.AUTOMATIC).time(((Animation)BossAnims.CHESED_DEATH.get()).getAnimTime() + 40).addCameraPos(new CameraPos(pos, forward.m_82548_()));
            for (Player player : this.getCombatants(true)) {
                if (!(player instanceof ServerPlayer)) continue;
                ServerPlayer serverPlayer = (ServerPlayer)player;
                FDLibCalls.startCutsceneForPlayer((ServerPlayer)serverPlayer, (CutsceneData)cutsceneData);
            }
        }
    }

    private void passiveEntityPullInArena() {
        int pullRadius = 5;
        int arenaRadius = ARENA_RADIUS - 2;
        List entities = this.m_9236_().m_6443_(Entity.class, new AABB((double)(-arenaRadius - pullRadius), -2.0, (double)(-arenaRadius - pullRadius), (double)(arenaRadius + pullRadius), 20.0, (double)(arenaRadius + pullRadius)).m_82383_(this.m_20182_()), entity -> !(entity instanceof ChesedBossBuddy));
        for (Entity entity2 : entities) {
            Vec3 b;
            Vec3 h;
            double dist;
            if (entity2 instanceof ItemEntity || !((dist = (h = (b = this.m_20182_().m_82546_(entity2.m_20182_())).m_82542_(1.0, 0.0, 1.0)).m_82553_()) >= (double)arenaRadius) || !(dist <= (double)(arenaRadius + pullRadius))) continue;
            ClipContext clipContext = new ClipContext(entity2.m_20182_().m_82520_(0.0, (double)(entity2.m_20206_() / 2.0f), 0.0), this.m_20182_().m_82520_(0.0, 2.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
            BlockHitResult result = this.m_9236_().m_45547_(clipContext);
            if (result.m_6662_() != HitResult.Type.MISS) continue;
            Vec3 speed = b.m_82541_();
            boolean doParticles = false;
            if (entity2 instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)entity2;
                if (!serverPlayer.m_7500_() && !serverPlayer.m_5833_()) {
                    FDLibCalls.setServerPlayerSpeed((ServerPlayer)serverPlayer, (Vec3)speed);
                    doParticles = true;
                }
            } else {
                doParticles = true;
                entity2.m_20256_(speed);
            }
            if (!doParticles) continue;
            RushParticleOptions rush = new RushParticleOptions(speed.m_82548_(), new FDColor(0.8f, 1.0f, 1.0f, 1.0f), this.f_19796_.m_188501_() * 0.5f + 0.25f, 0.05f + this.f_19796_.m_188501_() * 0.025f, 2 + this.f_19796_.m_188503_(2));
            BallParticleOptions ball = BallParticleOptions.builder().color(40, 200 + this.f_19796_.m_188503_(30), 255).scalingOptions(0, 20, 20).size(0.25f).scalingOptions(2, 0, 4).build();
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)rush, entity2.m_20185_(), entity2.m_20186_() + (double)(entity2.m_20206_() / 2.0f), entity2.m_20189_(), 10, (double)(entity2.m_20205_() / 2.0f), (double)(entity2.m_20206_() / 2.0f), (double)(entity2.m_20205_() / 2.0f), 0.0);
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ball, entity2.m_20185_(), entity2.m_20186_() + (double)(entity2.m_20206_() / 2.0f), entity2.m_20189_(), 10, (double)(entity2.m_20205_() / 2.0f), (double)(entity2.m_20206_() / 2.0f), (double)(entity2.m_20205_() / 2.0f), 0.0);
        }
    }

    public void playInEarsSound(SoundEvent soundEvent, float volume, float pitch) {
        for (Player player : this.getCombatants(true, 10.0f)) {
            FDPacketHandler.INSTANCE.sendTo((Object)new PlaySoundInEarsPacket(soundEvent, pitch, volume), ((ServerPlayer)player).f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    private void stopAllNonIdleAnimations() {
        for (String ticker : new ArrayList(this.getAnimationSystem().getTickers().keySet())) {
            if (ticker.equals("IDLE")) continue;
            this.getAnimationSystem().stopAnimation(ticker);
        }
    }

    protected void m_6153_() {
        ++this.f_20919_;
        this.lookingAtTarget = false;
        if (!this.m_213877_()) {
            if (!this.m_9236_().m_5776_()) {
                if (this.f_20919_ >= ((Animation)BossAnims.CHESED_DEATH.get()).getAnimTime() + 50) {
                    BossSpawnerEntity spawnerEntity = this.getSpawner();
                    if (spawnerEntity != null) {
                        spawnerEntity.setActive(true);
                    }
                    this.m_142687_(Entity.RemovalReason.KILLED);
                } else if (this.f_20919_ == ((Animation)BossAnims.CHESED_DEATH.get()).getAnimTime() + 30) {
                    for (Player player : this.getCombatants(true)) {
                        if (!(player instanceof ServerPlayer)) continue;
                        ServerPlayer serverPlayer = (ServerPlayer)player;
                        FDLibCalls.sendScreenEffect((ServerPlayer)serverPlayer, (Supplier)FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), (int)5, (int)20, (int)10);
                    }
                } else if (this.f_20919_ == ((Animation)BossAnims.CHESED_DEATH.get()).getAnimTime()) {
                    this.dropLoot = true;
                    this.m_6668_(this.m_9236_().m_269111_().m_269264_());
                    for (Player combatant : this.getCombatants(true)) {
                        if (combatant.m_5833_()) continue;
                        BossCriteriaTriggers.BOSS_KILLED.trigger((ServerPlayer)combatant, (Entity)this);
                    }
                    if (this.captureDrops() != null) {
                        for (ItemEntity entity : this.captureDrops()) {
                            Vec3 rnd = new Vec3(1.0, 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
                            entity.m_20256_(rnd.m_82520_(0.0, 0.25, 0.0));
                        }
                    }
                }
            } else if (this.f_20919_ < 30) {
                this.spawnDyingParticles();
            } else if (this.f_20919_ == ((Animation)BossAnims.CHESED_DEATH.get()).getAnimTime() - 3) {
                Vec3 pos = this.m_20182_().m_82520_(0.0, (double)1.1f, 0.0);
                this.m_9236_().m_7106_((ParticleOptions)BallParticleOptions.builder().size(5.0f).scalingOptions(0, 0, 3).color(100, 230, 255).build(), pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
            } else if (this.f_20919_ == ((Animation)BossAnims.CHESED_DEATH.get()).getAnimTime()) {
                this.deathExplosionParticles();
            }
        }
    }

    protected boolean m_6125_() {
        return this.dropLoot;
    }

    private void deathExplosionParticles() {
        Vec3 pos = this.m_20182_().m_82520_(0.0, (double)1.1f, 0.0);
        this.m_9236_().m_7106_((ParticleOptions)BallParticleOptions.builder().size(5.0f).scalingOptions(3, 0, 3).color(100, 230, 255).build(), pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
        float speed = 1.0f;
        for (int i = 0; i < 300; ++i) {
            if (i % 2 == 0) {
                this.m_9236_().m_7106_((ParticleOptions)LightningParticleOptions.builder().color(20, 150 + this.f_19796_.m_188503_(50), 255).lifetime(30).maxLightningSegments(3).randomRoll(true).physics(true).build(), pos.f_82479_, pos.f_82480_, pos.f_82481_, (double)(this.f_19796_.m_188501_() * speed - speed / 2.0f), (double)(this.f_19796_.m_188501_() * speed - speed / 2.0f), (double)(this.f_19796_.m_188501_() * speed - speed / 2.0f));
            }
            this.m_9236_().m_7106_((ParticleOptions)BallParticleOptions.builder().color(40, 200 + this.f_19796_.m_188503_(30), 255).scalingOptions(0, 20, 20).physics(true).size(0.25f).build(), pos.f_82479_, pos.f_82480_, pos.f_82481_, (double)(this.f_19796_.m_188501_() * speed - speed / 2.0f), (double)(this.f_19796_.m_188501_() * speed - speed / 2.0f), (double)(this.f_19796_.m_188501_() * speed - speed / 2.0f));
        }
    }

    private void spawnDyingParticles() {
        for (int i = 0; i < 5; ++i) {
            Vec3 pos = new Vec3((double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f)).m_82541_().m_82542_(1.5, 1.5, 1.5).m_82520_(this.m_20185_() + (double)this.f_19796_.m_188501_() * 0.2 - 0.1, this.m_20186_() + (double)this.f_19796_.m_188501_() * 0.2 + (double)1.1f, this.m_20189_() + (double)this.f_19796_.m_188501_() * 0.2 - 0.1);
            this.m_9236_().m_7106_((ParticleOptions)LightningParticleOptions.builder().color(20, 150 + this.f_19796_.m_188503_(50), 255).lifetime(10).maxLightningSegments(3).randomRoll(true).build(), pos.f_82479_, pos.f_82480_, pos.f_82481_, (double)this.f_19796_.m_188501_() * 0.05 - 0.025, (double)this.f_19796_.m_188501_() * 0.05 - 0.025, (double)this.f_19796_.m_188501_() * 0.05 - 0.025);
            this.m_9236_().m_7106_((ParticleOptions)BallParticleOptions.builder().color(40, 200 + this.f_19796_.m_188503_(30), 255).scalingOptions(0, 10, 5).size(0.25f).build(), pos.f_82479_, pos.f_82480_, pos.f_82481_, (double)this.f_19796_.m_188501_() * 0.05 - 0.025, (double)this.f_19796_.m_188501_() * 0.05 - 0.025, (double)this.f_19796_.m_188501_() * 0.05 - 0.025);
        }
    }

    public void tickTeleportHome() {
        if (this.spawnPosition != null && this.m_20182_().m_82554_(this.spawnPosition) > 2.5) {
            this.m_6021_(this.spawnPosition.f_82479_, this.spawnPosition.f_82480_, this.spawnPosition.f_82481_);
        }
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

    private Vec3 getCenter() {
        return this.m_20182_().m_82520_(0.0, 1.3, 0.0);
    }

    public boolean isBelowHalfHP() {
        return (float)this.getRemainingHits() < (float)this.getBossMaxHits() / 2.0f;
    }

    private float targetDetectionRadius() {
        return 39.0f;
    }

    public int getBossMaxHits() {
        return BossConfigs.BOSS_CONFIG.get().chesedConfig.chesedMaxHits;
    }

    public int getRemainingHits() {
        return this.remainingHits;
    }

    public void decreaseHitCount(int amount) {
        this.remainingHits = Mth.m_14045_((int)(this.remainingHits - amount), (int)0, (int)this.getBossMaxHits());
        this.bossBar.broadcastEvent(0, amount * 10);
        if (this.remainingHits <= 0) {
            this.m_6074_();
        }
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(IS_ROLLING, (Object)false);
        this.f_19804_.m_135372_(IS_LAUNCHING_ORBS, (Object)false);
        this.f_19804_.m_135372_(IS_DRAINING_MONOLITHS, (Object)false);
        this.f_19804_.m_135372_(DRAIN_PERCENT, (Object)Float.valueOf(0.0f));
    }

    public void m_7380_(CompoundTag tag) {
        if (this.chain != null) {
            CompoundTag t = new CompoundTag();
            this.chain.save(t);
            tag.m_128365_("chain", (Tag)t);
        }
        if (this.bossSpawnerUUID != null) {
            tag.m_128362_("spawner", this.bossSpawnerUUID);
        }
        tag.m_128405_("bossHealth", this.remainingHits);
        this.bossInitializer.autoSave("initializer", tag);
        tag.m_128379_("alreadySpawned", this.alreadySpawned);
        tag.m_128405_("secondPhaseTicker", this.secondPhaseTicker);
        tag.m_128379_("secondPhaseBegun", this.secondPhaseAnimPlayed);
        super.m_7380_(tag);
    }

    public void m_7378_(CompoundTag tag) {
        this.secondPhaseTicker = tag.m_128451_("secondPhaseTicker");
        this.secondPhaseAnimPlayed = tag.m_128471_("secondPhaseBegun");
        this.alreadySpawned = tag.m_128471_("alreadySpawned");
        if (tag.m_128441_("spawner")) {
            this.bossSpawnerUUID = tag.m_128342_("spawner");
        }
        this.bossInitializer.autoLoad("initializer", tag);
        if (this.chain != null) {
            this.chain.load(tag.m_128469_("chain"));
        }
        this.remainingHits = tag.m_128441_("bossHealth") ? tag.m_128451_("bossHealth") : this.getBossMaxHits();
        super.m_7378_(tag);
    }

    public void m_6457_(ServerPlayer player) {
        super.m_6457_(player);
        this.bossBar.addPlayer(player);
    }

    public void m_6452_(ServerPlayer player) {
        super.m_6452_(player);
        this.bossBar.removePlayer(player);
    }

    public void setRolling(boolean state) {
        this.f_19804_.m_135381_(IS_ROLLING, (Object)state);
    }

    public boolean isRolling() {
        return (Boolean)this.f_19804_.m_135370_(IS_ROLLING);
    }

    public boolean isDrainingFromMonoliths() {
        return (Boolean)this.f_19804_.m_135370_(IS_DRAINING_MONOLITHS);
    }

    public void setDrainingMonoliths(boolean state) {
        this.f_19804_.m_135381_(IS_DRAINING_MONOLITHS, (Object)state);
    }

    public float getMonolithDrainPercent() {
        return ((Float)this.f_19804_.m_135370_(DRAIN_PERCENT)).floatValue();
    }

    public void setMonolithDrainPercent(float percent) {
        this.f_19804_.m_135381_(DRAIN_PERCENT, (Object)Float.valueOf(Mth.m_14036_((float)percent, (float)0.0f, (float)1.0f)));
    }

    public void sendPacketToCombatants(Object msg) {
        for (Player player : this.getCombatants(true)) {
            FDPacketHandler.INSTANCE.sendTo(msg, ((ServerPlayer)player).f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }

    public void m_20334_(double p_20335_, double p_20336_, double p_20337_) {
        super.m_20334_(0.0, 0.0, 0.0);
    }

    public void m_20256_(Vec3 p_20257_) {
        super.m_20256_(Vec3.f_82478_);
    }

    public int m_21529_() {
        return 5;
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
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            return (BossSpawnerEntity)serverLevel.m_8791_(this.bossSpawnerUUID);
        }
        return null;
    }

    public boolean m_6094_() {
        return false;
    }

    protected void m_6138_() {
        super.m_6138_();
    }

    public void m_5997_(double p_20286_, double p_20287_, double p_20288_) {
    }

    public void m_7334_(Entity entity) {
        if (!this.isRolling() && !(entity instanceof ChesedBossBuddy)) {
            Vec3 b = entity.m_20182_().m_82546_(this.m_20182_()).m_82541_().m_82520_(0.0, 0.3, 0.0);
            entity.m_20256_(b);
            entity.f_19812_ = true;
        }
    }

    protected void m_7324_(Entity entity) {
        if (!this.isRolling() && !(entity instanceof ChesedBossBuddy)) {
            Vec3 b = entity.m_20182_().m_82546_(this.m_20182_()).m_82542_(1.0, 0.0, 1.0).m_82541_().m_82520_(0.0, 0.5, 0.0);
            entity.m_20256_(b);
            entity.f_19812_ = true;
        }
    }

    public boolean m_21532_() {
        return true;
    }

    @Override
    public boolean onFDDespawn() {
        BossSpawnerEntity spawnerEntity = this.getSpawner();
        if (spawnerEntity != null) {
            spawnerEntity.setActive(true);
            return true;
        }
        return false;
    }
}

