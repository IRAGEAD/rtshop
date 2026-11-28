/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.init.FDScreenEffects
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.cutscenes.CameraPos
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData
 *  com.finderfeed.fdlib.systems.cutscenes.EasingType
 *  com.finderfeed.fdlib.systems.impact_frames.ImpactFrame
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.chromatic_abberation.ChromaticAbberationData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData
 *  com.finderfeed.fdlib.systems.shake.DefaultShakePacket
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.util.FDColor
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.BossInitializer;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSin;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.init.FDScreenEffects;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.cutscenes.CameraPos;
import com.finderfeed.fdlib.systems.cutscenes.CutsceneData;
import com.finderfeed.fdlib.systems.cutscenes.EasingType;
import com.finderfeed.fdlib.systems.impact_frames.ImpactFrame;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.chromatic_abberation.ChromaticAbberationData;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData;
import com.finderfeed.fdlib.systems.shake.DefaultShakePacket;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.util.FDColor;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class GeburahSecondPhaseInitializer
extends BossInitializer<GeburahEntity> {
    public GeburahSecondPhaseInitializer(GeburahEntity boss) {
        super(boss);
    }

    @Override
    public void onStart() {
        ((GeburahEntity)this.getBoss()).mainAttackChain.reset();
        CutsceneData cutsceneData = this.createCutsceneData();
        for (ServerPlayer serverPlayer : ((GeburahEntity)this.getBoss()).getArenaEntities(ServerPlayer.class)) {
            if (serverPlayer.m_21224_()) continue;
            FDLibCalls.startCutsceneForPlayer((ServerPlayer)serverPlayer, (CutsceneData)cutsceneData);
        }
    }

    private CutsceneData createCutsceneData() {
        GeburahEntity boss = (GeburahEntity)this.getBoss();
        Vec3 pos = boss.m_20182_();
        CutsceneData cutsceneData = CutsceneData.create().addScreenEffect(0, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 0, 0, 40).time(40).timeEasing(EasingType.EASE_IN_OUT);
        CameraPos lastPos = null;
        cutsceneData.addCameraPos(new CameraPos(pos.m_82520_(20.0, 4.0, 0.0), new Vec3(-1.0, 0.4, 0.0)));
        lastPos = new CameraPos(pos.m_82520_(28.0, 4.0, 0.0), new Vec3(-1.0, 0.5, 0.0));
        cutsceneData.addCameraPos(lastPos);
        CutsceneData cutsceneData1 = CutsceneData.create().addScreenEffect(0, FDScreenEffects.CHROMATIC_ABBERATION, (ScreenEffectData)new ChromaticAbberationData(0.025f), 0, 0, 20).time(40).timeEasing(EasingType.EASE_OUT);
        cutsceneData1.addCameraPos(new CameraPos(lastPos.getPos(), new Vec3(-1.0, 0.5, 0.0)));
        lastPos = new CameraPos(lastPos.getPos().m_82520_(2.0, -0.5, 0.0), new Vec3(-1.0, 0.6, 0.0));
        cutsceneData1.addCameraPos(lastPos);
        CutsceneData cutsceneData2 = CutsceneData.create().addScreenEffect(10, FDScreenEffects.SCREEN_COLOR, (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), 20, 20, 20).time(40).addCameraPos(lastPos);
        cutsceneData.nextCutscene(cutsceneData1.nextCutscene(cutsceneData2));
        return cutsceneData;
    }

    @Override
    public void onFinish() {
        ((GeburahEntity)this.getBoss()).mainAttackChain.reset();
    }

    @Override
    public void onTick() {
        GeburahEntity geburah = (GeburahEntity)this.getBoss();
        int tick = this.getTick();
        if (tick > 140) {
            this.setFinished();
        }
        if (tick >= 40) {
            if (tick == 40) {
                geburah.m_9236_().m_6263_(null, geburah.m_20185_(), geburah.m_20186_(), geburah.m_20189_(), BossSounds.GEBURAH_CORE_RAY_STRIKE.get(), SoundSource.HOSTILE, 5.0f, 0.8f);
                geburah.m_9236_().m_6263_(null, geburah.m_20185_(), geburah.m_20186_(), geburah.m_20189_(), BossSounds.CHESED_RAY.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
                for (ServerPlayer entity : BossTargetFinder.getEntitiesInCylinder(ServerPlayer.class, geburah.m_9236_(), geburah.m_20182_().m_82520_(0.0, -1.0, 0.0), 30.0f, 42.0f)) {
                    FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> entity), (Object)new DefaultShakePacket(FDShakeData.builder().outTime(20).amplitude(2.0f).build()));
                }
            }
            geburah.m_20088_().m_135381_(GeburahEntity.SECOND_PHASE, (Object)true);
        } else if (tick > 20) {
            if (tick == 21) {
                float angle = 2.0943952f;
                for (int i = 0; i < 3; ++i) {
                    StripeParticleOptions stripeParticleOptions = StripeParticleOptions.createHorizontalCircling(new FDColor(1.0f, 0.3f, 0.2f, 1.0f), new FDColor(1.0f, 0.6f, 0.2f, 1.0f), new Vec3(0.0, 1.0, 0.0), angle * (float)i, 0.3f, 20, 100, 0.0f, 10.0f, 0.5f, 0.5f, true, true);
                    FDLibCalls.sendParticles((ServerLevel)((ServerLevel)geburah.m_9236_()), (ParticleOptions)stripeParticleOptions, (Vec3)geburah.getCorePosition(), (double)200.0);
                }
            }
            BossUtil.geburahRayChargeParticles((ServerLevel)geburah.m_9236_(), geburah.getCorePosition(), 100.0, geburah);
            if (tick == 35) {
                FDLibCalls.sendImpactFrames((ServerLevel)((ServerLevel)((GeburahEntity)this.getBoss()).m_9236_()), (Vec3)((GeburahEntity)this.getBoss()).m_20182_(), (float)60.0f, (ImpactFrame[])new ImpactFrame[]{new ImpactFrame().setDuration(2), new ImpactFrame().setDuration(1).setInverted(true), new ImpactFrame().setDuration(1), new ImpactFrame().setDuration(1).setInverted(true), new ImpactFrame().setDuration(1)});
            }
        } else if (tick < 10) {
            geburah.propagateSins(0, new PlayerSin[0]);
            geburah.removeAllArenaTrash();
            geburah.judgementBirdSpawnTicker = 0;
            geburah.removeCrystalsFromPlayerInventories();
        }
    }
}

