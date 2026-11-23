package com.rtshop.client;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.rtshop.net.ItemStatsPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.stats.ItemStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 장비 능력치 편집기 (OP): 한 장비의 공격력·공격 속도·방어력 등을 툴팁 숫자 그대로 고친다. 빈칸 = 원래 값.
 * 저장하면 config/rtshop/item_stats.json 에 쓰이고 모두에게 바로 적용된다. "다른 장비"로 아무 아이템이나 고를 수 있다.
 */
public class ItemStatsScreen extends Screen {
    private static final String[] COMMON = {
            "minecraft:generic.attack_damage", "minecraft:generic.attack_speed", "minecraft:generic.armor",
            "minecraft:generic.armor_toughness", "minecraft:generic.knockback_resistance", "minecraft:generic.max_health",
            "minecraft:generic.movement_speed", "minecraft:generic.luck",
            "epicfight:impact", "epicfight:armor_negation", "epicfight:max_strikes"};
    private static final int ROW = 18;

    private String itemId;
    private ItemStack stack = ItemStack.EMPTY;
    private EquipmentSlot slot = EquipmentSlot.MAINHAND;
    private final List<String> attrs = new ArrayList<>();
    private final Map<String, Double> original = new LinkedHashMap<>();
    /** 편집 중인 값 (빈 문자열 = 원래 값). */
    private final Map<String, String> edit = new LinkedHashMap<>();
    private int fx, fy, W, H, rows, scroll;
    private boolean dirty;

    public ItemStatsScreen(String itemId) {
        super(Component.translatable("rtshop.itemstats.title"));
        select(itemId);
    }

    public static void open(String itemId) {
        Minecraft.getInstance().setScreen(new ItemStatsScreen(itemId));
    }

    /** 설정이 바뀌어 다시 받았을 때 (저장 직후): 열려 있으면 새 값으로 다시 그린다. */
    public static void refresh() {
        if (Minecraft.getInstance().screen instanceof ItemStatsScreen s) {
            s.select(s.itemId);
            if (s.width > 0) s.rebuildWidgets();
        }
    }

    public String itemForTest() {
        return itemId;
    }

    public List<String> attrsForTest() {
        return attrs;
    }

    public Map<String, Double> originalForTest() {
        return original;
    }

    private void select(String id) {
        itemId = id == null ? "" : id;
        attrs.clear();
        original.clear();
        edit.clear();
        scroll = 0;
        dirty = false;
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        stack = item == null || itemId.isEmpty() ? ItemStack.EMPTY : new ItemStack(item);
        if (stack.isEmpty()) return;
        slot = ItemStats.slotOf(stack);
        // 원래 값: 이 기능을 빼고 아이템이 원래 갖는 수정치 (툴팁처럼 공격력 +1, 공격 속도 +4)
        Multimap<Attribute, AttributeModifier> mods = stack.getItem().getAttributeModifiers(slot, stack);
        for (var en : mods.entries()) {
            ResourceLocation ar = ForgeRegistries.ATTRIBUTES.getKey(en.getKey());
            if (ar == null || en.getValue().getOperation() != AttributeModifier.Operation.ADDITION) continue;
            original.merge(ar.toString(), en.getValue().getAmount(), Double::sum);
        }
        for (String k : new ArrayList<>(original.keySet())) {
            Attribute a = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(k));
            original.put(k, original.get(k) + ItemStats.playerBase(a));
        }
        boolean weapon = slot == EquipmentSlot.MAINHAND;
        for (String k : COMMON) {
            ResourceLocation ar = ResourceLocation.tryParse(k);
            if (ar == null || !ForgeRegistries.ATTRIBUTES.containsKey(ar)) continue;
            // 무기에 방어 속성, 갑옷에 공격 속성은 원래 있거나 설정했을 때만 보인다
            boolean attack = k.contains("attack_") || k.startsWith("epicfight:");
            if (!original.containsKey(k) && weapon != attack && !hasSaved(k)) continue;
            attrs.add(k);
        }
        for (String k : original.keySet()) if (!attrs.contains(k)) attrs.add(k);
        Map<String, Double> saved = ItemStats.table(true).get(itemId);
        if (saved != null) for (String k : saved.keySet()) if (!attrs.contains(k)) attrs.add(k);
        for (String k : attrs) edit.put(k, saved != null && saved.containsKey(k) ? fmt(saved.get(k)) : "");
    }

    private boolean hasSaved(String k) {
        Map<String, Double> saved = ItemStats.table(true).get(itemId);
        return saved != null && saved.containsKey(k);
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        return String.format(Locale.ROOT, "%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private Component attrName(String id) {
        Attribute a = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(id));
        return a == null ? Component.literal(id) : Component.translatable(a.getDescriptionId());
    }

    @Override
    protected void init() {
        W = Math.min(360, width - 20);
        rows = Math.max(3, Math.min(attrs.size(), (height - 150) / ROW));
        H = 112 + rows * ROW;
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        int y0 = fy + 62;
        if (!stack.isEmpty()) {
            for (int i = 0; i < rows && i + scroll < attrs.size(); i++) {
                String k = attrs.get(i + scroll);
                int y = y0 + i * ROW;
                EditBox b = new EditBox(font, fx + W - 118, y, 60, 14, Component.empty());
                b.setMaxLength(12);
                b.setValue(edit.getOrDefault(k, ""));
                b.setFilter(s -> s.matches("-?[0-9]*\\.?[0-9]*"));
                b.setResponder(v -> {
                    if (!v.equals(edit.get(k))) {
                        edit.put(k, v);
                        dirty = true;
                    }
                });
                addRenderableWidget(b);
                addRenderableWidget(MButton.of(Component.translatable("rtshop.itemstats.default"), btn -> {
                    edit.put(k, "");
                    dirty = true;
                    rebuildWidgets();
                }).bounds(fx + W - 52, y - 1, 42, 16).build());
            }
        }
        int by = fy + H - 26;
        addRenderableWidget(MButton.of(Component.translatable("rtshop.itemstats.save"), b -> save()).bounds(fx + 10, by, 70, 18).build()).active = !stack.isEmpty();
        addRenderableWidget(MButton.of(Component.translatable("rtshop.itemstats.reset_all"), b -> {
            for (String k : attrs) edit.put(k, "");
            dirty = true;
            rebuildWidgets();
        }).bounds(fx + 84, by, 90, 18).build()).active = !stack.isEmpty();
        addRenderableWidget(MButton.of(Component.translatable("rtshop.itemstats.pick"), b ->
                minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, this::select))).bounds(fx + 178, by, 84, 18).build());
        addRenderableWidget(MButton.of(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(fx + W - 70, by, 60, 18).build());
    }

    private void save() {
        JsonObject o = new JsonObject();
        for (var en : edit.entrySet()) {
            String v = en.getValue().trim();
            if (v.isEmpty() || v.equals("-") || v.equals(".")) continue;
            try {
                o.addProperty(en.getKey(), Double.parseDouble(v));
            } catch (NumberFormatException ignored) {
            }
        }
        ShopNet.CHANNEL.sendToServer(new ItemStatsPacket(ItemStatsPacket.SAVE, itemId, o.toString()));
        dirty = false;
    }

    /** 테스트용: 값을 넣고 저장. */
    public void setAndSaveForTest(String attr, String value) {
        edit.put(attr, value);
        save();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, attrs.size() - rows);
        int s = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        if (s != scroll) {
            scroll = s;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        g.drawString(font, title, fx + 10, fy + 8, Ui.C_GOLD, false);
        if (dirty) g.drawString(font, Component.translatable("rtshop.enhedit.unsaved"), fx + 14 + font.width(title), fy + 8, 0xFFE0A070, false);
        int px = fx + 8, pw = W - 16;
        g.fill(px, fy + 22, px + pw, fy + H - 32, 0xFFE4D3AA);
        if (stack.isEmpty()) {
            g.drawString(font, Component.translatable("rtshop.itemstats.no_item"), fx + 16, fy + 32, Ui.C_INK, false);
        } else {
            g.renderItem(stack, fx + 14, fy + 28);
            g.drawString(font, Ui.clip(font, stack.getHoverName().getString(), W - 60), fx + 36, fy + 28, Ui.C_INK, false);
            g.drawString(font, Ui.clip(font, itemId + "  ·  " + Component.translatable("rtshop.itemstats.slot." + slot.getName()).getString(), W - 60),
                    fx + 36, fy + 38, Ui.C_INK_SOFT, false);
            int hy = fy + 50;
            g.drawString(font, Component.translatable("rtshop.itemstats.col_attr"), fx + 16, hy, Ui.C_INK_SOFT, false);
            g.drawString(font, Component.translatable("rtshop.itemstats.col_orig"), fx + W - 176, hy, Ui.C_INK_SOFT, false);
            g.drawString(font, Component.translatable("rtshop.itemstats.col_new"), fx + W - 118, hy, Ui.C_INK_SOFT, false);
            int y0 = fy + 62;
            for (int i = 0; i < rows && i + scroll < attrs.size(); i++) {
                String k = attrs.get(i + scroll);
                int y = y0 + i * ROW;
                if (i % 2 == 0) g.fill(px + 2, y - 2, px + pw - 2, y + ROW - 3, 0x18000000);
                g.drawString(font, Ui.clip(font, attrName(k).getString(), W - 200), fx + 16, y + 3, Ui.C_INK, false);
                Double o = original.get(k);
                g.drawString(font, o == null ? "-" : fmt(o), fx + W - 176, y + 3, Ui.C_INK_SOFT, false);
            }
            if (attrs.size() > rows) {
                String pg = (scroll + 1) + "-" + Math.min(attrs.size(), scroll + rows) + " / " + attrs.size();
                g.drawString(font, pg, fx + W - 14 - font.width(pg), fy + 28, Ui.C_INK_SOFT, false);
            }
        }
        g.drawString(font, Ui.clip(font, Component.translatable("rtshop.itemstats.hint").getString(), W - 20), fx + 10, fy + H - 44, Ui.C_INK_SOFT, false);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
