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
    /** 이 상인이 맡은 퀘스트가 하나라도 있는가 (지금 받을 수 있는 것이 없어도 단추는 보인다). */
    public final boolean any;

    public NpcQuestPacket(int entityId, int count, boolean any) {
        this.entityId = entityId;
        this.count = count;
        this.any = any || count > 0;
    }

    public static void encode(NpcQuestPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entityId);
        buf.writeVarInt(m.count);
        buf.writeBoolean(m.any);
    }

    public static NpcQuestPacket decode(FriendlyByteBuf buf) {
        return new NpcQuestPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(NpcQuestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.NpcButtons.setQuests(msg.entityId, msg.count, msg.any)));
        ctx.get().setPacketHandled(true);
    }
}
