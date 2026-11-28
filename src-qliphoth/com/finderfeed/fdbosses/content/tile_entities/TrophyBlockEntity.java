/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.FDBlockEntity
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package com.finderfeed.fdbosses.content.tile_entities;

import com.finderfeed.fdbosses.content.blocks.TrophyBlock;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.FDBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class TrophyBlockEntity
extends FDBlockEntity {
    public TrophyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TrophyBlock getBlock() {
        return (TrophyBlock)this.m_58900_().m_60734_();
    }

    public void onClick(Player player) {
        TrophyBlock trophyBlock = this.getBlock();
        Supplier<Animation> clickAnim = trophyBlock.getClickAnimation();
        Supplier<Animation> idleAnim = trophyBlock.getIdleAnimation();
        if (clickAnim != null && idleAnim != null) {
            this.getAnimationSystem().startAnimation("IDLE", AnimationTicker.builder(clickAnim).setLoopMode(Animation.LoopMode.ONCE).nextAnimation(AnimationTicker.builder(idleAnim).setLoopMode(Animation.LoopMode.LOOP).build()).build());
        }
    }

    public void tick() {
        TrophyBlock trophyBlock;
        super.tick();
        if (this.f_58857_.f_46443_ && (trophyBlock = this.getBlock()).getIdleAnimation() != null && this.getAnimationSystem().getTicker("IDLE") == null) {
            this.getAnimationSystem().startAnimation("IDLE", AnimationTicker.builder(trophyBlock.getIdleAnimation()).setLoopMode(Animation.LoopMode.LOOP).build());
        }
    }

    public int getAngleFromState() {
        BlockState state = this.m_58900_();
        Direction facing = (Direction)state.m_61143_((Property)HorizontalDirectionalBlock.f_54117_);
        int angle = 0;
        switch (facing) {
            case SOUTH: {
                angle = 180;
                break;
            }
            case WEST: {
                angle = 90;
                break;
            }
            case EAST: {
                angle = 270;
            }
        }
        return angle;
    }
}

