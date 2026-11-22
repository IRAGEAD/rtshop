package com.rtshop.net;

import com.rtshop.shop.ShopEditor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 상점 편집기 열기·저장 (OP). */
public final class ShopEditActionPacket {
    public final int action;
    public final String id;
    public final String json;

    public ShopEditActionPacket(int action, String id, String json) {
        this.action = action;
        this.id = id;
        this.json = json;
    }

    public static void encode(ShopEditActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        buf.writeUtf(m.id);
        Packed.write(buf, m.json);
    }

    public static ShopEditActionPacket decode(FriendlyByteBuf buf) {
        return new ShopEditActionPacket(buf.readVarInt(), buf.readUtf(64), Packed.read(buf));
    }

    public static void handle(ShopEditActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer p = c.getSender();
        c.enqueueWork(() -> {
            if (p != null) ShopEditor.handle(p, msg.action, msg.id, msg.json);
        });
        c.setPacketHandled(true);
    }
}
