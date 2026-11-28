/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 */
package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.entity.LootChestEntity;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ExperienceChestEntity
extends LootChestEntity {
    public ExperienceChestEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level, 0);
    }

    @Override
    protected InteractionResult m_6071_(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            AbyssParties.get(serverPlayer.f_8924_).openExperienceChest(serverPlayer, this);
        }
        return InteractionResult.m_19078_((boolean)this.m_9236_().f_46443_);
    }
}

