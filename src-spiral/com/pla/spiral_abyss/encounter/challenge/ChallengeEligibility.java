/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 */
package com.pla.spiral_abyss.encounter.challenge;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class ChallengeEligibility
implements Predicate<Challenge> {
    private final ServerLevel level;
    private final List<List<FloorDefinition.Enemy>> waves;
    private final Map<ResourceLocation, Optional<LivingEntity>> probes = new HashMap<ResourceLocation, Optional<LivingEntity>>();

    public ChallengeEligibility(ServerLevel level, List<List<FloorDefinition.Enemy>> waves) {
        this.level = level;
        this.waves = waves.stream().map(List::copyOf).toList();
    }

    @Override
    public boolean test(Challenge challenge) {
        if (challenge.type() != Challenge.Type.MOB_BURNING && challenge.type() != Challenge.Type.MOB_EFFECT) {
            return true;
        }
        for (List<FloorDefinition.Enemy> wave : this.waves) {
            int total = wave.stream().mapToInt(FloorDefinition.Enemy::count).sum();
            int eligible = wave.stream().filter(enemy -> this.susceptible(enemy.id(), challenge)).mapToInt(FloorDefinition.Enemy::count).sum();
            if (eligible >= challenge.requiredCount(total)) continue;
            return false;
        }
        return true;
    }

    private boolean susceptible(ResourceLocation id, Challenge challenge) {
        EntityType<?> type = AbyssRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) {
            return false;
        }
        if (challenge.type() == Challenge.Type.MOB_BURNING) {
            return !type.m_20672_();
        }
        Optional probe = this.probes.computeIfAbsent(id, ignored -> {
            try {
                Optional<Object> optional;
                Entity entity = type.m_20615_((Level)this.level);
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity)entity;
                    optional = Optional.of(living);
                } else {
                    optional = Optional.empty();
                }
                return optional;
            }
            catch (RuntimeException exception) {
                LogUtils.getLogger().warn("Cannot check Abyss effect susceptibility for {}", (Object)id, (Object)exception);
                return Optional.empty();
            }
        });
        if (probe.isEmpty()) {
            return false;
        }
        try {
            return challenge.ids().stream().allMatch(effectId -> {
                MobEffect effect = AbyssRegistries.MOB_EFFECTS.getValue((ResourceLocation)effectId);
                return effect != null && ((LivingEntity)probe.get()).m_7301_(new MobEffectInstance(effect, 200));
            });
        }
        catch (RuntimeException exception) {
            return false;
        }
    }
}

