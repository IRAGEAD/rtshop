/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.MobRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Mob
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.client.renderer.KeyEntityModel;
import com.pla.spiral_abyss.client.renderer.ModModelLayers;
import com.pla.spiral_abyss.entity.KeyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class KeyEntityRenderer
extends MobRenderer<KeyEntity, KeyEntityModel<KeyEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("spiral_abyss", "textures/entity/key_entity.png");
    private static final ResourceLocation CUBE_TEXTURE = new ResourceLocation("spiral_abyss", "textures/block/spinning_block.png");
    private static final float HALF_CUBE = 0.25f;
    private static final float[][][] CUBE_FACES = new float[][][]{new float[][]{{-0.25f, -0.25f, 0.25f}, {0.25f, -0.25f, 0.25f}, {0.25f, 0.25f, 0.25f}, {-0.25f, 0.25f, 0.25f}}, new float[][]{{0.25f, -0.25f, -0.25f}, {-0.25f, -0.25f, -0.25f}, {-0.25f, 0.25f, -0.25f}, {0.25f, 0.25f, -0.25f}}, new float[][]{{-0.25f, -0.25f, -0.25f}, {-0.25f, -0.25f, 0.25f}, {-0.25f, 0.25f, 0.25f}, {-0.25f, 0.25f, -0.25f}}, new float[][]{{0.25f, -0.25f, 0.25f}, {0.25f, -0.25f, -0.25f}, {0.25f, 0.25f, -0.25f}, {0.25f, 0.25f, 0.25f}}, new float[][]{{-0.25f, 0.25f, 0.25f}, {0.25f, 0.25f, 0.25f}, {0.25f, 0.25f, -0.25f}, {-0.25f, 0.25f, -0.25f}}, new float[][]{{-0.25f, -0.25f, -0.25f}, {0.25f, -0.25f, -0.25f}, {0.25f, -0.25f, 0.25f}, {-0.25f, -0.25f, 0.25f}}};
    private static final float[][] FACE_NORMALS = new float[][]{{0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, -1.0f}, {-1.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.0f}, {0.0f, 1.0f, 0.0f}, {0.0f, -1.0f, 0.0f}};
    private static final float[] FACE_U = new float[]{0.0f, 1.0f, 1.0f, 0.0f};
    private static final float[] FACE_V = new float[]{1.0f, 1.0f, 0.0f, 0.0f};

    public KeyEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new KeyEntityModel(context.m_174023_(ModModelLayers.KEY_ENTITY_LAYER)), 0.0f);
    }

    public ResourceLocation getTextureLocation(KeyEntity entity) {
        return TEXTURE;
    }

    protected int getBlockLightLevel(KeyEntity entity, BlockPos pos) {
        return 15;
    }

    public void render(KeyEntity entity, float yaw, float partialTicks, PoseStack poses, MultiBufferSource buffer, int light) {
        super.m_7392_((Mob)entity, yaw, partialTicks, poses, buffer, light);
        double time = (float)entity.m_9236_().m_46467_() + partialTicks;
        poses.m_85836_();
        poses.m_85837_(0.0, 3.5 + Math.sin(time / 10.0) / 20.0, 0.0);
        poses.m_252781_(Axis.f_252403_.m_252977_(45.0f));
        poses.m_252781_(Axis.f_252529_.m_252977_((float)(time * 4.0 % 360.0)));
        KeyEntityRenderer.renderCube(poses, buffer);
        poses.m_85849_();
    }

    private static void renderCube(PoseStack poses, MultiBufferSource buffer) {
        VertexConsumer vertices = buffer.m_6299_(RenderType.m_110458_((ResourceLocation)CUBE_TEXTURE));
        PoseStack.Pose pose = poses.m_85850_();
        for (int face = 0; face < CUBE_FACES.length; ++face) {
            float[] normal = FACE_NORMALS[face];
            for (int corner = 0; corner < 4; ++corner) {
                float[] point = CUBE_FACES[face][corner];
                vertices.m_252986_(pose.m_252922_(), point[0], point[1], point[2]).m_6122_(255, 255, 255, 255).m_7421_(FACE_U[corner], FACE_V[corner]).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(pose.m_252943_(), normal[0], normal[1], normal[2]).m_5752_();
            }
        }
    }
}

