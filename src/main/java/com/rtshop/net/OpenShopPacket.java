package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import com.rtshop.shop.ShopData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 상점 화면을 열고 상점 데이터를 전달. */
public final class OpenShopPacket {
    private final ShopData data;

    public OpenShopPacket(ShopData data) {
        this.data = data;
    }

    public static void encode(OpenShopPacket msg, FriendlyByteBuf buf) {
        msg.data.write(buf);
    }

    public static OpenShopPacket decode(FriendlyByteBuf buf) {
        return new OpenShopPacket(ShopData.read(buf));
    }

    public static void handle(OpenShopPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openShop(msg.data)));
        ctx.get().setPacketHandled(true);
    }
}
