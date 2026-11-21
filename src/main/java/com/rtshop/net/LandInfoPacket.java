package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import com.rtshop.land.LandData;
import com.rtshop.land.LandManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** 서버 -> 클라이언트: 토지 설정 화면에 보여 줄 정보. */
public final class LandInfoPacket {

    public static final class MemberView {
        public UUID id;
        public String name;
        public int perms;
    }

    public static final class ClaimView {
        public String dim;
        public int x0, y0, z0, x1, y1, z1;
        public String type;
        /** 바닥 칸 수. */
        public int area;
    }

    public static final class View {
        public int id;
        public String name;
        public String ownerName;
        public boolean owner;
        public int myPerms;
        public int defaultPerms;
        /** 구획 토지면 반환할 때 돌려받는 금액, 아니면 -1. */
        public long plotRefund = -1;
        public final List<MemberView> members = new ArrayList<>();
        public final List<ClaimView> claims = new ArrayList<>();
    }

    private final List<View> views;
    private final int currentId;

    public LandInfoPacket(List<View> views, int currentId) {
        this.views = views;
        this.currentId = currentId;
    }

    public static List<View> views(List<LandData.Land> lands, ServerPlayer viewer) {
        List<View> out = new ArrayList<>();
        for (LandData.Land l : lands) {
            View v = new View();
            v.id = l.id;
            v.name = l.name;
            v.ownerName = l.forSale() ? "-" : l.ownerName;
            v.owner = LandManager.isOwner(viewer, l) || LandManager.bypass(viewer);
            v.myPerms = LandManager.perms(viewer, l);
            v.defaultPerms = l.defaultPerms;
            if (l.plot) {
                v.plotRefund = com.rtshop.land.PlotManager.refundOf(l);
            }
            if (v.owner) {
                for (LandData.Member m : l.members.values()) {
                    MemberView mv = new MemberView();
                    mv.id = m.id;
                    mv.name = m.name;
                    mv.perms = m.perms;
                    v.members.add(mv);
                }
            }
            for (LandData.Claim c : l.claims) {
                ClaimView cv = new ClaimView();
                cv.dim = c.dim;
                cv.x0 = c.x0;
                cv.y0 = c.y0;
                cv.z0 = c.z0;
                cv.x1 = c.x1;
                cv.y1 = c.y1;
                cv.z1 = c.z1;
                cv.type = c.type == null ? "normal" : c.type;
                cv.area = c.area();
                v.claims.add(cv);
            }
            out.add(v);
        }
        return out;
    }

    public static void encode(LandInfoPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.currentId);
        buf.writeVarInt(msg.views.size());
        for (View v : msg.views) {
            buf.writeVarInt(v.id);
            buf.writeUtf(v.name);
            buf.writeUtf(v.ownerName);
            buf.writeBoolean(v.owner);
            buf.writeVarInt(v.myPerms);
            buf.writeVarInt(v.defaultPerms);
            buf.writeLong(v.plotRefund);
            buf.writeVarInt(v.members.size());
            for (MemberView m : v.members) {
                buf.writeUUID(m.id);
                buf.writeUtf(m.name);
                buf.writeVarInt(m.perms);
            }
            buf.writeVarInt(v.claims.size());
            for (ClaimView c : v.claims) {
                buf.writeUtf(c.dim);
                buf.writeInt(c.x0);
                buf.writeInt(c.y0);
                buf.writeInt(c.z0);
                buf.writeInt(c.x1);
                buf.writeInt(c.y1);
                buf.writeInt(c.z1);
                buf.writeUtf(c.type);
                buf.writeVarInt(c.area);
            }
        }
    }

    public static LandInfoPacket decode(FriendlyByteBuf buf) {
        int current = buf.readVarInt();
        int n = buf.readVarInt();
        List<View> views = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            View v = new View();
            v.id = buf.readVarInt();
            v.name = buf.readUtf();
            v.ownerName = buf.readUtf();
            v.owner = buf.readBoolean();
            v.myPerms = buf.readVarInt();
            v.defaultPerms = buf.readVarInt();
            v.plotRefund = buf.readLong();
            int mn = buf.readVarInt();
            for (int j = 0; j < mn; j++) {
                MemberView m = new MemberView();
                m.id = buf.readUUID();
                m.name = buf.readUtf();
                m.perms = buf.readVarInt();
                v.members.add(m);
            }
            int cn = buf.readVarInt();
            for (int j = 0; j < cn; j++) {
                ClaimView c = new ClaimView();
                c.dim = buf.readUtf();
                c.x0 = buf.readInt();
                c.y0 = buf.readInt();
                c.z0 = buf.readInt();
                c.x1 = buf.readInt();
                c.y1 = buf.readInt();
                c.z1 = buf.readInt();
                c.type = buf.readUtf();
                c.area = buf.readVarInt();
                v.claims.add(c);
            }
            views.add(v);
        }
        return new LandInfoPacket(views, current);
    }

    public static void handle(LandInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openLand(msg.views, msg.currentId)));
        ctx.get().setPacketHandled(true);
    }
}
