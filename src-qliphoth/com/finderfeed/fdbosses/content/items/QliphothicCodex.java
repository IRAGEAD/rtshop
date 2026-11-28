/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 */
package com.finderfeed.fdbosses.content.items;

import com.finderfeed.fdbosses.BossClientPackets;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class QliphothicCodex
extends Item {
    public QliphothicCodex(Item.Properties p_41383_) {
        super(p_41383_);
    }

    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand p_41434_) {
        if (level.m_5776_()) {
            BossClientPackets.openQliphoticCodex();
        }
        return super.m_7203_(level, player, p_41434_);
    }
}

