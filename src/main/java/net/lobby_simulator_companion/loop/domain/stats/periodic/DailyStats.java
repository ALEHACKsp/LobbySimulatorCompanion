package net.lobby_simulator_companion.loop.domain.stats.periodic;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author NickyRamone
 */
public class DailyStats extends PeriodStats {

    public DailyStats(LocalDateTime now) {
        super(now);
    }

    @Override
    protected LocalDateTime getPeriodStart(LocalDateTime now) {
        return now.toLocalDate().atStartOfDay();
    }

    @Override
    protected LocalDateTime getPeriodEnd(LocalDateTime now) {
        return now.toLocalDate().atTime(LocalTime.MAX);
    }

}
