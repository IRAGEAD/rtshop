package com.rtshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 퀘스트 편집기에서 아이템이나 몬스터를 고르는 화면.
 *  - 아이템: 모든 아이템을 아이콘 격자로 보여 주고 이름·ID·@모드이름 으로 검색.
 *  - 몬스터: 생물 목록 (스폰 알 아이콘 + 이름). 맨 위에 '모든 몬스터'.
 * 고르면 편집기로 돌아가 그 칸에 들어간다. Esc / 취소 는 바꾸지 않고 돌아간다.
 */
public class PickerScreen extends Screen {
    public enum Mode {
        ITEM, ENTITY
    }

    private record Option(String id, String name, String search, ItemStack icon, boolean monster) {
    }

    private static final int CELL = 18;
    private static final int ROW = 16;
    private static List<Option> itemCache;
    private static List<Option> entityCache;

    public final Screen parent;
    private final Mode mode;
    private final Consumer<String> onPick;
    private final List<Option> all;
    private List<Option> shown = new ArrayList<>();
    private EditBox search;
    private boolean monstersOnly = true;
    private int scroll = 0;
    private int fx, fy, W, H;

    public PickerScreen(Screen parent, Mode mode, Consumer<String> onPick) {
        super(Component.translatable(mode == Mode.ITEM ? "rtshop.pick.title_item" : "rtshop.pick.title_entity"));
        this.parent = parent;
        this.mode = mode;
        this.onPick = onPick;
        this.all = mode == Mode.ITEM ? items() : entities();
    }

    // ------------------------------------------------------------------ 목록 만들기 (처음 한 번)

    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    private static List<Option> items() {
        if (itemCache == null) {
            List<Option> out = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS) {
                if (item == Items.AIR) {
                    continue;
                }
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id == null) {
                    continue;
                }
                ItemStack s = new ItemStack(item);
                String name;
                try {
                    name = s.getHoverName().getString();
                } catch (Throwable t) {
                    name = id.toString();
                }
                out.add(new Option(id.toString(), name, lower(name + " " + id + " @" + id.getNamespace()), s, false));
            }
            itemCache = out;
        }
        return itemCache;
    }

    public static ItemStack eggFor(EntityType<?> type) {
        try {
            SpawnEggItem egg = ForgeSpawnEggItem.fromEntityType(type);
            return egg == null ? ItemStack.EMPTY : new ItemStack(egg);
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private static List<Option> entities() {
        if (entityCache == null) {
            List<Option> out = new ArrayList<>();
            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
                ItemStack egg = eggFor(type);
                boolean living = type.getCategory() != MobCategory.MISC || !egg.isEmpty();
                if (!living) {
                    continue;
                }
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
                if (id == null) {
                    continue;
                }
                String name = type.getDescription().getString();
                boolean monster = type.getCategory() == MobCategory.MONSTER;
                out.add(new Option(id.toString(), name, lower(name + " " + id + " @" + id.getNamespace()), egg, monster));
            }
            out.sort(Comparator.comparing((Option o) -> !o.monster()).thenComparing(Option::name));
            entityCache = out;
        }
        return entityCache;
    }

    // ------------------------------------------------------------------ 배치

    @Override
    protected void init() {
        W = Math.min(this.width - 8, 360);
        H = Math.min(this.height - 8, 250);
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        String old = search == null ? "" : search.getValue();
        int sw = W - 16 - (mode == Mode.ENTITY ? 78 : 0);
        search = new EditBox(this.font, fx + 8, fy + 28, sw, 14, Component.empty());
        search.setMaxLength(100);
        search.setValue(old);
        search.setResponder(v -> {
            scroll = 0;
            filter();
        });
        this.addRenderableWidget(search);
        this.setInitialFocus(search);
        filter();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void filter() {
        String q = lower(search == null ? "" : search.getValue().trim());
        List<Option> out = new ArrayList<>();
        if (mode == Mode.ENTITY && q.isEmpty()) {
            out.add(new Option("*", Component.translatable("rtshop.pick.any_monster").getString(), "", new ItemStack(Items.ZOMBIE_HEAD), true));
        }
        String[] words = q.isEmpty() ? new String[0] : q.split("\\s+");
        for (Option o : all) {
            if (mode == Mode.ENTITY && monstersOnly && !o.monster()) {
                continue;
            }
            boolean ok = true;
            for (String w : words) {
                if (!o.search().contains(w)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                out.add(o);
            }
        }
        shown = out;
    }

    private int gridTop() {
        return fy + 48;
    }

    private int gridH() {
        return H - 48 - 26;
    }

    private int cols() {
        return Math.max(1, (W - 20) / CELL);
    }

    private int visibleRows() {
        return Math.max(1, gridH() / (mode == Mode.ITEM ? CELL : ROW));
    }

    private int totalRows() {
        return mode == Mode.ITEM ? (shown.size() + cols() - 1) / cols() : shown.size();
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - visibleRows());
    }

    private int bottomY() {
        return fy + H - 22;
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        g.fill(fx + 5, fy + 4, fx + W - 5, fy + 23, Ui.C_WOOD_DARK);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        g.drawString(this.font, this.title, fx + 10, fy + 9, Ui.C_GOLD, true);
        String hint = Ui.clip(this.font, Component.translatable("rtshop.pick.hint").getString(), W - 30 - this.font.width(this.title));
        g.drawString(this.font, hint, fx + W - 10 - this.font.width(hint), fy + 9, Ui.C_PARCH_SHADE, false);
        if (mode == Mode.ENTITY) {
            Ui.button(g, this.font, fx + W - 8 - 74, fy + 27, 74, 16,
                    Component.translatable(monstersOnly ? "rtshop.pick.monsters_only" : "rtshop.pick.all_mobs").getString(),
                    Ui.inside(mx, my, fx + W - 82, fy + 27, 74, 16), true);
        }
        int gx = fx + 8;
        int gy = gridTop();
        int gw = W - 16;
        int gh = gridH();
        Ui.bevel(g, gx, gy, gw, gh, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        Option hover = null;
        if (mode == Mode.ITEM) {
            int cols = cols();
            int ox = gx + (gw - 4 - cols * CELL) / 2;
            for (int r = 0; r < visibleRows(); r++) {
                for (int c = 0; c < cols; c++) {
                    int i = (scroll + r) * cols + c;
                    if (i >= shown.size()) {
                        break;
                    }
                    int x = ox + c * CELL;
                    int y = gy + 2 + r * CELL;
                    boolean h = Ui.inside(mx, my, x, y, CELL, CELL);
                    g.fill(x, y, x + CELL - 1, y + CELL - 1, h ? 0x55A87A22 : 0x18000000);
                    g.renderItem(shown.get(i).icon(), x + 1, y + 1);
                    if (h) {
                        hover = shown.get(i);
                    }
                }
            }
        } else {
            for (int r = 0; r < visibleRows(); r++) {
                int i = scroll + r;
                if (i >= shown.size()) {
                    break;
                }
                Option o = shown.get(i);
                int y = gy + 2 + r * ROW;
                boolean h = Ui.inside(mx, my, gx + 2, y, gw - 8, ROW);
                if (h) {
                    g.fill(gx + 2, y, gx + gw - 6, y + ROW, 0x55A87A22);
                    hover = o;
                }
                if (!o.icon().isEmpty()) {
                    g.renderItem(o.icon(), gx + 4, y);
                }
                g.drawString(this.font, Ui.clip(this.font, o.name(), gw / 2 - 10), gx + 24, y + 4, o.monster() ? Ui.C_RED : Ui.C_INK, false);
                String id = Ui.clip(this.font, o.id(), gw / 2 - 16);
                g.drawString(this.font, id, gx + gw - 10 - this.font.width(id), y + 4, Ui.C_INK_SOFT, false);
            }
        }
        if (maxScroll() > 0) {
            int barH = Math.max(10, gh * visibleRows() / Math.max(1, totalRows()));
            int barY = gy + (gh - barH) * scroll / maxScroll();
            g.fill(gx + gw - 4, barY, gx + gw - 2, barY + barH, Ui.C_WOOD_HI);
        }
        String count = Component.translatable("rtshop.pick.count", Ui.num(shown.size())).getString();
        g.drawString(this.font, count, fx + 10, bottomY() + 4, Ui.C_CREAM, false);
        if (shown.isEmpty() && search.getValue().contains(":")) {
            String raw = Ui.clip(this.font, Component.translatable("rtshop.pick.enter_raw", search.getValue()).getString(), W - 140);
            g.drawString(this.font, raw, fx + 10 + this.font.width(count) + 10, bottomY() + 4, Ui.C_GOLD, false);
        }
        Ui.button(g, this.font, fx + W - 8 - 60, bottomY(), 60, 16, Component.translatable("rtshop.qedit.cancel").getString(),
                Ui.inside(mx, my, fx + W - 68, bottomY(), 60, 16), true);
        super.render(g, mx, my, partial);
        if (hover != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(hover.name()));
            tip.add(Component.literal(hover.id()).withStyle(net.minecraft.ChatFormatting.GRAY));
            g.renderComponentTooltip(this.font, tip, mx, my);
        }
    }

    // ------------------------------------------------------------------ 입력

    private Option at(double mx, double my) {
        int gx = fx + 8;
        int gy = gridTop();
        int gw = W - 16;
        if (!Ui.inside(mx, my, gx, gy, gw, gridH())) {
            return null;
        }
        if (mode == Mode.ITEM) {
            int cols = cols();
            int ox = gx + (gw - 4 - cols * CELL) / 2;
            int c = (int) Math.floor((mx - ox) / CELL);
            int r = (int) Math.floor((my - gy - 2) / CELL);
            if (c < 0 || c >= cols || r < 0) {
                return null;
            }
            int i = (scroll + r) * cols + c;
            return i < shown.size() ? shown.get(i) : null;
        }
        int r = (int) Math.floor((my - gy - 2) / ROW);
        int i = scroll + r;
        return r >= 0 && i < shown.size() ? shown.get(i) : null;
    }

    private void pick(String id) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        onPick.accept(id);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (mode == Mode.ENTITY && Ui.inside(mx, my, fx + W - 82, fy + 27, 74, 16)) {
                monstersOnly = !monstersOnly;
                scroll = 0;
                filter();
                return true;
            }
            if (Ui.inside(mx, my, fx + W - 68, bottomY(), 60, 16)) {
                onClose();
                return true;
            }
            Option o = at(mx, my);
            if (o != null) {
                pick(o.id());
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * 2));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            String v = search.getValue().trim();
            if (shown.size() == 1) {
                pick(shown.get(0).id());
                return true;
            }
            // 목록에 없는 ID 나 #태그 를 직접 넣고 싶을 때
            if (shown.isEmpty() && v.contains(":")) {
                pick(v);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // ------------------------------------------------------------------ 점검용

    public void searchForTest(String text) {
        search.setValue(text);
    }

    public int shownForTest() {
        return shown.size();
    }

    public void pickFirstForTest() {
        if (!shown.isEmpty()) {
            pick(shown.get(0).id());
        }
    }
}
