/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.llamalad7.mixinextras.sugar.ref.LocalRef
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.server.players.PlayerList
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.finderfeed.fdbosses.mixin;

import com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block.GeburahRespiteBlock;
import com.finderfeed.fdlib.data_structures.Pair;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PlayerList.class})
public class PlayerListMixin {
    @Shadow
    @Final
    private MinecraftServer f_11195_;

    @Inject(method={"respawn"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;getLevelData()Lnet/minecraft/world/level/storage/LevelData;")})
    public void respawn(ServerPlayer serverPlayer, boolean p_11238_, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal=1) LocalRef<ServerPlayer> serverPlayerLocalRef, @Local(ordinal=1) LocalRef<ServerLevel> serverLevelLocalRef) {
        CompoundTag respawnData;
        Pair<ResourceKey<Level>, BlockPos> respawnPos = GeburahRespiteBlock.getSpecialRespawnPoint(serverPlayer);
        if (respawnPos != null && (respawnData = GeburahRespiteBlock.getRespawnData(serverPlayer)).m_128471_("diedToGeburah")) {
            ServerLevel level = this.f_11195_.m_129880_((ResourceKey)respawnPos.first);
            Vec3 pos = ((BlockPos)respawnPos.second).m_252807_();
            if (level != null) {
                ServerPlayer player = (ServerPlayer)serverPlayerLocalRef.get();
                serverLevelLocalRef.set((Object)level);
                player.m_284127_(level);
                player.m_6034_(pos.f_82479_, pos.f_82480_, pos.f_82481_);
            }
            respawnData.m_128473_("diedToGeburah");
        }
    }
}

