/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$PlayerTickEvent
 *  net.minecraftforge.event.entity.living.LivingEvent$LivingJumpEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.geburah.sins;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.sins.GeburahTriggerSinEffectPacket;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.ActivePlayerSinInstance;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins;
import com.finderfeed.fdbosses.content.util.WorldBox;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.GeburahSins;
import com.finderfeed.fdlib.network.FDPacketHandler;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerSinsHandler {
    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Player player = event.player;
        Level level = player.m_9236_();
        if (!level.f_46443_) {
            PlayerSinsHandler.tickPlayerSins(player);
        }
    }

    public static void tickPlayerSins(Player player) {
        PlayerSins playerSins = PlayerSins.getPlayerSins(player);
        Iterator<ActivePlayerSinInstance> instanceIterator = playerSins.getActiveSins().iterator();
        int currentCooldown = playerSins.getSinGainCooldown();
        playerSins.setSinGainCooldown(Mth.m_14045_((int)(currentCooldown - 1), (int)0, (int)Integer.MAX_VALUE));
        while (instanceIterator.hasNext()) {
            ActivePlayerSinInstance activeSin = instanceIterator.next();
            WorldBox worldBox = activeSin.getSinActiveBox();
            if (worldBox.isEntityInBox((Entity)player)) {
                PlayerSin sin = activeSin.getSin();
                sin.playerTick(player, activeSin);
                activeSin.setActiveSinTime(activeSin.getActiveSinTime() + 1);
                continue;
            }
            instanceIterator.remove();
        }
        PlayerSins.setPlayerSins(player, playerSins, false);
    }

    public static void setActiveSins(ServerPlayer player, List<ActivePlayerSinInstance> instances) {
        PlayerSins playerSins = PlayerSins.getPlayerSins((Player)player);
        playerSins.setActiveSins(instances);
        PlayerSins.setPlayerSins((Player)player, playerSins);
    }

    public static void sin(ServerPlayer player, int cooldown) {
        PlayerSinsHandler.sin(player, cooldown, 1.0f, 1);
    }

    public static void sin(ServerPlayer player, int cooldown, float soundPitch, int amount) {
        PlayerSins playerSins = PlayerSins.getPlayerSins((Player)player);
        if (!playerSins.isGainingSinsOnCooldown() && BossUtil.isPlayerInSurvival((Player)player) && player.m_6084_()) {
            FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), (Object)new GeburahTriggerSinEffectPacket(soundPitch));
            int sinnedTimes = playerSins.getSinnedTimes();
            if (sinnedTimes + amount >= BossConfigs.BOSS_CONFIG.get().geburahConfig.maxPlayerSins) {
                player.m_6469_(BossDamageSources.GEBURAH_SINNED_TOO_MUCH_SOURCE, 9000000.0f);
            } else {
                playerSins.setSinnedTimes(sinnedTimes + amount);
                playerSins.setSinGainCooldown(cooldown);
                PlayerSins.setPlayerSins((Player)player, playerSins);
            }
        }
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        ServerPlayer player;
        PlayerSins playerSins;
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer && (playerSins = PlayerSins.getPlayerSins((Player)(player = (ServerPlayer)entity))).hasSinActive(GeburahSins.JUMPING_SIN.get())) {
            PlayerSinsHandler.sin(player, 40);
        }
    }
}

