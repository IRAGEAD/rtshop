/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.particle.ParticleProcessor
 *  com.finderfeed.fdlib.systems.particle.ParticleProcessorType
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.finderfeed.fdlib.util.FDCodecs
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.Vec3
 *  org.joml.AxisAngle4f
 *  org.joml.Quaternionf
 *  org.joml.Vector3d
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.client.particles.particle_processors;

import com.finderfeed.fdbosses.BossModEvents;
import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdlib.systems.particle.ParticleProcessor;
import com.finderfeed.fdlib.systems.particle.ParticleProcessorType;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.finderfeed.fdlib.util.FDCodecs;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class ChesedRayCircleParticleProcessor
implements ParticleProcessor<ChesedRayCircleParticleProcessor> {
    private Vec3 circleAround;
    private boolean forward = true;
    private float circleCount = 1.0f;
    private Vec3 previousPoint = null;
    private Vec3 initialPoint = null;
    private Vector3f axis = null;
    private Vec3 oldSpeed;

    public ChesedRayCircleParticleProcessor(Vec3 circleAround, float circleCount, boolean forward) {
        this.circleAround = circleAround;
        this.circleCount = circleCount;
        this.forward = forward;
        this.oldSpeed = Vec3.f_82478_;
    }

    public ParticleProcessorType<ChesedRayCircleParticleProcessor> type() {
        return BossModEvents.CHESED_CIRCLE_PARTICLE_PROCESSOR_PARTICLE_PROCESSOR_TYPE;
    }

    public void processParticle(Particle particle) {
        int lifetime = particle.f_107225_ - 2;
        if (particle.f_107224_ <= lifetime) {
            float p2 = (float)particle.f_107224_ / (float)lifetime;
            p2 = FDEasings.easeInOut((float)p2);
            double len = this.initialPoint.m_82546_(this.circleAround).m_82553_();
            float fullAngle = (float)Math.PI * 2 * this.circleCount;
            float targetAngle = fullAngle * p2;
            if (!this.forward) {
                targetAngle = -targetAngle;
            }
            Quaternionf q = new Quaternionf(new AxisAngle4f(targetAngle, this.axis.x, this.axis.y, this.axis.z));
            Vector3d p = q.transform((this.initialPoint.f_82479_ - this.circleAround.f_82479_) / len, (this.initialPoint.f_82480_ - this.circleAround.f_82480_) / len, (this.initialPoint.f_82481_ - this.circleAround.f_82481_) / len, new Vector3d());
            Vec3 targetPoint = this.circleAround.m_82520_(p.x * len, p.y * len, p.z * len);
            Vec3 speed = targetPoint.m_82546_(this.previousPoint);
            this.previousPoint = targetPoint;
            particle.f_107215_ -= this.oldSpeed.f_82479_;
            particle.f_107216_ -= this.oldSpeed.f_82480_;
            particle.f_107217_ -= this.oldSpeed.f_82481_;
            particle.f_107215_ += speed.f_82479_;
            particle.f_107216_ += speed.f_82480_;
            particle.f_107217_ += speed.f_82481_;
            this.oldSpeed = speed;
        }
    }

    public void init(Particle particle) {
        this.initialPoint = new Vec3(particle.f_107212_, particle.f_107213_, particle.f_107214_);
        Vec3 b = this.initialPoint.m_82546_(this.circleAround);
        Vec3 left = b.m_82537_(new Vec3(0.0, 1.0, 0.0));
        Vec3 axis = left.m_82537_(b);
        this.axis = new Vector3f((float)axis.f_82479_, (float)axis.f_82480_, (float)axis.f_82481_).normalize();
        this.previousPoint = this.initialPoint;
    }

    public static class Type
    implements ParticleProcessorType<ChesedRayCircleParticleProcessor> {
        public static final NetworkCodec<ChesedRayCircleParticleProcessor> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.VEC3, v -> v.circleAround, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.circleCount), (NetworkCodec)NetworkCodec.BOOL, v -> v.forward, ChesedRayCircleParticleProcessor::new);
        public static final Codec<ChesedRayCircleParticleProcessor> CODEC = RecordCodecBuilder.create(p -> p.group((App)FDCodecs.VEC3.fieldOf("circleAround").forGetter(v -> v.circleAround), (App)Codec.FLOAT.fieldOf("circleCount").forGetter(v -> Float.valueOf(v.circleCount)), (App)Codec.BOOL.fieldOf("forward").forGetter(v -> v.forward)).apply((Applicative)p, ChesedRayCircleParticleProcessor::new));

        public NetworkCodec<ChesedRayCircleParticleProcessor> networkCodec() {
            return STREAM_CODEC;
        }

        public Codec<ChesedRayCircleParticleProcessor> codec() {
            return CODEC;
        }

        public ResourceLocation id() {
            return FDBosses.location("move_on_a_circle_processor");
        }
    }
}

