/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.FurnaceLayouts;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.client.SurfaceLayouts;
import dev.dyslexic.cinematic.client.TableGridEditor;
import dev.dyslexic.cinematic.client.TableLayouts;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.network.chat.Component;

final class LayoutProfiles {
    private LayoutProfiles() {
    }

    static boolean supports(StationKind kind) {
        return kind != StationKind.ADDON;
    }

    static int supportedCount() {
        int count = 0;
        for (StationKind kind : StationKind.values()) {
            if (!LayoutProfiles.supports(kind)) continue;
            ++count;
        }
        return count;
    }

    static Component name(StationKind kind) {
        return Component.m_237115_((String)("cinematic.configuration." + Settings.key(kind)));
    }

    static Component value(int selected) {
        return TableGridEditor.text("profile", selected + 1);
    }

    static int selected(StationKind kind) {
        return switch (kind) {
            case StationKind.TABLE -> TableLayouts.selected();
            case StationKind.FURNACE, StationKind.SMOKER, StationKind.BLAST_FURNACE -> FurnaceLayouts.selected(kind);
            case StationKind.SMITHING, StationKind.CARTOGRAPHY -> SurfaceLayouts.selected(kind);
            default -> SpatialLayouts.selected(kind);
        };
    }

    static boolean select(StationKind kind, int selected) {
        if (selected < 0 || selected >= 3) {
            return false;
        }
        if (selected == LayoutProfiles.selected(kind)) {
            return true;
        }
        return switch (kind) {
            case StationKind.TABLE -> TableLayouts.save(TableLayouts.profiles(), selected);
            case StationKind.FURNACE, StationKind.SMOKER, StationKind.BLAST_FURNACE -> FurnaceLayouts.save(kind, FurnaceLayouts.profiles(kind), selected);
            case StationKind.SMITHING, StationKind.CARTOGRAPHY -> SurfaceLayouts.save(kind, SurfaceLayouts.profiles(kind), selected);
            default -> SpatialLayouts.save(kind, SpatialLayouts.profiles(kind), selected);
        };
    }
}

