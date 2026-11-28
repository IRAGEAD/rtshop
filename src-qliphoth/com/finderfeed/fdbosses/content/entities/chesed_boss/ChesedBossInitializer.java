/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.init.FDScreenEffects
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.cutscenes.CameraPos
 *  com.finderfeed.fdlib.systems.cutscenes.CurveType
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData
 *  com.finderfeed.fdlib.systems.cutscenes.CutsceneData$StopMode
 *  com.finderfeed.fdlib.systems.cutscenes.EasingType
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData
 *  com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectType
 *  com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData
 *  com.finderfeed.fdlib.systems.shake.DefaultShakePacket
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss;

import com.finderfeed.fdbosses.content.entities.BossInitializer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedEntity;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.init.FDScreenEffects;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.cutscenes.CameraPos;
import com.finderfeed.fdlib.systems.cutscenes.CurveType;
import com.finderfeed.fdlib.systems.cutscenes.CutsceneData;
import com.finderfeed.fdlib.systems.cutscenes.EasingType;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectData;
import com.finderfeed.fdlib.systems.screen.screen_effect.ScreenEffectType;
import com.finderfeed.fdlib.systems.screen.screen_effect.instances.datas.ScreenColorData;
import com.finderfeed.fdlib.systems.shake.DefaultShakePacket;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class ChesedBossInitializer
extends BossInitializer<ChesedEntity> {
    public ChesedBossInitializer(ChesedEntity boss) {
        super(boss);
    }

    @Override
    public void onStart() {
        ChesedEntity chesedEntity = (ChesedEntity)this.getBoss();
        Level level = chesedEntity.m_9236_();
        Vec3 position = chesedEntity.m_20182_();
        Vec3 forward = chesedEntity.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_();
        List<Player> players = chesedEntity.getCombatants(true);
        Vec3 left = forward.m_82524_(1.5707964f);
        double distBetweenPlayers = 2.0;
        double d = (double)(players.size() - 1) * distBetweenPlayers;
        double hd = d / 2.0;
        Vec3 baseTPPos = position.m_82549_(forward.m_82542_(20.0, 0.0, 20.0));
        Vec3 basePlayerTPPos = baseTPPos.m_82549_(left.m_82548_().m_82542_(hd, 0.0, hd));
        int i = 0;
        CutsceneData cutsceneData = this.startCutsceneData(position, forward);
        for (Player player : players) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            FDLibCalls.sendScreenEffect((ServerPlayer)serverPlayer, (ScreenEffectType)((ScreenEffectType)FDScreenEffects.SCREEN_COLOR.get()), (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), (int)0, (int)10, (int)20);
            double dist = (double)i * distBetweenPlayers;
            Vec3 tppos = basePlayerTPPos.m_82549_(left.m_82542_(dist, 0.0, dist));
            Vec3 between = basePlayerTPPos.m_82546_(position).m_82542_(1.0, 0.0, 1.0);
            float angle = 180.0f + (float)Math.toDegrees(Math.atan2(between.f_82479_, between.f_82481_));
            serverPlayer.m_8999_((ServerLevel)serverPlayer.m_9236_(), tppos.f_82479_, tppos.f_82480_, tppos.f_82481_, angle, 0.0f);
            ++i;
            FDLibCalls.startCutsceneForPlayer((ServerPlayer)serverPlayer, (CutsceneData)cutsceneData);
        }
        chesedEntity.getAnimationSystem().startAnimation("APPEAR", AnimationTicker.builder(BossAnims.CHESED_APPEAR).setLoopMode(Animation.LoopMode.ONCE).setToNullTransitionTime(0).build());
    }

    private CutsceneData startCutsceneData(Vec3 bossPosition, Vec3 forwardVector) {
        CutsceneData data = new CutsceneData().moveCurveType(CurveType.CATMULLROM).timeEasing(EasingType.EASE_OUT).time(100).stopMode(CutsceneData.StopMode.UNSTOPPABLE);
        Vec3 firstPos = bossPosition.m_82520_(0.0, 4.0, 0.0).m_82549_(forwardVector.m_82542_(40.0, 40.0, 40.0));
        Vec3 secondPos = bossPosition.m_82520_(0.0, 1.5, 0.0).m_82549_(forwardVector.m_82542_(3.0, 3.0, 3.0));
        data.addCameraPos(new CameraPos(firstPos, forwardVector.m_82548_()));
        data.addCameraPos(new CameraPos(secondPos, forwardVector.m_82548_()));
        return data;
    }

    @Override
    public void onFinish() {
        ChesedEntity chesedEntity = (ChesedEntity)this.getBoss();
        List<Player> players = chesedEntity.getCombatants(true);
        for (Player player : players) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            FDLibCalls.stopCutsceneForPlayer((ServerPlayer)serverPlayer);
        }
    }

    @Override
    public void onTick() {
        int endTick = 120;
        int idleAfterEnd = 20;
        ChesedEntity entity = (ChesedEntity)this.getBoss();
        if (this.getTick() == endTick) {
            ChesedEntity chesedEntity = (ChesedEntity)this.getBoss();
            List<Player> players = chesedEntity.getCombatants(true);
            for (Player player : players) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                FDLibCalls.stopCutsceneForPlayer((ServerPlayer)serverPlayer);
            }
        } else if (this.getTick() == endTick - 1) {
            List<Player> players = entity.getCombatants(true);
            for (Player player : players) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                FDLibCalls.sendScreenEffect((ServerPlayer)serverPlayer, (ScreenEffectType)((ScreenEffectType)FDScreenEffects.SCREEN_COLOR.get()), (ScreenEffectData)new ScreenColorData(0.0f, 0.0f, 0.0f, 1.0f), (int)0, (int)20, (int)20);
            }
        } else if (this.getTick() == 29) {
            entity.m_9236_().m_6263_(null, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), BossSounds.ROCK_IMPACT.get(), SoundSource.HOSTILE, 5.0f, 0.85f);
        } else if (this.getTick() == 30) {
            PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)entity.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(10.0f).amplitude(10.0f).inTime(0).stayTime(0).outTime(10).build(), (Vec3)entity.m_20182_().m_82520_(0.0, (double)(ChesedEntity.ARENA_HEIGHT - 2), 0.0), (double)ChesedEntity.ARENA_HEIGHT);
            entity.rockfallCastStones(15, 30.0f, 0.41887903f);
            entity.m_9236_().m_6263_(null, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), BossSounds.CHESED_RAY.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
        } else if (this.getTick() == 48) {
            SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(entity.m_20097_(), entity.m_20182_().m_82520_(0.0, 0.5, 0.0), new Vec3(1.0, 0.0, 0.0)).maxAngle((float)Math.PI * 2).maxSpeed(0.3f).collectRadius(2).maxParticleLifetime(30).count(200).maxVerticalSpeedEdges(0.15f).maxVerticalSpeedCenter(0.15f));
            entity.m_9236_().m_6263_(null, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), BossSounds.CHESED_FLOOR_SMASH.get(), SoundSource.HOSTILE, 10.0f, 1.0f);
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(this::getBoss), (Object)packet);
            DefaultShakePacket defaultShakePacket = new DefaultShakePacket(FDShakeData.builder().inTime(2).stayTime(5).outTime(5).amplitude(0.2f).frequency(50.0f).build());
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(this::getBoss), (Object)defaultShakePacket);
        } else if (this.getTick() != 72 && this.getTick() != 82 && this.getTick() >= endTick + idleAfterEnd) {
            this.setFinished();
        }
    }
}

