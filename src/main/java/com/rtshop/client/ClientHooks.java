package com.rtshop.client;

import com.rtshop.net.LandInfoPacket;
import com.rtshop.net.QuestOpenPacket;
import com.rtshop.shop.ShopData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 클라이언트 전용 진입점 (패킷 핸들러가 DistExecutor 로만 호출). */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void setBalance(long wallet, long bank) {
        ClientEconomy.wallet = wallet;
        ClientEconomy.bank = bank;
    }

    public static void openShop(ShopData data) {
        Minecraft.getInstance().setScreen(new ShopScreen(data));
    }

    public static void openBank(double lossMin, double lossMax) {
        Minecraft.getInstance().setScreen(new BankScreen(lossMin, lossMax));
    }

    public static void onResult(Component message, boolean success) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ShopScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof BankScreen screen) {
            screen.showResult(message, success);
        } else if (mc.player != null) {
            mc.player.displayClientMessage(message, true);
        }
    }

    /** 퀘스트 창을 열거나, 이미 열려 있으면 내용만 새로 고친다. */
    public static void openQuest(QuestOpenPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof QuestScreen screen) {
            screen.update(packet);
        } else {
            mc.setScreen(new QuestScreen(packet));
        }
    }

    public static void openHudEditor() {
        Minecraft.getInstance().setScreen(new HudEditScreen());
    }

    /** 토지 설정 화면을 열거나, 이미 열려 있으면 내용만 새로 고친다. */
    public static void openLand(List<LandInfoPacket.View> views, int currentId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof LandScreen screen) {
            screen.update(views, currentId);
        } else {
            mc.setScreen(new LandScreen(views, currentId));
        }
    }

    /** 방금 연 보관함이 토지 안에 있다는 서버의 알림 (PIN 버튼 표시용). */
    public static void containerInfo(int containerId, BlockPos pos, boolean canSet, boolean hasPin) {
        ClientLand.containerId = containerId;
        ClientLand.containerPos = pos;
        ClientLand.canSetPin = canSet;
        ClientLand.hasPin = hasPin;
    }

    /** PIN 으로 잠긴 보관함을 열려고 했을 때: PIN 입력 화면. */
    public static void openPinEntry(BlockPos pos) {
        Minecraft.getInstance().setScreen(new PinScreen(pos, PinScreen.Mode.ENTER, false));
    }
}
