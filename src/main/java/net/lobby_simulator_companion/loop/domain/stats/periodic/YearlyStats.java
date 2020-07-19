package net.lobby_simulator_companion.loop.domain.stats.periodic;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author NickyRamone
 */
public class YearlyStats extends PeriodStats {

    public YearlyStats(LocalDateTime now) {
        super(now);
    }

    @Override
    protected LocalDateTime getPeriodStart(LocalDateTime now) {
        return now.toLocalDate().atStartOfDay().withDayOfYear(1);
    }

    @Override
    protected LocalDateTime getPeriodEnd(LocalDateTime now) {
        return now.toLocalDate().withDayOfYear(now.toLocalDate().lengthOfYear()).atTime(LocalTime.MAX);
    }

}
