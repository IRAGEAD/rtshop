/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EntityType$Builder
 *  net.minecraft.world.entity.MobCategory
 */
package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.entity.ChestLootEntity;
import com.pla.spiral_abyss.entity.CopperChestEntity;
import com.pla.spiral_abyss.entity.ExperienceChestEntity;
import com.pla.spiral_abyss.entity.GoldenChestEntity;
import com.pla.spiral_abyss.entity.IronChestEntity;
import com.pla.spiral_abyss.entity.KeyEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create((String)"spiral_abyss", (ResourceKey)Registries.f_256939_);
    public static final RegistrySupplier<EntityType<KeyEntity>> KEY_ENTITY = ENTITY_TYPES.register("key_entity", () -> EntityType.Builder.m_20704_(KeyEntity::new, (MobCategory)MobCategory.CREATURE).m_20699_(2.5f, 4.0f).m_20712_("key_entity"));
    public static final RegistrySupplier<EntityType<CopperChestEntity>> COPPER_CHEST = ENTITY_TYPES.register("copper_chest", () -> EntityType.Builder.m_20704_(CopperChestEntity::new, (MobCategory)MobCategory.MISC).m_20699_(1.3f, 1.0f).m_20712_("copper_chest"));
    public static final RegistrySupplier<EntityType<IronChestEntity>> IRON_CHEST = ENTITY_TYPES.register("iron_chest", () -> EntityType.Builder.m_20704_(IronChestEntity::new, (MobCategory)MobCategory.MISC).m_20699_(1.3f, 1.0f).m_20712_("iron_chest"));
    public static final RegistrySupplier<EntityType<GoldenChestEntity>> GOLDEN_CHEST = ENTITY_TYPES.register("golden_chest", () -> EntityType.Builder.m_20704_(GoldenChestEntity::new, (MobCategory)MobCategory.MISC).m_20699_(1.3f, 1.0f).m_20712_("golden_chest"));
    public static final RegistrySupplier<EntityType<ExperienceChestEntity>> EXPERIENCE_CHEST = ENTITY_TYPES.register("experience_chest", () -> EntityType.Builder.m_20704_(ExperienceChestEntity::new, (MobCategory)MobCategory.MISC).m_20699_(1.3f, 1.0f).m_20712_("experience_chest"));
    public static final RegistrySupplier<EntityType<ChestLootEntity>> CHEST_LOOT = ENTITY_TYPES.register("chest_loot", () -> EntityType.Builder.m_20704_(ChestLootEntity::new, (MobCategory)MobCategory.MISC).m_20699_(0.25f, 0.25f).m_20702_(8).m_20717_(1).m_20712_("chest_loot"));
}

