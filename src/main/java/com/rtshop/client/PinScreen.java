package com.rtshop.client;

import com.rtshop.net.PinActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * 보관함 PIN 화면.
 *  - SET  : 토지 주인이 PIN 을 설정/변경/해제 (두 번 입력해서 확인)
 *  - ENTER: 잠긴 보관함을 열려는 사람이 PIN 을 입력
 * 숫자판을 눌러도 되고 키보드로 쳐도 된다. PIN 검사는 서버에서 한다.
 */
public class PinScreen extends Screen {
    public enum Mode {
        ENTER, SET
    }

    private static final int W = 150;
    private static final int H = 192;
    private static final int KEY_W = 30;
    private static final int KEY_H = 20;
    private static final int KEY_GAP = 4;
    private static final int MAX_LEN = 16;
    // 숫자판 배치: 1~9, 지우기, 0, 확인
    private static final String[] KEYS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "<", "0", "OK"};

    private final BlockPos pos;
    private final Mode mode;
    private final boolean hasPin;

    private String pin = "";
    private String first = null; // SET: 처음 입력한 값 (확인 입력을 기다리는 중)
    private Component error = null;
    private int fx, fy;

    public PinScreen(BlockPos pos, Mode mode, boolean hasPin) {
        super(Component.translatable(mode == Mode.SET ? "rtshop.pin.title_set" : "rtshop.pin.title_enter"));
        this.pos = pos;
        this.mode = mode;
        this.hasPin = hasPin;
    }

    @Override
    protected void init() {
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private int keyX(int i) {
        int total = KEY_W * 3 + KEY_GAP * 2;
        return fx + (W - total) / 2 + (i % 3) * (KEY_W + KEY_GAP);
    }

    private int keyY(int i) {
        return fy + 66 + (i / 3) * (KEY_H + KEY_GAP);
    }

    private int bottomY() {
        return fy + H - 26;
    }

    private boolean showRemove() {
        return mode == Mode.SET && hasPin;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = Ui.clip(this.font, this.title.getString(), W - 16);
        g.drawString(this.font, t, fx + (W - this.font.width(t)) / 2, fy + 9, Ui.C_GOLD, true);

        Component prompt;
        int color = Ui.C_CREAM;
        if (error != null) {
            prompt = error;
            color = 0xFFFF9C8C;
        } else if (mode == Mode.ENTER) {
            prompt = Component.translatable("rtshop.pin.prompt_enter");
        } else if (first == null) {
            prompt = Component.translatable(hasPin ? "rtshop.pin.prompt_change" : "rtshop.pin.prompt_new");
        } else {
            prompt = Component.translatable("rtshop.pin.prompt_again");
        }
        String ps = Ui.clip(this.font, prompt.getString(), W - 12);
        g.drawString(this.font, ps, fx + (W - this.font.width(ps)) / 2, fy + 29, color, false);

        // 입력 표시 (가려서 보여 줌)
        Ui.bevel(g, fx + 14, fy + 42, W - 28, 18, Ui.C_WOOD_DARK, Ui.C_SHADOW, Ui.C_WOOD_HI);
        String dots = "*".repeat(pin.length());
        if (dots.isEmpty()) {
            String hint = Ui.clip(this.font, Component.translatable("rtshop.pin.hint").getString(), W - 36);
            g.drawString(this.font, hint, fx + (W - this.font.width(hint)) / 2, fy + 47, 0xFF9C8460, false);
        } else {
            g.drawString(this.font, dots, fx + (W - this.font.width(dots)) / 2, fy + 48, Ui.C_GOLD, false);
        }

        for (int i = 0; i < KEYS.length; i++) {
            String label = KEYS[i];
            boolean enabled = true;
            if (label.equals("OK")) {
                label = Component.translatable("rtshop.pin.ok_button").getString();
                enabled = !pin.isEmpty();
            } else if (label.equals("<")) {
                label = Component.translatable("rtshop.pin.back").getString();
                enabled = !pin.isEmpty();
            }
            Ui.button(g, this.font, keyX(i), keyY(i), KEY_W, KEY_H, label,
                    enabled && Ui.inside(mx, my, keyX(i), keyY(i), KEY_W, KEY_H), enabled);
        }

        if (showRemove()) {
            Ui.button(g, this.font, fx + 10, bottomY(), 62, 18, Component.translatable("rtshop.pin.remove").getString(),
                    Ui.inside(mx, my, fx + 10, bottomY(), 62, 18), true);
            Ui.button(g, this.font, fx + W - 10 - 62, bottomY(), 62, 18, Component.translatable("rtshop.qty.cancel").getString(),
                    Ui.inside(mx, my, fx + W - 10 - 62, bottomY(), 62, 18), true);
        } else {
            Ui.button(g, this.font, fx + (W - 62) / 2, bottomY(), 62, 18, Component.translatable("rtshop.qty.cancel").getString(),
                    Ui.inside(mx, my, fx + (W - 62) / 2, bottomY(), 62, 18), true);
        }
    }

    private void type(char c) {
        if (c >= '0' && c <= '9' && pin.length() < MAX_LEN) {
            pin += c;
            error = null;
        }
    }

    private void backspace() {
        if (!pin.isEmpty()) {
            pin = pin.substring(0, pin.length() - 1);
        }
    }

    private void confirm() {
        if (pin.isEmpty()) {
            return;
        }
        if (mode == Mode.ENTER) {
            ShopNet.CHANNEL.sendToServer(new PinActionPacket(pos, PinActionPacket.TRY, pin));
            this.onClose();
            return;
        }
        if (first == null) {
            first = pin;
            pin = "";
            return;
        }
        if (!first.equals(pin)) {
            error = Component.translatable("rtshop.pin.mismatch");
            first = null;
            pin = "";
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
            return;
        }
        ShopNet.CHANNEL.sendToServer(new PinActionPacket(pos, PinActionPacket.SET, pin));
        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < KEYS.length; i++) {
            if (Ui.inside(mx, my, keyX(i), keyY(i), KEY_W, KEY_H)) {
                String k = KEYS[i];
                if (k.equals("OK")) {
                    if (!pin.isEmpty()) {
                        click();
                        confirm();
                    }
                } else if (k.equals("<")) {
                    if (!pin.isEmpty()) {
                        click();
                        backspace();
                    }
                } else {
                    click();
                    type(k.charAt(0));
                }
                return true;
            }
        }
        if (showRemove()) {
            if (Ui.inside(mx, my, fx + 10, bottomY(), 62, 18)) {
                click();
                ShopNet.CHANNEL.sendToServer(new PinActionPacket(pos, PinActionPacket.REMOVE, ""));
                this.onClose();
                return true;
            }
            if (Ui.inside(mx, my, fx + W - 10 - 62, bottomY(), 62, 18)) {
                click();
                this.onClose();
                return true;
            }
        } else if (Ui.inside(mx, my, fx + (W - 62) / 2, bottomY(), 62, 18)) {
            click();
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        type(c);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 259) { // Backspace
            backspace();
            return true;
        }
        if (key == 257 || key == 335) { // Enter
            confirm();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
