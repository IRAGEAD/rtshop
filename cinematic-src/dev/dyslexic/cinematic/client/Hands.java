/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.player.PlayerRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.PlayerModelPart
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.fml.ModList
 *  org.joml.Matrix3f
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.ArmMesh;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.client.SurfaceStation;
import dev.dyslexic.cinematic.client.TableLayouts;
import dev.dyslexic.cinematic.client.TableStation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class Hands {
    static final float ARM_SCALE = 0.55f;
    private static final boolean YSM_PRESENT = ModList.get().isLoaded("yes_steve_model");
    private static final double NATURAL_SEGMENT = (double)0.275f;
    private static final double FLOATING_LENGTH = 0.24750000536441805;
    private static final Anim.Spring3 RIGHT = new Anim.Spring3();
    private static final Anim.Spring3 LEFT = new Anim.Spring3();
    private static Vec3 outroRight = Vec3.f_82478_;
    private static Vec3 outroLeft = Vec3.f_82478_;
    private static Vec3 fromRight;
    private static Vec3 fromLeft;
    private static double tapAt;
    private static boolean leftWorks;
    private static boolean rightLanded;
    private static boolean leftLanded;
    private static final double ARM_RADIUS = 0.07875000149011611;

    private Hands() {
    }

    static void begin() {
        fromLeft = null;
        fromRight = null;
        leftLanded = false;
        rightLanded = false;
        leftWorks = false;
        tapAt = -10.0;
    }

    static void enterActive() {
        RIGHT.snap(Scene.station.rest(true));
        LEFT.snap(Scene.station.rest(false));
    }

    static void beginTransition() {
        fromRight = RIGHT.pos();
        fromLeft = LEFT.pos();
        leftLanded = false;
        rightLanded = false;
        leftWorks = false;
        tapAt = -10.0;
    }

    static void beginOutro() {
        outroRight = RIGHT.pos();
        outroLeft = LEFT.pos();
    }

    static void tap() {
        tapAt = Scene.clock();
    }

    static Vec3 shoulder(boolean right, Vec3 camera, float cameraYaw) {
        if (Settings.armStyle() == 1) {
            Camera view = Minecraft.m_91087_().f_91063_.m_109153_();
            Vector3f left = view.m_252775_();
            Vector3f up = view.m_253028_();
            Vector3f look = view.m_253058_();
            return camera.m_82549_(new Vec3((double)(-left.x()), (double)(-left.y()), (double)(-left.z())).m_82490_(right ? 0.38 : -0.38)).m_82546_(new Vec3((double)up.x(), (double)up.y(), (double)up.z()).m_82490_(0.46)).m_82549_(new Vec3((double)look.x(), (double)look.y(), (double)look.z()).m_82490_(0.08));
        }
        Vec3 forward = Scene.yawForward(cameraYaw);
        Vec3 rightVec = new Vec3(-forward.f_82481_, 0.0, forward.f_82479_);
        return camera.m_82549_(rightVec.m_82490_(right ? 0.38 : -0.38)).m_82520_(0.0, -0.46, 0.0).m_82549_(forward.m_82490_(0.08));
    }

    static boolean hides(Vec3 camera, float cameraYaw, Vec3 point) {
        for (boolean right : new boolean[]{true, false}) {
            Vec3 fist;
            Vec3 shoulder = Hands.shoulder(right, camera, cameraYaw);
            Vec3 hand = Hands.position(right);
            Vec3 along = hand.m_82546_(shoulder);
            if (along.m_82556_() < 1.0E-6) continue;
            if (Settings.armStyle() == 1 && along.m_82556_() >= 0.04) {
                Vec3[] bent = Hands.bentPoints(shoulder, hand, right, Scene.yawForward(cameraYaw));
                if (!(Hands.closest(camera, point, bent[0], bent[1]) < 0.07875000149011611) && !(Hands.closest(camera, point, bent[1], hand) < 0.07875000149011611)) continue;
                return true;
            }
            if (Settings.armStyle() == 2) {
                shoulder = hand.m_82546_(along.m_82541_().m_82490_(0.24750000536441805));
            }
            if (!(Hands.closest(camera, point, shoulder, fist = hand.m_82549_(along.m_82541_().m_82490_(0.05))) < 0.07875000149011611)) continue;
            return true;
        }
        return false;
    }

    private static double closest(Vec3 camera, Vec3 point, Vec3 a, Vec3 b) {
        double bb;
        Vec3 d1 = point.m_82546_(camera);
        Vec3 d2 = b.m_82546_(a);
        Vec3 r = camera.m_82546_(a);
        double aa = d1.m_82526_(d1);
        double ee = d2.m_82526_(d2);
        double f = d2.m_82526_(r);
        double c = d1.m_82526_(r);
        double denom = aa * ee - (bb = d1.m_82526_(d2)) * bb;
        double s = denom > 1.0E-9 ? Mth.m_14008_((double)((bb * f - c * ee) / denom), (double)0.0, (double)1.0) : 0.0;
        double t = (bb * s + f) / ee;
        if (t < 0.0) {
            t = 0.0;
            s = Mth.m_14008_((double)(-c / aa), (double)0.0, (double)1.0);
        } else if (t > 1.0) {
            t = 1.0;
            s = Mth.m_14008_((double)((bb - c) / aa), (double)0.0, (double)1.0);
        }
        if (s > 0.98) {
            return Double.MAX_VALUE;
        }
        return camera.m_82549_(d1.m_82490_(s)).m_82554_(a.m_82549_(d2.m_82490_(t)));
    }

    static Vec3 hidden(boolean right, Vec3 camera, float cameraYaw) {
        return Hands.shoulder(right, camera, cameraYaw).m_82549_(Scene.yawForward(cameraYaw).m_82490_(0.1)).m_82520_(0.0, -0.05, 0.0);
    }

    static void update(Vec3 camera, float cameraYaw) {
        ClientStation station = Scene.station;
        if (station == null) {
            return;
        }
        double dt = Scene.frameDt;
        switch (Scene.phase) {
            case INTRO: {
                double u = Scene.introProgress();
                boolean carried = fromRight != null && Scene.transition;
                double kr = carried ? Anim.window(u, 0.08, 0.85) : Anim.window(u, 0.42, 0.93);
                double kl = carried ? Anim.window(u, 0.12, 0.9) : Anim.window(u, 0.47, 0.98);
                RIGHT.snap(Hands.arrive(station, true, kr, camera, cameraYaw));
                LEFT.snap(Hands.arrive(station, false, kl, camera, cameraYaw));
                if (!rightLanded && kr >= 0.97) {
                    rightLanded = true;
                    Scene.sound(station.touchSound(), 0.45f, 0.82f);
                }
                if (leftLanded || !(kl >= 0.97)) break;
                leftLanded = true;
                Scene.sound(station.touchSound(), 0.38f, 0.74f);
                break;
            }
            case ACTIVE: {
                Hands.follow(station, camera, cameraYaw, dt);
                break;
            }
            case OUTRO: {
                double u = Scene.time / 1.05;
                RIGHT.snap(Hands.depart(outroRight, true, u, camera, cameraYaw));
                LEFT.snap(Hands.depart(outroLeft, false, u - 0.05, camera, cameraYaw));
                break;
            }
        }
    }

    private static Vec3 arrive(ClientStation station, boolean right, double k, Vec3 camera, float cameraYaw) {
        Vec3 hiddenAt = Hands.hidden(right, camera, cameraYaw);
        Vec3 was = right ? fromRight : fromLeft;
        Vec3 from = was != null && Scene.transition ? Anim.lerp(was, hiddenAt, Scene.released()) : hiddenAt;
        Vec3 to = station.rest(right);
        double e = Anim.outCubic(k);
        double hover = 0.11 * Anim.hump(Math.min(1.0, k * 1.15)) * (1.0 - Anim.window(k, 0.75, 1.0));
        return Anim.lerp(from, to, e).m_82520_(0.0, hover, 0.0);
    }

    private static Vec3 depart(Vec3 from, boolean right, double u, Vec3 camera, float cameraYaw) {
        double lift = Anim.outCubic(Anim.window(u, 0.0, 0.3)) * 0.07;
        double away = Anim.inCubic(Anim.window(u, 0.18, 0.72));
        return Anim.lerp(from.m_82520_(0.0, lift, 0.0), Hands.hidden(right, camera, cameraYaw), away);
    }

    private static void follow(ClientStation station, Vec3 camera, float cameraYaw, double dt) {
        Vec3 shoulder;
        Vec3[] special = station.handsOverride();
        if (special != null) {
            double stiffness = station.handsStiffness();
            RIGHT.update(special[0], stiffness, dt);
            LEFT.update(special[1], stiffness, dt);
            return;
        }
        Vec3 aim = station.aim;
        if (aim != null) {
            double lateral = station.lateral(aim);
            if (!leftWorks && lateral < -0.13) {
                leftWorks = true;
            } else if (leftWorks && lateral > -0.07) {
                leftWorks = false;
            }
        }
        boolean carrying = !Hands.carried().m_41619_();
        double tap = Anim.hump(Anim.window(Scene.clock() - tapAt, 0.0, 0.22));
        Vec3 working = aim != null ? station.handTarget(aim, shoulder, carrying, tap) : station.rest(!leftWorks).m_82520_(0.0, (carrying ? 0.05 : 0.0) - 0.02 * tap, 0.0);
        Vec3 reach = working.m_82546_(shoulder = Hands.shoulder(!leftWorks, camera, cameraYaw));
        if (reach.m_82553_() > station.maxReach()) {
            working = shoulder.m_82549_(reach.m_82541_().m_82490_(station.maxReach()));
        }
        double idle = Scene.clock();
        Vec3 restRight = station.rest(true).m_82520_(0.0, 0.003 * Math.sin(idle * 1.7), 0.0);
        Vec3 restLeft = station.rest(false).m_82520_(0.0, 0.003 * Math.sin(idle * 1.5 + 2.0), 0.0);
        RIGHT.update(leftWorks ? restRight : working, leftWorks ? 12.0 : 17.0, dt);
        LEFT.update(leftWorks ? working : restLeft, leftWorks ? 17.0 : 12.0, dt);
    }

    static Vec3 position(boolean right) {
        return (right ? RIGHT : LEFT).pos();
    }

    static ItemStack carried() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return player == null ? ItemStack.f_41583_ : player.f_36096_.m_142621_();
    }

    public static boolean itemInHand() {
        ClientStation station = Scene.station;
        return Scene.phase == Scene.Phase.ACTIVE && station != null && station.aim != null && station.handsOverride() == null;
    }

    static Vec3 heldPoint() {
        Camera camera = Minecraft.m_91087_().f_91063_.m_109153_();
        return Scene.station.heldItemPoint(leftWorks ? LEFT.pos() : RIGHT.pos(), Hands.shoulder(!leftWorks, camera.m_90583_(), camera.m_90590_()));
    }

    static void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, float cameraYaw, int light) {
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        ClientStation station = Scene.station;
        if (player == null || station == null || Scene.phase == Scene.Phase.NONE) {
            return;
        }
        Vec3 rightHand = RIGHT.pos();
        Vec3 leftHand = LEFT.pos();
        Hands.drawArm(pose, buffers, camera, rightHand, Hands.shoulder(true, camera, cameraYaw), true, light, station.forward());
        Hands.drawArm(pose, buffers, camera, leftHand, Hands.shoulder(false, camera, cameraYaw), false, light, station.forward());
        ItemStack held = Hands.carried();
        if (!held.m_41619_() && Hands.itemInHand()) {
            Vec3 hand = leftWorks ? leftHand : rightHand;
            Vec3 at = station.heldItemPoint(hand, Hands.shoulder(!leftWorks, camera, cameraYaw));
            if (station instanceof TableStation) {
                TableLayouts.current().renderItem(pose, buffers, camera, held, at, station.sideDirection(), 77L, 1.0f, station.heldItemTilt(), light, 1.0f, true);
            } else {
                float f;
                Direction direction = station.sideDirection();
                if (station instanceof FurnaceStation) {
                    FurnaceStation furnace = (FurnaceStation)station;
                    f = furnace.heldItemScale();
                } else if (station instanceof SurfaceStation) {
                    SurfaceStation surface = (SurfaceStation)station;
                    f = surface.heldItemScale();
                } else if (station instanceof SpatialTarget) {
                    SpatialTarget spatial = (SpatialTarget)((Object)station);
                    f = spatial.layoutHeldScale();
                } else {
                    f = 1.0f;
                }
                ItemPlacer.renderOne(pose, buffers, camera, held, at, direction, 77L, f, station.heldItemTilt(), light);
            }
        }
    }

    static void drawArm(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 hand, Vec3 shoulder, boolean right, int light, Vec3 forward) {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return;
        }
        int style = Settings.armStyle();
        if (YSM_PRESENT) {
            Hands.drawModelArm(pose, buffers, camera, hand, shoulder, right, light, forward, player);
            return;
        }
        if (style == 1) {
            Hands.drawBentArm(pose, buffers, camera, player.m_108560_(), "slim".equals(player.m_108564_()), player.m_36170_(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE), hand, shoulder, right, light, forward);
            return;
        }
        Hands.drawArm(pose, buffers, camera, player.m_108560_(), "slim".equals(player.m_108564_()), player.m_36170_(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE), hand, shoulder, right, light, forward);
    }

    private static void drawModelArm(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 hand, Vec3 shoulder, boolean right, int light, Vec3 forward, LocalPlayer player) {
        MultiBufferSource armBuffers;
        Vec3 up;
        Vec3 upPerp;
        EntityRenderer entityRenderer = Minecraft.m_91087_().m_91290_().m_114382_((Entity)player);
        if (!(entityRenderer instanceof PlayerRenderer)) {
            return;
        }
        PlayerRenderer renderer = (PlayerRenderer)entityRenderer;
        Vec3 d = hand.m_82546_(shoulder);
        if (d.m_82556_() < 1.0E-8) {
            return;
        }
        double length = d.m_82553_() + 0.18;
        d = d.m_82541_();
        int style = Settings.armStyle();
        if (style == 1) {
            length = Math.min(length, 0.7300000119209289);
            shoulder = hand.m_82546_(d.m_82490_(length - 0.18));
        }
        if ((upPerp = (up = new Vec3(0.0, 1.0, 0.0)).m_82546_(d.m_82490_(up.m_82526_(d)))).m_82556_() < 1.0E-6) {
            upPerp = forward;
        }
        upPerp = upPerp.m_82541_();
        Vec3 x = right ? upPerp.m_82490_(-1.0) : upPerp;
        Vec3 z = x.m_82537_(d);
        Quaternionf rotation = new Matrix3f((Vector3fc)new Vector3f((float)x.f_82479_, (float)x.f_82480_, (float)x.f_82481_), (Vector3fc)new Vector3f((float)d.f_82479_, (float)d.f_82480_, (float)d.f_82481_), (Vector3fc)new Vector3f((float)z.f_82479_, (float)z.f_82480_, (float)z.f_82481_)).getNormalizedRotation(new Quaternionf());
        pose.m_85836_();
        pose.m_85837_(shoulder.f_82479_ - camera.f_82479_, shoulder.f_82480_ - camera.f_82480_, shoulder.f_82481_ - camera.f_82481_);
        pose.m_252781_(rotation);
        pose.m_85841_(0.55f, (float)(length * 16.0 / 12.0), 0.55f);
        pose.m_252880_((float)(right ? 5 : -5) / 16.0f, -0.03125f, 0.0f);
        ArrayList clipped = style == 2 ? new ArrayList() : List.of();
        Vec3 clipAxis = d;
        MultiBufferSource multiBufferSource = armBuffers = style == 2 ? type -> {
            NearHandConsumer consumer = new NearHandConsumer(buffers.m_6299_(type), hand.m_82546_(camera), clipAxis, 0.24750000536441805);
            clipped.add(consumer);
            return consumer;
        } : buffers;
        if (right) {
            renderer.m_117770_(pose, armBuffers, light, (AbstractClientPlayer)player);
        } else {
            renderer.m_117813_(pose, armBuffers, light, (AbstractClientPlayer)player);
        }
        for (NearHandConsumer consumer : clipped) {
            consumer.flushPending();
        }
        pose.m_85849_();
    }

    private static void drawArm(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ResourceLocation texture, boolean slim, boolean sleeveShown, Vec3 hand, Vec3 shoulder, boolean right, int light, Vec3 forward) {
        Vec3 d = hand.m_82546_(shoulder);
        double length = d.m_82553_() + 0.18;
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 upPerp = up.m_82546_((d = d.m_82541_()).m_82490_(up.m_82526_(d)));
        if (upPerp.m_82556_() < 1.0E-6) {
            upPerp = forward;
        }
        upPerp = upPerp.m_82541_();
        Vec3 x = right ? upPerp.m_82490_(-1.0) : upPerp;
        Vec3 z = x.m_82537_(d);
        Quaternionf rotation = new Matrix3f((Vector3fc)new Vector3f((float)x.f_82479_, (float)x.f_82480_, (float)x.f_82481_), (Vector3fc)new Vector3f((float)d.f_82479_, (float)d.f_82480_, (float)d.f_82481_), (Vector3fc)new Vector3f((float)z.f_82479_, (float)z.f_82480_, (float)z.f_82481_)).getNormalizedRotation(new Quaternionf());
        float stretch = (float)Math.max(0.6, (length * 16.0 / (double)0.55f - 4.0) / 8.0);
        float centerX = slim ? (right ? -0.5f : 0.5f) : (right ? -1.0f : 1.0f);
        Vec3 pivot = hand.m_82546_(d.m_82490_(0.3437500074505806));
        pose.m_85836_();
        pose.m_85837_(pivot.f_82479_ - camera.f_82479_, pivot.f_82480_ - camera.f_82480_, pivot.f_82481_ - camera.f_82481_);
        pose.m_252781_(rotation);
        pose.m_85841_(0.55f, 0.55f, 0.55f);
        pose.m_252880_(-centerX / 16.0f, 0.0f, 0.0f);
        if (Settings.armStyle() == 2) {
            ArmMesh.arm(right, slim).renderFloating(pose, buffers.m_6299_(RenderType.m_110446_((ResourceLocation)texture)), light, OverlayTexture.f_118083_);
        } else {
            ArmMesh.arm(right, slim).render(pose, buffers.m_6299_(RenderType.m_110446_((ResourceLocation)texture)), light, OverlayTexture.f_118083_, stretch);
        }
        if (sleeveShown) {
            if (Settings.armStyle() == 2) {
                ArmMesh.sleeve(right, slim).renderFloating(pose, buffers.m_6299_(RenderType.m_110473_((ResourceLocation)texture)), light, OverlayTexture.f_118083_);
            } else {
                ArmMesh.sleeve(right, slim).render(pose, buffers.m_6299_(RenderType.m_110473_((ResourceLocation)texture)), light, OverlayTexture.f_118083_, stretch);
            }
        }
        pose.m_85849_();
    }

    private static void drawBentArm(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ResourceLocation texture, boolean slim, boolean sleeveShown, Vec3 hand, Vec3 shoulder, boolean right, int light, Vec3 forward) {
        Vec3 toward = hand.m_82546_(shoulder);
        if (toward.m_82556_() < 0.04) {
            Hands.drawArm(pose, buffers, camera, texture, slim, sleeveShown, hand, shoulder, right, light, forward);
            return;
        }
        Vec3[] points = Hands.bentPoints(shoulder, hand, right, forward);
        Hands.drawBentPiece(pose, buffers, camera, texture, slim, sleeveShown, points[0], points[1], right, light, forward, false);
        Hands.drawBentPiece(pose, buffers, camera, texture, slim, sleeveShown, points[1], points[2], right, light, forward, true);
    }

    private static Vec3[] bentPoints(Vec3 shoulder, Vec3 hand, boolean right, Vec3 forward) {
        Vec3 toward = hand.m_82546_(shoulder);
        double span = toward.m_82553_();
        Vec3 axis = toward.m_82490_(1.0 / span);
        Vec3 side = new Vec3(-forward.f_82481_, 0.0, forward.f_82479_).m_82490_(right ? 1.0 : -1.0);
        if ((side = side.m_82546_(axis.m_82490_(side.m_82526_(axis)))).m_82556_() < 1.0E-6) {
            side = forward.m_82546_(axis.m_82490_(forward.m_82526_(axis)));
        }
        if (side.m_82556_() < 1.0E-6) {
            side = new Vec3(1.0, 0.0, 0.0);
        }
        side = side.m_82541_();
        double upper = 0.275f;
        double forearm = 0.4125000089406967;
        if (span >= upper + forearm) {
            Vec3 elbow = hand.m_82546_(axis.m_82490_(forearm)).m_82549_(side.m_82490_(0.035));
            return new Vec3[]{shoulder, elbow, hand};
        }
        if (span <= forearm - upper) {
            return new Vec3[]{shoulder, shoulder.m_82549_(axis.m_82490_(span * 0.4)).m_82549_(side.m_82490_(0.06)), hand};
        }
        double along = (span * span + upper * upper - forearm * forearm) / (2.0 * span);
        double bend = Math.sqrt(Math.max(0.0, upper * upper - along * along));
        return new Vec3[]{shoulder, shoulder.m_82549_(axis.m_82490_(along)).m_82549_(side.m_82490_(bend)), hand};
    }

    private static void drawBentPiece(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ResourceLocation texture, boolean slim, boolean sleeveShown, Vec3 start, Vec3 end, boolean right, int light, Vec3 forward, boolean forearm) {
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 axis = end.m_82546_(start).m_82541_();
        Vec3 upPerp = up.m_82546_(axis.m_82490_(up.m_82526_(axis)));
        if (upPerp.m_82556_() < 1.0E-6) {
            upPerp = forward;
        }
        upPerp = upPerp.m_82541_();
        Vec3 x = right ? upPerp.m_82490_(-1.0) : upPerp;
        Vec3 z = x.m_82537_(axis);
        Quaternionf rotation = new Matrix3f((Vector3fc)new Vector3f((float)x.f_82479_, (float)x.f_82480_, (float)x.f_82481_), (Vector3fc)new Vector3f((float)axis.f_82479_, (float)axis.f_82480_, (float)axis.f_82481_), (Vector3fc)new Vector3f((float)z.f_82479_, (float)z.f_82480_, (float)z.f_82481_)).getNormalizedRotation(new Quaternionf());
        float centerX = slim ? (right ? -0.5f : 0.5f) : (right ? -1.0f : 1.0f);
        float modelLength = forearm ? 12.0f : 8.0f;
        float lengthScale = (float)(end.m_82554_(start) * 16.0 / (double)modelLength);
        Vec3 origin = start.m_82549_(axis.m_82490_(0.125 * (double)lengthScale));
        pose.m_85836_();
        pose.m_85837_(origin.f_82479_ - camera.f_82479_, origin.f_82480_ - camera.f_82480_, origin.f_82481_ - camera.f_82481_);
        pose.m_252781_(rotation);
        pose.m_85841_(0.55f, lengthScale, 0.55f);
        pose.m_252880_(-centerX / 16.0f, 0.0f, 0.0f);
        VertexConsumer solid = buffers.m_6299_(RenderType.m_110446_((ResourceLocation)texture));
        if (forearm) {
            ArmMesh.arm(right, slim).renderForearm(pose, solid, light, OverlayTexture.f_118083_);
        } else {
            ArmMesh.arm(right, slim).renderUpper(pose, solid, light, OverlayTexture.f_118083_);
        }
        if (sleeveShown) {
            VertexConsumer translucent = buffers.m_6299_(RenderType.m_110473_((ResourceLocation)texture));
            if (forearm) {
                ArmMesh.sleeve(right, slim).renderForearm(pose, translucent, light, OverlayTexture.f_118083_);
            } else {
                ArmMesh.sleeve(right, slim).renderUpper(pose, translucent, light, OverlayTexture.f_118083_);
            }
        }
        pose.m_85849_();
    }

    static {
        tapAt = -10.0;
    }

    private static final class NearHandConsumer
    implements VertexConsumer {
        private final VertexConsumer out;
        private final Vec3 hand;
        private final Vec3 axis;
        private final double length;
        private final Vertex[] quad = new Vertex[]{new Vertex(), new Vertex(), new Vertex(), new Vertex()};
        private int count;

        private NearHandConsumer(VertexConsumer out, Vec3 hand, Vec3 axis, double length) {
            this.out = out;
            this.hand = hand;
            this.axis = axis;
            this.length = length;
        }

        public VertexConsumer m_5483_(double x, double y, double z) {
            if (this.count == 4) {
                this.flush();
            }
            Vertex vertex = this.quad[this.count];
            vertex.x = (float)x;
            vertex.y = (float)y;
            vertex.z = (float)z;
            vertex.color = -1;
            vertex.v = 0.0f;
            vertex.u = 0.0f;
            vertex.lightV = 0;
            vertex.lightU = 0;
            vertex.overlayV = 0;
            vertex.overlayU = 0;
            vertex.nz = 0.0f;
            vertex.ny = 0.0f;
            vertex.nx = 0.0f;
            ++this.count;
            return this;
        }

        public void m_5752_() {
            if (this.count == 4) {
                this.flush();
            }
        }

        public void m_7404_(int red, int green, int blue, int alpha) {
            this.out.m_7404_(red, green, blue, alpha);
        }

        public void m_141991_() {
            this.out.m_141991_();
        }

        private Vertex current() {
            return this.quad[Math.max(0, this.count - 1)];
        }

        public VertexConsumer m_6122_(int red, int green, int blue, int alpha) {
            this.current().color = alpha << 24 | red << 16 | green << 8 | blue;
            return this;
        }

        public VertexConsumer m_7421_(float u, float v) {
            this.current().u = u;
            this.current().v = v;
            return this;
        }

        public VertexConsumer m_7122_(int u, int v) {
            this.current().overlayU = u;
            this.current().overlayV = v;
            return this;
        }

        public VertexConsumer m_7120_(int u, int v) {
            this.current().lightU = u;
            this.current().lightV = v;
            return this;
        }

        public VertexConsumer m_5601_(float x, float y, float z) {
            this.current().nx = x;
            this.current().ny = y;
            this.current().nz = z;
            return this;
        }

        private void flushPending() {
            if (this.count == 4) {
                this.flush();
            }
        }

        private void flush() {
            this.count = 0;
            ArrayList<Vertex> polygon = new ArrayList<Vertex>(6);
            Vertex previous = this.quad[3];
            double previousDistance = this.distance(previous);
            Vertex[] vertexArray = this.quad;
            int n = vertexArray.length;
            for (int i = 0; i < n; ++i) {
                Vertex current;
                double currentDistance;
                if (previousDistance >= 0.0 != (currentDistance = this.distance(current = vertexArray[i])) >= 0.0) {
                    polygon.add(Vertex.between(previous, current, (float)(previousDistance / (previousDistance - currentDistance))));
                }
                if (currentDistance >= 0.0) {
                    polygon.add(current);
                }
                previous = current;
                previousDistance = currentDistance;
            }
            if (polygon.size() < 3) {
                return;
            }
            int i = 1;
            while (i + 1 < polygon.size()) {
                this.emit((Vertex)polygon.get(0));
                this.emit((Vertex)polygon.get(i));
                this.emit((Vertex)polygon.get(i + 1));
                this.emit((Vertex)polygon.get(i + 1));
                ++i;
            }
        }

        private double distance(Vertex v) {
            return ((double)v.x - this.hand.f_82479_) * this.axis.f_82479_ + ((double)v.y - this.hand.f_82480_) * this.axis.f_82480_ + ((double)v.z - this.hand.f_82481_) * this.axis.f_82481_ + this.length;
        }

        private void emit(Vertex v) {
            this.out.m_5483_((double)v.x, (double)v.y, (double)v.z).m_193479_(v.color).m_7421_(v.u, v.v).m_7122_(v.overlayU, v.overlayV).m_7120_(v.lightU, v.lightV).m_5601_(v.nx, v.ny, v.nz).m_5752_();
        }

        private static final class Vertex {
            float x;
            float y;
            float z;
            float u;
            float v;
            float nx;
            float ny;
            float nz;
            int color;
            int overlayU;
            int overlayV;
            int lightU;
            int lightV;

            private Vertex() {
            }

            static Vertex between(Vertex a, Vertex b, float t) {
                Vertex v = new Vertex();
                v.x = Vertex.mix(a.x, b.x, t);
                v.y = Vertex.mix(a.y, b.y, t);
                v.z = Vertex.mix(a.z, b.z, t);
                v.u = Vertex.mix(a.u, b.u, t);
                v.v = Vertex.mix(a.v, b.v, t);
                v.nx = Vertex.mix(a.nx, b.nx, t);
                v.ny = Vertex.mix(a.ny, b.ny, t);
                v.nz = Vertex.mix(a.nz, b.nz, t);
                v.overlayU = a.overlayU;
                v.overlayV = a.overlayV;
                v.lightU = a.lightU;
                v.lightV = a.lightV;
                int ac = a.color;
                int bc = b.color;
                v.color = (int)Vertex.mix(ac >>> 24 & 0xFF, bc >>> 24 & 0xFF, t) << 24 | (int)Vertex.mix(ac >>> 16 & 0xFF, bc >>> 16 & 0xFF, t) << 16 | (int)Vertex.mix(ac >>> 8 & 0xFF, bc >>> 8 & 0xFF, t) << 8 | (int)Vertex.mix(ac & 0xFF, bc & 0xFF, t);
                return v;
            }

            private static float mix(float a, float b, float t) {
                return a + (b - a) * t;
            }
        }
    }
}

