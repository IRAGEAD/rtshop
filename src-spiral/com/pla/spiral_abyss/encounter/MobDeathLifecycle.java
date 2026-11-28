/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public final class MobDeathLifecycle {
    private static final TagKey<EntityType<?>> WAIT_FOR_REMOVAL = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "waits_for_removal"));
    private static final TagKey<EntityType<?>> DELAYS_DEATH_EVENT = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "delays_death_event"));
    private static final Set<ResourceLocation> DELAYED_DEATH_EVENTS = Set.of(new ResourceLocation("mutantmonsters", "mutant_zombie"), new ResourceLocation("mutantmonsters", "mutant_creeper"), new ResourceLocation("mutantmonsters", "mutant_enderman"), new ResourceLocation("goety", "apostle"));
    private static final Set<ResourceLocation> DELAYED_GOETY = Set.of(new ResourceLocation("goety", "vizier"), new ResourceLocation("goety", "ender_keeper"), new ResourceLocation("goety", "hostile_redstone_monstrosity"));

    private MobDeathLifecycle() {
    }

    public static boolean delaysDeathEvent(LivingEntity mob) {
        return mob.m_6095_().m_204039_(DELAYS_DEATH_EVENT) || DELAYED_DEATH_EVENTS.contains(AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_()));
    }

    public static boolean waitsForRemoval(LivingEntity mob) {
        return mob instanceof EnderDragon || MobDeathLifecycle.delaysDeathEvent(mob) || mob.m_6095_().m_204039_(WAIT_FOR_REMOVAL) || DELAYED_GOETY.contains(AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_()));
    }
}

