package com.rtshop.client;

import com.rtshop.RtShop;
import com.rtshop.net.ShopEditActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;

/**
 * 상인 화면(상점·은행·직업·뽑기·출석) 왼쪽 위의 단추:
 *  - [퀘스트 (N)]: 그 상인에게 받을 퀘스트가 있을 때. 누르면 퀘스트 창으로 (예전의 웅크리고 우클릭 대신).
 *  - [상점 편집]: OP 가 상점 화면에서 품목·가격을 바로 고친다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class NpcButtons {
    private NpcButtons() {
    }

    private static int npcId = -1;
    private static int quests;
    private static long setAt;
    private static WeakReference<Screen> bound = new WeakReference<>(null);

    public static void setQuests(int entityId, int count) {
        npcId = entityId;
        quests = count;
        setAt = System.currentTimeMillis();
        bound = new WeakReference<>(null);
    }

    private static boolean npcScreen(Screen s) {
        return s instanceof ShopScreen || s instanceof BankScreen || s instanceof JobScreen || s instanceof GachaScreen
                || s instanceof AttendanceScreen;
    }

    @SubscribeEvent
    public static void onOpen(ScreenEvent.Opening e) {
        if (npcScreen(e.getNewScreen()) && System.currentTimeMillis() - setAt < 4000 && bound.get() == null) {
            bound = new WeakReference<>(e.getNewScreen());
        }
    }

    private static boolean questButton(Screen s) {
        return quests > 0 && bound.get() == s;
    }

    private static boolean editButton(Screen s) {
        Minecraft mc = Minecraft.getInstance();
        return s instanceof ShopScreen && mc.player != null && mc.player.hasPermissions(2);
    }

    private static final int X = 6, Y = 6, W = 96, H = 18;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRender(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        if (!npcScreen(s)) return;
        GuiGraphics g = e.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int y = Y;
        g.pose().pushPose();
        g.pose().translate(0, 0, 450);
        if (questButton(s)) {
            boolean hov = Ui.inside(e.getMouseX(), e.getMouseY(), X, y, W, H);
            String label = Component.translatable("rtshop.npc.quest_button", quests).getString();
            Ui.bevel(g, X, y, W, H, hov ? Ui.C_GOLD : Ui.C_GOLD_DARK, Ui.C_GOLD, Ui.C_SHADOW);
            g.drawString(mc.font, label, X + (W - mc.font.width(label)) / 2, y + 5, Ui.C_INK, false);
            y += H + 4;
        }
        if (editButton(s)) {
            boolean hov = Ui.inside(e.getMouseX(), e.getMouseY(), X, y, W, H);
            Ui.button(g, mc.font, X, y, W, H, Component.translatable("rtshop.shopedit.button").getString(), hov, true);
        }
        g.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        Screen s = e.getScreen();
        if (!npcScreen(s) || e.getButton() != 0) return;
        Minecraft mc = Minecraft.getInstance();
        int y = Y;
        if (questButton(s)) {
            if (Ui.inside(e.getMouseX(), e.getMouseY(), X, y, W, H)) {
                e.setCanceled(true);
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                int id = npcId;
                s.onClose();
                mc.player.connection.sendCommand("rtshop quest open " + id);
                return;
            }
            y += H + 4;
        }
        if (editButton(s) && Ui.inside(e.getMouseX(), e.getMouseY(), X, y, W, H)) {
            e.setCanceled(true);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            String id = ((ShopScreen) s).shopIdForEdit();
            ShopNet.CHANNEL.sendToServer(new ShopEditActionPacket(ShopEditor.OPEN, id, ""));
        }
    }

    public static boolean questButtonForTest(Screen s) {
        return questButton(s);
    }
}
