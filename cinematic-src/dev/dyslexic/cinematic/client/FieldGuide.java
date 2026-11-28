/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Window
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.Holder
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.ai.village.poi.PoiTypes
 *  net.minecraft.world.entity.npc.VillagerProfession
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.Window;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneScreen;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.TableRecipeGuide;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.client.guide.RecipeViewer;
import dev.dyslexic.cinematic.client.guide.RecipeViewers;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class FieldGuide {
    private static final int W = 260;
    private static final int H = 168;
    private static final int COVER = 7;
    private static final int SPINE = 6;
    private static final int PAGE_W = 120;
    private static final int COLS = 6;
    private static final int ROWS = 4;
    private static final int CELL = 18;
    private static final int PAGE_SIZE = 24;
    private static final int GRID_Y = 42;
    private static final int TOGGLE_W = 30;
    private static final int TOGGLE_H = 28;
    private static final int CLOSE_W = 14;
    private final EditBox search;
    private boolean open;
    private double shown;
    private Tab tab = Tab.RECIPES;
    private List<ItemStack> items = List.of();
    private String query = "";
    private int page;
    @Nullable
    private ItemStack selected;
    private List<RecipeViewer.StationRecipe> recipes = List.of();
    private int recipeIndex;
    private List<RecipeViewer.SourcePage> sources = List.of();
    @Nullable
    private ClientStation station;
    private ItemStack stationItem = ItemStack.f_41583_;
    @Nullable
    private Component profession;
    @Nullable
    private Runnable professionPage;
    private float scale = 1.0f;
    private int x;
    private int y;
    private int toggleY;

    FieldGuide() {
        this.search = new EditBox(Minecraft.m_91087_().f_91062_, 0, 0, 108, 11, FieldGuide.text("search", new Object[0]));
        this.search.m_94199_(64);
        this.search.m_94182_(false);
        this.search.m_94202_(-593957);
        this.search.m_257771_((Component)FieldGuide.text("search", new Object[0]).m_6881_().m_130938_(s -> s.m_178520_(11389108)));
        this.search.m_94151_(value -> {
            this.page = 0;
            this.query = value;
            this.refresh();
        });
    }

    private static Component text(String key, Object ... args) {
        return Component.m_237110_((String)("cinematic.guide." + key), (Object[])args);
    }

    @Nullable
    private static RecipeViewer viewer() {
        return RecipeViewers.get();
    }

    boolean present() {
        return FieldGuide.viewer() != null && Scene.phase == Scene.Phase.ACTIVE && Scene.station != null && !FreeLook.looking();
    }

    void tick() {
        if (Scene.station != this.station) {
            this.station = Scene.station;
            this.stationItem = this.station == null || Minecraft.m_91087_().f_91073_ == null ? ItemStack.f_41583_ : new ItemStack((ItemLike)Minecraft.m_91087_().f_91073_.m_8055_(this.station.pos).m_60734_().m_5456_());
            this.profession = FieldGuide.professionOf(this.station);
            this.professionPage = null;
            this.select(null);
            this.refresh();
        }
    }

    private void refresh() {
        RecipeViewer viewer = FieldGuide.viewer();
        if (viewer == null || this.stationItem.m_41619_()) {
            this.items = List.of();
            return;
        }
        this.items = this.query.isBlank() ? viewer.stationOutputs(this.stationItem) : viewer.search(this.query, 240);
        this.page = Mth.m_14045_((int)this.page, (int)0, (int)(this.pages() - 1));
        if (this.professionPage == null && this.profession != null) {
            this.professionPage = viewer.professionPage(this.stationItem);
        }
    }

    private void select(@Nullable ItemStack stack) {
        this.selected = stack;
        this.recipeIndex = 0;
        RecipeViewer viewer = FieldGuide.viewer();
        this.recipes = stack == null || viewer == null ? List.of() : viewer.stationRecipes(this.stationItem, stack);
        List<Object> list = this.sources = stack == null || viewer == null ? List.of() : viewer.sources(stack);
        if (this.tab == Tab.SOURCES && stack == null) {
            this.tab = Tab.RECIPES;
        }
    }

    @Nullable
    private static Component professionOf(@Nullable ClientStation station) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (station == null || level == null) {
            return null;
        }
        Optional poi = PoiTypes.m_218075_((BlockState)level.m_8055_(station.pos));
        if (poi.isEmpty()) {
            return null;
        }
        for (VillagerProfession profession : BuiltInRegistries.f_256735_) {
            if (profession == VillagerProfession.f_35585_ || !profession.f_219628_().test((Holder)poi.get())) continue;
            ResourceLocation key = BuiltInRegistries.f_256735_.m_7981_((Object)profession);
            String name = key.m_135827_().equals("minecraft") ? key.m_135815_() : key.m_135827_() + "." + key.m_135815_();
            return Component.m_237115_((String)("entity.minecraft.villager." + name));
        }
        return null;
    }

    private boolean showsSources() {
        return this.selected != null && !this.sources.isEmpty();
    }

    private boolean showsProfession() {
        return this.profession != null && this.professionPage != null;
    }

    private int pages() {
        return Math.max(1, (this.items.size() + 24 - 1) / 24);
    }

    private void place() {
        Window window = Minecraft.m_91087_().m_91268_();
        int width = window.m_85445_();
        int height = window.m_85446_();
        this.scale = Math.min(1.0f, Math.min((float)height / 300.0f, (float)width / 560.0f));
        int right = width - 8;
        double workRight = 0.0;
        if (this.station != null) {
            for (Vec3 sample : this.station.workSamples()) {
                double[] at = Picking.toGui(sample);
                if (at == null) continue;
                workRight = Math.max(workRight, at[0]);
            }
        }
        float room = (float)(((double)right - workRight - 10.0) / 260.0);
        if (workRight > 0.0 && room < this.scale) {
            this.scale = Math.max(0.62f, room);
        }
        double slide = (1.0 - Anim.outCubic(this.shown)) * 276.0 * (double)this.scale;
        this.x = (int)Math.round((double)((float)right - 260.0f * this.scale) + slide);
        this.y = Mth.m_14045_((int)Math.round((float)height * 0.16f), (int)8, (int)Math.max(8, height - (int)(168.0f * this.scale) - 30));
        this.toggleY = Math.round((float)height * 0.45f);
        if (Settings.developerMode() && SceneScreen.editor() != null) {
            this.toggleY -= Math.round(28.0f * this.scale) + 6;
        }
    }

    private double lx(double mx) {
        return (mx - (double)this.x) / (double)this.scale;
    }

    private double ly(double my) {
        return (my - (double)this.y) / (double)this.scale;
    }

    private int leftPage() {
        return 7;
    }

    private int rightPage() {
        return 133;
    }

    private boolean inBook(double mx, double my) {
        return this.shown > 0.05 && this.lx(mx) >= 0.0 && this.lx(mx) < 260.0 && this.ly(my) >= -14.0 && this.ly(my) < 168.0;
    }

    private boolean inToggle(double mx, double my) {
        Window window = Minecraft.m_91087_().m_91268_();
        int width = window.m_85445_();
        return !this.open && this.shown < 0.05 && mx >= (double)((float)(width - 8) - 30.0f * this.scale) && mx < (double)(width - 8) && my >= (double)this.toggleY && my < (double)((float)this.toggleY + 28.0f * this.scale);
    }

    boolean over(double mx, double my) {
        return this.present() && (this.inBook(mx, my) || this.inToggle(mx, my));
    }

    void render(GuiGraphics g, Font font, float alpha) {
        if (!this.present()) {
            this.shown = 0.0;
            return;
        }
        this.shown = Anim.approach(this.shown, this.open ? 1.0 : 0.0, 9.0, Scene.frameDt);
        this.place();
        double[] mouse = Picking.mouseGui();
        if (this.shown < 0.05) {
            this.drawToggle(g, alpha, this.inToggle(mouse[0], mouse[1]));
            return;
        }
        float a = alpha * (float)Anim.smoother(this.shown * 1.4);
        int mx = (int)this.lx(mouse[0]);
        int my = (int)this.ly(mouse[1]);
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_((float)this.x, (float)this.y, 120.0f);
        g.m_280168_().m_85841_(this.scale, this.scale, 1.0f);
        this.drawTabs(g, font, a, mx, my);
        this.drawCover(g, a);
        float unfold = (float)Anim.smoother((this.shown - 0.25) / 0.75);
        if (unfold > 0.02f) {
            int spine = 127;
            int reach = Math.round(120.0f * unfold);
            g.m_280509_(spine - reach, 5, spine, 163, FieldGuide.color(a, 15590344));
            g.m_280509_(spine + 6, 5, spine + 6 + reach, 163, FieldGuide.color(a, 15919826));
            if (unfold > 0.6f) {
                float pa = a * (unfold - 0.6f) / 0.4f;
                this.drawLeftPage(g, font, pa, mx, my);
                switch (this.tab) {
                    case RECIPES: {
                        this.drawRecipePage(g, font, pa, mx, my, mouse);
                        break;
                    }
                    case SOURCES: {
                        this.drawSourcesPage(g, font, pa, mx, my);
                        break;
                    }
                    case PROFESSION: {
                        this.drawProfessionPage(g, font, pa, mx, my);
                    }
                }
            }
        }
        g.m_280168_().m_85849_();
    }

    private void drawToggle(GuiGraphics g, float alpha, boolean hover) {
        int width = Minecraft.m_91087_().m_91268_().m_85445_();
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_((float)(width - 8) - 30.0f * this.scale, (float)this.toggleY, 0.0f);
        g.m_280168_().m_85841_(this.scale, this.scale, 1.0f);
        g.m_280509_(0, 0, 30, 28, FieldGuide.color(alpha * 0.62f, 789520));
        g.m_280509_(5, 4, 26, 24, FieldGuide.color(alpha, 2046764));
        g.m_280509_(5, 4, 8, 24, FieldGuide.color(alpha, 1320217));
        g.m_280509_(10, 7, 23, 21, FieldGuide.color(alpha, hover ? 16183259 : 15590344));
        g.m_280509_(10, 11, 23, 12, FieldGuide.color(alpha * 0.5f, 12166268));
        g.m_280509_(10, 15, 21, 16, FieldGuide.color(alpha * 0.5f, 12166268));
        g.m_280509_(19, 4, 22, 12, FieldGuide.color(alpha, 11878446));
        g.m_280168_().m_85849_();
    }

    private void drawCover(GuiGraphics g, float a) {
        g.m_280509_(-2, -2, 262, 170, FieldGuide.color(a * 0.35f, 329738));
        g.m_280509_(0, 0, 260, 168, FieldGuide.color(a, 2046764));
        g.m_280509_(0, 0, 260, 1, FieldGuide.color(a, 3035712));
        g.m_280509_(0, 167, 260, 168, FieldGuide.color(a, 1386783));
        int spine = 127;
        g.m_280509_(spine, 0, spine + 6, 168, FieldGuide.color(a, 1320217));
        g.m_280509_(spine + 3, 2, spine + 3 + 1, 166, FieldGuide.color(a * 0.6f, 3035712));
    }

    private void drawTabs(GuiGraphics g, Font font, float a, int mx, int my) {
        int tx = this.leftPage() + 6;
        for (Tab t : Tab.values()) {
            boolean hover;
            if (t == Tab.SOURCES && !this.showsSources() || t == Tab.PROFESSION && !this.showsProfession()) continue;
            Component label = FieldGuide.text("tab." + t.name().toLowerCase(Locale.ROOT), new Object[0]);
            int w = font.m_92852_((FormattedText)label) + 10;
            boolean active = t == this.tab;
            boolean bl = hover = mx >= tx && mx < tx + w && my >= -12 && my < 2;
            int body = t == Tab.RECIPES ? 3042119 : (t == Tab.SOURCES ? 9067051 : 5980026);
            g.m_280509_(tx, active ? -13 : -10, tx + w, 2, FieldGuide.color(a, active || hover ? FieldGuide.lighter(body) : body));
            g.m_280614_(font, label, tx + 5, active ? -10 : -8, FieldGuide.color(a, 16183259), false);
            t.x = tx;
            t.w = w;
            tx += w + 3;
        }
        boolean hover = FieldGuide.overClose(mx, my);
        g.m_280509_(244, hover ? -11 : -9, 258, 2, FieldGuide.color(a, hover ? FieldGuide.lighter(2046764) : 2046764));
        g.m_280056_(font, "\u00d7", 258 - (14 + font.m_92895_("\u00d7")) / 2, hover ? -8 : -7, FieldGuide.color(a, 16183259), false);
    }

    private static boolean overClose(int mx, int my) {
        return mx >= 244 && mx < 258 && my >= -12 && my < 2;
    }

    private void drawLeftPage(GuiGraphics g, Font font, float a, int mx, int my) {
        int index;
        int px = this.leftPage();
        g.m_280509_(px + 4, 5, px + 120 - 4, 21, FieldGuide.color(a, this.search.m_93696_() ? 7517579 : 12166268));
        g.m_280509_(px + 5, 6, px + 120 - 5, 20, FieldGuide.color(a, 2046764));
        this.search.m_252865_(px + 8);
        this.search.m_253211_(9);
        this.search.m_93674_(104);
        this.search.m_93650_(a);
        this.search.m_88315_(g, mx, my, 0.0f);
        Component head = this.query.isBlank() ? FieldGuide.text("made_here", this.items.size()) : FieldGuide.text("found", this.items.size());
        g.m_280614_(font, head, px + 6, 27, FieldGuide.color(a, 8022613), false);
        int hover = this.entryAt(mx, my);
        for (int slot = 0; slot < 24 && (index = this.page * 24 + slot) < this.items.size(); ++slot) {
            ItemStack stack = this.items.get(index);
            int sx = this.gridX() + slot % 6 * 18;
            int sy = 42 + slot / 6 * 18;
            g.m_280509_(sx, sy, sx + 18 - 1, sy + 18 - 1, FieldGuide.color(a * (index == hover ? 0.28f : 0.1f), 9206626));
            if (this.selected != null && ItemStack.m_150942_((ItemStack)this.selected, (ItemStack)stack)) {
                FieldGuide.rim(g, sx, sy, 17, FieldGuide.color(a, 3042119));
            }
            g.m_280480_(stack, sx + 1, sy + 1);
        }
        if (this.items.isEmpty()) {
            g.m_280554_(font, (FormattedText)FieldGuide.text(this.query.isBlank() ? "nothing_here" : "nothing_found", new Object[0]), px + 8, 64, 104, FieldGuide.color(a, 8022613));
        }
        int py = 120;
        g.m_280056_(font, "\u2039", px + 8, py, FieldGuide.color(a * (this.page > 0 ? 1.0f : 0.3f), 3812898), false);
        String counter = this.page + 1 + " / " + this.pages();
        g.m_280056_(font, counter, px + (120 - font.m_92895_(counter)) / 2, py, FieldGuide.color(a, 8022613), false);
        g.m_280056_(font, "\u203a", px + 120 - 12, py, FieldGuide.color(a * (this.page < this.pages() - 1 ? 1.0f : 0.3f), 3812898), false);
        RecipeViewer viewer = FieldGuide.viewer();
        if (viewer != null) {
            List lines = font.m_92923_((FormattedText)FieldGuide.text("keys_hint", viewer.keyNames()), 108);
            int ly = 159 - Math.min(2, lines.size()) * 9;
            for (int i = 0; i < Math.min(2, lines.size()); ++i) {
                g.m_280649_(font, (FormattedCharSequence)lines.get(i), px + 6, ly + i * 9, FieldGuide.color(a * 0.8f, 9206626), false);
            }
        }
    }

    private int gridX() {
        return this.leftPage() + 6;
    }

    private int entryAt(int mx, int my) {
        int gx = mx - this.gridX();
        int gy = my - 42;
        if (gx < 0 || gx >= 108 || gy < 0 || gy >= 72) {
            return -1;
        }
        int index = this.page * 24 + gy / 18 * 6 + gx / 18;
        return index < this.items.size() ? index : -1;
    }

    private List<Button> recipeButtons() {
        RecipeViewer.StationRecipe recipe;
        ArrayList<Button> buttons = new ArrayList<Button>();
        if (this.selected == null) {
            return buttons;
        }
        RecipeViewer.StationRecipe stationRecipe = recipe = this.recipes.isEmpty() ? null : this.recipes.get(this.recipeIndex);
        if (recipe != null && this.canHint(recipe)) {
            buttons.add(new Button(this.station instanceof TableStation ? "show_table" : "show_furnace", () -> this.hint(recipe)));
        }
        if (recipe != null) {
            buttons.add(new Button("open_recipe", () -> Scene.consult(recipe::open)));
        }
        buttons.add(new Button("how_to_get", () -> Scene.consult(() -> FieldGuide.viewer().showHowToGet(this.selected))));
        buttons.add(new Button("uses", () -> Scene.consult(() -> FieldGuide.viewer().showUses(this.selected))));
        int bx = this.rightPage() + 6;
        int bw = 108;
        int by = 157 - buttons.size() * 14;
        for (Button b : buttons) {
            b.x = bx;
            b.y = by;
            b.w = bw;
            by += 14;
        }
        return buttons;
    }

    private void drawRecipePage(GuiGraphics g, Font font, float a, int mx, int my, double[] mouse) {
        int bottom;
        int px = this.rightPage();
        if (this.selected == null) {
            g.m_280554_(font, (FormattedText)FieldGuide.text("choose", new Object[0]), px + 10, 40, 100, FieldGuide.color(a, 8022613));
            return;
        }
        g.m_280480_(this.selected, px + 6, 7);
        g.m_280056_(font, FieldGuide.fit(font, this.selected.m_41786_().getString(), 86), px + 26, 11, FieldGuide.color(a, 3812898), false);
        g.m_280509_(px + 5, 26, px + 120 - 5, 27, FieldGuide.color(a * 0.5f, 12166268));
        List<Button> buttons = this.recipeButtons();
        int top = 31;
        int n = bottom = buttons.isEmpty() ? 157 : buttons.get((int)0).y - 3;
        if (this.recipes.isEmpty()) {
            g.m_280554_(font, (FormattedText)FieldGuide.text("not_here", new Object[0]), px + 8, top + 4, 104, FieldGuide.color(a, 8022613));
        } else {
            RecipeViewer.StationRecipe recipe = this.recipes.get(this.recipeIndex);
            String count = this.recipes.size() > 1 ? this.recipeIndex + 1 + "/" + this.recipes.size() + " \u00b7 " : "";
            g.m_280056_(font, FieldGuide.fit(font, count + recipe.category().getString(), 90), px + 8, top, FieldGuide.color(a, 5917239), false);
            if (this.recipes.size() > 1) {
                g.m_280056_(font, "\u2039", px + 120 - 22, top, FieldGuide.color(a, 3812898), false);
                g.m_280056_(font, "\u203a", px + 120 - 12, top, FieldGuide.color(a, 3812898), false);
            }
            int areaTop = top + 11;
            int areaH = bottom - areaTop;
            int areaW = 110;
            float fit = Math.min(1.0f, Math.min((float)areaW / (float)Math.max(1, recipe.width()), (float)areaH / (float)Math.max(1, recipe.height())));
            if (fit > 0.3f) {
                g.m_280168_().m_85836_();
                g.m_280168_().m_252880_((float)(px + 5) + ((float)areaW - (float)recipe.width() * fit) / 2.0f, (float)areaTop + ((float)areaH - (float)recipe.height() * fit) / 2.0f, 0.0f);
                g.m_280168_().m_85841_(fit, fit, 1.0f);
                g.m_280246_(1.0f, 1.0f, 1.0f, a);
                recipe.draw(g, 0, 0, -1, -1);
                g.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
                g.m_280168_().m_85849_();
            }
        }
        for (Button b : buttons) {
            this.drawButton(g, font, b, a, mx, my);
        }
    }

    private void drawSourcesPage(GuiGraphics g, Font font, float a, int mx, int my) {
        int px = this.rightPage();
        g.m_280614_(font, FieldGuide.text("sources_title", new Object[0]), px + 8, 9, FieldGuide.color(a, 3812898), false);
        g.m_280509_(px + 5, 20, px + 120 - 5, 21, FieldGuide.color(a * 0.5f, 12166268));
        int ly = 26;
        for (RecipeViewer.SourcePage source : this.sources) {
            boolean hover = mx >= px + 5 && mx < px + 120 - 5 && my >= ly && my < ly + 19;
            g.m_280509_(px + 5, ly, px + 120 - 5, ly + 18, FieldGuide.color(a * (hover ? 0.3f : 0.1f), 9206626));
            source.icon().draw(g, px + 7, ly + 1);
            String title = source.title().getString();
            int w = 70;
            if (font.m_92895_(title) <= w) {
                g.m_280056_(font, title, px + 26, ly + 5, FieldGuide.color(a, 3812898), false);
            } else {
                String first = font.m_92834_(title, w);
                int cut = first.lastIndexOf(32);
                if (cut > 0) {
                    first = first.substring(0, cut);
                }
                g.m_280056_(font, first, px + 26, ly + 1, FieldGuide.color(a, 3812898), false);
                g.m_280056_(font, FieldGuide.fit(font, title.substring(first.length()).strip(), w), px + 26, ly + 10, FieldGuide.color(a, 3812898), false);
            }
            String n = String.valueOf(source.entries());
            g.m_280056_(font, n, px + 120 - 9 - font.m_92895_(n), ly + 5, FieldGuide.color(a, 9067051), false);
            if ((ly += 20) <= 138) continue;
            break;
        }
        g.m_280554_(font, (FormattedText)FieldGuide.text("sources_hint", new Object[0]), px + 8, 141, 104, FieldGuide.color(a * 0.9f, 9206626));
    }

    private void drawProfessionPage(GuiGraphics g, Font font, float a, int mx, int my) {
        int px = this.rightPage();
        g.m_280614_(font, FieldGuide.text("profession_title", new Object[0]), px + 8, 9, FieldGuide.color(a, 8022613), false);
        g.m_280614_(font, this.profession, px + 8, 24, FieldGuide.color(a, 3812898), false);
        g.m_280509_(px + 5, 36, px + 120 - 5, 37, FieldGuide.color(a * 0.5f, 12166268));
        g.m_280554_(font, (FormattedText)FieldGuide.text("profession_text", new Object[0]), px + 8, 44, 104, FieldGuide.color(a, 5917239));
        Button open = this.professionButton();
        this.drawButton(g, font, open, a, mx, my);
    }

    private Button professionButton() {
        Button b = new Button("open_jep", () -> Scene.consult(this.professionPage));
        b.x = this.rightPage() + 6;
        b.y = 143;
        b.w = 108;
        return b;
    }

    private void drawButton(GuiGraphics g, Font font, Button b, float a, int mx, int my) {
        boolean hover = b.over(mx, my);
        g.m_280509_(b.x, b.y, b.x + b.w, b.y + 12, FieldGuide.color(a * (hover ? 0.95f : 0.8f), hover ? 3902044 : 3042119));
        g.m_280509_(b.x, b.y + 11, b.x + b.w, b.y + 12, FieldGuide.color(a, 2050609));
        Component label = FieldGuide.text(b.key, new Object[0]);
        g.m_280614_(font, label, b.x + (b.w - font.m_92852_((FormattedText)label)) / 2, b.y + 2, FieldGuide.color(a, 16183259), false);
    }

    private boolean canHint(RecipeViewer.StationRecipe recipe) {
        ResourceLocation id = recipe.recipeId();
        if (id == null) {
            return false;
        }
        ClientStation clientStation = this.station;
        if (clientStation instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            return table.recipeBook.guide.entry(id) != null;
        }
        clientStation = this.station;
        if (clientStation instanceof FurnaceStation) {
            FurnaceStation furnace = (FurnaceStation)clientStation;
            return furnace.cookingInput(id) != null;
        }
        return false;
    }

    private void hint(RecipeViewer.StationRecipe recipe) {
        ResourceLocation id = recipe.recipeId();
        if (id == null) {
            return;
        }
        ClientStation clientStation = this.station;
        if (clientStation instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            TableRecipeGuide.Entry entry = table.recipeBook.guide.entry(id);
            if (entry != null) {
                table.recipeBook.show(entry);
            }
        } else {
            clientStation = this.station;
            if (clientStation instanceof FurnaceStation) {
                FurnaceStation furnace = (FurnaceStation)clientStation;
                furnace.showHint(furnace.cookingInput(id));
            }
        }
    }

    boolean click(double mouseX, double mouseY, int button) {
        if (!this.present()) {
            return false;
        }
        if (this.inToggle(mouseX, mouseY)) {
            this.open = true;
            this.refresh();
            return true;
        }
        if (!this.inBook(mouseX, mouseY)) {
            this.search.m_93692_(false);
            return false;
        }
        if (button != 0 || this.shown < 0.9) {
            return true;
        }
        int mx = (int)this.lx(mouseX);
        int my = (int)this.ly(mouseY);
        for (Tab t : Tab.values()) {
            if (t.w <= 0 || mx < t.x || mx >= t.x + t.w || my < -13 || my >= 2 || t == Tab.SOURCES && !this.showsSources() || t == Tab.PROFESSION && !this.showsProfession()) continue;
            this.tab = t;
            return true;
        }
        if (FieldGuide.overClose(mx, my)) {
            this.fold();
            return true;
        }
        boolean inSearch = mx >= this.leftPage() + 4 && mx < this.leftPage() + 120 - 4 && my >= 6 && my < 21;
        this.search.m_93692_(inSearch);
        if (inSearch) {
            return true;
        }
        int entry = this.entryAt(mx, my);
        if (entry >= 0) {
            this.select(this.items.get(entry).m_41777_());
            this.tab = Tab.RECIPES;
            return true;
        }
        int py = 120;
        if (my >= py - 3 && my < py + 10) {
            if (mx < this.leftPage() + 24) {
                this.page = Math.max(0, this.page - 1);
            } else if (mx > this.leftPage() + 120 - 24) {
                this.page = Math.min(this.pages() - 1, this.page + 1);
            }
            return true;
        }
        if (this.tab == Tab.RECIPES) {
            if (this.recipes.size() > 1 && my >= 29 && my < 41) {
                if (mx >= this.rightPage() + 120 - 24 && mx < this.rightPage() + 120 - 14) {
                    this.recipeIndex = (this.recipeIndex + this.recipes.size() - 1) % this.recipes.size();
                } else if (mx >= this.rightPage() + 120 - 14) {
                    this.recipeIndex = (this.recipeIndex + 1) % this.recipes.size();
                }
                return true;
            }
            for (Button b : this.recipeButtons()) {
                if (!b.over(mx, my)) continue;
                b.action.run();
                return true;
            }
        } else if (this.tab == Tab.SOURCES) {
            int ly = 26;
            for (RecipeViewer.SourcePage source : this.sources) {
                if (mx >= this.rightPage() + 5 && mx < this.rightPage() + 120 - 5 && my >= ly && my < ly + 19) {
                    Scene.consult(source.open());
                    return true;
                }
                ly += 20;
            }
        } else if (this.tab == Tab.PROFESSION && this.professionButton().over(mx, my)) {
            this.professionButton().action.run();
        }
        return true;
    }

    boolean scroll(double mouseX, double mouseY, double amount) {
        if (!this.over(mouseX, mouseY)) {
            return false;
        }
        if (this.open) {
            this.page = Mth.m_14045_((int)(this.page - (int)Math.signum(amount)), (int)0, (int)(this.pages() - 1));
        }
        return true;
    }

    boolean charTyped(char c, int modifiers) {
        return this.present() && this.search.m_93696_() && this.search.m_5534_(c, modifiers);
    }

    boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!this.present() || !this.search.m_93696_()) {
            return false;
        }
        if (key == 256 || key == 257 || key == 335) {
            this.search.m_93692_(false);
        } else {
            this.search.m_7933_(key, scanCode, modifiers);
        }
        return true;
    }

    boolean viewerKey(int key, int scanCode, @Nullable ItemStack hovered) {
        ItemStack stack;
        RecipeViewer viewer = FieldGuide.viewer();
        if (viewer == null || Scene.phase != Scene.Phase.ACTIVE) {
            return false;
        }
        double[] mouse = Picking.mouseGui();
        ItemStack itemStack = stack = this.inBook(mouse[0], mouse[1]) ? this.hoveredEntry(mouse) : hovered;
        if (stack == null || stack.m_41619_()) {
            return false;
        }
        if (viewer.isShowRecipeKey(key, scanCode)) {
            Scene.consult(() -> viewer.showHowToGet(stack));
        } else if (viewer.isShowUsesKey(key, scanCode)) {
            Scene.consult(() -> viewer.showUses(stack));
        } else {
            return false;
        }
        return true;
    }

    @Nullable
    private ItemStack hoveredEntry(double[] mouse) {
        int entry = this.entryAt((int)this.lx(mouse[0]), (int)this.ly(mouse[1]));
        return entry < 0 ? null : this.items.get(entry);
    }

    @Nullable
    List<Component> tooltip(double mouseX, double mouseY) {
        int my;
        if (!this.present()) {
            return null;
        }
        if (this.inToggle(mouseX, mouseY)) {
            return List.of(FieldGuide.text("title", new Object[0]), FieldGuide.text("toggle_tip", new Object[0]).m_6881_().m_130940_(ChatFormatting.GRAY));
        }
        if (!this.inBook(mouseX, mouseY) || this.shown < 0.9) {
            return null;
        }
        int mx = (int)this.lx(mouseX);
        if (FieldGuide.overClose(mx, my = (int)this.ly(mouseY))) {
            return List.of(FieldGuide.text("close", new Object[0]));
        }
        int entry = this.entryAt(mx, my);
        if (entry >= 0) {
            ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)Minecraft.m_91087_(), (ItemStack)this.items.get(entry)));
            lines.add((Component)FieldGuide.text("entry_tip", new Object[0]).m_6881_().m_130940_(ChatFormatting.GRAY));
            return lines;
        }
        if (this.tab == Tab.RECIPES) {
            for (Button b : this.recipeButtons()) {
                if (!b.over(mx, my)) continue;
                return List.of(FieldGuide.text(b.key + "_tip", new Object[0]));
            }
        }
        if (this.tab == Tab.PROFESSION && this.professionButton().over(mx, my)) {
            return List.of(FieldGuide.text("open_jep_tip", new Object[0]));
        }
        if (this.tab == Tab.SOURCES && my >= 26 && mx >= this.rightPage() && (my - 26) / 20 < this.sources.size()) {
            return List.of(this.sources.get((my - 26) / 20).title(), FieldGuide.text("open_jer_tip", new Object[0]).m_6881_().m_130940_(ChatFormatting.GRAY));
        }
        return null;
    }

    void fold() {
        this.open = false;
        this.search.m_93692_(false);
    }

    private static void rim(GuiGraphics g, int x, int y, int size, int color) {
        g.m_280509_(x, y, x + size, y + 1, color);
        g.m_280509_(x, y + size - 1, x + size, y + size, color);
        g.m_280509_(x, y, x + 1, y + size, color);
        g.m_280509_(x + size - 1, y, x + size, y + size, color);
    }

    private static String fit(Font font, String line, int width) {
        if (font.m_92895_(line) <= width) {
            return line;
        }
        return font.m_92834_(line, width - font.m_92895_("\u2026")).stripTrailing() + "\u2026";
    }

    private static int lighter(int rgb) {
        int r = Math.min(255, (rgb >> 16 & 0xFF) + 28);
        int gr = Math.min(255, (rgb >> 8 & 0xFF) + 28);
        int b = Math.min(255, (rgb & 0xFF) + 28);
        return r << 16 | gr << 8 | b;
    }

    private static int color(float alpha, int rgb) {
        return Mth.m_14045_((int)Math.round(alpha * 255.0f), (int)0, (int)255) << 24 | rgb & 0xFFFFFF;
    }

    double[] togglePoint() {
        int width = Minecraft.m_91087_().m_91268_().m_85445_();
        return new double[]{(float)(width - 8) - 30.0f * this.scale / 2.0f, (float)this.toggleY + 28.0f * this.scale / 2.0f};
    }

    double[] point(int px, int py) {
        return new double[]{(float)this.x + (float)px * this.scale, (float)this.y + (float)py * this.scale};
    }

    double[] searchPoint() {
        return this.point(this.leftPage() + 60, 13);
    }

    double[] entryPoint(int slot) {
        return this.point(this.gridX() + slot % 6 * 18 + 9, 42 + slot / 6 * 18 + 9);
    }

    double[] tabPoint(String name) {
        for (Tab t : Tab.values()) {
            if (!t.name().equalsIgnoreCase(name)) continue;
            return this.point(t.x + t.w / 2, -5);
        }
        return this.point(0, 0);
    }

    @Nullable
    double[] buttonPoint(String key) {
        for (Button b : this.tab == Tab.PROFESSION ? List.of(this.professionButton()) : this.recipeButtons()) {
            if (!b.key.equals(key)) continue;
            return this.point(b.x + b.w / 2, b.y + 6);
        }
        return null;
    }

    double[] closePoint() {
        return this.point(251, -5);
    }

    double[] sourcePoint(int index) {
        return this.point(this.rightPage() + 60, 26 + index * 20 + 9);
    }

    boolean opened() {
        return this.open && this.shown > 0.95;
    }

    @Nullable
    ItemStack selected() {
        return this.selected;
    }

    int recipeCount() {
        return this.recipes.size();
    }

    int sourceCount() {
        return this.sources.size();
    }

    @Nullable
    Component profession() {
        return this.profession;
    }

    boolean professionPageReady() {
        return this.professionPage != null;
    }

    String tabName() {
        return this.tab.name();
    }

    int itemCount() {
        return this.items.size();
    }

    int indexOf(Item item) {
        for (int i = 0; i < this.items.size(); ++i) {
            if (!this.items.get(i).m_150930_(item)) continue;
            return i - this.page * 24;
        }
        return -1;
    }

    private static enum Tab {
        RECIPES,
        SOURCES,
        PROFESSION;

        int x;
        int w;
    }

    private static final class Button {
        final String key;
        final Runnable action;
        int x;
        int y;
        int w;

        Button(String key, Runnable action) {
            this.key = key;
            this.action = action;
        }

        boolean over(int mx, int my) {
            return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + 12;
        }
    }
}

