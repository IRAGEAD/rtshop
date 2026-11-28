/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.stats.Stats
 *  net.minecraft.tags.TagKey
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.EyeOfEnder
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Rarity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.gameevent.GameEvent
 *  net.minecraft.world.level.gameevent.GameEvent$Context
 *  net.minecraft.world.level.levelgen.structure.Structure
 */
package com.finderfeed.fdbosses.content.items;

import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.Structure;

public class LocatorEye<T extends EyeOfEnder>
extends Item {
    private TagKey<Structure> structureTagKey;
    private Supplier<EntityType<T>> eyeType;

    public LocatorEye(TagKey<Structure> structureTag, Supplier<EntityType<T>> eyeType) {
        super(new Item.Properties().m_41487_(64).m_41497_(Rarity.EPIC));
        this.structureTagKey = structureTag;
        this.eyeType = eyeType;
    }

    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.m_21120_(hand);
        player.m_6672_(hand);
        if (level instanceof ServerLevel) {
            ServerLevel serverlevel = (ServerLevel)level;
            BlockPos blockpos = serverlevel.m_215011_(this.structureTagKey, player.m_20183_(), 100, false);
            if (blockpos != null) {
                EyeOfEnder eyeofender = (EyeOfEnder)this.eyeType.get().m_20615_(level);
                eyeofender.m_6034_(player.m_20185_(), player.m_20227_(0.5), player.m_20189_());
                eyeofender.m_36972_(itemstack);
                eyeofender.m_36967_(blockpos);
                level.m_214171_(GameEvent.f_157778_, eyeofender.m_20182_(), GameEvent.Context.m_223717_((Entity)player));
                level.m_7967_((Entity)eyeofender);
                eyeofender.f_36954_ = true;
                float f = Mth.m_14179_((float)level.f_46441_.m_188501_(), (float)0.33f, (float)0.5f);
                level.m_6263_(null, player.m_20185_(), player.m_20186_(), player.m_20189_(), SoundEvents.f_11898_, SoundSource.NEUTRAL, 1.0f, f);
                itemstack.m_41774_(1);
                player.m_36246_(Stats.f_12982_.m_12902_((Object)this));
                player.m_21011_(hand, true);
                return InteractionResultHolder.m_19090_((Object)itemstack);
            }
            player.m_213846_((Component)Component.m_237115_((String)"fdbosses.word.cant_find_arena").m_130940_(ChatFormatting.RED));
        }
        return InteractionResultHolder.m_19096_((Object)itemstack);
    }

    public boolean m_5812_(ItemStack p_41453_) {
        return true;
    }
}

