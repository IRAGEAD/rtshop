/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraftforge.fml.loading.FMLPaths
 */
package dev.dyslexic.cinematic.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.LayoutFiles;
import dev.dyslexic.cinematic.client.ShelfLayoutBooks;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.station.StationKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import net.minecraftforge.fml.loading.FMLPaths;

final class SpatialLayouts {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "cinematic-spatial-layouts.json";
    private static SpatialAppearance[][] profiles = SpatialLayouts.defaults();
    private static final int TYPES = 12;
    private static int[] selected = new int[12];
    private static final SpatialAppearance[] current = SpatialLayouts.vanillaRow();
    private static final SpatialAppearance[] preview = new SpatialAppearance[12];
    private static boolean loaded;
    private static volatile boolean customShelf;

    private SpatialLayouts() {
    }

    static boolean customShelf() {
        return customShelf;
    }

    static int index(StationKind kind) {
        return switch (kind) {
            case StationKind.CHISELED_BOOKSHELF -> 1;
            case StationKind.BREWING -> 2;
            case StationKind.LOOM -> 3;
            case StationKind.ENCHANTING -> 4;
            case StationKind.ANVIL -> 5;
            case StationKind.GRINDSTONE -> 6;
            case StationKind.STONECUTTER -> 7;
            case StationKind.CAMPFIRE -> 8;
            case StationKind.LECTERN -> 9;
            case StationKind.BEACON -> 10;
            case StationKind.JUKEBOX -> 11;
            default -> throw new IllegalArgumentException("Not a spatial layout: " + String.valueOf((Object)kind));
        };
    }

    private static SpatialAppearance[][] defaults() {
        SpatialAppearance[][] values;
        for (Object[] objectArray : values = new SpatialAppearance[12][3]) {
            Arrays.fill(objectArray, SpatialAppearance.VANILLA);
        }
        return values;
    }

    private static SpatialAppearance[] vanillaRow() {
        Object[] row = new SpatialAppearance[12];
        Arrays.fill(row, SpatialAppearance.VANILLA);
        return row;
    }

    static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (Files.exists(SpatialLayouts.path(), new LinkOption[0])) {
            try {
                int i;
                int rows;
                if (Files.size(SpatialLayouts.path()) > 131072L) {
                    throw new IOException("Layout file is too large");
                }
                Saved saved = (Saved)JSON.fromJson(Files.readString(SpatialLayouts.path()), Saved.class);
                int n = saved != null && saved.version == 1 ? 3 : (saved != null && saved.version == 2 ? 5 : (rows = saved != null && saved.version == 3 ? 12 : -1));
                if (rows < 0 || saved.selected == null || saved.selected.length != rows || saved.profiles == null || saved.profiles.length != rows) {
                    throw new IOException("Unsupported layout file");
                }
                SpatialAppearance[][] restored = SpatialLayouts.defaults();
                for (i = 0; i < rows; ++i) {
                    if (saved.profiles[i] == null || saved.profiles[i].length != 3) {
                        throw new IOException("Invalid profile count");
                    }
                    for (int j = 0; j < 3; ++j) {
                        if (saved.profiles[i][j] == null) continue;
                        restored[i][j] = saved.profiles[i][j];
                    }
                }
                profiles = restored;
                for (i = 0; i < rows; ++i) {
                    SpatialLayouts.selected[i] = Math.max(0, Math.min(2, saved.selected[i]));
                }
            }
            catch (IOException | RuntimeException e) {
                Cinematic.LOGGER.warn("[Cinematic] Could not load station layouts; using vanilla", (Throwable)e);
            }
        }
        for (int i = 0; i < 12; ++i) {
            SpatialLayouts.current[i] = profiles[i][selected[i]];
        }
    }

    static SpatialAppearance current(StationKind kind) {
        SpatialLayouts.load();
        return current[SpatialLayouts.index(kind)];
    }

    static SpatialAppearance[] profiles(StationKind kind) {
        SpatialLayouts.load();
        return (SpatialAppearance[])profiles[SpatialLayouts.index(kind)].clone();
    }

    static int selected(StationKind kind) {
        SpatialLayouts.load();
        return selected[SpatialLayouts.index(kind)];
    }

    static void preview(StationKind kind, SpatialAppearance draft) {
        SpatialLayouts.preview[SpatialLayouts.index((StationKind)kind)] = draft;
    }

    static void cancelPreview(StationKind kind) {
        SpatialLayouts.preview[SpatialLayouts.index((StationKind)kind)] = null;
    }

    static void frame(double dt) {
        boolean custom;
        SpatialLayouts.load();
        for (int i = 0; i < 12; ++i) {
            SpatialAppearance target;
            SpatialAppearance spatialAppearance = target = preview[i] == null ? profiles[i][selected[i]] : preview[i];
            if (current[i].equals(target)) continue;
            SpatialLayouts.current[i] = current[i].blend(target, 1.0 - Math.exp(-18.0 * dt));
            boolean close = true;
            for (int r = -1; r < 9; ++r) {
                for (int f = 0; f < 7; ++f) {
                    close &= Math.abs(current[i].region(r).value(f) - target.region(r).value(f)) < 1.0E-5;
                }
            }
            if (!close) continue;
            SpatialLayouts.current[i] = target;
        }
        boolean bl = custom = !current[1].equals(SpatialAppearance.VANILLA);
        if (customShelf != custom) {
            customShelf = custom;
            ShelfLayoutBooks.layoutChanged(custom);
        }
    }

    static boolean save(StationKind kind, SpatialAppearance[] drafts, int active) {
        SpatialLayouts.load();
        if (drafts.length != 3 || active < 0 || active > 2 || Arrays.stream(drafts).anyMatch(Objects::isNull)) {
            return false;
        }
        SpatialAppearance[][] next = (SpatialAppearance[][])profiles.clone();
        int[] nextSelected = (int[])selected.clone();
        next[SpatialLayouts.index((StationKind)kind)] = (SpatialAppearance[])drafts.clone();
        nextSelected[SpatialLayouts.index((StationKind)kind)] = active;
        try {
            LayoutFiles.write(SpatialLayouts.path(), JSON.toJson((Object)new Saved(3, nextSelected, next)) + "\n");
            profiles = next;
            selected = nextSelected;
            SpatialLayouts.cancelPreview(kind);
            return true;
        }
        catch (IOException | RuntimeException e) {
            Cinematic.LOGGER.warn("[Cinematic] Could not save station layouts", (Throwable)e);
            return false;
        }
    }

    private record Saved(int version, int[] selected, SpatialAppearance[][] profiles) {
    }
}

