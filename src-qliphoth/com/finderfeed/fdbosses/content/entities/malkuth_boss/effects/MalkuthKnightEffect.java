/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectCategory
 *  net.minecraft.world.item.ItemStack
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.effects;

import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

public class MalkuthKnightEffect
extends MobEffect {
    public MalkuthKnightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4F4F4F);
    }

    public List<ItemStack> getCurativeItems() {
        return List.of();
    }
}

