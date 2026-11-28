/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.EntityModel
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.MobRenderer
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.client.renderer.ChestInteriorGlow;
import com.pla.spiral_abyss.client.renderer.CopperChestModel;
import com.pla.spiral_abyss.client.renderer.GoldenChestModel;
import com.pla.spiral_abyss.client.renderer.IronChestModel;
import com.pla.spiral_abyss.client.renderer.LootChestModel;
import com.pla.spiral_abyss.client.renderer.LootGlow;
import com.pla.spiral_abyss.entity.LootChestEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class LootChestRenderer
extends MobRenderer<LootChestEntity, LootChestModel> {
    private final ResourceLocation texture;

    private LootChestRenderer(EntityRendererProvider.Context context, LootChestModel model, String kind) {
        this(context, model, new ResourceLocation("spiral_abyss", "textures/entity/" + kind + "_chest.png"));
    }

    protected LootChestRenderer(EntityRendererProvider.Context context, LootChestModel model, ResourceLocation texture) {
        super(context, (EntityModel)model, 0.6f);
        this.texture = texture;
    }

    public static LootChestRenderer copper(EntityRendererProvider.Context context) {
        return new LootChestRenderer(context, (LootChestModel)new CopperChestModel(context.m_174023_(CopperChestModel.LAYER_LOCATION)), "copper");
    }

    public static LootChestRenderer iron(EntityRendererProvider.Context context) {
        return new LootChestRenderer(context, (LootChestModel)new IronChestModel(context.m_174023_(IronChestModel.LAYER_LOCATION)), "iron");
    }

    public static LootChestRenderer golden(EntityRendererProvider.Context context) {
        return new LootChestRenderer(context, (LootChestModel)new GoldenChestModel(context.m_174023_(GoldenChestModel.LAYER_LOCATION)), "golden");
    }

    public ResourceLocation getTextureLocation(LootChestEntity entity) {
        return this.texture;
    }

    public void render(LootChestEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.m_7392_((Mob)entity, yaw, partialTick, pose, buffers, light);
        if (entity.phase() != 2) {
            return;
        }
        VertexConsumer vertices = buffers.m_6299_(LootGlow.TYPE);
        int color = entity.rarePayout() ? 16764226 : 0xCCDFFF;
        Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_().m_82546_(entity.m_20182_());
        float time = (float)entity.f_19797_ + partialTick;
        float fade = Mth.m_14036_((float)(((float)(entity.m_9236_().m_46467_() - entity.phaseStart()) + partialTick) / 4.0f), (float)0.0f, (float)1.0f);
        float rotation = 180.0f - Mth.m_14189_((float)partialTick, (float)entity.f_20884_, (float)entity.f_20883_);
        pose.m_85836_();
        pose.m_252781_(Axis.f_252436_.m_252977_(rotation));
        camera = camera.m_82524_((float)Math.toRadians(-rotation));
        int seed = entity.m_20148_().hashCode() & 0x3FF;
        ChestInteriorGlow.render(pose, vertices, time, seed, color, fade);
        for (int ray = 0; ray < 16; ++ray) {
            double phase = (double)seed + (double)ray * 2.39996;
            double x = Math.sin(phase * 3.1 + (double)time * 0.018) * 0.48;
            double z = Math.sin(phase * 5.7 + (double)time * 0.023) * 0.33;
            float pulse = 0.55f + 0.45f * (float)Math.sin((double)time * 0.09 + phase);
            Vec3 from = new Vec3(x, 0.57, z);
            Vec3 to = new Vec3(x * 1.7 + Math.sin(phase) * 0.15, 1.45 + 0.8 * (double)pulse, z * 1.7 + Math.cos(phase) * 0.15);
            LootGlow.ribbon(pose, vertices, from, to, camera, 0.15f, 0.3f, color, (0.16f + 0.18f * pulse) * fade, 0.0f);
            LootGlow.ribbon(pose, vertices, from, from.m_165921_(to, 0.65), camera, 0.045f, 0.09f, 0xFFFFFF, 0.28f * pulse * fade, 0.0f);
        }
        for (int glint = 0; glint < 8; ++glint) {
            float progress = (time * 0.017f + (float)glint / 8.0f) % 1.0f;
            double angle = (double)glint * 2.4 + (double)time * 0.01;
            Vec3 point = new Vec3(Math.cos(angle) * (double)progress * 0.7, 0.55 + (double)progress * 1.6, Math.sin(angle) * (double)progress * 0.7);
            float alpha = (float)Math.sin((double)progress * Math.PI) * fade;
            LootGlow.ribbon(pose, vertices, point, point.m_82520_(0.0, 0.09, 0.0), camera, 0.035f, 0.0f, color, alpha, 0.0f);
        }
        pose.m_85849_();
    }
}

