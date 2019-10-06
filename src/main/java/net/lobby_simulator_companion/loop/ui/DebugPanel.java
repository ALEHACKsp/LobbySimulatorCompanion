package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.Server;
import net.lobby_simulator_companion.loop.service.SteamUser;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.DbdLogMonitorEvent;
import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.DbdLogMonitorEvent.EventType;

/**
 * A panel for debugging purposes.
 * Simulated connections and host user discovery.
 *
 * @author NickyRamone
 */
public class DebugPanel extends JPanel {

    private Set<Integer> connections = new HashSet<>();

    private MainPanel mainPanel;
    private DbdLogMonitor logMonitor;
    private JFrame frame;

    private Random random = new Random();


    public DebugPanel(MainPanel mainPanel, DbdLogMonitor logMonitor) throws Exception {
        this.mainPanel = mainPanel;
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
        frame.pack();
        frame.setLocation(800, 300);
        frame.setVisible(true);
    }

    private void addComponents() {
        JButton button = new JButton("Reset");
        button.addActionListener(e -> reset());
        frame.add(button);

        JPanel connectionContainer = new JPanel();
        connectionContainer.setLayout(new BoxLayout(connectionContainer, BoxLayout.X_AXIS));
        button = new JButton("Connect");
        button.addActionListener(e -> mainPanel.connect());
        connectionContainer.add(button);

        button = new JButton("Disconnect");
        button.addActionListener(e -> mainPanel.disconnect());
        connectionContainer.add(button);

        frame.add(connectionContainer);


        button = new JButton("update server");
        button.addActionListener(e -> mainPanel.updateServer(generateRandomServer()));
        frame.add(button);

        JPanel userContainer = new JPanel();
        userContainer.setLayout(new GridLayout(2, 1, 10, 10));
        button = new JButton("Detect killer A");
        button.addActionListener(e -> simulateKillerUserUpdate("DummyUserA"));
        userContainer.add(button);

        button = new JButton("Detect killer B");
        button.addActionListener(e -> simulateKillerUserUpdate("DummyUserB"));
        userContainer.add(button);
        frame.add(userContainer);
    }

    private Server generateRandomServer() {
        Server server = new Server();
        server.setCountry("some country");
        server.setRegion("some region");
        server.setCity("some city");

        return server;
    }

    private void reset() {
        connections.clear();
    }

    private void simulateKillerUserUpdate(String userName) {
        SteamUser killerUser = new SteamUser(String.valueOf(userName.hashCode()), userName);

        mainPanel.update(logMonitor, new DbdLogMonitorEvent(EventType.KILLER_STEAM_USER, killerUser));
    }

}
