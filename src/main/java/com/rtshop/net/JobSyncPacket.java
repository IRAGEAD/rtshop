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

    public JobSyncPacket(double mineSpeed, String combat, List<String> combatDesc, String life, List<String> lifeDesc, boolean spellLocked) {
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
    }

    public static JobSyncPacket decode(FriendlyByteBuf buf) {
        double mine = buf.readDouble();
        String c = buf.readUtf(128);
        List<String> cd = read(buf);
        String l = buf.readUtf(128);
        List<String> ld = read(buf);
        return new JobSyncPacket(mine, c, cd, l, ld, buf.readBoolean());
    }

    public static void handle(JobSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientJobCache.mineSpeed = msg.mineSpeed;
            ClientJobCache.combat = msg.combat;
            ClientJobCache.life = msg.life;
            ClientJobCache.combatDesc = msg.combatDesc;
            ClientJobCache.lifeDesc = msg.lifeDesc;
            ClientJobCache.spellLocked = msg.spellLocked;
        });
        ctx.get().setPacketHandled(true);
    }
}
