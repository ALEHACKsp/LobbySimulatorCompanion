package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.Server;
import net.lobby_simulator_companion.loop.service.SteamUser;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.DbdLogMonitorEvent;

/**
 * @author Nicky Ramone
 */
public class MainPanel extends JFrame implements Observer {

    private static final String MSG_CONNECTED = "Connected";
    private static final String MSG_DISCONNECTED = "Disconnected";

    private static final Dimension MINIMUM_SIZE = new Dimension(500, 30);
    private static final Dimension MAXIMUM_SIZE = new Dimension(500, 500);
    private static final int INFINITE_SIZE = 9999;
    private static final Font font = ResourceFactory.getRobotoFont();

    private AppProperties appProperties = Factory.getAppProperties();
    private long sessionStartTime;
    private Timer sessionTimer;

    private JPanel connectionPanel;
    private JPanel connMsgPanel;
    private JPanel messagePanel;
    private JLabel lastConnMsgLabel;
    private JLabel connStatusLabel;
    private JLabel connTimerLabel;
    private JLabel connStatusMinimizeLabel;
    private JPanel detailPanel;
    private ServerPanel serverPanel;
    private KillerPanel killerPanel;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private boolean frameLocked = true;


    public MainPanel() {
        initSessionTimer();

        mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2) {
                    frameLocked = !frameLocked;
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

        JPanel container = new JPanel();
        container.setBackground(Color.BLACK);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        setContentPane(container);

        connectionPanel = createConnectionStatusBar();
        container.setMaximumSize(new Dimension(INFINITE_SIZE, 30));
        container.add(connectionPanel);


        messagePanel = createMessagePanel();
        serverPanel = new ServerPanel(this);
        killerPanel = new KillerPanel(this);


        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.add(messagePanel);
        detailPanel.add(serverPanel);
        detailPanel.add(killerPanel);
        detailPanel.add(Box.createVerticalGlue());

        connStatusMinimizeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean expanded = detailPanel.isVisible();
                ImageIcon icon = expanded? ResourceFactory.getExpandIcon(): ResourceFactory.getCollapseIcon();
                connStatusMinimizeLabel.setIcon(icon);
                detailPanel.setVisible(!expanded);
                pack();
            }
        });

        container.add(detailPanel);

        addMouseListener(mouseListener);
        addMouseMotionListener(mouseMotionListener);

        pack();
        setVisible(true);
    }


    private JPanel createConnectionStatusBar() {
        Border border = new EmptyBorder(0, 5, 0, 5);

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

        connStatusMinimizeLabel = new JLabel();
        connStatusMinimizeLabel.setBorder(border);
        connStatusMinimizeLabel.setIcon(ResourceFactory.getCollapseIcon());
        connStatusMinimizeLabel.setForeground(Color.WHITE);
        connStatusMinimizeLabel.setFont(font);

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.setPreferredSize(new Dimension(500, 25));
        container.setMaximumSize(new Dimension(INFINITE_SIZE, 25));
        container.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        container.add(connMsgPanel, BorderLayout.CENTER);
        container.add(connStatusMinimizeLabel, BorderLayout.EAST);

        return container;
    }

    private JPanel createMessagePanel() {
        JPanel container = new JPanel();
        container.setBackground(Colors.MSG_BAR_BACKGROUND);
        container.setLayout(new FlowLayout());

        lastConnMsgLabel = new JLabel();
        lastConnMsgLabel.setForeground(Colors.MSG_BAR_FOREGROUND);
        lastConnMsgLabel.setFont(font);

        JLabel elapsedLabel = new JLabel();
        elapsedLabel.setForeground(Colors.MSG_BAR_FOREGROUND);
        elapsedLabel.setFont(font);

        JLabel clearButton = new JLabel();
        clearButton.setIcon(ResourceFactory.getResetIcon());
        clearButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                serverPanel.clearServer();
                killerPanel.clearKillerInfo();
                messagePanel.setVisible(false);
            }
        });

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
                case KILLER_STEAM_USER: updateKillerUser((SteamUser) event.argument); break;
                case KILLER_CHARACTER: updateKillerCharacter((String) event.argument); break;
            }
        }
    }

    private void updateKillerUser(SteamUser killerUser) {
        killerPanel.updateKillerUser(killerUser);
    }

    private void updateKillerCharacter(String killerCharacter) {
        killerPanel.updateKillerCharacter(killerCharacter);
    }



    private void initSessionTimer() {
        sessionTimer = new Timer(1000, e -> {
            connTimerLabel.setText("[" + formatTimeElapsed(getConnectionUptimeSeconds()) + "]");
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
        connectionPanel.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_CONNECTED_BACKGROUND);
        connTimerLabel.setVisible(true);
        sessionStartTime = System.currentTimeMillis();
        sessionTimer.start();
        pack();
    }

    public void disconnect() {
        sessionTimer.stop();
        connStatusLabel.setText(MSG_DISCONNECTED);
        connectionPanel.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        connMsgPanel.setBackground(Colors.CONNECTION_BAR_DISCONNECTED_BACKGROUND);
        lastConnMsgLabel.setText("Last connection [" + formatTimeElapsed(getConnectionUptimeSeconds()) + "]");
        connTimerLabel.setVisible(false);
        messagePanel.setVisible(true);
        pack();
    }

    public void updateServer(Server server) {
        serverPanel.updateServer(server);
    }

    private String formatTimeElapsed(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int remaind = totalSeconds % 3600;
        int minutes = remaind / 60;
        int seconds = remaind % 60;

        return hours == 0? String.format("%02d:%02d", minutes, seconds):
                String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void close() {
        dispose();
    }
}
