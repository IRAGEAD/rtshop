package com.rtshop.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** 점검용: 글꼴마다 글자가 줄 안 어디에 찍히는지 재는 화면 (검은 바탕에 흰 글자, 20칸 간격). */
public class FontCalibScreen extends Screen {
    public static final String[] IDS = {"", "rtshop:ui_sans_s", "rtshop:ui_sans", "rtshop:ui_sans_l",
            "rtshop:ui_serif_s", "rtshop:ui_serif", "rtshop:ui_serif_l"};

    public FontCalibScreen() {
        super(Component.literal("font calib"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0xFF000000);
        var mcFont = net.minecraft.client.Minecraft.getInstance().font;
        for (int i = 0; i < IDS.length; i++) {
            int y = 10 + i * 20;
            g.fill(4, y, 8, y + 9, 0xFFFF0000); // 줄의 위치 표시 (9칸)
            var c = Component.literal("가나다맡기 Ag 「습격의 징조」는 「조사대원」 · 『책』 【끝】…");
            if (!IDS[i].isEmpty()) {
                ResourceLocation rl = new ResourceLocation(IDS[i]);
                c = c.withStyle(s -> s.withFont(rl));
            }
            g.drawString(mcFont, c, 20, y, 0xFFFFFFFF, false);
        }
    }
}
