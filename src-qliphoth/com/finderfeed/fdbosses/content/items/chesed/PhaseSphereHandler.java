/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDServerItemAnimations
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Abilities
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.event.entity.player.PlayerEvent$Clone
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.items.chesed;

import com.finderfeed.fdbosses.content.items.chesed.PhaseSpherePacket;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDServerItemAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class PhaseSphereHandler {
    public static final String CHESED_ITEM_DATANAME = "phase_sphere_data";
    public static final String CHESED_ITEM_USE_TIME = "phase_sphere_time_data";
    public static boolean isUsingChesedItem = false;
    public static int clientsideChesedItemUseTick = -1;

    public static void onChesedItemUse(Player player) {
        if (!player.m_20159_()) {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                ItemStack item = player.m_21211_();
                if (item.m_150930_(BossItems.PHASE_SPHERE.get()) && PhaseSphereHandler.canContinueUsingChesedItem(player)) {
                    PhaseSphereHandler.chesedItemServerUseTick(player);
                } else {
                    PhaseSphereHandler.stopUsingChesedItem(player, CHESED_ITEM_DATANAME);
                }
            } else if (isUsingChesedItem) {
                Abilities abilities = player.m_150110_();
                abilities.m_35943_(0.15f);
                abilities.f_35936_ = true;
                abilities.f_35937_ = false;
                abilities.f_35935_ = true;
                player.f_19794_ = true;
                player.m_6853_(false);
                clientsideChesedItemUseTick = Mth.m_14045_((int)(clientsideChesedItemUseTick + 1), (int)0, (int)BossConfigs.BOSS_CONFIG.get().itemConfig.phaseSphereUseDuration);
            } else {
                clientsideChesedItemUseTick = -1;
            }
        }
    }

    private static void chesedItemServerUseTick(Player player) {
        CompoundTag perdata = player.getPersistentData();
        boolean sendPacket = false;
        if (!perdata.m_128441_(CHESED_ITEM_DATANAME)) {
            CompoundTag tag = new CompoundTag();
            player.m_150110_().m_35945_(tag);
            tag.m_128405_(CHESED_ITEM_USE_TIME, 0);
            perdata.m_128365_(CHESED_ITEM_DATANAME, (Tag)tag);
            sendPacket = true;
        }
        Abilities abilities = player.m_150110_();
        abilities.m_35943_(0.15f);
        abilities.f_35936_ = true;
        abilities.f_35937_ = false;
        abilities.f_35935_ = true;
        player.f_19794_ = true;
        player.m_6853_(false);
        if (sendPacket) {
            FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)player), (Object)new PhaseSpherePacket(player, true));
        }
        CompoundTag compound = perdata.m_128469_(CHESED_ITEM_DATANAME);
        int time = compound.m_128451_(CHESED_ITEM_USE_TIME);
        compound.m_128405_(CHESED_ITEM_USE_TIME, time + 1);
    }

    private static int getServerChesedItemUsedTime(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.m_128441_(CHESED_ITEM_DATANAME)) {
            CompoundTag tag = data.m_128469_(CHESED_ITEM_DATANAME);
            return tag.m_128451_(CHESED_ITEM_USE_TIME);
        }
        return -1;
    }

    private static void stopUsingChesedItem(Player player, String dataname) {
        CompoundTag perdata = player.getPersistentData();
        if (perdata.m_128441_(CHESED_ITEM_DATANAME)) {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                CompoundTag tag = perdata.m_128469_(dataname);
                player.m_150110_().m_35950_(tag);
                player.f_19794_ = false;
                FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), (Object)new PhaseSpherePacket(player, false));
                FDServerItemAnimations.startItemAnimation((LivingEntity)player, (String)"STOP_USE", (AnimationTicker)AnimationTicker.builder(BossAnims.CHESED_ITEM_USE).setToNullTransitionTime(0).reversed().build(), (InteractionHand)player.m_7655_());
                player.m_5810_();
                player.m_36335_().m_41524_(BossItems.PHASE_SPHERE.get(), BossConfigs.BOSS_CONFIG.get().itemConfig.phaseSphereCooldown);
            }
            perdata.m_128473_(dataname);
        }
    }

    public static boolean canContinueUsingChesedItem(Player player) {
        int useTime = PhaseSphereHandler.getServerChesedItemUsedTime(player);
        if (useTime > BossConfigs.BOSS_CONFIG.get().itemConfig.phaseSphereUseDuration) {
            return false;
        }
        BlockPos pos = player.m_20097_().m_7494_();
        Level level = player.m_9236_();
        int nonAirBlocks = 0;
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                for (int y = -1; y <= 1; ++y) {
                    BlockPos p = pos.m_7918_(x, y, z);
                    BlockState blockState = level.m_8055_(p);
                    if (blockState.m_60795_() || blockState.m_60812_((BlockGetter)level, p).m_83281_()) continue;
                    ++nonAirBlocks;
                }
            }
        }
        return useTime > 20 ? nonAirBlocks >= 10 : nonAirBlocks > 0;
    }

    @SubscribeEvent
    public static void deathEvent(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player copy = event.getEntity();
        CompoundTag data = original.getPersistentData();
        if (data.m_128441_(CHESED_ITEM_DATANAME)) {
            copy.getPersistentData().m_128365_(CHESED_ITEM_DATANAME, (Tag)data.m_128469_(CHESED_ITEM_DATANAME));
        }
    }
}

