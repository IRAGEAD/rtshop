/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.inventory.InventoryMenu
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.model.data.ModelData
 *  org.joml.Matrix3f
 *  org.joml.Quaternionf
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.Cinematic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public final class HammerModel {
    public static final ResourceLocation LOCATION = Cinematic.id("prop/hammer");
    private static final float SCALE = 0.3f;
    private static final Vec3 GRIP = new Vec3(0.5, 0.18, 0.5);
    static final double STRIKE_ALONG = (1.125 - HammerModel.GRIP.f_82480_) * (double)0.3f;
    static final double STRIKE_ACROSS = 0.11250000447034836;
    private static final float ARM_SCALE = 0.39000002f;
    private static final double WRIST = Math.toRadians(18.0);

    private HammerModel() {
    }

    static BakedModel model() {
        return Minecraft.m_91087_().m_91304_().getModel(LOCATION);
    }

    static void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 grip, Vec3 along, Vec3 headAxis, int light) {
        pose.m_85836_();
        pose.m_85837_(grip.f_82479_ - camera.f_82479_, grip.f_82480_ - camera.f_82480_, grip.f_82481_ - camera.f_82481_);
        HammerModel.draw(pose, buffers, along, headAxis, 0.3f, light);
        pose.m_85849_();
    }

    static void renderInArm(PoseStack pose, MultiBufferSource buffers, double shown, int light) {
        pose.m_85836_();
        pose.m_252880_(-0.0625f, 0.6f, -0.1f);
        HammerModel.draw(pose, buffers, new Vec3(0.0, Math.cos(WRIST), -Math.sin(WRIST)), new Vec3(0.0, -Math.sin(WRIST), -Math.cos(WRIST)), (float)(0.39000001549720764 * shown), light);
        pose.m_85849_();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 along, Vec3 headAxis, float scale, int light) {
        Vec3 z = headAxis.m_82537_(along);
        Quaternionf rotation = new Matrix3f((float)headAxis.f_82479_, (float)headAxis.f_82480_, (float)headAxis.f_82481_, (float)along.f_82479_, (float)along.f_82480_, (float)along.f_82481_, (float)z.f_82479_, (float)z.f_82480_, (float)z.f_82481_).getNormalizedRotation(new Quaternionf());
        pose.m_85836_();
        pose.m_252781_(rotation);
        pose.m_85841_(scale, scale, scale);
        pose.m_85837_(-HammerModel.GRIP.f_82479_, -HammerModel.GRIP.f_82480_, -HammerModel.GRIP.f_82481_);
        Minecraft.m_91087_().m_91289_().m_110937_().renderModel(pose.m_85850_(), buffers.m_6299_(RenderType.m_110458_((ResourceLocation)InventoryMenu.f_39692_)), null, HammerModel.model(), 1.0f, 1.0f, 1.0f, light, OverlayTexture.f_118083_, ModelData.EMPTY, null);
        pose.m_85849_();
    }
}

