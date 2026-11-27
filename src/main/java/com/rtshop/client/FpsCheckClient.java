package com.rtshop.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rtshop.RtShop;
import com.rtshop.stats.PerfCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /fpscheck [초] (3.73.0): FPS 가 떨어지는 곳에서 치면 화면 스레드를 그 시간(기본 30초) 동안 찍어 모드별로 어디에 시간이 드는지,
 * 그래픽 카드를 기다리는지(= GPU 병목)를 적은 파일을 게임 폴더에 남긴다. 사용자가 그 파일을 보내 주면 원인을 찾는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class FpsCheckClient {
    private FpsCheckClient() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fpscheck")
                .executes(ctx -> start(30))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 300)).executes(ctx -> start(IntegerArgumentType.getInteger(ctx, "seconds")))));
    }

    /** 점검용: 마지막 결과 파일. */
    public static volatile Path lastFileForTest;
    public static volatile List<String> lastReportForTest = List.of();

    static int start(int seconds) {
        Minecraft mc = Minecraft.getInstance();
        Thread render = Thread.currentThread();   // 클라이언트 명령은 화면 스레드에서 돈다
        PerfCheck.Run r = PerfCheck.start("client", render, seconds, () -> mc.getFps(), run -> mc.execute(() -> finish(mc, run)));
        if (r == null) {
            say(mc, Component.translatable("rtshop.fpscheck.busy").withStyle(ChatFormatting.RED));
            return 0;
        }
        say(mc, Component.translatable("rtshop.fpscheck.start", seconds).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static void say(Minecraft mc, Component c) {
        if (mc.player != null) mc.player.displayClientMessage(c, false);
    }

    /** 결과 파일을 적고 채팅에 요약 (화면 스레드에서). */
    static void finish(Minecraft mc, PerfCheck.Run run) {
        List<String> head = new ArrayList<>();
        try {
            head.add("RT Shop FPS check · " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " · RT Shop "
                    + net.minecraftforge.fml.ModList.get().getModContainerById(RtShop.MODID).map(c -> c.getModInfo().getVersion().toString()).orElse("?")
                    + " · mods " + net.minecraftforge.fml.ModList.get().size());
            head.add("GPU: " + com.mojang.blaze3d.platform.GlUtil.getRenderer() + " · " + com.mojang.blaze3d.platform.GlUtil.getVendor() + " · OpenGL "
                    + com.mojang.blaze3d.platform.GlUtil.getOpenGLVersion());
            Runtime rt = Runtime.getRuntime();
            head.add(String.format("CPU 코어 %d · Java %s · 메모리 %d / %d MB", rt.availableProcessors(), System.getProperty("java.version"),
                    (rt.totalMemory() - rt.freeMemory()) / 1048576, rt.maxMemory() / 1048576));
            var o = mc.options;
            head.add(String.format("화면 %dx%d · 렌더 거리 %d (실제 %d) · 시뮬레이션 %d · 그래픽 %s · 최대 FPS %d · 수직 동기화 %s · 구름 %s · 입자 %s · 엔티티 그림자 %s · 생물 군계 섞기 %d",
                    mc.getWindow().getWidth(), mc.getWindow().getHeight(), o.renderDistance().get(), o.getEffectiveRenderDistance(), o.simulationDistance().get(),
                    o.graphicsMode().get(), o.framerateLimit().get(), o.enableVsync().get(), o.cloudStatus().get(), o.particles().get(), o.entityShadows().get(),
                    o.biomeBlendRadius().get()));
            head.add("셰이더: " + (ShaderState.on() ? "켜짐 (" + shaderPack(mc) + ")" : "꺼짐")
                    + " · Distant Horizons " + (net.minecraftforge.fml.ModList.get().isLoaded("distanthorizons") ? "있음" : "없음")
                    + " · Farsight " + (net.minecraftforge.fml.ModList.get().isLoaded("farsight_view") ? "있음" : "없음")
                    + " · Arcane Vortex " + (net.minecraftforge.fml.ModList.get().isLoaded("arcanevortex") ? "있음" : "없음"));
            if (mc.level != null && mc.player != null) {
                head.add(String.format("자리 %s %d %d %d · 엔티티 %d · %s · %s", mc.level.dimension().location(), mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(),
                        mc.level.getEntityCount(), mc.levelRenderer.getChunkStatistics(), mc.getSingleplayerServer() != null
                                ? String.format("싱글플레이 (내장 서버 틱 %.1f ms)", mc.getSingleplayerServer().getAverageTickTime()) : "서버 접속"));
            }
        } catch (Throwable t) {
            head.add("(환경 정보 일부를 못 읽음: " + t + ")");
        }
        List<String> rep = PerfCheck.report(run, head, "FPS");
        lastReportForTest = rep;
        Path f = mc.gameDirectory.toPath().resolve("rtshop-fpscheck-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
        try {
            Files.write(f, rep, StandardCharsets.UTF_8);
            lastFileForTest = f;
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] fpscheck 결과를 쓰지 못했습니다: {}", e.toString());
        }
        // 채팅 요약: FPS · 그래픽 카드 기다림 · 시간을 많이 쓴 모드 다섯
        double avg = 0, min = Double.MAX_VALUE;
        for (double v : run.perSecond) {
            avg += v;
            min = Math.min(min, v);
        }
        avg = run.perSecond.isEmpty() ? 0 : avg / run.perSecond.size();
        StringBuilder top = new StringBuilder();
        int n = 0;
        for (Map.Entry<String, Integer> e : run.top(8)) {
            if (e.getKey().equals("minecraft")) continue;
            if (n++ >= 5) break;
            top.append(n == 1 ? "" : ", ").append(shortMod(e.getKey())).append(String.format(" %.0f%%", run.pct(e.getValue())));
        }
        say(mc, Component.translatable("rtshop.fpscheck.done", String.format("%.0f", avg), String.format("%.0f", run.perSecond.isEmpty() ? 0 : min),
                String.format("%.0f", run.pct(run.gpuWait)), top.toString()).withStyle(ChatFormatting.GOLD));
        say(mc, Component.translatable("rtshop.fpscheck.file", f.getFileName().toString()).withStyle(ChatFormatting.GRAY));
    }

    /** jar 이름에서 버전 꼬리를 떼어 짧게 ("ArcaneVortex [Alpha]-0.7.10-1.20.1-Forge.jar" → "ArcaneVortex [Alpha]"). */
    static String shortMod(String jar) {
        String s = jar.endsWith(".jar") ? jar.substring(0, jar.length() - 4) : jar;
        String[] parts = s.split("[-_](?=\\d|mc\\d|forge|Forge|neoforge)", 2);
        return parts[0].isEmpty() ? s : parts[0];
    }

    private static String shaderPack(Minecraft mc) {
        for (String name : new String[]{"oculus.properties", "iris.properties"}) {
            Path p = mc.gameDirectory.toPath().resolve("config").resolve(name);
            try {
                if (Files.exists(p)) {
                    for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        if (line.startsWith("shaderPack=")) return line.substring(11);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "?";
    }
}
