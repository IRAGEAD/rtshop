/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 */
package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.net.Payload;
import net.minecraft.network.FriendlyByteBuf;

public record SessionClosePayload() implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
    }

    public static SessionClosePayload read(FriendlyByteBuf buf) {
        return new SessionClosePayload();
    }
}

