package com.rtshop.net;

import com.rtshop.client.EnhanceFx;
import com.rtshop.enhance.Enhance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 강화 설정(CONFIG) 또는 강화 결과 연출(FX). */
public final class EnhancePacket {
    public static final int CONFIG = 0;
    public static final int FX = 1;
    public static final int EDITOR = 2;
    public static final int ODDS = 3;
    public static final int GUARD = 4;
    /** 누군가 최고 단계 강화에 성공: json = 그 플레이어 이름, to = 단계, item = 장비. */
    public static final int LEGEND = 5;

    public final int mode;
    public final String json;
    public final int outcome;
    public final boolean guarded;
    public final int from;
    public final int to;
    public final ItemStack item;

    private EnhancePacket(int mode, String json, int outcome, boolean guarded, int from, int to, ItemStack item) {
        this.mode = mode;
        this.json = json;
        this.outcome = outcome;
        this.guarded = guarded;
        this.from = from;
        this.to = to;
        this.item = item;
    }

    public static EnhancePacket config(String json) {
        return new EnhancePacket(CONFIG, json, 0, false, 0, 0, ItemStack.EMPTY);
    }

    public static EnhancePacket guard(int mask) {
        return new EnhancePacket(GUARD, "", mask, false, 0, 0, ItemStack.EMPTY);
    }

    public static EnhancePacket legend(String player, int level, ItemStack item) {
        return new EnhancePacket(LEGEND, player, 0, false, 0, level, item);
    }

    public static EnhancePacket open(int mode, String json) {
        return new EnhancePacket(mode, json, 0, false, 0, 0, ItemStack.EMPTY);
    }

    public static EnhancePacket fx(Enhance.Outcome o, boolean guarded, int from, int to, ItemStack item) {
        return new EnhancePacket(FX, "", o.ordinal(), guarded, from, to, item);
    }

    public static void encode(EnhancePacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.mode);
        Packed.write(buf, m.json);
        buf.writeVarInt(m.outcome);
        buf.writeBoolean(m.guarded);
        buf.writeVarInt(m.from);
        buf.writeVarInt(m.to);
        buf.writeItem(m.item);
    }

    public static EnhancePacket decode(FriendlyByteBuf buf) {
        return new EnhancePacket(buf.readVarInt(), Packed.read(buf), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                buf.readItem());
    }

    public static void handle(EnhancePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EnhanceFx.handle(msg)));
        ctx.get().setPacketHandled(true);
    }
}
