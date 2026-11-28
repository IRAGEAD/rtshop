/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.AbstractFurnaceBlock
 *  net.minecraft.world.level.block.AnvilBlock
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.GrindstoneBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.LecternBlock
 *  net.minecraft.world.level.block.LoomBlock
 *  net.minecraft.world.level.block.StonecutterBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.api.AddonStations;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public enum StationKind {
    TABLE,
    FURNACE,
    SMOKER,
    BLAST_FURNACE,
    ENCHANTING,
    ANVIL,
    GRINDSTONE,
    SMITHING,
    STONECUTTER,
    LOOM,
    CARTOGRAPHY,
    BREWING,
    CAMPFIRE,
    LECTERN,
    BEACON,
    JUKEBOX,
    CHISELED_BOOKSHELF,
    ADDON;

    private static final List<Direction> ANY_SIDE;

    @Nullable
    public static StationKind of(BlockState state) {
        if (state.m_60713_(Blocks.f_50091_)) {
            return TABLE;
        }
        if (state.m_60713_(Blocks.f_50094_)) {
            return FURNACE;
        }
        if (state.m_60713_(Blocks.f_50619_)) {
            return SMOKER;
        }
        if (state.m_60713_(Blocks.f_50620_)) {
            return BLAST_FURNACE;
        }
        if (state.m_60713_(Blocks.f_50201_)) {
            return ENCHANTING;
        }
        if (state.m_204336_(BlockTags.f_13033_)) {
            return ANVIL;
        }
        if (state.m_60713_(Blocks.f_50623_)) {
            return GRINDSTONE;
        }
        if (state.m_60713_(Blocks.f_50625_)) {
            return SMITHING;
        }
        if (state.m_60713_(Blocks.f_50679_)) {
            return STONECUTTER;
        }
        if (state.m_60713_(Blocks.f_50617_)) {
            return LOOM;
        }
        if (state.m_60713_(Blocks.f_50621_)) {
            return CARTOGRAPHY;
        }
        if (state.m_60713_(Blocks.f_50255_)) {
            return BREWING;
        }
        if (state.m_204336_(BlockTags.f_13087_)) {
            return CAMPFIRE;
        }
        if (state.m_60713_(Blocks.f_50624_)) {
            return LECTERN;
        }
        if (state.m_60713_(Blocks.f_50273_)) {
            return BEACON;
        }
        if (state.m_60713_(Blocks.f_50131_)) {
            return JUKEBOX;
        }
        if (state.m_60713_(Blocks.f_244299_)) {
            return CHISELED_BOOKSHELF;
        }
        return AddonStations.forBlock(state) == null ? null : ADDON;
    }

    public boolean matches(BlockState state) {
        return StationKind.of(state) == this;
    }

    public boolean isFurnace() {
        return this == FURNACE || this == SMOKER || this == BLAST_FURNACE;
    }

    public boolean usesMenu() {
        return switch (this) {
            case ENCHANTING, ANVIL, GRINDSTONE, SMITHING, STONECUTTER, LOOM, CARTOGRAPHY, BREWING, LECTERN, BEACON, ADDON -> true;
            default -> false;
        };
    }

    public boolean startsOnUse() {
        return this != CAMPFIRE;
    }

    public List<Direction> sides(BlockState state) {
        return switch (this) {
            case ADDON -> {
                AddonStations.Definition definition = AddonStations.forBlock(state);
                if (definition == null) {
                    yield List.of();
                }
                yield definition.usableSides(state);
            }
            case FURNACE, SMOKER, BLAST_FURNACE -> List.of((Direction)state.m_61143_((Property)AbstractFurnaceBlock.f_48683_));
            case ANVIL -> {
                Direction facing = (Direction)state.m_61143_((Property)AnvilBlock.f_48764_);
                yield List.of(facing.m_122427_(), facing.m_122428_());
            }
            case GRINDSTONE -> {
                Direction facing = (Direction)state.m_61143_((Property)GrindstoneBlock.f_54117_);
                yield List.of(facing, facing.m_122424_());
            }
            case STONECUTTER -> {
                Direction facing = (Direction)state.m_61143_((Property)StonecutterBlock.f_57063_);
                yield List.of(facing, facing.m_122424_());
            }
            case LOOM -> List.of((Direction)state.m_61143_((Property)LoomBlock.f_54117_));
            case LECTERN -> List.of((Direction)state.m_61143_((Property)LecternBlock.f_54465_));
            case CHISELED_BOOKSHELF -> List.of((Direction)state.m_61143_((Property)HorizontalDirectionalBlock.f_54117_));
            case JUKEBOX -> List.of(Direction.EAST, Direction.WEST);
            default -> ANY_SIDE;
        };
    }

    public boolean usable(BlockGetter level, BlockPos pos, BlockState state) {
        if (this == LECTERN && !((Boolean)state.m_61143_((Property)LecternBlock.f_54467_)).booleanValue()) {
            return false;
        }
        return !this.sides(state).isEmpty();
    }

    public boolean usedOn(BlockState state, Direction face) {
        return this != CHISELED_BOOKSHELF || face == state.m_61143_((Property)HorizontalDirectionalBlock.f_54117_);
    }

    public static boolean roomAt(BlockGetter level, BlockPos stand) {
        return level.m_8055_(stand).m_60812_(level, stand).m_83297_(Direction.Axis.Y) <= 0.2 && level.m_8055_(stand.m_7494_()).m_60812_(level, stand.m_7494_()).m_83281_();
    }

    static {
        ANY_SIDE = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    }
}

