/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance
 *  net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingApproachPhase
 *  net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
 *  net.minecraft.world.level.pathfinder.Node
 *  net.minecraft.world.level.pathfinder.Path
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.DragonArena;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingApproachPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DragonLandingApproachPhase.class})
public abstract class AbyssDragonLandingMixin
extends AbstractDragonPhaseInstance {
    @Shadow
    private Path f_31254_;

    @Shadow
    protected abstract void m_31264_();

    protected AbyssDragonLandingMixin(EnderDragon dragon) {
        super(dragon);
    }

    @Inject(method={"findNewTarget"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$localApproach(CallbackInfo ci) {
        if (!DragonArena.managed(this.f_31176_)) {
            return;
        }
        if (this.f_31254_ == null || this.f_31254_.m_77392_()) {
            int start = this.f_31176_.m_31155_();
            BlockPos origin = this.f_31176_.m_287165_();
            LivingEntity target = this.f_31176_.m_5448_();
            Vec3 approach = DragonArena.approach(origin, target == null ? null : target.m_20182_());
            int finish = this.f_31176_.m_31170_(approach.f_82479_, approach.f_82480_, approach.f_82481_);
            BlockPos landing = BlockPos.m_274446_((Position)DragonArena.landingTarget(this.f_31176_));
            this.f_31254_ = this.f_31176_.m_31104_(start, finish, new Node(landing.m_123341_(), landing.m_123342_(), landing.m_123343_()));
            if (this.f_31254_ != null) {
                this.f_31254_.m_77374_();
            }
        }
        this.m_31264_();
        if (this.f_31254_ != null && this.f_31254_.m_77392_()) {
            this.f_31176_.m_31157_().m_31416_(EnderDragonPhase.f_31380_);
        }
        ci.cancel();
    }
}

