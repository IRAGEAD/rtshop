package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 조합법 목록 / 전리품표 목록 / 전리품표 확률. */
public final class RulesDataPacket {
    public static final int RECIPES = 0;
    public static final int LOOT_LIST = 1;
    public static final int LOOT_TABLE = 2;

    public final int kind;
    public final String json;

    public RulesDataPacket(int kind, String json) {
        this.kind = kind;
        this.json = json;
    }

    public static void encode(RulesDataPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.kind);
        Packed.write(buf, m.json);
    }

    public static RulesDataPacket decode(FriendlyByteBuf buf) {
        return new RulesDataPacket(buf.readVarInt(), Packed.read(buf));
    }

    public static void handle(RulesDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.RulesScreens.onData(msg.kind, msg.json)));
        ctx.get().setPacketHandled(true);
    }
}
