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

    private static boolean anyQuests;
    /** [퀘스트] 단추로 퀘스트 창을 열었을 때: 돌아갈 상인(엔티티 번호)과 누른 시각. 퀘스트 창이 이것으로 [← 돌아가기] 단추를 만든다. */
    static int returnNpc = -1;
    static long returnArmedAt;

    public static void setQuests(int entityId, int count, boolean any) {
        npcId = entityId;
        quests = count;
        anyQuests = any || count > 0;
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
        return anyQuests && bound.get() == s;
    }

    private static boolean editButton(Screen s) {
        Minecraft mc = Minecraft.getInstance();
        return s instanceof ShopScreen && mc.player != null && mc.player.hasPermissions(2);
    }

    /** 상인 화면이 제목 줄에서 단추를 놓을 자리(제목 글자 바로 오른쪽)를 알려 준다. */
    public interface Host {
        /** {x, y}: 단추 줄의 왼쪽 위. 높이는 {@link #H}. */
        int[] npcButtonSpot();
    }

    public static final int H = 14;

    private static String questLabel() {
        return quests > 0 ? Component.translatable("rtshop.npc.quest_button", quests).getString()
                : Component.translatable("rtshop.npc.quest_button_none").getString();
    }

    private static String talkLabel() {
        return Component.translatable("rtshop.talk.button").getString();
    }

    private static String editLabel() {
        return Component.translatable("rtshop.shopedit.button").getString();
    }

    /** 단추들: {x, y, w, 종류(0 퀘스트, 1 편집)}. */
    private static java.util.List<int[]> layout(Screen s) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        int[] spot = s instanceof Host h ? h.npcButtonSpot() : new int[]{6, 6};
        int x = spot[0];
        if (bound.get() == s) {
            int w = mc.font.width(talkLabel()) + 12;
            out.add(new int[]{x, spot[1], w, 2});
            x += w + 4;
        }
        if (questButton(s)) {
            int w = mc.font.width(questLabel()) + 12;
            out.add(new int[]{x, spot[1], w, 0});
            x += w + 4;
        }
        if (editButton(s)) {
            int w = mc.font.width(editLabel()) + 12;
            out.add(new int[]{x, spot[1], w, 1});
        }
        return out;
    }

    /** 제목 줄에서 단추들이 차지하는 너비 (오른쪽 글자를 그만큼 줄이는 데 쓴다). */
    public static int width(Screen s) {
        int w = 0;
        for (int[] b : layout(s)) w += b[2] + 4;
        return w;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRender(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        if (!npcScreen(s)) return;
        GuiGraphics g = e.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        g.pose().pushPose();
        g.pose().translate(0, 0, 450);
        for (int[] b : layout(s)) {
            boolean hov = Ui.inside(e.getMouseX(), e.getMouseY(), b[0], b[1], b[2], H);
            if (b[3] == 2) {
                Ui.button(g, mc.font, b[0], b[1], b[2], H, talkLabel(), hov, true);
            } else if (b[3] == 0) {
                String label = questLabel();
                Ui.bevel(g, b[0], b[1], b[2], H, hov ? Ui.C_GOLD : Ui.C_GOLD_DARK, Ui.C_GOLD, Ui.C_SHADOW);
                g.drawString(mc.font, label, b[0] + (b[2] - mc.font.width(label)) / 2, b[1] + 3, Ui.C_INK, false);
            } else {
                Ui.button(g, mc.font, b[0], b[1], b[2], H, editLabel(), hov, true);
            }
        }
        g.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        Screen s = e.getScreen();
        if (!npcScreen(s) || e.getButton() != 0) return;
        Minecraft mc = Minecraft.getInstance();
        for (int[] b : layout(s)) {
            if (!Ui.inside(e.getMouseX(), e.getMouseY(), b[0], b[1], b[2], H)) continue;
            e.setCanceled(true);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (b[3] == 2) {
                int id = npcId;
                s.onClose();
                mc.player.connection.sendCommand("rtshop talk open " + id);
            } else if (b[3] == 0) {
                int id = npcId;
                returnNpc = id;
                returnArmedAt = System.currentTimeMillis();
                s.onClose();
                mc.player.connection.sendCommand("rtshop quest open " + id);
            } else {
                String id = ((ShopScreen) s).shopIdForEdit();
                ShopNet.CHANNEL.sendToServer(new ShopEditActionPacket(ShopEditor.OPEN, id, ""));
            }
            return;
        }
    }

    /** 시험용: [퀘스트] 단추를 누른 것과 같다. */
    public static boolean pressQuestForTest(Screen s) {
        if (!questButton(s)) return false;
        Minecraft mc = Minecraft.getInstance();
        int id = npcId;
        returnNpc = id;
        returnArmedAt = System.currentTimeMillis();
        s.onClose();
        mc.player.connection.sendCommand("rtshop quest open " + id);
        return true;
    }

    public static boolean pressTalkForTest(Screen s) {
        if (bound.get() != s) return false;
        Minecraft mc = Minecraft.getInstance();
        int id = npcId;
        s.onClose();
        mc.player.connection.sendCommand("rtshop talk open " + id);
        return true;
    }

    public static boolean questButtonForTest(Screen s) {
        return questButton(s);
    }
}
