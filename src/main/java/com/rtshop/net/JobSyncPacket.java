package com.rtshop.net;

import com.rtshop.job.ClientJobCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 클라이언트 쪽에서도 계산해야 하는 직업 패시브 (채굴 속도). */
public final class JobSyncPacket {
    private final double mineSpeed;

    public JobSyncPacket(double mineSpeed) {
        this.mineSpeed = mineSpeed;
    }

    public static void encode(JobSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.mineSpeed);
    }

    public static JobSyncPacket decode(FriendlyByteBuf buf) {
        return new JobSyncPacket(buf.readDouble());
    }

    public static void handle(JobSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientJobCache.mineSpeed = msg.mineSpeed);
        ctx.get().setPacketHandled(true);
    }
}
