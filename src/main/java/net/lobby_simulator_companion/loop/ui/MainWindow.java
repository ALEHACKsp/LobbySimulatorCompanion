package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.DbdLogMonitorEvent;

/**
 * @author NickyRamone
 */
public class MainWindow extends JFrame implements Observer {

    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    public static final String PROPERTY_EXIT_REQUEST = "exit.request";
    private static final String MSG_CONNECTED = "Connected";
    private static final String MSG_DISCONNECTED = "Disconnected";
    private static final Dimension MINIMUM_SIZE = new Dimension(500, 25);
    private static final Dimension MAXIMUM_SIZE = new Dimension(500, 500);
    private static final int INFINITE_SIZE = 9999;
    private static final Font font = ResourceFactory.getRobotoFont();

    /**
     * Minimum time connected from which we can assume that a match has taken place and it was not just
     * lobby waiting time.
     */
    private static final int MINIMUM_MATCH_SECONDS = 7 * 60;

    private AppProperties appProperties = Factory.getAppProperties();
    private long sessionStartTime;
    private Timer sessionTimer;
    private Timer matchCountTimer;

    private JPanel titleBar;
    private JPanel connMsgPanel;
    private JPanel messagePanel;
    private JLabel lastConnMsgLabel;
    private JLabel connStatusLabel;
    private JLabel connTimerLabel;
    private JLabel titleBarMinimizeLabel;
    private JPanel detailPanel;
    private ServerPanel serverPanel;
    private KillerPanel killerPanel;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private boolean frameLocked = true;


    public MainWindow(Settings settings, ServerPanel serverPanel, KillerPanel killerPanel) {
        this.serverPanel = serverPanel;
        this.killerPanel = killerPanel;
        initSessionTimers();

        /* for now, panels only fire a property change when they are collapsed or expanded */
        serverPanel.addPropertyChangeListener(evt -> pack());
        killerPanel.addPropertyChangeListener(evt -> pack());

        mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2) {
                    frameLocked = !frameLocked;
                    settings.set("frame_x", getLocationOnScreen().x);
                    settings.set("frame_y", getLocationOnScreen().y);
                }
            }
        };
        mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!frameLocked) {
                    setLocation(e.getXOnScreen() - (getPreferredSize().width / 2), e.getYOnScreen() - 6);
                }
            }
        };

        setAlwaysOnTop(true);
        setUndecorated(true);
        setOpacity(0.9f);
        setMinimumSize(MINIMUM_SIZE);
        setMaximumSize(MAXIMUM_SIZE);

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
                settings.set("ui.panel.main.collapsed", false);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                titleBarMinimizeLabel.setIcon(ResourceFactory.getExpandIcon());
                pack();
                super.componentHidden(e);
                settings.set("ui.panel.main.collapsed", true);
            }
        });
        detailPanel.add(messagePanel);
        detailPanel.add(serverPanel);
        detailPanel.add(killerPanel);
        detailPanel.add(Box.createVerticalGlue());
        detailPanel.setVisible(!settings.getBoolean("ui.panel.main.collapsed"));

        JPanel container = new JPanel();
        container.setMaximumSize(new Dimension(INFINITE_SIZE, 30));
        container.setBackground(Color.BLACK);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(titleBar);
        container.add(detailPanel);

        setContentPane(container);
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseMotionListener);
        setVisible(true);
        pack();
    }


    private JPanel createTitleBar() {
        Border border = new EmptyBorder(0, 5, 0, 5);

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

        connTimerLabel = new JLabel();
        connTimerLabel.setBorder(border);
        connTimerLabel.setForeground(Color.WHITE);
        connTimerLabel.setFont(font);
        connTimerLabel.setVisible(false);

        connMsgPanel = new JPanel();
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        connMsgPanel.add(appLabel);
        connMsgPanel.add(separatorLabel);
        connMsgPanel.add(connStatusLabel);
        connMsgPanel.add(connTimerLabel);

        titleBarMinimizeLabel = new JLabel();
        titleBarMinimizeLabel.setBorder(border);
        titleBarMinimizeLabel.setIcon(ResourceFactory.getCollapseIcon());
        titleBarMinimizeLabel.setForeground(Color.WHITE);
        titleBarMinimizeLabel.setFont(font);
        titleBarMinimizeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detailPanel.setVisible(!detailPanel.isVisible());
                pack();
            }
        });

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.setPreferredSize(new Dimension(200, 25));
        container.setMaximumSize(new Dimension(INFINITE_SIZE, 25));
        container.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        container.add(switchOffButton, BorderLayout.WEST);
        container.add(connMsgPanel, BorderLayout.CENTER);
        container.add(titleBarMinimizeLabel, BorderLayout.EAST);

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
            DbdLogMonitorEvent event = (DbdLogMonitorEvent) arg;

            switch (event.type) {
                case KILLER_ID:
                    killerPanel.receiveNewKillerPlayer((PlayerIdWrapper) event.argument);
                    break;
                case KILLER_CHARACTER:
                    updateKillerCharacter((String) event.argument);
                    break;
            }
        }
    }

    private void updateKillerCharacter(String killerCharacter) {
        killerPanel.updateKillerCharacter(killerCharacter);
    }


    private void initSessionTimers() {
        sessionTimer = new Timer(1000, e ->
                connTimerLabel.setText("[" + formatTimeElapsed(getConnectionUptimeSeconds()) + "]"));

        matchCountTimer = new Timer(MINIMUM_MATCH_SECONDS * 1000, e -> {
            killerPanel.updateMatchCount();
            matchCountTimer.stop();
        });
    }

    private int getConnectionUptimeSeconds() {
        return (int) (System.currentTimeMillis() - sessionStartTime) / 1000;
    }

    public void connect() {
        connStatusLabel.setText(MSG_CONNECTED);
        serverPanel.clearServer();
        killerPanel.clearKillerInfo();
        messagePanel.setVisible(false);
        titleBar.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        connTimerLabel.setVisible(true);
        sessionStartTime = System.currentTimeMillis();
        sessionTimer.start();
        matchCountTimer.start();
        pack();
    }

    public void disconnect() {
        sessionTimer.stop();
        matchCountTimer.stop();
        connStatusLabel.setText(MSG_DISCONNECTED);
        titleBar.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        lastConnMsgLabel.setText("Last connection (below): " + formatTimeElapsed(getConnectionUptimeSeconds()));
        connTimerLabel.setVisible(false);
        messagePanel.setVisible(true);
        pack();
    }

    public void updateServer(Server server) {
        serverPanel.updateServer(server);
    }

    private String formatTimeElapsed(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int mod = totalSeconds % 3600;
        int minutes = mod / 60;
        int seconds = mod % 60;

        return hours == 0 ? String.format("%02d:%02d", minutes, seconds) :
                String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void close() {
        dispose();
    }
}
