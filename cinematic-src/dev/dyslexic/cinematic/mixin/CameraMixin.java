/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Camera
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Camera.class})
public abstract class CameraMixin {
    @Shadow
    @Final
    private Quaternionf f_90559_;
    @Shadow
    @Final
    private Vector3f f_90554_;
    @Shadow
    @Final
    private Vector3f f_90555_;
    @Shadow
    @Final
    private Vector3f f_90556_;

    @Shadow
    protected abstract void m_90581_(Vec3 var1);

    @Shadow
    protected abstract void m_90572_(float var1, float var2);

    @Inject(method={"setup"}, at={@At(value="TAIL")})
    private void cinematic$scene(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        CameraRig.Pose pose;
        LocalPlayer player;
        Camera self = (Camera)this;
        float yaw = self.m_90590_();
        float pitch = self.m_90589_();
        if (Scene.active() && entity instanceof LocalPlayer && !(player = (LocalPlayer)entity).m_20159_()) {
            yaw = Mth.m_14189_((float)partialTick, (float)player.f_19859_, (float)player.m_146908_());
            pitch = Mth.m_14179_((float)partialTick, (float)player.f_19860_, (float)player.m_146909_());
        }
        if ((pose = CameraRig.override(self.m_90583_(), yaw, pitch)) != null) {
            this.m_90572_(pose.yaw(), pose.pitch());
            if (pose.roll() != 0.0f) {
                float deg = (float)Math.PI / 180;
                this.f_90559_.rotationYXZ(-pose.yaw() * deg, pose.pitch() * deg, pose.roll() * deg);
                this.f_90554_.set(0.0f, 0.0f, 1.0f).rotate((Quaternionfc)this.f_90559_);
                this.f_90555_.set(0.0f, 1.0f, 0.0f).rotate((Quaternionfc)this.f_90559_);
                this.f_90556_.set(1.0f, 0.0f, 0.0f).rotate((Quaternionfc)this.f_90559_);
            }
            this.m_90581_(pose.pos());
        }
    }
}

