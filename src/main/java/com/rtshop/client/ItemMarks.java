package com.rtshop.client;

import com.rtshop.farm.SeedRules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * 아이템 아이콘 위에 그리는 작은 표시.
 *  - 별 등급이 붙은 수확물: 왼쪽 위에 노란 별 1~3개
 *  - 상점 씨앗(심기 전용): 오른쪽 위에 초록 점
 * 인벤토리·핫바·상자 어디서든 보이므로, 모양이 같은 "수확한 당근"과 "심기 전용 당근"을 한눈에 구분할 수 있다.
 */
public final class ItemMarks {
    private ItemMarks() {
    }

    private static final int STAR = 0xFFFFE14A;
    private static final int STAR_EDGE = 0xFF6B4300;
    private static final int SEED = 0xFF6BD15A;
    private static final int SEED_EDGE = 0xFF17471A;

    /** Forge 의 IItemDecorator 로 등록된다. 무언가 그렸으면 true. */
    public static boolean render(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        int star = Math.min(3, SeedRules.star(stack));
        boolean seed = SeedRules.isLicensed(stack);
        if (star <= 0 && !seed) {
            return false;
        }
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 200.0F); // 아이템 그림보다 앞에
        for (int i = 0; i < star; i++) {
            int sx = x + i * 4;
            g.fill(sx, y, sx + 3, y + 3, STAR_EDGE);
            g.fill(sx + 1, y, sx + 2, y + 3, STAR);
            g.fill(sx, y + 1, sx + 3, y + 2, STAR);
        }
        if (seed) {
            g.fill(x + 12, y, x + 16, y + 4, SEED_EDGE);
            g.fill(x + 13, y + 1, x + 15, y + 3, SEED);
        }
        g.pose().popPose();
        return true;
    }
}
