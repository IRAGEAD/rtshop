/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
 */
package dev.dyslexic.cinematic.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class Picking {
    private static final Matrix4f VIEW_PROJ = new Matrix4f();
    private static final Matrix4f INVERSE = new Matrix4f();
    private static Vec3 camera = Vec3.f_82478_;
    private static boolean valid;

    private Picking() {
    }

    public static void capture(RenderLevelStageEvent event) {
        VIEW_PROJ.set((Matrix4fc)event.getProjectionMatrix()).mul((Matrix4fc)event.getPoseStack().m_85850_().m_252922_());
        VIEW_PROJ.invert(INVERSE);
        camera = event.getCamera().m_90583_();
        valid = true;
    }

    public static Vec3 camera() {
        return camera;
    }

    public static double[] mouseNdc() {
        Minecraft mc = Minecraft.m_91087_();
        double w = Math.max(1, mc.m_91268_().m_85443_());
        double h = Math.max(1, mc.m_91268_().m_85444_());
        return new double[]{mc.f_91067_.m_91589_() / w * 2.0 - 1.0, 1.0 - mc.f_91067_.m_91594_() / h * 2.0};
    }

    public static double[] mouseGui() {
        Minecraft mc = Minecraft.m_91087_();
        return new double[]{mc.f_91067_.m_91589_() * (double)mc.m_91268_().m_85445_() / (double)Math.max(1, mc.m_91268_().m_85443_()), mc.f_91067_.m_91594_() * (double)mc.m_91268_().m_85446_() / (double)Math.max(1, mc.m_91268_().m_85444_())};
    }

    @Nullable
    public static Vec3[] cursorRay() {
        if (!valid) {
            return null;
        }
        double[] ndc = Picking.mouseNdc();
        Vector4f near = INVERSE.transform(new Vector4f((float)ndc[0], (float)ndc[1], -1.0f, 1.0f));
        Vector4f far = INVERSE.transform(new Vector4f((float)ndc[0], (float)ndc[1], 1.0f, 1.0f));
        near.div(near.w);
        far.div(far.w);
        Vec3 origin = camera.m_82520_((double)near.x, (double)near.y, (double)near.z);
        Vec3 dir = new Vec3((double)(far.x - near.x), (double)(far.y - near.y), (double)(far.z - near.z)).m_82541_();
        return new Vec3[]{origin, dir};
    }

    @Nullable
    public static Vec3 cursorOnPlane(double planeY) {
        return Picking.cursorOnPlane(new Vec3(0.0, planeY, 0.0), new Vec3(0.0, 1.0, 0.0));
    }

    @Nullable
    public static Vec3 cursorOnPlane(Vec3 point, Vec3 normal) {
        if (!valid) {
            return null;
        }
        double[] ndc = Picking.mouseNdc();
        Vector4f near = INVERSE.transform(new Vector4f((float)ndc[0], (float)ndc[1], -1.0f, 1.0f));
        Vector4f far = INVERSE.transform(new Vector4f((float)ndc[0], (float)ndc[1], 1.0f, 1.0f));
        near.div(near.w);
        far.div(far.w);
        Vec3 origin = camera.m_82520_((double)near.x, (double)near.y, (double)near.z);
        Vec3 dir = new Vec3((double)(far.x - near.x), (double)(far.y - near.y), (double)(far.z - near.z));
        double denom = dir.m_82526_(normal);
        if (Math.abs(denom) < 1.0E-9) {
            return null;
        }
        double t = point.m_82546_(origin).m_82526_(normal) / denom;
        return t < 0.0 ? null : origin.m_82549_(dir.m_82490_(t));
    }

    @Nullable
    public static double[] toGui(Vec3 world) {
        if (!valid) {
            return null;
        }
        Vector4f clip = VIEW_PROJ.transform(new Vector4f((float)(world.f_82479_ - Picking.camera.f_82479_), (float)(world.f_82480_ - Picking.camera.f_82480_), (float)(world.f_82481_ - Picking.camera.f_82481_), 1.0f));
        if ((double)clip.w <= 1.0E-4) {
            return null;
        }
        Minecraft mc = Minecraft.m_91087_();
        double nx = clip.x / clip.w;
        double ny = clip.y / clip.w;
        return new double[]{(nx + 1.0) / 2.0 * (double)mc.m_91268_().m_85445_(), (1.0 - ny) / 2.0 * (double)mc.m_91268_().m_85446_()};
    }
}

