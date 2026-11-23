package com.rtshop.compat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 지형 생성 중 청크마다 쏟아지는 쓸모없는 경고를 걸러 낸다. 생성 스레드들이 로그 파일 쓰기를 기다리느라 느려지는 것을 막는다.
 * <ul>
 * <li>"Empty height range" — 어떤 모드의 지형 장식 높이 설정이 그 차원에 맞지 않아 청크마다 찍힌다 (장식만 빠지고 해는 없다).</li>
 * <li>"Skipping Entity with id" — 건축물에 들어 있지만 설치되지 않은 모드의 엔티티.</li>
 * </ul>
 * 걸러 낸 수는 /rtshop 진단 등에서 볼 수 있다 (SUPPRESSED).
 */
public final class LogNoise extends AbstractFilter {
    private static final List<String> PREFIXES = List.of("Empty height range", "Skipping Entity with id");
    public static final AtomicLong SUPPRESSED = new AtomicLong();
    private static boolean installed;

    private LogNoise() {
        super(Result.DENY, Result.NEUTRAL);
    }

    public static synchronized void install() {
        if (installed) return;
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ctx.getConfiguration().addFilter(new LogNoise());
            ctx.updateLoggers();
            installed = true;
        } catch (Throwable ignored) {
            // 로그 설정을 바꿀 수 없으면 그대로 둔다
        }
    }

    public static boolean noisy(String msg) {
        if (msg == null) return false;
        for (String p : PREFIXES) {
            if (msg.startsWith(p)) {
                SUPPRESSED.incrementAndGet();
                return true;
            }
        }
        return false;
    }

    private Result r(String msg) {
        return noisy(msg) ? onMatch : onMismatch;
    }

    @Override
    public Result filter(LogEvent event) {
        Message m = event.getMessage();
        return m == null ? onMismatch : r(m.getFormat() != null ? m.getFormat() : m.getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return r(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return msg == null ? onMismatch : r(msg.getFormat() != null ? msg.getFormat() : msg.getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return r(msg == null ? null : msg.toString());
    }
}
