/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation
 *  net.minecraft.world.entity.ai.attributes.Attributes
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.EndlessWaves;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.encounter.data.MobScaling;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class WaveScaling {
    public static final String HEALTH_DAMAGE_RATIO_TAG = "spiral_abyss_health_damage_ratio";
    public static final UUID HEALTH_MODIFIER = UUID.fromString("2fce3415-4a70-4976-a728-98234fdac4e3");

    private WaveScaling() {
    }

    public static void applyEndless(LivingEntity mob, int wave, MobCategory category, double partyScale) {
        AttributeInstance health = mob.m_21051_(Attributes.f_22276_);
        if (!WaveScaling.skipRaisingHealth(mob) && health != null) {
            health.m_22120_(HEALTH_MODIFIER);
            health.m_22125_(new AttributeModifier(HEALTH_MODIFIER, "Spiral Abyss health", 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            double initial = health.m_22135_();
            double target = (category == MobCategory.BOSS ? EndlessWaves.bossHealth(wave) : initial * EndlessWaves.mobScale(wave)) * partyScale;
            health.m_22120_(HEALTH_MODIFIER);
            if (initial > 0.0 && Double.isFinite(target)) {
                health.m_22125_(new AttributeModifier(HEALTH_MODIFIER, "Spiral Abyss health", target / initial - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            AbyssPlatform.data((Entity)mob).m_128473_(HEALTH_DAMAGE_RATIO_TAG);
        }
        mob.m_21153_(mob.m_21233_());
        AbyssPlatform.data((Entity)mob).m_128347_("spiral_abyss_damage_scale", EndlessWaves.mobScale(wave) * partyScale);
    }

    public static void apply(LivingEntity mob, int floor, MobCategory category, ResourceLocation mobId, double partyScale, MobCatalog.ScalingSnapshot plans) {
        MobScaling.Floor scaling = plans.at(category, mobId, floor);
        AttributeInstance health = mob.m_21051_(Attributes.f_22276_);
        if (!WaveScaling.skipRaisingHealth(mob) && health != null) {
            health.m_22120_(HEALTH_MODIFIER);
            health.m_22125_(new AttributeModifier(HEALTH_MODIFIER, "Spiral Abyss health", 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            double initial = health.m_22135_();
            double nativeTarget = (scaling.health().absolute() ? scaling.health().value() : initial * scaling.health().value()) * partyScale;
            health.m_22120_(HEALTH_MODIFIER);
            if (initial > 0.0 && Double.isFinite(nativeTarget) && nativeTarget > 0.0) {
                health.m_22125_(new AttributeModifier(HEALTH_MODIFIER, "Spiral Abyss health", nativeTarget / initial - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
                AbyssPlatform.data((Entity)mob).m_128473_(HEALTH_DAMAGE_RATIO_TAG);
            }
        }
        mob.m_21153_(mob.m_21233_());
        AbyssPlatform.data((Entity)mob).m_128347_("spiral_abyss_damage_scale", scaling.damage() * partyScale);
    }

    private static boolean skipRaisingHealth(LivingEntity mob) {
        if (!AbyssPlatform.data((Entity)mob).m_128471_("SkipRaisingHealth")) {
            return false;
        }
        AttributeInstance health = mob.m_21051_(Attributes.f_22276_);
        if (health != null) {
            health.m_22120_(HEALTH_MODIFIER);
        }
        AbyssPlatform.data((Entity)mob).m_128473_(HEALTH_DAMAGE_RATIO_TAG);
        return true;
    }
}

