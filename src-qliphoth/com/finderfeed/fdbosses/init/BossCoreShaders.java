/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterShadersEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.FDBosses;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class BossCoreShaders {
    public static ShaderInstance MALKUTH_BOSS_BAR;
    public static ShaderInstance PARTICLE_NO_DISCARD;
    public static ShaderInstance CHESED_ITEM_OVERLAY;
    public static ShaderInstance CODEX_UI;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), FDBosses.location("malkuth_boss_bar"), DefaultVertexFormat.f_85819_), inst -> {
            MALKUTH_BOSS_BAR = inst;
        });
        event.registerShader(new ShaderInstance(event.getResourceProvider(), FDBosses.location("particle_no_discard"), DefaultVertexFormat.f_85813_), shaderInstance -> {
            PARTICLE_NO_DISCARD = shaderInstance;
        });
        event.registerShader(new ShaderInstance(event.getResourceProvider(), FDBosses.location("chesed_item_overlay"), DefaultVertexFormat.f_85819_), shaderInstance -> {
            CHESED_ITEM_OVERLAY = shaderInstance;
        });
        event.registerShader(new ShaderInstance(event.getResourceProvider(), FDBosses.location("boss_codex_ui"), DefaultVertexFormat.f_85819_), shaderInstance -> {
            CODEX_UI = shaderInstance;
        });
    }
}

