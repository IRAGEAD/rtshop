/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.table.TableGrid;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

record TableLayout(double x, double z, double rotation, double width, double depth, double height, double itemScale) {
    static final TableLayout VANILLA = new TableLayout(0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0);

    TableLayout {
        rotation = TableLayout.finite(rotation, 0.0, -180.0, 180.0);
        width = TableLayout.finite(width, 1.0, 0.5, 1.7);
        depth = TableLayout.finite(depth, 1.0, 0.5, 1.7);
        height = TableLayout.finite(height, 0.0, -2.0, 2.0);
        itemScale = TableLayout.finite(itemScale, 1.0, 0.5, 1.5);
        double c = Math.abs(Math.cos(Math.toRadians(rotation)));
        double s = Math.abs(Math.sin(Math.toRadians(rotation)));
        double half = 0.28125;
        double extentX = half * (c * width + s * depth);
        double extentZ = half * (s * width + c * depth);
        double fit = Math.min(1.0, 0.48 / Math.max(extentX, extentZ));
        width *= fit;
        depth *= fit;
        double maxX = Math.max(0.0, (0.49 - (extentX *= fit)) * 16.0);
        double maxZ = Math.max(0.0, (0.49 - (extentZ *= fit)) * 16.0);
        x = TableLayout.finite(x, 0.0, -maxX, maxX);
        z = TableLayout.finite(z, 0.0, -maxZ, maxZ);
    }

    private static double finite(double value, double fallback, double min, double max) {
        return Mth.m_14008_((double)(Double.isFinite(value) ? value : fallback), (double)min, (double)max);
    }

    Vec3 center(BlockPos pos) {
        return new Vec3((double)pos.m_123341_() + 0.5 + this.x / 16.0, this.surfaceY(pos), (double)pos.m_123343_() + 0.5 + this.z / 16.0);
    }

    double surfaceY(BlockPos pos) {
        return (double)(pos.m_123342_() + 1) + this.height / 16.0;
    }

    Vec3 point(BlockPos pos, double localX, double localZ) {
        double a = Math.toRadians(this.rotation);
        double c = Math.cos(a);
        double s = Math.sin(a);
        double dx = (localX - 0.5) * this.width;
        double dz = (localZ - 0.5) * this.depth;
        return this.center(pos).m_82520_(dx * c + dz * s, 0.0, -dx * s + dz * c);
    }

    Vec3 cell(BlockPos pos, int index) {
        return this.point(pos, TableGrid.cellX(index), TableGrid.cellZ(index));
    }

    int cellAt(BlockPos pos, Vec3 world) {
        Vec3 d = world.m_82546_(this.center(pos));
        double a = Math.toRadians(this.rotation);
        double c = Math.cos(a);
        double s = Math.sin(a);
        return TableGrid.cellAt(0.5 + (d.f_82479_ * c - d.f_82481_ * s) / this.width, 0.5 + (d.f_82479_ * s + d.f_82481_ * c) / this.depth);
    }

    void cellPose(PoseStack pose, BlockPos pos, int index, Vec3 camera) {
        Vec3 at = this.cell(pos, index).m_82546_(camera);
        pose.m_85837_(at.f_82479_, at.f_82480_ + 0.002, at.f_82481_);
        pose.m_252781_(Axis.f_252436_.m_252977_((float)this.rotation));
        pose.m_85841_((float)this.width, 1.0f, (float)this.depth);
    }

    void renderItem(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ItemStack stack, Vec3 base, Direction facing, long seed, float scale, float tilt, int light, float opacity, boolean one) {
        if (this.rotation == 0.0) {
            if (one) {
                ItemPlacer.renderOne(pose, buffers, camera, stack, base, facing, seed, scale * (float)this.itemScale, tilt, light);
            } else {
                ItemPlacer.render(pose, buffers, camera, stack, base, facing, seed, scale * (float)this.itemScale, tilt, light, opacity);
            }
            return;
        }
        pose.m_85836_();
        Vec3 d = base.m_82546_(camera);
        pose.m_85837_(d.f_82479_, d.f_82480_, d.f_82481_);
        pose.m_252781_(Axis.f_252436_.m_252977_((float)this.rotation));
        pose.m_85837_(-d.f_82479_, -d.f_82480_, -d.f_82481_);
        if (one) {
            ItemPlacer.renderOne(pose, buffers, camera, stack, base, facing, seed, scale * (float)this.itemScale, tilt, light);
        } else {
            ItemPlacer.render(pose, buffers, camera, stack, base, facing, seed, scale * (float)this.itemScale, tilt, light, opacity);
        }
        pose.m_85849_();
    }

    AABB bounds(ItemStack stack, Vec3 base) {
        AABB original = ItemPlacer.restingBounds(stack, base);
        double half = Math.max(base.f_82479_ - original.f_82288_, base.f_82481_ - original.f_82290_) * Math.max(1.0, this.itemScale) * 1.42;
        return new AABB(base.f_82479_ - half, base.f_82480_, base.f_82481_ - half, base.f_82479_ + half, base.f_82480_ + (original.f_82292_ - base.f_82480_) * this.itemScale, base.f_82481_ + half);
    }

    double value(int field) {
        return switch (field) {
            case 0 -> this.x;
            case 1 -> this.z;
            case 2 -> this.rotation;
            case 3 -> this.width * 100.0;
            case 4 -> this.depth * 100.0;
            case 5 -> this.height;
            case 6 -> this.itemScale * 100.0;
            default -> throw new IllegalArgumentException("Layout field " + field);
        };
    }

    TableLayout with(int field, double value) {
        return new TableLayout(field == 0 ? value : this.x, field == 1 ? value : this.z, field == 2 ? value : this.rotation, field == 3 ? value / 100.0 : this.width, field == 4 ? value / 100.0 : this.depth, field == 5 ? value : this.height, field == 6 ? value / 100.0 : this.itemScale);
    }

    TableLayout blend(TableLayout target, double t) {
        return new TableLayout(Mth.m_14139_((double)t, (double)this.x, (double)target.x), Mth.m_14139_((double)t, (double)this.z, (double)target.z), Mth.m_14139_((double)t, (double)this.rotation, (double)target.rotation), Mth.m_14139_((double)t, (double)this.width, (double)target.width), Mth.m_14139_((double)t, (double)this.depth, (double)target.depth), Mth.m_14139_((double)t, (double)this.height, (double)target.height), Mth.m_14139_((double)t, (double)this.itemScale, (double)target.itemScale));
    }
}

