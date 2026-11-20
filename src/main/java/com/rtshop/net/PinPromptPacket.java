package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: PIN 으로 잠긴 보관함이니 PIN 입력 화면을 열 것. */
public final class PinPromptPacket {
    private final BlockPos pos;

    public PinPromptPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(PinPromptPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static PinPromptPacket decode(FriendlyByteBuf buf) {
        return new PinPromptPacket(buf.readBlockPos());
    }

    public static void handle(PinPromptPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openPinEntry(msg.pos)));
        ctx.get().setPacketHandled(true);
    }
}
