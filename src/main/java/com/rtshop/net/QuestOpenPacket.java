package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import com.rtshop.quest.QuestView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 서버 -> 클라이언트: 퀘스트 창을 열거나(이미 열려 있으면 내용만 새로 고침). journal 이면 수첩. */
public final class QuestOpenPacket {
    private static final int S = 4096;

    public final String npcKey;
    public final String title;
    public final String greeting;
    public final boolean journal;
    public final List<QuestView> views;

    public QuestOpenPacket(String npcKey, String title, String greeting, boolean journal, List<QuestView> views) {
        this.npcKey = npcKey;
        this.title = title;
        this.greeting = greeting;
        this.journal = journal;
        this.views = views;
    }

    public static void encode(QuestOpenPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.npcKey, 128);
        buf.writeUtf(msg.title, 256);
        buf.writeUtf(msg.greeting, S);
        buf.writeBoolean(msg.journal);
        buf.writeVarInt(msg.views.size());
        for (QuestView v : msg.views) {
            buf.writeUtf(v.id(), 128);
            buf.writeUtf(v.title(), 256);
            buf.writeUtf(v.chain(), 256);
            buf.writeVarInt(v.state());
            buf.writeUtf(v.text(), S);
            buf.writeVarInt(v.objectives().size());
            for (QuestView.ObjView o : v.objectives()) {
                buf.writeVarInt(o.type());
                buf.writeUtf(o.target(), 256);
                buf.writeUtf(o.text(), 512);
                buf.writeVarInt(o.cur());
                buf.writeVarInt(o.need());
            }
            buf.writeVarLong(v.rewardGold());
            buf.writeVarInt(v.rewardItems().size());
            for (QuestView.RewardItem r : v.rewardItems()) {
                buf.writeUtf(r.item(), 256);
                buf.writeVarInt(r.count());
            }
            buf.writeVarInt(v.choices().size());
            for (String c : v.choices()) {
                buf.writeUtf(c, 512);
            }
        }
    }

    public static QuestOpenPacket decode(FriendlyByteBuf buf) {
        String npcKey = buf.readUtf(128);
        String title = buf.readUtf(256);
        String greeting = buf.readUtf(S);
        boolean journal = buf.readBoolean();
        int n = buf.readVarInt();
        List<QuestView> views = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf(128);
            String qt = buf.readUtf(256);
            String chain = buf.readUtf(256);
            int state = buf.readVarInt();
            String text = buf.readUtf(S);
            int on = buf.readVarInt();
            List<QuestView.ObjView> objs = new ArrayList<>();
            for (int j = 0; j < on; j++) {
                objs.add(new QuestView.ObjView(buf.readVarInt(), buf.readUtf(256), buf.readUtf(512),
                        buf.readVarInt(), buf.readVarInt()));
            }
            long gold = buf.readVarLong();
            int rn = buf.readVarInt();
            List<QuestView.RewardItem> items = new ArrayList<>();
            for (int j = 0; j < rn; j++) {
                items.add(new QuestView.RewardItem(buf.readUtf(256), buf.readVarInt()));
            }
            int cn = buf.readVarInt();
            List<String> choices = new ArrayList<>();
            for (int j = 0; j < cn; j++) {
                choices.add(buf.readUtf(512));
            }
            views.add(new QuestView(id, qt, chain, state, text, objs, gold, items, choices));
        }
        return new QuestOpenPacket(npcKey, title, greeting, journal, views);
    }

    public static void handle(QuestOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openQuest(msg)));
        ctx.get().setPacketHandled(true);
    }
}
