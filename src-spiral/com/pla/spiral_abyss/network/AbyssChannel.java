/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.injectables.annotations.ExpectPlatform
 *  dev.architectury.injectables.annotations.ExpectPlatform$Transformed
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 */
package com.pla.spiral_abyss.network;

import com.pla.spiral_abyss.network.forge.AbyssChannelImpl;
import dev.architectury.injectables.annotations.ExpectPlatform;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public abstract class AbyssChannel {
    @ExpectPlatform
    @ExpectPlatform.Transformed
    public static AbyssChannel create(String version) {
        return AbyssChannelImpl.create(version);
    }

    protected abstract <T> void register(Registration<T> var1);

    public abstract void sendToPlayer(ServerPlayer var1, Object var2);

    public abstract void sendToServer(Object var1);

    public abstract void broadcast(Object var1);

    public <T> Builder<T> messageBuilder(Class<T> type, int id, Direction direction) {
        return new Builder<T>(type, id, direction);
    }

    public final class Builder<T> {
        private final Class<T> type;
        private final int id;
        private final Direction direction;
        private BiConsumer<T, FriendlyByteBuf> encoder;
        private Function<FriendlyByteBuf, T> decoder;
        private BiConsumer<T, Supplier<Context>> handler;

        private Builder(Class<T> type, int id, Direction direction) {
            this.type = type;
            this.id = id;
            this.direction = direction;
        }

        public Builder<T> encoder(BiConsumer<T, FriendlyByteBuf> value) {
            this.encoder = value;
            return this;
        }

        public Builder<T> decoder(Function<FriendlyByteBuf, T> value) {
            this.decoder = value;
            return this;
        }

        public Builder<T> consumerMainThread(BiConsumer<T, Supplier<Context>> value) {
            this.handler = value;
            return this;
        }

        public void add() {
            AbyssChannel.this.register(new Registration<T>(this.type, this.id, this.direction, this.encoder, this.decoder, this.handler));
        }
    }

    public static enum Direction {
        PLAY_TO_CLIENT,
        PLAY_TO_SERVER;

    }

    public record Registration<T>(Class<T> type, int id, Direction direction, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<Context>> handler) {
    }

    public record Context(ServerPlayer sender) {
        public ServerPlayer getSender() {
            return this.sender;
        }
    }
}

