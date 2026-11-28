/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.BoneTransformationController
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModelPart
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.renderer.MultiBufferSource
 */
package com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons;

import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.BoneTransformationController;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.bedrock.models.FDModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public class GeburahRotatingWeaponsBoneController
implements BoneTransformationController<GeburahEntity> {
    public void transformBone(GeburahEntity geburahEntity, FDModel fdModel, FDModelPart fdModelPart, PoseStack poseStack, MultiBufferSource multiBufferSource, String s, int i, int i1, float v) {
        GeburahWeaponRotationController rotationHandler = geburahEntity.getWeaponRotationController();
        float currentRotation = rotationHandler.getLerpedRotation(v);
        fdModelPart.addYRot(currentRotation);
    }
}

