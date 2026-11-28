/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.Direction
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.dyslexic.cinematic.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class AddonStations {
    private static final Map<ResourceLocation, Definition> BY_ID = new HashMap<ResourceLocation, Definition>();
    private static final Map<ResourceLocation, Definition> BY_BLOCK = new HashMap<ResourceLocation, Definition>();

    private AddonStations() {
    }

    public static synchronized void register(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        if ("minecraft".equals(definition.blockId().m_135827_()) || BY_ID.containsKey(definition.id()) || BY_BLOCK.containsKey(definition.blockId())) {
            throw new IllegalArgumentException("Duplicate or vanilla cinematic add-on station: " + String.valueOf(definition.id()));
        }
        BY_ID.put(definition.id(), definition);
        BY_BLOCK.put(definition.blockId(), definition);
    }

    @Nullable
    public static synchronized Definition byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    @Nullable
    public static synchronized Definition forBlock(BlockState state) {
        return BY_BLOCK.get(BuiltInRegistries.f_256975_.m_7981_((Object)state.m_60734_()));
    }

    public record Definition(ResourceLocation id, ResourceLocation blockId, Class<? extends AbstractContainerMenu> menuClass, Function<BlockState, List<Direction>> sides) {
        public Definition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(blockId, "blockId");
            Objects.requireNonNull(menuClass, "menuClass");
            Objects.requireNonNull(sides, "sides");
        }

        public boolean accepts(AbstractContainerMenu menu) {
            return this.menuClass.isInstance(menu);
        }

        public List<Direction> usableSides(BlockState state) {
            List<Direction> result = List.copyOf((Collection)this.sides.apply(state));
            if (result.stream().anyMatch(side -> side == null || !side.m_122434_().m_122479_())) {
                throw new IllegalStateException("Addon station " + String.valueOf(this.id) + " must provide horizontal working sides");
            }
            return result;
        }
    }
}

