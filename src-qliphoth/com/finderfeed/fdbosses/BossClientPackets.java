/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.ClientMixinHandler
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.broken_screen_effect.ShatteredScreenEffectHandler
 *  com.finderfeed.fdlib.systems.broken_screen_effect.ShatteredScreenSettings
 *  com.finderfeed.fdlib.systems.particle.CircleParticleProcessor
 *  com.finderfeed.fdlib.systems.particle.ParticleProcessor
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.EmitterProcessor
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterData
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterHandler
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.processors.CircleSpawnProcessor
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffect
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectOverlay
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle
 *  com.finderfeed.fdlib.systems.shake.DefaultShake
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.ScreenShake
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.FDUtil
 *  com.finderfeed.fdlib.util.client.particles.FDBlockParticleOptions
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$ParticleRenderTypesS
 *  com.mojang.blaze3d.platform.Window
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleEngine
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.client.sounds.SoundManager
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.AxisAngle4d
 *  org.joml.AxisAngle4f
 *  org.joml.Matrix4f
 *  org.joml.Quaterniond
 *  org.joml.Quaternionf
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.boss_codex.BossCodexScreen;
import com.finderfeed.fdbosses.client.boss_screen.BaseBossScreen;
import com.finderfeed.fdbosses.client.boss_screen.screen_definitions.BossScreens;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.client.particles.colored_jumping_particles.ColoredJumpingParticleOptions;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterSettings;
import com.finderfeed.fdbosses.content.entities.chesed_boss.radial_earthquake.RadialEarthquakeEntity;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.distortion_sphere.DistortionSphereEffect;
import com.finderfeed.fdbosses.content.entities.geburah.distortion_sphere.DistortionSphereEffectHandler;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_earthquake.GeburahEarthquake;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponRotation;
import com.finderfeed.fdbosses.content.entities.geburah.sins.ScreenFlashEffect;
import com.finderfeed.fdbosses.content.entities.geburah.sins.ScreenFlashEffectData;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthWeaknessHandler;
import com.finderfeed.fdbosses.content.items.chesed.PhaseSphereHandler;
import com.finderfeed.fdbosses.content.util.HorizontalCircleRandomDirections;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.ClientMixinHandler;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.broken_screen_effect.ShatteredScreenEffectHandler;
import com.finderfeed.fdlib.systems.broken_screen_effect.ShatteredScreenSettings;
import com.finderfeed.fdlib.systems.particle.CircleParticleProcessor;
import com.finderfeed.fdlib.systems.particle.ParticleProcessor;
import com.finderfeed.fdlib.systems.particle.particle_emitter.EmitterProcessor;
import com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterData;
import com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterHandler;
import com.finderfeed.fdlib.systems.particle.particle_emitter.processors.CircleSpawnProcessor;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffect;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectOverlay;
import com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle;
import com.finderfeed.fdlib.systems.shake.DefaultShake;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.ScreenShake;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.FDUtil;
import com.finderfeed.fdlib.util.client.particles.FDBlockParticleOptions;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4d;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BossClientPackets {
    private static Random random = new Random();

    public static void geburahSyncRayVisuals(int geburah, boolean rayState) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(geburah);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah1 = (GeburahEntity)entity;
            geburah1.setLaserVisualsState(rayState);
        }
    }

    public static void openQliphoticCodex() {
        Minecraft.m_91087_().m_91152_((Screen)new BossCodexScreen(true));
    }

    public static void openBossDossierScreenEntityType(EntityType<?> type, List<Item> drops) {
        BaseBossScreen baseBossScreen = BossScreens.getScreen(type, -1, drops);
        if (baseBossScreen != null) {
            Minecraft.m_91087_().m_91152_((Screen)baseBossScreen);
        }
    }

    public static void forceAttackEntity(int entityId) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            Minecraft.m_91087_().f_91072_.m_105223_((Player)Minecraft.m_91087_().f_91074_, (Entity)livingEntity);
            FDClientHelpers.getClientPlayer().m_6674_(InteractionHand.MAIN_HAND);
        }
    }

    public static void summonParticle(ParticleOptions particleOptions, double x, double y, double z, double xd, double yd, double zd, int lifetime) {
        ParticleEngine engine = Minecraft.m_91087_().f_91061_;
        Particle particle = engine.m_107370_(particleOptions, x, y, z, xd, yd, zd);
        particle.f_107225_ = lifetime;
    }

    public static void chesedItemUse(CompoundTag update, boolean noPhysics, boolean startedUsing) {
        Player player = FDClientHelpers.getClientPlayer();
        player.m_150110_().m_35950_(update);
        player.f_19794_ = noPhysics;
        PhaseSphereHandler.isUsingChesedItem = startedUsing;
    }

    public static void geburahScalesControllerSetDisplacement(int entityId, int displacement, int displacementTime) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah = (GeburahEntity)entity;
            geburah.getScalesController().setCurrentDisplacement(displacement, displacementTime);
        }
    }

    public static void geburahSinnedPacket(int entityId) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah = (GeburahEntity)entity;
            geburah.sinnedTicks = 25;
        }
    }

    public static void setPlayerMalkuthWeaknessAmount(int amount) {
        if (FDClientHelpers.getClientPlayer() != null) {
            MalkuthWeaknessHandler.setCurrentWeakness(FDClientHelpers.getClientPlayer(), amount);
        }
    }

    public static void closeDossierScreen() {
        Screen screen = Minecraft.m_91087_().f_91080_;
        if (screen instanceof BaseBossScreen) {
            BaseBossScreen baseBossScreen = (BaseBossScreen)screen;
            Minecraft.m_91087_().m_91152_(null);
        }
    }

    public static void syncPlayerSinsPacket(PlayerSins playerSins) {
        PlayerSins.setPlayerSins(FDClientHelpers.getClientPlayer(), playerSins);
    }

    public static void launchGeburahLaserPrepare(int geburah, int timeUntilAttack) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(geburah);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah1 = (GeburahEntity)entity;
            geburah1.laserAttackPreparator.launchPreparation(timeUntilAttack);
        }
    }

    public static void triggerSinEffect(float soundPitch) {
        SoundManager soundManager = Minecraft.m_91087_().m_91106_();
        soundManager.m_120367_((SoundInstance)SimpleSoundInstance.m_119755_((SoundEvent)BossSounds.GEBURAH_SIN.get(), (float)(soundPitch * (0.95f + random.nextFloat() * 0.1f)), (float)0.5f));
        ClientMixinHandler.addShake((ScreenShake)new DefaultShake(FDShakeData.builder().amplitude(0.5f).outTime(10).inTime(0).stayTime(0).build()));
        ResourceLocation data = ShatteredScreenSettings.DATA_1_GLASSY;
        ShatteredScreenEffectHandler.setCurrentEffect((ShatteredScreenSettings)new ShatteredScreenSettings(data, 0, 0, 40, 0.1f, 0.05f, true));
        ScreenEffectOverlay.addScreenEffect((ScreenEffect)new ScreenFlashEffect(new ScreenFlashEffectData(new FDColor(1.0f, 1.0f, 1.0f, 0.5f), 0.3f), 1, 0, 20));
    }

    public static void stopWeaponRotation(int entityId) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah = (GeburahEntity)entity;
            geburah.getWeaponRotationController().stopRotation();
        }
    }

    public static void startGeburahWeaponRotation(int entityId, GeburahWeaponRotation geburahWeaponRotation) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah = (GeburahEntity)entity;
            GeburahWeaponRotationController handler = geburah.getWeaponRotationController();
            geburahWeaponRotation.setRotatingWeaponsHandler(handler);
            handler.rotateWeapons(geburahWeaponRotation);
        }
    }

    public static void handleGeburahWeaponRotationSync(int entityId, float currentRotation) {
        GeburahEntity geburah;
        GeburahWeaponRotationController rotationWeapons;
        float rot;
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof GeburahEntity && Math.abs((rot = (rotationWeapons = (geburah = (GeburahEntity)entity).getWeaponRotationController()).getCurrentRotation()) - currentRotation) > 0.1f) {
            rotationWeapons.setRotation(currentRotation);
        }
    }

    public static void startGeburahDistortionEffect(int entityId) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah = (GeburahEntity)entity;
            Vec3 corePos = geburah.getCorePosition();
            Vec3 pos = geburah.m_20182_();
            DistortionSphereEffectHandler.setDistortionSphereEffect(new DistortionSphereEffect(corePos, 60, 64.0f, 2.0f, (float)pos.f_82480_));
        }
    }

    public static void openBossDossierScreen(int spawnerId, List<Item> drops) {
        BossSpawnerEntity bossSpawner;
        BaseBossScreen baseBossScreen;
        Level level = FDClientHelpers.getClientLevel();
        Entity entity = level.m_6815_(spawnerId);
        if (entity instanceof BossSpawnerEntity && (baseBossScreen = BossScreens.getScreen((bossSpawner = (BossSpawnerEntity)entity).getBossEntityType(), spawnerId, drops)) != null) {
            Minecraft.m_91087_().m_91152_((Screen)baseBossScreen);
        }
    }

    public static void malkuthSwordCharge(MalkuthAttackType malkuthAttackType, int entityId) {
        Level level = FDClientHelpers.getClientLevel();
        Entity entity = level.m_6815_(entityId);
        if (entity instanceof MalkuthEntity) {
            MalkuthEntity malkuthEntity = (MalkuthEntity)entity;
            String name = malkuthAttackType.isFire() ? "fire_sword_place" : "ice_sword_place";
            Matrix4f t = malkuthEntity.getModelPartTransformation((Entity)malkuthEntity, name, MalkuthEntity.getClientModel());
            Vector3f v1 = t.transformPosition(new Vector3f());
            Vector3f v2 = t.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
            Vector3d direction = new Vector3d((double)v2.x, (double)v2.y, (double)v2.z);
            Vector3d start = new Vector3d(malkuthEntity.m_20185_() + (double)v1.x, malkuthEntity.m_20186_() + (double)v1.y, malkuthEntity.m_20189_() + (double)v1.z);
            Vector3d end = start.add((Vector3dc)direction.mul(3.0, new Vector3d()), new Vector3d());
            Vector3d left = direction.cross((Vector3dc)new Vector3d(0.0, 1.0, 0.0), new Vector3d());
            Vector3d between = end.sub((Vector3dc)start, new Vector3d());
            int steps = 40;
            for (int i = 0; i < steps; ++i) {
                float b;
                float g;
                float r;
                float p = ((float)i + random.nextFloat()) / (float)steps;
                if (malkuthAttackType.isIce()) {
                    r = random.nextFloat() * 0.2f;
                    g = 0.7f + random.nextFloat() * 0.1f;
                    b = 0.9f + random.nextFloat() * 0.1f;
                } else {
                    r = 0.9f + random.nextFloat() * 0.1f;
                    g = 0.2f + random.nextFloat() * 0.2f;
                    b = random.nextFloat() * 0.2f;
                }
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(0.2f + random.nextFloat() * 0.1f).color(r, g, b).scalingOptions(0, 0, 10 + random.nextInt(4)).friction(0.6f).build();
                float rndAngle = (float)Math.PI * 2 * random.nextFloat();
                Quaterniond quaterniond = new Quaterniond(new AxisAngle4d((double)rndAngle, (Vector3dc)direction));
                Vector3d particleDirection = quaterniond.transform((Vector3dc)left, new Vector3d());
                Vector3d position = start.add((Vector3dc)between.mul((double)p, new Vector3d()), new Vector3d()).add(particleDirection.x * 0.5, particleDirection.y * 0.5, particleDirection.z * 0.5, new Vector3d());
                level.m_6493_((ParticleOptions)ballParticleOptions, true, position.x, position.y, position.z, particleDirection.x * (double)0.8f, particleDirection.y * (double)0.8f, particleDirection.z * (double)0.8f);
            }
        }
    }

    public static void geburahEarthquakeSpawnEarthshatters(int entityId, Vec3 direction, int radius, float angle) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof GeburahEarthquake) {
            GeburahEarthquake geburahEarthquake = (GeburahEarthquake)entity;
            geburahEarthquake.spawnEarthShattersOnRadius(radius, direction, angle);
            BossClientPackets.geburahEarthquakeParticles(geburahEarthquake.m_20182_(), radius, direction, angle);
        }
    }

    public static void geburahEarthquakeParticles(Vec3 tpos, int rad, Vec3 direction, float arcAngle) {
        Vec3 b = new Vec3((double)rad, 0.0, 0.0);
        float angle = rad != 0 ? 3.0f / (float)rad : (float)Math.PI * 2;
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        BlockPos prevPos = null;
        for (float i = 0.0f; i < (float)Math.PI * 2; i += angle) {
            Vec3 vec = b.m_82524_(i);
            if (FDMathUtil.angleBetweenVectors((Vec3)vec, (Vec3)direction) > (double)arcAngle) continue;
            Vec3 pos = tpos.m_82549_(vec);
            BlockPos ppos = FDMathUtil.vec3ToBlockPos((Vec3)pos);
            if (!ppos.equals(prevPos)) {
                Vec3 c = ppos.m_252807_();
                Vec3 dir = tpos.m_82546_(c).m_82542_(1.0, 0.0, 1.0).m_82541_();
                BlockState state = level.m_8055_(ppos);
                if (state.m_60795_()) continue;
                Vec3 sppos = new Vec3(c.f_82479_ + (double)(random.nextFloat() * 2.0f) - 1.0 - dir.f_82479_, c.f_82480_ + 0.1 + (double)random.nextFloat() * 0.19, c.f_82481_ + (double)(random.nextFloat() * 2.0f) - 1.0 - dir.f_82481_);
                Vec3 speed = dir.m_82524_(0.7853982f * (random.nextFloat() * 2.0f - 1.0f)).m_82542_(0.075, 0.0, 0.075).m_82520_(0.0, (double)(0.25f + random.nextFloat() * 0.2f), 0.0);
                FDBlockParticleOptions options = FDBlockParticleOptions.builder().lifetime(10 + random.nextInt(5)).state(state).quadSizeMultiplier(1.0f + random.nextFloat() * 0.2f).build();
                level.m_7106_((ParticleOptions)options, sppos.f_82479_, sppos.f_82480_, sppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
            prevPos = ppos;
        }
    }

    public static void posEvent(Vec3 pos, int event, int data) {
        switch (event) {
            case 1: {
                BossClientPackets.summonBlocksOutOfEarthParticles(pos, 5.0f);
                break;
            }
            case 2: {
                BossClientPackets.radialEarthquakeParticles(pos, data);
                break;
            }
            case 3: {
                BossClientPackets.rockfallParticles(pos, data);
                break;
            }
            case 4: {
                BossClientPackets.rayExplosion(pos, data);
                break;
            }
            case 5: {
                BossClientPackets.rayAttackSmoke(pos, data);
                break;
            }
            case 6: {
                BossClientPackets.chesedBoomParticles(pos, data);
                break;
            }
            case 7: {
                BossClientPackets.malkuthCannonShoot(pos, data);
                break;
            }
            case 8: {
                BossClientPackets.malkuthSwordChargeParticles(pos, data);
                break;
            }
            case 9: {
                BossClientPackets.malkuthFloat(data);
                break;
            }
            case 10: {
                BossClientPackets.malkuthFireballExplode(pos, data);
                break;
            }
            case 11: {
                BossClientPackets.volcanoErruptionParticles(pos, data);
                break;
            }
            case 12: {
                BossClientPackets.malkuthSwordInsertParticles(data);
                break;
            }
            case 13: {
                BossClientPackets.malkuthPlayerFireballExplode(pos, data);
                break;
            }
            case 23: {
                BossClientPackets.chesedAddEmitter(pos, data);
                break;
            }
            case 14: {
                BossClientPackets.geburahRayParticles(pos, data);
                break;
            }
            case 15: {
                BossClientPackets.geburahRayChargeParticles(pos, data);
                break;
            }
            case 16: {
                BossClientPackets.geburahWeaponsStartLaser(pos, data);
                break;
            }
            case 17: {
                BossClientPackets.judgementBirdRayParticles(pos, data);
                break;
            }
            case 18: {
                BossClientPackets.geburahTriggerSinPunishmentAttackEffect(pos, data);
                break;
            }
            case 19: {
                BossClientPackets.sinPunishmentParticles(pos, data);
                break;
            }
            case 20: {
                BossClientPackets.geburahPrepareParticles(pos, data);
                break;
            }
            case 21: {
                BossClientPackets.geburahPrepareRoundAndRoundRays(pos, data);
                break;
            }
            case 22: {
                BossClientPackets.divineGearRayParticles(pos, data);
            }
        }
    }

    public static void divineGearRayParticles(Vec3 pos, int data) {
        Level level = FDClientHelpers.getClientLevel();
        BallParticleOptions flash = BallParticleOptions.builder().color(1.0f, 0.8f, 0.2f).scalingOptions(1, 0, 2).brightness(2).size(7.0f).build();
        level.m_6493_((ParticleOptions)flash, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
        Vec3 direction = FDUtil.decodeDirection((int)data);
        Matrix4f mat = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)direction);
        float maxParticleSize = 1.5f;
        float minParticleSize = 0.1f;
        float particleFriction = 0.7f;
        int maxParticleRows = 10;
        int particleTravelTime = 100;
        float cumulativeSize = 0.0f;
        float travelDistWindow = 0.75f;
        float randomXZSpread = 0.15f;
        for (int i = 0; i < maxParticleRows; ++i) {
            float ip = (float)i / ((float)maxParticleRows - 1.0f);
            float size = FDMathUtil.lerp((float)minParticleSize, (float)maxParticleSize, (float)(1.0f - ip));
            int repetitionCount = Math.round(FDMathUtil.lerp((float)1.0f, (float)3.0f, (float)ip));
            int directionsCount = Math.round(FDMathUtil.lerp((float)1.0f, (float)7.0f, (float)ip));
            float r = FDMathUtil.lerp((float)1.0f, (float)1.0f, (float)ip);
            float gr = FDMathUtil.lerp((float)0.5f, (float)1.0f, (float)ip);
            float b = FDMathUtil.lerp((float)0.0f, (float)1.0f, (float)(ip * ip));
            for (int g = 0; g < repetitionCount; ++g) {
                for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, directionsCount, 1.0f)) {
                    float travelDistance = cumulativeSize + travelDistWindow * random.nextFloat();
                    float particleSpeed = BossClientPackets.calculateParticleSpeed(travelDistance, particleFriction, particleTravelTime);
                    Vec3 speed = BossUtil.matTransformDirectionVec3(mat, new Vec3(dir.f_82479_ * 0.5 + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread, (double)(3.0f - random.nextFloat() * 0.5f), dir.f_82481_ * 0.5 + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread).m_82541_().m_82542_((double)particleSpeed, (double)particleSpeed, (double)particleSpeed));
                    Vec3 spawnOffset = BossUtil.matTransformDirectionVec3(mat, dir.m_82542_(0.5, 0.5, 0.5));
                    BallParticleOptions ballParticleOptions = BallParticleOptions.builder().brightness(2).size(size).color(r, gr, b, 1.0f).friction(particleFriction).scalingOptions(0, 0, 20 + random.nextInt(10)).build();
                    level.m_6493_((ParticleOptions)ballParticleOptions, true, pos.f_82479_ + spawnOffset.f_82479_, pos.f_82480_ + spawnOffset.f_82480_, pos.f_82481_ + spawnOffset.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
                }
            }
            cumulativeSize += travelDistWindow;
        }
        for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, 12, 0.0f)) {
            float randomOffset = 0.3926991f + 0.3926991f * level.f_46441_.m_188501_();
            float f = level.f_46441_.m_188499_() ? 1.0f : -1.0f;
            float offset1 = 0.5f;
            float offset2 = 1.5f;
            Vec3 p1 = Vec3.f_82478_;
            Vec3 p2 = BossUtil.matTransformDirectionVec3(mat, dir.m_82524_(randomOffset *= f).m_82520_(dir.f_82479_ * (double)offset1, 3.0, dir.f_82481_ * (double)offset1).m_82520_((double)random.nextFloat() * 0.5 - 0.25, (double)(random.nextFloat() * 2.0f - 1.0f), (double)random.nextFloat() * 0.5 - 0.25));
            Vec3 p3 = BossUtil.matTransformDirectionVec3(mat, dir.m_82524_(-randomOffset).m_82520_(dir.f_82479_ * (double)offset2, 7.0, dir.f_82481_ * (double)offset2).m_82520_((double)random.nextFloat() * 0.5 - 0.25, (double)(random.nextFloat() * 2.0f - 1.0f), (double)random.nextFloat() * 0.5 - 0.25));
            StripeParticleOptions options = StripeParticleOptions.builder().startColor(new FDColor(1.0f, 0.4f, 0.1f, 1.0f)).endColor(new FDColor(1.0f, 0.8f, 0.8f, 1.0f)).lifetime(10 + random.nextInt(5)).lod(25).scale(0.1f).stripePercentLength(0.5f).endOutPercent(0.1f).startInPercent(0.1f).offsets(p1, p2, p3).build();
            level.m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
        }
    }

    public static void geburahPrepareRoundAndRoundRays(Vec3 p, int data) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(data);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah = (GeburahEntity)entity;
            geburah.triggerRaysShotPreparationEffect();
        }
    }

    public static void geburahPrepareParticles(Vec3 p, int data) {
        Level level = FDClientHelpers.getClientLevel();
        Entity entity = level.m_6815_(data);
        if (!(entity instanceof GeburahEntity)) {
            return;
        }
        GeburahEntity geburah = (GeburahEntity)entity;
        List<Pair<Vec3, Vec3>> positionsAndDirections = geburah.getCannonsPositionAndDirection();
        for (Pair<Vec3, Vec3> posAndDir : positionsAndDirections) {
            Vec3 pos = (Vec3)posAndDir.first;
            Vec3 direction = (Vec3)posAndDir.second;
            Matrix4f mat = new Matrix4f();
            FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)direction);
            for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, 6, 1.0f)) {
                dir = BossUtil.matTransformDirectionVec3(mat, dir);
                Vec3 startPos = pos.m_82549_(dir.m_82490_(1.5)).m_82549_(direction.m_82548_().m_82490_(2.0));
                Vec3 speed = direction.m_82490_(2.0).m_82549_(dir.m_82548_()).m_82490_((double)0.3f);
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().color(0.2f, 0.5f, 1.0f).brightness(2).friction(0.8f).size(0.25f).in(10).out(1).build();
                level.m_6493_((ParticleOptions)ballParticleOptions, true, startPos.f_82479_, startPos.f_82480_, startPos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
        }
    }

    public static void sinPunishmentParticles(Vec3 pos, int radius) {
        Level level = FDClientHelpers.getClientLevel();
        float length = (float)Math.PI * 2 * (float)radius;
        float step = 300.0f / length;
        float smokeDirStep = 0.3926991f;
        float minSmokeSize = 3.0f;
        float maxSmokeSize = 5.0f;
        float smokeFriction = 0.7f;
        for (float i = 0.0f; i < length; i += step) {
            float p = i / length;
            Vec3 direction = new Vec3(1.0, 0.0, 0.0).m_82524_((float)Math.PI * 2 * p + random.nextFloat() * (float)Math.PI / 16.0f);
            Vec3 between = direction.m_82490_((double)radius);
            Vec3 reversedDir = direction.m_82548_();
            Vec3 ppos = pos.m_82549_(between);
            float smokeTravelDistance = 0.5f;
            int smokeCount = 3;
            for (int c = 0; c < smokeCount; ++c) {
                float smokeP = (float)c / ((float)smokeCount - 1.0f);
                float particleSize = FDMathUtil.lerp((float)minSmokeSize, (float)maxSmokeSize, (float)(1.0f - smokeP));
                for (int dirswitch = -1; dirswitch <= 1; dirswitch += 2) {
                    for (int k = -4; k < 4; ++k) {
                        Vec3 smokeDir = direction.m_82490_((double)dirswitch).m_82524_((float)k * smokeDirStep);
                        float smokeSpeed = BossClientPackets.calculateParticleSpeed(smokeTravelDistance + random.nextFloat() * particleSize, smokeFriction, 40);
                        Vec3 speed = smokeDir.m_82490_((double)smokeSpeed);
                        float color = random.nextFloat() * 0.2f + 0.2f;
                        BigSmokeParticleOptions options = BigSmokeParticleOptions.builder().color(color, color, color).friction(smokeFriction).minSpeed(0.01f).size(particleSize).lifetime(0, 2, 100 + random.nextInt(60)).build();
                        level.m_7106_((ParticleOptions)options, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, (double)(random.nextFloat() * (1.0f - p) * 1.0f), speed.f_82481_);
                    }
                }
                smokeTravelDistance += particleSize;
            }
            for (int count = 0; count < 10; ++count) {
                for (int dirswitch = -1; dirswitch <= 1; dirswitch += 2) {
                    Vec3 randomOffset = direction.m_82490_((double)((float)dirswitch * random.nextFloat() * (float)radius / 2.0f)).m_82524_(0.7853982f * random.nextFloat());
                    float len = (float)randomOffset.m_82553_();
                    float pb = len / ((float)radius / 2.0f);
                    float ballSize = FDMathUtil.lerp((float)1.0f, (float)1.5f, (float)(1.0f - pb));
                    float ballYSpeed = FDMathUtil.lerp((float)(0.1f + random.nextFloat() * 0.2f), (float)(0.3f + random.nextFloat() * 0.6f), (float)FDEasings.easeOut((float)(1.0f - pb)));
                    Vec3 actualppos = ppos.m_82549_(randomOffset);
                    BallParticleOptions options = BallParticleOptions.builder().color(0.3f + random.nextFloat() * 0.1f, 0.8f, 1.0f).brightness(2).size(ballSize).friction(0.6f + random.nextFloat() * 0.2f).scalingOptions(2, 0, 20 + random.nextInt(40)).build();
                    Vec3 vspeed = direction.m_82490_((double)dirswitch).m_82490_((double)FDEasings.easeIn((float)pb));
                    level.m_7106_((ParticleOptions)options, actualppos.f_82479_, actualppos.f_82480_, actualppos.f_82481_, vspeed.f_82479_, (double)ballYSpeed, vspeed.f_82481_);
                }
            }
        }
        int hammersCount = 8;
        float angle = (float)Math.PI * 2 / (float)hammersCount;
        for (int i = 0; i < hammersCount; ++i) {
            Vec3 hammerOffset = new Vec3(1.0, 0.0, 0.0).m_82524_(angle * (float)i);
            Vec3 hammerImpactPos = pos.m_82549_(hammerOffset.m_82490_((double)radius));
            for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, 24, 0.5f)) {
                ColoredJumpingParticleOptions options = ColoredJumpingParticleOptions.builder().colorStart(new FDColor(0.5f, 0.8f, 1.0f, 1.0f)).colorEnd(new FDColor(1.0f, 1.0f, 1.0f, 1.0f)).maxJumpAmount(1).maxPointsInTrail(3).reflectionStrength(0.5f).size(0.05f).gravity(2.0f).lifetime(-1).build();
                float randomXZSpeed = random.nextFloat() * 0.3f + 0.3f;
                Vec3 speed = new Vec3(dir.f_82479_ * (double)randomXZSpeed, (double)(0.4f + random.nextFloat() * 0.2f), dir.f_82481_ * (double)randomXZSpeed);
                level.m_7106_((ParticleOptions)options, hammerImpactPos.f_82479_ + dir.f_82479_, hammerImpactPos.f_82480_, hammerImpactPos.f_82481_ + dir.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
        }
    }

    public static void geburahTriggerSinPunishmentAttackEffect(Vec3 pos, int data) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(data);
        if (!(entity instanceof GeburahEntity)) {
            return;
        }
        GeburahEntity geburah = (GeburahEntity)entity;
        geburah.sinPunishmentAttackTicker = 40;
    }

    public static void judgementBirdRayParticles(Vec3 pos, int data) {
        Level level = FDClientHelpers.getClientLevel();
        BallParticleOptions flash = BallParticleOptions.builder().color(0.5f, 0.8f, 1.0f).scalingOptions(1, 0, 2).brightness(4).size(8.0f).build();
        level.m_6493_((ParticleOptions)flash, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
        Vec3 direction = FDUtil.decodeDirection((int)data);
        Vec3 horizontalReversedDirection = direction.m_82542_(-1.0, 0.0, -1.0).m_82541_();
        Matrix4f mat = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)direction);
        float maxParticleSize = 0.5f;
        float minParticleSize = 0.1f;
        float particleFriction = 0.7f;
        int maxParticleRows = 6;
        int particleTravelTime = 100;
        float cumulativeSize = 0.0f;
        float cumulativeSize2 = 0.0f;
        float travelDistWindow = 0.5f;
        float travelDistWindow2 = 0.5f;
        float randomXZSpread = 0.1f;
        float angleSlamEarth = 0.7853982f;
        for (int i = 0; i < maxParticleRows; ++i) {
            float ip = (float)i / ((float)maxParticleRows - 1.0f);
            float size = FDMathUtil.lerp((float)minParticleSize, (float)maxParticleSize, (float)(1.0f - ip));
            int repetitionCount = Math.round(FDMathUtil.lerp((float)1.0f, (float)5.0f, (float)ip));
            int directionsCount = Math.round(FDMathUtil.lerp((float)1.0f, (float)6.0f, (float)ip));
            float r = FDMathUtil.lerp((float)0.0f, (float)1.0f, (float)(ip * ip));
            float gr = FDMathUtil.lerp((float)0.5f, (float)1.0f, (float)ip);
            float b = FDMathUtil.lerp((float)1.0f, (float)1.0f, (float)ip);
            for (int g = 0; g < repetitionCount; ++g) {
                for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, directionsCount, 1.0f)) {
                    float travelDistance = cumulativeSize + travelDistWindow * random.nextFloat();
                    float particleSpeed = BossClientPackets.calculateParticleSpeed(travelDistance, particleFriction, particleTravelTime);
                    Vec3 speed = BossUtil.matTransformDirectionVec3(mat, new Vec3(dir.f_82479_ + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread, (double)(3.0f - random.nextFloat() * 0.5f), dir.f_82481_ + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread).m_82541_().m_82542_((double)(particleSpeed += random.nextFloat() * 0.05f), (double)particleSpeed, (double)particleSpeed));
                    Vec3 spawnOffset = BossUtil.matTransformDirectionVec3(mat, dir.m_82542_(0.5, 0.5, 0.5));
                    BallParticleOptions ballParticleOptions = BallParticleOptions.builder().brightness(2).size(size).color(r, gr, b, 1.0f).friction(particleFriction).scalingOptions(0, 0, 20 + random.nextInt(10)).build();
                    level.m_6493_((ParticleOptions)ballParticleOptions, true, pos.f_82479_ + spawnOffset.f_82479_, pos.f_82480_ + spawnOffset.f_82480_, pos.f_82481_ + spawnOffset.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
                    double angleBetween = FDMathUtil.angleBetweenVectors((Vec3)horizontalReversedDirection, (Vec3)dir);
                    if (angleBetween > (double)angleSlamEarth) continue;
                    BallParticleOptions ballParticleOptions2 = BallParticleOptions.builder().brightness(2).size(size * 2.0f).color(r, gr, b, 1.0f).friction(particleFriction).scalingOptions(0, 0, 20 + random.nextInt(10)).build();
                    float travelDistance2 = cumulativeSize2 + travelDistWindow2 * random.nextFloat();
                    float particleSpeed2 = BossClientPackets.calculateParticleSpeed(travelDistance2, particleFriction, particleTravelTime);
                    Vec3 speed2 = new Vec3(dir.f_82479_ + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread, (double)(0.1f + random.nextFloat() * 0.5f), dir.f_82481_ + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread).m_82541_().m_82490_((double)particleSpeed2);
                    Vec3 spawnPos2 = pos.m_82549_(new Vec3(1.0, 0.0, 0.0).m_82524_((float)Math.PI * 2 * random.nextFloat()));
                    level.m_6493_((ParticleOptions)ballParticleOptions2, true, spawnPos2.f_82479_, spawnPos2.f_82480_, spawnPos2.f_82481_, speed2.f_82479_, speed2.f_82480_, speed2.f_82481_);
                }
            }
            cumulativeSize += travelDistWindow;
            cumulativeSize2 += travelDistWindow2;
        }
        int smokesCount = 10;
        float smokesFriction = 0.7f;
        int smokesTravelTime = 20;
        for (int i = 0; i < smokesCount; ++i) {
            float p = (float)i / (float)(smokesCount - 1);
            int count = Math.round(FDMathUtil.lerp((float)6.0f, (float)12.0f, (float)p));
            float distance = 0.3f * (float)i;
            for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, count, 1.0f)) {
                float particleSpeed = distance * (1.0f - smokesFriction) / (1.0f - (float)Math.pow(smokesFriction, smokesTravelTime));
                Vec3 pspeed = dir.m_82542_((double)particleSpeed, 0.0, (double)particleSpeed);
                float col = 0.2f + random.nextFloat() * 0.2f;
                BigSmokeParticleOptions options = BigSmokeParticleOptions.builder().color(col, col, col).friction(smokesFriction).size(1.0f).minSpeed(0.025f).lifetime(0, 0, 30).build();
                level.m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, pspeed.f_82479_, pspeed.f_82480_, pspeed.f_82481_);
            }
        }
        for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, 12, 1.0f)) {
            ColoredJumpingParticleOptions options = new ColoredJumpingParticleOptions.Builder().colorStart(new FDColor(1.0f, 1.0f, 1.0f, 1.0f)).colorEnd(new FDColor(0.3f, 0.8f, 1.0f, 1.0f)).maxPointsInTrail(5).reflectionStrength(0.33f).gravity(1.5f).lifetime(-1).maxJumpAmount(1).size(0.03f).build();
            float horizontalSpeed = random.nextFloat() * 0.25f + 0.1f;
            float verticalSpeed = random.nextFloat() * 0.1f + 0.25f;
            dir = dir.m_82546_(direction.m_82490_(0.75));
            level.m_7106_((ParticleOptions)options, pos.f_82479_, pos.f_82480_, pos.f_82481_, dir.f_82479_ * (double)horizontalSpeed, (double)verticalSpeed, dir.f_82481_ * (double)horizontalSpeed);
        }
    }

    public static void geburahWeaponsStartLaser(Vec3 pos, int data) {
        Level level = FDClientHelpers.getClientLevel();
        Entity entity = level.m_6815_(data);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah = (GeburahEntity)entity;
            float particleFriction = 0.8f;
            float r = 0.3f;
            float g = 0.7f;
            float b = 1.0f;
            float rowWidth = 0.5f;
            for (Pair<Vec3, Vec3> cannon : geburah.getCannonsPositionAndDirection()) {
                Vec3 cannonPos = (Vec3)cannon.first;
                Vec3 cannonDirection = (Vec3)cannon.second;
                Matrix4f mat = new Matrix4f();
                FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)cannonDirection);
                int rowCount = 5;
                float offs = 0.0f;
                for (int i = 0; i < rowCount; ++i) {
                    for (Vec3 dir : new HorizontalCircleRandomDirections(geburah.m_217043_(), 12, 1.0f)) {
                        float pspeed = BossClientPackets.calculateParticleSpeed(offs + random.nextFloat() * rowWidth, particleFriction, 10);
                        Vec3 desiredSpeed = new Vec3(dir.f_82479_ + (double)random.nextFloat() * 0.8 - 0.4, 3.0, dir.f_82481_ + (double)random.nextFloat() * 0.8 - 0.4).m_82541_().m_82490_((double)pspeed);
                        Vec3 sppos = BossUtil.matTransformDirectionVec3(mat, dir.m_82490_(0.25));
                        desiredSpeed = BossUtil.matTransformDirectionVec3(mat, desiredSpeed);
                        BallParticleOptions options = BallParticleOptions.builder().friction(particleFriction).size(0.5f).brightness(2).scalingOptions(0, 0, 10).color(r, g, b).build();
                        level.m_6493_((ParticleOptions)options, true, cannonPos.f_82479_ + sppos.f_82479_, cannonPos.f_82480_ + sppos.f_82480_, cannonPos.f_82481_ + sppos.f_82481_, desiredSpeed.f_82479_, desiredSpeed.f_82480_, desiredSpeed.f_82481_);
                    }
                    offs += rowWidth;
                }
            }
        }
    }

    public static void geburahRayChargeParticles(Vec3 pos, int data) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(data);
        if (entity instanceof GeburahEntity) {
            GeburahEntity geburah = (GeburahEntity)entity;
            pos = geburah.getCorePosition();
            int count = 3;
            float randomYRot = random.nextFloat() * (float)Math.PI * 2.0f;
            float randomXRot = random.nextFloat() * (float)Math.PI * 2.0f;
            float r = 1.0f;
            float gr = 0.49f;
            float b = 0.2f;
            Level level = FDClientHelpers.getClientLevel();
            for (int x = -count; x <= count; ++x) {
                for (int y = -count; y <= count; ++y) {
                    for (int z = -count; z <= count; ++z) {
                        if (random.nextFloat() > 0.05f) continue;
                        Vec3 v = new Vec3((double)x, (double)y, (double)z).m_82541_().m_82524_(randomYRot).m_82496_(randomXRot);
                        BallParticleOptions ballParticleOptions = BallParticleOptions.builder().brightness(2).size(0.5f).color(r, gr, b, 1.0f).friction(1.4f).scalingOptions(10, 0, 0).build();
                        Vec3 ppos = pos.m_82549_(v.m_82490_(2.0));
                        float speed = 0.05f;
                        level.m_6493_((ParticleOptions)ballParticleOptions, true, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, -v.f_82479_ * (double)speed, -v.f_82480_ * (double)speed, -v.f_82481_ * (double)speed);
                    }
                }
            }
        }
    }

    private static float calculateParticleSpeed(float travelDistance, float particleFriction, int particleTravelTime) {
        return travelDistance * (1.0f - particleFriction) / (1.0f - (float)Math.pow(particleFriction, particleTravelTime));
    }

    public static void geburahRayParticles(Vec3 pos, int data) {
        Level level = FDClientHelpers.getClientLevel();
        BallParticleOptions flash = BallParticleOptions.builder().color(1.0f, 0.8f, 0.2f).scalingOptions(1, 0, 2).brightness(4).size(15.0f).build();
        level.m_6493_((ParticleOptions)flash, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
        Vec3 direction = FDUtil.decodeDirection((int)data);
        Vec3 horizontalReversedDirection = direction.m_82542_(-1.0, 0.0, -1.0).m_82541_();
        Matrix4f mat = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)direction);
        float maxParticleSize = 2.0f;
        float minParticleSize = 0.1f;
        float particleFriction = 0.7f;
        int maxParticleRows = 10;
        int particleTravelTime = 100;
        float cumulativeSize = 0.0f;
        float cumulativeSize2 = 0.0f;
        float travelDistWindow = 1.0f;
        float travelDistWindow2 = 1.0f;
        float randomXZSpread = 0.25f;
        float angleSlamEarth = 0.7853982f;
        for (int i = 0; i < maxParticleRows; ++i) {
            float ip = (float)i / ((float)maxParticleRows - 1.0f);
            float size = FDMathUtil.lerp((float)minParticleSize, (float)maxParticleSize, (float)(1.0f - ip));
            int repetitionCount = Math.round(FDMathUtil.lerp((float)1.0f, (float)3.0f, (float)ip));
            int directionsCount = Math.round(FDMathUtil.lerp((float)1.0f, (float)7.0f, (float)ip));
            float r = FDMathUtil.lerp((float)1.0f, (float)1.0f, (float)ip);
            float gr = FDMathUtil.lerp((float)0.5f, (float)1.0f, (float)ip);
            float b = FDMathUtil.lerp((float)0.0f, (float)1.0f, (float)(ip * ip));
            for (int g = 0; g < repetitionCount; ++g) {
                for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, directionsCount, 1.0f)) {
                    float travelDistance = cumulativeSize + travelDistWindow * random.nextFloat();
                    float particleSpeed = BossClientPackets.calculateParticleSpeed(travelDistance, particleFriction, particleTravelTime);
                    Vec3 speed = BossUtil.matTransformDirectionVec3(mat, new Vec3(dir.f_82479_ + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread, (double)(3.0f - random.nextFloat() * 0.5f), dir.f_82481_ + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread).m_82541_().m_82542_((double)particleSpeed, (double)particleSpeed, (double)particleSpeed));
                    Vec3 spawnOffset = BossUtil.matTransformDirectionVec3(mat, dir.m_82542_(0.5, 0.5, 0.5));
                    BallParticleOptions ballParticleOptions = BallParticleOptions.builder().brightness(2).size(size).color(r, gr, b, 1.0f).friction(particleFriction).scalingOptions(0, 0, 20 + random.nextInt(10)).build();
                    level.m_6493_((ParticleOptions)ballParticleOptions, true, pos.f_82479_ + spawnOffset.f_82479_, pos.f_82480_ + spawnOffset.f_82480_, pos.f_82481_ + spawnOffset.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
                    double angleBetween = FDMathUtil.angleBetweenVectors((Vec3)horizontalReversedDirection, (Vec3)dir);
                    if (angleBetween > (double)angleSlamEarth) continue;
                    BallParticleOptions ballParticleOptions2 = BallParticleOptions.builder().brightness(2).size(size * 2.0f).color(r, gr, b, 1.0f).friction(particleFriction).scalingOptions(0, 0, 20 + random.nextInt(10)).build();
                    float travelDistance2 = cumulativeSize2 + travelDistWindow2 * random.nextFloat();
                    float particleSpeed2 = BossClientPackets.calculateParticleSpeed(travelDistance2, particleFriction, particleTravelTime);
                    Vec3 speed2 = new Vec3(dir.f_82479_ + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread, (double)(0.1f + random.nextFloat() * 0.5f), dir.f_82481_ + (double)(random.nextFloat() * randomXZSpread * 2.0f) - (double)randomXZSpread).m_82541_().m_82490_((double)particleSpeed2);
                    Vec3 spawnPos2 = pos.m_82549_(new Vec3(1.0, 0.0, 0.0).m_82524_((float)Math.PI * 2 * random.nextFloat()));
                    level.m_6493_((ParticleOptions)ballParticleOptions2, true, spawnPos2.f_82479_, spawnPos2.f_82480_, spawnPos2.f_82481_, speed2.f_82479_, speed2.f_82480_, speed2.f_82481_);
                }
            }
            cumulativeSize += travelDistWindow;
            cumulativeSize2 += travelDistWindow2;
        }
        int smokesCount = 10;
        float smokesFriction = 0.8f;
        int smokesTravelTime = 20;
        for (int i = 0; i < smokesCount; ++i) {
            float p = (float)i / (float)(smokesCount - 1);
            int count = Math.round(FDMathUtil.lerp((float)12.0f, (float)15.0f, (float)p));
            float distance = 0.8f * (float)i;
            for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, count, 1.0f)) {
                float particleSpeed = distance * (1.0f - smokesFriction) / (1.0f - (float)Math.pow(smokesFriction, smokesTravelTime));
                Vec3 pspeed = dir.m_82542_((double)particleSpeed, 0.0, (double)particleSpeed);
                float col = 0.2f + random.nextFloat() * 0.2f;
                BigSmokeParticleOptions options = BigSmokeParticleOptions.builder().color(col, col, col).friction(smokesFriction).size(4.0f).minSpeed(0.025f).lifetime(0, 0, 30).build();
                level.m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, pspeed.f_82479_, pspeed.f_82480_, pspeed.f_82481_);
            }
        }
        for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, 12, 0.0f)) {
            float randomOffset = 0.3926991f + 0.3926991f * level.f_46441_.m_188501_();
            float f = level.f_46441_.m_188499_() ? 1.0f : -1.0f;
            float offset1 = 2.0f;
            float offset2 = 3.0f;
            Vec3 p1 = Vec3.f_82478_;
            Vec3 p2 = BossUtil.matTransformDirectionVec3(mat, dir.m_82524_(randomOffset *= f).m_82520_(dir.f_82479_ * (double)offset1, 5.0, dir.f_82481_ * (double)offset1).m_82520_((double)random.nextFloat() * 0.5 - 0.25, (double)(random.nextFloat() * 2.0f - 1.0f), (double)random.nextFloat() * 0.5 - 0.25));
            Vec3 p3 = BossUtil.matTransformDirectionVec3(mat, dir.m_82524_(-randomOffset).m_82520_(dir.f_82479_ * (double)offset2, 10.0, dir.f_82481_ * (double)offset2).m_82520_((double)random.nextFloat() * 0.5 - 0.25, (double)(random.nextFloat() * 2.0f - 1.0f), (double)random.nextFloat() * 0.5 - 0.25));
            StripeParticleOptions options = StripeParticleOptions.builder().startColor(new FDColor(1.0f, 0.4f, 0.1f, 1.0f)).endColor(new FDColor(1.0f, 0.8f, 0.8f, 1.0f)).lifetime(10 + random.nextInt(5)).lod(25).scale(0.1f).stripePercentLength(0.5f).endOutPercent(0.1f).startInPercent(0.1f).offsets(p1, p2, p3).build();
            level.m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
        }
        for (Vec3 dir : new HorizontalCircleRandomDirections(level.f_46441_, 13, 1.0f)) {
            ColoredJumpingParticleOptions options = new ColoredJumpingParticleOptions.Builder().colorStart(new FDColor(1.0f, 1.0f, 1.0f, 1.0f)).colorEnd(new FDColor(1.0f, 0.8f, 0.3f, 1.0f)).maxPointsInTrail(5).reflectionStrength(0.33f).gravity(1.5f).lifetime(-1).maxJumpAmount(1).size(0.03f).build();
            float horizontalSpeed = random.nextFloat() * 0.5f + 0.2f;
            float verticalSpeed = random.nextFloat() * 0.3f + 0.3f;
            dir = dir.m_82546_(direction.m_82490_(0.75));
            level.m_7106_((ParticleOptions)options, pos.f_82479_, pos.f_82480_, pos.f_82481_, dir.f_82479_ * (double)horizontalSpeed, (double)verticalSpeed, dir.f_82481_ * (double)horizontalSpeed);
        }
    }

    public static void chesedAddEmitter(Vec3 pos, int entityId) {
        if (BossConfigs.BOSS_CONFIG_CLIENT.get().lessParticles) {
            return;
        }
        ParticleEmitterData emitterData = ParticleEmitterData.builder((ParticleOptions)BigSmokeParticleOptions.builder().color(0.25f, 0.25f, 0.25f).lifetime(0, 0, 100).size(1.0f).build()).lifetime(400).particlesPerTick(10).processor((EmitterProcessor)new CircleSpawnProcessor(new Vec3(0.0, -1.0, 0.0), 0.05f, 0.1f, 36.0f)).position(pos).build();
        ParticleEmitterHandler.addParticleEmitter((ParticleEmitterData)emitterData);
    }

    public static void malkuthSwordInsertParticles(int malkuthEntityId) {
        Level level = FDClientHelpers.getClientLevel();
        Entity entity = level.m_6815_(malkuthEntityId);
        if (!(entity instanceof MalkuthEntity)) {
            return;
        }
        MalkuthEntity malkuth = (MalkuthEntity)entity;
        Matrix4f iceSwordTransform = malkuth.getModelPartTransformation((Entity)malkuth, MalkuthEntity.getMalkuthSwordPlaceBone(MalkuthAttackType.ICE), MalkuthEntity.getClientModel());
        Matrix4f fireSwordTransform = malkuth.getModelPartTransformation((Entity)malkuth, MalkuthEntity.getMalkuthSwordPlaceBone(MalkuthAttackType.FIRE), MalkuthEntity.getClientModel());
        Vector3f v1 = iceSwordTransform.transformPosition(0.0f, 0.0f, 0.0f, new Vector3f());
        Vector3f v2 = fireSwordTransform.transformPosition(0.0f, 0.0f, 0.0f, new Vector3f());
        Vector3f vd1 = iceSwordTransform.transformDirection(0.0f, 1.0f, 0.0f, new Vector3f());
        Vector3f vd2 = fireSwordTransform.transformDirection(0.0f, 1.0f, 0.0f, new Vector3f());
        Vec3 posIce = malkuth.m_20182_().m_82520_((double)v1.x, (double)v1.y, (double)v1.z);
        Vec3 posFire = malkuth.m_20182_().m_82520_((double)v2.x, (double)v2.y, (double)v2.z);
        Vec3 dirIce = new Vec3((double)vd1.x, (double)vd1.y, (double)vd1.z);
        Vec3 dirFire = new Vec3((double)vd2.x, (double)vd2.y, (double)vd2.z);
        BossClientPackets.swordInsertParticles(MalkuthAttackType.ICE, posIce, dirIce);
        BossClientPackets.swordInsertParticles(MalkuthAttackType.FIRE, posFire, dirFire);
    }

    public static void swordInsertParticles(MalkuthAttackType type, Vec3 pos, Vec3 direction) {
        Matrix4f t = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)t, (Vec3)direction);
        Level level = FDClientHelpers.getClientLevel();
        float maxSize = 0.35f;
        float minSize = 0.1f;
        for (int i = 0; i < 200; ++i) {
            float angle = (float)Math.PI * 2 * random.nextFloat();
            float hoffset = random.nextFloat() * 0.5f;
            Vector3f v = new Vector3f(hoffset, 0.0f, 0.0f).rotateY(angle);
            v = t.transformDirection(v);
            Vec3 p = pos.m_82520_((double)v.x, (double)v.y, (double)v.z);
            float size = minSize + random.nextFloat() * (maxSize - minSize);
            Vector3f color = MalkuthEntity.getAndRandomizeColor(type, level.f_46441_);
            BallParticleOptions ballParticleOptions = BallParticleOptions.builder().scalingOptions(0, 0, 35 + random.nextInt(10)).size(size).color(color.x, color.y, color.z).brightness(2).friction(0.7f).build();
            float sizep = FDEasings.easeOut((float)(1.0f - size / maxSize));
            double speed = 0.2f + sizep * 2.0f * random.nextFloat();
            Vec3 sp = direction.m_82541_().m_82542_(speed, speed, speed);
            level.m_6493_((ParticleOptions)ballParticleOptions, true, p.f_82479_, p.f_82480_, p.f_82481_, sp.f_82479_ + (double)(random.nextFloat() * 0.5f) - 0.25, sp.f_82480_ + (double)(random.nextFloat() * 0.5f) - 0.25, sp.f_82481_ + (double)(random.nextFloat() * 0.5f) - 0.25);
            if (!(level.f_46441_.m_188501_() < 0.2f)) continue;
            ParticleType<GravityParticleOptions> ptype = type.isFire() ? BossParticles.FLAME_WITH_STONE.get() : BossParticles.ICE_CHUNK.get();
            GravityParticleOptions gravityParticleOptions = new GravityParticleOptions(ptype, 20 + random.nextInt(4), 0.6f + random.nextFloat() * 0.6f, 1.5999999f, 2.0f, true);
            level.m_6493_((ParticleOptions)gravityParticleOptions, true, p.f_82479_, p.f_82480_, p.f_82481_, sp.f_82479_ + (double)(random.nextFloat() * 0.5f) - 0.25, sp.f_82480_ + (double)(random.nextFloat() * 0.5f) - 0.25, sp.f_82481_ + (double)(random.nextFloat() * 0.5f) - 0.25);
        }
    }

    public static void volcanoErruptionParticles(Vec3 pos, int data) {
        Level level = FDClientHelpers.getClientLevel();
        float radius = (float)data / 16.0f;
        int amount = 30;
        float angle = (float)Math.PI * 2 / (float)amount;
        float maxBallSize = 1.0f;
        for (int i = 0; i < amount; ++i) {
            float rndAdd = random.nextFloat() * angle;
            float rndAdd2 = random.nextFloat() * angle;
            Vec3 offs = new Vec3(1.0, 0.0, 0.0).m_82524_(rndAdd + angle * (float)i);
            Vec3 offs2 = new Vec3(1.0, 0.0, 0.0).m_82524_(rndAdd2 + angle * (float)i);
            float aradius = radius - random.nextFloat() * 0.5f * radius;
            Vec3 ppos = pos.m_82549_(offs.m_82542_((double)aradius, (double)aradius, (double)aradius));
            Vec3 ppos2 = pos.m_82549_(offs2.m_82542_((double)radius, (double)radius, (double)radius));
            float size = 0.1f + random.nextFloat() * (maxBallSize - 0.1f);
            float sizeP = FDEasings.easeInOut((float)(size / maxBallSize));
            Vector3f color = MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.FIRE, level.f_46441_);
            BallParticleOptions ballParticleOptions = BallParticleOptions.builder().brightness(2).size(size).scalingOptions(0, 0, 20 + random.nextInt(10)).friction(0.9f - 0.1f * sizeP).color(color.x, color.y, color.z).build();
            level.m_6493_((ParticleOptions)ballParticleOptions, true, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, offs.f_82479_ * (double)(0.3f + random.nextFloat() * 0.2f), 1.0 + (double)random.nextFloat() * 0.5, offs.f_82481_ * (double)(0.3f + random.nextFloat() * 0.2f));
            if (random.nextBoolean()) {
                GravityParticleOptions options = new GravityParticleOptions(BossParticles.FLAME_WITH_STONE.get(), 20 + random.nextInt(4), 0.6f + random.nextFloat() * 0.6f, 1.5999999f, 2.0f, true);
                level.m_6493_((ParticleOptions)options, true, ppos2.f_82479_, ppos2.f_82480_, ppos2.f_82481_, offs2.f_82479_ * (double)(0.5f + random.nextFloat() * 0.2f), 0.5 + (double)random.nextFloat() * 0.5, offs2.f_82481_ * (double)(0.5f + random.nextFloat() * 0.2f));
            }
            if (!(random.nextFloat() > 0.8f)) continue;
            float col = 0.2f + random.nextFloat() * 0.1f;
            BigSmokeParticleOptions options = BigSmokeParticleOptions.builder().lifetime(0, 0, 20 + random.nextInt(10)).friction(0.8f).size(2.0f + random.nextFloat() * 2.0f).minSpeed(0.0025f).color(col, col, col).build();
            level.m_6493_((ParticleOptions)options, true, ppos2.f_82479_, ppos2.f_82480_, ppos2.f_82481_, offs2.f_82479_ * (double)(0.5f + random.nextFloat() * 0.2f), 0.5 + (double)random.nextFloat() * 0.5, offs2.f_82481_ * (double)(0.5f + random.nextFloat() * 0.2f));
        }
        Vec3 randomDir = new Vec3((double)radius, 0.0, 0.0).m_82524_((float)Math.PI * 2 * random.nextFloat());
        Vec3 randomDirN = randomDir.m_82541_();
        Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.FIRE);
        FDColor fireColorStart = new FDColor(colFire.x, colFire.y - random.nextFloat() * 0.1f - 0.3f, colFire.z, 0.5f);
        FDColor fireColor = new FDColor(colFire.x, colFire.y + random.nextFloat() * 0.1f, colFire.z, 1.0f);
        float firstMultiplier = 2.0f;
        float secondMultiplier = 8.0f;
        StripeParticleOptions stripeParticleOptions = StripeParticleOptions.builder().startColor(fireColorStart).endColor(fireColor).lifetime(10 + random.nextInt(10)).lod(50).scale(0.1f).stripePercentLength(0.5f).endOutPercent(0.2f).startInPercent(0.2f).offsets(Vec3.f_82478_, randomDirN.m_82542_((double)firstMultiplier, (double)firstMultiplier, (double)firstMultiplier).m_82520_((double)(random.nextFloat() * 2.0f - 1.0f), (double)(2.0f + random.nextFloat()), (double)(random.nextFloat() * 2.0f - 1.0f)), randomDirN.m_82542_((double)secondMultiplier, (double)secondMultiplier, (double)secondMultiplier).m_82520_((double)(random.nextFloat() * 2.0f - 1.0f), (double)(7.0f + random.nextFloat() * 3.0f), (double)(random.nextFloat() * 2.0f - 1.0f))).build();
        Vec3 stripePos = pos.m_82549_(randomDir);
        level.m_6493_((ParticleOptions)stripeParticleOptions, true, stripePos.f_82479_, stripePos.f_82480_, stripePos.f_82481_, 0.0, 0.0, 0.0);
    }

    public static void malkuthPlayerFireballExplode(Vec3 pos, int type) {
        Vec3 speed;
        MalkuthAttackType attackType = type == 1 ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
        Level level = FDClientHelpers.getClientLevel();
        for (int i = 0; i < 200; ++i) {
            Object options;
            Vector3f color = MalkuthEntity.getAndRandomizeColor(attackType, level.f_46441_);
            float speedMd = 1.0f;
            if (level.f_46441_.m_188501_() > 0.5f) {
                options = BallParticleOptions.builder().brightness(3).size(0.3f + random.nextFloat() * 0.2f).color(color.x, color.y, color.z).friction(0.7f).scalingOptions(0, 0, 10 + random.nextInt(10)).build();
            } else {
                speedMd = 0.5f;
                options = attackType.isFire() ? ((double)random.nextFloat() > 0.3 ? ParticleTypes.f_123744_ : ParticleTypes.f_123756_) : new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 20 + random.nextInt(4), 0.25f + random.nextFloat() * 0.2f, 0.79999995f, 2.0f, true);
            }
            float rnds = random.nextFloat() * 0.5f + 0.5f;
            speed = new Vec3((double)(random.nextFloat() * 2.0f - 1.0f), (double)(random.nextFloat() * 2.0f - 1.0f), (double)(random.nextFloat() * 2.0f - 1.0f)).m_82541_().m_82542_((double)(speedMd * rnds), (double)(speedMd * rnds), (double)(speedMd * rnds));
            float rnd = random.nextFloat();
            Vec3 ppos = pos.m_82549_(speed.m_82541_().m_82542_((double)rnd, (double)rnd, (double)rnd));
            level.m_7106_((ParticleOptions)options, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
        }
        Vector3f color = MalkuthEntity.getAndRandomizeColor(attackType, level.f_46441_);
        level.m_6493_((ParticleOptions)BallParticleOptions.builder().scalingOptions(1, 0, 2).brightness(2).size(10.0f).color(color.x, color.y, color.z).build(), true, pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0, 0.0, 0.0);
        for (int i = 0; i < 100; ++i) {
            float col = random.nextFloat() * 0.1f + 0.3f;
            BigSmokeParticleOptions options = BigSmokeParticleOptions.builder().color(col, col, col, 1.0f).size(1.0f + random.nextFloat() * 1.0f).minSpeed(0.01f).friction(0.7f).lifetime(0, 0, 25 + random.nextInt(5)).build();
            float rnd = random.nextFloat() * 1.0f + 0.5f;
            speed = new Vec3((double)(random.nextFloat() * 2.0f - 1.0f), (double)(random.nextFloat() * 2.0f - 1.0f), (double)(random.nextFloat() * 2.0f - 1.0f)).m_82541_().m_82542_((double)rnd, (double)rnd, (double)rnd);
            Vec3 ppos = pos.m_82549_(speed.m_82541_().m_82542_((double)0.15f, (double)0.15f, (double)0.15f));
            level.m_7106_((ParticleOptions)options, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
        }
    }

    public static void malkuthFireballExplode(Vec3 pos, int type) {
        int i;
        MalkuthAttackType attackType = type == 1 ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
        Level level = FDClientHelpers.getClientLevel();
        for (i = 0; i < 100; ++i) {
            Object options;
            Vector3f color = MalkuthEntity.getAndRandomizeColor(attackType, level.f_46441_);
            float vspeed = random.nextFloat() * 1.5f;
            float hspeed = FDEasings.easeOut((float)(1.5f - vspeed)) * 0.5f;
            if (level.f_46441_.m_188501_() > 0.5f) {
                options = BallParticleOptions.builder().brightness(3).size(0.1f + random.nextFloat() * 0.1f).color(color.x, color.y, color.z).friction(0.7f).scalingOptions(0, 0, 20 + random.nextInt(10)).build();
            } else if (attackType.isFire()) {
                vspeed *= 0.25f;
                hspeed *= 0.25f;
                options = (double)random.nextFloat() > 0.3 ? ParticleTypes.f_123744_ : ParticleTypes.f_123756_;
            } else {
                vspeed *= 0.75f;
                hspeed *= 0.75f;
                options = new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 20 + random.nextInt(4), 0.25f + random.nextFloat() * 0.2f, 0.79999995f, 2.0f, true);
            }
            Vec3 speed = new Vec3((double)vspeed, 0.0, 0.0).m_82524_(level.f_46441_.m_188501_() * (float)Math.PI * 2.0f).m_82520_(0.0, (double)hspeed, 0.0);
            float rnd = random.nextFloat();
            Vec3 ppos = pos.m_82549_(speed.m_82541_().m_82542_((double)rnd, (double)rnd, (double)rnd));
            level.m_7106_((ParticleOptions)options, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
        }
        for (i = 0; i < 30; ++i) {
            float col = random.nextFloat() * 0.1f + 0.3f;
            BigSmokeParticleOptions options = BigSmokeParticleOptions.builder().color(col, col, col, 1.0f).size(1.0f + random.nextFloat() * 1.0f).minSpeed(0.01f).friction(0.7f).lifetime(0, 0, 25 + random.nextInt(5)).build();
            Vec3 speed = new Vec3((double)(0.5f + level.f_46441_.m_188501_() * 1.0f), 0.0, 0.0).m_82524_((float)Math.PI * 2 * random.nextFloat());
            double hspeed = Math.sqrt(speed.f_82479_ * speed.f_82479_ + speed.f_82481_ * speed.f_82481_);
            Vec3 ppos = pos.m_82549_(speed.m_82541_().m_82542_((double)0.15f, (double)0.15f, (double)0.15f));
            level.m_7106_((ParticleOptions)options, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, speed.f_82480_ + (1.5 - hspeed) * (double)random.nextFloat(), speed.f_82481_);
        }
    }

    public static void malkuthFloat(int id) {
        ClientLevel clientLevel = (ClientLevel)FDClientHelpers.getClientLevel();
        Entity entity = clientLevel.m_6815_(id);
        if (entity instanceof MalkuthEntity) {
            MalkuthEntity malkuth = (MalkuthEntity)entity;
            Vec3 pos = malkuth.m_20182_();
            Vec3 old = new Vec3(malkuth.f_19854_, malkuth.f_19855_, malkuth.f_19856_);
            Vec3 between = old.m_82546_(pos);
            Vec3 nrm = between.m_82541_();
            double dist = between.m_82553_();
            float i = -1.0f;
            while ((double)i < dist) {
                Vec3 startPos = pos.m_82549_(nrm.m_82542_((double)i, (double)i, (double)i)).m_82520_(0.0, 2.0, 0.0);
                for (int g = 0; g < 10; ++g) {
                    Vector3f color = MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.getRandom(clientLevel.f_46441_), clientLevel.f_46441_);
                    float v = random.nextFloat() * 1.5f + 0.1f;
                    BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(0.3f - random.nextFloat() * 0.1f).scalingOptions(5, 0, random.nextInt(10) + 10).brightness(2).color(color.x, color.y, color.z).particleProcessor((ParticleProcessor)new CircleParticleProcessor(startPos.m_82520_(0.0, (double)(-v), 0.0), true, true, 1.0f)).build();
                    Vec3 horizontalOffset = new Vec3((double)(1.6f - v), 0.0, 0.0).m_82524_((float)Math.PI * 2 * random.nextFloat());
                    Vec3 ppos = startPos.m_82549_(horizontalOffset).m_82520_(0.0, (double)(-v), 0.0);
                    clientLevel.m_6493_((ParticleOptions)ballParticleOptions, true, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, 0.0, (double)(-random.nextFloat() * 0.1f), 0.0);
                }
                i += 1.0f;
            }
        }
    }

    public static void malkuthSwordChargeParticles(Vec3 encodedMalkuthAttackTypeISAIDDONTJUDGEME, int entityId) {
        Level level = FDClientHelpers.getClientLevel();
        Entity entity = level.m_6815_(entityId);
        if (entity instanceof MalkuthEntity) {
            MalkuthEntity malkuthEntity = (MalkuthEntity)entity;
            encodedMalkuthAttackTypeISAIDDONTJUDGEME = encodedMalkuthAttackTypeISAIDDONTJUDGEME.m_82546_(malkuthEntity.m_20182_());
            MalkuthAttackType malkuthAttackType = encodedMalkuthAttackTypeISAIDDONTJUDGEME.m_82526_(new Vec3(1.0, 0.0, 0.0)) > 0.0 ? MalkuthAttackType.FIRE : MalkuthAttackType.ICE;
            Vector3f color = MalkuthEntity.getMalkuthAttackPreparationParticleColor(malkuthAttackType);
            String boneName = MalkuthEntity.getMalkuthSwordPlaceBone(malkuthAttackType);
            FDModel clientModel = MalkuthEntity.getClientModel();
            Matrix4f boneTransform = malkuthEntity.getModelPartTransformation((Entity)malkuthEntity, boneName, clientModel);
            Vector3f swordDir = boneTransform.transformDirection(0.0f, 1.0f, 0.0f, new Vector3f());
            Vector3f swordDirUp = boneTransform.transformDirection(0.0f, 0.0f, -1.0f, new Vector3f());
            Vector3f swordPos = boneTransform.transformPosition(0.0f, 0.0f, 0.0f, new Vector3f());
            int totalParticles = 15;
            float startHeight = 0.5f;
            float swordBladeHeight = 1.75f;
            float particleSize = 0.15f;
            Vec3 swordWorldPosition = malkuthEntity.m_20182_().m_82520_((double)swordPos.x, (double)swordPos.y, (double)swordPos.z);
            for (int i = 0; i < totalParticles; ++i) {
                float p = (float)i / (float)(totalParticles - 1);
                float height = startHeight + p * swordBladeHeight + random.nextFloat() * 0.5f - 0.25f;
                Vec3 center = swordWorldPosition.m_82520_((double)(swordDir.x * startHeight + swordDir.x * height), (double)(swordDir.y * startHeight + swordDir.y * height), (double)(swordDir.z * startHeight + swordDir.z * height));
                Quaternionf quaternionf = new Quaternionf(new AxisAngle4f((float)Math.PI * 2 * random.nextFloat(), swordDir.x, swordDir.y, swordDir.z));
                Vector3f ppos = quaternionf.transform((Vector3fc)swordDirUp, new Vector3f()).mul(1.5f).add((float)center.f_82479_, (float)center.f_82480_, (float)center.f_82481_);
                float r = FDMathUtil.clamp((float)(color.x + random.nextFloat() * 0.2f), (float)0.0f, (float)1.0f);
                float g = FDMathUtil.clamp((float)(color.y + random.nextFloat() * 0.4f), (float)0.0f, (float)1.0f);
                float b = FDMathUtil.clamp((float)(color.z + random.nextFloat() * 0.2f), (float)0.0f, (float)1.0f);
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().color(r, g, b).size(particleSize).scalingOptions(10, 0, 5).brightness(2).particleProcessor((ParticleProcessor)new CircleParticleProcessor(center, true, true, 1.0f)).build();
                level.m_6493_((ParticleOptions)ballParticleOptions, true, (double)ppos.x, (double)ppos.y, (double)ppos.z, 0.0, 0.0, 0.0);
            }
        }
    }

    public static void malkuthCannonShoot(Vec3 pos, int data) {
        float p;
        int i;
        MalkuthAttackType malkuthAttackType = (data & 1) == 0 ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
        int directionData = data >> 1;
        Vec3 direction = FDUtil.decodeDirection((int)directionData);
        Vec3 left = direction.m_82537_(new Vec3(0.0, 1.0, 0.0)).m_82541_();
        Level level = FDClientHelpers.getClientLevel();
        int steps = 20;
        int totalParticlesPerAnglestep = 6;
        float anglestep = (float)Math.PI * 2 / (float)steps;
        for (float angle = 0.0f; angle <= (float)Math.PI * 2; angle += anglestep) {
            for (i = 0; i < totalParticlesPerAnglestep; ++i) {
                p = (float)i / (float)(totalParticlesPerAnglestep - 1);
                float cangle = angle + (random.nextFloat() - 0.5f) * anglestep;
                Quaternionf quaternionf = new Quaternionf(new AxisAngle4f(cangle, (float)direction.f_82479_, (float)direction.f_82480_, (float)direction.f_82481_));
                Vector3f particleDirection = quaternionf.transform(new Vector3f((float)left.f_82479_, (float)left.f_82480_, (float)left.f_82481_));
                float col = 0.2f + random.nextFloat() * 0.2f;
                BigSmokeParticleOptions bigSmokeParticleOptions = BigSmokeParticleOptions.builder().size(1.0f + random.nextFloat() * 0.5f).color(col, col, col).lifetime(0, 2, 30).friction(0.8f).minSpeed(0.025f).build();
                float dirspeed = 0.05f + random.nextFloat() * (0.5f + FDEasings.easeIn((float)p) * 0.25f);
                float sidespeed = 0.025f + FDEasings.easeIn((float)p) * 0.2f;
                level.m_6493_((ParticleOptions)bigSmokeParticleOptions, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, (double)(particleDirection.x * (sidespeed *= 1.5f)) + direction.f_82479_ * (double)(dirspeed *= 1.5f), (double)(particleDirection.y * sidespeed) + direction.f_82480_ * (double)dirspeed, (double)(particleDirection.z * sidespeed) + direction.f_82481_ * (double)dirspeed);
            }
        }
        int coloredParticlesCount = 30;
        for (i = 0; i < coloredParticlesCount; ++i) {
            float b;
            float g;
            float r;
            p = (float)i / (float)(coloredParticlesCount - 1);
            if (malkuthAttackType.isFire()) {
                r = 0.8f + random.nextFloat() * 0.2f;
                g = 0.3f + p * 0.5f;
                b = 0.1f + random.nextFloat() * 0.05f;
            } else {
                r = 0.1f + random.nextFloat() * 0.05f;
                g = 0.8f - p * 0.3f;
                b = 0.8f + random.nextFloat() * 0.2f;
            }
            Object options = random.nextFloat() > 0.75f ? BigSmokeParticleOptions.builder().size(1.0f + random.nextFloat() * 0.5f).color(r, g, b).lifetime(0, 2, 0).friction(0.8f).minSpeed(0.015f).build() : BallParticleOptions.builder().size(1.0f + random.nextFloat() * 0.5f).color(r, g, b).scalingOptions(0, 2, 0).friction(0.8f).build();
            Vec3 rnd = new Vec3((double)(random.nextFloat() * 2.0f - 1.0f), (double)(random.nextFloat() * 2.0f - 1.0f), (double)(random.nextFloat() * 2.0f - 1.0f)).m_82541_();
            float dirmod = 0.05f + p * 0.5f * 5.0f;
            float rndmod = 0.05f + p * 0.2f * 5.0f;
            Vec3 d = direction.m_82542_((double)dirmod, (double)dirmod, (double)dirmod).m_82549_(rnd.m_82542_((double)rndmod, (double)rndmod, (double)rndmod));
            level.m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, d.f_82479_, d.f_82480_, d.f_82481_);
        }
    }

    public static void chesedRayReflectParticles() {
        Window window = Minecraft.m_91087_().m_91268_();
        float w = window.m_85445_();
        float h = window.m_85446_();
        float spx = 10.0f;
        float spy = 40.0f;
        float friction = 0.4f;
        BossClientPackets.reflectParticlesPart(400, 0.0f, 0.0f, spx, spy, 250.0f, 2.0f, friction, 20.0f);
        BossClientPackets.reflectParticlesPart(400, w, 0.0f, -spx, spy, 250.0f, 2.0f, friction, 20.0f);
        BossClientPackets.reflectParticlesPart(400, w, h, -spx, -spy, 250.0f, 2.0f, friction, 20.0f);
        BossClientPackets.reflectParticlesPart(400, 0.0f, h, spx, -spy, 250.0f, 2.0f, friction, 20.0f);
    }

    public static void reflectParticlesPart(int count, float x, float y, float xd, float yd, float maxOffset, float maxSpeedMod, float friction, float size) {
        Vec3 offsetVector = new Vec3((double)xd, (double)yd, 0.0).m_82541_().m_82535_(1.5707964f);
        for (int i = 0; i < count; ++i) {
            float offset = (random.nextFloat() * 2.0f - 1.0f) * maxOffset;
            float xoff = offset * (float)offsetVector.f_82479_;
            float yoff = offset * (float)offsetVector.f_82480_;
            Vec3 sp = new Vec3((double)xd, (double)yd, 0.0).m_82535_(0.3926991f * (random.nextFloat() * 2.0f - 1.0f));
            float speedMod = maxSpeedMod * random.nextFloat();
            float sizeP = 1.0f - speedMod / maxSpeedMod;
            float frictionP = speedMod / maxSpeedMod;
            frictionP = frictionP * friction + 0.3f;
            ((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)FDTexturedSParticle.create((Function)FDRenderUtil.ParticleRenderTypesS.TEXTURES_BLUR_ADDITIVE, (ResourceLocation)BallParticle.LOCATION).setPos((double)(x + xoff), (double)(y + yoff), true)).setMaxQuadSize(size * sizeP).setSpeed(sp.f_82479_ * (double)speedMod, sp.f_82480_ * (double)speedMod)).setFriction((double)frictionP)).setColor(0.1f + random.nextFloat() * 0.1f - 0.05f, 0.8f + random.nextFloat() * 0.1f - 0.05f, 0.8f + random.nextFloat() * 0.1f - 0.05f, 0.8f)).setLifetime(15 + random.nextInt(5))).setQuadScaleOptions(ComplexEasingFunction.builder().addArea(1.0f, FDEasings::reversedEaseOut).build()).sendToOverlay();
        }
    }

    public static void chesedBoomParticles(Vec3 pos, int radiusFromCenter) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        int amount = 30;
        int amountPerAmount = 30;
        float angle = (float)Math.PI * 2 / (float)amount;
        for (int i = 0; i < amount; ++i) {
            for (int c = 0; c < amountPerAmount; ++c) {
                Vec3 v = new Vec3(36.0, 0.0, 0.0).m_82524_(angle * (float)i + (random.nextFloat() * 2.0f - 1.0f) * angle / 2.0f);
                Vec3 direction = v.m_82548_().m_82541_();
                float p = (float)c / (float)(amountPerAmount - 1);
                float size = (1.0f - p) * 2.0f + 1.0f;
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().friction(0.7f).size(size).color(100 + random.nextInt(50), 255, 255).scalingOptions(3, 0, 100).build();
                Vec3 ballParticlePos = pos.m_82549_(v).m_82520_(0.0, (double)(random.nextFloat() * 6.0f), 0.0);
                float power = 4.0f;
                float verticalBallParticleSpeed = power / 2.0f * p * 0.5f;
                float ballParticleSpeedF = power * p * 2.0f;
                Vec3 ballParticleSpeed = new Vec3(direction.f_82479_, (double)verticalBallParticleSpeed, direction.f_82481_).m_82542_((double)ballParticleSpeedF, 1.0, (double)ballParticleSpeedF).m_82524_(0.7853982f * (random.nextFloat() * 2.0f - 1.0f));
                level.m_6493_((ParticleOptions)ballParticleOptions, true, ballParticlePos.f_82479_, ballParticlePos.f_82480_, ballParticlePos.f_82481_, ballParticleSpeed.f_82479_, ballParticleSpeed.f_82480_, ballParticleSpeed.f_82481_);
                float h = p * p * p * p * 8.0f;
                float verticalSpeed = (random.nextFloat() * 2.0f - 1.0f) * 0.1f;
                float horizontalSpeed = (1.0f - p) * 1.5f;
                Vec3 speed = new Vec3(0.0, (double)verticalSpeed, 0.0).m_82549_(direction.m_82542_((double)horizontalSpeed, (double)horizontalSpeed, (double)horizontalSpeed)).m_82524_(0.7853982f * (random.nextFloat() * 2.0f - 1.0f));
                float friction = 0.85f;
                int cr = random.nextInt(50);
                BigSmokeParticleOptions smokeParticleOptions = BigSmokeParticleOptions.builder().size(8.0f).friction(friction).minSpeed(0.0f).color(50 + cr, 50 + cr, 50 + cr).lifetime(0, 60, 100 + random.nextInt(20)).build();
                level.m_6493_((ParticleOptions)smokeParticleOptions, true, pos.f_82479_ + v.f_82479_, pos.f_82480_ + v.f_82480_ + (double)h, pos.f_82481_ + v.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
        }
    }

    public static void rayAttackSmoke(Vec3 pos, int data) {
        Vec3 directionVector = FDUtil.decodeDirection((int)data);
        Matrix4f mt = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mt, (Vec3)directionVector);
        int camount = 20;
        float angle = (float)Math.PI * 2 / (float)camount;
        int ramount = 5;
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        for (int i = 0; i < camount; ++i) {
            for (int k = 0; k < ramount; ++k) {
                Vector3f direction = new Vector3f(1.0f, 0.0f, 0.0f).rotateY((float)i * angle + (random.nextFloat() * 2.0f - 1.0f) * angle / 2.0f);
                mt.transformPosition(direction);
                float strength = (float)k * 0.95f + (random.nextFloat() * 2.0f - 1.0f) * 0.35f;
                float friction = 0.65f;
                Vec3 speed = new Vec3((double)(direction.x * strength), (double)(direction.y * strength), (double)(direction.z * strength)).m_82549_(directionVector.m_82542_(0.15 + (double)(1.0f * random.nextFloat()), 0.15 + (double)(1.0f * random.nextFloat()), 0.15 + (double)(1.0f * random.nextFloat())));
                int cr = random.nextInt(50);
                BigSmokeParticleOptions options = BigSmokeParticleOptions.builder().size(5.0f).friction(friction).minSpeed(0.025f).color(50 + cr, 50 + cr, 50 + cr).lifetime(0, 20, 50).build();
                level.m_6493_((ParticleOptions)options, true, pos.f_82479_ + (double)direction.x, pos.f_82480_ + (double)direction.y, pos.f_82481_ + (double)direction.z, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
        }
    }

    public static void rayExplosion(Vec3 pos, int data) {
        int dx = (data & 0xFF0000) >> 16;
        int dy = (data & 0xFF00) >> 8;
        int dz = data & 0xFF;
        Vec3 direction = new Vec3((double)dx / 255.0 * 2.0 - 1.0, (double)dy / 255.0 * 2.0 - 1.0, (double)dz / 255.0 * 2.0 - 1.0);
        Matrix4f mt = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mt, (Vec3)direction);
        float sizeMod = (float)(data >> 28 & 0xF) / 15.0f;
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        int maxCount = data >> 24 & 0xF;
        int maxParticlePerCount = 15;
        float maxVerticalSpeed = 5.0f;
        float maxHorizontalSpeed = maxVerticalSpeed / 4.0f;
        float maxFriction = 0.7f;
        float maxSize = 2.5f * sizeMod;
        for (int i = 0; i < maxCount; ++i) {
            float p = (float)i / 10.0f;
            float angle = p * (float)Math.PI * 2.0f;
            float rangle = (float)Math.PI * 2 / (float)maxCount / 2.0f;
            for (int g = 0; g < maxParticlePerCount; ++g) {
                Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f).rotateY(angle + random.nextFloat() * rangle * 2.0f - rangle);
                Vector3f additionPos = new Vector3f((Vector3fc)dir);
                mt.transformPosition(additionPos);
                Vector3f ppos = new Vector3f((float)pos.f_82479_ + additionPos.x, (float)pos.f_82480_ + additionPos.y, (float)pos.f_82481_ + additionPos.z);
                float p2 = (float)g / (float)(maxParticlePerCount - 1);
                float size = maxSize / 2.0f * (1.0f - p2) + maxSize / 2.0f;
                float yspeed = maxVerticalSpeed * p2 + random.nextFloat() * maxVerticalSpeed / 5.0f;
                float zxspeedAddition = random.nextFloat() * maxHorizontalSpeed * 2.0f - maxHorizontalSpeed;
                float zxspeed = (maxHorizontalSpeed + zxspeedAddition / 4.0f) * p2;
                BallParticleOptions options = BallParticleOptions.builder().friction(maxFriction).size(size).color(100 + random.nextInt(50), 255, 255).scalingOptions(3, 0, 50).build();
                Vector3f I______Am_Speed = new Vector3f(dir.x * zxspeed, yspeed, dir.z * zxspeed);
                mt.transformPosition(I______Am_Speed);
                level.m_6493_((ParticleOptions)options, true, (double)ppos.x, (double)ppos.y, (double)ppos.z, (double)I______Am_Speed.x, (double)I______Am_Speed.y, (double)I______Am_Speed.z);
            }
        }
    }

    public static void rockfallParticles(Vec3 tpos, int maxRad) {
        for (int rad = 0; rad < maxRad; ++rad) {
            Vec3 b = new Vec3((double)rad, 0.0, 0.0);
            float angle = rad != 0 ? 0.5f / (float)rad : (float)Math.PI * 2;
            ClientLevel level = Minecraft.m_91087_().f_91073_;
            for (float i = 0.0f; i <= (float)Math.PI * 2; i += angle) {
                Vec3 v = b.m_82524_(i);
                level.m_6493_((ParticleOptions)ParticleTypes.f_123778_, true, tpos.f_82479_ + v.f_82479_ + (double)random.nextFloat() - 0.5, tpos.f_82480_ + v.f_82480_ + (double)random.nextFloat() * 0.1 - 0.05, tpos.f_82481_ + v.f_82481_ + (double)random.nextFloat() - 0.5, (double)random.nextFloat() * 0.025 - 0.0125, -0.015 - (double)random.nextFloat() * 0.015, (double)random.nextFloat() * 0.025 - 0.0125);
            }
        }
    }

    public static void spawnRadialEarthquakeShatters(int entityId, int radius) {
        Entity entity = FDClientHelpers.getClientLevel().m_6815_(entityId);
        if (entity instanceof RadialEarthquakeEntity) {
            RadialEarthquakeEntity radialEarthquakeEntity = (RadialEarthquakeEntity)entity;
            radialEarthquakeEntity.spawnEarthShattersOnRadius(radius);
        }
    }

    public static void radialEarthquakeParticles(Vec3 tpos, int rad) {
        if (BossConfigs.BOSS_CONFIG_CLIENT.get().lessParticles) {
            return;
        }
        Vec3 b = new Vec3((double)rad, 0.0, 0.0);
        float angle = rad != 0 ? 1.0f / (float)rad : (float)Math.PI * 2;
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        BlockPos prevPos = null;
        for (float i = 0.0f; i < (float)Math.PI * 2; i += angle) {
            Vec3 pos = tpos.m_82549_(b.m_82524_(i));
            BlockPos ppos = FDMathUtil.vec3ToBlockPos((Vec3)pos);
            if (!ppos.equals(prevPos)) {
                Vec3 c = ppos.m_252807_();
                Vec3 dir = tpos.m_82546_(c).m_82542_(1.0, 0.0, 1.0).m_82541_();
                BlockState state = level.m_8055_(ppos);
                if (state.m_60795_()) continue;
                Vec3 sppos = new Vec3(c.f_82479_ + (double)(random.nextFloat() * 2.0f) - 1.0 - dir.f_82479_, c.f_82480_ + 0.1 + (double)random.nextFloat() * 0.19, c.f_82481_ + (double)(random.nextFloat() * 2.0f) - 1.0 - dir.f_82481_);
                Vec3 speed = dir.m_82524_(0.7853982f * (random.nextFloat() * 2.0f - 1.0f)).m_82542_(0.075, 0.0, 0.075).m_82520_(0.0, (double)(0.25f + random.nextFloat() * 0.2f), 0.0);
                FDBlockParticleOptions options = FDBlockParticleOptions.builder().lifetime(10 + random.nextInt(5)).state(state).quadSizeMultiplier(1.0f + random.nextFloat() * 0.2f).build();
                level.m_7106_((ParticleOptions)options, sppos.f_82479_, sppos.f_82480_, sppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
            }
            prevPos = ppos;
        }
    }

    public static void summonBlocksOutOfEarthParticles(Vec3 pos, float radius) {
        Vec3 p = new Vec3((double)radius, 0.0, 0.0);
        float rotationAngle = 1.0f / radius;
        Vec3 poss = pos.m_82549_(p);
        for (float i = 0.0f; i <= (float)Math.PI * 2; i += rotationAngle) {
            Vec3 pose = pos.m_82549_(p.m_82524_(i + rotationAngle));
            BlockPos bpos = FDMathUtil.vec3ToBlockPos((Vec3)poss).m_7495_();
            BlockState state = Minecraft.m_91087_().f_91073_.m_8055_(bpos);
            if (state.m_60795_()) continue;
            Vec3 between = pose.m_82546_(poss).m_82541_();
            for (int k = 0; k < 5 + random.nextInt(5); ++k) {
                FDBlockParticleOptions options = FDBlockParticleOptions.builder().state(state).quadSizeMultiplier(1.5f + random.nextFloat() * 0.5f).lifetime(30 + random.nextInt(20)).build();
                float speedMod = random.nextFloat() + 0.5f;
                Minecraft.m_91087_().f_91073_.m_6493_((ParticleOptions)options, true, poss.f_82479_ + (double)(random.nextFloat() * 2.0f) - 1.0, poss.f_82480_, poss.f_82481_ + (double)(random.nextFloat() * 2.0f) - 1.0, between.f_82479_ * 0.3 * (double)speedMod, (double)(random.nextFloat() * 0.8f + 0.1f), between.f_82481_ * 0.3 * (double)speedMod);
            }
            poss = pose;
        }
    }

    public static void blockProjectileSlamParticles(SlamParticlesPacket.SlamData slamData) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        List<BlockState> states = BossClientPackets.collectStatesForSlam((Level)level, slamData.collectRadius, slamData.bPos);
        if (states.isEmpty()) {
            return;
        }
        Vec3 horizontal = slamData.direction.m_82542_(1.0, 0.0, 1.0).m_82541_();
        float angle = slamData.maxAngle / (float)slamData.count;
        float half = slamData.maxAngle / 2.0f;
        for (int i = 0; i <= slamData.count; ++i) {
            float a = -half + angle * (float)i;
            Vec3 rot = horizontal.m_82524_(a + (random.nextFloat() * 2.0f - 1.0f) * angle * 0.5f);
            float p = 1.0f - Math.abs(a) / half;
            p = FDEasings.easeOut((float)p);
            float verticalSpeed = FDMathUtil.lerp((float)slamData.maxVerticalSpeedEdges, (float)slamData.maxVerticalSpeedCenter, (float)p);
            int rowParticleCount = Math.round(slamData.maxSpeed / slamData.perRowDivide);
            for (int k = 1; k <= rowParticleCount + 1; ++k) {
                float percent = (float)k / (float)rowParticleCount;
                float sp = slamData.maxSpeed * percent + (random.nextFloat() * 2.0f - 1.0f) * slamData.perRowDivide;
                float vsp = verticalSpeed * percent + random.nextFloat() * verticalSpeed * -0.5f;
                int rnd = slamData.maxParticleLifetime / 4;
                if (rnd != 0) {
                    rnd = random.nextInt(rnd);
                }
                int lifetime = slamData.maxParticleLifetime - rnd;
                FDBlockParticleOptions options = FDBlockParticleOptions.builder().state(states.get(random.nextInt(states.size()))).lifetime(lifetime).quadSizeMultiplier(slamData.particleSizeMult).build();
                level.m_6493_((ParticleOptions)options, true, slamData.pos.f_82479_, slamData.pos.f_82480_, slamData.pos.f_82481_, rot.f_82479_ * (double)sp, (double)vsp, rot.f_82481_ * (double)sp);
            }
        }
    }

    private static List<BlockState> collectStatesForSlam(Level level, int collectRadius, BlockPos pos) {
        ArrayList<BlockState> states = new ArrayList<BlockState>();
        for (int x = -collectRadius; x <= collectRadius; ++x) {
            for (int y = -collectRadius; y <= collectRadius; ++y) {
                for (int z = -collectRadius; z <= collectRadius; ++z) {
                    BlockPos p = pos.m_7918_(x, y, z);
                    BlockState state = level.m_8055_(p);
                    if (state.m_60795_()) continue;
                    states.add(state);
                }
            }
        }
        return states;
    }

    public static void handleEarthShatterSpawnPacket(int entityId, EarthShatterSettings settings) {
        Entity entity = Minecraft.m_91087_().f_91073_.m_6815_(entityId);
        if (entity instanceof EarthShatterEntity) {
            EarthShatterEntity entity2 = (EarthShatterEntity)entity;
            entity2.settings = settings;
        }
    }

    public static Player getClientPlayer() {
        return Minecraft.m_91087_().f_91074_;
    }
}

