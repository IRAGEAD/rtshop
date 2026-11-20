package com.rtshop.client;

import com.rtshop.item.ModItems;
import com.rtshop.land.LandData;
import com.rtshop.land.LandManager;
import com.rtshop.net.LandActionPacket;
import com.rtshop.net.LandInfoPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 토지 설정 화면 (/rtshop land).
 * 왼쪽: 내 토지 목록 · 오른쪽: 선택한 토지의 이름, 기본 권한, 플레이어별 권한, 경계 표시, 반환.
 * 모든 변경은 서버에 요청만 보내고, 서버가 확인한 결과를 다시 받아 화면을 고친다.
 */
public class LandScreen extends Screen {
    private static final int FRAME_W = 400;
    private static final int FRAME_H = 236;
    private static final int LIST_W = 104;
    private static final int ROW_H = 22;
    private static final int M_ROW = 18;

    private static final int[] PERMS = {LandData.BREAK, LandData.PLACE, LandData.CONTAINER, LandData.USE, LandData.ENTITY};
    private static final String[] PERM_KEYS = {"rtshop.land.perm.break", "rtshop.land.perm.place",
            "rtshop.land.perm.container", "rtshop.land.perm.use", "rtshop.land.perm.entity"};

    private List<LandInfoPacket.View> views;
    private int currentId;
    private int selectedId = -1;
    private int listScroll = 0;
    private int memberScroll = 0;
    private int abandonTicks = 0;

    private EditBox nameBox;
    private EditBox memberBox;
    private final ItemStack icon = new ItemStack(ModItems.LAND_DEED.get());

    private int fx, fy, fw, fh;
    private int lx, ly, lh;
    private int px, py, pw, ph;

    public LandScreen(List<LandInfoPacket.View> views, int currentId) {
        super(Component.translatable("rtshop.land.title"));
        this.views = views == null ? new ArrayList<>() : views;
        this.currentId = currentId;
        pickSelection();
    }

    // =====================================================================================
    // 상태
    // =====================================================================================

    private LandInfoPacket.View find(int id) {
        for (LandInfoPacket.View v : views) {
            if (v.id == id) {
                return v;
            }
        }
        return null;
    }

    private LandInfoPacket.View selected() {
        return find(selectedId);
    }

    private void pickSelection() {
        if (find(selectedId) != null) {
            return;
        }
        if (find(currentId) != null) {
            selectedId = currentId;
        } else {
            selectedId = views.isEmpty() ? -1 : views.get(0).id;
        }
        memberScroll = 0;
        abandonTicks = 0;
    }

    /** 서버가 새 정보를 보냈을 때. */
    public void update(List<LandInfoPacket.View> newViews, int newCurrent) {
        this.views = newViews == null ? new ArrayList<>() : newViews;
        this.currentId = newCurrent;
        int before = selectedId;
        pickSelection();
        memberScroll = Math.max(0, Math.min(memberScroll, maxMemberScroll()));
        listScroll = Math.max(0, Math.min(listScroll, maxListScroll()));
        syncBoxes(before != selectedId);
    }

    private void syncBoxes(boolean resetName) {
        if (nameBox == null || memberBox == null) {
            return;
        }
        LandInfoPacket.View v = selected();
        boolean owner = v != null && v.owner;
        nameBox.setVisible(owner);
        memberBox.setVisible(owner);
        if (!owner) {
            nameBox.setFocused(false);
            memberBox.setFocused(false);
            if (this.getFocused() == nameBox || this.getFocused() == memberBox) {
                this.setFocused(null);
            }
        } else if (resetName || !nameBox.isFocused()) {
            nameBox.setValue(v.name);
        }
    }

    @Override
    protected void init() {
        fw = Math.min(FRAME_W, this.width - 8);
        fh = Math.min(FRAME_H, this.height - 8);
        fx = (this.width - fw) / 2;
        fy = (this.height - fh) / 2;
        lx = fx + 6;
        ly = fy + 28;
        lh = fh - 28 - 6;
        px = lx + LIST_W + 4;
        py = ly;
        pw = fx + fw - 6 - px;
        ph = lh;

        String oldMember = memberBox != null ? memberBox.getValue() : "";
        nameBox = new EditBox(this.font, px + 10, py + 9, 112, 10, Component.translatable("rtshop.land.name"));
        nameBox.setBordered(false);
        nameBox.setMaxLength(24);
        nameBox.setTextColor(0xFFF5E6C0);
        memberBox = new EditBox(this.font, px + 10, addY() + 4, 112, 10, Component.translatable("rtshop.land.add_hint"));
        memberBox.setBordered(false);
        memberBox.setMaxLength(16);
        memberBox.setTextColor(0xFFF5E6C0);
        memberBox.setValue(oldMember);
        this.addRenderableWidget(nameBox);
        this.addRenderableWidget(memberBox);
        syncBoxes(true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (nameBox != null) {
            nameBox.tick();
        }
        if (memberBox != null) {
            memberBox.tick();
        }
        if (abandonTicks > 0) {
            abandonTicks--;
        }
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void send(LandInfoPacket.View v, int action, String text, int value) {
        ShopNet.CHANNEL.sendToServer(new LandActionPacket(v.id, action, text == null ? "" : text, value));
    }

    // =====================================================================================
    // 배치
    // =====================================================================================

    private int maxListScroll() {
        return Math.max(0, views.size() * ROW_H + 4 - lh);
    }

    private int addY() {
        return py + ph - 38;
    }

    private int bottomY() {
        return py + ph - 19;
    }

    private int memberTop() {
        return py + 88;
    }

    private int memberVisible() {
        return Math.max(1, (addY() - 3 - memberTop()) / M_ROW);
    }

    private int maxMemberScroll() {
        LandInfoPacket.View v = selected();
        return v == null ? 0 : Math.max(0, v.members.size() - memberVisible());
    }

    private int defX(int i) {
        return px + 6 + i * 54;
    }

    private int defY() {
        return py + 58;
    }

    private int memToggleX(int i) {
        return px + 80 + i * 35;
    }

    private int removeX() {
        return px + 257;
    }

    // =====================================================================================
    // 그리기
    // =====================================================================================

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, fw, fh);
        g.fill(fx + 5, fy + 4, fx + fw - 5, fy + 23, Ui.C_WOOD_DARK);
        g.fill(fx + 5, fy + 22, fx + fw - 5, fy + 23, Ui.C_GOLD_DARK);
        g.renderItem(icon, fx + 9, fy + 5);
        g.drawString(this.font, this.title, fx + 29, fy + 9, Ui.C_GOLD, true);
        String help = Component.translatable("rtshop.land.help").getString();
        int helpMax = fw - 40 - this.font.width(this.title) - 12;
        help = Ui.clip(this.font, help, helpMax);
        g.drawString(this.font, help, fx + fw - 10 - this.font.width(help), fy + 9, 0xFFBFA472, false);

        drawList(g, mx, my);
        Ui.bevel(g, px, py, pw, ph, Ui.C_PARCH, Ui.C_PARCH_SHADE, Ui.C_SHADOW);

        LandInfoPacket.View v = selected();
        Component tip = null;
        if (v == null) {
            drawEmpty(g);
        } else {
            tip = drawDetail(g, v, mx, my);
        }

        super.render(g, mx, my, partial); // 입력 상자

        if (v != null && v.owner && memberBox != null && memberBox.getValue().isEmpty() && !memberBox.isFocused()) {
            g.drawString(this.font, Component.translatable("rtshop.land.add_hint"), px + 10, addY() + 4, 0xFF9C8460, false);
        }
        if (tip != null) {
            g.renderTooltip(this.font, this.font.split(tip, 190), mx, my);
        }
    }

    private void drawList(GuiGraphics g, int mx, int my) {
        Ui.bevel(g, lx, ly, LIST_W, lh, Ui.C_PARCH_DARK, Ui.C_PARCH_SHADE, Ui.C_SHADOW);
        g.enableScissor(lx + 1, ly + 1, lx + LIST_W - 1, ly + lh - 1);
        for (int i = 0; i < views.size(); i++) {
            LandInfoPacket.View v = views.get(i);
            int y = ly + 2 + i * ROW_H - listScroll;
            if (y + ROW_H < ly || y > ly + lh) {
                continue;
            }
            boolean sel = v.id == selectedId;
            boolean hov = Ui.inside(mx, my, lx + 2, y, LIST_W - 4, ROW_H - 1) && Ui.inside(mx, my, lx, ly, LIST_W, lh);
            if (sel) {
                g.fill(lx + 2, y, lx + LIST_W - 2, y + ROW_H - 1, Ui.C_PARCH_HI);
                g.fill(lx + 2, y, lx + 4, y + ROW_H - 1, Ui.C_GOLD_DARK);
            } else if (hov) {
                g.fill(lx + 2, y, lx + LIST_W - 2, y + ROW_H - 1, 0xFFE6D3A3);
            }
            g.drawString(this.font, Ui.clip(this.font, v.name, LIST_W - 12), lx + 7, y + 2, Ui.C_INK, false);
            String second;
            if (!v.owner) {
                second = Component.translatable("rtshop.land.list_other", v.ownerName).getString();
            } else if (v.id == currentId) {
                second = Component.translatable("rtshop.land.list_here", v.claims.size()).getString();
            } else {
                second = Component.translatable("rtshop.land.list_mine", v.claims.size()).getString();
            }
            g.drawString(this.font, Ui.clip(this.font, second, LIST_W - 12), lx + 7, y + 11,
                    v.id == currentId ? Ui.C_GREEN : Ui.C_INK_SOFT, false);
        }
        g.disableScissor();
        if (views.isEmpty()) {
            String s = Component.translatable("rtshop.land.none").getString();
            g.drawString(this.font, Ui.clip(this.font, s, LIST_W - 10), lx + 6, ly + 6, Ui.C_INK_SOFT, false);
        }
    }

    private void drawEmpty(GuiGraphics g) {
        String[] keys = {"rtshop.land.empty1", "rtshop.land.empty2", "rtshop.land.empty3", "rtshop.land.empty4"};
        int y = py + 14;
        for (String k : keys) {
            for (var line : this.font.split(Component.translatable(k), pw - 20)) {
                g.drawString(this.font, line, px + 10, y, Ui.C_INK, false);
                y += 10;
            }
            y += 4;
        }
    }

    private void toggle(GuiGraphics g, int x, int y, int w, String label, boolean on, boolean hover, boolean clickable) {
        int fill;
        if (on) {
            fill = hover && clickable ? 0xFF3F8A30 : Ui.C_GREEN;
        } else {
            fill = hover && clickable ? Ui.C_WOOD_HI : Ui.C_WOOD_DARK;
        }
        Ui.bevel(g, x, y, w, 14, fill, on ? 0xFF6FBF5A : Ui.C_WOOD_HI, Ui.C_SHADOW);
        String s = Ui.clip(this.font, label, w - 4);
        g.drawString(this.font, s, x + (w - this.font.width(s)) / 2, y + 3, on ? 0xFFFFFFFF : 0xFF9C8460, false);
    }

    private String permLabel(int i) {
        return Component.translatable(PERM_KEYS[i]).getString();
    }

    /** 선택한 토지의 상세 내용을 그리고, 마우스가 올라간 권한 버튼의 설명을 돌려준다. */
    private Component drawDetail(GuiGraphics g, LandInfoPacket.View v, int mx, int my) {
        Component tip = null;
        int small = 0;
        int large = 0;
        for (LandInfoPacket.ClaimView c : v.claims) {
            if ("large".equals(c.type)) {
                large++;
            } else {
                small++;
            }
        }
        int textMax = pw - 12;
        if (v.owner) {
            Ui.bevel(g, px + 6, py + 5, 120, 16, Ui.C_WOOD_DARK, Ui.C_SHADOW, Ui.C_WOOD_HI);
            Ui.button(g, this.font, px + 130, py + 5, 58, 16, Component.translatable("rtshop.land.rename").getString(),
                    Ui.inside(mx, my, px + 130, py + 5, 58, 16), true);
        } else {
            g.drawString(this.font, Ui.clip(this.font, v.name, textMax), px + 8, py + 9, Ui.C_INK, false);
        }
        String info1 = Component.translatable("rtshop.land.info1", v.ownerName, v.claims.size()).getString();
        g.drawString(this.font, Ui.clip(this.font, info1, textMax), px + 8, py + 25, Ui.C_INK, false);
        String info2;
        if (v.claims.isEmpty()) {
            info2 = "";
        } else {
            LandInfoPacket.ClaimView c = v.claims.get(0);
            info2 = Component.translatable("rtshop.land.info2", small, large,
                    (c.x0 + c.x1) / 2, (c.y0 + c.y1) / 2, (c.z0 + c.z1) / 2).getString();
        }
        g.drawString(this.font, Ui.clip(this.font, info2, textMax), px + 8, py + 35, Ui.C_INK_SOFT, false);

        if (!v.owner) {
            g.drawString(this.font, Component.translatable("rtshop.land.my_perms"), px + 8, py + 48, Ui.C_INK, false);
            for (int i = 0; i < 5; i++) {
                boolean hov = Ui.inside(mx, my, defX(i), defY(), 50, 14);
                toggle(g, defX(i), defY(), 50, permLabel(i), (v.myPerms & PERMS[i]) != 0, hov, false);
                if (hov) {
                    tip = Component.translatable(PERM_KEYS[i] + ".tip");
                }
            }
            int y = py + 80;
            for (var line : this.font.split(Component.translatable("rtshop.land.readonly"), pw - 20)) {
                g.drawString(this.font, line, px + 8, y, Ui.C_INK_SOFT, false);
                y += 10;
            }
            return tip;
        }

        // 기본 권한
        g.drawString(this.font, Component.translatable("rtshop.land.default_perms"), px + 8, py + 48, Ui.C_INK, false);
        for (int i = 0; i < 5; i++) {
            boolean hov = Ui.inside(mx, my, defX(i), defY(), 50, 14);
            toggle(g, defX(i), defY(), 50, permLabel(i), (v.defaultPerms & PERMS[i]) != 0, hov, true);
            if (hov) {
                tip = Component.translatable(PERM_KEYS[i] + ".tip");
            }
        }

        // 플레이어별 권한
        g.drawString(this.font, Component.translatable("rtshop.land.members", v.members.size()), px + 8, py + 77, Ui.C_INK, false);
        int vis = memberVisible();
        int top = memberTop();
        if (v.members.isEmpty()) {
            g.drawString(this.font, Ui.clip(this.font, Component.translatable("rtshop.land.members_none").getString(), textMax),
                    px + 8, top + 4, Ui.C_INK_SOFT, false);
        }
        for (int r = 0; r < vis; r++) {
            int idx = memberScroll + r;
            if (idx >= v.members.size()) {
                break;
            }
            LandInfoPacket.MemberView m = v.members.get(idx);
            int y = top + r * M_ROW;
            g.fill(px + 4, y - 1, px + pw - 4, y + 15, (r & 1) == 0 ? 0x22000000 : 0x11000000);
            g.drawString(this.font, Ui.clip(this.font, m.name, 70), px + 7, y + 3, Ui.C_INK, false);
            for (int i = 0; i < 5; i++) {
                boolean hov = Ui.inside(mx, my, memToggleX(i), y, 33, 14);
                toggle(g, memToggleX(i), y, 33, permLabel(i), (m.perms & PERMS[i]) != 0, hov, true);
                if (hov) {
                    tip = Component.translatable(PERM_KEYS[i] + ".tip");
                }
            }
            boolean hovX = Ui.inside(mx, my, removeX(), y, 14, 14);
            Ui.bevel(g, removeX(), y, 14, 14, hovX ? Ui.C_RED : Ui.C_WOOD, Ui.C_WOOD_HI, Ui.C_SHADOW);
            g.drawString(this.font, "x", removeX() + 4, y + 2, Ui.C_CREAM, false);
            if (hovX) {
                tip = Component.translatable("rtshop.land.remove_tip", m.name);
            }
        }
        if (v.members.size() > vis) {
            String more = (memberScroll + 1) + "-" + Math.min(v.members.size(), memberScroll + vis) + " / " + v.members.size();
            g.drawString(this.font, more, px + pw - 8 - this.font.width(more), py + 77, Ui.C_INK_SOFT, false);
        }

        // 추가
        Ui.bevel(g, px + 6, addY(), 120, 16, Ui.C_WOOD_DARK, Ui.C_SHADOW, Ui.C_WOOD_HI);
        Ui.button(g, this.font, px + 130, addY(), 58, 16, Component.translatable("rtshop.land.add").getString(),
                Ui.inside(mx, my, px + 130, addY(), 58, 16), true);

        // 아래 버튼
        Ui.button(g, this.font, px + 6, bottomY(), 92, 15, Component.translatable("rtshop.land.show_border").getString(),
                Ui.inside(mx, my, px + 6, bottomY(), 92, 15), true);
        boolean hovAb = Ui.inside(mx, my, px + pw - 6 - 112, bottomY(), 112, 15);
        String ab = Component.translatable(abandonTicks > 0 ? "rtshop.land.abandon_confirm" : "rtshop.land.abandon").getString();
        Ui.bevel(g, px + pw - 6 - 112, bottomY(), 112, 15, abandonTicks > 0 ? Ui.C_RED : (hovAb ? Ui.C_WOOD_HI : Ui.C_WOOD),
                Ui.C_WOOD_HI, Ui.C_SHADOW);
        ab = Ui.clip(this.font, ab, 106);
        g.drawString(this.font, ab, px + pw - 6 - 112 + (112 - this.font.width(ab)) / 2, bottomY() + 4, Ui.C_CREAM, false);
        if (hovAb) {
            tip = Component.translatable("rtshop.land.abandon_tip", v.claims.size());
        }
        return tip;
    }

    // =====================================================================================
    // 입력
    // =====================================================================================

    private void rename(LandInfoPacket.View v) {
        String n = nameBox.getValue().trim();
        if (!n.isEmpty() && !n.equals(v.name)) {
            send(v, LandManager.ACT_RENAME, n, 0);
        }
        nameBox.setFocused(false);
        this.setFocused(null);
    }

    private void addMember(LandInfoPacket.View v) {
        String n = memberBox.getValue().trim();
        if (!n.isEmpty()) {
            send(v, LandManager.ACT_ADD_MEMBER, n, 0);
            memberBox.setValue("");
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && Ui.inside(mx, my, lx, ly, LIST_W, lh)) {
            int rel = (int) my - (ly + 2) + listScroll;
            if (rel >= 0) {
                int idx = rel / ROW_H;
                if (idx < views.size() && views.get(idx).id != selectedId) {
                    click();
                    selectedId = views.get(idx).id;
                    memberScroll = 0;
                    abandonTicks = 0;
                    syncBoxes(true);
                }
            }
            return true;
        }
        LandInfoPacket.View v = selected();
        if (button == 0 && v != null && v.owner) {
            if (Ui.inside(mx, my, px + 130, py + 5, 58, 16)) {
                click();
                rename(v);
                return true;
            }
            for (int i = 0; i < 5; i++) {
                if (Ui.inside(mx, my, defX(i), defY(), 50, 14)) {
                    click();
                    send(v, LandManager.ACT_SET_DEFAULT, "", v.defaultPerms ^ PERMS[i]);
                    return true;
                }
            }
            int vis = memberVisible();
            for (int r = 0; r < vis; r++) {
                int idx = memberScroll + r;
                if (idx >= v.members.size()) {
                    break;
                }
                LandInfoPacket.MemberView m = v.members.get(idx);
                int y = memberTop() + r * M_ROW;
                for (int i = 0; i < 5; i++) {
                    if (Ui.inside(mx, my, memToggleX(i), y, 33, 14)) {
                        click();
                        send(v, LandManager.ACT_SET_MEMBER, m.id.toString(), m.perms ^ PERMS[i]);
                        return true;
                    }
                }
                if (Ui.inside(mx, my, removeX(), y, 14, 14)) {
                    click();
                    send(v, LandManager.ACT_REMOVE_MEMBER, m.id.toString(), 0);
                    return true;
                }
            }
            if (Ui.inside(mx, my, px + 130, addY(), 58, 16)) {
                click();
                addMember(v);
                return true;
            }
            if (Ui.inside(mx, my, px + 6, bottomY(), 92, 15)) {
                click();
                send(v, LandManager.ACT_SHOW_BORDER, "", 0);
                this.onClose(); // 경계(입자)가 보이도록 화면을 닫는다
                return true;
            }
            if (Ui.inside(mx, my, px + pw - 6 - 112, bottomY(), 112, 15)) {
                click();
                if (abandonTicks > 0) {
                    abandonTicks = 0;
                    send(v, LandManager.ACT_ABANDON, "", 0);
                } else {
                    abandonTicks = 80;
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (Ui.inside(mx, my, lx, ly, LIST_W, lh)) {
            listScroll = Math.max(0, Math.min(maxListScroll(), listScroll - (int) (delta * 14)));
            return true;
        }
        if (Ui.inside(mx, my, px, py, pw, ph)) {
            memberScroll = Math.max(0, Math.min(maxMemberScroll(), memberScroll - (delta > 0 ? 1 : -1)));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        LandInfoPacket.View v = selected();
        boolean typing = (nameBox != null && nameBox.isFocused()) || (memberBox != null && memberBox.isFocused());
        if (v != null && v.owner && (key == 257 || key == 335)) { // Enter
            if (nameBox.isFocused()) {
                rename(v);
                return true;
            }
            if (memberBox.isFocused()) {
                addMember(v);
                return true;
            }
        }
        if (!typing && this.minecraft != null && this.minecraft.options.keyInventory.matches(key, scan)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
