package com.rtshop.net;

import com.rtshop.job.JobEditor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 직업 편집기에서 저장 (jobs.json 전체). */
public final class JobEditSavePacket {
    public static final int MAX = 30_000;
    private final String json;

    public JobEditSavePacket(String json) {
        this.json = json;
    }

    public static void encode(JobEditSavePacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.json, MAX);
    }

    public static JobEditSavePacket decode(FriendlyByteBuf buf) {
        return new JobEditSavePacket(buf.readUtf(MAX));
    }

    public static void handle(JobEditSavePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp != null) {
                JobEditor.handleSave(sp, msg.json);
            }
        });
        c.setPacketHandled(true);
    }
}
