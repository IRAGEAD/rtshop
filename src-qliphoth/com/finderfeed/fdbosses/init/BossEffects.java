/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectCategory
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.RegistryObject
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.effects.MalkuthCowardEffect;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.effects.MalkuthKnightEffect;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.effects.SinnerEffect;
import com.finderfeed.fdbosses.effects.SimpleEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class BossEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create((ResourceKey)Registries.f_256929_, (String)"fdbosses");
    public static final RegistryObject<MobEffect> CHESED_GAZE = MOB_EFFECTS.register("chesed_gaze", () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 6823167));
    public static final RegistryObject<MobEffect> CHESED_DARKEN = MOB_EFFECTS.register("chesed_darken", () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0));
    public static final RegistryObject<MobEffect> CHESED_ENERGIZED = MOB_EFFECTS.register("chesed_energized", () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 60889));
    public static final RegistryObject<MobEffect> SHOCKED = MOB_EFFECTS.register("shocked", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 60889));
    public static final RegistryObject<MobEffect> MARK_OF_A_KNIGHT = MOB_EFFECTS.register("knight", MalkuthKnightEffect::new);
    public static final RegistryObject<MobEffect> MARK_OF_A_COWARD = MOB_EFFECTS.register("coward", MalkuthCowardEffect::new);
    public static final RegistryObject<MobEffect> SINNER = MOB_EFFECTS.register("sinner", SinnerEffect::new);
}

