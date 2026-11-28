/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList$Builder
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.finderfeed.fdbosses.mixin;

import com.finderfeed.fdbosses.client.BossCommonMixinHandle;
import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Entity.class}, priority=1001)
public class EntityMixin {
    @Inject(method={"collideBoundingBox"}, at={@At(value="INVOKE", target="Lcom/google/common/collect/ImmutableList$Builder;addAll(Ljava/lang/Iterable;)Lcom/google/common/collect/ImmutableList$Builder;", ordinal=1)})
    private static void collectColliders(Entity entity, Vec3 smth, AABB box, Level level, List<VoxelShape> shapes, CallbackInfoReturnable<Vec3> cir, @Local ImmutableList.Builder<VoxelShape> builder) {
        BossCommonMixinHandle.entityCollidersMixin(entity, level, shapes, box.m_82369_(smth), cir, builder);
    }
}

