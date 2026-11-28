package com.rtshop.client;

import com.rtshop.net.JobChoosePacket;
import com.rtshop.net.JobOpenPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 직업 화면: 전투 / 생활 탭, 왼쪽 직업 목록, 오른쪽 패시브 설명과 고르기·바꾸기 버튼.
 * 직업 안내인 앞에서만 고르거나 바꿀 수 있고, /rtshop job 으로 열면 보기만 할 수 있다.
 */
public class JobScreen extends Screen implements NpcButtons.Host {
    @Override
    public int[] npcButtonSpot() {
        return new int[]{fx + 10 + this.font.width(this.title) + 10, fy + 6};
    }

    private static final String COMBAT = "combat";
    private static final String LIFE = "life";
    private static final int LIST_W = 120;
    private static final int ROW = 24;

    private JobOpenPacket data;
    private String tab;
    private String selected;
    private boolean confirm = false;
    private Component status;
    private boolean statusOk;
    private int statusTicks;
    private int fx, fy, W, H;

    public JobScreen(JobOpenPacket data) {
        super(Component.translatable("rtshop.job.title"));
        this.data = data;
        this.tab = data.combat.isEmpty() || !data.life.isEmpty() ? COMBAT : LIFE;
        pickDefault();
    }

    public void update(JobOpenPacket packet) {
        this.data = packet;
        confirm = false;
        if (find(selected) == null) {
            pickDefault();
        }
    }

    public void showResult(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
        statusTicks = 200;
    }

    private void pickDefault() {
        String cur = COMBAT.equals(tab) ? data.combat : data.life;
        selected = null;
        for (JobOpenPacket.JobView j : tabJobs()) {
            if (selected == null || j.id().equals(cur)) {
                selected = j.id();
            }
        }
    }

    private List<JobOpenPacket.JobView> tabJobs() {
        List<JobOpenPacket.JobView> out = new ArrayList<>();
        for (JobOpenPacket.JobView j : data.jobs) {
            if (j.slot().equals(tab)) {
                out.add(j);
            }
        }
        return out;
    }

    private JobOpenPacket.JobView find(String id) {
        if (id == null) {
            return null;
        }
        for (JobOpenPacket.JobView j : data.jobs) {
            if (j.id().equals(id) && j.slot().equals(tab)) {
                return j;
            }
        }
        return null;
    }

    private String nameOf(String id) {
        for (JobOpenPacket.JobView j : data.jobs) {
            if (j.id().equals(id)) {
                return j.name();
            }
        }
        return id.isEmpty() ? Component.translatable("rtshop.job.none").getString() : id;
    }

    private static ItemStack icon(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        return item == null || item == Items.AIR ? new ItemStack(Items.BOOK) : new ItemStack(item);
    }

    @Override
    protected void init() {
        W = Math.min(this.width - 8, 420);
        H = Math.min(this.height - 8, 260);
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (statusTicks > 0 && --statusTicks == 0) {
            status = null;
        }
    }

    private int listX() {
        return fx + 6;
    }

    private int listY() {
        return fy + 46;
    }

    private int listH() {
        return H - 46 - 8;
    }

    private int rx() {
        return fx + 6 + LIST_W + 6;
    }

    private int rw() {
        return W - LIST_W - 18;
    }

    private int btnX() {
        return rx() + rw() - 118;
    }

    private int btnY() {
        return fy + H - 30;
    }

    private long cooldown() {
        return COMBAT.equals(tab) ? data.combatCooldown : data.lifeCooldown;
    }

    private long cost() {
        return COMBAT.equals(tab) ? data.costCombat : data.costLife;
    }

    private String current() {
        return COMBAT.equals(tab) ? data.combat : data.life;
    }

    /** 3.84: 이 칸에서 내려놓은 직업 ("" = 없음). */
    private String last() {
        return COMBAT.equals(tab) ? data.lastCombat : data.lastLife;
    }

    /** 이 직업을 고르는 것이 무료인가: 처음 고르기, 또는 내려놓은 직업으로 돌아오기. */
    private boolean freePick(JobOpenPacket.JobView j) {
        return current().isEmpty() && (last().isEmpty() || last().equals(j.id()));
    }

    private String buttonLabel(JobOpenPacket.JobView j) {
        if (j.state() == JobOpenPacket.CURRENT) {
            return Component.translatable("rtshop.job.btn_resign").getString();   // 3.84: 지금 직업 → 내려놓기
        }
        if (freePick(j)) {
            return Component.translatable("rtshop.job.btn_choose").getString();
        }
        return Component.translatable("rtshop.job.btn_change", Ui.num(cost())).getString();
    }

    private boolean buttonEnabled(JobOpenPacket.JobView j) {
        if (!data.atNpc) {
            return false;
        }
        if (j.state() == JobOpenPacket.CURRENT) {
            return true;   // 내려놓기는 언제든 무료
        }
        if (j.state() != JobOpenPacket.AVAILABLE) {
            return false;
        }
        if (freePick(j)) {
            return true;
        }
        return cooldown() <= 0 && ClientEconomy.wallet >= cost();
    }

    private static String duration(long ms) {
        long min = (ms + 59_999) / 60_000;
        long h = min / 60;
        long m = min % 60;
        if (h >= 24) {
            return Component.translatable("rtshop.job.dur_days", h / 24, h % 24).getString();
        }
        return h > 0 ? Component.translatable("rtshop.job.dur_hours", h, m).getString()
                : Component.translatable("rtshop.job.dur_minutes", m).getString();
    }

    private static String hours(double h) {
        return h == Math.rint(h) ? Long.toString((long) h) : String.format(java.util.Locale.ROOT, "%.1f", h);
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        g.drawString(this.font, this.title, fx + 10, fy + 9, Ui.C_GOLD, true);
        String now = Component.translatable("rtshop.job.now", nameOf(data.combat), nameOf(data.life)).getString();
        now = Ui.clip(this.font, now, W - 30 - this.font.width(this.title) - NpcButtons.width(this));
        g.drawString(this.font, now, fx + W - 10 - this.font.width(now), fy + 9, Ui.C_CREAM, false);

        // 탭
        int tw = (W - 12 - 4) / 2;
        String[] tabs = {COMBAT, LIFE};
        for (int i = 0; i < 2; i++) {
            int x = fx + 6 + i * (tw + 4);
            String label = Component.translatable(i == 0 ? "rtshop.job.tab_combat" : "rtshop.job.tab_life").getString();
            Ui.button(g, this.font, x, fy + 27, tw, 15, label, Ui.inside(mx, my, x, fy + 27, tw, 15), !tabs[i].equals(tab));
        }

        // 목록
        Ui.bevel(g, listX(), listY(), LIST_W, listH(), Ui.C_PARCH_DARK, Ui.C_PARCH_SHADE, Ui.C_SHADOW);
        List<JobOpenPacket.JobView> jobs = tabJobs();
        for (int i = 0; i < jobs.size(); i++) {
            JobOpenPacket.JobView j = jobs.get(i);
            int y = listY() + 2 + i * ROW;
            if (y + ROW > listY() + listH()) {
                break;
            }
            boolean sel = j.id().equals(selected);
            boolean hover = Ui.inside(mx, my, listX() + 2, y, LIST_W - 4, ROW - 2);
            if (sel) {
                g.fill(listX() + 2, y, listX() + LIST_W - 2, y + ROW - 2, 0x55A87A22);
            } else if (hover) {
                g.fill(listX() + 2, y, listX() + LIST_W - 2, y + ROW - 2, 0x22000000);
            }
            g.renderItem(icon(j.icon()), listX() + 4, y + 3);
            g.drawString(this.font, Ui.clip(this.font, j.name(), LIST_W - 28), listX() + 24, y + 3, Ui.C_INK, false);
            String st;
            int col;
            if (j.state() == JobOpenPacket.CURRENT) {
                st = Component.translatable("rtshop.job.state_current").getString();
                col = Ui.C_GREEN;
            } else if (j.state() == JobOpenPacket.LOCKED) {
                st = Component.translatable("rtshop.job.state_locked").getString();
                col = Ui.C_RED;
            } else {
                st = Component.translatable("rtshop.job.state_open").getString();
                col = Ui.C_INK_SOFT;
            }
            g.drawString(this.font, Ui.clip(this.font, st, LIST_W - 28), listX() + 24, y + 13, col, false);
        }

        // 상세
        int x = rx();
        int w = rw();
        int top = listY();
        Ui.bevel(g, x, top, w, listH(), Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        JobOpenPacket.JobView j = find(selected);
        if (j != null) {
            g.pose().pushPose();
            g.pose().translate(x + 8, top + 6, 0);
            g.pose().scale(1.5F, 1.5F, 1.0F);
            g.renderItem(icon(j.icon()), 0, 0);
            g.pose().popPose();
            Ui.scaledText(g, this.font, j.name(), x + 36, top + 10, 1.4F, Ui.C_INK, false);
            int y = top + 36;
            g.drawString(this.font, Component.translatable("rtshop.job.passives"), x + 8, y, Ui.C_GOLD_DARK, false);
            y += 11;
            // '+' 장점(초록) / '-' 단점(빨강) / ' ' 참고
            for (String d : j.desc()) {
                if (d.isEmpty()) {
                    continue;
                }
                char kind = d.charAt(0);
                String text = (kind == '+' ? "▲ " : kind == '-' ? "▼ " : "• ") + d.substring(1);
                int col = kind == '+' ? Ui.C_GREEN : kind == '-' ? Ui.C_RED : Ui.C_INK;
                for (FormattedCharSequence line : this.font.split(Component.literal(text), w - 20)) {
                    if (y > btnY() - 22) {
                        break;
                    }
                    g.drawString(this.font, line, x + 10, y, col, false);
                    y += 10;
                }
                y += 1;
            }
            y += 6;
            List<Component> info = new ArrayList<>();
            int infoColor = Ui.C_INK_SOFT;
            if (j.state() == JobOpenPacket.CURRENT) {
                info.add(Component.translatable("rtshop.job.info_current"));
                info.add(Component.translatable("rtshop.job.info_resign"));
            } else if (j.state() == JobOpenPacket.LOCKED) {
                infoColor = Ui.C_RED;
                info.add(j.lockQuest().isEmpty() ? Component.translatable("rtshop.job.info_locked")
                        : Component.translatable("rtshop.job.info_locked_quest", j.lockQuest()));
            } else if (freePick(j)) {
                info.add(Component.translatable(last().isEmpty() ? "rtshop.job.info_free" : "rtshop.job.info_return_free"));
            } else {
                double h = COMBAT.equals(tab) ? data.hoursCombat : data.hoursLife;
                info.add(Component.translatable("rtshop.job.info_change", Ui.num(cost()), hours(h)));
                if (cooldown() > 0) {
                    infoColor = Ui.C_RED;
                    info.add(Component.translatable("rtshop.job.info_cooldown", duration(cooldown())));
                } else if (ClientEconomy.wallet < cost()) {
                    infoColor = Ui.C_RED;
                    info.add(Component.translatable("rtshop.job.info_no_money", Ui.num(ClientEconomy.wallet)));
                }
            }
            if (!data.atNpc) {
                info.add(Component.translatable("rtshop.job.info_need_npc"));
            }
            for (Component c : info) {
                for (FormattedCharSequence line : this.font.split(c, w - 16)) {
                    if (y > btnY() - 12) {
                        break;
                    }
                    g.drawString(this.font, line, x + 8, y, infoColor, false);
                    y += 10;
                }
            }
            boolean en = buttonEnabled(j);
            Ui.button(g, this.font, btnX(), btnY(), 112, 18, buttonLabel(j), Ui.inside(mx, my, btnX(), btnY(), 112, 18), en);
        }
        if (status != null) {
            List<FormattedCharSequence> lines = this.font.split(status, Math.max(40, btnX() - x - 12));
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.drawString(this.font, lines.get(i), x + 8, btnY() + 1 + i * 9, statusOk ? Ui.C_GREEN : Ui.C_RED, false);
            }
        }
        super.render(g, mx, my, partial);
        if (confirm) {
            renderConfirm(g, mx, my);
        }
    }

    // ------------------------------------------------------------------ 확인 창

    private int[] confirmBox() {
        int w = Math.min(W - 40, 260), h = 84;
        return new int[]{(this.width - w) / 2, (this.height - h) / 2, w, h};
    }

    private void renderConfirm(GuiGraphics g, int mx, int my) {
        JobOpenPacket.JobView j = find(selected);
        if (j == null) {
            confirm = false;
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, this.width, this.height, 0xA0000000);
        int[] b = confirmBox();
        Ui.woodFrame(g, b[0], b[1], b[2], b[3]);
        boolean resign = j.state() == JobOpenPacket.CURRENT;
        String title = Component.translatable(resign ? "rtshop.job.confirm_resign_title" : freePick(j) ? "rtshop.job.confirm_title" : "rtshop.job.confirm_change_title", j.name()).getString();
        g.drawCenteredString(this.font, Ui.clip(this.font, title, b[2] - 16), b[0] + b[2] / 2, b[1] + 10, Ui.C_GOLD);
        String body = resign ? Component.translatable("rtshop.job.confirm_resign_body", Ui.num(cost())).getString()
                : freePick(j) ? Component.translatable("rtshop.job.confirm_body").getString()
                : Component.translatable("rtshop.job.confirm_change_body", Ui.num(cost())).getString();
        List<net.minecraft.util.FormattedCharSequence> ls = this.font.split(Component.literal(body), b[2] - 20);
        for (int i = 0; i < Math.min(3, ls.size()); i++) {
            g.drawCenteredString(this.font, ls.get(i), b[0] + b[2] / 2, b[1] + 26 + i * 10, Ui.C_CREAM);
        }
        int bw = (b[2] - 30) / 2, by = b[1] + b[3] - 24;
        Ui.button(g, this.font, b[0] + 10, by, bw, 16, Component.translatable("rtshop.job.confirm_yes").getString(),
                Ui.inside(mx, my, b[0] + 10, by, bw, 16), true);
        Ui.button(g, this.font, b[0] + 20 + bw, by, bw, 16, Component.translatable("rtshop.job.confirm_no").getString(),
                Ui.inside(mx, my, b[0] + 20 + bw, by, bw, 16), true);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ 입력

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (confirm) {
            // 확인 창이 떠 있으면 그 단추만 받는다
            int[] b = confirmBox();
            int bw = (b[2] - 30) / 2, by = b[1] + b[3] - 24;
            JobOpenPacket.JobView j = find(selected);
            if (button == 0 && Ui.inside(mx, my, b[0] + 10, by, bw, 16) && j != null) {
                click();
                confirm = false;
                ShopNet.CHANNEL.sendToServer(new JobChoosePacket(j.state() == JobOpenPacket.CURRENT ? com.rtshop.job.Jobs.RESIGN + tab : j.id()));
            } else if (button != 0 || Ui.inside(mx, my, b[0] + 20 + bw, by, bw, 16) || !Ui.inside(mx, my, b[0], b[1], b[2], b[3])) {
                click();
                confirm = false;
            }
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        int tw = (W - 12 - 4) / 2;
        for (int i = 0; i < 2; i++) {
            int x = fx + 6 + i * (tw + 4);
            if (Ui.inside(mx, my, x, fy + 27, tw, 15)) {
                String t = i == 0 ? COMBAT : LIFE;
                if (!t.equals(tab)) {
                    tab = t;
                    confirm = false;
                    pickDefault();
                    click();
                }
                return true;
            }
        }
        List<JobOpenPacket.JobView> jobs = tabJobs();
        for (int i = 0; i < jobs.size(); i++) {
            int y = listY() + 2 + i * ROW;
            if (Ui.inside(mx, my, listX() + 2, y, LIST_W - 4, ROW - 2)) {
                selected = jobs.get(i).id();
                confirm = false;
                click();
                return true;
            }
        }
        JobOpenPacket.JobView j = find(selected);
        if (j != null && buttonEnabled(j) && Ui.inside(mx, my, btnX(), btnY(), 112, 18)) {
            click();
            confirm = true; // 고르기·바꾸기 모두 확인 창에서 한 번 더
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    // ------------------------------------------------------------------ 점검용

    public void selectForTest(String slot, String id) {
        tab = slot;
        selected = id;
        confirm = false;
    }

    public void pressForTest() {
        JobOpenPacket.JobView j = find(selected);
        if (j != null && j.state() != JobOpenPacket.CURRENT && buttonEnabled(j)) {
            if (!current().isEmpty() && !confirm) {
                confirm = true;
            }
            confirm = false;
            ShopNet.CHANNEL.sendToServer(new JobChoosePacket(j.id()));
        }
    }

    /** 점검용: 지금 직업을 내려놓는다 (확인 창의 [예] 와 같다). */
    public void resignForTest() {
        JobOpenPacket.JobView j = find(selected);
        if (j != null && j.state() == JobOpenPacket.CURRENT && buttonEnabled(j)) {
            confirm = false;
            ShopNet.CHANNEL.sendToServer(new JobChoosePacket(com.rtshop.job.Jobs.RESIGN + tab));
        }
    }

    public String buttonLabelForTest() {
        JobOpenPacket.JobView j = find(selected);
        return j == null ? "" : buttonLabel(j);
    }

    public String currentForTest(String slot) {
        return COMBAT.equals(slot) ? data.combat : data.life;
    }
}
