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
import dev.dyslexic.cinematic.client.TableLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import net.minecraftforge.fml.loading.FMLPaths;

final class TableLayouts {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "cinematic-table-layouts.json";
    private static TableLayout[] profiles = new TableLayout[]{TableLayout.VANILLA, TableLayout.VANILLA, TableLayout.VANILLA};
    private static int selected;
    private static boolean loaded;
    private static TableLayout preview;
    private static TableLayout current;

    private TableLayouts() {
    }

    static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (Files.exists(TableLayouts.path(), new LinkOption[0])) {
            try {
                if (Files.size(TableLayouts.path()) > 65536L) {
                    throw new IOException("Layout file is too large");
                }
                Saved saved = (Saved)JSON.fromJson(Files.readString(TableLayouts.path()), Saved.class);
                if (saved == null || saved.version != 1 || saved.profiles == null || saved.profiles.length != 3) {
                    throw new IOException("Unsupported layout file");
                }
                for (int i = 0; i < 3; ++i) {
                    TableLayouts.profiles[i] = saved.profiles[i] == null ? TableLayout.VANILLA : saved.profiles[i];
                }
                selected = Math.max(0, Math.min(2, saved.selected));
            }
            catch (IOException | RuntimeException e) {
                Cinematic.LOGGER.warn("[Cinematic] Could not load crafting-table layouts; using vanilla", (Throwable)e);
            }
        }
        current = profiles[selected];
    }

    static TableLayout current() {
        TableLayouts.load();
        return current;
    }

    static int selected() {
        TableLayouts.load();
        return selected;
    }

    static TableLayout[] profiles() {
        TableLayouts.load();
        return (TableLayout[])profiles.clone();
    }

    static void preview(TableLayout draft) {
        preview = draft;
    }

    static void cancelPreview() {
        preview = null;
    }

    static void frame(double dt) {
        TableLayout target;
        TableLayouts.load();
        TableLayout tableLayout = target = preview == null ? profiles[selected] : preview;
        if (current.equals(target)) {
            return;
        }
        if (TableLayouts.close(current = current.blend(target, 1.0 - Math.exp(-18.0 * dt)), target)) {
            current = target;
        }
    }

    private static boolean close(TableLayout a, TableLayout b) {
        for (int i = 0; i < 7; ++i) {
            if (!(Math.abs(a.value(i) - b.value(i)) > 1.0E-5)) continue;
            return false;
        }
        return true;
    }

    static boolean save(TableLayout[] drafts, int active) {
        if (drafts.length != 3 || active < 0 || active > 2 || Arrays.stream(drafts).anyMatch(Objects::isNull)) {
            return false;
        }
        try {
            LayoutFiles.write(TableLayouts.path(), JSON.toJson((Object)new Saved(1, active, drafts)) + "\n");
            profiles = (TableLayout[])drafts.clone();
            selected = active;
            preview = null;
            return true;
        }
        catch (IOException | RuntimeException e) {
            Cinematic.LOGGER.warn("[Cinematic] Could not save crafting-table layouts", (Throwable)e);
            return false;
        }
    }

    static {
        current = TableLayout.VANILLA;
    }

    private record Saved(int version, int selected, TableLayout[] profiles) {
    }
}

