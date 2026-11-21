package com.rtshop.net;

import com.rtshop.client.ClientQuestHud;
import com.rtshop.quest.QuestView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 서버 -> 클라이언트: 화면 구석의 퀘스트 HUD 에 보일 진행 중인 퀘스트 (바뀔 때만 보낸다). */
public final class QuestHudPacket {
    /** 퀘스트 한 건: 완료할 수 있으면 ready, 그때 돌아갈 NPC 이름이 turnIn. */
    public record Entry(String id, String title, boolean ready, String turnIn, List<QuestView.ObjView> objectives) {
    }

    public final List<Entry> entries;

    public QuestHudPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public static void encode(QuestHudPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (Entry e : msg.entries) {
            buf.writeUtf(e.id(), 128);
            buf.writeUtf(e.title(), 256);
            buf.writeBoolean(e.ready());
            buf.writeUtf(e.turnIn(), 256);
            buf.writeVarInt(e.objectives().size());
            for (QuestView.ObjView o : e.objectives()) {
                buf.writeVarInt(o.type());
                buf.writeUtf(o.target(), 256);
                buf.writeUtf(o.text(), 512);
                buf.writeVarInt(o.cur());
                buf.writeVarInt(o.need());
            }
        }
    }

    public static QuestHudPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf(128);
            String title = buf.readUtf(256);
            boolean ready = buf.readBoolean();
            String turnIn = buf.readUtf(256);
            int on = buf.readVarInt();
            List<QuestView.ObjView> objs = new ArrayList<>();
            for (int j = 0; j < on; j++) {
                objs.add(new QuestView.ObjView(buf.readVarInt(), buf.readUtf(256), buf.readUtf(512), buf.readVarInt(), buf.readVarInt()));
            }
            list.add(new Entry(id, title, ready, turnIn, objs));
        }
        return new QuestHudPacket(list);
    }

    public static void handle(QuestHudPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientQuestHud.set(msg.entries)));
        ctx.get().setPacketHandled(true);
    }
}
