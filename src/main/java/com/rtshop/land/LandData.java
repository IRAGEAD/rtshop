package com.rtshop.land;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 월드 저장 데이터(world/data/rtshop_land.dat): 토지, 권한, 보관함 PIN. */
public final class LandData extends SavedData {
    private static final String NAME = "rtshop_land";

    // 권한 비트
    public static final int BREAK = 1;
    public static final int PLACE = 2;
    public static final int CONTAINER = 4;
    public static final int USE = 8;
    public static final int ENTITY = 16;
    public static final int ALL = 31;

    /** 토지 한 구역(직육면체, 좌표는 양 끝 포함). */
    public static final class Claim {
        public int landId;
        public String dim;
        public int x0, y0, z0, x1, y1, z1;
        public String type;

        public boolean contains(int x, int y, int z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }

        public boolean intersects(int ax0, int ay0, int az0, int ax1, int ay1, int az1) {
            return ax0 <= x1 && ax1 >= x0 && ay0 <= y1 && ay1 >= y0 && az0 <= z1 && az1 >= z0;
        }

        /** 면이 맞닿아 있는가 (모서리·꼭짓점만 닿는 것은 제외). */
        public boolean touches(Claim o) {
            if (!dim.equals(o.dim)) {
                return false;
            }
            boolean ox = x0 <= o.x1 && x1 >= o.x0;
            boolean oy = y0 <= o.y1 && y1 >= o.y0;
            boolean oz = z0 <= o.z1 && z1 >= o.z0;
            boolean ax = x1 + 1 == o.x0 || o.x1 + 1 == x0;
            boolean ay = y1 + 1 == o.y0 || o.y1 + 1 == y0;
            boolean az = z1 + 1 == o.z0 || o.z1 + 1 == z0;
            return (ax && oy && oz) || (ay && ox && oz) || (az && ox && oy);
        }
    }

    public static final class Member {
        public UUID id;
        public String name;
        public int perms;
    }

    /** 토지(맞닿은 구역들의 묶음). 권한 설정은 토지 단위. */
    public static final class Land {
        public int id;
        public UUID owner;
        public String ownerName = "";
        public String name = "";
        public int defaultPerms = 0;
        public final Map<UUID, Member> members = new LinkedHashMap<>();
        public final List<Claim> claims = new ArrayList<>();
    }

    public static final class Pin {
        public String salt;
        public String hash;
    }

    private final Map<Integer, Land> lands = new LinkedHashMap<>();
    private final Map<String, Map<Long, List<Claim>>> index = new HashMap<>();
    private final Map<String, Pin> pins = new HashMap<>();
    private final Set<UUID> firstDeed = new HashSet<>();
    private int nextId = 1;

    public static LandData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(LandData::load, LandData::new, NAME);
    }

    // ------------------------------------------------------------------ 조회

    private static long chunkKey(int cx, int cz) {
        return ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
    }

    public Claim claimAt(String dim, int x, int y, int z) {
        Map<Long, List<Claim>> m = index.get(dim);
        if (m == null) {
            return null;
        }
        List<Claim> list = m.get(chunkKey(x >> 4, z >> 4));
        if (list == null) {
            return null;
        }
        for (Claim c : list) {
            if (c.contains(x, y, z)) {
                return c;
            }
        }
        return null;
    }

    public Claim claimAt(String dim, BlockPos pos) {
        return claimAt(dim, pos.getX(), pos.getY(), pos.getZ());
    }

    public Land land(int id) {
        return lands.get(id);
    }

    public Land landAt(String dim, BlockPos pos) {
        Claim c = claimAt(dim, pos);
        return c == null ? null : lands.get(c.landId);
    }

    public Iterable<Land> lands() {
        return lands.values();
    }

    public List<Land> landsOf(UUID owner) {
        List<Land> out = new ArrayList<>();
        for (Land l : lands.values()) {
            if (l.owner.equals(owner)) {
                out.add(l);
            }
        }
        return out;
    }

    public int claimCount(UUID owner) {
        int n = 0;
        for (Land l : lands.values()) {
            if (l.owner.equals(owner)) {
                n += l.claims.size();
            }
        }
        return n;
    }

    /** 주어진 상자와 겹치거나(grow=0) 일정 거리 안에 있는 구역들. */
    public List<Claim> claimsNear(String dim, int x0, int y0, int z0, int x1, int y1, int z1, int grow) {
        List<Claim> out = new ArrayList<>();
        Map<Long, List<Claim>> m = index.get(dim);
        if (m == null) {
            return out;
        }
        int ax0 = x0 - grow, ay0 = y0 - grow, az0 = z0 - grow, ax1 = x1 + grow, ay1 = y1 + grow, az1 = z1 + grow;
        for (int cx = ax0 >> 4; cx <= ax1 >> 4; cx++) {
            for (int cz = az0 >> 4; cz <= az1 >> 4; cz++) {
                List<Claim> list = m.get(chunkKey(cx, cz));
                if (list == null) {
                    continue;
                }
                for (Claim c : list) {
                    if (c.intersects(ax0, ay0, az0, ax1, ay1, az1) && !out.contains(c)) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 변경

    private void indexClaim(Claim c) {
        Map<Long, List<Claim>> m = index.computeIfAbsent(c.dim, k -> new HashMap<>());
        for (int cx = c.x0 >> 4; cx <= c.x1 >> 4; cx++) {
            for (int cz = c.z0 >> 4; cz <= c.z1 >> 4; cz++) {
                m.computeIfAbsent(chunkKey(cx, cz), k -> new ArrayList<>()).add(c);
            }
        }
    }

    private void unindexClaim(Claim c) {
        Map<Long, List<Claim>> m = index.get(c.dim);
        if (m == null) {
            return;
        }
        for (int cx = c.x0 >> 4; cx <= c.x1 >> 4; cx++) {
            for (int cz = c.z0 >> 4; cz <= c.z1 >> 4; cz++) {
                List<Claim> list = m.get(chunkKey(cx, cz));
                if (list != null) {
                    list.remove(c);
                }
            }
        }
    }

    public Land createLand(UUID owner, String ownerName) {
        Land l = new Land();
        l.id = nextId++;
        l.owner = owner;
        l.ownerName = ownerName;
        l.name = "#" + l.id;
        lands.put(l.id, l);
        setDirty();
        return l;
    }

    public void addClaim(Land land, Claim c) {
        c.landId = land.id;
        land.claims.add(c);
        indexClaim(c);
        setDirty();
    }

    /** b 의 구역을 모두 a 로 옮기고 b 를 없앤다. */
    public void merge(Land a, Land b) {
        if (a == b) {
            return;
        }
        for (Claim c : b.claims) {
            c.landId = a.id;
            a.claims.add(c);
        }
        b.claims.clear();
        lands.remove(b.id);
        setDirty();
    }

    public void removeLand(Land land) {
        for (Claim c : land.claims) {
            unindexClaim(c);
            // 구역 안의 PIN 도 지운다
            pins.keySet().removeIf(k -> {
                String[] p = k.split("\\|");
                if (p.length != 2 || !p[0].equals(c.dim)) {
                    return false;
                }
                String[] xyz = p[1].split(",");
                try {
                    return c.contains(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                } catch (RuntimeException ex) {
                    return false;
                }
            });
        }
        lands.remove(land.id);
        setDirty();
    }

    public void touch() {
        setDirty();
    }

    // ------------------------------------------------------------------ PIN / 첫 토지

    public static String pinKey(String dim, BlockPos pos) {
        return dim + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public Pin pin(String dim, BlockPos pos) {
        return pins.get(pinKey(dim, pos));
    }

    public void setPin(String dim, BlockPos pos, Pin pin) {
        if (pin == null) {
            pins.remove(pinKey(dim, pos));
        } else {
            pins.put(pinKey(dim, pos), pin);
        }
        setDirty();
    }

    public boolean firstDeedUsed(UUID id) {
        return firstDeed.contains(id);
    }

    public void markFirstDeed(UUID id) {
        if (firstDeed.add(id)) {
            setDirty();
        }
    }

    public void clearFirstDeed(UUID id) {
        if (firstDeed.remove(id)) {
            setDirty();
        }
    }

    // ------------------------------------------------------------------ 저장

    public static LandData load(CompoundTag tag) {
        LandData d = new LandData();
        d.nextId = Math.max(1, tag.getInt("nextId"));
        ListTag ll = tag.getList("lands", Tag.TAG_COMPOUND);
        for (int i = 0; i < ll.size(); i++) {
            CompoundTag lt = ll.getCompound(i);
            Land l = new Land();
            l.id = lt.getInt("id");
            l.owner = lt.getUUID("owner");
            l.ownerName = lt.getString("ownerName");
            l.name = lt.getString("name");
            l.defaultPerms = lt.getInt("defaultPerms");
            ListTag ml = lt.getList("members", Tag.TAG_COMPOUND);
            for (int j = 0; j < ml.size(); j++) {
                CompoundTag mt = ml.getCompound(j);
                Member m = new Member();
                m.id = mt.getUUID("id");
                m.name = mt.getString("name");
                m.perms = mt.getInt("perms");
                l.members.put(m.id, m);
            }
            ListTag cl = lt.getList("claims", Tag.TAG_COMPOUND);
            for (int j = 0; j < cl.size(); j++) {
                CompoundTag ct = cl.getCompound(j);
                Claim c = new Claim();
                c.landId = l.id;
                c.dim = ct.getString("dim");
                int[] b = ct.getIntArray("box");
                if (b.length != 6) {
                    continue;
                }
                c.x0 = b[0];
                c.y0 = b[1];
                c.z0 = b[2];
                c.x1 = b[3];
                c.y1 = b[4];
                c.z1 = b[5];
                c.type = ct.getString("type");
                l.claims.add(c);
                d.indexClaim(c);
            }
            d.lands.put(l.id, l);
            d.nextId = Math.max(d.nextId, l.id + 1);
        }
        ListTag pl = tag.getList("pins", Tag.TAG_COMPOUND);
        for (int i = 0; i < pl.size(); i++) {
            CompoundTag pt = pl.getCompound(i);
            Pin p = new Pin();
            p.salt = pt.getString("salt");
            p.hash = pt.getString("hash");
            d.pins.put(pt.getString("key"), p);
        }
        ListTag fl = tag.getList("firstDeed", Tag.TAG_COMPOUND);
        for (int i = 0; i < fl.size(); i++) {
            d.firstDeed.add(fl.getCompound(i).getUUID("id"));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("nextId", nextId);
        ListTag ll = new ListTag();
        for (Land l : lands.values()) {
            CompoundTag lt = new CompoundTag();
            lt.putInt("id", l.id);
            lt.putUUID("owner", l.owner);
            lt.putString("ownerName", l.ownerName);
            lt.putString("name", l.name);
            lt.putInt("defaultPerms", l.defaultPerms);
            ListTag ml = new ListTag();
            for (Member m : l.members.values()) {
                CompoundTag mt = new CompoundTag();
                mt.putUUID("id", m.id);
                mt.putString("name", m.name);
                mt.putInt("perms", m.perms);
                ml.add(mt);
            }
            lt.put("members", ml);
            ListTag cl = new ListTag();
            for (Claim c : l.claims) {
                CompoundTag ct = new CompoundTag();
                ct.putString("dim", c.dim);
                ct.putIntArray("box", new int[]{c.x0, c.y0, c.z0, c.x1, c.y1, c.z1});
                ct.putString("type", c.type == null ? "normal" : c.type);
                cl.add(ct);
            }
            lt.put("claims", cl);
            ll.add(lt);
        }
        tag.put("lands", ll);
        ListTag pl = new ListTag();
        for (Map.Entry<String, Pin> e : pins.entrySet()) {
            CompoundTag pt = new CompoundTag();
            pt.putString("key", e.getKey());
            pt.putString("salt", e.getValue().salt);
            pt.putString("hash", e.getValue().hash);
            pl.add(pt);
        }
        tag.put("pins", pl);
        ListTag fl = new ListTag();
        for (UUID id : firstDeed) {
            CompoundTag ft = new CompoundTag();
            ft.putUUID("id", id);
            fl.add(ft);
        }
        tag.put("firstDeed", fl);
        return tag;
    }
}
