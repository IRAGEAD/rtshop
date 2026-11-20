package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 거래 결과 메시지. */
public final class ResultPacket {
    private final Component message;
    private final boolean success;

    public ResultPacket(Component message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public static void encode(ResultPacket msg, FriendlyByteBuf buf) {
        buf.writeComponent(msg.message);
        buf.writeBoolean(msg.success);
    }

    public static ResultPacket decode(FriendlyByteBuf buf) {
        Component c = buf.readComponent();
        boolean ok = buf.readBoolean();
        return new ResultPacket(c, ok);
    }

    public static void handle(ResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onResult(msg.message, msg.success)));
        ctx.get().setPacketHandled(true);
    }
}
