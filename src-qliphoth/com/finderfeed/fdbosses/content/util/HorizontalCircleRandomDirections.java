/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 */
package com.finderfeed.fdbosses.content.util;

import java.util.Iterator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class HorizontalCircleRandomDirections
implements Iterable<Vec3> {
    private RandomSource randomSource;
    private float angle;
    private int directionsCount;
    private float randomDirectionsPercent;

    public HorizontalCircleRandomDirections(RandomSource randomSource, int directionsCount, float randomDirectionsPercent) {
        this.directionsCount = directionsCount;
        this.randomDirectionsPercent = randomDirectionsPercent;
        this.angle = (float)Math.PI * 2 / (float)directionsCount;
        this.randomSource = randomSource;
    }

    @Override
    @NotNull
    public Iterator<Vec3> iterator() {
        return new Iterator<Vec3>(){
            private int currentDirection = 0;

            @Override
            public boolean hasNext() {
                return this.currentDirection < HorizontalCircleRandomDirections.this.directionsCount;
            }

            @Override
            public Vec3 next() {
                float currentAngle = (float)this.currentDirection * HorizontalCircleRandomDirections.this.angle + HorizontalCircleRandomDirections.this.randomSource.m_188501_() * HorizontalCircleRandomDirections.this.randomDirectionsPercent * HorizontalCircleRandomDirections.this.angle;
                Vec3 v = new Vec3(1.0, 0.0, 0.0).m_82524_(currentAngle);
                ++this.currentDirection;
                return v;
            }
        };
    }
}

