package com.rtshop.client;

import com.rtshop.compat.EfWeapons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 무기 설명서를 펼친 화면: 그 무기의 종류, 기본 연속 공격, 달리며·뛰어서 치는 공격, 특수기의 이름·설명·쓰는 법, 전투 조작 키.
 * 내용은 Epic Fight 가 가진 실제 무기 정보와 지금 설정된 키에서 읽어 온다 (그래서 키를 바꾸면 설명서의 키 이름도 따라 바뀐다).
 */
public class WeaponManualScreen extends Screen {
    private static final int W = 300, H = 222;

    /** 글줄 하나: 제목(금색 띠) 또는 본문. */
    private record Line(FormattedCharSequence text, int color, boolean head) {
    }

    private final ItemStack weapon;
    private final List<Line> lines = new ArrayList<>();
    private int fx, fy, scroll;
    private String skillId = "", category = "";
    private int combo;

    public WeaponManualScreen(ItemStack weapon) {
        super(weapon.isEmpty() ? Component.translatable("item.rtshop.weapon_manual")
                : Component.translatable("item.rtshop.weapon_manual.named", weapon.getHoverName()));
        this.weapon = weapon;
    }

    public String skillForTest() {
        return skillId;
    }

    public String categoryForTest() {
        return category;
    }

    public int comboForTest() {
        return combo;
    }

    public int linesForTest() {
        return lines.size();
    }

    private static String key(String name) {
        return "[" + QuestText.keyLabel(name) + "]";
    }

    /** 드는 방식의 이름 (한 손, 양손…). */
    private static String style(String id) {
        String k = "rtshop.manual.style." + id;
        return I18n.exists(k) ? I18n.get(k) : id.replace('_', ' ');
    }

    private void head(String key) {
        if (!lines.isEmpty()) lines.add(new Line(FormattedCharSequence.EMPTY, 0, false));
        lines.add(new Line(Component.translatable(key).getVisualOrderText(), Ui.C_GOLD_DARK, true));
    }

    private void body(Component c, int color) {
        for (FormattedCharSequence s : this.font.split(c, W - 40)) lines.add(new Line(s, color, false));
    }

    private void body(String key, Object... args) {
        body(Component.translatable(key, args), 0xFF4A3520);
    }

    /** 특수기 설명글: Epic Fight 의 번역글(skill.<모드>.<이름>.tooltip)에 그 기술의 수치를 채워 넣는다. */
    private static String skillText(Object skill, String id) {
        int c = id.indexOf(':');
        String k = "skill." + (c < 0 ? id : id.substring(0, c) + "." + id.substring(c + 1)) + ".tooltip";
        if (!I18n.exists(k)) return "";
        List<Object> args = new ArrayList<>();
        try {
            Object r = skill.getClass().getMethod("getTooltipArgsOfScreen", List.class).invoke(skill, args);
            if (r instanceof List<?> l) {
                args = new ArrayList<>(l);
            }
        } catch (Throwable ignored) {
        }
        try {
            String s = I18n.get(k, args.toArray());
            if (!s.startsWith("Format error")) return s;
        } catch (Throwable ignored) {
        }
        return I18n.get(k).replace("%s", "?").replace("%d", "?");
    }

    private static String skillName(String id) {
        int c = id.indexOf(':');
        String k = "skill." + (c < 0 ? id : id.substring(0, c) + "." + id.substring(c + 1));
        return I18n.exists(k) ? I18n.get(k) : id;
    }

    private void build() {
        lines.clear();
        Minecraft mc = Minecraft.getInstance();
        EfWeapons.Info info = weapon.isEmpty() ? null : EfWeapons.info(mc.player, weapon);
        if (info == null) {
            body(Component.translatable("rtshop.manual.none"), Ui.C_INK_SOFT);
            return;
        }
        category = info.category();
        skillId = info.skill();
        combo = info.combo();
        String catKey = "rtshop.manual.cat." + category;
        String cat = I18n.exists(catKey) ? I18n.get(catKey) : category.replace('_', ' ');
        body(Component.translatable("rtshop.manual.kind", cat), Ui.C_INK);

        // 드는 방식(한 손·양손·쌍수…)마다 연속 공격과 특수기가 다를 수 있다: 같은 것끼리는 묶어서 적는다
        List<EfWeapons.Form> forms = info.forms();
        boolean many = forms.stream().map(f -> f.combo() + "|" + f.skill()).distinct().count() > 1;
        head("rtshop.manual.h.basic");
        if (many) {
            body("rtshop.manual.basic0", key("key.epicfight.attack"));
            for (EfWeapons.Form f : forms) {
                if (f.combo() > 0) body("rtshop.manual.basic_style", style(f.style()), f.combo());
            }
        } else if (combo > 0) {
            body("rtshop.manual.basic", key("key.epicfight.attack"), combo);
        } else {
            body("rtshop.manual.basic0", key("key.epicfight.attack"));
        }
        if (info.dashAndAir()) {
            body("rtshop.manual.dash", key("key.sprint"), key("key.epicfight.attack"));
            body("rtshop.manual.air", key("key.jump"), key("key.epicfight.attack"));
        }

        if (!skillId.isEmpty()) {
            head("rtshop.manual.h.skill");
            java.util.LinkedHashMap<String, List<String>> by = new java.util.LinkedHashMap<>();
            java.util.HashMap<String, Object> objs = new java.util.HashMap<>();
            for (EfWeapons.Form f : forms) {
                if (f.skill().isEmpty()) continue;
                by.computeIfAbsent(f.skill(), x -> new ArrayList<>()).add(style(f.style()));
                objs.putIfAbsent(f.skill(), f.skillObject());
            }
            for (var e : by.entrySet()) {
                String name = "◆ " + skillName(e.getKey());
                if (by.size() > 1) name += "  (" + String.join(" · ", e.getValue()) + ")";
                body(Component.literal(name), 0xFF7A3B12);
                Object skill = objs.get(e.getKey());
                String text = skill == null ? "" : skillText(skill, e.getKey());
                if (!text.isEmpty()) body(Component.literal(text), 0xFF4A3520);
            }
            body("rtshop.manual.skill_use", key("key.epicfight.weapon_innate_skill"));
            body(Component.translatable("rtshop.manual.skill_gauge"), Ui.C_INK_SOFT);
        }
        if (many) body(Component.translatable("rtshop.manual.style_note"), Ui.C_INK_SOFT);

        head("rtshop.manual.h.keys");
        body("rtshop.manual.k.mode", key("key.epicfight.switch_mode"));
        body("rtshop.manual.k.dodge", key("key.epicfight.dodge"));
        body("rtshop.manual.k.guard", key("key.epicfight.guard"));
        body("rtshop.manual.k.lock", key("key.epicfight.lock_on"));
        body(Component.translatable("rtshop.manual.k.note"), Ui.C_INK_SOFT);
    }

    @Override
    protected void init() {
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        build();
    }

    private int visible() {
        return (H - 62) / 11;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = Ui.clip(this.font, this.title.getString(), W - 60);
        int tw = this.font.width(t) + (weapon.isEmpty() ? 0 : 20);
        int tx = fx + (W - tw) / 2;
        if (!weapon.isEmpty()) {
            g.renderItem(weapon, tx, fy + 5);
            tx += 20;
        }
        g.drawString(this.font, t, tx, fy + 9, Ui.C_GOLD, true);
        int px = fx + 8, py = fy + 28, pw = W - 16, ph = H - 54;
        Ui.bevel(g, px, py, pw, ph, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        int vis = visible();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - vis)));
        for (int i = 0; i < vis && i + scroll < lines.size(); i++) {
            Line l = lines.get(i + scroll);
            int y = py + 7 + i * 11;
            if (l.head) {
                g.fill(px + 8, y + 9, px + pw - 10, y + 10, 0x60A07828);
                g.drawString(this.font, l.text, px + 10, y, l.color, false);
            } else {
                g.drawString(this.font, l.text, px + 12, y, l.color, false);
            }
        }
        if (lines.size() > vis) {
            int bh = Math.max(12, (ph - 6) * vis / lines.size()), by = py + 3 + (ph - 6 - bh) * scroll / Math.max(1, lines.size() - vis);
            g.fill(px + pw - 5, by, px + pw - 3, by + bh, Ui.C_PARCH_SHADE);
        }
        int bw = 70, bx = fx + (W - bw) / 2, by = fy + H - 22;
        Ui.button(g, this.font, bx, by, bw, 16, I18n.get("rtshop.manual.close"), Ui.inside(mx, my, bx, by, bw, 16), true);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int bw = 70, bx = fx + (W - bw) / 2, by = fy + H - 22;
        if (button == 0 && Ui.inside(mx, my, bx, by, bw, 16)) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta) * 2);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
