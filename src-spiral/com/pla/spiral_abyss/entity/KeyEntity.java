/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.ai.attributes.AttributeSupplier$Builder
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 */
package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.entity.ArenaInteractable;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class KeyEntity
extends ArenaInteractable {
    public KeyEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.m_21530_();
        this.m_20242_(true);
    }

    @Override
    public void m_8119_() {
        super.m_8119_();
        Player player = this.m_9236_().m_45930_((Entity)this, 10.0);
        if (player != null) {
            this.m_7618_(EntityAnchorArgument.Anchor.EYES, player.m_20182_());
        }
    }

    protected InteractionResult m_6071_(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer) {
            ServerLevel level;
            ServerPlayer serverPlayer = (ServerPlayer)player;
            Level level2 = this.m_9236_();
            if (level2 instanceof ServerLevel && (level = (ServerLevel)level2).m_46472_().equals(AbyssDimension.LEVEL)) {
                AbyssParties.get(serverPlayer.f_8924_).startKey(serverPlayer, this);
            }
        }
        return InteractionResult.m_19078_((boolean)this.m_9236_().f_46443_);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.0);
    }
}

