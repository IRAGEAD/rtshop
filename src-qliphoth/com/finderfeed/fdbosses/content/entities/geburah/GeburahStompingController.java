/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.client.particles.arc_preparation_particle.ArcAttackPreparationParticleOptions;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_earthquake.GeburahEarthquake;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class GeburahStompingController
implements AutoSerializable {
    @SerializableField
    private float travelSpeed = 1.0f;
    @SerializableField
    private int stompTime = -1;
    private List<StompInstance> stomps;
    @SerializableField
    private float damage = 1.0f;
    private GeburahEntity geburah;
    private int stompRadius;

    public GeburahStompingController(GeburahEntity geburah, int stompRadius) {
        this.geburah = geburah;
        this.stompRadius = stompRadius;
    }

    public void tick() {
        if (this.stompTime > 0) {
            this.stompTime = Mth.m_14045_((int)(this.stompTime - 1), (int)0, (int)Integer.MAX_VALUE);
        } else if (this.stompTime == 0) {
            this.summonStomps();
            this.stompTime = -1;
            this.stomps = null;
        }
    }

    private void summonStomps() {
        if (this.stomps == null) {
            return;
        }
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.geburah.m_9236_()), (FDShakeData)FDShakeData.builder().outTime(8).amplitude(0.7f).frequency(10.0f).build(), (Vec3)this.geburah.m_20182_(), (double)120.0);
        for (StompInstance stomp : this.stomps) {
            Vec2 direction = stomp.direction;
            float angle = stomp.angle;
            GeburahEarthquake geburahEarthquake = GeburahEarthquake.summon(this.geburah.m_9236_(), this.geburah.m_20097_(), 5, this.stompRadius, this.travelSpeed, this.damage, new Vec3((double)direction.f_82470_, 0.0, (double)direction.f_82471_), angle);
        }
        if (!this.stomps.isEmpty()) {
            this.geburah.m_9236_().m_6263_(null, this.geburah.m_20185_(), this.geburah.m_20186_(), this.geburah.m_20189_(), BossSounds.GEBURAH_STOMP.get(), SoundSource.HOSTILE, 3.0f, 0.6f);
        }
    }

    public void stomp(int stompPrepareTime, boolean prepareParticles, float travelSpeed, float damage, StompInstance ... stompInstances) {
        this.stomp(stompPrepareTime, prepareParticles, travelSpeed, damage, Arrays.stream(stompInstances).toList());
    }

    public void stompFullCircle(int stompPrepareTime, boolean prepareParticles, float travelSpeed, float damage) {
        ArrayList<StompInstance> stompInstances = new ArrayList<StompInstance>();
        stompInstances.add(new StompInstance(new Vec2(1.0f, 0.0f), 0.7853982f));
        stompInstances.add(new StompInstance(new Vec2(-1.0f, 0.0f), 0.7853982f));
        stompInstances.add(new StompInstance(new Vec2(0.0f, 1.0f), 0.7853982f));
        stompInstances.add(new StompInstance(new Vec2(0.0f, -1.0f), 0.7853982f));
        this.stomp(stompPrepareTime, prepareParticles, travelSpeed, damage, stompInstances);
    }

    public void stomp(int stompPrepareTime, boolean prepareParticles, float travelSpeed, float damage, List<StompInstance> stompInstances) {
        if (this.stompTime == -1 && !stompInstances.isEmpty()) {
            this.stompTime = stompPrepareTime;
            this.damage = damage;
            this.travelSpeed = travelSpeed;
            this.causeStompAnimation(stompPrepareTime);
            if (prepareParticles) {
                this.sendPrepareParticles(stompPrepareTime, stompInstances);
            }
            this.stomps = new ArrayList<StompInstance>(stompInstances);
        }
    }

    private void sendPrepareParticles(int stompTime, List<StompInstance> stompInstances) {
        for (StompInstance stomp : stompInstances) {
            Vec2 direction = stomp.direction;
            float angle = stomp.angle;
            int fadeIn = Math.round((float)stompTime * 0.25f);
            int fadeOut = 10;
            ArcAttackPreparationParticleOptions options = new ArcAttackPreparationParticleOptions(new Vec3((double)direction.f_82470_, 0.0, (double)direction.f_82471_), this.stompRadius + 2, angle, stompTime, fadeIn, fadeOut, 1.0f, 0.1f, 0.1f, 0.2f);
            FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.geburah.m_9236_()), (ParticleOptions)options, (Vec3)this.geburah.m_20182_().m_82520_(0.0, (double)0.01f, 0.0), (double)120.0);
        }
    }

    private void causeStompAnimation(int stompTime) {
        if (stompTime != 0) {
            AnimationSystem animSystem = this.geburah.getAnimationSystem();
            Animation animation = (Animation)BossAnims.GEBURAH_STOMP.get();
            int animLength = animation.getAnimTime();
            float animationSpeed = (float)animLength / (float)stompTime;
            animSystem.startAnimation("stomping", AnimationTicker.builder((Animation)animation).setSpeed(animationSpeed).build());
        }
    }

    public void autoSave(CompoundTag tag) {
        super.autoSave(tag);
        if (this.stomps != null) {
            int size = this.stomps.size();
            tag.m_128405_("stomps", size);
            for (int i = 0; i < size; ++i) {
                StompInstance stompInstance = this.stomps.get(i);
                Vec2 direction = stompInstance.direction;
                float angle = stompInstance.angle;
                tag.m_128350_("stomp_" + i + "_direction_x", direction.f_82470_);
                tag.m_128350_("stomp_" + i + "_direction_z", direction.f_82471_);
                tag.m_128350_("stomp_" + i + "_angle", angle);
            }
        }
    }

    public void autoLoad(CompoundTag tag) {
        super.autoLoad(tag);
        if (tag.m_128441_("stomps")) {
            this.stomps = new ArrayList<StompInstance>();
            int size = tag.m_128451_("stomps");
            for (int i = 0; i < size; ++i) {
                Vec2 direction = new Vec2(tag.m_128457_("stomp_" + i + "_direction_x"), tag.m_128457_("stomp_" + i + "_direction_z"));
                float angle = tag.m_128457_("stomp_" + i + "_angle");
                this.stomps.add(new StompInstance(direction, angle));
            }
        }
    }

    public static class StompInstance {
        public Vec2 direction;
        public float angle;

        public StompInstance(Vec2 direction, float angle) {
            this.direction = direction.m_165902_();
            this.angle = angle;
        }
    }
}

