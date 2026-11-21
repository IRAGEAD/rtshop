package com.rtshop.net;

import com.rtshop.land.PlotBrowser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 토지 상인 창의 구획 요청 (열기·미리보기·구매·경계·초보 농지). */
public final class PlotActionPacket {
    public final int action;
    public final int id;

    public PlotActionPacket(int action, int id) {
        this.action = action;
        this.id = id;
    }

    public static void encode(PlotActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        buf.writeVarInt(m.id);
    }

    public static PlotActionPacket decode(FriendlyByteBuf buf) {
        return new PlotActionPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(PlotActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer p = c.getSender();
        c.enqueueWork(() -> {
            if (p != null) {
                PlotBrowser.handle(p, msg.action, msg.id);
            }
        });
        c.setPacketHandled(true);
    }
}
