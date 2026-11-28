/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.LightBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerContextAssignable;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedEntity;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public class ChesedBossSpawner
extends BossSpawnerEntity {
    public ChesedBossSpawner(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            BlockPos pos = this.m_20097_().m_7494_();
            if (this.isActive()) {
                BlockState state = this.m_9236_().m_8055_(pos);
                if (state.m_60795_() && !state.m_60713_(Blocks.f_152480_)) {
                    this.m_9236_().m_7731_(pos, (BlockState)Blocks.f_152480_.m_49966_().m_61124_((Property)LightBlock.f_153657_, (Comparable)Integer.valueOf(15)), 3);
                }
            } else if (this.m_9236_().m_8055_(pos).m_60734_() == Blocks.f_152480_) {
                this.m_9236_().m_7731_(pos, Blocks.f_50016_.m_49966_(), 3);
            }
        } else if (this.m_9236_().f_46443_ && this.isActive()) {
            Vec3 add = new Vec3(0.5 + 0.5 * (double)this.f_19796_.m_188501_(), 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_()).m_82520_(0.0, 0.75, 0.0);
            float lightningAdditiveLength = 1.0f - this.f_19796_.m_188501_();
            Vec3 add2 = new Vec3(0.75 + (double)(lightningAdditiveLength * lightningAdditiveLength * 0.25f), 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_()).m_82520_(0.0, 0.75 + (double)this.f_19796_.m_188501_() * 0.25 - 0.125 + (double)(lightningAdditiveLength * 0.5f), 0.0);
            Vec3 rnd = this.m_20182_().m_82549_(add);
            Vec3 rnd2 = this.m_20182_().m_82549_(add2);
            double l = add.m_82542_(1.0, 0.0, 1.0).m_82553_() * 1.5;
            l *= l;
            this.m_9236_().m_6493_((ParticleOptions)BallParticleOptions.builder().size(0.15f).color(100 + this.f_19796_.m_188503_(50), 255, 255).scalingOptions(10, 0, 30).build(), true, rnd.f_82479_, rnd.f_82480_, rnd.f_82481_, 0.0, (double)(0.025f + this.f_19796_.m_188501_() * 0.025f) / Math.max(1.0, l), 0.0);
            if (this.m_9236_().m_46467_() % 10L == 0L) {
                this.m_9236_().m_6493_((ParticleOptions)LightningParticleOptions.builder().color(60, 200 + this.f_19796_.m_188503_(50), 255).lifetime(10).maxLightningSegments(3).randomRoll(true).build(), true, rnd2.f_82479_, rnd2.f_82480_, rnd2.f_82481_, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public EntityType<? extends BossSpawnerContextAssignable> getBossEntityType() {
        return BossEntities.CHESED.get();
    }

    @Override
    public Vec3 getPlayerItemsDropPosition(ServerPlayer serverPlayer, Vec3 deathPosition) {
        if (BossTargetFinder.isPointInCylinder(deathPosition, this.m_20182_().m_82520_(0.0, -2.0, 0.0), ChesedEntity.ARENA_HEIGHT, ChesedEntity.ARENA_RADIUS)) {
            Vec3 offset = this.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_((double)(ChesedEntity.ARENA_RADIUS + 4), 0.0, (double)(ChesedEntity.ARENA_RADIUS + 4)).m_82520_(0.0, 1.0, 0.0);
            return this.m_20182_().m_82549_(offset);
        }
        return null;
    }

    @Override
    public void setActive(boolean state) {
        super.setActive(state);
    }

    @Override
    public boolean canInteractWithBlockPos(BlockPos blockPos) {
        Vec3 v = blockPos.m_252807_();
        Vec3 pos = this.m_20182_();
        double yDiff = v.f_82480_ - pos.f_82480_;
        double hdist = v.m_82542_(1.0, 0.0, 1.0).m_82554_(pos.m_82542_(1.0, 0.0, 1.0));
        return !(hdist < (double)ChesedEntity.ARENA_RADIUS) || !(yDiff > -2.0) || !(yDiff < (double)ChesedEntity.ARENA_HEIGHT);
    }

    @Override
    public Component onArenaDestructionMessage() {
        return Component.m_237115_((String)"fdbosses.word.tried_to_break_arena").m_130940_(ChatFormatting.RED);
    }
}

