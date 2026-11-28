/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.model.geom.ModelPart$Cube
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.PlayerModelPart
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix3f
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.CampfireArmor;
import java.util.EnumSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class LegMesh {
    private final ModelPart.Cube thigh;
    private final ModelPart.Cube shin;
    private final float kneeOverlap;
    private static final float HALF = 6.0f;
    private static final float KNEE_OVERLAP = 2.0f;
    private static final float INSET = 0.02f;
    private static final LegMesh RIGHT = new LegMesh(0, 16, 0.0f);
    private static final LegMesh RIGHT_PANTS = new LegMesh(0, 32, 0.25f);
    private static final LegMesh LEFT = new LegMesh(16, 48, 0.0f);
    private static final LegMesh LEFT_PANTS = new LegMesh(0, 48, 0.25f);

    private LegMesh(int u, int v, float grow) {
        this(u, v, grow, 64.0f, 2.0f);
    }

    LegMesh(int u, int v, float grow, float textureHeight, float overlap) {
        this.kneeOverlap = overlap;
        EnumSet<Direction> closed = EnumSet.allOf(Direction.class);
        this.thigh = new ModelPart.Cube(u, v, -2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, grow, grow, grow, false, 64.0f, textureHeight, closed);
        this.shin = new ModelPart.Cube(u, v + 6, -2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, grow - 0.02f, grow, grow - 0.02f, false, 64.0f, textureHeight, closed);
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 camera, LocalPlayer player, boolean right, Vec3 hip, Vec3 knee, Vec3 foot, Vec3 outward, int light) {
        ResourceLocation texture = player.m_108560_();
        LegMesh leg = right ? RIGHT : LEFT;
        LegMesh pants = right ? RIGHT_PANTS : LEFT_PANTS;
        boolean pantsShown = player.m_36170_(right ? PlayerModelPart.RIGHT_PANTS_LEG : PlayerModelPart.LEFT_PANTS_LEG);
        Vec3 along = knee.m_82546_(hip);
        Vec3 past = along.m_82556_() < 1.0E-8 ? knee : knee.m_82549_(along.m_82541_().m_82490_((double)0.06875f));
        LegMesh.segment(pose, buffers.m_6299_(RenderType.m_110446_((ResourceLocation)texture)), camera, leg.thigh, hip, past, outward, light);
        LegMesh.segment(pose, buffers.m_6299_(RenderType.m_110446_((ResourceLocation)texture)), camera, leg.shin, knee, foot, outward, light);
        if (pantsShown) {
            LegMesh.segment(pose, buffers.m_6299_(RenderType.m_110473_((ResourceLocation)texture)), camera, pants.thigh, hip, past, outward, light);
            LegMesh.segment(pose, buffers.m_6299_(RenderType.m_110473_((ResourceLocation)texture)), camera, pants.shin, knee, foot, outward, light);
        }
        CampfireArmor.drawInScene(pose, buffers, camera, (AbstractClientPlayer)player, right, hip, knee, foot, outward, light);
    }

    static void drawOnModel(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, boolean right, float knee, int light, int overlay) {
        ResourceLocation texture = player.m_108560_();
        LegMesh leg = right ? RIGHT : LEFT;
        LegMesh pants = right ? RIGHT_PANTS : LEFT_PANTS;
        boolean pantsShown = player.m_36170_(right ? PlayerModelPart.RIGHT_PANTS_LEG : PlayerModelPart.LEFT_PANTS_LEG);
        pose.m_85836_();
        pose.m_85836_();
        pose.m_85841_(1.0f, 1.3333334f, 1.0f);
        leg.thigh.m_171332_(pose.m_85850_(), buffers.m_6299_(RenderType.m_110446_((ResourceLocation)texture)), light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        if (pantsShown) {
            pants.thigh.m_171332_(pose.m_85850_(), buffers.m_6299_(RenderType.m_110473_((ResourceLocation)texture)), light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        pose.m_85849_();
        pose.m_252880_(0.0f, 0.375f, 0.0f);
        pose.m_252781_(Axis.f_252529_.m_252961_(knee));
        leg.shin.m_171332_(pose.m_85850_(), buffers.m_6299_(RenderType.m_110446_((ResourceLocation)texture)), light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        if (pantsShown) {
            pants.shin.m_171332_(pose.m_85850_(), buffers.m_6299_(RenderType.m_110473_((ResourceLocation)texture)), light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        pose.m_85849_();
    }

    void drawBentOnModel(PoseStack pose, VertexConsumer consumer, float knee, int light, float red, float green, float blue) {
        pose.m_85836_();
        pose.m_85836_();
        pose.m_85841_(1.0f, (6.0f + this.kneeOverlap) / 6.0f, 1.0f);
        this.thigh.m_171332_(pose.m_85850_(), consumer, light, OverlayTexture.f_118083_, red, green, blue, 1.0f);
        pose.m_85849_();
        pose.m_252880_(0.0f, 0.375f, 0.0f);
        pose.m_252781_(Axis.f_252529_.m_252961_(knee));
        this.shin.m_171332_(pose.m_85850_(), consumer, light, OverlayTexture.f_118083_, red, green, blue, 1.0f);
        pose.m_85849_();
    }

    void drawShinOnModel(PoseStack pose, VertexConsumer consumer, float knee, int light, float red, float green, float blue) {
        pose.m_85836_();
        pose.m_252880_(0.0f, 0.375f, 0.0f);
        pose.m_252781_(Axis.f_252529_.m_252961_(knee));
        this.shin.m_171332_(pose.m_85850_(), consumer, light, OverlayTexture.f_118083_, red, green, blue, 1.0f);
        pose.m_85849_();
    }

    void drawBentInScene(PoseStack pose, VertexConsumer consumer, Vec3 camera, Vec3 hip, Vec3 knee, Vec3 foot, Vec3 outward, int light, float red, float green, float blue) {
        Vec3 along = knee.m_82546_(hip);
        Vec3 past = along.m_82556_() < 1.0E-8 ? knee : knee.m_82549_(along.m_82541_().m_82490_((double)(this.kneeOverlap / 16.0f * 0.55f)));
        LegMesh.segment(pose, consumer, camera, this.thigh, hip, past, outward, light, red, green, blue);
        LegMesh.segment(pose, consumer, camera, this.shin, knee, foot, outward, light, red, green, blue);
    }

    void drawShinInScene(PoseStack pose, VertexConsumer consumer, Vec3 camera, Vec3 knee, Vec3 foot, Vec3 outward, int light, float red, float green, float blue) {
        LegMesh.segment(pose, consumer, camera, this.shin, knee, foot, outward, light, red, green, blue);
    }

    private static void segment(PoseStack pose, VertexConsumer buffer, Vec3 camera, ModelPart.Cube cube, Vec3 from, Vec3 to, Vec3 outward, int light) {
        LegMesh.segment(pose, buffer, camera, cube, from, to, outward, light, 1.0f, 1.0f, 1.0f);
    }

    private static void segment(PoseStack pose, VertexConsumer buffer, Vec3 camera, ModelPart.Cube cube, Vec3 from, Vec3 to, Vec3 outward, int light, float red, float green, float blue) {
        Vec3 d = to.m_82546_(from);
        double length = d.m_82553_();
        if (length < 1.0E-4) {
            return;
        }
        Vec3 x = outward.m_82546_((d = d.m_82490_(1.0 / length)).m_82490_(outward.m_82526_(d)));
        if (x.m_82556_() < 1.0E-6) {
            x = new Vec3(0.0, 1.0, 0.0).m_82537_(d);
        }
        x = x.m_82541_();
        Vec3 z = x.m_82537_(d);
        Quaternionf rotation = new Matrix3f((Vector3fc)new Vector3f((float)x.f_82479_, (float)x.f_82480_, (float)x.f_82481_), (Vector3fc)new Vector3f((float)d.f_82479_, (float)d.f_82480_, (float)d.f_82481_), (Vector3fc)new Vector3f((float)z.f_82479_, (float)z.f_82480_, (float)z.f_82481_)).getNormalizedRotation(new Quaternionf());
        float scale = 0.55f;
        float stretch = (float)(length * 16.0 / (double)scale / 6.0);
        pose.m_85836_();
        pose.m_85837_(from.f_82479_ - camera.f_82479_, from.f_82480_ - camera.f_82480_, from.f_82481_ - camera.f_82481_);
        pose.m_252781_(rotation);
        pose.m_85841_(scale, scale * stretch, scale);
        cube.m_171332_(pose.m_85850_(), buffer, light, OverlayTexture.f_118083_, red, green, blue, 1.0f);
        pose.m_85849_();
    }
}

