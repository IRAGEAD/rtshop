package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 열려 있는 상점(식당)의 '지금 살 수 있는 품목' 번호 목록 (메뉴가 늘었을 때 창을 닫지 않고 바로 반영). */
public final class ShopMenuPacket {
    public final String shopId;
    public final int version;
    public final int[] buyable;

    public ShopMenuPacket(String shopId, int version, int[] buyable) {
        this.shopId = shopId;
        this.version = version;
        this.buyable = buyable;
    }

    public static void encode(ShopMenuPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.shopId);
        buf.writeVarInt(m.version);
        buf.writeVarIntArray(m.buyable);
    }

    public static ShopMenuPacket decode(FriendlyByteBuf buf) {
        return new ShopMenuPacket(buf.readUtf(), buf.readVarInt(), buf.readVarIntArray(8192));
    }

    public static void handle(ShopMenuPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.ClientHooks.shopMenu(m.shopId, m.version, m.buyable)));
        ctx.get().setPacketHandled(true);
    }
}
