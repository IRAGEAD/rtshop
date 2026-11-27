package com.rtshop.stats;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /rtshop perfcheck [초] (3.73.0, OP · 서버 콘솔): 서버 스레드를 그 시간(기본 30초) 동안 찍어 모드별로 틱 시간을 어디에 쓰는지 적는다.
 * 결과: config/rtshop/export/perfcheck-<시각>.txt 와 명령을 친 곳에 요약.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class PerfCheckServer {
    private PerfCheckServer() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("perfcheck").requires(s -> s.hasPermission(2))
                .executes(ctx -> start(ctx.getSource(), 30))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 300)).executes(ctx -> start(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds"))))));
    }

    /** 점검용: 마지막 결과. */
    public static volatile List<String> lastReportForTest = List.of();
    public static volatile Path lastFileForTest;

    static int start(CommandSourceStack src, int seconds) {
        MinecraftServer server = src.getServer();
        Thread t = server.getRunningThread();
        PerfCheck.Run r = PerfCheck.start("server", t, seconds, () -> server.getAverageTickTime(), run -> server.execute(() -> finish(src, server, run)));
        if (r == null) {
            src.sendFailure(Component.literal("[RT Shop] perfcheck: 이미 진단 중입니다 / already running"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("[RT Shop] 서버 진단 " + seconds + "초 시작 (평소처럼 플레이하세요) / server check started").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    static void finish(CommandSourceStack src, MinecraftServer server, PerfCheck.Run run) {
        List<String> head = new ArrayList<>();
        try {
            head.add("RT Shop server check · " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " · mods " + net.minecraftforge.fml.ModList.get().size()
                    + " · players " + server.getPlayerCount());
            Runtime rt = Runtime.getRuntime();
            head.add(String.format("CPU 코어 %d · Java %s · 메모리 %d / %d MB · 시야 거리 %d · 시뮬레이션 거리 %d", rt.availableProcessors(), System.getProperty("java.version"),
                    (rt.totalMemory() - rt.freeMemory()) / 1048576, rt.maxMemory() / 1048576, server.getPlayerList().getViewDistance(), server.getPlayerList().getSimulationDistance()));
            for (ServerLevel l : server.getAllLevels()) {
                int ents = 0;
                for (var ignored : l.getAllEntities()) ents++;
                if (ents == 0 && l.getChunkSource().getLoadedChunksCount() == 0) continue;
                head.add(String.format("  %s: 엔티티 %d · 불러온 청크 %d", l.dimension().location(), ents, l.getChunkSource().getLoadedChunksCount()));
            }
        } catch (Throwable t) {
            head.add("(환경 정보 일부를 못 읽음: " + t + ")");
        }
        List<String> rep = PerfCheck.report(run, head, "틱 시간(ms, 50 아래면 20 TPS)");
        lastReportForTest = rep;
        Path dir = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("export");
        Path f = dir.resolve("perfcheck-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
        try {
            Files.createDirectories(dir);
            Files.write(f, rep, StandardCharsets.UTF_8);
            lastFileForTest = f;
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] perfcheck 결과를 쓰지 못했습니다: {}", e.toString());
        }
        double avg = 0, max = 0;
        for (double v : run.perSecond) {
            avg += v;
            max = Math.max(max, v);
        }
        avg = run.perSecond.isEmpty() ? 0 : avg / run.perSecond.size();
        StringBuilder top = new StringBuilder();
        int n = 0;
        for (Map.Entry<String, Integer> e : run.top(8)) {
            if (e.getKey().equals("minecraft")) continue;
            if (n++ >= 5) break;
            top.append(n == 1 ? "" : ", ").append(e.getKey()).append(String.format(" %.0f%%", run.pct(e.getValue())));
        }
        String summary = String.format("[RT Shop] 서버 진단: 틱 평균 %.1f ms (가장 김 %.1f ms) · 시간을 많이 쓴 모드: %s · 파일 config/rtshop/export/%s", avg, max,
                top.length() == 0 ? "-" : top.toString(), f.getFileName());
        src.sendSuccess(() -> Component.literal(summary).withStyle(ChatFormatting.GOLD), false);
    }
}
