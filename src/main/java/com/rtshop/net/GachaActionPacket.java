package com.rtshop.net;

import com.rtshop.gacha.Gacha;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 뽑기 창의 버튼 (뽑기 / 분해 / 조각 교환 / 티켓 상점 / 편집). 인자는 JSON. */
public final class GachaActionPacket {
    public static final int PULL = 0;
    public static final int DISMANTLE = 1;
    public static final int DISMANTLE_ALL = 2;
    public static final int EXCHANGE = 3;
    public static final int SHOP = 4;
    public static final int EDIT = 5;
    public static final int SAVE = 6;
    public static final int AUTOFILL = 7;
    public static final int SIMULATE = 8;

    public final int action;
    public final String arg;

    public GachaActionPacket(int action, String arg) {
        this.action = action;
        this.arg = arg == null ? "" : arg;
    }

    public static void encode(GachaActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        Packed.write(buf, m.arg);
    }

    public static GachaActionPacket decode(FriendlyByteBuf buf) {
        return new GachaActionPacket(buf.readVarInt(), Packed.read(buf));
    }

    public static void handle(GachaActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) {
                Gacha.handleAction(p, msg.action, msg.arg);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
