/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Camera
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Camera.class})
public abstract class PortalEntryCameraMixin {
    @Shadow
    private boolean f_90560_;

    @Shadow
    protected abstract void m_90581_(Vec3 var1);

    @Shadow
    protected abstract void m_90572_(float var1, float var2);

    @Inject(method={"setup"}, at={@At(value="TAIL")})
    private void spiralAbyss$entryCamera(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo callback) {
        PortalEntryAnimation.CameraPose pose = PortalEntryAnimation.camera();
        if (pose == null) {
            return;
        }
        this.f_90560_ = true;
        this.m_90581_(pose.position());
        this.m_90572_(pose.yaw(), pose.pitch());
    }
}

