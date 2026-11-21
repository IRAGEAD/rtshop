package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 토지 상인 창의 구획 목록 (JSON). */
public final class PlotListPacket {
    public final String json;

    public PlotListPacket(String json) {
        this.json = json;
    }

    public static void encode(PlotListPacket m, FriendlyByteBuf buf) {
        Packed.write(buf, m.json);
    }

    public static PlotListPacket decode(FriendlyByteBuf buf) {
        return new PlotListPacket(Packed.read(buf));
    }

    public static void handle(PlotListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.PlotBrowserScreen.onList(msg.json)));
        ctx.get().setPacketHandled(true);
    }
}
