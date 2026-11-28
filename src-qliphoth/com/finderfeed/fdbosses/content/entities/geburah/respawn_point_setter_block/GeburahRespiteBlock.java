/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.EntityBlock
 *  net.minecraft.world.level.block.GlazedTerracottaBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block;

import com.finderfeed.fdbosses.init.BossTileEntities;
import com.finderfeed.fdlib.data_structures.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class GeburahRespiteBlock
extends GlazedTerracottaBlock
implements EntityBlock {
    public static final String DATA_NAME = "geburahRespawnData";

    public GeburahRespiteBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            if (hand == InteractionHand.MAIN_HAND) {
                GeburahRespiteBlock.putSpecialRespawnPoint(serverPlayer, (ResourceKey<Level>)serverPlayer.m_9236_().m_46472_(), serverPlayer.m_20097_().m_7494_());
                serverPlayer.m_213846_((Component)Component.m_237115_((String)"fdbosses.word.set_special_respawn_point"));
                serverPlayer.m_6674_(InteractionHand.MAIN_HAND);
                return InteractionResult.SUCCESS;
            }
        }
        return super.m_6227_(state, level, pos, player, hand, p_60508_);
    }

    public static CompoundTag getRespawnData(ServerPlayer serverPlayer) {
        CompoundTag perdata = serverPlayer.getPersistentData();
        if (perdata.m_128441_(DATA_NAME)) {
            return perdata.m_128469_(DATA_NAME);
        }
        return null;
    }

    public static void putRespawnData(ServerPlayer serverPlayer, CompoundTag tag) {
        CompoundTag perdata = serverPlayer.getPersistentData();
        perdata.m_128365_(DATA_NAME, (Tag)tag);
    }

    public static void putSpecialRespawnPoint(ServerPlayer serverPlayer, ResourceKey<Level> dimension, BlockPos pos) {
        CompoundTag tag;
        CompoundTag perdata = serverPlayer.getPersistentData();
        if (!perdata.m_128441_(DATA_NAME)) {
            tag = new CompoundTag();
            perdata.m_128365_(DATA_NAME, (Tag)tag);
        }
        tag = perdata.m_128469_(DATA_NAME);
        tag.m_128405_("x", pos.m_123341_());
        tag.m_128405_("y", pos.m_123342_());
        tag.m_128405_("z", pos.m_123343_());
        tag.m_128359_("dimension", dimension.m_135782_().toString());
    }

    public static Pair<ResourceKey<Level>, BlockPos> getSpecialRespawnPoint(ServerPlayer serverPlayer) {
        CompoundTag perdata = serverPlayer.getPersistentData();
        if (!perdata.m_128441_(DATA_NAME)) {
            return null;
        }
        CompoundTag data = perdata.m_128469_(DATA_NAME);
        int x = data.m_128451_("x");
        int y = data.m_128451_("y");
        int z = data.m_128451_("z");
        ResourceKey dimension = ResourceKey.m_135785_((ResourceKey)Registries.f_256858_, (ResourceLocation)new ResourceLocation(data.m_128461_("dimension")));
        return new Pair((Object)dimension, (Object)new BlockPos(x, y, z));
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return BossTileEntities.GEBURAH_RESPITE_BLOCK.get().m_155264_(pos, state);
    }
}

