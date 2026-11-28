/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.world.item.crafting.RecipeType
 */
package dev.dyslexic.cinematic.furnace;

import dev.dyslexic.cinematic.furnace.FurnaceZones;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.crafting.RecipeType;

public final class FurnaceLayout {
    public final List<Zone> zones;
    public final List<Bed> beds;
    public final List<Glow> glows;
    public final double[] cookGauge;
    public final double[] burnGauge;
    public final RecipeType<?> recipeType;
    public static final FurnaceLayout FURNACE = new FurnaceLayout(List.of(new Zone(0, FurnaceZones.Face.FRONT, 0.11, 0.5, 0.5, 0.92, new double[]{FurnaceLayout.px(3.0), 0.5, FurnaceLayout.px(9.0), FurnaceLayout.px(13.0)}), new Zone(2, FurnaceZones.Face.FRONT, 0.5, 0.89, 0.5, 0.92, new double[]{0.5, FurnaceLayout.px(13.0), FurnaceLayout.px(9.0), FurnaceLayout.px(13.0)}), new Zone(1, FurnaceZones.Face.FRONT, 0.11, 0.89, 0.0, 0.39, new double[]{FurnaceLayout.px(3.0), FurnaceLayout.px(13.0), FurnaceLayout.px(1.0), FurnaceLayout.px(5.0)})), List.of(new Bed(0, FurnaceZones.Face.FRONT, 0.22, 0.47, FurnaceLayout.px(9.0), 0.035, 3, 0.0f), new Bed(2, FurnaceZones.Face.FRONT, 0.53, 0.78, FurnaceLayout.px(9.0), 0.035, 3, 0.0f), new Bed(1, FurnaceZones.Face.FRONT, 0.26, 0.74, FurnaceLayout.px(1.0), 0.035, 4, 0.0f)), List.of(new Glow(FurnaceZones.Face.FRONT, FurnaceLayout.px(3.0), FurnaceLayout.px(13.0), FurnaceLayout.px(1.0), FurnaceLayout.px(5.0), true), new Glow(FurnaceZones.Face.FRONT, FurnaceLayout.px(3.0), FurnaceLayout.px(13.0), FurnaceLayout.px(9.0), FurnaceLayout.px(13.0), false)), new double[]{FurnaceLayout.px(3.0), FurnaceLayout.px(13.0), 0.405, 0.43}, new double[]{FurnaceLayout.px(3.0), FurnaceLayout.px(13.0), 0.012, 0.037}, RecipeType.f_44108_);
    public static final FurnaceLayout SMOKER = new FurnaceLayout(List.of(new Zone(0, FurnaceZones.Face.FRONT, 0.1, 0.47, 0.46, 0.86, new double[]{FurnaceLayout.px(3.0), FurnaceLayout.px(7.5), FurnaceLayout.px(8.0), FurnaceLayout.px(12.0)}), new Zone(2, FurnaceZones.Face.FRONT, 0.47, 0.84, 0.46, 0.86, new double[]{FurnaceLayout.px(7.5), FurnaceLayout.px(12.0), FurnaceLayout.px(8.0), FurnaceLayout.px(12.0)}), new Zone(1, FurnaceZones.Face.FRONT, 0.08, 0.92, 0.0, 0.46, new double[]{FurnaceLayout.px(2.0), FurnaceLayout.px(14.0), FurnaceLayout.px(1.0), FurnaceLayout.px(7.0)})), List.of(new Bed(0, FurnaceZones.Face.FRONT, 0.24, 0.43, FurnaceLayout.px(8.0), 0.035, 3, 0.0f), new Bed(2, FurnaceZones.Face.FRONT, 0.5, 0.7, FurnaceLayout.px(8.0), 0.035, 3, 0.0f), new Bed(1, FurnaceZones.Face.FRONT, 0.22, 0.78, FurnaceLayout.px(1.0), 0.035, 4, 0.0f)), List.of(new Glow(FurnaceZones.Face.FRONT, FurnaceLayout.px(2.0), FurnaceLayout.px(14.0), FurnaceLayout.px(1.0), FurnaceLayout.px(7.0), true), new Glow(FurnaceZones.Face.FRONT, FurnaceLayout.px(3.0), FurnaceLayout.px(12.0), FurnaceLayout.px(8.0), FurnaceLayout.px(12.0), false)), new double[]{FurnaceLayout.px(3.0), FurnaceLayout.px(13.0), 0.452, 0.482}, new double[]{FurnaceLayout.px(3.0), FurnaceLayout.px(13.0), 0.012, 0.037}, RecipeType.f_44110_);
    public static final FurnaceLayout BLAST_FURNACE = new FurnaceLayout(List.of(new Zone(0, FurnaceZones.Face.TOP, 0.06, 0.94, 0.12, 0.94, new double[]{0.22, 0.78, 0.3, 0.86}), new Zone(2, FurnaceZones.Face.FRONT, 0.12, 0.88, 0.52, 0.86, new double[]{FurnaceLayout.px(3.0), FurnaceLayout.px(13.0), FurnaceLayout.px(9.0), FurnaceLayout.px(10.5)}), new Zone(1, FurnaceZones.Face.FRONT, 0.12, 0.88, 0.16, 0.52, new double[]{FurnaceLayout.px(4.0), FurnaceLayout.px(12.0), FurnaceLayout.px(5.0), FurnaceLayout.px(8.0)})), List.of(new Bed(0, FurnaceZones.Face.TOP, 0.32, 0.68, 0.58, 0.045, 3, 62.0f), new Bed(2, FurnaceZones.Face.FRONT, 0.3, 0.7, FurnaceLayout.px(9.0), 0.035, 3, 0.0f), new Bed(1, FurnaceZones.Face.FRONT, 0.32, 0.68, FurnaceLayout.px(5.0), 0.035, 3, 0.0f)), List.of(new Glow(FurnaceZones.Face.FRONT, FurnaceLayout.px(4.0), FurnaceLayout.px(12.0), FurnaceLayout.px(5.0), FurnaceLayout.px(8.0), true), new Glow(FurnaceZones.Face.TOP, 0.26, 0.74, 0.34, 0.82, false)), new double[]{FurnaceLayout.px(4.0), FurnaceLayout.px(12.0), 0.24, 0.265}, new double[]{FurnaceLayout.px(4.0), FurnaceLayout.px(12.0), 0.012, 0.037}, RecipeType.f_44109_);

    private FurnaceLayout(List<Zone> zones, List<Bed> beds, List<Glow> glows, double[] cookGauge, double[] burnGauge, RecipeType<?> recipeType) {
        this.zones = zones;
        this.beds = beds;
        this.glows = glows;
        this.cookGauge = cookGauge;
        this.burnGauge = burnGauge;
        this.recipeType = recipeType;
    }

    private static double px(double pixels) {
        return pixels / 16.0;
    }

    @Nullable
    public static FurnaceLayout of(StationKind kind) {
        return switch (kind) {
            case StationKind.FURNACE -> FURNACE;
            case StationKind.SMOKER -> SMOKER;
            case StationKind.BLAST_FURNACE -> BLAST_FURNACE;
            default -> null;
        };
    }

    public int zoneAt(FurnaceZones.Face face, double u, double v) {
        for (Zone zone : this.zones) {
            if (!zone.contains(face, u, v)) continue;
            return zone.slot();
        }
        return -1;
    }

    @Nullable
    public Zone zone(int slot) {
        for (Zone zone : this.zones) {
            if (zone.slot() != slot) continue;
            return zone;
        }
        return null;
    }

    public boolean usesTop() {
        for (Zone zone : this.zones) {
            if (zone.face() != FurnaceZones.Face.TOP) continue;
            return true;
        }
        return false;
    }

    public double[] center(int slot) {
        Zone zone = this.zone(slot);
        double[] h = zone.highlight();
        return new double[]{(h[0] + h[1]) / 2.0, (h[2] + h[3]) / 2.0};
    }

    public record Zone(int slot, FurnaceZones.Face face, double u0, double u1, double v0, double v1, double[] highlight) {
        boolean contains(FurnaceZones.Face f, double u, double v) {
            return f == this.face && u >= this.u0 && u <= this.u1 && v >= this.v0 && v <= this.v1;
        }
    }

    public record Bed(int slot, FurnaceZones.Face face, double u0, double u1, double floor, double depth, int maxCopies, float tilt) {
    }

    public record Glow(FurnaceZones.Face face, double u0, double u1, double v0, double v1, boolean fire) {
    }
}

