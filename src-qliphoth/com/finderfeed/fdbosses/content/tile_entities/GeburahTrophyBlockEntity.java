/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.tile_entities;

import com.finderfeed.fdbosses.content.tile_entities.TrophyBlockEntity;
import com.finderfeed.fdbosses.init.BossTileEntities;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class GeburahTrophyBlockEntity
extends TrophyBlockEntity {
    public GeburahTrophyBlockEntity(BlockPos pos, BlockState state) {
        super(BossTileEntities.GEBURAH_TROPHY.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.f_58857_.f_46443_ && this.f_58857_.m_46467_() % 3L == 0L) {
            BallParticleOptions ballParticle = BallParticleOptions.builder().color(1.0f, 0.8f, 0.3f).scalingOptions(0, 0, 20).brightness(1).size(0.225f).build();
            Vec3 corePos = this.m_58899_().m_252807_().m_82520_(0.0, 1.65, 0.0);
            this.f_58857_.m_6493_((ParticleOptions)ballParticle, true, corePos.f_82479_, corePos.f_82480_, corePos.f_82481_, 0.0, 0.0, 0.0);
        }
    }
}

