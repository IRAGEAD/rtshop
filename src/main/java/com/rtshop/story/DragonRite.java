package com.rtshop.story;

import com.rtshop.RtShop;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 귀환의 의식 (용 소환식): 허공의 용은 처음부터 싸우러 나오지 않는다. 출구 받침대 위에 알로 굳어 잠들어 있고,
 * 받침대 네 모서리에 수정을 놓는 의식(바닐라의 용 다시 부르기)으로 깨운다. 그래서 조사대마다 자기 차례에 용을 부를 수 있다.
 * 구현: 누군가 허공에 처음 들어왔을 때 아직 한 번도 쓰러진 적 없는 용을 '이미 쓰러진 것'으로 정리한다 (전리품·경험치 없이).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class DragonRite {
    private DragonRite() {
    }

    /** 처음의 용을 재운다. 재웠으면 true. */
    public static boolean settle(ServerLevel end) {
        StorySystems.ensure();
        if (!StorySystems.dragonRite || end.dimension() != Level.END) return false;
        EndDragonFight fight = end.getDragonFight();
        if (fight == null || fight.hasPreviouslyKilledDragon()) return false;
        List<EnderDragon> dragons = new ArrayList<>(end.getDragons());
        if (dragons.isEmpty()) return false;
        try {
            for (EnderDragon d : dragons) {
                fight.setDragonKilled(d); // 출구를 열고 받침대 위에 알을 놓는다
                d.discard();
            }
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 용 소환식: 처음의 용을 재우지 못했습니다: {}", t.toString());
            return false;
        }
        for (ServerPlayer p : end.players()) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.rite.sleeping"));
        }
        return true;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer sp && sp.tickCount % 40 == 21 && sp.level().dimension() == Level.END) {
            settle(sp.serverLevel());
        }
    }
}
