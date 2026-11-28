/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.ClientFurnaces;
import dev.dyslexic.cinematic.client.EditorIcon;
import dev.dyslexic.cinematic.client.FurnaceAppearance;
import dev.dyslexic.cinematic.client.FurnaceLayouts;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.LayoutEditor;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.TableGridEditor;
import dev.dyslexic.cinematic.furnace.FurnaceLayout;
import dev.dyslexic.cinematic.furnace.FurnaceZones;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

final class FurnaceGridEditor
implements LayoutEditor {
    private static final int W = 180;
    private static final int H = 342;
    private static final int ROW_Y = 112;
    private static final int ROW_H = 24;
    private static final String[] FIELDS = new String[]{"offset_u", "offset_v", "width", "height", "out", "item_size", "click_width", "click_height", "item_u", "item_v"};
    private static final String[] REGIONS = new String[]{"input", "fuel", "result", "cooking", "burning"};
    private static final int[] BASIC = new int[]{0, 1, 2, 3, 4, 5};
    private static final int[] ADVANCED = new int[]{6, 7, 8, 9};
    private static final int[] GAUGE = new int[]{0, 1, 2, 3, 4};
    private final FurnaceStation station;
    private FurnaceAppearance[] drafts;
    private int profile;
    private int region;
    private int slider = -1;
    private boolean open;
    private boolean advanced;
    private boolean saveError;
    private double shown;
    private double x;
    private double y;
    private double toggleX;
    private double toggleY;
    private double tooltipAt;
    private float scale;
    private int tooltipField = -1;
    private Vec3 dragFrom;
    private FurnaceAppearance dragLayout;

    FurnaceGridEditor(FurnaceStation station) {
        this.station = station;
        this.layout();
    }

    static Component text(String key, Object ... args) {
        return Component.m_237110_((String)("cinematic.furnace_editor." + key), (Object[])args);
    }

    @Override
    public boolean editing() {
        return this.open;
    }

    @Override
    public boolean visible() {
        return this.open || this.shown > 0.01;
    }

    double amount() {
        return this.shown;
    }

    private int[] fields() {
        return this.region >= 3 ? GAUGE : (this.advanced ? ADVANCED : BASIC);
    }

    private static boolean percent(int field) {
        return field == 2 || field == 3 || field == 5 || field == 6 || field == 7;
    }

    private static double min(int field) {
        return FurnaceGridEditor.percent(field) ? 50.0 : (field == 4 ? -2.0 : (field >= 8 ? -3.0 : -6.0));
    }

    private static double max(int field) {
        return FurnaceGridEditor.percent(field) ? 150.0 : -FurnaceGridEditor.min(field);
    }

    private static double step(int field) {
        return FurnaceGridEditor.percent(field) ? 1.0 : 0.125;
    }

    private void layout() {
        Window window = Minecraft.m_91087_().m_91268_();
        int width = window.m_85445_();
        int height = window.m_85446_();
        this.scale = Math.min(1.0f, Math.min((float)height / 450.0f, (float)width / 700.0f));
        this.x = (double)((float)(width - 8) - 180.0f * this.scale) + (1.0 - this.shown) * 192.0 * (double)this.scale;
        this.y = (double)height * 0.14;
        this.toggleX = (float)(width - 8) - 30.0f * this.scale;
        this.toggleY = (double)height * 0.45;
    }

    private double lx(double mx) {
        return (mx - this.x) / (double)this.scale;
    }

    private double ly(double my) {
        return (my - this.y) / (double)this.scale;
    }

    private boolean inPanel(double mx, double my) {
        return this.visible() && this.lx(mx) >= 0.0 && this.lx(mx) < 180.0 && this.ly(my) >= 0.0 && this.ly(my) < 342.0;
    }

    private boolean toggle(double mx, double my) {
        return Settings.developerMode() && !this.visible() && mx >= this.toggleX && mx < this.toggleX + (double)(30.0f * this.scale) && my >= this.toggleY && my < this.toggleY + (double)(28.0f * this.scale);
    }

    @Override
    public boolean over(double mx, double my) {
        return this.open || this.inPanel(mx, my) || this.toggle(mx, my);
    }

    void frame() {
        int field;
        if (this.open && (!Settings.developerMode() || Scene.phase != Scene.Phase.ACTIVE)) {
            this.cancel();
        }
        this.shown = Anim.approach(this.shown, this.open ? 1.0 : 0.0, 16.0, Scene.frameDt);
        this.layout();
        double[] mouse = Picking.mouseGui();
        int row = (int)((this.ly(mouse[1]) - 112.0) / 24.0);
        int n = field = this.open && this.inPanel(mouse[0], mouse[1]) && this.ly(mouse[1]) >= 112.0 && row < this.fields().length && (this.ly(mouse[1]) - 112.0) % 24.0 < 11.0 && this.slider < 0 ? this.fields()[row] : -1;
        if (field != this.tooltipField) {
            this.tooltipField = field;
            this.tooltipAt = Scene.clock();
        }
    }

    void open() {
        if (!Settings.developerMode() || this.station.busy() || Scene.phase != Scene.Phase.ACTIVE) {
            return;
        }
        this.drafts = FurnaceLayouts.profiles(this.station.kind());
        this.profile = FurnaceLayouts.selected(this.station.kind());
        this.open = true;
        this.advanced = false;
        this.saveError = false;
        FurnaceLayouts.preview(this.station.kind(), this.drafts[this.profile]);
        this.station.clearCursor();
    }

    void cancel() {
        this.open = false;
        this.release();
        FurnaceLayouts.cancelPreview(this.station.kind());
    }

    private void save() {
        if (FurnaceLayouts.save(this.station.kind(), this.drafts, this.profile)) {
            this.cancel();
        } else {
            this.saveError = true;
        }
    }

    private void change(int field, double value) {
        this.drafts[this.profile] = this.drafts[this.profile].with(this.region, this.drafts[this.profile].region(this.region).with(field, value));
        FurnaceLayouts.preview(this.station.kind(), this.drafts[this.profile]);
        this.saveError = false;
    }

    private void slide(double mx) {
        double t = Mth.m_14008_((double)((this.lx(mx) - 11.0) / 158.0), (double)0.0, (double)1.0);
        this.change(this.slider, (double)Math.round(Mth.m_14139_((double)t, (double)FurnaceGridEditor.min(this.slider), (double)FurnaceGridEditor.max(this.slider)) / FurnaceGridEditor.step(this.slider)) * FurnaceGridEditor.step(this.slider));
    }

    @Override
    public boolean click(double mx, double my, int button) {
        this.layout();
        if (this.toggle(mx, my)) {
            if (button == 0) {
                this.open();
            }
            return true;
        }
        if (!this.open) {
            return this.inPanel(mx, my);
        }
        if (button != 0) {
            return true;
        }
        if (this.inPanel(mx, my)) {
            double px = this.lx(mx);
            double py = this.ly(my);
            if (py >= 29.0 && py <= 48.0) {
                this.profile = Math.floorMod(this.profile + (px < 90.0 ? -1 : 1), 3);
                FurnaceLayouts.preview(this.station.kind(), this.drafts[this.profile]);
            } else if (py >= 55.0 && py <= 75.0) {
                this.region = Math.floorMod(this.region + (px < 90.0 ? -1 : 1), 5);
                this.release();
            } else if (py >= 81.0 && py <= 99.0 && this.region < 3) {
                this.advanced = !this.advanced;
            } else if (py >= 112.0 && py < (double)(112 + this.fields().length * 24)) {
                this.slider = this.fields()[(int)((py - 112.0) / 24.0)];
                this.slide(mx);
            } else if (py >= 290.0 && py <= 306.0) {
                this.drafts[this.profile] = FurnaceAppearance.VANILLA;
                FurnaceLayouts.preview(this.station.kind(), this.drafts[this.profile]);
                this.saveError = false;
            } else if (py >= 314.0 && py <= 332.0) {
                if (px < 90.0) {
                    this.save();
                } else {
                    this.cancel();
                }
            }
        } else {
            FurnaceStation.ZoneHit hit = this.station.zoneHit(true);
            if (hit != null) {
                this.region = hit.index();
                this.dragFrom = hit.world();
                this.dragLayout = this.drafts[this.profile];
            }
        }
        return true;
    }

    @Override
    public boolean drag(double mx, double my, int button) {
        FurnaceZones.Face face;
        Vec3 normal;
        Vec3 hit;
        if (!this.open) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        if (this.slider >= 0) {
            this.slide(mx);
        } else if (this.dragFrom != null && (hit = Picking.cursorOnPlane(this.dragFrom, normal = (face = this.dragLayout.face(this.station.layout, this.region)) == FurnaceZones.Face.FRONT ? this.station.side() : new Vec3(0.0, 1.0, 0.0))) != null) {
            Vec3 delta = hit.m_82546_(this.dragFrom);
            FurnaceAppearance.Region base = this.dragLayout.region(this.region);
            double u = base.u() + delta.m_82526_(this.station.right()) * 16.0;
            double v = base.v() + (face == FurnaceZones.Face.FRONT ? delta.f_82480_ : -delta.m_82526_(this.station.side())) * 16.0;
            this.drafts[this.profile] = this.dragLayout.with(this.region, base.with(0, u).with(1, v));
            FurnaceLayouts.preview(this.station.kind(), this.drafts[this.profile]);
            this.saveError = false;
        }
        return true;
    }

    @Override
    public void release() {
        this.slider = -1;
        this.dragFrom = null;
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        if (!this.open) {
            return false;
        }
        int row = (int)((this.ly(my) - 112.0) / 24.0);
        if (this.inPanel(mx, my) && this.ly(my) >= 112.0 && row < this.fields().length) {
            int field = this.fields()[row];
            this.change(field, this.drafts[this.profile].region(this.region).value(field) + Math.signum(amount) * FurnaceGridEditor.step(field));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key) {
        if (this.open) {
            if (key == 256) {
                this.cancel();
            } else if (key == 257 || key == 335) {
                this.save();
            }
            return true;
        }
        if (Settings.developerMode() && key == 71) {
            this.open();
            return true;
        }
        return false;
    }

    List<Component> tooltip(double mx, double my) {
        if (this.toggle(mx, my)) {
            return List.of(FurnaceGridEditor.text("open", new Object[0]), TableGridEditor.text("shortcut", new Object[0]));
        }
        if (this.open && this.tooltipField >= 0 && Scene.clock() - this.tooltipAt > 0.55) {
            return List.of(FurnaceGridEditor.text(FIELDS[this.tooltipField] + ".tooltip", new Object[0]));
        }
        return null;
    }

    void render(GuiGraphics g, Font font, float alpha) {
        this.layout();
        if (!Settings.developerMode() && !this.visible()) {
            return;
        }
        double[] mouse = Picking.mouseGui();
        if (!this.visible()) {
            g.m_280168_().m_85836_();
            g.m_280168_().m_85837_(this.toggleX, this.toggleY, 0.0);
            g.m_280168_().m_85841_(this.scale, this.scale, 1.0f);
            FurnaceGridEditor.glass(g, 30, 28, alpha);
            if (this.toggle(mouse[0], mouse[1])) {
                g.m_280509_(1, 1, 29, 27, FurnaceGridEditor.color((double)alpha * 0.12, 255, 255, 255));
            }
            EditorIcon.draw(g, alpha);
            g.m_280168_().m_85849_();
            return;
        }
        float a = alpha * (float)this.shown;
        if (a * 255.0f < 8.0f || this.drafts == null) {
            return;
        }
        g.m_280168_().m_85836_();
        g.m_280168_().m_85837_(this.x, this.y, 20.0);
        g.m_280168_().m_85841_(this.scale, this.scale, 1.0f);
        FurnaceGridEditor.glass(g, 180, 342, a);
        g.m_280614_(font, FurnaceGridEditor.text("title", new Object[0]), 10, 10, FurnaceGridEditor.color(a, 242, 234, 216), false);
        this.selector(g, font, 29, TableGridEditor.text("profile", this.profile + 1), a);
        this.selector(g, font, 55, FurnaceGridEditor.text(REGIONS[this.region], new Object[0]), a);
        this.button(g, font, 9, 81, 162, 18, FurnaceGridEditor.text(this.region >= 3 ? "indicators" : (this.advanced ? "basic" : "advanced"), new Object[0]), a, this.advanced && this.region < 3);
        int[] fields = this.fields();
        for (int row = 0; row < fields.length; ++row) {
            int field = fields[row];
            int yy = 112 + row * 24;
            double value = this.drafts[this.profile].region(this.region).value(field);
            String number = FurnaceGridEditor.percent(field) ? String.format(Locale.ROOT, "%.0f%%", value) : String.format(Locale.ROOT, "%+.2f px", value);
            g.m_280614_(font, FurnaceGridEditor.text(FIELDS[field], new Object[0]), 10, yy, FurnaceGridEditor.color((double)a * 0.9, 224, 227, 226), false);
            g.m_280056_(font, number, 170 - font.m_92895_(number), yy, FurnaceGridEditor.color(a, 103, 211, 231), false);
            int end = 11 + (int)Math.round(158.0 * (value - FurnaceGridEditor.min(field)) / (FurnaceGridEditor.max(field) - FurnaceGridEditor.min(field)));
            g.m_280509_(11, yy + 15, 169, yy + 17, FurnaceGridEditor.color((double)a * 0.14, 255, 255, 255));
            g.m_280509_(11, yy + 15, end, yy + 17, FurnaceGridEditor.color((double)a * 0.75, 103, 211, 231));
            g.m_280509_(end - 2, yy + 12, end + 2, yy + 20, FurnaceGridEditor.color(a, 196, 240, 245));
        }
        g.m_280614_(font, FurnaceGridEditor.text(this.saveError ? "save_error" : "drag_hint", new Object[0]), 10, 272, FurnaceGridEditor.color((double)a * 0.75, this.saveError ? 255 : 181, this.saveError ? 122 : 195, this.saveError ? 112 : 199), false);
        this.button(g, font, 9, 290, 162, 16, FurnaceGridEditor.text("reset", new Object[0]), a, false);
        this.button(g, font, 9, 314, 78, 18, TableGridEditor.text("save", new Object[0]), a, true);
        this.button(g, font, 93, 314, 78, 18, TableGridEditor.text("cancel", new Object[0]), a, false);
        g.m_280168_().m_85849_();
        if (this.open) {
            for (int i = 0; i < 5; ++i) {
                double[] at = Picking.toGui(this.station.geometry().center(this.station.layout, this.station.pos, this.station.sideDirection(), i));
                if (at == null || this.inPanel(at[0], at[1])) continue;
                int px = (int)at[0];
                int py = (int)at[1];
                g.m_280509_(px - 4, py - 4, px + 4, py + 5, FurnaceGridEditor.color((double)a * 0.6, 12, 16, 19));
                g.m_280137_(font, Integer.toString(i + 1), px, py - 3, FurnaceGridEditor.color((double)a * (i == this.region ? 1.0 : 0.45), 196, 240, 245));
            }
        }
    }

    private void selector(GuiGraphics g, Font font, int yy, Component label, float a) {
        this.button(g, font, 9, yy, 162, 19, label, a, false);
        g.m_280056_(font, "\u2039", 15, yy + 6, FurnaceGridEditor.color(a, 103, 211, 231), false);
        g.m_280056_(font, "\u203a", 160, yy + 6, FurnaceGridEditor.color(a, 103, 211, 231), false);
    }

    private void button(GuiGraphics g, Font font, int bx, int by, int w, int h, Component label, float a, boolean accent) {
        double[] mouse = Picking.mouseGui();
        boolean hover = this.lx(mouse[0]) >= (double)bx && this.lx(mouse[0]) < (double)(bx + w) && this.ly(mouse[1]) >= (double)by && this.ly(mouse[1]) < (double)(by + h);
        g.m_280509_(bx, by, bx + w, by + h, FurnaceGridEditor.color((double)a * (accent ? (hover ? 0.3 : 0.2) : (hover ? 0.14 : 0.07)), 103, 211, 231));
        g.m_280614_(font, label, bx + (w - font.m_92852_((FormattedText)label)) / 2, by + (h - 8) / 2, FurnaceGridEditor.color(a, 224, 238, 238), false);
    }

    private static void glass(GuiGraphics g, int w, int h, float a) {
        g.m_280509_(0, 0, w, h, FurnaceGridEditor.color((double)a * 0.85, 12, 16, 19));
        g.m_280509_(0, 0, w, 1, FurnaceGridEditor.color((double)a * 0.55, 103, 211, 231));
        g.m_280509_(0, h - 1, w, h, FurnaceGridEditor.color((double)a * 0.25, 103, 211, 231));
        g.m_280509_(0, 1, 1, h - 1, FurnaceGridEditor.color((double)a * 0.18, 103, 211, 231));
        g.m_280509_(w - 1, 1, w, h - 1, FurnaceGridEditor.color((double)a * 0.18, 103, 211, 231));
    }

    private static int color(double a, int r, int g, int b) {
        return Mth.m_14045_((int)((int)Math.round(a * 255.0)), (int)0, (int)255) << 24 | r << 16 | g << 8 | b;
    }

    void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        if (!this.visible()) {
            return;
        }
        FurnaceAppearance layout = this.station.geometry();
        int[] cyan = new int[]{103, 211, 231};
        for (int i = 0; i < 5; ++i) {
            double[] r = layout.rect(this.station.layout, i, false);
            FurnaceZones.Face face = layout.face(this.station.layout, i);
            double out = layout.region(i).out() / 16.0 + 0.006;
            double t = Math.min(0.003, (r[3] - r[2]) / 3.0);
            int a = (int)(this.shown * (double)(i == this.region ? 190 : 65));
            for (double[] edge : new double[][]{{r[0], r[1], r[2], r[2] + t}, {r[0], r[1], r[3] - t, r[3]}, {r[0], r[0] + t, r[2], r[3]}, {r[1] - t, r[1], r[2], r[3]}}) {
                FurnaceStation.quad(pose, buffers.m_6299_(Fx.HIGHLIGHT), camera, this.station.pos, this.station.sideDirection(), face, edge, out, cyan, cyan, a, false);
            }
            if (i != this.region || !this.advanced || i >= 3) continue;
            int[] gray = new int[]{180, 195, 199};
            FurnaceStation.quad(pose, buffers.m_6299_(Fx.HIGHLIGHT), camera, this.station.pos, this.station.sideDirection(), face, layout.rect(this.station.layout, i, true), out + 0.001, gray, gray, (int)(this.shown * 18.0), false);
        }
        for (FurnaceLayout.Bed bed : this.station.layout.beds) {
            if (!ClientFurnaces.item(this.station.pos, bed.slot()).m_41619_()) continue;
            ItemStack demo = new ItemStack((ItemLike)(bed.slot() == 1 ? Items.f_42413_ : (this.station.kind() == StationKind.SMOKER ? (bed.slot() == 0 ? Items.f_42579_ : Items.f_42580_) : (bed.slot() == 0 ? Items.f_151050_ : Items.f_42416_))));
            Vec3 base = layout.point(this.station.layout, this.station.pos, this.station.sideDirection(), bed.slot(), (bed.u0() + bed.u1()) / 2.0, bed.floor(), -bed.depth(), true);
            ItemPlacer.render(pose, buffers, camera, demo, base, this.station.sideDirection(), 91 + bed.slot(), 0.95f * (float)layout.region(bed.slot()).itemScale(), bed.tilt(), light, (float)this.shown * 0.45f);
        }
    }

    double[] togglePoint() {
        this.layout();
        return new double[]{this.toggleX + (double)(15.0f * this.scale), this.toggleY + (double)(14.0f * this.scale)};
    }

    double[] controlPoint(double px, double py) {
        this.layout();
        return new double[]{this.x + px * (double)this.scale, this.y + py * (double)this.scale};
    }

    double[] sliderPoint(int row, double value) {
        int field = this.fields()[row];
        return this.controlPoint(11.0 + 158.0 * (value - FurnaceGridEditor.min(field)) / (FurnaceGridEditor.max(field) - FurnaceGridEditor.min(field)), 112 + row * 24 + 16);
    }
}

