/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerBossEvent
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.BossEvent$BossBarColor
 *  net.minecraft.world.BossEvent$BossBarOverlay
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.platform.AbyssPlatform;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class BossHealthBars {
    public static final String HIDE_BOSS_BAR_TAG = "spiral_abyss_hide_boss_bar";
    private static final Set<ServerBossEvent> REGISTERED = Collections.newSetFromMap(new WeakHashMap());
    private static final Set<ServerBossEvent> FALLBACKS = Collections.newSetFromMap(new WeakHashMap());
    private final Map<UUID, ServerBossEvent> bars = new HashMap<UUID, ServerBossEvent>();
    private final Map<UUID, Integer> ages = new HashMap<UUID, Integer>();

    public static void registered(ServerBossEvent event) {
        REGISTERED.add(event);
    }

    public void tick(List<LivingEntity> enemies, List<ServerPlayer> players) {
        HashSet<UUID> active = new HashSet<UUID>();
        for (LivingEntity mob : enemies) {
            if (!AbyssPlatform.data((Entity)mob).m_128471_("spiral_abyss_wave_boss") || !mob.m_6084_() || AbyssPlatform.data((Entity)mob).m_128471_(HIDE_BOSS_BAR_TAG)) continue;
            UUID id2 = mob.m_20148_();
            active.add(id2);
            if (this.ages.merge(id2, 1, Integer::sum) < 5) continue;
            ServerBossEvent bar = this.bars.get(id2);
            for (ServerPlayer player : players) {
                if (BossHealthBars.hasOtherBar(player)) {
                    if (bar == null) continue;
                    bar.m_6539_(player);
                    continue;
                }
                if (bar == null) {
                    bar = new ServerBossEvent(mob.m_5446_(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
                    FALLBACKS.add(bar);
                    this.bars.put(id2, bar);
                }
                bar.m_6543_(player);
            }
            if (bar == null) continue;
            bar.m_6456_(mob.m_5446_());
            bar.m_142711_(Math.max(0.0f, Math.min(1.0f, mob.m_21223_() / mob.m_21233_())));
            for (ServerPlayer player : List.copyOf(bar.m_8324_())) {
                if (players.contains(player)) continue;
                bar.m_6539_(player);
            }
        }
        this.ages.keySet().removeIf(id -> !active.contains(id));
        this.bars.entrySet().removeIf(entry -> {
            if (active.contains(entry.getKey())) {
                return false;
            }
            ((ServerBossEvent)entry.getValue()).m_7706_();
            return true;
        });
    }

    public void clear() {
        this.bars.values().forEach(ServerBossEvent::m_7706_);
        this.bars.clear();
        this.ages.clear();
    }

    private static boolean hasOtherBar(ServerPlayer player) {
        for (ServerBossEvent event : REGISTERED) {
            if (FALLBACKS.contains(event) || !event.m_8324_().contains(player)) continue;
            return true;
        }
        return false;
    }
}

