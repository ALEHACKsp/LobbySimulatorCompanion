package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.service.DbdSteamLogMonitor;
import net.lobby_simulator_companion.loop.service.SteamUser;

import javax.swing.*;
import java.awt.*;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Timer;

/**
 * A panel for debugging purposes.
 * Simulated connections and host user discovery.
 *
 * @author NickyRamone
 */
public class DebugPanel extends JPanel {

    private Set<Integer> connections = new HashSet<>();

    private Overlay mainPanel;
    private DbdSteamLogMonitor logMonitor;
    private JFrame frame;

    private Random random = new Random();


    public DebugPanel(Overlay overlay, DbdSteamLogMonitor logMonitor) throws Exception {
        mainPanel = overlay;
        this.logMonitor = logMonitor;
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        setOpaque(true);
        frame = new JFrame("Debug Panel");

        /** Since this frame will be always on top, having the focus state in true
         * will make other windows unclickable so we need to set it to false. */
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(Color.WHITE);
        frame.setLayout(new GridLayout(0, 1, 10, 10));
        frame.setAlwaysOnTop(true);
        addComponents();
        startPingSimulator();
        frame.pack();
        frame.setLocation(700, 300);
        frame.setVisible(true);
    }

    private void addComponents() {
        JButton button = new JButton("Reset");
        button.addActionListener(e -> reset());
        frame.add(button);

        JPanel connectionContainer = new JPanel();
        connectionContainer.setLayout(new GridLayout(2, 2, 10, 10));
        button = new JButton("Add Connection 0");
        button.addActionListener(e -> addConnection(0));
        connectionContainer.add(button);

        button = new JButton("Remove Connection 0");
        button.addActionListener(e -> removeConnection(0));
        connectionContainer.add(button);

        button = new JButton("Add Connection 1");
        connectionContainer.add(button);
        button.addActionListener(e -> addConnection(1));

        button = new JButton("Remove Connection 1");
        button.addActionListener(e -> removeConnection(1));
        connectionContainer.add(button);

        frame.add(connectionContainer);

        JPanel userContainer = new JPanel();
        userContainer.setLayout(new GridLayout(2, 1, 10, 10));
        button = new JButton("Join lobby A");
        button.addActionListener(e -> simulateLobby("DummyUserA"));
        userContainer.add(button);

        button = new JButton("Join Lobby B");
        button.addActionListener(e -> simulateLobby("DummyUserB"));
        userContainer.add(button);
        frame.add(userContainer);
    }

    private void reset() {
        connections.clear();
    }


    private void addConnection(int connectionId) {
        connections.add(connectionId);
    }

    private void removeConnection(int connectionId) {
        connections.remove(connectionId);
    }

    private void simulateLobby(String userName) {
        SteamUser user = new SteamUser(String.valueOf(userName.hashCode()), userName);
        mainPanel.update(logMonitor, user);
    }


    private void startPingSimulator() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                for (Integer connectionId : connections) {
                    try {
                        Inet4Address ip = (Inet4Address) Inet4Address.getByName("1.2.3." + connectionId);
                        int ping = getConnectionPing(connectionId);
                        mainPanel.setPing(ip, ping);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 1000);

    }

    private int getConnectionPing(int connectionId) {
        int min = connectionId * 100;
        int max = (connectionId + 1) * 100;

        return random.nextInt((max - min)) + min;
    }
}
