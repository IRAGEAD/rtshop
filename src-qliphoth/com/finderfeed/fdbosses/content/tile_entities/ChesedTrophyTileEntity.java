/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Math
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.tile_entities;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningOptions;
import com.finderfeed.fdbosses.content.tile_entities.TrophyBlockEntity;
import com.finderfeed.fdbosses.init.BossTileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Vector3f;

public class ChesedTrophyTileEntity
extends TrophyBlockEntity {
    public ChesedTrophyTileEntity(BlockPos pos, BlockState state) {
        super(BossTileEntities.CHESED_TROPHY.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.f_58857_.f_46443_ && this.f_58857_.m_46467_() % 2L == 0L) {
            float rad = 0.5f;
            BlockPos bp = this.m_58899_();
            float height = 0.4f;
            Vec3 basePos = new Vec3((double)bp.m_123341_() + 0.5, (double)((float)bp.m_123342_() + height), (double)bp.m_123343_() + 0.5);
            float baseAngle = -Math.toRadians((float)(-this.getAngleFromState())) + 0.7853982f;
            float randomRange = 4.712389f;
            for (int i = 0; i < 2; ++i) {
                Vector3f endOffset = new Vector3f(0.0f, 0.0f, rad).rotateY(baseAngle + randomRange * this.f_58857_.f_46441_.m_188501_()).add(0.0f, -height, 0.0f);
                Vec3 end = basePos.m_82520_((double)endOffset.x, (double)endOffset.y, (double)endOffset.z);
                this.f_58857_.m_6493_((ParticleOptions)ArcLightningOptions.builder(BossParticles.ARC_LIGHTNING.get()).end(end.f_82479_, end.f_82480_, end.f_82481_).lifetime(2).color(1 + this.f_58857_.f_46441_.m_188503_(40), 183 + this.f_58857_.f_46441_.m_188503_(60), 165 + this.f_58857_.f_46441_.m_188503_(60)).lightningSpread(0.05f).width(0.05f).segments(6).circleOffset(this.f_58857_.f_46441_.m_188501_() * 0.5f - 0.5f).build(), true, basePos.f_82479_, basePos.f_82480_, basePos.f_82481_, 0.0, 0.0, 0.0);
            }
        }
    }
}

