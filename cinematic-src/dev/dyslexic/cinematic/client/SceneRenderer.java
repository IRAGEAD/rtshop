/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.EnchantRenderer;
import dev.dyslexic.cinematic.client.FrameProbe;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.FurnaceRenderer;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.MenuRenderer;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.client.Showcase;
import dev.dyslexic.cinematic.client.TableRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public final class SceneRenderer {
    private SceneRenderer() {
    }

    private static void outlineNeighbor(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ClientLevel level) {
        if (Scene.neighbor == null) {
            return;
        }
        VoxelShape shape = level.m_8055_(Scene.neighbor).m_60808_((BlockGetter)level, Scene.neighbor);
        if (shape.m_83281_()) {
            return;
        }
        AABB box = shape.m_83215_().m_82338_(Scene.neighbor).m_82400_(0.004).m_82386_(-cam.f_82479_, -cam.f_82480_, -cam.f_82481_);
        LevelRenderer.m_109646_((PoseStack)pose, (VertexConsumer)buffers.m_6299_(RenderType.m_110504_()), (AABB)box, (float)1.0f, (float)0.88f, (float)0.6f, (float)0.75f);
    }

    public static void render(RenderLevelStageEvent event) {
        boolean stationPresent;
        Minecraft mc = Minecraft.m_91087_();
        ClientLevel level = mc.f_91073_;
        if (level == null) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cam = camera.m_90583_();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.m_91269_().m_110104_();
        boolean bl = stationPresent = Scene.station == null || Scene.station.stillThere((Level)level);
        if (stationPresent) {
            Scene.updateCursor();
        }
        Hands.update(cam, camera.m_90590_());
        TableRenderer.renderAll(pose, (MultiBufferSource)buffers, cam, event.getFrustum(), level);
        FurnaceRenderer.renderAll(pose, (MultiBufferSource)buffers, cam, event.getFrustum(), level);
        EnchantRenderer.renderAll(pose, (MultiBufferSource)buffers, camera, event.getFrustum(), level);
        MenuRenderer.renderAll(pose, (MultiBufferSource)buffers, camera, event.getFrustum(), level);
        ShelfBooks.renderAll(pose, (MultiBufferSource)buffers, camera, event.getFrustum(), level);
        Fx.renderSparks(pose, (MultiBufferSource)buffers, camera, Scene.frameDt);
        ClientStation station = Scene.station;
        if (Scene.active() && station != null && stationPresent) {
            int light = LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)station.lightPos());
            if (Scene.phase == Scene.Phase.ACTIVE) {
                station.renderWorld(pose, (MultiBufferSource)buffers, cam, light);
                SceneRenderer.outlineNeighbor(pose, (MultiBufferSource)buffers, cam, level);
            }
            if (!FreeLook.showsBody()) {
                station.renderBody(pose, (MultiBufferSource)buffers, cam, light);
                Hands.render(pose, (MultiBufferSource)buffers, cam, camera.m_90590_(), light);
            }
        }
        buffers.m_109911_();
        if (Showcase.ENABLED) {
            FrameProbe.sample(camera);
        }
    }
}

