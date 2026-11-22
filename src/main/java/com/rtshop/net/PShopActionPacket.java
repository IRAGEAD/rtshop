package com.rtshop.net;

import com.rtshop.pshop.PlayerShops;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 플레이어 상점 동작 (사기, 올리기, 재고, 가격, 이름 등). */
public final class PShopActionPacket {
    public final BlockPos pos;
    public final int action;
    public final String json;

    public PShopActionPacket(BlockPos pos, int action, String json) {
        this.pos = pos;
        this.action = action;
        this.json = json;
    }

    public static void encode(PShopActionPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeVarInt(m.action);
        buf.writeUtf(m.json, 4096);
    }

    public static PShopActionPacket decode(FriendlyByteBuf buf) {
        return new PShopActionPacket(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(4096));
    }

    public static void handle(PShopActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer p = c.getSender();
        c.enqueueWork(() -> {
            if (p != null) PlayerShops.handle(p, msg.pos, msg.action, msg.json);
        });
        c.setPacketHandled(true);
    }
}
