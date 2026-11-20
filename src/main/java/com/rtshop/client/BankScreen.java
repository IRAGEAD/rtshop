package com.rtshop.client;

import com.rtshop.net.BankPacket;
import com.rtshop.net.ClosePacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** 은행 화면: 이자 없는 단순 보관. 은행 NPC 를 우클릭했을 때만 열립니다. */
public class BankScreen extends Screen {
    private static final int W = 236;
    private static final int H = 176;
    private static final long[] QUICK = {100, 1_000, 10_000};

    private final double lossMin;
    private final double lossMax;

    private EditBox amountBox;
    private int fx, fy;
    private Component status = null;
    private boolean statusOk = true;
    private int statusTicks = 0;

    public BankScreen(double lossMin, double lossMax) {
        super(Component.translatable("rtshop.bank.title"));
        this.lossMin = lossMin;
        this.lossMax = lossMax;
    }

    @Override
    protected void init() {
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        String old = amountBox != null ? amountBox.getValue() : "";
        amountBox = new EditBox(this.font, fx + 16, fy + 85, W - 32, 10, Component.translatable("rtshop.bank.amount"));
        amountBox.setBordered(false);
        amountBox.setMaxLength(12);
        amountBox.setTextColor(0xFFF5E6C0);
        amountBox.setFilter(s -> s.matches("\\d*"));
        amountBox.setValue(old);
        this.addRenderableWidget(amountBox);
        this.setInitialFocus(amountBox);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        ShopNet.CHANNEL.sendToServer(new ClosePacket());
    }

    @Override
    public void tick() {
        super.tick();
        if (amountBox != null) {
            amountBox.tick();
        }
        if (statusTicks > 0 && --statusTicks == 0) {
            status = null;
        }
    }

    public void showResult(Component message, boolean ok) {
        this.status = message;
        this.statusOk = ok;
        this.statusTicks = 80;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                ok ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, 1.0F));
    }

    private long amount() {
        try {
            return amountBox.getValue().isEmpty() ? 0 : Long.parseLong(amountBox.getValue());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    // 버튼 위치
    private int quickX(int i) {
        return fx + 12 + i * 54;
    }

    private int quickY() {
        return fy + 102;
    }

    private int btnY() {
        return fy + 122;
    }

    private int btnX(int i) {
        return fx + 12 + i * 54;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);

        // 제목
        g.fill(fx + 5, fy + 4, fx + W - 5, fy + 23, Ui.C_WOOD_DARK);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        Ui.coin(g, fx + 9, fy + 5, 16);
        g.drawString(this.font, this.title, fx + 29, fy + 9, Ui.C_GOLD, true);

        // 잔액 패널
        Ui.bevel(g, fx + 8, fy + 28, W - 16, 44, Ui.C_PARCH, Ui.C_PARCH_SHADE, Ui.C_SHADOW);
        drawBalance(g, fy + 34, Component.translatable("rtshop.bank.wallet").getString(), ClientEconomy.wallet, Ui.C_RED);
        drawBalance(g, fy + 54, Component.translatable("rtshop.bank.saved").getString(), ClientEconomy.bank, Ui.C_GREEN);
        g.fill(fx + 14, fy + 49, fx + W - 14, fy + 50, Ui.C_PARCH_SHADE);

        // 금액 입력
        g.drawString(this.font, Component.translatable("rtshop.bank.amount"), fx + 12, fy + 73, Ui.C_CREAM, false);
        Ui.bevel(g, fx + 11, fy + 81, W - 22, 18, Ui.C_WOOD_DARK, Ui.C_SHADOW, Ui.C_WOOD_HI);

        for (int i = 0; i < QUICK.length; i++) {
            Ui.button(g, this.font, quickX(i), quickY(), 50, 16, "+" + Ui.num(QUICK[i]),
                    Ui.inside(mx, my, quickX(i), quickY(), 50, 16), true);
        }
        Ui.button(g, this.font, quickX(3), quickY(), 50, 16, Component.translatable("rtshop.bank.clear").getString(),
                Ui.inside(mx, my, quickX(3), quickY(), 50, 16), true);

        String[] labels = {"rtshop.bank.deposit", "rtshop.bank.withdraw", "rtshop.bank.deposit_all", "rtshop.bank.withdraw_all"};
        for (int i = 0; i < 4; i++) {
            Ui.button(g, this.font, btnX(i), btnY(), 50, 18, Component.translatable(labels[i]).getString(),
                    Ui.inside(mx, my, btnX(i), btnY(), 50, 18), true);
        }

        // 안내/결과
        Ui.bevel(g, fx + 6, fy + H - 30, W - 12, 24, Ui.C_WOOD_DARK, Ui.C_SHADOW, Ui.C_WOOD_HI);
        if (status != null) {
            g.drawString(this.font, Ui.clip(this.font, status.getString(), W - 24), fx + 12, fy + H - 22,
                    statusOk ? 0xFFB8E08A : 0xFFFF9C8C, false);
        } else {
            String l1 = Component.translatable("rtshop.bank.info1", Ui.num(lossMin), Ui.num(lossMax)).getString();
            String l2 = Component.translatable("rtshop.bank.info2").getString();
            g.drawString(this.font, Ui.clip(this.font, l1, W - 24), fx + 12, fy + H - 27, 0xFFBFA472, false);
            g.drawString(this.font, Ui.clip(this.font, l2, W - 24), fx + 12, fy + H - 17, 0xFFBFA472, false);
        }

        super.render(g, mx, my, partial);
    }

    private void drawBalance(GuiGraphics g, int y, String label, long value, int labelColor) {
        g.drawString(this.font, label, fx + 16, y + 1, labelColor, false);
        String v = Ui.num(value) + " G";
        int vx = fx + W - 16 - this.font.width(v);
        g.drawString(this.font, v, vx, y + 1, Ui.C_INK, false);
        Ui.coin(g, vx - 12, y, 10);
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void send(boolean deposit, long amount) {
        if (amount <= 0) {
            showResult(Component.translatable("rtshop.msg.bank_bad_amount"), false);
            return;
        }
        ShopNet.CHANNEL.sendToServer(new BankPacket(deposit, amount));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < QUICK.length; i++) {
                if (Ui.inside(mx, my, quickX(i), quickY(), 50, 16)) {
                    click();
                    amountBox.setValue(Long.toString(Math.min(999_999_999_999L, amount() + QUICK[i])));
                    return true;
                }
            }
            if (Ui.inside(mx, my, quickX(3), quickY(), 50, 16)) {
                click();
                amountBox.setValue("");
                return true;
            }
            for (int i = 0; i < 4; i++) {
                if (Ui.inside(mx, my, btnX(i), btnY(), 50, 18)) {
                    click();
                    switch (i) {
                        case 0 -> send(true, amount());
                        case 1 -> send(false, amount());
                        case 2 -> send(true, ClientEconomy.wallet);
                        default -> send(false, ClientEconomy.bank);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
