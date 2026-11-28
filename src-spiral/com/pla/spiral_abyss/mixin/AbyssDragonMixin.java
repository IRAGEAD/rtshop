/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ExperienceOrb
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.boss.enderdragon.EndCrystal
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.level.dimension.end.EndDragonFight
 *  net.minecraft.world.level.pathfinder.Node
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironments;
import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.encounter.DragonCombat;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={EnderDragon.class})
public abstract class AbyssDragonMixin {
    @Shadow
    private EndDragonFight f_31073_;
    @Shadow
    private BlockPos f_286933_;
    @Shadow
    @Final
    private Node[] f_31077_;
    @Shadow
    @Final
    private int[] f_31078_;
    @Unique
    private final DragonCombat spiralAbyss$combat = new DragonCombat();
    @Unique
    private static final EntityDataAccessor<BlockPos> SPIRAL_ABYSS_ORIGIN = SynchedEntityData.m_135353_(EnderDragon.class, (EntityDataSerializer)EntityDataSerializers.f_135038_);
    @Unique
    private static final EntityDataAccessor<Optional<UUID>> SPIRAL_ABYSS_CRYSTAL = SynchedEntityData.m_135353_(EnderDragon.class, (EntityDataSerializer)EntityDataSerializers.f_135041_);

    @Inject(method={"aiStep"}, at={@At(value="HEAD")})
    private void spiralAbyss$combatPhases(CallbackInfo ci) {
        this.spiralAbyss$combat.tick((EnderDragon)this);
    }

    @Inject(method={"defineSynchedData"}, at={@At(value="TAIL")})
    private void spiralAbyss$defineData(CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon)this;
        dragon.m_20088_().m_135372_(SPIRAL_ABYSS_ORIGIN, (Object)BlockPos.f_121853_);
        dragon.m_20088_().m_135372_(SPIRAL_ABYSS_CRYSTAL, Optional.empty());
    }

    @Inject(method={"setFightOrigin"}, at={@At(value="TAIL")})
    private void spiralAbyss$syncOrigin(BlockPos origin, CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon)this;
        if (DragonArena.abyss(dragon) && !dragon.m_9236_().f_46443_) {
            dragon.m_20088_().m_135381_(SPIRAL_ABYSS_ORIGIN, (Object)origin);
        }
    }

    @Inject(method={"onSyncedDataUpdated"}, at={@At(value="HEAD")})
    private void spiralAbyss$receiveOrigin(EntityDataAccessor<?> key, CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon)this;
        if (key.equals(SPIRAL_ABYSS_ORIGIN) && DragonArena.abyss(dragon)) {
            this.f_286933_ = (BlockPos)dragon.m_20088_().m_135370_(SPIRAL_ABYSS_ORIGIN);
        }
        if (key.equals(SPIRAL_ABYSS_CRYSTAL) && DragonArena.abyss(dragon) && ((Optional)dragon.m_20088_().m_135370_(SPIRAL_ABYSS_CRYSTAL)).isEmpty()) {
            dragon.f_31086_ = null;
        }
    }

    @Inject(method={"aiStep", "tickDeath", "kill"}, at={@At(value="HEAD")})
    private void spiralAbyss$detachEndFight(CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon)this;
        if (!DragonArena.abyss(dragon)) {
            return;
        }
        this.f_31073_ = null;
        if (DragonArena.defeated((LivingEntity)dragon)) {
            dragon.f_31086_ = null;
            if (!dragon.m_9236_().f_46443_) {
                dragon.m_20088_().m_135381_(SPIRAL_ABYSS_CRYSTAL, Optional.empty());
            }
        }
        if (DragonArena.managed(dragon)) {
            dragon.m_287266_(BlockPos.m_122022_((long)AbyssPlatform.data((Entity)dragon).m_128454_("spiral_abyss_dragon_origin")));
        }
    }

    @Inject(method={"setDragonFight"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$noEndFight(EndDragonFight fight, CallbackInfo ci) {
        if (DragonArena.abyss((EnderDragon)this)) {
            this.f_31073_ = null;
            ci.cancel();
        }
    }

    @Redirect(method={"aiStep"}, at=@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerLevel;getDragonFight()Lnet/minecraft/world/level/dimension/end/EndDragonFight;"))
    private EndDragonFight spiralAbyss$noAutomaticFight(ServerLevel level) {
        return DragonArena.abyss((EnderDragon)this) ? null : level.m_8586_();
    }

    @Inject(method={"findClosestNode()I"}, at={@At(value="HEAD")})
    private void spiralAbyss$localNodes(CallbackInfoReturnable<Integer> ci) {
        EnderDragon dragon = (EnderDragon)this;
        if (!DragonArena.managed(dragon) || this.f_31077_[0] != null) {
            return;
        }
        for (int index = 0; index < this.f_31077_.length; ++index) {
            BlockPos pos = DragonArena.node(this.f_286933_, index);
            this.f_31077_[index] = new Node(pos.m_123341_(), pos.m_123342_(), pos.m_123343_());
        }
        int[] graph = new int[]{6146, 8197, 8202, 16404, 32808, 32848, 65696, 131392, 131712, 263424, 526848, 525313, 1581057, 3166214, 2138120, 6373424, 4358208, 12910976, 9044480, 9706496, 15216640, 0xD0E000, 11763712, 0x7E0000};
        System.arraycopy(graph, 0, this.f_31078_, 0, graph.length);
    }

    @Inject(method={"checkCrystals"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$ownedCrystals(CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon)this;
        if (!DragonArena.abyss(dragon)) {
            return;
        }
        if (DragonArena.defeated((LivingEntity)dragon)) {
            dragon.f_31086_ = null;
            if (!dragon.m_9236_().f_46443_) {
                dragon.m_20088_().m_135381_(SPIRAL_ABYSS_CRYSTAL, Optional.empty());
            }
            ci.cancel();
            return;
        }
        if (dragon.m_9236_().f_46443_) {
            Optional selected = (Optional)dragon.m_20088_().m_135370_(SPIRAL_ABYSS_CRYSTAL);
            dragon.f_31086_ = selected.flatMap(id -> dragon.m_9236_().m_6443_(EndCrystal.class, dragon.m_20191_().m_82400_(96.0), crystal -> crystal.m_20148_().equals(id)).stream().findFirst()).orElse(null);
        } else {
            dragon.f_31086_ = dragon.m_9236_().m_6443_(EndCrystal.class, dragon.m_20191_().m_82400_(64.0), crystal -> BossEnvironments.crystalFor(crystal, dragon)).stream().min(Comparator.comparingDouble(arg_0 -> ((EnderDragon)dragon).m_20280_(arg_0))).orElse(null);
            if (dragon.f_31086_ != null && dragon.f_19797_ % 10 == 0 && dragon.m_21223_() < dragon.m_21233_()) {
                dragon.m_21153_(Math.min(dragon.m_21233_(), dragon.m_21223_() + 1.0f));
            }
            dragon.m_20088_().m_135381_(SPIRAL_ABYSS_CRYSTAL, Optional.ofNullable(dragon.f_31086_).map(Entity::m_20148_));
        }
        ci.cancel();
    }

    @Redirect(method={"tickDeath"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void spiralAbyss$noDragonXp(ServerLevel level, Vec3 position, int amount) {
        if (!DragonArena.abyss((EnderDragon)this)) {
            ExperienceOrb.m_147082_((ServerLevel)level, (Vec3)position, (int)amount);
        }
    }
}

