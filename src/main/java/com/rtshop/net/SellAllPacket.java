package com.rtshop.net;

import com.rtshop.shop.ShopServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 판매 목록에 보이는 품목을 전부 판매. */
public final class SellAllPacket {
    private static final int MAX = 400;
    private final String shopId;
    private final int version;
    private final int[] indexes;
    private final long[] keys;

    public SellAllPacket(String shopId, int version, int[] indexes, long[] keys) {
        this.shopId = shopId;
        this.version = version;
        this.indexes = indexes;
        this.keys = keys;
    }

    public static void encode(SellAllPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.shopId);
        buf.writeVarInt(msg.version);
        int n = Math.min(MAX, Math.min(msg.indexes.length, msg.keys.length));
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeVarInt(msg.indexes[i]);
            buf.writeVarLong(msg.keys[i]);
        }
    }

    public static SellAllPacket decode(FriendlyByteBuf buf) {
        String sid = buf.readUtf();
        int v = buf.readVarInt();
        int n = Math.max(0, Math.min(MAX, buf.readVarInt()));
        int[] idx = new int[n];
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) {
            idx[i] = buf.readVarInt();
            keys[i] = buf.readVarLong();
        }
        return new SellAllPacket(sid, v, idx, keys);
    }

    public static void handle(SellAllPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                ShopServer.handleSellAll(sender, msg.shopId, msg.version, msg.indexes, msg.keys);
            }
        });
        c.setPacketHandled(true);
    }
}
