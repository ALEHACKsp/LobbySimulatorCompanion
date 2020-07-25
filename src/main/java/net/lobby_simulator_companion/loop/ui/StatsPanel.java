package net.lobby_simulator_companion.loop.ui;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.service.GameEvent;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.ui.common.CollapsablePanel;
import net.lobby_simulator_companion.loop.ui.common.ComponentUtils;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.ui.common.UiConstants;
import net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator;

import javax.swing.*;
import java.awt.*;

import static net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator.UiEvent;


/**
 * @author NickyRamone
 */
@Slf4j
public class StatsPanel extends JPanel {

    private static final Font font = ResourceFactory.getRobotoFont();
    private static final String SETTINGS__STATS_TYPE_SELECTED = "ui.panel.stats.selected";


    @RequiredArgsConstructor
    private enum AggregatedStatsType {
        PERIOD_STATS("By period"),
        MATCH_COUNT_ROLLING_STATS("By match-count (rolling)");

        private final String description;


        @Override
        public String toString() {
            return description;
        }
    }

    private final Settings settings;
    private final LoopDataService dataService;
    private final PeriodAggregateStatsPanel periodStatsPanel;
    private final RollingAggregateStatsPanel rollingStatsPanel;

    private AbstractAggregateStatsPanel selectedStatsPanel;


    public StatsPanel(Settings settings, LoopDataService dataService, GameStateManager gameStateManager,
                      UiEventOrchestrator uiEventOrchestrator,
                      PeriodAggregateStatsPanel periodStatsPanel, RollingAggregateStatsPanel rollingStatsPanel) {
        this.settings = settings;
        this.dataService = dataService;
        this.periodStatsPanel = periodStatsPanel;
        this.rollingStatsPanel = rollingStatsPanel;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel collapsablePanel = new CollapsablePanel(
                createTitleBar(),
                createDetailsPanel(),
                settings, "ui.panel.stats.collapsed");
        collapsablePanel.addPropertyChangeListener(evt ->
                uiEventOrchestrator.fireEvent(UiEvent.STRUCTURE_RESIZED));
        add(collapsablePanel);

        gameStateManager.registerListener(GameEvent.UPDATED_STATS,
                evt -> refreshStatsOnScreen());
    }

    private JPanel createTitleBar() {
        JLabel titleLabel = new JLabel("Aggregate stats:");
        titleLabel.setBorder(ComponentUtils.DEFAULT_BORDER);
        titleLabel.setForeground(UiConstants.COLOR__TITLE_BAR__FG);
        titleLabel.setFont(font);

        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(UiConstants.COLOR__TITLE_BAR__BG);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(titleLabel);
        container.add(Box.createHorizontalGlue());

        return container;
    }

    private JPanel createDetailsPanel() {
        AggregatedStatsType storedSelectedStatsType = settings.get(SETTINGS__STATS_TYPE_SELECTED, AggregatedStatsType.class,
                AggregatedStatsType.MATCH_COUNT_ROLLING_STATS);

        JComboBox aggregationSelection = new JComboBox<>(AggregatedStatsType.values());
        aggregationSelection.setMaximumSize(new Dimension(250, 25));
        aggregationSelection.setFont(font);
        aggregationSelection.setSelectedItem(storedSelectedStatsType);
        refreshStatsTypeSelection(storedSelectedStatsType);

        aggregationSelection.addActionListener(e -> {
            AggregatedStatsType selected = AggregatedStatsType.values()[aggregationSelection.getSelectedIndex()];
            refreshStatsTypeSelection(selected);
        });

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        container.add(aggregationSelection);
        container.add(periodStatsPanel);
        container.add(rollingStatsPanel);

        return container;
    }


    private void refreshStatsTypeSelection(AggregatedStatsType statsType) {
        settings.set(SETTINGS__STATS_TYPE_SELECTED, statsType);

        switch (statsType) {
            case PERIOD_STATS:
                rollingStatsPanel.setVisible(false);
                selectedStatsPanel = periodStatsPanel;
                break;
            case MATCH_COUNT_ROLLING_STATS:
                periodStatsPanel.setVisible(false);
                selectedStatsPanel = rollingStatsPanel;
                break;
        }

        refreshStatsOnScreen();
        selectedStatsPanel.setVisible(true);
    }


    public void refreshStatsOnScreen() {
        selectedStatsPanel.refreshStatsOnScreen();
    }

}
