/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.CartographyStation;
import dev.dyslexic.cinematic.client.SmithingStation;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

record SurfaceAppearance(Region first, Region second, Region third) {
    static final SurfaceAppearance VANILLA = new SurfaceAppearance(Region.VANILLA, Region.VANILLA, Region.VANILLA);

    SurfaceAppearance {
        first = first == null ? Region.VANILLA : first;
        second = second == null ? Region.VANILLA : second;
        third = third == null ? Region.VANILLA : third;
    }

    private static double finite(double value, double fallback, double min, double max) {
        return Mth.m_14008_((double)(Double.isFinite(value) ? value : fallback), (double)min, (double)max);
    }

    Region region(int slot) {
        return switch (slot) {
            case 0 -> this.first;
            case 1 -> this.second;
            case 2 -> this.third;
            default -> throw new IllegalArgumentException("Surface slot " + slot);
        };
    }

    SurfaceAppearance with(int slot, Region value) {
        return new SurfaceAppearance(slot == 0 ? value : this.first, slot == 1 ? value : this.second, slot == 2 ? value : this.third);
    }

    SurfaceAppearance blend(SurfaceAppearance target, double t) {
        return new SurfaceAppearance(this.first.blend(target.first, t), this.second.blend(target.second, t), this.third.blend(target.third, t));
    }

    static int count(StationKind kind) {
        return kind == StationKind.SMITHING ? 3 : 2;
    }

    static double baseX(StationKind kind, int slot) {
        return (kind == StationKind.SMITHING ? SmithingStation.LAYOUT : CartographyStation.LAYOUT)[slot][0];
    }

    static double baseZ(StationKind kind, int slot) {
        return (kind == StationKind.SMITHING ? SmithingStation.LAYOUT : CartographyStation.LAYOUT)[slot][1];
    }

    static double radius(StationKind kind, int slot) {
        return kind == StationKind.SMITHING ? (double)SmithingStation.SIZES[slot] * 0.55 : (slot == 0 ? 0.28 : 0.132);
    }

    Vec3 point(StationKind kind, Vec3 top, Direction side, int slot) {
        Region r = this.region(slot);
        Vec3 out = Vec3.m_82528_((Vec3i)side.m_122436_());
        Vec3 right = Vec3.m_82528_((Vec3i)side.m_122424_().m_122427_().m_122436_());
        if (r.equals(Region.VANILLA)) {
            return top.m_82549_(right.m_82490_(SurfaceAppearance.baseX(kind, slot))).m_82549_(out.m_82490_(SurfaceAppearance.baseZ(kind, slot)));
        }
        double a = Math.toRadians(r.rotation);
        double c = Math.abs(Math.cos(a));
        double s = Math.abs(Math.sin(a));
        double[] half = this.halfExtents(kind, slot);
        double extentX = half[0] * c + half[1] * s;
        double extentZ = half[0] * s + half[1] * c;
        double x = Mth.m_14008_((double)(SurfaceAppearance.baseX(kind, slot) + r.x / 16.0), (double)(-Math.max(0.0, 0.49 - extentX)), (double)Math.max(0.0, 0.49 - extentX));
        double z = Mth.m_14008_((double)(SurfaceAppearance.baseZ(kind, slot) + r.z / 16.0), (double)(-Math.max(0.0, 0.49 - extentZ)), (double)Math.max(0.0, 0.49 - extentZ));
        return top.m_82549_(right.m_82490_(x)).m_82549_(out.m_82490_(z)).m_82520_(0.0, r.height / 16.0, 0.0);
    }

    Vec3 axis(Direction side, int slot, boolean across) {
        Vec3 basis = Vec3.m_82528_((Vec3i)(across ? side.m_122424_().m_122427_() : side).m_122436_());
        return basis.m_82524_((float)Math.toRadians(this.region((int)slot).rotation));
    }

    double[] halfExtents(StationKind kind, int slot) {
        Region r = this.region(slot);
        double x = SurfaceAppearance.radius(kind, slot) * r.width;
        double z = SurfaceAppearance.radius(kind, slot) * r.depth;
        double a = Math.toRadians(r.rotation);
        double c = Math.abs(Math.cos(a));
        double s = Math.abs(Math.sin(a));
        double fit = Math.min(1.0, 0.48 / Math.max(x * c + z * s, x * s + z * c));
        return new double[]{x * fit, z * fit};
    }

    boolean contains(StationKind kind, Vec3 top, Direction side, int slot, Vec3 hit) {
        Vec3 delta = hit.m_82546_(this.point(kind, top, side, slot));
        double[] half = this.halfExtents(kind, slot);
        return Math.abs(delta.m_82526_(this.axis(side, slot, true))) <= half[0] && Math.abs(delta.m_82526_(this.axis(side, slot, false))) <= half[1];
    }

    record Region(double x, double z, double rotation, double width, double depth, double height, double itemScale) {
        static final Region VANILLA = new Region(0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0);

        Region {
            x = SurfaceAppearance.finite(x, 0.0, -6.0, 6.0);
            z = SurfaceAppearance.finite(z, 0.0, -6.0, 6.0);
            rotation = SurfaceAppearance.finite(rotation, 0.0, -180.0, 180.0);
            width = SurfaceAppearance.finite(width, 1.0, 0.5, 1.5);
            depth = SurfaceAppearance.finite(depth, 1.0, 0.5, 1.5);
            height = SurfaceAppearance.finite(height, 0.0, -2.0, 2.0);
            itemScale = SurfaceAppearance.finite(itemScale, 1.0, 0.5, 1.5);
        }

        double value(int field) {
            return switch (field) {
                case 0 -> this.x;
                case 1 -> this.z;
                case 2 -> this.rotation;
                case 3 -> this.width * 100.0;
                case 4 -> this.depth * 100.0;
                case 5 -> this.height;
                case 6 -> this.itemScale * 100.0;
                default -> throw new IllegalArgumentException("Surface field " + field);
            };
        }

        Region with(int field, double value) {
            return new Region(field == 0 ? value : this.x, field == 1 ? value : this.z, field == 2 ? value : this.rotation, field == 3 ? value / 100.0 : this.width, field == 4 ? value / 100.0 : this.depth, field == 5 ? value : this.height, field == 6 ? value / 100.0 : this.itemScale);
        }

        Region blend(Region target, double t) {
            return new Region(Mth.m_14139_((double)t, (double)this.x, (double)target.x), Mth.m_14139_((double)t, (double)this.z, (double)target.z), Mth.m_14139_((double)t, (double)this.rotation, (double)target.rotation), Mth.m_14139_((double)t, (double)this.width, (double)target.width), Mth.m_14139_((double)t, (double)this.depth, (double)target.depth), Mth.m_14139_((double)t, (double)this.height, (double)target.height), Mth.m_14139_((double)t, (double)this.itemScale, (double)target.itemScale));
        }
    }
}

