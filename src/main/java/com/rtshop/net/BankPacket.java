package com.rtshop.net;

import com.rtshop.shop.ShopServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 입금/출금 요청. */
public final class BankPacket {
    private final boolean deposit;
    private final long amount;

    public BankPacket(boolean deposit, long amount) {
        this.deposit = deposit;
        this.amount = amount;
    }

    public static void encode(BankPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.deposit);
        buf.writeVarLong(msg.amount);
    }

    public static BankPacket decode(FriendlyByteBuf buf) {
        boolean d = buf.readBoolean();
        long a = buf.readVarLong();
        return new BankPacket(d, a);
    }

    public static void handle(BankPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                ShopServer.handleBank(sender, msg.deposit, msg.amount);
            }
        });
        c.setPacketHandled(true);
    }
}
