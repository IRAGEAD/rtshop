/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.LivingEntity
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.finderfeed.fdbosses.client;

import com.finderfeed.fdbosses.BossClientEvents;
import com.finderfeed.fdbosses.init.BossEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class BossClientMixinHandle {
    public static void darknessCalculate(LivingEntity entity, float pticks, CallbackInfoReturnable<Float> cir) {
        if (entity.m_21023_((MobEffect)BossEffects.CHESED_DARKEN.get()) || entity.m_21023_((MobEffect)BossEffects.CHESED_GAZE.get())) {
            MobEffectInstance mobeffectinstance = entity.m_21124_(MobEffects.f_19611_);
            float value = !mobeffectinstance.m_267633_(200) ? 1.0f : 0.7f + Mth.m_14031_((float)(((float)mobeffectinstance.m_19557_() - pticks) * (float)Math.PI * 0.2f)) * 0.3f;
            float percent = 1.0f - Math.max(BossClientEvents.getChesedGazePercent(pticks), BossClientEvents.getChesedDarkenPercent(pticks));
            cir.setReturnValue((Object)Float.valueOf(value * percent));
        }
    }
}

