/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList$Builder
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.ThrownEnderpearl
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.finderfeed.fdbosses.client;

import com.finderfeed.fdbosses.content.entities.chesed_boss.kinetic_field.ChesedKineticFieldEntity;
import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class BossCommonMixinHandle {
    public static void entityCollidersMixin(Entity entity, Level level, List<VoxelShape> shapes, AABB box, CallbackInfoReturnable<Vec3> cir, ImmutableList.Builder<VoxelShape> builder) {
        block5: {
            block4: {
                block3: {
                    if (!(entity instanceof Player)) break block3;
                    Player player = (Player)entity;
                    break block4;
                }
                if (!(entity instanceof ThrownEnderpearl)) break block5;
            }
            AABB searchBox = new AABB(-20.0, -20.0, -20.0, 20.0, 20.0, 20.0).m_82383_(entity.m_20182_());
            List list = level.m_45976_(ChesedKineticFieldEntity.class, searchBox);
            for (ChesedKineticFieldEntity e : list) {
                VoxelShape shape = e.getCollisionShape(entity);
                builder.add((Object)shape);
            }
        }
    }
}

