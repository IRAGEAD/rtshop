package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 내 PvP 상태 (켜짐 여부, 지금 있는 곳의 종류, 전투 중이라 끌 수 없는 남은 초). */
public final class PvpPacket {
    public final boolean enabled;
    public final boolean on;
    public final int area;
    public final int lock;

    public PvpPacket(boolean enabled, boolean on, int area, int lock) {
        this.enabled = enabled;
        this.on = on;
        this.area = area;
        this.lock = lock;
    }

    public static void encode(PvpPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.enabled);
        buf.writeBoolean(m.on);
        buf.writeByte(m.area);
        buf.writeVarInt(m.lock);
    }

    public static PvpPacket decode(FriendlyByteBuf buf) {
        return new PvpPacket(buf.readBoolean(), buf.readBoolean(), buf.readByte(), buf.readVarInt());
    }

    public static void handle(PvpPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.PvpHud.set(m.enabled, m.on, m.area, m.lock)));
        ctx.get().setPacketHandled(true);
    }
}
