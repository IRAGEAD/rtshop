/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.HoverEvent
 *  net.minecraft.network.chat.HoverEvent$Action
 *  net.minecraft.network.chat.HoverEvent$ItemStackInfo
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.entity.ChestLootEntity;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RareRewardBroadcast {
    private static final String FLOOR = "spiral_abyss_reward_floor";
    private static final String ANNOUNCED = "spiral_abyss_reward_announced";

    private RareRewardBroadcast() {
    }

    public static void markDrop(ChestLootEntity item, int floor) {
        if (floor >= 1 && floor <= 6) {
            AbyssPlatform.data((Entity)item).m_128405_(FLOOR, floor);
        }
    }

    public static void pickedUp(AbyssEvents.PlayerEvent.ItemPickupEvent event) {
        ChestLootEntity loot;
        ServerPlayer player;
        block5: {
            block4: {
                Player player2 = event.getEntity();
                if (!(player2 instanceof ServerPlayer)) break block4;
                player = (ServerPlayer)player2;
                player2 = event.getOriginalEntity();
                if (player2 instanceof ChestLootEntity && (loot = (ChestLootEntity)player2).rare() && !event.getStack().m_41619_()) break block5;
            }
            return;
        }
        CompoundTag metadata = AbyssPlatform.data((Entity)loot);
        int floor = metadata.m_128451_(FLOOR);
        if (floor < 1 || floor > 6 || metadata.m_128471_(ANNOUNCED)) {
            return;
        }
        metadata.m_128379_(ANNOUNCED, true);
        RareRewardBroadcast.acquired(player, event.getStack(), floor);
    }

    public static void acquired(ServerPlayer player, ItemStack reward, int floor) {
        if (!((Boolean)AbyssConfig.BROADCAST_RARE_LOOT.get()).booleanValue() || reward.m_41619_()) {
            return;
        }
        ItemStack stack = reward.m_41777_();
        stack.m_41764_(1);
        MutableComponent name = player.m_7755_().m_6881_().m_130940_(ChatFormatting.AQUA);
        MutableComponent itemName = Component.m_237119_().m_7220_((Component)stack.m_41786_().m_6881_().m_130940_(ChatFormatting.GOLD)).m_130938_(style -> style.m_131140_(ChatFormatting.GOLD).m_131144_(new HoverEvent(HoverEvent.Action.f_130832_, (Object)new HoverEvent.ItemStackInfo(stack))));
        player.f_8924_.m_6846_().m_240416_((Component)Component.m_237110_((String)"message.spiral_abyss.rare_reward", (Object[])new Object[]{name, itemName, floor}), false);
        AbyssNetwork.broadcastRareReward((Component)name, stack);
    }
}

