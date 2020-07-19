package net.lobby_simulator_companion.loop.domain.stats.periodic;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author NickyRamone
 */
public class MonthlyStats extends PeriodStats {

    public MonthlyStats(LocalDateTime now) {
        super(now);
    }

    @Override
    protected LocalDateTime getPeriodStart(LocalDateTime now) {
        return now.toLocalDate().atStartOfDay().withDayOfMonth(1);
    }

    @Override
    protected LocalDateTime getPeriodEnd(LocalDateTime now) {
        return now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()).atTime(LocalTime.MAX);
    }

}
