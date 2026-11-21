package com.rtshop.net;

import com.rtshop.quest.QuestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 퀘스트 수락(0) / 완료(1) / 포기(2). 완료 때 choice 는 고른 선택지 번호(없으면 -1). */
public final class QuestActionPacket {
    public static final int ACCEPT = 0;
    public static final int TURN_IN = 1;
    public static final int ABANDON = 2;

    private final int action;
    private final String questId;
    private final int choice;

    public QuestActionPacket(int action, String questId, int choice) {
        this.action = action;
        this.questId = questId;
        this.choice = choice;
    }

    public static void encode(QuestActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeUtf(msg.questId, 128);
        buf.writeVarInt(msg.choice + 1);
    }

    public static QuestActionPacket decode(FriendlyByteBuf buf) {
        int action = buf.readVarInt();
        String id = buf.readUtf(128);
        int choice = buf.readVarInt() - 1;
        return new QuestActionPacket(action, id, choice);
    }

    public static void handle(QuestActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                QuestManager.handleAction(sender, msg.action, msg.questId, msg.choice);
            }
        });
        c.setPacketHandled(true);
    }
}
