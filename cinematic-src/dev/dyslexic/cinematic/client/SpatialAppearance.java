/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.BrewingStation;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.LoomStation;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.client.SurfaceAppearance;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

record SpatialAppearance(SurfaceAppearance.Region group, List<SurfaceAppearance.Region> slots) {
    private static final double FLOATING_BOUND = 0.8;
    static final SpatialAppearance VANILLA = new SpatialAppearance(SurfaceAppearance.Region.VANILLA, Collections.nCopies(9, SurfaceAppearance.Region.VANILLA));

    SpatialAppearance {
        if (group == null) {
            group = SurfaceAppearance.Region.VANILLA;
        }
        ArrayList<SurfaceAppearance.Region> safe = new ArrayList<SurfaceAppearance.Region>(9);
        for (int i = 0; i < 9; ++i) {
            safe.add(slots != null && i < slots.size() && slots.get(i) != null ? slots.get(i) : SurfaceAppearance.Region.VANILLA);
        }
        slots = List.copyOf(safe);
    }

    SurfaceAppearance.Region region(int slot) {
        return slot < 0 ? this.group : this.slots.get(slot);
    }

    SpatialAppearance with(int slot, SurfaceAppearance.Region value) {
        if (slot < 0) {
            return new SpatialAppearance(value, this.slots);
        }
        ArrayList<SurfaceAppearance.Region> next = new ArrayList<SurfaceAppearance.Region>(this.slots);
        next.set(slot, value);
        return new SpatialAppearance(this.group, next);
    }

    SpatialAppearance blend(SpatialAppearance other, double k) {
        ArrayList<SurfaceAppearance.Region> next = new ArrayList<SurfaceAppearance.Region>(9);
        for (int i = 0; i < 9; ++i) {
            next.add(this.region(i).blend(other.region(i), k));
        }
        return new SpatialAppearance(this.group.blend(other.group, k), next);
    }

    static int count(StationKind kind) {
        return switch (kind) {
            case StationKind.CHISELED_BOOKSHELF -> 6;
            case StationKind.BREWING, StationKind.ENCHANTING -> 5;
            case StationKind.LOOM, StationKind.ANVIL, StationKind.GRINDSTONE, StationKind.STONECUTTER -> 3;
            case StationKind.CAMPFIRE -> 4;
            case StationKind.LECTERN, StationKind.JUKEBOX -> 1;
            case StationKind.BEACON -> 8;
            default -> throw new IllegalArgumentException("Not a spatial layout: " + String.valueOf((Object)kind));
        };
    }

    Vec3 axis(Frame frame, int slot, boolean across) {
        double angle = Math.toRadians(this.group.rotation() + (slot < 0 ? 0.0 : this.region(slot).rotation()));
        return across ? frame.across.m_82490_(Math.cos(angle)).m_82549_(frame.up.m_82490_(Math.sin(angle))) : frame.up.m_82490_(Math.cos(angle)).m_82546_(frame.across.m_82490_(Math.sin(angle)));
    }

    double rotation(int slot) {
        return this.group.rotation() + this.region(slot).rotation();
    }

    float scale(int slot) {
        return (float)(this.group.itemScale() * this.region(slot).itemScale());
    }

    double[] half(Frame frame, int slot) {
        if (RemainingLayouts.supports(frame.kind)) {
            double[] half = RemainingLayouts.half(frame.kind, slot);
            return new double[]{half[0] * this.group.width() * this.region(slot).width(), half[1] * this.group.depth() * this.region(slot).depth()};
        }
        if (frame.kind == StationKind.LOOM || frame.kind == StationKind.ENCHANTING) {
            double u = frame.kind == StationKind.LOOM ? (slot == 0 ? 0.21 : 0.1) : (slot == 1 ? 0.115 : 0.15);
            double v = frame.kind == StationKind.LOOM && slot == 0 ? 0.42 : u;
            return new double[]{u * this.group.width() * this.region(slot).width(), v * this.group.depth() * this.region(slot).depth()};
        }
        double u = frame.kind == StationKind.BREWING ? 0.1 : (double)(slot % 3 == 0 ? 6 : 5) / 32.0;
        double v = frame.kind == StationKind.BREWING ? 0.1 : 0.25;
        double angle = Math.toRadians(this.rotation(slot));
        double c = Math.abs(Math.cos(angle));
        double s = Math.abs(Math.sin(angle));
        double fit = Math.min(1.0, 0.48 / Math.max((u *= this.group.width() * this.region(slot).width()) * c + (v *= this.group.depth() * this.region(slot).depth()) * s, u * s + v * c));
        return new double[]{u * fit, v * fit};
    }

    Vec3 point(Frame frame, int slot) {
        if (this.group.equals(SurfaceAppearance.Region.VANILLA) && this.region(slot).equals(SurfaceAppearance.Region.VANILLA)) {
            return frame.base(slot);
        }
        Vec3 rel = frame.base(slot).m_82546_(frame.origin);
        SurfaceAppearance.Region r = this.region(slot);
        Vec3 moved = this.axis(frame, -1, true).m_82490_(rel.m_82526_(frame.across) * this.group.width() + r.x() / 16.0).m_82549_(this.axis(frame, -1, false).m_82490_(rel.m_82526_(frame.up) * this.group.depth() + r.z() / 16.0)).m_82549_(frame.across.m_82490_(this.group.x() / 16.0)).m_82549_(frame.up.m_82490_(this.group.z() / 16.0));
        double[] half = this.half(frame, slot);
        if (frame.kind == StationKind.CHISELED_BOOKSHELF) {
            int[] spine = ShelfBooks.SPINES[slot];
            half = new double[]{(double)(spine[2] - spine[0]) / 32.0 * (double)this.scale(slot), (double)(spine[3] - spine[1]) / 32.0 * (double)this.scale(slot)};
        }
        Vec3 a = this.axis(frame, slot, true);
        Vec3 b = this.axis(frame, slot, false);
        double hu = Math.min(0.48, Math.abs(a.m_82526_(frame.across)) * half[0] + Math.abs(b.m_82526_(frame.across)) * half[1]);
        double hv = Math.min(0.48, Math.abs(a.m_82526_(frame.up)) * half[0] + Math.abs(b.m_82526_(frame.up)) * half[1]);
        double bound = RemainingLayouts.supports(frame.kind) ? 1.2 : (frame.kind == StationKind.ENCHANTING ? 0.8 : 0.49);
        hu = Math.min(bound - 0.01, hu);
        hv = Math.min(bound - 0.01, hv);
        double u = Mth.m_14008_((double)moved.m_82526_(frame.across), (double)(-bound + hu), (double)(bound - hu));
        double v = Mth.m_14008_((double)moved.m_82526_(frame.up), (double)(-bound + hv), (double)(bound - hv));
        double n = rel.m_82526_(frame.normal) + (this.group.height() + r.height()) / 16.0;
        if (frame.kind == StationKind.BREWING && slot < 3) {
            n += 0.18374999426305294 * (double)(this.scale(slot) - 1.0f);
        }
        n = frame.kind == StationKind.BREWING ? Mth.m_14008_((double)n, (double)-0.33, (double)0.675) : (frame.kind == StationKind.LOOM || RemainingLayouts.supports(frame.kind) ? n : Math.max(frame.kind == StationKind.CHISELED_BOOKSHELF ? 0.004 : 0.0, n));
        return frame.origin.m_82549_(frame.across.m_82490_(u)).m_82549_(frame.up.m_82490_(v)).m_82549_(frame.normal.m_82490_(n));
    }

    boolean contains(Frame frame, int slot, Vec3 hit) {
        Vec3 rel = hit.m_82546_(this.point(frame, slot));
        double[] half = this.half(frame, slot);
        return Math.abs(rel.m_82526_(this.axis(frame, slot, true))) <= half[0] && Math.abs(rel.m_82526_(this.axis(frame, slot, false))) <= half[1];
    }

    record Frame(StationKind kind, BlockPos pos, Direction side, Direction grid, Vec3 origin, Vec3 across, Vec3 up, Vec3 normal) {
        static Frame of(StationKind kind, BlockPos pos, Direction side, Direction grid) {
            Vec3 out = Vec3.m_82528_((Vec3i)side.m_122436_());
            Vec3 right = ShelfBooks.right(side);
            return switch (kind) {
                case StationKind.CHISELED_BOOKSHELF -> new Frame(kind, pos, side, side, Vec3.m_82512_((Vec3i)pos).m_82549_(out.m_82490_(0.5)), right, new Vec3(0.0, 1.0, 0.0), out);
                case StationKind.BREWING -> new Frame(kind, pos, side, Direction.UP, Vec3.m_82539_((Vec3i)pos).m_82520_(0.0, 0.45, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0));
                case StationKind.ENCHANTING, StationKind.LOOM -> new Frame(kind, pos, side, Direction.UP, Vec3.m_82539_((Vec3i)pos).m_82520_(0.0, kind == StationKind.LOOM ? 1.0 : 0.75, 0.0), right, out, new Vec3(0.0, 1.0, 0.0));
                default -> RemainingLayouts.frame(kind, pos, side);
            };
        }

        Vec3 base(int slot) {
            if (RemainingLayouts.supports(this.kind)) {
                return RemainingLayouts.base(this, slot);
            }
            if (this.kind == StationKind.LOOM) {
                return this.origin.m_82549_(this.across.m_82490_(LoomStation.CORNERS[slot][0])).m_82549_(this.up.m_82490_(LoomStation.CORNERS[slot][1]));
            }
            if (this.kind == StationKind.ENCHANTING) {
                if (slot == 0) {
                    return EnchantFx.itemCenter(this.pos, this.side, 0.0);
                }
                if (slot == 1) {
                    return EnchantFx.lapisCorner(this.pos, this.side);
                }
                int off = slot - 3;
                return this.origin.m_82520_(0.0, off == 0 ? 0.92 : 0.74, 0.0).m_82549_(this.up.m_82490_(off == 0 ? -0.12 : 0.02)).m_82549_(this.across.m_82490_((double)off * 0.4));
            }
            if (this.kind == StationKind.CHISELED_BOOKSHELF) {
                int[] r = ShelfBooks.SPINES[slot];
                return this.origin.m_82549_(this.across.m_82490_((double)(r[0] + r[2]) / 32.0 - 0.5)).m_82549_(this.up.m_82490_(0.5 - (double)(r[1] + r[3]) / 32.0)).m_82549_(this.normal.m_82490_(0.004));
            }
            if (slot < 3) {
                return new Vec3((double)this.pos.m_123341_() + BrewingStation.FEET[slot][0] / 16.0, (double)this.pos.m_123342_() + 0.30874999426305294, (double)this.pos.m_123343_() + BrewingStation.FEET[slot][1] / 16.0);
            }
            return slot == 3 ? Vec3.m_82539_((Vec3i)this.pos).m_82520_(0.0, 0.885, 0.0) : Vec3.m_82539_((Vec3i)this.pos).m_82549_(Vec3.m_82528_((Vec3i)this.side.m_122436_()).m_82490_(0.1)).m_82520_(0.0, 0.16, 0.0);
        }
    }
}

