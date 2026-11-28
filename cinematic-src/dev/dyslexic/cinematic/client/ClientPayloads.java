/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.CampfireStation;
import dev.dyslexic.cinematic.client.ClientEnchants;
import dev.dyslexic.cinematic.client.ClientFurnaces;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientTables;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.net.AtStationPayload;
import dev.dyslexic.cinematic.net.CampfireProgressPayload;
import dev.dyslexic.cinematic.net.EnchantDisplayPayload;
import dev.dyslexic.cinematic.net.EnchantFxPayload;
import dev.dyslexic.cinematic.net.FurnaceContentsPayload;
import dev.dyslexic.cinematic.net.FurnaceStatePayload;
import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.net.SessionClosePayload;
import dev.dyslexic.cinematic.net.TableContentsPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientPayloads {
    public static final Map<Integer, AtStation> AT_STATION = new HashMap<Integer, AtStation>();

    private ClientPayloads() {
    }

    public static void tableContents(TableContentsPayload payload) {
        ClientTables.apply(payload);
    }

    public static void furnaceContents(FurnaceContentsPayload payload) {
        ClientFurnaces.apply(payload);
    }

    public static void furnaceState(FurnaceStatePayload payload) {
        FurnaceStation station = Scene.station(FurnaceStation.class);
        if (station != null && station.pos.equals((Object)payload.pos())) {
            station.applyState(payload);
        }
    }

    public static void campfireProgress(CampfireProgressPayload payload) {
        CampfireStation station = Scene.station(CampfireStation.class);
        if (station != null && station.pos.equals((Object)payload.pos())) {
            station.applyProgress(payload);
        }
    }

    public static void close(SessionClosePayload payload) {
        Scene.serverClosed();
    }

    public static void atStation(AtStationPayload payload) {
        if (payload.pos().isPresent()) {
            AT_STATION.put(payload.entityId(), new AtStation(payload.kind(), payload.pos().get()));
        } else {
            AT_STATION.remove(payload.entityId());
        }
    }

    @Nullable
    public static StationKind atStation(int entityId) {
        AtStation at = AT_STATION.get(entityId);
        return at == null ? null : at.kind();
    }

    public static boolean someoneAt(BlockPos pos) {
        for (AtStation at : AT_STATION.values()) {
            if (!at.pos().equals((Object)pos)) continue;
            return true;
        }
        return false;
    }

    public static void enchantDisplay(EnchantDisplayPayload payload) {
        ClientEnchants.apply(payload);
    }

    public static void enchantFx(EnchantFxPayload payload) {
        boolean mine;
        Minecraft mc = Minecraft.m_91087_();
        boolean bl = mine = mc.f_91074_ != null && payload.entityId() == mc.f_91074_.m_19879_() && Scene.station(EnchantStation.class) != null;
        if (!mine) {
            ClientEnchants.flash(payload.pos());
        }
    }

    public static void menuDisplay(MenuDisplayPayload payload) {
        ClientMenus.apply(payload);
    }

    public static void menuFx(MenuFxPayload payload) {
        boolean mine;
        if (payload.kind() == StationKind.CHISELED_BOOKSHELF) {
            ShelfBooks.cue(payload);
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        boolean bl = mine = mc.f_91074_ != null && payload.entityId() == mc.f_91074_.m_19879_() && Scene.active() && Scene.station.pos.equals((Object)payload.pos());
        if (mine) {
            return;
        }
        if (payload.cue() == MenuFxPayload.Cue.MOVE) {
            ClientMenus.move(payload);
        } else {
            ClientMenus.fx(payload);
        }
    }

    public static void clear() {
        AT_STATION.clear();
    }

    public record AtStation(StationKind kind, BlockPos pos) {
    }
}

