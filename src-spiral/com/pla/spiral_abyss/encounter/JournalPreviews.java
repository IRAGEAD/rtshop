/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.server.level.ServerPlayer
 */
package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.RewardPreviews;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.world.AbyssParties;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

public final class JournalPreviews {
    private JournalPreviews() {
    }

    public static List<AbyssNetwork.BannerBoss> bosses(ServerPlayer player) {
        AbyssParties parties = AbyssParties.get(player.f_8924_);
        RewardPreviews rewards = new RewardPreviews(player.f_8924_);
        FloorDefinition.Rewards noBaseRewards = new FloorDefinition.Rewards(List.of(), 0, 0);
        ArrayList<AbyssNetwork.BannerBoss> result = new ArrayList<AbyssNetwork.BannerBoss>();
        for (int floor = 5; floor <= 6; ++floor) {
            int wave;
            FloorDefinition definition = FloorCatalog.snapshot().floors().get(floor);
            if (definition == null) continue;
            int n = wave = floor == 5 ? 3 : 2;
            while (wave <= 3) {
                if (definition.waves().size() >= wave) {
                    try {
                        for (FloorDefinition.Enemy enemy : parties.cycleLineup(floor, wave, definition.waves().get(wave - 1))) {
                            if (enemy.category() != MobCategory.BOSS && enemy.category() != null) continue;
                            List<AbyssNetwork.RewardPreview> linked = rewards.floor(noBaseRewards, Set.of(enemy.id())).stream().filter(reward -> reward.rare() && !reward.table()).toList();
                            for (int count = 0; count < enemy.count() && result.size() < 4; ++count) {
                                result.add(new AbyssNetwork.BannerBoss(floor, wave, enemy.id(), linked, MobCatalog.bossOptions(enemy.id()).previewAnimation()));
                            }
                        }
                    }
                    catch (IllegalArgumentException ex) {
                        LogUtils.getLogger().warn("Cannot preview Abyss journal floor {} wave {}: {}", new Object[]{floor, wave, ex.getMessage()});
                    }
                }
                ++wave;
            }
        }
        return List.copyOf(result);
    }
}

