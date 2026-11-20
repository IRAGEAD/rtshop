package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 은행 화면 열기. */
public final class OpenBankPacket {
    private final double lossMin;
    private final double lossMax;

    public OpenBankPacket(double lossMin, double lossMax) {
        this.lossMin = lossMin;
        this.lossMax = lossMax;
    }

    public static void encode(OpenBankPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.lossMin);
        buf.writeDouble(msg.lossMax);
    }

    public static OpenBankPacket decode(FriendlyByteBuf buf) {
        double a = buf.readDouble();
        double b = buf.readDouble();
        return new OpenBankPacket(a, b);
    }

    public static void handle(OpenBankPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openBank(msg.lossMin, msg.lossMax)));
        ctx.get().setPacketHandled(true);
    }
}
