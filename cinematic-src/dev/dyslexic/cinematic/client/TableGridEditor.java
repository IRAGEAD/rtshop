/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.EditorIcon;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.LayoutEditor;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.TableLayout;
import dev.dyslexic.cinematic.client.TableLayouts;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.table.TableGrid;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class TableGridEditor
implements LayoutEditor {
    private static final int W = 164;
    private static final int H = 290;
    private static final int ROW_Y = 64;
    private static final int ROW_H = 25;
    private static final String[] FIELDS = new String[]{"offset_x", "offset_z", "rotation", "width", "depth", "height", "item_size"};
    private static final double[] MIN = new double[]{-6.0, -6.0, -180.0, 50.0, 50.0, -2.0, 50.0};
    private static final double[] MAX = new double[]{6.0, 6.0, 180.0, 170.0, 170.0, 2.0, 150.0};
    private final TableStation station;
    private TableLayout[] drafts;
    private int profile;
    private boolean open;
    private boolean saveError;
    private double shown;
    private int slider = -1;
    private Vec3 dragFrom;
    private TableLayout dragLayout;
    private int tooltipField = -1;
    private double tooltipAt;
    private float scale;
    private double x;
    private double y;
    private double toggleX;
    private double toggleY;

    TableGridEditor(TableStation station) {
        this.station = station;
        this.layout();
    }

    static Component text(String key, Object ... args) {
        return Component.m_237110_((String)("cinematic.grid_editor." + key), (Object[])args);
    }

    @Override
    public boolean editing() {
        return this.open;
    }

    @Override
    public boolean visible() {
        return this.open || this.shown > 0.01;
    }

    private void layout() {
        Window window = Minecraft.m_91087_().m_91268_();
        int width = window.m_85445_();
        int height = window.m_85446_();
        this.scale = Math.min(1.0f, Math.min((float)height / 380.0f, (float)width / 650.0f));
        this.x = (double)((float)(width - 8) - 164.0f * this.scale) + (1.0 - this.shown) * 176.0 * (double)this.scale;
        this.y = (double)height * 0.14;
        this.toggleX = (float)(width - 8) - 30.0f * this.scale;
        this.toggleY = (double)height * 0.45;
    }

    void frame() {
        int field;
        if (this.open && (!Settings.developerMode() || Scene.phase != Scene.Phase.ACTIVE)) {
            this.cancel();
        }
        this.shown = Anim.approach(this.shown, this.open ? 1.0 : 0.0, 16.0, Scene.frameDt);
        this.layout();
        double[] mouse = Picking.mouseGui();
        int n = field = this.open && this.inPanel(mouse[0], mouse[1]) && this.ly(mouse[1]) >= 64.0 && this.ly(mouse[1]) < 239.0 && (this.ly(mouse[1]) - 64.0) % 25.0 < 11.0 && this.slider < 0 ? (int)((this.ly(mouse[1]) - 64.0) / 25.0) : -1;
        if (field != this.tooltipField) {
            this.tooltipField = field;
            this.tooltipAt = Scene.clock();
        }
    }

    private double lx(double mx) {
        return (mx - this.x) / (double)this.scale;
    }

    private double ly(double my) {
        return (my - this.y) / (double)this.scale;
    }

    private boolean inPanel(double mx, double my) {
        return this.visible() && this.lx(mx) >= 0.0 && this.lx(mx) < 164.0 && this.ly(my) >= 0.0 && this.ly(my) < 290.0;
    }

    private boolean toggle(double mx, double my) {
        return Settings.developerMode() && !this.visible() && mx >= this.toggleX && mx < this.toggleX + (double)(30.0f * this.scale) && my >= this.toggleY && my < this.toggleY + (double)(28.0f * this.scale);
    }

    @Override
    public boolean over(double mx, double my) {
        return this.open || this.inPanel(mx, my) || this.toggle(mx, my);
    }

    void open() {
        if (!Settings.developerMode() || this.station.busy() || Scene.phase != Scene.Phase.ACTIVE) {
            return;
        }
        this.station.release(1);
        this.station.recipeBook.fold();
        this.drafts = TableLayouts.profiles();
        this.profile = TableLayouts.selected();
        this.open = true;
        this.saveError = false;
        TableLayouts.preview(this.drafts[this.profile]);
        this.station.clearCursor();
    }

    void cancel() {
        this.open = false;
        this.slider = -1;
        this.dragFrom = null;
        TableLayouts.cancelPreview();
    }

    private void save() {
        if (TableLayouts.save(this.drafts, this.profile)) {
            this.cancel();
        } else {
            this.saveError = true;
        }
    }

    private void change(int field, double value) {
        this.drafts[this.profile] = this.drafts[this.profile].with(field, value);
        TableLayouts.preview(this.drafts[this.profile]);
        this.saveError = false;
    }

    private void slide(double mx) {
        double t = Mth.m_14008_((double)((this.lx(mx) - 11.0) / 142.0), (double)0.0, (double)1.0);
        double step = this.slider == 0 || this.slider == 1 || this.slider == 5 ? 0.125 : 1.0;
        this.change(this.slider, (double)Math.round(Mth.m_14139_((double)t, (double)MIN[this.slider], (double)MAX[this.slider]) / step) * step);
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
                this.profile = Math.floorMod(this.profile + (px < 82.0 ? -1 : 1), 3);
                TableLayouts.preview(this.drafts[this.profile]);
            } else if (py >= 64.0 && py < 239.0) {
                this.slider = (int)((py - 64.0) / 25.0);
                this.slide(mx);
            } else if (py >= 241.0 && py <= 257.0) {
                this.drafts[this.profile] = TableLayout.VANILLA;
                TableLayouts.preview(this.drafts[this.profile]);
            } else if (py >= 264.0 && py <= 282.0) {
                if (px < 82.0) {
                    this.save();
                } else {
                    this.cancel();
                }
            }
        } else {
            Vec3 hit = Picking.cursorOnPlane(TableLayouts.current().surfaceY(this.station.pos));
            if (hit != null && TableLayouts.current().cellAt(this.station.pos, hit) >= 0) {
                this.dragFrom = hit;
                this.dragLayout = this.drafts[this.profile];
            }
        }
        return true;
    }

    @Override
    public boolean drag(double mx, double my, int button) {
        Vec3 hit;
        if (!this.open) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        if (this.slider >= 0) {
            this.slide(mx);
        } else if (this.dragFrom != null && (hit = Picking.cursorOnPlane(this.dragLayout.surfaceY(this.station.pos))) != null) {
            Vec3 delta = hit.m_82546_(this.dragFrom).m_82490_(16.0);
            this.drafts[this.profile] = new TableLayout(this.dragLayout.x() + delta.f_82479_, this.dragLayout.z() + delta.f_82481_, this.dragLayout.rotation(), this.dragLayout.width(), this.dragLayout.depth(), this.dragLayout.height(), this.dragLayout.itemScale());
            TableLayouts.preview(this.drafts[this.profile]);
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
        if (this.inPanel(mx, my)) {
            int field = (int)((this.ly(my) - 64.0) / 25.0);
            if (this.ly(my) >= 64.0 && field >= 0 && field < 7) {
                this.change(field, this.drafts[this.profile].value(field) + Math.signum(amount) * (field < 2 || field == 5 ? 0.125 : 1.0));
            }
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
            return List.of(TableGridEditor.text("open", new Object[0]), TableGridEditor.text("shortcut", new Object[0]));
        }
        if (this.open && this.tooltipField >= 0 && Scene.clock() - this.tooltipAt > 0.55) {
            return List.of(TableGridEditor.text(FIELDS[this.tooltipField] + ".tooltip", new Object[0]));
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
            TableGridEditor.glass(g, 30, 28, alpha);
            if (this.toggle(mouse[0], mouse[1])) {
                g.m_280509_(1, 1, 29, 27, TableGridEditor.color((double)alpha * 0.12, 255, 255, 255));
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
        TableGridEditor.glass(g, 164, 290, a);
        g.m_280614_(font, TableGridEditor.text("title", new Object[0]), 10, 10, TableGridEditor.color(a, 242, 234, 216), false);
        this.button(g, font, 9, 29, 146, 19, TableGridEditor.text("profile", this.profile + 1), a, false);
        g.m_280056_(font, "\u2039", 15, 35, TableGridEditor.color(a, 103, 211, 231), false);
        g.m_280056_(font, "\u203a", 144, 35, TableGridEditor.color(a, 103, 211, 231), false);
        g.m_280614_(font, TableGridEditor.text(this.saveError ? "save_error" : "drag_hint", new Object[0]), 10, 53, TableGridEditor.color((double)a * 0.75, this.saveError ? 255 : 181, this.saveError ? 122 : 195, this.saveError ? 112 : 199), false);
        for (int i = 0; i < 7; ++i) {
            int yy = 64 + i * 25;
            double value = this.drafts[this.profile].value(i);
            String number = i == 2 ? String.format(Locale.ROOT, "%.0f\u00b0", value) : (i == 3 || i == 4 || i == 6 ? String.format(Locale.ROOT, "%.0f%%", value) : String.format(Locale.ROOT, "%+.2f px", value));
            g.m_280614_(font, TableGridEditor.text(FIELDS[i], new Object[0]), 10, yy, TableGridEditor.color((double)a * 0.9, 224, 227, 226), false);
            g.m_280056_(font, number, 154 - font.m_92895_(number), yy, TableGridEditor.color(a, 103, 211, 231), false);
            int end = 11 + (int)Math.round(142.0 * (value - MIN[i]) / (MAX[i] - MIN[i]));
            g.m_280509_(11, yy + 15, 153, yy + 17, TableGridEditor.color((double)a * 0.14, 255, 255, 255));
            g.m_280509_(11, yy + 15, end, yy + 17, TableGridEditor.color((double)a * 0.75, 103, 211, 231));
            g.m_280509_(end - 2, yy + 12, end + 2, yy + 20, TableGridEditor.color(a, 196, 240, 245));
        }
        this.button(g, font, 9, 241, 146, 16, TableGridEditor.text("reset", new Object[0]), a, false);
        this.button(g, font, 9, 264, 70, 18, TableGridEditor.text("save", new Object[0]), a, true);
        this.button(g, font, 85, 264, 70, 18, TableGridEditor.text("cancel", new Object[0]), a, false);
        g.m_280168_().m_85849_();
        if (this.open) {
            for (int logical = 0; logical < 9; ++logical) {
                int world = TableGrid.worldIndex(this.station.sideDirection(), logical / 3, logical % 3);
                double[] at = Picking.toGui(TableLayouts.current().cell(this.station.pos, world).m_82520_(0.0, 0.025, 0.0));
                if (at == null || this.inPanel(at[0], at[1])) continue;
                int px = (int)at[0];
                int py = (int)at[1];
                g.m_280509_(px - 4, py - 4, px + 4, py + 5, TableGridEditor.color((double)a * 0.6, 12, 16, 19));
                g.m_280137_(font, Integer.toString(logical + 1), px, py - 3, TableGridEditor.color(a, 196, 240, 245));
            }
        }
    }

    private void button(GuiGraphics g, Font font, int bx, int by, int w, int h, Component label, float a, boolean accent) {
        double[] mouse = Picking.mouseGui();
        boolean hover = this.lx(mouse[0]) >= (double)bx && this.lx(mouse[0]) < (double)(bx + w) && this.ly(mouse[1]) >= (double)by && this.ly(mouse[1]) < (double)(by + h);
        g.m_280509_(bx, by, bx + w, by + h, TableGridEditor.color((double)a * (accent ? (hover ? 0.3 : 0.2) : (hover ? 0.14 : 0.07)), 103, 211, 231));
        g.m_280614_(font, label, bx + (w - font.m_92852_((FormattedText)label)) / 2, by + (h - 8) / 2, TableGridEditor.color(a, 224, 238, 238), false);
    }

    private static void glass(GuiGraphics g, int w, int h, float a) {
        g.m_280509_(0, 0, w, h, TableGridEditor.color((double)a * 0.85, 12, 16, 19));
        g.m_280509_(0, 0, w, 1, TableGridEditor.color((double)a * 0.55, 103, 211, 231));
        g.m_280509_(0, h - 1, w, h, TableGridEditor.color((double)a * 0.25, 103, 211, 231));
        g.m_280509_(0, 1, 1, h - 1, TableGridEditor.color((double)a * 0.18, 103, 211, 231));
        g.m_280509_(w - 1, 1, w, h - 1, TableGridEditor.color((double)a * 0.18, 103, 211, 231));
    }

    private static int color(double a, int r, int g, int b) {
        return Mth.m_14045_((int)((int)Math.round(a * 255.0)), (int)0, (int)255) << 24 | r << 16 | g << 8 | b;
    }

    void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera) {
        if (!this.visible()) {
            return;
        }
        TableLayout layout = TableLayouts.current();
        for (int i = 0; i < 9; ++i) {
            pose.m_85836_();
            layout.cellPose(pose, this.station.pos, i, camera);
            double half = 0.08775;
            double t = 0.003;
            VertexConsumer out = buffers.m_6299_(Fx.HIGHLIGHT);
            Matrix4f m = pose.m_85850_().m_252922_();
            TableGridEditor.quad(out, m, -half, -half, half, half, (int)(this.shown * 10.0));
            TableGridEditor.quad(out, m, -half, -half, half, -half + t, (int)(this.shown * 160.0));
            TableGridEditor.quad(out, m, -half, half - t, half, half, (int)(this.shown * 160.0));
            TableGridEditor.quad(out, m, -half, -half, -half + t, half, (int)(this.shown * 160.0));
            TableGridEditor.quad(out, m, half - t, -half, half, half, (int)(this.shown * 160.0));
            pose.m_85849_();
        }
    }

    private static void quad(VertexConsumer out, Matrix4f m, double x0, double z0, double x1, double z1, int a) {
        out.m_252986_(m, (float)x0, 0.0f, (float)z0).m_6122_(103, 211, 231, a).m_5752_();
        out.m_252986_(m, (float)x0, 0.0f, (float)z1).m_6122_(103, 211, 231, a).m_5752_();
        out.m_252986_(m, (float)x1, 0.0f, (float)z1).m_6122_(103, 211, 231, a).m_5752_();
        out.m_252986_(m, (float)x1, 0.0f, (float)z0).m_6122_(103, 211, 231, a).m_5752_();
    }

    double[] togglePoint() {
        this.layout();
        return new double[]{this.toggleX + (double)(15.0f * this.scale), this.toggleY + (double)(14.0f * this.scale)};
    }

    double[] controlPoint(double px, double py) {
        this.layout();
        return new double[]{this.x + px * (double)this.scale, this.y + py * (double)this.scale};
    }

    double[] sliderPoint(int field, double value) {
        return this.controlPoint(11.0 + 142.0 * (value - MIN[field]) / (MAX[field] - MIN[field]), 64 + field * 25 + 16);
    }
}

