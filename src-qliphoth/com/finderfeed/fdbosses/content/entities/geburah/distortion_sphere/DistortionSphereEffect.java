/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.distortion_sphere;

import com.finderfeed.fdlib.util.rendering.FDEasings;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class DistortionSphereEffect {
    public int currentTime = 0;
    public Vec3 position;
    public int effectTime;
    public float sphereRadius;
    public float sphereWidth;
    public float yFloorPos;

    public DistortionSphereEffect(Vec3 position, int effectTime, float sphereMaxRadius, float sphereWidth, float yFloorPos) {
        this.position = position;
        this.effectTime = effectTime;
        this.sphereRadius = sphereMaxRadius;
        this.sphereWidth = sphereWidth;
        this.yFloorPos = yFloorPos;
    }

    public boolean tick() {
        this.currentTime = Mth.m_14045_((int)(this.currentTime + 1), (int)0, (int)this.effectTime);
        return this.currentTime == this.effectTime;
    }

    public float getSphereRadius(float pticks) {
        float p = FDEasings.easeOut((float)this.getPercent(pticks));
        return p * this.sphereRadius;
    }

    public float getInnerSphereRadius(float pticks) {
        float p = FDEasings.easeOut((float)this.getPercent(pticks));
        return Mth.m_14036_((float)(p * this.sphereRadius - this.sphereWidth), (float)0.0f, (float)2.1474836E9f);
    }

    public float getEffectStrength(float pticks) {
        float p = FDEasings.easeOut((float)(1.0f - this.getPercent(pticks)));
        return p;
    }

    public float getPercent(float pticks) {
        float p = Mth.m_14036_((float)(((float)this.currentTime + pticks) / (float)this.effectTime), (float)0.0f, (float)1.0f);
        return p;
    }
}

