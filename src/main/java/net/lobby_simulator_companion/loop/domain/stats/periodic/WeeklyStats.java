package net.lobby_simulator_companion.loop.domain.stats.periodic;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * @author NickyRamone
 */
public class WeeklyStats extends PeriodStats {

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

}
