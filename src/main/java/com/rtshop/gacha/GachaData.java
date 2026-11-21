package com.rtshop.gacha;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 월드 저장 데이터(world/data/rtshop_gacha.dat): 플레이어별 뽑은 횟수(천장 구상용), 모은 아이템(NEW 표시), 최근 기록. */
public final class GachaData extends SavedData {
    private static final String NAME = "rtshop_gacha";
    public static final int HISTORY = 50;

    public static final class Entry {
        /** 뽑기 종류·단계별 뽑은 횟수 ("weapon/high" → 횟수). 나중에 천장을 넣을 때 쓴다. */
        public final Map<String, Integer> pulls = new LinkedHashMap<>();
        /** 한 번이라도 뽑은 칸 (Entry.key). */
        public final Set<String> owned = new HashSet<>();
        /** 최근 기록 (새것이 앞): "등급|종류|key|시각" */
        public final List<String> history = new ArrayList<>();
    }

    private final Map<UUID, Entry> players = new LinkedHashMap<>();

    public static GachaData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(GachaData::load, GachaData::new, NAME);
    }

    public Entry of(UUID id) {
        return players.computeIfAbsent(id, k -> new Entry());
    }

    public void dirty() {
        setDirty();
    }

    public void clear(UUID id) {
        players.remove(id);
        setDirty();
    }

    public static GachaData load(CompoundTag tag) {
        GachaData d = new GachaData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("id")) {
                continue;
            }
            Entry e = d.of(c.getUUID("id"));
            CompoundTag pulls = c.getCompound("pulls");
            for (String k : pulls.getAllKeys()) {
                e.pulls.put(k, pulls.getInt(k));
            }
            ListTag owned = c.getList("owned", Tag.TAG_STRING);
            for (int j = 0; j < owned.size(); j++) {
                e.owned.add(owned.getString(j));
            }
            ListTag hist = c.getList("history", Tag.TAG_STRING);
            for (int j = 0; j < hist.size(); j++) {
                e.history.add(hist.getString(j));
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> en : players.entrySet()) {
            Entry e = en.getValue();
            CompoundTag c = new CompoundTag();
            c.putUUID("id", en.getKey());
            CompoundTag pulls = new CompoundTag();
            e.pulls.forEach(pulls::putInt);
            c.put("pulls", pulls);
            ListTag owned = new ListTag();
            for (String s : e.owned) {
                owned.add(StringTag.valueOf(s));
            }
            c.put("owned", owned);
            ListTag hist = new ListTag();
            for (String s : e.history) {
                hist.add(StringTag.valueOf(s));
            }
            c.put("history", hist);
            list.add(c);
        }
        tag.put("players", list);
        return tag;
    }
}
