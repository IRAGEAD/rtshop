package com.rtshop.client;

import com.rtshop.RtShop;
import com.rtshop.farm.FarmConfig;
import com.rtshop.farm.SeedRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** 클라이언트 전용 게임 이벤트: 보관함의 PIN 버튼, 아이템 설명(별 등급·씨앗), 단축키. */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    // ------------------------------------------------------------------ 보관함 PIN 버튼

    /**
     * 보관함 화면 위쪽에 붙는 "PIN 설정" 버튼.
     * 서버가 "이 보관함은 네 토지 안에 있다"고 알려 준 경우에만 보이고 눌린다.
     */
    private static final class PinButton extends AbstractWidget {
        private static final int BW = 62;
        private static final int BH = 14;
        private final AbstractContainerScreen<?> screen;

        PinButton(AbstractContainerScreen<?> screen) {
            super(0, 0, BW, BH, Component.empty());
            this.screen = screen;
        }

        private boolean shown() {
            return ClientLand.canSetPin && ClientLand.containerPos != null && ClientLand.containerId >= 0
                    && screen.getMenu() != null && screen.getMenu().containerId == ClientLand.containerId;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            if (!shown()) {
                return;
            }
            int x = screen.getGuiLeft() + screen.getXSize() - BW;
            int y = Math.max(0, screen.getGuiTop() - BH - 2);
            this.setX(x);
            this.setY(y);
            boolean hover = Ui.inside(mx, my, x, y, BW, BH);
            String label = Component.translatable(ClientLand.hasPin ? "rtshop.pin.button_change" : "rtshop.pin.button_set").getString();
            int fill = hover ? Ui.C_WOOD_HI : Ui.C_WOOD;
            Ui.bevel(g, x, y, BW, BH, fill, ClientLand.hasPin ? Ui.C_GOLD : Ui.C_WOOD_HI, Ui.C_SHADOW);
            Minecraft mc = Minecraft.getInstance();
            String s = Ui.clip(mc.font, label, BW - 4);
            g.drawString(mc.font, s, x + (BW - mc.font.width(s)) / 2, y + 3, ClientLand.hasPin ? Ui.C_GOLD : Ui.C_CREAM, false);
        }

        @Override
        protected boolean clicked(double mx, double my) {
            return shown() && super.clicked(mx, my);
        }

        @Override
        public boolean isMouseOver(double mx, double my) {
            return shown() && super.isMouseOver(mx, my);
        }

        @Override
        public void onClick(double mx, double my) {
            BlockPos pos = ClientLand.containerPos;
            boolean has = ClientLand.hasPin;
            Minecraft mc = Minecraft.getInstance();
            if (pos == null || mc.player == null) {
                return;
            }
            mc.player.closeContainer();
            ClientLand.clear();
            mc.setScreen(new PinScreen(pos, PinScreen.Mode.SET, has));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> acs) {
            event.addListener(new PinButton(acs));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLand.clear();
        ClientEconomy.wallet = 0;
        ClientEconomy.bank = 0;
    }

    // ------------------------------------------------------------------ 아이템 설명

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        try {
            ItemStack s = event.getItemStack();
            if (s.isEmpty()) {
                return;
            }
            List<Component> tips = event.getToolTip();
            int at = Math.min(1, tips.size());
            int star = SeedRules.star(s);
            if (star > 0) {
                tips.add(at++, Component.translatable("rtshop.tip.star", SeedRules.starText(star)).withStyle(ChatFormatting.YELLOW));
            }
            if (SeedRules.isLicensed(s)) {
                tips.add(at, Component.translatable(SeedRules.isDual(s) ? "rtshop.tip.seed_dual" : "rtshop.tip.seed")
                        .withStyle(ChatFormatting.GREEN));
            } else if (FarmConfig.get().requireShopSeeds && SeedRules.isPlantable(s)) {
                tips.add(at, Component.translatable(SeedRules.isDual(s) ? "rtshop.tip.not_seed_dual" : "rtshop.tip.not_seed")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } catch (Throwable ignored) {
            // 설명 한 줄 때문에 게임이 멈추면 안 된다
        }
    }

    // ------------------------------------------------------------------ 단축키

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.HUD_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new HudEditScreen());
            }
        }
        while (ClientModEvents.LAND_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.player.connection != null) {
                mc.player.connection.sendCommand("rtshop land");
            }
        }
        while (ClientModEvents.JOB_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.player.connection != null) {
                mc.player.connection.sendCommand("rtshop job");
            }
        }
        while (ClientModEvents.QUEST_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.player.connection != null) {
                mc.player.connection.sendCommand("rtshop quest");
            }
        }
    }
}
