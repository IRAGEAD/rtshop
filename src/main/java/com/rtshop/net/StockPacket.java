package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 조합 증서 창에서 한 일 (3.72). 0 = 다시 보기 / 1 = 사기(union, arg = 주 수) / 2 = 팔기(serial) / 3 = 배당 받기. */
public final class StockPacket {
    private final int action;
    private final String union;
    private final int arg;
    private final String serial;

    public StockPacket(int action, String union, int arg, String serial) {
        this.action = action;
        this.union = union == null ? "" : union;
        this.arg = arg;
        this.serial = serial == null ? "" : serial;
    }

    public static void encode(StockPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeUtf(msg.union, 64);
        buf.writeVarInt(msg.arg);
        buf.writeUtf(msg.serial, 64);
    }

    public static StockPacket decode(FriendlyByteBuf buf) {
        int action = buf.readVarInt();
        String union = buf.readUtf(64);
        int arg = buf.readVarInt();
        return new StockPacket(action, union, arg, buf.readUtf(64));
    }

    public static void handle(StockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                com.rtshop.stock.Stocks.fromScreen(sender, msg.action, msg.union, msg.arg, msg.serial);
            }
        });
        c.setPacketHandled(true);
    }
}
