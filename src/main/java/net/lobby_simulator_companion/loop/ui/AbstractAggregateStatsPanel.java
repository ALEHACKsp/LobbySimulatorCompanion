package net.lobby_simulator_companion.loop.ui;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.RealmMap;
import net.lobby_simulator_companion.loop.domain.stats.AggregateStats;
import net.lobby_simulator_companion.loop.service.GameEvent;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.StatsUtils;
import net.lobby_simulator_companion.loop.ui.common.ComponentUtils;
import net.lobby_simulator_companion.loop.ui.common.NameValueInfoPanel;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.ui.common.UiConstants;
import net.lobby_simulator_companion.loop.util.TimeUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import static net.lobby_simulator_companion.loop.ui.common.UiConstants.*;

/**
 * @param <G> Enum describing the aggregate group.
 * @param <A> Aggregate stats
 * @author NickyRamone
 */
@Slf4j
public abstract class AbstractAggregateStatsPanel<G extends Enum<G>, A extends AggregateStats> extends JPanel {

    private enum StatType {
        MATCHES_PLAYED("Matches played"),
        TIME_PLAYED("Total play time", "time since the match started until you died or escaped"),
        TIME_QUEUED("Total queue time", "time waiting for a lobby"),
        TIME_WAITED("Total wait time", "time since you started searching for match until match started"),
        AVG_MATCH_DURATION("Average match duration"),
        AVG_QUEUE_TIME("Average queue time"),
        AVG_MATCH_WAIT_TIME("Average match wait time"),
        ESCAPES("Escapes"),
        ESCAPES_IN_A_ROW("Escapes in a row - streak"),
        MAX_ESCAPES_IN_A_ROW("Escapes in a row - record"),
        DEATHS("Deaths"),
        DEATHS_IN_A_ROW("Deaths in a row - streak"),
        MAX_DEATHS_IN_A_ROW("Deaths in a row - record"),
        SURVIVAL_PROBABILITY("Survival rate"),
        MAP_RANDOMNESS("Map variation"),
        KILLER_VARIABILITY("Killer variation");

        @Getter
        final String description;

        @Getter
        final String tooltip;


        StatType(String description) {
            this(description, null);
        }

        StatType(String description, String tooltip) {
            this.description = description;
            this.tooltip = tooltip;
        }

        @Override
        public String toString() {
            return description;
        }
    }


    private final Settings settings;
    private final G[] statGroups;

    private NameValueInfoPanel statsContainer;
    private JLabel periodLabel;
    private JLabel statsPeriodTitle;
    private G currentStatGroup;


    AbstractAggregateStatsPanel(Settings settings, GameStateManager gameStateManager, Class<G> groupEnumClass, String settingsKey) {
        this.settings = settings;

        statGroups = groupEnumClass.getEnumConstants();
        currentStatGroup = settings.get(settingsKey, groupEnumClass, statGroups[0]);

        draw(settingsKey);
        gameStateManager.registerListener(GameEvent.UPDATED_STATS, e -> refreshStatsOnScreen());
    }


    private void draw(String settingsKey) {
        periodLabel = new JLabel();
        periodLabel.setBorder(ComponentUtils.DEFAULT_BORDER);
        periodLabel.setForeground(Color.MAGENTA);
        periodLabel.setFont(ResourceFactory.getRobotoFont());

        JPanel periodLabelContainer = new JPanel();
        periodLabelContainer.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        periodLabelContainer.add(periodLabel);


        JPanel periodContainer = new JPanel();
        periodContainer.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        periodContainer.add(ComponentUtils.createButtonLabel(null, null, ResourceFactory.Icon.LEFT, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentStatGroup = getPreviousStatPeriod();
                settings.set(settingsKey, currentStatGroup);
                refreshStatsOnScreen();
            }
        }));
        periodContainer.add(periodLabelContainer);
        periodContainer.add(ComponentUtils.createButtonLabel(null, null, ResourceFactory.Icon.RIGHT, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentStatGroup = getNextStatPeriod();
                settings.set(settingsKey, currentStatGroup);
                refreshStatsOnScreen();
            }
        }));

        statsPeriodTitle = new JLabel();
        statsPeriodTitle.setForeground(Color.MAGENTA);
        statsPeriodTitle.setFont(ResourceFactory.getRobotoFont());

        JLabel copyToClipboardButton = ComponentUtils.createButtonLabel(
                null,
                "copy stats to clipboard",
                ResourceFactory.Icon.COPY_TO_CLIPBOARD,
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        copyStatsToClipboard();
                    }
                });

        JPanel freqTitleContainer = new JPanel();
        freqTitleContainer.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        freqTitleContainer.add(statsPeriodTitle);
        freqTitleContainer.add(copyToClipboardButton);

        NameValueInfoPanel.Builder builder = new NameValueInfoPanel.Builder();
        builder.setSizes(WIDTH__INFO_PANEL__NAME_COLUMN, WIDTH__INFO_PANEL__VALUE_COLUMN, 410);
        for (StatType statType : StatType.values()) {
            builder.addField(statType, statType + ":", statType.tooltip);
        }

        statsContainer = builder.build();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        add(periodContainer);
        add(freqTitleContainer);
        add(statsContainer);
    }

    private G getPreviousStatPeriod() {
        int idx = (currentStatGroup.ordinal() == 0 ? statGroups.length : currentStatGroup.ordinal()) - 1;
        return statGroups[idx];
    }

    private G getNextStatPeriod() {
        int idx = (currentStatGroup.ordinal() + 1) % statGroups.length;
        return statGroups[idx];
    }


    protected void refreshStatsOnScreen() {
        A stats = getStatsForGroup(currentStatGroup);
        periodLabel.setText(currentStatGroup.toString());
        String subtitle = getStatsGroupSubTitle(currentStatGroup, stats);
        statsPeriodTitle.setText(subtitle);

        setStatValue(StatType.MATCHES_PLAYED, String.valueOf(stats.getMatchesPlayed()));
        setStatValue(StatType.TIME_PLAYED, TimeUtil.formatTimeUpToYears(stats.getSecondsPlayed()));
        setStatValue(StatType.TIME_QUEUED, TimeUtil.formatTimeUpToYears(stats.getSecondsQueued()));
        setStatValue(StatType.TIME_WAITED, TimeUtil.formatTimeUpToYears(stats.getSecondsWaited()));
        setStatValue(StatType.AVG_MATCH_DURATION, TimeUtil.formatTimeUpToYears(stats.getAverageSecondsPerMatch()));
        setStatValue(StatType.AVG_QUEUE_TIME, TimeUtil.formatTimeUpToYears(stats.getAverageSecondsInQueue()));
        setStatValue(StatType.AVG_MATCH_WAIT_TIME, TimeUtil.formatTimeUpToYears(stats.getAverageSecondsWaitedPerMatch()));
        setStatValue(StatType.ESCAPES, String.valueOf(stats.getEscapes()));
        setStatValue(StatType.ESCAPES_IN_A_ROW, String.valueOf(stats.getEscapesInARow()));
        setStatValue(StatType.MAX_ESCAPES_IN_A_ROW, String.valueOf(stats.getMaxEscapesInARow()));
        setStatValue(StatType.DEATHS, String.valueOf(stats.getDeaths()));
        setStatValue(StatType.DEATHS_IN_A_ROW, String.valueOf(stats.getDeathsInARow()));
        setStatValue(StatType.MAX_DEATHS_IN_A_ROW, String.valueOf(stats.getMaxDeathsInARow()));

        setStatValue(StatType.SURVIVAL_PROBABILITY, stats.getMatchesSubmitted() == 0 ?
                "N/A" :
                String.format("%.1f %%", stats.getSurvivalProbability()));

        float mapVariability = calculateMapsDistro(stats);
        setStatValue(StatType.MAP_RANDOMNESS, stats.getMatchesPlayed() == 0 ?
                "N/A" :
                String.format("%.1f %% (%s)", mapVariability * 100, getVariabilityLabel(mapVariability)));

        float killerVariability = calculateKillersDistro(stats);
        setStatValue(StatType.KILLER_VARIABILITY, stats.getMatchesPlayed() == 0 ?
                "N/A" :
                String.format("%.1f %% (%s)", killerVariability * 100, getVariabilityLabel(killerVariability)));
    }

    protected abstract A getStatsForGroup(G currentStatGroup);


    protected abstract String getStatsGroupSubTitle(G currentStatGroup, A stats);


    private void setStatValue(StatType statType, String value) {
        statsContainer.get(statType, JLabel.class).setText(value);
    }

    private float calculateMapsDistro(AggregateStats stats) {
        Collection<Integer> mapsDistro = Arrays.stream(RealmMap.values())
                .filter(RealmMap::isIdentified)
                .map(rm -> stats.getMapStats() != null
                        && stats.getMapStats() != null
                        && stats.getMapStats().containsKey(rm) ?
                        stats.getMapStats().get(rm).getMatches()
                        : 0)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        return StatsUtils.rateDistribution(mapsDistro);
    }

    private float calculateKillersDistro(AggregateStats stats) {
        Collection<Integer> distro = Arrays.stream(Killer.values())
                .filter(Killer::isIdentified)
                .map(k -> stats.getKillersStats() != null
                        && stats.getKillersStats() != null
                        && stats.getKillersStats().containsKey(k) ?
                        stats.getKillersStats().get(k).getMatches() :
                        0)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        return StatsUtils.rateDistribution(distro);
    }

    private String getVariabilityLabel(float variability) {
        String label;

        if (variability >= 0 && variability <= 0.25) {
            label = "poor";
        } else if (variability > 0.25 && variability <= 0.6) {
            label = "really bad";
        } else if (variability > 0.6 && variability <= 0.7) {
            label = "quite bad";
        } else if (variability > 0.7 && variability <= 0.8) {
            label = "decent";
        } else if (variability > 0.8 && variability <= 0.9) {
            label = "good";
        } else {
            label = "very good";
        }

        return label;
    }

    private void copyStatsToClipboard() {
        StringBuilder content = new StringBuilder();
        content.append(statsPeriodTitle.getText()).append('\n');
        content.append("-----------------------------------\n");

        statsContainer.entrySet().forEach(e -> {
            content.append(((StatType) e.getKey()).description).append(": ");
            content.append(((JLabel) e.getValue()).getText()).append('\n');
        });

        StringSelection stringSelection = new StringSelection(content.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

}
