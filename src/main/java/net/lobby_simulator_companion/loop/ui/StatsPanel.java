package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.RealmMap;
import net.lobby_simulator_companion.loop.domain.stats.PeriodStats;
import net.lobby_simulator_companion.loop.domain.stats.Stats;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.service.StatsUtils;
import net.lobby_simulator_companion.loop.ui.common.CollapsablePanel;
import net.lobby_simulator_companion.loop.ui.common.Colors;
import net.lobby_simulator_companion.loop.ui.common.ComponentUtils;
import net.lobby_simulator_companion.loop.ui.common.NameValueInfoPanel;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static net.lobby_simulator_companion.loop.domain.stats.Stats.Period;
import static net.lobby_simulator_companion.loop.ui.common.ResourceFactory.Icon;

/**
 * @author NickyRamone
 */
public class StatsPanel extends JPanel {

    public static final String EVENT_STRUCTURE_CHANGED = "structure_changed";

    private static final Font font = ResourceFactory.getRobotoFont();
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

    private enum StatType {
        MATCHES_PLAYED("Matches played"),
        TIME_PLAYED("Total play time", "time since the match started until you died or escaped"),
        TIME_QUEUED("Total queue time", "time waiting for a lobby"),
        TIME_WAITED("Total wait time", "time since you started searching for match until match started"),
        AVG_MATCH_DURATION("Average match duration"),
        AVG_QUEUE_TIME("Average queue time"),
        AVG_MATCH_WAIT_TIME("Average match wait time"),
        MATCHES_SUBMITTED("Matches submitted"),
        ESCAPES("Escapes"),
        ESCAPES_IN_A_ROW("Escapes in a row - streak"),
        MAX_ESCAPES_IN_A_ROW("Escapes in a row - record"),
        DEATHS("Deaths"),
        DEATHS_IN_A_ROW("Deaths in a row - streak"),
        MAX_DEATHS_IN_A_ROW("Deaths in a row - record"),
        SURVIVAL_PROBABILITY("Survival rate"),
        MAP_RANDOMNESS("Map variation"),
        KILLER_VARIABILITY("Killer variation");

        final String description;
        final String tooltip;

        StatType(String description) {
            this(description, null);
        }

        StatType(String description, String tooltip) {
            this.description = description;
            this.tooltip = tooltip;
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Settings settings;
    private final LoopDataService dataService;

    private JLabel periodLabel;
    private JLabel statsPeriodTitle;
    private NameValueInfoPanel<StatType> statsContainer;
    private final Period[] statPeriods = Period.values();
    private Period currentStatPeriod;
    private Stats statsBackup;
    private Killer killer = Killer.UNIDENTIFIED;
    private RealmMap realmMap;


    public StatsPanel(Settings settings, LoopDataService dataService) {
        this.settings = settings;
        this.dataService = dataService;

        backupStats();
        currentStatPeriod = Period.valueOf(settings.get("ui.panel.stats.period", Period.DAILY.name()));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel collapsablePanel = new CollapsablePanel(
                createTitleBar(),
                createDetailsPanel(),
                settings, "ui.panel.stats.collapsed");
        collapsablePanel.addPropertyChangeListener(evt -> firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null));
        add(collapsablePanel);
        initStatResetTimers();
    }

    private void initStatResetTimers() {
        dataService.getStats().asStream().forEach(this::initStatResetTimer);
    }

    private void initStatResetTimer(PeriodStats periodStats) {
        if (periodStats.getPeriodEnd() == null) {
            return;
        }
        Date statsResetDate = Date.from(periodStats.getPeriodEnd().atZone(ZoneId.systemDefault()).toInstant()
                .plusSeconds(5L));

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.debug("Resetting stats timer for {}", periodStats.getClass());
                periodStats.reset();
                dataService.notifyChange();
                timer.cancel();
                SwingUtilities.invokeLater(() -> refreshStats());
                initStatResetTimer(periodStats);
            }
        }, statsResetDate);
    }


    private JPanel createTitleBar() {
        JLabel titleLabel = new JLabel("Stats:");
        titleLabel.setBorder(ComponentUtils.DEFAULT_BORDER);
        titleLabel.setForeground(Colors.STATUS_BAR_FOREGROUND);
        titleLabel.setFont(font);

        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(Colors.STATUS_BAR_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(titleLabel);
        container.add(Box.createHorizontalGlue());

        return container;
    }

    private JPanel createDetailsPanel() {
        periodLabel = new JLabel();
        periodLabel.setBorder(ComponentUtils.DEFAULT_BORDER);
        periodLabel.setForeground(Color.MAGENTA);
        periodLabel.setFont(font);

        JPanel periodLabelContainer = new JPanel();
        periodLabelContainer.setBackground(Colors.INFO_PANEL_BACKGROUND);
        periodLabelContainer.setPreferredSize(new Dimension(80, 38));
        periodLabelContainer.add(periodLabel);


        JPanel periodContainer = new JPanel();
        periodContainer.setBackground(Colors.INFO_PANEL_BACKGROUND);
        periodContainer.add(ComponentUtils.createButtonLabel(null, "previous period", Icon.LEFT, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentStatPeriod = getPreviousStatPeriod();
                settings.set("ui.panel.stats.period", currentStatPeriod);
                refreshStats();
            }
        }));
        periodContainer.add(periodLabelContainer);
        periodContainer.add(ComponentUtils.createButtonLabel(null, "next period", Icon.RIGHT, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentStatPeriod = getNextStatPeriod();
                settings.set("ui.panel.stats.period", currentStatPeriod);
                refreshStats();
            }
        }));

        statsPeriodTitle = new JLabel();
        statsPeriodTitle.setForeground(Color.MAGENTA);
        statsPeriodTitle.setFont(font);

        JLabel copyToClipboardButton = ComponentUtils.createButtonLabel(
                null,
                "copy stats to clipboard",
                Icon.COPY_TO_CLIPBOARD,
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        copyStatsToClipboard();
                    }
                });

        JPanel freqTitleContainer = new JPanel();
        freqTitleContainer.setBackground(Colors.INFO_PANEL_BACKGROUND);
        freqTitleContainer.add(statsPeriodTitle);
        freqTitleContainer.add(copyToClipboardButton);

        NameValueInfoPanel.Builder<StatType> builder = new NameValueInfoPanel.Builder<>();
        for (StatType statType : StatType.values()) {
            builder.addField(statType, statType.description + ":", statType.tooltip);
        }

        statsContainer = builder.build();

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(Colors.INFO_PANEL_BACKGROUND);
        container.add(periodContainer);
        container.add(freqTitleContainer);
        container.add(statsContainer);

        refreshStats();

        return container;
    }


    private Period getNextStatPeriod() {
        int idx = (currentStatPeriod.ordinal() + 1) % statPeriods.length;
        return statPeriods[idx];
    }

    private Period getPreviousStatPeriod() {
        int idx = (currentStatPeriod.ordinal() == 0 ? statPeriods.length : currentStatPeriod.ordinal()) - 1;
        return statPeriods[idx];
    }


    public void backupStats() {
        statsBackup = dataService.getStats().clone();
    }

    public void restoreStats() {
        if (statsBackup != null) {
            dataService.getStats().copyFrom(statsBackup);
            dataService.notifyChange();
            refreshStats();
        }
    }

    public void notifyMatchSurvival(Boolean escaped) {
        if (escaped == null) {
            return;
        }
        if (escaped) {
            dataService.getStats().incrementEscapes(killer, realmMap);
        } else {
            dataService.getStats().incrementDeaths(killer, realmMap);
        }
        dataService.notifyChange();
        refreshStats();
    }

    public void refreshStats() {
        Stats stats = dataService.getStats();
        PeriodStats currentStats = stats.get(currentStatPeriod);
        periodLabel.setText(currentStatPeriod.description());
        String subtitle = null;

        switch (currentStatPeriod) {
            case DAILY:
                subtitle = DATE_FORMATTER.format(currentStats.getPeriodStart());
                break;
            case WEEKLY:
                subtitle = String.format("%s - %s",
                        DATE_FORMATTER.format(currentStats.getPeriodStart()),
                        DATE_FORMATTER.format(currentStats.getPeriodEnd()));
                break;
            case MONTHLY:
                subtitle = String.format("%s - %s",
                        DATE_FORMATTER.format(currentStats.getPeriodStart()),
                        DATE_FORMATTER.format(currentStats.getPeriodEnd()));
                break;
            case YEARLY:
                subtitle = String.format("%s - %s",
                        DATE_FORMATTER.format(currentStats.getPeriodStart()),
                        DATE_FORMATTER.format(currentStats.getPeriodEnd()));
                break;
            case GLOBAL:
                subtitle = String.format("Since %s",
                        DATE_FORMATTER.format(currentStats.getPeriodStart()));
                break;
        }

        statsPeriodTitle.setText(subtitle);
        setStatValue(StatType.MATCHES_PLAYED, String.valueOf(currentStats.getMatchesPlayed()));
        setStatValue(StatType.TIME_PLAYED, TimeUtil.formatTimeUpToYears(currentStats.getSecondsPlayed()));
        setStatValue(StatType.TIME_QUEUED, TimeUtil.formatTimeUpToYears(currentStats.getSecondsQueued()));
        setStatValue(StatType.TIME_WAITED, TimeUtil.formatTimeUpToYears(currentStats.getSecondsWaited()));
        setStatValue(StatType.AVG_MATCH_DURATION, TimeUtil.formatTimeUpToYears(currentStats.getAverageSecondsPerMatch()));
        setStatValue(StatType.AVG_QUEUE_TIME, TimeUtil.formatTimeUpToYears(currentStats.getAverageSecondsInQueue()));
        setStatValue(StatType.AVG_MATCH_WAIT_TIME, TimeUtil.formatTimeUpToYears(currentStats.getAverageSecondsWaitedPerMatch()));
        setStatValue(StatType.MATCHES_SUBMITTED, String.valueOf(currentStats.getMatchesSubmitted()));
        setStatValue(StatType.ESCAPES, String.valueOf(currentStats.getEscapes()));
        setStatValue(StatType.ESCAPES_IN_A_ROW, String.valueOf(currentStats.getEscapesInARow()));
        setStatValue(StatType.MAX_ESCAPES_IN_A_ROW, String.valueOf(currentStats.getMaxEscapesInARow()));
        setStatValue(StatType.DEATHS, String.valueOf(currentStats.getDeaths()));
        setStatValue(StatType.DEATHS_IN_A_ROW, String.valueOf(currentStats.getDeathsInARow()));
        setStatValue(StatType.MAX_DEATHS_IN_A_ROW, String.valueOf(currentStats.getMaxDeathsInARow()));

        setStatValue(StatType.SURVIVAL_PROBABILITY, currentStats.getMatchesSubmitted() == 0 ?
                "N/A" :
                String.format("%.1f %%", currentStats.getSurvivalProbability()));

        float mapVariability = calculateMapsDistro(currentStats);
        setStatValue(StatType.MAP_RANDOMNESS, currentStats.getMatchesPlayed() == 0 ?
                "N/A" :
                String.format("%.1f %% (%s)", mapVariability * 100, getVariabilityLabel(mapVariability)));

        float killerVariability = calculateKillersDistro(currentStats);
        setStatValue(StatType.KILLER_VARIABILITY, currentStats.getMatchesPlayed() == 0 ?
                "N/A" :
                String.format("%.1f %% (%s)", killerVariability * 100, getVariabilityLabel(killerVariability)));
    }


    private float calculateMapsDistro(PeriodStats periodStats) {
        Collection<Integer> mapsDistro = Arrays.stream(RealmMap.values())
                .filter(RealmMap::isIdentified)
                .map(rm -> periodStats.getMapStats() != null
                        && periodStats.getMapStats() != null
                        && periodStats.getMapStats().containsKey(rm) ?
                        periodStats.getMapStats().get(rm).getMatches()
                        : 0)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        return StatsUtils.rateDistribution(mapsDistro);
    }

    private float calculateKillersDistro(PeriodStats periodStats) {
        Collection<Integer> distro = Arrays.stream(Killer.values())
                .filter(Killer::isIdentified)
                .map(k -> periodStats.getKillersStats() != null
                        && periodStats.getKillersStats() != null
                        && periodStats.getKillersStats().containsKey(k) ?
                        periodStats.getKillersStats().get(k).getMatches() :
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
            label = "pretty bad";
        } else if (variability > 0.6 && variability <= 0.7) {
            label = "a bit bad";
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
        content.append(statsPeriodTitle.getText() + "\n");
        content.append("-----------------------------------\n");

        statsContainer.entrySet().forEach(e -> {
            content.append(e.getKey().description + ": ");
            content.append(((JLabel) e.getValue()).getText() + "\n");
        });

        StringSelection stringSelection = new StringSelection(content.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    private void setStatValue(StatType statType, String value) {
        statsContainer.get(statType, JLabel.class).setText(value);
    }


    public void updateKiller(Killer killer) {
        this.killer = killer;
    }

    public void updateMap(RealmMap realmMap) {
        this.realmMap = realmMap;
    }


    public void notifyMatchDetected() {
        firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null);
    }

    public void notifyEndOfMatch(int matchSeconds) {
        Stats stats = dataService.getStats();
        stats.incrementMatchesPlayed(killer, realmMap);
        stats.incrementSecondsPlayed(matchSeconds, killer, realmMap);
        dataService.notifyChange();
        refreshStats();
    }

}
