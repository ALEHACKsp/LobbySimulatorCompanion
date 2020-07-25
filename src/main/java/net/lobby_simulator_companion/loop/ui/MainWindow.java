package net.lobby_simulator_companion.loop.ui;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.domain.stats.Match;
import net.lobby_simulator_companion.loop.service.GameEvent;
import net.lobby_simulator_companion.loop.service.GameStateManager;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.ui.common.ComponentUtils;
import net.lobby_simulator_companion.loop.ui.common.FontUtil;
import net.lobby_simulator_companion.loop.ui.common.MouseDragListener;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.ui.common.UiConstants;
import net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator;
import net.lobby_simulator_companion.loop.util.TimeUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static net.lobby_simulator_companion.loop.ui.common.ResourceFactory.Icon;
import static net.lobby_simulator_companion.loop.ui.common.UiConstants.WIDTH__LOOP_MAIN;
import static net.lobby_simulator_companion.loop.ui.common.UiEventOrchestrator.UiEvent;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * @author NickyRamone
 */
@Slf4j
public class MainWindow extends JFrame {

    public static final String PROPERTY_EXIT_REQUEST = "exit.request";

    private static final String SETTING__WINDOW_FRAME_X = "ui.window.position.x";
    private static final String SETTING__WINDOW_FRAME_Y = "ui.window.position.y";
    private static final String SETTING__MAIN_PANEL_COLLAPSED = "ui.panel.main.collapsed";
    private static final Dimension MINIMUM_SIZE = new Dimension(WIDTH__LOOP_MAIN, 25);
    private static final Dimension MAXIMUM_SIZE = new Dimension(WIDTH__LOOP_MAIN, 500);
    private static final Border NO_BORDER = BorderFactory.createEmptyBorder();
    private static final int INFINITE_SIZE = 9999;
    private static final Font font = ResourceFactory.getRobotoFont();

    private static final int MAX_KILLER_PLAYER_NAME_LEN = 25;
    private static final String MSG__KILLER_VS_RECORD__NONE = "You have not played against this killer player.";
    private static final String MSG__KILLER_VS_RECORD__TIED = "You are tied against this killer player.";
    private static final String MSG__KILLER_VS_RECORD__KILLER_LOSES = "You dominate this killer player in record.";
    private static final String MSG__KILLER_VS_RECORD__KILLER_WINS = "You are dominated by this killer player in record.";
    private static final String MSG__STATUS__STARTING_UP = "Starting up...";
    private static final String MSG__STATUS__CONNECTED = "In Lobby";
    private static final String MSG__STATUS__DISCONNECTED = "Idle";
    private static final String MSG__STATUS__IN_MATCH = "In match";
    private static final String MSG__STATUS__MATCH_FINISHED = "Match finished";
    private static final String MSG__STATUS__SEARCHING_LOBBY = "Searching for lobby";
    private static final String MSG__TITLE_BAR__QUEUE_TIME = "Queue: ";
    private static final String MSG__TITLE_BAR__MATCH_TIME = "Match: ";
    private static final String TOOLTIP__BUTTON__FORCE_DISCONNECT = "Force disconnect (this will not affect the game)";
    private static final String TOOLTIP__BUTTON__EXIT_APP = "Exit application";
    private static final String MSG__STATUS__MATCH_CANCELLED = "Match was cancelled.";
    private static final String MSG__STATUS__PLAYER_ESCAPED = "You escaped :)";
    private static final String MSG__STATUS__PLAYER_DIED = "You died :(";

    private final Settings settings;
    private final AppProperties appProperties;
    private final LoopDataService dataService;
    private final GameStateManager gameStateManager;
    private final UiEventOrchestrator uiEventOrchestrator;
    private final ServerPanel serverPanel;
    private final KillerPanel killerPanel;
    private final MatchPanel matchPanel;
    private final StatsPanel statsPanel;
    private final SurvivalInputPanel survivalInputPanel;

    private Timer queueTimer;
    private Timer matchTimer;

    private JPanel titleBar;
    private JPanel messagePanel;
    private JLabel lastConnMsgLabel;
    private JLabel connStatusLabel;
    private JLabel killerSkullIcon;
    private JPanel killerInfoContainer;
    private JLabel killerPlayerValueLabel;
    private JLabel killerPlayerRateLabel;
    private JLabel killerPlayerNotesLabel;
    private JLabel killerSubtitleLabel;
    private JPanel titleBarTimerContainer;
    private JLabel connTimerLabel;
    private JLabel disconnectButton;
    private JLabel titleBarMinimizeLabel;
    private JPanel detailPanel;
    private boolean detailPanelSavedVisibilityState;


    public MainWindow(Settings settings, AppProperties appProperties, LoopDataService loopDataService,
                      GameStateManager gameStateManager, UiEventOrchestrator uiEventOrchestrator,
                      ServerPanel serverPanel, MatchPanel matchPanel, KillerPanel killerPanel, StatsPanel statsPanel,
                      SurvivalInputPanel survivalInputPanel) {
        this.settings = settings;
        this.appProperties = appProperties;
        this.dataService = loopDataService;
        this.gameStateManager = gameStateManager;
        this.uiEventOrchestrator = uiEventOrchestrator;
        this.serverPanel = serverPanel;
        this.matchPanel = matchPanel;
        this.killerPanel = killerPanel;
        this.statsPanel = statsPanel;
        this.survivalInputPanel = survivalInputPanel;

        initTimers();
        draw();
        hidePanels();
        showStatus(MSG__STATUS__STARTING_UP);
    }

    private void hidePanels() {
        titleBarMinimizeLabel.setVisible(false);
        detailPanelSavedVisibilityState = detailPanel.isVisible();
        detailPanel.setVisible(false);
    }

    private void restorePanels() {
        detailPanel.setVisible(detailPanelSavedVisibilityState);
        titleBarMinimizeLabel.setVisible(true);
    }

    public void start() {
        restorePanels();
        showStatus(MSG__STATUS__DISCONNECTED);
        initGameStateListeners(gameStateManager, uiEventOrchestrator);
    }


    private void initGameStateListeners(GameStateManager gameStateManager, UiEventOrchestrator uiEventOrchestrator) {
        gameStateManager.registerListener(GameEvent.DISCONNECTED, evt -> handleServerDisconnect());
        gameStateManager.registerListener(GameEvent.START_LOBBY_SEARCH, evt -> handleLobbySearchStart());
        gameStateManager.registerListener(GameEvent.CONNECTED_TO_LOBBY, evt -> handleLobbyConnect());
        gameStateManager.registerListener(GameEvent.START_MAP_GENERATION, evt -> handleMapGeneration());
        gameStateManager.registerListener(GameEvent.MATCH_STARTED, evt -> handleMatchStart());
        gameStateManager.registerListener(GameEvent.MATCH_ENDED, evt -> handleMatchEnd((Match) evt.getValue()));
        gameStateManager.registerListener(GameEvent.NEW_KILLER_PLAYER, evt -> refreshKillerPlayerOnTitleBar((Player) evt.getValue()));

        uiEventOrchestrator.registerListener(UiEvent.UPDATE_KILLER_PLAYER_TITLE_EXTRA,
                evt -> refreshKillerPlayerSubtitleOnScreen((String) evt.getValue()));
        uiEventOrchestrator.registerListener(UiEvent.STRUCTURE_RESIZED, evt -> pack());
    }


    public void showMessage(String message) {
        lastConnMsgLabel.setText(message);
        messagePanel.setVisible(true);
        pack();
    }

    public void hideMessage() {
        messagePanel.setVisible(false);
        pack();
    }

    private void showStatus(String statusMessage) {
        connStatusLabel.setText(statusMessage);
    }

    private void initTimers() {
        matchTimer = new Timer(1000, e -> displayMatchTimer(gameStateManager.getMatchDurationInSeconds()));
        queueTimer = new Timer(1000, e -> displayQueueTimer(gameStateManager.getQueueTimeInSeconds()));
    }

    private void draw() {
        setAlwaysOnTop(true);
        setUndecorated(true);
        setOpacity(0.9f);
        setMinimumSize(MINIMUM_SIZE);
        setMaximumSize(MAXIMUM_SIZE);
        setLocation(settings.getInt(SETTING__WINDOW_FRAME_X), settings.getInt(SETTING__WINDOW_FRAME_Y));

        createComponents();
    }

    private void createComponents() {
        titleBar = createTitleBar();
        messagePanel = createMessagePanel();

        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                titleBarMinimizeLabel.setIcon(ResourceFactory.getIcon(Icon.COLLAPSE));
                pack();
                super.componentShown(e);
                settings.set(SETTING__MAIN_PANEL_COLLAPSED, false);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                titleBarMinimizeLabel.setIcon(ResourceFactory.getIcon(Icon.EXPAND));
                pack();
                super.componentHidden(e);
                settings.set(SETTING__MAIN_PANEL_COLLAPSED, true);
            }
        });

        survivalInputPanel.addPropertyChangeListener(evt -> {
            if (SurvivalInputPanel.EVENT_SURVIVAL_INPUT_DONE.equals(evt.getPropertyName())) {
                survivalInputPanel.setVisible(false);
                detailPanel.setVisible(detailPanelSavedVisibilityState);
                pack();
            }
        });

        detailPanel.add(serverPanel);
        detailPanel.add(killerPanel);
        detailPanel.add(matchPanel);
        detailPanel.add(statsPanel);
        detailPanel.add(Box.createVerticalGlue());
        detailPanel.setVisible(!settings.getBoolean(SETTING__MAIN_PANEL_COLLAPSED));


        JPanel collapsablePanel = new JPanel();
        collapsablePanel.setMaximumSize(new Dimension(INFINITE_SIZE, 30));
        collapsablePanel.setBackground(Color.BLACK);
        collapsablePanel.setLayout(new BoxLayout(collapsablePanel, BoxLayout.Y_AXIS));
        collapsablePanel.add(titleBar);
        collapsablePanel.add(messagePanel);
        collapsablePanel.add(survivalInputPanel);
        collapsablePanel.add(detailPanel);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                Point frameLocation = getLocation();
                settings.set(SETTING__WINDOW_FRAME_X, frameLocation.x);
                settings.set(SETTING__WINDOW_FRAME_Y, frameLocation.y);
            }
        });

        setContentPane(collapsablePanel);
        setVisible(true);
        pack();
    }


    private JPanel createTitleBar() {
        Border border = new EmptyBorder(3, 5, 0, 5);

        JLabel appLabel = new JLabel(appProperties.get("app.name.short"));
        appLabel.setBorder(border);
        appLabel.setForeground(Color.WHITE);
        appLabel.setFont(font);

        JLabel separatorLabel = new JLabel("|");
        separatorLabel.setBorder(border);
        separatorLabel.setForeground(Color.WHITE);
        separatorLabel.setFont(font);

        connStatusLabel = new JLabel();
        connStatusLabel.setBorder(border);
        connStatusLabel.setForeground(Color.WHITE);
        connStatusLabel.setFont(font);

        killerSkullIcon = new JLabel();
        killerSkullIcon.setBorder(border);
        killerSkullIcon.setFont(font);

        killerPlayerValueLabel = new JLabel();
        killerPlayerValueLabel.setBorder(border);
        killerPlayerValueLabel.setForeground(Color.BLUE);
        killerPlayerValueLabel.setFont(font);

        killerPlayerRateLabel = new JLabel();
        killerPlayerRateLabel.setBorder(new EmptyBorder(2, 0, 0, 5));
        killerPlayerRateLabel.setVisible(false);

        killerPlayerNotesLabel = new JLabel();
        killerPlayerNotesLabel.setVisible(false);
        killerPlayerNotesLabel.setBorder(new EmptyBorder(2, 0, 0, 5));
        killerPlayerNotesLabel.setIcon(ResourceFactory.getIcon(Icon.EDIT));

        killerSubtitleLabel = new JLabel();
        killerSubtitleLabel.setBorder(new EmptyBorder(2, 0, 0, 5));
        killerSubtitleLabel.setForeground(Color.BLUE);
        killerSubtitleLabel.setFont(font);

        killerInfoContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        killerInfoContainer.setBorder(NO_BORDER);
        killerInfoContainer.setBackground(UiConstants.COLOR__STATUS_BAR__DISCONNECTED__BG);
        killerInfoContainer.add(killerSkullIcon);
        killerInfoContainer.add(killerPlayerValueLabel);
        killerInfoContainer.add(killerPlayerRateLabel);
        killerInfoContainer.add(killerPlayerNotesLabel);
        killerInfoContainer.add(killerSubtitleLabel);
        killerInfoContainer.setVisible(false);

        JLabel timerSeparatorLabel = new JLabel();
        timerSeparatorLabel.setBorder(border);
        timerSeparatorLabel.setText("|");

        connTimerLabel = new JLabel();
        connTimerLabel.setBorder(border);
        connTimerLabel.setForeground(Color.WHITE);
        connTimerLabel.setFont(font);
        displayQueueTimer(0);

        titleBarTimerContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBarTimerContainer.setBackground(UiConstants.COLOR__STATUS_BAR__CONNECTED__BG);
        titleBarTimerContainer.add(timerSeparatorLabel);
        titleBarTimerContainer.add(connTimerLabel);
        titleBarTimerContainer.setVisible(false);

        JPanel connMsgPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        connMsgPanel.setBackground(UiConstants.COLOR__STATUS_BAR__DISCONNECTED__BG);
        connMsgPanel.add(appLabel);
        connMsgPanel.add(separatorLabel);
        connMsgPanel.add(connStatusLabel);
        connMsgPanel.add(killerInfoContainer);
        connMsgPanel.add(titleBarTimerContainer);
        MouseDragListener mouseDragListener = new MouseDragListener(this);
        connMsgPanel.addMouseListener(mouseDragListener);
        connMsgPanel.addMouseMotionListener(mouseDragListener);

        disconnectButton = ComponentUtils.createButtonLabel(
                null,
                TOOLTIP__BUTTON__FORCE_DISCONNECT,
                Icon.DISCONNECT,
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        gameStateManager.forceDisconnect();
                    }
                });
        disconnectButton.setBorder(border);
        disconnectButton.setVisible(false);

        JLabel switchOffButton = new JLabel();
        switchOffButton.setBorder(border);
        switchOffButton.setIcon(ResourceFactory.getIcon(Icon.SWITCH_OFF));
        switchOffButton.setToolTipText(TOOLTIP__BUTTON__EXIT_APP);
        switchOffButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchOffButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                firePropertyChange(PROPERTY_EXIT_REQUEST, false, true);
            }
        });

        titleBarMinimizeLabel = new JLabel();
        titleBarMinimizeLabel.setBorder(border);
        titleBarMinimizeLabel.setIcon(ResourceFactory.getIcon(Icon.COLLAPSE));
        titleBarMinimizeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        titleBarMinimizeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detailPanel.setVisible(!detailPanel.isVisible());
                pack();
            }
        });

        JPanel titleBarButtonContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBarButtonContainer.setBackground(UiConstants.COLOR__STATUS_BAR__DISCONNECTED__BG);
        titleBarButtonContainer.add(disconnectButton);
        titleBarButtonContainer.add(switchOffButton);
        titleBarButtonContainer.add(titleBarMinimizeLabel);

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.setPreferredSize(new Dimension(200, 25));
        container.setMaximumSize(new Dimension(INFINITE_SIZE, 25));
        container.setBackground(UiConstants.COLOR__STATUS_BAR__DISCONNECTED__BG);
        container.add(connMsgPanel, BorderLayout.CENTER);
        container.add(titleBarButtonContainer, BorderLayout.EAST);

        return container;
    }

    private JPanel createMessagePanel() {
        lastConnMsgLabel = new JLabel();
        lastConnMsgLabel.setForeground(UiConstants.COLOR__MSG_BAR__FG);
        lastConnMsgLabel.setFont(font);

        JLabel elapsedLabel = new JLabel();
        elapsedLabel.setForeground(UiConstants.COLOR__MSG_BAR__FG);
        elapsedLabel.setFont(font);

        JPanel container = new JPanel();
        container.setBackground(UiConstants.COLOR__MSG_BAR__BG);
        container.setLayout(new FlowLayout());
        container.add(lastConnMsgLabel);
        container.add(elapsedLabel);
        container.setVisible(false);

        return container;
    }

    private void refreshKillerPlayerSubtitleOnScreen(String subtitle) {
        connStatusLabel.setVisible(false);
        killerInfoContainer.setVisible(true);
        killerSubtitleLabel.setText(subtitle);
        killerSubtitleLabel.setVisible(isNotEmpty(subtitle));
    }

    private void refreshKillerPlayerOnTitleBar(Player player) {
        connStatusLabel.setVisible(false);
        killerInfoContainer.setVisible(true);

        if (player != null) {
            if (player.getMatchesPlayed() == 0) {
                killerSkullIcon.setIcon(ResourceFactory.getIcon(Icon.SKULL_WHITE));
                killerSkullIcon.setToolTipText(MSG__KILLER_VS_RECORD__NONE);
            } else if (player.getEscapes() == player.getDeaths()) {
                killerSkullIcon.setIcon(ResourceFactory.getIcon(Icon.SKULL_BLACK));
                killerSkullIcon.setToolTipText(MSG__KILLER_VS_RECORD__TIED);
            } else if (player.getEscapes() > player.getDeaths()) {
                killerSkullIcon.setIcon(ResourceFactory.getIcon(Icon.SKULL_BLUE));
                killerSkullIcon.setToolTipText(MSG__KILLER_VS_RECORD__KILLER_LOSES);
            } else {
                killerSkullIcon.setIcon(ResourceFactory.getIcon(Icon.SKULL_RED));
                killerSkullIcon.setToolTipText(MSG__KILLER_VS_RECORD__KILLER_WINS);
            }
            killerPlayerValueLabel.setText(player.getMostRecentName().map(this::shortenKillerPlayerName).orElse(null));
            refreshKillerPlayerRateOnTitleBar(player.getRating());
            killerPlayerNotesLabel.setVisible(player.getDescription() != null && !player.getDescription().isEmpty());
        }
    }

    private void refreshKillerPlayerRateOnTitleBar(Player.Rating rate) {
        if (rate == Player.Rating.THUMBS_UP) {
            killerPlayerRateLabel.setIcon(ResourceFactory.getIcon(Icon.THUMBS_UP));
            killerPlayerRateLabel.setVisible(true);
        } else if (rate == Player.Rating.THUMBS_DOWN) {
            killerPlayerRateLabel.setIcon(ResourceFactory.getIcon(Icon.THUMBS_DOWN));
            killerPlayerRateLabel.setVisible(true);
        } else {
            killerPlayerRateLabel.setIcon(null);
            killerPlayerRateLabel.setVisible(false);
        }
    }


    private String shortenKillerPlayerName(String playerName) {
        String result = playerName;
        if (result.length() > MAX_KILLER_PLAYER_NAME_LEN) {
            result = result.substring(0, MAX_KILLER_PLAYER_NAME_LEN - 3) + "...";
        }

        return FontUtil.replaceNonDisplayableChars(result);
    }


    private void handleServerDisconnect() {
        disconnectButton.setVisible(false);
        queueTimer.stop();
        displayQueueTimer(0);
        changeTitleBarColor(UiConstants.COLOR__STATUS_BAR__DISCONNECTED__BG, Color.WHITE);
        connStatusLabel.setText("Idle");
        connStatusLabel.setVisible(true);
        killerInfoContainer.setVisible(false);
        titleBarTimerContainer.setVisible(false);
        messagePanel.setVisible(false);
        pack();
    }

    private void handleLobbySearchStart() {
        disconnectButton.setVisible(true);
        changeTitleBarColor(UiConstants.COLOR__STATUS_BAR__SEARCHING_LOBBY__BG, Color.BLACK);
        connStatusLabel.setText(MSG__STATUS__SEARCHING_LOBBY);
        queueTimer.start();
        titleBarTimerContainer.setVisible(true);
    }

    private void handleLobbyConnect() {
        changeTitleBarColor(UiConstants.COLOR__STATUS_BAR__CONNECTED__BG, Color.WHITE);
        disconnectButton.setVisible(true);
        queueTimer.stop();
        connStatusLabel.setText(MSG__STATUS__CONNECTED);
        survivalInputPanel.setVisible(false);
        messagePanel.setVisible(false);
        pack();
    }

    private void handleMapGeneration() {
        titleBarTimerContainer.setVisible(false);
    }


    private void handleMatchStart() {
        connStatusLabel.setText(MSG__STATUS__IN_MATCH);
        displayMatchTimer(0);
        matchTimer.start();
        titleBarTimerContainer.setVisible(true);
    }


    private void handleMatchEnd(Match match) {
        matchTimer.stop();
        titleBarTimerContainer.setVisible(false);
        killerInfoContainer.setVisible(false);
        connStatusLabel.setText(MSG__STATUS__MATCH_FINISHED);
        connStatusLabel.setVisible(true);
        connTimerLabel.setText(TimeUtil.formatTimeUpToHours(0));

        if (match.isCancelled()) {
            showMessage(MSG__STATUS__MATCH_CANCELLED);
        } else {
            showMessage(match.escaped() ?
                    MSG__STATUS__PLAYER_ESCAPED
                    : MSG__STATUS__PLAYER_DIED);
        }
    }

    private void changeTitleBarColor(Color bgColor, Color fgColor) {
        Queue<Component> queue = new LinkedList<>();
        queue.add(titleBar);

        while (!queue.isEmpty()) {
            Component c = queue.poll();

            if (c instanceof JPanel) {
                JPanel panel = (JPanel) c;
                panel.setBackground(bgColor);
                queue.addAll(Arrays.asList(panel.getComponents()));
            } else if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                label.setForeground(fgColor);
            }
        }
    }

    private void displayQueueTimer(int seconds) {
        connTimerLabel.setText(MSG__TITLE_BAR__QUEUE_TIME + TimeUtil.formatTimeUpToHours(seconds));
    }

    private void displayMatchTimer(int seconds) {
        connTimerLabel.setText(MSG__TITLE_BAR__MATCH_TIME + TimeUtil.formatTimeUpToHours(seconds));
    }

    public void close() {
        settings.forceSave();
        dataService.save();
        dispose();
    }

}
