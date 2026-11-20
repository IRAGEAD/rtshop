package com.rtshop.net;

import com.rtshop.RtShop;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** rtshop 전용 네트워크 채널 (다른 모드와 채널 이름이 겹치지 않음). */
public final class ShopNet {
    private ShopNet() {
    }

    private static final String PROTOCOL = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RtShop.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenShopPacket.class, OpenShopPacket::encode, OpenShopPacket::decode, OpenShopPacket::handle);
        CHANNEL.registerMessage(id++, ResultPacket.class, ResultPacket::encode, ResultPacket::decode, ResultPacket::handle);
        CHANNEL.registerMessage(id++, BalancePacket.class, BalancePacket::encode, BalancePacket::decode, BalancePacket::handle);
        CHANNEL.registerMessage(id++, OpenBankPacket.class, OpenBankPacket::encode, OpenBankPacket::decode, OpenBankPacket::handle);
        CHANNEL.registerMessage(id++, BankPacket.class, BankPacket::encode, BankPacket::decode, BankPacket::handle);
        CHANNEL.registerMessage(id++, TransactionPacket.class, TransactionPacket::encode, TransactionPacket::decode, TransactionPacket::handle);
        CHANNEL.registerMessage(id++, ClosePacket.class, ClosePacket::encode, ClosePacket::decode, ClosePacket::handle);
    }
}
