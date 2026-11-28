/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 */
package com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations;

import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponRotation;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;

public class GeburahConstantWeaponRotation
extends GeburahWeaponRotation {
    public static final NetworkCodec<GeburahConstantWeaponRotation> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.rotationSpeed), speed -> {
        GeburahConstantWeaponRotation geburahConstantWeaponRotation = new GeburahConstantWeaponRotation(speed.floatValue());
        return geburahConstantWeaponRotation;
    });
    private float rotationSpeed;
    private boolean playSound = false;

    public GeburahConstantWeaponRotation(GeburahConstantWeaponRotation other) {
        this.rotationSpeed = other.rotationSpeed;
    }

    public GeburahConstantWeaponRotation(float speed) {
        this.rotationSpeed = speed;
    }

    @Override
    public void tick() {
        GeburahWeaponRotationController handler = this.rotatingWeaponsHandler;
        handler.currentRotation += this.rotationSpeed;
    }

    @Override
    public boolean finishedRotation() {
        return false;
    }

    @Override
    public boolean shouldPlayRotationSound() {
        return this.playSound;
    }

    @Override
    public GeburahWeaponRotation.Type rotationType() {
        return GeburahWeaponRotation.Type.CONSTANT_ROTATION;
    }

    public float getRotationSpeed() {
        return this.rotationSpeed;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }
}

