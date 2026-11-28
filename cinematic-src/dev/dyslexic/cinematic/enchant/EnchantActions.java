/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.stats.Stats
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.EnchantmentMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 */
package dev.dyslexic.cinematic.enchant;

import dev.dyslexic.cinematic.Compat;
import dev.dyslexic.cinematic.net.EnchantDisplayPayload;
import dev.dyslexic.cinematic.net.EnchantFxPayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.station.Sessions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public final class EnchantActions {
    private static final Map<UUID, Integer> SHOWN = new HashMap<UUID, Integer>();
    private static final Map<UUID, Integer> ENCHANTED = new HashMap<UUID, Integer>();

    private EnchantActions() {
    }

    public static void close(ServerPlayer player, Sessions.Session session) {
        SHOWN.remove(player.m_20148_());
        ENCHANTED.remove(player.m_20148_());
        EnchantActions.show(player, session, ItemStack.f_41583_, 0);
    }

    public static void tick(ServerPlayer player, Sessions.Session session) {
        AbstractContainerMenu abstractContainerMenu = player.f_36096_;
        if (!(abstractContainerMenu instanceof EnchantmentMenu)) {
            return;
        }
        EnchantmentMenu menu = (EnchantmentMenu)abstractContainerMenu;
        ItemStack item = menu.m_38853_(0).m_7993_();
        int lapis = menu.m_38853_(1).m_7993_().m_41613_();
        int fingerprint = (item.m_41619_() ? 0 : Compat.hashItem(item)) * 31 + lapis;
        Integer last = SHOWN.put(player.m_20148_(), fingerprint);
        if (last == null || last != fingerprint) {
            EnchantActions.show(player, session, item, lapis);
        }
    }

    private static void show(ServerPlayer player, Sessions.Session session, ItemStack item, int lapis) {
        Network.sendToTrackingChunk(player.m_284548_(), new ChunkPos(session.pos()), new EnchantDisplayPayload(session.pos(), item.m_41777_(), lapis, session.side().m_122416_()));
    }

    public static void watch(ServerPlayer player, Sessions.Session session) {
        int count = player.m_8951_().m_13015_(Stats.f_12988_.m_12902_((Object)Stats.f_12964_));
        Integer last = ENCHANTED.put(player.m_20148_(), count);
        if (last != null && count > last) {
            EnchantActions.flare(player, session);
        }
    }

    private static void flare(ServerPlayer player, Sessions.Session session) {
        ServerLevel level = player.m_284548_();
        Network.sendToTrackingChunk(level, new ChunkPos(session.pos()), new EnchantFxPayload(session.pos(), player.m_19879_()));
        double x = (double)session.pos().m_123341_() + 0.5;
        double y = (double)session.pos().m_123342_() + 1.1;
        double z = (double)session.pos().m_123343_() + 0.5;
        level.m_8767_((ParticleOptions)ParticleTypes.f_123809_, x, y, z, 40, 0.3, 0.3, 0.3, 0.6);
    }
}

