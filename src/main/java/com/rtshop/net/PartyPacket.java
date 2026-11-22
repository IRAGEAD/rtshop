package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 내 파티 (파티원 이름·체력·접속 여부). "{}" 면 파티 없음. */
public final class PartyPacket {
    public final String json;

    public PartyPacket(String json) {
        this.json = json;
    }

    public static void encode(PartyPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.json, 262144);
    }

    public static PartyPacket decode(FriendlyByteBuf buf) {
        return new PartyPacket(buf.readUtf(262144));
    }

    public static void handle(PartyPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.PartyHud.set(m.json)));
        ctx.get().setPacketHandled(true);
    }
}
