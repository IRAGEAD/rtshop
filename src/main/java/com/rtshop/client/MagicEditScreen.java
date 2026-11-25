package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.MagicActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 마법 뽑기 편집기 (OP): 분야별 스킬북 목록(등급·가중치·빼기·더하기), 인챈트 재설정 목록, 확률·티켓 수·10회 보장.
 * 저장하면 config/rtshop/magic_gacha.json 에 쓰이고 바로 적용된다. [자동 채우기]는 이 세계의 스킬·인챈트로 목록을 다시 만든다.
 */
public class MagicEditScreen extends Screen {
    private static final String[] GRADES = MagicGachaScreen.GRADES;
    private static final String[] CATS = MagicGachaScreen.CATS;
    private JsonObject cfg;
    private JsonArray knownSkills = new JsonArray(), knownEnchants = new JsonArray();
    private int tab = 0; // 0~4 분야, 5 인챈트, 6 확률·비용
    private int scroll = 0;
    private boolean dirty;
    private int x0, y0, W, H;
    private final List<Object[]> hits = new ArrayList<>();
    private ChoicePopup popup;
    private Component status;
    private boolean statusOk;

    public MagicEditScreen(JsonObject o) {
        super(Component.translatable("rtshop.magic.editor"));
        update(o);
    }

    public void update(JsonObject o) {
        cfg = o.deepCopy();
        knownSkills = cfg.has("knownSkills") ? cfg.getAsJsonArray("knownSkills") : new JsonArray();
        knownEnchants = cfg.has("knownEnchants") ? cfg.getAsJsonArray("knownEnchants") : new JsonArray();
        cfg.remove("knownSkills");
        cfg.remove("knownEnchants");
        dirty = false;
        if (width > 0) rebuildWidgets();
    }

    public void showResult(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(k, a).getString();
    }

    private JsonArray list() {
        if (tab == 5) return cfg.getAsJsonArray("enchants");
        return cfg.getAsJsonObject("pools").getAsJsonArray(CATS[tab]);
    }

    private void changed() {
        dirty = true;
    }

    /** 점검용 */
    public int rowsForTest() {
        return tab <= 5 ? list().size() : 0;
    }

    public void tabForTest(int t) {
        tab = t;
        scroll = 0;
        if (width > 0) rebuildWidgets();
    }

    public void saveForTest() {
        save();
    }

    public void setGradeForTest(int row, String grade) {
        list().get(row).getAsJsonObject().addProperty("grade", grade);
        changed();
    }

    private void save() {
        JsonObject a = new JsonObject();
        a.addProperty("json", cfg.toString());
        ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.SAVE, a.toString()));
        dirty = false;
    }

    // ------------------------------------------------------------------ 화면

    @Override
    protected void init() {
        W = Math.min(width - 8, 460);
        H = Math.min(height - 8, 260);
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        if (tab == 6) initOdds();
        if (tab == 7) initExtra();
    }

    private void numberBox(int x, int y, int w, String value, java.util.function.Consumer<String> set) {
        EditBox b = new EditBox(font, x, y, w, 14, Component.empty());
        b.setValue(value);
        b.setFilter(s -> s.matches("[0-9]*\\.?[0-9]*"));
        b.setResponder(v -> {
            set.accept(v);
            changed();
        });
        addRenderableWidget(b);
    }

    private static String num(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private void initOdds() {
        JsonObject odds = cfg.getAsJsonObject("odds");
        String[] rows = {"select", "random", "enchant"};
        int y = y0 + 70;
        for (int r = 0; r < rows.length; r++) {
            JsonArray a = odds.getAsJsonArray(rows[r]);
            for (int i = 0; i < GRADES.length; i++) {
                final int k = i;
                if (rows[r].equals("enchant") && i == 5) continue; // 인챈트에는 야마토 등급 없음
                numberBox(x0 + 96 + i * 58, y + r * 22, 50, num(a.get(i).getAsDouble()), v -> {
                    try {
                        a.set(k, new com.google.gson.JsonPrimitive(v.isEmpty() ? 0 : Double.parseDouble(v)));
                    } catch (NumberFormatException ignored) {
                    }
                });
            }
        }
        JsonObject costs = cfg.getAsJsonObject("costs");
        String[] ck = {"select", "random", "reroll"};
        for (int i = 0; i < ck.length; i++) {
            final String key = ck[i];
            numberBox(x0 + 96 + i * 116, y0 + 160, 40, costs.get(key).getAsString(), v -> {
                try {
                    costs.addProperty(key, Math.max(1, Math.min(64, v.isEmpty() ? 1 : (int) Double.parseDouble(v))));
                } catch (NumberFormatException ignored) {
                }
            });
        }
    }

    private JsonObject section(String key) {
        if (!cfg.has(key)) cfg.add(key, new JsonObject());
        return cfg.getAsJsonObject(key);
    }

    private static boolean flag(JsonObject o, String k, boolean def) {
        return o.has(k) ? o.get(k).getAsBoolean() : def;
    }

    private static String val(JsonObject o, String k, String def) {
        return o.has(k) ? num(o.get(k).getAsDouble()) : def;
    }

    /** 천장 칸 */
    private void initExtra() {
        JsonObject pity = section("pity");
        numberBox(x0 + 130, y0 + 66, 44, val(pity, "count", "80"), v -> {
            try {
                pity.addProperty("count", Math.max(1, Math.min(100000, v.isEmpty() ? 1 : (int) Double.parseDouble(v))));
            } catch (NumberFormatException ignored) {
            }
        });
    }

    private void renderExtra(GuiGraphics g, int mx, int my, int px, int py, int pw) {
        JsonObject pity = section("pity");
        int ty = y0 + 48;
        for (var line : font.split(Component.translatable("rtshop.magic.edit_pity_head"), pw - 12)) {
            g.drawString(font, line, px + 6, ty, Ui.C_INK, false);
            ty += 9;
            if (ty > y0 + 60) break;
        }
        boolean pOn = flag(pity, "enabled", true);
        btn(g, mx, my, x0 + 16, y0 + 65, 60, 16, tr(pOn ? "rtshop.magic.edit_on" : "rtshop.magic.edit_off"), () -> {
            pity.addProperty("enabled", !pOn);
            changed();
        });
        g.drawString(font, tr("rtshop.magic.edit_pity_count"), x0 + 90, y0 + 69, Ui.C_INK, false);
        g.drawString(font, tr("rtshop.magic.edit_pity_grade"), x0 + 190, y0 + 69, Ui.C_INK, false);
        String pg = pity.has("grade") ? pity.get("grade").getAsString() : "legendary";
        int gx = x0 + 250;
        btn(g, mx, my, gx, y0 + 65, 90, 16, tr("rtshop.gacha.grade." + pg) + " ▼", () -> {
            List<ChoicePopup.Option> o = new ArrayList<>();
            for (int i = 2; i < 6; i++) o.add(new ChoicePopup.Option(GRADES[i], tr("rtshop.gacha.grade." + GRADES[i])));
            popup = new ChoicePopup(gx, y0 + 65, 90, 16, height, font, o, pg, v -> {
                pity.addProperty("grade", v);
                changed();
            });
        });
        boolean sh = flag(pity, "shared", true);
        btn(g, mx, my, x0 + 16, y0 + 86, 230, 16, tr("rtshop.magic.edit_pity_shared", tr(sh ? "rtshop.magic.edit_on" : "rtshop.magic.edit_off")), () -> {
            pity.addProperty("shared", !sh);
            changed();
        });
    }

    private void hit(int x, int y, int w, int h, Runnable r) {
        hits.add(new Object[]{x, y, w, h, r});
    }

    private void btn(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, Runnable r) {
        Ui.button(g, font, x, y, w, h, label, Ui.inside(mx, my, x, y, w, h) && popup == null, true);
        hit(x, y, w, h, r);
    }

    private ItemStack bookIcon(JsonObject e) {
        return MagicGachaScreen.stackOf(e);
    }

    private String entryName(JsonObject e) {
        if (tab == 5) {
            Enchantment en = ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(e.get("enchant").getAsString()));
            return en == null ? e.get("enchant").getAsString() : en.getFullname(e.get("level").getAsInt()).getString();
        }
        JsonObject x = e.deepCopy();
        String nbt = e.has("nbt") ? e.get("nbt").getAsString() : "";
        int i = nbt.indexOf("skill:\"");
        if (i >= 0) x.addProperty("skill", nbt.substring(i + 7, nbt.indexOf('"', i + 7)));
        return MagicGachaScreen.skillName(x, bookIcon(e));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        hits.clear();
        Ui.woodFrame(g, x0, y0, W, H);
        g.drawString(font, title, x0 + 10, y0 + 8, Ui.C_GOLD, false);
        if (dirty) g.drawString(font, tr("rtshop.enhedit.unsaved"), x0 + 14 + font.width(title), y0 + 8, 0xFFE0A070, false);
        // 탭
        String[] names = new String[8];
        for (int i = 0; i < 5; i++) names[i] = tr("rtshop.magic.cat." + CATS[i]);
        names[5] = tr("rtshop.magic.tab.reroll");
        names[6] = tr("rtshop.magic.edit_odds");
        names[7] = tr("rtshop.magic.edit_extra");
        int tw = (W - 20) / 8;
        for (int i = 0; i < 8; i++) {
            final int k = i;
            int bx = x0 + 10 + i * tw;
            boolean sel = i == tab;
            g.fill(bx, y0 + 22, bx + tw - 3, y0 + 36, sel ? 0xFF6A4AA8 : 0xFF3E2512);
            String s = Ui.clip(font, names[i], tw - 8);
            g.drawString(font, s, bx + (tw - 3 - font.width(s)) / 2, y0 + 25, sel ? 0xFFF4ECFF : Ui.C_CREAM, false);
            hit(bx, y0 + 22, tw - 3, 14, () -> {
                tab = k;
                scroll = 0;
                rebuildWidgets();
            });
        }
        int px = x0 + 10, py = y0 + 40, pw = W - 20, ph = H - 72;
        g.fill(px, py, px + pw, py + ph, Ui.C_PARCH);
        if (tab <= 5) renderList(g, mx, my, px, py, pw, ph);
        else if (tab == 6) renderOdds(g, mx, my, px, py, pw);
        else renderExtra(g, mx, my, px, py, pw);
        // 아래 단추
        int by = y0 + H - 28;
        btn(g, mx, my, x0 + 10, by, 60, 18, tr("rtshop.enhedit.save"), this::save);
        btn(g, mx, my, x0 + 74, by, 92, 18, tr("rtshop.magic.autofill"), () ->
                ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.AUTOFILL, "")));
        btn(g, mx, my, x0 + W - 70, by, 60, 18, tr("rtshop.enhodds.close"), this::onClose);
        if (status != null) g.drawString(font, Ui.clip(font, status.getString(), W - 260), x0 + 172, by + 5, statusOk ? 0xFF8FD67A : 0xFFE88070, false);
        super.render(g, mx, my, partial);
        if (popup != null) {
            g.flush();
            popup.render(g, font, mx, my);
        }
    }

    private int rowsFit(int ph) {
        return Math.max(1, (ph - 26) / 18);
    }

    private void renderList(GuiGraphics g, int mx, int my, int px, int py, int pw, int ph) {
        JsonArray l = list();
        int rows = rowsFit(ph);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, l.size() - rows)));
        for (int r = 0; r < rows && scroll + r < l.size(); r++) {
            final int idx = scroll + r;
            JsonObject e = l.get(idx).getAsJsonObject();
            int y = py + 4 + r * 18;
            if (r % 2 == 0) g.fill(px + 2, y - 1, px + pw - 2, y + 17, 0x14000000);
            g.renderItem(tab == 5 ? new ItemStack(Items.ENCHANTED_BOOK) : bookIcon(e), px + 4, y);
            g.drawString(font, Ui.clip(font, entryName(e), pw - 230), px + 24, y + 4, Ui.C_INK, false);
            String grade = e.get("grade").getAsString();
            int gx = px + pw - 200;
            g.fill(gx, y + 1, gx + 70, y + 15, 0xFFE0CDA4);
            g.drawString(font, Ui.clip(font, tr("rtshop.gacha.grade." + grade) + " ▼", 66), gx + 3, y + 4, GachaColors.dark(grade), false);
            hit(gx, y + 1, 70, 14, () -> {
                List<ChoicePopup.Option> o = new ArrayList<>();
                for (String gr : GRADES) {
                    if (gr.equals("common")) continue; // 일반 등급 자리는 '실패'
                    if (gr.equals("yamato") && (tab != 0)) continue; // 야마토는 공격 분야에만
                    o.add(new ChoicePopup.Option(gr, tr("rtshop.gacha.grade." + gr)));
                }
                popup = new ChoicePopup(gx, y + 1, 70, 14, height, font, o, grade, v -> {
                    e.addProperty("grade", v);
                    changed();
                });
            });
            // 가중치
            int wx = px + pw - 124;
            int w = e.get("weight").getAsInt();
            btn(g, mx, my, wx, y + 1, 14, 14, "-", () -> {
                e.addProperty("weight", Math.max(0, w - (hasShiftDown() ? 10 : 1)));
                changed();
            });
            String ws = String.valueOf(w);
            g.drawString(font, ws, wx + 30 - font.width(ws) / 2, y + 4, Ui.C_INK, false);
            btn(g, mx, my, wx + 46, y + 1, 14, 14, "+", () -> {
                e.addProperty("weight", Math.min(10000, w + (hasShiftDown() ? 10 : 1)));
                changed();
            });
            if (tab == 5) {
                int lv = e.get("level").getAsInt();
                btn(g, mx, my, wx + 64, y + 1, 22, 14, "Lv-", () -> {
                    e.addProperty("level", Math.max(1, lv - 1));
                    changed();
                });
                btn(g, mx, my, wx + 88, y + 1, 22, 14, "Lv+", () -> {
                    e.addProperty("level", Math.min(10, lv + 1));
                    changed();
                });
            }
            btn(g, mx, my, px + pw - 16, y + 1, 14, 14, "✕", () -> {
                l.remove(idx);
                changed();
            });
        }
        if (l.size() > rows) {
            String pg = (scroll + 1) + "-" + Math.min(l.size(), scroll + rows) + " / " + l.size();
            g.drawString(font, pg, px + pw - 6 - font.width(pg), py + ph - 12, Ui.C_INK_SOFT, false);
        }
        if (l.isEmpty()) g.drawString(font, tr("rtshop.magic.edit_empty"), px + 8, py + 8, Ui.C_INK_SOFT, false);
        // 더하기
        btn(g, mx, my, px + 4, py + ph - 20, 110, 16, tab == 5 ? tr("rtshop.magic.add_enchant") : tr("rtshop.magic.add_skill"), () -> {
            List<ChoicePopup.Option> o = new ArrayList<>();
            if (tab == 5) {
                for (JsonElement k : knownEnchants) {
                    JsonObject x = k.getAsJsonObject();
                    Enchantment en = ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(x.get("id").getAsString()));
                    o.add(new ChoicePopup.Option(x.get("id").getAsString(), en == null ? x.get("id").getAsString() : en.getFullname(1).getString(),
                            new ItemStack(Items.ENCHANTED_BOOK)));
                }
            } else {
                for (JsonElement k : knownSkills) {
                    JsonObject x = k.getAsJsonObject();
                    String id = x.get("id").getAsString();
                    JsonObject probe = new JsonObject();
                    probe.addProperty("item", "epicfight:skillbook");
                    probe.addProperty("nbt", "{skill:\"" + id + "\"}");
                    probe.addProperty("skill", id);
                    ItemStack icon = MagicGachaScreen.stackOf(probe);
                    o.add(new ChoicePopup.Option(id, MagicGachaScreen.skillName(probe, icon) + " (" + tr("rtshop.magic.cat." + x.get("cat").getAsString()) + ")", icon));
                }
            }
            if (o.isEmpty()) return;
            popup = new ChoicePopup(px + 4, py + ph - 20, 200, 16, height, font, o, "", v -> {
                JsonObject n = new JsonObject();
                if (tab == 5) {
                    n.addProperty("enchant", v);
                    n.addProperty("level", 1);
                    n.addProperty("grade", "uncommon");
                } else {
                    n.addProperty("item", "epicfight:skillbook");
                    n.addProperty("count", 1);
                    n.addProperty("nbt", "{skill:\"" + v + "\"}");
                    String gr = "rare";
                    for (JsonElement k : knownSkills) if (k.getAsJsonObject().get("id").getAsString().equals(v)) gr = k.getAsJsonObject().get("grade").getAsString();
                    if (gr.equals("yamato") && tab != 0) gr = "legendary";
                    n.addProperty("grade", gr);
                }
                n.addProperty("weight", 10);
                list().add(n);
                changed();
            });
        });
    }

    private void renderOdds(GuiGraphics g, int mx, int my, int px, int py, int pw) {
        g.drawString(font, tr("rtshop.magic.edit_odds_head"), px + 6, py + 6, Ui.C_INK, false);
        for (int i = 0; i < GRADES.length; i++) {
            String s = i == 0 ? tr("rtshop.magic.fail") : tr("rtshop.gacha.grade." + GRADES[i]); // 첫 칸 = 실패 확률
            g.drawString(font, Ui.clip(font, s, 54), x0 + 96 + i * 58, y0 + 58, GachaColors.dark(GRADES[i]), false);
        }
        String[] rows = {tr("rtshop.magic.tab.select"), tr("rtshop.magic.tab.random"), tr("rtshop.magic.tab.reroll")};
        for (int r = 0; r < 3; r++) g.drawString(font, Ui.clip(font, rows[r], 80), px + 6, y0 + 73 + r * 22, Ui.C_INK, false);
        g.drawString(font, tr("rtshop.magic.edit_costs"), px + 6, y0 + 146, Ui.C_INK, false);
        String[] ck = {tr("rtshop.magic.tab.select"), tr("rtshop.magic.tab.random"), tr("rtshop.magic.tab.reroll")};
        for (int i = 0; i < 3; i++) g.drawString(font, Ui.clip(font, ck[i], 70), x0 + 96 + i * 116 + 44, y0 + 163, Ui.C_INK_SOFT, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (popup != null) {
            if (popup.click(mx, my)) popup = null;
            return true;
        }
        for (Object[] h : new ArrayList<>(hits)) {
            if (Ui.inside(mx, my, (int) h[0], (int) h[1], (int) h[2], (int) h[3])) {
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
                ((Runnable) h[4]).run();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (popup != null) {
            popup.scroll(delta);
            return true;
        }
        scroll = Math.max(0, scroll - (int) Math.signum(delta) * 3);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
