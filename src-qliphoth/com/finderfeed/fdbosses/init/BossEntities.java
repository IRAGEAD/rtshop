/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EntityType$Builder
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.MobCategory
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraftforge.event.entity.EntityAttributeCreationEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.registries.IForgeRegistry
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.entities.EyeOfChesedEntity;
import com.finderfeed.fdbosses.content.entities.EyeOfMalkuthEntity;
import com.finderfeed.fdbosses.content.entities.base.BossMonsterMob;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossSpawner;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedFireTrailEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_crystal.ChesedCrystalEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_mini_ray.ChesedMiniRay;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_monolith.ChesedMonolith;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_one_shot_vertical_ray.ChesedOneShotVerticalRayEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_vertical_ray.ChesedMovingVerticalRay;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.electric_sphere.ChesedElectricSphereEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.falling_block.ChesedFallingBlock;
import com.finderfeed.fdbosses.content.entities.chesed_boss.flying_block_entity.FlyingBlockEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.kinetic_field.ChesedKineticFieldEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.radial_earthquake.RadialEarthquakeEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ray_reflector.ChesedRayReflector;
import com.finderfeed.fdbosses.content.entities.chesed_sword_buff.FlyingSwordEntity;
import com.finderfeed.fdbosses.content.entities.geburah.EyeOfGeburah;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahBossSpawner;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircleJudgementBird;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahChainTrapCastCircle;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahRayCastingCircle;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahSinCrystalCastCircle;
import com.finderfeed.fdbosses.content.entities.geburah.chain_trap.ChainTrapSummonProjectile;
import com.finderfeed.fdbosses.content.entities.geburah.chain_trap.GeburahChainTrapEntity;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_bell.GeburahBell;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_earthquake.GeburahEarthquake;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_explosive_crystal.GeburahSinCrystal;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_opening_floor.GeburahOpeningFloor;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile.JudgementBallProjectile;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_bird.JudgementBirdEntity;
import com.finderfeed.fdbosses.content.entities.geburah.justice_hammer.JusticeHammerAttack;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
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
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_warrior.MalkuthWarriorEntity;
import com.finderfeed.fdbosses.content.items.geburah.DivineGear;
import com.finderfeed.fdbosses.content.items.malkuth.MalkuthFistChain;
import com.finderfeed.fdbosses.content.projectiles.ChesedBlockProjectile;
import com.finderfeed.fdbosses.content.projectiles.MalkuthPlayerFireIceBall;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.MOD)
public class BossEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create((IForgeRegistry)ForgeRegistries.ENTITY_TYPES, (String)"fdbosses");
    public static final Supplier<EntityType<ChesedEntity>> CHESED = ENTITIES.register("chesed", () -> EntityType.Builder.m_20704_(ChesedEntity::new, (MobCategory)MobCategory.CREATURE).m_20699_(5.0f, 3.0f).m_20712_("chesed"));
    public static final Supplier<EntityType<ChesedMonolith>> CHESED_MONOLITH = ENTITIES.register("chesed_monolith", () -> EntityType.Builder.m_20704_(ChesedMonolith::new, (MobCategory)MobCategory.CREATURE).m_20699_(1.0f, 3.0f).m_20712_("chesed_monolith"));
    public static final Supplier<EntityType<EarthShatterEntity>> EARTH_SHATTER = ENTITIES.register("earth_shatter", () -> EntityType.Builder.m_20704_(EarthShatterEntity::new, (MobCategory)MobCategory.MISC).m_20699_(1.0f, 1.0f).m_20712_("earth_shatter"));
    public static final Supplier<EntityType<ChesedBlockProjectile>> BLOCK_PROJECTILE = ENTITIES.register("block_projectile", () -> EntityType.Builder.m_20704_(ChesedBlockProjectile::new, (MobCategory)MobCategory.MISC).m_20699_(1.0f, 1.0f).m_20717_(1).m_20712_("block_projectile"));
    public static final Supplier<EntityType<ChesedFallingBlock>> CHESED_FALLING_BLOCK = ENTITIES.register("chesed_falling_block", () -> EntityType.Builder.m_20704_(ChesedFallingBlock::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("chesed_falling_block"));
    public static final Supplier<EntityType<FlyingBlockEntity>> FLYING_BLOCK = ENTITIES.register("flying_block", () -> EntityType.Builder.m_20704_(FlyingBlockEntity::new, (MobCategory)MobCategory.MISC).m_20699_(1.0f, 1.0f).m_20712_("flying_block"));
    public static final Supplier<EntityType<ChesedElectricSphereEntity>> CHESED_ELECTRIC_SPHERE = ENTITIES.register("chesed_electric_sphere", () -> EntityType.Builder.m_20704_(ChesedElectricSphereEntity::new, (MobCategory)MobCategory.MISC).m_20699_(1.0f, 1.0f).m_20712_("electric_sphere"));
    public static final Supplier<EntityType<RadialEarthquakeEntity>> RADIAL_EARTHQUAKE = ENTITIES.register("radial_earthquake", () -> EntityType.Builder.m_20704_(RadialEarthquakeEntity::new, (MobCategory)MobCategory.MISC).m_20702_(60).m_20699_(0.2f, 0.2f).m_20712_("radial_earthquake"));
    public static final Supplier<EntityType<EyeOfChesedEntity>> EYE_OF_CHESED = ENTITIES.register("eye_of_chesed", () -> EntityType.Builder.m_20704_(EyeOfChesedEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20702_(4).m_20717_(4).m_20712_("eye_of_chesed"));
    public static final Supplier<EntityType<EyeOfMalkuthEntity>> EYE_OF_MALKUTH = ENTITIES.register("eye_of_malkuth", () -> EntityType.Builder.m_20704_(EyeOfMalkuthEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20702_(4).m_20717_(4).m_20712_("eye_of_chesed"));
    public static final Supplier<EntityType<EyeOfGeburah>> EYE_OF_GEBURAH = ENTITIES.register("eye_of_geburah", () -> EntityType.Builder.m_20704_(EyeOfGeburah::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20702_(4).m_20717_(4).m_20712_("eye_of_geburah"));
    public static final Supplier<EntityType<ChesedCrystalEntity>> CHESED_CRYSTAL = ENTITIES.register("chesed_crystal", () -> EntityType.Builder.m_20704_(ChesedCrystalEntity::new, (MobCategory)MobCategory.MISC).m_20699_(3.5f, 4.0f).m_20712_("chesed_crystal"));
    public static final Supplier<EntityType<ChesedMovingVerticalRay>> CHESED_VERTICAL_RAY_ATTACK = ENTITIES.register("chesed_vertical_ray_attack", () -> EntityType.Builder.m_20704_(ChesedMovingVerticalRay::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20717_(1).m_20712_("chesed_vertical_ray_attack"));
    public static final Supplier<EntityType<ChesedFireTrailEntity>> CHESED_FIRE_TRAIL = ENTITIES.register("chesed_fire_trail", () -> EntityType.Builder.m_20704_(ChesedFireTrailEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20712_("chesed_fire_trail"));
    public static final Supplier<EntityType<ChesedKineticFieldEntity>> CHESED_KINETIC_FIELD = ENTITIES.register("chesed_kinetic_field", () -> EntityType.Builder.m_20704_(ChesedKineticFieldEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20712_("chesed_kinetic_field"));
    public static final Supplier<EntityType<ChesedOneShotVerticalRayEntity>> CHESED_ONE_SHOT_VERTICAL_RAY_ATTACK = ENTITIES.register("chesed_one_shot_vertical_ray_attack", () -> EntityType.Builder.m_20704_(ChesedOneShotVerticalRayEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20712_("chesed_one_shot_vertical_ray_attack"));
    public static final Supplier<EntityType<ChesedBossSpawner>> CHESED_BOSS_SPAWNER = ENTITIES.register("chesed_boss_spawner", () -> EntityType.Builder.m_20704_(ChesedBossSpawner::new, (MobCategory)MobCategory.MISC).m_20699_(2.0f, 2.0f).m_20712_("chesed_boss_spawner"));
    public static final Supplier<EntityType<ChesedRayReflector>> CHESED_RAY_REFLECTOR = ENTITIES.register("chesed_ray_reflector", () -> EntityType.Builder.m_20704_(ChesedRayReflector::new, (MobCategory)MobCategory.MISC).m_20699_(1.0f, 2.0f).m_20712_("chesed_ray_reflector"));
    public static final Supplier<EntityType<FlyingSwordEntity>> FLYING_SWORD = ENTITIES.register("flying_sword", () -> EntityType.Builder.m_20704_(FlyingSwordEntity::new, (MobCategory)MobCategory.MISC).m_20717_(1).m_20699_(0.2f, 0.2f).m_20712_("flying_sword"));
    public static final Supplier<EntityType<ChesedMiniRay>> CHESED_MINI_RAY = ENTITIES.register("chesed_mini_ray", () -> EntityType.Builder.m_20704_(ChesedMiniRay::new, (MobCategory)MobCategory.MISC).m_20717_(1).m_20699_(0.2f, 0.2f).m_20712_("chesed_mini_ray"));
    public static final Supplier<EntityType<MalkuthEntity>> MALKUTH = ENTITIES.register("malkuth", () -> EntityType.Builder.m_20704_(MalkuthEntity::new, (MobCategory)MobCategory.CREATURE).m_20699_(1.5f, 3.0f).m_20712_("malkuth"));
    public static final Supplier<EntityType<MalkuthSlashProjectile>> MALKUTH_SLASH = ENTITIES.register("malkuth_slash", () -> EntityType.Builder.m_20704_(MalkuthSlashProjectile::new, (MobCategory)MobCategory.MISC).m_20717_(1).m_20699_(0.2f, 0.2f).m_20712_("malkuth_slash"));
    public static final Supplier<EntityType<MalkuthCrushAttack>> MALKUTH_CRUSH = ENTITIES.register("malkuth_crush", () -> EntityType.Builder.m_20704_(MalkuthCrushAttack::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("malkuth_crush"));
    public static final Supplier<EntityType<MalkuthChainEntity>> MALKUTH_CHAIN = ENTITIES.register("malkuth_chain", () -> EntityType.Builder.m_20704_(MalkuthChainEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("malkuth_chain"));
    public static final Supplier<EntityType<MalkuthEarthquake>> MALKUTH_EARTHQUAKE = ENTITIES.register("malkuth_earthquake", () -> EntityType.Builder.m_20704_(MalkuthEarthquake::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("malkuth_earthquake"));
    public static final Supplier<EntityType<MalkuthRepairEntity>> MALKUTH_REPAIR_ENTITY = ENTITIES.register("malkuth_repair_entity", () -> EntityType.Builder.m_20704_(MalkuthRepairEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20702_(40).setShouldReceiveVelocityUpdates(true).m_20717_(1).m_20712_("malkuth_repair_entity"));
    public static final Supplier<EntityType<MalkuthCannonProjectile>> MALKUTH_CANNON_PROJECTILE = ENTITIES.register("malkuth_cannon_projectile", () -> EntityType.Builder.m_20704_(MalkuthCannonProjectile::new, (MobCategory)MobCategory.MISC).m_20717_(1).m_20699_(0.5f, 0.5f).m_20712_("malkuth_cannon_projectile"));
    public static final Supplier<EntityType<MalkuthCannonEntity>> MALKUTH_CANNON = ENTITIES.register("malkuth_cannon", () -> EntityType.Builder.m_20704_(MalkuthCannonEntity::new, (MobCategory)MobCategory.MISC).m_20699_(2.0f, 2.0f).m_20712_("malkuth_cannon"));
    public static final Supplier<EntityType<MalkuthBossSpawner>> MALKUTH_BOSS_SPAWNER = ENTITIES.register("malkuth_boss_spawner", () -> EntityType.Builder.m_20704_(MalkuthBossSpawner::new, (MobCategory)MobCategory.MISC).m_20699_(1.0f, 2.0f).m_20712_("malkuth_boss_spawner"));
    public static final Supplier<EntityType<MalkuthGiantSwordSlash>> MALKUTH_GIANT_SWORD = ENTITIES.register("malkuth_giant_sword_slash", () -> EntityType.Builder.m_20704_(MalkuthGiantSwordSlash::new, (MobCategory)MobCategory.MISC).m_20699_(2.0f, 2.0f).m_20712_("malkuth_giant_sword_slash"));
    public static final Supplier<EntityType<MalkuthRepairCrystal>> MALKUTH_REPAIR_CRYSTAL = ENTITIES.register("malkuth_repair_crystal", () -> EntityType.Builder.m_20704_(MalkuthRepairCrystal::new, (MobCategory)MobCategory.MISC).m_20699_(2.0f, 2.0f).m_20712_("malkuth_repair_crystal"));
    public static final Supplier<EntityType<MalkuthBoulderEntity>> MALKUTH_BOULDER = ENTITIES.register("malkuth_boulder", () -> EntityType.Builder.m_20704_(MalkuthBoulderEntity::new, (MobCategory)MobCategory.MISC).m_20702_(30).m_20699_(3.0f, 3.0f).m_20712_("malkuth_boulder"));
    public static final Supplier<EntityType<MalkuthPlatform>> MALKUTH_PLATFORM = ENTITIES.register("malkuth_platform", () -> EntityType.Builder.m_20704_(MalkuthPlatform::new, (MobCategory)MobCategory.MISC).m_20702_(30).m_20699_(5.0f, 1.0f).m_20712_("malkuth_platform"));
    public static final Supplier<EntityType<MalkuthFloorEntity>> MALKUTH_FLOOR = ENTITIES.register("malkuth_floor", () -> EntityType.Builder.m_20704_(MalkuthFloorEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.3f, 0.3f).m_20712_("malkuth_floor"));
    public static final Supplier<EntityType<MalkuthFireball>> MALKUTH_FIREBALL = ENTITIES.register("malkuth_fireball", () -> EntityType.Builder.m_20704_(MalkuthFireball::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20712_("malkuth_fireball"));
    public static final Supplier<EntityType<MalkuthPlayerFireIceBall>> MALKUTH_PLAYER_FIREBALL = ENTITIES.register("malkuth_player_fireball", () -> EntityType.Builder.m_20704_(MalkuthPlayerFireIceBall::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20712_("malkuth_player_fireball"));
    public static final Supplier<EntityType<MalkuthWarriorEntity>> FIRE_MALKUTH_WARRIOR = ENTITIES.register("fire_malkuth_warrior", () -> EntityType.Builder.m_20704_((type, lvl) -> new MalkuthWarriorEntity((EntityType<? extends BossMonsterMob>)type, lvl, MalkuthAttackType.FIRE), (MobCategory)MobCategory.MONSTER).m_20699_(0.6f, 1.8f).m_20712_("fire_malkuth_warrior"));
    public static final Supplier<EntityType<MalkuthWarriorEntity>> ICE_MALKUTH_WARRIOR = ENTITIES.register("ice_malkuth_warrior", () -> EntityType.Builder.m_20704_((type, lvl) -> new MalkuthWarriorEntity((EntityType<? extends BossMonsterMob>)type, lvl, MalkuthAttackType.ICE), (MobCategory)MobCategory.MONSTER).m_20699_(0.6f, 1.8f).m_20712_("ice_malkuth_warrior"));
    public static final Supplier<EntityType<GeburahEntity>> GEBURAH = ENTITIES.register("geburah", () -> EntityType.Builder.m_20704_(GeburahEntity::new, (MobCategory)MobCategory.MISC).m_20699_(2.0f, 2.0f).m_20712_("geburah"));
    public static final Supplier<EntityType<GeburahChainTrapEntity>> GEBURAH_CHAIN_TRAP = ENTITIES.register("geburah_chain_trap", () -> EntityType.Builder.m_20704_(GeburahChainTrapEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.5f, 0.5f).m_20712_("geburah_chain_trap"));
    public static final Supplier<EntityType<GeburahEarthquake>> GEBURAH_EARTHQUAKE = ENTITIES.register("geburah_earthquake", () -> EntityType.Builder.m_20704_(GeburahEarthquake::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("geburah_earthquake"));
    public static final Supplier<EntityType<JudgementBallProjectile>> GEBURAH_JUDGEMENT_BALL = ENTITIES.register("geburah_judgement_ball", () -> EntityType.Builder.m_20704_(JudgementBallProjectile::new, (MobCategory)MobCategory.MISC).m_20699_(0.5f, 0.5f).m_20712_("geburah_judgement_ball"));
    public static final Supplier<EntityType<JusticeHammerAttack>> JUSTICE_HAMMER = ENTITIES.register("justice_hammer", () -> EntityType.Builder.m_20704_(JusticeHammerAttack::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("justice_hammer"));
    public static final Supplier<EntityType<GeburahSinCrystal>> GEBURAH_SIN_CRYSTAL = ENTITIES.register("geburah_sin_crystal", () -> EntityType.Builder.m_20704_(GeburahSinCrystal::new, (MobCategory)MobCategory.MISC).m_20717_(1).m_20699_(0.2f, 0.2f).m_20712_("geburah_sin_crystal"));
    public static final Supplier<EntityType<GeburahSinCrystalCastCircle>> GEBURAH_CASTING_CIRCLE_SIN_CRYSTAL = ENTITIES.register("geburah_casting_circle_sin_crystal", () -> EntityType.Builder.m_20704_(GeburahSinCrystalCastCircle::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("geburah_casting_circle_sin_crystal"));
    public static final Supplier<EntityType<GeburahChainTrapCastCircle>> GEBURAH_CASTING_CIRCLE_CHAIN_TRAP = ENTITIES.register("geburah_casting_circle_chain_trap", () -> EntityType.Builder.m_20704_(GeburahChainTrapCastCircle::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("geburah_casting_circle_chain_trap"));
    public static final Supplier<EntityType<GeburahRayCastingCircle>> GEBURAH_CASTING_CIRCLE_RAY = ENTITIES.register("geburah_casting_circle_ray", () -> EntityType.Builder.m_20704_(GeburahRayCastingCircle::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("geburah_casting_circle_ray"));
    public static final Supplier<EntityType<GeburahCastingCircleJudgementBird>> GEBURAH_CASTING_CIRCLE_JUDGEMENT_BIRD = ENTITIES.register("geburah_casting_circle_judgement_bird", () -> EntityType.Builder.m_20704_(GeburahCastingCircleJudgementBird::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("geburah_casting_circle_judgement_bird"));
    public static final Supplier<EntityType<ChainTrapSummonProjectile>> GEBURAH_CHAIN_TRAP_SUMMON_PROJECTILE = ENTITIES.register("geburah_chain_trap_summon_projectile", () -> EntityType.Builder.m_20704_(ChainTrapSummonProjectile::new, (MobCategory)MobCategory.MISC).m_20699_(0.2f, 0.2f).m_20712_("geburah_chain_trap_summon_projectile"));
    public static final Supplier<EntityType<JudgementBirdEntity>> JUDGEMENT_BIRD = ENTITIES.register("judgement_bird", () -> EntityType.Builder.m_20704_(JudgementBirdEntity::new, (MobCategory)MobCategory.CREATURE).m_20699_(0.5f, 1.0f).m_20712_("judgement_bird"));
    public static final Supplier<EntityType<GeburahBell>> GEBURAH_BELL = ENTITIES.register("geburah_bell", () -> EntityType.Builder.m_20704_(GeburahBell::new, (MobCategory)MobCategory.CREATURE).m_20699_(1.0f, 1.0f).m_20712_("geburah_bell"));
    public static final Supplier<EntityType<GeburahOpeningFloor>> GEBURAH_OPENING_FLOOR = ENTITIES.register("geburah_opening_floor", () -> EntityType.Builder.m_20704_(GeburahOpeningFloor::new, (MobCategory)MobCategory.MISC).m_20699_(1.0f, 1.0f).m_20712_("geburah_opening_floor"));
    public static final Supplier<EntityType<GeburahBossSpawner>> GEBURAH_BOSS_SPAWNER = ENTITIES.register("geburah_boss_spawner", () -> EntityType.Builder.m_20704_(GeburahBossSpawner::new, (MobCategory)MobCategory.MISC).m_20699_(1.0f, 2.0f).m_20712_("geburah_boss_spawner"));
    public static final Supplier<EntityType<MalkuthFistChain>> MALKUTH_FIST_CHAIN = ENTITIES.register("malkuth_fist_chain", () -> EntityType.Builder.m_20704_(MalkuthFistChain::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20712_("malkuth_fist_chain"));
    public static final Supplier<EntityType<DivineGear>> DIVINE_GEAR = ENTITIES.register("divine_gear", () -> EntityType.Builder.m_20704_(DivineGear::new, (MobCategory)MobCategory.MISC).m_20699_(2.0f, 3.0f).m_20712_("divine_gear"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GEBURAH_BELL.get(), LivingEntity.m_21183_().m_22268_(Attributes.f_22276_, 2.0).m_22265_());
        event.put(CHESED.get(), Mob.m_21552_().m_22268_(Attributes.f_22276_, 10.0).m_22265_());
        event.put(CHESED_ELECTRIC_SPHERE.get(), LivingEntity.m_21183_().m_22265_());
        event.put(CHESED_VERTICAL_RAY_ATTACK.get(), LivingEntity.m_21183_().m_22265_());
        event.put(CHESED_MONOLITH.get(), LivingEntity.m_21183_().m_22268_(Attributes.f_22276_, 50.0).m_22265_());
        event.put(CHESED_CRYSTAL.get(), LivingEntity.m_21183_().m_22268_(Attributes.f_22276_, 20.0).m_22265_());
        event.put(MALKUTH_CHAIN.get(), LivingEntity.m_21183_().m_22268_(Attributes.f_22276_, 20.0).m_22265_());
        event.put(MALKUTH.get(), Mob.m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22265_());
        event.put(MALKUTH_CANNON.get(), LivingEntity.m_21183_().m_22268_(Attributes.f_22276_, 20.0).m_22265_());
        event.put(GEBURAH.get(), LivingEntity.m_21183_().m_22268_(Attributes.f_22276_, 20.0).m_22265_());
        event.put(JUDGEMENT_BIRD.get(), Mob.m_21552_().m_22268_(Attributes.f_22277_, 32.0).m_22268_(Attributes.f_22279_, (double)0.35f).m_22265_());
        event.put(FIRE_MALKUTH_WARRIOR.get(), Mob.m_21552_().m_22268_(Attributes.f_22276_, 60.0).m_22268_(Attributes.f_22279_, (double)0.23f).m_22268_(Attributes.f_22278_, 1.0).m_22265_());
        event.put(ICE_MALKUTH_WARRIOR.get(), Mob.m_21552_().m_22268_(Attributes.f_22276_, 60.0).m_22268_(Attributes.f_22279_, (double)0.23f).m_22268_(Attributes.f_22278_, 1.0).m_22265_());
    }
}

