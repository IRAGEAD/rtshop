/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.util;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdlib.data_structures.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CylinderPlayerPositionsCollector {
    private HashMap<Player, PlayerPositionData> positionData = new HashMap();
    private Level level;
    private float radius;
    private float height;
    private Predicate<Player> predicate;

    public CylinderPlayerPositionsCollector(Level level, float radius, float height, Predicate<Player> predicate) {
        this.level = level;
        this.radius = radius;
        this.height = height;
        this.predicate = predicate;
    }

    public void tick(Vec3 pos) {
        List<Player> players = BossTargetFinder.getEntitiesInCylinder(Player.class, this.level, pos, this.height, this.radius, this.predicate);
        for (Player player : players) {
            if (this.positionData.containsKey(player)) {
                PlayerPositionData data = this.positionData.get(player);
                data.oldPos = data.currentPos;
                data.currentPos = player.m_20182_();
                continue;
            }
            this.positionData.put(player, new PlayerPositionData(player));
        }
        this.removeUnusedPlayers(players);
    }

    public Collection<Player> getPlayers() {
        return this.positionData.keySet();
    }

    public Pair<Vec3, Vec3> getOldAndCurrentPlayerPosition(Player player) {
        PlayerPositionData data = this.positionData.get(player);
        if (data == null) {
            return new Pair((Object)player.m_20182_(), (Object)player.m_20182_());
        }
        return new Pair((Object)data.oldPos, (Object)data.currentPos);
    }

    public List<Vec3> getCurrentPlayerPositions() {
        return new ArrayList<Vec3>(this.survivalPlayersData().stream().map(positionData -> positionData.currentPos).toList());
    }

    public List<Vec3> getOldPlayerPositions() {
        return new ArrayList<Vec3>(this.survivalPlayersData().stream().map(positionData -> positionData.oldPos).toList());
    }

    private List<PlayerPositionData> survivalPlayersData() {
        return this.positionData.entrySet().stream().filter(e -> BossUtil.isPlayerInSurvival((Player)e.getKey())).map(Map.Entry::getValue).toList();
    }

    private void removeUnusedPlayers(List<Player> currentPlayersInsideCylinder) {
        this.positionData.entrySet().removeIf(data -> !currentPlayersInsideCylinder.contains(data.getKey()) || ((Player)data.getKey()).m_21224_());
    }

    private static class PlayerPositionData {
        private Vec3 currentPos;
        private Vec3 oldPos;

        public PlayerPositionData(Player player) {
            this.currentPos = player.m_20182_();
            this.oldPos = player.m_20182_();
        }

        public void setCurrentPos(Vec3 currentPos) {
            this.currentPos = currentPos;
        }

        public void setOldPos(Vec3 oldPos) {
            this.oldPos = oldPos;
        }

        public Vec3 getCurrentPos() {
            return this.currentPos;
        }

        public Vec3 getOldPos() {
            return this.oldPos;
        }
    }
}

