/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 */
package com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations;

import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponRotation;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.finderfeed.fdlib.util.math.FDMathUtil;

public class GeburahLerpingRotation
extends GeburahWeaponRotation {
    public static final NetworkCodec<GeburahLerpingRotation> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.rotationSnapshot), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.maxSpeed), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.angle), (rotationSnapshot, maxspeed, angle) -> {
        GeburahLerpingRotation lerpingRotation = new GeburahLerpingRotation(null, maxspeed.floatValue(), angle.floatValue());
        lerpingRotation.rotationSnapshot = rotationSnapshot.floatValue();
        return lerpingRotation;
    });
    private float rotationSnapshot;
    private float maxSpeed;
    private float angle;
    private boolean finished = false;

    public GeburahLerpingRotation(GeburahWeaponRotationController controller, float maxSpeed, float moveAngle) {
        if (controller != null) {
            this.rotationSnapshot = controller.getCurrentRotation();
        }
        this.maxSpeed = maxSpeed;
        this.angle = moveAngle;
    }

    public GeburahLerpingRotation(GeburahLerpingRotation rotation) {
        this.rotationSnapshot = rotation.rotationSnapshot;
        this.maxSpeed = rotation.maxSpeed;
        this.angle = rotation.angle;
        this.finished = rotation.finished;
    }

    @Override
    public void tick() {
        GeburahWeaponRotationController handler = this.rotatingWeaponsHandler;
        float targetRotation = this.rotationSnapshot + this.angle;
        float remainingRotation = targetRotation - handler.currentRotation;
        float speed = FDMathUtil.lerp((float)0.0f, (float)this.maxSpeed, (float)Math.max(0.0f, remainingRotation / this.angle));
        if ((double)speed < 0.01) {
            this.finished = true;
        }
        handler.currentRotation = this.angle > 0.0f ? (handler.currentRotation += speed) : (handler.currentRotation -= speed);
    }

    @Override
    public boolean finishedRotation() {
        return this.finished;
    }

    @Override
    public boolean shouldPlayRotationSound() {
        return true;
    }

    @Override
    public GeburahWeaponRotation.Type rotationType() {
        return GeburahWeaponRotation.Type.LERPING_ROTATE_BY;
    }
}

