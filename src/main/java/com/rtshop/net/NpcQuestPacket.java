package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 방금 말을 건 상인에게 받을 수 있는 퀘스트가 있다 (상점 화면에 [퀘스트] 단추). */
public final class NpcQuestPacket {
    public final int entityId;
    public final int count;

    public NpcQuestPacket(int entityId, int count) {
        this.entityId = entityId;
        this.count = count;
    }

    public static void encode(NpcQuestPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entityId);
        buf.writeVarInt(m.count);
    }

    public static NpcQuestPacket decode(FriendlyByteBuf buf) {
        return new NpcQuestPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(NpcQuestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.NpcButtons.setQuests(msg.entityId, msg.count)));
        ctx.get().setPacketHandled(true);
    }
}
