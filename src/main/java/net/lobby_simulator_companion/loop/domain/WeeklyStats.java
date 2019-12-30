package net.lobby_simulator_companion.loop.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * @author NickyRamone
 */
public class WeeklyStats extends PeriodStats {

    public WeeklyStats(PeriodStats other) {
        super(other);
    }

    public WeeklyStats(LocalDateTime now) {
        super(now);
    }

    @Override
    protected LocalDateTime getPeriodStart(LocalDateTime now) {
        return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
    }

    @Override
    protected LocalDateTime getPeriodEnd(LocalDateTime now) {
        return now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toLocalDate().atTime(LocalTime.MAX);
    }

    @Override
    public WeeklyStats clone() {
        return new WeeklyStats(this);
    }


}
