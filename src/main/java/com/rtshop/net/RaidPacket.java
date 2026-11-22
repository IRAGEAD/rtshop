package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 레이드 안내인 창(VIEW), 성공 뒤 이동 투표·실패 확인(PROMPT), 보상 편집기(EDITOR), 주간 던전 보상(ABYSS). 내용은 JSON. */
public final class RaidPacket {
    public static final int VIEW = 0, PROMPT = 1, EDITOR = 2, ABYSS = 3;
    public final int kind;
    public final String json;

    public RaidPacket(int kind, String json) {
        this.kind = kind;
        this.json = json;
    }

    public static void encode(RaidPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.kind);
        Packed.write(buf, m.json);
    }

    public static RaidPacket decode(FriendlyByteBuf buf) {
        return new RaidPacket(buf.readVarInt(), Packed.read(buf));
    }

    public static void handle(RaidPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.RaidClient.receive(m.kind, m.json)));
        ctx.get().setPacketHandled(true);
    }
}
