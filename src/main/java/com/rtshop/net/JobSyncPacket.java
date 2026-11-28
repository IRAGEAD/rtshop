package com.rtshop.net;

import com.rtshop.job.ClientJobCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 서버 -> 클라이언트: 클라이언트 쪽에서도 알아야 하는 직업 정보.
 * 채굴 속도(블록이 빨리 깨지게)와, 지금 직업의 이름·패시브 설명(가방 창의 직업 딱지가 보여 준다).
 */
public final class JobSyncPacket {
    private final double mineSpeed;
    private final String combat, life;
    private final List<String> combatDesc, lifeDesc;
    /** 마법사가 아니라서 마법을 쓸 수 없는가 (3.49). */
    private final boolean spellLocked;
    /** 3.84: 음식 설명이 실제와 같게 — 요리사 음식 보너스와 양산 음식 규칙(켜짐 · 배수 · 아이템). */
    private final double foodBonus;
    private final boolean foodEnabled;
    private final double foodFactor;
    private final List<String> foodItems;
    /** 3.84 (M-34 · M-47 · M-64): 주문 물건의 설명 — 마법사인가, 마법사가 아닌 직업의 위력 · 시간 배수, 지팡이 없이 쓰는 위력. */
    private final boolean mage;
    private final double nonMagePower, nonMageCastTime, noStaffPower;

    public JobSyncPacket(double mineSpeed, String combat, List<String> combatDesc, String life, List<String> lifeDesc, boolean spellLocked) {
        this(mineSpeed, combat, combatDesc, life, lifeDesc, spellLocked, 0, true, 1, List.of());
    }

    public JobSyncPacket(double mineSpeed, String combat, List<String> combatDesc, String life, List<String> lifeDesc, boolean spellLocked,
                         double foodBonus, boolean foodEnabled, double foodFactor, List<String> foodItems) {
        this(mineSpeed, combat, combatDesc, life, lifeDesc, spellLocked, foodBonus, foodEnabled, foodFactor, foodItems, false, 0.6, 1.3, 0.6);
    }

    public JobSyncPacket(double mineSpeed, String combat, List<String> combatDesc, String life, List<String> lifeDesc, boolean spellLocked,
                         double foodBonus, boolean foodEnabled, double foodFactor, List<String> foodItems,
                         boolean mage, double nonMagePower, double nonMageCastTime, double noStaffPower) {
        this.mage = mage;
        this.nonMagePower = nonMagePower;
        this.nonMageCastTime = nonMageCastTime;
        this.noStaffPower = noStaffPower;
        this.foodBonus = foodBonus;
        this.foodEnabled = foodEnabled;
        this.foodFactor = foodFactor;
        this.foodItems = foodItems;
        this.spellLocked = spellLocked;
        this.mineSpeed = mineSpeed;
        this.combat = combat;
        this.life = life;
        this.combatDesc = combatDesc;
        this.lifeDesc = lifeDesc;
    }

    private static void write(FriendlyByteBuf buf, List<String> l) {
        buf.writeVarInt(l.size());
        for (String s : l) buf.writeUtf(s, 512);
    }

    private static List<String> read(FriendlyByteBuf buf) {
        int n = Math.min(64, buf.readVarInt());
        List<String> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(buf.readUtf(512));
        return l;
    }

    public static void encode(JobSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.mineSpeed);
        buf.writeUtf(msg.combat, 128);
        write(buf, msg.combatDesc);
        buf.writeUtf(msg.life, 128);
        write(buf, msg.lifeDesc);
        buf.writeBoolean(msg.spellLocked);
        buf.writeDouble(msg.foodBonus);
        buf.writeBoolean(msg.foodEnabled);
        buf.writeDouble(msg.foodFactor);
        write(buf, msg.foodItems);
        buf.writeBoolean(msg.mage);
        buf.writeDouble(msg.nonMagePower);
        buf.writeDouble(msg.nonMageCastTime);
        buf.writeDouble(msg.noStaffPower);
    }

    public static JobSyncPacket decode(FriendlyByteBuf buf) {
        double mine = buf.readDouble();
        String c = buf.readUtf(128);
        List<String> cd = read(buf);
        String l = buf.readUtf(128);
        List<String> ld = read(buf);
        boolean locked = buf.readBoolean();
        double fb = buf.readDouble();
        boolean fe = buf.readBoolean();
        double ff = buf.readDouble();
        List<String> fi = read(buf);
        return new JobSyncPacket(mine, c, cd, l, ld, locked, fb, fe, ff, fi, buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(JobSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientJobCache.mineSpeed = msg.mineSpeed;
            ClientJobCache.combat = msg.combat;
            ClientJobCache.life = msg.life;
            ClientJobCache.combatDesc = msg.combatDesc;
            ClientJobCache.lifeDesc = msg.lifeDesc;
            ClientJobCache.spellLocked = msg.spellLocked;
            ClientJobCache.mage = msg.mage;
            ClientJobCache.nonMagePower = msg.nonMagePower;
            ClientJobCache.nonMageCastTime = msg.nonMageCastTime;
            ClientJobCache.noStaffPower = msg.noStaffPower;
            com.rtshop.rules.FoodScale.clientCookBonus = msg.foodBonus;
            com.rtshop.rules.FoodScale.clientEnabled = msg.foodEnabled;
            com.rtshop.rules.FoodScale.clientFactor = msg.foodFactor;
            com.rtshop.rules.FoodScale.clientItems = java.util.Set.copyOf(msg.foodItems);
            com.rtshop.rules.FoodScale.clientSynced = true;
        });
        ctx.get().setPacketHandled(true);
    }
}
