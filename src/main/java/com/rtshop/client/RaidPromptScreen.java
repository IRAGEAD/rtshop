package com.rtshop.client;

import com.google.gson.JsonObject;
import com.rtshop.net.RaidActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.raid.Raid;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 레이드가 끝난 뒤: 성공이면 "바로 마을로 이동할까요?" 투표, 실패면 실패 문구와 이동 확인. */
public class RaidPromptScreen extends Screen {
    private JsonObject data;
    private long openedAt = System.currentTimeMillis();
    private final long shownAt = System.currentTimeMillis();

    public RaidPromptScreen(JsonObject data) {
        super(Component.literal("raid"));
        this.data = data;
    }

    public void update(JsonObject o) {
        if (!o.get("mode").getAsString().equals(data.get("mode").getAsString())) {
            openedAt = System.currentTimeMillis();
        }
        int left = secondsLeft();
        this.data = o;
        if (o.get("seconds").getAsInt() < left || left <= 0) {
            openedAt = System.currentTimeMillis();
        } else {
            // 남은 시간은 처음 받은 값에서 이어서 센다
            o.addProperty("seconds", left);
            openedAt = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean vote() {
        return "vote".equals(data.get("mode").getAsString());
    }

    private String t(String k) {
        JsonObject t = data.getAsJsonObject("texts");
        return t.has(k) ? t.get(k).getAsString() : k;
    }

    private int secondsLeft() {
        return (int) Math.max(0, data.get("seconds").getAsInt() - (System.currentTimeMillis() - openedAt) / 1000);
    }

    private int bw() {
        return 250;
    }

    private int bx() {
        return (this.width - bw()) / 2;
    }

    private int by() {
        return vote() ? this.height / 2 + 24 : this.height / 2 - 40;
    }

    private int[] agreeBtn() {
        return vote() ? new int[]{bx() + 14, by() + 50, 106, 18} : new int[]{bx() + 60, by() + 62, 130, 18};
    }

    private int[] stayBtn() {
        return new int[]{bx() + 130, by() + 50, 106, 18};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        int x = bx(), y = by(), w = bw(), h = vote() ? 78 : 92;
        boolean fail = !vote();
        float time = (System.currentTimeMillis() - shownAt) / 1000F;
        if (fail) {
            // 붉게 조여 오는 화면
            Fx2D.begin(g, false);
            Fx2D.rect(0, 0, this.width, this.height / 2F, 0xB0300000, 0xB0300000, 0x30100000, 0x30100000);
            Fx2D.rect(0, this.height / 2F, this.width, this.height, 0x30100000, 0x30100000, 0xB0300000, 0xB0300000);
            Fx2D.end();
        } else {
            Fx2D.begin(g, true);
            for (int i = 0; i < 12; i++) {
                Fx2D.ray(this.width / 2F, y + h / 2F, i * 30 + time * 14, 5, 190, Fx2D.rgba(0xFFE08A, 0.22F), Fx2D.rgba(0xFFE08A, 0));
            }
            Fx2D.end();
        }
        RaidFx.luxPanel(g, x, y, w, h, time, fail ? RaidFx.FIRE : 0xF0C060);
        RaidFx.embers(g, x, y, w, h, time, 14, !fail);
        if (fail) {
            RaidFx.luxTitle(g, this.font, t("failTitle"), this.width / 2, y + 10, 0xFF6A50, time);
            RaidFx.hairline(g, x + 20, x + w - 20, y + 30, 0xD4A85A, 0.9F);
            RaidText.center(g, this.font, t("failSub"), RaidText.SANS, this.width / 2, y + 36, 0xFFE0C2B4);
            RaidText.center(g, this.font, t("failBody") + " (" + secondsLeft() + ")", RaidText.SANS, this.width / 2, y + 49, 0xFFB8AF9C);
            int[] a = agreeBtn();
            RaidFx.luxButton(g, this.font, a[0], a[1], a[2], a[3], t("failConfirm"), true, Ui.inside(mx, my, a[0], a[1], a[2], a[3]), time, true);
            return;
        }
        boolean voted = data.get("voted").getAsBoolean();
        RaidText.engraved(g, this.font, t("voteTitle"), RaidText.SERIF, this.width / 2 - RaidText.w(this.font, t("voteTitle"), RaidText.SERIF) / 2, y + 9, 8);
        RaidFx.hairline(g, x + 20, x + w - 20, y + 21, 0xD4A85A, 0.9F);
        String count;
        try {
            count = String.format(t("voteCount"), data.get("agreed").getAsInt(), data.get("total").getAsInt());
        } catch (Exception e) {
            count = data.get("agreed").getAsInt() + " / " + data.get("total").getAsInt();
        }
        RaidText.center(g, this.font, count + "  ·  " + secondsLeft() + "s", RaidText.SANS, this.width / 2, y + 27, 0xFFF0D08A);
        int[] a = agreeBtn(), s = stayBtn();
        RaidFx.luxButton(g, this.font, a[0], a[1], a[2], a[3], voted ? "✔ " + t("voteAgree") : t("voteAgree"), !voted, Ui.inside(mx, my, a[0], a[1], a[2], a[3]), time, true);
        RaidFx.luxButton(g, this.font, s[0], s[1], s[2], s[3], t("voteStay"), true, Ui.inside(mx, my, s[0], s[1], s[2], s[3]), time, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int[] a = agreeBtn();
        if (Ui.inside(mx, my, a[0], a[1], a[2], a[3])) {
            agree();
            return true;
        }
        int[] s = stayBtn();
        if (vote() && Ui.inside(mx, my, s[0], s[1], s[2], s[3])) {
            RaidClient.voteDismissed = true;
            this.onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    public void agree() {
        if (vote()) {
            if (!data.get("voted").getAsBoolean()) {
                ShopNet.CHANNEL.sendToServer(new RaidActionPacket(Raid.ACT_VOTE, ""));
            }
        } else {
            ShopNet.CHANNEL.sendToServer(new RaidActionPacket(Raid.ACT_LEAVE, ""));
            this.onClose();
        }
    }

    @Override
    public void onClose() {
        if (vote()) {
            RaidClient.voteDismissed = true;
        }
        super.onClose();
    }

    public boolean voteForTest() {
        return vote();
    }

    public int agreedForTest() {
        return data.get("agreed").getAsInt();
    }
}
