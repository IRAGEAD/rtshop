package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 이야기 연출 (CINEMA: 장 제목·보스 등장·환영·자막) / LORE: 아이템 이야기 목록. */
public final class StoryPacket {
    public static final int CINEMA = 0, LORE = 1;
    public final int kind;
    public final String json;

    public StoryPacket(int kind, String json) {
        this.kind = kind;
        this.json = json;
    }

    public static void encode(StoryPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.kind);
        buf.writeUtf(m.json, 1 << 20);
    }

    public static StoryPacket decode(FriendlyByteBuf buf) {
        return new StoryPacket(buf.readVarInt(), buf.readUtf(1 << 20));
    }

    public static void handle(StoryPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.StoryClient.receive(m.kind, m.json)));
        ctx.get().setPacketHandled(true);
    }
}
