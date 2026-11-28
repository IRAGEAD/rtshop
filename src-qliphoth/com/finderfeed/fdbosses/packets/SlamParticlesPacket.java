/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.packets;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:slam_particles")
public class SlamParticlesPacket
extends FDPacket {
    private SlamData slamData;

    public SlamParticlesPacket(SlamData data) {
        this.slamData = data;
    }

    public SlamParticlesPacket(FriendlyByteBuf buf) {
        this.slamData = (SlamData)SlamData.STREAM_CODEC.fromNetwork(buf);
    }

    public void write(FriendlyByteBuf buf) {
        SlamData.STREAM_CODEC.toNetwork(buf, (Object)this.slamData);
    }

    public void clientAction(Supplier<NetworkEvent.Context> context) {
        BossClientPackets.blockProjectileSlamParticles(this.slamData);
    }

    public void serverAction(Supplier<NetworkEvent.Context> context) {
    }

    public static class SlamData {
        public static final NetworkCodec<SlamData> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.VEC3, v -> v.pos, (NetworkCodec)NetworkCodec.VEC3, v -> v.direction, (NetworkCodec)NetworkCodec.BLOCK_POS, v -> v.bPos, (NetworkCodec)NetworkCodec.INT, v -> v.collectRadius, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.maxSpeed), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.maxVerticalSpeedEdges), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.maxVerticalSpeedCenter), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.perRowDivide), (NetworkCodec)NetworkCodec.INT, v -> v.count, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.maxAngle), (NetworkCodec)NetworkCodec.INT, v -> v.maxParticleLifetime, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.particleSizeMult), (pos, direction, bPos, collectRadius, maxSpeed, maxVerticalSpeedEdges, maxVerticalSpeedCenter, perRowDivide, count, maxAngle, maxParticleLifetime, particleSize) -> {
            SlamData data = new SlamData((BlockPos)bPos, (Vec3)pos, (Vec3)direction);
            data.maxSpeed = maxSpeed.floatValue();
            data.collectRadius = collectRadius;
            data.maxVerticalSpeedEdges = maxVerticalSpeedEdges.floatValue();
            data.maxVerticalSpeedCenter = maxVerticalSpeedCenter.floatValue();
            data.count = count;
            data.maxAngle = maxAngle.floatValue();
            data.perRowDivide = perRowDivide.floatValue();
            data.maxParticleLifetime = maxParticleLifetime;
            data.particleSizeMult = particleSize.floatValue();
            return data;
        });
        public Vec3 pos;
        public Vec3 direction;
        public BlockPos bPos;
        public int collectRadius = 3;
        public float maxSpeed = 0.75f;
        public float maxVerticalSpeedEdges = 0.4f;
        public float maxVerticalSpeedCenter = 0.6f;
        public float particleSizeMult = 2.0f;
        public float perRowDivide = 0.25f;
        public int count = 30;
        public int maxParticleLifetime = 60;
        public float maxAngle = 1.0471976f;

        public SlamData(BlockPos bPos, Vec3 pos, Vec3 direction) {
            this.pos = pos;
            this.bPos = bPos;
            this.direction = direction;
        }

        public SlamData particleSizeMultiplier(float mult) {
            this.particleSizeMult = mult;
            return this;
        }

        public SlamData maxAngle(float maxAngle) {
            this.maxAngle = maxAngle;
            return this;
        }

        public SlamData perRowDivide(float perRowDivide) {
            this.perRowDivide = perRowDivide;
            return this;
        }

        public SlamData count(int count) {
            this.count = count;
            return this;
        }

        public SlamData maxParticleLifetime(int lifetime) {
            this.maxParticleLifetime = lifetime;
            return this;
        }

        public SlamData collectRadius(int collectRadius) {
            this.collectRadius = collectRadius;
            return this;
        }

        public SlamData maxSpeed(float maxSpeed) {
            this.maxSpeed = maxSpeed;
            return this;
        }

        public SlamData maxVerticalSpeedEdges(float speed) {
            this.maxVerticalSpeedEdges = speed;
            return this;
        }

        public SlamData maxVerticalSpeedCenter(float speed) {
            this.maxVerticalSpeedCenter = speed;
            return this;
        }
    }
}

