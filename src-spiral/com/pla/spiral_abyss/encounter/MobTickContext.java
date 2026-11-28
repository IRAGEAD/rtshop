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
 *  net.minecraft.world.entity.projectile.Projectile
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public final class MobTickContext {
    private static final TagKey<EntityType<?>> SCRIPTED_LOOT = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "suppress_tick_loot"));
    private static final TagKey<EntityType<?>> PROTECT_INVENTORY = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "protect_player_inventory"));
    private static final TagKey<EntityType<?>> NO_BLOCK_CHANGES = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "no_arena_block_changes"));
    private static final TagKey<EntityType<?>> SUPPRESS_DEATH_EFFECTS = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "suppress_death_effects"));
    private static final TagKey<EntityType<?>> REMOVAL_LOOT = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "suppress_removal_loot"));
    private static final ThreadLocal<LivingEntity> CURRENT = new ThreadLocal();
    private static final ThreadLocal<LivingEntity> BLOCK_SOURCE = new ThreadLocal();

    private MobTickContext() {
    }

    public static void tick(Entity entity, Runnable action) {
        QliphothEncounters.tick(entity, () -> MobTickContext.guardedTick(entity, action));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void guardedTick(Entity entity, Runnable action) {
        boolean changeBlockSource;
        LivingEntity current = null;
        LivingEntity blockSource = QliphothEncounters.blockSource();
        if (entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            LivingEntity mob;
            Entity owner;
            if (entity instanceof Projectile) {
                Projectile projectile = (Projectile)entity;
                v0 = projectile.m_19749_();
            } else {
                v0 = owner = entity;
            }
            if (owner instanceof LivingEntity) {
                mob = (LivingEntity)owner;
                if (owner.m_9236_() == entity.m_9236_() && owner.m_6095_().m_204039_(NO_BLOCK_CHANGES) && AbyssPlatform.data(owner).m_128403_("spiral_abyss_run")) {
                    blockSource = mob;
                }
            }
            if (entity instanceof LivingEntity) {
                mob = (LivingEntity)entity;
                if (entity.m_6095_().m_204039_(REMOVAL_LOOT) || (entity.m_6095_().m_204039_(SCRIPTED_LOOT) || entity.m_6095_().m_204039_(PROTECT_INVENTORY) || entity.m_6095_().m_204039_(SUPPRESS_DEATH_EFFECTS)) && AbyssPlatform.data(entity).m_128403_("spiral_abyss_run")) {
                    current = mob;
                }
            }
        }
        LivingEntity previous = CURRENT.get();
        LivingEntity previousBlockSource = BLOCK_SOURCE.get();
        boolean changeCurrent = current != previous;
        boolean bl = changeBlockSource = blockSource != previousBlockSource;
        if (changeCurrent) {
            MobTickContext.set(CURRENT, current);
        }
        if (changeBlockSource) {
            MobTickContext.set(BLOCK_SOURCE, blockSource);
        }
        try {
            action.run();
        }
        finally {
            if (changeCurrent) {
                MobTickContext.set(CURRENT, previous);
            }
            if (changeBlockSource) {
                MobTickContext.set(BLOCK_SOURCE, previousBlockSource);
            }
        }
    }

    private static void set(ThreadLocal<LivingEntity> context, LivingEntity source) {
        if (source == null) {
            context.remove();
        } else {
            context.set(source);
        }
    }

    public static LivingEntity scriptedRewardSource() {
        LivingEntity source = CURRENT.get();
        return source != null && (source.m_6095_().m_204039_(SCRIPTED_LOOT) || source.m_6095_().m_204039_(REMOVAL_LOOT)) ? source : null;
    }

    public static boolean protectsInventory() {
        LivingEntity source = CURRENT.get();
        return source != null && source.m_6095_().m_204039_(PROTECT_INVENTORY);
    }

    public static LivingEntity suppressedDeathSource() {
        LivingEntity source = CURRENT.get();
        return source != null && source.m_21223_() <= 0.0f && source.m_6095_().m_204039_(SUPPRESS_DEATH_EFFECTS) ? source : null;
    }

    public static LivingEntity blockChangeSource() {
        LivingEntity source = BLOCK_SOURCE.get();
        return source == null ? MobTickContext.suppressedDeathSource() : source;
    }
}

