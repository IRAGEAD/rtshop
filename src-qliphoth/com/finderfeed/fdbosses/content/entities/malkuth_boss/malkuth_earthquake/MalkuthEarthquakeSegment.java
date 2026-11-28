/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class MalkuthEarthquakeSegment {
    private static FDModel BASE_MODEL;
    private float angle;
    private Type type;
    private int currentTime;
    private int maxTime;
    private Vec3 offset;

    public static FDModel getBaseModel() {
        if (BASE_MODEL == null) {
            BASE_MODEL = new FDModel(BossModels.MALKUTH_EARTHQUAKE_PART.get());
        }
        return BASE_MODEL;
    }

    public MalkuthEarthquakeSegment(Type type, Vec3 offset, float angle, int time) {
        this.angle = angle;
        this.type = type;
        this.maxTime = time;
        this.offset = offset;
    }

    public float getAngle() {
        return this.angle;
    }

    public Vec3 getOffset() {
        return this.offset;
    }

    public int getMaxTime() {
        return this.maxTime;
    }

    public Type getType() {
        return this.type;
    }

    public int getCurrentTime() {
        return this.currentTime;
    }

    public void tick() {
        ++this.currentTime;
    }

    public boolean hasFinished() {
        return this.currentTime >= this.maxTime;
    }

    public float getUpPercent(float pticks) {
        float time = (float)this.currentTime + pticks;
        float quarter = (float)this.maxTime * 0.25f;
        if (time <= quarter) {
            return FDEasings.easeOut((float)(time / quarter));
        }
        return FDEasings.easeOut((float)(1.0f - (time -= quarter) / ((float)this.maxTime - quarter)));
    }

    public static enum Type {
        FIRE_1(0, FDBosses.location("textures/entities/malkuth/malkuth_earthquake_fire_1.png"), MalkuthEarthquakeSegment::getBaseModel),
        FIRE_2(1, FDBosses.location("textures/entities/malkuth/malkuth_earthquake_fire_2.png"), MalkuthEarthquakeSegment::getBaseModel),
        ICE_1(2, FDBosses.location("textures/entities/malkuth/malkuth_earthquake_ice_1.png"), MalkuthEarthquakeSegment::getBaseModel),
        ICE_2(3, FDBosses.location("textures/entities/malkuth/malkuth_earthquake_ice_2.png"), MalkuthEarthquakeSegment::getBaseModel),
        ICE_SPIKE(4, FDBosses.location("textures/entities/malkuth/malkuth_big_ice_spike.png"), null);

        private ResourceLocation texture;
        private Supplier<FDModel> model;
        private int id;

        private Type(int id, ResourceLocation texture, Supplier<FDModel> model) {
            this.texture = texture;
            this.model = model;
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public Supplier<FDModel> getModel() {
            return this.model;
        }

        public boolean isModel() {
            return this.model != null;
        }

        public ResourceLocation getTexture() {
            return this.texture;
        }

        public static Type getRandomBaseSegment(Random random) {
            if (random.nextBoolean()) {
                if (random.nextBoolean()) {
                    return FIRE_1;
                }
                return FIRE_2;
            }
            float rnd = random.nextFloat();
            if (rnd > 0.66f) {
                return ICE_1;
            }
            if (rnd > 0.33f) {
                return ICE_2;
            }
            return FIRE_2;
        }

        public static Type getRandomBaseSegment(MalkuthAttackType attackType, Random random, boolean includeRockInIce) {
            int id = random.nextInt(2);
            if (attackType.isFire()) {
                if (id == 0) {
                    return FIRE_1;
                }
                return FIRE_2;
            }
            if (includeRockInIce && random.nextInt(2) == 1) {
                return FIRE_2;
            }
            if (id == 0) {
                return ICE_1;
            }
            return ICE_2;
        }
    }
}

