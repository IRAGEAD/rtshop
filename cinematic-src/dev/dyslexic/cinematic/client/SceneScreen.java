/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.Options
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.NonNullList
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.FieldGuide;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.InventoryEffects;
import dev.dyslexic.cinematic.client.LayoutEditor;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RecipeChoiceStrip;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.client.SurfaceStation;
import dev.dyslexic.cinematic.client.TableStation;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SceneScreen
extends Screen {
    private static final int SLOT = 18;
    private static final int PAD = 5;
    private static final int ROW_GAP = 4;
    private static final int INNER = 162;
    private static final int FIELD_W = 170;
    private double expand;
    private float scale = 1.0f;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private final InventoryEffects inventoryEffects = new InventoryEffects();
    @Nullable
    private EditBox field;
    @Nullable
    private Consumer<String> commit;
    final FieldGuide guide = new FieldGuide();

    public SceneScreen() {
        super((Component)Component.m_237115_((String)"cinematic.scene"));
    }

    public boolean m_7043_() {
        return false;
    }

    protected void m_7856_() {
        super.m_7856_();
        Scene.consulted();
    }

    public void m_86600_() {
        super.m_86600_();
        if (!Scene.active()) {
            Minecraft.m_91087_().m_91152_(null);
            return;
        }
        this.inventoryEffects.tick();
        this.guide.tick();
    }

    public void m_280273_(GuiGraphics graphics) {
    }

    private static float presence() {
        if (Scene.transition && Scene.phase == Scene.Phase.INTRO) {
            return 1.0f;
        }
        if (Scene.phase == Scene.Phase.OUTRO) {
            return (float)(1.0 - Anim.inOutSine(Scene.time / 0.22));
        }
        return (float)Anim.outCubic(Scene.time / 0.45);
    }

    private static boolean working() {
        return Scene.phase == Scene.Phase.ACTIVE && Scene.station != null;
    }

    static LayoutEditor editor() {
        ClientStation clientStation = Scene.station;
        if (clientStation instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            return table.gridEditor;
        }
        clientStation = Scene.station;
        if (clientStation instanceof FurnaceStation) {
            FurnaceStation furnace = (FurnaceStation)clientStation;
            return furnace.gridEditor;
        }
        clientStation = Scene.station;
        if (clientStation instanceof SurfaceStation) {
            SurfaceStation surface = (SurfaceStation)clientStation;
            return surface.gridEditor;
        }
        clientStation = Scene.station;
        if (clientStation instanceof SpatialTarget) {
            SpatialTarget spatial = (SpatialTarget)((Object)clientStation);
            return spatial.layoutEditor();
        }
        return null;
    }

    public static void edit(String initial, int maxLength, Consumer<String> onCommit) {
        Screen screen = Minecraft.m_91087_().f_91080_;
        if (screen instanceof SceneScreen) {
            SceneScreen screen2 = (SceneScreen)screen;
            screen2.openField(initial, maxLength, onCommit);
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Nullable
    public static String editing() {
        Screen screen = Minecraft.m_91087_().f_91080_;
        if (!(screen instanceof SceneScreen)) return null;
        SceneScreen screen2 = (SceneScreen)screen;
        if (screen2.field == null) return null;
        String string = screen2.field.m_94155_();
        return string;
    }

    public static void commitEdit(String value) {
        Screen screen = Minecraft.m_91087_().f_91080_;
        if (screen instanceof SceneScreen) {
            SceneScreen screen2 = (SceneScreen)screen;
            if (screen2.field != null) {
                screen2.field.m_94144_(value);
                screen2.closeField(true);
            }
        }
    }

    private void openField(String initial, int maxLength, Consumer<String> onCommit) {
        EditBox box = new EditBox(this.f_96547_, 0, 0, 170, 12, (Component)Component.m_237115_((String)"cinematic.scene.text"));
        box.m_94199_(maxLength);
        box.m_94182_(false);
        box.m_94202_(-857384);
        box.m_94144_(initial);
        box.m_93692_(true);
        box.m_94201_();
        box.m_94208_(0);
        this.field = box;
        this.commit = onCommit;
    }

    private void closeField(boolean keep) {
        EditBox box = this.field;
        Consumer<String> then = this.commit;
        this.field = null;
        this.commit = null;
        if (keep && box != null && then != null) {
            then.accept(box.m_94155_());
        }
    }

    private void drawField(GuiGraphics g, float alpha) {
        if (this.field == null) {
            return;
        }
        int x = (this.f_96543_ - 170) / 2;
        int y = this.f_96544_ - 52;
        g.m_280509_(x - 8, y - 15, x + 170 + 8, y + 14, SceneScreen.argb(0.62f * alpha, 12, 12, 16));
        g.m_280509_(x - 8, y + 13, x + 170 + 8, y + 14, SceneScreen.argb(0.45f * alpha, 255, 224, 122));
        g.m_280614_(this.f_96547_, (Component)Component.m_237115_((String)"cinematic.scene.text"), x, y - 11, SceneScreen.argb(0.7f * alpha, 200, 192, 176), false);
        this.field.m_252865_(x);
        this.field.m_253211_(y + 1);
        this.field.m_88315_(g, 0, 0, 0.0f);
    }

    private void layout() {
        this.scale = Mth.m_14036_((float)((float)this.f_96544_ * 0.068f / 18.0f), (float)0.6f, (float)1.25f);
        float appear = SceneScreen.presence();
        this.panelW = Math.round(172.0f * this.scale);
        int full = 28 + (int)Math.round(this.expand * 58.0);
        this.panelH = Math.round((float)full * this.scale);
        this.panelX = (this.f_96543_ - this.panelW) / 2;
        this.panelY = Math.round(6.0f - (1.0f - appear) * (float)(this.panelH + 10));
    }

    private boolean overPanel(double mx, double my) {
        int reach = Math.round((float)(28 + (this.expand > 0.5 ? 58 : 0)) * this.scale);
        return mx >= (double)(this.panelX - 4) && mx <= (double)(this.panelX + this.panelW + 4) && my >= 0.0 && my <= (double)(this.panelY + Math.max(this.panelH, reach) + 4);
    }

    private int slotAt(double mx, double my) {
        double lx = (mx - (double)this.panelX) / (double)this.scale - 5.0;
        double ly = (my - (double)this.panelY) / (double)this.scale - 5.0;
        if (lx < 0.0 || lx >= 162.0) {
            return -1;
        }
        int col = (int)(lx / 18.0);
        if (ly >= 0.0 && ly < 18.0) {
            return 36 + col;
        }
        double below = ly - 18.0 - 4.0;
        if (this.expand > 0.9 && below >= 0.0 && below < 54.0) {
            return 9 + (int)(below / 18.0) * 9 + col;
        }
        return -1;
    }

    private static int inventoryIndex(int menuSlot) {
        return menuSlot >= 36 ? menuSlot - 36 : menuSlot;
    }

    private static int menuSlot(LocalPlayer player, int drawerSlot) {
        int index = SceneScreen.inventoryIndex(drawerSlot);
        NonNullList slots = player.f_36096_.f_38839_;
        for (int i = 0; i < slots.size(); ++i) {
            if (((Slot)slots.get((int)i)).f_40218_ != player.m_150109_() || ((Slot)slots.get(i)).m_150661_() != index) continue;
            return i;
        }
        return -1;
    }

    /*
     * Unable to fully structure code
     */
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        block13: {
            mc = Minecraft.m_91087_();
            player = mc.f_91074_;
            station = Scene.station;
            if (player == null || station == null) {
                return;
            }
            drawer = station.showsInventory();
            look = (float)FreeLook.amount();
            v0 = over = SceneScreen.working() != false && drawer != false && FreeLook.looking() == false && this.overPanel(mouseX, mouseY) != false;
            if (over) ** GOTO lbl-1000
            if (!(station instanceof TableStation)) break block13;
            table = (TableStation)station;
            if (table.recipeBook.over(mouseX, mouseY)) ** GOTO lbl-1000
        }
        if (SceneScreen.editor() != null && SceneScreen.editor().over(mouseX, mouseY) != false || this.guideShown() != false && this.guide.over(mouseX, mouseY) != false || station.choices() != null && station.choices().over(mouseX, mouseY)) lbl-1000:
        // 3 sources

        {
            v1 = true;
        } else {
            v1 = false;
        }
        Scene.overPanel = v1;
        this.expand = Anim.approach(this.expand, over != false ? 1.0 : 0.0, 13.0, Scene.frameDt);
        this.layout();
        alpha = (SceneScreen.working() != false ? (float)Anim.smoother(Scene.time / 0.4) : SceneScreen.presence()) * (1.0f - look);
        hovered = over != false ? this.slotAt(mouseX, mouseY) : -1;
        this.inventoryEffects.beginFrame(SceneScreen.working() != false && FreeLook.looking() == false && this.field == null && (SceneScreen.editor() == null || SceneScreen.editor().visible() == false));
        if (alpha > 0.01f) {
            station.renderScreen(graphics, this.f_96547_, alpha);
            if (station.choices() != null) {
                station.choices().render(graphics, this.f_96547_, alpha);
            }
            if (this.guideShown()) {
                this.guide.render(graphics, this.f_96547_, alpha);
            }
            if (drawer) {
                this.drawPanel(graphics, player, hovered, alpha);
            }
            this.drawHints(graphics, station.hint(), alpha);
        }
        if (look > 0.01f) {
            this.drawLookHint(graphics, look);
        }
        if (FreeLook.looking()) {
            return;
        }
        if (SceneScreen.working() && Scene.neighbor != null) {
            this.drawMoveOver(graphics, mouseX, mouseY);
        }
        if (!SceneScreen.working()) {
            this.closeField(false);
        }
        this.drawField(graphics, alpha);
        if (SceneScreen.working()) {
            this.drawTooltip(graphics, player, station, hovered, mouseX, mouseY);
        }
        carried = player.f_36096_.m_142621_();
        this.inventoryEffects.endFrame(Scene.overPanel);
        if (!(!SceneScreen.working() || carried.m_41619_() || Hands.itemInHand() || station.handsOverride() != null || SceneScreen.editor() != null && SceneScreen.editor().visible())) {
            graphics.m_280168_().m_85836_();
            graphics.m_280168_().m_252880_(0.0f, 0.0f, 300.0f);
            this.inventoryEffects.carried(graphics, carried, mouseX, mouseY, (BiConsumer<Integer, Integer>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;Ljava/lang/Object;)V, lambda$render$0(net.minecraft.client.gui.GuiGraphics net.minecraft.world.item.ItemStack java.lang.Integer java.lang.Integer ), (Ljava/lang/Integer;Ljava/lang/Integer;)V)((SceneScreen)this, (GuiGraphics)graphics, (ItemStack)carried));
            graphics.m_280168_().m_85849_();
        }
    }

    private void drawPanel(GuiGraphics g, LocalPlayer player, int hovered, float alpha) {
        int bg = SceneScreen.argb(0.46f * alpha, 12, 12, 16);
        int edge = SceneScreen.argb(0.2f * alpha, 255, 255, 255);
        g.m_280509_(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, bg);
        g.m_280509_(this.panelX, this.panelY + this.panelH - 1, this.panelX + this.panelW, this.panelY + this.panelH, edge);
        g.m_280509_(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelH - 1, SceneScreen.argb(0.08f * alpha, 255, 255, 255));
        g.m_280509_(this.panelX + this.panelW - 1, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH - 1, SceneScreen.argb(0.08f * alpha, 255, 255, 255));
        g.m_280588_(this.panelX, Math.max(0, this.panelY), this.panelX + this.panelW, this.panelY + this.panelH);
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_((float)this.panelX, (float)this.panelY, 0.0f);
        g.m_280168_().m_85841_(this.scale, this.scale, 1.0f);
        int selected = player.m_150109_().f_35977_;
        for (int col = 0; col < 9; ++col) {
            this.drawSlot(g, player, 36 + col, 5 + col * 18, 5, hovered, col == selected, alpha);
        }
        if (this.expand > 0.02) {
            float rows = (float)Anim.smoother(this.expand);
            int dividerY = 25;
            g.m_280509_(7, dividerY, 165, dividerY + 1, SceneScreen.argb(0.12f * alpha * rows, 255, 255, 255));
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.drawSlot(g, player, 9 + row * 9 + col, 5 + col * 18, 27 + row * 18, hovered, false, alpha * rows);
                }
            }
        } else {
            int gx = 86;
            g.m_280509_(gx - 9, 25, gx + 9, 26, SceneScreen.argb(0.35f * alpha, 255, 255, 255));
        }
        g.m_280168_().m_85849_();
        g.m_280618_();
    }

    private void drawSlot(GuiGraphics g, LocalPlayer player, int menuSlot, int x, int y, int hovered, boolean selected, float alpha) {
        g.m_280509_(x + 1, y + 1, x + 18 - 1, y + 18 - 1, SceneScreen.argb(0.13f * alpha, 255, 255, 255));
        if (selected) {
            int c = SceneScreen.argb(0.75f * alpha, 255, 226, 150);
            g.m_280509_(x, y, x + 18, y + 1, c);
            g.m_280509_(x, y + 18 - 1, x + 18, y + 18, c);
            g.m_280509_(x, y, x + 1, y + 18, c);
            g.m_280509_(x + 18 - 1, y, x + 18, y + 18, c);
        }
        if (menuSlot == hovered) {
            g.m_280509_(x + 1, y + 1, x + 18 - 1, y + 18 - 1, SceneScreen.argb(0.35f * alpha, 255, 255, 255));
        }
        if (alpha > 0.35f) {
            ItemStack stack = player.m_150109_().m_8020_(SceneScreen.inventoryIndex(menuSlot));
            double cx = (float)this.panelX + (float)(x + 9) * this.scale;
            double cy = (float)this.panelY + (float)(y + 9) * this.scale;
            if (cy >= (double)Math.max(0, this.panelY) && cy < (double)(this.panelY + this.panelH)) {
                this.inventoryEffects.drawer(SceneScreen.inventoryIndex(menuSlot), stack, cx, cy, menuSlot == hovered);
            }
            g.m_280480_(stack, x + 1, y + 1);
            g.m_280370_(this.f_96547_, stack, x + 1, y + 1);
        }
    }

    private void drawHints(GuiGraphics g, Component hint, float alpha) {
        if (!Settings.controlHints()) {
            return;
        }
        double fade = SceneScreen.working() ? 1.0 - 0.55 * Anim.smoother((Scene.time - 7.0) / 2.0) : 0.45;
        int a = (int)((double)alpha * fade * 200.0);
        if (a < 8) {
            return;
        }
        int w = this.f_96547_.m_92852_((FormattedText)hint);
        float s = Math.min(1.0f, ((float)this.f_96543_ - 24.0f) / (float)w);
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_((float)this.f_96543_ / 2.0f, (float)this.f_96544_ - 6.0f - 11.0f * s, 0.0f);
        g.m_280168_().m_85841_(s, s, 1.0f);
        g.m_280509_(-w / 2 - 6, -4, w / 2 + 6, 11, (int)((double)a * 0.45) << 24);
        g.m_280614_(this.f_96547_, hint, -w / 2, 0, a << 24 | 0xF2EAD8, false);
        g.m_280168_().m_85849_();
    }

    private void drawLookHint(GuiGraphics g, float look) {
        Component hint = FreeLook.hint();
        int a = Math.round(look * 190.0f);
        if (a < 8) {
            return;
        }
        int w = this.f_96547_.m_92852_((FormattedText)hint);
        int y = this.f_96544_ - (int)Math.round((double)this.f_96544_ * 0.085 * 0.55) - 14;
        g.m_280509_(this.f_96543_ / 2 - w / 2 - 6, y - 3, this.f_96543_ / 2 + w / 2 + 6, y + 11, (int)((double)a * 0.45) << 24);
        g.m_280614_(this.f_96547_, hint, this.f_96543_ / 2 - w / 2, y, a << 24 | 0xF2EAD8, false);
    }

    private void drawMoveOver(GuiGraphics g, int mx, int my) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || Scene.neighbor == null) {
            return;
        }
        MutableComponent name = mc.f_91073_.m_8055_(Scene.neighbor).m_60734_().m_49954_();
        MutableComponent how = Component.m_237115_((String)"cinematic.move_over");
        int nameW = this.f_96547_.m_92852_((FormattedText)name);
        int howW = this.f_96547_.m_92852_((FormattedText)how);
        int w = nameW + 9 + howW + 12;
        int h = 13;
        int x = Math.min(mx + 10, this.f_96543_ - w - 2);
        int y = Math.min(my + 10, this.f_96544_ - h - 2);
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_(0.0f, 0.0f, 400.0f);
        g.m_280509_(x, y, x + w, y + h, SceneScreen.argb(0.62f, 12, 12, 16));
        g.m_280509_(x, y + h - 1, x + w, y + h, SceneScreen.argb(0.5f, 255, 226, 150));
        g.m_280614_(this.f_96547_, (Component)name, x + 6, y + 3, -7520, false);
        g.m_280056_(this.f_96547_, "\u00b7", x + 6 + nameW + 2, y + 3, -7699336, false);
        g.m_280614_(this.f_96547_, (Component)how, x + 6 + nameW + 9, y + 3, -857384, false);
        g.m_280168_().m_85849_();
    }

    private void drawTooltip(GuiGraphics g, LocalPlayer player, ClientStation station, int hovered, int mx, int my) {
        List<Component> lines;
        List<Component> guideLines;
        ItemStack carried = player.f_36096_.m_142621_();
        List<Component> list = guideLines = this.guideShown() && carried.m_41619_() ? this.guide.tooltip(mx, my) : null;
        if (guideLines != null) {
            g.m_280666_(this.f_96547_, guideLines, mx, my);
            return;
        }
        if (hovered >= 0) {
            ItemStack stack = player.m_150109_().m_8020_(SceneScreen.inventoryIndex(hovered));
            if (carried.m_41619_() && !stack.m_41619_()) {
                g.m_280153_(this.f_96547_, stack, mx, my);
            }
            return;
        }
        List<Component> list2 = lines = station.choices() != null && carried.m_41619_() ? station.choices().tooltip(mx, my) : null;
        if (lines == null) {
            lines = station.tooltip(carried);
        }
        if (lines == null || lines.isEmpty()) {
            return;
        }
        ArrayList<FormattedCharSequence> seq = new ArrayList<FormattedCharSequence>(lines.size());
        for (Component line : lines) {
            if (SceneScreen.editor() != null && SceneScreen.editor().editing()) {
                seq.addAll(this.f_96547_.m_92923_((FormattedText)line, Math.min(210, this.f_96543_ / 2)));
                continue;
            }
            seq.add(line.m_7532_());
        }
        g.m_280245_(this.f_96547_, seq, mx, my);
    }

    private boolean guideShown() {
        return SceneScreen.working() && (SceneScreen.editor() == null || !SceneScreen.editor().visible());
    }

    @Nullable
    private ItemStack hoveredPanelItem() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        double[] mouse = Picking.mouseGui();
        if (player == null || Scene.station == null || !Scene.station.showsInventory() || !this.overPanel(mouse[0], mouse[1])) {
            return null;
        }
        int slot = this.slotAt(mouse[0], mouse[1]);
        return slot < 0 ? null : player.m_150109_().m_8020_(SceneScreen.inventoryIndex(slot));
    }

    private static int argb(float a, int r, int g, int b) {
        int alpha = Mth.m_14045_((int)Math.round(a * 255.0f), (int)0, (int)255);
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    void panelClick(int drawerSlot, int button, boolean shift) {
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (drawerSlot < 0 || player == null || mc.f_91072_ == null || Scene.station == null) {
            return;
        }
        int target = SceneScreen.menuSlot(player, drawerSlot);
        if (shift) {
            Scene.station.quickMove(SceneScreen.inventoryIndex(drawerSlot), target);
        } else if (target >= 0) {
            mc.f_91072_.m_171799_(player.f_36096_.f_38840_, target, button, ClickType.PICKUP, (Player)player);
        }
    }

    public boolean m_6375_(double mx, double my, int button) {
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (player == null || mc.f_91072_ == null || button != 0 && button != 1 || !SceneScreen.working() || FreeLook.looking()) {
            return true;
        }
        if (SceneScreen.editor() != null && SceneScreen.editor().click(mx, my, button)) {
            return true;
        }
        if (this.field != null) {
            if (!this.field.m_6375_(mx, my, button)) {
                this.closeField(true);
            }
            return true;
        }
        this.inventoryEffects.action(SceneScreen.m_96638_() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
        RecipeChoiceStrip choices = Scene.station.choices();
        if (choices != null) {
            int picked = choices.click(mx, my);
            if (picked >= 0) {
                Scene.station.chose(picked);
            }
            if (picked != -2) {
                return true;
            }
        }
        if (this.guideShown() && this.guide.click(mx, my, button)) {
            return true;
        }
        ClientStation clientStation = Scene.station;
        if (clientStation instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            if (table.recipeBook.click(mx, my, button)) {
                table.release(button);
                return true;
            }
        }
        if (Scene.station.showsInventory() && this.overPanel(mx, my)) {
            this.panelClick(this.slotAt(mx, my), button, SceneScreen.m_96638_());
            return true;
        }
        if (button == 1 && Scene.neighbor != null) {
            Scene.moveOver();
            return true;
        }
        Scene.station.click(button, SceneScreen.m_96638_());
        return true;
    }

    public boolean m_6050_(double mx, double my, double scrollY) {
        ClientStation clientStation;
        if (FreeLook.looking()) {
            FreeLook.scroll(scrollY);
            return true;
        }
        if (SceneScreen.working() && SceneScreen.editor() != null && SceneScreen.editor().scroll(mx, my, scrollY)) {
            return true;
        }
        if (SceneScreen.working() && this.guideShown() && this.guide.scroll(mx, my, scrollY)) {
            return true;
        }
        if (SceneScreen.working() && (clientStation = Scene.station) instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            if (table.recipeBook.scroll(mx, my, scrollY)) {
                return true;
            }
        }
        if (!(!SceneScreen.working() || Scene.station.showsInventory() && this.overPanel(mx, my))) {
            Scene.station.scroll(scrollY);
        }
        return true;
    }

    public boolean m_5534_(char c, int modifiers) {
        ClientStation clientStation;
        if (SceneScreen.working() && SceneScreen.editor() != null && SceneScreen.editor().editing()) {
            return true;
        }
        if (SceneScreen.working() && this.guideShown() && this.guide.charTyped(c, modifiers)) {
            return true;
        }
        if (SceneScreen.working() && (clientStation = Scene.station) instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            if (table.recipeBook.charTyped(c, modifiers)) {
                return true;
            }
        }
        return this.field != null ? this.field.m_5534_(c, modifiers) : super.m_5534_(c, modifiers);
    }

    public boolean m_7979_(double mx, double my, int button, double dx, double dy) {
        ClientStation clientStation;
        if (FreeLook.looking()) {
            return true;
        }
        if (SceneScreen.working() && SceneScreen.editor() != null && SceneScreen.editor().drag(mx, my, button)) {
            return true;
        }
        if (SceneScreen.working() && this.guideShown() && this.guide.over(mx, my)) {
            ClientStation clientStation2;
            if (button == 1 && (clientStation2 = Scene.station) instanceof TableStation) {
                TableStation table = (TableStation)clientStation2;
                table.release(button);
            }
            return true;
        }
        if (SceneScreen.working() && (clientStation = Scene.station) instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            if (table.recipeBook.over(mx, my)) {
                if (button == 1) {
                    table.release(button);
                }
                return true;
            }
        }
        if (SceneScreen.working()) {
            this.inventoryEffects.action(SceneScreen.m_96638_() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            Scene.station.drag(button);
        }
        return true;
    }

    public boolean m_6348_(double mx, double my, int button) {
        if (SceneScreen.editor() != null) {
            SceneScreen.editor().release();
        }
        if (Scene.station != null) {
            Scene.station.release(button);
        }
        return true;
    }

    public boolean m_7933_(int key, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        Options options = mc.f_91066_;
        if (!SceneScreen.working()) {
            return true;
        }
        if (SceneScreen.editor() != null && SceneScreen.editor().editing()) {
            SceneScreen.editor().keyPressed(key);
            return true;
        }
        if (this.field == null && FreeLook.keyPressed(key, scanCode)) {
            return true;
        }
        if (FreeLook.looking()) {
            if (key == 256 || options.f_92092_.m_90832_(key, scanCode) || options.f_92085_.m_90832_(key, scanCode) || options.f_92087_.m_90832_(key, scanCode) || options.f_92086_.m_90832_(key, scanCode) || options.f_92088_.m_90832_(key, scanCode) || options.f_92089_.m_90832_(key, scanCode)) {
                FreeLook.stop();
            }
            return true;
        }
        if (this.field != null) {
            if (key == 256) {
                this.closeField(false);
            } else if (key == 257 || key == 335) {
                this.closeField(true);
            } else {
                this.field.m_7933_(key, scanCode, modifiers);
            }
            return true;
        }
        this.inventoryEffects.action(SceneScreen.m_96638_() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
        if (this.guideShown() && this.guide.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        ClientStation clientStation = Scene.station;
        if (clientStation instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            if (table.recipeBook.keyPressed(key, scanCode, modifiers)) {
                return true;
            }
        }
        if (this.guideShown() && this.guide.viewerKey(key, scanCode, this.hoveredPanelItem())) {
            return true;
        }
        if (SceneScreen.editor() != null && SceneScreen.editor().keyPressed(key)) {
            return true;
        }
        if (Scene.station.keyPressed(key)) {
            return true;
        }
        if (key == 256 || options.f_92092_.m_90832_(key, scanCode) || options.f_92085_.m_90832_(key, scanCode) || options.f_92087_.m_90832_(key, scanCode) || options.f_92086_.m_90832_(key, scanCode) || options.f_92088_.m_90832_(key, scanCode) || options.f_92089_.m_90832_(key, scanCode)) {
            Scene.leave();
            return true;
        }
        if (player != null && mc.f_91072_ != null) {
            int slot;
            double[] mouse = Picking.mouseGui();
            int n = slot = Scene.station.showsInventory() && this.overPanel(mouse[0], mouse[1]) ? SceneScreen.menuSlot(player, this.slotAt(mouse[0], mouse[1])) : -1;
            if (slot >= 0) {
                for (int i = 0; i < 9; ++i) {
                    if (!options.f_92056_[i].m_90832_(key, scanCode)) continue;
                    this.inventoryEffects.action(ClickType.SWAP);
                    mc.f_91072_.m_171799_(player.f_36096_.f_38840_, slot, i, ClickType.SWAP, (Player)player);
                    return true;
                }
                if (options.f_92094_.m_90832_(key, scanCode)) {
                    this.inventoryEffects.action(ClickType.THROW);
                    mc.f_91072_.m_171799_(player.f_36096_.f_38840_, slot, SceneScreen.m_96637_() ? 1 : 0, ClickType.THROW, (Player)player);
                    return true;
                }
            }
        }
        return super.m_7933_(key, scanCode, modifiers);
    }

    public boolean m_7920_(int key, int scanCode, int modifiers) {
        if (FreeLook.keyReleased(key, scanCode)) {
            return true;
        }
        return super.m_7920_(key, scanCode, modifiers);
    }

    public void m_7379_() {
        Scene.leave();
    }

    private /* synthetic */ void lambda$render$0(GuiGraphics graphics, ItemStack carried, Integer x, Integer y) {
        graphics.m_280480_(carried, x.intValue(), y.intValue());
        graphics.m_280370_(this.f_96547_, carried, x.intValue(), y.intValue());
    }
}

