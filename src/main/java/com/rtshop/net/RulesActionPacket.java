package com.rtshop.net;

import com.rtshop.rules.RulesEditor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 조합법·전리품 편집기 요청 (OP). */
public final class RulesActionPacket {
    public final int action;
    public final String key;
    public final String json;

    public RulesActionPacket(int action, String key, String json) {
        this.action = action;
        this.key = key;
        this.json = json;
    }

    public static void encode(RulesActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        buf.writeUtf(m.key);
        Packed.write(buf, m.json);
    }

    public static RulesActionPacket decode(FriendlyByteBuf buf) {
        return new RulesActionPacket(buf.readVarInt(), buf.readUtf(256), Packed.read(buf));
    }

    public static void handle(RulesActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer p = c.getSender();
        c.enqueueWork(() -> {
            if (p != null) RulesEditor.handle(p, msg.action, msg.key, msg.json);
        });
        c.setPacketHandled(true);
    }
}
