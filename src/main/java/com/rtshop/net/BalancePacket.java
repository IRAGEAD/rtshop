package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 소지금/예금 동기화. */
public final class BalancePacket {
    private final long wallet;
    private final long bank;

    public BalancePacket(long wallet, long bank) {
        this.wallet = wallet;
        this.bank = bank;
    }

    public static void encode(BalancePacket msg, FriendlyByteBuf buf) {
        buf.writeVarLong(msg.wallet);
        buf.writeVarLong(msg.bank);
    }

    public static BalancePacket decode(FriendlyByteBuf buf) {
        long w = buf.readVarLong();
        long b = buf.readVarLong();
        return new BalancePacket(w, b);
    }

    public static void handle(BalancePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.setBalance(msg.wallet, msg.bank)));
        ctx.get().setPacketHandled(true);
    }
}
