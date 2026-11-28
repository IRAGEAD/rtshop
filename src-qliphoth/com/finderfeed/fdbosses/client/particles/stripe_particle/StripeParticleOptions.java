/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.finderfeed.fdlib.util.FDCodecs
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleOptions$Deserializer
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.client.particles.stripe_particle;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.finderfeed.fdlib.util.FDCodecs;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class StripeParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<StripeParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<StripeParticleOptions>(){

        public StripeParticleOptions fromCommand(ParticleType<StripeParticleOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return new StripeParticleOptions();
        }

        public StripeParticleOptions fromNetwork(ParticleType<StripeParticleOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (StripeParticleOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final Codec<StripeParticleOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)FDCodecs.COLOR.fieldOf("startColor").forGetter(v -> v.startColor), (App)FDCodecs.COLOR.fieldOf("endColor").forGetter(v -> v.endColor), (App)Codec.INT.fieldOf("lifetime").forGetter(v -> v.lifetime), (App)Codec.INT.fieldOf("shapeVerties").forGetter(v -> v.shapeVertices), (App)Codec.INT.fieldOf("lod").forGetter(v -> v.lod), (App)Codec.FLOAT.fieldOf("scale").forGetter(v -> Float.valueOf(v.scale)), (App)Codec.FLOAT.fieldOf("stripePercentLength").forGetter(v -> Float.valueOf(v.stripePercentLength)), (App)Codec.FLOAT.fieldOf("startInPercent").forGetter(v -> Float.valueOf(v.startInPercent)), (App)Codec.FLOAT.fieldOf("endOutPercent").forGetter(v -> Float.valueOf(v.endOutPercent)), (App)Codec.list((Codec)FDCodecs.VEC3).fieldOf("offsets").forGetter(v -> v.offsets)).apply((Applicative)p, (startColor, endColor, lifetime, shapeVertices, lod, scale, stripePercentLength, startInPercent, endOutPercent, offsets) -> StripeParticleOptions.builder().startColor((FDColor)startColor).endColor((FDColor)endColor).shapeVertices((int)shapeVertices).lifetime((int)lifetime).scale(scale.floatValue()).stripePercentLength(stripePercentLength.floatValue()).startInPercent(startInPercent.floatValue()).endOutPercent(endOutPercent.floatValue()).offsets((List<Vec3>)offsets).build()));
    public static final NetworkCodec<StripeParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.COLOR, v -> v.startColor, (NetworkCodec)NetworkCodec.COLOR, v -> v.endColor, (NetworkCodec)NetworkCodec.INT, v -> v.lifetime, (NetworkCodec)NetworkCodec.INT, v -> v.shapeVertices, (NetworkCodec)NetworkCodec.INT, v -> v.lod, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.scale), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.stripePercentLength), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.startInPercent), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.endOutPercent), (NetworkCodec)NetworkCodec.listOf((NetworkCodec)NetworkCodec.VEC3), v -> v.offsets, (startColor, endColor, lifetime, shapeVertices, lod, scale, stripePercentLength, startInPercent, endOutPercent, offsets) -> StripeParticleOptions.builder().startColor((FDColor)startColor).endColor((FDColor)endColor).lifetime((int)lifetime).scale(scale.floatValue()).stripePercentLength(stripePercentLength.floatValue()).startInPercent(startInPercent.floatValue()).endOutPercent(endOutPercent.floatValue()).shapeVertices((int)shapeVertices).offsets((List<Vec3>)offsets).build());
    private float stripePercentLength;
    private int shapeVertices = 3;
    private FDColor startColor = new FDColor(1.0f, 1.0f, 1.0f, 1.0f);
    private FDColor endColor = new FDColor(1.0f, 1.0f, 1.0f, 1.0f);
    private int lifetime = 20;
    private List<Vec3> offsets = new ArrayList<Vec3>();
    private float scale = 1.0f;
    private int lod = 25;
    private float startInPercent = 0.5f;
    private float endOutPercent = 0.5f;

    private StripeParticleOptions() {
    }

    public StripeParticleOptions(FDColor startColor, FDColor endColor, int lifetime, int lod, float scale, float stripePercentLength, Vec3 ... offsets) {
        this.offsets = List.of(offsets);
        this.endColor = endColor;
        this.startColor = startColor;
        this.lifetime = lifetime;
        this.scale = scale;
        this.lod = lod;
        this.stripePercentLength = stripePercentLength;
    }

    public StripeParticleOptions(FDColor startColor, FDColor endColor, int lifetime, int lod, float scale, float stripePercentLength, List<Vec3> offsets) {
        this(startColor, endColor, lifetime, lod, scale, stripePercentLength, new Vec3[0]);
        this.offsets = offsets;
    }

    public static StripeParticleOptions createHorizontalCircling(FDColor startColor, FDColor endColor, Vec3 direction, float startingAngle, float scale, int lifetime, int lod, float verticalDistance, float radius, float circlesAmount, float stripePercentLength, boolean circleDirection, boolean in) {
        direction = direction.m_82541_();
        Vec3 startingHorizontalDirection = new Vec3(1.0, 0.0, 0.0).m_82524_(startingAngle);
        float step = 0.3926991f;
        float wholeRotation = circlesAmount * (float)Math.PI * 2.0f;
        ArrayList<Vec3> positions = new ArrayList<Vec3>();
        for (float i = 0.0f; i < wholeRotation; i += step) {
            float p = in ? 1.0f - i / wholeRotation : i / wholeRotation;
            float currentRadius = p * radius;
            float currentVerticalOffset = verticalDistance * p;
            Vec3 dir = startingHorizontalDirection.m_82542_((double)currentRadius, (double)currentRadius, (double)currentRadius).m_82524_(circleDirection ? i : -i);
            positions.add(dir.m_82520_((double)0.001f, (double)currentVerticalOffset, (double)0.001f));
        }
        Matrix4f transform = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)transform, (Vec3)direction);
        for (int i = 0; i < positions.size(); ++i) {
            Vec3 oldpos = (Vec3)positions.get(i);
            Vector3f v = transform.transformPosition((float)oldpos.f_82479_, (float)oldpos.f_82480_, (float)oldpos.f_82481_, new Vector3f());
            positions.set(i, new Vec3((double)v.x, (double)v.y, (double)v.z));
        }
        return new StripeParticleOptions(startColor, endColor, lifetime, lod, scale, stripePercentLength, positions);
    }

    public float getEndOutPercent() {
        return this.endOutPercent;
    }

    public float getStartInPercent() {
        return this.startInPercent;
    }

    public int getLOD() {
        return this.lod;
    }

    public float getStripePercentLength() {
        return this.stripePercentLength;
    }

    public float getScale() {
        return this.scale;
    }

    public FDColor getStartColor() {
        return this.startColor;
    }

    public FDColor getEndColor() {
        return this.endColor;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public List<Vec3> getOffsets() {
        return this.offsets;
    }

    public int getShapeVertices() {
        return this.shapeVertices;
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.STRIPE_PARTICLE.get();
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "zhopa";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private StripeParticleOptions stripeParticleOptions = new StripeParticleOptions();

        public Builder shapeVertices(int shapeVertices) {
            this.stripeParticleOptions.shapeVertices = shapeVertices;
            return this;
        }

        public Builder stripePercentLength(float stripePercentLength) {
            this.stripeParticleOptions.stripePercentLength = stripePercentLength;
            return this;
        }

        public Builder startColor(FDColor color) {
            this.stripeParticleOptions.startColor = color;
            return this;
        }

        public Builder endColor(FDColor color) {
            this.stripeParticleOptions.endColor = color;
            return this;
        }

        public Builder lifetime(int lifetime) {
            this.stripeParticleOptions.lifetime = lifetime;
            return this;
        }

        public Builder offsets(List<Vec3> offsets) {
            this.stripeParticleOptions.offsets = offsets;
            return this;
        }

        public Builder offsets(Vec3 ... offsets) {
            this.stripeParticleOptions.offsets = List.of(offsets);
            return this;
        }

        public Builder scale(float scale) {
            this.stripeParticleOptions.scale = scale;
            return this;
        }

        public Builder lod(int lod) {
            this.stripeParticleOptions.lod = lod;
            return this;
        }

        public Builder startInPercent(float startPercent) {
            this.stripeParticleOptions.startInPercent = startPercent;
            return this;
        }

        public Builder endOutPercent(float endPercent) {
            this.stripeParticleOptions.endOutPercent = endPercent;
            return this;
        }

        public StripeParticleOptions build() {
            return this.stripeParticleOptions;
        }
    }
}

