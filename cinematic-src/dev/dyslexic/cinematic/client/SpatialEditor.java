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
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.EditorIcon;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.LayoutEditor;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.client.SurfaceAppearance;
import dev.dyslexic.cinematic.client.TableGridEditor;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SpatialEditor
implements LayoutEditor {
    private static final int W = 180;
    private static final int H = 362;
    private static final int ROW_Y = 108;
    private static final int ROW_H = 22;
    private static final String[] FIELDS = new String[]{"offset_x", "offset_z", "rotation", "width", "depth", "height", "item_size"};
    private static final int[] FIELDS_INDEX = new int[]{0, 1, 2, 3, 4, 5, 6};
    private final SpatialTarget target;
    private final ClientStation station;
    private SpatialAppearance[] drafts;
    private int profile;
    private int region = -1;
    private int slider = -1;
    private boolean open;
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
    private Vec3 dragNormal;
    private SpatialAppearance dragLayout;

    SpatialEditor(SpatialTarget target) {
        this.target = target;
        this.station = target.layoutStation();
        this.layout();
    }

    static Component text(String key, Object ... args) {
        return Component.m_237110_((String)("cinematic.spatial_editor." + key), (Object[])args);
    }

    @Override
    public boolean editing() {
        return this.open;
    }

    @Override
    public boolean visible() {
        return this.open || this.shown > 0.01;
    }

    float presence() {
        return (float)this.shown;
    }

    private int[] fields() {
        return FIELDS_INDEX;
    }

    private static boolean percent(int field) {
        return field == 3 || field == 4 || field == 6;
    }

    private static double min(int field) {
        return SpatialEditor.percent(field) ? 50.0 : (field == 2 ? -180.0 : (field == 5 ? -2.0 : -6.0));
    }

    private static double max(int field) {
        return SpatialEditor.percent(field) ? 150.0 : -SpatialEditor.min(field);
    }

    private static double step(int field) {
        return SpatialEditor.percent(field) || field == 2 ? 1.0 : 0.125;
    }

    private String fieldKey(int field) {
        if (this.station.kind() == StationKind.LECTERN && field == 1) {
            return "along_book";
        }
        if (Math.abs(this.target.layoutFrame().normal().f_82480_) > 0.5 && this.station.kind() != StationKind.BREWING) {
            return switch (field) {
                case 1 -> "towards";
                default -> FIELDS[field];
            };
        }
        if (this.station.kind() != StationKind.BREWING) {
            return switch (field) {
                case 1 -> "offset_y";
                case 4 -> "area_height";
                case 5 -> "outward";
                default -> FIELDS[field];
            };
        }
        return FIELDS[field];
    }

    private Component regionName() {
        if (this.region < 0) {
            return SpatialEditor.text("all", new Object[0]);
        }
        return switch (this.station.kind()) {
            case StationKind.CHISELED_BOOKSHELF -> SpatialEditor.text("book", this.region + 1);
            case StationKind.LOOM -> SpatialEditor.text(this.region == 0 ? "cloth" : (this.region == 1 ? "dye" : "pattern"), new Object[0]);
            case StationKind.ENCHANTING -> {
                if (this.region < 2) {
                    yield SpatialEditor.text(this.region == 0 ? "enchant_item" : "lapis", new Object[0]);
                }
                yield SpatialEditor.text("offer", this.region - 1);
            }
            case StationKind.ANVIL -> SpatialEditor.text(this.region == 0 ? "workpiece" : (this.region == 1 ? "addition" : "nameplate"), new Object[0]);
            case StationKind.GRINDSTONE -> SpatialEditor.text(this.region < 2 ? "input" : "grinding", this.region + 1);
            case StationKind.STONECUTTER -> SpatialEditor.text(this.region == 0 ? "stone" : (this.region == 1 ? "cutting" : "shapes"), new Object[0]);
            case StationKind.CAMPFIRE -> SpatialEditor.text("food", this.region + 1);
            case StationKind.LECTERN -> SpatialEditor.text("reading_book", new Object[0]);
            case StationKind.BEACON -> {
                if (this.region == 0) {
                    yield SpatialEditor.text("payment", new Object[0]);
                }
                yield SpatialEditor.text("power", this.region);
            }
            case StationKind.JUKEBOX -> SpatialEditor.text("record_slot", new Object[0]);
            default -> this.region < 3 ? SpatialEditor.text("bottle", this.region + 1) : SpatialEditor.text(this.region == 3 ? "ingredient" : "fuel", new Object[0]);
        };
    }

    private void layout() {
        Window window = Minecraft.m_91087_().m_91268_();
        int width = window.m_85445_();
        int height = window.m_85446_();
        this.scale = Math.min(1.0f, Math.min((float)height / 470.0f, (float)width / 700.0f));
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
        return this.visible() && this.lx(mx) >= 0.0 && this.lx(mx) < 180.0 && this.ly(my) >= 0.0 && this.ly(my) < 362.0;
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
        int row = (int)((this.ly(mouse[1]) - 108.0) / 22.0);
        int n = field = this.open && this.inPanel(mouse[0], mouse[1]) && this.ly(mouse[1]) >= 108.0 && row < this.fields().length && (this.ly(mouse[1]) - 108.0) % 22.0 < 11.0 && this.slider < 0 ? this.fields()[row] : -1;
        if (field != this.tooltipField) {
            this.tooltipField = field;
            this.tooltipAt = Scene.clock();
        }
    }

    void open() {
        if (!Settings.developerMode() || this.station.busy() || Scene.phase != Scene.Phase.ACTIVE) {
            return;
        }
        this.drafts = SpatialLayouts.profiles(this.station.kind());
        this.profile = SpatialLayouts.selected(this.station.kind());
        this.open = true;
        this.saveError = false;
        SpatialLayouts.preview(this.station.kind(), this.drafts[this.profile]);
        this.station.clearCursor();
    }

    void cancel() {
        this.open = false;
        this.release();
        SpatialLayouts.cancelPreview(this.station.kind());
    }

    private void save() {
        if (SpatialLayouts.save(this.station.kind(), this.drafts, this.profile)) {
            this.cancel();
        } else {
            this.saveError = true;
        }
    }

    private void change(int field, double value) {
        this.drafts[this.profile] = this.drafts[this.profile].with(this.region, this.drafts[this.profile].region(this.region).with(field, value));
        SpatialLayouts.preview(this.station.kind(), this.drafts[this.profile]);
        this.saveError = false;
    }

    private void slide(double mx) {
        double t = Mth.m_14008_((double)((this.lx(mx) - 11.0) / 158.0), (double)0.0, (double)1.0);
        this.change(this.slider, (double)Math.round(Mth.m_14139_((double)t, (double)SpatialEditor.min(this.slider), (double)SpatialEditor.max(this.slider)) / SpatialEditor.step(this.slider)) * SpatialEditor.step(this.slider));
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
                SpatialLayouts.preview(this.station.kind(), this.drafts[this.profile]);
            } else if (py >= 55.0 && py <= 75.0) {
                this.region = Math.floorMod(this.region + 1 + (px < 90.0 ? -1 : 1), SpatialAppearance.count(this.station.kind()) + 1) - 1;
                this.release();
            } else if (py >= 108.0 && py < (double)(108 + this.fields().length * 22)) {
                this.slider = this.fields()[(int)((py - 108.0) / 22.0)];
                this.slide(mx);
            } else if (py >= 306.0 && py <= 322.0) {
                this.drafts[this.profile] = SpatialAppearance.VANILLA;
                SpatialLayouts.preview(this.station.kind(), this.drafts[this.profile]);
                this.saveError = false;
            } else if (py >= 334.0 && py <= 352.0) {
                if (px < 90.0) {
                    this.save();
                } else {
                    this.cancel();
                }
            }
        } else {
            int hit = this.target.layoutPick();
            if (hit >= 0) {
                if (this.region >= 0) {
                    this.region = hit;
                }
                Vec3 at = this.target.layoutPoint(hit);
                this.dragNormal = this.station.kind() == StationKind.BREWING || this.station.kind() == StationKind.ENCHANTING || this.station.kind() == StationKind.BEACON ? Picking.camera().m_82546_(at).m_82541_() : this.target.layoutNormal(hit);
                this.dragFrom = Picking.cursorOnPlane(at, this.dragNormal);
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
        } else if (this.dragFrom != null && (hit = Picking.cursorOnPlane(this.dragFrom, this.dragNormal)) != null) {
            Vec3 delta = hit.m_82546_(this.dragFrom);
            SurfaceAppearance.Region base = this.dragLayout.region(this.region);
            Vec3 across = this.region < 0 ? this.target.layoutFrame().across() : this.dragLayout.axis(this.target.layoutFrame(), -1, true);
            Vec3 up = this.region < 0 ? this.target.layoutFrame().up() : this.dragLayout.axis(this.target.layoutFrame(), -1, false);
            double u = base.x() + delta.m_82526_(across) * 16.0;
            double v = base.z() + delta.m_82526_(up) * 16.0;
            SurfaceAppearance.Region moved = base.with(0, u).with(1, v);
            if (this.station.kind() == StationKind.BREWING || this.station.kind() == StationKind.ENCHANTING || this.station.kind() == StationKind.BEACON) {
                moved = moved.with(5, base.height() + delta.m_82526_(this.target.layoutFrame().normal()) * 16.0);
            }
            this.drafts[this.profile] = this.dragLayout.with(this.region, moved);
            SpatialLayouts.preview(this.station.kind(), this.drafts[this.profile]);
            this.saveError = false;
        }
        return true;
    }

    @Override
    public void release() {
        this.slider = -1;
        this.dragFrom = null;
        this.dragNormal = null;
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        if (!this.open) {
            return false;
        }
        int row = (int)((this.ly(my) - 108.0) / 22.0);
        if (this.inPanel(mx, my) && this.ly(my) >= 108.0 && row < this.fields().length) {
            int field = this.fields()[row];
            this.change(field, this.drafts[this.profile].region(this.region).value(field) + Math.signum(amount) * SpatialEditor.step(field));
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
            return List.of(SpatialEditor.text("open", new Object[0]), TableGridEditor.text("shortcut", new Object[0]));
        }
        if (this.open && this.tooltipField >= 0 && Scene.clock() - this.tooltipAt > 0.55) {
            return List.of(SpatialEditor.text(this.fieldKey(this.tooltipField) + ".tooltip", new Object[0]));
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
            SpatialEditor.glass(g, 30, 28, alpha);
            if (this.toggle(mouse[0], mouse[1])) {
                g.m_280509_(1, 1, 29, 27, SpatialEditor.color((double)alpha * 0.12, 255, 255, 255));
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
        SpatialEditor.glass(g, 180, 362, a);
        g.m_280614_(font, SpatialEditor.text("title." + this.station.kind().name().toLowerCase(Locale.ROOT), new Object[0]), 10, 10, SpatialEditor.color(a, 242, 234, 216), false);
        this.selector(g, font, 29, TableGridEditor.text("profile", this.profile + 1), a);
        this.selector(g, font, 55, this.regionName(), a);
        g.m_280614_(font, SpatialEditor.text("placement", new Object[0]), 10, 87, SpatialEditor.color((double)a * 0.65, 181, 195, 199), false);
        int[] fields = this.fields();
        for (int row = 0; row < fields.length; ++row) {
            int field = fields[row];
            int yy = 108 + row * 22;
            double value = this.drafts[this.profile].region(this.region).value(field);
            String number = field == 2 ? String.format(Locale.ROOT, "%+.0f\u00b0", value) : (SpatialEditor.percent(field) ? String.format(Locale.ROOT, "%.0f%%", value) : String.format(Locale.ROOT, "%+.2f px", value));
            g.m_280614_(font, SpatialEditor.text(this.fieldKey(field), new Object[0]), 10, yy, SpatialEditor.color((double)a * 0.9, 224, 227, 226), false);
            g.m_280056_(font, number, 170 - font.m_92895_(number), yy, SpatialEditor.color(a, 103, 211, 231), false);
            int end = 11 + (int)Math.round(158.0 * (value - SpatialEditor.min(field)) / (SpatialEditor.max(field) - SpatialEditor.min(field)));
            g.m_280509_(11, yy + 15, 169, yy + 17, SpatialEditor.color((double)a * 0.14, 255, 255, 255));
            g.m_280509_(11, yy + 15, end, yy + 17, SpatialEditor.color((double)a * 0.75, 103, 211, 231));
            g.m_280509_(end - 2, yy + 12, end + 2, yy + 20, SpatialEditor.color(a, 196, 240, 245));
        }
        g.m_280614_(font, SpatialEditor.text(this.saveError ? "save_error" : "drag_hint", new Object[0]), 10, 286, SpatialEditor.color((double)a * 0.75, this.saveError ? 255 : 181, this.saveError ? 122 : 195, this.saveError ? 112 : 199), false);
        this.button(g, font, 9, 306, 162, 16, SpatialEditor.text("reset", new Object[0]), a, false);
        this.button(g, font, 9, 334, 78, 18, TableGridEditor.text("save", new Object[0]), a, true);
        this.button(g, font, 93, 334, 78, 18, TableGridEditor.text("cancel", new Object[0]), a, false);
        g.m_280168_().m_85849_();
        if (this.open) {
            for (int i = 0; i < SpatialAppearance.count(this.station.kind()); ++i) {
                double[] at = Picking.toGui(this.target.layoutPoint(i));
                if (at == null || this.inPanel(at[0], at[1])) continue;
                int px = (int)at[0];
                int py = (int)at[1];
                g.m_280509_(px - 4, py - 4, px + 4, py + 5, SpatialEditor.color((double)a * 0.6, 12, 16, 19));
                g.m_280137_(font, Integer.toString(i + 1), px, py - 3, SpatialEditor.color((double)a * (this.region < 0 || i == this.region ? 1.0 : 0.45), 196, 240, 245));
            }
        }
    }

    private void selector(GuiGraphics g, Font font, int yy, Component label, float a) {
        this.button(g, font, 9, yy, 162, 19, label, a, false);
        g.m_280056_(font, "\u2039", 15, yy + 6, SpatialEditor.color(a, 103, 211, 231), false);
        g.m_280056_(font, "\u203a", 160, yy + 6, SpatialEditor.color(a, 103, 211, 231), false);
    }

    private void button(GuiGraphics g, Font font, int bx, int by, int w, int h, Component label, float a, boolean accent) {
        double[] mouse = Picking.mouseGui();
        boolean hover = this.lx(mouse[0]) >= (double)bx && this.lx(mouse[0]) < (double)(bx + w) && this.ly(mouse[1]) >= (double)by && this.ly(mouse[1]) < (double)(by + h);
        g.m_280509_(bx, by, bx + w, by + h, SpatialEditor.color((double)a * (accent ? (hover ? 0.3 : 0.2) : (hover ? 0.14 : 0.07)), 103, 211, 231));
        g.m_280614_(font, label, bx + (w - font.m_92852_((FormattedText)label)) / 2, by + (h - 8) / 2, SpatialEditor.color(a, 224, 238, 238), false);
    }

    private static void glass(GuiGraphics g, int w, int h, float a) {
        g.m_280509_(0, 0, w, h, SpatialEditor.color((double)a * 0.85, 12, 16, 19));
        g.m_280509_(0, 0, w, 1, SpatialEditor.color((double)a * 0.55, 103, 211, 231));
        g.m_280509_(0, h - 1, w, h, SpatialEditor.color((double)a * 0.25, 103, 211, 231));
        g.m_280509_(0, 1, 1, h - 1, SpatialEditor.color((double)a * 0.18, 103, 211, 231));
        g.m_280509_(w - 1, 1, w, h - 1, SpatialEditor.color((double)a * 0.18, 103, 211, 231));
    }

    private static int color(double a, int r, int g, int b) {
        return Mth.m_14045_((int)((int)Math.round(a * 255.0)), (int)0, (int)255) << 24 | r << 16 | g << 8 | b;
    }

    void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        if (!this.visible()) {
            return;
        }
        SpatialAppearance.Frame frame = this.target.layoutFrame();
        for (int i = 0; i < SpatialAppearance.count(this.station.kind()); ++i) {
            double[] half = this.target.geometry().half(frame, i);
            Vec3 at = this.target.layoutPoint(i).m_82549_(this.target.layoutNormal(i).m_82490_(0.006));
            Vec3 across = this.target.layoutAxis(i, true);
            Vec3 up = this.target.layoutAxis(i, false);
            double edge = 0.003;
            int alpha = (int)(this.shown * (double)(this.region < 0 || i == this.region ? 190 : 65));
            SpatialEditor.patch(pose, buffers, camera, at, across, up, -half[0], half[0], -half[1], -half[1] + edge, alpha);
            SpatialEditor.patch(pose, buffers, camera, at, across, up, -half[0], half[0], half[1] - edge, half[1], alpha);
            SpatialEditor.patch(pose, buffers, camera, at, across, up, -half[0], -half[0] + edge, -half[1], half[1], alpha);
            SpatialEditor.patch(pose, buffers, camera, at, across, up, half[0] - edge, half[0], -half[1], half[1], alpha);
            if (!this.target.layoutItem(i).m_41619_()) continue;
            MultiBufferSource ghost = this.station.kind() == StationKind.ENCHANTING && i >= 2 ? buffers : ItemPlacer.opacity(buffers, (float)this.shown * 0.4f);
            this.target.layoutGhost(pose, ghost, camera, i, light);
        }
    }

    private static void patch(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 at, Vec3 across, Vec3 out, double x0, double x1, double z0, double z1, int alpha) {
        VertexConsumer vertex = buffers.m_6299_(Fx.HIGHLIGHT);
        Matrix4f matrix = pose.m_85850_().m_252922_();
        for (double[] p : new double[][]{{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}}) {
            Vec3 v = at.m_82549_(across.m_82490_(p[0])).m_82549_(out.m_82490_(p[1])).m_82546_(camera);
            vertex.m_252986_(matrix, (float)v.f_82479_, (float)v.f_82480_, (float)v.f_82481_).m_6122_(103, 211, 231, alpha).m_5752_();
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
        return this.controlPoint(11.0 + 158.0 * (value - SpatialEditor.min(field)) / (SpatialEditor.max(field) - SpatialEditor.min(field)), 108 + row * 22 + 16);
    }
}

