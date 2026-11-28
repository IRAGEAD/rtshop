/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$DestFactor
 *  com.mojang.blaze3d.platform.GlStateManager$SourceFactor
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.VertexFormat
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.Util
 *  net.minecraft.client.renderer.RenderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard
 *  net.minecraft.client.renderer.RenderStateShard$TextureStateShard
 *  net.minecraft.client.renderer.RenderStateShard$TransparencyStateShard
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.RenderType$CompositeState
 *  net.minecraft.resources.ResourceLocation
 */
package com.finderfeed.fdbosses.client.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class BossRenderTypes
extends RenderType {
    public static final RenderType LIGHTNING_NO_CULL = BossRenderTypes.m_173215_((String)"lightning_no_cull", (VertexFormat)DefaultVertexFormat.f_85815_, (VertexFormat.Mode)VertexFormat.Mode.QUADS, (int)256, (boolean)false, (boolean)true, (RenderType.CompositeState)RenderType.CompositeState.m_110628_().m_173292_(f_173091_).m_110687_(f_110114_).m_110685_(f_110136_).m_110675_(f_110127_).m_110661_(RenderStateShard.f_110110_).m_110691_(false));
    public static final RenderStateShard.TransparencyStateShard FD_ADDITIVE_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("fd_additive_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    public static Function<ResourceLocation, RenderType> TEXT_ADDITIVE = Util.m_143827_(BossRenderTypes::textAdditive);

    public BossRenderTypes(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
        super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
    }

    public static RenderType textAdditive(ResourceLocation locationIn) {
        RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_().m_173292_(RenderType.f_173086_).m_173290_((RenderStateShard.EmptyTextureStateShard)new RenderStateShard.TextureStateShard(locationIn, false, false)).m_110685_(FD_ADDITIVE_TRANSPARENCY).m_110671_(RenderType.f_110152_).m_110691_(false);
        return RenderType.m_173215_((String)"neoforge_text", (VertexFormat)DefaultVertexFormat.f_85820_, (VertexFormat.Mode)VertexFormat.Mode.QUADS, (int)256, (boolean)false, (boolean)true, (RenderType.CompositeState)rendertype$state);
    }
}

