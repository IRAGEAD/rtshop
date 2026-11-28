/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.post_shaders.FDPostShaderInitializeEvent
 *  com.finderfeed.fdlib.systems.post_shaders.FDRenderPostShaderEvent$Level
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.EffectInstance
 *  net.minecraft.client.renderer.PostChain
 *  net.minecraft.client.renderer.PostPass
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.ClientPlayerNetworkEvent$LoggingOut
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.geburah.distortion_sphere;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.distortion_sphere.DistortionSphereEffect;
import com.finderfeed.fdlib.systems.post_shaders.FDPostShaderInitializeEvent;
import com.finderfeed.fdlib.systems.post_shaders.FDRenderPostShaderEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid="fdbosses", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class DistortionSphereEffectHandler {
    private static PostChain sphericalDistortionEffect;
    private static DistortionSphereEffect currentEffect;

    public static void setDistortionSphereEffect(DistortionSphereEffect effect) {
        currentEffect = effect;
    }

    public static DistortionSphereEffect getCurrentEffect() {
        return currentEffect;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        currentEffect = null;
    }

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (currentEffect != null && !Minecraft.m_91087_().m_91104_() && currentEffect.tick()) {
            currentEffect = null;
        }
    }

    @SubscribeEvent
    public static void registerPostShader(FDPostShaderInitializeEvent event) {
        event.registerPostChain((textureManager, resourceProvider, renderTarget) -> {
            try {
                return new PostChain(textureManager, resourceProvider, renderTarget, FDBosses.location("shaders/post/spherical_distortion_field.json"));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, postChain -> {
            sphericalDistortionEffect = postChain;
        });
    }

    @SubscribeEvent
    public static void renderLevelStageEvent(RenderLevelStageEvent event) {
        if (sphericalDistortionEffect == null) {
            return;
        }
        RenderLevelStageEvent.Stage stage = event.getStage();
        if (stage != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        if (currentEffect == null) {
            return;
        }
        Camera camera = Minecraft.m_91087_().f_91063_.m_109153_();
        Vec3 camPos = camera.m_90583_();
        Vec3 spherePos = DistortionSphereEffectHandler.currentEffect.position;
        Vector3f relativeSpherePos = spherePos.m_82546_(camPos).m_252839_();
        Matrix4f mat = new Matrix4f((Matrix4fc)RenderSystem.getModelViewMatrix());
        Matrix4f projection = new Matrix4f((Matrix4fc)event.getProjectionMatrix());
        float pticks = event.getPartialTick();
        float radius = currentEffect.getSphereRadius(pticks);
        float innerRadius = currentEffect.getInnerSphereRadius(pticks);
        float strength = currentEffect.getEffectStrength(pticks);
        float y = DistortionSphereEffectHandler.currentEffect.yFloorPos;
        float floorOffset = y - (float)spherePos.f_82480_;
        Matrix4f invertedProjection = projection.invert(new Matrix4f());
        Matrix4f invertedModelview = mat.invert(new Matrix4f());
        for (PostPass pass : DistortionSphereEffectHandler.sphericalDistortionEffect.f_110009_) {
            EffectInstance effect = pass.m_110074_();
            effect.m_108960_("inverseProjection").m_5679_(invertedProjection);
            effect.m_108960_("inverseModelview").m_5679_(invertedModelview);
            effect.m_108960_("sphereRelativePosition").m_142276_(relativeSpherePos);
            effect.m_108960_("sphereRadius").m_5985_(radius);
            effect.m_108960_("innerSphereRadius").m_5985_(innerRadius);
            effect.m_108960_("floorOffset").m_5985_(floorOffset);
            effect.m_108960_("effectStrength").m_5985_(strength * 2.0f);
        }
    }

    @SubscribeEvent
    public static void renderShader(FDRenderPostShaderEvent.Level event) {
        if (sphericalDistortionEffect == null) {
            return;
        }
        if (currentEffect != null) {
            Vec3 position = DistortionSphereEffectHandler.currentEffect.position;
            Camera camera = Minecraft.m_91087_().f_91063_.m_109153_();
            Vec3 camPos = camera.m_90583_();
            float renderDistance = (Integer)Minecraft.m_91087_().f_91066_.m_231984_().m_231551_() * 16;
            if (camPos.m_82554_(position) < (double)renderDistance) {
                event.doDefaultShaderBeforeShaderStuff();
                sphericalDistortionEffect.m_110023_(event.getPartialTicks());
            }
        }
    }
}

