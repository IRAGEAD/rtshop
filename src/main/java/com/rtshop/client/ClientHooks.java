package com.rtshop.client;

import com.rtshop.shop.ShopData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
}
