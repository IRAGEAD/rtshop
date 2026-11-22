package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 상점 파일 내용 (OP 상점 편집기). */
public final class ShopEditDataPacket {
    public final String id;
    public final String json;

    public ShopEditDataPacket(String id, String json) {
        this.id = id;
        this.json = json;
    }

    public static void encode(ShopEditDataPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.id);
        Packed.write(buf, m.json);
    }

    public static ShopEditDataPacket decode(FriendlyByteBuf buf) {
        return new ShopEditDataPacket(buf.readUtf(64), Packed.read(buf));
    }

    public static void handle(ShopEditDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.ShopEditScreen.open(msg.id, msg.json)));
        ctx.get().setPacketHandled(true);
    }
}
