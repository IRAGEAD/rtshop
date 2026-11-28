/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.CameraType
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
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

import com.mojang.blaze3d.platform.InputConstants;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.CampfireStation;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.KeyLabels;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneScreen;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.Showcase;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FreeLook {
    public static final KeyMapping LOOK = new KeyMapping("key.cinematic.look", InputConstants.Type.KEYSYM, 342, "key.categories.cinematic");
    private static final float START_TURN = 38.0f;
    private static final float START_PITCH = 24.0f;
    private static final double START_DISTANCE = 2.5;
    private static final double NEAREST = 1.3;
    private static final double FURTHEST = 4.5;
    private static final double RATE = 5.5;
    private static boolean wanted;
    private static double amount;
    private static float yaw;
    private static float pitch;
    private static double distance;
    private static boolean grabbed;
    private static double savedX;
    private static double savedY;
    private static double lastX;
    private static double lastY;
    private static boolean third;

    private FreeLook() {
    }

    public static double amount() {
        return Anim.smoother(amount);
    }

    public static boolean looking() {
        return wanted || amount > 0.02;
    }

    public static boolean showsBody() {
        return third;
    }

    static boolean keyPressed(int key, int scanCode) {
        if (!LOOK.m_90832_(key, scanCode) || Scene.phase != Scene.Phase.ACTIVE) {
            return false;
        }
        wanted = !Settings.lookToggles() || !wanted;
        return true;
    }

    static boolean keyReleased(int key, int scanCode) {
        if (!LOOK.m_90832_(key, scanCode)) {
            return false;
        }
        if (!Settings.lookToggles()) {
            wanted = false;
        }
        return true;
    }

    static void stop() {
        wanted = false;
    }

    static void scroll(double amount) {
        distance = Mth.m_14008_((double)(distance - amount * 0.35), (double)1.3, (double)4.5);
    }

    static Component hint() {
        return Component.m_237110_((String)(Settings.lookToggles() ? "cinematic.look.toggle" : "cinematic.look.hold"), (Object[])new Object[]{KeyLabels.of(LOOK)});
    }

    static void frame(double dt) {
        boolean needThird;
        Minecraft mc = Minecraft.m_91087_();
        if (Scene.phase != Scene.Phase.ACTIVE || !(mc.f_91080_ instanceof SceneScreen) || !mc.m_91302_() && !Showcase.ENABLED) {
            wanted = false;
        }
        if (wanted && !grabbed) {
            FreeLook.grab(mc);
        } else if (!wanted && grabbed) {
            FreeLook.release(mc);
        }
        if (grabbed) {
            double dx = mc.f_91067_.m_91589_() - lastX;
            double dy = mc.f_91067_.m_91594_() - lastY;
            lastX = mc.f_91067_.m_91589_();
            lastY = mc.f_91067_.m_91594_();
            double s = (Double)mc.f_91066_.m_231964_().m_231551_() * 0.6 + 0.2;
            double k = s * s * s * 8.0 * 0.15;
            yaw += (float)(dx * k);
            pitch = Mth.m_14036_((float)(pitch + (float)(dy * k)), (float)-20.0f, (float)80.0f);
        }
        if ((amount = Anim.approach(amount, wanted ? 1.0 : 0.0, 5.5, dt)) < 0.001 && !wanted) {
            amount = 0.0;
        }
        boolean bl = Scene.station != null && (Scene.phase == Scene.Phase.ACTIVE ? FreeLook.amount() > 0.3 : third && FreeLook.outroFar(mc)) ? true : (needThird = false);
        if (needThird != third) {
            third = needThird;
            mc.f_91066_.m_92157_(third ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
        }
    }

    private static boolean outroFar(Minecraft mc) {
        CameraRig.Pose last = CameraRig.last();
        return Scene.phase == Scene.Phase.OUTRO && last != null && mc.f_91074_ != null && last.pos().m_82554_(mc.f_91074_.m_146892_()) > 0.7;
    }

    private static void grab(Minecraft mc) {
        savedX = mc.f_91067_.m_91589_();
        savedY = mc.f_91067_.m_91594_();
        InputConstants.m_84833_((long)mc.m_91268_().m_85439_(), (int)212995, (double)savedX, (double)savedY);
        lastX = mc.f_91067_.m_91589_();
        lastY = mc.f_91067_.m_91594_();
        grabbed = true;
        if (amount < 0.05 && mc.f_91074_ != null) {
            yaw = mc.f_91074_.m_146908_() + 38.0f;
            pitch = 24.0f;
            distance = 2.5;
        }
    }

    private static void release(Minecraft mc) {
        InputConstants.m_84833_((long)mc.m_91268_().m_85439_(), (int)212993, (double)savedX, (double)savedY);
        mc.f_91067_.f_91507_ = savedX;
        mc.f_91067_.f_91508_ = savedY;
        grabbed = false;
    }

    static void reset() {
        Minecraft mc = Minecraft.m_91087_();
        wanted = false;
        amount = 0.0;
        if (grabbed) {
            FreeLook.release(mc);
        }
        if (third) {
            third = false;
            mc.f_91066_.m_92157_(CameraType.FIRST_PERSON);
        }
    }

    static CameraRig.Pose apply(CameraRig.Pose scene) {
        double d;
        Vec3 dir;
        double w = FreeLook.amount();
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        ClientStation station = Scene.station;
        if (w <= 0.001 || player == null || station == null || mc.f_91073_ == null) {
            return scene;
        }
        Vec3 focus = FreeLook.focus(player, station);
        BlockHitResult hit = mc.f_91073_.m_45547_(new ClipContext(focus, focus.m_82546_((dir = Vec3.m_82498_((float)pitch, (float)yaw)).m_82490_((d = distance) + 0.3)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, (Entity)player));
        if (hit.m_6662_() != HitResult.Type.MISS) {
            d = Math.max(0.5, hit.m_82450_().m_82554_(focus) - 0.3);
        }
        Vec3 orbit = focus.m_82546_(dir.m_82490_(d));
        Vec3 pos = Anim.lerp(scene.pos(), orbit, w).m_82520_(0.0, 0.35 * Anim.hump(w), 0.0);
        float turnedYaw = scene.yaw() + Mth.m_14177_((float)(yaw - scene.yaw())) * (float)w;
        return new CameraRig.Pose(pos, turnedYaw, Mth.m_14179_((float)((float)w), (float)scene.pitch(), (float)pitch), Mth.m_14179_((float)((float)w), (float)scene.roll(), (float)0.0f));
    }

    private static Vec3 focus(LocalPlayer player, ClientStation station) {
        double chest = station instanceof CampfireStation ? 0.45 : (player.m_6047_() ? 0.85 : 1.05);
        return Anim.lerp(player.m_20182_().m_82520_(0.0, chest, 0.0), station.workPoint(), 0.3);
    }

    static {
        distance = 2.5;
    }
}

