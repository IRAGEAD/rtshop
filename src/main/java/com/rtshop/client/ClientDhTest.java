package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.rtshop.client.ClientSelfTest.check;
import static com.rtshop.client.ClientSelfTest.shot;

/**
 * Distant Horizons 의 가까운 청크 빈틈 재현과 고침 확인 (3.70.0). JVM 옵션 -Drtshop.clienttest=true -Drtshop.clienttest.only=dh,
 * 런처가 전용 서버(시야 거리 6)로 바로 들어간 상태에서: 렌더 거리 12 로 두고 높이 올라 지평선을 찍고(고치기 전),
 * DhViewGuard 를 켜서 렌더 거리가 서버 시야 거리로 맞춰진 뒤 다시 찍는다. 결과 rtshop-dh-client.txt, 사진 170 · 171.
 */
final class ClientDhTest {
    private ClientDhTest() {
    }

    static final boolean ENABLED = "dh".equals(System.getProperty("rtshop.clienttest.only", ""));
    private static final List<String> NOTES = new ArrayList<>();
    private static int climb;

    static void run(Minecraft mc, int k) {
        if (mc.player == null) return;
        // 다른 모드가 띄우는 창(설정 동기화 확인 등)은 닫는다: 사진이 지형을 담게
        if (mc.screen != null) {
            RtShop.LOGGER.info("[rtshop-dh] closing {}", mc.screen.getClass().getName());
            mc.setScreen(null);
        }
        switch (k) {
            case 0 -> {
                ClientConfig.get().dhMatchServer = false;
                mc.options.renderDistance().set(12);
                ClientSelfTest.wait = 200;
            }
            case 1 -> {
                NOTES.add("joined " + (mc.getCurrentServer() == null ? "?" : mc.getCurrentServer().ip) + " · render distance " + mc.options.renderDistance().get()
                        + " · effective " + mc.options.getEffectiveRenderDistance() + " · server view " + DhViewGuard.serverView
                        + " · DH " + net.minecraftforge.fml.ModList.get().isLoaded("distanthorizons") + " · Farsight " + net.minecraftforge.fml.ModList.get().isLoaded("farsight_view"));
                mc.player.getAbilities().flying = true;
                climb = 0;
                ClientSelfTest.wait = 0;
            }
            case 2 -> {
                // 한 틱에 1 칸씩 90 칸 올라간다 (서버가 '너무 빠르다'로 막지 않게 천천히)
                if (climb < 90) {
                    climb++;
                    mc.player.setDeltaMovement(0, 0, 0);
                    mc.player.setPos(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ());
                    mc.player.setXRot(12F);
                    ClientSelfTest.wait = 0;
                    stay();
                    return;
                }
                ClientSelfTest.wait = 1200;   // 청크와 먼 풍경이 들어올 때까지 1 분
            }
            case 3 -> {
                shot(mc, "170_dh_gap_before");
                NOTES.add("before: render distance " + mc.options.renderDistance().get() + " · effective " + mc.options.getEffectiveRenderDistance()
                        + " · " + mc.levelRenderer.getChunkStatistics());
                ClientConfig.get().dhMatchServer = true;
                ClientSelfTest.wait = 600;
            }
            case 4 -> {
                shot(mc, "171_dh_gap_after");
                int raw = mc.options.renderDistance().get(), eff = mc.options.getEffectiveRenderDistance();
                NOTES.add("after: render distance " + raw + " · effective " + eff + " · " + mc.levelRenderer.getChunkStatistics());
                check("3.70.0 with Distant Horizons on a server, the render distance is set to what the server sends (no empty ring between real chunks and far views), even with Farsight",
                        DhViewGuard.serverView >= 2 && raw == DhViewGuard.serverView && DhViewGuard.clampedForTest == raw,
                        raw + " / effective " + eff + " / server " + DhViewGuard.serverView + " / clamped " + DhViewGuard.clampedForTest);
                for (String n : NOTES) RtShop.LOGGER.info("[rtshop-dh] {}", n);
                try {
                    Files.write(mc.gameDirectory.toPath().resolve("rtshop-dh-notes.txt"), NOTES, StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                }
                ClientSelfTest.wait = 20;
            }
            case 5 -> {
                if (mc.getConnection() != null) mc.getConnection().getConnection().disconnect(net.minecraft.network.chat.Component.literal("done"));
                ClientSelfTest.wait = 60;
            }
            case 6 -> {
                ClientSelfTest.finish(mc);
                mc.stop();
            }
            default -> {
            }
        }
    }

    /** 같은 걸음을 다시 돌게 한다 (올라가는 동안). */
    private static void stay() {
        ClientSelfTest.repeatStep();
    }
}
