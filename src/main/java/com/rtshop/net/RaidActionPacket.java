package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 레이드 입장·이동 투표·실패 확인, 보상 편집기 저장. */
public final class RaidActionPacket {
    public final int action;
    public final String payload;

    public RaidActionPacket(int action, String payload) {
        this.action = action;
        this.payload = payload == null ? "" : payload;
    }

    public static void encode(RaidActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        Packed.write(buf, m.payload);
    }

    public static RaidActionPacket decode(FriendlyByteBuf buf) {
        return new RaidActionPacket(buf.readVarInt(), Packed.read(buf));
    }

    public static void handle(RaidActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) {
                com.rtshop.raid.Raid.handle(p, m.action, m.payload);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
