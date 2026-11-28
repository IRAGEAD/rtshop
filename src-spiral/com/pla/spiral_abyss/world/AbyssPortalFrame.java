/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.core.Position
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Half
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import com.pla.spiral_abyss.block.AbyssGateCurveBlock;
import com.pla.spiral_abyss.block.AbyssGateSlabBlock;
import com.pla.spiral_abyss.block.AbyssGateVerticalBlock;
import com.pla.spiral_abyss.block.AbyssPortalBlock;
import com.pla.spiral_abyss.init.BlockInit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public record AbyssPortalFrame(BlockPos base, Direction front) {
    public static final int TOP = 5;
    public static final int RADIUS = 2;

    public AbyssPortalFrame {
        base = base.m_7949_();
        if (!front.m_122434_().m_122479_()) {
            throw new IllegalArgumentException("Horizontal portal facing required");
        }
    }

    public Direction.Axis axis() {
        return this.front.m_122434_() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    public BlockPos anchor() {
        return this.at(0, 5);
    }

    public static AbyssPortalFrame fromActivation(BlockPos pos, BlockState state) {
        Direction front = (Direction)state.m_61143_((Property)AbyssGateActivationBlock.f_54117_);
        return new AbyssPortalFrame(pos.m_6625_(5), front);
    }

    private boolean hasActivator(Level level) {
        BlockState state = level.m_8055_(this.anchor());
        return state.m_60713_((Block)BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get()) && state.m_61143_((Property)AbyssGateActivationBlock.f_54117_) == this.front;
    }

    public BlockPos at(int across, int up) {
        return this.base.m_7918_(this.axis() == Direction.Axis.X ? across : 0, up, this.axis() == Direction.Axis.Z ? across : 0);
    }

    public List<BlockPos> opening() {
        ArrayList<BlockPos> result = new ArrayList<BlockPos>(12);
        for (int y = 1; y < 5; ++y) {
            int width = 1;
            for (int x = -width; x <= width; ++x) {
                result.add(this.at(x, y));
            }
        }
        return result;
    }

    public boolean loaded(Level level) {
        return level.m_46832_(this.at(-2, 0), this.at(2, 5)) && level.m_46805_(this.anchor());
    }

    public boolean validFrame(Level level) {
        if (!this.loaded(level)) {
            return false;
        }
        for (int y = 0; y <= 5; ++y) {
            for (int x = -2; x <= 2; ++x) {
                BlockState expected = this.expectedFrameState(x, y);
                if (expected == null) continue;
                BlockState actual = level.m_8055_(this.at(x, y));
                if (actual.m_60713_((Block)BlockInit.ABYSS_GATE_HORIZONTAL.get())) {
                    actual = (BlockState)actual.m_61124_(AbyssGateSlabBlock.EXTENSION, (Comparable)((Object)AbyssGateSlabBlock.Extension.NONE));
                }
                if (actual.equals(expected)) continue;
                return false;
            }
        }
        return true;
    }

    public BlockState expectedFrameState(int x, int y) {
        if ((y == 0 || y == 5 && x != 0) && Math.abs(x) <= 1) {
            boolean bottom = y == 0 == (x == 0);
            return (BlockState)((BlockState)((AbyssGateSlabBlock)((Object)BlockInit.ABYSS_GATE_HORIZONTAL.get())).m_49966_().m_61124_(AbyssGateSlabBlock.AXIS, (Comparable)this.axis())).m_61124_((Property)SlabBlock.f_56353_, (Comparable)(bottom ? SlabType.BOTTOM : SlabType.TOP));
        }
        if ((y == 1 || y == 4) && Math.abs(x) == 2) {
            Direction inward;
            Direction direction = inward = this.axis() == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
            if (x < 0) {
                inward = inward.m_122424_();
            }
            return (BlockState)((BlockState)((AbyssGateCurveBlock)((Object)BlockInit.ABYSS_GATE_CURVE.get())).m_49966_().m_61124_((Property)AbyssGateCurveBlock.f_54117_, (Comparable)inward)).m_61124_(AbyssGateCurveBlock.HALF, (Comparable)(y == 1 ? Half.TOP : Half.BOTTOM));
        }
        if (y >= 2 && y < 4 && Math.abs(x) == 2) {
            Direction outward;
            Direction direction = outward = this.axis() == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            if (x < 0) {
                outward = outward.m_122424_();
            }
            return (BlockState)((AbyssGateVerticalBlock)((Object)BlockInit.ABYSS_GATE_VERTICAL.get())).m_49966_().m_61124_((Property)AbyssGateVerticalBlock.f_54117_, (Comparable)outward);
        }
        return null;
    }

    public boolean active(Level level) {
        return this.loaded(level) && this.hasActivator(level) && (Boolean)level.m_8055_(this.anchor()).m_61143_((Property)AbyssGateActivationBlock.ACTIVE) != false && this.validFrame(level) && this.opening().stream().allMatch(p -> this.portalCell(level.m_8055_(p)));
    }

    private boolean portalCell(BlockState state) {
        return state.m_60713_((Block)BlockInit.ABYSS_PORTAL.get()) && state.m_61143_(AbyssPortalBlock.AXIS) == this.axis();
    }

    public boolean activate(ServerLevel level) {
        if (!(level.m_46472_().equals(Level.f_46428_) && this.validFrame((Level)level) && this.hasActivator((Level)level) && this.opening().stream().allMatch(p -> level.m_8055_(p).m_60795_() || this.portalCell(level.m_8055_(p))) && level.m_6857_().m_61937_(this.anchor()) && level.m_6857_().m_61937_(this.at(-2, 0)) && level.m_6857_().m_61937_(this.at(2, 5)))) {
            return false;
        }
        level.m_7731_(this.anchor(), (BlockState)level.m_8055_(this.anchor()).m_61124_((Property)AbyssGateActivationBlock.ACTIVE, (Comparable)Boolean.valueOf(true)), 2);
        for (BlockPos pos : this.opening()) {
            level.m_7731_(pos, (BlockState)((AbyssPortalBlock)((Object)BlockInit.ABYSS_PORTAL.get())).m_49966_().m_61124_(AbyssPortalBlock.AXIS, (Comparable)this.axis()), 2);
        }
        if (this.active((Level)level)) {
            return true;
        }
        this.deactivate((Level)level);
        return false;
    }

    public void deactivate(Level level) {
        this.clearOpening(level);
        if (level.m_46805_(this.anchor()) && this.hasActivator(level)) {
            level.m_7731_(this.anchor(), (BlockState)level.m_8055_(this.anchor()).m_61124_((Property)AbyssGateActivationBlock.ACTIVE, (Comparable)Boolean.valueOf(false)), 2);
        }
    }

    public void clearOpening(Level level) {
        for (BlockPos pos : this.opening()) {
            if (!level.m_46805_(pos) || !this.portalCell(level.m_8055_(pos))) continue;
            level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 2);
        }
    }

    public static Optional<AbyssPortalFrame> fromOpening(Level level, BlockPos pos, Direction.Axis axis) {
        for (int y = 1; y < 5; ++y) {
            int width = 1;
            for (int x = -width; x <= width; ++x) {
                BlockPos base = pos.m_7918_(axis == Direction.Axis.X ? -x : 0, -y, axis == Direction.Axis.Z ? -x : 0);
                for (Direction front : Direction.Plane.HORIZONTAL) {
                    AbyssPortalFrame frame;
                    if (front.m_122434_() == axis || !level.m_46805_((frame = new AbyssPortalFrame(base, front)).anchor()) || !frame.hasActivator(level) || !((Boolean)level.m_8055_(frame.anchor()).m_61143_((Property)AbyssGateActivationBlock.ACTIVE)).booleanValue()) continue;
                    return Optional.of(frame);
                }
            }
        }
        return Optional.empty();
    }

    public Vec3 returnPosition(ServerPlayer player) {
        Level level = player.m_9236_();
        double side = this.axis() == Direction.Axis.X ? player.m_20189_() - (double)this.base.m_123343_() - 0.5 : player.m_20185_() - (double)this.base.m_123341_() - 0.5;
        int sign = side < 0.0 ? -1 : 1;
        for (int facing : new int[]{sign, -sign}) {
            for (int distance = 2; distance <= 4; ++distance) {
                BlockPos column = this.base.m_7918_(this.axis() == Direction.Axis.Z ? facing * distance : 0, 0, this.axis() == Direction.Axis.X ? facing * distance : 0);
                for (int y = 2; y >= -4; --y) {
                    VoxelShape shape;
                    BlockPos floor = column.m_6630_(y);
                    if (!level.m_46805_(floor) || (shape = level.m_8055_(floor).m_60812_((BlockGetter)level, floor)).m_83281_()) continue;
                    Vec3 candidate = new Vec3((double)floor.m_123341_() + 0.5, (double)floor.m_123342_() + shape.m_83297_(Direction.Axis.Y), (double)floor.m_123343_() + 0.5);
                    if (!level.m_6857_().m_61937_(BlockPos.m_274446_((Position)candidate)) || !level.m_6425_(floor).m_76178_() || level.m_46855_(player.m_20191_().m_82383_(candidate.m_82546_(player.m_20182_()))) || !level.m_45756_((Entity)player, player.m_20191_().m_82383_(candidate.m_82546_(player.m_20182_())))) continue;
                    return candidate;
                }
            }
        }
        return player.m_20182_();
    }
}

