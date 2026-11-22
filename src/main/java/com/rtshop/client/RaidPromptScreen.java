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
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
        g.fill(x, y, x + w, y + h, fail ? 0xF01C0606 : 0xF0141008);
        g.fill(x, y, x + w, y + 2, fail ? 0xFFB3261E : 0xFFE8B84A);
        if (fail) {
            g.pose().pushPose();
            g.pose().translate(this.width / 2F, y + 10, 0);
            g.pose().scale(2F, 2F, 1F);
            g.drawCenteredString(this.font, t("failTitle"), 0, 0, 0xFFFF5040);
            g.pose().popPose();
            g.drawCenteredString(this.font, t("failSub"), this.width / 2, y + 32, 0xFFD9B0A8);
            g.drawCenteredString(this.font, t("failBody") + " (" + secondsLeft() + ")", this.width / 2, y + 46, 0xFFB8AF9C);
            int[] a = agreeBtn();
            Ui.button(g, this.font, a[0], a[1], a[2], a[3], t("failConfirm"), Ui.inside(mx, my, a[0], a[1], a[2], a[3]), true);
            return;
        }
        boolean voted = data.get("voted").getAsBoolean();
        g.drawCenteredString(this.font, t("voteTitle"), this.width / 2, y + 8, 0xFFF5E6C0);
        String count;
        try {
            count = String.format(t("voteCount"), data.get("agreed").getAsInt(), data.get("total").getAsInt());
        } catch (Exception e) {
            count = data.get("agreed").getAsInt() + " / " + data.get("total").getAsInt();
        }
        g.drawCenteredString(this.font, count + "  ·  " + secondsLeft() + "s", this.width / 2, y + 24, 0xFFE8B84A);
        int[] a = agreeBtn(), s = stayBtn();
        Ui.button(g, this.font, a[0], a[1], a[2], a[3], voted ? "✔ " + t("voteAgree") : t("voteAgree"), Ui.inside(mx, my, a[0], a[1], a[2], a[3]), !voted);
        Ui.button(g, this.font, s[0], s[1], s[2], s[3], t("voteStay"), Ui.inside(mx, my, s[0], s[1], s[2], s[3]), true);
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
