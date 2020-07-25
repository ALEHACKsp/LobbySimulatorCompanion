package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.stats.AggregateStats;
import net.lobby_simulator_companion.loop.domain.stats.Match;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.LoopDataService;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static net.lobby_simulator_companion.loop.domain.MatchLog.RollingGroup;

/**
 * @author NickyRamone
 */
public class RollingAggregateStatsPanel extends AbstractAggregateStatsPanel<RollingGroup, AggregateStats> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final LoopDataService dataService;


    public RollingAggregateStatsPanel(Settings settings, LoopDataService dataService, GameStateManager gameStateManager) {
        super(settings, gameStateManager, RollingGroup.class, "ui.panel.stats.rollingGroup");
        this.dataService = dataService;
        refreshStatsOnScreen();
    }

    @Override
    protected AggregateStats getStatsForGroup(RollingGroup rollingGroup) {
        return dataService.getMatchLog().getStats(rollingGroup);
    }

    @Override
    protected String getStatsGroupSubTitle(RollingGroup currentStatGroup, AggregateStats stats) {
        return Optional.ofNullable(dataService.getMatchLog().getOldestMatchForGroup(currentStatGroup))
                .map(Match::getMatchStartTime)
                .map(date -> "Since " + DATE_FORMATTER.format(date))
                .orElse(null);
    }
}
