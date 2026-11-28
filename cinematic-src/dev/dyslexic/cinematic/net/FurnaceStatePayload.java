/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 */
package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.net.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record FurnaceStatePayload(BlockPos pos, int litTime, int litDuration, int cookingProgress, int cookingTotalTime, String choice) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130064_(this.pos);
        buf.m_130130_(this.litTime);
        buf.m_130130_(this.litDuration);
        buf.m_130130_(this.cookingProgress);
        buf.m_130130_(this.cookingTotalTime);
        buf.m_130070_(this.choice);
    }

    public static FurnaceStatePayload read(FriendlyByteBuf buf) {
        return new FurnaceStatePayload(buf.m_130135_(), buf.m_130242_(), buf.m_130242_(), buf.m_130242_(), buf.m_130242_(), buf.m_130277_());
    }
}

