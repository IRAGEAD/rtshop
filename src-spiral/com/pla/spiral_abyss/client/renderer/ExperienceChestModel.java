/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.geom.ModelLayerLocation
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.model.geom.PartPose
 *  net.minecraft.client.model.geom.builders.CubeDeformation
 *  net.minecraft.client.model.geom.builders.CubeListBuilder
 *  net.minecraft.client.model.geom.builders.LayerDefinition
 *  net.minecraft.client.model.geom.builders.MeshDefinition
 *  net.minecraft.client.model.geom.builders.PartDefinition
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.client.renderer;

import com.pla.spiral_abyss.client.renderer.GoldenChestAnimations;
import com.pla.spiral_abyss.client.renderer.LootChestModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class ExperienceChestModel
extends LootChestModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("spiral_abyss", "experience_chest"), "main");

    public ExperienceChestModel(ModelPart root) {
        super(root, GoldenChestAnimations.spawn, GoldenChestAnimations.idle, GoldenChestAnimations.open);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.m_171576_();
        PartDefinition chest = partdefinition.m_171599_("chest", CubeListBuilder.m_171558_().m_171514_(76, 79).m_171488_(-10.0f, -3.0f, -8.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)).m_171514_(18, 98).m_171488_(-9.5f, -7.0f, -7.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).m_171514_(31, 98).m_171488_(-9.5f, -7.0f, 4.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).m_171514_(57, 98).m_171488_(6.5f, -7.0f, 4.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).m_171514_(44, 98).m_171488_(6.5f, -7.0f, -7.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).m_171514_(83, 98).m_171488_(-6.0f, -2.0f, -7.5f, 12.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(1, 106).m_171488_(-6.0f, -2.0f, 6.5f, 12.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(93, 79).m_171488_(6.0f, -3.0f, -8.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)).m_171514_(1, 22).m_171488_(-10.0f, -9.0f, -8.0f, 20.0f, 2.0f, 16.0f, new CubeDeformation(0.0f)).m_171514_(1, 98).m_171488_(6.0f, -3.0f, 4.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)).m_171514_(38, 79).m_171488_(8.5f, -2.0f, -4.0f, 1.0f, 2.0f, 8.0f, new CubeDeformation(0.0f)).m_171514_(57, 79).m_171488_(-9.5f, -2.0f, -4.0f, 1.0f, 2.0f, 8.0f, new CubeDeformation(0.0f)).m_171514_(110, 79).m_171488_(-10.0f, -3.0f, 4.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)).m_171514_(1, 1).m_171488_(-9.0f, -7.0f, -7.0f, 18.0f, 6.0f, 14.0f, new CubeDeformation(0.0f)), PartPose.m_171419_((float)0.0f, (float)24.0f, (float)0.0f));
        PartDefinition lid = chest.m_171599_("lid", CubeListBuilder.m_171558_().m_171514_(66, 60).m_171488_(-9.5f, -4.9f, -15.5f, 3.0f, 3.0f, 15.0f, new CubeDeformation(0.0f)).m_171514_(1, 79).m_171488_(6.5f, -4.9f, -15.5f, 3.0f, 3.0f, 15.0f, new CubeDeformation(0.0f)).m_171514_(1, 41).m_171488_(-10.0f, -1.9f, -16.0f, 20.0f, 2.0f, 16.0f, new CubeDeformation(0.0f)).m_171514_(1, 60).m_171488_(-9.0f, -4.4f, -15.0f, 18.0f, 4.0f, 14.0f, new CubeDeformation(0.0f)).m_171514_(70, 98).m_171488_(-2.0f, -2.4f, -16.5f, 4.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.m_171419_((float)0.0f, (float)-9.1f, (float)8.0f));
        PartDefinition tag_1 = chest.m_171599_("tag_1", CubeListBuilder.m_171558_(), PartPose.m_171419_((float)0.0f, (float)-22.0f, (float)0.0f));
        PartDefinition tag_2 = chest.m_171599_("tag_2", CubeListBuilder.m_171558_(), PartPose.m_171419_((float)0.0f, (float)-18.0f, (float)0.0f));
        PartDefinition hitbox = chest.m_171599_("hitbox", CubeListBuilder.m_171558_().m_171514_(0, 0).m_171488_(-9.0f, -14.1f, -9.0f, 18.0f, 14.0f, 18.0f, new CubeDeformation(0.0f)), PartPose.m_171419_((float)0.0f, (float)0.0f, (float)0.0f));
        return LayerDefinition.m_171565_((MeshDefinition)meshdefinition, (int)128, (int)128);
    }
}

