package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 플레이어 상점 화면 내용 (JSON). */
public final class PShopDataPacket {
    public final String json;

    public PShopDataPacket(String json) {
        this.json = json;
    }

    public static void encode(PShopDataPacket m, FriendlyByteBuf buf) {
        Packed.write(buf, m.json);
    }

    public static PShopDataPacket decode(FriendlyByteBuf buf) {
        return new PShopDataPacket(Packed.read(buf));
    }

    public static void handle(PShopDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.PlayerShopScreen.onData(msg.json)));
        ctx.get().setPacketHandled(true);
    }
}
