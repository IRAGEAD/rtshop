package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: Gold 표시 박스 위치 조정 화면 열기 (/rtshop hud). */
public final class OpenHudEditorPacket {
    public OpenHudEditorPacket() {
    }

    public static void encode(OpenHudEditorPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenHudEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenHudEditorPacket();
    }

    public static void handle(OpenHudEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openHudEditor()));
        ctx.get().setPacketHandled(true);
    }
}
