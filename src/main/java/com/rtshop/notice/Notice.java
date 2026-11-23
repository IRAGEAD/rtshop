package com.rtshop.notice;

import com.rtshop.net.NoticePacket;
import com.rtshop.net.ShopNet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * RT Shop 알림: 채팅 대신 화면 아래(핫바 위)에 경고 문구처럼 잠시 보이는 알림 상자로 보낸다.
 * 누를 수 있는 글(링크·명령)이 든 알림은 채팅에서만 누를 수 있으므로 채팅으로 보낸다.
 */
public final class Notice {
    private Notice() {
    }

    /** 점검용: 지금까지 알림 상자로 보낸 수. */
    public static volatile int sentForTest;
    public static volatile Component lastForTest;

    public static void send(Player p, Component c) {
        send(p, c, false);
    }

    /**
     * 바닐라 displayClientMessage(c, overlay) 자리. overlay=true 였던 상태 알림(경고·진행도)은 한 칸을 바꿔 쓰고,
     * 나머지는 차례로 쌓인다.
     */
    public static void send(Player p, Component c, boolean status) {
        if (p == null || c == null) return;
        if (p.level().isClientSide && !hasClick(c)) {
            // 클라이언트 쪽에서 바로 띄우는 알림 (아이템 사용 등)
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.rtshop.client.NoticeHud.push(c, status));
            return;
        }
        if (!(p instanceof ServerPlayer sp) || hasClick(c)) {
            p.sendSystemMessage(c);
            return;
        }
        sentForTest++;
        lastForTest = c;
        if (sp instanceof FakePlayer || sp.connection == null) return;
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new NoticePacket(c, status));
        } catch (Exception e) {
            sp.sendSystemMessage(c);
        }
    }

    /** 모두에게 알림 (전설 뽑기·강화 소식 등). 서버 기록에도 남긴다. */
    public static void broadcast(net.minecraft.server.MinecraftServer server, Component c, boolean ignoredOverlay) {
        if (server == null || c == null) return;
        server.sendSystemMessage(c);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) send(p, c, false);
    }

    public static boolean hasClick(Component c) {
        if (c.getStyle().getClickEvent() != null) return true;
        if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
            for (Object a : t.getArgs()) if (a instanceof Component ac && hasClick(ac)) return true;
        }
        for (Component s : c.getSiblings()) if (hasClick(s)) return true;
        return false;
    }
}
