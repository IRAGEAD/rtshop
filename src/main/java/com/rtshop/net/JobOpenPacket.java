package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 서버 -> 클라이언트: 직업 화면을 열거나 새로 고침. */
public final class JobOpenPacket {
    public static final int LOCKED = 0;
    public static final int AVAILABLE = 1;
    public static final int CURRENT = 2;

    public record JobView(String id, String slot, String name, String icon, List<String> desc, int state, String lockQuest) {
    }

    public final boolean atNpc;
    public final String combat;
    public final String life;
    public final long combatCooldown;
    public final long lifeCooldown;
    public final long costCombat;
    public final long costLife;
    public final double hoursCombat;
    public final double hoursLife;
    public final List<JobView> jobs;
    /** 3.84: 내려놓은 직업 (같은 직업은 무료로 돌아온다, 다른 직업은 바꾸기와 같다). */
    public final String lastCombat, lastLife;

    public JobOpenPacket(boolean atNpc, String combat, String life, long combatCooldown, long lifeCooldown, long costCombat, long costLife,
                         double hoursCombat, double hoursLife, List<JobView> jobs) {
        this(atNpc, combat, life, combatCooldown, lifeCooldown, costCombat, costLife, hoursCombat, hoursLife, jobs, "", "");
    }

    public JobOpenPacket(boolean atNpc, String combat, String life, long combatCooldown, long lifeCooldown, long costCombat, long costLife,
                         double hoursCombat, double hoursLife, List<JobView> jobs, String lastCombat, String lastLife) {
        this.lastCombat = lastCombat == null ? "" : lastCombat;
        this.lastLife = lastLife == null ? "" : lastLife;
        this.atNpc = atNpc;
        this.combat = combat;
        this.life = life;
        this.combatCooldown = combatCooldown;
        this.lifeCooldown = lifeCooldown;
        this.costCombat = costCombat;
        this.costLife = costLife;
        this.hoursCombat = hoursCombat;
        this.hoursLife = hoursLife;
        this.jobs = jobs;
    }

    public static void encode(JobOpenPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.atNpc);
        buf.writeUtf(m.combat, 64);
        buf.writeUtf(m.life, 64);
        buf.writeVarLong(m.combatCooldown);
        buf.writeVarLong(m.lifeCooldown);
        buf.writeVarLong(m.costCombat);
        buf.writeVarLong(m.costLife);
        buf.writeDouble(m.hoursCombat);
        buf.writeDouble(m.hoursLife);
        buf.writeVarInt(m.jobs.size());
        for (JobView j : m.jobs) {
            buf.writeUtf(j.id(), 64);
            buf.writeUtf(j.slot(), 16);
            buf.writeUtf(j.name(), 128);
            buf.writeUtf(j.icon(), 128);
            buf.writeVarInt(j.desc().size());
            for (String d : j.desc()) {
                buf.writeUtf(d, 512);
            }
            buf.writeVarInt(j.state());
            buf.writeUtf(j.lockQuest(), 256);
        }
        buf.writeUtf(m.lastCombat, 64);
        buf.writeUtf(m.lastLife, 64);
    }

    public static JobOpenPacket decode(FriendlyByteBuf buf) {
        boolean atNpc = buf.readBoolean();
        String combat = buf.readUtf(64);
        String life = buf.readUtf(64);
        long cc = buf.readVarLong();
        long lc = buf.readVarLong();
        long costC = buf.readVarLong();
        long costL = buf.readVarLong();
        double hc = buf.readDouble();
        double hl = buf.readDouble();
        int n = buf.readVarInt();
        List<JobView> jobs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf(64);
            String slot = buf.readUtf(16);
            String name = buf.readUtf(128);
            String icon = buf.readUtf(128);
            int dn = buf.readVarInt();
            List<String> desc = new ArrayList<>();
            for (int k = 0; k < dn; k++) {
                desc.add(buf.readUtf(512));
            }
            int state = buf.readVarInt();
            String lock = buf.readUtf(256);
            jobs.add(new JobView(id, slot, name, icon, desc, state, lock));
        }
        return new JobOpenPacket(atNpc, combat, life, cc, lc, costC, costL, hc, hl, jobs, buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(JobOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openJobs(msg)));
        ctx.get().setPacketHandled(true);
    }
}
