package net.lobby_simulator_companion.loop.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.stats.Match;
import net.lobby_simulator_companion.loop.service.GameEvent;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.ui.common.CollapsablePanel;
import net.lobby_simulator_companion.loop.ui.common.NameValueInfoPanel;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.ui.common.UiConstants;
import net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator;
import net.lobby_simulator_companion.loop.util.TimeUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.lobby_simulator_companion.loop.service.log_event_orchestrators.ChaseEventManager.ChaseInfo;
import static net.lobby_simulator_companion.loop.ui.common.UiConstants.COLOR__INFO_PANEL__BG;
import static net.lobby_simulator_companion.loop.ui.common.UiConstants.WIDTH__INFO_PANEL__NAME_COLUMN;
import static net.lobby_simulator_companion.loop.ui.common.UiConstants.WIDTH__INFO_PANEL__VALUE_COLUMN;
import static net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator.UiEvent;

/**
 * @author NickyRamone
 */
@Slf4j
public class MatchPanel extends JPanel {

    private static final String MSG__SURVIVAL_TRUE = "yes";
    private static final String MSG__SURVIVAL_FALSE = "no";
    private static final String MSG__NOT_AVAILABLE = "--";
    private static final String MSG__RUNNER_PODIUM = "Runners chase times";

    private static final Font font = ResourceFactory.getRobotoFont();


    @RequiredArgsConstructor
    private enum InfoType {
        QUEUE_TIME("Time in queue:"),
        WAIT_TIME("Total match wait time:"),
        PLAY_TIME("Time played in match:"),
        REALM_MAP("Map:"),
        SURVIVED("Survived?:");

        final String description;
    }

    private final GameStateManager gameStateManager;
    private final UiEventOrchestrator uiEventOrchestrator;

    @Getter
    private NameValueInfoPanel matchInfoPanel;
    private JPanel detailsRunnersPanel;
    private NameValueInfoPanel runnerPodiumPanel;

    public MatchPanel(Settings settings, GameStateManager gameStateManager, UiEventOrchestrator uiEventOrchestrator) {
        this.gameStateManager = gameStateManager;
        this.uiEventOrchestrator = uiEventOrchestrator;

        draw(settings);
        initListeners();
    }

    private void initListeners() {
        gameStateManager.registerListener(GameEvent.CONNECTED_TO_LOBBY,
                evt -> {
                    refreshMatchInfoOnScreen();
                    detailsRunnersPanel.setVisible(false);
                });
        gameStateManager.registerListener(GameEvent.MATCH_STARTED,
                evt -> refreshMatchInfoOnScreen());
        gameStateManager.registerListener(GameEvent.UPDATED_CHASE_SUMMARY,
                evt -> refreshChaseSummaryOnScreen((List<ChaseInfo>) evt.getValue()));
        gameStateManager.registerListener(GameEvent.MATCH_ENDED,
                evt -> refreshMatchInfoOnScreen());
    }

    private void draw(Settings settings) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel collapsablePanel = new CollapsablePanel(
                createTitleBar(),
                createDetailsPanel(),
                settings,
                "ui.panel.match.collapsed");
        collapsablePanel.addPropertyChangeListener(evt ->
                uiEventOrchestrator.fireEvent(UiEventOrchestrator.UiEvent.STRUCTURE_RESIZED));

        add(collapsablePanel);
    }

    private JPanel createTitleBar() {
        Border border = new EmptyBorder(5, 5, 5, 5);

        JLabel summaryLabel = new JLabel("Match:");
        summaryLabel.setBorder(border);
        summaryLabel.setForeground(UiConstants.COLOR__TITLE_BAR__FG);
        summaryLabel.setFont(ResourceFactory.getRobotoFont());

        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(UiConstants.COLOR__TITLE_BAR__BG);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(summaryLabel);
        container.add(Box.createHorizontalGlue());

        return container;
    }


    private JPanel createDetailsPanel() {

        JPanel matchInfoPanel = createMatchInfoPanel();
        JPanel runnersPanel = createRunnersPanel();
        detailsRunnersPanel = runnersPanel;
        detailsRunnersPanel.setVisible(false);

        JPanel boxContainer = new JPanel();
        boxContainer.setBackground(COLOR__INFO_PANEL__BG);
        boxContainer.setLayout(new BoxLayout(boxContainer, BoxLayout.Y_AXIS));
        boxContainer.add(Box.createVerticalStrut(5));
        boxContainer.add(matchInfoPanel);
        boxContainer.add(Box.createVerticalStrut(10));
        boxContainer.add(runnersPanel);

        return boxContainer;
    }

    private JPanel createMatchInfoPanel() {
        NameValueInfoPanel.Builder panelBuilder = new NameValueInfoPanel.Builder();
        panelBuilder.setSizes(WIDTH__INFO_PANEL__NAME_COLUMN, WIDTH__INFO_PANEL__VALUE_COLUMN, 120);
        panelBuilder.addField(InfoType.QUEUE_TIME, InfoType.QUEUE_TIME.description);
        panelBuilder.addField(InfoType.WAIT_TIME, InfoType.WAIT_TIME.description);
        panelBuilder.addField(InfoType.PLAY_TIME, InfoType.PLAY_TIME.description);
        panelBuilder.addField(InfoType.REALM_MAP, InfoType.REALM_MAP.description);
        panelBuilder.addField(InfoType.SURVIVED, InfoType.SURVIVED.description);
        matchInfoPanel = panelBuilder.build();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(matchInfoPanel);

        return panel;
    }

    @SuppressWarnings("unchecked")
    private JPanel createRunnersPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(460, 130));
        panel.setBackground(UiConstants.COLOR__INFO_PANEL__BG);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        Font underlinedFont = font.deriveFont(attributes);

        JLabel title = new JLabel(MSG__RUNNER_PODIUM, JLabel.LEFT);
        title.setFont(underlinedFont);
        title.setForeground(Color.MAGENTA);
        title.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        NameValueInfoPanel.Builder panelBuilder = new NameValueInfoPanel.Builder();
        panelBuilder.setSizes(300, 300, 200);
        panelBuilder.addField(0, null);
        panelBuilder.addField(1, null);
        panelBuilder.addField(2, null);
        panelBuilder.addField(3, null);

        runnerPodiumPanel = panelBuilder.build();
        panel.add(title, JLabel.CENTER);
        panel.add(Box.createVerticalStrut(5));
        panel.add(runnerPodiumPanel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }


    private void refreshMatchInfoOnScreen() {
        Match match = gameStateManager.getCurrentMatch();
        refreshMatchInfoField(InfoType.QUEUE_TIME, TimeUtil.formatTimeUpToYears(match.getSecondsQueued()));
        refreshMatchInfoField(InfoType.WAIT_TIME, TimeUtil.formatTimeUpToYears(match.getSecondsWaited()));
        refreshMatchInfoField(InfoType.PLAY_TIME,
                match.isCancelled() ? MSG__NOT_AVAILABLE : TimeUtil.formatTimeUpToYears(match.getSecondsPlayed()));
        refreshMatchInfoField(InfoType.REALM_MAP, match.getRealmMap());
        refreshMatchInfoField(InfoType.SURVIVED,
                Optional.ofNullable(match.getEscaped())
                        .map(escaped -> escaped ? MSG__SURVIVAL_TRUE : MSG__SURVIVAL_FALSE)
                        .orElse(MSG__NOT_AVAILABLE)
        );
    }

    private void refreshMatchInfoField(InfoType infoType, Object value) {
        matchInfoPanel.get(infoType).setText(Optional.ofNullable(value).map(Object::toString).orElse(MSG__NOT_AVAILABLE));
    }


    private void refreshChaseSummaryOnScreen(List<ChaseInfo> chases) {
        log.trace("Chase summary: {}", chases);
        detailsRunnersPanel.setVisible(!chases.isEmpty());
        int numRunners = chases.size();

        for (int i = 0; i < numRunners; i++) {
            refreshChaseRow(i, chases.get(i));
        }

        for (int i = numRunners; i < 4; i++) {
            refreshChaseRow(i, null);
        }

        uiEventOrchestrator.fireEvent(UiEvent.STRUCTURE_RESIZED);
    }


    private void refreshChaseRow(int rowIndex, ChaseInfo chaseSummaryEntry) {
        JLabel nameLabel = runnerPodiumPanel.getNameComponent(rowIndex);
        JLabel valueLabel = (JLabel) runnerPodiumPanel.getValueComponent(rowIndex);

        String player = null;
        String chaseTime = null;

        if (chaseSummaryEntry != null) {
            StringBuilder sb = new StringBuilder();

            sb.append(Optional.ofNullable(chaseSummaryEntry.getPlayerName()).orElse("?"));
            sb.append(" (").append(chaseSummaryEntry.getSurvivor());

            if (chaseSummaryEntry.getSurvivorTeamIdx() >= 0) {
                sb.append(' ').append(chaseSummaryEntry.getSurvivorTeamIdx() + 1);
            }

            sb.append("):");
            player = sb.toString();
            chaseTime = TimeUtil.formatTimeUpToYears((int) chaseSummaryEntry.getTotalChaseMillis() / 1000)
                    + " (" + chaseSummaryEntry.getChaseCount() + " chases)";
        }

        nameLabel.setText(player);
        valueLabel.setText(chaseTime);
    }

}
