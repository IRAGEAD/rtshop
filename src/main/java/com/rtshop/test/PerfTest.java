package com.rtshop.test;

import com.rtshop.RtShop;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 성능 측정 (3.70.0, 사용자 요청 "모드팩의 성능을 최적화하는 것을 최우선"). JVM 옵션 -Drtshop.perftest=true 일 때만 돈다 (CI 의 perf job).
 * 실제 지형의 새 세계에서 서버가 틱을 도는 동안 단계별로 틱 시간(MSPT)을 재고, 단계의 시작 · 끝 시각을 적어 둔다
 * (같은 시간에 JFR 이 기록한 CPU 표본을 그 단계별로 나눠 모드별 몫을 낸다: .github/scripts/perf_report.py).
 * <ol>
 * <li>idle: 시작 지점에 RT Shop NPC 24 명을 세워 두고 가만히 (스폰 청크만 돈다)</li>
 * <li>worldgen: 멀리 떨어진 14×14 청크를 끝까지 만든다 (지형 생성 모드들의 무게)</li>
 * <li>mobs: 몹 150 마리를 시작 지점에 풀어 두고 (AI · Epic Fight 같은 몹마다 붙는 기능의 무게)</li>
 * </ol>
 * 결과: rtshop-perf-server.txt (서버 폴더). 끝나면 서버를 끈다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class PerfTest {
    private PerfTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("rtshop.perftest");
    }

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final List<String> OUT = new ArrayList<>();
    private static final List<Double> MS = new ArrayList<>();
    private static String phase = "";
    private static long phaseStart;
    private static int left = -1;
    private static int stage = 0;
    private static long tickStart;
    private static boolean started;

    static String clock(long millis) {
        return CLOCK.format(Instant.ofEpochMilli(millis));
    }

    /** 단계 시각 (JFR 의 표본 시각과 같은 꼴: 시:분:초.밀리초, 이 컴퓨터의 시간대). */
    public static String clockOf(long millis) {
        return clock(millis);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!enabled() || Boolean.getBoolean("rtshop.perftest.serve")) return;   // serve: 측정 없이 켜 두기만 (DH 접속 시험용)
        started = true;
        Runtime rt = Runtime.getRuntime();
        OUT.add("RT Shop performance (server)");
        OUT.add("started " + clock(System.currentTimeMillis()) + " · mods " + net.minecraftforge.fml.ModList.get().size()
                + " · heap after start " + (rt.totalMemory() - rt.freeMemory()) / 1048576 + " MB of " + rt.maxMemory() / 1048576 + " MB");
        begin("warmup", 300);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (!enabled() || !started) return;
        if (event.phase == TickEvent.Phase.START) {
            tickStart = System.nanoTime();
            return;
        }
        MS.add((System.nanoTime() - tickStart) / 1e6);
        if (left > 0 && --left == 0) {
            next(event.getServer());
        }
    }

    private static void begin(String name, int ticks) {
        phase = name;
        phaseStart = System.currentTimeMillis();
        MS.clear();
        left = ticks;
    }

    /** 단계 하나를 닫고 결과 한 줄을 적는다. */
    private static void close(String extra) {
        long end = System.currentTimeMillis();
        List<Double> s = new ArrayList<>(MS);
        Collections.sort(s);
        double sum = 0;
        for (double d : s) sum += d;
        double avg = s.isEmpty() ? 0 : sum / s.size();
        double p95 = s.isEmpty() ? 0 : s.get(Math.min(s.size() - 1, (int) (s.size() * 0.95)));
        double max = s.isEmpty() ? 0 : s.get(s.size() - 1);
        OUT.add(String.format("PHASE %s %s %s ticks %d mspt avg %.2f p95 %.2f max %.1f%s", phase, clock(phaseStart), clock(end), s.size(), avg, p95, max,
                extra.isEmpty() ? "" : " · " + extra));
        RtShop.LOGGER.info("[rtshop-perf] {}", OUT.get(OUT.size() - 1));
    }

    private static void next(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        try {
            switch (stage++) {
                case 0 -> {
                    close("");
                    level.setDayTime(18000);
                    server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    int n = 0;
                    String[] kinds = {"blacksmith", "farmer", "fisher", "food", "general", "land", "raid", "gacha_shop", "attendance_shop", "bank"};
                    for (int i = 0; i < 24; i++) {
                        double a = i * Math.PI * 2 / 24, r = 6 + (i % 3) * 3;
                        int x = spawn.getX() + (int) Math.round(Math.cos(a) * r), z = spawn.getZ() + (int) Math.round(Math.sin(a) * r);
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        if (com.rtshop.shop.ShopServer.spawnNpc(level, new Vec3(x + 0.5, y, z + 0.5), (float) Math.toDegrees(a), kinds[i % kinds.length]) != null) n++;
                    }
                    OUT.add("town: " + n + " RT Shop NPCs around the spawn point");
                    begin("idle", 600);
                }
                case 1 -> {
                    close("entities " + count(level) + " · loaded chunks " + level.getChunkSource().getLoadedChunksCount());
                    // 지형 생성: 시작 지점에서 멀리 떨어진 14×14 청크를 끝까지 (이 시간 동안 서버 틱은 멈춘다 — 생성 속도를 잰다)
                    phase = "worldgen";
                    phaseStart = System.currentTimeMillis();
                    MS.clear();
                    int cx0 = (spawn.getX() >> 4) + 48, cz0 = (spawn.getZ() >> 4) - 7, made = 0;
                    long t0 = System.nanoTime();
                    for (int i = 0; i < 14; i++) {
                        for (int j = 0; j < 14; j++) {
                            level.getChunk(cx0 + i, cz0 + j);
                            made++;
                        }
                    }
                    double sec = (System.nanoTime() - t0) / 1e9;
                    close(String.format("%d chunks in %.1f s = %.2f chunks/s", made, sec, made / Math.max(0.001, sec)));
                    begin("after-worldgen", 200);
                }
                case 2 -> {
                    close("");
                    // 몹: 시작 지점(스폰 청크 = 늘 도는 곳)에 150 마리
                    Object[][] mobs = {{EntityType.ZOMBIE, 35}, {EntityType.SKELETON, 25}, {EntityType.SPIDER, 15}, {EntityType.CREEPER, 10},
                            {EntityType.COW, 20}, {EntityType.SHEEP, 20}, {EntityType.PIG, 10}, {EntityType.VILLAGER, 15}};
                    int k = 0, ok = 0;
                    for (Object[] m : mobs) {
                        EntityType<?> type = (EntityType<?>) m[0];
                        for (int i = 0; i < (Integer) m[1]; i++, k++) {
                            double a = k * 2.39996, r = 8 + (k % 17) * 2.2;
                            int x = spawn.getX() + (int) Math.round(Math.cos(a) * r), z = spawn.getZ() + (int) Math.round(Math.sin(a) * r);
                            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                            Entity e = type.create(level);
                            if (e == null) continue;
                            e.moveTo(x + 0.5, y, z + 0.5, (float) Math.toDegrees(a), 0F);
                            if (e instanceof Mob mob) {
                                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.COMMAND, null, null);
                                mob.setPersistenceRequired();
                            }
                            if (level.addFreshEntity(e)) ok++;
                        }
                    }
                    OUT.add("mobs: " + ok + " spawned around the spawn point");
                    begin("mobs", 600);
                }
                default -> {
                    close("entities " + count(level) + " · loaded chunks " + level.getChunkSource().getLoadedChunksCount());
                    System.gc();
                    Runtime rt = Runtime.getRuntime();
                    OUT.add("heap after GC " + (rt.totalMemory() - rt.freeMemory()) / 1048576 + " MB of " + rt.maxMemory() / 1048576 + " MB");
                    OUT.add("");
                    OUT.add("RESULT: DONE");
                    write(server);
                    left = -1;
                    server.halt(false);
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop-perf] stage failed", t);
            OUT.add("ERROR in " + phase + ": " + t);
            OUT.add("RESULT: DONE");
            write(server);
            left = -1;
            server.halt(false);
        }
    }

    private static int count(ServerLevel level) {
        int n = 0;
        for (Entity ignored : level.getAllEntities()) n++;
        return n;
    }

    private static void write(MinecraftServer server) {
        try {
            Files.write(server.getServerDirectory().toPath().resolve("rtshop-perf-server.txt"), OUT, StandardCharsets.UTF_8);
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop-perf] could not write the result", e);
        }
    }
}
