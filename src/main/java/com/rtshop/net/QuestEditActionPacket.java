package com.rtshop.net;

import com.rtshop.quest.QuestEditor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 편집기에서 퀘스트/NPC 저장·삭제 (QuestEditor.SAVE_QUEST 등). */
public final class QuestEditActionPacket {
    /** 서버로 가는 패킷은 32KB 를 넘을 수 없다. 글자 수가 아니라 바이트 기준이라 한글은 1만 자 정도. */
    public static final int MAX_JSON = 30_000;

    private final int action;
    private final String origId;
    private final String json;

    public QuestEditActionPacket(int action, String origId, String json) {
        this.action = action;
        this.origId = origId;
        this.json = json;
    }

    public static void encode(QuestEditActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeUtf(msg.origId, 256);
        buf.writeUtf(msg.json, MAX_JSON);
    }

    public static QuestEditActionPacket decode(FriendlyByteBuf buf) {
        int a = buf.readVarInt();
        String id = buf.readUtf(256);
        String json = buf.readUtf(MAX_JSON);
        return new QuestEditActionPacket(a, id, json);
    }

    public static void handle(QuestEditActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                QuestEditor.handle(sender, msg.action, msg.origId, msg.json);
            }
        });
        c.setPacketHandled(true);
    }
}
