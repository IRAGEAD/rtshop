/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.furnace.FurnaceLayout;
import dev.dyslexic.cinematic.furnace.FurnaceZones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

record FurnaceAppearance(Region input, Region fuel, Region result, Region cooking, Region burning) {
    static final FurnaceAppearance VANILLA = new FurnaceAppearance(Region.VANILLA, Region.VANILLA, Region.VANILLA, Region.VANILLA, Region.VANILLA);

    FurnaceAppearance {
        input = input == null ? Region.VANILLA : input;
        fuel = fuel == null ? Region.VANILLA : fuel;
        result = result == null ? Region.VANILLA : result;
        cooking = cooking == null ? Region.VANILLA : cooking;
        burning = burning == null ? Region.VANILLA : burning;
    }

    private static double finite(double value, double fallback, double min, double max) {
        return Mth.m_14008_((double)(Double.isFinite(value) ? value : fallback), (double)min, (double)max);
    }

    Region region(int index) {
        return switch (index) {
            case 0 -> this.input;
            case 1 -> this.fuel;
            case 2 -> this.result;
            case 3 -> this.cooking;
            case 4 -> this.burning;
            default -> throw new IllegalArgumentException("Furnace region " + index);
        };
    }

    FurnaceAppearance with(int index, Region value) {
        return new FurnaceAppearance(index == 0 ? value : this.input, index == 1 ? value : this.fuel, index == 2 ? value : this.result, index == 3 ? value : this.cooking, index == 4 ? value : this.burning);
    }

    FurnaceAppearance blend(FurnaceAppearance target, double t) {
        return new FurnaceAppearance(this.input.blend(target.input, t), this.fuel.blend(target.fuel, t), this.result.blend(target.result, t), this.cooking.blend(target.cooking, t), this.burning.blend(target.burning, t));
    }

    FurnaceZones.Face face(FurnaceLayout layout, int region) {
        return region < 3 ? layout.zone(region).face() : FurnaceZones.Face.FRONT;
    }

    double[] original(FurnaceLayout layout, int region) {
        return region < 3 ? layout.zone(region).highlight() : (region == 3 ? layout.cookGauge : layout.burnGauge);
    }

    private double[] transform(FurnaceLayout layout, int index) {
        double[] rect = this.original(layout, index);
        Region r = this.region(index);
        double cx = (rect[0] + rect[1]) / 2.0;
        double cy = (rect[2] + rect[3]) / 2.0;
        double w = Math.min(r.width, 0.96 / (rect[1] - rect[0]));
        double h = Math.min(r.height, 0.96 / (rect[3] - rect[2]));
        double hw = (rect[1] - rect[0]) * w / 2.0;
        double hh = (rect[3] - rect[2]) * h / 2.0;
        double u = Mth.m_14008_((double)(cx + r.u / 16.0), (double)(0.02 + hw), (double)(0.98 - hw));
        double v = Mth.m_14008_((double)(cy + r.v / 16.0), (double)(0.02 + hh), (double)(0.98 - hh));
        if (r.u == 0.0 && r.width == 1.0) {
            u = cx;
        }
        if (r.v == 0.0 && r.height == 1.0) {
            v = cy;
        }
        return new double[]{cx, cy, u, v, w, h};
    }

    double[] uv(FurnaceLayout layout, int index, double u, double v, boolean item) {
        if (this.region(index).equals(Region.VANILLA)) {
            return new double[]{u, v};
        }
        double[] t = this.transform(layout, index);
        Region r = this.region(index);
        return new double[]{t[2] + (u - t[0]) * t[4] + (item ? r.itemU / 16.0 : 0.0), t[3] + (v - t[1]) * t[5] + (item ? r.itemV / 16.0 : 0.0)};
    }

    double[] rect(FurnaceLayout layout, int index, boolean click) {
        double[] source = this.original(layout, index);
        if (!click && this.region(index).equals(Region.VANILLA)) {
            return source;
        }
        if (click && index < 3) {
            FurnaceLayout.Zone z = layout.zone(index);
            source = new double[]{z.u0(), z.u1(), z.v0(), z.v1()};
        }
        double[] low = this.uv(layout, index, source[0], source[2], false);
        double[] high = this.uv(layout, index, source[1], source[3], false);
        if (click) {
            Region r = this.region(index);
            double cx = (low[0] + high[0]) / 2.0;
            double cy = (low[1] + high[1]) / 2.0;
            double hw = (high[0] - low[0]) * r.clickWidth / 2.0;
            double hh = (high[1] - low[1]) * r.clickHeight / 2.0;
            return new double[]{Mth.m_14008_((double)(cx - hw), (double)0.0, (double)1.0), Mth.m_14008_((double)(cx + hw), (double)0.0, (double)1.0), Mth.m_14008_((double)(cy - hh), (double)0.0, (double)1.0), Mth.m_14008_((double)(cy + hh), (double)0.0, (double)1.0)};
        }
        return new double[]{low[0], high[0], low[1], high[1]};
    }

    Vec3 point(FurnaceLayout layout, BlockPos pos, Direction side, int index, double u, double v, double out, boolean item) {
        if (this.region(index).equals(Region.VANILLA)) {
            return FurnaceZones.point(pos, side, this.face(layout, index), u, v, out);
        }
        double[] at = this.uv(layout, index, u, v, item);
        return FurnaceZones.point(pos, side, this.face(layout, index), at[0], at[1], out + this.region((int)index).out / 16.0);
    }

    Vec3 center(FurnaceLayout layout, BlockPos pos, Direction side, int index) {
        double[] r = this.original(layout, index);
        return this.point(layout, pos, side, index, (r[0] + r[1]) / 2.0, (r[2] + r[3]) / 2.0, 0.004, false);
    }

    double[] glowRect(FurnaceLayout layout, FurnaceLayout.Glow glow, int index) {
        double u0 = glow.u0();
        double u1 = glow.u1();
        double v0 = glow.v0();
        double v1 = glow.v1();
        if (this.equals(VANILLA)) {
            return new double[]{u0, u1, v0, v1};
        }
        double[] bounds = this.original(layout, index);
        u0 = Math.max(u0, bounds[0]);
        u1 = Math.min(u1, bounds[1]);
        v0 = Math.max(v0, bounds[2]);
        v1 = Math.min(v1, bounds[3]);
        if (u0 >= u1 || v0 >= v1) {
            return null;
        }
        double[] low = this.uv(layout, index, u0, v0, false);
        double[] high = this.uv(layout, index, u1, v1, false);
        return new double[]{low[0], high[0], low[1], high[1]};
    }

    static boolean contains(double[] r, double u, double v) {
        return u >= r[0] && u <= r[1] && v >= r[2] && v <= r[3];
    }

    record Region(double u, double v, double width, double height, double out, double itemU, double itemV, double itemScale, double clickWidth, double clickHeight) {
        static final Region VANILLA = new Region(0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

        Region {
            u = FurnaceAppearance.finite(u, 0.0, -6.0, 6.0);
            v = FurnaceAppearance.finite(v, 0.0, -6.0, 6.0);
            width = FurnaceAppearance.finite(width, 1.0, 0.5, 1.5);
            height = FurnaceAppearance.finite(height, 1.0, 0.5, 1.5);
            out = FurnaceAppearance.finite(out, 0.0, -2.0, 2.0);
            itemU = FurnaceAppearance.finite(itemU, 0.0, -3.0, 3.0);
            itemV = FurnaceAppearance.finite(itemV, 0.0, -3.0, 3.0);
            itemScale = FurnaceAppearance.finite(itemScale, 1.0, 0.5, 1.5);
            clickWidth = FurnaceAppearance.finite(clickWidth, 1.0, 0.5, 1.5);
            clickHeight = FurnaceAppearance.finite(clickHeight, 1.0, 0.5, 1.5);
        }

        double value(int field) {
            return switch (field) {
                case 0 -> this.u;
                case 1 -> this.v;
                case 2 -> this.width * 100.0;
                case 3 -> this.height * 100.0;
                case 4 -> this.out;
                case 5 -> this.itemScale * 100.0;
                case 6 -> this.clickWidth * 100.0;
                case 7 -> this.clickHeight * 100.0;
                case 8 -> this.itemU;
                case 9 -> this.itemV;
                default -> throw new IllegalArgumentException("Furnace field " + field);
            };
        }

        Region with(int field, double value) {
            return new Region(field == 0 ? value : this.u, field == 1 ? value : this.v, field == 2 ? value / 100.0 : this.width, field == 3 ? value / 100.0 : this.height, field == 4 ? value : this.out, field == 8 ? value : this.itemU, field == 9 ? value : this.itemV, field == 5 ? value / 100.0 : this.itemScale, field == 6 ? value / 100.0 : this.clickWidth, field == 7 ? value / 100.0 : this.clickHeight);
        }

        Region blend(Region target, double t) {
            return new Region(Mth.m_14139_((double)t, (double)this.u, (double)target.u), Mth.m_14139_((double)t, (double)this.v, (double)target.v), Mth.m_14139_((double)t, (double)this.width, (double)target.width), Mth.m_14139_((double)t, (double)this.height, (double)target.height), Mth.m_14139_((double)t, (double)this.out, (double)target.out), Mth.m_14139_((double)t, (double)this.itemU, (double)target.itemU), Mth.m_14139_((double)t, (double)this.itemV, (double)target.itemV), Mth.m_14139_((double)t, (double)this.itemScale, (double)target.itemScale), Mth.m_14139_((double)t, (double)this.clickWidth, (double)target.clickWidth), Mth.m_14139_((double)t, (double)this.clickHeight, (double)target.clickHeight));
        }
    }
}

