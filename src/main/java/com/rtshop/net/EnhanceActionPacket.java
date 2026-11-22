package com.rtshop.net;

import com.rtshop.enhance.Enhance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 강화 확률표 열기, OP 편집기 열기·저장. */
public final class EnhanceActionPacket {
    public static final int OPEN_EDITOR = 0;
    public static final int SAVE = 1;
    public static final int OPEN_ODDS = 2;

    public final int action;
    public final String json;

    public EnhanceActionPacket(int action, String json) {
        this.action = action;
        this.json = json;
    }

    public static void encode(EnhanceActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        Packed.write(buf, m.json);
    }

    public static EnhanceActionPacket decode(FriendlyByteBuf buf) {
        return new EnhanceActionPacket(buf.readVarInt(), Packed.read(buf));
    }

    public static void handle(EnhanceActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer p = c.getSender();
        c.enqueueWork(() -> {
            if (p != null) {
                Enhance.action(p, msg.action, msg.json);
            }
        });
        c.setPacketHandled(true);
    }
}
