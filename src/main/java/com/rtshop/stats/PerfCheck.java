package com.rtshop.stats;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 성능 진단 (3.73.0, 사용자 요청 W-22 "fps 가 이전보다 더 심하게 떨어지는 문제"): 한 스레드(화면 스레드나 서버 스레드)의 콜 스택을 초당 100번 찍어
 * 어느 모드가 시간을 쓰는지 센다. 사용자 컴퓨터에서 직접 돌려 그 결과 파일을 받으면, CI(그래픽 카드 없음)로는 볼 수 없는 진짜 원인을 볼 수 있다.
 * <ul>
 * <li>책임 모드: 스택 위(가장 안쪽)에서부터 처음 만나는 모드 프레임 — 바닐라 코드를 부른 모드에 시간을 돌린다. 하나도 없으면 "minecraft".</li>
 * <li>포함: 스택 어디든 그 모드가 있으면 (한 표본에 한 번).</li>
 * <li>모드 이름: 클래스가 들어 있는 jar 파일 이름. 바닐라 클래스에 끼워 넣은 mixin 처리기(handler$…$modid$…)는 그 mod id.</li>
 * <li>그래픽 카드 기다림: 스택 위쪽이 버퍼 교환 · glFinish 이면 GPU 가 병목(또는 수직 동기화), FPS 제한으로 쉬는 것은 따로 센다.</li>
 * </ul>
 */
public final class PerfCheck {
    private PerfCheck() {
    }

    private static final Pattern MIXIN = Pattern.compile("^(?:handler|redirect|modify\\w*|wrap\\w*|localvar|constant)\\$[0-9a-z]+\\$([a-z0-9_]+)\\$");
    private static final String[] BASE = {"java.", "jdk.", "sun.", "javax.", "net.minecraft.", "com.mojang.", "net.minecraftforge.", "cpw.", "org.lwjgl.", "it.unimi.",
            "com.google.", "org.joml.", "io.netty.", "org.apache.", "org.spongepowered.", "org.slf4j.", "com.sun.", "oshi.", "org.objectweb."};
    private static final Map<String, String> JAR = new ConcurrentHashMap<>();

    /** 한 번의 진단. */
    public static final class Run {
        public final String what;
        public final Thread target;
        public final long started = System.currentTimeMillis();
        public final long endAt;
        public int samples, gpuWait, fpsSleep;
        public final Map<String, Integer> self = new HashMap<>(), incl = new HashMap<>(), where = new HashMap<>();
        /** 초마다 재는 값 (FPS 또는 서버 MSPT). */
        public final List<Double> perSecond = new ArrayList<>();
        public volatile boolean done;

        public Run(String what, Thread target, int seconds) {
            this.what = what;
            this.target = target;
            this.endAt = started + seconds * 1000L;
        }

        public double pct(int n) {
            return samples == 0 ? 0 : n * 100.0 / samples;
        }

        /** 책임 모드 순 (많은 것부터). */
        public List<Map.Entry<String, Integer>> top(int n) {
            List<Map.Entry<String, Integer>> l = new ArrayList<>(self.entrySet());
            l.sort((a, b) -> b.getValue() - a.getValue());
            return l.subList(0, Math.min(n, l.size()));
        }
    }

    private static volatile Run current;

    public static Run current() {
        return current;
    }

    /**
     * 진단을 시작한다 (이미 돌고 있으면 null). perSecond 는 초마다 한 번 부르는 측정(그 스레드에서 안전한 값만), onDone 은 끝났을 때 (샘플링 스레드에서 부른다).
     */
    public static synchronized Run start(String what, Thread target, int seconds, java.util.function.DoubleSupplier perSecond, Consumer<Run> onDone) {
        if (current != null && !current.done) return null;
        Run r = new Run(what, target, Math.max(5, Math.min(300, seconds)));
        current = r;
        Thread t = new Thread(() -> {
            long nextSecond = System.currentTimeMillis() + 1000;
            try {
                while (System.currentTimeMillis() < r.endAt && target.isAlive()) {
                    sample(r, target.getStackTrace());
                    if (System.currentTimeMillis() >= nextSecond) {
                        nextSecond += 1000;
                        try {
                            if (perSecond != null) r.perSecond.add(perSecond.getAsDouble());
                        } catch (Throwable ignored) {
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException ignored) {
            } finally {
                r.done = true;
                try {
                    if (onDone != null) onDone.accept(r);
                } catch (Throwable ignored) {
                }
            }
        }, "rtshop-perfcheck");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
        return r;
    }

    static void sample(Run r, StackTraceElement[] st) {
        if (st.length == 0) return;
        r.samples++;
        // 위쪽 몇 칸: 그래픽 카드 기다림 · FPS 제한으로 쉬기
        for (int i = 0; i < Math.min(6, st.length); i++) {
            String m = st[i].getMethodName(), c = st[i].getClassName();
            if (m.contains("SwapBuffers") || m.contains("swapBuffers") || m.equals("nglFinish") || m.equals("glFinish") || m.contains("glClientWaitSync")) {
                r.gpuWait++;
                break;
            }
            if (c.contains("RenderSystem") && m.contains("limitDisplayFPS")) {
                r.fpsSleep++;
                break;
            }
        }
        String resp = null;
        Set<String> seen = new HashSet<>();
        for (StackTraceElement e : st) {
            String mod = modOf(e);
            if (mod == null) continue;
            if (resp == null) {
                resp = mod;
                r.where.merge(mod + " · " + shortName(e), 1, Integer::sum);
            }
            seen.add(mod);
        }
        r.self.merge(resp == null ? "minecraft" : resp, 1, Integer::sum);
        for (String m : seen) r.incl.merge(m, 1, Integer::sum);
    }

    private static String shortName(StackTraceElement e) {
        String c = e.getClassName();
        int dot = c.lastIndexOf('.');
        return (dot >= 0 ? c.substring(dot + 1) : c) + "." + e.getMethodName();
    }

    /** 이 프레임을 쓴 모드 (바닐라 · 자바 · 라이브러리면 null). */
    public static String modOf(StackTraceElement e) {
        Matcher m = MIXIN.matcher(e.getMethodName());
        if (m.find()) return "mixin:" + m.group(1);
        String c = e.getClassName();
        for (String b : BASE) if (c.startsWith(b)) return null;
        return JAR.computeIfAbsent(c, PerfCheck::jarOf);
    }

    private static String jarOf(String cls) {
        try {
            Class<?> k = Class.forName(cls, false, PerfCheck.class.getClassLoader());
            if (k.getProtectionDomain() != null && k.getProtectionDomain().getCodeSource() != null) {
                URL u = k.getProtectionDomain().getCodeSource().getLocation();
                if (u != null) {
                    String s = URLDecoder.decode(u.toString(), StandardCharsets.UTF_8);
                    int jar = s.toLowerCase(java.util.Locale.ROOT).indexOf(".jar");
                    if (jar > 0) {
                        String head = s.substring(0, jar + 4);
                        int slash = Math.max(head.lastIndexOf('/'), head.lastIndexOf('\\'));
                        return head.substring(slash + 1);
                    }
                }
            }
            if (k.getModule() != null && k.getModule().getName() != null) return k.getModule().getName();
        } catch (Throwable ignored) {
        }
        // 못 찾으면 패키지 앞 세 마디
        String[] p = cls.split("\\.");
        return p.length >= 3 ? p[0] + "." + p[1] + "." + p[2] : cls;
    }

    /** 결과를 글로 (파일에 적는 것). head = 맨 위에 붙일 환경 줄들. */
    public static List<String> report(Run r, List<String> head, String perSecondName) {
        List<String> out = new ArrayList<>(head);
        out.add(String.format("표본 %d개 (초당 약 100번, %d초)", r.samples, (System.currentTimeMillis() - r.started) / 1000));
        if (!r.perSecond.isEmpty()) {
            double sum = 0, min = Double.MAX_VALUE, max = 0;
            for (double v : r.perSecond) {
                sum += v;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            out.add(String.format("%s: 평균 %.1f · 가장 낮음 %.1f · 가장 높음 %.1f", perSecondName, sum / r.perSecond.size(), min, max));
        }
        out.add(String.format("그래픽 카드 기다림 %.1f%% · FPS 제한으로 쉼 %.1f%%", r.pct(r.gpuWait), r.pct(r.fpsSleep)));
        out.add("");
        out.add("모드별 시간 (그 모드가 직접 쓴 몫 / 스택 어디든 들어 있던 몫):");
        Map<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : r.top(40)) sorted.put(e.getKey(), e.getValue());
        for (Map.Entry<String, Integer> e : sorted.entrySet()) {
            out.add(String.format("  %5.1f%% / %5.1f%%  %s", r.pct(e.getValue()), r.pct(r.incl.getOrDefault(e.getKey(), 0)), e.getKey()));
        }
        out.add("");
        out.add("가장 바쁜 곳 (모드 · 클래스.메서드):");
        List<Map.Entry<String, Integer>> w = new ArrayList<>(r.where.entrySet());
        w.sort((a, b) -> b.getValue() - a.getValue());
        for (int i = 0; i < Math.min(25, w.size()); i++) out.add(String.format("  %5.1f%%  %s", r.pct(w.get(i).getValue()), w.get(i).getKey()));
        return out;
    }
}
