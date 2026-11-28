/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.music.music_areas.shapes.FDMusicAreaShape
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.server.players.PlayerList
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdlib.systems.music.music_areas.shapes.FDMusicAreaShape;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;

public class BossMusicAreaCylinder
extends FDMusicAreaShape {
    private float radius;
    private float height;

    public BossMusicAreaCylinder(float radius, float height) {
        this.radius = radius;
        this.height = height;
    }

    public List<ServerPlayer> getPlayersInside(ServerLevel level, Vec3 position) {
        PlayerList playerList = level.m_7654_().m_6846_();
        List<ServerPlayer> players = playerList.m_11314_().stream().filter(player -> BossTargetFinder.isPointInCylinder(player.m_20182_(), position, this.height, this.radius) && player.m_9236_().m_46472_().equals(level.m_46472_())).toList();
        return players;
    }
}

