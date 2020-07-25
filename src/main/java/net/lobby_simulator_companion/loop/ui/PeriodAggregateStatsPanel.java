package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.stats.Stats;
import net.lobby_simulator_companion.loop.domain.stats.periodic.PeriodStats;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.LoopDataService;

import java.time.format.DateTimeFormatter;

import static net.lobby_simulator_companion.loop.domain.stats.Stats.Period;

/**
 * @author NickyRamone
 */
public class PeriodAggregateStatsPanel extends AbstractAggregateStatsPanel<Period, PeriodStats> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final LoopDataService dataService;


    public PeriodAggregateStatsPanel(Settings settings, LoopDataService dataService, GameStateManager gameStateManager) {
        super(settings, gameStateManager, Stats.Period.class, "ui.panel.stats.period");
        this.dataService = dataService;
        refreshStatsOnScreen();
    }


    @Override
    protected PeriodStats getStatsForGroup(Period period) {
        return dataService.getStats().get(period);
    }


    @Override
    protected String getStatsGroupSubTitle(Period period, PeriodStats stats) {
        String subtitle = null;

        switch (period) {
            case DAILY:
                subtitle = DATE_FORMATTER.format(stats.getPeriodStart());
                break;
            case WEEKLY:
                subtitle = String.format("%s - %s",
                        DATE_FORMATTER.format(stats.getPeriodStart()),
                        DATE_FORMATTER.format(stats.getPeriodEnd()));
                break;
            case MONTHLY:
                subtitle = String.format("%s - %s",
                        DATE_FORMATTER.format(stats.getPeriodStart()),
                        DATE_FORMATTER.format(stats.getPeriodEnd()));
                break;
            case YEARLY:
                subtitle = String.format("%s - %s",
                        DATE_FORMATTER.format(stats.getPeriodStart()),
                        DATE_FORMATTER.format(stats.getPeriodEnd()));
                break;
            case GLOBAL:
                subtitle = String.format("Since %s",
                        DATE_FORMATTER.format(stats.getPeriodStart()));
                break;
        }

        return subtitle;
    }

}
