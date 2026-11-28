/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.resources.ResourceLocation
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.particles.DecalParticle;
import com.finderfeed.fdbosses.client.particles.DecalParticleOptions;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class GeburahRayStrikeDecalParticle
extends DecalParticle {
    public static final ResourceLocation LOCATION = FDBosses.location("textures/particle/geburah/ray_strike_decal.png");
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(){

        public void m_6505_(BufferBuilder tesselator, TextureManager p_107437_) {
            if (Minecraft.m_91085_()) {
                Minecraft.m_91087_().m_91385_().m_83947_(false);
            }
            RenderSystem.enableBlend();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::m_172829_);
            FDRenderUtil.bindTexture((ResourceLocation)LOCATION);
            tesselator.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85813_);
        }

        public void m_6294_(Tesselator p_107438_) {
            p_107438_.m_85914_();
            if (Minecraft.m_91085_()) {
                Minecraft.m_91087_().f_91060_.m_109830_().m_83945_(Minecraft.m_91087_().m_91385_());
                Minecraft.m_91087_().f_91060_.m_109830_().m_83947_(false);
            }
            RenderSystem.disableBlend();
        }
    };

    public GeburahRayStrikeDecalParticle(DecalParticleOptions decalParticleOptions, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(decalParticleOptions, level, x, y, z, xd, yd, zd);
        this.f_107204_ = this.f_107231_ = (float)(level.f_46441_.m_188503_(4) * 90);
    }

    protected int m_6355_(float p_107249_) {
        return 0xF000F0;
    }

    public ParticleRenderType m_7556_() {
        return RENDER_TYPE;
    }

    public static class Factory
    implements ParticleProvider<DecalParticleOptions> {
        @Nullable
        public Particle createParticle(DecalParticleOptions decalParticleOptions, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new GeburahRayStrikeDecalParticle(decalParticleOptions, level, x, y, z, xd, yd, zd);
        }
    }
}

