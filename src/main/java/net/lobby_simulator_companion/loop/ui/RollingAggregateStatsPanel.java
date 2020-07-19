package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.stats.AggregateStats;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.LoopDataService;

import static net.lobby_simulator_companion.loop.domain.MatchLog.RollingGroup;

/**
 * @author NickyRamone
 */
public class RollingAggregateStatsPanel extends AbstractAggregateStatsPanel<RollingGroup, AggregateStats> {

    private final LoopDataService dataService;


    public RollingAggregateStatsPanel(Settings settings, LoopDataService dataService, GameStateManager gameStateManager) {
        super(settings, gameStateManager, RollingGroup.class, "ui.panel.stats.rollingGroup");
        this.dataService = dataService;
        refreshStatsOnScreen();
    }

    @Override
    protected AggregateStats getStatsForGroup(RollingGroup rollingGroup) {
        return dataService.getMatchHistory().getStats(rollingGroup);
    }

    @Override
    protected String getStatsGroupSubTitle(RollingGroup currentStatGroup, AggregateStats stats) {
        return null;
    }
}
