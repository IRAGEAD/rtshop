/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.SurfaceAppearance;
import dev.dyslexic.cinematic.client.SurfaceEditor;
import dev.dyslexic.cinematic.client.SurfaceLayouts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;

abstract class SurfaceStation<M extends AbstractContainerMenu>
extends MenuStation<M> {
    final SurfaceEditor gridEditor = new SurfaceEditor(this);

    SurfaceStation(BlockPos pos, Direction side, Class<M> menuType, int inputs) {
        super(pos, side, menuType, inputs);
    }

    SurfaceAppearance geometry() {
        return SurfaceLayouts.current(this.kind());
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return this.geometry().point(this.kind(), this.center(), this.side, slot).m_82549_(this.right().m_82490_(this.denyShake()));
    }

    float surfaceScale(int slot) {
        return (float)this.geometry().region(slot).itemScale();
    }

    float surfaceRotation(int slot) {
        return (float)this.geometry().region(slot).rotation();
    }

    Vec3 surfaceRight(int slot) {
        return this.geometry().axis(this.side, slot, true);
    }

    Vec3 surfaceOut(int slot) {
        return this.geometry().axis(this.side, slot, false);
    }

    @Override
    public boolean busy() {
        return super.busy() || this.gridEditor.editing();
    }

    @Override
    public boolean showsInventory() {
        return !this.gridEditor.visible();
    }

    @Override
    public void beforeFrame() {
        this.gridEditor.frame();
    }

    @Override
    public void onEnd() {
        this.gridEditor.cancel();
        super.onEnd();
    }

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        this.gridEditor.render(g, font, alpha);
    }

    @Override
    public List<Vec3> workSamples() {
        if (this.geometry().equals(SurfaceAppearance.VANILLA)) {
            return super.workSamples();
        }
        ArrayList<Vec3> points = new ArrayList<Vec3>(super.workSamples());
        for (int i = 0; i < SurfaceAppearance.count(this.kind()); ++i) {
            points.add(this.restPoint(i).m_82520_(0.0, 0.04, 0.0));
        }
        return points;
    }

    int surfaceSlot(boolean occupiedOnly) {
        int selected = -1;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < SurfaceAppearance.count(this.kind()); ++i) {
            double d;
            if (occupiedOnly && this.slot(i).m_41619_()) continue;
            Vec3 at = this.restPoint(i);
            Vec3 hit = Picking.cursorOnPlane(at.f_82480_);
            if (hit == null || !this.geometry().contains(this.kind(), this.center(), this.side, i, hit) || !((d = hit.m_82557_(Picking.camera())) < distance)) continue;
            distance = d;
            selected = i;
        }
        return selected;
    }

    float heldItemScale(int target) {
        return target < 0 ? 1.0f : this.surfaceScale(target);
    }

    abstract float heldItemScale();
}

