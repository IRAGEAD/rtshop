package com.rtshop.land;

import com.rtshop.RtShop;
import com.rtshop.job.Jobs;
import com.rtshop.land.LandData.Claim;
import com.rtshop.land.LandData.Land;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 초보 농지: 농부(설정의 starterPlotJob) 직업을 얻은 플레이어에게 "starter" 묶음의 빈 구획 하나를 무료로 준다.
 * 한 사람당 한 번만 받는다. 반환하면 다시 묶음으로 돌아가 다음 초보 농부에게 간다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class StarterPlots {
    private StarterPlots() {
    }

    /** 직업이 바뀐 뒤나 접속할 때 부른다. 줄 것이 있으면 주고, 설정에 따라 회수한다. */
    public static void check(ServerPlayer p) {
        try {
            LandConfig cfg = LandConfig.get();
            if (cfg.starterPlotJob.isEmpty() || cfg.starterPlotGroup.isEmpty()) {
                return;
            }
            LandData data = LandData.get(p.server);
            boolean hasJob = Jobs.has(p, cfg.starterPlotJob);
            if (!hasJob) {
                if (cfg.starterPlotReturnWhenJobLost) {
                    for (Land l : data.landsOf(p.getUUID())) {
                        if (l.plot && cfg.starterPlotGroup.equals(l.group)) {
                            PlotManager.resetToSale(data, l);
                            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.starter_revoked", l.name).withStyle(ChatFormatting.YELLOW));
                        }
                    }
                }
                return;
            }
            if (data.starterGiven(p.getUUID())) {
                return;
            }
            Land free = null;
            for (Land l : data.lands()) {
                if (l.forSale() && cfg.starterPlotGroup.equals(l.group) && (free == null || l.id < free.id)) {
                    free = l;
                }
            }
            if (free == null) {
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.starter_none").withStyle(ChatFormatting.GOLD));
                for (ServerPlayer op : p.server.getPlayerList().getPlayers()) {
                    if (op.hasPermissions(2) && op != p) {
                        com.rtshop.notice.Notice.send(op, Component.translatable("rtshop.plot.starter_none_op", p.getGameProfile().getName())
                                .withStyle(ChatFormatting.GOLD));
                    }
                }
                return;
            }
            give(data, free, p);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] starter plot check failed", t);
        }
    }

    /** 초보 농지를 준다 (무료, 반환해도 돈은 없음). */
    public static void give(LandData data, Land land, ServerPlayer p) {
        land.owner = p.getUUID();
        land.ownerName = p.getGameProfile().getName();
        land.paid = 0;
        land.defaultPerms = 0;
        land.members.clear();
        data.setStarterGiven(p.getUUID(), true);
        data.touch();
        Claim c = PlotManager.plotClaim(land);
        int x = c == null ? 0 : (c.x0 + c.x1) / 2, z = c == null ? 0 : (c.z0 + c.z1) / 2;
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.starter_given", land.name, c == null ? 0 : c.area(), x, z)
                .withStyle(ChatFormatting.GREEN));
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.starter_hint").withStyle(ChatFormatting.GRAY));
        RtShop.LOGGER.info("[rtshop] {} 에게 초보 농지 '{}' 를 주었습니다", p.getGameProfile().getName(), land.name);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            check(sp);
        }
    }
}
