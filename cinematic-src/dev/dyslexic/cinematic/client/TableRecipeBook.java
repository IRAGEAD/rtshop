/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.TableLayout;
import dev.dyslexic.cinematic.client.TableLayouts;
import dev.dyslexic.cinematic.client.TableRecipeGuide;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.table.TableGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class TableRecipeBook {
    private static final int WIDTH = 128;
    private static final int HEIGHT = 226;
    private static final int CELL = 27;
    private static final int COLS = 4;
    private static final int ROWS = 4;
    private static final int LAY_Y = 205;
    private static final int LAY_H = 15;
    private static final int GRID_X = 10;
    private static final int GRID_Y = 61;
    private static final int PAGE_SIZE = 16;
    private static final ItemStack BOOK = new ItemStack((ItemLike)Items.f_42517_);
    private final TableStation station;
    final TableRecipeGuide guide;
    private final EditBox search;
    private List<TableRecipeGuide.Entry> filtered = List.of();
    private boolean open;
    private boolean craftableOnly;
    private double shown;
    private double selectedAt;
    private int revision = -1;
    private int page;
    private float scale = 1.0f;
    private int x;
    private int y;
    private int toggleY;

    TableRecipeBook(TableStation station) {
        this.station = station;
        this.guide = new TableRecipeGuide(station);
        this.search = new EditBox(Minecraft.m_91087_().f_91062_, 9, 29, 110, 12, TableRecipeBook.text("search", new Object[0]));
        this.search.m_94199_(96);
        this.search.m_94182_(false);
        this.search.m_94202_(-857384);
        this.search.m_257771_(TableRecipeBook.text("search", new Object[0]));
        this.search.m_94151_(value -> {
            this.page = 0;
            this.filter();
        });
    }

    private static Component text(String key, Object ... args) {
        return Component.m_237110_((String)("cinematic.recipes." + key), (Object[])args);
    }

    void tick() {
        if (!this.open && this.guide.selected == null) {
            return;
        }
        this.guide.tick();
        if (this.revision != this.guide.revision) {
            this.revision = this.guide.revision;
            this.filter();
        }
    }

    private void filter() {
        String query = this.search.m_94155_().strip().toLowerCase(Locale.ROOT);
        this.filtered = this.guide.entries.stream().filter(e -> (!this.craftableOnly || e.craftable) && e.search.contains(query)).toList();
        this.page = Mth.m_14045_((int)this.page, (int)0, (int)(this.pages() - 1));
    }

    private int pages() {
        return Math.max(1, (this.filtered.size() + 16 - 1) / 16);
    }

    private double localX(double mx) {
        return (mx - (double)this.x) / (double)this.scale;
    }

    private double localY(double my) {
        return (my - (double)this.y) / (double)this.scale;
    }

    private boolean inPanel(double mx, double my) {
        return this.shown > 0.02 && this.localX(mx) >= 0.0 && this.localX(mx) < 128.0 && this.localY(my) >= 0.0 && this.localY(my) < 226.0;
    }

    private boolean inToggle(double mx, double my) {
        return !this.open && this.shown < 0.02 && mx >= 8.0 && mx < (double)(8.0f + 32.0f * this.scale) && my >= (double)this.toggleY && my < (double)((float)this.toggleY + 28.0f * this.scale);
    }

    boolean over(double mx, double my) {
        return this.inPanel(mx, my) || this.inToggle(mx, my);
    }

    void fold() {
        this.open = false;
        this.search.m_93692_(false);
    }

    void show(TableRecipeGuide.Entry entry) {
        this.guide.select(entry);
        this.selectedAt = Scene.clock();
    }

    private int entryAt(double mx, double my) {
        double lx = this.localX(mx) - 10.0;
        double ly = this.localY(my) - 61.0;
        if (lx < 0.0 || lx >= 108.0 || ly < 0.0 || ly >= 108.0) {
            return -1;
        }
        int index = this.page * 16 + (int)(ly / 27.0) * 4 + (int)(lx / 27.0);
        return index < this.filtered.size() ? index : -1;
    }

    void render(GuiGraphics g, Font font, float alpha) {
        int index;
        Window window = Minecraft.m_91087_().m_91268_();
        int width = window.m_85445_();
        int height = window.m_85446_();
        this.scale = Math.min(1.0f, Math.min((float)height / 300.0f, (float)width / 530.0f));
        this.shown = Anim.approach(this.shown, this.open ? 1.0 : 0.0, 15.0, Scene.frameDt);
        this.x = (int)Math.round(8.0 - (1.0 - this.shown) * 140.0 * (double)this.scale);
        this.y = Math.round((float)height * 0.15f);
        this.toggleY = Math.round((float)height * 0.45f);
        double[] mouse = Picking.mouseGui();
        if (this.shown < 0.02) {
            g.m_280168_().m_85836_();
            g.m_280168_().m_252880_(8.0f, (float)this.toggleY, 0.0f);
            g.m_280168_().m_85841_(this.scale, this.scale, 1.0f);
            TableRecipeBook.glass(g, 32, 28, alpha);
            if (this.inToggle(mouse[0], mouse[1])) {
                g.m_280509_(1, 1, 31, 27, TableRecipeBook.color(alpha * 0.12f, 255, 226, 150));
            }
            g.m_280480_(BOOK, 8, 6);
            g.m_280168_().m_85849_();
            return;
        }
        float a = alpha * (float)this.shown;
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_((float)this.x, (float)this.y, 0.0f);
        g.m_280168_().m_85841_(this.scale, this.scale, 1.0f);
        TableRecipeBook.glass(g, 128, 226, a);
        g.m_280614_(font, TableRecipeBook.text("title", new Object[0]), 9, 9, TableRecipeBook.color(a, 242, 234, 216), false);
        g.m_280056_(font, "\u2212", 111, 8, TableRecipeBook.color(a * 0.8f, 242, 234, 216), false);
        g.m_280509_(8, 25, 120, 43, TableRecipeBook.color(a * 0.08f, 255, 255, 255));
        this.search.m_93650_(a);
        this.search.m_88315_(g, (int)this.localX(mouse[0]), (int)this.localY(mouse[1]), 0.0f);
        g.m_280509_(9, 48, 18, 57, TableRecipeBook.color(a * 0.15f, 255, 255, 255));
        if (this.craftableOnly) {
            g.m_280509_(11, 50, 16, 55, TableRecipeBook.color(a * 0.9f, 255, 226, 150));
        }
        g.m_280614_(font, TableRecipeBook.text("craftable", new Object[0]), 23, 49, TableRecipeBook.color(a * 0.8f, 242, 234, 216), false);
        int hover = this.entryAt(mouse[0], mouse[1]);
        for (int slot = 0; slot < 16 && (index = this.page * 16 + slot) < this.filtered.size(); ++slot) {
            TableRecipeGuide.Entry entry = this.filtered.get(index);
            int sx = 10 + slot % 4 * 27;
            int sy = 61 + slot / 4 * 27;
            g.m_280509_(sx, sy, sx + 27 - 2, sy + 27 - 2, TableRecipeBook.color(a * (index == hover ? 0.19f : 0.06f), 255, 255, 255));
            if (entry == this.guide.selected) {
                TableRecipeBook.rim(g, sx, sy, 25, TableRecipeBook.color(a * 0.85f, 255, 226, 150));
            }
            g.m_280246_(1.0f, 1.0f, 1.0f, a * (entry.craftable ? 1.0f : 0.6f));
            g.m_280480_(entry.result, sx + 4, sy + 3);
            g.m_280370_(font, entry.result, sx + 4, sy + 3);
            g.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
            if (!entry.craftable) continue;
            g.m_280509_(sx + 9, sy + 27 - 5, sx + 15, sy + 27 - 4, TableRecipeBook.color(a * 0.8f, 143, 211, 176));
        }
        if (this.filtered.isEmpty()) {
            g.m_280554_(font, (FormattedText)TableRecipeBook.text("empty", new Object[0]), 15, 96, 98, TableRecipeBook.color(a * 0.7f, 210, 206, 198));
        }
        g.m_280056_(font, "\u2039", 12, 171, TableRecipeBook.color(a * (this.page > 0 ? 0.85f : 0.25f), 242, 234, 216), false);
        String counter = this.page + 1 + " / " + this.pages();
        g.m_280056_(font, counter, (128 - font.m_92895_(counter)) / 2, 171, TableRecipeBook.color(a * 0.65f, 242, 234, 216), false);
        g.m_280056_(font, "\u203a", 110, 171, TableRecipeBook.color(a * (this.page < this.pages() - 1 ? 0.85f : 0.25f), 242, 234, 216), false);
        g.m_280509_(8, 185, 120, 186, TableRecipeBook.color(a * 0.12f, 255, 255, 255));
        if (this.guide.selected != null) {
            g.m_280056_(font, font.m_92834_(this.guide.selected.result.m_41786_().getString(), 94), 9, 190, TableRecipeBook.color(a * 0.85f, 242, 234, 216), false);
            g.m_280056_(font, "\u00d7", 111, 190, TableRecipeBook.color(a * 0.85f, 242, 234, 216), false);
        } else {
            g.m_280614_(font, TableRecipeBook.text("choose", new Object[0]), 9, 190, TableRecipeBook.color(a * 0.55f, 242, 234, 216), false);
        }
        this.drawLayButton(g, font, a, mouse);
        g.m_280168_().m_85849_();
    }

    private int layable() {
        if (this.guide.selected == null || this.station.busy()) {
            return 0;
        }
        return this.guide.takeBackMoves().isEmpty() ? this.guide.layoutMoves().size() : this.guide.layableAfterTakingBack();
    }

    private boolean inLayButton(double mx, double my) {
        double lx = this.localX(mx);
        double ly = this.localY(my);
        return this.open && this.shown > 0.9 && this.guide.selected != null && lx >= 8.0 && lx < 120.0 && ly >= 205.0 && ly < 220.0;
    }

    private TableRecipeGuide.MaxLayout maxLayout() {
        return this.guide.selected == null || this.station.busy() ? new TableRecipeGuide.MaxLayout(0, 0, List.of()) : this.guide.maxLayout();
    }

    void layOut(boolean all) {
        List<Object> moves;
        List back;
        List<Object> list = back = this.guide.selected == null || this.station.busy() ? List.of() : this.guide.takeBackMoves();
        if (!back.isEmpty()) {
            if (all ? this.maxLayout().moves().isEmpty() : this.layable() == 0) {
                return;
            }
            ArrayList<int[]> moves2 = new ArrayList<int[]>(back);
            moves2.add(new int[]{-1, -3, all ? 1 : 0});
            this.station.layOut(moves2);
            return;
        }
        List<Object> list2 = all ? this.maxLayout().moves() : (moves = this.layable() > 0 ? this.guide.layoutMoves() : List.of());
        if (!moves.isEmpty()) {
            this.station.layOut(moves);
        }
    }

    private void drawLayButton(GuiGraphics g, Font font, float a, double[] mouse) {
        boolean hover;
        if (this.guide.selected == null) {
            return;
        }
        boolean all = Screen.m_96638_() && !this.station.busy();
        TableRecipeGuide.MaxLayout max = all ? this.maxLayout() : null;
        int count = all ? max.moves().size() : this.layable();
        boolean live = count > 0;
        boolean bl = hover = live && this.inLayButton(mouse[0], mouse[1]);
        g.m_280509_(8, 205, 120, 220, TableRecipeBook.color(a * (hover ? 0.3f : (live ? 0.16f : 0.06f)), 255, 226, 150));
        g.m_280509_(8, 219, 120, 220, TableRecipeBook.color(a * (live ? 0.6f : 0.15f), 255, 226, 150));
        Component label = all ? (live ? TableRecipeBook.text("lay_out_all", max.sets()) : TableRecipeBook.text("no_more_sets", new Object[0])) : (live ? TableRecipeBook.text("lay_out", count) : TableRecipeBook.text(this.station.busy() ? "laying" : "nothing_to_lay", new Object[0]));
        int w = font.m_92852_((FormattedText)label);
        g.m_280614_(font, label, (128 - w) / 2, 209, TableRecipeBook.color(a * (live ? 0.95f : 0.45f), 242, 234, 216), false);
    }

    private static void glass(GuiGraphics g, int w, int h, float alpha) {
        g.m_280509_(0, 0, w, h, TableRecipeBook.color(alpha * 0.62f, 12, 12, 16));
        g.m_280509_(0, h - 1, w, h, TableRecipeBook.color(alpha * 0.22f, 255, 255, 255));
        g.m_280509_(0, 0, 1, h - 1, TableRecipeBook.color(alpha * 0.08f, 255, 255, 255));
        g.m_280509_(w - 1, 0, w, h - 1, TableRecipeBook.color(alpha * 0.08f, 255, 255, 255));
    }

    private static void rim(GuiGraphics g, int x, int y, int size, int color) {
        g.m_280509_(x, y, x + size, y + 1, color);
        g.m_280509_(x, y + size - 1, x + size, y + size, color);
        g.m_280509_(x, y, x + 1, y + size, color);
        g.m_280509_(x + size - 1, y, x + size, y + size, color);
    }

    private static int color(float alpha, int r, int g, int b) {
        return Mth.m_14045_((int)Math.round(alpha * 255.0f), (int)0, (int)255) << 24 | r << 16 | g << 8 | b;
    }

    boolean click(double mx, double my, int button) {
        if (this.inToggle(mx, my)) {
            this.open = true;
            return true;
        }
        if (!this.inPanel(mx, my)) {
            this.search.m_93692_(false);
            return false;
        }
        if (button != 0 || !this.open) {
            return true;
        }
        double lx = this.localX(mx);
        double ly = this.localY(my);
        this.search.m_93692_(ly >= 25.0 && ly < 43.0 && lx >= 8.0 && lx < 120.0);
        if (this.search.m_93696_()) {
            this.search.m_6375_(lx, ly, button);
            return true;
        }
        if (this.inLayButton(mx, my)) {
            this.layOut(Screen.m_96638_());
        } else if (ly < 23.0 && lx >= 103.0) {
            this.open = false;
        } else if (ly >= 46.0 && ly < 59.0) {
            this.craftableOnly = !this.craftableOnly;
            this.page = 0;
            this.filter();
        } else if (ly >= 169.0 && ly < 184.0) {
            if (lx < 32.0) {
                this.page = Math.max(0, this.page - 1);
            } else if (lx > 96.0) {
                this.page = Math.min(this.pages() - 1, this.page + 1);
            }
        } else if (ly >= 186.0 && ly < 205.0 && lx >= 103.0) {
            this.guide.select(null);
        } else {
            int index = this.entryAt(mx, my);
            if (index >= 0) {
                this.guide.select(this.filtered.get(index));
                this.selectedAt = Scene.clock();
            }
        }
        return true;
    }

    boolean scroll(double mx, double my, double amount) {
        if (!this.over(mx, my)) {
            return false;
        }
        if (this.open) {
            this.page = Mth.m_14045_((int)(this.page - (int)Math.signum(amount)), (int)0, (int)(this.pages() - 1));
        }
        return true;
    }

    boolean charTyped(char c, int modifiers) {
        return this.search.m_93696_() && this.search.m_5534_(c, modifiers);
    }

    boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!this.search.m_93696_()) {
            return false;
        }
        if (key == 256 || key == 257 || key == 335) {
            this.search.m_93692_(false);
        } else {
            this.search.m_7933_(key, scanCode, modifiers);
        }
        return true;
    }

    @Nullable
    List<Component> tooltip(double mx, double my) {
        ItemStack expected;
        if (this.inToggle(mx, my)) {
            return List.of(TableRecipeBook.text("title", new Object[0]));
        }
        if (this.inPanel(mx, my)) {
            double lx = this.localX(mx);
            double ly = this.localY(my);
            if (ly < 23.0 && lx >= 103.0) {
                return List.of(TableRecipeBook.text("hide", new Object[0]));
            }
            if (this.inLayButton(mx, my)) {
                ArrayList<Component> lines = new ArrayList<Component>();
                lines.add(TableRecipeBook.text(this.station.busy() ? "laying" : (this.layable() > 0 ? "lay_out_tip" : "nothing_to_lay_tip"), new Object[0]));
                if (this.guide.missingCount > 0) {
                    lines.add((Component)TableRecipeBook.text("lay_missing", this.guide.missingCount).m_6881_().m_130940_(ChatFormatting.RED));
                }
                if (!this.station.busy()) {
                    TableRecipeGuide.MaxLayout max = this.maxLayout();
                    lines.add((Component)(max.moves().isEmpty() ? TableRecipeBook.text(max.current() > 0 ? "lay_all_full" : "lay_all_none", new Object[0]) : TableRecipeBook.text("lay_all_tip", max.sets())).m_6881_().m_130940_(ChatFormatting.GRAY));
                }
                return lines;
            }
            if (ly >= 186.0 && ly < 205.0 && this.guide.selected != null) {
                return List.of(this.guide.selected.result.m_41786_(), TableRecipeBook.text("clear", new Object[0]));
            }
            int index = this.entryAt(mx, my);
            if (index < 0) {
                return null;
            }
            TableRecipeGuide.Entry entry = this.filtered.get(index);
            ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)Minecraft.m_91087_(), (ItemStack)entry.result));
            lines.add((Component)TableRecipeBook.text(entry.craftable ? "available" : "unavailable", new Object[0]).m_6881_().m_130940_(entry.craftable ? ChatFormatting.GREEN : ChatFormatting.RED));
            lines.add((Component)TableRecipeBook.text("preview", new Object[0]).m_6881_().m_130940_(ChatFormatting.GRAY));
            if (Screen.m_96638_()) {
                lines.add((Component)Component.m_237113_((String)entry.holder.id().toString()).m_130940_(ChatFormatting.DARK_GRAY));
            }
            return lines;
        }
        if (this.guide.selected == null || this.station.hoverCell < 0) {
            return null;
        }
        int cell = this.logicalCell(this.station.hoverCell);
        if (this.guide.grid[cell].m_43947_()) {
            return this.guide.wrong(cell) ? List.of(TableRecipeBook.text("extra", new Object[0]).m_6881_().m_130940_(ChatFormatting.RED)) : null;
        }
        ItemStack itemStack = expected = this.guide.placed[cell] ? ItemStack.f_41583_ : this.guide.ghost(cell);
        if (expected.m_41619_()) {
            return null;
        }
        return List.of(expected.m_41786_(), TableRecipeBook.text(this.guide.wrong(cell) ? "wrong" : (this.guide.missing[cell] ? "missing" : "place"), new Object[0]).m_6881_().m_130940_(this.guide.wrong(cell) || this.guide.missing[cell] ? ChatFormatting.RED : ChatFormatting.GRAY));
    }

    private int logicalCell(int world) {
        for (int i = 0; i < 9; ++i) {
            if (TableGrid.worldIndex(this.station.side, i / 3, i % 3) != world) continue;
            return i;
        }
        return 0;
    }

    void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        if (this.guide.selected == null || this.station.craftProgress() >= 0.0) {
            return;
        }
        if (buffers instanceof MultiBufferSource.BufferSource) {
            MultiBufferSource.BufferSource source = (MultiBufferSource.BufferSource)buffers;
            source.m_109911_();
        }
        float appear = (float)Anim.smoother((Scene.clock() - this.selectedAt) / 0.28);
        for (int logical = 0; logical < 9; ++logical) {
            int g;
            int r;
            boolean red;
            boolean wrong = this.guide.wrong(logical);
            if (this.guide.grid[logical].m_43947_() && !wrong) continue;
            int world = TableGrid.worldIndex(this.station.side, logical / 3, logical % 3);
            TableLayout layout = TableLayouts.current();
            Vec3 base = layout.cell(this.station.pos, world).m_82520_(0.0, 0.002, 0.0);
            boolean bl = red = this.guide.missing[logical] || wrong;
            int n = red ? 255 : (r = this.guide.placed[logical] ? 143 : 166);
            int n2 = red ? 106 : (g = this.guide.placed[logical] ? 211 : 211);
            int b = red ? 98 : (this.guide.placed[logical] ? 176 : 235);
            pose.m_85836_();
            layout.cellPose(pose, this.station.pos, world, cam);
            TableRecipeBook.drawRim(pose, buffers, Vec3.f_82478_, r, g, b, appear * (this.guide.placed[logical] ? 0.3f : 0.7f));
            pose.m_85849_();
            if (this.guide.placed[logical] || wrong) continue;
            layout.renderItem(pose, buffers, cam, this.guide.ghost(logical), base, this.station.side, world, 1.0f, 0.0f, light, appear * 0.28f, false);
        }
        if (buffers instanceof MultiBufferSource.BufferSource) {
            MultiBufferSource.BufferSource source = (MultiBufferSource.BufferSource)buffers;
            source.m_109911_();
        }
    }

    private static void drawRim(PoseStack pose, MultiBufferSource buffers, Vec3 at, int r, int g, int b, float alpha) {
        double h = 0.08475;
        double t = 0.004;
        VertexConsumer out = buffers.m_6299_(RenderType.m_110502_());
        Matrix4f m = pose.m_85850_().m_252922_();
        TableRecipeBook.strip(out, m, at, -h, -h, h, -h + t, r, g, b, alpha);
        TableRecipeBook.strip(out, m, at, -h, h - t, h, h, r, g, b, alpha);
        TableRecipeBook.strip(out, m, at, -h, -h, -h + t, h, r, g, b, alpha);
        TableRecipeBook.strip(out, m, at, h - t, -h, h, h, r, g, b, alpha);
    }

    private static void strip(VertexConsumer out, Matrix4f m, Vec3 at, double x0, double z0, double x1, double z1, int r, int g, int b, float a) {
        int alpha = Math.round(a * 255.0f);
        out.m_252986_(m, (float)(at.f_82479_ + x0), (float)at.f_82480_, (float)(at.f_82481_ + z0)).m_6122_(r, g, b, alpha).m_5752_();
        out.m_252986_(m, (float)(at.f_82479_ + x0), (float)at.f_82480_, (float)(at.f_82481_ + z1)).m_6122_(r, g, b, alpha).m_5752_();
        out.m_252986_(m, (float)(at.f_82479_ + x1), (float)at.f_82480_, (float)(at.f_82481_ + z1)).m_6122_(r, g, b, alpha).m_5752_();
        out.m_252986_(m, (float)(at.f_82479_ + x1), (float)at.f_82480_, (float)(at.f_82481_ + z0)).m_6122_(r, g, b, alpha).m_5752_();
    }

    double[] togglePoint() {
        return new double[]{24.0f * this.scale + 8.0f, (float)this.toggleY + 14.0f * this.scale};
    }

    double[] layPoint() {
        return this.controlPoint(64, 212);
    }

    double[] controlPoint(int px, int py) {
        return new double[]{(float)this.x + (float)px * this.scale, (float)this.y + (float)py * this.scale};
    }

    @Nullable
    double[] recipePoint(ResourceLocation id) {
        for (int i = 0; i < this.filtered.size(); ++i) {
            if (!this.filtered.get((int)i).holder.id().equals((Object)id)) continue;
            this.page = i / 16;
            int slot = i % 16;
            return this.controlPoint(10 + slot % 4 * 27 + 12, 61 + slot / 4 * 27 + 12);
        }
        return null;
    }
}

