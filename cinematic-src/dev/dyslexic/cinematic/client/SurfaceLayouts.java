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
import dev.dyslexic.cinematic.client.SurfaceAppearance;
import dev.dyslexic.cinematic.station.StationKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import net.minecraftforge.fml.loading.FMLPaths;

final class SurfaceLayouts {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "cinematic-surface-layouts.json";
    private static SurfaceAppearance[][] profiles = SurfaceLayouts.defaults();
    private static int[] selected = new int[2];
    private static final SurfaceAppearance[] current = new SurfaceAppearance[]{SurfaceAppearance.VANILLA, SurfaceAppearance.VANILLA};
    private static final SurfaceAppearance[] preview = new SurfaceAppearance[2];
    private static boolean loaded;

    private SurfaceLayouts() {
    }

    static int index(StationKind kind) {
        return switch (kind) {
            case StationKind.SMITHING -> 0;
            case StationKind.CARTOGRAPHY -> 1;
            default -> throw new IllegalArgumentException("Not a tabletop: " + String.valueOf((Object)kind));
        };
    }

    private static SurfaceAppearance[][] defaults() {
        SurfaceAppearance[][] values;
        for (Object[] objectArray : values = new SurfaceAppearance[2][3]) {
            Arrays.fill(objectArray, SurfaceAppearance.VANILLA);
        }
        return values;
    }

    static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (Files.exists(SurfaceLayouts.path(), new LinkOption[0])) {
            try {
                int i;
                if (Files.size(SurfaceLayouts.path()) > 131072L) {
                    throw new IOException("Layout file is too large");
                }
                Saved saved = (Saved)JSON.fromJson(Files.readString(SurfaceLayouts.path()), Saved.class);
                if (saved == null || saved.version != 1 || saved.selected == null || saved.selected.length != 2 || saved.profiles == null || saved.profiles.length != 2) {
                    throw new IOException("Unsupported layout file");
                }
                SurfaceAppearance[][] restored = SurfaceLayouts.defaults();
                for (i = 0; i < 2; ++i) {
                    if (saved.profiles[i] == null || saved.profiles[i].length != 3) {
                        throw new IOException("Invalid profile count");
                    }
                    for (int j = 0; j < 3; ++j) {
                        if (saved.profiles[i][j] == null) continue;
                        restored[i][j] = saved.profiles[i][j];
                    }
                }
                profiles = restored;
                for (i = 0; i < 2; ++i) {
                    SurfaceLayouts.selected[i] = Math.max(0, Math.min(2, saved.selected[i]));
                }
            }
            catch (IOException | RuntimeException e) {
                Cinematic.LOGGER.warn("[Cinematic] Could not load tabletop layouts; using vanilla", (Throwable)e);
            }
        }
        for (int i = 0; i < 2; ++i) {
            SurfaceLayouts.current[i] = profiles[i][selected[i]];
        }
    }

    static SurfaceAppearance current(StationKind kind) {
        SurfaceLayouts.load();
        return current[SurfaceLayouts.index(kind)];
    }

    static SurfaceAppearance[] profiles(StationKind kind) {
        SurfaceLayouts.load();
        return (SurfaceAppearance[])profiles[SurfaceLayouts.index(kind)].clone();
    }

    static int selected(StationKind kind) {
        SurfaceLayouts.load();
        return selected[SurfaceLayouts.index(kind)];
    }

    static void preview(StationKind kind, SurfaceAppearance draft) {
        SurfaceLayouts.preview[SurfaceLayouts.index((StationKind)kind)] = draft;
    }

    static void cancelPreview(StationKind kind) {
        SurfaceLayouts.preview[SurfaceLayouts.index((StationKind)kind)] = null;
    }

    static void frame(double dt) {
        SurfaceLayouts.load();
        for (int i = 0; i < 2; ++i) {
            SurfaceAppearance target;
            SurfaceAppearance surfaceAppearance = target = preview[i] == null ? profiles[i][selected[i]] : preview[i];
            if (current[i].equals(target)) continue;
            SurfaceLayouts.current[i] = current[i].blend(target, 1.0 - Math.exp(-18.0 * dt));
            boolean close = true;
            for (int r = 0; r < 3; ++r) {
                for (int f = 0; f < 7; ++f) {
                    close &= Math.abs(current[i].region(r).value(f) - target.region(r).value(f)) < 1.0E-5;
                }
            }
            if (!close) continue;
            SurfaceLayouts.current[i] = target;
        }
    }

    static boolean save(StationKind kind, SurfaceAppearance[] drafts, int active) {
        SurfaceLayouts.load();
        if (drafts.length != 3 || active < 0 || active > 2 || Arrays.stream(drafts).anyMatch(Objects::isNull)) {
            return false;
        }
        SurfaceAppearance[][] next = (SurfaceAppearance[][])profiles.clone();
        int[] nextSelected = (int[])selected.clone();
        next[SurfaceLayouts.index((StationKind)kind)] = (SurfaceAppearance[])drafts.clone();
        nextSelected[SurfaceLayouts.index((StationKind)kind)] = active;
        try {
            LayoutFiles.write(SurfaceLayouts.path(), JSON.toJson((Object)new Saved(1, nextSelected, next)) + "\n");
            profiles = next;
            selected = nextSelected;
            SurfaceLayouts.cancelPreview(kind);
            return true;
        }
        catch (IOException | RuntimeException e) {
            Cinematic.LOGGER.warn("[Cinematic] Could not save tabletop layouts", (Throwable)e);
            return false;
        }
    }

    private record Saved(int version, int[] selected, SurfaceAppearance[][] profiles) {
    }
}

