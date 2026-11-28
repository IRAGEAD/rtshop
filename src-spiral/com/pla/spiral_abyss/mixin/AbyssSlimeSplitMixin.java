/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.monster.Slime
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.platform.AbyssPlatform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={Slime.class})
public abstract class AbyssSlimeSplitMixin {
    @Redirect(method={"remove"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/monster/Slime;getSize()I"))
    private int spiralAbyss$noWaveSplitting(Slime slime) {
        return AbyssPlatform.data((Entity)slime).m_128403_("spiral_abyss_run") ? 1 : slime.m_33632_();
    }
}

