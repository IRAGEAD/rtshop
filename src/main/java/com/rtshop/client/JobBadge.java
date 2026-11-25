package com.rtshop.client;

import com.rtshop.RtShop;
import com.rtshop.job.ClientJobCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 가방 창 왼쪽의 [직업] 딱지: 마우스를 올리면 지금 전투·생활 직업과 그 패시브(▲ 버프 ▼ 디버프)가 보이고,
 * 누르면 직업 화면이 열린다 (바꾸는 것은 직업 안내인에게서만). 직업 정보는 서버가 JobSyncPacket 으로 보내 준다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class JobBadge {
    private JobBadge() {
    }

    public static final int W = 34, H = 16;

    /** 딱지의 자리 {x, y}: 가방 창 왼쪽 바깥, 위에서 조금 아래. */
    static int[] spot(InventoryScreen s) {
        return new int[]{s.getGuiLeft() - W - 3, s.getGuiTop() + 4};
    }

    /** 마우스를 올렸을 때 보이는 줄들. */
    public static List<Component> lines() {
        List<Component> out = new ArrayList<>();
        section(out, "rtshop.job.badge.combat", ClientJobCache.combat, ClientJobCache.combatDesc);
        out.add(Component.empty());
        section(out, "rtshop.job.badge.life", ClientJobCache.life, ClientJobCache.lifeDesc);
        out.add(Component.empty());
        out.add(Component.translatable("rtshop.job.badge.open").withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    private static void section(List<Component> out, String key, String job, List<String> desc) {
        boolean none = job == null || job.isEmpty();
        out.add(Component.translatable(key, none ? Component.translatable("rtshop.job.none").getString() : job)
                .withStyle(none ? ChatFormatting.GRAY : ChatFormatting.GOLD));
        if (none) return;
        for (String d : desc) {
            if (d.isEmpty()) continue;
            char c = d.charAt(0);
            String text = d.substring(1);
            if (c == '+') out.add(Component.literal("▲ " + text).withStyle(ChatFormatting.GREEN));
            else if (c == '-') out.add(Component.literal("▼ " + text).withStyle(ChatFormatting.RED));
            else out.add(Component.literal(text).withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean over(InventoryScreen s, double mx, double my) {
        int[] p = spot(s);
        return mx >= p[0] && mx < p[0] + W && my >= p[1] && my < p[1] + H;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof InventoryScreen s)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) return;
        GuiGraphics g = e.getGuiGraphics();
        int[] p = spot(s);
        if (p[0] < 2) return; // 창이 화면 왼쪽 끝에 붙어 있으면(제작법 책이 열린 작은 화면) 그리지 않는다
        boolean hov = over(s, e.getMouseX(), e.getMouseY());
        boolean was = UiText.flat;
        UiText.flat = true;
        try {
            Ui.button(g, UiFont.get(), p[0], p[1], W, H, Component.translatable("rtshop.job.badge").getString(), hov, true);
        } finally {
            UiText.flat = was;
        }
        if (hov && mc.player.containerMenu.getCarried().isEmpty()) {
            g.renderComponentTooltip(mc.font, lines(), e.getMouseX(), e.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!(e.getScreen() instanceof InventoryScreen s) || e.getButton() != 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator() || spot(s)[0] < 2 || !over(s, e.getMouseX(), e.getMouseY())) return;
        if (!mc.player.containerMenu.getCarried().isEmpty()) return;
        e.setCanceled(true);
        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
        mc.player.connection.sendCommand("rtshop job");
    }
}
