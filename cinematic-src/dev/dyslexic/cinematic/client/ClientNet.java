/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientPacketListener
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.Payload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class ClientNet {
    private ClientNet() {
    }

    public static boolean serverHasChannel() {
        ClientPacketListener connection = Minecraft.m_91087_().m_91403_();
        return connection != null && Network.present(connection.m_104910_());
    }

    public static void sendToServer(Payload payload) {
        if (ClientNet.serverHasChannel()) {
            Network.CHANNEL.sendToServer((Object)payload);
        }
    }
}

