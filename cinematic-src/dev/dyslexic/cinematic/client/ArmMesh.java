/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.model.geom.ModelPart$Cube
 *  net.minecraft.core.Direction
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

public final class ArmMesh {
    private final ModelPart.Cube upper;
    private final ModelPart.Cube hand;
    private final ModelPart.Cube cap;
    public static final float UPPER = 8.0f;
    public static final float HAND = 4.0f;
    private static final ArmMesh RIGHT_WIDE = new ArmMesh(40, 16, -3.0f, 4.0f, 0.0f);
    private static final ArmMesh RIGHT_WIDE_SLEEVE = new ArmMesh(40, 32, -3.0f, 4.0f, 0.25f);
    private static final ArmMesh LEFT_WIDE = new ArmMesh(32, 48, -1.0f, 4.0f, 0.0f);
    private static final ArmMesh LEFT_WIDE_SLEEVE = new ArmMesh(48, 48, -1.0f, 4.0f, 0.25f);
    private static final ArmMesh RIGHT_SLIM = new ArmMesh(40, 16, -2.0f, 3.0f, 0.0f);
    private static final ArmMesh RIGHT_SLIM_SLEEVE = new ArmMesh(40, 32, -2.0f, 3.0f, 0.25f);
    private static final ArmMesh LEFT_SLIM = new ArmMesh(32, 48, -1.0f, 3.0f, 0.0f);
    private static final ArmMesh LEFT_SLIM_SLEEVE = new ArmMesh(48, 48, -1.0f, 3.0f, 0.25f);

    private ArmMesh(int u, int v, float x0, float width, float grow) {
        EnumSet<Direction> sides = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
        EnumSet<Direction> upperFaces = EnumSet.copyOf(sides);
        upperFaces.add(Direction.DOWN);
        this.upper = new ModelPart.Cube(u, v, x0, -2.0f, -2.0f, width, 8.0f, 4.0f, grow, grow, grow, false, 64.0f, 64.0f, upperFaces);
        this.hand = new ModelPart.Cube(u, v + 8, x0, 6.0f, -2.0f, width, 4.0f, 4.0f, grow, grow, grow, false, 64.0f, 64.0f, sides);
        this.cap = new ModelPart.Cube(u, v, x0, -2.0f, -2.0f, width, 12.0f, 4.0f, grow, grow, grow, false, 64.0f, 64.0f, EnumSet.of(Direction.UP));
    }

    public static ArmMesh arm(boolean right, boolean slim) {
        return right ? (slim ? RIGHT_SLIM : RIGHT_WIDE) : (slim ? LEFT_SLIM : LEFT_WIDE);
    }

    public static ArmMesh sleeve(boolean right, boolean slim) {
        return right ? (slim ? RIGHT_SLIM_SLEEVE : RIGHT_WIDE_SLEEVE) : (slim ? LEFT_SLIM_SLEEVE : LEFT_WIDE_SLEEVE);
    }

    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, float stretch) {
        this.hand.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        this.cap.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        pose.m_85836_();
        float joint = 0.375f;
        pose.m_252880_(0.0f, joint, 0.0f);
        pose.m_85841_(1.0f, stretch, 1.0f);
        pose.m_252880_(0.0f, -joint, 0.0f);
        this.upper.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        pose.m_85849_();
    }

    public void renderUpper(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        this.upper.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        this.cap.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void renderForearm(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        this.upper.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        this.hand.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void renderFloating(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        this.hand.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        pose.m_85836_();
        pose.m_252880_(0.0f, 0.375f, 0.0f);
        pose.m_85841_(1.0f, 0.4f, 1.0f);
        pose.m_252880_(0.0f, -0.375f, 0.0f);
        this.upper.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        pose.m_85849_();
        pose.m_85836_();
        pose.m_252880_(0.0f, 0.3f, 0.0f);
        this.cap.m_171332_(pose.m_85850_(), buffer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        pose.m_85849_();
    }
}

