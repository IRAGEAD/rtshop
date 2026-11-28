/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.debug;

import com.finderfeed.fdbosses.content.entities.geburah.distortion_sphere.DistortionSphereEffect;
import com.finderfeed.fdbosses.content.entities.geburah.distortion_sphere.DistortionSphereEffectHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DebugStick
extends Item {
    private Vec3 p1;
    private Vec3 p2;
    private Vec3 anchor = Vec3.f_82478_;

    public DebugStick(Item.Properties p_41383_) {
        super(p_41383_);
    }

    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        if (level.f_46443_) {
            DistortionSphereEffectHandler.setDistortionSphereEffect(new DistortionSphereEffect(player.m_146892_().m_82549_(player.m_20154_().m_82490_(50.0)), 60, 64.0f, 2.0f, 0.0f));
        }
        return InteractionResultHolder.m_19096_((Object)player.m_21120_(hand));
    }
}

