/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.trails.FDTrailDataGenerator
 *  com.finderfeed.fdlib.systems.trails.FDTrailRenderer
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Quaternionfc
 *  org.joml.Vector3d
 */
package com.finderfeed.fdbosses.client.particles.colored_jumping_particles;

import com.finderfeed.fdbosses.client.particles.colored_jumping_particles.ColoredJumpingParticleOptions;
import com.finderfeed.fdlib.systems.trails.FDTrailDataGenerator;
import com.finderfeed.fdlib.systems.trails.FDTrailRenderer;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector3d;

public class ColoredJumpingParticle
extends Particle {
    private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = Mth.m_144952_((double)100.0);
    private boolean stoppedByCollision;
    public ColoredJumpingParticleOptions options;
    private FDTrailDataGenerator<ColoredJumpingParticle> trail;
    public int ignoreCollisionTicks = 0;
    public boolean disappearWhenSpeedIsLow = false;
    private int currentJumpAmount = 0;
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(){

        public void m_6294_(Tesselator tesselator) {
            tesselator.m_85914_();
            RenderSystem.setShader(GameRenderer::m_172829_);
            RenderSystem.disableBlend();
        }

        @Nullable
        public void m_6505_(BufferBuilder tesselator, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.setShader(GameRenderer::m_172811_);
            tesselator.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        }
    };

    public ColoredJumpingParticle(ColoredJumpingParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.options = options;
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.f_107227_ = options.colorStart.r;
        this.f_107228_ = options.colorStart.g;
        this.f_107229_ = options.colorStart.b;
        this.f_107230_ = options.colorStart.a;
        this.f_107226_ = options.gravity;
        this.disappearWhenSpeedIsLow = options.lifetime == -1;
        this.f_107225_ = options.lifetime;
        if (this.f_107225_ == -1) {
            this.f_107225_ = Integer.MAX_VALUE;
        }
        this.trail = new FDTrailDataGenerator((coloredJumpingParticle, aFloat) -> FDMathUtil.interpolateVectors((Vec3)new Vec3(coloredJumpingParticle.f_107209_, coloredJumpingParticle.f_107210_, coloredJumpingParticle.f_107211_), (Vec3)new Vec3(coloredJumpingParticle.f_107212_, coloredJumpingParticle.f_107213_, coloredJumpingParticle.f_107214_), (float)aFloat.floatValue()), options.maxPointsInTrail, 0.1f);
        this.f_172258_ = 1.0f;
        this.m_107250_(0.05f, 0.05f);
    }

    public void m_5989_() {
        if (this.disappearWhenSpeedIsLow && this.f_107218_ && this.currentJumpAmount >= this.options.maxJumpAmount && this.trail.getPoints().size() <= 1) {
            this.m_107274_();
            return;
        }
        this.f_107209_ = this.f_107212_;
        this.f_107210_ = this.f_107213_;
        this.f_107211_ = this.f_107214_;
        if (this.f_107224_++ >= this.f_107225_) {
            this.m_107274_();
        } else {
            this.f_107216_ -= 0.04 * (double)this.f_107226_;
            this.m_6257_(this.f_107215_, this.f_107216_, this.f_107217_);
            if (this.f_172259_ && this.f_107213_ == this.f_107210_) {
                this.f_107215_ *= 1.1;
                this.f_107217_ *= 1.1;
            }
            if (this.f_107218_) {
                this.f_107215_ *= (double)0.7f;
                this.f_107217_ *= (double)0.7f;
            }
        }
        this.ignoreCollisionTicks = Mth.m_14045_((int)(this.ignoreCollisionTicks - 1), (int)0, (int)Integer.MAX_VALUE);
        this.trail.tick((Object)this);
    }

    public void m_6257_(double xd, double yd, double zd) {
        if (!this.stoppedByCollision) {
            double d1 = yd;
            if (this.f_107219_ && (xd != 0.0 || yd != 0.0 || zd != 0.0) && xd * xd + yd * yd + zd * zd < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
                Vec3 vec3 = this.handleCollision(xd, yd, zd);
                xd = vec3.f_82479_;
                yd = vec3.f_82480_;
                zd = vec3.f_82481_;
            }
            if (xd != 0.0 || yd != 0.0 || zd != 0.0) {
                this.m_107259_(this.m_107277_().m_82386_(xd, yd, zd));
                this.m_107275_();
            }
            if (Math.abs(d1) >= (double)1.0E-5f && Math.abs(yd) < (double)1.0E-5f) {
                this.stoppedByCollision = true;
            }
            this.f_107218_ = d1 != yd && d1 < 0.0;
        }
    }

    private Vec3 handleCollision(double xd, double yd, double zd) {
        if (this.ignoreCollisionTicks > 0) {
            return new Vec3(xd, yd, zd);
        }
        Vec3 oldShift = new Vec3(xd, yd, zd);
        Vec3 newShift = Entity.m_198894_(null, (Vec3)oldShift, (AABB)this.m_107277_(), (Level)this.f_107208_, List.of());
        if (!oldShift.equals((Object)newShift) && this.currentJumpAmount < this.options.maxJumpAmount) {
            float zMod;
            Vec3 thisPos = this.getPos();
            Vec3 endPos = thisPos.m_82520_(xd * 2.0, yd * 2.0, zd * 2.0);
            ClipContext clipContext = new ClipContext(thisPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
            BlockHitResult result = this.f_107208_.m_45547_(clipContext);
            Direction direction = result.m_82434_();
            Vector3d shift = new Vector3d(xd, yd, zd).reflect((double)direction.m_122429_(), (double)direction.m_122430_(), (double)direction.m_122431_());
            float xMod = direction.m_122429_() == 0 ? 1.0f : this.options.reflectionStrength;
            float yMod = direction.m_122430_() == 0 ? 1.0f : this.options.reflectionStrength;
            float f = zMod = direction.m_122431_() == 0 ? 1.0f : this.options.reflectionStrength;
            if (Direction.Plane.HORIZONTAL.test(direction)) {
                this.ignoreCollisionTicks = 0;
                if (shift.y > 0.0) {
                    shift.y = -shift.y;
                }
            }
            this.f_107215_ = shift.x * (double)xMod;
            this.f_107216_ = shift.y * (double)yMod;
            this.f_107217_ = shift.z * (double)zMod;
            ++this.currentJumpAmount;
            return new Vec3(shift.x * (double)xMod, shift.y * (double)yMod, shift.z * (double)zMod);
        }
        return newShift;
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float pticks) {
        Vec3 pos = FDMathUtil.interpolateVectors((Vec3)new Vec3(this.f_107209_, this.f_107210_, this.f_107211_), (Vec3)new Vec3(this.f_107212_, this.f_107213_, this.f_107214_), (float)pticks);
        Vec3 camPos = camera.m_90583_();
        Vec3 translatePosition = pos.m_82546_(camPos);
        Matrix4f mat = new Matrix4f();
        mat.translate((float)translatePosition.f_82479_, (float)translatePosition.f_82480_, (float)translatePosition.f_82481_);
        mat.rotate((Quaternionfc)camera.m_253121_());
        float radius = this.options.size;
        FDColor color = this.options.colorStart;
        vertex.m_252986_(mat, radius, radius, 0.0f).m_85950_(color.r, color.g, color.b, color.a).m_5752_();
        vertex.m_252986_(mat, -radius, radius, 0.0f).m_85950_(color.r, color.g, color.b, color.a).m_5752_();
        vertex.m_252986_(mat, -radius, -radius, 0.0f).m_85950_(color.r, color.g, color.b, color.a).m_5752_();
        vertex.m_252986_(mat, radius, -radius, 0.0f).m_85950_(color.r, color.g, color.b, color.a).m_5752_();
        Trails.TO_RENDER_TRAILS_ON.add(this);
    }

    public ParticleRenderType m_7556_() {
        return RENDER_TYPE;
    }

    @Mod.EventBusSubscriber(value={Dist.CLIENT}, modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE)
    public static class Trails {
        public static List<ColoredJumpingParticle> TO_RENDER_TRAILS_ON = new ArrayList<ColoredJumpingParticle>();

        @SubscribeEvent
        public static void renderTrails(RenderLevelStageEvent event) {
            RenderLevelStageEvent.Stage stage = event.getStage();
            if (stage != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                return;
            }
            if (TO_RENDER_TRAILS_ON.isEmpty()) {
                return;
            }
            Tesselator tesselator = Tesselator.m_85913_();
            BufferBuilder builder = tesselator.m_85915_();
            builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::m_172811_);
            PoseStack matrices = event.getPoseStack();
            for (ColoredJumpingParticle particle : TO_RENDER_TRAILS_ON) {
                matrices.m_85836_();
                Vec3 pos = FDMathUtil.interpolateVectors((Vec3)new Vec3(particle.f_107209_, particle.f_107210_, particle.f_107211_), (Vec3)new Vec3(particle.f_107212_, particle.f_107213_, particle.f_107214_), (float)event.getPartialTick()).m_82546_(Minecraft.m_91087_().f_91063_.m_109153_().m_90583_());
                matrices.m_85837_(pos.f_82479_, pos.f_82480_, pos.f_82481_);
                FDTrailRenderer.renderTrail((Object)((Object)particle), particle.trail, (VertexConsumer)builder, (PoseStack)matrices, (float)particle.options.size, (int)4, (int)30, (float)event.getPartialTick(), (FDColor)particle.options.colorEnd, (FDColor)particle.options.colorStart);
                matrices.m_85849_();
            }
            BufferBuilder.RenderedBuffer meshData = builder.m_231175_();
            if (meshData != null) {
                BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)meshData);
            }
            TO_RENDER_TRAILS_ON.clear();
        }
    }

    public static class Factory
    implements ParticleProvider<ColoredJumpingParticleOptions> {
        public Particle createParticle(ColoredJumpingParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            ColoredJumpingParticle particle = new ColoredJumpingParticle(options, level, x, y, z, xd, yd, zd);
            return particle;
        }
    }
}

