/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.ai.attributes.Attribute
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.WaveScaling;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={AttributeInstance.class})
public abstract class AbyssHealthAttributeMixin {
    @Redirect(method={"calculateValue"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"))
    private double spiralAbyss$scaledHealth(Attribute attribute, double value) {
        double sanitized = attribute.m_6740_(value);
        if (attribute == Attributes.f_22276_ && Double.isFinite(value) && value > 1024.0 && ((AttributeInstance)this).m_22111_(WaveScaling.HEALTH_MODIFIER) != null) {
            return Math.max(sanitized, Math.min(3.4028234663852886E38, value));
        }
        return sanitized;
    }
}

