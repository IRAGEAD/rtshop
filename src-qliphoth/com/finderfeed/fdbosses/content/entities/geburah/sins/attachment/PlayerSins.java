/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtOps
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.player.Player
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.geburah.sins.attachment;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.ActivePlayerSinInstance;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.SyncPlayerSinsPacket;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public class PlayerSins {
    public static final Codec<PlayerSins> CODEC = RecordCodecBuilder.create(p -> p.group((App)Codec.list(ActivePlayerSinInstance.CODEC).fieldOf("active_sins").forGetter(v -> v.activeSins), (App)Codec.INT.fieldOf("sinned_times").forGetter(v -> v.sinnedTimes), (App)Codec.INT.fieldOf("cooldown").forGetter(v -> v.sinGainCooldown)).apply((Applicative)p, PlayerSins::new));
    public static final NetworkCodec<PlayerSins> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.listOf(ActivePlayerSinInstance.STREAM_CODEC), v -> v.activeSins, (NetworkCodec)NetworkCodec.INT, v -> v.sinnedTimes, (NetworkCodec)NetworkCodec.INT, v -> v.sinGainCooldown, PlayerSins::new);
    private List<ActivePlayerSinInstance> activeSins = new ArrayList<ActivePlayerSinInstance>();
    private int sinnedTimes = 0;
    private int sinGainCooldown = 0;

    public PlayerSins() {
    }

    protected PlayerSins(List<ActivePlayerSinInstance> activeSins, int sinnedTimes, int cooldown) {
        this(activeSins);
        this.sinnedTimes = sinnedTimes;
        this.sinGainCooldown = cooldown;
    }

    public PlayerSins(PlayerSins other) {
        this.activeSins = new ArrayList<ActivePlayerSinInstance>(other.activeSins);
        this.sinnedTimes = other.sinnedTimes;
        this.sinGainCooldown = other.sinGainCooldown;
    }

    public PlayerSins(List<ActivePlayerSinInstance> activeSins) {
        this.activeSins = new ArrayList<ActivePlayerSinInstance>(activeSins);
    }

    public static PlayerSins getPlayerSins(Player player) {
        CompoundTag playerTag = PlayerSins.getPlayerSinsTag(player);
        if (!playerTag.m_128441_("playerSins")) {
            PlayerSins.setPlayerSins(player, new PlayerSins(), true);
        }
        Tag tag = playerTag.m_128423_("playerSins");
        PlayerSins sins = (PlayerSins)((Pair)CODEC.decode((DynamicOps)NbtOps.f_128958_, (Object)tag).get().left().get()).getFirst();
        return sins;
    }

    public static void setPlayerSins(Player player, PlayerSins playerSins, boolean sendUpdate) {
        DataResult t = CODEC.encodeStart((DynamicOps)NbtOps.f_128958_, (Object)playerSins);
        Tag tag = (Tag)t.get().left().get();
        CompoundTag playerTag = PlayerSins.getPlayerSinsTag(player);
        playerTag.m_128365_("playerSins", tag);
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            if (sendUpdate) {
                FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), (Object)new SyncPlayerSinsPacket(playerSins));
            }
        }
    }

    public static void setPlayerSins(Player player, PlayerSins playerSins) {
        PlayerSins.setPlayerSins(player, playerSins, true);
    }

    public static CompoundTag getPlayerSinsTag(Player player) {
        CompoundTag playerSins = BossUtil.getOrCreateTag(BossUtil.getPlayerTag(player), "player_sins");
        return playerSins;
    }

    public boolean hasSinActive(PlayerSin playerSin) {
        return this.activeSins.stream().anyMatch(inst -> inst.getSin() == playerSin);
    }

    public boolean hasExactlyThisSins(Collection<PlayerSin> playerSins) {
        int count = 0;
        for (ActivePlayerSinInstance sin : this.getActiveSins()) {
            PlayerSin s = sin.getSin();
            if (!playerSins.contains(s)) continue;
            ++count;
        }
        return count == playerSins.size() && playerSins.size() == this.getActiveSins().size();
    }

    public void setActiveSins(List<ActivePlayerSinInstance> instances) {
        this.activeSins = new ArrayList<ActivePlayerSinInstance>(instances);
    }

    public ActivePlayerSinInstance getSin(PlayerSin playerSin) {
        ActivePlayerSinInstance instance = null;
        for (ActivePlayerSinInstance sin : this.activeSins) {
            if (sin.getSin() != playerSin) continue;
            instance = sin;
            break;
        }
        return instance;
    }

    public int getSinnedTimes() {
        return this.sinnedTimes;
    }

    public void setSinnedTimes(int sinnedTimes) {
        this.sinnedTimes = sinnedTimes;
    }

    public List<ActivePlayerSinInstance> getActiveSins() {
        return this.activeSins;
    }

    public int getSinGainCooldown() {
        return this.sinGainCooldown;
    }

    public void setSinGainCooldown(int sinGainCooldown) {
        this.sinGainCooldown = Mth.m_14045_((int)sinGainCooldown, (int)0, (int)Integer.MAX_VALUE);
    }

    public boolean isGainingSinsOnCooldown() {
        return this.sinGainCooldown > 0;
    }
}

