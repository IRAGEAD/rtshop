/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.DimensionSpecialEffects
 *  net.minecraft.client.renderer.DimensionSpecialEffects$SkyType
 *  net.minecraft.client.renderer.FogRenderer
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.material.FogType
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class AbyssSkyEffects
extends DimensionSpecialEffects {
    public static final ResourceLocation ID = new ResourceLocation("spiral_abyss", "spiral_abyss");
    private static final ResourceLocation TEXTURE = new ResourceLocation("spiral_abyss", "textures/environment/galaxy_blue.png");
    private static final float[][][] FACES = new float[][][]{new float[][]{{-1.0f, -1.0f, -1.0f}, {-1.0f, -1.0f, 1.0f}, {1.0f, -1.0f, 1.0f}, {1.0f, -1.0f, -1.0f}}, new float[][]{{-1.0f, 1.0f, 1.0f}, {-1.0f, 1.0f, -1.0f}, {1.0f, 1.0f, -1.0f}, {1.0f, 1.0f, 1.0f}}, new float[][]{{1.0f, 1.0f, 1.0f}, {1.0f, -1.0f, 1.0f}, {-1.0f, -1.0f, 1.0f}, {-1.0f, 1.0f, 1.0f}}, new float[][]{{-1.0f, 1.0f, 1.0f}, {-1.0f, -1.0f, 1.0f}, {-1.0f, -1.0f, -1.0f}, {-1.0f, 1.0f, -1.0f}}, new float[][]{{-1.0f, 1.0f, -1.0f}, {-1.0f, -1.0f, -1.0f}, {1.0f, -1.0f, -1.0f}, {1.0f, 1.0f, -1.0f}}, new float[][]{{1.0f, 1.0f, -1.0f}, {1.0f, -1.0f, -1.0f}, {1.0f, -1.0f, 1.0f}, {1.0f, 1.0f, 1.0f}}};

    public AbyssSkyEffects() {
        super(Float.NaN, false, DimensionSpecialEffects.SkyType.NONE, true, false);
    }

    public Vec3 m_5927_(Vec3 color, float brightness) {
        return new Vec3(0.025, 0.045, 0.1);
    }

    public boolean m_5781_(int x, int z) {
        return false;
    }

    public float[] m_7518_(float time, float partialTick) {
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack pose, Camera camera, Matrix4f projection, boolean foggy, Runnable setupFog) {
        LivingEntity living;
        Entity entity;
        setupFog.run();
        if (foggy || camera.m_167685_() != FogType.NONE || (entity = camera.m_90592_()) instanceof LivingEntity && ((living = (LivingEntity)entity).m_21023_(MobEffects.f_19610_) || living.m_21023_(MobEffects.f_216964_))) {
            return true;
        }
        pose.m_85836_();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableBlend();
        FogRenderer.m_109017_();
        try {
            pose.m_252781_(Axis.f_252436_.m_252977_(((float)(level.m_46467_() % 24000L) + partialTick) * 360.0f / 24000.0f));
            RenderSystem.setShader(GameRenderer::m_172820_);
            RenderSystem.setShaderTexture((int)0, (ResourceLocation)TEXTURE);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            BufferBuilder buffer = Tesselator.m_85913_().m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85819_);
            Matrix4f matrix = pose.m_85850_().m_252922_();
            for (int face = 0; face < FACES.length; ++face) {
                float u0 = (float)(face % 3) / 3.0f;
                float u1 = u0 + 0.33333334f;
                float v0 = (float)(face / 3) / 2.0f;
                float v1 = v0 + 0.5f;
                for (int corner = 0; corner < 4; ++corner) {
                    float[] xyz = FACES[face][corner];
                    buffer.m_252986_(matrix, xyz[0] * 100.0f, xyz[1] * 100.0f, xyz[2] * 100.0f).m_7421_(corner < 2 ? u0 : u1, corner == 0 || corner == 3 ? v0 : v1).m_6122_(255, 255, 255, 255).m_5752_();
                }
            }
            BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        }
        finally {
            pose.m_85849_();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            setupFog.run();
        }
        return true;
    }

    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack pose, double x, double y, double z, Matrix4f projection) {
        return true;
    }

    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture light, double x, double y, double z) {
        return true;
    }

    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }
}

