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
 * 런처가 전용 서버(시야 거리 6)로 바로 들어간 상태에서: 렌더 거리 12 로 두고 높이 올라 35도 아래를 내려다보며 찍고(고치기 전),
 * DhViewGuard 를 켜서 렌더 거리가 서버 시야 거리로 맞춰진 뒤 다시 찍는다. 결과 rtshop-dh-client.txt, 사진 170 · 171.
 * 3.71.0: 먼 풍경 자료가 서버에서 올 시간을 2분 반 주고, 찍을 때마다 DH 가 고리(150 칸) · 먼 곳(320 칸)에 LOD 자료를 갖고 있는지 적는다
 * (3.70.0 의 사진은 1 분 만에 찍어 먼 풍경이 아직 없었다).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = RtShop.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
final class ClientDhTest {
    private ClientDhTest() {
    }

    static final boolean ENABLED = "dh".equals(System.getProperty("rtshop.clienttest.only", ""));
    private static final List<String> NOTES = new ArrayList<>();
    private static int climb, probes;
    private static double hoverY = -1;
    /** 내려다보는 각도: 90 칸 위에서 35 도면 화면 가운데가 128 칸(8 청크) 앞, 곧 진짜 청크(6)와 먼 풍경 사이 고리에 닿는다. */
    private static final float PITCH = 35F;

    /** 올라간 높이에 떠 있게 한다 (서버나 다른 모드가 날기를 꺼도 떨어지지 않게). */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != net.minecraftforge.event.TickEvent.Phase.END || hoverY < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getAbilities().flying = true;
        mc.player.setDeltaMovement(0, 0, 0);
        if (Math.abs(mc.player.getY() - hoverY) > 0.5) mc.player.setPos(mc.player.getX(), hoverY, mc.player.getZ());
        mc.player.setXRot(PITCH);
    }

    static void run(Minecraft mc, int k) {
        if (mc.player == null) return;
        // 다른 모드가 띄우는 창(설정 동기화 확인 등)은 닫는다: 사진이 지형을 담게
        if (mc.screen != null) {
            RtShop.LOGGER.info("[rtshop-dh] closing {}", mc.screen.getClass().getName());
            mc.setScreen(null);
        }
        switch (k) {
            case 0 -> {
                // 맞춤을 끈다: 들어올 때 이미 맞춰 두었으면 원래 값으로 되돌린 뒤에 12 로 올린다 (다음 걸음)
                ClientConfig.get().dhMatchServer = false;
                ClientSelfTest.wait = 5;
            }
            case 1 -> {
                mc.options.renderDistance().set(12);
                NOTES.add("joined " + (mc.getCurrentServer() == null ? "?" : mc.getCurrentServer().ip) + " · render distance " + mc.options.renderDistance().get()
                        + " · effective " + mc.options.getEffectiveRenderDistance() + " · server view " + DhViewGuard.serverView
                        + " · DH " + net.minecraftforge.fml.ModList.get().isLoaded("distanthorizons") + " · Farsight " + net.minecraftforge.fml.ModList.get().isLoaded("farsight_view"));
                mc.player.getAbilities().flying = true;
                mc.player.onUpdateAbilities();
                climb = 0;
                hoverY = -1;
                ClientSelfTest.wait = 0;
            }
            case 2 -> {
                // 한 틱에 1 칸씩 90 칸 올라간다 (서버가 '너무 빠르다'로 막지 않게 천천히)
                if (climb < 90) {
                    climb++;
                    mc.player.getAbilities().flying = true;
                    mc.player.setDeltaMovement(0, 0, 0);
                    mc.player.setPos(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ());
                    mc.player.setXRot(PITCH);
                    ClientSelfTest.wait = 0;
                    stay();
                    return;
                }
                hoverY = mc.player.getY();   // 사진을 찍을 때까지 이 높이에 떠 있는다 (onTick)
                probes = 0;
                NOTES.add("t0s: " + lodProbe(mc));
                ClientSelfTest.wait = 600;   // 청크와 먼 풍경(서버가 보내는 LOD)이 들어올 때까지 2 분 반 (30초마다 LOD 자료를 센다)
            }
            case 3 -> {
                // 3.73.0: 먼 풍경이 들어오는 빠르기 (서버의 전송 한도를 바꿔 두 번 견준다: CI 의 DH 단계)
                if (probes < 4) {
                    probes++;
                    NOTES.add("t" + probes * 30 + "s: " + mc.levelRenderer.getChunkStatistics() + " · " + lodProbe(mc));
                    ClientSelfTest.wait = 600;
                    stay();
                    return;
                }
                shot(mc, "170_dh_gap_before");
                NOTES.add("before: render distance " + mc.options.renderDistance().get() + " · effective " + mc.options.getEffectiveRenderDistance()
                        + " · " + mc.levelRenderer.getChunkStatistics() + " · " + lodProbe(mc));
                ClientConfig.get().dhMatchServer = true;
                ClientSelfTest.wait = 900;
            }
            case 4 -> {
                shot(mc, "171_dh_gap_after");
                int raw = mc.options.renderDistance().get(), eff = mc.options.getEffectiveRenderDistance();
                NOTES.add("after: render distance " + raw + " · effective " + eff + " · " + mc.levelRenderer.getChunkStatistics() + " · " + lodProbe(mc));
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
                hoverY = -1;
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

    /**
     * DH 가 LOD 자료를 갖고 있는지 (DH API 를 리플렉션으로): 내 둘레 8 방향의 150 칸(고리) · 320 칸(먼 곳).
     * 자료가 있는데 사진에 안 보이면 그리기 문제, 자료가 없으면 서버에서 아직 안 온 것이다.
     */
    static String lodProbe(Minecraft mc) {
        try {
            Class<?> d = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
            Object world = d.getField("worldProxy").get(null), terrain = d.getField("terrainRepo").get(null);
            if (world == null || terrain == null) return "DH API not ready";
            Object level = null;
            java.lang.reflect.Method all = world.getClass().getMethod("getAllLoadedLevelWrappers");
            all.setAccessible(true);
            for (Object l : (Iterable<?>) all.invoke(world)) {
                if (String.valueOf(l).contains("overworld")) level = l;
            }
            if (level == null) return "DH: no overworld level";
            java.lang.reflect.Method col = null;
            StringBuilder names = new StringBuilder();
            for (java.lang.reflect.Method m : terrain.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getDeclaringClass() != Object.class) names.append(m.getName()).append('/').append(p.length).append(' ');
                // DH 3.3 의 모양: getColumnDataAtBlockPos(level, x, z, …) — 넷째 이후의 인자는 기본값으로 채운다
                if (p.length >= 3 && p[1] == int.class && p[2] == int.class && m.getName().toLowerCase(java.util.Locale.ROOT).contains("column")) col = m;
            }
            if (col == null) return "DH: no column lookup (" + names.toString().trim() + ")";
            col.setAccessible(true);
            int px = mc.player.getBlockX(), pz = mc.player.getBlockZ(), ring = 0, far = 0;
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                if (hasLod(col, terrain, level, px + (int) Math.round(Math.cos(a) * 150), pz + (int) Math.round(Math.sin(a) * 150))) ring++;
                if (hasLod(col, terrain, level, px + (int) Math.round(Math.cos(a) * 320), pz + (int) Math.round(Math.sin(a) * 320))) far++;
            }
            return "DH LOD data: ring (150) " + ring + "/8 · far (320) " + far + "/8";
        } catch (Throwable t) {
            return "DH probe: " + t;
        }
    }

    private static boolean hasLod(java.lang.reflect.Method col, Object terrain, Object level, int x, int z) throws Exception {
        Class<?>[] pt = col.getParameterTypes();
        Object[] args = new Object[pt.length];
        args[0] = level;
        args[1] = x;
        args[2] = z;
        for (int i = 3; i < pt.length; i++) {
            Class<?> t = pt[i];
            args[i] = t == boolean.class ? Boolean.FALSE : t == int.class ? 0 : t == long.class ? 0L : t == double.class ? 0D : t == float.class ? 0F
                    : t == byte.class ? (byte) 0 : t == short.class ? (short) 0 : null;
        }
        Object r = col.invoke(terrain, args);
        if (r == null) return false;
        if (!r.getClass().getField("success").getBoolean(r)) return false;
        Object payload = r.getClass().getField("payload").get(r);
        return payload != null && (!payload.getClass().isArray() || java.lang.reflect.Array.getLength(payload) > 0);
    }

    /** 같은 걸음을 다시 돌게 한다 (올라가는 동안). */
    private static void stay() {
        ClientSelfTest.repeatStep();
    }
}
