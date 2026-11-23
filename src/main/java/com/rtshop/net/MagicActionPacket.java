package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 마법 뽑기 동작 (뽑기, 재설정, 편집기 열기·저장·자동 채우기, 새로 고침). */
public final class MagicActionPacket {
    public static final int PULL = 0, REROLL = 1, EDIT = 2, SAVE = 3, AUTOFILL = 4, REFRESH = 5, SHOP = 6;
    public final int action;
    public final String json;

    public MagicActionPacket(int action, String json) {
        this.action = action;
        this.json = json;
    }

    public static void encode(MagicActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        buf.writeUtf(m.json, 1 << 21);
    }

    public static MagicActionPacket decode(FriendlyByteBuf buf) {
        return new MagicActionPacket(buf.readVarInt(), buf.readUtf(1 << 21));
    }

    public static void handle(MagicActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        var p = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (p != null) com.rtshop.magic.MagicGacha.handleAction(p, m.action, m.json);
        });
        ctx.get().setPacketHandled(true);
    }
}
