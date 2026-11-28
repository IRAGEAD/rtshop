/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraftforge.event.entity.player.PlayerEvent$Clone
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.network.NetworkDirection
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.packets.SetClientMalkuthWeaknessAmountPacket;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE, modid="fdbosses")
public class MalkuthWeaknessHandler {
    public static String WEAKNESS_LEVEL = "weakness_level";
    public static final int MAX = 100;
    public static final int MIN = -100;

    @SubscribeEvent
    public static void cloneEvent(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();
        if (!original.m_9236_().f_46443_) {
            MalkuthWeaknessHandler.setCurrentWeakness(newPlayer, MalkuthWeaknessHandler.getCurrentWeaknessLevel(original));
        }
    }

    public static CompoundTag getWeaknessTag(Player player) {
        return BossUtil.getOrCreateTag(BossUtil.getPlayerTag(player), "malkuth_weakness");
    }

    public static int getCurrentWeaknessLevel(Player player) {
        CompoundTag tag = MalkuthWeaknessHandler.getWeaknessTag(player);
        return FDMathUtil.clamp((int)tag.m_128451_(WEAKNESS_LEVEL), (int)-100, (int)100);
    }

    public static void setCurrentWeakness(Player player, int amount) {
        amount = FDMathUtil.clamp((int)amount, (int)-100, (int)100);
        CompoundTag tag = MalkuthWeaknessHandler.getWeaknessTag(player);
        tag.m_128405_(WEAKNESS_LEVEL, amount);
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            FDPacketHandler.INSTANCE.sendTo((Object)new SetClientMalkuthWeaknessAmountPacket(amount), serverPlayer.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    public static void damageWeakness(MalkuthAttackType damageType, Player player, int amount) {
        int current = MalkuthWeaknessHandler.getCurrentWeaknessLevel(player);
        if (current >= 0) {
            if (damageType.isIce()) {
                if (current - amount < 0) {
                    MalkuthWeaknessHandler.setCurrentWeakness(player, current - amount * 2);
                } else {
                    MalkuthWeaknessHandler.setCurrentWeakness(player, current - amount);
                }
            } else {
                MalkuthWeaknessHandler.setCurrentWeakness(player, current + amount);
            }
        } else if (damageType.isFire()) {
            if (current + amount >= 0) {
                MalkuthWeaknessHandler.setCurrentWeakness(player, current + amount * 2);
            } else {
                MalkuthWeaknessHandler.setCurrentWeakness(player, current + amount);
            }
        } else {
            MalkuthWeaknessHandler.setCurrentWeakness(player, current - amount);
        }
    }

    public static MalkuthAttackType getWeakTo(Player player) {
        return MalkuthWeaknessHandler.getCurrentWeaknessLevel(player) >= 0 ? MalkuthAttackType.FIRE : MalkuthAttackType.ICE;
    }

    public static boolean isWeakTo(Player player, MalkuthAttackType malkuthAttackType) {
        return MalkuthWeaknessHandler.getWeakTo(player).equals((Object)malkuthAttackType);
    }
}

