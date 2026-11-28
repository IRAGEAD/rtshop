/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkDirection
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.NetworkRegistry
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.network.simple.SimpleChannel
 */
package com.pla.spiral_abyss.network.forge;

import com.pla.spiral_abyss.network.AbyssChannel;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AbyssChannelImpl
extends AbyssChannel {
    private final SimpleChannel channel = NetworkRegistry.newSimpleChannel((ResourceLocation)new ResourceLocation("spiral_abyss", "main"), () -> version, version::equals, version::equals);

    private AbyssChannelImpl(String version) {
    }

    public static AbyssChannel create(String version) {
        return new AbyssChannelImpl(version);
    }

    @Override
    protected <T> void register(AbyssChannel.Registration<T> r) {
        this.channel.messageBuilder(r.type(), r.id(), r.direction() == AbyssChannel.Direction.PLAY_TO_CLIENT ? NetworkDirection.PLAY_TO_CLIENT : NetworkDirection.PLAY_TO_SERVER).encoder(r.encoder()).decoder(r.decoder()).consumerMainThread((message, context) -> r.handler().accept(message, () -> AbyssChannelImpl.lambda$register$1((Supplier)context))).add();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        this.channel.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public void sendToServer(Object message) {
        this.channel.sendToServer(message);
    }

    @Override
    public void broadcast(Object message) {
        this.channel.send(PacketDistributor.ALL.noArg(), message);
    }

    private static /* synthetic */ AbyssChannel.Context lambda$register$1(Supplier context) {
        return new AbyssChannel.Context(((NetworkEvent.Context)context.get()).getSender());
    }
}

