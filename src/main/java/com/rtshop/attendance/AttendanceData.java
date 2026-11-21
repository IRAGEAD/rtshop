package com.rtshop.attendance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 월드 저장 데이터(world/data/rtshop_attendance.dat): 플레이어별 출석 진행도. */
public final class AttendanceData extends SavedData {
    private static final String NAME = "rtshop_attendance";

    public static final class Entry {
        /** 지금 연속 출석 일수 (0 = 0일차). */
        public int streak;
        /** 지금까지 출석한 날 수 (기록용). */
        public int total;
        /** 마지막으로 출석한 출석일 번호 (기준 시각으로 나눈 날짜, -1 = 없음). */
        public long lastDay = -1;
        /** 이번 연속 출석에서 '하루 봐주기'를 이미 썼는지. */
        public boolean graceUsed;
        /** 초기화되기 전 연속 출석 일수와 초기화된 시각(ms). 복구권이 쓴다. 0 = 없음. */
        public int savedStreak;
        public long resetAt;
        /** 복구권을 마지막으로 쓴 시각(ms). */
        public long restoreAt;
        /** 이번 주(주 시작일의 날짜 번호)와 출석한 요일(비트 0~6), 받은 주간 보상(출석 일수 d 를 받았으면 비트 d-1). */
        public long week = -1;
        public int weekMask;
        public int weekClaimed;
        /** 마지막으로 '오늘 출석하세요' 안내를 보낸 출석일. */
        public long remindedDay = -1;

        public int weekCount() {
            return Integer.bitCount(weekMask);
        }
    }

    private final Map<UUID, Entry> players = new LinkedHashMap<>();

    public static AttendanceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AttendanceData::load, AttendanceData::new, NAME);
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

    public static AttendanceData load(CompoundTag tag) {
        AttendanceData d = new AttendanceData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("id")) {
                continue;
            }
            Entry e = d.of(c.getUUID("id"));
            e.streak = c.getInt("streak");
            e.total = c.getInt("total");
            e.lastDay = c.contains("lastDay") ? c.getLong("lastDay") : -1;
            e.graceUsed = c.getBoolean("graceUsed");
            e.savedStreak = c.getInt("savedStreak");
            e.resetAt = c.getLong("resetAt");
            e.restoreAt = c.getLong("restoreAt");
            e.week = c.contains("week") ? c.getLong("week") : -1;
            e.weekMask = c.getInt("weekMask");
            e.weekClaimed = c.getInt("weekClaimed");
            e.remindedDay = c.contains("remindedDay") ? c.getLong("remindedDay") : -1;
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
            c.putInt("streak", e.streak);
            c.putInt("total", e.total);
            c.putLong("lastDay", e.lastDay);
            c.putBoolean("graceUsed", e.graceUsed);
            c.putInt("savedStreak", e.savedStreak);
            c.putLong("resetAt", e.resetAt);
            c.putLong("restoreAt", e.restoreAt);
            c.putLong("week", e.week);
            c.putInt("weekMask", e.weekMask);
            c.putInt("weekClaimed", e.weekClaimed);
            c.putLong("remindedDay", e.remindedDay);
            list.add(c);
        }
        tag.put("players", list);
        return tag;
    }
}
