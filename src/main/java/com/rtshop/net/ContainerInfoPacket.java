package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 방금 연 보관함이 토지 안에 있음 (PIN 버튼 표시용). */
public final class ContainerInfoPacket {
    private final int containerId;
    private final BlockPos pos;
    private final boolean canSet;
    private final boolean hasPin;

    public ContainerInfoPacket(int containerId, BlockPos pos, boolean canSet, boolean hasPin) {
        this.containerId = containerId;
        this.pos = pos;
        this.canSet = canSet;
        this.hasPin = hasPin;
    }

    public static void encode(ContainerInfoPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.canSet);
        buf.writeBoolean(msg.hasPin);
    }

    public static ContainerInfoPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        BlockPos pos = buf.readBlockPos();
        boolean canSet = buf.readBoolean();
        boolean hasPin = buf.readBoolean();
        return new ContainerInfoPacket(id, pos, canSet, hasPin);
    }

    public static void handle(ContainerInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHooks.containerInfo(msg.containerId, msg.pos, msg.canSet, msg.hasPin)));
        ctx.get().setPacketHandled(true);
    }
}
