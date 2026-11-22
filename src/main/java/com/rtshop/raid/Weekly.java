package com.rtshop.raid;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** 주간 초기화 (기본: 매주 월요일 오전 6시, 서버 컴퓨터의 시간대 기준). */
public final class Weekly {
    private Weekly() {
    }

    /** 점검용: 시계를 앞으로 돌린다 (ms). */
    public static long testClockOffset = 0;

    public static long now() {
        return System.currentTimeMillis() + testClockOffset;
    }

    /** 이 시각이 속한 주의 시작 시각 (ms). day: 1 = 월요일 … 7 = 일요일. */
    public static long weekStart(long nowMs, int day, int hour) {
        ZonedDateTime t = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault());
        DayOfWeek d = DayOfWeek.of(Math.max(1, Math.min(7, day)));
        ZonedDateTime s = t.with(TemporalAdjusters.previousOrSame(d)).truncatedTo(ChronoUnit.DAYS).withHour(Math.max(0, Math.min(23, hour)));
        if (s.isAfter(t)) {
            s = s.minusWeeks(1);
        }
        return s.toInstant().toEpochMilli();
    }

    /** 주 번호 (주가 바뀌면 값이 달라진다). */
    public static long id(long nowMs, int day, int hour) {
        return weekStart(nowMs, day, hour) / 3_600_000L;
    }

    /** 다음 초기화까지 남은 시간 (ms). */
    public static long untilReset(long nowMs, int day, int hour) {
        ZonedDateTime s = Instant.ofEpochMilli(weekStart(nowMs, day, hour)).atZone(ZoneId.systemDefault()).plusWeeks(1);
        return Math.max(0, s.toInstant().toEpochMilli() - nowMs);
    }
}
