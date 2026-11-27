package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rtshop.client.ClientSelfTest.onServer;
import static com.rtshop.client.ClientSelfTest.shot;

/**
 * 화면 쪽 성능 측정 (3.70.0). JVM 옵션 -Drtshop.clienttest=true -Drtshop.clienttest.only=perf 일 때만 돈다 (CI 의 perf job).
 * 실제 지형 세계에서: 마을(RT Shop NPC 24 명) 앞에서 고개를 돌리며 · 날아서 새 땅을 지나며 · 몹 60 마리 곁에서, FPS 와 내장 서버의 틱 시간을 잰다.
 * 단계의 시작 · 끝 시각을 적어 JFR 표본을 단계별로 나눈다. 결과: rtshop-perf-client.txt.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
final class ClientPerfTest {
    private ClientPerfTest() {
    }

    static final boolean ENABLED = "perf".equals(System.getProperty("rtshop.clienttest.only", ""));
    private static final List<String> OUT = new ArrayList<>();
    private static final List<Integer> FPS = new ArrayList<>();
    private static final List<Double> MSPT = new ArrayList<>();
    private static String phase = "";
    private static int left;
    private static long phaseStart;
    private static boolean fly;
    private static double flyY;
    private static int frames;
    private static final String VARIANT = System.getProperty("rtshop.perf.variant", "base");

    /** k 번째 걸음 (ClientSelfTest 의 걸음 기계가 부른다: 걸음 사이에 내장 서버가 밀린 일을 마칠 때까지 기다린다). */
    static void run(Minecraft mc, int k) {
        switch (k) {
            case 0 -> ClientSelfTest.wait = 300;   // 세계가 다 뜰 때까지
            case 1 -> {
                OUT.add("RT Shop performance (client)");
                OUT.add("mods " + net.minecraftforge.fml.ModList.get().size() + " · render distance " + mc.options.renderDistance().get()
                        + " · graphics " + mc.options.graphicsMode().get() + " · GL " + com.mojang.blaze3d.platform.GlUtil.getRenderer() + " " + com.mojang.blaze3d.platform.GlUtil.getOpenGLVersion());
                onServer(mc, sp -> {
                    var level = sp.serverLevel();
                    var server = sp.server;
                    level.setDayTime(6000);
                    server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    level.setWeatherParameters(6000, 0, false, false);
                    sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                    sp.getAbilities().flying = true;
                    sp.onUpdateAbilities();
                    BlockPos s = level.getSharedSpawnPos();
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.getX(), s.getZ());
                    sp.teleportTo(level, s.getX() + 0.5, y + 1.6, s.getZ() + 0.5, 0F, 10F);
                    String[] kinds = {"blacksmith", "farmer", "fisher", "food", "general", "land", "raid", "gacha_shop", "attendance_shop", "bank"};
                    for (int i = 0; i < 24; i++) {
                        double a = i * Math.PI * 2 / 24, r = 5 + (i % 3) * 3;
                        int x = s.getX() + (int) Math.round(Math.cos(a) * r), z = s.getZ() + (int) Math.round(Math.sin(a) * r);
                        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        com.rtshop.shop.ShopServer.spawnNpc(level, new Vec3(x + 0.5, h, z + 0.5), (float) Math.toDegrees(a) + 90F, kinds[i % kinds.length]);
                    }
                });
                ClientSelfTest.wait = 200;
            }
            case 2 -> begin("town", 600, false);
            case 3 -> {
                shot(mc, "160_perf_town");
                if (mc.player != null) flyY = mc.player.getY() + 30;
                begin("explore", 1200, true);
            }
            case 4 -> {
                onServer(mc, sp -> {
                    var level = sp.serverLevel();
                    Object[][] mobs = {{EntityType.ZOMBIE, 14}, {EntityType.SKELETON, 10}, {EntityType.SPIDER, 6}, {EntityType.COW, 10},
                            {EntityType.SHEEP, 10}, {EntityType.VILLAGER, 10}};
                    int k2 = 0;
                    for (Object[] m : mobs) {
                        for (int i = 0; i < (Integer) m[1]; i++, k2++) {
                            double a = k2 * 2.39996, r = 6 + (k2 % 9) * 2;
                            int x = sp.getBlockX() + (int) Math.round(Math.cos(a) * r), z = sp.getBlockZ() + (int) Math.round(Math.sin(a) * r);
                            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                            Entity e = ((EntityType<?>) m[0]).create(level);
                            if (e == null) continue;
                            e.moveTo(x + 0.5, h, z + 0.5, (float) Math.toDegrees(a), 0F);
                            if (e instanceof Mob mob) {
                                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.COMMAND, null, null);
                                mob.setPersistenceRequired();
                            }
                            level.addFreshEntity(e);
                        }
                    }
                    // 몹 곁으로 내려간다
                    int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp.getBlockX(), sp.getBlockZ());
                    sp.teleportTo(level, sp.getX(), h + 6, sp.getZ(), sp.getYRot(), 25F);
                });
                ClientSelfTest.wait = 100;
            }
            case 5 -> begin("mobs", 600, false);
            case 6 -> {
                shot(mc, "161_perf_mobs");
                // 지도 품질(FANCY · FAST)을 눈으로 견주게 큰 지도를 열어 찍는다 (마을 위로 돌아가서)
                onServer(mc, sp -> {
                    var level = sp.serverLevel();
                    BlockPos s = level.getSharedSpawnPos();
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.getX(), s.getZ());
                    sp.teleportTo(level, s.getX() + 0.5, y + 1.6, s.getZ() + 0.5, 0F, 10F);
                });
                ClientSelfTest.wait = 200;
            }
            case 7 -> {
                openMap(mc);
                ClientSelfTest.wait = 100;
            }
            case 8 -> {
                if (mc.screen != null && mc.screen.getClass().getName().startsWith("dev.diorama")) shot(mc, "162_perf_map_" + VARIANT);
                mc.setScreen(null);
                OUT.add("");
                OUT.add("RESULT: DONE");
                try {
                    Files.write(mc.gameDirectory.toPath().resolve("rtshop-perf-client.txt"), OUT, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    RtShop.LOGGER.error("[rtshop-perf] could not write the client result", e);
                }
                com.rtshop.test.PerfTest.dumpJfr();
                ClientSelfTest.finish(mc);
                mc.stop();
            }
            default -> {
            }
        }
    }

    private static void begin(String name, int ticks, boolean flying) {
        phase = name;
        left = ticks;
        fly = flying;
        phaseStart = System.currentTimeMillis();
        frames = 0;
        FPS.clear();
        MSPT.clear();
        ClientSelfTest.wait = ticks + 10;
    }

    /** Diorama 의 큰 지도 (없거나 열 수 없으면 그냥 지나간다). */
    private static void openMap(Minecraft mc) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("diorama")) return;
        try {
            Class<?> ms = Class.forName("dev.diorama.MapScreen");
            for (java.lang.reflect.Constructor<?> k : ms.getDeclaredConstructors()) {
                Class<?>[] ps = k.getParameterTypes();
                Object[] args = new Object[ps.length];
                boolean ok = true;
                for (int i = 0; i < ps.length; i++) {
                    if (ps[i] == boolean.class) args[i] = false;
                    else if (ps[i] == int.class) args[i] = 0;
                    else if (ps[i] == double.class) args[i] = 0D;
                    else if (ps[i] == float.class) args[i] = 0F;
                    else if (ps[i].isPrimitive()) ok = false;
                }
                if (!ok) continue;
                try {
                    k.setAccessible(true);
                    mc.setScreen((net.minecraft.client.gui.screens.Screen) k.newInstance(args));
                    return;
                } catch (Throwable t) {
                    RtShop.LOGGER.info("[rtshop-perf] map screen {}: {}", java.util.Arrays.toString(ps), t.toString());
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.info("[rtshop-perf] map screen: {}", t.toString());
        }
    }

    /** 그린 화면 수 (FPS 를 소수점까지: 소프트웨어 그리기에서는 초당 2~3 장이라 정수 FPS 로는 차이가 안 보인다). */
    @SubscribeEvent
    public static void onFrame(TickEvent.RenderTickEvent event) {
        if (ENABLED && event.phase == TickEvent.Phase.END && left > 0) frames++;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || left <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // 고개를 천천히 돌린다 (화면에 들어오는 것이 계속 바뀌게)
        mc.player.setYRot(mc.player.getYRot() + 1.5F);
        if (fly) {
            // 새 땅으로: 한 틱에 0.6 칸 (초당 12 칸 — 겉날개로 나는 정도)
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(0, 0, 0);
            mc.player.setPos(mc.player.getX() + 0.6, flyY, mc.player.getZ());
        }
        if (left % 20 == 0) {
            FPS.add(mc.getFps());
            if (mc.getSingleplayerServer() != null) MSPT.add((double) mc.getSingleplayerServer().getAverageTickTime());
        }
        if (--left == 0) {
            List<Integer> f = new ArrayList<>(FPS);
            Collections.sort(f);
            double sum = 0;
            for (int v : f) sum += v;
            double ms = 0, msMax = 0;
            for (double v : MSPT) {
                ms += v;
                msMax = Math.max(msMax, v);
            }
            long now = System.currentTimeMillis();
            OUT.add(String.format("PHASE %s %s %s fps avg %.1f min %d p10 %d · frames %d = %.2f fps · server mspt avg %.1f max %.1f · entities %d · %s", phase,
                    com.rtshop.test.PerfTest.clockOf(phaseStart), com.rtshop.test.PerfTest.clockOf(now),
                    f.isEmpty() ? 0 : sum / f.size(), f.isEmpty() ? 0 : f.get(0), f.isEmpty() ? 0 : f.get(Math.min(f.size() - 1, f.size() / 10)),
                    frames, frames * 1000.0 / Math.max(1, now - phaseStart),
                    MSPT.isEmpty() ? 0 : ms / MSPT.size(), msMax, mc.level == null ? 0 : mc.level.getEntityCount(), mc.levelRenderer.getChunkStatistics()));
            RtShop.LOGGER.info("[rtshop-perf] {}", OUT.get(OUT.size() - 1));
            fly = false;
        }
    }
}
