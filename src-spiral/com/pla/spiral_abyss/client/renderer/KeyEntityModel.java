/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.model.EntityModel
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.model.geom.PartPose
 *  net.minecraft.client.model.geom.builders.CubeDeformation
 *  net.minecraft.client.model.geom.builders.CubeListBuilder
 *  net.minecraft.client.model.geom.builders.LayerDefinition
 *  net.minecraft.client.model.geom.builders.MeshDefinition
 *  net.minecraft.client.model.geom.builders.PartDefinition
 *  net.minecraft.world.entity.Entity
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class KeyEntityModel<T extends Entity>
extends EntityModel<T> {
    private final ModelPart key_head_perfect_symmetric_3d;
    private final ModelPart stick;

    public KeyEntityModel(ModelPart root) {
        this.key_head_perfect_symmetric_3d = root.m_171324_("key_head_perfect_symmetric_3d");
        this.stick = root.m_171324_("stick");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.m_171576_();
        PartDefinition key_head_perfect_symmetric_3d = partdefinition.m_171599_("key_head_perfect_symmetric_3d", CubeListBuilder.m_171558_().m_171514_(2, 84).m_171488_(13.0f, -50.0f, -1.0f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(10, 84).m_171488_(-14.0f, -50.0f, -1.0f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(18, 84).m_171488_(13.0f, -49.0f, -1.0f, 3.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(28, 84).m_171488_(-15.0f, -49.0f, -1.0f, 3.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(38, 84).m_171488_(13.0f, -48.0f, -1.0f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(50, 84).m_171488_(-16.0f, -48.0f, -1.0f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(62, 84).m_171488_(13.0f, -46.0f, -1.0f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(78, 84).m_171488_(-18.0f, -46.0f, -1.0f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(94, 84).m_171488_(16.0f, -45.0f, -1.0f, 3.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(104, 84).m_171488_(13.0f, -45.0f, -1.0f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(112, 84).m_171488_(-14.0f, -45.0f, -1.0f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(120, 84).m_171488_(-18.0f, -45.0f, -1.0f, 3.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(130, 84).m_171488_(12.0f, -44.0f, -1.0f, 7.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(148, 84).m_171488_(-18.0f, -44.0f, -1.0f, 7.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(166, 84).m_171488_(10.0f, -42.0f, -1.0f, 9.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(188, 84).m_171488_(-18.0f, -42.0f, -1.0f, 9.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(210, 84).m_171488_(8.0f, -40.0f, -1.0f, 11.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(2, 89).m_171488_(-18.0f, -40.0f, -1.0f, 11.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(28, 89).m_171488_(7.0f, -39.0f, -1.0f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(56, 89).m_171488_(-18.0f, -39.0f, -1.0f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(84, 89).m_171488_(6.0f, -38.0f, -1.0f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(112, 89).m_171488_(-17.0f, -38.0f, -1.0f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(140, 89).m_171488_(5.0f, -37.0f, -1.0f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(168, 89).m_171488_(-16.0f, -37.0f, -1.0f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(196, 89).m_171488_(3.0f, -36.0f, -1.0f, 13.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(226, 89).m_171488_(-15.0f, -36.0f, -1.0f, 13.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(2, 93).m_171488_(-14.0f, -35.0f, -1.0f, 29.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(64, 93).m_171488_(-13.0f, -34.0f, -1.0f, 27.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(122, 93).m_171488_(-12.0f, -33.0f, -1.0f, 25.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(176, 93).m_171488_(-11.0f, -32.0f, -1.0f, 23.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(2, 97).m_171488_(-9.0f, -31.0f, -1.0f, 19.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(44, 97).m_171488_(-5.0f, -30.0f, -1.0f, 11.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(70, 97).m_171488_(-3.0f, -29.0f, -1.0f, 7.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).m_171514_(52, 64).m_171488_(-14.0f, -11.0f, -1.0f, 13.0f, 15.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.m_171419_((float)0.0f, (float)12.0f, (float)0.0f));
        PartDefinition stick = partdefinition.m_171599_("stick", CubeListBuilder.m_171558_().m_171514_(0, 41).m_171488_(-1.0f, -23.0f, -2.0f, 3.0f, 35.0f, 3.0f, new CubeDeformation(0.0f)).m_171514_(2, 34).m_171488_(-1.0f, -28.0f, -1.0f, 3.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.m_171419_((float)0.0f, (float)12.0f, (float)0.0f));
        return LayerDefinition.m_171565_((MeshDefinition)meshdefinition, (int)256, (int)256);
    }

    public void m_6973_(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void m_7695_(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.key_head_perfect_symmetric_3d.m_104306_(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.stick.m_104306_(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

