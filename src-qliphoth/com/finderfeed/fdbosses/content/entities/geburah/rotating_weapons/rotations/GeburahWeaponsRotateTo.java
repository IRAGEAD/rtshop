/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.util.Mth
 */
package com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations;

import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponRotation;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import net.minecraft.util.Mth;

public class GeburahWeaponsRotateTo
extends GeburahWeaponRotation {
    public static final NetworkCodec<GeburahWeaponsRotateTo> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.rotationSnapshot), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.rotationTarget), (NetworkCodec)NetworkCodec.INT, v -> v.rotationTime, (NetworkCodec)NetworkCodec.INT, v -> v.currentRotationTime, (NetworkCodec)NetworkCodec.BOOL, v -> v.easeInOut, (aFloat, aFloat2, integer, integer2, easeInOut) -> {
        GeburahWeaponsRotateTo rotateTo = new GeburahWeaponsRotateTo(null, aFloat2.floatValue(), (int)integer, (boolean)easeInOut);
        rotateTo.currentRotationTime = integer2;
        rotateTo.rotationSnapshot = aFloat.floatValue();
        return rotateTo;
    });
    private float rotationSnapshot;
    private float rotationTarget;
    private int rotationTime;
    private boolean easeInOut;
    private int currentRotationTime = 0;

    public GeburahWeaponsRotateTo(GeburahWeaponRotationController rotationHandler, float rotationTarget, int rotationTime, boolean easeInOut) {
        if (rotationHandler != null) {
            this.rotationSnapshot = rotationHandler.currentRotation;
        }
        this.rotationTarget = rotationTarget;
        this.rotationTime = rotationTime;
        this.easeInOut = easeInOut;
    }

    public GeburahWeaponsRotateTo(GeburahWeaponRotationController rotationHandler, float rotationTarget, int rotationTime) {
        this(rotationHandler, rotationTarget, rotationTime, true);
    }

    public GeburahWeaponsRotateTo(GeburahWeaponsRotateTo other) {
        this.rotationSnapshot = other.rotationSnapshot;
        this.rotationTarget = other.rotationTarget;
        this.currentRotationTime = other.currentRotationTime;
        this.rotationTime = other.rotationTime;
        this.easeInOut = other.easeInOut;
    }

    @Override
    public void tick() {
        float rotation;
        this.currentRotationTime = Mth.m_14045_((int)(this.currentRotationTime + 1), (int)0, (int)this.rotationTime);
        float p = (float)this.currentRotationTime / (float)this.rotationTime;
        p = this.easeInOut ? FDEasings.easeInOut((float)p) : FDEasings.easeOut((float)p);
        this.rotatingWeaponsHandler.currentRotation = rotation = FDMathUtil.lerp((float)this.rotationSnapshot, (float)this.rotationTarget, (float)p);
    }

    @Override
    public boolean finishedRotation() {
        return this.rotationTime == this.currentRotationTime;
    }

    @Override
    public boolean shouldPlayRotationSound() {
        return true;
    }

    @Override
    public GeburahWeaponRotation.Type rotationType() {
        return GeburahWeaponRotation.Type.ROTATE_TO;
    }
}

