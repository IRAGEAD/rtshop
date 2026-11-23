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
            String s = Ui.clip(UiFont.get(), label, BW - 4);
            g.drawString(UiFont.get(), s, x + (BW - UiFont.get().width(s)) / 2, y + 3, ClientLand.hasPin ? Ui.C_GOLD : Ui.C_CREAM, false);
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

    /** Spiral Abyss 층 선택 화면: 열릴 때 주간 보상 정보를 받아 오고, 화면 왼쪽 아래에 그린다. */
    @SubscribeEvent
    public static void onAbyssScreenInit(ScreenEvent.Init.Post event) {
        if (AbyssPanel.isFloorScreen(event.getScreen())) {
            try {
                AbyssPanel.request();
            } catch (Throwable ignored) {
            }
        }
    }

    /** 층 선택 화면의 주간 보상 카드에서 [자세히 보기] 를 누르면 상세 창을 연다. */
    @SubscribeEvent
    public static void onAbyssScreenClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (AbyssPanel.isFloorScreen(event.getScreen()) && event.getButton() == 0) {
            try {
                if (AbyssPanel.click(event.getScreen(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
            } catch (Throwable ignored) {
            }
        }
    }

    @SubscribeEvent
    public static void onAbyssScreenRender(ScreenEvent.Render.Post event) {
        if (AbyssPanel.isFloorScreen(event.getScreen())) {
            try {
                AbyssPanel.render(event.getGuiGraphics(), event.getScreen().width, event.getScreen().height, event.getMouseX(), event.getMouseY());
            } catch (Throwable ignored) {
            }
        }
    }

    /** Fantasy Mounts 퀘스트 대화창이 저절로 뜨지 않게 한다 (탈것을 처음 볼 때마다 뜨던 것). 설정에서 켤 수 있다. */
    @SubscribeEvent
    public static void onFantasyQuestPopup(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && !ClientConfig.get().fantasyQuestPopups
                && event.getNewScreen().getClass().getName().equals("com.bonfyre.fantasy.client.gui.QuestDialogueScreen")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> acs) {
            event.addListener(new PinButton(acs));
        }
        // 인벤토리 위의 [퀘스트 수첩]: 어디서든 퀘스트 내용을 본다 (받기·완료·포기는 NPC 앞에서)
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inv) {
            String label = net.minecraft.network.chat.Component.translatable("rtshop.quest.journal_button").getString();
            int w = UiFont.get().width(label) + 10;
            event.addListener(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal(label), b -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.connection.sendCommand("rtshop quest");
                }
            }).bounds(inv.getGuiLeft() + inv.getXSize() - w, inv.getGuiTop() - 16, w, 14).build());
        }
    }

    /** 접속하면 내 스토리 언어를 서버에 알린다. */
    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        HudLang.sendStoryLang();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLand.clear();
        ClientQuestHud.clear();
        SpotClient.clear();
        PvpHud.clear();
        RaidClient.abyss = null;
        ClientEconomy.wallet = 0;
        ClientEconomy.bank = 0;
    }

    // ------------------------------------------------------------------ 이야기 연출: 화면 흔들림, 보스 아이템 이야기

    /** 연출이 도는 동안에는 채팅·체력·핫바 같은 화면 요소를 숨긴다 (영화처럼). */
    /** 연출 중(이야기 장면·뽑기·강화 대성공 장면)에는 화면의 모든 표시(퀘스트 HUD, 다른 모드 표시 포함)를 잠시 끈다. */
    public static boolean hideHud() {
        Minecraft mc = Minecraft.getInstance();
        return Cinematic.active() || EnhanceGrandFx.running()
                || (mc.screen instanceof GachaScreen gs && gs.fxActiveForTest())
                || (mc.screen instanceof MagicGachaScreen ms && ms.fxActiveForTest()) || MagicClient.approachingForTest();
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onGuiPre(net.minecraftforge.client.event.RenderGuiEvent.Pre event) {
        if (!hideHud()) return;
        // 화면 표시 전체를 건너뛰되, 이야기 장면만은 직접 그린다
        int sw = event.getWindow().getGuiScaledWidth(), sh = event.getWindow().getGuiScaledHeight();
        if (Cinematic.active()) Cinematic.render(event.getGuiGraphics(), sw, sh);
        if (EnhanceFx.active() && Minecraft.getInstance().screen == null) EnhanceFx.render(event.getGuiGraphics(), sw, sh);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onOverlayPre(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        if (!Cinematic.active()) return;
        net.minecraft.resources.ResourceLocation id = event.getOverlay().id();
        if (id.getNamespace().equals("rtshop") && id.getPath().equals("cinematic")) return;
        if (id.getPath().equals("vignette") || id.getPath().equals("frostbite") || id.getPath().equals("helmet")) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent event) {
        if (Cinematic.active()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        float[] s = Cinematic.shake();
        if (s[0] != 0 || s[1] != 0) {
            event.setYaw(event.getYaw() + s[0]);
            event.setPitch(event.getPitch() + s[1]);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void onLoreTooltip(ItemTooltipEvent event) {
        try {
            // 무기 분류 (전투 직업의 무기 상성 기준)
            if (ClientConfig.get().weaponTypeTooltip) {
                com.rtshop.job.JobConfig jc = com.rtshop.job.JobConfig.get();
                String type = jc.weaponType(event.getItemStack());
                if (!type.equals(com.rtshop.job.WeaponTypes.NONE) && !type.equals(com.rtshop.job.WeaponTypes.FIST)) {
                    event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                            net.minecraft.network.chat.Component.translatable("rtshop.job.weapon_type_line", jc.typeName(type))
                                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            StoryClient.onTooltip(event);
        } catch (Throwable ignored) {
        }
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
            if (com.rtshop.gacha.Gacha.marked(s)) {
                String g = com.rtshop.gacha.Gacha.gradeOf(s);
                tips.add(at++, Component.translatable("rtshop.gacha.tip_mark", Component.translatable("rtshop.gacha.grade." + g))
                        .withStyle(st -> st.withColor(GachaColors.of(g))));
                tips.add(at++, Component.translatable("rtshop.gacha.tip_nosell").withStyle(ChatFormatting.DARK_GRAY));
            }
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
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) MagicClient.frame(event.renderTickTime);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) MagicClient.tick();
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
        while (ClientModEvents.PVP_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.player.connection != null) {
                mc.player.connection.sendCommand("pvp");
            }
        }
        while (ClientModEvents.QUEST_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.player.connection != null) {
                mc.player.connection.sendCommand("rtshop quest");
            }
        }
    }
}
