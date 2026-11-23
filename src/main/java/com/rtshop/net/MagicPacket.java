package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 마법 뽑기 (VIEW 창 내용, RESULT 뽑기·재설정 결과, EDITOR 편집기). */
public final class MagicPacket {
    public static final int VIEW = 0, RESULT = 1, EDITOR = 2;
    public final int kind;
    public final String json;

    public MagicPacket(int kind, String json) {
        this.kind = kind;
        this.json = json;
    }

    public static void encode(MagicPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.kind);
        buf.writeUtf(m.json, 1 << 21);
    }

    public static MagicPacket decode(FriendlyByteBuf buf) {
        return new MagicPacket(buf.readVarInt(), buf.readUtf(1 << 21));
    }

    public static void handle(MagicPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.MagicClient.receive(m.kind, m.json)));
        ctx.get().setPacketHandled(true);
    }
}
