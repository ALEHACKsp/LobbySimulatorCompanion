package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.domain.Server;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.PlayerDto;
import net.lobby_simulator_companion.loop.util.TimeUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * @author NickyRamone
 */
public class MainWindow extends JFrame implements Observer {

    public static final String PROPERTY_EXIT_REQUEST = "exit.request";

    private static final String SETTING__WINDOW_FRAME_X = "ui.window.position.x";
    private static final String SETTING__WINDOW_FRAME_Y = "ui.window.position.y";
    private static final String SETTING__MAIN_PANEL_COLLAPSED = "ui.panel.main.collapsed";
    private static final String MSG_CONNECTED = "Connected";
    private static final String MSG_DISCONNECTED = "Disconnected";
    private static final String MSG_KILLER_CHARACTER = "(as %s)";
    private static final Dimension MINIMUM_SIZE = new Dimension(600, 25);
    private static final Dimension MAXIMUM_SIZE = new Dimension(600, 500);
    private static final Border NO_BORDER = new EmptyBorder(0, 0, 0, 0);
    private static final int INFINITE_SIZE = 9999;
    private static final Font font = ResourceFactory.getRobotoFont();

    /**
     * Minimum time connected from which we can assume that a match has taken place.
     */
    private static final int MIN_MATCH_SECONDS = 5 * 60;
    private static final int MAX_KILLER_PLAYER_NAME_LEN = 25;

    private Settings settings;
    private AppProperties appProperties = Factory.getAppProperties();
    private boolean connected;
    private Long matchStartTime;
    private Timer matchTimer;

    private JPanel titleBar;
    private JPanel connMsgPanel;
    private JPanel messagePanel;
    private JLabel lastConnMsgLabel;
    private JLabel connStatusLabel;
    private JPanel killerInfoContainer;
    private JLabel killerPlayerValueLabel;
    private JLabel killerPlayerRateLabel;
    private JLabel killerPlayerNotesLabel;
    private JLabel killerCharLabel;
    private JPanel titleBarTimerContainer;
    private JLabel connTimerLabel;
    private JLabel titleBarMinimizeLabel;
    private JPanel titleBarButtonContainer;
    private JPanel detailPanel;
    private ServerPanel serverPanel;
    private KillerPanel killerPanel;


    public MainWindow(Settings settings, ServerPanel serverPanel, KillerPanel killerPanel) {
        this.settings = settings;
        this.serverPanel = serverPanel;
        this.killerPanel = killerPanel;
        initSessionTimers();

        serverPanel.addPropertyChangeListener(evt -> pack());
        killerPanel.addPropertyChangeListener(evt -> {
            String propertyName = evt.getPropertyName();

            if (KillerPanel.EVENT_STRUCTURE_CHANGED.equals(propertyName)) {
                pack();
            } else if (connected && KillerPanel.EVENT_KILLER_UPDATE.equals(propertyName)) {
                updateKillerOnTitleBar();
            }
        });

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
                titleBarMinimizeLabel.setIcon(ResourceFactory.getCollapseIcon());
                pack();
                super.componentShown(e);
                settings.set(SETTING__MAIN_PANEL_COLLAPSED, false);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                titleBarMinimizeLabel.setIcon(ResourceFactory.getExpandIcon());
                pack();
                super.componentHidden(e);
                settings.set(SETTING__MAIN_PANEL_COLLAPSED, true);
            }
        });
        detailPanel.add(messagePanel);
        detailPanel.add(serverPanel);
        detailPanel.add(killerPanel);
        detailPanel.add(Box.createVerticalGlue());
        detailPanel.setVisible(!settings.getBoolean(SETTING__MAIN_PANEL_COLLAPSED));

        JPanel container = new JPanel();
        container.setMaximumSize(new Dimension(INFINITE_SIZE, 30));
        container.setBackground(Color.BLACK);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(titleBar);
        container.add(detailPanel);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                Point frameLocation = getLocation();
                settings.set(SETTING__WINDOW_FRAME_X, frameLocation.x);
                settings.set(SETTING__WINDOW_FRAME_Y, frameLocation.y);
            }
        });

        setContentPane(container);
        setVisible(true);
        pack();
    }


    private void updateKillerOnTitleBar() {
        Player player = killerPanel.getKillerPlayer();
        connStatusLabel.setVisible(false);
        killerInfoContainer.setVisible(true);

        if (settings.getExperimentalSwitch(2)) {
            String killerChar = killerPanel.getKillerCharacter();
            killerCharLabel.setVisible(killerChar != null && killerPanel.isShowKillerCharacter());
            if (killerChar != null) {
                killerCharLabel.setText(String.format(MSG_KILLER_CHARACTER, killerChar));
            }
        }

        if (player != null) {
            if (settings.getExperimentalSwitch(1)) {
                killerPlayerValueLabel.setText(shortenKillerPlayerName(player.getMostRecentName()));
            }

            updateKillerRateOnTitleBar(player.getRating());
            killerPlayerNotesLabel.setVisible(player.getDescription() != null && !player.getDescription().isEmpty());
        }
    }

    private void updateKillerRateOnTitleBar(Player.Rating rate) {
        if (rate == Player.Rating.THUMBS_UP) {
            killerPlayerRateLabel.setIcon(ResourceFactory.getThumbsUpIcon());
            killerPlayerRateLabel.setVisible(true);
        } else if (rate == Player.Rating.THUMBS_DOWN) {
            killerPlayerRateLabel.setIcon(ResourceFactory.getThumbsDownIcon());
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

        JLabel separatorLabel = new JLabel("-");
        separatorLabel.setBorder(border);
        separatorLabel.setForeground(Color.WHITE);
        separatorLabel.setFont(font);

        connStatusLabel = new JLabel(MSG_DISCONNECTED);
        connStatusLabel.setBorder(border);
        connStatusLabel.setForeground(Color.WHITE);
        connStatusLabel.setFont(font);

        JLabel killerPlayerLabel = new JLabel();
        killerPlayerLabel.setBorder(border);
        killerPlayerLabel.setFont(font);
        killerPlayerLabel.setIcon(ResourceFactory.getSkullIcon());

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
        killerPlayerNotesLabel.setIcon(ResourceFactory.getEditIcon());

        killerCharLabel = new JLabel();
        killerCharLabel.setBorder(new EmptyBorder(2, 0, 0, 5));
        killerCharLabel.setForeground(Color.BLUE);
        killerCharLabel.setFont(font);

        killerInfoContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        killerInfoContainer.setBorder(NO_BORDER);
        killerInfoContainer.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        killerInfoContainer.add(killerPlayerLabel);
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

        titleBarTimerContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBarTimerContainer.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        titleBarTimerContainer.add(timerSeparatorLabel);
        titleBarTimerContainer.add(connTimerLabel);
        titleBarTimerContainer.setVisible(false);

        connMsgPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        connMsgPanel.add(appLabel);
        connMsgPanel.add(separatorLabel);
        connMsgPanel.add(connStatusLabel);
        connMsgPanel.add(killerInfoContainer);
        connMsgPanel.add(titleBarTimerContainer);
        MouseDragListener mouseDragListener = new MouseDragListener(this);
        connMsgPanel.addMouseListener(mouseDragListener);
        connMsgPanel.addMouseMotionListener(mouseDragListener);

        JLabel switchOffButton = new JLabel();
        switchOffButton.setBorder(border);
        switchOffButton.setIcon(ResourceFactory.getSwitchOffIcon());
        switchOffButton.setToolTipText("Exit application");
        switchOffButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                firePropertyChange(PROPERTY_EXIT_REQUEST, false, true);
            }
        });

        titleBarMinimizeLabel = new JLabel();
        titleBarMinimizeLabel.setBorder(border);
        titleBarMinimizeLabel.setIcon(ResourceFactory.getCollapseIcon());
        titleBarMinimizeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detailPanel.setVisible(!detailPanel.isVisible());
                pack();
            }
        });

        titleBarButtonContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBarButtonContainer.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
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

        JLabel clearButton = new JLabel();
        clearButton.setIcon(ResourceFactory.getClearIcon());
        clearButton.setToolTipText("Clear data (it will still be saved)");
        clearButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                serverPanel.clearServer();
                killerPanel.clearKillerInfo();
                messagePanel.setVisible(false);
                pack();
            }
        });

        JPanel container = new JPanel();
        container.setBackground(Colors.MSG_BAR_BACKGROUND);
        container.setLayout(new FlowLayout());
        container.add(lastConnMsgLabel);
        container.add(elapsedLabel);
        container.add(clearButton);
        container.setVisible(false);

        return container;
    }


    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof DbdLogMonitor) {
            DbdLogMonitor.Event event = (DbdLogMonitor.Event) arg;

            switch (event.type) {
                case KILLER_PLAYER:
                    SwingUtilities.invokeLater(() -> killerPanel.receiveNewKillerPlayer((PlayerDto) event.argument));
                    break;
                case KILLER_CHARACTER:
                    String killerCharacter = (String) event.argument;
                    SwingUtilities.invokeLater(() -> updateKillerCharacter(killerCharacter));
                    break;
            }
        }
    }

    private void updateKillerCharacter(String killerCharacter) {
        killerPanel.updateKillerCharacter(killerCharacter);
    }

    private String shortenKillerPlayerName(String playerName) {
        String result = playerName;
        if (result.length() > MAX_KILLER_PLAYER_NAME_LEN) {
            result = result.substring(0, MAX_KILLER_PLAYER_NAME_LEN - 3) + "...";
        }

        return result;
    }


    private void initSessionTimers() {
        matchTimer = new Timer(1000, e ->
                connTimerLabel.setText(TimeUtil.formatTimeElapsed(getMatchDuration())));
    }

    private int getMatchDuration() {
        return matchStartTime == null ? 0 : (int) (System.currentTimeMillis() - matchStartTime) / 1000;
    }

    public void connectToMatch(String ipAddress) {
        connected = true;
        connStatusLabel.setText(MSG_CONNECTED);
        serverPanel.clearServer();
        killerPanel.clearKillerInfo();
        messagePanel.setVisible(false);
        titleBar.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        killerInfoContainer.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        titleBarButtonContainer.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        startMatch();
        serverPanel.updateServerIpAddress(ipAddress);
        pack();
    }

    public void disconnectFromMatch() {
        connected = false;
        connStatusLabel.setText(MSG_DISCONNECTED);
        connStatusLabel.setVisible(true);
        killerInfoContainer.setVisible(false);
        titleBar.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        titleBarButtonContainer.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);

        lastConnMsgLabel.setText("Last connection (below): " + TimeUtil.formatTimeElapsed(getMatchDuration()));
        endMatch();
        titleBarTimerContainer.setVisible(false);
        messagePanel.setVisible(true);
        pack();
    }

    private void startMatch() {
        resetTimer();
        matchStartTime = System.currentTimeMillis();
        matchTimer.start();
        titleBarTimerContainer.setVisible(true);
    }

    private void endMatch() {
        matchTimer.stop();
        int matchTime = getMatchDuration();
        if (matchTime >= MIN_MATCH_SECONDS) {
            killerPanel.updateKillerMatchTime(matchTime);
            killerPanel.updateMatchCount();
        }
        resetTimer();
        matchStartTime = null;
    }

    public void updateServer(Server server) {
        serverPanel.updateServerPanel(server);
    }

    private void resetTimer() {
        matchStartTime = System.currentTimeMillis();
        connTimerLabel.setText(TimeUtil.formatTimeElapsed(0));
    }

    public void close() {
        settings.forceSave();
        dispose();
    }

}
