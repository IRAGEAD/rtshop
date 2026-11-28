/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.Level
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.content.entities.geburah.sins.PlayerSinsHandler;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ExplosiveCrystalItem
extends Item {
    public ExplosiveCrystalItem(Item.Properties props) {
        super(props.m_41487_(1).m_41503_(450));
    }

    public void m_6883_(ItemStack item, Level level, Entity entity, int p_41407_, boolean p_41408_) {
        super.m_6883_(item, level, entity, p_41407_, p_41408_);
        if (!level.f_46443_) {
            item.m_41721_(item.m_41773_() + 1);
            if (item.m_41773_() >= item.m_41776_()) {
                if (entity instanceof ServerPlayer) {
                    ServerPlayer player = (ServerPlayer)entity;
                    PlayerSinsHandler.sin(player, 20);
                }
                item.m_41764_(0);
            }
        }
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.m_9236_().f_46443_) {
            ItemStack item = entity.m_32055_();
            item.m_41721_(item.m_41773_() + 1);
            entity.m_32045_(item.m_41777_());
            if (item.m_41773_() >= item.m_41776_()) {
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(0.25f).scalingOptions(0, 0, 10).color(0.3f, 0.7f, 1.0f).friction(0.7f).build();
                ((ServerLevel)entity.m_9236_()).m_8767_((ParticleOptions)ballParticleOptions, entity.m_20185_(), entity.m_20186_() + (double)entity.m_20206_(), entity.m_20189_(), 70, (double)0.1f, (double)0.1f, (double)0.1f, (double)0.4f);
                entity.m_146870_();
            }
        }
        return super.onEntityItemUpdate(stack, entity);
    }

    public void m_7373_(ItemStack p_41421_, Level p_339594_, List<Component> text, TooltipFlag p_41424_) {
        super.m_7373_(p_41421_, p_339594_, text, p_41424_);
    }
}

