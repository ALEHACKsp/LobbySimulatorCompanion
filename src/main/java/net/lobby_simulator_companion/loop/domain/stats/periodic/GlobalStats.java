package net.lobby_simulator_companion.loop.domain.stats.periodic;

import java.time.LocalDateTime;

/**
 * @author NickyRamone
 */
public class GlobalStats extends PeriodStats {

    public GlobalStats(LocalDateTime now) {
        super(now);
    }

    @Override
    protected LocalDateTime getPeriodStart(LocalDateTime now) {
        return now;
    }

    @Override
    protected LocalDateTime getPeriodEnd(LocalDateTime now) {
        return null;
    }

}
