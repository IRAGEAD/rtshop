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

public record CampfireProgressPayload(BlockPos pos, boolean lit, int[] progress, int[] total) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130064_(this.pos);
        buf.writeBoolean(this.lit);
        for (int i = 0; i < 4; ++i) {
            buf.m_130130_(this.progress[i]);
            buf.m_130130_(this.total[i]);
        }
    }

    public static CampfireProgressPayload read(FriendlyByteBuf buf) {
        BlockPos pos = buf.m_130135_();
        boolean lit = buf.readBoolean();
        int[] progress = new int[4];
        int[] total = new int[4];
        for (int i = 0; i < 4; ++i) {
            progress[i] = buf.m_130242_();
            total[i] = buf.m_130242_();
        }
        return new CampfireProgressPayload(pos, lit, progress, total);
    }
}

