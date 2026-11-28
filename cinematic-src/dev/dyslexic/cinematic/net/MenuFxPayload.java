/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.item.ItemStack
 */
package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.net.Payload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record MenuFxPayload(BlockPos pos, StationKind kind, int entityId, Cue cue, int variant, List<ItemStack> items, ItemStack result) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130064_(this.pos);
        buf.m_130068_((Enum)this.kind);
        buf.m_130130_(this.entityId);
        buf.m_130068_((Enum)this.cue);
        buf.m_130130_(this.variant);
        buf.m_236828_(this.items, FriendlyByteBuf::m_130055_);
        buf.m_130055_(this.result);
    }

    public static MenuFxPayload read(FriendlyByteBuf buf) {
        return new MenuFxPayload(buf.m_130135_(), (StationKind)buf.m_130066_(StationKind.class), buf.m_130242_(), (Cue)buf.m_130066_(Cue.class), buf.m_130242_(), buf.m_236845_(FriendlyByteBuf::m_130267_), buf.m_130267_());
    }

    public static MenuFxPayload beat(BlockPos pos, StationKind kind, int entityId) {
        return new MenuFxPayload(pos, kind, entityId, Cue.BEAT, 0, List.of(), ItemStack.f_41583_);
    }

    public static enum Cue {
        BEAT,
        MOVE,
        PULL,
        PUT,
        READ,
        READ_END;

    }
}

