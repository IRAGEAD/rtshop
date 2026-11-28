/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 */
package net.spell_engine.network;

import com.google.common.collect.Iterables;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellAssignments;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.delivery.melee.Melee;
import net.spell_engine.network.Packets;

public class ServerNetwork {
    public static List<Packets.Payload> joinSyncPayloads() {
        return List.of(new Packets.SpellRegistrySync(SpellAssignments.encoded), new Packets.ConfigSync(SpellEngineMod.config));
    }

    public static void handleCastRequest(Packets.CastRequest packet, MinecraftServer server, ServerPlayer player) {
        ServerLevel world = (ServerLevel)Iterables.tryFind((Iterable)server.m_129785_(), element -> element == player.m_9236_()).orNull();
        if (world == null || world.f_46443_) {
            return;
        }
        world.m_7654_().m_201446_(() -> ((SpellCaster.Player)player).getInteractor().requestCast(packet.spellId(), packet.snapshot()));
    }

    public static void handleTargetStream(Packets.TargetStream packet, MinecraftServer server, ServerPlayer player) {
        ServerLevel world = (ServerLevel)Iterables.tryFind((Iterable)server.m_129785_(), element -> element == player.m_9236_()).orNull();
        if (world == null || world.f_46443_) {
            return;
        }
        world.m_7654_().m_201446_(() -> ((SpellCaster.Player)player).getInteractor().submitTargets(packet.spellId(), packet.snapshot()));
    }

    public static void handleCastInput(Packets.CastInput packet, MinecraftServer server, ServerPlayer player) {
        ServerLevel world = (ServerLevel)Iterables.tryFind((Iterable)server.m_129785_(), element -> element == player.m_9236_()).orNull();
        if (world == null || world.f_46443_) {
            return;
        }
        world.m_7654_().m_201446_(() -> ((SpellCaster.Player)player).getInteractor().requestEnd(packet.spellId(), packet.snapshot()));
    }

    public static void handleAttackFxBroadcast(Packets.AttackFxBroadcast packet, MinecraftServer server, ServerPlayer player) {
        ServerLevel world = (ServerLevel)Iterables.tryFind((Iterable)server.m_129785_(), element -> element == player.m_9236_()).orNull();
        if (world == null || world.f_46443_) {
            return;
        }
        world.m_7654_().m_201446_(() -> Melee.broadcastAttackFx(player, packet.attackContext()));
    }

    public static void handleAttackPerform(Packets.AttackPerform packet, MinecraftServer server, ServerPlayer player) {
        ServerLevel world = (ServerLevel)Iterables.tryFind((Iterable)server.m_129785_(), element -> element == player.m_9236_()).orNull();
        if (world == null || world.f_46443_) {
            return;
        }
        world.m_7654_().m_201446_(() -> Melee.performAttackAgainstTargets(player, packet.attackContext(), packet.targetIds()));
    }

    public static void onPlayerConnectOrChangeWorld(ServerPlayer player) {
        ((SpellCaster.Player)player).getCooldownManager().pushSync();
        SpellContainerSource.syncServerSideContainers((Player)player);
    }
}

