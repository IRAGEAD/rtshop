package com.rtshop.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 월드 저장 데이터(world/data/rtshop_economy.dat): 플레이어별 소지금과 예금. */
public final class EconomyData extends SavedData {
    private static final String NAME = "rtshop_economy";

    private static final class Account {
        long wallet;
        long bank;
    }

    private final Map<UUID, Account> accounts = new HashMap<>();
    /** 발행되어 아직 쓰이지 않은 수표: 일련번호 -> 금액. */
    private final Map<UUID, Long> checks = new HashMap<>();

    public static EconomyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EconomyData::load, EconomyData::new, NAME);
    }

    private static EconomyData load(CompoundTag tag) {
        EconomyData d = new EconomyData();
        ListTag list = tag.getList("accounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Account a = new Account();
            a.wallet = Math.max(0, c.getLong("wallet"));
            a.bank = Math.max(0, c.getLong("bank"));
            d.accounts.put(c.getUUID("id"), a);
        }
        ListTag cl = tag.getList("checks", Tag.TAG_COMPOUND);
        for (int i = 0; i < cl.size(); i++) {
            CompoundTag c = cl.getCompound(i);
            if (c.hasUUID("id") && c.getLong("amount") > 0) {
                d.checks.put(c.getUUID("id"), c.getLong("amount"));
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Account> e : accounts.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putLong("wallet", e.getValue().wallet);
            c.putLong("bank", e.getValue().bank);
            list.add(c);
        }
        tag.put("accounts", list);
        ListTag cl = new ListTag();
        for (Map.Entry<UUID, Long> e : checks.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putLong("amount", e.getValue());
            cl.add(c);
        }
        tag.put("checks", cl);
        return tag;
    }

    public boolean has(UUID id) {
        return accounts.containsKey(id);
    }

    private Account acc(UUID id) {
        return accounts.computeIfAbsent(id, k -> new Account());
    }

    public long wallet(UUID id) {
        Account a = accounts.get(id);
        return a == null ? 0 : a.wallet;
    }

    public long bank(UUID id) {
        Account a = accounts.get(id);
        return a == null ? 0 : a.bank;
    }

    public void setWallet(UUID id, long v) {
        acc(id).wallet = Math.max(0, v);
        setDirty();
    }

    public void setBank(UUID id, long v) {
        acc(id).bank = Math.max(0, v);
        setDirty();
    }

    /** 수표 장부. 금액이 0 이하이면 지운다. */
    public void setCheck(UUID id, long amount) {
        if (amount <= 0) {
            checks.remove(id);
        } else {
            checks.put(id, amount);
        }
        setDirty();
    }

    /** 유효한 수표면 금액, 아니면 0. */
    public long checkAmount(UUID id) {
        Long v = checks.get(id);
        return v == null ? 0 : v;
    }

    public void removeAccount(UUID id) {
        if (accounts.remove(id) != null) {
            setDirty();
        }
    }
}
