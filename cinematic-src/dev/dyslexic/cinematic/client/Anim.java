/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import net.minecraft.world.phys.Vec3;

public final class Anim {
    private Anim() {
    }

    public static double clamp01(double t) {
        return t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t);
    }

    public static double window(double t, double a, double b) {
        return Anim.clamp01((t - a) / (b - a));
    }

    public static double smoother(double t) {
        t = Anim.clamp01(t);
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    public static double inOutCubic(double t) {
        return (t = Anim.clamp01(t)) < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
    }

    public static double inOutSine(double t) {
        return -(Math.cos(Math.PI * Anim.clamp01(t)) - 1.0) / 2.0;
    }

    public static double outCubic(double t) {
        t = Anim.clamp01(t);
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    public static double inCubic(double t) {
        t = Anim.clamp01(t);
        return t * t * t;
    }

    public static double outBack(double t, double overshoot) {
        t = Anim.clamp01(t);
        double c3 = overshoot + 1.0;
        return 1.0 + c3 * Math.pow(t - 1.0, 3.0) + overshoot * Math.pow(t - 1.0, 2.0);
    }

    public static double hump(double t) {
        return Math.sin(Math.PI * Anim.clamp01(t));
    }

    public static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return new Vec3(a.f_82479_ + (b.f_82479_ - a.f_82479_) * t, a.f_82480_ + (b.f_82480_ - a.f_82480_) * t, a.f_82481_ + (b.f_82481_ - a.f_82481_) * t);
    }

    public static double approach(double current, double target, double rate, double dt) {
        return target + (current - target) * Math.exp(-rate * dt);
    }

    public static final class Spring3 {
        public double x;
        public double y;
        public double z;
        private double vx;
        private double vy;
        private double vz;

        public void snap(Vec3 p) {
            this.x = p.f_82479_;
            this.y = p.f_82480_;
            this.z = p.f_82481_;
            this.vz = 0.0;
            this.vy = 0.0;
            this.vx = 0.0;
        }

        public Vec3 pos() {
            return new Vec3(this.x, this.y, this.z);
        }

        public double speed() {
            return Math.sqrt(this.vx * this.vx + this.vy * this.vy + this.vz * this.vz);
        }

        public void update(Vec3 target, double omega, double dt) {
            double e = Math.exp(-omega * dt);
            double dx = this.x - target.f_82479_;
            double tx = (this.vx + omega * dx) * dt;
            this.vx = (this.vx - omega * tx) * e;
            this.x = target.f_82479_ + (dx + tx) * e;
            double dy = this.y - target.f_82480_;
            double ty = (this.vy + omega * dy) * dt;
            this.vy = (this.vy - omega * ty) * e;
            this.y = target.f_82480_ + (dy + ty) * e;
            double dz = this.z - target.f_82481_;
            double tz = (this.vz + omega * dz) * dt;
            this.vz = (this.vz - omega * tz) * e;
            this.z = target.f_82481_ + (dz + tz) * e;
        }
    }
}

