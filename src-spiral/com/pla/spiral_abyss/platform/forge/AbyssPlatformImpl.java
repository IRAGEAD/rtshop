/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.CreativeModeTab$Builder
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.common.ToolActions
 *  net.minecraftforge.common.util.ITeleporter
 */
package com.pla.spiral_abyss.platform.forge;

import com.pla.spiral_abyss.forge.world.AbyssTeleporter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.ITeleporter;

public final class AbyssPlatformImpl {
    public static CompoundTag data(Entity entity) {
        return entity.getPersistentData();
    }

    public static boolean canShieldBlock(ItemStack stack) {
        return stack.canPerformAction(ToolActions.SHIELD_BLOCK);
    }

    public static CreativeModeTab.Builder creativeTab() {
        return CreativeModeTab.builder();
    }

    public static Entity transfer(ServerPlayer player, ServerLevel level, Vec3 position, float yaw, float pitch) {
        return player.changeDimension(level, (ITeleporter)new AbyssTeleporter(position, yaw, pitch));
    }
}

