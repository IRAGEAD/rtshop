package com.rtshop.net;

import com.rtshop.job.Jobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 직업 화면에서 이 직업을 고름. */
public final class JobChoosePacket {
    private final String jobId;

    public JobChoosePacket(String jobId) {
        this.jobId = jobId;
    }

    public static void encode(JobChoosePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.jobId, 64);
    }

    public static JobChoosePacket decode(FriendlyByteBuf buf) {
        return new JobChoosePacket(buf.readUtf(64));
    }

    public static void handle(JobChoosePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp != null) {
                Jobs.handleChoose(sp, msg.jobId);
            }
        });
        c.setPacketHandled(true);
    }
}
