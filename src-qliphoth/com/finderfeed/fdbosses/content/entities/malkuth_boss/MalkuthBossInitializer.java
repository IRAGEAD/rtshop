/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.init.FDScreenEffects
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer$Mode
 *  com.finderfeed.fdlib.systems.cutscenes.CameraPos
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData
 *  com.finderfeed.fdlib.systems.cutscenes.EasingType
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectType
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData
 *  com.finderfeed.fdlib.systems.shake.DefaultShakePacket
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.BossInitializer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonEntity;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.init.FDScreenEffects;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadControllerContainer;
import com.finderfeed.fdlib.systems.cutscenes.CameraPos;
import com.finderfeed.fdlib.systems.cutscenes.CutsceneData;
import com.finderfeed.fdlib.systems.cutscenes.EasingType;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectType;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData;
import com.finderfeed.fdlib.systems.shake.DefaultShakePacket;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MalkuthBossInitializer
extends BossInitializer<MalkuthEntity> {
    private ProjectileMovementPath movePath;

    public MalkuthBossInitializer(MalkuthEntity boss) {
        super(boss);
    }

    @Override
    public void onStart() {
        MalkuthEntity malkuth = (MalkuthEntity)this.getBoss();
        Vec3 v1 = new Vec3(-6.5, -1.0, -25.0).m_82549_(malkuth.spawnPosition);
        Vec3 v2 = new Vec3(6.5, -1.0, -25.0).m_82549_(malkuth.spawnPosition);
        AABB aabb1 = new AABB(-5.0, -5.0, -5.0, 5.0, 5.0, 5.0).m_82383_(v1);
        AABB aabb2 = new AABB(-5.0, -5.0, -5.0, 5.0, 5.0, 5.0).m_82383_(v2);
        if (malkuth.m_9236_().m_45976_(MalkuthCannonEntity.class, aabb1).isEmpty()) {
            MalkuthCannonEntity cannon1 = MalkuthCannonEntity.summon(malkuth.m_9236_(), v1, v1.m_82520_(0.0, 0.0, 100.0), MalkuthAttackType.FIRE);
            cannon1.setPlayerControlled(true);
        }
        if (malkuth.m_9236_().m_45976_(MalkuthCannonEntity.class, aabb2).isEmpty()) {
            MalkuthCannonEntity cannon2 = MalkuthCannonEntity.summon(malkuth.m_9236_(), v2, v2.m_82520_(0.0, 0.0, 100.0), MalkuthAttackType.ICE);
            cannon2.setPlayerControlled(true);
        }
        CutsceneData cutsceneData = this.constructCutscene();
        MalkuthEntity boss = (MalkuthEntity)this.getBoss();
        Vec3 sppos = boss.spawnPosition;
        for (ServerPlayer player : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, malkuth.m_9236_(), malkuth.spawnPosition.m_82520_(0.0, -2.0, 0.0), 30.0f, 30.0f)) {
            FDLibCalls.startCutsceneForPlayer((ServerPlayer)player, (CutsceneData)cutsceneData);
        }
        MalkuthBossInitializer.teleportCombatants(boss);
    }

    private CutsceneData constructCutscene() {
        Vec3 base = ((MalkuthEntity)this.getBoss()).spawnPosition;
        CutsceneData data1 = CutsceneData.create().time(60).addScreenEffect(0, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 5, 15).addScreenEffect(40, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 20, 0, 0).addCameraPos(new CameraPos(base.m_82520_(10.281, 11.647, 22.042), new Vec3(0.623, -0.037, 0.781))).addCameraPos(new CameraPos(base.m_82520_(6.313, 11.647, 23.5), new Vec3(0.623, -0.037, 0.781)));
        CutsceneData data2 = CutsceneData.create().time(60).addScreenEffect(0, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 5, 15).addScreenEffect(40, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 20, 0, 0).addCameraPos(new CameraPos(base.m_82520_(-10.281, 11.647, 22.042), new Vec3(-0.623, -0.037, 0.781))).addCameraPos(new CameraPos(base.m_82520_(-6.313, 11.647, 23.5), new Vec3(-0.623, -0.037, 0.781)));
        CutsceneData data3 = CutsceneData.create().time(35).addScreenEffect(0, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 5, 15).addCameraPos(new CameraPos(base.m_82520_(0.0, 26.296, 36.434), new Vec3(0.0, 0.28, 0.96)));
        CameraPos headCameraPos = new CameraPos(base.m_82520_(0.0, 3.034, -2.649), new Vec3(-0.009, -0.612, 0.911));
        CameraPos lastCameraPos = new CameraPos(base.m_82520_(0.0, 2.4, -7.178), new Vec3(0.0, 0.017, 1.0));
        CutsceneData data4 = CutsceneData.create().time(35).lookEasing(EasingType.EASE_IN_OUT).timeEasing(EasingType.EASE_IN_OUT).addCameraPos(new CameraPos(base.m_82520_(0.0, 26.296, 36.434), new Vec3(0.0, 0.28, 0.96))).addCameraPos(new CameraPos(base.m_82520_(-0.223, 15.398, 9.756), new Vec3(-0.005, 0.497, 0.868))).addCameraPos(new CameraPos(base.m_82520_(0.452, 4.528, 1.622), new Vec3(-0.009, 0.698, 0.716))).addCameraPos(new CameraPos(base.m_82520_(0.0, 4.147, -1.75), new Vec3(0.0, 0.853, 0.522)));
        CutsceneData data42 = CutsceneData.create().time(10).timeEasing(EasingType.EASE_IN).addCameraPos(new CameraPos(base.m_82520_(0.0, 4.147, -1.75), new Vec3(0.0, 0.853, 0.522))).addCameraPos(headCameraPos);
        CutsceneData data43 = CutsceneData.create().time(10).addCameraPos(headCameraPos);
        CutsceneData data5 = CutsceneData.create().time(5).timeEasing(EasingType.EASE_OUT).addCameraPos(headCameraPos).addCameraPos(lastCameraPos);
        CutsceneData data6 = CutsceneData.create().time(10).addScreenEffect(9, (ScreenEffectType)FDScreenEffects.SCREEN_COLOR.get(), (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 20, 10).addCameraPos(lastCameraPos);
        data1.nextCutscene(data2.nextCutscene(data3.nextCutscene(data4.nextCutscene(data42.nextCutscene(data43.nextCutscene(data5.nextCutscene(data6)))))));
        return data1;
    }

    @Override
    public void onFinish() {
    }

    @Override
    public void onTick() {
        MalkuthEntity boss = (MalkuthEntity)this.getBoss();
        Vec3 base = boss.spawnPosition;
        Vec3 startPos = base.m_82520_(0.0, 23.62, 59.886);
        int tick = this.getTick();
        int movePathTime = 35;
        if (this.movePath == null) {
            this.movePath = new ProjectileMovementPath(movePathTime, false).addPos(startPos).addPos(base.m_82520_(0.0, 46.091, 44.675)).addPos(base.m_82520_(0.0, 49.459, 18.125)).addPos(base.m_82520_(0.0, 21.895, 3.019)).addPos(base);
            boss.m_6021_(startPos.f_82479_, startPos.f_82480_, startPos.f_82481_);
            boss.m_7618_(EntityAnchorArgument.Anchor.FEET, boss.m_20182_().m_82520_(0.0, 0.0, -100.0));
        }
        int bossJumpStart = 160;
        int erruptionStart = bossJumpStart - 5;
        int erruptionTime = 50;
        if (tick == erruptionStart - 30) {
            boss.m_9236_().m_6263_(null, startPos.f_82479_, startPos.f_82480_, startPos.f_82481_, BossSounds.MALKUTH_VOLCANO_RUMBLING_LOOP.get(), SoundSource.AMBIENT, 6.0f, 1.0f);
            DefaultShakePacket.send((ServerLevel)((ServerLevel)boss.m_9236_()), (Vec3)boss.spawnPosition, (double)30.0, (FDShakeData)FDShakeData.builder().amplitude(0.1f).stayTime(60).build());
        } else if (tick == erruptionStart) {
            boss.m_9236_().m_6263_(null, startPos.f_82479_, startPos.f_82480_, startPos.f_82481_, BossSounds.MALKUTH_VOLCANO_ERRUPTION.get(), SoundSource.AMBIENT, 6.0f, 1.0f);
            DefaultShakePacket.send((ServerLevel)((ServerLevel)boss.m_9236_()), (Vec3)boss.spawnPosition, (double)30.0, (FDShakeData)FDShakeData.builder().amplitude(1.0f).outTime(30).build());
        } else if (tick >= erruptionStart && tick <= erruptionStart + erruptionTime) {
            BossUtil.volcanoErruptionParticles((ServerLevel)boss.m_9236_(), startPos.m_82520_(0.0, 3.0, 0.0), 86, 120.0);
        }
        if (tick >= bossJumpStart) {
            if (tick == bossJumpStart) {
                boss.getHeadControllerContainer().setControllersMode(HeadControllerContainer.Mode.ANIMATION);
                boss.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_SUMMON_ANIM).setLoopMode(Animation.LoopMode.HOLD_ON_LAST_FRAME).setToNullTransitionTime(0).build());
            } else if (tick == bossJumpStart + movePathTime + 3) {
                DefaultShakePacket.send((ServerLevel)((ServerLevel)boss.m_9236_()), (Vec3)boss.spawnPosition, (double)30.0, (FDShakeData)FDShakeData.builder().amplitude(0.5f).outTime(10).build());
                boss.doJumpStartParticles(1.0f);
            }
            if (tick <= bossJumpStart + movePathTime) {
                boss.f_19794_ = true;
                boss.m_20242_(true);
                this.movePath.tick((Entity)boss);
            } else if (tick == bossJumpStart + movePathTime + 1) {
                boss.f_19794_ = false;
                boss.m_20242_(false);
                boss.m_6021_(base.f_82479_, base.f_82480_, base.f_82481_);
            } else if (tick == bossJumpStart + movePathTime + 50) {
                boss.getAnimationSystem().stopAnimation("MAIN");
                boss.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.MALKUTH_IDLE).build());
            } else if (tick >= bossJumpStart + movePathTime + 80) {
                this.setFinished();
            }
        }
        this.summonCannonsTicks();
    }

    private void summonCannonsTicks() {
        int tick = this.getTick();
        MalkuthEntity boss = (MalkuthEntity)this.getBoss();
        HashMap<Integer, Pair> cannonSpawnMap = new HashMap<Integer, Pair>(Map.of(9, new Pair((Object)new Vec3(9.0, 10.0, 29.5), (Object)MalkuthAttackType.ICE), 6, new Pair((Object)new Vec3(13.0, 10.0, 27.5), (Object)MalkuthAttackType.FIRE), 11, new Pair((Object)new Vec3(11.0, 3.0, 29.0), (Object)MalkuthAttackType.FIRE), 52, new Pair((Object)new Vec3(-9.0, 10.0, 29.5), (Object)MalkuthAttackType.ICE), 49, new Pair((Object)new Vec3(-13.0, 10.0, 27.5), (Object)MalkuthAttackType.FIRE), 48, new Pair((Object)new Vec3(-11.0, 3.0, 29.0), (Object)MalkuthAttackType.ICE)));
        if (cannonSpawnMap.containsKey(tick)) {
            Pair sppos = cannonSpawnMap.get(tick);
            Vec3 pos = ((Vec3)sppos.first).m_82549_(boss.spawnPosition);
            List<MalkuthCannonEntity> cannons = BossTargetFinder.getEntitiesInCylinder(MalkuthCannonEntity.class, boss.m_9236_(), pos.m_82520_(0.0, -0.1, 0.0), 4.0f, 2.0f);
            if (cannons.isEmpty()) {
                MalkuthCannonEntity.summon(boss.m_9236_(), pos, pos.m_82520_(0.0, 0.0, -100.0), (MalkuthAttackType)((Object)sppos.second));
            }
        }
    }

    public static void teleportCombatants(MalkuthEntity boss) {
        Vec3 spawnPos = boss.spawnPosition;
        Vec3 mainOffset = new Vec3(0.0, 0.0, -20.0);
        int t = 2;
        for (Player player : boss.getCombatants(true)) {
            Vec3 pos;
            int l = t % 3;
            if (l == 2) {
                pos = spawnPos.m_82549_(mainOffset);
            } else if (l == 1) {
                pos = spawnPos.m_82549_(mainOffset.m_82542_(-1.0, 0.0, 0.0));
            } else {
                mainOffset = mainOffset.m_82524_(0.3926991f);
                pos = spawnPos.m_82549_(mainOffset);
            }
            player.m_6021_(pos.f_82479_, pos.f_82480_ - 0.99, pos.f_82481_);
            ++t;
        }
    }
}

