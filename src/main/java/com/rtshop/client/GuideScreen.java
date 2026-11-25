package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 조사대 안내서(가이드북): 왼쪽에서 갈래와 쪽을 고르면 오른쪽에 그림과 글이 나온다. 위의 칸에 낱말을 치면 그 낱말이 든 쪽만 남는다.
 * 설치되지 않은 모드의 쪽은 보이지 않는다.
 */
public class GuideScreen extends Screen {
    private static final int W = 396, H = 236, LIST_W = 132, ROW_H = 12;
    private static final int C_TEXT = 0xFF4A3520, C_HEAD = 0xFF7A3B1E;

    private static final class Page {
        String category = "", title = "", text = "", keys = "", image = "";
        int iw, ih;
        boolean header;
        final List<ItemStack> icons = new ArrayList<>();
        String lower = "";
    }

    private final List<Page> all = new ArrayList<>(), rows = new ArrayList<>();
    private Page sel;
    private int listScroll, textScroll, contentH, fx, fy, pages;
    private EditBox search;
    private String query = "";

    public GuideScreen(JsonObject o) {
        super(Component.translatable("rtshop.guide.title"));
        if (!o.has("categories")) return;
        for (JsonElement ce : o.getAsJsonArray("categories")) {
            JsonObject c = ce.getAsJsonObject();
            String cat = str(c, "name");
            List<Page> list = new ArrayList<>();
            for (JsonElement pe : c.getAsJsonArray("pages")) {
                JsonObject j = pe.getAsJsonObject();
                if (!modOk(str(j, "mod"))) continue;
                Page p = new Page();
                p.category = cat;
                p.title = str(j, "title");
                p.text = str(j, "text");
                p.keys = str(j, "keys");
                p.image = str(j, "image");
                p.iw = j.has("iw") ? j.get("iw").getAsInt() : 480;
                p.ih = j.has("ih") ? j.get("ih").getAsInt() : 270;
                if (j.has("icons")) {
                    for (JsonElement ie : j.getAsJsonArray("icons")) {
                        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(ie.getAsString()));
                        if (item != null && item != Items.AIR) p.icons.add(new ItemStack(item));
                    }
                }
                p.lower = (p.title + " " + p.keys + " " + p.text).toLowerCase(Locale.ROOT);
                list.add(p);
            }
            if (list.isEmpty()) continue;
            Page h = new Page();
            h.header = true;
            h.category = cat;
            h.title = cat;
            all.add(h);
            all.addAll(list);
            pages += list.size();
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    /** 쪽에 적힌 모드 가운데 하나라도 있으면 보인다 (비어 있으면 늘 보인다). */
    static boolean modOk(String mods) {
        if (mods == null || mods.isBlank()) return true;
        for (String m : mods.split(",")) if (ModList.get().isLoaded(m.trim())) return true;
        return false;
    }

    @Override
    protected void init() {
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        search = new EditBox(font, fx + 10, fy + 29, LIST_W - 4, 12, Component.translatable("rtshop.guide.search"));
        search.setMaxLength(40);
        search.setValue(query);
        search.setHint(Component.translatable("rtshop.guide.search"));
        search.setResponder(s -> {
            query = s;
            refilter();
        });
        addRenderableWidget(search);
        refilter();
    }

    private void refilter() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        rows.clear();
        if (q.isEmpty()) {
            rows.addAll(all);
        } else {
            Page head = null;
            for (Page p : all) {
                if (p.header) {
                    head = p;
                    continue;
                }
                if (!p.lower.contains(q)) continue;
                if (head != null) {
                    rows.add(head);
                    head = null;
                }
                rows.add(p);
            }
        }
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, rows.size() - visible())));
        if (sel == null || !rows.contains(sel)) {
            sel = null;
            for (Page p : rows) {
                if (!p.header) {
                    select(p);
                    break;
                }
            }
        }
    }

    private void select(Page p) {
        sel = p;
        textScroll = 0;
        contentH = 0;
    }

    private int visible() {
        return (H - 54) / ROW_H;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = title.getString();
        g.drawString(font, t, fx + (W - font.width(t)) / 2, fy + 9, Ui.C_GOLD, true);
        int lx = fx + 8, ly = fy + 45, lh = H - 53;
        Ui.bevel(g, lx, ly, LIST_W, lh, Ui.C_WOOD_DARK, Ui.C_WOOD, Ui.C_SHADOW);
        for (int i = 0; i < visible() && i + listScroll < rows.size(); i++) {
            Page p = rows.get(i + listScroll);
            int ry = ly + 3 + i * ROW_H;
            if (p.header) {
                g.drawString(font, Ui.clip(font, p.title, LIST_W - 8), lx + 4, ry + 2, Ui.C_GOLD, false);
                continue;
            }
            boolean on = p == sel, hov = Ui.inside(mx, my, lx + 2, ry, LIST_W - 4, ROW_H);
            if (on || hov) g.fill(lx + 2, ry, lx + LIST_W - 2, ry + ROW_H - 1, on ? 0x80E8B84A : 0x40E8B84A);
            g.drawString(font, Ui.clip(font, p.title, LIST_W - 16), lx + 10, ry + 2, on ? 0xFFFFF2C8 : Ui.C_CREAM, false);
        }
        if (rows.size() > visible()) {
            int bh = Math.max(12, (lh - 6) * visible() / rows.size()), by = ly + 3 + (lh - 6 - bh) * listScroll / Math.max(1, rows.size() - visible());
            g.fill(lx + LIST_W - 3, by, lx + LIST_W - 1, by + bh, Ui.C_GOLD_DARK);
        }
        int dx = lx + LIST_W + 6, dy = fy + 28, dw = W - LIST_W - 22, dh = H - 36;
        Ui.bevel(g, dx, dy, dw, dh, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        if (sel == null) {
            g.drawString(font, Component.translatable("rtshop.guide.none").getString(), dx + 8, dy + 8, Ui.C_INK_SOFT, false);
            super.render(g, mx, my, partial);
            return;
        }
        g.enableScissor(dx + 2, dy + 2, dx + dw - 2, dy + dh - 2);
        int y = dy + 7 - textScroll;
        g.drawString(font, sel.title, dx + 8, y, C_HEAD, false);
        Ui.scaledText(g, font, sel.category, dx + dw - 10 - font.width(sel.category) * 0.8F, y + 1, 0.8F, Ui.C_INK_SOFT, false);
        y += 12;
        g.fill(dx + 6, y, dx + dw - 6, y + 1, Ui.C_PARCH_SHADE);
        y += 5;
        if (!sel.image.isEmpty()) {
            ResourceLocation tex = ResourceLocation.tryParse(sel.image);
            if (tex != null && sel.iw > 0 && sel.ih > 0) {
                int w = dw - 16, h = w * sel.ih / sel.iw;
                g.fill(dx + 7, y - 1, dx + 9 + w, y + h + 1, Ui.C_INK_SOFT);
                g.blit(tex, dx + 8, y, w, h, 0F, 0F, sel.iw, sel.ih, sel.iw, sel.ih);
                y += h + 6;
            }
        }
        ItemStack hover = ItemStack.EMPTY;
        if (!sel.icons.isEmpty()) {
            for (int i = 0; i < sel.icons.size(); i++) {
                int ix = dx + 8 + i * 20;
                if (ix + 18 > dx + dw - 6) break;
                g.fill(ix - 1, y - 1, ix + 17, y + 17, Ui.C_PARCH_DARK);
                g.renderItem(sel.icons.get(i), ix, y);
                if (Ui.inside(mx, my, ix, y, 17, 17) && my > dy && my < dy + dh) hover = sel.icons.get(i);
            }
            y += 22;
        }
        for (String para : sel.text.split("\n", -1)) {
            if (para.isEmpty()) {
                y += 5;
                continue;
            }
            boolean bullet = para.startsWith("· ");
            for (FormattedCharSequence s : font.split(Component.literal(para), dw - 18)) {
                g.drawString(font, s, dx + 8, y, bullet ? C_TEXT : C_TEXT, false);
                y += 10;
            }
            y += 2;
        }
        contentH = y + textScroll - dy;
        g.disableScissor();
        if (contentH > dh) {
            int bh = Math.max(12, (dh - 6) * dh / contentH), by = dy + 3 + (dh - 6 - bh) * textScroll / Math.max(1, contentH - dh + 8);
            g.fill(dx + dw - 4, by, dx + dw - 2, Math.min(dy + dh - 3, by + bh), Ui.C_PARCH_SHADE);
        }
        super.render(g, mx, my, partial);
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = fx + 8, ly = fy + 45;
        if (button == 0 && Ui.inside(mx, my, lx, ly, LIST_W, H - 53)) {
            int i = (int) ((my - ly - 3) / ROW_H) + listScroll;
            if (i >= 0 && i < rows.size() && !rows.get(i).header && rows.get(i) != sel) {
                select(rows.get(i));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1F));
            }
            search.setFocused(false);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < fx + 8 + LIST_W + 3) {
            listScroll = Math.max(0, Math.min(Math.max(0, rows.size() - visible()), listScroll - (int) Math.signum(delta) * 2));
        } else {
            int max = Math.max(0, contentH - (H - 36) + 8);
            textScroll = Math.max(0, Math.min(max, textScroll - (int) Math.signum(delta) * 14));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (search.isFocused() && key != 256) {
            search.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ---- 시험용
    public int pagesForTest() {
        return pages;
    }

    public int rowsForTest() {
        int n = 0;
        for (Page p : rows) if (!p.header) n++;
        return n;
    }

    public void searchForTest(String q) {
        search.setValue(q);
    }

    public String titleForTest() {
        return sel == null ? "" : sel.title;
    }

    public boolean selectForTest(String title) {
        for (Page p : rows) {
            if (!p.header && p.title.equals(title)) {
                select(p);
                return true;
            }
        }
        return false;
    }

    /** 쪽 제목이 모두 목록 칸 안에 들어가는가. */
    public boolean fitsForTest() {
        for (Page p : all) if (!p.header && font.width(p.title) > LIST_W - 16) return false;
        return true;
    }

    public void scrollTextForTest(int by) {
        textScroll = Math.max(0, by);
    }
}
