package com.rtshop.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 월드 저장 데이터(world/data/rtshop_quests.dat): 플레이어별 퀘스트 진행, 완료 기록, 표식(flag). */
public final class QuestData extends SavedData {
    private static final String NAME = "rtshop_quests";

    /** 플레이어 한 명의 퀘스트 기록. */
    public static final class Progress {
        public final Set<String> completed = new LinkedHashSet<>();
        /** 진행 중인 퀘스트: 퀘스트 ID -> 목표별 진행 수치. */
        public final Map<String, int[]> active = new LinkedHashMap<>();
        public final Set<String> flags = new LinkedHashSet<>();
    }

    private final Map<UUID, Progress> players = new LinkedHashMap<>();

    public static QuestData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(QuestData::load, QuestData::new, NAME);
    }

    public Progress of(UUID id) {
        return players.computeIfAbsent(id, k -> new Progress());
    }

    public Progress peek(UUID id) {
        return players.get(id);
    }

    public void dirty() {
        setDirty();
    }

    public void clear(UUID id) {
        players.remove(id);
        setDirty();
    }

    public static QuestData load(CompoundTag tag) {
        QuestData d = new QuestData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("id")) {
                continue;
            }
            Progress p = d.of(c.getUUID("id"));
            ListTag done = c.getList("done", Tag.TAG_STRING);
            for (int j = 0; j < done.size(); j++) {
                p.completed.add(done.getString(j));
            }
            ListTag flags = c.getList("flags", Tag.TAG_STRING);
            for (int j = 0; j < flags.size(); j++) {
                p.flags.add(flags.getString(j));
            }
            ListTag act = c.getList("active", Tag.TAG_COMPOUND);
            for (int j = 0; j < act.size(); j++) {
                CompoundTag a = act.getCompound(j);
                p.active.put(a.getString("quest"), a.getIntArray("progress"));
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Progress> e : players.entrySet()) {
            Progress p = e.getValue();
            if (p.completed.isEmpty() && p.active.isEmpty() && p.flags.isEmpty()) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            ListTag done = new ListTag();
            for (String s : p.completed) {
                done.add(StringTag.valueOf(s));
            }
            c.put("done", done);
            ListTag flags = new ListTag();
            for (String s : p.flags) {
                flags.add(StringTag.valueOf(s));
            }
            c.put("flags", flags);
            ListTag act = new ListTag();
            for (Map.Entry<String, int[]> a : p.active.entrySet()) {
                CompoundTag at = new CompoundTag();
                at.putString("quest", a.getKey());
                at.put("progress", new IntArrayTag(a.getValue()));
                act.add(at);
            }
            c.put("active", act);
            list.add(c);
        }
        tag.put("players", list);
        return tag;
    }
}
