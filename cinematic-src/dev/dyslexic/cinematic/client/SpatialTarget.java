/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.BrewingBottles;
import dev.dyslexic.cinematic.client.BrewingStation;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialEditor;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

interface SpatialTarget {
    default public ClientStation layoutStation() {
        return (ClientStation)((Object)this);
    }

    public SpatialEditor layoutEditor();

    default public SpatialAppearance.Frame layoutFrame() {
        ClientStation station = this.layoutStation();
        return SpatialAppearance.Frame.of(station.kind(), station.pos, station.sideDirection(), Direction.UP);
    }

    default public ItemStack layoutItem(int slot) {
        return RemainingLayouts.item(this.layoutStation(), slot);
    }

    default public SpatialAppearance geometry() {
        return SpatialLayouts.current(this.layoutStation().kind());
    }

    default public Vec3 layoutPoint(int slot) {
        return this.geometry().point(this.layoutFrame(), slot);
    }

    default public Vec3 layoutNormal(int slot) {
        return RemainingLayouts.billboard(this.layoutStation().kind(), slot) ? Picking.camera().m_82546_(this.layoutPoint(slot)).m_82541_() : this.layoutFrame().normal();
    }

    default public Vec3 layoutAxis(int slot, boolean across) {
        if (!RemainingLayouts.billboard(this.layoutStation().kind(), slot)) {
            return this.geometry().axis(this.layoutFrame(), slot, across);
        }
        Vec3 n = this.layoutNormal(slot);
        Vec3 a = new Vec3(0.0, 1.0, 0.0).m_82537_(n).m_82541_();
        Vec3 b = n.m_82537_(a);
        double angle = Math.toRadians(this.geometry().rotation(slot));
        return across ? a.m_82490_(Math.cos(angle)).m_82549_(b.m_82490_(Math.sin(angle))) : b.m_82490_(Math.cos(angle)).m_82546_(a.m_82490_(Math.sin(angle)));
    }

    default public List<Vec3> layoutSamples(List<Vec3> original) {
        if (this.geometry().equals(SpatialAppearance.VANILLA)) {
            return original;
        }
        ArrayList<Vec3> points = new ArrayList<Vec3>(original);
        for (int i = 0; i < SpatialAppearance.count(this.layoutStation().kind()); ++i) {
            points.add(this.layoutPoint(i).m_82549_(this.layoutFrame().normal().m_82490_(0.04)));
        }
        return points;
    }

    default public int layoutPick() {
        if (this.layoutStation().kind() == StationKind.BREWING) {
            return this.volumePick();
        }
        int best = -1;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < SpatialAppearance.count(this.layoutStation().kind()); ++i) {
            double d;
            Vec3 rel;
            Vec3 hit = Picking.cursorOnPlane(this.layoutPoint(i), this.layoutNormal(i));
            double[] half = this.geometry().half(this.layoutFrame(), i);
            Vec3 vec3 = rel = hit == null ? null : hit.m_82546_(this.layoutPoint(i));
            if (rel == null || !(Math.abs(rel.m_82526_(this.layoutAxis(i, true))) <= half[0]) || !(Math.abs(rel.m_82526_(this.layoutAxis(i, false))) <= half[1])) continue;
            double d2 = d = RemainingLayouts.supports(this.layoutStation().kind()) ? Math.pow(rel.m_82526_(this.layoutAxis(i, true)) / half[0], 2.0) + Math.pow(rel.m_82526_(this.layoutAxis(i, false)) / half[1], 2.0) + hit.m_82557_(Picking.camera()) * 1.0E-7 : hit.m_82557_(Picking.camera());
            if (!(d < distance)) continue;
            best = i;
            distance = d;
        }
        return best;
    }

    private int volumePick() {
        Vec3[] ray = Picking.cursorRay();
        if (ray == null) {
            return -1;
        }
        int best = -1;
        double nearest = Double.MAX_VALUE;
        SpatialAppearance.Frame frame = this.layoutFrame();
        SpatialAppearance layout = this.geometry();
        for (int i = 0; i < 5; ++i) {
            Vec3 origin = ray[0].m_82546_(this.layoutPoint(i));
            double[] half = layout.half(frame, i);
            double[] lengths = new double[]{half[0], half[1], (i < 3 ? 0.12 : 0.065) * (double)layout.scale(i)};
            Vec3[] axes = new Vec3[]{layout.axis(frame, i, true), layout.axis(frame, i, false), frame.normal()};
            double enter = 0.0;
            double leave = Double.MAX_VALUE;
            for (int a = 0; a < 3; ++a) {
                double p = origin.m_82526_(axes[a]);
                double d = ray[1].m_82526_(axes[a]);
                if (Math.abs(d) < 1.0E-9) {
                    if (!(Math.abs(p) > lengths[a])) continue;
                    leave = -1.0;
                    break;
                }
                double t0 = (-lengths[a] - p) / d;
                double t1 = (lengths[a] - p) / d;
                enter = Math.max(enter, Math.min(t0, t1));
                leave = Math.min(leave, Math.max(t0, t1));
            }
            if (!(leave >= enter) || !(enter < nearest)) continue;
            nearest = enter;
            best = i;
        }
        return best;
    }

    default public float layoutHeldScale() {
        return RemainingLayouts.supports(this.layoutStation().kind()) ? this.geometry().scale(0) : 1.0f;
    }

    default public void layoutGhost(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int slot, int light) {
        StationKind kind = this.layoutStation().kind();
        if (RemainingLayouts.supports(kind)) {
            RemainingLayouts.ghost(this, pose, buffers, camera, slot, light);
            return;
        }
        ItemStack item = new ItemStack((ItemLike)(slot < 3 ? Items.f_42590_ : (slot == 3 ? Items.f_42588_ : Items.f_42593_)));
        if (slot < 3) {
            BrewingBottles.draw(pose, buffers, camera, camera, item, this.layoutPoint(slot), 1.0f, this.geometry().scale(slot), BrewingStation.itemRotation(slot), light);
        } else {
            ArrayList<ItemStack> items = new ArrayList<ItemStack>(Collections.nCopies(5, ItemStack.f_41583_));
            items.set(slot, item);
            BrewingStation.draw(pose, buffers, Minecraft.m_91087_().f_91063_.m_109153_(), this.layoutStation().pos, this.layoutStation().sideDirection(), items, -1, -1.0, light);
        }
    }
}

