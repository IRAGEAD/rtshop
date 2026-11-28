/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class AbyssRewardCarriers {
    private static final TagKey<EntityType<?>> CARRIERS = AbyssRewardCarriers.tag("suppressed_reward_carriers");
    private static final TagKey<EntityType<?>> CORPSES = AbyssRewardCarriers.tag("remove_corpse_on_death");
    private static final TagKey<EntityType<?>> DEATH_SPAWNS = AbyssRewardCarriers.tag("suppressed_death_spawns");

    private AbyssRewardCarriers() {
    }

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", name));
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static boolean suppress(Entity entity) {
        if (!(entity instanceof Mob)) return false;
        Mob mob = (Mob)entity;
        if (entity.m_9236_().f_46443_) return false;
        if (!entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) return false;
        if (entity.m_6095_().m_204039_(CARRIERS)) return true;
        if (!entity.m_6095_().m_204039_(CORPSES)) return false;
        if (!AbyssPlatform.data(entity).m_128403_("spiral_abyss_run")) return false;
        if (!(mob.m_21223_() <= 0.0f)) return false;
        return true;
    }

    public static void join(AbyssEvents.EntityJoinLevelEvent event) {
        LivingEntity source = MobTickContext.suppressedDeathSource();
        if (AbyssRewardCarriers.suppress(event.getEntity()) || source != null && event.getLevel() == source.m_9236_() && event.getEntity().m_6095_().m_204039_(DEATH_SPAWNS)) {
            event.setCanceled(true);
        }
    }

    public static void deathExplosion(AbyssEvents.ExplosionEvent.Start event) {
        LivingEntity source = MobTickContext.suppressedDeathSource();
        if (source != null && event.getLevel() == source.m_9236_()) {
            event.setCanceled(true);
        }
    }

    public static void tick(AbyssEvents.LivingEvent.LivingTickEvent event) {
        if (AbyssRewardCarriers.suppress((Entity)event.getEntity())) {
            event.getEntity().m_146870_();
            event.setCanceled(true);
        }
    }
}

