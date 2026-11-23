package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 조합법 만들기 (3×3 조합대 모양). 칸을 누르면 아이템 고르기, 우클릭하면 비우기.
 * 모양 그대로(shaped) 또는 모양 상관없이(shapeless). 결과 아이템과 개수를 정한다.
 */
public class RecipeMakeScreen extends Screen {
    final RecipeEditScreen parent;
    private final String editId;
    private final String[] grid = new String[9];
    private String result = "";
    private int count = 1;
    private boolean shaped = true;
    private boolean blockOriginal = true;
    private String id;
    private String error = "";
    private int fx, fy, W, H;

    public RecipeMakeScreen(RecipeEditScreen parent, String editId, JsonObject recipe) {
        super(Component.translatable("rtshop.rules.make_title"));
        this.parent = parent;
        this.editId = editId;
        for (int i = 0; i < 9; i++) grid[i] = "";
        this.id = editId == null ? "" : editId;
        if (recipe != null) load(recipe);
    }

    private void load(JsonObject r) {
        String type = r.has("type") ? r.get("type").getAsString() : "";
        try {
            if (type.endsWith("crafting_shaped")) {
                shaped = true;
                JsonArray pat = r.getAsJsonArray("pattern");
                JsonObject key = r.getAsJsonObject("key");
                for (int row = 0; row < Math.min(3, pat.size()); row++) {
                    String line = pat.get(row).getAsString();
                    for (int col = 0; col < Math.min(3, line.length()); col++) {
                        String k = String.valueOf(line.charAt(col));
                        if (key.has(k)) grid[row * 3 + col] = ingredientItem(key.get(k));
                    }
                }
            } else if (type.endsWith("crafting_shapeless")) {
                shaped = false;
                JsonArray ing = r.getAsJsonArray("ingredients");
                for (int i = 0; i < Math.min(9, ing.size()); i++) grid[i] = ingredientItem(ing.get(i));
            }
            JsonObject res = r.getAsJsonObject("result");
            result = res.get("item").getAsString();
            count = res.has("count") ? res.get("count").getAsInt() : 1;
        } catch (Exception e) {
            error = Component.translatable("rtshop.rules.make_unsupported").getString();
        }
        blockOriginal = false;
    }

    private static String ingredientItem(JsonElement e) {
        if (e.isJsonArray()) e = e.getAsJsonArray().get(0);
        JsonObject o = e.getAsJsonObject();
        return o.has("item") ? o.get("item").getAsString() : "";
    }

    JsonObject build() {
        JsonObject r = new JsonObject();
        if (shaped) {
            int minR = 3, maxR = -1, minC = 3, maxC = -1;
            for (int i = 0; i < 9; i++) {
                if (!grid[i].isEmpty()) {
                    minR = Math.min(minR, i / 3);
                    maxR = Math.max(maxR, i / 3);
                    minC = Math.min(minC, i % 3);
                    maxC = Math.max(maxC, i % 3);
                }
            }
            if (maxR < 0) return null;
            Map<String, Character> keys = new LinkedHashMap<>();
            JsonArray pat = new JsonArray();
            for (int row = minR; row <= maxR; row++) {
                StringBuilder sb = new StringBuilder();
                for (int col = minC; col <= maxC; col++) {
                    String it = grid[row * 3 + col];
                    if (it.isEmpty()) {
                        sb.append(' ');
                    } else {
                        Character c = keys.get(it);
                        if (c == null) {
                            c = (char) ('A' + keys.size());
                            keys.put(it, c);
                        }
                        sb.append(c);
                    }
                }
                pat.add(sb.toString());
            }
            r.addProperty("type", "minecraft:crafting_shaped");
            r.add("pattern", pat);
            JsonObject key = new JsonObject();
            for (Map.Entry<String, Character> e : keys.entrySet()) {
                JsonObject ing = new JsonObject();
                ing.addProperty("item", e.getKey());
                key.add(String.valueOf(e.getValue()), ing);
            }
            r.add("key", key);
        } else {
            JsonArray ing = new JsonArray();
            for (String it : grid) {
                if (it.isEmpty()) continue;
                JsonObject o = new JsonObject();
                o.addProperty("item", it);
                ing.add(o);
            }
            if (ing.isEmpty()) return null;
            r.addProperty("type", "minecraft:crafting_shapeless");
            r.add("ingredients", ing);
        }
        JsonObject res = new JsonObject();
        res.addProperty("item", result);
        res.addProperty("count", count);
        r.add("result", res);
        return r;
    }

    private String autoId() {
        String base = result.isEmpty() ? "recipe" : result.replace(':', '_');
        String cand = "rtshop:custom/" + base;
        int n = 2;
        while (parent.custom.containsKey(cand) && !cand.equals(editId)) cand = "rtshop:custom/" + base + "_" + n++;
        return cand;
    }

    @Override
    protected void init() {
        W = Math.min(300, width - 10);
        H = Math.min(214, height - 10);
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        int gx = fx + 20, gy = fy + 40;
        for (int i = 0; i < 9; i++) {
            int k = i;
            addRenderableWidget(new SlotButton(gx + (i % 3) * 22, gy + (i / 3) * 22, () -> grid[k], v -> grid[k] = v));
        }
        addRenderableWidget(new SlotButton(fx + 140, gy + 22, () -> result, v -> {
            result = v;
            if (editId == null) id = "";
        }));
        EditBox cnt = new EditBox(font, fx + 166, gy + 26, 30, 14, Component.empty());
        cnt.setValue(Integer.toString(count));
        cnt.setFilter(s -> s.isEmpty() || s.matches("[0-9]{1,2}"));
        cnt.setResponder(v -> count = v.isEmpty() ? 1 : Math.max(1, Math.min(64, Integer.parseInt(v))));
        addRenderableWidget(cnt);
        addRenderableWidget(MButton.builder(Component.translatable(shaped ? "rtshop.rules.shaped" : "rtshop.rules.shapeless"), b -> {
            shaped = !shaped;
            rebuildWidgets();
        }).bounds(fx + 20, gy + 72, 120, 16).build());
        if (editId == null) {
            addRenderableWidget(MButton.builder(Component.translatable(blockOriginal ? "rtshop.rules.block_orig_on" : "rtshop.rules.block_orig_off"), b -> {
                blockOriginal = !blockOriginal;
                rebuildWidgets();
            }).bounds(fx + 20, gy + 92, W - 40, 16).build());
        }
        int by = fy + H - 26;
        addRenderableWidget(MButton.builder(Component.translatable("rtshop.rules.make_ok"), b -> done()).bounds(fx + W - 172, by, 80, 18).build());
        addRenderableWidget(MButton.builder(Component.translatable("gui.cancel"), b -> minecraft.setScreen(parent)).bounds(fx + W - 88, by, 76, 18).build());
    }

    private void done() {
        if (result.isEmpty()) {
            error = Component.translatable("rtshop.rules.make_need_result").getString();
            return;
        }
        JsonObject r = build();
        if (r == null) {
            error = Component.translatable("rtshop.rules.make_need_items").getString();
            return;
        }
        String rid = editId != null ? editId : autoId();
        parent.putCustom(rid, r, editId == null && blockOriginal, result);
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.bevel(g, fx + 6, fy + 24, W - 12, H - 54, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        g.drawString(font, title, fx + 10, fy + 8, Ui.C_GOLD, true);
        g.drawString(font, "→", fx + 100, fy + 40 + 27, Ui.C_INK, false);
        g.drawString(font, Component.translatable("rtshop.rules.make_hint"), fx + 12, fy + 28, Ui.C_INK_SOFT, false);
        if (!result.isEmpty()) {
            g.drawString(font, Ui.clip(font, RulesScreens.name(result), W - 150), fx + 140, fy + 40 + 46, Ui.C_INK, false);
        }
        if (!error.isEmpty()) g.drawString(font, Ui.clip(font, error, W - 20), fx + 12, fy + H - 40, Ui.C_RED, false);
        super.render(g, mx, my, partial);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 아이템 칸: 왼쪽 클릭 = 고르기, 오른쪽 클릭 = 비우기. */
    class SlotButton extends Button {
        private final java.util.function.Supplier<String> get;
        private final java.util.function.Consumer<String> set;

        SlotButton(int x, int y, java.util.function.Supplier<String> get, java.util.function.Consumer<String> set) {
            super(x, y, 20, 20, Component.empty(), b -> {
            }, DEFAULT_NARRATION);
            this.get = get;
            this.set = set;
        }

        @Override
        public void onPress() {
            minecraft.setScreen(new PickerScreen(RecipeMakeScreen.this, PickerScreen.Mode.ITEM, v -> {
                set.accept(v);
                error = "";
            }));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 1 && isMouseOver(mx, my)) {
                set.accept("");
                return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        protected boolean isValidClickButton(int button) {
            return button == 0 || button == 1;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            int x = getX(), y = getY();
            g.fill(x, y, x + 20, y + 20, isHovered() ? 0xFF9A8A6A : 0xFF8B8B8B);
            g.fill(x + 1, y + 1, x + 19, y + 19, 0xFF373737);
            g.fill(x + 1, y + 1, x + 20, y + 20, 0xFFFFFFFF);
            g.fill(x + 1, y + 1, x + 19, y + 19, 0xFF8B8B8B);
            String v = get.get();
            if (!v.isEmpty()) g.renderItem(new ItemStack(RulesScreens.item(v)), x + 2, y + 2);
            if (isHovered() && !v.isEmpty()) g.renderTooltip(font, Component.literal(RulesScreens.name(v)), mx, my);
        }
    }

    // 시험용
    public void fillForTest(String[] items, String out, int n, boolean shapedRecipe) {
        System.arraycopy(items, 0, grid, 0, 9);
        result = out;
        count = n;
        shaped = shapedRecipe;
        rebuildWidgets();
    }

    public void doneForTest() {
        done();
    }

    public List<String> gridForTest() {
        return new ArrayList<>(List.of(grid));
    }
}
