package com.rtshop.net;

import com.rtshop.shop.ShopServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 구매/판매 요청. 모든 검증은 서버에서 다시 수행합니다. */
public final class TransactionPacket {
    private final String shopId;
    private final int dataVersion;
    private final int entryIndex;
    private final boolean sell;
    private final int amount;

    public TransactionPacket(String shopId, int dataVersion, int entryIndex, boolean sell, int amount) {
        this.shopId = shopId;
        this.dataVersion = dataVersion;
        this.entryIndex = entryIndex;
        this.sell = sell;
        this.amount = amount;
    }

    public static void encode(TransactionPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.shopId);
        buf.writeVarInt(msg.dataVersion);
        buf.writeVarInt(msg.entryIndex);
        buf.writeBoolean(msg.sell);
        buf.writeVarInt(msg.amount);
    }

    public static TransactionPacket decode(FriendlyByteBuf buf) {
        String sid = buf.readUtf();
        int v = buf.readVarInt();
        int idx = buf.readVarInt();
        boolean sell = buf.readBoolean();
        int amount = buf.readVarInt();
        return new TransactionPacket(sid, v, idx, sell, amount);
    }

    public static void handle(TransactionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                ShopServer.handleTransaction(sender, msg.shopId, msg.dataVersion, msg.entryIndex, msg.sell, msg.amount);
            }
        });
        c.setPacketHandled(true);
    }
}
