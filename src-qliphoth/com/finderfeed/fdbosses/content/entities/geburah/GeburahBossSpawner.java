/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerContextAssignable;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block.GeburahRespiteBlock;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GeburahBossSpawner
extends BossSpawnerEntity {
    public GeburahBossSpawner(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.m_9236_().f_46443_ && this.isActive()) {
            Vec3 add = new Vec3(0.5 + 0.5 * (double)this.f_19796_.m_188501_(), 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
            Vec3 rnd = this.m_20182_().m_82549_(add);
            double l = add.m_82542_(1.0, 0.0, 1.0).m_82553_() * 1.5;
            l *= l;
            this.m_9236_().m_6493_((ParticleOptions)BallParticleOptions.builder().size(0.15f).color(100 + this.f_19796_.m_188503_(50), 255, 255).scalingOptions(10, 0, 30).build(), true, rnd.f_82479_, rnd.f_82480_, rnd.f_82481_, 0.0, (double)(0.025f + this.f_19796_.m_188501_() * 0.025f) / Math.max(1.0, l), 0.0);
        }
    }

    @Override
    public EntityType<? extends BossSpawnerContextAssignable> getBossEntityType() {
        return BossEntities.GEBURAH.get();
    }

    @Override
    public Vec3 getPlayerItemsDropPosition(ServerPlayer serverPlayer, Vec3 deathPosition) {
        Pair<ResourceKey<Level>, BlockPos> respawnData = GeburahRespiteBlock.getSpecialRespawnPoint(serverPlayer);
        if (respawnData != null) {
            BlockPos pos = (BlockPos)respawnData.second;
            return pos.m_252807_();
        }
        return deathPosition;
    }

    @Override
    public boolean canInteractWithBlockPos(BlockPos blockPos) {
        Vec3 pos = this.m_20182_();
        Vec3 block = blockPos.m_252807_();
        return !BossTargetFinder.isPointInCylinder(block, pos.m_82520_(0.0, -2.0, 0.0), 200.0f, 52.0f);
    }

    @Override
    public Component onArenaDestructionMessage() {
        return Component.m_237115_((String)"fdbosses.word.tried_to_break_arena").m_130940_(ChatFormatting.RED);
    }
}

