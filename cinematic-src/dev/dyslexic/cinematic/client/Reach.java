/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.CampfireStation;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.Settings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Reach {
    static final double SURFACE_HIGH = 1.42;
    static final double FACE_HIGH = 1.85;
    static final double LOW = -0.6;
    static final double FAR = 1.5;
    static final double FURTHER_OUT = 0.9;
    static final int ROUTE_RADIUS = 10;
    static final double[] BESIDE_ANGLES = new double[]{55.0, 40.0, 70.0};
    static final double[] BESIDE_REACH = new double[]{0.95, 1.1, 1.25, 1.4};
    static final double HARM_MARGIN = 0.12;
    static final double CAMERA_AHEAD = 1.05;
    static final double CAMERA_DOWN = 0.95;
    static final double CAMERA_UP = 0.16;
    static final double LEAN_OVER = 1.2;
    static final double LEAN_OVER_GAP = 0.45;
    static final double LEAN_OVER_RISE = 0.35;

    private Reach() {
    }

    public static Plan plan(ClientStation station, LocalPlayer player) {
        Level level = player.m_9236_();
        List<Vec3> usual = station.approach(player);
        Plan there = null;
        if ((usual.isEmpty() || Reach.walkable(level, (Entity)player, usual)) && (there = Reach.judge(station, player, usual, false)).scene()) {
            return there;
        }
        if (!(station instanceof CampfireStation)) {
            Vec3 stand = usual.isEmpty() ? player.m_20182_() : usual.get(usual.size() - 1);
            Routes routes = null;
            for (Vec3 feet : Reach.candidates(station, stand)) {
                List<Vec3> path;
                if (!Reach.firm(level, (Entity)player, feet) || !Reach.judge(station, player, List.of(feet), false).scene()) continue;
                if (routes == null) {
                    routes = new Routes(level, (Entity)player);
                }
                if ((path = routes.to(feet)) == null) continue;
                return new Plan(true, path, feet, "", null, null);
            }
        }
        return there != null ? there : Reach.judge(station, player, List.of(), true);
    }

    public static Plan planHere(ClientStation station, LocalPlayer player) {
        Plan plan = Reach.judge(station, player, List.of(), false);
        if (!plan.scene()) {
            return plan;
        }
        Vec3 eye = player.m_146892_();
        AABB own = new AABB(station.pos);
        for (Vec3 point : station.workSamples()) {
            BlockHitResult hit = player.m_9236_().m_45547_(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)player));
            if (hit.m_6662_() == HitResult.Type.MISS || own.m_82390_(point) && hit.m_82425_().equals((Object)station.pos)) continue;
            return new Plan(false, List.of(), player.m_20182_(), "work hidden from the real eye position", Reason.COVERED, (Component)player.m_9236_().m_8055_(hit.m_82425_()).m_60734_().m_49954_());
        }
        return plan;
    }

    public static Plan planBeside(ClientStation station, LocalPlayer player) {
        Level level = player.m_9236_();
        Vec3 work = station.workPoint();
        Vec3 base = new Vec3(work.f_82479_, player.m_20186_(), work.f_82481_);
        Routes routes = null;
        for (double angle : BESIDE_ANGLES) {
            for (double reach : BESIDE_REACH) {
                for (double sign : new double[]{1.0, -1.0}) {
                    List<Vec3> path;
                    double swing = angle * sign;
                    Vec3 feet = base.m_82549_(ClientStation.turned(station.side(), swing).m_82490_(reach));
                    station.swing = swing;
                    if (!Reach.firm(level, (Entity)player, feet) || !Reach.judge(station, player, List.of(feet), false).scene()) continue;
                    if (routes == null) {
                        routes = new Routes(level, (Entity)player);
                    }
                    if ((path = routes.to(feet)) == null) continue;
                    return new Plan(true, path, feet, "", null, null);
                }
            }
        }
        station.swing = 0.0;
        return Reach.judge(station, player, List.of(), true);
    }

    private static Plan judge(ClientStation station, LocalPlayer player, List<Vec3> path, boolean stayed) {
        Object why;
        Vec3 feet = path.isEmpty() ? player.m_20182_() : path.get(path.size() - 1);
        Vec3 work = station.workPoint();
        double high = work.f_82480_ - feet.f_82480_;
        Reason reason = null;
        MutableComponent blocker = null;
        if (!station.workable(feet)) {
            why = String.format("the work is out of reach: %.2f over the feet, %.2f away", high, Math.hypot(work.f_82479_ - feet.f_82479_, work.f_82481_ - feet.f_82481_));
            reason = station instanceof CampfireStation ? (stayed ? Reason.NO_WAY : Reason.NO_SEAT) : (high > (station.workOnTop() ? 1.42 : 1.85) ? Reason.TOO_HIGH : (high < -0.6 ? Reason.TOO_LOW : (stayed ? Reason.NO_WAY : Reason.TOO_FAR)));
        } else if (Reach.standingOn(station.pos, feet)) {
            why = "standing on the block itself";
            reason = Reason.ON_TOP;
        } else if (!station.inFront(feet)) {
            why = "standing beside or behind the side it is worked from (" + station.sideDirection().m_122433_() + ")";
            reason = Reason.WRONG_SIDE;
        } else {
            BlockHitResult hit = Reach.hidden(station, player, feet);
            if (hit == null) {
                why = "";
            } else {
                BlockPos at = hit.m_82425_();
                why = String.format("the work zone is not in view: %s at %s is in the way", player.m_9236_().m_8055_(at).m_60734_().m_49954_().getString(), at.m_123344_());
                reason = at.equals((Object)station.pos) ? Reason.WRONG_SIDE : Reason.COVERED;
                blocker = player.m_9236_().m_8055_(at).m_60734_().m_49954_();
            }
        }
        if (stayed && !((String)why).isEmpty()) {
            why = "no safe way to the usual place, staying put; " + (String)why;
            if (reason == Reason.WRONG_SIDE) {
                reason = Reason.NO_WAY;
            }
        }
        return new Plan(((String)why).isEmpty(), path, feet, (String)why, reason, (Component)blocker);
    }

    private static List<Vec3> candidates(ClientStation station, Vec3 stand) {
        Vec3 out = station.side();
        Vec3 across = station.right();
        ArrayList<Vec3> list = new ArrayList<Vec3>();
        for (double o = 0.0; o <= 0.900001; o += 0.15) {
            for (double a : new double[]{0.0, -0.25, 0.25}) {
                list.add(stand.m_82549_(out.m_82490_(o)).m_82549_(across.m_82490_(a)));
            }
        }
        list.sort(Comparator.comparingDouble(p -> p.m_82557_(stand)));
        return list;
    }

    static boolean firm(Level level, Entity player, Vec3 feet) {
        AABB under = new AABB(feet.f_82479_ - 0.1, feet.f_82480_ - 0.55, feet.f_82481_ - 0.1, feet.f_82479_ + 0.1, feet.f_82480_ + 0.01, feet.f_82481_ + 0.1);
        return Reach.standable(level, player, feet) && !level.m_45756_(player, under);
    }

    static boolean standingOn(BlockPos pos, Vec3 feet) {
        return Math.abs(feet.f_82479_ - ((double)pos.m_123341_() + 0.5)) < 0.75 && Math.abs(feet.f_82481_ - ((double)pos.m_123343_() + 0.5)) < 0.75 && feet.f_82480_ > (double)pos.m_123342_() + 0.4;
    }

    @Nullable
    static BlockHitResult hidden(ClientStation station, LocalPlayer player, Vec3 feet) {
        CameraRig.Pose lean = station.view();
        double height = station.crouchesFor(lean.pos().f_82480_ - feet.f_82480_) ? 1.27 : 1.62;
        Vec3 from = Reach.cameraPose(station, lean, feet.m_82520_(0.0, height, 0.0)).pos();
        AABB own = new AABB(station.pos);
        for (Vec3 to : station.workSamples()) {
            BlockHitResult hit = player.m_9236_().m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)player));
            if (hit.m_6662_() == HitResult.Type.MISS || own.m_82390_(to) && hit.m_82425_().equals((Object)station.pos)) continue;
            return hit;
        }
        return null;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    static boolean inReach(Vec3 work, boolean surface, Vec3 feet) {
        double h = work.f_82480_ - feet.f_82480_;
        if (!(h >= -0.6)) return false;
        double d = surface ? 1.42 : 1.85;
        if (!(h <= d)) return false;
        if (!(Math.hypot(work.f_82479_ - feet.f_82479_, work.f_82481_ - feet.f_82481_) <= 1.5)) return false;
        return true;
    }

    static boolean walkable(Level level, Entity player, List<Vec3> path) {
        Vec3 from = player.m_20182_();
        for (Vec3 to : path) {
            if (!Reach.clear(level, player, from, to)) {
                return false;
            }
            from = to;
        }
        return true;
    }

    static boolean clear(Level level, Entity player, Vec3 from, Vec3 to) {
        double length = Math.hypot(to.f_82479_ - from.f_82479_, to.f_82481_ - from.f_82481_);
        int steps = Math.max(1, (int)Math.ceil(length / 0.2));
        for (int i = 1; i <= steps; ++i) {
            if (Reach.standable(level, player, Anim.lerp(from, to, (double)i / (double)steps))) continue;
            return false;
        }
        return true;
    }

    static boolean standable(Level level, Entity player, Vec3 feet) {
        double w = (double)(player.m_20205_() / 2.0f) - 0.02;
        AABB body = new AABB(feet.f_82479_ - w, feet.f_82480_ + 0.6, feet.f_82481_ - w, feet.f_82479_ + w, feet.f_82480_ + (double)player.m_20206_(), feet.f_82481_ + w);
        AABB under = new AABB(feet.f_82479_ - w, feet.f_82480_ - 0.55, feet.f_82481_ - w, feet.f_82479_ + w, feet.f_82480_ + 0.01, feet.f_82481_ + w);
        if (!level.m_45756_(player, body) || level.m_45756_(player, under)) {
            return false;
        }
        double m = w + 0.12;
        for (BlockPos at : BlockPos.m_121940_((BlockPos)BlockPos.m_274561_((double)(feet.f_82479_ - m), (double)(feet.f_82480_ - 0.5), (double)(feet.f_82481_ - m)), (BlockPos)BlockPos.m_274561_((double)(feet.f_82479_ + m), (double)(feet.f_82480_ + 1.1), (double)(feet.f_82481_ + m)))) {
            if (Reach.harmless(level.m_8055_(at))) continue;
            return false;
        }
        return true;
    }

    private static boolean harmless(BlockState state) {
        return !state.m_204336_(BlockTags.f_13076_) && !state.m_204336_(BlockTags.f_13087_) && !state.m_60713_(Blocks.f_50450_) && !state.m_60713_(Blocks.f_50128_) && !state.m_60713_(Blocks.f_50685_) && !state.m_60713_(Blocks.f_152499_) && !state.m_60713_(Blocks.f_50070_) && !state.m_60819_().m_205070_(FluidTags.f_13132_);
    }

    static CameraRig.Pose tethered(CameraRig.Pose pose, Vec3 eye, Vec3 aimedAt) {
        double ahead;
        double scale;
        Vec3 off = pose.pos().m_82546_(eye);
        double ox = off.f_82479_;
        double oz = off.f_82481_;
        double dy = Mth.m_14008_((double)off.f_82480_, (double)-0.95, (double)0.16);
        double tooLow = -0.95 - off.f_82480_;
        if (tooLow > 0.0) {
            double tx = aimedAt.f_82479_ - pose.pos().f_82479_;
            double tz = aimedAt.f_82481_ - pose.pos().f_82481_;
            double toward = Math.hypot(tx, tz);
            double move = Math.min(tooLow * 1.2, toward - 0.45);
            if (move > 0.0) {
                ox += tx / toward * move;
                oz += tz / toward * move;
            }
            dy += Math.min(tooLow, 0.35);
        }
        double d = scale = (ahead = Math.hypot(ox, oz)) > 1.05 ? 1.05 / ahead : 1.0;
        if (scale == 1.0 && dy == off.f_82480_) {
            return pose;
        }
        Vec3 at = eye.m_82520_(ox * scale, dy, oz * scale);
        Vec3 look = aimedAt.m_82546_(at);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(at, yaw, pitch, pose.roll());
    }

    static CameraRig.Pose cameraPose(ClientStation station, CameraRig.Pose pose, Vec3 eye) {
        Vec3 aim = Reach.lookPoint(pose, station.center());
        CameraRig.Pose held = station.freeCamera() ? pose : Reach.tethered(pose, eye, aim);
        double strength = Settings.leanStrength();
        if (strength >= 0.999) {
            return held;
        }
        Vec3 at = Anim.lerp(eye, held.pos(), strength);
        Vec3 look = aim.m_82546_(at);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(at, yaw, pitch, held.roll() * (float)strength);
    }

    static Vec3 lookPoint(CameraRig.Pose pose, Vec3 station) {
        double distance = Math.max(0.3, pose.pos().m_82554_(station));
        return pose.pos().m_82549_(Vec3.m_82498_((float)pose.pitch(), (float)pose.yaw()).m_82490_(distance));
    }

    public record Plan(boolean scene, List<Vec3> path, Vec3 feet, String why, @Nullable Reason reason, @Nullable Component blocker) {
    }

    private static final class Routes {
        private final Level level;
        private final Entity player;
        private final Vec3 start;
        private final BlockPos origin;
        private final Map<Long, Long> from = new HashMap<Long, Long>();
        private final ArrayDeque<BlockPos> open = new ArrayDeque();

        Routes(Level level, Entity player) {
            this.level = level;
            this.player = player;
            this.start = player.m_20182_();
            this.origin = BlockPos.m_274561_((double)this.start.f_82479_, (double)(this.start.f_82480_ + 0.1), (double)this.start.f_82481_);
            this.from.put(this.origin.m_121878_(), this.origin.m_121878_());
            this.open.add(this.origin);
        }

        @Nullable
        List<Vec3> to(Vec3 goal) {
            if (Reach.clear(this.level, this.player, this.start, goal)) {
                return List.of(goal);
            }
            BlockPos goalBlock = BlockPos.m_274561_((double)goal.f_82479_, (double)((double)this.origin.m_123342_() + 0.1), (double)goal.f_82481_);
            while (!this.from.containsKey(goalBlock.m_121878_()) && !this.open.isEmpty()) {
                BlockPos at = this.open.poll();
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        if (dx == 0 && dz == 0 || !this.step(at, dx, dz)) continue;
                        BlockPos next = at.m_7918_(dx, 0, dz);
                        this.from.put(next.m_121878_(), at.m_121878_());
                        this.open.add(next);
                    }
                }
            }
            if (!this.from.containsKey(goalBlock.m_121878_())) {
                return null;
            }
            ArrayList<Vec3> points = new ArrayList<Vec3>();
            points.add(goal);
            long at = this.from.get(goalBlock.m_121878_());
            while (at != this.origin.m_121878_()) {
                points.add(this.middle(BlockPos.m_122022_((long)at)));
                at = this.from.get(at);
            }
            points.add(this.start);
            Collections.reverse(points);
            ArrayList<Vec3> path = new ArrayList<Vec3>();
            int i = 0;
            while (i < points.size() - 1) {
                int j;
                for (j = points.size() - 1; j > i + 1 && !Reach.clear(this.level, this.player, (Vec3)points.get(i), (Vec3)points.get(j)); --j) {
                }
                if (!Reach.clear(this.level, this.player, (Vec3)points.get(i), (Vec3)points.get(j))) {
                    return null;
                }
                path.add((Vec3)points.get(j));
                i = j;
            }
            return path;
        }

        private boolean step(BlockPos at, int dx, int dz) {
            BlockPos next = at.m_7918_(dx, 0, dz);
            if (this.from.containsKey(next.m_121878_()) || Math.abs(next.m_123341_() - this.origin.m_123341_()) > 10 || Math.abs(next.m_123343_() - this.origin.m_123343_()) > 10 || !Reach.standable(this.level, this.player, this.middle(next))) {
                return false;
            }
            return dx == 0 || dz == 0 || Reach.standable(this.level, this.player, this.middle(at.m_7918_(dx, 0, 0))) && Reach.standable(this.level, this.player, this.middle(at.m_7918_(0, 0, dz)));
        }

        private Vec3 middle(BlockPos block) {
            return new Vec3((double)block.m_123341_() + 0.5, this.start.f_82480_, (double)block.m_123343_() + 0.5);
        }
    }

    public static enum Reason {
        TOO_HIGH,
        TOO_LOW,
        TOO_FAR,
        NO_WAY,
        ON_TOP,
        WRONG_SIDE,
        COVERED,
        BLOCKED,
        NO_SEAT;

    }
}

