package com.rtshop.net;

import com.rtshop.land.Borders;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Supplier;

/** 서버 -> 클라이언트: 지금 보여 줄 토지 경계 모양들 (빈 목록 = 지우기). */
public final class BorderPacket {
    public final List<Borders.Shape> shapes;

    public BorderPacket(List<Borders.Shape> shapes) {
        this.shapes = shapes;
    }

    public static void encode(BorderPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.shapes.size());
        for (Borders.Shape s : m.shapes) {
            buf.writeInt(s.x0);
            buf.writeInt(s.z0);
            buf.writeInt(s.x1);
            buf.writeInt(s.z1);
            buf.writeInt(s.y0);
            buf.writeInt(s.y1);
            buf.writeInt(s.color);
            buf.writeByte(s.open);
            buf.writeBoolean(s.mask != null);
            if (s.mask != null) {
                buf.writeByteArray(s.mask.toByteArray());
            }
        }
    }

    public static BorderPacket decode(FriendlyByteBuf buf) {
        int n = Math.min(256, buf.readVarInt());
        List<Borders.Shape> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Borders.Shape s = new Borders.Shape();
            s.x0 = buf.readInt();
            s.z0 = buf.readInt();
            s.x1 = buf.readInt();
            s.z1 = buf.readInt();
            s.y0 = buf.readInt();
            s.y1 = buf.readInt();
            s.color = buf.readInt();
            s.open = buf.readByte();
            if (buf.readBoolean()) {
                s.mask = BitSet.valueOf(buf.readByteArray(1 << 16));
            }
            list.add(s);
        }
        return new BorderPacket(list);
    }

    public static void handle(BorderPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.BorderRenderer.set(msg.shapes)));
        ctx.get().setPacketHandled(true);
    }
}
