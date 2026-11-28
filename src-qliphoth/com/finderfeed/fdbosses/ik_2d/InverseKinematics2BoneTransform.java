/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimatedObject
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.BoneTransformationController
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModelPart
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.Direction$Axis
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.finderfeed.fdbosses.ik_2d;

import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimatedObject;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.BoneTransformationController;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.bedrock.models.FDModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class InverseKinematics2BoneTransform<T extends AnimatedObject>
implements BoneTransformationController<T> {
    private String controllerBoneStart;
    private String controllerBoneEnd;
    private String first;
    private String second;
    private String end;
    private Direction.Axis legRotateAroundAxis;
    private Direction.Axis legForwardAxis;
    private boolean negateAngles;

    public InverseKinematics2BoneTransform(Direction.Axis legForwardAxis, Direction.Axis legRotateAroundAxis, String controllerBoneStart, String controllerBoneEnd, String first, String second, String end, boolean negateAngles) {
        this.controllerBoneEnd = controllerBoneEnd;
        this.controllerBoneStart = controllerBoneStart;
        this.legRotateAroundAxis = legRotateAroundAxis;
        this.first = first;
        this.second = second;
        this.end = end;
        this.negateAngles = negateAngles;
        this.legForwardAxis = legForwardAxis;
    }

    public void transformBone(T animatedObject, FDModel fdModel, FDModelPart fdModelPart, PoseStack poseStack, MultiBufferSource multiBufferSource, String s, int i, int i1, float v) {
        FDModelPart controllerBoneStart = fdModel.getModelPart(this.controllerBoneStart);
        FDModelPart controllerBoneEnd = fdModel.getModelPart(this.controllerBoneEnd);
        FDModelPart firstPart = fdModel.getModelPart(this.first);
        FDModelPart secondPart = fdModel.getModelPart(this.second);
        FDModelPart endPart = fdModel.getModelPart(this.end);
        double d = this.distanceBetweenModelParts(fdModel, controllerBoneStart, controllerBoneEnd);
        double d1 = this.distanceBetweenModelParts(fdModel, firstPart, secondPart);
        double d2 = this.distanceBetweenModelParts(fdModel, secondPart, endPart);
        firstPart.x = controllerBoneStart.x;
        firstPart.y = controllerBoneStart.y;
        firstPart.z = controllerBoneStart.z;
        double distBetweenControllers = 0.0;
        switch (this.legForwardAxis) {
            case X: {
                distBetweenControllers = controllerBoneStart.x - controllerBoneEnd.x;
                break;
            }
            case Y: {
                distBetweenControllers = controllerBoneStart.y - controllerBoneEnd.y;
                break;
            }
            case Z: {
                distBetweenControllers = controllerBoneStart.z - controllerBoneEnd.z;
            }
        }
        double cos = Math.toDegrees(Math.sin((distBetweenControllers /= 16.0) / d));
        double angle1 = Math.toDegrees(Math.acos((d1 * d1 + d * d - d2 * d2) / (2.0 * d1 * d))) + cos;
        double angle2 = Math.toDegrees(Math.acos((d1 * d1 + d2 * d2 - d * d) / (2.0 * d1 * d2))) - 180.0;
        if (d > d1 + d2) {
            angle1 = cos;
            angle2 = 0.0;
        }
        if (this.negateAngles) {
            angle1 = -angle1;
            angle2 = -angle2;
        }
        switch (this.legRotateAroundAxis) {
            case X: {
                firstPart.setXRot((float)angle1);
                firstPart.setYRot(0.0f);
                firstPart.setZRot(0.0f);
                secondPart.setXRot((float)angle2);
                secondPart.setYRot(0.0f);
                secondPart.setZRot(0.0f);
                break;
            }
            case Y: {
                firstPart.setXRot(0.0f);
                firstPart.setYRot((float)angle1);
                firstPart.setZRot(0.0f);
                secondPart.setXRot(0.0f);
                secondPart.setYRot((float)angle2);
                secondPart.setZRot(0.0f);
                break;
            }
            case Z: {
                firstPart.setXRot(0.0f);
                firstPart.setYRot(0.0f);
                firstPart.setZRot((float)angle1);
                secondPart.setXRot(0.0f);
                secondPart.setYRot(0.0f);
                secondPart.setZRot((float)angle2);
            }
        }
        endPart.x = controllerBoneEnd.x;
        endPart.y = controllerBoneEnd.y;
        endPart.z = controllerBoneEnd.z;
        endPart.setXRot(controllerBoneEnd.getXRot());
        endPart.setYRot(controllerBoneEnd.getYRot());
        endPart.setZRot(controllerBoneEnd.getZRot());
        endPart.xScale = controllerBoneEnd.xScale;
        endPart.yScale = controllerBoneEnd.yScale;
        endPart.zScale = controllerBoneEnd.zScale;
    }

    private double distanceBetweenModelParts(FDModel model, FDModelPart part1, FDModelPart part2) {
        Matrix4f t1 = model.getModelPartTransformation(part1);
        Matrix4f t2 = model.getModelPartTransformation(part2);
        Vector3f p1 = t1.transformPosition(new Vector3f());
        Vector3f p2 = t2.transformPosition(new Vector3f());
        return p1.distance((Vector3fc)p2);
    }
}

