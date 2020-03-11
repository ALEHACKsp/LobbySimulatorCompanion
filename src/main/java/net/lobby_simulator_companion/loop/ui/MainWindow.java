package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.domain.RealmMap;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import net.lobby_simulator_companion.loop.service.PlayerDto;
import net.lobby_simulator_companion.loop.ui.common.Colors;
import net.lobby_simulator_companion.loop.ui.common.ComponentUtils;
import net.lobby_simulator_companion.loop.ui.common.MouseDragListener;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import net.lobby_simulator_companion.loop.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;

import static net.lobby_simulator_companion.loop.ui.common.ResourceFactory.Icon;


/**
 * @author NickyRamone
 */
public class MainWindow extends JFrame implements Observer {

    public static final String PROPERTY_EXIT_REQUEST = "exit.request";

    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

    private static final String SETTING__WINDOW_FRAME_X = "ui.window.position.x";
    private static final String SETTING__WINDOW_FRAME_Y = "ui.window.position.y";
    private static final String SETTING__MAIN_PANEL_COLLAPSED = "ui.panel.main.collapsed";
    private static final String MSG_CONNECTED = "In Lobby";
    private static final String MSG_DISCONNECTED = "Idle";
    private static final String MSG_KILLER_CHARACTER = "(as %s)";
    private static final Dimension MINIMUM_SIZE = new Dimension(600, 25);
    private static final Dimension MAXIMUM_SIZE = new Dimension(600, 500);
    private static final Border NO_BORDER = new EmptyBorder(0, 0, 0, 0);
    private static final int INFINITE_SIZE = 9999;
    private static final Font font = ResourceFactory.getRobotoFont();

    /**
     * Minimum time connected from which we can assume that a match has taken place.
     */
    private static final int MIN_MATCH_SECONDS = 60;
    private static final int MAX_KILLER_PLAYER_NAME_LEN = 25;
    private static final int SURVIVAL_INPUT_WINDOW_DELAY = 3000;


    private enum GameState {
        IDLE,
        SEARCHING_LOBBY,
        IN_LOBBY,
        IN_MATCH,
        AFTER_MATCH
    }

    private Settings settings;
    private DbdLogMonitor dbdLogMonitor;
    private LoopDataService dataService;
    private ServerPanel serverPanel;
    private KillerPanel killerPanel;
    private StatsPanel statsPanel;

    private AppProperties appProperties = Factory.getAppProperties();
    private GameState gameState = GameState.IDLE;
    private Timer queueTimer;
    private Long queueStartTime;
    private Integer queueTime;
    private Long matchWaitStartTime;
    private int matchWaitTime;
    private Timer matchTimer;
    private Long matchStartTime;
    private boolean connectionCountedAsMatch = false;

    private SurvivalInputPanel survivalInputPanel;
    private JPanel titleBar;
    private JPanel messagePanel;
    private JLabel lastConnMsgLabel;
    private JLabel connStatusLabel;
    private JLabel killerSkullIcon;
    private JPanel killerInfoContainer;
    private JLabel killerPlayerValueLabel;
    private JLabel killerPlayerRateLabel;
    private JLabel killerPlayerNotesLabel;
    private JLabel killerCharLabel;
    private JPanel titleBarTimerContainer;
    private JLabel connTimerLabel;
    private JLabel disconnectButton;
    private JLabel titleBarMinimizeLabel;
    private JPanel detailPanel;


    public MainWindow(Settings settings, DbdLogMonitor dbdLogMonitor, LoopDataService loopDataService,
                      ServerPanel serverPanel, KillerPanel killerPanel, StatsPanel statsPanel) {
        this.settings = settings;
        this.dbdLogMonitor = dbdLogMonitor;
        this.dataService = loopDataService;
        this.serverPanel = serverPanel;
        this.killerPanel = killerPanel;
        this.statsPanel = statsPanel;

        dbdLogMonitor.addObserver(this);
        initTimers();

        serverPanel.addPropertyChangeListener(evt -> pack());
        killerPanel.addPropertyChangeListener(evt -> {
            String propertyName = evt.getPropertyName();

            if (KillerPanel.EVENT_STRUCTURE_CHANGED.equals(propertyName)) {
                pack();
            } else if (gameState == GameState.IN_LOBBY && KillerPanel.EVENT_KILLER_UPDATE.equals(propertyName)) {
                updateKillerOnTitleBar();
            }
        });
        statsPanel.addPropertyChangeListener(evt -> pack());


        setAlwaysOnTop(true);
        setUndecorated(true);
        setOpacity(0.9f);
        setMinimumSize(MINIMUM_SIZE);
        setMaximumSize(MAXIMUM_SIZE);
        setLocation(settings.getInt(SETTING__WINDOW_FRAME_X), settings.getInt(SETTING__WINDOW_FRAME_Y));

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

        survivalInputPanel = new SurvivalInputPanel(statsPanel, killerPanel, dataService);
        survivalInputPanel.addPropertyChangeListener(evt -> {
            survivalInputPanel.setVisible(false);
            pack();
        });

        detailPanel.add(messagePanel);
        detailPanel.add(serverPanel);
        detailPanel.add(killerPanel);
        detailPanel.add(statsPanel);
        detailPanel.add(Box.createVerticalGlue());
        detailPanel.setVisible(!settings.getBoolean(SETTING__MAIN_PANEL_COLLAPSED));


        JPanel collapsablePanel = new JPanel();
        collapsablePanel.setMaximumSize(new Dimension(INFINITE_SIZE, 30));
        collapsablePanel.setBackground(Color.BLACK);
        collapsablePanel.setLayout(new BoxLayout(collapsablePanel, BoxLayout.Y_AXIS));
        collapsablePanel.add(titleBar);
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


    private void updateKillerOnTitleBar() {
        Player player = killerPanel.getKillerPlayer();
        connStatusLabel.setVisible(false);
        killerInfoContainer.setVisible(true);

        if (settings.getExperimentalSwitch(2)) {
            Killer killerChar = killerPanel.getKillerCharacter();
            killerCharLabel.setVisible(killerChar != null && killerPanel.isShowKillerCharacter());
            if (killerChar != null && killerChar != Killer.UNIDENTIFIED) {
                killerCharLabel.setText(String.format(MSG_KILLER_CHARACTER, killerChar.alias()));
            }
        }

        if (player != null) {
            if (player.getMatchesPlayed() == 0) {
                killerSkullIcon.setIcon(ResourceFactory.getIcon(Icon.SKULL_WHITE));
                killerSkullIcon.setToolTipText("You have not played against this killer player.");
            }
            else if (player.getEscapesAgainst() == player.getDeathsBy()) {
                killerSkullIcon.setIcon(ResourceFactory.getIcon(Icon.SKULL_BLACK));
                killerSkullIcon.setToolTipText("You are tied against this killer player.");
            }
            else if (player.getEscapesAgainst() > player.getDeathsBy()) {
                killerSkullIcon.setIcon(ResourceFactory.getIcon(Icon.SKULL_BLUE));
                killerSkullIcon.setToolTipText("You dominate this killer player in record.");
            }
            else {
                killerSkullIcon.setIcon(ResourceFactory.getIcon(Icon.SKULL_RED));
                killerSkullIcon.setToolTipText("You are dominated by this killer player in record.");
            }

            if (settings.getExperimentalSwitch(1)) {
                killerPlayerValueLabel.setText(shortenKillerPlayerName(player.getMostRecentName()));
            }

            updateKillerRateOnTitleBar(player.getRating());
            killerPlayerNotesLabel.setVisible(player.getDescription() != null && !player.getDescription().isEmpty());
        }
    }

    private void updateKillerRateOnTitleBar(Player.Rating rate) {
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

        connStatusLabel = new JLabel(MSG_DISCONNECTED);
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

        killerCharLabel = new JLabel();
        killerCharLabel.setBorder(new EmptyBorder(2, 0, 0, 5));
        killerCharLabel.setForeground(Color.BLUE);
        killerCharLabel.setFont(font);

        killerInfoContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        killerInfoContainer.setBorder(NO_BORDER);
        killerInfoContainer.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        killerInfoContainer.add(killerSkullIcon);
        killerInfoContainer.add(killerPlayerValueLabel);
        killerInfoContainer.add(killerPlayerRateLabel);
        killerInfoContainer.add(killerPlayerNotesLabel);

        killerInfoContainer.add(killerCharLabel);
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
        titleBarTimerContainer.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        titleBarTimerContainer.add(timerSeparatorLabel);
        titleBarTimerContainer.add(connTimerLabel);
        titleBarTimerContainer.setVisible(false);

        JPanel connMsgPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
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
                "Disconnect (this will not affect the game)",
                Icon.DISCONNECT,
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        turnToIdle();
                    }
                });
        disconnectButton.setBorder(border);
        disconnectButton.setVisible(false);

        JLabel switchOffButton = new JLabel();
        switchOffButton.setBorder(border);
        switchOffButton.setIcon(ResourceFactory.getIcon(Icon.SWITCH_OFF));
        switchOffButton.setToolTipText("Exit application");
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
        titleBarButtonContainer.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        titleBarButtonContainer.add(disconnectButton);
        titleBarButtonContainer.add(switchOffButton);
        titleBarButtonContainer.add(titleBarMinimizeLabel);

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.setPreferredSize(new Dimension(200, 25));
        container.setMaximumSize(new Dimension(INFINITE_SIZE, 25));
        container.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        container.add(connMsgPanel, BorderLayout.CENTER);
        container.add(titleBarButtonContainer, BorderLayout.EAST);

        return container;
    }

    private JPanel createMessagePanel() {
        lastConnMsgLabel = new JLabel();
        lastConnMsgLabel.setForeground(Colors.MSG_BAR_FOREGROUND);
        lastConnMsgLabel.setFont(font);

        JLabel elapsedLabel = new JLabel();
        elapsedLabel.setForeground(Colors.MSG_BAR_FOREGROUND);
        elapsedLabel.setFont(font);

        JPanel container = new JPanel();
        container.setBackground(Colors.MSG_BAR_BACKGROUND);
        container.setLayout(new FlowLayout());
        container.add(lastConnMsgLabel);
        container.add(elapsedLabel);
        container.setVisible(false);

        return container;
    }


    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof DbdLogMonitor) {
            DbdLogMonitor.Event event = (DbdLogMonitor.Event) arg;
            Runnable action = null;

            switch (event.type) {
                case MATCH_WAIT:
                    action = this::notifyWaitingForMatch;
                    break;
                case MATCH_WAIT_CANCEL:
                    action = this::notifyMatchWaitCancel;
                    break;
                case SERVER_CONNECT:
                    action = () -> notifyServerConnect(((InetSocketAddress) event.argument).getHostName());
                    break;
                case KILLER_PLAYER:
                    action = () -> notifyNewKillerPlayer((PlayerDto) event.argument);
                    break;
                case KILLER_CHARACTER:
                    action = () -> notifyNewKillerCharacter((Killer) event.argument);
                    break;
                case MAP_GENERATE:
                    action = () -> notifyMapGeneration((String) event.argument);
                    break;
                case MATCH_START:
                    action = this::notifyMatchStart;
                    break;
                case MATCH_END:
                    action = this::notifyMatchEnd;
                    break;
                case SERVER_DISCONNECT:
                    action = this::notifyServerDisconnect;
                    break;
            }

            SwingUtilities.invokeLater(action);
        }
    }

    private String shortenKillerPlayerName(String playerName) {
        String result = playerName;
        if (result.length() > MAX_KILLER_PLAYER_NAME_LEN) {
            result = result.substring(0, MAX_KILLER_PLAYER_NAME_LEN - 3) + "...";
        }

        return result;
    }


    private void initTimers() {
        matchTimer = new Timer(1000, e -> {
            int seconds = getMatchDuration();
            displayMatchTimer(seconds);

            if (seconds >= MIN_MATCH_SECONDS && !connectionCountedAsMatch) {
                connectionCountedAsMatch = true;
                statsPanel.notifyMatchDetected();
            }
        });

        queueTimer = new Timer(1000, e ->
                displayQueueTimer((int) (System.currentTimeMillis() - queueStartTime) / 1000));
    }

    private int getMatchDuration() {
        return matchStartTime == null ? 0 : (int) (System.currentTimeMillis() - matchStartTime) / 1000;
    }

    private void notifyWaitingForMatch() {
        if (gameState != GameState.IDLE) {
            return;
        }
        logger.debug("Event: waiting for match");
        disconnectButton.setVisible(true);
        changeTitleBarColor(Colors.CONNECTION_BAR_WAITING, Color.BLACK);
        connStatusLabel.setText("Waiting for match");
        queueStartTime = System.currentTimeMillis();
        queueTimer.start();
        matchWaitStartTime = queueStartTime;
        titleBarTimerContainer.setVisible(true);
        gameState = GameState.SEARCHING_LOBBY;
    }

    private void notifyMatchWaitCancel() {
        if (gameState != GameState.SEARCHING_LOBBY && gameState != GameState.IN_LOBBY) {
            return;
        }
        logger.debug("Event: match wait cancel");
        turnToIdle();
        pack();
    }

    private void notifyServerConnect(String serverAddress) {
        connectToMatch(serverAddress);
        notifyLobbyJoin();
    }

    private void notifyLobbyJoin() {
        if (gameState != GameState.SEARCHING_LOBBY) {
            return;
        }

        logger.debug("Event: lobby join");
        disconnectButton.setVisible(true);
        queueTime = (int) (System.currentTimeMillis() - queueStartTime) / 1000;
        queueStartTime = null;
        queueTimer.stop();
        dataService.getStats().incrementLobbiesFound();
        dataService.getStats().incrementSecondsInQueue(queueTime);
        dataService.notifyChange();
        statsPanel.refreshStats();
        statsPanel.updateMap(RealmMap.UNIDENTIFIED);
        connStatusLabel.setText(MSG_CONNECTED);
        killerPanel.clearKillerInfo();
        survivalInputPanel.setVisible(false);
        messagePanel.setVisible(false);
        changeTitleBarColor(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND, Color.WHITE);
        pack();
        gameState = GameState.IN_LOBBY;
    }

    private void notifyNewKillerPlayer(PlayerDto killerPlayer) {
        if (gameState != GameState.IN_LOBBY) {
            return;
        }
        logger.debug("Event: new killer player");
        killerPanel.receiveNewKillerPlayer(killerPlayer);
    }

    private void notifyNewKillerCharacter(Killer killer) {
        if (gameState != GameState.IN_LOBBY) {
            return;
        }
        logger.debug("Event: new killer character");
        killerPanel.updateKillerCharacter(killer);
        statsPanel.updateKiller(killer);
    }


    private void notifyMapGeneration(String mapId) {
        RealmMap realmMap = RealmMap.fromDbdId(mapId);
        logger.debug("Event: map generation: {}", realmMap.alias());
        statsPanel.updateMap(realmMap);
    }


    private void notifyMatchStart() {
        if (gameState != GameState.IN_LOBBY) {
            return;
        }
        logger.debug("Event: match start");

        matchWaitTime += (int) (System.currentTimeMillis() - matchWaitStartTime) / 1000;
        matchWaitStartTime = null;
        dataService.getStats().incrementSecondsWaited(matchWaitTime);
        dataService.notifyChange();
        statsPanel.refreshStats();

        matchStartTime = System.currentTimeMillis();
        connStatusLabel.setText("In match");
        displayMatchTimer(0);
        connectionCountedAsMatch = false;
        matchTimer.start();
        titleBarTimerContainer.setVisible(true);
        gameState = GameState.IN_MATCH;
    }

    private void notifyMatchEnd() {
        if (gameState != GameState.IN_MATCH) {
            return;
        }
        logger.debug("Event: match end");
        matchTimer.stop();
        reportEndOfMatchStats();
        titleBarTimerContainer.setVisible(false);
        killerInfoContainer.setVisible(false);
        connStatusLabel.setText("Match finished");
        connStatusLabel.setVisible(true);
        connTimerLabel.setText(TimeUtil.formatTimeUpToHours(0));
        lastConnMsgLabel.setText(String.format("Last match => actual play: %s / in queue: %s / overall wait: %s",
                TimeUtil.formatTimeUpToHours(getMatchDuration()),
                TimeUtil.formatTimeUpToHours(queueTime),
                TimeUtil.formatTimeUpToHours(matchWaitTime)));
        matchWaitTime = 0;
        messagePanel.setVisible(true);
        queueTime = null;
        gameState = GameState.AFTER_MATCH;
        pack();
    }

    private void reportEndOfMatchStats() {
        survivalInputPanel.reset();
        int matchTime = getMatchDuration();
        if (matchTime >= MIN_MATCH_SECONDS) {
            killerPanel.notifyEndOfMatch(matchTime);
            statsPanel.notifyEndOfMatch(matchTime);
            survivalInputPanel.setVisible(true);
        }
    }

    private void notifyServerDisconnect() {
        if (gameState != GameState.IN_MATCH && gameState != GameState.AFTER_MATCH && gameState != GameState.IN_LOBBY) {
            return;
        }
        logger.debug("Event: server disconnect");
        turnToIdle();
        pack();
    }

    public void connectToMatch(String ipAddress) {
        serverPanel.clearServer();
        serverPanel.updateServerIpAddress(ipAddress);
        changeTitleBarColor(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND, Color.WHITE);
    }

    private void turnToIdle() {
        disconnectButton.setVisible(false);
        dbdLogMonitor.resetKiller();
        queueStartTime = null;
        queueTime = null;
        queueTimer.stop();
        displayQueueTimer(0);
        matchWaitTime += matchWaitStartTime != null? (int) (System.currentTimeMillis() - matchWaitStartTime) / 1000: 0;
        matchWaitStartTime = null;
        changeTitleBarColor(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND, Color.WHITE);
        connStatusLabel.setText("Idle");
        connStatusLabel.setVisible(true);
        killerInfoContainer.setVisible(false);
        titleBarTimerContainer.setVisible(false);
        gameState = GameState.IDLE;
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
        connTimerLabel.setText("[Q] " + TimeUtil.formatTimeUpToHours(seconds));
    }

    private void displayMatchTimer(int seconds) {
        connTimerLabel.setText("[M] " + TimeUtil.formatTimeUpToHours(seconds));
    }

    public void close() {
        settings.forceSave();
        dataService.save();
        dispose();
    }


    private static final class SurvivalInputPanel extends JPanel {
        private static final String EVENT_SURVIVAL_INPUT_DONE = "survival_input_done";

        private enum SelectionState {NONE, ESCAPED, DIED, IGNORE}

        private StatsPanel statsPanel;
        private KillerPanel killerPanel;
        private LoopDataService dataService;
        private JLabel escapedButton;
        private JLabel diedButton;
        private JLabel ignoreButton;
        private Timer timer;

        private SelectionState selectionState = SelectionState.NONE;
        private Player killerPlayerBackup;


        SurvivalInputPanel(StatsPanel statsPanel, KillerPanel killerPanel, LoopDataService dataService) {
            this.statsPanel = statsPanel;
            this.killerPanel = killerPanel;
            this.dataService = dataService;
            initTimer();

            UIManager.put("ToolTip.font", ResourceFactory.getRobotoFont().deriveFont(14f));
            escapedButton = ComponentUtils.createButtonLabel(
                    Colors.INFO_PANEL_NAME_FOREGROUND,
                    "Click to indicate that you survived this match",
                    Icon.ESCAPED_BUTTON, new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            handleSurvivalStatusChange(SelectionState.ESCAPED);
                        }
                    });
            diedButton = ComponentUtils.createButtonLabel(
                    Colors.INFO_PANEL_NAME_FOREGROUND,
                    "Click to indicate that you died on this match",
                    Icon.DIED_BUTTON, new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            handleSurvivalStatusChange(SelectionState.DIED);
                        }
                    });

            ignoreButton = ComponentUtils.createButtonLabel(
                    Colors.INFO_PANEL_NAME_FOREGROUND,
                    "Click to ignore reporting your survival status for this match.",
                    Icon.IGNORE_BUTTON, new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            handleSurvivalStatusChange(SelectionState.IGNORE);
                        }
                    }
            );

            JLabel msg1Label = new JLabel("Did you survive or die this match?");
            msg1Label.setForeground(Color.BLACK);
            msg1Label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            msg1Label.setFont(font);

            JPanel msgPanel1 = new JPanel();
            msgPanel1.setLayout(new BoxLayout(msgPanel1, BoxLayout.Y_AXIS));
            msgPanel1.setBackground(Color.YELLOW);
            msgPanel1.add(msg1Label);

            JPanel survivalStatusButtonPanel = new JPanel();
            survivalStatusButtonPanel.setBackground(Colors.INFO_PANEL_BACKGROUND);
            survivalStatusButtonPanel.add(escapedButton);
            survivalStatusButtonPanel.add(diedButton);
            survivalStatusButtonPanel.add(ignoreButton);
            survivalStatusButtonPanel.setVisible(true);

            JPanel survivalMessagePanel = new JPanel();
            survivalMessagePanel.setBackground(Colors.INFO_PANEL_BACKGROUND);
            survivalMessagePanel.add(msgPanel1);
            survivalMessagePanel.setVisible(true);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(survivalStatusButtonPanel);
            add(survivalMessagePanel);
            setVisible(false);

            SurvivalInputPanel thisPanel = this;

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    thisPanel.backupStats();
                }
            });
        }

        private void backupStats() {
            if (killerPanel.getKillerPlayer() != null) {
                killerPlayerBackup = killerPanel.getKillerPlayer().clone();
            }
            statsPanel.backupStats();
        }

        private void handleSurvivalStatusChange(SelectionState newSelectionState) {
            restoreStats();

            if (newSelectionState == this.selectionState) {
                resetButtons();
                this.selectionState = SelectionState.NONE;
                timer.stop();
                return;
            }

            resetButtons();
            Boolean survived = null;

            if (newSelectionState == SelectionState.IGNORE) {
                ignoreButton.setIcon(ResourceFactory.getIcon(Icon.IGNORE_BUTTON_PRESSED));
            } else if (newSelectionState == SelectionState.ESCAPED) {
                escapedButton.setIcon(ResourceFactory.getIcon(Icon.ESCAPED_BUTTON_PRESSED));
                survived = true;
            } else {
                diedButton.setIcon(ResourceFactory.getIcon(Icon.DIED_BUTTON_PRESSED));
                survived = false;
            }

            killerPanel.notifySurvivalAgainstCurrentKiller(survived);
            statsPanel.notifyMatchSurvival(survived);
            this.selectionState = newSelectionState;
            timer.restart();
        }

        private void restoreStats() {
            Player killerPlayer = killerPanel.getKillerPlayer();

            if (killerPlayer != null) {
                killerPlayer.setEscapesAgainst(killerPlayerBackup.getEscapesAgainst());
                killerPlayer.setDeaths(killerPlayerBackup.getDeathsBy());
                dataService.notifyChange();
                killerPanel.updateKillerPlayer(killerPlayer);
            }

            statsPanel.restoreStats();
        }

        private void initTimer() {
            timer = new Timer(SURVIVAL_INPUT_WINDOW_DELAY, e -> {
                timer.stop();
                firePropertyChange(EVENT_SURVIVAL_INPUT_DONE, null, null);
            });
        }

        private void reset() {
            resetButtons();
            selectionState = SelectionState.NONE;
            timer.stop();
        }

        private void resetButtons() {
            escapedButton.setIcon(ResourceFactory.getIcon(Icon.ESCAPED_BUTTON));
            diedButton.setIcon(ResourceFactory.getIcon(Icon.DIED_BUTTON));
            ignoreButton.setIcon(ResourceFactory.getIcon(Icon.IGNORE_BUTTON));
        }
    }
}
