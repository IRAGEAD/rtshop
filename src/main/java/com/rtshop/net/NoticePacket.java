package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: RT Shop 알림 한 줄 (채팅 대신 화면 아래 알림 상자에 잠시 보인다). */
public final class NoticePacket {
    public final Component text;
    /** 상태 알림 (예전 경고 문구 자리): 새 상태 알림이 오면 앞의 것을 바꿔 쓴다. */
    public final boolean status;

    public NoticePacket(Component text, boolean status) {
        this.text = text;
        this.status = status;
    }

    public static void encode(NoticePacket m, FriendlyByteBuf buf) {
        buf.writeComponent(m.text);
        buf.writeBoolean(m.status);
    }

    public static NoticePacket decode(FriendlyByteBuf buf) {
        return new NoticePacket(buf.readComponent(), buf.readBoolean());
    }

    public static void handle(NoticePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.NoticeHud.push(m.text, m.status)));
        ctx.get().setPacketHandled(true);
    }
}
