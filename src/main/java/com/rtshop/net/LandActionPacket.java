package com.rtshop.net;

import com.rtshop.land.LandManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 토지 설정 변경 요청. 권한 확인은 서버에서 한다. */
public final class LandActionPacket {
    private final int landId;
    private final int action;
    private final String text;
    private final int value;

    public LandActionPacket(int landId, int action, String text, int value) {
        this.landId = landId;
        this.action = action;
        this.text = text;
        this.value = value;
    }

    public static void encode(LandActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.landId);
        buf.writeVarInt(msg.action);
        buf.writeUtf(msg.text, 64);
        buf.writeVarInt(msg.value);
    }

    public static LandActionPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int action = buf.readVarInt();
        String text = buf.readUtf(64);
        int value = buf.readVarInt();
        return new LandActionPacket(id, action, text, value);
    }

    public static void handle(LandActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                LandManager.handleAction(sender, msg.landId, msg.action, msg.text, msg.value);
            }
        });
        c.setPacketHandled(true);
    }
}
