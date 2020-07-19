package net.lobby_simulator_companion.loop.domain.stats.periodic;

import lombok.Getter;
import net.lobby_simulator_companion.loop.domain.stats.AggregateStats;

import java.time.LocalDateTime;

/***
 * @author NickyRamone
 */
public abstract class PeriodStats extends AggregateStats {

    @Getter
    private LocalDateTime periodStart;

    @Getter
    private LocalDateTime periodEnd;


    public PeriodStats(LocalDateTime now) {
        periodStart = getPeriodStart(now);
        periodEnd = getPeriodEnd(now);
    }

    public void reset() {
        super.reset();
        LocalDateTime now = LocalDateTime.now();
        periodStart = getPeriodStart(now);
        periodEnd = getPeriodEnd(now);
    }

    abstract LocalDateTime getPeriodStart(LocalDateTime now);

    abstract LocalDateTime getPeriodEnd(LocalDateTime now);

}
