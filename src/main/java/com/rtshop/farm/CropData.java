package com.rtshop.farm;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 월드 저장 데이터(world/data/rtshop_crops.dat): 상점 씨앗으로 심은 작물의 위치.
 * 이 표시가 있는 작물만 "정식으로 심은 작물"로 인정해 별 등급을 매기고,
 * 다른 모드가 씨앗 없이 다시 심어 놓은 싹은 표시가 없으므로 치운다.
 */
public final class CropData extends SavedData {
    private static final String NAME = "rtshop_crops";
    /** 여러 칸짜리 작물(벼, 덩굴 토마토 등)은 맨 아래 칸에만 표시가 있으므로 아래로 이만큼 찾아본다. */
    private static final int CHAIN = 4;

    private final Map<String, LongOpenHashSet> marks = new HashMap<>();

    public static CropData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CropData::load, CropData::new, NAME);
    }

    public static CropData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static String dim(ServerLevel level) {
        return level.dimension().location().toString();
    }

    public boolean isMarked(String dim, BlockPos pos) {
        LongOpenHashSet set = marks.get(dim);
        return set != null && set.contains(pos.asLong());
    }

    public void mark(String dim, BlockPos pos) {
        if (marks.computeIfAbsent(dim, k -> new LongOpenHashSet()).add(pos.asLong())) {
            setDirty();
        }
    }

    public boolean unmark(String dim, BlockPos pos) {
        LongOpenHashSet set = marks.get(dim);
        if (set != null && set.remove(pos.asLong())) {
            setDirty();
            return true;
        }
        return false;
    }

    /** pos 또는 그 아래 몇 칸 안에서 표시된 위치를 찾는다. 없으면 null. */
    public BlockPos findMark(String dim, BlockPos pos) {
        LongOpenHashSet set = marks.get(dim);
        if (set == null || set.isEmpty()) {
            return null;
        }
        for (int i = 0; i <= CHAIN; i++) {
            BlockPos p = i == 0 ? pos : pos.below(i);
            if (set.contains(p.asLong())) {
                return p;
            }
        }
        return null;
    }

    public int count() {
        int n = 0;
        for (LongOpenHashSet s : marks.values()) {
            n += s.size();
        }
        return n;
    }

    public static CropData load(CompoundTag tag) {
        CropData d = new CropData();
        CompoundTag dims = tag.getCompound("dims");
        for (String key : dims.getAllKeys()) {
            long[] arr = dims.getLongArray(key);
            if (arr.length > 0) {
                d.marks.put(key, new LongOpenHashSet(arr));
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag dims = new CompoundTag();
        for (Map.Entry<String, LongOpenHashSet> e : marks.entrySet()) {
            if (!e.getValue().isEmpty()) {
                dims.putLongArray(e.getKey(), e.getValue().toLongArray());
            }
        }
        tag.put("dims", dims);
        return tag;
    }
}
