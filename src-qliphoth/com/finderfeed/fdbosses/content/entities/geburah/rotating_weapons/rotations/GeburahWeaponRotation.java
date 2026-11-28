/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 */
package com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations;

import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahConstantWeaponRotation;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahLerpingRotation;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponsRotateTo;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import java.util.function.Function;

public abstract class GeburahWeaponRotation {
    protected GeburahWeaponRotationController rotatingWeaponsHandler;

    public abstract void tick();

    public abstract boolean finishedRotation();

    public abstract boolean shouldPlayRotationSound();

    public abstract Type rotationType();

    public void setRotatingWeaponsHandler(GeburahWeaponRotationController rotatingWeaponsHandler) {
        this.rotatingWeaponsHandler = rotatingWeaponsHandler;
    }

    public static enum Type {
        ROTATE_TO(GeburahWeaponsRotateTo.STREAM_CODEC, v -> new GeburahWeaponsRotateTo((GeburahWeaponsRotateTo)v)),
        LERPING_ROTATE_BY(GeburahLerpingRotation.STREAM_CODEC, v -> new GeburahLerpingRotation((GeburahLerpingRotation)v)),
        CONSTANT_ROTATION(GeburahConstantWeaponRotation.STREAM_CODEC, v -> new GeburahConstantWeaponRotation((GeburahConstantWeaponRotation)v));

        public final NetworkCodec<? extends GeburahWeaponRotation> codec;
        public final Function<GeburahWeaponRotation, GeburahWeaponRotation> copyHandler;

        private Type(NetworkCodec<? extends GeburahWeaponRotation> codec, Function<GeburahWeaponRotation, GeburahWeaponRotation> copyHandler) {
            this.codec = codec;
            this.copyHandler = copyHandler;
        }
    }
}

