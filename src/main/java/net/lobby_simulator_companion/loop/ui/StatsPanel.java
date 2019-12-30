package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.PeriodStats;
import net.lobby_simulator_companion.loop.domain.Stats;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.ui.common.CollapsablePanel;
import net.lobby_simulator_companion.loop.ui.common.Colors;
import net.lobby_simulator_companion.loop.ui.common.ComponentUtils;
import net.lobby_simulator_companion.loop.ui.common.NameValueInfoPanel;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.util.TimeUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static net.lobby_simulator_companion.loop.domain.Stats.Period;
import static net.lobby_simulator_companion.loop.ui.common.ResourceFactory.Icon;

/**
 * @author NickyRamone
 */
public class StatsPanel extends JPanel {

    public static final String EVENT_STRUCTURE_CHANGED = "structure_changed";

    private static final Font font = ResourceFactory.getRobotoFont();

    private enum StatType {
        MATCHES_PLAYED("Matches played"),
        TIME_PLAYED("Time played"),
        TIME_WAITED("Time waited"),
        AVG_TIME_PER_MATCH("Average time per match"),
        AVG_WAIT_TIME_PER_MATCH("Average wait time per match"),
        MATCHES_SUBMITTED("Matches submitted"),
        ESCAPES("Escapes"),
        ESCAPES_IN_A_ROW("Escapes in a row"),
        MAX_ESCAPES_IN_A_ROW("Max. escapes in a row"),
        DEATHS("Deaths"),
        DEATHS_IN_A_ROW("Deaths in a row"),
        MAX_DEATHS_IN_A_ROW("Max. deaths in a row"),
        SURVIVAL_PROBABILITY("Survival probability");

        String description;

        StatType(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private enum SurvivalStatus {None, Escaped, Died}

    private final Settings settings;
    private final LoopDataService dataService;

    private JLabel escapedButton;
    private JLabel diedButton;
    private JLabel periodLabel;
    private JLabel statsPeriodTitle;
    private NameValueInfoPanel<StatType> statsContainer;
    private final Period[] statPeriods = Period.values();
    private Period currentStatPeriod;
    private SurvivalStatus survivalStatus = SurvivalStatus.None;
    private Stats statsBackup;
    private boolean matchEnded;
    private Killer killer = Killer.UNIDENTIFIED;


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
        Date statsResetDate = Date.from(periodStats.getPeriodEnd().atZone(ZoneId.systemDefault()).toInstant());

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
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

        JPanel freqTitleContainer = new JPanel();
        freqTitleContainer.setBackground(Colors.INFO_PANEL_BACKGROUND);
        freqTitleContainer.add(statsPeriodTitle);

        NameValueInfoPanel.Builder<StatType> builder = new NameValueInfoPanel.Builder<>();
        for (StatType statType : StatType.values()) {
            builder.addField(statType, statType.description() + ":");
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
            survivalStatus = SurvivalStatus.None;
            refreshStats();
        }
    }

    public void notifyMatchSurvival(Boolean escaped) {
        if (escaped == null) {
            return;
        }
        if (escaped) {
            dataService.getStats().incrementEscapes(killer);
        } else {
            dataService.getStats().incrementDeaths(killer);
        }
        dataService.notifyChange();
        refreshStats();
    }

    private void refreshStats() {
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
        setStatValue(StatType.TIME_WAITED, TimeUtil.formatTimeUpToYears(currentStats.getSecondsWaited()));
        setStatValue(StatType.AVG_TIME_PER_MATCH, TimeUtil.formatTimeUpToYears(currentStats.getAverageSecondsPerMatch()));
        setStatValue(StatType.AVG_WAIT_TIME_PER_MATCH, TimeUtil.formatTimeUpToYears(currentStats.getAverageSecondsWaitedPerMatch()));
        setStatValue(StatType.MATCHES_SUBMITTED, String.valueOf(currentStats.getMatchesSubmitted()));
        setStatValue(StatType.ESCAPES, String.valueOf(currentStats.getEscapes()));
        setStatValue(StatType.ESCAPES_IN_A_ROW, String.valueOf(currentStats.getEscapesInARow()));
        setStatValue(StatType.MAX_ESCAPES_IN_A_ROW, String.valueOf(currentStats.getMaxEscapesInARow()));
        setStatValue(StatType.DEATHS, String.valueOf(currentStats.getDeaths()));
        setStatValue(StatType.DEATHS_IN_A_ROW, String.valueOf(currentStats.getDeathsInARow()));
        setStatValue(StatType.MAX_DEATHS_IN_A_ROW, String.valueOf(currentStats.getMaxDeathsInARow()));
        setStatValue(StatType.SURVIVAL_PROBABILITY, String.format("%.1f %%", currentStats.getSurvivalProbability()));
    }

    private void setStatValue(StatType statType, String value) {
        statsContainer.get(statType, JLabel.class).setText(value);
    }


    public void updateKiller(Killer killer) {
        this.killer = killer;
    }


    public void notifyMatchDetected() {
        matchEnded = false;
        firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null);
    }

    public void notifyEndOfMatch(int waitSeconds, int matchSeconds) {
        matchEnded = true;
        Stats stats = dataService.getStats();
        stats.incrementMatchesPlayed(killer);
        stats.incrementSecondsPlayed(killer, matchSeconds);
        stats.incrementSecondsWaited(waitSeconds);
        backupStats();
        if (survivalStatus == SurvivalStatus.Escaped) {
            stats.incrementEscapes(killer);
        } else if (survivalStatus == SurvivalStatus.Died) {
            stats.incrementDeaths(killer);
        }
        dataService.notifyChange();
        refreshStats();
    }

}
