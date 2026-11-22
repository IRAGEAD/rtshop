package com.rtshop.raid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** world/data/rtshop_raid.dat : 플레이어별 입장 조건 처치 수, 주간 보상을 받은 주, 성공 횟수. */
public final class RaidData extends SavedData {
    private static final String NAME = "rtshop_raid";

    public static final class Entry {
        /** 몬스터 ID → 처치 수 (입장 조건). */
        public final Map<String, Integer> kills = new LinkedHashMap<>();
        /** 주간 보상을 마지막으로 받은 주 번호 (Weekly.id). */
        public long weeklyWeek = -1;
        public int clears;
    }

    private final Map<UUID, Entry> players = new LinkedHashMap<>();

    public static RaidData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RaidData::load, RaidData::new, NAME);
    }

    public Entry of(UUID id) {
        return players.computeIfAbsent(id, k -> new Entry());
    }

    public void touch() {
        setDirty();
    }

    public static RaidData load(CompoundTag tag) {
        RaidData d = new RaidData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("id")) continue;
            Entry e = d.of(c.getUUID("id"));
            CompoundTag k = c.getCompound("kills");
            for (String key : k.getAllKeys()) e.kills.put(key, k.getInt(key));
            e.weeklyWeek = c.getLong("weeklyWeek");
            e.clears = c.getInt("clears");
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> en : players.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", en.getKey());
            CompoundTag k = new CompoundTag();
            en.getValue().kills.forEach(k::putInt);
            c.put("kills", k);
            c.putLong("weeklyWeek", en.getValue().weeklyWeek);
            c.putInt("clears", en.getValue().clears);
            list.add(c);
        }
        tag.put("players", list);
        return tag;
    }
}
