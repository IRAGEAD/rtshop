/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.dyslexic.cinematic.api.client;

import dev.dyslexic.cinematic.api.AddonStations;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class AddonScenes {
    private static final Map<ResourceLocation, Factory> FACTORIES = new HashMap<ResourceLocation, Factory>();

    private AddonScenes() {
    }

    public static synchronized void register(ResourceLocation id, Factory factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        if (AddonStations.byId(id) == null) {
            throw new IllegalArgumentException("Register the add-on station before its client scene: " + String.valueOf(id));
        }
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalArgumentException("Duplicate cinematic add-on scene: " + String.valueOf(id));
        }
    }

    public static synchronized boolean available(BlockState state) {
        AddonStations.Definition definition = AddonStations.forBlock(state);
        return definition != null && FACTORIES.containsKey(definition.id());
    }

    @Nullable
    public static synchronized ClientStation create(BlockPos pos, BlockState state, LocalPlayer player, Direction side) {
        Factory factory;
        AddonStations.Definition definition = AddonStations.forBlock(state);
        Factory factory2 = factory = definition == null ? null : FACTORIES.get(definition.id());
        if (factory == null) {
            return null;
        }
        ClientStation station = Objects.requireNonNull(factory.create(pos, state, player, side), "addon scene");
        if (station.kind() != StationKind.ADDON || !station.pos.equals((Object)pos)) {
            throw new IllegalStateException("Addon scene " + String.valueOf(definition.id()) + " must target its registered block");
        }
        return station;
    }

    @FunctionalInterface
    public static interface Factory {
        public ClientStation create(BlockPos var1, BlockState var2, LocalPlayer var3, Direction var4);
    }
}

