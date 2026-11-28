/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Reach;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CameraRig {
    @Nullable
    private static Pose last;
    private static double weight;
    private static double parallaxX;
    private static double parallaxY;
    private static Vec3 reachLean;

    private CameraRig() {
    }

    public static double weight() {
        return weight;
    }

    @Nullable
    public static Pose last() {
        return last;
    }

    public static float roll() {
        return Scene.active() && last != null ? last.roll() : 0.0f;
    }

    @Nullable
    public static Pose override(Vec3 livePos, float liveYaw, float livePitch) {
        Pose pose;
        ClientStation station = Scene.station;
        if (!Scene.active() || station == null) {
            last = null;
            weight = 0.0;
            return null;
        }
        Pose live = new Pose(livePos, liveYaw, livePitch, 0.0f);
        switch (Scene.phase) {
            case INTRO: {
                double u = Scene.introProgress();
                double ep = Scene.arrived() ? Anim.inOutCubic(Anim.window(u, Scene.walkShare, 1.0)) : 0.0;
                double er = Scene.arrived() ? Anim.inOutCubic(Anim.window(u, Scene.walkShare, 0.82)) : 0.0;
                Pose lean = CameraRig.tether(CameraRig.breathe(station.view(), 0.0), livePos, station);
                Pose start = Scene.transitionFrom == null ? live : CameraRig.blend(Scene.transitionFrom, live, Scene.released());
                Vec3 pos = Anim.lerp(start.pos(), lean.pos(), ep).m_82520_(0.0, (Scene.transition ? 0.02 : 0.045) * Anim.hump(u), 0.0);
                float turn = Mth.m_14177_((float)(lean.yaw() - start.yaw()));
                float yaw = start.yaw() + turn * (float)er;
                float pitch = Mth.m_14179_((float)((float)er), (float)start.pitch(), (float)lean.pitch());
                float swing = (float)(Anim.hump(u) * Mth.m_14008_((double)((double)turn * 0.04), (double)-1.6, (double)1.6));
                pose = new Pose(pos, yaw, pitch, swing + Mth.m_14179_((float)((float)er), (float)start.roll(), (float)lean.roll()));
                weight = Scene.transition ? Math.max(ep, 1.0 - Scene.released()) : ep;
                break;
            }
            case ACTIVE: {
                Pose base = CameraRig.tether(CameraRig.breathe(station.view(), Anim.smoother(Scene.time / 0.6)), livePos, station);
                pose = FreeLook.apply(CameraRig.kick(CameraRig.naturalReach(base, livePos, station), station.cameraKick()));
                weight = 1.0;
                break;
            }
            default: {
                Pose from = Scene.outroFrom != null ? Scene.outroFrom : station.view();
                double u = Scene.time / 1.05;
                double ep = Anim.inOutCubic(Anim.window(u, 0.14, 1.0));
                double er = Anim.inOutCubic(Anim.window(u, 0.2, 1.0));
                Vec3 pos = Anim.lerp(from.pos(), live.pos(), ep).m_82520_(0.0, 0.03 * Anim.hump(ep), 0.0);
                float yaw = from.yaw() + Mth.m_14177_((float)(live.yaw() - from.yaw())) * (float)er;
                float pitch = Mth.m_14179_((float)((float)er), (float)from.pitch(), (float)live.pitch());
                float roll = Mth.m_14179_((float)((float)er), (float)from.roll(), (float)0.0f);
                pose = new Pose(pos, yaw, pitch, roll);
                weight = (1.0 - ep) * Scene.outroWeight;
            }
        }
        last = pose;
        return pose;
    }

    private static Pose blend(Pose a, Pose b, double k) {
        float f = (float)k;
        return new Pose(Anim.lerp(a.pos(), b.pos(), k), a.yaw() + Mth.m_14177_((float)(b.yaw() - a.yaw())) * f, Mth.m_14179_((float)f, (float)a.pitch(), (float)b.pitch()), Mth.m_14179_((float)f, (float)a.roll(), (float)b.roll()));
    }

    private static Pose tether(Pose pose, Vec3 eye, ClientStation station) {
        BlockHitResult hit;
        Pose held = Reach.cameraPose(station, pose, eye);
        if (!Scene.clientOnly()) {
            return held;
        }
        Vec3 offset = held.pos().m_82546_(eye);
        Vec3 end = eye.m_82549_(offset.m_82553_() > 0.35 ? offset.m_82541_().m_82490_(0.35) : offset);
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ != null && mc.f_91074_ != null && (hit = mc.f_91073_.m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)mc.f_91074_))).m_6662_() != HitResult.Type.MISS) {
            Vec3 travel = hit.m_82450_().m_82546_(eye);
            end = eye.m_82549_(travel.m_82553_() > 0.08 ? travel.m_82541_().m_82490_(travel.m_82553_() - 0.08) : Vec3.f_82478_);
        }
        return new Pose(end, held.yaw(), held.pitch(), held.roll());
    }

    private static Pose kick(Pose pose, double kick) {
        if (kick <= 0.001 || !Settings.cameraShake()) {
            return pose;
        }
        double t = Scene.clock();
        float shakeYaw = (float)((Math.sin(t * 47.0) + Math.sin(t * 31.0 + 1.0)) * 0.18 * kick);
        float shakePitch = (float)((Math.sin(t * 53.0 + 2.0) + Math.sin(t * 29.0)) * 0.14 * kick);
        return new Pose(pose.pos(), pose.yaw() + shakeYaw, pose.pitch() + shakePitch, pose.roll() + shakeYaw * 0.4f);
    }

    private static Pose naturalReach(Pose pose, Vec3 liveEye, ClientStation station) {
        BlockHitResult hit;
        Vec3 fromEye;
        Vec3 toward;
        Vec3 target = Vec3.f_82478_;
        if (Settings.armStyle() == 1 && station.aim != null && FreeLook.amount() < 0.01 && (toward = station.aim.m_82546_(pose.pos())).m_82556_() > 1.0E-6) {
            target = toward.m_82541_().m_82490_(Math.min(0.09, toward.m_82553_() * 0.12));
        }
        if ((reachLean = Anim.lerp(reachLean, target, 1.0 - Math.exp(-Scene.frameDt * 7.0))).m_82556_() < 1.0E-8) {
            return pose;
        }
        Vec3 end = pose.pos().m_82549_(reachLean);
        if (Scene.clientOnly() && (fromEye = end.m_82546_(liveEye)).m_82553_() > 0.35) {
            end = liveEye.m_82549_(fromEye.m_82541_().m_82490_(0.35));
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ != null && mc.f_91074_ != null && (hit = mc.f_91073_.m_45547_(new ClipContext(pose.pos(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)mc.f_91074_))).m_6662_() != HitResult.Type.MISS) {
            end = pose.pos();
        }
        return new Pose(end, pose.yaw(), pose.pitch(), pose.roll());
    }

    private static Pose breathe(Pose pose, double parallax) {
        double[] dArray;
        if (!Settings.livingCamera()) {
            parallaxY = 0.0;
            parallaxX = 0.0;
            return pose;
        }
        double t = Scene.clock();
        double dt = Scene.frameDt;
        if (Scene.phase == Scene.Phase.ACTIVE) {
            dArray = Picking.mouseNdc();
        } else {
            double[] dArray2 = new double[2];
            dArray2[0] = 0.0;
            dArray = dArray2;
            dArray2[1] = 0.0;
        }
        double[] ndc = dArray;
        parallaxX = Anim.approach(parallaxX, Mth.m_14008_((double)ndc[0], (double)-1.0, (double)1.0), 4.0, dt);
        parallaxY = Anim.approach(parallaxY, Mth.m_14008_((double)ndc[1], (double)-1.0, (double)1.0), 4.0, dt);
        Vec3 pos = pose.pos().m_82520_(0.0, Math.sin(t * 1.3) * 0.0035, 0.0);
        float yaw = pose.yaw() + (float)(Math.sin(t * 0.61) * 0.16 + parallaxX * 1.1 * parallax);
        float pitch = pose.pitch() + (float)(Math.sin(t * 0.83 + 1.1) * 0.13 - parallaxY * 0.8 * parallax);
        return new Pose(pos, yaw, pitch, pose.roll());
    }

    public static double fov(double vanilla) {
        ClientStation station = Scene.station;
        if (station == null) {
            return vanilla;
        }
        double punch = Settings.cameraShake() ? 7.0 * station.cameraKick() : 0.0;
        return Mth.m_14139_((double)(weight * Settings.sceneZoom() * (1.0 - FreeLook.amount())), (double)vanilla, (double)station.leanFov()) - punch * (1.0 - FreeLook.amount());
    }

    public static void reset() {
        last = null;
        weight = 0.0;
        parallaxY = 0.0;
        parallaxX = 0.0;
        reachLean = Vec3.f_82478_;
    }

    static {
        reachLean = Vec3.f_82478_;
    }

    public record Pose(Vec3 pos, float yaw, float pitch, float roll) {
    }
}

