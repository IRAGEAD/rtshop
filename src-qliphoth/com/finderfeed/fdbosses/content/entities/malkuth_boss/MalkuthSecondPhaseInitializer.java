/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.init.FDScreenEffects
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer$Mode
 *  com.finderfeed.fdlib.systems.cutscenes.CameraPos
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData
 *  com.finderfeed.fdlib.systems.cutscenes.EasingType
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.content.entities.BossInitializer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossInitializer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonEntity;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.init.FDScreenEffects;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer;
import com.finderfeed.fdlib.systems.cutscenes.CameraPos;
import com.finderfeed.fdlib.systems.cutscenes.CutsceneData;
import com.finderfeed.fdlib.systems.cutscenes.EasingType;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class MalkuthSecondPhaseInitializer
extends BossInitializer<MalkuthEntity> {
    public MalkuthSecondPhaseInitializer(MalkuthEntity boss) {
        super(boss);
    }

    @Override
    public void onStart() {
        MalkuthEntity boss = (MalkuthEntity)this.getBoss();
        boss.getAnimationSystem().stopAnimation("MAIN");
        boss.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build());
        boss.getHeadControllerContainer().setControllersMode(HeadControllerContainer.Mode.ANIMATION);
        Vec3 tppos = boss.spawnPosition.m_82549_(MalkuthEntity.WALL_OFFSET);
        boss.m_6021_(tppos.f_82479_, tppos.f_82480_, tppos.f_82481_);
        boss.m_7618_(EntityAnchorArgument.Anchor.EYES, tppos.m_82520_(0.0, 0.0, -100.0));
        CutsceneData data = this.createCutscene();
        for (ServerPlayer player : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, boss.m_9236_(), boss.spawnPosition.m_82520_(0.0, -2.0, 0.0), 30.0f, 40.0f)) {
            FDLibCalls.startCutsceneForPlayer((ServerPlayer)player, (CutsceneData)data);
        }
        boss.removeThingsForSecondPhase();
        MalkuthBossInitializer.teleportCombatants(boss);
    }

    private CutsceneData createCutscene() {
        Vec3 base = ((MalkuthEntity)this.getBoss()).spawnPosition;
        CameraPos start = new CameraPos(base.m_82520_(0.0, 16.148, 22.391), new Vec3(0.0, -0.053, 0.999));
        CameraPos end = new CameraPos(base.m_82520_(0.0, 20.653, 38.78), new Vec3(0.001, -0.185, -0.983));
        CutsceneData data1 = CutsceneData.create().time(22).addScreenEffect(0, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 0, 20).addCameraPos(start);
        CutsceneData data2 = CutsceneData.create().time(15).timeEasing(EasingType.EASE_IN_OUT).addCameraPos(start).addCameraPos(new CameraPos(base.m_82520_(4.695, 17.0, 29.002), new Vec3(-0.909, -0.417, -0.009))).addCameraPos(end);
        CutsceneData data3 = CutsceneData.create().time(80).addScreenEffect(60, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 20, 20, 40).addCameraPos(end);
        data1.nextCutscene(data2.nextCutscene(data3));
        return data1;
    }

    @Override
    public void onFinish() {
        MalkuthEntity boss = (MalkuthEntity)this.getBoss();
        Vec3 tppos = boss.spawnPosition;
        boss.m_6021_(tppos.f_82479_, tppos.f_82480_, tppos.f_82481_);
        boss.lookAtTarget = true;
        if (boss.m_5448_() != null) {
            boss.m_21563_().m_148051_((Entity)boss.m_5448_());
        }
        boss.getHeadControllerContainer().setControllersMode(HeadControllerContainer.Mode.LOOK);
    }

    @Override
    public void onTick() {
        MalkuthEntity boss = (MalkuthEntity)this.getBoss();
        int tick = this.getTick();
        int start = 10;
        int cannonShootTick = start + 20;
        int cannonDestroyTick = cannonShootTick + 42;
        int endTick = cannonDestroyTick + 40;
        if (tick == start) {
            boss.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_SWORD_FORWARD).setSpeed(0.75f).setToNullTransitionTime(0).build());
        } else if (tick == cannonShootTick) {
            List<MalkuthCannonEntity> cannons = boss.getMalkuthCannons();
            for (int i = 0; i < cannons.size(); ++i) {
                MalkuthCannonEntity cannon = cannons.get(i);
                Vec3 pos = i < cannons.size() / 2 ? boss.spawnPosition.m_82549_(MalkuthEntity.FIRE_PLAYER_CANNON_OFFSET) : boss.spawnPosition.m_82549_(MalkuthEntity.ICE_PLAYER_CANNON_OFFSET);
                cannon.shoot(List.of(pos), 0.0f);
            }
        } else if (tick == cannonDestroyTick) {
            List<MalkuthCannonEntity> playerCannons = boss.getPlayerCannons(false);
            for (MalkuthCannonEntity cannon : playerCannons) {
                cannon.setBrokenRequiresMaterials(true);
            }
        } else if (tick == endTick) {
            Vec3 tppos = boss.spawnPosition;
            boss.m_6021_(tppos.f_82479_, tppos.f_82480_, tppos.f_82481_);
            boss.m_7618_(EntityAnchorArgument.Anchor.EYES, tppos.m_82520_(0.0, 0.0, -100.0));
            boss.getAnimationSystem().stopAnimation("MAIN");
            boss.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build());
        } else if (tick >= endTick + 40) {
            this.setFinished();
        }
    }
}

