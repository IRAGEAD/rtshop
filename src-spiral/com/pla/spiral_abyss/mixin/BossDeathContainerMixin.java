/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.BossDeathBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={RandomizableContainerBlockEntity.class})
public abstract class BossDeathContainerMixin {
    @Inject(method={"setItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiral_abyss$suppressDeathItems(int slot, ItemStack stack, CallbackInfo callback) {
        RandomizableContainerBlockEntity container = (RandomizableContainerBlockEntity)this;
        if (BossDeathBlocks.suppressLoot(container.m_58904_(), container.m_58899_())) {
            callback.cancel();
        }
    }

    @Inject(method={"setLootTable(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceLocation;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private static void spiral_abyss$suppressDeathLoot(BlockGetter reader, RandomSource random, BlockPos pos, ResourceLocation table, CallbackInfo callback) {
        Level level;
        if (reader instanceof Level && BossDeathBlocks.suppressLoot(level = (Level)reader, pos)) {
            callback.cancel();
        }
    }

    @Inject(method={"setLootTable(Lnet/minecraft/resources/ResourceLocation;J)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiral_abyss$suppressAssignedDeathLoot(ResourceLocation table, long seed, CallbackInfo callback) {
        RandomizableContainerBlockEntity container = (RandomizableContainerBlockEntity)this;
        if (BossDeathBlocks.suppressLoot(container.m_58904_(), container.m_58899_())) {
            callback.cancel();
        }
    }
}

