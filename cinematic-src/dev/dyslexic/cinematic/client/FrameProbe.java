/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Camera
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.Scene;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class FrameProbe {
    private static final List<double[]> FRAMES = new ArrayList<double[]>();
    private static String segment;
    private static int afterShot;

    private FrameProbe() {
    }

    static void shotTaken() {
        afterShot = 2;
    }

    static void sample(Camera camera) {
        String key;
        String string = key = Scene.active() && Scene.station != null ? String.valueOf((Object)Scene.station.kind()) + " " + String.valueOf((Object)Scene.phase) : null;
        if (!Objects.equals(key, segment)) {
            FrameProbe.report();
            FRAMES.clear();
            segment = key;
        }
        if (key == null) {
            return;
        }
        Vec3 c = camera.m_90583_();
        Vec3 r = Hands.position(true);
        Vec3 l = Hands.position(false);
        FRAMES.add(new double[]{Scene.clock(), afterShot-- > 0 ? Double.NaN : (double)System.nanoTime() / 1.0E9, c.f_82479_, c.f_82480_, c.f_82481_, camera.m_90590_(), camera.m_90589_(), r.f_82479_, r.f_82480_, r.f_82481_, l.f_82479_, l.f_82480_, l.f_82481_});
    }

    private static void report() {
        int n = FRAMES.size();
        if (segment == null || n < 12) {
            return;
        }
        double[] dt = IntStream.range(1, n).mapToDouble(i -> FRAMES.get(i)[1] - FRAMES.get(i - 1)[1]).filter(d -> !Double.isNaN(d)).toArray();
        double mean = Arrays.stream(dt).average().orElse(0.0);
        double sd = Math.sqrt(Arrays.stream(dt).map(d -> (d - mean) * (d - mean)).average().orElse(0.0));
        double max = Arrays.stream(dt).max().orElse(0.0);
        Cinematic.LOGGER.info("[Probe] {} frames={} time={} dt={}\u00b1{}ms max={}ms | camera {} | turn {} | right hand {} | left hand {}", new Object[]{segment, n, String.format("%.2fs", FRAMES.get(n - 1)[0] - FRAMES.get(0)[0]), String.format("%.1f", mean * 1000.0), String.format("%.1f", sd * 1000.0), String.format("%.0f", max * 1000.0), FrameProbe.evenness(2, false), FrameProbe.evenness(5, true), FrameProbe.evenness(7, false), FrameProbe.evenness(10, false)});
    }

    private static String evenness(int at, boolean angles) {
        int n = FRAMES.size();
        double[] speed = new double[n - 1];
        for (int i = 1; i < n; ++i) {
            double[] a = FRAMES.get(i - 1);
            double[] b = FRAMES.get(i);
            double step = b[0] - a[0];
            double d = angles ? Math.hypot(Mth.m_14175_((double)(b[at] - a[at])), b[at + 1] - a[at + 1]) : Math.sqrt(FrameProbe.sq(b[at] - a[at]) + FrameProbe.sq(b[at + 1] - a[at + 1]) + FrameProbe.sq(b[at + 2] - a[at + 2]));
            speed[i - 1] = step > 1.0E-6 && !Double.isNaN(a[1]) && !Double.isNaN(b[1]) ? d / step : -1.0;
        }
        double usual = Arrays.stream(speed).filter(s -> s >= 0.0).average().orElse(0.0);
        double d = angles ? 2.0 : 0.02;
        if (usual < d) {
            return "still";
        }
        double uneven = 0.0;
        int counted = 0;
        int stalls = 0;
        for (int i = 2; i < speed.length - 2; ++i) {
            double[] around = new double[]{speed[i - 2], speed[i - 1], speed[i + 1], speed[i + 2]};
            if (speed[i] < 0.0 || Arrays.stream(around).anyMatch(s -> s < 0.0)) continue;
            Arrays.sort(around);
            double local = (around[1] + around[2]) / 2.0;
            uneven += Math.abs(speed[i] - local) / usual;
            ++counted;
            if (!(local > usual * 0.5) || !(speed[i] < local * 0.25)) continue;
            ++stalls;
        }
        return counted == 0 ? "-" : String.format("uneven %.1f%% stalls %d", 100.0 * uneven / (double)counted, stalls);
    }

    private static double sq(double v) {
        return v * v;
    }
}

