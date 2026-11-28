/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boss_spawner;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerContextAssignable;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MalkuthBossSpawner
extends BossSpawnerEntity {
    public MalkuthBossSpawner(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            for (Player player : BossTargetFinder.getEntitiesInArc(Player.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -2.0, 0.0), new Vec2(0.0f, -1.0f), (float)Math.PI, 13.0f, 29.0f)) {
                if (!this.isActive()) {
                    if (player.m_21023_((MobEffect)BossEffects.MARK_OF_A_KNIGHT.get())) continue;
                    player.m_7292_(new MobEffectInstance((MobEffect)BossEffects.MARK_OF_A_KNIGHT.get(), -1, 0, true, false));
                    continue;
                }
                if (player.m_21023_((MobEffect)BossEffects.MARK_OF_A_KNIGHT.get())) {
                    player.m_21195_((MobEffect)BossEffects.MARK_OF_A_KNIGHT.get());
                }
                if (!player.m_21023_((MobEffect)BossEffects.MARK_OF_A_COWARD.get())) continue;
                player.m_21195_((MobEffect)BossEffects.MARK_OF_A_COWARD.get());
            }
        } else if (this.isActive()) {
            if (this.f_19797_ % 2 == 0) {
                Vec3 add = new Vec3(0.25 + 0.25 * (double)this.f_19796_.m_188501_(), 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_()).m_82520_(0.0, 0.75, 0.0);
                Vec3 rnd = this.m_20182_().m_82549_(add);
                double l = add.m_82542_(1.0, 0.0, 1.0).m_82553_() * (double)0.1f;
                l *= l;
                Vector3f color = this.f_19796_.m_188499_() ? MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.FIRE, this.f_19796_) : MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.ICE, this.f_19796_);
                float vspeed = 0.025f;
                this.m_9236_().m_6493_((ParticleOptions)BallParticleOptions.builder().size(0.15f).brightness(2).color(color.x, color.y, color.z).scalingOptions(10, 0, 30).build(), true, rnd.f_82479_, rnd.f_82480_, rnd.f_82481_, add.f_82479_ * (double)vspeed, (double)(0.025f + this.f_19796_.m_188501_() * 0.025f) / Math.max(1.0, l), add.f_82481_ * (double)vspeed);
            }
            if (this.f_19797_ % 10 == 0) {
                Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.getRandom(this.f_19796_));
                FDColor fireColorStart = new FDColor(colFire.x, colFire.y - this.f_19796_.m_188501_() * 0.1f - 0.3f, colFire.z, 0.5f);
                FDColor fireColor = new FDColor(colFire.x, colFire.y + this.f_19796_.m_188501_() * 0.1f, colFire.z, 1.0f);
                StripeParticleOptions stripeParticleOptions = StripeParticleOptions.createHorizontalCircling(fireColorStart, fireColor, new Vec3((double)0.01f, 1.0, 0.0), (float)Math.PI, 0.05f, 20, 40, 2.0f, 0.75f, 2.0f, 0.5f, this.f_19796_.m_188499_(), false);
                this.m_9236_().m_6493_((ParticleOptions)stripeParticleOptions, true, this.m_20185_(), this.m_20186_(), this.m_20189_(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public EntityType<? extends BossSpawnerContextAssignable> getBossEntityType() {
        return BossEntities.MALKUTH.get();
    }

    @Override
    public Vec3 getPlayerItemsDropPosition(ServerPlayer serverPlayer, Vec3 deathPosition) {
        if (BossTargetFinder.isPointInCylinder(deathPosition, this.m_20182_().m_82520_(0.0, -2.0, 0.0), 13.0f, 29.0f)) {
            return this.m_20182_().m_82520_(0.0, 3.0, -34.0);
        }
        return null;
    }

    @Override
    public boolean canInteractWithBlockPos(BlockPos blockPos) {
        Vec3 v = blockPos.m_252807_();
        Vec3 pos = this.m_20182_();
        double yDiff = v.f_82480_ - pos.f_82480_;
        double hdist = v.m_82542_(1.0, 0.0, 1.0).m_82554_(pos.m_82542_(1.0, 0.0, 1.0));
        return !(hdist < 39.0) || !(yDiff > -4.0) || !(yDiff < 211.0);
    }

    @Override
    public Component onArenaDestructionMessage() {
        return Component.m_237115_((String)"fdbosses.word.tried_to_break_arena").m_130940_(ChatFormatting.RED);
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }
}

