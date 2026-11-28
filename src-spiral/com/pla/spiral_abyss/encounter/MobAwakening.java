/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.Mob
 */
package com.pla.spiral_abyss.encounter;

import java.lang.reflect.Method;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class MobAwakening {
    private static final TagKey<EntityType<?>> SPAWN_AWAKENED = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "spawn_awakened"));

    private MobAwakening() {
    }

    public static void apply(Mob mob) {
        if (!mob.m_6095_().m_204039_(SPAWN_AWAKENED)) {
            return;
        }
        try {
            Method awaken = mob.getClass().getMethod("setAwaken", Boolean.TYPE);
            Method sleep = mob.getClass().getMethod("setSleep", Boolean.TYPE);
            awaken.invoke((Object)mob, true);
            sleep.invoke((Object)mob, false);
        }
        catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Cannot awaken tagged arena mob " + String.valueOf(mob.m_6095_()), ex);
        }
    }
}

