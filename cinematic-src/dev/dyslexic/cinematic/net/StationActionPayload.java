/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 */
package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.net.Payload;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record StationActionPayload(BlockPos pos, StationKind kind, Action action, int index, int button, boolean shift, int side) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130064_(this.pos);
        buf.m_130068_((Enum)this.kind);
        buf.m_130068_((Enum)this.action);
        buf.m_130130_(this.index);
        buf.writeByte(this.button);
        buf.writeBoolean(this.shift);
        buf.writeByte(this.side);
    }

    public static StationActionPayload read(FriendlyByteBuf buf) {
        return new StationActionPayload(buf.m_130135_(), (StationKind)buf.m_130066_(StationKind.class), (Action)buf.m_130066_(Action.class), buf.m_130242_(), buf.readByte(), buf.readBoolean(), buf.readByte());
    }

    public static StationActionPayload of(BlockPos pos, StationKind kind, Action action) {
        return new StationActionPayload(pos, kind, action, 0, 0, false, 0);
    }

    public static enum Action {
        BEGIN,
        CLICK,
        PAINT,
        CRAFT,
        LEAVE,
        WINDOW,
        PLACE,
        SPREAD,
        QUICK,
        CHOOSE;

    }
}

