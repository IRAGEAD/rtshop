/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.FDRegistries
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.RegistryObject
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdlib.systems.FDRegistries;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class BossAnims {
    public static DeferredRegister<Animation> ANIMATIONS = DeferredRegister.create((ResourceKey)FDRegistries.ANIMATIONS_KEY, (String)"fdbosses");
    public static RegistryObject<Animation> CHESED_IDLE = ANIMATIONS.register("chesed_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_BOOM_ATTACK = ANIMATIONS.register("chesed_boom_attack", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_ATTACK = ANIMATIONS.register("chesed_attack", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_ROLL_UP = ANIMATIONS.register("chesed_roll_up", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_ROLL_UP_END = ANIMATIONS.register("chesed_roll_up_end", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_SECOND_PHASE_PREPARE = ANIMATIONS.register("chesed_second_phase_prepare", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_SECOND_PHASE_STRIKE_WALLS = ANIMATIONS.register("chesed_second_phase_strike_walls", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_ROLL_UP_JUST_END = ANIMATIONS.register("chesed_roll_up_just_end", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_ROLL = ANIMATIONS.register("chesed_roll", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_ROLL_ROLL = ANIMATIONS.register("chesed_roll_roll", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_CAST = ANIMATIONS.register("chesed_cast", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_EARTHQUAKE_CAST = ANIMATIONS.register("chesed_earthquake_cast", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_ROCKFALL_CAST = ANIMATIONS.register("chesed_rockfall_cast", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> ELECTRIC_ORB_IDLE = ANIMATIONS.register("electric_orb_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"electric_orb")));
    public static RegistryObject<Animation> CHESED_CRYSTAL_SPAWN = ANIMATIONS.register("chesed_crystal_spawn", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_crystal_animations")));
    public static RegistryObject<Animation> CHESED_MONOLITH_IDLE = ANIMATIONS.register("chesed_monolith_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_monolith")));
    public static RegistryObject<Animation> CHESED_MONOLITH_TURN_OFF = ANIMATIONS.register("chesed_monolith_turn_off", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_monolith")));
    public static RegistryObject<Animation> CHESED_KINETIC_FIELD_SPAWN = ANIMATIONS.register("kinetic_field_spawn", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"kinetic_field")));
    public static RegistryObject<Animation> CHESED_APPEAR = ANIMATIONS.register("chesed_appear", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_SCREEN_APPEAR = ANIMATIONS.register("chesed_screen_appear", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> CHESED_DEATH = ANIMATIONS.register("chesed_death", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed")));
    public static RegistryObject<Animation> RAY_REFLECTOR_ACTIVATE = ANIMATIONS.register("ray_reflector_activate", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_ray_reflector_animations")));
    public static RegistryObject<Animation> MALKUTH_TEST = ANIMATIONS.register("test_malkuth", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_IDLE = ANIMATIONS.register("malkuth_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SLASH_FIRE = ANIMATIONS.register("malkuth_slash_fire", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SLASH_ICE = ANIMATIONS.register("malkuth_slash_ice", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_JUMP_FORWARD_PREPARE = ANIMATIONS.register("malkuth_jump_forward_prepare", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_JUMP_CRUSH_ATTACK_START = ANIMATIONS.register("malkuth_jump_crush_attack_start", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_JUMP_CRUSH_ATTACK_MIDAIR = ANIMATIONS.register("malkuth_jump_crush_attack_midair", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_JUMP_CRUSH_ATTACK_END = ANIMATIONS.register("malkuth_jump_crush_attack_end", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_PULL_AND_PUNCH = ANIMATIONS.register("malkuth_pull_and_punch", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SCREEN_APPEAR = ANIMATIONS.register("malkuth_screen_appear", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SCREEN_IDLE = ANIMATIONS.register("malkuth_screen_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_CRUSH_ATTACK_CRUSH = ANIMATIONS.register("malkuth_crush_attack_attack", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_crush_attack")));
    public static RegistryObject<Animation> MALKUTH_CANNON_SHOOT = ANIMATIONS.register("malkuth_cannon_shoot", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_cannon")));
    public static RegistryObject<Animation> MALKUTH_CANNON_BREAK = ANIMATIONS.register("malkuth_cannon_break", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_cannon")));
    public static RegistryObject<Animation> MALKUTH_CANNON_REPAIR = ANIMATIONS.register("malkuth_cannon_repair", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_cannon")));
    public static RegistryObject<Animation> MALKUTH_CANNON_SUMMON = ANIMATIONS.register("malkuth_cannon_summon", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_cannon")));
    public static RegistryObject<Animation> MALKUTH_JUMP_AND_LAND = ANIMATIONS.register("malkuth_jump_and_land", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_JUMP_AND_CRUSH = ANIMATIONS.register("malkuth_jump_with_crush", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_CRUSH_ATTACK_FULL = ANIMATIONS.register("malkuth_jump_crush_attack_full", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SWORD_FORWARD = ANIMATIONS.register("malkuth_sword_forward", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_CAROUSEL_SLASH_1 = ANIMATIONS.register("malkuth_carousel_slash_1", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_GIANT_SWORD_ATTACK = ANIMATIONS.register("malkuth_giant_sword_attack", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SINGLE_EARTHQUAKE_FIRE = ANIMATIONS.register("malkuth_single_earthquake_fire", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SINGLE_EARTHQUAKE_ICE = ANIMATIONS.register("malkuth_single_earthquake_ice", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SUMMON_THROW_SIDE_STONES = ANIMATIONS.register("malkuth_summon_throw_side_stones", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_JUMP_TO_FLOAT = ANIMATIONS.register("malkuth_jump_to_float", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_FLOAT = ANIMATIONS.register("malkuth_float", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_SUMMON_AND_FIRE_FIREBALLS = ANIMATIONS.register("malkuth_summon_fireballs", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_REPAIR_CRYSTAL_SUMMON = ANIMATIONS.register("malkuth_crystal_summon", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_repair_crystal")));
    public static RegistryObject<Animation> MALKUTH_SUMMON_ANIM = ANIMATIONS.register("malkuth_summon_anim", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> MALKUTH_WARRIOR_IDLE = ANIMATIONS.register("malkuth_warrior_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_warrior")));
    public static RegistryObject<Animation> MALKUTH_WARRIOR_ATTACK = ANIMATIONS.register("malkuth_warrior_attack", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_warrior")));
    public static RegistryObject<Animation> MALKUTH_WARRIOR_WALK = ANIMATIONS.register("malkuth_warrior_walk", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_warrior")));
    public static RegistryObject<Animation> MALKUTH_WARRIOR_SLAM_EARTH = ANIMATIONS.register("malkuth_warrior_slam_earth", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_warrior")));
    public static RegistryObject<Animation> MALKUTH_WARRIOR_DIE = ANIMATIONS.register("malkuth_warrior_die", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_warrior")));
    public static RegistryObject<Animation> MALKUTH_DEATH = ANIMATIONS.register("malkuth_death", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth")));
    public static RegistryObject<Animation> GEBURAH_STOMP = ANIMATIONS.register("geburah_stomp", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah")));
    public static RegistryObject<Animation> GEBURAH_FIRE_CANNONS = ANIMATIONS.register("geburah_fire_all_cannons", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah")));
    public static RegistryObject<Animation> JUSTICE_HAMMER_SMACK = ANIMATIONS.register("justice_hammer_smack", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"justice_hammer")));
    public static RegistryObject<Animation> GEBURAH_TILT_LEFT = ANIMATIONS.register("geburah_tilt_left", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah")));
    public static RegistryObject<Animation> GEBURAH_TILT_RIGHT = ANIMATIONS.register("geburah_tilt_right", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah")));
    public static RegistryObject<Animation> JUDGEMENT_BIRD_FLY = ANIMATIONS.register("judgement_bird_fly", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"judgement_bird")));
    public static RegistryObject<Animation> JUDGEMENT_BIRD_CAST = ANIMATIONS.register("judgement_bird_cast", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"judgement_bird")));
    public static RegistryObject<Animation> GEBURAH_BELL_RING = ANIMATIONS.register("geburah_bell_ring", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah_bell")));
    public static RegistryObject<Animation> GEBURAH_APPEAR = ANIMATIONS.register("geburah_appear", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah")));
    public static RegistryObject<Animation> CHESED_ITEM_IDLE = ANIMATIONS.register("chesed_item_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_item")));
    public static RegistryObject<Animation> CHESED_ITEM_USE = ANIMATIONS.register("chesed_item_use", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_item")));
    public static RegistryObject<Animation> MALKUTH_FIST_FLYING = ANIMATIONS.register("malkuth_fist_flying", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_fist")));
    public static RegistryObject<Animation> MALKUTH_FIST_GRAB = ANIMATIONS.register("malkuth_fist_grab", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_fist")));
    public static RegistryObject<Animation> DIVINE_GEAR_IDLE = ANIMATIONS.register("divine_gear_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"divine_gear")));
    public static RegistryObject<Animation> DIVINE_GEAR_SUMMON = ANIMATIONS.register("divine_gear_summon", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"divine_gear")));
    public static RegistryObject<Animation> DIVINE_GEAR_DEATH = ANIMATIONS.register("divine_gear_death", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"divine_gear")));
    public static RegistryObject<Animation> DIVINE_GEAR_ITEM_IDLE = ANIMATIONS.register("divine_gear_item_idle", () -> new Animation(ResourceLocation.m_214293_((String)"fdbosses", (String)"divine_gear")));
}

