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
import dev.dyslexic.cinematic.client.FurnaceAppearance;
import dev.dyslexic.cinematic.client.LayoutFiles;
import dev.dyslexic.cinematic.station.StationKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import net.minecraftforge.fml.loading.FMLPaths;

final class FurnaceLayouts {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "cinematic-furnace-layouts.json";
    private static FurnaceAppearance[][] profiles = FurnaceLayouts.defaults();
    private static int[] selected = new int[3];
    private static final FurnaceAppearance[] current = new FurnaceAppearance[]{FurnaceAppearance.VANILLA, FurnaceAppearance.VANILLA, FurnaceAppearance.VANILLA};
    private static final FurnaceAppearance[] preview = new FurnaceAppearance[3];
    private static boolean loaded;

    private FurnaceLayouts() {
    }

    static int index(StationKind kind) {
        return switch (kind) {
            case StationKind.FURNACE -> 0;
            case StationKind.SMOKER -> 1;
            case StationKind.BLAST_FURNACE -> 2;
            default -> throw new IllegalArgumentException("Not a furnace: " + String.valueOf((Object)kind));
        };
    }

    private static FurnaceAppearance[][] defaults() {
        FurnaceAppearance[][] values;
        for (Object[] objectArray : values = new FurnaceAppearance[3][3]) {
            Arrays.fill(objectArray, FurnaceAppearance.VANILLA);
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
        if (Files.exists(FurnaceLayouts.path(), new LinkOption[0])) {
            try {
                int i;
                if (Files.size(FurnaceLayouts.path()) > 131072L) {
                    throw new IOException("Layout file is too large");
                }
                Saved saved = (Saved)JSON.fromJson(Files.readString(FurnaceLayouts.path()), Saved.class);
                if (saved == null || saved.version != 1 || saved.selected == null || saved.selected.length != 3 || saved.profiles == null || saved.profiles.length != 3) {
                    throw new IOException("Unsupported layout file");
                }
                FurnaceAppearance[][] restored = FurnaceLayouts.defaults();
                for (i = 0; i < 3; ++i) {
                    if (saved.profiles[i] == null || saved.profiles[i].length != 3) {
                        throw new IOException("Invalid profile count");
                    }
                    for (int j = 0; j < 3; ++j) {
                        if (saved.profiles[i][j] == null) continue;
                        restored[i][j] = saved.profiles[i][j];
                    }
                }
                profiles = restored;
                for (i = 0; i < 3; ++i) {
                    FurnaceLayouts.selected[i] = Math.max(0, Math.min(2, saved.selected[i]));
                }
            }
            catch (IOException | RuntimeException e) {
                Cinematic.LOGGER.warn("[Cinematic] Could not load furnace layouts; using vanilla", (Throwable)e);
            }
        }
        for (int i = 0; i < 3; ++i) {
            FurnaceLayouts.current[i] = profiles[i][selected[i]];
        }
    }

    static FurnaceAppearance current(StationKind kind) {
        FurnaceLayouts.load();
        return current[FurnaceLayouts.index(kind)];
    }

    static FurnaceAppearance[] profiles(StationKind kind) {
        FurnaceLayouts.load();
        return (FurnaceAppearance[])profiles[FurnaceLayouts.index(kind)].clone();
    }

    static int selected(StationKind kind) {
        FurnaceLayouts.load();
        return selected[FurnaceLayouts.index(kind)];
    }

    static void preview(StationKind kind, FurnaceAppearance draft) {
        FurnaceLayouts.preview[FurnaceLayouts.index((StationKind)kind)] = draft;
    }

    static void cancelPreview(StationKind kind) {
        FurnaceLayouts.preview[FurnaceLayouts.index((StationKind)kind)] = null;
    }

    static void frame(double dt) {
        FurnaceLayouts.load();
        for (int i = 0; i < 3; ++i) {
            FurnaceAppearance target;
            FurnaceAppearance furnaceAppearance = target = preview[i] == null ? profiles[i][selected[i]] : preview[i];
            if (current[i].equals(target)) continue;
            FurnaceLayouts.current[i] = current[i].blend(target, 1.0 - Math.exp(-18.0 * dt));
            boolean close = true;
            for (int r = 0; r < 5; ++r) {
                for (int f = 0; f < 10; ++f) {
                    close &= Math.abs(current[i].region(r).value(f) - target.region(r).value(f)) < 1.0E-5;
                }
            }
            if (!close) continue;
            FurnaceLayouts.current[i] = target;
        }
    }

    static boolean save(StationKind kind, FurnaceAppearance[] drafts, int active) {
        FurnaceLayouts.load();
        if (drafts.length != 3 || active < 0 || active > 2 || Arrays.stream(drafts).anyMatch(Objects::isNull)) {
            return false;
        }
        FurnaceAppearance[][] next = (FurnaceAppearance[][])profiles.clone();
        int[] nextSelected = (int[])selected.clone();
        next[FurnaceLayouts.index((StationKind)kind)] = (FurnaceAppearance[])drafts.clone();
        nextSelected[FurnaceLayouts.index((StationKind)kind)] = active;
        try {
            LayoutFiles.write(FurnaceLayouts.path(), JSON.toJson((Object)new Saved(1, nextSelected, next)) + "\n");
            profiles = next;
            selected = nextSelected;
            FurnaceLayouts.cancelPreview(kind);
            return true;
        }
        catch (IOException | RuntimeException e) {
            Cinematic.LOGGER.warn("[Cinematic] Could not save furnace layouts", (Throwable)e);
            return false;
        }
    }

    private record Saved(int version, int[] selected, FurnaceAppearance[][] profiles) {
    }
}

