package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 서버 -> 클라이언트: 퀘스트 편집기를 열거나(이미 열려 있으면 내용만 새로 고침). */
public final class QuestEditorPacket {
    private static final int J = 262_144;

    /** 파일에 적힌 퀘스트 또는 NPC 하나. status 가 비어 있지 않으면 지금 쓰이지 않는 이유. */
    public record Entry(String id, String file, String json, String status) {
    }

    public final List<Entry> quests;
    public final List<Entry> npcs;
    /** [NPC 키, 이름] — 퀘스트를 주는/받는 NPC 고르기용 */
    public final List<String[]> npcKeys;
    public final List<String> problems;

    public QuestEditorPacket(List<Entry> quests, List<Entry> npcs, List<String[]> npcKeys, List<String> problems) {
        this.quests = quests;
        this.npcs = npcs;
        this.npcKeys = npcKeys;
        this.problems = problems;
    }

    private static void writeEntries(FriendlyByteBuf buf, List<Entry> list) {
        buf.writeVarInt(list.size());
        for (Entry e : list) {
            buf.writeUtf(e.id(), 256);
            buf.writeUtf(e.file(), 256);
            buf.writeUtf(e.json(), J);
            buf.writeUtf(e.status(), 2048);
        }
    }

    private static List<Entry> readEntries(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new Entry(buf.readUtf(256), buf.readUtf(256), buf.readUtf(J), buf.readUtf(2048)));
        }
        return out;
    }

    public static void encode(QuestEditorPacket msg, FriendlyByteBuf buf) {
        writeEntries(buf, msg.quests);
        writeEntries(buf, msg.npcs);
        buf.writeVarInt(msg.npcKeys.size());
        for (String[] k : msg.npcKeys) {
            buf.writeUtf(k[0], 256);
            buf.writeUtf(k[1], 256);
        }
        buf.writeVarInt(msg.problems.size());
        for (String p : msg.problems) {
            buf.writeUtf(p, 2048);
        }
    }

    public static QuestEditorPacket decode(FriendlyByteBuf buf) {
        List<Entry> q = readEntries(buf);
        List<Entry> n = readEntries(buf);
        int kn = buf.readVarInt();
        List<String[]> keys = new ArrayList<>();
        for (int i = 0; i < kn; i++) {
            keys.add(new String[]{buf.readUtf(256), buf.readUtf(256)});
        }
        int pn = buf.readVarInt();
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < pn; i++) {
            problems.add(buf.readUtf(2048));
        }
        return new QuestEditorPacket(q, n, keys, problems);
    }

    public static void handle(QuestEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openQuestEditor(msg)));
        ctx.get().setPacketHandled(true);
    }
}
