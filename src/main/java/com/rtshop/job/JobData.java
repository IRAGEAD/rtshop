package com.rtshop.job;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 월드 저장 데이터(world/data/rtshop_jobs.dat): 플레이어별 전투·생활 직업과 마지막으로 바꾼 시각. */
public final class JobData extends SavedData {
    private static final String NAME = "rtshop_jobs";

    public static final class Entry {
        public String combat = "";
        public String life = "";
        /** 마지막으로 '바꾼'(처음 고른 것 제외) 시각, 실제 시간 ms. 0 = 아직 바꾼 적 없음. */
        public long combatChangedAt;
        public long lifeChangedAt;

        public String get(String slot) {
            return JobConfig.COMBAT.equals(slot) ? combat : life;
        }

        public long changedAt(String slot) {
            return JobConfig.COMBAT.equals(slot) ? combatChangedAt : lifeChangedAt;
        }

        public void set(String slot, String id, long changedAt) {
            if (JobConfig.COMBAT.equals(slot)) {
                combat = id;
                combatChangedAt = changedAt;
            } else {
                life = id;
                lifeChangedAt = changedAt;
            }
        }
    }

    private final Map<UUID, Entry> players = new LinkedHashMap<>();

    public static JobData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(JobData::load, JobData::new, NAME);
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

    /** 이 칸에 이 직업을 가진 사람 수 (나를 빼고, 히든 직업의 서버 인원 한도 · 3.76.0). */
    public int count(String slot, String jobId, UUID except) {
        int n = 0;
        for (Map.Entry<UUID, Entry> e : players.entrySet()) {
            if (!e.getKey().equals(except) && jobId.equals(e.getValue().get(slot))) n++;
        }
        return n;
    }

    public static JobData load(CompoundTag tag) {
        JobData d = new JobData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("id")) {
                continue;
            }
            Entry e = d.of(c.getUUID("id"));
            e.combat = c.getString("combat");
            e.life = c.getString("life");
            e.combatChangedAt = c.getLong("combatAt");
            e.lifeChangedAt = c.getLong("lifeAt");
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> e : players.entrySet()) {
            Entry v = e.getValue();
            if (v.combat.isEmpty() && v.life.isEmpty()) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putString("combat", v.combat);
            c.putString("life", v.life);
            c.putLong("combatAt", v.combatChangedAt);
            c.putLong("lifeAt", v.lifeChangedAt);
            list.add(c);
        }
        tag.put("players", list);
        return tag;
    }
}
