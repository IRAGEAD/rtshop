/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.injectables.annotations.ExpectPlatform
 *  dev.architectury.injectables.annotations.ExpectPlatform$Transformed
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.CreativeModeTab$Builder
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.platform;

import com.pla.spiral_abyss.platform.forge.AbyssPlatformImpl;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class AbyssPlatform {
    @ExpectPlatform
    @ExpectPlatform.Transformed
    public static CompoundTag data(Entity entity) {
        return AbyssPlatformImpl.data(entity);
    }

    @ExpectPlatform
    @ExpectPlatform.Transformed
    public static boolean canShieldBlock(ItemStack stack) {
        return AbyssPlatformImpl.canShieldBlock(stack);
    }

    @ExpectPlatform
    @ExpectPlatform.Transformed
    public static CreativeModeTab.Builder creativeTab() {
        return AbyssPlatformImpl.creativeTab();
    }

    /*
     * WARNING - void declaration
     */
    @ExpectPlatform
    @ExpectPlatform.Transformed
    public static Entity transfer(ServerPlayer player, ServerLevel level, Vec3 position, float yaw, float pitch) {
        void var4_4;
        void var3_3;
        void var2_2;
        void var1_1;
        return AbyssPlatformImpl.transfer(player, (ServerLevel)var1_1, (Vec3)var2_2, (float)var3_3, (float)var4_4);
    }
}

