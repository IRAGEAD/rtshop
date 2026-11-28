/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.Connection
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.network.NetworkDirection
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.NetworkRegistry
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.network.simple.SimpleChannel
 */
package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.ClientBrewing;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.net.AtStationPayload;
import dev.dyslexic.cinematic.net.BrewingContentsPayload;
import dev.dyslexic.cinematic.net.CampfireProgressPayload;
import dev.dyslexic.cinematic.net.EnchantDisplayPayload;
import dev.dyslexic.cinematic.net.EnchantFxPayload;
import dev.dyslexic.cinematic.net.FurnaceContentsPayload;
import dev.dyslexic.cinematic.net.FurnaceStatePayload;
import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.net.Payload;
import dev.dyslexic.cinematic.net.SessionClosePayload;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.net.TableContentsPayload;
import dev.dyslexic.cinematic.station.Sessions;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    public static final String VERSION = "12";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel((ResourceLocation)Cinematic.id("main"), () -> "12", (Predicate)NetworkRegistry.acceptMissingOr((String)"12"), (Predicate)NetworkRegistry.acceptMissingOr((String)"12"));
    private static int nextId;

    private Network() {
    }

    public static boolean accepts(ServerPlayer player) {
        return CHANNEL.isRemotePresent(player.f_8906_.f_9742_);
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        if (Network.accepts(player)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), (Object)payload);
        }
    }

    public static void sendToTrackingChunk(ServerLevel level, ChunkPos pos, Payload payload) {
        for (ServerPlayer player : level.m_7726_().f_8325_.m_183262_(pos, false)) {
            Network.sendToPlayer(player, payload);
        }
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, Payload payload) {
        Level level = entity.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        for (ServerPlayer player : level2.m_7726_().f_8325_.m_183262_(entity.m_146902_(), false)) {
            if (player == entity) continue;
            Network.sendToPlayer(player, payload);
        }
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            Network.sendToPlayer(player, payload);
        }
    }

    public static void register() {
        Network.toServer(StationActionPayload.class, StationActionPayload::read, (payload, player) -> Sessions.handle(player, payload));
        Network.toClient(TableContentsPayload.class, TableContentsPayload::read, payload -> ClientPayloads.tableContents(payload));
        Network.toClient(FurnaceContentsPayload.class, FurnaceContentsPayload::read, payload -> ClientPayloads.furnaceContents(payload));
        Network.toClient(BrewingContentsPayload.class, BrewingContentsPayload::read, payload -> ClientBrewing.apply(payload));
        Network.toClient(FurnaceStatePayload.class, FurnaceStatePayload::read, payload -> ClientPayloads.furnaceState(payload));
        Network.toClient(CampfireProgressPayload.class, CampfireProgressPayload::read, payload -> ClientPayloads.campfireProgress(payload));
        Network.toClient(EnchantDisplayPayload.class, EnchantDisplayPayload::read, payload -> ClientPayloads.enchantDisplay(payload));
        Network.toClient(EnchantFxPayload.class, EnchantFxPayload::read, payload -> ClientPayloads.enchantFx(payload));
        Network.toClient(MenuDisplayPayload.class, MenuDisplayPayload::read, payload -> ClientPayloads.menuDisplay(payload));
        Network.toClient(MenuFxPayload.class, MenuFxPayload::read, payload -> ClientPayloads.menuFx(payload));
        Network.toClient(SessionClosePayload.class, SessionClosePayload::read, payload -> ClientPayloads.close(payload));
        Network.toClient(AtStationPayload.class, AtStationPayload::read, payload -> ClientPayloads.atStation(payload));
    }

    private static <T extends Payload> void toServer(Class<T> type, Function<FriendlyByteBuf, T> read, BiConsumer<T, ServerPlayer> handler) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(Payload::write).decoder(read).consumerMainThread((payload, context) -> {
            ServerPlayer player = ((NetworkEvent.Context)context.get()).getSender();
            if (player != null) {
                handler.accept(payload, player);
            }
        }).add();
    }

    private static <T extends Payload> void toClient(Class<T> type, Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(Payload::write).decoder(read).consumerMainThread((payload, context) -> handler.accept(payload)).add();
    }

    public static boolean present(Connection connection) {
        return CHANNEL.isRemotePresent(connection);
    }
}

