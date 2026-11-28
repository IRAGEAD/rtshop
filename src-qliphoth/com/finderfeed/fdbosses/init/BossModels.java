/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.FDRegistries
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModelInfo
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdlib.systems.FDRegistries;
import com.finderfeed.fdlib.systems.bedrock.models.FDModelInfo;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;

public class BossModels {
    public static final DeferredRegister<FDModelInfo> INFOS = DeferredRegister.create((ResourceKey)FDRegistries.BEDROCK_MODEL_INFOS_KEY, (String)"fdbosses");
    public static final Supplier<FDModelInfo> CHESED = INFOS.register("chesed", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed"), 1.0f));
    public static final Supplier<FDModelInfo> CHESED_INFLATED = INFOS.register("chesed_inflated", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed"), 1.2f));
    public static final Supplier<FDModelInfo> CHESED_CRYSTAL_LAYER = INFOS.register("chesed_crystal_layer", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed"), 1.01f));
    public static final Supplier<FDModelInfo> CHESED_ELECTRIC_SPHERE = INFOS.register("chesed_electric_sphere", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"electric_orb"), 1.0f));
    public static final Supplier<FDModelInfo> CHESED_CRYSTAL = INFOS.register("chesed_crystal", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_crystal"), 1.0f));
    public static final Supplier<FDModelInfo> CHESED_MONOLITH = INFOS.register("chesed_monolith", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_monolith"), 1.0f));
    public static final Supplier<FDModelInfo> CHESED_KINETIC_FIELD = INFOS.register("chesed_kinetic_field", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_kinetic_field"), 1.0f));
    public static final Supplier<FDModelInfo> BOSS_SPAWNER = INFOS.register("boss_spawner", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"boss_spawner"), 1.0f));
    public static final Supplier<FDModelInfo> BOSS_SPAWNER_CRYSTAL_LAYER = INFOS.register("boss_spawner_crystal_layer", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"boss_spawner"), 1.01f));
    public static final Supplier<FDModelInfo> CHESED_RAY_REFLECTOR = INFOS.register("chesed_ray_reflector", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_ray_reflector"), 1.0f));
    public static final Supplier<FDModelInfo> WIP_MODEL = INFOS.register("wip", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"wip"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH = INFOS.register("malkuth", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_SCREEN = INFOS.register("malkuth_screen", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_screen"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_SWORD = INFOS.register("malkuth_sword", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_sword"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_CRUSH_ATTACK = INFOS.register("malkuth_crush_attack", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_crush_attack"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_CANNON = INFOS.register("malkuth_cannon", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_cannon"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_CANNON_PROJECTILE = INFOS.register("malkuth_cannon_projectile", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_cannon_projectile"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_EARTHQUAKE_PART = INFOS.register("malkuth_earthquake_part", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_earthquake_part"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_BOULDER_ICE = INFOS.register("malkuth_boulder_ice", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_boulder_ice"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_BOULDER_FIRE = INFOS.register("malkuth_boulder_fire", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_boulder_fire"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_PLATFORM = INFOS.register("malkuth_platform", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_platform"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_REPAIR_CRYSTAL = INFOS.register("malkuth_repair_crystal", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_repair_crystal"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_SPAWNER = INFOS.register("malkuth_boss_spawner", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_boss_spawner"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_WARRIOR = INFOS.register("malkuth_warrior", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_warrior"), 1.0f));
    public static final Supplier<FDModelInfo> GEBURAH = INFOS.register("geburah", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah"), 1.0f));
    public static final Supplier<FDModelInfo> GEBURAH_LAYER = INFOS.register("geburah_layer", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah_no_core"), 1.01f));
    public static final Supplier<FDModelInfo> JUDGEMENT_BALL = INFOS.register("judgement_ball", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"judgement_ball"), 1.0f));
    public static final Supplier<FDModelInfo> JUDGEMENT_BALL_LAYER = INFOS.register("judgement_ball_layer", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"judgement_ball"), 1.05f));
    public static final Supplier<FDModelInfo> JUSTICE_HAMMER = INFOS.register("justice_hammer", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"justice_hammer"), 1.0f));
    public static final Supplier<FDModelInfo> JUDGEMENT_BIRD = INFOS.register("judgement_bird", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"judgement_bird"), 1.0f));
    public static final Supplier<FDModelInfo> JUDGEMENT_BIRD_LAYER = INFOS.register("judgement_bird_layer", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"judgement_bird"), 1.05f));
    public static final Supplier<FDModelInfo> GEBURAH_BELL = INFOS.register("geburah_bell", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"geburah_bell"), 1.0f));
    public static final Supplier<FDModelInfo> JUSTITIA = INFOS.register("justitia", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"justitia"), 1.0f));
    public static final Supplier<FDModelInfo> CHESED_ITEM = INFOS.register("chesed_item", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"chesed_item"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_FIST = INFOS.register("malkuth_fist", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_fist"), 1.0f));
    public static final Supplier<FDModelInfo> MALKUTH_FIST_LAYER = INFOS.register("malkuth_fist_layer", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_fist"), 1.1f));
    public static final Supplier<FDModelInfo> MALKUTH_FIST_FLYING = INFOS.register("malkuth_fist_flying", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"malkuth_fist_flying"), 1.0f));
    public static final Supplier<FDModelInfo> DIVINE_GEAR = INFOS.register("divine_gear", () -> new FDModelInfo(ResourceLocation.m_214293_((String)"fdbosses", (String)"divine_gear"), 1.0f));
}

