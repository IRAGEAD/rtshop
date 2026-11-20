package com.rtshop.net;

import com.rtshop.shop.ShopServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 상점 창을 닫았음. */
public final class ClosePacket {
    public ClosePacket() {
    }

    public static void encode(ClosePacket msg, FriendlyByteBuf buf) {
    }

    public static ClosePacket decode(FriendlyByteBuf buf) {
        return new ClosePacket();
    }

    public static void handle(ClosePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                ShopServer.closeSession(sender);
            }
        });
        c.setPacketHandled(true);
    }
}
