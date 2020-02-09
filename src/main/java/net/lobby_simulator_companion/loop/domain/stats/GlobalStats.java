package net.lobby_simulator_companion.loop.domain.stats;

import java.time.LocalDateTime;

/**
 * @author NickyRamone
 */
public class GlobalStats extends PeriodStats {

    public GlobalStats(PeriodStats other) {
        super(other);
    }

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

    @Override
    public GlobalStats clone() {
        return new GlobalStats(this);
    }
}
