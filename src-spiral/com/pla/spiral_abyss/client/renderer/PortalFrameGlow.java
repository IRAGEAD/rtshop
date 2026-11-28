/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

final class PortalFrameGlow {
    private static final Map<BakedModel, Map<BlockState, List<Face>>> CACHE = new WeakHashMap<BakedModel, Map<BlockState, List<Face>>>();
    private static final Map<ResourceLocation, Skin> SKINS = Map.of(PortalFrameGlow.id("block/abyss_gate_1"), PortalFrameGlow.skin("abyss_gate_1"));

    private PortalFrameGlow() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("spiral_abyss", path);
    }

    private static Skin skin(String name) {
        return new Skin(PortalFrameGlow.id("textures/block/glow/" + name + "_emissive.png"), PortalFrameGlow.id("textures/block/glow/" + name + "_halo.png"));
    }

    static void render(AbyssPortalBlockEntity portal, PoseStack pose, MultiBufferSource buffers) {
        Level level = portal.m_58904_();
        AbyssPortalFrame frame = portal.frame();
        if (level == null || !frame.active(level)) {
            return;
        }
        for (int y = 0; y <= 5; ++y) {
            for (int across = -2; across <= 2; ++across) {
                if (frame.expectedFrameState(across, y) == null) continue;
                BlockPos pos = frame.at(across, y);
                BlockState state = level.m_8055_(pos);
                BakedModel model = Minecraft.m_91087_().m_91289_().m_110910_(state);
                List faces = CACHE.computeIfAbsent(model, ignored -> new HashMap()).computeIfAbsent(state, ignored -> PortalFrameGlow.bake(model, state));
                pose.m_85836_();
                BlockPos offset = pos.m_121996_((Vec3i)frame.base());
                pose.m_252880_((float)offset.m_123341_(), (float)offset.m_123342_(), (float)offset.m_123343_());
                for (Face face : faces) {
                    PortalFrameGlow.draw(pose, buffers.m_6299_(RenderType.m_234335_((ResourceLocation)face.skin.core, (boolean)false)), face, 0.002f);
                    PortalFrameGlow.draw(pose, buffers.m_6299_(RenderType.m_234335_((ResourceLocation)face.skin.halo, (boolean)false)), face, 0.003f);
                }
                pose.m_85849_();
            }
        }
    }

    private static List<Face> bake(BakedModel model, BlockState state) {
        ArrayList<Face> result = new ArrayList<Face>();
        PortalFrameGlow.append(result, model.m_213637_(state, null, RandomSource.m_216335_((long)0L)));
        for (Direction side : Direction.values()) {
            PortalFrameGlow.append(result, model.m_213637_(state, side, RandomSource.m_216335_((long)0L)));
        }
        return List.copyOf(result);
    }

    private static void append(List<Face> result, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.m_173410_();
            Skin skin = SKINS.get(sprite.m_245424_().m_246162_());
            if (skin == null) continue;
            int[] data = quad.m_111303_();
            int stride = data.length / 4;
            float[] vertices = new float[20];
            for (int v = 0; v < 4; ++v) {
                int source = v * stride;
                int target = v * 5;
                vertices[target] = Float.intBitsToFloat(data[source]);
                vertices[target + 1] = Float.intBitsToFloat(data[source + 1]);
                vertices[target + 2] = Float.intBitsToFloat(data[source + 2]);
                vertices[target + 3] = (Float.intBitsToFloat(data[source + 4]) - sprite.m_118409_()) / (sprite.m_118410_() - sprite.m_118409_());
                vertices[target + 4] = (Float.intBitsToFloat(data[source + 5]) - sprite.m_118411_()) / (sprite.m_118412_() - sprite.m_118411_());
            }
            Vector3f edgeA = new Vector3f(vertices[5] - vertices[0], vertices[6] - vertices[1], vertices[7] - vertices[2]);
            Vector3f edgeB = new Vector3f(vertices[10] - vertices[0], vertices[11] - vertices[1], vertices[12] - vertices[2]);
            Vector3f normal = edgeA.cross((Vector3fc)edgeB);
            if (normal.lengthSquared() < 1.0E-8f) continue;
            result.add(new Face(vertices, normal.normalize(), skin));
        }
    }

    private static void draw(PoseStack pose, VertexConsumer out, Face face, float offset) {
        for (int v = 0; v < 4; ++v) {
            int i = v * 5;
            out.m_252986_(pose.m_85850_().m_252922_(), face.vertices[i] + face.normal.x * offset, face.vertices[i + 1] + face.normal.y * offset, face.vertices[i + 2] + face.normal.z * offset).m_6122_(255, 255, 255, 255).m_7421_(face.vertices[i + 3], face.vertices[i + 4]).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(pose.m_85850_().m_252943_(), face.normal.x, face.normal.y, face.normal.z).m_5752_();
        }
    }

    private record Skin(ResourceLocation core, ResourceLocation halo) {
    }

    private record Face(float[] vertices, Vector3f normal, Skin skin) {
    }
}

