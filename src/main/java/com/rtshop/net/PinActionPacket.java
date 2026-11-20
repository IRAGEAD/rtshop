package com.rtshop.net;

import com.rtshop.land.LandManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: PIN 설정(0) / 해제(1) / 입력 시도(2). */
public final class PinActionPacket {
    public static final int SET = 0;
    public static final int REMOVE = 1;
    public static final int TRY = 2;

    private final BlockPos pos;
    private final int action;
    private final String pin;

    public PinActionPacket(BlockPos pos, int action, String pin) {
        this.pos = pos;
        this.action = action;
        this.pin = pin;
    }

    public static void encode(PinActionPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.action);
        buf.writeUtf(msg.pin, 32);
    }

    public static PinActionPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int action = buf.readVarInt();
        String pin = buf.readUtf(32);
        return new PinActionPacket(pos, action, pin);
    }

    public static void handle(PinActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender == null) {
                return;
            }
            switch (msg.action) {
                case SET -> LandManager.setPin(sender, msg.pos, msg.pin);
                case REMOVE -> LandManager.setPin(sender, msg.pos, "");
                case TRY -> {
                    if (LandManager.tryPin(sender, msg.pos, msg.pin)) {
                        LandManager.openStorage(sender, msg.pos);
                    }
                }
                default -> {
                }
            }
        });
        c.setPacketHandled(true);
    }
}
