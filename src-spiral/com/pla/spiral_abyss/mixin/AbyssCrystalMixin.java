/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.boss.enderdragon.EndCrystal
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.level.Level
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EndCrystal.class})
public abstract class AbyssCrystalMixin {
    @Inject(method={"onDestroyedBy"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$notifyOwner(DamageSource source, CallbackInfo ci) {
        ServerLevel level;
        EndCrystal crystal = (EndCrystal)this;
        Level level2 = crystal.m_9236_();
        if (!(level2 instanceof ServerLevel && (level = (ServerLevel)level2).m_46472_().equals(AbyssDimension.LEVEL) && AbyssPlatform.data((Entity)crystal).m_128403_("spiral_abyss_crystal_owner"))) {
            return;
        }
        Entity entity = level.m_8791_(AbyssPlatform.data((Entity)crystal).m_128342_("spiral_abyss_crystal_owner"));
        if (entity instanceof EnderDragon) {
            EnderDragon dragon = (EnderDragon)entity;
            dragon.m_31124_(crystal, crystal.m_20183_(), source);
        }
        ci.cancel();
    }
}

