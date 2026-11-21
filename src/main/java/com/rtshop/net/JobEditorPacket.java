package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 직업 편집기를 연다 (jobs.json 내용 그대로). */
public final class JobEditorPacket {
    public final String json;

    public JobEditorPacket(String json) {
        this.json = json;
    }

    public static void encode(JobEditorPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.json, 262_144);
    }

    public static JobEditorPacket decode(FriendlyByteBuf buf) {
        return new JobEditorPacket(buf.readUtf(262_144));
    }

    public static void handle(JobEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openJobEditor(msg)));
        ctx.get().setPacketHandled(true);
    }
}
