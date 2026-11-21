package com.rtshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * 편집기에서 여러 선택지 중 하나를 고르는 팝업 목록.
 * 버튼을 누르면 그 아래(자리가 없으면 위)에 선택지가 한꺼번에 펼쳐지고, 하나를 누르면 고르고 닫힌다.
 * 목록 밖을 누르거나 Esc 를 누르면 그냥 닫힌다. 선택지가 많으면 마우스 휠로 넘긴다.
 */
public final class ChoicePopup {
    /** 선택지 하나: 값, 보일 글자, (있으면) 아이콘. */
    public record Option(String value, String label, ItemStack icon) {
        public Option(String value, String label) {
            this(value, label, ItemStack.EMPTY);
        }
    }

    private static final int ROW = 12;

    private final List<Option> options;
    private final String current;
    private final Consumer<String> onPick;
    private final int x, w, rows;
    private int y;
    private int scroll = 0;

    public ChoicePopup(int anchorX, int anchorY, int anchorW, int anchorH, int screenH, Font font, List<Option> options, String current,
                       Consumer<String> onPick) {
        this.options = options;
        this.current = current;
        this.onPick = onPick;
        int widest = anchorW;
        for (Option o : options) {
            widest = Math.max(widest, font.width(o.label()) + (o.icon().isEmpty() ? 14 : 26));
        }
        int screenW = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        this.w = Math.min(widest, Math.max(anchorW, screenW - 8));
        int maxRows = Math.max(3, Math.min(options.size(), (screenH - 16) / ROW / 2));
        this.rows = Math.min(options.size(), Math.max(3, Math.min(14, maxRows)));
        int h = rows * ROW + 4;
        int below = anchorY + anchorH;
        this.y = below + h <= screenH - 4 ? below : Math.max(4, anchorY - h);
        // 화면 오른쪽 밖으로 나가지 않게 왼쪽으로 민다
        this.x = Math.max(4, Math.min(anchorX, screenW - 4 - this.w));
        int idx = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value().equals(current)) {
                idx = i;
            }
        }
        if (idx >= rows) {
            scroll = Math.min(options.size() - rows, idx - rows / 2);
        }
    }

    private int height() {
        return rows * ROW + 4;
    }

    public void render(GuiGraphics g, Font font, int mx, int my) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x - 1, y - 1, x + w + 1, y + height() + 1, Ui.C_SHADOW);
        g.fill(x, y, x + w, y + height(), Ui.C_PARCH_HI);
        for (int r = 0; r < rows; r++) {
            int i = scroll + r;
            if (i >= options.size()) {
                break;
            }
            Option o = options.get(i);
            int ry = y + 2 + r * ROW;
            boolean hover = Ui.inside(mx, my, x, ry, w, ROW);
            boolean cur = o.value().equals(current);
            if (hover) {
                g.fill(x + 1, ry, x + w - 1, ry + ROW, 0x66A87A22);
            } else if (cur) {
                g.fill(x + 1, ry, x + w - 1, ry + ROW, 0x33A87A22);
            }
            int tx = x + 4;
            if (!o.icon().isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x + 2, ry, 0);
                g.pose().scale(0.75F, 0.75F, 1.0F);
                g.renderItem(o.icon(), 0, 0);
                g.pose().popPose();
                tx = x + 16;
            }
            g.drawString(font, Ui.clip(font, (cur ? "✔ " : "") + o.label(), x + w - tx - 4), tx, ry + 2, cur ? Ui.C_GREEN : Ui.C_INK, false);
        }
        if (options.size() > rows) {
            int total = options.size();
            int barH = Math.max(8, height() * rows / total);
            int barY = y + (height() - barH) * scroll / Math.max(1, total - rows);
            g.fill(x + w - 3, barY, x + w - 1, barY + barH, Ui.C_WOOD_HI);
        }
        g.pose().popPose();
    }

    /** 클릭을 처리한다. 팝업이 닫혀야 하면 true. */
    public boolean click(double mx, double my) {
        if (Ui.inside(mx, my, x, y, w, height())) {
            int r = (int) ((my - y - 2) / ROW);
            int i = scroll + r;
            if (r >= 0 && r < rows && i < options.size()) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                onPick.accept(options.get(i).value());
                return true;
            }
            return false;
        }
        return true; // 바깥을 누르면 닫기
    }

    public void scroll(double delta) {
        scroll = Math.max(0, Math.min(Math.max(0, options.size() - rows), scroll - (int) Math.signum(delta)));
    }

    /** 점검용: 이 값을 고른다. */
    public void pickForTest(String value) {
        onPick.accept(value);
    }

    public int sizeForTest() {
        return options.size();
    }
}
