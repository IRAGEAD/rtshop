package com.rtshop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rtshop.story.CompanionEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;

/** 동행 NPC: 주민의 모습(직업 옷 포함)으로 그린다. 쓰러졌을 때는 몸을 숙인다. */
public class CompanionRenderer extends MobRenderer<CompanionEntity, VillagerModel<CompanionEntity>> {
    private static final ResourceLocation TEX = new ResourceLocation("textures/entity/villager/villager.png");

    public CompanionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerModel<>(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        addLayer(new VillagerProfessionLayer<>(this, ctx.getResourceManager(), "villager"));
    }

    @Override
    public ResourceLocation getTextureLocation(CompanionEntity e) {
        return TEX;
    }

    @Override
    protected void setupRotations(CompanionEntity e, PoseStack ps, float age, float yaw, float partial) {
        super.setupRotations(e, ps, age, yaw, partial);
        if (e.isDowned()) {
            ps.translate(0, -0.25, 0);
            ps.mulPose(Axis.XP.rotationDegrees(28F));
        }
    }

    @Override
    protected void scale(CompanionEntity e, PoseStack ps, float partial) {
        ps.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
