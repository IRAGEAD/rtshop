/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.init.FDScreenEffects
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.cutscenes.CameraPos
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData
 *  com.finderfeed.fdlib.systems.cutscenes.EasingType
 *  com.finderfeed.fdlib.systems.impact_frames.ImpactFrame
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectType
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.chromatic_abberation.ChromaticAbberationData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData
 *  com.finderfeed.fdlib.systems.shake.DefaultShakePacket
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.content.entities.BossInitializer;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_opening_floor.GeburahOpeningFloor;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.init.FDScreenEffects;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.cutscenes.CameraPos;
import com.finderfeed.fdlib.systems.cutscenes.CutsceneData;
import com.finderfeed.fdlib.systems.cutscenes.EasingType;
import com.finderfeed.fdlib.systems.impact_frames.ImpactFrame;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectType;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.chromatic_abberation.ChromaticAbberationData;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData;
import com.finderfeed.fdlib.systems.shake.DefaultShakePacket;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GeburahBossInitializer
extends BossInitializer<GeburahEntity> {
    public GeburahBossInitializer(GeburahEntity boss) {
        super(boss);
    }

    @Override
    public void onStart() {
        GeburahEntity geburah = (GeburahEntity)this.getBoss();
        geburah.getAnimationSystem().startAnimation("layer", AnimationTicker.builder(BossAnims.GEBURAH_APPEAR).build());
        Level level = ((GeburahEntity)this.getBoss()).m_9236_();
        GeburahOpeningFloor openingFloor = new GeburahOpeningFloor(BossEntities.GEBURAH_OPENING_FLOOR.get(), level);
        openingFloor.m_146884_(((GeburahEntity)this.getBoss()).m_20182_());
        level.m_7967_((Entity)openingFloor);
        CutsceneData cutscene = this.cutsceneData();
        List<ServerPlayer> players = BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, level, geburah.m_20182_(), 80.0f, 32.0f);
        for (ServerPlayer serverPlayer : players) {
            FDLibCalls.startCutsceneForPlayer((ServerPlayer)serverPlayer, (CutsceneData)cutscene);
        }
        int count = players.size();
        float angle = (float)Math.PI * 2 / (float)count;
        for (int i = 0; i < count; ++i) {
            float a = angle * (float)i;
            Vec3 v = new Vec3(20.0, 0.0, 0.0).m_82524_(a);
            Vec3 pos = v.m_82549_(geburah.m_20182_());
            ServerPlayer player = players.get(i);
            player.m_8999_((ServerLevel)geburah.m_9236_(), pos.f_82479_, pos.f_82480_, pos.f_82481_, (float)(-Math.toDegrees(a)) + 90.0f, 0.0f);
        }
    }

    @Override
    public void onFinish() {
        ((GeburahEntity)this.getBoss()).getAnimationSystem().stopAnimation("layer");
    }

    @Override
    public void onTick() {
        if (this.getTick() == ((Animation)BossAnims.GEBURAH_APPEAR.get()).getAnimTime() + 1) {
            FDLibCalls.sendImpactFrames((ServerLevel)((ServerLevel)((GeburahEntity)this.getBoss()).m_9236_()), (Vec3)((GeburahEntity)this.getBoss()).m_20182_(), (float)60.0f, (ImpactFrame[])new ImpactFrame[]{new ImpactFrame().setDuration(2), new ImpactFrame().setDuration(1).setInverted(true), new ImpactFrame().setDuration(1), new ImpactFrame().setDuration(1).setInverted(true), new ImpactFrame().setDuration(1)});
        } else if (this.getTick() == ((Animation)BossAnims.GEBURAH_APPEAR.get()).getAnimTime() - 3) {
            DefaultShakePacket.send((ServerLevel)((ServerLevel)((GeburahEntity)this.getBoss()).m_9236_()), (Vec3)((GeburahEntity)this.getBoss()).m_20182_(), (double)60.0, (FDShakeData)FDShakeData.builder().inTime(5).outTime(10).amplitude(2.0f).build());
        } else if (this.getTick() == ((Animation)BossAnims.GEBURAH_APPEAR.get()).getAnimTime()) {
            ((GeburahEntity)this.getBoss()).m_20088_().m_135381_(GeburahEntity.OPERATING, (Object)true);
        } else if (this.getTick() == ((Animation)BossAnims.GEBURAH_APPEAR.get()).getAnimTime() + 2) {
            Level level = ((GeburahEntity)this.getBoss()).m_9236_();
            GeburahEntity geburah = (GeburahEntity)this.getBoss();
            level.m_6263_(null, geburah.m_20185_(), geburah.m_20186_(), geburah.m_20189_(), BossSounds.GEBURAH_CORE_RAY_STRIKE.get(), SoundSource.HOSTILE, 5.0f, 0.8f);
            level.m_6263_(null, geburah.m_20185_(), geburah.m_20186_(), geburah.m_20189_(), BossSounds.CHESED_RAY.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
        } else if (this.getTick() == 100) {
            ((GeburahEntity)this.getBoss()).m_9236_().m_6263_(null, ((GeburahEntity)this.getBoss()).m_20185_(), ((GeburahEntity)this.getBoss()).m_20186_(), ((GeburahEntity)this.getBoss()).m_20189_(), BossSounds.GEBURAH_RISE.get(), SoundSource.AMBIENT, 3.0f, 1.0f);
        }
        if (this.getTick() > ((Animation)BossAnims.GEBURAH_APPEAR.get()).getAnimTime() + 100) {
            this.setFinished();
        }
    }

    private CutsceneData cutsceneData() {
        GeburahEntity geburah = (GeburahEntity)this.getBoss();
        Vec3 bossPos = geburah.m_20182_();
        CutsceneData cutsceneData = CutsceneData.create().addScreenEffect(0, (ScreenEffectType)FDScreenEffects.SCREEN_COLOR.get(), (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 0, 20).time(90).timeEasing(EasingType.EASE_IN_OUT);
        Vec3 p1 = new Vec3(0.0, 0.0, 8.0);
        Vec3 p2 = p1.m_82524_(0.3926991f);
        Vec3 p3 = p1.m_82524_(0.7853982f);
        Vec3 p4 = p1.m_82524_(1.1780972f);
        Vec3 p5 = p1.m_82524_(1.5707964f);
        p1 = p1.m_82549_(bossPos.m_82520_(0.0, 2.0, 0.0));
        p2 = p2.m_82549_(bossPos.m_82520_(0.0, 2.0, 0.0));
        p3 = p3.m_82549_(bossPos.m_82520_(0.0, 2.0, 0.0));
        p4 = p4.m_82549_(bossPos.m_82520_(0.0, 2.0, 0.0));
        p5 = p5.m_82549_(bossPos.m_82520_(0.0, 2.0, 0.0));
        cutsceneData.addCameraPos(new CameraPos(p1, bossPos.m_82546_(p1)));
        cutsceneData.addCameraPos(new CameraPos(p2, bossPos.m_82546_(p2)));
        cutsceneData.addCameraPos(new CameraPos(p3, bossPos.m_82546_(p3)));
        cutsceneData.addCameraPos(new CameraPos(p4, bossPos.m_82546_(p4)));
        CameraPos last = new CameraPos(p5, bossPos.m_82546_(p5));
        cutsceneData.addCameraPos(last);
        CutsceneData cutsceneData1 = CutsceneData.create().time(10).addCameraPos(last);
        CutsceneData cutsceneData2 = CutsceneData.create().time(50).timeEasing(EasingType.EASE_IN_OUT).addCameraPos(last);
        last = new CameraPos(last.getPos().m_82520_(17.0, 2.0, 0.0), new Vec3(-2.0, 0.75, 0.0));
        CutsceneData cutsceneData22 = cutsceneData2.addCameraPos(last);
        CutsceneData cutsceneData3 = CutsceneData.create().time(20).addCameraPos(last);
        CutsceneData cutsceneData4 = CutsceneData.create().addScreenEffect(0, FDScreenEffects.CHROMATIC_ABBERATION, (ScreenEffectData)new ChromaticAbberationData(0.025f), 0, 0, 20).time(30).addCameraPos(last).timeEasing(EasingType.EASE_OUT);
        last = new CameraPos(last.getPos().m_82520_(2.0, 0.0, 0.0), new Vec3(-2.0, 0.75, 0.0));
        CutsceneData cutsceneData42 = cutsceneData4.addCameraPos(last);
        CutsceneData cutsceneData5 = CutsceneData.create().addScreenEffect(10, (ScreenEffectType)FDScreenEffects.SCREEN_COLOR.get(), (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 20, 10, 20).time(30).addCameraPos(last);
        cutsceneData.nextCutscene(cutsceneData1.nextCutscene(cutsceneData22.nextCutscene(cutsceneData3.nextCutscene(cutsceneData42.nextCutscene(cutsceneData5)))));
        return cutsceneData;
    }
}

