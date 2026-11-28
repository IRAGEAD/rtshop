/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.forge.mixin;

import com.pla.spiral_abyss.encounter.QliphothEncounters;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity"}, remap=false)
public abstract class QliphothSpawnerMixin {
    @Inject(method={"spawn"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiral_abyss$controllerOwnsSpawn(CallbackInfo ci) {
        if (QliphothEncounters.helper((Entity)this)) {
            ci.cancel();
        }
    }

    @Inject(method={"setActive"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiral_abyss$keepArenaSpawnerInactive(boolean active, CallbackInfo ci) {
        if (active && QliphothEncounters.helper((Entity)this)) {
            ci.cancel();
        }
    }
}

